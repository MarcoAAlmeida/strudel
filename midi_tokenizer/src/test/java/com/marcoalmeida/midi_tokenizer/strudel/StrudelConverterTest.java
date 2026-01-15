package com.marcoalmeida.midi_tokenizer.strudel;

import com.marcoalmeida.midi_tokenizer.midi.MidiParser;
import com.marcoalmeida.midi_tokenizer.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class StrudelConverterTest {

    private StrudelConverter converter;
    private MidiParser midiParser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        midiParser = new MidiParser();
        converter = new StrudelConverter(midiParser);
    }

    @Test
    void testConvert_SimpleMelody() throws Exception {
        // Create a simple MIDI file with a C major scale
        File midiFile = createTestMidiFile("test.mid", new int[]{60, 62, 64, 65, 67});

        ConversionOptions options = ConversionOptions.defaults();
        String result = converter.convert(midiFile.getAbsolutePath(), options);

        assertNotNull(result);
        // Check for individual notes (may be in brackets with @ notation)
        assertTrue(result.contains("c4"), "Missing c4");
        assertTrue(result.contains("d4"), "Missing d4");
        assertTrue(result.contains("e4"), "Missing e4");
        assertTrue(result.contains("f4"), "Missing f4");
        assertTrue(result.contains("g4"), "Missing g4");
        // Pattern name comes from track name, which is "Piano" -> "track_0"
        assertTrue(result.contains("/* \"track_0\" */") || result.contains("/* \"test\" */"));
        assertTrue(result.contains("Source: test.mid"));
        assertTrue(result.contains("setcpm("));
        assertTrue(result.contains("let track_0 = note(`<") || result.contains("let test = note(`<"));  // Template uses backticks and angle brackets
        assertTrue(result.contains(".room(0.2)"));
    }

    @Test
    void testConvert_WithTempoOverride() throws Exception {
        File midiFile = createTestMidiFile("tempo.mid", new int[]{60, 64, 67});

        ConversionOptions options = new ConversionOptions(140, 0, null, true);
        String result = converter.convert(midiFile.getAbsolutePath(), options);

        assertTrue(result.contains("setcpm(140/")); // bpm/beatsPerCycle format
        assertTrue(result.contains("Tempo: 140 BPM"));
    }

    @Test
    void testConvert_InvalidTrackIndex() throws Exception {
        File midiFile = createTestMidiFile("test.mid", new int[]{60});

        ConversionOptions options = new ConversionOptions(null, 5, null, true);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> converter.convert(midiFile.getAbsolutePath(), options)
        );
        
        assertTrue(exception.getMessage().contains("Track index 5 out of bounds"));
    }

    @Test
    void testConvert_EmptyTrack() throws Exception {
        File midiFile = createTestMidiFile("empty.mid", new int[]{});

        ConversionOptions options = ConversionOptions.defaults();
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> converter.convert(midiFile.getAbsolutePath(), options)
        );
        
        assertTrue(exception.getMessage().contains("has no note events"));
    }

    @Test
    void testConvert_PatternNameFromFilename() throws Exception {
        File midiFile = createTestMidiFile("my-song.mid", new int[]{60, 64});

        ConversionOptions options = ConversionOptions.defaults();
        String result = converter.convert(midiFile.getAbsolutePath(), options);

        // Pattern name comes from track name (track_0) not filename
        assertTrue(result.contains("/* \"track_0\" */"));
        assertTrue(result.contains("let track_0 = note(`<"));
        assertTrue(result.contains("track_0.room(0.2)"));
    }

    @Test
    void testConvert_WithSharps() throws Exception {
        File midiFile = createTestMidiFile("sharps.mid", new int[]{61, 63, 66});

        ConversionOptions options = ConversionOptions.defaults();
        String result = converter.convert(midiFile.getAbsolutePath(), options);

        // Check for individual sharp notes (may be in brackets with @ notation)
        assertTrue(result.contains("c#4"));
        assertTrue(result.contains("d#4"));
        assertTrue(result.contains("f#4"));
    }

    // Phase 2: Multi-track tests

    @Test
    void testConvert_AllTracks() throws Exception {
        // Create MIDI file with 3 tracks
        File midiFile = createMultiTrackMidiFile("multitrack.mid");

        // Convert all tracks (trackIndex = null)
        ConversionOptions options = new ConversionOptions(null, null, null, true);
        String result = converter.convert(midiFile.getAbsolutePath(), options);

        assertNotNull(result);
        // Should have track definitions
        assertTrue(result.contains("let track0 ="), "Missing track0");
        assertTrue(result.contains("let track1 ="), "Missing track1");
        assertTrue(result.contains("let track2 ="), "Missing track2");
        
        // Should have stack call
        assertTrue(result.contains("stack(track0, track1, track2)"), "Missing stack call");
        
        // Should show track count in metadata
        assertTrue(result.contains("Tracks: 3 non-empty"), "Missing track count");
        
        // Should have instruments
        assertTrue(result.contains("sound(\"piano\")"), "Missing piano");
        assertTrue(result.contains("sound(\"gm_acoustic_bass\")"), "Missing bass");
    }

    @Test
    void testConvert_SpecificTrackStillWorks() throws Exception {
        // Create MIDI file with 3 tracks
        File midiFile = createMultiTrackMidiFile("multitrack.mid");

        // Convert only track 1 (old behavior still works)
        ConversionOptions options = new ConversionOptions(null, 1, null, true);
        String result = converter.convert(midiFile.getAbsolutePath(), options);

        assertNotNull(result);
        // Should have single track pattern (Phase 1.9 format)
        assertTrue(result.contains("let track_1 = note(`<"), "Missing single track pattern");
        
        // Should NOT have stack call
        assertFalse(result.contains("stack("), "Should not have stack call for single track");
        
        // Should show single track in metadata
        assertTrue(result.contains("Track: 1"), "Missing track number");
    }

    @Test
    void testConvert_AllEmptyTracksError() throws Exception {
        // Create MIDI file with all empty tracks
        File midiFile = createEmptyTracksMidiFile("empty-tracks.mid");

        ConversionOptions options = new ConversionOptions(null, null, null, true);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> converter.convert(midiFile.getAbsolutePath(), options)
        );
        
        assertTrue(exception.getMessage().contains("No tracks with note events found"));
        assertTrue(exception.getMessage().contains("all are empty"));
    }

    @Test
    void testConvert_SparseTrackIndices() throws Exception {
        // Create MIDI file with tracks 0, 2, 5 non-empty (sparse indices)
        File midiFile = createSparseTrackMidiFile("sparse.mid");

        ConversionOptions options = new ConversionOptions(null, null, null, true);
        String result = converter.convert(midiFile.getAbsolutePath(), options);

        assertNotNull(result);
        // Should preserve original track indices
        assertTrue(result.contains("let track0 ="), "Missing track0");
        assertTrue(result.contains("let track2 ="), "Missing track2");
        assertTrue(result.contains("let track5 ="), "Missing track5");
        
        // Should NOT have track1, track3, track4
        assertFalse(result.contains("let track1 ="), "Should not have track1");
        assertFalse(result.contains("let track3 ="), "Should not have track3");
        assertFalse(result.contains("let track4 ="), "Should not have track4");
        
        // Stack call should use correct indices
        assertTrue(result.contains("stack(track0, track2, track5)"), "Missing correct stack call");
        
        // Metadata should show sparse indices
        assertTrue(result.contains("(0, 2, 5 from 6 total)"), "Missing sparse track info");
    }

    @Test
    void testConvert_InstrumentMapping() throws Exception {
        // Create MIDI file with different instruments
        File midiFile = createInstrumentMidiFile("instruments.mid");

        ConversionOptions options = new ConversionOptions(null, null, null, true);
        String result = converter.convert(midiFile.getAbsolutePath(), options);

        assertNotNull(result);
        // Check that different instruments are mapped
        assertTrue(result.contains("sound(\"piano\")"), "Missing piano (program 0)");
        assertTrue(result.contains("sound(\"gm_violin\")"), "Missing violin (program 40)");
        assertTrue(result.contains("sound(\"gm_flute\")"), "Missing flute (program 73)");
    }

    /**
     * Creates a simple test MIDI file with the given notes.
     */
    private File createTestMidiFile(String filename, int[] noteNumbers) throws Exception {
        File file = tempDir.resolve(filename).toFile();
        
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        Track track = sequence.createTrack();

        // Add tempo meta event (120 BPM)
        int tempo = 500000; // microseconds per quarter note (120 BPM)
        MetaMessage tempoMessage = new MetaMessage();
        byte[] tempoData = new byte[]{
            (byte) ((tempo >> 16) & 0xFF),
            (byte) ((tempo >> 8) & 0xFF),
            (byte) (tempo & 0xFF)
        };
        tempoMessage.setMessage(0x51, tempoData, 3);
        track.add(new MidiEvent(tempoMessage, 0));

        // Add track name
        MetaMessage trackName = new MetaMessage();
        trackName.setMessage(0x03, "Piano".getBytes(), "Piano".length());
        track.add(new MidiEvent(trackName, 0));

        // Add program change (piano = 0)
        ShortMessage programChange = new ShortMessage();
        programChange.setMessage(ShortMessage.PROGRAM_CHANGE, 0, 0, 0);
        track.add(new MidiEvent(programChange, 0));

        // Add notes
        long tick = 0;
        for (int noteNumber : noteNumbers) {
            // Note on
            ShortMessage noteOn = new ShortMessage();
            noteOn.setMessage(ShortMessage.NOTE_ON, 0, noteNumber, 64);
            track.add(new MidiEvent(noteOn, tick));

            // Note off
            ShortMessage noteOff = new ShortMessage();
            noteOff.setMessage(ShortMessage.NOTE_OFF, 0, noteNumber, 0);
            track.add(new MidiEvent(noteOff, tick + 480));

            tick += 480; // Move to next quarter note
        }

        // Write to file
        MidiSystem.write(sequence, 1, file);
        
        return file;
    }

    /**
     * Creates a multi-track MIDI file with 3 tracks.
     */
    private File createMultiTrackMidiFile(String filename) throws Exception {
        File file = tempDir.resolve(filename).toFile();
        
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        
        // Track 0: Piano (C major chord)
        Track track0 = sequence.createTrack();
        addTempoAndTimeSignature(track0);
        addTrackName(track0, "Piano");
        addProgramChange(track0, 0, 0);  // Piano
        addNote(track0, 0, 60, 0, 480);   // C4
        addNote(track0, 0, 64, 0, 480);   // E4
        addNote(track0, 0, 67, 0, 480);   // G4
        
        // Track 1: Bass (Root notes)
        Track track1 = sequence.createTrack();
        addTrackName(track1, "Bass");
        addProgramChange(track1, 0, 32);  // Acoustic Bass
        addNote(track1, 0, 36, 0, 480);   // C2
        addNote(track1, 0, 40, 480, 960); // E2
        
        // Track 2: Melody (Simple melody)
        Track track2 = sequence.createTrack();
        addTrackName(track2, "Melody");
        addProgramChange(track2, 0, 73);  // Flute
        addNote(track2, 0, 72, 0, 240);   // C5
        addNote(track2, 0, 74, 240, 480); // D5
        addNote(track2, 0, 76, 480, 720); // E5
        
        MidiSystem.write(sequence, 1, file);
        return file;
    }

    /**
     * Creates a MIDI file with all empty tracks.
     */
    private File createEmptyTracksMidiFile(String filename) throws Exception {
        File file = tempDir.resolve(filename).toFile();
        
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        
        // Create 3 empty tracks (no notes)
        for (int i = 0; i < 3; i++) {
            Track track = sequence.createTrack();
            if (i == 0) {
                addTempoAndTimeSignature(track);
            }
            addTrackName(track, "Track" + i);
            addProgramChange(track, 0, 0);
        }
        
        MidiSystem.write(sequence, 1, file);
        return file;
    }

    /**
     * Creates a MIDI file with sparse track indices (0, 2, 5 non-empty).
     */
    private File createSparseTrackMidiFile(String filename) throws Exception {
        File file = tempDir.resolve(filename).toFile();
        
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        
        // Track 0: Has notes
        Track track0 = sequence.createTrack();
        addTempoAndTimeSignature(track0);
        addTrackName(track0, "Piano");
        addProgramChange(track0, 0, 0);
        addNote(track0, 0, 60, 0, 480);
        
        // Track 1: Empty
        Track track1 = sequence.createTrack();
        addTrackName(track1, "Empty1");
        addProgramChange(track1, 0, 0);
        
        // Track 2: Has notes
        Track track2 = sequence.createTrack();
        addTrackName(track2, "Bass");
        addProgramChange(track2, 0, 32);
        addNote(track2, 0, 36, 0, 480);
        
        // Track 3: Empty
        Track track3 = sequence.createTrack();
        addTrackName(track3, "Empty2");
        addProgramChange(track3, 0, 0);
        
        // Track 4: Empty
        Track track4 = sequence.createTrack();
        addTrackName(track4, "Empty3");
        addProgramChange(track4, 0, 0);
        
        // Track 5: Has notes
        Track track5 = sequence.createTrack();
        addTrackName(track5, "Strings");
        addProgramChange(track5, 0, 48);
        addNote(track5, 0, 67, 0, 480);
        
        MidiSystem.write(sequence, 1, file);
        return file;
    }

    /**
     * Creates a MIDI file with different instruments.
     */
    private File createInstrumentMidiFile(String filename) throws Exception {
        File file = tempDir.resolve(filename).toFile();
        
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        
        // Track 0: Piano (program 0)
        Track track0 = sequence.createTrack();
        addTempoAndTimeSignature(track0);
        addTrackName(track0, "Piano");
        addProgramChange(track0, 0, 0);
        addNote(track0, 0, 60, 0, 480);
        
        // Track 1: Violin (program 40)
        Track track1 = sequence.createTrack();
        addTrackName(track1, "Violin");
        addProgramChange(track1, 0, 40);
        addNote(track1, 0, 64, 0, 480);
        
        // Track 2: Flute (program 73)
        Track track2 = sequence.createTrack();
        addTrackName(track2, "Flute");
        addProgramChange(track2, 0, 73);
        addNote(track2, 0, 72, 0, 480);
        
        MidiSystem.write(sequence, 1, file);
        return file;
    }

    // Helper methods for creating MIDI events

    private void addTempoAndTimeSignature(Track track) throws InvalidMidiDataException {
        // Add tempo (120 BPM)
        int tempo = 500000;
        MetaMessage tempoMessage = new MetaMessage();
        byte[] tempoData = new byte[]{
            (byte) ((tempo >> 16) & 0xFF),
            (byte) ((tempo >> 8) & 0xFF),
            (byte) (tempo & 0xFF)
        };
        tempoMessage.setMessage(0x51, tempoData, 3);
        track.add(new MidiEvent(tempoMessage, 0));

        // Add time signature (4/4)
        MetaMessage timeSigMessage = new MetaMessage();
        byte[] timeSigData = new byte[]{4, 2, 24, 8};  // 4/4 time
        timeSigMessage.setMessage(0x58, timeSigData, 4);
        track.add(new MidiEvent(timeSigMessage, 0));
    }

    private void addTrackName(Track track, String name) throws InvalidMidiDataException {
        MetaMessage trackName = new MetaMessage();
        trackName.setMessage(0x03, name.getBytes(), name.length());
        track.add(new MidiEvent(trackName, 0));
    }

    private void addProgramChange(Track track, int channel, int program) throws InvalidMidiDataException {
        ShortMessage programChange = new ShortMessage();
        programChange.setMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0);
        track.add(new MidiEvent(programChange, 0));
    }

    private void addNote(Track track, int channel, int note, long startTick, long endTick) throws InvalidMidiDataException {
        // Note on
        ShortMessage noteOn = new ShortMessage();
        noteOn.setMessage(ShortMessage.NOTE_ON, channel, note, 64);
        track.add(new MidiEvent(noteOn, startTick));

        // Note off
        ShortMessage noteOff = new ShortMessage();
        noteOff.setMessage(ShortMessage.NOTE_OFF, channel, note, 0);
        track.add(new MidiEvent(noteOff, endTick));
    }
}
