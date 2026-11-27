package com.example.game

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Five irreversible Breach Strata for Arc 3 ("The Unmaking").
 * No resets: locking each Memory Wedge permanently secures the stratum and raises the Crimson Flood.
 */
enum class Arc3Stratum(
  val number: Int,
  val id: String,
  val title: String,
  val blurTitle: String,
  val fractureGoal: Double,
  val baseClotPerSec: Double,
  val baseFloodLevel: Float,
  val wedgeId: String,
  val wedgeTitle: String,
  val wedgeCaption: String,
) {
  BROKEN_GROUT(
    number = 1,
    id = "broken_grout",
    title = "BROKEN GROUT",
    blurTitle = "BROKEN GROUT",
    fractureGoal = 120.0,
    baseClotPerSec = 3.5,
    baseFloodLevel = 0.08f,
    wedgeId = "tax_plat_1998",
    wedgeTitle = "1998 TAX PLAT",
    wedgeCaption = "Folded parcel map jammed dry into the cracked tile seam above the valve.",
  ),
  UNSTITCHED_SEAM(
    number = 2,
    id = "unstitched_seam",
    title = "UNSTITCHED SEAM",
    blurTitle = "UNSTITCHED SEAM",
    fractureGoal = 750.0,
    baseClotPerSec = 16.0,
    baseFloodLevel = 0.24f,
    wedgeId = "second_chair_polaroid",
    wedgeTitle = "SECOND CHAIR POLAROID",
    wedgeCaption = "Forced deep into the torn scar ridge before warm plaster knits back shut.",
  ),
  SPLINTERED_RIBS(
    number = 3,
    id = "splintered_ribs",
    title = "SPLINTERED RIBS",
    blurTitle = "SPLINTERED RIBS",
    fractureGoal = 3_800.0,
    baseClotPerSec = 68.0,
    baseFloodLevel = 0.44f,
    wedgeId = "reel_four_waveform",
    wedgeTitle = "REEL 4 WAVEFORM",
    wedgeCaption = "Slack magnetic ribbon packed tight between cracked calcified ribs.",
  ),
  SHARED_PULSE(
    number = 4,
    id = "shared_pulse",
    title = "SHARED PULSE",
    blurTitle = "YOUR PULSE",
    fractureGoal = 18_000.0,
    baseClotPerSec = 260.0,
    baseFloodLevel = 0.68f,
    wedgeId = "drainage_loop_blueprint",
    wedgeTitle = "CLOSED DRAINAGE PLAT",
    wedgeCaption = "Soaked storm-main schematic braced behind the sheared flange bolts.",
  ),
  THE_OPEN_SEAM(
    number = 5,
    id = "the_open_seam",
    title = "THE OPEN SEAM",
    blurTitle = "OPEN VESSEL",
    fractureGoal = 75_000.0,
    baseClotPerSec = 820.0,
    baseFloodLevel = 0.86f,
    wedgeId = "file_zero_intake",
    wedgeTitle = "FILE #00 · SUBJECT INTAKE",
    wedgeCaption = "Your own badge sheet driven straight into the weeping doorway seam.",
  );

  companion object {
    fun byNumber(num: Int): Arc3Stratum = entries.firstOrNull { it.number == num } ?: THE_OPEN_SEAM

    fun byWedgeId(id: String): Arc3Stratum? = entries.firstOrNull { it.wedgeId == id }
  }
}

enum class Arc3UnmakeCategory(val id: String, val label: String) {
  SHATTER_JAR("shatter_jar", "SHATTERED JARS"),
  REVERSE_APPARATUS("reverse_apparatus", "REVERSED APPARATUS"),
  UNSTITCH_SCAR("unstitch_scar", "UNSTITCHED SEAMS"),
}

/**
 * 18 irreversible Unmaking acts that cannibalize the player's Arc 1 chamber to fuel active breach
 * power and weaken the room's clotting resistance.
 */
