package com.example.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Pure Kotlin DSP procedural foley and room-ambience synthesizer generating 16-bit PCM audio
 * with zero external audio assets. Tuned strictly around deep, heavy closed-room acoustics:
 * cast-iron valve weight, thick wet seepage, long fleshy grafting, distinct organism signatures,
 * and dark subterranean ambience — never bright arcade tones or shrill pings.
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
    val pDark = 1.0 - (phase.coerceIn(0, 4) * 0.04)
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

  /** Deep, heavy cast-iron valve lug thud + muffled closed-room pipe resonance + dark seepage. */
  private fun renderValvePress(pitch: Double, phase: Int): ShortArray {
    val len = (SAMPLE_RATE * 0.21).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(101 + phase * 17)
    var lp = 0.0
    var lp2 = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val n = noise.nextFloat().toDouble()
      // Heavily damped low-mid mechanical ratchet noise (no bright hiss)
      lp += 0.13 * (n - lp)
      lp2 += 0.045 * (lp - lp2)
      val ratchet = (lp - lp2 * 0.45) * exp(-t * 58.0) * 0.62
      // Deep cast-iron mass thump (84Hz -> 40Hz) + dull recessed collar resonance (165Hz -> 96Hz)
      val subFreq = (40.0 + 44.0 * exp(-t * 24.0)) * pitch
      val bodyFreq = (96.0 + 69.0 * exp(-t * 32.0)) * pitch
      val iron =
        sin(TWO_PI * subFreq * t) * exp(-t * 17.0) * 0.78 +
          sin(TWO_PI * bodyFreq * t) * exp(-t * 28.0) * 0.30
      // Muted dark seepage plop trailing 32ms behind the lug
      val dropT = (t - 0.032).coerceAtLeast(0.0)
      val drop =
        if (t > 0.032) {
          val freq = (148.0 + dropT * 620.0).coerceAtMost(240.0) * pitch
          sin(TWO_PI * freq * dropT) * exp(-dropT * 38.0) * (0.14 + phase * 0.035)
        } else 0.0
      buf[i] = (ratchet + iron + drop).toFloat()
    }
    return toPcm16(buf, 0.84f)
  }

  /** Suture-Hand: dry-wet knuckle/joint crack + heavy palm grip settling onto cold cast iron. */
  private fun renderSutureHandBuy(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.28).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(202)
    var lp = 0.0
    var lp2 = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val n = noise.nextFloat().toDouble()
      lp += 0.16 * (n - lp)
      lp2 += 0.05 * (lp - lp2)
      // Heavy iron grip weight
      val gripFreq = (48.0 + 54.0 * exp(-t * 18.0)) * pitch
      val grip = sin(TWO_PI * gripFreq * t) * exp(-t * 15.0) * 0.72
      // Two staggered dull knuckle/tendon snaps settling on the wheel rim
      val k1 = sin(TWO_PI * ((260.0 - t * 900.0).coerceAtLeast(115.0) * pitch) * t) * exp(-t * 48.0) * 0.34
      val k2T = (t - 0.075).coerceAtLeast(0.0)
      val k2 =
        if (t > 0.075) {
          sin(TWO_PI * ((215.0 - k2T * 750.0).coerceAtLeast(95.0) * pitch) * k2T) *
            exp(-k2T * 42.0) *
            0.30
        } else 0.0
      val dampGrip = (lp - lp2 * 0.3) * exp(-t * 22.0) * 0.42
      buf[i] = (grip + k1 + k2 + dampGrip).toFloat()
    }
    return toPcm16(buf, 0.80f)
  }

  /** Leech-Clerk: sluggish wet suction attach + low slimy glass drag across the counter bezel. */
  private fun renderLeechClerkBuy(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.34).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(213)
    var lp = 0.0
    var lp2 = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val n = noise.nextFloat().toDouble()
      val mod = 0.06 + 0.09 * sin(PI * (t / 0.34).coerceAtMost(1.0)).coerceAtLeast(0.0)
      lp += mod * (n - lp)
      lp2 += 0.03 * (lp - lp2)
      // Wet suction slurp sweeping downward
      val slurpFreq = (178.0 * exp(-t * 6.5) + 64.0) * pitch
      val slurpEnv = sin(PI * (t / 0.28).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val slurp = sin(TWO_PI * slurpFreq * t) * slurpEnv * 0.56
      // Thick mucus squelch texture
      val slime = (lp - lp2 * 0.4) * slurpEnv * 0.72
      // Late soft suction-cup seal pop
      val popT = (t - 0.14).coerceAtLeast(0.0)
      val pop =
        if (t > 0.14) {
          val pf = (118.0 - popT * 380.0).coerceAtLeast(52.0) * pitch
          sin(TWO_PI * pf * popT) * exp(-popT * 26.0) * 0.48
        } else 0.0
      buf[i] = (slurp + slime + pop).toFloat()
    }
    return toPcm16(buf, 0.80f)
  }

  /** Weeping Lamp: dull hollow dark-glass vessel groan + slow heavy dark-water droplet plop. */
  private fun renderWeepingLampBuy(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.40).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(224)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.08 * (noise.nextFloat() - lp)
      // Heavy hollow dark-glass bell resonance (low, moody, non-bright)
      val bellEnv = exp(-t * 7.8) * (1.0 - exp(-t * 55.0))
      val glass =
        (sin(TWO_PI * (146.0 * pitch) * t) * 0.56 +
          sin(TWO_PI * (220.0 * pitch) * t) * 0.24 +
          sin(TWO_PI * (73.0 * pitch) * t) * 0.32) * bellEnv
      // Slow condensation droplet plopping into pooled ichor at 135ms
      val dropT = (t - 0.135).coerceAtLeast(0.0)
      val drop =
        if (t > 0.135) {
          val df = (155.0 + dropT * 740.0).coerceAtMost(265.0) * pitch
          sin(TWO_PI * df * dropT) * exp(-dropT * 28.0) * 0.34
        } else 0.0
      val sway = lp * exp(-t * 12.0) * 0.22
      buf[i] = (glass + drop + sway).toFloat()
    }
    return toPcm16(buf, 0.78f)
  }

  /** Sump-Siphon: cavernous iron pipe vacuum inhale + deep submerged gurgling gulp. */
  private fun renderSumpSiphonBuy(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.44).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(235)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.065 * (noise.nextFloat() - lp)
      val env = sin(PI * (t / 0.44).coerceAtMost(1.0)).coerceAtLeast(0.0)
      // Deep bellows sub-inhale (68Hz -> 38Hz) with bubbling modulation
      val bubble = 0.70 + 0.30 * sin(TWO_PI * 14.0 * t)
      val pipeFreq = (38.0 + 30.0 * exp(-t * 4.5)) * pitch
      val pipe = sin(TWO_PI * pipeFreq * t) * env * 0.72
      val gurgle = lp * env * bubble * 0.85
      val valveSeat =
        if (t > 0.22) {
          val dt = t - 0.22
          sin(TWO_PI * (92.0 * pitch) * dt) * exp(-dt * 20.0) * 0.42
        } else 0.0
      buf[i] = (pipe + gurgle + valveSeat).toFloat()
    }
    return toPcm16(buf, 0.82f)
  }

  /** Dull bone/enamel root snap + damp plaster crumble. */
  private fun renderToothPull(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.22).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(303)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.16 * (noise.nextFloat() - lp)
      val snapFreq = (520.0 - t * 1850.0).coerceAtLeast(175.0) * pitch
      val snap = sin(TWO_PI * snapFreq * t) * exp(-t * 46.0) * 0.48
      val socketPop = sin(TWO_PI * ((128.0 - t * 340.0).coerceAtLeast(56.0) * pitch) * t) * exp(-t * 24.0) * 0.62
      val mortar = lp * exp(-t * 28.0) * 0.42
      buf[i] = (snap + socketPop + mortar).toFloat()
    }
    return toPcm16(buf, 0.78f)
  }

  /** Heavy damp condensation wipe + low hollow glass-eye blink. */
  private fun renderGlassEyeWipe(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.32).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(404)
    var lp1 = 0.0
    var lp2 = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val n = noise.nextFloat().toDouble()
      val cutoff = 0.05 + 0.11 * sin(PI * (t / 0.32)).coerceAtLeast(0.0)
      lp1 += cutoff * (n - lp1)
      lp2 += 0.025 * (n - lp2)
      val wipeEnv = sin(PI * (t / 0.32)).coerceAtLeast(0.0)
      val wipe = (lp1 - lp2) * wipeEnv * 0.88
      val blink =
        if (t > 0.17) {
          val bt = t - 0.17
          val bf = (118.0 - bt * 320.0).coerceAtLeast(62.0) * pitch
          sin(TWO_PI * bf * bt) * exp(-bt * 26.0) * 0.48
        } else 0.0
      buf[i] = (wipe + blink).toFloat()
    }
    return toPcm16(buf, 0.76f)
  }

  /** Thick viscous blister rupture + sluggish oozing seepage. */
  private fun renderNoduleProd(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.30).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(505)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.08 * (noise.nextFloat() - lp)
      val sweep = (146.0 - 82.0 * sin(PI * (t / 0.30).coerceAtMost(1.0))) * pitch
      val squelch = sin(TWO_PI * sweep * t) * exp(-t * 13.0) * 0.66
      val ooze = lp * exp(-t * 10.5) * 0.68
      buf[i] = (squelch + ooze).toFloat()
    }
    return toPcm16(buf, 0.78f)
  }

  /** Deep calcified rib creak + hollow marrow pry. */
  private fun renderRibPry(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.36).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(606)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.15 * (noise.nextFloat() - lp)
      val fm = sin(TWO_PI * 24.0 * t) * 28.0
      val creak = sin(TWO_PI * (128.0 * pitch + fm) * t) * exp(-t * 9.5) * 0.50
      val crack = lp * exp(-t * 24.0) * 0.54
      val marrow = sin(TWO_PI * ((74.0 - t * 90.0).coerceAtLeast(38.0) * pitch) * t) * exp(-t * 12.0) * 0.56
      buf[i] = (creak + crack + marrow).toFloat()
    }
    return toPcm16(buf, 0.80f)
  }

  /** Thick dark apothecary glass vessel thud + deep wax-seal vacuum cork. */
  private fun renderJarBottle(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.32).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(650)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.09 * (noise.nextFloat() - lp)
      val vessel =
        (sin(TWO_PI * (260.0 * pitch) * t) * 0.36 + sin(TWO_PI * (388.0 * pitch) * t) * 0.16) *
          exp(-t * 22.0)
      val waxT = (t - 0.055).coerceAtLeast(0.0)
      val seal =
        if (t > 0.055) {
          val f = (84.0 - waxT * 180.0).coerceAtLeast(38.0) * pitch
          sin(TWO_PI * f * waxT) * exp(-waxT * 16.0) * 0.72
        } else 0.0
      val cork = lp * exp(-t * 26.0) * 0.36
      buf[i] = (vessel + seal + cork).toFloat()
    }
    return toPcm16(buf, 0.78f)
  }

  /**
   * Long (~940ms), visceral, deeply fleshy grafting ritual:
   * - Stage 1 (0..320ms): heavy wet tissue compression & peristaltic sub-throb
   * - Stage 2 (200..650ms): three staggered soggy braiding/suction gulps (zero metallic pings)
   * - Stage 3 (560..940ms): heavy damp organic exhale settling into the wall
   */
  private fun renderGraftPerform(pitch: Double): ShortArray {
    val duration = 0.94
    val len = (SAMPLE_RATE * duration).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(707)
    var lp1 = 0.0
    var lp2 = 0.0
    val pulseStarts = doubleArrayOf(0.18, 0.38, 0.56)
    val pulseFreqs = doubleArrayOf(158.0, 134.0, 114.0)
    val pulseAmps = doubleArrayOf(0.46, 0.52, 0.44)
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val n = noise.nextFloat().toDouble()
      val wobble = 0.55 + 0.45 * sin(TWO_PI * 7.8 * t)
      // Wet formant-modulated dark squelch bed (stays strictly low-passed, warm & fleshy)
      val cutoff = 0.045 + 0.075 * wobble * (1.0 - (t / duration) * 0.45)
      lp1 += cutoff * (n - lp1)
      lp2 += 0.022 * (lp1 - lp2)
      val bedEnv = sin(PI * (t / duration).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val fleshNoise = (lp1 - lp2 * 0.35) * bedEnv * (0.85 + 0.35 * wobble)

      // Deep visceral sub-throb descending from 58Hz -> 34Hz
      val subFreq = (34.0 + 24.0 * exp(-t * 2.2)) * pitch
      val sub = sin(TWO_PI * subFreq * t) * bedEnv * 0.62

      // Three staggered wet fleshy braiding suction contractions
      var pulses = 0.0
      for (k in pulseStarts.indices) {
        val dt = t - pulseStarts[k]
        if (dt >= 0.0 && dt < 0.28) {
          val pEnv = sin(PI * (dt / 0.26).coerceAtMost(1.0)).coerceAtLeast(0.0) * exp(-dt * 5.2)
          val pf = (pulseFreqs[k] * exp(-dt * 6.5) + 46.0) * pitch
          val wetRipple = 0.72 + 0.28 * sin(TWO_PI * 19.0 * dt)
          pulses += sin(TWO_PI * pf * dt) * pEnv * wetRipple * pulseAmps[k]
        }
      }

      // Late damp organic exhale settling into masonry
      val exhale =
        if (t > 0.58) {
          val et = t - 0.58
          val eEnv = sin(PI * (et / 0.36).coerceAtMost(1.0)).coerceAtLeast(0.0)
          lp2 * eEnv * 1.15
        } else 0.0

      buf[i] = (fleshNoise + sub + pulses + exhale).toFloat()
    }
    return toPcm16(buf, 0.84f)
  }

  /** Coarse surgical thread draw through damp skin + dull low-register seam settle. */
  private fun renderScarStitch(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.26).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(808)
    var lp = 0.0
    var lp2 = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val n = noise.nextFloat().toDouble()
      lp += 0.18 * (n - lp)
      lp2 += 0.05 * (lp - lp2)
      val thread = (lp - lp2) * sin(PI * (t / 0.26).coerceAtMost(1.0)) * 0.56
      val knotT = (t - 0.10).coerceAtLeast(0.0)
      val knot =
        if (t > 0.10) {
          sin(TWO_PI * ((240.0 - knotT * 780.0).coerceAtLeast(110.0) * pitch) * knotT) *
            exp(-knotT * 34.0) *
            0.38
        } else 0.0
      val sub = sin(TWO_PI * ((104.0 - t * 180.0).coerceAtLeast(48.0) * pitch) * t) * exp(-t * 18.0) * 0.56
      buf[i] = (thread + knot + sub).toFloat()
    }
    return toPcm16(buf, 0.78f)
  }

  /** Heavy rusted cast-iron floor grate / diversion latch clank. */
  private fun renderIronClank(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.28).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(909)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.13 * (noise.nextFloat() - lp)
      val clang =
        (sin(TWO_PI * ((115.0 - t * 140.0).coerceAtLeast(56.0) * pitch) * t) * 0.62 +
          sin(TWO_PI * (232.0 * pitch) * t) * 0.28 +
          sin(TWO_PI * (348.0 * pitch) * t) * 0.12) * exp(-t * 16.0)
      buf[i] = (clang + lp * exp(-t * 32.0) * 0.50).toFloat()
    }
    return toPcm16(buf, 0.80f)
  }

  /** Submerged rusted chain-hook haul & dark sump water slosh. */
  private fun renderSumpDredge(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.48).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1010)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.075 * (noise.nextFloat() - lp)
      val sluice = lp * sin(PI * (t / 0.48).coerceAtMost(1.0)) * 0.82
      val chain1 = sin(TWO_PI * (196.0 * pitch) * t) * exp(-t * 28.0) * 0.36
      val chain2 =
        if (t > 0.14) {
          sin(TWO_PI * (154.0 * pitch) * (t - 0.14)) * exp(-(t - 0.14) * 24.0) * 0.40
        } else 0.0
      val rise = sin(TWO_PI * ((56.0 + t * 72.0) * pitch) * t) * exp(-t * 7.5) * 0.48
      buf[i] = (sluice + chain1 + chain2 + rise).toFloat()
    }
    return toPcm16(buf, 0.80f)
  }

  /** Deeply muffled triple knuckle-knock behind thick wet masonry. */
  private fun renderEchoTripleKnock(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.58).toInt()
    val buf = FloatArray(len)
    val offsets = doubleArrayOf(0.0, 0.155, 0.315)
    val freqs = doubleArrayOf(92.0, 86.0, 80.0)
    val amps = doubleArrayOf(0.64, 0.50, 0.38)
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      var sum = 0.0
      for (k in offsets.indices) {
        val dt = t - offsets[k]
        if (dt >= 0.0) {
          val f = (freqs[k] * exp(-dt * 8.0)).coerceAtLeast(44.0) * pitch
          val tone = sin(TWO_PI * f * dt) * 0.74 + sin(TWO_PI * (f * 1.52) * dt) * 0.26
          sum += tone * exp(-dt * 22.0) * amps[k]
        }
      }
      buf[i] = sum.toFloat()
    }
    return toPcm16(buf, 0.76f)
  }

  /** Massive iron jaws bolting down over the valve. */
  private fun renderClampLock(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.38).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1111)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.11 * (noise.nextFloat() - lp)
      val slam = sin(TWO_PI * ((74.0 - t * 85.0).coerceAtLeast(36.0) * pitch) * t) * exp(-t * 11.5) * 0.82
      val boltT = (t - 0.08).coerceAtLeast(0.0)
      val bolt =
        if (t > 0.08) {
          sin(TWO_PI * ((168.0 - boltT * 280.0).coerceAtLeast(76.0) * pitch) * boltT) *
            exp(-boltT * 26.0) *
            0.46
        } else 0.0
      val grit = lp * exp(-t * 24.0) * 0.56
      buf[i] = (slam + bolt + grit).toFloat()
    }
    return toPcm16(buf, 0.84f)
  }

  /** Deep pressurized masonry release into a wet cavern bloom. */
  private fun renderMarrowBloom(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.62).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1212)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.095 * (noise.nextFloat() - lp)
      val groan = sin(TWO_PI * ((62.0 - t * 38.0).coerceAtLeast(34.0) * pitch) * t) * exp(-t * 4.5) * 0.72
      val fissure = lp * exp(-t * 5.8) * 0.74
      val wetPop =
        if (t > 0.12) {
          val dt = t - 0.12
          sin(TWO_PI * ((154.0 - dt * 240.0).coerceAtLeast(82.0) * pitch) * dt) *
            exp(-dt * 18.0) *
            0.30
        } else 0.0
      buf[i] = (groan + fissure + wetPop).toFloat()
    }
    return toPcm16(buf, 0.82f)
  }

  /** Cavernous hollow swallow + dull calcified tooth settle. */
  private fun renderThroatSwallow(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.48).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1260)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.08 * (noise.nextFloat() - lp)
      val freq = (116.0 * exp(-t * 4.2) + 40.0) * pitch
      val gulp = sin(TWO_PI * freq * t) * exp(-t * 5.6) * 0.76
      val wet = lp * exp(-t * 8.0) * 0.46
      val boneT = (t - 0.20).coerceAtLeast(0.0)
      val bone =
        if (t > 0.20) {
          sin(TWO_PI * (208.0 * pitch) * boneT) * exp(-boneT * 30.0) * 0.28
        } else 0.0
      buf[i] = (gulp + wet + bone).toFloat()
    }
    return toPcm16(buf, 0.80f)
  }

  /** Heavy cold surgical shear + deep cavernous severance drop. */
  private fun renderExcisionShear(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.84).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1313)
    var lp = 0.0
    var lp2 = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val n = noise.nextFloat().toDouble()
      lp += 0.16 * (n - lp)
      lp2 += 0.04 * (lp - lp2)
      val bladeEnv = exp(-t * 4.4) * (1.0 - exp(-t * 36.0))
      val shear =
        ((lp - lp2) * 0.48 +
          sin(TWO_PI * ((380.0 - t * 240.0).coerceAtLeast(135.0) * pitch) * t) * 0.24) * bladeEnv
      val bassDrop =
        sin(TWO_PI * ((74.0 * exp(-t * 2.2) + 30.0) * pitch) * t) * exp(-t * 2.8) * 0.78
      val hush = lp2 * exp(-t * 4.2) * 0.40
      buf[i] = (shear + bassDrop + hush).toFloat()
    }
    return toPcm16(buf, 0.84f)
  }

  /** Low mournful damp seam sigh dissolving into muffled exterior rain (Arc 1 -> Arc 2). */
  private fun renderPleadSalvation(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.96).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1414)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.095 * (noise.nextFloat() - lp)
      val sighEnv = sin(PI * (t / 0.96).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val lowChord =
        (sin(TWO_PI * (54.0 * pitch) * t) * 0.54 + sin(TWO_PI * (81.0 * pitch) * t) * 0.30) *
          sighEnv
      val rainSwell = lp * (t / 0.96) * exp(-t * 1.3) * 0.60
      buf[i] = (lowChord + rainSwell).toFloat()
    }
    return toPcm16(buf, 0.78f)
  }

  /** Quiet, low-register cistern condensation drop announcing a newly unlocked Story fragment. */
  private fun renderStoryCondensationDrip(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.24).toInt()
    val buf = FloatArray(len)
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val freq = (245.0 + t * 920.0).coerceAtMost(385.0) * pitch
      val drop = sin(TWO_PI * freq * t) * exp(-t * 26.0) * 0.44
      val cistern = sin(TWO_PI * (126.0 * pitch) * t) * exp(-t * 17.0) * 0.26
      buf[i] = (drop + cistern).toFloat()
    }
    return toPcm16(buf, 0.46f)
  }

  /** Deep brooding architectural groan when advancing a Phase. */
  private fun renderPhaseShift(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.72).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1470)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.05 * (noise.nextFloat() - lp)
      val env = sin(PI * (t / 0.72).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val sub =
        sin(TWO_PI * (48.0 * pitch) * t) * 0.58 + sin(TWO_PI * (72.0 * pitch) * t) * 0.26
      buf[i] = ((sub + lp * 0.35) * env).toFloat()
    }
    return toPcm16(buf, 0.68f)
  }

  /** Heavy muffled wooden desk stamp + dark blotter slap (Arc 2). */
  private fun renderDeskStamp(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.18).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1515)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.15 * (noise.nextFloat() - lp)
      val wood = sin(TWO_PI * ((98.0 - t * 240.0).coerceAtLeast(44.0) * pitch) * t) * exp(-t * 28.0) * 0.78
      val paper = lp * exp(-t * 50.0) * 0.48
      buf[i] = (wood + paper).toFloat()
    }
    return toPcm16(buf, 0.78f)
  }

  /** Heavy damp manila folder slide + low desk drawer tap (Arc 2). */
  private fun renderPaperFolder(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.24).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1616)
    var lp = 0.0
    var lp2 = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val n = noise.nextFloat().toDouble()
      lp += 0.17 * (n - lp)
      lp2 += 0.05 * (lp - lp2)
      val env = sin(PI * (t / 0.24).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val paper = (lp - lp2) * env * 0.54
      val tap = sin(TWO_PI * ((142.0 - t * 260.0).coerceAtLeast(72.0) * pitch) * t) * exp(-t * 32.0) * 0.42
      buf[i] = (paper + tap).toFloat()
    }
    return toPcm16(buf, 0.74f)
  }

  /** Corkboard pin press + dark damped bass red-string pluck (Arc 2). */
  private fun renderRedStringPin(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.26).toInt()
    val buf = FloatArray(len)
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val twang =
        (sin(TWO_PI * (164.0 * pitch) * t) * 0.56 + sin(TWO_PI * (246.0 * pitch) * t) * 0.22) *
          exp(-t * 15.0)
      val tack = sin(TWO_PI * ((310.0 - t * 1100.0).coerceAtLeast(115.0) * pitch) * t) * exp(-t * 48.0) * 0.34
      buf[i] = (twang + tack).toFloat()
    }
    return toPcm16(buf, 0.74f)
  }

  /** Damp envelope slit / muted brass reel latch (Arc 2). */
  private fun renderDeskAnomaly(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.24).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1717)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.14 * (noise.nextFloat() - lp)
      val tear = lp * exp(-t * 18.0) * 0.62
      val click = sin(TWO_PI * ((218.0 - t * 480.0).coerceAtLeast(110.0) * pitch) * t) * exp(-t * 36.0) * 0.38
      buf[i] = (tear + click).toFloat()
    }
    return toPcm16(buf, 0.76f)
  }

  /** Heavy case folder closed / chapter connected (Arc 2). */
  private fun renderCaseChapterThud(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.42).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1818)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.10 * (noise.nextFloat() - lp)
      val thud = sin(TWO_PI * ((76.0 - t * 110.0).coerceAtLeast(38.0) * pitch) * t) * exp(-t * 10.5) * 0.76
      val rustle = lp * exp(-t * 15.0) * 0.46
      buf[i] = (thud + rustle).toFloat()
    }
    return toPcm16(buf, 0.78f)
  }

  /** File #00 reveal — desk folder giving way to cold subterranean chamber grout underneath. */
  private fun renderFileZeroReveal(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.94).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1919)
    var lp = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      lp += 0.075 * (noise.nextFloat() - lp)
      val env = sin(PI * (t / 0.94).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val drone =
        (sin(TWO_PI * (42.0 * pitch) * t) * 0.62 + sin(TWO_PI * (63.0 * pitch) * t) * 0.32) * env
      val bleed = lp * env * 0.46
      buf[i] = (drone + bleed).toFloat()
    }
    return toPcm16(buf, 0.80f)
  }

  /** Arc 3 hollow merged subterranean threshold resonance. */
  private fun renderConvergenceTone(pitch: Double): ShortArray {
    val len = (SAMPLE_RATE * 0.80).toInt()
    val buf = FloatArray(len)
    for (i in 0 until len) {
      val t = i.toDouble() / SAMPLE_RATE
      val env = sin(PI * (t / 0.80).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val tone =
        sin(TWO_PI * (50.0 * pitch) * t) * 0.54 + sin(TWO_PI * (75.0 * pitch) * t) * 0.24
      buf[i] = (tone * env).toFloat()
    }
    return toPcm16(buf, 0.70f)
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
