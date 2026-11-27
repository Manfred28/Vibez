package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AssetDialogueRepository
import com.example.data.DialogueCatalog
import com.example.data.OrganStateSerializer
import com.example.game.ActiveBreachRupture
import com.example.game.Arc2DeskState
import com.example.game.Arc3Balance
import com.example.game.Arc3BreachEngine
import com.example.game.Arc3BreachState
import com.example.game.Arc3Stratum
import com.example.game.Arc3UnmakeAct
import com.example.game.BreachRuptureType
import com.example.game.CorroborationId
import com.example.game.GameArc
import com.example.game.OrganBalance
import com.example.game.OrganDevEngine
import com.example.game.OrganGameEngine
import com.example.game.OrganGameState
import com.example.game.ScarNodeId
import com.example.game.SpecimenJar
import com.example.game.arc3LegacyFractureMultiplier
import com.example.game.arc3LegacyHemorrhageMultiplier
import com.example.game.arc3WrenchFractureYield
import com.example.game.arc3WrenchHemorrhageYield
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Arc3BreachEngineTest {

  private fun loadCatalog(): DialogueCatalog {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return AssetDialogueRepository(context).loadCatalog()
  }

  private fun freshArc3(catalog: DialogueCatalog = DialogueCatalog.Empty): OrganGameState =
    OrganGameEngine.initialState(catalog).copy(activeArc = GameArc.ARC_3_CONVERGENCE)

  @Test
  fun wrenchBreach_buildsMomentumCapsFractureAtGoalAndTriggersFirstWrenchDialogue() {
    val catalog = loadCatalog()
    var state = freshArc3(catalog)

    assertEquals(0.0, state.arc3.fracture, 0.0001)
    assertEquals(0.0, state.arc3.momentumCharge, 0.0001)

    state = Arc3BreachEngine.wrenchBreach(state, catalog)
    assertEquals(1, state.arc3.wrenchCount)
    assertEquals(Arc3Balance.BASE_WRENCH_FRACTURE, state.arc3.fracture, 0.0001)
    assertEquals(Arc3Balance.BASE_WRENCH_HEMORRHAGE, state.arc3.hemorrhage, 0.0001)
    assertEquals(Arc3Balance.MOMENTUM_GAIN_PER_TAP, state.arc3.momentumCharge, 0.0001)

    // Subsequent rapid taps build momentum up to 1.0, trigger opening wrench fragment at 3+, and boost yield
    val yieldAfterOne = state.arc3WrenchFractureYield()
    repeat(12) { state = Arc3BreachEngine.wrenchBreach(state, catalog) }
    assertTrue("Wrenching must unlock Phase 7 opening wrench fragment", "dlg_arc3_first_wrench" in state.triggeredDialogueIds)
    assertEquals(1.0, state.arc3.momentumCharge, 0.0001)
    assertTrue("Momentum must boost wrench yield", state.arc3WrenchFractureYield() > yieldAfterOne)

    // Pushing past Stratum I goal caps fracture cleanly and enables Drive Wedge
    repeat(30) { state = Arc3BreachEngine.wrenchBreach(state, catalog) }
    assertEquals(Arc3Stratum.BROKEN_GROUT.fractureGoal, state.arc3.fracture, 0.0001)
    assertTrue(state.arc3.canDriveWedge)
  }

  @Test
  fun forgivingClotting_honorsGraceWindowRampsSmoothlyFloorsAtZeroAndNeverLosesWedgesOrHemorrhage() {
    val catalog = DialogueCatalog.Empty
    var state =
      freshArc3(catalog).copy(
        arc3 =
          Arc3BreachState(
            currentStratum = 2,
            lockedWedges = setOf(Arc3Stratum.BROKEN_GROUT),
            fracture = 80.0,
            hemorrhage = 250.0,
            lifetimeHemorrhage = 250.0,
            momentumCharge = 0.8,
          )
      )

    // During 1.4s grace period, fracture does not decay at all
    state = Arc3BreachEngine.tick(state, 1.0, catalog)
    assertEquals(80.0, state.arc3.fracture, 0.0001)
    assertFalse("Clotting must remain inactive during grace window", state.arc3.isActivelyClotting)

    // After grace window expires, clotting pulls fracture downward while preserving hemorrhage & locked wedges
    state = Arc3BreachEngine.tick(state, 1.0, catalog)
    assertTrue("Clotting must engage after grace window", state.arc3.isActivelyClotting)
    assertTrue("Fracture must pull back during clotting", state.arc3.fracture < 80.0)
    assertEquals(250.0, state.arc3.hemorrhage, 0.0001)
    assertEquals(setOf(Arc3Stratum.BROKEN_GROUT), state.arc3.lockedWedges)

    // Long idle floors fracture at 0.0 without losing locked Wedge I or Hemorrhage
    state = Arc3BreachEngine.tick(state, 120.0, catalog)
    assertEquals(0.0, state.arc3.fracture, 0.0001)
    assertEquals(0.0, state.arc3.momentumCharge, 0.0001)
    assertEquals(250.0, state.arc3.hemorrhage, 0.0001)
    assertEquals(setOf(Arc3Stratum.BROKEN_GROUT), state.arc3.lockedWedges)
  }

  @Test
  fun allEighteenUnmakeActs_scalePowerReduceClottingFreezeOnJarShatterAndTriggerCanonFragments() {
    val catalog = loadCatalog()
    var state =
      freshArc3(catalog).copy(
        arc3 =
          Arc3BreachState(
            currentStratum = 5,
            lockedWedges = setOf(Arc3Stratum.BROKEN_GROUT, Arc3Stratum.UNSTITCHED_SEAM, Arc3Stratum.SPLINTERED_RIBS, Arc3Stratum.SHARED_PULSE),
            hemorrhage = 5_000_000.0,
            lifetimeHemorrhage = 5_000_000.0,
          )
      )

    val initialWrench = state.arc3WrenchFractureYield()
    assertEquals(18, Arc3UnmakeAct.entries.size)

    // Shattering first jar freezes clotting for >= 3.0s and boosts fracture
    state = Arc3BreachEngine.performUnmake(state, Arc3UnmakeAct.SHATTER_YESTERDAYS_DUST, catalog)
    assertTrue(state.arc3.isJarShattered(SpecimenJar.YESTERDAYS_DUST))
    assertTrue(state.arc3.isClotFrozen)
    assertTrue(state.arc3.clotFreezeRemainingSeconds >= 3.0)
    assertEquals(Arc3UnmakeAct.SHATTER_YESTERDAYS_DUST.instantFractureSurge, state.arc3.fracture, 0.0001)
    assertTrue("First jar shatter must trigger Phase 7 jar dialogue", "dlg_arc3_first_jar_shatter" in state.triggeredDialogueIds)

    // Re-attempting already completed Unmake is a safe no-op
    val duplicateAttempt = Arc3BreachEngine.performUnmake(state, Arc3UnmakeAct.SHATTER_YESTERDAYS_DUST, catalog)
    assertEquals(state, duplicateAttempt)

    // Complete all remaining 17 Unmaking acts
    Arc3UnmakeAct.entries.drop(1).forEach { act ->
      state = Arc3BreachEngine.performUnmake(state, act, catalog)
    }

    assertEquals(18, state.arc3.completedUnmakes.size)
    assertEquals(6, state.arc3.shatteredJarCount)
    assertEquals(6, state.arc3.reversedApparatusCount)
    assertEquals(6, state.arc3.unstitchedScarCount)
    assertTrue(state.arc3.antiClotReduction in 0.35..Arc3Balance.MAX_ANTI_CLOT_FRACTION)
    assertTrue(state.arc3.bleedPressurePerSec > 500.0)
    assertTrue(
      "All 18 Unmakes must massively multiply wrench force",
      state.arc3WrenchFractureYield() > initialWrench * 100.0,
    )
    assertTrue("All 6 jars shattered triggers canon fragment", "dlg_arc3_all_jars_shattered" in state.triggeredDialogueIds)
    assertTrue("Unstitching scars triggers canon fragment", "dlg_arc3_scar_unstitched" in state.triggeredDialogueIds)
    assertTrue("Unmaking apparatus triggers canon fragment", "dlg_arc3_first_unmake" in state.triggeredDialogueIds)
  }

  @Test
  fun breachRuptures_spawnDeliverDistinctBuffsAndTriggerRuptureDialogues() {
    val catalog = loadCatalog()
    var state = freshArc3(catalog).copy(arc3 = Arc3BreachState(currentStratum = 4))

    // Natural tick spawns a rupture once FIRST_RUPTURE_DELAY_SEC elapses
    state = Arc3BreachEngine.tick(state, Arc3Balance.FIRST_RUPTURE_DELAY_SEC + 0.1, catalog)
    assertEquals(1, state.arc3.activeRuptures.size)

    // Inject all 4 rupture types and verify distinct effects
    val injected =
      listOf(
        ActiveBreachRupture(101L, BreachRuptureType.CLOTTING_SEAM, 0.2f, 0.25f, 12.0),
        ActiveBreachRupture(102L, BreachRuptureType.SPLINTERED_RIB, 0.8f, 0.25f, 12.0),
        ActiveBreachRupture(103L, BreachRuptureType.TORN_SUTURE, 0.2f, 0.50f, 12.0),
        ActiveBreachRupture(104L, BreachRuptureType.ARTERIAL_WELL, 0.8f, 0.50f, 12.0),
      )
    state = state.copy(arc3 = state.arc3.copy(activeRuptures = injected))

    state = Arc3BreachEngine.tearBreachRupture(state, 101L, catalog)
    assertTrue("CLOTTING_SEAM freezes clotting", state.arc3.clotFreezeRemainingSeconds >= 3.5)

    state = Arc3BreachEngine.tearBreachRupture(state, 102L, catalog)
    assertTrue("SPLINTERED_RIB grants rib force", state.arc3.ribForceRemainingSeconds >= 4.5)

    state = Arc3BreachEngine.tearBreachRupture(state, 103L, catalog)
    assertTrue("TORN_SUTURE grants 2x frenzy and clot freeze", state.arc3.frenzyRemainingSeconds >= 6.0)

    state = Arc3BreachEngine.tearBreachRupture(state, 104L, catalog)
    assertEquals(4, state.arc3.totalRuptureInteractions)
    assertTrue("Tearing breach ruptures unlocks Phase 7 rupture dialogue", "dlg_arc3_first_rupture" in state.triggeredDialogueIds)
  }

  @Test
  fun sequentialFiveWedgesAndFiveInteractiveFinalPulls_leadToTotalCrimsonSubmersion() {
    val catalog = loadCatalog()
    var state = freshArc3(catalog)

    // Drive Wedges I..V sequentially
    for (stratum in Arc3Stratum.entries) {
      assertEquals(stratum.number, state.arc3.currentStratum)
      state = state.copy(arc3 = state.arc3.copy(fracture = stratum.fractureGoal))
      assertTrue("Stratum ${stratum.number} should allow driving wedge", state.arc3.canDriveWedge)
      state = Arc3BreachEngine.driveMemoryWedge(state, catalog)
      assertTrue("Wedge ${stratum.number} must be permanently locked", stratum in state.arc3.lockedWedges)
    }

    assertTrue("Locking Wedge V must enter interactive Finale", state.arc3.isInFinale)
    assertFalse(state.arc3.submersionComplete)
    assertEquals(0, state.arc3.finalPullsCompleted)
    assertEquals(0.86f, state.arc3.floodLevel, 0.001f)
    assertTrue(state.arc3.canPerformFinalPull)
    assertTrue("Locking all 5 wedges triggers Wedge V dialogue", "dlg_arc3_wedge_5_file_zero" in state.triggeredDialogueIds)

    // Wrenching & Unmaking are disabled once Finale starts
    val wrenchNoOp = Arc3BreachEngine.wrenchBreach(state, catalog)
    assertEquals(state.arc3.wrenchCount, wrenchNoOp.arc3.wrenchCount)

    // Execute 5 deliberate Final Pulls with 1.8s cooldown gating between pulls
    for (pull in 1..Arc3Balance.FINAL_PULLS_TOTAL) {
      assertTrue("Pull $pull must be ready", state.arc3.canPerformFinalPull)
      state = Arc3BreachEngine.performFinalPull(state, catalog)
      assertEquals(pull, state.arc3.finalPullsCompleted)
      if (pull < Arc3Balance.FINAL_PULLS_TOTAL) {
        assertFalse("Cooldown must gate immediate next pull after pull $pull", state.arc3.canPerformFinalPull)
        assertEquals(Arc3Balance.FINAL_PULL_COOLDOWN_SEC, state.arc3.finalPullCooldownSeconds, 0.001)
        // Attempting pull during cooldown is ignored
        val blocked = Arc3BreachEngine.performFinalPull(state, catalog)
        assertEquals(pull, blocked.arc3.finalPullsCompleted)
        // Tick down cooldown
        state = Arc3BreachEngine.tick(state, Arc3Balance.FINAL_PULL_COOLDOWN_SEC + 0.05, catalog)
        assertTrue(state.arc3.canPerformFinalPull)
      }
    }

    assertTrue("5th pull must complete total crimson submersion", state.arc3.submersionComplete)
    assertEquals(1.0f, state.arc3.floodLevel, 0.0001f)
    assertEquals(1.0f, state.arc3.telemetryBlurFactor, 0.0001f)
    assertTrue("Final ambiguous crimson flood dialogue must trigger", "dlg_arc3_submersion_end" in state.triggeredDialogueIds)
  }

  @Test
  fun legacyReflection_reflectsRealArc1AndArc2ProgressAndFallsBackSafelyForDevEntry() {
    val devEmpty = freshArc3()
    assertEquals(1.0, devEmpty.arc3LegacyHemorrhageMultiplier(), 0.0001)
    assertEquals(1.0, devEmpty.arc3LegacyFractureMultiplier(), 0.0001)

    val fullLegacy =
      devEmpty.copy(
        ownedJars = SpecimenJar.entries.toSet(),
        unlockedScarNodes = ScarNodeId.entries.toSet(),
        arc2 = Arc2DeskState(discoveredCorroborations = CorroborationId.entries.toSet()),
      )
    assertEquals(1.30, fullLegacy.arc3LegacyHemorrhageMultiplier(), 0.0001)
    assertEquals(1.30, fullLegacy.arc3LegacyFractureMultiplier(), 0.0001)
    assertTrue(fullLegacy.arc3WrenchFractureYield() > devEmpty.arc3WrenchFractureYield())
    assertTrue(fullLegacy.arc3WrenchHemorrhageYield() > devEmpty.arc3WrenchHemorrhageYield())
  }

  @Test
  fun persistenceAndNonDestructiveIsolation_preserveArc3AcrossV4SerializationAndArc1Excision() {
    val catalog = loadCatalog()
    val state =
      OrganGameEngine.initialState(catalog).copy(
        activeArc = GameArc.ARC_1_CHAMBER,
        ichor = OrganBalance.EXCISION_ICHOR_THRESHOLD + 5_000.0,
        cycleLifetimeIchor = OrganBalance.EXCISION_ICHOR_THRESHOLD + 5_000.0,
        phase = 4,
        arc3 =
          Arc3BreachState(
            currentStratum = 3,
            lockedWedges = setOf(Arc3Stratum.BROKEN_GROUT, Arc3Stratum.UNSTITCHED_SEAM),
            fracture = 900.0,
            hemorrhage = 3_400.0,
            lifetimeHemorrhage = 5_100.0,
            wrenchCount = 44,
            completedUnmakes = setOf(Arc3UnmakeAct.SHATTER_YESTERDAYS_DUST, Arc3UnmakeAct.REVERSE_SUTURE_HANDS),
          ),
      )

    // Arc 1 Excision must preserve Arc 3 progress intact
    val afterExcision = OrganGameEngine.performExcision(state, catalog)
    assertEquals(3, afterExcision.arc3.currentStratum)
    assertEquals(2, afterExcision.arc3.lockedWedges.size)
    assertEquals(44, afterExcision.arc3.wrenchCount)

    // Schema v4 round-trip preserves full Arc 3 breach state
    val json = OrganStateSerializer.serialize(afterExcision)
    assertEquals(4, OrganStateSerializer.detectSchemaVersion(JSONObject(json)))
    val restored = OrganStateSerializer.deserialize(json, catalog)
    assertNotNull(restored)
    assertEquals(3, restored!!.arc3.currentStratum)
    assertEquals(900.0, restored.arc3.fracture, 0.001)
    assertEquals(3_400.0, restored.arc3.hemorrhage, 0.001)
    assertEquals(2, restored.arc3.completedUnmakes.size)
  }

  @Test
  fun devEngineArc3Actions_jumpStrataGrantResourcesSpawnRuptureEnterFinaleAndReset() {
    val catalog = loadCatalog()
    var state = freshArc3(catalog)

    state = OrganDevEngine.setArc3Stratum(state, 4, catalog)
    assertEquals(4, state.arc3.currentStratum)
    assertEquals(3, state.arc3.lockedWedges.size)
    assertTrue("Stratum IV shears the valve into the Open Seam", state.arc3.isValveSheared)

    state = OrganDevEngine.assignArc3Resources(state, hemorrhage = 50_000.0, fillFracture = true, catalog = catalog)
    assertEquals(50_000.0, state.arc3.hemorrhage, 0.001)
    assertTrue(state.arc3.canDriveWedge)

    state = OrganDevEngine.spawnBreachRupture(state)
    assertEquals(1, state.arc3.activeRuptures.size)

    state = OrganDevEngine.jumpArc3Finale(state, submerged = false, catalog = catalog)
    assertTrue(state.arc3.isInFinale)
    assertFalse(state.arc3.submersionComplete)

    state = OrganDevEngine.jumpArc3Finale(state, submerged = true, catalog = catalog)
    assertTrue(state.arc3.submersionComplete)
    assertEquals(1.0f, state.arc3.floodLevel, 0.0001f)

    state = OrganDevEngine.resetArc3(state, catalog)
    assertEquals(1, state.arc3.currentStratum)
    assertTrue(state.arc3.lockedWedges.isEmpty())
    assertFalse(state.arc3.submersionComplete)
  }
}
