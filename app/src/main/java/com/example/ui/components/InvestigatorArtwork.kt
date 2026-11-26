package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.ActiveDeskAnomaly
import com.example.game.CaseExhibit
import com.example.game.DeskAnomalyType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Opacity weights for the 5 progressive Arc 2 photographic desk backdrops across Cases I..V.
 * Strictly honors the 4-Act Slow-Boil Quarantine: Cases I..III keep Case IV & Case V at 0f.
 * Mirrored 1:1 on Web (`computeDeskLayerAlphas` in `docs/src/canvas/arc2DeskCanvas.js`).
 */
internal data class DeskLayerAlphas(
  val case1: Float,
  val case2: Float,
  val case3: Float,
  val case4: Float,
  val case5: Float,
)

internal fun computeDeskLayerAlphas(
  caseAngle: Int,
  hauntDepth: Float,
  lampOn: Boolean,
  fileZeroRevealed: Boolean,
): DeskLayerAlphas {
  val clampedCase = caseAngle.coerceIn(1, 5)
  val haunt = hauntDepth.coerceIn(0f, 1f)
  if (fileZeroRevealed) {
    return DeskLayerAlphas(
      case1 = 0f,
      case2 = 0f,
      case3 = 0f,
      case4 = 0.14f,
      case5 = 0.94f,
    )
  }
  return when (clampedCase) {
    1 -> DeskLayerAlphas(case1 = 0.90f, case2 = 0f, case3 = 0f, case4 = 0f, case5 = 0f)
    2 -> DeskLayerAlphas(case1 = 0.22f, case2 = 0.88f, case3 = 0f, case4 = 0f, case5 = 0f)
    3 -> DeskLayerAlphas(case1 = 0f, case2 = 0.20f, case3 = 0.88f, case4 = 0f, case5 = 0f)
    4 ->
      DeskLayerAlphas(
        case1 = 0f,
        case2 = 0f,
        case3 = 0.18f,
        case4 = if (lampOn) 0.78f else 0.92f,
        case5 = if (!lampOn && haunt >= 0.72f) 0.24f else 0f,
      )
    else ->
      DeskLayerAlphas(
        case1 = 0f,
        case2 = 0f,
        case3 = 0f,
        case4 = 0.26f,
        case5 = if (lampOn) 0.82f else 0.92f,
      )
  }
}

@DrawableRes
internal fun deskStageDrawableRes(stage: Int): Int =
  when (stage.coerceIn(1, 5)) {
    1 -> R.drawable.img_desk_case1
    2 -> R.drawable.img_desk_case2
    3 -> R.drawable.img_desk_case3
    4 -> R.drawable.img_desk_case4
    else -> R.drawable.img_desk_case5
  }

@DrawableRes
internal fun deskAnomalyDrawableRes(type: DeskAnomalyType): Int =
  when (type) {
    DeskAnomalyType.EVIDENCE_ENVELOPE -> R.drawable.img_desk_anomaly_envelope
    DeskAnomalyType.FOGGED_SPECTACLES -> R.drawable.img_desk_anomaly_spectacles
    DeskAnomalyType.REEL_TAPE -> R.drawable.img_desk_anomaly_reel_tape
    DeskAnomalyType.ENAMEL_EXHIBIT -> R.drawable.img_desk_anomaly_enamel
  }

@DrawableRes
internal fun caseExhibitDrawableRes(exhibit: CaseExhibit): Int =
  when (exhibit) {
    CaseExhibit.WATER_STAINED_PETITION -> R.drawable.img_exhibit_water_stained_petition
    CaseExhibit.UNLISTED_PAY_STUB -> R.drawable.img_exhibit_unlisted_pay_stub
    CaseExhibit.TAX_PLAT_1998 -> R.drawable.img_exhibit_tax_plat_1998
    CaseExhibit.INSIDE_CONDENSATION_SWAB -> R.drawable.img_exhibit_inside_condensation_swab
    CaseExhibit.TUESDAY_STATEMENT_COPY -> R.drawable.img_exhibit_tuesday_statement_copy
    CaseExhibit.SECOND_CHAIR_POLAROID -> R.drawable.img_exhibit_second_chair_polaroid
    CaseExhibit.COAT_CLAIM_TICKET -> R.drawable.img_exhibit_coat_claim_ticket
    CaseExhibit.REEL_FOUR_WAVEFORM -> R.drawable.img_exhibit_reel_four_waveform
    CaseExhibit.INTAKE_THUMB_CAST -> R.drawable.img_exhibit_intake_thumb_cast
    CaseExhibit.DRAINAGE_LOOP_BLUEPRINT -> R.drawable.img_exhibit_drainage_loop_blueprint
  }

/**
 * Pure haunted breathing scale `(scaleX, scaleY)` for circular macro Desk Anomaly nodes,
 * mirrored 1:1 by CSS `desk-anomaly-breathe` / `desk-anomaly-pulse` keyframes on Web.
 */
