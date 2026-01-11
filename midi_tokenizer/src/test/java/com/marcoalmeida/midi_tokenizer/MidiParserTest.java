package com.marcoalmeida.midi_tokenizer;

import com.marcoalmeida.midi_tokenizer.midi.MidiParser;
import com.marcoalmeida.midi_tokenizer.midi.NoteUtils;
import com.marcoalmeida.midi_tokenizer.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.midi.*;
import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the MIDI parser using programmatically created MIDI sequences.
 */
class MidiParserTest {
    
    @Test
    void testNoteNumberToName() {
        assertEquals("C4", NoteUtils.noteNumberToName(60)); // Middle C
        assertEquals("A4", NoteUtils.noteNumberToName(69)); // A440
        assertEquals("C-1", NoteUtils.noteNumberToName(0));
        assertEquals("G9", NoteUtils.noteNumberToName(127));
        assertEquals("C#5", NoteUtils.noteNumberToName(73));
        assertEquals("D#3", NoteUtils.noteNumberToName(51));
    }
    
    @Test
    void testNoteNameComponents() {
        assertEquals("C", NoteUtils.getNoteName(60));
        assertEquals("A", NoteUtils.getNoteName(69));
        assertEquals("C#", NoteUtils.getNoteName(73));
        
        assertEquals(4, NoteUtils.getOctave(60));
        assertEquals(4, NoteUtils.getOctave(69));
        assertEquals(-1, NoteUtils.getOctave(0));
    }
    
    @Test
    void testInvalidNoteNumber() {
        assertThrows(IllegalArgumentException.class, () -> NoteUtils.noteNumberToName(-1));
        assertThrows(IllegalArgumentException.class, () -> NoteUtils.noteNumberToName(128));
    }
    
