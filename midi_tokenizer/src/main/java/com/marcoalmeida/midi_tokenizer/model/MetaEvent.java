package com.marcoalmeida.midi_tokenizer.model;

/**
 * Represents a MIDI meta event.
 */
public class MetaEvent {
    private String type;
    private double time;
    private String value;

    public MetaEvent() {
    }

    public MetaEvent(String type, double time, String value) {
        this.type = type;
        this.time = time;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
