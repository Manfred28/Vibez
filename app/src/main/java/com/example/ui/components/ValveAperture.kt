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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

internal data class ValveResidue(val id: Long, val label: String, val xDriftDp: Float)

@Composable
internal fun ValveApertureArt(
  breathPhaseProvider: () -> Float,
  distortion: Float,
  ringColor: Color,
  veinColor: Color,
  modifier: Modifier = Modifier,
) {
  val shadowColor = MaterialTheme.colorScheme.background
  val boneColor = MaterialTheme.colorScheme.onBackground
  val clotContainer = MaterialTheme.colorScheme.tertiaryContainer
  Canvas(modifier = modifier.size(228.dp).testTag("valve_aperture_art")) {
    val breath = breathPhaseProvider()
    val center = Offset(size.width / 2f, size.height / 2f)
    val socketRadius = size.minDimension * 0.365f
    val flangeRadius = size.minDimension * 0.415f

    // 1. Deep sunken masonry recess shadow & damp bruised seepage halo around valve socket
    val bruiseR = flangeRadius + (8f + distortion * 12f + breath * 4f).dp.toPx()
    drawCircle(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              shadowColor.copy(alpha = 0.90f),
              clotContainer.copy(alpha = 0.22f + distortion * 0.34f),
              Color.Transparent,
            ),
          center = center,
          radius = bruiseR,
        ),
      radius = bruiseR,
      center = center,
    )

    // 2. Heavy recessed cast-iron mounting flange ring seated in the tile/flesh wall
    drawCircle(
      color = shadowColor.copy(alpha = 0.82f),
      radius = flangeRadius,
      center = center,
      style = Stroke(width = 7.dp.toPx()),
    )
    drawCircle(
      color = ringColor.copy(alpha = 0.55f),
      radius = flangeRadius + 2.5.dp.toPx(),
      center = center,
      style = Stroke(width = 1.2.dp.toPx()),
    )
    drawCircle(
      color = boneColor.copy(alpha = 0.14f),
      radius = flangeRadius - 2.5.dp.toPx(),
      center = center,
      style = Stroke(width = 1.0.dp.toPx()),
    )

    // 8 recessed tarnished iron flange bolts
    val boltR = flangeRadius
    for (b in 0 until 8) {
      val ang = b * (Math.PI / 4.0) + Math.PI / 8.0
      val bx = center.x + kotlin.math.cos(ang).toFloat() * boltR
      val by = center.y + kotlin.math.sin(ang).toFloat() * boltR
      drawCircle(color = shadowColor.copy(alpha = 0.94f), radius = 3.2.dp.toPx(), center = Offset(bx, by))
      drawCircle(color = ringColor.copy(alpha = 0.72f), radius = 2.0.dp.toPx(), center = Offset(bx, by))
      drawCircle(
        color = boneColor.copy(alpha = 0.22f),
        radius = 0.8.dp.toPx(),
        center = Offset(bx - 0.6.dp.toPx(), by - 0.6.dp.toPx()),
      )
    }

    // 3. Living bruised subcutaneous tissue lip pulsing just inside the mounting socket
    val swellWidth = (2.2f + distortion * 2.8f + breath * 1.2f).dp.toPx()
    drawCircle(
      color = veinColor.copy(alpha = 0.24f + distortion * 0.36f + breath * 0.08f),
      radius = socketRadius,
      center = center,
      style = Stroke(width = swellWidth),
    )
  }
}



