---
title: MIDI Tokenizer
layout: ../../layouts/MainLayout.astro
---

# MIDI Tokenizer

The MIDI Tokenizer is a standalone Java CLI tool for parsing Standard MIDI Files (SMF) and converting them into LLM-friendly JSON representations.

## Overview

This tool reads MIDI files (type 0 and 1), extracts musical events and metadata, pairs Note On/Off events into complete notes with durations, and outputs structured data suitable for machine learning applications.

## Key Features

- **Parse SMF Type 0 & 1**: Support for both single-track and multi-track MIDI files
- **Note Pairing**: Automatically pairs Note On and Note Off events to create notes with duration
- **Time Conversion**: Converts MIDI ticks to seconds using the tempo map
- **Scientific Pitch Notation**: Maps MIDI note numbers to standard notation (60 → C4)
- **Multiple Output Formats**: JSON (default) or human-readable text
- **Self-Contained**: Standalone Gradle module with wrapper included

## Quick Start

The MIDI Tokenizer is located in the `midi_tokenizer/` directory as a standalone Gradle project.

### Build

```bash
cd midi_tokenizer
./gradlew build
```

### Run

Start the interactive CLI:

```bash
./gradlew bootRun
```

### Parse a MIDI File

Once in the Spring Shell, use the `parse` command:

```
parse --input-file path/to/your/file.mid
```

Output to a file:

```
parse --input-file path/to/file.mid --output result.json
```

Get human-readable text instead of JSON:

```
parse --input-file path/to/file.mid --format text
```

## Output Format

### JSON Structure

The default JSON output includes:

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

The text format provides a human-readable summary:

```
MIDI File
=========
Format: 1
Division: 480 ticks per quarter note

Tracks: 2

Tempo Map:
  Tick 0: 120.00 BPM

--- Track 0: Piano ---
Notes: 24

Meta Events:
  0.000s - tempo: 120.00 BPM
  0.000s - trackName: Piano

Notes:
  0.000s - C4 (MIDI 60) vel=100 dur=0.500s ch=0
  0.500s - E4 (MIDI 64) vel=100 dur=0.500s ch=0
  ...
```

## Use Cases

### Machine Learning

The JSON output is designed for LLM and ML applications:

- Generate similar musical patterns
- Analyze musical structure
- Transform or remix compositions
- Answer questions about music content

### Music Analysis

- Extract note sequences for analysis
- Study tempo changes over time
- Examine instrument usage per track
- Extract lyrics and markers

### Data Conversion

- Convert proprietary MIDI to open JSON format
- Prepare training data for music AI models
- Archive MIDI files in text-searchable format

## MIDI Primer

For a comprehensive understanding of MIDI files, see the included [MIDI Primer](https://github.com/MarcoAAlmeida/strudel/blob/main/midi_tokenizer/docs/midi-primer.md) which covers:

- Standard MIDI File (SMF) format types
- MIDI events and meta events
- Tempo maps and time conversion
- Note numbering and scientific pitch notation
- Common MIDI file issues

## Technical Details

### Dependencies

- **Java 21**: Modern Java with latest language features
- **Spring Boot**: Application framework
- **Spring Shell**: Interactive CLI
- **Jackson**: JSON serialization
- **javax.sound.midi**: Built-in MIDI parsing (no external MIDI library needed)

### Architecture

- **Application.java**: Spring Boot entry point
- **MidiShellCommands.java**: CLI command interface
- **MidiParser.java**: Core parsing using `javax.sound.midi`
- **Model classes**: POJOs for structured data (MidiFile, Track, Note, etc.)
- **NoteUtils.java**: MIDI number ↔ scientific pitch notation

### Testing

Unit tests use programmatically constructed MIDI sequences (no binary fixtures):

```bash
./gradlew test
```

Tests cover:
- Note pairing (On/Off events)
- Tempo conversion (ticks → seconds)
- Note naming (60 → C4)
- Velocity-zero-as-Note-Off handling

## Scientific Pitch Notation

The tool uses the standard where:

- **MIDI 60 = C4** (middle C)
- **MIDI 69 = A4** (concert A, 440 Hz)
- Each octave spans 12 semitones

Examples:
- 21 → A0 (lowest A on piano)
- 60 → C4 (middle C)
- 108 → C8 (very high C)

## Future Enhancements

Potential additions:

- MIDI file generation from JSON
- Support for SMF type 2
- Control change event extraction
- Program change tracking
- Pitch bend analysis
- Real-time MIDI input parsing

## Contributing

The MIDI Tokenizer is part of the Strudel project. Contributions welcome!

See the module's [README](https://github.com/MarcoAAlmeida/strudel/blob/main/midi_tokenizer/README.md) for development details.

## License

AGPL-3.0-or-later (same as Strudel project)
