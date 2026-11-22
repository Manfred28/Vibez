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
import com.example.game.CreatureType
import com.example.game.OrganBalance
import com.example.game.OrganGameEngine
import com.example.game.OrganGameState
import com.example.game.SpecimenJar
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
    composeTestRule.onNodeWithTag("apparatus_deck").performScrollTo().assertIsDisplayed()

    // Verify Phase 1 bespoke specimen portraits render inside organism deck rows
    composeTestRule
      .onNodeWithTag("art_suture_hand", useUnmergedTree = true)
      .performScrollTo()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("art_leech_clerk", useUnmergedTree = true)
      .performScrollTo()
      .assertIsDisplayed()

    // Buy Suture-Hand (25 Ichor) via UI
    composeTestRule.onNodeWithTag("buy_suture_hand").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, viewModel.uiState.value.creatureCount(CreatureType.SUTURE_HAND))
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

    // Open Notes Archive sheet via the dedicated Archive pill button
    composeTestRule.onNodeWithTag("open_archive_button").performScrollTo().performClick()
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

    // Switch to JARS & GRAFT tab, bottle Yesterday's Dust (18 Stillness), and perform Graft
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

    if (!viewModel.uiState.value.canExcise) {
      composeTestRule
        .onNodeWithTag("threshold_status")
        .assertTextEquals(
          "EXCISION MASS: ${
            java.text.NumberFormat.getIntegerInstance()
              .format(viewModel.uiState.value.cycleLifetimeIchorDisplay)
          } / 150,000"
        )
      composeTestRule.runOnIdle { viewModel.advanceTime(5_000.0) }
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

    // Verify full Cycle 1 post-reset state in UI
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
    composeTestRule.onNodeWithTag("counter_text").assertTextEquals("0")
    composeTestRule
      .onNodeWithTag("dialogue_speaker")
      .assertTextEquals("// ${excisionFragment.speaker.uppercase()}")
    composeTestRule
      .onNodeWithTag("dialogue_body")
      .assertTextEquals(excisionFragment.lines.joinToString("\n"))
  }
}
