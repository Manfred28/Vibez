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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.CreatureType
import com.example.game.CultDossierLine
import com.example.game.OrganDeckTab
import com.example.game.OrganGameState
import com.example.game.ScarNodeId
import com.example.game.SpecimenJar
import com.example.ui.theme.BleakTheme
import java.text.NumberFormat

@Composable
fun ApparatusDeck(
  state: OrganGameState,
  selectedTab: OrganDeckTab,
  onSelectTab: (OrganDeckTab) -> Unit,
  onBuyCreature: (CreatureType) -> Unit,
  onBottleJar: (SpecimenJar) -> Unit,
  onPerformGraft: () -> Unit,
  onPerformExcision: () -> Unit,
  onUnlockScarNode: (ScarNodeId) -> Unit = {},
  onToggleDrainSeal: () -> Unit = {},
  onDredgeSump: () -> Unit = {},
  onSetEchoDiversion: (Int) -> Unit = {},
  onActivateValveClamp: () -> Unit = {},
  onPerformChoirLungGraft: () -> Unit = {},
  onOfferToThroat: (CultDossierLine) -> Unit = {},
  onPerformBreachToArc2: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val atmosphere = BleakTheme.atmosphere

  val availableTabs = buildList {
    add(OrganDeckTab.APPARATUS)
    if (state.isJarsAndGraftUnlocked) add(OrganDeckTab.JARS_AND_GRAFT)
    if (state.isScarTreeUnlocked) add(OrganDeckTab.SCAR_TREE)
    if (state.isChamberRitesUnlocked) add(OrganDeckTab.CHAMBER_RITES)
    if (state.canExcise || state.canBreachToArc2) add(OrganDeckTab.EXCISION)
  }

  val activeTab = if (selectedTab in availableTabs) selectedTab else OrganDeckTab.APPARATUS

  Surface(
    modifier = modifier.fillMaxWidth().testTag("apparatus_deck"),
    shape = MaterialTheme.shapes.small,
    color = colorScheme.surface.copy(alpha = 0.90f),
    border = BorderStroke(1.dp, atmosphere.hairline),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(modifier = Modifier.fillMaxWidth()) {
        availableTabs.forEach { tab ->
          val isSelected = tab == activeTab
          val label =
            when (tab) {
              OrganDeckTab.APPARATUS -> stringResource(R.string.tab_organisms)
              OrganDeckTab.JARS_AND_GRAFT -> stringResource(R.string.tab_jars_graft)
              OrganDeckTab.SCAR_TREE -> stringResource(R.string.tab_scar_tree)
              OrganDeckTab.CHAMBER_RITES -> stringResource(R.string.tab_rites)
              OrganDeckTab.EXCISION -> stringResource(R.string.tab_excision)
            }
          val isExcisionTab = tab == OrganDeckTab.EXCISION

          Surface(
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("deck_tab_${tab.name.lowercase()}")
                .clickable(role = Role.Tab) { onSelectTab(tab) },
            color =
              when {
                isSelected && isExcisionTab -> colorScheme.tertiaryContainer
                isSelected -> colorScheme.surfaceVariant
                else -> colorScheme.surface
              },
          ) {
            Text(
              text = label,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, letterSpacing = 0.5.sp),
              color =
                when {
                  isExcisionTab -> colorScheme.tertiary
                  isSelected -> colorScheme.onBackground
                  else -> colorScheme.onSurfaceVariant
                },
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp),
            )
          }
        }
      }

      HorizontalDivider(color = atmosphere.hairline, thickness = 1.dp)

      if (state.scarTissue > 0L || state.highestExcisionMilestone > 0) {
        Surface(
          modifier = Modifier.fillMaxWidth().testTag("prestige_passive_banner"),
          color = colorScheme.tertiaryContainer.copy(alpha = 0.45f),
        ) {
          Text(
            text =
              stringResource(
                R.string.scar_passive_readout,
                formatMultiplier(state.scarMultiplier),
                formatMultiplier(state.scarStillnessMultiplier),
              ),
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onBackground.copy(alpha = 0.92f),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
          )
        }
        HorizontalDivider(color = atmosphere.hairline, thickness = 1.dp)
      }

      Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        when (activeTab) {
          OrganDeckTab.APPARATUS ->
            OrganismsList(state = state, onBuyCreature = onBuyCreature)
          OrganDeckTab.JARS_AND_GRAFT ->
            JarsAndGraftList(
              state = state,
              onBottleJar = onBottleJar,
              onPerformGraft = onPerformGraft,
            )
          OrganDeckTab.SCAR_TREE ->
            ScarTreePanel(state = state, onUnlockNode = onUnlockScarNode)
          OrganDeckTab.CHAMBER_RITES ->
            ChamberRitesPanel(
              state = state,
              onToggleDrainSeal = onToggleDrainSeal,
              onDredgeSump = onDredgeSump,
              onSetEchoDiversion = onSetEchoDiversion,
              onActivateValveClamp = onActivateValveClamp,
              onPerformChoirLungGraft = onPerformChoirLungGraft,
              onOfferToThroat = onOfferToThroat,
            )
          OrganDeckTab.EXCISION ->
            ExcisionRitualPanel(
              state = state,
              onPerformExcision = onPerformExcision,
              onPerformBreachToArc2 = onPerformBreachToArc2,
            )
        }
      }
    }
  }
}

