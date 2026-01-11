# MIDI Tokenizer

A POC CLI application for parsing Standard MIDI Files (SMF) type 0 and 1, extracting events and metadata, and emitting LLM-friendly JSON representations.

## Features

- Parse SMF type 0 and 1 files using Java's built-in `javax.sound.midi`
- Pair Note On/Off events into note events with durations
- Convert ticks to seconds using the tempo map
- Map MIDI note numbers to scientific pitch notation (e.g., 60 → C4)
- Output JSON or human-readable text format
- Built with Spring Boot and Spring Shell for interactive CLI

## Requirements

- Java 21 or later
- No external dependencies required (uses Gradle wrapper)

## Quick Start

### Build the project

```bash
./gradlew build
```

### Run the application

```bash
./gradlew bootRun
```

This starts an interactive Spring Shell session.

### CLI Commands

#### Parse a MIDI file (JSON output to stdout)

```
parse --input-file path/to/file.mid
```

#### Parse with text output

```
parse --input-file path/to/file.mid --format text
```

#### Parse and save to file

```
parse --input-file path/to/file.mid --output output.json
```

## Running Tests

```bash
./gradlew test
```

Tests construct MIDI sequences programmatically to avoid binary fixtures.

## Output Format

### JSON Format (default)

```json
{
  "format": 1,
  "division": 480,
  "tracks": [
    {
      "trackNumber": 0,
      "name": "Piano",
      "notes": [
        {
          "midiNote": 60,
          "noteName": "C4",
          "velocity": 100,
          "startTime": 0.0,
          "duration": 0.5,
          "channel": 0
        }
      ],
      "metaEvents": [
        {
          "type": "tempo",
          "time": 0.0,
          "value": "120.00 BPM"
        }
      ]
    }
  ],
  "tempoMap": {
    "changes": [
      {
        "tick": 0,
        "microsecondsPerQuarterNote": 500000
      }
    ]
  }
}
```

### Text Format

Human-readable format showing:
- File metadata (format, division)
- Tempo map
- Track information with notes and meta events

## Scientific Pitch Notation

The tool uses standard scientific pitch notation where:
- MIDI note 60 = C4 (middle C)
- MIDI note 69 = A4 (concert A, 440 Hz)
- Each octave contains 12 semitones

## Architecture

- **Application.java**: Spring Boot main class
- **MidiShellCommands.java**: Spring Shell CLI command interface
- **MidiParser.java**: Core parsing logic using `javax.sound.midi`
- **Model classes**: POJOs for JSON serialization (MidiFile, Track, Note, etc.)
- **NoteUtils.java**: MIDI note number to scientific pitch notation conversion

## Learn More

See [docs/midi-primer.md](docs/midi-primer.md) for a comprehensive guide to:
- Standard MIDI File (SMF) format
- SMF types 0, 1, and 2
- Tempo maps and time conversion
- Meta events
- Note numbering and scientific pitch notation

## License

AGPL-3.0-or-later (compatible with the Strudel project)
