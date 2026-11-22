package com.example

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.example.data.AssetDialogueRepository
import com.example.data.EscalationCurve
import com.example.game.OrganBalance
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
class DialogueAssetValidationTest {

  private fun loadDialoguesJson(): JSONObject {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val raw = context.assets.open("dialogues.json").bufferedReader().use { it.readText() }
    return JSONObject(raw)
  }

  @Test
  fun dialoguesAsset_existsAndFollowsDesignBibleSchema() {
    val root = loadDialoguesJson()

    assertTrue("schemaVersion must be >= 2", root.getInt("schemaVersion") >= 2)
    assertEquals("GAME_DESIGN_BIBLE.md", root.getString("designReference"))

    val phases = root.getJSONArray("phases")
    assertEquals("Must define escalation phases 0..4", 5, phases.length())
    for (index in 0 until phases.length()) {
      val phase = phases.getJSONObject(index)
      assertEquals(index, phase.getInt("id"))
      assertFalse(phase.getString("name").isBlank())
      assertFalse(phase.getString("intensity").isBlank())
    }
  }

  @Test
  fun dialogueEntries_haveUniqueIdsValidTriggersCoverAllPhasesAndIncludeExcisionFragments() {
    val allowedTriggerTypes =
      setOf(
        "counter_reached",
        "creature_owned",
        "anomaly_interacted",
        "stillness_reached",
        "jar_bottled",
        "excision_completed",
      )
    val targetedTypes = setOf("creature_owned", "anomaly_interacted")

    val root = loadDialoguesJson()
    val entries = root.getJSONArray("entries")
    assertTrue("dialogues.json must contain all rebalanced Direction 1 fragments", entries.length() >= 16)

    val seenIds = mutableSetOf<String>()
    val counterThresholdByPhase = mutableMapOf<Int, Long>()
    val allCounterThresholds = mutableSetOf<Long>()
    var hasExcisionEntry = false

    for (index in 0 until entries.length()) {
      val entry = entries.getJSONObject(index)
      val id = entry.getString("id")
      assertFalse("Dialogue id must not be blank", id.isBlank())
      assertTrue("Duplicate dialogue id: $id", seenIds.add(id))

      val phase = entry.getInt("phase")
      assertTrue("Phase $phase out of range 0..4", phase in 0..4)
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
      if (triggerType == "counter_reached") {
        allCounterThresholds.add(threshold)
        val prev = counterThresholdByPhase[phase]
        if (prev == null || threshold < prev) {
          counterThresholdByPhase[phase] = threshold
        }
      }
      if (triggerType == "excision_completed" && threshold == 1L) {
        hasExcisionEntry = true
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
  }

  @Test
  fun assetDialogueRepository_parsesAllDirectionOneEntriesIncludingTargets() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()

    assertTrue(catalog.schemaVersion >= 2)
    assertEquals(5, catalog.phases.size)
    assertTrue(catalog.entries.size >= 16)
    assertEquals("The Counting Room", catalog.phaseName(0))
    assertEquals("The Endless Hollow", catalog.phaseName(4))

    val toothEntry = catalog.entries.firstOrNull { it.trigger.target == "pale_tooth" }
    assertNotNull("Expected pale_tooth anomaly fragment", toothEntry)
    val excisionReadyEntry =
      catalog.entries.firstOrNull {
        it.trigger.type == "counter_reached" &&
          it.trigger.threshold == OrganBalance.EXCISION_ICHOR_THRESHOLD
      }
    assertNotNull("Expected Excision-ready Phase 4 fragment", excisionReadyEntry)
    val excisionEntry = catalog.entries.firstOrNull { it.trigger.type == "excision_completed" }
    assertNotNull("Expected excision_completed fragment", excisionEntry)
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
}
