package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.theme.TapCounterTheme
import java.text.NumberFormat
import kotlin.math.roundToInt

private data class TapBurst(val id: Long, val xDriftDp: Float)

@Composable
fun CounterRoute(modifier: Modifier = Modifier, viewModel: CounterViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  CounterScreen(count = uiState.count, onIncrement = viewModel::increment, modifier = modifier)
}

@Composable
fun CounterScreen(count: Long, onIncrement: () -> Unit, modifier: Modifier = Modifier) {
  val backgroundColor = MaterialTheme.colorScheme.background
  val primaryGlow = MaterialTheme.colorScheme.primary
  val tertiaryGlow = MaterialTheme.colorScheme.tertiary

  val formattedCount = remember(count) { NumberFormat.getIntegerInstance().format(count) }
  val counterContentDescription =
    stringResource(R.string.counter_value_content_description, formattedCount)

  val counterScale = remember { Animatable(1f) }
  LaunchedEffect(count) {
    if (count > 0L) {
      counterScale.snapTo(1.08f)
      counterScale.animateTo(
        targetValue = 1f,
        animationSpec =
          spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
      )
    }
  }

  val bursts = remember { mutableStateListOf<TapBurst>() }
  var nextBurstId by remember { mutableLongStateOf(0L) }

  Scaffold(
    modifier =
      modifier.fillMaxSize().drawBehind {
        drawRect(color = backgroundColor)
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(primaryGlow.copy(alpha = 0.16f), Color.Transparent),
              center = Offset(size.width * 0.5f, size.height * 0.58f),
              radius = size.minDimension * 0.72f,
            ),
          radius = size.minDimension * 0.72f,
          center = Offset(size.width * 0.5f, size.height * 0.58f),
        )
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(tertiaryGlow.copy(alpha = 0.08f), Color.Transparent),
              center = Offset(size.width * 0.5f, size.height * 0.25f),
              radius = size.minDimension * 0.55f,
            ),
          radius = size.minDimension * 0.55f,
          center = Offset(size.width * 0.5f, size.height * 0.25f),
        )
      },
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets.safeDrawing,
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 24.dp)) {
      Column(
        modifier = Modifier.fillMaxSize().widthIn(max = 600.dp).align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = stringResource(R.string.counter_label),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        val dynamicFontSize =
          when {
            formattedCount.length > 12 -> 36.sp
            formattedCount.length > 9 -> 44.sp
            formattedCount.length > 6 -> 54.sp
            else -> 68.sp
          }

        Text(
          text = formattedCount,
          style = MaterialTheme.typography.displayLarge.copy(fontSize = dynamicFontSize),
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          modifier =
            Modifier.testTag("counter_text")
              .semantics { contentDescription = counterContentDescription }
              .graphicsLayer {
                scaleX = counterScale.value
                scaleY = counterScale.value
              },
        )

        Spacer(modifier = Modifier.height(56.dp))

        Box(contentAlignment = Alignment.Center) {
          TactileIncrementButton(
            onTap = {
              onIncrement()
              val id = nextBurstId++
              val drift = ((id % 5) - 2) * 14f
              bursts.add(TapBurst(id = id, xDriftDp = drift))
            }
          )

          bursts.forEach { burst ->
            FloatingPlusOne(
              burst = burst,
              onFinished = { bursts.removeAll { it.id == burst.id } },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TactileIncrementButton(onTap: () -> Unit, modifier: Modifier = Modifier) {
  val hapticFeedback = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val isHovered by interactionSource.collectIsHoveredAsState()
  var pointerHovered by remember { mutableStateOf(false) }

  val activeHover = isHovered || pointerHovered

  val pressOffset by
    animateDpAsState(
      targetValue = if (isPressed) 6.dp else 0.dp,
      animationSpec =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
      label = "button_press_offset",
    )

  val buttonScale by
    animateFloatAsState(
      targetValue =
        when {
          isPressed -> 0.96f
          activeHover -> 1.02f
          else -> 1f
        },
      animationSpec =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
      label = "button_scale",
    )

  val tapButtonDesc = stringResource(R.string.tap_button_content_description)
  val buttonDiameter = 176.dp
  val shadowDepth = 8.dp

  Box(
    modifier =
      modifier.size(width = buttonDiameter, height = buttonDiameter + shadowDepth).graphicsLayer {
        scaleX = buttonScale
        scaleY = buttonScale
      },
    contentAlignment = Alignment.TopCenter,
  ) {
    // 3D tactile base rim
    Surface(
      modifier = Modifier.size(buttonDiameter).offset(y = shadowDepth),
      shape = CircleShape,
      color = MaterialTheme.colorScheme.primaryContainer,
      tonalElevation = 0.dp,
    ) {}

    // Tactile top face
    Surface(
      modifier =
        Modifier.size(buttonDiameter)
          .offset(y = pressOffset)
          .minimumInteractiveComponentSize()
          .clip(CircleShape)
          .testTag("increment_button")
          .semantics { contentDescription = tapButtonDesc }
          .pointerInput(Unit) {
            awaitPointerEventScope {
              while (true) {
                val event = awaitPointerEvent()
                when (event.type) {
                  PointerEventType.Enter -> pointerHovered = true
                  PointerEventType.Exit -> pointerHovered = false
                }
              }
            }
          }
          .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, color = MaterialTheme.colorScheme.onPrimary),
            role = Role.Button,
            onClick = {
              hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onTap()
            },
          ),
      shape = CircleShape,
      color = MaterialTheme.colorScheme.primary,
      border =
        BorderStroke(
          width = 2.dp,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.28f),
        ),
    ) {
      Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = stringResource(R.string.tap_button_plus_one),
          style = MaterialTheme.typography.headlineLarge.copy(fontSize = 44.sp),
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = stringResource(R.string.tap_button_action),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
        )
      }
    }
  }
}

@Composable
private fun FloatingPlusOne(burst: TapBurst, onFinished: () -> Unit) {
  val progress = remember { Animatable(0f) }

  LaunchedEffect(burst.id) {
    progress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
    )
    onFinished()
  }

  val yOffsetPx = -(72f + progress.value * 96f)
  val alpha = 1f - (progress.value * progress.value)

  Text(
    text = stringResource(R.string.tap_button_plus_one),
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.primary,
    modifier =
      Modifier.offset { IntOffset(x = burst.xDriftDp.dp.roundToPx(), y = yOffsetPx.dp.roundToPx()) }
        .alpha(alpha.coerceIn(0f, 1f)),
  )
}

@Preview(showBackground = true)
@Composable
private fun CounterScreenDarkPreview() {
  TapCounterTheme(darkTheme = true) { CounterScreen(count = 42L, onIncrement = {}) }
}

@Preview(showBackground = true)
@Composable
private fun CounterScreenLightPreview() {
  TapCounterTheme(darkTheme = false) { CounterScreen(count = 1_234L, onIncrement = {}) }
}
