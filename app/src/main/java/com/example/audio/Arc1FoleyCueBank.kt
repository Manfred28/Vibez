package com.example.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Procedural foley cue bank for Arc 1 (The Chamber — valve, organisms, anomalies, rites, excisions). */
internal object Arc1FoleyCueBank {

  fun synthesize(cue: SoundCue, pitch: Double, pDark: Double, phase: Int): ShortArray? =
    when (cue) {
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
      SoundCue.GRAFT_CHOIR_LUNG -> renderChoirLungGraft(pitch)
      SoundCue.RELIC_INSPECT -> renderRelicInspect(pitch)
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
      else -> null
    }

  /**
   * Deep heavy cast-iron-in-masonry wall valve thump: dominant sub-bass & low-mid wall-housing
   * chest thump + low-passed impact mass + darkened bolted cast-iron modal under-ring +
   * phase-escalating wet organ seepage behind the rim.
   */
  private fun renderValvePress(pitch: Double, phase: Int): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.28).toInt()
    val buf = FloatArray(len)
    val thudNoise = NoiseGen(103 + phase * 19)
    val thudFilter = SvFilter()

    // 1. Dominant deep sub-bass & masonry chest thump (108Hz -> 36Hz + 152Hz -> 62Hz body punch + damped impact)
    var subPhase = 0.0
    var bodyPhase = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val subF = (36.0 + 72.0 * exp(-t * 34.0)) * pitch
      val bodyF = (62.0 + 90.0 * exp(-t * 40.0)) * pitch
      subPhase += TWO_PI * subF / FOLEY_SAMPLE_RATE
      bodyPhase += TWO_PI * bodyF / FOLEY_SAMPLE_RATE
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
    val len = (FOLEY_SAMPLE_RATE * 0.42).toInt()
    val buf = FloatArray(len)
    addTendonTear(buf, 0.0, 0.26, pitch, gain = 0.72, fiberCount = 16, seed = 202)
    addWetSquelch(buf, 0.03, 0.24, pitch, gain = 0.38, formantStartHz = 260.0, formantEndHz = 520.0, seed = 205)
    // Cold skeletal fingers clamping onto the iron valve wheel at 165ms
    addIronModalRing(buf, 0.165, 0.22, 265.0 * pitch, gain = 0.44, decayScale = 1.35, seed = 209, includeRatchetGrit = false)
    return toPcm16(buf, 0.82f, roomWet = 0.16f)
  }

  /** Leech-Clerk: long viscous wet mucosal squelch + irregular suction-cup bubbles dragging on cold glass. */
  private fun renderLeechClerkBuy(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.46).toInt()
    val buf = FloatArray(len)
    addWetSquelch(buf, 0.0, 0.42, pitch, gain = 0.78, formantStartHz = 220.0, formantEndHz = 620.0, wobbleHz = 8.5, seed = 213)
    addBubbleCluster(buf, 0.04, 0.32, count = 7, baseFreqHz = 185.0 * pitch, gain = 0.32, seed = 219)
    return toPcm16(buf, 0.82f, roomWet = 0.14f)
  }

  /** Weeping Lamp: eerie hollow thick-glass vessel toll + slow heavy viscous condensation drops. */
  private fun renderWeepingLampBuy(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.52).toInt()
    val buf = FloatArray(len)
    val glassPartials = doubleArrayOf(182.0, 346.0, 588.0, 91.0)
    val glassAmps = doubleArrayOf(0.46, 0.24, 0.12, 0.30)
    val glassDecays = doubleArrayOf(5.8, 8.5, 13.0, 6.2)
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
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
    val len = (FOLEY_SAMPLE_RATE * 0.56).toInt()
    val buf = FloatArray(len)
    addWetSquelch(buf, 0.0, 0.48, pitch, gain = 0.72, formantStartHz = 420.0, formantEndHz = 150.0, wobbleHz = 6.2, seed = 235)
    addBubbleCluster(buf, 0.05, 0.38, count = 8, baseFreqHz = 140.0 * pitch, gain = 0.34, seed = 241)
    addIronModalRing(buf, 0.28, 0.24, 175.0 * pitch, gain = 0.36, decayScale = 1.3, seed = 247, includeRatchetGrit = false)
    return toPcm16(buf, 0.84f, roomWet = 0.18f)
  }

  /** Pull Pale Tooth: taut periodontal ligament tearing out of wet plaster + enamel root wrench pop. */
  private fun renderToothPull(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.36).toInt()
    val buf = FloatArray(len)
    addTendonTear(buf, 0.0, 0.28, pitch, gain = 0.84, fiberCount = 18, seed = 303)
    addWetSquelch(buf, 0.08, 0.22, pitch, gain = 0.42, formantStartHz = 380.0, formantEndHz = 190.0, seed = 309)
    addBubbleCluster(buf, 0.16, 0.12, count = 4, baseFreqHz = 230.0 * pitch, gain = 0.28, seed = 313)
    return toPcm16(buf, 0.82f, roomWet = 0.15f)
  }

  /** Wipe Glass Eye: damp rubbery condensation smear + wet mucosal eyelid squelch + hollow sphere roll. */
  private fun renderGlassEyeWipe(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.42).toInt()
    val buf = FloatArray(len)
    addWetSquelch(buf, 0.0, 0.34, pitch, gain = 0.74, formantStartHz = 290.0, formantEndHz = 780.0, wobbleHz = 6.0, seed = 404)
    addBubbleCluster(buf, 0.14, 0.20, count = 5, baseFreqHz = 210.0 * pitch, gain = 0.26, seed = 409)
    return toPcm16(buf, 0.80f, roomWet = 0.14f)
  }

  /** Prod Swollen Nodule: taut blister membrane tearing open + juicy oozing wet squelch & bubble burst. */
  private fun renderNoduleProd(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.44).toInt()
    val buf = FloatArray(len)
    addTendonTear(buf, 0.0, 0.16, pitch, gain = 0.56, fiberCount = 12, seed = 505)
    addWetSquelch(buf, 0.04, 0.38, pitch, gain = 0.84, formantStartHz = 480.0, formantEndHz = 175.0, wobbleHz = 9.5, seed = 509)
    addBubbleCluster(buf, 0.07, 0.30, count = 7, baseFreqHz = 175.0 * pitch, gain = 0.34, seed = 515)
    return toPcm16(buf, 0.84f, roomWet = 0.14f)
  }

  /** Pry Split Chitin Rib: deep calcified bone wrench + long fibrous tendon/ligament ripping + marrow groan. */
  private fun renderRibPry(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.52).toInt()
    val buf = FloatArray(len)
    addTendonTear(buf, 0.0, 0.44, pitch * 0.92, gain = 0.90, fiberCount = 24, seed = 606)
    addWetSquelch(buf, 0.10, 0.36, pitch, gain = 0.50, formantStartHz = 280.0, formantEndHz = 540.0, wobbleHz = 6.5, seed = 611)
    addBubbleCluster(buf, 0.18, 0.24, count = 5, baseFreqHz = 155.0 * pitch, gain = 0.24, seed = 617)
    return toPcm16(buf, 0.84f, roomWet = 0.18f)
  }

  /** Bottle Specimen Jar: dark thick-glass vessel resonance + viscous suction wax-cork seal. */
  private fun renderJarBottle(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.40).toInt()
    val buf = FloatArray(len)
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
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
    val len = (FOLEY_SAMPLE_RATE * duration).toInt()
    val buf = FloatArray(len)

    // Deep peristaltic body pressure swell (60Hz -> 33Hz)
    var subPhase = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val frac = t / duration
      val env = sin(PI * frac).coerceAtLeast(0.0)
      val f = (33.0 + 27.0 * exp(-t * 2.0)) * pitch
      subPhase += TWO_PI * f / FOLEY_SAMPLE_RATE
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

  /**
   * Choir-Lung Grafting (~1.18s): hollow pleural air-draw through wet bronchi, slow calcified
   * cartilage expansion, and a deep resonant respiratory settle inside the masonry cavity.
   */
  private fun renderChoirLungGraft(pitch: Double): ShortArray {
    val duration = 1.18
    val len = (FOLEY_SAMPLE_RATE * duration).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(761)
    val inhaleFilter = SvFilter()
    val cavityFilter = SvFilter()

    var breathPhase = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val raw = noise.nextFloat().toDouble()
      // Hollow pleural inhale (0..540ms) then sagging exhale (540..1180ms)
      val inhaleEnv = if (t < 0.54) sin(PI * (t / 0.54)).coerceAtLeast(0.0) else 0.0
      val exhaleEnv = if (t >= 0.46) sin(PI * ((t - 0.46) / 0.72).coerceAtMost(1.0)).coerceAtLeast(0.0) else 0.0
      val inhaleAir =
        inhaleFilter.processBandpass(raw, (290.0 + t * 520.0) * pitch, 2.4) * inhaleEnv * 0.68
      val exhaleAir =
        cavityFilter.processBandpass(raw, (460.0 - (t - 0.46).coerceAtLeast(0.0) * 320.0) * pitch, 2.8) *
          exhaleEnv *
          0.60
      val subFreq = (48.0 + 18.0 * sin(PI * (t / duration))) * pitch
      breathPhase += TWO_PI * subFreq / FOLEY_SAMPLE_RATE
      val pleuralBody =
        (sin(breathPhase) * 0.48 + sin(breathPhase * 2.02) * 0.18) *
          sin(PI * (t / duration).coerceAtMost(1.0)).coerceAtLeast(0.0)
      buf[i] = (inhaleAir + exhaleAir + pleuralBody).toFloat()
    }

    // Wet pleural cartilage expansion + deep mucosal settle
    addTendonTear(buf, 0.14, 0.38, pitch * 0.84, gain = 0.54, fiberCount = 16, seed = 769)
    addWetSquelch(
      buf,
      0.38,
      0.66,
      pitch * 0.88,
      gain = 0.62,
      formantStartHz = 380.0,
      formantEndHz = 140.0,
      wobbleHz = 4.6,
      seed = 773,
    )
    addBubbleCluster(buf, 0.24, 0.62, count = 8, baseFreqHz = 152.0 * pitch, gain = 0.28, seed = 787)
    return toPcm16(buf, 0.84f, roomWet = 0.22f)
  }

  /** Dredged Relic Inspection: subtle damp iron/slate pickup clink + wet oilcloth/paper shift. */
  private fun renderRelicInspect(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.24).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(797)
    val brushFilter = SvFilter()
    addIronModalRing(
      buf = buf,
      startSec = 0.0,
      durationSec = 0.16,
      baseFreqHz = 365.0 * pitch,
      gain = 0.30,
      decayScale = 2.0,
      seed = 801,
      includeRatchetGrit = false,
      clangScale = 0.26,
    )
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val env = sin(PI * (t / 0.24).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val brush =
        brushFilter.processBandpass(noise.nextFloat().toDouble(), 780.0 - t * 1400.0, 1.8) *
          env *
          0.42
      val settle =
        sin(TWO_PI * (132.0 * pitch) * t) * exp(-t * 22.0) * (1.0 - exp(-t * 140.0)) * 0.24
      buf[i] += (brush + settle).toFloat()
    }
    return toPcm16(buf, 0.68f, roomWet = 0.14f)
  }

  /** Suture Scar Node: surgical thread dragging taut through damp flesh + crisp tendon cinch. */
  private fun renderScarStitch(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.38).toInt()
    val buf = FloatArray(len)
    addTendonTear(buf, 0.0, 0.30, pitch * 1.05, gain = 0.74, fiberCount = 15, seed = 808)
    addWetSquelch(buf, 0.06, 0.26, pitch, gain = 0.44, formantStartHz = 520.0, formantEndHz = 220.0, wobbleHz = 9.0, seed = 811)
    return toPcm16(buf, 0.80f, roomWet = 0.14f)
  }

  /** Heavy rusted cast-iron floor sump grate latch slam & metallic ring. */
  private fun renderIronClank(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.34).toInt()
    val buf = FloatArray(len)
    addIronModalRing(buf, 0.0, 0.32, 205.0 * pitch, gain = 0.78, decayScale = 0.92, seed = 909, includeRatchetGrit = true)
    addIronModalRing(buf, 0.048, 0.22, 290.0 * pitch, gain = 0.34, decayScale = 1.25, seed = 919, includeRatchetGrit = false)
    return toPcm16(buf, 0.84f, roomWet = 0.20f)
  }

  /** Submerged rusted iron chain-link haul & dark viscous sump water slosh. */
  private fun renderSumpDredge(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.56).toInt()
    val buf = FloatArray(len)
    addWetSquelch(buf, 0.0, 0.50, pitch, gain = 0.64, formantStartHz = 280.0, formantEndHz = 460.0, wobbleHz = 6.8, seed = 1010)
    addBubbleCluster(buf, 0.04, 0.42, count = 8, baseFreqHz = 160.0 * pitch, gain = 0.28, seed = 1013)
    addIronModalRing(buf, 0.02, 0.20, 320.0 * pitch, gain = 0.42, decayScale = 1.4, seed = 1019, includeRatchetGrit = false)
    addIronModalRing(buf, 0.16, 0.24, 255.0 * pitch, gain = 0.46, decayScale = 1.2, seed = 1021, includeRatchetGrit = false)
    return toPcm16(buf, 0.84f, roomWet = 0.20f)
  }

  /** Deeply muffled triple knuckle-knock behind thick wet masonry. */
  private fun renderEchoTripleKnock(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.62).toInt()
    val buf = FloatArray(len)
    val offsets = doubleArrayOf(0.0, 0.16, 0.325)
    val freqs = doubleArrayOf(96.0, 88.0, 82.0)
    val amps = doubleArrayOf(0.66, 0.52, 0.40)
    val noise = NoiseGen(1061)
    val masonryFilter = SvFilter()
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
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
    val len = (FOLEY_SAMPLE_RATE * 0.44).toInt()
    val buf = FloatArray(len)
    addIronModalRing(buf, 0.0, 0.40, 185.0 * pitch, gain = 0.80, decayScale = 0.85, seed = 1111, includeRatchetGrit = true)
    addIronModalRing(buf, 0.095, 0.30, 278.0 * pitch, gain = 0.52, decayScale = 1.05, seed = 1117, includeRatchetGrit = false)
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val slam = sin(TWO_PI * ((72.0 - t * 80.0).coerceAtLeast(34.0) * pitch) * t) * exp(-t * 10.5) * 0.65
      buf[i] += slam.toFloat()
    }
    return toPcm16(buf, 0.86f, roomWet = 0.22f)
  }

  /** Iron clamp release snapping open into a cavernous tearing marrow & wet tissue bloom. */
  private fun renderMarrowBloom(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.78).toInt()
    val buf = FloatArray(len)
    addIronModalRing(buf, 0.0, 0.28, 225.0 * pitch, gain = 0.52, decayScale = 1.15, seed = 1212, includeRatchetGrit = true)
    addTendonTear(buf, 0.04, 0.48, pitch * 0.90, gain = 0.76, fiberCount = 22, seed = 1217)
    addWetSquelch(buf, 0.08, 0.64, pitch, gain = 0.74, formantStartHz = 520.0, formantEndHz = 150.0, wobbleHz = 6.5, seed = 1223)
    addBubbleCluster(buf, 0.14, 0.52, count = 9, baseFreqHz = 160.0 * pitch, gain = 0.28, seed = 1229)
    return toPcm16(buf, 0.86f, roomWet = 0.20f)
  }

  /** Cavernous wet esophageal swallow + mucosal squelch + calcified tooth settle. */
  private fun renderThroatSwallow(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.60).toInt()
    val buf = FloatArray(len)
    addWetSquelch(buf, 0.0, 0.52, pitch * 0.92, gain = 0.82, formantStartHz = 460.0, formantEndHz = 140.0, wobbleHz = 5.5, seed = 1260)
    addBubbleCluster(buf, 0.08, 0.38, count = 7, baseFreqHz = 145.0 * pitch, gain = 0.30, seed = 1265)
    addTendonTear(buf, 0.22, 0.24, pitch * 0.86, gain = 0.44, fiberCount = 12, seed = 1271)
    return toPcm16(buf, 0.82f, roomWet = 0.18f)
  }

  /** Heavy cold surgical iron shear slicing straight through thick sinew into a hollow collapse. */
  private fun renderExcisionShear(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.96).toInt()
    val buf = FloatArray(len)
    // Heavy cold iron shears biting shut
    addIronModalRing(buf, 0.0, 0.46, 265.0 * pitch, gain = 0.68, decayScale = 0.82, seed = 1313, includeRatchetGrit = true)
    // Thick sinew & flesh ripping apart under the shear
    addTendonTear(buf, 0.04, 0.56, pitch * 0.88, gain = 0.82, fiberCount = 26, seed = 1319)
    addWetSquelch(buf, 0.18, 0.66, pitch * 0.86, gain = 0.58, formantStartHz = 380.0, formantEndHz = 120.0, wobbleHz = 4.8, seed = 1327)
    var subPhase = 0.0
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val f = (72.0 * exp(-t * 2.2) + 29.0) * pitch
      subPhase += TWO_PI * f / FOLEY_SAMPLE_RATE
      buf[i] += (sin(subPhase) * exp(-t * 2.6) * 0.62).toFloat()
    }
    return toPcm16(buf, 0.86f, roomWet = 0.22f)
  }

  /** Low mournful damp seam sigh dissolving into muffled exterior rain (Arc 1 -> Arc 2). */
  private fun renderPleadSalvation(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.98).toInt()
    val buf = FloatArray(len)
    val noise = NoiseGen(1414)
    val rainFilter = SvFilter()
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
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
    val len = (FOLEY_SAMPLE_RATE * 0.28).toInt()
    val buf = FloatArray(len)
    addBubbleCluster(buf, 0.0, 0.04, count = 1, baseFreqHz = 225.0 * pitch, gain = 0.52, seed = 1451)
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val cistern = sin(TWO_PI * (122.0 * pitch) * t) * exp(-t * 14.0) * (1.0 - exp(-t * 90.0)) * 0.26
      buf[i] += cistern.toFloat()
    }
    return toPcm16(buf, 0.50f, roomWet = 0.24f)
  }

  /** Deep brooding architectural groan + wet masonry shift when advancing a Phase. */
  private fun renderPhaseShift(pitch: Double): ShortArray {
    val len = (FOLEY_SAMPLE_RATE * 0.78).toInt()
    val buf = FloatArray(len)
    addWetSquelch(buf, 0.06, 0.62, pitch * 0.85, gain = 0.42, formantStartHz = 180.0, formantEndHz = 310.0, wobbleHz = 3.5, seed = 1470)
    for (i in 0 until len) {
      val t = i.toDouble() / FOLEY_SAMPLE_RATE
      val env = sin(PI * (t / 0.78).coerceAtMost(1.0)).coerceAtLeast(0.0)
      val sub = sin(TWO_PI * (46.0 * pitch) * t) * 0.58 + sin(TWO_PI * (69.0 * pitch) * t) * 0.26
      buf[i] += (sub * env).toFloat()
    }
    return toPcm16(buf, 0.72f, roomWet = 0.22f)
  }
}
