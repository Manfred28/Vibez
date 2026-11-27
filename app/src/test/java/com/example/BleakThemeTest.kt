package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.theme.BleakAtmosphere
import com.example.ui.theme.BleakColorScheme
import com.example.ui.theme.BleakMonoFontFamily
import com.example.ui.theme.BleakShapes
import com.example.ui.theme.BleakTheme
import com.example.ui.theme.BleakTypography
import com.example.ui.theme.BreachCrimsonColorScheme
import com.example.ui.theme.bleakAtmosphereForPhase
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BleakThemeTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun chroma(color: Color): Float {
    val maxChannel = max(color.red, max(color.green, color.blue))
    val minChannel = min(color.red, min(color.green, color.blue))
    return maxChannel - minChannel
  }

  @Test
  fun bleakColorScheme_isDarkDesaturatedAndReadable() {
    assertTrue("Background must be abyssal dark", BleakColorScheme.background.luminance() < 0.02f)
    assertTrue("Surface must be dark", BleakColorScheme.surface.luminance() < 0.05f)
    assertTrue("SurfaceVariant must be dark", BleakColorScheme.surfaceVariant.luminance() < 0.08f)

    // Verify all core tokens are heavily desaturated (low chroma)
    assertTrue("Primary must be desaturated", chroma(BleakColorScheme.primary) < 0.12f)
    assertTrue("Secondary must be desaturated", chroma(BleakColorScheme.secondary) < 0.15f)
    assertTrue("Tertiary must be desaturated", chroma(BleakColorScheme.tertiary) < 0.25f)
    assertTrue("onBackground must be muted bone", chroma(BleakColorScheme.onBackground) < 0.08f)

    // Verify legible luminance separation between foreground bone ink and dark background
    val contrastRatio =
      (BleakColorScheme.onBackground.luminance() + 0.05f) /
        (BleakColorScheme.background.luminance() + 0.05f)
    assertTrue("Foreground contrast ratio must meet WCAG AAA (>= 7.0)", contrastRatio >= 7.0f)
  }

  @Test
  fun bleakTypography_usesMonospacedFontAcrossAllRolesAndTabularNumeralsForDisplay() {
    val styles =
      listOf(
        BleakTypography.displayLarge,
        BleakTypography.displayMedium,
        BleakTypography.headlineLarge,
        BleakTypography.headlineMedium,
        BleakTypography.titleLarge,
        BleakTypography.titleMedium,
        BleakTypography.bodyLarge,
        BleakTypography.bodyMedium,
        BleakTypography.labelLarge,
        BleakTypography.labelMedium,
        BleakTypography.labelSmall,
      )

    styles.forEach { style -> assertEquals(BleakMonoFontFamily, style.fontFamily) }
    assertEquals("tnum", BleakTypography.displayLarge.fontFeatureSettings)
    assertEquals("tnum", BleakTypography.displayMedium.fontFeatureSettings)
    assertEquals("tnum", BleakTypography.headlineLarge.fontFeatureSettings)
  }

  @Test
  fun bleakAtmosphere_clampsOutOfRangePhasesAndEscalatesIntensityAcrossPhases0To4() {
    assertEquals(0, bleakAtmosphereForPhase(-3).phase)
    assertEquals(7, bleakAtmosphereForPhase(99).phase)

    val atmospheres = (0..4).map { bleakAtmosphereForPhase(it) }
    for (phase in 1..4) {
      val prev = atmospheres[phase - 1]
      val curr = atmospheres[phase]
      assertTrue(
        "Phase $phase distortion must exceed phase ${phase - 1}",
        curr.distortionFactor > prev.distortionFactor,
      )
      assertTrue(
        "Phase $phase scanlineAlpha must exceed phase ${phase - 1}",
        curr.scanlineAlpha > prev.scanlineAlpha,
      )
      assertTrue(
        "Phase $phase auraPrimaryAlpha must exceed phase ${phase - 1}",
        curr.auraPrimaryAlpha > prev.auraPrimaryAlpha,
      )
      assertTrue(
        "Phase $phase pulseDurationMs must tighten from phase ${phase - 1}",
        curr.pulseDurationMs < prev.pulseDurationMs,
      )
    }
  }

  @Test
  fun bleakThemeComposable_providesColorSchemeTypographyShapesAndAtmosphere() {
    var capturedAtmosphere: BleakAtmosphere? = null
    var capturedBackground: Color? = null
    var capturedFontFamily: Any? = null
    var capturedSmallShape: Any? = null

    composeTestRule.setContent {
      BleakTheme(phase = 3) {
        capturedAtmosphere = BleakTheme.atmosphere
        capturedBackground = MaterialTheme.colorScheme.background
        capturedFontFamily = MaterialTheme.typography.bodyLarge.fontFamily
        capturedSmallShape = MaterialTheme.shapes.small
      }
    }
    composeTestRule.waitForIdle()

    assertEquals(bleakAtmosphereForPhase(3), capturedAtmosphere)
    assertEquals(BleakColorScheme.background, capturedBackground)
    assertEquals(BleakMonoFontFamily, capturedFontFamily)
    assertEquals(BleakShapes.small, capturedSmallShape)
  }

  @Test
  fun bleakThemeComposable_appliesBreachCrimsonColorSchemeAndPhase7AtmosphereInBreachMode() {
    var capturedAtmosphere: BleakAtmosphere? = null
    var capturedBackground: Color? = null
    var capturedPrimary: Color? = null

    composeTestRule.setContent {
      BleakTheme(phase = 7, breachMode = true) {
        capturedAtmosphere = BleakTheme.atmosphere
        capturedBackground = MaterialTheme.colorScheme.background
        capturedPrimary = MaterialTheme.colorScheme.primary
      }
    }
    composeTestRule.waitForIdle()

    assertEquals(7, capturedAtmosphere?.phase)
    assertEquals(BreachCrimsonColorScheme.background, capturedBackground)
    assertEquals(BreachCrimsonColorScheme.primary, capturedPrimary)
  }
}
