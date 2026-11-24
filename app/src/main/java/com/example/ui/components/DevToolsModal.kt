package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.game.GameArc
import com.example.game.OrganGameState
import com.example.game.ScarNodeId
import com.example.ui.theme.BleakTheme

/**
 * Clinical Dev Testing Console allowing testers to arbitrarily switch Arcs, unlock Excision
 * Milestones 0..6, toggle Surgical Scar Tree nodes, assign custom or preset resources for Arc 1
 * and Arc 2, spawn anomalies, fast-forward time, or wipe local save data.
 */
@Composable
fun DevToolsModal(
  state: OrganGameState,
  onSetActiveArc: (GameArc) -> Unit,
  onSetMilestone: (Int) -> Unit,
  onAssignArc1Resources: (ichor: Double?, stillness: Double?, availableScar: Long?) -> Unit,
  onSetAllScarNodes: (Boolean) -> Unit,
  onSpawnWallAnomaly: () -> Unit,
  onAssignArc2Resources: (
    leads: Double?,
    silence: Double?,
    complicity: Float?,
    completedChapters: Int?,
  ) -> Unit,
  onSpawnDeskAnomaly: () -> Unit,
  onAdvanceTime: (Double) -> Unit,
  onResetAll: () -> Unit,
  onDismiss: () -> Unit,
) {
  val colorScheme = MaterialTheme.colorScheme
  val atmosphere = BleakTheme.atmosphere

  var ichorInput by remember { mutableStateOf(state.ichorDisplay.toString()) }
  var stillnessInput by remember { mutableStateOf(state.stillnessDisplay.toString()) }
  var scarInput by remember { mutableStateOf(state.availableScarTissue.toString()) }
  var leadsInput by remember { mutableStateOf(state.arc2.leadsDisplay.toString()) }
  var silenceInput by remember { mutableStateOf(state.arc2.silenceDisplay.toString()) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).testTag("dev_tools_dialog"),
      shape = MaterialTheme.shapes.small,
      color = colorScheme.surface,
      border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.7f)),
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(R.string.dev_modal_title),
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.primary,
          )
          DevChip(
            label = stringResource(R.string.dev_modal_close),
            selected = false,
            testTag = "dev_close_button",
            onClick = onDismiss,
          )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = atmosphere.hairline)
        Spacer(modifier = Modifier.height(8.dp))

        Column(
          modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          // 1. Arc Switcher
          DevSectionHeader("ACTIVE ARC")
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            DevChip(
              label = "ARC 1",
              selected = state.activeArc == GameArc.ARC_1_CHAMBER,
              testTag = "dev_arc_1",
              onClick = { onSetActiveArc(GameArc.ARC_1_CHAMBER) },
              modifier = Modifier.weight(1f),
            )
            DevChip(
              label = "ARC 2",
              selected = state.activeArc == GameArc.ARC_2_INVESTIGATION,
              testTag = "dev_arc_2",
              onClick = { onSetActiveArc(GameArc.ARC_2_INVESTIGATION) },
              modifier = Modifier.weight(1f),
            )
            DevChip(
              label = "ARC 3",
              selected = state.activeArc == GameArc.ARC_3_CONVERGENCE,
              testTag = "dev_arc_3",
              onClick = { onSetActiveArc(GameArc.ARC_3_CONVERGENCE) },
              modifier = Modifier.weight(1f),
            )
          }

          // 2. Arbitrary Excision Milestone Unlock (0..6)
          DevSectionHeader("EXCISION MILESTONE (CURRENT: M${state.highestExcisionMilestone}/6)")
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            (0..6).forEach { m ->
              DevChip(
                label = "M$m",
                selected = state.highestExcisionMilestone == m,
                testTag = "dev_milestone_$m",
                onClick = { onSetMilestone(m) },
                modifier = Modifier.weight(1f),
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            DevChip(
              label = "UNLOCK ALL SCARS",
              selected = state.unlockedScarNodes.size == ScarNodeId.entries.size,
              testTag = "dev_unlock_all_scars",
              onClick = { onSetAllScarNodes(true) },
              modifier = Modifier.weight(1f),
            )
            DevChip(
              label = "CLEAR SCARS",
              selected = false,
              testTag = "dev_clear_scars",
              onClick = { onSetAllScarNodes(false) },
              modifier = Modifier.weight(1f),
            )
          }

          HorizontalDivider(color = atmosphere.hairline)

          // 3. Arc 1 Arbitrary Resource Assignment
          DevSectionHeader("ARC 1 RESOURCES (SET EXACT OR PRESET)")
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            OutlinedTextField(
              value = ichorInput,
              onValueChange = { ichorInput = it.filter { ch -> ch.isDigit() } },
              label = { Text("ICHOR", style = MaterialTheme.typography.labelSmall) },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f).testTag("dev_ichor_input"),
            )
            OutlinedTextField(
              value = stillnessInput,
              onValueChange = { stillnessInput = it.filter { ch -> ch.isDigit() } },
              label = { Text("STILL", style = MaterialTheme.typography.labelSmall) },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f).testTag("dev_stillness_input"),
            )
            OutlinedTextField(
              value = scarInput,
              onValueChange = { scarInput = it.filter { ch -> ch.isDigit() } },
              label = { Text("SCAR", style = MaterialTheme.typography.labelSmall) },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f).testTag("dev_scar_input"),
            )
          }
          DevChip(
            label = "APPLY ARC 1 RESOURCES",
            selected = true,
            testTag = "dev_apply_arc1_button",
            onClick = {
              onAssignArc1Resources(
                ichorInput.toDoubleOrNull(),
                stillnessInput.toDoubleOrNull(),
                scarInput.toLongOrNull(),
              )
            },
            modifier = Modifier.fillMaxWidth(),
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            DevChip(
              label = "+250K ICHOR",
              selected = false,
              testTag = "dev_add_250k_ichor",
              onClick = {
                val next = state.ichor + 250_000.0
                ichorInput = next.toLong().toString()
                onAssignArc1Resources(next, null, null)
              },
              modifier = Modifier.weight(1f),
            )
            DevChip(
              label = "+40B ICHOR",
              selected = false,
              testTag = "dev_add_5b_ichor",
              onClick = {
                val next = state.ichor + 40_000_000_000.0
                ichorInput = next.toLong().toString()
                onAssignArc1Resources(next, null, null)
              },
              modifier = Modifier.weight(1f),
            )
            DevChip(
              label = "+50 SCAR",
              selected = false,
              testTag = "dev_add_50_scar",
              onClick = {
                val next = state.availableScarTissue + 50L
                scarInput = next.toString()
                onAssignArc1Resources(null, null, next)
              },
              modifier = Modifier.weight(1f),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            DevChip(
              label = "SPAWN WALL ANOMALY",
              selected = false,
              testTag = "dev_spawn_wall_anomaly",
              onClick = onSpawnWallAnomaly,
              modifier = Modifier.weight(1f),
            )
            DevChip(
              label = "+60s TICK",
              selected = false,
              testTag = "dev_tick_60s",
              onClick = { onAdvanceTime(60.0) },
              modifier = Modifier.weight(1f),
            )
          }

          HorizontalDivider(color = atmosphere.hairline)

          // 4. Arc 2 Resources & Chapters
          DevSectionHeader("ARC 2 RESOURCES & CHAPTERS (CH ${state.arc2.completedChapters}/4)")
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            OutlinedTextField(
              value = leadsInput,
              onValueChange = { leadsInput = it.filter { ch -> ch.isDigit() } },
              label = { Text("LEADS", style = MaterialTheme.typography.labelSmall) },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f).testTag("dev_leads_input"),
            )
            OutlinedTextField(
              value = silenceInput,
              onValueChange = { silenceInput = it.filter { ch -> ch.isDigit() } },
              label = { Text("SILENCE", style = MaterialTheme.typography.labelSmall) },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f).testTag("dev_silence_input"),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            DevChip(
              label = "APPLY ARC 2",
              selected = true,
              testTag = "dev_apply_arc2_button",
              onClick = {
                onAssignArc2Resources(
                  leadsInput.toDoubleOrNull(),
                  silenceInput.toDoubleOrNull(),
                  null,
                  null,
                )
              },
              modifier = Modifier.weight(1f),
            )
            DevChip(
              label = "+250K LEADS & 300 SIL",
              selected = false,
              testTag = "dev_boost_arc2_button",
              onClick = {
                val l = state.arc2.leads + 250_000.0
                val s = state.arc2.silence + 300.0
                leadsInput = l.toLong().toString()
                silenceInput = s.toLong().toString()
                onAssignArc2Resources(l, s, null, null)
              },
              modifier = Modifier.weight(1f),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            (0..4).forEach { ch ->
              DevChip(
                label = "CH$ch",
                selected = state.arc2.completedChapters == ch,
                testTag = "dev_chapter_$ch",
                onClick = {
                  onAssignArc2Resources(null, null, (0.1f + ch * 0.225f).coerceAtMost(1f), ch)
                },
                modifier = Modifier.weight(1f),
              )
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            DevChip(
              label = "SPAWN DESK ANOMALY",
              selected = false,
              testTag = "dev_spawn_desk_anomaly",
              onClick = onSpawnDeskAnomaly,
              modifier = Modifier.weight(1f),
            )
            DevChip(
              label = "WIPE SAVE",
              selected = false,
              testTag = "dev_wipe_save_button",
              onClick = onResetAll,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DevSectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
private fun DevChip(
  label: String,
  selected: Boolean,
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
        .clickable(role = Role.Button, onClick = onClick),
    shape = MaterialTheme.shapes.extraSmall,
    color = if (selected) colorScheme.primaryContainer else colorScheme.surfaceVariant,
    border =
      BorderStroke(
        width = 1.dp,
        color = if (selected) colorScheme.primary else colorScheme.outlineVariant,
      ),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, letterSpacing = 0.6.sp),
      color = if (selected) colorScheme.primary else colorScheme.onBackground,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
    )
  }
}
