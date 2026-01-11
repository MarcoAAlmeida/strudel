package com.marcoalmeida.midi_tokenizer.model;

import java.util.List;

/**
 * Represents a MIDI file with its metadata and tracks.
 */
public class MidiFile {
    private int format;
    private int division;
    private List<Track> tracks;
    private TempoMap tempoMap;

    public MidiFile() {
    }

    public MidiFile(int format, int division, List<Track> tracks, TempoMap tempoMap) {
        this.format = format;
        this.division = division;
        this.tracks = tracks;
        this.tempoMap = tempoMap;
    }

    public int getFormat() {
        return format;
    }

    public void setFormat(int format) {
        this.format = format;
    }

    public int getDivision() {
        return division;
    }

    public void setDivision(int division) {
        this.division = division;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
    }

    public TempoMap getTempoMap() {
        return tempoMap;
    }

    public void setTempoMap(TempoMap tempoMap) {
        this.tempoMap = tempoMap;
    }
}
