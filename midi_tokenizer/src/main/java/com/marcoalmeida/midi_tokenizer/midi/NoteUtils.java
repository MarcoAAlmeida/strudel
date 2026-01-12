package com.marcoalmeida.midi_tokenizer.midi;

/**
 * Utility class for converting MIDI note numbers to scientific pitch notation.
 * Uses the A440 standard and 12-tone equal temperament (12-TET) mapping.
 */
public class NoteUtils {
    
    private static final String[] NOTE_NAMES = {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };
    
    /**
     * Converts a MIDI note number to scientific pitch notation.
     * Middle C (MIDI note 60) is represented as C4.
     * 
     * @param noteNumber MIDI note number (0-127)
     * @return Scientific pitch notation (e.g., "C4", "A#5")
     * @throws IllegalArgumentException if noteNumber is not in range 0-127
     */
    public static String noteNumberToName(int noteNumber) {
        if (noteNumber < 0 || noteNumber > 127) {
            throw new IllegalArgumentException("MIDI note number must be between 0 and 127, got: " + noteNumber);
        }
        
        int octave = (noteNumber / 12) - 1;
        int noteIndex = noteNumber % 12;
        
        return NOTE_NAMES[noteIndex] + octave;
    }
    
    /**
     * Gets the note name without octave (e.g., "C#", "A").
     * 
     * @param noteNumber MIDI note number (0-127)
     * @return Note name without octave
     * @throws IllegalArgumentException if noteNumber is not in range 0-127
     */
    public static String getNoteName(int noteNumber) {
        if (noteNumber < 0 || noteNumber > 127) {
            throw new IllegalArgumentException("MIDI note number must be between 0 and 127, got: " + noteNumber);
        }
        
        return NOTE_NAMES[noteNumber % 12];
    }
    
    /**
     * Gets the octave number for a MIDI note.
     * 
     * @param noteNumber MIDI note number (0-127)
     * @return Octave number
     * @throws IllegalArgumentException if noteNumber is not in range 0-127
     */
    public static int getOctave(int noteNumber) {
        if (noteNumber < 0 || noteNumber > 127) {
            throw new IllegalArgumentException("MIDI note number must be between 0 and 127, got: " + noteNumber);
        }
        
        return (noteNumber / 12) - 1;
    }
}
