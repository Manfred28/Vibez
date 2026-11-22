package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.game.OrganGameState

/** Detailed anatomical glass-vessel portrait for the Weeping Lamp specimen viewport. */
@Composable
internal fun WeepingLampSpecimenPortrait(
  isWeeping: Boolean,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val vesselPath = remember { Path() }
  val dropPath = remember { Path() }

  Canvas(modifier = modifier) {
    val cx = size.width * 0.5f
    val topY = 0f
    val socketY = size.height * 0.24f
    val bulbBottomY = size.height * 0.73f

    // Subtle interior damp glow halo
    drawCircle(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              colorScheme.primary.copy(alpha = if (isWeeping) 0.40f else 0.18f),
              Color.Transparent,
            ),
          center = Offset(cx, size.height * 0.50f),
          radius = size.minDimension * 0.48f,
        ),
      radius = size.minDimension * 0.48f,
      center = Offset(cx, size.height * 0.50f),
    )

    // Braided ceiling cord
    drawLine(
      color = colorScheme.outline,
      start = Offset(cx, topY),
      end = Offset(cx, socketY),
      strokeWidth = 2.dp.toPx(),
    )

    // Tarnished brass/chitin socket collar
    val collarTopLeft = Offset(cx - 5.5.dp.toPx(), socketY - 3.dp.toPx())
    val collarSize = Size(11.dp.toPx(), 6.5.dp.toPx())
    drawRect(color = colorScheme.surfaceVariant, topLeft = collarTopLeft, size = collarSize)
    drawRect(
      color = colorScheme.outline,
      topLeft = collarTopLeft,
      size = collarSize,
      style = Stroke(width = 1.dp.toPx()),
    )

    // Organic pear/bell glass vessel path
    vesselPath.rewind()
    vesselPath.moveTo(cx - 4.5.dp.toPx(), socketY + 3.dp.toPx())
    vesselPath.cubicTo(
      cx - 12.dp.toPx(),
      size.height * 0.42f,
      cx - 14.dp.toPx(),
      size.height * 0.66f,
      cx,
      bulbBottomY,
    )
    vesselPath.cubicTo(
      cx + 14.dp.toPx(),
      size.height * 0.66f,
      cx + 12.dp.toPx(),
      size.height * 0.42f,
      cx + 4.5.dp.toPx(),
      socketY + 3.dp.toPx(),
    )
    vesselPath.close()

    drawPath(
      path = vesselPath,
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              (if (isWeeping) colorScheme.primary else colorScheme.secondary).copy(alpha = 0.68f),
              colorScheme.surfaceVariant.copy(alpha = 0.92f),
              colorScheme.background,
            ),
          center = Offset(cx - 2.dp.toPx(), size.height * 0.48f),
          radius = 18.dp.toPx(),
        ),
    )

    // Internal fluid meniscus & pulsing vein filament
    val filamentColor =
      if (isWeeping) colorScheme.onBackground.copy(alpha = 0.78f)
      else colorScheme.primary.copy(alpha = 0.62f)
    drawLine(
      color = filamentColor,
      start = Offset(cx, socketY + 4.dp.toPx()),
      end = Offset(cx, size.height * 0.56f),
      strokeWidth = 1.1.dp.toPx(),
      cap = StrokeCap.Round,
    )
    drawLine(
      color = colorScheme.primary.copy(alpha = 0.55f),
      start = Offset(cx - 7.dp.toPx(), size.height * 0.58f),
      end = Offset(cx + 7.dp.toPx(), size.height * 0.58f),
      strokeWidth = 1.dp.toPx(),
    )

    // Glass rim highlight + outline
    drawPath(
      path = vesselPath,
      color =
        if (isWeeping) colorScheme.primary.copy(alpha = 0.92f)
        else colorScheme.outline.copy(alpha = 0.90f),
      style = Stroke(width = 1.1.dp.toPx()),
    )
    drawLine(
      color = colorScheme.onBackground.copy(alpha = 0.35f),
      start = Offset(cx - 5.5.dp.toPx(), size.height * 0.40f),
      end = Offset(cx - 6.5.dp.toPx(), size.height * 0.55f),
      strokeWidth = 1.3.dp.toPx(),
      cap = StrokeCap.Round,
    )

    // Hanging teardrop condensation droplet below the vessel
    val dropTop = bulbBottomY + 2.dp.toPx()
    val dropBottom = size.height * 0.90f
    dropPath.rewind()
    dropPath.moveTo(cx, dropTop)
    dropPath.quadraticTo(cx - 3.6.dp.toPx(), dropBottom, cx, dropBottom + 1.5.dp.toPx())
    dropPath.quadraticTo(cx + 3.6.dp.toPx(), dropBottom, cx, dropTop)
    dropPath.close()
    drawPath(
      path = dropPath,
      color = if (isWeeping) colorScheme.primary else colorScheme.secondary.copy(alpha = 0.85f),
    )
  }
}

