package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.game.OrganGameState
import kotlin.math.PI
import kotlin.math.sin

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
 * - M2+: Heavy recessed cast-iron & mortared sump basin with rivets, dark viscous runoff pool,
 *        and rising hydraulic ichor bubbles when sealed/pressurized
 * - M3+: Chiseled jagged Neighbor Aperture masonry breach in upper-right wall with dim lichen glow
 * - M4+: Pressurized arterial Marrow Bloom vignette & iron valve clamp tension brackets
 * - M5+: Organic stretched calcified cartilage & translucent pale sinew webs anchoring the corners
 */
internal fun DrawScope.drawExcisionChamberMutations(
  state: OrganGameState,
  breath: Float,
  wallCreep: Float,
  primaryColor: Color,
  tertiaryColor: Color,
  outlineColor: Color,
  backgroundColor: Color,
  boneColor: Color,
  scratchPath: Path,
) {
  // Excision II+: Heavy recessed cast-iron & mortared floor sump basin along bottom threshold
  if (state.highestExcisionMilestone >= 2) {
    val pressureFrac = (state.drainPressure / 100.0).toFloat().coerceIn(0f, 1f)
    val basinW = (size.width * 0.50f).coerceAtMost(230.dp.toPx())
    val basinH = 22.dp.toPx()
    val basinLeft = (size.width - basinW) * 0.5f
    val basinTop = size.height - basinH - 4.dp.toPx()
    val cornerR = CornerRadius(5.dp.toPx(), 5.dp.toPx())

    // Dark wet runoff pool seeping outward across the chamber floor
    val poolR = basinW * (0.64f + pressureFrac * 0.22f)
    drawCircle(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              if (state.drainSealed) tertiaryColor.copy(alpha = 0.28f + pressureFrac * 0.34f)
              else backgroundColor.copy(alpha = 0.86f),
              backgroundColor.copy(alpha = 0.62f),
              Color.Transparent,
            ),
          center = Offset(size.width * 0.5f, size.height),
          radius = poolR,
        ),
      radius = poolR,
      center = Offset(size.width * 0.5f, size.height),
    )

    // Mortared stone outer bezel frame with recessed shadow lip
    val pad = 4.dp.toPx()
    drawRoundRect(
      color = backgroundColor.copy(alpha = 0.94f),
      topLeft = Offset(basinLeft - pad, basinTop - pad),
      size = Size(basinW + pad * 2f, basinH + pad * 2f),
      cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
    )
    drawRoundRect(
      color = outlineColor.copy(alpha = 0.52f),
      topLeft = Offset(basinLeft - pad, basinTop - pad),
      size = Size(basinW + pad * 2f, basinH + pad * 2f),
      cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
      style = Stroke(1.2.dp.toPx()),
    )

    // Deep abyssal sump pit interior
    drawRoundRect(
      brush =
        Brush.verticalGradient(
          colors =
            listOf(
              backgroundColor,
              if (state.drainSealed) tertiaryColor.copy(alpha = 0.22f + pressureFrac * 0.42f)
              else backgroundColor.copy(alpha = 0.96f),
            ),
          startY = basinTop,
          endY = basinTop + basinH,
        ),
      topLeft = Offset(basinLeft, basinTop),
      size = Size(basinW, basinH),
      cornerRadius = cornerR,
    )

    // Animated viscous sump bubbles / hydraulic meniscus surging underneath the iron grate
    val bubblePhase = wallCreep * 2.0 * PI
    for (b in 0 until 5) {
      val bx = basinLeft + basinW * (0.14f + b * 0.18f)
      val pulse = ((sin(bubblePhase + b * 1.45) + 1.0) * 0.5).toFloat()
      val by = basinTop + basinH * (0.68f - pulse * 0.28f * (0.4f + pressureFrac))
      val br = (1.6f + pulse * (1.8f + pressureFrac * 2.2f)).dp.toPx()
      val bColor =
        if (state.drainSealed) tertiaryColor.copy(alpha = 0.40f + pressureFrac * 0.45f)
        else primaryColor.copy(alpha = 0.16f + pulse * 0.16f)
      drawCircle(color = bColor, radius = br, center = Offset(bx, by))
      drawCircle(
        color = boneColor.copy(alpha = 0.18f * pulse),
        radius = br * 0.35f,
        center = Offset(bx - br * 0.25f, by - br * 0.25f),
      )
    }

    // Heavy forged cast-iron bars with bevel highlight & horizontal spine brace
    val barCount = 7
    for (i in 1..barCount) {
      val bx = basinLeft + basinW * (i.toFloat() / (barCount + 1))
      drawLine(
        color = backgroundColor.copy(alpha = 0.95f),
        start = Offset(bx, basinTop + 1.dp.toPx()),
        end = Offset(bx, basinTop + basinH - 1.dp.toPx()),
        strokeWidth = 4.2.dp.toPx(),
        cap = StrokeCap.Butt,
      )
      drawLine(
        color = outlineColor.copy(alpha = if (state.drainSealed) 0.82f else 0.62f),
        start = Offset(bx - 0.6.dp.toPx(), basinTop + 1.dp.toPx()),
        end = Offset(bx - 0.6.dp.toPx(), basinTop + basinH - 1.dp.toPx()),
        strokeWidth = 2.0.dp.toPx(),
      )
      drawLine(
        color = boneColor.copy(alpha = 0.18f),
        start = Offset(bx - 1.2.dp.toPx(), basinTop + 2.dp.toPx()),
        end = Offset(bx - 1.2.dp.toPx(), basinTop + basinH - 2.dp.toPx()),
        strokeWidth = 0.7.dp.toPx(),
      )
    }

    // Center cast-iron cross-beam + metallic top rim highlight
    val midBarY = basinTop + basinH * 0.5f
    drawLine(
      color = backgroundColor.copy(alpha = 0.92f),
      start = Offset(basinLeft + 2.dp.toPx(), midBarY),
      end = Offset(basinLeft + basinW - 2.dp.toPx(), midBarY),
      strokeWidth = 3.2.dp.toPx(),
    )
    drawLine(
      color = outlineColor.copy(alpha = 0.65f),
      start = Offset(basinLeft + 2.dp.toPx(), midBarY - 0.5.dp.toPx()),
      end = Offset(basinLeft + basinW - 2.dp.toPx(), midBarY - 0.5.dp.toPx()),
      strokeWidth = 1.1.dp.toPx(),
    )

    // Outer cast-iron flange rim + 4 corner hex rivets
    drawRoundRect(
      color =
        if (state.drainSealed) tertiaryColor.copy(alpha = 0.72f + pressureFrac * 0.24f)
        else outlineColor.copy(alpha = 0.78f),
      topLeft = Offset(basinLeft, basinTop),
      size = Size(basinW, basinH),
      cornerRadius = cornerR,
      style = Stroke(1.8.dp.toPx()),
    )
    val rivetOffsets =
      listOf(
        Offset(basinLeft + 6.dp.toPx(), basinTop + 5.dp.toPx()),
        Offset(basinLeft + basinW - 6.dp.toPx(), basinTop + 5.dp.toPx()),
        Offset(basinLeft + 6.dp.toPx(), basinTop + basinH - 5.dp.toPx()),
        Offset(basinLeft + basinW - 6.dp.toPx(), basinTop + basinH - 5.dp.toPx()),
      )
    for (rivet in rivetOffsets) {
      drawCircle(color = backgroundColor, radius = 2.4.dp.toPx(), center = rivet)
      drawCircle(color = outlineColor.copy(alpha = 0.85f), radius = 1.5.dp.toPx(), center = rivet)
      drawCircle(
        color = boneColor.copy(alpha = 0.28f),
        radius = 0.6.dp.toPx(),
        center = Offset(rivet.x - 0.4.dp.toPx(), rivet.y - 0.4.dp.toPx()),
      )
    }
  }

  // Excision III+: Chiseled jagged Neighbor Aperture masonry breach in upper-right wall
  if (state.highestExcisionMilestone >= 3) {
    val sx = size.width * 0.86f
    val sy = size.height * 0.22f
    val activeEcho = state.echoDiversionPercent > 0

    // Chipped-mortar bruised halo
    drawCircle(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              if (activeEcho) primaryColor.copy(alpha = 0.24f + breath * 0.16f)
              else backgroundColor.copy(alpha = 0.78f),
              Color.Transparent,
            ),
          center = Offset(sx, sy),
          radius = 24.dp.toPx(),
        ),
      radius = 24.dp.toPx(),
      center = Offset(sx, sy),
    )

    // Irregular chiseled stone fracture rim
    scratchPath.rewind()
    scratchPath.moveTo(sx - 5.dp.toPx(), sy - 20.dp.toPx())
    scratchPath.lineTo(sx + 4.dp.toPx(), sy - 16.dp.toPx())
    scratchPath.lineTo(sx + 6.5.dp.toPx(), sy - 4.dp.toPx())
    scratchPath.lineTo(sx + 3.5.dp.toPx(), sy + 10.dp.toPx())
    scratchPath.lineTo(sx + 5.dp.toPx(), sy + 20.dp.toPx())
    scratchPath.lineTo(sx - 4.dp.toPx(), sy + 17.dp.toPx())
    scratchPath.lineTo(sx - 6.dp.toPx(), sy + 3.dp.toPx())
    scratchPath.lineTo(sx - 3.dp.toPx(), sy - 9.dp.toPx())
    scratchPath.close()

    drawPath(
      path = scratchPath,
      color = backgroundColor.copy(alpha = 0.96f),
    )
    drawPath(
      path = scratchPath,
      color =
        if (activeEcho) primaryColor.copy(alpha = 0.48f + breath * 0.28f)
        else outlineColor.copy(alpha = 0.65f),
      style = Stroke(width = 1.4.dp.toPx(), join = StrokeJoin.Miter),
    )

    // Faint damp luminescence deep inside the slit
    drawCircle(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              if (activeEcho) primaryColor.copy(alpha = 0.72f + breath * 0.22f)
              else boneColor.copy(alpha = 0.16f),
              Color.Transparent,
            ),
          center = Offset(sx, sy),
          radius = 6.dp.toPx(),
        ),
      radius = 6.dp.toPx(),
      center = Offset(sx, sy),
    )
  }

  // Excision IV+: Pressurized Marrow Bloom / Valve Clamp arterial tension glow & iron jaw brackets
  if (state.isValveClamped || state.marrowBloomRemainingSeconds > 0.0) {
    val tintAlpha = if (state.isValveClamped) 0.24f + breath * 0.14f else 0.20f + breath * 0.08f
    drawRect(
      brush =
        Brush.radialGradient(
          colors = listOf(Color.Transparent, tertiaryColor.copy(alpha = tintAlpha)),
          center = Offset(size.width * 0.5f, size.height * 0.60f),
          radius = size.minDimension * 0.68f,
        )
    )
  }

  // Excision V+: Organic stretched calcified cartilage & translucent pale sinew webs in upper corners
  if (state.highestExcisionMilestone >= 5) {
    for (side in listOf(-1f, 1f)) {
      val cornerX = if (side < 0f) 0f else size.width
      val spanX = size.width * 0.24f * -side
      val spanY = size.height * 0.20f
      val stretch = breath * 4.dp.toPx()

      // Translucent ossified membrane webbing
      scratchPath.rewind()
      scratchPath.moveTo(cornerX, 0f)
      scratchPath.lineTo(cornerX + spanX, 0f)
      scratchPath.cubicTo(
        cornerX + spanX * 0.55f,
        spanY * 0.35f + stretch,
        cornerX + spanX * 0.28f,
        spanY * 0.68f + stretch,
        cornerX,
        spanY,
      )
      scratchPath.close()
      drawPath(
        path = scratchPath,
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                boneColor.copy(alpha = 0.14f + breath * 0.04f),
                tertiaryColor.copy(alpha = 0.16f),
                Color.Transparent,
              ),
            center = Offset(cornerX, 0f),
            radius = spanY * 1.15f,
          ),
      )

      // Taut sinew & calcified tendon strands spanning the corner cavity
      for (strand in 0..2) {
        val sx = cornerX + spanX * (0.38f + strand * 0.26f)
        val ey = spanY * (0.42f + strand * 0.25f)
        val ctrlX = cornerX + spanX * (0.18f + strand * 0.10f)
        val ctrlY = ey * 0.52f + stretch
        scratchPath.rewind()
        scratchPath.moveTo(sx, 0f)
        scratchPath.quadraticTo(ctrlX, ctrlY, cornerX, ey)
        drawPath(
          path = scratchPath,
          color = backgroundColor.copy(alpha = 0.72f),
          style = Stroke(width = (3.4f - strand * 0.6f).dp.toPx(), cap = StrokeCap.Round),
        )
        drawPath(
          path = scratchPath,
          color =
            if (strand == 1) boneColor.copy(alpha = 0.32f + breath * 0.08f)
            else tertiaryColor.copy(alpha = 0.38f + breath * 0.08f),
          style = Stroke(width = (1.6f - strand * 0.25f).dp.toPx(), cap = StrokeCap.Round),
        )
      }
    }
  }
}
