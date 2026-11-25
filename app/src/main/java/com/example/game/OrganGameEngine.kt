package com.example.game

import com.example.data.DialogueCatalog
import com.example.data.EscalationCurve
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Pure, deterministic state engine for Arc 1 ("The Tending of the Unnamed Organ"),
 * delegating Arc 2 desk actions cleanly to [Arc2InvestigatorEngine].
 */
object OrganGameEngine {

  private val FOUNDATIONAL_RETAINED_JARS: Set<SpecimenJar> =
    setOf(
      SpecimenJar.YESTERDAYS_DUST,
      SpecimenJar.UNSPENT_BREATH,
      SpecimenJar.ALMOST_A_NAME,
    )

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
      activeArc = GameArc.ARC_1_CHAMBER,
      phase = 0,
      phaseName = catalog.phaseName(0),
      nextPhaseThreshold = EscalationCurve.nextThresholdForPhase(0),
      phaseProgress = 0f,
    )

  fun pressValve(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    if (state.activeArc != GameArc.ARC_1_CHAMBER || state.isValveClamped) return state
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
    if (state.activeArc == GameArc.ARC_2_INVESTIGATION) {
      return Arc2InvestigatorEngine.tick(state, deltaSeconds, catalog)
    }
    if (state.activeArc == GameArc.ARC_3_CONVERGENCE) return state

    val nextIdleSec = state.secondsSinceLastTap + deltaSeconds
    val hasWeepingLamp = state.creatureCount(CreatureType.WEEPING_LAMP) > 0
    val stillnessNowActive = hasWeepingLamp && nextIdleSec >= state.stillnessIdleDelaySec

    val preProductionState =
      if (stillnessNowActive == state.isStillnessActive) state
      else state.copy(isStillnessActive = stillnessNowActive)
    val gainedIchor = preProductionState.ichorPerSecond * deltaSeconds
    val gainedStillness = preProductionState.stillnessPerSecond * deltaSeconds
    val nextGaze = max(0.0, state.gazeRemainingSeconds - deltaSeconds)
    val nextBloom = max(0.0, state.marrowBloomRemainingSeconds - deltaSeconds)
    val nextGraftCooldown = max(0.0, state.graftCooldownRemainingSeconds - deltaSeconds)
    val nextLungCooldown = max(0.0, state.choirLungCooldownRemainingSeconds - deltaSeconds)

    // Suture Tier 5: Perpetual Seam auto-ratchets valve turns without breaking Stillness
    val autoValveActive = state.hasNode(ScarNodeId.PERPETUAL_SEAM) && !state.isValveClamped
    val autoValveCharge =
      if (autoValveActive) state.autoValveChargeSeconds + deltaSeconds
      else state.autoValveChargeSeconds
    val autoValveTurns =
      if (autoValveActive) floor(autoValveCharge / OrganBalance.PERPETUAL_SEAM_INTERVAL_SEC).toInt()
      else 0
    val nextAutoValveCharge =
      if (autoValveActive) {
        autoValveCharge - autoValveTurns * OrganBalance.PERPETUAL_SEAM_INTERVAL_SEC
      } else {
        autoValveCharge
      }
    val autoValveIchor =
      if (autoValveTurns > 0) autoValveTurns * preProductionState.manualTapYield else 0.0

    // Excision II: Drain Grate pressure buildup when sealed (amplified by Echo Ossuary)
    val nextDrainPressure =
      if (state.highestExcisionMilestone >= 2 && state.drainSealed) {
        val siphonAssist = state.creatureCount(CreatureType.SUMP_SIPHON) * 1.5
        val ossuarySpeed = if (state.hasNode(ScarNodeId.ECHO_OSSUARY)) 1.45 else 1.0
        min(100.0, state.drainPressure + (4.0 + siphonAssist) * ossuarySpeed * deltaSeconds)
      } else {
        max(0.0, state.drainPressure - 2.0 * deltaSeconds)
      }

    // Excision III: Neighbor Aperture periodic Resonance Echo charge tracking
    val echoInterval = state.echoPulseIntervalSec
    val echoActive = state.highestExcisionMilestone >= 3 && state.echoDiversionPercent > 0
    val chargedEcho = if (echoActive) state.echoChargeSeconds + deltaSeconds else state.echoChargeSeconds
    val echoPulseReady = echoActive && chargedEcho >= echoInterval

    // Excision IV: Valve Clamp countdown
    val clampWasActive = state.clampRemainingSeconds > 0.0
    val remainingClamp =
      if (clampWasActive) max(0.0, state.clampRemainingSeconds - deltaSeconds) else 0.0

    val totalCycleIchorGain = gainedIchor + autoValveIchor
    var nextState =
      state.copy(
        ichor = state.ichor + totalCycleIchorGain,
        cycleLifetimeIchor = state.cycleLifetimeIchor + totalCycleIchorGain,
        stillness = state.stillness + gainedStillness,
        cycleLifetimeStillness = state.cycleLifetimeStillness + gainedStillness,
        secondsSinceLastTap = nextIdleSec,
        isStillnessActive = stillnessNowActive,
        gazeRemainingSeconds = nextGaze,
        marrowBloomRemainingSeconds = nextBloom,
        graftCooldownRemainingSeconds = nextGraftCooldown,
        choirLungCooldownRemainingSeconds = nextLungCooldown,
        autoValveChargeSeconds = nextAutoValveCharge,
        drainPressure = nextDrainPressure,
        echoChargeSeconds = if (echoPulseReady) state.echoChargeSeconds else chargedEcho,
        clampRemainingSeconds = remainingClamp,
      )

    if (echoPulseReady) {
      val baseEchoMultiplier =
        when (nextState.echoDiversionPercent) {
          25 -> 10.5
          50 -> 18.0
          else -> 0.0
        }
      val echoYieldBoost =
        (if (nextState.hasNode(ScarNodeId.ECHO_OSSUARY)) 1.35 else 1.0) *
          (if (nextState.hasNode(ScarNodeId.BONE_RESONANCE)) 1.40 else 1.0)
      val echoBurst = max(250.0, nextState.rawIchorPerSecond * baseEchoMultiplier * echoYieldBoost)
      nextState =
        nextState.copy(
          ichor = nextState.ichor + echoBurst,
          cycleLifetimeIchor = nextState.cycleLifetimeIchor + echoBurst,
          echoChargeSeconds = chargedEcho - echoInterval,
          echoPulsesTriggered = nextState.echoPulsesTriggered + 1,
        )
    }

    if (clampWasActive && remainingClamp == 0.0) {
      nextState = triggerMarrowBloom(nextState)
    }

    // Surgical Tree (Marrow Tier 2): Automatic Tendril Suture
    if (
      nextState.hasNode(ScarNodeId.AUTO_TENDRIL_SUTURE) &&
        nextState.isGraftUnlocked &&
        nextState.creatureCount(CreatureType.SUTURE_HAND) >= 4 &&
        nextState.creatureCount(CreatureType.LEECH_CLERK) >= 4
    ) {
      nextState = performGraft(nextState, catalog)
    }

    nextState = finalizeProgression(nextState, catalog)
    nextState = advanceAnomalySpawner(nextState, deltaSeconds)
    return nextState
  }

  fun buyCreature(
    state: OrganGameState,
    type: CreatureType,
    catalog: DialogueCatalog,
  ): OrganGameState {
    if (!state.isCreatureUnlocked(type)) return state
    val cost = state.creatureCost(type)
    if (state.ichor < cost.toDouble()) return state

    val updatedCreatures = state.creatures.toMutableMap()
    updatedCreatures[type] = (updatedCreatures[type] ?: 0) + 1

    val updated = state.copy(ichor = state.ichor - cost.toDouble(), creatures = updatedCreatures)
    return finalizeProgression(updated, catalog)
  }

  fun buyMaxCreature(
    state: OrganGameState,
    type: CreatureType,
    catalog: DialogueCatalog,
  ): OrganGameState {
    if (!state.isColonyProliferationUnlocked || !state.isCreatureUnlocked(type)) return state
    val quote = state.maxAffordableCreatureQuote(type)
    if (quote.count <= 0) return state

    val updatedCreatures = state.creatures.toMutableMap()
    updatedCreatures[type] = (updatedCreatures[type] ?: 0) + quote.count

    val updated =
      state.copy(
        ichor = max(0.0, state.ichor - quote.totalCost.toDouble()),
        creatures = updatedCreatures,
      )
    return finalizeProgression(updated, catalog)
  }

  fun bottleJar(
    state: OrganGameState,
    jar: SpecimenJar,
    catalog: DialogueCatalog,
  ): OrganGameState {
    if (!state.isJarUnlocked(jar) || jar in state.ownedJars) return state
    val cost = state.effectiveJarCost(jar)
    if (state.stillness < cost) return state

    val updated =
      state.copy(stillness = state.stillness - cost, ownedJars = state.ownedJars + jar)
    return finalizeProgression(updated, catalog)
  }

  fun performGraft(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    if (!state.canAffordGraft) return state

    val stillnessCost = state.graftStillnessCost.toDouble()
    val updatedCreatures = state.creatures.toMutableMap()
    updatedCreatures[CreatureType.SUTURE_HAND] =
      state.creatureCount(CreatureType.SUTURE_HAND) - OrganBalance.GRAFT_SUTURE_COST
    updatedCreatures[CreatureType.LEECH_CLERK] =
      state.creatureCount(CreatureType.LEECH_CLERK) - OrganBalance.GRAFT_LEECH_COST

    val updated =
      state.copy(
        stillness = max(0.0, state.stillness - stillnessCost),
        creatures = updatedCreatures,
        graftedTendrils = state.graftedTendrils + 1,
        graftCooldownRemainingSeconds = state.graftCooldownDurationSec,
        lastSacrificePulse = state.lastSacrificePulse + 1L,
        lastSacrificeKind = GraftSacrificeKind.TENDRIL,
      )
    return finalizeProgression(updated, catalog)
  }

  fun performChoirLungGraft(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    if (!state.canAffordChoirLung) return state
    val stillnessCost = state.choirLungStillnessCost.toDouble()
    val updatedCreatures = state.creatures.toMutableMap()
    updatedCreatures[CreatureType.WEEPING_LAMP] =
      state.creatureCount(CreatureType.WEEPING_LAMP) - OrganBalance.CHOIR_LUNG_LAMP_COST
    val updated =
      state.copy(
        stillness = max(0.0, state.stillness - stillnessCost),
        creatures = updatedCreatures,
        graftedTendrils = state.graftedTendrils - OrganBalance.CHOIR_LUNG_TENDRIL_COST,
        choirLungs = state.choirLungs + 1,
        choirLungCooldownRemainingSeconds = state.choirLungCooldownDurationSec,
        lastSacrificePulse = state.lastSacrificePulse + 1L,
        lastSacrificeKind = GraftSacrificeKind.CHOIR_LUNG,
      )
    return finalizeProgression(updated, catalog)
  }

  fun unlockScarNode(
    state: OrganGameState,
    node: ScarNodeId,
    catalog: DialogueCatalog,
  ): OrganGameState {
    if (!state.canUnlockNode(node)) return state
    val updated =
      state.copy(
        availableScarTissue = state.availableScarTissue - node.cost,
        unlockedScarNodes = state.unlockedScarNodes + node,
      )
    return finalizeProgression(updated, catalog)
  }

  fun buyRepeatableScarGraft(
    state: OrganGameState,
    graft: RepeatableScarGraft,
    catalog: DialogueCatalog,
  ): OrganGameState {
    if (!state.canBuyRepeatableGraft(graft)) return state
    val cost = state.repeatableCost(graft)
    val updatedRanks = state.repeatableScarRanks.toMutableMap()
    updatedRanks[graft] = state.repeatableRank(graft) + 1
    val updated =
      state.copy(
        availableScarTissue = state.availableScarTissue - cost,
        repeatableScarRanks = updatedRanks,
      )
    return finalizeProgression(updated, catalog)
  }

  fun toggleDrainSeal(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    if (state.highestExcisionMilestone < 2) return state
    val updated = state.copy(drainSealed = !state.drainSealed)
    return finalizeProgression(updated, catalog)
  }

  fun dredgeSump(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    val nextRelic = state.nextDredgeableRelic ?: return state
    if (!state.canDredgeSump) return state
    val burstIchor = max(25_000.0, state.rawIchorPerSecond * 15.0)
    val updated =
      state.copy(
        ichor = state.ichor + burstIchor,
        cycleLifetimeIchor = state.cycleLifetimeIchor + burstIchor,
        drainPressure = 0.0,
        drainSealed = false,
        dredgedRelics = state.dredgedRelics + nextRelic,
        lifetimeRelicsDredged = state.lifetimeRelicsDredged + 1,
      )
    return finalizeProgression(updated, catalog)
  }

  fun setEchoDiversion(
    state: OrganGameState,
    percent: Int,
    catalog: DialogueCatalog,
  ): OrganGameState {
    if (state.highestExcisionMilestone < 3) return state
    val clamped =
      when (percent) {
        0, 25, 50 -> percent
        else -> 0
      }
    val updated = state.copy(echoDiversionPercent = clamped)
    return finalizeProgression(updated, catalog)
  }

  fun activateValveClamp(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    if (state.highestExcisionMilestone < 4 || state.isValveClamped) return state
    val updated = state.copy(clampRemainingSeconds = state.valveClampDurationSec)
    return finalizeProgression(updated, catalog)
  }

  fun offerToThroat(
    state: OrganGameState,
    line: CultDossierLine,
    catalog: DialogueCatalog,
  ): OrganGameState {
    if (!state.canOfferToThroat(line)) return state
    val updatedCreatures = state.creatures.toMutableMap()
    updatedCreatures[CreatureType.SUMP_SIPHON] =
      state.creatureCount(CreatureType.SUMP_SIPHON) - line.siphonCost
    val nextJars =
      if (line.requiresJar && !state.hasNode(ScarNodeId.WAX_SEALED_MEMORY)) {
        state.ownedJars - state.ownedJars.first()
      } else {
        state.ownedJars
      }
    val updated =
      state.copy(
        creatures = updatedCreatures,
        graftedTendrils = state.graftedTendrils - line.tendrilCost,
        ownedJars = nextJars,
        unredactedDossierLines = state.unredactedDossierLines + line,
      )
    return finalizeProgression(updated, catalog)
  }

  fun interactAnomaly(
    state: OrganGameState,
    anomalyId: Long,
    catalog: DialogueCatalog,
  ): OrganGameState {
    val anomaly = state.activeAnomalies.firstOrNull { it.id == anomalyId } ?: return state
    val remaining = state.activeAnomalies.filterNot { it.id == anomalyId }
    val rewardBoost = state.anomalyRewardMultiplier

    val updatedCounts = state.anomalyInteractions.toMutableMap()
    updatedCounts[anomaly.type] = (updatedCounts[anomaly.type] ?: 0) + 1

    val rewarded =
      when (anomaly.type) {
        WallAnomalyType.PALE_TOOTH -> {
          val bonus =
            OrganBalance.TOOTH_BASE_ICHOR_REWARD *
              state.scarMultiplier *
              state.relicIchorMultiplier *
              state.dossierIchorMultiplier *
              rewardBoost
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
          val ichorBonus =
            OrganBalance.NODULE_BASE_ICHOR_REWARD *
              state.scarMultiplier *
              state.relicIchorMultiplier *
              state.dossierIchorMultiplier *
              rewardBoost
          val stillnessBonus =
            OrganBalance.NODULE_BASE_STILLNESS_REWARD *
              state.scarStillnessMultiplier *
              state.relicStillnessMultiplier *
              rewardBoost
          state.copy(
            ichor = state.ichor + ichorBonus,
            cycleLifetimeIchor = state.cycleLifetimeIchor + ichorBonus,
            stillness = state.stillness + stillnessBonus,
            cycleLifetimeStillness = state.cycleLifetimeStillness + stillnessBonus,
            activeAnomalies = remaining,
            anomalyInteractions = updatedCounts,
          )
        }
        WallAnomalyType.CHITIN_RIB -> {
          val ichorBonus =
            OrganBalance.RIB_BASE_ICHOR_REWARD *
              state.scarMultiplier *
              state.relicIchorMultiplier *
              state.dossierIchorMultiplier *
              rewardBoost
          val stillnessBonus =
            OrganBalance.RIB_BASE_STILLNESS_REWARD *
              state.scarStillnessMultiplier *
              state.relicStillnessMultiplier *
              rewardBoost
          state.copy(
            ichor = state.ichor + ichorBonus,
            cycleLifetimeIchor = state.cycleLifetimeIchor + ichorBonus,
            stillness = state.stillness + stillnessBonus,
            cycleLifetimeStillness = state.cycleLifetimeStillness + stillnessBonus,
            gazeRemainingSeconds = max(state.gazeRemainingSeconds, OrganBalance.RIB_GAZE_DURATION_SEC),
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
    // Strict anti-skip sequential lock: advance by at most +1 Excision Milestone per Excision
    val nextMilestone = ExcisionMilestone.nextAfter(state.highestExcisionMilestone)
    val newHighestMilestone =
      if (nextMilestone != null && state.cycleLifetimeIchorDisplay >= nextMilestone.massThreshold) {
        nextMilestone.number
      } else {
        state.highestExcisionMilestone
      }

    val startingCreatures = buildMap {
      if (state.hasNode(ScarNodeId.CALLOUSED_PALM)) {
        put(CreatureType.SUTURE_HAND, 3)
        put(CreatureType.LEECH_CLERK, 1)
      }
      if (state.hasNode(ScarNodeId.MEMBRANE_RECALL)) {
        put(CreatureType.WEEPING_LAMP, 2)
        if (newHighestMilestone >= 2) {
          put(CreatureType.SUMP_SIPHON, 2)
        }
      }
    }
    val startingTendrils = if (state.hasNode(ScarNodeId.MEMBRANE_RECALL)) 1 else 0
    val retainedRelics =
      if (state.hasNode(ScarNodeId.HOLLOW_LUNG_CHOIR)) state.dredgedRelics else emptySet()
    val retainedJars =
      if (state.hasNode(ScarNodeId.CATATONIC_TIDE)) {
        state.ownedJars.filterTo(mutableSetOf()) { it in FOUNDATIONAL_RETAINED_JARS }
      } else {
        emptySet()
      }

    val resetState =
      OrganGameState(
        activeArc = GameArc.ARC_1_CHAMBER,
        ichor = 0.0,
        cycleLifetimeIchor = 0.0,
        stillness = 0.0,
        cycleLifetimeStillness = 0.0,
        scarTissue = state.scarTissue + gainedScars,
        availableScarTissue = state.availableScarTissue + gainedScars,
        highestExcisionMilestone = newHighestMilestone,
        unlockedScarNodes = state.unlockedScarNodes,
        repeatableScarRanks = state.repeatableScarRanks,
        cycle = state.cycle + 1,
        phase = 0,
        phaseName = catalog.phaseName(0),
        nextPhaseThreshold = EscalationCurve.nextThresholdForPhase(0),
        phaseProgress = 0f,
        creatures = startingCreatures,
        graftedTendrils = startingTendrils,
        choirLungs = 0,
        graftCooldownRemainingSeconds = 0.0,
        choirLungCooldownRemainingSeconds = 0.0,
        autoValveChargeSeconds = 0.0,
        ownedJars = retainedJars,
        activeAnomalies = emptyList(),
        anomalyInteractions = emptyMap(),
        secondsSinceLastTap = 0.0,
        isStillnessActive = false,
        gazeRemainingSeconds = 0.0,
        secondsUntilNextAnomaly = OrganBalance.FIRST_ANOMALY_DELAY_SEC,
        nextAnomalyId = state.nextAnomalyId,
        drainSealed = false,
        drainPressure = 0.0,
        dredgedRelics = retainedRelics,
        lifetimeRelicsDredged = state.lifetimeRelicsDredged,
        echoDiversionPercent = 0,
        echoChargeSeconds = 0.0,
        echoPulsesTriggered = state.echoPulsesTriggered,
        clampRemainingSeconds = 0.0,
        marrowBloomRemainingSeconds = 0.0,
        marrowBloomCount = state.marrowBloomCount,
        unredactedDossierLines = state.unredactedDossierLines,
        arc2 = state.arc2,
        activeDialogue = state.activeDialogue,
        discoveredDialogues = state.discoveredDialogues,
        triggeredDialogueIds = state.triggeredDialogueIds,
      )

    return finalizeProgression(resetState, catalog)
  }

  /** Excision VI climax: cuts through the mortar and launches Arc 2 ("The Inquiry"). */
  fun performBreachToArc2(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    if (!state.canBreachToArc2) return state
    val breached =
      state.copy(
        activeArc = GameArc.ARC_2_INVESTIGATION,
        highestExcisionMilestone = max(state.highestExcisionMilestone, 6),
      )
    return finalizeProgression(breached, catalog)
  }

  fun spawnImmediateWallAnomaly(state: OrganGameState, allowOverflow: Boolean = false): OrganGameState {
    val maxAnomalies =
      when {
        allowOverflow -> 5
        state.hasNode(ScarNodeId.SOFTENED_MORTAR) -> 4
        else -> OrganBalance.MAX_ACTIVE_ANOMALIES
      }
    if (state.activeAnomalies.size >= maxAnomalies) return state
    val effectivePhase = max(1, state.phase)
    val eligibleTypes = WallAnomalyType.entries.filter { it.minPhase <= effectivePhase }
    if (eligibleTypes.isEmpty()) return state

    val activeTypes = state.activeAnomalies.map { it.type }.toSet()
    val preferredPool = eligibleTypes.filterNot { it in activeTypes }.ifEmpty { eligibleTypes }
    val id = state.nextAnomalyId
    val chosenType = preferredPool[((id - 1L) % preferredPool.size).toInt()]

    val usedAnchors = state.activeAnomalies.map { it.xFraction to it.yFraction }.toSet()
    val freeAnchors = anomalyAnchors.filterNot { it in usedAnchors }.ifEmpty { anomalyAnchors }
    val (x, y) = freeAnchors[((id - 1L) % freeAnchors.size).toInt()]

    val spawnInterval =
      if (state.hasNode(ScarNodeId.SOFTENED_MORTAR)) {
        OrganBalance.ANOMALY_SPAWN_INTERVAL_SEC / OrganBalance.SOFTENED_MORTAR_SPEED_MULT
      } else {
        OrganBalance.ANOMALY_SPAWN_INTERVAL_SEC
      }

    return state.copy(
      activeAnomalies = state.activeAnomalies + ActiveWallAnomaly(id, chosenType, x, y),
      nextAnomalyId = id + 1L,
      secondsUntilNextAnomaly = spawnInterval,
    )
  }

  internal fun finalizeExternalTransition(
    state: OrganGameState,
    catalog: DialogueCatalog,
  ): OrganGameState = finalizeProgression(state, catalog)

  private fun triggerMarrowBloom(state: OrganGameState): OrganGameState {
    var bloomed =
      state.copy(
        marrowBloomRemainingSeconds = state.marrowBloomDurationSec,
        marrowBloomCount = state.marrowBloomCount + 1,
      )
    repeat(4) { bloomed = spawnImmediateWallAnomaly(bloomed, allowOverflow = true) }
    return bloomed
  }

  private fun advanceAnomalySpawner(state: OrganGameState, deltaSeconds: Double): OrganGameState {
    val maxAnomalies =
      if (state.hasNode(ScarNodeId.SOFTENED_MORTAR)) 4 else OrganBalance.MAX_ACTIVE_ANOMALIES
    if (state.phase < 1 || state.activeAnomalies.size >= maxAnomalies) {
      return state
    }

    val remaining = state.secondsUntilNextAnomaly - deltaSeconds
    if (remaining > 0.0) {
      return state.copy(secondsUntilNextAnomaly = remaining)
    }

    return spawnImmediateWallAnomaly(state)
  }

  private fun finalizeProgression(
    state: OrganGameState,
    catalog: DialogueCatalog,
  ): OrganGameState {
    val lifetimeDisplay = state.cycleLifetimeIchorDisplay
    val nextPhase = EscalationCurve.phaseForCount(lifetimeDisplay)
    val goalThreshold =
      state.nextExcisionMilestone?.massThreshold ?: ExcisionMilestone.FINAL_SEVERANCE.massThreshold
    val nextPhaseName = catalog.phaseName(nextPhase)
    val nextThreshold = EscalationCurve.nextThresholdForPhase(nextPhase)
    val nextProgress =
      EscalationCurve.progressWithinPhase(
        count = lifetimeDisplay,
        finalGoalThreshold = goalThreshold,
      )

    val base =
      if (
        state.phase == nextPhase &&
          state.phaseName == nextPhaseName &&
          state.nextPhaseThreshold == nextThreshold &&
          state.phaseProgress == nextProgress
      ) {
        state
      } else {
        state.copy(
          phase = nextPhase,
          phaseName = nextPhaseName,
          nextPhaseThreshold = nextThreshold,
          phaseProgress = nextProgress,
        )
      }

    val newlyTriggered = DialogueTriggerEvaluator.collectNewlyTriggered(base, catalog)
    if (newlyTriggered.isEmpty()) return base

    val latest = newlyTriggered.last()
    return base.copy(
      activeDialogue = latest,
      discoveredDialogues = base.discoveredDialogues + newlyTriggered,
      triggeredDialogueIds = base.triggeredDialogueIds + newlyTriggered.map { it.id },
    )
  }
}
