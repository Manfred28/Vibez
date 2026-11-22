package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.OrganGameState
import com.example.ui.theme.BleakTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Phase-mutating chamber wall backdrop (Phases 0..4):
 * - Phase 0: Cold, sterile fluorescent slate tile with intact crisp grout lines.
 * - Phase 1: Damp cracked asylum masonry surfaces with corner seepage & mortar fractures.
 * - Phase 2: Sickly olive-lichen wash, vertical condensation streaks, and pulsing veins.
 * - Phase 3: Inward-buckling curved tile grid & split bruised corner tissue pockets.
 * - Phase 4: Encroaching chitinous rib-like arches curving inward over arterial fissures.
 */
@Composable
internal fun ChamberWallBackdrop(
  state: OrganGameState,
  breathPhaseProvider: () -> Float,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val atmosphere = BleakTheme.atmosphere

  val wallArtAlpha =
    when (state.phase) {
      0 -> 0.26f
      1 -> 0.66f
      2 -> 0.72f
      3 -> 0.78f
      else -> 0.84f
    }

  val wallColorFilter =
    remember(state.phase) {
      when (state.phase) {
        0 ->
          ColorFilter.colorMatrix(
            ColorMatrix(
              floatArrayOf(
                0.78f, 0f, 0f, 0f, 0f,
                0f, 0.84f, 0f, 0f, 2f,
                0f, 0f, 0.92f, 0f, 6f,
                0f, 0f, 0f, 1f, 0f,
              )
            )
          )
        1 -> null
        2 ->
          ColorFilter.colorMatrix(
            ColorMatrix(
              floatArrayOf(
                0.96f, 0f, 0f, 0f, 2f,
                0f, 1.05f, 0f, 0f, 4f,
                0f, 0f, 0.82f, 0f, -4f,
                0f, 0f, 0f, 1f, 0f,
              )
            )
          )
        3 ->
          ColorFilter.colorMatrix(
            ColorMatrix(
              floatArrayOf(
                1.08f, 0f, 0f, 0f, 6f,
                0f, 0.82f, 0f, 0f, -4f,
                0f, 0f, 0.98f, 0f, 6f,
                0f, 0f, 0f, 1f, 0f,
              )
            )
          )
        else ->
          ColorFilter.colorMatrix(
            ColorMatrix(
              floatArrayOf(
                1.18f, 0f, 0f, 0f, 10f,
                0f, 0.72f, 0f, 0f, -8f,
                0f, 0f, 0.76f, 0f, -6f,
                0f, 0f, 0f, 1f, 0f,
              )
            )
          )
      }
    }

  val scratchPath = remember { Path() }

  Box(modifier = modifier.fillMaxSize()) {
    Image(
      painter = painterResource(id = R.drawable.img_wall_phase1),
      contentDescription = stringResource(R.string.art_wall_phase1_cd),
      contentScale = ContentScale.Crop,
      colorFilter = wallColorFilter,
      modifier = Modifier.fillMaxSize().alpha(wallArtAlpha).testTag("chamber_wall_art"),
    )

    Canvas(modifier = Modifier.fillMaxSize().testTag("chamber_wall_phase_${state.phase}")) {
      val breath = breathPhaseProvider()
      val valveCenter = Offset(size.width * 0.5f, size.height * 0.62f)

      val midTint =
        when (state.phase) {
          0 -> colorScheme.background.copy(alpha = 0.42f)
          1 -> colorScheme.background.copy(alpha = 0.30f)
          2 -> colorScheme.secondaryContainer.copy(alpha = 0.36f)
          3 -> colorScheme.tertiaryContainer.copy(alpha = 0.42f)
          else -> colorScheme.tertiaryContainer.copy(alpha = 0.52f)
        }
      drawRect(
        brush =
          Brush.verticalGradient(
            colors =
              listOf(
                colorScheme.background.copy(alpha = 0.78f),
                midTint,
                colorScheme.background.copy(alpha = 0.72f),
              )
          )
      )

      if (state.phase == 0) {
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(colorScheme.onBackground.copy(alpha = 0.08f), Color.Transparent),
              center = Offset(size.width * 0.5f, 0f),
              radius = size.width * 0.65f,
            ),
          radius = size.width * 0.65f,
          center = Offset(size.width * 0.5f, 0f),
        )
      }

      val tileStep = 38.dp.toPx()
      val tileLineColor =
        if (state.phase >= 2) {
          colorScheme.tertiary.copy(alpha = 0.14f + state.phase * 0.04f)
        } else {
          atmosphere.hairline.copy(alpha = 0.24f)
        }
      val warpStrength =
        when {
          state.phase >= 4 -> 18.dp.toPx() + breath * 5.dp.toPx()
          state.phase == 3 -> 10.dp.toPx() + breath * 3.dp.toPx()
          else -> 0f
        }
      val warpStroke = Stroke(width = 1.1f)

      var x = tileStep
      while (x < size.width) {
        if (warpStrength > 0f) {
          val pull = ((valveCenter.x - x) / size.width) * warpStrength * 2.2f
          scratchPath.rewind()
          scratchPath.moveTo(x, 0f)
          scratchPath.quadraticTo(x + pull, valveCenter.y, x, size.height)
          drawPath(scratchPath, tileLineColor, style = warpStroke)
        } else {
          drawLine(tileLineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        }
        x += tileStep
      }
      var y = tileStep
      while (y < size.height) {
        if (warpStrength > 0f) {
          val pull = ((valveCenter.y - y) / size.height) * warpStrength * 1.8f
          scratchPath.rewind()
          scratchPath.moveTo(0f, y)
          scratchPath.quadraticTo(valveCenter.x, y + pull, size.width, y)
          drawPath(scratchPath, tileLineColor, style = warpStroke)
        } else {
          drawLine(tileLineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        y += tileStep
      }

      if (state.phase >= 1) {
        val stainRadius = size.minDimension * (0.28f + state.phase * 0.05f)
        val stainColor =
          if (state.phase >= 3) colorScheme.tertiaryContainer.copy(alpha = 0.42f)
          else colorScheme.background.copy(alpha = 0.48f)
        drawCircle(
          brush = Brush.radialGradient(listOf(stainColor, Color.Transparent), Offset(0f, size.height), stainRadius),
          radius = stainRadius,
          center = Offset(0f, size.height),
        )
        drawCircle(
          brush =
            Brush.radialGradient(
              listOf(stainColor, Color.Transparent),
              Offset(size.width, size.height * 0.85f),
              stainRadius,
            ),
          radius = stainRadius,
          center = Offset(size.width, size.height * 0.85f),
        )
      }

      if (state.phase >= 2) {
        val streakColor = colorScheme.primary.copy(alpha = 0.16f)
        for (s in 1..5) {
          val sx = size.width * (s / 6f)
          val topY = size.height * (0.12f + (s % 3) * 0.14f)
          val len = size.height * (0.24f + breath * 0.08f)
          drawLine(
            streakColor,
            Offset(sx, topY),
            Offset(sx, topY + len),
            strokeWidth = 1.2.dp.toPx(),
            cap = StrokeCap.Round,
          )
          drawCircle(
            colorScheme.primary.copy(alpha = 0.24f),
            radius = 1.8.dp.toPx(),
            center = Offset(sx, topY + len),
          )
        }
      }

      if (state.phase >= 3) {
        val cornerColor = colorScheme.tertiary.copy(alpha = 0.26f + breath * 0.08f)
        val pocketOutline = colorScheme.outline.copy(alpha = 0.65f)
        val pocketStroke = Stroke(1.2.dp.toPx())
        val pocketSize = 46.dp.toPx()

        scratchPath.rewind()
        scratchPath.moveTo(0f, size.height * 0.36f)
        scratchPath.quadraticTo(pocketSize * 0.85f, size.height * 0.44f, 0f, size.height * 0.56f)
        scratchPath.close()
        drawPath(scratchPath, cornerColor)
        drawPath(scratchPath, pocketOutline, style = pocketStroke)

        scratchPath.rewind()
        scratchPath.moveTo(size.width, size.height * 0.42f)
        scratchPath.quadraticTo(
          size.width - pocketSize * 0.85f,
          size.height * 0.50f,
          size.width,
          size.height * 0.64f,
        )
        scratchPath.close()
        drawPath(scratchPath, cornerColor)
        drawPath(scratchPath, pocketOutline, style = pocketStroke)
      }

      if (state.phase >= 4) {
        val ribColor = colorScheme.onSurfaceVariant.copy(alpha = 0.34f + breath * 0.10f)
        val ribShadow = colorScheme.background.copy(alpha = 0.78f)
        val shadowStroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        val boneStroke = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
        for (r in 0..3) {
          val baseY = size.height * (0.22f + r * 0.20f)
          val reach =
            size.width * (0.20f + (if (r == 1 || r == 2) 0.07f else 0f) + breath * 0.02f)

          scratchPath.rewind()
          scratchPath.moveTo(0f, baseY - 12.dp.toPx())
          scratchPath.quadraticTo(reach, baseY, 0f, baseY + 12.dp.toPx())
          drawPath(scratchPath, ribShadow, style = shadowStroke)
          drawPath(scratchPath, ribColor, style = boneStroke)

          scratchPath.rewind()
          scratchPath.moveTo(size.width, baseY - 12.dp.toPx())
          scratchPath.quadraticTo(size.width - reach, baseY, size.width, baseY + 12.dp.toPx())
          drawPath(scratchPath, ribShadow, style = shadowStroke)
          drawPath(scratchPath, ribColor, style = boneStroke)
        }
      }

      if (state.scarTissue > 0L) {
        val seamCount = min(state.scarTissue.toInt(), 4)
        val scarColor = colorScheme.onBackground.copy(alpha = 0.36f)
        for (s in 0 until seamCount) {
          val startY = size.height * (0.14f + s * 0.22f)
          val endY = startY + size.height * 0.16f
          drawLine(
            scarColor,
            Offset(0f, startY),
            Offset(size.width, endY),
            strokeWidth = 1.5.dp.toPx(),
          )
          for (st in 1 until 8) {
            val t = st / 8f
            val sx = size.width * t
            val sy = startY + (endY - startY) * t
            drawLine(
              scarColor,
              Offset(sx - 3.dp.toPx(), sy - 5.dp.toPx()),
              Offset(sx + 3.dp.toPx(), sy + 5.dp.toPx()),
              strokeWidth = 1.1.dp.toPx(),
            )
          }
        }
      }

      if (state.phase >= 1) {
        val fissureCount = state.phase * 2 + 2 + state.graftedTendrils.coerceAtMost(4)
        val veinColor =
          if (state.phase >= 2) {
            colorScheme.tertiary.copy(alpha = 0.24f + atmosphere.distortionFactor * 0.30f)
          } else {
            colorScheme.outline.copy(alpha = 0.56f)
          }
        val fissureStroke =
          Stroke(width = (1.3f + state.phase * 0.45f).dp.toPx(), cap = StrokeCap.Round)
        for (i in 0 until fissureCount) {
          val angle = (i * (2.0 * PI / fissureCount)) + (breath - 0.5) * 0.08
          val reach = size.minDimension * (0.36f + state.phase * 0.11f + breath * 0.03f)
          val mid =
            Offset(
              valveCenter.x + cos(angle + 0.14).toFloat() * (reach * 0.52f),
              valveCenter.y + sin(angle - 0.12).toFloat() * (reach * 0.52f),
            )
          val end =
            Offset(
              valveCenter.x + cos(angle).toFloat() * reach,
              valveCenter.y + sin(angle).toFloat() * reach,
            )
          scratchPath.rewind()
          scratchPath.moveTo(valveCenter.x, valveCenter.y)
          scratchPath.quadraticTo(mid.x, mid.y, end.x, end.y)
          drawPath(path = scratchPath, color = veinColor, style = fissureStroke)
        }
      }

      drawExcisionChamberMutations(
        state = state,
        breath = breath,
        primaryColor = colorScheme.primary,
        tertiaryColor = colorScheme.tertiary,
        outlineColor = colorScheme.outline,
        backgroundColor = colorScheme.background,
        scratchPath = scratchPath,
      )
    }
  }
}
