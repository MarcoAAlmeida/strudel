# MIDI Primer

A brief introduction to MIDI concepts relevant to parsing Standard MIDI Files.

## What is MIDI?

MIDI (Musical Instrument Digital Interface) is a technical standard for communicating musical information between electronic instruments, computers, and other devices. Unlike audio files (MP3, WAV), MIDI files don't contain sound recordings—they contain instructions for how to play music.

## Standard MIDI File (SMF) Formats

### Type 0
- Single track containing all MIDI events
- All channels mixed into one track
- Simpler structure, easier to parse

### Type 1
- Multiple tracks, each potentially representing different instruments or voices
- Track 0 often contains tempo and time signature metadata
- Most common format for multi-instrument compositions

### Type 2
- Multiple independent sequences
- Rarely used
- Not supported by this parser

## Time Representation

MIDI files use two time representations:

### Ticks
- MIDI's native time unit
- Integer values representing positions in musical time
- The **division** (or resolution) specifies ticks per quarter note (e.g., 480 ticks = 1 quarter note)
- Division is stored in the MIDI file header

### Microseconds/Seconds
- Real-world time
- Calculated by combining ticks with tempo information
- Requires building a tempo map to handle tempo changes

### Converting Ticks to Seconds

Formula:
```
seconds = (ticks × microseconds_per_quarter) / (division × 1,000,000)
```

Example:
- Ticks: 480
- Division: 480 ticks per quarter note
- Tempo: 500,000 microseconds per quarter note (120 BPM)
- Result: 480 × 500,000 / (480 × 1,000,000) = 0.5 seconds

## Tempo

### Set Tempo Meta Event (0x51)
- Specifies microseconds per quarter note
- Default: 500,000 microseconds (120 BPM)
- Can change multiple times within a piece

### BPM Calculation
```
BPM = 60,000,000 / microseconds_per_quarter
```

Example:
- 500,000 microseconds per quarter = 120 BPM
- 375,000 microseconds per quarter = 160 BPM

### Tempo Map
A tempo map is a list of tempo changes throughout the piece:
```
[
  { tick: 0, microseconds_per_quarter: 500000, bpm: 120 },
  { tick: 1920, microseconds_per_quarter: 375000, bpm: 160 }
]
```

## Time Signature (0x58)

Specifies the musical time signature (e.g., 4/4, 3/4, 6/8).

**Data format** (4 bytes):
1. Numerator (e.g., 4 for 4/4)
2. Denominator power (e.g., 2 means 2^2 = 4 for 4/4)
3. MIDI clocks per metronome click (usually 24)
4. Number of 32nd notes per 24 MIDI clocks (usually 8)

**Example**: 4/4 time
```
[4, 2, 24, 8]
```

## Key Signature (0x59)

Specifies the musical key signature.

**Data format** (2 bytes):
1. Number of sharps (positive) or flats (negative)
   - -7 = 7 flats, 0 = C major/A minor, 7 = 7 sharps
2. Major/minor flag
   - 0 = major, 1 = minor

**Example**: C major
```
[0, 0]
```

**Example**: D major (2 sharps)
```
[2, 0]
```

## MIDI Messages

### Note On (0x90)
Starts a note playing.

**Data**:
- Channel (0-15)
- Note number (0-127)
- Velocity (1-127, loudness)

**Special case**: Note On with velocity 0 is treated as Note Off.

### Note Off (0x80)
Stops a note.

**Data**:
- Channel (0-15)
- Note number (0-127)
- Release velocity (usually ignored)

### Program Change (0xC0)
Changes the instrument sound (General MIDI program).

**Data**:
- Channel (0-15)
- Program number (0-127)
  - 0 = Acoustic Grand Piano
  - 24 = Acoustic Guitar
  - 40 = Violin
  - etc.

### Control Change (0xB0)
Modifies continuous controllers.

**Data**:
- Channel (0-15)
- Controller number (0-127)
  - 7 = Volume
  - 10 = Pan
  - 64 = Sustain pedal
  - etc.
