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
# In Spring Shell interactive mode (Phase 1.9):

# Default: Polyphonic conversion
shell:>convert --input input.mid --output output.txt

# Non-polyphonic mode (simpler, better for melodies)
shell:>convert --input input.mid --output output.txt --no-polyphony

# Convert existing JSON to Strudel pattern
shell:>convert --input input.json --output output.txt

# Convert with tempo override
shell:>convert --input input.mid --output output.txt --tempo 120

# Auto-generate output filename (input.mid -> input.txt)
shell:>convert --input input.mid

# With custom quantization
shell:>convert --input jazz.mid --quantize 12

# All options combined
shell:>convert --input song.mid --output out.txt --tempo 120 --quantize 16 --track 1 --no-polyphony
```

### Command Options

- `--input` (required) - Path to MIDI file (.mid, .midi) or JSON file (.json)
- `--output` (optional) - Output file path (auto-generated from input filename if omitted)
- `--tempo` (optional) - Override tempo/BPM
- `--quantize` (optional) - Quantization level (auto-detected from time signature if omitted; use 12 or 24 for triplets)
- `--track` (optional) - Track index to convert (default: 0)
- `--no-polyphony` (optional) - Disable polyphonic conversion, use simpler single-note mode (default: polyphonic enabled)

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

**Decision**: ❌ Phase 1.5 approach rejected - reverting to Phase 1 simplicity with smart quantization defaults

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

### Phase 1.7: Smart Quantization with Phase 1 Simplicity ⏳ **IN PROGRESS**

**Goal**: Return to Phase 1's simpler non-polyphonic approach while keeping Phase 1.5's time signature detection and adding smart quantization defaults.

**Status**: 📋 Planning complete, ready for implementation

**Rationale**: Phase 1.5's polyphonic output with per-note durations produced less musical results than Phase 1's simpler patterns. The overhead of calculating individual note durations made output sound choppy even at the same grid resolution.

**What We're Keeping from Phase 1.5**:
- ✅ Time signature detection and validation (single time signature only)
- ✅ Multi-time-signature error with clear message
- ✅ Default 4/4 fallback if no time signature found

**What We're Reverting from Phase 1**:
- ✅ 50% occupancy rule (notes must occupy >50% of slice)
- ✅ Conflict resolution (longest duration wins when multiple notes compete)
- ✅ Slice-count `@N` notation (not per-note quarter-precision)
- ✅ Simple `String[]` data structure (not `List<NoteEvent>[]`)
- ✅ Non-polyphonic output (single note per slice)

**What's New in Phase 1.7**:
- ✅ **Smart quantization defaults** based on detected time signature
- ✅ **Optional --quantize override** for triplets/complex rhythms
- ✅ **Enhanced metadata** showing time signature, quantization, and grid meaning

**Smart Quantization Defaults**:

| Time Signature | Default Quantization | Slices per Measure | Grid Meaning |
|----------------|---------------------|-------------------|--------------|
| 4/4 | 16 | 16 | 16th note resolution, @4 = quarter note |
| 3/4 | 6 | 6 | Eighth note resolution (waltz feel) |
| 6/8 | 6 | 6 | Eighth note resolution (compound meter) |
| 2/4 | 16 | 8 | 16th note resolution, @4 = quarter note |
| Other | 16 | varies | 16th note default, user can override |

**Rationale for Defaults**:
- **4/4, 2/4**: Most common, use 16th note grid (captures sixteenth notes, works for most pop/rock/electronic)
- **3/4**: Waltz feel typically uses eighth notes, not sixteenth notes - use 6 (not 12)
- **6/8**: Compound meter, eighth note is the pulse - use 6 (not 12)
- **User can override**: For triplets, use `--quantize 12` or `--quantize 24`

**Command Signature** (Updated):

```bash
# Use smart default (4/4 → 16)
shell:>convert --input song.mid

# Override for triplets
shell:>convert --input jazz.mid --quantize 12

# Override for very detailed rhythms
shell:>convert --input complex.mid --quantize 32

