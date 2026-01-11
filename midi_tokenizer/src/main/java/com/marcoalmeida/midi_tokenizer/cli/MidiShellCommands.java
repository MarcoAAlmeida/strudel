package com.marcoalmeida.midi_tokenizer.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.marcoalmeida.midi_tokenizer.midi.MidiParser;
import com.marcoalmeida.midi_tokenizer.model.MidiFile;
import com.marcoalmeida.midi_tokenizer.model.Note;
import com.marcoalmeida.midi_tokenizer.model.Track;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Spring Shell commands for MIDI tokenization.
 */
@ShellComponent
public class MidiShellCommands {

    private final MidiParser parser = new MidiParser();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Parse a MIDI file and output JSON representation.
     * 
     * @param inputFile Path to the MIDI file
     * @param output Optional output file path (default: stdout)
     * @param format Output format: json or text (default: json)
     */
    @ShellMethod(value = "Parse a MIDI file and output tokenized representation", key = "parse")
    public String parse(
            @ShellOption(help = "Path to the MIDI file") String inputFile,
            @ShellOption(help = "Output file path (optional)", defaultValue = ShellOption.NULL) String output,
            @ShellOption(help = "Output format: json or text", defaultValue = "json") String format
    ) {
        try {
            File file = new File(inputFile);
            if (!file.exists()) {
                return "Error: File not found: " + inputFile;
            }

            MidiFile midiFile = parser.parse(file);

            String result;
            if ("text".equalsIgnoreCase(format)) {
                result = formatAsText(midiFile);
            } else {
                result = objectMapper.writeValueAsString(midiFile);
            }

            if (output != null) {
                try (FileWriter writer = new FileWriter(output)) {
                    writer.write(result);
                }
                return "Output written to: " + output;
            } else {
                return result;
            }

        } catch (Exception e) {
            return "Error parsing MIDI file: " + e.getMessage();
        }
    }

    /**
     * Format MIDI file as human-readable text.
     */
    private String formatAsText(MidiFile midiFile) {
        StringBuilder sb = new StringBuilder();
        sb.append("MIDI File\n");
        sb.append("=========\n");
        sb.append(String.format("Format: %d\n", midiFile.getFormat()));
        sb.append(String.format("Division: %d ticks per quarter note\n", midiFile.getDivision()));
        sb.append(String.format("\nTracks: %d\n", midiFile.getTracks().size()));

        if (!midiFile.getTempoMap().getChanges().isEmpty()) {
            sb.append("\nTempo Map:\n");
            midiFile.getTempoMap().getChanges().forEach(change -> {
                double bpm = 60000000.0 / change.getMicrosecondsPerQuarterNote();
                sb.append(String.format("  Tick %d: %.2f BPM\n", change.getTick(), bpm));
            });
        }

        for (Track track : midiFile.getTracks()) {
            sb.append(String.format("\n--- Track %d: %s ---\n", track.getTrackNumber(), track.getName()));
            sb.append(String.format("Notes: %d\n", track.getNotes().size()));
            
            if (!track.getMetaEvents().isEmpty()) {
                sb.append("\nMeta Events:\n");
                track.getMetaEvents().forEach(event -> {
                    sb.append(String.format("  %.3fs - %s: %s\n", 
                        event.getTime(), event.getType(), event.getValue()));
                });
            }

            if (!track.getNotes().isEmpty()) {
                sb.append("\nNotes:\n");
                track.getNotes().stream().limit(10).forEach(note -> {
                    sb.append(String.format("  %.3fs - %s (MIDI %d) vel=%d dur=%.3fs ch=%d\n",
                        note.getStartTime(), note.getNoteName(), note.getMidiNote(),
                        note.getVelocity(), note.getDuration(), note.getChannel()));
                });
                if (track.getNotes().size() > 10) {
                    sb.append(String.format("  ... and %d more notes\n", track.getNotes().size() - 10));
                }
            }
        }

        return sb.toString();
    }
}
