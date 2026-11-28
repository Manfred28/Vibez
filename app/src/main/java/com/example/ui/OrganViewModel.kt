package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.InMemoryAudioEngine
import com.example.audio.OrganAudioEngine
import com.example.audio.SoundActionHint
import com.example.audio.SoundCue
import com.example.audio.SoundCueResolver
import com.example.data.DialogueCatalog
import com.example.data.DialogueRepository
import com.example.data.OrganSaveRepository
import com.example.game.Arc2InvestigatorEngine
import com.example.game.Arc3BreachEngine
import com.example.game.Arc3UnmakeAct
import com.example.game.AuditRedaction
import com.example.game.CaseExhibit
import com.example.game.CreatureType
import com.example.game.CultDossierLine
import com.example.game.GameArc
import com.example.game.InvestigatorContact
import com.example.game.OrganDevEngine
import com.example.game.OrganGameEngine
import com.example.game.OrganGameState
import com.example.game.RepeatableScarGraft
import com.example.game.ScarNodeId
import com.example.game.SpecimenJar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Unified ViewModel for Arc 1 (Chamber + 6 Excisions + Surgical Scar Tree), Arc 2 (Haunted
 * Investigator Desk), Arc 3 threshold, procedural audio engine, local persistence, and Dev Console.
 */
