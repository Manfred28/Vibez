package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.AssetDialogueRepository
import com.example.game.CreatureType
import com.example.game.OrganBalance
import com.example.game.OrganDeckTab
import com.example.game.OrganGameState
import com.example.game.SpecimenJar
import com.example.ui.components.ApparatusDeck
import com.example.ui.components.ChamberCanvas
import com.example.ui.components.FragmentArchiveSheet
import com.example.ui.components.TransmissionLedgerPanel
import com.example.ui.theme.BleakTheme
import java.text.NumberFormat

@Composable
fun OrganRoute(modifier: Modifier = Modifier, viewModel: OrganViewModel? = null) {
  val resolvedViewModel =
    viewModel
      ?: run {
        val appContext = LocalContext.current.applicationContext
        val factory =
          remember(appContext) { OrganViewModel.factory(AssetDialogueRepository(appContext)) }
        viewModel(factory = factory)
      }

  val uiState by resolvedViewModel.uiState.collectAsStateWithLifecycle()

  BleakTheme(phase = uiState.phase) {
    OrganScreen(
      state = uiState,
      onPressValve = resolvedViewModel::pressValve,
      onBuyCreature = resolvedViewModel::buyCreature,
      onBottleJar = resolvedViewModel::bottleJar,
      onPerformGraft = resolvedViewModel::performGraft,
      onInteractAnomaly = resolvedViewModel::interactAnomaly,
      onPerformExcision = resolvedViewModel::performExcision,
      modifier = modifier,
    )
  }
}

@Composable
fun OrganScreen(
  state: OrganGameState,
  onPressValve: () -> Unit,
  onBuyCreature: (CreatureType) -> Unit,
  onBottleJar: (SpecimenJar) -> Unit,
  onPerformGraft: () -> Unit,
  onInteractAnomaly: (Long) -> Unit,
  onPerformExcision: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val atmosphere = BleakTheme.atmosphere

  var selectedTab by remember { mutableStateOf(OrganDeckTab.APPARATUS) }
  var archiveVisible by remember { mutableStateOf(false) }
  val scrollState = rememberScrollState()

  LaunchedEffect(state.cycle) {
    if (state.cycle > 0) {
      scrollState.animateScrollTo(0)
    }
  }

  val phaseTitle = (state.phaseName ?: defaultPhaseName(state.phase)).uppercase()

  // Deferred breathing transition (read strictly inside draw phase to avoid 60fps recomposition)
  val infiniteTransition = rememberInfiniteTransition(label = "organ_breathing")
  val breathPhaseState =
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = atmosphere.pulseDurationMs, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "breath_phase",
    )

  Scaffold(
    modifier =
      modifier.fillMaxSize().drawBehind {
        drawRect(color = colorScheme.background)

        val breath = breathPhaseState.value
        val radiusFactor = 0.60f + breath * 0.14f
        val driftX = (breath - 0.5f) * size.width * 0.08f * (0.4f + atmosphere.distortionFactor)
        val driftY = (0.5f - breath) * size.height * 0.04f

        drawCircle(
          brush =
            Brush.radialGradient(
              colors =
                listOf(
                  atmosphere.auraPrimary.copy(alpha = atmosphere.auraPrimaryAlpha),
                  Color.Transparent,
                ),
              center = Offset(size.width * 0.5f + driftX, size.height * 0.45f + driftY),
              radius = size.minDimension * radiusFactor,
            ),
          radius = size.minDimension * radiusFactor,
          center = Offset(size.width * 0.5f + driftX, size.height * 0.45f + driftY),
        )

        drawCircle(
          brush =
            Brush.radialGradient(
              colors =
                listOf(
                  atmosphere.auraGrotesque.copy(alpha = atmosphere.auraGrotesqueAlpha),
                  Color.Transparent,
                ),
              center = Offset(size.width * 0.24f - driftX, size.height * 0.20f),
              radius = size.minDimension * 0.54f,
            ),
          radius = size.minDimension * 0.54f,
          center = Offset(size.width * 0.24f - driftX, size.height * 0.20f),
        )

        val stepPx = 10.dp.toPx().coerceAtLeast(6f)
        var y = 0f
        val scanlineColor = colorScheme.onBackground.copy(alpha = atmosphere.scanlineAlpha)
        while (y < size.height) {
          drawLine(scanlineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
          y += stepPx
        }
      },
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets.safeDrawing,
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
      Column(
        modifier =
          Modifier.fillMaxSize()
            .widthIn(max = 600.dp)
            .align(Alignment.TopCenter)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        PhaseTelemetryHeader(state = state, phaseTitle = phaseTitle)

        ChamberCanvas(
          state = state,
          breathPhaseProvider = { breathPhaseState.value },
          onPressValve = onPressValve,
          onInteractAnomaly = onInteractAnomaly,
          modifier = Modifier.height(296.dp),
        )

        AnimatedVisibility(
          visible = state.isApparatusUnlocked,
          enter = fadeIn(tween(300)),
        ) {
          ApparatusDeck(
            state = state,
            selectedTab = selectedTab,
            onSelectTab = { selectedTab = it },
            onBuyCreature = onBuyCreature,
            onBottleJar = onBottleJar,
            onPerformGraft = onPerformGraft,
            onPerformExcision = {
              selectedTab = OrganDeckTab.APPARATUS
              onPerformExcision()
            },
          )
        }

        TransmissionLedgerPanel(
          activeDialogue = state.activeDialogue,
          discoveredCount = state.discoveredDialogues.size,
          onOpenArchive = { archiveVisible = true },
        )

        Spacer(modifier = Modifier.height(6.dp))
      }
    }
  }

  if (archiveVisible) {
    FragmentArchiveSheet(
      fragments = state.discoveredDialogues,
      onDismiss = { archiveVisible = false },
    )
  }
}

