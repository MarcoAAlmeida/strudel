/*
superdoughdata.mjs - Data needed for running superdough (defaults, mappings, etc.)
Copyright (C) 2025 Strudel contributors - see <https://codeberg.org/uzu/strudel/src/branch/main/packages/superdough/superdoughdata.mjs>
This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details. You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

// Mapping from control name to webaudio node and parameter
const CONTROL_TARGETS = {
  stretch: { node: 'stretch', param: 'pitchFactor' },
  gain: { node: 'gain', param: 'gain' },
  postgain: { node: 'post', param: 'gain' },
  pan: { node: 'pan', param: 'pan' },
  tremolo: { node: 'tremolo', param: 'rate' },
  tremolosync: { node: 'tremolo', param: 'sync' },
  tremolodepth: { node: 'tremolo_gain', param: 'gain' },
  tremoloskew: { node: 'tremolo', param: 'skew' },
  tremolophase: { node: 'tremolo', param: 'phase' },
  tremoloshape: { node: 'tremolo', param: 'shape' },

  // MODULATORS
  lfo: { node: 'lfo', param: 'frequency' },
  env: { node: 'env', param: 'depth' },
  bmod: { node: 'bmod', param: 'depth' },

  // LPF
  cutoff: { node: 'lpf', param: 'frequency' },
  resonance: { node: 'lpf', param: 'Q' },
  lprate: { node: 'lpf_lfo', param: 'rate' },
  lpsync: { node: 'lpf_lfo', param: 'sync' },
  lpdepth: { node: 'lpf_lfo', param: 'depth' },
  lpdepthfrequency: { node: 'lpf_lfo', param: 'depth' },
  lpshape: { node: 'lpf_lfo', param: 'shape' },
  lpdc: { node: 'lpf_lfo', param: 'dcoffset' },
  lpskew: { node: 'lpf_lfo', param: 'skew' },

  // HPF
  hcutoff: { node: 'hpf', param: 'frequency' },
  hresonance: { node: 'hpf', param: 'Q' },
  hprate: { node: 'hpf_lfo', param: 'rate' },
  hpsync: { node: 'hpf_lfo', param: 'sync' },
  hpdepth: { node: 'hpf_lfo', param: 'depth' },
  hpdepthfrequency: { node: 'hpf_lfo', param: 'depth' },
  hpshape: { node: 'hpf_lfo', param: 'shape' },
  hpdc: { node: 'hpf_lfo', param: 'dcoffset' },
  hpskew: { node: 'hpf_lfo', param: 'skew' },

  // BPF
  bandf: { node: 'bpf', param: 'frequency' },
  bandq: { node: 'bpf', param: 'Q' },
  bprate: { node: 'bpf_lfo', param: 'rate' },
  bpsync: { node: 'bpf_lfo', param: 'sync' },
  bpdepth: { node: 'bpf_lfo', param: 'depth' },
  bpdepthfrequency: { node: 'bpf_lfo', param: 'depth' },
  bpshape: { node: 'bpf_lfo', param: 'shape' },
  bpdc: { node: 'bpf_lfo', param: 'dcoffset' },
  bpskew: { node: 'bpf_lfo', param: 'skew' },

  vowel: { node: 'vowel', param: 'frequency' },

  // DISTORTION
  coarse: { node: 'coarse', param: 'coarse' },
  crush: { node: 'crush', param: 'crush' },
  shape: { node: 'shape', param: 'shape' },
  shapevol: { node: 'shape', param: 'postgain' },
  distort: { node: 'distort', param: 'distort' },
  distortvol: { node: 'distort', param: 'postgain' },

  // COMPRESSOR
  compressor: { node: 'compressor', param: 'threshold' },
  compressorRatio: { node: 'compressor', param: 'ratio' },
  compressorKnee: { node: 'compressor', param: 'knee' },
  compressorAttack: { node: 'compressor', param: 'attack' },
  compressorRelease: { node: 'compressor', param: 'release' },

  // PHASER
  phaserrate: { node: 'phaser', param: 'rate' },
  phaserdepth: { node: 'phaser', param: 'depth' },
  phasersweep: { node: 'phaser', param: 'sweep' },
  phasercenter: { node: 'phaser', param: 'frequency' },

  // ORBIT EFFECTS
  delaytime: { node: 'delay', param: 'delayTime' },
  delayfeedback: { node: 'delay', param: 'feedback' },
  delaysync: { node: 'delay', param: 'sync' },
  dry: { node: 'dry', param: 'gain' },
  room: { node: 'room', param: 'wet' },
  roomfade: { node: 'room', param: 'fade' },
  roomlp: { node: 'room', param: 'lp' },
  djf: { node: 'djf', param: 'value' },
  busgain: { node: 'bus', param: 'gain' },

  // SYNTHS
  s: { node: 'source', param: 'frequency' },
  detune: { node: 'source', param: 'detune' },
  wt: { node: 'source', param: 'position' },
  warp: { node: 'source', param: 'warp' },
  freq: { node: 'source', param: 'frequency' },
  note: { node: 'source', param: 'frequency' },
};

export function getSuperdoughControlTargets() {
  return CONTROL_TARGETS;
}