# All parameters
shell:>convert --input song.mid --output out.txt --tempo 120 --quantize 24 --track 1
```

**Implementation Changes**:

1. **ConversionOptions.java** - Add back quantization field:
   ```java
   public record ConversionOptions(
       Integer overrideTempo,    // Override tempo in BPM
       Integer trackIndex,       // Track to convert (default: 0)
       Integer quantization      // Quantization level (optional, auto-calculated if null)
   ) {
       public int getEffectiveTrackIndex() {
           return trackIndex != null ? trackIndex : 0;
       }
       
       // NEW: Get quantization level (use override or calculate from time signature)
       public int getEffectiveQuantization(int numerator, int denominator) {
           if (quantization != null) {
               return quantization;
           }
           
           // Smart defaults based on time signature
           if (numerator == 3 && denominator == 4) return 6;   // 3/4 waltz
           if (numerator == 6 && denominator == 8) return 6;   // 6/8 compound
           if (numerator == 2 && denominator == 4) return 16;  // 2/4 march
           return 16;  // Default: 4/4 and other time signatures
       }
   }
   ```

2. **MidiShellCommands.java** - Restore --quantize parameter:
   ```java
   @ShellMethod(key = "convert", value = "Convert MIDI file to Strudel pattern")
   public String convert(
       @ShellOption(help = "Path to MIDI or JSON file") String input,
       @ShellOption(defaultValue = ShellOption.NULL, help = "Output file path") String output,
       @ShellOption(defaultValue = ShellOption.NULL, help = "Override tempo/BPM") Integer tempo,
       @ShellOption(defaultValue = ShellOption.NULL, help = "Convert specific track only") Integer track,
       @ShellOption(defaultValue = ShellOption.NULL, help = "Quantization level (optional, auto-detected)") Integer quantize
   ) {
       ConversionOptions options = new ConversionOptions(tempo, track, quantize);
       // ... rest of method
   }
   ```

3. **RhythmConverter.java** - Revert to Phase 1 approach:
   - Remove `GRID_BASE` constant (no longer needed)
   - Change data structure from `List<NoteEvent>[]` back to `String[]`
   - Restore 50% occupancy rule
   - Restore conflict resolution (longest duration wins)
   - Remove per-note duration tracking
   - Restore slice-count `@N` notation
   - Use floor division instead of `Math.round()` for grid snapping
   
   ```java
   /**
    * Converts MIDI note events to quantized Strudel cycle pattern using fixed-grid approach.
    * 
    * Algorithm:
    * 1. Calculate slices per measure from quantization level and time signature
    * 2. For each slice, find notes that occupy >50% of slice duration
    * 3. If multiple notes compete, longest total duration wins
    * 4. Merge consecutive identical notes with @N notation (slice count)
    * 5. Empty slices become rests (~)
    * 
    * @param events MIDI note events (assumed sorted by tick)
    * @param division MIDI division (ticks per quarter note)
    * @param numerator Time signature numerator
    * @param denominator Time signature denominator
    * @param quantization Quantization level (slices per 4/4 measure)
    * @return Strudel pattern string wrapped in <>
    */
   public static String toQuantizedCyclePattern(
       List<NoteEvent> events, 
       int division, 
       int numerator, 
       int denominator,
       int quantization  // NEW parameter
   ) {
       // Calculate slices per measure based on quantization and time signature
       int slicesPerMeasure = (quantization * numerator) / denominator;
       
       // ... rest of Phase 1 algorithm
   }
   ```

4. **StrudelConverter.java** - Pass quantization to RhythmConverter:
   ```java
   // Get effective quantization (override or smart default)
   int quantization = options.getEffectiveQuantization(numerator, denominator);
   
   // Convert to pattern
   String pattern = RhythmConverter.toQuantizedCyclePattern(
       track.events(), 
       division,
       numerator, 
       denominator,
       quantization  // NEW parameter
   );
   ```

5. **StrudelTemplate.java** - Enhanced metadata:
   ```java
   /**
   Source: {filename}
   Tempo: {tempo} BPM
   Time Signature: {numerator}/{denominator}
   Quantization: {quantization} ({default|override})
   Grid: {gridMeaning}
   Track: {trackIndex}
   Converted: {timestamp}
   **/
   ```
   
   Where `gridMeaning` examples:
   - `16 = sixteenth notes, @4 = quarter note, @8 = half note`
   - `6 = eighth notes (3/4), @3 = dotted quarter, @6 = dotted half`
   - `12 = triplet eighths, @3 = quarter note, @6 = half note`

**Output Example (4/4 with default quantization=16)**:

```javascript
/* "azul" */
/**
Source: azul.mid
Tempo: 160 BPM
Time Signature: 4/4
Quantization: 16 (default)
Grid: 16 = sixteenth notes, @4 = quarter note, @8 = half note
Track: 1
Converted: 2026-01-13 15:30:00
**/

setcpm(40)

let pattern = note(`<
  [~ ~ ~ ~ ~ ~ ~ ~] 
  [f6 ~ d6 ~ g#5 ~ g5 f5] 
  [f5 ~ a5 ~ f5@4 d5 ~]
>`).room(0.2)

pattern
```

**Output Example (3/4 with default quantization=6)**:

```javascript
/* "waltz" */
/**
Source: waltz.mid
Tempo: 180 BPM
Time Signature: 3/4
Quantization: 6 (default)
Grid: 6 = eighth notes, @3 = dotted quarter, @6 = dotted half
Track: 0
Converted: 2026-01-13 15:35:00
**/

setcpm(45)

let pattern = note(`<
  [c4 e4 g4 c4 e4 g4]
  [d4@3 ~ ~ d4@3]
>`).room(0.2)

pattern
```

**Output Example (Override with --quantize 12)**:

```javascript
/* "triplet-song" */
/**
Source: triplet-song.mid
Tempo: 120 BPM
Time Signature: 4/4
Quantization: 12 (override)
Grid: 12 = triplet eighths, @3 = quarter note, @6 = half note
Track: 0
Converted: 2026-01-13 15:40:00
**/

setcpm(30)

let pattern = note(`<
  [c4 ~ ~ d4 ~ ~ e4 ~ ~ c4 ~ ~]
  [f4@6 ~ ~ ~ ~ ~ g4@6]
>`).room(0.2)

