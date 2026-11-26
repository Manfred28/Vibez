package com.example.game

import com.example.data.DialogueCatalog
import kotlin.math.max

/**
 * Pure developer testing helpers allowing arbitrary milestone unlocking, resource assignment,
 * surgical tree toggling, Arc 2 Case Angle jumps, Lamp toggling, Exhibit unlocking, and wipes.
 */
object OrganDevEngine {

  fun setActiveArc(
    state: OrganGameState,
    arc: GameArc,
    catalog: DialogueCatalog,
  ): OrganGameState {
    val adjusted =
      when (arc) {
        GameArc.ARC_1_CHAMBER -> state.copy(activeArc = arc)
        GameArc.ARC_2_INVESTIGATION ->
          state.copy(
            activeArc = arc,
            highestExcisionMilestone = max(state.highestExcisionMilestone, 6),
          )
        GameArc.ARC_3_CONVERGENCE ->
          state.copy(
            activeArc = arc,
            highestExcisionMilestone = max(state.highestExcisionMilestone, 6),
            arc2 =
              state.arc2.copy(
                caseAngle = 5,
                completedCasePivots = 5,
                fileZeroRevealed = true,
                pinnedExhibits = CaseExhibit.entries.toSet(),
                dissolvedRedactions = AuditRedaction.entries.toSet(),
              ),
          )
      }
    return OrganGameEngine.finalizeExternalTransition(adjusted, catalog)
  }

  fun setHighestMilestone(
    state: OrganGameState,
    milestone: Int,
    catalog: DialogueCatalog,
  ): OrganGameState {
    val clamped = milestone.coerceIn(0, 6)
    val adjusted =
      state.copy(
        highestExcisionMilestone = clamped,
        cycle = if (clamped > 0) max(state.cycle, clamped) else state.cycle,
      )
    return OrganGameEngine.finalizeExternalTransition(adjusted, catalog)
  }

  fun assignArc1Resources(
    state: OrganGameState,
    ichor: Double? = null,
    stillness: Double? = null,
    availableScar: Long? = null,
    catalog: DialogueCatalog,
  ): OrganGameState {
    val nextIchor = ichor?.coerceAtLeast(0.0) ?: state.ichor
    val nextStillness = stillness?.coerceAtLeast(0.0) ?: state.stillness
    val nextAvailScar = availableScar?.coerceAtLeast(0L) ?: state.availableScarTissue
    val nextLifetimeScar = max(state.scarTissue, nextAvailScar)

    val adjusted =
      state.copy(
        ichor = nextIchor,
        cycleLifetimeIchor = max(state.cycleLifetimeIchor, nextIchor),
        stillness = nextStillness,
        cycleLifetimeStillness = max(state.cycleLifetimeStillness, nextStillness),
        availableScarTissue = nextAvailScar,
        scarTissue = nextLifetimeScar,
      )
    return OrganGameEngine.finalizeExternalTransition(adjusted, catalog)
  }

  fun setAllScarNodes(
    state: OrganGameState,
    unlockAll: Boolean,
    catalog: DialogueCatalog,
  ): OrganGameState {
    val adjusted =
      state.copy(
        unlockedScarNodes = if (unlockAll) ScarNodeId.entries.toSet() else emptySet(),
        repeatableScarRanks = if (unlockAll) state.repeatableScarRanks else emptyMap(),
        highestExcisionMilestone =
          if (unlockAll) max(state.highestExcisionMilestone, 1)
          else state.highestExcisionMilestone,
      )
    return OrganGameEngine.finalizeExternalTransition(adjusted, catalog)
  }

  fun assignArc2Resources(
    state: OrganGameState,
    leads: Double? = null,
    silence: Double? = null,
    caseAngle: Int? = null,
    completedChapters: Int? = null,
    catalog: DialogueCatalog,
  ): OrganGameState {
    val desk = state.arc2
    val nextLeads = leads?.coerceAtLeast(0.0) ?: desk.leads
    val nextSilence = silence?.coerceAtLeast(0.0) ?: desk.silence
    val nextPivots =
      when {
        completedChapters != null -> completedChapters.coerceIn(0, 5)
        caseAngle != null -> (caseAngle.coerceIn(1, 5) - 1).coerceAtLeast(0)
        else -> desk.completedCasePivots
      }
    val nextAngle =
      when {
        caseAngle != null -> caseAngle.coerceIn(1, 5)
        completedChapters != null -> (nextPivots + 1).coerceIn(1, 5)
        else -> desk.caseAngle
      }
    val nextPinned = desk.pinnedExhibits.toMutableSet()
    CaseAngle.entries.forEach { angle ->
      if (nextPivots >= angle.number && angle.keystoneReward != null) {
        nextPinned.add(angle.keystoneReward)
      }
    }
    val nextFileZero = nextPivots >= 5 || (caseAngle == null && completedChapters == null && desk.fileZeroRevealed)
    val nextDesk =
      desk.copy(
        leads = nextLeads,
        lifetimeLeads = max(desk.lifetimeLeads, nextLeads),
        silence = nextSilence,
        lifetimeSilence = max(desk.lifetimeSilence, nextSilence),
        caseAngle = nextAngle,
        completedCasePivots = nextPivots,
        pinnedExhibits = nextPinned,
        fileZeroRevealed = nextFileZero,
      )
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = nextDesk), catalog)
  }

  fun setArc2AllExhibits(
    state: OrganGameState,
    unlockAll: Boolean,
    catalog: DialogueCatalog,
  ): OrganGameState {
    val desk = state.arc2
    val nextPinned =
      if (unlockAll) CaseExhibit.entries.toSet()
      else setOf(CaseExhibit.WATER_STAINED_PETITION)
    val nextWired = if (unlockAll) desk.wiredPairs else emptySet()
    val nextDesk = desk.copy(pinnedExhibits = nextPinned, wiredPairs = nextWired)
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = nextDesk), catalog)
  }

  fun toggleArc2Lamp(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    val desk = state.arc2
    val nextLampOn = !desk.lampOn
    val nextDesk =
      desk.copy(
        lampOn = nextLampOn,
        lampToggleCount = desk.lampToggleCount + 1,
        lampOffCount = if (!nextLampOn) desk.lampOffCount + 1 else desk.lampOffCount,
      )
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = nextDesk), catalog)
  }

  fun resetArc2(state: OrganGameState, catalog: DialogueCatalog): OrganGameState =
    OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = Arc2DeskState()), catalog)

  fun spawnWallAnomaly(state: OrganGameState): OrganGameState =
    OrganGameEngine.spawnImmediateWallAnomaly(state, allowOverflow = true)

  fun spawnDeskAnomaly(state: OrganGameState): OrganGameState =
    state.copy(arc2 = Arc2InvestigatorEngine.spawnImmediateDeskAnomaly(state.arc2))

  fun resetAll(catalog: DialogueCatalog): OrganGameState = OrganGameEngine.initialState(catalog)
}
