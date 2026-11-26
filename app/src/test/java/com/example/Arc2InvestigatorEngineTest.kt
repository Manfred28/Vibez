package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AssetDialogueRepository
import com.example.data.OrganStateSerializer
import com.example.game.Arc2Balance
import com.example.game.Arc2InvestigatorEngine
import com.example.game.AuditRedaction
import com.example.game.CaseAngle
import com.example.game.CaseExhibit
import com.example.game.CorroborationId
import com.example.game.DeskAnomalyType
import com.example.game.ExhibitPair
import com.example.game.GameArc
import com.example.game.InvestigatorContact
import com.example.game.OrganDevEngine
import com.example.game.OrganGameEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Arc2InvestigatorEngineTest {

  @Test
  fun lampDualLensCorkboardWarrantsReelScrubRedactionsAndPivots_workEndToEnd() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()

    var state =
      OrganDevEngine.setActiveArc(
        OrganGameEngine.initialState(catalog),
        GameArc.ARC_2_INVESTIGATION,
        catalog,
      )
    assertEquals(1, state.arc2.caseAngle)
    assertTrue(state.arc2.lampOn)
    assertEquals(0.0, state.arc2.silencePerSecond, 0.0001)
    assertEquals("dlg_arc2_entry", state.activeDialogue?.id)

    // Lamp On gathers full Leads; Lamp Off dims Leads to 0.25x and activates After-Hours Silence
    val lampOnTap = state.arc2.inspectYield
    state = Arc2InvestigatorEngine.inspectCaseFile(state, catalog)
    assertEquals(lampOnTap, state.arc2.leads, 0.001)

    state = Arc2InvestigatorEngine.toggleLamp(state, catalog)
    assertFalse(state.arc2.lampOn)
    assertEquals(1, state.arc2.lampOffCount)
    assertTrue(state.arc2.silencePerSecond > 0.0)
    assertEquals(lampOnTap * Arc2Balance.LAMP_OFF_BASE_LEADS_FACTOR, state.arc2.inspectYield, 0.01)

    state = Arc2InvestigatorEngine.tick(state, 120.0, catalog)
    assertTrue(state.arc2.silence >= 45.0)

    // Unlock Case I Silence Exhibit & wire first curated Corroboration
    assertTrue(state.arc2.canUnlockExhibit(CaseExhibit.UNLISTED_PAY_STUB))
    state = Arc2InvestigatorEngine.unlockExhibit(state, CaseExhibit.UNLISTED_PAY_STUB, catalog)
    assertTrue(CaseExhibit.UNLISTED_PAY_STUB in state.arc2.pinnedExhibits)

    state =
      Arc2InvestigatorEngine.toggleExhibitPair(
        state,
        CaseExhibit.WATER_STAINED_PETITION,
        CaseExhibit.UNLISTED_PAY_STUB,
        catalog,
      )
    assertEquals(1, state.arc2.wiredPairs.size)
    assertTrue(CorroborationId.SMUDGED_LEDGER_DATE in state.arc2.discoveredCorroborations)
    assertTrue("dlg_arc2_corr_smudged_date" in state.triggeredDialogueIds)

    // Spool cap is 1 in Case I — cannot wire a second pair until pivoting
    state = OrganDevEngine.setArc2AllExhibits(state, unlockAll = true, catalog = catalog)
    val blocked =
      Arc2InvestigatorEngine.toggleExhibitPair(
        state,
        CaseExhibit.TAX_PLAT_1998,
        CaseExhibit.SECOND_CHAIR_POLAROID,
        catalog,
      )
    assertEquals(1, blocked.arc2.wiredPairs.size)

    // Unwire & verify +15% Speculative Thread bonus on a non-curated pair
    state =
      Arc2InvestigatorEngine.toggleExhibitPair(
        state,
        CaseExhibit.WATER_STAINED_PETITION,
        CaseExhibit.UNLISTED_PAY_STUB,
        catalog,
      )
    assertEquals(0, state.arc2.wiredPairs.size)
    val baseYield = state.arc2.inspectYield
    state =
      Arc2InvestigatorEngine.toggleExhibitPair(
        state,
        CaseExhibit.WATER_STAINED_PETITION,
        CaseExhibit.TAX_PLAT_1998,
        catalog,
      )
    assertEquals(1, state.arc2.wiredPairs.size)
    assertTrue(state.arc2.inspectYield > baseYield * 1.14)

    // Case I Soft-Reset Pivot wipes Leads & Contacts, keeps Exhibits & Corroborations, adds 1 spool
    state = OrganDevEngine.assignArc2Resources(state, leads = 12_000.0, silence = 120.0, catalog = catalog)
    state = Arc2InvestigatorEngine.recruitContact(state, InvestigatorContact.NIGHT_ARCHIVIST, catalog)
    assertEquals(1, state.arc2.contactCount(InvestigatorContact.NIGHT_ARCHIVIST))
    assertTrue(state.arc2.canPivotNextCase)
    state = Arc2InvestigatorEngine.pivotNextCase(state, catalog)
    assertEquals(2, state.arc2.caseAngle)
    assertEquals(1, state.arc2.completedCasePivots)
    assertEquals(0.0, state.arc2.leads, 0.001)
    assertTrue(state.arc2.contacts.isEmpty())
    assertEquals(2, state.arc2.maxStringSpools)
    assertTrue(CaseExhibit.TAX_PLAT_1998 in state.arc2.pinnedExhibits)
    assertTrue(CorroborationId.SMUDGED_LEDGER_DATE in state.arc2.discoveredCorroborations)

    // Case III unlocks Bulk Warrants & Acoustic Reel Scrubbing (Lamp Off only)
    state = OrganDevEngine.assignArc2Resources(state, leads = 50_000.0, silence = 500.0, caseAngle = 3, catalog = catalog)
    assertTrue(state.arc2.isBulkWarrantUnlocked)
    assertTrue(state.arc2.isReelScrubUnlocked)
    state = Arc2InvestigatorEngine.recruitMaxContact(state, InvestigatorContact.NIGHT_ARCHIVIST, catalog)
    assertTrue(state.arc2.contactCount(InvestigatorContact.NIGHT_ARCHIVIST) > 5)

    if (state.arc2.lampOn) state = Arc2InvestigatorEngine.toggleLamp(state, catalog)
    val preScrubRate = state.arc2.silencePerSecond
    assertTrue(state.arc2.canTriggerReelScrub)
    state = Arc2InvestigatorEngine.triggerReelScrub(state, catalog)
    assertEquals(1, state.arc2.reelScrubCount)
    assertTrue(state.arc2.silencePerSecond > preScrubRate * 2.5)

    // Case IV unlocks 4 Audit Redactions (Lamp Off only); Clause III unlocks 15% Lamp-On Silence trickle
    state = OrganDevEngine.assignArc2Resources(state, leads = 15_000_000.0, silence = 20_000.0, caseAngle = 4, catalog = catalog)
    assertTrue(state.arc2.isRedactionSolventUnlocked)
    AuditRedaction.entries.forEach { clause ->
      assertTrue(state.arc2.canDissolveRedaction(clause))
      state = Arc2InvestigatorEngine.dissolveRedaction(state, clause, catalog)
    }
    assertEquals(4, state.arc2.dissolvedRedactions.size)

    state = Arc2InvestigatorEngine.toggleLamp(state, catalog)
    assertTrue(state.arc2.lampOn)
    assertTrue("Clause III must enable 15% Lamp-On Silence trickle", state.arc2.silencePerSecond > 0.0)
  }

  @Test
  fun slowBoilAnomalyQuarantine_preventsEnamelExhibitInCasesOneThroughThree() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()

    for (angle in 1..3) {
      var state =
        OrganDevEngine.assignArc2Resources(
          OrganDevEngine.setActiveArc(
            OrganGameEngine.initialState(catalog),
            GameArc.ARC_2_INVESTIGATION,
            catalog,
          ),
          caseAngle = angle,
          catalog = catalog,
        )
      assertTrue(
        "Case $angle hauntDepth must stay quarantined <= 0.56 (got ${state.arc2.hauntDepth})",
        state.arc2.hauntDepth <= 0.56f,
      )
      repeat(40) {
        state = state.copy(arc2 = state.arc2.copy(activeDeskAnomalies = emptyList()))
        state = OrganDevEngine.spawnDeskAnomaly(state)
        assertNotEquals(
          "enamel_exhibit must never spawn in Case $angle",
          DeskAnomalyType.ENAMEL_EXHIBIT,
          state.arc2.activeDeskAnomalies.first().type,
        )
      }
    }
  }

  @Test
  fun schemaV2ToV3Migration_preservesArc1AndMapsLegacyArc2FieldsSafely() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()

    val legacyV2 =
      """
      {
        "schemaVersion": 2,
        "activeArc": "arc_2",
        "ichor": 123456,
        "cycleLifetimeIchor": 250000,
        "stillness": 99,
        "cycleLifetimeStillness": 150,
        "scarTissue": 12,
        "availableScarTissue": 5,
        "highestExcisionMilestone": 6,
        "unlockedScarNodes": ["first_incision"],
        "cycle": 6,
        "arc2": {
          "leads": 8800,
          "lifetimeLeads": 25000,
          "silence": 75,
          "lifetimeSilence": 140,
          "complicity": 0.45,
          "contacts": {"night_archivist": 4},
          "ownedDeductions": ["hydro_ward_invoices", "missing_caretaker_roster"],
          "completedChapters": 2,
          "activeDeskAnomalies": [{"id": 7, "type": "red_string", "xFraction": 0.2, "yFraction": 0.4}],
          "deskAnomalyInteractions": {"red_string": 3},
          "fileZeroRevealed": false
        }
      }
      """.trimIndent()

    val migrated = OrganStateSerializer.deserialize(legacyV2, catalog)
    assertNotNull(migrated)
    requireNotNull(migrated)
    assertEquals(123_456L, migrated.ichorDisplay)
    assertEquals(12L, migrated.scarTissue)
    assertEquals(6, migrated.highestExcisionMilestone)
    assertEquals(3, migrated.arc2.caseAngle)
    assertEquals(2, migrated.arc2.completedCasePivots)
    assertTrue(migrated.arc2.lampOn)
    assertTrue(CaseExhibit.TAX_PLAT_1998 in migrated.arc2.pinnedExhibits)
    assertTrue(CaseExhibit.SECOND_CHAIR_POLAROID in migrated.arc2.pinnedExhibits)
    assertTrue(CaseExhibit.UNLISTED_PAY_STUB in migrated.arc2.pinnedExhibits)
    assertTrue(CaseExhibit.INSIDE_CONDENSATION_SWAB in migrated.arc2.pinnedExhibits)
    assertEquals(3, migrated.arc2.deskAnomalyInteractions[DeskAnomalyType.ENAMEL_EXHIBIT])
  }

  @Test
  fun arc2FiveCasePacingSimulation_completesInTargetFiftyToSeventyFiveMinuteWindow() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()

    val deterministicRng = kotlin.random.Random(1337)

    var state =
      OrganDevEngine.setActiveArc(
        OrganGameEngine.initialState(catalog),
        GameArc.ARC_2_INVESTIGATION,
        catalog,
      )
    val priorityPairs =
      listOf(
        CaseExhibit.TAX_PLAT_1998 to CaseExhibit.DRAINAGE_LOOP_BLUEPRINT,
        CaseExhibit.WATER_STAINED_PETITION to CaseExhibit.INTAKE_THUMB_CAST,
        CaseExhibit.DRAINAGE_LOOP_BLUEPRINT to CaseExhibit.INTAKE_THUMB_CAST,
        CaseExhibit.COAT_CLAIM_TICKET to CaseExhibit.SECOND_CHAIR_POLAROID,
        CaseExhibit.TAX_PLAT_1998 to CaseExhibit.SECOND_CHAIR_POLAROID,
        CaseExhibit.UNLISTED_PAY_STUB to CaseExhibit.TUESDAY_STATEMENT_COPY,
        CaseExhibit.INSIDE_CONDENSATION_SWAB to CaseExhibit.TUESDAY_STATEMENT_COPY,
        CaseExhibit.COAT_CLAIM_TICKET to CaseExhibit.REEL_FOUR_WAVEFORM,
        CaseExhibit.INSIDE_CONDENSATION_SWAB to CaseExhibit.REEL_FOUR_WAVEFORM,
        CaseExhibit.WATER_STAINED_PETITION to CaseExhibit.UNLISTED_PAY_STUB,
      )

    var elapsedSeconds = 0
    val maxSeconds = 90 * 60

    while (!state.arc2.fileZeroRevealed && elapsedSeconds < maxSeconds) {
      val curCase = state.arc2.currentCase

      // 1. Unlock affordable Silence Exhibits
      CaseExhibit.entries.forEach { ex ->
        if (state.arc2.canUnlockExhibit(ex)) {
          state = Arc2InvestigatorEngine.unlockExhibit(state, ex, catalog)
        }
      }

      // 2. Wire highest-value available curated corroborations up to spool cap
      val desiredPairs = mutableListOf<ExhibitPair>()
      for ((a, b) in priorityPairs) {
        if (a in state.arc2.pinnedExhibits && b in state.arc2.pinnedExhibits) {
          desiredPairs.add(ExhibitPair.canonical(a, b))
          if (desiredPairs.size >= state.arc2.maxStringSpools) break
        }
      }
      state.arc2.wiredPairs.toList().forEach { existing ->
        if (existing !in desiredPairs) {
          state = Arc2InvestigatorEngine.toggleExhibitPair(state, existing.first, existing.second, catalog)
        }
      }
      desiredPairs.forEach { pair ->
        if (pair !in state.arc2.wiredPairs && state.arc2.wiredPairs.size < state.arc2.maxStringSpools) {
          state = Arc2InvestigatorEngine.toggleExhibitPair(state, pair.first, pair.second, catalog)
        }
      }

      // 3. Lamp On vs Lamp Off strategy
      val hasUnpinnedEx =
        CaseExhibit.entries.any {
          !it.isKeystone && it.unlockCaseAngle <= state.arc2.caseAngle && it !in state.arc2.pinnedExhibits
        }
      val hasUndissolvedRedaction =
        state.arc2.caseAngle >= 4 && state.arc2.dissolvedRedactions.size < AuditRedaction.entries.size
      val needSilence =
        hasUnpinnedEx ||
          hasUndissolvedRedaction ||
          curCase.requiredReelScrubs > state.arc2.reelScrubCount ||
          state.arc2.silence < curCase.silenceCost.toDouble()
      val wantLampOn = !needSilence || state.arc2.totalContactsOwned < 6
      if (state.arc2.lampOn != wantLampOn) {
        state = Arc2InvestigatorEngine.toggleLamp(state, catalog)
      }

      // 4. Reel Scrub & Audit Redactions while Lamp Off
      if (!state.arc2.lampOn && state.arc2.canTriggerReelScrub) {
        state = Arc2InvestigatorEngine.triggerReelScrub(state, catalog)
      }
      if (!state.arc2.lampOn) {
        AuditRedaction.entries.forEach { clause ->
          if (state.arc2.canDissolveRedaction(clause)) {
            state = Arc2InvestigatorEngine.dissolveRedaction(state, clause, catalog)
          }
        }
      }

      // 5. Interact with active Desk Anomalies
      state.arc2.activeDeskAnomalies.toList().forEach { anomaly ->
        state = Arc2InvestigatorEngine.interactDeskAnomaly(state, anomaly.id, catalog)
      }

      // 6. Recruit best ROI contact when affordable
      if (state.arc2.leads < curCase.leadsCost.toDouble()) {
        var bestContact: InvestigatorContact? = null
        var bestPayback = Double.POSITIVE_INFINITY
        for (c in InvestigatorContact.entries) {
          if (!state.arc2.isContactUnlocked(c)) continue
          val cost = state.arc2.contactCost(c).toDouble()
          if (state.arc2.leads < cost) continue
          val remainingNeeded = curCase.leadsCost.toDouble() - state.arc2.leads
          if (cost > remainingNeeded * 0.45 && state.arc2.leadsPerSecond > 0.0) continue
          val payback = cost / c.baseLeadsPerSec
          if (payback < bestPayback) {
            bestPayback = payback
            bestContact = c
          }
        }
        if (bestContact != null) {
          state = Arc2InvestigatorEngine.recruitContact(state, bestContact, catalog)
        }
      }

      // 7. Moderate human tapping
      val tapsThisSec =
        if (state.arc2.lampOn) {
          if (elapsedSeconds % 2 == 0) 2 else 1
        } else {
          if (elapsedSeconds % 4 == 0) 1 else 0
        }
      repeat(tapsThisSec) {
        state = Arc2InvestigatorEngine.inspectCaseFile(state, catalog)
      }

      // 8. Pivot Case Angle as soon as ready
      if (state.arc2.canPivotNextCase) {
        state = Arc2InvestigatorEngine.pivotNextCase(state, catalog)
      }

      state = Arc2InvestigatorEngine.tick(state, 1.0, catalog, random = deterministicRng)
      elapsedSeconds += 1
    }

    val elapsedMinutes = elapsedSeconds / 60.0
    assertTrue(
      "Expected File #00 to be unsealed within 90m; stuck at Case ${state.arc2.caseAngle}",
      state.arc2.fileZeroRevealed,
    )
    assertTrue(
      "Expected 5-Case Arc 2 pacing in ~50-75m band (tolerance 45-80m), actual: ${"%.1f".format(elapsedMinutes)} min",
      elapsedMinutes in 45.0..80.0,
    )
  }
}
