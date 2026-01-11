package com.marcoalmeida.midi_tokenizer.midi;

import com.marcoalmeida.midi_tokenizer.model.*;
import com.marcoalmeida.midi_tokenizer.util.NoteUtils;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Parser for Standard MIDI Files (SMF) type 0 and 1.
 * Extracts events, metadata, pairs Note On/Off events, and converts ticks to seconds.
 */
public class MidiParser {

    /**
     * Parse a MIDI file and return a structured representation.
     */
    public MidiFile parse(File file) throws InvalidMidiDataException, IOException {
        Sequence sequence = MidiSystem.getSequence(file);
        return parseSequence(sequence);
    }

    /**
     * Parse a MIDI Sequence (useful for testing).
     */
    public MidiFile parseSequence(Sequence sequence) {
        int format = 1; // Assume format 1 if we have multiple tracks
        if (sequence.getTracks().length == 1) {
            format = 0;
        }

        int division = sequence.getResolution();
        TempoMap tempoMap = extractTempoMap(sequence);
        List<com.marcoalmeida.midi_tokenizer.model.Track> tracks = new ArrayList<>();

        javax.sound.midi.Track[] midiTracks = sequence.getTracks();
        for (int i = 0; i < midiTracks.length; i++) {
            tracks.add(parseTrack(midiTracks[i], i, division, tempoMap));
        }

        return new MidiFile(format, division, tracks, tempoMap);
    }

    /**
     * Extract tempo map from all tracks.
     */
    private TempoMap extractTempoMap(Sequence sequence) {
        TempoMap tempoMap = new TempoMap();

        for (javax.sound.midi.Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();

                if (message instanceof MetaMessage) {
                    MetaMessage meta = (MetaMessage) message;
                    if (meta.getType() == 0x51) { // Set Tempo
                        byte[] data = meta.getData();
                        int mpqn = ((data[0] & 0xff) << 16) | ((data[1] & 0xff) << 8) | (data[2] & 0xff);
                        tempoMap.addChange(event.getTick(), mpqn);
                    }
                }
            }
        }

        return tempoMap;
    }

    /**
     * Parse a single track.
     */
    private com.marcoalmeida.midi_tokenizer.model.Track parseTrack(javax.sound.midi.Track midiTrack, int trackNumber, int division, TempoMap tempoMap) {
        String trackName = "Track " + trackNumber;
        List<Note> notes = new ArrayList<>();
        List<MetaEvent> metaEvents = new ArrayList<>();
        Map<Integer, NoteOnEvent> activeNotes = new HashMap<>();

        for (int i = 0; i < midiTrack.size(); i++) {
            MidiEvent event = midiTrack.get(i);
            MidiMessage message = event.getMessage();

            if (message instanceof ShortMessage) {
                ShortMessage sm = (ShortMessage) message;
                int command = sm.getCommand();

                if (command == ShortMessage.NOTE_ON) {
                    int note = sm.getData1();
                    int velocity = sm.getData2();
                    int channel = sm.getChannel();

                    if (velocity > 0) {
                        // Real note on
                        int key = (channel << 8) | note;
                        activeNotes.put(key, new NoteOnEvent(event.getTick(), note, velocity, channel));
                    } else {
                        // Note on with velocity 0 is actually note off
                        int key = (channel << 8) | note;
                        NoteOnEvent noteOn = activeNotes.remove(key);
                        if (noteOn != null) {
                            notes.add(createNote(noteOn, event.getTick(), division, tempoMap));
                        }
                    }
                } else if (command == ShortMessage.NOTE_OFF) {
                    int note = sm.getData1();
                    int channel = sm.getChannel();
                    int key = (channel << 8) | note;
                    NoteOnEvent noteOn = activeNotes.remove(key);
                    if (noteOn != null) {
                        notes.add(createNote(noteOn, event.getTick(), division, tempoMap));
                    }
                }
            } else if (message instanceof MetaMessage) {
                MetaMessage meta = (MetaMessage) message;
                double time = tempoMap.ticksToSeconds(event.getTick(), division);

                switch (meta.getType()) {
                    case 0x03: // Track name
                        trackName = new String(meta.getData());
                        metaEvents.add(new MetaEvent("trackName", time, trackName));
                        break;
                    case 0x01: // Text event
                        metaEvents.add(new MetaEvent("text", time, new String(meta.getData())));
                        break;
                    case 0x02: // Copyright
                        metaEvents.add(new MetaEvent("copyright", time, new String(meta.getData())));
                        break;
                    case 0x04: // Instrument name
                        metaEvents.add(new MetaEvent("instrumentName", time, new String(meta.getData())));
                        break;
                    case 0x05: // Lyric
                        metaEvents.add(new MetaEvent("lyric", time, new String(meta.getData())));
                        break;
                    case 0x06: // Marker
                        metaEvents.add(new MetaEvent("marker", time, new String(meta.getData())));
                        break;
                    case 0x51: // Set tempo
                        byte[] data = meta.getData();
                        int mpqn = ((data[0] & 0xff) << 16) | ((data[1] & 0xff) << 8) | (data[2] & 0xff);
                        double bpm = 60000000.0 / mpqn;
                        metaEvents.add(new MetaEvent("tempo", time, String.format("%.2f BPM", bpm)));
                        break;
                    case 0x58: // Time signature
                        metaEvents.add(new MetaEvent("timeSignature", time, 
                            meta.getData()[0] + "/" + (1 << meta.getData()[1])));
                        break;
                }
            }
        }

        return new com.marcoalmeida.midi_tokenizer.model.Track(trackNumber, trackName, notes, metaEvents);
    }

    /**
     * Create a Note from NoteOn and NoteOff events.
     */
    private Note createNote(NoteOnEvent noteOn, long offTick, int division, TempoMap tempoMap) {
        double startTime = tempoMap.ticksToSeconds(noteOn.tick, division);
        double endTime = tempoMap.ticksToSeconds(offTick, division);
        double duration = endTime - startTime;

        String noteName = NoteUtils.midiNoteToName(noteOn.note);

        return new Note(noteOn.note, noteName, noteOn.velocity, startTime, duration, noteOn.channel);
    }

    /**
     * Helper class to store Note On event data.
     */
    private static class NoteOnEvent {
        final long tick;
        final int note;
        final int velocity;
        final int channel;

        NoteOnEvent(long tick, int note, int velocity, int channel) {
            this.tick = tick;
            this.note = note;
            this.velocity = velocity;
            this.channel = channel;
        }
    }
}
