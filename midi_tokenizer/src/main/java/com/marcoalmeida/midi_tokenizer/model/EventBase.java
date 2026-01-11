package com.marcoalmeida.midi_tokenizer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = NoteEvent.class, name = "note"),
    @JsonSubTypes.Type(value = ControlChangeEvent.class, name = "control_change"),
    @JsonSubTypes.Type(value = ProgramChangeEvent.class, name = "program_change"),
    @JsonSubTypes.Type(value = PitchBendEvent.class, name = "pitch_bend"),
    @JsonSubTypes.Type(value = MetaEvent.class, name = "meta")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class EventBase {
    private long tick;
    private Double timeSeconds;

    public EventBase(long tick, Double timeSeconds) {
        this.tick = tick;
        this.timeSeconds = timeSeconds;
    }

    public long getTick() {
        return tick;
    }

    public void setTick(long tick) {
        this.tick = tick;
    }

    public Double getTimeSeconds() {
        return timeSeconds;
    }

    public void setTimeSeconds(Double timeSeconds) {
        this.timeSeconds = timeSeconds;
    }

    public abstract String toText();
}
