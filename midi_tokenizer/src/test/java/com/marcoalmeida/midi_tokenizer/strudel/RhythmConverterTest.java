package com.marcoalmeida.midi_tokenizer.strudel;

import com.marcoalmeida.midi_tokenizer.model.EventOutput;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RhythmConverterTest {

    /**
     * Phase 1.8: Tests for polyphonic conversion with integer durations and time-based positioning
     */

    @Test
    void testToQuantizedCyclePattern_SingleMeasure() {
        // 4 quarter notes in one measure (4/4 time, 120 BPM)
        // Each quarter = 0.5 seconds at 120 BPM
        List<EventOutput> events = Arrays.asList(
            createNoteEvent(60, 0, 480, 0.0, 0.5),      // C4 at beat 1
            createNoteEvent(62, 480, 480, 0.5, 0.5),    // D4 at beat 2
            createNoteEvent(64, 960, 480, 1.0, 0.5),    // E4 at beat 3
            createNoteEvent(65, 1440, 480, 1.5, 0.5)    // F4 at beat 4
        );

        String pattern = RhythmConverter.toQuantizedCyclePattern(events, 480, 4, 4, 16, 120, true, 1);
        
        // Should be wrapped in <>
        assertTrue(pattern.startsWith("<"));
        assertTrue(pattern.endsWith(">"));
        assertTrue(pattern.contains("c4"));
        assertTrue(pattern.contains("d4"));
        assertTrue(pattern.contains("e4"));
        assertTrue(pattern.contains("f4"));
    }

    @Test
    void testToQuantizedCyclePattern_Polyphony() {
        // Phase 1.8: Test simultaneous notes (chord)
        // C major chord at beat 1, all notes start at same time
        List<EventOutput> events = Arrays.asList(
            createNoteEvent(60, 0, 960, 0.0, 1.0),   // C4 - half note
            createNoteEvent(64, 0, 480, 0.0, 0.5),   // E4 - quarter note
            createNoteEvent(67, 0, 720, 0.0, 0.75)   // G4 - dotted quarter
        );

        String pattern = RhythmConverter.toQuantizedCyclePattern(events, 480, 4, 4, 16, 120, true, 1);
        
        // Should contain bracket notation for polyphony [note1,note2,note3]
        assertTrue(pattern.contains("[") && pattern.contains(","));
        assertTrue(pattern.contains("c4"));
        assertTrue(pattern.contains("e4"));
        assertTrue(pattern.contains("g4"));
    }

    @Test
    void testToQuantizedCyclePattern_IntegerDurations() {
        // Phase 1.8: Test integer durations
        // At 16-quantization, quarter note = 4 slices
        List<EventOutput> events = Arrays.asList(
            createNoteEvent(60, 0, 960, 0.0, 1.0)   // C4 half note = 8 slices
        );

        String pattern = RhythmConverter.toQuantizedCyclePattern(events, 480, 4, 4, 16, 120, true, 1);
        
        // Should have integer duration @8 (not @8.0 or @7.5)
        assertTrue(pattern.contains("c4@8"));
        assertFalse(pattern.contains("."));  // No decimal points
    }

    @Test
    void testToQuantizedCyclePattern_MinimumDuration() {
        // Phase 1.8: Very short note should round up to 1
        // 0.1 second note at 120 BPM, 16-quantization
        // sliceTimeSeconds = (60/120) * (4/16) = 0.125 seconds
        // 0.1 / 0.125 = 0.8 slices → rounds to 1
        List<EventOutput> events = Arrays.asList(
            createNoteEvent(60, 0, 50, 0.0, 0.1)   // Very short note
        );

        String pattern = RhythmConverter.toQuantizedCyclePattern(events, 480, 4, 4, 16, 120, true, 1);
        
        // Should appear in pattern (not dropped)
        assertTrue(pattern.contains("c4"));
        // Should not have duration annotation (duration = 1 is implicit)
        assertFalse(pattern.contains("c4@"));
    }

    @Test
    void testToQuantizedCyclePattern_EmptyMeasureCompact() {
        // Phase 1.8: Empty measure should use compact notation [~@16]
        // Note in measure 1, skip measure 2, note in measure 3
        List<EventOutput> events = Arrays.asList(
            createNoteEvent(60, 0, 480, 0.0, 0.5),        // Measure 1
            createNoteEvent(62, 3840, 480, 4.0, 0.5)      // Measure 3 (skip measure 2)
        );

        String pattern = RhythmConverter.toQuantizedCyclePattern(events, 480, 4, 4, 16, 120, true, 3);
        
        // Should contain compact rest notation
        assertTrue(pattern.contains("~@16"));
    }

    @Test
    void testToQuantizedCyclePattern_MultipleMeasures() {
        // Notes spanning two measures (4/4 time, 480 ticks/quarter, 120 BPM)
        List<EventOutput> events = Arrays.asList(
            createNoteEvent(60, 0, 480, 0.0, 0.5),        // Measure 1
            createNoteEvent(62, 480, 480, 0.5, 0.5),      // Measure 1
            createNoteEvent(64, 1920, 480, 2.0, 0.5),     // Measure 2
            createNoteEvent(65, 2400, 480, 2.5, 0.5)      // Measure 2
        );

        String pattern = RhythmConverter.toQuantizedCyclePattern(events, 480, 4, 4, 16, 120, true, 2);
        
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
            createNoteEvent(60, 0, 480, 0.0, 0.5),      // C4 at beat 1
            createNoteEvent(62, 960, 480, 1.0, 0.5)     // D4 at beat 3 (beat 2 is rest)
        );

        String pattern = RhythmConverter.toQuantizedCyclePattern(events, 480, 4, 4, 16, 120, true, 1);
        
        // Should contain a rest
        assertTrue(pattern.contains("~"));
    }

    @Test
    void testToQuantizedCyclePattern_EmptyList() {
        String pattern = RhythmConverter.toQuantizedCyclePattern(
            Collections.emptyList(), 480, 4, 4, 16, 120, true, 1
        );
        assertEquals("", pattern);
    }

    @Test
    void testToQuantizedCyclePattern_ThreeFourTime() {
        // 3/4 time signature - 3 quarter notes per measure
        // At 180 BPM, quarter note = 60/180 = 0.333 seconds
        List<EventOutput> events = Arrays.asList(
            createNoteEvent(60, 0, 480, 0.0, 0.333),      // Measure 1
            createNoteEvent(62, 480, 480, 0.333, 0.333),  // Measure 1
            createNoteEvent(64, 960, 480, 0.666, 0.333),  // Measure 1
            createNoteEvent(65, 1440, 480, 1.0, 0.333)    // Measure 2
        );

        String pattern = RhythmConverter.toQuantizedCyclePattern(events, 480, 3, 4, 6, 180, true, 2);
        
        // Should have two measures
        long openBrackets = pattern.chars().filter(ch -> ch == '[').count();
        assertEquals(2, openBrackets);
    }

    @Test
    void testToQuantizedCyclePattern_PolyWithDifferentDurations() {
        // Phase 1.8: Polyphonic notes with different integer durations
        // [c4@4,e4@2,g4@6] - C lasts 4 slices, E lasts 2, G lasts 6
        List<EventOutput> events = Arrays.asList(
            createNoteEvent(60, 0, 960, 0.0, 1.0),   // C4 - half note = 8 slices → rounds to 8
            createNoteEvent(64, 0, 480, 0.0, 0.5),   // E4 - quarter note = 4 slices → rounds to 4
            createNoteEvent(67, 0, 720, 0.0, 0.75)   // G4 - dotted quarter = 6 slices → rounds to 6
        );

        String pattern = RhythmConverter.toQuantizedCyclePattern(events, 480, 4, 4, 16, 120, true, 1);
        
        // Should have all three notes with different durations
        assertTrue(pattern.contains("c4@8"));
        assertTrue(pattern.contains("e4@4"));
        assertTrue(pattern.contains("g4@6"));
    }

    private EventOutput createNoteEvent(int noteNumber, long tick, long durationTicks, 
                                       double timeSeconds, double durationSeconds) {
        EventOutput event = new EventOutput();
        event.setType("note");
        event.setNoteNumber(noteNumber);
        event.setTick(tick);
        event.setVelocity(64);
        event.setDurationTicks(durationTicks);
        event.setTimeSeconds(timeSeconds);  // Phase 1.8: time-based positioning
        event.setDurationSeconds(durationSeconds);  // Phase 1.8: time-based duration
        return event;
    }
}
