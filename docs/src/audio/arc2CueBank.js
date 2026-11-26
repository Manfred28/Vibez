/**
 * Arc 2 Investigator Desk & Arc 3 Convergence procedural foley cue synthesizer bank.
 */

import { SOUND_CUES } from './cueResolver.js?v=3.12';

export function renderArc2Cue(cue, _phase, pitch, now, dsp) {
  const {
    triggerTone,
    triggerNoise,
    triggerIronModalRing,
    triggerTendonTear,
    triggerWetSquelch,
  } = dsp;

  switch (cue) {
    case SOUND_CUES.ARC2_STAMP:
      // Deep heavy muffled wooden desk stamp thump + dark blotter slap
      triggerTone(now, 0.18, 84 * pitch, 36 * pitch, 0.52, 'sine', 0.004);
      triggerTone(now, 0.12, 128 * pitch, 56 * pitch, 0.22, 'sine', 0.004);
      triggerNoise(now, 0.10, 'lowpass', 340 * pitch, 145 * pitch, 1.25, 0.24, 0.004);
      return true;

    case SOUND_CUES.ARC2_CONTACT:
      // Heavy damp manila folder slide + low desk drawer tap
      triggerNoise(now, 0.22, 'bandpass', 680, 300, 1.8, 0.24, 0.015);
      triggerTone(now + 0.05, 0.13, 142 * pitch, 72 * pitch, 0.24, 'triangle', 0.005);
      return true;

    case SOUND_CUES.ARC2_DEDUCTION:
      // Corkboard pin press + dark damped bass red-string pluck
      triggerTone(now, 0.07, 290 * pitch, 110 * pitch, 0.18, 'triangle', 0.003);
      triggerTone(now + 0.02, 0.24, 158 * pitch, 92 * pitch, 0.26, 'sine', 0.006);
      return true;

    case SOUND_CUES.ARC2_DESK_ANOMALY:
      // Damp envelope slit / muted brass reel latch
      triggerTendonTear(now, 0.22, pitch * 1.12, 0.44, 12);
      triggerIronModalRing(now + 0.04, 0.18, 340 * pitch, 0.24, 1.6, false);
      return true;

    case SOUND_CUES.ARC2_CHAPTER:
      // Heavy case-file slam
      triggerTone(now, 0.32, 76 * pitch, 38 * pitch, 0.38, 'sine', 0.005);
      triggerNoise(now + 0.02, 0.24, 'lowpass', 440, 180, 1.2, 0.24, 0.006);
      return true;

    case SOUND_CUES.ARC2_FILE_ZERO:
      // Heavy manila dossier flap + cold subterranean grout bleed
      triggerWetSquelch(now + 0.08, 0.74, pitch * 0.82, 0.34, 220, 135, 3.2);
      triggerTone(now + 0.04, 0.88, 42 * pitch, 34 * pitch, 0.42, 'sine', 0.06);
      triggerTone(now + 0.12, 0.72, 63 * pitch, 52 * pitch, 0.20, 'sine', 0.06);
      return true;

    case SOUND_CUES.ARC3_ENTER:
      // Dual subterranean convergence drone
      triggerTone(now, 0.84, 50 * pitch, 38 * pitch, 0.36, 'sine', 0.10);
      triggerTone(now + 0.04, 0.76, 75 * pitch, 58 * pitch, 0.18, 'sine', 0.10);
      return true;

    default:
      return false;
  }
}
