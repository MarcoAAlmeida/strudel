package com.marcoalmeida.midi_tokenizer;

import com.marcoalmeida.midi_tokenizer.midi.MidiParser;
import com.marcoalmeida.midi_tokenizer.model.FileOutput;
import com.marcoalmeida.midi_tokenizer.model.NoteEvent;
import com.marcoalmeida.midi_tokenizer.model.EventBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MidiParser using programmatically created MIDI sequences.
 */
public class MidiParserTest {

    @Test
    public void testParseSimpleSequence(@TempDir Path tempDir) throws Exception {
        // Create a simple MIDI sequence with one track
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        Track track = sequence.createTrack();
        
        // Add a tempo event (120 BPM = 500000 microseconds per quarter note)
        addTempoEvent(track, 0, 500000);
        
        // Add a note: C4 (60) at tick 0, duration 480 ticks (one quarter note)
        addNoteOnEvent(track, 0, 0, 60, 100);
        addNoteOffEvent(track, 480, 0, 60);
        
        // Add end of track
        addEndOfTrackEvent(track, 960);
        
        // Write to temporary file
        File midiFile = tempDir.resolve("test.mid").toFile();
        MidiSystem.write(sequence, 1, midiFile);
        
        // Parse the file
        MidiParser parser = new MidiParser();
        parser.setIncludeMeta(true);
        parser.setUseSeconds(true);
        
        FileOutput output = parser.parse(midiFile);
        
        // Verify output
        assertNotNull(output);
        assertEquals("1.0", output.getSchemaVersion());
        assertEquals("test.mid", output.getFile().getFilename());
        assertEquals(480, output.getFile().getDivision());
        
        // Verify tempo map
        assertNotNull(output.getMetadata());
        assertFalse(output.getMetadata().getTempoMap().isEmpty());
        assertEquals(120.0, output.getMetadata().getTempoMap().get(0).getBpm(), 0.1);
        
        // Verify tracks
        assertFalse(output.getTracks().isEmpty());
        
        // Find note events
        long noteCount = output.getTracks().get(0).getEvents().stream()
            .filter(e -> e instanceof NoteEvent)
            .count();
        assertEquals(1, noteCount, "Should have exactly one note event");
        
        // Verify note properties
        NoteEvent note = (NoteEvent) output.getTracks().get(0).getEvents().stream()
            .filter(e -> e instanceof NoteEvent)
            .findFirst()
            .orElseThrow();
        
        assertEquals(60, note.getNoteNumber());
        assertEquals("C4", note.getNoteName());
        assertEquals(100, note.getVelocity());
        assertEquals(0, note.getChannel());
        assertEquals(480L, note.getDurationTicks());
        assertNotNull(note.getDurationSeconds());
        assertTrue(note.getDurationSeconds() > 0.0);
    }

    @Test
    public void testParseMultipleNotes(@TempDir Path tempDir) throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        Track track = sequence.createTrack();
        
        addTempoEvent(track, 0, 500000);
        
        // Add three notes
        addNoteOnEvent(track, 0, 0, 60, 100);    // C4
        addNoteOffEvent(track, 480, 0, 60);
        
        addNoteOnEvent(track, 480, 0, 64, 90);   // E4
        addNoteOffEvent(track, 960, 0, 64);
        
        addNoteOnEvent(track, 960, 0, 67, 80);   // G4
        addNoteOffEvent(track, 1440, 0, 67);
        
        addEndOfTrackEvent(track, 1440);
        
        File midiFile = tempDir.resolve("multiple.mid").toFile();
        MidiSystem.write(sequence, 1, midiFile);
        
        MidiParser parser = new MidiParser();
        FileOutput output = parser.parse(midiFile);
        
        assertNotNull(output);
        
