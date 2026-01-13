package com.marcoalmeida.midi_tokenizer.midi;

import com.marcoalmeida.midi_tokenizer.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Core MIDI parser that reads Standard MIDI Files (types 0 and 1)
 * and converts them to an LLM-friendly JSON representation.
 */
@Component
public class MidiParser {
    
    private static final int NOTE_ON = 0x90;
    private static final int NOTE_OFF = 0x80;
    private static final int PROGRAM_CHANGE = 0xC0;
    private static final int CONTROL_CHANGE = 0xB0;
    private static final int PITCH_BEND = 0xE0;
    
    private static final int META_SEQUENCE_NUMBER = 0x00;
    private static final int META_TEXT = 0x01;
    private static final int META_COPYRIGHT = 0x02;
    private static final int META_TRACK_NAME = 0x03;
    private static final int META_INSTRUMENT_NAME = 0x04;
    private static final int META_LYRIC = 0x05;
    private static final int META_MARKER = 0x06;
    private static final int META_CUE_POINT = 0x07;
    private static final int META_CHANNEL_PREFIX = 0x20;
    private static final int META_END_OF_TRACK = 0x2F;
    private static final int META_SET_TEMPO = 0x51;
    private static final int META_SMPTE_OFFSET = 0x54;
    private static final int META_TIME_SIGNATURE = 0x58;
    private static final int META_KEY_SIGNATURE = 0x59;
    
    private static final long DEFAULT_TEMPO_MICROSECONDS_PER_QUARTER = 500000; // 120 BPM
    
    private final ObjectMapper objectMapper;
    
    public MidiParser() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    /**
     * Parse a MIDI file and return the JSON output as a string.
     */
    public String parseToJson(File file, boolean includeTimeSeconds, boolean includeMeta) throws Exception {
        MidiOutput output = parse(file, includeTimeSeconds, includeMeta);
        return objectMapper.writeValueAsString(output);
    }
    
    /**
     * Parse a MIDI file and return the structured output.
     */
    public MidiOutput parse(File file, boolean includeTimeSeconds, boolean includeMeta) throws Exception {
        Sequence sequence = MidiSystem.getSequence(file);
        
        MidiOutput output = new MidiOutput();
        
        // File metadata
        int format = getSequenceFormat(sequence);
        int division = sequence.getResolution();
        
        // Build tempo map
        List<TempoEntry> tempoMap = buildTempoMap(sequence);
        output.getMetadata().setTempoMap(tempoMap);
        
        // Calculate total duration
        long maxTick = calculateMaxTick(sequence);
        double durationSeconds = includeTimeSeconds ? ticksToSeconds(maxTick, division, tempoMap) : 0.0;
        
        FileMetadata fileMetadata = new FileMetadata(
            file.getName(),
            format,
            division,
            maxTick,
            durationSeconds
        );
        output.setFile(fileMetadata);
        
        // Parse tracks
        Track[] tracks = sequence.getTracks();
        for (int i = 0; i < tracks.length; i++) {
            TrackOutput trackOutput = parseTrack(tracks[i], i, division, tempoMap, includeTimeSeconds, includeMeta);
            output.getTracks().add(trackOutput);
        }
        
        // Extract time and key signatures
        extractGlobalMetadata(sequence, output.getMetadata());
        
        return output;
    }
    
    private int getSequenceFormat(Sequence sequence) {
        float divisionType = sequence.getDivisionType();
        if (divisionType == Sequence.PPQ) {
            // PPQ sequences can be type 0 or 1; we'll determine by track count
            return sequence.getTracks().length == 1 ? 0 : 1;
        }
        return 1; // SMPTE-based sequences are typically type 1
    }
    
