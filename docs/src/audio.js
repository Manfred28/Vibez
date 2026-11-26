/**
 * Zero-asset Web Audio procedural foley & phase-escalating room ambience engine for
 * "The Tending of the Unnamed Organ" (1:1 parity with Android SoundCueResolver + ProceduralFoleySynth).
 *
 * Thin facade preserving all public exports while delegating cue resolution, DSP primitives,
 * and arc-specific cue banks to modular sub-files under `./audio/`.
 */

import {
  AUDIO_MUTE_STORAGE_KEY,
  CUE_MIN_INTERVAL_MS,
  SOUND_ACTION_HINTS,
  SOUND_CUES,
  computeAmbientProfile,
  readMutedPreference,
  resolveTransitionCues,
  writeMutedPreference,
} from './audio/cueResolver.js?v=3.14';
import {
  IRON_MODE_RATIOS,
  createCrackleNoiseBuffer,
  createDspPrimitives,
  createNoiseBuffer,
} from './audio/dspPrimitives.js?v=3.14';
import { renderArc1Cue } from './audio/arc1CueBank.js?v=3.14';
import { renderArc2Cue } from './audio/arc2CueBank.js?v=3.14';

export {
  AUDIO_MUTE_STORAGE_KEY,
  IRON_MODE_RATIOS,
  SOUND_ACTION_HINTS,
  SOUND_CUES,
  computeAmbientProfile,
  readMutedPreference,
  resolveTransitionCues,
  writeMutedPreference,
};

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
  let dsp = null;
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
      dsp = createDspPrimitives(ctx, sfxBus, noiseBuffer, crackleNoiseBuffer);
      initAmbientGraph();
      applyAmbientTargets(currentProfile);
      startAmbientScheduler();
      return ctx;
    } catch {
      ctx = null;
      dsp = null;
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

  function renderCue(cue, phase, pitch) {
    if (!ctx || !sfxBus || !dsp) return;
    const now = ctx.currentTime + 0.003;
    if (!renderArc1Cue(cue, phase, pitch, now, dsp)) {
      renderArc2Cue(cue, phase, pitch, now, dsp);
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
      dsp = null;
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
