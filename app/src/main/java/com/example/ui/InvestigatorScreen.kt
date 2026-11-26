package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.CaseDeduction
import com.example.game.InvestigationChapter
import com.example.game.InvestigatorContact
import com.example.game.InvestigatorDeckTab
import com.example.game.OrganGameState
import com.example.ui.components.ArcTopChromeBar
import com.example.ui.components.DeckTabCell
import com.example.ui.components.DrippingTransmissionOverlay
import com.example.ui.components.FragmentArchiveSheet
import com.example.ui.components.InvestigatorDeskCanvas
import com.example.ui.components.PinnedLedgerBottomBar
import com.example.ui.components.TabAlertEvaluator
import com.example.ui.components.formatCompactDecimal
import com.example.ui.components.rememberEventDripChannel
import com.example.ui.theme.BleakTheme
import java.text.NumberFormat
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun InvestigatorScreen(
  state: OrganGameState,
  actions: InvestigatorActions,
  onOpenDevTools: () -> Unit,
  isAudioMuted: Boolean = false,
  onToggleAudioMute: () -> Unit = {},
  modifier: Modifier = Modifier,
  eventDripRoll: () -> Double = { Random.nextDouble() },
) {
  val colorScheme = MaterialTheme.colorScheme
  val atmosphere = BleakTheme.atmosphere
  val desk = state.arc2
  val nf = remember { NumberFormat.getIntegerInstance() }

  var selectedTab by remember { mutableStateOf(InvestigatorDeckTab.CONTACTS) }
  var seenArc2Keys by remember {
    mutableStateOf(
      mapOf(selectedTab to TabAlertEvaluator.arc2UnlockKeys(state, selectedTab))
    )
  }
  val activeArc2Keys = TabAlertEvaluator.arc2UnlockKeys(state, selectedTab)
  LaunchedEffect(selectedTab, activeArc2Keys) {
    val existing = seenArc2Keys[selectedTab].orEmpty()
    if (!existing.containsAll(activeArc2Keys)) {
      seenArc2Keys = seenArc2Keys + (selectedTab to (existing + activeArc2Keys))
    }
  }
  var archiveVisible by remember { mutableStateOf(false) }
  val scrollState = rememberScrollState()
  val isFreshReset =
    desk.lifetimeLeads == 0.0 &&
      desk.completedChapters == 0 &&
      desk.deskAnomalyInteractions.isEmpty() &&
      state.discoveredDialogues.isEmpty()
  val dripChannel =
    rememberEventDripChannel(
      speakerLabel = stringResource(R.string.drip_desk_event_speaker),
      isFreshReset = isFreshReset,
    )

  val infiniteTransition = rememberInfiniteTransition(label = "desk_haunting")
  val breathState =
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = atmosphere.pulseDurationMs, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "desk_breath",
    )
  val dropState =
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 2400, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "desk_drop",
    )

  Scaffold(
    modifier = modifier.fillMaxSize().background(colorScheme.background).testTag("investigator_screen"),
    containerColor = colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    bottomBar = {
      PinnedLedgerBottomBar(
        activeDialogue = state.activeDialogue,
        discoveredCount = state.discoveredDialogues.size,
        onOpenArchive = { archiveVisible = true },
      )
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 18.dp, vertical = 12.dp)
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
        // Precinct Case Header + Shared Top Chrome
        Column(modifier = Modifier.fillMaxWidth()) {
          ArcTopChromeBar(
            title =
              if (desk.fileZeroRevealed) stringResource(R.string.arc2_header_file_zero)
              else stringResource(R.string.arc2_header),
            titleStyle = MaterialTheme.typography.labelMedium,
            titleColor = colorScheme.onSurfaceVariant,
            titleTestTag = "arc2_phase_header",
            isAudioMuted = isAudioMuted,
            onToggleAudioMute = onToggleAudioMute,
            onOpenDevTools = onOpenDevTools,
            emphasizedPills = true,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text =
              stringResource(
                R.string.arc2_complicity_readout,
                (desk.complicity * 100f).roundToInt(),
                nf.format(desk.silenceDisplay),
              ),
            style = MaterialTheme.typography.labelSmall,
            color = if (desk.complicity >= 0.7f) colorScheme.tertiary else colorScheme.primary,
            modifier = Modifier.testTag("arc2_complicity_status"),
          )
          Spacer(modifier = Modifier.height(6.dp))
          LinearProgressIndicator(
            progress = { desk.complicity.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = if (desk.fileZeroRevealed) colorScheme.tertiary else colorScheme.primary,
            trackColor = atmosphere.hairline,
            drawStopIndicator = {},
          )
        }

        // File #00 Climax Banner & Arc 3 Transition Gate
        if (desk.fileZeroRevealed) {
          Surface(
            modifier = Modifier.fillMaxWidth().testTag("file_zero_reveal_card"),
            shape = MaterialTheme.shapes.extraSmall,
            color = colorScheme.tertiaryContainer.copy(alpha = 0.85f),
            border = BorderStroke(1.5.dp, colorScheme.tertiary),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = stringResource(R.string.arc2_file_zero_banner),
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onTertiaryContainer,
              )
              Surface(
                modifier =
                  Modifier.fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("enter_arc3_button")
                    .clickable(role = Role.Button, onClick = actions.onEnterArc3),
                shape = MaterialTheme.shapes.extraSmall,
                color = colorScheme.errorContainer,
                border = BorderStroke(1.dp, colorScheme.error),
              ) {
                Text(
                  text = stringResource(R.string.arc2_enter_arc3),
                  style = MaterialTheme.typography.labelLarge,
                  color = colorScheme.onErrorContainer,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                )
              }
            }
          }
        }

        InvestigatorDeskCanvas(
          state = state,
          breathPhaseProvider = { breathState.value },
          dropPhaseProvider = { dropState.value },
          onInspectCaseFile = actions.onInspectCaseFile,
          onInteractDeskAnomaly = actions.onInteractDeskAnomaly,
          onEmitThematicDrip = dripChannel::emit,
          modifier = Modifier.height(276.dp),
        )

        // Lower Investigation Deck
        Surface(
          modifier = Modifier.fillMaxWidth().testTag("investigator_deck"),
          shape = MaterialTheme.shapes.small,
          color = colorScheme.surface.copy(alpha = 0.92f),
          border = BorderStroke(1.dp, atmosphere.hairline),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()) {
              InvestigatorDeckTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                val label =
                  when (tab) {
                    InvestigatorDeckTab.CONTACTS -> stringResource(R.string.arc2_tab_contacts)
                    InvestigatorDeckTab.DEDUCTIONS -> stringResource(R.string.arc2_tab_deductions)
                    InvestigatorDeckTab.EVIDENCE_BOARD -> stringResource(R.string.arc2_tab_board)
                  }
                val isAlerting =
                  TabAlertEvaluator.shouldAlertArc2Tab(
                    state = state,
                    tab = tab,
                    activeTab = selectedTab,
                    seenKeysByTab = seenArc2Keys,
                  )
                DeckTabCell(
                  label = label,
                  isSelected = selected,
                  isAlerting = isAlerting,
                  isExcisionAccent = false,
                  tabTestTag = "arc2_tab_${tab.name.lowercase()}",
                  alertPipTestTag = "arc2_tab_alert_${tab.name.lowercase()}",
                  onClick = {
                    val unlocked = TabAlertEvaluator.arc2UnlockKeys(state, tab)
                    seenArc2Keys = seenArc2Keys + (tab to (seenArc2Keys[tab].orEmpty() + unlocked))
                    selectedTab = tab
                  },
                  modifier = Modifier.weight(1f),
                )
              }
            }
            HorizontalDivider(color = atmosphere.hairline)

            Column(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              when (selectedTab) {
                InvestigatorDeckTab.CONTACTS -> {
                  InvestigatorContact.entries.forEach { contact ->
                    val count = desk.contactCount(contact)
                    val cost = desk.contactCost(contact)
                    val canAfford = desk.leads >= cost.toDouble()
                    val title =
                      when (contact) {
                        InvestigatorContact.NIGHT_ARCHIVIST ->
                          stringResource(R.string.arc2_contact_archivist)
                        InvestigatorContact.BRIBED_ORDERLY ->
                          stringResource(R.string.arc2_contact_orderly)
                        InvestigatorContact.MICROFILM_SCANNER ->
                          stringResource(R.string.arc2_contact_scanner)
                        InvestigatorContact.AUDIO_TRANSCRIBER ->
                          stringResource(R.string.arc2_contact_transcriber)
                      }
                    val desc =
                      when (contact) {
                        InvestigatorContact.NIGHT_ARCHIVIST ->
                          stringResource(R.string.arc2_contact_archivist_desc)
                        InvestigatorContact.BRIBED_ORDERLY ->
                          stringResource(R.string.arc2_contact_orderly_desc)
                        InvestigatorContact.MICROFILM_SCANNER ->
                          stringResource(R.string.arc2_contact_scanner_desc)
                        InvestigatorContact.AUDIO_TRANSCRIBER ->
                          stringResource(R.string.arc2_contact_transcriber_desc)
                      }
                    val each = formatCompactDecimal(desk.effectiveContactLeadsPerSec(contact))
                    InvestigatorActionRow(
                      title = "$title [$count]",
                      subtitle = "$desc · +$each/s",
                      actionLabel = "${nf.format(cost)} LEADS",
                      enabled = canAfford,
                      accent = false,
                      testTag = "recruit_${contact.id}",
                      onClick = { actions.onRecruitContact(contact) },
                    )
                  }
                }
                InvestigatorDeckTab.DEDUCTIONS -> {
                  CaseDeduction.entries.forEach { ded ->
                    val owned = ded in desk.ownedDeductions
                    val canAfford = !owned && desk.silence >= ded.silenceCost
                    val title =
                      when (ded) {
                        CaseDeduction.HYDRO_WARD_INVOICES ->
                          stringResource(R.string.arc2_deduction_invoices)
                        CaseDeduction.MISSING_CARETAKER_ROSTER ->
                          stringResource(R.string.arc2_deduction_roster)
                        CaseDeduction.MORTAR_SAMPLE_MATCH ->
                          stringResource(R.string.arc2_deduction_mortar)
                      }
                    val desc =
                      when (ded) {
                        CaseDeduction.HYDRO_WARD_INVOICES ->
                          stringResource(R.string.arc2_deduction_invoices_desc)
                        CaseDeduction.MISSING_CARETAKER_ROSTER ->
                          stringResource(R.string.arc2_deduction_roster_desc)
                        CaseDeduction.MORTAR_SAMPLE_MATCH ->
                          stringResource(R.string.arc2_deduction_mortar_desc)
                      }
                    InvestigatorActionRow(
                      title = title,
                      subtitle = desc,
                      actionLabel =
                        if (owned) stringResource(R.string.jar_sealed_badge)
                        else "${stringResource(R.string.arc2_pin_action)} · ${ded.silenceCost.toInt()} SIL",
                      enabled = canAfford,
                      accent = owned,
                      testTag = "deduction_${ded.id}",
                      onClick = { actions.onPinDeduction(ded) },
                    )
                  }
                }
                InvestigatorDeckTab.EVIDENCE_BOARD -> {
                  val visibleChapters =
                    InvestigationChapter.entries.filter { it.number <= desk.completedChapters + 1 }
                  visibleChapters.forEach { chapter ->
                    val completed = desk.completedChapters >= chapter.number
                    val isNext = desk.nextChapter == chapter
                    val canConnect = isNext && desk.canConnectNextChapter
                    val chapterTitle =
                      when (chapter) {
                        InvestigationChapter.MISSING_CARETAKERS -> "THE MISSING CARETAKERS"
                        InvestigationChapter.HYDRO_WARD_CULT -> "THE HYDRO-WARD CULT"
                        InvestigationChapter.BRICK_WORK_BLUEPRINT -> "THE BRICK-WORK BLUEPRINT"
                        InvestigationChapter.FILE_ZERO_SUBJECT_INTAKE -> "FILE #00: SUBJECT INTAKE"
                      }
                    val costStr = buildString {
                      append("${nf.format(chapter.leadsCost)} LEADS")
                      if (chapter.silenceCost > 0L) append(" · ${chapter.silenceCost} SILENCE")
                    }
                    InvestigatorActionRow(
                      title = chapterTitle,
                      subtitle = if (completed) "RED STRING PINNED" else costStr,
                      actionLabel =
                        if (completed) stringResource(R.string.arc2_chapter_sealed)
                        else stringResource(R.string.arc2_chapter_connect),
                      enabled = canConnect,
                      accent = completed || isNext,
                      testTag = "chapter_connect_${chapter.number}",
                      onClick = actions.onConnectNextChapter,
                    )
                  }
                }
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
      }

      DrippingTransmissionOverlay(
        activeDialogue = state.activeDialogue,
        latestEvent = dripChannel.latestEvent,
        modifier = Modifier.fillMaxSize(),
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
internal fun InvestigatorActionRow(
  title: String,
  subtitle: String,
  actionLabel: String,
  enabled: Boolean,
  accent: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  val colorScheme = MaterialTheme.colorScheme
  Surface(
    modifier =
      Modifier.fillMaxWidth()
        .minimumInteractiveComponentSize()
        .testTag(testTag)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    shape = MaterialTheme.shapes.extraSmall,
    color =
      when {
        enabled && accent -> colorScheme.tertiaryContainer.copy(alpha = 0.72f)
        enabled -> colorScheme.surfaceVariant
        else -> colorScheme.surfaceContainerLowest
      },
    border =
      BorderStroke(
        width = 1.dp,
        color =
          when {
            enabled && accent -> colorScheme.tertiary
            enabled -> colorScheme.outline
            else -> colorScheme.outlineVariant
          },
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
          color = if (enabled) colorScheme.onBackground else colorScheme.onSurfaceVariant,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = colorScheme.onSurfaceVariant,
        )
      }
      Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = colorScheme.background.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, if (enabled) colorScheme.primary else colorScheme.outlineVariant),
      ) {
        Text(
          text = actionLabel,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
          color = if (enabled) colorScheme.primary else colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        )
      }
    }
  }
}
