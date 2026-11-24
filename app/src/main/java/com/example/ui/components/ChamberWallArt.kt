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
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Phase-mutating chamber wall backdrop (Phases 0..4):
 * - Phase 0: Cold, sterile fluorescent slate tile with intact crisp grout lines.
 * - Phase 1: Damp cracked asylum masonry surfaces with subtle corner seepage & fractures.
 * - Phase 2: Subtle fleshy wall texture emerges beneath cracking tiles with nascent capillary veins.
 * - Phase 3: Living animated wall — peristaltic grout contractions, creeping seam undulations,
 *            weeping pore seepage crawling down thickened vascular fissures.
 * - Phase 4: Deep fleshy membrane with systolic/diastolic arterial heartbeat pulses radiating
 *            outward from the valve across swollen branching veins & encroaching rib arches.
 */
@Composable
internal fun ChamberWallBackdrop(
  state: OrganGameState,
  breathPhaseProvider: () -> Float,
  wallCreepPhaseProvider: () -> Float,
  arterialPulsePhaseProvider: () -> Float,
  echoRippleProvider: () -> Float,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val atmosphere = BleakTheme.atmosphere

  val tileWallAlpha =
    when (state.phase) {
      0 -> 0.24f
      1 -> 0.56f
      2 -> 0.52f
      3 -> 0.38f
      else -> 0.22f
    }

  val fleshyWallAlpha =
    when (state.phase) {
      0, 1 -> 0f
      2 -> 0.28f
      3 -> 0.56f
      else -> 0.80f
    }

  val wallColorFilter =
    remember(state.phase) {
      when (state.phase) {
        0 ->
          ColorFilter.colorMatrix(
            ColorMatrix(
              floatArrayOf(
                0.82f, 0f, 0f, 0f, 0f,
                0f, 0.86f, 0f, 0f, 1f,
                0f, 0f, 0.92f, 0f, 4f,
                0f, 0f, 0f, 1f, 0f,
              )
            )
          )
        1 -> null
        2 ->
          ColorFilter.colorMatrix(
            ColorMatrix(
              floatArrayOf(
                0.96f, 0f, 0f, 0f, 1f,
                0f, 1.01f, 0f, 0f, 2f,
                0f, 0f, 0.88f, 0f, -2f,
                0f, 0f, 0f, 1f, 0f,
              )
            )
          )
        3 ->
          ColorFilter.colorMatrix(
            ColorMatrix(
              floatArrayOf(
                1.04f, 0f, 0f, 0f, 3f,
                0f, 0.88f, 0f, 0f, -2f,
                0f, 0f, 0.94f, 0f, 2f,
                0f, 0f, 0f, 1f, 0f,
              )
            )
          )
        else ->
          ColorFilter.colorMatrix(
            ColorMatrix(
              floatArrayOf(
                1.08f, 0f, 0f, 0f, 5f,
                0f, 0.82f, 0f, 0f, -4f,
                0f, 0f, 0.84f, 0f, -3f,
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
      modifier = Modifier.fillMaxSize().alpha(tileWallAlpha).testTag("chamber_wall_art"),
    )

    if (fleshyWallAlpha > 0f) {
      Image(
        painter = painterResource(id = R.drawable.img_wall_fleshy),
        contentDescription = stringResource(R.string.art_wall_fleshy_cd),
        contentScale = ContentScale.Crop,
        colorFilter = wallColorFilter,
        modifier = Modifier.fillMaxSize().alpha(fleshyWallAlpha).testTag("chamber_wall_fleshy_art"),
      )
    }

    Canvas(modifier = Modifier.fillMaxSize().testTag("chamber_wall_phase_${state.phase}")) {
      val breath = breathPhaseProvider()
      val creep = wallCreepPhaseProvider()
      val arterialCycle = arterialPulsePhaseProvider()
      val echoRipple = echoRippleProvider()
      val valveCenter = Offset(size.width * 0.5f, size.height * 0.62f)

      // Phase 4 double-beat cardiac impulse ("lub-dub" systolic + diastolic peak)
      val doubleBeat =
        if (state.phase >= 4) {
          val lub = exp(-((arterialCycle - 0.18f) * (arterialCycle - 0.18f)) / 0.0045f)
          val dub = exp(-((arterialCycle - 0.36f) * (arterialCycle - 0.36f)) / 0.0070f) * 0.68f
          (lub + dub).coerceIn(0f, 1f)
        } else {
          0f
        }

      val midTint =
        when (state.phase) {
          0 -> colorScheme.background.copy(alpha = 0.44f)
          1 -> colorScheme.background.copy(alpha = 0.34f)
          2 -> colorScheme.secondaryContainer.copy(alpha = 0.24f)
          3 -> colorScheme.tertiaryContainer.copy(alpha = 0.28f)
          else -> colorScheme.tertiaryContainer.copy(alpha = 0.34f + doubleBeat * 0.10f)
        }
      drawRect(
        brush =
          Brush.verticalGradient(
            colors =
              listOf(
                colorScheme.background.copy(alpha = 0.74f),
                midTint,
                colorScheme.background.copy(alpha = 0.68f),
              )
          )
      )

      if (state.phase == 0) {
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(colorScheme.onBackground.copy(alpha = 0.06f), Color.Transparent),
              center = Offset(size.width * 0.5f, 0f),
              radius = size.width * 0.65f,
            ),
          radius = size.width * 0.65f,
          center = Offset(size.width * 0.5f, 0f),
        )
      }

      // Tile grout grid with Phase 3+ living peristaltic undulation
      val tileStep = 38.dp.toPx()
      val tileLineColor =
        if (state.phase >= 2) {
          colorScheme.tertiary.copy(alpha = 0.12f + state.phase * 0.035f + doubleBeat * 0.08f)
        } else {
          atmosphere.hairline.copy(alpha = 0.22f)
        }
      val warpStrength =
        when {
          state.phase >= 4 -> 17.dp.toPx() + breath * 4.dp.toPx() + doubleBeat * 5.dp.toPx()
          state.phase == 3 -> 10.dp.toPx() + breath * 3.dp.toPx()
          else -> 0f
        }
      val warpStroke = Stroke(width = 1.1f)

      var x = tileStep
      var colIdx = 0
      while (x < size.width) {
        if (warpStrength > 0f) {
          val peristalsis =
            if (state.phase >= 3) sin(creep * 2.0 * PI + colIdx * 0.85).toFloat() * 4.5.dp.toPx()
            else 0f
          val pull = ((valveCenter.x - x) / size.width) * warpStrength * 2.1f + peristalsis
          scratchPath.rewind()
          scratchPath.moveTo(x, 0f)
          scratchPath.quadraticTo(x + pull, valveCenter.y + peristalsis * 0.6f, x, size.height)
          drawPath(scratchPath, tileLineColor, style = warpStroke)
        } else {
          drawLine(tileLineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        }
        x += tileStep
        colIdx++
      }
      var y = tileStep
      var rowIdx = 0
      while (y < size.height) {
        if (warpStrength > 0f) {
          val peristalsis =
            if (state.phase >= 3) cos(creep * 2.0 * PI + rowIdx * 0.95).toFloat() * 3.5.dp.toPx()
            else 0f
          val pull = ((valveCenter.y - y) / size.height) * warpStrength * 1.7f + peristalsis
          scratchPath.rewind()
          scratchPath.moveTo(0f, y)
          scratchPath.quadraticTo(valveCenter.x, y + pull, size.width, y)
          drawPath(scratchPath, tileLineColor, style = warpStroke)
        } else {
          drawLine(tileLineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        y += tileStep
        rowIdx++
      }

      if (state.phase >= 1) {
        val stainRadius = size.minDimension * (0.26f + state.phase * 0.04f)
        val stainColor =
          if (state.phase >= 3) colorScheme.tertiaryContainer.copy(alpha = 0.34f)
          else colorScheme.background.copy(alpha = 0.42f)
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

      // Phase 2+: Condensation streaks + Phase 3 animated crawling seepage droplets
      if (state.phase >= 2) {
        val streakColor = colorScheme.primary.copy(alpha = 0.14f)
        for (s in 1..5) {
          val sx = size.width * (s / 6f)
          val topY = size.height * (0.12f + (s % 3) * 0.14f)
          val creepDrop =
            if (state.phase >= 3) ((creep + s * 0.21f) % 1f) * size.height * 0.28f
            else breath * size.height * 0.06f
          val len = size.height * 0.18f + creepDrop
          drawLine(
            streakColor,
            Offset(sx, topY),
            Offset(sx, (topY + len).coerceAtMost(size.height)),
            strokeWidth = 1.15.dp.toPx(),
            cap = StrokeCap.Round,
          )
          drawCircle(
            colorScheme.primary.copy(alpha = 0.22f),
            radius = 1.7.dp.toPx(),
            center = Offset(sx, (topY + len).coerceAtMost(size.height)),
          )
        }
      }

      // Phase 3+: Living bruised tissue pockets contracting & creeping pores along the flanks
      if (state.phase >= 3) {
        val leftContract = sin(creep * 2.0 * PI).toFloat() * 7.dp.toPx()
        val rightContract = cos(creep * 2.0 * PI + 0.7).toFloat() * 7.dp.toPx()
        val cornerColor = colorScheme.tertiary.copy(alpha = 0.24f + breath * 0.08f + doubleBeat * 0.12f)
        val pocketOutline = colorScheme.outline.copy(alpha = 0.62f)
        val pocketStroke = Stroke(1.2.dp.toPx())
        val pocketSize = 44.dp.toPx()

        scratchPath.rewind()
        scratchPath.moveTo(0f, size.height * 0.34f)
        scratchPath.quadraticTo(
          pocketSize * 0.88f + leftContract,
          size.height * 0.44f,
          0f,
          size.height * 0.58f,
        )
        scratchPath.close()
        drawPath(scratchPath, cornerColor)
        drawPath(scratchPath, pocketOutline, style = pocketStroke)

        scratchPath.rewind()
        scratchPath.moveTo(size.width, size.height * 0.40f)
        scratchPath.quadraticTo(
          size.width - pocketSize * 0.88f - rightContract,
          size.height * 0.50f,
          size.width,
          size.height * 0.64f,
        )
        scratchPath.close()
        drawPath(scratchPath, cornerColor)
        drawPath(scratchPath, pocketOutline, style = pocketStroke)

        // Subcutaneous peristaltic swell nodes traveling down the fleshy seams
        for (p in 0 until 4) {
          val t = (creep + p * 0.25f) % 1f
          val px = if (p % 2 == 0) size.width * (0.14f + sin(t * PI).toFloat() * 0.05f)
            else size.width * (0.86f - sin(t * PI).toFloat() * 0.05f)
          val py = size.height * (0.18f + t * 0.66f)
          val r = (3.5f + sin(t * PI).toFloat() * 3.0f).dp.toPx()
          drawCircle(
            color = colorScheme.tertiary.copy(alpha = 0.22f * sin(t * PI).toFloat()),
            radius = r,
            center = Offset(px, py),
          )
        }
      }

      // Phase 4: Deep arterial double-beat pulse wave expanding outward from the valve
      if (state.phase >= 4) {
        val waveRadius = size.minDimension * (0.12f + arterialCycle * 0.68f)
        val waveAlpha = ((1f - arterialCycle) * (0.28f + doubleBeat * 0.22f)).coerceIn(0f, 0.46f)
        drawCircle(
          color = colorScheme.tertiary.copy(alpha = waveAlpha),
          radius = waveRadius,
          center = valveCenter,
          style = Stroke(width = (2.6f + doubleBeat * 2.2f).dp.toPx()),
        )
        if (doubleBeat > 0.08f) {
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    colorScheme.tertiary.copy(alpha = doubleBeat * 0.22f),
                    Color.Transparent,
                  ),
                center = valveCenter,
                radius = size.minDimension * 0.56f,
              ),
            radius = size.minDimension * 0.56f,
            center = valveCenter,
          )
        }

        // Encroaching calcified rib arches pulsing with the arterial beat
        val ribColor = colorScheme.onSurfaceVariant.copy(alpha = 0.30f + doubleBeat * 0.14f)
        val ribShadow = colorScheme.background.copy(alpha = 0.72f)
        val shadowStroke = Stroke(width = 5.5.dp.toPx(), cap = StrokeCap.Round)
        val boneStroke = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        for (r in 0..3) {
          val baseY = size.height * (0.22f + r * 0.20f)
          val reach =
            size.width * (0.18f + (if (r == 1 || r == 2) 0.06f else 0f) + doubleBeat * 0.025f)

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
        val scarColor = colorScheme.onBackground.copy(alpha = 0.32f)
        for (s in 0 until seamCount) {
          val startY = size.height * (0.14f + s * 0.22f)
          val endY = startY + size.height * 0.16f
          drawLine(
            scarColor,
            Offset(0f, startY),
            Offset(size.width, endY),
            strokeWidth = 1.4.dp.toPx(),
          )
          for (st in 1 until 8) {
            val t = st / 8f
            val sx = size.width * t
            val sy = startY + (endY - startY) * t
            drawLine(
              scarColor,
              Offset(sx - 3.dp.toPx(), sy - 5.dp.toPx()),
              Offset(sx + 3.dp.toPx(), sy + 5.dp.toPx()),
              strokeWidth = 1.0.dp.toPx(),
            )
          }
        }
      }

      // Organic branching vascular network growing thicker from Phase 1 -> Phase 4
      if (state.phase >= 1) {
        val fissureCount = state.phase * 2 + 2 + state.graftedTendrils.coerceAtMost(4)
        val veinAlpha =
          if (state.phase >= 2) {
            (0.24f + atmosphere.distortionFactor * 0.32f + doubleBeat * 0.22f).coerceAtMost(0.88f)
          } else {
            0.52f
          }
        val veinColor =
          if (state.phase >= 2) colorScheme.tertiary.copy(alpha = veinAlpha)
          else colorScheme.outline.copy(alpha = veinAlpha)
        val baseWidth = (1.2f + state.phase * 0.48f + doubleBeat * 0.9f).dp.toPx()
        val fissureStroke = Stroke(width = baseWidth, cap = StrokeCap.Round)
        val branchStroke = Stroke(width = (baseWidth * 0.55f).coerceAtLeast(1f), cap = StrokeCap.Round)

        for (i in 0 until fissureCount) {
          val creepWiggle =
            if (state.phase >= 3) sin(creep * 2.0 * PI + i * 1.1) * 0.045 else 0.0
          val angle = (i * (2.0 * PI / fissureCount)) + (breath - 0.5) * 0.06 + creepWiggle
          val reach =
            size.minDimension * (0.34f + state.phase * 0.11f + breath * 0.025f + doubleBeat * 0.03f)
          val mid =
            Offset(
              valveCenter.x + cos(angle + 0.16).toFloat() * (reach * 0.52f),
              valveCenter.y + sin(angle - 0.14).toFloat() * (reach * 0.52f),
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

          // Secondary capillary fork in Phases 2-4
          if (state.phase >= 2) {
            val forkDir = if (i % 2 == 0) 0.42 else -0.42
            val forkEnd =
              Offset(
                mid.x + cos(angle + forkDir).toFloat() * (reach * 0.34f),
                mid.y + sin(angle + forkDir).toFloat() * (reach * 0.34f),
              )
            drawLine(
              color = veinColor.copy(alpha = veinAlpha * 0.72f),
              start = mid,
              end = forkEnd,
              strokeWidth = branchStroke.width,
              cap = StrokeCap.Round,
            )
          }
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

      // Neighbor Aperture Echo ripple expanding from the wall slit on echo pulse
      if (state.highestExcisionMilestone >= 3 && echoRipple in 0.001f..0.999f) {
        val slitOrigin = Offset(size.width * 0.11f, size.height * 0.41f)
        val rippleAlpha = (1f - echoRipple) * 0.65f
        val r1 = (12f + echoRipple * size.width * 0.55f)
        val r2 = (4f + echoRipple * size.width * 0.36f)
        drawCircle(
          color = colorScheme.primary.copy(alpha = rippleAlpha),
          radius = r1,
          center = slitOrigin,
          style = Stroke(width = 1.6.dp.toPx()),
        )
        drawCircle(
          color = colorScheme.primary.copy(alpha = rippleAlpha * 0.65f),
          radius = r2,
          center = slitOrigin,
          style = Stroke(width = 1.1.dp.toPx()),
        )
      }
    }
  }
}
