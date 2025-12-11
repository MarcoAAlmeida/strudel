/*
superdoughdata.mjs - Data needed for running superdough (defaults, mappings, etc.)
Copyright (C) 2025 Strudel contributors - see <https://codeberg.org/uzu/strudel/src/branch/main/packages/superdough/superdoughdata.mjs>
This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details. You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

const CONTROL_DATA = {
  stretch: { param: 'stretch.pitchFactor', default: 1, min: -4, max: 100 },
  gain: { param: 'gain.gain', default: 0.8, min: 0, max: 10 },
  postgain: { param: 'post.gain', default: 1, min: 0, max: 10 },
  pan: { param: 'pan.pan', min: 0, max: 1 },
  tremolo: { param: 'tremolo.rate', min: 0, max: 40 },
  tremolosync: { param: 'tremolo.sync', min: 0, max: 8 },
  tremolodepth: { param: 'tremolo_gain.gain', default: 1, min: 0, max: 10 },
  tremoloskew: { param: 'tremolo.skew', min: 0, max: 1 },
  tremolophase: { param: 'tremolo.phase', default: 0, min: 0, max: 1 },
  tremoloshape: { param: 'tremolo.shape', min: 0, max: 4 },

  // LPF
  cutoff: { param: 'lpf.frequency', min: 20, max: 24000 },
  resonance: { param: 'lpf.Q', min: 0.1, max: 30 },
  lprate: { param: 'lpf_lfo.rate', min: 0, max: 40 },
  lpsync: { param: 'lpf_lfo.sync', min: 0, max: 8 },
  lpdepth: { param: 'lpf_lfo.depth', min: 20, max: 24000 },
  lpdepthfrequency: { param: 'lpf_lfo.depth', min: 20, max: 24000 },
  lpshape: { param: 'lpf_lfo.shape', min: 0, max: 4 },
  lpdc: { param: 'lpf_lfo.dcoffset', min: -1, max: 1 },
  lpskew: { param: 'lpf_lfo.skew', min: 0, max: 1 },

  // HPF
  hcutoff: { param: 'hpf.frequency', min: 20, max: 24000 },
  hresonance: { param: 'hpf.Q', min: 0.1, max: 30 },
  hprate: { param: 'hpf_lfo.rate', min: 0, max: 40 },
  hpsync: { param: 'hpf_lfo.sync', min: 0, max: 8 },
  hpdepth: { param: 'hpf_lfo.depth', min: 20, max: 24000 },
  hpdepthfrequency: { param: 'hpf_lfo.depth', min: 20, max: 24000 },
  hpshape: { param: 'hpf_lfo.shape', min: 0, max: 4 },
  hpdc: { param: 'hpf_lfo.dcoffset', min: -1, max: 1 },
  hpskew: { param: 'hpf_lfo.skew', min: 0, max: 1 },

  // BPF
  bandf: { param: 'bpf.frequency', min: 20, max: 24000 },
  bandq: { param: 'bpf.Q', min: 0.1, max: 30 },
  bprate: { param: 'bpf_lfo.rate', min: 0, max: 40 },
  bpsync: { param: 'bpf_lfo.sync', min: 0, max: 8 },
  bpdepth: { param: 'bpf_lfo.depth', min: 20, max: 24000 },
  bpdepthfrequency: { param: 'bpf_lfo.depth', min: 20, max: 24000 },
  bpshape: { param: 'bpf_lfo.shape', min: 0, max: 4 },
  bpdc: { param: 'bpf_lfo.dcoffset', min: -1, max: 1 },
  bpskew: { param: 'bpf_lfo.skew', min: 0, max: 1 },

  vowel: { param: 'vowel.frequency', min: 200, max: 4000 },

  // DISTORTION
  coarse: { param: 'coarse.coarse', min: 1, max: 64 },
  crush: { param: 'crush.crush', min: 1, max: 16 },
  shape: { param: 'shape.shape', min: -1, max: 0.999 },
  shapevol: { param: 'shape.postgain', default: 1, min: 0, max: 1 },
  distort: { param: 'distort.distort', min: 0, max: 5 },
  distortvol: { param: 'distort.postgain', default: 1, min: 0, max: 1 },

  // COMPRESSOR
  compressor: { param: 'compressor.threshold', default: -3, min: -100, max: 0 },
  compressorRatio: { param: 'compressor.ratio', default: 10, min: 1, max: 20 },
  compressorKnee: { param: 'compressor.knee', default: 10, min: 0, max: 40 },
  compressorAttack: { param: 'compressor.attack', default: 0.005, min: 0, max: 1 },
  compressorRelease: { param: 'compressor.release', default: 0.05, min: 0, max: 2 },

  // PHASER
  phaserrate: { param: 'phaser.rate', min: 0, max: 40 },
  phaserdepth: { param: 'phaser.depth', default: 0.75, min: 0, max: 1 },
  phasersweep: { param: 'phaser.sweep', min: 0, max: 5000 },
  phasercenter: { param: 'phaser.frequency', min: 20, max: 24000 },

  // ORBIT EFFECTS
  delaytime: { param: 'delay.delayTime', min: 0, max: 4 },
  delayfeedback: { param: 'delay.feedback', default: 0.5, min: 0, max: 0.98 },
  delaysync: { param: 'delay.sync', default: 3 / 16, min: 0, max: 2 },
  dry: { param: 'dry.gain', min: 0, max: 1 },
  room: { param: 'room.wet', min: 0, max: 1 },
  roomfade: { param: 'room.fade', min: 0, max: 1 },
  roomlp: { param: 'room.lp', min: 20, max: 24000 },
  djf: { param: 'djf.value', min: 0, max: 1 },
  busgain: { param: 'bus.gain', default: 1, min: 0, max: 10 },

  // SYNTHS
  detune: { param: 'source.detune', min: 0, max: 1 },
  wt: { param: 'source.position', min: 0, max: 1 },
  warp: { param: 'source.warp', min: 0, max: 1 },
  freq: { param: 'source.frequency', min: 20, max: 24000 },
};

export function getSuperdoughControlData() {
  return CONTROL_DATA;
}
