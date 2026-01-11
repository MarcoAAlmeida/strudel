package com.marcoalmeida.midi_tokenizer.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NoteEvent extends EventBase {
    private int channel;
    private int noteNumber;
    private String noteName;
    private int velocity;
    private Long durationTicks;
    private Double durationSeconds;

    public NoteEvent(long tick, Double timeSeconds, int channel, int noteNumber, String noteName, int velocity) {
        super(tick, timeSeconds);
        this.channel = channel;
        this.noteNumber = noteNumber;
        this.noteName = noteName;
        this.velocity = velocity;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getNoteNumber() {
        return noteNumber;
    }

    public void setNoteNumber(int noteNumber) {
        this.noteNumber = noteNumber;
    }

    public String getNoteName() {
        return noteName;
    }

    public void setNoteName(String noteName) {
        this.noteName = noteName;
    }

    public int getVelocity() {
        return velocity;
    }

    public void setVelocity(int velocity) {
        this.velocity = velocity;
    }

    public Long getDurationTicks() {
        return durationTicks;
    }

    public void setDurationTicks(Long durationTicks) {
        this.durationTicks = durationTicks;
    }

    public Double getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Double durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    @Override
    public String toText() {
        return String.format("Note ch=%d %s(%d) vel=%d tick=%d time=%.3fs dur=%dticks/%.3fs",
            channel, noteName, noteNumber, velocity, getTick(), 
            getTimeSeconds() != null ? getTimeSeconds() : 0.0,
            durationTicks != null ? durationTicks : 0,
            durationSeconds != null ? durationSeconds : 0.0);
    }
}
