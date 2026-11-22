package com.example.game

import com.example.data.DialogueCatalog
import com.example.data.DialogueEntry
import com.example.data.EscalationCurve
import kotlin.math.max

/**
 * Pure, deterministic state engine for Direction 1 ("The Tending of the Unnamed Organ"). Every
 * transition returns a fresh [OrganGameState] with updated escalation phase and progress-bound
 * dialogue triggers evaluated from [DialogueCatalog].
 */
object OrganGameEngine {

  private val anomalyAnchors: List<Pair<Float, Float>> =
    listOf(
      0.16f to 0.24f,
      0.84f to 0.26f,
      0.15f to 0.74f,
      0.85f to 0.72f,
      0.22f to 0.48f,
      0.79f to 0.50f,
    )

  fun initialState(catalog: DialogueCatalog): OrganGameState =
    OrganGameState(
      phase = 0,
      phaseName = catalog.phaseName(0),
      nextPhaseThreshold = EscalationCurve.nextThresholdForPhase(0),
      phaseProgress = 0f,
    )

  fun pressValve(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    val gained = state.manualTapYield
    val updated =
      state.copy(
        ichor = state.ichor + gained,
        cycleLifetimeIchor = state.cycleLifetimeIchor + gained,
        secondsSinceLastTap = 0.0,
        isStillnessActive = false,
      )
    return finalizeProgression(updated, catalog)
  }

  fun tick(
    state: OrganGameState,
    deltaSeconds: Double,
    catalog: DialogueCatalog,
  ): OrganGameState {
    if (deltaSeconds <= 0.0) return state

    val nextIdleSec = state.secondsSinceLastTap + deltaSeconds
    val hasWeepingLamp = state.creatureCount(CreatureType.WEEPING_LAMP) > 0
    val stillnessNowActive = hasWeepingLamp && nextIdleSec >= OrganBalance.STILLNESS_IDLE_DELAY_SEC

    val preProductionState = state.copy(isStillnessActive = stillnessNowActive)
    val gainedIchor = preProductionState.ichorPerSecond * deltaSeconds
    val gainedStillness = preProductionState.stillnessPerSecond * deltaSeconds
    val nextGaze = max(0.0, state.gazeRemainingSeconds - deltaSeconds)

    var nextState =
      preProductionState.copy(
        ichor = state.ichor + gainedIchor,
        cycleLifetimeIchor = state.cycleLifetimeIchor + gainedIchor,
        stillness = state.stillness + gainedStillness,
        cycleLifetimeStillness = state.cycleLifetimeStillness + gainedStillness,
        secondsSinceLastTap = nextIdleSec,
        gazeRemainingSeconds = nextGaze,
      )

    nextState = finalizeProgression(nextState, catalog)
    nextState = advanceAnomalySpawner(nextState, deltaSeconds)
    return nextState
  }

  fun buyCreature(
    state: OrganGameState,
    type: CreatureType,
    catalog: DialogueCatalog,
  ): OrganGameState {
    val cost = state.creatureCost(type)
    if (state.ichor < cost.toDouble()) return state

    val updatedCreatures = state.creatures.toMutableMap()
    updatedCreatures[type] = (updatedCreatures[type] ?: 0) + 1

    val updated = state.copy(ichor = state.ichor - cost.toDouble(), creatures = updatedCreatures)
    return finalizeProgression(updated, catalog)
  }

  fun bottleJar(
    state: OrganGameState,
    jar: SpecimenJar,
    catalog: DialogueCatalog,
  ): OrganGameState {
    if (jar in state.ownedJars || state.stillness < jar.stillnessCost) return state

    val updated =
      state.copy(stillness = state.stillness - jar.stillnessCost, ownedJars = state.ownedJars + jar)
    return finalizeProgression(updated, catalog)
  }

