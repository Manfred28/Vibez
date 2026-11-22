package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DialogueCatalog
import com.example.data.DialogueRepository
import com.example.game.CreatureType
import com.example.game.OrganGameEngine
import com.example.game.OrganGameState
import com.example.game.SpecimenJar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel for Direction 1 ("The Tending of the Unnamed Organ").
 *
 * Design & Progression Contract: All mechanics, escalation phases, and emotional/aesthetic shifts
 * align with `/GAME_DESIGN_BIBLE.md`. Progress-bound narrative fragments live strictly in
 * `app/src/main/assets/dialogues.json`.
 */
class OrganViewModel(
  dialogueRepository: DialogueRepository? = null,
  autoTick: Boolean = true,
  private val tickIntervalMs: Long = 100L,
) : ViewModel() {

  private val catalog: DialogueCatalog =
    try {
      dialogueRepository?.loadCatalog() ?: DialogueCatalog.Empty
    } catch (_: Exception) {
      DialogueCatalog.Empty
    }

  private val _uiState = MutableStateFlow(OrganGameEngine.initialState(catalog))
  val uiState: StateFlow<OrganGameState> = _uiState.asStateFlow()

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

  fun pressValve() {
    _uiState.update { current -> OrganGameEngine.pressValve(current, catalog) }
  }

  fun advanceTime(deltaSeconds: Double) {
    _uiState.update { current -> OrganGameEngine.tick(current, deltaSeconds, catalog) }
  }

  fun buyCreature(type: CreatureType) {
    _uiState.update { current -> OrganGameEngine.buyCreature(current, type, catalog) }
  }

  fun bottleJar(jar: SpecimenJar) {
    _uiState.update { current -> OrganGameEngine.bottleJar(current, jar, catalog) }
  }

  fun performGraft() {
    _uiState.update { current -> OrganGameEngine.performGraft(current, catalog) }
  }

  fun interactAnomaly(anomalyId: Long) {
    _uiState.update { current -> OrganGameEngine.interactAnomaly(current, anomalyId, catalog) }
  }

  fun performExcision() {
    _uiState.update { current -> OrganGameEngine.performExcision(current, catalog) }
  }

  companion object {
    fun factory(dialogueRepository: DialogueRepository): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          require(modelClass.isAssignableFrom(OrganViewModel::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
          }
          return OrganViewModel(dialogueRepository = dialogueRepository) as T
        }
      }
  }
}
