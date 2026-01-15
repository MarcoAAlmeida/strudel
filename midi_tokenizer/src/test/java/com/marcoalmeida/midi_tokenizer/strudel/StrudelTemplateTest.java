package com.marcoalmeida.midi_tokenizer.strudel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StrudelTemplateTest {

    @Test
    void testRender_BasicStructure() {
        String result = StrudelTemplate.render(
            "melody",
            "test.mid",
            120.0,
            4,        // beatsPerCycle
            0,        // trackIndex
            "Piano",  // trackName
            4,        // timeSignatureNumerator
            4,        // timeSignatureDenominator
            16,       // quantization
            "default", // quantizationSource
            "16 = sixteenth notes, @4 = quarter note, @8 = half note", // gridMeaning
            16,       // slicesPerMeasure
            "<[c4 d4 e4] [f4 g4 a4]>",
            "piano",
            true      // polyphonicMode
        );

        assertNotNull(result);
        assertTrue(result.contains("/* \"melody\" */"));
        assertTrue(result.contains("Source: test.mid"));
        assertTrue(result.contains("Tempo: 120 BPM"));
        assertTrue(result.contains("Time Signature: 4/4"));
        assertTrue(result.contains("Quantization: 16 (default)"));
        assertTrue(result.contains("Grid: 16 = sixteenth notes"));
        assertTrue(result.contains("Track: 0 (Piano)"));
        assertTrue(result.contains("setcpm(120/4)")); // bpm/beatsPerCycle
        assertTrue(result.contains("let melody = note(`<"));
        assertTrue(result.contains("`).sound(\"piano\")"));
        assertTrue(result.contains("melody.room(0.2)"));
    }

    @Test
    void testRender_CalculatesCpmCorrectly() {
        String result = StrudelTemplate.render(
            "test",
            "test.mid",
            140.0,
            4,
            0,
            "Track",
            4,
            4,
            12,
            "override",
            "12 = triplet eighths, @3 = quarter note, @6 = half note",
            12,
            "<[c4]>",
            "piano",
            true
        );

        assertTrue(result.contains("setcpm(140/4)")); // bpm/beatsPerCycle
        assertTrue(result.contains("Quantization: 12 (override)"));
    }

    @Test
    void testRender_IncludesConvertedDate() {
        String result = StrudelTemplate.render(
            "test",
            "test.mid",
            120.0,
            4,
            0,
            "Track",
            4,
            4,
            16,
            "default",
            "16 = sixteenth notes, @4 = quarter note, @8 = half note",
            16,
            "<[c4]>",
            "piano",
            true
        );

        assertTrue(result.contains("Converted: "));
        // Date should be in format YYYY-MM-DD
        assertTrue(result.matches("(?s).*Converted: \\d{4}-\\d{2}-\\d{2}.*"));
    }

    @Test
    void testRender_DifferentInstrument() {
        String result = StrudelTemplate.render(
            "bass",
            "bass.mid",
            90.0,
            4,
            0,
            "Bass Track",
            4,
            4,
            16,
            "default",
            "16 = sixteenth notes, @4 = quarter note, @8 = half note",
            16,
            "<[c2 e2 g2]>",
            "triangle",
            true
        );

        assertTrue(result.contains("let bass = note(`<"));
        assertTrue(result.contains("`).sound(\"triangle\")"));
        assertTrue(result.contains("bass.room(0.2)"));
    }

    @Test
    void testRender_CompleteFileStructure() {
        String result = StrudelTemplate.render(
            "pattern",
            "file.mid",
            120.0,
            4,
            0,
            "Track 1",
            4,
            4,
            16,
            "default",
            "16 = sixteenth notes, @4 = quarter note, @8 = half note",
            16,
            "<[c4 d4]>",
            "piano",
            true
        );

        // Check structure order
        int titleIndex = result.indexOf("/* \"pattern\" */");
        int sourceIndex = result.indexOf("Source: file.mid");
        int cpmIndex = result.indexOf("setcpm(");
        int letIndex = result.indexOf("let pattern");
        int roomIndex = result.indexOf("pattern.room(");

        assertTrue(titleIndex < sourceIndex);
        assertTrue(sourceIndex < cpmIndex);
        assertTrue(cpmIndex < letIndex);
        assertTrue(letIndex < roomIndex);
    }
}
