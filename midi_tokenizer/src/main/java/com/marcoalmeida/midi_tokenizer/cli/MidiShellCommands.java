package com.marcoalmeida.midi_tokenizer.cli;

import com.marcoalmeida.midi_tokenizer.midi.MidiParser;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Spring Shell commands for parsing MIDI files.
 */
@ShellComponent
public class MidiShellCommands {
    
    private final MidiParser parser = new MidiParser();
    
    /**
     * Parse a MIDI file and output JSON representation.
     * 
     * @param input Input MIDI file path
     * @param output Optional output file path (defaults to stdout)
     * @param format Output format (json or text) - currently only json is supported
     * @param time Time format (seconds or ticks)
     * @param includeMeta Whether to include meta events
     */
    @ShellMethod(key = "parse", value = "Parse a MIDI file and output JSON representation")
    public String parse(
            @ShellOption(help = "Input MIDI file path") String input,
            @ShellOption(help = "Output file path (optional, defaults to stdout)", defaultValue = ShellOption.NULL) String output,
            @ShellOption(help = "Output format: json or text", defaultValue = "json") String format,
            @ShellOption(help = "Time format: seconds or ticks", defaultValue = "seconds") String time,
            @ShellOption(help = "Include meta events", defaultValue = "true") boolean includeMeta
    ) {
        try {
            File inputFile = new File(input);
            if (!inputFile.exists()) {
                return "Error: Input file not found: " + input;
            }
            
            if (!inputFile.getName().toLowerCase().endsWith(".mid") && 
                !inputFile.getName().toLowerCase().endsWith(".midi")) {
                return "Error: Input file must be a MIDI file (.mid or .midi)";
            }
            
            boolean includeTimeSeconds = time.equalsIgnoreCase("seconds");
            
            String json = parser.parseToJson(inputFile, includeTimeSeconds, includeMeta);
            
            if (output != null) {
                try (FileWriter writer = new FileWriter(output)) {
                    writer.write(json);
                }
                return "Successfully wrote output to: " + output;
            } else {
                return json;
            }
            
        } catch (Exception e) {
            return "Error parsing MIDI file: " + e.getMessage();
        }
    }
    
    /**
     * Display help information about the parse command.
     */
    @ShellMethod(key = "help-parse", value = "Display detailed help for the parse command")
    public String helpParse() {
        return """
                Parse MIDI File Command
                =======================
                
                Usage: parse --input <file> [options]
                
                Required:
                  --input         Path to the MIDI file to parse
                
                Optional:
                  --output        Path to write JSON output (default: stdout)
                  --format        Output format: json (default: json)
                  --time          Time format: seconds or ticks (default: seconds)
                  --include-meta  Include meta events (default: true)
                
                Examples:
                  parse --input song.mid
                  parse --input song.mid --output song.json
                  parse --input song.mid --time ticks
                  parse --input song.mid --include-meta false
                """;
    }
}
