package com.example.game

import com.example.data.DialogueEntry
import com.example.data.EscalationCurve
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

enum class CreatureType(
  val id: String,
  val baseCost: Double,
  val costGrowth: Double,
  val baseIchorPerSec: Double,
  val unlockIchorThreshold: Long,
  val requiredMilestone: Int = 0,
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
    baseCost = 1_190.0,
    costGrowth = 1.32,
    baseIchorPerSec = 6.0,
    unlockIchorThreshold = 1_000L,
  ),
  SUMP_SIPHON(
    id = "sump_siphon",
    baseCost = 28_000.0,
    costGrowth = 1.28,
    baseIchorPerSec = 42.0,
    unlockIchorThreshold = 15_000L,
    requiredMilestone = 2,
  );

  companion object {
    fun byId(id: String): CreatureType? = entries.firstOrNull { it.id == id }
  }
}

enum class SpecimenJar(
  val id: String,
  val stillnessCost: Double,
  val unlockPhase: Int = 0,
  val requiresCycleOrMilestone: Boolean = false,
) {
  YESTERDAYS_DUST(id = "yesterdays_dust", stillnessCost = 24.0, unlockPhase = 0),
  UNSPENT_BREATH(id = "unspent_breath", stillnessCost = 65.0, unlockPhase = 0),
  ALMOST_A_NAME(id = "almost_a_name", stillnessCost = 140.0, unlockPhase = 0),
  SALT_FROM_THE_GROUT(id = "salt_from_the_grout", stillnessCost = 320.0, unlockPhase = 3),
  A_BORROWED_PULSE(id = "a_borrowed_pulse", stillnessCost = 750.0, unlockPhase = 4),
  THE_UNCOUNTED_HOUR(
    id = "the_uncounted_hour",
    stillnessCost = 1_800.0,
    unlockPhase = 0,
    requiresCycleOrMilestone = true,
  );

  companion object {
    fun byId(id: String): SpecimenJar? = entries.firstOrNull { it.id == id }
  }
}

enum class GraftSacrificeKind(val id: String) {
  TENDRIL("tendril"),
  CHOIR_LUNG("choir_lung"),
}

enum class WallAnomalyType(val id: String, val minPhase: Int) {
  PALE_TOOTH(id = "pale_tooth", minPhase = 1),
  GLASS_EYE(id = "glass_eye", minPhase = 2),
  SWOLLEN_NODULE(id = "swollen_nodule", minPhase = 3),
  CHITIN_RIB(id = "chitin_rib", minPhase = 4);

  companion object {
    fun byId(id: String): WallAnomalyType? = entries.firstOrNull { it.id == id }
  }
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
  SCAR_TREE,
  CHAMBER_RITES,
  EXCISION,
}

data class BulkCreatureQuote(
  val count: Int,
  val totalCost: Long,
)

/**
 * Unified immutable state spanning Arc 1 (Chamber + 6 Excisions + 5-Tier Surgical Scar Tree),
 * Arc 2 (Haunted Investigator Desk), and Arc 3 (Convergence threshold hook).
 */
