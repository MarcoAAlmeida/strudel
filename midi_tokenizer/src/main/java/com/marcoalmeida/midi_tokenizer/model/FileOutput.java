package com.marcoalmeida.midi_tokenizer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileOutput {
    @JsonProperty("schema_version")
    private String schemaVersion = "1.0";
    
    private FileMetadata file;
    private Metadata metadata;
    private List<TrackOutput> tracks = new ArrayList<>();

    public FileOutput() {
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public FileMetadata getFile() {
        return file;
    }

    public void setFile(FileMetadata file) {
        this.file = file;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public List<TrackOutput> getTracks() {
        return tracks;
    }

    public void setTracks(List<TrackOutput> tracks) {
        this.tracks = tracks;
    }

    public String toText() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== MIDI File Analysis ===\n\n");
        sb.append(file.toText()).append("\n\n");
        sb.append(metadata.toText()).append("\n");
        sb.append(String.format("Tracks: %d\n\n", tracks.size()));
        for (TrackOutput track : tracks) {
            sb.append(track.toText()).append("\n");
        }
        return sb.toString();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FileMetadata {
        private String filename;
        private int format;
        private int division;
        @JsonProperty("duration_ticks")
        private Long durationTicks;
        @JsonProperty("duration_seconds")
        private Double durationSeconds;

        public FileMetadata(String filename, int format, int division) {
            this.filename = filename;
            this.format = format;
            this.division = division;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
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

        public String toText() {
            return String.format("File: %s\nFormat: %d\nDivision: %d ticks/quarter\nDuration: %d ticks (%.3f seconds)",
                filename, format, division,
                durationTicks != null ? durationTicks : 0,
                durationSeconds != null ? durationSeconds : 0.0);
        }
    }
}
