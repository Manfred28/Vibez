package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.CultDossierLine
import com.example.game.DredgedRelic
import com.example.game.OrganGameState
import com.example.ui.theme.BleakTheme
import kotlin.math.ceil

/**
 * Progressive Chamber Rites Deck panel hosting Excision II–V active mechanics:
 * - Excision II: Floor Drain Grate Seal/Vent + Tappable Inspectable Dredged Relics
 * - Excision III: Neighbor Aperture Echo Diversion (0% / 25% / 50%)
 * - Excision IV: Valve Clamp (20s hold -> Marrow Bloom)
 * - Excision V: The Ossified Throat & Redacted Cult Dossier
 */
@Composable
fun ChamberRitesPanel(
  state: OrganGameState,
  onToggleDrainSeal: () -> Unit,
  onDredgeSump: () -> Unit,
  onSetEchoDiversion: (Int) -> Unit,
  onActivateValveClamp: () -> Unit,
  onOfferToThroat: (CultDossierLine) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val atmosphere = BleakTheme.atmosphere
  var inspectedRelic by remember { mutableStateOf<DredgedRelic?>(null) }

  Column(
    modifier = modifier.fillMaxWidth().testTag("chamber_rites_panel"),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // --- Excision II: Floor Drain Grate & Inspectable Dredged Relics ---
    if (state.highestExcisionMilestone >= 2) {
      RiteSectionCard(
        title = stringResource(R.string.rite_drain_title),
        subtitle = stringResource(R.string.rite_drain_desc),
        artworkKind = RiteArtworkKind.FLOOR_DRAIN,
        artworkActive = state.drainSealed || state.drainPressure >= 100.0,
        testTag = "rite_drain_section",
      ) {
        val pressurePct = state.drainPressure.toInt().coerceIn(0, 100)
        Text(
          text = stringResource(R.string.rite_drain_pressure, pressurePct),
          style = MaterialTheme.typography.labelSmall,
          color = if (pressurePct >= 100) colorScheme.primary else colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag("drain_pressure_text"),
        )
        LinearProgressIndicator(
          progress = { (state.drainPressure / 100.0).toFloat().coerceIn(0f, 1f) },
          modifier = Modifier.fillMaxWidth().height(3.dp),
          color = colorScheme.primary,
          trackColor = atmosphere.hairline,
          drawStopIndicator = {},
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          RiteButton(
            label =
              if (state.drainSealed) stringResource(R.string.rite_drain_vent)
              else stringResource(R.string.rite_drain_seal),
            enabled = true,
            highlighted = state.drainSealed,
            testTag = "drain_seal_button",
            onClick = onToggleDrainSeal,
            modifier = Modifier.weight(1f),
          )
          RiteButton(
            label =
              if (state.nextDredgeableRelic == null) stringResource(R.string.rite_drain_complete)
              else stringResource(R.string.rite_drain_dredge),
            enabled = state.canDredgeSump,
            highlighted = state.canDredgeSump,
            testTag = "dredge_sump_button",
            onClick = onDredgeSump,
            modifier = Modifier.weight(1f),
          )
        }

        // Interactive graphic Dredged Relic tags with inline thematic inspection
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          DredgedRelic.entries.forEach { relic ->
            val owned = relic in state.dredgedRelics
            val selected = inspectedRelic == relic
            val relicName =
              when (relic) {
                DredgedRelic.PATIENT_WRISTBAND -> stringResource(R.string.relic_wristband_name)
                DredgedRelic.BRASS_WARD_KEY -> stringResource(R.string.relic_brass_key_name)
                DredgedRelic.BLEEDING_LOGBOOK -> stringResource(R.string.relic_logbook_name)
              }

            Surface(
              modifier =
                Modifier.weight(1f)
                  .minimumInteractiveComponentSize()
                  .testTag("relic_inspect_${relic.id}")
                  .clickable(role = Role.Button) {
                    inspectedRelic = if (selected) null else relic
                  },
              shape = MaterialTheme.shapes.extraSmall,
              color =
                when {
                  selected -> colorScheme.primaryContainer.copy(alpha = 0.72f)
                  owned -> colorScheme.surface.copy(alpha = 0.92f)
                  else -> colorScheme.background.copy(alpha = 0.55f)
                },
              border =
                BorderStroke(
                  width = 1.dp,
                  color =
                    when {
                      selected -> colorScheme.primary
                      owned -> colorScheme.outline
                      else -> colorScheme.outlineVariant
                    },
                ),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
              ) {
                RelicArtworkEmblem(relic = relic, owned = owned)
                Text(
                  text = if (owned) relicName else "???",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, letterSpacing = 0.5.sp),
                  color = if (owned) colorScheme.onBackground else colorScheme.onSurfaceVariant,
                  maxLines = 1,
                )
              }
            }
          }
        }

        inspectedRelic?.let { relic ->
          val owned = relic in state.dredgedRelics
          val hintText =
            if (!owned) {
              stringResource(R.string.relic_locked_hint)
            } else {
              when (relic) {
                DredgedRelic.PATIENT_WRISTBAND -> stringResource(R.string.relic_wristband_hint)
                DredgedRelic.BRASS_WARD_KEY -> stringResource(R.string.relic_brass_key_hint)
                DredgedRelic.BLEEDING_LOGBOOK -> stringResource(R.string.relic_logbook_hint)
              }
            }
          Surface(
            modifier = Modifier.fillMaxWidth().testTag("relic_inspection_card"),
            shape = MaterialTheme.shapes.extraSmall,
            color = colorScheme.background.copy(alpha = 0.78f),
            border = BorderStroke(1.dp, if (owned) colorScheme.primary.copy(alpha = 0.65f) else atmosphere.hairline),
          ) {
            Text(
              text = hintText,
              style = MaterialTheme.typography.bodySmall,
              color = if (owned) colorScheme.onBackground else colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            )
          }
        }
      }
    }

    // --- Excision III: Neighbor Aperture Echo Routing ---
    if (state.highestExcisionMilestone >= 3) {
      RiteSectionCard(
        title = stringResource(R.string.rite_echo_title),
        subtitle = stringResource(R.string.rite_echo_desc),
        artworkKind = RiteArtworkKind.NEIGHBOR_APERTURE,
        artworkActive = state.echoDiversionPercent > 0,
        testTag = "rite_echo_section",
      ) {
        Text(
          text = stringResource(R.string.rite_echo_pulses, state.echoPulsesTriggered),
          style = MaterialTheme.typography.labelSmall,
          color = colorScheme.primary,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf(0, 25, 50).forEach { pct ->
            RiteButton(
              label = if (pct == 0) "CLOSED (0%)" else "ROUTE $pct%",
              enabled = true,
              highlighted = state.echoDiversionPercent == pct,
              testTag = "echo_diversion_$pct",
              onClick = { onSetEchoDiversion(pct) },
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }

    // --- Excision IV: Valve Clamp ---
    if (state.highestExcisionMilestone >= 4) {
      RiteSectionCard(
        title = stringResource(R.string.rite_clamp_title),
        subtitle = stringResource(R.string.rite_clamp_desc),
        artworkKind = RiteArtworkKind.VALVE_CLAMP,
        artworkActive = state.isValveClamped || state.marrowBloomRemainingSeconds > 0.0,
        testTag = "rite_clamp_section",
      ) {
        val clampLabel =
          if (state.isValveClamped) {
            stringResource(R.string.rite_clamp_active, ceil(state.clampRemainingSeconds).toInt())
          } else {
            stringResource(R.string.rite_clamp_action, state.valveClampDurationSec.toInt())
          }
        RiteButton(
          label = clampLabel,
          enabled = !state.isValveClamped,
          highlighted = state.isValveClamped,
          testTag = "activate_valve_clamp_button",
          onClick = onActivateValveClamp,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }

    // --- Excision V: The Ossified Throat & Redacted Cult Dossier ---
    if (state.highestExcisionMilestone >= 5) {
      RiteSectionCard(
        title = stringResource(R.string.rite_throat_title),
        subtitle = stringResource(R.string.rite_throat_desc),
        artworkKind = RiteArtworkKind.OSSIFIED_THROAT,
        artworkActive = state.unredactedDossierLines.isNotEmpty(),
        testTag = "rite_throat_section",
      ) {
        CultDossierLine.entries.forEach { line ->
          val unsealed = line in state.unredactedDossierLines
          val canOffer = state.canOfferToThroat(line)
          val costText = buildString {
            append("${line.siphonCost} Siphons")
            if (line.tendrilCost > 0) append(" · ${line.tendrilCost} Tendrils")
            if (line.requiresJar) append(" · 1 Jar")
          }
          val labelText =
            if (unsealed) {
              "LINE 0${line.lineNumber} — UNSEALED"
            } else {
              "LINE 0${line.lineNumber} [████████████] ($costText)"
            }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = labelText,
              style = MaterialTheme.typography.bodySmall,
              color = if (unsealed) colorScheme.primary else colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            RiteButton(
              label =
                if (unsealed) stringResource(R.string.rite_throat_unsealed)
                else stringResource(R.string.rite_throat_offer),
              enabled = canOffer,
              highlighted = unsealed,
              testTag = "offer_throat_${line.id}",
              onClick = { onOfferToThroat(line) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RiteSectionCard(
  title: String,
  subtitle: String,
  artworkKind: RiteArtworkKind,
  artworkActive: Boolean,
  testTag: String,
  content: @Composable () -> Unit,
) {
  val colorScheme = MaterialTheme.colorScheme
  Surface(
    modifier = Modifier.fillMaxWidth().testTag(testTag),
    shape = MaterialTheme.shapes.extraSmall,
    color = colorScheme.surfaceVariant.copy(alpha = 0.80f),
    border = BorderStroke(1.dp, colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        RiteArtworkEmblem(kind = artworkKind, active = artworkActive)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.8.sp),
            color = colorScheme.onBackground,
          )
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
          )
        }
      }
      content()
    }
  }
}

@Composable
private fun RiteButton(
  label: String,
  enabled: Boolean,
  highlighted: Boolean,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  Surface(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .testTag(testTag)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    shape = MaterialTheme.shapes.extraSmall,
    color =
      when {
        highlighted -> colorScheme.tertiaryContainer.copy(alpha = 0.85f)
        enabled -> colorScheme.surface.copy(alpha = 0.92f)
        else -> colorScheme.background.copy(alpha = 0.65f)
      },
    border =
      BorderStroke(
        width = 1.dp,
        color =
          when {
            highlighted -> colorScheme.tertiary
            enabled -> colorScheme.primary.copy(alpha = 0.55f)
            else -> colorScheme.outlineVariant
          },
      ),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.7.sp),
      color =
        when {
          highlighted -> colorScheme.onTertiaryContainer
          enabled -> colorScheme.primary
          else -> colorScheme.onSurfaceVariant
        },
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
    )
  }
}
