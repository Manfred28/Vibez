package com.example.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

internal const val FOLEY_SAMPLE_RATE: Int = 22050
internal const val TWO_PI: Double = 2.0 * PI

// Inharmonic cast-iron wheel ring modal ratios (authentic cast-iron clank/clang spectrum)
private val IRON_MODE_RATIOS = doubleArrayOf(1.00, 1.53, 2.24, 2.89, 3.71, 4.68)
private val IRON_MODE_AMPS = doubleArrayOf(0.50, 0.48, 0.44, 0.36, 0.28, 0.20)
private val IRON_MODE_DECAYS = doubleArrayOf(10.5, 13.0, 16.5, 21.0, 27.0, 34.0)

/** Deterministic xorshift32 noise generator for reproducible procedural foley synthesis. */
internal class NoiseGen(seed: Int) {
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
internal class SvFilter {
  private var low = 0.0
  private var band = 0.0

  fun processBandpass(input: Double, freqHz: Double, q: Double): Double {
    val clampedFreq = freqHz.coerceIn(45.0, FOLEY_SAMPLE_RATE * 0.36)
    val f = (2.0 * sin(PI * clampedFreq / FOLEY_SAMPLE_RATE)).coerceIn(0.01, 1.15)
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
internal fun applyChamberSlapback(samples: FloatArray, wetAmount: Float = 0.14f): FloatArray {
  val d1 = (FOLEY_SAMPLE_RATE * 0.019).toInt()
  val d2 = (FOLEY_SAMPLE_RATE * 0.035).toInt()
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

internal fun toPcm16(samples: FloatArray, masterGain: Float, roomWet: Float = 0.14f): ShortArray {
  val spatial = if (roomWet > 0f) applyChamberSlapback(samples, roomWet) else samples
  val out = ShortArray(spatial.size)
  for (i in spatial.indices) {
    val shaped = tanh((spatial[i] * masterGain).toDouble()).toFloat()
    val clamped = shaped.coerceIn(-0.95f, 0.95f)
    out[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
  }
  return out
}

/**
 * Adds authentic struck/ratcheted cast-iron resonance: 6 inharmonic metallic modes + gritty
 * metal-on-metal pawl friction + heavy wall-housing impact weight.
 */
internal fun addIronModalRing(
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
  val startIdx = (startSec * FOLEY_SAMPLE_RATE).toInt().coerceAtLeast(0)
  val count = (durationSec * FOLEY_SAMPLE_RATE).toInt().coerceAtLeast(1)
  val endIdx = (startIdx + count).coerceAtMost(buf.size)
  val noise = NoiseGen(seed)
  val gritFilter = SvFilter()
  val clangFilter = SvFilter()
  val attackRate = if (clangScale < 0.5) 220.0 else 420.0

  for (i in startIdx until endIdx) {
    val t = (i - startIdx).toDouble() / FOLEY_SAMPLE_RATE
    val attack = 1.0 - exp(-t * attackRate)
    var modes = 0.0
    for (m in IRON_MODE_RATIOS.indices) {
      val f = baseFreqHz * IRON_MODE_RATIOS[m]
      // Subtle pitch sag on initial iron impact gives massive physical weight
      val sag = 1.0 + 0.025 * exp(-t * 48.0)
      modes +=
        sin(TWO_PI * f * sag * t) * exp(-t * IRON_MODE_DECAYS[m] * decayScale) * IRON_MODE_AMPS[m]
    }
    val n = noise.nextFloat().toDouble()
    val clangBurst =
      clangFilter.processBandpass(n, (baseFreqHz * 4.2).coerceIn(500.0, 2900.0), 3.4) *
        exp(-t * 58.0) *
        0.64 *
        clangScale
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
internal fun addTendonTear(
  buf: FloatArray,
  startSec: Double,
  durationSec: Double,
  pitch: Double,
  gain: Double,
  fiberCount: Int = 18,
  seed: Int = 311,
) {
  val startIdx = (startSec * FOLEY_SAMPLE_RATE).toInt().coerceAtLeast(0)
  val count = (durationSec * FOLEY_SAMPLE_RATE).toInt().coerceAtLeast(1)
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
    val t = (i - startIdx).toDouble() / FOLEY_SAMPLE_RATE
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
    val fiberFreq =
      (1220.0 + (1.0 - frac * 0.45) * 920.0 + wobble * 1.6).coerceIn(620.0, 2900.0) * pitch
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
internal fun addWetSquelch(
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
  val startIdx = (startSec * FOLEY_SAMPLE_RATE).toInt().coerceAtLeast(0)
  val count = (durationSec * FOLEY_SAMPLE_RATE).toInt().coerceAtLeast(1)
  val endIdx = (startIdx + count).coerceAtMost(buf.size)
  val noise = NoiseGen(seed)
  val f1 = SvFilter()
  val f2 = SvFilter()
  var crackleEnv = 0.0

  for (i in startIdx until endIdx) {
    val t = (i - startIdx).toDouble() / FOLEY_SAMPLE_RATE
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
    val freq2 =
      (freq1 * 2.15 + 140.0 * cos(TWO_PI * (wobbleHz * 1.35) * t)).coerceIn(320.0, 2600.0)

    val cavity1 = f1.processBandpass(wetSource, freq1, 5.8) * 0.85
    val cavity2 = f2.processBandpass(wetSource, freq2, 6.4) * 0.52
    val fleshSub =
      sin(TWO_PI * ((78.0 - 24.0 * frac) * pitch) * t) * env * (0.7 + 0.3 * lfo) * 0.38

    buf[i] += (((cavity1 + cavity2) * env + fleshSub) * gain).toFloat()
  }
}

/**
 * Adds a staggered cluster of wet cavity bubble pops (soft attack, organic upward pitch chirp)
 * that instantly sells thick liquid mucosa, blister rupture, and submerged suction.
 */
internal fun addBubbleCluster(
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
    val sIdx = (bStart * FOLEY_SAMPLE_RATE).toInt().coerceAtLeast(0)
    val eIdx = ((bStart + bDur) * FOLEY_SAMPLE_RATE).toInt().coerceAtMost(buf.size)
    var phaseAcc = 0.0
    for (i in sIdx until eIdx) {
      val dt = (i - sIdx).toDouble() / FOLEY_SAMPLE_RATE
      val u = (dt / bDur).coerceIn(0.0, 1.0)
      val env = sin(PI * u) * exp(-u * 1.4)
      val f = f0 * (1.0 + u * u * 1.05)
      phaseAcc += TWO_PI * f / FOLEY_SAMPLE_RATE
      buf[i] += (sin(phaseAcc) * env * bGain).toFloat()
    }
  }
}