@Composable
internal fun RecessedValveButton(
  yieldLabel: String,
  wheelRotationDegreesProvider: () -> Float,
  onPress: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val hapticFeedback = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val pressOffset by
    animateDpAsState(
      targetValue = if (isPressed) 6.dp else 0.dp,
      animationSpec =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
      label = "valve_press_offset",
    )

  val valveScale by
    animateFloatAsState(
      targetValue = if (isPressed) 0.96f else 1f,
      animationSpec =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
      label = "valve_scale",
    )

  val valveCd = stringResource(R.string.valve_content_description)
  val diameter = 132.dp
  val depth = 6.dp

  Box(
    modifier =
      modifier.size(width = diameter, height = diameter + depth).graphicsLayer {
        scaleX = valveScale
        scaleY = valveScale
      },
    contentAlignment = Alignment.TopCenter,
  ) {
    Surface(
      modifier = Modifier.size(diameter).offset(y = depth),
      shape = CircleShape,
      color = colorScheme.surfaceContainerLowest,
      border = BorderStroke(1.dp, colorScheme.outlineVariant),
    ) {}

    Surface(
      modifier =
        Modifier.size(diameter)
          .offset(y = pressOffset)
          .minimumInteractiveComponentSize()
          .clip(CircleShape)
          .testTag("increment_button")
          .semantics { contentDescription = valveCd }
          .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, color = colorScheme.tertiary),
            role = Role.Button,
            onClick = {
              hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
              onPress()
            },
          ),
      shape = CircleShape,
      color = if (isPressed) colorScheme.tertiaryContainer else colorScheme.surface,
      border =
        BorderStroke(
          width = 1.dp,
          color =
            if (isPressed) colorScheme.tertiary.copy(alpha = 0.85f)
            else colorScheme.outline.copy(alpha = 0.72f),
        ),
    ) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Rotating iron valve wheel & radial spoke notches (turns monotonically clockwise)
        Box(
          modifier =
            Modifier.fillMaxSize().graphicsLayer {
              rotationZ = wheelRotationDegreesProvider()
            }
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_recessed_valve),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
              Modifier.fillMaxSize()
                .clip(CircleShape)
                .alpha(if (isPressed) 0.48f else 0.62f)
                .testTag("valve_artwork"),
          )

          Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width * 0.5f, size.height * 0.5f)
            val outerR = size.minDimension * 0.47f
            val innerR = size.minDimension * 0.34f
            val spokeColor = colorScheme.onBackground.copy(alpha = 0.24f)
            val notchColor = colorScheme.tertiary.copy(alpha = 0.42f)
            for (k in 0 until 6) {
              val ang = k * (Math.PI / 3.0)
              val dx = kotlin.math.cos(ang).toFloat()
              val dy = kotlin.math.sin(ang).toFloat()
              drawLine(
                color = if (k == 0) notchColor else spokeColor,
                start = Offset(c.x + dx * innerR, c.y + dy * innerR),
                end = Offset(c.x + dx * outerR, c.y + dy * outerR),
                strokeWidth = if (k == 0) 2.4.dp.toPx() else 1.4.dp.toPx(),
              )
            }
          }
        }

        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(
                Brush.radialGradient(
                  colors =
                    listOf(
                      (if (isPressed) colorScheme.tertiaryContainer else colorScheme.background)
                        .copy(alpha = if (isPressed) 0.74f else 0.70f),
                      colorScheme.background.copy(alpha = 0.46f),
                    )
                )
              )
        )

        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Text(
            text = yieldLabel,
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp),
            color = colorScheme.onBackground.copy(alpha = 0.88f),
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = stringResource(R.string.valve_action_label),
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.onBackground.copy(alpha = 0.70f),
          )
        }
      }
    }
  }
}

@Composable
internal fun DissolvingResidue(residue: ValveResidue, onFinished: () -> Unit) {
  val progress = remember { Animatable(0f) }

  LaunchedEffect(residue.id) {
    progress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing),
    )
    onFinished()
  }

  val yOffsetDp = -(62f + progress.value * 66f)
  val alpha = (1f - progress.value) * 0.65f

  Text(
    text = residue.label,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.primary,
    modifier =
      Modifier.offset { IntOffset(x = residue.xDriftDp.dp.roundToPx(), y = yOffsetDp.dp.roundToPx()) }
        .alpha(alpha.coerceIn(0f, 1f)),
  )
}
