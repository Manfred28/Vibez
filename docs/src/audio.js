/**
 * Zero-asset Web Audio procedural foley & phase-escalating room ambience engine for
 * "The Tending of the Unnamed Organ" (1:1 parity with Android SoundCueResolver + ProceduralFoleySynth).
 *
 * Acoustic architecture:
 * - True cast-iron inharmonic modal bank (6 detuned iron ring modes + gritty pawl ratchet + heavy
 *   wall-mounted housing thud) for the valve, floor sump grate, iron clamp, and surgical shears.
 * - Visceral organic flesh/tendon synthesis: stochastic fibrous tearing trains through dual
 *   sweeping formant resonators, LFO-modulated wet mucosa squelches, and irregular cavity bubble
 *   clusters — eliminating generic percussive synth clicks.
 * - Subtle closed-tile-chamber early reflection tail (`19ms` / `35ms`) grounding every cue in a
 *   damp subterranean room, with zero tab-switch clicks.
 */

import { CREATURE_TYPES, DESK_ANOMALY_TYPES, GAME_ARCS, WALL_ANOMALY_TYPES } from './engine.js?v=3.9';

export const AUDIO_MUTE_STORAGE_KEY = 'unnamed_organ_audio_muted_v1';

// Inharmonic cast-iron wheel ring modal ratios (authentic cast-iron clank/clang spectrum)
export const IRON_MODE_RATIOS = Object.freeze([1.0, 1.53, 2.24, 2.89, 3.71, 4.68]);
const IRON_MODE_AMPS = Object.freeze([0.50, 0.48, 0.44, 0.36, 0.28, 0.20]);
const IRON_MODE_DECAYS = Object.freeze([10.5, 13.0, 16.5, 21.0, 27.0, 34.0]);

export const SOUND_CUES = Object.freeze({
  VALVE_PRESS: 'valve_press',
  CREATURE_BUY_SUTURE_HAND: 'creature_buy_suture_hand',
  CREATURE_BUY_LEECH_CLERK: 'creature_buy_leech_clerk',
  CREATURE_BUY_WEEPING_LAMP: 'creature_buy_weeping_lamp',
  CREATURE_BUY_SUMP_SIPHON: 'creature_buy_sump_siphon',
  ANOMALY_TOOTH: 'anomaly_tooth',
  ANOMALY_EYE: 'anomaly_eye',
  ANOMALY_NODULE: 'anomaly_nodule',
  ANOMALY_RIB: 'anomaly_rib',
  JAR_BOTTLE: 'jar_bottle',
  GRAFT_PERFORM: 'graft_perform',
  SCAR_UNLOCK: 'scar_unlock',
  DRAIN_TOGGLE: 'drain_toggle',
  SUMP_DREDGE: 'sump_dredge',
  ECHO_PULSE: 'echo_pulse',
  CLAMP_ENGAGE: 'clamp_engage',
  MARROW_BLOOM: 'marrow_bloom',
  THROAT_OFFER: 'throat_offer',
  EXCISION_CUT: 'excision_cut',
  PLEAD_SALVATION: 'plead_salvation',
  STORY_DRIP: 'story_drip',
  PHASE_SHIFT: 'phase_shift',
  ARC2_STAMP: 'arc2_stamp',
  ARC2_CONTACT: 'arc2_contact',
  ARC2_DEDUCTION: 'arc2_deduction',
  ARC2_DESK_ANOMALY: 'arc2_desk_anomaly',
  ARC2_CHAPTER: 'arc2_chapter',
  ARC2_FILE_ZERO: 'arc2_file_zero',
  ARC3_ENTER: 'arc3_enter',
});

export const SOUND_ACTION_HINTS = Object.freeze({
  TICK: 'tick',
  VALVE_PRESS: 'valve_press',
  CREATURE_BUY: 'creature_buy',
  JAR_BOTTLE: 'jar_bottle',
  GRAFT_PERFORM: 'graft_perform',
  ANOMALY_INTERACT: 'anomaly_interact',
  SCAR_UNLOCK: 'scar_unlock',
  DRAIN_TOGGLE: 'drain_toggle',
  SUMP_DREDGE: 'sump_dredge',
  ECHO_DIVERSION: 'echo_diversion',
  CLAMP_ENGAGE: 'clamp_engage',
  THROAT_OFFER: 'throat_offer',
  EXCISION: 'excision',
  BREACH_TO_ARC2: 'breach_to_arc2',
  ARC2_STAMP: 'arc2_stamp',
  ARC2_CONTACT: 'arc2_contact',
  ARC2_DEDUCTION: 'arc2_deduction',
  ARC2_DESK_ANOMALY: 'arc2_desk_anomaly',
  ARC2_CHAPTER: 'arc2_chapter',
  ARC3_ENTER: 'arc3_enter',
  DEV: 'dev',
});

const CUE_MIN_INTERVAL_MS = Object.freeze({
  [SOUND_CUES.VALVE_PRESS]: 62,
  [SOUND_CUES.CREATURE_BUY_SUTURE_HAND]: 85,
  [SOUND_CUES.CREATURE_BUY_LEECH_CLERK]: 95,
  [SOUND_CUES.CREATURE_BUY_WEEPING_LAMP]: 100,
  [SOUND_CUES.CREATURE_BUY_SUMP_SIPHON]: 110,
  [SOUND_CUES.ANOMALY_TOOTH]: 95,
  [SOUND_CUES.ANOMALY_EYE]: 115,
  [SOUND_CUES.ANOMALY_NODULE]: 115,
  [SOUND_CUES.ANOMALY_RIB]: 130,
  [SOUND_CUES.JAR_BOTTLE]: 125,
  [SOUND_CUES.GRAFT_PERFORM]: 300,
  [SOUND_CUES.SCAR_UNLOCK]: 115,
  [SOUND_CUES.DRAIN_TOGGLE]: 110,
  [SOUND_CUES.SUMP_DREDGE]: 190,
  [SOUND_CUES.ECHO_PULSE]: 260,
  [SOUND_CUES.CLAMP_ENGAGE]: 170,
  [SOUND_CUES.MARROW_BLOOM]: 320,
  [SOUND_CUES.THROAT_OFFER]: 220,
  [SOUND_CUES.EXCISION_CUT]: 420,
  [SOUND_CUES.PLEAD_SALVATION]: 450,
  [SOUND_CUES.STORY_DRIP]: 380,
  [SOUND_CUES.PHASE_SHIFT]: 450,
  [SOUND_CUES.ARC2_STAMP]: 62,
  [SOUND_CUES.ARC2_CONTACT]: 80,
  [SOUND_CUES.ARC2_DEDUCTION]: 120,
  [SOUND_CUES.ARC2_DESK_ANOMALY]: 110,
  [SOUND_CUES.ARC2_CHAPTER]: 240,
  [SOUND_CUES.ARC2_FILE_ZERO]: 450,
  [SOUND_CUES.ARC3_ENTER]: 450,
});

function sumMapValues(mapObj) {
  if (!mapObj || typeof mapObj !== 'object') return 0;
  let total = 0;
  for (const val of Object.values(mapObj)) {
    total += Number(val) || 0;
  }
  return total;
}

function mapsDiffer(a, b) {
  const keys = new Set([...Object.keys(a || {}), ...Object.keys(b || {})]);
  for (const k of keys) {
    if ((a?.[k] ?? 0) !== (b?.[k] ?? 0)) return true;
  }
  return false;
}

function clamp01(v) {
  return Math.max(0, Math.min(1, v));
}

export function readMutedPreference(storage) {
  const store = storage ?? (typeof window !== 'undefined' ? window.localStorage : null);
  if (!store) return false;
  try {
    return store.getItem(AUDIO_MUTE_STORAGE_KEY) === 'true';
  } catch {
    return false;
  }
}

export function writeMutedPreference(muted, storage) {
  const store = storage ?? (typeof window !== 'undefined' ? window.localStorage : null);
  if (!store) return;
  try {
    store.setItem(AUDIO_MUTE_STORAGE_KEY, muted ? 'true' : 'false');
  } catch {
    // Ignore private browsing storage errors.
  }
}

/**
 * Pure transition-diff cue resolver matching Android `SoundCueResolver.resolveTransitionCues`.
 */
