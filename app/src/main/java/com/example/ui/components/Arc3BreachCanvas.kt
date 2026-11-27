package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.ActiveBreachRupture
import com.example.game.Arc3Stratum
import com.example.game.BreachRuptureType
import com.example.game.OrganGameState
import com.example.game.SpecimenJar
import com.example.game.arc3WrenchFractureYield
import com.example.ui.theme.ArterialCrimson
import com.example.ui.theme.ArterialShadow
import com.example.ui.theme.BloodSoakedPaper
import com.example.ui.theme.BrightMeniscus
import com.example.ui.theme.DeepClotTide
import com.example.ui.theme.FreshSeepage
import java.text.NumberFormat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

private data class BreachResidue(val id: Long, val label: String, val xDriftDp: Float)

/**
 * Interactive Arc 3 ("The Unmaking") breach canvas:
 * Progressive blood-seeping wall photography, permanent bleeding scar gashes, intact vs shattered
 * specimen jars reflecting real Arc 1 shelves, pinned Arc 2 Memory Wedges, counter-clockwise
 * wrenching valve -> sheared Open Seam, feathered macro wall ruptures, and rising animated
 * volumetric Crimson Flood.
 */
@Composable
internal fun Arc3BreachCanvas(
  state: OrganGameState,
  breathPhaseProvider: () -> Float,
  wavePhaseProvider: () -> Float,
  onWrenchBreach: () -> Unit,
  onTearRupture: (Long) -> Unit,
  onEmitThematicDrip: (eventKey: String, text: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val breach = state.arc3
  val colorScheme = MaterialTheme.colorScheme
  val numberFormat = remember { NumberFormat.getIntegerInstance() }
  val residues = remember { mutableStateListOf<BreachResidue>() }
  var nextResidueId by remember { mutableLongStateOf(1L) }

  // Counter-clockwise wrench angle (reverses Arc 1's clockwise obedience)
  var targetWheelAngle by remember { mutableFloatStateOf(0f) }
  val wheelAngle = remember { Animatable(0f) }
  LaunchedEffect(targetWheelAngle) {
    wheelAngle.animateTo(
      targetValue = targetWheelAngle,
      animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
    )
  }

  val animatedFlood by
    animateFloatAsState(
      targetValue = breach.floodLevel.coerceIn(0f, 1f),
      animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
      label = "crimson_flood_level",
    )

  val dripClot = stringResource(R.string.drip_arc3_rupture_clot)
  val dripRib = stringResource(R.string.drip_arc3_rupture_rib)
  val dripSuture = stringResource(R.string.drip_arc3_rupture_suture)
  val dripWell = stringResource(R.string.drip_arc3_rupture_well)

  val wrenchYield = floor(state.arc3WrenchFractureYield()).toLong().coerceAtLeast(1L)
  val yieldLabel = "+${numberFormat.format(wrenchYield)}"

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.small)
        .clipToBounds()
        .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.small)
        .background(ArterialShadow)
        .testTag("arc3_breach_canvas"),
    contentAlignment = Alignment.Center,
  ) {
    val canvasW = maxWidth
    val canvasH = maxHeight

    // 1. Progressive blood-seeping wall photography (Early -> Deep -> Flood)
    val deepBlend =
      when {
        breach.isInFinale -> 1f
        breach.currentStratum >= 3 -> ((breach.currentStratum - 2) * 0.45f).coerceIn(0f, 1f)
        else -> 0f
      }
    Image(
      painter = painterResource(id = R.drawable.img_wall_arc3_breach_early),
      contentDescription = stringResource(R.string.art_wall_arc3_breach_early_cd),
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize().alpha(0.78f * (1f - deepBlend * 0.65f)).testTag("art_arc3_breach_wall"),
    )
    if (deepBlend > 0.01f) {
      Image(
        painter = painterResource(id = R.drawable.img_wall_arc3_breach_deep),
        contentDescription = stringResource(R.string.art_wall_arc3_breach_deep_cd),
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().alpha(deepBlend * 0.88f),
      )
    }

    // 2. Permanent unstitched bleeding scar slashes & clamped clotting strain vignette
    Canvas(modifier = Modifier.fillMaxSize()) {
      val breath = breathPhaseProvider()
      val scarCount = breach.unstitchedScarCount.coerceIn(0, 6)
      for (i in 0 until scarCount) {
        val xStart = size.width * (0.12f + (i % 3) * 0.32f)
        val yStart = size.height * (0.14f + (i / 3) * 0.36f)
        val xEnd = xStart + size.width * (if (i % 2 == 0) 0.22f else -0.18f)
        val yEnd = yStart + size.height * 0.26f
        drawLine(
          color = DeepClotTide.copy(alpha = 0.85f),
          start = Offset(xStart, yStart),
          end = Offset(xEnd, yEnd),
          strokeWidth = (5f + breath * 2f).dp.toPx(),
          cap = StrokeCap.Round,
        )
        drawLine(
          color = BrightMeniscus.copy(alpha = 0.52f + breath * 0.22f),
          start = Offset(xStart, yStart),
          end = Offset(xEnd, yEnd),
          strokeWidth = 1.8.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }

      // Clotting contraction halo around perimeter when clotting actively pulls back
      if (breach.isActivelyClotting) {
        drawRect(
          brush =
            Brush.radialGradient(
              colors =
                listOf(
                  Color.Transparent,
                  FreshSeepage.copy(alpha = 0.26f + breath * 0.16f),
                  DeepClotTide.copy(alpha = 0.72f),
                ),
              center = Offset(size.width * 0.5f, size.height * 0.5f),
              radius = size.maxDimension * 0.62f,
            )
        )
      }

      // Strata IV-V inward-throbbing tunnel-vision vignette
      val blur = breach.telemetryBlurFactor
      if (blur > 0.05f) {
        drawRect(
          brush =
            Brush.radialGradient(
              colorStops =
                arrayOf(
                  0.0f to Color.Transparent,
                  (0.55f - blur * 0.15f) to Color.Transparent,
                  1.0f to ArterialShadow.copy(alpha = (0.45f + blur * 0.40f).coerceIn(0f, 0.90f)),
                ),
              center = Offset(size.width * 0.5f, size.height * 0.5f),
              radius = size.maxDimension * (0.68f - breath * 0.04f),
            )
        )
      }
    }

    // 3. Pinned Arc 2 Memory Wedges jammed along the upper breach seam
    if (breach.lockedWedges.isNotEmpty()) {
      Row(
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp, start = 10.dp, end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Arc3Stratum.entries.forEach { stratum ->
          val pinned = stratum in breach.lockedWedges
          Surface(
            shape = RoundedCornerShape(2.dp),
            color =
              if (pinned) ArterialCrimson.copy(alpha = 0.85f)
              else ArterialShadow.copy(alpha = 0.55f),
            border =
              BorderStroke(
                width = 1.dp,
                color = if (pinned) BrightMeniscus else colorScheme.outlineVariant.copy(alpha = 0.45f),
              ),
            modifier = Modifier.testTag("arc3_canvas_wedge_${stratum.number}"),
          ) {
            Text(
              text = if (pinned) "W${stratum.number}" else "·",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
              color = if (pinned) BloodSoakedPaper else colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }
      }
    }

    // 4. Lower left & right Specimen Jar Shelves showing intact vs shattered glass
    Arc3ShatteredJarShelves(
      state = state,
      modifier = Modifier.fillMaxSize(),
    )

    // 5. Central focal breach trigger: Reverse-wrenching iron valve (Strata I-III) -> Sheared Open Seam (Strata IV-V)
    if (!breach.isInFinale) {
      Arc3FocalBreachTrigger(
        isValveSheared = breach.isValveSheared,
        yieldLabel = yieldLabel,
        momentumCharge = breach.momentumCharge.toFloat(),
        wheelRotationProvider = { wheelAngle.value },
        breathPhaseProvider = breathPhaseProvider,
        onWrench = {
          targetWheelAngle -= 34f
          val drift = ((nextResidueId * 23L) % 44L - 22L).toFloat()
          residues.add(BreachResidue(nextResidueId++, yieldLabel, drift))
          if (residues.size > 6) residues.removeAt(0)
          onWrenchBreach()
        },
      )
    }

    // 6. Floating crimson fracture residues
    residues.forEach { res ->
      BreachDissolvingResidue(
        residue = res,
        onFinished = { residues.removeAll { it.id == res.id } },
      )
    }

    // 7. Rising animated Volumetric Crimson Flood layer (non-blocking)
    CrimsonFloodOverlay(
      floodLevel = animatedFlood,
      wavePhaseProvider = wavePhaseProvider,
      breathPhaseProvider = breathPhaseProvider,
      modifier = Modifier.fillMaxSize().testTag("arc3_crimson_flood_layer"),
    )

    // 8. Active urgent wall ruptures floating above the flood line so they remain effortlessly tappable
    breach.activeRuptures.forEach { rupture ->
      val xDp = canvasW * rupture.xFraction - 38.dp
      val yDp = canvasH * rupture.yFraction - 38.dp
      BreachRuptureNode(
        rupture = rupture,
        breathPhaseProvider = breathPhaseProvider,
        onTear = {
          val dripText =
            when (rupture.type) {
              BreachRuptureType.CLOTTING_SEAM -> dripClot
              BreachRuptureType.SPLINTERED_RIB -> dripRib
              BreachRuptureType.TORN_SUTURE -> dripSuture
              BreachRuptureType.ARTERIAL_WELL -> dripWell
            }
          onEmitThematicDrip("arc3_rupture_${rupture.type.id}", dripText)
          onTearRupture(rupture.id)
        },
        modifier =
          Modifier.align(Alignment.TopStart)
            .offset(
              x = xDp.coerceIn(6.dp, (canvasW - 82.dp).coerceAtLeast(6.dp)),
              y = yDp.coerceIn(10.dp, (canvasH - 82.dp).coerceAtLeast(10.dp)),
            ),
      )
    }
  }
}

@Composable
internal fun CrimsonFloodOverlay(
  floodLevel: Float,
  wavePhaseProvider: () -> Float,
  breathPhaseProvider: () -> Float,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val level = floodLevel.coerceIn(0f, 1f)
    if (level <= 0.005f) return@Canvas

    val wave = wavePhaseProvider()
    val breath = breathPhaseProvider()
    val surfaceY = size.height * (1f - level)
    val amp = ((5f + level * 6f) * (1f - (level - 0.92f).coerceAtLeast(0f) * 8f).coerceIn(0.2f, 1f)).dp.toPx()

    // Back darker tide wave
    val backPath =
      Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, surfaceY - amp * 0.4f)
        val steps = 20
        for (s in 0..steps) {
          val u = s.toFloat() / steps
          val x = size.width * u
          val y = surfaceY - amp * 0.35f + sin(u * 2f * PI.toFloat() + wave * 2f * PI.toFloat() + 1.1f) * amp * 0.7f
          lineTo(x, y)
        }
        lineTo(size.width, size.height)
        close()
      }
    drawPath(
      path = backPath,
      color = DeepClotTide.copy(alpha = (0.58f + level * 0.32f).coerceIn(0f, 0.92f)),
    )

    // Front wet arterial body wave
    val frontPath =
      Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, surfaceY)
        val steps = 24
        for (s in 0..steps) {
          val u = s.toFloat() / steps
          val x = size.width * u
          val y =
            surfaceY +
              sin(u * 2.4f * PI.toFloat() - wave * 2f * PI.toFloat()) * amp +
              cos(u * 4.2f * PI.toFloat() + wave * 2f * PI.toFloat()) * amp * 0.30f
          lineTo(x, y)
        }
        lineTo(size.width, size.height)
        close()
      }
    drawPath(
      path = frontPath,
      brush =
        Brush.verticalGradient(
          colors =
            listOf(
              ArterialCrimson.copy(alpha = (0.72f + level * 0.22f).coerceIn(0f, 0.94f)),
              FreshSeepage.copy(alpha = (0.82f + level * 0.14f).coerceIn(0f, 0.96f)),
              DeepClotTide.copy(alpha = 0.96f),
            ),
          startY = surfaceY.coerceAtLeast(0f),
          endY = size.height,
        ),
    )

    // Glistening bright meniscus surface rim
    if (level < 0.985f) {
      val meniscusPath =
        Path().apply {
          val steps = 24
          for (s in 0..steps) {
            val u = s.toFloat() / steps
            val x = size.width * u
            val y =
              surfaceY +
                sin(u * 2.4f * PI.toFloat() - wave * 2f * PI.toFloat()) * amp +
                cos(u * 4.2f * PI.toFloat() + wave * 2f * PI.toFloat()) * amp * 0.30f
            if (s == 0) moveTo(x, y) else lineTo(x, y)
          }
        }
      drawPath(
        path = meniscusPath,
        color = BrightMeniscus.copy(alpha = 0.55f + breath * 0.25f),
        style = Stroke(width = 2.dp.toPx()),
      )
    }

    // Slow drifting submerged depth currents
    val currentCount = (3 + (level * 5).roundToInt()).coerceAtMost(8)
    for (k in 0 until currentCount) {
      val cy = surfaceY + (size.height - surfaceY) * ((k + 0.5f) / currentCount)
      val cx = size.width * (((wave * (0.35f + k * 0.08f)) + k * 0.21f) % 1f)
      drawOval(
        color =
          if (k % 2 == 0) BrightMeniscus.copy(alpha = 0.10f + breath * 0.06f)
          else ArterialShadow.copy(alpha = 0.24f),
        topLeft = Offset(cx - 42.dp.toPx(), cy - 5.dp.toPx()),
        size = Size(84.dp.toPx(), 10.dp.toPx()),
      )
    }
  }
}

