package com.example

import com.example.audio.AmbientSoundProfile
import com.example.audio.InMemoryAudioEngine
import com.example.audio.ProceduralFoleySynth
import com.example.audio.SoundActionHint
import com.example.audio.SoundCue
import com.example.audio.SoundCueResolver
import com.example.data.DialogueCatalog
import com.example.data.DialogueEntry
import com.example.data.DialogueTrigger
import com.example.game.ActiveDeskAnomaly
import com.example.game.ActiveWallAnomaly
import com.example.game.Arc2InvestigatorEngine
import com.example.game.CaseDeduction
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

    // Creature buy
    val bought = OrganGameEngine.buyCreature(base, CreatureType.SUTURE_HAND, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.CREATURE_BUY),
      SoundCueResolver.resolveTransitionCues(base, bought, SoundActionHint.CREATURE_BUY),
    )

    // Jar bottle
    val jarred = OrganGameEngine.bottleJar(base, SpecimenJar.YESTERDAYS_DUST, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.JAR_BOTTLE),
      SoundCueResolver.resolveTransitionCues(base, jarred, SoundActionHint.JAR_BOTTLE),
    )

    // Graft tendril
    val grafted = OrganGameEngine.performGraft(base, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.GRAFT_PERFORM),
      SoundCueResolver.resolveTransitionCues(base, grafted, SoundActionHint.GRAFT_PERFORM),
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
        ichor = 12_000_000_000.0,
        cycleLifetimeIchor = 12_000_000_000.0,
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
              leads = 250_000.0,
              lifetimeLeads = 250_000.0,
              silence = 300.0,
              activeDeskAnomalies =
                listOf(ActiveDeskAnomaly(5L, DeskAnomalyType.EVIDENCE_ENVELOPE, 0.5f, 0.5f)),
            ),
        )
      }

    val stamped = Arc2InvestigatorEngine.inspectCaseFile(arc2Base, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.ARC2_STAMP),
      SoundCueResolver.resolveTransitionCues(arc2Base, stamped, SoundActionHint.ARC2_STAMP),
    )

    val contacted =
      Arc2InvestigatorEngine.recruitContact(
        arc2Base,
        InvestigatorContact.NIGHT_ARCHIVIST,
        DialogueCatalog.Empty,
      )
    assertEquals(
      listOf(SoundCue.ARC2_CONTACT),
      SoundCueResolver.resolveTransitionCues(arc2Base, contacted, SoundActionHint.ARC2_CONTACT),
    )

    val deduced =
      Arc2InvestigatorEngine.pinDeduction(
        arc2Base,
        CaseDeduction.HYDRO_WARD_INVOICES,
        DialogueCatalog.Empty,
      )
    assertEquals(
      listOf(SoundCue.ARC2_DEDUCTION),
      SoundCueResolver.resolveTransitionCues(arc2Base, deduced, SoundActionHint.ARC2_DEDUCTION),
    )

    val deskAnomaly =
      Arc2InvestigatorEngine.interactDeskAnomaly(arc2Base, 5L, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.ARC2_DESK_ANOMALY),
      SoundCueResolver.resolveTransitionCues(
        arc2Base,
        deskAnomaly,
        SoundActionHint.ARC2_DESK_ANOMALY,
      ),
    )

    // Chapter 1 -> ARC2_CHAPTER; Chapter 4 -> ARC2_FILE_ZERO
    val ch1 = Arc2InvestigatorEngine.connectNextChapter(arc2Base, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.ARC2_CHAPTER),
      SoundCueResolver.resolveTransitionCues(arc2Base, ch1, SoundActionHint.ARC2_CHAPTER),
    )
    val ch3Done = arc2Base.copy(arc2 = arc2Base.arc2.copy(completedChapters = 3))
    val ch4 = Arc2InvestigatorEngine.connectNextChapter(ch3Done, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.ARC2_FILE_ZERO),
      SoundCueResolver.resolveTransitionCues(ch3Done, ch4, SoundActionHint.ARC2_CHAPTER),
    )

    // Arc 3 convergence
    val arc3 = Arc2InvestigatorEngine.enterArc3Convergence(ch4, DialogueCatalog.Empty)
    assertEquals(
      listOf(SoundCue.ARC3_ENTER),
      SoundCueResolver.resolveTransitionCues(ch4, arc3, SoundActionHint.ARC3_ENTER),
    )
  }

  @Test
  fun computeAmbientProfile_escalatesAcrossPhasesStillnessClampArc2AndArc3() {
    val p0 = SoundCueResolver.computeAmbientProfile(OrganGameState(phase = 0))
    val p4 = SoundCueResolver.computeAmbientProfile(OrganGameState(phase = 4))
    val p2Still =
      SoundCueResolver.computeAmbientProfile(OrganGameState(phase = 2, isStillnessActive = true))
    val p2Active =
      SoundCueResolver.computeAmbientProfile(OrganGameState(phase = 2, isStillnessActive = false))
    val clamped =
      SoundCueResolver.computeAmbientProfile(OrganGameState(phase = 4, clampRemainingSeconds = 8.0))
    val arc2 =
      SoundCueResolver.computeAmbientProfile(
        OrganGameState(
          activeArc = GameArc.ARC_2_INVESTIGATION,
          arc2 = OrganGameState().arc2.copy(complicity = 0.8f, isSilenceActive = true),
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
    assertTrue(arc2.rainMix > 0.7f)
    assertTrue(arc2.complicityBleed >= 0.8f)
    assertTrue(arc3.rainMix > 0.3f && arc3.fleshDepth > 0.2f)
  }

  @Test
  fun proceduralFoleySynth_synthesizesNonSilentBoundedPcmForEveryCueAndAmbientBed() {
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

    vm.toggleAudioMuted()
    assertTrue(vm.isAudioMuted.value)

    // Save reset updates ambient profile back to Phase 0 without overriding user mute choice
    vm.devResetAll()
    assertTrue(vm.isAudioMuted.value)
    assertEquals(0, engine.ambientProfiles.last().phase)

    vm.setAudioForegroundActive(false)
    assertEquals(listOf(false), engine.foregroundStates)
  }
}
