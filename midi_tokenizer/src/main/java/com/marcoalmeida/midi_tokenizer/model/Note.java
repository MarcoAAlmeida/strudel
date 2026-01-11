package com.marcoalmeida.midi_tokenizer.model;

/**
 * Represents a musical note with timing and pitch information.
 */
public class Note {
    private int midiNote;
    private String noteName;
    private int velocity;
    private double startTime;
    private double duration;
    private int channel;

    public Note() {
    }

    public Note(int midiNote, String noteName, int velocity, double startTime, double duration, int channel) {
        this.midiNote = midiNote;
        this.noteName = noteName;
        this.velocity = velocity;
        this.startTime = startTime;
        this.duration = duration;
        this.channel = channel;
    }

    public int getMidiNote() {
        return midiNote;
    }

    public void setMidiNote(int midiNote) {
        this.midiNote = midiNote;
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

    public double getStartTime() {
        return startTime;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }
}
