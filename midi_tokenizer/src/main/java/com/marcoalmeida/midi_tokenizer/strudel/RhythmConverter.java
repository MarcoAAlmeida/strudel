package com.marcoalmeida.midi_tokenizer.strudel;

import com.marcoalmeida.midi_tokenizer.model.EventOutput;

import java.util.*;

/**
 * Converts MIDI note events to Strudel pattern strings with polyphonic support.
 * Phase 1.9: Dual-mode conversion - polyphonic (default) or non-polyphonic (--no-polyphony).
 */
public class RhythmConverter {

    /**
     * Internal class to hold note with its duration.
     */
    private static class NoteWithDuration {
        final String noteName;
        final int duration;  // Integer slices

        NoteWithDuration(String noteName, int duration) {
            this.noteName = noteName;
            this.duration = duration;
        }
    }

    /**
     * Converts MIDI note events to Strudel cycle pattern.
     * Supports both polyphonic and non-polyphonic modes.
     * 
     * @param noteEvents   MIDI note events with timeSeconds and durationSeconds
     * @param division     MIDI division (ticks per quarter note)
     * @param numerator    Time signature numerator
     * @param denominator  Time signature denominator
     * @param quantization Quantization level (slices per 4/4 measure)
     * @param tempo        Tempo in BPM
     * @param polyphonic   Enable polyphonic mode (true) or non-polyphonic (false)
     * @param totalMeasures Total number of measures to generate (for multi-track sync)
     * @return Strudel pattern string wrapped in <>
     */
    public static String toQuantizedCyclePattern(
        List<EventOutput> noteEvents,
        int division,
        int numerator,
        int denominator,
        int quantization,
        int tempo,
        boolean polyphonic,
        int totalMeasures
    ) {
        if (noteEvents.isEmpty()) {
            return "";
        }

        if (polyphonic) {
            return toPolyphonicPattern(noteEvents, division, numerator, denominator, quantization, tempo, totalMeasures);
        } else {
            return toNonPolyphonicPattern(noteEvents, division, numerator, denominator, quantization, tempo, totalMeasures);
        }
    }

