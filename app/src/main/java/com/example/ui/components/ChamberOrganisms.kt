package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Living undulating blind segmented Leech-Clerks (up to 6) clinging around the edges of the
 * recessed counter glass plate, with anterior pale suction mouths pressed against the glass.
 * Outer canvas margin guarantees full 7-segment bodies & tapered tails are never clipped.
 */
@Composable
internal fun LeechClerksGlassOverlay(
  leechCount: Int,
  breathPhaseProvider: () -> Float,
  modifier: Modifier = Modifier,
) {
  if (leechCount <= 0) return

  val bodyDark = MaterialTheme.colorScheme.secondaryContainer
  val bodyMid = MaterialTheme.colorScheme.secondary
  val highlightColor = MaterialTheme.colorScheme.primary
  val mouthColor = MaterialTheme.colorScheme.tertiary
  val shadowColor = MaterialTheme.colorScheme.background
  val visibleLeeches = min(leechCount, 6)

  Canvas(modifier = modifier.testTag("leech_clerks_overlay")) {
    val breath = breathPhaseProvider()

    for (i in 0 until visibleLeeches) {
      val onLeft = i % 2 == 0
      val row = i / 2
      val phaseAngle = breath * 2.0 * PI + i * 1.15
      val crawl = sin(phaseAngle).toFloat()

      // Anchored along the recessed counter glass bezel (24dp outer clearance keeps tails 100% inside)
      val headX =
        if (onLeft) 27.dp.toPx() + crawl * 2.0.dp.toPx()
        else size.width - 27.dp.toPx() - crawl * 2.0.dp.toPx()
      val baseY = size.height * (0.25f + row * 0.25f)

      // 7 tapered undulating annelid segments trailing cleanly outward without clipping
      val segmentCount = 7
      for (seg in (segmentCount - 1) downTo 0) {
        val dir = if (onLeft) -1f else 1f
        val segWave = sin(phaseAngle - seg * 0.62).toFloat() * (1.7f + seg * 0.24f).dp.toPx()
        val sx = headX + dir * seg * 3.0.dp.toPx()
        val sy = baseY + segWave
        val radius =
          when {
            seg == 0 -> 3.9.dp.toPx()
            seg <= 3 -> (4.3f - seg * 0.24f).dp.toPx()
            else -> (3.4f - (seg - 3) * 0.58f).dp.toPx().coerceAtLeast(1.5.dp.toPx())
          }

        drawCircle(
          color = shadowColor.copy(alpha = 0.88f),
          radius = radius + 1.1.dp.toPx(),
          center = Offset(sx, sy + 0.8.dp.toPx()),
        )
        drawCircle(
          color = if (seg % 2 == 0) bodyMid.copy(alpha = 0.94f) else bodyDark.copy(alpha = 0.96f),
          radius = radius,
          center = Offset(sx, sy),
        )
        // Damp dorsal ridge specular glint
        drawCircle(
          color = highlightColor.copy(alpha = 0.32f),
          radius = radius * 0.34f,
          center = Offset(sx - radius * 0.22f, sy - radius * 0.32f),
        )
      }

      // Pulsing pale fleshy anterior suction mouth pressed directly onto the counter glass
      val mouthPulse = 1f + 0.14f * sin(phaseAngle * 2.0).toFloat()
      val mouthCenter = Offset(headX + (if (onLeft) 2.2.dp.toPx() else -2.2.dp.toPx()), baseY)
      drawCircle(
        color = mouthColor.copy(alpha = 0.65f),
        radius = 4.5.dp.toPx() * mouthPulse,
        center = mouthCenter,
      )
      drawCircle(
        color = shadowColor.copy(alpha = 0.92f),
        radius = 1.9.dp.toPx() * mouthPulse,
        center = mouthCenter,
      )
      drawCircle(
        color = highlightColor.copy(alpha = 0.55f),
        radius = 4.5.dp.toPx() * mouthPulse,
        center = mouthCenter,
        style = Stroke(width = 0.9.dp.toPx()),
      )
    }
  }
}

/**
 * Suspended organic pear/bell glass Weeping Lamps (up to 5) hanging from the chamber ceiling.
 * Dormant lamps sway gently in dim grey; when Stillness activates after 3s idle, their vessels
 * illuminate with sickly pale-lichen glow and weep strictly downward-falling condensation droplets.
 */
