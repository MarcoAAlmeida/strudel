package com.marcoalmeida.midi_tokenizer.midi;

import com.marcoalmeida.midi_tokenizer.model.*;
import com.marcoalmeida.midi_tokenizer.util.NoteUtils;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Parser for Standard MIDI Files (SMF) types 0 and 1.
 * Uses javax.sound.midi to parse MIDI sequences and convert to an LLM-friendly JSON representation.
 */
public class MidiParser {

    private static final int DEFAULT_TEMPO_MICROSECONDS = 500000; // 120 BPM
    
    private boolean includeMeta = true;
    private boolean useSeconds = true;

    public MidiParser() {
    }

    public void setIncludeMeta(boolean includeMeta) {
        this.includeMeta = includeMeta;
    }

    public void setUseSeconds(boolean useSeconds) {
        this.useSeconds = useSeconds;
    }

    /**
     * Parse a MIDI file and return the structured output.
     */
    public FileOutput parse(File file) throws InvalidMidiDataException, IOException {
        Sequence sequence = MidiSystem.getSequence(file);
        
        FileOutput output = new FileOutput();
        
        // File metadata
        int format = getSequenceType(sequence);
        int division = sequence.getResolution();
        FileOutput.FileMetadata fileMetadata = new FileOutput.FileMetadata(
            file.getName(), format, division
        );
        output.setFile(fileMetadata);
        
        // Build tempo map
        Metadata metadata = new Metadata();
        List<TempoEntry> tempoMap = buildTempoMap(sequence);
        metadata.setTempoMap(tempoMap);
        
        // Parse all tracks for metadata and events
        parseTracks(sequence, metadata, output, division, tempoMap);
        
        output.setMetadata(metadata);
        
        // Calculate file duration
        long maxTick = getMaxTick(sequence);
        fileMetadata.setDurationTicks(maxTick);
        if (useSeconds && !tempoMap.isEmpty()) {
            fileMetadata.setDurationSeconds(ticksToSeconds(maxTick, division, tempoMap));
        }
        
        return output;
    }

    private int getSequenceType(Sequence sequence) {
        float divisionType = sequence.getDivisionType();
        Track[] tracks = sequence.getTracks();
        
        // SMF format is not directly available via javax.sound.midi API
        // We infer: if there's only one track, it's likely format 0
        if (tracks.length == 1) {
            return 0;
        } else {
            return 1; // Assume format 1 for multiple tracks
        }
    }

