package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DialogueCatalog
import com.example.data.DialogueRepository
import com.example.data.OrganSaveRepository
import com.example.game.Arc2InvestigatorEngine
import com.example.game.CaseDeduction
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
 * Investigator Desk), Arc 3 threshold, local persistence, and Dev Testing Console.
 */
class OrganViewModel(
  dialogueRepository: DialogueRepository? = null,
  private val saveRepository: OrganSaveRepository? = null,
  autoTick: Boolean = true,
  private val tickIntervalMs: Long = 100L,
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

  private var secondsSinceLastAutosave: Double = 0.0
  private var hasUnsavedChanges: Boolean = false

  init {
    if (autoTick) {
      viewModelScope.launch {
        while (isActive) {
          delay(tickIntervalMs)
          advanceTime(tickIntervalMs / 1000.0)
        }
      }
    }
  }

  private fun mutate(
    persistImmediately: Boolean = true,
    transform: (OrganGameState) -> OrganGameState,
  ) {
    var changed = false
    var updated: OrganGameState = _uiState.value
    _uiState.update { current ->
      val next = transform(current)
      changed = next !== current
      updated = next
      next
    }
    if (changed) {
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
    var updated: OrganGameState = _uiState.value
    _uiState.update { current ->
      val next = OrganGameEngine.tick(current, deltaSeconds, catalog)
      changed = next !== current
      updated = next
      next
    }
    if (changed) {
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
    super.onCleared()
  }

  // --- Arc 1 Actions ---
  fun pressValve() = mutate(persistImmediately = false) { OrganGameEngine.pressValve(it, catalog) }

  fun buyCreature(type: CreatureType) =
    mutate { OrganGameEngine.buyCreature(it, type, catalog) }

  fun bottleJar(jar: SpecimenJar) =
    mutate { OrganGameEngine.bottleJar(it, jar, catalog) }

  fun performGraft() = mutate { OrganGameEngine.performGraft(it, catalog) }

  fun performChoirLungGraft() =
    mutate { OrganGameEngine.performChoirLungGraft(it, catalog) }

  fun interactAnomaly(anomalyId: Long) =
    mutate { OrganGameEngine.interactAnomaly(it, anomalyId, catalog) }

  fun unlockScarNode(node: ScarNodeId) =
    mutate { OrganGameEngine.unlockScarNode(it, node, catalog) }

  fun buyRepeatableScarGraft(graft: RepeatableScarGraft) =
    mutate { OrganGameEngine.buyRepeatableScarGraft(it, graft, catalog) }

  fun toggleDrainSeal() = mutate { OrganGameEngine.toggleDrainSeal(it, catalog) }

  fun dredgeSump() = mutate { OrganGameEngine.dredgeSump(it, catalog) }

  fun setEchoDiversion(percent: Int) =
    mutate { OrganGameEngine.setEchoDiversion(it, percent, catalog) }

  fun activateValveClamp() = mutate { OrganGameEngine.activateValveClamp(it, catalog) }

  fun offerToThroat(line: CultDossierLine) =
    mutate { OrganGameEngine.offerToThroat(it, line, catalog) }

  fun performExcision() = mutate { OrganGameEngine.performExcision(it, catalog) }

  fun performBreachToArc2() =
    mutate { OrganGameEngine.performBreachToArc2(it, catalog) }

  // --- Arc 2 Investigator Actions ---
  fun inspectCaseFile() =
    mutate(persistImmediately = false) { Arc2InvestigatorEngine.inspectCaseFile(it, catalog) }

  fun recruitContact(contact: InvestigatorContact) =
    mutate { Arc2InvestigatorEngine.recruitContact(it, contact, catalog) }

  fun pinDeduction(deduction: CaseDeduction) =
    mutate { Arc2InvestigatorEngine.pinDeduction(it, deduction, catalog) }

  fun interactDeskAnomaly(anomalyId: Long) =
    mutate { Arc2InvestigatorEngine.interactDeskAnomaly(it, anomalyId, catalog) }

  fun connectNextChapter() =
    mutate { Arc2InvestigatorEngine.connectNextChapter(it, catalog) }

  fun enterArc3Convergence() =
    mutate { Arc2InvestigatorEngine.enterArc3Convergence(it, catalog) }

  // --- Dev Testing Console Actions ---
  fun devSetActiveArc(arc: GameArc) =
    mutate { OrganDevEngine.setActiveArc(it, arc, catalog) }

  fun devSetHighestMilestone(milestone: Int) =
    mutate { OrganDevEngine.setHighestMilestone(it, milestone, catalog) }

  fun devAssignArc1Resources(
    ichor: Double? = null,
    stillness: Double? = null,
    availableScar: Long? = null,
  ) = mutate {
    OrganDevEngine.assignArc1Resources(it, ichor, stillness, availableScar, catalog)
  }

  fun devSetAllScarNodes(unlockAll: Boolean) =
    mutate { OrganDevEngine.setAllScarNodes(it, unlockAll, catalog) }

  fun devSpawnWallAnomaly() = mutate { OrganDevEngine.spawnWallAnomaly(it) }

  fun devAssignArc2Resources(
    leads: Double? = null,
    silence: Double? = null,
    complicity: Float? = null,
    completedChapters: Int? = null,
  ) = mutate {
    OrganDevEngine.assignArc2Resources(it, leads, silence, complicity, completedChapters, catalog)
  }

  fun devSpawnDeskAnomaly() = mutate { OrganDevEngine.spawnDeskAnomaly(it) }

  fun devResetAll() {
    secondsSinceLastAutosave = 0.0
    hasUnsavedChanges = false
    saveRepository?.clear()
    _uiState.value = OrganDevEngine.resetAll(catalog)
  }

  companion object {
    fun factory(
      dialogueRepository: DialogueRepository,
      saveRepository: OrganSaveRepository? = null,
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
          ) as T
        }
      }
  }
}
