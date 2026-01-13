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

# Convert with options
shell:>convert --input input.mid --output output.txt --tempo 120 --quantize 8

# Auto-generate output filename (input.mid -> input.txt)
shell:>convert --input input.mid

# Convert specific track with finer quantization
shell:>convert --input song.mid --track 1 --quantize 32
```

### Command Options

- `--input` (required) - Path to MIDI file (.mid, .midi) or JSON file (.json)
- `--output` (optional) - Output file path (auto-generated from input filename if omitted)
- `--tempo` (optional) - Override tempo/BPM
- `--track` (optional) - Convert specific track only by index (default: 0)
- `--quantize` (required) - Quantization level (slices per measure in 4/4): any integer value works
  - Common values: 8 (eighth notes), 12 (triplet eighths), 16 (sixteenth notes), 24 (triplet sixteenths), 32 (thirty-second notes)
  - Default: 16

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

**Input**: MIDI file with single or multiple melody tracks

**Output Example**:
```javascript
/* "azul" */
/**
Source: azul.mid
Tempo: 160 BPM  
Time Signature: 4/4
Track: 1
Quantization: 8th notes
**/

setcpm(40)

let pattern = note(`<[~@8] [f6 ~ d6 ~ g#5 ~ g5 f5] [f5 g5 ~ a5 f5@2 d5 ~]>`).room(0.2)

pattern
```

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

**Known Gaps**:
- ⚠️ Missing `--verbose` flag for debugging output
- ⚠️ No pattern simplification (collapsing `[~@8] [~@8]` → longer rests)
- ⚠️ Static utility classes instead of injectable services (testing limitation)

---

### Phase 2: Multi-Track Support with Arrangement

**Goal**: Convert multiple MIDI tracks and use `arrange()` for structure.

**⚠️ Quantization Considerations**: 
- All tracks must use **same quantization level** to maintain synchronization
- Global time grid prevents timing drift between tracks
- Different rhythmic densities may require different quantization per track (drums=16, melody=8)
- Need to preserve measure alignment across all tracks

**Features**:
- Process all tracks in MIDI file simultaneously on shared time grid
- Generate named patterns per track
- Use `arrange()` and `stack()` for playback structure
- Handle track names and instruments
- Maintain synchronization via global quantization grid

**Input**: Multi-track MIDI (drums, bass, melody)

**Output Example**:
```javascript
/* "Full Song" */
/**
Source: full-song.mid
Tempo: 100 BPM
Tracks: 3
  - Track 0: Drums
  - Track 1: Bass
  - Track 2: Lead
Converted: 2024-01-15
**/

setcpm(100/4)

// Track 0: Drums
let drums = s("bd sd bd sd").bank("RolandTR808")

// Track 1: Bass
let bass = note("c2 ~ e2 g2").sound("sawtooth").lpf(800)

// Track 2: Lead  
let lead = note("c4 e4 g4 b4").sound("triangle")

