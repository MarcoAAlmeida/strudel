package com.marcoalmeida.midi_tokenizer.strudel;

import java.util.Map;

/**
 * Maps MIDI General MIDI program numbers (0-127) to Strudel instruments.
 * Prefers high-quality samples like piano, fmpiano, steinway over gm_* instruments.
 * 
 * @see <a href="https://en.wikipedia.org/wiki/General_MIDI">General MIDI Specification</a>
 */
public class GMInstrumentMapper {
    
    // Direct GM program to Strudel instrument mapping
    private static final Map<Integer, String> GM_TO_STRUDEL = Map.ofEntries(
        // Piano (0-7) - use high-quality samples
        Map.entry(0, "piano"),      // Acoustic Grand Piano → best piano sample
        Map.entry(1, "steinway"),   // Bright Acoustic Piano → Steinway sample
        Map.entry(2, "fmpiano"),    // Electric Grand Piano → FM Piano
        Map.entry(3, "gm_piano"),   // Honky-tonk Piano
        Map.entry(4, "gm_epiano1"), // Electric Piano 1
        Map.entry(5, "gm_epiano2"), // Electric Piano 2
        Map.entry(6, "gm_harpsichord"), 
        Map.entry(7, "gm_clavinet"),
        
        // Chromatic Percussion (8-15)
        Map.entry(8, "gm_celesta"),
        Map.entry(9, "gm_glockenspiel"),
        Map.entry(10, "gm_music_box"),
        Map.entry(11, "vibraphone"),  // Use sample, not gm
        Map.entry(12, "marimba"),     // Use sample, not gm
        Map.entry(13, "xylophone_medium_ff"),
        Map.entry(14, "gm_tubular_bells"),
        Map.entry(15, "gm_dulcimer"),
        
        // Organ (16-23)
        Map.entry(16, "gm_drawbar_organ"),
        Map.entry(17, "gm_percussive_organ"),
        Map.entry(18, "gm_rock_organ"),
        Map.entry(19, "gm_church_organ"),
        Map.entry(20, "gm_reed_organ"),
        Map.entry(21, "gm_accordion"),
        Map.entry(22, "harmonica"),  // Use sample
        Map.entry(23, "gm_bandoneon"),
        
        // Guitar (24-31)
        Map.entry(24, "gm_acoustic_guitar_nylon"),
        Map.entry(25, "gm_acoustic_guitar_steel"),
        Map.entry(26, "gm_electric_guitar_jazz"),
        Map.entry(27, "gm_electric_guitar_clean"),
        Map.entry(28, "gm_electric_guitar_muted"),
        Map.entry(29, "gm_overdriven_guitar"),
        Map.entry(30, "gm_distortion_guitar"),
        Map.entry(31, "gm_guitar_harmonics"),
        
        // Bass (32-39)
        Map.entry(32, "gm_acoustic_bass"),
        Map.entry(33, "gm_electric_bass_finger"),
        Map.entry(34, "gm_electric_bass_pick"),
        Map.entry(35, "gm_fretless_bass"),
        Map.entry(36, "gm_slap_bass_1"),
        Map.entry(37, "gm_slap_bass_2"),
        Map.entry(38, "gm_synth_bass_1"),
        Map.entry(39, "gm_synth_bass_2"),
        
        // Strings (40-47)
        Map.entry(40, "gm_violin"),
        Map.entry(41, "gm_viola"),
        Map.entry(42, "gm_cello"),
        Map.entry(43, "gm_contrabass"),
        Map.entry(44, "gm_tremolo_strings"),
        Map.entry(45, "gm_pizzicato_strings"),
        Map.entry(46, "gm_orchestral_harp"),
        Map.entry(47, "timpani"),  // Use sample
        
        // Ensemble (48-55)
        Map.entry(48, "gm_string_ensemble_1"),
        Map.entry(49, "gm_string_ensemble_2"),
        Map.entry(50, "gm_synth_strings_1"),
        Map.entry(51, "gm_synth_strings_2"),
        Map.entry(52, "gm_choir_aahs"),
        Map.entry(53, "gm_voice_oohs"),
        Map.entry(54, "gm_synth_choir"),
        Map.entry(55, "gm_orchestra_hit"),
        
        // Brass (56-63)
        Map.entry(56, "gm_trumpet"),
        Map.entry(57, "gm_trombone"),
        Map.entry(58, "gm_tuba"),
        Map.entry(59, "gm_muted_trumpet"),
        Map.entry(60, "gm_french_horn"),
        Map.entry(61, "gm_brass_section"),
        Map.entry(62, "gm_synth_brass_1"),
        Map.entry(63, "gm_synth_brass_2"),
        
        // Reed (64-71)
        Map.entry(64, "gm_soprano_sax"),
        Map.entry(65, "gm_alto_sax"),
        Map.entry(66, "gm_tenor_sax"),
        Map.entry(67, "gm_baritone_sax"),
        Map.entry(68, "gm_oboe"),
        Map.entry(69, "gm_english_horn"),
        Map.entry(70, "gm_bassoon"),
        Map.entry(71, "gm_clarinet"),
        
        // Pipe (72-79)
        Map.entry(72, "gm_piccolo"),
        Map.entry(73, "gm_flute"),
        Map.entry(74, "recorder_alto_sus"),  // Use sample
        Map.entry(75, "gm_pan_flute"),
        Map.entry(76, "gm_blown_bottle"),
        Map.entry(77, "gm_shakuhachi"),
        Map.entry(78, "gm_whistle"),
        Map.entry(79, "ocarina"),  // Use sample
        
        // Synth Lead (80-87)
        Map.entry(80, "gm_lead_1_square"),
        Map.entry(81, "gm_lead_2_sawtooth"),
        Map.entry(82, "gm_lead_3_calliope"),
        Map.entry(83, "gm_lead_4_chiff"),
        Map.entry(84, "gm_lead_5_charang"),
        Map.entry(85, "gm_lead_6_voice"),
        Map.entry(86, "gm_lead_7_fifths"),
        Map.entry(87, "gm_lead_8_bass_lead"),
        
        // Synth Pad (88-95)
        Map.entry(88, "gm_pad_new_age"),
        Map.entry(89, "gm_pad_warm"),
        Map.entry(90, "gm_pad_poly"),
        Map.entry(91, "gm_pad_choir"),
        Map.entry(92, "gm_pad_bowed"),
        Map.entry(93, "gm_pad_metallic"),
        Map.entry(94, "gm_pad_halo"),
        Map.entry(95, "gm_pad_sweep"),
        
        // Synth Effects (96-103)
        Map.entry(96, "gm_fx_rain"),
        Map.entry(97, "gm_fx_soundtrack"),
        Map.entry(98, "gm_fx_crystal"),
        Map.entry(99, "gm_fx_atmosphere"),
        Map.entry(100, "gm_fx_brightness"),
        Map.entry(101, "gm_fx_goblins"),
        Map.entry(102, "gm_fx_echoes"),
        Map.entry(103, "gm_fx_sci_fi"),
        
        // Ethnic (104-111)
        Map.entry(104, "gm_sitar"),
        Map.entry(105, "gm_banjo"),
        Map.entry(106, "gm_shamisen"),
        Map.entry(107, "gm_koto"),
        Map.entry(108, "kalimba"),  // Use sample
        Map.entry(109, "gm_bagpipe"),
        Map.entry(110, "gm_fiddle"),
        Map.entry(111, "gm_shanai"),
        
        // Percussive (112-119)
        Map.entry(112, "gm_tinkle_bell"),
        Map.entry(113, "agogo"),  // Use sample
        Map.entry(114, "gm_steel_drums"),
        Map.entry(115, "woodblock"),  // Use sample
        Map.entry(116, "gm_taiko_drum"),
        Map.entry(117, "gm_melodic_tom"),
        Map.entry(118, "gm_synth_drum"),
        Map.entry(119, "gm_reverse_cymbal"),
        
        // Sound Effects (120-127)
        Map.entry(120, "gm_guitar_fret_noise"),
        Map.entry(121, "gm_breath_noise"),
        Map.entry(122, "gm_seashore"),
        Map.entry(123, "gm_bird_tweet"),
        Map.entry(124, "gm_telephone"),
        Map.entry(125, "gm_helicopter"),
        Map.entry(126, "gm_applause"),
        Map.entry(127, "gm_gunshot")
    );
    
    /**
     * Map MIDI program number to Strudel instrument.
     * 
     * @param program GM program number (0-127)
     * @param channel MIDI channel (0-15, channel 9 is drums)
     * @return Strudel instrument name
     */
    public static String map(int program, int channel) {
        // Channel 9 (MIDI channels are 0-indexed, so channel 10 in MIDI spec = 9 in code)
        // is drums - for now, treat as melodic with piano
        // TODO: Map channel 9 to percussion sounds (bassdrum, snare, hihat, etc.)
        if (channel == 9) {
            return "piano";  // Safe default for drums
        }
        
        // Direct mapping if available
        if (GM_TO_STRUDEL.containsKey(program)) {
            return GM_TO_STRUDEL.get(program);
        }
        
        // Category-based fallback for unmapped programs
        if (program >= 0 && program <= 7) return "piano";
        if (program >= 32 && program <= 39) return "gm_acoustic_bass";
        if (program >= 40 && program <= 47) return "gm_string_ensemble_1";
        if (program >= 56 && program <= 63) return "gm_trumpet";
        if (program >= 64 && program <= 71) return "gm_alto_sax";
        if (program >= 72 && program <= 79) return "gm_flute";
        
        // Generic fallback
        return "piano";
    }
}
