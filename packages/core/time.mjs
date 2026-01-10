let time;
let cpsFunc;
export function getTime() {
  if (!time) {
    throw new Error('no time set! use setTime to define a time source');
  }
  return time();
}

export function setTime(func) {
  time = func;
}

export function setCpsFunc(func) {
  cpsFunc = func;
}

export function getCps() {
  return cpsFunc?.();
}