pattern
```

**Test Coverage** (To Be Updated):

Remove polyphony tests, add:
- ✅ Smart default quantization for 4/4 (should be 16)
- ✅ Smart default quantization for 3/4 (should be 6)
- ✅ Smart default quantization for 6/8 (should be 6)
- ✅ Smart default quantization for 2/4 (should be 16)
- ✅ Override default with --quantize parameter
- ✅ 50% occupancy rule edge cases (49% ignored, 51% included)
- ✅ Conflict resolution (longest duration wins)
- ✅ Slice-count duration merging (@N notation)
- ✅ Multi-time-signature validation still works

**Success Criteria**: ✅ Phase 1.7 Complete When:
- ✅ Reverted to non-polyphonic output (single note per slice)
- ✅ Restored 50% occupancy rule
- ✅ Restored conflict resolution
- ✅ Restored slice-count `@N` notation
- ✅ Smart quantization defaults working (4/4→16, 3/4→6, 6/8→6)
- ✅ --quantize override working
- ✅ Enhanced metadata showing quantization and grid meaning
- ✅ All tests passing
- ✅ Musical output quality as good as or better than Phase 1

**Philosophy**:
- **Keep it simple**: No polyphony, no per-note durations, no automatic detection
- **Smart defaults**: Good defaults for common time signatures
- **User control**: Easy override when defaults don't fit
- **Clear documentation**: Metadata explains what the grid means

**Files to Modify** (All existing files):
- `src/main/java/com/marcoalmeida/midi_tokenizer/cli/MidiShellCommands.java` - Add --quantize parameter
- `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/ConversionOptions.java` - Add quantization field + getEffectiveQuantization()
- `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/RhythmConverter.java` - Revert to Phase 1 algorithm + quantization parameter
- `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/StrudelConverter.java` - Use getEffectiveQuantization()
- `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/StrudelTemplate.java` - Enhanced metadata

**Test Files to Update**:
- `src/test/java/com/marcoalmeida/midi_tokenizer/strudel/RhythmConverterTest.java` - Remove polyphony tests, add smart default tests
- `src/test/java/com/marcoalmeida/midi_tokenizer/strudel/StrudelConverterTest.java` - Update for ConversionOptions changes
- `src/test/java/com/marcoalmeida/midi_tokenizer/strudel/StrudelTemplateTest.java` - Update expected metadata format
- `src/test/java/com/marcoalmeida/midi_tokenizer/strudel/NoteConverterTest.java` - No changes needed

---

### Phase 1.8: Polyphonic Conversion with Integer Durations 📋 **PLANNED**

**Goal**: Implement polyphonic preservation using grid-snapped positions with integer durations, avoiding the musical quality issues from Phase 1.5.

**Status**: 📋 Planning complete, awaiting implementation

**Key Differences from Phase 1.5**:

| Aspect | Phase 1.5 (Rejected) | Phase 1.8 (Planned) |
|--------|---------------------|-------------------|
| **Position Calculation** | tickPosition / ticksPerSlice (floor) | Math.round(timeSeconds / sliceTimeSeconds) |
| **Duration Precision** | Quarter-precision (0.25, 0.5, 0.75, 1.25) | Integer only (1, 2, 3, 4) |
| **Minimum Duration** | 0.25 slices | 1 slice (round up) |
| **Data Structure** | List<NoteEvent>[] with durations | Map<Integer, List<NoteWithDuration>> |
| **Merging** | Consecutive identical notes merged | NO merging (preserve all) |
| **Rest Notation** | ~ per slot | [~@16] for full measure |

**Why These Changes?**

1. **timeSeconds-based positioning**: More accurate than tick-based division, respects actual MIDI timing
2. **Integer durations**: Simpler, more readable patterns (no 1.25, 0.75, etc.)
3. **Round up minimum**: Never lose notes (0.4 slices → 1 slice, not dropped)
4. **No consecutive merging**: Preserve all note events as-is (important for legato vs. re-triggered notes)
5. **Compact rest notation**: [~@16] cleaner than [~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~]

**Algorithm Pseudocode**:

```java
// Data structure
Map<Integer, List<NoteWithDuration>> grid = new HashMap<>();

class NoteWithDuration {
    String noteName;  // "c4", "d#5", etc.
    int duration;     // Integer slices (1, 2, 3, ...)
}

// Step 1: Calculate grid parameters
double sliceTimeSeconds = (60.0 / tempo) * (4.0 / quantization);
int slicesPerMeasure = (quantization * numerator) / denominator;

// Step 2: Populate grid with notes (NO merging, NO conflict resolution)
for (NoteEvent event : events) {
    // Calculate position using timeSeconds
    int gridPosition = (int) Math.round(event.timeSeconds / sliceTimeSeconds);
    
    // Calculate duration in slices
    double durationInSlices = event.durationSeconds / sliceTimeSeconds;
    int integerDuration = (int) Math.round(durationInSlices);
    
    // Minimum duration = 1 (always round up)
    if (integerDuration < 1) {
        integerDuration = 1;
    }
    
    // Add to grid (allow multiple notes at same position)
    grid.computeIfAbsent(gridPosition, k -> new ArrayList<>())
        .add(new NoteWithDuration(noteName, integerDuration));
}

