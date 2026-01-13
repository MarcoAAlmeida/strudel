package com.marcoalmeida.midi_tokenizer.strudel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Renders Strudel pattern files from converted MIDI data.
 */
public class StrudelTemplate {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Renders a complete Strudel pattern file.
     *
     * @param patternName    Pattern variable name
     * @param sourceFile     Source MIDI filename
     * @param bpm            Tempo in beats per minute
     * @param beatsPerCycle  Number of beats per cycle (typically 4 for 4/4 time)
     * @param trackIndex     MIDI track index
     * @param trackName      Name of the MIDI track
     * @param quantization   Quantization level (8, 16, or 32)
     * @param pattern        Strudel pattern string (e.g., "c4 d4 e4")
     * @param instrument     Strudel instrument/sound name
     * @return Complete Strudel pattern file content
     */
    public static String render(
        String patternName,
        String sourceFile,
        double bpm,
        int beatsPerCycle,
        int trackIndex,
        String trackName,
        int quantization,
        String pattern,
        String instrument
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
        sb.append("Track: ").append(trackIndex);
        if (trackName != null && !trackName.isEmpty()) {
            sb.append(" (").append(trackName).append(")");
        }
        sb.append("\n");
        sb.append("Quantization: ").append(quantization).append("th notes\n");
        sb.append("Converted: ").append(convertedDate).append("\n");
        sb.append("**/\n\n");
        
        // Set tempo: cycles per minute = bpm / beatsPerCycle
        sb.append("setcpm(").append((int) bpm).append("/").append(beatsPerCycle).append(")\n\n");
        
        // Pattern definition with angle brackets for sequential playback
        sb.append("let ").append(patternName).append(" = note(`<\n");
        sb.append(formattedPattern);
        sb.append("\n>`).sound(\"").append(instrument).append("\")\n\n");
        
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
}
