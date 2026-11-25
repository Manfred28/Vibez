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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
 * Opacity weights for the 5 progressive chamber wall photographic layers across Phases 0..4
 * and Excisions I..VI. Mirrored 1:1 on Web (`computeWallLayerAlphas` in `chamberCanvas.js`).
 */
internal data class WallLayerAlphas(
  val phase1Tile: Float,
  val phase2Fleshy: Float,
  val phase3Muscle: Float,
  val phase4Hollow: Float,
  val lateExcision: Float,
)

internal fun computeWallLayerAlphas(
  phase: Int,
  highestExcisionMilestone: Int,
  cycle: Int = 0,
): WallLayerAlphas {
  val clampedPhase = phase.coerceIn(0, 4)
  val phase1Tile =
    when (clampedPhase) {
      0 -> 0.34f
      1 -> 0.76f
      2 -> 0.34f
      3 -> 0.14f
      else -> 0f
    }
  val phase2Fleshy =
    when (clampedPhase) {
      0, 1 -> 0f
      2 -> 0.80f
      3 -> 0.28f
      else -> 0f
    }
  val phase3Muscle =
    when (clampedPhase) {
      0, 1, 2 -> 0f
      3 -> 0.86f
      else -> 0.26f
    }
  val phase4Hollow = if (clampedPhase >= 4) 0.90f else 0f

  val baseExcision =
    when {
      highestExcisionMilestone >= 6 -> 0.78f
      highestExcisionMilestone == 5 -> 0.62f
      highestExcisionMilestone == 4 -> 0.44f
      highestExcisionMilestone == 3 -> 0.26f
      highestExcisionMilestone == 2 && cycle >= 2 -> 0.12f
      else -> 0f
    }
  val phaseScale =
    when (clampedPhase) {
      0 -> 0.42f
      1 -> 0.58f
      2 -> 0.78f
      else -> 1.0f
    }
  val lateExcision = (baseExcision * phaseScale).coerceIn(0f, 0.82f)

  return WallLayerAlphas(
    phase1Tile = phase1Tile,
    phase2Fleshy = phase2Fleshy,
    phase3Muscle = phase3Muscle,
    phase4Hollow = phase4Hollow,
    lateExcision = lateExcision,
  )
}