// Step 3: Format output measure by measure
for (int measure = 0; measure < totalMeasures; measure++) {
    int measureStart = measure * slicesPerMeasure;
    int measureEnd = measureStart + slicesPerMeasure;
    
    // Check if entire measure is empty
    boolean isEmpty = true;
    for (int i = measureStart; i < measureEnd; i++) {
        if (grid.containsKey(i)) {
            isEmpty = false;
            break;
        }
    }
    
    if (isEmpty) {
        // Use compact notation for empty measure
        output.append("[~@").append(slicesPerMeasure).append("] ");
    } else {
        output.append("[");
        for (int i = measureStart; i < measureEnd; i++) {
            if (grid.containsKey(i)) {
                List<NoteWithDuration> notes = grid.get(i);
                
                // Format as chord if multiple notes
                if (notes.size() > 1) {
                    output.append("[");
                    for (int j = 0; j < notes.size(); j++) {
                        NoteWithDuration note = notes.get(j);
                        output.append(note.noteName);
                        if (note.duration > 1) {
                            output.append("@").append(note.duration);
                        }
                        if (j < notes.size() - 1) {
                            output.append(",");  // No spaces
                        }
                    }
                    output.append("]");
                } else {
                    // Single note
                    NoteWithDuration note = notes.get(0);
                    output.append(note.noteName);
                    if (note.duration > 1) {
                        output.append("@").append(note.duration);
                    }
                }
            } else {
                // Empty slot
                output.append("~");
            }
            
            if (i < measureEnd - 1) {
                output.append(" ");
            }
        }
        output.append("] ");
    }
}
```

**Design Decisions** (User-Confirmed):

1. ✅ **Minimum Duration**: Notes with duration < 0.5 slices → round up to 1 (never drop notes)
2. ✅ **Empty Measure Notation**: Use compact `[~@16]` instead of `[~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~]`
3. ✅ **Chord Formatting**: No spaces → `[c4,e4@2,g4]` (not `[c4, e4@2, g4]`)
4. ✅ **Note Order**: Preserve MIDI order (don't sort by pitch)

**Example Conversions**:

**Input**: 3 simultaneous notes starting at tick 0, different durations
- C4: 0→480 ticks (quarter note)
- E4: 0→240 ticks (eighth note)  
- G4: 0→720 ticks (dotted quarter)

**Output** (quantization=16, 4/4):
```javascript
[[c4@4,e4@2,g4@6] ~ ~ ~ ~ ~ ~ ~ ~ ~ ~]
// C4 lasts 4 sixteenths (quarter note)
// E4 lasts 2 sixteenths (eighth note)
// G4 lasts 6 sixteenths (dotted quarter)
// All start together at position 0
```

**Empty Measure**:
```javascript
// Phase 1.5 (verbose):
[~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~]

// Phase 1.8 (compact):
[~@16]
```

**Implementation Changes**:

1. **RhythmConverter.java** - Complete rewrite:
   - Change from `String[]` to `Map<Integer, List<NoteWithDuration>>`
   - Use `timeSeconds` for position calculation (not tick-based division)
   - Use `Math.round()` for both position and duration
   - Integer durations only (1, 2, 3, ...) with minimum = 1
   - NO consecutive merging (preserve all notes)
   - Use `[~@N]` for empty measures

2. **No changes needed**:
   - ConversionOptions.java (already has quantization field)
   - MidiShellCommands.java (already has --quantize parameter)
   - StrudelConverter.java (already passes quantization)
   - StrudelTemplate.java (already has metadata format)

**Test Coverage** (To Be Added):

- ✅ Polyphonic notes with different integer durations
- ✅ Minimum duration rounding (0.4 slices → 1)
- ✅ Empty measure compact notation [~@16]
- ✅ Chord formatting without spaces [c4,e4@2,g4]
- ✅ timeSeconds-based positioning accuracy
- ✅ No consecutive note merging

**Success Criteria**: ✅ Phase 1.8 Complete When:
- ✅ All simultaneous notes preserved with integer durations
- ✅ Position calculated using timeSeconds (not tick division)
- ✅ Minimum duration = 1 (round up, never drop)
- ✅ Empty measures use [~@N] notation
- ✅ Chords formatted without spaces
- ✅ NO consecutive merging (preserve note boundaries)
- ✅ All tests passing
- ✅ Musical output quality better than Phase 1.5

**Files to Modify**:
- `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/RhythmConverter.java` - Complete rewrite

**Test Files to Update**:
- `src/test/java/com/marcoalmeida/midi_tokenizer/strudel/RhythmConverterTest.java` - Add Phase 1.8 tests

---

### Phase 1.9: Polyphonic Toggle with --no-polyphony Flag 📋 **PLANNED**

**Goal**: Provide user choice between polyphonic (Phase 1.8) and non-polyphonic (Phase 1.7) output via command-line flag, combining best of both approaches.

**Status**: 📋 Planning - Phase 1.8 complete, awaiting implementation

**Rationale**: Real-world testing shows mixed results with polyphonic conversion:
- ✅ **Works well**: Dense chords, piano parts, layered synths (e.g., interstellar.mid, in_blue.mid)
- ❌ **Works poorly**: Simple melodies, monophonic instruments, some MIDI files have timing artifacts that create unwanted polyphony (e.g., azul.mid)
- 💡 **Solution**: Let user choose based on their specific MIDI file and musical intent

**Real-World Examples**:
- **interstellar.mid**: Use default (polyphonic) - preserves beautiful chord voicings
- **in_blue.mid**: Use default (polyphonic) - good results with layered parts
- **azul.mid**: Use `--no-polyphony` - cleaner melodic output without timing artifacts

**Command Signature**:

```bash
# Default: Polyphonic conversion (Phase 1.8)
shell:>convert --input song.mid

