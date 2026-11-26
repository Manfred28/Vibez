package com.example.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Procedural foley cue bank for Arc 2 (The Inquiry desk & corkboard) and Arc 3 threshold. */
internal object Arc2FoleyCueBank {

  fun synthesize(cue: SoundCue, pitch: Double): ShortArray? =
    when (cue) {
      SoundCue.ARC2_STAMP -> renderDeskStamp(pitch)
      SoundCue.ARC2_LAMP_TOGGLE -> renderBrassPullChain(pitch)
      SoundCue.ARC2_CONTACT -> renderPaperFolder(pitch)
      SoundCue.ARC2_DEDUCTION -> renderRedStringPin(pitch)
      SoundCue.ARC2_STRING_WIRE -> renderRedStringPin(pitch * 1.08)
      SoundCue.ARC2_REEL_SCRUB -> renderReelScrub(pitch)
      SoundCue.ARC2_REDACTION_DISSOLVE -> renderSolventDissolve(pitch)
      SoundCue.ARC2_DESK_ANOMALY -> renderDeskAnomaly(pitch)
      SoundCue.ARC2_CHAPTER -> renderCaseChapterThud(pitch)
      SoundCue.ARC2_FILE_ZERO -> renderFileZeroReveal(pitch)
      SoundCue.ARC3_ENTER -> renderConvergenceTone(pitch)
      else -> null
    }