internal fun computeDeskAnomalyBreathScale(
  anomalyId: Long,
  type: DeskAnomalyType,
  breathPhase: Float,
): Pair<Float, Float> {
  val offset = ((abs(anomalyId) * 0.31f) % 1f)
  val cycle = (breathPhase.coerceIn(0f, 1f) + offset) % 1f
  val wave =
    if (type == DeskAnomalyType.ENAMEL_EXHIBIT) {
      val primary = ((sin(cycle * 2.0 * PI) + 1.0) * 0.5).toFloat()
      val echo = ((sin((cycle * 4.0 - 0.32) * PI) + 1.0) * 0.5).toFloat()
      (primary * 0.72f + echo * 0.28f).coerceIn(0f, 1f)
    } else {
      val primary = ((sin((cycle - 0.14) * 2.0 * PI) + 1.0) * 0.5).toFloat()
      val secondary = ((sin(cycle * 4.0 * PI) + 1.0) * 0.5).toFloat()
      (primary * 0.85f + secondary * 0.15f).coerceIn(0f, 1f)
    }
  val (minX, ampX, minY, ampY) =
    when (type) {
      DeskAnomalyType.EVIDENCE_ENVELOPE -> floatArrayOf(0.985f, 0.042f, 0.988f, 0.036f)
      DeskAnomalyType.FOGGED_SPECTACLES -> floatArrayOf(0.982f, 0.048f, 0.986f, 0.042f)
      DeskAnomalyType.REEL_TAPE -> floatArrayOf(0.982f, 0.052f, 0.986f, 0.044f)
      DeskAnomalyType.ENAMEL_EXHIBIT -> floatArrayOf(0.980f, 0.062f, 0.984f, 0.050f)
    }
  return (minX + ampX * wave) to (minY + ampY * wave)
}

/**
 * Borderless circular radially feathered macro photograph for Arc 2 Desk Anomalies.
 * Dissolves seamlessly into the nocturnal desk surface with subtle haunted breathing motion.
 */
@Composable
internal fun DeskAnomalyNode(
  anomaly: ActiveDeskAnomaly,
  breathPhaseProvider: () -> Float,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val hapticFeedback = LocalHapticFeedback.current

  val label =
    when (anomaly.type) {
      DeskAnomalyType.EVIDENCE_ENVELOPE -> stringResource(R.string.desk_anomaly_envelope_label)
      DeskAnomalyType.FOGGED_SPECTACLES -> stringResource(R.string.desk_anomaly_spectacles_label)
      DeskAnomalyType.REEL_TAPE -> stringResource(R.string.desk_anomaly_tape_label)
      DeskAnomalyType.ENAMEL_EXHIBIT -> stringResource(R.string.desk_anomaly_enamel_label)
    }
  val cd =
    when (anomaly.type) {
      DeskAnomalyType.EVIDENCE_ENVELOPE -> stringResource(R.string.desk_anomaly_envelope_cd)
      DeskAnomalyType.FOGGED_SPECTACLES -> stringResource(R.string.desk_anomaly_spectacles_cd)
      DeskAnomalyType.REEL_TAPE -> stringResource(R.string.desk_anomaly_tape_cd)
      DeskAnomalyType.ENAMEL_EXHIBIT -> stringResource(R.string.desk_anomaly_enamel_cd)
    }
  val accentColor =
    when (anomaly.type) {
      DeskAnomalyType.EVIDENCE_ENVELOPE -> colorScheme.primary
      DeskAnomalyType.FOGGED_SPECTACLES -> colorScheme.secondary
      DeskAnomalyType.REEL_TAPE -> colorScheme.onBackground
      DeskAnomalyType.ENAMEL_EXHIBIT -> colorScheme.tertiary
    }
  val cavityColor =
    when (anomaly.type) {
      DeskAnomalyType.EVIDENCE_ENVELOPE -> colorScheme.primaryContainer
      DeskAnomalyType.FOGGED_SPECTACLES -> colorScheme.secondaryContainer
      DeskAnomalyType.REEL_TAPE -> colorScheme.surfaceVariant
      DeskAnomalyType.ENAMEL_EXHIBIT -> colorScheme.tertiaryContainer
    }

  Box(
    modifier =
      modifier
        .size(76.dp)
        .minimumInteractiveComponentSize()
        .drawBehind {
          val (sx, _) = computeDeskAnomalyBreathScale(anomaly.id, anomaly.type, breathPhaseProvider())
          val glow = ((sx - 0.98f) / 0.062f).coerceIn(0f, 1f)
          val center = Offset(size.width * 0.5f, size.height * 0.5f)
          val haloR = size.minDimension * (0.54f + glow * 0.14f)
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    colorScheme.background.copy(alpha = 0.88f),
                    cavityColor.copy(alpha = 0.34f + glow * 0.26f),
                    accentColor.copy(alpha = 0.12f + glow * 0.18f),
                    Color.Transparent,
                  ),
                center = center,
                radius = haloR,
              ),
            radius = haloR,
            center = center,
          )
        }
        .graphicsLayer {
          val (sx, sy) = computeDeskAnomalyBreathScale(anomaly.id, anomaly.type, breathPhaseProvider())
          scaleX = sx
          scaleY = sy
        }
        .testTag("desk_anomaly_${anomaly.type.id}_${anomaly.id}")
        .semantics { contentDescription = cd }
        .clickable(
          role = Role.Button,
          onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
          },
        ),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier.size(66.dp).clip(CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Image(
        painter = painterResource(id = deskAnomalyDrawableRes(anomaly.type)),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
          Modifier.fillMaxSize()
            .graphicsLayer {
              scaleX = 1.12f
              scaleY = 1.12f
            }
            .testTag("desk_anomaly_art_${anomaly.type.id}"),
      )
      Canvas(modifier = Modifier.fillMaxSize()) {
        val (sx, _) = computeDeskAnomalyBreathScale(anomaly.id, anomaly.type, breathPhaseProvider())
        val glow = ((sx - 0.98f) / 0.062f).coerceIn(0f, 1f)
        val center = Offset(size.width * 0.5f, size.height * 0.5f)
        val radius = size.minDimension * 0.5f
        drawCircle(
          brush =
            Brush.radialGradient(
              colorStops =
                arrayOf(
                  0.0f to Color.Transparent,
                  0.48f to Color.Transparent,
                  0.78f to colorScheme.background.copy(alpha = 0.60f),
                  1.0f to colorScheme.background.copy(alpha = 0.96f),
                ),
              center = center,
              radius = radius,
            ),
          radius = radius,
          center = center,
        )
        drawCircle(
          color = accentColor.copy(alpha = 0.18f + glow * 0.24f),
          radius = radius - 1.5.dp.toPx(),
          center = center,
          style = Stroke(width = 1.1.dp.toPx()),
        )
      }
    }

    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, letterSpacing = 1.1.sp),
      color = accentColor.copy(alpha = 0.94f),
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .background(
            color = colorScheme.background.copy(alpha = 0.82f),
            shape = RoundedCornerShape(2.dp),
          )
          .padding(horizontal = 5.dp, vertical = 1.dp),
    )
  }
}

