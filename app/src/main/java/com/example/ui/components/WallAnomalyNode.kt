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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    }

  val cd =
    when (anomaly.type) {
      WallAnomalyType.PALE_TOOTH -> stringResource(R.string.anomaly_tooth_cd)
      WallAnomalyType.GLASS_EYE -> stringResource(R.string.anomaly_eye_cd)
      WallAnomalyType.SWOLLEN_NODULE -> stringResource(R.string.anomaly_nodule_cd)
    }

  val accentColor =
    when (anomaly.type) {
      WallAnomalyType.PALE_TOOTH -> colorScheme.onBackground
      WallAnomalyType.GLASS_EYE -> colorScheme.primary
      WallAnomalyType.SWOLLEN_NODULE -> colorScheme.tertiary
    }

  Surface(
    modifier =
      modifier
        .size(62.dp)
        .minimumInteractiveComponentSize()
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
