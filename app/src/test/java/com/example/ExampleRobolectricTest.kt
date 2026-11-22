package com.example

import android.content.Context
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.CounterRoute
import com.example.ui.CounterViewModel
import com.example.ui.theme.TapCounterTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Tap Counter", appName)
  }

  @Test
  fun `tapping increment button increases counter`() {
    val viewModel = CounterViewModel()
    composeTestRule.setContent { TapCounterTheme { CounterRoute(viewModel = viewModel) } }

    composeTestRule.onNodeWithTag("counter_text").assertTextEquals("0")
    composeTestRule.onNodeWithTag("increment_button").performClick()
    composeTestRule.onNodeWithTag("counter_text").assertTextEquals("1")
    composeTestRule.onNodeWithTag("increment_button").performClick()
    composeTestRule.onNodeWithTag("counter_text").assertTextEquals("2")
  }
}
