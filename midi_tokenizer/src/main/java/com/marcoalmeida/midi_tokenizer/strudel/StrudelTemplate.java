package com.marcoalmeida.midi_tokenizer.strudel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Renders Strudel pattern files from converted MIDI data.
 */
public class StrudelTemplate {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Renders a complete Strudel pattern file.
     *
     * @param patternName               Pattern variable name
     * @param sourceFile                Source MIDI filename
     * @param bpm                       Tempo in beats per minute
     * @param beatsPerCycle             Number of beats per cycle (typically 4 for 4/4 time)
     * @param trackIndex                MIDI track index
     * @param trackName                 Name of the MIDI track
     * @param timeSignatureNumerator    Time signature numerator (e.g., 4 for 4/4)
     * @param timeSignatureDenominator  Time signature denominator (e.g., 4 for 4/4)
     * @param quantization              Quantization level used
     * @param quantizationSource        Source of quantization ("default" or "override")
     * @param gridMeaning               Description of what the grid represents
     * @param slicesPerMeasure          Number of slices per measure
     * @param pattern                   Strudel pattern string (e.g., "c4 d4 e4")
     * @param instrument                Strudel instrument/sound name
     * @param polyphonicMode            Whether polyphonic mode was used
     * @return Complete Strudel pattern file content
     */
    public static String render(
        String patternName,
        String sourceFile,
        double bpm,
        int beatsPerCycle,
        int trackIndex,
        String trackName,
        int timeSignatureNumerator,
        int timeSignatureDenominator,
        int quantization,
        String quantizationSource,
        String gridMeaning,
        int slicesPerMeasure,
        String pattern,
        String instrument,
        boolean polyphonicMode
    ) {
        String convertedDate = LocalDateTime.now().format(DATE_FORMATTER);

        // Format pattern with line breaks between cycles for readability
        String formattedPattern = formatPatternWithLineBreaks(pattern);

        StringBuilder sb = new StringBuilder();
        
        // Title comment
        sb.append("/* \"").append(patternName).append("\" */\n");
        
        // Metadata block
        sb.append("/**\n");
        sb.append("Source: ").append(sourceFile).append("\n");
        sb.append("Tempo: ").append((int) bpm).append(" BPM\n");
        sb.append("Time Signature: ").append(timeSignatureNumerator).append("/").append(timeSignatureDenominator).append("\n");
        sb.append("Quantization: ").append(quantization).append(" (").append(quantizationSource).append(")\n");
        sb.append("Grid: ").append(gridMeaning).append("\n");
        sb.append("Mode: ").append(polyphonicMode ? "Polyphonic" : "Non-polyphonic").append("\n");
        sb.append("Track: ").append(trackIndex);
        if (trackName != null && !trackName.isEmpty()) {
            // Remove NUL characters and other control characters from track name
            String sanitizedTrackName = trackName.replaceAll("[\u0000-\u001F\u007F]", "");
            if (!sanitizedTrackName.isEmpty()) {
                sb.append(" (").append(sanitizedTrackName).append(")");
            }
        }
        sb.append("\n");
        sb.append("Converted: ").append(convertedDate).append("\n");
        sb.append("**/\n\n");
        
        // Set tempo: cycles per minute = bpm / beatsPerCycle
        sb.append("setcpm(").append((int) bpm).append("/").append(beatsPerCycle).append(")\n\n");
        
        // Pattern definition (angle brackets removed - already in pattern)
        sb.append("let ").append(patternName).append(" = note(`");
        sb.append(formattedPattern);
        sb.append("`).sound(\"").append(instrument).append("\")\n\n");
        
        // Room effect
        sb.append(patternName).append(".room(0.2)\n");
        
        return sb.toString();
    }

    /**
     * Formats a pattern string by adding line breaks between cycles.
     * Each cycle is a bracketed section like [notes].
     */
    private static String formatPatternWithLineBreaks(String pattern) {
        StringBuilder result = new StringBuilder();
        int depth = 0;
        
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            
            if (c == '[') {
                depth++;
                if (depth == 1 && result.length() > 0) {
                    // Start of a new top-level cycle, add line break
                    result.append('\n');
                }
            }
            
            result.append(c);
            
            if (c == ']' && depth == 1) {
                // End of a top-level cycle
                depth--;
                // Skip trailing space if present
                if (i + 1 < pattern.length() && pattern.charAt(i + 1) == ' ') {
                    i++;
                }
            } else if (c == ']') {
                depth--;
            }
        }
        