@Composable
private fun OrganismsList(
  state: OrganGameState,
  onBuyCreature: (CreatureType) -> Unit,
) {
  val visibleCreatures = CreatureType.entries.filter { state.isCreatureUnlocked(it) }
  visibleCreatures.forEach { type ->
    OrganismSpecimenCard(
      type = type,
      state = state,
      onBuy = { onBuyCreature(type) },
    )
  }
}

@Composable
private fun JarsAndGraftList(
  state: OrganGameState,
  onBottleJar: (SpecimenJar) -> Unit,
  onPerformGraft: () -> Unit,
) {
  val numberFormat = remember { NumberFormat.getIntegerInstance() }

  SpecimenJar.entries.forEach { jar ->
    val owned = jar in state.ownedJars
    val canAfford = !owned && state.stillness >= jar.stillnessCost

    val title =
      when (jar) {
        SpecimenJar.YESTERDAYS_DUST -> stringResource(R.string.jar_dust_name)
        SpecimenJar.UNSPENT_BREATH -> stringResource(R.string.jar_breath_name)
        SpecimenJar.ALMOST_A_NAME -> stringResource(R.string.jar_name_title)
      }
    val effect =
      when (jar) {
        SpecimenJar.YESTERDAYS_DUST -> stringResource(R.string.jar_dust_effect)
        SpecimenJar.UNSPENT_BREATH -> stringResource(R.string.jar_breath_effect)
        SpecimenJar.ALMOST_A_NAME -> stringResource(R.string.jar_name_effect)
      }

    val actionLabel =
      if (owned) {
        stringResource(R.string.jar_sealed_badge)
      } else {
        "${stringResource(R.string.jar_bottle_action)} · ${
          stringResource(R.string.cost_stillness_format, numberFormat.format(jar.stillnessCost.toLong()))
        }"
      }

    DeckActionRow(
      title = title,
      subtitle = effect,
      actionLabel = actionLabel,
      enabled = canAfford,
      accentAction = owned,
      testTag = "jar_${jar.id}",
      onClick = { onBottleJar(jar) },
    )
  }

  HorizontalDivider(color = BleakTheme.atmosphere.hairline, thickness = 1.dp)

  if (state.isGraftUnlocked) {
    DeckActionRow(
      title =
        "${stringResource(R.string.graft_title)} ${
          stringResource(R.string.owned_count_format, state.graftedTendrils)
        }",
      subtitle = stringResource(R.string.graft_desc),
      actionLabel = stringResource(R.string.graft_action),
      enabled = state.canAffordGraft,
      accentAction = true,
      testTag = "perform_graft_button",
      onClick = onPerformGraft,
    )
  } else {
    Text(
      text = stringResource(R.string.graft_locked_hint),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(vertical = 4.dp),
    )
  }
}