arrange(
  [1, drums],
  [4, stack(drums, bass)],
  [4, stack(drums, bass, lead)],
  [2, drums]
).room(0.2)
```

**Success Criteria**:
- Extract all tracks with correct instrument mapping
- Generate unique, meaningful variable names per track
- Create logical arrangement structure
- Handle track muting/volume

**Implementation Steps**:
1. **Extend RhythmConverter** to handle multiple tracks on same global grid
2. Implement track iteration in `StrudelConverter` (already has single-track support)
3. Create MIDI program → Strudel sound mapping system in `InstrumentMapper`
4. Build pattern variable naming strategy in `PatternNamer` (camelCase, track names)
5. Implement `ArrangementGenerator` for `arrange()` and `stack()` block generation
6. Add track metadata extraction and formatting
7. **Handle quantization conflicts** when multiple tracks have notes at same grid position

**Quantization-Specific Tasks**:
- Ensure all tracks quantized to same global grid (shared measure boundaries)
- Detect when different tracks need different quantization levels
- Option to use highest quantization needed across all tracks
- Test with multi-track MIDI to verify synchronization

**Files to Create**:
- `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/InstrumentMapper.java` - MIDI program to sound mapping
- `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/PatternNamer.java` - Variable naming from track names
- `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/ArrangementGenerator.java` - Arrangement block generation
- `src/test/java/com/marcoalmeida/midi_tokenizer/strudel/ArrangementGeneratorTest.java` - Tests
- `src/test/java/com/marcoalmeida/midi_tokenizer/strudel/MultiTrackTest.java` - Integration tests

---

### Phase 3: Rhythm and Timing Enhancement

**Goal**: Improve rhythm representation within quantization constraints.

**⚠️ Quantization Considerations**:

The quantization approach handles most rhythms but has some limitations:
- ✅ **Triplets**: Fully supported with appropriate quantization (12, 24, etc.)
- ⚠️ **Swing timing**: Can be approximated with fine quantization or use Strudel's `.swing()` modifier
- ⚠️ **Polyrhythm**: Use LCM quantization (e.g., 20 for 5-against-4) or combine patterns
- ❌ **Micro-timing variations**: All events snap to grid - humanization lost
- ⚠️ **Complex syncopation**: Grid approximation - may need manual adjustment

**Features** (achievable with quantization):
- ✅ Handle rests (already working via `~`)
- ✅ Triplets with appropriate quantization (12, 24, etc.)
- ✅ Basic syncopation (as long as it aligns with grid)
- ✅ Tempo changes (multiple `setcpm()` calls)
- ✅ Different time signatures per section
- ✅ Pattern repetition detection (`*2`, `*4` notation)
- ⚠️ **Triplet detection**: Auto-detect and suggest optimal quantization
- ⚠️ **Swing detection**: Detect swing ratio and suggest `.swing()` modifier or quantization adjustment

**Hybrid Approach** (recommended):
1. Use quantization for simple, grid-aligned material
2. **Detect complex rhythms** (triplets, swing) via pattern analysis
3. Add **Strudel modifiers** in comments: `.fast(3/2)` for triplets, `.swing()` for swing
4. Warn user when MIDI timing doesn't fit quantization grid

**Input**: MIDI with rhythms that align to quantization grid

**Output Example**:
```javascript
/* "Son Clave" */
/**
Source: son-clave.mid
Tempo: 100 BPM
Time Signature: 4/4
Style: Afro-Cuban
[18 Rhythms you should know](https://www.youtube.com/watch?v=ZROR_E5bFEI)
**/

setcpm(100/4)

// 3-2 Son Clave pattern
let son_clave_3_by_2 = s("[bd@1.5 bd@1.5 bd] [~ bd bd ~]").bank("tr909")

// 2-3 Son Clave pattern (reversed)
let son_clave_2_by_3 = s("[~ bd bd ~] [bd@1.5 bd@1.5 bd]").bank("tr909")

// Tresillo rhythm
let tresillo = s("[bd@1.5 bd@1.5 bd]*2").bank("rolandtr909")

arrange(
  [4, son_clave_3_by_2],
  [4, son_clave_2_by_3],
  [4, tresillo]
).room(0.02)
```

**Success Criteria**:
- Accurately represent note durations using `@` notation
- Handle rests with `~` 
- Detect and preserve rhythmic patterns
- Support polymeter and polyrhythm with brackets `[]` and grouping

**Implementation Steps**:
1. **Extend RhythmConverter** with pattern repetition detection (`*2`, `*4`)
2. Create `RhythmPatternDetector` for triplet/swing detection
3. Implement tempo change support with multiple `setcpm()` calls in template
4. Add `--verbose` flag for debugging

**Files to Create**:
- `src/main/java/com/marcoalmeida/midi_tokenizer/strudel/RhythmPatternDetector.java` - Detect triplets, swing, polyrhythm
- `src/test/java/com/marcoalmeida/midi_tokenizer/strudel/RhythmPatternDetectorTest.java` - Tests



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
│   │       └── strudel/                           # NEW: Strudel conversion package
│   │           ├── StrudelConverter.java          # Main conversion service (Phase 1)
│   │           ├── NoteConverter.java             # MIDI note to note name (Phase 1)
│   │           ├── StrudelTemplate.java           # File template rendering (Phase 1)
│   │           ├── TrackProcessor.java            # Track separation (Phase 2)
│   │           ├── InstrumentMapper.java          # MIDI program mapping (Phase 2)
│   │           ├── PatternGenerator.java          # Pattern generation (Phase 2)
│   │           ├── ArrangementGenerator.java      # Arrangement blocks (Phase 2)
│   │           ├── RhythmAnalyzer.java            # Rhythm analysis (Phase 3)
│   │           └── DurationCalculator.java        # Tick to duration (Phase 3)
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
[Select Track] - ✅ IMPLEMENTED (via ConversionOptions)
  ↓
NoteConverter - ✅ IMPLEMENTED (MIDI note → "c4")
  ↓
RhythmConverter - ✅ IMPLEMENTED (Quantization Grid Engine)
  ├─ Calculate grid (8/16/32 slices per measure)
  ├─ Map notes to slices (50% occupancy rule)
  ├─ Resolve conflicts (longest note wins)
  ├─ Merge durations (@N notation)
  └─ Insert rests (~)
  ↓
ArrangementGenerator → Multi-track arrange() - ⏳ PHASE 2
  ↓
StrudelTemplate - ✅ IMPLEMENTED (File template rendering)
  ↓
Strudel Pattern File (.txt) - ✅ OUTPUT
```

