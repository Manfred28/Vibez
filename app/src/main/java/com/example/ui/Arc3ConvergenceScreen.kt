package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.Arc3Balance
import com.example.game.Arc3DeckTab
import com.example.game.Arc3UnmakeCategory
import com.example.game.OrganGameState
import com.example.ui.components.Arc3BreachCanvas
import com.example.ui.components.Arc3BreachDeck
import com.example.ui.components.ArcTopChromeBar
import com.example.ui.components.CrimsonFloodOverlay
import com.example.ui.components.DrippingTransmissionOverlay
import com.example.ui.components.FragmentArchiveSheet
import com.example.ui.components.PinnedLedgerBottomBar
import com.example.ui.components.rememberEventDripChannel
import com.example.ui.theme.ArterialCrimson
import com.example.ui.theme.ArterialShadow
import com.example.ui.theme.BloodSoakedPaper
import com.example.ui.theme.BrightMeniscus
import com.example.ui.theme.DeepClotTide
import com.example.ui.theme.FreshSeepage
import java.text.NumberFormat
import kotlin.math.ceil
import kotlin.random.Random

/**
 * Arc 3 ("The Unmaking") full playable screen:
 * 1. Active Breach (Strata I..V): counter-clockwise wrenching, forgiving Clotting tension,
 *    18 chamber-dismantling Unmaking acts, urgent wall ruptures, 5 irreversible Memory Wedges,
 *    and rising Abstract Crimson Flood.
 * 2. Interactive 5-Pull Finale: bottom deck collapses and chrome progressively dissolves across
 *    5 heavy deliberate pulls until total crimson submersion.
 * 3. Submerged Aftermath: full-bleed breathing crimson liquid field with ambiguous quiet finality
 *    (red is the end — only the Fragment Archive and clinical Dev Console remain accessible).
 */