# Non-polyphonic: Simple single-note approach (Phase 1.7 style)
shell:>convert --input song.mid --no-polyphony

# With all parameters
shell:>convert --input song.mid --output out.txt --tempo 120 --quantize 16 --track 1 --no-polyphony
```

**Behavioral Differences**:

| Feature | Default (Polyphonic) | With --no-polyphony |
|---------|---------------------|-------------------|
| **Simultaneous Notes** | Preserved as chords `[c4,e4,g4]` | Conflict resolution (longest wins) |
| **Grid Occupancy** | All notes captured | 50% occupancy rule |
| **Duration Merging** | No merging (preserve boundaries) | Consecutive identical notes merged |
| **Positioning** | Time-based (timeSeconds) | Time-based (timeSeconds) |
| **Duration Precision** | Integer only (1, 2, 3) | Integer only (1, 2, 3) |
| **Minimum Duration** | 1 slice (round up) | 1 slice (round up) |
| **Empty Measures** | Compact `[~@16]` | Compact `[~@16]` |
| **Best For** | Chords, piano, dense harmony | Melodies, bass lines, single instruments |

**Key Design Decision**: Both modes use **Phase 1.8's improvements** (time-based positioning, integer durations) but differ in polyphony handling.

**Implementation Strategy**:

1. **ConversionOptions.java** - Add polyphony flag:
   ```java
   public record ConversionOptions(
       Integer overrideTempo,    // Override tempo in BPM
       Integer trackIndex,       // Track to convert (default: 0)
       Integer quantization,     // Quantization level (optional, auto-calculated)
       Boolean enablePolyphony   // Enable polyphonic conversion (default: true)
   ) {
       public int getEffectiveTrackIndex() {
           return trackIndex != null ? trackIndex : 0;
       }
       
       public int getEffectiveQuantization(int numerator, int denominator) {
           if (quantization != null) {
               return quantization;
           }
           
           // Smart defaults based on time signature
           if (numerator == 3 && denominator == 4) return 6;   // 3/4 waltz
           if (numerator == 6 && denominator == 8) return 6;   // 6/8 compound
           if (numerator == 2 && denominator == 4) return 16;  // 2/4 march
           return 16;  // Default: 4/4 and other time signatures
       }
       
       public boolean isPolyphonicMode() {
           return enablePolyphony == null || enablePolyphony;  // Default: true
       }
   }
   ```

2. **MidiShellCommands.java** - Add --no-polyphony flag:
   ```java
   @ShellMethod(key = "convert", value = "Convert MIDI file to Strudel pattern")
   public String convert(
       @ShellOption(help = "Path to MIDI or JSON file") String input,
       @ShellOption(defaultValue = ShellOption.NULL, help = "Output file path") String output,
       @ShellOption(defaultValue = ShellOption.NULL, help = "Override tempo/BPM") Integer tempo,
       @ShellOption(defaultValue = ShellOption.NULL, help = "Convert specific track only") Integer track,
       @ShellOption(defaultValue = ShellOption.NULL, help = "Quantization level (optional, auto-detected)") Integer quantize,
       @ShellOption(defaultValue = "false", help = "Disable polyphonic conversion (use simple single-note mode)") boolean noPolyphony
   ) {
       ConversionOptions options = new ConversionOptions(tempo, track, quantize, !noPolyphony);
       // ... rest of method
   }
   ```

3. **RhythmConverter.java** - Dual algorithm with shared time-based positioning:
   ```java
   /**
    * Converts MIDI note events to Strudel cycle pattern.
    * Supports both polyphonic and non-polyphonic modes.
    * 
    * @param noteEvents   MIDI note events with timeSeconds and durationSeconds
    * @param division     MIDI division (ticks per quarter note)
    * @param numerator    Time signature numerator
    * @param denominator  Time signature denominator
    * @param quantization Quantization level (slices per 4/4 measure)
    * @param tempo        Tempo in BPM
    * @param polyphonic   Enable polyphonic mode (true) or non-polyphonic (false)
    * @return Strudel pattern string wrapped in <>
    */
   public static String toQuantizedCyclePattern(
       List<EventOutput> noteEvents,
       int division,
       int numerator,
       int denominator,
       int quantization,
       int tempo,
       boolean polyphonic
   ) {
       if (noteEvents.isEmpty()) {
           return "";
       }

       if (polyphonic) {
           return toPolyphonicPattern(noteEvents, division, numerator, denominator, quantization, tempo);
       } else {
           return toNonPolyphonicPattern(noteEvents, division, numerator, denominator, quantization, tempo);
       }
   }

   /**
    * Phase 1.8 algorithm: Polyphonic preservation with integer durations.
    * - Preserves ALL simultaneous notes as chords [c4,e4,g4]
    * - No conflict resolution, no merging
    * - Integer durations only
    */
   private static String toPolyphonicPattern(...) {
       // Current Phase 1.8 implementation
       // Map<Integer, List<NoteWithDuration>> grid
       // ...
   }

   /**
    * Phase 1.7 style algorithm: Non-polyphonic with time-based positioning.
    * - 50% occupancy rule: note must occupy >50% of slice
    * - Conflict resolution: longest duration wins
    * - Consecutive identical notes merged with @N notation
    * - Uses timeSeconds for positioning (Phase 1.8 improvement)
    * - Integer durations only (Phase 1.8 improvement)
    */
   private static String toNonPolyphonicPattern(
       List<EventOutput> noteEvents,
       int division,
       int numerator,
       int denominator,
       int quantization,
       int tempo
   ) {
       // Calculate grid parameters (same as polyphonic)
       int slicesPerMeasure = (quantization * numerator) / denominator;
       double sliceTimeSeconds = (60.0 / tempo) * (4.0 / quantization);
       
       // Find maximum position
       int maxPosition = 0;
       for (EventOutput event : noteEvents) {
           int gridPosition = (int) Math.round(event.getTimeSeconds() / sliceTimeSeconds);
           if (gridPosition > maxPosition) {
               maxPosition = gridPosition;
           }
       }
       
       int numMeasures = (maxPosition / slicesPerMeasure) + 1;
       
       // Create grid to hold note names (single note per slice)
       String[] slices = new String[slicesPerMeasure * numMeasures];
       Arrays.fill(slices, "");
       
       // Place notes in grid using 50% occupancy rule + conflict resolution
       for (EventOutput event : noteEvents) {
           double noteStartTime = event.getTimeSeconds();
           double noteEndTime = noteStartTime + event.getDurationSeconds();
           String noteName = NoteConverter.toStrudelNoteName(event.getNoteNumber());
           
           // Calculate which slices this note occupies >50%
           int startSlice = (int) Math.round(noteStartTime / sliceTimeSeconds);
           int endSlice = (int) Math.round(noteEndTime / sliceTimeSeconds);
           
           for (int sliceIdx = startSlice; sliceIdx < endSlice && sliceIdx < slices.length; sliceIdx++) {
               // Calculate how much of this slice the note occupies
               double sliceStart = sliceIdx * sliceTimeSeconds;
               double sliceEnd = (sliceIdx + 1) * sliceTimeSeconds;
               
               // Calculate overlap
               double overlapStart = Math.max(noteStartTime, sliceStart);
               double overlapEnd = Math.min(noteEndTime, sliceEnd);
               double overlapDuration = overlapEnd - overlapStart;
               
               // 50% occupancy rule: note must occupy >50% of slice
               if (overlapDuration > sliceTimeSeconds / 2) {
                   // Conflict resolution: if slot already taken, longest duration wins
                   if (slices[sliceIdx].isEmpty()) {
                       slices[sliceIdx] = noteName;
                   } else {
                       // Find duration of existing note
                       double existingDuration = findNoteDuration(slices[sliceIdx], noteEvents);
                       if (event.getDurationSeconds() > existingDuration) {
                           slices[sliceIdx] = noteName;
                       }
                   }
               }
           }
       }
       
       // Build pattern with consecutive note merging
       StringBuilder pattern = new StringBuilder();
       pattern.append("<");
       
       for (int measure = 0; measure < numMeasures; measure++) {
           int measureStart = measure * slicesPerMeasure;
           int measureEnd = measureStart + slicesPerMeasure;
           
           // Check if entire measure is empty
           boolean isEmpty = true;
           for (int i = measureStart; i < measureEnd; i++) {
               if (!slices[i].isEmpty()) {
                   isEmpty = false;
                   break;
               }
           }
           
           if (isEmpty) {
               // Compact rest notation
               pattern.append("[~@").append(slicesPerMeasure).append("]");
           } else {
               pattern.append("[");
               
               int sliceIdx = 0;
               while (sliceIdx < slicesPerMeasure) {
                   int absoluteSliceIdx = measureStart + sliceIdx;
                   
                   if (absoluteSliceIdx >= slices.length) {
                       break;
                   }
                   
                   String currentNote = slices[absoluteSliceIdx];
                   
                   if (currentNote.isEmpty()) {
                       // Rest - count consecutive
                       int restCount = 1;
                       while (sliceIdx + restCount < slicesPerMeasure) {
                           int nextAbsIdx = measureStart + sliceIdx + restCount;
                           if (nextAbsIdx >= slices.length || !slices[nextAbsIdx].isEmpty()) {
                               break;
                           }
                           restCount++;
                       }
                       
                       if (restCount == 1) {
                           pattern.append("~");
                       } else {
                           pattern.append("~@").append(restCount);
                       }
                       sliceIdx += restCount;
                   } else {
                       // Note - count consecutive identical (merging)
                       int noteCount = 1;
                       while (sliceIdx + noteCount < slicesPerMeasure) {
                           int nextAbsIdx = measureStart + sliceIdx + noteCount;
                           if (nextAbsIdx >= slices.length || !currentNote.equals(slices[nextAbsIdx])) {
                               break;
                           }
                           noteCount++;
                       }
                       
                       if (noteCount == 1) {
                           pattern.append(currentNote);
                       } else {
                           pattern.append(currentNote).append("@").append(noteCount);
                       }
                       sliceIdx += noteCount;
                   }
                   
                   if (sliceIdx < slicesPerMeasure) {
                       pattern.append(" ");
                   }
               }
               
               pattern.append("]");
           }
           
           if (measure < numMeasures - 1) {
               pattern.append(" ");
           }
       }
       
       pattern.append(">");
       return pattern.toString();
   }
   
   private static double findNoteDuration(String noteName, List<EventOutput> noteEvents) {
       for (EventOutput event : noteEvents) {
           if (NoteConverter.toStrudelNoteName(event.getNoteNumber()).equals(noteName)) {
               return event.getDurationSeconds();
           }
       }
       return 0.0;
   }
   ```

4. **StrudelConverter.java** - Pass polyphony flag:
   ```java
   // Convert to quantized cycle-based pattern (Phase 1.9: with polyphony toggle)
   String pattern = RhythmConverter.toQuantizedCyclePattern(
       noteEvents,
       midiOutput.getFile().getDivision(),
       timeSignatureNumerator,
       timeSignatureDenominator,
       quantization,
       (int) Math.round(bpm),
       options.isPolyphonicMode()  // NEW: polyphony flag
   );
   ```

5. **StrudelTemplate.java** - Updated metadata:
   ```java
   /**
   Source: {filename}
   Tempo: {tempo} BPM
   Time Signature: {numerator}/{denominator}
   Quantization: {quantization} ({default|override})
   Grid: {gridMeaning}
   Mode: {Polyphonic|Non-polyphonic}  // NEW field
   Track: {trackIndex}
   Converted: {timestamp}
   **/
   ```

**Output Examples**:

**Default (Polyphonic)**:
```javascript
/* "interstellar" */
/**
Source: interstellar.mid
Tempo: 94 BPM
Time Signature: 3/4
Quantization: 6 (default)
Grid: 6 = eighth notes, @3 = dotted quarter, @6 = dotted half
Mode: Polyphonic
Track: 0
Converted: 2026-01-13
**/

