package com.example.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Pure Kotlin DSP procedural foley and room-ambience synthesizer generating 16-bit PCM audio
 * with zero external audio assets. Tuned strictly around damp mechanical iron, wet seepage,
 * organic clicks, and hushed melancholic ambience.
 */
object ProceduralFoleySynth {
  const val SAMPLE_RATE: Int = 22050
  private const val TWO_PI = 2.0 * PI

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
  }

  private fun toPcm16(samples: FloatArray, masterGain: Float): ShortArray {
    val out = ShortArray(samples.size)
    for (i in samples.indices) {
      val shaped = tanh((samples[i] * masterGain).toDouble()).toFloat()
      val clamped = shaped.coerceIn(-0.95f, 0.95f)
      out[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
    }
    return out
  }

  fun synthesizeCue(cue: SoundCue, phase: Int = 0, pitchFactor: Float = 1f): ShortArray {
    val pitch = pitchFactor.coerceIn(0.82f, 1.18f).toDouble()
    val pDark = 1.0 - (phase.coerceIn(0, 4) * 0.045)
    return when (cue) {
      SoundCue.VALVE_PRESS -> renderValvePress(pitch * pDark, phase)
      SoundCue.CREATURE_BUY -> renderCreatureBuy(pitch * pDark)
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

  /** Heavy iron ratchet click + low metallic body + tiny wet seepage tail. */
  private fun renderValvePress(pitch: Double, phase: Int): ShortArray {
    val len = (SAMPLE_RATE * 0.16).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(101 + phase * 17)
    var lp = 0.0
    var bp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val n = noise.nextFloat().toDouble()
      lp += 0.24 * (n - lp)
      bp += 0.55 * ((n - lp) - bp)
      // Mechanical ratchet transient
      val clickEnv = exp(-t * 95.0)
      val ratchet = bp * clickEnv * 0.72
      // Cold recessed iron body resonance
      val bodyEnv = exp(-t * 28.0)
      val iron =
        (sin(TWO_PI * (128.0 * pitch) * t) * 0.55 + sin(TWO_PI * (284.0 * pitch) * t) * 0.25) *
          bodyEnv
      // Tiny wet drop plop trailing 26ms behind the iron click
      val dropT = (t - 0.026).coerceAtLeast(0.0)
      val drop =
        if (t > 0.026) {
          val freq = (320.0 + dropT * 2600.0) * pitch
          sin(TWO_PI * freq * dropT) * exp(-dropT * 52.0) * (0.20 + phase * 0.04)
        } else 0.0
      buf[i] = (ratchet + iron + drop).toFloat()
    }
    return toPcm16(buf, 0.78f)
  }

  /** Wet mechanical seating thud + faint suture settle. */
  private fun renderCreatureBuy(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.22).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(202)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.16 * (noise.nextFloat() - lp)
      val thudFreq = (145.0 * exp(-t * 14.0) + 58.0) * pitch
      val thud = sin(TWO_PI * thudFreq * t) * exp(-t * 18.0) * 0.65
      val wet = lp * exp(-t * 24.0) * 0.42
      val tick =
        if (t > 0.065) {
          sin(TWO_PI * (540.0 * pitch) * (t - 0.065)) * exp(-(t - 0.065) * 70.0) * 0.22
        } else 0.0
      buf[i] = (thud + wet + tick).toFloat()
    }
    return toPcm16(buf, 0.72f)
  }

  /** Hollow enamel snap + dry plaster flake. */
  private fun renderToothPull(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.19).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(303)
    var hpPrev = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val n = noise.nextFloat().toDouble()
      val hp = n - hpPrev * 0.86
      hpPrev = n
      val snapFreq = (1380.0 - t * 3200.0).coerceAtLeast(420.0) * pitch
      val snap = sin(TWO_PI * snapFreq * t) * exp(-t * 58.0) * 0.62
      val pop = sin(TWO_PI * (240.0 * pitch) * t) * exp(-t * 32.0) * 0.42
      val grit = hp * exp(-t * 40.0) * 0.30
      buf[i] = (snap + pop + grit).toFloat()
    }
    return toPcm16(buf, 0.76f)
  }

  /** Soft damp condensation wipe + muffled glass-eye blink. */
  private fun renderGlassEyeWipe(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.28).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(404)
    var lp1 = 0.0
    var lp2 = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val n = noise.nextFloat().toDouble()
      val cutoff = 0.08 + 0.22 * sin(PI * (t / 0.28)).coerceAtLeast(0.0)
      lp1 += cutoff * (n - lp1)
      lp2 += 0.04 * (n - lp2)
      val wipeEnv = sin(PI * (t / 0.28)).coerceAtLeast(0.0)
      val wipe = (lp1 - lp2) * wipeEnv * 0.85
      val blink =
        if (t > 0.16) {
          sin(TWO_PI * (190.0 * pitch) * (t - 0.16)) * exp(-(t - 0.16) * 35.0) * 0.36
        } else 0.0
      buf[i] = (wipe + blink).toFloat()
    }
    return toPcm16(buf, 0.72f)
  }

  /** Viscous low squelch + faint warm static hiss. */
  private fun renderNoduleProd(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.24).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(505)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.12 * (noise.nextFloat() - lp)
      val sweep = (230.0 - 115.0 * sin(PI * (t / 0.24).coerceAtMost(1.0))) * pitch
      val squelch = sin(TWO_PI * sweep * t) * exp(-t * 16.0) * 0.58
      val staticWet = lp * exp(-t * 14.0) * 0.55
      buf[i] = (squelch + staticWet).toFloat()
    }
    return toPcm16(buf, 0.74f)
  }

  /** Dry chitinous creak + hollow marrow pry. */
  private fun renderRibPry(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.30).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(606)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.30 * (noise.nextFloat() - lp)
      val fm = sin(TWO_PI * 44.0 * t) * 55.0
      val creak = sin(TWO_PI * (215.0 * pitch + fm) * t) * exp(-t * 11.0) * 0.45
      val crack = lp * exp(-t * 28.0) * 0.48
      val hollow = sin(TWO_PI * (430.0 * pitch) * t) * exp(-t * 22.0) * 0.26
      buf[i] = (creak + crack + hollow).toFloat()
    }
    return toPcm16(buf, 0.76f)
  }

  /** Muted glass clink + wax-seal vacuum thump. */
  private fun renderJarBottle(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.28).toInt()
    val buf = FloatArray(len)
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val glass =
        (sin(TWO_PI * (1140.0 * pitch) * t) * 0.34 + sin(TWO_PI * (1980.0 * pitch) * t) * 0.18) *
          exp(-t * 34.0)
      val waxT = (t - 0.06).coerceAtLeast(0.0)
      val seal =
        if (t > 0.06) {
          val f = (108.0 - waxT * 240.0).coerceAtLeast(52.0) * pitch
          sin(TWO_PI * f * waxT) * exp(-waxT * 20.0) * 0.62
        } else 0.0
      buf[i] = (glass + seal).toFloat()
    }
    return toPcm16(buf, 0.74f)
  }

  /** Layered wet stitch + compressed organic breath swell. */
  private fun renderGraftPerform(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.48).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(707)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.14 * (noise.nextFloat() - lp)
      val breathEnv = sin(PI * (t / 0.48).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val breath = lp * breathEnv * 0.55
      val low = sin(TWO_PI * (82.0 * pitch) * t) * breathEnv * 0.44
      val stitch1 = sin(TWO_PI * (620.0 * pitch) * t) * exp(-t * 55.0) * 0.26
      val stitch2 =
        if (t > 0.14) {
          sin(TWO_PI * (510.0 * pitch) * (t - 0.14)) * exp(-(t - 0.14) * 52.0) * 0.24
        } else 0.0
      buf[i] = (breath + low + stitch1 + stitch2).toFloat()
    }
    return toPcm16(buf, 0.76f)
  }

  /** Taut surgical thread draw + crisp pale-seam tick. */
  private fun renderScarStitch(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.22).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(808)
    var prev = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val n = noise.nextFloat().toDouble()
      val hp = n - prev * 0.90
      prev = n
      val thread = hp * sin(PI * (t / 0.22).coerceAtMost(1.0)) * 0.26
      val knotT = (t - 0.09).coerceAtLeast(0.0)
      val knot =
        if (t > 0.09) {
          sin(TWO_PI * (760.0 * pitch) * knotT) * exp(-knotT * 48.0) * 0.45
        } else 0.0
      val sub = sin(TWO_PI * (140.0 * pitch) * t) * exp(-t * 24.0) * 0.35
      buf[i] = (thread + knot + sub).toFloat()
    }
    return toPcm16(buf, 0.72f)
  }

  /** Heavy iron grate / valve diversion clank. */
  private fun renderIronClank(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.24).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(909)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.25 * (noise.nextFloat() - lp)
      val clang =
        (sin(TWO_PI * (174.0 * pitch) * t) * 0.5 +
          sin(TWO_PI * (392.0 * pitch) * t) * 0.32 +
          sin(TWO_PI * (611.0 * pitch) * t) * 0.18) * exp(-t * 20.0)
      buf[i] = (clang + lp * exp(-t * 45.0) * 0.45).toFloat()
    }
    return toPcm16(buf, 0.76f)
  }

  /** Submerged chain-hook pull & dark sump sluice. */
  private fun renderSumpDredge(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.42).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1010)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.10 * (noise.nextFloat() - lp)
      val sluice = lp * sin(PI * (t / 0.42).coerceAtMost(1.0)) * 0.72
      val chain1 = sin(TWO_PI * (420.0 * pitch) * t) * exp(-t * 36.0) * 0.32
      val chain2 =
        if (t > 0.12) {
          sin(TWO_PI * (340.0 * pitch) * (t - 0.12)) * exp(-(t - 0.12) * 28.0) * 0.36
        } else 0.0
      val rise = sin(TWO_PI * ((90.0 + t * 140.0) * pitch) * t) * exp(-t * 9.0) * 0.35
      buf[i] = (sluice + chain1 + chain2 + rise).toFloat()
    }
    return toPcm16(buf, 0.78f)
  }

  /** Masonry-muffled triple knock from the neighbor aperture. */
  private fun renderEchoTripleKnock(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.54).toInt()
    val buf = FloatArray(len)
    val offsets = doubleArrayOf(0.0, 0.145, 0.295)
    val amps = doubleArrayOf(0.56, 0.44, 0.34)
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      var sum = 0.0
      for (k in offsets.indices) {
        val dt = t - offsets[k]
        if (dt >= 0.0) {
          val tone =
            sin(TWO_PI * (162.0 * pitch) * dt) * 0.65 + sin(TWO_PI * (248.0 * pitch) * dt) * 0.35
          sum += tone * exp(-dt * 26.0) * amps[k]
        }
      }
      buf[i] = sum.toFloat()
    }
    return toPcm16(buf, 0.70f)
  }

  /** Heavy iron collar clamping over the valve. */
  private fun renderClampLock(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.34).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1111)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.18 * (noise.nextFloat() - lp)
      val slam = sin(TWO_PI * ((95.0 - t * 80.0).coerceAtLeast(48.0) * pitch) * t) * exp(-t * 14.0) * 0.75
      val bolt = sin(TWO_PI * (330.0 * pitch) * t) * exp(-t * 40.0) * 0.42
      val grit = lp * exp(-t * 32.0) * 0.55
      buf[i] = (slam + bolt + grit).toFloat()
    }
    return toPcm16(buf, 0.80f)
  }

  /** Pressurized masonry release into wet fissure bloom. */
  private fun renderMarrowBloom(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.56).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1212)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.15 * (noise.nextFloat() - lp)
      val groan = sin(TWO_PI * ((72.0 + t * 55.0) * pitch) * t) * exp(-t * 5.2) * 0.62
      val fissure = lp * exp(-t * 7.0) * 0.68
      val wetPop =
        if (t > 0.10) {
          sin(TWO_PI * ((260.0 + (t - 0.10) * 900.0) * pitch) * (t - 0.10)) *
            exp(-(t - 0.10) * 24.0) *
            0.28
        } else 0.0
      buf[i] = (groan + fissure + wetPop).toFloat()
    }
    return toPcm16(buf, 0.78f)
  }

  /** Deep hollow swallow + dry bone settle. */
  private fun renderThroatSwallow(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.44).toInt()
    val buf = FloatArray(len)
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val freq = (170.0 * exp(-t * 4.5) + 52.0) * pitch
      val gulp = sin(TWO_PI * freq * t) * exp(-t * 6.5) * 0.72
      val boneT = (t - 0.18).coerceAtLeast(0.0)
      val bone =
        if (t > 0.18) {
          sin(TWO_PI * (490.0 * pitch) * boneT) * exp(-boneT * 42.0) * 0.30
        } else 0.0
      buf[i] = (gulp + bone).toFloat()
    }
    return toPcm16(buf, 0.76f)
  }

  /** Slow cold metallic surgical shear fading to hushed silence. */
  private fun renderExcisionShear(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.78).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1313)
    var lp = 0.0
    var hpPrev = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val n = noise.nextFloat().toDouble()
      lp += 0.22 * (n - lp)
      val hp = n - hpPrev * 0.92
      hpPrev = n
      val bladeEnv = exp(-t * 4.8) * (1.0 - exp(-t * 45.0))
      val shear =
        (hp * 0.36 + sin(TWO_PI * ((780.0 - t * 420.0).coerceAtLeast(220.0) * pitch) * t) * 0.28) *
          bladeEnv
      val bassDrop =
        sin(TWO_PI * ((92.0 * exp(-t * 2.4) + 38.0) * pitch) * t) * exp(-t * 3.2) * 0.68
      val hush = lp * exp(-t * 5.0) * 0.25
      buf[i] = (shear + bassDrop + hush).toFloat()
    }
    return toPcm16(buf, 0.80f)
  }

  /** Warm damp seam sigh dissolving into cold rain static (Arc 1 -> Arc 2). */
  private fun renderPleadSalvation(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.90).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1414)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.16 * (noise.nextFloat() - lp)
      val sighEnv = sin(PI * (t / 0.90).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val lowChord =
        (sin(TWO_PI * (74.0 * pitch) * t) * 0.45 + sin(TWO_PI * (110.5 * pitch) * t) * 0.28) *
          sighEnv
      val rainSwell = lp * (t / 0.90) * exp(-t * 1.4) * 0.58
      buf[i] = (lowChord + rainSwell).toFloat()
    }
    return toPcm16(buf, 0.76f)
  }

  /** Ultra-quiet condensation drop announcing a newly unlocked Story fragment. */
  private fun renderStoryCondensationDrip(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.20).toInt()
    val buf = FloatArray(len)
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val freq = (480.0 + t * 2400.0) * pitch
      val drop = sin(TWO_PI * freq * t) * exp(-t * 34.0) * 0.48
      val ring = sin(TWO_PI * (240.0 * pitch) * t) * exp(-t * 22.0) * 0.16
      buf[i] = (drop + ring).toFloat()
    }
    return toPcm16(buf, 0.48f)
  }

  /** Subtle brooding chamber settle when advancing a Phase. */
  private fun renderPhaseShift(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.64).toInt()
    val buf = FloatArray(len)
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val env = sin(PI * (t / 0.64).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val sub =
        sin(TWO_PI * (64.0 * pitch) * t) * 0.52 + sin(TWO_PI * (96.0 * pitch) * t) * 0.28
      buf[i] = (sub * env).toFloat()
    }
    return toPcm16(buf, 0.62f)
  }

  /** Heavy inked wooden desk stamp + dry paper slap (Arc 2). */
  private fun renderDeskStamp(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.15).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1515)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.30 * (noise.nextFloat() - lp)
      val wood = sin(TWO_PI * ((118.0 - t * 260.0).coerceAtLeast(54.0) * pitch) * t) * exp(-t * 36.0) * 0.72
      val paper = lp * exp(-t * 68.0) * 0.48
      buf[i] = (wood + paper).toFloat()
    }
    return toPcm16(buf, 0.74f)
  }

  /** Dry manila folder slide / contact recruitment (Arc 2). */
  private fun renderPaperFolder(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.20).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1616)
    var prev = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val n = noise.nextFloat().toDouble()
      val hp = n - prev * 0.84
      prev = n
      val env = sin(PI * (t / 0.20).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val paper = hp * env * 0.46
      val tap = sin(TWO_PI * (185.0 * pitch) * t) * exp(-t * 40.0) * 0.30
      buf[i] = (paper + tap).toFloat()
    }
    return toPcm16(buf, 0.70f)
  }

  /** Taut red-string pinboard pluck (Arc 2). */
  private fun renderRedStringPin(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.24).toInt()
    val buf = FloatArray(len)
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val twang =
        (sin(TWO_PI * (310.0 * pitch) * t) * 0.52 + sin(TWO_PI * (625.0 * pitch) * t) * 0.24) *
          exp(-t * 18.0)
      val tack = sin(TWO_PI * (920.0 * pitch) * t) * exp(-t * 65.0) * 0.30
      buf[i] = (twang + tack).toFloat()
    }
    return toPcm16(buf, 0.70f)
  }

  /** Damp envelope slit / reel click on desk anomaly (Arc 2). */
  private fun renderDeskAnomaly(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.22).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1717)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.26 * (noise.nextFloat() - lp)
      val tear = lp * exp(-t * 22.0) * 0.56
      val click = sin(TWO_PI * (520.0 * pitch) * t) * exp(-t * 48.0) * 0.32
      buf[i] = (tear + click).toFloat()
    }
    return toPcm16(buf, 0.72f)
  }

  /** Heavy case folder closed / chapter connected (Arc 2). */
  private fun renderCaseChapterThud(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.38).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1818)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.14 * (noise.nextFloat() - lp)
      val thud = sin(TWO_PI * (88.0 * pitch) * t) * exp(-t * 12.0) * 0.68
      val rustle = lp * exp(-t * 18.0) * 0.40
      buf[i] = (thud + rustle).toFloat()
    }
    return toPcm16(buf, 0.76f)
  }

  /** File #00 reveal — desk folder giving way to cold chamber grout underneath. */
  private fun renderFileZeroReveal(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.88).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1919)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.10 * (noise.nextFloat() - lp)
      val env = sin(PI * (t / 0.88).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val drone =
        (sin(TWO_PI * (55.0 * pitch) * t) * 0.55 + sin(TWO_PI * (82.4 * pitch) * t) * 0.32) * env
      val bleed = lp * env * 0.42
      buf[i] = (drone + bleed).toFloat()
    }
    return toPcm16(buf, 0.76f)
  }

  /** Arc 3 hollow merged threshold resonance. */
  private fun renderConvergenceTone(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.72).toInt()
    val buf = FloatArray(len)
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val env = sin(PI * (t / 0.72).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val tone =
        sin(TWO_PI * (68.0 * pitch) * t) * 0.45 + sin(TWO_PI * (136.0 * pitch) * t) * 0.22
      buf[i] = (tone * env).toFloat()
    }
    return toPcm16(buf, 0.66f)
  }

  /**
   * Stateful streaming room-ambience generator. Smoothly slews mix parameters so phase shifts,
   * Stillness entry, Valve Clamp tension, and Arc 2 rain transitions crossfade with zero clicks.
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

    // Irregular condensation drip oscillator state
    private var samplesUntilNextDrip: Int = (sampleRate * 1.4).toInt()
    private var activeDripSamples: Int = 0
    private var activeDripAge: Int = 0
    private var activeDripBaseFreq: Double = 420.0
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

        // 1. Cold 60Hz fluorescent ballast / tungsten hum
        val hum =
          (sin(TWO_PI * 60.0 * t) * 0.62 + sin(TWO_PI * 120.0 * t) * 0.22) * sHum * 0.11

        // 2. Slow damp low-mid peristalsis & Valve Clamp sub-tension
        val rawNoise = noise.nextFloat().toDouble()
        fleshLp += 0.015 * (rawNoise - fleshLp)
        val breathLfo = 0.55 + 0.45 * sin(TWO_PI * 0.18 * t)
        val flesh =
          (sin(TWO_PI * 43.0 * t) * 0.55 + fleshLp * 1.4) * (sFlesh + sClamp * 0.55) * breathLfo * 0.13

        // 3. Phase 4 systolic/diastolic cardiac double-beat (subtle low pulse ~0.72Hz cycle)
        var cardiac = 0.0
        if (sCardiac > 0.01) {
          val cycleT = (t * 0.72) % 1.0
          val beat1 = exp(-cycleT * 26.0)
          val beat2 = if (cycleT > 0.22) exp(-(cycleT - 0.22) * 30.0) * 0.68 else 0.0
          cardiac = sin(TWO_PI * 52.0 * t) * (beat1 + beat2) * sCardiac * 0.16
        }

        // 4. Arc 2 soft rain on glass (band-shaped noise)
        val rainHp = rawNoise - rainHpPrev * 0.76
        rainHpPrev = rawNoise
        rainLp += 0.18 * (rainHp - rainLp)
        val rain = rainLp * sRain * (0.20 - sStill * 0.04)

        // 5. Sparse irregular condensation water drips
        var drip = 0.0
        if (activeDripSamples > 0) {
          val dt = activeDripAge.toDouble() / sampleRate
          val f = activeDripBaseFreq + dt * 2100.0
          drip = sin(TWO_PI * f * dt) * exp(-dt * 42.0) * activeDripGain
          activeDripAge++
          if (activeDripAge >= activeDripSamples) {
            activeDripSamples = 0
          }
        } else if (profile.dripRate > 0.02f) {
          samplesUntilNextDrip--
          if (samplesUntilNextDrip <= 0) {
            val r = (noise.nextFloat() + 1f) * 0.5f // 0..1
            activeDripBaseFreq = 340.0 + r * 260.0
            activeDripGain = 0.08 + profile.dripRate * 0.09 + sStill * 0.05
            activeDripSamples = (sampleRate * 0.14).toInt()
            activeDripAge = 0
            val minGap = (1.1 - profile.dripRate * 0.55).coerceAtLeast(0.45)
            val maxGap = (4.2 - profile.dripRate * 2.1).coerceAtLeast(1.2)
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