enum class Arc3UnmakeAct(
  val id: String,
  val category: Arc3UnmakeCategory,
  val minStratum: Int,
  val title: String,
  val description: String,
  val hemorrhageCost: Double,
  val wrenchMultiplier: Double = 1.0,
  val ruptureMultiplier: Double = 1.0,
  val antiClotFraction: Double = 0.0,
  val bleedPressurePerSec: Double = 0.0,
  val instantFractureSurge: Double = 0.0,
  val linkedJar: SpecimenJar? = null,
  val linkedCreature: CreatureType? = null,
  val linkedScarNode: ScarNodeId? = null,
) {
  // --- 6 Specimen Jar Shatters (permanent anti-clot + Fracture burst + shelf breakage) ---
  SHATTER_YESTERDAYS_DUST(
    id = "shatter_yesterdays_dust",
    category = Arc3UnmakeCategory.SHATTER_JAR,
    minStratum = 1,
    title = "SHATTER: YESTERDAY'S DUST",
    description = "Break the first wax seal on the baseboard; spilled brine weakens clotting.",
    hemorrhageCost = 18.0,
    wrenchMultiplier = 1.25,
    antiClotFraction = 0.06,
    instantFractureSurge = 24.0,
    linkedJar = SpecimenJar.YESTERDAYS_DUST,
  ),
  SHATTER_UNSPENT_BREATH(
    id = "shatter_unspent_breath",
    category = Arc3UnmakeCategory.SHATTER_JAR,
    minStratum = 1,
    title = "SHATTER: UNSPENT BREATH",
    description = "Crush the fogged jar against the grout; cold pressure jolts the valve loose.",
    hemorrhageCost = 52.0,
    wrenchMultiplier = 1.25,
    antiClotFraction = 0.06,
    instantFractureSurge = 40.0,
    linkedJar = SpecimenJar.UNSPENT_BREATH,
  ),
  SHATTER_ALMOST_A_NAME(
    id = "shatter_almost_a_name",
    category = Arc3UnmakeCategory.SHATTER_JAR,
    minStratum = 1,
    title = "SHATTER: ALMOST A NAME",
    description = "Smash the unlabeled glass; warm mortar flinches away from the shards.",
    hemorrhageCost = 115.0,
    wrenchMultiplier = 1.30,
    antiClotFraction = 0.06,
    instantFractureSurge = 65.0,
    linkedJar = SpecimenJar.ALMOST_A_NAME,
  ),
  SHATTER_SALT_GROUT(
    id = "shatter_salt_grout",
    category = Arc3UnmakeCategory.SHATTER_JAR,
    minStratum = 2,
    title = "SHATTER: SALT FROM THE GROUT",
    description = "Salt-burn the weeping seams so clots tear open cleaner and slower.",
    hemorrhageCost = 420.0,
    wrenchMultiplier = 1.32,
    antiClotFraction = 0.06,
    instantFractureSurge = 190.0,
    linkedJar = SpecimenJar.SALT_FROM_THE_GROUT,
  ),
  SHATTER_BORROWED_PULSE(
    id = "shatter_borrowed_pulse",
    category = Arc3UnmakeCategory.SHATTER_JAR,
    minStratum = 2,
    title = "SHATTER: A BORROWED PULSE",
    description = "Drop the pulsing vessel onto tile; its double-beat floods your wrenching.",
    hemorrhageCost = 1_150.0,
    wrenchMultiplier = 1.35,
    antiClotFraction = 0.06,
    instantFractureSurge = 310.0,
    linkedJar = SpecimenJar.A_BORROWED_PULSE,
  ),
  SHATTER_UNCOUNTED_HOUR(
    id = "shatter_uncounted_hour",
    category = Arc3UnmakeCategory.SHATTER_JAR,
    minStratum = 3,
    title = "SHATTER: THE UNCOUNTED HOUR",
    description = "Break the tallest seal on the shelf; stillness drains from the chamber forever.",
    hemorrhageCost = 4_200.0,
    wrenchMultiplier = 1.40,
    antiClotFraction = 0.06,
    instantFractureSurge = 1_100.0,
    linkedJar = SpecimenJar.THE_UNCOUNTED_HOUR,
  ),

  // --- 6 Reversed Organism Apparatuses (wrench scaling + sub-clot bleed pressure) ---
  REVERSE_SUTURE_HANDS(
    id = "reverse_suture_hands",
    category = Arc3UnmakeCategory.REVERSE_APPARATUS,
    minStratum = 1,
    title = "REVERSE SUTURE-HANDS",
    description = "Force the stitched fingers to claw outward at the mortar instead of turning clockwise.",
    hemorrhageCost = 35.0,
    wrenchMultiplier = 1.45,
    bleedPressurePerSec = 1.4,
    linkedCreature = CreatureType.SUTURE_HAND,
  ),
  REVERSE_LEECH_CLERKS(
    id = "reverse_leech_clerks",
    category = Arc3UnmakeCategory.REVERSE_APPARATUS,
    minStratum = 2,
    title = "SPLIT LEECH-CLERK GLASS",
    description = "Crack the tally bezel; blind mouths chew straight through the wet plaster.",
    hemorrhageCost = 260.0,
    wrenchMultiplier = 1.50,
    bleedPressurePerSec = 6.5,
    linkedCreature = CreatureType.LEECH_CLERK,
  ),
  BURST_WEEPING_LAMPS(
    id = "burst_weeping_lamps",
    category = Arc3UnmakeCategory.REVERSE_APPARATUS,
    minStratum = 2,
    title = "BURST WEEPING LAMPS",
    description = "Overload the ceiling glass until dark oil and sparks rain over the seams.",
    hemorrhageCost = 780.0,
    wrenchMultiplier = 1.55,
    bleedPressurePerSec = 10.0,
    linkedCreature = CreatureType.WEEPING_LAMP,
  ),
  BACKFLOOD_SUMP_SIPHONS(
    id = "backflood_sump_siphons",
    category = Arc3UnmakeCategory.REVERSE_APPARATUS,
    minStratum = 3,
    title = "BACK-FLOOD SUMP-SIPHONS",
    description = "Invert the leather bladders to pump crimson runoff backward beneath the tiles.",
    hemorrhageCost = 2_400.0,
    wrenchMultiplier = 1.65,
    bleedPressurePerSec = 28.0,
    linkedCreature = CreatureType.SUMP_SIPHON,
  ),
  RIP_CHOIR_TENDRILS(
    id = "rip_choir_tendrils",
    category = Arc3UnmakeCategory.REVERSE_APPARATUS,
    minStratum = 3,
    title = "TEAR CHOIR-TENDRILS",
    description = "Unbraid the hooked sinews from the valve rim and drag them against the ribs.",
    hemorrhageCost = 6_800.0,
    wrenchMultiplier = 1.70,
    bleedPressurePerSec = 42.0,
  ),
  RUPTURE_CHOIR_LUNGS(
    id = "rupture_choir_lungs",
    category = Arc3UnmakeCategory.REVERSE_APPARATUS,
    minStratum = 4,
    title = "OVER-INFLATE CHOIR-LUNGS",
    description = "Force every pleural graft past bursting until the calcified arches crack outward.",
    hemorrhageCost = 21_000.0,
    wrenchMultiplier = 1.85,
    bleedPressurePerSec = 120.0,
  ),

  // --- 6 Unstitched Scar Seams & Broken Rites (bleeding wall gashes + heavy multipliers) ---
  UNSTITCH_FIRST_INCISION(
    id = "unstitch_first_incision",
    category = Arc3UnmakeCategory.UNSTITCH_SCAR,
    minStratum = 1,
    title = "UNSTITCH: FIRST INCISION",
    description = "Rip open the root suture across the chamber wall; refuse to let it close again.",
    hemorrhageCost = 85.0,
    wrenchMultiplier = 1.60,
    ruptureMultiplier = 1.35,
    linkedScarNode = ScarNodeId.FIRST_INCISION,
  ),
  UNSTITCH_SUTURE_LINE(
    id = "unstitch_suture_line",
    category = Arc3UnmakeCategory.UNSTITCH_SCAR,
    minStratum = 2,
    title = "UNSTITCH: SUTURE BRANCH",
    description = "Pull the bone-thread loose from Calloused Palm to Perpetual Seam.",
    hemorrhageCost = 1_500.0,
    wrenchMultiplier = 1.75,
    ruptureMultiplier = 1.40,
    linkedScarNode = ScarNodeId.PERPETUAL_SEAM,
  ),
  UNSTITCH_STILLNESS_LINE(
    id = "unstitch_stillness_line",
    category = Arc3UnmakeCategory.UNSTITCH_SCAR,
    minStratum = 3,
    title = "UNSTITCH: STILLNESS BRANCH",
    description = "Tear apart the catatonic tide sutures so the room can no longer lull you quiet.",
    hemorrhageCost = 9_200.0,
    wrenchMultiplier = 1.80,
    ruptureMultiplier = 1.45,
    linkedScarNode = ScarNodeId.CATATONIC_TIDE,
  ),
  UNSTITCH_MARROW_LINE(
    id = "unstitch_marrow_line",
    category = Arc3UnmakeCategory.UNSTITCH_SCAR,
    minStratum = 4,
    title = "UNSTITCH: MARROW BRANCH",
    description = "Split the resonant bone ridges wide; your own thumb aches along the fault line.",
    hemorrhageCost = 38_000.0,
    wrenchMultiplier = 1.95,
    ruptureMultiplier = 1.55,
    linkedScarNode = ScarNodeId.BONE_RESONANCE,
  ),
  BREAK_FLOOR_GRATE_THROAT(
    id = "break_floor_grate_throat",
    category = Arc3UnmakeCategory.UNSTITCH_SCAR,
    minStratum = 4,
    title = "SPLIT GRATE & THROAT",
    description = "Pry the cast-iron floor grate out of the sump and tear through the ossified throat.",
    hemorrhageCost = 72_000.0,
    wrenchMultiplier = 2.05,
    bleedPressurePerSec = 150.0,
  ),
  SPLIT_BRICKED_JAMB(
    id = "split_bricked_jamb",
    category = Arc3UnmakeCategory.UNSTITCH_SCAR,
    minStratum = 5,
    title = "TEAR THE BRICKED FRAME",
    description = "Wrench the ancient mortar loose around the sealed doorway silhouette.",
    hemorrhageCost = 165_000.0,
    wrenchMultiplier = 2.40,
    ruptureMultiplier = 1.70,
    bleedPressurePerSec = 320.0,
  );

  companion object {
    fun byId(id: String): Arc3UnmakeAct? = entries.firstOrNull { it.id == id }
  }
}

