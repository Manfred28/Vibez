package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.ActiveWallAnomaly
import com.example.game.WallAnomalyType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Pure organic breathing transform `(scaleX, scaleY)` for wall anomaly nodes so each anomaly
 * subtly expands and settles out of phase with neighboring anomalies, matching the Web CSS
 * `wall-anomaly-breathe` / `wall-anomaly-pulse` keyframes 1:1.
 */
internal fun computeWallAnomalyBreathScale(
  anomalyId: Long,
  type: WallAnomalyType,
  breathPhase: Float,
): Pair<Float, Float> {
  val offset = ((abs(anomalyId) * 0.29f) % 1f)
  val cycle = (breathPhase.coerceIn(0f, 1f) + offset) % 1f
  val wave =
    if (type == WallAnomalyType.SWOLLEN_NODULE) {
      // Heavy arterial double-beat throb mirroring CSS `wall-anomaly-pulse`
      val primary = ((sin(cycle * 2.0 * PI) + 1.0) * 0.5).toFloat()
      val echo = ((sin((cycle * 4.0 - 0.35) * PI) + 1.0) * 0.5).toFloat()
      (primary * 0.72f + echo * 0.28f).coerceIn(0f, 1f)
    } else {
      // Reluctant asymmetric inhalation + brief hold mirroring CSS `wall-anomaly-breathe`
      val primary = ((sin((cycle - 0.12) * 2.0 * PI) + 1.0) * 0.5).toFloat()
      val secondary = ((sin(cycle * 4.0 * PI) + 1.0) * 0.5).toFloat()
      (primary * 0.84f + secondary * 0.16f).coerceIn(0f, 1f)
    }
  val (minX, ampX, minY, ampY) =
    when (type) {
      WallAnomalyType.PALE_TOOTH -> floatArrayOf(0.984f, 0.044f, 0.988f, 0.038f)
      WallAnomalyType.GLASS_EYE -> floatArrayOf(0.982f, 0.052f, 0.988f, 0.044f)
      WallAnomalyType.SWOLLEN_NODULE -> floatArrayOf(0.980f, 0.064f, 0.986f, 0.052f)
      WallAnomalyType.CHITIN_RIB -> floatArrayOf(0.982f, 0.056f, 0.988f, 0.046f)
    }
  return (minX + ampX * wave) to (minY + ampY * wave)
}

internal fun wallAnomalyDrawableRes(type: WallAnomalyType): Int =
  when (type) {
    WallAnomalyType.PALE_TOOTH -> R.drawable.img_anomaly_pale_tooth
    WallAnomalyType.GLASS_EYE -> R.drawable.img_anomaly_glass_eye
    WallAnomalyType.SWOLLEN_NODULE -> R.drawable.img_anomaly_swollen_nodule
    WallAnomalyType.CHITIN_RIB -> R.drawable.img_anomaly_chitin_rib
  }

/**
 * Interactive photographic wall anomaly specimen erupting directly from the damp masonry wall.
 * Renders a circular edge-feathered macro photograph with living organic breathing scale and
 * soft cavity glow — eliminating rigid square icon boxes and flat vector symbols.
 */