export function resolveTransitionCues(prev, next, action = SOUND_ACTION_HINTS.TICK) {
  if (!prev || !next || prev === next || action === SOUND_ACTION_HINTS.DEV) {
    return [];
  }
  const cues = [];

  // 1. Primary action-driven cues (gated on genuine state change so failed taps stay silent)
  switch (action) {
    case SOUND_ACTION_HINTS.VALVE_PRESS:
      if (next.cycleLifetimeIchor > prev.cycleLifetimeIchor) {
        cues.push(SOUND_CUES.VALVE_PRESS);
      }
      break;
    case SOUND_ACTION_HINTS.CREATURE_BUY: {
      const bought = Object.values(CREATURE_TYPES).find(
        (t) => (next.creatures?.[t.id] ?? 0) > (prev.creatures?.[t.id] ?? 0),
      );
      if (bought?.id === CREATURE_TYPES.SUTURE_HAND.id) {
        cues.push(SOUND_CUES.CREATURE_BUY_SUTURE_HAND);
      } else if (bought?.id === CREATURE_TYPES.LEECH_CLERK.id) {
        cues.push(SOUND_CUES.CREATURE_BUY_LEECH_CLERK);
      } else if (bought?.id === CREATURE_TYPES.WEEPING_LAMP.id) {
        cues.push(SOUND_CUES.CREATURE_BUY_WEEPING_LAMP);
      } else if (bought?.id === CREATURE_TYPES.SUMP_SIPHON.id) {
        cues.push(SOUND_CUES.CREATURE_BUY_SUMP_SIPHON);
      }
      break;
    }
    case SOUND_ACTION_HINTS.JAR_BOTTLE:
      if ((next.ownedJars?.length ?? 0) > (prev.ownedJars?.length ?? 0)) {
        cues.push(SOUND_CUES.JAR_BOTTLE);
      }
      break;
    case SOUND_ACTION_HINTS.GRAFT_PERFORM:
      if (
        (next.graftedTendrils ?? 0) > (prev.graftedTendrils ?? 0) ||
        (next.choirLungs ?? 0) > (prev.choirLungs ?? 0)
      ) {
        cues.push(SOUND_CUES.GRAFT_PERFORM);
      }
      break;
    case SOUND_ACTION_HINTS.ANOMALY_INTERACT: {
      const types = Object.values(WALL_ANOMALY_TYPES);
      const interacted = types.find(
        (t) => (next.anomalyInteractions?.[t.id] ?? 0) > (prev.anomalyInteractions?.[t.id] ?? 0),
      );
      if (interacted?.id === WALL_ANOMALY_TYPES.PALE_TOOTH.id) {
        cues.push(SOUND_CUES.ANOMALY_TOOTH);
      } else if (interacted?.id === WALL_ANOMALY_TYPES.GLASS_EYE.id) {
        cues.push(SOUND_CUES.ANOMALY_EYE);
      } else if (interacted?.id === WALL_ANOMALY_TYPES.SWOLLEN_NODULE.id) {
        cues.push(SOUND_CUES.ANOMALY_NODULE);
      } else if (interacted?.id === WALL_ANOMALY_TYPES.CHITIN_RIB.id) {
        cues.push(SOUND_CUES.ANOMALY_RIB);
      }
      break;
    }
    case SOUND_ACTION_HINTS.SCAR_UNLOCK:
      if (
        (next.unlockedScarNodes?.length ?? 0) > (prev.unlockedScarNodes?.length ?? 0) ||
        sumMapValues(next.repeatableScarRanks) > sumMapValues(prev.repeatableScarRanks)
      ) {
        cues.push(SOUND_CUES.SCAR_UNLOCK);
      }
      break;
    case SOUND_ACTION_HINTS.DRAIN_TOGGLE:
      if (next.drainSealed !== prev.drainSealed) {
        cues.push(SOUND_CUES.DRAIN_TOGGLE);
      }
      break;
    case SOUND_ACTION_HINTS.SUMP_DREDGE:
      if ((next.lifetimeRelicsDredged ?? 0) > (prev.lifetimeRelicsDredged ?? 0)) {
        cues.push(SOUND_CUES.SUMP_DREDGE);
      }
      break;
    case SOUND_ACTION_HINTS.ECHO_DIVERSION:
      if (next.echoDiversionPercent !== prev.echoDiversionPercent) {
        cues.push(SOUND_CUES.DRAIN_TOGGLE);
      }
      break;
    case SOUND_ACTION_HINTS.CLAMP_ENGAGE:
      if ((next.clampRemainingSeconds ?? 0) > (prev.clampRemainingSeconds ?? 0)) {
        cues.push(SOUND_CUES.CLAMP_ENGAGE);
      }
      break;
    case SOUND_ACTION_HINTS.THROAT_OFFER:
      if ((next.unredactedDossierLines?.length ?? 0) > (prev.unredactedDossierLines?.length ?? 0)) {
        cues.push(SOUND_CUES.THROAT_OFFER);
      }
      break;
    case SOUND_ACTION_HINTS.EXCISION:
      if ((next.cycle ?? 0) > (prev.cycle ?? 0)) {
        cues.push(SOUND_CUES.EXCISION_CUT);
      }
      break;
    case SOUND_ACTION_HINTS.BREACH_TO_ARC2:
      if (
        next.activeArc === GAME_ARCS.ARC_2_INVESTIGATION &&
        prev.activeArc !== GAME_ARCS.ARC_2_INVESTIGATION
      ) {
        cues.push(SOUND_CUES.PLEAD_SALVATION);
      }
      break;
    case SOUND_ACTION_HINTS.ARC2_STAMP:
      if ((next.arc2?.lifetimeLeads ?? 0) > (prev.arc2?.lifetimeLeads ?? 0)) {
        cues.push(SOUND_CUES.ARC2_STAMP);
      }
      break;
    case SOUND_ACTION_HINTS.ARC2_CONTACT:
      if (mapsDiffer(prev.arc2?.contacts, next.arc2?.contacts)) {
        cues.push(SOUND_CUES.ARC2_CONTACT);
      }
      break;
    case SOUND_ACTION_HINTS.ARC2_DEDUCTION:
      if ((next.arc2?.ownedDeductions?.length ?? 0) > (prev.arc2?.ownedDeductions?.length ?? 0)) {
        cues.push(SOUND_CUES.ARC2_DEDUCTION);
      }
      break;
    case SOUND_ACTION_HINTS.ARC2_DESK_ANOMALY: {
      const interactedDesk = Object.values(DESK_ANOMALY_TYPES).some(
        (t) =>
          (next.arc2?.deskAnomalyInteractions?.[t.id] ?? 0) >
          (prev.arc2?.deskAnomalyInteractions?.[t.id] ?? 0),
      );
      if (interactedDesk) {
        cues.push(SOUND_CUES.ARC2_DESK_ANOMALY);
      }
      break;
    }
    case SOUND_ACTION_HINTS.ARC2_CHAPTER:
      if (next.arc2?.fileZeroRevealed && !prev.arc2?.fileZeroRevealed) {
        cues.push(SOUND_CUES.ARC2_FILE_ZERO);
      } else if ((next.arc2?.completedChapters ?? 0) > (prev.arc2?.completedChapters ?? 0)) {
        cues.push(SOUND_CUES.ARC2_CHAPTER);
      }
      break;
    case SOUND_ACTION_HINTS.ARC3_ENTER:
      if (
        next.activeArc === GAME_ARCS.ARC_3_CONVERGENCE &&
        prev.activeArc !== GAME_ARCS.ARC_3_CONVERGENCE
      ) {
        cues.push(SOUND_CUES.ARC3_ENTER);
      }
      break;
    default:
      break;
  }

  // 2. Passive / emergent chamber events
  if ((next.echoPulsesTriggered ?? 0) > (prev.echoPulsesTriggered ?? 0)) {
    cues.push(SOUND_CUES.ECHO_PULSE);
  }
  if ((next.marrowBloomCount ?? 0) > (prev.marrowBloomCount ?? 0)) {
    cues.push(SOUND_CUES.MARROW_BLOOM);
  }
  if (
    action === SOUND_ACTION_HINTS.TICK &&
    (next.graftedTendrils ?? 0) > (prev.graftedTendrils ?? 0)
  ) {
    cues.push(SOUND_CUES.GRAFT_PERFORM);
  }
  if (
    next.activeArc === GAME_ARCS.ARC_1_CHAMBER &&
    prev.activeArc === GAME_ARCS.ARC_1_CHAMBER &&
    next.phase > prev.phase &&
    !cues.includes(SOUND_CUES.EXCISION_CUT)
  ) {
    cues.push(SOUND_CUES.PHASE_SHIFT);
  }

  // 3. Quiet story condensation cue when a brand-new fragment unlocks
  const newStoryUnlocked =
    next.activeDialogue != null &&
    next.activeDialogue.id !== prev.activeDialogue?.id &&
    (next.triggeredDialogueIds?.length ?? 0) > (prev.triggeredDialogueIds?.length ?? 0);
  if (
    newStoryUnlocked &&
    !cues.includes(SOUND_CUES.EXCISION_CUT) &&
    !cues.includes(SOUND_CUES.PLEAD_SALVATION) &&
    !cues.includes(SOUND_CUES.ARC2_FILE_ZERO)
  ) {
    cues.push(SOUND_CUES.STORY_DRIP);
  }

  return cues;
}

