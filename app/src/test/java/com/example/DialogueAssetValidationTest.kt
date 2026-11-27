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
import com.example.game.DeskAnomalyType
import com.example.game.DredgedRelic
import com.example.game.InvestigatorContact
import com.example.game.OrganBalance
import com.example.game.OrganDevEngine
import com.example.game.OrganGameEngine
import com.example.game.ScarNodeId
import com.example.ui.components.caseExhibitDrawableRes
import com.example.ui.components.computeDeskAnomalyBreathScale
import com.example.ui.components.computeDeskLayerAlphas
import com.example.ui.components.contactDrawableRes
import com.example.ui.components.deskAnomalyDrawableRes
import com.example.ui.components.deskStageDrawableRes
import com.example.ui.components.dredgedRelicDrawableRes
import com.example.ui.components.graftDrawableRes
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
    assertEquals("Must define escalation phases 0..7 (Arc 1 + Arc 2 + Arc 3 Breach)", 8, phases.length())
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
        "arc3_wrench_count",
        "arc3_jars_shattered",
        "arc3_unmake_count",
        "arc3_scars_unstitched",
        "arc3_rupture_interacted",
        "arc3_wedge_locked",
        "arc3_stratum_reached",
        "arc3_final_pull",
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
      assertTrue("Phase $phase out of range 0..7", phase in 0..7)
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
    assertEquals(8, catalog.phases.size)
    assertTrue(catalog.entries.size >= 105)
    assertEquals("The Counting Room", catalog.phaseName(0))
    assertEquals("The Endless Hollow", catalog.phaseName(4))
    assertEquals("The Inquiry", catalog.phaseName(5))
    assertEquals("File #00", catalog.phaseName(6))
    assertEquals("The Breach", catalog.phaseName(7))
    assertTrue(
      "Expected >= 20 Phase 7 Arc 3 Breach dialogue entries",
      catalog.entries.count { it.phase == 7 } >= 20,
    )

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
  fun arc2BespokePhotographicAssets_allNineteenLoadCleanlyAndSlowBoilAlphasEnforceQuarantine() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // 1. All 5 progressive nocturnal desk stages load cleanly
    for (stage in 1..5) {
      val resId = deskStageDrawableRes(stage)
      assertNotNull(
        "Arc 2 desk stage $stage drawable must load cleanly",
        ContextCompat.getDrawable(context, resId),
      )
    }

    // 2. All 4 circular macro desk anomaly drawables load cleanly
    for (type in DeskAnomalyType.entries) {
      assertNotNull(
        "Arc 2 desk anomaly ${type.id} drawable must load cleanly",
        ContextCompat.getDrawable(context, deskAnomalyDrawableRes(type)),
      )
    }

    // 3. All 10 Corkboard / Dossier Exhibit Polaroid drawables load cleanly
    assertEquals(10, CaseExhibit.entries.size)
    for (exhibit in CaseExhibit.entries) {
      assertNotNull(
        "Arc 2 exhibit ${exhibit.id} drawable must load cleanly",
        ContextCompat.getDrawable(context, caseExhibitDrawableRes(exhibit)),
      )
    }

    // 4. Slow-Boil Quarantine: Cases I–III strictly keep Case IV & Case V at 0f regardless of Lamp or Haunt
    for (caseAngle in 1..3) {
      for (lampOn in listOf(true, false)) {
        val alphas =
          computeDeskLayerAlphas(
            caseAngle = caseAngle,
            hauntDepth = 0.95f,
            lampOn = lampOn,
            fileZeroRevealed = false,
          )
        assertEquals("Case $caseAngle (lampOn=$lampOn) must quarantine Case IV backdrop", 0f, alphas.case4, 0.0001f)
        assertEquals("Case $caseAngle (lampOn=$lampOn) must quarantine Case V backdrop", 0f, alphas.case5, 0.0001f)
      }
    }

    // Case IV Lamp On shows Case IV only (no Case V); Lamp Off at high haunt subtly reveals Case V
    val c4On = computeDeskLayerAlphas(4, 0.90f, lampOn = true, fileZeroRevealed = false)
    assertEquals(0.78f, c4On.case4, 0.0001f)
    assertEquals(0f, c4On.case5, 0.0001f)

    val c4OffLow = computeDeskLayerAlphas(4, 0.50f, lampOn = false, fileZeroRevealed = false)
    assertEquals(0.92f, c4OffLow.case4, 0.0001f)
    assertEquals(0f, c4OffLow.case5, 0.0001f)

    val c4OffHigh = computeDeskLayerAlphas(4, 0.80f, lampOn = false, fileZeroRevealed = false)
    assertEquals(0.92f, c4OffHigh.case4, 0.0001f)
    assertEquals(0.24f, c4OffHigh.case5, 0.0001f)

    // Case V & File #00 escalate Case V backdrop while clearing early precinct stages
    val c5On = computeDeskLayerAlphas(5, 0.90f, lampOn = true, fileZeroRevealed = false)
    assertEquals(0f, c5On.case1, 0.0001f)
    assertEquals(0.82f, c5On.case5, 0.0001f)

    val fileZero = computeDeskLayerAlphas(5, 1.0f, lampOn = false, fileZeroRevealed = true)
    assertEquals(0f, fileZero.case1, 0.0001f)
    assertEquals(0.14f, fileZero.case4, 0.0001f)
    assertEquals(0.94f, fileZero.case5, 0.0001f)

    // 5. Desk anomaly haunted breathing stays within subtle organic bounds across all 4 types
    for (type in DeskAnomalyType.entries) {
      for (phaseStep in 0..10) {
        val (sx, sy) = computeDeskAnomalyBreathScale(42L, type, phaseStep / 10f)
        assertTrue("scaleX=$sx out of organic bounds for ${type.id}", sx in 0.97f..1.06f)
        assertTrue("scaleY=$sy out of organic bounds for ${type.id}", sy in 0.97f..1.06f)
      }
    }
  }

  @Test
  fun bespokeMacroPortraitsAndCenterpieceAssets_allThirteenLoadCleanly() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    val directDrawables =
      listOf(
        R.drawable.img_weeping_lamp,
        R.drawable.img_sump_siphon,
        R.drawable.img_graft_tendril,
        R.drawable.img_graft_choir_lung,
        R.drawable.img_relic_patient_wristband,
        R.drawable.img_relic_brass_ward_key,
        R.drawable.img_relic_caretaker_logbook,
        R.drawable.img_contact_night_archivist,
        R.drawable.img_contact_bribed_orderly,
        R.drawable.img_contact_microfilm_scanner,
        R.drawable.img_contact_audio_transcriber,
        R.drawable.img_arc2_case_stamp,
        R.drawable.img_arc3_convergence,
      )
    assertEquals(13, directDrawables.size)
    directDrawables.forEach { resId ->
      assertNotNull(
        "Photographic drawable resource $resId must load cleanly",
        ContextCompat.getDrawable(context, resId),
      )
    }

    // Verify all typed drawable mappers resolve cleanly
    assertEquals(R.drawable.img_graft_tendril, graftDrawableRes(isLung = false))
    assertEquals(R.drawable.img_graft_choir_lung, graftDrawableRes(isLung = true))
    assertEquals(3, DredgedRelic.entries.size)
    DredgedRelic.entries.forEach { relic ->
      assertNotNull(
        "Dredged relic ${relic.id} drawable must load cleanly",
        ContextCompat.getDrawable(context, dredgedRelicDrawableRes(relic)),
      )
    }
    assertEquals(4, InvestigatorContact.entries.size)
    InvestigatorContact.entries.forEach { contact ->
      assertNotNull(
        "Investigator contact ${contact.id} drawable must load cleanly",
        ContextCompat.getDrawable(context, contactDrawableRes(contact)),
      )
    }

    // Verify all 5 Arc 3 Breach & Abstract Crimson Flood photographic drawables load cleanly
    listOf(
      R.drawable.img_wall_arc3_breach_early,
      R.drawable.img_wall_arc3_breach_deep,
      R.drawable.img_arc3_crimson_flood,
      R.drawable.img_rupture_clotting_seam,
      R.drawable.img_rupture_arterial_well,
    ).forEach { resId ->
      assertNotNull(
        "Arc 3 photographic drawable resource $resId must load cleanly",
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
