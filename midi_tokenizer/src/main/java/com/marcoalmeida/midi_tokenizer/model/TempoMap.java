package com.marcoalmeida.midi_tokenizer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a tempo map for converting ticks to time.
 */
public class TempoMap {
    private List<TempoChange> changes;

    public TempoMap() {
        this.changes = new ArrayList<>();
    }

    public TempoMap(List<TempoChange> changes) {
        this.changes = changes;
    }

    public List<TempoChange> getChanges() {
        return changes;
    }

    public void setChanges(List<TempoChange> changes) {
        this.changes = changes;
    }

    public void addChange(long tick, int microsecondsPerQuarterNote) {
        changes.add(new TempoChange(tick, microsecondsPerQuarterNote));
    }

    /**
     * Convert ticks to seconds using the tempo map.
     */
    public double ticksToSeconds(long ticks, int division) {
        if (changes.isEmpty()) {
            // Default tempo: 120 BPM = 500000 microseconds per quarter note
            return (ticks * 500000.0) / (division * 1000000.0);
        }

        double time = 0.0;
        long currentTick = 0;
        int currentTempo = 500000; // Default tempo

        for (TempoChange change : changes) {
            if (ticks <= change.getTick()) {
                break;
            }
            long tickDiff = change.getTick() - currentTick;
            time += (tickDiff * currentTempo) / (division * 1000000.0);
            currentTick = change.getTick();
            currentTempo = change.getMicrosecondsPerQuarterNote();
        }

        long remainingTicks = ticks - currentTick;
        time += (remainingTicks * currentTempo) / (division * 1000000.0);

        return time;
    }

    public static class TempoChange {
        private long tick;
        private int microsecondsPerQuarterNote;

        public TempoChange() {
        }

        public TempoChange(long tick, int microsecondsPerQuarterNote) {
            this.tick = tick;
            this.microsecondsPerQuarterNote = microsecondsPerQuarterNote;
        }

        public long getTick() {
            return tick;
        }

        public void setTick(long tick) {
            this.tick = tick;
        }

        public int getMicrosecondsPerQuarterNote() {
            return microsecondsPerQuarterNote;
        }

        public void setMicrosecondsPerQuarterNote(int microsecondsPerQuarterNote) {
            this.microsecondsPerQuarterNote = microsecondsPerQuarterNote;
        }
    }
}