/**
 * Pure ambient soundscape parameter mapper matching Android `SoundCueResolver.computeAmbientProfile`.
 */
export function computeAmbientProfile(state) {
  if (!state || state.activeArc === GAME_ARCS.ARC_1_CHAMBER) {
    const p = Math.max(0, Math.min(4, state?.phase ?? 0));
    const clamped = (state?.clampRemainingSeconds ?? 0) > 0;
    const still = Boolean(state?.isStillnessActive);
    const humTable = [0.24, 0.28, 0.34, 0.4, 0.46];
    const dripTable = [0.08, 0.26, 0.42, 0.5, 0.56];
    const fleshTable = [0.0, 0.12, 0.36, 0.62, 0.82];
    const hum = humTable[p] * (still ? 0.72 : 1.0);
    const drips = dripTable[p] + (still ? 0.28 : 0.0);
    const flesh = fleshTable[p];
    const cardiac = p >= 4 ? 0.72 : p === 3 ? 0.18 : 0.0;
    return {
      activeArc: GAME_ARCS.ARC_1_CHAMBER,
      phase: p,
      humGain: clamp01(hum),
      dripRate: clamp01(drips),
      fleshDepth: clamp01(flesh),
      cardiacPulse: cardiac,
      stillnessFocus: still ? 1.0 : 0.0,
      clampTension: clamped ? 0.9 : 0.0,
      rainMix: 0.0,
      complicityBleed: 0.0,
    };
  }
  if (state.activeArc === GAME_ARCS.ARC_2_INVESTIGATION) {
    const complicity = clamp01(state.arc2?.complicity ?? 0.05);
    const silent = Boolean(state.arc2?.isSilenceActive);
    const fileZero = Boolean(state.arc2?.fileZeroRevealed);
    return {
      activeArc: GAME_ARCS.ARC_2_INVESTIGATION,
      phase: fileZero ? 6 : 5,
      humGain: silent ? 0.2 : 0.28,
      dripRate: clamp01(0.14 + complicity * 0.35 + (silent ? 0.22 : 0.0)),
      fleshDepth: clamp01(complicity * 0.55),
      cardiacPulse: fileZero ? 0.45 : complicity * 0.22,
      stillnessFocus: silent ? 0.85 : 0.0,
      clampTension: 0.0,
      rainMix: silent ? 0.86 : 0.72,
      complicityBleed: complicity,
    };
  }
  return {
    activeArc: GAME_ARCS.ARC_3_CONVERGENCE,
    phase: 6,
    humGain: 0.3,
    dripRate: 0.32,
    fleshDepth: 0.4,
    cardiacPulse: 0.28,
    stillnessFocus: 0.5,
    clampTension: 0.0,
    rainMix: 0.52,
    complicityBleed: 0.65,
  };
}

/* ==================== WEB AUDIO PROCEDURAL SYNTHESIS ENGINE ==================== */

