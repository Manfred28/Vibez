/**
 * Arc 1 Chamber procedural foley cue synthesizer bank.
 */

import { SOUND_CUES } from './cueResolver.js?v=3.13';

export function renderArc1Cue(cue, phase, pitch, now, dsp) {
  const {
    triggerTone,
    triggerNoise,
    triggerIronModalRing,
    triggerTendonTear,
    triggerWetSquelch,
    triggerBubbleCluster,
  } = dsp;

  const p = Math.max(0, Math.min(6, phase ?? 0));
  const pDark = 1.0 - Math.min(4, p) * 0.035;
  const darkPitch = pitch * pDark;

  switch (cue) {
    case SOUND_CUES.VALVE_PRESS:
      // Deep heavy cast-iron-in-masonry wall valve thump: dominant sub/chest thump + darkened iron ring
      triggerTone(now, 0.24, 108 * darkPitch, 36 * darkPitch, 0.66, 'sine', 0.005);
      triggerTone(now, 0.15, 152 * darkPitch, 62 * darkPitch, 0.36, 'sine', 0.005);
      triggerNoise(now, 0.11, 'lowpass', 260 * darkPitch, 115 * darkPitch, 1.35, 0.34, 0.004);
      triggerIronModalRing(now, 0.25, 154 * darkPitch, 0.40, 1.25, true, 0.24);
      triggerIronModalRing(now + 0.046, 0.17, 192 * darkPitch, 0.15, 1.50, false, 0.20);
      if (p >= 1) {
        const wet = Math.max(0.25, Math.min(1.0, p / 4));
        triggerWetSquelch(now + 0.06, 0.18, darkPitch, 0.26 * wet, 280, 140, 10.5);
        triggerBubbleCluster(now + 0.07, 0.13, 2 + Math.min(4, p), 165 * darkPitch, 0.12 * wet);
      }
      return true;

    case SOUND_CUES.CREATURE_BUY_SUTURE_HAND:
      // Wet fascia tearing loose from plaster + tendons snapping + cold iron rim grip
      triggerTendonTear(now, 0.26, darkPitch, 0.62, 16);
      triggerWetSquelch(now + 0.03, 0.24, darkPitch, 0.34, 260, 520, 8.0);
      triggerIronModalRing(now + 0.165, 0.22, 265 * darkPitch, 0.42, 1.35, false);
      return true;

    case SOUND_CUES.CREATURE_BUY_LEECH_CLERK:
      // Long viscous wet mucosal squelch + irregular suction-cup bubbles dragging on cold glass
      triggerWetSquelch(now, 0.42, darkPitch, 0.72, 220, 620, 8.5);
      triggerBubbleCluster(now + 0.04, 0.32, 7, 185 * darkPitch, 0.24);
      return true;

    case SOUND_CUES.CREATURE_BUY_WEEPING_LAMP:
      // Eerie hollow thick-glass vessel toll + slow heavy viscous condensation drops
      triggerTone(now, 0.46, 182 * darkPitch, 179 * darkPitch, 0.30, 'sine', 0.015);
      triggerTone(now, 0.36, 346 * darkPitch, 340 * darkPitch, 0.15, 'sine', 0.014);
      triggerTone(now, 0.26, 588 * darkPitch, 578 * darkPitch, 0.08, 'sine', 0.012);
      triggerTone(now, 0.44, 91 * darkPitch, 86 * darkPitch, 0.22, 'sine', 0.018);
      triggerBubbleCluster(now + 0.14, 0.26, 4, 195 * darkPitch, 0.20);
      return true;

    case SOUND_CUES.CREATURE_BUY_SUMP_SIPHON:
      // Cavernous wet bellows inhale + submerged bubbling iron-pipe gulp + iron seat clank
      triggerWetSquelch(now, 0.48, darkPitch, 0.68, 420, 150, 6.2);
      triggerBubbleCluster(now + 0.05, 0.38, 8, 140 * darkPitch, 0.26);
      triggerIronModalRing(now + 0.28, 0.24, 175 * darkPitch, 0.36, 1.3, false);
      return true;

    case SOUND_CUES.ANOMALY_TOOTH:
      // Taut periodontal ligament tearing out of wet plaster + enamel root wrench pop
      triggerTendonTear(now, 0.28, pitch, 0.72, 18);
      triggerWetSquelch(now + 0.08, 0.22, pitch, 0.38, 380, 190, 8.5);
      triggerBubbleCluster(now + 0.16, 0.12, 4, 230 * pitch, 0.20);
      return true;

    case SOUND_CUES.ANOMALY_EYE:
      // Damp rubbery condensation smear + wet mucosal eyelid squelch + hollow sphere roll
      triggerWetSquelch(now, 0.34, pitch, 0.68, 290, 780, 6.0);
      triggerBubbleCluster(now + 0.14, 0.20, 5, 210 * pitch, 0.20);
      return true;

    case SOUND_CUES.ANOMALY_NODULE:
      // Taut blister membrane tearing open + juicy oozing wet squelch & bubble burst
      triggerTendonTear(now, 0.16, pitch, 0.50, 12);
      triggerWetSquelch(now + 0.04, 0.38, pitch, 0.76, 480, 175, 9.5);
      triggerBubbleCluster(now + 0.07, 0.30, 7, 175 * pitch, 0.26);
      return true;

    case SOUND_CUES.ANOMALY_RIB:
      // Deep calcified bone wrench + long fibrous tendon/ligament ripping + marrow groan
      triggerTendonTear(now, 0.44, pitch * 0.92, 0.78, 24);
      triggerWetSquelch(now + 0.10, 0.36, pitch, 0.46, 280, 540, 6.5);
      triggerBubbleCluster(now + 0.18, 0.24, 5, 155 * pitch, 0.20);
      return true;

    case SOUND_CUES.JAR_BOTTLE:
      // Dark thick-glass vessel resonance + viscous suction wax-cork seal
      triggerTone(now, 0.28, 248 * pitch, 244 * pitch, 0.24, 'sine', 0.008);
      triggerTone(now, 0.22, 412 * pitch, 405 * pitch, 0.12, 'sine', 0.008);
      triggerTone(now, 0.28, 124 * pitch, 118 * pitch, 0.18, 'sine', 0.01);
      triggerWetSquelch(now + 0.05, 0.26, pitch, 0.54, 360, 165, 8.0);
      triggerBubbleCluster(now + 0.08, 0.18, 4, 170 * pitch, 0.22);
      return true;

    case SOUND_CUES.GRAFT_PERFORM:
      // Long (~1.15s) deeply organic & grotesque grafting ritual:
      // peristaltic body swell + 2 overlapping tendon tears + 2 mucosa squelches + 11 bubbles
      triggerTone(now, 1.12, 60 * pitch, 33 * pitch, 0.34, 'sine', 0.16);
      triggerTendonTear(now + 0.05, 0.48, pitch * 0.96, 0.66, 24);
      triggerTendonTear(now + 0.34, 0.52, pitch * 0.88, 0.72, 26);
      triggerWetSquelch(now + 0.02, 0.72, pitch, 0.66, 220, 590, 7.2);
      triggerWetSquelch(now + 0.52, 0.58, pitch * 0.92, 0.60, 440, 155, 5.4);
      triggerBubbleCluster(now + 0.16, 0.74, 11, 165 * pitch, 0.24);
      return true;

    case SOUND_CUES.SCAR_UNLOCK:
      // Surgical thread dragging taut through damp flesh + crisp tendon cinch
      triggerTendonTear(now, 0.30, pitch * 1.05, 0.64, 15);
      triggerWetSquelch(now + 0.06, 0.26, pitch, 0.42, 520, 220, 9.0);
      return true;

    case SOUND_CUES.DRAIN_TOGGLE:
      // Heavy rusted cast-iron floor sump grate latch slam & metallic ring
      triggerIronModalRing(now, 0.32, 205 * pitch, 0.72, 0.92, true);
      triggerIronModalRing(now + 0.048, 0.22, 290 * pitch, 0.32, 1.25, false);
      return true;

    case SOUND_CUES.SUMP_DREDGE:
      // Submerged rusted iron chain-link haul & dark viscous sump water slosh
      triggerWetSquelch(now, 0.50, pitch, 0.58, 280, 460, 6.8);
      triggerBubbleCluster(now + 0.04, 0.42, 8, 160 * pitch, 0.22);
      triggerIronModalRing(now + 0.02, 0.20, 320 * pitch, 0.38, 1.4, false);
      triggerIronModalRing(now + 0.16, 0.24, 255 * pitch, 0.42, 1.2, false);
      return true;

    case SOUND_CUES.ECHO_PULSE:
      // Deeply muffled triple knuckle-knock behind thick wet masonry
      for (let i = 0; i < 3; i += 1) {
        const t = now + i * 0.16;
        const amp = 0.30 * (1 - i * 0.2);
        triggerTone(t, 0.12, (96 - i * 7) * pitch, 44 * pitch, amp, 'sine', 0.005);
        triggerNoise(t, 0.09, 'lowpass', 240, 100, 1.1, amp * 0.65, 0.005);
      }
      return true;

    case SOUND_CUES.CLAMP_ENGAGE:
      // Massive iron jaws bolting shut over the valve with cold metallic locking ring
      triggerIronModalRing(now, 0.40, 185 * pitch, 0.74, 0.85, true);
      triggerIronModalRing(now + 0.095, 0.30, 278 * pitch, 0.48, 1.05, false);
      triggerTone(now, 0.30, 72 * pitch, 34 * pitch, 0.42, 'sine', 0.004);
      return true;

    case SOUND_CUES.MARROW_BLOOM:
      // Iron clamp release snapping open into a cavernous tearing marrow & wet tissue bloom
      triggerIronModalRing(now, 0.28, 225 * pitch, 0.48, 1.15, true);
      triggerTendonTear(now + 0.04, 0.48, pitch * 0.90, 0.66, 22);
      triggerWetSquelch(now + 0.08, 0.64, pitch, 0.68, 520, 150, 6.5);
      triggerBubbleCluster(now + 0.14, 0.52, 9, 160 * pitch, 0.22);
      return true;

    case SOUND_CUES.THROAT_OFFER:
      // Cavernous wet esophageal swallow + mucosal squelch + calcified tooth settle
      triggerWetSquelch(now, 0.52, pitch * 0.92, 0.74, 460, 140, 5.5);
      triggerBubbleCluster(now + 0.08, 0.38, 7, 145 * pitch, 0.24);
      triggerTendonTear(now + 0.22, 0.24, pitch * 0.86, 0.40, 12);
      return true;

    case SOUND_CUES.EXCISION_CUT:
      // Heavy cold surgical iron shear slicing straight through thick sinew into hollow collapse
      triggerIronModalRing(now, 0.46, 265 * pitch, 0.64, 0.82, true);
      triggerTendonTear(now + 0.04, 0.56, pitch * 0.88, 0.74, 26);
      triggerWetSquelch(now + 0.18, 0.66, pitch * 0.86, 0.54, 380, 120, 4.8);
      triggerTone(now + 0.04, 0.82, 72 * pitch, 29 * pitch, 0.42, 'sine', 0.02);
      return true;

    case SOUND_CUES.PLEAD_SALVATION:
      // Low mournful damp seam sigh dissolving into muffled exterior rain
      triggerTone(now, 0.94, 54 * pitch, 46 * pitch, 0.34, 'sine', 0.12);
      triggerTone(now + 0.06, 0.88, 81 * pitch, 72 * pitch, 0.18, 'sine', 0.14);
      triggerNoise(now + 0.10, 0.84, 'bandpass', 620, 320, 0.9, 0.26, 0.14);
      return true;

    case SOUND_CUES.STORY_DRIP:
      // Quiet low-register cistern condensation drop announcing a newly unlocked Story fragment
      triggerBubbleCluster(now, 0.02, 1, 225 * pitch, 0.20);
      triggerTone(now + 0.05, 0.18, 122 * pitch, 110 * pitch, 0.10, 'sine', 0.01);
      return true;

    case SOUND_CUES.PHASE_SHIFT:
      // Deep brooding architectural groan + wet masonry shift
      triggerWetSquelch(now + 0.06, 0.62, pitch * 0.85, 0.38, 180, 310, 3.5);
      triggerTone(now, 0.76, 46 * pitch, 36 * pitch, 0.36, 'sine', 0.10);
      triggerTone(now, 0.68, 69 * pitch, 54 * pitch, 0.18, 'triangle', 0.10);
      return true;

    default:
      return false;
  }
}
