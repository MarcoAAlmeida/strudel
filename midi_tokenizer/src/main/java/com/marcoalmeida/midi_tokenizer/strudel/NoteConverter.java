package com.marcoalmeida.midi_tokenizer.strudel;

import com.marcoalmeida.midi_tokenizer.midi.NoteUtils;

/**
 * Converts MIDI note numbers to Strudel note format (lowercase).
 */
public class NoteConverter {

    /**
     * Converts a MIDI note number to Strudel note name format.
     * Strudel uses lowercase note names (e.g., "c4", "d#5").
     *
     * @param noteNumber MIDI note number (0-127)
     * @return Strudel-formatted note name in lowercase
     */
    public static String toStrudelNoteName(int noteNumber) {
        return NoteUtils.noteNumberToName(noteNumber).toLowerCase();
    }
}
