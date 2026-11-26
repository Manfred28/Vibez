package com.example

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.example.data.AssetDialogueRepository
import com.example.data.EscalationCurve
import com.example.data.OrganStateSerializer
import com.example.data.SharedPreferencesOrganSaveRepository
import com.example.game.CaseExhibit
import com.example.game.CorroborationId
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
        "scar_node_specific",
        "repeatable_graft_total",
        "relic_dredged",
        "echo_routed",
        "marrow_bloom",
        "dossier_line_unredacted",
        "arc_entered",
        "case_angle_reached",
        "case_chapter_completed",
        "corroboration_discovered",
        "lamp_toggled",
        "arc2_silence_reached",
        "reel_scrubbed",
        "redaction_dissolved",
        "desk_anomaly_interacted",
      )
    val targetedTypes =
      setOf(
        "cycle_counter_reached",
        "creature_owned",
        "anomaly_interacted",
        "scar_node_specific",
        "arc_entered",
        "corroboration_discovered",
        "desk_anomaly_interacted",
      )

    val root = loadDialoguesJson()
    val entries = root.getJSONArray("entries")
    assertTrue("dialogues.json must contain full Arc 1 escape-to-plea + Excision I-VI + Arc 2 Slow Boil + Arc 3 fragments", entries.length() >= 85)

    val seenIds = mutableSetOf<String>()
    val counterThresholdByPhase = mutableMapOf<Int, Long>()
    val allCounterThresholds = mutableSetOf<Long>()
    val seenMilestones = mutableSetOf<Long>()
    val seenDossierLines = mutableSetOf<Long>()
    val seenChapters = mutableSetOf<Long>()
    val seenCaseAngles = mutableSetOf<Long>()
    val seenRedactions = mutableSetOf<Long>()
    val seenCorroborations = mutableSetOf<String>()
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
        val target = trigger.optString("target", "")
        assertFalse("Trigger target required for $id", target.isBlank())
        if (triggerType == "scar_node_specific") {
          assertNotNull("Unknown ScarNodeId '$target' in $id", ScarNodeId.byId(target))
        }
        if (triggerType == "corroboration_discovered") {
          assertNotNull("Unknown CorroborationId '$target' in $id", CorroborationId.byId(target))
        }
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
        "case_angle_reached" -> seenCaseAngles.add(threshold)
        "redaction_dissolved" -> seenRedactions.add(threshold)
        "corroboration_discovered" -> seenCorroborations.add(trigger.getString("target"))
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
    for (ch in 1L..5L) {
      assertTrue("Expected case_chapter_completed fragment for Case Pivot $ch", ch in seenChapters)
    }
    for (angle in 2L..5L) {
      assertTrue("Expected case_angle_reached fragment for Case $angle", angle in seenCaseAngles)
    }
    for (clause in 1L..4L) {
      assertTrue("Expected redaction_dissolved fragment for Clause $clause", clause in seenRedactions)
    }
    assertEquals(
      "All 10 curated Corroborations must have dedicated story fragments",
      CorroborationId.entries.size,
      seenCorroborations.size,
    )
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
    assertNotNull("Expected repeat-farming excision_completed fragment", catalog.entries.firstOrNull { it.id == "dlg_excision_farm_cycles" })
    assertNotNull("Expected mid-tree scar_node_unlocked fragment", catalog.entries.firstOrNull { it.id == "dlg_scar_mid_tree" })
    assertNotNull("Expected deep-tree scar_node_unlocked fragment", catalog.entries.firstOrNull { it.id == "dlg_scar_deep_tree" })
    val postPrestigeEntries = catalog.entries.filter { it.trigger.type == "cycle_counter_reached" }
    assertTrue("Expected at least 2 Cycle 1+ prestige story fragments", postPrestigeEntries.size >= 2)

    // Verify deep multi-step escape-to-plea threads across Relics, Echoes, Blooms, Jars, Scar branches & Repeatables
    val relicThresholds = catalog.entries.filter { it.trigger.type == "relic_dredged" }.map { it.trigger.threshold }.toSet()
    assertTrue("Expected Relic 1, 2, 3 narrative thread", setOf(1L, 2L, 3L).all { it in relicThresholds })
    val echoThresholds = catalog.entries.filter { it.trigger.type == "echo_routed" }.map { it.trigger.threshold }.toSet()
    assertTrue("Expected escalating Echo correspondence (1, 4, 12, 25)", setOf(1L, 4L, 12L, 25L).all { it in echoThresholds })
    val bloomThresholds = catalog.entries.filter { it.trigger.type == "marrow_bloom" }.map { it.trigger.threshold }.toSet()
    assertTrue("Expected escalating Marrow Bloom thread (1, 3, 6)", setOf(1L, 3L, 6L).all { it in bloomThresholds })
    val specificNodes = catalog.entries.filter { it.trigger.type == "scar_node_specific" }.mapNotNull { it.trigger.target }.toSet()
    assertTrue(
      "Expected all 3 Scar Tree branch capstones to have dedicated fragments",
      setOf("perpetual_seam", "catatonic_tide", "bone_resonance").all { it in specificNodes },
    )
    assertTrue(
      "Expected Repeatable Surgical Graft narrative fragments",
      catalog.entries.count { it.trigger.type == "repeatable_graft_total" } >= 2,
    )
    val pleaClimax = catalog.entries.first { it.id == "dlg_excision_m6_breach" }
    val pleaText = pleaClimax.lines.joinToString(" ")
    assertTrue(
      "Excision VI climax must center on Plead for Salvation",
      pleaText.contains("plea for salvation", ignoreCase = true),
    )
    assertTrue(
      "Excision VI climax must hold the surreal thin line between leaving and acceptance/staying",
      pleaText.contains("pulled out", ignoreCase = true) && pleaText.contains("stay", ignoreCase = true),
    )
    val arc2Entry = catalog.entries.first { it.id == "dlg_arc2_entry" }
    assertTrue(
      "Arc 2 entry must open on the received unsigned plea for salvation",
      arc2Entry.lines.joinToString(" ").contains("plea for salvation", ignoreCase = true),
    )
    // Anti-cliché guardrail: prevent regression into literal locked-room thriller / prison-break tropes
    val fullArchiveText = catalog.entries.joinToString("\n") { it.lines.joinToString(" ") }
    listOf("crowbar", "rescue code", "blow the chamber open", "night shift ran late").forEach { forbiddenCliche ->
      assertFalse(
        "Surreal narrative archive must not regress into literal escape-room cliché '$forbiddenCliche'",
        fullArchiveText.contains(forbiddenCliche, ignoreCase = true),
      )
    }
  }

  @Test
  fun arc2SlowBoilQuarantine_preventsGrotesqueRoomSpoilersInCasesOneThroughThree() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AssetDialogueRepository(context).loadCatalog()
    val byId = catalog.entries.associateBy { it.id }

    val bannedEarlyTerms =
      listOf(
        "hydro-ward",
        "molar",
        "grout",
        "valve",
        "ichor",
        "suture",
        "caretaker",
        "tile",
        "vessel",
        "scar",
      )
    val earlyIds =
      listOf(
        "dlg_arc2_entry",
        "dlg_arc2_lamp_first_off",
        "dlg_arc2_silence_first",
        "dlg_arc2_envelope",
        "dlg_arc2_corr_smudged_date",
        "dlg_arc2_case1_dead_end",
        "dlg_arc2_case2_entry",
        "dlg_arc2_spectacles",
        "dlg_arc2_corr_unlisted_annex",
        "dlg_arc2_corr_warm_room_tone",
        "dlg_arc2_corr_phantom_roster",
        "dlg_arc2_case2_dead_end",
        "dlg_arc2_case3_entry",
        "dlg_arc2_reel_tape",
        "dlg_arc2_reel_scrub_first",
        "dlg_arc2_reel_scrub_deep",
        "dlg_arc2_corr_cadence",
        "dlg_arc2_corr_witness_two_places",
        "dlg_arc2_corr_borrowed_coat",
        "dlg_arc2_case3_dead_end",
      )

    earlyIds.forEach { id ->
      val entry = byId[id]
      assertNotNull("Expected early Arc 2 dialogue entry $id", entry)
      val body = "${entry!!.speaker} ${entry.lines.joinToString(" ")}".lowercase()
      bannedEarlyTerms.forEach { banned ->
        assertFalse(
          "Early Arc 2 entry $id leaked quarantined Room vocabulary '$banned'",
          body.contains(banned),
        )
      }
    }
    assertNotNull("Expected Case IV+ enamel_exhibit fragment", byId["dlg_arc2_enamel_exhibit"])
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
    state = OrganDevEngine.assignArc2Resources(state, leads = 4_200.0, silence = 75.0, caseAngle = 3, completedChapters = 2, catalog = catalog)
    state = OrganDevEngine.toggleArc2Lamp(state, catalog)

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
    assertEquals(3, restored.arc2.caseAngle)
    assertEquals(2, restored.arc2.completedCasePivots)
    assertFalse(restored.arc2.lampOn)
    assertTrue(CaseExhibit.TAX_PLAT_1998 in restored.arc2.pinnedExhibits)

    // Corrupt JSON fails cleanly to null
    assertNull(OrganStateSerializer.deserialize("{corrupt", catalog))

    repo.clear()
    assertNull(repo.load(catalog))
  }
}