/** Active urgent tears erupting across the chamber canvas in Arc 3. */
enum class BreachRuptureType(
  val id: String,
  val minStratum: Int,
  val actionLabel: String,
  val fractureWrenchMult: Double,
  val hemorrhageWrenchMult: Double,
  val clotFreezeSec: Double = 0.0,
  val ribForceSec: Double = 0.0,
  val frenzySec: Double = 0.0,
) {
  CLOTTING_SEAM(
    id = "clotting_seam",
    minStratum = 1,
    actionLabel = "RIP",
    fractureWrenchMult = 4.2,
    hemorrhageWrenchMult = 3.0,
    clotFreezeSec = 3.5,
  ),
  SPLINTERED_RIB(
    id = "splintered_rib",
    minStratum = 2,
    actionLabel = "SNAP",
    fractureWrenchMult = 3.2,
    hemorrhageWrenchMult = 6.5,
    ribForceSec = 4.5,
  ),
  TORN_SUTURE(
    id = "torn_suture",
    minStratum = 3,
    actionLabel = "TEAR",
    fractureWrenchMult = 4.8,
    hemorrhageWrenchMult = 4.5,
    clotFreezeSec = 6.0,
    frenzySec = 6.0,
  ),
  ARTERIAL_WELL(
    id = "arterial_well",
    minStratum = 4,
    actionLabel = "BREACH",
    fractureWrenchMult = 7.5,
    hemorrhageWrenchMult = 8.0,
    clotFreezeSec = 4.5,
  );

  companion object {
    fun byId(id: String): BreachRuptureType? = entries.firstOrNull { it.id == id }
  }
}

