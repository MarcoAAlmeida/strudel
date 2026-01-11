package com.marcoalmeida.midi_tokenizer.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeySignatureEntry {
    private long tick;
    private int key;
    private String scale;

    public KeySignatureEntry(long tick, int key, String scale) {
        this.tick = tick;
        this.key = key;
        this.scale = scale;
    }

    public long getTick() {
        return tick;
    }

    public void setTick(long tick) {
        this.tick = tick;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public String getScale() {
        return scale;
    }

    public void setScale(String scale) {
        this.scale = scale;
    }

    public String toText() {
        return String.format("KeySig tick=%d key=%d scale=%s", tick, key, scale);
    }
}
