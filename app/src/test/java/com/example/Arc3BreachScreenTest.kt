package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.audio.InMemoryAudioEngine
import com.example.data.AssetDialogueRepository
import com.example.game.Arc3Balance
import com.example.game.Arc3Stratum
import com.example.game.Arc3UnmakeAct
import com.example.game.GameArc
import com.example.ui.OrganRoute
import com.example.ui.OrganViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Arc3BreachScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createViewModel(): OrganViewModel {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return OrganViewModel(
      dialogueRepository = AssetDialogueRepository(context),
      saveRepository = null,
      autoTick = false,
      audioEngine = InMemoryAudioEngine(),
    )
  }

  @Test
  fun arc3ActiveBreach_wrenchShatterJarAndDriveWedgeUpdateUiAndState() {
    val vm = createViewModel()
    vm.devSetActiveArc(GameArc.ARC_3_CONVERGENCE)

    composeTestRule.setContent { OrganRoute(viewModel = vm) }
    composeTestRule.waitForIdle()

    // Breach canvas, stratum header, and wrench trigger render immediately
    composeTestRule.onNodeWithTag("arc3_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("arc3_stratum_header").assertIsDisplayed().assertTextContains("BROKEN GROUT")
    composeTestRule.onNodeWithTag("arc3_breach_canvas").assertIsDisplayed()
    composeTestRule.onNodeWithTag("arc3_wrench_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, vm.uiState.value.arc3.wrenchCount)
    assertTrue(vm.uiState.value.arc3.hemorrhage > 0.0)

    // Grant hemorrhage to afford shattering Yesterday's Dust in the UNMAKE tab
    composeTestRule.runOnIdle {
      vm.devAssignArc3Resources(hemorrhage = 100.0, fillFracture = false)
    }
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("arc3_unmake_${Arc3UnmakeAct.SHATTER_YESTERDAYS_DUST.id}")
      .performScrollTo()
      .assertIsDisplayed()
      .performClick()
    composeTestRule.waitForIdle()

    assertTrue(Arc3UnmakeAct.SHATTER_YESTERDAYS_DUST in vm.uiState.value.arc3.completedUnmakes)

    // Fill fracture, switch to WEDGES tab, and drive Memory Wedge I
    composeTestRule.runOnIdle {
      vm.devAssignArc3Resources(fillFracture = true)
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("arc3_tab_wedges").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("arc3_drive_wedge_button").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    assertTrue(Arc3Stratum.BROKEN_GROUT in vm.uiState.value.arc3.lockedWedges)
    assertEquals(2, vm.uiState.value.arc3.currentStratum)
    composeTestRule.onNodeWithTag("arc3_stratum_header").performScrollTo().assertTextContains("UNSTITCHED SEAM")
  }

  @Test
  fun arc3InteractiveFivePullFinale_dissolvesDeckAndCompletesIntoSubmergedAftermathNavigation() {
    val vm = createViewModel()
    vm.devJumpArc3Finale(submerged = false)

    composeTestRule.setContent { OrganRoute(viewModel = vm) }
    composeTestRule.waitForIdle()

    // Finale view renders with lone pull trigger and crimson flood backdrop (deck is collapsed/gone)
    composeTestRule.onNodeWithTag("arc3_finale_container").assertIsDisplayed()
    composeTestRule.onNodeWithTag("art_arc3_crimson_flood", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("arc3_breach_deck").assertDoesNotExist()

    // Perform all 5 deliberate pulls with cooldown advances between pulls
    for (pull in 1..Arc3Balance.FINAL_PULLS_TOTAL) {
      composeTestRule.onNodeWithTag("arc3_final_pull_button").assertIsDisplayed().performClick()
      composeTestRule.waitForIdle()
      assertEquals(pull, vm.uiState.value.arc3.finalPullsCompleted)
      if (pull < Arc3Balance.FINAL_PULLS_TOTAL) {
        composeTestRule.runOnIdle { vm.advanceTime(Arc3Balance.FINAL_PULL_COOLDOWN_SEC + 0.1) }
        composeTestRule.waitForIdle()
      }
    }

    // Quiet Submerged Aftermath appears with working Archive / Chamber / Desk navigation
    assertTrue(vm.uiState.value.arc3.submersionComplete)
    composeTestRule.onNodeWithTag("arc3_submerged_view").assertIsDisplayed()
    composeTestRule.onNodeWithTag("arc3_aftermath_archive_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("arc3_to_arc1_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("arc3_to_arc2_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    assertEquals(GameArc.ARC_2_INVESTIGATION, vm.uiState.value.activeArc)
    composeTestRule.onNodeWithTag("investigator_screen").assertIsDisplayed()
  }
}
