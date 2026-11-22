package com.example.game

import com.example.data.DialogueEntry
import com.example.data.EscalationCurve
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

enum class CreatureType(
  val id: String,
  val baseCost: Double,
  val costGrowth: Double,
  val baseIchorPerSec: Double,
  val unlockIchorThreshold: Long,
) {
  SUTURE_HAND(
    id = "suture_hand",
    baseCost = 25.0,
    costGrowth = 1.24,
    baseIchorPerSec = 0.6,
    unlockIchorThreshold = 25L,
  ),
  LEECH_CLERK(
    id = "leech_clerk",
    baseCost = 180.0,
    costGrowth = 1.26,
    baseIchorPerSec = 2.4,
    unlockIchorThreshold = 75L,
  ),
  WEEPING_LAMP(
    id = "weeping_lamp",
    baseCost = 1_400.0,
    costGrowth = 1.28,
    baseIchorPerSec = 6.0,
    unlockIchorThreshold = 1_200L,
  ),
}

enum class SpecimenJar(val id: String, val stillnessCost: Double) {
  YESTERDAYS_DUST(id = "yesterdays_dust", stillnessCost = 24.0),
  UNSPENT_BREATH(id = "unspent_breath", stillnessCost = 65.0),
  ALMOST_A_NAME(id = "almost_a_name", stillnessCost = 140.0),
}

enum class WallAnomalyType(val id: String, val minPhase: Int) {
  PALE_TOOTH(id = "pale_tooth", minPhase = 1),
  GLASS_EYE(id = "glass_eye", minPhase = 2),
  SWOLLEN_NODULE(id = "swollen_nodule", minPhase = 3),
}

/** Normalized coordinates (0f..1f) anchoring interactive wall anomalies inside the Chamber Canvas. */
data class ActiveWallAnomaly(
  val id: Long,
  val type: WallAnomalyType,
  val xFraction: Float,
  val yFraction: Float,
)

enum class OrganDeckTab {
  APPARATUS,
  JARS_AND_GRAFT,
  EXCISION,
}

/**
 * Complete immutable state for Direction 1 ("The Tending of the Unnamed Organ"), paced for ~15
 * minutes of moderate activity from tap 1 to First Excision.
 */
