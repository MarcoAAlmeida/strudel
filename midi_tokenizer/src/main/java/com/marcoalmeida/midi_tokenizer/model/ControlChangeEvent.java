package com.marcoalmeida.midi_tokenizer.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ControlChangeEvent extends EventBase {
    private int channel;
    private int controller;
    private int value;

    public ControlChangeEvent(long tick, Double timeSeconds, int channel, int controller, int value) {
        super(tick, timeSeconds);
        this.channel = channel;
        this.controller = controller;
        this.value = value;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getController() {
        return controller;
    }

    public void setController(int controller) {
        this.controller = controller;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public String toText() {
        return String.format("CC ch=%d ctrl=%d val=%d tick=%d time=%.3fs",
            channel, controller, value, getTick(), 
            getTimeSeconds() != null ? getTimeSeconds() : 0.0);
    }
}
