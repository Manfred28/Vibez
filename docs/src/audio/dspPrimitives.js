/**
 * Shared physical acoustic synthesis primitives for Web Audio procedural foley:
 * cast-iron inharmonic modal rings, fibrous tendon tearing, wet mucosa squelches,
 * bubble clusters, and pink/crackle excitation noise buffers.
 */

// Inharmonic cast-iron wheel ring modal ratios (authentic cast-iron clank/clang spectrum)
export const IRON_MODE_RATIOS = Object.freeze([1.0, 1.53, 2.24, 2.89, 3.71, 4.68]);
const IRON_MODE_AMPS = Object.freeze([0.50, 0.48, 0.44, 0.36, 0.28, 0.20]);
const IRON_MODE_DECAYS = Object.freeze([10.5, 13.0, 16.5, 21.0, 27.0, 34.0]);

export function createNoiseBuffer(audioCtx) {
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
export function createCrackleNoiseBuffer(audioCtx) {
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

export function createDspPrimitives(ctx, sfxBus, noiseBuffer, crackleNoiseBuffer) {
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
    clangScale = 1.0,
  ) {
    const bus = ctx.createGain();
    bus.gain.value = peakGain;
    bus.connect(sfxBus);
    const modeAttack = clangScale < 0.5 ? 0.006 : 0.003;

    for (let m = 0; m < IRON_MODE_RATIOS.length; m += 1) {
      const f = Math.min(3200, Math.max(60, baseFreq * IRON_MODE_RATIOS[m]));
      const modeDur = Math.min(duration, Math.max(0.055, 3.8 / (IRON_MODE_DECAYS[m] * decayScale)));
      const osc = ctx.createOscillator();
      const g = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(f * 1.026, startTime);
      osc.frequency.exponentialRampToValueAtTime(f, startTime + 0.032);

      g.gain.setValueAtTime(0.0001, startTime);
      g.gain.linearRampToValueAtTime(IRON_MODE_AMPS[m] * 0.48, startTime + modeAttack);
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
    clangGain.gain.linearRampToValueAtTime(Math.max(0.0002, 0.52 * clangScale), startTime + 0.0025);
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
      rModGain.gain.setValueAtTime(Math.max(0.0002, 0.28 * clangScale), startTime);
      rModGain.gain.exponentialRampToValueAtTime(0.0001, startTime + rDur);

      const teethLfo = ctx.createOscillator();
      teethLfo.type = 'sine';
      teethLfo.frequency.value = 46;
      const teethDepth = ctx.createGain();
      teethDepth.gain.value = 0.18 * clangScale;
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

  return {
    triggerTone,
    triggerNoise,
    triggerIronModalRing,
    triggerTendonTear,
    triggerWetSquelch,
    triggerBubbleCluster,
  };
}
