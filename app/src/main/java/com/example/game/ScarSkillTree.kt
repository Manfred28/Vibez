package com.example.game

/** Top-level narrative & mechanical arc active in the unified engine. */
enum class GameArc(val id: String) {
  ARC_1_CHAMBER("arc_1"),
  ARC_2_INVESTIGATION("arc_2"),
  ARC_3_CONVERGENCE("arc_3"),
}

/**
 * Six distinct Excision milestones spanning Arc 1. Each milestone requires permanent Scar Tissue
 * surgical upgrades to breach cleanly, advances strictly +1 per Excision, and awards zero free
 * bonus Scars so every Scar is earned along the smooth harvest curve.
 */
enum class ExcisionMilestone(
  val number: Int,
  val massThreshold: Long,
  val firstClearScarBonus: Long,
  val code: String,
) {
  FIRST_SEVERANCE(
    number = 1,
    massThreshold = 210_000L,
    firstClearScarBonus = 0L,
    code = "EXCISION I // FIRST SEVERANCE",
  ),
  SUNKEN_DRAIN(
    number = 2,
    massThreshold = 1_800_000L,
    firstClearScarBonus = 0L,
    code = "EXCISION II // THE SUNKEN DRAIN",
  ),
  UNBRICKED_SEAM(
    number = 3,
    massThreshold = 16_000_000L,
    firstClearScarBonus = 0L,
    code = "EXCISION III // THE UNBRICKED SEAM",
  ),
  BREATH_HOLD(
    number = 4,
    massThreshold = 140_000_000L,
    firstClearScarBonus = 0L,
    code = "EXCISION IV // THE BREATH-HOLD",
  ),
  MEMBRANE_AUDIT(
    number = 5,
    massThreshold = 1_250_000_000L,
    firstClearScarBonus = 0L,
    code = "EXCISION V // THE MEMBRANE AUDIT",
  ),
  FINAL_SEVERANCE(
    number = 6,
    massThreshold = 12_000_000_000L,
    firstClearScarBonus = 0L,
    code = "EXCISION VI // PLEAD FOR SALVATION",
  );

  companion object {
    fun byNumber(number: Int): ExcisionMilestone? = entries.firstOrNull { it.number == number }

    fun nextAfter(highestCleared: Int): ExcisionMilestone? = byNumber(highestCleared + 1)
  }
}

enum class ScarBranch(val id: String) {
  ROOT("root"),
  SUTURE("suture"),
  STILLNESS("stillness"),
  MARROW("marrow"),
}

/**
 * Expanded 5-tier branching Scar Tissue surgical skill tree unlocked after Excision I.
 * - SUTURE: compresses repeat runs by eliminating early-run friction and automating the valve.
 * - STILLNESS: deepens depressive idle mechanics, lowers Stillness costs & graft cooldowns, and amplifies Lungs & Jars.
 * - MARROW: supercharges wall anomalies, automatic grafting, Drain/Echo/Bloom rites, and Dossier resonance.
 */
enum class ScarNodeId(
  val id: String,
  val branch: ScarBranch,
  val tier: Int,
  val cost: Long,
  val prerequisite: ScarNodeId?,
) {
  FIRST_INCISION(
    id = "first_incision",
    branch = ScarBranch.ROOT,
    tier = 0,
    cost = 1L,
    prerequisite = null,
  ),
  // SUTURE BRANCH (T1..T5)
  CALLOUSED_PALM(
    id = "calloused_palm",
    branch = ScarBranch.SUTURE,
    tier = 1,
    cost = 2L,
    prerequisite = FIRST_INCISION,
  ),
  TWIN_STITCH(
    id = "twin_stitch",
    branch = ScarBranch.SUTURE,
    tier = 2,
    cost = 5L,
    prerequisite = CALLOUSED_PALM,
  ),
  COLONY_INERTIA(
    id = "colony_inertia",
    branch = ScarBranch.SUTURE,
    tier = 3,
    cost = 11L,
    prerequisite = TWIN_STITCH,
  ),
  MEMBRANE_RECALL(
    id = "membrane_recall",
    branch = ScarBranch.SUTURE,
    tier = 4,
    cost = 22L,
    prerequisite = COLONY_INERTIA,
  ),
  PERPETUAL_SEAM(
    id = "perpetual_seam",
    branch = ScarBranch.SUTURE,
    tier = 5,
    cost = 45L,
    prerequisite = MEMBRANE_RECALL,
  ),
  // STILLNESS BRANCH (T1..T5)
  SHORTER_SILENCE(
    id = "shorter_silence",
    branch = ScarBranch.STILLNESS,
    tier = 1,
    cost = 2L,
    prerequisite = FIRST_INCISION,
  ),
  WAX_SEALED_MEMORY(
    id = "wax_sealed_memory",
    branch = ScarBranch.STILLNESS,
    tier = 2,
    cost = 5L,
    prerequisite = SHORTER_SILENCE,
  ),
  DEEP_WEEPING(
    id = "deep_weeping",
    branch = ScarBranch.STILLNESS,
    tier = 3,
    cost = 11L,
    prerequisite = WAX_SEALED_MEMORY,
  ),
  HOLLOW_LUNG_CHOIR(
    id = "hollow_lung_choir",
    branch = ScarBranch.STILLNESS,
    tier = 4,
    cost = 22L,
    prerequisite = DEEP_WEEPING,
  ),
  CATATONIC_TIDE(
    id = "catatonic_tide",
    branch = ScarBranch.STILLNESS,
    tier = 5,
    cost = 45L,
    prerequisite = HOLLOW_LUNG_CHOIR,
  ),
  // MARROW BRANCH (T1..T5)
  SOFTENED_MORTAR(
    id = "softened_mortar",
    branch = ScarBranch.MARROW,
    tier = 1,
    cost = 2L,
    prerequisite = FIRST_INCISION,
  ),
  AUTO_TENDRIL_SUTURE(
    id = "auto_tendril_suture",
    branch = ScarBranch.MARROW,
    tier = 2,
    cost = 6L,
    prerequisite = SOFTENED_MORTAR,
  ),
  ECHO_OSSUARY(
    id = "echo_ossuary",
    branch = ScarBranch.MARROW,
    tier = 3,
    cost = 12L,
    prerequisite = AUTO_TENDRIL_SUTURE,
  ),
  BLOOM_INCISION(
    id = "bloom_incision",
    branch = ScarBranch.MARROW,
    tier = 4,
    cost = 24L,
    prerequisite = ECHO_OSSUARY,
  ),
  BONE_RESONANCE(
    id = "bone_resonance",
    branch = ScarBranch.MARROW,
    tier = 5,
    cost = 48L,
    prerequisite = BLOOM_INCISION,
  );

  companion object {
    fun byId(id: String): ScarNodeId? = entries.firstOrNull { it.id == id }
  }
}

