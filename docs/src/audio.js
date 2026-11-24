/**
 * Zero-asset Web Audio procedural foley & phase-escalating room ambience engine for
 * "The Tending of the Unnamed Organ" (1:1 parity with Android SoundCueResolver + ProceduralFoleySynth).
 *
 * Acoustic identity: damp mechanical iron, wet seepage, enamel snaps, organic clicks, and quiet
 * fluorescent ballast hum — never arcade tones, melodic fanfare, or jumpscares.
 */

import { DESK_ANOMALY_TYPES, GAME_ARCS, WALL_ANOMALY_TYPES } from './engine.js?v=2.9';

export const AUDIO_MUTE_STORAGE_KEY = 'unnamed_organ_audio_muted_v1';

export const SOUND_CUES = Object.freeze({
  VALVE_PRESS: 'valve_press',
  CREATURE_BUY: 'creature_buy',
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
  [SOUND_CUES.CREATURE_BUY]: 80,
  [SOUND_CUES.ANOMALY_TOOTH]: 90,
  [SOUND_CUES.ANOMALY_EYE]: 110,
  [SOUND_CUES.ANOMALY_NODULE]: 110,
  [SOUND_CUES.ANOMALY_RIB]: 120,
  [SOUND_CUES.JAR_BOTTLE]: 120,
  [SOUND_CUES.GRAFT_PERFORM]: 180,
  [SOUND_CUES.SCAR_UNLOCK]: 110,
  [SOUND_CUES.DRAIN_TOGGLE]: 110,
  [SOUND_CUES.SUMP_DREDGE]: 180,
  [SOUND_CUES.ECHO_PULSE]: 260,
  [SOUND_CUES.CLAMP_ENGAGE]: 160,
  [SOUND_CUES.MARROW_BLOOM]: 300,
  [SOUND_CUES.THROAT_OFFER]: 200,
  [SOUND_CUES.EXCISION_CUT]: 400,
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
    case SOUND_ACTION_HINTS.CREATURE_BUY:
      if (mapsDiffer(prev.creatures, next.creatures)) {
        cues.push(SOUND_CUES.CREATURE_BUY);
      }
      break;
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
      b1 = 0.92 * b1 + white * 0.25;
      data[i] = Math.max(-1, Math.min(1, white * 0.55 + b1 * 0.35 + b0 * 0.2));
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
      compressor.attack.value = 0.005;
      compressor.release.value = 0.14;

      masterGain = ctx.createGain();
      masterGain.gain.value = muted || !foregroundActive ? 0.0001 : 0.85;

      sfxBus = ctx.createGain();
      sfxBus.gain.value = 0.72;

      ambientBus = ctx.createGain();
      ambientBus.gain.value = 0.28;

      sfxBus.connect(compressor);
      ambientBus.connect(compressor);
      compressor.connect(masterGain);
      masterGain.connect(ctx.destination);

      noiseBuffer = createNoiseBuffer(ctx);
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

    // 1. Fluorescent ballast 60Hz + 120Hz hum
    const humOsc1 = ctx.createOscillator();
    humOsc1.type = 'sine';
    humOsc1.frequency.value = 59.6;

    const humOsc2 = ctx.createOscillator();
    humOsc2.type = 'triangle';
    humOsc2.frequency.value = 119.2;

    const humFilter = ctx.createBiquadFilter();
    humFilter.type = 'lowpass';
    humFilter.frequency.value = 210;

    const humGainNode = ctx.createGain();
    humGainNode.gain.value = 0.0;

    const humOverGain = ctx.createGain();
    humOverGain.gain.value = 0.24;

    humOsc1.connect(humFilter);
    humOsc2.connect(humOverGain);
    humOverGain.connect(humFilter);
    humFilter.connect(humGainNode);
    humGainNode.connect(ambientBus);

    humOsc1.start();
    humOsc2.start();

    // 2. Damp flesh / peristalsis low-mid noise bed with slow LFO modulation
    const fleshNoise = ctx.createBufferSource();
    fleshNoise.buffer = noiseBuffer;
    fleshNoise.loop = true;

    const fleshFilter = ctx.createBiquadFilter();
    fleshFilter.type = 'bandpass';
    fleshFilter.frequency.value = 135;
    fleshFilter.Q.value = 1.6;

    const fleshLfo = ctx.createOscillator();
    fleshLfo.type = 'sine';
    fleshLfo.frequency.value = 0.16;
    const fleshLfoDepth = ctx.createGain();
    fleshLfoDepth.gain.value = 42;
    fleshLfo.connect(fleshLfoDepth);
    fleshLfoDepth.connect(fleshFilter.frequency);

    const fleshGainNode = ctx.createGain();
    fleshGainNode.gain.value = 0.0;

    fleshNoise.connect(fleshFilter);
    fleshFilter.connect(fleshGainNode);
    fleshGainNode.connect(ambientBus);

    fleshNoise.start();
    fleshLfo.start();

    // 3. Arc 2 precinct window rain hiss
    const rainNoise = ctx.createBufferSource();
    rainNoise.buffer = noiseBuffer;
    rainNoise.loop = true;

    const rainFilter = ctx.createBiquadFilter();
    rainFilter.type = 'bandpass';
    rainFilter.frequency.value = 1650;
    rainFilter.Q.value = 0.7;

    const rainGainNode = ctx.createGain();
    rainGainNode.gain.value = 0.0;

    rainNoise.connect(rainFilter);
    rainFilter.connect(rainGainNode);
    rainGainNode.connect(ambientBus);
    rainNoise.start();

    // 4. Valve Clamp iron strain drone
    const clampOsc = ctx.createOscillator();
    clampOsc.type = 'sawtooth';
    clampOsc.frequency.value = 78;
    const clampFilter = ctx.createBiquadFilter();
    clampFilter.type = 'bandpass';
    clampFilter.frequency.value = 210;
    clampFilter.Q.value = 4.5;
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
    ambientNodes.humGainNode.gain.setTargetAtTime(profile.humGain * 0.24 * scale, now, 0.35);
    ambientNodes.fleshGainNode.gain.setTargetAtTime(
      (profile.fleshDepth * 0.24 + profile.complicityBleed * 0.08) * scale,
      now,
      0.45,
    );
    ambientNodes.rainGainNode.gain.setTargetAtTime(profile.rainMix * 0.16 * scale, now, 0.45);
    ambientNodes.clampGainNode.gain.setTargetAtTime(profile.clampTension * 0.11 * scale, now, 0.25);
  }

  function spawnAmbientDrip(now, wetness) {
    if (!ctx || !ambientBus) return;
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    const base = 760 + Math.random() * 520;
    osc.type = 'sine';
    osc.frequency.setValueAtTime(base * 0.72, now);
    osc.frequency.exponentialRampToValueAtTime(base * 1.48, now + 0.055);
    gain.gain.setValueAtTime(0.0001, now);
    gain.gain.linearRampToValueAtTime(0.11 * (0.65 + wetness * 0.5), now + 0.006);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.095);
    osc.connect(gain);
    gain.connect(ambientBus);
    osc.start(now);
    osc.stop(now + 0.11);
  }

  function spawnAmbientCardiac(now, strength) {
    if (!ctx || !ambientBus) return;
    const beats = [
      { offset: 0, freq: 56, amp: 0.28 * strength },
      { offset: 0.24, freq: 48, amp: 0.17 * strength },
    ];
    for (const b of beats) {
      const t = now + b.offset;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(b.freq * 1.12, t);
      osc.frequency.exponentialRampToValueAtTime(b.freq * 0.75, t + 0.17);
      gain.gain.setValueAtTime(0.0001, t);
      gain.gain.linearRampToValueAtTime(b.amp, t + 0.025);
      gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.22);
      osc.connect(gain);
      gain.connect(ambientBus);
      osc.start(t);
      osc.stop(t + 0.24);
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
        const gap = Math.max(0.75, 4.4 - currentProfile.dripRate * 3.1) * (0.65 + Math.random() * 0.75);
        nextDripAt = now + gap;
      }
    }, 240);
  }

  function syncMasterState() {
    if (!ctx || !masterGain) return;
    const active = !muted && foregroundActive;
    const now = ctx.currentTime;
    masterGain.gain.setTargetAtTime(active ? 0.85 : 0.0001, now, 0.04);
    applyAmbientTargets(currentProfile);
    if (!active && ctx.state === 'running') {
      ctx.suspend().catch(() => {});
    } else if (active && ctx.state === 'suspended') {
      ctx.resume().catch(() => {});
    }
  }

  // --- Low-level Web Audio synthesis helpers ---
  function triggerTone(
    startTime,
    duration,
    freqStart,
    freqEnd,
    peakGain,
    type = 'sine',
    attack = 0.006,
  ) {
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = type;
    osc.frequency.setValueAtTime(Math.max(20, freqStart), startTime);
    if (freqEnd !== freqStart) {
      osc.frequency.exponentialRampToValueAtTime(Math.max(20, freqEnd), startTime + duration);
    }
    gain.gain.setValueAtTime(0.0001, startTime);
    gain.gain.linearRampToValueAtTime(peakGain, startTime + Math.min(attack, duration * 0.4));
    gain.gain.exponentialRampToValueAtTime(0.0001, startTime + duration);
    osc.connect(gain);
    gain.connect(sfxBus);
    osc.start(startTime);
    osc.stop(startTime + duration + 0.015);
  }

  function triggerNoise(
    startTime,
    duration,
    filterType,
    freqStart,
    freqEnd,
    q,
    peakGain,
    attack = 0.005,
  ) {
    const src = ctx.createBufferSource();
    src.buffer = noiseBuffer;
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
    gain.gain.linearRampToValueAtTime(peakGain, startTime + Math.min(attack, duration * 0.4));
    gain.gain.exponentialRampToValueAtTime(0.0001, startTime + duration);

    src.connect(filter);
    filter.connect(gain);
    gain.connect(sfxBus);
    const offset = Math.random() * 1.4;
    src.start(startTime, offset);
    src.stop(startTime + duration + 0.015);
  }

  function renderCue(cue, phase, pitch) {
    if (!ctx || !sfxBus || !noiseBuffer) return;
    const now = ctx.currentTime + 0.003;
    const p = Math.max(0, Math.min(6, phase ?? 0));
    const wetness = Math.min(1, p / 4);

    switch (cue) {
      case SOUND_CUES.VALVE_PRESS:
        // Heavy recessed cast-iron valve click + damp pipe clank + phase-scaled seepage
        triggerTone(now, 0.14, 152 * pitch, 72 * pitch, 0.38, 'triangle', 0.003);
        triggerTone(now, 0.08, 410 * pitch, 240 * pitch, 0.16, 'sine', 0.002);
        triggerNoise(now, 0.075, 'bandpass', 1800 * pitch, 900 * pitch, 2.6, 0.22, 0.002);
        if (wetness > 0.08) {
          triggerNoise(
            now + 0.025,
            0.12,
            'bandpass',
            560 * pitch,
            240 * pitch,
            3.2,
            0.15 * wetness,
            0.012,
          );
        }
        break;

      case SOUND_CUES.CREATURE_BUY:
        // Wet chitin click + flesh settling
        triggerNoise(now, 0.07, 'bandpass', 1450 * pitch, 680 * pitch, 3.4, 0.24, 0.003);
        triggerTone(now + 0.015, 0.17, 195 * pitch, 82 * pitch, 0.26, 'sine', 0.01);
        triggerNoise(now + 0.055, 0.13, 'bandpass', 480 * pitch, 220 * pitch, 2.2, 0.16, 0.018);
        break;

      case SOUND_CUES.ANOMALY_TOOTH:
        // Dry enamel snap + mortar crunch
        triggerTone(now, 0.038, 1950 * pitch, 640 * pitch, 0.26, 'triangle', 0.001);
        triggerNoise(now, 0.06, 'highpass', 1750, 1100, 1.4, 0.26, 0.001);
        triggerNoise(now + 0.035, 0.11, 'bandpass', 780, 320, 2.1, 0.18, 0.008);
        break;

      case SOUND_CUES.ANOMALY_EYE:
        // Wet glass wipe + soft resonant glass ping
        triggerNoise(now, 0.16, 'bandpass', 880 * pitch, 1950 * pitch, 2.8, 0.18, 0.02);
        triggerTone(now + 0.065, 0.19, 1340 * pitch, 1290 * pitch, 0.11, 'sine', 0.005);
        break;

      case SOUND_CUES.ANOMALY_NODULE:
        // Swollen blister pop + warm seepage
        triggerTone(now, 0.095, 290 * pitch, 105 * pitch, 0.34, 'sine', 0.003);
        triggerNoise(now + 0.012, 0.22, 'bandpass', 820, 260, 2.0, 0.22, 0.01);
        break;

      case SOUND_CUES.ANOMALY_RIB:
        // Calcified rib crack + hollow chamber surge
        triggerTone(now, 0.05, 1180 * pitch, 340 * pitch, 0.26, 'sawtooth', 0.001);
        triggerNoise(now, 0.085, 'bandpass', 1350, 520, 2.2, 0.26, 0.002);
        triggerTone(now + 0.04, 0.28, 135 * pitch, 62 * pitch, 0.3, 'triangle', 0.014);
        break;

      case SOUND_CUES.JAR_BOTTLE:
        // Hollow glass jar corking + wax seal thump
        triggerTone(now, 0.12, 540 * pitch, 670 * pitch, 0.2, 'sine', 0.004);
        triggerTone(now + 0.04, 0.18, 820 * pitch, 790 * pitch, 0.1, 'sine', 0.006);
        triggerNoise(now + 0.06, 0.09, 'lowpass', 480, 180, 1.2, 0.22, 0.005);
        break;

      case SOUND_CUES.GRAFT_PERFORM:
        // Tight braided sinew suture + damp chordal settling
        triggerNoise(now, 0.18, 'bandpass', 1100, 420, 3.0, 0.24, 0.012);
        triggerTone(now + 0.05, 0.34, 115 * pitch, 78 * pitch, 0.32, 'triangle', 0.025);
        triggerTone(now + 0.1, 0.26, 172 * pitch, 138 * pitch, 0.16, 'sine', 0.03);
        break;

      case SOUND_CUES.SCAR_UNLOCK:
        // Surgical thread pull + bone click
        triggerNoise(now, 0.11, 'bandpass', 2200 * pitch, 1050 * pitch, 3.6, 0.22, 0.004);
        triggerTone(now + 0.045, 0.14, 340 * pitch, 145 * pitch, 0.24, 'triangle', 0.004);
        break;

      case SOUND_CUES.DRAIN_TOGGLE:
        // Heavy cast-iron grate latch clank
        triggerTone(now, 0.15, 225 * pitch, 118 * pitch, 0.34, 'triangle', 0.002);
        triggerTone(now, 0.11, 610 * pitch, 340 * pitch, 0.14, 'square', 0.002);
        triggerNoise(now, 0.1, 'bandpass', 1100, 480, 2.4, 0.22, 0.003);
        break;

      case SOUND_CUES.SUMP_DREDGE:
        // Chain haul + wet relic surfacing
        triggerNoise(now, 0.26, 'bandpass', 620, 260, 1.8, 0.24, 0.02);
        triggerTone(now + 0.05, 0.28, 175 * pitch, 92 * pitch, 0.28, 'triangle', 0.02);
        triggerTone(now + 0.18, 0.16, 760 * pitch, 1120 * pitch, 0.14, 'sine', 0.006);
        break;

      case SOUND_CUES.ECHO_PULSE:
        // Muffled triple knuckle-knock behind wet mortar
        for (let i = 0; i < 3; i += 1) {
          const t = now + i * 0.13;
          const amp = 0.24 * (1 - i * 0.2);
          triggerTone(t, 0.09, (124 - i * 8) * pitch, 68 * pitch, amp, 'sine', 0.004);
          triggerNoise(t, 0.065, 'lowpass', 360, 180, 1.2, amp * 0.65, 0.004);
        }
        break;

      case SOUND_CUES.CLAMP_ENGAGE:
        // Dual iron ratchet lock + pressurized groan
        triggerTone(now, 0.09, 310 * pitch, 160 * pitch, 0.3, 'sawtooth', 0.002);
        triggerTone(now + 0.095, 0.24, 155 * pitch, 74 * pitch, 0.34, 'triangle', 0.005);
        triggerNoise(now + 0.08, 0.18, 'bandpass', 950, 320, 2.8, 0.2, 0.005);
        break;

      case SOUND_CUES.MARROW_BLOOM:
        // Wall pressure split + wet resonant bloom
        triggerTone(now, 0.42, 145 * pitch, 54 * pitch, 0.38, 'triangle', 0.01);
        triggerNoise(now, 0.38, 'bandpass', 1450, 260, 1.7, 0.26, 0.008);
        triggerTone(now + 0.14, 0.3, 280 * pitch, 185 * pitch, 0.16, 'sine', 0.03);
        break;

      case SOUND_CUES.THROAT_OFFER:
        // Low wet swallow + ink unsealing hiss
        triggerTone(now, 0.22, 165 * pitch, 76 * pitch, 0.28, 'sine', 0.02);
        triggerNoise(now + 0.09, 0.24, 'bandpass', 920, 340, 2.2, 0.2, 0.025);
        break;

      case SOUND_CUES.EXCISION_CUT:
        // Cold surgical blade shear + heavy wet severance
        triggerNoise(now, 0.28, 'highpass', 1900, 620, 1.8, 0.28, 0.003);
        triggerTone(now + 0.05, 0.56, 130 * pitch, 42 * pitch, 0.42, 'triangle', 0.018);
        triggerNoise(now + 0.14, 0.42, 'lowpass', 520, 140, 1.1, 0.24, 0.03);
        break;

      case SOUND_CUES.PLEAD_SALVATION:
        // Mortar seam opening into exterior rain + low hollow bell-iron swell
        triggerTone(now, 0.72, 92 * pitch, 58 * pitch, 0.34, 'sine', 0.08);
        triggerTone(now + 0.08, 0.68, 224 * pitch, 198 * pitch, 0.14, 'triangle', 0.09);
        triggerNoise(now + 0.1, 0.65, 'bandpass', 1650, 980, 0.9, 0.24, 0.12);
        break;

      case SOUND_CUES.STORY_DRIP:
        // Subtle single condensation drop in an empty tiled cistern
        triggerTone(now, 0.085, 680 * pitch, 1210 * pitch, 0.15, 'sine', 0.004);
        triggerTone(now + 0.09, 0.14, 980 * pitch, 740 * pitch, 0.06, 'sine', 0.008);
        break;

      case SOUND_CUES.PHASE_SHIFT:
        // Deep architectural groan behind the grout
        triggerTone(now, 0.62, 74 * pitch, 46 * pitch, 0.34, 'triangle', 0.07);
        triggerNoise(now + 0.04, 0.52, 'lowpass', 340, 120, 1.4, 0.2, 0.06);
        break;

      case SOUND_CUES.ARC2_STAMP:
        // Wooden rubber ink-stamp thud + dry paper rustle
        triggerTone(now, 0.075, 185 * pitch, 76 * pitch, 0.34, 'triangle', 0.002);
        triggerNoise(now, 0.065, 'bandpass', 1500 * pitch, 740 * pitch, 1.9, 0.2, 0.002);
        break;

      case SOUND_CUES.ARC2_CONTACT:
        // Desk file folder rustle + telephone/drawer click
        triggerNoise(now, 0.11, 'bandpass', 1800, 950, 1.6, 0.2, 0.006);
        triggerTone(now + 0.055, 0.09, 320 * pitch, 180 * pitch, 0.2, 'triangle', 0.003);
        break;

      case SOUND_CUES.ARC2_DEDUCTION:
        // Brass thumbtack press into corkboard
        triggerTone(now, 0.045, 980 * pitch, 410 * pitch, 0.22, 'triangle', 0.002);
        triggerNoise(now + 0.015, 0.09, 'bandpass', 1100, 420, 2.2, 0.18, 0.003);
        break;

      case SOUND_CUES.ARC2_DESK_ANOMALY:
        // Damp envelope / reel tape click
        triggerNoise(now, 0.12, 'bandpass', 1400, 620, 2.2, 0.2, 0.006);
        triggerTone(now + 0.03, 0.12, 460 * pitch, 290 * pitch, 0.16, 'sine', 0.005);
        break;

      case SOUND_CUES.ARC2_CHAPTER:
        // Taut red string snap + heavy case pin thud
        triggerTone(now, 0.18, 260 * pitch, 112 * pitch, 0.28, 'triangle', 0.003);
        triggerNoise(now + 0.02, 0.16, 'bandpass', 1750, 680, 2.6, 0.22, 0.004);
        break;

      case SOUND_CUES.ARC2_FILE_ZERO:
        // Heavy manila dossier flap + chamber valve iron bleed
        triggerNoise(now, 0.25, 'bandpass', 1200, 320, 1.5, 0.24, 0.008);
        triggerTone(now + 0.06, 0.65, 86 * pitch, 48 * pitch, 0.38, 'triangle', 0.03);
        triggerTone(now + 0.2, 0.36, 380 * pitch, 240 * pitch, 0.14, 'sine', 0.04);
        break;

      case SOUND_CUES.ARC3_ENTER:
        // Dual convergence drone: exterior rain + damp chamber resonance
        triggerTone(now, 0.78, 108 * pitch, 64 * pitch, 0.34, 'sine', 0.09);
        triggerNoise(now + 0.06, 0.72, 'bandpass', 1420, 640, 1.1, 0.22, 0.12);
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
        cue === SOUND_CUES.CREATURE_BUY;
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