@Composable
internal fun WallAnomalyNode(
  anomaly: ActiveWallAnomaly,
  breathPhaseProvider: () -> Float,
  onInteract: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val hapticFeedback = LocalHapticFeedback.current

  val label =
    when (anomaly.type) {
      WallAnomalyType.PALE_TOOTH -> stringResource(R.string.anomaly_tooth_label)
      WallAnomalyType.GLASS_EYE -> stringResource(R.string.anomaly_eye_label)
      WallAnomalyType.SWOLLEN_NODULE -> stringResource(R.string.anomaly_nodule_label)
      WallAnomalyType.CHITIN_RIB -> stringResource(R.string.anomaly_rib_label)
    }

  val cd =
    when (anomaly.type) {
      WallAnomalyType.PALE_TOOTH -> stringResource(R.string.anomaly_tooth_cd)
      WallAnomalyType.GLASS_EYE -> stringResource(R.string.anomaly_eye_cd)
      WallAnomalyType.SWOLLEN_NODULE -> stringResource(R.string.anomaly_nodule_cd)
      WallAnomalyType.CHITIN_RIB -> stringResource(R.string.anomaly_rib_cd)
    }

  val accentColor =
    when (anomaly.type) {
      WallAnomalyType.PALE_TOOTH -> colorScheme.onBackground
      WallAnomalyType.GLASS_EYE -> colorScheme.primary
      WallAnomalyType.SWOLLEN_NODULE -> colorScheme.tertiary
      WallAnomalyType.CHITIN_RIB -> colorScheme.tertiary
    }

  val cavityColor =
    when (anomaly.type) {
      WallAnomalyType.SWOLLEN_NODULE,
      WallAnomalyType.CHITIN_RIB -> colorScheme.tertiaryContainer
      WallAnomalyType.GLASS_EYE -> colorScheme.primaryContainer
      WallAnomalyType.PALE_TOOTH -> colorScheme.background
    }

  Box(
    modifier =
      modifier
        .size(76.dp)
        .minimumInteractiveComponentSize()
        .drawBehind {
          val (sx, _) = computeWallAnomalyBreathScale(anomaly.id, anomaly.type, breathPhaseProvider())
          val glowStrength = ((sx - 0.98f) / 0.064f).coerceIn(0f, 1f)
          val center = Offset(size.width * 0.5f, size.height * 0.5f)
          val haloR = size.minDimension * (0.54f + glowStrength * 0.14f)
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    colorScheme.background.copy(alpha = 0.88f),
                    cavityColor.copy(alpha = 0.36f + glowStrength * 0.28f),
                    accentColor.copy(alpha = 0.10f + glowStrength * 0.18f),
                    Color.Transparent,
                  ),
                center = center,
                radius = haloR,
              ),
            radius = haloR,
            center = center,
          )
        }
        .graphicsLayer {
          val (sx, sy) = computeWallAnomalyBreathScale(anomaly.id, anomaly.type, breathPhaseProvider())
          scaleX = sx
          scaleY = sy
        }
        .testTag("anomaly_${anomaly.type.id}_${anomaly.id}")
        .semantics { contentDescription = cd }
        .clickable(
          role = Role.Button,
          onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onInteract()
          },
        ),
    contentAlignment = Alignment.Center,
  ) {
    // Circular photographic macro specimen dissolved seamlessly into the damp wall
    Box(
      modifier = Modifier.size(66.dp).clip(CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Image(
        painter = painterResource(id = wallAnomalyDrawableRes(anomaly.type)),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
          Modifier.fillMaxSize()
            .graphicsLayer {
              scaleX = 1.12f
              scaleY = 1.12f
            }
            .testTag("anomaly_art_${anomaly.type.id}"),
      )

      // Feathered radial cavity vignette + faint wet organic rim glint (no crisp sticker border)
      Canvas(modifier = Modifier.fillMaxSize()) {
        val (sx, _) = computeWallAnomalyBreathScale(anomaly.id, anomaly.type, breathPhaseProvider())
        val glowStrength = ((sx - 0.98f) / 0.064f).coerceIn(0f, 1f)
        val center = Offset(size.width * 0.5f, size.height * 0.5f)
        val radius = size.minDimension * 0.5f

        drawCircle(
          brush =
            Brush.radialGradient(
              colorStops =
                arrayOf(
                  0.0f to Color.Transparent,
                  0.50f to Color.Transparent,
                  0.78f to colorScheme.background.copy(alpha = 0.58f),
                  1.0f to colorScheme.background.copy(alpha = 0.96f),
                ),
              center = center,
              radius = radius,
            ),
          radius = radius,
          center = center,
        )

        drawCircle(
          color = accentColor.copy(alpha = 0.16f + glowStrength * 0.24f),
          radius = radius - 1.5.dp.toPx(),
          center = center,
          style = Stroke(width = 1.1.dp.toPx()),
        )
      }
    }

    // Minimal dark-whisper action cue tucked along the lower edge
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, letterSpacing = 1.1.sp),
      color = accentColor.copy(alpha = 0.92f),
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .background(
            color = colorScheme.background.copy(alpha = 0.78f),
            shape = RoundedCornerShape(2.dp),
          )
          .padding(horizontal = 5.dp, vertical = 1.dp),
    )
  }
}
