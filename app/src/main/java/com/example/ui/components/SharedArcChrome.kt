package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * Unified top-bar chrome shared across Arc 1 (Chamber), Arc 2 (Investigator Desk), and Arc 3
 * (Convergence Threshold). Preserves consistent audio mute and clinical dev-console pill layout.
 */
@Composable
fun ArcTopChromeBar(
  title: String,
  titleStyle: TextStyle,
  titleColor: Color,
  isAudioMuted: Boolean,
  onToggleAudioMute: () -> Unit,
  modifier: Modifier = Modifier,
  titleTestTag: String? = null,
  onOpenDevTools: (() -> Unit)? = null,
  emphasizedPills: Boolean = false,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val titleModifier =
      if (titleTestTag != null) Modifier.weight(1f).testTag(titleTestTag) else Modifier.weight(1f)
    Text(
      text = title,
      style = titleStyle,
      color = titleColor,
      modifier = titleModifier,
    )
    Row(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      AudioMuteTogglePill(
        isMuted = isAudioMuted,
        onToggle = onToggleAudioMute,
        emphasized = emphasizedPills,
      )
      if (onOpenDevTools != null) {
        DevToolsTriggerPill(onClick = onOpenDevTools, emphasized = emphasizedPills)
      }
    }
  }
}

/**
 * Shared state holder for the subordinate left-margin thematic event-drip lane across arcs.
 * Cleared automatically when a fresh reset occurs so stale drips never bleed across resets.
 */
@Stable
internal class EventDripChannelState(initialSpeakerLabel: String) {
  internal var speakerLabel: String = initialSpeakerLabel
  private var nextId by mutableLongStateOf(1L)

  var latestEvent by mutableStateOf<ThematicDripEvent?>(null)
    private set

  fun emit(eventKey: String, text: String) {
    val id = nextId++
    latestEvent =
      ThematicDripEvent(
        id = id,
        speaker = "// $speakerLabel",
        text = text,
        eventKey = eventKey,
      )
  }

  fun clear() {
    latestEvent = null
  }
}

@Composable
internal fun rememberEventDripChannel(
  speakerLabel: String,
  isFreshReset: Boolean,
): EventDripChannelState {
  val channel = remember { EventDripChannelState(speakerLabel) }
  channel.speakerLabel = speakerLabel
  LaunchedEffect(isFreshReset) {
    if (isFreshReset) {
      channel.clear()
    }
  }
  return channel
}
