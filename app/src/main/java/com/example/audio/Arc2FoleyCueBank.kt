package com.example.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Procedural foley cue bank for Arc 2 (The Inquiry desk & corkboard) and Arc 3 threshold. */
internal object Arc2FoleyCueBank {

  fun synthesize(cue: SoundCue, pitch: Double): ShortArray? =
    when (cue) {
      SoundCue.ARC2_STAMP -> renderDeskStamp(pitch)
      SoundCue.ARC2_LAMP_ON -> renderBrassPullChainOn(pitch)
      SoundCue.ARC2_LAMP_OFF -> renderBrassPullChainOff(pitch)
      SoundCue.ARC2_CONTACT_ARCHIVIST -> renderContactArchivist(pitch)
      SoundCue.ARC2_CONTACT_ORDERLY -> renderContactOrderly(pitch)
      SoundCue.ARC2_CONTACT_SCANNER -> renderContactScanner(pitch)
      SoundCue.ARC2_CONTACT_TRANSCRIBER -> renderContactTranscriber(pitch)
      SoundCue.ARC2_DEDUCTION -> renderRedStringPin(pitch)
      SoundCue.ARC2_CORROBORATION_LOCK -> renderCorroborationLock(pitch)
      SoundCue.ARC2_STRING_WIRE -> renderRedStringPin(pitch * 1.08)
      SoundCue.ARC2_STRING_UNWIRE -> renderStringUnwire(pitch)
      SoundCue.ARC2_REEL_SCRUB -> renderReelScrub(pitch)
      SoundCue.ARC2_REDACTION_DISSOLVE -> renderSolventDissolve(pitch)
      SoundCue.ARC2_ANOMALY_ENVELOPE -> renderAnomalyEnvelope(pitch)
      SoundCue.ARC2_ANOMALY_SPECTACLES -> renderAnomalySpectacles(pitch)
      SoundCue.ARC2_ANOMALY_REEL_TAPE -> renderAnomalyReelTape(pitch)
      SoundCue.ARC2_ANOMALY_ENAMEL -> renderAnomalyEnamel(pitch)
      SoundCue.ARC2_CHAPTER -> renderCaseChapterThud(pitch)
      SoundCue.ARC2_FILE_ZERO -> renderFileZeroReveal(pitch)
      SoundCue.ARC3_ENTER -> renderConvergenceTone(pitch)
      else -> null
    }

