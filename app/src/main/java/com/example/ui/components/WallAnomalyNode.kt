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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
            drawLine(
              color = colorScheme.outline,
              start = Offset(2.dp.toPx(), size.height * 0.72f),
              end = Offset(size.width - 2.dp.toPx(), size.height * 0.66f),
              strokeWidth = 1.5.dp.toPx(),
            )
            val lift = breath * 2.dp.toPx()
            drawRoundRect(
              color = colorScheme.onBackground.copy(alpha = 0.90f),
              topLeft = Offset(cx - 6.dp.toPx(), cy - 7.dp.toPx() - lift),
              size = Size(12.dp.toPx(), 14.dp.toPx()),
              cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
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