@Composable
fun Arc3ConvergenceScreen(
  state: OrganGameState,
  actions: Arc3BreachActions = Arc3BreachActions(),
  onOpenDevTools: () -> Unit = {},
  isAudioMuted: Boolean = false,
  onToggleAudioMute: () -> Unit = {},
  modifier: Modifier = Modifier,
  eventDripRoll: () -> Double = { Random.nextDouble() },
) {
  val breach = state.arc3
  val colorScheme = MaterialTheme.colorScheme
  var selectedTab by remember { mutableStateOf(Arc3DeckTab.UNMAKE) }
  var archiveVisible by remember { mutableStateOf(false) }
  val scrollState = rememberScrollState()

  val isFreshArc3 = breach.wrenchCount == 0 && breach.lockedWedges.isEmpty() && !breach.submersionComplete
  val dripChannel =
    rememberEventDripChannel(
      speakerLabel = stringResource(R.string.drip_arc3_speaker),
      isFreshReset = isFreshArc3,
    )

  val infiniteTransition = rememberInfiniteTransition(label = "arc3_breach_motion")
  val breathPhaseState =
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = if (breach.submersionComplete) 4200 else 1900, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "arc3_breath",
    )
  val wavePhaseState =
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = if (breach.submersionComplete) 5600 else 3200, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "arc3_wave",
    )

  val dripJar = stringResource(R.string.drip_arc3_jar_shatter)
  val dripTear = stringResource(R.string.drip_arc3_unmake_tear)
  val dripWedge = stringResource(R.string.drip_arc3_wedge_drive)

  val animatedFlood by
    animateFloatAsState(
      targetValue = breach.floodLevel.coerceIn(0f, 1f),
      animationSpec = tween(durationMillis = 700),
      label = "arc3_screen_flood",
    )

  Scaffold(
    modifier = modifier.fillMaxSize().background(ArterialShadow).testTag("arc3_screen"),
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets.safeDrawing,
    bottomBar = {
      PinnedLedgerBottomBar(
        activeDialogue = state.activeDialogue,
        discoveredCount = state.discoveredDialogues.size,
        onOpenArchive = { archiveVisible = true },
      )
    },
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize()) {
      // Full-bleed Abstract Crimson Flood backdrop intensifying into Finale & Submersion
      if (breach.isInFinale || breach.submersionComplete) {
        Image(
          painter = painterResource(id = R.drawable.img_arc3_crimson_flood),
          contentDescription = stringResource(R.string.art_arc3_crimson_flood_cd),
          contentScale = ContentScale.Crop,
          modifier =
            Modifier.fillMaxSize()
              .graphicsLayer {
                val b = breathPhaseState.value
                val s = 1.03f + b * 0.04f
                scaleX = s
                scaleY = s
              }
              .testTag("art_arc3_crimson_flood"),
        )
        CrimsonFloodOverlay(
          floodLevel = animatedFlood,
          wavePhaseProvider = { wavePhaseState.value },
          breathPhaseProvider = { breathPhaseState.value },
          modifier = Modifier.fillMaxSize(),
        )
      }

      if (breach.submersionComplete) {
        Arc3SubmergedAftermathContent(
          discoveredCount = state.discoveredDialogues.size,
          onOpenArchive = { archiveVisible = true },
          onOpenDevTools = onOpenDevTools,
          isAudioMuted = isAudioMuted,
          onToggleAudioMute = onToggleAudioMute,
          modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
      } else if (breach.isInFinale) {
        Arc3FinaleDissolutionContent(
          state = state,
          breathPhaseProvider = { breathPhaseState.value },
          onPerformFinalPull = actions.onPerformFinalPull,
          onOpenDevTools = onOpenDevTools,
          isAudioMuted = isAudioMuted,
          onToggleAudioMute = onToggleAudioMute,
          modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
      } else {
        // Active Breach Mode (Strata I..V)
        Box(
          modifier =
            Modifier.fillMaxSize()
              .padding(innerPadding)
              .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
          Column(
            modifier =
              Modifier.fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Arc3TelemetryHeader(
              state = state,
              breathPhaseProvider = { breathPhaseState.value },
              onOpenDevTools = onOpenDevTools,
              isAudioMuted = isAudioMuted,
              onToggleAudioMute = onToggleAudioMute,
            )

            Arc3BreachCanvas(
              state = state,
              breathPhaseProvider = { breathPhaseState.value },
              wavePhaseProvider = { wavePhaseState.value },
              onWrenchBreach = actions.onWrenchBreach,
              onTearRupture = actions.onTearRupture,
              onPerformUnmake = { act ->
                dripChannel.emit(
                  "arc3_unmake_${act.id}",
                  if (act.category == Arc3UnmakeCategory.SHATTER_JAR) dripJar else dripTear,
                )
                actions.onPerformUnmake(act)
              },
              onDriveWedge = {
                dripChannel.emit("arc3_wedge_${breach.currentStratum}", dripWedge)
                actions.onDriveWedge()
              },
              onEmitThematicDrip = dripChannel::emit,
              modifier = Modifier.height(312.dp),
            )

            AnimatedVisibility(
              visible = !breach.isInFinale,
              exit = fadeOut(tween(400)) + shrinkVertically(tween(400)),
            ) {
              Arc3BreachDeck(
                state = state,
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it },
                onPerformUnmake = { act ->
                  dripChannel.emit(
                    "arc3_unmake_${act.id}",
                    if (act.category == Arc3UnmakeCategory.SHATTER_JAR) dripJar else dripTear,
                  )
                  actions.onPerformUnmake(act)
                },
                onDriveWedge = {
                  dripChannel.emit("arc3_wedge_${breach.currentStratum}", dripWedge)
                  actions.onDriveWedge()
                },
              )
            }

            Spacer(modifier = Modifier.height(8.dp))
          }
        }
      }

      // Dual-lane dripping story/event overlay
      DrippingTransmissionOverlay(
        activeDialogue = state.activeDialogue,
        latestEvent = dripChannel.latestEvent,
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        eventDripRoll = eventDripRoll,
      )
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
private fun Arc3TelemetryHeader(
  state: OrganGameState,
  breathPhaseProvider: () -> Float,
  onOpenDevTools: () -> Unit,
  isAudioMuted: Boolean,
  onToggleAudioMute: () -> Unit,
) {
  val breach = state.arc3
  val colorScheme = MaterialTheme.colorScheme
  val numberFormat = remember { NumberFormat.getIntegerInstance() }
  val blur = breach.telemetryBlurFactor
  val showBlurLabels = blur >= 0.35f && breathPhaseProvider() > 0.56f

  val headerTitle = if (showBlurLabels) breach.activeStratum.blurTitle else breach.activeStratum.title
  val hemLabel =
    stringResource(if (showBlurLabels) R.string.arc3_hemorrhage_blur_label else R.string.arc3_hemorrhage_label)
  val fracLabel =
    stringResource(if (showBlurLabels) R.string.arc3_fracture_blur_label else R.string.arc3_fracture_label)

  Column(modifier = Modifier.fillMaxWidth()) {
    ArcTopChromeBar(
      title = headerTitle,
      titleStyle = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.4.sp),
      titleColor = BrightMeniscus,
      titleTestTag = "arc3_stratum_header",
      isAudioMuted = isAudioMuted,
      onToggleAudioMute = onToggleAudioMute,
      onOpenDevTools = onOpenDevTools,
      emphasizedPills = true,
    )

    Spacer(modifier = Modifier.height(6.dp))
    LinearProgressIndicator(
      progress = { breach.stratumProgress.coerceIn(0f, 1f) },
      modifier = Modifier.fillMaxWidth().height(3.5.dp).testTag("arc3_fracture_progress_bar"),
      color = if (breach.canDriveWedge) BrightMeniscus else ArterialCrimson,
      trackColor = DeepClotTide,
    )

    Spacer(modifier = Modifier.height(6.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(
          text = "$fracLabel: ${numberFormat.format(breach.fractureDisplay)} / ${numberFormat.format(breach.activeStratum.fractureGoal.toLong())}",
          style = MaterialTheme.typography.labelSmall,
          color = BloodSoakedPaper,
          modifier = Modifier.testTag("arc3_fracture_readout"),
        )
        Text(
          text = "$hemLabel: ${numberFormat.format(breach.hemorrhageDisplay)}",
          style = MaterialTheme.typography.labelSmall,
          color = BrightMeniscus,
          modifier = Modifier.testTag("arc3_hemorrhage_readout"),
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        if (breach.isClotFrozen) {
          val frozenSec = ceil(maxOf(breach.clotFreezeRemainingSeconds, breach.frenzyRemainingSeconds)).toInt()
          StatusBadge(
            text = stringResource(R.string.arc3_clot_frozen_label, frozenSec),
            bg = FreshSeepage,
            fg = BloodSoakedPaper,
            tag = "arc3_clot_frozen_badge",
          )
        } else if (breach.isActivelyClotting) {
          val clotStr = String.format("%.1f", breach.activeClotPerSec)
          val bursting = breach.isClotBursting
          val clotText =
            stringResource(
              when {
                bursting -> R.string.arc3_clotting_surge_label
                showBlurLabels -> R.string.arc3_coagulation_label
                else -> R.string.arc3_clotting_label
              },
              clotStr,
            )
          StatusBadge(
            text = clotText,
            bg = if (bursting) ArterialCrimson else DeepClotTide,
            fg = if (bursting) BloodSoakedPaper else BrightMeniscus,
            tag = "arc3_clotting_badge",
          )
        }
        if (breach.frenzyRemainingSeconds > 0.0) {
          StatusBadge(
            text = stringResource(R.string.arc3_frenzy_label, ceil(breach.frenzyRemainingSeconds).toInt()),
            bg = ArterialCrimson,
            fg = BloodSoakedPaper,
            tag = "arc3_frenzy_badge",
          )
        } else if (breach.ribForceRemainingSeconds > 0.0) {
          StatusBadge(
            text = stringResource(R.string.arc3_rib_force_label, ceil(breach.ribForceRemainingSeconds).toInt()),
            bg = FreshSeepage,
            fg = BloodSoakedPaper,
            tag = "arc3_rib_force_badge",
          )
        }
      }
    }
  }
}

@Composable
private fun StatusBadge(text: String, bg: Color, fg: Color, tag: String) {
  Surface(
    shape = MaterialTheme.shapes.extraSmall,
    color = bg,
    border = BorderStroke(1.dp, fg.copy(alpha = 0.55f)),
    modifier = Modifier.testTag(tag),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
      color = fg,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
    )
  }
}

/**
 * Interactive 5-Pull UI Dissolution Finale:
 * Bottom deck is gone; chrome progressively dissolves across pulls 1..5 until total submersion.
 */
@Composable
private fun Arc3FinaleDissolutionContent(
  state: OrganGameState,
  breathPhaseProvider: () -> Float,
  onPerformFinalPull: () -> Unit,
  onOpenDevTools: () -> Unit,
  isAudioMuted: Boolean,
  onToggleAudioMute: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val breach = state.arc3
  val haptic = LocalHapticFeedback.current
  val pulls = breach.finalPullsCompleted
  val chromeAlpha = (1f - pulls * 0.21f).coerceIn(0.10f, 1f)
  val ready = breach.canPerformFinalPull
  val pullCd = stringResource(R.string.arc3_finale_pull_cd)

  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp).testTag("arc3_finale_container"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Box(modifier = Modifier.fillMaxWidth().alpha(chromeAlpha)) {
      ArcTopChromeBar(
        title = stringResource(R.string.arc3_finale_header),
        titleStyle = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
        titleColor = BloodSoakedPaper,
        titleTestTag = "arc3_finale_title",
        isAudioMuted = isAudioMuted,
        onToggleAudioMute = onToggleAudioMute,
        onOpenDevTools = onOpenDevTools,
        emphasizedPills = false,
      )
    }

    // Center lone deliberate pulsing pull button
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      val b = breathPhaseStateValue(breathPhaseProvider)
      val scale = if (ready) 1f + b * 0.05f else 0.94f
      Surface(
        modifier =
          Modifier.size(164.dp)
            .graphicsLayer {
              scaleX = scale
              scaleY = scale
            }
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .testTag("arc3_final_pull_button")
            .semantics { contentDescription = pullCd }
            .clickable(enabled = ready, role = Role.Button) {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onPerformFinalPull()
            },
        shape = CircleShape,
        color = if (ready) ArterialCrimson.copy(alpha = 0.86f) else DeepClotTide.copy(alpha = 0.65f),
        border = BorderStroke(2.dp, if (ready) BloodSoakedPaper.copy(alpha = 0.85f) else BrightMeniscus.copy(alpha = 0.35f)),
      ) {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(
                Brush.radialGradient(
                  colors = listOf(FreshSeepage.copy(alpha = 0.55f), DeepClotTide.copy(alpha = 0.85f))
                )
              ),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text =
                when {
                  !ready && breach.finalPullCooldownSeconds > 0.0 ->
                    stringResource(
                      R.string.arc3_finale_pull_wait_secs,
                      ceil(breach.finalPullCooldownSeconds).toInt().coerceAtLeast(1),
                    )
                  !ready -> stringResource(R.string.arc3_finale_pull_wait)
                  pulls >= Arc3Balance.FINAL_PULLS_TOTAL - 1 ->
                    stringResource(R.string.arc3_finale_pull_last_label)
                  else -> stringResource(R.string.arc3_finale_pull_label)
                },
              style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.6.sp),
              color = if (ready) BloodSoakedPaper else BloodSoakedPaper.copy(alpha = 0.62f),
              textAlign = TextAlign.Center,
            )
            if (!ready && breach.finalPullCooldownSeconds > 0.0) {
              Spacer(modifier = Modifier.height(8.dp))
              val cdFrac =
                (1.0 - breach.finalPullCooldownSeconds / Arc3Balance.FINAL_PULL_COOLDOWN_SEC)
                  .toFloat()
                  .coerceIn(0f, 1f)
              LinearProgressIndicator(
                progress = { cdFrac },
                modifier = Modifier.widthIn(max = 92.dp).height(3.dp),
                color = BloodSoakedPaper,
                trackColor = ArterialShadow,
              )
            }
          }
        }
      }

      // 5 dissolving crimson pips
      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.alpha((chromeAlpha + 0.15f).coerceAtMost(1f)),
      ) {
        repeat(Arc3Balance.FINAL_PULLS_TOTAL) { idx ->
          val filled = idx < pulls
          Box(
            modifier =
              Modifier.size(10.dp)
                .clip(CircleShape)
                .background(if (filled) BloodSoakedPaper else ArterialShadow.copy(alpha = 0.65f))
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))
  }
}