/**
 * Repeatable Scar Tissue sinks unlocked once [ScarNodeId.FIRST_INCISION] is sutured.
 * Gives every harvest cycle a permanent outlet and directly rewards Scar-farming runs.
 */
enum class RepeatableScarGraft(
  val id: String,
  val baseCost: Long,
  val costGrowth: Double,
) {
  KELOID_THICKENING(
    id = "keloid_thickening",
    baseCost = 3L,
    costGrowth = 1.45,
  ),
  SEDIMENT_WELL(
    id = "sediment_well",
    baseCost = 3L,
    costGrowth = 1.45,
  ),
  SCAR_HARVEST_HOOK(
    id = "scar_harvest_hook",
    baseCost = 4L,
    costGrowth = 1.55,
  );

  companion object {
    fun byId(id: String): RepeatableScarGraft? = entries.firstOrNull { it.id == id }
  }
}

/** Water-damaged relics dredged from the Floor Drain Grate (unlocked at Excision II). */
enum class DredgedRelic(
  val id: String,
  val ichorMultiplier: Double,
  val stillnessMultiplier: Double,
) {
  PATIENT_WRISTBAND(
    id = "patient_wristband",
    ichorMultiplier = 1.28,
    stillnessMultiplier = 1.12,
  ),
  BRASS_WARD_KEY(
    id = "brass_ward_key",
    ichorMultiplier = 1.35,
    stillnessMultiplier = 1.18,
  ),
  BLEEDING_LOGBOOK(
    id = "bleeding_logbook",
    ichorMultiplier = 1.45,
    stillnessMultiplier = 1.25,
  );

  companion object {
    fun byId(id: String): DredgedRelic? = entries.firstOrNull { it.id == id }
  }
}

/**
 * Redacted institutional lines of "Rite of the Quiet Vessel" permanently unsealed by feeding
 * organism colonies into The Ossified Throat (unlocked at Excision V).
 */
enum class CultDossierLine(
  val id: String,
  val lineNumber: Int,
  val siphonCost: Int,
  val tendrilCost: Int,
  val requiresJar: Boolean,
  val ichorMultiplier: Double,
) {
  LINE_1_HYDRO_WARD(
    id = "dossier_line_1",
    lineNumber = 1,
    siphonCost = 2,
    tendrilCost = 0,
    requiresJar = false,
    ichorMultiplier = 1.35,
  ),
  LINE_2_MEMORY_BRINE(
    id = "dossier_line_2",
    lineNumber = 2,
    siphonCost = 3,
    tendrilCost = 1,
    requiresJar = true,
    ichorMultiplier = 1.40,
  ),
  LINE_3_BRICKED_INTAKE(
    id = "dossier_line_3",
    lineNumber = 3,
    siphonCost = 4,
    tendrilCost = 2,
    requiresJar = true,
    ichorMultiplier = 1.45,
  ),
  LINE_4_QUIET_VESSEL(
    id = "dossier_line_4",
    lineNumber = 4,
    siphonCost = 5,
    tendrilCost = 3,
    requiresJar = true,
    ichorMultiplier = 1.55,
  );

  companion object {
    fun byId(id: String): CultDossierLine? = entries.firstOrNull { it.id == id }
  }
}
