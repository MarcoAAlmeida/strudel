package com.marcoalmeida.midi_tokenizer.strudel;

import com.marcoalmeida.midi_tokenizer.model.EventOutput;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts MIDI note events to Strudel pattern strings with cycle-based grouping.
 * Uses 24-grid universal base for automatic time-signature-aware quantization.
 * Preserves polyphony using comma notation.
 */
public class RhythmConverter {

    private static final int GRID_BASE = 8;  // 8-grid for clean 8th note resolution

    /**
     * NoteEvent record for tracking simultaneous notes with individual durations.
     */
    record NoteEvent(String noteName, double duration) {}

    /**
     * Converts note events to polyphonic 24-grid Strudel pattern.
     * Uses automatic time-signature-aware grid calculation.
     * Preserves ALL simultaneous notes using comma notation.
     *
     * @param noteEvents List of note events from a track
     * @param division MIDI ticks per quarter note
     * @param timeSignatureNumerator Time signature numerator (e.g., 4 for 4/4)
     * @param timeSignatureDenominator Time signature denominator (e.g., 4 for 4/4)
     * @return Polyphonic pattern string with [measure] grouping and comma notation
     */
    public static String toQuantizedCyclePattern(
        List<EventOutput> noteEvents,
        int division,
        int timeSignatureNumerator,
        int timeSignatureDenominator
    ) {
        if (noteEvents.isEmpty()) {
            return "";
        }

        // Calculate 24-grid slices per measure
        // Formula: slicesPerMeasure = 24 * numerator / denominator
        // Examples: 4/4 → 24, 3/4 → 18, 6/8 → 18, 5/4 → 30
        int slicesPerMeasure = GRID_BASE * timeSignatureNumerator / timeSignatureDenominator;
        
        // Calculate slice duration in ticks
        long ticksPerQuarterNote = division;
        long ticksPerMeasure = ticksPerQuarterNote * timeSignatureNumerator * 4 / timeSignatureDenominator;
        double ticksPerSlice = (double) ticksPerMeasure / slicesPerMeasure;

        // Find the range of measures
        long maxTick = noteEvents.stream()
            .mapToLong(e -> e.getTick() + e.getDurationTicks())
            .max()
            .orElse(0);
        int numMeasures = (int) Math.ceil((double) maxTick / ticksPerMeasure);

        // Create List<NoteEvent>[] to collect simultaneous notes
        @SuppressWarnings("unchecked")
        List<NoteEvent>[] slices = new ArrayList[slicesPerMeasure * numMeasures];
        for (int i = 0; i < slices.length; i++) {
            slices[i] = new ArrayList<>();
        }

        // Collect notes at their START positions only (trigger-point tracking)
        for (EventOutput event : noteEvents) {
            long noteStart = event.getTick();
            
            // SNAP to nearest grid position to avoid micro-timing issues
            int startSlice = (int) Math.round(noteStart / ticksPerSlice);
            
            // Skip notes that start beyond our grid
            if (startSlice >= slices.length) {
                continue;
            }

            // Calculate duration with quarter-precision rounding
            double durationInSlices = event.getDurationTicks() / ticksPerSlice;
            double roundedDuration = Math.round(durationInSlices * 4.0) / 4.0;
            
            // Minimum duration of 0.25
            if (roundedDuration < 0.25) {
                roundedDuration = 0.25;
            }

            String noteName = NoteConverter.toStrudelNoteName(event.getNoteNumber());
            slices[startSlice].add(new NoteEvent(noteName, roundedDuration));
        }

        // Build pattern measure by measure
        StringBuilder pattern = new StringBuilder();

        for (int measureNum = 0; measureNum < numMeasures; measureNum++) {
            pattern.append("[");

            for (int sliceIdx = 0; sliceIdx < slicesPerMeasure; sliceIdx++) {
                int absoluteSliceIdx = measureNum * slicesPerMeasure + sliceIdx;
                
                if (slices[absoluteSliceIdx].isEmpty()) {
                    // Empty slot = rest
                    pattern.append("~");
                } else if (slices[absoluteSliceIdx].size() == 1) {
                    // Single note - no brackets needed
                    NoteEvent ne = slices[absoluteSliceIdx].get(0);
                    if (ne.duration == 1.0) {
                        pattern.append(ne.noteName);
                    } else {
                        String durationStr = formatDuration(ne.duration);
                        pattern.append(ne.noteName).append("@").append(durationStr);
                    }
                } else {
                    // Multiple simultaneous notes - wrap in brackets
                    String noteGroup = slices[absoluteSliceIdx].stream()
                        .map(ne -> {
                            // Omit @1 suffix (implicit duration)
                            if (ne.duration == 1.0) {
                                return ne.noteName;
                            } else {
                                // Format duration, removing unnecessary decimals
                                String durationStr = formatDuration(ne.duration);
                                return ne.noteName + "@" + durationStr;
                            }
                        })
                        .collect(Collectors.joining(","));
                    pattern.append("[").append(noteGroup).append("]");
                }
                
                if (sliceIdx < slicesPerMeasure - 1) {
                    pattern.append(" ");
                }
            }

            pattern.append("] ");
        }

        // Remove trailing space
        if (pattern.length() > 0 && pattern.charAt(pattern.length() - 1) == ' ') {
            pattern.setLength(pattern.length() - 1);
        }

        return pattern.toString();
    }

