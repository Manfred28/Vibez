package com.example.game

import com.example.data.DialogueCatalog
import kotlin.math.max

/**
 * Pure developer testing helpers allowing arbitrary milestone unlocking, resource assignment,
 * surgical tree toggling, arc switching, and anomaly spawning without mutating production rules.
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
            arc2 = state.arc2.copy(completedChapters = 4, fileZeroRevealed = true, complicity = 1f),
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
    complicity: Float? = null,
    completedChapters: Int? = null,
    catalog: DialogueCatalog,
  ): OrganGameState {
    val desk = state.arc2
    val nextLeads = leads?.coerceAtLeast(0.0) ?: desk.leads
    val nextSilence = silence?.coerceAtLeast(0.0) ?: desk.silence
    val nextChapters = completedChapters?.coerceIn(0, 4) ?: desk.completedChapters
    val nextComplicity = complicity?.coerceIn(0.05f, 1.0f) ?: desk.complicity
    val nextDesk =
      desk.copy(
        leads = nextLeads,
        lifetimeLeads = max(desk.lifetimeLeads, nextLeads),
        silence = nextSilence,
        lifetimeSilence = max(desk.lifetimeSilence, nextSilence),
        complicity = nextComplicity,
        completedChapters = nextChapters,
        fileZeroRevealed = nextChapters >= 4 || desk.fileZeroRevealed,
      )
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc2 = nextDesk), catalog)
  }

  fun spawnWallAnomaly(state: OrganGameState): OrganGameState =
    OrganGameEngine.spawnImmediateWallAnomaly(state, allowOverflow = true)

  fun spawnDeskAnomaly(state: OrganGameState): OrganGameState =
    state.copy(arc2 = Arc2InvestigatorEngine.spawnImmediateAnomaly(state.arc2))

  fun resetAll(catalog: DialogueCatalog): OrganGameState = OrganGameEngine.initialState(catalog)
}