**Current Flow (Phase 1)**:
```
MIDI → Parser → Converter → Select Track → Extract Notes → 
RhythmConverter (Quantization) → StrudelTemplate → .txt File
```

### Key Classes and Methods

```java
package com.marcoalmeida.midi_tokenizer.strudel;

/**
 * Utility class for converting MIDI note numbers to Strudel note names.
 */
public class NoteConverter {
    
    /**
     * Convert MIDI note number to Strudel note name
     * @param noteNumber MIDI note number (0-127)
     * @return Note name like "c4", "d#5"
     * 
     * Example:
     * midiNoteToNoteName(60) → "c4"
     * midiNoteToNoteName(61) → "c#4"
     */
    public static String midiNoteToNoteName(int noteNumber) {
        String[] notes = {"c", "c#", "d", "d#", "e", "f", "f#", "g", "g#", "a", "a#", "b"};
        int octave = (noteNumber / 12) - 1;
        String note = notes[noteNumber % 12];
        return note + octave;
    }
}

/**
 * ACTUAL IMPLEMENTATION: Static utility for quantization-based rhythm conversion.
 */
public class RhythmConverter {
    
    /**
     * Convert note events to quantized Strudel pattern.
     * Quantizes notes to a fixed grid (8th, 16th, 32nd notes).
     * 
     * @param noteEvents List of note events from a track
     * @param division MIDI ticks per quarter note
     * @param timeSignatureNumerator Time signature numerator (e.g., 4 for 4/4)
     * @param timeSignatureDenominator Time signature denominator (e.g., 4 for 4/4)
     * @param quantization Quantization level (8=eighth, 16=sixteenth, 32=thirty-second)
     * @return Quantized pattern string with [measure] grouping
     * 
     * Example:
     * toQuantizedCyclePattern(events, 480, 4, 4, 16) → 
     *   "[c4@4 d4@2 e4@2] [f4@8] [~@8]"
     * 
     * How it works:
     * 1. Divides each measure into N slices (N = quantization * timeSig / 4)
     * 2. Maps each MIDI note to grid slices using 50% occupancy rule
     * 3. Merges consecutive identical notes with @N notation
     * 4. Wraps each measure in [...]
     */
    public static String toQuantizedCyclePattern(
        List<EventOutput> noteEvents,
        int division,
        int timeSignatureNumerator,
        int timeSignatureDenominator,
        int quantization
    ) {
        // See actual implementation in RhythmConverter.java
        return null;
    }
}

/**
 * Service for generating Strudel patterns from track data.
 */
public class PatternGenerator {
    
    /**
     * Generate Strudel pattern from track data
     * @param track Track data from MidiOutput
     * @param options Conversion options
     * @return Strudel pattern code
     */
    public String generatePattern(TrackOutput track, ConversionOptions options) {
        // Create Strudel pattern from track data
        return null;
    }
}

/**
 * Template renderer for complete Strudel pattern files.
 */
public class StrudelTemplate {
    
    /**
     * Render complete pattern file using template
     * @param metadata Song metadata
     * @param patterns Pattern definitions
     * @param arrangement Arrangement structure
     * @return Complete .txt file content
     */
    public String renderTemplate(Metadata metadata, List<String> patterns, String arrangement) {
        // Generate complete .txt file following 00_SonClave.txt structure
        return null;
    }
}

/**
 * Main conversion service coordinating all converters.
 */
@Service
public class StrudelConverter {
    
    private final MidiParser midiParser;
    private final NoteConverter noteConverter;
    private final RhythmAnalyzer rhythmAnalyzer;
    private final PatternGenerator patternGenerator;
    private final StrudelTemplate template;
    
    /**
     * Convert MIDI file or JSON to Strudel pattern
     * @param inputPath Path to .mid or .json file
     * @param options Conversion options
     * @return Strudel pattern text
     */
    public String convert(String inputPath, ConversionOptions options) throws Exception {
        // Main conversion pipeline
        MidiOutput midiData;
        
        if (inputPath.endsWith(".mid")) {
            midiData = midiParser.parse(inputPath, options.isUseSeconds());
        } else if (inputPath.endsWith(".json")) {
            // Parse JSON file
            ObjectMapper mapper = new ObjectMapper();
            midiData = mapper.readValue(new File(inputPath), MidiOutput.class);
        } else {
            throw new IllegalArgumentException("Input must be .mid or .json file");
        }
        
        // Process tracks and generate patterns
        List<String> patterns = new ArrayList<>();
        for (TrackOutput track : midiData.tracks()) {
            String pattern = patternGenerator.generatePattern(track, options);
            patterns.add(pattern);
        }
        
        // Generate arrangement
        String arrangement = generateArrangement(patterns);
        
        // Render template
        return template.renderTemplate(midiData.metadata(), patterns, arrangement);
    }
}

/**
 * ACTUAL IMPLEMENTATION: Options for Strudel conversion.
 */
public record ConversionOptions(
    Integer overrideTempo,    // Override tempo in BPM
    Integer trackIndex,        // Track to convert (default: 0)
    Integer quantization       // Quantization level: any integer (8, 12, 16, 24, 32, etc.) (default: 16)
) {
    // Provides defaults for optional parameters
    public int getEffectiveTrackIndex() {
        return trackIndex != null ? trackIndex : 0;
    }
    
    public int getEffectiveQuantization() {
        return quantization != null ? quantization : 16;
    }
    
    // Note: simplify and verbose flags NOT YET IMPLEMENTED
}
```

