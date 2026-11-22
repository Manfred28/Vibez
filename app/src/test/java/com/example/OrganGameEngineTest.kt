package com.example

import com.example.data.DialogueCatalog
import com.example.data.DialogueEntry
import com.example.data.DialogueRepository
import com.example.data.DialogueTrigger
import com.example.data.EscalationCurve
import com.example.data.PhaseMetadata
import com.example.game.ActiveWallAnomaly
import com.example.game.Arc2Balance
import com.example.game.Arc2InvestigatorEngine
import com.example.game.CaseDeduction
import com.example.game.CreatureType
import com.example.game.CultDossierLine
import com.example.game.DredgedRelic
import com.example.game.ExcisionMilestone
import com.example.game.GameArc
import com.example.game.InvestigatorContact
import com.example.game.OrganBalance
import com.example.game.OrganDevEngine
import com.example.game.OrganGameEngine
import com.example.game.OrganGameState
import com.example.game.ScarNodeId
import com.example.game.SpecimenJar
import com.example.game.WallAnomalyType
import com.example.ui.OrganViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrganGameEngineTest {

  private fun sampleCatalog(): DialogueCatalog =
    DialogueCatalog(
      schemaVersion = 3,
      phases =
        listOf(
          PhaseMetadata(0, "The Counting Room", "latent"),
          PhaseMetadata(1, "Uncanny Drift", "uneasy"),
          PhaseMetadata(2, "Apparatus & Flesh", "heavy"),
          PhaseMetadata(3, "Collapse of Meaning", "dread"),
          PhaseMetadata(4, "The Endless Hollow", "overwhelming"),
          PhaseMetadata(5, "The Inquiry", "haunted"),
          PhaseMetadata(6, "The Convergence", "threshold"),
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
            id = "dlg_rib",
            phase = 4,
            mood = "grotesque",
            trigger = DialogueTrigger("anomaly_interacted", 1L, target = "chitin_rib"),
            once = true,
            speaker = "Marrow Seam",
            lines = listOf("Rib pried open."),
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
          DialogueEntry(
            id = "dlg_scar_first",
            phase = 0,
            mood = "grotesque",
            trigger = DialogueTrigger("scar_node_unlocked", 1L),
            once = true,
            speaker = "Surgical Note",
            lines = listOf("First incision sutured."),
          ),
          DialogueEntry(
            id = "dlg_relic",
            phase = 2,
            mood = "existential",
            trigger = DialogueTrigger("relic_dredged", 1L),
            once = true,
            speaker = "Drain",
            lines = listOf("Wristband dredged."),
          ),
          DialogueEntry(
            id = "dlg_echo",
            phase = 3,
            mood = "uncanny",
            trigger = DialogueTrigger("echo_routed", 1L),
            once = true,
            speaker = "Aperture",
            lines = listOf("Resonance returned."),
          ),
          DialogueEntry(
            id = "dlg_bloom",
            phase = 4,
            mood = "grotesque",
            trigger = DialogueTrigger("marrow_bloom", 1L),
            once = true,
            speaker = "Clamp",
            lines = listOf("Marrow bloom erupted."),
          ),
          DialogueEntry(
            id = "dlg_dossier_1",
            phase = 4,
            mood = "existential",
            trigger = DialogueTrigger("dossier_line_unredacted", 1L),
            once = true,
            speaker = "Dossier",
            lines = listOf("Line 1 unsealed."),
          ),
          DialogueEntry(
            id = "dlg_arc2_entry",
            phase = 5,
            mood = "depressive",
            trigger = DialogueTrigger("arc_entered", 1L, target = "arc_2"),
            once = true,
            speaker = "Desk",
            lines = listOf("Entered Arc 2."),
          ),
          DialogueEntry(
            id = "dlg_arc2_file_zero",
            phase = 5,
            mood = "existential",
            trigger = DialogueTrigger("case_chapter_completed", 4L),
            once = true,
            speaker = "File #00",
            lines = listOf("It is your own signature."),
          ),
          DialogueEntry(
            id = "dlg_arc3_entry",
            phase = 6,
            mood = "hopeless",
            trigger = DialogueTrigger("arc_entered", 1L, target = "arc_3"),
            once = true,
            speaker = "Threshold",
            lines = listOf("Entered Arc 3."),
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

    // Phase 4 Split Chitin Rib grants scaled Ichor + Stillness + 6s Eye Gaze without breaking Stillness
    val p4WithRib =
      OrganGameEngine.initialState(catalog).copy(
        ichor = 90_000.0,
        cycleLifetimeIchor = 90_000.0,
        phase = 4,
        isStillnessActive = true,
        ownedJars = setOf(SpecimenJar.ALMOST_A_NAME),
        activeAnomalies = listOf(ActiveWallAnomaly(102L, WallAnomalyType.CHITIN_RIB, 0.8f, 0.3f)),
      )
    val pried = OrganGameEngine.interactAnomaly(p4WithRib, 102L, catalog)
    assertEquals(
      90_000.0 + OrganBalance.RIB_BASE_ICHOR_REWARD * OrganBalance.ALMOST_A_NAME_MULTIPLIER,
      pried.ichor,
      0.001,
    )
    assertEquals(
      OrganBalance.RIB_BASE_STILLNESS_REWARD * OrganBalance.ALMOST_A_NAME_MULTIPLIER,
      pried.stillness,
      0.001,
    )
    assertEquals(OrganBalance.RIB_GAZE_DURATION_SEC, pried.gazeRemainingSeconds, 0.001)
    assertTrue("Prying a Chitin Rib must preserve active Stillness", pried.isStillnessActive)
    assertEquals("dlg_rib", pried.activeDialogue?.id)

    // Verify Chitin Rib can spawn organically in Phase 4 when other anomalies are already on screen
    var p4Spawner =
      OrganGameEngine.initialState(catalog).copy(
        ichor = 90_000.0,
        cycleLifetimeIchor = 90_000.0,
        phase = 4,
        nextAnomalyId = 4L,
        secondsUntilNextAnomaly = 0.5,
      )
    p4Spawner = OrganGameEngine.tick(p4Spawner, 1.0, catalog)
    assertEquals(WallAnomalyType.CHITIN_RIB, p4Spawner.activeAnomalies.first().type)
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
    // Excision I first clear yields 1 base + 2 M1 milestone bonus = 3 Scar Tissue
    assertEquals(3L, ready.pendingScarTissue)

    val postReset = OrganGameEngine.performExcision(ready, catalog)
    assertEquals(1, postReset.cycle)
    assertEquals(1, postReset.highestExcisionMilestone)
    assertEquals(3L, postReset.scarTissue)
    assertEquals(3L, postReset.availableScarTissue)
    assertEquals(0L, postReset.ichorDisplay)
    assertEquals(0L, postReset.stillnessDisplay)
    assertEquals(0, postReset.phase)
    assertTrue(postReset.creatures.isEmpty())
    assertEquals(0, postReset.graftedTendrils)
    assertTrue(postReset.ownedJars.isEmpty())
    assertTrue(postReset.anomalyInteractions.isEmpty())
    // scarMultiplier = 1.0 + 1*0.25 + 3*0.15 = 1.70; scarStillnessMultiplier = 1.0 + 1*0.20 + 3*0.10 = 1.50
    assertEquals(1.70, postReset.scarMultiplier, 0.001)
    assertEquals(1.50, postReset.scarStillnessMultiplier, 0.001)
    assertEquals(1.70, postReset.manualTapYield, 0.001)
    assertEquals("dlg_excision", postReset.activeDialogue?.id)

    // Verify prestige passive Stillness multiplier applies to Weeping Lamps
    val cycle1LampIdle =
      postReset.copy(
        creatures = mapOf(CreatureType.WEEPING_LAMP to 1),
        isStillnessActive = true,
      )
    assertEquals(
      OrganBalance.LAMP_STILLNESS_PER_SEC * 1.50,
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

    val scarIchorMult = 1.0 + 2 * OrganBalance.SCAR_RESONANCE_PER_UNIT // 1.30x
    val scarStillMult = 1.0 + 2 * OrganBalance.SCAR_STILLNESS_RESONANCE_PER_UNIT // 1.20x
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

  @Test
  fun scarSkillTree_enforcesPrerequisitesAppliesAllBranchPowersAndCompressesRepeatRuns() {
    val catalog = sampleCatalog()
    var state =
      OrganGameEngine.initialState(catalog).copy(
        highestExcisionMilestone = 1,
        scarTissue = 25L,
        availableScarTissue = 25L,
      )

    // Prerequisite check: Tier 1 cannot be sutured before FIRST_INCISION root
    assertFalse(state.canUnlockNode(ScarNodeId.CALLOUSED_PALM))
    assertTrue(state.canUnlockNode(ScarNodeId.FIRST_INCISION))

    state = OrganGameEngine.unlockScarNode(state, ScarNodeId.FIRST_INCISION, catalog)
    assertEquals(24L, state.availableScarTissue)
    assertEquals("dlg_scar_first", state.activeDialogue?.id)

    // Unlock Suture + Stillness + Marrow Tier 1-2 nodes
    state = OrganGameEngine.unlockScarNode(state, ScarNodeId.CALLOUSED_PALM, catalog)
    state = OrganGameEngine.unlockScarNode(state, ScarNodeId.SHORTER_SILENCE, catalog)
    state = OrganGameEngine.unlockScarNode(state, ScarNodeId.WAX_SEALED_MEMORY, catalog)
    state = OrganGameEngine.unlockScarNode(state, ScarNodeId.SOFTENED_MORTAR, catalog)
    state = OrganGameEngine.unlockScarNode(state, ScarNodeId.AUTO_TENDRIL_SUTURE, catalog)
    assertEquals(7L, state.availableScarTissue)

    // Verify Shorter Silence reduces Stillness delay to 1.5s
    assertEquals(1.5, state.stillnessIdleDelaySec, 0.001)

    // Verify Wax-Sealed Memory preserves jars and Calloused Palm seeds creatures across Excision reset
    val preReset =
      state.copy(
        ichor = 220_000.0,
        cycleLifetimeIchor = 220_000.0,
        phase = 4,
        ownedJars = setOf(SpecimenJar.YESTERDAYS_DUST, SpecimenJar.UNSPENT_BREATH),
      )
    val postReset = OrganGameEngine.performExcision(preReset, catalog)
    assertEquals(3, postReset.creatureCount(CreatureType.SUTURE_HAND))
    assertEquals(1, postReset.creatureCount(CreatureType.LEECH_CLERK))
    assertEquals(setOf(SpecimenJar.YESTERDAYS_DUST, SpecimenJar.UNSPENT_BREATH), postReset.ownedJars)

    // Verify Deep Weeping drips 40% Stillness even while actively tapping
    val deepWeepingState =
      postReset.copy(
        unlockedScarNodes = postReset.unlockedScarNodes + ScarNodeId.DEEP_WEEPING,
        creatures = mapOf(CreatureType.WEEPING_LAMP to 1),
        isStillnessActive = false,
      )
    assertTrue("Deep Weeping must generate Stillness even while not idle", deepWeepingState.stillnessPerSecond > 0.0)

    // Verify Auto-Tendril Suture automatically grafts when 4+ Hands & 4+ Leeches exist in Phase 3+
    val autoGraftReady =
      postReset.copy(
        cycleLifetimeIchor = 20_000.0,
        phase = 3,
        creatures = mapOf(CreatureType.SUTURE_HAND to 4, CreatureType.LEECH_CLERK to 4),
      )
    val afterAutoGraft = OrganGameEngine.tick(autoGraftReady, 1.0, catalog)
    assertEquals(1, afterAutoGraft.graftedTendrils)

    // Verify an upgraded Cycle 2 repeat harvest compresses cleanly from ~15 min down under 7 min
    var sim = postReset
    var sec = 0
    while (!sim.canExcise && sec < 480) {
      if (sec % 2 == 0) sim = OrganGameEngine.pressValve(sim, catalog)
      if (sim.activeAnomalies.isNotEmpty()) {
        sim = OrganGameEngine.interactAnomaly(sim, sim.activeAnomalies.first().id, catalog)
      }
      val lampCost = sim.creatureCost(CreatureType.WEEPING_LAMP)
      val leechCost = sim.creatureCost(CreatureType.LEECH_CLERK)
      when {
        sim.cycleLifetimeIchorDisplay >= CreatureType.WEEPING_LAMP.unlockIchorThreshold &&
          sim.ichor >= lampCost -> sim = OrganGameEngine.buyCreature(sim, CreatureType.WEEPING_LAMP, catalog)
        sim.cycleLifetimeIchorDisplay >= CreatureType.LEECH_CLERK.unlockIchorThreshold &&
          sim.ichor >= leechCost -> sim = OrganGameEngine.buyCreature(sim, CreatureType.LEECH_CLERK, catalog)
      }
      sim = OrganGameEngine.tick(sim, 1.0, catalog)
      sec += 1
    }
    assertTrue("Upgraded Cycle 2 repeat run should reach Excision in <= 7 min (actual ${sec}s)", sim.canExcise && sec <= 420)
  }

  @Test
  fun excisionsTwoThroughSix_unlockSumpDrainEchoClampChoirLungThroatDossierAndBreachToArc2() {
    val catalog = sampleCatalog()

    // Excision II: Sump-Siphon + Floor Drain Grate Seal -> 100% -> Dredge Relic
    var s = OrganDevEngine.setHighestMilestone(OrganGameEngine.initialState(catalog), 2, catalog)
    s = OrganDevEngine.assignArc1Resources(s, ichor = 500_000.0, stillness = 100.0, catalog = catalog)
    s = OrganGameEngine.buyCreature(s, CreatureType.SUMP_SIPHON, catalog)
    assertEquals(1, s.creatureCount(CreatureType.SUMP_SIPHON))

    s = OrganGameEngine.toggleDrainSeal(s, catalog)
    assertTrue(s.drainSealed)
    s = OrganGameEngine.tick(s, 20.0, catalog)
    assertTrue(s.canDredgeSump)
    s = OrganGameEngine.dredgeSump(s, catalog)
    assertTrue(DredgedRelic.PATIENT_WRISTBAND in s.dredgedRelics)
    assertEquals(0.0, s.drainPressure, 0.001)
    assertEquals("dlg_relic", s.activeDialogue?.id)

    // Excision III: Neighbor Aperture Echo Diversion -> 12s burst
    s = OrganDevEngine.setHighestMilestone(s, 3, catalog)
    s = OrganGameEngine.setEchoDiversion(s, 50, catalog)
    assertEquals(50, s.echoDiversionPercent)
    val ichorBeforeEcho = s.ichor
    s = OrganGameEngine.tick(s, 13.0, catalog)
    assertTrue(s.echoPulsesTriggered >= 1)
    assertTrue(s.ichor > ichorBeforeEcho)
    assertEquals("dlg_echo", s.activeDialogue?.id)

    // Excision IV: Valve Clamp locks manual press for 20s then triggers Marrow Bloom + Choir-Lung
    s = OrganDevEngine.setHighestMilestone(s, 4, catalog)
    s = OrganGameEngine.activateValveClamp(s, catalog)
    assertTrue(s.isValveClamped)
    val beforeBlocked = s.ichor
    assertEquals(beforeBlocked, OrganGameEngine.pressValve(s, catalog).ichor, 0.001)
    s = OrganGameEngine.tick(s, 21.0, catalog)
    assertEquals(1, s.marrowBloomCount)
    assertTrue(s.marrowBloomRemainingSeconds > 0.0)
    assertTrue(s.activeAnomalies.size >= 2)
    assertEquals("dlg_bloom", s.activeDialogue?.id)

    s =
      s.copy(
        creatures = s.creatures + (CreatureType.WEEPING_LAMP to 3),
        graftedTendrils = 2,
      )
    assertTrue(s.canAffordChoirLung)
    s = OrganGameEngine.performChoirLungGraft(s, catalog)
    assertEquals(1, s.choirLungs)
    assertEquals(1, s.creatureCount(CreatureType.WEEPING_LAMP))
    assertEquals(1, s.graftedTendrils)

    // Excision V: Ossified Throat unseals Redacted Cult Dossier lines permanently
    s = OrganDevEngine.setHighestMilestone(s, 5, catalog)
    s = s.copy(creatures = s.creatures + (CreatureType.SUMP_SIPHON to 3))
    assertTrue(s.canOfferToThroat(CultDossierLine.LINE_1_HYDRO_WARD))
    s = OrganGameEngine.offerToThroat(s, CultDossierLine.LINE_1_HYDRO_WARD, catalog)
    assertTrue(CultDossierLine.LINE_1_HYDRO_WARD in s.unredactedDossierLines)
    assertEquals(CultDossierLine.LINE_1_HYDRO_WARD.ichorMultiplier, s.dossierIchorMultiplier, 0.001)
    assertEquals("dlg_dossier_1", s.activeDialogue?.id)

    // Excision VI: Breach Ritual enters Arc 2 ("The Inquiry")
    s = OrganDevEngine.setHighestMilestone(s, 6, catalog)
    assertTrue(s.canBreachToArc2)
    val arc2 = OrganGameEngine.performBreachToArc2(s, catalog)
    assertEquals(GameArc.ARC_2_INVESTIGATION, arc2.activeArc)
    assertEquals("dlg_arc2_entry", arc2.activeDialogue?.id)
    assertEquals(6, ExcisionMilestone.entries.size)
  }

  @Test
  fun arc2InvestigatorEngine_drivesInspectSilenceContactsDeductionsAnomaliesFileZeroAndArc3() {
    val catalog = sampleCatalog()
    var state = OrganDevEngine.setActiveArc(OrganGameEngine.initialState(catalog), GameArc.ARC_2_INVESTIGATION, catalog)
    assertEquals(GameArc.ARC_2_INVESTIGATION, state.activeArc)

    state = Arc2InvestigatorEngine.inspectCaseFile(state, catalog)
    assertEquals(1L, state.arc2.leadsDisplay)

    // Late-Night Silence engages after 2.5s idle and creeps Complicity
    val complicityBefore = state.arc2.complicity
    state = Arc2InvestigatorEngine.tick(state, Arc2Balance.SILENCE_IDLE_DELAY_SEC + 1.0, catalog)
    assertTrue(state.arc2.isSilenceActive)
    assertTrue(state.arc2.complicity > complicityBefore)

    // Boost resources, recruit Contact, pin Deduction, spawn & click Desk Anomaly
    state = OrganDevEngine.assignArc2Resources(state, leads = 250_000.0, silence = 500.0, catalog = catalog)
    state = Arc2InvestigatorEngine.recruitContact(state, InvestigatorContact.NIGHT_ARCHIVIST, catalog)
    assertEquals(1, state.arc2.contactCount(InvestigatorContact.NIGHT_ARCHIVIST))
    assertTrue(state.arc2.leadsPerSecond > 0.0)

    state = Arc2InvestigatorEngine.pinDeduction(state, CaseDeduction.HYDRO_WARD_INVOICES, catalog)
    assertTrue(CaseDeduction.HYDRO_WARD_INVOICES in state.arc2.ownedDeductions)

    state = OrganDevEngine.spawnDeskAnomaly(state)
    assertEquals(1, state.arc2.activeDeskAnomalies.size)
    val anomalyId = state.arc2.activeDeskAnomalies.first().id
    state = Arc2InvestigatorEngine.interactDeskAnomaly(state, anomalyId, catalog)
    assertTrue(state.arc2.activeDeskAnomalies.isEmpty())

    // Connect Chapters 1..4 to reveal File #00 twist
    for (ch in 1..4) {
      assertTrue(state.arc2.canConnectNextChapter)
      state = Arc2InvestigatorEngine.connectNextChapter(state, catalog)
      assertEquals(ch, state.arc2.completedChapters)
    }
    assertTrue(state.arc2.fileZeroRevealed)
    assertEquals(1.0f, state.arc2.complicity, 0.001f)
    assertEquals("dlg_arc2_file_zero", state.activeDialogue?.id)

    // Cross into Arc 3 open convergence threshold
    val arc3 = Arc2InvestigatorEngine.enterArc3Convergence(state, catalog)
    assertEquals(GameArc.ARC_3_CONVERGENCE, arc3.activeArc)
    assertEquals("dlg_arc3_entry", arc3.activeDialogue?.id)
  }

  @Test
  fun devEngine_mutatesMilestonesResourcesScarNodesAnomaliesAndResetsCleanly() {
    val catalog = sampleCatalog()
    var state = OrganGameEngine.initialState(catalog)
    state = OrganDevEngine.setHighestMilestone(state, 5, catalog)
    state = OrganDevEngine.assignArc1Resources(state, ichor = 123_456.0, stillness = 89.0, availableScar = 14L, catalog = catalog)
    state = OrganDevEngine.setAllScarNodes(state, unlockAll = true, catalog = catalog)
    state = OrganDevEngine.assignArc2Resources(state, leads = 4_200.0, silence = 75.0, completedChapters = 2, catalog = catalog)
    state = OrganDevEngine.spawnWallAnomaly(state)

    assertEquals(5, state.highestExcisionMilestone)
    assertEquals(123_456L, state.ichorDisplay)
    assertEquals(89L, state.stillnessDisplay)
    assertEquals(14L, state.availableScarTissue)
    assertEquals(ScarNodeId.entries.size, state.unlockedScarNodes.size)
    assertEquals(4_200L, state.arc2.leadsDisplay)
    assertEquals(2, state.arc2.completedChapters)
    assertTrue(state.activeAnomalies.isNotEmpty())

    // Dev reset wipes everything back to clean Phase 0 Arc 1
    val reset = OrganDevEngine.resetAll(catalog)
    assertEquals(GameArc.ARC_1_CHAMBER, reset.activeArc)
    assertEquals(0, reset.highestExcisionMilestone)
    assertTrue(reset.unlockedScarNodes.isEmpty())
    assertNotNull(reset)
  }
}
