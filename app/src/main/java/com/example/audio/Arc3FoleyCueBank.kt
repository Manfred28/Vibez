package com.example.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Procedural foley cue bank for Arc 3 ("The Unmaking"):
 * Reverse iron wrenching, specimen jar glass shatter, tearing chamber apparatus & scar sutures,
 * urgent wall rupture pops, driving Arc 2 Memory Wedges, clotting warning pulse, and the 5
 * deliberate Final Pulls dissolving into the Abstract Crimson Flood.
 */
internal object Arc3FoleyCueBank {

  fun synthesize(cue: SoundCue, pitch: Double): ShortArray? =
    when (cue) {
      SoundCue.ARC3_WRENCH -> renderReverseWrench(pitch)
      SoundCue.ARC3_JAR_SHATTER -> renderJarShatter(pitch)
      SoundCue.ARC3_UNMAKE_TEAR -> renderUnmakeTear(pitch)
      SoundCue.ARC3_RUPTURE_POP -> renderRupturePop(pitch)
      SoundCue.ARC3_WEDGE_DRIVE -> renderWedgeDrive(pitch)
      SoundCue.ARC3_CLOT_WARNING -> renderClotWarning(pitch)
      SoundCue.ARC3_FINAL_PULL -> renderFinalPull(pitch)
      SoundCue.ARC3_SUBMERSION_END -> renderSubmersionEnd(pitch)
      else -> null
    }

