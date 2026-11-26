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
 * Placeholder-art interactive Investigator Desk Arena for Arc 2 ("The Inquiry").
 * Features the brass Lamp On / Lamp Off pull-chain toggle and strictly Slow-Boil-gated visuals:
 * - Cases I–III (`hauntDepth < 0.60`): pure cold rain, tungsten cone vs dark slate, inside mist,
 *   and faint second-chair shadow (Case II+); zero wet grout or Room bleed.
 * - Case IV (`0.60..0.82`): Lamp Off subtly exposes faint wet tile grout beneath the blotter.
 * - Case V (`0.85..1.00`): persistent tile grout even under Lamp On, grey oil weep from the lamp,
 *   corner reflection scratches, and File #00 blotter peel-back.
 */
@Composable
fun InvestigatorDeskCanvas(
  state: OrganGameState,
  breathPhaseProvider: () -> Float,
  dropPhaseProvider: () -> Float,
  onToggleLamp: () -> Unit,
  onInspectCaseFile: () -> Unit,
  onInteractDeskAnomaly: (Long) -> Unit,
  onEmitThematicDrip: (eventKey: String, text: String) -> Unit = { _, _ -> },
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val atmosphere = BleakTheme.atmosphere
  val desk = state.arc2
  val nf = remember { NumberFormat.getIntegerInstance() }
  val formattedLeads = remember(desk.leadsDisplay) { nf.format(desk.leadsDisplay) }
  val formattedSilence = remember(desk.silenceDisplay) { nf.format(desk.silenceDisplay) }
  val formattedTap = remember(desk.inspectYield) { formatCompactDecimal(desk.inspectYield) }
  val inspectCd = stringResource(R.string.arc2_inspect_cd)
  val lampToggleCd = stringResource(R.string.arc2_lamp_toggle_cd)
  val scratchPath = remember { Path() }

  Surface(
    modifier = modifier.fillMaxWidth().testTag("investigator_desk_canvas"),
    shape = MaterialTheme.shapes.small,
    color = colorScheme.surface.copy(alpha = 0.94f),
    border =
      BorderStroke(
        width = 1.dp,
        color = if (desk.lampOn) atmosphere.hairline else colorScheme.secondary.copy(alpha = 0.55f),
      ),
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val boxW = maxWidth
      val boxH = maxHeight

      Canvas(modifier = Modifier.fillMaxSize().testTag("haunted_desk_backdrop")) {
        val breath = breathPhaseProvider()
        val drop = dropPhaseProvider()
        val haunt = desk.hauntDepth.coerceIn(0f, 1f)
        val lampOn = desk.lampOn

        // 1. Base dark nocturnal office slate (deeper shadow when Lamp Off)
        drawRect(color = if (lampOn) colorScheme.background else Color(0xFF06080B))

        // 2. Overhead tungsten lamp cone (bright warm pool when On; cold moonlit slate glow when Off)
        val lampSway = (breath - 0.5f) * 10.dp.toPx()
        val lampOrigin = Offset(size.width * 0.5f + lampSway, 0f)
        if (lampOn) {
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    colorScheme.primary.copy(alpha = 0.28f + breath * 0.04f),
                    colorScheme.primary.copy(alpha = 0.06f),
                    Color.Transparent,
                  ),
                center = Offset(lampOrigin.x, size.height * 0.44f),
                radius = size.minDimension * 0.78f,
              ),
            radius = size.minDimension * 0.78f,
            center = Offset(lampOrigin.x, size.height * 0.44f),
          )
        } else {
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    colorScheme.secondary.copy(alpha = 0.14f + breath * 0.04f),
                    Color.Transparent,
                  ),
                center = Offset(size.width * 0.18f, size.height * 0.22f),
                radius = size.minDimension * 0.85f,
              ),
            radius = size.minDimension * 0.85f,
            center = Offset(size.width * 0.18f, size.height * 0.22f),
          )
        }

        // 3. Cold rain streaks across dark precinct glass
        val rainAlpha = if (lampOn) 0.12f else 0.24f
        val rainColor = colorScheme.onSurfaceVariant.copy(alpha = rainAlpha)
        for (i in 1..11) {
          val rx = size.width * (i / 12f)
          val startY = ((drop * size.height * 0.55f) + (i * 21f)) % (size.height * 0.46f)
          drawLine(
            color = rainColor,
            start = Offset(rx, startY),
            end = Offset(rx - 2.5.dp.toPx(), startY + 15.dp.toPx()),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round,
          )
        }

        // 4. Case II+ Eerie Motif (non-grotesque): faint second-chair shadow across desk when Lamp Off
        if (!lampOn && desk.caseAngle in 2..4) {
          val chairAlpha = 0.09f + breath * 0.05f
          val chairColor = colorScheme.onSurfaceVariant.copy(alpha = chairAlpha)
          val cx = size.width * 0.82f
          val cy = size.height * 0.66f
          drawLine(chairColor, Offset(cx - 18.dp.toPx(), cy - 28.dp.toPx()), Offset(cx - 18.dp.toPx(), cy + 20.dp.toPx()), strokeWidth = 2.dp.toPx())
          drawLine(chairColor, Offset(cx + 14.dp.toPx(), cy - 26.dp.toPx()), Offset(cx + 14.dp.toPx(), cy + 20.dp.toPx()), strokeWidth = 2.dp.toPx())
          drawLine(chairColor, Offset(cx - 18.dp.toPx(), cy - 22.dp.toPx()), Offset(cx + 14.dp.toPx(), cy - 20.dp.toPx()), strokeWidth = 2.dp.toPx())
          drawLine(chairColor, Offset(cx - 20.dp.toPx(), cy), Offset(cx + 16.dp.toPx(), cy + 2.dp.toPx()), strokeWidth = 2.dp.toPx())
        }

        // 5. SLOW-BOIL ROOM BLEED (STRICTLY QUARANTINED UNTIL CASE IV+):
        // Case IV: faint wet tile grout only shows while Lamp Off.
        // Case V / File #00: persistent tile grout even when Lamp On!
        val showGrout = (desk.caseAngle == 4 && !lampOn) || desk.caseAngle >= 5 || desk.fileZeroRevealed
        if (showGrout) {
          val bleedStrength = ((haunt - 0.55f) / 0.45f).coerceIn(0.15f, 1f)
          val groutAlpha = (bleedStrength * if (lampOn) 0.22f else 0.34f).coerceIn(0.06f, 0.36f)
          val groutColor = colorScheme.secondary.copy(alpha = groutAlpha)
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
        }

        // 6. Suspended brass desk lamp hood silhouette
        scratchPath.rewind()
        scratchPath.moveTo(lampOrigin.x - 24.dp.toPx(), 16.dp.toPx())
        scratchPath.lineTo(lampOrigin.x + 24.dp.toPx(), 16.dp.toPx())
        scratchPath.lineTo(lampOrigin.x + 14.dp.toPx(), 4.dp.toPx())
        scratchPath.lineTo(lampOrigin.x - 14.dp.toPx(), 4.dp.toPx())
        scratchPath.close()
        drawPath(scratchPath, colorScheme.surfaceVariant)
        drawPath(
          scratchPath,
          if (lampOn) colorScheme.primary else colorScheme.outline,
          style = Stroke(1.2.dp.toPx()),
        )

        // 7. Case V+ Grotesque Intrusion: grey oil drop weeping from lamp hood + glass scratches
        if (desk.caseAngle >= 5 || desk.fileZeroRevealed) {
          val dropY = 18.dp.toPx() + drop * (size.height * 0.44f)
          val dropAlpha = (1f - drop) * 0.72f
          drawCircle(
            color = colorScheme.secondary.copy(alpha = dropAlpha),
            radius = 2.4.dp.toPx(),
            center = Offset(lampOrigin.x, dropY),
          )
          val scratchColor = colorScheme.tertiary.copy(alpha = 0.28f + breath * 0.14f)
          for (s in 0..3) {
            val sx = size.width * 0.80f + s * 6.dp.toPx()
            drawLine(
              color = scratchColor,
              start = Offset(sx, 14.dp.toPx()),
              end = Offset(sx - 5.dp.toPx(), 40.dp.toPx() + (s % 2) * 8.dp.toPx()),
              strokeWidth = 1.2.dp.toPx(),
              cap = StrokeCap.Round,
            )
          }
        }

        // 8. File #00 blotter peel-back diagonal fold
        if (desk.fileZeroRevealed) {
          scratchPath.rewind()
          scratchPath.moveTo(0f, size.height)
          scratchPath.lineTo(size.width * 0.28f, size.height)
          scratchPath.lineTo(0f, size.height * 0.68f)
          scratchPath.close()
          drawPath(scratchPath, colorScheme.tertiaryContainer.copy(alpha = 0.74f))
          drawPath(scratchPath, colorScheme.tertiary, style = Stroke(1.4.dp.toPx()))
        }
      }

      // Interactive Brass Lamp Pull-Chain Toggle (>= 48dp touch target, top-right)
      Surface(
        modifier =
          Modifier.align(Alignment.TopEnd)
            .padding(top = 8.dp, end = 10.dp)
            .minimumInteractiveComponentSize()
            .testTag("lamp_toggle_button")
            .semantics { contentDescription = lampToggleCd }
            .clickable(role = Role.Switch, onClick = onToggleLamp),
        shape = MaterialTheme.shapes.extraSmall,
        color =
          if (desk.lampOn) colorScheme.primaryContainer.copy(alpha = 0.88f)
          else colorScheme.surfaceVariant.copy(alpha = 0.92f),
        border =
          BorderStroke(
            width = 1.2.dp,
            color = if (desk.lampOn) colorScheme.primary else colorScheme.secondary,
          ),
      ) {
        Text(
          text =
            if (desk.lampOn) stringResource(R.string.arc2_lamp_on_label)
            else stringResource(R.string.arc2_lamp_off_label),
          style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
          color = if (desk.lampOn) colorScheme.primary else colorScheme.secondary,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        )
      }

      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Spacer(modifier = Modifier.height(6.dp))
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
                color = if (desk.lampOn) colorScheme.primary else colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("arc2_leads_rate_text"),
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = colorScheme.background.copy(alpha = 0.78f),
            border = BorderStroke(1.dp, colorScheme.outline),
          ) {
            Text(
              text = formattedLeads,
              style = MaterialTheme.typography.displayLarge.copy(fontSize = 42.sp),
              color = colorScheme.onBackground,
              maxLines = 1,
              modifier =
                Modifier.padding(horizontal = 26.dp, vertical = 5.dp).testTag("arc2_leads_counter"),
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            val silRateStr =
              if (desk.silencePerSecond > 0.0) {
                " (${stringResource(R.string.arc2_silence_rate_format, formatCompactDecimal(desk.silencePerSecond))})"
              } else {
                ""
              }
            Text(
              text = "${stringResource(R.string.arc2_silence_readout, formattedSilence)}$silRateStr",
              style = MaterialTheme.typography.labelSmall,
              color = if (!desk.lampOn) colorScheme.secondary else colorScheme.onSurfaceVariant,
              modifier = Modifier.testTag("arc2_silence_counter"),
            )
            if (!desk.lampOn) {
              Text(
                text = stringResource(R.string.arc2_silence_badge),
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.secondary,
                modifier = Modifier.testTag("arc2_silence_badge"),
              )
            }
          }

          if (desk.isReelScrubbing || desk.focusRemainingSeconds > 0.0) {
            Spacer(modifier = Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              if (desk.isReelScrubbing) {
                Text(
                  text =
                    stringResource(
                      R.string.arc2_scrub_badge,
                      ceil(desk.reelScrubRemainingSeconds).toInt(),
                    ),
                  style = MaterialTheme.typography.labelSmall,
                  color = colorScheme.tertiary,
                  modifier = Modifier.testTag("arc2_scrub_badge"),
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
        }

        // Tactile Case File Stamp centerpiece (>= 48dp touch target)
        Surface(
          modifier =
            Modifier.size(width = 176.dp, height = 82.dp)
              .minimumInteractiveComponentSize()
              .testTag("inspect_case_button")
              .semantics { contentDescription = inspectCd }
              .clickable(role = Role.Button, onClick = onInspectCaseFile),
          shape = MaterialTheme.shapes.small,
          color = colorScheme.surfaceVariant.copy(alpha = if (desk.lampOn) 0.94f else 0.72f),
          border =
            BorderStroke(
              width = 1.5.dp,
              color =
                if (desk.lampOn) colorScheme.primary.copy(alpha = 0.80f)
                else colorScheme.outline,
            ),
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
              color = if (desk.lampOn) colorScheme.primary else colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Interactive Desk Anomalies (strictly gated by Case Angle & Lamp posture)
      val envelopeDrip = stringResource(R.string.drip_desk_anomaly_envelope)
      val spectaclesDrip = stringResource(R.string.drip_desk_anomaly_spectacles)
      val tapeDrip = stringResource(R.string.drip_desk_anomaly_tape)
      val enamelDrip = stringResource(R.string.drip_desk_anomaly_enamel)

      desk.activeDeskAnomalies.forEach { anomaly ->
        key(anomaly.id) {
          val offsetX = (boxW - 84.dp) * anomaly.xFraction
          val offsetY = (boxH - 52.dp) * anomaly.yFraction
          val dripLine =
            when (anomaly.type) {
              DeskAnomalyType.EVIDENCE_ENVELOPE -> envelopeDrip
              DeskAnomalyType.FOGGED_SPECTACLES -> spectaclesDrip
              DeskAnomalyType.REEL_TAPE -> tapeDrip
              DeskAnomalyType.ENAMEL_EXHIBIT -> enamelDrip
            }
          DeskAnomalyChip(
            anomaly = anomaly,
            onClick = {
              onInteractDeskAnomaly(anomaly.id)
              onEmitThematicDrip("desk_${anomaly.type.id}", dripLine)
            },
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
      DeskAnomalyType.REEL_TAPE -> "PLAY"
      DeskAnomalyType.ENAMEL_EXHIBIT -> "CATALOG"
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