data class ActiveBreachRupture(
  val id: Long,
  val type: BreachRuptureType,
  val xFraction: Float,
  val yFraction: Float,
  val remainingSeconds: Double,
)

enum class Arc3DeckTab {
  UNMAKE,
  WEDGES,
}

object Arc3Balance {
  const val BASE_WRENCH_FRACTURE: Double = 4.5
  const val BASE_WRENCH_HEMORRHAGE: Double = 5.5
  const val MOMENTUM_GAIN_PER_TAP: Double = 0.12
  const val MOMENTUM_MAX_BONUS: Double = 0.80
  const val MOMENTUM_DECAY_DELAY_SEC: Double = 0.85
  const val MOMENTUM_DECAY_PER_SEC: Double = 0.36
  const val CLOT_GRACE_SEC: Double = 1.40
  const val CLOT_RAMP_SEC: Double = 1.50
  const val MAX_ANTI_CLOT_FRACTION: Double = 0.45
  const val RIB_FORCE_MULTIPLIER: Double = 1.50
  const val FRENZY_MULTIPLIER: Double = 2.00
  const val FIRST_RUPTURE_DELAY_SEC: Double = 5.0
  const val RUPTURE_SPAWN_INTERVAL_SEC: Double = 9.0
  const val RUPTURE_LIFETIME_SEC: Double = 16.0
  const val MAX_ACTIVE_RUPTURES: Int = 3
  const val FINAL_PULLS_TOTAL: Int = 5
  const val FINAL_PULL_COOLDOWN_SEC: Double = 1.8
}

