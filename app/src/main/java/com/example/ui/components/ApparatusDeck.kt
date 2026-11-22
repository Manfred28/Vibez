package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.CreatureType
import com.example.game.OrganDeckTab
import com.example.game.OrganGameState
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
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val atmosphere = BleakTheme.atmosphere

  val availableTabs = buildList {
    add(OrganDeckTab.APPARATUS)
    if (state.isJarsAndGraftUnlocked) add(OrganDeckTab.JARS_AND_GRAFT)
    if (state.canExcise) add(OrganDeckTab.EXCISION)
  }

  val activeTab = if (selectedTab in availableTabs) selectedTab else OrganDeckTab.APPARATUS

  Surface(
    modifier = modifier.fillMaxWidth().testTag("apparatus_deck"),
    shape = MaterialTheme.shapes.small,
    color = colorScheme.surface.copy(alpha = 0.90f),
    border = BorderStroke(1.dp, atmosphere.hairline),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Stark institutional tab strip
      Row(modifier = Modifier.fillMaxWidth()) {
        availableTabs.forEach { tab ->
          val isSelected = tab == activeTab
          val label =
            when (tab) {
              OrganDeckTab.APPARATUS -> stringResource(R.string.tab_organisms)
              OrganDeckTab.JARS_AND_GRAFT -> stringResource(R.string.tab_jars_graft)
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
              style = MaterialTheme.typography.labelMedium,
              color =
                when {
                  isExcisionTab -> colorScheme.tertiary
                  isSelected -> colorScheme.onBackground
                  else -> colorScheme.onSurfaceVariant
                },
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            )
          }
        }
      }

      HorizontalDivider(color = atmosphere.hairline, thickness = 1.dp)

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
          OrganDeckTab.EXCISION ->
            ExcisionRitualPanel(state = state, onPerformExcision = onPerformExcision)
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
  val numberFormat = remember { NumberFormat.getIntegerInstance() }
  val visibleCreatures =
    CreatureType.entries.filter { type ->
      state.cycle > 0 ||
        state.cycleLifetimeIchorDisplay >= type.unlockIchorThreshold ||
        state.creatureCount(type) > 0
    }

  visibleCreatures.forEach { type ->
    val count = state.creatureCount(type)
    val cost = state.creatureCost(type)
    val canAfford = state.ichor >= cost.toDouble()

    val title =
      when (type) {
        CreatureType.SUTURE_HAND -> stringResource(R.string.creature_suture_name)
        CreatureType.LEECH_CLERK -> stringResource(R.string.creature_leech_name)
        CreatureType.WEEPING_LAMP -> stringResource(R.string.creature_lamp_name)
      }
    val desc =
      when (type) {
        CreatureType.SUTURE_HAND -> stringResource(R.string.creature_suture_desc)
        CreatureType.LEECH_CLERK -> stringResource(R.string.creature_leech_desc)
        CreatureType.WEEPING_LAMP -> stringResource(R.string.creature_lamp_desc)
      }

    DeckActionRow(
      title = "$title ${stringResource(R.string.owned_count_format, count)}",
      subtitle = desc,
      actionLabel = stringResource(R.string.cost_ichor_format, numberFormat.format(cost)),
      enabled = canAfford,
      accentAction = false,
      testTag = "buy_${type.id}",
      leadingArt = { CreatureSpecimenPortrait(type = type, enabled = canAfford || count > 0) },
      onClick = { onBuyCreature(type) },
    )
  }
}

@Composable
private fun CreatureSpecimenPortrait(type: CreatureType, enabled: Boolean) {
  val colorScheme = MaterialTheme.colorScheme
  val borderColor = if (enabled) colorScheme.outline else colorScheme.outlineVariant
  val contentAlpha = if (enabled) 0.96f else 0.52f

  Surface(
    modifier = Modifier.size(48.dp),
    shape = MaterialTheme.shapes.extraSmall,
    color = colorScheme.background,
    border = BorderStroke(1.dp, borderColor),
  ) {
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
        Box(modifier = Modifier.fillMaxSize().alpha(contentAlpha).testTag("art_weeping_lamp")) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            drawLine(
              color = colorScheme.outline,
              start = Offset(cx, 0f),
              end = Offset(cx, size.height * 0.28f),
              strokeWidth = 1.4.dp.toPx(),
            )
            drawOval(
              brush =
                Brush.radialGradient(
                  colors = listOf(colorScheme.primary.copy(alpha = 0.55f), colorScheme.surfaceVariant),
                  center = Offset(cx, size.height * 0.48f),
                  radius = size.minDimension * 0.34f,
                ),
              topLeft = Offset(cx - 9.dp.toPx(), size.height * 0.26f),
              size = Size(18.dp.toPx(), 22.dp.toPx()),
            )
            drawOval(
              color = colorScheme.primary.copy(alpha = 0.78f),
              topLeft = Offset(cx - 9.dp.toPx(), size.height * 0.26f),
              size = Size(18.dp.toPx(), 22.dp.toPx()),
              style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
              color = colorScheme.primary,
              radius = 2.dp.toPx(),
              center = Offset(cx, size.height * 0.84f),
            )
          }
        }
    }
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
  leadingArt: (@Composable () -> Unit)? = null,
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
      if (leadingArt != null) {
        leadingArt()
        Spacer(modifier = Modifier.width(10.dp))
      }

      Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          color = if (enabled) colorScheme.onBackground else colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.9f else 0.6f),
        )
      }

      Text(
        text = actionLabel,
        style = MaterialTheme.typography.labelSmall,
        color =
          when {
            enabled && accentAction -> colorScheme.tertiary
            enabled -> colorScheme.primary
            else -> colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
          },
      )
    }
  }
}
