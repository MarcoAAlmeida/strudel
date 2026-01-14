package com.marcoalmeida.midi_tokenizer.strudel;

import com.marcoalmeida.midi_tokenizer.midi.MidiParser;
import com.marcoalmeida.midi_tokenizer.model.EventOutput;
import com.marcoalmeida.midi_tokenizer.model.MidiOutput;
import com.marcoalmeida.midi_tokenizer.model.TempoEntry;
import com.marcoalmeida.midi_tokenizer.model.TrackOutput;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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

        // Get time signature (default to 4/4 if not available)
        int timeSignatureNumerator = 4;
        int timeSignatureDenominator = 4;
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
            timeSignatureNumerator = timeSignature.getNumerator();
            timeSignatureDenominator = timeSignature.getDenominator();
        }

        // Get effective quantization (override or smart default)
        int quantization = options.getEffectiveQuantization(timeSignatureNumerator, timeSignatureDenominator);
        
        // Calculate slices per measure
        int slicesPerMeasure = (quantization * timeSignatureNumerator) / timeSignatureDenominator;

        // Convert to quantized cycle-based pattern (Phase 1.9: with polyphony toggle)
        boolean polyphonicMode = options.isPolyphonicMode();
        String pattern = RhythmConverter.toQuantizedCyclePattern(
            noteEvents,
            midiOutput.getFile().getDivision(),
            timeSignatureNumerator,
            timeSignatureDenominator,
            quantization,
            (int) Math.round(bpm),
            polyphonicMode  // Phase 1.9: polyphony flag
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
}
