package com.example.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Pure Kotlin DSP procedural foley and room-ambience synthesizer generating 16-bit PCM audio
 * with zero external audio assets.
 *
 * Acoustic architecture:
 * - True cast-iron inharmonic modal bank (6 detuned iron ring modes + gritty pawl ratchet + heavy
 *   wall-mounted housing thud) for the valve, floor sump grate, iron clamp, and surgical shears.
 * - Visceral organic flesh/tendon synthesis: stochastic fibrous tearing trains through dual
 *   sweeping formant resonators, LFO-modulated wet mucosa squelches, and irregular cavity bubble
 *   clusters — eliminating generic percussive synth clicks.
 * - Subtle closed-tile-chamber early reflection tail (`19ms` / `35ms`) grounding every cue in a
 *   damp subterranean room.
 */
object ProceduralFoleySynth {
  const val SAMPLE_RATE: Int = 22050
  private const val TWO_PI = 2.0 * PI

  // Inharmonic cast-iron wheel ring modal ratios (authentic cast-iron clank/clang spectrum)
  private val IRON_MODE_RATIOS = doubleArrayOf(1.00, 1.53, 2.24, 2.89, 3.71, 4.68)
  private val IRON_MODE_AMPS = doubleArrayOf(0.50, 0.48, 0.44, 0.36, 0.28, 0.20)
  private val IRON_MODE_DECAYS = doubleArrayOf(10.5, 13.0, 16.5, 21.0, 27.0, 34.0)

  private class NoiseGen(seed: Int) {
    private var state: Int = if (seed == 0) 0x6D2B79F5 else seed

    fun nextFloat(): Float {
      var x = state
      x = x xor (x shl 13)
      x = x xor (x ushr 17)
      x = x xor (x shl 5)
      state = x
      return ((x and 0x7FFFFFFF).toFloat() / 0x3FFFFFFF.toFloat()) - 1f
    }

    fun nextUnit(): Double = (nextFloat().toDouble() + 1.0) * 0.5
  }

  /** Lightweight 2-pole state-variable resonant filter for sweeping wet formants & iron grit. */
  private class SvFilter {
    private var low = 0.0
    private var band = 0.0

    fun processBandpass(input: Double, freqHz: Double, q: Double): Double {
      val clampedFreq = freqHz.coerceIn(45.0, SAMPLE_RATE * 0.36)
      val f = (2.0 * sin(PI * clampedFreq / SAMPLE_RATE)).coerceIn(0.01, 1.15)
      val damp = (1.0 / q.coerceIn(0.6, 14.0)).coerceIn(0.07, 1.6)
      low += f * band
      val high = input - low - damp * band
      band += f * high
      return band
    }

    fun processLowpass(input: Double, freqHz: Double, q: Double): Double {
      processBandpass(input, freqHz, q)
      return low
    }
  }

  /** Adds subtle damp tiled-chamber early slapback reflections (19ms & 35ms) without washing transients. */
  private fun applyChamberSlapback(samples: FloatArray, wetAmount: Float = 0.14f): FloatArray {
    val d1 = (SAMPLE_RATE * 0.019).toInt()
    val d2 = (SAMPLE_RATE * 0.035).toInt()
    val out = FloatArray(samples.size)
    var lp = 0f
    for (i in samples.indices) {
      val dry = samples[i]
      val r1 = if (i >= d1) samples[i - d1] * 0.68f else 0f
      val r2 = if (i >= d2) samples[i - d2] * 0.42f else 0f
      lp += 0.24f * ((r1 + r2) - lp)
      out[i] = dry + lp * wetAmount
    }
    return out
  }

