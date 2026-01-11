package com.marcoalmeida.midi_tokenizer.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetaEvent extends EventBase {
    private int metaType;
    private String metaTypeName;
    private String data;

    public MetaEvent(long tick, Double timeSeconds, int metaType, String metaTypeName, String data) {
        super(tick, timeSeconds);
        this.metaType = metaType;
        this.metaTypeName = metaTypeName;
        this.data = data;
    }

    public int getMetaType() {
        return metaType;
    }

    public void setMetaType(int metaType) {
        this.metaType = metaType;
    }

    public String getMetaTypeName() {
        return metaTypeName;
    }

    public void setMetaTypeName(String metaTypeName) {
        this.metaTypeName = metaTypeName;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toText() {
        return String.format("Meta type=0x%02X(%s) data=\"%s\" tick=%d time=%.3fs",
            metaType, metaTypeName, data, getTick(), 
            getTimeSeconds() != null ? getTimeSeconds() : 0.0);
    }
}
