package com.marcoalmeida.midi_tokenizer.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.marcoalmeida.midi_tokenizer.midi.MidiParser;
import com.marcoalmeida.midi_tokenizer.model.FileOutput;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Spring Shell commands for MIDI file parsing.
 */
@ShellComponent
public class MidiShellCommands {

    @ShellMethod(key = "parse", value = "Parse a MIDI file and output JSON or text representation")
    public String parse(
            @ShellOption(help = "Input MIDI file path") String input,
            @ShellOption(help = "Output file path (optional, defaults to stdout)", defaultValue = ShellOption.NULL) String output,
            @ShellOption(help = "Output format: json or text", defaultValue = "json") String format,
            @ShellOption(help = "Time unit: seconds or ticks", defaultValue = "seconds") String time,
            @ShellOption(help = "Include metadata events", defaultValue = "true") boolean includeMeta
    ) {
        try {
            File inputFile = new File(input);
            if (!inputFile.exists()) {
                return "Error: Input file does not exist: " + input;
            }

            // Configure parser
            MidiParser parser = new MidiParser();
            parser.setIncludeMeta(includeMeta);
            parser.setUseSeconds("seconds".equalsIgnoreCase(time));

            // Parse the file
            FileOutput result = parser.parse(inputFile);

            // Generate output
            String outputContent;
            if ("text".equalsIgnoreCase(format)) {
                outputContent = result.toText();
            } else {
                // JSON format (default)
                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                outputContent = mapper.writeValueAsString(result);
            }

            // Write to file or stdout
            if (output != null) {
                try (FileWriter writer = new FileWriter(output)) {
                    writer.write(outputContent);
                }
                return "Successfully wrote output to: " + output;
            } else {
                return outputContent;
            }

        } catch (Exception e) {
            return "Error parsing MIDI file: " + e.getMessage();
        }
    }
}
