package com.marcoalmeida.midi_tokenizer.util;

/**
 * Utility class for MIDI note number to scientific pitch notation conversion.
 * Uses the convention where middle C (MIDI note 60) = C4.
 */
public class NoteUtils {
    
    private static final String[] NOTE_NAMES = {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    /**
     * Converts a MIDI note number to scientific pitch notation.
     * Middle C (60) = C4.
     * 
     * @param noteNumber MIDI note number (0-127)
     * @return Scientific pitch notation (e.g., "C4", "A#5")
     */
    public static String noteNumberToName(int noteNumber) {
        if (noteNumber < 0 || noteNumber > 127) {
            return "Unknown";
        }
        
        int octave = (noteNumber / 12) - 1;
        int pitchClass = noteNumber % 12;
        
        return NOTE_NAMES[pitchClass] + octave;
    }

    /**
     * Gets the pitch class name without octave.
     * 
     * @param noteNumber MIDI note number (0-127)
     * @return Pitch class name (e.g., "C", "A#")
     */
    public static String getPitchClassName(int noteNumber) {
        if (noteNumber < 0 || noteNumber > 127) {
            return "Unknown";
        }
        return NOTE_NAMES[noteNumber % 12];
    }
}