  fun performGraft(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    if (!state.canAffordGraft) return state

    val updatedCreatures = state.creatures.toMutableMap()
    updatedCreatures[CreatureType.SUTURE_HAND] =
      state.creatureCount(CreatureType.SUTURE_HAND) - OrganBalance.GRAFT_SUTURE_COST
    updatedCreatures[CreatureType.LEECH_CLERK] =
      state.creatureCount(CreatureType.LEECH_CLERK) - OrganBalance.GRAFT_LEECH_COST

    val updated =
      state.copy(creatures = updatedCreatures, graftedTendrils = state.graftedTendrils + 1)
    return finalizeProgression(updated, catalog)
  }

  fun interactAnomaly(
    state: OrganGameState,
    anomalyId: Long,
    catalog: DialogueCatalog,
  ): OrganGameState {
    val anomaly = state.activeAnomalies.firstOrNull { it.id == anomalyId } ?: return state
    val remaining = state.activeAnomalies.filterNot { it.id == anomalyId }
    val jarBoost = if (SpecimenJar.ALMOST_A_NAME in state.ownedJars) 2.5 else 1.0

    val updatedCounts = state.anomalyInteractions.toMutableMap()
    updatedCounts[anomaly.type] = (updatedCounts[anomaly.type] ?: 0) + 1

    val rewarded =
      when (anomaly.type) {
        WallAnomalyType.PALE_TOOTH -> {
          val bonus = OrganBalance.TOOTH_BASE_ICHOR_REWARD * state.scarMultiplier * jarBoost
          state.copy(
            ichor = state.ichor + bonus,
            cycleLifetimeIchor = state.cycleLifetimeIchor + bonus,
            activeAnomalies = remaining,
            anomalyInteractions = updatedCounts,
          )
        }
        WallAnomalyType.GLASS_EYE -> {
          state.copy(
            gazeRemainingSeconds = OrganBalance.GAZE_DURATION_SEC,
            activeAnomalies = remaining,
            anomalyInteractions = updatedCounts,
          )
        }
        WallAnomalyType.SWOLLEN_NODULE -> {
          val ichorBonus = OrganBalance.NODULE_BASE_ICHOR_REWARD * state.scarMultiplier * jarBoost
          val stillnessBonus = OrganBalance.NODULE_BASE_STILLNESS_REWARD * jarBoost
          state.copy(
            ichor = state.ichor + ichorBonus,
            cycleLifetimeIchor = state.cycleLifetimeIchor + ichorBonus,
            stillness = state.stillness + stillnessBonus,
            cycleLifetimeStillness = state.cycleLifetimeStillness + stillnessBonus,
            activeAnomalies = remaining,
            anomalyInteractions = updatedCounts,
          )
        }
      }

    return finalizeProgression(rewarded, catalog)
  }

  fun performExcision(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    if (!state.canExcise) return state

    val gainedScars = state.pendingScarTissue
    val resetState =
      OrganGameState(
        ichor = 0.0,
        cycleLifetimeIchor = 0.0,
        stillness = 0.0,
        cycleLifetimeStillness = 0.0,
        scarTissue = state.scarTissue + gainedScars,
        cycle = state.cycle + 1,
        phase = 0,
        phaseName = catalog.phaseName(0),
        nextPhaseThreshold = EscalationCurve.nextThresholdForPhase(0),
        phaseProgress = 0f,
        creatures = emptyMap(),
        graftedTendrils = 0,
        ownedJars = emptySet(),
        activeAnomalies = emptyList(),
        anomalyInteractions = emptyMap(),
        secondsSinceLastTap = 0.0,
        isStillnessActive = false,
        gazeRemainingSeconds = 0.0,
        secondsUntilNextAnomaly = OrganBalance.FIRST_ANOMALY_DELAY_SEC,
        nextAnomalyId = state.nextAnomalyId,
        activeDialogue = state.activeDialogue,
        discoveredDialogues = state.discoveredDialogues,
        triggeredDialogueIds = state.triggeredDialogueIds,
      )

    return finalizeProgression(resetState, catalog)
  }

