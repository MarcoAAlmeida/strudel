package com.marcoalmeida.midi_tokenizer.strudel;

/**
 * Options for MIDI to Strudel conversion.
 *
 * @param overrideTempo   Optional tempo override in BPM
 * @param trackIndex      Optional track index to convert (default: 0)
 * @param quantization    Optional quantization level (auto-calculated if null)
 * @param enablePolyphony Enable polyphonic conversion (default: true). False uses non-polyphonic mode.
 */
public record ConversionOptions(
    Integer overrideTempo,
    Integer trackIndex,
    Integer quantization,
    Boolean enablePolyphony
) {
    /**
     * Creates default options (no tempo override, track 0, auto-quantization, polyphonic enabled).
     */
    public static ConversionOptions defaults() {
        return new ConversionOptions(null, 0, null, true);
    }

    /**
     * Gets the effective track index (defaults to 0 if null).
     */
    public int getEffectiveTrackIndex() {
        return trackIndex != null ? trackIndex : 0;
    }

    /**
     * Checks if polyphonic mode is enabled.
     * Defaults to true if not explicitly set.
     * 
     * @return true if polyphonic mode enabled, false for non-polyphonic mode
     */
    public boolean isPolyphonicMode() {
        return enablePolyphony == null || enablePolyphony;
    }

    /**
     * Gets the effective quantization level.
     * Uses override if provided, otherwise calculates smart default based on time signature.
     * 
     * Smart defaults:
     * - 3/4 (waltz) → 6 (eighth note resolution)
     * - 6/8 (compound) → 6 (eighth note resolution)
     * - 2/4 (march) → 16 (sixteenth note resolution)
     * - 4/4 and others → 16 (sixteenth note resolution)
     *
     * @param numerator   Time signature numerator
     * @param denominator Time signature denominator
     * @return Effective quantization level
     */
    public int getEffectiveQuantization(int numerator, int denominator) {
        if (quantization != null) {
            return quantization;
        }
        
        // Smart defaults based on time signature
        if (numerator == 3 && denominator == 4) return 6;   // 3/4 waltz - eighth notes
        if (numerator == 6 && denominator == 8) return 6;   // 6/8 compound - eighth notes
        if (numerator == 2 && denominator == 4) return 16;  // 2/4 march - sixteenth notes
        return 16;  // Default: 4/4 and other time signatures - sixteenth notes
    }
}
