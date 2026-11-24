package com.example.audio

import com.example.game.CreatureType
import com.example.game.DeskAnomalyType
import com.example.game.GameArc
import com.example.game.OrganGameState
import com.example.game.WallAnomalyType

/** Semantic sound cues shared 1:1 between the Android and Web procedural foley engines. */
enum class SoundCue(val id: String, val minIntervalMs: Long = 55L) {
  VALVE_PRESS("valve_press", minIntervalMs = 62L),
  CREATURE_BUY_SUTURE_HAND("creature_buy_suture_hand", minIntervalMs = 80L),
  CREATURE_BUY_LEECH_CLERK("creature_buy_leech_clerk", minIntervalMs = 85L),
  CREATURE_BUY_WEEPING_LAMP("creature_buy_weeping_lamp", minIntervalMs = 95L),
  CREATURE_BUY_SUMP_SIPHON("creature_buy_sump_siphon", minIntervalMs = 100L),
  ANOMALY_TOOTH("anomaly_tooth", minIntervalMs = 90L),
  ANOMALY_EYE("anomaly_eye", minIntervalMs = 110L),
  ANOMALY_NODULE("anomaly_nodule", minIntervalMs = 110L),
  ANOMALY_RIB("anomaly_rib", minIntervalMs = 120L),
  JAR_BOTTLE("jar_bottle", minIntervalMs = 120L),
  GRAFT_PERFORM("graft_perform", minIntervalMs = 260L),
  SCAR_UNLOCK("scar_unlock", minIntervalMs = 110L),
  DRAIN_TOGGLE("drain_toggle", minIntervalMs = 110L),
  SUMP_DREDGE("sump_dredge", minIntervalMs = 180L),
  ECHO_PULSE("echo_pulse", minIntervalMs = 260L),
  CLAMP_ENGAGE("clamp_engage", minIntervalMs = 160L),
  MARROW_BLOOM("marrow_bloom", minIntervalMs = 300L),
  THROAT_OFFER("throat_offer", minIntervalMs = 200L),
  EXCISION_CUT("excision_cut", minIntervalMs = 400L),
  PLEAD_SALVATION("plead_salvation", minIntervalMs = 450L),
  STORY_DRIP("story_drip", minIntervalMs = 380L),
  PHASE_SHIFT("phase_shift", minIntervalMs = 450L),
  ARC2_STAMP("arc2_stamp", minIntervalMs = 62L),
  ARC2_CONTACT("arc2_contact", minIntervalMs = 80L),
  ARC2_DEDUCTION("arc2_deduction", minIntervalMs = 120L),
  ARC2_DESK_ANOMALY("arc2_desk_anomaly", minIntervalMs = 110L),
  ARC2_CHAPTER("arc2_chapter", minIntervalMs = 240L),
  ARC2_FILE_ZERO("arc2_file_zero", minIntervalMs = 450L),
  ARC3_ENTER("arc3_enter", minIntervalMs = 450L),
}

/** High-level action context passed alongside state diffs so only valid state changes emit SFX. */
enum class SoundActionHint {
  TICK,
  VALVE_PRESS,
  CREATURE_BUY,
  JAR_BOTTLE,
  GRAFT_PERFORM,
  ANOMALY_INTERACT,
  SCAR_UNLOCK,
  DRAIN_TOGGLE,
  SUMP_DREDGE,
  ECHO_DIVERSION,
  CLAMP_ENGAGE,
  THROAT_OFFER,
  EXCISION,
  BREACH_TO_ARC2,
  ARC2_STAMP,
  ARC2_CONTACT,
  ARC2_DEDUCTION,
  ARC2_DESK_ANOMALY,
  ARC2_CHAPTER,
  ARC3_ENTER,
  DEV,
}

/** Smooth ambient soundscape targets derived purely from [OrganGameState]. */
data class AmbientSoundProfile(
  val activeArc: GameArc,
  val phase: Int,
  val humGain: Float,
  val dripRate: Float,
  val fleshDepth: Float,
  val cardiacPulse: Float,
  val stillnessFocus: Float,
  val clampTension: Float,
  val rainMix: Float,
  val complicityBleed: Float,
)

/**
 * Pure, deterministic transition-diff cue resolver and ambient profile calculator.
 * Keeps audio routing consistent across manual actions and passive tick events (Echo pulses,
 * Marrow Blooms, Auto-Tendril Suture, phase transitions, and story drips).
 */
object SoundCueResolver {