    private List<TempoEntry> buildTempoMap(Sequence sequence) {
        List<TempoEntry> tempoMap = new ArrayList<>();
        
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                
                if (message instanceof MetaMessage) {
                    MetaMessage meta = (MetaMessage) message;
                    if (meta.getType() == META_SET_TEMPO) {
                        byte[] data = meta.getData();
                        long microsecondsPerQuarter = ((data[0] & 0xFF) << 16) | 
                                                     ((data[1] & 0xFF) << 8) | 
                                                     (data[2] & 0xFF);
                        double bpm = 60000000.0 / microsecondsPerQuarter;
                        
                        tempoMap.add(new TempoEntry(event.getTick(), microsecondsPerQuarter, bpm));
                    }
                }
            }
        }
        
        // If no tempo events, add default
        if (tempoMap.isEmpty()) {
            tempoMap.add(new TempoEntry(0, DEFAULT_TEMPO_MICROSECONDS_PER_QUARTER, 120.0));
        }
        
        // Sort by tick
        tempoMap.sort(Comparator.comparingLong(TempoEntry::getTick));
        
        return tempoMap;
    }
    
    private void extractGlobalMetadata(Sequence sequence, Metadata metadata) {
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                
                if (message instanceof MetaMessage) {
                    MetaMessage meta = (MetaMessage) message;
                    
                    if (meta.getType() == META_TIME_SIGNATURE) {
                        byte[] data = meta.getData();
                        int numerator = data[0] & 0xFF;
                        int denominator = 1 << (data[1] & 0xFF);
                        int clocksPerClick = data[2] & 0xFF;
                        int thirtySecondsPer24Clocks = data[3] & 0xFF;
                        
                        metadata.getTimeSignatures().add(new TimeSignatureEntry(
                            event.getTick(), numerator, denominator, 
                            clocksPerClick, thirtySecondsPer24Clocks
                        ));
                    } else if (meta.getType() == META_KEY_SIGNATURE) {
                        byte[] data = meta.getData();
                        int sharpsFlats = data[0]; // Signed byte
                        int majorMinor = data[1] & 0xFF;
                        
                        metadata.getKeySignatures().add(new KeySignatureEntry(
                            event.getTick(), sharpsFlats, majorMinor
                        ));
                    }
                }
            }
        }
    }
    
    private TrackOutput parseTrack(Track track, int index, int division, 
                                   List<TempoEntry> tempoMap, boolean includeTimeSeconds,
                                   boolean includeMeta) {
        TrackOutput trackOutput = new TrackOutput(index);
        
        // Map to track note-on events: key is (channel << 8) | noteNumber
        Map<Integer, NoteOnInfo> activeNotes = new HashMap<>();
        
        for (int i = 0; i < track.size(); i++) {
            MidiEvent event = track.get(i);
            MidiMessage message = event.getMessage();
            
            if (message instanceof ShortMessage) {
                ShortMessage sm = (ShortMessage) message;
                int command = sm.getCommand();
                int channel = sm.getChannel();
                
                if (command == NOTE_ON) {
                    int noteNumber = sm.getData1();
                    int velocity = sm.getData2();
                    
                    if (velocity == 0) {
                        // Note-on with velocity 0 is a note-off
                        handleNoteOff(activeNotes, trackOutput, event.getTick(), channel, 
                                    noteNumber, division, tempoMap, includeTimeSeconds);
                    } else {
                        // Actual note-on
                        int key = (channel << 8) | noteNumber;
                        activeNotes.put(key, new NoteOnInfo(event.getTick(), velocity));
                    }
                } else if (command == NOTE_OFF) {
                    int noteNumber = sm.getData1();
                    handleNoteOff(activeNotes, trackOutput, event.getTick(), channel, 
                                noteNumber, division, tempoMap, includeTimeSeconds);
                } else if (command == PROGRAM_CHANGE) {
                    int program = sm.getData1();
                    trackOutput.getProgramChanges().add(
                        new ProgramChangeEvent(event.getTick(), channel, program)
                    );
                } else if (command == CONTROL_CHANGE) {
                    EventOutput eventOutput = new EventOutput();
                    eventOutput.setType("control_change");
                    eventOutput.setTick(event.getTick());
                    eventOutput.setChannel(channel);
                    eventOutput.setController(sm.getData1());
                    eventOutput.setValue(sm.getData2());
                    
                    if (includeTimeSeconds) {
                        eventOutput.setTimeSeconds(ticksToSeconds(event.getTick(), division, tempoMap));
                    }
                    
                    trackOutput.getEvents().add(eventOutput);
                } else if (command == PITCH_BEND) {
                    int lsb = sm.getData1();
                    int msb = sm.getData2();
                    int pitchBend = (msb << 7) | lsb;
                    
                    EventOutput eventOutput = new EventOutput();
                    eventOutput.setType("pitch_bend");
                    eventOutput.setTick(event.getTick());
                    eventOutput.setChannel(channel);
                    eventOutput.setPitchBend(pitchBend - 8192); // Center at 0
                    
                    if (includeTimeSeconds) {
                        eventOutput.setTimeSeconds(ticksToSeconds(event.getTick(), division, tempoMap));
                    }
                    
                    trackOutput.getEvents().add(eventOutput);
                }
            } else if (includeMeta && message instanceof MetaMessage) {
                MetaMessage meta = (MetaMessage) message;
                processMetaMessage(meta, event.getTick(), trackOutput, division, tempoMap, includeTimeSeconds);
            }
        }
        
        return trackOutput;
    }
    
    private void handleNoteOff(Map<Integer, NoteOnInfo> activeNotes, TrackOutput trackOutput,
                              long offTick, int channel, int noteNumber, int division,
                              List<TempoEntry> tempoMap, boolean includeTimeSeconds) {
        int key = (channel << 8) | noteNumber;
        NoteOnInfo noteOn = activeNotes.remove(key);
        
        if (noteOn != null) {
            long durationTicks = offTick - noteOn.tick;
            
            EventOutput eventOutput = new EventOutput();
            eventOutput.setType("note");
            eventOutput.setTick(noteOn.tick);
            eventOutput.setChannel(channel);
            eventOutput.setNoteNumber(noteNumber);
            eventOutput.setNoteName(NoteUtils.noteNumberToName(noteNumber));
            eventOutput.setVelocity(noteOn.velocity);
            eventOutput.setDurationTicks(durationTicks);
            
            if (includeTimeSeconds) {
                double startTime = ticksToSeconds(noteOn.tick, division, tempoMap);
                double endTime = ticksToSeconds(offTick, division, tempoMap);
                eventOutput.setTimeSeconds(startTime);
                eventOutput.setDurationSeconds(endTime - startTime);
            }
            
            trackOutput.getEvents().add(eventOutput);
        }
    }
    
    private void processMetaMessage(MetaMessage meta, long tick, TrackOutput trackOutput,
                                   int division, List<TempoEntry> tempoMap, boolean includeTimeSeconds) {
        int type = meta.getType();
        byte[] data = meta.getData();
        
        String text = null;
        String metaType = null;
        
        switch (type) {
            case META_TRACK_NAME:
                metaType = "track_name";
                text = new String(data);
                trackOutput.setName(text);
                break;
            case META_INSTRUMENT_NAME:
                metaType = "instrument_name";
                text = new String(data);
                break;
            case META_TEXT:
                metaType = "text";
                text = new String(data);
                break;
            case META_COPYRIGHT:
                metaType = "copyright";
                text = new String(data);
                break;
            case META_LYRIC:
                metaType = "lyric";
                text = new String(data);
                break;
            case META_MARKER:
                metaType = "marker";
                text = new String(data);
                break;
            case META_CUE_POINT:
                metaType = "cue_point";
                text = new String(data);
                break;
            default:
                return; // Skip other meta events
        }
        
        if (metaType != null) {
            EventOutput eventOutput = new EventOutput();
            eventOutput.setType("meta");
            eventOutput.setTick(tick);
            eventOutput.setText(metaType + ": " + (text != null ? text : ""));
            
            if (includeTimeSeconds) {
                eventOutput.setTimeSeconds(ticksToSeconds(tick, division, tempoMap));
            }
            
            trackOutput.getEvents().add(eventOutput);
        }
    }
    
    private long calculateMaxTick(Sequence sequence) {
        long maxTick = 0;
        for (Track track : sequence.getTracks()) {
            if (track.size() > 0) {
                long trackEnd = track.get(track.size() - 1).getTick();
                maxTick = Math.max(maxTick, trackEnd);
            }
        }
        return maxTick;
    }
    
    private double ticksToSeconds(long ticks, int division, List<TempoEntry> tempoMap) {
        double seconds = 0.0;
        long currentTick = 0;
        
        for (int i = 0; i < tempoMap.size(); i++) {
            TempoEntry tempo = tempoMap.get(i);
            long nextTick = (i + 1 < tempoMap.size()) ? tempoMap.get(i + 1).getTick() : ticks;
            
            if (ticks <= tempo.getTick()) {
                break;
            }
            
            if (nextTick > ticks) {
                nextTick = ticks;
            }
            
            long ticksInSegment = nextTick - Math.max(currentTick, tempo.getTick());
            double secondsInSegment = (ticksInSegment * tempo.getMicrosecondsPerQuarter()) / 
                                     (division * 1000000.0);
            seconds += secondsInSegment;
            currentTick = nextTick;
        }
        
        // Handle ticks before first tempo change
        if (currentTick < ticks && (tempoMap.isEmpty() || tempoMap.get(0).getTick() > 0)) {
            long firstTempoTick = tempoMap.isEmpty() ? ticks : Math.min(ticks, tempoMap.get(0).getTick());
            long ticksInSegment = firstTempoTick - currentTick;
            double secondsInSegment = (ticksInSegment * DEFAULT_TEMPO_MICROSECONDS_PER_QUARTER) / 
                                     (division * 1000000.0);
            seconds = secondsInSegment + seconds;
        }
        
        return seconds;
    }
    
    private static class NoteOnInfo {
        final long tick;
        final int velocity;
        
        NoteOnInfo(long tick, int velocity) {
            this.tick = tick;
            this.velocity = velocity;
        }
    }
}
