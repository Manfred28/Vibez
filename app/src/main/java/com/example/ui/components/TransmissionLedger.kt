package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DialogueEntry
import com.example.ui.theme.BleakTheme

@Composable
fun TransmissionLedgerPanel(
  activeDialogue: DialogueEntry?,
  discoveredCount: Int,
  onOpenArchive: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val atmosphere = BleakTheme.atmosphere

  Surface(
    modifier = modifier.fillMaxWidth().testTag("dialogue_panel"),
    shape = MaterialTheme.shapes.small,
    color = colorScheme.surface.copy(alpha = 0.94f),
    border = BorderStroke(1.dp, atmosphere.hairline),
  ) {
    AnimatedContent(
      targetState = activeDialogue,
      transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(160)) },
      label = "dialogue_transition",
    ) { entry ->
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
        if (entry == null) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.transmission_header_idle),
              style = MaterialTheme.typography.labelMedium,
              color = colorScheme.onSurfaceVariant,
            )
            ArchivePillButton(discoveredCount = discoveredCount, onClick = onOpenArchive)
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = stringResource(R.string.transmission_idle_body),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier.testTag("dialogue_body"),
          )
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.transmission_speaker_format, entry.speaker.uppercase()),
              style = MaterialTheme.typography.labelMedium,
              color = colorScheme.primary,
              modifier = Modifier.weight(1f).testTag("dialogue_speaker"),
            )
            ArchivePillButton(discoveredCount = discoveredCount, onClick = onOpenArchive)
          }

          Spacer(modifier = Modifier.height(6.dp))
          HorizontalDivider(color = atmosphere.hairline, thickness = 1.dp)
          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = entry.lines.joinToString(separator = "\n"),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurface,
            modifier = Modifier.testTag("dialogue_body"),
          )
        }
      }
    }
  }
}

@Composable
private fun ArchivePillButton(discoveredCount: Int, onClick: () -> Unit) {
  val colorScheme = MaterialTheme.colorScheme
  val archiveCd = stringResource(R.string.transmission_open_archive_cd)
  Surface(
    modifier =
      Modifier.minimumInteractiveComponentSize()
        .testTag("open_archive_button")
        .semantics { contentDescription = archiveCd }
        .clickable(role = Role.Button, onClick = onClick),
    shape = MaterialTheme.shapes.extraSmall,
    color = colorScheme.surfaceVariant,
    border = BorderStroke(1.dp, colorScheme.outlineVariant),
  ) {
    Text(
      text = stringResource(R.string.transmission_notes_count, discoveredCount),
      style = MaterialTheme.typography.labelSmall,
      color = if (discoveredCount > 0) colorScheme.tertiary else colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FragmentArchiveSheet(
  fragments: List<DialogueEntry>,
  onDismiss: () -> Unit,
) {
  val colorScheme = MaterialTheme.colorScheme
  val atmosphere = BleakTheme.atmosphere
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = colorScheme.surface,
    contentColor = colorScheme.onSurface,
    shape = MaterialTheme.shapes.small,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 20.dp, vertical = 12.dp)
          .testTag("fragment_archive_sheet"),
    ) {
      Text(
        text = stringResource(R.string.transmission_archive_title),
        style = MaterialTheme.typography.titleMedium,
        color = colorScheme.primary,
      )
      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = atmosphere.hairline, thickness = 1.dp)
      Spacer(modifier = Modifier.height(12.dp))

      if (fragments.isEmpty()) {
        Text(
          text = stringResource(R.string.transmission_archive_empty),
          style = MaterialTheme.typography.bodyMedium,
          color = colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 20.dp),
        )
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          items(items = fragments.reversed(), key = { it.id }) { entry ->
            Surface(
              shape = MaterialTheme.shapes.extraSmall,
              color = colorScheme.surfaceContainerLowest,
              border = BorderStroke(1.dp, atmosphere.hairline),
            ) {
              Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text =
                      stringResource(R.string.transmission_speaker_format, entry.speaker.uppercase()),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.primary,
                  )
                  Text(
                    text = entry.mood.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.tertiary,
                  )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = entry.lines.joinToString("\n"),
                  style = MaterialTheme.typography.bodyMedium,
                  color = colorScheme.onSurface,
                )
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