/**
 * Framed evidence macro Polaroid portrait for Corkboard cards and Dossier unlock rows.
 */
@Composable
internal fun ExhibitPolaroidThumb(
  exhibit: CaseExhibit,
  pinned: Boolean,
  wired: Boolean = false,
  selected: Boolean = false,
  size: Dp = 46.dp,
  modifier: Modifier = Modifier,
) {
  val cs = MaterialTheme.colorScheme
  val borderColor =
    when {
      selected -> cs.primary
      wired -> Color(0xFF9E2B2E)
      pinned -> cs.outline
      else -> cs.outlineVariant
    }
  val unpinnedFilter =
    remember {
      ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.25f) })
    }

  Surface(
    modifier = modifier.size(size).testTag("art_exhibit_${exhibit.id}"),
    shape = MaterialTheme.shapes.extraSmall,
    color = Color(0xFF0A0D10),
    border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Image(
        painter = painterResource(id = caseExhibitDrawableRes(exhibit)),
        contentDescription = exhibit.title,
        contentScale = ContentScale.Crop,
        colorFilter = if (pinned) null else unpinnedFilter,
        modifier = Modifier.fillMaxSize().alpha(if (pinned) 0.96f else 0.54f),
      )
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
          brush =
            Brush.radialGradient(
              colors =
                listOf(
                  Color.Transparent,
                  Color(0xFF070A0D).copy(alpha = if (pinned) 0.42f else 0.64f),
                ),
              center = Offset(this.size.width * 0.5f, this.size.height * 0.5f),
              radius = this.size.maxDimension * 0.72f,
            )
        )
        if (pinned) {
          // Subtle aged Polaroid corner brackets
          val inset = 2.5.dp.toPx()
          val arm = 4.dp.toPx()
          val bracketColor = cs.onBackground.copy(alpha = 0.28f)
          drawLine(bracketColor, Offset(inset, inset), Offset(inset + arm, inset), 1.dp.toPx())
          drawLine(bracketColor, Offset(inset, inset), Offset(inset, inset + arm), 1.dp.toPx())
          drawLine(
            bracketColor,
            Offset(this.size.width - inset, this.size.height - inset),
            Offset(this.size.width - inset - arm, this.size.height - inset),
            1.dp.toPx(),
          )
          drawLine(
            bracketColor,
            Offset(this.size.width - inset, this.size.height - inset),
            Offset(this.size.width - inset, this.size.height - inset - arm),
            1.dp.toPx(),
          )
        }
      }
    }
  }
}
