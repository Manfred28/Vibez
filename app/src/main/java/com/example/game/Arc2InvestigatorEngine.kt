package com.example.game

import com.example.data.DialogueCatalog
import kotlin.math.max
import kotlin.random.Random

/**
 * Pure simulation engine for Arc 2 ("The Inquiry").
 * Implements Lamp On/Off dual-lens investigation, Red-String Corkboard wiring,
 * 5 escalating Case Angles with soft-reset pivots, Acoustic Reel Scrubbing,
 * 4 Case IV Audit Redactions, and strictly Slow-Boil-gated Desk Anomalies.
 */
object Arc2InvestigatorEngine {

  fun toggleLamp(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    if (state.activeArc != GameArc.ARC_2_INVESTIGATION) return state
    val desk = state.arc2
    val nextLampOn = !desk.lampOn
    val updatedDesk =
      desk.copy(
        lampOn = nextLampOn,
        lampToggleCount = desk.lampToggleCount + 1,
        lampOffCount = if (!nextLampOn) desk.lampOffCount + 1 else desk.lampOffCount,
        // Switching the lamp back on cuts an active After-Hours acoustic reel scrub short
        reelScrubRemainingSeconds = if (nextLampOn) 0.0 else desk.reelScrubRemainingSeconds,
      )
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = updatedDesk), catalog)
  }

  fun inspectCaseFile(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    if (state.activeArc != GameArc.ARC_2_INVESTIGATION) return state
    val desk = state.arc2
    val gain = desk.inspectYield
    val updatedDesk =
      desk.copy(
        leads = desk.leads + gain,
        lifetimeLeads = desk.lifetimeLeads + gain,
      )
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = updatedDesk), catalog)
  }

  fun recruitContact(
    state: OrganGameState,
    contact: InvestigatorContact,
    catalog: DialogueCatalog,
  ): OrganGameState {
    if (state.activeArc != GameArc.ARC_2_INVESTIGATION) return state
    val desk = state.arc2
    if (!desk.isContactUnlocked(contact)) return state
    val cost = desk.contactCost(contact)
    if (desk.leads < cost.toDouble()) return state

    val nextContacts = desk.contacts.toMutableMap()
    nextContacts[contact] = desk.contactCount(contact) + 1
    val updatedDesk =
      desk.copy(
        leads = (desk.leads - cost.toDouble()).coerceAtLeast(0.0),
        contacts = nextContacts,
      )
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = updatedDesk), catalog)
  }

  fun recruitMaxContact(
    state: OrganGameState,
    contact: InvestigatorContact,
    catalog: DialogueCatalog,
  ): OrganGameState {
    if (state.activeArc != GameArc.ARC_2_INVESTIGATION) return state
    val desk = state.arc2
    if (!desk.isBulkWarrantUnlocked || !desk.isContactUnlocked(contact)) return state
    val (affordableCount, totalCost) = desk.maxAffordableContacts(contact)
    if (affordableCount <= 0) return state

    val nextContacts = desk.contacts.toMutableMap()
    nextContacts[contact] = desk.contactCount(contact) + affordableCount
    val updatedDesk =
      desk.copy(
        leads = (desk.leads - totalCost.toDouble()).coerceAtLeast(0.0),
        contacts = nextContacts,
      )
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = updatedDesk), catalog)
  }

  fun unlockExhibit(
    state: OrganGameState,
    exhibit: CaseExhibit,
    catalog: DialogueCatalog,
  ): OrganGameState {
    if (state.activeArc != GameArc.ARC_2_INVESTIGATION) return state
    val desk = state.arc2
    if (!desk.canUnlockExhibit(exhibit)) return state
    val updatedDesk =
      desk.copy(
        silence = (desk.silence - exhibit.silenceCost).coerceAtLeast(0.0),
        pinnedExhibits = desk.pinnedExhibits + exhibit,
      )
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = updatedDesk), catalog)
  }

  fun toggleExhibitPair(
    state: OrganGameState,
    a: CaseExhibit,
    b: CaseExhibit,
    catalog: DialogueCatalog,
  ): OrganGameState {
    if (state.activeArc != GameArc.ARC_2_INVESTIGATION) return state
    if (a == b) return state
    val desk = state.arc2
    if (a !in desk.pinnedExhibits || b !in desk.pinnedExhibits) return state

    val pair = ExhibitPair.canonical(a, b)
    val updatedDesk =
      if (pair in desk.wiredPairs) {
        desk.copy(wiredPairs = desk.wiredPairs - pair)
      } else {
        if (desk.wiredPairs.size >= desk.maxStringSpools) return state
        val matched = CorroborationId.matchPair(pair)
        desk.copy(
          wiredPairs = desk.wiredPairs + pair,
          discoveredCorroborations =
            if (matched != null) desk.discoveredCorroborations + matched
            else desk.discoveredCorroborations,
        )
      }
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = updatedDesk), catalog)
  }

  fun triggerReelScrub(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    if (state.activeArc != GameArc.ARC_2_INVESTIGATION) return state
    val desk = state.arc2
    if (!desk.canTriggerReelScrub) return state
    val updatedDesk =
      desk.copy(
        reelScrubRemainingSeconds = Arc2Balance.REEL_SCRUB_DURATION_SEC,
        reelScrubCooldownSeconds = 0.0,
        reelScrubCount = desk.reelScrubCount + 1,
      )
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = updatedDesk), catalog)
  }

  fun dissolveRedaction(
    state: OrganGameState,
    redaction: AuditRedaction,
    catalog: DialogueCatalog,
  ): OrganGameState {
    if (state.activeArc != GameArc.ARC_2_INVESTIGATION) return state
    val desk = state.arc2
    if (!desk.canDissolveRedaction(redaction)) return state
    val updatedDesk =
      desk.copy(
        silence = (desk.silence - redaction.silenceCost).coerceAtLeast(0.0),
        dissolvedRedactions = desk.dissolvedRedactions + redaction,
      )
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = updatedDesk), catalog)
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
    val interactions = desk.deskAnomalyInteractions.toMutableMap()
    interactions[anomaly.type] = (interactions[anomaly.type] ?: 0) + 1

    val potency = desk.anomalyPotencyMultiplier
    val leadsAnchor = max(12.0 * (1 + desk.completedCasePivots), desk.leadsPerSecond * 14.0 + desk.inspectYield * 10.0)
    val silenceAnchor = max(5.0 * (1 + desk.completedCasePivots), desk.silencePerSecond * 9.0 + 4.0)

    var leadsGain = 0.0
    var silenceGain = 0.0
    var nextFocus = desk.focusRemainingSeconds

    when (anomaly.type) {
      DeskAnomalyType.EVIDENCE_ENVELOPE -> {
        leadsGain = leadsAnchor * 1.15 * potency
      }
      DeskAnomalyType.FOGGED_SPECTACLES -> {
        nextFocus = max(desk.focusRemainingSeconds, Arc2Balance.FOCUS_DURATION_SEC)
        silenceGain = silenceAnchor * 0.65 * potency
      }
      DeskAnomalyType.REEL_TAPE -> {
        val scrubAmp = if (desk.isReelScrubbing) 1.5 else 1.0
        leadsGain = leadsAnchor * 0.85 * potency * scrubAmp
        silenceGain = silenceAnchor * 1.35 * potency * scrubAmp
      }
      DeskAnomalyType.ENAMEL_EXHIBIT -> {
        leadsGain = leadsAnchor * 1.85 * potency
        silenceGain = silenceAnchor * 1.65 * potency
      }
    }

    val updatedDesk =
      desk.copy(
        leads = desk.leads + leadsGain,
        lifetimeLeads = desk.lifetimeLeads + leadsGain,
        silence = desk.silence + silenceGain,
        lifetimeSilence = desk.lifetimeSilence + silenceGain,
        activeDeskAnomalies = remaining,
        deskAnomalyInteractions = interactions,
        focusRemainingSeconds = nextFocus,
      )
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = updatedDesk), catalog)
  }

  /**
   * Soft-reset Case Angle Pivot ("File Dead End & Reopen Case"):
   * Consumes the Case Angle's Silence gate, wipes transient Leads & Municipal Contacts to zero,
   * permanently pins the Case's Keystone Exhibit to the Corkboard, adds +1 Red String Spool,
   * and opens the next Case Angle (or unseals File #00 on Case V completion).
   */
  fun pivotNextCase(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    if (state.activeArc != GameArc.ARC_2_INVESTIGATION) return state
    val desk = state.arc2
    if (!desk.canPivotNextCase) return state

    val current = desk.currentCase
    val nextPivots = (desk.completedCasePivots + 1).coerceAtMost(5)
    val isFileZero = nextPivots >= 5
    val nextAngle = if (isFileZero) 5 else (current.number + 1).coerceAtMost(5)
    val nextPinned =
      if (current.keystoneReward != null) desk.pinnedExhibits + current.keystoneReward
      else desk.pinnedExhibits

    val updatedDesk =
      if (isFileZero) {
        desk.copy(
          leads = (desk.leads - current.leadsCost.toDouble()).coerceAtLeast(0.0),
          silence = (desk.silence - current.silenceCost.toDouble()).coerceAtLeast(0.0),
          completedCasePivots = 5,
          caseAngle = 5,
          fileZeroRevealed = true,
        )
      } else {
        desk.copy(
          leads = 0.0,
          silence = (desk.silence - current.silenceCost.toDouble()).coerceAtLeast(0.0),
          contacts = emptyMap(),
          caseAngle = nextAngle,
          completedCasePivots = nextPivots,
          pinnedExhibits = nextPinned,
          activeDeskAnomalies = emptyList(),
          focusRemainingSeconds = 0.0,
          reelScrubRemainingSeconds = 0.0,
          secondsUntilNextAnomaly = Arc2Balance.FIRST_DESK_ANOMALY_DELAY_SEC,
        )
      }
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = updatedDesk), catalog)
  }

  /** Backward-compatible alias for [pivotNextCase]. */
  fun connectNextChapter(state: OrganGameState, catalog: DialogueCatalog): OrganGameState =
    pivotNextCase(state, catalog)

  fun enterArc3Convergence(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    if (!state.arc2.fileZeroRevealed) return state
    val updated = state.copy(activeArc = GameArc.ARC_3_CONVERGENCE)
    return OrganGameEngine.finalizeExternalTransition(updated, catalog)
  }

  fun tick(
    state: OrganGameState,
    deltaSeconds: Double,
    catalog: DialogueCatalog,
    random: Random = Random.Default,
  ): OrganGameState {
    if (deltaSeconds <= 0.0 || state.activeArc != GameArc.ARC_2_INVESTIGATION) return state
    val desk = state.arc2

    val nextFocus = (desk.focusRemainingSeconds - deltaSeconds).coerceAtLeast(0.0)

    // Handle active Acoustic Reel Scrubbing countdown & transition into cooldown
    var nextScrubRem = desk.reelScrubRemainingSeconds
    var nextScrubCd = desk.reelScrubCooldownSeconds
    if (nextScrubRem > 0.0) {
      if (desk.lampOn) {
        nextScrubRem = 0.0
        nextScrubCd = Arc2Balance.REEL_SCRUB_COOLDOWN_SEC
      } else {
        val after = nextScrubRem - deltaSeconds
        if (after <= 0.0) {
          nextScrubRem = 0.0
          nextScrubCd = Arc2Balance.REEL_SCRUB_COOLDOWN_SEC
        } else {
          nextScrubRem = after
        }
      }
    } else if (nextScrubCd > 0.0) {
      nextScrubCd = (nextScrubCd - deltaSeconds).coerceAtLeast(0.0)
    }

    val leadsGain = desk.leadsPerSecond * deltaSeconds
    val silenceGain = desk.silencePerSecond * deltaSeconds
    val nextLampOffSec =
      if (!desk.lampOn) desk.totalLampOffSeconds + deltaSeconds else desk.totalLampOffSeconds

    val survived =
      desk.activeDeskAnomalies
        .map { it.copy(remainingSeconds = it.remainingSeconds - deltaSeconds) }
        .filter { it.remainingSeconds > 0.0 }

    val spawnRateFactor = if (desk.isReelScrubbing && !desk.lampOn) 1.65 else 1.0
    var timer = desk.secondsUntilNextAnomaly - deltaSeconds * spawnRateFactor
    var nextId = desk.nextAnomalyId
    val finalAnomalies = survived.toMutableList()
    if (timer <= 0.0 && finalAnomalies.size < Arc2Balance.MAX_ACTIVE_DESK_ANOMALIES) {
      finalAnomalies.add(spawnDeskAnomaly(desk, nextId, random))
      nextId += 1L
      timer =
        if (desk.lampOn) Arc2Balance.LAMP_ON_ANOMALY_INTERVAL_SEC
        else Arc2Balance.LAMP_OFF_ANOMALY_INTERVAL_SEC
    }

    val updatedDesk =
      desk.copy(
        leads = desk.leads + leadsGain,
        lifetimeLeads = desk.lifetimeLeads + leadsGain,
        silence = desk.silence + silenceGain,
        lifetimeSilence = desk.lifetimeSilence + silenceGain,
        totalLampOffSeconds = nextLampOffSec,
        focusRemainingSeconds = nextFocus,
        reelScrubRemainingSeconds = nextScrubRem,
        reelScrubCooldownSeconds = nextScrubCd,
        activeDeskAnomalies = finalAnomalies,
        secondsUntilNextAnomaly = timer.coerceAtLeast(0.0),
        nextAnomalyId = nextId,
      )
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = updatedDesk), catalog)
  }

  fun spawnImmediateDeskAnomaly(
    desk: Arc2DeskState,
    random: Random = Random.Default,
  ): Arc2DeskState {
    val anomaly = spawnDeskAnomaly(desk, desk.nextAnomalyId, random)
    return desk.copy(
      activeDeskAnomalies = (desk.activeDeskAnomalies + anomaly).takeLast(Arc2Balance.MAX_ACTIVE_DESK_ANOMALIES),
      nextAnomalyId = desk.nextAnomalyId + 1L,
      secondsUntilNextAnomaly =
        if (desk.lampOn) Arc2Balance.LAMP_ON_ANOMALY_INTERVAL_SEC
        else Arc2Balance.LAMP_OFF_ANOMALY_INTERVAL_SEC,
    )
  }

  private fun spawnDeskAnomaly(
    desk: Arc2DeskState,
    id: Long,
    random: Random,
  ): ActiveDeskAnomaly {
    val pool = mutableListOf<DeskAnomalyType>()
    // Case I+: damp municipal arrears envelope (heavily favored with Lamp On)
    repeat(if (desk.lampOn) 4 else 1) { pool.add(DeskAnomalyType.EVIDENCE_ENVELOPE) }
    // Case II+: fogged spectacles (favored in Lamp Off After-Hours)
    if (desk.caseAngle >= DeskAnomalyType.FOGGED_SPECTACLES.unlockCaseAngle) {
      repeat(if (desk.lampOn) 1 else 3) { pool.add(DeskAnomalyType.FOGGED_SPECTACLES) }
    }
    // Case III+: warped magnetic reel tape (strongly favored in Lamp Off / Reel Scrubbing)
    if (desk.caseAngle >= DeskAnomalyType.REEL_TAPE.unlockCaseAngle) {
      repeat(if (desk.lampOn) 1 else 3) { pool.add(DeskAnomalyType.REEL_TAPE) }
    }
    // Case IV+ ONLY: grotesque enamel molar pouch — quarantined completely out of Cases I–III!
    if (desk.caseAngle >= DeskAnomalyType.ENAMEL_EXHIBIT.unlockCaseAngle) {
      repeat(if (desk.caseAngle >= 5) 3 else 2) { pool.add(DeskAnomalyType.ENAMEL_EXHIBIT) }
    }
    val type = pool[random.nextInt(pool.size)]
    val onLeft = random.nextBoolean()
    val x = if (onLeft) 0.05f + random.nextFloat() * 0.17f else 0.74f + random.nextFloat() * 0.17f
    val y = 0.24f + random.nextFloat() * 0.56f
    return ActiveDeskAnomaly(
      id = id,
      type = type,
      xFraction = x,
      yFraction = y,
      remainingSeconds = Arc2Balance.DESK_ANOMALY_LIFETIME_SEC,
    )
  }
}
