package com.marcoalmeida.midi_tokenizer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Root object for the parsed MIDI file output.
 */
public class MidiOutput {
    private String schemaVersion = "1.0";
    private FileMetadata file;
    private Metadata metadata = new Metadata();
    private List<TrackOutput> tracks = new ArrayList<>();
    
    public MidiOutput() {}
    
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
    
    public FileMetadata getFile() { return file; }
    public void setFile(FileMetadata file) { this.file = file; }
    
    public Metadata getMetadata() { return metadata; }
    public void setMetadata(Metadata metadata) { this.metadata = metadata; }
    
    public List<TrackOutput> getTracks() { return tracks; }
    public void setTracks(List<TrackOutput> tracks) { this.tracks = tracks; }
}