@Composable
private fun PhaseTelemetryHeader(
  state: OrganGameState,
  phaseTitle: String,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val atmosphere = BleakTheme.atmosphere

  val formattedThresholds =
    remember(state.cycleLifetimeIchorDisplay, state.nextPhaseThreshold) {
      val next = state.nextPhaseThreshold
      if (next != null) {
        val nf = NumberFormat.getIntegerInstance()
        nf.format(state.cycleLifetimeIchorDisplay) to nf.format(next)
      } else {
        null
      }
    }

  val formattedExcisionProgress =
    remember(state.cycleLifetimeIchorDisplay) {
      val nf = NumberFormat.getIntegerInstance()
      nf.format(state.cycleLifetimeIchorDisplay) to nf.format(OrganBalance.EXCISION_ICHOR_THRESHOLD)
    }

  val formattedScar =
    remember(state.scarTissue) { NumberFormat.getIntegerInstance().format(state.scarTissue) }

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.phase_readout, state.phase, phaseTitle),
        style = MaterialTheme.typography.labelMedium,
        color = colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("phase_header"),
      )
      Text(
        text = stringResource(R.string.transmission_phase_badge, state.phase),
        style = MaterialTheme.typography.labelSmall,
        color = colorScheme.tertiary,
      )
    }

    if (state.cycle > 0 || state.scarTissue > 0L) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = stringResource(R.string.cycle_readout, state.cycle, formattedScar),
        style = MaterialTheme.typography.labelSmall,
        color = colorScheme.primary,
        modifier = Modifier.testTag("cycle_status"),
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    LinearProgressIndicator(
      progress = { state.phaseProgress.coerceIn(0f, 1f) },
      modifier = Modifier.fillMaxWidth().height(2.dp).testTag("phase_progress_bar"),
      color = if (state.phase >= 2) colorScheme.tertiary else colorScheme.primary,
      trackColor = atmosphere.hairline,
      drawStopIndicator = {},
    )

    Spacer(modifier = Modifier.height(5.dp))

    val thresholdText =
      when {
        state.canExcise -> stringResource(R.string.threshold_excision_ready)
        formattedThresholds != null ->
          stringResource(
            R.string.threshold_progress_format,
            formattedThresholds.first,
            formattedThresholds.second,
          )
        state.phase >= 4 ->
          stringResource(
            R.string.threshold_excision_progress_format,
            formattedExcisionProgress.first,
            formattedExcisionProgress.second,
          )
        else -> stringResource(R.string.threshold_endless_label)
      }

    Text(
      text = thresholdText,
      style = MaterialTheme.typography.labelSmall,
      color =
        if (state.canExcise) colorScheme.tertiary
        else colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
      modifier = Modifier.testTag("threshold_status"),
    )
  }
}

@Composable
private fun defaultPhaseName(phase: Int): String =
  when (phase.coerceIn(0, 4)) {
    0 -> stringResource(R.string.default_phase_0_name)
    1 -> stringResource(R.string.default_phase_1_name)
    2 -> stringResource(R.string.default_phase_2_name)
    3 -> stringResource(R.string.default_phase_3_name)
    else -> stringResource(R.string.default_phase_4_name)
  }

@Preview(showBackground = true)
@Composable
private fun OrganScreenPhase0Preview() {
  BleakTheme(phase = 0) {
    OrganScreen(
      state = OrganGameState(phaseName = "The Counting Room"),
      onPressValve = {},
      onBuyCreature = {},
      onBottleJar = {},
      onPerformGraft = {},
      onInteractAnomaly = {},
      onPerformExcision = {},
    )
  }
}
