package com.example.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CounterUiState(val count: Long = 0L)

class CounterViewModel : ViewModel() {
  private val _uiState = MutableStateFlow(CounterUiState())
  val uiState: StateFlow<CounterUiState> = _uiState.asStateFlow()

  fun increment() {
    _uiState.update { current -> current.copy(count = current.count + 1L) }
  }
}
