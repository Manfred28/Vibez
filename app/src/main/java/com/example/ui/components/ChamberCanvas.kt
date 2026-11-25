package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.game.GraftSacrificeKind
import com.example.game.OrganGameState
import com.example.game.WallAnomalyType
import com.example.ui.theme.BleakTheme
import java.text.NumberFormat
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.coroutines.launch

@Composable
fun ChamberCanvas(
  state: OrganGameState,
  breathPhaseProvider: () -> Float,
  dropPhaseProvider: () -> Float,
  onPressValve: () -> Unit,
  onInteractAnomaly: (Long) -> Unit,
  onEmitThematicDrip: (eventKey: String, text: String) -> Unit = { _, _ -> },
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val atmosphere = BleakTheme.atmosphere
  val scope = rememberCoroutineScope()

  val formattedIchor =
    remember(state.ichorDisplay) { NumberFormat.getIntegerInstance().format(state.ichorDisplay) }
  val counterCd = stringResource(R.string.counter_value_content_description, formattedIchor)
  val formattedTapYield =
    remember(state.manualTapYield) { formatValveYield(state.manualTapYield) }

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

  // Monotonic clockwise tap ratchet on the iron valve wheel (+24 deg per manual press)
  val tapWheelRotation = remember { Animatable(0f) }

  // Continuous Robolectric-safe infinite transitions for living chamber motion
  val chamberMotion = rememberInfiniteTransition(label = "chamber_living_motion")
  val leechPatrolState =
    chamberMotion.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 11500, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "leech_patrol",
    )
  val wallCreepState =
    chamberMotion.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 4200, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "wall_creep",
    )
  val arterialPulseState =
    chamberMotion.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1550, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "arterial_pulse",
    )

  // Suture-Hand driven clockwise wheel speed scales smoothly with count up to 110 deg/s visual cap
  val handSpeedDegPerSec =
    remember(sutureCount) {
      if (sutureCount <= 0) 0f
      else min(110f, 12f + 26f * ln(1f + sutureCount.toFloat()))
    }
  val handRevPeriodMs =
    remember(handSpeedDegPerSec) {
      if (handSpeedDegPerSec <= 0f) 18000
      else (360_000f / handSpeedDegPerSec).roundToInt().coerceIn(3272, 18000)
    }
  val handCyclePeriodMs =
    remember(sutureCount) {
      if (sutureCount <= 0) 3200
      else (3200f / (1f + 0.55f * ln(1f + sutureCount.toFloat()))).roundToInt().coerceIn(850, 3200)
    }

  val handMotion = rememberInfiniteTransition(label = "suture_hand_rotation")
  val handWheelAngleState =
    handMotion.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = handRevPeriodMs, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "hand_wheel_angle",
    )
  val handCycleState =
    handMotion.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = handCyclePeriodMs, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "hand_claw_cycle",
    )

  // Neighbor Aperture Echo slit ripple + subordinate thematic drip
  val echoAnswerText = stringResource(R.string.drip_echo_answer)
  val echoRipple = remember { Animatable(0f) }
  var echoInitialized by remember { mutableStateOf(false) }
  var lastEchoCount by remember { mutableIntStateOf(state.echoPulsesTriggered) }
  LaunchedEffect(state.echoPulsesTriggered) {
    if (!echoInitialized) {
      echoInitialized = true
      lastEchoCount = state.echoPulsesTriggered
      return@LaunchedEffect
    }
    if (state.echoPulsesTriggered > lastEchoCount) {
      lastEchoCount = state.echoPulsesTriggered
      onEmitThematicDrip("echo_answer", echoAnswerText)
      echoRipple.snapTo(0.01f)
      echoRipple.animateTo(1f, animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing))
      echoRipple.snapTo(0f)
    }
  }

  // Graft sacrifice subordinate thematic drip (paired with GraftSacrificeOverlay death FX)
  val sacrificeTendrilText = stringResource(R.string.drip_sacrifice_tendril)
  val sacrificeLungText = stringResource(R.string.drip_sacrifice_lung)
  var sacrificeDripInitialized by remember { mutableStateOf(false) }
  var lastSacrificeDripPulse by remember { mutableLongStateOf(state.lastSacrificePulse) }
  LaunchedEffect(state.lastSacrificePulse) {
    if (!sacrificeDripInitialized) {
      sacrificeDripInitialized = true
      lastSacrificeDripPulse = state.lastSacrificePulse
      return@LaunchedEffect
    }
    if (state.lastSacrificePulse > lastSacrificeDripPulse) {
      lastSacrificeDripPulse = state.lastSacrificePulse
      val isLung = state.lastSacrificeKind == GraftSacrificeKind.CHOIR_LUNG
      onEmitThematicDrip(
        if (isLung) "graft_choir_lung" else "graft_tendril",
        if (isLung) sacrificeLungText else sacrificeTendrilText,
      )
    }
  }

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
        wallCreepPhaseProvider = { wallCreepState.value },
        arterialPulsePhaseProvider = { arterialPulseState.value },
        echoRippleProvider = { echoRipple.value },
      )

      // Flanking Weeping Lamps & downward droplets — rendered behind foreground counter/income text
      WeepingLampsCeilingOverlay(
        lampCount = lampCount,
        isStillnessActive = state.isStillnessActive,
        breathPhaseProvider = breathPhaseProvider,
        dropPhaseProvider = dropPhaseProvider,
      )

      // Twin lower-flank iron specimen ledges holding sealed Specimen Jars (behind interactive overlays)
      ChamberSpecimenShelves(
        ownedJars = state.ownedJars,
        isStillnessActive = state.isStillnessActive,
        breathPhaseProvider = breathPhaseProvider,
        arterialPulsePhaseProvider = { arterialPulseState.value },
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

          // Recessed damp counter glass plate with perimeter-crawling Leech-Clerks
          Box(contentAlignment = Alignment.Center) {
            Surface(
              modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
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
                patrolPhaseProvider = { leechPatrolState.value },
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
            if (state.marrowBloomRemainingSeconds > 0.0) {
              Text(
                text =
                  stringResource(
                    R.string.bloom_active_tag,
                    ceil(state.marrowBloomRemainingSeconds).toInt(),
                  ),
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.tertiary,
                modifier = Modifier.testTag("bloom_active_badge"),
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

          val valveYieldLabel =
            if (state.isValveClamped) {
              stringResource(R.string.valve_clamped_label, ceil(state.clampRemainingSeconds).toInt())
            } else {
              stringResource(R.string.valve_plus_format, formattedTapYield)
            }
          RecessedValveButton(
            yieldLabel = valveYieldLabel,
            wheelRotationDegreesProvider = {
              val handContribution =
                if (sutureCount > 0 && !state.isValveClamped) handWheelAngleState.value else 0f
              tapWheelRotation.value + handContribution
            },
            onPress = {
              if (!state.isValveClamped) {
                onPressValve()
                scope.launch {
                  tapWheelRotation.animateTo(
                    targetValue = tapWheelRotation.targetValue + 24f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                  )
                }
                val id = nextResidueId++
                val drift = ((id % 5) - 2) * 14f
                if (residues.size >= 14) {
                  residues.removeAt(0)
                }
                residues.add(ValveResidue(id = id, label = "+$formattedTapYield", xDriftDp = drift))
              }
            },
          )

          SutureHandsValveOverlay(
            breathPhaseProvider = breathPhaseProvider,
            handCyclePhaseProvider = {
              if (sutureCount > 0 && !state.isValveClamped) handCycleState.value
              else breathPhaseProvider()
            },
            sutureCount = sutureCount,
            graftCount = state.graftedTendrils,
            choirLungCount = state.choirLungs,
            isStillnessActive = state.isStillnessActive,
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

      // Grotesque creature death/dissolution animation during Grafting sacrifices
      GraftSacrificeOverlay(
        sacrificePulse = state.lastSacrificePulse,
        sacrificeKind = state.lastSacrificeKind,
      )

      // Wall anomalies emit slow thematic drips to the subordinate left margin (never fast popups)
      val toothDrip = stringResource(R.string.drip_anomaly_tooth)
      val eyeDrip = stringResource(R.string.drip_anomaly_eye)
      val noduleDrip = stringResource(R.string.drip_anomaly_nodule)
      val ribDrip = stringResource(R.string.drip_anomaly_rib)

      state.activeAnomalies.forEach { anomaly ->
        key(anomaly.id) {
          val offsetX = (boxWidth - 62.dp) * anomaly.xFraction
          val offsetY = (boxHeight - 62.dp) * anomaly.yFraction
          val dripLine =
            when (anomaly.type) {
              WallAnomalyType.PALE_TOOTH -> toothDrip
              WallAnomalyType.GLASS_EYE -> eyeDrip
              WallAnomalyType.SWOLLEN_NODULE -> noduleDrip
              WallAnomalyType.CHITIN_RIB -> ribDrip
            }
          WallAnomalyNode(
            anomaly = anomaly,
            breathPhaseProvider = breathPhaseProvider,
            onInteract = {
              onInteractAnomaly(anomaly.id)
              onEmitThematicDrip("anomaly_${anomaly.type.id}", dripLine)
            },
            modifier = Modifier.offset(x = offsetX, y = offsetY),
          )
        }
      }
    }
  }
}

internal fun formatCompactDecimal(value: Double): String {
  val roundedTenths = (value * 10.0).roundToLong()
  val whole = roundedTenths / 10L
  val frac = kotlin.math.abs(roundedTenths % 10L)
  return if (frac == 0L) {
    NumberFormat.getIntegerInstance().format(whole)
  } else {
    NumberFormat.getNumberInstance()
      .apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
      }
      .format(roundedTenths / 10.0)
  }
}

internal fun formatValveYield(value: Double): String {
  if (value < 10_000.0) return formatCompactDecimal(value)
  val (scaled, suffix) =
    when {
      value < 1_000_000.0 -> value / 1_000.0 to "K"
      value < 1_000_000_000.0 -> value / 1_000_000.0 to "M"
      value < 1_000_000_000_000.0 -> value / 1_000_000_000.0 to "B"
      else -> value / 1_000_000_000_000.0 to "T"
    }
  val maxDecimals =
    when {
      scaled < 10.0 -> 2
      scaled < 100.0 -> 1
      else -> 0
    }
  val formatted =
    NumberFormat.getNumberInstance()
      .apply {
        minimumFractionDigits = 0
        maximumFractionDigits = maxDecimals
        isGroupingUsed = false
      }
      .format(scaled)
  return "$formatted$suffix"
}

internal fun formatMultiplier(value: Double): String {
  val rounded = (value * 100.0).roundToLong() / 100.0
  return NumberFormat.getNumberInstance()
    .apply {
      minimumFractionDigits = 0
      maximumFractionDigits = 2
      isGroupingUsed = false
    }
    .format(rounded)
}