/** Anatomical Canvas portrait for Tier 4 Organism: Sump-Siphon (unlocked at Excision II). */
@Composable
internal fun SumpSiphonSpecimenPortrait(
  active: Boolean,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val bladderPath = remember { Path() }

  Canvas(modifier = modifier) {
    val cx = size.width * 0.5f
    val baseY = size.height * 0.84f

    // Floor grate iron bars at base
    for (i in 0..4) {
      val gx = size.width * (0.16f + i * 0.17f)
      drawLine(
        color = colorScheme.outline,
        start = Offset(gx, baseY),
        end = Offset(gx, size.height * 0.96f),
        strokeWidth = 2.dp.toPx(),
      )
    }
    drawLine(
      color = colorScheme.outline,
      start = Offset(size.width * 0.12f, baseY),
      end = Offset(size.width * 0.88f, baseY),
      strokeWidth = 1.6.dp.toPx(),
    )

    // Ribbed leather siphon bladder rising from grate
    bladderPath.rewind()
    bladderPath.moveTo(cx - 10.dp.toPx(), baseY)
    bladderPath.cubicTo(
      cx - 16.dp.toPx(),
      size.height * 0.52f,
      cx - 11.dp.toPx(),
      size.height * 0.22f,
      cx,
      size.height * 0.16f,
    )
    bladderPath.cubicTo(
      cx + 11.dp.toPx(),
      size.height * 0.22f,
      cx + 16.dp.toPx(),
      size.height * 0.52f,
      cx + 10.dp.toPx(),
      baseY,
    )
    bladderPath.close()
    drawPath(
      path = bladderPath,
      brush =
        Brush.verticalGradient(
          colors =
            listOf(
              if (active) colorScheme.tertiary.copy(alpha = 0.72f)
              else colorScheme.surfaceVariant,
              colorScheme.surfaceVariant,
              colorScheme.background,
            )
        ),
    )
    drawPath(bladderPath, colorScheme.outline, style = Stroke(1.3.dp.toPx()))

    // Transverse leather ribs & intake valve ring
    for (r in 1..4) {
      val ry = size.height * (0.26f + r * 0.12f)
      val halfW = (7f + (r % 2) * 3f).dp.toPx()
      drawLine(
        color = colorScheme.onSurfaceVariant.copy(alpha = 0.52f),
        start = Offset(cx - halfW, ry),
        end = Offset(cx + halfW, ry),
        strokeWidth = 1.4.dp.toPx(),
        cap = StrokeCap.Round,
      )
    }
    drawCircle(
      color = if (active) colorScheme.primary else colorScheme.outline,
      radius = 4.5.dp.toPx(),
      center = Offset(cx, size.height * 0.20f),
      style = Stroke(1.5.dp.toPx()),
    )
  }
}

/**
 * Renders Excision II-V physical chamber wall mutations inside ChamberWallBackdrop:
 * - M2+: Black runoff sump pool & iron floor grate along bottom edge
 * - M3+: Vertical unbricked Neighbor Aperture wall slit pulsing echoes
 * - M4+: Pressurized crimson vein pulse & iron clamp brackets during Valve Clamp
 * - M5+: Creeping pale organic membrane border along the frame edge
 */
