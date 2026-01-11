# MIDI Primer

A comprehensive guide to understanding Standard MIDI Files (SMF) and MIDI concepts.

## What is MIDI?

MIDI (Musical Instrument Digital Interface) is a technical standard that describes a protocol, digital interface, and electrical connectors that connect electronic musical instruments, computers, and related audio devices. Unlike audio files, MIDI files don't contain actual sound; they contain instructions for how to play music.

## Standard MIDI Files (SMF)

Standard MIDI Files are a standardized format for storing MIDI data in files. They allow MIDI sequences to be shared between different programs and devices.

### SMF Types

There are three types of Standard MIDI Files:

#### Type 0 (Single Track)
- Contains a single track with all MIDI data
- All channels are combined in one track
- Simplest format, often used for single-instrument recordings
- Easy to play back but harder to edit individual parts

#### Type 1 (Multi-Track)
- Contains multiple tracks that play simultaneously
- Each track typically represents a different instrument or part
- Most common format for multi-instrument compositions
- Allows independent editing of each part
- Track 0 often contains tempo map and global meta events

#### Type 2 (Sequential Tracks)
- Contains multiple independent tracks that play sequentially
- Rarely used in practice
- Each track is a separate pattern or sequence
- Not commonly supported by modern software

## MIDI Events

### Channel Voice Messages

These are the core MIDI messages that produce sound:

- **Note On**: Start playing a note (includes note number and velocity)
- **Note Off**: Stop playing a note
- **Control Change**: Modify control parameters (volume, pan, modulation, etc.)
- **Program Change**: Select an instrument/sound
- **Pitch Bend**: Continuous pitch modification
- **Aftertouch**: Pressure applied after a note is struck

### Meta Events

Meta events contain non-performance information about the MIDI file:

- **0x00**: Sequence Number
- **0x01**: Text Event (arbitrary text)
- **0x02**: Copyright Notice
- **0x03**: Track Name
- **0x04**: Instrument Name
- **0x05**: Lyric (text for singing)
- **0x06**: Marker (named point in time)
- **0x07**: Cue Point (description of something happening)
- **0x20**: MIDI Channel Prefix
- **0x2F**: End of Track (required at the end of each track)
- **0x51**: Set Tempo (microseconds per quarter note)
- **0x54**: SMPTE Offset
- **0x58**: Time Signature
- **0x59**: Key Signature
- **0x7F**: Sequencer Specific Meta Event

## Timing and the Tempo Map

### Ticks and Division

MIDI timing is based on "ticks" - the smallest unit of time in a MIDI file.

- **Division** (also called "resolution"): Ticks per quarter note
- Common values: 96, 120, 192, 480, 960
- Higher division = more precise timing

### Tempo Map

The tempo map defines how ticks are converted to real time:

- **Tempo**: Microseconds per quarter note (default: 500,000 = 120 BPM)
- **BPM Calculation**: 60,000,000 / microseconds per quarter note
- Tempo can change throughout the piece via Set Tempo (0x51) meta events

### Time Conversion Formula

To convert ticks to seconds:

```
time_in_seconds = (ticks * microseconds_per_quarter_note) / (division * 1,000,000)
```

With tempo changes, you must:
1. Track all tempo change events with their tick positions
2. Calculate time segments between tempo changes
3. Sum the time for each segment

Example:
```
Division: 480 ticks per quarter note
Tempo: 500,000 microseconds per quarter note (120 BPM)
Tick 480: 
  = (480 * 500,000) / (480 * 1,000,000)
  = 240,000,000 / 480,000,000
  = 0.5 seconds
```

## Note Numbering and Scientific Pitch Notation

### MIDI Note Numbers

- MIDI notes range from 0 to 127
- Each number represents a specific pitch
- 12 semitones (notes) per octave

### Scientific Pitch Notation

Standard naming convention used in this tool:

- **Middle C** (MIDI 60) = **C4**
- **Concert A** (MIDI 69) = **A4** (440 Hz)

The formula:
```
Octave = (MIDI_note / 12) - 1
Note_name = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"][MIDI_note % 12]
```

Examples:
- MIDI 21 = A0 (lowest A on piano)
- MIDI 60 = C4 (middle C)
- MIDI 69 = A4 (concert A)
- MIDI 108 = C8 (very high C)

### Note Ranges

Common instrument ranges in MIDI:
- Piano: 21 (A0) to 108 (C8)
- Guitar: 40 (E2) to 84 (C6)
- Bass: 28 (E1) to 67 (G4)
- Violin: 55 (G3) to 103 (G7)

## Channels

- MIDI supports 16 channels (0-15)
- Each channel can play a different instrument
- Channel 10 (index 9) is traditionally reserved for drums/percussion
- Allows multiple instruments to play simultaneously on different channels

## Note Pairing

In MIDI, notes are represented by two separate events:

1. **Note On**: When the note starts (includes velocity)
2. **Note Off**: When the note ends

Special case:
- A Note On with velocity 0 is equivalent to Note Off
- This is a common optimization in MIDI files

To reconstruct notes with duration:
1. Track all Note On events
2. When Note Off occurs, pair it with the corresponding Note On
3. Calculate duration from the difference in ticks/time
4. Handle the same note on multiple channels separately

## Velocity

- Velocity represents how hard a note is struck (0-127)
- 0 = silent (or Note Off when in Note On message)
- 127 = maximum loudness
- Affects both volume and timbre in many instruments

## LLM-Friendly Representation

For machine learning applications, MIDI data is often converted to JSON with:

- **Absolute timing** (seconds, not ticks)
- **Note objects** with start time, duration, pitch name
- **Tempo map** for reference
- **Meta events** for context (track names, lyrics, markers)
- **Clear structure** separating tracks and channels

This makes it easier for LLMs to:
- Understand musical structure
- Generate similar patterns
- Answer questions about the music
- Transform or remix the content

## Common MIDI File Issues

1. **Missing End of Track**: Some files may be malformed
2. **Overlapping Notes**: Same note on same channel before previous ends
3. **Orphaned Note Events**: Note On without matching Note Off
4. **Invalid Tempo**: Tempo changes with invalid values
5. **Out of Range Notes**: Note numbers outside 0-127

A robust parser should handle these gracefully.

## Further Reading

- [MIDI 1.0 Specification](https://www.midi.org/specifications)
- [Standard MIDI Files Specification](https://www.midi.org/specifications/midi1-specifications/standard-midi-files)
- [General MIDI Specification](https://www.midi.org/specifications/midi1-specifications/general-midi-specifications)

## Tools and Libraries

- **Java**: `javax.sound.midi` (used in this project)
- **Python**: `mido`, `pretty_midi`
- **JavaScript**: `midi-parser-js`, `tonejs/Midi`
- **C++**: `RtMidi`, `Midifile`

## Summary

Understanding MIDI files requires knowledge of:
- File structure (type 0, 1, or 2)
- Event types (channel voice messages and meta events)
- Timing system (ticks, division, tempo map)
- Note representation (pairing Note On/Off)
- Pitch notation (MIDI numbers to note names)

This knowledge enables effective parsing, transformation, and generation of MIDI data for music applications and machine learning.
