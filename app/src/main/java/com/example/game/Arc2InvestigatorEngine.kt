package com.example.game

import com.example.data.DialogueCatalog
import kotlin.math.max
import kotlin.math.min

/**
 * Pure deterministic engine for Arc 2 ("The Inquiry" — Haunted Investigator Desk).
 * Mirrors Arc 1's cadence (Inspect/Stamp, Late-Night Silence, Desk Anomalies, Contacts,
 * Deductions, Red-String Chapters) while steadily escalating Complicity until File #00 reveals
 * that the Investigator is the subject who never left the chamber.
 */
object Arc2InvestigatorEngine {

  private val deskAnchors: List<Pair<Float, Float>> =
    listOf(
      0.16f to 0.26f,
      0.84f to 0.28f,
      0.17f to 0.74f,
      0.83f to 0.72f,
      0.24f to 0.50f,
      0.77f to 0.52f,
    )

  fun inspectCaseFile(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    if (state.activeArc != GameArc.ARC_2_INVESTIGATION) return state
    val desk = state.arc2
    val gained = desk.inspectYield
    val nextDesk =
      desk.copy(
        leads = desk.leads + gained,
        lifetimeLeads = desk.lifetimeLeads + gained,
        secondsSinceLastAction = 0.0,
        isSilenceActive = false,
      )
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = nextDesk), catalog)
  }

  fun tick(state: OrganGameState, deltaSeconds: Double, catalog: DialogueCatalog): OrganGameState {
    if (state.activeArc != GameArc.ARC_2_INVESTIGATION || deltaSeconds <= 0.0) return state
    val desk = state.arc2
    val nextIdle = desk.secondsSinceLastAction + deltaSeconds
    val silenceNow = nextIdle >= Arc2Balance.SILENCE_IDLE_DELAY_SEC

    val activeDesk = desk.copy(isSilenceActive = silenceNow)
    val gainedLeads = activeDesk.leadsPerSecond * deltaSeconds
    val gainedSilence = activeDesk.silencePerSecond * deltaSeconds
    val nextFocus = max(0.0, desk.focusRemainingSeconds - deltaSeconds)

    // Complicity creeps steadily with silence & completed chapters, bleeding Arc 1 into the desk
    val chapterFloor = (desk.completedChapters * 0.22f).coerceAtMost(0.92f)
    val silenceCreep = if (silenceNow) (deltaSeconds * 0.004).toFloat() else 0f
    val nextComplicity = max(desk.complicity + silenceCreep, chapterFloor).coerceIn(0.05f, 1.0f)

    var updatedDesk =
      activeDesk.copy(
        leads = desk.leads + gainedLeads,
        lifetimeLeads = desk.lifetimeLeads + gainedLeads,
        silence = desk.silence + gainedSilence,
        lifetimeSilence = desk.lifetimeSilence + gainedSilence,
        complicity = nextComplicity,
        secondsSinceLastAction = nextIdle,
        focusRemainingSeconds = nextFocus,
      )
    updatedDesk = advanceDeskAnomalySpawner(updatedDesk, deltaSeconds)
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = updatedDesk), catalog)
  }

  fun recruitContact(
    state: OrganGameState,
    contact: InvestigatorContact,
    catalog: DialogueCatalog,
  ): OrganGameState {
    if (state.activeArc != GameArc.ARC_2_INVESTIGATION) return state
    val desk = state.arc2
    val cost = desk.contactCost(contact)
    if (desk.leads < cost.toDouble()) return state

    val updatedContacts = desk.contacts.toMutableMap()
    updatedContacts[contact] = (updatedContacts[contact] ?: 0) + 1
    val nextDesk = desk.copy(leads = desk.leads - cost.toDouble(), contacts = updatedContacts)
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = nextDesk), catalog)
  }

  fun pinDeduction(
    state: OrganGameState,
    deduction: CaseDeduction,
    catalog: DialogueCatalog,
  ): OrganGameState {
    if (state.activeArc != GameArc.ARC_2_INVESTIGATION) return state
    val desk = state.arc2
    if (deduction in desk.ownedDeductions || desk.silence < deduction.silenceCost) return state

    val nextDesk =
      desk.copy(
        silence = desk.silence - deduction.silenceCost,
        ownedDeductions = desk.ownedDeductions + deduction,
        complicity = min(1.0f, desk.complicity + 0.08f),
      )
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = nextDesk), catalog)
  }

  fun interactDeskAnomaly(
    state: OrganGameState,
    anomalyId: Long,
    catalog: DialogueCatalog,
  ): OrganGameState {
    if (state.activeArc != GameArc.ARC_2_INVESTIGATION) return state
    val desk = state.arc2
    val anomaly = desk.activeDeskAnomalies.firstOrNull { it.id == anomalyId } ?: return state
    val remaining = desk.activeDeskAnomalies.filterNot { it.id == anomalyId }
    val mortarBoost =
      if (CaseDeduction.MORTAR_SAMPLE_MATCH in desk.ownedDeductions) {
        Arc2Balance.MORTAR_SILENCE_AND_ANOMALY_MULTIPLIER
      } else {
        1.0
      }

    val updatedCounts = desk.deskAnomalyInteractions.toMutableMap()
    updatedCounts[anomaly.type] = (updatedCounts[anomaly.type] ?: 0) + 1

    val nextDesk =
      when (anomaly.type) {
        DeskAnomalyType.EVIDENCE_ENVELOPE -> {
          val bonus = Arc2Balance.ENVELOPE_BASE_LEADS * desk.chapterMultiplier * mortarBoost
          desk.copy(
            leads = desk.leads + bonus,
            lifetimeLeads = desk.lifetimeLeads + bonus,
            complicity = min(1.0f, desk.complicity + 0.06f),
            activeDeskAnomalies = remaining,
            deskAnomalyInteractions = updatedCounts,
          )
        }
        DeskAnomalyType.FOGGED_SPECTACLES -> {
          desk.copy(
            focusRemainingSeconds = Arc2Balance.FOCUS_DURATION_SEC,
            complicity = min(1.0f, desk.complicity + 0.05f),
            activeDeskAnomalies = remaining,
            deskAnomalyInteractions = updatedCounts,
          )
        }
        DeskAnomalyType.RED_STRING -> {
          val leadBonus = Arc2Balance.RED_STRING_BASE_LEADS * desk.chapterMultiplier * mortarBoost
          val silenceBonus = Arc2Balance.RED_STRING_BASE_SILENCE * mortarBoost
          desk.copy(
            leads = desk.leads + leadBonus,
            lifetimeLeads = desk.lifetimeLeads + leadBonus,
            silence = desk.silence + silenceBonus,
            lifetimeSilence = desk.lifetimeSilence + silenceBonus,
            complicity = min(1.0f, desk.complicity + 0.08f),
            activeDeskAnomalies = remaining,
            deskAnomalyInteractions = updatedCounts,
          )
        }
        DeskAnomalyType.REEL_TAPE -> {
          val leadBonus = Arc2Balance.REEL_TAPE_BASE_LEADS * desk.chapterMultiplier * mortarBoost
          val silenceBonus = Arc2Balance.REEL_TAPE_BASE_SILENCE * mortarBoost
          desk.copy(
            leads = desk.leads + leadBonus,
            lifetimeLeads = desk.lifetimeLeads + leadBonus,
            silence = desk.silence + silenceBonus,
            lifetimeSilence = desk.lifetimeSilence + silenceBonus,
            focusRemainingSeconds = max(desk.focusRemainingSeconds, 8.0),
            complicity = min(1.0f, desk.complicity + 0.10f),
            activeDeskAnomalies = remaining,
            deskAnomalyInteractions = updatedCounts,
          )
        }
      }
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = nextDesk), catalog)
  }

  fun connectNextChapter(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    if (state.activeArc != GameArc.ARC_2_INVESTIGATION) return state
    val desk = state.arc2
    val chapter = desk.nextChapter ?: return state
    if (!desk.canConnectNextChapter) return state

    val isFileZero = chapter == InvestigationChapter.FILE_ZERO_SUBJECT_INTAKE
    val nextDesk =
      desk.copy(
        leads = desk.leads - chapter.leadsCost.toDouble(),
        silence = desk.silence - chapter.silenceCost.toDouble(),
        completedChapters = chapter.number,
        complicity = if (isFileZero) 1.0f else min(0.95f, desk.complicity + 0.22f),
        fileZeroRevealed = desk.fileZeroRevealed || isFileZero,
      )
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = nextDesk), catalog)
  }

  fun enterArc3Convergence(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    if (!state.arc2.fileZeroRevealed) return state
    val updated = state.copy(activeArc = GameArc.ARC_3_CONVERGENCE)
    return OrganGameEngine.finalizeExternalTransition(updated, catalog)
  }

  fun spawnImmediateAnomaly(desk: Arc2DeskState): Arc2DeskState {
    if (desk.activeDeskAnomalies.size >= Arc2Balance.MAX_DESK_ANOMALIES) return desk
    val eligible = DeskAnomalyType.entries.filter { it.minChapter <= desk.completedChapters }
    val activeTypes = desk.activeDeskAnomalies.map { it.type }.toSet()
    val pool = eligible.filterNot { it in activeTypes }.ifEmpty { eligible }
    val id = desk.nextAnomalyId
    val chosen = pool[((id - 1L) % pool.size).toInt()]
    val used = desk.activeDeskAnomalies.map { it.xFraction to it.yFraction }.toSet()
    val free = deskAnchors.filterNot { it in used }.ifEmpty { deskAnchors }
    val (x, y) = free[((id - 1L) % free.size).toInt()]
    return desk.copy(
      activeDeskAnomalies = desk.activeDeskAnomalies + ActiveDeskAnomaly(id, chosen, x, y),
      nextAnomalyId = id + 1L,
      secondsUntilNextAnomaly = Arc2Balance.DESK_ANOMALY_INTERVAL_SEC,
    )
  }

  private fun advanceDeskAnomalySpawner(desk: Arc2DeskState, deltaSeconds: Double): Arc2DeskState {
    if (desk.activeDeskAnomalies.size >= Arc2Balance.MAX_DESK_ANOMALIES) return desk
    val remaining = desk.secondsUntilNextAnomaly - deltaSeconds
    if (remaining > 0.0) return desk.copy(secondsUntilNextAnomaly = remaining)
    return spawnImmediateAnomaly(desk)
  }
}