@Composable
private fun Arc3FocalBreachTrigger(
  isValveSheared: Boolean,
  yieldLabel: String,
  momentumCharge: Float,
  wheelRotationProvider: () -> Float,
  breathPhaseProvider: () -> Float,
  onWrench: () -> Unit,
) {
  val colorScheme = MaterialTheme.colorScheme
  val hapticFeedback = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val pressOffset by
    animateDpAsState(
      targetValue = if (isPressed) 5.dp else 0.dp,
      animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
      label = "arc3_wrench_offset",
    )
  val scale by
    animateFloatAsState(
      targetValue = if (isPressed) 0.94f else 1f + momentumCharge * 0.04f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
      label = "arc3_wrench_scale",
    )

  val wrenchCd = stringResource(R.string.arc3_wrench_cd)
  val diameter = 140.dp

  Box(
    modifier =
      Modifier.size(diameter + 36.dp).graphicsLayer {
        scaleX = scale
        scaleY = scale
      },
    contentAlignment = Alignment.Center,
  ) {
    // Bleeding socket aura & momentum tension ring
    Canvas(modifier = Modifier.fillMaxSize()) {
      val breath = breathPhaseProvider()
      val c = Offset(size.width * 0.5f, size.height * 0.5f)
      val outerR = size.minDimension * 0.49f
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                ArterialCrimson.copy(alpha = 0.42f + momentumCharge * 0.28f + breath * 0.14f),
                DeepClotTide.copy(alpha = 0.36f),
                Color.Transparent,
              ),
            center = c,
            radius = outerR,
          ),
        radius = outerR,
        center = c,
      )
      if (momentumCharge > 0.02f) {
        drawArc(
          color = BrightMeniscus.copy(alpha = 0.72f),
          startAngle = -90f,
          sweepAngle = -360f * momentumCharge.coerceIn(0f, 1f),
          useCenter = false,
          topLeft = Offset(c.x - outerR * 0.86f, c.y - outerR * 0.86f),
          size = Size(outerR * 1.72f, outerR * 1.72f),
          style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round),
        )
      }
    }

    Surface(
      modifier =
        Modifier.size(diameter)
          .offset(y = pressOffset)
          .minimumInteractiveComponentSize()
          .clip(CircleShape)
          .testTag("arc3_wrench_button")
          .semantics { contentDescription = wrenchCd }
          .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, color = BrightMeniscus),
            role = Role.Button,
            onClick = {
              hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
              onWrench()
            },
          ),
      shape = CircleShape,
      color = if (isPressed) FreshSeepage else ArterialShadow,
      border =
        BorderStroke(
          width = if (isValveSheared) 2.dp else 1.5.dp,
          color = if (isPressed || isValveSheared) BrightMeniscus else ArterialCrimson,
        ),
    ) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!isValveSheared) {
          // Counter-clockwise shuddering iron valve
          Box(
            modifier =
              Modifier.fillMaxSize().graphicsLayer {
                rotationZ = wheelRotationProvider()
              }
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_recessed_valve),
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize().alpha(if (isPressed) 0.44f else 0.58f),
            )
          }
        } else {
          // Sheared Open Seam pulsing arterial red in Strata IV-V
          Image(
            painter = painterResource(id = R.drawable.img_rupture_arterial_well),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.76f),
          )
        }

        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(
                Brush.radialGradient(
                  colors =
                    listOf(
                      ArterialShadow.copy(alpha = if (isPressed) 0.58f else 0.66f),
                      FreshSeepage.copy(alpha = 0.48f),
                    )
                )
              )
        )

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Text(
            text = yieldLabel,
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
            color = BloodSoakedPaper,
          )
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text =
              stringResource(
                if (isValveSheared) R.string.arc3_tear_open_action else R.string.arc3_wrench_action
              ),
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.6.sp),
            color = BrightMeniscus,
          )
        }
      }
    }
  }
}

