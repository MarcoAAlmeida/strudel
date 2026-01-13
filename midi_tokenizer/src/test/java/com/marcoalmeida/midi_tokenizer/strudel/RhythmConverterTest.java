package com.marcoalmeida.midi_tokenizer.strudel;

import com.marcoalmeida.midi_tokenizer.model.EventOutput;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RhythmConverterTest {

    @Test
    void testToQuantizedCyclePattern_SingleMeasure() {
        // 4 quarter notes in one measure (4/4 time)
        List<EventOutput> events = Arrays.asList(
            createNoteEvent(60, 0, 480),      // C4 at beat 1
            createNoteEvent(62, 480, 480),    // D4 at beat 2
            createNoteEvent(64, 960, 480),    // E4 at beat 3
            createNoteEvent(65, 1440, 480)    // F4 at beat 4
        );

        String pattern = RhythmConverter.toQuantizedCyclePattern(events, 480, 4, 4);
        
        // Should be one bracketed measure
        assertTrue(pattern.startsWith("["));
        assertTrue(pattern.endsWith("]"));
        assertTrue(pattern.contains("c4"));
        assertTrue(pattern.contains("d4"));
        assertTrue(pattern.contains("e4"));
        assertTrue(pattern.contains("f4"));
    }

    @Test
    void testToQuantizedCyclePattern_MultipleMeasures() {
        // Notes spanning two measures (4/4 time, 480 ticks/quarter)
        // Measure 1: 0-1919, Measure 2: 1920-3839
        List<EventOutput> events = Arrays.asList(
            createNoteEvent(60, 0, 480),      // Measure 1
            createNoteEvent(62, 480, 480),    // Measure 1
            createNoteEvent(64, 1920, 480),   // Measure 2
            createNoteEvent(65, 2400, 480)    // Measure 2
        );

        String pattern = RhythmConverter.toQuantizedCyclePattern(events, 480, 4, 4);
        
        // Should have two bracketed measures
        long openBrackets = pattern.chars().filter(ch -> ch == '[').count();
        long closeBrackets = pattern.chars().filter(ch -> ch == ']').count();
        assertEquals(2, openBrackets);
        assertEquals(2, closeBrackets);
    }

    @Test
    void testToQuantizedCyclePattern_WithRests() {
        // Notes with gaps (rests)
        List<EventOutput> events = Arrays.asList(
            createNoteEvent(60, 0, 480),      // C4 at beat 1
            createNoteEvent(62, 960, 480)     // D4 at beat 3 (beat 2 is rest)
        );

        String pattern = RhythmConverter.toQuantizedCyclePattern(events, 480, 4, 4);
        
        // Should contain a rest
        assertTrue(pattern.contains("~"));
    }

    @Test
    void testToQuantizedCyclePattern_WithDurations() {
        // Half note (2 quarters = 960 ticks) with 8-grid quantization
        // 4/4 measure = 8 slices, half note = 4 slices
        List<EventOutput> events = Arrays.asList(
            createNoteEvent(60, 0, 960)  // C4 half note
        );

        String pattern = RhythmConverter.toQuantizedCyclePattern(events, 480, 4, 4);
        
        // Should have duration notation @4 (4 slices in 8-grid)
        assertTrue(pattern.contains("c4@4"));
    }

    @Test
    void testToQuantizedCyclePattern_EmptyList() {
        String pattern = RhythmConverter.toQuantizedCyclePattern(
            Collections.emptyList(), 480, 4, 4
        );
        assertEquals("", pattern);
    }

    @Test
    void testToQuantizedCyclePattern_ThreeFourTime() {
        // 3/4 time signature - 3 quarter notes per measure
        // Measure length = 480 * 3 = 1440 ticks
        List<EventOutput> events = Arrays.asList(
            createNoteEvent(60, 0, 480),      // Measure 1
            createNoteEvent(62, 480, 480),    // Measure 1
            createNoteEvent(64, 960, 480),    // Measure 1
            createNoteEvent(65, 1440, 480)    // Measure 2
        );

        String pattern = RhythmConverter.toQuantizedCyclePattern(events, 480, 3, 4);
        
        // Should have two measures
        long openBrackets = pattern.chars().filter(ch -> ch == '[').count();
        assertEquals(2, openBrackets);
    }

    private EventOutput createNoteEvent(int noteNumber, long tick, long durationTicks) {
        EventOutput event = new EventOutput();
        event.setType("note");
        event.setNoteNumber(noteNumber);
        event.setTick(tick);
        event.setVelocity(64);
        event.setDurationTicks(durationTicks);
        return event;
    }
}
