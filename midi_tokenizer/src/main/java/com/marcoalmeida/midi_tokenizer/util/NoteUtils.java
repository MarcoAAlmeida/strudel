package com.marcoalmeida.midi_tokenizer.util;

/**
 * Utility class for converting MIDI note numbers to scientific pitch notation.
 * Uses the standard where MIDI note 60 = C4 (middle C).
 */
public class NoteUtils {
    
    private static final String[] NOTE_NAMES = {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    /**
     * Convert a MIDI note number (0-127) to scientific pitch notation.
     * MIDI note 60 = C4 (middle C).
     * 
     * @param midiNote MIDI note number (0-127)
     * @return Scientific pitch notation (e.g., "C4", "A#5")
     * @throws IllegalArgumentException if midiNote is out of range
     */
    public static String midiNoteToName(int midiNote) {
        if (midiNote < 0 || midiNote > 127) {
            throw new IllegalArgumentException("MIDI note must be between 0 and 127");
        }
        
        int octave = (midiNote / 12) - 1;
        int noteIndex = midiNote % 12;
        
        return NOTE_NAMES[noteIndex] + octave;
    }

    /**
     * Get the note name without octave.
     * 
     * @param midiNote MIDI note number (0-127)
     * @return Note name (e.g., "C", "A#")
     */
    public static String getNoteName(int midiNote) {
        if (midiNote < 0 || midiNote > 127) {
            throw new IllegalArgumentException("MIDI note must be between 0 and 127");
        }
        return NOTE_NAMES[midiNote % 12];
    }

    /**
     * Get the octave number.
     * 
     * @param midiNote MIDI note number (0-127)
     * @return Octave number
     */
    public static int getOctave(int midiNote) {
        if (midiNote < 0 || midiNote > 127) {
            throw new IllegalArgumentException("MIDI note must be between 0 and 127");
        }
        return (midiNote / 12) - 1;
    }
}
