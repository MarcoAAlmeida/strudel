package com.marcoalmeida.midi_tokenizer.model;

/**
 * Represents a time signature change in the MIDI file.
 */
public class TimeSignatureEntry {
    private long tick;
    private int numerator;
    private int denominator;
    private int clocksPerClick;
    private int thirtySecondsPer24Clocks;
    
    public TimeSignatureEntry() {}
    
    public TimeSignatureEntry(long tick, int numerator, int denominator, 
                             int clocksPerClick, int thirtySecondsPer24Clocks) {
        this.tick = tick;
        this.numerator = numerator;
        this.denominator = denominator;
        this.clocksPerClick = clocksPerClick;
        this.thirtySecondsPer24Clocks = thirtySecondsPer24Clocks;
    }
    
    public long getTick() { return tick; }
    public void setTick(long tick) { this.tick = tick; }
    
    public int getNumerator() { return numerator; }
    public void setNumerator(int numerator) { this.numerator = numerator; }
    
    public int getDenominator() { return denominator; }
    public void setDenominator(int denominator) { this.denominator = denominator; }
    
    public int getClocksPerClick() { return clocksPerClick; }
    public void setClocksPerClick(int clocksPerClick) { this.clocksPerClick = clocksPerClick; }
    
    public int getThirtySecondsPer24Clocks() { return thirtySecondsPer24Clocks; }
    public void setThirtySecondsPer24Clocks(int thirtySecondsPer24Clocks) { 
        this.thirtySecondsPer24Clocks = thirtySecondsPer24Clocks; 
    }
}
