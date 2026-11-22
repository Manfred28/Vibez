package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.ActiveDeskAnomaly
import com.example.game.DeskAnomalyType
import com.example.game.OrganGameState
import com.example.ui.theme.BleakTheme
import java.text.NumberFormat
import kotlin.math.ceil

/**
 * Upper arena for Arc 2 ("The Inquiry"): a rain-streaked Investigator Case Desk subtly haunted
 * by the Arc 1 chamber. As Complicity rises, wet tile grout lines bleed through the leather
 * blotter, the tungsten desk lamp drips downward condensation during Silence, and the window
 * reflection ghosts faint Arc 1 Scar Tissue numbers and claw-marks.
 */
@Composable
fun InvestigatorDeskCanvas(
  state: OrganGameState,
  breathPhaseProvider: () -> Float,
  dropPhaseProvider: () -> Float,
  onInspectCaseFile: () -> Unit,
  onInteractDeskAnomaly: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val atmosphere = BleakTheme.atmosphere
  val desk = state.arc2
  val nf = remember { NumberFormat.getIntegerInstance() }
  val formattedLeads = remember(desk.leadsDisplay) { nf.format(desk.leadsDisplay) }
  val formattedTap = remember(desk.inspectYield) { formatCompactDecimal(desk.inspectYield) }
  val inspectCd = stringResource(R.string.arc2_inspect_cd)

  val scratchPath = remember { Path() }

  Surface(
    modifier = modifier.fillMaxWidth().testTag("investigator_desk_canvas"),
    shape = MaterialTheme.shapes.small,
    color = colorScheme.surface.copy(alpha = 0.92f),
    border = BorderStroke(1.dp, atmosphere.hairline),
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val boxW = maxWidth
      val boxH = maxHeight

      // Haunted Desk Blotter + Tungsten Lamp + Arc 1 Bleed Canvas
      Canvas(modifier = Modifier.fillMaxSize().testTag("haunted_desk_backdrop")) {
        val breath = breathPhaseProvider()
        val drop = dropPhaseProvider()
        val complicity = desk.complicity.coerceIn(0f, 1f)

        // Base dark desk blotter
        drawRect(color = colorScheme.background)

        // Warm-ash tungsten desk lamp cone from top center
        val lampSway = (breath - 0.5f) * 14.dp.toPx()
        val lampOrigin = Offset(size.width * 0.5f + lampSway, 0f)
        drawCircle(
          brush =
            Brush.radialGradient(
              colors =
                listOf(
                  colorScheme.primary.copy(alpha = 0.22f + breath * 0.05f),
                  Color.Transparent,
                ),
              center = Offset(lampOrigin.x, size.height * 0.42f),
              radius = size.minDimension * 0.72f,
            ),
          radius = size.minDimension * 0.72f,
          center = Offset(lampOrigin.x, size.height * 0.42f),
        )

        // Rain streaks down the upper windowpane band
        val rainColor = colorScheme.onSurfaceVariant.copy(alpha = 0.14f)
        for (i in 1..9) {
          val rx = size.width * (i / 10f)
          val startY = ((drop * size.height * 0.45f) + (i * 23f)) % (size.height * 0.40f)
          drawLine(
            color = rainColor,
            start = Offset(rx, startY),
            end = Offset(rx - 2.dp.toPx(), startY + 14.dp.toPx()),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round,
          )
        }

        // HAUNTING BLEED #1: Faint Arc 1 damp tile grout lines bleeding through the desk blotter
        val groutAlpha = (0.04f + complicity * 0.22f + if (desk.isSilenceActive) 0.08f else 0f)
        val groutColor = colorScheme.secondary.copy(alpha = groutAlpha.coerceAtMost(0.36f))
        val step = 36.dp.toPx()
        var gx = step
        while (gx < size.width) {
          drawLine(groutColor, Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
          gx += step
        }
        var gy = step
        while (gy < size.height) {
          drawLine(groutColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
          gy += step
        }

        // Desk lamp shade silhouette at top center
        scratchPath.rewind()
        scratchPath.moveTo(lampOrigin.x - 24.dp.toPx(), 16.dp.toPx())
        scratchPath.lineTo(lampOrigin.x + 24.dp.toPx(), 16.dp.toPx())
        scratchPath.lineTo(lampOrigin.x + 14.dp.toPx(), 4.dp.toPx())
        scratchPath.lineTo(lampOrigin.x - 14.dp.toPx(), 4.dp.toPx())
        scratchPath.close()
        drawPath(scratchPath, colorScheme.surfaceVariant)
        drawPath(scratchPath, colorScheme.outline, style = Stroke(1.2.dp.toPx()))

        // HAUNTING BLEED #2: Desk lamp weeps a single downward grey condensation drop in Silence
        if (desk.isSilenceActive || complicity >= 0.45f) {
          val dropY = 18.dp.toPx() + drop * (size.height * 0.46f)
          val dropAlpha = (1f - drop) * 0.70f
          drawCircle(
            color = colorScheme.secondary.copy(alpha = dropAlpha),
            radius = 2.4.dp.toPx(),
            center = Offset(lampOrigin.x, dropY),
          )
        }

        // HAUNTING BLEED #3: Corner glass reflection showing Suture-Hand scratches at high complicity
        if (complicity >= 0.30f) {
          val scratchAlpha = ((complicity - 0.25f) * 0.48f).coerceIn(0.05f, 0.42f)
          val scratchColor = colorScheme.tertiary.copy(alpha = scratchAlpha)
          for (s in 0..3) {
            val sx = size.width * 0.80f + s * 6.dp.toPx()
            drawLine(
              color = scratchColor,
              start = Offset(sx, 14.dp.toPx()),
              end = Offset(sx - 5.dp.toPx(), 42.dp.toPx() + (s % 2) * 8.dp.toPx()),
              strokeWidth = 1.2.dp.toPx(),
              cap = StrokeCap.Round,
            )
          }
        }

        // File #00 blotter peel-back diagonal fold once revealed
        if (desk.fileZeroRevealed) {
          scratchPath.rewind()
          scratchPath.moveTo(0f, size.height)
          scratchPath.lineTo(size.width * 0.28f, size.height)
          scratchPath.lineTo(0f, size.height * 0.68f)
          scratchPath.close()
          drawPath(scratchPath, colorScheme.tertiaryContainer.copy(alpha = 0.72f))
          drawPath(scratchPath, colorScheme.tertiary, style = Stroke(1.4.dp.toPx()))
        }
      }

      // Subtle haunted reflection whisper referencing real Arc 1 Scar Tissue
      if (desk.complicity >= 0.35f) {
        Text(
          text = "SCAR ECHO // ${nf.format(state.scarTissue)}",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, letterSpacing = 1.sp),
          color = colorScheme.tertiary.copy(alpha = (desk.complicity * 0.55f).coerceIn(0.15f, 0.55f)),
          modifier =
            Modifier.align(Alignment.TopEnd)
              .padding(top = 8.dp, end = 10.dp)
              .testTag("arc1_haunting_echo"),
        )
      }

      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.arc2_leads_label),
              style = MaterialTheme.typography.labelLarge,
              color = colorScheme.onSurfaceVariant,
            )
            if (desk.leadsPerSecond > 0.0) {
              Text(
                text = "+${formatCompactDecimal(desk.leadsPerSecond)} /s",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.primary,
                modifier = Modifier.testTag("arc2_leads_rate_text"),
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = colorScheme.background.copy(alpha = 0.74f),
            border = BorderStroke(1.dp, colorScheme.outline),
          ) {
            Text(
              text = formattedLeads,
              style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp),
              color = colorScheme.onBackground,
              maxLines = 1,
              modifier =
                Modifier.padding(horizontal = 28.dp, vertical = 6.dp).testTag("arc2_leads_counter"),
            )
          }

          Spacer(modifier = Modifier.height(4.dp))

          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (desk.isSilenceActive) {
              Text(
                text = stringResource(R.string.arc2_silence_badge),
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.secondary,
                modifier = Modifier.testTag("arc2_silence_badge"),
              )
            }
            if (desk.focusRemainingSeconds > 0.0) {
              Text(
                text =
                  stringResource(
                    R.string.arc2_focus_badge,
                    ceil(desk.focusRemainingSeconds).toInt(),
                  ),
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.primary,
                modifier = Modifier.testTag("arc2_focus_badge"),
              )
            }
          }
        }

        // Tactile Case File / Typewriter Stamp centerpiece (>= 48dp touch target)
        Surface(
          modifier =
            Modifier.size(width = 168.dp, height = 84.dp)
              .minimumInteractiveComponentSize()
              .testTag("inspect_case_button")
              .semantics { contentDescription = inspectCd }
              .clickable(role = Role.Button, onClick = onInspectCaseFile),
          shape = MaterialTheme.shapes.small,
          color = colorScheme.surfaceVariant.copy(alpha = 0.94f),
          border = BorderStroke(1.5.dp, colorScheme.primary.copy(alpha = 0.78f)),
        ) {
          Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Text(
              text = stringResource(R.string.arc2_inspect_label),
              style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.4.sp),
              color = colorScheme.onBackground,
            )
            Text(
              text = "+$formattedTap LEADS",
              style = MaterialTheme.typography.labelMedium,
              color = colorScheme.primary,
            )
          }
        }
      }

      // Interactive Desk Anomalies
      desk.activeDeskAnomalies.forEach { anomaly ->
        key(anomaly.id) {
          val offsetX = (boxW - 76.dp) * anomaly.xFraction
          val offsetY = (boxH - 52.dp) * anomaly.yFraction
          DeskAnomalyChip(
            anomaly = anomaly,
            onClick = { onInteractDeskAnomaly(anomaly.id) },
            modifier = Modifier.offset(x = offsetX, y = offsetY),
          )
        }
      }
    }
  }
}

@Composable
private fun DeskAnomalyChip(
  anomaly: ActiveDeskAnomaly,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val label =
    when (anomaly.type) {
      DeskAnomalyType.EVIDENCE_ENVELOPE -> "UNSEAL"
      DeskAnomalyType.FOGGED_SPECTACLES -> "WIPE"
      DeskAnomalyType.RED_STRING -> "UNTANGLE"
      DeskAnomalyType.REEL_TAPE -> "PLAY"
    }
  Surface(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .testTag("desk_anomaly_${anomaly.type.id}_${anomaly.id}")
        .clickable(role = Role.Button, onClick = onClick),
    shape = MaterialTheme.shapes.extraSmall,
    color = colorScheme.tertiaryContainer.copy(alpha = 0.92f),
    border = BorderStroke(1.2.dp, colorScheme.primary),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
      color = colorScheme.onBackground,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
    )
  }
}
