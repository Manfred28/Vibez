package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.DeskAnomalyType
import com.example.game.OrganGameState
import com.example.ui.theme.BleakTheme
import java.text.NumberFormat
import kotlin.math.ceil

/**
 * Photographic interactive Investigator Desk Arena for Arc 2 ("The Inquiry").
 * Layers 5 progressive nocturnal desk photographs (`img_desk_case1..5`) with strict Slow-Boil
 * Quarantine, tactile Brass Lamp On / Lamp Off pull-chain lighting grades, animated rain streaks,
 * Case IV–V Room bleed overlays, and borderless circular radially feathered Desk Anomalies.
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

  val deskLayers =
    remember(desk.caseAngle, desk.hauntDepth, desk.lampOn, desk.fileZeroRevealed) {
      computeDeskLayerAlphas(
        caseAngle = desk.caseAngle,
        hauntDepth = desk.hauntDepth,
        lampOn = desk.lampOn,
        fileZeroRevealed = desk.fileZeroRevealed,
      )
    }

  val lampOffPhotoFilter =
    remember {
      ColorFilter.colorMatrix(
        ColorMatrix(
          floatArrayOf(
            0.54f, 0f, 0f, 0f, -10f,
            0f, 0.62f, 0f, 0f, -6f,
            0f, 0f, 0.78f, 0f, 4f,
            0f, 0f, 0f, 1f, 0f,
          )
        )
      )
    }
  val lampOnPhotoFilter =
    remember {
      ColorFilter.colorMatrix(
        ColorMatrix(
          floatArrayOf(
            0.96f, 0f, 0f, 0f, 4f,
            0f, 0.90f, 0f, 0f, 0f,
            0f, 0f, 0.80f, 0f, -6f,
            0f, 0f, 0f, 1f, 0f,
          )
        )
      )
    }
  val activePhotoFilter = if (desk.lampOn) lampOnPhotoFilter else lampOffPhotoFilter

  Surface(
    modifier = modifier.fillMaxWidth().testTag("investigator_desk_canvas"),
    shape = MaterialTheme.shapes.small,
    color = colorScheme.surface.copy(alpha = 0.96f),
    border =
      BorderStroke(
        width = 1.dp,
        color = if (desk.lampOn) atmosphere.hairline else colorScheme.secondary.copy(alpha = 0.58f),
      ),
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val boxW = maxWidth
      val boxH = maxHeight

      // 5-stage progressive photographic nocturnal desk backdrop (strictly Slow-Boil quarantined)
      Box(modifier = Modifier.fillMaxSize()) {
        if (deskLayers.case1 > 0f) {
          Image(
            painter = painterResource(id = deskStageDrawableRes(1)),
            contentDescription = stringResource(R.string.art_desk_case1_cd),
            contentScale = ContentScale.Crop,
            colorFilter = activePhotoFilter,
            modifier = Modifier.fillMaxSize().alpha(deskLayers.case1).testTag("desk_stage_photo_1"),
          )
        }
        if (deskLayers.case2 > 0f) {
          Image(
            painter = painterResource(id = deskStageDrawableRes(2)),
            contentDescription = stringResource(R.string.art_desk_case2_cd),
            contentScale = ContentScale.Crop,
            colorFilter = activePhotoFilter,
            modifier = Modifier.fillMaxSize().alpha(deskLayers.case2).testTag("desk_stage_photo_2"),
          )
        }
        if (deskLayers.case3 > 0f) {
          Image(
            painter = painterResource(id = deskStageDrawableRes(3)),
            contentDescription = stringResource(R.string.art_desk_case3_cd),
            contentScale = ContentScale.Crop,
            colorFilter = activePhotoFilter,
            modifier = Modifier.fillMaxSize().alpha(deskLayers.case3).testTag("desk_stage_photo_3"),
          )
        }
        if (deskLayers.case4 > 0f) {
          Image(
            painter = painterResource(id = deskStageDrawableRes(4)),
            contentDescription = stringResource(R.string.art_desk_case4_cd),
            contentScale = ContentScale.Crop,
            colorFilter = activePhotoFilter,
            modifier = Modifier.fillMaxSize().alpha(deskLayers.case4).testTag("desk_stage_photo_4"),
          )
        }
        if (deskLayers.case5 > 0f) {
          Image(
            painter = painterResource(id = deskStageDrawableRes(5)),
            contentDescription = stringResource(R.string.art_desk_case5_cd),
            contentScale = ContentScale.Crop,
            colorFilter = activePhotoFilter,
            modifier =
              Modifier.fillMaxSize()
                .graphicsLayer {
                  val breath = breathPhaseProvider()
                  val s = 1.012f + breath * 0.014f
                  scaleX = s
                  scaleY = s
                }
                .alpha(deskLayers.case5)
                .testTag("desk_stage_photo_5"),
          )
        }
      }

      // Dynamic lighting vignette, rain glass, Lamp On/Off cones, and Case IV-V Room bleed overlays
      Canvas(modifier = Modifier.fillMaxSize().testTag("haunted_desk_backdrop")) {
        val breath = breathPhaseProvider()
        val drop = dropPhaseProvider()
        val haunt = desk.hauntDepth.coerceIn(0f, 1f)
        val lampOn = desk.lampOn

        // 1. Atmospheric dark vignette & central readability recess
        drawRect(
          brush =
            Brush.verticalGradient(
              colors =
                listOf(
                  Color(0xFF06090D).copy(alpha = if (lampOn) 0.56f else 0.72f),
                  Color(0xFF070A0E).copy(alpha = if (lampOn) 0.24f else 0.42f),
                  Color(0xFF06080B).copy(alpha = if (lampOn) 0.58f else 0.72f),
                )
            )
        )
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(Color(0xFF06090D).copy(alpha = 0.52f), Color.Transparent),
              center = Offset(size.width * 0.5f, size.height * 0.28f),
              radius = size.minDimension * 0.54f,
            ),
          radius = size.minDimension * 0.54f,
          center = Offset(size.width * 0.5f, size.height * 0.28f),
        )

        // 2. Overhead tungsten lamp cone (Lamp On) OR cold moonlit slate wash (Lamp Off)
        val lampSway = (breath - 0.5f) * 8.dp.toPx()
        val lampOrigin = Offset(size.width * 0.5f + lampSway, 0f)
        if (lampOn) {
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    colorScheme.primary.copy(alpha = 0.24f + breath * 0.04f),
                    colorScheme.primary.copy(alpha = 0.06f),
                    Color.Transparent,
                  ),
                center = Offset(lampOrigin.x, size.height * 0.42f),
                radius = size.minDimension * 0.74f,
              ),
            radius = size.minDimension * 0.74f,
            center = Offset(lampOrigin.x, size.height * 0.42f),
          )
        } else {
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    colorScheme.secondary.copy(alpha = 0.16f + breath * 0.04f),
                    Color.Transparent,
                  ),
                center = Offset(size.width * 0.16f, size.height * 0.20f),
                radius = size.minDimension * 0.85f,
              ),
            radius = size.minDimension * 0.85f,
            center = Offset(size.width * 0.16f, size.height * 0.20f),
          )
        }

        // 3. Cold rain streaks across dark precinct glass
        val rainAlpha = if (lampOn) 0.11f else 0.22f
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

        // 4. Slow-Boil-quarantined wet tile grout hairline overlay (Case IV Lamp Off or Case V+)
        val showGrout = (desk.caseAngle == 4 && !lampOn) || desk.caseAngle >= 5 || desk.fileZeroRevealed
        if (showGrout) {
          val bleedStrength = ((haunt - 0.55f) / 0.45f).coerceIn(0.15f, 1f)
          val groutAlpha = (bleedStrength * if (lampOn) 0.14f else 0.24f).coerceIn(0.05f, 0.26f)
          val groutColor = colorScheme.secondary.copy(alpha = groutAlpha)
          val step = 38.dp.toPx()
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

        // 5. Case V+ Grotesque Intrusion: slow grey oil bead falling from lamp center + corner scratches
        if (desk.caseAngle >= 5 || desk.fileZeroRevealed) {
          val dropY = 14.dp.toPx() + drop * (size.height * 0.46f)
          val dropAlpha = (1f - drop) * 0.72f
          drawCircle(
            color = colorScheme.secondary.copy(alpha = dropAlpha),
            radius = 2.4.dp.toPx(),
            center = Offset(lampOrigin.x, dropY),
          )
          val scratchColor = colorScheme.tertiary.copy(alpha = 0.26f + breath * 0.14f)
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

        // 6. File #00 blotter peel-back accent corner
        if (desk.fileZeroRevealed) {
          scratchPath.rewind()
          scratchPath.moveTo(0f, size.height)
          scratchPath.lineTo(size.width * 0.24f, size.height)
          scratchPath.lineTo(0f, size.height * 0.72f)
          scratchPath.close()
          drawPath(scratchPath, colorScheme.tertiaryContainer.copy(alpha = 0.62f))
          drawPath(scratchPath, colorScheme.tertiary, style = Stroke(1.3.dp.toPx()))
        }
      }

      // Interactive Tactile Brass Lamp Pull-Chain Toggle (>= 48dp touch target, top-right)
      Row(
        modifier =
          Modifier.align(Alignment.TopEnd)
            .padding(top = 6.dp, end = 10.dp),
        verticalAlignment = Alignment.Top,
      ) {
        Canvas(modifier = Modifier.size(width = 10.dp, height = 34.dp)) {
          val cx = size.width * 0.5f
          val beadColor =
            if (desk.lampOn) Color(0xFFD2A860) else Color(0xFF6B7B8F)
          for (b in 0..4) {
            drawCircle(
              color = beadColor.copy(alpha = 0.85f),
              radius = 1.6.dp.toPx(),
              center = Offset(cx, (4 + b * 6).dp.toPx()),
            )
          }
          drawCircle(
            color = beadColor,
            radius = 3.2.dp.toPx(),
            center = Offset(cx, 31.dp.toPx()),
          )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Surface(
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .testTag("lamp_toggle_button")
              .semantics { contentDescription = lampToggleCd }
              .clickable(role = Role.Switch, onClick = onToggleLamp),
          shape = MaterialTheme.shapes.extraSmall,
          color =
            if (desk.lampOn) Color(0xFF281F13).copy(alpha = 0.90f)
            else Color(0xFF0C121B).copy(alpha = 0.92f),
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
            color = Color(0xFF080C10).copy(alpha = 0.80f),
            border =
              BorderStroke(
                1.dp,
                if (desk.lampOn) colorScheme.primary.copy(alpha = 0.45f) else colorScheme.outline,
              ),
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
            Modifier.size(width = 186.dp, height = 84.dp)
              .minimumInteractiveComponentSize()
              .testTag("inspect_case_button")
              .semantics { contentDescription = inspectCd }
              .clickable(role = Role.Button, onClick = onInspectCaseFile),
          shape = MaterialTheme.shapes.small,
          color = Color(0xFF0C1116),
          border =
            BorderStroke(
              width = 1.5.dp,
              color =
                if (desk.lampOn) colorScheme.primary.copy(alpha = 0.85f)
                else colorScheme.secondary.copy(alpha = 0.58f),
            ),
        ) {
          Box(modifier = Modifier.fillMaxSize()) {
            Image(
              painter = painterResource(id = R.drawable.img_arc2_case_stamp),
              contentDescription = stringResource(R.string.art_arc2_case_stamp_cd),
              contentScale = ContentScale.Crop,
              colorFilter = activePhotoFilter,
              modifier =
                Modifier.fillMaxSize()
                  .alpha(if (desk.lampOn) 0.72f else 0.50f)
                  .testTag("art_arc2_case_stamp"),
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
              drawRect(
                brush =
                  Brush.radialGradient(
                    colors =
                      listOf(
                        Color(0xFF070A0E).copy(alpha = if (desk.lampOn) 0.66f else 0.74f),
                        Color(0xFF070A0E).copy(alpha = if (desk.lampOn) 0.42f else 0.56f),
                      ),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = size.maxDimension * 0.72f,
                  )
              )
              val inset = 5.dp.toPx()
              drawRect(
                color =
                  (if (desk.lampOn) colorScheme.primary else colorScheme.secondary).copy(alpha = 0.28f),
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(width = 1.dp.toPx()),
              )
            }
            Column(
              modifier = Modifier.fillMaxSize().padding(8.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
            ) {
              Text(
                text = stringResource(R.string.arc2_inspect_label),
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.6.sp),
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
      }

      // Interactive Circular Feathered Macro Desk Anomalies (strictly gated by Case Angle & Lamp posture)
      val envelopeDrip = stringResource(R.string.drip_desk_anomaly_envelope)
      val spectaclesDrip = stringResource(R.string.drip_desk_anomaly_spectacles)
      val tapeDrip = stringResource(R.string.drip_desk_anomaly_tape)
      val enamelDrip = stringResource(R.string.drip_desk_anomaly_enamel)

      desk.activeDeskAnomalies.forEach { anomaly ->
        key(anomaly.id) {
          val offsetX = ((boxW - 76.dp) * anomaly.xFraction).coerceIn(4.dp, (boxW - 80.dp).coerceAtLeast(4.dp))
          val offsetY = ((boxH - 76.dp) * anomaly.yFraction).coerceIn(4.dp, (boxH - 80.dp).coerceAtLeast(4.dp))
          val dripLine =
            when (anomaly.type) {
              DeskAnomalyType.EVIDENCE_ENVELOPE -> envelopeDrip
              DeskAnomalyType.FOGGED_SPECTACLES -> spectaclesDrip
              DeskAnomalyType.REEL_TAPE -> tapeDrip
              DeskAnomalyType.ENAMEL_EXHIBIT -> enamelDrip
            }
          DeskAnomalyNode(
            anomaly = anomaly,
            breathPhaseProvider = breathPhaseProvider,
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
