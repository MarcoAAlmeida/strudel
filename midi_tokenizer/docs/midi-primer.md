# MIDI Primer for MIDI Tokenizer

This document provides a thorough overview of MIDI concepts as they relate to the MIDI Tokenizer module.

## What is MIDI?

MIDI (Musical Instrument Digital Interface) is a technical standard that describes a communications protocol, digital interface, and electrical connectors that connect a wide variety of electronic musical instruments, computers, and related audio devices. Unlike audio formats (MP3, WAV), MIDI does not contain actual sound but rather instructions on how to produce sound.

## Standard MIDI Files (SMF)

A Standard MIDI File (SMF) is a file format that stores MIDI data in a standardized way. SMF files have the `.mid` or `.midi` extension.

### SMF Format Types

SMF defines three format types:

- **Format 0**: Single multi-channel track. All MIDI events are stored in one track.
- **Format 1**: Multiple tracks, intended for simultaneous playback. Each track typically represents a different instrument or part.
- **Format 2**: Multiple independent sequences (rarely used).

The MIDI Tokenizer currently supports **Format 0 and Format 1**.

### Format Detection

The `javax.sound.midi` API doesn't directly expose the SMF format type. The tokenizer infers format:
- If there's only one track → Format 0
- If there are multiple tracks → Format 1

This heuristic works for most files but isn't perfect. Future versions may parse the file header directly.

## Time Representation

### Ticks vs. Microseconds

MIDI time is represented in **ticks** (also called "pulses"). The meaning of a tick depends on the file's **division** (also called **resolution** or **PPQ - Pulses Per Quarter note**).

**Division**: A number stored in the MIDI file header that indicates how many ticks constitute one quarter note. Common values are 96, 120, 192, 384, 480, 960.

Example:
- Division = 480 ticks/quarter note
- A half note = 960 ticks
- An eighth note = 240 ticks

To convert ticks to **real time** (seconds), you need the **tempo**.

### Tempo

Tempo in MIDI is specified in **microseconds per quarter note** (µs/quarter), not BPM directly.

**Formula to convert BPM to µs/quarter:**
```
microseconds_per_quarter = 60,000,000 / BPM
```

Example:
- 120 BPM → 500,000 µs/quarter
- 100 BPM → 600,000 µs/quarter

**Default Tempo**: If no tempo event is present, MIDI assumes 120 BPM (500,000 µs/quarter).

### Tempo Meta Event (0x51)

Tempo changes are communicated via a **Set Tempo** meta event (type `0x51`). This event contains 3 bytes representing the microseconds per quarter note.

**Data format:**
```
[BB BB BB] (3 bytes, big-endian)
```

Example:
- `07 A1 20` (hex) = 500,000 (decimal) = 120 BPM

A MIDI file can have multiple tempo events at different tick positions, creating a **tempo map**.

### Converting Ticks to Seconds

**Formula:**
```
time_seconds = (ticks × microseconds_per_quarter) / (division × 1,000,000)
```

**With tempo changes:**

The tokenizer builds a tempo map and applies tempo changes at their respective tick positions. To find the time in seconds for a given tick:

1. Start at tick 0 with the initial tempo
2. For each tempo change before the target tick:
   - Calculate elapsed time from the previous tempo change to this one
   - Update the current tempo
3. Calculate elapsed time from the last tempo change to the target tick
4. Sum all elapsed times

This is a straightforward approach suitable for the POC. More complex implementations might interpolate or handle edge cases differently.

## Time Signature Meta Event (0x58)

Time signature events specify the musical meter (e.g., 4/4, 3/4, 6/8).

**Data format (4 bytes):**
```
[nn dd cc bb]
```

- `nn`: Numerator (e.g., 4 for 4/4)
- `dd`: Denominator as a power of 2 (e.g., 2 for 4, meaning 2^2 = 4)
- `cc`: MIDI clocks per metronome click (typically 24)
- `bb`: Number of 32nd notes per quarter note (typically 8)

**Example:**
- 4/4 time: `04 02 18 08`
  - Numerator = 4
  - Denominator = 2^2 = 4

The tokenizer records time signatures in the metadata and optionally includes them as meta events in tracks.

## Key Signature Meta Event (0x59)

Key signature events specify the key and scale (major/minor).

**Data format (2 bytes):**
```
[sf mi]
```

