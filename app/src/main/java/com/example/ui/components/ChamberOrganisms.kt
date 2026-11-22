package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Rendered directly over the valve rim (non-clickable so touches pass straight to the valve).
 * Iterated anatomical design: shaded curved metacarpal palm & wrist silhouettes reaching out of
 * the cracked masonry, surgical cross-sutures with tiny dried-clot puncture dots, articulated
 * 2-jointed fingers with pale fingernail tips gripping and pressing the valve rim, plus braided
 * Grafted Choir-Tendrils.
 */
@Composable
internal fun SutureHandsValveOverlay(
  breathPhaseProvider: () -> Float,
  sutureCount: Int,
  graftCount: Int,
  boneColor: Color,
  stitchColor: Color,
  shadowColor: Color,
  modifier: Modifier = Modifier,
) {
  if (sutureCount <= 0 && graftCount <= 0) return

  val knuckleTone = MaterialTheme.colorScheme.onSurfaceVariant
  val nailColor = MaterialTheme.colorScheme.onBackground

  Canvas(modifier = modifier.size(228.dp).testTag("suture_hands_overlay")) {
    val breath = breathPhaseProvider()
    val center = Offset(size.width / 2f, size.height / 2f)
    val valveRimRadius = 64.dp.toPx()
    val armStartRadius = size.minDimension * 0.495f

    // Thick intertwined double-strand Grafted Choir-Tendrils arching onto the valve collar
    val visibleGrafts = min(graftCount, 4)
    for (g in 0 until visibleGrafts) {
      val angle = (g * (PI / 2.0)) + PI / 4.0 + (breath - 0.5) * 0.10
      val dirX = cos(angle).toFloat()
      val dirY = sin(angle).toFloat()
      val perpX = -dirY
      val perpY = dirX

      val start = Offset(center.x + dirX * armStartRadius, center.y + dirY * armStartRadius)
      val midR = (armStartRadius + valveRimRadius) * 0.52f
      val midA = Offset(center.x + dirX * midR + perpX * 8.5.dp.toPx(), center.y + dirY * midR + perpY * 8.5.dp.toPx())
      val midB = Offset(center.x + dirX * midR - perpX * 6.5.dp.toPx(), center.y + dirY * midR - perpY * 6.5.dp.toPx())
      val end = Offset(center.x + dirX * (valveRimRadius - 4.dp.toPx()), center.y + dirY * (valveRimRadius - 4.dp.toPx()))

      val primaryStrand =
        Path().apply {
          moveTo(start.x, start.y)
          quadraticTo(midA.x, midA.y, end.x, end.y)
        }
      val secondaryStrand =
        Path().apply {
          moveTo(start.x - perpX * 2.dp.toPx(), start.y - perpY * 2.dp.toPx())
          quadraticTo(midB.x, midB.y, end.x, end.y)
        }

      drawPath(primaryStrand, shadowColor.copy(alpha = 0.88f), style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
      drawPath(secondaryStrand, shadowColor.copy(alpha = 0.80f), style = Stroke(5.5.dp.toPx(), cap = StrokeCap.Round))
      drawPath(primaryStrand, stitchColor.copy(alpha = 0.92f), style = Stroke(4.2.dp.toPx(), cap = StrokeCap.Round))
      drawPath(secondaryStrand, knuckleTone.copy(alpha = 0.72f), style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round))
      drawCircle(color = shadowColor, radius = 4.6.dp.toPx(), center = end)
      drawCircle(color = boneColor.copy(alpha = 0.84f), radius = 3.1.dp.toPx(), center = end)
    }

    // Articulated shaded pale Suture-Hands gripping and rhythmically pressing the valve collar
    val visibleHands = min(sutureCount, 8)
    for (i in 0 until visibleHands) {
      val baseAngle = (i * (2.0 * PI / visibleHands)) - PI / 2.0 + (PI / 8.0)
      val dirX = cos(baseAngle).toFloat()
      val dirY = sin(baseAngle).toFloat()
      val perpX = -dirY
      val perpY = dirX

      val pressWave = ((sin(breath * 2.0 * PI + i * 1.05) + 1.0) * 0.5).toFloat()
      val pressInset = pressWave * 7.dp.toPx()

      val wristR = armStartRadius
      val palmR = valveRimRadius + 13.5.dp.toPx() - pressInset * 0.65f
      val tipR = valveRimRadius - 5.5.dp.toPx() - pressInset

      val wrist = Offset(center.x + dirX * wristR, center.y + dirY * wristR)
      val palm = Offset(center.x + dirX * palmR, center.y + dirY * palmR)
      val wristHalfW = 4.2.dp.toPx()
      val palmHalfW = 7.4.dp.toPx()

      // Curved metacarpal wrist-to-palm silhouette
      val palmPath =
        Path().apply {
          moveTo(wrist.x - perpX * wristHalfW, wrist.y - perpY * wristHalfW)
          quadraticTo(
            (wrist.x + palm.x) * 0.5f - perpX * (wristHalfW * 0.85f),
            (wrist.y + palm.y) * 0.5f - perpY * (wristHalfW * 0.85f),
            palm.x - perpX * palmHalfW,
            palm.y - perpY * palmHalfW,
          )
          quadraticTo(
            palm.x - dirX * 4.dp.toPx(),
            palm.y - dirY * 4.dp.toPx(),
            palm.x + perpX * palmHalfW,
            palm.y + perpY * palmHalfW,
          )
          quadraticTo(
            (wrist.x + palm.x) * 0.5f + perpX * (wristHalfW * 0.85f),
            (wrist.y + palm.y) * 0.5f + perpY * (wristHalfW * 0.85f),
            wrist.x + perpX * wristHalfW,
            wrist.y + perpY * wristHalfW,
          )
          close()
        }

      // Drop shadow & shaded anatomical pale flesh fill
      drawPath(palmPath, shadowColor.copy(alpha = 0.92f), style = Stroke(3.6.dp.toPx(), join = StrokeJoin.Round))
      drawPath(
        path = palmPath,
        brush =
          Brush.linearGradient(
            colors = listOf(knuckleTone.copy(alpha = 0.82f), boneColor.copy(alpha = 0.95f)),
            start = wrist,
            end = palm,
          ),
      )

      // Dark surgical cross-sutures across the stitched wrist + tiny dried-clot puncture dots
      for (stitchIdx in 1..3) {
        val t = stitchIdx * 0.24f
        val sx = wrist.x + (palm.x - wrist.x) * t
        val sy = wrist.y + (palm.y - wrist.y) * t
        val halfLen = (3.4f + stitchIdx * 0.5f).dp.toPx()
        val p1 = Offset(sx - perpX * halfLen, sy - perpY * halfLen)
        val p2 = Offset(sx + perpX * halfLen, sy + perpY * halfLen)
        drawLine(shadowColor.copy(alpha = 0.9f), p1, p2, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(stitchColor, p1, p2, strokeWidth = 1.1.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(stitchColor.copy(alpha = 0.9f), radius = 1.1.dp.toPx(), center = p1)
        drawCircle(stitchColor.copy(alpha = 0.9f), radius = 1.1.dp.toPx(), center = p2)
      }

      // Three articulated pale fingers with knuckle joints & pale fingernails gripping the rim
      val fingerSpreads = floatArrayOf(-6.2f, 0f, 6.2f)
      val fingerLengths = floatArrayOf(0.90f, 1.07f, 0.88f)
      for (f in fingerSpreads.indices) {
        val spreadPx = fingerSpreads[f].dp.toPx()
        val base = Offset(palm.x + perpX * (spreadPx * 0.68f), palm.y + perpY * (spreadPx * 0.68f))
        val knuckle =
          Offset(
            palm.x - dirX * 5.6.dp.toPx() + perpX * (spreadPx * 0.84f),
            palm.y - dirY * 5.6.dp.toPx() + perpY * (spreadPx * 0.84f),
          )
        val tipRadius = tipR - (if (f == 1) 2.6.dp.toPx() else 0f) + (1f - fingerLengths[f]) * 7.dp.toPx()
        val tip = Offset(center.x + dirX * tipRadius + perpX * (spreadPx * 0.52f), center.y + dirY * tipRadius + perpY * (spreadPx * 0.52f))

        val fingerPath =
          Path().apply {
            moveTo(base.x, base.y)
            lineTo(knuckle.x, knuckle.y)
            lineTo(tip.x, tip.y)
          }
        drawPath(fingerPath, shadowColor.copy(alpha = 0.92f), style = Stroke(4.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(fingerPath, boneColor.copy(alpha = 0.95f), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        // Joint knuckle bead + pale fingernail tip
        drawCircle(knuckleTone.copy(alpha = 0.82f), radius = 1.5.dp.toPx(), center = knuckle)
        drawCircle(nailColor.copy(alpha = 0.96f), radius = 1.25.dp.toPx(), center = tip)

        // Subtle dark oil press ripple during peak rhythmic press
        if (f == 1 && pressWave > 0.72f) {
          val pulseAlpha = ((pressWave - 0.72f) / 0.28f) * 0.58f
          drawCircle(
            color = stitchColor.copy(alpha = pulseAlpha),
            radius = (3.5f + pressWave * 5f).dp.toPx(),
            center = tip,
            style = Stroke(width = 1.2.dp.toPx()),
          )
        }
      }
    }
  }
}

/**
 * Living undulating blind segmented Leech-Clerks (up to 6) clinging around the edges of the
 * recessed counter glass plate, with anterior pale suction mouths pressed against the glass.
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

      val headX =
        if (onLeft) 15.dp.toPx() + crawl * 2.2.dp.toPx()
        else size.width - 15.dp.toPx() - crawl * 2.2.dp.toPx()
      val baseY = size.height * (0.24f + row * 0.27f)

      // 7 tapered undulating annelid segments trailing off the bezel edge
      val segmentCount = 7
      for (seg in (segmentCount - 1) downTo 0) {
        val dir = if (onLeft) -1f else 1f
        val segWave = sin(phaseAngle - seg * 0.62).toFloat() * (1.8f + seg * 0.25f).dp.toPx()
        val sx = headX + dir * seg * 3.1.dp.toPx()
        val sy = baseY + segWave
        val radius =
          when {
            seg == 0 -> 4.0.dp.toPx()
            seg <= 3 -> (4.4f - seg * 0.22f).dp.toPx()
            else -> (3.6f - (seg - 3) * 0.55f).dp.toPx().coerceAtLeast(1.8.dp.toPx())
          }

        drawCircle(color = shadowColor.copy(alpha = 0.88f), radius = radius + 1.2.dp.toPx(), center = Offset(sx, sy + 0.8.dp.toPx()))
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

      // Pulsing pale fleshy anterior suction mouth pressed directly onto the glass
      val mouthPulse = 1f + 0.14f * sin(phaseAngle * 2.0).toFloat()
      val mouthCenter = Offset(headX + (if (onLeft) 2.2.dp.toPx() else -2.2.dp.toPx()), baseY)
      drawCircle(
        color = mouthColor.copy(alpha = 0.65f),
        radius = 4.6.dp.toPx() * mouthPulse,
        center = mouthCenter,
      )
      drawCircle(
        color = shadowColor.copy(alpha = 0.92f),
        radius = 2.0.dp.toPx() * mouthPulse,
        center = mouthCenter,
      )
      drawCircle(
        color = highlightColor.copy(alpha = 0.55f),
        radius = 4.6.dp.toPx() * mouthPulse,
        center = mouthCenter,
        style = Stroke(width = 0.9.dp.toPx()),
      )
    }
  }
}

/**
 * Suspended organic pear/bell glass Weeping Lamps (up to 5) hanging from the chamber ceiling.
 * Dormant lamps sway gently in dim grey; when Stillness activates after 3s idle, their vessels
 * illuminate with sickly pale-lichen glow and weep falling condensation droplets.
 */
@Composable
internal fun WeepingLampsCeilingOverlay(
  lampCount: Int,
  isStillnessActive: Boolean,
  breathPhaseProvider: () -> Float,
  modifier: Modifier = Modifier,
) {
  if (lampCount <= 0) return

  val colorScheme = MaterialTheme.colorScheme
  val visibleLamps = min(lampCount, 5)

  Canvas(modifier = modifier.fillMaxSize().testTag("weeping_lamps_overlay")) {
    val breath = breathPhaseProvider()
    val spacing = size.width / (visibleLamps + 1)

    for (i in 1..visibleLamps) {
      val anchorX = spacing * i
      val sway = sin(breath * 2.0 * PI + i * 0.95).toFloat() * 6.5.dp.toPx()
      val cordLen = (24f + (i % 2) * 8f).dp.toPx()
      val socketCenter = Offset(anchorX + sway, cordLen)

      // Ceiling iron hook + slightly curved swaying braided cord
      drawCircle(color = colorScheme.outline, radius = 2.2.dp.toPx(), center = Offset(anchorX, 2.dp.toPx()))
      val cordPath =
        Path().apply {
          moveTo(anchorX, 2.dp.toPx())
          quadraticTo(anchorX + sway * 0.35f, cordLen * 0.55f, socketCenter.x, socketCenter.y)
        }
      drawPath(cordPath, colorScheme.background.copy(alpha = 0.85f), style = Stroke(2.4.dp.toPx()))
      drawPath(cordPath, colorScheme.outline.copy(alpha = 0.90f), style = Stroke(1.2.dp.toPx()))

      // Luminous sickly-lichen aura when weeping under Stillness
      if (isStillnessActive) {
        val haloRadius = (26f + breath * 5f).dp.toPx()
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(colorScheme.primary.copy(alpha = 0.32f), Color.Transparent),
              center = Offset(socketCenter.x, socketCenter.y + 12.dp.toPx()),
              radius = haloRadius,
            ),
          radius = haloRadius,
          center = Offset(socketCenter.x, socketCenter.y + 12.dp.toPx()),
        )
      }

      // Tarnished socket collar
      drawRect(
        color = colorScheme.surfaceVariant,
        topLeft = Offset(socketCenter.x - 4.dp.toPx(), socketCenter.y - 1.5.dp.toPx()),
        size = Size(8.dp.toPx(), 4.5.dp.toPx()),
      )
      drawRect(
        color = colorScheme.outline,
        topLeft = Offset(socketCenter.x - 4.dp.toPx(), socketCenter.y - 1.5.dp.toPx()),
        size = Size(8.dp.toPx(), 4.5.dp.toPx()),
        style = Stroke(width = 0.9.dp.toPx()),
      )

      // Organic bell/pear glass vessel
      val vesselTop = socketCenter.y + 3.dp.toPx()
      val vesselBottom = vesselTop + 19.dp.toPx()
      val vesselPath =
        Path().apply {
          moveTo(socketCenter.x - 3.4.dp.toPx(), vesselTop)
          cubicTo(
            socketCenter.x - 9.dp.toPx(),
            vesselTop + 6.dp.toPx(),
            socketCenter.x - 9.5.dp.toPx(),
            vesselBottom - 2.dp.toPx(),
            socketCenter.x,
            vesselBottom,
          )
          cubicTo(
            socketCenter.x + 9.5.dp.toPx(),
            vesselBottom - 2.dp.toPx(),
            socketCenter.x + 9.dp.toPx(),
            vesselTop + 6.dp.toPx(),
            socketCenter.x + 3.4.dp.toPx(),
            vesselTop,
          )
          close()
        }

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

      // Swelling & falling teardrop condensation droplets during Stillness
      if (isStillnessActive) {
        val dropProgress = ((breath + i * 0.27f) % 1f)
        val dropY = vesselBottom + 3.dp.toPx() + dropProgress * 26.dp.toPx()
        val dropAlpha = (1f - dropProgress * 0.72f).coerceIn(0.18f, 0.92f)
        val dropPath =
          Path().apply {
            moveTo(socketCenter.x, dropY - 4.dp.toPx())
            quadraticTo(socketCenter.x - 2.6.dp.toPx(), dropY + 1.dp.toPx(), socketCenter.x, dropY + 2.5.dp.toPx())
            quadraticTo(socketCenter.x + 2.6.dp.toPx(), dropY + 1.dp.toPx(), socketCenter.x, dropY - 4.dp.toPx())
            close()
          }
        drawPath(dropPath, colorScheme.primary.copy(alpha = dropAlpha))
      }
    }
  }
}
