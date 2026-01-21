# MIDI Tokenizer

  A standalone Java 21 CLI application for parsing Standard MIDI Files (SMF types 0 and 1) and converting them to:
  - **LLM-friendly JSON representation** for analysis and processing
  - **Strudel live coding patterns** for creative musical exploration

## Philosophy: Beyond Traditional Notation

Music notation systems shape how we think about rhythm, harmony, and time. Traditional staff notation emerged from centuries of Western classical practice, optimized for instruments and performance traditions of that era. MIDI, while more flexible, still carries assumptions about quantization and discrete events.

**Strudel's mini-notation offers something different**: a way to think about musical patterns as transformable, composable code. It's not constrained by what fits on a staff or what a pianist can physically play. You can express polyrhythms, algorithmic variations, and temporal manipulations that would be cumbersome or impossible in traditional notation.

**This tool bridges the gap** - converting the discrete, time-stamped world of MIDI into Strudel's pattern language - but the real power comes from thinking beyond what the original MIDI file contains. Use the conversion as a starting point, then explore what's possible when you're not limited by tradition. Triplets, swing, polyrhythms - these aren't special cases requiring workarounds. They're just different ways of slicing time.

The quantization grid isn't a limitation - it's a choice. Pick the grid that makes sense for your rhythm, whether that's 8, 12, 24, or any other division. The code doesn't care about "conventional" time signatures or note values.

---

## Quick Start

```bash
# Build the application
./gradlew bootJar

# Run the interactive shell
java -jar build/libs/midi-tokenizer.jar

# Convert a MIDI file to Strudel pattern
shell:>convert --input samples/in_blue.mid

# Parse a MIDI file to JSON
shell:>parse --input samples/interstellar.mid --output interstellar.json
```

---

---

## Commands Reference

The application provides an interactive Spring Shell CLI with two main commands:

### convert - Convert MIDI to Strudel Pattern

Convert MIDI files to Strudel live coding patterns. Automatically detects tempo, time signature, and optimal quantization.

**Syntax:**
```shell
convert --input <file> [--output <file>] [--tempo <bpm>] [--track <index>] [--quantize <level>] [--no-polyphony]
```

**Parameters:**
- `--input` (required): Path to MIDI file (.mid, .midi) or JSON file (.json)
- `--output` (optional): Output file path. If omitted, creates a .txt file next to the input file
- `--tempo` (optional): Override tempo in BPM (e.g., `--tempo 120`)
- `--track` (optional): Convert only specific track by index (e.g., `--track 0`)
- `--quantize` (optional): Set quantization level (e.g., `--quantize 8`). Auto-detected if omitted
- `--no-polyphony` (optional): Disable polyphonic conversion, use simple single-note mode

**Examples:**

```shell
# Simple conversion - auto-detects everything
shell:>convert --input samples/in_blue.mid

# Convert with specific quantization
shell:>convert --input samples/shape.mid --quantize 8

# Disable polyphony for simpler monophonic output
shell:>convert --input samples/azul.mid --no-polyphony --quantize 8

# Convert polyphonic piece (excellent for complex arrangements)
shell:>convert --input samples/interstellar.mid --output my_pattern.txt

# Convert only track 0 with custom tempo
shell:>convert --input song.mid --track 0 --tempo 120
```

### parse - Parse MIDI to JSON

Parse a MIDI file and output structured JSON representation suitable for analysis or LLM processing.

**Syntax:**
```shell
parse --input <file> [--output <file>] [--format json] [--time <seconds|ticks>] [--include-meta <true|false>]
```

**Parameters:**
- `--input` (required): Path to the MIDI file to parse
- `--output` (optional): Path to write JSON output. If omitted, prints to console
- `--format` (optional): Output format, currently only `json` (default: json)
- `--time` (optional): Time format: `seconds` or `ticks` (default: seconds)
- `--include-meta` (optional): Include meta events like track names, lyrics (default: true)

**Examples:**

```shell
# Basic parsing to console
shell:>parse --input samples/interstellar.mid

# Save to file
shell:>parse --input samples/in_blue.mid --output in_blue.json

# Use tick-based timing for precise MIDI manipulation
shell:>parse --input song.mid --time ticks

# Exclude meta events for cleaner output
shell:>parse --input song.mid --include-meta false
```

### Other Commands

```shell
# Get detailed help on parse command
shell:>help-parse

# Exit the shell
shell:>exit
```

---

## Sample MIDI Files

The `samples/` directory contains example MIDI files to help you explore the converter:

| File | Description | Recommended Parameters |
|------|-------------|----------------------|
| `in_blue.mid` | A beautiful, well-structured piece | None needed - auto-detection works perfectly |
| `shape.mid` | Good rhythm example | `--quantize 8` |
| `azul.mid` | Monophonic melody | `--no-polyphony --quantize 8` |
| `interstellar.mid` | Excellent polyphonic example | None needed - showcases polyphony well |

