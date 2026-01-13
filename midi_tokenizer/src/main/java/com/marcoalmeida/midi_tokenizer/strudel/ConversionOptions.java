package com.marcoalmeida.midi_tokenizer.strudel;

/**
 * Options for MIDI to Strudel conversion.
 *
 * @param overrideTempo Optional tempo override in BPM
 * @param trackIndex    Optional track index to convert (default: 0)
 * @param quantization  Quantization level (8=8th notes, 16=16th notes, 32=32nd notes, default: 16)
 */
public record ConversionOptions(
    Integer overrideTempo,
    Integer trackIndex,
    Integer quantization
) {
    /**
     * Creates default options (no tempo override, track 0, 16th note quantization).
     */
    public static ConversionOptions defaults() {
        return new ConversionOptions(null, 0, 16);
    }

    /**
     * Gets the effective track index (defaults to 0 if null).
     */
    public int getEffectiveTrackIndex() {
        return trackIndex != null ? trackIndex : 0;
    }

    /**
     * Gets the effective quantization level (defaults to 16 if null).
     */
    public int getEffectiveQuantization() {
        return quantization != null ? quantization : 16;
    }
}
