/**
 * Arc 2 Investigator Desk & Arc 3 Convergence procedural foley cue synthesizer bank.
 */

import { SOUND_CUES } from './cueResolver.js?v=3.15';

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

    case SOUND_CUES.ARC2_LAMP_ON:
      // Lamp On: crisp upward brass pull-chain detent + faint warm tungsten filament settle
      triggerIronModalRing(now, 0.08, 680 * pitch, 0.28, 2.5, false, 0.42);
      triggerIronModalRing(now + 0.028, 0.10, 540 * pitch, 0.24, 2.1, false, 0.36);
      triggerTone(now + 0.015, 0.16, 120 * pitch, 120 * pitch, 0.15, 'sine', 0.018);
      triggerTone(now + 0.015, 0.14, 240 * pitch, 240 * pitch, 0.07, 'sine', 0.018);
      return true;

    case SOUND_CUES.ARC2_LAMP_OFF:
      // Lamp Off: heavier downward brass drop-clack settling into muted nocturnal rain quiet
      triggerIronModalRing(now, 0.09, 460 * pitch, 0.26, 2.2, false, 0.34);
      triggerIronModalRing(now + 0.038, 0.12, 320 * pitch, 0.28, 1.9, false, 0.28);
      triggerTone(now, 0.18, 96 * pitch, 48 * pitch, 0.26, 'sine', 0.005);
      return true;

    case SOUND_CUES.ARC2_CONTACT_ARCHIVIST:
      // Night Archivist: heavy damp basement ledger thump + thick manila folder slide
      triggerNoise(now, 0.30, 'bandpass', 620, 360, 1.8, 0.28, 0.04);
      triggerTone(now, 0.18, 118 * pitch, 58 * pitch, 0.30, 'sine', 0.006);
      return true;

    case SOUND_CUES.ARC2_CONTACT_ORDERLY:
      // Municipal Courier: hurried carbon slip sliding under wooden office door + muffled double latch tap
      triggerNoise(now, 0.18, 'bandpass', 1180, 760, 2.2, 0.24, 0.018);
      triggerTone(now + 0.10, 0.09, 148 * pitch, 112 * pitch, 0.24, 'triangle', 0.004);
      triggerTone(now + 0.175, 0.09, 132 * pitch, 98 * pitch, 0.18, 'triangle', 0.004);
      return true;

    case SOUND_CUES.ARC2_CONTACT_SCANNER:
      // Microfilm Reader: crisp glass shutter click + short motorized parcel-roll advance whirr
      triggerIronModalRing(now, 0.08, 780 * pitch, 0.24, 2.8, false, 0.40);
      triggerTone(now + 0.04, 0.26, 162 * pitch, 164 * pitch, 0.18, 'sine', 0.03);
      triggerTone(now + 0.04, 0.24, 324 * pitch, 328 * pitch, 0.09, 'sine', 0.03);
      triggerNoise(now + 0.04, 0.26, 'bandpass', 940, 1080, 2.5, 0.18, 0.03);
      return true;

    case SOUND_CUES.ARC2_CONTACT_TRANSCRIBER:
      // Reel Transcriber: mechanical dictation key clack + brief tape-spool start chirp
      triggerIronModalRing(now, 0.07, 540 * pitch, 0.28, 2.6, false, 0.38);
      triggerIronModalRing(now + 0.055, 0.09, 410 * pitch, 0.22, 2.3, false, 0.32);
      triggerTone(now + 0.07, 0.21, 210 * pitch, 305 * pitch, 0.16, 'sine', 0.025);
      triggerNoise(now + 0.07, 0.21, 'bandpass', 1380, 1180, 2.4, 0.16, 0.025);
      return true;

    case SOUND_CUES.ARC2_DEDUCTION:
      // Corkboard pin press + dark damped bass red-string pluck
      triggerTone(now, 0.07, 290 * pitch, 110 * pitch, 0.18, 'triangle', 0.003);
      triggerTone(now + 0.02, 0.24, 158 * pitch, 92 * pitch, 0.26, 'sine', 0.006);
      return true;

    case SOUND_CUES.ARC2_CORROBORATION_LOCK:
      // Curated Corroboration lock: crisp brass pin press + taut bowed dark fifth resonance
      triggerIronModalRing(now, 0.10, 590 * pitch, 0.24, 2.4, false, 0.32);
      triggerTone(now, 0.09, 260 * pitch, 110 * pitch, 0.18, 'triangle', 0.003);
      triggerTone(now + 0.01, 0.52, 110 * pitch, 108 * pitch, 0.30, 'sine', 0.015);
      triggerTone(now + 0.01, 0.48, 165 * pitch, 162 * pitch, 0.22, 'sine', 0.015);
      triggerTone(now + 0.01, 0.34, 220 * pitch, 217 * pitch, 0.10, 'sine', 0.015);
      return true;

    case SOUND_CUES.ARC2_STRING_WIRE:
      // Speculative crimson twine snap + cork thumbtack press
      triggerTone(now, 0.05, 315 * pitch, 120 * pitch, 0.16, 'triangle', 0.002);
      triggerTone(now + 0.02, 0.26, 171 * pitch, 102 * pitch, 0.26, 'sine', 0.004);
      triggerNoise(now + 0.02, 0.07, 'bandpass', 1100, 420, 2.1, 0.12, 0.003);
      return true;

    case SOUND_CUES.ARC2_STRING_UNWIRE:
      // Unwiring a corkboard thread: soft slack twine release + muted cork pin pull
      triggerTone(now, 0.18, 148 * pitch, 78 * pitch, 0.24, 'sine', 0.008);
      triggerNoise(now, 0.16, 'bandpass', 520, 660, 1.9, 0.18, 0.012);
      return true;

    case SOUND_CUES.ARC2_REEL_SCRUB:
      // Magnetic capstan latch + tape hiss swell + slow submerged metallic thump
      triggerIronModalRing(now, 0.14, 520 * pitch, 0.22, 1.8, false);
      triggerNoise(now + 0.04, 0.42, 'bandpass', 1100, 450, 2.2, 0.22, 0.03);
      triggerTone(now + 0.14, 0.28, 52 * pitch, 46 * pitch, 0.34, 'sine', 0.018);
      return true;

    case SOUND_CUES.ARC2_REDACTION_DISSOLVE:
      // Chemical solvent wipe hiss + heavy paper fiber dampening
      triggerNoise(now, 0.44, 'bandpass', 1650, 520, 1.5, 0.24, 0.025);
      triggerTone(now + 0.04, 0.30, 88 * pitch, 54 * pitch, 0.22, 'sine', 0.018);
      return true;

    case SOUND_CUES.ARC2_ANOMALY_ENVELOPE:
      // Desk Anomaly 1 — Evidence Envelope: damp manila envelope paper slit + crease rustle
      triggerTendonTear(now, 0.20, pitch * 1.18, 0.38, 10);
      triggerNoise(now, 0.28, 'bandpass', 1480, 920, 1.6, 0.26, 0.02);
      return true;

    case SOUND_CUES.ARC2_ANOMALY_SPECTACLES:
      // Desk Anomaly 2 — Fogged Spectacles: damped cloth wipe across cold glass lenses + rim tap
      triggerNoise(now, 0.17, 'bandpass', 920, 1220, 2.2, 0.24, 0.025);
      triggerNoise(now + 0.14, 0.20, 'bandpass', 1160, 740, 2.2, 0.20, 0.025);
      triggerTone(now + 0.19, 0.12, 640 * pitch, 635 * pitch, 0.14, 'sine', 0.004);
      triggerTone(now + 0.19, 0.10, 985 * pitch, 975 * pitch, 0.08, 'sine', 0.004);
      return true;

    case SOUND_CUES.ARC2_ANOMALY_REEL_TAPE:
      // Desk Anomaly 3 — Reel-to-Reel Tape: brass spool latch + warped tape hiss + distant basement thump
      triggerIronModalRing(now, 0.11, 440 * pitch, 0.24, 2.0, false, 0.34);
      triggerNoise(now + 0.02, 0.34, 'bandpass', 1260, 940, 2.3, 0.22, 0.04);
      triggerTone(now + 0.12, 0.18, 64 * pitch, 54 * pitch, 0.28, 'sine', 0.02);
      return true;

    case SOUND_CUES.ARC2_ANOMALY_ENAMEL:
      // Desk Anomaly 4 (Case IV+ only) — Enamel Exhibit: cold molar/enamel clink in steel tray + wet subterranean tail
      triggerIronModalRing(now, 0.14, 510 * pitch, 0.30, 2.2, false, 0.34);
      triggerIronModalRing(now + 0.075, 0.12, 430 * pitch, 0.20, 2.4, false, 0.28);
      triggerWetSquelch(now + 0.10, 0.26, pitch * 0.90, 0.30, 340, 160, 6.2);
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
