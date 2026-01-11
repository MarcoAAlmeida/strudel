package com.marcoalmeida.midi_tokenizer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Metadata {
    private List<TempoEntry> tempoMap = new ArrayList<>();
    private List<TimeSignatureEntry> timeSignatures = new ArrayList<>();
    private List<KeySignatureEntry> keySignatures = new ArrayList<>();

    public List<TempoEntry> getTempoMap() {
        return tempoMap;
    }

    public void setTempoMap(List<TempoEntry> tempoMap) {
        this.tempoMap = tempoMap;
    }

    public List<TimeSignatureEntry> getTimeSignatures() {
        return timeSignatures;
    }

    public void setTimeSignatures(List<TimeSignatureEntry> timeSignatures) {
        this.timeSignatures = timeSignatures;
    }

    public List<KeySignatureEntry> getKeySignatures() {
        return keySignatures;
    }

    public void setKeySignatures(List<KeySignatureEntry> keySignatures) {
        this.keySignatures = keySignatures;
    }

    public String toText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Metadata:\n");
        if (!tempoMap.isEmpty()) {
            sb.append("  Tempo Map:\n");
            for (TempoEntry entry : tempoMap) {
                sb.append("    ").append(entry.toText()).append("\n");
            }
        }
        if (!timeSignatures.isEmpty()) {
            sb.append("  Time Signatures:\n");
            for (TimeSignatureEntry entry : timeSignatures) {
                sb.append("    ").append(entry.toText()).append("\n");
            }
        }
        if (!keySignatures.isEmpty()) {
            sb.append("  Key Signatures:\n");
            for (KeySignatureEntry entry : keySignatures) {
                sb.append("    ").append(entry.toText()).append("\n");
            }
        }
        return sb.toString();
    }
}
