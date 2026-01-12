package com.marcoalmeida.midi_tokenizer.model;

/**
 * Represents a key signature change in the MIDI file.
 */
public class KeySignatureEntry {
    private long tick;
    private int sharpsFlats;
    private int majorMinor;
    
    public KeySignatureEntry() {}
    
    public KeySignatureEntry(long tick, int sharpsFlats, int majorMinor) {
        this.tick = tick;
        this.sharpsFlats = sharpsFlats;
        this.majorMinor = majorMinor;
    }
    
    public long getTick() { return tick; }
    public void setTick(long tick) { this.tick = tick; }
    
    public int getSharpsFlats() { return sharpsFlats; }
    public void setSharpsFlats(int sharpsFlats) { this.sharpsFlats = sharpsFlats; }
    
    public int getMajorMinor() { return majorMinor; }
    public void setMajorMinor(int majorMinor) { this.majorMinor = majorMinor; }
}
