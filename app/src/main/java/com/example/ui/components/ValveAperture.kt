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
import androidx.compose.ui.graphics.PathEffect
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal data class ValveResidue(val id: Long, val label: String, val xDriftDp: Float)

@Composable
internal fun ValveApertureArt(
  breathPhaseProvider: () -> Float,
  distortion: Float,
  sutureCount: Int,
  graftCount: Int,
  ringColor: Color,
  veinColor: Color,
  tendrilColor: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.size(192.dp)) {
    val breath = breathPhaseProvider()
    val center = Offset(size.width / 2f, size.height / 2f)
    val outerRadius = size.minDimension * (0.47f + breath * 0.02f)
    val innerRadius = size.minDimension * 0.41f

    drawCircle(
      color = ringColor.copy(alpha = 0.8f),
      radius = outerRadius,
      center = center,
      style =
        Stroke(
          width = 1.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 11f)),
        ),
    )

    val drift = (2.dp.toPx() + 6.dp.toPx() * distortion) * (breath - 0.5f)
    drawCircle(
      color = veinColor.copy(alpha = 0.18f + distortion * 0.30f),
      radius = innerRadius,
      center = Offset(center.x + drift, center.y - drift * 0.7f),
      style = Stroke(width = 1.2.dp.toPx()),
    )

    val visibleHands = min(sutureCount, 8)
    for (i in 0 until visibleHands) {
      val angle = (i * (2.0 * PI / visibleHands)) - PI / 2.0
      val reachInset = (10.dp.toPx() * sin((breath * PI) + i)).toFloat()
      val outerPt =
        Offset(
          center.x + cos(angle).toFloat() * (outerRadius + 4.dp.toPx()),
          center.y + sin(angle).toFloat() * (outerRadius + 4.dp.toPx()),
        )
      val tipPt =
        Offset(
          center.x + cos(angle).toFloat() * (innerRadius - 10.dp.toPx() - reachInset * 0.4f),
          center.y + sin(angle).toFloat() * (innerRadius - 10.dp.toPx() - reachInset * 0.4f),
        )
      drawLine(
        color = tendrilColor.copy(alpha = 0.65f),
        start = outerPt,
        end = tipPt,
        strokeWidth = 1.6.dp.toPx(),
        cap = StrokeCap.Round,
      )
      drawCircle(color = tendrilColor, radius = 2.4.dp.toPx(), center = tipPt)
    }

    val visibleGrafts = min(graftCount, 4)
    for (g in 0 until visibleGrafts) {
      val angle = (g * (PI / 2.0)) + PI / 4.0 + (breath - 0.5) * 0.12
      val start =
        Offset(
          center.x + cos(angle).toFloat() * (outerRadius + 6.dp.toPx()),
          center.y + sin(angle).toFloat() * (outerRadius + 6.dp.toPx()),
        )
      val end =
        Offset(
          center.x + cos(angle).toFloat() * (innerRadius - 14.dp.toPx()),
          center.y + sin(angle).toFloat() * (innerRadius - 14.dp.toPx()),
        )
      drawLine(
        color = veinColor.copy(alpha = 0.85f),
        start = start,
        end = end,
        strokeWidth = 3.2.dp.toPx(),
        cap = StrokeCap.Round,
      )
    }
  }
}

@Composable
internal fun RecessedValveButton(
  yieldLabel: String,
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
      color = if (isPressed) colorScheme.tertiaryContainer else colorScheme.surfaceVariant,
      border =
        BorderStroke(
          width = 1.5.dp,
          color = if (isPressed) colorScheme.tertiary else colorScheme.outline,
        ),
    ) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
          painter = painterResource(id = R.drawable.img_recessed_valve),
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier =
            Modifier.fillMaxSize()
              .clip(CircleShape)
              .alpha(if (isPressed) 0.72f else 0.88f)
              .testTag("valve_artwork"),
        )

        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(
                Brush.radialGradient(
                  colors =
                    listOf(
                      (if (isPressed) colorScheme.tertiaryContainer else colorScheme.background)
                        .copy(alpha = if (isPressed) 0.72f else 0.62f),
                      colorScheme.background.copy(alpha = 0.28f),
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
            color = colorScheme.onBackground,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = stringResource(R.string.valve_action_label),
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.onBackground.copy(alpha = 0.85f),
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
