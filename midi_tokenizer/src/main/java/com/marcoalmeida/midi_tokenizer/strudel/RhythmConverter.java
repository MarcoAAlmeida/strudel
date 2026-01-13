package com.marcoalmeida.midi_tokenizer.strudel;

import com.marcoalmeida.midi_tokenizer.model.EventOutput;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts MIDI note events to Strudel pattern strings with cycle-based grouping.
 */
public class RhythmConverter {

    /**
     * Converts note events to quantized Strudel pattern.
     * Quantizes notes to a fixed grid (8th, 16th, 32nd notes).
     * All cycles have the same number of time slices.
     *
     * @param noteEvents List of note events from a track
     * @param division MIDI ticks per quarter note
     * @param timeSignatureNumerator Time signature numerator (e.g., 4 for 4/4)
     * @param timeSignatureDenominator Time signature denominator (e.g., 4 for 4/4)
     * @param quantization Quantization level (8=eighth notes, 16=sixteenth notes, etc.)
     * @return Quantized cycle-based pattern string
     */
    public static String toQuantizedCyclePattern(
        List<EventOutput> noteEvents,
        int division,
        int timeSignatureNumerator,
        int timeSignatureDenominator,
        int quantization
    ) {
        if (noteEvents.isEmpty()) {
            return "";
        }

        // Calculate slice duration in ticks
        long ticksPerQuarterNote = division;
        long ticksPerSlice = ticksPerQuarterNote * 4 / quantization;
        int slicesPerMeasure = quantization * timeSignatureNumerator / timeSignatureDenominator;
        long ticksPerMeasure = ticksPerQuarterNote * timeSignatureNumerator * 4 / timeSignatureDenominator;

        // Find the range of measures
        long maxTick = noteEvents.stream()
            .mapToLong(e -> e.getTick() + e.getDurationTicks())
            .max()
            .orElse(0);
        int numMeasures = (int) Math.ceil((double) maxTick / ticksPerMeasure);

        // Build pattern measure by measure
        StringBuilder pattern = new StringBuilder();

        for (int measureNum = 0; measureNum < numMeasures; measureNum++) {
            long measureStartTick = measureNum * ticksPerMeasure;
            long measureEndTick = measureStartTick + ticksPerMeasure;

            // Create time slices for this measure
            String[] slices = new String[slicesPerMeasure];
            Arrays.fill(slices, "~"); // Initialize with rests

            // Fill slices with notes
            for (EventOutput event : noteEvents) {
                long noteStart = event.getTick();
                long noteEnd = noteStart + event.getDurationTicks();

                // Skip notes outside this measure
                if (noteEnd <= measureStartTick || noteStart >= measureEndTick) {
                    continue;
                }

                // Find which slices this note occupies
                int startSlice = (int) Math.max(0, (noteStart - measureStartTick) / ticksPerSlice);
                int endSlice = (int) Math.min(slicesPerMeasure - 1, (noteEnd - measureStartTick - 1) / ticksPerSlice);

                String noteName = NoteConverter.toStrudelNoteName(event.getNoteNumber());

                for (int sliceIdx = startSlice; sliceIdx <= endSlice; sliceIdx++) {
                    long sliceStart = measureStartTick + sliceIdx * ticksPerSlice;
                    long sliceEnd = sliceStart + ticksPerSlice;

                    // Calculate how much of this slice the note occupies
                    long noteInSliceStart = Math.max(noteStart, sliceStart);
                    long noteInSliceEnd = Math.min(noteEnd, sliceEnd);
                    long noteInSliceDuration = noteInSliceEnd - noteInSliceStart;
                    long sliceDuration = sliceEnd - sliceStart;

                    // If note occupies more than 50% of slice, it takes precedence
                    if (noteInSliceDuration > sliceDuration / 2) {
                        // If there's already a note, pick the one with longer total duration
                        if ("~".equals(slices[sliceIdx])) {
                            slices[sliceIdx] = noteName;
                        } else {
                            // Compare durations - keep longer note
                            long existingNoteDuration = findNoteDuration(slices[sliceIdx], noteEvents, sliceStart);
                            if (event.getDurationTicks() > existingNoteDuration) {
                                slices[sliceIdx] = noteName;
                            }
                        }
                    }
                }
            }

            // Merge consecutive identical notes and build measure pattern
            pattern.append("[");
            int i = 0;
            while (i < slicesPerMeasure) {
                String currentNote = slices[i];
                int count = 1;

                // Count consecutive identical notes/rests
                while (i + count < slicesPerMeasure && slices[i + count].equals(currentNote)) {
                    count++;
                }

                // Calculate duration in terms of quantization units
                int durationInSlices = count;
                
                if (count == 1) {
                    pattern.append(currentNote).append(" ");
                } else {
                    // Use @ notation for durations > 1 slice
                    pattern.append(currentNote).append("@").append(durationInSlices).append(" ");
                }

                i += count;
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
     * Helper method to find the duration of a note by name at a given tick position.
     */
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