    private List<TempoEntry> buildTempoMap(Sequence sequence) {
        List<TempoEntry> tempoMap = new ArrayList<>();
        
        // Search all tracks for tempo events
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                
                if (message instanceof MetaMessage) {
                    MetaMessage meta = (MetaMessage) message;
                    if (meta.getType() == 0x51) { // Set Tempo
                        byte[] data = meta.getData();
                        int microsecondsPerQuarter = ((data[0] & 0xFF) << 16) 
                                                   | ((data[1] & 0xFF) << 8) 
                                                   | (data[2] & 0xFF);
                        tempoMap.add(new TempoEntry(event.getTick(), microsecondsPerQuarter));
                    }
                }
            }
        }
        
        // If no tempo events found, add default
        if (tempoMap.isEmpty()) {
            tempoMap.add(new TempoEntry(0, DEFAULT_TEMPO_MICROSECONDS));
        }
        
        // Sort by tick
        tempoMap.sort(Comparator.comparingLong(TempoEntry::getTick));
        
        return tempoMap;
    }

    private void parseTracks(Sequence sequence, Metadata metadata, FileOutput output, 
                            int division, List<TempoEntry> tempoMap) {
        Track[] tracks = sequence.getTracks();
        
        for (int trackIndex = 0; trackIndex < tracks.length; trackIndex++) {
            TrackOutput trackOutput = new TrackOutput(trackIndex);
            Track track = tracks[trackIndex];
            
            // Track active notes (for note-on/note-off pairing)
            Map<Integer, NoteEvent> activeNotes = new HashMap<>(); // key: (channel << 8) | noteNumber
            
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                long tick = event.getTick();
                Double timeSeconds = useSeconds ? ticksToSeconds(tick, division, tempoMap) : null;
                
                if (message instanceof ShortMessage) {
                    ShortMessage sm = (ShortMessage) message;
                    int command = sm.getCommand();
                    int channel = sm.getChannel();
                    
                    switch (command) {
                        case ShortMessage.NOTE_ON:
                            int noteNumber = sm.getData1();
                            int velocity = sm.getData2();
                            
                            if (velocity == 0) {
                                // Note On with velocity 0 = Note Off
                                handleNoteOff(trackOutput, activeNotes, channel, noteNumber, 
                                            tick, timeSeconds, division, tempoMap);
                            } else {
                                // Start a new note
                                String noteName = NoteUtils.noteNumberToName(noteNumber);
                                NoteEvent noteEvent = new NoteEvent(tick, timeSeconds, channel, 
                                                                    noteNumber, noteName, velocity);
                                int key = (channel << 8) | noteNumber;
                                activeNotes.put(key, noteEvent);
                            }
                            break;
                            
                        case ShortMessage.NOTE_OFF:
                            noteNumber = sm.getData1();
                            handleNoteOff(trackOutput, activeNotes, channel, noteNumber, 
                                        tick, timeSeconds, division, tempoMap);
                            break;
                            
                        case ShortMessage.CONTROL_CHANGE:
                            int controller = sm.getData1();
                            int value = sm.getData2();
                            trackOutput.getEvents().add(
                                new ControlChangeEvent(tick, timeSeconds, channel, controller, value)
                            );
                            break;
                            
                        case ShortMessage.PROGRAM_CHANGE:
                            int program = sm.getData1();
                            trackOutput.getEvents().add(
                                new ProgramChangeEvent(tick, timeSeconds, channel, program)
                            );
                            if (!trackOutput.getProgramChanges().contains(program)) {
                                trackOutput.getProgramChanges().add(program);
                            }
                            break;
                            
                        case ShortMessage.PITCH_BEND:
                            int lsb = sm.getData1();
                            int msb = sm.getData2();
                            int bendValue = (msb << 7) | lsb;
                            trackOutput.getEvents().add(
                                new PitchBendEvent(tick, timeSeconds, channel, bendValue)
                            );
                            break;
                    }
                    
                } else if (message instanceof MetaMessage && includeMeta) {
                    MetaMessage meta = (MetaMessage) message;
                    int type = meta.getType();
                    byte[] data = meta.getData();
                    
                    switch (type) {
                        case 0x03: // Track/Sequence Name
                            String name = new String(data, StandardCharsets.ISO_8859_1);
                            trackOutput.setName(name);
                            if (includeMeta) {
                                trackOutput.getEvents().add(
                                    new MetaEvent(tick, timeSeconds, type, "track_name", name)
                                );
                            }
                            break;
                            
                        case 0x04: // Instrument Name
                            if (includeMeta) {
                                String instrument = new String(data, StandardCharsets.ISO_8859_1);
                                trackOutput.getEvents().add(
                                    new MetaEvent(tick, timeSeconds, type, "instrument_name", instrument)
                                );
                            }
                            break;
                            
                        case 0x05: // Lyric
                            if (includeMeta) {
                                String lyric = new String(data, StandardCharsets.ISO_8859_1);
                                trackOutput.getEvents().add(
                                    new MetaEvent(tick, timeSeconds, type, "lyric", lyric)
                                );
                            }
                            break;
                            
                        case 0x06: // Marker
                            if (includeMeta) {
                                String marker = new String(data, StandardCharsets.ISO_8859_1);
                                trackOutput.getEvents().add(
                                    new MetaEvent(tick, timeSeconds, type, "marker", marker)
                                );
                            }
                            break;
                            
                        case 0x58: // Time Signature
                            if (data.length >= 2) {
                                int numerator = data[0] & 0xFF;
                                int denominatorPower = data[1] & 0xFF;
                                int denominator = (int) Math.pow(2, denominatorPower);
                                TimeSignatureEntry timeSig = new TimeSignatureEntry(tick, numerator, denominator);
                                metadata.getTimeSignatures().add(timeSig);
                                if (includeMeta) {
                                    trackOutput.getEvents().add(
                                        new MetaEvent(tick, timeSeconds, type, "time_signature", 
                                                    numerator + "/" + denominator)
                                    );
                                }
                            }
                            break;
                            
                        case 0x59: // Key Signature
                            if (data.length >= 2) {
                                int sf = data[0]; // sharps/flats
                                int mi = data[1] & 0xFF; // major/minor
                                String scale = (mi == 0) ? "major" : "minor";
                                KeySignatureEntry keySig = new KeySignatureEntry(tick, sf, scale);
                                metadata.getKeySignatures().add(keySig);
                                if (includeMeta) {
                                    trackOutput.getEvents().add(
                                        new MetaEvent(tick, timeSeconds, type, "key_signature", 
                                                    sf + " " + scale)
                                    );
                                }
                            }
                            break;
                            
                        case 0x2F: // End of Track
                            if (includeMeta) {
                                trackOutput.getEvents().add(
                                    new MetaEvent(tick, timeSeconds, type, "end_of_track", "")
                                );
                            }
                            break;
                            
                        case 0x51: // Tempo (already handled in buildTempoMap)
                            // Skip to avoid duplication, already in metadata.tempoMap
                            break;
                            
                        default:
                            if (includeMeta) {
                                String dataStr = bytesToHex(data);
                                trackOutput.getEvents().add(
                                    new MetaEvent(tick, timeSeconds, type, "unknown_meta", dataStr)
                                );
                            }
                            break;
                    }
                }
            }
            
            output.getTracks().add(trackOutput);
        }
    }

    private void handleNoteOff(TrackOutput trackOutput, Map<Integer, NoteEvent> activeNotes,
                              int channel, int noteNumber, long offTick, Double offTimeSeconds,
                              int division, List<TempoEntry> tempoMap) {
        int key = (channel << 8) | noteNumber;
        NoteEvent noteEvent = activeNotes.remove(key);
        
        if (noteEvent != null) {
            long durationTicks = offTick - noteEvent.getTick();
            noteEvent.setDurationTicks(durationTicks);
            
            if (useSeconds && offTimeSeconds != null && noteEvent.getTimeSeconds() != null) {
                double durationSeconds = offTimeSeconds - noteEvent.getTimeSeconds();
                noteEvent.setDurationSeconds(durationSeconds);
            }
            
            trackOutput.getEvents().add(noteEvent);
        }
    }

    private double ticksToSeconds(long tick, int division, List<TempoEntry> tempoMap) {
        if (tempoMap.isEmpty()) {
            return 0.0;
        }
        
        double timeSeconds = 0.0;
        long currentTick = 0;
        int currentTempo = tempoMap.get(0).getMicrosecondsPerQuarter();
        
        for (int i = 0; i < tempoMap.size(); i++) {
            TempoEntry entry = tempoMap.get(i);
            
            if (tick <= entry.getTick()) {
                break;
            }
            
            // Calculate time from currentTick to this tempo change
            long deltaTicks = entry.getTick() - currentTick;
            double deltaSeconds = (deltaTicks * currentTempo) / (division * 1_000_000.0);
            timeSeconds += deltaSeconds;
            
            currentTick = entry.getTick();
            currentTempo = entry.getMicrosecondsPerQuarter();
        }
        
        // Calculate remaining time from last tempo change to target tick
        long deltaTicks = tick - currentTick;
        double deltaSeconds = (deltaTicks * currentTempo) / (division * 1_000_000.0);
        timeSeconds += deltaSeconds;
        
        return timeSeconds;
    }

    private long getMaxTick(Sequence sequence) {
        long maxTick = 0;
        for (Track track : sequence.getTracks()) {
            if (track.size() > 0) {
                long lastTick = track.get(track.size() - 1).getTick();
                maxTick = Math.max(maxTick, lastTick);
            }
        }
        return maxTick;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
