# MIDI Tokenizer

A standalone Java 21 CLI application for parsing Standard MIDI Files (SMF types 0 and 1) and converting them to an LLM-friendly JSON representation.

## Features

- Parse MIDI files (SMF types 0 and 1)
- Extract note events with durations (pairing Note On/Off events)
- Convert MIDI note numbers to scientific pitch notation (e.g., 60 → C4)
- Build tempo map and convert ticks to seconds
- Extract program changes, control changes, pitch bends
- Extract meta events (track names, lyrics, markers, time signatures, key signatures)
- Output structured JSON suitable for LLM analysis

## Requirements

- Java 21 or later
- Gradle (included via Gradle wrapper)

## Building

Build the executable JAR:

```bash
./gradlew build
```

This creates `build/libs/midi-tokenizer.jar`.

## Running

### Using Gradle

```bash
./gradlew bootRun
```

### Using the JAR directly

```bash
java -jar build/libs/midi-tokenizer.jar
```

## Usage

The application provides a Spring Shell interactive CLI. Available commands:

### Parse Command

Parse a MIDI file and output JSON representation.

**Basic usage:**
```shell
parse --input song.mid
```

**Write to file:**
```shell
parse --input song.mid --output song.json
```

**Output time in ticks instead of seconds:**
```shell
parse --input song.mid --time ticks
```

**Exclude meta events:**
```shell
parse --input song.mid --include-meta false
```

### Command Options

- `--input` (required): Path to the MIDI file to parse
- `--output` (optional): Path to write JSON output (default: stdout)
- `--format` (optional): Output format, currently only `json` (default: json)
- `--time` (optional): Time format: `seconds` or `ticks` (default: seconds)
- `--include-meta` (optional): Include meta events (default: true)

### Help

Get help on the parse command:
```shell
help-parse
```

Exit the shell:
```shell
exit
```

## JSON Output Schema

The output JSON has the following structure:

```json
{
  "schemaVersion": "1.0",
  "file": {
    "filename": "song.mid",
    "format": 1,
    "division": 480,
    "durationTicks": 1920,
    "durationSeconds": 4.0
  },
  "metadata": {
    "tempoMap": [
      {
        "tick": 0,
        "microsecondsPerQuarter": 500000,
        "bpm": 120.0
      }
    ],
    "timeSignatures": [
      {
        "tick": 0,
        "numerator": 4,
        "denominator": 4,
        "clocksPerClick": 24,
        "thirtySecondsPer24Clocks": 8
      }
    ],
    "keySignatures": [
      {
        "tick": 0,
        "sharpsFlats": 0,
        "majorMinor": 0
      }
    ]
  },
  "tracks": [
    {
      "index": 0,
      "name": "Piano",
      "programChanges": [
        {
          "tick": 0,
          "channel": 0,
          "program": 0
        }
      ],
      "events": [
        {
          "type": "note",
          "tick": 0,
          "timeSeconds": 0.0,
          "channel": 0,
          "noteNumber": 60,
          "noteName": "C4",
          "velocity": 100,
          "durationTicks": 480,
          "durationSeconds": 1.0
        }
      ]
    }
  ]
}
```

### Event Types

- **note**: Note events with `noteNumber`, `noteName`, `velocity`, `durationTicks`, `durationSeconds`
- **control_change**: Control change events with `controller` and `value`
- **pitch_bend**: Pitch bend events with `pitchBend` (centered at 0)
- **meta**: Meta events like track names, lyrics, markers with `text`

## Running Tests

Run unit tests:

```bash
./gradlew test
```

Tests use programmatically created MIDI sequences (no external MIDI files required).

## Documentation

See [docs/midi-primer.md](docs/midi-primer.md) for a primer on MIDI concepts including:
- MIDI file formats (SMF types 0 and 1)
- Ticks vs. microseconds
- Note numbering and scientific pitch notation
- Tempo and time signatures
- Note pairing and duration calculation

## License

This module is part of the Strudel project and follows the repository's existing license (AGPL-3.0-or-later).

## Technical Details

- **Framework**: Spring Boot 3.2.1 + Spring Shell 3.2.0
- **Build Tool**: Gradle with Kotlin DSL
- **MIDI Library**: Java's built-in `javax.sound.midi` (no external native dependencies)
- **JSON**: Jackson for serialization

## Project Structure

```
midi_tokenizer/
├── build.gradle.kts          # Gradle build configuration
├── settings.gradle.kts        # Gradle settings
├── gradle.properties          # Gradle properties
├── README.md                  # This file
├── docs/
│   └── midi-primer.md         # MIDI concepts primer
├── src/
│   ├── main/
│   │   └── java/com/marcoalmeida/midi_tokenizer/
│   │       ├── Application.java           # Spring Boot main class
│   │       ├── cli/
│   │       │   └── MidiShellCommands.java # CLI commands
│   │       ├── midi/
│   │       │   ├── MidiParser.java        # Core parsing logic
│   │       │   └── NoteUtils.java         # Note conversion utilities
│   │       └── model/                      # JSON output model classes
│   └── test/
│       └── java/com/marcoalmeida/midi_tokenizer/
│           └── MidiParserTest.java        # Unit tests
└── gradle/                                # Gradle wrapper files
```

## Examples

### Example 1: Basic parsing

```shell
shell:>parse --input /path/to/song.mid
{
  "schemaVersion": "1.0",
  "file": { ... },
  "metadata": { ... },
  "tracks": [ ... ]
}
```

### Example 2: Save to file

```shell
shell:>parse --input song.mid --output song.json
Successfully wrote output to: song.json
```

### Example 3: Ticks-based timing

```shell
shell:>parse --input song.mid --time ticks
```

This will output event times in MIDI ticks instead of seconds, useful for precise MIDI manipulation.
