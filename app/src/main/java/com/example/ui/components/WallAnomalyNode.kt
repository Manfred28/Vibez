package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
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

  Surface(
    modifier =
      modifier
        .size(62.dp)
        .minimumInteractiveComponentSize()
        .drawBehind {
          val (sx, _) = computeWallAnomalyBreathScale(anomaly.id, anomaly.type, breathPhaseProvider())
          val glowStrength = ((sx - 0.98f) / 0.064f).coerceIn(0f, 1f)
          val pad = (2.5f + glowStrength * 3.5f).dp.toPx()
          drawRoundRect(
            color = accentColor.copy(alpha = 0.06f + glowStrength * 0.18f),
            topLeft = Offset(-pad, -pad),
            size = Size(size.width + pad * 2f, size.height + pad * 2f),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
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
    shape = MaterialTheme.shapes.small,
    color = colorScheme.surfaceContainerLowest.copy(alpha = 0.90f),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Canvas(modifier = Modifier.size(30.dp)) {
        val breath = breathPhaseProvider()
        val cx = size.width / 2f
        val cy = size.height / 2f
        when (anomaly.type) {
          WallAnomalyType.PALE_TOOTH -> {
            val lift = breath * 2.dp.toPx()

            // Jagged plaster fissure cavity
            val crackPath =
              Path().apply {
                moveTo(2.dp.toPx(), size.height * 0.72f)
                lineTo(cx - 6.dp.toPx(), size.height * 0.68f)
                lineTo(cx, size.height * 0.79f)
                lineTo(cx + 6.dp.toPx(), size.height * 0.66f)
                lineTo(size.width - 2.dp.toPx(), size.height * 0.70f)
              }
            drawOval(
              color = colorScheme.background.copy(alpha = 0.92f),
              topLeft = Offset(cx - 9.dp.toPx(), size.height * 0.60f),
              size = Size(18.dp.toPx(), 7.dp.toPx()),
            )
            drawPath(
              path = crackPath,
              color = colorScheme.outline,
              style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
            )

            // Anatomical bifurcated molar crown + roots emerging from the seam
            val topY = cy - 9.dp.toPx() - lift
            val midY = cy + 1.dp.toPx() - lift
            val rootY = cy + 7.5.dp.toPx() - lift * 0.4f
            val toothPath =
              Path().apply {
                moveTo(cx - 6.5.dp.toPx(), topY + 2.5.dp.toPx())
                quadraticTo(cx - 4.5.dp.toPx(), topY - 1.5.dp.toPx(), cx - 1.2.dp.toPx(), topY + 1.4.dp.toPx())
                quadraticTo(cx, topY + 2.4.dp.toPx(), cx + 1.2.dp.toPx(), topY + 1.4.dp.toPx())
                quadraticTo(cx + 4.5.dp.toPx(), topY - 1.5.dp.toPx(), cx + 6.5.dp.toPx(), topY + 2.5.dp.toPx())
                quadraticTo(cx + 7.dp.toPx(), midY, cx + 4.2.dp.toPx(), rootY)
                lineTo(cx + 1.6.dp.toPx(), midY + 2.dp.toPx())
                lineTo(cx, midY + 1.dp.toPx())
                lineTo(cx - 1.6.dp.toPx(), midY + 2.dp.toPx())
                lineTo(cx - 4.2.dp.toPx(), rootY)
                quadraticTo(cx - 7.dp.toPx(), midY, cx - 6.5.dp.toPx(), topY + 2.5.dp.toPx())
                close()
              }

            drawPath(
              path = toothPath,
              brush =
                Brush.verticalGradient(
                  colors =
                    listOf(
                      colorScheme.onBackground,
                      colorScheme.onSurfaceVariant,
                      colorScheme.surfaceVariant,
                    ),
                  startY = topY,
                  endY = rootY,
                ),
            )
            drawPath(
              path = toothPath,
              color = colorScheme.outline.copy(alpha = 0.85f),
              style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
            )
            // Subtle enamel cusp specular glint
            drawLine(
              color = colorScheme.onBackground.copy(alpha = 0.75f),
              start = Offset(cx - 3.8.dp.toPx(), topY + 3.dp.toPx()),
              end = Offset(cx - 2.6.dp.toPx(), topY + 6.5.dp.toPx()),
              strokeWidth = 1.1.dp.toPx(),
              cap = StrokeCap.Round,
            )
          }
          WallAnomalyType.GLASS_EYE -> {
            val eyeHeight = (12.dp.toPx() * (0.45f + 0.55f * (1f - breath * 0.4f)))
            drawOval(
              color = colorScheme.primaryContainer,
              topLeft = Offset(cx - 12.dp.toPx(), cy - eyeHeight / 2f),
              size = Size(24.dp.toPx(), eyeHeight),
            )
            drawOval(
              color = colorScheme.primary,
              topLeft = Offset(cx - 12.dp.toPx(), cy - eyeHeight / 2f),
              size = Size(24.dp.toPx(), eyeHeight),
              style = Stroke(width = 1.2.dp.toPx()),
            )
            drawCircle(
              color = colorScheme.onBackground,
              radius = 3.2.dp.toPx(),
              center = Offset(cx + (breath - 0.5f) * 3.dp.toPx(), cy),
            )
          }
          WallAnomalyType.SWOLLEN_NODULE -> {
            val radius = (8.5f + breath * 2.5f).dp.toPx()
            drawCircle(color = colorScheme.tertiaryContainer, radius = radius, center = Offset(cx, cy))
            drawCircle(
              color = colorScheme.tertiary,
              radius = radius,
              center = Offset(cx, cy),
              style = Stroke(width = 1.4.dp.toPx()),
            )
            drawCircle(
              color = colorScheme.tertiary.copy(alpha = 0.55f),
              radius = radius * 0.42f,
              center = Offset(cx - 2.dp.toPx(), cy - 1.5.dp.toPx()),
            )
          }
          WallAnomalyType.CHITIN_RIB -> {
            val spread = (1.0f + breath * 1.6f).dp.toPx()
            // Raw arterial fissure cavity parted open by the calcified ribs
            drawOval(
              color = colorScheme.tertiaryContainer.copy(alpha = 0.96f),
              topLeft = Offset(cx - 6.5.dp.toPx(), cy - 11.dp.toPx()),
              size = Size(13.dp.toPx(), 22.dp.toPx()),
            )
            drawLine(
              color = colorScheme.tertiary.copy(alpha = 0.72f + breath * 0.22f),
              start = Offset(cx, cy - 8.5.dp.toPx()),
              end = Offset(cx, cy + 8.5.dp.toPx()),
              strokeWidth = (2.2f + breath * 0.9f).dp.toPx(),
              cap = StrokeCap.Round,
            )

            // Twin curved bleached-bone rib prongs arching inward and prying the masonry apart
            for (side in intArrayOf(-1, 1)) {
              val s = side.toFloat()
              val ribPath =
                Path().apply {
                  moveTo(cx + s * (11.5.dp.toPx()), cy - 8.5.dp.toPx())
                  cubicTo(
                    cx + s * (3.0.dp.toPx() + spread),
                    cy - 5.dp.toPx(),
                    cx + s * (2.2.dp.toPx() + spread),
                    cy + 5.dp.toPx(),
                    cx + s * (10.5.dp.toPx()),
                    cy + 9.dp.toPx(),
                  )
                }
              drawPath(
                path = ribPath,
                color = colorScheme.background.copy(alpha = 0.94f),
                style = Stroke(width = 5.4.dp.toPx(), cap = StrokeCap.Round),
              )
              drawPath(
                path = ribPath,
                color = colorScheme.onBackground.copy(alpha = 0.92f),
                style = Stroke(width = 3.0.dp.toPx(), cap = StrokeCap.Round),
              )
              // Hairline calcified fracture across the curved bone
              drawLine(
                color = colorScheme.tertiary.copy(alpha = 0.85f),
                start = Offset(cx + s * (4.8.dp.toPx() + spread * 0.6f), cy - 1.dp.toPx()),
                end = Offset(cx + s * (7.2.dp.toPx() + spread * 0.6f), cy + 1.4.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
              )
            }
          }
        }
      }

      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
        color = accentColor,
      )
    }
  }
}