    /**
     * Phase 1.8 algorithm: Polyphonic preservation with integer durations.
     * - Preserves ALL simultaneous notes as chords [c4,e4,g4]
     * - No conflict resolution, no merging
     * - Integer durations only
     * - Time-based positioning
     */
    private static String toPolyphonicPattern(
        List<EventOutput> noteEvents,
        int division,
        int numerator,
        int denominator,
        int quantization,
        int tempo,
        int totalMeasures
    ) {
        // Calculate grid parameters
        int slicesPerMeasure = (quantization * numerator) / denominator;
        
        // Calculate slice time in seconds
        // BPM = beats per minute, one beat = quarter note
        // sliceTimeSeconds = (60 / tempo) * (4 / quantization)
        double sliceTimeSeconds = (60.0 / tempo) * (4.0 / quantization);

        // Populate grid using Map<position, List<NoteWithDuration>>
        Map<Integer, List<NoteWithDuration>> grid = new HashMap<>();
        
        // Find maximum position for measure calculation
        int maxPosition = 0;
        
        for (EventOutput event : noteEvents) {
            // Calculate grid position using timeSeconds (more accurate than tick division)
            int gridPosition = (int) Math.round(event.getTimeSeconds() / sliceTimeSeconds);
            
            // Calculate duration in slices
            double durationInSlices = event.getDurationSeconds() / sliceTimeSeconds;
            int integerDuration = (int) Math.round(durationInSlices);
            
            // Minimum duration = 1 (always round up, never drop notes)
            if (integerDuration < 1) {
                integerDuration = 1;
            }
            
            String noteName = NoteConverter.toStrudelNoteName(event.getNoteNumber());
            
            // Add to grid (allow multiple notes at same position - polyphony)
            grid.computeIfAbsent(gridPosition, k -> new ArrayList<>())
                .add(new NoteWithDuration(noteName, integerDuration));
        }

        // Use totalMeasures parameter for multi-track synchronization
        int numMeasures = totalMeasures;

        // Build pattern measure by measure
        StringBuilder pattern = new StringBuilder();
        pattern.append("<");

        for (int measure = 0; measure < numMeasures; measure++) {
            int measureStart = measure * slicesPerMeasure;
            int measureEnd = measureStart + slicesPerMeasure;
            
// Check if entire measure is empty
            boolean isEmpty = true;
            for (int i = measureStart; i < measureEnd; i++) {
                if (grid.containsKey(i)) {
                    isEmpty = false;
                    break;
                }
            }
            
            if (isEmpty) {
                // Use compact notation for empty measure
                pattern.append("[~@").append(slicesPerMeasure).append("]");
            } else {
                pattern.append("[");
                int i = measureStart;
                while (i < measureEnd) {
                    if (grid.containsKey(i)) {
                        List<NoteWithDuration> notes = grid.get(i);
                        
                        // Format as chord if multiple notes
                        if (notes.size() > 1) {
                            pattern.append("[");
                            for (int j = 0; j < notes.size(); j++) {
                                NoteWithDuration note = notes.get(j);
                                pattern.append(note.noteName);
                                if (note.duration > 1) {
                                    pattern.append("@").append(note.duration);
                                }
                                if (j < notes.size() - 1) {
                                    pattern.append(",");  // No spaces in chord notation
                                }
                            }
                            pattern.append("]");
                        } else {
                            // Single note
                            NoteWithDuration note = notes.get(0);
                            pattern.append(note.noteName);
                            if (note.duration > 1) {
                                pattern.append("@").append(note.duration);
                            }
                        }
                        i++;
                    } else {
                        // Empty slot - count consecutive rests
                        int restCount = 1;
                        while (i + restCount < measureEnd && !grid.containsKey(i + restCount)) {
                            restCount++;
                        }
                        
                        if (restCount == 1) {
                            pattern.append("~");
                        } else {
                            pattern.append("~@").append(restCount);
                        }
                        i += restCount;
                    }
                    
                    if (i < measureEnd) {
                        pattern.append(" ");
                    }
                }
                pattern.append("]");
            }
            
            if (measure < numMeasures - 1) {
                pattern.append(" ");
            }
        }

        pattern.append(">");

        return pattern.toString();
    }

