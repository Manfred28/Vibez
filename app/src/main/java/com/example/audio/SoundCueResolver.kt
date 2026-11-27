package com.example.audio

import com.example.game.Arc3UnmakeCategory
import com.example.game.CorroborationId
import com.example.game.CreatureType
import com.example.game.DeskAnomalyType
import com.example.game.GameArc
import com.example.game.InvestigatorContact
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
  GRAFT_CHOIR_LUNG("graft_choir_lung", minIntervalMs = 300L),
  RELIC_INSPECT("relic_inspect", minIntervalMs = 95L),
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
  ARC2_LAMP_ON("arc2_lamp_on", minIntervalMs = 95L),
  ARC2_LAMP_OFF("arc2_lamp_off", minIntervalMs = 95L),
  ARC2_CONTACT_ARCHIVIST("arc2_contact_archivist", minIntervalMs = 80L),
  ARC2_CONTACT_ORDERLY("arc2_contact_orderly", minIntervalMs = 80L),
  ARC2_CONTACT_SCANNER("arc2_contact_scanner", minIntervalMs = 80L),
  ARC2_CONTACT_TRANSCRIBER("arc2_contact_transcriber", minIntervalMs = 80L),
  ARC2_DEDUCTION("arc2_deduction", minIntervalMs = 120L),
  ARC2_CORROBORATION_LOCK("arc2_corroboration_lock", minIntervalMs = 110L),
  ARC2_STRING_WIRE("arc2_string_wire", minIntervalMs = 110L),
  ARC2_STRING_UNWIRE("arc2_string_unwire", minIntervalMs = 110L),
  ARC2_REEL_SCRUB("arc2_reel_scrub", minIntervalMs = 240L),
  ARC2_REDACTION_DISSOLVE("arc2_redaction_dissolve", minIntervalMs = 220L),
  ARC2_ANOMALY_ENVELOPE("arc2_anomaly_envelope", minIntervalMs = 110L),
  ARC2_ANOMALY_SPECTACLES("arc2_anomaly_spectacles", minIntervalMs = 110L),
  ARC2_ANOMALY_REEL_TAPE("arc2_anomaly_reel_tape", minIntervalMs = 110L),
  ARC2_ANOMALY_ENAMEL("arc2_anomaly_enamel", minIntervalMs = 110L),
  ARC2_CHAPTER("arc2_chapter", minIntervalMs = 240L),
  ARC2_FILE_ZERO("arc2_file_zero", minIntervalMs = 450L),
  ARC3_ENTER("arc3_enter", minIntervalMs = 450L),
  ARC3_WRENCH("arc3_wrench", minIntervalMs = 60L),
  ARC3_JAR_SHATTER("arc3_jar_shatter", minIntervalMs = 110L),
  ARC3_UNMAKE_TEAR("arc3_unmake_tear", minIntervalMs = 150L),
  ARC3_RUPTURE_POP("arc3_rupture_pop", minIntervalMs = 100L),
  ARC3_WEDGE_DRIVE("arc3_wedge_drive", minIntervalMs = 280L),
  ARC3_CLOT_WARNING("arc3_clot_warning", minIntervalMs = 900L),
  ARC3_FINAL_PULL("arc3_final_pull", minIntervalMs = 350L),
  ARC3_SUBMERSION_END("arc3_submersion_end", minIntervalMs = 600L),
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
  ARC2_LAMP_TOGGLE,
  ARC2_CONTACT,
  ARC2_DEDUCTION,
  ARC2_STRING_WIRE,
  ARC2_REEL_SCRUB,
  ARC2_REDACTION_DISSOLVE,
  ARC2_DESK_ANOMALY,
  ARC2_CHAPTER,
  ARC3_ENTER,
  ARC3_WRENCH,
  ARC3_UNMAKE,
  ARC3_RUPTURE_TEAR,
  ARC3_WEDGE_DRIVE,
  ARC3_FINAL_PULL,
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
        if (next.choirLungs > prev.choirLungs) {
          cues.add(SoundCue.GRAFT_CHOIR_LUNG)
        } else if (next.graftedTendrils > prev.graftedTendrils) {
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
      SoundActionHint.ARC2_LAMP_TOGGLE -> {
        if (next.arc2.lampOn != prev.arc2.lampOn) {
          cues.add(if (next.arc2.lampOn) SoundCue.ARC2_LAMP_ON else SoundCue.ARC2_LAMP_OFF)
        }
      }
      SoundActionHint.ARC2_CONTACT -> {
        val hiredContact =
          InvestigatorContact.entries.firstOrNull { contact ->
            next.arc2.contactCount(contact) > prev.arc2.contactCount(contact)
          }
        when (hiredContact) {
          InvestigatorContact.NIGHT_ARCHIVIST -> cues.add(SoundCue.ARC2_CONTACT_ARCHIVIST)
          InvestigatorContact.BRIBED_ORDERLY -> cues.add(SoundCue.ARC2_CONTACT_ORDERLY)
          InvestigatorContact.MICROFILM_SCANNER -> cues.add(SoundCue.ARC2_CONTACT_SCANNER)
          InvestigatorContact.AUDIO_TRANSCRIBER -> cues.add(SoundCue.ARC2_CONTACT_TRANSCRIBER)
          null -> Unit
        }
      }
      SoundActionHint.ARC2_DEDUCTION -> {
        if (next.arc2.pinnedExhibits.size > prev.arc2.pinnedExhibits.size) {
          cues.add(SoundCue.ARC2_DEDUCTION)
        }
      }
      SoundActionHint.ARC2_STRING_WIRE -> {
        if (next.arc2.wiredPairs.size < prev.arc2.wiredPairs.size) {
          cues.add(SoundCue.ARC2_STRING_UNWIRE)
        } else if (next.arc2.wiredPairs != prev.arc2.wiredPairs) {
          val addedPairs = next.arc2.wiredPairs - prev.arc2.wiredPairs
          val matchedCorroboration = addedPairs.any { CorroborationId.matchPair(it) != null }
          cues.add(
            if (matchedCorroboration) SoundCue.ARC2_CORROBORATION_LOCK
            else SoundCue.ARC2_STRING_WIRE
          )
        }
      }
      SoundActionHint.ARC2_REEL_SCRUB -> {
        if (next.arc2.reelScrubCount > prev.arc2.reelScrubCount) {
          cues.add(SoundCue.ARC2_REEL_SCRUB)
        }
      }
      SoundActionHint.ARC2_REDACTION_DISSOLVE -> {
        if (next.arc2.dissolvedRedactions.size > prev.arc2.dissolvedRedactions.size) {
          cues.add(SoundCue.ARC2_REDACTION_DISSOLVE)
        }
      }
      SoundActionHint.ARC2_DESK_ANOMALY -> {
        val interactedDesk =
          DeskAnomalyType.entries.firstOrNull { type ->
            (next.arc2.deskAnomalyInteractions[type] ?: 0) > (prev.arc2.deskAnomalyInteractions[type] ?: 0)
          }
        when (interactedDesk) {
          DeskAnomalyType.EVIDENCE_ENVELOPE -> cues.add(SoundCue.ARC2_ANOMALY_ENVELOPE)
          DeskAnomalyType.FOGGED_SPECTACLES -> cues.add(SoundCue.ARC2_ANOMALY_SPECTACLES)
          DeskAnomalyType.REEL_TAPE -> cues.add(SoundCue.ARC2_ANOMALY_REEL_TAPE)
          DeskAnomalyType.ENAMEL_EXHIBIT -> cues.add(SoundCue.ARC2_ANOMALY_ENAMEL)
          null -> Unit
        }
      }
      SoundActionHint.ARC2_CHAPTER -> {
        if (next.arc2.fileZeroRevealed && !prev.arc2.fileZeroRevealed) {
          cues.add(SoundCue.ARC2_FILE_ZERO)
        } else if (next.arc2.completedCasePivots > prev.arc2.completedCasePivots) {
          cues.add(SoundCue.ARC2_CHAPTER)
        }
      }
      SoundActionHint.ARC3_ENTER -> {
        if (next.activeArc == GameArc.ARC_3_CONVERGENCE && prev.activeArc != GameArc.ARC_3_CONVERGENCE) {
          cues.add(SoundCue.ARC3_ENTER)
        }
      }
      SoundActionHint.ARC3_WRENCH -> {
        if (next.arc3.wrenchCount > prev.arc3.wrenchCount) {
          cues.add(SoundCue.ARC3_WRENCH)
        }
      }
      SoundActionHint.ARC3_UNMAKE -> {
        val addedActs = next.arc3.completedUnmakes - prev.arc3.completedUnmakes
        if (addedActs.isNotEmpty()) {
          if (addedActs.any { it.category == Arc3UnmakeCategory.SHATTER_JAR }) {
            cues.add(SoundCue.ARC3_JAR_SHATTER)
          } else {
            cues.add(SoundCue.ARC3_UNMAKE_TEAR)
          }
        }
      }
      SoundActionHint.ARC3_RUPTURE_TEAR -> {
        if (next.arc3.totalRuptureInteractions > prev.arc3.totalRuptureInteractions) {
          cues.add(SoundCue.ARC3_RUPTURE_POP)
        }
      }
      SoundActionHint.ARC3_WEDGE_DRIVE -> {
        if (next.arc3.lockedWedges.size > prev.arc3.lockedWedges.size) {
          cues.add(SoundCue.ARC3_WEDGE_DRIVE)
        }
      }
      SoundActionHint.ARC3_FINAL_PULL -> {
        if (next.arc3.submersionComplete && !prev.arc3.submersionComplete) {
          cues.add(SoundCue.ARC3_SUBMERSION_END)
        } else if (next.arc3.finalPullsCompleted > prev.arc3.finalPullsCompleted) {
          cues.add(SoundCue.ARC3_FINAL_PULL)
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
      action == SoundActionHint.TICK &&
        next.activeArc == GameArc.ARC_3_CONVERGENCE &&
        next.arc3.isActivelyClotting &&
        !prev.arc3.isActivelyClotting
    ) {
      cues.add(SoundCue.ARC3_CLOT_WARNING)
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
        SoundCue.ARC2_FILE_ZERO !in cues &&
        SoundCue.ARC3_SUBMERSION_END !in cues
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
        val haunt = state.arc2.hauntDepth.coerceIn(0f, 1f)
        val lampOff = !state.arc2.lampOn
        val scrubbing = state.arc2.isReelScrubbing
        // Slow Boil audio quarantine: no flesh/cardiac chamber bleed until Case IV (hauntDepth >= 0.60)
        val roomBleed = ((haunt - 0.58f) / 0.42f).coerceIn(0f, 1f)
        AmbientSoundProfile(
          activeArc = GameArc.ARC_2_INVESTIGATION,
          phase = if (state.arc2.fileZeroRevealed || state.arc2.caseAngle >= 5) 6 else 5,
          humGain = if (lampOff) (0.10f + roomBleed * 0.14f) else 0.30f,
          dripRate = (0.06f + roomBleed * 0.44f + if (lampOff && haunt >= 0.55f) 0.16f else 0f).coerceIn(0f, 1f),
          fleshDepth = (roomBleed * 0.58f).coerceIn(0f, 1f),
          cardiacPulse = if (state.arc2.fileZeroRevealed) 0.45f else (roomBleed * 0.28f),
          stillnessFocus = if (scrubbing) 1.0f else if (lampOff) 0.85f else 0f,
          clampTension = if (scrubbing) 0.35f else 0f,
          rainMix = if (lampOff) 0.88f else 0.66f,
          complicityBleed = haunt,
        )
      }
      GameArc.ARC_3_CONVERGENCE -> {
        val breach = state.arc3
        val flood = breach.floodLevel.coerceIn(0f, 1f)
        val submerged = breach.submersionComplete
        val clotting = breach.isActivelyClotting
        AmbientSoundProfile(
          activeArc = GameArc.ARC_3_CONVERGENCE,
          phase = 7,
          humGain = if (submerged) 0.14f else (0.32f + flood * 0.24f).coerceIn(0f, 1f),
          dripRate = if (submerged) 0.12f else (0.38f + flood * 0.42f).coerceIn(0f, 1f),
          fleshDepth = (0.45f + flood * 0.55f).coerceIn(0f, 1f),
          cardiacPulse = if (submerged) 0.48f else (0.28f + flood * 0.68f).coerceIn(0f, 1f),
          stillnessFocus = if (submerged) 0.92f else 0f,
          clampTension = if (clotting) 0.62f else 0f,
          rainMix = 0f,
          complicityBleed = (0.55f + flood * 0.45f).coerceIn(0f, 1f),
        )
      }
    }
}
