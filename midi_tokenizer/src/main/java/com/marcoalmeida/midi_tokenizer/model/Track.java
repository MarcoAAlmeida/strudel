package com.marcoalmeida.midi_tokenizer.model;

import java.util.List;

/**
 * Represents a MIDI track with its notes and metadata.
 */
public class Track {
    private int trackNumber;
    private String name;
    private List<Note> notes;
    private List<MetaEvent> metaEvents;

    public Track() {
    }

    public Track(int trackNumber, String name, List<Note> notes, List<MetaEvent> metaEvents) {
        this.trackNumber = trackNumber;
        this.name = name;
        this.notes = notes;
        this.metaEvents = metaEvents;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(int trackNumber) {
        this.trackNumber = trackNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Note> getNotes() {
        return notes;
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes;
    }

    public List<MetaEvent> getMetaEvents() {
        return metaEvents;
    }

    public void setMetaEvents(List<MetaEvent> metaEvents) {
        this.metaEvents = metaEvents;
    }
}
