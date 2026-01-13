---
applyTo: "**"
---

# MIDI to Strudel Pattern Converter - Implementation Guide

## Overview

Add a new Spring Shell command to the existing MIDI Tokenizer Java application that converts MIDI files to Strudel live coding patterns, enabling musicians to import existing MIDI compositions into the Strudel environment.

This extends the current `parse` command with a new `convert` command that outputs Strudel pattern syntax instead of JSON.

## Command Specification

### Basic Usage

```bash
# In Spring Shell interactive mode:
shell:>convert --input input.mid --output output.txt

# Convert existing JSON to Strudel pattern
shell:>convert --input input.json --output output.txt

# Convert with tempo override
shell:>convert --input input.mid --output output.txt --tempo 120

# Auto-generate output filename (input.mid -> input.txt)
shell:>convert --input input.mid
```

### Command Options

- `--input` (required) - Path to MIDI file (.mid, .midi) or JSON file (.json)
- `--output` (optional) - Output file path (auto-generated from input filename if omitted)
- `--tempo` (optional) - Override tempo/BPM

## File Structure Template

All generated pattern files follow this structure (based on `00_SonClave.txt`):

```javascript
/* "Pattern Name" */
/**
Source: filename.mid
[Link to reference video](https://example.com)
[Another reference](https://example.com)

Additional notes about the pattern
**/

setcpm(tempo/4)

// Reusable pattern definitions
let pattern_name = s(`pattern`).sound(`instrument`)
let another_pattern = note(`notes`).sound(`synth`)

// Final arrangement
arrange(
  [bars, pattern_name],
  [bars, another_pattern],
).room(0.2)
```

## Implementation Phases

### Phase 1: POC (Proof of Concept) - Basic Single Track Conversion ✅ **COMPLETE**

**Goal**: Convert a single MIDI track with notes to a basic Strudel pattern.

**Status**: ✅ Implemented and tested with quantization-based approach

**Actual Implementation**:
- ✅ Parse MIDI file to JSON (reused existing parser)
- ✅ Extract notes from one track (configurable via `--track` option)
- ✅ Convert to `note()` pattern using **quantization grid** approach
- ✅ Generate complete file structure with metadata
- ✅ Support both MIDI (.mid, .midi) and JSON (.json) input
- ✅ Auto-generate output filename if not specified
- ✅ 22 unit tests across 4 test classes (NoteConverter, RhythmConverter, StrudelConverter, StrudelTemplate)

**Quantization Approach**:

Phase 1 uses a **fixed-grid time-slicing approach** rather than continuous time representation:

1. **Grid Calculation**: Divide each measure into fixed time slices based on quantization level
   - 8 = eighth notes (8 slices per 4/4 measure)
   - 12 = triplet eighths or dotted rhythms (12 slices per 4/4 measure)
   - 16 = sixteenth notes (16 slices per 4/4 measure) - **DEFAULT**
   - 24 = triplet sixteenths (24 slices per 4/4 measure) - **BEST FOR MIXED STRAIGHT/TRIPLET**
   - 32 = thirty-second notes (32 slices per 4/4 measure)
   - Any integer value supported - use LCM for complex polyrhythms

2. **Measure-by-Measure Processing**: Each measure processed independently with fixed slice count

3. **50% Occupancy Rule**: If a MIDI note occupies >50% of a time slice, it claims that slot

4. **Conflict Resolution**: When multiple notes compete for same slice, longest total duration wins

5. **Duration Merging**: Consecutive identical notes merged with `@N` notation (N = slice count)
   - Example: `c4@4` at 16th note quantization = quarter note (4 sixteenth-note slices)
   - Example: `c4@2` at 8th note quantization = quarter note (2 eighth-note slices)

6. **Rest Insertion**: Empty slices become `~` (rest)

7. **Pattern Structure**: Each measure wrapped in `[...]`, all measures wrapped in `<...>` for sequential playback

**Quantization Trade-offs**:

✅ **Advantages**:
- Simple, deterministic output
- Avoids floating-point duration calculations  
- Works well for rhythmically simple material
- Produces consistent, readable patterns
- Easy to understand and debug

⚠️ **Limitations**:
- **All events snap to grid** - micro-timing and humanization lost
- **50% threshold is arbitrary** - notes at 49% ignored, 51% included
- **`@N` values are context-dependent** - meaning changes with quantization level
- **May misrepresent syncopation** if note starts off-grid
- **Triplets and complex rhythms**: Use appropriate quantization (12, 24, etc.) or combine with Strudel's `/3`, `.fast()`, `.slow()` modifiers

