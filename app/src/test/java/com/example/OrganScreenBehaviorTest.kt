package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.AssetDialogueRepository
import com.example.data.OrganStateSerializer
import com.example.game.ActiveWallAnomaly
import com.example.game.CreatureType
import com.example.game.DredgedRelic
import com.example.game.OrganBalance
import com.example.game.OrganGameEngine
import com.example.game.OrganGameState
import com.example.game.RepeatableScarGraft
import com.example.game.ScarNodeId
import com.example.game.InvestigatorDeckTab
import com.example.game.OrganDeckTab
import com.example.game.SpecimenJar
import com.example.game.WallAnomalyType
import com.example.ui.OrganRoute
import com.example.ui.OrganScreen
import com.example.ui.OrganViewModel
import com.example.ui.components.EventDripGate
import com.example.ui.components.REPEAT_EVENT_DRIP_CHANCE
import com.example.ui.components.STORY_DRIP_QUEUE_GAP_MS
import com.example.ui.components.STORY_DRIP_TOTAL_MS
import com.example.ui.components.StoryDripQueue
import com.example.ui.components.StoryDripQueueState
import com.example.ui.components.TabAlertEvaluator
import com.example.ui.components.computeWallAnomalyBreathScale
import com.example.ui.components.computeWallLayerAlphas
import com.example.ui.components.partitionChamberShelfJars
import com.example.ui.components.specimenJarDrawableRes
import com.example.ui.components.wallAnomalyDrawableRes
import com.example.ui.components.weepingLampLayout
import com.example.ui.theme.BleakTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OrganScreenBehaviorTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createViewModel(): OrganViewModel {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return OrganViewModel(dialogueRepository = AssetDialogueRepository(context), autoTick = false)
  }

  @Test
  fun appNameResource_matchesExpectedIdentity() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("Tap Counter", context.getString(R.string.app_name))
  }

  @Test
  fun initialScreen_showsStarkValvedWallHidesDeckAndTriggersFirstFragmentOnTap() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val firstFragment =
      AssetDialogueRepository(context).loadCatalog().entries.first { it.trigger.threshold == 1L }
    val viewModel = createViewModel()

    composeTestRule.setContent { OrganRoute(viewModel = viewModel) }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("phase_header")
      .assertIsDisplayed()
      .assertTextEquals("THE COUNTING ROOM")
    // Diegetic progress bar & next room stage readout render without clinical PHASE / P0 meta-jargon
    composeTestRule.onNodeWithTag("phase_progress_bar").assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("threshold_status")
      .assertIsDisplayed()
      .assertTextEquals("NEXT: UNCANNY DRIFT · 0 / 75")
    composeTestRule.onNodeWithTag("chamber_wall_art").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chamber_wall_phase_0").assertIsDisplayed()
    composeTestRule.onNodeWithTag("suture_hands_overlay").assertDoesNotExist()
    composeTestRule.onNodeWithTag("valve_artwork", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("counter_text").assertTextEquals("0")
    composeTestRule
      .onNodeWithTag("increment_button")
      .assertIsDisplayed()
      .assert(hasClickAction())
      .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
      .assertContentDescriptionEquals("Press the recessed wall valve to extract ichor")

    // Progressive disclosure: Apparatus Deck is hidden at count 0
    composeTestRule.onNodeWithTag("apparatus_deck").assertDoesNotExist()
    composeTestRule.onNodeWithTag("dialogue_body").assertTextEquals("AWAITING INPUT_")

    // First valve press updates counter and loads real Phase 0 fragment from dialogues.json
    composeTestRule.onNodeWithTag("increment_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("counter_text").assertTextEquals("1")
    composeTestRule
      .onNodeWithTag("dialogue_speaker")
      .assertTextEquals("// ${firstFragment.speaker.uppercase()}")
    composeTestRule
      .onNodeWithTag("dialogue_body")
      .assertTextEquals(firstFragment.lines.joinToString("\n"))
  }

  @Test
  fun phaseOneProgression_rendersPhaseOneGraphicsUnlocksOrganismsSpawnsToothAndOpensArchive() {
    val viewModel = createViewModel()
    composeTestRule.setContent { OrganRoute(viewModel = viewModel) }
    composeTestRule.waitForIdle()

    // Tap 75 times to enter Phase 1 (Uncanny Drift), revealing both Suture-Hand & Leech-Clerk
    repeat(75) { composeTestRule.onNodeWithTag("increment_button").performClick() }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("phase_header")
      .assertTextEquals("UNCANNY DRIFT")
    composeTestRule.onNodeWithTag("chamber_wall_art").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chamber_wall_phase_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("apparatus_deck").performScrollTo().assertIsDisplayed()

    // Verify Phase 1 wet-lab organism dossier cards & portraits render with initial DORMANT status
    composeTestRule
      .onNodeWithTag("organism_status_suture_hand")
      .performScrollTo()
      .assertIsDisplayed()
      .assertTextEquals("DORMANT")
    composeTestRule
      .onNodeWithTag("organism_yield_suture_hand")
      .performScrollTo()
      .assertIsDisplayed()
      .assertTextEquals("EACH: +0.6/s")
    composeTestRule
      .onNodeWithTag("organism_affordability_suture_hand")
      .performScrollTo()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("art_suture_hand", useUnmergedTree = true)
      .performScrollTo()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("art_leech_clerk", useUnmergedTree = true)
      .performScrollTo()
      .assertIsDisplayed()

    // Buy Suture-Hand (25 Ichor) via UI -> status flips to PRESSING VALVE, colony rate updates,
    // and Suture-Hands anatomical overlay appears directly on valve rim
    composeTestRule.onNodeWithTag("buy_suture_hand").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, viewModel.uiState.value.creatureCount(CreatureType.SUTURE_HAND))
    composeTestRule
      .onNodeWithTag("organism_status_suture_hand")
      .performScrollTo()
      .assertTextEquals("TURNING IRON")
    composeTestRule
      .onNodeWithTag("organism_yield_suture_hand")
      .performScrollTo()
      .assertTextEquals("EACH: +0.6/s · COLONY: +0.6/s")
    composeTestRule.onNodeWithTag("suture_hands_overlay").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("ichor_rate_text").performScrollTo().assertIsDisplayed()

    // Advance time to spawn Pale Tooth anomaly directly on the chamber wall and pull it
    composeTestRule.runOnIdle {
      viewModel.advanceTime(OrganBalance.FIRST_ANOMALY_DELAY_SEC + 0.5)
    }
    composeTestRule.waitForIdle()

    val toothId = viewModel.uiState.value.activeAnomalies.first().id
    val ichorBeforePull = viewModel.uiState.value.ichorDisplay
    composeTestRule.onNodeWithTag("anomaly_pale_tooth_$toothId").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    assertTrue(viewModel.uiState.value.ichorDisplay > ichorBeforePull)
    composeTestRule.onNodeWithTag("anomaly_pale_tooth_$toothId").assertDoesNotExist()

    // Pinned story ledger remains directly visible at bottom without scrolling even after deck unlocks
    composeTestRule.onNodeWithTag("dialogue_panel").assertIsDisplayed()
    composeTestRule.onNodeWithTag("dialogue_body").assertIsDisplayed()

    // Open Notes Archive sheet directly from pinned bottom ledger (no scrolling required)
    composeTestRule.onNodeWithTag("open_archive_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("fragment_archive_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithText("// RECOVERED FRAGMENTS").assertIsDisplayed()
  }

  @Test
  fun stillnessAndJarsAndGrafting_workEndToEndThroughUiDeck() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()
    var state by
      mutableStateOf(
        OrganGameEngine.initialState(catalog).copy(
          ichor = 17_000.0,
          cycleLifetimeIchor = 17_000.0,
          stillness = 70.0,
          phase = 3,
          phaseName = catalog.phaseName(3),
          creatures =
            mapOf(
              CreatureType.SUTURE_HAND to 2,
              CreatureType.LEECH_CLERK to 2,
              CreatureType.WEEPING_LAMP to 1,
            ),
          isStillnessActive = true,
          gazeRemainingSeconds = 8.0,
        )
      )

    composeTestRule.setContent {
      BleakTheme(phase = state.phase) {
        OrganScreen(
          state = state,
          onPressValve = { state = OrganGameEngine.pressValve(state, catalog) },
          onBuyCreature = { type -> state = OrganGameEngine.buyCreature(state, type, catalog) },
          onBottleJar = { jar -> state = OrganGameEngine.bottleJar(state, jar, catalog) },
          onPerformGraft = { state = OrganGameEngine.performGraft(state, catalog) },
          onInteractAnomaly = { id -> state = OrganGameEngine.interactAnomaly(state, id, catalog) },
          onPerformExcision = { state = OrganGameEngine.performExcision(state, catalog) },
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("stillness_active_badge").assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("gaze_active_badge")
      .assertIsDisplayed()
      .assertTextEquals("EYE GAZE (8s)")
    // Living chamber organisms: Suture-Hands on valve, patrolling Leech-Clerks on counter glass, foreground Weeping Lamps
    composeTestRule.onNodeWithTag("suture_hands_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("leech_clerks_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("weeping_lamps_overlay").assertIsDisplayed()

    // Verify Weeping Lamp dossier card reflects active Stillness state & Stillness drip telemetry
    composeTestRule
      .onNodeWithTag("organism_status_leech_clerk")
      .performScrollTo()
      .assertTextEquals("GRAZING GLASS")
    composeTestRule
      .onNodeWithTag("organism_status_weeping_lamp")
      .performScrollTo()
      .assertTextEquals("WEEPING")
    composeTestRule
      .onNodeWithTag("art_weeping_lamp", useUnmergedTree = true)
      .performScrollTo()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("organism_stillness_yield_weeping_lamp")
      .performScrollTo()
      .assertIsDisplayed()

    // Grafting now lives directly inside ORGANISMS alongside living colonies
    composeTestRule.onNodeWithTag("art_graft_tendril", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("perform_graft_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, state.graftedTendrils)
    assertTrue(state.isGraftOnCooldown)
    composeTestRule.onNodeWithText("GRAFTED CHOIR-TENDRIL [1]").assertIsDisplayed()
    composeTestRule.onNodeWithTag("graft_cooldown_bar").performScrollTo().assertIsDisplayed()

    // JARS tab holds unlocked Specimen Jars only (locked Phase 4 / Cycle 1 jars remain hidden)
    composeTestRule.onNodeWithTag("deck_tab_jars_and_graft").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("art_jar_yesterdays_dust", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("jar_a_borrowed_pulse").assertDoesNotExist()
    composeTestRule.onNodeWithTag("jar_the_uncounted_hour").assertDoesNotExist()
    composeTestRule.onNodeWithTag("jar_yesterdays_dust").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertTrue(SpecimenJar.YESTERDAYS_DUST in state.ownedJars)
    composeTestRule.onNodeWithText("SEALED").assertIsDisplayed()
  }

  @Test
  fun phaseFourExcisionProgressAndSeverance_showsMassProgressThenResetsChamberToCycleOne() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()
    val excisionFragment = catalog.entries.first { it.trigger.type == "excision_completed" }

    val viewModel = createViewModel()
    // Build up through real engine steps to Phase 4 entry (85,000+ but < 150,000)
    repeat(75) { viewModel.pressValve() }
    viewModel.buyCreature(CreatureType.SUTURE_HAND)
    viewModel.advanceTime(300.0)
    viewModel.buyCreature(CreatureType.LEECH_CLERK)
    viewModel.advanceTime(500.0)
    viewModel.buyCreature(CreatureType.WEEPING_LAMP)
    // Idle under Stillness so Weeping Lamp drives Ichor cleanly past 85,000 into Phase 4 (< 150,000)
    viewModel.advanceTime(6_000.0)
    assertEquals(4, viewModel.uiState.value.phase)

    composeTestRule.setContent { OrganRoute(viewModel = viewModel) }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("chamber_wall_phase_4").assertIsDisplayed()

    if (!viewModel.uiState.value.canExcise) {
      // Phase 4 pre-Excision displays progress toward First Severance while hiding Cycle 0 EXCISION tab
      composeTestRule.onNodeWithTag("phase_progress_bar").assertIsDisplayed()
      composeTestRule
        .onNodeWithTag("threshold_status")
        .assertIsDisplayed()
        .assertTextEquals(
          "NEXT: FIRST SEVERANCE · ${String.format(java.util.Locale.US, "%,d", viewModel.uiState.value.cycleLifetimeIchorDisplay)} / 210,000"
        )
      composeTestRule.onNodeWithTag("deck_tab_excision").assertDoesNotExist()
      composeTestRule.runOnIdle { viewModel.advanceTime(9_000.0) }
      composeTestRule.waitForIdle()
    }

    assertTrue("Expected Excision to be ready in Phase 4", viewModel.uiState.value.canExcise)

    composeTestRule
      .onNodeWithTag("threshold_status")
      .assertTextEquals("SEAM READY // FIRST SEVERANCE")
    composeTestRule
      .onNodeWithTag("deck_tab_alert_excision", useUnmergedTree = true)
      .performScrollTo()
      .assertIsDisplayed()

    // Open EXCISION tab -> verify Next Scar visual indicator -> Arm -> Confirm Severance
    composeTestRule.onNodeWithTag("deck_tab_excision").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("next_scar_status").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("next_scar_progress_bar").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("excision_reward_text").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("excision_arm_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("excision_confirm_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Verify full Cycle 1 post-reset state: top header stays decluttered (no prestige_passive_banner),
    // and relocated cycle/resonance telemetry renders inside Excision tab
    assertEquals(1, viewModel.uiState.value.cycle)
    assertTrue(viewModel.uiState.value.scarTissue >= 1L)

    composeTestRule
      .onNodeWithTag("phase_header")
      .assertTextEquals("THE COUNTING ROOM")
    composeTestRule.onNodeWithTag("prestige_passive_banner").assertDoesNotExist()
    composeTestRule.onNodeWithTag("counter_text").performScrollTo().assertTextEquals("0")

    // Switch to EXCISION tab to inspect relocated cycle & permanent resonance status
    composeTestRule.onNodeWithTag("deck_tab_excision").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("cycle_status")
      .performScrollTo()
      .assertIsDisplayed()
      .assertTextEquals(
        "CYCLE 01 · FIRST SEVERANCE · TOTAL SCAR ${viewModel.uiState.value.scarTissue}"
      )
    composeTestRule.onNodeWithTag("scar_passive_status").performScrollTo().assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("dialogue_speaker")
      .assertIsDisplayed()
      .assertTextEquals("// ${excisionFragment.speaker.uppercase()}")
    composeTestRule
      .onNodeWithTag("dialogue_body")
      .assertIsDisplayed()
      .assertTextEquals(excisionFragment.lines.joinToString("\n"))

    // Reaching 75 Ichor in Cycle 1 unlocks the new prestige-bound handwriting fragment (50 * 1.5535 = 77.6 >= 75)
    val scarHandwritingFragment =
      catalog.entries.first { it.id == "dlg_cycle_1_scar_handwriting" }
    repeat(50) { viewModel.pressValve() }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("dialogue_speaker")
      .assertIsDisplayed()
      .assertTextEquals("// ${scarHandwritingFragment.speaker.uppercase()}")
    composeTestRule
      .onNodeWithTag("dialogue_body")
      .assertIsDisplayed()
      .assertTextEquals(scarHandwritingFragment.lines.joinToString("\n"))
  }

  @Test
  fun phaseFourChitinRibAnomaly_rendersPryActionGrantsBurstGazeAndTriggersMarrowSeamFragment() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()
    val ribFragment = catalog.entries.first { it.id == "dlg_p4_chitin_rib" }

    var state by
      mutableStateOf(
        OrganGameEngine.initialState(catalog).copy(
          ichor = 90_000.0,
          cycleLifetimeIchor = 90_000.0,
          stillness = 10.0,
          phase = 4,
          phaseName = catalog.phaseName(4),
          activeAnomalies = listOf(ActiveWallAnomaly(77L, WallAnomalyType.CHITIN_RIB, 0.82f, 0.28f)),
        )
      )

    composeTestRule.setContent {
      BleakTheme(phase = state.phase) {
        OrganScreen(
          state = state,
          onPressValve = { state = OrganGameEngine.pressValve(state, catalog) },
          onBuyCreature = { type -> state = OrganGameEngine.buyCreature(state, type, catalog) },
          onBottleJar = { jar -> state = OrganGameEngine.bottleJar(state, jar, catalog) },
          onPerformGraft = { state = OrganGameEngine.performGraft(state, catalog) },
          onInteractAnomaly = { id -> state = OrganGameEngine.interactAnomaly(state, id, catalog) },
          onPerformExcision = { state = OrganGameEngine.performExcision(state, catalog) },
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("anomaly_chitin_rib_77")
      .assertIsDisplayed()
      .assertContentDescriptionEquals("Pry cracked calcified rib parting the chamber wall")
      .performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("anomaly_chitin_rib_77").assertDoesNotExist()
    composeTestRule
      .onNodeWithTag("gaze_active_badge")
      .assertIsDisplayed()
      .assertTextEquals("EYE GAZE (6s)")
    composeTestRule
      .onNodeWithTag("dialogue_speaker")
      .assertIsDisplayed()
      .assertTextEquals("// ${ribFragment.speaker.uppercase()}")
    composeTestRule
      .onNodeWithTag("dialogue_body")
      .assertIsDisplayed()
      .assertTextEquals(ribFragment.lines.joinToString("\n"))
  }

  @Test
  fun clinicalDevConsole_unlocksMilestonesAssignsScarSuturesTreeNodeAndShowsChamberRites() {
    val viewModel = createViewModel()
    composeTestRule.setContent { OrganRoute(viewModel = viewModel) }
    composeTestRule.waitForIdle()

    // Open Clinical Dev Console from header pill
    composeTestRule.onNodeWithTag("open_dev_tools_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dev_tools_dialog").assertIsDisplayed()

    // Unlock Milestone 5 & grant +50 Scar Tissue, then close modal
    composeTestRule.onNodeWithTag("dev_milestone_5").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dev_add_50_scar").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dev_close_button").performClick()
    composeTestRule.waitForIdle()

    assertEquals(5, viewModel.uiState.value.highestExcisionMilestone)
    assertEquals(50L, viewModel.uiState.value.availableScarTissue)

    // Switch to SCARS tab and suture the root FIRST_INCISION node
    composeTestRule.onNodeWithTag("deck_tab_scar_tree").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("scar_tree_panel").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("suture_selected_node_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    assertEquals(49L, viewModel.uiState.value.availableScarTissue)
    assertTrue(viewModel.uiState.value.unlockedScarNodes.isNotEmpty())

    // Switch to RITES tab and verify Excision II-V chamber rites sections all render
    composeTestRule.onNodeWithTag("deck_tab_chamber_rites").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("rite_drain_section").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("rite_echo_section").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("rite_clamp_section").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("rite_throat_section").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun arc2InvestigatorDeskAndFileZeroTwist_flowsThroughInspectBoardRevealAndArc3Threshold() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()
    val fileZeroFragment = catalog.entries.first { it.id == "dlg_arc2_ch4_file_zero" }
    val viewModel = createViewModel()

    composeTestRule.setContent { OrganRoute(viewModel = viewModel) }
    composeTestRule.waitForIdle()

    // Jump directly to Arc 2 and boost Leads/Silence via Clinical Dev Console
    composeTestRule.onNodeWithTag("open_dev_tools_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dev_arc_2").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dev_boost_arc2_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dev_close_button").performClick()
    composeTestRule.waitForIdle()

    // Verify Haunted Investigator Desk renders with pure diegetic header (no CH 0/4 or ARC 02)
    composeTestRule.onNodeWithTag("investigator_screen").assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("arc2_phase_header")
      .assertIsDisplayed()
      .assertTextEquals("THE INQUIRY · CASE #14-B")
    val leadsBefore = viewModel.uiState.value.arc2.leadsDisplay
    composeTestRule.onNodeWithTag("inspect_case_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertTrue(viewModel.uiState.value.arc2.leadsDisplay > leadsBefore)

    // Switch to BOARD tab: future locked threads stay hidden and visible threads use pure titles (no CH 01 //)
    composeTestRule
      .onNodeWithTag("arc2_tab_alert_evidence_board", useUnmergedTree = true)
      .performScrollTo()
      .assertIsDisplayed()
    composeTestRule.onNodeWithTag("arc2_tab_evidence_board").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("THE MISSING CARETAKERS").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("chapter_connect_2").assertDoesNotExist()
    composeTestRule.onNodeWithTag("chapter_connect_4").assertDoesNotExist()
    for (ch in 1..4) {
      composeTestRule.onNodeWithTag("chapter_connect_$ch").performScrollTo().performClick()
      composeTestRule.waitForIdle()
    }

    assertTrue(viewModel.uiState.value.arc2.fileZeroRevealed)
    composeTestRule
      .onNodeWithTag("arc2_phase_header")
      .performScrollTo()
      .assertTextEquals("FILE #00 — SUBJECT INTAKE")
    composeTestRule.onNodeWithTag("file_zero_reveal_card").performScrollTo().assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("dialogue_body")
      .assertIsDisplayed()
      .assertTextEquals(fileZeroFragment.lines.joinToString("\n"))

    // Enter Convergence threshold with pure diegetic navigation labels (no ARC 1/2/3)
    composeTestRule
      .onNodeWithTag("enter_arc3_button")
      .performScrollTo()
      .assertTextEquals("STEP PAST THE SEAM")
      .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("arc3_screen").assertIsDisplayed()
    composeTestRule.onNodeWithText("CONVERGENCE THRESHOLD").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("RETURN TO THE CHAMBER").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("RETURN TO THE DESK").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun dredgedRelics_areTappableGraphicChipsRevealingThematicInspectionNotes() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()
    var state by
      mutableStateOf(
        OrganGameEngine.initialState(catalog).copy(
          highestExcisionMilestone = 2,
          cycle = 1,
          dredgedRelics = setOf(DredgedRelic.PATIENT_WRISTBAND),
        )
      )

    composeTestRule.setContent {
      BleakTheme(phase = state.phase) {
        OrganScreen(
          state = state,
          onPressValve = {},
          onBuyCreature = {},
          onBottleJar = {},
          onPerformGraft = {},
          onInteractAnomaly = {},
          onPerformExcision = {},
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("deck_tab_chamber_rites").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Locked relic reveals sunken thematic hint
    composeTestRule.onNodeWithTag("relic_inspect_brass_ward_key").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("relic_inspection_card").performScrollTo().assertIsDisplayed()
    composeTestRule
      .onNodeWithText("Still sunk beneath the grate. Seal the floor until it rises.")
      .assertIsDisplayed()

    // Owned relic reveals its thematic inspection note
    composeTestRule.onNodeWithTag("relic_inspect_patient_wristband").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithText("Sodden tag #14 — the valve pulls heavier from beneath the mortar.")
      .assertIsDisplayed()

    // Tapping the same relic again collapses inspection card
    composeTestRule.onNodeWithTag("relic_inspect_patient_wristband").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("relic_inspection_card").assertDoesNotExist()
  }

  @Test
  fun drippingMarginOverlay_slowsStoryDripSuppressesEventDripDuringStoryAndShowsEventWhenIdle() {
    assertEquals(16_000L, STORY_DRIP_TOTAL_MS)
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()
    var state by
      mutableStateOf(
        OrganGameEngine.initialState(catalog).copy(
          ichor = 90.0,
          cycleLifetimeIchor = 90.0,
          phase = 1,
          phaseName = catalog.phaseName(1),
          activeAnomalies = listOf(ActiveWallAnomaly(12L, WallAnomalyType.PALE_TOOTH, 0.2f, 0.25f)),
        )
      )

    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      BleakTheme(phase = state.phase) {
        OrganScreen(
          state = state,
          onPressValve = { state = OrganGameEngine.pressValve(state, catalog) },
          onBuyCreature = {},
          onBottleJar = {},
          onPerformGraft = {},
          onInteractAnomaly = { id -> state = OrganGameEngine.interactAnomaly(state, id, catalog) },
          onPerformExcision = {},
        )
      }
    }
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.waitForIdle()

    // Pulling first Pale Tooth triggers a story fragment -> story drip displays while chamber event drip is hidden
    composeTestRule.onNodeWithTag("anomaly_pale_tooth_12").performClick()
    composeTestRule.mainClock.advanceTimeBy(600)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("dripping_story_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("dripping_event_overlay").assertDoesNotExist()

    // Still active past 12 seconds (slower 16s story drip)
    composeTestRule.mainClock.advanceTimeBy(12_000)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dripping_story_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("dripping_event_overlay").assertDoesNotExist()

    // Once 16s story drip completes, a subsequent chamber anomaly without a new story beat shows the chamber event drip
    composeTestRule.mainClock.advanceTimeBy(4_500)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dripping_story_overlay").assertDoesNotExist()

    composeTestRule.runOnIdle {
      state =
        state.copy(
          activeAnomalies = listOf(ActiveWallAnomaly(13L, WallAnomalyType.PALE_TOOTH, 0.25f, 0.3f))
        )
    }
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("anomaly_pale_tooth_13").performClick()
    composeTestRule.mainClock.advanceTimeBy(600)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("dripping_story_overlay").assertDoesNotExist()
    composeTestRule.onNodeWithTag("dripping_event_overlay").assertIsDisplayed()
  }

  @Test
  fun stateSerializer_roundTripsGraftCooldownsLateJarsAndRepeatableRanksAndLoadsLegacyJson() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()
    val state =
      OrganGameEngine.initialState(catalog).copy(
        cycle = 1,
        highestExcisionMilestone = 4,
        stillness = 420.0,
        graftedTendrils = 2,
        choirLungs = 1,
        graftCooldownRemainingSeconds = 4.25,
        choirLungCooldownRemainingSeconds = 7.5,
        ownedJars = setOf(SpecimenJar.SALT_FROM_THE_GROUT, SpecimenJar.THE_UNCOUNTED_HOUR),
        unlockedScarNodes = setOf(ScarNodeId.FIRST_INCISION, ScarNodeId.MEMBRANE_RECALL),
        repeatableScarRanks =
          mapOf(
            RepeatableScarGraft.KELOID_THICKENING to 2,
            RepeatableScarGraft.SCAR_HARVEST_HOOK to 1,
          ),
      )

    val raw = OrganStateSerializer.serialize(state)
    val restored = OrganStateSerializer.deserialize(raw, catalog)

    assertNotNull(restored)
    assertEquals(4.25, restored!!.graftCooldownRemainingSeconds, 0.001)
    assertEquals(7.5, restored.choirLungCooldownRemainingSeconds, 0.001)
    assertTrue(SpecimenJar.SALT_FROM_THE_GROUT in restored.ownedJars)
    assertTrue(SpecimenJar.THE_UNCOUNTED_HOUR in restored.ownedJars)
    assertTrue(ScarNodeId.MEMBRANE_RECALL in restored.unlockedScarNodes)
    assertEquals(2, restored.repeatableRank(RepeatableScarGraft.KELOID_THICKENING))
    assertEquals(1, restored.repeatableRank(RepeatableScarGraft.SCAR_HARVEST_HOOK))

    // Backwards compatibility: legacy JSON missing repeatableScarRanks loads safely with empty map
    val legacyJson =
      """{"version":2,"cycle":2,"highestExcisionMilestone":2,"scarTissue":6,"availableScarTissue":4,"ichor":500,"cycleLifetimeIchor":500}"""
    val migrated = OrganStateSerializer.deserialize(legacyJson, catalog)
    assertNotNull(migrated)
    assertTrue(migrated!!.repeatableScarRanks.isEmpty())
    assertEquals(2, migrated.highestExcisionMilestone)
  }

  @Test
  fun scarTreePanel_rendersFiveTierNodesAndUnlocksRepeatableGraftsAfterFirstIncision() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()
    var state by
      mutableStateOf(
        OrganGameEngine.initialState(catalog).copy(
          cycle = 1,
          highestExcisionMilestone = 1,
          scarTissue = 20L,
          availableScarTissue = 20L,
        )
      )

    composeTestRule.setContent {
      BleakTheme(phase = state.phase) {
        OrganScreen(
          state = state,
          onPressValve = {},
          onBuyCreature = {},
          onBottleJar = {},
          onPerformGraft = {},
          onInteractAnomaly = {},
          onPerformExcision = {},
          onUnlockScarNode = { node -> state = OrganGameEngine.unlockScarNode(state, node, catalog) },
          onBuyRepeatableScarGraft = { graft ->
            state = OrganGameEngine.buyRepeatableScarGraft(state, graft, catalog)
          },
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("deck_tab_scar_tree").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Tier 4 and Tier 5 nodes render across all 3 branches
    composeTestRule.onNodeWithTag("scar_node_membrane_recall").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("scar_node_perpetual_seam").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("scar_node_hollow_lung_choir").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("scar_node_catatonic_tide").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("scar_node_bloom_incision").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("scar_node_bone_resonance").performScrollTo().assertIsDisplayed()

    // Repeatable grafts stay completely hidden until FIRST_INCISION is sutured
    composeTestRule.onNodeWithTag("repeatable_scar_grafts_section").assertDoesNotExist()
    composeTestRule.onNodeWithTag("repeatable_grafts_locked_hint").assertDoesNotExist()

    // Suture FIRST_INCISION -> unlocks repeatable grafts tray and allows buying Keloid Thickening
    composeTestRule.onNodeWithTag("suture_selected_node_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertTrue(ScarNodeId.FIRST_INCISION in state.unlockedScarNodes)

    composeTestRule.onNodeWithTag("repeatable_scar_grafts_section").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("repeatable_graft_keloid_thickening").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, state.repeatableRank(RepeatableScarGraft.KELOID_THICKENING))
    assertEquals(18L, state.availableScarTissue) // 20 - 1 (First Incision) - 1 (Keloid Rank 1)

    // Subsequent purchases follow 1, 2, 3 Scar costs
    composeTestRule.onNodeWithTag("repeatable_graft_keloid_thickening").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(2, state.repeatableRank(RepeatableScarGraft.KELOID_THICKENING))
    assertEquals(17L, state.availableScarTissue) // 18 - 1 (Rank 2)

    composeTestRule.onNodeWithTag("repeatable_graft_keloid_thickening").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(3, state.repeatableRank(RepeatableScarGraft.KELOID_THICKENING))
    assertEquals(15L, state.availableScarTissue) // 17 - 2 (Rank 3)

    composeTestRule.onNodeWithTag("repeatable_graft_keloid_thickening").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(4, state.repeatableRank(RepeatableScarGraft.KELOID_THICKENING))
    assertEquals(12L, state.availableScarTissue) // 15 - 3 (Rank 4)
  }

  @Test
  fun excisionPanel_distinguishesFarmVsMilestonePushAndDisplaysAntiSkipSeamHoldNote() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()
    var state by
      mutableStateOf(
        OrganGameEngine.initialState(catalog).copy(
          cycle = 1,
          highestExcisionMilestone = 1,
          ichor = 300_000.0,
          cycleLifetimeIchor = 300_000.0,
          phase = 4,
          phaseName = catalog.phaseName(4),
        )
      )

    composeTestRule.setContent {
      BleakTheme(phase = state.phase) {
        OrganScreen(
          state = state,
          onPressValve = {},
          onBuyCreature = {},
          onBottleJar = {},
          onPerformGraft = {},
          onInteractAnomaly = {},
          onPerformExcision = { state = OrganGameEngine.performExcision(state, catalog) },
        )
      }
    }
    composeTestRule.waitForIdle()

    // Early Scar-farming run below M2 (1.8M) displays HARVEST SCARS EARLY button and no seam hold note
    composeTestRule.onNodeWithTag("deck_tab_excision").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("HARVEST SCARS EARLY (+2)").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("excision_seam_hold_note").assertDoesNotExist()

    // At 1.2M (past 3rd scar at 811k, below M2 at 1.8M), Next Scar clamps to 1,800,000 instead of 2,116,667
    composeTestRule.runOnIdle {
      state = state.copy(ichor = 1_200_000.0, cycleLifetimeIchor = 1_200_000.0)
    }
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("next_scar_status")
      .performScrollTo()
      .assertTextEquals("NEXT SCAR TISSUE: 1,200,000 / 1,800,000")
    composeTestRule.onNodeWithText("HARVEST SCARS EARLY (+3)").performScrollTo().assertIsDisplayed()

    // Reaching exact M2 threshold (1.8M) grants the pulled 4th Scar early right on the milestone
    composeTestRule.runOnIdle {
      state = state.copy(ichor = 1_800_000.0, cycleLifetimeIchor = 1_800_000.0)
    }
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("excision_arm_button")
      .performScrollTo()
      .assertTextEquals("SEVER: THE SUNKEN DRAIN (+4 SCAR)")

    // Jump to 30M Ichor (exceeds both M2 1.8M and M3 24M) -> displays SEVER: THE SUNKEN DRAIN + seam hold note
    composeTestRule.runOnIdle {
      state = state.copy(ichor = 30_000_000.0, cycleLifetimeIchor = 30_000_000.0)
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("excision_seam_hold_note").performScrollTo().assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("excision_arm_button")
      .performScrollTo()
      .assertTextEquals("SEVER: THE SUNKEN DRAIN (+${state.pendingScarTissue} SCAR)")
      .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("excision_confirm_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Hard sequential cap: even with 30M mass, advances strictly to THE SUNKEN DRAIN (M2)
    assertEquals(2, state.highestExcisionMilestone)
  }

  @Test
  fun excisionSixPleadForSalvation_rendersReframedClimaxUiAndTransitionsToArc2CaseFile() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()
    val arc2PleaEntry = catalog.entries.first { it.id == "dlg_arc2_entry" }
    val viewModel = createViewModel()

    // Prepare Excision V cleared + 160B cycle mass so Excision VI ("Plead for Salvation") opens cleanly
    viewModel.devSetHighestMilestone(5)
    viewModel.devAssignArc1Resources(ichor = 160_000_000_000.0)

    composeTestRule.setContent { OrganRoute(viewModel = viewModel) }
    composeTestRule.waitForIdle()

    // Top threshold status reflects Plead for Salvation
    composeTestRule
      .onNodeWithTag("threshold_status")
      .assertIsDisplayed()
      .assertTextEquals("SEAM UNSEALED // PLEAD FOR SALVATION")

    // Excision panel displays pure Plead for Salvation heading & button (no EXCISION VI // prefix)
    composeTestRule.onNodeWithTag("deck_tab_excision").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("EXCISION VI // PLEAD FOR SALVATION").assertDoesNotExist()
    composeTestRule
      .onNodeWithTag("breach_to_arc2_button")
      .performScrollTo()
      .assertIsDisplayed()
      .assertTextEquals("PLEAD FOR SALVATION")
      .performClick()
    composeTestRule.waitForIdle()

    // Transitions directly into Arc 2 Investigator Desk with the received Case Folder #14-B distress plea
    composeTestRule.onNodeWithTag("investigator_screen").assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("dialogue_speaker")
      .assertIsDisplayed()
      .assertTextEquals("// ${arc2PleaEntry.speaker.uppercase()}")
    composeTestRule
      .onNodeWithTag("dialogue_body")
      .assertIsDisplayed()
      .assertTextEquals(arc2PleaEntry.lines.joinToString("\n"))
  }

  @Test
  fun audioMuteToggle_startsUnmutedTogglesStateAndRemainsAccessibleAcrossArcs() {
    val viewModel = createViewModel()
    composeTestRule.setContent { OrganRoute(viewModel = viewModel) }
    composeTestRule.waitForIdle()

    // Starts unmuted by default on Arc 1 header
    assertEquals(false, viewModel.isAudioMuted.value)
    composeTestRule
      .onNodeWithTag("audio_mute_toggle")
      .assertIsDisplayed()
      .assert(hasClickAction())
      .assertContentDescriptionEquals("Mute sound effects and room ambience")
    composeTestRule.onNodeWithText("SFX").assertIsDisplayed()

    // Tap to mute -> updates state, label, and accessibility description
    composeTestRule.onNodeWithTag("audio_mute_toggle").performClick()
    composeTestRule.waitForIdle()
    assertEquals(true, viewModel.isAudioMuted.value)
    composeTestRule
      .onNodeWithTag("audio_mute_toggle")
      .assertContentDescriptionEquals("Unmute sound effects and room ambience")
    composeTestRule.onNodeWithText("MUTE").assertIsDisplayed()

    // Remains visible and synced across Arc 2 Investigator Desk and Arc 3 Convergence
    composeTestRule.runOnIdle { viewModel.devSetActiveArc(com.example.game.GameArc.ARC_2_INVESTIGATION) }
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("audio_mute_toggle")
      .assertIsDisplayed()
      .assertContentDescriptionEquals("Unmute sound effects and room ambience")

    composeTestRule.runOnIdle { viewModel.devSetActiveArc(com.example.game.GameArc.ARC_3_CONVERGENCE) }
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("audio_mute_toggle")
      .assertIsDisplayed()
      .performClick()
    composeTestRule.waitForIdle()
    assertEquals(false, viewModel.isAudioMuted.value)
    composeTestRule
      .onNodeWithTag("audio_mute_toggle")
      .assertContentDescriptionEquals("Mute sound effects and room ambience")
  }

  @Test
  fun tabAlertsHiddenUnlocksAndChoirLungInOrganisms_workAcrossArc1AndArc2() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()

    // 1. Pure TabAlertEvaluator assertions (Arc 1 + Arc 2)
    val phase2State =
      OrganGameEngine.initialState(catalog).copy(
        ichor = 1_700.0,
        cycleLifetimeIchor = 1_700.0,
        phase = 2,
        phaseName = catalog.phaseName(2),
        stillness = 0.0,
      )
    assertTrue(
      TabAlertEvaluator.shouldAlertArc1Tab(
        state = phase2State,
        tab = OrganDeckTab.JARS_AND_GRAFT,
        activeTab = OrganDeckTab.APPARATUS,
        seenKeysByTab = emptyMap(),
      )
    )
    // Once JARS tab keys are seen and no jar is affordable, alert clears
    val seenJars =
      mapOf(
        OrganDeckTab.JARS_AND_GRAFT to
          TabAlertEvaluator.arc1UnlockKeys(phase2State, OrganDeckTab.JARS_AND_GRAFT)
      )
    assertFalse(
      TabAlertEvaluator.shouldAlertArc1Tab(
        state = phase2State,
        tab = OrganDeckTab.JARS_AND_GRAFT,
        activeTab = OrganDeckTab.APPARATUS,
        seenKeysByTab = seenJars,
      )
    )
    // Becoming able to afford a jar re-triggers tab readiness pulse while off-tab
    val affordableJarState = phase2State.copy(stillness = 30.0)
    assertTrue(
      TabAlertEvaluator.shouldAlertArc1Tab(
        state = affordableJarState,
        tab = OrganDeckTab.JARS_AND_GRAFT,
        activeTab = OrganDeckTab.APPARATUS,
        seenKeysByTab = seenJars,
      )
    )
    // Active tab never pulses
    assertFalse(
      TabAlertEvaluator.shouldAlertArc1Tab(
        state = affordableJarState,
        tab = OrganDeckTab.JARS_AND_GRAFT,
        activeTab = OrganDeckTab.JARS_AND_GRAFT,
        seenKeysByTab = emptyMap(),
      )
    )
    // Choir-Tendril & Choir-Lung keys and readiness belong to APPARATUS (ORGANISMS)
    val m4GraftReady =
      phase2State.copy(
        phase = 3,
        cycle = 1,
        highestExcisionMilestone = 4,
        stillness = 250.0,
        graftedTendrils = 1,
        creatures =
          mapOf(
            CreatureType.SUTURE_HAND to 2,
            CreatureType.LEECH_CLERK to 2,
            CreatureType.WEEPING_LAMP to 2,
          ),
      )
    val apparatusKeys = TabAlertEvaluator.arc1UnlockKeys(m4GraftReady, OrganDeckTab.APPARATUS)
    assertTrue("graft_tendril" in apparatusKeys)
    assertTrue("graft_choir_lung" in apparatusKeys)
    assertTrue(TabAlertEvaluator.isArc1TabReady(m4GraftReady, OrganDeckTab.APPARATUS))

    // Arc 2 unseen & readiness checks
    val arc2Ready =
      OrganGameEngine.initialState(catalog).let { base ->
        base.copy(
          arc2 = base.arc2.copy(leads = 1_000.0, lifetimeLeads = 1_000.0, silence = 50.0)
        )
      }
    assertTrue(
      TabAlertEvaluator.shouldAlertArc2Tab(
        state = arc2Ready,
        tab = InvestigatorDeckTab.DEDUCTIONS,
        activeTab = InvestigatorDeckTab.CONTACTS,
        seenKeysByTab = mapOf(InvestigatorDeckTab.DEDUCTIONS to setOf("arc2_deductions")),
      )
    )

    // 2. Compose UI: Phase 2 hides locked jars & tendril graft; unseen JARS tab pulses until opened
    var state by mutableStateOf(phase2State)
    composeTestRule.setContent {
      BleakTheme(phase = state.phase) {
        OrganScreen(
          state = state,
          onPressValve = {},
          onBuyCreature = {},
          onBottleJar = {},
          onPerformGraft = {},
          onInteractAnomaly = {},
          onPerformExcision = {},
        )
      }
    }
    composeTestRule.waitForIdle()

    // Phase 2 ORGANISMS hides Phase 3 Tendril & M4 Choir-Lung grafting
    composeTestRule.onNodeWithTag("perform_graft_button").assertDoesNotExist()
    composeTestRule.onNodeWithTag("perform_choir_lung_button").assertDoesNotExist()
    // Unseen JARS tab displays the deep-red pulsing pip while on ORGANISMS
    composeTestRule
      .onNodeWithTag("deck_tab_alert_jars_and_graft", useUnmergedTree = true)
      .performScrollTo()
      .assertIsDisplayed()

    // Opening JARS marks its unlocks seen; switching back to ORGANISMS leaves pip cleared
    composeTestRule.onNodeWithTag("deck_tab_jars_and_graft").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("jar_salt_from_the_grout").assertDoesNotExist()
    composeTestRule.onNodeWithTag("deck_tab_apparatus").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("deck_tab_alert_jars_and_graft", useUnmergedTree = true)
      .assertDoesNotExist()

    // Upgrading to M4 exposes Choir-Lung inside ORGANISMS (and NOT inside RITES)
    composeTestRule.runOnIdle { state = m4GraftReady }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("perform_choir_lung_button").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("deck_tab_chamber_rites").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("perform_choir_lung_button").assertDoesNotExist()
    // Off-tab ORGANISMS now pulses because Choir-Tendril & Choir-Lung are ready to graft
    composeTestRule
      .onNodeWithTag("deck_tab_alert_apparatus", useUnmergedTree = true)
      .performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun wallEventDrip_showsGuaranteedFirstSightingAndThirtyPercentChanceOnRepeats() {
    // 1. Pure EventDripGate unit verification
    assertEquals(0.30, REPEAT_EVENT_DRIP_CHANCE, 0.0001)
    assertFalse(EventDripGate.shouldShow("", emptySet()) { 0.0 })
    assertTrue(EventDripGate.shouldShow("anomaly_pale_tooth", emptySet()) { 0.99 })
    val seenTooth = setOf("anomaly_pale_tooth")
    assertTrue(EventDripGate.shouldShow("anomaly_pale_tooth", seenTooth) { 0.0 })
    assertTrue(EventDripGate.shouldShow("anomaly_pale_tooth", seenTooth) { 0.299 })
    assertFalse(EventDripGate.shouldShow("anomaly_pale_tooth", seenTooth) { 0.30 })
    assertFalse(EventDripGate.shouldShow("anomaly_pale_tooth", seenTooth) { 0.99 })
    assertFalse(EventDripGate.shouldShow("anomaly_pale_tooth", seenTooth) { Double.NaN })
    assertTrue(EventDripGate.shouldShow("anomaly_glass_eye", seenTooth) { 0.99 })

    // 2. Robolectric Compose UI verification with deterministic injectable roll
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()
    var nextRoll = 0.99
    var state by
      mutableStateOf(
        OrganGameEngine.initialState(catalog).copy(
          ichor = 90.0,
          cycleLifetimeIchor = 90.0,
          phase = 1,
          phaseName = catalog.phaseName(1),
          triggeredDialogueIds = setOf("dlg_p2_glass_eye"),
          activeAnomalies = listOf(ActiveWallAnomaly(12L, WallAnomalyType.PALE_TOOTH, 0.2f, 0.25f)),
        )
      )

    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      BleakTheme(phase = state.phase) {
        OrganScreen(
          state = state,
          onPressValve = {},
          onBuyCreature = {},
          onBottleJar = {},
          onPerformGraft = {},
          onInteractAnomaly = { id -> state = OrganGameEngine.interactAnomaly(state, id, catalog) },
          onPerformExcision = {},
          eventDripRoll = { nextRoll },
        )
      }
    }
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.waitForIdle()

    // Pull 1 triggers story drip -> chamber drip is suppressed and must NOT consume guaranteed first sighting
    composeTestRule.onNodeWithTag("anomaly_pale_tooth_12").performClick()
    composeTestRule.mainClock.advanceTimeBy(600)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dripping_story_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("dripping_event_overlay").assertDoesNotExist()

    // Wait for 16s story drip to complete
    composeTestRule.mainClock.advanceTimeBy(16_500)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dripping_story_overlay").assertDoesNotExist()

    // Pull 2: first unsuppressed sighting of Pale Tooth -> displays 100% even when roll is 0.99
    composeTestRule.runOnIdle {
      state =
        state.copy(
          activeAnomalies = listOf(ActiveWallAnomaly(13L, WallAnomalyType.PALE_TOOTH, 0.25f, 0.3f))
        )
    }
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("anomaly_pale_tooth_13").performClick()
    composeTestRule.mainClock.advanceTimeBy(600)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dripping_event_overlay").assertIsDisplayed()

    // Wait for event drip (~4.8s) to clear
    composeTestRule.mainClock.advanceTimeBy(5_200)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dripping_event_overlay").assertDoesNotExist()

    // Pull 3: repeat Pale Tooth with roll = 0.99 (>= 0.30) -> stays quiet
    nextRoll = 0.99
    composeTestRule.runOnIdle {
      state =
        state.copy(
          activeAnomalies = listOf(ActiveWallAnomaly(14L, WallAnomalyType.PALE_TOOTH, 0.25f, 0.3f))
        )
    }
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("anomaly_pale_tooth_14").performClick()
    composeTestRule.mainClock.advanceTimeBy(600)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dripping_event_overlay").assertDoesNotExist()

    // Pull 4: repeat Pale Tooth with roll = 0.15 (< 0.30) -> triggers drip text
    nextRoll = 0.15
    composeTestRule.runOnIdle {
      state =
        state.copy(
          activeAnomalies = listOf(ActiveWallAnomaly(15L, WallAnomalyType.PALE_TOOTH, 0.25f, 0.3f))
        )
    }
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("anomaly_pale_tooth_15").performClick()
    composeTestRule.mainClock.advanceTimeBy(600)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dripping_event_overlay").assertIsDisplayed()

    // Wait for event drip to clear, then test a distinct unseen event (Glass Eye) with roll = 0.99 -> 100% shown
    composeTestRule.mainClock.advanceTimeBy(5_200)
    composeTestRule.waitForIdle()
    nextRoll = 0.99
    composeTestRule.runOnIdle {
      state =
        state.copy(
          activeAnomalies = listOf(ActiveWallAnomaly(16L, WallAnomalyType.GLASS_EYE, 0.72f, 0.3f))
        )
    }
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("anomaly_glass_eye_16").performClick()
    composeTestRule.mainClock.advanceTimeBy(600)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dripping_event_overlay").assertIsDisplayed()
  }

  @Test
  fun weepingLamps_hangStrictlyOnFlanksWithoutMiddleLampHidingIncomeNumber() {
    assertTrue(weepingLampLayout(0).isEmpty())
    for (count in 1..6) {
      val slots = weepingLampLayout(count)
      assertEquals(count.coerceAtMost(5), slots.size)
      slots.forEach { slot ->
        assertTrue(
          "Lamp at x=${slot.xFraction} for count=$count must stay clear of center income band (0.25..0.75)",
          slot.xFraction <= 0.24f || slot.xFraction >= 0.76f,
        )
      }
    }

    // Verify 3 & 5 lamps render alongside the income readout cleanly in Robolectric Compose UI
    val catalog = AssetDialogueRepository(ApplicationProvider.getApplicationContext()).loadCatalog()
    val state =
      OrganGameState(
        ichor = 4_200.0,
        cycleLifetimeIchor = 4_200.0,
        phase = 2,
        phaseName = catalog.phaseName(2),
        creatures = mapOf(CreatureType.WEEPING_LAMP to 5),
        isStillnessActive = true,
      )
    composeTestRule.setContent {
      BleakTheme(phase = 2) {
        OrganScreen(
          state = state,
          onPressValve = {},
          onBuyCreature = {},
          onBottleJar = {},
          onPerformGraft = {},
          onInteractAnomaly = {},
          onPerformExcision = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("weeping_lamps_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ichor_rate_text").assertIsDisplayed()
    composeTestRule.onNodeWithTag("counter_text").assertIsDisplayed()
  }

  @Test
  fun wallAnomalyNodes_breatheSubtlyWithOrganicScaleAndRenderAllTypes() {
    // Every wall anomaly type stays within subtle organic breathing bounds and varies across breathPhase
    for (type in WallAnomalyType.entries) {
      val samples = (0..10).map { step -> computeWallAnomalyBreathScale(1L, type, step / 10f) }
      val minX = samples.minOf { it.first }
      val maxX = samples.maxOf { it.first }
      val minY = samples.minOf { it.second }
      val maxY = samples.maxOf { it.second }
      assertTrue("${type.id} scaleX must stay subtle (0.97..1.06)", minX >= 0.97f && maxX <= 1.06f)
      assertTrue("${type.id} scaleY must stay subtle (0.97..1.06)", minY >= 0.97f && maxY <= 1.06f)
      assertTrue("${type.id} must actually breathe across a cycle", (maxX - minX) >= 0.03f)
    }

    // Distinct anomaly IDs breathe out of phase with each other so simultaneous nodes feel organic
    val id1 = computeWallAnomalyBreathScale(1L, WallAnomalyType.GLASS_EYE, 0.25f)
    val id2 = computeWallAnomalyBreathScale(2L, WallAnomalyType.GLASS_EYE, 0.25f)
    assertTrue("Distinct anomaly IDs should have desynchronized breath phases", id1 != id2)

    // All 4 wall anomaly types render and remain tappable in Robolectric Compose UI
    val catalog = AssetDialogueRepository(ApplicationProvider.getApplicationContext()).loadCatalog()
    val clickedIds = mutableListOf<Long>()
    val state =
      OrganGameState(
        ichor = 90_000.0,
        cycleLifetimeIchor = 90_000.0,
        phase = 4,
        phaseName = catalog.phaseName(4),
        activeAnomalies =
          listOf(
            ActiveWallAnomaly(101L, WallAnomalyType.PALE_TOOTH, 0.14f, 0.24f),
            ActiveWallAnomaly(102L, WallAnomalyType.GLASS_EYE, 0.84f, 0.24f),
            ActiveWallAnomaly(103L, WallAnomalyType.SWOLLEN_NODULE, 0.14f, 0.72f),
            ActiveWallAnomaly(104L, WallAnomalyType.CHITIN_RIB, 0.84f, 0.72f),
          ),
      )
    composeTestRule.setContent {
      BleakTheme(phase = 4) {
        OrganScreen(
          state = state,
          onPressValve = {},
          onBuyCreature = {},
          onBottleJar = {},
          onPerformGraft = {},
          onInteractAnomaly = { clickedIds += it },
          onPerformExcision = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    for (tag in
      listOf(
        "anomaly_pale_tooth_101",
        "anomaly_glass_eye_102",
        "anomaly_swollen_nodule_103",
        "anomaly_chitin_rib_104",
      )) {
      composeTestRule.onNodeWithTag(tag).assertIsDisplayed().performClick()
    }
    composeTestRule.waitForIdle()
    assertEquals(listOf(101L, 102L, 103L, 104L), clickedIds)
  }

  @Test
  fun colonyProliferationButton_hiddenBeforeExcisionThreeAndBulkBuysCreaturesAtM3() {
    val viewModel = createViewModel()
    viewModel.devSetHighestMilestone(2)
    viewModel.devAssignArc1Resources(ichor = 25_000.0)

    composeTestRule.setContent { OrganRoute(viewModel = viewModel) }
    composeTestRule.waitForIdle()

    // Before Excision III (M2), Proliferate strip must stay hidden
    composeTestRule.onNodeWithTag("proliferate_suture_hand").assertDoesNotExist()
    composeTestRule.onNodeWithTag("proliferate_leech_clerk").assertDoesNotExist()

    // Unlock Excision III (M3): Proliferate strips appear and register colony_proliferation tab key
    composeTestRule.runOnIdle { viewModel.devSetHighestMilestone(3) }
    composeTestRule.waitForIdle()

    val stateAtM3 = viewModel.uiState.value
    assertTrue(
      "colony_proliferation" in
        com.example.ui.components.TabAlertEvaluator.arc1UnlockKeys(
          stateAtM3,
          com.example.game.OrganDeckTab.APPARATUS,
        ),
    )
    val expectedHands = stateAtM3.maxAffordableCreatureQuote(CreatureType.SUTURE_HAND).count
    assertTrue("Should afford multiple Suture-Hands with 25k Ichor", expectedHands >= 5)

    composeTestRule
      .onNodeWithTag("proliferate_suture_hand")
      .performScrollTo()
      .assertIsDisplayed()
      .performClick()
    composeTestRule.waitForIdle()

    assertEquals(expectedHands, viewModel.uiState.value.creatureCount(CreatureType.SUTURE_HAND))
    assertTrue(
      "Remaining Ichor should be below cost of next single Suture-Hand",
      viewModel.uiState.value.ichor <
        viewModel.uiState.value.creatureCost(CreatureType.SUTURE_HAND).toDouble(),
    )
  }

  @Test
  fun formatValveYieldAndCompactDecimal_preventLargeNumberOverflowAndValveClipping() {
    assertEquals("1", com.example.ui.components.formatValveYield(1.0))
    assertEquals("9,840", com.example.ui.components.formatValveYield(9_840.0))
    assertEquals("12.5K", com.example.ui.components.formatValveYield(12_500.0))
    assertEquals("142K", com.example.ui.components.formatValveYield(142_300.0))
    assertEquals("2.92M", com.example.ui.components.formatValveYield(2_923_756.0))
    assertEquals("36B", com.example.ui.components.formatValveYield(36_000_000_000.0))
    // Regression check: formatCompactDecimal must not saturate at Int.MAX_VALUE above 214.7M
    assertEquals("500,000,000", com.example.ui.components.formatCompactDecimal(500_000_000.0))
  }

  @Test
  fun progressiveChamberWallLayers_transformAcrossPhasesAndLateExcisionsAndRenderChoirLungs() {
    // 1. Unit assertions for 5-stage photographic wall blending (exact parity with Web)
    val p0 = computeWallLayerAlphas(phase = 0, highestExcisionMilestone = 0, cycle = 0)
    assertEquals(0.34f, p0.phase1Tile, 0.001f)
    assertEquals(0f, p0.phase2Fleshy, 0.001f)
    assertEquals(0f, p0.phase3Muscle, 0.001f)
    assertEquals(0f, p0.phase4Hollow, 0.001f)
    assertEquals(0f, p0.lateExcision, 0.001f)

    val p2 = computeWallLayerAlphas(phase = 2, highestExcisionMilestone = 0, cycle = 0)
    assertEquals(0.34f, p2.phase1Tile, 0.001f)
    assertEquals(0.80f, p2.phase2Fleshy, 0.001f)
    assertEquals(0f, p2.phase3Muscle, 0.001f)

    val p3 = computeWallLayerAlphas(phase = 3, highestExcisionMilestone = 0, cycle = 0)
    assertEquals(0.86f, p3.phase3Muscle, 0.001f)
    assertEquals(0f, p3.phase4Hollow, 0.001f)

    val p4M0 = computeWallLayerAlphas(phase = 4, highestExcisionMilestone = 0, cycle = 0)
    assertEquals(0f, p4M0.phase1Tile, 0.001f)
    assertEquals(0.26f, p4M0.phase3Muscle, 0.001f)
    assertEquals(0.90f, p4M0.phase4Hollow, 0.001f)
    assertEquals(0f, p4M0.lateExcision, 0.001f)

    val p0M5 = computeWallLayerAlphas(phase = 0, highestExcisionMilestone = 5, cycle = 5)
    val p4M5 = computeWallLayerAlphas(phase = 4, highestExcisionMilestone = 5, cycle = 5)
    val p4M6 = computeWallLayerAlphas(phase = 4, highestExcisionMilestone = 6, cycle = 6)
    assertTrue("Fresh post-Excision P0 starts cleaner than P4", p0M5.lateExcision > 0f && p0M5.lateExcision < p4M5.lateExcision)
    assertTrue("M6 deepens late-Excision scars beyond M5", p4M6.lateExcision > p4M5.lateExcision && p4M6.lateExcision <= 0.82f)

    // 2. Robolectric Compose UI verification across Phase 2 -> Phase 3 -> Phase 4 Late Excision M5
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()
    var state by
      mutableStateOf(
        OrganGameEngine.initialState(catalog).copy(
          ichor = 2_400.0,
          cycleLifetimeIchor = 2_400.0,
          phase = 2,
          phaseName = catalog.phaseName(2),
          highestExcisionMilestone = 0,
        )
      )

    composeTestRule.setContent {
      BleakTheme(phase = state.phase) {
        OrganScreen(
          state = state,
          onPressValve = {},
          onBuyCreature = {},
          onBottleJar = {},
          onPerformGraft = {},
          onInteractAnomaly = {},
          onPerformExcision = {},
        )
      }
    }
    composeTestRule.waitForIdle()

    // Phase 2 Cycle 0 shows tile + fleshy lichen wall, hides Phase 3/4/Late Excision
    composeTestRule.onNodeWithTag("chamber_wall_art").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chamber_wall_fleshy_art").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chamber_wall_phase3_art").assertDoesNotExist()
    composeTestRule.onNodeWithTag("chamber_wall_phase4_art").assertDoesNotExist()
    composeTestRule.onNodeWithTag("chamber_wall_excision_late_art").assertDoesNotExist()

    // Phase 3 Cycle 0 introduces muscular buckling wall layer
    composeTestRule.runOnIdle {
      state = state.copy(ichor = 25_000.0, cycleLifetimeIchor = 25_000.0, phase = 3, phaseName = catalog.phaseName(3))
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("chamber_wall_phase3_art").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chamber_wall_phase4_art").assertDoesNotExist()

    // Phase 4 + Excision V + Grafted Choir-Lungs + Grafted Tendrils + sealed Floor Sump + Echo routing
    composeTestRule.runOnIdle {
      state =
        state.copy(
          ichor = 1_500_000_000.0,
          cycleLifetimeIchor = 1_500_000_000.0,
          phase = 4,
          phaseName = catalog.phaseName(4),
          cycle = 6,
          highestExcisionMilestone = 5,
          scarTissue = 64L,
          creatures =
            mapOf(
              CreatureType.SUTURE_HAND to 8,
              CreatureType.LEECH_CLERK to 6,
              CreatureType.WEEPING_LAMP to 5,
              CreatureType.SUMP_SIPHON to 4,
            ),
          graftedTendrils = 4,
          choirLungs = 3,
          drainSealed = true,
          drainPressure = 72.0,
          echoDiversionPercent = 50,
          isStillnessActive = true,
        )
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("chamber_wall_art").assertDoesNotExist()
    composeTestRule.onNodeWithTag("chamber_wall_phase3_art").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chamber_wall_phase4_art").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chamber_wall_excision_late_art").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chamber_wall_phase_4").assertIsDisplayed()
    composeTestRule.onNodeWithTag("valve_aperture_art").assertIsDisplayed()
    composeTestRule.onNodeWithTag("suture_hands_overlay").assertIsDisplayed()
  }

  @Test
  fun wallAnomalies_renderDistinctPhotographicMacroSpecimensAndRemainInteractive() {
    val drawables = WallAnomalyType.entries.map { wallAnomalyDrawableRes(it) }
    assertEquals("Each wall anomaly type must map to its own distinct photographic drawable", 4, drawables.toSet().size)
    assertEquals(R.drawable.img_anomaly_pale_tooth, wallAnomalyDrawableRes(WallAnomalyType.PALE_TOOTH))
    assertEquals(R.drawable.img_anomaly_glass_eye, wallAnomalyDrawableRes(WallAnomalyType.GLASS_EYE))
    assertEquals(R.drawable.img_anomaly_swollen_nodule, wallAnomalyDrawableRes(WallAnomalyType.SWOLLEN_NODULE))
    assertEquals(R.drawable.img_anomaly_chitin_rib, wallAnomalyDrawableRes(WallAnomalyType.CHITIN_RIB))

    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()
    val interactedIds = mutableListOf<Long>()
    val state =
      OrganGameEngine.initialState(catalog).copy(
        ichor = 90_000.0,
        cycleLifetimeIchor = 90_000.0,
        phase = 4,
        phaseName = catalog.phaseName(4),
        activeAnomalies =
          listOf(
            ActiveWallAnomaly(101L, WallAnomalyType.PALE_TOOTH, 0.16f, 0.25f),
            ActiveWallAnomaly(102L, WallAnomalyType.GLASS_EYE, 0.84f, 0.25f),
            ActiveWallAnomaly(103L, WallAnomalyType.SWOLLEN_NODULE, 0.16f, 0.72f),
            ActiveWallAnomaly(104L, WallAnomalyType.CHITIN_RIB, 0.84f, 0.72f),
          ),
      )

    composeTestRule.setContent {
      BleakTheme(phase = state.phase) {
        OrganScreen(
          state = state,
          onPressValve = {},
          onBuyCreature = {},
          onBottleJar = {},
          onPerformGraft = {},
          onInteractAnomaly = { interactedIds.add(it) },
          onPerformExcision = {},
        )
      }
    }
    composeTestRule.waitForIdle()

    for (anomaly in state.activeAnomalies) {
      composeTestRule
        .onNodeWithTag("anomaly_art_${anomaly.type.id}", useUnmergedTree = true)
        .assertIsDisplayed()
      composeTestRule
        .onNodeWithTag("anomaly_${anomaly.type.id}_${anomaly.id}")
        .assertIsDisplayed()
        .assert(hasClickAction())
    }
    composeTestRule.onNodeWithTag("anomaly_swollen_nodule_103").performClick()
    composeTestRule.waitForIdle()
    assertEquals(listOf(103L), interactedIds)
  }

  @Test
  fun scarsAndExcisionTabs_stopPulsingAfterFirstClickAndOnlyRepulseOnFreshEvents() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()

    // 1. Pure TabAlertEvaluator assertions: SCARS and EXCISION are one-shot (never persistent isArc1TabReady)
    val postM1 =
      OrganGameEngine.initialState(catalog).copy(
        cycle = 1,
        highestExcisionMilestone = 1,
        scarTissue = 4L,
        availableScarTissue = 4L,
        ichor = 300_000.0,
        cycleLifetimeIchor = 300_000.0,
        phase = 4,
        phaseName = catalog.phaseName(4),
        nextPhaseThreshold = null,
      )
    assertFalse(TabAlertEvaluator.isArc1TabReady(postM1, OrganDeckTab.SCAR_TREE))
    assertFalse(TabAlertEvaluator.isArc1TabReady(postM1, OrganDeckTab.EXCISION))
    assertTrue(
      TabAlertEvaluator.shouldAlertArc1Tab(postM1, OrganDeckTab.SCAR_TREE, OrganDeckTab.APPARATUS, emptyMap())
    )
    assertTrue(
      TabAlertEvaluator.shouldAlertArc1Tab(postM1, OrganDeckTab.EXCISION, OrganDeckTab.APPARATUS, emptyMap())
    )

    // After viewing SCARS & EXCISION without spending Scars or excising, both stay silent
    val seenOnce =
      mapOf(
        OrganDeckTab.SCAR_TREE to TabAlertEvaluator.arc1UnlockKeys(postM1, OrganDeckTab.SCAR_TREE),
        OrganDeckTab.EXCISION to TabAlertEvaluator.arc1UnlockKeys(postM1, OrganDeckTab.EXCISION),
      )
    assertFalse(
      TabAlertEvaluator.shouldAlertArc1Tab(postM1, OrganDeckTab.SCAR_TREE, OrganDeckTab.APPARATUS, seenOnce)
    )
    assertFalse(
      TabAlertEvaluator.shouldAlertArc1Tab(postM1, OrganDeckTab.EXCISION, OrganDeckTab.APPARATUS, seenOnce)
    )

    // Earning fresh Scar Tissue (raising lifetime scarTissue) re-pulses SCARS; crossing M2 (1.8M) re-pulses EXCISION
    val freshScars = postM1.copy(scarTissue = 8L, availableScarTissue = 8L)
    assertTrue(
      TabAlertEvaluator.shouldAlertArc1Tab(freshScars, OrganDeckTab.SCAR_TREE, OrganDeckTab.APPARATUS, seenOnce)
    )
    val crossedM2 = postM1.copy(ichor = 1_800_000.0, cycleLifetimeIchor = 1_800_000.0)
    assertTrue(
      TabAlertEvaluator.shouldAlertArc1Tab(crossedM2, OrganDeckTab.EXCISION, OrganDeckTab.APPARATUS, seenOnce)
    )

    // 2. Live Robolectric Compose UI verification of click-to-silence & fresh-event re-pulse
    var state by mutableStateOf(postM1)
    composeTestRule.setContent {
      BleakTheme(phase = state.phase) {
        OrganScreen(
          state = state,
          onPressValve = {},
          onBuyCreature = {},
          onBottleJar = {},
          onPerformGraft = {},
          onInteractAnomaly = {},
          onPerformExcision = {},
        )
      }
    }
    composeTestRule.waitForIdle()

    // Both SCARS and EXCISION pulse initially; Phase 4 header shows milestone progress + EXCISION READY
    composeTestRule
      .onNodeWithTag("threshold_status")
      .assertTextEquals("THE SUNKEN DRAIN · 300,000 / 1,800,000 · EXCISION READY")
    composeTestRule
      .onNodeWithTag("deck_tab_alert_scar_tree", useUnmergedTree = true)
      .performScrollTo()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("deck_tab_alert_excision", useUnmergedTree = true)
      .performScrollTo()
      .assertIsDisplayed()

    // Click SCARS without spending any Scars, then return to ORGANISMS -> SCARS pip is silenced
    composeTestRule.onNodeWithTag("deck_tab_scar_tree").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("deck_tab_apparatus").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("deck_tab_alert_scar_tree", useUnmergedTree = true)
      .assertDoesNotExist()

    // Click EXCISION without severing, then return to ORGANISMS -> EXCISION pip is silenced
    composeTestRule.onNodeWithTag("deck_tab_excision").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("deck_tab_apparatus").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("deck_tab_alert_excision", useUnmergedTree = true)
      .assertDoesNotExist()

    // Crossing M2 (1.8M) re-pulses EXCISION and updates header to SEAM READY // THE SUNKEN DRAIN
    composeTestRule.runOnIdle { state = crossedM2 }
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("threshold_status")
      .assertTextEquals("SEAM READY // THE SUNKEN DRAIN")
    composeTestRule
      .onNodeWithTag("deck_tab_alert_excision", useUnmergedTree = true)
      .performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun specimenJars_renderMacroPortraitsAndTwinChamberWallShelvesWithCatatonicTideRetention() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()

    // 1. All 6 Specimen Jars map to 6 distinct photographic macro drawables
    val jarDrawables = SpecimenJar.entries.map { specimenJarDrawableRes(it) }
    assertEquals(6, jarDrawables.toSet().size)
    assertTrue(jarDrawables.all { it != 0 })

    // 2. Shelf partitioning cleanly splits 3 foundational jars (left) vs 3 deep jars (right)
    val (emptyLeft, emptyRight) = partitionChamberShelfJars(emptySet())
    assertTrue(emptyLeft.isEmpty() && emptyRight.isEmpty())
    val (allLeft, allRight) = partitionChamberShelfJars(SpecimenJar.entries.toSet())
    assertEquals(
      listOf(SpecimenJar.YESTERDAYS_DUST, SpecimenJar.UNSPENT_BREATH, SpecimenJar.ALMOST_A_NAME),
      allLeft,
    )
    assertEquals(
      listOf(SpecimenJar.SALT_FROM_THE_GROUT, SpecimenJar.A_BORROWED_PULSE, SpecimenJar.THE_UNCOUNTED_HOUR),
      allRight,
    )

    // 3. Robolectric Compose UI: shelves stay hidden at 0 jars, appear progressively, and respect Catatonic Tide
    var state by
      mutableStateOf(
        OrganGameEngine.initialState(catalog).copy(
          cycle = 2,
          highestExcisionMilestone = 2,
          ichor = 300_000.0,
          cycleLifetimeIchor = 300_000.0,
          stillness = 5_000.0,
          isStillnessActive = true,
          phase = 4,
          phaseName = catalog.phaseName(4),
          nextPhaseThreshold = null,
          unlockedScarNodes = setOf(ScarNodeId.FIRST_INCISION, ScarNodeId.CATATONIC_TIDE),
          ownedJars = emptySet(),
        )
      )

    composeTestRule.setContent {
      BleakTheme(phase = state.phase) {
        OrganScreen(
          state = state,
          onPressValve = {},
          onBuyCreature = {},
          onBottleJar = { jar -> state = OrganGameEngine.bottleJar(state, jar, catalog) },
          onPerformGraft = {},
          onInteractAnomaly = {},
          onPerformExcision = { state = OrganGameEngine.performExcision(state, catalog) },
        )
      }
    }
    composeTestRule.waitForIdle()

    // Zero jars owned -> neither chamber wall shelf renders
    composeTestRule.onNodeWithTag("chamber_jar_shelf_left", useUnmergedTree = true).assertDoesNotExist()
    composeTestRule.onNodeWithTag("chamber_jar_shelf_right", useUnmergedTree = true).assertDoesNotExist()

    // Open JARS tab -> all 6 photographic jar portraits render
    composeTestRule.onNodeWithTag("deck_tab_jars_and_graft").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    for (jar in SpecimenJar.entries) {
      composeTestRule
        .onNodeWithTag("art_jar_${jar.id}", useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
    }

    // Bottling Yesterday's Dust populates ONLY the left foundational shelf
    composeTestRule.onNodeWithTag("jar_yesterdays_dust").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("chamber_jar_shelf_left", useUnmergedTree = true)
      .performScrollTo()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("chamber_jar_yesterdays_dust", useUnmergedTree = true)
      .assertIsDisplayed()
    composeTestRule.onNodeWithTag("chamber_jar_shelf_right", useUnmergedTree = true).assertDoesNotExist()

    // Bottling A Borrowed Pulse and The Uncounted Hour populates the right deep shelf
    composeTestRule.onNodeWithTag("jar_a_borrowed_pulse").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("jar_the_uncounted_hour").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("chamber_jar_shelf_right", useUnmergedTree = true)
      .performScrollTo()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("chamber_jar_a_borrowed_pulse", useUnmergedTree = true)
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("chamber_jar_the_uncounted_hour", useUnmergedTree = true)
      .assertIsDisplayed()

    // Perform Excision with CATATONIC_TIDE -> left foundational shelf stays standing while right deep shelf clears
    composeTestRule.runOnIdle { state = OrganGameEngine.performExcision(state, catalog) }
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("chamber_jar_shelf_left", useUnmergedTree = true)
      .performScrollTo()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("chamber_jar_yesterdays_dust", useUnmergedTree = true)
      .assertIsDisplayed()
    composeTestRule.onNodeWithTag("chamber_jar_shelf_right", useUnmergedTree = true).assertDoesNotExist()
  }

  @Test
  fun drippingMarginOverlay_queuesIncomingStoryDripsFifoWithoutDismissingOngoingDrip() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()
    val entryA = catalog.entries[0]
    val entryB = catalog.entries[1]
    val entryC = catalog.entries[2]
    val textA = entryA.lines.joinToString(" ").trim()
    val textB = entryB.lines.joinToString(" ").trim()
    val textC = entryC.lines.joinToString(" ").trim()

    // 1. Pure FIFO StoryDripQueue unit verification
    assertEquals(300L, STORY_DRIP_QUEUE_GAP_MS)
    var q = StoryDripQueueState()
    assertFalse(q.isBusy)

    q = StoryDripQueue.enqueue(q, entryA)
    assertEquals(entryA.id, q.active?.id)
    assertTrue(q.pending.isEmpty())
    assertTrue(q.isBusy)

    // Enqueueing newer entries never evicts active and ignores duplicate IDs
    q = StoryDripQueue.enqueue(q, entryB)
    q = StoryDripQueue.enqueue(q, entryA)
    q = StoryDripQueue.enqueue(q, entryB)
    q = StoryDripQueue.enqueue(q, entryC)
    assertEquals(entryA.id, q.active?.id)
    assertEquals(listOf(entryB.id, entryC.id), q.pending.map { it.id })

    q = StoryDripQueue.completeActive(q)
    assertEquals(entryB.id, q.active?.id)
    assertEquals(listOf(entryC.id), q.pending.map { it.id })

    q = StoryDripQueue.completeActive(q)
    assertEquals(entryC.id, q.active?.id)
    assertTrue(q.pending.isEmpty())

    q = StoryDripQueue.completeActive(q)
    assertFalse(q.isBusy)
    assertEquals(null, q.active)

    // 2. Live Robolectric Compose UI test through real OrganScreen: ongoing drip completes untouched, then queued drips play FIFO
    var state by
      mutableStateOf(
        OrganGameEngine.initialState(catalog).copy(
          ichor = 90.0,
          cycleLifetimeIchor = 90.0,
          phase = 1,
          phaseName = catalog.phaseName(1),
          triggeredDialogueIds = catalog.entries.map { it.id }.toSet(),
          activeDialogue = null,
          activeAnomalies =
            listOf(
              ActiveWallAnomaly(91L, WallAnomalyType.PALE_TOOTH, 0.2f, 0.25f),
              ActiveWallAnomaly(92L, WallAnomalyType.PALE_TOOTH, 0.8f, 0.28f),
            ),
        )
      )

    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      BleakTheme(phase = state.phase) {
        OrganScreen(
          state = state,
          onPressValve = {},
          onBuyCreature = {},
          onBottleJar = {},
          onPerformGraft = {},
          onInteractAnomaly = { id -> state = OrganGameEngine.interactAnomaly(state, id, catalog) },
          onPerformExcision = {},
          eventDripRoll = { 0.0 },
        )
      }
    }
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.waitForIdle()

    // Deliver entryA -> begins dripping on right margin
    composeTestRule.runOnIdle { state = state.copy(activeDialogue = entryA) }
    composeTestRule.mainClock.advanceTimeBy(600)
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("dripping_story_overlay")
      .assertIsDisplayed()
      .assert(hasAnyDescendant(hasText(textA)))

    // 4 seconds later, entryB and entryC arrive -> entryA MUST continue undisturbed (never dismissed)
    composeTestRule.mainClock.advanceTimeBy(4_000)
    composeTestRule.runOnIdle { state = state.copy(activeDialogue = entryB) }
    composeTestRule.mainClock.advanceTimeBy(300)
    composeTestRule.runOnIdle { state = state.copy(activeDialogue = entryC) }
    composeTestRule.mainClock.advanceTimeBy(300)
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("dripping_story_overlay")
      .assertIsDisplayed()
      .assert(hasAnyDescendant(hasText(textA)))

    // Still showing entryA at 14.2s total elapsed
    composeTestRule.mainClock.advanceTimeBy(9_000)
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("dripping_story_overlay")
      .assertIsDisplayed()
      .assert(hasAnyDescendant(hasText(textA)))

    // At ~16.1s (inside 300ms inter-drip breather): pulling an anomaly keeps event drip suppressed
    composeTestRule.mainClock.advanceTimeBy(1_900)
    composeTestRule.onNodeWithTag("anomaly_pale_tooth_91").performClick()
    composeTestRule.mainClock.advanceTimeBy(50)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dripping_event_overlay").assertDoesNotExist()

    // Past the 300ms breather -> entryB promotes and plays its full duration
    composeTestRule.mainClock.advanceTimeBy(600)
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("dripping_story_overlay")
      .assertIsDisplayed()
      .assert(hasAnyDescendant(hasText(textB)))

    // After entryB finishes (~16s + 300ms gap) -> entryC promotes and plays
    composeTestRule.mainClock.advanceTimeBy(16_400)
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("dripping_story_overlay")
      .assertIsDisplayed()
      .assert(hasAnyDescendant(hasText(textC)))

    // After entryC finishes -> story lane clears and chamber events are allowed again
    composeTestRule.mainClock.advanceTimeBy(16_400)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dripping_story_overlay").assertDoesNotExist()

    composeTestRule.onNodeWithTag("anomaly_pale_tooth_92").performClick()
    composeTestRule.mainClock.advanceTimeBy(500)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dripping_event_overlay").assertIsDisplayed()
  }
}
