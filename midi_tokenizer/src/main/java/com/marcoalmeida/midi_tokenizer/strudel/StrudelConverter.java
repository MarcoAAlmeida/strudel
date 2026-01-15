package com.marcoalmeida.midi_tokenizer.strudel;

import com.marcoalmeida.midi_tokenizer.midi.MidiParser;
import com.marcoalmeida.midi_tokenizer.model.EventOutput;
import com.marcoalmeida.midi_tokenizer.model.MidiOutput;
import com.marcoalmeida.midi_tokenizer.model.ProgramChangeEvent;
import com.marcoalmeida.midi_tokenizer.model.TempoEntry;
import com.marcoalmeida.midi_tokenizer.model.TrackOutput;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main service for converting MIDI files to Strudel patterns.
 */
@Service
public class StrudelConverter {

    private final MidiParser midiParser;

    public StrudelConverter(MidiParser midiParser) {
        this.midiParser = midiParser;
    }

    /**
     * Converts a MIDI file to a Strudel pattern.
     *
     * @param inputPath Path to MIDI (.mid) or JSON (.json) file
     * @param options   Conversion options
     * @return Strudel pattern file content
     * @throws IOException              if file reading fails
     * @throws IllegalArgumentException if track is empty or invalid
     */
    public String convert(String inputPath, ConversionOptions options) throws IOException {
        // Parse MIDI or load JSON
        MidiOutput midiOutput = loadMidiData(inputPath);

        // Check if should process all tracks or single track
        if (options.shouldProcessAllTracks()) {
            return convertAllTracks(midiOutput, inputPath, options);
        } else {
            return convertSingleTrack(midiOutput, inputPath, options);
        }
    }

    /**
     * Convert a single track (Phase 1.9 behavior).
     */
    private String convertSingleTrack(MidiOutput midiOutput, String inputPath, ConversionOptions options) throws IOException {
        // Select track
        int trackIndex = options.getEffectiveTrackIndex();
        if (trackIndex >= midiOutput.getTracks().size()) {
            throw new IllegalArgumentException(
                String.format("Track index %d out of bounds. File has %d track(s).",
                    trackIndex, midiOutput.getTracks().size())
            );
        }

        TrackOutput track = midiOutput.getTracks().get(trackIndex);

        // Extract note events
        List<EventOutput> noteEvents = track.getEvents().stream()
            .filter(event -> "note".equals(event.getType()))
            .collect(Collectors.toList());

        if (noteEvents.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("Track %d (%s) has no note events.",
                    trackIndex, track.getName())
            );
        }

        // Determine tempo
        double bpm = determineTempo(midiOutput, options);

        // Get time signature and validate
        TimeSignatureInfo timeSig = validateAndGetTimeSignature(midiOutput);
        int timeSignatureNumerator = timeSig.numerator();
        int timeSignatureDenominator = timeSig.denominator();

        // Get effective quantization (override or smart default)
        int quantization = options.getEffectiveQuantization(timeSignatureNumerator, timeSignatureDenominator);
        
        // Calculate slices per measure
        int slicesPerMeasure = (quantization * timeSignatureNumerator) / timeSignatureDenominator;

        // Convert to quantized cycle-based pattern (Phase 1.9: with polyphony toggle)
        boolean polyphonicMode = options.isPolyphonicMode();
        
        // Calculate measures needed for this track
        int totalMeasures = calculateMeasuresNeeded(noteEvents, 
            (int) Math.round(bpm), quantization, timeSignatureNumerator, timeSignatureDenominator);
        
        String pattern = RhythmConverter.toQuantizedCyclePattern(
            noteEvents,
            midiOutput.getFile().getDivision(),
            timeSignatureNumerator,
            timeSignatureDenominator,
            quantization,
            (int) Math.round(bpm),
            polyphonicMode,  // Phase 1.9: polyphony flag
            totalMeasures
        );

        // Determine instrument
        String instrument = determineInstrument(track);

        // Generate pattern name from track index
        String patternName = "track_" + trackIndex;

        // Calculate beats per cycle (for 4/4 time = 4 beats, for 3/4 time = 3 beats, etc.)
        int beatsPerCycle = timeSignatureNumerator * (4 / timeSignatureDenominator);

        // Calculate grid meaning description
        String gridMeaning = generateGridMeaning(quantization, slicesPerMeasure);
        
        // Determine if quantization is default or override
        String quantizationSource = options.quantization() != null ? "override" : "default";