  /** Reverse iron wrenching: grinding counter-clockwise cast-iron pawl strain + wet grout crack. */
  private fun renderReverseWrench(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.25).toInt()
    val buf = FloatArray(len)
    addIronModalRing(
      buf,
      startSec = 0.0,
      durationSec = 0.16,
      baseFreqHz = 215.0 * pitch,
      gain = 0.48,
      decayScale = 1.65,
      seed = 3101,
      includeRatchetGrit = true,
      clangScale = 0.85,
    )
    addWetSquelch(
      buf,
      startSec = 0.035,
      durationSec = 0.18,
      pitch = pitch * 0.94,
      gain = 0.44,
      formantStartHz = 210.0,
      formantEndHz = 470.0,
      wobbleHz = 9.5,
      seed = 3109,
    )
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val torqueThump =
        sin(TWO_PI * ((72.0 - t * 95.0).coerceAtLeast(36.0) * pitch) * t) *
          (1.0 - exp(-t * 180.0)) *
          exp(-t * 17.0) *
          0.56
      buf[i] += torqueThump.toFloat()
    }
    return toPcm16(buf, 0.84f, roomWet = 0.16f)
  }

  /** Shattering an Arc 1 Specimen Jar: sharp vitreous glass crack + cascading shard tinkles + wet brine spill. */
  private fun renderJarShatter(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.46).toInt()
    val buf = FloatArray(len)
    val rng = NoiseGen(3203)
    val shardFilter = SvFilter()
    // High inharmonic glass impact transients
    val glassFreqs = doubleArrayOf(980.0, 1490.0, 2140.0, 2860.0, 3520.0)
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      var crack = 0.0
      if (t < 0.14) {
        val env = (1.0 - exp(-t * 520.0)) * exp(-t * 32.0)
        for (idx in glassFreqs.indices) {
          val f = glassFreqs[idx] * pitch
          crack += sin(TWO_PI * f * t) * env * (0.28 - idx * 0.035)
        }
      }
      val burst =
        shardFilter.processBandpass(rng.nextFloat().toDouble(), 2400.0 - t * 2200.0, 1.8) *
          exp(-t * 18.0) *
          0.52
      buf[i] += (crack + burst).toFloat()
    }
    // Cascading secondary shard clinks on tile
    val shardOffsets = doubleArrayOf(0.045, 0.088, 0.135, 0.195, 0.265)
    for (k in shardOffsets.indices) {
      addIronModalRing(
        buf,
        startSec = shardOffsets[k],
        durationSec = 0.09,
        baseFreqHz = (760.0 + k * 145.0) * pitch,
        gain = 0.24 - k * 0.03,
        decayScale = 3.2,
        seed = 3217 + k * 19,
        includeRatchetGrit = false,
        clangScale = 0.55,
      )
    }
    // Brine & ichor splashing across tile
    addWetSquelch(
      buf,
      startSec = 0.06,
      durationSec = 0.34,
      pitch = pitch,
      gain = 0.42,
      formantStartHz = 540.0,
      formantEndHz = 210.0,
      wobbleHz = 8.4,
      seed = 3251,
    )
    addBubbleCluster(buf, 0.12, 0.24, 7, 360.0 * pitch, gain = 0.26, seed = 3259)
    return toPcm16(buf, 0.82f, roomWet = 0.18f)
  }

  /** Reversing apparatus or unstitching a chamber scar: ripping fascia + groaning cast iron + wet surge. */
  private fun renderUnmakeTear(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.54).toInt()
    val buf = FloatArray(len)
    addTendonTear(
      buf,
      startSec = 0.0,
      durationSec = 0.38,
      pitch = pitch * 0.92,
      gain = 0.72,
      fiberCount = 24,
      seed = 3307,
    )
    addIronModalRing(
      buf,
      startSec = 0.02,
      durationSec = 0.24,
      baseFreqHz = 175.0 * pitch,
      gain = 0.34,
      decayScale = 1.4,
      seed = 3313,
      includeRatchetGrit = true,
      clangScale = 0.62,
    )
    addWetSquelch(
      buf,
      startSec = 0.14,
      durationSec = 0.36,
      pitch = pitch * 0.86,
      gain = 0.54,
      formantStartHz = 360.0,
      formantEndHz = 155.0,
      wobbleHz = 6.5,
      seed = 3323,
    )
    return toPcm16(buf, 0.84f, roomWet = 0.20f)
  }

  /** Tearing an active wall rupture: pressurized blister burst + sharp wet snap + crimson gush. */
  private fun renderRupturePop(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.38).toInt()
    val buf = FloatArray(len)
    addTendonTear(
      buf,
      startSec = 0.0,
      durationSec = 0.18,
      pitch = pitch * 1.12,
      gain = 0.56,
      fiberCount = 14,
      seed = 3407,
    )
    addWetSquelch(
      buf,
      startSec = 0.04,
      durationSec = 0.30,
      pitch = pitch * 0.96,
      gain = 0.62,
      formantStartHz = 480.0,
      formantEndHz = 195.0,
      wobbleHz = 10.5,
      seed = 3413,
    )
    addBubbleCluster(buf, 0.05, 0.22, 9, 290.0 * pitch, gain = 0.34, seed = 3433)
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val pop =
        sin(TWO_PI * ((190.0 * exp(-t * 28.0) + 48.0) * pitch) * t) *
          exp(-t * 22.0) *
          0.68
      buf[i] += pop.toFloat()
    }
    return toPcm16(buf, 0.86f, roomWet = 0.18f)
  }

  /** Driving an Arc 2 Memory Wedge into the breach: colossal iron-and-masonry lock + wet wall recoil. */
  private fun renderWedgeDrive(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.86).toInt()
    val buf = FloatArray(len)
    addIronModalRing(
      buf,
      startSec = 0.0,
      durationSec = 0.44,
      baseFreqHz = 142.0 * pitch,
      gain = 0.66,
      decayScale = 0.95,
      seed = 3511,
      includeRatchetGrit = false,
      clangScale = 1.05,
    )
    addTendonTear(
      buf,
      startSec = 0.06,
      durationSec = 0.48,
      pitch = pitch * 0.84,
      gain = 0.58,
      fiberCount = 26,
      seed = 3527,
    )
    addWetSquelch(
      buf,
      startSec = 0.16,
      durationSec = 0.62,
      pitch = pitch * 0.80,
      gain = 0.54,
      formantStartHz = 280.0,
      formantEndHz = 125.0,
      wobbleHz = 4.6,
      seed = 3539,
    )
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val hammer =
        sin(TWO_PI * ((62.0 * exp(-t * 8.0) + 32.0) * pitch) * t) *
          (1.0 - exp(-t * 240.0)) *
          exp(-t * 4.2) *
          0.78
      val lockFifth =
        sin(TWO_PI * (96.0 * pitch) * t) *
          (1.0 - exp(-t * 60.0)) *
          exp(-t * 3.8) *
          0.32
      buf[i] += (hammer + lockFifth).toFloat()
    }
    return toPcm16(buf, 0.88f, roomWet = 0.24f)
  }

  /** Clotting warning: low viscous suction draw reminding the player the seam is knitting shut. */
  private fun renderClotWarning(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.42).toInt()
    val buf = FloatArray(len)
    addWetSquelch(
      buf,
      startSec = 0.0,
      durationSec = 0.38,
      pitch = pitch * 0.78,
      gain = 0.48,
      formantStartHz = 145.0,
      formantEndHz = 295.0,
      wobbleHz = 4.2,
      seed = 3607,
    )
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val env = sin(PI * (t / 0.42).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val draw = sin(TWO_PI * ((44.0 + t * 28.0) * pitch) * t) * env * 0.44
      buf[i] += draw.toFloat()
    }
    return toPcm16(buf, 0.72f, roomWet = 0.20f)
  }

  /** Finale interactive Final Pulls (1..4): immense slow hydraulic heave + ripping membrane + muffled pulse. */
  private fun renderFinalPull(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.92).toInt()
    val buf = FloatArray(len)
    addTendonTear(
      buf,
      startSec = 0.0,
      durationSec = 0.64,
      pitch = pitch * 0.76,
      gain = 0.64,
      fiberCount = 28,
      seed = 3701,
    )
    addWetSquelch(
      buf,
      startSec = 0.12,
      durationSec = 0.74,
      pitch = pitch * 0.72,
      gain = 0.62,
      formantStartHz = 240.0,
      formantEndHz = 105.0,
      wobbleHz = 3.2,
      seed = 3719,
    )
    addBubbleCluster(buf, 0.20, 0.56, 10, 195.0 * pitch, gain = 0.30, seed = 3733)
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val env = sin(PI * (t / 0.92).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val heave =
        (sin(TWO_PI * (38.0 * pitch) * t) * 0.68 + sin(TWO_PI * (57.0 * pitch) * t) * 0.30) * env
      buf[i] += heave.toFloat()
    }
    return toPcm16(buf, 0.86f, roomWet = 0.28f)
  }

  /** Final Pull 5 / Complete Submersion: warm underwater low-pass flood overtaking everything into a lone muffled heartbeat. */
  private fun renderSubmersionEnd(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 1.45).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(3803)
    val floodFilter = SvFilter()
    addWetSquelch(
      buf,
      startSec = 0.0,
      durationSec = 0.90,
      pitch = pitch * 0.68,
      gain = 0.56,
      formantStartHz = 290.0,
      formantEndHz = 92.0,
      wobbleHz = 2.4,
      seed = 3821,
    )
    addBubbleCluster(buf, 0.10, 0.82, 12, 165.0 * pitch, gain = 0.26, seed = 3833)
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val frac = (t / 1.45).coerceIn(0.0, 1.0)
      val rushEnv = sin(PI * frac).coerceAtLeast(0.0)
      val cutoff = (460.0 * exp(-frac * 2.2) + 85.0)
      val rush = floodFilter.processLowpass(noise.nextFloat().toDouble(), cutoff, 1.4) * rushEnv * 0.65
      // One deep muffled submerged double-beat at the end
      val beat1 = if (t >= 0.68) exp(-(t - 0.68) * 18.0) else 0.0
      val beat2 = if (t >= 0.92) exp(-(t - 0.92) * 20.0) * 0.72 else 0.0
      val heartbeat = sin(TWO_PI * (40.0 * pitch) * t) * (beat1 + beat2) * 0.72
      val subDrone = sin(TWO_PI * (34.0 * pitch) * t) * rushEnv * 0.44
      buf[i] += (rush + heartbeat + subDrone).toFloat()
    }
    return toPcm16(buf, 0.88f, roomWet = 0.32f)
  }
}