@Composable
internal fun WeepingLampsCeilingOverlay(
  lampCount: Int,
  isStillnessActive: Boolean,
  breathPhaseProvider: () -> Float,
  dropPhaseProvider: () -> Float,
  modifier: Modifier = Modifier,
) {
  if (lampCount <= 0) return

  val colorScheme = MaterialTheme.colorScheme
  val visibleLamps = min(lampCount, 5)
  val cordPath = remember { Path() }
  val vesselPath = remember { Path() }
  val dropPath = remember { Path() }

  Canvas(modifier = modifier.fillMaxSize().testTag("weeping_lamps_overlay")) {
    val breath = breathPhaseProvider()
    val dropPhase = dropPhaseProvider()
    val spacing = size.width / (visibleLamps + 1)

    for (i in 1..visibleLamps) {
      val anchorX = spacing * i
      val sway = sin(breath * 2.0 * PI + i * 0.95).toFloat() * 6.5.dp.toPx()
      val cordLen = (24f + (i % 2) * 8f).dp.toPx()
      val socketCenter = Offset(anchorX + sway, cordLen)

      // Ceiling iron hook + slightly curved swaying braided cord
      drawCircle(
        color = colorScheme.outline,
        radius = 2.2.dp.toPx(),
        center = Offset(anchorX, 2.dp.toPx()),
      )
      cordPath.rewind()
      cordPath.moveTo(anchorX, 2.dp.toPx())
      cordPath.quadraticTo(anchorX + sway * 0.35f, cordLen * 0.55f, socketCenter.x, socketCenter.y)
      drawPath(cordPath, colorScheme.background.copy(alpha = 0.85f), style = Stroke(2.4.dp.toPx()))
      drawPath(cordPath, colorScheme.outline.copy(alpha = 0.90f), style = Stroke(1.2.dp.toPx()))

      // Luminous sickly-lichen aura when weeping under Stillness
      if (isStillnessActive) {
        val haloRadius = (26f + breath * 5f).dp.toPx()
        val haloCenter = Offset(socketCenter.x, socketCenter.y + 12.dp.toPx())
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(colorScheme.primary.copy(alpha = 0.32f), Color.Transparent),
              center = haloCenter,
              radius = haloRadius,
            ),
          radius = haloRadius,
          center = haloCenter,
        )
      }

      // Tarnished socket collar
      val collarTopLeft = Offset(socketCenter.x - 4.dp.toPx(), socketCenter.y - 1.5.dp.toPx())
      val collarSize = Size(8.dp.toPx(), 4.5.dp.toPx())
      drawRect(color = colorScheme.surfaceVariant, topLeft = collarTopLeft, size = collarSize)
      drawRect(
        color = colorScheme.outline,
        topLeft = collarTopLeft,
        size = collarSize,
        style = Stroke(width = 0.9.dp.toPx()),
      )

      // Organic bell/pear glass vessel
      val vesselTop = socketCenter.y + 3.dp.toPx()
      val vesselBottom = vesselTop + 19.dp.toPx()
      vesselPath.rewind()
      vesselPath.moveTo(socketCenter.x - 3.4.dp.toPx(), vesselTop)
      vesselPath.cubicTo(
        socketCenter.x - 9.dp.toPx(),
        vesselTop + 6.dp.toPx(),
        socketCenter.x - 9.5.dp.toPx(),
        vesselBottom - 2.dp.toPx(),
        socketCenter.x,
        vesselBottom,
      )
      vesselPath.cubicTo(
        socketCenter.x + 9.5.dp.toPx(),
        vesselBottom - 2.dp.toPx(),
        socketCenter.x + 9.dp.toPx(),
        vesselTop + 6.dp.toPx(),
        socketCenter.x + 3.4.dp.toPx(),
        vesselTop,
      )
      vesselPath.close()

      drawPath(
        path = vesselPath,
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                (if (isStillnessActive) colorScheme.primary else colorScheme.secondary).copy(
                  alpha = if (isStillnessActive) 0.78f else 0.42f
                ),
                colorScheme.surfaceVariant.copy(alpha = 0.92f),
              ),
            center = Offset(socketCenter.x - 1.5.dp.toPx(), vesselTop + 9.dp.toPx()),
            radius = 12.dp.toPx(),
          ),
      )

      // Internal fluid meniscus + vertical filament
      drawLine(
        color =
          if (isStillnessActive) colorScheme.onBackground.copy(alpha = 0.75f)
          else colorScheme.outline.copy(alpha = 0.60f),
        start = Offset(socketCenter.x, vesselTop + 1.5.dp.toPx()),
        end = Offset(socketCenter.x, vesselTop + 11.dp.toPx()),
        strokeWidth = 1.dp.toPx(),
      )
      drawPath(
        path = vesselPath,
        color =
          if (isStillnessActive) colorScheme.primary.copy(alpha = 0.88f)
          else colorScheme.outline.copy(alpha = 0.80f),
        style = Stroke(width = 1.dp.toPx()),
      )

      // Strictly downward-falling teardrop condensation droplets during Stillness
      if (isStillnessActive) {
        val dropProgress = (dropPhase + i * 0.27f) % 1f
        // Swelling bead hanging under glass tip before detaching (0.0..0.18), then gravity fall
        val fallNorm = ((dropProgress - 0.16f) / 0.84f).coerceIn(0f, 1f)
        val gravityCurve = fallNorm * fallNorm
        val dropY = vesselBottom + 2.dp.toPx() + gravityCurve * 30.dp.toPx()
        val swellScale =
          if (dropProgress < 0.16f) 0.55f + (dropProgress / 0.16f) * 0.45f else 1f
        val stretchY = 1f + fallNorm * 0.35f
        val dropAlpha =
          if (dropProgress < 0.16f) 0.55f + (dropProgress / 0.16f) * 0.35f
          else ((1f - fallNorm * 0.85f).coerceIn(0.10f, 0.90f))

        val halfW = 2.4.dp.toPx() * swellScale
        val topOffset = 3.8.dp.toPx() * swellScale * stretchY
        val botOffset = 2.4.dp.toPx() * swellScale
        dropPath.rewind()
        dropPath.moveTo(socketCenter.x, dropY - topOffset)
        dropPath.quadraticTo(
          socketCenter.x - halfW,
          dropY + botOffset * 0.45f,
          socketCenter.x,
          dropY + botOffset,
        )
        dropPath.quadraticTo(
          socketCenter.x + halfW,
          dropY + botOffset * 0.45f,
          socketCenter.x,
          dropY - topOffset,
        )
        dropPath.close()
        drawPath(dropPath, colorScheme.primary.copy(alpha = dropAlpha))
      }
    }
  }
}
