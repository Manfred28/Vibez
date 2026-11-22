package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = AmberGold,
    onPrimary = AmberInk,
    primaryContainer = CopperShadowDark,
    onPrimaryContainer = WarmCream,
    secondary = AmberGoldDeep,
    onSecondary = AmberInk,
    tertiary = EmberAccent,
    onTertiary = WarmWhite,
    background = ObsidianBg,
    onBackground = LightInk,
    surface = ObsidianSurface,
    onSurface = LightInk,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = MutedInk,
    outline = ObsidianOutline,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = AmberGoldDeep,
    onPrimary = WarmWhite,
    primaryContainer = CopperShadow,
    onPrimaryContainer = WarmCream,
    secondary = AmberGold,
    onSecondary = AmberInk,
    tertiary = EmberAccentDark,
    onTertiary = WarmWhite,
    background = StonePaperBg,
    onBackground = CharcoalInk,
    surface = WarmWhite,
    onSurface = CharcoalInk,
    surfaceVariant = StoneSurfaceVariant,
    onSurfaceVariant = CharcoalMuted,
    outline = StoneOutline,
  )

@Composable
fun TapCounterTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
