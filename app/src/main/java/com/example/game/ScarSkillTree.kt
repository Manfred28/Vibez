package com.example.game

/** Top-level narrative & mechanical arc active in the unified engine. */
enum class GameArc(val id: String) {
  ARC_1_CHAMBER("arc_1"),
  ARC_2_INVESTIGATION("arc_2"),
  ARC_3_CONVERGENCE("arc_3"),
}

/**
 * Six distinct Excision milestones spanning Arc 1. Each milestone requires permanent Scar Tissue
 * surgical upgrades to breach cleanly and unlocks one unmistakable chamber feature + narrative beat.
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
    firstClearScarBonus = 2L,
    code = "EXCISION I // FIRST SEVERANCE",
  ),
  SUNKEN_DRAIN(
    number = 2,
    massThreshold = 1_200_000L,
    firstClearScarBonus = 5L,
    code = "EXCISION II // THE SUNKEN DRAIN",
  ),
  UNBRICKED_SEAM(
    number = 3,
    massThreshold = 8_500_000L,
    firstClearScarBonus = 9L,
    code = "EXCISION III // THE UNBRICKED SEAM",
  ),
  BREATH_HOLD(
    number = 4,
    massThreshold = 60_000_000L,
    firstClearScarBonus = 15L,
    code = "EXCISION IV // THE BREATH-HOLD",
  ),
  MEMBRANE_AUDIT(
    number = 5,
    massThreshold = 450_000_000L,
    firstClearScarBonus = 24L,
    code = "EXCISION V // THE MEMBRANE AUDIT",
  ),
  FINAL_SEVERANCE(
    number = 6,
    massThreshold = 3_500_000_000L,
    firstClearScarBonus = 40L,
    code = "EXCISION VI // THE FINAL SEVERANCE",
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
 * Branching Scar Tissue surgical skill tree unlocked after Excision I.
 * - SUTURE: compresses repeat runs toward 5-7 min by eliminating early-run friction.
 * - STILLNESS: deepens depressive idle mechanics, lowers Stillness costs & graft cooldowns, and amplifies Specimen Jars.
 * - MARROW: supercharges wall anomalies, automatic grafting, and high-tier chamber rites.
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
    cost = 12L,
    prerequisite = TWIN_STITCH,
  ),
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
    cost = 12L,
    prerequisite = WAX_SEALED_MEMORY,
  ),
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
  BONE_RESONANCE(
    id = "bone_resonance",
    branch = ScarBranch.MARROW,
    tier = 3,
    cost = 14L,
    prerequisite = AUTO_TENDRIL_SUTURE,
  );

  companion object {
    fun byId(id: String): ScarNodeId? = entries.firstOrNull { it.id == id }
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
    ichorMultiplier = 1.45,
    stillnessMultiplier = 1.15,
  ),
  BRASS_WARD_KEY(
    id = "brass_ward_key",
    ichorMultiplier = 1.65,
    stillnessMultiplier = 1.25,
  ),
  BLEEDING_LOGBOOK(
    id = "bleeding_logbook",
    ichorMultiplier = 1.90,
    stillnessMultiplier = 1.40,
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
    ichorMultiplier = 2.4,
  ),
  LINE_2_MEMORY_BRINE(
    id = "dossier_line_2",
    lineNumber = 2,
    siphonCost = 3,
    tendrilCost = 1,
    requiresJar = true,
    ichorMultiplier = 2.6,
  ),
  LINE_3_BRICKED_INTAKE(
    id = "dossier_line_3",
    lineNumber = 3,
    siphonCost = 4,
    tendrilCost = 2,
    requiresJar = true,
    ichorMultiplier = 2.8,
  ),
  LINE_4_QUIET_VESSEL(
    id = "dossier_line_4",
    lineNumber = 4,
    siphonCost = 5,
    tendrilCost = 3,
    requiresJar = true,
    ichorMultiplier = 3.2,
  );

  companion object {
    fun byId(id: String): CultDossierLine? = entries.firstOrNull { it.id == id }
  }
}
