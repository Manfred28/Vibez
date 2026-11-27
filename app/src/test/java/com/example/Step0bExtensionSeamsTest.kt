package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.audio.InMemoryAudioEngine
import com.example.data.Arc1SaveCodec
import com.example.data.Arc2SaveCodec
import com.example.data.Arc3SaveCodec
import com.example.data.AssetDialogueRepository
import com.example.data.DialogueCatalog
import com.example.data.OrganStateSerializer
import com.example.game.ActiveDeskAnomaly
import com.example.game.Arc2DeskState
import com.example.game.Arc2InvestigatorEngine
import com.example.game.Arc3BreachState
import com.example.game.Arc3Stratum
import com.example.game.Arc3UnmakeAct
import com.example.game.AuditRedaction
import com.example.game.CaseExhibit
import com.example.game.CorroborationId
import com.example.game.CreatureType
import com.example.game.DeskAnomalyType
import com.example.game.ExhibitPair
import com.example.game.GameArc
import com.example.game.InvestigatorContact
import com.example.game.OrganGameEngine
import com.example.game.ScarNodeId
import com.example.ui.Arc3BreachActions
import com.example.ui.Arc3ConvergenceScreen
import com.example.ui.InvestigatorActions
import com.example.ui.InvestigatorScreen
import com.example.ui.OrganViewModel
import com.example.ui.theme.BleakTheme
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Step0bExtensionSeamsTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun saveSerializer_routesV1V2AndForwardV3SchemasWithIsolatedArcCodecs() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()

    // 1. Legacy v1 payload (no schemaVersion, no availableScarTissue, no highestExcisionMilestone, no arc2)
    val v1Raw =
      """{"cycle":2,"scarTissue":5,"ichor":420,"cycleLifetimeIchor":800,"creatures":{"suture_hand":3}}"""
    val v1Root = JSONObject(v1Raw)
    assertEquals(1, OrganStateSerializer.detectSchemaVersion(v1Root))
    val migratedV1 = OrganStateSerializer.deserialize(v1Raw, catalog)
    assertNotNull(migratedV1)
    assertEquals(GameArc.ARC_1_CHAMBER, migratedV1!!.activeArc)
    assertEquals(5L, migratedV1.scarTissue)
    assertEquals(5L, migratedV1.availableScarTissue)
    assertEquals(1, migratedV1.highestExcisionMilestone)
    assertEquals(3, migratedV1.creatureCount(CreatureType.SUTURE_HAND))
    assertEquals(0.0, migratedV1.arc2.leads, 0.001)

    // 2. Native v4 round-trip via isolated Arc1SaveCodec + Arc2SaveCodec + Arc3SaveCodec
    val richState =
      OrganGameEngine.initialState(catalog).copy(
        activeArc = GameArc.ARC_2_INVESTIGATION,
        cycle = 3,
        scarTissue = 14L,
        availableScarTissue = 9L,
        highestExcisionMilestone = 6,
        unlockedScarNodes = setOf(ScarNodeId.FIRST_INCISION),
        arc2 =
          Arc2DeskState(
            leads = 320.0,
            lifetimeLeads = 640.0,
            silence = 18.0,
            lifetimeSilence = 24.0,
            caseAngle = 3,
            completedCasePivots = 2,
            lampOn = false,
            contacts = mapOf(InvestigatorContact.NIGHT_ARCHIVIST to 2),
            pinnedExhibits = setOf(CaseExhibit.WATER_STAINED_PETITION, CaseExhibit.UNLISTED_PAY_STUB, CaseExhibit.TAX_PLAT_1998),
            wiredPairs = setOf(ExhibitPair.canonical(CaseExhibit.WATER_STAINED_PETITION, CaseExhibit.UNLISTED_PAY_STUB)),
            discoveredCorroborations = setOf(CorroborationId.SMUDGED_LEDGER_DATE),
            deskAnomalyInteractions = mapOf(DeskAnomalyType.EVIDENCE_ENVELOPE to 3),
            fileZeroRevealed = false,
          ),
        arc3 =
          Arc3BreachState(
            currentStratum = 2,
            lockedWedges = setOf(Arc3Stratum.BROKEN_GROUT),
            fracture = 180.0,
            hemorrhage = 420.0,
            lifetimeHemorrhage = 610.0,
            wrenchCount = 19,
            completedUnmakes = setOf(Arc3UnmakeAct.SHATTER_YESTERDAYS_DUST),
          ),
      )
    val serializedV4 = OrganStateSerializer.serialize(richState)
    val parsedV4 = JSONObject(serializedV4)
    assertEquals(4, OrganStateSerializer.detectSchemaVersion(parsedV4))
    val restoredV4 = OrganStateSerializer.deserialize(serializedV4, catalog)
    assertNotNull(restoredV4)
    assertEquals(GameArc.ARC_2_INVESTIGATION, restoredV4!!.activeArc)
    assertEquals(14L, restoredV4.scarTissue)
    assertEquals(9L, restoredV4.availableScarTissue)
    assertEquals(320.0, restoredV4.arc2.leads, 0.001)
    assertEquals(2, restoredV4.arc2.contactCount(InvestigatorContact.NIGHT_ARCHIVIST))
    assertEquals(3, restoredV4.arc2.caseAngle)
    assertEquals(2, restoredV4.arc2.completedCasePivots)
    assertEquals(false, restoredV4.arc2.lampOn)
    assertTrue(CorroborationId.SMUDGED_LEDGER_DATE in restoredV4.arc2.discoveredCorroborations)
    assertEquals(2, restoredV4.arc3.currentStratum)
    assertTrue(Arc3Stratum.BROKEN_GROUT in restoredV4.arc3.lockedWedges)
    assertTrue(Arc3UnmakeAct.SHATTER_YESTERDAYS_DUST in restoredV4.arc3.completedUnmakes)

    // Direct codec isolation check
    val isolatedArc2 = Arc2SaveCodec.decode(Arc2SaveCodec.encode(richState.arc2), schemaVersion = 4)
    assertEquals(richState.arc2.leads, isolatedArc2.leads, 0.001)
    assertEquals(richState.arc2.completedCasePivots, isolatedArc2.completedCasePivots)
    val isolatedArc3 = Arc3SaveCodec.decode(Arc3SaveCodec.encode(richState.arc3))
    assertEquals(2, isolatedArc3.currentStratum)
    assertEquals(19, isolatedArc3.wrenchCount)
    val isolatedArc1 = Arc1SaveCodec.decode(parsedV4, schemaVersion = 4)
    assertEquals(richState.scarTissue, isolatedArc1.scarTissue)

    // 3. v3 -> v4 migration (missing arc3 object defaults cleanly to fresh Stratum I)
    val v3Root = JSONObject(serializedV4)
    v3Root.put("schemaVersion", 3)
    v3Root.remove("arc3")
    assertEquals(3, OrganStateSerializer.detectSchemaVersion(v3Root))
    val migratedV3 = OrganStateSerializer.deserialize(v3Root.toString(), catalog)
    assertNotNull(migratedV3)
    assertEquals(1, migratedV3!!.arc3.currentStratum)
    assertTrue(migratedV3.arc3.lockedWedges.isEmpty())

    // 4. Forward-compatible v5 payload with extra keys preserves Arc 1, Arc 2, and Arc 3 cleanly
    val v5Root = JSONObject(serializedV4)
    v5Root.put("schemaVersion", 5)
    v5Root.getJSONObject("arc3").put("futureFlag", true)
    assertEquals(5, OrganStateSerializer.detectSchemaVersion(v5Root))

    val restoredV5 = OrganStateSerializer.deserialize(v5Root.toString(), catalog)
    assertNotNull(restoredV5)
    assertEquals(14L, restoredV5!!.scarTissue)
    assertEquals(9L, restoredV5.availableScarTissue)
    assertEquals(320.0, restoredV5.arc2.leads, 0.001)
    assertEquals(2, restoredV5.arc3.currentStratum)
  }

  @Test
  fun viewModel_investigatorActions_delegatesAllElevenArc2Actions() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vm =
      OrganViewModel(
        dialogueRepository = AssetDialogueRepository(context),
        saveRepository = null,
        autoTick = false,
        audioEngine = InMemoryAudioEngine(),
      )
    val actions = vm.investigatorActions

    vm.devSetActiveArc(GameArc.ARC_2_INVESTIGATION)
    vm.devAssignArc2Resources(leads = 15_000.0, silence = 200.0, caseAngle = 1, completedChapters = 0)
    vm.devSpawnDeskAnomaly()

    val beforeLeads = vm.uiState.value.arc2.leads
    actions.onInspectCaseFile()
    assertTrue(vm.uiState.value.arc2.leads > beforeLeads)

    actions.onToggleLamp()
    assertEquals(false, vm.uiState.value.arc2.lampOn)

    actions.onRecruitContact(InvestigatorContact.NIGHT_ARCHIVIST)
    assertEquals(1, vm.uiState.value.arc2.contactCount(InvestigatorContact.NIGHT_ARCHIVIST))

    actions.onUnlockExhibit(CaseExhibit.UNLISTED_PAY_STUB)
    assertTrue(CaseExhibit.UNLISTED_PAY_STUB in vm.uiState.value.arc2.pinnedExhibits)

    actions.onToggleExhibitPair(CaseExhibit.WATER_STAINED_PETITION, CaseExhibit.UNLISTED_PAY_STUB)
    assertEquals(1, vm.uiState.value.arc2.wiredPairs.size)

    val anomalyId = vm.uiState.value.arc2.activeDeskAnomalies.first().id
    actions.onInteractDeskAnomaly(anomalyId)
    assertTrue(vm.uiState.value.arc2.activeDeskAnomalies.none { it.id == anomalyId })

    actions.onPivotNextCase()
    assertEquals(2, vm.uiState.value.arc2.caseAngle)
    assertEquals(1, vm.uiState.value.arc2.completedCasePivots)

    // Case IV actions: bulk warrant, reel scrub, dissolve redaction
    vm.devAssignArc2Resources(leads = 50_000.0, silence = 20_000.0, caseAngle = 4, completedChapters = 3)
    actions.onRecruitMaxContact(InvestigatorContact.NIGHT_ARCHIVIST)
    assertTrue(vm.uiState.value.arc2.contactCount(InvestigatorContact.NIGHT_ARCHIVIST) > 5)
    actions.onTriggerReelScrub()
    assertEquals(1, vm.uiState.value.arc2.reelScrubCount)
    actions.onDissolveRedaction(AuditRedaction.CLAUSE_SEEPAGE)
    assertTrue(AuditRedaction.CLAUSE_SEEPAGE in vm.uiState.value.arc2.dissolvedRedactions)

    vm.devAssignArc2Resources(completedChapters = 5)
    actions.onEnterArc3()
    assertEquals(GameArc.ARC_3_CONVERGENCE, vm.uiState.value.activeArc)
  }

  @Test
  fun investigatorScreen_usesSharedTopChromeGroupedActionsAndCrossArcEventDripLane() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = DialogueCatalog.Empty
    val anomaly =
      ActiveDeskAnomaly(
        id = 51L,
        type = DeskAnomalyType.EVIDENCE_ENVELOPE,
        xFraction = 0.2f,
        yFraction = 0.25f,
        remainingSeconds = 14.0,
      )
    var state by
      mutableStateOf(
        OrganGameEngine.initialState(catalog).copy(
          activeArc = GameArc.ARC_2_INVESTIGATION,
          arc2 = Arc2DeskState(leads = 10.0, lifetimeLeads = 10.0, activeDeskAnomalies = listOf(anomaly)),
        )
      )
    var muteToggled = 0
    var devOpened = 0
    var inspectClicks = 0

    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      BleakTheme(phase = 5, investigatorMode = true) {
        InvestigatorScreen(
          state = state,
          actions =
            InvestigatorActions(
              onInspectCaseFile = {
                inspectClicks++
                state = Arc2InvestigatorEngine.inspectCaseFile(state, catalog)
              },
              onInteractDeskAnomaly = { id ->
                state = Arc2InvestigatorEngine.interactDeskAnomaly(state, id, catalog)
              },
            ),
          onOpenDevTools = { devOpened++ },
          isAudioMuted = false,
          onToggleAudioMute = { muteToggled++ },
          eventDripRoll = { 0.0 },
        )
      }
    }
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.waitForIdle()

    // Shared top-bar chrome controls present and interactive
    composeTestRule.onNodeWithTag("arc2_phase_header").assertIsDisplayed()
    composeTestRule.onNodeWithTag("audio_mute_toggle").assertIsDisplayed().performClick()
    composeTestRule.onNodeWithTag("open_dev_tools_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, muteToggled)
    assertEquals(1, devOpened)

    // Grouped InvestigatorActions dispatches stamp clicks cleanly
    composeTestRule.onNodeWithTag("inspect_case_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, inspectClicks)

    // Tapping a Desk Anomaly emits into the shared left-margin DrippingTransmissionOverlay
    composeTestRule.onNodeWithTag("desk_anomaly_evidence_envelope_51").performClick()
    composeTestRule.mainClock.advanceTimeBy(500)
    composeTestRule.waitForIdle()

    val expectedSpeaker = "// ${context.getString(R.string.drip_desk_event_speaker)}"
    val expectedBody = context.getString(R.string.drip_desk_anomaly_envelope)
    composeTestRule
      .onNodeWithTag("dripping_event_overlay")
      .assertIsDisplayed()
      .assert(hasAnyDescendant(hasText(expectedSpeaker)))
      .assert(hasAnyDescendant(hasText(expectedBody)))
  }

  @Test
  fun arc3ConvergenceScreen_usesSharedTopChromeWithoutDuplicateDevTrigger() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()
    val state = OrganGameEngine.initialState(catalog).copy(activeArc = GameArc.ARC_3_CONVERGENCE)
    var muteToggled = 0
    var devOpened = 0

    composeTestRule.setContent {
      BleakTheme(phase = 7, breachMode = true) {
        Arc3ConvergenceScreen(
          state = state,
          actions = Arc3BreachActions(),
          onOpenDevTools = { devOpened++ },
          isAudioMuted = false,
          onToggleAudioMute = { muteToggled++ },
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("audio_mute_toggle").assertIsDisplayed().performClick()
    composeTestRule.onNodeWithTag("open_dev_tools_button").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, muteToggled)
    assertEquals(1, devOpened)
  }
}
