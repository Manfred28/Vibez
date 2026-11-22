package com.example.data

/**
 * Escalation progression curve for Direction 1 ("The Tending of the Unnamed Organ"), aligned with
 * `/GAME_DESIGN_BIBLE.md` and `app/src/main/assets/dialogues.json`.
 */
object EscalationCurve {
  val phaseThresholds: List<Long> = listOf(0L, 25L, 150L, 600L, 2_000L)

  fun phaseForCount(count: Long): Int {
    val safeCount = count.coerceAtLeast(0L)
    var resolved = 0
    for (index in phaseThresholds.indices) {
      if (safeCount >= phaseThresholds[index]) {
        resolved = index
      }
    }
    return resolved
  }

  fun nextThresholdForPhase(phase: Int): Long? {
    val nextPhase = phase.coerceIn(0, phaseThresholds.lastIndex) + 1
    return phaseThresholds.getOrNull(nextPhase)
  }

  fun progressWithinPhase(count: Long): Float {
    val safeCount = count.coerceAtLeast(0L)
    val phase = phaseForCount(safeCount)
    val nextThreshold = nextThresholdForPhase(phase) ?: return 1f
    val startThreshold = phaseThresholds[phase]
    val span = (nextThreshold - startThreshold).coerceAtLeast(1L)
    return ((safeCount - startThreshold).toFloat() / span.toFloat()).coerceIn(0f, 1f)
  }
}

data class PhaseMetadata(val id: Int, val name: String, val intensity: String)

data class DialogueTrigger(val type: String, val threshold: Long, val target: String? = null)

data class DialogueEntry(
  val id: String,
  val phase: Int,
  val mood: String,
  val trigger: DialogueTrigger,
  val once: Boolean,
  val speaker: String,
  val lines: List<String>,
)

data class DialogueCatalog(
  val schemaVersion: Int,
  val phases: List<PhaseMetadata>,
  val entries: List<DialogueEntry>,
) {
  fun phaseName(phaseId: Int): String? = phases.firstOrNull { it.id == phaseId }?.name

  companion object {
    val Empty = DialogueCatalog(schemaVersion = 1, phases = emptyList(), entries = emptyList())
  }
}