data class OrganGameState(
  val ichor: Double = 0.0,
  val cycleLifetimeIchor: Double = 0.0,
  val stillness: Double = 0.0,
  val cycleLifetimeStillness: Double = 0.0,
  val scarTissue: Long = 0L,
  val cycle: Int = 0,
  val phase: Int = 0,
  val phaseName: String? = null,
  val nextPhaseThreshold: Long? = EscalationCurve.nextThresholdForPhase(0),
  val phaseProgress: Float = 0f,
  val creatures: Map<CreatureType, Int> = emptyMap(),
  val graftedTendrils: Int = 0,
  val ownedJars: Set<SpecimenJar> = emptySet(),
  val activeAnomalies: List<ActiveWallAnomaly> = emptyList(),
  val anomalyInteractions: Map<WallAnomalyType, Int> = emptyMap(),
  val secondsSinceLastTap: Double = 0.0,
  val isStillnessActive: Boolean = false,
  val gazeRemainingSeconds: Double = 0.0,
  val secondsUntilNextAnomaly: Double = OrganBalance.FIRST_ANOMALY_DELAY_SEC,
  val nextAnomalyId: Long = 1L,
  val activeDialogue: DialogueEntry? = null,
  val discoveredDialogues: List<DialogueEntry> = emptyList(),
  val triggeredDialogueIds: Set<String> = emptySet(),
) {
  val ichorDisplay: Long
    get() = floor(ichor).toLong()

  val cycleLifetimeIchorDisplay: Long
    get() = floor(cycleLifetimeIchor).toLong()

  val stillnessDisplay: Long
    get() = floor(stillness).toLong()

  fun creatureCount(type: CreatureType): Int = creatures[type] ?: 0

  fun creatureCost(type: CreatureType): Long {
    val owned = creatureCount(type)
    return floor(type.baseCost * type.costGrowth.pow(owned)).toLong()
  }

  val scarMultiplier: Double
    get() = 1.0 + scarTissue * OrganBalance.SCAR_TISSUE_BONUS_PER_UNIT

  val gazeMultiplier: Double
    get() = if (gazeRemainingSeconds > 0.0) OrganBalance.GAZE_PASSIVE_MULTIPLIER else 1.0

  val manualTapYield: Double
    get() {
      val dustBonus =
        if (SpecimenJar.YESTERDAYS_DUST in ownedJars) OrganBalance.YESTERDAYS_DUST_TAP_BONUS
        else 0.0
      val toothBonus = (anomalyInteractions[WallAnomalyType.PALE_TOOTH] ?: 0) * 0.5
      return (1.0 + dustBonus + toothBonus) * scarMultiplier
    }

  val ichorPerSecond: Double
    get() {
      val clerkMultiplier =
        if (SpecimenJar.UNSPENT_BREATH in ownedJars) OrganBalance.UNSPENT_BREATH_MULTIPLIER
        else 1.0
      val lampMultiplier =
        (if (SpecimenJar.ALMOST_A_NAME in ownedJars) OrganBalance.ALMOST_A_NAME_MULTIPLIER
        else 1.0) * (if (isStillnessActive) 2.0 else 1.0)

      val sutureRate =
        creatureCount(CreatureType.SUTURE_HAND) *
          CreatureType.SUTURE_HAND.baseIchorPerSec *
          clerkMultiplier
      val leechRate =
        creatureCount(CreatureType.LEECH_CLERK) *
          CreatureType.LEECH_CLERK.baseIchorPerSec *
          clerkMultiplier
      val lampRate =
        creatureCount(CreatureType.WEEPING_LAMP) *
          CreatureType.WEEPING_LAMP.baseIchorPerSec *
          lampMultiplier
      val graftRate = graftedTendrils * OrganBalance.GRAFT_ICHOR_PER_SEC

      return (sutureRate + leechRate + lampRate + graftRate) * scarMultiplier * gazeMultiplier
    }

  val stillnessPerSecond: Double
    get() {
      val lampMultiplier =
        if (SpecimenJar.ALMOST_A_NAME in ownedJars) OrganBalance.ALMOST_A_NAME_MULTIPLIER else 1.0
      val lampStillness =
        if (isStillnessActive) {
          creatureCount(CreatureType.WEEPING_LAMP) *
            OrganBalance.LAMP_STILLNESS_PER_SEC *
            lampMultiplier
        } else {
          0.0
        }
      val graftStillness = graftedTendrils * OrganBalance.GRAFT_STILLNESS_PER_SEC
      return (lampStillness + graftStillness) * gazeMultiplier
    }

  val isApparatusUnlocked: Boolean
    get() =
      cycle > 0 ||
        cycleLifetimeIchorDisplay >= CreatureType.SUTURE_HAND.unlockIchorThreshold ||
        creatures.values.any { it > 0 }

  val isJarsAndGraftUnlocked: Boolean
    get() =
      cycle > 0 ||
        phase >= 2 ||
        stillnessDisplay > 0L ||
        ownedJars.isNotEmpty() ||
        graftedTendrils > 0

  val isGraftUnlocked: Boolean
    get() = cycle > 0 || phase >= 3 || graftedTendrils > 0

  val canAffordGraft: Boolean
    get() =
      isGraftUnlocked &&
        creatureCount(CreatureType.SUTURE_HAND) >= OrganBalance.GRAFT_SUTURE_COST &&
        creatureCount(CreatureType.LEECH_CLERK) >= OrganBalance.GRAFT_LEECH_COST

  val canExcise: Boolean
    get() = cycleLifetimeIchorDisplay >= OrganBalance.EXCISION_ICHOR_THRESHOLD

  val pendingScarTissue: Long
    get() {
      if (!canExcise) return 0L
      return floor(sqrt(cycleLifetimeIchor / OrganBalance.EXCISION_ICHOR_THRESHOLD.toDouble()))
        .toLong()
        .coerceAtLeast(1L)
    }
}

object OrganBalance {
  const val STILLNESS_IDLE_DELAY_SEC: Double = 3.0
  const val LAMP_STILLNESS_PER_SEC: Double = 0.45
  const val GAZE_DURATION_SEC: Double = 12.0
  const val GAZE_PASSIVE_MULTIPLIER: Double = 1.6
  const val YESTERDAYS_DUST_TAP_BONUS: Double = 3.0
  const val UNSPENT_BREATH_MULTIPLIER: Double = 1.75
  const val ALMOST_A_NAME_MULTIPLIER: Double = 1.8
  const val GRAFT_SUTURE_COST: Int = 2
  const val GRAFT_LEECH_COST: Int = 2
  const val GRAFT_ICHOR_PER_SEC: Double = 18.0
  const val GRAFT_STILLNESS_PER_SEC: Double = 0.7
  const val EXCISION_ICHOR_THRESHOLD: Long = 150_000L
  const val SCAR_TISSUE_BONUS_PER_UNIT: Double = 0.75
  const val FIRST_ANOMALY_DELAY_SEC: Double = 14.0
  const val ANOMALY_SPAWN_INTERVAL_SEC: Double = 26.0
  const val MAX_ACTIVE_ANOMALIES: Int = 3
  const val TOOTH_BASE_ICHOR_REWARD: Double = 25.0
  const val NODULE_BASE_ICHOR_REWARD: Double = 220.0
  const val NODULE_BASE_STILLNESS_REWARD: Double = 6.0
}
