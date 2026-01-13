package com.marcoalmeida.midi_tokenizer.strudel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoteConverterTest {

    @Test
    void testToStrudelNoteName_MiddleC() {
        assertEquals("c4", NoteConverter.toStrudelNoteName(60));
    }

    @Test
    void testToStrudelNoteName_CSharp() {
        assertEquals("c#5", NoteConverter.toStrudelNoteName(73));
    }

    @Test
    void testToStrudelNoteName_LowNote() {
        assertEquals("c0", NoteConverter.toStrudelNoteName(12));
    }

    @Test
    void testToStrudelNoteName_HighNote() {
        assertEquals("g9", NoteConverter.toStrudelNoteName(127));
    }

    @Test
    void testToStrudelNoteName_AllLowercase() {
        String result = NoteConverter.toStrudelNoteName(61); // C#4
        assertTrue(result.equals(result.toLowerCase()));
    }
}
