# MIDI Tokenizer

A proof-of-concept Java 21 CLI tool (Spring Boot + Spring Shell) that parses Standard MIDI Files (SMF) types 0 and 1 and emits an LLM-friendly JSON representation. Also supports optional human-readable text output.

## Requirements

- Java 21 (toolchain)
- No additional installation required - uses bundled Gradle wrapper

## Quick Start

### Build the Module

```bash
cd midi_tokenizer
./gradlew build
```

### Run the CLI

The application uses Spring Shell, providing an interactive command-line interface:

```bash
./gradlew bootRun
```

This will start an interactive shell where you can enter commands.

### Parse Command

The `parse` command analyzes a MIDI file and outputs its structure.

**Syntax:**
```
parse --input <file> [--output <file>] [--format json|text] [--time seconds|ticks] [--include-meta true|false]
```

**Options:**
- `--input <file>` (required): Path to the input MIDI file
- `--output <file>` (optional): Path to output file. If omitted, writes to stdout
- `--format json|text` (default: `json`): Output format
  - `json`: Pretty-printed JSON with full structure
  - `text`: Human-readable compact text summary
- `--time seconds|ticks` (default: `seconds`): Time representation
  - `seconds`: Convert tick times to seconds using tempo map
  - `ticks`: Use raw tick values only
- `--include-meta true|false` (default: `true`): Include meta events (lyrics, markers, etc.)

**Examples:**

Parse a MIDI file and display JSON to stdout:
```
parse --input my-song.mid
```

Parse and save to a file:
```
parse --input my-song.mid --output analysis.json
```

Generate human-readable text output:
```
parse --input my-song.mid --format text
```

Use tick-based timing instead of seconds:
```
parse --input my-song.mid --time ticks
```

Exclude metadata events:
```
parse --input my-song.mid --include-meta false
```

### Run Tests

```bash
./gradlew test
```

Tests create MIDI sequences programmatically (no binary fixtures) and verify parsing behavior.

## JSON Output Schema

The JSON output follows this structure:

### Top-Level Fields

- `schema_version` (string): Version of the output schema (currently "1.0")
- `file` (object): File metadata
  - `filename` (string): Name of the input file
  - `format` (int): SMF format type (0 or 1)
  - `division` (int): Ticks per quarter note
  - `duration_ticks` (long, optional): Total duration in ticks
  - `duration_seconds` (double, optional): Total duration in seconds
- `metadata` (object): Global metadata
  - `tempo_map` (array): Tempo changes throughout the file
    - Each entry: `{ tick, microseconds_per_quarter, bpm }`
  - `time_signatures` (array): Time signature changes
    - Each entry: `{ tick, numerator, denominator }`
  - `key_signatures` (array): Key signature changes
    - Each entry: `{ tick, key, scale }`
- `tracks` (array): Array of track objects

### Track Object

- `track_index` (int): Zero-based track index
- `name` (string, optional): Track name from meta event
- `program_changes` (array of int): List of unique program numbers used
- `events` (array): All events in the track

### Event Types

All events have these common fields:
- `type` (string): Event type identifier
- `tick` (long): Absolute tick position
- `time_seconds` (double, optional): Time in seconds (if `--time seconds`)

**Note Event** (`type: "note"`):
- `channel` (int): MIDI channel (0-15)
- `note_number` (int): MIDI note number (0-127)
- `note_name` (string): Scientific pitch notation (e.g., "C4", "A#5")
- `velocity` (int): Note velocity (0-127)
- `duration_ticks` (long): Note duration in ticks
- `duration_seconds` (double, optional): Note duration in seconds

**Control Change Event** (`type: "control_change"`):
- `channel` (int)
- `controller` (int): Controller number (0-127)
- `value` (int): Controller value (0-127)

**Program Change Event** (`type: "program_change"`):
- `channel` (int)
- `program` (int): Program number (0-127)

**Pitch Bend Event** (`type: "pitch_bend"`):
- `channel` (int)
- `value` (int): 14-bit pitch bend value

**Meta Event** (`type: "meta"`, if `--include-meta true`):
- `meta_type` (int): Meta message type code (e.g., 0x03 for track name)
- `meta_type_name` (string): Human-readable name
- `data` (string): Meta event data

## Example JSON Output

```json
{
  "schema_version": "1.0",
  "file": {
    "filename": "example.mid",
    "format": 1,
    "division": 480,
    "duration_ticks": 1920,
    "duration_seconds": 4.0
  },
  "metadata": {
    "tempo_map": [
      {
        "tick": 0,
        "microseconds_per_quarter": 500000,
        "bpm": 120.0
      }
    ],
    "time_signatures": [
      {
        "tick": 0,
        "numerator": 4,
        "denominator": 4
      }
    ],
    "key_signatures": []
  },
  "tracks": [
    {
      "track_index": 0,
      "name": "Piano",
      "program_changes": [0],
      "events": [
        {
          "type": "note",
          "tick": 0,
          "time_seconds": 0.0,
          "channel": 0,
          "note_number": 60,
          "note_name": "C4",
          "velocity": 100,
          "duration_ticks": 480,
          "duration_seconds": 0.5
        }
      ]
    }
  ]
}
```

## Implementation Notes

### MIDI Parsing

- Uses Java's built-in `javax.sound.midi` package (no external native libraries)
- Supports SMF types 0 and 1
- Handles standard MIDI events:
  - Note On/Off (with velocity 0 treated as Note Off)
  - Control Change, Program Change, Pitch Bend
  - Meta events: tempo, time signature, key signature, track name, instrument, lyrics, markers, end-of-track

### Tempo and Timing

- Default tempo: 500,000 microseconds per quarter note (120 BPM)
- Builds a tempo map from Set Tempo meta events (0x51)
- Converts ticks to seconds using the tempo map and file division
- **Note**: Current implementation uses a straightforward conversion approach suitable for this POC. Tempo changes are applied at their tick positions.

### Note Pairing

- Matches Note On events with corresponding Note Off events (or Note On with velocity 0)
- Computes note durations in both ticks and seconds
- Uses a per-channel, per-note-number key for pairing

### Limitations (POC)

This is a proof-of-concept implementation with the following known limitations:

- Simplified SMF format detection (inferred from track count)
- Does not handle SMPTE time division or frame-based timing
- Does not implement running status optimization
- Straightforward tempo-to-seconds mapping (no complex edge case handling)
- No support for SMF format 2 (multiple independent sequences)

## Future Enhancements

Potential follow-up features (not implemented in this version):

1. **More Robust Tempo Handling**: Accumulate tempo changes more accurately across complex tempo maps
2. **Instrument Name Mapping**: Map program numbers to General MIDI instrument names
3. **LLM Token Limit Chunking**: Option to split large MIDI files into smaller chunks for LLM context windows
4. **Strudel REPL Conversion**: Experimental conversion to Strudel pattern notation (research phase)
5. **Binary Output**: Option to compress JSON for network transmission
6. **Performance Metrics**: Report parsing time and memory usage

## Documentation

For a detailed primer on MIDI concepts used in this module, see:
- [docs/midi-primer.md](docs/midi-primer.md) - MIDI concepts and implementation details
- [Website Documentation](../website/src/pages/learn/midi-tokenizer.md) - Integrated documentation on the Strudel website

## License

This module is part of the Strudel project and follows the repository's AGPL-3.0-or-later license.