internal fun DrawScope.drawExcisionChamberMutations(
  state: OrganGameState,
  breath: Float,
  primaryColor: Color,
  tertiaryColor: Color,
  outlineColor: Color,
  backgroundColor: Color,
  scratchPath: Path,
) {
  // Excision II+: Black sump pool & iron floor drain grate
  if (state.highestExcisionMilestone >= 2) {
    val poolHeight = (14.dp.toPx() + (state.drainPressure / 100.0).toFloat() * 14.dp.toPx())
    drawRect(
      brush =
        Brush.verticalGradient(
          colors = listOf(Color.Transparent, backgroundColor.copy(alpha = 0.88f)),
          startY = size.height - poolHeight * 1.6f,
          endY = size.height,
        ),
      topLeft = Offset(0f, size.height - poolHeight * 1.6f),
      size = Size(size.width, poolHeight * 1.6f),
    )
    val grateW = size.width * 0.34f
    val grateLeft = (size.width - grateW) * 0.5f
    val grateTop = size.height - 9.dp.toPx()
    drawRect(
      color = outlineColor.copy(alpha = if (state.drainSealed) 0.90f else 0.48f),
      topLeft = Offset(grateLeft, grateTop),
      size = Size(grateW, 7.dp.toPx()),
      style = Stroke(1.2.dp.toPx()),
    )
    for (bar in 1..6) {
      val bx = grateLeft + grateW * (bar / 7f)
      drawLine(
        color = outlineColor.copy(alpha = 0.65f),
        start = Offset(bx, grateTop),
        end = Offset(bx, grateTop + 7.dp.toPx()),
        strokeWidth = 1.1.dp.toPx(),
      )
    }
  }

  // Excision III+: Unbricked Neighbor Aperture wall slit
  if (state.highestExcisionMilestone >= 3) {
    val slitX = size.width * 0.11f
    val slitTop = size.height * 0.24f
    val slitBottom = size.height * 0.58f
    val echoGlow = if (state.echoDiversionPercent > 0) 0.42f + breath * 0.25f else 0.14f
    drawLine(
      color = primaryColor.copy(alpha = echoGlow),
      start = Offset(slitX, slitTop),
      end = Offset(slitX, slitBottom),
      strokeWidth = 5.dp.toPx(),
      cap = StrokeCap.Round,
    )
    drawLine(
      color = backgroundColor.copy(alpha = 0.95f),
      start = Offset(slitX, slitTop + 2.dp.toPx()),
      end = Offset(slitX, slitBottom - 2.dp.toPx()),
      strokeWidth = 2.dp.toPx(),
      cap = StrokeCap.Round,
    )
  }

  // Excision IV+: Valve Clamp iron pressure brackets & Marrow Bloom vignette
  if (state.isValveClamped || state.marrowBloomRemainingSeconds > 0.0) {
    val tintAlpha = if (state.isValveClamped) 0.24f + breath * 0.14f else 0.18f
    drawRect(
      brush =
        Brush.radialGradient(
          colors = listOf(Color.Transparent, tertiaryColor.copy(alpha = tintAlpha)),
          center = Offset(size.width * 0.5f, size.height * 0.55f),
          radius = size.minDimension * 0.72f,
        )
    )
  }

  // Excision V+: Creeping pale membrane border along lower corners
  if (state.highestExcisionMilestone >= 5) {
    val membraneColor = tertiaryColor.copy(alpha = 0.26f + breath * 0.06f)
    scratchPath.rewind()
    scratchPath.moveTo(0f, size.height)
    scratchPath.lineTo(0f, size.height * 0.74f)
    scratchPath.quadraticTo(size.width * 0.12f, size.height * 0.86f, size.width * 0.24f, size.height)
    scratchPath.close()
    drawPath(scratchPath, membraneColor)

    scratchPath.rewind()
    scratchPath.moveTo(size.width, size.height)
    scratchPath.lineTo(size.width, size.height * 0.74f)
    scratchPath.quadraticTo(size.width * 0.88f, size.height * 0.86f, size.width * 0.76f, size.height)
    scratchPath.close()
    drawPath(scratchPath, membraneColor)
  }
}
