/**
 * Arc 2 Investigator Desk & Arc 3 Convergence procedural foley cue synthesizer bank.
 */

import { SOUND_CUES } from './cueResolver.js?v=3.14';

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
      triggerNoise(now, 0.1, 'lowpass', 340 * pitch, 145 * pitch, 1.25, 0.24, 0.004);
      return true;

    case SOUND_CUES.ARC2_LAMP_TOGGLE:
      // Crisp double-detent brass pull-chain switch + faint filament buzz fade
      triggerIronModalRing(now, 0.075, 1180 * pitch, 0.24, 2.2, false);
      triggerIronModalRing(now + 0.042, 0.09, 860 * pitch, 0.3, 1.9, false);
      triggerNoise(now + 0.01, 0.08, 'bandpass', 1800, 950, 2.4, 0.12, 0.003);
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

    case SOUND_CUES.ARC2_STRING_WIRE:
      // Taut crimson twine snap + cork thumbtack press
      triggerTone(now, 0.05, 360 * pitch, 150 * pitch, 0.16, 'triangle', 0.002);
      triggerTone(now + 0.025, 0.28, 194 * pitch, 108 * pitch, 0.28, 'sine', 0.004);
      triggerNoise(now + 0.02, 0.07, 'bandpass', 1100, 420, 2.1, 0.12, 0.003);
      return true;

    case SOUND_CUES.ARC2_REEL_SCRUB:
      // Magnetic capstan latch + tape hiss swell + slow submerged metallic thump
      triggerIronModalRing(now, 0.14, 520 * pitch, 0.22, 1.8, false);
      triggerNoise(now + 0.04, 0.42, 'bandpass', 920, 420, 1.5, 0.2, 0.03);
      triggerTone(now + 0.14, 0.28, 66 * pitch, 42 * pitch, 0.32, 'sine', 0.012);
      return true;

    case SOUND_CUES.ARC2_REDACTION_DISSOLVE:
      // Chemical solvent wipe hiss + heavy paper fiber dampening
      triggerNoise(now, 0.34, 'bandpass', 1350, 460, 1.6, 0.22, 0.02);
      triggerTone(now + 0.06, 0.26, 98 * pitch, 54 * pitch, 0.22, 'sine', 0.018);
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
      triggerTone(now + 0.12, 0.72, 63 * pitch, 52 * pitch, 0.2, 'sine', 0.06);
      return true;

    case SOUND_CUES.ARC3_ENTER:
      // Dual subterranean convergence drone
      triggerTone(now, 0.84, 50 * pitch, 38 * pitch, 0.36, 'sine', 0.1);
      triggerTone(now + 0.04, 0.76, 75 * pitch, 58 * pitch, 0.18, 'sine', 0.1);
      return true;

    default:
      return false;
  }
}