export function createWebAudioEngine({ storage } = {}) {
  let muted = readMutedPreference(storage);
  let foregroundActive = true;
  let ctx = null;
  let masterGain = null;
  let sfxBus = null;
  let ambientBus = null;
  let noiseBuffer = null;
  let crackleNoiseBuffer = null;
  let ambientNodes = null;
  let ambientTimer = null;
  let currentProfile = computeAmbientProfile(null);
  let nextCardiacAt = 0;
  let nextDripAt = 0;
  const lastPlayedMs = new Map();

  function getAudioContextCtor() {
    if (typeof window === 'undefined') return null;
    return window.AudioContext || window.webkitAudioContext || null;
  }

  function createNoiseBuffer(audioCtx) {
    const length = Math.floor(audioCtx.sampleRate * 2.0);
    const buffer = audioCtx.createBuffer(1, length, audioCtx.sampleRate);
    const data = buffer.getChannelData(0);
    let b0 = 0;
    let b1 = 0;
    for (let i = 0; i < length; i += 1) {
      const white = Math.random() * 2 - 1;
      b0 = 0.99 * b0 + white * 0.08;
      b1 = 0.93 * b1 + white * 0.22;
      data[i] = Math.max(-1, Math.min(1, white * 0.35 + b1 * 0.45 + b0 * 0.30));
    }
    return buffer;
  }

  /**
   * Crackle-rich sticky wet excitation noise: injects stochastic viscous micro-pop bursts into
   * filtered noise so resonant bandpass sweeps sound like wet flesh/mucosa/sinew rather than hiss.
   */
  function createCrackleNoiseBuffer(audioCtx) {
    const length = Math.floor(audioCtx.sampleRate * 2.0);
    const buffer = audioCtx.createBuffer(1, length, audioCtx.sampleRate);
    const data = buffer.getChannelData(0);
    let crackleEnv = 0;
    let smooth = 0;
    for (let i = 0; i < length; i += 1) {
      const white = Math.random() * 2 - 1;
      if (Math.random() > 0.988) {
        crackleEnv = 0.85 + Math.random() * 0.95;
      }
      crackleEnv *= 0.92;
      smooth = smooth * 0.55 + white * 0.45;
      const sample = smooth * (0.42 + crackleEnv * 1.45);
      data[i] = Math.max(-1, Math.min(1, sample));
    }
    return buffer;
  }

  function ensureContext() {
    if (ctx) return ctx;
    const Ctor = getAudioContextCtor();
    if (!Ctor) return null;
    try {
      ctx = new Ctor();
      const compressor = ctx.createDynamicsCompressor();
      compressor.threshold.value = -16;
      compressor.knee.value = 18;
      compressor.ratio.value = 4;
      compressor.attack.value = 0.004;
      compressor.release.value = 0.14;

      // Global closed-room warmth ceiling: preserves cast-iron overtones & wet tearing formants
      // up to 3.4kHz while keeping everything grounded inside a damp windowless chamber.
      const roomFilter = ctx.createBiquadFilter();
      roomFilter.type = 'lowpass';
      roomFilter.frequency.value = 3400;
      roomFilter.Q.value = 0.55;

      masterGain = ctx.createGain();
      masterGain.gain.value = muted || !foregroundActive ? 0.0001 : 0.88;

      sfxBus = ctx.createGain();
      sfxBus.gain.value = 0.82;

      ambientBus = ctx.createGain();
      ambientBus.gain.value = 0.30;

      // Subtle tiled-chamber early slapback reflections (19ms & 35ms damped by 1400Hz lowpass)
      const slapDelay1 = ctx.createDelay(0.1);
      slapDelay1.delayTime.value = 0.019;
      const slapGain1 = ctx.createGain();
      slapGain1.gain.value = 0.68;

      const slapDelay2 = ctx.createDelay(0.1);
      slapDelay2.delayTime.value = 0.035;
      const slapGain2 = ctx.createGain();
      slapGain2.gain.value = 0.42;

      const slapDamp = ctx.createBiquadFilter();
      slapDamp.type = 'lowpass';
      slapDamp.frequency.value = 1400;
      slapDamp.Q.value = 0.7;

      const slapMix = ctx.createGain();
      slapMix.gain.value = 0.15;

      sfxBus.connect(roomFilter);
      sfxBus.connect(slapDelay1);
      sfxBus.connect(slapDelay2);
      slapDelay1.connect(slapGain1);
      slapDelay2.connect(slapGain2);
      slapGain1.connect(slapDamp);
      slapGain2.connect(slapDamp);
      slapDamp.connect(slapMix);
      slapMix.connect(roomFilter);

      ambientBus.connect(roomFilter);
      roomFilter.connect(compressor);
      compressor.connect(masterGain);
      masterGain.connect(ctx.destination);

      noiseBuffer = createNoiseBuffer(ctx);
      crackleNoiseBuffer = createCrackleNoiseBuffer(ctx);
      initAmbientGraph();
      applyAmbientTargets(currentProfile);
      startAmbientScheduler();
      return ctx;
    } catch {
      ctx = null;
      return null;
    }
  }

  function initAmbientGraph() {
    if (!ctx || !ambientBus || ambientNodes) return;

    // 1. Deep subterranean 50Hz + 100Hz fluorescent ballast hum
    const humOsc1 = ctx.createOscillator();
    humOsc1.type = 'sine';
    humOsc1.frequency.value = 49.8;

    const humOsc2 = ctx.createOscillator();
    humOsc2.type = 'triangle';
    humOsc2.frequency.value = 99.6;

    const humFilter = ctx.createBiquadFilter();
    humFilter.type = 'lowpass';
    humFilter.frequency.value = 140;

    const humGainNode = ctx.createGain();
    humGainNode.gain.value = 0.0;

    const humOverGain = ctx.createGain();
    humOverGain.gain.value = 0.18;

    humOsc1.connect(humFilter);
    humOsc2.connect(humOverGain);
    humOverGain.connect(humFilter);
    humFilter.connect(humGainNode);
    humGainNode.connect(ambientBus);

    humOsc1.start();
    humOsc2.start();

    // 2. Damp subterranean peristalsis low noise bed with slow LFO modulation
    const fleshNoise = ctx.createBufferSource();
    fleshNoise.buffer = noiseBuffer;
    fleshNoise.loop = true;

    const fleshFilter = ctx.createBiquadFilter();
    fleshFilter.type = 'bandpass';
    fleshFilter.frequency.value = 88;
    fleshFilter.Q.value = 1.8;

    const fleshLfo = ctx.createOscillator();
    fleshLfo.type = 'sine';
    fleshLfo.frequency.value = 0.14;
    const fleshLfoDepth = ctx.createGain();
    fleshLfoDepth.gain.value = 28;
    fleshLfo.connect(fleshLfoDepth);
    fleshLfoDepth.connect(fleshFilter.frequency);

    const fleshGainNode = ctx.createGain();
    fleshGainNode.gain.value = 0.0;

    fleshNoise.connect(fleshFilter);
    fleshFilter.connect(fleshGainNode);
    fleshGainNode.connect(ambientBus);

    fleshNoise.start();
    fleshLfo.start();

    // 3. Arc 2 muffled precinct window rain behind cold glass
    const rainNoise = ctx.createBufferSource();
    rainNoise.buffer = noiseBuffer;
    rainNoise.loop = true;

    const rainFilter = ctx.createBiquadFilter();
    rainFilter.type = 'bandpass';
    rainFilter.frequency.value = 820;
    rainFilter.Q.value = 0.85;

    const rainGainNode = ctx.createGain();
    rainGainNode.gain.value = 0.0;

    rainNoise.connect(rainFilter);
    rainFilter.connect(rainGainNode);
    rainGainNode.connect(ambientBus);
    rainNoise.start();

    // 4. Valve Clamp deep iron strain drone
    const clampOsc = ctx.createOscillator();
    clampOsc.type = 'sawtooth';
    clampOsc.frequency.value = 54;
    const clampFilter = ctx.createBiquadFilter();
    clampFilter.type = 'lowpass';
    clampFilter.frequency.value = 145;
    clampFilter.Q.value = 3.5;
    const clampGainNode = ctx.createGain();
    clampGainNode.gain.value = 0.0;
    clampOsc.connect(clampFilter);
    clampFilter.connect(clampGainNode);
    clampGainNode.connect(ambientBus);
    clampOsc.start();

    ambientNodes = {
      humGainNode,
      fleshGainNode,
      rainGainNode,
      clampGainNode,
    };
  }

  function applyAmbientTargets(profile) {
    if (!ctx || !ambientNodes) return;
    const now = ctx.currentTime;
    const active = !muted && foregroundActive && ctx.state === 'running';
    const scale = active ? 1.0 : 0.0;
    ambientNodes.humGainNode.gain.setTargetAtTime(profile.humGain * 0.26 * scale, now, 0.35);
    ambientNodes.fleshGainNode.gain.setTargetAtTime(
      (profile.fleshDepth * 0.28 + profile.complicityBleed * 0.09) * scale,
      now,
      0.45,
    );
    ambientNodes.rainGainNode.gain.setTargetAtTime(profile.rainMix * 0.16 * scale, now, 0.45);
    ambientNodes.clampGainNode.gain.setTargetAtTime(profile.clampTension * 0.13 * scale, now, 0.25);
  }

  function spawnAmbientDrip(now, wetness) {
    if (!ctx || !ambientBus) return;
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    const base = 210 + Math.random() * 115;
    osc.type = 'sine';
    osc.frequency.setValueAtTime(base * 0.85, now);
    osc.frequency.exponentialRampToValueAtTime(Math.min(420, base * 1.38), now + 0.065);
    gain.gain.setValueAtTime(0.0001, now);
    gain.gain.linearRampToValueAtTime(0.10 * (0.65 + wetness * 0.5), now + 0.008);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.125);
    osc.connect(gain);
    gain.connect(ambientBus);
    osc.start(now);
    osc.stop(now + 0.14);
  }

  function spawnAmbientCardiac(now, strength) {
    if (!ctx || !ambientBus) return;
    const beats = [
      { offset: 0, freq: 46, amp: 0.32 * strength },
      { offset: 0.24, freq: 40, amp: 0.20 * strength },
    ];
    for (const b of beats) {
      const t = now + b.offset;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(b.freq * 1.12, t);
      osc.frequency.exponentialRampToValueAtTime(b.freq * 0.72, t + 0.18);
      gain.gain.setValueAtTime(0.0001, t);
      gain.gain.linearRampToValueAtTime(b.amp, t + 0.028);
      gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.24);
      osc.connect(gain);
      gain.connect(ambientBus);
      osc.start(t);
      osc.stop(t + 0.26);
    }
  }

  function startAmbientScheduler() {
    if (ambientTimer != null || typeof window === 'undefined') return;
    ambientTimer = window.setInterval(() => {
      if (!ctx || muted || !foregroundActive || ctx.state !== 'running') return;
      const now = ctx.currentTime;
      if (currentProfile.cardiacPulse > 0.08 && now >= nextCardiacAt) {
        spawnAmbientCardiac(now, currentProfile.cardiacPulse);
        nextCardiacAt = now + 1.45;
      }
      if (currentProfile.dripRate > 0.04 && now >= nextDripAt) {
        spawnAmbientDrip(now, currentProfile.stillnessFocus);
        const gap = Math.max(0.85, 4.5 - currentProfile.dripRate * 3.1) * (0.65 + Math.random() * 0.75);
        nextDripAt = now + gap;
      }
    }, 240);
  }

  function syncMasterState() {
    if (!ctx || !masterGain) return;
    const active = !muted && foregroundActive;
    const now = ctx.currentTime;
    masterGain.gain.setTargetAtTime(active ? 0.88 : 0.0001, now, 0.04);
    applyAmbientTargets(currentProfile);
    if (!active && ctx.state === 'running') {
      ctx.suspend().catch(() => {});
    } else if (active && ctx.state === 'suspended') {
      ctx.resume().catch(() => {});
    }
  }

  // ==================== SHARED PHYSICAL ACOUSTIC PRIMITIVES ====================

  function triggerTone(
    startTime,
    duration,
    freqStart,
    freqEnd,
    peakGain,
    type = 'sine',
    attack = 0.01,
  ) {
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = type;
    osc.frequency.setValueAtTime(Math.max(20, freqStart), startTime);
    if (freqEnd !== freqStart) {
      osc.frequency.exponentialRampToValueAtTime(Math.max(20, freqEnd), startTime + duration);
    }
    gain.gain.setValueAtTime(0.0001, startTime);
    gain.gain.linearRampToValueAtTime(peakGain, startTime + Math.min(attack, duration * 0.45));
    gain.gain.exponentialRampToValueAtTime(0.0001, startTime + duration);
    osc.connect(gain);
    gain.connect(sfxBus);
    osc.start(startTime);
    osc.stop(startTime + duration + 0.025);
  }

  function triggerNoise(
    startTime,
    duration,
    filterType,
    freqStart,
    freqEnd,
    q,
    peakGain,
    attack = 0.01,
    useCrackle = false,
  ) {
    const src = ctx.createBufferSource();
    src.buffer = useCrackle ? crackleNoiseBuffer : noiseBuffer;
    src.loop = true;
    const filter = ctx.createBiquadFilter();
    filter.type = filterType;
    filter.frequency.setValueAtTime(Math.max(40, freqStart), startTime);
    if (freqEnd !== freqStart) {
      filter.frequency.exponentialRampToValueAtTime(Math.max(40, freqEnd), startTime + duration);
    }
    filter.Q.value = q;

    const gain = ctx.createGain();
    gain.gain.setValueAtTime(0.0001, startTime);
    gain.gain.linearRampToValueAtTime(peakGain, startTime + Math.min(attack, duration * 0.45));
    gain.gain.exponentialRampToValueAtTime(0.0001, startTime + duration);

    src.connect(filter);
    filter.connect(gain);
    gain.connect(sfxBus);
    src.start(startTime, Math.random() * 1.4);
    src.stop(startTime + duration + 0.025);
  }

  /**
   * Authentic struck/ratcheted cast-iron modal bank: 6 inharmonic metallic partials with
   * initial impact pitch sag + metallic clang transient + 46Hz pawl gear ratchet scrape.
   */
  function triggerIronModalRing(
    startTime,
    duration,
    baseFreq,
    peakGain,
    decayScale = 1.0,
    includeRatchet = true,
  ) {
    const bus = ctx.createGain();
    bus.gain.value = peakGain;
    bus.connect(sfxBus);

    for (let m = 0; m < IRON_MODE_RATIOS.length; m += 1) {
      const f = Math.min(3200, Math.max(60, baseFreq * IRON_MODE_RATIOS[m]));
      const modeDur = Math.min(duration, Math.max(0.055, 3.8 / (IRON_MODE_DECAYS[m] * decayScale)));
      const osc = ctx.createOscillator();
      const g = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(f * 1.026, startTime);
      osc.frequency.exponentialRampToValueAtTime(f, startTime + 0.032);

      g.gain.setValueAtTime(0.0001, startTime);
      g.gain.linearRampToValueAtTime(IRON_MODE_AMPS[m] * 0.48, startTime + 0.003);
      g.gain.exponentialRampToValueAtTime(0.0001, startTime + modeDur);

      osc.connect(g);
      g.connect(bus);
      osc.start(startTime);
      osc.stop(startTime + modeDur + 0.02);
    }

    // Metallic strike burst
    const clangDur = Math.min(duration, 0.06);
    const clangSrc = ctx.createBufferSource();
    clangSrc.buffer = noiseBuffer;
    clangSrc.loop = true;
    const clangFilter = ctx.createBiquadFilter();
    clangFilter.type = 'bandpass';
    clangFilter.frequency.value = Math.min(2900, Math.max(500, baseFreq * 4.2));
    clangFilter.Q.value = 3.4;
    const clangGain = ctx.createGain();
    clangGain.gain.setValueAtTime(0.0001, startTime);
    clangGain.gain.linearRampToValueAtTime(0.52, startTime + 0.0025);
    clangGain.gain.exponentialRampToValueAtTime(0.0001, startTime + clangDur);
    clangSrc.connect(clangFilter);
    clangFilter.connect(clangGain);
    clangGain.connect(bus);
    clangSrc.start(startTime, Math.random() * 1.2);
    clangSrc.stop(startTime + clangDur + 0.02);

    // Cast-iron pawl ratchet grit (46Hz gear-tooth AM modulation)
    if (includeRatchet) {
      const rDur = Math.min(duration, 0.09);
      const rSrc = ctx.createBufferSource();
      rSrc.buffer = crackleNoiseBuffer;
      rSrc.loop = true;
      const rFilter = ctx.createBiquadFilter();
      rFilter.type = 'bandpass';
      const rStart = Math.min(2600, Math.max(420, baseFreq * 4.8));
      const rEnd = Math.min(1800, Math.max(320, baseFreq * 2.1));
      rFilter.frequency.setValueAtTime(rStart, startTime);
      rFilter.frequency.exponentialRampToValueAtTime(rEnd, startTime + rDur);
      rFilter.Q.value = 4.2;

      const rModGain = ctx.createGain();
      rModGain.gain.setValueAtTime(0.28, startTime);
      rModGain.gain.exponentialRampToValueAtTime(0.0001, startTime + rDur);

      const teethLfo = ctx.createOscillator();
      teethLfo.type = 'sine';
      teethLfo.frequency.value = 46;
      const teethDepth = ctx.createGain();
      teethDepth.gain.value = 0.18;
      teethLfo.connect(teethDepth);
      teethDepth.connect(rModGain.gain);

      rSrc.connect(rFilter);
      rFilter.connect(rModGain);
      rModGain.connect(bus);
      rSrc.start(startTime, Math.random() * 1.2);
      teethLfo.start(startTime);
      rSrc.stop(startTime + rDur + 0.02);
      teethLfo.stop(startTime + rDur + 0.02);
    }
  }

  /**
   * Fibrous sinew / tendon tearing: stochastic train of irregular snapping micro-fibers routed
   * through dual sweeping resonant formant bands (wet tissue cavity + taut fascia rasp), layered
   * over a stretched low-mid ligament groan with 29Hz fibrous flutter.
   */
  function triggerTendonTear(startTime, duration, pitch, peakGain, fiberCount = 18) {
    const out = ctx.createGain();
    out.gain.value = peakGain;
    out.connect(sfxBus);

    // Dual sweeping formant resonators shared by the ripping source & snapping fibers
    const ripBus = ctx.createGain();
    ripBus.gain.value = 1.0;

    const cavityFilter = ctx.createBiquadFilter();
    cavityFilter.type = 'bandpass';
    cavityFilter.Q.value = 4.6;
    cavityFilter.frequency.setValueAtTime(Math.max(180, 320 * pitch), startTime);
    cavityFilter.frequency.exponentialRampToValueAtTime(Math.min(950, 710 * pitch), startTime + duration);

    const fiberFilter = ctx.createBiquadFilter();
    fiberFilter.type = 'bandpass';
    fiberFilter.Q.value = 3.8;
    fiberFilter.frequency.setValueAtTime(Math.min(2900, 2140 * pitch), startTime);
    fiberFilter.frequency.exponentialRampToValueAtTime(Math.max(620, 1220 * pitch), startTime + duration);

    const wobbleLfo = ctx.createOscillator();
    wobbleLfo.type = 'sine';
    wobbleLfo.frequency.value = 11.5;
    const wobbleDepth1 = ctx.createGain();
    wobbleDepth1.gain.value = 85;
    const wobbleDepth2 = ctx.createGain();
    wobbleDepth2.gain.value = 150;
    wobbleLfo.connect(wobbleDepth1);
    wobbleLfo.connect(wobbleDepth2);
    wobbleDepth1.connect(cavityFilter.frequency);
    wobbleDepth2.connect(fiberFilter.frequency);

    const cavityGain = ctx.createGain();
    cavityGain.gain.value = 0.78;
    const fiberGain = ctx.createGain();
    fiberGain.gain.value = 0.56;

    ripBus.connect(cavityFilter);
    ripBus.connect(fiberFilter);
    cavityFilter.connect(cavityGain);
    fiberFilter.connect(fiberGain);
    cavityGain.connect(out);
    fiberGain.connect(out);

    // Continuous wet fibrous friction bed across the rip arc
    const bedSrc = ctx.createBufferSource();
    bedSrc.buffer = crackleNoiseBuffer;
    bedSrc.loop = true;
    const bedGain = ctx.createGain();
    bedGain.gain.setValueAtTime(0.0001, startTime);
    bedGain.gain.linearRampToValueAtTime(0.42, startTime + duration * 0.25);
    bedGain.gain.linearRampToValueAtTime(0.48, startTime + duration * 0.65);
    bedGain.gain.exponentialRampToValueAtTime(0.0001, startTime + duration);
    bedSrc.connect(bedGain);
    bedGain.connect(ripBus);
    bedSrc.start(startTime, Math.random() * 1.4);
    bedSrc.stop(startTime + duration + 0.03);

    // Stochastic irregular snapping tendon micro-fibers
    const snaps = Math.max(8, Math.min(28, fiberCount));
    for (let k = 0; k < snaps; k += 1) {
      const progress = k / snaps;
      const jitter = (Math.random() - 0.5) * (duration / snaps) * 1.4;
      const t = startTime + Math.max(0, Math.min(duration - 0.02, progress * duration * 0.88 + jitter));
      const snapDur = 0.016 + Math.random() * 0.012;
      const snapAmp = (0.38 + 0.48 * Math.sin(Math.PI * progress) + Math.random() * 0.25) * 0.42;

      const osc = ctx.createOscillator();
      osc.type = 'sawtooth';
      const f0 = (760 + Math.random() * 1350) * pitch;
      osc.frequency.setValueAtTime(f0, t);
      osc.frequency.exponentialRampToValueAtTime(Math.max(180, f0 * 0.36), t + snapDur);

      const sg = ctx.createGain();
      sg.gain.setValueAtTime(0.0001, t);
      sg.gain.linearRampToValueAtTime(snapAmp, t + 0.002);
      sg.gain.exponentialRampToValueAtTime(0.0001, t + snapDur);

      osc.connect(sg);
      sg.connect(ripBus);
      osc.start(t);
      osc.stop(t + snapDur + 0.01);
    }

    // Stretched elastic ligament groan pitching downward under tension with 29Hz flutter
    const groanOsc = ctx.createOscillator();
    groanOsc.type = 'triangle';
    groanOsc.frequency.setValueAtTime(Math.max(45, 195 * pitch), startTime);
    groanOsc.frequency.exponentialRampToValueAtTime(Math.max(38, 54 * pitch), startTime + duration);

    const groanLp = ctx.createBiquadFilter();
    groanLp.type = 'lowpass';
    groanLp.frequency.value = 340;

    const groanGain = ctx.createGain();
    groanGain.gain.setValueAtTime(0.0001, startTime);
    groanGain.gain.linearRampToValueAtTime(0.28, startTime + duration * 0.28);
    groanGain.gain.exponentialRampToValueAtTime(0.0001, startTime + duration);

    const flutterOsc = ctx.createOscillator();
    flutterOsc.type = 'sine';
    flutterOsc.frequency.value = 29;
    const flutterDepth = ctx.createGain();
    flutterDepth.gain.value = 0.09;
    flutterOsc.connect(flutterDepth);
    flutterDepth.connect(groanGain.gain);

    groanOsc.connect(groanLp);
    groanLp.connect(groanGain);
    groanGain.connect(out);

    wobbleLfo.start(startTime);
    wobbleLfo.stop(startTime + duration + 0.03);
    groanOsc.start(startTime);
    groanOsc.stop(startTime + duration + 0.03);
    flutterOsc.start(startTime);
    flutterOsc.stop(startTime + duration + 0.03);
  }

  /**
   * Thick, wet, organic mucosa squelch using crackle-injected wet noise routed through dual
   * LFO-swept vocal/cavity formant bandpass filters + slow fleshy pressure sub-swell.
   */
  function triggerWetSquelch(
    startTime,
    duration,
    pitch,
    peakGain,
    formantStart = 240,
    formantEnd = 640,
    wobbleHz = 7.5,
  ) {
    const out = ctx.createGain();
    out.gain.value = peakGain;
    out.connect(sfxBus);

    const src = ctx.createBufferSource();
    src.buffer = crackleNoiseBuffer;
    src.loop = true;

    const f1 = ctx.createBiquadFilter();
    f1.type = 'bandpass';
    f1.Q.value = 5.8;
    const f1Start = Math.max(110, Math.min(1100, formantStart * pitch));
    const f1End = Math.max(110, Math.min(1100, formantEnd * pitch));
    f1.frequency.setValueAtTime(f1Start, startTime);
    f1.frequency.exponentialRampToValueAtTime(f1End, startTime + duration);

    const f2 = ctx.createBiquadFilter();
    f2.type = 'bandpass';
    f2.Q.value = 6.4;
    f2.frequency.setValueAtTime(Math.min(2600, f1Start * 2.15), startTime);
    f2.frequency.exponentialRampToValueAtTime(Math.min(2600, f1End * 2.15), startTime + duration);

    const lfo1 = ctx.createOscillator();
    lfo1.type = 'sine';
    lfo1.frequency.value = wobbleHz;
    const lfo1Depth = ctx.createGain();
    lfo1Depth.gain.value = Math.max(40, (f1Start + f1End) * 0.18);
    lfo1.connect(lfo1Depth);
    lfo1Depth.connect(f1.frequency);

    const lfo2 = ctx.createOscillator();
    lfo2.type = 'sine';
    lfo2.frequency.value = wobbleHz * 1.35;
    const lfo2Depth = ctx.createGain();
    lfo2Depth.gain.value = 140;
    lfo2.connect(lfo2Depth);
    lfo2Depth.connect(f2.frequency);

    const envGain = ctx.createGain();
    envGain.gain.setValueAtTime(0.0001, startTime);
    envGain.gain.linearRampToValueAtTime(0.75, startTime + Math.min(0.06, duration * 0.32));
    envGain.gain.linearRampToValueAtTime(0.62, startTime + duration * 0.68);
    envGain.gain.exponentialRampToValueAtTime(0.0001, startTime + duration);

    const f2Mix = ctx.createGain();
    f2Mix.gain.value = 0.56;

    src.connect(f1);
    src.connect(f2);
    f1.connect(envGain);
    f2.connect(f2Mix);
    f2Mix.connect(envGain);
    envGain.connect(out);

    // Fleshy low-end body pressure swell
    const subOsc = ctx.createOscillator();
    subOsc.type = 'sine';
    subOsc.frequency.setValueAtTime(Math.max(36, 78 * pitch), startTime);
    subOsc.frequency.exponentialRampToValueAtTime(Math.max(30, 52 * pitch), startTime + duration);
    const subGain = ctx.createGain();
    subGain.gain.setValueAtTime(0.0001, startTime);
    subGain.gain.linearRampToValueAtTime(0.32, startTime + duration * 0.35);
    subGain.gain.exponentialRampToValueAtTime(0.0001, startTime + duration);
    subOsc.connect(subGain);
    subGain.connect(out);

    src.start(startTime, Math.random() * 1.4);
    lfo1.start(startTime);
    lfo2.start(startTime);
    subOsc.start(startTime);
    src.stop(startTime + duration + 0.03);
    lfo1.stop(startTime + duration + 0.03);
    lfo2.stop(startTime + duration + 0.03);
    subOsc.stop(startTime + duration + 0.03);
  }

  /**
   * Staggered cluster of wet cavity bubble pops (soft attack, upward pitch chirp) selling thick
   * liquid mucosa, blister rupture, and submerged suction.
   */
  function triggerBubbleCluster(startTime, span, count, baseFreq, peakGain) {
    for (let b = 0; b < count; b += 1) {
      const t = startTime + Math.random() * Math.max(0.01, span);
      const dur = 0.028 + Math.random() * 0.032;
      const f0 = Math.max(85, baseFreq * (0.68 + Math.random() * 0.78));
      const amp = peakGain * (0.55 + Math.random() * 0.55);

      const osc = ctx.createOscillator();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(f0, t);
      osc.frequency.exponentialRampToValueAtTime(Math.min(1200, f0 * 2.05), t + dur);

      const g = ctx.createGain();
      g.gain.setValueAtTime(0.0001, t);
      g.gain.linearRampToValueAtTime(amp, t + dur * 0.28);
      g.gain.exponentialRampToValueAtTime(0.0001, t + dur);

      osc.connect(g);
      g.connect(sfxBus);
      osc.start(t);
      osc.stop(t + dur + 0.015);
    }
  }

  function renderCue(cue, phase, pitch) {
    if (!ctx || !sfxBus || !noiseBuffer || !crackleNoiseBuffer) return;
    const now = ctx.currentTime + 0.003;
    const p = Math.max(0, Math.min(6, phase ?? 0));
    const pDark = 1.0 - Math.min(4, p) * 0.035;
    const darkPitch = pitch * pDark;

    switch (cue) {
      case SOUND_CUES.VALVE_PRESS:
        // Heavy wet cast-iron wall valve crank: 6-mode iron clang + pawl ratchet + tight housing weight
        triggerIronModalRing(now, 0.25, 285 * darkPitch, 0.78, 0.95, true);
        triggerTone(now, 0.14, 88 * darkPitch, 44 * darkPitch, 0.28, 'sine', 0.004);
        triggerIronModalRing(now + 0.042, 0.16, 365 * darkPitch, 0.38, 1.28, false);
        if (p >= 1) {
          const wet = Math.max(0.25, Math.min(1.0, p / 4));
          triggerWetSquelch(now + 0.055, 0.17, darkPitch, 0.28 * wet, 340, 180, 12.0);
          triggerBubbleCluster(now + 0.065, 0.12, 2 + Math.min(4, p), 190 * darkPitch, 0.13 * wet);
        }
        break;

      case SOUND_CUES.CREATURE_BUY_SUTURE_HAND:
        // Wet fascia tearing loose from plaster + tendons snapping + cold iron rim grip
        triggerTendonTear(now, 0.26, darkPitch, 0.62, 16);
        triggerWetSquelch(now + 0.03, 0.24, darkPitch, 0.34, 260, 520, 8.0);
        triggerIronModalRing(now + 0.165, 0.22, 265 * darkPitch, 0.42, 1.35, false);
        break;

      case SOUND_CUES.CREATURE_BUY_LEECH_CLERK:
        // Long viscous wet mucosal squelch + irregular suction-cup bubbles dragging on cold glass
        triggerWetSquelch(now, 0.42, darkPitch, 0.72, 220, 620, 8.5);
        triggerBubbleCluster(now + 0.04, 0.32, 7, 185 * darkPitch, 0.24);
        break;

      case SOUND_CUES.CREATURE_BUY_WEEPING_LAMP:
        // Eerie hollow thick-glass vessel toll + slow heavy viscous condensation drops
        triggerTone(now, 0.46, 182 * darkPitch, 179 * darkPitch, 0.30, 'sine', 0.015);
        triggerTone(now, 0.36, 346 * darkPitch, 340 * darkPitch, 0.15, 'sine', 0.014);
        triggerTone(now, 0.26, 588 * darkPitch, 578 * darkPitch, 0.08, 'sine', 0.012);
        triggerTone(now, 0.44, 91 * darkPitch, 86 * darkPitch, 0.22, 'sine', 0.018);
        triggerBubbleCluster(now + 0.14, 0.26, 4, 195 * darkPitch, 0.20);
        break;

      case SOUND_CUES.CREATURE_BUY_SUMP_SIPHON:
        // Cavernous wet bellows inhale + submerged bubbling iron-pipe gulp + iron seat clank
        triggerWetSquelch(now, 0.48, darkPitch, 0.68, 420, 150, 6.2);
        triggerBubbleCluster(now + 0.05, 0.38, 8, 140 * darkPitch, 0.26);
        triggerIronModalRing(now + 0.28, 0.24, 175 * darkPitch, 0.36, 1.3, false);
        break;

      case SOUND_CUES.ANOMALY_TOOTH:
        // Taut periodontal ligament tearing out of wet plaster + enamel root wrench pop
        triggerTendonTear(now, 0.28, pitch, 0.72, 18);
        triggerWetSquelch(now + 0.08, 0.22, pitch, 0.38, 380, 190, 8.5);
        triggerBubbleCluster(now + 0.16, 0.12, 4, 230 * pitch, 0.20);
        break;

      case SOUND_CUES.ANOMALY_EYE:
        // Damp rubbery condensation smear + wet mucosal eyelid squelch + hollow sphere roll
        triggerWetSquelch(now, 0.34, pitch, 0.68, 290, 780, 6.0);
        triggerBubbleCluster(now + 0.14, 0.20, 5, 210 * pitch, 0.20);
        break;

      case SOUND_CUES.ANOMALY_NODULE:
        // Taut blister membrane tearing open + juicy oozing wet squelch & bubble burst
        triggerTendonTear(now, 0.16, pitch, 0.50, 12);
        triggerWetSquelch(now + 0.04, 0.38, pitch, 0.76, 480, 175, 9.5);
        triggerBubbleCluster(now + 0.07, 0.30, 7, 175 * pitch, 0.26);
        break;

      case SOUND_CUES.ANOMALY_RIB:
        // Deep calcified bone wrench + long fibrous tendon/ligament ripping + marrow groan
        triggerTendonTear(now, 0.44, pitch * 0.92, 0.78, 24);
        triggerWetSquelch(now + 0.10, 0.36, pitch, 0.46, 280, 540, 6.5);
        triggerBubbleCluster(now + 0.18, 0.24, 5, 155 * pitch, 0.20);
        break;

      case SOUND_CUES.JAR_BOTTLE:
        // Dark thick-glass vessel resonance + viscous suction wax-cork seal
        triggerTone(now, 0.28, 248 * pitch, 244 * pitch, 0.24, 'sine', 0.008);
        triggerTone(now, 0.22, 412 * pitch, 405 * pitch, 0.12, 'sine', 0.008);
        triggerTone(now, 0.28, 124 * pitch, 118 * pitch, 0.18, 'sine', 0.01);
        triggerWetSquelch(now + 0.05, 0.26, pitch, 0.54, 360, 165, 8.0);
        triggerBubbleCluster(now + 0.08, 0.18, 4, 170 * pitch, 0.22);
        break;

      case SOUND_CUES.GRAFT_PERFORM:
        // Long (~1.15s) deeply organic & grotesque grafting ritual:
        // peristaltic body swell + 2 overlapping tendon tears + 2 mucosa squelches + 11 bubbles
        triggerTone(now, 1.12, 60 * pitch, 33 * pitch, 0.34, 'sine', 0.16);
        triggerTendonTear(now + 0.05, 0.48, pitch * 0.96, 0.66, 24);
        triggerTendonTear(now + 0.34, 0.52, pitch * 0.88, 0.72, 26);
        triggerWetSquelch(now + 0.02, 0.72, pitch, 0.66, 220, 590, 7.2);
        triggerWetSquelch(now + 0.52, 0.58, pitch * 0.92, 0.60, 440, 155, 5.4);
        triggerBubbleCluster(now + 0.16, 0.74, 11, 165 * pitch, 0.24);
        break;

      case SOUND_CUES.SCAR_UNLOCK:
        // Surgical thread dragging taut through damp flesh + crisp tendon cinch
        triggerTendonTear(now, 0.30, pitch * 1.05, 0.64, 15);
        triggerWetSquelch(now + 0.06, 0.26, pitch, 0.42, 520, 220, 9.0);
        break;

      case SOUND_CUES.DRAIN_TOGGLE:
        // Heavy rusted cast-iron floor sump grate latch slam & metallic ring
        triggerIronModalRing(now, 0.32, 205 * pitch, 0.72, 0.92, true);
        triggerIronModalRing(now + 0.048, 0.22, 290 * pitch, 0.32, 1.25, false);
        break;

      case SOUND_CUES.SUMP_DREDGE:
        // Submerged rusted iron chain-link haul & dark viscous sump water slosh
        triggerWetSquelch(now, 0.50, pitch, 0.58, 280, 460, 6.8);
        triggerBubbleCluster(now + 0.04, 0.42, 8, 160 * pitch, 0.22);
        triggerIronModalRing(now + 0.02, 0.20, 320 * pitch, 0.38, 1.4, false);
        triggerIronModalRing(now + 0.16, 0.24, 255 * pitch, 0.42, 1.2, false);
        break;

      case SOUND_CUES.ECHO_PULSE:
        // Deeply muffled triple knuckle-knock behind thick wet masonry
        for (let i = 0; i < 3; i += 1) {
          const t = now + i * 0.16;
          const amp = 0.30 * (1 - i * 0.2);
          triggerTone(t, 0.12, (96 - i * 7) * pitch, 44 * pitch, amp, 'sine', 0.005);
          triggerNoise(t, 0.09, 'lowpass', 240, 100, 1.1, amp * 0.65, 0.005);
        }
        break;

      case SOUND_CUES.CLAMP_ENGAGE:
        // Massive iron jaws bolting shut over the valve with cold metallic locking ring
        triggerIronModalRing(now, 0.40, 185 * pitch, 0.74, 0.85, true);
        triggerIronModalRing(now + 0.095, 0.30, 278 * pitch, 0.48, 1.05, false);
        triggerTone(now, 0.30, 72 * pitch, 34 * pitch, 0.42, 'sine', 0.004);
        break;

      case SOUND_CUES.MARROW_BLOOM:
        // Iron clamp release snapping open into a cavernous tearing marrow & wet tissue bloom
        triggerIronModalRing(now, 0.28, 225 * pitch, 0.48, 1.15, true);
        triggerTendonTear(now + 0.04, 0.48, pitch * 0.90, 0.66, 22);
        triggerWetSquelch(now + 0.08, 0.64, pitch, 0.68, 520, 150, 6.5);
        triggerBubbleCluster(now + 0.14, 0.52, 9, 160 * pitch, 0.22);
        break;

      case SOUND_CUES.THROAT_OFFER:
        // Cavernous wet esophageal swallow + mucosal squelch + calcified tooth settle
        triggerWetSquelch(now, 0.52, pitch * 0.92, 0.74, 460, 140, 5.5);
        triggerBubbleCluster(now + 0.08, 0.38, 7, 145 * pitch, 0.24);
        triggerTendonTear(now + 0.22, 0.24, pitch * 0.86, 0.40, 12);
        break;

      case SOUND_CUES.EXCISION_CUT:
        // Heavy cold surgical iron shear slicing straight through thick sinew into hollow collapse
        triggerIronModalRing(now, 0.46, 265 * pitch, 0.64, 0.82, true);
        triggerTendonTear(now + 0.04, 0.56, pitch * 0.88, 0.74, 26);
        triggerWetSquelch(now + 0.18, 0.66, pitch * 0.86, 0.54, 380, 120, 4.8);
        triggerTone(now + 0.04, 0.82, 72 * pitch, 29 * pitch, 0.42, 'sine', 0.02);
        break;

      case SOUND_CUES.PLEAD_SALVATION:
        // Low mournful damp seam sigh dissolving into muffled exterior rain
        triggerTone(now, 0.94, 54 * pitch, 46 * pitch, 0.34, 'sine', 0.12);
        triggerTone(now + 0.06, 0.88, 81 * pitch, 72 * pitch, 0.18, 'sine', 0.14);
        triggerNoise(now + 0.10, 0.84, 'bandpass', 620, 320, 0.9, 0.26, 0.14);
        break;

      case SOUND_CUES.STORY_DRIP:
        // Quiet low-register cistern condensation drop announcing a newly unlocked Story fragment
        triggerBubbleCluster(now, 0.02, 1, 225 * pitch, 0.20);
        triggerTone(now + 0.05, 0.18, 122 * pitch, 110 * pitch, 0.10, 'sine', 0.01);
        break;

      case SOUND_CUES.PHASE_SHIFT:
        // Deep brooding architectural groan + wet masonry shift
        triggerWetSquelch(now + 0.06, 0.62, pitch * 0.85, 0.38, 180, 310, 3.5);
        triggerTone(now, 0.76, 46 * pitch, 36 * pitch, 0.36, 'sine', 0.10);
        triggerTone(now, 0.68, 69 * pitch, 54 * pitch, 0.18, 'triangle', 0.10);
        break;

      case SOUND_CUES.ARC2_STAMP:
        // Heavy muffled wooden desk stamp + dark blotter slap
        triggerTone(now, 0.14, 98 * pitch, 44 * pitch, 0.38, 'sine', 0.003);
        triggerNoise(now, 0.10, 'lowpass', 520 * pitch, 220 * pitch, 1.3, 0.22, 0.003);
        break;

      case SOUND_CUES.ARC2_CONTACT:
        // Heavy damp manila folder slide + low desk drawer tap
        triggerNoise(now, 0.22, 'bandpass', 680, 300, 1.8, 0.24, 0.015);
        triggerTone(now + 0.05, 0.13, 142 * pitch, 72 * pitch, 0.24, 'triangle', 0.005);
        break;

      case SOUND_CUES.ARC2_DEDUCTION:
        // Corkboard pin press + dark damped bass red-string pluck
        triggerTone(now, 0.07, 290 * pitch, 110 * pitch, 0.18, 'triangle', 0.003);
        triggerTone(now + 0.02, 0.24, 158 * pitch, 92 * pitch, 0.26, 'sine', 0.006);
        break;

      case SOUND_CUES.ARC2_DESK_ANOMALY:
        // Damp envelope slit / muted brass reel latch
        triggerTendonTear(now, 0.22, pitch * 1.12, 0.44, 12);
        triggerIronModalRing(now + 0.04, 0.18, 340 * pitch, 0.24, 1.6, false);
        break;

      case SOUND_CUES.ARC2_CHAPTER:
        // Heavy case-file slam
        triggerTone(now, 0.32, 76 * pitch, 38 * pitch, 0.38, 'sine', 0.005);
        triggerNoise(now + 0.02, 0.24, 'lowpass', 440, 180, 1.2, 0.24, 0.006);
        break;

      case SOUND_CUES.ARC2_FILE_ZERO:
        // Heavy manila dossier flap + cold subterranean grout bleed
        triggerWetSquelch(now + 0.08, 0.74, pitch * 0.82, 0.34, 220, 135, 3.2);
        triggerTone(now + 0.04, 0.88, 42 * pitch, 34 * pitch, 0.42, 'sine', 0.06);
        triggerTone(now + 0.12, 0.72, 63 * pitch, 52 * pitch, 0.20, 'sine', 0.06);
        break;

      case SOUND_CUES.ARC3_ENTER:
        // Dual subterranean convergence drone
        triggerTone(now, 0.84, 50 * pitch, 38 * pitch, 0.36, 'sine', 0.10);
        triggerTone(now + 0.04, 0.76, 75 * pitch, 58 * pitch, 0.18, 'sine', 0.10);
        break;

      default:
        break;
    }
  }

  function unlock() {
    const audioCtx = ensureContext();
    if (!audioCtx) return;
    if (!muted && foregroundActive && audioCtx.state === 'suspended') {
      audioCtx.resume().then(() => applyAmbientTargets(currentProfile)).catch(() => {});
    } else {
      applyAmbientTargets(currentProfile);
    }
  }

  function playCues(cues, phase = 0) {
    if (!cues || cues.length === 0 || muted || !foregroundActive) return;
    if (!ctx || ctx.state !== 'running') return;
    const nowMs = Date.now();
    for (const cue of cues) {
      const minGap = CUE_MIN_INTERVAL_MS[cue] ?? 55;
      const prevMs = lastPlayedMs.get(cue) ?? 0;
      if (nowMs - prevMs < minGap) continue;
      lastPlayedMs.set(cue, nowMs);
      const wideJitter =
        cue === SOUND_CUES.VALVE_PRESS ||
        cue === SOUND_CUES.ARC2_STAMP ||
        cue === SOUND_CUES.CREATURE_BUY_SUTURE_HAND ||
        cue === SOUND_CUES.CREATURE_BUY_LEECH_CLERK ||
        cue === SOUND_CUES.CREATURE_BUY_WEEPING_LAMP ||
        cue === SOUND_CUES.CREATURE_BUY_SUMP_SIPHON;
      const pitch = wideJitter ? 0.93 + Math.random() * 0.14 : 0.97 + Math.random() * 0.06;
      try {
        renderCue(cue, phase, pitch);
      } catch {
        // Never allow Web Audio scheduling errors to interrupt gameplay.
      }
    }
  }

  function updateAmbience(profile) {
    currentProfile = profile;
    applyAmbientTargets(profile);
  }

  function setMuted(nextMuted) {
    const flag = Boolean(nextMuted);
    if (muted === flag) return muted;
    muted = flag;
    writeMutedPreference(muted, storage);
    if (!muted) {
      unlock();
    }
    syncMasterState();
    return muted;
  }

  function toggleMuted() {
    return setMuted(!muted);
  }

  function setForegroundActive(active) {
    foregroundActive = Boolean(active);
    syncMasterState();
  }

  function isMuted() {
    return muted;
  }

  function dispose() {
    if (ambientTimer != null && typeof window !== 'undefined') {
      window.clearInterval(ambientTimer);
      ambientTimer = null;
    }
    if (ctx) {
      ctx.close().catch(() => {});
      ctx = null;
      ambientNodes = null;
    }
  }

  return {
    unlock,
    playCues,
    updateAmbience,
    setMuted,
    toggleMuted,
    setForegroundActive,
    isMuted,
    dispose,
  };
}
