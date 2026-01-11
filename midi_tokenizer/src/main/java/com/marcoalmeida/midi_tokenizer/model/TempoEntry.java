package com.marcoalmeida.midi_tokenizer.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TempoEntry {
    private long tick;
    private int microsecondsPerQuarter;
    private double bpm;

    public TempoEntry(long tick, int microsecondsPerQuarter) {
        this.tick = tick;
        this.microsecondsPerQuarter = microsecondsPerQuarter;
        this.bpm = 60_000_000.0 / microsecondsPerQuarter;
    }

    public long getTick() {
        return tick;
    }

    public void setTick(long tick) {
        this.tick = tick;
    }

    public int getMicrosecondsPerQuarter() {
        return microsecondsPerQuarter;
    }

    public void setMicrosecondsPerQuarter(int microsecondsPerQuarter) {
        this.microsecondsPerQuarter = microsecondsPerQuarter;
    }

    public double getBpm() {
        return bpm;
    }

    public void setBpm(double bpm) {
        this.bpm = bpm;
    }

    public String toText() {
        return String.format("Tempo tick=%d bpm=%.2f µs/q=%d", tick, bpm, microsecondsPerQuarter);
    }
}
