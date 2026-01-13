package com.marcoalmeida.midi_tokenizer.cli;

import com.marcoalmeida.midi_tokenizer.midi.MidiParser;
import com.marcoalmeida.midi_tokenizer.strudel.ConversionOptions;
import com.marcoalmeida.midi_tokenizer.strudel.StrudelConverter;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Spring Shell commands for parsing MIDI files.
 */
@ShellComponent
public class MidiShellCommands {
    
    private final MidiParser parser;
    private final StrudelConverter strudelConverter;

    public MidiShellCommands(MidiParser parser, StrudelConverter strudelConverter) {
        this.parser = parser;
        this.strudelConverter = strudelConverter;
    }
    
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

    /**
     * Convert a MIDI file to Strudel pattern.
     *
     * @param input  Input MIDI (.mid) or JSON (.json) file path
     * @param output Optional output file path (defaults to input basename with .txt)
     * @param tempo  Optional tempo override in BPM
     * @param track  Optional track index to convert (defaults to 0)
     * @param quantize Quantization level: 8=8th notes, 16=16th notes (default), 32=32nd notes
     */
    @ShellMethod(key = "convert", value = "Convert MIDI file to Strudel pattern")
    public String convert(
            @ShellOption(help = "Path to MIDI or JSON file") String input,
            @ShellOption(help = "Output file path (optional)", defaultValue = ShellOption.NULL) String output,
            @ShellOption(help = "Tempo override in BPM", defaultValue = ShellOption.NULL) Integer tempo,
            @ShellOption(help = "Track index to convert", defaultValue = ShellOption.NULL) Integer track,
            @ShellOption(help = "Quantization level (8, 16, 32)", defaultValue = "16") Integer quantize
    ) {
        try {
            File inputFile = new File(input);
            if (!inputFile.exists()) {
                return "Error: Input file not found: " + input;
            }

            String inputLower = inputFile.getName().toLowerCase();
            if (!inputLower.endsWith(".mid") && 
                !inputLower.endsWith(".midi") && 
                !inputLower.endsWith(".json")) {
                return "Error: Input file must be a MIDI file (.mid, .midi) or JSON file (.json)";
            }

            // Validate quantization
            if (quantize != 8 && quantize != 16 && quantize != 32) {
                return "Error: Quantization must be 8, 16, or 32";
            }

            // Create conversion options
            ConversionOptions options = new ConversionOptions(tempo, track, quantize);

            // Convert
            String strudelPattern = strudelConverter.convert(input, options);

            // Determine output path
            String outputPath = output;
            if (outputPath == null) {
                // Derive from input: azul.mid -> azul.txt
                Path inputPath = Path.of(input);
                String basename = inputPath.getFileName().toString();
                int dotIndex = basename.lastIndexOf('.');
                if (dotIndex > 0) {
                    basename = basename.substring(0, dotIndex);
                }
                outputPath = inputPath.getParent() != null 
                    ? inputPath.getParent().resolve(basename + ".txt").toString()
                    : basename + ".txt";
            }

            // Write to file
            Files.writeString(Path.of(outputPath), strudelPattern);
            return "Successfully wrote Strudel pattern to: " + outputPath;

        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        } catch (Exception e) {
            return "Error converting MIDI file: " + e.getMessage();
        }
    }
}