- Value (0-127)

### Pitch Bend (0xE0)
Bends the pitch of notes.

**Data**:
- Channel (0-15)
- LSB (least significant byte)
- MSB (most significant byte)
- Combined value: (MSB << 7) | LSB
- Range: 0-16383, centered at 8192

## Note Numbers and Scientific Pitch Notation

MIDI note numbers (0-127) map to specific pitches using 12-tone equal temperament (12-TET).

### A440 Standard
- A4 (440 Hz) = MIDI note 69
- Middle C (C4) = MIDI note 60

### Formula
```
Note name = NOTE_NAMES[note_number % 12]
Octave = (note_number / 12) - 1
```

Where `NOTE_NAMES = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]`

### Examples
- 0 → C-1
- 60 → C4 (Middle C)
- 69 → A4 (A440)
- 73 → C#5
- 127 → G9

### Enharmonic Notes
MIDI doesn't distinguish between enharmonic equivalents:
- C# and Db are both represented as C#
- F# and Gb are both represented as F#

## Pairing Notes and Computing Durations

MIDI represents notes as separate "on" and "off" events. To compute note durations:

1. Track Note On events in a map keyed by (channel, note_number)
2. When a Note Off (or Note On with velocity 0) occurs, find the matching Note On
3. Calculate duration:
   ```
   duration_ticks = note_off_tick - note_on_tick
   duration_seconds = ticks_to_seconds(note_off_tick) - ticks_to_seconds(note_on_tick)
   ```

### Handling Overlapping Notes
The same note can be played multiple times on the same channel before the first instance ends. Use a stack or list to handle this:

```
Channel 0, Note 60: On at tick 0
Channel 0, Note 60: On at tick 100  (overlap!)
Channel 0, Note 60: Off at tick 150 (matches second On)
Channel 0, Note 60: Off at tick 200 (matches first On)
```

## Running Status

MIDI uses "running status" to reduce file size by omitting repeated status bytes. Java's `javax.sound.midi` library automatically handles this, so parsers don't need to implement it manually.

## Meta Events

Meta events provide non-performance information:

- **0x00**: Sequence Number
- **0x01**: Text Event
- **0x02**: Copyright Notice
- **0x03**: Sequence/Track Name
- **0x04**: Instrument Name
- **0x05**: Lyric
- **0x06**: Marker
- **0x07**: Cue Point
- **0x2F**: End of Track (required at the end of each track)
- **0x51**: Set Tempo
- **0x54**: SMPTE Offset
- **0x58**: Time Signature
- **0x59**: Key Signature

## SMPTE Offset (0x54)

SMPTE (Society of Motion Picture and Television Engineers) time code for synchronizing with video.

**Data** (5 bytes):
1. Hours
2. Minutes
3. Seconds
4. Frames
5. Fractional frames

Used in film scoring and video production.

## Common Pitfalls

1. **Assuming constant tempo**: Always build a tempo map—tempo can change.
2. **Forgetting Note On velocity 0**: Treat as Note Off.
3. **Ignoring channels**: Multiple instruments can play the same note on different channels.
4. **Division confusion**: Division is ticks per quarter note, not per beat.
5. **Time signature ≠ tempo**: Time signature is musical meter; tempo is speed.

## References

- [MIDI 1.0 Specification](https://www.midi.org/specifications)
- [Standard MIDI File Format Spec](https://www.cs.cmu.edu/~music/cmsip/readings/Standard-MIDI-file-format-updated.pdf)
- [General MIDI Sound Set](https://www.midi.org/specifications/item/gm-level-1-sound-set)
- [Scientific Pitch Notation](https://en.wikipedia.org/wiki/Scientific_pitch_notation)

## Further Reading

- **Microtonal Music**: MIDI supports pitch bend for non-12-TET tunings
- **MIDI 2.0**: Newer spec with higher resolution and bidirectional communication
- **SysEx Messages**: System Exclusive messages for device-specific data
- **MIDI Controllers**: List of standard controller numbers (CC 0-127)
