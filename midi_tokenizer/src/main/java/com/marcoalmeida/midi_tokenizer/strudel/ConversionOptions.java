package com.marcoalmeida.midi_tokenizer.strudel;

/**
 * Options for MIDI to Strudel conversion.
 *
 * @param overrideTempo Optional tempo override in BPM
 * @param trackIndex    Optional track index to convert (default: 0)
 */
public record ConversionOptions(
    Integer overrideTempo,
    Integer trackIndex
) {
    /**
     * Creates default options (no tempo override, track 0).
     */
    public static ConversionOptions defaults() {
        return new ConversionOptions(null, 0);
    }

    /**
     * Gets the effective track index (defaults to 0 if null).
     */
    public int getEffectiveTrackIndex() {
        return trackIndex != null ? trackIndex : 0;
    }
}