    /**
     * Phase 1.7 style algorithm: Non-polyphonic with time-based positioning.
     * - 50% occupancy rule: note must occupy >50% of slice
     * - Conflict resolution: longest duration wins
     * - Consecutive identical notes merged with @N notation
     * - Uses timeSeconds for positioning (Phase 1.8 improvement)
     * - Integer durations only (Phase 1.8 improvement)
     */
    private static String toNonPolyphonicPattern(
        List<EventOutput> noteEvents,
        int division,
        int numerator,
        int denominator,
        int quantization,
        int tempo,
        int totalMeasures
    ) {
        // Calculate grid parameters (same as polyphonic)
        int slicesPerMeasure = (quantization * numerator) / denominator;
        double sliceTimeSeconds = (60.0 / tempo) * (4.0 / quantization);
        
        // Use totalMeasures parameter for multi-track synchronization
        int numMeasures = totalMeasures;
        
        // Create grid to hold note names (single note per slice)
        String[] slices = new String[slicesPerMeasure * numMeasures];
        Arrays.fill(slices, "");
        
        // Place notes in grid using 50% occupancy rule + conflict resolution
        for (EventOutput event : noteEvents) {
            double noteStartTime = event.getTimeSeconds();
            double noteEndTime = noteStartTime + event.getDurationSeconds();
            String noteName = NoteConverter.toStrudelNoteName(event.getNoteNumber());
            
            // Calculate which slices this note occupies >50%
            int startSlice = (int) Math.round(noteStartTime / sliceTimeSeconds);
            int endSlice = (int) Math.round(noteEndTime / sliceTimeSeconds);
            
            for (int sliceIdx = startSlice; sliceIdx < endSlice && sliceIdx < slices.length; sliceIdx++) {
                // Calculate how much of this slice the note occupies
                double sliceStart = sliceIdx * sliceTimeSeconds;
                double sliceEnd = (sliceIdx + 1) * sliceTimeSeconds;
                
                // Calculate overlap
                double overlapStart = Math.max(noteStartTime, sliceStart);
                double overlapEnd = Math.min(noteEndTime, sliceEnd);
                double overlapDuration = overlapEnd - overlapStart;
                
                // 50% occupancy rule: note must occupy >50% of slice
                if (overlapDuration > sliceTimeSeconds / 2) {
                    // Conflict resolution: if slot already taken, longest duration wins
                    if (slices[sliceIdx].isEmpty()) {
                        slices[sliceIdx] = noteName;
                    } else {
                        // Find duration of existing note
                        double existingDuration = findNoteDuration(slices[sliceIdx], noteEvents);
                        if (event.getDurationSeconds() > existingDuration) {
                            slices[sliceIdx] = noteName;
                        }
                    }
                }
            }
        }
        
        // Build pattern with consecutive note merging
        StringBuilder pattern = new StringBuilder();
        pattern.append("<");
        
        for (int measure = 0; measure < numMeasures; measure++) {
            int measureStart = measure * slicesPerMeasure;
            int measureEnd = measureStart + slicesPerMeasure;
            
            // Check if entire measure is empty
            boolean isEmpty = true;
            for (int i = measureStart; i < measureEnd; i++) {
                if (!slices[i].isEmpty()) {
                    isEmpty = false;
                    break;
                }
            }
            
            if (isEmpty) {
                // Compact rest notation
                pattern.append("[~@").append(slicesPerMeasure).append("]");
            } else {
                pattern.append("[");
                
                int sliceIdx = 0;
                while (sliceIdx < slicesPerMeasure) {
                    int absoluteSliceIdx = measureStart + sliceIdx;
                    
                    if (absoluteSliceIdx >= slices.length) {
                        break;
                    }
                    
                    String currentNote = slices[absoluteSliceIdx];
                    
                    if (currentNote.isEmpty()) {
                        // Rest - count consecutive
                        int restCount = 1;
                        while (sliceIdx + restCount < slicesPerMeasure) {
                            int nextAbsIdx = measureStart + sliceIdx + restCount;
                            if (nextAbsIdx >= slices.length || !slices[nextAbsIdx].isEmpty()) {
                                break;
                            }
                            restCount++;
                        }
                        
                        if (restCount == 1) {
                            pattern.append("~");
                        } else {
                            pattern.append("~@").append(restCount);
                        }
                        sliceIdx += restCount;
                    } else {
                        // Note - count consecutive identical (merging)
                        int noteCount = 1;
                        while (sliceIdx + noteCount < slicesPerMeasure) {
                            int nextAbsIdx = measureStart + sliceIdx + noteCount;
                            if (nextAbsIdx >= slices.length || !currentNote.equals(slices[nextAbsIdx])) {
                                break;
                            }
                            noteCount++;
                        }
                        
                        if (noteCount == 1) {
                            pattern.append(currentNote);
                        } else {
                            pattern.append(currentNote).append("@").append(noteCount);
                        }
                        sliceIdx += noteCount;
                    }
                    
                    if (sliceIdx < slicesPerMeasure) {
                        pattern.append(" ");
                    }
                }
                
                pattern.append("]");
            }
            
            if (measure < numMeasures - 1) {
                pattern.append(" ");
            }
        }
        
        pattern.append(">");
        return pattern.toString();
    }
    
    /**
     * Helper method to find the duration of a note by name.
     */
    private static double findNoteDuration(String noteName, List<EventOutput> noteEvents) {
        for (EventOutput event : noteEvents) {
            if (NoteConverter.toStrudelNoteName(event.getNoteNumber()).equals(noteName)) {
                return event.getDurationSeconds();
            }
        }
        return 0.0;
    }
}