  /** Crisp brass desk-lamp pull-chain click + faint filament settle. */
  private fun renderBrassPullChain(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.16).toInt()
    val buf = FloatArray(len)
    addIronModalRing(buf, 0.0, 0.09, 620.0 * pitch, gain = 0.34, decayScale = 2.4, seed = 2111, includeRatchetGrit = false)
    addIronModalRing(buf, 0.032, 0.10, 480.0 * pitch, gain = 0.28, decayScale = 2.0, seed = 2119, includeRatchetGrit = false)
    return toPcm16(buf, 0.72f, roomWet = 0.10f)
  }

  /** Warped magnetic reel hiss + low veiled sub-surface thump (Case III Acoustic Scrub). */
  private fun renderReelScrub(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.56).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(2223)
    val hissFilter = SvFilter()
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val env = sin(PI * (t / 0.56).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val hiss = hissFilter.processBandpass(noise.nextFloat().toDouble(), 1100.0 - t * 650.0, 2.2) * env * 0.42
      val subThump = sin(TWO_PI * (52.0 * pitch) * t) * exp(-((t - 0.24) * (t - 0.24)) * 90.0) * 0.65
      buf[i] = (hiss + subThump).toFloat()
    }
    return toPcm16(buf, 0.76f, roomWet = 0.18f)
  }

  /** Cold damp solvent hiss lifting censor ink off municipal ledger paper (Case IV Redaction). */
  private fun renderSolventDissolve(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.48).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(2331)
    val filter = SvFilter()
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val env = (1.0 - exp(-t * 18.0)) * exp(-t * 4.5)
      val sizzle = filter.processBandpass(noise.nextFloat().toDouble(), 1650.0 - t * 1400.0, 1.5) * env * 0.52
      val paperSigh = sin(TWO_PI * (88.0 * pitch) * t) * exp(-t * 8.0) * 0.30
      buf[i] = (sizzle + paperSigh).toFloat()
    }
    return toPcm16(buf, 0.74f, roomWet = 0.16f)
  }

  /** Deep heavy muffled wooden desk stamp thump + dark blotter slap (Arc 2). */
  private fun renderDeskStamp(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.22).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1515)
    val woodFilter = SvFilter()
    var subPhase = 0.0
    var bodyPhase = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val subF = (36.0 + 48.0 * exp(-t * 30.0)) * pitch
      val bodyF = (56.0 + 72.0 * exp(-t * 36.0)) * pitch
      subPhase += TWO_PI * subF / FOLEY_SAMPLE_RATE
      bodyPhase += TWO_PI * bodyF / FOLEY_SAMPLE_RATE
      val attack = 1.0 - exp(-t * 240.0)
      val woodSub = sin(subPhase) * attack * exp(-t * 16.0) * 0.88
      val woodBody = sin(bodyPhase) * attack * exp(-t * 24.0) * 0.34
      val paper =
        woodFilter.processLowpass(noise.nextFloat().toDouble(), 340.0, 1.25) * exp(-t * 32.0) * 0.48
      buf[i] = (woodSub + woodBody + paper).toFloat()
    }
    return toPcm16(buf, 0.84f, roomWet = 0.12f)
  }

  /** Heavy damp manila folder slide + low desk drawer tap (Arc 2). */
  private fun renderPaperFolder(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.28).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1616)
    val paperFilter = SvFilter()
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val env = sin(PI * (t / 0.28).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val paper =
        paperFilter.processBandpass(noise.nextFloat().toDouble(), 680.0 - t * 900.0, 1.8) *
          env *
          0.78
      val tap =
        sin(TWO_PI * ((142.0 - t * 260.0).coerceAtLeast(72.0) * pitch) * t) * exp(-t * 28.0) * 0.36
      buf[i] = (paper + tap).toFloat()
    }
    return toPcm16(buf, 0.74f, roomWet = 0.10f)
  }

  /** Corkboard pin press + dark damped bass red-string pluck (Arc 2). */
  private fun renderRedStringPin(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.30).toInt()
    val buf = FloatArray(len)
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val twang =
        (sin(TWO_PI * (158.0 * pitch) * t) * 0.56 + sin(TWO_PI * (237.0 * pitch) * t) * 0.24) *
          exp(-t * 12.5) *
          (1.0 - exp(-t * 140.0))
      val tack =
        sin(TWO_PI * ((290.0 - t * 980.0).coerceAtLeast(110.0) * pitch) * t) * exp(-t * 42.0) * 0.32
      buf[i] = (twang + tack).toFloat()
    }
    return toPcm16(buf, 0.74f, roomWet = 0.12f)
  }

  /** Damp envelope slit / muted brass reel latch (Arc 2). */
  private fun renderDeskAnomaly(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.28).toInt()
    val buf = FloatArray(len)
    addTendonTear(buf, 0.0, 0.22, pitch * 1.12, gain = 0.52, fiberCount = 12, seed = 1717)
    addIronModalRing(
      buf,
      0.04,
      0.18,
      340.0 * pitch,
      gain = 0.26,
      decayScale = 1.6,
      seed = 1721,
      includeRatchetGrit = false,
    )
    return toPcm16(buf, 0.76f, roomWet = 0.12f)
  }

  /** Heavy case folder closed / chapter connected (Arc 2). */
  private fun renderCaseChapterThud(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.44).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1818)
    val filter = SvFilter()
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val thud =
        sin(TWO_PI * ((76.0 - t * 110.0).coerceAtLeast(38.0) * pitch) * t) * exp(-t * 10.5) * 0.76
      val rustle =
        filter.processLowpass(noise.nextFloat().toDouble(), 440.0, 1.2) * exp(-t * 14.0) * 0.52
      buf[i] = (thud + rustle).toFloat()
    }
    return toPcm16(buf, 0.78f, roomWet = 0.14f)
  }

  /** File #00 reveal — desk folder giving way to cold subterranean chamber grout underneath. */
  private fun renderFileZeroReveal(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.96).toInt()
    val buf = FloatArray(len)
    addWetSquelch(
      buf,
      0.08,
      0.74,
      pitch * 0.82,
      gain = 0.38,
      formantStartHz = 220.0,
      formantEndHz = 135.0,
      wobbleHz = 3.2,
      seed = 1919,
    )
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val env = sin(PI * (t / 0.96).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val drone =
        (sin(TWO_PI * (42.0 * pitch) * t) * 0.62 + sin(TWO_PI * (63.0 * pitch) * t) * 0.32) * env
      buf[i] += drone.toFloat()
    }
    return toPcm16(buf, 0.80f, roomWet = 0.22f)
  }

  /** Arc 3 hollow merged subterranean threshold resonance. */
  private fun renderConvergenceTone(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.84).toInt()
    val buf = FloatArray(len)
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val env = sin(PI * (t / 0.84).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val tone =
        sin(TWO_PI * (50.0 * pitch) * t) * 0.54 + sin(TWO_PI * (75.0 * pitch) * t) * 0.24
      buf[i] = (tone * env).toFloat()
    }
    return toPcm16(buf, 0.70f, roomWet = 0.24f)
  }
}