/**
 * Five-stage progressive biomechanical chamber wall backdrop (Phases 0..4 + Excisions III..VI):
 * - Phase 0: Cold sterile fluorescent slate masonry (`img_wall_phase1`, cool clinical filter).
 * - Phase 1: Damp fractured charcoal tile with dark corner seepage (`img_wall_phase1`).
 * - Phase 2: Lichen-stained tiles splitting with nascent fibrous veins (`img_wall_fleshy`).
 * - Phase 3: Buckling tiles heaving apart over glistening muscular fascia & sinew (`img_wall_phase3`).
 * - Phase 4: Calcified rib arches & deep arterial hollow pulsing with a cardiac double-beat (`img_wall_phase4`).
 * - Late Excisions (M3..M6): Progressively overlays surgical bone-thread scar seams, riveted iron,
 *   and ghostly ossified membranes (`img_wall_excision_late`) so late runs visibly transform.
 *
 * Replaces primitive wireframe grids, flat diagonal scar strokes, and spider-web lines with
 * photographic textures, volumetric shadowed arterial root-cords around the valve recess,
 * recessed keloid flank clefts, and heavy architectural Excision mutations.
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

  val layers =
    remember(state.phase, state.highestExcisionMilestone, state.cycle) {
      computeWallLayerAlphas(state.phase, state.highestExcisionMilestone, state.cycle)
    }

  val phase0ColdFilter =
    remember {
      ColorFilter.colorMatrix(
        ColorMatrix(
          floatArrayOf(
            0.78f, 0f, 0f, 0f, -2f,
            0f, 0.85f, 0f, 0f, 1f,
            0f, 0f, 0.94f, 0f, 5f,
            0f, 0f, 0f, 1f, 0f,
          )
        )
      )
    }

  val phaseOrganicFilter =
    remember(state.phase) {
      when (state.phase) {
        0 -> phase0ColdFilter
        1 -> null
        2 ->
          ColorFilter.colorMatrix(
            ColorMatrix(
              floatArrayOf(
                0.96f, 0f, 0f, 0f, 0f,
                0f, 1.00f, 0f, 0f, 1f,
                0f, 0f, 0.90f, 0f, -2f,
                0f, 0f, 0f, 1f, 0f,
              )
            )
          )
        3 ->
          ColorFilter.colorMatrix(
            ColorMatrix(
              floatArrayOf(
                1.03f, 0f, 0f, 0f, 2f,
                0f, 0.92f, 0f, 0f, -2f,
                0f, 0f, 0.94f, 0f, -1f,
                0f, 0f, 0f, 1f, 0f,
              )
            )
          )
        else ->
          ColorFilter.colorMatrix(
            ColorMatrix(
              floatArrayOf(
                1.05f, 0f, 0f, 0f, 3f,
                0f, 0.88f, 0f, 0f, -3f,
                0f, 0f, 0.88f, 0f, -3f,
                0f, 0f, 0f, 1f, 0f,
              )
            )
          )
      }
    }

  val scratchPath = remember { Path() }

  Box(modifier = modifier.fillMaxSize()) {
    // Layer 1: Damp cracked charcoal tile (Phases 0..3)
    if (layers.phase1Tile > 0f) {
      Image(
        painter = painterResource(id = R.drawable.img_wall_phase1),
        contentDescription = stringResource(R.string.art_wall_phase1_cd),
        contentScale = ContentScale.Crop,
        colorFilter = if (state.phase == 0) phase0ColdFilter else null,
        modifier = Modifier.fillMaxSize().alpha(layers.phase1Tile).testTag("chamber_wall_art"),
      )
    }

    // Layer 2: Phase 2 lichen & creeping fibrous veins inside grout seams
    if (layers.phase2Fleshy > 0f) {
      Image(
        painter = painterResource(id = R.drawable.img_wall_fleshy),
        contentDescription = stringResource(R.string.art_wall_fleshy_cd),
        contentScale = ContentScale.Crop,
        colorFilter = phaseOrganicFilter,
        modifier =
          Modifier.fillMaxSize().alpha(layers.phase2Fleshy).testTag("chamber_wall_fleshy_art"),
      )
    }

    // Layer 3: Phase 3 buckling masonry splitting over glistening muscular fascia & sinew
    if (layers.phase3Muscle > 0f) {
      Image(
        painter = painterResource(id = R.drawable.img_wall_phase3),
        contentDescription = stringResource(R.string.art_wall_phase3_cd),
        contentScale = ContentScale.Crop,
        colorFilter = phaseOrganicFilter,
        modifier =
          Modifier.fillMaxSize()
            .graphicsLayer {
              val breath = breathPhaseProvider()
              val s = 1.015f + breath * 0.018f
              scaleX = s
              scaleY = s
            }
            .alpha(layers.phase3Muscle)
            .testTag("chamber_wall_phase3_art"),
      )
    }

    // Layer 4: Phase 4 calcified rib-arches & arterial hollow breathing with cardiac double-beat
    if (layers.phase4Hollow > 0f) {
      Image(
        painter = painterResource(id = R.drawable.img_wall_phase4),
        contentDescription = stringResource(R.string.art_wall_phase4_cd),
        contentScale = ContentScale.Crop,
        colorFilter = phaseOrganicFilter,
        modifier =
          Modifier.fillMaxSize()
            .graphicsLayer {
              val breath = breathPhaseProvider()
              val arterial = arterialPulsePhaseProvider()
              val lub = exp(-((arterial - 0.18f) * (arterial - 0.18f)) / 0.005f)
              val s = 1.02f + breath * 0.018f + lub * 0.016f
              scaleX = s
              scaleY = s
            }
            .alpha(layers.phase4Hollow)
            .testTag("chamber_wall_phase4_art"),
      )
    }

    // Layer 5: Late-Excision (M3..M6) scarred surgical sutures, riveted iron & ossified membranes
    if (layers.lateExcision > 0f) {
      Image(
        painter = painterResource(id = R.drawable.img_wall_excision_late),
        contentDescription = stringResource(R.string.art_wall_excision_late_cd),
        contentScale = ContentScale.Crop,
        modifier =
          Modifier.fillMaxSize()
            .graphicsLayer {
              val breath = breathPhaseProvider()
              val s = 1.01f + breath * 0.014f
              scaleX = s
              scaleY = s
            }
            .alpha(layers.lateExcision)
            .testTag("chamber_wall_excision_late_art"),
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

      // 1. Atmospheric vignette & central readability recess (keeps HUD crisp while letting edges stay dramatic)
      val midTint =
        when (state.phase) {
          0 -> colorScheme.background.copy(alpha = 0.42f)
          1 -> colorScheme.background.copy(alpha = 0.28f)
          2 -> colorScheme.secondaryContainer.copy(alpha = 0.18f)
          3 -> colorScheme.tertiaryContainer.copy(alpha = 0.20f)
          else -> colorScheme.tertiaryContainer.copy(alpha = 0.24f + doubleBeat * 0.08f)
        }
      drawRect(
        brush =
          Brush.verticalGradient(
            colors =
              listOf(
                colorScheme.background.copy(alpha = 0.72f),
                midTint,
                colorScheme.background.copy(alpha = 0.66f),
              )
          )
      )

      // Subtle central dark shadow halo around counter & valve so typography never loses contrast
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(colorScheme.background.copy(alpha = 0.44f), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.28f),
            radius = size.minDimension * 0.52f,
          ),
        radius = size.minDimension * 0.52f,
        center = Offset(size.width * 0.5f, size.height * 0.28f),
      )

      // 2. Phase 0 only: sterile cold overhead fluorescent wash & faint architectural slate mortar
      if (state.phase == 0) {
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(colorScheme.onBackground.copy(alpha = 0.07f), Color.Transparent),
              center = Offset(size.width * 0.5f, 0f),
              radius = size.width * 0.68f,
            ),
          radius = size.width * 0.68f,
          center = Offset(size.width * 0.5f, 0f),
        )
        val step = 42.dp.toPx()
        val sterileLine = atmosphere.hairline.copy(alpha = 0.12f)
        var x = step
        while (x < size.width) {
          drawLine(sterileLine, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
          x += step
        }
        var y = step
        while (y < size.height) {
          drawLine(sterileLine, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
          y += step
        }
      }

      // 3. Phase 1+: Deep damp corner moisture seepage stains
      if (state.phase >= 1) {
        val stainRadius = size.minDimension * (0.28f + state.phase * 0.045f)
        val stainColor =
          if (state.phase >= 3) colorScheme.tertiaryContainer.copy(alpha = 0.36f)
          else colorScheme.background.copy(alpha = 0.44f)
        for (corner in listOf(Offset(0f, size.height), Offset(size.width, size.height * 0.88f))) {
          drawCircle(
            brush = Brush.radialGradient(listOf(stainColor, Color.Transparent), corner, stainRadius),
            radius = stainRadius,
            center = corner,
          )
        }
      }

      // 4. Phase 2+: Fine damp condensation glints sliding quietly down the wet masonry
      if (state.phase >= 2) {
        val streakAlpha = if (state.phase >= 4) 0.10f else 0.15f
        val streakColor = colorScheme.primary.copy(alpha = streakAlpha)
        for (s in 1..4) {
          val sx = size.width * (0.12f + s * 0.19f)
          val topY = size.height * (0.10f + (s % 2) * 0.16f)
          val travel =
            if (state.phase >= 3) ((creep + s * 0.27f) % 1f) * size.height * 0.24f
            else breath * size.height * 0.05f
          val endY = (topY + size.height * 0.14f + travel).coerceAtMost(size.height * 0.88f)
          drawLine(
            brush =
              Brush.verticalGradient(
                colors = listOf(Color.Transparent, streakColor, colorScheme.primary.copy(alpha = streakAlpha * 1.4f)),
                startY = topY,
                endY = endY,
              ),
            start = Offset(sx, topY),
            end = Offset(sx, endY),
            strokeWidth = 1.0.dp.toPx(),
            cap = StrokeCap.Round,
          )
          drawCircle(
            color = colorScheme.primary.copy(alpha = streakAlpha * 1.6f),
            radius = 1.4.dp.toPx(),
            center = Offset(sx, endY),
          )
        }
      }

      // 5. Phase 3+: Soft volumetric peristaltic subsurface swells & wet pore glints along outer flanks
      if (state.phase >= 3) {
        val creepAngle = creep * 2.0 * PI
        for (p in 0 until 4) {
          val phaseShift = (creep + p * 0.25f) % 1f
          val swell = sin(phaseShift * PI).toFloat()
          val px =
            if (p % 2 == 0) size.width * (0.10f + swell * 0.04f)
            else size.width * (0.90f - swell * 0.04f)
          val py = size.height * (0.22f + phaseShift * 0.56f)
          val haloR = (16f + swell * 12f + doubleBeat * 6f).dp.toPx()
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    colorScheme.tertiary.copy(alpha = (0.14f + doubleBeat * 0.10f) * swell),
                    colorScheme.background.copy(alpha = 0.22f * swell),
                    Color.Transparent,
                  ),
                center = Offset(px, py),
                radius = haloR,
              ),
            radius = haloR,
            center = Offset(px, py),
          )
          // Specular wet membrane bead
          val beadOffset = (sin(creepAngle + p * 1.3) * 1.5).toFloat().dp.toPx()
          drawCircle(
            color = colorScheme.onBackground.copy(alpha = 0.16f * swell),
            radius = 1.4.dp.toPx(),
            center = Offset(px + beadOffset, py - 1.5.dp.toPx()),
          )
        }
      }

      // 6. Phase 4: Soft subsurface cardiac arterial illumination (no harsh wireframe ring)
      if (state.phase >= 4 && doubleBeat > 0.03f) {
        val waveR = size.minDimension * (0.22f + arterialCycle * 0.62f)
        drawCircle(
          brush =
            Brush.radialGradient(
              colors =
                listOf(
                  colorScheme.tertiary.copy(alpha = doubleBeat * 0.24f),
                  colorScheme.tertiaryContainer.copy(alpha = doubleBeat * 0.14f),
                  Color.Transparent,
                ),
              center = valveCenter,
              radius = waveR,
            ),
          radius = waveR,
          center = valveCenter,
        )
      }

      // 7. Volumetric muscular arterial root-cords anchoring the recessed valve into the living wall (Phases 2..4)
      if (state.phase >= 2) {
        val cordCount = (4 + state.phase + min(state.graftedTendrils, 3)).coerceAtMost(10)
        val creepAngle = creep * 2.0 * PI
        val rootStartR = 60.dp.toPx()
        val maxReach = size.minDimension * (0.25f + (state.phase - 1) * 0.065f + doubleBeat * 0.025f)
        val bodyAlpha = (0.48f + (state.phase - 2) * 0.16f + doubleBeat * 0.14f).coerceIn(0f, 0.86f)

        for (i in 0 until cordCount) {
          val baseAng = i * (2.0 * PI / cordCount) + 0.35
          val sway = sin(creepAngle + i * 1.37) * 0.055 + (breath - 0.5) * 0.04
          val ang = baseAng + sway
          val dx = cos(ang).toFloat()
          val dy = sin(ang).toFloat()
          val px = -dy
          val py = dx
          val bendDir = if (i % 2 == 0) 1f else -1f
          val cordReach = maxReach * (0.78f + (i % 3) * 0.14f)

          val start = Offset(valveCenter.x + dx * rootStartR, valveCenter.y + dy * rootStartR)
          val ctrl1 =
            Offset(
              valveCenter.x + dx * (rootStartR + cordReach * 0.38f) + px * (11.dp.toPx() * bendDir),
              valveCenter.y + dy * (rootStartR + cordReach * 0.38f) + py * (11.dp.toPx() * bendDir),
            )
          val ctrl2 =
            Offset(
              valveCenter.x + dx * (rootStartR + cordReach * 0.72f) - px * (7.dp.toPx() * bendDir),
              valveCenter.y + dy * (rootStartR + cordReach * 0.72f) - py * (7.dp.toPx() * bendDir),
            )
          val end =
            Offset(
              valveCenter.x + dx * (rootStartR + cordReach),
              valveCenter.y + dy * (rootStartR + cordReach),
            )

          val thickness = (2.8f + (state.phase - 2) * 1.1f + doubleBeat * 0.9f).dp.toPx()
          scratchPath.rewind()
          scratchPath.moveTo(start.x, start.y)
          scratchPath.cubicTo(ctrl1.x, ctrl1.y, ctrl2.x, ctrl2.y, end.x, end.y)

          // Deep recessed cavity shadow beneath the organic cord
          drawPath(
            path = scratchPath,
            brush =
              Brush.linearGradient(
                colors =
                  listOf(
                    colorScheme.background.copy(alpha = 0.85f),
                    colorScheme.background.copy(alpha = 0.52f),
                    Color.Transparent,
                  ),
                start = start,
                end = end,
              ),
            style = Stroke(width = thickness * 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
          )
          // Bruised muscular oxblood body fading smoothly into the photographic wall
          drawPath(
            path = scratchPath,
            brush =
              Brush.linearGradient(
                colors =
                  listOf(
                    colorScheme.tertiaryContainer.copy(alpha = bodyAlpha),
                    colorScheme.tertiary.copy(alpha = bodyAlpha * 0.78f),
                    Color.Transparent,
                  ),
                start = start,
                end = end,
              ),
            style = Stroke(width = thickness, cap = StrokeCap.Round, join = StrokeJoin.Round),
          )
          // Fine glossy wet specular ridge along the upper flesh crest
          drawPath(
            path = scratchPath,
            brush =
              Brush.linearGradient(
                colors =
                  listOf(
                    colorScheme.onBackground.copy(alpha = 0.16f + doubleBeat * 0.10f),
                    colorScheme.tertiary.copy(alpha = 0.18f),
                    Color.Transparent,
                  ),
                start = start,
                end = end,
              ),
            style = Stroke(width = (thickness * 0.28f).coerceAtLeast(1f), cap = StrokeCap.Round),
          )
        }
      }

      // 8. Permanent Scar Tissue: Recessed organic keloid suture clefts tucked along chamber flanks (Cycles 1+)
      if (state.scarTissue > 0L) {
        val cleftCount = min(state.scarTissue.toInt(), 4)
        val flankAnchors =
          listOf(
            Triple(0.02f, 0.24f, 1f),
            Triple(0.98f, 0.30f, -1f),
            Triple(0.02f, 0.74f, 1f),
            Triple(0.98f, 0.70f, -1f),
          )
        for (s in 0 until cleftCount) {
          val (fx, fy, inward) = flankAnchors[s]
          val ax = size.width * fx
          val ay = size.height * fy
          val len = size.width * 0.17f
          val endX = ax + inward * len
          val endY = ay + size.height * (if (s % 2 == 0) 0.065f else -0.055f)
          val midX = (ax + endX) * 0.5f + inward * 6.dp.toPx()
          val midY = (ay + endY) * 0.5f - 8.dp.toPx()

          scratchPath.rewind()
          scratchPath.moveTo(ax, ay)
          scratchPath.quadraticTo(midX, midY, endX, endY)

          // Recessed bruised shadow trough
          drawPath(
            path = scratchPath,
            brush =
              Brush.linearGradient(
                colors = listOf(colorScheme.background.copy(alpha = 0.85f), Color.Transparent),
                start = Offset(ax, ay),
                end = Offset(endX, endY),
              ),
            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round),
          )
          // Deep keloid seam line
          drawPath(
            path = scratchPath,
            brush =
              Brush.linearGradient(
                colors = listOf(colorScheme.tertiary.copy(alpha = 0.62f), Color.Transparent),
                start = Offset(ax, ay),
                end = Offset(endX, endY),
              ),
            style = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round),
          )
          // Curved surgical bone-thread staples across the cleft
          for (st in 1..4) {
            val t = st / 5.2f
            val omt = 1f - t
            val sx = omt * omt * ax + 2f * omt * t * midX + t * t * endX
            val sy = omt * omt * ay + 2f * omt * t * midY + t * t * endY
            val stapleAlpha = (1f - t * 0.75f) * 0.58f
            drawLine(
              color = colorScheme.background.copy(alpha = stapleAlpha),
              start = Offset(sx - 2.dp.toPx(), sy - 4.5.dp.toPx()),
              end = Offset(sx + 2.dp.toPx(), sy + 4.5.dp.toPx()),
              strokeWidth = 2.2.dp.toPx(),
              cap = StrokeCap.Round,
            )
            drawLine(
              color = colorScheme.onBackground.copy(alpha = stapleAlpha * 0.85f),
              start = Offset(sx - 1.8.dp.toPx(), sy - 4.dp.toPx()),
              end = Offset(sx + 1.8.dp.toPx(), sy + 4.dp.toPx()),
              strokeWidth = 1.0.dp.toPx(),
              cap = StrokeCap.Round,
            )
          }
        }
      }

      // 9. Excision II-V physical chamber mutations (heavy floor sump basin, chiseled aperture, clamp, ossified webs)
      drawExcisionChamberMutations(
        state = state,
        breath = breath,
        wallCreep = creep,
        primaryColor = colorScheme.primary,
        tertiaryColor = colorScheme.tertiary,
        outlineColor = colorScheme.outline,
        backgroundColor = colorScheme.background,
        boneColor = colorScheme.onBackground,
        scratchPath = scratchPath,
      )

      // Neighbor Aperture Echo acoustic shimmer rings expanding from the chiseled upper-right breach
      if (state.highestExcisionMilestone >= 3 && echoRipple in 0.001f..0.999f) {
        val slitOrigin = Offset(size.width * 0.86f, size.height * 0.22f)
        val rippleAlpha = (1f - echoRipple) * 0.62f
        val r1 = 10.dp.toPx() + echoRipple * size.width * 0.32f
        val r2 = 4.dp.toPx() + echoRipple * size.width * 0.20f
        drawCircle(
          color = colorScheme.primary.copy(alpha = rippleAlpha),
          radius = r1,
          center = slitOrigin,
          style = Stroke(width = 1.6.dp.toPx()),
        )
        drawCircle(
          color = colorScheme.primary.copy(alpha = rippleAlpha * 0.55f),
          radius = r2,
          center = slitOrigin,
          style = Stroke(width = 1.0.dp.toPx()),
        )
      }
    }
  }
}
