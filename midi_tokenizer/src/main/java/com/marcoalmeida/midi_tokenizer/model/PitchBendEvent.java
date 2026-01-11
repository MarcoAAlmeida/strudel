package com.marcoalmeida.midi_tokenizer.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PitchBendEvent extends EventBase {
    private int channel;
    private int value;

    public PitchBendEvent(long tick, Double timeSeconds, int channel, int value) {
        super(tick, timeSeconds);
        this.channel = channel;
        this.value = value;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public String toText() {
        return String.format("PitchBend ch=%d val=%d tick=%d time=%.3fs",
            channel, value, getTick(), 
            getTimeSeconds() != null ? getTimeSeconds() : 0.0);
    }
}