    @Test
    void testParseSimpleMidiSequence(@TempDir Path tempDir) throws Exception {
        // Create a simple MIDI sequence with one note
        File midiFile = tempDir.resolve("test.mid").toFile();
        
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        Track track = sequence.createTrack();
        
        // Add a C4 (middle C) note starting at tick 0, duration 480 ticks
        addNoteToTrack(track, 0, 60, 100, 0, 480);
        
        // Set tempo to 120 BPM (500000 microseconds per quarter note)
        addTempoToTrack(track, 0, 500000);
        
        // End of track
        addEndOfTrack(track, 960);
        
        // Write to file
        MidiSystem.write(sequence, 1, midiFile);
        
        // Parse the file
        MidiParser parser = new MidiParser();
        MidiOutput output = parser.parse(midiFile, true, true);
        
        // Verify file metadata
        assertNotNull(output.getFile());
        assertEquals("test.mid", output.getFile().getFilename());
        assertEquals(480, output.getFile().getDivision());
        
        // Verify tempo
        assertEquals(1, output.getMetadata().getTempoMap().size());
        TempoEntry tempo = output.getMetadata().getTempoMap().get(0);
        assertEquals(0, tempo.getTick());
        assertEquals(500000, tempo.getMicrosecondsPerQuarter());
        assertEquals(120.0, tempo.getBpm(), 0.01);
        
        // Verify tracks
        assertEquals(1, output.getTracks().size());
        TrackOutput trackOutput = output.getTracks().get(0);
        
        // Find the note event
        EventOutput noteEvent = trackOutput.getEvents().stream()
            .filter(e -> "note".equals(e.getType()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(noteEvent);
        assertEquals(60, noteEvent.getNoteNumber());
        assertEquals("C4", noteEvent.getNoteName());
        assertEquals(100, noteEvent.getVelocity());
        assertEquals(480, noteEvent.getDurationTicks());
        assertNotNull(noteEvent.getTimeSeconds());
        assertNotNull(noteEvent.getDurationSeconds());
    }
    
    @Test
    void testParseMultipleNotes(@TempDir Path tempDir) throws Exception {
        File midiFile = tempDir.resolve("test_multi.mid").toFile();
        
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        Track track = sequence.createTrack();
        
        // Add multiple notes
        addNoteToTrack(track, 0, 60, 100, 0, 480);      // C4
        addNoteToTrack(track, 0, 480, 64, 90, 480);     // E4
        addNoteToTrack(track, 0, 960, 67, 80, 480);     // G4
        
        addTempoToTrack(track, 0, 500000);
        addEndOfTrack(track, 1440);
        
        MidiSystem.write(sequence, 1, midiFile);
        
        MidiParser parser = new MidiParser();
        MidiOutput output = parser.parse(midiFile, true, true);
        
        TrackOutput trackOutput = output.getTracks().get(0);
        
        long noteCount = trackOutput.getEvents().stream()
            .filter(e -> "note".equals(e.getType()))
            .count();
        
        assertEquals(3, noteCount, "Should have 3 note events");
    }
    
    @Test
    void testProgramChange(@TempDir Path tempDir) throws Exception {
        File midiFile = tempDir.resolve("test_program.mid").toFile();
        
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        Track track = sequence.createTrack();
        
        // Add program change
        ShortMessage programChange = new ShortMessage();
        programChange.setMessage(ShortMessage.PROGRAM_CHANGE, 0, 42, 0);
        track.add(new MidiEvent(programChange, 0));
        
        addNoteToTrack(track, 0, 480, 60, 100, 480);
        addTempoToTrack(track, 0, 500000);
        addEndOfTrack(track, 960);
        
        MidiSystem.write(sequence, 1, midiFile);
        
        MidiParser parser = new MidiParser();
        MidiOutput output = parser.parse(midiFile, true, true);
        
        TrackOutput trackOutput = output.getTracks().get(0);
        assertEquals(1, trackOutput.getProgramChanges().size());
        
        ProgramChangeEvent pc = trackOutput.getProgramChanges().get(0);
        assertEquals(0, pc.getChannel());
        assertEquals(42, pc.getProgram());
    }
    
    @Test
    void testTimeSignature(@TempDir Path tempDir) throws Exception {
        File midiFile = tempDir.resolve("test_timesig.mid").toFile();
        
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        Track track = sequence.createTrack();
        
        // Add 4/4 time signature
        MetaMessage timeSig = new MetaMessage();
        timeSig.setMessage(0x58, new byte[]{4, 2, 24, 8}, 4); // 4/4 time
        track.add(new MidiEvent(timeSig, 0));
        
        addTempoToTrack(track, 0, 500000);
        addEndOfTrack(track, 480);
        
        MidiSystem.write(sequence, 1, midiFile);
        
        MidiParser parser = new MidiParser();
        MidiOutput output = parser.parse(midiFile, true, true);
        
        assertEquals(1, output.getMetadata().getTimeSignatures().size());
        TimeSignatureEntry ts = output.getMetadata().getTimeSignatures().get(0);
        assertEquals(4, ts.getNumerator());
        assertEquals(4, ts.getDenominator());
    }
    
    @Test
    void testKeySignature(@TempDir Path tempDir) throws Exception {
        File midiFile = tempDir.resolve("test_keysig.mid").toFile();
        
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        Track track = sequence.createTrack();
        
        // Add C major key signature (0 sharps/flats, major mode)
        MetaMessage keySig = new MetaMessage();
        keySig.setMessage(0x59, new byte[]{0, 0}, 2);
        track.add(new MidiEvent(keySig, 0));
        
        addTempoToTrack(track, 0, 500000);
        addEndOfTrack(track, 480);
        
        MidiSystem.write(sequence, 1, midiFile);
        
        MidiParser parser = new MidiParser();
        MidiOutput output = parser.parse(midiFile, true, true);
        
        assertEquals(1, output.getMetadata().getKeySignatures().size());
        KeySignatureEntry ks = output.getMetadata().getKeySignatures().get(0);
        assertEquals(0, ks.getSharpsFlats());
        assertEquals(0, ks.getMajorMinor());
    }
    
    @Test
    void testTrackName(@TempDir Path tempDir) throws Exception {
        File midiFile = tempDir.resolve("test_trackname.mid").toFile();
        
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        Track track = sequence.createTrack();
        
        // Add track name
        MetaMessage trackName = new MetaMessage();
        String name = "Piano Track";
        trackName.setMessage(0x03, name.getBytes(), name.length());
        track.add(new MidiEvent(trackName, 0));
        
        addTempoToTrack(track, 0, 500000);
        addEndOfTrack(track, 480);
        
        MidiSystem.write(sequence, 1, midiFile);
        
        MidiParser parser = new MidiParser();
        MidiOutput output = parser.parse(midiFile, true, true);
        
        assertEquals(1, output.getTracks().size());
        assertEquals("Piano Track", output.getTracks().get(0).getName());
    }
    
    @Test
    void testParseToJson(@TempDir Path tempDir) throws Exception {
        File midiFile = tempDir.resolve("test_json.mid").toFile();
        
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        Track track = sequence.createTrack();
        
        addNoteToTrack(track, 0, 0, 60, 100, 480);
        addTempoToTrack(track, 0, 500000);
        addEndOfTrack(track, 480);
        
        MidiSystem.write(sequence, 1, midiFile);
        
        MidiParser parser = new MidiParser();
        String json = parser.parseToJson(midiFile, true, true);
        
        assertNotNull(json);
        assertTrue(json.contains("schemaVersion"));
        assertTrue(json.contains("file"));
        assertTrue(json.contains("metadata"));
        assertTrue(json.contains("tracks"));
        assertTrue(json.contains("C4"));
    }
    
    // Helper methods
    
    private void addNoteToTrack(Track track, int channel, long tick, int noteNumber, 
                               int velocity, long duration) throws InvalidMidiDataException {
        // Note on
        ShortMessage noteOn = new ShortMessage();
        noteOn.setMessage(ShortMessage.NOTE_ON, channel, noteNumber, velocity);
        track.add(new MidiEvent(noteOn, tick));
        
        // Note off
        ShortMessage noteOff = new ShortMessage();
        noteOff.setMessage(ShortMessage.NOTE_OFF, channel, noteNumber, 0);
        track.add(new MidiEvent(noteOff, tick + duration));
    }
    
    private void addTempoToTrack(Track track, long tick, int microsecondsPerQuarter) 
            throws InvalidMidiDataException {
        byte[] data = new byte[3];
        data[0] = (byte) ((microsecondsPerQuarter >> 16) & 0xFF);
        data[1] = (byte) ((microsecondsPerQuarter >> 8) & 0xFF);
        data[2] = (byte) (microsecondsPerQuarter & 0xFF);
        
        MetaMessage tempo = new MetaMessage();
        tempo.setMessage(0x51, data, 3);
        track.add(new MidiEvent(tempo, tick));
    }
    
    private void addEndOfTrack(Track track, long tick) throws InvalidMidiDataException {
        MetaMessage endOfTrack = new MetaMessage();
        endOfTrack.setMessage(0x2F, new byte[0], 0);
        track.add(new MidiEvent(endOfTrack, tick));
    }
}
