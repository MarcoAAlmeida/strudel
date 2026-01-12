package com.marcoalmeida.midi_tokenizer.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Base class for all MIDI events in the output.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventOutput {
    private String type;
    private long tick;
    private Double timeSeconds;
    private Integer channel;
    
    // For note events
    private Integer noteNumber;
    private String noteName;
    private Integer velocity;
    private Long durationTicks;
    private Double durationSeconds;
    
    // For control change events
    private Integer controller;
    private Integer value;
    
    // For program change events
    private Integer program;
    
    // For pitch bend events
    private Integer pitchBend;
    
    // For meta events
    private String text;
    
    public EventOutput() {}
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public long getTick() { return tick; }
    public void setTick(long tick) { this.tick = tick; }
    
    public Double getTimeSeconds() { return timeSeconds; }
    public void setTimeSeconds(Double timeSeconds) { this.timeSeconds = timeSeconds; }
    
    public Integer getChannel() { return channel; }
    public void setChannel(Integer channel) { this.channel = channel; }
    
    public Integer getNoteNumber() { return noteNumber; }
    public void setNoteNumber(Integer noteNumber) { this.noteNumber = noteNumber; }
    
    public String getNoteName() { return noteName; }
    public void setNoteName(String noteName) { this.noteName = noteName; }
    
    public Integer getVelocity() { return velocity; }
    public void setVelocity(Integer velocity) { this.velocity = velocity; }
    
    public Long getDurationTicks() { return durationTicks; }
    public void setDurationTicks(Long durationTicks) { this.durationTicks = durationTicks; }
    
    public Double getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Double durationSeconds) { this.durationSeconds = durationSeconds; }
    
    public Integer getController() { return controller; }
    public void setController(Integer controller) { this.controller = controller; }
    
    public Integer getValue() { return value; }
    public void setValue(Integer value) { this.value = value; }
    
    public Integer getProgram() { return program; }
    public void setProgram(Integer program) { this.program = program; }
    
    public Integer getPitchBend() { return pitchBend; }
    public void setPitchBend(Integer pitchBend) { this.pitchBend = pitchBend; }
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
