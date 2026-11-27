package com.example

import com.example.audio.AmbientSoundProfile
import com.example.audio.Arc1FoleyCueBank
import com.example.audio.Arc2FoleyCueBank
import com.example.audio.Arc3FoleyCueBank
import com.example.audio.InMemoryAudioEngine
import com.example.audio.ProceduralFoleySynth
import com.example.audio.SoundActionHint
import com.example.audio.SoundCue
import com.example.audio.SoundCueResolver
import com.example.data.DialogueCatalog
import com.example.data.DialogueEntry
import com.example.data.DialogueTrigger
import com.example.game.ActiveBreachRupture
import com.example.game.ActiveDeskAnomaly
import com.example.game.ActiveWallAnomaly
import com.example.game.Arc2InvestigatorEngine
import com.example.game.Arc3BreachEngine
import com.example.game.Arc3BreachState
import com.example.game.Arc3Stratum
import com.example.game.Arc3UnmakeAct
import com.example.game.AuditRedaction
import com.example.game.BreachRuptureType
import com.example.game.CaseExhibit
import com.example.game.CorroborationId
import com.example.game.CreatureType
import com.example.game.CultDossierLine
import com.example.game.DeskAnomalyType
import com.example.game.GameArc
import com.example.game.InvestigatorContact
import com.example.game.OrganBalance
import com.example.game.OrganGameEngine
import com.example.game.OrganGameState
import com.example.game.RepeatableScarGraft
import com.example.game.ScarNodeId
import com.example.game.SpecimenJar
import com.example.game.WallAnomalyType
import com.example.ui.OrganViewModel
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrganAudioTest {

  private class RecordingAudioEngine(initialMuted: Boolean = false) :
    InMemoryAudioEngine(initialMuted) {
    val playedCues = mutableListOf<Pair<SoundCue, Int>>()
    val ambientProfiles = mutableListOf<AmbientSoundProfile>()
    var foregroundStates = mutableListOf<Boolean>()
    var releasedCount = 0

    override fun playCues(cues: List<SoundCue>, phase: Int) {
      cues.forEach { playedCues.add(it to phase) }
    }

    override fun updateAmbience(profile: AmbientSoundProfile) {
      ambientProfiles.add(profile)
    }

    override fun setForegroundActive(active: Boolean) {
      foregroundStates.add(active)
    }

    override fun release() {
      releasedCount += 1
    }
  }

  private val sampleCatalog =
    DialogueCatalog(
      schemaVersion = 3,
      phases = emptyList(),
      entries =
        listOf(
          DialogueEntry(
            id = "dlg_test_first",
            phase = 0,
            speaker = "GROUT",
            mood = "clinical",
            trigger = DialogueTrigger(type = "counter_reached", threshold = 1L),
            lines = listOf("One press."),
            once = true,
          )
        ),
    )

  @Test
  fun resolveTransitionCues_emitsOnlyForGenuineStateChangesAndNeverOnFailedActions() {
    val start = OrganGameEngine.initialState(sampleCatalog)

    // Unaffordable creature buy returns identical state -> emits zero cues
    val failedBuy = OrganGameEngine.buyCreature(start, CreatureType.SUTURE_HAND, sampleCatalog)
    assertTrue(
      SoundCueResolver.resolveTransitionCues(start, failedBuy, SoundActionHint.CREATURE_BUY)
        .isEmpty()
    )

    // Genuine valve press emits VALVE_PRESS + STORY_DRIP when first story unlocks
    val afterPress = OrganGameEngine.pressValve(start, sampleCatalog)
    val pressCues =
      SoundCueResolver.resolveTransitionCues(start, afterPress, SoundActionHint.VALVE_PRESS)
    assertEquals(listOf(SoundCue.VALVE_PRESS, SoundCue.STORY_DRIP), pressCues)

    // Dev actions never emit noisy SFX even when state mutates dramatically
    val devMutated = start.copy(ichor = 999_999.0, cycleLifetimeIchor = 999_999.0, phase = 4)
    assertTrue(
      SoundCueResolver.resolveTransitionCues(start, devMutated, SoundActionHint.DEV).isEmpty()
    )
  }

  @Test
  fun resolveTransitionCues_coversAllArc1ManualRiteAnomalyAndPassiveTickEvents() {
    val base =
      OrganGameEngine.initialState(DialogueCatalog.Empty).copy(
        ichor = 50_000.0,
        cycleLifetimeIchor = 50_000.0,
        stillness = 400.0,
        phase = 3,
        highestExcisionMilestone = 5,
        availableScarTissue = 20L,
        scarTissue = 20L,
        creatures =
          mapOf(
            CreatureType.SUTURE_HAND to 4,
            CreatureType.LEECH_CLERK to 4,
            CreatureType.WEEPING_LAMP to 2,
            CreatureType.SUMP_SIPHON to 2,
          ),
      )

    // All 4 purchasable organisms map to distinct signature foley cues
    val creatureExpectations =
      listOf(
        CreatureType.SUTURE_HAND to SoundCue.CREATURE_BUY_SUTURE_HAND,
        CreatureType.LEECH_CLERK to SoundCue.CREATURE_BUY_LEECH_CLERK,
        CreatureType.WEEPING_LAMP to SoundCue.CREATURE_BUY_WEEPING_LAMP,
        CreatureType.SUMP_SIPHON to SoundCue.CREATURE_BUY_SUMP_SIPHON,
      )
    for ((creature, expectedCue) in creatureExpectations) {
      val bought = OrganGameEngine.buyCreature(base, creature, DialogueCatalog.Empty)
      assertEquals(
        listOf(expectedCue),
        SoundCueResolver.resolveTransitionCues(base, bought, SoundActionHint.CREATURE_BUY),
      )
    }

    // Jar bottle
    val jarred = OrganGameEngine.bottleJar(base, SpecimenJar.YESTERDAYS_DUST, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.JAR_BOTTLE),
      SoundCueResolver.resolveTransitionCues(base, jarred, SoundActionHint.JAR_BOTTLE),
    )

    // Graft tendril & Choir-Lung emit distinct foley cues
    val grafted = OrganGameEngine.performGraft(base, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.GRAFT_PERFORM),
      SoundCueResolver.resolveTransitionCues(base, grafted, SoundActionHint.GRAFT_PERFORM),
    )
    val lungBase = base.copy(graftedTendrils = 1)
    val lungGrafted = OrganGameEngine.performChoirLungGraft(lungBase, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.GRAFT_CHOIR_LUNG),
      SoundCueResolver.resolveTransitionCues(lungBase, lungGrafted, SoundActionHint.GRAFT_PERFORM),
    )

    // All 4 wall anomalies map to distinct foley cues
    val anomalyExpectations =
      listOf(
        WallAnomalyType.PALE_TOOTH to SoundCue.ANOMALY_TOOTH,
        WallAnomalyType.GLASS_EYE to SoundCue.ANOMALY_EYE,
        WallAnomalyType.SWOLLEN_NODULE to SoundCue.ANOMALY_NODULE,
        WallAnomalyType.CHITIN_RIB to SoundCue.ANOMALY_RIB,
      )
    for ((type, expectedCue) in anomalyExpectations) {
      val withAnomaly = base.copy(activeAnomalies = listOf(ActiveWallAnomaly(9L, type, 0.5f, 0.5f)))
      val interacted = OrganGameEngine.interactAnomaly(withAnomaly, 9L, DialogueCatalog.Empty)
      assertEquals(
        listOf(expectedCue),
        SoundCueResolver.resolveTransitionCues(
          withAnomaly,
          interacted,
          SoundActionHint.ANOMALY_INTERACT,
        ),
      )
    }

    // Scar tree node + repeatable graft
    val scarred = OrganGameEngine.unlockScarNode(base, ScarNodeId.FIRST_INCISION, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.SCAR_UNLOCK),
      SoundCueResolver.resolveTransitionCues(base, scarred, SoundActionHint.SCAR_UNLOCK),
    )
    val repeatable =
      OrganGameEngine.buyRepeatableScarGraft(
        scarred,
        RepeatableScarGraft.KELOID_THICKENING,
        DialogueCatalog.Empty,
      )
    assertEquals(
      listOf(SoundCue.SCAR_UNLOCK),
      SoundCueResolver.resolveTransitionCues(scarred, repeatable, SoundActionHint.SCAR_UNLOCK),
    )

    // Drain toggle, Sump dredge, Echo diversion, Valve clamp, Throat offer
    val sealed = OrganGameEngine.toggleDrainSeal(base, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.DRAIN_TOGGLE),
      SoundCueResolver.resolveTransitionCues(base, sealed, SoundActionHint.DRAIN_TOGGLE),
    )
    val readySump = base.copy(drainPressure = 100.0)
    val dredged = OrganGameEngine.dredgeSump(readySump, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.SUMP_DREDGE),
      SoundCueResolver.resolveTransitionCues(readySump, dredged, SoundActionHint.SUMP_DREDGE),
    )
    val diverted = OrganGameEngine.setEchoDiversion(base, 50, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.DRAIN_TOGGLE),
      SoundCueResolver.resolveTransitionCues(base, diverted, SoundActionHint.ECHO_DIVERSION),
    )
    val clamped = OrganGameEngine.activateValveClamp(base, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.CLAMP_ENGAGE),
      SoundCueResolver.resolveTransitionCues(base, clamped, SoundActionHint.CLAMP_ENGAGE),
    )
    val throatReady = base.copy(graftedTendrils = 3, ownedJars = setOf(SpecimenJar.YESTERDAYS_DUST))
    val offered =
      OrganGameEngine.offerToThroat(
        throatReady,
        CultDossierLine.entries.first(),
        DialogueCatalog.Empty,
      )
    assertEquals(
      listOf(SoundCue.THROAT_OFFER),
      SoundCueResolver.resolveTransitionCues(throatReady, offered, SoundActionHint.THROAT_OFFER),
    )

    // Passive ticks: Echo pulse + Marrow Bloom + Phase shift
    val tickPrep =
      base.copy(
        phase = 1,
        ichor = 1_599.0,
        cycleLifetimeIchor = 1_599.0,
        echoDiversionPercent = 50,
        echoChargeSeconds = OrganBalance.ECHO_PULSE_INTERVAL_SEC - 0.1,
        clampRemainingSeconds = 0.1,
      )
    val afterTick = OrganGameEngine.tick(tickPrep, 0.5, DialogueCatalog.Empty)
    val tickCues = SoundCueResolver.resolveTransitionCues(tickPrep, afterTick, SoundActionHint.TICK)
    assertTrue(SoundCue.ECHO_PULSE in tickCues)
    assertTrue(SoundCue.MARROW_BLOOM in tickCues)
    assertTrue(SoundCue.PHASE_SHIFT in tickCues)

    // Excision cut & Plead for Salvation breach
    val excisionReady =
      base.copy(ichor = 250_000.0, cycleLifetimeIchor = 250_000.0, phase = 4)
    val excised = OrganGameEngine.performExcision(excisionReady, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.EXCISION_CUT),
      SoundCueResolver.resolveTransitionCues(excisionReady, excised, SoundActionHint.EXCISION),
    )

    val breachReady =
      base.copy(
        highestExcisionMilestone = 5,
        ichor = 160_000_000_000.0,
        cycleLifetimeIchor = 160_000_000_000.0,
        phase = 4,
      )
    val breached = OrganGameEngine.performBreachToArc2(breachReady, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.PLEAD_SALVATION),
      SoundCueResolver.resolveTransitionCues(
        breachReady,
        breached,
        SoundActionHint.BREACH_TO_ARC2,
      ),
    )
  }

  @Test
  fun resolveTransitionCues_coversArc2InvestigatorAndArc3ConvergenceEvents() {
    val arc2Base =
      OrganGameEngine.initialState(DialogueCatalog.Empty).let { initial ->
        initial.copy(
          activeArc = GameArc.ARC_2_INVESTIGATION,
          arc2 =
            initial.arc2.copy(
              leads = 90_000_000.0,
              lifetimeLeads = 90_000_000.0,
              silence = 20_000.0,
              lifetimeSilence = 20_000.0,
              caseAngle = 1,
              lampOn = true,
              activeDeskAnomalies =
                listOf(ActiveDeskAnomaly(5L, DeskAnomalyType.EVIDENCE_ENVELOPE, 0.5f, 0.5f, 14.0)),
            ),
        )
      }

    val stamped = Arc2InvestigatorEngine.inspectCaseFile(arc2Base, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.ARC2_STAMP),
      SoundCueResolver.resolveTransitionCues(arc2Base, stamped, SoundActionHint.ARC2_STAMP),
    )

    // Lamp OFF and Lamp ON emit distinct brass chain foley cues
    val lampOff = Arc2InvestigatorEngine.toggleLamp(arc2Base, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.ARC2_LAMP_OFF),
      SoundCueResolver.resolveTransitionCues(arc2Base, lampOff, SoundActionHint.ARC2_LAMP_TOGGLE),
    )
    val lampBackOn = Arc2InvestigatorEngine.toggleLamp(lampOff, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.ARC2_LAMP_ON),
      SoundCueResolver.resolveTransitionCues(lampOff, lampBackOn, SoundActionHint.ARC2_LAMP_TOGGLE),
    )

    // All 4 Arc 2 contacts resolve to distinct signature cues
    val case4Rich =
      arc2Base.copy(
        arc2 = arc2Base.arc2.copy(caseAngle = 4, completedCasePivots = 3),
      )
    val contactExpectations =
      listOf(
        InvestigatorContact.NIGHT_ARCHIVIST to SoundCue.ARC2_CONTACT_ARCHIVIST,
        InvestigatorContact.BRIBED_ORDERLY to SoundCue.ARC2_CONTACT_ORDERLY,
        InvestigatorContact.MICROFILM_SCANNER to SoundCue.ARC2_CONTACT_SCANNER,
        InvestigatorContact.AUDIO_TRANSCRIBER to SoundCue.ARC2_CONTACT_TRANSCRIBER,
      )
    for ((contact, expectedCue) in contactExpectations) {
      val hired = Arc2InvestigatorEngine.recruitContact(case4Rich, contact, DialogueCatalog.Empty)
      assertEquals(
        listOf(expectedCue),
        SoundCueResolver.resolveTransitionCues(case4Rich, hired, SoundActionHint.ARC2_CONTACT),
      )
    }

    val unlockedExhibit =
      Arc2InvestigatorEngine.unlockExhibit(
        Arc2InvestigatorEngine.unlockExhibit(
          arc2Base,
          CaseExhibit.UNLISTED_PAY_STUB,
          DialogueCatalog.Empty,
        ),
        CaseExhibit.TAX_PLAT_1998,
        DialogueCatalog.Empty,
      )
    assertEquals(
      listOf(SoundCue.ARC2_DEDUCTION),
      SoundCueResolver.resolveTransitionCues(
        arc2Base,
        Arc2InvestigatorEngine.unlockExhibit(
          arc2Base,
          CaseExhibit.UNLISTED_PAY_STUB,
          DialogueCatalog.Empty,
        ),
        SoundActionHint.ARC2_DEDUCTION,
      ),
    )

    // Curated corroboration pair -> ARC2_CORROBORATION_LOCK; removing string -> ARC2_STRING_UNWIRE; non-curated pair -> ARC2_STRING_WIRE
    val wired =
      Arc2InvestigatorEngine.toggleExhibitPair(
        unlockedExhibit,
        CaseExhibit.WATER_STAINED_PETITION,
        CaseExhibit.UNLISTED_PAY_STUB,
        DialogueCatalog.Empty,
      )
    assertEquals(
      listOf(SoundCue.ARC2_CORROBORATION_LOCK),
      SoundCueResolver.resolveTransitionCues(unlockedExhibit, wired, SoundActionHint.ARC2_STRING_WIRE),
    )
    val unwired =
      Arc2InvestigatorEngine.toggleExhibitPair(
        wired,
        CaseExhibit.WATER_STAINED_PETITION,
        CaseExhibit.UNLISTED_PAY_STUB,
        DialogueCatalog.Empty,
      )
    assertEquals(
      listOf(SoundCue.ARC2_STRING_UNWIRE),
      SoundCueResolver.resolveTransitionCues(wired, unwired, SoundActionHint.ARC2_STRING_WIRE),
    )
    val unwiredWithPlat =
      unwired.copy(
        arc2 = unwired.arc2.copy(pinnedExhibits = unwired.arc2.pinnedExhibits + CaseExhibit.TAX_PLAT_1998),
      )
    val speculativeWire =
      Arc2InvestigatorEngine.toggleExhibitPair(
        unwiredWithPlat,
        CaseExhibit.WATER_STAINED_PETITION,
        CaseExhibit.TAX_PLAT_1998,
        DialogueCatalog.Empty,
      )
    assertEquals(
      listOf(SoundCue.ARC2_STRING_WIRE),
      SoundCueResolver.resolveTransitionCues(
        unwiredWithPlat,
        speculativeWire,
        SoundActionHint.ARC2_STRING_WIRE,
      ),
    )

    val case4Dark =
      wired.copy(
        arc2 = wired.arc2.copy(caseAngle = 4, completedCasePivots = 3, lampOn = false),
      )
    val scrubbed = Arc2InvestigatorEngine.triggerReelScrub(case4Dark, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.ARC2_REEL_SCRUB),
      SoundCueResolver.resolveTransitionCues(case4Dark, scrubbed, SoundActionHint.ARC2_REEL_SCRUB),
    )

    val dissolved =
      Arc2InvestigatorEngine.dissolveRedaction(
        scrubbed,
        AuditRedaction.CLAUSE_SEEPAGE,
        DialogueCatalog.Empty,
      )
    assertEquals(
      listOf(SoundCue.ARC2_REDACTION_DISSOLVE),
      SoundCueResolver.resolveTransitionCues(
        scrubbed,
        dissolved,
        SoundActionHint.ARC2_REDACTION_DISSOLVE,
      ),
    )

    // All 4 Desk Anomaly types map to distinct tactile foley cues
    val deskAnomalyExpectations =
      listOf(
        DeskAnomalyType.EVIDENCE_ENVELOPE to SoundCue.ARC2_ANOMALY_ENVELOPE,
        DeskAnomalyType.FOGGED_SPECTACLES to SoundCue.ARC2_ANOMALY_SPECTACLES,
        DeskAnomalyType.REEL_TAPE to SoundCue.ARC2_ANOMALY_REEL_TAPE,
        DeskAnomalyType.ENAMEL_EXHIBIT to SoundCue.ARC2_ANOMALY_ENAMEL,
      )
    for ((type, expectedCue) in deskAnomalyExpectations) {
      val withAnomaly =
        arc2Base.copy(
          arc2 =
            arc2Base.arc2.copy(
              activeDeskAnomalies = listOf(ActiveDeskAnomaly(5L, type, 0.5f, 0.5f, 14.0))
            )
        )
      val collected =
        Arc2InvestigatorEngine.interactDeskAnomaly(withAnomaly, 5L, DialogueCatalog.Empty)
      assertEquals(
        listOf(expectedCue),
        SoundCueResolver.resolveTransitionCues(
          withAnomaly,
          collected,
          SoundActionHint.ARC2_DESK_ANOMALY,
        ),
      )
    }

    // Case I pivot -> ARC2_CHAPTER; Case V pivot -> ARC2_FILE_ZERO
    val pivoted = Arc2InvestigatorEngine.pivotNextCase(wired, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.ARC2_CHAPTER),
      SoundCueResolver.resolveTransitionCues(wired, pivoted, SoundActionHint.ARC2_CHAPTER),
    )
    val case5Ready =
      arc2Base.copy(
        arc2 =
          arc2Base.arc2.copy(
            caseAngle = 5,
            completedCasePivots = 4,
            discoveredCorroborations = CorroborationId.entries.take(5).toSet(),
            reelScrubCount = 1,
            dissolvedRedactions = AuditRedaction.entries.toSet(),
          ),
      )
    val fileZero = Arc2InvestigatorEngine.pivotNextCase(case5Ready, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.ARC2_FILE_ZERO),
      SoundCueResolver.resolveTransitionCues(case5Ready, fileZero, SoundActionHint.ARC2_CHAPTER),
    )

    // Arc 3 convergence
    val arc3 = Arc2InvestigatorEngine.enterArc3Convergence(fileZero, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.ARC3_ENTER),
      SoundCueResolver.resolveTransitionCues(fileZero, arc3, SoundActionHint.ARC3_ENTER),
    )
  }

  @Test
  fun computeAmbientProfile_escalatesAcrossPhasesStillnessClampArc2SlowBoilAndArc3() {
    val p0 = SoundCueResolver.computeAmbientProfile(OrganGameState(phase = 0))
    val p4 = SoundCueResolver.computeAmbientProfile(OrganGameState(phase = 4))
    val p2Still =
      SoundCueResolver.computeAmbientProfile(OrganGameState(phase = 2, isStillnessActive = true))
    val p2Active =
      SoundCueResolver.computeAmbientProfile(OrganGameState(phase = 2, isStillnessActive = false))
    val clamped =
      SoundCueResolver.computeAmbientProfile(OrganGameState(phase = 4, clampRemainingSeconds = 8.0))
    // Slow-Boil Quarantine: Cases I–III have ZERO fleshDepth and ZERO cardiacPulse
    val arc2Early =
      SoundCueResolver.computeAmbientProfile(
        OrganGameState(
          activeArc = GameArc.ARC_2_INVESTIGATION,
          arc2 = OrganGameState().arc2.copy(caseAngle = 2, lampOn = false),
        )
      )
    val arc2Late =
      SoundCueResolver.computeAmbientProfile(
        OrganGameState(
          activeArc = GameArc.ARC_2_INVESTIGATION,
          arc2 =
            OrganGameState().arc2.copy(
              caseAngle = 5,
              completedCasePivots = 4,
              lampOn = false,
              leads = 50_000_000.0,
              dissolvedRedactions = AuditRedaction.entries.toSet(),
            ),
        )
      )
    val arc3 =
      SoundCueResolver.computeAmbientProfile(OrganGameState(activeArc = GameArc.ARC_3_CONVERGENCE))

    assertTrue(p4.humGain > p0.humGain)
    assertTrue(p4.fleshDepth > p0.fleshDepth)
    assertEquals(0f, p0.cardiacPulse, 0.0001f)
    assertTrue(p4.cardiacPulse > 0.5f)
    assertTrue(p2Still.dripRate > p2Active.dripRate)
    assertTrue(p2Still.humGain < p2Active.humGain)
    assertTrue(clamped.clampTension > 0.8f)
    assertTrue(arc2Early.rainMix > 0.5f)
    assertEquals(0f, arc2Early.fleshDepth, 0.0001f)
    assertEquals(0f, arc2Early.cardiacPulse, 0.0001f)
    assertTrue(arc2Late.rainMix > 0.7f)
    assertTrue(arc2Late.fleshDepth > 0f)
    assertTrue(arc2Late.cardiacPulse > 0f)
    assertTrue(arc2Late.complicityBleed >= 0.8f)
    assertEquals(7, arc3.phase)
    assertEquals(0f, arc3.rainMix, 0.0001f)
    assertTrue(arc3.fleshDepth >= 0.45f && arc3.cardiacPulse >= 0.25f)

    val arc3Clotting =
      SoundCueResolver.computeAmbientProfile(
        OrganGameState(
          activeArc = GameArc.ARC_3_CONVERGENCE,
          arc3 = Arc3BreachState(fracture = 50.0, secondsSinceLastAction = 3.5),
        )
      )
    assertTrue("Actively clotting Arc 3 breach must raise clampTension", arc3Clotting.clampTension > 0.5f)
  }

  @Test
  fun proceduralFoleySynth_synthesizesNonSilentBoundedPcmForEveryCueAndAmbientBed() {
    assertEquals(52, SoundCue.entries.size)
    // Grafting should be a long, fleshy ~1.15s ritual (significantly longer than quick valve taps)
    val graftPcm = ProceduralFoleySynth.synthesizeCue(SoundCue.GRAFT_PERFORM, phase = 3)
    val lungPcm = ProceduralFoleySynth.synthesizeCue(SoundCue.GRAFT_CHOIR_LUNG, phase = 3)
    val valvePcm = ProceduralFoleySynth.synthesizeCue(SoundCue.VALVE_PRESS, phase = 0)
    assertTrue(
      "Expected long fleshy graft waveform (>= 0.85s), got ${graftPcm.size} samples",
      graftPcm.size >= (ProceduralFoleySynth.SAMPLE_RATE * 0.85).toInt(),
    )
    assertTrue(
      "Expected sustained Choir-Lung waveform (>= 1.05s), got ${lungPcm.size} samples",
      lungPcm.size >= (ProceduralFoleySynth.SAMPLE_RATE * 1.05).toInt(),
    )
    assertTrue("Graft waveform should be much longer than valve tap", graftPcm.size > valvePcm.size * 3)

    for (cue in SoundCue.entries) {
      val pcm = ProceduralFoleySynth.synthesizeCue(cue, phase = 4, pitchFactor = 1.0f)
      assertTrue("Cue ${cue.id} produced empty PCM", pcm.size > 400)
      var peak = 0
      for (sample in pcm) {
        val a = abs(sample.toInt())
        if (a > peak) peak = a
      }
      assertTrue("Cue ${cue.id} was silent (peak=$peak)", peak > 400)
      assertTrue("Cue ${cue.id} exceeded safe Short ceiling", peak <= Short.MAX_VALUE.toInt())
    }

    val gen = ProceduralFoleySynth.AmbientBedGenerator()
    val buf = ShortArray(2205)
    for (profile in
      listOf(
        SoundCueResolver.computeAmbientProfile(OrganGameState(phase = 0)),
        SoundCueResolver.computeAmbientProfile(OrganGameState(phase = 4)),
        SoundCueResolver.computeAmbientProfile(
          OrganGameState(activeArc = GameArc.ARC_2_INVESTIGATION)
        ),
      )) {
      gen.fillChunk(buf, profile)
      var energy = 0L
      for (s in buf) energy += abs(s.toLong())
      assertTrue("Ambient profile ${profile.activeArc}/P${profile.phase} was silent", energy > 5_000L)
    }
  }

  @Test
  fun organViewModel_dispatchesAudioCuesTogglesMuteAndPreservesMuteAcrossDevReset() {
    val engine = RecordingAudioEngine(initialMuted = false)
    val vm = OrganViewModel(autoTick = false, audioEngine = engine)

    assertFalse(vm.isAudioMuted.value)
    assertTrue(engine.ambientProfiles.isNotEmpty())

    vm.pressValve()
    assertEquals(listOf(SoundCue.VALVE_PRESS to 0), engine.playedCues)

    vm.playRelicInspectCue()
    assertEquals(SoundCue.RELIC_INSPECT to 0, engine.playedCues.last())

    vm.toggleAudioMuted()
    assertTrue(vm.isAudioMuted.value)

    // Save reset updates ambient profile back to Phase 0 without overriding user mute choice
    vm.devResetAll()
    assertTrue(vm.isAudioMuted.value)
    assertEquals(0, engine.ambientProfiles.last().phase)

    vm.setAudioForegroundActive(false)
    assertEquals(listOf(false), engine.foregroundStates)
  }

  @Test
  fun proceduralFoleySynth_producesCastIronOvertonesForValveAndSustainedOrganicEnergyForGraftAndTears() {
    val sr = ProceduralFoleySynth.SAMPLE_RATE

    // 1. Recessed wall valve must sound like a deep, heavy bolted cast-iron thump:
    // low-frequency thump energy (<= 180Hz) must dominate high-frequency transient energy,
    // and its spectral brightness must sit noticeably deeper than the brighter iron sump grate clank.
    val valveP0 = ProceduralFoleySynth.synthesizeCue(SoundCue.VALVE_PRESS, phase = 0, pitchFactor = 1f)
    val valveP4 = ProceduralFoleySynth.synthesizeCue(SoundCue.VALVE_PRESS, phase = 4, pitchFactor = 1f)
    val drainClank = ProceduralFoleySynth.synthesizeCue(SoundCue.DRAIN_TOGGLE, phase = 0, pitchFactor = 1f)
    val deskStamp = ProceduralFoleySynth.synthesizeCue(SoundCue.ARC2_STAMP, phase = 5, pitchFactor = 1f)
    assertTrue("Valve cue should be >= 0.26s to carry deep thump and iron tail", valveP0.size >= (sr * 0.26).toInt())

    fun spectralRatio(pcm: ShortArray): Double {
      var diffEnergy = 0L
      var totalEnergy = 0L
      for (i in 1 until pcm.size) {
        val s = pcm[i].toInt()
        totalEnergy += abs(s)
        diffEnergy += abs(s - pcm[i - 1].toInt())
      }
      return diffEnergy.toDouble() / totalEnergy.coerceAtLeast(1L).toDouble()
    }

    fun lowToHighEnergyRatio(pcm: ShortArray, cutoffHz: Double = 180.0): Double {
      // Single-pole RC low-pass split at cutoffHz to separate deep thump body from clicky highs
      val alpha = (2.0 * Math.PI * cutoffHz) / (2.0 * Math.PI * cutoffHz + sr.toDouble())
      var lp = 0.0
      var lowEnergy = 0.0
      var highEnergy = 0.0
      for (sample in pcm) {
        val x = sample.toDouble()
        lp += alpha * (x - lp)
        val hp = x - lp
        lowEnergy += abs(lp)
        highEnergy += abs(hp)
      }
      return lowEnergy / highEnergy.coerceAtLeast(1.0)
    }

    val valveRatio = spectralRatio(valveP0)
    val drainRatio = spectralRatio(drainClank)
    val valveLowHigh = lowToHighEnergyRatio(valveP0)
    val stampLowHigh = lowToHighEnergyRatio(deskStamp)
    assertTrue(
      "Valve thump must be dominated by deep low-end mass (low/high=$valveLowHigh)",
      valveLowHigh > 1.35,
    )
    assertTrue(
      "Valve thump must sit measurably deeper than the metallic drain grate clank (valve=$valveRatio, drain=$drainRatio)",
      valveRatio < drainRatio * 0.72,
    )
    assertTrue(
      "Valve thump should stay deep yet retain damped cast-iron modal texture (ratio=$valveRatio)",
      valveRatio in 0.018..0.12,
    )
    assertTrue(
      "Arc 2 desk stamp must also be deep thump-dominant (low/high=$stampLowHigh)",
      stampLowHigh > 1.25,
    )

    // Phase 4 valve adds wet organic seepage tail compared to Phase 0 dry iron thump
    var diffP0P4 = 0L
    val tailStart = (sr * 0.06).toInt()
    val tailEnd = minOf(valveP0.size, valveP4.size)
    for (i in tailStart until tailEnd) {
      diffP0P4 += abs(valveP4[i].toInt() - valveP0[i].toInt())
    }
    assertTrue("Phase 4 valve should diverge in wet trailing seepage from Phase 0", diffP0P4 > 40_000L)

    // 2. Grafting must be a long (~1.15s) multi-stage fleshy/tendon-tearing cue with sustained energy
    // across early, middle, and late thirds (never a short click + empty tail)
    val graft = ProceduralFoleySynth.synthesizeCue(SoundCue.GRAFT_PERFORM, phase = 3, pitchFactor = 1f)
    assertTrue("Expected ~1.15s graft waveform (>= 1.05s)", graft.size >= (sr * 1.05).toInt())
    val third = graft.size / 3
    for (seg in 0..2) {
      var segPeak = 0
      for (i in (seg * third) until ((seg + 1) * third)) {
        val a = abs(graft[i].toInt())
        if (a > segPeak) segPeak = a
      }
      assertTrue("Graft segment $seg should carry strong sustained flesh/tear energy (peak=$segPeak)", segPeak > 2_500)
    }

    // 3. Tendon-tearing & wet-squelch cues (Tooth, Nodule, Rib, Leech, Excision) must sustain body energy
    // well past initial 40ms transient (proving they are not short percussive clicks)
    for (cue in listOf(
      SoundCue.ANOMALY_TOOTH,
      SoundCue.ANOMALY_NODULE,
      SoundCue.ANOMALY_RIB,
      SoundCue.CREATURE_BUY_LEECH_CLERK,
      SoundCue.EXCISION_CUT,
    )) {
      val pcm = ProceduralFoleySynth.synthesizeCue(cue, phase = 3, pitchFactor = 1f)
      val bodyStart = (sr * 0.05).toInt()
      val bodyEnd = (sr * 0.28).toInt().coerceAtMost(pcm.size)
      var bodyEnergy = 0L
      for (i in bodyStart until bodyEnd) bodyEnergy += abs(pcm[i].toInt())
      val meanAbs = bodyEnergy.toDouble() / (bodyEnd - bodyStart).coerceAtLeast(1)
      assertTrue("Cue ${cue.id} must sustain fleshy/tearing body past 50ms (meanAbs=$meanAbs)", meanAbs > 700.0)
    }
  }

  @Test
  fun modularFoleyCueBanks_partitionAllSoundCuesWithoutOverlapAndMatchFacade() {
    var arc1Count = 0
    var arc2Count = 0
    var arc3Count = 0
    val phase = 2
    val pitchFactor = 1.03f
    val pitch = pitchFactor.toDouble()
    val pDark = 1.0 - (phase * 0.035)

    for (cue in SoundCue.entries) {
      val fromArc1 = Arc1FoleyCueBank.synthesize(cue, pitch, pDark, phase)
      val fromArc2 = Arc2FoleyCueBank.synthesize(cue, pitch)
      val fromArc3 = Arc3FoleyCueBank.synthesize(cue, pitch)
      val matchCount = listOfNotNull(fromArc1, fromArc2, fromArc3).size

      assertEquals(
        "Cue ${cue.id} must belong to exactly one modular cue bank",
        1,
        matchCount,
      )
      when {
        fromArc1 != null -> arc1Count++
        fromArc2 != null -> arc2Count++
        else -> arc3Count++
      }

      val bankPcm = fromArc1 ?: fromArc2 ?: fromArc3!!
      val facadePcm = ProceduralFoleySynth.synthesizeCue(cue, phase = phase, pitchFactor = pitchFactor)
      assertTrue(
        "ProceduralFoleySynth facade output must be bit-for-bit identical to cue bank for ${cue.id}",
        bankPcm.contentEquals(facadePcm),
      )
    }

    assertEquals(24, arc1Count)
    assertEquals(20, arc2Count)
    assertEquals(8, arc3Count)
    assertEquals(SoundCue.entries.size, arc1Count + arc2Count + arc3Count)
  }

  @Test
  fun soundCueResolver_emitsDistinctArc3BreachAndFinaleCues() {
    val catalog = com.example.data.DialogueCatalog.Empty
    val base = OrganGameState(activeArc = GameArc.ARC_3_CONVERGENCE)

    val wrenched = Arc3BreachEngine.wrenchBreach(base, catalog)
    assertTrue(
      SoundCueResolver.resolveTransitionCues(base, wrenched, SoundActionHint.ARC3_WRENCH)
        .contains(SoundCue.ARC3_WRENCH)
    )

    val richJar = base.copy(arc3 = Arc3BreachState(hemorrhage = 100.0))
    val shattered = Arc3BreachEngine.performUnmake(richJar, Arc3UnmakeAct.SHATTER_YESTERDAYS_DUST, catalog)
    assertTrue(
      SoundCueResolver.resolveTransitionCues(richJar, shattered, SoundActionHint.ARC3_UNMAKE)
        .contains(SoundCue.ARC3_JAR_SHATTER)
    )

    val reversed = Arc3BreachEngine.performUnmake(richJar, Arc3UnmakeAct.REVERSE_SUTURE_HANDS, catalog)
    assertTrue(
      SoundCueResolver.resolveTransitionCues(richJar, reversed, SoundActionHint.ARC3_UNMAKE)
        .contains(SoundCue.ARC3_UNMAKE_TEAR)
    )

    val withRupture =
      base.copy(
        arc3 =
          Arc3BreachState(
            activeRuptures =
              listOf(ActiveBreachRupture(1L, BreachRuptureType.CLOTTING_SEAM, 0.2f, 0.3f, 10.0))
          )
      )
    val torn = Arc3BreachEngine.tearBreachRupture(withRupture, 1L, catalog)
    assertTrue(
      SoundCueResolver.resolveTransitionCues(withRupture, torn, SoundActionHint.ARC3_RUPTURE_TEAR)
        .contains(SoundCue.ARC3_RUPTURE_POP)
    )

    val wedgeReady = base.copy(arc3 = Arc3BreachState(fracture = 120.0))
    val wedged = Arc3BreachEngine.driveMemoryWedge(wedgeReady, catalog)
    assertTrue(
      SoundCueResolver.resolveTransitionCues(wedgeReady, wedged, SoundActionHint.ARC3_WEDGE_DRIVE)
        .contains(SoundCue.ARC3_WEDGE_DRIVE)
    )

    val clottingStart = base.copy(arc3 = Arc3BreachState(fracture = 40.0, secondsSinceLastAction = 1.35))
    val clottingTicked = Arc3BreachEngine.tick(clottingStart, 0.3, catalog)
    assertTrue(
      SoundCueResolver.resolveTransitionCues(clottingStart, clottingTicked, SoundActionHint.TICK)
        .contains(SoundCue.ARC3_CLOT_WARNING)
    )

    val finaleStart = base.copy(arc3 = Arc3BreachState(lockedWedges = Arc3Stratum.entries.toSet()))
    val pull1 = Arc3BreachEngine.performFinalPull(finaleStart, catalog)
    assertTrue(
      SoundCueResolver.resolveTransitionCues(finaleStart, pull1, SoundActionHint.ARC3_FINAL_PULL)
        .contains(SoundCue.ARC3_FINAL_PULL)
    )

    val preEnd = finaleStart.copy(arc3 = finaleStart.arc3.copy(finalPullsCompleted = 4))
    val ended = Arc3BreachEngine.performFinalPull(preEnd, catalog)
    assertTrue(
      SoundCueResolver.resolveTransitionCues(preEnd, ended, SoundActionHint.ARC3_FINAL_PULL)
        .contains(SoundCue.ARC3_SUBMERSION_END)
    )
  }
}
