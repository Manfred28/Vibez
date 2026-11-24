package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.game.GraftSacrificeKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Living undulating blind segmented Leech-Clerks (up to 6) actively crawling around a smooth
 * rounded-rectangle perimeter track hugging the counter glass plate. Each leech moves continuously
 * with tangent-aligned undulating segments, tapered tail, wet dorsal glint, and pulsing suction mouth.
 */
@Composable
internal fun LeechClerksGlassOverlay(
  leechCount: Int,
  breathPhaseProvider: () -> Float,
  patrolPhaseProvider: () -> Float,
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
    val patrol = patrolPhaseProvider()

    val insetX = 19.dp.toPx().coerceAtMost(size.width * 0.25f)
    val insetY = 7.dp.toPx().coerceAtMost(size.height * 0.30f)
    val left = insetX
    val right = (size.width - insetX).coerceAtLeast(left + 12f)
    val top = insetY
    val bottom = (size.height - insetY).coerceAtLeast(top + 12f)
    val trackW = right - left
    val trackH = bottom - top
    val perimeter = 2f * (trackW + trackH)

    fun sampleTrack(sRaw: Float): Pair<Offset, Offset> {
      var s = sRaw % perimeter
      if (s < 0f) s += perimeter
      return when {
        s < trackW -> Offset(left + s, top) to Offset(1f, 0f)
        s < trackW + trackH -> Offset(right, top + (s - trackW)) to Offset(0f, 1f)
        s < 2f * trackW + trackH ->
          Offset(right - (s - trackW - trackH), bottom) to Offset(-1f, 0f)
        else -> Offset(left, bottom - (s - 2f * trackW - trackH)) to Offset(0f, -1f)
      }
    }

    for (i in 0 until visibleLeeches) {
      val dirSign = if (i % 2 == 0) 1f else -1f
      val speedMult = 1f + (i % 3) * 0.14f
      val startFrac = (i.toFloat() / visibleLeeches.toFloat())
      val headS = ((startFrac + patrol * dirSign * speedMult) * perimeter)
      val wiggleAngle = breath * 2.0 * PI + i * 1.4

      val segmentCount = 7
      val segStepPx = 3.2.dp.toPx()

      // Draw from tail to head so anterior segments overlap cleanly
      for (seg in (segmentCount - 1) downTo 0) {
        val (pt, tan) = sampleTrack(headS - dirSign * seg * segStepPx)
        val normX = -tan.y
        val normY = tan.x
        val sway = sin(wiggleAngle - seg * 0.72).toFloat() * (1.2f + seg * 0.22f).dp.toPx()
        val sx = pt.x + normX * sway
        val sy = pt.y + normY * sway

        val radius =
          when {
            seg == 0 -> 3.6.dp.toPx()
            seg <= 3 -> (4.0f - seg * 0.24f).dp.toPx()
            else -> (3.1f - (seg - 3) * 0.52f).dp.toPx().coerceAtLeast(1.4.dp.toPx())
          }

        drawCircle(
          color = shadowColor.copy(alpha = 0.86f),
          radius = radius + 1.0.dp.toPx(),
          center = Offset(sx, sy + 0.7.dp.toPx()),
        )
        drawCircle(
          color = if (seg % 2 == 0) bodyMid.copy(alpha = 0.94f) else bodyDark.copy(alpha = 0.96f),
          radius = radius,
          center = Offset(sx, sy),
        )
        drawCircle(
          color = highlightColor.copy(alpha = 0.30f),
          radius = radius * 0.32f,
          center = Offset(sx - radius * 0.2f, sy - radius * 0.3f),
        )
      }

      // Pulsing pale suction mouth leading along the perimeter bezel
      val (headPt, headTan) = sampleTrack(headS)
      val mouthPulse = 1f + 0.14f * sin(wiggleAngle * 2.0).toFloat()
      val mouthCenter =
        Offset(headPt.x + headTan.x * dirSign * 2.0.dp.toPx(), headPt.y + headTan.y * dirSign * 2.0.dp.toPx())
      drawCircle(
        color = mouthColor.copy(alpha = 0.65f),
        radius = 4.0.dp.toPx() * mouthPulse,
        center = mouthCenter,
      )
      drawCircle(
        color = shadowColor.copy(alpha = 0.92f),
        radius = 1.7.dp.toPx() * mouthPulse,
        center = mouthCenter,
      )
      drawCircle(
        color = highlightColor.copy(alpha = 0.52f),
        radius = 4.0.dp.toPx() * mouthPulse,
        center = mouthCenter,
        style = Stroke(width = 0.85.dp.toPx()),
      )
    }
  }
}

