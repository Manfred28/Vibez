package com.example.audio

import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Pure Kotlin DSP procedural foley and room-ambience synthesizer facade generating 16-bit PCM
 * audio with zero external audio assets.
 *
 * Modular architecture:
 * - [FoleyDspPrimitives]: shared cast-iron modal resonance, tendon tearing, wet mucosa squelch,
 *   cavity bubble clusters, SVF filter, and damp tiled-chamber early slapback.
 * - [Arc1FoleyCueBank]: all Arc 1 chamber foley cues.
 * - [Arc2FoleyCueBank]: all Arc 2 haunted investigator desk and Arc 3 threshold cues.
 * - [AmbientBedGenerator]: continuous slewed subterranean room ambience and exterior rain bed.
 */
object ProceduralFoleySynth {
  const val SAMPLE_RATE: Int = FOLEY_SAMPLE_RATE

  fun synthesizeCue(cue: SoundCue, phase: Int = 0, pitchFactor: Float = 1f): ShortArray {
    val pitch = pitchFactor.coerceIn(0.82f, 1.18f).toDouble()
    val pDark = 1.0 - (phase.coerceIn(0, 4) * 0.035)
    return Arc1FoleyCueBank.synthesize(cue, pitch, pDark, phase)
      ?: Arc2FoleyCueBank.synthesize(cue, pitch)
      ?: Arc3FoleyCueBank.synthesize(cue, pitch)
      ?: error("Unhandled procedural sound cue: ${cue.id}")
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
          (sin(TWO_PI * 36.0 * t) * 0.60 + fleshLp * 1.35) *
            (sFlesh + sClamp * 0.55) *
            breathLfo *
            0.14

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
