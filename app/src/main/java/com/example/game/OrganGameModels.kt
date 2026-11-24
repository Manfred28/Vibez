package com.example.game

import com.example.data.DialogueEntry
import com.example.data.EscalationCurve
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
    baseIchorPerSec = 140.0,
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

/**
 * Unified immutable state spanning Arc 1 (Chamber + 6 Excisions + Surgical Scar Tree),
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
  // Spendable Scar Tissue available for suturing Surgical Skill Tree nodes
  val availableScarTissue: Long = 0L,
  val highestExcisionMilestone: Int = 0,
  val unlockedScarNodes: Set<ScarNodeId> = emptySet(),
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

  fun creatureCount(type: CreatureType): Int = creatures[type] ?: 0

  fun isCreatureUnlocked(type: CreatureType): Boolean {
    if (highestExcisionMilestone < type.requiredMilestone) return false
    return cycle > 0 ||
      cycleLifetimeIchorDisplay >= type.unlockIchorThreshold ||
      creatureCount(type) > 0
  }

  fun creatureCost(type: CreatureType): Long {
    val owned = creatureCount(type)
    val growth =
      if (hasNode(ScarNodeId.COLONY_INERTIA)) max(1.14, type.costGrowth - 0.05)
      else type.costGrowth
    return floor(type.baseCost * growth.pow(owned)).toLong()
  }

  val stillnessIdleDelaySec: Double
    get() =
      if (hasNode(ScarNodeId.SHORTER_SILENCE)) OrganBalance.SHORTER_SILENCE_DELAY_SEC
      else OrganBalance.STILLNESS_IDLE_DELAY_SEC

  val boneResonanceBonus: Double
    get() = if (hasNode(ScarNodeId.BONE_RESONANCE)) 2.5 else 1.0

  val jarEffectBoost: Double
    get() = if (hasNode(ScarNodeId.WAX_SEALED_MEMORY)) 1.6 else 1.0

  val stillnessCostMultiplier: Double
    get() =
      if (hasNode(ScarNodeId.WAX_SEALED_MEMORY)) OrganBalance.WAX_STILLNESS_COST_MULTIPLIER
      else 1.0

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

  /**
   * Permanent Scar multiplier combining baseline milestone/scar resonance with Surgical Tree
   * core nodes. Equals 1.0 in fresh Cycle 0 so initial 15-minute pacing stays exact.
   */
  val scarMultiplier: Double
    get() {
      val baseResonance =
        1.0 +
          highestExcisionMilestone * OrganBalance.MILESTONE_ICHOR_RESONANCE +
          scarTissue * OrganBalance.SCAR_RESONANCE_PER_UNIT
      val rootMult = if (hasNode(ScarNodeId.FIRST_INCISION)) 1.5 else 1.0
      val boneMult = if (hasNode(ScarNodeId.BONE_RESONANCE)) 4.0 else 1.0
      return baseResonance * rootMult * boneMult
    }

  val scarStillnessMultiplier: Double
    get() {
      val baseResonance =
        1.0 +
          highestExcisionMilestone * OrganBalance.MILESTONE_STILLNESS_RESONANCE +
          scarTissue * OrganBalance.SCAR_STILLNESS_RESONANCE_PER_UNIT
      val rootMult = if (hasNode(ScarNodeId.FIRST_INCISION)) 1.25 else 1.0
      val silenceMult = if (hasNode(ScarNodeId.SHORTER_SILENCE)) 1.6 else 1.0
      return baseResonance * rootMult * silenceMult
    }

  val relicIchorMultiplier: Double
    get() {
      var mult = 1.0
      dredgedRelics.forEach { relic ->
        val effective = 1.0 + (relic.ichorMultiplier - 1.0) * boneResonanceBonus
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
      val perLineBoost = if (hasNode(ScarNodeId.BONE_RESONANCE)) 1.35 else 1.0
      unredactedDossierLines.forEach { line -> mult *= line.ichorMultiplier * perLineBoost }
      return mult
    }

  val gazeMultiplier: Double
    get() = if (gazeRemainingSeconds > 0.0) OrganBalance.GAZE_PASSIVE_MULTIPLIER else 1.0

  val bloomMultiplier: Double
    get() =
      if (marrowBloomRemainingSeconds > 0.0) {
        if (hasNode(ScarNodeId.BONE_RESONANCE)) 3.5 else OrganBalance.MARROW_BLOOM_MULTIPLIER
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
      val mortarBoost = if (hasNode(ScarNodeId.SOFTENED_MORTAR)) 3.0 else 1.0
      val echoBoost =
        if (echoDiversionPercent > 0) {
          1.0 + (echoDiversionPercent / 25.0) * 0.75 * boneResonanceBonus
        } else {
          1.0
        }
      return jarBoost * mortarBoost * echoBoost
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
      val palmMult = if (hasNode(ScarNodeId.CALLOUSED_PALM)) 2.2 else 1.0
      val echoBurst = if (hasNode(ScarNodeId.TWIN_STITCH)) ichorPerSecond * 3.0 else 0.0
      return (1.0 + dustBonus + toothBonus) *
        scarMultiplier *
        relicIchorMultiplier *
        dossierIchorMultiplier *
        palmMult + echoBurst
    }

  val globalIchorMultiplier: Double
    get() =
      scarMultiplier *
        relicIchorMultiplier *
        dossierIchorMultiplier *
        gazeMultiplier *
        bloomMultiplier *
        uncountedHourMultiplier

  fun effectiveCreatureIchorPerSec(type: CreatureType): Double {
    val inertiaMult = if (hasNode(ScarNodeId.COLONY_INERTIA)) 3.2 else 1.0
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
          val twin = if (hasNode(ScarNodeId.TWIN_STITCH)) 2.5 else 1.0
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
              isStillnessActive -> 2.0
              hasNode(ScarNodeId.DEEP_WEEPING) -> 1.4
              else -> 1.0
            }
          val deepMult = if (hasNode(ScarNodeId.DEEP_WEEPING)) 3.5 else 1.0
          nameJar * weepingSurge * deepMult
        }
        CreatureType.SUMP_SIPHON -> {
          val autoMult = if (hasNode(ScarNodeId.AUTO_TENDRIL_SUTURE)) 3.0 else 1.0
          val sealPenalty = if (drainSealed) 0.85 else 1.0
          autoMult * sealPenalty
        }
      }
    return type.baseIchorPerSec *
      specimenMultiplier *
      inertiaMult *
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
      val graftNodeMult = if (hasNode(ScarNodeId.AUTO_TENDRIL_SUTURE)) 3.0 else 1.0
      val graftRate =
        graftedTendrils *
          OrganBalance.GRAFT_ICHOR_PER_SEC *
          graftNodeMult *
          borrowedPulseMultiplier *
          globalIchorMultiplier
      val deepLungMult = if (hasNode(ScarNodeId.DEEP_WEEPING)) 3.5 else 1.0
      val lungStillnessBoost = if (isStillnessActive) 2.0 else 1.0
      val lungRate =
        choirLungs *
          OrganBalance.CHOIR_LUNG_ICHOR_PER_SEC *
          deepLungMult *
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
          hasNode(ScarNodeId.DEEP_WEEPING) && creatureCount(CreatureType.WEEPING_LAMP) > 0 -> 0.40
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
      val lungStillness =
        choirLungs *
          OrganBalance.CHOIR_LUNG_STILLNESS_PER_SEC *
          scarStillnessMultiplier *
          relicStillnessMultiplier *
          gazeMultiplier *
          (if (isStillnessActive) 1.5 else 1.0)
      return (lampStillness + graftStillness + lungStillness) *
        borrowedPulseMultiplier *
        uncountedHourMultiplier
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

  /** True when the current run's mass breaches the next one-time Excision Milestone (I..VI). */
  val canClearNextMilestone: Boolean
    get() {
      val next = nextExcisionMilestone ?: return false
      return cycleLifetimeIchorDisplay >= next.massThreshold
    }

  /** True when Milestone VI threshold is reached or cleared, opening the Breach Ritual into Arc 2. */
  val canBreachToArc2: Boolean
    get() =
      highestExcisionMilestone >= 6 ||
        cycleLifetimeIchorDisplay >= ExcisionMilestone.FINAL_SEVERANCE.massThreshold

  val currentBaseScarHarvest: Long
    get() =
      if (!canExcise) 0L
      else
        floor(sqrt(cycleLifetimeIchor / OrganBalance.EXCISION_ICHOR_THRESHOLD.toDouble()))
          .toLong()
          .coerceAtLeast(1L)

  val nextBaseScarMassThreshold: Long
    get() {
      val nextBase = currentBaseScarHarvest + 1L
      return nextBase * nextBase * OrganBalance.EXCISION_ICHOR_THRESHOLD
    }

  val nextScarProgress: Float
    get() {
      val prevBase = currentBaseScarHarvest
      val prevMass = prevBase * prevBase * OrganBalance.EXCISION_ICHOR_THRESHOLD
      val nextMass = nextBaseScarMassThreshold
      val span = max(1L, nextMass - prevMass).toDouble()
      return ((cycleLifetimeIchor - prevMass.toDouble()) / span).toFloat().coerceIn(0f, 1f)
    }

  /** Total Scar Tissue granted if excising right now (base harvest + any newly cleared milestone bonuses). */
  val pendingScarTissue: Long
    get() {
      if (!canExcise) return 0L
      val baseHarvest = currentBaseScarHarvest
      val milestoneBonus =
        ExcisionMilestone.entries
          .filter {
            it.number > highestExcisionMilestone && cycleLifetimeIchorDisplay >= it.massThreshold
          }
          .sumOf { it.firstClearScarBonus }
      return baseHarvest + milestoneBonus
    }
}