  fun resolveTransitionCues(
    prev: OrganGameState,
    next: OrganGameState,
    action: SoundActionHint = SoundActionHint.TICK,
  ): List<SoundCue> {
    if (prev === next || action == SoundActionHint.DEV) return emptyList()
    val cues = ArrayList<SoundCue>(3)

    // 1. Primary action-driven cues (gated on genuine state change so failed taps stay silent)
    when (action) {
      SoundActionHint.VALVE_PRESS -> {
        if (next.cycleLifetimeIchor > prev.cycleLifetimeIchor) {
          cues.add(SoundCue.VALVE_PRESS)
        }
      }
      SoundActionHint.CREATURE_BUY -> {
        val boughtType =
          CreatureType.entries.firstOrNull { type ->
            next.creatureCount(type) > prev.creatureCount(type)
          }
        when (boughtType) {
          CreatureType.SUTURE_HAND -> cues.add(SoundCue.CREATURE_BUY_SUTURE_HAND)
          CreatureType.LEECH_CLERK -> cues.add(SoundCue.CREATURE_BUY_LEECH_CLERK)
          CreatureType.WEEPING_LAMP -> cues.add(SoundCue.CREATURE_BUY_WEEPING_LAMP)
          CreatureType.SUMP_SIPHON -> cues.add(SoundCue.CREATURE_BUY_SUMP_SIPHON)
          null -> Unit
        }
      }
      SoundActionHint.JAR_BOTTLE -> {
        if (next.ownedJars.size > prev.ownedJars.size) {
          cues.add(SoundCue.JAR_BOTTLE)
        }
      }
      SoundActionHint.GRAFT_PERFORM -> {
        if (next.graftedTendrils > prev.graftedTendrils || next.choirLungs > prev.choirLungs) {
          cues.add(SoundCue.GRAFT_PERFORM)
        }
      }
      SoundActionHint.ANOMALY_INTERACT -> {
        val interactedType =
          WallAnomalyType.entries.firstOrNull { type ->
            (next.anomalyInteractions[type] ?: 0) > (prev.anomalyInteractions[type] ?: 0)
          }
        when (interactedType) {
          WallAnomalyType.PALE_TOOTH -> cues.add(SoundCue.ANOMALY_TOOTH)
          WallAnomalyType.GLASS_EYE -> cues.add(SoundCue.ANOMALY_EYE)
          WallAnomalyType.SWOLLEN_NODULE -> cues.add(SoundCue.ANOMALY_NODULE)
          WallAnomalyType.CHITIN_RIB -> cues.add(SoundCue.ANOMALY_RIB)
          null -> Unit
        }
      }
      SoundActionHint.SCAR_UNLOCK -> {
        val repPrev = prev.repeatableScarRanks.values.sum()
        val repNext = next.repeatableScarRanks.values.sum()
        if (next.unlockedScarNodes.size > prev.unlockedScarNodes.size || repNext > repPrev) {
          cues.add(SoundCue.SCAR_UNLOCK)
        }
      }
      SoundActionHint.DRAIN_TOGGLE -> {
        if (next.drainSealed != prev.drainSealed) {
          cues.add(SoundCue.DRAIN_TOGGLE)
        }
      }
      SoundActionHint.SUMP_DREDGE -> {
        if (next.lifetimeRelicsDredged > prev.lifetimeRelicsDredged) {
          cues.add(SoundCue.SUMP_DREDGE)
        }
      }
      SoundActionHint.ECHO_DIVERSION -> {
        if (next.echoDiversionPercent != prev.echoDiversionPercent) {
          cues.add(SoundCue.DRAIN_TOGGLE)
        }
      }
      SoundActionHint.CLAMP_ENGAGE -> {
        if (next.clampRemainingSeconds > prev.clampRemainingSeconds) {
          cues.add(SoundCue.CLAMP_ENGAGE)
        }
      }
      SoundActionHint.THROAT_OFFER -> {
        if (next.unredactedDossierLines.size > prev.unredactedDossierLines.size) {
          cues.add(SoundCue.THROAT_OFFER)
        }
      }
      SoundActionHint.EXCISION -> {
        if (next.cycle > prev.cycle) {
          cues.add(SoundCue.EXCISION_CUT)
        }
      }
      SoundActionHint.BREACH_TO_ARC2 -> {
        if (next.activeArc == GameArc.ARC_2_INVESTIGATION && prev.activeArc != GameArc.ARC_2_INVESTIGATION) {
          cues.add(SoundCue.PLEAD_SALVATION)
        }
      }
      SoundActionHint.ARC2_STAMP -> {
        if (next.arc2.lifetimeLeads > prev.arc2.lifetimeLeads) {
          cues.add(SoundCue.ARC2_STAMP)
        }
      }
      SoundActionHint.ARC2_CONTACT -> {
        if (next.arc2.contacts != prev.arc2.contacts) {
          cues.add(SoundCue.ARC2_CONTACT)
        }
      }
      SoundActionHint.ARC2_DEDUCTION -> {
        if (next.arc2.ownedDeductions.size > prev.arc2.ownedDeductions.size) {
          cues.add(SoundCue.ARC2_DEDUCTION)
        }
      }
      SoundActionHint.ARC2_DESK_ANOMALY -> {
        val interactedDesk =
          DeskAnomalyType.entries.any { type ->
            (next.arc2.deskAnomalyInteractions[type] ?: 0) > (prev.arc2.deskAnomalyInteractions[type] ?: 0)
          }
        if (interactedDesk) {
          cues.add(SoundCue.ARC2_DESK_ANOMALY)
        }
      }
      SoundActionHint.ARC2_CHAPTER -> {
        if (next.arc2.fileZeroRevealed && !prev.arc2.fileZeroRevealed) {
          cues.add(SoundCue.ARC2_FILE_ZERO)
        } else if (next.arc2.completedChapters > prev.arc2.completedChapters) {
          cues.add(SoundCue.ARC2_CHAPTER)
        }
      }
      SoundActionHint.ARC3_ENTER -> {
        if (next.activeArc == GameArc.ARC_3_CONVERGENCE && prev.activeArc != GameArc.ARC_3_CONVERGENCE) {
          cues.add(SoundCue.ARC3_ENTER)
        }
      }
      SoundActionHint.TICK, SoundActionHint.DEV -> Unit
    }

    // 2. Passive / emergent chamber events (can occur on ticks or alongside actions)
    if (next.echoPulsesTriggered > prev.echoPulsesTriggered) {
      cues.add(SoundCue.ECHO_PULSE)
    }
    if (next.marrowBloomCount > prev.marrowBloomCount) {
      cues.add(SoundCue.MARROW_BLOOM)
    }
    if (action == SoundActionHint.TICK && next.graftedTendrils > prev.graftedTendrils) {
      cues.add(SoundCue.GRAFT_PERFORM)
    }
    if (
      next.activeArc == GameArc.ARC_1_CHAMBER &&
        prev.activeArc == GameArc.ARC_1_CHAMBER &&
        next.phase > prev.phase &&
        SoundCue.EXCISION_CUT !in cues
    ) {
      cues.add(SoundCue.PHASE_SHIFT)
    }

    // 3. Quiet story condensation cue when a brand-new fragment unlocks (unless eclipsed by Excision/Arc shift)
    val newStoryUnlocked =
      next.activeDialogue != null &&
        next.activeDialogue.id != prev.activeDialogue?.id &&
        next.triggeredDialogueIds.size > prev.triggeredDialogueIds.size
    if (
      newStoryUnlocked &&
        SoundCue.EXCISION_CUT !in cues &&
        SoundCue.PLEAD_SALVATION !in cues &&
        SoundCue.ARC2_FILE_ZERO !in cues
    ) {
      cues.add(SoundCue.STORY_DRIP)
    }

    return cues
  }