internal data class WeepingLampSlot(
  val xFraction: Float,
  val cordDp: Float,
)

/**
 * Computes staggered ceiling anchor positions for 1..5 visible Weeping Lamps.
 * Every slot stays strictly on the left (`<= 0.24`) or right (`>= 0.76`) flanks so no lamp,
 * Stillness halo, or condensation droplet ever sits over the center income readout or digits.
 */
internal fun weepingLampLayout(lampCount: Int): List<WeepingLampSlot> {
  val visibleLamps = min(lampCount, 5)
  if (visibleLamps <= 0) return emptyList()
  return when (visibleLamps) {
    1 -> listOf(WeepingLampSlot(0.14f, 26f))
    2 -> listOf(WeepingLampSlot(0.13f, 26f), WeepingLampSlot(0.87f, 30f))
    3 ->
      listOf(
        WeepingLampSlot(0.10f, 30f),
        WeepingLampSlot(0.21f, 16f),
        WeepingLampSlot(0.88f, 28f),
      )
    4 ->
      listOf(
        WeepingLampSlot(0.09f, 32f),
        WeepingLampSlot(0.21f, 16f),
        WeepingLampSlot(0.79f, 18f),
        WeepingLampSlot(0.91f, 32f),
      )
    else ->
      listOf(
        WeepingLampSlot(0.08f, 24f),
        WeepingLampSlot(0.20f, 14f),
        WeepingLampSlot(0.14f, 62f),
        WeepingLampSlot(0.80f, 16f),
        WeepingLampSlot(0.92f, 34f),
      )
  }
}

