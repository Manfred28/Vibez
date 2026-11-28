package com.example.game

import com.example.data.DialogueCatalog
import kotlin.math.max
import kotlin.math.min

/**
 * Pure, deterministic state engine for Arc 3 ("The Unmaking"):
 * No resets — active Fracture vs Clotting between 5 irreversible Memory Wedge checkpoints,
 * 18 chamber-dismantling Unmaking acts, urgent wall ruptures, and 5 heavy interactive Final Pulls
 * dissolving into the Abstract Crimson Flood.
 */
object Arc3BreachEngine {

  private val ruptureAnchors: List<Pair<Float, Float>> =
    listOf(
      0.16f to 0.22f,
      0.84f to 0.24f,
      0.20f to 0.46f,
      0.81f to 0.44f,
      0.17f to 0.64f,
      0.83f to 0.62f,
    )

  fun wrenchBreach(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    if (state.activeArc != GameArc.ARC_3_CONVERGENCE) return state
    val breach = state.arc3
    if (breach.isInFinale || breach.submersionComplete) return state

    val gainedFracture = state.arc3WrenchFractureYield()
    val gainedHemorrhage = state.arc3WrenchHemorrhageYield()
    val nextMomentum = min(1.0, breach.momentumCharge + Arc3Balance.MOMENTUM_GAIN_PER_TAP)
    val cappedFracture = min(breach.activeStratum.fractureGoal, breach.fracture + gainedFracture)

    val updated =
      breach.copy(
        fracture = cappedFracture,
        hemorrhage = breach.hemorrhage + gainedHemorrhage,
        lifetimeHemorrhage = breach.lifetimeHemorrhage + gainedHemorrhage,
        wrenchCount = breach.wrenchCount + 1,
        momentumCharge = nextMomentum,
        secondsSinceLastAction = 0.0,
      )
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc3 = updated), catalog)
  }

  fun performUnmake(
    state: OrganGameState,
    act: Arc3UnmakeAct,
    catalog: DialogueCatalog,
  ): OrganGameState {
    if (state.activeArc != GameArc.ARC_3_CONVERGENCE) return state
    val breach = state.arc3
    if (!breach.canPerformUnmake(act)) return state

    val nextHemorrhage = max(0.0, breach.hemorrhage - act.hemorrhageCost)
    val nextFracture =
      min(breach.activeStratum.fractureGoal, breach.fracture + act.instantFractureSurge)
    val jarClotFreeze =
      if (act.category == Arc3UnmakeCategory.SHATTER_JAR) {
        max(breach.clotFreezeRemainingSeconds, 3.0)
      } else {
        breach.clotFreezeRemainingSeconds
      }

    val updated =
      breach.copy(
        hemorrhage = nextHemorrhage,
        fracture = nextFracture,
        completedUnmakes = breach.completedUnmakes + act,
        clotFreezeRemainingSeconds = jarClotFreeze,
        secondsSinceLastAction = 0.0,
      )
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc3 = updated), catalog)
  }

  fun tearBreachRupture(
    state: OrganGameState,
    ruptureId: Long,
    catalog: DialogueCatalog,
  ): OrganGameState {
    if (state.activeArc != GameArc.ARC_3_CONVERGENCE) return state
    val breach = state.arc3
    if (breach.isInFinale || breach.submersionComplete) return state

    val target = breach.activeRuptures.firstOrNull { it.id == ruptureId } ?: return state
    val remaining = breach.activeRuptures.filterNot { it.id == ruptureId }

    val baseFracture = state.arc3WrenchFractureYield() * breach.unmakeRuptureMultiplier
    val baseHemorrhage = state.arc3WrenchHemorrhageYield() * breach.unmakeRuptureMultiplier
    val fracGain = baseFracture * target.type.fractureWrenchMult
    val hemGain = baseHemorrhage * target.type.hemorrhageWrenchMult

    val nextCounts = breach.ruptureInteractions.toMutableMap()
    nextCounts[target.type] = (nextCounts[target.type] ?: 0) + 1

    val updated =
      breach.copy(
        fracture = min(breach.activeStratum.fractureGoal, breach.fracture + fracGain),
        hemorrhage = breach.hemorrhage + hemGain,
        lifetimeHemorrhage = breach.lifetimeHemorrhage + hemGain,
        activeRuptures = remaining,
        ruptureInteractions = nextCounts,
        clotFreezeRemainingSeconds = max(breach.clotFreezeRemainingSeconds, target.type.clotFreezeSec),
        ribForceRemainingSeconds = max(breach.ribForceRemainingSeconds, target.type.ribForceSec),
        frenzyRemainingSeconds = max(breach.frenzyRemainingSeconds, target.type.frenzySec),
        secondsSinceLastAction = 0.0,
      )
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc3 = updated), catalog)
  }

  fun driveMemoryWedge(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    if (state.activeArc != GameArc.ARC_3_CONVERGENCE) return state
    val breach = state.arc3
    if (!breach.canDriveWedge) return state

    val locked = breach.activeStratum
    val nextLocked = breach.lockedWedges + locked
    val enteringFinale = nextLocked.size >= Arc3Stratum.entries.size
    val nextStratum = if (enteringFinale) Arc3Stratum.THE_OPEN_SEAM.number else locked.number + 1

    val updated =
      breach.copy(
        currentStratum = nextStratum,
        lockedWedges = nextLocked,
        fracture = 0.0,
        secondsSinceLastAction = 0.0,
        clotFreezeRemainingSeconds = if (enteringFinale) 0.0 else 4.0,
        activeRuptures = if (enteringFinale) emptyList() else breach.activeRuptures,
        finalPullCooldownSeconds =
          if (enteringFinale) Arc3Balance.FINAL_PULL_COOLDOWN_SEC
          else breach.finalPullCooldownSeconds,
      )
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc3 = updated), catalog)
  }

  fun performFinalPull(state: OrganGameState, catalog: DialogueCatalog): OrganGameState {
    if (state.activeArc != GameArc.ARC_3_CONVERGENCE) return state
    val breach = state.arc3
    if (!breach.canPerformFinalPull) return state

    val nextPulls = breach.finalPullsCompleted + 1
    val finished = nextPulls >= Arc3Balance.FINAL_PULLS_TOTAL
    val updated =
      breach.copy(
        finalPullsCompleted = nextPulls,
        finalPullCooldownSeconds = if (finished) 0.0 else Arc3Balance.FINAL_PULL_COOLDOWN_SEC,
        submersionComplete = finished,
      )
    return OrganGameEngine.finalizeExternalTransition(state.copy(arc3 = updated), catalog)
  }

  fun tick(
    state: OrganGameState,
    deltaSeconds: Double,
    catalog: DialogueCatalog,
  ): OrganGameState {
    if (state.activeArc != GameArc.ARC_3_CONVERGENCE || deltaSeconds <= 0.0) return state
    val breach = state.arc3

    if (breach.submersionComplete) return state

    if (breach.isInFinale) {
      if (breach.finalPullCooldownSeconds <= 0.0) return state
      val nextCd = max(0.0, breach.finalPullCooldownSeconds - deltaSeconds)
      return state.copy(arc3 = breach.copy(finalPullCooldownSeconds = nextCd))
    }

    val nextIdle = breach.secondsSinceLastAction + deltaSeconds
    val nextClotFreeze = max(0.0, breach.clotFreezeRemainingSeconds - deltaSeconds)
    val nextRibForce = max(0.0, breach.ribForceRemainingSeconds - deltaSeconds)
    val nextFrenzy = max(0.0, breach.frenzyRemainingSeconds - deltaSeconds)
    val nextMomentum =
      if (nextIdle >= Arc3Balance.MOMENTUM_DECAY_DELAY_SEC) {
        max(0.0, breach.momentumCharge - Arc3Balance.MOMENTUM_DECAY_PER_SEC * deltaSeconds)
      } else {
        breach.momentumCharge
      }

    // Active Clotting pulls Fracture gently back toward 0 (never below locked Wedge)
    val preClotState =
      breach.copy(
        secondsSinceLastAction = nextIdle,
        clotFreezeRemainingSeconds = nextClotFreeze,
        ribForceRemainingSeconds = nextRibForce,
        frenzyRemainingSeconds = nextFrenzy,
        momentumCharge = nextMomentum,
      )
    val clotPull = preClotState.activeClotPerSec * deltaSeconds
    val nextFracture = max(0.0, breach.fracture - clotPull)

    // Passive Hemorrhage trickle from reversed organisms (never advances Fracture on its own)
    val bleedHemorrhage = breach.bleedPressurePerSec * 0.20 * deltaSeconds

    // Decay active ruptures and spawn fresh urgent tears
    val survivingRuptures =
      breach.activeRuptures.mapNotNull { r ->
        val rem = r.remainingSeconds - deltaSeconds
        if (rem > 0.0) r.copy(remainingSeconds = rem) else null
      }

    var nextBreach =
      preClotState.copy(
        fracture = nextFracture,
        hemorrhage = breach.hemorrhage + bleedHemorrhage,
        lifetimeHemorrhage = breach.lifetimeHemorrhage + bleedHemorrhage,
        activeRuptures = survivingRuptures,
      )

    if (nextBreach.activeRuptures.size < Arc3Balance.MAX_ACTIVE_RUPTURES) {
      val remSpawn = nextBreach.secondsUntilNextRupture - deltaSeconds
      nextBreach =
        if (remSpawn <= 0.0) {
          spawnImmediateRupture(nextBreach)
        } else {
          nextBreach.copy(secondsUntilNextRupture = remSpawn)
        }
    }

    return OrganGameEngine.finalizeExternalTransition(state.copy(arc3 = nextBreach), catalog)
  }

  fun spawnImmediateRupture(breach: Arc3BreachState): Arc3BreachState {
    if (breach.isInFinale || breach.submersionComplete) return breach
    if (breach.activeRuptures.size >= Arc3Balance.MAX_ACTIVE_RUPTURES + 1) return breach

    val eligible = BreachRuptureType.entries.filter { it.minStratum <= breach.currentStratum }
    if (eligible.isEmpty()) return breach

    val activeTypes = breach.activeRuptures.map { it.type }.toSet()
    val pool = eligible.filterNot { it in activeTypes }.ifEmpty { eligible }
    val id = breach.nextRuptureId
    val chosenType = pool[((id - 1L) % pool.size).toInt()]

    // Keep ruptures above the rising flood line when possible
    val maxY = (0.82f - breach.floodLevel * 0.45f).coerceIn(0.26f, 0.66f)
    val usedAnchors = breach.activeRuptures.map { it.xFraction to it.yFraction }.toSet()
    val freeAnchors = ruptureAnchors.filterNot { it in usedAnchors }.ifEmpty { ruptureAnchors }
    val (rawX, rawY) = freeAnchors[((id - 1L) % freeAnchors.size).toInt()]
    val clampedY = min(rawY, maxY)

    return breach.copy(
      activeRuptures =
        breach.activeRuptures +
          ActiveBreachRupture(
            id = id,
            type = chosenType,
            xFraction = rawX,
            yFraction = clampedY,
            remainingSeconds = Arc3Balance.RUPTURE_LIFETIME_SEC,
          ),
      nextRuptureId = id + 1L,
      secondsUntilNextRupture = Arc3Balance.RUPTURE_SPAWN_INTERVAL_SEC,
    )
  }
}
