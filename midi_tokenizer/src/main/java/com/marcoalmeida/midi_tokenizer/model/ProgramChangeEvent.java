package com.marcoalmeida.midi_tokenizer.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProgramChangeEvent extends EventBase {
    private int channel;
    private int program;

    public ProgramChangeEvent(long tick, Double timeSeconds, int channel, int program) {
        super(tick, timeSeconds);
        this.channel = channel;
        this.program = program;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getProgram() {
        return program;
    }

    public void setProgram(int program) {
        this.program = program;
    }

    @Override
    public String toText() {
        return String.format("PC ch=%d prog=%d tick=%d time=%.3fs",
            channel, program, getTick(), 
            getTimeSeconds() != null ? getTimeSeconds() : 0.0);
    }
}
