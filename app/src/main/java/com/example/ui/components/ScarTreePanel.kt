package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.OrganGameState
import com.example.game.RepeatableScarGraft
import com.example.game.ScarNodeId
import com.example.ui.theme.BleakTheme
import java.text.NumberFormat

/**
 * Interactive 5-tier 3-branch Scar Tissue Surgical Skill Tree + Repeatable Surgical Grafts
 * unlocked after Excision I. Renders a stitched suture chart connecting Root -> Suture /
 * Stillness / Marrow (5 tiers each), paired with a cryptic inspection card and repeatable sinks.
 */
@Composable
fun ScarTreePanel(
  state: OrganGameState,
  onUnlockNode: (ScarNodeId) -> Unit,
  onBuyRepeatableGraft: (RepeatableScarGraft) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val atmosphere = BleakTheme.atmosphere
  val nf = remember { NumberFormat.getIntegerInstance() }
  var selectedNode by remember { mutableStateOf(ScarNodeId.FIRST_INCISION) }

  Column(
    modifier = modifier.fillMaxWidth().testTag("scar_tree_panel"),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.scar_tree_title),
        style = MaterialTheme.typography.titleMedium,
        color = colorScheme.onBackground,
      )
      Text(
        text = stringResource(R.string.scar_tree_available, nf.format(state.availableScarTissue)),
        style = MaterialTheme.typography.labelMedium,
        color = colorScheme.primary,
        modifier = Modifier.testTag("scar_tree_available_text"),
      )
    }

    Text(
      text =
        "${stringResource(R.string.excision_cycle_summary, state.cycle, state.highestExcisionMilestone, nf.format(state.scarTissue))} · ${
          stringResource(R.string.scar_passive_readout, formatMultiplier(state.scarMultiplier), formatMultiplier(state.scarStillnessMultiplier))
        }",
      style = MaterialTheme.typography.bodySmall,
      color = colorScheme.onSurfaceVariant,
      modifier = Modifier.testTag("scar_wallet_status"),
    )

    // Surgical suture graph: Root -> 3 branches x 5 tiers
    Box(modifier = Modifier.fillMaxWidth()) {
      Canvas(modifier = Modifier.matchParentSize()) {
        val threadColor = colorScheme.outline.copy(alpha = 0.65f)
        val stitchedColor = colorScheme.tertiary.copy(alpha = 0.85f)
        val dashed = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)

        val rootBottom = Offset(size.width * 0.5f, 44.dp.toPx())
        val colFractions = listOf(1f / 6f, 0.5f, 5f / 6f)
        val branchRoots =
          listOf(
            ScarNodeId.CALLOUSED_PALM,
            ScarNodeId.SHORTER_SILENCE,
            ScarNodeId.SOFTENED_MORTAR,
          )
        val tier1TopY = 78.dp.toPx()

        colFractions.forEachIndexed { idx, xFrac ->
          val target = Offset(size.width * xFrac, tier1TopY)
          val active = state.hasNode(branchRoots[idx])
          drawLine(
            color = if (active) stitchedColor else threadColor,
            start = rootBottom,
            end = target,
            strokeWidth = if (active) 2.dp.toPx() else 1.2.dp.toPx(),
            pathEffect = if (active) null else dashed,
          )
          // Vertical spine connecting Tier 1 -> Tier 5
          drawLine(
            color = threadColor.copy(alpha = 0.55f),
            start = target,
            end = Offset(size.width * xFrac, size.height - 20.dp.toPx()),
            strokeWidth = 1.2.dp.toPx(),
            pathEffect = dashed,
          )
        }
      }

      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        ScarNodeTile(
          node = ScarNodeId.FIRST_INCISION,
          state = state,
          selected = selectedNode == ScarNodeId.FIRST_INCISION,
          onSelect = { selectedNode = ScarNodeId.FIRST_INCISION },
          modifier = Modifier.fillMaxWidth(0.68f),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          BranchColumn(
            header = stringResource(R.string.scar_branch_suture),
            nodes =
              listOf(
                ScarNodeId.CALLOUSED_PALM,
                ScarNodeId.TWIN_STITCH,
                ScarNodeId.COLONY_INERTIA,
                ScarNodeId.MEMBRANE_RECALL,
                ScarNodeId.PERPETUAL_SEAM,
              ),
            state = state,
            selectedNode = selectedNode,
            onSelect = { selectedNode = it },
            modifier = Modifier.weight(1f),
          )
          BranchColumn(
            header = stringResource(R.string.scar_branch_stillness),
            nodes =
              listOf(
                ScarNodeId.SHORTER_SILENCE,
                ScarNodeId.WAX_SEALED_MEMORY,
                ScarNodeId.DEEP_WEEPING,
                ScarNodeId.HOLLOW_LUNG_CHOIR,
                ScarNodeId.CATATONIC_TIDE,
              ),
            state = state,
            selectedNode = selectedNode,
            onSelect = { selectedNode = it },
            modifier = Modifier.weight(1f),
          )
          BranchColumn(
            header = stringResource(R.string.scar_branch_marrow),
            nodes =
              listOf(
                ScarNodeId.SOFTENED_MORTAR,
                ScarNodeId.AUTO_TENDRIL_SUTURE,
                ScarNodeId.ECHO_OSSUARY,
                ScarNodeId.BLOOM_INCISION,
                ScarNodeId.BONE_RESONANCE,
              ),
            state = state,
            selectedNode = selectedNode,
            onSelect = { selectedNode = it },
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    HorizontalDivider(color = atmosphere.hairline, thickness = 1.dp)

    // Selected surgical node inspection & suture action card
    val owned = state.hasNode(selectedNode)
    val canUnlock = state.canUnlockNode(selectedNode)
    val prereqMissing =
      selectedNode.prerequisite != null && !state.hasNode(selectedNode.prerequisite!!)

    Surface(
      modifier = Modifier.fillMaxWidth().testTag("scar_node_detail_card"),
      shape = MaterialTheme.shapes.extraSmall,
      color = colorScheme.surfaceVariant.copy(alpha = 0.85f),
      border =
        BorderStroke(
          width = 1.dp,
          color =
            when {
              owned -> colorScheme.tertiary
              canUnlock -> colorScheme.primary
              else -> colorScheme.outlineVariant
            },
        ),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = scarNodeName(selectedNode),
          style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
          color = colorScheme.onBackground,
        )
        Text(
          text = scarNodeDesc(selectedNode),
          style = MaterialTheme.typography.bodySmall,
          color = colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        val actionLabel =
          when {
            owned -> stringResource(R.string.scar_status_sutured)
            prereqMissing -> stringResource(R.string.scar_status_locked)
            else -> stringResource(R.string.scar_action_suture, nf.format(selectedNode.cost))
          }
        Surface(
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("suture_selected_node_button")
              .clickable(enabled = canUnlock, role = Role.Button) { onUnlockNode(selectedNode) },
          shape = MaterialTheme.shapes.extraSmall,
          color =
            when {
              owned -> colorScheme.tertiaryContainer.copy(alpha = 0.6f)
              canUnlock -> colorScheme.primaryContainer
              else -> colorScheme.surface
            },
          border =
            BorderStroke(
              width = 1.dp,
              color =
                when {
                  owned -> colorScheme.tertiary
                  canUnlock -> colorScheme.primary
                  else -> colorScheme.outlineVariant
                },
            ),
        ) {
          Text(
            text = actionLabel,
            style = MaterialTheme.typography.labelLarge,
            color =
              when {
                owned -> colorScheme.onTertiaryContainer
                canUnlock -> colorScheme.primary
                else -> colorScheme.onSurfaceVariant
              },
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
          )
        }
      }
    }

    HorizontalDivider(color = atmosphere.hairline, thickness = 1.dp)

    // Repeatable Surgical Grafts Tray
    Column(
      modifier = Modifier.fillMaxWidth().testTag("repeatable_scar_grafts_section"),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
        text = stringResource(R.string.scar_repeatables_title),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
        color = colorScheme.onSurfaceVariant,
      )

      if (!state.isRepeatableGraftsUnlocked) {
        Text(
          text = stringResource(R.string.scar_repeatables_locked),
          style = MaterialTheme.typography.bodySmall,
          color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
          modifier = Modifier.testTag("repeatable_grafts_locked_hint"),
        )
      } else {
        RepeatableScarGraft.entries.forEach { graft ->
          val rank = state.repeatableRank(graft)
          val cost = state.repeatableCost(graft)
          val canBuy = state.canBuyRepeatableGraft(graft)

          Surface(
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("repeatable_graft_${graft.id}")
                .clickable(enabled = canBuy, role = Role.Button) { onBuyRepeatableGraft(graft) },
            shape = MaterialTheme.shapes.extraSmall,
            color =
              if (canBuy) colorScheme.surfaceVariant
              else colorScheme.surfaceContainerLowest,
            border =
              BorderStroke(
                width = 1.dp,
                color = if (canBuy) colorScheme.primary else colorScheme.outlineVariant,
              ),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                  text =
                    "${repeatableGraftName(graft)} · ${
                      stringResource(R.string.scar_repeatable_rank_format, rank)
                    }",
                  style = MaterialTheme.typography.labelMedium,
                  color =
                    if (canBuy) colorScheme.onBackground
                    else colorScheme.onBackground.copy(alpha = 0.8f),
                )
                Text(
                  text = repeatableGraftDesc(graft),
                  style = MaterialTheme.typography.bodySmall,
                  color = colorScheme.onSurfaceVariant,
                )
              }
              Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color =
                  if (canBuy) colorScheme.primaryContainer
                  else colorScheme.surface,
                border =
                  BorderStroke(
                    width = 1.dp,
                    color = if (canBuy) colorScheme.primary else colorScheme.outlineVariant,
                  ),
              ) {
                Text(
                  text = stringResource(R.string.scar_repeatable_action_format, nf.format(cost)),
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                  color = if (canBuy) colorScheme.primary else colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BranchColumn(
  header: String,
  nodes: List<ScarNodeId>,
  state: OrganGameState,
  selectedNode: ScarNodeId,
  onSelect: (ScarNodeId) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = header,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.sp),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
    )
    nodes.forEach { node ->
      ScarNodeTile(
        node = node,
        state = state,
        selected = selectedNode == node,
        onSelect = { onSelect(node) },
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun ScarNodeTile(
  node: ScarNodeId,
  state: OrganGameState,
  selected: Boolean,
  onSelect: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val owned = state.hasNode(node)
  val affordable = state.canUnlockNode(node)

  Surface(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .testTag("scar_node_${node.id}")
        .clickable(role = Role.Button, onClick = onSelect),
    shape = MaterialTheme.shapes.extraSmall,
    color =
      when {
        owned -> colorScheme.tertiaryContainer.copy(alpha = 0.85f)
        affordable -> colorScheme.primaryContainer.copy(alpha = 0.72f)
        else -> colorScheme.surfaceContainerLowest
      },
    border =
      BorderStroke(
        width = if (selected) 1.5.dp else 1.dp,
        color =
          when {
            selected -> colorScheme.onBackground
            owned -> colorScheme.tertiary
            affordable -> colorScheme.primary
            else -> colorScheme.outlineVariant
          },
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = scarNodeName(node),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.4.sp),
        color = if (owned || affordable) colorScheme.onBackground else colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = if (owned) "✓" else "${node.cost} SCAR",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
        color =
          when {
            owned -> colorScheme.tertiary
            affordable -> colorScheme.primary
            else -> colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
          },
      )
    }
  }
}

@Composable
private fun scarNodeName(node: ScarNodeId): String =
  when (node) {
    ScarNodeId.FIRST_INCISION -> stringResource(R.string.scar_node_first_incision_name)
    ScarNodeId.CALLOUSED_PALM -> stringResource(R.string.scar_node_calloused_palm_name)
    ScarNodeId.TWIN_STITCH -> stringResource(R.string.scar_node_twin_stitch_name)
    ScarNodeId.COLONY_INERTIA -> stringResource(R.string.scar_node_colony_inertia_name)
    ScarNodeId.MEMBRANE_RECALL -> stringResource(R.string.scar_node_membrane_recall_name)
    ScarNodeId.PERPETUAL_SEAM -> stringResource(R.string.scar_node_perpetual_seam_name)
    ScarNodeId.SHORTER_SILENCE -> stringResource(R.string.scar_node_shorter_silence_name)
    ScarNodeId.WAX_SEALED_MEMORY -> stringResource(R.string.scar_node_wax_sealed_memory_name)
    ScarNodeId.DEEP_WEEPING -> stringResource(R.string.scar_node_deep_weeping_name)
    ScarNodeId.HOLLOW_LUNG_CHOIR -> stringResource(R.string.scar_node_hollow_lung_choir_name)
    ScarNodeId.CATATONIC_TIDE -> stringResource(R.string.scar_node_catatonic_tide_name)
    ScarNodeId.SOFTENED_MORTAR -> stringResource(R.string.scar_node_softened_mortar_name)
    ScarNodeId.AUTO_TENDRIL_SUTURE -> stringResource(R.string.scar_node_auto_tendril_suture_name)
    ScarNodeId.ECHO_OSSUARY -> stringResource(R.string.scar_node_echo_ossuary_name)
    ScarNodeId.BLOOM_INCISION -> stringResource(R.string.scar_node_bloom_incision_name)
    ScarNodeId.BONE_RESONANCE -> stringResource(R.string.scar_node_bone_resonance_name)
  }

@Composable
private fun scarNodeDesc(node: ScarNodeId): String =
  when (node) {
    ScarNodeId.FIRST_INCISION -> stringResource(R.string.scar_node_first_incision_desc)
    ScarNodeId.CALLOUSED_PALM -> stringResource(R.string.scar_node_calloused_palm_desc)
    ScarNodeId.TWIN_STITCH -> stringResource(R.string.scar_node_twin_stitch_desc)
    ScarNodeId.COLONY_INERTIA -> stringResource(R.string.scar_node_colony_inertia_desc)
    ScarNodeId.MEMBRANE_RECALL -> stringResource(R.string.scar_node_membrane_recall_desc)
    ScarNodeId.PERPETUAL_SEAM -> stringResource(R.string.scar_node_perpetual_seam_desc)
    ScarNodeId.SHORTER_SILENCE -> stringResource(R.string.scar_node_shorter_silence_desc)
    ScarNodeId.WAX_SEALED_MEMORY -> stringResource(R.string.scar_node_wax_sealed_memory_desc)
    ScarNodeId.DEEP_WEEPING -> stringResource(R.string.scar_node_deep_weeping_desc)
    ScarNodeId.HOLLOW_LUNG_CHOIR -> stringResource(R.string.scar_node_hollow_lung_choir_desc)
    ScarNodeId.CATATONIC_TIDE -> stringResource(R.string.scar_node_catatonic_tide_desc)
    ScarNodeId.SOFTENED_MORTAR -> stringResource(R.string.scar_node_softened_mortar_desc)
    ScarNodeId.AUTO_TENDRIL_SUTURE -> stringResource(R.string.scar_node_auto_tendril_suture_desc)
    ScarNodeId.ECHO_OSSUARY -> stringResource(R.string.scar_node_echo_ossuary_desc)
    ScarNodeId.BLOOM_INCISION -> stringResource(R.string.scar_node_bloom_incision_desc)
    ScarNodeId.BONE_RESONANCE -> stringResource(R.string.scar_node_bone_resonance_desc)
  }

@Composable
private fun repeatableGraftName(graft: RepeatableScarGraft): String =
  when (graft) {
    RepeatableScarGraft.KELOID_THICKENING -> stringResource(R.string.scar_repeatable_keloid_name)
    RepeatableScarGraft.SEDIMENT_WELL -> stringResource(R.string.scar_repeatable_sediment_name)
    RepeatableScarGraft.SCAR_HARVEST_HOOK -> stringResource(R.string.scar_repeatable_hook_name)
  }

@Composable
private fun repeatableGraftDesc(graft: RepeatableScarGraft): String =
  when (graft) {
    RepeatableScarGraft.KELOID_THICKENING -> stringResource(R.string.scar_repeatable_keloid_desc)
    RepeatableScarGraft.SEDIMENT_WELL -> stringResource(R.string.scar_repeatable_sediment_desc)
    RepeatableScarGraft.SCAR_HARVEST_HOOK -> stringResource(R.string.scar_repeatable_hook_desc)
  }