/**
 * Suspended organic pear/bell glass Weeping Lamps (up to 5) hanging on staggered left/right
 * ceiling flanks behind the foreground counter/income HUD so every lamp stays visible while
 * never obscuring the center Ichor rate or digits.
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
  val slots = remember(lampCount) { weepingLampLayout(lampCount) }
  val cordPath = remember { Path() }
  val vesselPath = remember { Path() }
  val dropPath = remember { Path() }

  Canvas(modifier = modifier.fillMaxSize().testTag("weeping_lamps_overlay")) {
    val breath = breathPhaseProvider()
    val dropPhase = dropPhaseProvider()

    for (idx in slots.indices) {
      val slot = slots[idx]
      val anchorX = size.width * slot.xFraction
      val sway = sin(breath * 2.0 * PI + (idx + 1) * 0.95).toFloat() * 4.5.dp.toPx()
      val cordLen = slot.cordDp.dp.toPx()
      val socketCenter = Offset(anchorX + sway, cordLen)

      // Ceiling iron hook + swaying braided cord
      drawCircle(
        color = colorScheme.outline,
        radius = 2.0.dp.toPx(),
        center = Offset(anchorX, 2.dp.toPx()),
      )
      cordPath.rewind()
      cordPath.moveTo(anchorX, 2.dp.toPx())
      cordPath.quadraticTo(anchorX + sway * 0.35f, cordLen * 0.55f, socketCenter.x, socketCenter.y)
      drawPath(cordPath, colorScheme.background.copy(alpha = 0.85f), style = Stroke(2.3.dp.toPx()))
      drawPath(cordPath, colorScheme.outline.copy(alpha = 0.92f), style = Stroke(1.1.dp.toPx()))

      // Luminous sickly-lichen aura when weeping under Stillness
      if (isStillnessActive) {
        val haloRadius = (24f + breath * 5f).dp.toPx()
        val haloCenter = Offset(socketCenter.x, socketCenter.y + 11.dp.toPx())
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(colorScheme.primary.copy(alpha = 0.30f), Color.Transparent),
              center = haloCenter,
              radius = haloRadius,
            ),
          radius = haloRadius,
          center = haloCenter,
        )
      }

      // Tarnished socket collar
      val collarTopLeft = Offset(socketCenter.x - 3.8.dp.toPx(), socketCenter.y - 1.5.dp.toPx())
      val collarSize = Size(7.6.dp.toPx(), 4.2.dp.toPx())
      drawRect(color = colorScheme.surfaceVariant, topLeft = collarTopLeft, size = collarSize)
      drawRect(
        color = colorScheme.outline,
        topLeft = collarTopLeft,
        size = collarSize,
        style = Stroke(width = 0.9.dp.toPx()),
      )

      // Organic bell/pear glass vessel
      val vesselTop = socketCenter.y + 2.8.dp.toPx()
      val vesselBottom = vesselTop + 17.5.dp.toPx()
      vesselPath.rewind()
      vesselPath.moveTo(socketCenter.x - 3.2.dp.toPx(), vesselTop)
      vesselPath.cubicTo(
        socketCenter.x - 8.5.dp.toPx(),
        vesselTop + 5.5.dp.toPx(),
        socketCenter.x - 9.0.dp.toPx(),
        vesselBottom - 2.dp.toPx(),
        socketCenter.x,
        vesselBottom,
      )
      vesselPath.cubicTo(
        socketCenter.x + 9.0.dp.toPx(),
        vesselBottom - 2.dp.toPx(),
        socketCenter.x + 8.5.dp.toPx(),
        vesselTop + 5.5.dp.toPx(),
        socketCenter.x + 3.2.dp.toPx(),
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
                  alpha = if (isStillnessActive) 0.80f else 0.45f
                ),
                colorScheme.surfaceVariant.copy(alpha = 0.92f),
              ),
            center = Offset(socketCenter.x - 1.5.dp.toPx(), vesselTop + 8.5.dp.toPx()),
            radius = 11.dp.toPx(),
          ),
      )

      drawLine(
        color =
          if (isStillnessActive) colorScheme.onBackground.copy(alpha = 0.78f)
          else colorScheme.outline.copy(alpha = 0.60f),
        start = Offset(socketCenter.x, vesselTop + 1.5.dp.toPx()),
        end = Offset(socketCenter.x, vesselTop + 10.dp.toPx()),
        strokeWidth = 1.dp.toPx(),
      )
      drawPath(
        path = vesselPath,
        color =
          if (isStillnessActive) colorScheme.primary.copy(alpha = 0.90f)
          else colorScheme.outline.copy(alpha = 0.82f),
        style = Stroke(width = 1.dp.toPx()),
      )

      // Strictly downward-falling teardrop condensation droplets along the flanks during Stillness
      if (isStillnessActive) {
        val dropProgress = (dropPhase + (idx + 1) * 0.27f) % 1f
        val fallNorm = ((dropProgress - 0.16f) / 0.84f).coerceIn(0f, 1f)
        val gravityCurve = fallNorm * fallNorm
        val fallDistance = 36f.dp.toPx()
        val dropY = vesselBottom + 2.dp.toPx() + gravityCurve * fallDistance
        val swellScale =
          if (dropProgress < 0.16f) 0.55f + (dropProgress / 0.16f) * 0.45f else 1f
        val stretchY = 1f + fallNorm * 0.35f
        val dropAlpha =
          if (dropProgress < 0.16f) 0.55f + (dropProgress / 0.16f) * 0.35f
          else ((1f - fallNorm * 0.85f).coerceIn(0.10f, 0.90f))

        val halfW = 2.3.dp.toPx() * swellScale
        val topOffset = 3.6.dp.toPx() * swellScale * stretchY
        val botOffset = 2.3.dp.toPx() * swellScale
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

/**
 * Grotesque ~1.4s creature death/dissolution animation played over the valve collar whenever a
 * Choir-Tendril (2 Hands + 2 Leeches) or Choir-Lung (2 Lamps + 1 Tendril) graft consumes specimens.
 */