data class OrganGameState(
  val activeArc: GameArc = GameArc.ARC_1_CHAMBER,
  val ichor: Double = 0.0,
  val cycleLifetimeIchor: Double = 0.0,
  val stillness: Double = 0.0,
  val cycleLifetimeStillness: Double = 0.0,
  // Lifetime Scar Tissue earned across all excisions (mirrors legacy scarTissue field)
  val scarTissue: Long = 0L,
  // Spendable Scar Tissue available for suturing Surgical Skill Tree nodes & repeatable grafts
  val availableScarTissue: Long = 0L,
  val highestExcisionMilestone: Int = 0,
  val unlockedScarNodes: Set<ScarNodeId> = emptySet(),
  val repeatableScarRanks: Map<RepeatableScarGraft, Int> = emptyMap(),
  val cycle: Int = 0,
  val phase: Int = 0,
  val phaseName: String? = null,
  val nextPhaseThreshold: Long? = EscalationCurve.nextThresholdForPhase(0),
  val phaseProgress: Float = 0f,
  val creatures: Map<CreatureType, Int> = emptyMap(),
  val graftedTendrils: Int = 0,
  val choirLungs: Int = 0,
  val graftCooldownRemainingSeconds: Double = 0.0,
  val choirLungCooldownRemainingSeconds: Double = 0.0,
  val autoValveChargeSeconds: Double = 0.0,
  val lastSacrificePulse: Long = 0L,
  val lastSacrificeKind: GraftSacrificeKind? = null,
  val ownedJars: Set<SpecimenJar> = emptySet(),
  val activeAnomalies: List<ActiveWallAnomaly> = emptyList(),
  val anomalyInteractions: Map<WallAnomalyType, Int> = emptyMap(),
  val secondsSinceLastTap: Double = 0.0,
  val isStillnessActive: Boolean = false,
  val gazeRemainingSeconds: Double = 0.0,
  val secondsUntilNextAnomaly: Double = OrganBalance.FIRST_ANOMALY_DELAY_SEC,
  val nextAnomalyId: Long = 1L,
  // Excision II: Floor Drain Grate & Dredged Relics
  val drainSealed: Boolean = false,
  val drainPressure: Double = 0.0,
  val dredgedRelics: Set<DredgedRelic> = emptySet(),
  val lifetimeRelicsDredged: Int = 0,
  // Excision III: Neighbor Aperture Echo Routing
  val echoDiversionPercent: Int = 0,
  val echoChargeSeconds: Double = 0.0,
  val echoPulsesTriggered: Int = 0,
  // Excision IV: Valve Clamp & Marrow Bloom
  val clampRemainingSeconds: Double = 0.0,
  val marrowBloomRemainingSeconds: Double = 0.0,
  val marrowBloomCount: Int = 0,
  // Excision V: Ossified Throat & Permanent Cult Dossier
  val unredactedDossierLines: Set<CultDossierLine> = emptySet(),
  // Arc 2: Haunted Investigator Desk state
  val arc2: Arc2DeskState = Arc2DeskState(),
  // Arc 3: The Unmaking (No-Reset Breach & Crimson Flood) state
  val arc3: Arc3BreachState = Arc3BreachState(),
  // Shared Progress-Bound Narrative Ledger
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

  fun hasNode(node: ScarNodeId): Boolean = node in unlockedScarNodes

  fun canUnlockNode(node: ScarNodeId): Boolean {
    if (node in unlockedScarNodes) return false
    val prereqOk = node.prerequisite == null || node.prerequisite in unlockedScarNodes
    return prereqOk && availableScarTissue >= node.cost
  }

  fun repeatableRank(graft: RepeatableScarGraft): Int = repeatableScarRanks[graft] ?: 0

  fun repeatableCost(graft: RepeatableScarGraft): Long =
    floor(graft.baseCost * graft.costGrowth.pow(repeatableRank(graft))).toLong().coerceAtLeast(1L)

  val isRepeatableGraftsUnlocked: Boolean
    get() = hasNode(ScarNodeId.FIRST_INCISION)

  fun canBuyRepeatableGraft(graft: RepeatableScarGraft): Boolean =
    isRepeatableGraftsUnlocked && availableScarTissue >= repeatableCost(graft)

  fun creatureCount(type: CreatureType): Int = creatures[type] ?: 0

  fun isCreatureUnlocked(type: CreatureType): Boolean {
    if (highestExcisionMilestone < type.requiredMilestone) return false
    return cycle > 0 ||
      cycleLifetimeIchorDisplay >= type.unlockIchorThreshold ||
      creatureCount(type) > 0
  }

  private fun effectiveCreatureGrowth(type: CreatureType): Double =
    if (hasNode(ScarNodeId.COLONY_INERTIA)) max(1.14, type.costGrowth - 0.04)
    else type.costGrowth

  fun creatureCost(type: CreatureType): Long {
    val owned = creatureCount(type)
    return floor(type.baseCost * effectiveCreatureGrowth(type).pow(owned)).toLong()
  }

  /** Thematic bulk-spawn ("PROLIFERATE") unlocks after clearing Excision III (The Unbricked Seam). */
  val isColonyProliferationUnlocked: Boolean
    get() = highestExcisionMilestone >= 3

  fun maxAffordableCreatureQuote(type: CreatureType): BulkCreatureQuote {
    if (!isCreatureUnlocked(type)) return BulkCreatureQuote(0, 0L)
    var remaining = ichor
    var owned = creatureCount(type)
    val growth = effectiveCreatureGrowth(type)
    var bought = 0
    var totalSpent = 0L
    while (bought < 500) {
      val nextCost = floor(type.baseCost * growth.pow(owned)).toLong().coerceAtLeast(1L)
      if (remaining < nextCost.toDouble()) break
      remaining -= nextCost.toDouble()
      totalSpent += nextCost
      owned += 1
      bought += 1
    }
    return BulkCreatureQuote(bought, totalSpent)
  }

  val stillnessIdleDelaySec: Double
    get() =
      when {
        hasNode(ScarNodeId.CATATONIC_TIDE) -> OrganBalance.CATATONIC_TIDE_DELAY_SEC
        hasNode(ScarNodeId.SHORTER_SILENCE) -> OrganBalance.SHORTER_SILENCE_DELAY_SEC
        else -> OrganBalance.STILLNESS_IDLE_DELAY_SEC
      }

  val jarEffectBoost: Double
    get() =
      1.0 +
        (if (hasNode(ScarNodeId.WAX_SEALED_MEMORY)) 0.35 else 0.0) +
        (if (hasNode(ScarNodeId.CATATONIC_TIDE)) 0.65 else 0.0)

  val sedimentCostMultiplier: Double
    get() =
      max(
        OrganBalance.SEDIMENT_WELL_COST_FLOOR,
        OrganBalance.SEDIMENT_WELL_COST_FACTOR.pow(repeatableRank(RepeatableScarGraft.SEDIMENT_WELL)),
      )

  val stillnessCostMultiplier: Double
    get() {
      val waxMult =
        if (hasNode(ScarNodeId.WAX_SEALED_MEMORY)) OrganBalance.WAX_STILLNESS_COST_MULTIPLIER
        else 1.0
      return waxMult * sedimentCostMultiplier
    }

  val graftCooldownDurationSec: Double
    get() =
      OrganBalance.GRAFT_COOLDOWN_SEC *
        if (hasNode(ScarNodeId.WAX_SEALED_MEMORY)) OrganBalance.WAX_GRAFT_COOLDOWN_MULTIPLIER
        else 1.0

  val choirLungCooldownDurationSec: Double
    get() =
      OrganBalance.CHOIR_LUNG_COOLDOWN_SEC *
        if (hasNode(ScarNodeId.WAX_SEALED_MEMORY)) OrganBalance.WAX_GRAFT_COOLDOWN_MULTIPLIER
        else 1.0

  val echoPulseIntervalSec: Double
    get() =
      if (hasNode(ScarNodeId.ECHO_OSSUARY)) OrganBalance.ECHO_OSSUARY_INTERVAL_SEC
      else OrganBalance.ECHO_PULSE_INTERVAL_SEC

  val valveClampDurationSec: Double
    get() =
      if (hasNode(ScarNodeId.BLOOM_INCISION)) OrganBalance.BLOOM_INCISION_CLAMP_DURATION_SEC
      else OrganBalance.VALVE_CLAMP_DURATION_SEC

  val marrowBloomDurationSec: Double
    get() =
      if (hasNode(ScarNodeId.BLOOM_INCISION)) OrganBalance.BLOOM_INCISION_DURATION_SEC
      else OrganBalance.MARROW_BLOOM_DURATION_SEC

  fun isJarUnlocked(jar: SpecimenJar): Boolean {
    if (jar in ownedJars) return true
    if (jar.requiresCycleOrMilestone) {
      return cycle > 0 || highestExcisionMilestone >= 1 || scarTissue > 0L
    }
    return cycle > 0 || phase >= jar.unlockPhase
  }

  fun effectiveJarCost(jar: SpecimenJar): Double =
    max(1.0, floor(jar.stillnessCost * stillnessCostMultiplier))

  val graftStillnessCost: Long
    get() =
      floor(
          OrganBalance.GRAFT_BASE_STILLNESS_COST *
            OrganBalance.GRAFT_STILLNESS_GROWTH.pow(graftedTendrils) *
            stillnessCostMultiplier
        )
        .toLong()
        .coerceAtLeast(1L)

  val choirLungStillnessCost: Long
    get() =
      floor(
          OrganBalance.CHOIR_LUNG_BASE_STILLNESS_COST *
            OrganBalance.CHOIR_LUNG_STILLNESS_GROWTH.pow(choirLungs) *
            stillnessCostMultiplier
        )
        .toLong()
        .coerceAtLeast(1L)

  val groutSaltOrganismMultiplier: Double
    get() =
      if (SpecimenJar.SALT_FROM_THE_GROUT in ownedJars) {
        1.0 + OrganBalance.SALT_GROUT_BONUS * jarEffectBoost
      } else {
        1.0
      }

  val borrowedPulseMultiplier: Double
    get() =
      if (SpecimenJar.A_BORROWED_PULSE in ownedJars) {
        1.0 + OrganBalance.BORROWED_PULSE_BONUS * jarEffectBoost
      } else {
        1.0
      }

  val uncountedHourMultiplier: Double
    get() =
      if (SpecimenJar.THE_UNCOUNTED_HOUR in ownedJars) {
        1.0 + OrganBalance.UNCOUNTED_HOUR_BONUS * jarEffectBoost
      } else {
        1.0
      }

  val keloidIchorMultiplier: Double
    get() = 1.0 + repeatableRank(RepeatableScarGraft.KELOID_THICKENING) * OrganBalance.KELOID_ICHOR_PER_RANK

  val sedimentStillnessMultiplier: Double
    get() = 1.0 + repeatableRank(RepeatableScarGraft.SEDIMENT_WELL) * OrganBalance.SEDIMENT_STILLNESS_PER_RANK

  val scarHarvestMultiplier: Double
    get() = 1.0 + repeatableRank(RepeatableScarGraft.SCAR_HARVEST_HOOK) * OrganBalance.SCAR_HARVEST_HOOK_PER_RANK

  val catatonicTideMultiplier: Double
    get() =
      if (!hasNode(ScarNodeId.CATATONIC_TIDE)) {
        1.0
      } else if (isStillnessActive) {
        3.20
      } else {
        1.75
      }

  /**
   * Permanent Scar multiplier combining root-softcapped milestone/scar resonance with Surgical Tree
   * core nodes and Keloid Thickening. Equals 1.0 in fresh Cycle 0 so initial 15-minute pacing stays exact.
   */
  val scarMultiplier: Double
    get() {
      val baseResonance =
        1.0 +
          highestExcisionMilestone * OrganBalance.MILESTONE_ICHOR_RESONANCE +
          sqrt(scarTissue.toDouble()) * OrganBalance.SCAR_RESONANCE_SQRT_FACTOR
      val rootMult = if (hasNode(ScarNodeId.FIRST_INCISION)) 1.45 else 1.0
      val boneMult = if (hasNode(ScarNodeId.BONE_RESONANCE)) 2.10 else 1.0
      return baseResonance * rootMult * boneMult * keloidIchorMultiplier
    }

  val scarStillnessMultiplier: Double
    get() {
      val baseResonance =
        1.0 +
          highestExcisionMilestone * OrganBalance.MILESTONE_STILLNESS_RESONANCE +
          sqrt(scarTissue.toDouble()) * OrganBalance.SCAR_STILLNESS_SQRT_FACTOR
      val rootMult = if (hasNode(ScarNodeId.FIRST_INCISION)) 1.20 else 1.0
      val silenceMult = if (hasNode(ScarNodeId.SHORTER_SILENCE)) 1.50 else 1.0
      return baseResonance * rootMult * silenceMult * sedimentStillnessMultiplier
    }

  val relicAmplifier: Double
    get() =
      (if (hasNode(ScarNodeId.ECHO_OSSUARY)) 1.35 else 1.0) *
        (if (hasNode(ScarNodeId.BONE_RESONANCE)) 1.40 else 1.0)

  val relicIchorMultiplier: Double
    get() {
      var mult = 1.0
      val amp = relicAmplifier
      dredgedRelics.forEach { relic ->
        val effective = 1.0 + (relic.ichorMultiplier - 1.0) * amp
        mult *= effective
      }
      return mult
    }

  val relicStillnessMultiplier: Double
    get() {
      var mult = 1.0
      dredgedRelics.forEach { relic -> mult *= relic.stillnessMultiplier }
      return mult
    }

  val dossierIchorMultiplier: Double
    get() {
      var mult = 1.0
      val perLineBoost = if (hasNode(ScarNodeId.BONE_RESONANCE)) 1.18 else 1.0
      unredactedDossierLines.forEach { line -> mult *= line.ichorMultiplier * perLineBoost }
      return mult
    }

  val gazeMultiplier: Double
    get() = if (gazeRemainingSeconds > 0.0) OrganBalance.GAZE_PASSIVE_MULTIPLIER else 1.0

  val bloomMultiplier: Double
    get() =
      if (marrowBloomRemainingSeconds > 0.0) {
        if (hasNode(ScarNodeId.BLOOM_INCISION)) OrganBalance.BLOOM_INCISION_MULTIPLIER
        else OrganBalance.MARROW_BLOOM_MULTIPLIER
      } else {
        1.0
      }

  val anomalyRewardMultiplier: Double
    get() {
      val jarBoost =
        if (SpecimenJar.ALMOST_A_NAME in ownedJars) {
          OrganBalance.ALMOST_A_NAME_MULTIPLIER * jarEffectBoost
        } else {
          1.0
        }
      val mortarBoost = if (hasNode(ScarNodeId.SOFTENED_MORTAR)) 1.8 else 1.0
      val baseEchoBonus =
        when (echoDiversionPercent) {
          25 -> 0.35
          50 -> 0.70
          else -> 0.0
        }
      val echoBoost =
        1.0 + baseEchoBonus * (if (hasNode(ScarNodeId.BONE_RESONANCE)) 1.40 else 1.0)
      val bloomAnomalyBoost =
        if (marrowBloomRemainingSeconds > 0.0 && hasNode(ScarNodeId.BLOOM_INCISION)) 1.5 else 1.0
      return jarBoost * mortarBoost * echoBoost * bloomAnomalyBoost
    }

  val manualTapYield: Double
    get() {
      val dustBonus =
        if (SpecimenJar.YESTERDAYS_DUST in ownedJars) {
          OrganBalance.YESTERDAYS_DUST_TAP_BONUS * jarEffectBoost
        } else {
          0.0
        }
      val toothBonus = (anomalyInteractions[WallAnomalyType.PALE_TOOTH] ?: 0) * 0.5
      val palmMult = if (hasNode(ScarNodeId.CALLOUSED_PALM)) 1.8 else 1.0
      val seamMult = if (hasNode(ScarNodeId.PERPETUAL_SEAM)) 2.50 else 1.0
      val echoBurst = if (hasNode(ScarNodeId.TWIN_STITCH)) ichorPerSecond * 1.5 else 0.0
      return (1.0 + dustBonus + toothBonus) *
        scarMultiplier *
        relicIchorMultiplier *
        dossierIchorMultiplier *
        palmMult *
        seamMult + echoBurst
    }

  val globalIchorMultiplier: Double
    get() =
      scarMultiplier *
        relicIchorMultiplier *
        dossierIchorMultiplier *
        gazeMultiplier *
        bloomMultiplier *
        uncountedHourMultiplier *
        catatonicTideMultiplier

  fun effectiveCreatureIchorPerSec(type: CreatureType): Double {
    val inertiaMult = if (hasNode(ScarNodeId.COLONY_INERTIA)) 1.90 else 1.0
    val seamMult = if (hasNode(ScarNodeId.PERPETUAL_SEAM)) 2.50 else 1.0
    val specimenMultiplier =
      when (type) {
        CreatureType.SUTURE_HAND,
        CreatureType.LEECH_CLERK -> {
          val breath =
            if (SpecimenJar.UNSPENT_BREATH in ownedJars) {
              OrganBalance.UNSPENT_BREATH_MULTIPLIER * jarEffectBoost
            } else {
              1.0
            }
          val twin = if (hasNode(ScarNodeId.TWIN_STITCH)) 1.6 else 1.0
          breath * twin
        }
        CreatureType.WEEPING_LAMP -> {
          val nameJar =
            if (SpecimenJar.ALMOST_A_NAME in ownedJars) {
              OrganBalance.ALMOST_A_NAME_MULTIPLIER * jarEffectBoost
            } else {
              1.0
            }
          val weepingSurge =
            when {
              isStillnessActive -> if (hasNode(ScarNodeId.HOLLOW_LUNG_CHOIR)) 2.6 else 2.0
              hasNode(ScarNodeId.DEEP_WEEPING) -> 1.25
              else -> 1.0
            }
          val deepMult = if (hasNode(ScarNodeId.DEEP_WEEPING)) 2.0 else 1.0
          nameJar * weepingSurge * deepMult
        }
        CreatureType.SUMP_SIPHON -> {
          val autoMult = if (hasNode(ScarNodeId.AUTO_TENDRIL_SUTURE)) 1.65 else 1.0
          val recallMult = if (hasNode(ScarNodeId.MEMBRANE_RECALL)) 2.20 else 1.0
          val sealPenalty = if (drainSealed) 0.85 else 1.0
          autoMult * recallMult * sealPenalty
        }
      }
    return type.baseIchorPerSec *
      specimenMultiplier *
      inertiaMult *
      seamMult *
      groutSaltOrganismMultiplier *
      globalIchorMultiplier
  }

  fun totalCreatureIchorPerSec(type: CreatureType): Double =
    creatureCount(type) * effectiveCreatureIchorPerSec(type)

  fun effectiveLampStillnessPerSec(): Double {
    val lampMultiplier =
      if (SpecimenJar.ALMOST_A_NAME in ownedJars) {
        OrganBalance.ALMOST_A_NAME_MULTIPLIER * jarEffectBoost
      } else {
        1.0
      }
    return OrganBalance.LAMP_STILLNESS_PER_SEC *
      lampMultiplier *
      scarStillnessMultiplier *
      relicStillnessMultiplier *
      gazeMultiplier
  }

  val rawIchorPerSecond: Double
    get() {
      val creaturesRate =
        CreatureType.entries.sumOf { totalCreatureIchorPerSec(it) }
      val seamColonyMult = if (hasNode(ScarNodeId.PERPETUAL_SEAM)) 2.50 else 1.0
      val graftNodeMult =
        (if (hasNode(ScarNodeId.AUTO_TENDRIL_SUTURE)) 1.65 else 1.0) *
          (if (hasNode(ScarNodeId.MEMBRANE_RECALL)) 2.20 else 1.0)
      val graftRate =
        graftedTendrils *
          OrganBalance.GRAFT_ICHOR_PER_SEC *
          graftNodeMult *
          seamColonyMult *
          borrowedPulseMultiplier *
          globalIchorMultiplier
      val choirNodeMult = if (hasNode(ScarNodeId.HOLLOW_LUNG_CHOIR)) 2.10 else 1.0
      val lungStillnessBoost =
        if (isStillnessActive) {
          if (hasNode(ScarNodeId.HOLLOW_LUNG_CHOIR)) 2.0 else 1.5
        } else {
          1.0
        }
      val lungRate =
        choirLungs *
          OrganBalance.CHOIR_LUNG_ICHOR_PER_SEC *
          choirNodeMult *
          seamColonyMult *
          lungStillnessBoost *
          borrowedPulseMultiplier *
          globalIchorMultiplier
      return creaturesRate + graftRate + lungRate
    }

  /** Net Ichor/sec after active Neighbor Aperture echo diversion and Drain Grate sealing. */
  val ichorPerSecond: Double
    get() {
      val echoFactor = 1.0 - (echoDiversionPercent / 100.0)
      val drainFactor = if (drainSealed) 0.85 else 1.0
      return rawIchorPerSecond * echoFactor * drainFactor
    }

  val stillnessPerSecond: Double
    get() {
      val lampFraction =
        when {
          isStillnessActive -> 1.0
          hasNode(ScarNodeId.DEEP_WEEPING) && creatureCount(CreatureType.WEEPING_LAMP) > 0 -> 0.50
          else -> 0.0
        }
      val lampStillness =
        creatureCount(CreatureType.WEEPING_LAMP) * effectiveLampStillnessPerSec() * lampFraction
      val graftStillness =
        graftedTendrils *
          OrganBalance.GRAFT_STILLNESS_PER_SEC *
          scarStillnessMultiplier *
          relicStillnessMultiplier *
          gazeMultiplier
      val choirNodeMult = if (hasNode(ScarNodeId.HOLLOW_LUNG_CHOIR)) 2.10 else 1.0
      val lungStillnessBoost =
        if (isStillnessActive) {
          if (hasNode(ScarNodeId.HOLLOW_LUNG_CHOIR)) 2.0 else 1.5
        } else {
          1.0
        }
      val lungStillness =
        choirLungs *
          OrganBalance.CHOIR_LUNG_STILLNESS_PER_SEC *
          choirNodeMult *
          scarStillnessMultiplier *
          relicStillnessMultiplier *
          gazeMultiplier *
          lungStillnessBoost
      return (lampStillness + graftStillness + lungStillness) *
        borrowedPulseMultiplier *
        uncountedHourMultiplier *
        catatonicTideMultiplier
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

  val isScarTreeUnlocked: Boolean
    get() = highestExcisionMilestone >= 1 || cycle > 0 || scarTissue > 0L

  val isChamberRitesUnlocked: Boolean
    get() = highestExcisionMilestone >= 2

  val isValveClamped: Boolean
    get() = clampRemainingSeconds > 0.0

  val isGraftOnCooldown: Boolean
    get() = graftCooldownRemainingSeconds > 0.0

  val isChoirLungOnCooldown: Boolean
    get() = choirLungCooldownRemainingSeconds > 0.0

  val canAffordGraft: Boolean
    get() =
      isGraftUnlocked &&
        !isGraftOnCooldown &&
        creatureCount(CreatureType.SUTURE_HAND) >= OrganBalance.GRAFT_SUTURE_COST &&
        creatureCount(CreatureType.LEECH_CLERK) >= OrganBalance.GRAFT_LEECH_COST &&
        stillness >= graftStillnessCost.toDouble()

  val canAffordChoirLung: Boolean
    get() =
      highestExcisionMilestone >= 4 &&
        !isChoirLungOnCooldown &&
        creatureCount(CreatureType.WEEPING_LAMP) >= OrganBalance.CHOIR_LUNG_LAMP_COST &&
        graftedTendrils >= OrganBalance.CHOIR_LUNG_TENDRIL_COST &&
        stillness >= choirLungStillnessCost.toDouble()

  val nextDredgeableRelic: DredgedRelic?
    get() = DredgedRelic.entries.firstOrNull { it !in dredgedRelics }

  val canDredgeSump: Boolean
    get() =
      highestExcisionMilestone >= 2 &&
        drainPressure >= 100.0 &&
        nextDredgeableRelic != null

  val nextRedactedLine: CultDossierLine?
    get() = CultDossierLine.entries.firstOrNull { it !in unredactedDossierLines }

  fun canOfferToThroat(line: CultDossierLine): Boolean {
    if (highestExcisionMilestone < 5 || line in unredactedDossierLines) return false
    val enoughSiphons = creatureCount(CreatureType.SUMP_SIPHON) >= line.siphonCost
    val enoughTendrils = graftedTendrils >= line.tendrilCost
    val enoughJars = !line.requiresJar || ownedJars.isNotEmpty()
    return enoughSiphons && enoughTendrils && enoughJars
  }

  val nextExcisionMilestone: ExcisionMilestone?
    get() = ExcisionMilestone.nextAfter(highestExcisionMilestone)

  /** True when player can execute at least a Harvest & Molt reset (210k floor). */
  val canExcise: Boolean
    get() = cycleLifetimeIchorDisplay >= OrganBalance.EXCISION_ICHOR_THRESHOLD

  /** True when the current run's mass breaches the next sequential Excision Milestone (I..VI). */
  val canClearNextMilestone: Boolean
    get() {
      val next = nextExcisionMilestone ?: return false
      return cycleLifetimeIchorDisplay >= next.massThreshold
    }

  /**
   * True when cycle mass crosses a milestone beyond the immediate next seam; the sequential +1
   * cap still restricts this Excision to severing only [nextExcisionMilestone].
   */
  val hasExcessMilestoneMass: Boolean
    get() {
      if (!canClearNextMilestone) return false
      val beyond = ExcisionMilestone.byNumber(highestExcisionMilestone + 2) ?: return false
      return cycleLifetimeIchorDisplay >= beyond.massThreshold
    }

  /**
   * True when Excision V is already cleared and Excision VI mass is met (or Excision VI is already
   * cleared), opening the Breach Ritual into Arc 2 without allowing Excision V to be skipped.
   */
  val canBreachToArc2: Boolean
    get() =
      highestExcisionMilestone >= 6 ||
        (highestExcisionMilestone >= 5 &&
          cycleLifetimeIchorDisplay >= ExcisionMilestone.FINAL_SEVERANCE.massThreshold)

  private val minExcisionScarHarvest: Long
    get() =
      floor(OrganBalance.SCAR_CURVE_BASE_SCALE * scarHarvestMultiplier)
        .toLong()
        .coerceAtLeast(2L)

  private fun rawScarHarvestForMass(mass: Double): Long {
    if (mass < OrganBalance.EXCISION_ICHOR_THRESHOLD.toDouble()) return 0L
    val ratio = mass / OrganBalance.EXCISION_ICHOR_THRESHOLD.toDouble()
    val raw =
      OrganBalance.SCAR_CURVE_BASE_SCALE *
        ratio.pow(OrganBalance.SCAR_CURVE_EXPONENT) *
        scarHarvestMultiplier
    return floor(raw).toLong().coerceAtLeast(2L)
  }

  /**
   * When the active next Excision milestone sits strictly inside a scar band
   * (`rawScarHarvestForMass(M - 1) == rawScarHarvestForMass(M) = c`), the natural threshold for
   * scar `c + 1` would lie beyond `M`. Pull scar `c + 1` down to `M` so "Next Scar Tissue" never
   * exceeds the current milestone threshold and reaching `M` awards that scar early.
   */
  private val milestonePulledScarCount: Long?
    get() {
      val milestoneMass = nextExcisionMilestone?.massThreshold ?: return null
      if (milestoneMass <= OrganBalance.EXCISION_ICHOR_THRESHOLD) return null
      val atMilestone = rawScarHarvestForMass(milestoneMass.toDouble())
      val justBefore = rawScarHarvestForMass((milestoneMass - 1L).toDouble())
      return if (atMilestone == justBefore && atMilestone > 0L) atMilestone + 1L else null
    }

  private fun scarHarvestForMass(mass: Double): Long {
    val raw = rawScarHarvestForMass(mass)
    if (raw == 0L) return 0L
    val pulled = milestonePulledScarCount
    val milestoneMass = nextExcisionMilestone?.massThreshold
    if (
      pulled != null &&
        milestoneMass != null &&
        mass >= milestoneMass.toDouble() &&
        raw == pulled - 1L
    ) {
      return pulled
    }
    return raw
  }

  fun massRequiredForScarCount(targetScars: Long): Long {
    if (targetScars <= 0L) return 0L
    if (targetScars <= minExcisionScarHarvest) return OrganBalance.EXCISION_ICHOR_THRESHOLD
    val pulled = milestonePulledScarCount
    val milestoneMass = nextExcisionMilestone?.massThreshold
    if (pulled != null && milestoneMass != null && targetScars == pulled) {
      return milestoneMass
    }
    val denom = OrganBalance.SCAR_CURVE_BASE_SCALE * scarHarvestMultiplier
    val ratio = (targetScars.toDouble() / denom).pow(1.0 / OrganBalance.SCAR_CURVE_EXPONENT)
    var candidate =
      max(
        OrganBalance.EXCISION_ICHOR_THRESHOLD,
        ceil(OrganBalance.EXCISION_ICHOR_THRESHOLD * ratio).toLong(),
      )
    while (rawScarHarvestForMass(candidate.toDouble()) < targetScars) {
      candidate += 1L
    }
    return candidate
  }

  val currentBaseScarHarvest: Long
    get() = if (!canExcise) 0L else scarHarvestForMass(cycleLifetimeIchor)

  val nextBaseScarMassThreshold: Long
    get() =
      if (!canExcise) OrganBalance.EXCISION_ICHOR_THRESHOLD
      else massRequiredForScarCount(currentBaseScarHarvest + 1L)

  val nextScarProgress: Float
    get() {
      if (!canExcise) {
        return (cycleLifetimeIchor / OrganBalance.EXCISION_ICHOR_THRESHOLD.toDouble())
          .toFloat()
          .coerceIn(0f, 1f)
      }
      val prevMass =
        if (currentBaseScarHarvest <= minExcisionScarHarvest) {
          OrganBalance.EXCISION_ICHOR_THRESHOLD
        } else {
          massRequiredForScarCount(currentBaseScarHarvest)
        }
      val nextMass = nextBaseScarMassThreshold
      val span = max(1L, nextMass - prevMass).toDouble()
      return ((cycleLifetimeIchor - prevMass.toDouble()) / span).toFloat().coerceIn(0f, 1f)
    }

  /**
   * Total Scar Tissue granted if excising right now: smooth harvest curve plus any first-clear
   * milestone bonus on the next sequential Excision (+1 on First Severance -> 3 Scars at 210k).
   */
  val pendingScarTissue: Long
    get() {
      if (!canExcise) return 0L
      val milestoneBonus =
        if (canClearNextMilestone) nextExcisionMilestone?.firstClearScarBonus ?: 0L else 0L
      return currentBaseScarHarvest + milestoneBonus
    }
}

object OrganBalance {
  const val STILLNESS_IDLE_DELAY_SEC: Double = 3.0
  const val SHORTER_SILENCE_DELAY_SEC: Double = 1.5
  const val CATATONIC_TIDE_DELAY_SEC: Double = 0.8
  const val LAMP_STILLNESS_PER_SEC: Double = 0.45
  const val GAZE_DURATION_SEC: Double = 12.0
  const val GAZE_PASSIVE_MULTIPLIER: Double = 1.6
  const val YESTERDAYS_DUST_TAP_BONUS: Double = 3.0
  const val UNSPENT_BREATH_MULTIPLIER: Double = 1.75
  const val ALMOST_A_NAME_MULTIPLIER: Double = 1.8
  const val SALT_GROUT_BONUS: Double = 0.45
  const val BORROWED_PULSE_BONUS: Double = 0.55
  const val UNCOUNTED_HOUR_BONUS: Double = 0.75
  const val WAX_STILLNESS_COST_MULTIPLIER: Double = 0.70
  const val WAX_GRAFT_COOLDOWN_MULTIPLIER: Double = 0.50
  const val GRAFT_SUTURE_COST: Int = 2
  const val GRAFT_LEECH_COST: Int = 2
  const val GRAFT_BASE_STILLNESS_COST: Double = 35.0
  const val GRAFT_STILLNESS_GROWTH: Double = 1.55
  const val GRAFT_COOLDOWN_SEC: Double = 6.0
  const val GRAFT_ICHOR_PER_SEC: Double = 18.0
  const val GRAFT_STILLNESS_PER_SEC: Double = 0.7
  const val CHOIR_LUNG_LAMP_COST: Int = 2
  const val CHOIR_LUNG_TENDRIL_COST: Int = 1
  const val CHOIR_LUNG_BASE_STILLNESS_COST: Double = 180.0
  const val CHOIR_LUNG_STILLNESS_GROWTH: Double = 1.65
  const val CHOIR_LUNG_COOLDOWN_SEC: Double = 10.0
  const val CHOIR_LUNG_ICHOR_PER_SEC: Double = 160.0
  const val CHOIR_LUNG_STILLNESS_PER_SEC: Double = 3.5
  const val EXCISION_ICHOR_THRESHOLD: Long = 210_000L
  const val SCAR_CURVE_BASE_SCALE: Double = 2.0
  const val SCAR_CURVE_EXPONENT: Double = 0.30
  const val MILESTONE_ICHOR_RESONANCE: Double = 0.20
  const val MILESTONE_STILLNESS_RESONANCE: Double = 0.15
  const val SCAR_RESONANCE_SQRT_FACTOR: Double = 0.25
  const val SCAR_STILLNESS_SQRT_FACTOR: Double = 0.18
  const val KELOID_ICHOR_PER_RANK: Double = 0.18
  const val SEDIMENT_STILLNESS_PER_RANK: Double = 0.16
  const val SEDIMENT_WELL_COST_FACTOR: Double = 0.96
  const val SEDIMENT_WELL_COST_FLOOR: Double = 0.68
  const val SCAR_HARVEST_HOOK_PER_RANK: Double = 0.12
  const val PERPETUAL_SEAM_INTERVAL_SEC: Double = 0.75
  const val FIRST_ANOMALY_DELAY_SEC: Double = 14.0
  const val ANOMALY_SPAWN_INTERVAL_SEC: Double = 26.0
  const val SOFTENED_MORTAR_SPEED_MULT: Double = 1.6
  const val MAX_ACTIVE_ANOMALIES: Int = 3
  const val TOOTH_BASE_ICHOR_REWARD: Double = 25.0
  const val NODULE_BASE_ICHOR_REWARD: Double = 220.0
  const val NODULE_BASE_STILLNESS_REWARD: Double = 6.0
  const val RIB_BASE_ICHOR_REWARD: Double = 1_400.0
  const val RIB_BASE_STILLNESS_REWARD: Double = 14.0
  const val RIB_GAZE_DURATION_SEC: Double = 6.0
  const val ECHO_PULSE_INTERVAL_SEC: Double = 12.0
  const val ECHO_OSSUARY_INTERVAL_SEC: Double = 9.0
  const val VALVE_CLAMP_DURATION_SEC: Double = 20.0
  const val BLOOM_INCISION_CLAMP_DURATION_SEC: Double = 15.0
  const val MARROW_BLOOM_DURATION_SEC: Double = 25.0
  const val BLOOM_INCISION_DURATION_SEC: Double = 32.0
  const val MARROW_BLOOM_MULTIPLIER: Double = 2.2
  const val BLOOM_INCISION_MULTIPLIER: Double = 3.1
}