  private fun toPcm16(samples: FloatArray, masterGain: Float, roomWet: Float = 0.14f): ShortArray {
    val spatial = if (roomWet > 0f) applyChamberSlapback(samples, roomWet) else samples
    val out = ShortArray(spatial.size)
    for (i in spatial.indices) {
      val shaped = tanh((spatial[i] * masterGain).toDouble()).toFloat()
      val clamped = shaped.coerceIn(-0.95f, 0.95f)
      out[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
    }
    return out
  }

  // ==================== SHARED PHYSICAL ACOUSTIC PRIMITIVES ====================

  /**
   * Adds authentic struck/ratcheted cast-iron resonance: 6 inharmonic metallic modes + gritty
   * metal-on-metal pawl friction + heavy wall-housing impact weight.
   */
  private fun addIronModalRing(
    buf: FloatArray,
    startSec: Double,
    durationSec: Double,
    baseFreqHz: Double,
    gain: Double,
    decayScale: Double = 1.0,
    seed: Int = 71,
    includeRatchetGrit: Boolean = true,
    clangScale: Double = 1.0,
  ) {
    val startIdx = (startSec * SAMPLE_RATE).toInt().coerceAtLeast(0)
    val count = (durationSec * SAMPLE_RATE).toInt().coerceAtLeast(1)
    val endIdx = (startIdx + count).coerceAtMost(buf.size)
    val noise = NoiseGen(seed)
    val gritFilter = SvFilter()
    val clangFilter = SvFilter()
    val attackRate = if (clangScale < 0.5) 220.0 else 420.0

    for (i in startIdx until endIdx) {
      val t = (i - startIdx).toDouble() / SAMPLE_RATE
      val attack = 1.0 - exp(-t * attackRate)
      var modes = 0.0
      for (m in IRON_MODE_RATIOS.indices) {
        val f = baseFreqHz * IRON_MODE_RATIOS[m]
        // Subtle pitch sag on initial iron impact gives massive physical weight
        val sag = 1.0 + 0.025 * exp(-t * 48.0)
        modes += sin(TWO_PI * f * sag * t) * exp(-t * IRON_MODE_DECAYS[m] * decayScale) * IRON_MODE_AMPS[m]
      }
      val n = noise.nextFloat().toDouble()
      val clangBurst = clangFilter.processBandpass(n, (baseFreqHz * 4.2).coerceIn(500.0, 2900.0), 3.4) *
        exp(-t * 58.0) * 0.64 * clangScale
      val ratchet =
        if (includeRatchetGrit && t < 0.085) {
          val teeth = 0.55 + 0.45 * sin(TWO_PI * 46.0 * t)
          val sweep = (baseFreqHz * 4.8 * exp(-t * 9.0)).coerceIn(360.0, 2600.0)
          gritFilter.processBandpass(n, sweep, 4.2) * exp(-t * 26.0) * teeth * 0.46 * clangScale
        } else 0.0

      buf[i] += ((modes * attack * 0.78 + clangBurst + ratchet) * gain).toFloat()
    }
  }

  /**
   * Adds fibrous sinew / tendon tearing: a bursty stochastic train of irregular micro-fibers
   * snapping asynchronously across two sweeping formant bands (wet cavity + taut fibrous rasp),
   * layered over a stretched low-mid ligament tension groan.
   */
  private fun addTendonTear(
    buf: FloatArray,
    startSec: Double,
    durationSec: Double,
    pitch: Double,
    gain: Double,
    fiberCount: Int = 18,
    seed: Int = 311,
  ) {
    val startIdx = (startSec * SAMPLE_RATE).toInt().coerceAtLeast(0)
    val count = (durationSec * SAMPLE_RATE).toInt().coerceAtLeast(1)
    val endIdx = (startIdx + count).coerceAtMost(buf.size)
    val rng = NoiseGen(seed)

    // Pre-schedule irregular snapping fiber transients clustered along a tension-and-rip arc
    val snapTimes = DoubleArray(fiberCount)
    val snapAmps = DoubleArray(fiberCount)
    val snapFreqs = DoubleArray(fiberCount)
    for (k in 0 until fiberCount) {
      val progress = k.toDouble() / fiberCount.coerceAtLeast(1)
      // Slightly denser ripping toward the middle/climax of the tear
      val jitter = (rng.nextUnit() - 0.5) * (durationSec / fiberCount) * 1.45
      snapTimes[k] = (progress * durationSec * 0.88 + jitter).coerceIn(0.0, durationSec - 0.015)
      snapAmps[k] = 0.45 + 0.55 * sin(PI * progress) + rng.nextUnit() * 0.35
      snapFreqs[k] = (760.0 + rng.nextUnit() * 1350.0) * pitch
    }

    val cavityFilter = SvFilter()
    val fiberFilter = SvFilter()
    val groanNoise = NoiseGen(seed + 97)

    for (i in startIdx until endIdx) {
      val t = (i - startIdx).toDouble() / SAMPLE_RATE
      val frac = (t / durationSec).coerceIn(0.0, 1.0)
      val env = sin(PI * frac).coerceAtLeast(0.0)

      // Continuous taut wet-fiber friction excitation with irregular micro-tears
      var rawRip = groanNoise.nextFloat().toDouble() * 0.32
      for (k in 0 until fiberCount) {
        val dt = t - snapTimes[k]
        if (dt >= 0.0 && dt < 0.022) {
          // Sharp asymmetric fibrous snap impulse
          val fiber = sin(TWO_PI * snapFreqs[k] * dt) * exp(-dt * 210.0)
          val pop = exp(-dt * 145.0) * (1.0 - dt * 32.0).coerceAtLeast(0.0)
          rawRip += (fiber * 0.72 + pop * 0.55) * snapAmps[k]
        }
      }

      // Dual moving resonant formants: wet tissue pocket (310..760Hz) + ripping fascia (1150..2300Hz)
      val wobble = sin(TWO_PI * 11.5 * t) * 95.0
      val cavityFreq = (320.0 + frac * 390.0 + wobble).coerceIn(180.0, 950.0) * pitch
      val fiberFreq = (1220.0 + (1.0 - frac * 0.45) * 920.0 + wobble * 1.6).coerceIn(620.0, 2900.0) * pitch
      val wetBody = cavityFilter.processBandpass(rawRip, cavityFreq, 4.6) * 0.82
      val tautRasp = fiberFilter.processBandpass(rawRip, fiberFreq, 3.8) * 0.62

      // Stretched elastic ligament groan pitching down under tension with gritty flutter
      val flutter = 0.72 + 0.28 * sin(TWO_PI * 29.0 * t)
      val groanFreq = (148.0 * exp(-frac * 1.45) + 54.0) * pitch
      val groan = sin(TWO_PI * groanFreq * t) * env * flutter * 0.34

      buf[i] += ((wetBody + tautRasp + groan) * gain).toFloat()
    }
  }

  /**
   * Adds thick, wet, organic mucosa squelch using crackle-injected wet noise routed through two
   * LFO-swept resonant vocal/cavity formant filters + slow fleshy pressure swell.
   */
  private fun addWetSquelch(
    buf: FloatArray,
    startSec: Double,
    durationSec: Double,
    pitch: Double,
    gain: Double,
    formantStartHz: Double = 240.0,
    formantEndHz: Double = 640.0,
    wobbleHz: Double = 7.5,
    seed: Int = 521,
  ) {
    val startIdx = (startSec * SAMPLE_RATE).toInt().coerceAtLeast(0)
    val count = (durationSec * SAMPLE_RATE).toInt().coerceAtLeast(1)
    val endIdx = (startIdx + count).coerceAtMost(buf.size)
    val noise = NoiseGen(seed)
    val f1 = SvFilter()
    val f2 = SvFilter()
    var crackleEnv = 0.0

    for (i in startIdx until endIdx) {
      val t = (i - startIdx).toDouble() / SAMPLE_RATE
      val frac = (t / durationSec).coerceIn(0.0, 1.0)
      val env = sin(PI * frac).coerceAtLeast(0.0) * (1.0 - exp(-t * 28.0))
      val n = noise.nextFloat().toDouble()

      // Inject irregular wet sticky micro-pops so the noise sounds viscous rather than static hiss
      if (noise.nextUnit() > 0.986) {
        crackleEnv = 0.9 + noise.nextUnit() * 0.8
      }
      crackleEnv *= 0.91
      val wetSource = n * (0.45 + crackleEnv * 1.35)

      val lfo = 0.5 + 0.5 * sin(TWO_PI * wobbleHz * t + 0.4)
      val sweep = formantStartHz + (formantEndHz - formantStartHz) * frac
      val freq1 = (sweep * (0.76 + 0.46 * lfo)).coerceIn(110.0, 1100.0) * pitch
      val freq2 = (freq1 * 2.15 + 140.0 * cos(TWO_PI * (wobbleHz * 1.35) * t)).coerceIn(320.0, 2600.0)

      val cavity1 = f1.processBandpass(wetSource, freq1, 5.8) * 0.85
      val cavity2 = f2.processBandpass(wetSource, freq2, 6.4) * 0.52
      val fleshSub = sin(TWO_PI * ((78.0 - 24.0 * frac) * pitch) * t) * env * (0.7 + 0.3 * lfo) * 0.38

      buf[i] += (((cavity1 + cavity2) * env + fleshSub) * gain).toFloat()
    }
  }

  /**
   * Adds a staggered cluster of wet cavity bubble pops (soft attack, organic upward pitch chirp)
   * that instantly sells thick liquid mucosa, blister rupture, and submerged suction.
   */
  private fun addBubbleCluster(
    buf: FloatArray,
    startSec: Double,
    spanSec: Double,
    count: Int,
    baseFreqHz: Double,
    gain: Double,
    seed: Int = 823,
  ) {
    val rng = NoiseGen(seed)
    for (b in 0 until count) {
      val bStart = startSec + rng.nextUnit() * spanSec
      val bDur = 0.026 + rng.nextUnit() * 0.032
      val f0 = baseFreqHz * (0.68 + rng.nextUnit() * 0.78)
      val bGain = gain * (0.55 + rng.nextUnit() * 0.55)
      val sIdx = (bStart * SAMPLE_RATE).toInt().coerceAtLeast(0)
      val eIdx = ((bStart + bDur) * SAMPLE_RATE).toInt().coerceAtMost(buf.size)
      var phaseAcc = 0.0
      for (i in sIdx until eIdx) {
        val dt = (i - sIdx).toDouble() / SAMPLE_RATE
        val u = (dt / bDur).coerceIn(0.0, 1.0)
        val env = sin(PI * u) * exp(-u * 1.4)
        val f = f0 * (1.0 + u * u * 1.05)
        phaseAcc += TWO_PI * f / SAMPLE_RATE
        buf[i] += (sin(phaseAcc) * env * bGain).toFloat()
      }
    }
  }

  fun synthesizeCue(cue: SoundCue, phase: Int = 0, pitchFactor: Float = 1f): ShortArray {
    val pitch = pitchFactor.coerceIn(0.82f, 1.18f).toDouble()
    val pDark = 1.0 - (phase.coerceIn(0, 4) * 0.035)
    return when (cue) {
      SoundCue.VALVE_PRESS -> renderValvePress(pitch * pDark, phase)
      SoundCue.CREATURE_BUY_SUTURE_HAND -> renderSutureHandBuy(pitch * pDark)
      SoundCue.CREATURE_BUY_LEECH_CLERK -> renderLeechClerkBuy(pitch * pDark)
      SoundCue.CREATURE_BUY_WEEPING_LAMP -> renderWeepingLampBuy(pitch * pDark)
      SoundCue.CREATURE_BUY_SUMP_SIPHON -> renderSumpSiphonBuy(pitch * pDark)
      SoundCue.ANOMALY_TOOTH -> renderToothPull(pitch)
      SoundCue.ANOMALY_EYE -> renderGlassEyeWipe(pitch)
      SoundCue.ANOMALY_NODULE -> renderNoduleProd(pitch)
      SoundCue.ANOMALY_RIB -> renderRibPry(pitch)
      SoundCue.JAR_BOTTLE -> renderJarBottle(pitch)
      SoundCue.GRAFT_PERFORM -> renderGraftPerform(pitch)
      SoundCue.SCAR_UNLOCK -> renderScarStitch(pitch)
      SoundCue.DRAIN_TOGGLE -> renderIronClank(pitch)
      SoundCue.SUMP_DREDGE -> renderSumpDredge(pitch)
      SoundCue.ECHO_PULSE -> renderEchoTripleKnock(pitch)
      SoundCue.CLAMP_ENGAGE -> renderClampLock(pitch)
      SoundCue.MARROW_BLOOM -> renderMarrowBloom(pitch)
      SoundCue.THROAT_OFFER -> renderThroatSwallow(pitch)
      SoundCue.EXCISION_CUT -> renderExcisionShear(pitch)
      SoundCue.PLEAD_SALVATION -> renderPleadSalvation(pitch)
      SoundCue.STORY_DRIP -> renderStoryCondensationDrip(pitch)
      SoundCue.PHASE_SHIFT -> renderPhaseShift(pitch)
      SoundCue.ARC2_STAMP -> renderDeskStamp(pitch)
      SoundCue.ARC2_CONTACT -> renderPaperFolder(pitch)
      SoundCue.ARC2_DEDUCTION -> renderRedStringPin(pitch)
      SoundCue.ARC2_DESK_ANOMALY -> renderDeskAnomaly(pitch)
      SoundCue.ARC2_CHAPTER -> renderCaseChapterThud(pitch)
      SoundCue.ARC2_FILE_ZERO -> renderFileZeroReveal(pitch)
      SoundCue.ARC3_ENTER -> renderConvergenceTone(pitch)
    }
  }

  /**
   * Deep heavy cast-iron-in-masonry wall valve thump: dominant sub-bass & low-mid wall-housing
   * chest thump + low-passed impact mass + darkened bolted cast-iron modal under-ring +
   * phase-escalating wet organ seepage behind the rim.
   */
  private fun renderValvePress(pitch: Double, phase: Int): ShortArray {
    val len = (SAMPLE_RATE * 0.28).toInt()
    val buf = FloatArray(len)
    val thudNoise = NoiseGen(103 + phase * 19)
    val thudFilter = SvFilter()

    // 1. Dominant deep sub-bass & masonry chest thump (108Hz -> 36Hz + 152Hz -> 62Hz body punch + damped impact)
    var subPhase = 0.0
    var bodyPhase = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val subF = (36.0 + 72.0 * exp(-t * 34.0)) * pitch
      val bodyF = (62.0 + 90.0 * exp(-t * 40.0)) * pitch
      subPhase += TWO_PI * subF / SAMPLE_RATE
      bodyPhase += TWO_PI * bodyF / SAMPLE_RATE
      val subAttack = 1.0 - exp(-t * 240.0)
      val bodyAttack = 1.0 - exp(-t * 260.0)
      val subThump = sin(subPhase) * subAttack * exp(-t * 13.5) * 0.98
      val bodyPunch = sin(bodyPhase) * bodyAttack * exp(-t * 22.0) * 0.54
      val impactCut = (115.0 + 145.0 * exp(-t * 30.0)) * pitch
      val impactMass =
        thudFilter.processLowpass(thudNoise.nextFloat().toDouble(), impactCut, 1.35) *
          (1.0 - exp(-t * 280.0)) *
          exp(-t * 26.0) *
          0.56
      buf[i] += (subThump + bodyPunch + impactMass).toFloat()
    }

    // 2. Darkened, heavy bolted cast-iron modal under-ring (154Hz fundamental + muted 192Hz lug seat)
    addIronModalRing(
      buf = buf,
      startSec = 0.0,
      durationSec = 0.26,
      baseFreqHz = 154.0 * pitch,
      gain = 0.44,
      decayScale = 1.25,
      seed = 101 + phase * 17,
      includeRatchetGrit = true,
      clangScale = 0.24,
    )
    addIronModalRing(
      buf = buf,
      startSec = 0.046,
      durationSec = 0.17,
      baseFreqHz = 192.0 * pitch,
      gain = 0.16,
      decayScale = 1.50,
      seed = 149 + phase * 7,
      includeRatchetGrit = false,
      clangScale = 0.20,
    )

    // 3. Phase 1+: Damp organic ichor seepage trailing behind the heavy iron turn
    if (phase >= 1) {
      val wet = (phase / 4.0).coerceIn(0.25, 1.0)
      addWetSquelch(
        buf = buf,
        startSec = 0.06,
        durationSec = 0.18,
        pitch = pitch,
        gain = 0.26 * wet,
        formantStartHz = 280.0,
        formantEndHz = 140.0,
        wobbleHz = 10.5,
        seed = 181 + phase * 13,
      )
      addBubbleCluster(
        buf = buf,
        startSec = 0.07,
        spanSec = 0.13,
        count = 2 + phase,
        baseFreqHz = 165.0 * pitch,
        gain = 0.13 * wet,
        seed = 197 + phase * 11,
      )
    }

    return toPcm16(buf, 0.90f, roomWet = 0.16f)
  }