/**
 * Isolated immutable state for Arc 3 ("The Unmaking").
 * Non-destructive: never mutates or erases Arc 1 Chamber or Arc 2 Investigator save data.
 */
data class Arc3BreachState(
  val currentStratum: Int = 1,
  val lockedWedges: Set<Arc3Stratum> = emptySet(),
  val fracture: Double = 0.0,
  val hemorrhage: Double = 0.0,
  val lifetimeHemorrhage: Double = 0.0,
  val wrenchCount: Int = 0,
  val momentumCharge: Double = 0.0,
  val secondsSinceLastAction: Double = 0.0,
  val clotFreezeRemainingSeconds: Double = 0.0,
  val ribForceRemainingSeconds: Double = 0.0,
  val frenzyRemainingSeconds: Double = 0.0,
  val completedUnmakes: Set<Arc3UnmakeAct> = emptySet(),
  val activeRuptures: List<ActiveBreachRupture> = emptyList(),
  val ruptureInteractions: Map<BreachRuptureType, Int> = emptyMap(),
  val secondsUntilNextRupture: Double = Arc3Balance.FIRST_RUPTURE_DELAY_SEC,
  val nextRuptureId: Long = 1L,
  val finalPullsCompleted: Int = 0,
  val finalPullCooldownSeconds: Double = 0.0,
  val submersionComplete: Boolean = false,
) {
  val activeStratum: Arc3Stratum
    get() = Arc3Stratum.byNumber(currentStratum)

  val isInFinale: Boolean
    get() = lockedWedges.size >= Arc3Stratum.entries.size

  val hemorrhageDisplay: Long
    get() = floor(hemorrhage).toLong()

  val fractureDisplay: Long
    get() = floor(fracture).toLong()

  val stratumProgress: Float
    get() =
      if (isInFinale) {
        (finalPullsCompleted.toFloat() / Arc3Balance.FINAL_PULLS_TOTAL.toFloat()).coerceIn(0f, 1f)
      } else {
        (fracture / activeStratum.fractureGoal).toFloat().coerceIn(0f, 1f)
      }

  val canDriveWedge: Boolean
    get() = !isInFinale && activeStratum !in lockedWedges && fracture >= activeStratum.fractureGoal

  val canPerformFinalPull: Boolean
    get() =
      isInFinale &&
        !submersionComplete &&
        finalPullsCompleted < Arc3Balance.FINAL_PULLS_TOTAL &&
        finalPullCooldownSeconds <= 0.0

  val shatteredJarCount: Int
    get() = completedUnmakes.count { it.category == Arc3UnmakeCategory.SHATTER_JAR }

  val reversedApparatusCount: Int
    get() = completedUnmakes.count { it.category == Arc3UnmakeCategory.REVERSE_APPARATUS }

  val unstitchedScarCount: Int
    get() = completedUnmakes.count { it.category == Arc3UnmakeCategory.UNSTITCH_SCAR }

  val totalRuptureInteractions: Int
    get() = ruptureInteractions.values.sum()

  fun isJarShattered(jar: SpecimenJar): Boolean =
    completedUnmakes.any { it.linkedJar == jar }

  val antiClotReduction: Double
    get() =
      completedUnmakes
        .sumOf { it.antiClotFraction }
        .coerceIn(0.0, Arc3Balance.MAX_ANTI_CLOT_FRACTION)

  val unmakeWrenchMultiplier: Double
    get() {
      var mult = 1.0
      completedUnmakes.forEach { mult *= it.wrenchMultiplier }
      return mult
    }

  val unmakeRuptureMultiplier: Double
    get() {
      var mult = 1.0
      completedUnmakes.forEach { mult *= it.ruptureMultiplier }
      return mult
    }

  val bleedPressurePerSec: Double
    get() = completedUnmakes.sumOf { it.bleedPressurePerSec }

  val isClotFrozen: Boolean
    get() = clotFreezeRemainingSeconds > 0.0 || frenzyRemainingSeconds > 0.0

  /** Current active clotting pull (Fracture/sec) after grace window, anti-clot jars, and bleed pressure. */
  val activeClotPerSec: Double
    get() {
      if (isInFinale || submersionComplete || isClotFrozen || fracture <= 0.0) return 0.0
      if (secondsSinceLastAction < Arc3Balance.CLOT_GRACE_SEC) return 0.0
      val ramp =
        ((secondsSinceLastAction - Arc3Balance.CLOT_GRACE_SEC) / Arc3Balance.CLOT_RAMP_SEC)
          .coerceIn(0.0, 1.0)
      val reducedBase = activeStratum.baseClotPerSec * (1.0 - antiClotReduction)
      val netClot = max(reducedBase * 0.25, reducedBase - bleedPressurePerSec * 0.45)
      return netClot * (0.35 + 0.65 * ramp)
    }

  val isActivelyClotting: Boolean
    get() = activeClotPerSec > 0.05 && fracture > 0.0

  val momentumMultiplier: Double
    get() = 1.0 + momentumCharge.coerceIn(0.0, 1.0) * Arc3Balance.MOMENTUM_MAX_BONUS

  val temporaryForceMultiplier: Double
    get() =
      (if (ribForceRemainingSeconds > 0.0) Arc3Balance.RIB_FORCE_MULTIPLIER else 1.0) *
        (if (frenzyRemainingSeconds > 0.0) Arc3Balance.FRENZY_MULTIPLIER else 1.0)

  /** Continuous 0..1 visual & audio flood level across Strata I..V and the 5 Final Pulls. */
  val floodLevel: Float
    get() {
      if (submersionComplete || finalPullsCompleted >= Arc3Balance.FINAL_PULLS_TOTAL) return 1.0f
      if (isInFinale) {
        return when (finalPullsCompleted) {
          0 -> 0.86f
          1 -> 0.90f
          2 -> 0.94f
          3 -> 0.97f
          4 -> 0.99f
          else -> 1.0f
        }
      }
      val prevFloor =
        if (lockedWedges.isEmpty()) 0.02f
        else Arc3Stratum.byNumber(lockedWedges.size).baseFloodLevel
      val targetFloor = activeStratum.baseFloodLevel
      val unmakeLift = (completedUnmakes.size / 18f) * 0.04f
      return min(0.86f, prevFloor + (targetFloor - prevFloor) * stratumProgress * 0.72f + unmakeLift)
    }

  /** Telemetry blur begins at Stratum IV (Shared Pulse) and saturates during Final Pulls. */
  val telemetryBlurFactor: Float
    get() =
      when {
        submersionComplete -> 1.0f
        isInFinale -> 0.85f + (finalPullsCompleted * 0.03f)
        currentStratum >= 5 -> 0.62f + stratumProgress * 0.23f
        currentStratum == 4 -> 0.25f + stratumProgress * 0.37f
        else -> 0f
      }.coerceIn(0f, 1f)

  val isValveSheared: Boolean
    get() = currentStratum >= 4 || isInFinale

  fun isUnmakeVisible(act: Arc3UnmakeAct): Boolean =
    act in completedUnmakes || act.minStratum <= currentStratum

  fun canPerformUnmake(act: Arc3UnmakeAct): Boolean =
    !isInFinale &&
      act !in completedUnmakes &&
      act.minStratum <= currentStratum &&
      hemorrhage >= act.hemorrhageCost
}

/** Derived legacy reflection bonuses from real Arc 1 & Arc 2 progress (safe fallback = 1.0x). */
fun OrganGameState.arc3LegacyHemorrhageMultiplier(): Double =
  1.0 + (ownedJars.size / 6.0) * 0.15 + (unlockedScarNodes.size / 16.0) * 0.15

fun OrganGameState.arc3LegacyFractureMultiplier(): Double =
  1.0 + arc2.discoveredCorroborations.size * 0.03

fun OrganGameState.arc3WrenchFractureYield(): Double =
  Arc3Balance.BASE_WRENCH_FRACTURE *
    arc3.unmakeWrenchMultiplier *
    arc3.momentumMultiplier *
    arc3.temporaryForceMultiplier *
    arc3LegacyFractureMultiplier()

fun OrganGameState.arc3WrenchHemorrhageYield(): Double =
  Arc3Balance.BASE_WRENCH_HEMORRHAGE *
    arc3.unmakeWrenchMultiplier *
    (1.0 + arc3.momentumCharge * 0.40) *
    arc3.temporaryForceMultiplier *
    arc3LegacyHemorrhageMultiplier()
