package com.marcoalmeida.midi_tokenizer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a MIDI track in the output.
 */
public class TrackOutput {
    private int index;
    private String name;
    private List<ProgramChangeEvent> programChanges = new ArrayList<>();
    private List<EventOutput> events = new ArrayList<>();
    
    public TrackOutput() {}
    
    public TrackOutput(int index) {
        this.index = index;
    }
    
    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public List<ProgramChangeEvent> getProgramChanges() { return programChanges; }
    public void setProgramChanges(List<ProgramChangeEvent> programChanges) { 
        this.programChanges = programChanges; 
    }
    
    public List<EventOutput> getEvents() { return events; }
    public void setEvents(List<EventOutput> events) { this.events = events; }
}