- `sf`: Number of sharps (positive) or flats (negative). Range: -7 to +7
  - 0 = C major / A minor
  - 1 = G major / E minor (1 sharp)
  - -1 = F major / D minor (1 flat)
- `mi`: 0 = major, 1 = minor

**Example:**
- D major: `02 00` (2 sharps, major)
- B♭ major: `FE 00` (-2 flats, major)

The tokenizer records key signatures in the metadata.

## MIDI Events

### Note On and Note Off

**Note On** (status byte `0x90 - 0x9F`):
- Status byte: `0x9n` where `n` is the channel (0-15)
- Data byte 1: Note number (0-127)
- Data byte 2: Velocity (1-127)

**Note Off** (status byte `0x80 - 0x8F`):
- Status byte: `0x8n` where `n` is the channel
- Data byte 1: Note number (0-127)
- Data byte 2: Release velocity (usually ignored, often 0)

**Important nuance**: A Note On with velocity 0 is treated as a Note Off. This is a common optimization in MIDI files.

### Note Pairing

To compute note durations, the tokenizer must **pair** Note On events with their corresponding Note Off events.

**Pairing strategy:**
- Track active notes using a key: `(channel << 8) | noteNumber`
- When a Note On is received (velocity > 0), store it in a map
- When a Note Off is received (or Note On with velocity 0):
  - Look up the corresponding Note On
  - Calculate duration: `note_off_tick - note_on_tick`
  - Add the completed note to the output

**Multiple simultaneous notes**: The same note number can be played multiple times simultaneously on the same channel (e.g., in a chord or with overlap). The tokenizer uses a simple last-in-first-out (LIFO) approach for this POC. More sophisticated implementations might track note IDs.

### Control Change (0xB0 - 0xBF)

Control Change messages modify various parameters (volume, pan, modulation, etc.).

- Status byte: `0xBn` where `n` is the channel
- Data byte 1: Controller number (0-127)
- Data byte 2: Controller value (0-127)

**Common controllers:**
- 7: Volume
- 10: Pan
- 1: Modulation wheel
- 64: Sustain pedal

### Program Change (0xC0 - 0xCF)

Program Change messages select an instrument (sound patch) for a channel.

- Status byte: `0xCn` where `n` is the channel
- Data byte 1: Program number (0-127)

**General MIDI**: Program numbers 0-127 correspond to standard instrument sounds (0 = Acoustic Grand Piano, 40 = Violin, etc.). Future versions of the tokenizer may map program numbers to instrument names.

### Pitch Bend (0xE0 - 0xEF)

Pitch Bend messages modify the pitch of all notes on a channel.

- Status byte: `0xEn` where `n` is the channel
- Data bytes: 14-bit value (LSB, MSB)
- Range: 0-16383, with 8192 as the center (no bend)

## Note Numbering and Scientific Pitch

MIDI note numbers range from 0 to 127. The tokenizer converts these to **scientific pitch notation** for readability.

### Mapping Convention

The tokenizer uses the convention where **middle C (MIDI note 60) = C4**.

**Formula:**
```
octave = (note_number / 12) - 1
pitch_class = note_number % 12
```

**Pitch classes:**
```
0 = C, 1 = C#, 2 = D, 3 = D#, 4 = E, 5 = F,
6 = F#, 7 = G, 8 = G#, 9 = A, 10 = A#, 11 = B
```

**Examples:**
- MIDI 60 → 60/12 = 5, octave = 5-1 = 4, pitch_class = 0 → **C4**
- MIDI 69 → 69/12 = 5, octave = 4, pitch_class = 9 → **A4** (440 Hz)
- MIDI 72 → 72/12 = 6, octave = 5, pitch_class = 0 → **C5**

### 12-Tone Equal Temperament (12-TET)

The tokenizer assumes 12-TET tuning (standard Western tuning with 12 semitones per octave). MIDI supports microtonal music via pitch bend, but note names remain 12-TET based.

## Meta Events

Meta events carry non-performance data (text, tempo, time signature, etc.). They are identified by status byte `0xFF` followed by a type byte.

### Common Meta Event Types

