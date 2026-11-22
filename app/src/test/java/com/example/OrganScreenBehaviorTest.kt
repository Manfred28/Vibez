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
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.AssetDialogueRepository
import com.example.game.ActiveWallAnomaly
import com.example.game.CreatureType
import com.example.game.OrganBalance
import com.example.game.OrganGameEngine
import com.example.game.OrganGameState
import com.example.game.SpecimenJar
import com.example.game.WallAnomalyType
import com.example.ui.OrganRoute
import com.example.ui.OrganScreen
import com.example.ui.OrganViewModel
import com.example.ui.theme.BleakTheme
import org.junit.Assert.assertEquals
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
      .assertTextEquals("PHASE 00 // THE COUNTING ROOM")
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
      .assertTextEquals("PHASE 01 // UNCANNY DRIFT")
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
      .assertTextEquals("PRESSING VALVE")
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
          stillness = 30.0,
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
      .assertTextEquals("EYE GAZE: PASSIVE ×1.6 (8s)")
    // Living chamber organisms: Suture-Hands on valve, Leech-Clerks on counter glass, Weeping Lamps overhead
    composeTestRule.onNodeWithTag("suture_hands_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("leech_clerks_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("weeping_lamps_overlay").assertIsDisplayed()

    // Verify Weeping Lamp dossier card reflects active Stillness 2x state & Stillness drip telemetry
    composeTestRule
      .onNodeWithTag("organism_status_leech_clerk")
      .performScrollTo()
      .assertTextEquals("GRAZING GLASS")
    composeTestRule
      .onNodeWithTag("organism_status_weeping_lamp")
      .performScrollTo()
      .assertTextEquals("WEEPING ×2")
    composeTestRule
      .onNodeWithTag("art_weeping_lamp", useUnmergedTree = true)
      .performScrollTo()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("organism_stillness_yield_weeping_lamp")
      .performScrollTo()
      .assertIsDisplayed()

    // Switch to JARS & GRAFT tab, bottle Yesterday's Dust, and perform Graft
    composeTestRule.onNodeWithTag("deck_tab_jars_and_graft").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("jar_yesterdays_dust").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertTrue(SpecimenJar.YESTERDAYS_DUST in state.ownedJars)
    composeTestRule.onNodeWithText("SEALED").assertIsDisplayed()

    composeTestRule.onNodeWithTag("perform_graft_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, state.graftedTendrils)
    composeTestRule.onNodeWithText("GRAFTED CHOIR-TENDRIL [1]").assertIsDisplayed()
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
      composeTestRule
        .onNodeWithTag("threshold_status")
        .assertTextEquals(
          "EXCISION MASS: ${
            java.text.NumberFormat.getIntegerInstance()
              .format(viewModel.uiState.value.cycleLifetimeIchorDisplay)
          } / 210,000"
        )
      composeTestRule.runOnIdle { viewModel.advanceTime(9_000.0) }
      composeTestRule.waitForIdle()
    }

    assertTrue("Expected Excision to be ready in Phase 4", viewModel.uiState.value.canExcise)

    composeTestRule
      .onNodeWithTag("threshold_status")
      .assertTextEquals("OVERGROWTH CRITICAL // EXCISION READY")

    // Open EXCISION tab -> Arm -> Confirm Severance
    composeTestRule.onNodeWithTag("deck_tab_excision").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("excision_reward_text").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("excision_arm_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("excision_confirm_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Verify full Cycle 1 post-reset state, passive Scar multipliers, and Cycle 1 story beats in UI
    assertEquals(1, viewModel.uiState.value.cycle)
    assertTrue(viewModel.uiState.value.scarTissue >= 1L)

    composeTestRule
      .onNodeWithTag("phase_header")
      .assertTextEquals("PHASE 00 // THE COUNTING ROOM")
    composeTestRule
      .onNodeWithTag("cycle_status")
      .performScrollTo()
      .assertIsDisplayed()
      .assertTextEquals("CYCLE 01 // SCAR TISSUE: ${viewModel.uiState.value.scarTissue}")
    composeTestRule.onNodeWithTag("scar_passive_status").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("prestige_passive_banner").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("counter_text").performScrollTo().assertTextEquals("0")
    composeTestRule
      .onNodeWithTag("dialogue_speaker")
      .assertIsDisplayed()
      .assertTextEquals("// ${excisionFragment.speaker.uppercase()}")
    composeTestRule
      .onNodeWithTag("dialogue_body")
      .assertIsDisplayed()
      .assertTextEquals(excisionFragment.lines.joinToString("\n"))

    // Reaching 75 Ichor in Cycle 1 unlocks the new prestige-bound handwriting fragment (45 * 1.70 = 76.5 >= 75)
    val scarHandwritingFragment =
      catalog.entries.first { it.id == "dlg_cycle_1_scar_handwriting" }
    repeat(45) { viewModel.pressValve() }
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
      .assertTextEquals("EYE GAZE: PASSIVE ×1.6 (6s)")
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

    // Verify Haunted Investigator Desk renders and Stamp Case File increments Leads
    composeTestRule.onNodeWithTag("investigator_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("arc2_phase_header").assertIsDisplayed()
    val leadsBefore = viewModel.uiState.value.arc2.leadsDisplay
    composeTestRule.onNodeWithTag("inspect_case_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertTrue(viewModel.uiState.value.arc2.leadsDisplay > leadsBefore)

    // Switch to BOARD tab and connect Chapters 1..4 to trigger File #00 twist
    composeTestRule.onNodeWithTag("arc2_tab_evidence_board").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    for (ch in 1..4) {
      composeTestRule.onNodeWithTag("chapter_connect_$ch").performScrollTo().performClick()
      composeTestRule.waitForIdle()
    }

    assertTrue(viewModel.uiState.value.arc2.fileZeroRevealed)
    composeTestRule.onNodeWithTag("file_zero_reveal_card").performScrollTo().assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("dialogue_body")
      .assertIsDisplayed()
      .assertTextEquals(fileZeroFragment.lines.joinToString("\n"))

    // Enter Arc 3 open convergence threshold
    composeTestRule.onNodeWithTag("enter_arc3_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("arc3_screen").assertIsDisplayed()
  }
}