@Composable
internal fun GraftSacrificeOverlay(
  sacrificePulse: Long,
  sacrificeKind: GraftSacrificeKind?,
  modifier: Modifier = Modifier,
) {
  val cs = MaterialTheme.colorScheme
  var initialized by remember { mutableStateOf(false) }
  var lastPulse by remember { mutableLongStateOf(sacrificePulse) }
  var active by remember { mutableStateOf(false) }
  val progress = remember { Animatable(0f) }

  LaunchedEffect(sacrificePulse) {
    if (!initialized) {
      initialized = true
      lastPulse = sacrificePulse
      return@LaunchedEffect
    }
    if (sacrificePulse > lastPulse) {
      lastPulse = sacrificePulse
      active = true
      progress.snapTo(0f)
      progress.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1380, easing = FastOutSlowInEasing),
      )
      active = false
    }
  }

  if (!active) return

  val p = progress.value
  val fade = (1f - ((p - 0.65f) / 0.35f).coerceIn(0f, 1f))

  Canvas(modifier = modifier.fillMaxSize().testTag("graft_sacrifice_overlay")) {
    val center = Offset(size.width * 0.5f, size.height * 0.62f)
    val startR = size.minDimension * 0.42f
    val shrinkR = startR * (1f - p * 0.86f)

    // Dark-clot suturing collapse ring
    drawCircle(
      color = cs.tertiary.copy(alpha = 0.55f * fade),
      radius = (18f + p * 46f).dp.toPx(),
      center = center,
      style = Stroke(width = (2.4f * fade).dp.toPx()),
    )

    val count = if (sacrificeKind == GraftSacrificeKind.TENDRIL) 4 else 3
    for (i in 0 until count) {
      val baseAngle = i * (2.0 * PI / count) + p * 0.85
      val px = center.x + cos(baseAngle).toFloat() * shrinkR
      val py = center.y + sin(baseAngle).toFloat() * shrinkR
      val nodePos = Offset(px, py)

      // Taut black surgical suture thread yanking the sacrificed creature inward
      drawLine(
        color = cs.background.copy(alpha = 0.90f * fade),
        start = nodePos,
        end = center,
        strokeWidth = 2.4.dp.toPx(),
        cap = StrokeCap.Round,
      )
      drawLine(
        color = cs.tertiary.copy(alpha = 0.85f * fade),
        start = nodePos,
        end = center,
        strokeWidth = 1.1.dp.toPx(),
        cap = StrokeCap.Round,
      )

      // Shriveling creature silhouette (pale gnarled curl for Hand/Lamp, dark segmented knot for Leech)
      val bodyColor =
        if (sacrificeKind == GraftSacrificeKind.TENDRIL && i % 2 == 1) cs.secondary
        else cs.onBackground
      val bodyRadius = (10f * (1f - p * 0.68f)).coerceAtLeast(2.5f).dp.toPx()
      drawCircle(
        color = cs.background.copy(alpha = 0.92f * fade),
        radius = bodyRadius + 1.5.dp.toPx(),
        center = nodePos,
      )
      drawCircle(
        color = bodyColor.copy(alpha = 0.78f * fade),
        radius = bodyRadius,
        center = nodePos,
      )

      // Splattering ichor dissolution droplets trailing off the dying form
      for (d in 1..3) {
        val dropAngle = baseAngle + d * 0.42
        val dropDist = (8f + p * 22f * d).dp.toPx()
        val dpPos = Offset(px + cos(dropAngle).toFloat() * dropDist * 0.35f, py + sin(dropAngle).toFloat() * dropDist * 0.35f + p * 14.dp.toPx())
        drawCircle(
          color = cs.tertiary.copy(alpha = 0.72f * fade),
          radius = (2.1f * (1f - p * 0.5f)).dp.toPx(),
          center = dpPos,
        )
      }
    }
  }
}
