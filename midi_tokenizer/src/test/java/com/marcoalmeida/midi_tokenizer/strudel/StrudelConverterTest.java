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

        ConversionOptions options = new ConversionOptions(140, 0, 16);
        String result = converter.convert(midiFile.getAbsolutePath(), options);

        assertTrue(result.contains("setcpm(140/")); // bpm/beatsPerCycle format
        assertTrue(result.contains("Tempo: 140 BPM"));
    }

    @Test
    void testConvert_InvalidTrackIndex() throws Exception {
        File midiFile = createTestMidiFile("test.mid", new int[]{60});

        ConversionOptions options = new ConversionOptions(null, 5, 16);
        
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
}