setcpm(94/3)

let track_0 = note(`<
[[a4,a5,f2@9,c4@9,e4@9,f4@9,a4@9] e6@3 ~ ~]
[[a4,a5] ~ e6@3 ~]
>`).room(0.2)

track_0
```

**With --no-polyphony**:
```javascript
/* "interstellar" */
/**
Source: interstellar.mid
Tempo: 94 BPM
Time Signature: 3/4
Quantization: 6 (default)
Grid: 6 = eighth notes, @3 = dotted quarter, @6 = dotted half
Mode: Non-polyphonic
Track: 0
Converted: 2026-01-13
**/

setcpm(94/3)

let track_0 = note(`<
[a4@9 e6@3 ~ ~]
[a4 ~ e6@3 ~]
>`).room(0.2)

track_0
```

**Test Coverage** (To Be Added):

- ✅ Polyphonic mode preserves simultaneous notes
- ✅ Non-polyphonic mode applies 50% occupancy rule
- ✅ Non-polyphonic mode resolves conflicts (longest wins)
- ✅ Non-polyphonic mode merges consecutive identical notes
- ✅ Both modes use time-based positioning
- ✅ Both modes use integer durations
- ✅ Both modes use compact rest notation
- ✅ Flag correctly toggles between modes
- ✅ Metadata reflects current mode

**Success Criteria**: ✅ Phase 1.9 Complete When:
- ✅ --no-polyphony flag working in command line
- ✅ Default behavior is polyphonic (Phase 1.8)
- ✅ --no-polyphony uses non-polyphonic algorithm (Phase 1.7 style with Phase 1.8 improvements)
- ✅ Both modes share time-based positioning and integer durations
- ✅ Both modes tested with real MIDI files
- ✅ Metadata shows current mode (Polyphonic/Non-polyphonic)
- ✅ All tests passing
- ✅ Users can choose best mode for their specific MIDI file

**Philosophy**:
- **Give users control**: Some files work better polyphonic, others non-polyphonic
- **Same underlying tech**: Both use Phase 1.8's time-based positioning and integer durations
- **Different use cases**: Polyphonic for chords/piano, non-polyphonic for melodies/bass
- **Easy to compare**: Same file, different flags, choose what sounds better

**Files to Modify**:
- `src/main/java/com/marcoalmeida/midi_tokenizer/cli/MidiShellCommands.java` - Add --no-polyphony flag
- `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/ConversionOptions.java` - Add enablePolyphony field
- `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/RhythmConverter.java` - Add dual-mode logic
- `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/StrudelConverter.java` - Pass polyphony flag
- `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/StrudelTemplate.java` - Add mode to metadata

**Test Files to Update**:
- `src/test/java/com/marcoalmeida/midi_tokenizer/strudel/RhythmConverterTest.java` - Add non-polyphonic mode tests
- `src/test/java/com/marcoalmeida/midi_tokenizer/strudel/StrudelConverterTest.java` - Test flag behavior
- `src/test/java/com/marcoalmeida/midi_tokenizer/strudel/StrudelTemplateTest.java` - Update metadata tests

---

### Phase 2: Multi-Track Output ⏳ **NEXT** (After Phase 1.9 Complete)

**Goal**: Output all tracks as separate patterns (user arranges manually in Strudel).

**Status**: ⏳ Planned - waiting for Phase 1.9 completion

**Note**: Phase 2 will build on Phase 1.9's flexible polyphonic/non-polyphonic approach with smart quantization defaults.

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
- **RhythmConverter**: Dual-mode rhythm conversion with polyphonic/non-polyphonic support (Phase 1.9)
- **StrudelConverter**: Main service coordinating conversion pipeline
- **StrudelTemplate**: Renders output file with metadata and patterns
- **ConversionOptions**: Configuration record (tempo override, track selection, quantization, polyphony mode)
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
2. **Phase 1.5**: ❌ REJECTED (Musically) - Polyphonic 8-grid with auto-calculation
   - Polyphonic output sounded less musical than Phase 1 for some files
   - Per-note quarter-precision durations added unnecessary complexity
   - Decision: Need user choice between polyphonic/non-polyphonic
3. **Phase 1.7**: ✅ COMPLETE (Conceptually) - Smart quantization with Phase 1 simplicity
   - Smart defaults: 4/4→16, 3/4→6, 6/8→6, 2/4→16
   - Optional --quantize override for triplets/complex rhythms
   - Non-polyphonic approach (50% occupancy, conflict resolution)
4. **Phase 1.8**: ✅ COMPLETE - Polyphonic conversion with integer durations
   - Time-based positioning using timeSeconds (more accurate)
   - Integer durations only (1, 2, 3) - simpler than quarter-precision
   - Preserves ALL simultaneous notes (polyphony)
   - Minimum duration = 1 (never drops notes)
   - Compact rest notation [~@16]
5. **Phase 1.9**: ⏳ IN PROGRESS - Polyphonic toggle with --no-polyphony flag
   - Combines Phase 1.7 and Phase 1.8 approaches
   - Default: Polyphonic mode (Phase 1.8)
   - With --no-polyphony: Non-polyphonic mode (Phase 1.7 style with Phase 1.8 improvements)
   - Let users choose based on their MIDI file
6. **Phase 2**: ⏳ NEXT - Multi-track output (waiting for Phase 1.9 completion)
7. **Validate output**: Manually test in Strudel REPL after each phase

**Current Status**: Phase 1.8 complete, Phase 1.9 specification complete, ready for implementation.

## Success Metrics

### Phase 1 Success ✅
- ✅ Converts single-track MIDI to valid Strudel code
- ✅ Generated code plays in Strudel REPL
- ✅ Note names are correct
- ✅ Musical output sounds good (simple, clean patterns)

### Phase 1.5 Success ❌ (Musical) - REJECTED
- ✅ All simultaneous notes preserved (no polyphony loss)
- ✅ 8-grid auto-calculated from time signature
- ✅ Per-note durations with quarter-precision
- ✅ interstellar.mid outputs all 5 notes
- ✅ All tests passing
- ❌ Musical quality worse than Phase 1 for some files (patterns sound choppy)
- ❌ Polyphonic output harder to customize in Strudel
- ❌ Per-note duration overhead degraded musical output

### Phase 1.7 Success ✅ (Conceptually)
- ✅ Smart quantization defaults: 4/4→16, 3/4→6, 6/8→6, 2/4→16
- ✅ --quantize override working
- ✅ Non-polyphonic approach (simpler for most use cases)
- ✅ Enhanced metadata showing quantization and grid meaning

### Phase 1.8 Success ✅
- ✅ Polyphonic preservation with integer durations
- ✅ Time-based positioning (timeSeconds) - more accurate than tick division
- ✅ Integer durations only (no decimals) - simpler than quarter-precision
- ✅ Minimum duration = 1 (never drops notes)
- ✅ Compact rest notation [~@16]
- ✅ All tests passing (11 comprehensive tests)
- ✅ Real MIDI files convert successfully (azul.mid, interstellar.mid)
- ⚠️ Mixed results: Some files sound great (interstellar.mid), others less so (in_blue.mid)

### Phase 1.9 Success 🔜
- 🔜 --no-polyphony flag working in command line
- 🔜 Default behavior is polyphonic (Phase 1.8)
- 🔜 --no-polyphony uses non-polyphonic algorithm (Phase 1.7 style with Phase 1.8 improvements)
- 🔜 Both modes share time-based positioning and integer durations
- 🔜 Both modes tested with real MIDI files (azul.mid, interstellar.mid, in_blue.mid)
- 🔜 Metadata shows current mode (Polyphonic/Non-polyphonic)
- 🔜 All tests passing
- 🔜 Users can choose best mode for their specific MIDI file
- 🔜 --quantize override working
- 🔜 Enhanced metadata showing quantization and grid meaning
- 🔜 Musical output quality as good as or better than Phase 1
### Phase 2 Success 🔜
- 🔜 All tracks converted (not just first track)
- 🔜 Each track outputs as separate pattern (track0, track1, etc.)
- 🔜 Simple stack() call generated
- 🔜 Empty tracks skipped automatically

---
7: Single-track conversion with smart quantization
shell:>convert --input simple-melody.mid --output my-melody.txt

# With tempo override
shell:>convert --input simple-melody.mid --tempo 120

# Auto-generated output file (azul.mid → azul.txt) with smart default
shell:>convert --input azul.mid

# Override quantization for triplets
shell:>convert --input jazz-triplets.mid --quantize 12

# Override for very detailed rhythms
shell:>convert --input complex.mid --quantize 32

# Convert specific track with custom quantization
shell:>convert --input multi-track.mid --track 2 --quantize 24d --output my-melody.txt

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