  /** Suture-Hand: wet fascia tearing loose from plaster + tendons snapping + cold iron rim grip. */
  private fun renderSutureHandBuy(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.42).toInt()
    val buf = FloatArray(len)
    addTendonTear(buf, 0.0, 0.26, pitch, gain = 0.72, fiberCount = 16, seed = 202)
    addWetSquelch(buf, 0.03, 0.24, pitch, gain = 0.38, formantStartHz = 260.0, formantEndHz = 520.0, seed = 205)
    // Cold skeletal fingers clamping onto the iron valve wheel at 165ms
    addIronModalRing(buf, 0.165, 0.22, 265.0 * pitch, gain = 0.44, decayScale = 1.35, seed = 209, includeRatchetGrit = false)
    return toPcm16(buf, 0.82f, roomWet = 0.16f)
  }

  /** Leech-Clerk: long viscous wet mucosal squelch + irregular suction-cup bubbles dragging on cold glass. */
  private fun renderLeechClerkBuy(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.46).toInt()
    val buf = FloatArray(len)
    addWetSquelch(buf, 0.0, 0.42, pitch, gain = 0.78, formantStartHz = 220.0, formantEndHz = 620.0, wobbleHz = 8.5, seed = 213)
    addBubbleCluster(buf, 0.04, 0.32, count = 7, baseFreqHz = 185.0 * pitch, gain = 0.32, seed = 219)
    return toPcm16(buf, 0.82f, roomWet = 0.14f)
  }

  /** Weeping Lamp: eerie hollow thick-glass vessel toll + slow heavy viscous condensation drops. */
  private fun renderWeepingLampBuy(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.52).toInt()
    val buf = FloatArray(len)
    val glassPartials = doubleArrayOf(182.0, 346.0, 588.0, 91.0)
    val glassAmps = doubleArrayOf(0.46, 0.24, 0.12, 0.30)
    val glassDecays = doubleArrayOf(5.8, 8.5, 13.0, 6.2)
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val attack = 1.0 - exp(-t * 50.0)
      var g = 0.0
      for (k in glassPartials.indices) {
        g += sin(TWO_PI * (glassPartials[k] * pitch) * t) * exp(-t * glassDecays[k]) * glassAmps[k]
      }
      buf[i] = (g * attack).toFloat()
    }
    addBubbleCluster(buf, 0.14, 0.26, count = 4, baseFreqHz = 195.0 * pitch, gain = 0.28, seed = 224)
    return toPcm16(buf, 0.80f, roomWet = 0.22f)
  }

  /** Sump-Siphon: cavernous wet bellows inhale + submerged bubbling iron-pipe gulp + iron seat clank. */
  private fun renderSumpSiphonBuy(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.56).toInt()
    val buf = FloatArray(len)
    addWetSquelch(buf, 0.0, 0.48, pitch, gain = 0.72, formantStartHz = 420.0, formantEndHz = 150.0, wobbleHz = 6.2, seed = 235)
    addBubbleCluster(buf, 0.05, 0.38, count = 8, baseFreqHz = 140.0 * pitch, gain = 0.34, seed = 241)
    addIronModalRing(buf, 0.28, 0.24, 175.0 * pitch, gain = 0.36, decayScale = 1.3, seed = 247, includeRatchetGrit = false)
    return toPcm16(buf, 0.84f, roomWet = 0.18f)
  }

  /** Pull Pale Tooth: taut periodontal ligament tearing out of wet plaster + enamel root wrench pop. */
  private fun renderToothPull(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.36).toInt()
    val buf = FloatArray(len)
    addTendonTear(buf, 0.0, 0.28, pitch, gain = 0.84, fiberCount = 18, seed = 303)
    addWetSquelch(buf, 0.08, 0.22, pitch, gain = 0.42, formantStartHz = 380.0, formantEndHz = 190.0, seed = 309)
    addBubbleCluster(buf, 0.16, 0.12, count = 4, baseFreqHz = 230.0 * pitch, gain = 0.28, seed = 313)
    return toPcm16(buf, 0.82f, roomWet = 0.15f)
  }

  /** Wipe Glass Eye: damp rubbery condensation smear + wet mucosal eyelid squelch + hollow sphere roll. */
  private fun renderGlassEyeWipe(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.42).toInt()
    val buf = FloatArray(len)
    addWetSquelch(buf, 0.0, 0.34, pitch, gain = 0.74, formantStartHz = 290.0, formantEndHz = 780.0, wobbleHz = 6.0, seed = 404)
    addBubbleCluster(buf, 0.14, 0.20, count = 5, baseFreqHz = 210.0 * pitch, gain = 0.26, seed = 409)
    return toPcm16(buf, 0.80f, roomWet = 0.14f)
  }

  /** Prod Swollen Nodule: taut blister membrane tearing open + juicy oozing wet squelch & bubble burst. */
  private fun renderNoduleProd(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.44).toInt()
    val buf = FloatArray(len)
    addTendonTear(buf, 0.0, 0.16, pitch, gain = 0.56, fiberCount = 12, seed = 505)
    addWetSquelch(buf, 0.04, 0.38, pitch, gain = 0.84, formantStartHz = 480.0, formantEndHz = 175.0, wobbleHz = 9.5, seed = 509)
    addBubbleCluster(buf, 0.07, 0.30, count = 7, baseFreqHz = 175.0 * pitch, gain = 0.34, seed = 515)
    return toPcm16(buf, 0.84f, roomWet = 0.14f)
  }

  /** Pry Split Chitin Rib: deep calcified bone wrench + long fibrous tendon/ligament ripping + marrow groan. */
  private fun renderRibPry(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.52).toInt()
    val buf = FloatArray(len)
    addTendonTear(buf, 0.0, 0.44, pitch * 0.92, gain = 0.90, fiberCount = 24, seed = 606)
    addWetSquelch(buf, 0.10, 0.36, pitch, gain = 0.50, formantStartHz = 280.0, formantEndHz = 540.0, wobbleHz = 6.5, seed = 611)
    addBubbleCluster(buf, 0.18, 0.24, count = 5, baseFreqHz = 155.0 * pitch, gain = 0.24, seed = 617)
    return toPcm16(buf, 0.84f, roomWet = 0.18f)
  }

  /** Bottle Specimen Jar: dark thick-glass vessel resonance + viscous suction wax-cork seal. */
  private fun renderJarBottle(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.40).toInt()
    val buf = FloatArray(len)
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val attack = 1.0 - exp(-t * 95.0)
      val vessel =
        (sin(TWO_PI * (248.0 * pitch) * t) * 0.40 +
          sin(TWO_PI * (412.0 * pitch) * t) * 0.20 +
          sin(TWO_PI * (124.0 * pitch) * t) * 0.24) * exp(-t * 14.0) * attack
      buf[i] = vessel.toFloat()
    }
    addWetSquelch(buf, 0.05, 0.26, pitch, gain = 0.58, formantStartHz = 360.0, formantEndHz = 165.0, wobbleHz = 8.0, seed = 650)
    addBubbleCluster(buf, 0.08, 0.18, count = 4, baseFreqHz = 170.0 * pitch, gain = 0.28, seed = 653)
    return toPcm16(buf, 0.80f, roomWet = 0.16f)
  }

  /**
   * Long (~1.15s) deeply organic & grotesque grafting ritual:
   * - Stage 1 (0..450ms): heavy damp flesh compression & first slow tendon rip
   * - Stage 2 (260..820ms): overlapping sinew tearing & wet mucosa braiding squelches + cavity bubbles
   * - Stage 3 (680..1150ms): visceral deep sub-exhale & wet settling into the wall
   */
  private fun renderGraftPerform(pitch: Double): ShortArray {
    val duration = 1.15
    val len = (SAMPLE_RATE * duration).toInt()
    val buf = FloatArray(len)

    // Deep peristaltic body pressure swell (60Hz -> 33Hz)
    var subPhase = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val frac = t / duration
      val env = sin(PI * frac).coerceAtLeast(0.0)
      val f = (33.0 + 27.0 * exp(-t * 2.0)) * pitch
      subPhase += TWO_PI * f / SAMPLE_RATE
      buf[i] = (sin(subPhase) * env * 0.44).toFloat()
    }

    // Dual overlapping fibrous tendon/sinew tearing waves
    addTendonTear(buf, 0.05, 0.48, pitch * 0.96, gain = 0.74, fiberCount = 24, seed = 707)
    addTendonTear(buf, 0.34, 0.52, pitch * 0.88, gain = 0.80, fiberCount = 26, seed = 719)

    // Deep wet mucosa braiding squelch + dense organic bubble suction
    addWetSquelch(buf, 0.02, 0.72, pitch, gain = 0.72, formantStartHz = 220.0, formantEndHz = 590.0, wobbleHz = 7.2, seed = 727)
    addWetSquelch(buf, 0.52, 0.58, pitch * 0.92, gain = 0.66, formantStartHz = 440.0, formantEndHz = 155.0, wobbleHz = 5.4, seed = 733)
    addBubbleCluster(buf, 0.16, 0.74, count = 11, baseFreqHz = 165.0 * pitch, gain = 0.30, seed = 743)

    return toPcm16(buf, 0.85f, roomWet = 0.18f)
  }

  /** Suture Scar Node: surgical thread dragging taut through damp flesh + crisp tendon cinch. */
  private fun renderScarStitch(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.38).toInt()
    val buf = FloatArray(len)
    addTendonTear(buf, 0.0, 0.30, pitch * 1.05, gain = 0.74, fiberCount = 15, seed = 808)
    addWetSquelch(buf, 0.06, 0.26, pitch, gain = 0.44, formantStartHz = 520.0, formantEndHz = 220.0, wobbleHz = 9.0, seed = 811)
    return toPcm16(buf, 0.80f, roomWet = 0.14f)
  }

  /** Heavy rusted cast-iron floor sump grate latch slam & metallic ring. */
  private fun renderIronClank(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.34).toInt()
    val buf = FloatArray(len)
    addIronModalRing(buf, 0.0, 0.32, 205.0 * pitch, gain = 0.78, decayScale = 0.92, seed = 909, includeRatchetGrit = true)
    addIronModalRing(buf, 0.048, 0.22, 290.0 * pitch, gain = 0.34, decayScale = 1.25, seed = 919, includeRatchetGrit = false)
    return toPcm16(buf, 0.84f, roomWet = 0.20f)
  }

  /** Submerged rusted iron chain-link haul & dark viscous sump water slosh. */
  private fun renderSumpDredge(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.56).toInt()
    val buf = FloatArray(len)
    addWetSquelch(buf, 0.0, 0.50, pitch, gain = 0.64, formantStartHz = 280.0, formantEndHz = 460.0, wobbleHz = 6.8, seed = 1010)
    addBubbleCluster(buf, 0.04, 0.42, count = 8, baseFreqHz = 160.0 * pitch, gain = 0.28, seed = 1013)
    addIronModalRing(buf, 0.02, 0.20, 320.0 * pitch, gain = 0.42, decayScale = 1.4, seed = 1019, includeRatchetGrit = false)
    addIronModalRing(buf, 0.16, 0.24, 255.0 * pitch, gain = 0.46, decayScale = 1.2, seed = 1021, includeRatchetGrit = false)
    return toPcm16(buf, 0.84f, roomWet = 0.20f)
  }

  /** Deeply muffled triple knuckle-knock behind thick wet masonry. */
  private fun renderEchoTripleKnock(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.62).toInt()
    val buf = FloatArray(len)
    val offsets = doubleArrayOf(0.0, 0.16, 0.325)
    val freqs = doubleArrayOf(96.0, 88.0, 82.0)
    val amps = doubleArrayOf(0.66, 0.52, 0.40)
    val noise = NoiseGen(1061)
    val masonryFilter = SvFilter()
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val n = noise.nextFloat().toDouble()
      var sum = 0.0
      for (k in offsets.indices) {
        val dt = t - offsets[k]
        if (dt >= 0.0) {
          val f = (freqs[k] * exp(-dt * 7.5)).coerceAtLeast(44.0) * pitch
          val tone = sin(TWO_PI * f * dt) * 0.72 + sin(TWO_PI * (f * 1.56) * dt) * 0.28
          val thumpNoise = masonryFilter.processLowpass(n, 240.0, 1.2) * exp(-dt * 34.0) * 0.55
          sum += (tone * exp(-dt * 20.0) + thumpNoise) * amps[k]
        }
      }
      buf[i] = sum.toFloat()
    }
    return toPcm16(buf, 0.78f, roomWet = 0.22f)
  }

  /** Massive iron jaws bolting shut over the valve with cold metallic locking ring. */
  private fun renderClampLock(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.44).toInt()
    val buf = FloatArray(len)
    addIronModalRing(buf, 0.0, 0.40, 185.0 * pitch, gain = 0.80, decayScale = 0.85, seed = 1111, includeRatchetGrit = true)
    addIronModalRing(buf, 0.095, 0.30, 278.0 * pitch, gain = 0.52, decayScale = 1.05, seed = 1117, includeRatchetGrit = false)
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val slam = sin(TWO_PI * ((72.0 - t * 80.0).coerceAtLeast(34.0) * pitch) * t) * exp(-t * 10.5) * 0.65
      buf[i] += slam.toFloat()
    }
    return toPcm16(buf, 0.86f, roomWet = 0.22f)
  }

  /** Iron clamp release snapping open into a cavernous tearing marrow & wet tissue bloom. */
  private fun renderMarrowBloom(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.78).toInt()
    val buf = FloatArray(len)
    addIronModalRing(buf, 0.0, 0.28, 225.0 * pitch, gain = 0.52, decayScale = 1.15, seed = 1212, includeRatchetGrit = true)
    addTendonTear(buf, 0.04, 0.48, pitch * 0.90, gain = 0.76, fiberCount = 22, seed = 1217)
    addWetSquelch(buf, 0.08, 0.64, pitch, gain = 0.74, formantStartHz = 520.0, formantEndHz = 150.0, wobbleHz = 6.5, seed = 1223)
    addBubbleCluster(buf, 0.14, 0.52, count = 9, baseFreqHz = 160.0 * pitch, gain = 0.28, seed = 1229)
    return toPcm16(buf, 0.86f, roomWet = 0.20f)
  }

  /** Cavernous wet esophageal swallow + mucosal squelch + calcified tooth settle. */
  private fun renderThroatSwallow(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.60).toInt()
    val buf = FloatArray(len)
    addWetSquelch(buf, 0.0, 0.52, pitch * 0.92, gain = 0.82, formantStartHz = 460.0, formantEndHz = 140.0, wobbleHz = 5.5, seed = 1260)
    addBubbleCluster(buf, 0.08, 0.38, count = 7, baseFreqHz = 145.0 * pitch, gain = 0.30, seed = 1265)
    addTendonTear(buf, 0.22, 0.24, pitch * 0.86, gain = 0.44, fiberCount = 12, seed = 1271)
    return toPcm16(buf, 0.82f, roomWet = 0.18f)
  }

  /** Heavy cold surgical iron shear slicing straight through thick sinew into a hollow collapse. */
  private fun renderExcisionShear(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.96).toInt()
    val buf = FloatArray(len)
    // Heavy cold iron shears biting shut
    addIronModalRing(buf, 0.0, 0.46, 265.0 * pitch, gain = 0.68, decayScale = 0.82, seed = 1313, includeRatchetGrit = true)
    // Thick sinew & flesh ripping apart under the shear
    addTendonTear(buf, 0.04, 0.56, pitch * 0.88, gain = 0.82, fiberCount = 26, seed = 1319)
    addWetSquelch(buf, 0.18, 0.66, pitch * 0.86, gain = 0.58, formantStartHz = 380.0, formantEndHz = 120.0, wobbleHz = 4.8, seed = 1327)
    var subPhase = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val f = (72.0 * exp(-t * 2.2) + 29.0) * pitch
      subPhase += TWO_PI * f / SAMPLE_RATE
      buf[i] += (sin(subPhase) * exp(-t * 2.6) * 0.62).toFloat()
    }
    return toPcm16(buf, 0.86f, roomWet = 0.22f)
  }

  /** Low mournful damp seam sigh dissolving into muffled exterior rain (Arc 1 -> Arc 2). */
  private fun renderPleadSalvation(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.98).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1414)
    val rainFilter = SvFilter()
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val sighEnv = sin(PI * (t / 0.98).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val lowChord =
        (sin(TWO_PI * (54.0 * pitch) * t) * 0.54 + sin(TWO_PI * (81.0 * pitch) * t) * 0.30) *
          sighEnv
      val rainSwell = rainFilter.processBandpass(noise.nextFloat().toDouble(), 620.0, 0.9) *
        (t / 0.98) * exp(-t * 1.2) * 0.72
      buf[i] = (lowChord + rainSwell).toFloat()
    }
    return toPcm16(buf, 0.78f, roomWet = 0.20f)
  }

  /** Quiet, low-register cistern condensation drop announcing a newly unlocked Story fragment. */
  private fun renderStoryCondensationDrip(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.28).toInt()
    val buf = FloatArray(len)
    addBubbleCluster(buf, 0.0, 0.04, count = 1, baseFreqHz = 225.0 * pitch, gain = 0.52, seed = 1451)
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val cistern = sin(TWO_PI * (122.0 * pitch) * t) * exp(-t * 14.0) * (1.0 - exp(-t * 90.0)) * 0.26
      buf[i] += cistern.toFloat()
    }
    return toPcm16(buf, 0.50f, roomWet = 0.24f)
  }

  /** Deep brooding architectural groan + wet masonry shift when advancing a Phase. */
  private fun renderPhaseShift(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.78).toInt()
    val buf = FloatArray(len)
    addWetSquelch(buf, 0.06, 0.62, pitch * 0.85, gain = 0.42, formantStartHz = 180.0, formantEndHz = 310.0, wobbleHz = 3.5, seed = 1470)
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val env = sin(PI * (t / 0.78).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val sub = sin(TWO_PI * (46.0 * pitch) * t) * 0.58 + sin(TWO_PI * (69.0 * pitch) * t) * 0.26
      buf[i] += (sub * env).toFloat()
    }
    return toPcm16(buf, 0.72f, roomWet = 0.22f)
  }

  /** Deep heavy muffled wooden desk stamp thump + dark blotter slap (Arc 2). */
  private fun renderDeskStamp(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.22).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1515)
    val woodFilter = SvFilter()
    var subPhase = 0.0
    var bodyPhase = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val subF = (36.0 + 48.0 * exp(-t * 30.0)) * pitch
      val bodyF = (56.0 + 72.0 * exp(-t * 36.0)) * pitch
      subPhase += TWO_PI * subF / SAMPLE_RATE
      bodyPhase += TWO_PI * bodyF / SAMPLE_RATE
      val attack = 1.0 - exp(-t * 240.0)
      val woodSub = sin(subPhase) * attack * exp(-t * 16.0) * 0.88
      val woodBody = sin(bodyPhase) * attack * exp(-t * 24.0) * 0.34
      val paper = woodFilter.processLowpass(noise.nextFloat().toDouble(), 340.0, 1.25) * exp(-t * 32.0) * 0.48
      buf[i] = (woodSub + woodBody + paper).toFloat()
    }
    return toPcm16(buf, 0.84f, roomWet = 0.12f)
  }

  /** Heavy damp manila folder slide + low desk drawer tap (Arc 2). */
  private fun renderPaperFolder(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.28).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1616)
    val paperFilter = SvFilter()
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val env = sin(PI * (t / 0.28).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val paper = paperFilter.processBandpass(noise.nextFloat().toDouble(), 680.0 - t * 900.0, 1.8) * env * 0.78
      val tap = sin(TWO_PI * ((142.0 - t * 260.0).coerceAtLeast(72.0) * pitch) * t) * exp(-t * 28.0) * 0.36
      buf[i] = (paper + tap).toFloat()
    }
    return toPcm16(buf, 0.74f, roomWet = 0.10f)
  }

  /** Corkboard pin press + dark damped bass red-string pluck (Arc 2). */
  private fun renderRedStringPin(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.30).toInt()
    val buf = FloatArray(len)
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val twang =
        (sin(TWO_PI * (158.0 * pitch) * t) * 0.56 + sin(TWO_PI * (237.0 * pitch) * t) * 0.24) *
          exp(-t * 12.5) * (1.0 - exp(-t * 140.0))
      val tack = sin(TWO_PI * ((290.0 - t * 980.0).coerceAtLeast(110.0) * pitch) * t) * exp(-t * 42.0) * 0.32
      buf[i] = (twang + tack).toFloat()
    }
    return toPcm16(buf, 0.74f, roomWet = 0.12f)
  }

  /** Damp envelope slit / muted brass reel latch (Arc 2). */
  private fun renderDeskAnomaly(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.28).toInt()
    val buf = FloatArray(len)
    addTendonTear(buf, 0.0, 0.22, pitch * 1.12, gain = 0.52, fiberCount = 12, seed = 1717)
    addIronModalRing(buf, 0.04, 0.18, 340.0 * pitch, gain = 0.26, decayScale = 1.6, seed = 1721, includeRatchetGrit = false)
    return toPcm16(buf, 0.76f, roomWet = 0.12f)
  }

  /** Heavy case folder closed / chapter connected (Arc 2). */
  private fun renderCaseChapterThud(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.44).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1818)
    val filter = SvFilter()
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val thud = sin(TWO_PI * ((76.0 - t * 110.0).coerceAtLeast(38.0) * pitch) * t) * exp(-t * 10.5) * 0.76
      val rustle = filter.processLowpass(noise.nextFloat().toDouble(), 440.0, 1.2) * exp(-t * 14.0) * 0.52
      buf[i] = (thud + rustle).toFloat()
    }
    return toPcm16(buf, 0.78f, roomWet = 0.14f)
  }

  /** File #00 reveal — desk folder giving way to cold subterranean chamber grout underneath. */
  private fun renderFileZeroReveal(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.96).toInt()
    val buf = FloatArray(len)
    addWetSquelch(buf, 0.08, 0.74, pitch * 0.82, gain = 0.38, formantStartHz = 220.0, formantEndHz = 135.0, wobbleHz = 3.2, seed = 1919)
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val env = sin(PI * (t / 0.96).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val drone =
        (sin(TWO_PI * (42.0 * pitch) * t) * 0.62 + sin(TWO_PI * (63.0 * pitch) * t) * 0.32) * env
      buf[i] += drone.toFloat()
    }
    return toPcm16(buf, 0.80f, roomWet = 0.22f)
  }

  /** Arc 3 hollow merged subterranean threshold resonance. */
  private fun renderConvergenceTone(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.84).toInt()
    val buf = FloatArray(len)
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val env = sin(PI * (t / 0.84).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val tone =
        sin(TWO_PI * (50.0 * pitch) * t) * 0.54 + sin(TWO_PI * (75.0 * pitch) * t) * 0.24
      buf[i] = (tone * env).toFloat()
    }
    return toPcm16(buf, 0.70f, roomWet = 0.24f)
  }

  /**
   * Stateful streaming room-ambience generator tuned for a deep, closed-off subterranean chamber.
   * Smoothly slews mix parameters so phase shifts, Stillness entry, Valve Clamp tension, and
   * Arc 2 rain transitions crossfade with zero clicks.
   */
  class AmbientBedGenerator(private val sampleRate: Int = SAMPLE_RATE) {
    private val noise = NoiseGen(424242)
    private var sampleCursor: Long = 0L
    private var rainLp = 0.0
    private var rainHpPrev = 0.0
    private var fleshLp = 0.0

    // Smoothed parameters
    private var sHum = 0.0
    private var sFlesh = 0.0
    private var sCardiac = 0.0
    private var sClamp = 0.0
    private var sRain = 0.0
    private var sStill = 0.0

    // Irregular subterranean condensation drip oscillator state (low, muted cistern droplets)
    private var samplesUntilNextDrip: Int = (sampleRate * 1.4).toInt()
    private var activeDripSamples: Int = 0
    private var activeDripAge: Int = 0
    private var activeDripBaseFreq: Double = 240.0
    private var activeDripGain: Double = 0.0

    fun fillChunk(buffer: ShortArray, profile: AmbientSoundProfile) {
      val slew = 0.00045
      for (i in buffer.indices) {
        sHum += slew * (profile.humGain - sHum)
        sFlesh += slew * (profile.fleshDepth - sFlesh)
        sCardiac += slew * (profile.cardiacPulse - sCardiac)
        sClamp += slew * (profile.clampTension - sClamp)
        sRain += slew * (profile.rainMix - sRain)
        sStill += slew * (profile.stillnessFocus - sStill)

        val t = sampleCursor.toDouble() / sampleRate
        sampleCursor++

        // 1. Deep muted 50Hz / 100Hz subterranean fluorescent ballast hum
        val hum =
          (sin(TWO_PI * 50.0 * t) * 0.68 + sin(TWO_PI * 100.0 * t) * 0.18) * sHum * 0.12

        // 2. Slow damp subterranean peristalsis & Valve Clamp sub-tension
        val rawNoise = noise.nextFloat().toDouble()
        fleshLp += 0.011 * (rawNoise - fleshLp)
        val breathLfo = 0.55 + 0.45 * sin(TWO_PI * 0.15 * t)
        val flesh =
          (sin(TWO_PI * 36.0 * t) * 0.60 + fleshLp * 1.35) * (sFlesh + sClamp * 0.55) * breathLfo * 0.14

        // 3. Phase 4 deep systolic/diastolic cardiac double-beat (~0.72Hz cycle)
        var cardiac = 0.0
        if (sCardiac > 0.01) {
          val cycleT = (t * 0.72) % 1.0
          val beat1 = exp(-cycleT * 24.0)
          val beat2 = if (cycleT > 0.22) exp(-(cycleT - 0.22) * 28.0) * 0.68 else 0.0
          cardiac = sin(TWO_PI * 44.0 * t) * (beat1 + beat2) * sCardiac * 0.18
        }

        // 4. Arc 2 muffled rain behind thick cold window glass
        val rainHp = rawNoise - rainHpPrev * 0.64
        rainHpPrev = rawNoise
        rainLp += 0.11 * (rainHp - rainLp)
        val rain = rainLp * sRain * (0.20 - sStill * 0.04)

        // 5. Sparse, muted low-register cistern water drips (never shrill)
        var drip = 0.0
        if (activeDripSamples > 0) {
          val dt = activeDripAge.toDouble() / sampleRate
          val f = (activeDripBaseFreq + dt * 850.0).coerceAtMost(420.0)
          drip = sin(TWO_PI * f * dt) * exp(-dt * 34.0) * activeDripGain
          activeDripAge++
          if (activeDripAge >= activeDripSamples) {
            activeDripSamples = 0
          }
        } else if (profile.dripRate > 0.02f) {
          samplesUntilNextDrip--
          if (samplesUntilNextDrip <= 0) {
            val r = (noise.nextFloat() + 1f) * 0.5f // 0..1
            activeDripBaseFreq = 210.0 + r * 115.0
            activeDripGain = 0.07 + profile.dripRate * 0.08 + sStill * 0.045
            activeDripSamples = (sampleRate * 0.16).toInt()
            activeDripAge = 0
            val minGap = (1.2 - profile.dripRate * 0.55).coerceAtLeast(0.50)
            val maxGap = (4.4 - profile.dripRate * 2.1).coerceAtLeast(1.3)
            val gapSec = minGap + ((noise.nextFloat() + 1f) * 0.5f) * (maxGap - minGap)
            samplesUntilNextDrip = (sampleRate * gapSec).toInt()
          }
        }

        val mix = tanh((hum + flesh + cardiac + rain + drip) * 1.15).toFloat().coerceIn(-0.6f, 0.6f)
        buffer[i] = (mix * Short.MAX_VALUE).toInt().toShort()
      }
    }
  }
}
