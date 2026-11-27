/**
 * Procedural Web Audio foley cue bank for Arc 3 ("The Unmaking"):
 * 1:1 parity with Android Arc3FoleyCueBank.kt.
 */

import { SOUND_CUES } from './cueResolver.js?v=3.16';

export function renderArc3Cue(cue, phase, pitch, now, dsp) {
  const {
    triggerTone,
    triggerNoise,
    triggerIronModalRing,
    triggerTendonTear,
    triggerWetSquelch,
    triggerBubbleCluster,
  } = dsp;

  switch (cue) {
    case SOUND_CUES.ARC3_WRENCH:
      triggerIronModalRing(now, 0.16, 215 * pitch, 0.42, 1.6, true, 0.85);
      triggerWetSquelch(now + 0.03, 0.18, pitch * 0.94, 0.36, 210, 470, 9.5);
      triggerTone(now, 0.16, 74 * pitch, 36 * pitch, 0.44, 'sine', 0.008);
      return true;

    case SOUND_CUES.ARC3_JAR_SHATTER:
      triggerTone(now, 0.11, 1490 * pitch, 980 * pitch, 0.22, 'triangle', 0.003);
      triggerTone(now + 0.01, 0.10, 2240 * pitch, 1480 * pitch, 0.18, 'sine', 0.003);
      triggerNoise(now, 0.22, 'bandpass', 2400, 900, 1.8, 0.38, 0.004, true);
      triggerIronModalRing(now + 0.05, 0.09, 860 * pitch, 0.20, 3.0, false, 0.50);
      triggerIronModalRing(now + 0.11, 0.08, 1080 * pitch, 0.16, 3.2, false, 0.45);
      triggerWetSquelch(now + 0.06, 0.34, pitch, 0.36, 540, 210, 8.4);
      triggerBubbleCluster(now + 0.12, 0.24, 7, 360 * pitch, 0.22);
      return true;

    case SOUND_CUES.ARC3_UNMAKE_TEAR:
      triggerTendonTear(now, 0.38, pitch * 0.92, 0.56, 22);
      triggerIronModalRing(now + 0.02, 0.24, 175 * pitch, 0.30, 1.4, true, 0.60);
      triggerWetSquelch(now + 0.14, 0.36, pitch * 0.86, 0.44, 360, 155, 6.5);
      return true;

    case SOUND_CUES.ARC3_RUPTURE_POP:
      triggerTendonTear(now, 0.18, pitch * 1.12, 0.46, 14);
      triggerWetSquelch(now + 0.04, 0.30, pitch * 0.96, 0.48, 480, 195, 10.5);
      triggerBubbleCluster(now + 0.05, 0.22, 9, 290 * pitch, 0.26);
      triggerTone(now, 0.18, 190 * pitch, 48 * pitch, 0.48, 'sine', 0.006);
      return true;

    case SOUND_CUES.ARC3_WEDGE_DRIVE:
      triggerIronModalRing(now, 0.44, 142 * pitch, 0.54, 0.95, false, 1.0);
      triggerTendonTear(now + 0.06, 0.48, pitch * 0.84, 0.46, 24);
      triggerWetSquelch(now + 0.16, 0.62, pitch * 0.8, 0.44, 280, 125, 4.6);
      triggerTone(now, 0.58, 66 * pitch, 32 * pitch, 0.58, 'sine', 0.008);
      triggerTone(now + 0.02, 0.46, 96 * pitch, 92 * pitch, 0.26, 'triangle', 0.015);
      return true;

    case SOUND_CUES.ARC3_CLOT_WARNING:
      triggerWetSquelch(now, 0.38, pitch * 0.78, 0.38, 145, 295, 4.2);
      triggerTone(now, 0.40, 44 * pitch, 72 * pitch, 0.32, 'sine', 0.06);
      return true;

    case SOUND_CUES.ARC3_FINAL_PULL:
      triggerTendonTear(now, 0.64, pitch * 0.76, 0.52, 26);
      triggerWetSquelch(now + 0.12, 0.74, pitch * 0.72, 0.50, 240, 105, 3.2);
      triggerBubbleCluster(now + 0.20, 0.56, 10, 195 * pitch, 0.24);
      triggerTone(now, 0.86, 38 * pitch, 34 * pitch, 0.52, 'sine', 0.09);
      return true;

    case SOUND_CUES.ARC3_SUBMERSION_END:
      triggerWetSquelch(now, 0.90, pitch * 0.68, 0.48, 290, 92, 2.4);
      triggerBubbleCluster(now + 0.10, 0.82, 12, 165 * pitch, 0.22);
      triggerNoise(now, 1.30, 'lowpass', 460, 85, 1.4, 0.46, 0.12, false);
      triggerTone(now + 0.68, 0.24, 44 * pitch, 32 * pitch, 0.54, 'sine', 0.02);
      triggerTone(now + 0.92, 0.26, 40 * pitch, 30 * pitch, 0.40, 'sine', 0.02);
      return true;

    default:
      return false;
  }
}
