package com.marcoalmeida.midi_tokenizer.model;

/**
 * Represents file metadata in the output JSON.
 */
public class FileMetadata {
    private String filename;
    private int format;
    private int division;
    private long durationTicks;
    private double durationSeconds;
    
    public FileMetadata() {}
    
    public FileMetadata(String filename, int format, int division, long durationTicks, double durationSeconds) {
        this.filename = filename;
        this.format = format;
        this.division = division;
        this.durationTicks = durationTicks;
        this.durationSeconds = durationSeconds;
    }
    
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    
    public int getFormat() { return format; }
    public void setFormat(int format) { this.format = format; }
    
    public int getDivision() { return division; }
    public void setDivision(int division) { this.division = division; }
    
    public long getDurationTicks() { return durationTicks; }
    public void setDurationTicks(long durationTicks) { this.durationTicks = durationTicks; }
    
    public double getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(double durationSeconds) { this.durationSeconds = durationSeconds; }
}