        return result.toString();
    }

    /**
     * Renders a multi-track Strudel pattern file (Phase 2).
     *
     * @param sourceFile       Source MIDI filename
     * @param bpm              Tempo in beats per minute
     * @param numerator        Time signature numerator
     * @param denominator      Time signature denominator
     * @param quantization     Quantization level used
     * @param totalTracks      Total number of tracks in MIDI file
     * @param trackPatterns    List of non-empty track patterns
     * @param polyphonicMode   Whether polyphonic mode was used
     * @return Complete multi-track Strudel pattern file content
     */
    public static String renderMultiTrack(
        String sourceFile,
        double bpm,
        int numerator,
        int denominator,
        int quantization,
        int totalTracks,
        List<TrackPattern> trackPatterns,
        boolean polyphonicMode
    ) {
        String convertedDate = LocalDateTime.now().format(DATE_FORMATTER);
        
        // Strip file extension for title
        String title = stripExtension(sourceFile);
        
        // Determine quantization source
        String quantizationSource = "default";  // In Phase 2, always using smart defaults
        
        // Generate grid meaning description
        String gridMeaning = generateGridMeaning(quantization, numerator, denominator);
        
        StringBuilder sb = new StringBuilder();
        
        // Title comment
        sb.append("/* \"").append(title).append("\" */\n");
        
        // Metadata block
        sb.append("/**\n");
        sb.append("Source: ").append(sourceFile).append("\n");
        sb.append("Tempo: ").append((int) Math.round(bpm)).append(" BPM\n");
        sb.append("Time Signature: ").append(numerator).append("/").append(denominator).append("\n");
        sb.append("Quantization: ").append(quantization).append(" (").append(quantizationSource).append(")\n");
        sb.append("Grid: ").append(gridMeaning).append("\n");
        sb.append("Mode: ").append(polyphonicMode ? "Polyphonic" : "Non-polyphonic").append("\n");
        sb.append("Tracks: ").append(trackPatterns.size()).append(" non-empty");
        
        // Show which tracks if not all tracks
        if (totalTracks != trackPatterns.size()) {
            String indices = trackPatterns.stream()
                .map(tp -> String.valueOf(tp.index()))
                .collect(Collectors.joining(", "));
            sb.append(" (").append(indices).append(" from ").append(totalTracks).append(" total)");
        }
        sb.append("\n");
        sb.append("Converted: ").append(convertedDate).append("\n");
        sb.append("**/\n\n");
        
        // Set tempo: cycles per minute = bpm / beatsPerCycle
        int beatsPerCycle = numerator * (4 / denominator);
        sb.append("setcpm(").append((int) Math.round(bpm)).append("/").append(beatsPerCycle).append(")\n\n");
        
        // Track definitions with instruments
        for (TrackPattern tp : trackPatterns) {
            String trackName = (tp.name() != null && !tp.name().trim().isEmpty()) ? 
                tp.name().replaceAll("[\u0000-\u001F\u007F]", "") : "Track " + tp.index();
            
            sb.append("// Track ").append(tp.index());
            if (!trackName.isEmpty() && !trackName.equals("Track " + tp.index())) {
                sb.append(": ").append(trackName);
            }
            sb.append("\n");
            
            String formattedPattern = formatPatternWithLineBreaks(tp.pattern());
            sb.append("let track").append(tp.index())
              .append(" = note(`").append(formattedPattern).append("`)")
              .append(".sound(\"").append(tp.instrument()).append("\")")
              .append(".room(0.2)\n\n");
        }
        
        // Stack call with all tracks
        String trackNames = trackPatterns.stream()
            .map(tp -> "track" + tp.index())
            .collect(Collectors.joining(", "));
        sb.append("// Play all tracks together (customize as needed)\n");
        sb.append("stack(").append(trackNames).append(")\n");
        
        return sb.toString();
    }

    /**
     * Strip file extension from filename.
     */
    private static String stripExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(0, dotIndex);
        }
        return filename;
    }

    /**
     * Generate grid meaning description based on quantization level.
     */
    private static String generateGridMeaning(int quantization, int numerator, int denominator) {
        if (quantization == 6) {
            return "6 = eighth notes, @3 = dotted quarter, @6 = dotted half";
        } else if (quantization == 8) {
            return "8 = eighth notes, @4 = half note";
        } else if (quantization == 12) {
            return "12 = triplet eighths, @3 = quarter note, @6 = half note";
        } else if (quantization == 16) {
            return "16 = sixteenth notes, @4 = quarter note, @8 = half note";
        } else if (quantization == 24) {
            return "24 = triplet sixteenths, @6 = quarter note, @12 = half note";
        } else if (quantization == 32) {
            return "32 = thirty-second notes, @8 = quarter note, @16 = half note";
        } else {
            // Generic description
            int slicesPerMeasure = (quantization * numerator) / denominator;
            int quarterNote = quantization / 4;
            int halfNote = quantization / 2;
            return String.format("%d slices per measure, @%d = quarter note, @%d = half note", 
                slicesPerMeasure, quarterNote, halfNote);
        }
    }

    /**
     * Track pattern information for multi-track rendering.
     */
    public record TrackPattern(int index, String name, String instrument, String pattern) {}
}
