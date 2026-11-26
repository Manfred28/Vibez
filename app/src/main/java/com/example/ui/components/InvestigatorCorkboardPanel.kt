package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
private val CorkMid = Color(0xFF231C17)
private val CrimsonThread = Color(0xFFA82D30)
private val SpeculativeThread = Color(0xFF836869)
private val BrassPin = Color(0xFFC8A058)
private val BrassPinHighlight = Color(0xFFE7CA8C)

/**
 * Interactive Photographic Red-String Corkboard (`BOARD` tab):
 * - Displays live crimson cotton threads & brass pushpins across a dark cork canvas.
 * - Renders each pinned Exhibit as a tactile evidence Polaroid with its bespoke macro photograph,
 *   brass pin head, wire badge, and accessible tap-to-wire controls.
 */
@Composable
internal fun InvestigatorCorkboardPanel(
  desk: Arc2DeskState,
  onToggleExhibitPair: (CaseExhibit, CaseExhibit) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  var selectedExhibit by remember { mutableStateOf<CaseExhibit?>(null) }

  if (selectedExhibit != null && selectedExhibit !in desk.pinnedExhibits) {
    selectedExhibit = null
  }

  val pinnedOrdered = remember(desk.pinnedExhibits) {
    CaseExhibit.entries.filter { it in desk.pinnedExhibits }
  }
  val threadPath = remember { Path() }

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

    // Atmospheric Dark Corkboard Thread Map with draped cotton strings and brass pushpins
    Surface(
      modifier = Modifier.fillMaxWidth().height(132.dp).testTag("corkboard_string_canvas"),
      shape = MaterialTheme.shapes.extraSmall,
      color = CorkDark,
      border = BorderStroke(1.dp, colorScheme.outlineVariant),
    ) {
      Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        drawRect(
          brush =
            Brush.radialGradient(
              colors = listOf(CorkMid.copy(alpha = 0.85f), CorkDark),
              center = Offset(size.width * 0.5f, size.height * 0.45f),
              radius = size.maxDimension * 0.72f,
            )
        )
        // Subtle cork grain speckles
        for (g in 0..24) {
          val gx = ((g * 43 + 17) % 100) / 100f * size.width
          val gy = ((g * 67 + 11) % 100) / 100f * size.height
          drawCircle(
            color = Color(0xFF0E0B09).copy(alpha = 0.45f),
            radius = 1.1.dp.toPx(),
            center = Offset(gx, gy),
          )
        }

        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        desk.wiredPairs.forEach { pair ->
          val start = Offset(size.width * pair.first.boardX, size.height * pair.first.boardY)
          val end = Offset(size.width * pair.second.boardX, size.height * pair.second.boardY)
          val sagY = maxOf(start.y, end.y) + 12.dp.toPx()
          val mid = Offset((start.x + end.x) * 0.5f, sagY.coerceAtMost(size.height - 4.dp.toPx()))
          val isCurated = CorroborationId.matchPair(pair) != null

          threadPath.rewind()
          threadPath.moveTo(start.x, start.y)
          threadPath.quadraticTo(mid.x, mid.y, end.x, end.y)

          // Drop shadow under taut cotton thread
          drawPath(
            path = threadPath,
            color = Color.Black.copy(alpha = 0.48f),
            style = Stroke(width = if (isCurated) 3.4.dp.toPx() else 2.2.dp.toPx(), cap = StrokeCap.Round),
          )
          drawPath(
            path = threadPath,
            color = if (isCurated) CrimsonThread else SpeculativeThread,
            style =
              Stroke(
                width = if (isCurated) 2.2.dp.toPx() else 1.4.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = if (isCurated) null else dash,
              ),
          )
        }

        pinnedOrdered.forEach { ex ->
          val pt = Offset(size.width * ex.boardX, size.height * ex.boardY)
          val isSelected = ex == selectedExhibit
          if (isSelected) {
            drawCircle(
              color = BrassPinHighlight.copy(alpha = 0.30f),
              radius = 11.dp.toPx(),
              center = pt,
            )
          }
          drawCircle(color = Color.Black.copy(alpha = 0.7f), radius = 5.2.dp.toPx(), center = pt + Offset(1.dp.toPx(), 1.5.dp.toPx()))
          drawCircle(
            color = if (isSelected) colorScheme.tertiary else BrassPin,
            radius = if (isSelected) 5.5.dp.toPx() else 4.2.dp.toPx(),
            center = pt,
          )
          drawCircle(
            color = BrassPinHighlight.copy(alpha = 0.78f),
            radius = 1.4.dp.toPx(),
            center = pt - Offset(1.dp.toPx(), 1.dp.toPx()),
          )
        }
      }
    }

    // Two-column Photographic Evidence Polaroid Cards (>= 48dp touch targets)
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
                isSelected -> Color(0xFF281F15).copy(alpha = 0.94f)
                wiredCount > 0 -> Color(0xFF191D22)
                else -> Color(0xFF13171B)
              },
            border =
              BorderStroke(
                width = if (isSelected) 1.5.dp else 1.dp,
                color =
                  when {
                    isSelected -> BrassPinHighlight
                    wiredCount > 0 -> CrimsonThread
                    else -> colorScheme.outlineVariant
                  },
              ),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Box(contentAlignment = Alignment.TopCenter) {
                  ExhibitPolaroidThumb(
                    exhibit = exhibit,
                    pinned = true,
                    wired = wiredCount > 0,
                    selected = isSelected,
                    size = 48.dp,
                  )
                  // Brass pushpin head seated along top edge of Polaroid
                  Box(
                    modifier =
                      Modifier.padding(top = 2.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colorScheme.tertiary else BrassPin)
                  )
                }
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = exhibit.shortLabel,
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                      color = colorScheme.primary,
                    )
                    if (wiredCount > 0) {
                      Text(
                        text = "×$wiredCount",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = CrimsonThread,
                      )
                    }
                  }
                  Text(
                    text = exhibit.title.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.5.sp),
                    color = colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
              Text(
                text = exhibit.caption,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp, lineHeight = 13.sp),
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
