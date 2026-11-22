package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.CreatureType
import com.example.game.OrganGameState
import com.example.ui.theme.BleakTheme
import java.text.NumberFormat
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun ChamberCanvas(
  state: OrganGameState,
  breathPhaseProvider: () -> Float,
  onPressValve: () -> Unit,
  onInteractAnomaly: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val atmosphere = BleakTheme.atmosphere

  val formattedIchor =
    remember(state.ichorDisplay) { NumberFormat.getIntegerInstance().format(state.ichorDisplay) }
  val counterCd = stringResource(R.string.counter_value_content_description, formattedIchor)
  val formattedTapYield =
    remember(state.manualTapYield) { formatCompactDecimal(state.manualTapYield) }

  val counterRecoil = remember { Animatable(1f) }
  LaunchedEffect(state.ichorDisplay) {
    if (state.ichorDisplay > 0L) {
      counterRecoil.snapTo(0.96f)
      counterRecoil.animateTo(
        targetValue = 1f,
        animationSpec =
          spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
      )
    }
  }

  val residues = remember { mutableStateListOf<ValveResidue>() }
  var nextResidueId by remember { mutableLongStateOf(0L) }

  val sutureCount = state.creatureCount(CreatureType.SUTURE_HAND)
  val leechCount = state.creatureCount(CreatureType.LEECH_CLERK)
  val lampCount = state.creatureCount(CreatureType.WEEPING_LAMP)

  Surface(
    modifier = modifier.fillMaxWidth().testTag("chamber_canvas"),
    shape = MaterialTheme.shapes.small,
    color = colorScheme.surface.copy(alpha = 0.62f),
    border = BorderStroke(1.dp, atmosphere.hairline),
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val boxWidth = maxWidth
      val boxHeight = maxHeight

      // Living tiled wall, grout cracks, organic veins, hanging weeping lamps & scar seams
      Canvas(modifier = Modifier.fillMaxSize()) {
        val breath = breathPhaseProvider()
        val tileStep = 38.dp.toPx()

        val tileLineColor = atmosphere.hairline.copy(alpha = 0.34f)
        var x = tileStep
        while (x < size.width) {
          drawLine(tileLineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
          x += tileStep
        }
        var y = tileStep
        while (y < size.height) {
          drawLine(tileLineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
          y += tileStep
        }

        if (state.scarTissue > 0L) {
          val seamCount = min(state.scarTissue.toInt(), 4)
          val scarColor = colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
          for (s in 0 until seamCount) {
            val startY = size.height * (0.14f + s * 0.22f)
            val endY = startY + size.height * 0.16f
            drawLine(
              scarColor,
              Offset(0f, startY),
              Offset(size.width, endY),
              strokeWidth = 1.4.dp.toPx(),
            )
            for (st in 1 until 7) {
              val t = st / 7f
              val sx = size.width * t
              val sy = startY + (endY - startY) * t
              drawLine(
                scarColor,
                Offset(sx - 3.dp.toPx(), sy - 5.dp.toPx()),
                Offset(sx + 3.dp.toPx(), sy + 5.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
              )
            }
          }
        }

        if (state.phase >= 1) {
          val fissureCount = state.phase * 2 + state.graftedTendrils.coerceAtMost(4)
          val veinColor =
            if (state.phase >= 2) {
              colorScheme.tertiary.copy(alpha = 0.20f + atmosphere.distortionFactor * 0.26f)
            } else {
              colorScheme.outline.copy(alpha = 0.45f)
            }
          val center = Offset(size.width * 0.5f, size.height * 0.60f)
          for (i in 0 until fissureCount) {
            val angle = (i * (2.0 * PI / fissureCount)) + (breath - 0.5) * 0.08
            val reach = size.minDimension * (0.36f + state.phase * 0.11f + breath * 0.03f)
            val mid =
              Offset(
                center.x + cos(angle + 0.14).toFloat() * (reach * 0.52f),
                center.y + sin(angle - 0.12).toFloat() * (reach * 0.52f),
              )
            val end =
              Offset(
                center.x + cos(angle).toFloat() * reach,
                center.y + sin(angle).toFloat() * reach,
              )
            val path =
              Path().apply {
                moveTo(center.x, center.y)
                quadraticTo(mid.x, mid.y, end.x, end.y)
              }
            drawPath(
              path = path,
              color = veinColor,
              style = Stroke(width = (1.2f + state.phase * 0.45f).dp.toPx(), cap = StrokeCap.Round),
            )
          }
        }

        if (lampCount > 0) {
          val visibleLamps = min(lampCount, 5)
          val spacing = size.width / (visibleLamps + 1)
          for (i in 1..visibleLamps) {
            val sway = (breath - 0.5f) * 8.dp.toPx() * if (i % 2 == 0) 1f else -1f
            val anchorX = spacing * i
            val cordBottom = Offset(anchorX + sway, 26.dp.toPx())
            drawLine(colorScheme.outline, Offset(anchorX, 0f), cordBottom, strokeWidth = 1.dp.toPx())
            val bulbColor =
              if (state.isStillnessActive) colorScheme.secondary else colorScheme.surfaceVariant
            drawOval(
              color = bulbColor,
              topLeft = Offset(cordBottom.x - 6.dp.toPx(), cordBottom.y),
              size = Size(12.dp.toPx(), 16.dp.toPx()),
            )
            drawOval(
              color =
                if (state.isStillnessActive) colorScheme.primary.copy(alpha = 0.7f)
                else colorScheme.outline,
              topLeft = Offset(cordBottom.x - 6.dp.toPx(), cordBottom.y),
              size = Size(12.dp.toPx(), 16.dp.toPx()),
              style = Stroke(width = 1.dp.toPx()),
            )
            if (state.isStillnessActive) {
              val dropY = cordBottom.y + 18.dp.toPx() + breath * 16.dp.toPx()
              drawCircle(
                color =
                  colorScheme.primary.copy(alpha = (1f - breath * 0.6f).coerceIn(0.15f, 0.8f)),
                radius = 2.2.dp.toPx(),
                center = Offset(cordBottom.x, dropY),
              )
            }
          }
        }
      }

      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.ichor_label),
              style = MaterialTheme.typography.labelLarge,
              color = colorScheme.onSurfaceVariant,
            )
            if (state.ichorPerSecond > 0.0) {
              Text(
                text =
                  stringResource(
                    R.string.ichor_rate_format,
                    formatCompactDecimal(state.ichorPerSecond),
                  ),
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.primary,
                modifier = Modifier.testTag("ichor_rate_text"),
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          Box(contentAlignment = Alignment.Center) {
            val dynamicFontSize =
              when {
                formattedIchor.length > 9 -> 38.sp
                formattedIchor.length > 6 -> 46.sp
                else -> 54.sp
              }

            Text(
              text = formattedIchor,
              style = MaterialTheme.typography.displayLarge.copy(fontSize = dynamicFontSize),
              color = colorScheme.onBackground,
              maxLines = 1,
              modifier =
                Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                  .testTag("counter_text")
                  .semantics { contentDescription = counterCd }
                  .graphicsLayer {
                    scaleX = counterRecoil.value
                    scaleY = counterRecoil.value
                  },
            )

            if (leechCount > 0) {
              LeechClerkOverlay(
                leechCount = leechCount,
                breathPhaseProvider = breathPhaseProvider,
                modifier = Modifier.matchParentSize(),
              )
            }
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (state.isJarsAndGraftUnlocked || state.stillnessDisplay > 0L) {
              Text(
                text =
                  stringResource(
                    R.string.stillness_readout,
                    NumberFormat.getIntegerInstance().format(state.stillnessDisplay),
                  ),
                style = MaterialTheme.typography.labelSmall,
                color =
                  if (state.isStillnessActive) colorScheme.primary else colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("stillness_text"),
              )
            }
            if (state.isStillnessActive) {
              Text(
                text = stringResource(R.string.stillness_active_tag),
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.secondary,
                modifier = Modifier.testTag("stillness_active_badge"),
              )
            }
            if (state.gazeRemainingSeconds > 0.0) {
              Text(
                text =
                  stringResource(
                    R.string.gaze_active_tag,
                    ceil(state.gazeRemainingSeconds).toInt(),
                  ),
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.tertiary,
                modifier = Modifier.testTag("gaze_active_badge"),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
          ValveApertureArt(
            breathPhaseProvider = breathPhaseProvider,
            distortion = atmosphere.distortionFactor,
            sutureCount = sutureCount,
            graftCount = state.graftedTendrils,
            ringColor = atmosphere.hairline,
            veinColor = colorScheme.tertiary,
            tendrilColor = colorScheme.primary,
          )

          RecessedValveButton(
            yieldLabel = stringResource(R.string.valve_plus_format, formattedTapYield),
            onPress = {
              onPressValve()
              val id = nextResidueId++
              val drift = ((id % 5) - 2) * 14f
              if (residues.size >= 14) {
                residues.removeAt(0)
              }
              residues.add(ValveResidue(id = id, label = "+$formattedTapYield", xDriftDp = drift))
            },
          )

          residues.forEach { residue ->
            key(residue.id) {
              DissolvingResidue(
                residue = residue,
                onFinished = { residues.removeAll { it.id == residue.id } },
              )
            }
          }
        }
      }

      state.activeAnomalies.forEach { anomaly ->
        key(anomaly.id) {
          val offsetX = (boxWidth - 62.dp) * anomaly.xFraction
          val offsetY = (boxHeight - 62.dp) * anomaly.yFraction
          WallAnomalyNode(
            anomaly = anomaly,
            breathPhaseProvider = breathPhaseProvider,
            onInteract = { onInteractAnomaly(anomaly.id) },
            modifier = Modifier.offset(x = offsetX, y = offsetY),
          )
        }
      }
    }
  }
}

@Composable
private fun LeechClerkOverlay(
  leechCount: Int,
  breathPhaseProvider: () -> Float,
  modifier: Modifier = Modifier,
) {
  val bodyColor = MaterialTheme.colorScheme.secondary
  val outlineColor = MaterialTheme.colorScheme.outline
  val visibleLeeches = min(leechCount, 6)

  Canvas(modifier = modifier) {
    val breath = breathPhaseProvider()
    for (i in 0 until visibleLeeches) {
      val onLeft = i % 2 == 0
      val row = i / 2
      val baseX = if (onLeft) 4.dp.toPx() else size.width - 14.dp.toPx()
      val baseY = (size.height * (0.24f + row * 0.28f)) + (breath - 0.5f) * 3.dp.toPx()
      for (seg in 0..2) {
        val segX = baseX + (if (onLeft) seg * 3.2.dp.toPx() else -seg * 3.2.dp.toPx())
        val radius = (3.4f - seg * 0.5f).dp.toPx()
        drawCircle(color = bodyColor.copy(alpha = 0.85f), radius = radius, center = Offset(segX, baseY))
        drawCircle(
          color = outlineColor,
          radius = radius,
          center = Offset(segX, baseY),
          style = Stroke(width = 0.8.dp.toPx()),
        )
      }
    }
  }
}

internal fun formatCompactDecimal(value: Double): String {
  val rounded = (value * 10.0).roundToInt() / 10.0
  return if (rounded == rounded.toLong().toDouble()) {
    NumberFormat.getIntegerInstance().format(rounded.toLong())
  } else {
    NumberFormat.getNumberInstance()
      .apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
      }
      .format(rounded)
  }
}