@Composable
private fun ExcisionRitualPanel(
  state: OrganGameState,
  onPerformExcision: () -> Unit,
  onPerformBreachToArc2: () -> Unit,
) {
  val colorScheme = MaterialTheme.colorScheme
  val numberFormat = remember { NumberFormat.getIntegerInstance() }
  var armed by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier.fillMaxWidth().testTag("excision_panel"),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = stringResource(R.string.excision_title),
      style = MaterialTheme.typography.titleMedium,
      color = colorScheme.tertiary,
    )
    Text(
      text = stringResource(R.string.excision_summary),
      style = MaterialTheme.typography.bodyMedium,
      color = colorScheme.onSurfaceVariant,
    )

    state.nextExcisionMilestone?.let { next ->
      Text(
        text =
          stringResource(
            R.string.excision_milestone_progress,
            next.code,
            numberFormat.format(state.cycleLifetimeIchorDisplay),
            numberFormat.format(next.massThreshold),
          ),
        style = MaterialTheme.typography.labelSmall,
        color = if (state.canClearNextMilestone) colorScheme.primary else colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("excision_milestone_text"),
      )
      if (state.canClearNextMilestone) {
        Text(
          text = stringResource(R.string.excision_milestone_cleared_banner),
          style = MaterialTheme.typography.labelSmall,
          color = colorScheme.primary,
        )
      }
    }

    Text(
      text =
        stringResource(
          R.string.excision_reward_format,
          numberFormat.format(state.pendingScarTissue),
        ),
      style = MaterialTheme.typography.labelLarge,
      color = colorScheme.onBackground,
      modifier = Modifier.testTag("excision_reward_text"),
    )

    Spacer(modifier = Modifier.height(2.dp))

    if (!armed) {
      Surface(
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("excision_arm_button")
            .clickable(enabled = state.canExcise, role = Role.Button) { armed = true },
        shape = MaterialTheme.shapes.extraSmall,
        color = colorScheme.tertiaryContainer,
        border = BorderStroke(1.dp, colorScheme.tertiary),
      ) {
        Text(
          text = stringResource(R.string.excision_action_arm),
          style = MaterialTheme.typography.labelLarge,
          color = colorScheme.onTertiaryContainer,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
      }
    } else {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Surface(
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("excision_cancel_button")
              .clickable(role = Role.Button) { armed = false },
          shape = MaterialTheme.shapes.extraSmall,
          color = colorScheme.surfaceVariant,
          border = BorderStroke(1.dp, colorScheme.outline),
        ) {
          Text(
            text = stringResource(R.string.excision_action_cancel),
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
          )
        }
        Surface(
          modifier =
            Modifier.weight(2f)
              .minimumInteractiveComponentSize()
              .testTag("excision_confirm_button")
              .clickable(enabled = state.canExcise, role = Role.Button) {
                armed = false
                onPerformExcision()
              },
          shape = MaterialTheme.shapes.extraSmall,
          color = colorScheme.errorContainer,
          border = BorderStroke(1.5.dp, colorScheme.error),
        ) {
          Text(
            text = stringResource(R.string.excision_action_confirm),
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
          )
        }
      }
    }

    // Excision VI Breach Ritual into Arc 2
    if (state.canBreachToArc2) {
      HorizontalDivider(color = BleakTheme.atmosphere.hairline)
      Text(
        text = stringResource(R.string.excision_breach_title),
        style = MaterialTheme.typography.titleMedium,
        color = colorScheme.primary,
      )
      Text(
        text = stringResource(R.string.excision_breach_desc),
        style = MaterialTheme.typography.bodySmall,
        color = colorScheme.onSurfaceVariant,
      )
      Surface(
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("breach_to_arc2_button")
            .clickable(role = Role.Button, onClick = onPerformBreachToArc2),
        shape = MaterialTheme.shapes.extraSmall,
        color = colorScheme.primaryContainer,
        border = BorderStroke(1.5.dp, colorScheme.primary),
      ) {
        Text(
          text = stringResource(R.string.excision_breach_action),
          style = MaterialTheme.typography.labelLarge,
          color = colorScheme.primary,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        )
      }
    }
  }
}

@Composable
private fun DeckActionRow(
  title: String,
  subtitle: String,
  actionLabel: String,
  enabled: Boolean,
  accentAction: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  val colorScheme = MaterialTheme.colorScheme

  Surface(
    modifier =
      Modifier.fillMaxWidth()
        .minimumInteractiveComponentSize()
        .testTag(testTag)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    shape = MaterialTheme.shapes.extraSmall,
    color =
      when {
        enabled && accentAction -> colorScheme.tertiaryContainer.copy(alpha = 0.7f)
        enabled -> colorScheme.surfaceVariant
        else -> colorScheme.surfaceContainerLowest
      },
    border =
      BorderStroke(
        width = 1.dp,
        color =
          when {
            enabled && accentAction -> colorScheme.tertiary
            enabled -> colorScheme.outline
            else -> colorScheme.outlineVariant
          },
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
        Text(
          text = title,
          style =
            MaterialTheme.typography.titleMedium.copy(
              fontSize = 13.sp,
              lineHeight = 18.sp,
              letterSpacing = 0.8.sp,
            ),
          color = if (enabled) colorScheme.onBackground else colorScheme.onBackground.copy(alpha = 0.82f),
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color =
            if (enabled) colorScheme.onBackground.copy(alpha = 0.90f)
            else colorScheme.onSurfaceVariant,
        )
      }

      Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color =
          when {
            enabled && accentAction -> colorScheme.tertiaryContainer
            enabled -> colorScheme.background.copy(alpha = 0.72f)
            else -> colorScheme.surface
          },
        border =
          BorderStroke(
            width = 1.dp,
            color =
              when {
                enabled && accentAction -> colorScheme.tertiary
                enabled -> colorScheme.primary.copy(alpha = 0.55f)
                else -> colorScheme.outlineVariant
              },
          ),
      ) {
        Text(
          text = actionLabel,
          style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.8.sp),
          color =
            when {
              enabled && accentAction -> colorScheme.onTertiaryContainer
              enabled -> colorScheme.primary
              else -> colorScheme.onSurfaceVariant
            },
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        )
      }
    }
  }
}