        // Render template (Phase 1.9: with polyphonic mode)
        return StrudelTemplate.render(
            patternName,
            Path.of(inputPath).getFileName().toString(),
            bpm,
            beatsPerCycle,
            trackIndex,
            track.getName(),
            timeSignatureNumerator,
            timeSignatureDenominator,
            quantization,
            quantizationSource,
            gridMeaning,
            slicesPerMeasure,
            pattern,
            instrument,
            polyphonicMode  // Phase 1.9: polyphonic mode flag
        );
    }

    /**
     * Convert all non-empty tracks (Phase 2 behavior).
     */
    private String convertAllTracks(MidiOutput midiOutput, String inputPath, ConversionOptions options) throws IOException {
        // Validate single time signature (existing validation)
        TimeSignatureInfo timeSig = validateAndGetTimeSignature(midiOutput);
        
        // Determine tempo
        double bpm = determineTempo(midiOutput, options);
        
        // Get effective quantization
        int quantization = options.getEffectiveQuantization(timeSig.numerator(), timeSig.denominator());
        
        // First pass: collect all track note events and calculate global measure count
        int globalMeasures = 0;
        List<TrackNoteData> trackDataList = new ArrayList<>();
        
        for (int i = 0; i < midiOutput.getTracks().size(); i++) {
            TrackOutput track = midiOutput.getTracks().get(i);
            
            // Extract note events
            List<EventOutput> noteEvents = track.getEvents().stream()
                .filter(event -> "note".equals(event.getType()))
                .collect(Collectors.toList());
            
            if (noteEvents.isEmpty()) {
                continue;  // Skip empty tracks
            }
            
            // Calculate measures needed for this track
            int trackMeasures = calculateMeasuresNeeded(noteEvents, 
                (int) Math.round(bpm), quantization, timeSig.numerator(), timeSig.denominator());
            
            if (trackMeasures > globalMeasures) {
                globalMeasures = trackMeasures;
            }
            
            trackDataList.add(new TrackNoteData(i, track, noteEvents));
        }
        
        // Error if all tracks empty
        if (trackDataList.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("No tracks with note events found. File has %d track(s) but all are empty.",
                    midiOutput.getTracks().size())
            );
        }
        
        // Second pass: convert all tracks with global measure count
        List<StrudelTemplate.TrackPattern> trackPatterns = new ArrayList<>();
        for (TrackNoteData trackData : trackDataList) {
            // Determine instrument from program change
            String instrument = determineInstrumentWithMapper(trackData.track);
            
            // Convert pattern with global measure count for synchronization
            String pattern = RhythmConverter.toQuantizedCyclePattern(
                trackData.noteEvents,
                midiOutput.getFile().getDivision(),
                timeSig.numerator(),
                timeSig.denominator(),
                quantization,
                (int) Math.round(bpm),
                options.isPolyphonicMode(),
                globalMeasures  // All tracks use same measure count
            );
            
            trackPatterns.add(new StrudelTemplate.TrackPattern(
                trackData.index, trackData.track.getName(), instrument, pattern));
        }
        
        // Render multi-track template
        return StrudelTemplate.renderMultiTrack(
            Path.of(inputPath).getFileName().toString(),
            bpm,
            timeSig.numerator(),
            timeSig.denominator(),
            quantization,
            midiOutput.getTracks().size(),
            trackPatterns,
            options.isPolyphonicMode()
        );
    }

    private MidiOutput loadMidiData(String inputPath) throws IOException {
        try {
            File inputFile = new File(inputPath);
            if (inputPath.endsWith(".json")) {
                // Load from JSON file
                return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(inputFile, MidiOutput.class);
            } else {
                // Parse MIDI file
                return midiParser.parse(inputFile, true, true);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to load MIDI data: " + e.getMessage(), e);
        }
    }

    private double determineTempo(MidiOutput midiOutput, ConversionOptions options) {
        if (options.overrideTempo() != null) {
            return options.overrideTempo();
        }

        // Get first tempo from tempo map
        if (!midiOutput.getMetadata().getTempoMap().isEmpty()) {
            TempoEntry firstTempo = midiOutput.getMetadata().getTempoMap().get(0);
            return firstTempo.getBpm();
        }

        // Default to 120 BPM
        return 120.0;
    }

    /**
     * Validate single time signature and return time signature info.
     */
    private TimeSignatureInfo validateAndGetTimeSignature(MidiOutput midiOutput) {
        int numerator = 4;
        int denominator = 4;
        
        if (!midiOutput.getMetadata().getTimeSignatures().isEmpty()) {
            // Validate single time signature only
            if (midiOutput.getMetadata().getTimeSignatures().size() > 1) {
                List<Long> tickPositions = midiOutput.getMetadata().getTimeSignatures().stream()
                    .map(ts -> ts.getTick())
                    .collect(Collectors.toList());
                throw new UnsupportedOperationException(
                    String.format(
                        "Multiple time signatures detected (%d changes at ticks: %s). " +
                        "Only single time signature files are supported. " +
                        "Split your MIDI file by time signature before conversion.",
                        midiOutput.getMetadata().getTimeSignatures().size(),
                        tickPositions.stream().map(String::valueOf).collect(Collectors.joining(", "))
                    )
                );
            }
            var timeSignature = midiOutput.getMetadata().getTimeSignatures().get(0);
            numerator = timeSignature.getNumerator();
            denominator = timeSignature.getDenominator();
        }
        
        return new TimeSignatureInfo(numerator, denominator);
    }

    /**
     * Determine instrument using GM mapper (Phase 2).
     */
    private String determineInstrumentWithMapper(TrackOutput track) {
        List<ProgramChangeEvent> programChanges = track.getProgramChanges();
        
        if (programChanges.isEmpty()) {
            return "piano";  // Default fallback
        }
        
        // Use first program change (ignore mid-track changes)
        ProgramChangeEvent firstProgram = programChanges.get(0);
        return GMInstrumentMapper.map(firstProgram.getProgram(), firstProgram.getChannel());
    }

    /**
     * Determine instrument - simple mapping for Phase 1 (kept for single-track mode).
     */
    private String determineInstrument(TrackOutput track) {
        // Phase 1: Simple mapping - program 0 = piano, others = triangle
        if (track.getProgramChanges().isEmpty()) {
            return "piano";
        }

        int program = track.getProgramChanges().get(0).getProgram();
        return program == 0 ? "piano" : "triangle";
    }

    private String generateGridMeaning(int quantization, int slicesPerMeasure) {
        // Generate meaningful description of what the grid represents
        // Examples: "16 = sixteenth notes, @4 = quarter note, @8 = half note"
        
        if (quantization == 6) {
            return "6 = eighth notes, @3 = dotted quarter, @6 = dotted half";
        } else if (quantization == 8) {
            return "8 = eighth notes, @4 = half note";
        } else if (quantization == 12) {
            return "12 = triplet eighths, @3 = quarter note, @6 = half note";
        } else if (quantization == 16) {
            return "16 = sixteenth notes, @4 = quarter note, @8 = half note";
        } else if (quantization == 24) {
            return "24 = triplet sixteenths, @6 = quarter note, @12 = half note";
        } else if (quantization == 32) {
            return "32 = thirty-second notes, @8 = quarter note, @16 = half note";
        } else {
            // Generic description
            int quarterNote = quantization / 4;
            int halfNote = quantization / 2;
            return String.format("%d slices per measure, @%d = quarter note, @%d = half note", 
                slicesPerMeasure, quarterNote, halfNote);
        }
    }

    private String generatePatternName(String inputPath) {
        String filename = Path.of(inputPath).getFileName().toString();
        // Remove extension
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            filename = filename.substring(0, dotIndex);
        }
        // Convert to valid identifier (replace spaces/hyphens with underscores)
        return filename.replaceAll("[\\s-]+", "_");
    }

    /**
     * Calculate number of measures needed for a track based on its note events.
     */
    private int calculateMeasuresNeeded(
        List<EventOutput> noteEvents, 
        int tempo, 
        int quantization,
        int numerator,
        int denominator
    ) {
        if (noteEvents.isEmpty()) {
            return 1;
        }
        
        int slicesPerMeasure = (quantization * numerator) / denominator;
        double sliceTimeSeconds = (60.0 / tempo) * (4.0 / quantization);
        
        // Find maximum position
        int maxPosition = 0;
        for (EventOutput event : noteEvents) {
            int gridPosition = (int) Math.round(event.getTimeSeconds() / sliceTimeSeconds);
            if (gridPosition > maxPosition) {
                maxPosition = gridPosition;
            }
        }
        
        return (maxPosition / slicesPerMeasure) + 1;
    }

    /**
     * Helper record to hold track data during two-pass processing.
     */
    private record TrackNoteData(int index, TrackOutput track, List<EventOutput> noteEvents) {}

    /**
     * Time signature information.
     */
    record TimeSignatureInfo(int numerator, int denominator) {}
}
