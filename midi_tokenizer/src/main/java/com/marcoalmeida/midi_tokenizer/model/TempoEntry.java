package com.marcoalmeida.midi_tokenizer.model;

/**
 * Represents a tempo change entry in the tempo map.
 */
public class TempoEntry {
    private long tick;
    private long microsecondsPerQuarter;
    private double bpm;
    
    public TempoEntry() {}
    
    public TempoEntry(long tick, long microsecondsPerQuarter, double bpm) {
        this.tick = tick;
        this.microsecondsPerQuarter = microsecondsPerQuarter;
        this.bpm = bpm;
    }
    
    public long getTick() { return tick; }
    public void setTick(long tick) { this.tick = tick; }
    
    public long getMicrosecondsPerQuarter() { return microsecondsPerQuarter; }
    public void setMicrosecondsPerQuarter(long microsecondsPerQuarter) { 
        this.microsecondsPerQuarter = microsecondsPerQuarter; 
    }
    
    public double getBpm() { return bpm; }
    public void setBpm(double bpm) { this.bpm = bpm; }
}