private fun breathPhaseStateValue(provider: () -> Float): Float = provider().coerceIn(0f, 1f)

/**
 * Quiet Submerged Aftermath:
 * Full-bleed breathing crimson liquid field with final stillness — red is the end.
 * Only the translucent Fragment Archive pill remains accessible above the ledger.
 */
@Composable
private fun Arc3SubmergedAftermathContent(
  discoveredCount: Int,
  onOpenArchive: () -> Unit,
  onOpenDevTools: () -> Unit,
  isAudioMuted: Boolean,
  onToggleAudioMute: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp).testTag("arc3_submerged_view"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    ArcTopChromeBar(
      title = stringResource(R.string.arc3_submerged_title),
      titleStyle = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp),
      titleColor = BloodSoakedPaper.copy(alpha = 0.65f),
      titleTestTag = "arc3_submerged_header",
      isAudioMuted = isAudioMuted,
      onToggleAudioMute = onToggleAudioMute,
      onOpenDevTools = onOpenDevTools,
      emphasizedPills = false,
      modifier = Modifier.alpha(0.76f),
    )

    Spacer(modifier = Modifier.weight(1f))

    // Lone translucent Archive pill resting quietly above the pinned ledger (red is the end)
    AftermathNavPill(
      label = "${stringResource(R.string.arc3_archive_pill)} [$discoveredCount]",
      testTag = "arc3_aftermath_archive_button",
      onClick = onOpenArchive,
      modifier = Modifier.widthIn(min = 164.dp, max = 240.dp).padding(bottom = 6.dp),
    )
  }
}

@Composable
private fun AftermathNavPill(
  label: String,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .testTag(testTag)
        .clickable(role = Role.Button, onClick = onClick),
    shape = MaterialTheme.shapes.extraSmall,
    color = ArterialShadow.copy(alpha = 0.58f),
    border = BorderStroke(1.dp, BloodSoakedPaper.copy(alpha = 0.28f)),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.9.sp),
      color = BloodSoakedPaper.copy(alpha = 0.85f),
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
    )
  }
}