object OrganBalance {
  const val STILLNESS_IDLE_DELAY_SEC: Double = 3.0
  const val SHORTER_SILENCE_DELAY_SEC: Double = 1.5
  const val LAMP_STILLNESS_PER_SEC: Double = 0.45
  const val GAZE_DURATION_SEC: Double = 12.0
  const val GAZE_PASSIVE_MULTIPLIER: Double = 1.6
  const val YESTERDAYS_DUST_TAP_BONUS: Double = 3.0
  const val UNSPENT_BREATH_MULTIPLIER: Double = 1.75
  const val ALMOST_A_NAME_MULTIPLIER: Double = 1.8
  const val SALT_GROUT_BONUS: Double = 0.45
  const val BORROWED_PULSE_BONUS: Double = 0.55
  const val UNCOUNTED_HOUR_BONUS: Double = 0.75
  const val WAX_STILLNESS_COST_MULTIPLIER: Double = 0.65
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
  const val CHOIR_LUNG_ICHOR_PER_SEC: Double = 260.0
  const val CHOIR_LUNG_STILLNESS_PER_SEC: Double = 4.5
  const val EXCISION_ICHOR_THRESHOLD: Long = 210_000L
  const val MILESTONE_ICHOR_RESONANCE: Double = 0.25
  const val MILESTONE_STILLNESS_RESONANCE: Double = 0.20
  const val SCAR_RESONANCE_PER_UNIT: Double = 0.15
  const val SCAR_STILLNESS_RESONANCE_PER_UNIT: Double = 0.10
  const val FIRST_ANOMALY_DELAY_SEC: Double = 14.0
  const val ANOMALY_SPAWN_INTERVAL_SEC: Double = 26.0
  const val MAX_ACTIVE_ANOMALIES: Int = 3
  const val TOOTH_BASE_ICHOR_REWARD: Double = 25.0
  const val NODULE_BASE_ICHOR_REWARD: Double = 220.0
  const val NODULE_BASE_STILLNESS_REWARD: Double = 6.0
  const val RIB_BASE_ICHOR_REWARD: Double = 1_400.0
  const val RIB_BASE_STILLNESS_REWARD: Double = 14.0
  const val RIB_GAZE_DURATION_SEC: Double = 6.0
  const val ECHO_PULSE_INTERVAL_SEC: Double = 12.0
  const val VALVE_CLAMP_DURATION_SEC: Double = 20.0
  const val MARROW_BLOOM_DURATION_SEC: Double = 25.0
  const val MARROW_BLOOM_MULTIPLIER: Double = 2.5
}
