package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.CreatureType
import com.example.game.OrganGameState
import com.example.ui.theme.BleakTheme
import java.text.NumberFormat

/**
 * Wet-lab Specimen Dossier Card for Organisms (Suture-Hand, Leech-Clerk, Weeping Lamp).
 * Replaces flat generic shop rows with:
 * - Clinical catalog header (`SPEC-01..03`), colony count `[N]`, and live physiological status badge
 * - Framed 64dp specimen observation viewport with corner registration reticles & vignette glass
 * - Live effective yield telemetry (reflecting Jars, Stillness 2x, Eye Gaze, and Scar Tissue)
 * - Tactile >=48dp Cultivate button and silent 2dp affordability creep bar along the bottom edge
 */
@Composable
internal fun OrganismSpecimenCard(
  type: CreatureType,
  state: OrganGameState,
  onBuy: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val atmosphere = BleakTheme.atmosphere
  val numberFormat = remember { NumberFormat.getIntegerInstance() }

  val count = state.creatureCount(type)
  val cost = state.creatureCost(type)
  val canAfford = state.ichor >= cost.toDouble()
  val affordabilityProgress =
    if (cost <= 0L) 1f else (state.ichor / cost.toDouble()).toFloat().coerceIn(0f, 1f)

  val specCode =
    when (type) {
      CreatureType.SUTURE_HAND -> stringResource(R.string.creature_suture_code)
      CreatureType.LEECH_CLERK -> stringResource(R.string.creature_leech_code)
      CreatureType.WEEPING_LAMP -> stringResource(R.string.creature_lamp_code)
      CreatureType.SUMP_SIPHON -> stringResource(R.string.creature_siphon_code)
    }
  val title =
    when (type) {
      CreatureType.SUTURE_HAND -> stringResource(R.string.creature_suture_name)
      CreatureType.LEECH_CLERK -> stringResource(R.string.creature_leech_name)
      CreatureType.WEEPING_LAMP -> stringResource(R.string.creature_lamp_name)
      CreatureType.SUMP_SIPHON -> stringResource(R.string.creature_siphon_name)
    }
  val desc =
    when (type) {
      CreatureType.SUTURE_HAND -> stringResource(R.string.creature_suture_desc)
      CreatureType.LEECH_CLERK -> stringResource(R.string.creature_leech_desc)
      CreatureType.WEEPING_LAMP -> stringResource(R.string.creature_lamp_desc)
      CreatureType.SUMP_SIPHON -> stringResource(R.string.creature_siphon_desc)
    }

  val isWeeping = type == CreatureType.WEEPING_LAMP && count > 0 && state.isStillnessActive
  val isActivelyWorking = count > 0 && (type != CreatureType.WEEPING_LAMP || isWeeping)
  val statusLabel =
    when {
      count <= 0 -> stringResource(R.string.organism_status_dormant)
      type == CreatureType.SUTURE_HAND -> stringResource(R.string.organism_status_suture_active)
      type == CreatureType.LEECH_CLERK -> stringResource(R.string.organism_status_leech_active)
      type == CreatureType.SUMP_SIPHON -> stringResource(R.string.organism_status_siphon_active)
      state.isStillnessActive -> stringResource(R.string.organism_status_lamp_weeping)
      else -> stringResource(R.string.organism_status_lamp_idle)
    }

  val eachRate = state.effectiveCreatureIchorPerSec(type)
  val colonyRate = state.totalCreatureIchorPerSec(type)
  val yieldText =
    if (count > 0) {
      stringResource(
        R.string.organism_yield_colony_format,
        formatCompactDecimal(eachRate),
        formatCompactDecimal(colonyRate),
      )
    } else {
      stringResource(R.string.organism_yield_each_format, formatCompactDecimal(eachRate))
    }

  val cardBorderColor =
    when {
      isWeeping -> colorScheme.primary.copy(alpha = 0.72f)
      canAfford -> colorScheme.outline
      count > 0 -> colorScheme.outlineVariant
      else -> atmosphere.hairline
    }

  Surface(
    modifier = modifier.fillMaxWidth().testTag("organism_card_${type.id}"),
    shape = MaterialTheme.shapes.extraSmall,
    color =
      if (canAfford || count > 0) colorScheme.surfaceVariant.copy(alpha = 0.92f)
      else colorScheme.surfaceContainerLowest,
    border = BorderStroke(1.dp, cardBorderColor),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Clinical Specimen Dossier Header Strip
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(colorScheme.background.copy(alpha = 0.48f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f).padding(end = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = specCode,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.0.sp),
            color = colorScheme.onSurfaceVariant,
            maxLines = 1,
          )
          Text(
            text = "$title ${stringResource(R.string.owned_count_format, count)}",
            style =
              MaterialTheme.typography.titleMedium.copy(
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.6.sp,
              ),
            color =
              if (canAfford || count > 0) colorScheme.onBackground
              else colorScheme.onBackground.copy(alpha = 0.80f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        // Live physiological status badge
        Surface(
          shape = MaterialTheme.shapes.extraSmall,
          color =
            when {
              isWeeping -> colorScheme.primary.copy(alpha = 0.22f)
              isActivelyWorking -> colorScheme.secondaryContainer.copy(alpha = 0.72f)
              else -> colorScheme.surface
            },
          border =
            BorderStroke(
              width = 1.dp,
              color =
                when {
                  isWeeping -> colorScheme.primary.copy(alpha = 0.80f)
                  isActivelyWorking -> colorScheme.outline
                  else -> colorScheme.outlineVariant
                },
            ),
        ) {
          Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.9.sp),
            color =
              when {
                isWeeping -> colorScheme.primary
                isActivelyWorking -> colorScheme.onBackground.copy(alpha = 0.90f)
                else -> colorScheme.onSurfaceVariant
              },
            modifier =
              Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                .testTag("organism_status_${type.id}"),
          )
        }
      }

      HorizontalDivider(color = atmosphere.hairline.copy(alpha = 0.85f), thickness = 1.dp)

      // Specimen Viewport + Telemetry + Cultivate Control
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OrganismSpecimenViewport(
          type = type,
          active = canAfford || count > 0,
          isWeeping = isWeeping,
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(
          modifier = Modifier.weight(1f).padding(end = 8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color =
              if (canAfford || count > 0) colorScheme.onBackground.copy(alpha = 0.92f)
              else colorScheme.onSurfaceVariant,
          )

          Text(
            text = yieldText,
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                letterSpacing = 0.6.sp,
              ),
            color = if (count > 0) colorScheme.primary else colorScheme.secondary,
            modifier = Modifier.testTag("organism_yield_${type.id}"),
          )

          if (type == CreatureType.WEEPING_LAMP) {
            val stillnessTotal =
              if (count > 0) state.effectiveLampStillnessPerSec() * count
              else state.effectiveLampStillnessPerSec()
            Text(
              text =
                stringResource(
                  R.string.organism_lamp_stillness_format,
                  formatCompactDecimal(stillnessTotal),
                ),
              style =
                MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  letterSpacing = 0.5.sp,
                ),
              color =
                if (isWeeping) colorScheme.primary else colorScheme.onSurfaceVariant,
              modifier = Modifier.testTag("organism_stillness_yield_${type.id}"),
            )
          }
        }

        // Tactile Cultivate button (>= 48dp touch target)
        Surface(
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .testTag("buy_${type.id}")
              .clickable(enabled = canAfford, role = Role.Button, onClick = onBuy),
          shape = MaterialTheme.shapes.extraSmall,
          color =
            if (canAfford) colorScheme.surface.copy(alpha = 0.90f)
            else colorScheme.background.copy(alpha = 0.65f),
          border =
            BorderStroke(
              width = 1.dp,
              color =
                if (canAfford) colorScheme.primary.copy(alpha = 0.44f)
                else colorScheme.outlineVariant,
            ),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              text = stringResource(R.string.organism_action_cultivate),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.sp),
              color =
                if (canAfford) colorScheme.onBackground.copy(alpha = 0.76f)
                else colorScheme.onSurfaceVariant,
            )
            Text(
              text = stringResource(R.string.cost_ichor_format, numberFormat.format(cost)),
              style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.6.sp),
              color =
                if (canAfford) colorScheme.primary.copy(alpha = 0.86f)
                else colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        }
      }

      // Silent 2dp affordability creep bar along the card base
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(2.5.dp)
            .background(colorScheme.background.copy(alpha = 0.75f))
            .testTag("organism_affordability_${type.id}"),
      ) {
        Box(
          modifier =
            Modifier.fillMaxHeight()
              .fillMaxWidth(affordabilityProgress)
              .background(
                when {
                  canAfford -> colorScheme.primary
                  isWeeping -> colorScheme.primary.copy(alpha = 0.75f)
                  else -> colorScheme.tertiary.copy(alpha = 0.68f)
                }
              ),
        )
      }
    }
  }
}

