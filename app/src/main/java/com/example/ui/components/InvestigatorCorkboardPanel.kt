package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.Arc2DeskState
import com.example.game.CaseExhibit
import com.example.game.CorroborationId
import com.example.game.ExhibitPair

private val CorkDark = Color(0xFF1A1512)
private val CrimsonThread = Color(0xFF9E2B2E)
private val SpeculativeThread = Color(0xFF6E4B4D)
private val BrassPin = Color(0xFFC29B56)

/**
 * Interactive placeholder Red-String Corkboard (`BOARD` tab):
 * - Tap one pinned Exhibit Polaroid to select it, then tap a second pinned Exhibit to wire/unwire
 *   a crimson cotton thread between them (up to `maxStringSpools`).
 * - Curated pairs unlock named Corroborations + story beats; non-curated pairs grant a forgiving
 *   +15% Speculative Thread bonus so players can experiment freely without softlocks.
 */
@Composable
internal fun InvestigatorCorkboardPanel(
  desk: Arc2DeskState,
  onToggleExhibitPair: (CaseExhibit, CaseExhibit) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  var selectedExhibit by remember { mutableStateOf<CaseExhibit?>(null) }

  // Clear selection if exhibit somehow no longer pinned (e.g., dev reset)
  if (selectedExhibit != null && selectedExhibit !in desk.pinnedExhibits) {
    selectedExhibit = null
  }

  val pinnedOrdered = remember(desk.pinnedExhibits) {
    CaseExhibit.entries.filter { it in desk.pinnedExhibits }
  }

  Column(
    modifier = modifier.fillMaxWidth().testTag("corkboard_panel"),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "RED STRING SPOOLS: ${desk.wiredPairs.size} / ${desk.maxStringSpools}",
        style = MaterialTheme.typography.labelMedium,
        color = colorScheme.primary,
        modifier = Modifier.testTag("corkboard_spools_text"),
      )
      Text(
        text = "CORROBORATIONS: ${desk.discoveredCorroborations.size} / ${CorroborationId.entries.size}",
        style = MaterialTheme.typography.labelSmall,
        color = colorScheme.onSurfaceVariant,
      )
    }

    Text(
      text =
        if (selectedExhibit != null) {
          "SELECTED: ${selectedExhibit!!.title.uppercase()} — TAP ANOTHER EXHIBIT TO WIRE OR UNWIRE"
        } else if (pinnedOrdered.size < 2) {
          "PIN ANOTHER EXHIBIT FROM DOSSIER (SILENCE) TO START WIRING RED STRING"
        } else {
          "TAP TWO PINNED EXHIBITS TO STRING THEM TOGETHER"
        },
      style = MaterialTheme.typography.bodySmall,
      color = if (selectedExhibit != null) colorScheme.tertiary else colorScheme.onSurfaceVariant,
      modifier = Modifier.testTag("corkboard_hint_text"),
    )

    // Visual placeholder Corkboard Canvas showing pinned nodes and live crimson threads
    Surface(
      modifier = Modifier.fillMaxWidth().height(128.dp).testTag("corkboard_string_canvas"),
      shape = MaterialTheme.shapes.extraSmall,
      color = CorkDark,
      border = BorderStroke(1.dp, colorScheme.outlineVariant),
    ) {
      Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Subtle corkboard pin grid
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        desk.wiredPairs.forEach { pair ->
          val start = Offset(size.width * pair.first.boardX, size.height * pair.first.boardY)
          val end = Offset(size.width * pair.second.boardX, size.height * pair.second.boardY)
          val isCurated = CorroborationId.matchPair(pair) != null
          drawLine(
            color = if (isCurated) CrimsonThread else SpeculativeThread,
            start = start,
            end = end,
            strokeWidth = if (isCurated) 2.5.dp.toPx() else 1.5.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = if (isCurated) null else dash,
          )
        }
        pinnedOrdered.forEach { ex ->
          val pt = Offset(size.width * ex.boardX, size.height * ex.boardY)
          val isSelected = ex == selectedExhibit
          drawCircle(
            color = if (isSelected) colorScheme.tertiary else BrassPin,
            radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
            center = pt,
          )
        }
      }
    }

    // Two-column placeholder Exhibit Polaroid cards (>= 48dp touch targets)
    pinnedOrdered.chunked(2).forEach { rowItems ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        rowItems.forEach { exhibit ->
          val isSelected = exhibit == selectedExhibit
          val wiredCount = desk.wiredPairs.count { it.first == exhibit || it.second == exhibit }
          Surface(
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("exhibit_card_${exhibit.id}")
                .clickable(role = Role.Button) {
                  val current = selectedExhibit
                  when {
                    current == null -> selectedExhibit = exhibit
                    current == exhibit -> selectedExhibit = null
                    else -> {
                      onToggleExhibitPair(current, exhibit)
                      selectedExhibit = null
                    }
                  }
                },
            shape = MaterialTheme.shapes.extraSmall,
            color =
              when {
                isSelected -> colorScheme.tertiaryContainer.copy(alpha = 0.85f)
                wiredCount > 0 -> colorScheme.surfaceVariant
                else -> colorScheme.surfaceContainerLowest
              },
            border =
              BorderStroke(
                width = if (isSelected) 1.5.dp else 1.dp,
                color =
                  when {
                    isSelected -> colorScheme.tertiary
                    wiredCount > 0 -> CrimsonThread
                    else -> colorScheme.outlineVariant
                  },
              ),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = exhibit.shortLabel,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                  color = colorScheme.primary,
                )
                if (wiredCount > 0) {
                  Text(
                    text = "WIRED ×$wiredCount",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                    color = colorScheme.tertiary,
                  )
                }
              }
              Text(
                text = exhibit.title.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                color = colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = exhibit.caption,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
        if (rowItems.size == 1) {
          Spacer(modifier = Modifier.weight(1f))
        }
      }
    }

    // Active Wired Threads & Unwire Controls
    if (desk.wiredPairs.isNotEmpty()) {
      Text(
        text = "ACTIVE THREADS",
        style = MaterialTheme.typography.labelSmall,
        color = colorScheme.onSurfaceVariant,
      )
      desk.wiredPairs.forEach { pair ->
        val corr = CorroborationId.matchPair(pair)
        val title = corr?.title?.uppercase() ?: "SPECULATIVE THREAD"
        val subtitle =
          if (corr != null) {
            "${pair.first.shortLabel} ↔ ${pair.second.shortLabel} · ${corr.effectSummary}"
          } else {
            "${pair.first.shortLabel} ↔ ${pair.second.shortLabel} · +15% Leads & +15% Silence"
          }
        WiredThreadRow(
          pair = pair,
          title = title,
          subtitle = subtitle,
          isCurated = corr != null,
          onUnwire = { onToggleExhibitPair(pair.first, pair.second) },
        )
      }
    }
  }
}

@Composable
private fun WiredThreadRow(
  pair: ExhibitPair,
  title: String,
  subtitle: String,
  isCurated: Boolean,
  onUnwire: () -> Unit,
) {
  val colorScheme = MaterialTheme.colorScheme
  Surface(
    modifier =
      Modifier.fillMaxWidth()
        .minimumInteractiveComponentSize()
        .testTag("unwire_pair_${pair.key}")
        .clickable(role = Role.Button, onClick = onUnwire),
    shape = MaterialTheme.shapes.extraSmall,
    color = if (isCurated) colorScheme.tertiaryContainer.copy(alpha = 0.55f) else colorScheme.surfaceVariant,
    border = BorderStroke(1.dp, if (isCurated) CrimsonThread else colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
          color = if (isCurated) colorScheme.onBackground else colorScheme.onSurfaceVariant,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
          color = colorScheme.onSurfaceVariant,
        )
      }
      Box(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(
          text = "UNWIRE",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
          color = colorScheme.primary,
        )
      }
    }
  }
}