    /**
     * Format duration value, removing unnecessary decimals.
     * Examples: 2.0 → "2", 1.5 → "1.5", 0.25 → "0.25"
     */
    private static String formatDuration(double duration) {
        if (duration == (long) duration) {
            return String.format("%d", (long) duration);
        } else {
            return String.format("%s", duration).replaceAll("\\.?0+$", "");
        }
    }

    /**
     * Helper method to find the duration of a note by name at a given tick position.
     * @deprecated No longer needed with polyphonic approach
     */
    @Deprecated
    private static long findNoteDuration(String noteName, List<EventOutput> noteEvents, long nearTick) {
        for (EventOutput event : noteEvents) {
            if (NoteConverter.toStrudelNoteName(event.getNoteNumber()).equals(noteName) &&
                Math.abs(event.getTick() - nearTick) < 100) { // Within tolerance
                return event.getDurationTicks();
            }
        }
        return 0;
    }

    /**
     * Converts note events to Strudel pattern with cycle brackets.
     * Each cycle (measure) is wrapped in [] brackets.
     * Durations are added using @ notation.
     * Rests are inserted as ~ for gaps.
     *
     * @deprecated Use toQuantizedCyclePattern for better rhythm preservation
     */
    @Deprecated
    public static String toCyclePattern(
        List<EventOutput> noteEvents,
        double bpm,
        int division,
        int timeSignatureNumerator,
        int timeSignatureDenominator
    ) {
        if (noteEvents.isEmpty()) {
            return "";
        }

        // Calculate measure duration in ticks
        // For 4/4 time: 4 quarter notes per measure
        // For 3/4 time: 3 quarter notes per measure
        long ticksPerQuarterNote = division;
        long ticksPerMeasure = ticksPerQuarterNote * timeSignatureNumerator * 4 / timeSignatureDenominator;
        
        // Quarter note duration in seconds (for reference)
        double quarterNoteSeconds = 60.0 / bpm;
        
        // Group notes by measure
        Map<Integer, List<EventOutput>> measureMap = new TreeMap<>();
        for (EventOutput event : noteEvents) {
            int measureNumber = (int) (event.getTick() / ticksPerMeasure);
            measureMap.computeIfAbsent(measureNumber, k -> new ArrayList<>()).add(event);
        }
        
        // Build pattern with cycle brackets
        StringBuilder pattern = new StringBuilder();
        
        for (Map.Entry<Integer, List<EventOutput>> entry : measureMap.entrySet()) {
            List<EventOutput> measureNotes = entry.getValue();
            
            // Sort notes by tick within measure
            measureNotes.sort(Comparator.comparingLong(EventOutput::getTick));
            
            pattern.append("[");
            
            long measureStartTick = entry.getKey() * ticksPerMeasure;
            long currentTick = measureStartTick;
            
            for (EventOutput event : measureNotes) {
                // Check for rest before this note
                long gapTicks = event.getTick() - currentTick;
                double gapQuarters = (double) gapTicks / ticksPerQuarterNote;
                
                // Insert rest if gap is significant (> 5% of a quarter note)
                if (gapQuarters > 0.05) {
                    pattern.append("~ ");
                }
                
                // Add note with duration
                String noteName = NoteConverter.toStrudelNoteName(event.getNoteNumber());
                double durationQuarters = (double) event.getDurationTicks() / ticksPerQuarterNote;
                
                // Only add duration notation if not exactly 1 quarter note
                if (Math.abs(durationQuarters - 1.0) > 0.05) {
                    pattern.append(String.format("%s@%.2f ", noteName, durationQuarters));
                } else {
                    pattern.append(noteName).append(" ");
                }
                
                currentTick = event.getTick() + event.getDurationTicks();
            }
            
            // Remove trailing space
            if (pattern.charAt(pattern.length() - 1) == ' ') {
                pattern.setLength(pattern.length() - 1);
            }
            
            pattern.append("] ");
        }
        
        // Remove trailing space
        if (pattern.length() > 0 && pattern.charAt(pattern.length() - 1) == ' ') {
            pattern.setLength(pattern.length() - 1);
        }
        
        return pattern.toString();
    }
    
    /**
     * Converts a list of note events to a simple space-separated Strudel pattern.
     * Legacy method - prefer toCyclePattern for proper rhythm preservation.
     *
     * @param noteEvents List of note events from a track
     * @return Space-separated pattern string (e.g., "c4 d4 e4 f4 g4")
     */
    @Deprecated
    public static String toSimplePattern(List<EventOutput> noteEvents) {
        return noteEvents.stream()
            .map(event -> NoteConverter.toStrudelNoteName(event.getNoteNumber()))
            .collect(Collectors.joining(" "));
    }
}
