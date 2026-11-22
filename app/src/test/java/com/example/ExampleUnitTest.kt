package com.example

import com.example.ui.CounterViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun counterViewModel_startsAtZeroAndIncrementsByOne() {
    val viewModel = CounterViewModel()
    assertEquals(0L, viewModel.uiState.value.count)

    viewModel.increment()
    assertEquals(1L, viewModel.uiState.value.count)

    viewModel.increment()
    viewModel.increment()
    assertEquals(3L, viewModel.uiState.value.count)
  }
}