        long noteCount = output.getTracks().get(0).getEvents().stream()
            .filter(e -> e instanceof NoteEvent)
            .count();
        assertEquals(3, noteCount, "Should have three note events");
    }

    @Test
    public void testParseWithTimeSignature(@TempDir Path tempDir) throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        Track track = sequence.createTrack();
        
        addTempoEvent(track, 0, 500000);
        addTimeSignatureEvent(track, 0, 4, 4);
        
        addNoteOnEvent(track, 0, 0, 60, 100);
        addNoteOffEvent(track, 480, 0, 60);
        
        addEndOfTrackEvent(track, 960);
        
        File midiFile = tempDir.resolve("timesig.mid").toFile();
        MidiSystem.write(sequence, 1, midiFile);
        
        MidiParser parser = new MidiParser();
        FileOutput output = parser.parse(midiFile);
        
        assertNotNull(output);
        assertFalse(output.getMetadata().getTimeSignatures().isEmpty());
        assertEquals(4, output.getMetadata().getTimeSignatures().get(0).getNumerator());
        assertEquals(4, output.getMetadata().getTimeSignatures().get(0).getDenominator());
    }

    @Test
    public void testParseWithoutSeconds(@TempDir Path tempDir) throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        Track track = sequence.createTrack();
        
        addNoteOnEvent(track, 0, 0, 60, 100);
        addNoteOffEvent(track, 480, 0, 60);
        addEndOfTrackEvent(track, 960);
        
        File midiFile = tempDir.resolve("noticks.mid").toFile();
        MidiSystem.write(sequence, 1, midiFile);
        
        MidiParser parser = new MidiParser();
        parser.setUseSeconds(false);
        
        FileOutput output = parser.parse(midiFile);
        
        assertNotNull(output);
        
        NoteEvent note = (NoteEvent) output.getTracks().get(0).getEvents().stream()
            .filter(e -> e instanceof NoteEvent)
            .findFirst()
            .orElseThrow();
        
        assertNull(note.getTimeSeconds(), "Time in seconds should be null when useSeconds=false");
        assertNull(note.getDurationSeconds(), "Duration in seconds should be null when useSeconds=false");
    }

    // Helper methods to create MIDI events
    
    private void addNoteOnEvent(Track track, long tick, int channel, int note, int velocity) 
            throws InvalidMidiDataException {
        ShortMessage msg = new ShortMessage();
        msg.setMessage(ShortMessage.NOTE_ON, channel, note, velocity);
        track.add(new MidiEvent(msg, tick));
    }
    
    private void addNoteOffEvent(Track track, long tick, int channel, int note) 
            throws InvalidMidiDataException {
        ShortMessage msg = new ShortMessage();
        msg.setMessage(ShortMessage.NOTE_OFF, channel, note, 0);
        track.add(new MidiEvent(msg, tick));
    }
    
    private void addTempoEvent(Track track, long tick, int microsecondsPerQuarter) 
            throws InvalidMidiDataException {
        byte[] data = new byte[3];
        data[0] = (byte) ((microsecondsPerQuarter >> 16) & 0xFF);
        data[1] = (byte) ((microsecondsPerQuarter >> 8) & 0xFF);
        data[2] = (byte) (microsecondsPerQuarter & 0xFF);
        
        MetaMessage msg = new MetaMessage();
        msg.setMessage(0x51, data, 3);
        track.add(new MidiEvent(msg, tick));
    }
    
    private void addTimeSignatureEvent(Track track, long tick, int numerator, int denominator) 
            throws InvalidMidiDataException {
        byte[] data = new byte[4];
        data[0] = (byte) numerator;
        data[1] = (byte) (Math.log(denominator) / Math.log(2)); // Convert to power of 2
        data[2] = 24; // MIDI clocks per metronome click
        data[3] = 8;  // 32nd notes per quarter note
        
        MetaMessage msg = new MetaMessage();
        msg.setMessage(0x58, data, 4);
        track.add(new MidiEvent(msg, tick));
    }
    
    private void addEndOfTrackEvent(Track track, long tick) throws InvalidMidiDataException {
        MetaMessage msg = new MetaMessage();
        msg.setMessage(0x2F, new byte[0], 0);
        track.add(new MidiEvent(msg, tick));
    }
}
