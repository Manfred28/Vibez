package com.example

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.example.data.AssetDialogueRepository
import com.example.data.EscalationCurve
import com.example.data.OrganStateSerializer
import com.example.data.SharedPreferencesOrganSaveRepository
import com.example.game.OrganBalance
import com.example.game.OrganDevEngine
import com.example.game.OrganGameEngine
import com.example.game.ScarNodeId
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DialogueAssetValidationTest {

  private fun loadDialoguesJson(): JSONObject {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val raw = context.assets.open("dialogues.json").bufferedReader().use { it.readText() }
    return JSONObject(raw)
  }

  @Test
  fun dialoguesAsset_existsAndFollowsDesignBibleSchema() {
    val root = loadDialoguesJson()

    assertTrue("schemaVersion must be >= 3", root.getInt("schemaVersion") >= 3)
    assertEquals("GAME_DESIGN_BIBLE.md", root.getString("designReference"))

    val phases = root.getJSONArray("phases")
    assertEquals("Must define escalation phases 0..6 (Arc 1 + Arc 2 + Arc 3)", 7, phases.length())
    for (index in 0 until phases.length()) {
      val phase = phases.getJSONObject(index)
      assertEquals(index, phase.getInt("id"))
      assertFalse(phase.getString("name").isBlank())
      assertFalse(phase.getString("intensity").isBlank())
    }
  }

  @Test
  fun dialogueEntries_haveUniqueIdsValidTriggersCoverAllPhasesAndIncludeMultiArcFragments() {
    val allowedTriggerTypes =
      setOf(
        "counter_reached",
        "cycle_counter_reached",
        "creature_owned",
        "anomaly_interacted",
        "stillness_reached",
        "jar_bottled",
        "excision_completed",
        "excision_milestone_reached",
        "scar_node_unlocked",
        "relic_dredged",
        "echo_routed",
        "marrow_bloom",
        "dossier_line_unredacted",
        "arc_entered",
        "case_chapter_completed",
        "desk_anomaly_interacted",
      )
    val targetedTypes =
      setOf(
        "cycle_counter_reached",
        "creature_owned",
        "anomaly_interacted",
        "arc_entered",
        "desk_anomaly_interacted",
      )

    val root = loadDialoguesJson()
    val entries = root.getJSONArray("entries")
    assertTrue("dialogues.json must contain all Arc 1 + Excision I-VI + Arc 2 + Arc 3 fragments", entries.length() >= 45)

    val seenIds = mutableSetOf<String>()
    val counterThresholdByPhase = mutableMapOf<Int, Long>()
    val allCounterThresholds = mutableSetOf<Long>()
    val seenMilestones = mutableSetOf<Long>()
    val seenDossierLines = mutableSetOf<Long>()
    val seenChapters = mutableSetOf<Long>()
    val seenArcs = mutableSetOf<String>()
    var hasExcisionEntry = false

    for (index in 0 until entries.length()) {
      val entry = entries.getJSONObject(index)
      val id = entry.getString("id")
      assertFalse("Dialogue id must not be blank", id.isBlank())
      assertTrue("Duplicate dialogue id: $id", seenIds.add(id))

      val phase = entry.getInt("phase")
      assertTrue("Phase $phase out of range 0..6", phase in 0..6)
      assertFalse("Mood must not be blank for $id", entry.getString("mood").isBlank())
      assertFalse("Speaker must not be blank for $id", entry.getString("speaker").isBlank())

      val trigger = entry.getJSONObject("trigger")
      val triggerType = trigger.getString("type")
      assertTrue("Unsupported trigger type '$triggerType' for $id", triggerType in allowedTriggerTypes)
      val threshold = trigger.getLong("threshold")
      assertTrue("Threshold must be > 0 for $id", threshold > 0L)

      if (triggerType in targetedTypes) {
        assertFalse("Trigger target required for $id", trigger.optString("target", "").isBlank())
      }
      when (triggerType) {
        "counter_reached" -> {
          allCounterThresholds.add(threshold)
          val prev = counterThresholdByPhase[phase]
          if (prev == null || threshold < prev) {
            counterThresholdByPhase[phase] = threshold
          }
        }
        "excision_completed" -> if (threshold == 1L) hasExcisionEntry = true
        "excision_milestone_reached" -> seenMilestones.add(threshold)
        "dossier_line_unredacted" -> seenDossierLines.add(threshold)
        "case_chapter_completed" -> seenChapters.add(threshold)
        "arc_entered" -> seenArcs.add(trigger.getString("target"))
      }

      val lines = entry.getJSONArray("lines")
      assertTrue("Dialogue $id must have at least one line", lines.length() > 0)
      for (lineIndex in 0 until lines.length()) {
        assertFalse(
          "Dialogue line $lineIndex in $id must not be blank",
          lines.getString(lineIndex).isBlank(),
        )
      }
    }

    for (phase in 0..4) {
      assertTrue(
        "Phase $phase must have a counter_reached entry",
        phase in counterThresholdByPhase,
      )
    }
    for (phase in 1..4) {
      assertEquals(
        "Phase $phase dialogue threshold must align with EscalationCurve",
        EscalationCurve.phaseThresholds[phase],
        counterThresholdByPhase.getValue(phase),
      )
    }
    assertTrue(
      "Must include a Phase 4 counter_reached entry at Excision threshold (${OrganBalance.EXCISION_ICHOR_THRESHOLD})",
      OrganBalance.EXCISION_ICHOR_THRESHOLD in allCounterThresholds,
    )
    assertTrue("Must include a post-reset excision_completed fragment for Cycle 1", hasExcisionEntry)
    for (m in 2L..6L) {
      assertTrue("Expected excision_milestone_reached fragment for M$m", m in seenMilestones)
    }
    for (line in 1L..4L) {
      assertTrue("Expected dossier_line_unredacted fragment for line $line", line in seenDossierLines)
    }
    for (ch in 1L..4L) {
      assertTrue("Expected case_chapter_completed fragment for Chapter $ch", ch in seenChapters)
    }
    assertTrue("Expected arc_entered fragment for arc_2", "arc_2" in seenArcs)
    assertTrue("Expected arc_entered fragment for arc_3", "arc_3" in seenArcs)
  }

  @Test
  fun assetDialogueRepository_parsesAllMultiArcEntriesIncludingTargets() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()

    assertTrue(catalog.schemaVersion >= 3)
    assertEquals(7, catalog.phases.size)
    assertTrue(catalog.entries.size >= 45)
    assertEquals("The Counting Room", catalog.phaseName(0))
    assertEquals("The Endless Hollow", catalog.phaseName(4))
    assertEquals("The Inquiry", catalog.phaseName(5))
    assertEquals("File #00", catalog.phaseName(6))

    assertNotNull("Expected pale_tooth anomaly fragment", catalog.entries.firstOrNull { it.trigger.target == "pale_tooth" })
    assertNotNull("Expected Phase 4 chitin_rib anomaly fragment", catalog.entries.firstOrNull { it.trigger.target == "chitin_rib" })
    assertNotNull("Expected sump_siphon fragment", catalog.entries.firstOrNull { it.trigger.target == "sump_siphon" })
    assertNotNull("Expected choir_lung fragment", catalog.entries.firstOrNull { it.trigger.target == "choir_lung" })
    assertNotNull("Expected scar_node_unlocked fragment", catalog.entries.firstOrNull { it.trigger.type == "scar_node_unlocked" })
    assertNotNull("Expected relic_dredged fragment", catalog.entries.firstOrNull { it.trigger.type == "relic_dredged" })
    assertNotNull("Expected echo_routed fragment", catalog.entries.firstOrNull { it.trigger.type == "echo_routed" })
    assertNotNull("Expected marrow_bloom fragment", catalog.entries.firstOrNull { it.trigger.type == "marrow_bloom" })
    assertNotNull("Expected File #00 chapter 4 fragment", catalog.entries.firstOrNull { it.id == "dlg_arc2_ch4_file_zero" })
    assertNotNull("Expected desk anomaly envelope fragment", catalog.entries.firstOrNull { it.trigger.target == "evidence_envelope" })
    assertNotNull("Expected Arc 3 convergence fragment", catalog.entries.firstOrNull { it.trigger.target == "arc_3" })
    assertNotNull(
      "Expected mid-Phase 4 counter_reached fragment",
      catalog.entries.firstOrNull {
        it.phase == 4 && it.trigger.type == "counter_reached" && it.trigger.threshold == 145_000L
      },
    )
    assertNotNull(
      "Expected Excision-ready Phase 4 fragment",
      catalog.entries.firstOrNull {
        it.trigger.type == "counter_reached" &&
          it.trigger.threshold == OrganBalance.EXCISION_ICHOR_THRESHOLD
      },
    )
    assertNotNull("Expected excision_completed fragment", catalog.entries.firstOrNull { it.trigger.type == "excision_completed" })
    val postPrestigeEntries = catalog.entries.filter { it.trigger.type == "cycle_counter_reached" }
    assertTrue("Expected at least 2 Cycle 1+ prestige story fragments", postPrestigeEntries.size >= 2)
  }

  @Test
  fun phaseOneGraphicAssets_allExistAndLoadAsDrawables() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val expectedDrawables =
      listOf(
        R.drawable.img_wall_phase1,
        R.drawable.img_recessed_valve,
        R.drawable.img_suture_hand,
        R.drawable.img_leech_clerk,
      )

    expectedDrawables.forEach { resId ->
      assertNotNull(
        "Phase 1 drawable resource $resId must load cleanly",
        ContextCompat.getDrawable(context, resId),
      )
    }
  }

  @Test
  fun organStateSerializerAndSharedPreferencesRepository_roundTripMultiArcStateAndRejectCorruptJson() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()
    val repo = SharedPreferencesOrganSaveRepository(context)
    repo.clear()

    var state = OrganGameEngine.initialState(catalog)
    state = OrganDevEngine.setHighestMilestone(state, 5, catalog)
    state = OrganDevEngine.assignArc1Resources(state, ichor = 123_456.0, stillness = 89.0, availableScar = 14L, catalog = catalog)
    state = OrganDevEngine.setAllScarNodes(state, unlockAll = true, catalog = catalog)
    state = OrganDevEngine.assignArc2Resources(state, leads = 4_200.0, silence = 75.0, completedChapters = 2, catalog = catalog)

    repo.save(state)
    val restored = repo.load(catalog)
    assertNotNull(restored)
    requireNotNull(restored)
    assertEquals(5, restored.highestExcisionMilestone)
    assertEquals(123_456L, restored.ichorDisplay)
    assertEquals(89L, restored.stillnessDisplay)
    assertEquals(14L, restored.availableScarTissue)
    assertEquals(ScarNodeId.entries.size, restored.unlockedScarNodes.size)
    assertEquals(4_200L, restored.arc2.leadsDisplay)
    assertEquals(2, restored.arc2.completedChapters)

    // Corrupt JSON fails cleanly to null
    assertNull(OrganStateSerializer.deserialize("{corrupt", catalog))

    repo.clear()
    assertNull(repo.load(catalog))
  }
}