  fun computeAmbientProfile(state: OrganGameState): AmbientSoundProfile =
    when (state.activeArc) {
      GameArc.ARC_1_CHAMBER -> {
        val p = state.phase.coerceIn(0, 4)
        val clamped = state.clampRemainingSeconds > 0.0
        val still = state.isStillnessActive
        val hum =
          when (p) {
            0 -> 0.24f
            1 -> 0.28f
            2 -> 0.34f
            3 -> 0.40f
            else -> 0.46f
          } * if (still) 0.72f else 1.0f
        val drips =
          when (p) {
            0 -> 0.08f
            1 -> 0.26f
            2 -> 0.42f
            3 -> 0.50f
            else -> 0.56f
          } + if (still) 0.28f else 0.0f
        val flesh =
          when (p) {
            0 -> 0.0f
            1 -> 0.12f
            2 -> 0.36f
            3 -> 0.62f
            else -> 0.82f
          }
        val cardiac = if (p >= 4) 0.72f else if (p == 3) 0.18f else 0.0f
        AmbientSoundProfile(
          activeArc = GameArc.ARC_1_CHAMBER,
          phase = p,
          humGain = hum.coerceIn(0f, 1f),
          dripRate = drips.coerceIn(0f, 1f),
          fleshDepth = flesh.coerceIn(0f, 1f),
          cardiacPulse = cardiac,
          stillnessFocus = if (still) 1f else 0f,
          clampTension = if (clamped) 0.9f else 0f,
          rainMix = 0f,
          complicityBleed = 0f,
        )
      }
      GameArc.ARC_2_INVESTIGATION -> {
        val complicity = state.arc2.complicity.coerceIn(0f, 1f)
        val silent = state.arc2.isSilenceActive
        AmbientSoundProfile(
          activeArc = GameArc.ARC_2_INVESTIGATION,
          phase = if (state.arc2.fileZeroRevealed) 6 else 5,
          humGain = if (silent) 0.20f else 0.28f,
          dripRate = (0.14f + complicity * 0.35f + if (silent) 0.22f else 0f).coerceIn(0f, 1f),
          fleshDepth = (complicity * 0.55f).coerceIn(0f, 1f),
          cardiacPulse = if (state.arc2.fileZeroRevealed) 0.45f else (complicity * 0.22f),
          stillnessFocus = if (silent) 0.85f else 0f,
          clampTension = 0f,
          rainMix = if (silent) 0.86f else 0.72f,
          complicityBleed = complicity,
        )
      }
      GameArc.ARC_3_CONVERGENCE ->
        AmbientSoundProfile(
          activeArc = GameArc.ARC_3_CONVERGENCE,
          phase = 6,
          humGain = 0.30f,
          dripRate = 0.32f,
          fleshDepth = 0.40f,
          cardiacPulse = 0.28f,
          stillnessFocus = 0.5f,
          clampTension = 0f,
          rainMix = 0.52f,
          complicityBleed = 0.65f,
        )
    }
}
