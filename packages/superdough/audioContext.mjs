import { analysers, resetGlobalEffects } from "./superdough.mjs";

let audioContext;

export const setDefaultAudioContext = () => {
  audioContext = new AudioContext();
  resetGlobalEffects()
  return audioContext;
};

export const setAudioContext = (context) => {
  audioContext = context
  return audioContext;
};

export const getAudioContext = () => {
  if (!audioContext) {
    return setDefaultAudioContext();
  }

  return audioContext;
};

export function getAudioContextCurrentTime() {
  return getAudioContext().currentTime;
}