class OrganViewModel(
  dialogueRepository: DialogueRepository? = null,
  private val saveRepository: OrganSaveRepository? = null,
  autoTick: Boolean = true,
  private val tickIntervalMs: Long = 100L,
  private val audioEngine: OrganAudioEngine = InMemoryAudioEngine(),
) : ViewModel() {

  private val catalog: DialogueCatalog =
    try {
      dialogueRepository?.loadCatalog() ?: DialogueCatalog.Empty
    } catch (_: Exception) {
      DialogueCatalog.Empty
    }

  private val _uiState =
    MutableStateFlow(
      try {
        saveRepository?.load(catalog) ?: OrganGameEngine.initialState(catalog)
      } catch (_: Exception) {
        OrganGameEngine.initialState(catalog)
      }
    )
  val uiState: StateFlow<OrganGameState> = _uiState.asStateFlow()
  val isAudioMuted: StateFlow<Boolean> = audioEngine.isMuted

  private var secondsSinceLastAutosave: Double = 0.0
  private var hasUnsavedChanges: Boolean = false

  init {
    audioEngine.updateAmbience(SoundCueResolver.computeAmbientProfile(_uiState.value))
    if (autoTick) {
      viewModelScope.launch {
        while (isActive) {
          delay(tickIntervalMs)
          advanceTime(tickIntervalMs / 1000.0)
        }
      }
    }
  }

  fun toggleAudioMuted() {
    audioEngine.toggleMuted()
  }

  fun setAudioForegroundActive(active: Boolean) {
    audioEngine.setForegroundActive(active)
  }

  fun playRelicInspectCue() {
    audioEngine.playCues(listOf(SoundCue.RELIC_INSPECT), _uiState.value.phase)
  }

  private fun dispatchAudioTransition(
    previous: OrganGameState,
    updated: OrganGameState,
    actionHint: SoundActionHint,
  ) {
    val cues = SoundCueResolver.resolveTransitionCues(previous, updated, actionHint)
    if (cues.isNotEmpty()) {
      audioEngine.playCues(cues, updated.phase)
    }
    audioEngine.updateAmbience(SoundCueResolver.computeAmbientProfile(updated))
  }

  private fun mutate(
    persistImmediately: Boolean = true,
    actionHint: SoundActionHint = SoundActionHint.TICK,
    transform: (OrganGameState) -> OrganGameState,
  ) {
    var changed = false
    var previous: OrganGameState = _uiState.value
    var updated: OrganGameState = _uiState.value
    _uiState.update { current ->
      previous = current
      val next = transform(current)
      changed = next !== current
      updated = next
      next
    }
    if (changed) {
      dispatchAudioTransition(previous, updated, actionHint)
      if (persistImmediately) {
        secondsSinceLastAutosave = 0.0
        hasUnsavedChanges = false
        saveRepository?.save(updated)
      } else {
        hasUnsavedChanges = true
      }
    }
  }

  fun advanceTime(deltaSeconds: Double) {
    var changed = false
    var previous: OrganGameState = _uiState.value
    var updated: OrganGameState = _uiState.value
    _uiState.update { current ->
      previous = current
      val next = OrganGameEngine.tick(current, deltaSeconds, catalog)
      changed = next !== current
      updated = next
      next
    }
    if (changed) {
      dispatchAudioTransition(previous, updated, SoundActionHint.TICK)
      hasUnsavedChanges = true
    }
    secondsSinceLastAutosave += deltaSeconds
    if (secondsSinceLastAutosave >= 5.0) {
      secondsSinceLastAutosave = 0.0
      if (hasUnsavedChanges) {
        hasUnsavedChanges = false
        saveRepository?.save(updated)
      }
    }
  }

  override fun onCleared() {
    if (hasUnsavedChanges) {
      hasUnsavedChanges = false
      saveRepository?.save(_uiState.value)
    }
    audioEngine.release()
    super.onCleared()
  }

  // --- Arc 1 Actions ---
  fun pressValve() =
    mutate(persistImmediately = false, actionHint = SoundActionHint.VALVE_PRESS) {
      OrganGameEngine.pressValve(it, catalog)
    }

  fun buyCreature(type: CreatureType) =
    mutate(actionHint = SoundActionHint.CREATURE_BUY) {
      OrganGameEngine.buyCreature(it, type, catalog)
    }

  fun buyMaxCreature(type: CreatureType) =
    mutate(actionHint = SoundActionHint.CREATURE_BUY) {
      OrganGameEngine.buyMaxCreature(it, type, catalog)
    }

  fun bottleJar(jar: SpecimenJar) =
    mutate(actionHint = SoundActionHint.JAR_BOTTLE) {
      OrganGameEngine.bottleJar(it, jar, catalog)
    }

  fun performGraft() =
    mutate(actionHint = SoundActionHint.GRAFT_PERFORM) {
      OrganGameEngine.performGraft(it, catalog)
    }

  fun performChoirLungGraft() =
    mutate(actionHint = SoundActionHint.GRAFT_PERFORM) {
      OrganGameEngine.performChoirLungGraft(it, catalog)
    }

  fun interactAnomaly(anomalyId: Long) =
    mutate(actionHint = SoundActionHint.ANOMALY_INTERACT) {
      OrganGameEngine.interactAnomaly(it, anomalyId, catalog)
    }

  fun unlockScarNode(node: ScarNodeId) =
    mutate(actionHint = SoundActionHint.SCAR_UNLOCK) {
      OrganGameEngine.unlockScarNode(it, node, catalog)
    }

  fun buyRepeatableScarGraft(graft: RepeatableScarGraft) =
    mutate(actionHint = SoundActionHint.SCAR_UNLOCK) {
      OrganGameEngine.buyRepeatableScarGraft(it, graft, catalog)
    }

  fun toggleDrainSeal() =
    mutate(actionHint = SoundActionHint.DRAIN_TOGGLE) {
      OrganGameEngine.toggleDrainSeal(it, catalog)
    }

  fun dredgeSump() =
    mutate(actionHint = SoundActionHint.SUMP_DREDGE) {
      OrganGameEngine.dredgeSump(it, catalog)
    }

  fun setEchoDiversion(percent: Int) =
    mutate(actionHint = SoundActionHint.ECHO_DIVERSION) {
      OrganGameEngine.setEchoDiversion(it, percent, catalog)
    }

  fun activateValveClamp() =
    mutate(actionHint = SoundActionHint.CLAMP_ENGAGE) {
      OrganGameEngine.activateValveClamp(it, catalog)
    }

  fun offerToThroat(line: CultDossierLine) =
    mutate(actionHint = SoundActionHint.THROAT_OFFER) {
      OrganGameEngine.offerToThroat(it, line, catalog)
    }

  fun performExcision() =
    mutate(actionHint = SoundActionHint.EXCISION) {
      OrganGameEngine.performExcision(it, catalog)
    }

  fun performBreachToArc2() =
    mutate(actionHint = SoundActionHint.BREACH_TO_ARC2) {
      OrganGameEngine.performBreachToArc2(it, catalog)
    }

  // --- Arc 2 Investigator Actions ---
  val investigatorActions: InvestigatorActions =
    InvestigatorActions(
      onToggleLamp = ::toggleArc2Lamp,
      onInspectCaseFile = ::inspectCaseFile,
      onRecruitContact = ::recruitContact,
      onRecruitMaxContact = ::recruitMaxContact,
      onUnlockExhibit = ::unlockExhibit,
      onToggleExhibitPair = ::toggleExhibitPair,
      onTriggerReelScrub = ::triggerReelScrub,
      onDissolveRedaction = ::dissolveRedaction,
      onInteractDeskAnomaly = ::interactDeskAnomaly,
      onPivotNextCase = ::pivotNextCase,
      onEnterArc3 = ::enterArc3Convergence,
    )

  fun toggleArc2Lamp() =
    mutate(actionHint = SoundActionHint.ARC2_LAMP_TOGGLE) {
      Arc2InvestigatorEngine.toggleLamp(it, catalog)
    }

  fun inspectCaseFile() =
    mutate(persistImmediately = false, actionHint = SoundActionHint.ARC2_STAMP) {
      Arc2InvestigatorEngine.inspectCaseFile(it, catalog)
    }

  fun recruitContact(contact: InvestigatorContact) =
    mutate(actionHint = SoundActionHint.ARC2_CONTACT) {
      Arc2InvestigatorEngine.recruitContact(it, contact, catalog)
    }

  fun recruitMaxContact(contact: InvestigatorContact) =
    mutate(actionHint = SoundActionHint.ARC2_CONTACT) {
      Arc2InvestigatorEngine.recruitMaxContact(it, contact, catalog)
    }

  fun unlockExhibit(exhibit: CaseExhibit) =
    mutate(actionHint = SoundActionHint.ARC2_DEDUCTION) {
      Arc2InvestigatorEngine.unlockExhibit(it, exhibit, catalog)
    }

  fun toggleExhibitPair(a: CaseExhibit, b: CaseExhibit) =
    mutate(actionHint = SoundActionHint.ARC2_STRING_WIRE) {
      Arc2InvestigatorEngine.toggleExhibitPair(it, a, b, catalog)
    }

  fun triggerReelScrub() =
    mutate(actionHint = SoundActionHint.ARC2_REEL_SCRUB) {
      Arc2InvestigatorEngine.triggerReelScrub(it, catalog)
    }

  fun dissolveRedaction(redaction: AuditRedaction) =
    mutate(actionHint = SoundActionHint.ARC2_REDACTION_DISSOLVE) {
      Arc2InvestigatorEngine.dissolveRedaction(it, redaction, catalog)
    }

  fun interactDeskAnomaly(anomalyId: Long) =
    mutate(actionHint = SoundActionHint.ARC2_DESK_ANOMALY) {
      Arc2InvestigatorEngine.interactDeskAnomaly(it, anomalyId, catalog)
    }

  fun pivotNextCase() =
    mutate(actionHint = SoundActionHint.ARC2_CHAPTER) {
      Arc2InvestigatorEngine.pivotNextCase(it, catalog)
    }

  fun enterArc3Convergence() =
    mutate(actionHint = SoundActionHint.ARC3_ENTER) {
      Arc2InvestigatorEngine.enterArc3Convergence(it, catalog)
    }

  // --- Arc 3 Breach Actions ("The Unmaking") ---
  val arc3BreachActions: Arc3BreachActions =
    Arc3BreachActions(
      onWrenchBreach = ::wrenchArc3Breach,
      onPerformUnmake = ::performArc3Unmake,
      onTearRupture = ::tearArc3Rupture,
      onDriveWedge = ::driveArc3Wedge,
      onPerformFinalPull = ::performArc3FinalPull,
    )

  fun wrenchArc3Breach() =
    mutate(persistImmediately = false, actionHint = SoundActionHint.ARC3_WRENCH) {
      Arc3BreachEngine.wrenchBreach(it, catalog)
    }

  fun performArc3Unmake(act: Arc3UnmakeAct) =
    mutate(actionHint = SoundActionHint.ARC3_UNMAKE) {
      Arc3BreachEngine.performUnmake(it, act, catalog)
    }

  fun tearArc3Rupture(ruptureId: Long) =
    mutate(actionHint = SoundActionHint.ARC3_RUPTURE_TEAR) {
      Arc3BreachEngine.tearBreachRupture(it, ruptureId, catalog)
    }

  fun driveArc3Wedge() =
    mutate(actionHint = SoundActionHint.ARC3_WEDGE_DRIVE) {
      Arc3BreachEngine.driveMemoryWedge(it, catalog)
    }

  fun performArc3FinalPull() =
    mutate(actionHint = SoundActionHint.ARC3_FINAL_PULL) {
      Arc3BreachEngine.performFinalPull(it, catalog)
    }

  // --- Dev Testing Console Actions ---
  fun devSetActiveArc(arc: GameArc) =
    mutate(actionHint = SoundActionHint.DEV) { OrganDevEngine.setActiveArc(it, arc, catalog) }

  fun devSetHighestMilestone(milestone: Int) =
    mutate(actionHint = SoundActionHint.DEV) {
      OrganDevEngine.setHighestMilestone(it, milestone, catalog)
    }

  fun devAssignArc1Resources(
    ichor: Double? = null,
    stillness: Double? = null,
    availableScar: Long? = null,
  ) = mutate(actionHint = SoundActionHint.DEV) {
    OrganDevEngine.assignArc1Resources(it, ichor, stillness, availableScar, catalog)
  }

  fun devSetAllScarNodes(unlockAll: Boolean) =
    mutate(actionHint = SoundActionHint.DEV) {
      OrganDevEngine.setAllScarNodes(it, unlockAll, catalog)
    }

  fun devSpawnWallAnomaly() =
    mutate(actionHint = SoundActionHint.DEV) { OrganDevEngine.spawnWallAnomaly(it) }

  fun devAssignArc2Resources(
    leads: Double? = null,
    silence: Double? = null,
    caseAngle: Int? = null,
    completedChapters: Int? = null,
  ) = mutate(actionHint = SoundActionHint.DEV) {
    OrganDevEngine.assignArc2Resources(it, leads, silence, caseAngle, completedChapters, catalog)
  }

  fun devSetArc2AllExhibits(unlockAll: Boolean) =
    mutate(actionHint = SoundActionHint.DEV) {
      OrganDevEngine.setArc2AllExhibits(it, unlockAll, catalog)
    }

  fun devToggleArc2Lamp() =
    mutate(actionHint = SoundActionHint.DEV) {
      OrganDevEngine.toggleArc2Lamp(it, catalog)
    }

  fun devResetArc2() =
    mutate(actionHint = SoundActionHint.DEV) {
      OrganDevEngine.resetArc2(it, catalog)
    }

  fun devSpawnDeskAnomaly() =
    mutate(actionHint = SoundActionHint.DEV) { OrganDevEngine.spawnDeskAnomaly(it) }

  fun devSetArc3Stratum(stratum: Int) =
    mutate(actionHint = SoundActionHint.DEV) { OrganDevEngine.setArc3Stratum(it, stratum, catalog) }

  fun devAssignArc3Resources(hemorrhage: Double? = null, fillFracture: Boolean = false) =
    mutate(actionHint = SoundActionHint.DEV) {
      OrganDevEngine.assignArc3Resources(it, hemorrhage, fillFracture, catalog)
    }

  fun devJumpArc3Finale(submerged: Boolean = false) =
    mutate(actionHint = SoundActionHint.DEV) { OrganDevEngine.jumpArc3Finale(it, submerged, catalog) }

  fun devSpawnBreachRupture() =
    mutate(actionHint = SoundActionHint.DEV) { OrganDevEngine.spawnBreachRupture(it) }

  fun devResetArc3() =
    mutate(actionHint = SoundActionHint.DEV) { OrganDevEngine.resetArc3(it, catalog) }

  fun devResetAll() {
    secondsSinceLastAutosave = 0.0
    hasUnsavedChanges = false
    saveRepository?.clear()
    val fresh = OrganDevEngine.resetAll(catalog)
    _uiState.value = fresh
    audioEngine.updateAmbience(SoundCueResolver.computeAmbientProfile(fresh))
  }

  companion object {
    fun factory(
      dialogueRepository: DialogueRepository,
      saveRepository: OrganSaveRepository? = null,
      audioEngineProvider: () -> OrganAudioEngine = { InMemoryAudioEngine() },
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          require(modelClass.isAssignableFrom(OrganViewModel::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
          }
          return OrganViewModel(
            dialogueRepository = dialogueRepository,
            saveRepository = saveRepository,
            audioEngine = audioEngineProvider(),
          ) as T
        }
      }
  }
}
