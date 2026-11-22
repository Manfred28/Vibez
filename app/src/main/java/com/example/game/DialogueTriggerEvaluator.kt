package com.example.game

import com.example.data.DialogueCatalog
import com.example.data.DialogueEntry

/**
 * Pure, zero-allocation-on-no-op evaluator for progress-bound narrative dialogue triggers.
 * Separated from [OrganGameEngine] for modularity and fast 10Hz tick evaluation.
 */
internal object DialogueTriggerEvaluator {

  fun collectNewlyTriggered(state: OrganGameState, catalog: DialogueCatalog): List<DialogueEntry> {
    val entries = catalog.entries
    if (entries.isEmpty()) return emptyList()
    var triggered: MutableList<DialogueEntry>? = null
    for (i in entries.indices) {
      val entry = entries[i]
      if ((!entry.once || entry.id !in state.triggeredDialogueIds) && isTriggerSatisfied(state, entry)) {
        if (triggered == null) {
          triggered = ArrayList(2)
        }
        triggered.add(entry)
      }
    }
    return triggered ?: emptyList()
  }

  fun isTriggerSatisfied(state: OrganGameState, entry: DialogueEntry): Boolean {
    val trigger = entry.trigger
    return when (trigger.type) {
      "counter_reached" -> state.cycleLifetimeIchorDisplay >= trigger.threshold
      "cycle_counter_reached" -> {
        val requiredCycle = trigger.target?.toIntOrNull() ?: 1
        state.cycle >= requiredCycle && state.cycleLifetimeIchorDisplay >= trigger.threshold
      }
      "creature_owned" ->
        when (trigger.target) {
          CreatureType.SUTURE_HAND.id ->
            state.creatureCount(CreatureType.SUTURE_HAND) >= trigger.threshold
          CreatureType.LEECH_CLERK.id ->
            state.creatureCount(CreatureType.LEECH_CLERK) >= trigger.threshold
          CreatureType.WEEPING_LAMP.id ->
            state.creatureCount(CreatureType.WEEPING_LAMP) >= trigger.threshold
          CreatureType.SUMP_SIPHON.id ->
            state.creatureCount(CreatureType.SUMP_SIPHON) >= trigger.threshold
          "grafted_tendril" -> state.graftedTendrils >= trigger.threshold
          "choir_lung" -> state.choirLungs >= trigger.threshold
          else -> false
        }
      "anomaly_interacted" -> {
        val anomalyType = WallAnomalyType.byId(trigger.target ?: "")
        anomalyType != null && (state.anomalyInteractions[anomalyType] ?: 0) >= trigger.threshold
      }
      "stillness_reached" -> state.cycleLifetimeStillness >= trigger.threshold.toDouble()
      "jar_bottled" -> state.ownedJars.size >= trigger.threshold
      "excision_completed" -> state.cycle >= trigger.threshold
      "excision_milestone_reached" -> state.highestExcisionMilestone >= trigger.threshold
      "scar_node_unlocked" -> state.unlockedScarNodes.size >= trigger.threshold
      "relic_dredged" -> state.lifetimeRelicsDredged >= trigger.threshold
      "echo_routed" -> state.echoPulsesTriggered >= trigger.threshold
      "marrow_bloom" -> state.marrowBloomCount >= trigger.threshold
      "dossier_line_unredacted" -> state.unredactedDossierLines.size >= trigger.threshold
      "arc_entered" -> {
        val targetArc =
          when (trigger.target) {
            GameArc.ARC_1_CHAMBER.id -> GameArc.ARC_1_CHAMBER
            GameArc.ARC_2_INVESTIGATION.id -> GameArc.ARC_2_INVESTIGATION
            GameArc.ARC_3_CONVERGENCE.id -> GameArc.ARC_3_CONVERGENCE
            else -> null
          }
        targetArc != null && state.activeArc == targetArc
      }
      "case_chapter_completed" -> state.arc2.completedChapters >= trigger.threshold
      "desk_anomaly_interacted" -> {
        val deskType = DeskAnomalyType.byId(trigger.target ?: "")
        deskType != null && (state.arc2.deskAnomalyInteractions[deskType] ?: 0) >= trigger.threshold
      }
      else -> false
    }
  }
}
