package com.marcoalmeida.midi_tokenizer.model;

/**
 * Represents a program change event.
 */
public class ProgramChangeEvent {
    private long tick;
    private int channel;
    private int program;
    
    public ProgramChangeEvent() {}
    
    public ProgramChangeEvent(long tick, int channel, int program) {
        this.tick = tick;
        this.channel = channel;
        this.program = program;
    }
    
    public long getTick() { return tick; }
    public void setTick(long tick) { this.tick = tick; }
    
    public int getChannel() { return channel; }
    public void setChannel(int channel) { this.channel = channel; }
    
    public int getProgram() { return program; }
    public void setProgram(int program) { this.program = program; }
}
