package com.marcoalmeida.midi_tokenizer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents metadata collected from the MIDI file.
 */
public class Metadata {
    private List<TempoEntry> tempoMap = new ArrayList<>();
    private List<TimeSignatureEntry> timeSignatures = new ArrayList<>();
    private List<KeySignatureEntry> keySignatures = new ArrayList<>();
    
    public Metadata() {}
    
    public List<TempoEntry> getTempoMap() { return tempoMap; }
    public void setTempoMap(List<TempoEntry> tempoMap) { this.tempoMap = tempoMap; }
    
    public List<TimeSignatureEntry> getTimeSignatures() { return timeSignatures; }
    public void setTimeSignatures(List<TimeSignatureEntry> timeSignatures) { 
        this.timeSignatures = timeSignatures; 
    }
    
    public List<KeySignatureEntry> getKeySignatures() { return keySignatures; }
    public void setKeySignatures(List<KeySignatureEntry> keySignatures) { 
        this.keySignatures = keySignatures; 
    }
}