@Composable
private fun OrganismSpecimenViewport(
  type: CreatureType,
  active: Boolean,
  isWeeping: Boolean,
) {
  val colorScheme = MaterialTheme.colorScheme
  val borderColor =
    when {
      isWeeping -> colorScheme.primary.copy(alpha = 0.85f)
      active -> colorScheme.outline
      else -> colorScheme.outlineVariant
    }
  val contentAlpha = if (active) 0.96f else 0.56f
  val lampCd = stringResource(R.string.art_weeping_lamp_cd)

  Surface(
    modifier = Modifier.size(64.dp),
    shape = MaterialTheme.shapes.extraSmall,
    color = colorScheme.background,
    border = BorderStroke(1.dp, borderColor),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      when (type) {
        CreatureType.SUTURE_HAND ->
          Image(
            painter = painterResource(id = R.drawable.img_suture_hand),
            contentDescription = stringResource(R.string.art_suture_hand_cd),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(contentAlpha).testTag("art_suture_hand"),
          )
        CreatureType.LEECH_CLERK ->
          Image(
            painter = painterResource(id = R.drawable.img_leech_clerk),
            contentDescription = stringResource(R.string.art_leech_clerk_cd),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(contentAlpha).testTag("art_leech_clerk"),
          )
        CreatureType.WEEPING_LAMP ->
          WeepingLampSpecimenPortrait(
            isWeeping = isWeeping,
            modifier =
              Modifier.fillMaxSize()
                .alpha(contentAlpha)
                .testTag("art_weeping_lamp")
                .semantics { contentDescription = lampCd },
          )
        CreatureType.SUMP_SIPHON -> {
          val siphonCd = stringResource(R.string.art_sump_siphon_cd)
          SumpSiphonSpecimenPortrait(
            active = active,
            modifier =
              Modifier.fillMaxSize()
                .alpha(contentAlpha)
                .testTag("art_sump_siphon")
                .semantics { contentDescription = siphonCd },
          )
        }
      }

      // Specimen glass vignette + clinical corner registration reticles
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
          brush =
            Brush.radialGradient(
              colors = listOf(Color.Transparent, colorScheme.background.copy(alpha = 0.42f)),
              center = Offset(size.width * 0.5f, size.height * 0.5f),
              radius = size.minDimension * 0.68f,
            )
        )
        val tick = 5.dp.toPx()
        val inset = 3.dp.toPx()
        val reticleColor =
          if (isWeeping) colorScheme.primary.copy(alpha = 0.72f)
          else colorScheme.onBackground.copy(alpha = 0.34f)
        val stroke = 1.dp.toPx()

        // Top-left & bottom-right observation brackets
        drawLine(reticleColor, Offset(inset, inset), Offset(inset + tick, inset), stroke)
        drawLine(reticleColor, Offset(inset, inset), Offset(inset, inset + tick), stroke)
        drawLine(
          reticleColor,
          Offset(size.width - inset, size.height - inset),
          Offset(size.width - inset - tick, size.height - inset),
          stroke,
        )
        drawLine(
          reticleColor,
          Offset(size.width - inset, size.height - inset),
          Offset(size.width - inset, size.height - inset - tick),
          stroke,
        )
      }
    }
  }
}