**Try them out:**
```shell
shell:>convert --input samples/in_blue.mid
shell:>convert --input samples/shape.mid --quantize 8
shell:>convert --input samples/azul.mid --no-polyphony --quantize 8
shell:>convert --input samples/interstellar.mid
```

---

---

## Building and Testing

### Requirements

- Java 21 or later
- Gradle (included via Gradle wrapper)

### Building the Application

Build the executable JAR:

```bash
./gradlew bootJar
```

This creates `build/libs/midi-tokenizer.jar`.

**Note:** Do not use `./gradlew bootRun` - it has terminal compatibility issues with Spring Shell on all platforms because Gradle intercepts stdin/stdout, preventing the interactive shell from working properly. Always build the JAR and run it directly.

### Running the Application

Build and run the JAR directly for proper Spring Shell compatibility:

```bash
./gradlew bootJar && java -jar build/libs/midi-tokenizer.jar
```

Or on Windows:
```bash
./gradlew.bat bootJar && java -jar build/libs/midi-tokenizer.jar
```

### Running Tests

Run all unit tests:

```bash
./gradlew test
```

View test reports in your browser:
```bash
# After running tests, open:
build/reports/tests/test/index.html
```

Tests use programmatically created MIDI sequences (no external MIDI files required) and cover:
- MIDI parsing and event extraction
- Note conversion and duration calculation
- Strudel pattern generation
- Rhythm quantization and polyphony handling
- GM instrument mapping

### Clean Build

Remove all build artifacts and start fresh:

```bash
./gradlew clean build
```

---

## Project Structure

```
midi_tokenizer/
├── build.gradle.kts              # Gradle build configuration
├── settings.gradle.kts            # Gradle settings
├── gradle.properties              # Gradle properties
├── README.md                      # This file
├── samples/                       # Example MIDI files
│   ├── in_blue.mid
│   ├── shape.mid
│   ├── azul.mid
│   └── interstellar.mid
├── docs/
│   └── midi-primer.md             # MIDI concepts primer
└── src/
    ├── main/
    │   └── java/com/marcoalmeida/midi_tokenizer/
    │       ├── Application.java               # Spring Boot main class
    │       ├── cli/
    │       │   └── MidiShellCommands.java     # CLI commands
    │       ├── midi/
    │       │   ├── MidiParser.java            # Core MIDI parsing
    │       │   └── NoteUtils.java             # Note conversion
    │       ├── strudel/
    │       │   ├── StrudelConverter.java      # Main converter
    │       │   ├── RhythmConverter.java       # Rhythm quantization
    │       │   ├── NoteConverter.java         # Note pattern generation
    │       │   ├── GMInstrumentMapper.java    # MIDI instrument mapping
    │       │   └── StrudelTemplate.java       # Pattern templates
    │       └── model/                          # JSON output models
    └── test/
        └── java/com/marcoalmeida/midi_tokenizer/
            ├── MidiParserTest.java
            └── strudel/                        # Strudel converter tests
```

---

## JSON Output Schema (for `parse` command)

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

---

## Features

### MIDI Parsing
- Parse MIDI files (SMF types 0 and 1)
- Extract note events with durations (pairing Note On/Off events)
- Convert MIDI note numbers to scientific pitch notation (e.g., 60 → C4)
- Build tempo map and convert ticks to seconds
- Extract program changes, control changes, pitch bends
- Extract meta events (track names, lyrics, markers, time signatures, key signatures)
- Output structured JSON suitable for LLM analysis

### Strudel Conversion
- Automatic tempo and time signature detection
- Intelligent rhythm quantization with auto-detection
- Polyphonic and monophonic pattern generation
- GM instrument mapping to Strudel sound names
- Support for complex rhythmic patterns and rests
- Multi-track conversion with proper separation

---

## Resources

### Strudel Pattern Writing

When working with the generated Strudel patterns, these resources will help you understand and enhance them:

- [Basic Notes](https://strudel.cc/workshop/first-notes/) - Introduction to Strudel note patterns
- [Mini Notation](https://strudel.cc/learn/mini-notation/) - The pattern language used in converted output

### MIDI Concepts

See [docs/midi-primer.md](docs/midi-primer.md) for a comprehensive primer on MIDI concepts including:
- MIDI file formats (SMF types 0 and 1)
- Ticks vs. microseconds
- Note numbering and scientific pitch notation
- Tempo and time signatures
- Note pairing and duration calculation

---

## Technical Details

- **Framework**: Spring Boot 3.2.1 + Spring Shell 3.2.0
- **Build Tool**: Gradle with Kotlin DSL
- **MIDI Library**: Java's built-in `javax.sound.midi` (no external native dependencies)
- **JSON**: Jackson for serialization

---

## License

This module is part of the Strudel project and follows the repository's existing license (AGPL-3.0-or-later).