**Success Criteria**: ✅ All Met
- ✅ Correctly parse note pitches (C4, D#5, etc.) - lowercase output
- ✅ Preserve timing via quantization grid (not exact, but musically useful)
- ✅ Generate valid Strudel syntax that plays correctly
- ✅ Handle time signatures (4/4, 3/4, etc.)
- ✅ Support tempo override
- ✅ Comprehensive test coverage
- ✅ Musical output quality good (simple, readable patterns)

**Input**: MIDI file with single or multiple melody tracks

**Output Example**:

**Files Created**: ✅
- `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/StrudelConverter.java` - Main conversion service (@Service)
- `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/NoteConverter.java` - Note conversion utility (static)
- `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/RhythmConverter.java` - **Quantization engine** (static)
- `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/StrudelTemplate.java` - Template rendering (static)
- `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/ConversionOptions.java` - Configuration record
- `src/test/java/com/marcoalmeida/midi_tokenizer/strudel/NoteConverterTest.java` - 5 tests
- `src/test/java/com/marcoalmeida/midi_tokenizer/strudel/RhythmConverterTest.java` - 6 tests
- `src/test/java/com/marcoalmeida/midi_tokenizer/strudel/StrudelConverterTest.java` - 6 integration tests
- `src/test/java/com/marcoalmeida/midi_tokenizer/strudel/StrudelTemplateTest.java` - 5 tests

**Updated Files**: ✅
- `src/main/java/com/marcoalmeida/midi_tokenizer/cli/MidiShellCommands.java` - Added `convert` command

**Known Gaps from Phase 1**:
- ⚠️ Missing `--verbose` flag for debugging output
- ⚠️ No pattern simplification (collapsing consecutive rests)
- ⚠️ Static utility classes instead of injectable services
- ❌ Polyphony Lost (fixed in Phase 1.5)
- ❌ Manual Quantization (fixed in Phase 1.5)
- ❌ Context-Dependent @N (partially addressed in Phase 1.5)

---

### Phase 1.5: Polyphonic 8-Grid Conversion ✅ **COMPLETE** (⚠️ Results Not Satisfactory)

**Goal**: Fix polyphony loss and implement automatic time-signature-aware quantization.

**Status**: ✅ Implemented and tested - polyphony preserved, but musical output quality worse than Phase 1

**Problem Statement**:

Phase 1 had fundamental limitations:
1. **Polyphony Loss**: Multiple simultaneous notes (chords) were dropped - only longest note survived
2. **Manual Quantization**: User must guess appropriate --quantize value
3. **Context-Dependent Durations**: `@4` meant different things at 8th vs 16th quantization
4. **50% Occupancy Rule**: Notes occupying <50% of slice were silently ignored

**Example of Polyphony Bug**:
```json
// interstellar.json has 5 simultaneous notes at tick 0:
{ "tick": 0, "note": 53, "name": "F2" },  // F2
{ "tick": 0, "note": 60, "name": "C4" },  // C4
{ "tick": 0, "note": 64, "name": "E4" },  // E4
{ "tick": 0, "note": 65, "name": "F4" },  // F4
{ "tick": 0, "note": 69, "name": "A4" }   // A4

// Phase 1 output (ONLY 2 notes!):
[f2 a4@11]  // C4, E4, F4 dropped by conflict resolution!
```

**Actual Implementation (Phase 1.5)**:

The implementation went through iterative refinement to find optimal grid size:

**Iteration 1 - 24-Grid** (Initial attempt):
- Formula: `slicesPerMeasure = 24 * numerator / denominator`
- 4/4 → 24 slices per measure
- **Problem**: Output sounded choppy, with weird gaps and rushed notes
- **Root cause**: Too fine grid captured micro-timing variations from MIDI quantization errors

**Iteration 2 - 12-Grid** (Refinement):
- Changed GRID_BASE from 24 to 12
- 4/4 → 12 slices per measure
- **Problem**: Still had fractional durations and leading rests

**Iteration 3 - 8-Grid** (Final):
- Changed GRID_BASE from 12 to 8
- 4/4 → 8 slices per measure (8th-note resolution)
- **Result**: Clean output, closest to original MIDI feel
- **Trade-off**: Coarser grid, but musically more coherent

**What Was Successfully Implemented**:

1. ✅ **Removed --quantize Parameter**: Fully automatic grid calculation
   - No user input needed for quantization level
   - Auto-calculates from time signature

2. ✅ **Polyphony Preservation**: ALL simultaneous notes preserved
   - Changed from `String[]` to `List<NoteEvent>[]` 
   - Bracket notation: `[f2,c4,e4,f4,a4@11]`
   - All 5 notes from interstellar.mid now appear in output

3. ✅ **Grid Snapping**: Math.round() instead of floor division
   - Snaps notes to nearest grid slot
   - Prevents micro-timing issues

4. ✅ **Per-Note Duration Tracking**: Quarter-precision rounding
   - `Math.round(duration * 4.0) / 4.0`
   - Allowed values: 0.25, 0.5, 0.75, 1, 1.25, 1.5, 2, etc.
   - Implicit @1: Notes with duration=1 don't show `@1` suffix

5. ✅ **Multi-Time-Signature Validation**: 
   - Throws error if MIDI has multiple time signatures
   - Clear error message with tick positions

6. ✅ **All Tests Passing**: 32 tests across all test classes

**8-Grid Resolution System** (Final Implementation):

| Time Signature | Formula | Slices per Measure | Grid Unit |
|----------------|---------|-------------------|-----------|
| 4/4 | 8 * 4 / 4 | 8 | eighth = 1, quarter = 2, half = 4 |
| 3/4 | 8 * 3 / 4 | 6 | eighth = 1, quarter = 2 |
| 6/8 | 8 * 6 / 8 | 6 | eighth = 1, dotted quarter = 3 |
| 5/4 | 8 * 5 / 4 | 10 | eighth = 1, quarter = 2 |

**Why 8-Grid?** 
- Balances musical accuracy with pattern cleanliness
- 8th-note resolution sufficient for most music
- Avoids capturing unwanted micro-timing
- Produces clean, readable patterns

**Polyphony Representation** (Implemented):

Strudel uses **comma notation** for simultaneous notes:

```javascript
// Single note with duration
[c4@2]           // C4 plays for 2 grid units

// Simultaneous notes (chord) - all with same duration (1 implicit)
[c4,e4,g4]       // C major chord, each note duration = 1

// Simultaneous notes with different durations
[c4@2,e4,g4@1.5] // C4 lasts 2 units, E4 lasts 1 unit (implicit), G4 lasts 1.5 units

// Mixed notes and rests
[~ c4@2,d4 ~ ~]  // Slot 0: rest
                 // Slot 1: C4 (2 units) + D4 (1 unit) start together
                 // Slot 2: rest (C4 still sounding)
                 // Slot 3: rest

// Real-world example (interstellar.mid with 8-grid):
[f2,c4,e4,f4,a4@11 ~ ~ ...]  // All 5 notes preserved!
```

**Duration Precision**:

```java
// Quarter-precision rounding (0.25 steps)
double rawDuration = durationTicks / ticksPerSlice;
double roundedDuration = Math.round(rawDuration * 4.0) / 4.0;

// Examples:
// 1.23 → 1.25
// 1.87 → 1.75  
// 0.12 → 0.25 (minimum)
// 2.51 → 2.5

// Implicit @1 - don't output when duration = 1.0
if (roundedDuration == 1.0) {
    return noteName;  // "c4"
} else {
    return noteName + "@" + roundedDuration;  // "c4@2", "c4@1.5", "c4@0.25"
}
```

**Multi-Time-Signature Validation**:

Phase 1.5 supports **single time signature only**. Multiple time signatures within one file will error:

```java
// In StrudelConverter.java
if (timeSignatures.size() > 1) {
    List<Long> tickPositions = timeSignatures.stream()
        .map(TimeSignatureEntry::tick)
        .collect(Collectors.toList());
    
    throw new UnsupportedOperationException(
        String.format(
            "Multiple time signatures detected (%d changes at ticks: %s). " +
            "Only single time signature files are supported. " +
            "Split your MIDI file by time signature before conversion.",
            timeSignatures.size(),
            tickPositions.stream().map(String::valueOf).collect(Collectors.joining(", "))
        )
    );
}
```

**Implementation Changes** (Completed):

1. **MidiShellCommands.java** (line 93):
   ```java
   // REMOVED --quantize parameter:
   // Before: @ShellOption(help = "Quantization level (8, 16, 32)", defaultValue = "16") Integer quantize
   
   // Command signature became:
   public String convert(
       @ShellOption(help = "Path to MIDI or JSON file") String input,
       @ShellOption(defaultValue = ShellOption.NULL, help = "Output file path") String output,
       @ShellOption(defaultValue = ShellOption.NULL, help = "Override tempo/BPM") Integer tempo,
       @ShellOption(defaultValue = ShellOption.NULL, help = "Convert specific track only") Integer track
   )
   ```

2. **ConversionOptions.java**:
   ```java
   // REMOVED quantization field:
   public record ConversionOptions(
       Integer overrideTempo,    // Override tempo in BPM
       Integer trackIndex        // Track to convert (default: 0)
       // quantization REMOVED - now auto-calculated
   ) {
       public int getEffectiveTrackIndex() {
           return trackIndex != null ? trackIndex : 0;
       }
       // getEffectiveQuantization() REMOVED
   }
   ```

3. **RhythmConverter.java**:
   - Changed GRID_BASE from 24 → 12 → 8 (final)
   - Changed data structure from `String[]` to `List<NoteEvent>[]`
   - Removed 50% occupancy rule and conflict resolution
   - Added grid snapping with `Math.round()`
   - Added polyphonic formatting with comma notation
   - Quarter-precision duration rounding

4. **StrudelConverter.java**:
   - Added multi-time-signature validation (throws error if multiple time sigs)

5. **StrudelTemplate.java**:
   - Updated metadata to show grid information instead of quantization level

**Output Example (Polyphonic with 8-Grid)**:

```javascript
/* "interstellar" */
/**
Source: interstellar.mid
Tempo: 80 BPM
Time Signature: 3/4
Grid: 8-base (6 slices per measure)
Track: 0
Converted: [timestamp]
**/

setcpm(20)

// Polyphonic pattern with all notes preserved
let pattern = note(`<
  [f2,c4,e4,f4,a4@5.5 ~ ~ ~ ~ ~]
  [f2,c4,e4,f4,a4@5.5 ~ ~ ~ ~ ~]
  [g2,d4,f#4,g4,b4@5.5 ~ ~ ~ ~ ~]
>`).room(0.2)

pattern
```

**Output Example (azul.mid with 8-grid)**:

```javascript
/* "azul" */
/**
Source: azul.mid
Tempo: 160 BPM
Time Signature: 4/4
Grid: 8-base (8 slices per measure)
Track: 1
Converted: [timestamp]
**/

setcpm(40)

let pattern = note(`<
  [~ ~ ~ ~] 
  [f6 ~ d6 ~ g#5 ~ g5 f5] 
  [f5,g5 ~ a5 ~ f5@2 ~ d5 ~]
>`).room(0.2)

pattern
```

**Test Coverage**:

- ✅ Polyphony preservation (simultaneous notes)
- ✅ Per-note duration tracking
- ✅ Grid calculation for different time signatures
- ✅ Quarter-precision rounding
- ✅ Implicit @1 omission
- ✅ Grid snapping with Math.round()

**Success Criteria**: ✅ Phase 1.5 Complete (Technical Goals Met)

**Technical Achievements**:
- ✅ All simultaneous notes preserved (no polyphony loss)
- ✅ --quantize parameter removed (fully automatic)
- ✅ 8-grid formula working for all common time signatures
- ✅ Per-note duration tracking with quarter-precision
- ✅ Implicit @1 (notes with duration=1 don't show @1)
- ✅ Multi-time-signature validation throws clear error
- ✅ interstellar.mid converts with all 5 notes: `[f2,c4,e4,f4,a4@5.5]`
- ✅ All 32 tests passing (no regressions, new polyphony tests added)
- ✅ Grid snapping prevents micro-timing issues

**Musical Quality Assessment**: ⚠️ NOT Satisfactory
- ❌ Output sounds less musical than Phase 1 (non-polyphonic)
- ❌ Polyphonic patterns feel cluttered and harder to listen to
- ❌ Duration annotations may be adding complexity without benefit
- ⚠️ 8-grid resolution may still be too coarse or too fine depending on material

**Lessons Learned**:
1. **Grid Size is Critical**: 24-grid too detailed (choppy), 8-grid cleaner but may lose nuance
2. **Polyphony ≠ Better**: Technically correct doesn't mean musically superior
3. **Duration Annotations**: Per-note durations may not be necessary for Strudel patterns
4. **Phase 1 Strengths**: Simpler patterns (without polyphony) easier to understand and customize
5. **Iterative Testing Required**: Musical output quality requires listening tests, not just passing unit tests

**Potential Next Steps** (To Be Decided):
1. Revert to Phase 1 approach (no polyphony, simpler patterns)
2. Make polyphony optional (--polyphonic flag)
3. Implement pattern simplification (remove unnecessary duration annotations)
4. Try different grid sizes (4-grid, 16-grid) for comparison
5. Hybrid approach: preserve polyphony only for chords (3+ simultaneous notes)

**Files Modified** (Completed):
- ✅ `src/main/java/com/marcoalmeida/midi_tokenizer/cli/MidiShellCommands.java` - Removed --quantize
- ✅ `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/ConversionOptions.java` - Removed quantization field
- ✅ `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/RhythmConverter.java` - 8-grid + polyphony + grid snapping
- ✅ `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/StrudelConverter.java` - Multi-time-sig validation
- ✅ `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/StrudelTemplate.java` - Updated metadata

**Test Files Updated** (All tests passing):
- ✅ `src/test/java/com/marcoalmeida/midi_tokenizer/strudel/RhythmConverterTest.java` - Added polyphony tests, updated for 8-grid
- ✅ `src/test/java/com/marcoalmeida/midi_tokenizer/strudel/StrudelConverterTest.java` - Updated for ConversionOptions changes
- ✅ `src/test/java/com/marcoalmeida/midi_tokenizer/strudel/StrudelTemplateTest.java` - Updated expected metadata
- ✅ `src/test/java/com/marcoalmeida/midi_tokenizer/strudel/NoteConverterTest.java` - No changes (still passing)

---

### Phase 2: Multi-Track Output ⏳ **NEXT** (After Phase 1.5 Review)

**Goal**: Output all tracks as separate patterns (user arranges manually in Strudel).

**Status**: ⏳ Planned - waiting for Phase 1.5 review and decision on polyphony approach

**Note**: Phase 2 implementation depends on resolution of Phase 1.5 musical quality issues. May need to:
- Revert to Phase 1 approach (simpler, non-polyphonic patterns)
- Make polyphony optional via flag
- Refine grid size or pattern simplification before proceeding

**Philosophy**: Keep it minimal - generate raw patterns, let users customize arrangement in Strudel.

**Features**:
- Process **all tracks** automatically (no --track option)
- Simple pattern naming: `track0`, `track1`, `track2`, etc.
- All tracks use `note()` (no instrument mapping)
- Basic `stack()` output (user customizes manually)
- All tracks synchronized on same 24-grid

**Input**: Multi-track MIDI (drums, bass, melody)

**Output Example**:
```javascript
/* "full-song" */
/**
Source: full-song.mid
Tempo: 100 BPM
Time Signature: 4/4
Grid: 24-base (24 slices per measure)
Tracks: 3
**/

setcpm(25)

// Track 0
let track0 = note(`<[c4,e4,g4@6 ~ ~ ~] [~@24]>`).room(0.2)

// Track 1
let track1 = note(`<[c2@12 e2@12] [g2@24]>`).room(0.2)

// Track 2
let track2 = note(`<[c5 e5 g5 ~ c5 e5 g5 ~] [~@24]>`).room(0.2)

// Play all tracks together (customize as needed)
stack(track0, track1, track2)
```

**Implementation Changes**:

1. **ConversionOptions.java** - Remove trackIndex:
   ```java
   public record ConversionOptions(
       Integer overrideTempo    // Override tempo in BPM
       // trackIndex REMOVED - always process all tracks
   ) {}
   ```

2. **MidiShellCommands.java** - Remove --track option:
   ```java
   @ShellMethod(key = "convert", value = "Convert MIDI file to Strudel pattern")
   public String convert(
       @ShellOption(help = "Path to MIDI or JSON file") String input,
       @ShellOption(defaultValue = ShellOption.NULL, help = "Output file path") String output,
       @ShellOption(defaultValue = ShellOption.NULL, help = "Override tempo/BPM") Integer tempo
       // --track option REMOVED
   )
   ```

3. **StrudelConverter.java** - Loop all tracks:
   ```java
   // CHANGE: Process all tracks instead of single track
   List<String> trackPatterns = new ArrayList<>();
   for (int i = 0; i < midiData.tracks().size(); i++) {
       TrackOutput track = midiData.tracks().get(i);
       if (track.events().isEmpty()) {
           continue;  // Skip empty tracks
       }
       
       String pattern = RhythmConverter.toQuantizedCyclePattern(
           track.events(), 
           division,
           numerator, 
           denominator
       );
       trackPatterns.add(pattern);
   }
   ```

4. **StrudelTemplate.java** - Render multiple patterns:
   ```java
   // Generate pattern definitions for each track
   StringBuilder patterns = new StringBuilder();
   for (int i = 0; i < trackPatterns.size(); i++) {
       patterns.append(String.format("// Track %d%n", i));
       patterns.append(String.format("let track%d = note(`%s`).room(0.2)%n%n", i, trackPatterns.get(i)));
   }
   
   // Generate simple stack() call
   String trackNames = IntStream.range(0, trackPatterns.size())
       .mapToObj(i -> "track" + i)
       .collect(Collectors.joining(", "));
   patterns.append(String.format("// Play all tracks together (customize as needed)%n"));
   patterns.append(String.format("stack(%s)", trackNames));
   ```

**Success Criteria**: ✅ Phase 2 Complete When:
- ✅ All tracks converted (not just one)
- ✅ Each track outputs as separate `trackN` pattern
- ✅ Simple `stack()` call generated
- ✅ Empty tracks skipped automatically
- ✅ All tracks synchronized on same 24-grid
- ✅ Multi-track test case passes

**Files to Modify** (NO new files created):
- `src/main/java/com/marcoalmeida/midi_tokenizer/cli/MidiShellCommands.java` - Remove --track option
- `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/ConversionOptions.java` - Remove trackIndex field
- `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/StrudelConverter.java` - Loop all tracks
- `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/StrudelTemplate.java` - Output multiple patterns + stack()

**Files to Create**:
- `src/test/java/com/marcoalmeida/midi_tokenizer/strudel/MultiTrackConverterTest.java` - Multi-track integration test

---

## Future Enhancements

Beyond Phase 2, these features could be added:

### Pattern Optimization
- **Collapse consecutive rests**: `[~ ~ ~ ~]` → `[~@4]`
- **Detect repetition**: `[a b c] [a b c]` → `[a b c]*2`
- **Simplify durations**: Remove unnecessary @N notation when implied

### Rhythm Detection

- **Triplet/swing detection**: Auto-detect and add Strudel modifiers in comments
- **Tempo change support**: Multiple `setcpm()` calls for tempo changes within file
- **--verbose flag**: Debugging output showing grid calculations and note mappings

### Advanced Arrangement (Complex)
- **Instrument mapping**: MIDI program → Strudel sound/bank mapping
- **Smart arrange()**: Auto-generate arrangement structure based on track patterns
- **Pattern naming**: Use track names instead of track0, track1
- **Track grouping**: Detect drums/bass/melody and arrange accordingly

### Other
- **Chord detection**: Better representation for dense polyphony
- **Velocity mapping**: Map MIDI velocity to `.gain()` values
- **Pattern library**: Detect known patterns (bossa nova, clave, etc.)
- **MIDI export**: Reverse conversion (Strudel → MIDI)
- **Real-time conversion**: Convert while MIDI plays
- **Batch processing**: Convert multiple MIDI files at once
- **Custom templates**: User-defined output templates

---

## Technical Architecture

### File Structure

```
midi_tokenizer/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   └── java/com/marcoalmeida/midi_tokenizer/
│   │       ├── Application.java
│   │       ├── cli/
│   │       │   └── MidiShellCommands.java        # Add convert command here
│   │       ├── midi/
│   │       │   ├── MidiParser.java               # Existing parser (reuse)
│   │       │   └── NoteUtils.java                # Existing utilities
│   │       ├── model/                             # Existing model classes (reuse)
│   │       │   ├── MidiOutput.java
│   │       │   ├── TrackOutput.java
│   │       │   └── ...
│   │       └── strudel/                           # Strudel conversion package
│   │           ├── StrudelConverter.java          # Main conversion service
│   │           ├── NoteConverter.java             # MIDI note to note name
│   │           ├── RhythmConverter.java           # 24-grid polyphonic rhythm engine
│   │           ├── StrudelTemplate.java           # File template rendering
│   │           └── ConversionOptions.java         # Configuration record
│   └── test/
│       └── java/com/marcoalmeida/midi_tokenizer/
│           ├── MidiParserTest.java                # Existing tests
│           ├── StrudelConverterTest.java          # NEW: Converter tests
│           ├── NoteConverterTest.java             # NEW: Note conversion tests
│           ├── RhythmAnalyzerTest.java            # NEW: Rhythm tests
│           └── TrackProcessorTest.java            # NEW: Track tests
└── test-fixtures/                                  # Sample MIDI files
    ├── simple-melody.mid
    ├── multi-track.mid
    └── son-clave.mid
```

### Core Data Flow

```
MIDI File → MidiParser → MidiOutput (JSON model) - ✅ EXISTING
  ↓
StrudelConverter - ✅ IMPLEMENTED (@Service)
  ↓
[Loop All Tracks] - ⏳ PHASE 2 (currently single track)
  ↓
NoteConverter - ✅ IMPLEMENTED (MIDI note → "c4")
  ↓
RhythmConverter - ⏳ PHASE 1.5 (24-grid + polyphony)
  ├─ Calculate 24-grid (auto from time signature)
  ├─ Collect ALL notes at trigger points
  ├─ Track per-note durations (quarter-precision)
  ├─ Format with comma notation (polyphony)
  └─ Insert rests (~)
  ↓
StrudelTemplate - ✅ IMPLEMENTED (File template rendering)
  ├─ ⏳ PHASE 2: Output multiple track patterns
  └─ ⏳ PHASE 2: Generate stack() call
  ↓
Strudel Pattern File (.txt) - ✅ OUTPUT
```

**Phase 1.5 Flow** (polyphonic single track):
```
MIDI → Parser → Converter → Extract Notes from Track 0 → 
RhythmConverter (24-Grid + Polyphony) → StrudelTemplate → .txt File
```

**Phase 2 Flow** (multi-track):
```
MIDI → Parser → Converter → Loop All Tracks → 
RhythmConverter (24-Grid + Polyphony per track) → 
StrudelTemplate (Multiple Patterns + stack()) → .txt File
```

### Key Classes

- **NoteConverter**: Converts MIDI note numbers to Strudel note names (c4, d#5, etc.)
- **RhythmConverter**: 8-grid polyphonic rhythm conversion with automatic grid calculation
- **StrudelConverter**: Main service coordinating conversion pipeline
- **StrudelTemplate**: Renders output file with metadata and patterns
- **ConversionOptions**: Configuration record (tempo override, track selection)

---

## Testing Strategy

### Test Coverage

- **Unit Tests**: 32 tests across 4 test classes (NoteConverter, RhythmConverter, StrudelConverter, StrudelTemplate)
- **Integration Tests**: Full conversion pipeline testing
- **Test Fixtures**: Sample MIDI files for each phase in `test-fixtures/`

### Test Fixtures

Create sample MIDI files for each phase in `test-fixtures/`:

- **Phase 1**: `simple-melody.mid` - Single track, simple rhythm
- **Phase 1.5**: `interstellar.mid` - Polyphonic single track (chords)
- **Phase 2**: `multi-track.mid` - 3 tracks (drums, bass, melody)

---

## Code Style Guidelines

- Follow standard Java naming conventions (PascalCase for classes, camelCase for methods)
- Use Java records for immutable data classes
- Use `@Service`, `@Component` annotations for Spring beans
- Use JavaDoc for all public methods and classes
- Run `./gradlew test` before committing

---

## Phase Implementation Order

1. **Phase 1**: ✅ COMPLETE - Basic single-track conversion with manual quantization
2. **Phase 1.5**: ✅ COMPLETE (Technically) - Polyphonic 8-grid with auto-calculation
   - ⚠️ Musical quality issues identified
   - 🔄 Under review - may need to revert or refine approach
3. **Phase 2**: ⏳ ON HOLD - Multi-track output (waiting for Phase 1.5 resolution)
4. **Validate output**: Manually test in Strudel REPL after each phase

**Current Status**: Phase 1.5 complete from technical perspective but produces less satisfactory musical results than Phase 1. Need to decide whether to:
- Keep Phase 1.5 and refine (pattern simplification, different grid sizes)
- Revert to Phase 1 approach (simpler patterns without polyphony)
- Make polyphony optional (best of both worlds)

---

## Success Metrics

### Phase 1 Success ✅
- ✅ Converts single-track MIDI to valid Strudel code
- ✅ Generated code plays in Strudel REPL
- ✅ Note names are correct
- ✅ Musical output sounds good (simple, clean patterns)

### Phase 1.5 Success ✅ (Technical) / ⚠️ (Musical)
- ✅ All simultaneous notes preserved (no polyphony loss)
- ✅ 8-grid auto-calculated from time signature
- ✅ Per-note durations with quarter-precision
- ✅ interstellar.mid outputs all 5 notes
- ✅ All tests passing
- ⚠️ Musical quality worse than Phase 1 (patterns feel cluttered)
- ⚠️ Polyphonic output harder to customize in Strudel
- ⚠️ May need pattern simplification or revert to Phase 1 approach

### Phase 2 Success 🔜
- 🔜 All tracks converted (not just first track)
- 🔜 Each track outputs as separate pattern (track0, track1, etc.)
- 🔜 Simple stack() call generated
- 🔜 Empty tracks skipped automatically

---

## Example Command Usage

```bash
# First, run the Spring Shell application
./gradlew bootJar && java -jar build/libs/midi-tokenizer.jar

# Then in the shell:

# Phase 1.5: Single-track conversion (automatic 8-grid calculation)
shell:>convert --input simple-melody.mid --output my-melody.txt

# With tempo override
shell:>convert --input simple-melody.mid --tempo 120

# Auto-generated output file (azul.mid → azul.txt)
shell:>convert --input azul.mid

# Polyphonic conversion (all simultaneous notes preserved)
shell:>convert --input interstellar.mid

# Phase 2: Multi-track conversion (all tracks as separate patterns)
shell:>convert --input full-song.mid --tempo 100
```

---

## Future Enhancements

Beyond Phase 2, these features could be added:

- **Chord detection**: Detect and represent simultaneous notes
- **Velocity mapping**: Map MIDI velocity to `.gain()` values
- **Pattern optimization**: Compress and simplify patterns
- **Pattern detection**: Identify repeating musical phrases
- **MIDI export**: Reverse conversion (Strudel → MIDI)
- **Real-time conversion**: Convert while MIDI plays
- **Pattern library**: Detect known musical patterns (bossa nova, reggae, etc.)
- **Batch processing**: Convert multiple MIDI files at once
- **Custom templates**: User-defined output templates

---

## Resources

### Strudel Documentation
- [First Sounds](https://strudel.cc/workshop/first-sounds/)
- [First Notes](https://strudel.cc/workshop/first-notes/)
- [Recipes](https://strudel.cc/recipes/recipes/)

### MIDI Specification
- [MIDI Messages](https://www.midi.org/specifications)
- [General MIDI](https://en.wikipedia.org/wiki/General_MIDI)

### Java/Spring Testing
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web)

---

## Getting Started

1. **Create Phase 1 files**:
   - `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/StrudelConverter.java`
   - `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/NoteConverter.java`
   - `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/StrudelTemplate.java`
   - `src/test/java/com/marcoalmeida/midi_tokenizer/NoteConverterTest.java`

2. **Add convert command to**:
   - `src/main/java/com/marcoalmeida/midi_tokenizer/cli/MidiShellCommands.java`

3. **Implement basic conversion**:
   ```bash
   ./gradlew test
   ```

4. **Test with simple MIDI file**:
   ```bash
   ./gradlew bootJar && java -jar build/libs/midi-tokenizer.jar
   shell:>convert --input test-fixtures/simple-melody.mid
   ```

5. **Iterate through phases**

---

## Quantization Architecture: Decision Points and Trade-offs

### Why Quantization?

The Phase 1 implementation chose a **quantization grid approach** over the originally planned continuous-time duration approach. This was a pragmatic architectural decision with important implications.

**Key Advantages**:
1. **Simplicity** - Fixed grid is easier to implement and debug than floating-point durations
2. **Determinism** - Same input always produces same output; no rounding edge cases
3. **Readability** - Patterns align to musical grid, easier to read and modify
4. **Performance** - Integer math faster than duration calculations
5. **Works for most music** - Grid-aligned material (most pop, rock, electronic) converts well

**What Quantization Doesn't Capture**:
1. **Micro-timing** - All timing snapped to grid (no humanization)
2. **Context-dependent** - `@4` means different things at different quantization levels
3. **50% threshold** - Notes <50% of a slice are ignored

### Quantization vs. Duration-Based Approach

| Aspect | Quantization (Current) | Duration-Based (Original Plan) |
|--------|----------------------|-------------------------------|
| **Complexity** | Low - integer grid | Medium - float calculations |
| **Accuracy** | Approximate - grid snap | High - preserves exact timing |
| **Triplets** | \u274c Cannot represent | \u2705 Natural representation |
| **Swing** | \u274c Lost | \u2705 Preserved |
| **Readability** | \u2705 Grid-aligned | \u26a0\ufe0f Complex durations |
| **Pattern matching** | \u26a0\ufe0f Harder (context-dependent) | \u2705 Easier (actual values) |
| **Phase 2 sync** | \u2705 Global grid | \u26a0\ufe0f Need alignment logic |
| **Phase 3 rhythm** | \u274c Blocked | \u2705 Possible |

### Recommendations Going Forward

**Current Approach**: Quantization works well for most music. Pick the right grid (8, 12, 16, 24, 32) and convert.

**Path forward**:
1. Complete Phase 2 (multi-track) with global quantization grid
2. Add Phase 3 detection for triplets/swing
3. Add `--verbose` flag for debugging

### Quantization Level Guide

**Choosing the Right Quantization**:

- **8** - Straight eighth notes, simple rhythms (rock, pop)
- **12** - Triplet eighths, shuffle feel, 12/8 time
- **16** - Straight sixteenth notes (DEFAULT) - works for most music
- **24** - Triplet sixteenths, complex jazz, mixed straight/triplet
- **32** - Thirty-second notes, very detailed rhythms
- **6** - Triplet quarters (waltz triplets)
- **20** - Polyrhythm: 5-against-4
- **LCM approach** - For complex polyrhythms, use least common multiple

**Pro Tip**: When in doubt, use a higher quantization (24 or 32) - you can always simplify the pattern manually in Strudel.

---

### Next Phase Priorities

Given quantization approach, recommended order:

1. **Phase 2 Next**:
   - Multi-track with global grid
   - Test synchronization thoroughly
   - **Reason**: Build on solid single-track foundation

2. **Then Phase 3**:
   - Add detection heuristics for triplets/swing
   - Implement warnings for poor grid fit
   - Suggest Strudel modifiers in comments
   - **Reason**: Realistic enhancement within quantization constraints

### Testing Recommendations

1. **Create test MIDI files** for each edge case:
   - Perfect grid alignment (4/4 eighth notes) - should quantize perfectly
   - Triplets - should detect and warn
   - Swing eighth notes - should detect swing ratio
   - Polyrhythm (5 against 4) - should warn about poor fit
   - Mixed quantization needs (drums=16, melody=8)

---

**Remember**: Start simple, test often, and make it work before making it perfect!

The quantization approach is **good enough for most music** and **works now**.
Perfect accuracy can come later through hybrid approach if needed.
