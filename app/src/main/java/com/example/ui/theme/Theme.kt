package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Escalation-aware atmospheric tokens that intensify the surreal, grotesque, and hopeless mood
 * across Phases 0–4 (Arc 1) and Phases 5–6 (Arc 2 Investigator Desk) as defined in `/GAME_DESIGN_BIBLE.md`.
 */
@Immutable
data class BleakAtmosphere(
  val phase: Int,
  val auraPrimary: Color,
  val auraPrimaryAlpha: Float,
  val auraGrotesque: Color,
  val auraGrotesqueAlpha: Float,
  val hairline: Color,
  val scanlineAlpha: Float,
  val distortionFactor: Float,
  val pulseDurationMs: Int,
)

val BleakColorScheme: ColorScheme =
  darkColorScheme(
    primary = PaleLichen,
    onPrimary = AbyssInk,
    primaryContainer = LichenShadow,
    onPrimaryContainer = BleachedBone,
    secondary = BileOlive,
    onSecondary = AbyssInk,
    secondaryContainer = BileShadow,
    onSecondaryContainer = BleachedBone,
    tertiary = DriedClot,
    onTertiary = BleachedBone,
    tertiaryContainer = ClotShadow,
    onTertiaryContainer = BleachedBone,
    background = VoidAbyss,
    onBackground = BleachedBone,
    surface = SunkenSlate,
    onSurface = BleachedBone,
    surfaceVariant = ChitinSurface,
    onSurfaceVariant = WeatheredAsh,
    surfaceContainerLowest = VoidAbyss,
    surfaceContainerLow = SunkenSlate,
    surfaceContainer = ApparatusSurface,
    surfaceContainerHigh = ChitinSurface,
    outline = FractureOutline,
    outlineVariant = HairlineAsh,
    error = DreadError,
    onError = BleachedBone,
    errorContainer = DreadErrorContainer,
    onErrorContainer = BleachedBone,
  )

val InvestigatorColorScheme: ColorScheme =
  darkColorScheme(
    primary = TungstenAmberAsh,
    onPrimary = AbyssInk,
    primaryContainer = TungstenShadow,
    onPrimaryContainer = ManilaAsh,
    secondary = PaleLichen,
    onSecondary = AbyssInk,
    secondaryContainer = LichenShadow,
    onSecondaryContainer = BleachedBone,
    tertiary = DriedClot,
    onTertiary = BleachedBone,
    tertiaryContainer = ClotShadow,
    onTertiaryContainer = BleachedBone,
    background = RainNightSlate,
    onBackground = ManilaAsh,
    surface = DeskBlotterSurface,
    onSurface = ManilaAsh,
    surfaceVariant = FolderSlate,
    onSurfaceVariant = WeatheredAsh,
    surfaceContainerLowest = RainNightSlate,
    surfaceContainerLow = DeskBlotterSurface,
    surfaceContainer = FolderSlate,
    surfaceContainerHigh = ChitinSurface,
    outline = PrecinctOutline,
    outlineVariant = PrecinctLine,
    error = DreadError,
    onError = BleachedBone,
    errorContainer = DreadErrorContainer,
    onErrorContainer = BleachedBone,
  )

val BleakShapes =
  Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(3.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(8.dp),
  )

/** Resolves the phase-specific atmospheric parameters for escalation phases 0..4. */
fun bleakAtmosphereForPhase(phase: Int): BleakAtmosphere =
  when (phase.coerceIn(0, 6)) {
    0 ->
      BleakAtmosphere(
        phase = 0,
        auraPrimary = PaleLichen,
        auraPrimaryAlpha = 0.08f,
        auraGrotesque = BileOlive,
        auraGrotesqueAlpha = 0.03f,
        hairline = HairlineAsh,
        scanlineAlpha = 0.04f,
        distortionFactor = 0.0f,
        pulseDurationMs = 5200,
      )
    1 ->
      BleakAtmosphere(
        phase = 1,
        auraPrimary = BileOlive,
        auraPrimaryAlpha = 0.11f,
        auraGrotesque = DriedClot,
        auraGrotesqueAlpha = 0.05f,
        hairline = HairlineAsh,
        scanlineAlpha = 0.06f,
        distortionFactor = 0.25f,
        pulseDurationMs = 4400,
      )
    2 ->
      BleakAtmosphere(
        phase = 2,
        auraPrimary = DriedClot,
        auraPrimaryAlpha = 0.14f,
        auraGrotesque = BileOlive,
        auraGrotesqueAlpha = 0.09f,
        hairline = FractureOutline,
        scanlineAlpha = 0.08f,
        distortionFactor = 0.50f,
        pulseDurationMs = 3600,
      )
    3 ->
      BleakAtmosphere(
        phase = 3,
        auraPrimary = BruisedViolet,
        auraPrimaryAlpha = 0.16f,
        auraGrotesque = DriedClot,
        auraGrotesqueAlpha = 0.12f,
        hairline = FractureOutline,
        scanlineAlpha = 0.10f,
        distortionFactor = 0.75f,
        pulseDurationMs = 3000,
      )
    4 ->
      BleakAtmosphere(
        phase = 4,
        auraPrimary = BruisedViolet,
        auraPrimaryAlpha = 0.20f,
        auraGrotesque = DriedClot,
        auraGrotesqueAlpha = 0.15f,
        hairline = DriedClot.copy(alpha = 0.45f),
        scanlineAlpha = 0.13f,
        distortionFactor = 1.0f,
        pulseDurationMs = 2400,
      )
    5 ->
      BleakAtmosphere(
        phase = 5,
        auraPrimary = TungstenAmberAsh,
        auraPrimaryAlpha = 0.10f,
        auraGrotesque = PaleLichen,
        auraGrotesqueAlpha = 0.05f,
        hairline = PrecinctLine,
        scanlineAlpha = 0.05f,
        distortionFactor = 0.35f,
        pulseDurationMs = 4600,
      )
    else ->
      BleakAtmosphere(
        phase = 6,
        auraPrimary = DriedClot,
        auraPrimaryAlpha = 0.18f,
        auraGrotesque = TungstenAmberAsh,
        auraGrotesqueAlpha = 0.10f,
        hairline = DriedClot.copy(alpha = 0.48f),
        scanlineAlpha = 0.11f,
        distortionFactor = 0.90f,
        pulseDurationMs = 2800,
      )
  }

val LocalBleakAtmosphere = staticCompositionLocalOf { bleakAtmosphereForPhase(0) }

object BleakTheme {
  val atmosphere: BleakAtmosphere
    @Composable @ReadOnlyComposable get() = LocalBleakAtmosphere.current
}

/**
 * Core application theme enforcing a dark, desaturated, monospaced surreal/grotesque/hopeless
 * aesthetic aligned with `/GAME_DESIGN_BIBLE.md`.
 */
@Composable
fun BleakTheme(
  phase: Int = 0,
  investigatorMode: Boolean = false,
  content: @Composable () -> Unit,
) {
  val effectivePhase = if (investigatorMode && phase < 5) 5 else phase
  val atmosphere = remember(effectivePhase) { bleakAtmosphereForPhase(effectivePhase) }
  val scheme = if (investigatorMode) InvestigatorColorScheme else BleakColorScheme
  CompositionLocalProvider(LocalBleakAtmosphere provides atmosphere) {
    MaterialTheme(
      colorScheme = scheme,
      typography = BleakTypography,
      shapes = BleakShapes,
      content = content,
    )
  }
}