@Composable
private fun Arc3ShatteredJarShelves(
  state: OrganGameState,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier) {
    Arc3ShelfRow(
      jars = FOUNDATIONAL_SHELF_JARS,
      state = state,
      shelfTag = "arc3_jar_shelf_left",
      modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 6.dp),
    )
    Arc3ShelfRow(
      jars = DEEP_SHELF_JARS,
      state = state,
      shelfTag = "arc3_jar_shelf_right",
      modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 6.dp),
    )
  }
}

@Composable
private fun Arc3ShelfRow(
  jars: List<SpecimenJar>,
  state: OrganGameState,
  shelfTag: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.testTag(shelfTag),
    horizontalArrangement = Arrangement.spacedBy(3.dp),
    verticalAlignment = Alignment.Bottom,
  ) {
    jars.forEach { jar ->
      val shattered = state.arc3.isJarShattered(jar)
      val ownedInArc1 = jar in state.ownedJars
      Box(
        modifier =
          Modifier.size(width = 24.dp, height = 34.dp)
            .testTag(if (shattered) "arc3_jar_shattered_${jar.id}" else "arc3_jar_intact_${jar.id}"),
        contentAlignment = Alignment.BottomCenter,
      ) {
        if (!shattered) {
          Image(
            painter = painterResource(id = specimenJarDrawableRes(jar)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
              Modifier.fillMaxSize()
                .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                .alpha(if (ownedInArc1) 0.88f else 0.50f),
          )
        } else {
          // Shattered glass shards + spilled crimson brine on iron ledge
          Canvas(modifier = Modifier.fillMaxSize()) {
            drawOval(
              color = ArterialCrimson.copy(alpha = 0.72f),
              topLeft = Offset(2.dp.toPx(), size.height - 9.dp.toPx()),
              size = Size(size.width - 4.dp.toPx(), 7.dp.toPx()),
            )
            drawLine(
              color = BloodSoakedPaper.copy(alpha = 0.75f),
              start = Offset(4.dp.toPx(), size.height - 4.dp.toPx()),
              end = Offset(10.dp.toPx(), size.height - 12.dp.toPx()),
              strokeWidth = 1.4.dp.toPx(),
            )
            drawLine(
              color = BloodSoakedPaper.copy(alpha = 0.65f),
              start = Offset(size.width - 5.dp.toPx(), size.height - 5.dp.toPx()),
              end = Offset(size.width - 11.dp.toPx(), size.height - 10.dp.toPx()),
              strokeWidth = 1.2.dp.toPx(),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BreachRuptureNode(
  rupture: ActiveBreachRupture,
  breathPhaseProvider: () -> Float,
  onTear: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val hapticFeedback = LocalHapticFeedback.current
  val resId =
    when (rupture.type) {
      BreachRuptureType.CLOTTING_SEAM -> R.drawable.img_rupture_clotting_seam
      BreachRuptureType.SPLINTERED_RIB -> R.drawable.img_anomaly_chitin_rib
      BreachRuptureType.TORN_SUTURE -> R.drawable.img_rupture_clotting_seam
      BreachRuptureType.ARTERIAL_WELL -> R.drawable.img_rupture_arterial_well
    }
  val cd =
    when (rupture.type) {
      BreachRuptureType.CLOTTING_SEAM -> stringResource(R.string.arc3_rupture_clotting_seam_cd)
      BreachRuptureType.SPLINTERED_RIB -> stringResource(R.string.arc3_rupture_splintered_rib_cd)
      BreachRuptureType.TORN_SUTURE -> stringResource(R.string.arc3_rupture_torn_suture_cd)
      BreachRuptureType.ARTERIAL_WELL -> stringResource(R.string.arc3_rupture_arterial_well_cd)
    }

  Box(
    modifier =
      modifier
        .size(76.dp)
        .minimumInteractiveComponentSize()
        .graphicsLayer {
          val b = breathPhaseProvider()
          val s = 0.96f + b * 0.08f
          scaleX = s
          scaleY = s
          if (rupture.type == BreachRuptureType.TORN_SUTURE) rotationZ = 42f
        }
        .testTag("arc3_rupture_${rupture.type.id}_${rupture.id}")
        .semantics { contentDescription = cd }
        .clickable(
          role = Role.Button,
          onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onTear()
          },
        ),
    contentAlignment = Alignment.Center,
  ) {
    Box(modifier = Modifier.size(64.dp).clip(CircleShape)) {
      Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
      Canvas(modifier = Modifier.fillMaxSize()) {
        val c = Offset(size.width * 0.5f, size.height * 0.5f)
        val r = size.minDimension * 0.5f
        drawCircle(
          brush =
            Brush.radialGradient(
              colorStops =
                arrayOf(
                  0.0f to Color.Transparent,
                  0.52f to Color.Transparent,
                  0.84f to ArterialShadow.copy(alpha = 0.65f),
                  1.0f to ArterialShadow.copy(alpha = 0.96f),
                ),
              center = c,
              radius = r,
            ),
          radius = r,
          center = c,
        )
        drawCircle(
          color = BrightMeniscus.copy(alpha = 0.60f),
          radius = r - 1.5.dp.toPx(),
          center = c,
          style = Stroke(width = 1.4.dp.toPx()),
        )
      }
    }

    Text(
      text = rupture.type.actionLabel,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, letterSpacing = 1.1.sp),
      color = BloodSoakedPaper,
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .graphicsLayer {
            if (rupture.type == BreachRuptureType.TORN_SUTURE) rotationZ = -42f
          }
          .background(ArterialCrimson.copy(alpha = 0.92f), RoundedCornerShape(2.dp))
          .padding(horizontal = 5.dp, vertical = 1.dp),
    )
  }
}

@Composable
private fun BreachDissolvingResidue(residue: BreachResidue, onFinished: () -> Unit) {
  val progress = remember { Animatable(0f) }
  LaunchedEffect(residue.id) {
    progress.animateTo(1f, tween(durationMillis = 560, easing = FastOutSlowInEasing))
    onFinished()
  }
  val yOffsetDp = -(58f + progress.value * 58f)
  val alpha = (1f - progress.value) * 0.85f
  Text(
    text = residue.label,
    style = MaterialTheme.typography.titleMedium,
    color = BrightMeniscus,
    modifier =
      Modifier.offset { IntOffset(residue.xDriftDp.dp.roundToPx(), yOffsetDp.dp.roundToPx()) }
        .alpha(alpha.coerceIn(0f, 1f)),
  )
}
