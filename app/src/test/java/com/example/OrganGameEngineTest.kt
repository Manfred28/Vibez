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
            trigger = DialogueTrigger("stillness_reached", 12L),
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
          DialogueEntry(
            id = "dlg_cycle_1_echo",
            phase = 1,
            mood = "existential",
            trigger = DialogueTrigger("cycle_counter_reached", 75L, target = "1"),
            once = true,
            speaker = "Caretaker Margin",
            lines = listOf("Cycle 1 handwriting echo."),
          ),
        ),
    )

  @Test
  fun initialState_startsBareInPhaseZeroWithLockedDeckAndExcision() {
    val state = OrganGameEngine.initialState(sampleCatalog())

    assertEquals(0L, state.ichorDisplay)
    assertEquals(0, state.phase)
    assertEquals("The Counting Room", state.phaseName)
    assertEquals(75L, state.nextPhaseThreshold)
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
    var state = OrganGameEngine.initialState(catalog).copy(ichor = 24.0, cycleLifetimeIchor = 24.0)

    // Cannot buy when below base cost (25)
    assertEquals(
      0,
      OrganGameEngine.buyCreature(state, CreatureType.SUTURE_HAND, catalog)
        .creatureCount(CreatureType.SUTURE_HAND),
    )

    state = state.copy(ichor = 75.0, cycleLifetimeIchor = 75.0)
    val bought = OrganGameEngine.buyCreature(state, CreatureType.SUTURE_HAND, catalog)

    assertEquals(1, bought.creatureCount(CreatureType.SUTURE_HAND))
    assertEquals(50.0, bought.ichor, 0.001)
    assertEquals(0.6, bought.ichorPerSecond, 0.001)
    assertTrue("Cost must scale after purchase", bought.creatureCost(CreatureType.SUTURE_HAND) > 25L)
    assertEquals("dlg_suture", bought.activeDialogue?.id)

    // Verify Weeping Lamp is 15% cheaper (1,190 Ichor base cost, unlocks at 1,000 Ichor)
    assertEquals(1_190L, bought.creatureCost(CreatureType.WEEPING_LAMP))
    assertEquals(1_000L, CreatureType.WEEPING_LAMP.unlockIchorThreshold)
  }

  @Test
  fun weepingLamps_harvestStillnessAndDoubleLampIchorOnlyWhenIdleForThreeSeconds() {
    val catalog = sampleCatalog()
    var state =
      OrganGameEngine.initialState(catalog).copy(
        ichor = 1_600.0,
        cycleLifetimeIchor = 1_600.0,
        creatures = mapOf(CreatureType.WEEPING_LAMP to 2),
      )

    // Tick 2.0s: below 3s threshold, so stillness is NOT active yet (base rate = 2 * 6.0 = 12/s)
    state = OrganGameEngine.tick(state, 2.0, catalog)
    assertFalse(state.isStillnessActive)
    assertEquals(0.0, state.stillness, 0.001)
    assertEquals(1_624.0, state.ichor, 0.001)

    // Tick 15.0s more (total idle 17s >= 3s): stillness activates (2 * 0.45 = 0.9/s -> 13.5,
    // 2 * 6.0 * 2 = 24 ichor/s -> +360)
    state = OrganGameEngine.tick(state, 15.0, catalog)
    assertTrue(state.isStillnessActive)
    assertEquals(13.5, state.stillness, 0.001)
    assertEquals(1_624.0 + 360.0, state.ichor, 0.001)
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
    var p1 =
      OrganGameEngine.initialState(catalog).copy(ichor = 80.0, cycleLifetimeIchor = 80.0, phase = 1)
    p1 = OrganGameEngine.tick(p1, OrganBalance.FIRST_ANOMALY_DELAY_SEC + 0.5, catalog)
    assertEquals(1, p1.activeAnomalies.size)
    val tooth = p1.activeAnomalies.first()
    assertEquals(WallAnomalyType.PALE_TOOTH, tooth.type)

    val pulled = OrganGameEngine.interactAnomaly(p1, tooth.id, catalog)
    assertTrue(pulled.activeAnomalies.isEmpty())
    assertEquals(1, pulled.anomalyInteractions[WallAnomalyType.PALE_TOOTH])
    assertEquals(80.0 + OrganBalance.TOOTH_BASE_ICHOR_REWARD, pulled.ichor, 0.001)
    assertEquals(1.5, pulled.manualTapYield, 0.001)
    assertEquals("dlg_tooth", pulled.activeDialogue?.id)

    // Phase 2 Glass Eye activates 12s of 1.6x Gaze multiplier and decays over time
    val p2WithEye =
      OrganGameEngine.initialState(catalog).copy(
        ichor = 1_700.0,
        cycleLifetimeIchor = 1_700.0,
        phase = 2,
        creatures = mapOf(CreatureType.SUTURE_HAND to 2),
        activeAnomalies = listOf(ActiveWallAnomaly(99L, WallAnomalyType.GLASS_EYE, 0.2f, 0.2f)),
      )
    val wiped = OrganGameEngine.interactAnomaly(p2WithEye, 99L, catalog)
    assertEquals(OrganBalance.GAZE_DURATION_SEC, wiped.gazeRemainingSeconds, 0.001)
    assertEquals(OrganBalance.GAZE_PASSIVE_MULTIPLIER, wiped.gazeMultiplier, 0.001)
    assertEquals(2 * 0.6 * OrganBalance.GAZE_PASSIVE_MULTIPLIER, wiped.ichorPerSecond, 0.001)

    val afterDecay = OrganGameEngine.tick(wiped, OrganBalance.GAZE_DURATION_SEC + 1.0, catalog)
    assertEquals(0.0, afterDecay.gazeRemainingSeconds, 0.001)
    assertEquals(1.0, afterDecay.gazeMultiplier, 0.001)

    // Phase 3 Swollen Nodule yields both Ichor and Stillness
    val p3WithNodule =
      OrganGameEngine.initialState(catalog).copy(
        ichor = 16_500.0,
        cycleLifetimeIchor = 16_500.0,
        phase = 3,
        activeAnomalies = listOf(ActiveWallAnomaly(101L, WallAnomalyType.SWOLLEN_NODULE, 0.8f, 0.7f)),
      )
    val prodded = OrganGameEngine.interactAnomaly(p3WithNodule, 101L, catalog)
    assertEquals(16_500.0 + OrganBalance.NODULE_BASE_ICHOR_REWARD, prodded.ichor, 0.001)
    assertEquals(OrganBalance.NODULE_BASE_STILLNESS_REWARD, prodded.stillness, 0.001)
  }

  @Test
  fun specimenJars_deductStillnessApplyMultipliersAndPreventDuplicateBottling() {
    val catalog = sampleCatalog()
    var state =
      OrganGameEngine.initialState(catalog).copy(
        ichor = 1_700.0,
        cycleLifetimeIchor = 1_700.0,
        stillness = 100.0,
        creatures = mapOf(CreatureType.SUTURE_HAND to 2, CreatureType.LEECH_CLERK to 1),
      )

    state = OrganGameEngine.bottleJar(state, SpecimenJar.YESTERDAYS_DUST, catalog)
    assertTrue(SpecimenJar.YESTERDAYS_DUST in state.ownedJars)
    assertEquals(76.0, state.stillness, 0.001)
    assertEquals(4.0, state.manualTapYield, 0.001)
    assertEquals("dlg_jar", state.activeDialogue?.id)

    // Duplicate bottling does nothing
    val duplicate = OrganGameEngine.bottleJar(state, SpecimenJar.YESTERDAYS_DUST, catalog)
    assertEquals(76.0, duplicate.stillness, 0.001)

    // Unspent Breath boosts Suture-Hands (2*0.6=1.2) + Leech-Clerks (1*2.4=2.4) by 1.75x -> 6.3/s
    val breath = OrganGameEngine.bottleJar(state, SpecimenJar.UNSPENT_BREATH, catalog)
    assertTrue(SpecimenJar.UNSPENT_BREATH in breath.ownedJars)
    assertEquals(6.3, breath.ichorPerSecond, 0.001)
  }

  @Test
  fun grafting_requiresPhaseThreeAndConsumesFourCreaturesForChoirTendril() {
    val catalog = sampleCatalog()
    val lockedP2 =
      OrganGameEngine.initialState(catalog).copy(
        cycleLifetimeIchor = 2_000.0,
        phase = 2,
        creatures = mapOf(CreatureType.SUTURE_HAND to 3, CreatureType.LEECH_CLERK to 2),
      )
    assertFalse(lockedP2.canAffordGraft)
    assertEquals(0, OrganGameEngine.performGraft(lockedP2, catalog).graftedTendrils)

    val readyP3 = lockedP2.copy(cycleLifetimeIchor = 16_500.0, phase = 3)
    assertTrue(readyP3.canAffordGraft)

    val grafted = OrganGameEngine.performGraft(readyP3, catalog)
    assertEquals(1, grafted.graftedTendrils)
    assertEquals(1, grafted.creatureCount(CreatureType.SUTURE_HAND))
    assertEquals(0, grafted.creatureCount(CreatureType.LEECH_CLERK))
    assertEquals(0.6 + OrganBalance.GRAFT_ICHOR_PER_SEC, grafted.ichorPerSecond, 0.001)
    assertEquals(OrganBalance.GRAFT_STILLNESS_PER_SEC, grafted.stillnessPerSecond, 0.001)
    assertEquals("dlg_graft", grafted.activeDialogue?.id)
  }

  @Test
  fun excision_resetsCycleProgressGrantsScarTissueMultiplierAndTriggersCycleOneNote() {
    val catalog = sampleCatalog()
    val notReady =
      OrganGameEngine.initialState(catalog).copy(
        ichor = 100_000.0,
        cycleLifetimeIchor = 100_000.0,
        phase = 4,
      )
    assertFalse("Phase 4 entry alone must not immediately unlock Excision", notReady.canExcise)
    assertEquals(0L, notReady.pendingScarTissue)
    assertEquals(0, OrganGameEngine.performExcision(notReady, catalog).cycle)

    val ready =
      OrganGameEngine.initialState(catalog).copy(
        ichor = 215_000.0,
        cycleLifetimeIchor = 215_000.0,
        stillness = 80.0,
        phase = 4,
        creatures = mapOf(CreatureType.SUTURE_HAND to 5, CreatureType.WEEPING_LAMP to 2),
        graftedTendrils = 1,
        ownedJars = setOf(SpecimenJar.YESTERDAYS_DUST),
        anomalyInteractions = mapOf(WallAnomalyType.PALE_TOOTH to 3),
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
    assertTrue(postReset.anomalyInteractions.isEmpty())
    assertEquals(1.75, postReset.scarMultiplier, 0.001)
    assertEquals(1.25, postReset.scarStillnessMultiplier, 0.001)
    assertEquals(1.75, postReset.manualTapYield, 0.001)
    assertEquals("dlg_excision", postReset.activeDialogue?.id)

    // Verify prestige passive Stillness multiplier applies to Weeping Lamps & Swollen Nodules
    val cycle1LampIdle =
      postReset.copy(
        creatures = mapOf(CreatureType.WEEPING_LAMP to 1),
        isStillnessActive = true,
      )
    assertEquals(
      OrganBalance.LAMP_STILLNESS_PER_SEC * 1.25,
      cycle1LampIdle.stillnessPerSecond,
      0.0001,
    )

    // Verify cycle_counter_reached story entry does NOT trigger in Cycle 0, but DOES in Cycle 1+
    val cycle0Phase1 =
      OrganGameEngine.pressValve(
        OrganGameEngine.initialState(catalog).copy(ichor = 74.0, cycleLifetimeIchor = 74.0),
        catalog,
      )
    assertFalse("dlg_cycle_1_echo" in cycle0Phase1.triggeredDialogueIds)

    val cycle1Phase1 =
      OrganGameEngine.pressValve(
        postReset.copy(ichor = 74.0, cycleLifetimeIchor = 74.0),
        catalog,
      )
    assertEquals("dlg_cycle_1_echo", cycle1Phase1.activeDialogue?.id)
  }

  @Test
  fun escalationCurveAndViewModelFallback_handleBoundariesAndRepositoryErrorsSafely() {
    assertEquals(0, EscalationCurve.phaseForCount(-1L))
    assertEquals(0, EscalationCurve.phaseForCount(74L))
    assertEquals(1, EscalationCurve.phaseForCount(75L))
    assertEquals(2, EscalationCurve.phaseForCount(1_600L))
    assertEquals(3, EscalationCurve.phaseForCount(16_000L))
    assertEquals(4, EscalationCurve.phaseForCount(85_000L))
    assertNull(EscalationCurve.nextThresholdForPhase(4))
    assertEquals(
      0.5f,
      EscalationCurve.progressWithinPhase(
        count = 147_500L,
        finalGoalThreshold = OrganBalance.EXCISION_ICHOR_THRESHOLD,
      ),
      0.01f,
    )

    val brokenRepo =
      object : DialogueRepository {
        override fun loadCatalog(): DialogueCatalog = error("Disk fault")
      }
    val viewModel = OrganViewModel(dialogueRepository = brokenRepo, autoTick = false)
    viewModel.pressValve()
    assertEquals(1L, viewModel.uiState.value.ichorDisplay)
    assertNull(viewModel.uiState.value.activeDialogue)
  }

  @Test
  fun firstCyclePacing_reachesFirstExcisionInRoughlyFifteenMinutesWithModerateActivity() {
    val catalog = sampleCatalog()
    var state = OrganGameEngine.initialState(catalog)
    var elapsedSec = 0

    // Simulate realistic moderate activity:
    // - Before Weeping Lamps: ~1.2 manual taps/sec (alternating 1 and 2 taps/sec) + anomalies + purchases
    // - Once Weeping Lamps unlock: alternates 25s Stillness pauses (hands off) with 20s active tending
    while (!state.canExcise && elapsedSec < 1_800) {
      val hasLamps = state.creatureCount(CreatureType.WEEPING_LAMP) > 0
      val inStillnessWindow = hasLamps && (elapsedSec % 45) < 25

      if (!inStillnessWindow) {
        // Moderate manual valve tapping (~1.2 taps/sec early, ~0.8 taps/sec once automated)
        val tapsThisSecond =
          when {
            !hasLamps && elapsedSec % 5 == 0 -> 2
            !hasLamps -> 1
            elapsedSec % 2 == 0 -> 1
            else -> 0
          }
        repeat(tapsThisSecond) { state = OrganGameEngine.pressValve(state, catalog) }

        // Interact with any wall anomaly during active windows
        if (state.activeAnomalies.isNotEmpty()) {
          state = OrganGameEngine.interactAnomaly(state, state.activeAnomalies.first().id, catalog)
        }
      }

      // Bottle affordable Specimen Jars (does not break Stillness)
      SpecimenJar.entries.forEach { jar ->
        if (jar !in state.ownedJars && state.stillness >= jar.stillnessCost) {
          state = OrganGameEngine.bottleJar(state, jar, catalog)
        }
      }

      // Perform Graft once Phase 3 unlocks and creatures are available
      if (state.canAffordGraft) {
        state = OrganGameEngine.performGraft(state, catalog)
      }

      // Purchase best-value affordable organism (with brief saving for next tier)
      val lampCost = state.creatureCost(CreatureType.WEEPING_LAMP)
      val leechCost = state.creatureCost(CreatureType.LEECH_CLERK)
      val sutureCost = state.creatureCost(CreatureType.SUTURE_HAND)
      val lampUnlocked = state.cycleLifetimeIchorDisplay >= CreatureType.WEEPING_LAMP.unlockIchorThreshold
      val leechUnlocked = state.cycleLifetimeIchorDisplay >= CreatureType.LEECH_CLERK.unlockIchorThreshold

      when {
        lampUnlocked && state.ichor >= lampCost ->
          state = OrganGameEngine.buyCreature(state, CreatureType.WEEPING_LAMP, catalog)
        leechUnlocked && state.ichor >= leechCost && (!lampUnlocked || leechCost < lampCost * 0.55) ->
          state = OrganGameEngine.buyCreature(state, CreatureType.LEECH_CLERK, catalog)
        state.isApparatusUnlocked &&
          state.ichor >= sutureCost &&
          (!leechUnlocked || sutureCost < leechCost * 0.45) ->
          state = OrganGameEngine.buyCreature(state, CreatureType.SUTURE_HAND, catalog)
      }

      state = OrganGameEngine.tick(state, 1.0, catalog)
      elapsedSec += 1
    }

    assertTrue("Expected moderate-activity Cycle 0 to reach Excision within 30 min", state.canExcise)
    assertTrue(
      "Expected Cycle 0 -> First Excision in 12..18 min (720..1080s), actual was ${elapsedSec}s (${elapsedSec / 60.0} min)",
      elapsedSec in 720..1_080,
    )
  }

  @Test
  fun perOrganismEffectiveRates_matchAggregateIchorAndStillnessAcrossAllMultipliers() {
    val catalog = sampleCatalog()
    val state =
      OrganGameEngine.initialState(catalog).copy(
        creatures =
          mapOf(
            CreatureType.SUTURE_HAND to 3,
            CreatureType.LEECH_CLERK to 2,
            CreatureType.WEEPING_LAMP to 2,
          ),
        ownedJars = setOf(SpecimenJar.UNSPENT_BREATH, SpecimenJar.ALMOST_A_NAME),
        isStillnessActive = true,
        gazeRemainingSeconds = 10.0,
        scarTissue = 2L,
      )

    val scarIchorMult = 1.0 + 2 * OrganBalance.SCAR_TISSUE_BONUS_PER_UNIT // 2.5x
    val scarStillMult = 1.0 + 2 * OrganBalance.SCAR_STILLNESS_BONUS_PER_UNIT // 1.5x
    val gazeMult = OrganBalance.GAZE_PASSIVE_MULTIPLIER // 1.6x

    val expectedSutureEach =
      CreatureType.SUTURE_HAND.baseIchorPerSec *
        OrganBalance.UNSPENT_BREATH_MULTIPLIER *
        scarIchorMult *
        gazeMult
    val expectedLeechEach =
      CreatureType.LEECH_CLERK.baseIchorPerSec *
        OrganBalance.UNSPENT_BREATH_MULTIPLIER *
        scarIchorMult *
        gazeMult
    val expectedLampEach =
      CreatureType.WEEPING_LAMP.baseIchorPerSec *
        OrganBalance.ALMOST_A_NAME_MULTIPLIER *
        2.0 *
        scarIchorMult *
        gazeMult
    val expectedLampStillnessEach =
      OrganBalance.LAMP_STILLNESS_PER_SEC *
        OrganBalance.ALMOST_A_NAME_MULTIPLIER *
        scarStillMult *
        gazeMult

    assertEquals(expectedSutureEach, state.effectiveCreatureIchorPerSec(CreatureType.SUTURE_HAND), 0.0001)
    assertEquals(expectedLeechEach, state.effectiveCreatureIchorPerSec(CreatureType.LEECH_CLERK), 0.0001)
    assertEquals(expectedLampEach, state.effectiveCreatureIchorPerSec(CreatureType.WEEPING_LAMP), 0.0001)
    assertEquals(expectedLampStillnessEach, state.effectiveLampStillnessPerSec(), 0.0001)

    val summedIchor =
      state.totalCreatureIchorPerSec(CreatureType.SUTURE_HAND) +
        state.totalCreatureIchorPerSec(CreatureType.LEECH_CLERK) +
        state.totalCreatureIchorPerSec(CreatureType.WEEPING_LAMP)
    assertEquals(state.ichorPerSecond, summedIchor, 0.0001)
    assertEquals(state.stillnessPerSecond, 2 * expectedLampStillnessEach, 0.0001)
  }
}
