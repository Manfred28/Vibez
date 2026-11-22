package com.example

import com.example.data.DialogueCatalog
import com.example.data.DialogueEntry
import com.example.data.DialogueRepository
import com.example.data.DialogueTrigger
import com.example.data.EscalationCurve
import com.example.data.PhaseMetadata
import com.example.game.ActiveWallAnomaly
import com.example.game.CreatureType
import com.example.game.OrganBalance
import com.example.game.OrganGameEngine
import com.example.game.OrganGameState
import com.example.game.SpecimenJar
import com.example.game.WallAnomalyType
import com.example.ui.OrganViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrganGameEngineTest {

  private fun sampleCatalog(): DialogueCatalog =
    DialogueCatalog(
      schemaVersion = 2,
      phases =
        listOf(
          PhaseMetadata(0, "The Counting Room", "latent"),
          PhaseMetadata(1, "Uncanny Drift", "uneasy"),
          PhaseMetadata(2, "Apparatus & Flesh", "heavy"),
          PhaseMetadata(3, "Collapse of Meaning", "dread"),
          PhaseMetadata(4, "The Endless Hollow", "overwhelming"),
        ),
      entries =
        listOf(
          DialogueEntry(
            id = "dlg_first",
            phase = 0,
            mood = "clinical",
            trigger = DialogueTrigger("counter_reached", 1L),
            once = true,
            speaker = "Intake",
            lines = listOf("One drop."),
          ),
          DialogueEntry(
            id = "dlg_suture",
            phase = 0,
            mood = "uncanny",
            trigger = DialogueTrigger("creature_owned", 1L, target = "suture_hand"),
            once = true,
            speaker = "Vent",
            lines = listOf("Suture hand."),
          ),
          DialogueEntry(
            id = "dlg_tooth",
            phase = 1,
            mood = "grotesque",
            trigger = DialogueTrigger("anomaly_interacted", 1L, target = "pale_tooth"),
            once = true,
            speaker = "Wall Seam",
            lines = listOf("Pulled tooth."),
          ),
          DialogueEntry(
            id = "dlg_stillness",
            phase = 2,
            mood = "depressive",
            trigger = DialogueTrigger("stillness_reached", 5L),
            once = true,
            speaker = "Letter",
            lines = listOf("Stillness filled."),
          ),
          DialogueEntry(
            id = "dlg_jar",
            phase = 2,
            mood = "existential",
            trigger = DialogueTrigger("jar_bottled", 1L),
            once = true,
            speaker = "Shelf",
            lines = listOf("Jar sealed."),
          ),
          DialogueEntry(
            id = "dlg_graft",
            phase = 3,
            mood = "grotesque",
            trigger = DialogueTrigger("creature_owned", 1L, target = "grafted_tendril"),
            once = true,
            speaker = "Surgical Margin",
            lines = listOf("Graft complete."),
          ),
          DialogueEntry(
            id = "dlg_excision",
            phase = 0,
            mood = "hopeless",
            trigger = DialogueTrigger("excision_completed", 1L),
            once = true,
            speaker = "Overwritten",
            lines = listOf("Cycle 1."),
          ),
        ),
    )

  @Test
  fun initialState_startsBareInPhaseZeroWithLockedDeckAndExcision() {
    val state = OrganGameEngine.initialState(sampleCatalog())

    assertEquals(0L, state.ichorDisplay)
    assertEquals(0, state.phase)
    assertEquals("The Counting Room", state.phaseName)
    assertEquals(25L, state.nextPhaseThreshold)
    assertFalse(state.isApparatusUnlocked)
    assertFalse(state.isJarsAndGraftUnlocked)
    assertFalse(state.canExcise)
    assertNull(state.activeDialogue)
  }

  @Test
  fun pressValve_increasesIchorTriggersFirstFragmentAndBreaksStillness() {
    val catalog = sampleCatalog()
    val idleState =
      OrganGameEngine.initialState(catalog).copy(secondsSinceLastTap = 5.0, isStillnessActive = true)

    val after = OrganGameEngine.pressValve(idleState, catalog)

    assertEquals(1L, after.ichorDisplay)
    assertEquals(1L, after.cycleLifetimeIchorDisplay)
    assertEquals(0.0, after.secondsSinceLastTap, 0.0001)
    assertFalse(after.isStillnessActive)
    assertEquals("dlg_first", after.activeDialogue?.id)
    assertEquals(1, after.discoveredDialogues.size)
  }

  @Test
  fun buyCreature_deductsScalingCostIncreasesPassiveRateAndTriggersFragment() {
    val catalog = sampleCatalog()
    var state = OrganGameEngine.initialState(catalog).copy(ichor = 14.0, cycleLifetimeIchor = 14.0)

    // Cannot buy when below base cost (15)
    assertEquals(0, OrganGameEngine.buyCreature(state, CreatureType.SUTURE_HAND, catalog).creatureCount(CreatureType.SUTURE_HAND))

    state = state.copy(ichor = 40.0, cycleLifetimeIchor = 40.0)
    val bought = OrganGameEngine.buyCreature(state, CreatureType.SUTURE_HAND, catalog)

    assertEquals(1, bought.creatureCount(CreatureType.SUTURE_HAND))
    assertEquals(25.0, bought.ichor, 0.001)
    assertEquals(1.0, bought.ichorPerSecond, 0.001)
    assertTrue("Cost must scale after purchase", bought.creatureCost(CreatureType.SUTURE_HAND) > 15L)
    assertEquals("dlg_suture", bought.activeDialogue?.id)
  }

  @Test
  fun weepingLamps_harvestStillnessAndDoubleLampIchorOnlyWhenIdleForThreeSeconds() {
    val catalog = sampleCatalog()
    var state =
      OrganGameEngine.initialState(catalog).copy(
        ichor = 200.0,
        cycleLifetimeIchor = 200.0,
        creatures = mapOf(CreatureType.WEEPING_LAMP to 2),
      )

    // Tick 2.0s: below 3s threshold, so stillness is NOT active yet (base rate = 2 * 6 = 12/s)
    state = OrganGameEngine.tick(state, 2.0, catalog)
    assertFalse(state.isStillnessActive)
    assertEquals(0.0, state.stillness, 0.001)
    assertEquals(224.0, state.ichor, 0.001)

    // Tick 3.0s more (total idle 5.0s >= 3.0s): stillness activates (2 * 1 = 2 stillness/s, 2 * 6 * 2 = 24 ichor/s)
    state = OrganGameEngine.tick(state, 3.0, catalog)
    assertTrue(state.isStillnessActive)
    assertEquals(6.0, state.stillness, 0.001)
    assertEquals(224.0 + 72.0, state.ichor, 0.001)
    assertEquals("dlg_stillness", state.activeDialogue?.id)

    // Tapping the valve breaks stillness immediately
    val tapped = OrganGameEngine.pressValve(state, catalog)
    assertFalse(tapped.isStillnessActive)
  }

  @Test
  fun wallAnomalies_spawnFromPhaseOneAndGrantDistinctMechanicalEffects() {
    val catalog = sampleCatalog()

    // Phase 0 never spawns wall anomalies
    val p0Ticked = OrganGameEngine.tick(OrganGameEngine.initialState(catalog), 20.0, catalog)
    assertTrue(p0Ticked.activeAnomalies.isEmpty())

    // Phase 1 spawns Pale Tooth after timer
    var p1 = OrganGameEngine.initialState(catalog).copy(ichor = 30.0, cycleLifetimeIchor = 30.0, phase = 1)
    p1 = OrganGameEngine.tick(p1, OrganBalance.FIRST_ANOMALY_DELAY_SEC + 0.5, catalog)
    assertEquals(1, p1.activeAnomalies.size)
    val tooth = p1.activeAnomalies.first()
    assertEquals(WallAnomalyType.PALE_TOOTH, tooth.type)

    val pulled = OrganGameEngine.interactAnomaly(p1, tooth.id, catalog)
    assertTrue(pulled.activeAnomalies.isEmpty())
    assertEquals(1, pulled.anomalyInteractions[WallAnomalyType.PALE_TOOTH])
    assertEquals(30.0 + OrganBalance.TOOTH_BASE_ICHOR_REWARD, pulled.ichor, 0.001)
    assertEquals(1.5, pulled.manualTapYield, 0.001)
    assertEquals("dlg_tooth", pulled.activeDialogue?.id)

    // Phase 2 Glass Eye activates 15s of 2x Gaze multiplier and decays over time
    val p2WithEye =
      OrganGameEngine.initialState(catalog).copy(
        ichor = 160.0,
        cycleLifetimeIchor = 160.0,
        phase = 2,
        creatures = mapOf(CreatureType.SUTURE_HAND to 2),
        activeAnomalies = listOf(ActiveWallAnomaly(99L, WallAnomalyType.GLASS_EYE, 0.2f, 0.2f)),
      )
    val wiped = OrganGameEngine.interactAnomaly(p2WithEye, 99L, catalog)
    assertEquals(OrganBalance.GAZE_DURATION_SEC, wiped.gazeRemainingSeconds, 0.001)
    assertEquals(2.0, wiped.gazeMultiplier, 0.001)
    assertEquals(4.0, wiped.ichorPerSecond, 0.001)

    val afterDecay = OrganGameEngine.tick(wiped, OrganBalance.GAZE_DURATION_SEC + 1.0, catalog)
    assertEquals(0.0, afterDecay.gazeRemainingSeconds, 0.001)
    assertEquals(1.0, afterDecay.gazeMultiplier, 0.001)

    // Phase 3 Swollen Nodule yields both Ichor and Stillness
    val p3WithNodule =
      OrganGameEngine.initialState(catalog).copy(
        ichor = 650.0,
        cycleLifetimeIchor = 650.0,
        phase = 3,
        activeAnomalies = listOf(ActiveWallAnomaly(101L, WallAnomalyType.SWOLLEN_NODULE, 0.8f, 0.7f)),
      )
    val prodded = OrganGameEngine.interactAnomaly(p3WithNodule, 101L, catalog)
    assertEquals(650.0 + OrganBalance.NODULE_BASE_ICHOR_REWARD, prodded.ichor, 0.001)
    assertEquals(OrganBalance.NODULE_BASE_STILLNESS_REWARD, prodded.stillness, 0.001)
  }

  @Test
  fun specimenJars_deductStillnessApplyMultipliersAndPreventDuplicateBottling() {
    val catalog = sampleCatalog()
    var state =
      OrganGameEngine.initialState(catalog).copy(
        ichor = 200.0,
        cycleLifetimeIchor = 200.0,
        stillness = 30.0,
        creatures = mapOf(CreatureType.SUTURE_HAND to 2, CreatureType.LEECH_CLERK to 1),
      )

    state = OrganGameEngine.bottleJar(state, SpecimenJar.YESTERDAYS_DUST, catalog)
    assertTrue(SpecimenJar.YESTERDAYS_DUST in state.ownedJars)
    assertEquals(22.0, state.stillness, 0.001)
    assertEquals(4.0, state.manualTapYield, 0.001)
    assertEquals("dlg_jar", state.activeDialogue?.id)

    // Duplicate bottling does nothing
    val duplicate = OrganGameEngine.bottleJar(state, SpecimenJar.YESTERDAYS_DUST, catalog)
    assertEquals(22.0, duplicate.stillness, 0.001)

    // Unspent Breath doubles Suture-Hands (2*1=2) + Leech-Clerks (1*4=4) -> 12/s
    val breath = OrganGameEngine.bottleJar(state, SpecimenJar.UNSPENT_BREATH, catalog)
    assertTrue(SpecimenJar.UNSPENT_BREATH in breath.ownedJars)
    assertEquals(12.0, breath.ichorPerSecond, 0.001)
  }

  @Test
  fun grafting_requiresPhaseThreeAndConsumesFourCreaturesForChoirTendril() {
    val catalog = sampleCatalog()
    val lockedP2 =
      OrganGameEngine.initialState(catalog).copy(
        cycleLifetimeIchor = 200.0,
        phase = 2,
        creatures = mapOf(CreatureType.SUTURE_HAND to 3, CreatureType.LEECH_CLERK to 2),
      )
    assertFalse(lockedP2.canAffordGraft)
    assertEquals(0, OrganGameEngine.performGraft(lockedP2, catalog).graftedTendrils)

    val readyP3 = lockedP2.copy(cycleLifetimeIchor = 700.0, phase = 3)
    assertTrue(readyP3.canAffordGraft)

    val grafted = OrganGameEngine.performGraft(readyP3, catalog)
    assertEquals(1, grafted.graftedTendrils)
    assertEquals(1, grafted.creatureCount(CreatureType.SUTURE_HAND))
    assertEquals(0, grafted.creatureCount(CreatureType.LEECH_CLERK))
    assertEquals(1.0 + OrganBalance.GRAFT_ICHOR_PER_SEC, grafted.ichorPerSecond, 0.001)
    assertEquals(OrganBalance.GRAFT_STILLNESS_PER_SEC, grafted.stillnessPerSecond, 0.001)
    assertEquals("dlg_graft", grafted.activeDialogue?.id)
  }

  @Test
  fun excision_resetsCycleProgressGrantsScarTissueMultiplierAndTriggersCycleOneNote() {
    val catalog = sampleCatalog()
    val notReady =
      OrganGameEngine.initialState(catalog).copy(ichor = 1_500.0, cycleLifetimeIchor = 1_500.0)
    assertFalse(notReady.canExcise)
    assertEquals(0L, notReady.pendingScarTissue)
    assertEquals(0, OrganGameEngine.performExcision(notReady, catalog).cycle)

    val ready =
      OrganGameEngine.initialState(catalog).copy(
        ichor = 2_200.0,
        cycleLifetimeIchor = 2_200.0,
        stillness = 50.0,
        phase = 4,
        creatures = mapOf(CreatureType.SUTURE_HAND to 5, CreatureType.WEEPING_LAMP to 2),
        graftedTendrils = 1,
        ownedJars = setOf(SpecimenJar.YESTERDAYS_DUST),
      )
    assertTrue(ready.canExcise)
    assertEquals(1L, ready.pendingScarTissue)

    val postReset = OrganGameEngine.performExcision(ready, catalog)
    assertEquals(1, postReset.cycle)
    assertEquals(1L, postReset.scarTissue)
    assertEquals(0L, postReset.ichorDisplay)
    assertEquals(0L, postReset.stillnessDisplay)
    assertEquals(0, postReset.phase)
    assertTrue(postReset.creatures.isEmpty())
    assertEquals(0, postReset.graftedTendrils)
    assertTrue(postReset.ownedJars.isEmpty())
    assertEquals(1.75, postReset.scarMultiplier, 0.001)
    assertEquals(1.75, postReset.manualTapYield, 0.001)
    assertEquals("dlg_excision", postReset.activeDialogue?.id)
  }

  @Test
  fun escalationCurveAndViewModelFallback_handleBoundariesAndRepositoryErrorsSafely() {
    assertEquals(0, EscalationCurve.phaseForCount(-1L))
    assertEquals(0, EscalationCurve.phaseForCount(24L))
    assertEquals(1, EscalationCurve.phaseForCount(25L))
    assertEquals(2, EscalationCurve.phaseForCount(150L))
    assertEquals(3, EscalationCurve.phaseForCount(600L))
    assertEquals(4, EscalationCurve.phaseForCount(2_000L))
    assertNull(EscalationCurve.nextThresholdForPhase(4))

    val brokenRepo =
      object : DialogueRepository {
        override fun loadCatalog(): DialogueCatalog = error("Disk fault")
      }
    val viewModel = OrganViewModel(dialogueRepository = brokenRepo, autoTick = false)
    viewModel.pressValve()
    assertEquals(1L, viewModel.uiState.value.ichorDisplay)
    assertNull(viewModel.uiState.value.activeDialogue)
  }
}
