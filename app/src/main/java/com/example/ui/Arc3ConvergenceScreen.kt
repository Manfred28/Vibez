package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.ExcisionMilestone
import com.example.game.GameArc
import com.example.game.OrganGameState
import com.example.ui.components.ArcTopChromeBar
import com.example.ui.components.FragmentArchiveSheet
import com.example.ui.components.PinnedLedgerBottomBar

/** Clean, open Arc 3 boundary screen preserving both Arc 1 and Arc 2 ledgers without dead ends. */
@Composable
fun Arc3ConvergenceScreen(
  state: OrganGameState,
  onSwitchArc: (GameArc) -> Unit,
  onOpenDevTools: () -> Unit,
  isAudioMuted: Boolean = false,
  onToggleAudioMute: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  var archiveVisible by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier.fillMaxSize().background(colorScheme.background).testTag("arc3_screen"),
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
      Column(
        modifier =
          Modifier.fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
      Surface(
        modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
        shape = MaterialTheme.shapes.small,
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.tertiary),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          ArcTopChromeBar(
            title = stringResource(R.string.arc3_title),
            titleStyle = MaterialTheme.typography.titleMedium,
            titleColor = colorScheme.tertiary,
            isAudioMuted = isAudioMuted,
            onToggleAudioMute = onToggleAudioMute,
            onOpenDevTools = null,
            emphasizedPills = true,
          )
          Text(
            text = stringResource(R.string.arc3_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
          )
          val lastExcisionName = ExcisionMilestone.byNumber(state.highestExcisionMilestone)?.code
          val convergenceSummary =
            buildList {
                add("SCAR TISSUE: ${state.scarTissue}")
                if (lastExcisionName != null) add(lastExcisionName)
                add("DOSSIER: ${state.unredactedDossierLines.size}/4")
                add("RECOVERED NOTES: ${state.discoveredDialogues.size}")
              }
              .joinToString(" · ")
          Text(
            text = convergenceSummary,
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.primary,
          )
          InvestigatorActionRow(
            title = stringResource(R.string.arc3_return_chamber),
            subtitle = "Step back to the tiled room with all scars intact.",
            actionLabel = "CHAMBER",
            enabled = true,
            accent = false,
            testTag = "arc3_to_arc1_button",
            onClick = { onSwitchArc(GameArc.ARC_1_CHAMBER) },
          )
          InvestigatorActionRow(
            title = stringResource(R.string.arc3_return_desk),
            subtitle = "Step back to the rain-streaked desk and File #00 pinboard.",
            actionLabel = "DESK",
            enabled = true,
            accent = true,
            testTag = "arc3_to_arc2_button",
            onClick = { onSwitchArc(GameArc.ARC_2_INVESTIGATION) },
          )
          InvestigatorActionRow(
            title = stringResource(R.string.dev_modal_title),
            subtitle = "Inspect or modify any milestone, arc, or resource.",
            actionLabel = stringResource(R.string.dev_button_label),
            enabled = true,
            accent = false,
            testTag = "open_dev_tools_button",
            onClick = onOpenDevTools,
          )
        }
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
