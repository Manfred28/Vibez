package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Bundled monospaced font family reinforcing the cold, clinical, existential atmosphere defined in
 * `/GAME_DESIGN_BIBLE.md`.
 */
val BleakMonoFontFamily =
  FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono, FontWeight.Bold),
  )

val BleakTypography =
  Typography(
    displayLarge =
      TextStyle(
        fontFamily = BleakMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 62.sp,
        lineHeight = 68.sp,
        letterSpacing = (-2.0).sp,
        fontFeatureSettings = "tnum",
      ),
    displayMedium =
      TextStyle(
        fontFamily = BleakMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        letterSpacing = (-1.0).sp,
        fontFeatureSettings = "tnum",
      ),
    headlineLarge =
      TextStyle(
        fontFamily = BleakMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = 1.0.sp,
        fontFeatureSettings = "tnum",
      ),
    headlineMedium =
      TextStyle(
        fontFamily = BleakMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.5.sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = BleakMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 1.2.sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = BleakMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 1.0.sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = BleakMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.35.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = BleakMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.3.sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = BleakMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 2.6.sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = BleakMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 2.0.sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = BleakMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.8.sp,
      ),
  )
