package com.marcoalmeida.midi_tokenizer.strudel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GMInstrumentMapper.
 */
class GMInstrumentMapperTest {

    @Test
    void testPianoMapping() {
        // Piano family (0-7) should use high-quality samples
        assertEquals("piano", GMInstrumentMapper.map(0, 0), "Acoustic Grand Piano");
        assertEquals("steinway", GMInstrumentMapper.map(1, 0), "Bright Acoustic Piano");
        assertEquals("fmpiano", GMInstrumentMapper.map(2, 0), "Electric Grand Piano");
        assertEquals("gm_piano", GMInstrumentMapper.map(3, 0), "Honky-tonk Piano");
    }

    @Test
    void testGuitarMapping() {
        assertEquals("gm_acoustic_guitar_nylon", GMInstrumentMapper.map(24, 0));
        assertEquals("gm_acoustic_guitar_steel", GMInstrumentMapper.map(25, 0));
        assertEquals("gm_electric_guitar_clean", GMInstrumentMapper.map(27, 0));
    }

    @Test
    void testBassMapping() {
        assertEquals("gm_acoustic_bass", GMInstrumentMapper.map(32, 0));
        assertEquals("gm_electric_bass_finger", GMInstrumentMapper.map(33, 0));
        assertEquals("gm_fretless_bass", GMInstrumentMapper.map(35, 0));
    }

    @Test
    void testStringMapping() {
        assertEquals("gm_violin", GMInstrumentMapper.map(40, 0));
        assertEquals("gm_viola", GMInstrumentMapper.map(41, 0));
        assertEquals("gm_cello", GMInstrumentMapper.map(42, 0));
        assertEquals("gm_string_ensemble_1", GMInstrumentMapper.map(48, 0));
    }

    @Test
    void testBrassMapping() {
        assertEquals("gm_trumpet", GMInstrumentMapper.map(56, 0));
        assertEquals("gm_trombone", GMInstrumentMapper.map(57, 0));
        assertEquals("gm_french_horn", GMInstrumentMapper.map(60, 0));
    }

    @Test
    void testReedMapping() {
        assertEquals("gm_soprano_sax", GMInstrumentMapper.map(64, 0));
        assertEquals("gm_alto_sax", GMInstrumentMapper.map(65, 0));
        assertEquals("gm_tenor_sax", GMInstrumentMapper.map(66, 0));
        assertEquals("gm_clarinet", GMInstrumentMapper.map(71, 0));
    }

    @Test
    void testPipeMapping() {
        assertEquals("gm_flute", GMInstrumentMapper.map(73, 0));
        assertEquals("recorder_alto_sus", GMInstrumentMapper.map(74, 0));
        assertEquals("ocarina", GMInstrumentMapper.map(79, 0));
    }

    @Test
    void testSynthLeadMapping() {
        assertEquals("gm_lead_1_square", GMInstrumentMapper.map(80, 0));
        assertEquals("gm_lead_3_calliope", GMInstrumentMapper.map(82, 0));
    }

    @Test
    void testPercussiveMapping() {
        assertEquals("agogo", GMInstrumentMapper.map(113, 0));
        assertEquals("kalimba", GMInstrumentMapper.map(108, 0));
    }

    @Test
    void testDrumChannel() {
        // Channel 9 (MIDI channel 10) is drums
        assertEquals("piano", GMInstrumentMapper.map(0, 9), "Drums on channel 9 should default to piano");
        assertEquals("piano", GMInstrumentMapper.map(35, 9), "Drums on channel 9 should default to piano");
    }

    @Test
    void testUnmappedProgramsFallback() {
        // Unmapped programs should fall back to category or generic piano
        // Test a program that's not explicitly mapped (e.g., program 128 would be out of range)
        // But within 0-127, all should have some mapping
        
        // Piano category fallback (0-7)
        String pianoInstrument = GMInstrumentMapper.map(4, 0);
        assertTrue(pianoInstrument.equals("gm_epiano1"), "Program 4 should map to epiano1");
        
        // Bass category fallback (32-39)
        String bassInstrument = GMInstrumentMapper.map(38, 0);
        assertEquals("gm_synth_bass_1", bassInstrument, "Program 38 should map to synth_bass_1");
    }

    @Test
    void testNonDrumChannels() {
        // Non-drum channels should use GM program mapping
        assertEquals("piano", GMInstrumentMapper.map(0, 0), "Program 0 on channel 0");
        assertEquals("piano", GMInstrumentMapper.map(0, 1), "Program 0 on channel 1");
        assertEquals("piano", GMInstrumentMapper.map(0, 8), "Program 0 on channel 8");
        // Channel 9 is drums
        assertEquals("piano", GMInstrumentMapper.map(0, 9), "Program 0 on channel 9 (drums)");
        assertEquals("piano", GMInstrumentMapper.map(0, 10), "Program 0 on channel 10");
    }
}