  private fun advanceAnomalySpawner(state: OrganGameState, deltaSeconds: Double): OrganGameState {
    if (state.phase < 1 || state.activeAnomalies.size >= OrganBalance.MAX_ACTIVE_ANOMALIES) {
      return state
    }

    val remaining = state.secondsUntilNextAnomaly - deltaSeconds
    if (remaining > 0.0) {
      return state.copy(secondsUntilNextAnomaly = remaining)
    }

    val eligibleTypes = WallAnomalyType.entries.filter { it.minPhase <= state.phase }
    if (eligibleTypes.isEmpty()) return state

    // Prefer spawning an eligible type not currently on screen so all anomaly mechanics stay visible
    val activeTypes = state.activeAnomalies.map { it.type }.toSet()
    val preferredPool = eligibleTypes.filterNot { it in activeTypes }.ifEmpty { eligibleTypes }
    val id = state.nextAnomalyId
    val chosenType = preferredPool[((id - 1L) % preferredPool.size).toInt()]

    val usedAnchors = state.activeAnomalies.map { it.xFraction to it.yFraction }.toSet()
    val freeAnchors = anomalyAnchors.filterNot { it in usedAnchors }.ifEmpty { anomalyAnchors }
    val (x, y) = freeAnchors[((id - 1L) % freeAnchors.size).toInt()]

    val spawned = ActiveWallAnomaly(id = id, type = chosenType, xFraction = x, yFraction = y)
    return state.copy(
      activeAnomalies = state.activeAnomalies + spawned,
      nextAnomalyId = id + 1L,
      secondsUntilNextAnomaly = OrganBalance.ANOMALY_SPAWN_INTERVAL_SEC,
    )
  }

  private fun finalizeProgression(
    state: OrganGameState,
    catalog: DialogueCatalog,
  ): OrganGameState {
    val lifetimeDisplay = state.cycleLifetimeIchorDisplay
    val nextPhase = EscalationCurve.phaseForCount(lifetimeDisplay)
    val base =
      state.copy(
        phase = nextPhase,
        phaseName = catalog.phaseName(nextPhase),
        nextPhaseThreshold = EscalationCurve.nextThresholdForPhase(nextPhase),
        phaseProgress = EscalationCurve.progressWithinPhase(lifetimeDisplay),
      )

    val newlyTriggered =
      catalog.entries.filter { entry ->
        (!entry.once || entry.id !in base.triggeredDialogueIds) && isTriggerSatisfied(base, entry)
      }

    if (newlyTriggered.isEmpty()) return base

    val latest = newlyTriggered.last()
    return base.copy(
      activeDialogue = latest,
      discoveredDialogues = base.discoveredDialogues + newlyTriggered,
      triggeredDialogueIds = base.triggeredDialogueIds + newlyTriggered.map { it.id },
    )
  }

  private fun isTriggerSatisfied(state: OrganGameState, entry: DialogueEntry): Boolean {
    val trigger = entry.trigger
    return when (trigger.type) {
      "counter_reached" -> state.cycleLifetimeIchorDisplay >= trigger.threshold
      "creature_owned" ->
        when (trigger.target) {
          CreatureType.SUTURE_HAND.id ->
            state.creatureCount(CreatureType.SUTURE_HAND) >= trigger.threshold
          CreatureType.LEECH_CLERK.id ->
            state.creatureCount(CreatureType.LEECH_CLERK) >= trigger.threshold
          CreatureType.WEEPING_LAMP.id ->
            state.creatureCount(CreatureType.WEEPING_LAMP) >= trigger.threshold
          "grafted_tendril" -> state.graftedTendrils >= trigger.threshold
          else -> false
        }
      "anomaly_interacted" -> {
        val anomalyType = WallAnomalyType.entries.firstOrNull { it.id == trigger.target }
        anomalyType != null && (state.anomalyInteractions[anomalyType] ?: 0) >= trigger.threshold
      }
      "stillness_reached" -> state.cycleLifetimeStillness >= trigger.threshold.toDouble()
      "jar_bottled" -> state.ownedJars.size >= trigger.threshold
      "excision_completed" -> state.cycle >= trigger.threshold
      else -> false
    }
  }
}