  /** Lamp On: crisp upward brass pull-chain detent + faint warm tungsten filament settle. */
  private fun renderBrassPullChainOn(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.20).toInt()
    val buf = FloatArray(len)
    addIronModalRing(
      buf,
      0.0,
      0.08,
      680.0 * pitch,
      gain = 0.34,
      decayScale = 2.5,
      seed = 2111,
      includeRatchetGrit = false,
      clangScale = 0.42,
    )
    addIronModalRing(
      buf,
      0.028,
      0.10,
      540.0 * pitch,
      gain = 0.28,
      decayScale = 2.1,
      seed = 2119,
      includeRatchetGrit = false,
      clangScale = 0.36,
    )
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val filament =
        (sin(TWO_PI * (120.0 * pitch) * t) * 0.22 + sin(TWO_PI * (240.0 * pitch) * t) * 0.09) *
          (1.0 - exp(-t * 45.0)) *
          exp(-t * 14.0)
      buf[i] += filament.toFloat()
    }
    return toPcm16(buf, 0.72f, roomWet = 0.10f)
  }

  /** Lamp Off: heavier downward brass drop-clack settling into muted nocturnal rain quiet. */
  private fun renderBrassPullChainOff(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.22).toInt()
    val buf = FloatArray(len)
    addIronModalRing(
      buf,
      0.0,
      0.09,
      460.0 * pitch,
      gain = 0.32,
      decayScale = 2.2,
      seed = 2131,
      includeRatchetGrit = false,
      clangScale = 0.34,
    )
    addIronModalRing(
      buf,
      0.038,
      0.12,
      320.0 * pitch,
      gain = 0.34,
      decayScale = 1.9,
      seed = 2137,
      includeRatchetGrit = false,
      clangScale = 0.28,
    )
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val drop =
        sin(TWO_PI * ((96.0 - t * 180.0).coerceAtLeast(48.0) * pitch) * t) *
          exp(-t * 20.0) *
          0.36
      buf[i] += drop.toFloat()
    }
    return toPcm16(buf, 0.72f, roomWet = 0.12f)
  }

  /** Night Archivist: heavy damp basement ledger thump + thick manila folder slide. */
  private fun renderContactArchivist(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.30).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1616)
    val paperFilter = SvFilter()
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val env = sin(PI * (t / 0.30).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val paper =
        paperFilter.processBandpass(noise.nextFloat().toDouble(), 620.0 - t * 820.0, 1.8) *
          env *
          0.74
      val ledgerThump =
        sin(TWO_PI * ((118.0 - t * 220.0).coerceAtLeast(58.0) * pitch) * t) *
          exp(-t * 22.0) *
          0.54
      buf[i] = (paper + ledgerThump).toFloat()
    }
    return toPcm16(buf, 0.76f, roomWet = 0.12f)
  }

  /** Municipal Courier: hurried carbon slip sliding under wooden office door + muffled latch tap. */
  private fun renderContactOrderly(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.28).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1643)
    val slipFilter = SvFilter()
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val slipEnv = if (t < 0.18) sin(PI * (t / 0.18)).coerceAtLeast(0.0) else 0.0
      val slip =
        slipFilter.processBandpass(noise.nextFloat().toDouble(), 1180.0 - t * 2100.0, 2.2) *
          slipEnv *
          0.66
      val knock1 =
        if (t >= 0.10) {
          val dt = t - 0.10
          sin(TWO_PI * (148.0 * pitch) * dt) * exp(-dt * 34.0) * 0.44
        } else {
          0.0
        }
      val knock2 =
        if (t >= 0.175) {
          val dt = t - 0.175
          sin(TWO_PI * (132.0 * pitch) * dt) * exp(-dt * 36.0) * 0.32
        } else {
          0.0
        }
      buf[i] = (slip + knock1 + knock2).toFloat()
    }
    return toPcm16(buf, 0.74f, roomWet = 0.11f)
  }

  /** Microfilm Reader: crisp glass shutter click + short motorized parcel-roll advance whirr. */
  private fun renderContactScanner(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.32).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1669)
    val whirrFilter = SvFilter()
    addIronModalRing(
      buf,
      0.0,
      0.08,
      780.0 * pitch,
      gain = 0.28,
      decayScale = 2.8,
      seed = 1673,
      includeRatchetGrit = false,
      clangScale = 0.40,
    )
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val motorEnv =
        if (t >= 0.04) sin(PI * ((t - 0.04) / 0.26).coerceAtMost(1.0)).coerceAtLeast(0.0) else 0.0
      val motorHum =
        (sin(TWO_PI * (162.0 * pitch) * t) * 0.30 + sin(TWO_PI * (324.0 * pitch) * t) * 0.14) *
          motorEnv
      val filmNoise =
        whirrFilter.processBandpass(noise.nextFloat().toDouble(), 940.0 + t * 520.0, 2.5) *
          motorEnv *
          0.42
      buf[i] += (motorHum + filmNoise).toFloat()
    }
    return toPcm16(buf, 0.72f, roomWet = 0.10f)
  }

  /** Reel Transcriber: mechanical dictation key clack + brief tape-spool start chirp. */
  private fun renderContactTranscriber(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.30).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1693)
    val tapeFilter = SvFilter()
    addIronModalRing(
      buf,
      0.0,
      0.07,
      540.0 * pitch,
      gain = 0.34,
      decayScale = 2.6,
      seed = 1697,
      includeRatchetGrit = false,
      clangScale = 0.38,
    )
    addIronModalRing(
      buf,
      0.055,
      0.09,
      410.0 * pitch,
      gain = 0.26,
      decayScale = 2.3,
      seed = 1699,
      includeRatchetGrit = false,
      clangScale = 0.32,
    )
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val chirpEnv =
        if (t >= 0.07) sin(PI * ((t - 0.07) / 0.21).coerceAtMost(1.0)).coerceAtLeast(0.0) else 0.0
      val capstan =
        sin(TWO_PI * ((210.0 + t * 440.0) * pitch) * t) * chirpEnv * 0.24
      val hiss =
        tapeFilter.processBandpass(noise.nextFloat().toDouble(), 1380.0 - t * 900.0, 2.4) *
          chirpEnv *
          0.36
      buf[i] += (capstan + hiss).toFloat()
    }
    return toPcm16(buf, 0.74f, roomWet = 0.12f)
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
      val hiss =
        hissFilter.processBandpass(noise.nextFloat().toDouble(), 1100.0 - t * 650.0, 2.2) *
          env *
          0.42
      val subThump =
        sin(TWO_PI * (52.0 * pitch) * t) * exp(-((t - 0.24) * (t - 0.24)) * 90.0) * 0.65
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
      val sizzle =
        filter.processBandpass(noise.nextFloat().toDouble(), 1650.0 - t * 1400.0, 1.5) *
          env *
          0.52
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

  /** Corkboard pin press + dry speculative crimson twine pluck (Arc 2). */
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

  /** Curated Corroboration lock: crisp brass pin press + taut bowed dark fifth resonance. */
  private fun renderCorroborationLock(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.54).toInt()
    val buf = FloatArray(len)
    addIronModalRing(
      buf,
      0.0,
      0.10,
      590.0 * pitch,
      gain = 0.26,
      decayScale = 2.4,
      seed = 1657,
      includeRatchetGrit = false,
      clangScale = 0.32,
    )
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val attack = 1.0 - exp(-t * 85.0)
      val fundamental = sin(TWO_PI * (110.0 * pitch) * t) * 0.48 * exp(-t * 5.2)
      val fifth = sin(TWO_PI * (165.0 * pitch) * t) * 0.36 * exp(-t * 5.8)
      val octave = sin(TWO_PI * (220.0 * pitch) * t) * 0.16 * exp(-t * 8.5)
      val snap =
        sin(TWO_PI * ((260.0 - t * 720.0).coerceAtLeast(110.0) * pitch) * t) *
          exp(-t * 38.0) *
          0.28
      buf[i] += ((fundamental + fifth + octave) * attack + snap).toFloat()
    }
    return toPcm16(buf, 0.78f, roomWet = 0.18f)
  }

  /** Unwiring a corkboard thread: soft slack twine release + muted cork pin pull. */
  private fun renderStringUnwire(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.22).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1663)
    val corkFilter = SvFilter()
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val slack =
        sin(TWO_PI * ((148.0 - t * 280.0).coerceAtLeast(78.0) * pitch) * t) *
          exp(-t * 18.0) *
          (1.0 - exp(-t * 120.0)) *
          0.46
      val corkPull =
        corkFilter.processBandpass(noise.nextFloat().toDouble(), 520.0 + t * 650.0, 1.9) *
          exp(-t * 24.0) *
          0.40
      buf[i] = (slack + corkPull).toFloat()
    }
    return toPcm16(buf, 0.68f, roomWet = 0.10f)
  }

  /** Desk Anomaly 1 — Evidence Envelope: damp manila envelope paper slit + crisp crease rustle. */
  private fun renderAnomalyEnvelope(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.28).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1717)
    val slitFilter = SvFilter()
    addTendonTear(buf, 0.0, 0.20, pitch * 1.18, gain = 0.42, fiberCount = 10, seed = 1719)
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val env = sin(PI * (t / 0.28).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val paperSlit =
        slitFilter.processBandpass(noise.nextFloat().toDouble(), 1480.0 - t * 1850.0, 1.6) *
          env *
          0.62
      buf[i] += paperSlit.toFloat()
    }
    return toPcm16(buf, 0.74f, roomWet = 0.11f)
  }

  /** Desk Anomaly 2 — Fogged Spectacles: damped cloth wipe across cold glass lenses + faint rim tap. */
  private fun renderAnomalySpectacles(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.34).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1733)
    val wipeFilter = SvFilter()
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val stroke1 = if (t < 0.17) sin(PI * (t / 0.17)).coerceAtLeast(0.0) else 0.0
      val stroke2 = if (t >= 0.14) sin(PI * ((t - 0.14) / 0.20).coerceAtMost(1.0)).coerceAtLeast(0.0) else 0.0
      val wipe =
        wipeFilter.processBandpass(noise.nextFloat().toDouble(), 920.0 + sin(t * 22.0) * 320.0, 2.2) *
          (stroke1 * 0.56 + stroke2 * 0.44)
      val glassClink =
        if (t >= 0.19) {
          val dt = t - 0.19
          (sin(TWO_PI * (640.0 * pitch) * dt) * 0.22 + sin(TWO_PI * (985.0 * pitch) * dt) * 0.12) *
            exp(-dt * 26.0)
        } else {
          0.0
        }
      buf[i] = (wipe + glassClink).toFloat()
    }
    return toPcm16(buf, 0.72f, roomWet = 0.12f)
  }

  /** Desk Anomaly 3 — Reel-to-Reel Tape: brass spool latch + warped tape hiss + distant basement thump. */
  private fun renderAnomalyReelTape(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.36).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1753)
    val tapeFilter = SvFilter()
    addIronModalRing(
      buf,
      0.0,
      0.11,
      440.0 * pitch,
      gain = 0.28,
      decayScale = 2.0,
      seed = 1759,
      includeRatchetGrit = false,
      clangScale = 0.34,
    )
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val hissEnv = sin(PI * (t / 0.36).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val hiss =
        tapeFilter.processBandpass(noise.nextFloat().toDouble(), 1260.0 - t * 900.0, 2.3) *
          hissEnv *
          0.46
      val knock =
        sin(TWO_PI * (64.0 * pitch) * t) * exp(-((t - 0.20) * (t - 0.20)) * 130.0) * 0.48
      buf[i] += (hiss + knock).toFloat()
    }
    return toPcm16(buf, 0.76f, roomWet = 0.14f)
  }

  /** Desk Anomaly 4 (Case IV+ only) — Enamel Exhibit: cold hollow molar/enamel clink in steel tray + wet subterranean tail. */
  private fun renderAnomalyEnamel(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.40).toInt()
    val buf = FloatArray(len)
    // Two cold enamel-on-steel tray ticks
    addIronModalRing(
      buf,
      0.0,
      0.14,
      510.0 * pitch,
      gain = 0.36,
      decayScale = 2.2,
      seed = 1777,
      includeRatchetGrit = false,
      clangScale = 0.34,
    )
    addIronModalRing(
      buf,
      0.075,
      0.12,
      430.0 * pitch,
      gain = 0.24,
      decayScale = 2.4,
      seed = 1783,
      includeRatchetGrit = false,
      clangScale = 0.28,
    )
    // Faint wet subterranean seepage bleeding under the tray (only reached in Case IV+)
    addWetSquelch(
      buf,
      0.10,
      0.26,
      pitch * 0.90,
      gain = 0.34,
      formantStartHz = 340.0,
      formantEndHz = 160.0,
      wobbleHz = 6.2,
      seed = 1789,
    )
    return toPcm16(buf, 0.78f, roomWet = 0.16f)
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
