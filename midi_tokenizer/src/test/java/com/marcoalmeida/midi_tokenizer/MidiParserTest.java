package com.marcoalmeida.midi_tokenizer;

import com.marcoalmeida.midi_tokenizer.midi.MidiParser;
import com.marcoalmeida.midi_tokenizer.model.MidiFile;
import com.marcoalmeida.midi_tokenizer.model.Note;
import com.marcoalmeida.midi_tokenizer.model.Track;
import com.marcoalmeida.midi_tokenizer.util.NoteUtils;
import org.junit.jupiter.api.Test;

import javax.sound.midi.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MIDI parsing functionality.
 */
class MidiParserTest {

    private final MidiParser parser = new MidiParser();

    @Test
    void testNoteUtils() {
        // Test MIDI 60 = C4 (middle C)
        assertEquals("C4", NoteUtils.midiNoteToName(60));
        assertEquals("A4", NoteUtils.midiNoteToName(69));
        assertEquals("C#5", NoteUtils.midiNoteToName(73));
        assertEquals("A0", NoteUtils.midiNoteToName(21));
        assertEquals("C8", NoteUtils.midiNoteToName(108));
        
        // Test individual components
        assertEquals("C", NoteUtils.getNoteName(60));
        assertEquals("A", NoteUtils.getNoteName(69));
        assertEquals(4, NoteUtils.getOctave(60));
        assertEquals(4, NoteUtils.getOctave(69));
    }

    @Test
    void testSimpleSequenceParsing() throws Exception {
        // Create a simple MIDI sequence programmatically
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        javax.sound.midi.Track track = sequence.createTrack();

        // Set tempo to 120 BPM (500000 microseconds per quarter note)
        addTempoEvent(track, 0, 500000);

        // Add a C4 (middle C) note at tick 0, duration 480 ticks (1 quarter note)
        addNoteOnOff(track, 0, 60, 100, 0, 480);

        // Add an E4 note at tick 480, duration 480 ticks
        addNoteOnOff(track, 0, 64, 100, 480, 960);

        // Add a G4 note at tick 960, duration 480 ticks
        addNoteOnOff(track, 0, 67, 100, 960, 1440);

        // Parse the sequence
        MidiFile midiFile = parser.parseSequence(sequence);

        // Verify basic structure
        assertEquals(0, midiFile.getFormat()); // Single track = format 0
        assertEquals(480, midiFile.getDivision());
        assertEquals(1, midiFile.getTracks().size());

        // Verify tempo map
        assertEquals(1, midiFile.getTempoMap().getChanges().size());
        assertEquals(500000, midiFile.getTempoMap().getChanges().get(0).getMicrosecondsPerQuarterNote());

        // Verify notes
        Track parsedTrack = midiFile.getTracks().get(0);
        assertEquals(3, parsedTrack.getNotes().size());

        // Check first note (C4)
        Note note1 = parsedTrack.getNotes().get(0);
        assertEquals(60, note1.getMidiNote());
        assertEquals("C4", note1.getNoteName());
        assertEquals(100, note1.getVelocity());
        assertEquals(0.0, note1.getStartTime(), 0.001);
        assertEquals(0.5, note1.getDuration(), 0.001); // 480 ticks at 120 BPM = 0.5 seconds

        // Check second note (E4)
        Note note2 = parsedTrack.getNotes().get(1);
        assertEquals(64, note2.getMidiNote());
        assertEquals("E4", note2.getNoteName());
        assertEquals(0.5, note2.getStartTime(), 0.001);

        // Check third note (G4)
        Note note3 = parsedTrack.getNotes().get(2);
        assertEquals(67, note3.getMidiNote());
        assertEquals("G4", note3.getNoteName());
        assertEquals(1.0, note3.getStartTime(), 0.001);
    }

    @Test
    void testTempoChange() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        javax.sound.midi.Track track = sequence.createTrack();

        // Start with 120 BPM
        addTempoEvent(track, 0, 500000);

        // Add a note at 120 BPM
        addNoteOnOff(track, 0, 60, 100, 0, 480);

        // Change to 60 BPM (double the microseconds per quarter note)
        addTempoEvent(track, 480, 1000000);

        // Add another note at 60 BPM
        addNoteOnOff(track, 0, 64, 100, 480, 960);

        MidiFile midiFile = parser.parseSequence(sequence);

        // Verify tempo map has 2 changes
        assertEquals(2, midiFile.getTempoMap().getChanges().size());

        Track parsedTrack = midiFile.getTracks().get(0);
        assertEquals(2, parsedTrack.getNotes().size());

        // First note: 480 ticks at 120 BPM = 0.5 seconds
        Note note1 = parsedTrack.getNotes().get(0);
        assertEquals(0.5, note1.getDuration(), 0.001);

        // Second note: 480 ticks at 60 BPM = 1.0 second
        Note note2 = parsedTrack.getNotes().get(1);
        assertEquals(1.0, note2.getDuration(), 0.001);
        assertEquals(0.5, note2.getStartTime(), 0.001);
    }

    @Test
    void testNoteVelocityZeroAsNoteOff() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        javax.sound.midi.Track track = sequence.createTrack();

        addTempoEvent(track, 0, 500000);

        // Use Note On with velocity 0 as Note Off (common MIDI practice)
        ShortMessage noteOn = new ShortMessage();
        noteOn.setMessage(ShortMessage.NOTE_ON, 0, 60, 100);
        track.add(new MidiEvent(noteOn, 0));

        ShortMessage noteOffAsOn = new ShortMessage();
        noteOffAsOn.setMessage(ShortMessage.NOTE_ON, 0, 60, 0); // velocity 0
        track.add(new MidiEvent(noteOffAsOn, 480));

        MidiFile midiFile = parser.parseSequence(sequence);
        Track parsedTrack = midiFile.getTracks().get(0);

        assertEquals(1, parsedTrack.getNotes().size());
        Note note = parsedTrack.getNotes().get(0);
        assertEquals(0.5, note.getDuration(), 0.001);
    }

    /**
     * Helper to add a tempo event.
     */
    private void addTempoEvent(javax.sound.midi.Track track, long tick, int microsecondsPerQuarterNote) throws Exception {
        MetaMessage meta = new MetaMessage();
        byte[] data = new byte[3];
        data[0] = (byte) ((microsecondsPerQuarterNote >> 16) & 0xFF);
        data[1] = (byte) ((microsecondsPerQuarterNote >> 8) & 0xFF);
        data[2] = (byte) (microsecondsPerQuarterNote & 0xFF);
        meta.setMessage(0x51, data, 3);
        track.add(new MidiEvent(meta, tick));
    }

    /**
     * Helper to add Note On and Note Off events.
     */
    private void addNoteOnOff(javax.sound.midi.Track track, int channel, int note, int velocity, 
                               long onTick, long offTick) throws Exception {
        ShortMessage noteOn = new ShortMessage();
        noteOn.setMessage(ShortMessage.NOTE_ON, channel, note, velocity);
        track.add(new MidiEvent(noteOn, onTick));

        ShortMessage noteOff = new ShortMessage();
        noteOff.setMessage(ShortMessage.NOTE_OFF, channel, note, 0);
        track.add(new MidiEvent(noteOff, offTick));
    }
}