---

## Testing Strategy

### Unit Tests (JUnit 5)

Each class has corresponding test class:

```java
package com.marcoalmeida.midi_tokenizer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.marcoalmeida.midi_tokenizer.strudel.NoteConverter;

class NoteConverterTest {
    
    @Test
    void shouldConvertMiddleCCorrectly() {
        assertEquals("c4", NoteConverter.midiNoteToNoteName(60));
    }
    
    @Test
    void shouldHandleSharps() {
        assertEquals("c#4", NoteConverter.midiNoteToNoteName(61));
    }
    
    @Test
    void shouldHandleDifferentOctaves() {
        assertEquals("c5", NoteConverter.midiNoteToNoteName(72));
        assertEquals("c3", NoteConverter.midiNoteToNoteName(48));
    }
}
```

### Integration Tests

Test full conversion pipeline:

```java
package com.marcoalmeida.midi_tokenizer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.marcoalmeida.midi_tokenizer.strudel.StrudelConverter;
import com.marcoalmeida.midi_tokenizer.strudel.ConversionOptions;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StrudelConverterIntegrationTest {
    
    @Autowired
    private StrudelConverter converter;
    
    @Test
    void shouldConvertSimpleMelody() throws Exception {
        var options = new ConversionOptions(null, false, false, null);
        String output = converter.convert("test-fixtures/simple-melody.mid", options);
        
        assertTrue(output.contains("setcpm("));
        assertTrue(output.contains("note("));
        assertTrue(output.matches(".*[cdefgab]\\d.*")); // Contains note names
    }
    
    @Test
    void shouldHandleMultiTrack() throws Exception {
        var options = new ConversionOptions(null, false, false, null);
        String output = converter.convert("test-fixtures/multi-track.mid", options);
        
        assertTrue(output.contains("arrange("));
        assertTrue(output.contains("stack("));
    }
}
```

### Test Fixtures

Create sample MIDI files for each phase in `test-fixtures/`:

- **Phase 1**: `simple-melody.mid` - Single track, simple rhythm
- **Phase 2**: `multi-track.mid` - 3 tracks (drums, bass, melody)
- **Phase 3**: `son-clave.mid` - Complex Afro-Cuban rhythms

---

## Code Style Guidelines

### Follow Java and Spring Conventions

- Use standard Java naming conventions (PascalCase for classes, camelCase for methods)
- Use Java records for immutable data classes where appropriate
- Use `@Service`, `@Component` annotations for Spring beans
- Follow existing code style in `MidiParser` and `MidiShellCommands`
- Use JavaDoc for all public methods and classes
- Run `./gradlew test` before committing

### Pattern Generation Style

- Use Java text blocks (`"""`) for multi-line string templates
- Prefer declarative style (define patterns, then arrange)
- Use meaningful variable names (not `track1`, `pattern2`)
- Add comments explaining musical concepts
- Follow 00_SonClave.txt template structure

### Error Handling

