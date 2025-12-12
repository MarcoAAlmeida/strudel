let audioContext;

export const setDefaultAudioContext = (existingAudioCtx) => {
  audioContext = existingAudioCtx ?? new AudioContext();
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