| Type | Hex  | Name                | Description                           |
|------|------|---------------------|---------------------------------------|
| 0x00 | 0x00 | Sequence Number     | Sequence identifier                   |
| 0x01 | 0x01 | Text Event          | Arbitrary text                        |
| 0x02 | 0x02 | Copyright Notice    | Copyright string                      |
| 0x03 | 0x03 | Track Name          | Name of the track/sequence            |
| 0x04 | 0x04 | Instrument Name     | Name of the instrument                |
| 0x05 | 0x05 | Lyric               | Lyric text                            |
| 0x06 | 0x06 | Marker              | Marker text (e.g., rehearsal mark)    |
| 0x07 | 0x07 | Cue Point           | Cue text                              |
| 0x2F | 0x2F | End of Track        | Marks the end of a track              |
| 0x51 | 0x51 | Set Tempo           | Tempo in µs per quarter note          |
| 0x58 | 0x58 | Time Signature      | Time signature                        |
| 0x59 | 0x59 | Key Signature       | Key signature                         |

The tokenizer extracts and records these events when `--include-meta` is enabled.

## Running Status

**Running Status** is an optimization in MIDI data where if consecutive messages have the same status byte, the status byte can be omitted from subsequent messages.

Example:
```
90 3C 64  (Note On, C4, velocity 100)
3C 64     (Note On, C4, velocity 100 - status byte omitted)
```

The `javax.sound.midi` library automatically handles running status during parsing, so the tokenizer doesn't need to implement this explicitly.

## SMPTE Time Division

Some MIDI files use **SMPTE-based time division** instead of PPQ (ticks per quarter note). SMPTE division encodes frame rate and ticks per frame.

**The current tokenizer does NOT support SMPTE time division.** This is a known limitation of the POC. Most musical MIDI files use PPQ.

## Future Improvements

### 1. More Robust Tempo Accumulation

The current implementation applies tempo changes at their tick positions using a sequential scan. For files with many tempo changes, this could be optimized with an interval tree or binary search.

### 2. Instrument Name Mapping

Map General MIDI program numbers (0-127) to human-readable instrument names:
```
0 → "Acoustic Grand Piano"
40 → "Violin"
```

### 3. Advanced Note Pairing

Handle overlapping notes of the same pitch on the same channel more robustly. Options:
- Track note IDs explicitly
- Use a stack (LIFO) or queue (FIFO) for each (channel, note) pair

### 4. SMPTE Support

Add support for SMPTE time division for video/film scoring applications.

### 5. Chunking for LLM Token Limits

Large MIDI files can produce very long JSON outputs. Implement chunking strategies:
- Split by time (e.g., every 10 seconds)
- Split by event count
- Split by track

### 6. Strudel Pattern Conversion

Research and prototype conversion from MIDI note sequences to Strudel pattern notation. This would enable MIDI files to be used as input for live coding sessions.

Example (conceptual):
```javascript
// MIDI notes: C4 E4 G4 C5 (quarter notes)
s("piano").note("c4 e4 g4 c5")
```

This is an experimental feature and requires careful mapping of MIDI timing, velocity, and articulation to Strudel's pattern language.

## References

- [MIDI 1.0 Specification](https://www.midi.org/specifications)
- [Standard MIDI Files Specification](https://www.midi.org/specifications/file-format-specifications/standard-midi-files)
- [General MIDI Specification](https://www.midi.org/specifications/midi1-specifications/general-midi-specifications)
- [Scientific Pitch Notation](https://en.wikipedia.org/wiki/Scientific_pitch_notation)

## Implementation in MIDI Tokenizer

The tokenizer implements these concepts using Java's `javax.sound.midi` package:

- **Sequence**: Represents a MIDI file with tracks and division
- **Track**: Sequence of MIDI events
- **MidiEvent**: A MIDI message at a specific tick
- **MidiMessage**: Base class for all MIDI messages
  - **ShortMessage**: Channel messages (Note On/Off, CC, PC, Pitch Bend)
  - **MetaMessage**: Meta events (tempo, time signature, text, etc.)

The `MidiParser` class orchestrates parsing, builds the tempo map, pairs note on/off events, and converts everything to the `FileOutput` data model for JSON serialization.

## Summary

Understanding these MIDI concepts is essential for working with the MIDI Tokenizer:

1. **Ticks** are the basic time unit; **division** defines ticks per quarter note
2. **Tempo** (in µs/quarter) converts ticks to real time
3. **Note On/Off pairing** is required to compute note durations
4. **Meta events** provide structure (tempo, time signature, key signature)
5. **Scientific pitch notation** (C4 = MIDI 60) makes note names readable
6. The tokenizer outputs **JSON** with a well-defined schema for LLM consumption

This POC demonstrates the fundamentals and can be extended with more sophisticated features in future iterations.
