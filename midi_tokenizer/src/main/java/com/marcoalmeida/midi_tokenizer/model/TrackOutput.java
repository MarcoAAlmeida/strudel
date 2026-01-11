package com.marcoalmeida.midi_tokenizer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrackOutput {
    private int trackIndex;
    private String name;
    private List<Integer> programChanges = new ArrayList<>();
    private List<EventBase> events = new ArrayList<>();

    public TrackOutput(int trackIndex) {
        this.trackIndex = trackIndex;
    }

    public int getTrackIndex() {
        return trackIndex;
    }

    public void setTrackIndex(int trackIndex) {
        this.trackIndex = trackIndex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Integer> getProgramChanges() {
        return programChanges;
    }

    public void setProgramChanges(List<Integer> programChanges) {
        this.programChanges = programChanges;
    }

    public List<EventBase> getEvents() {
        return events;
    }

    public void setEvents(List<EventBase> events) {
        this.events = events;
    }

    public String toText() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Track %d", trackIndex));
        if (name != null && !name.isEmpty()) {
            sb.append(String.format(" \"%s\"", name));
        }
        sb.append(String.format(" (%d events)\n", events.size()));
        if (!programChanges.isEmpty()) {
            sb.append("  Programs: ").append(programChanges).append("\n");
        }
        for (EventBase event : events) {
            sb.append("  ").append(event.toText()).append("\n");
        }
        return sb.toString();
    }
}
