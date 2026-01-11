package com.marcoalmeida.midi_tokenizer.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TimeSignatureEntry {
    private long tick;
    private int numerator;
    private int denominator;

    public TimeSignatureEntry(long tick, int numerator, int denominator) {
        this.tick = tick;
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public long getTick() {
        return tick;
    }

    public void setTick(long tick) {
        this.tick = tick;
    }

    public int getNumerator() {
        return numerator;
    }

    public void setNumerator(int numerator) {
        this.numerator = numerator;
    }

    public int getDenominator() {
        return denominator;
    }

    public void setDenominator(int denominator) {
        this.denominator = denominator;
    }

    public String toText() {
        return String.format("TimeSig tick=%d %d/%d", tick, numerator, denominator);
    }
}