```java
public String convert(String filepath, ConversionOptions options) throws Exception {
    if (!filepath.endsWith(".mid") && !filepath.endsWith(".json")) {
        throw new IllegalArgumentException("Input must be .mid or .json file");
    }
    
    // Validate MIDI data
    if (midiData.tracks() == null || midiData.tracks().isEmpty()) {
        throw new IllegalStateException("No tracks found in MIDI file");
    }
    
    // Handle conversion errors
    try {
        return template.renderTemplate(metadata, patterns, arrangement);
    } catch (Exception e) {
        throw new RuntimeException("Failed to generate Strudel pattern: " + e.getMessage(), e);
    }
}
```

### Spring Shell Command Example

Add to `MidiShellCommands.java`:

```java
@ShellMethod(key = "convert", value = "Convert MIDI file to Strudel pattern")
public String convert(
    @ShellOption(help = "Path to MIDI or JSON file") String input,
    @ShellOption(defaultValue = ShellOption.NULL, help = "Output file path") String output,
    @ShellOption(defaultValue = ShellOption.NULL, help = "Override tempo/BPM") Integer tempo,
    @ShellOption(defaultValue = "false", help = "Simplify pattern complexity") boolean simplify,
    @ShellOption(defaultValue = "false", help = "Show detailed conversion info") boolean verbose,
    @ShellOption(defaultValue = ShellOption.NULL, help = "Convert specific track only") Integer track
) {
    try {
        var options = new ConversionOptions(tempo, simplify, verbose, track);
        String result = strudelConverter.convert(input, options);
        
        if (output != null) {
            Files.writeString(Path.of(output), result);
            return "Successfully wrote output to: " + output;
        } else {
            return result;
        }
    } catch (Exception e) {
        return "Error: " + e.getMessage();
    }
}
```

---

## Phase Implementation Order

1. **Start with Phase 1** - Get basic conversion working with single track
2. **Validate output** - Manually test in Strudel REPL to ensure patterns play correctly
3. **Add Phase 2** - Multi-track support for complete songs
4. **Move to Phase 3** - Rhythm is critical for musical accuracy

This order prioritizes core functionality before advanced features.

---

## Success Metrics

### Phase 1 Success
- ✅ Converts single-track MIDI to valid Strudel code
- ✅ Generated code plays in Strudel REPL
- ✅ Note names are correct

### Phase 2 Success
- ✅ Handles multi-track MIDI files
- ✅ Generates proper `arrange()` blocks
- ✅ Instrument mapping sounds musical

### Phase 3 Success
- ✅ Preserves complex rhythms accurately
- ✅ Handles rests and syncopation
- ✅ Son Clave pattern converts correctly

---

## Example Command Usage

```bash
# First, run the Spring Shell application
./gradlew bootJar && java -jar build/libs/midi-tokenizer.jar

# Then in the shell:

# Phase 1: Basic conversion (default 16th note quantization)
shell:>convert --input simple-melody.mid --output my-melody.txt

# With coarser quantization (8th notes)
shell:>convert --input simple-melody.mid --quantize 8

# For triplet-based music (12 = triplet eighths)
shell:>convert --input jazz-melody.mid --quantize 12

# For mixed straight and triplet (24 = triplet sixteenths)
shell:>convert --input afro-cuban.mid --quantize 24

# With finer quantization (32nd notes)
shell:>convert --input complex-rhythm.mid --quantize 32

# Phase 2: Multi-track with specific tempo and track
shell:>convert --input full-song.mid --track 1 --tempo 100 --quantize 16

# Auto-generated output file (azul.mid → azul.txt)
shell:>convert --input azul.mid --track 1 --quantize 8

# Phase 3: Complex rhythm (use finer quantization)
shell:>convert --input son-clave.mid --quantize 32
```

---

## Future Enhancements

Beyond Phase 3:

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

2. **Add quantization quality metrics**:
   - Grid fitness score (0-1, how well MIDI aligns)
   - Suggested quantization level (8/16/32)
   - Warning count (notes >20% off grid)

3. **Document known limitations** in output:
   ```javascript
   /**
   Source: jazz-swing.mid
   \u26a0\ufe0f Quantization warnings:
     - Detected swing ratio: 66/33 (triplet feel)
     - 23 notes >20% off 16th note grid
     - Suggestion: Add .swing() modifier for swing feel
   **/
   ```

---

**Remember**: Start simple, test often, and make it work before making it perfect!

The quantization approach is **good enough for most music** and **works now**. Perfect accuracy can come later through hybrid approach if needed.
