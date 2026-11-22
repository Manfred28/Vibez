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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.CreatureType
import com.example.game.OrganBalance
import com.example.game.OrganGameState
import com.example.game.SpecimenJar
import com.example.game.WallAnomalyType
import com.example.ui.theme.BleakTheme
import java.text.NumberFormat
import kotlin.math.ceil
import kotlin.math.roundToInt

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
    color = colorScheme.surface.copy(alpha = 0.86f),
    border = BorderStroke(1.dp, atmosphere.hairline),
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val boxWidth = maxWidth
      val boxHeight = maxHeight

      ChamberWallBackdrop(
        state = state,
        breathPhaseProvider = breathPhaseProvider,
      )

      WeepingLampsCeilingOverlay(
        lampCount = lampCount,
        isStillnessActive = state.isStillnessActive,
        breathPhaseProvider = breathPhaseProvider,
      )

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

          Spacer(modifier = Modifier.height(6.dp))

          // Recessed damp counter glass plate where Leech-Clerks cling and graze
          Box(contentAlignment = Alignment.Center) {
            Surface(
              shape = MaterialTheme.shapes.extraSmall,
              color = colorScheme.background.copy(alpha = 0.62f),
              border =
                BorderStroke(
                  width = 1.dp,
                  color =
                    if (leechCount > 0) colorScheme.secondary.copy(alpha = 0.65f)
                    else atmosphere.hairline.copy(alpha = 0.72f),
                ),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.matchParentSize()) {
                  drawRect(
                    brush =
                      Brush.linearGradient(
                        colors =
                          listOf(
                            colorScheme.onBackground.copy(alpha = 0.06f),
                            Color.Transparent,
                            colorScheme.primary.copy(alpha = if (leechCount > 0) 0.05f else 0.0f),
                          ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                      )
                  )
                }

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
                    Modifier.padding(horizontal = 30.dp, vertical = 6.dp)
                      .testTag("counter_text")
                      .semantics { contentDescription = counterCd }
                      .graphicsLayer {
                        scaleX = counterRecoil.value
                        scaleY = counterRecoil.value
                      },
                )
              }
            }

            if (leechCount > 0) {
              LeechClerksGlassOverlay(
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
            ringColor = atmosphere.hairline,
            veinColor = colorScheme.tertiary,
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

          SutureHandsValveOverlay(
            breathPhaseProvider = breathPhaseProvider,
            sutureCount = sutureCount,
            graftCount = state.graftedTendrils,
            boneColor = colorScheme.onBackground,
            stitchColor = colorScheme.tertiary,
            shadowColor = colorScheme.background,
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
          val jarBoost =
            if (SpecimenJar.ALMOST_A_NAME in state.ownedJars) OrganBalance.ALMOST_A_NAME_MULTIPLIER
            else 1.0
          val feedbackLabel =
            when (anomaly.type) {
              WallAnomalyType.PALE_TOOTH ->
                stringResource(
                  R.string.anomaly_tooth_feedback,
                  formatCompactDecimal(OrganBalance.TOOTH_BASE_ICHOR_REWARD * state.scarMultiplier * jarBoost),
                )
              WallAnomalyType.GLASS_EYE -> stringResource(R.string.anomaly_eye_feedback)
              WallAnomalyType.SWOLLEN_NODULE ->
                stringResource(
                  R.string.anomaly_nodule_feedback,
                  formatCompactDecimal(OrganBalance.NODULE_BASE_ICHOR_REWARD * state.scarMultiplier * jarBoost),
                  formatCompactDecimal(
                    OrganBalance.NODULE_BASE_STILLNESS_REWARD * state.scarStillnessMultiplier * jarBoost
                  ),
                )
            }
          WallAnomalyNode(
            anomaly = anomaly,
            breathPhaseProvider = breathPhaseProvider,
            onInteract = {
              onInteractAnomaly(anomaly.id)
              val id = nextResidueId++
              if (residues.size >= 14) {
                residues.removeAt(0)
              }
              residues.add(ValveResidue(id = id, label = feedbackLabel, xDriftDp = 0f))
            },
            modifier = Modifier.offset(x = offsetX, y = offsetY),
          )
        }
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

internal fun formatMultiplier(value: Double): String {
  val rounded = (value * 100.0).roundToInt() / 100.0
  return NumberFormat.getNumberInstance()
    .apply {
      minimumFractionDigits = 0
      maximumFractionDigits = 2
      isGroupingUsed = false
    }
    .format(rounded)
}
