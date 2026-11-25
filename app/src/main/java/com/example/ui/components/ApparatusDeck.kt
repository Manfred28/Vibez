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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.game.ExcisionMilestone
import com.example.game.OrganDeckTab
import com.example.game.OrganGameState
import com.example.game.RepeatableScarGraft
import com.example.game.ScarNodeId
import com.example.game.SpecimenJar
import com.example.ui.theme.BleakTheme
import java.text.NumberFormat
import kotlin.math.ceil

@Composable
fun ApparatusDeck(
  state: OrganGameState,
  selectedTab: OrganDeckTab,
  onSelectTab: (OrganDeckTab) -> Unit,
  onBuyCreature: (CreatureType) -> Unit,
  onBottleJar: (SpecimenJar) -> Unit,
  onPerformGraft: () -> Unit,
  onPerformExcision: () -> Unit,
  onBuyMaxCreature: (CreatureType) -> Unit = {},
  onUnlockScarNode: (ScarNodeId) -> Unit = {},
  onBuyRepeatableScarGraft: (RepeatableScarGraft) -> Unit = {},
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
    if (state.canExcise || state.canBreachToArc2 || state.cycle > 0 || state.highestExcisionMilestone > 0) {
      add(OrganDeckTab.EXCISION)
    }
  }

  val activeTab = if (selectedTab in availableTabs) selectedTab else OrganDeckTab.APPARATUS
  var seenKeysByTab by remember {
    mutableStateOf(
      mapOf(activeTab to TabAlertEvaluator.arc1UnlockKeys(state, activeTab))
    )
  }

  val isFreshReset =
    state.cycle == 0 &&
      state.cycleLifetimeIchor == 0.0 &&
      state.scarTissue == 0L &&
      state.highestExcisionMilestone == 0
  LaunchedEffect(isFreshReset) {
    if (isFreshReset) {
      seenKeysByTab = mapOf(activeTab to TabAlertEvaluator.arc1UnlockKeys(state, activeTab))
    }
  }

  val activeTabKeys = TabAlertEvaluator.arc1UnlockKeys(state, activeTab)
  LaunchedEffect(activeTab, activeTabKeys) {
    val existing = seenKeysByTab[activeTab].orEmpty()
    if (!existing.containsAll(activeTabKeys)) {
      seenKeysByTab = seenKeysByTab + (activeTab to (existing + activeTabKeys))
    }
  }

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
          val isAlerting =
            TabAlertEvaluator.shouldAlertArc1Tab(
              state = state,
              tab = tab,
              activeTab = activeTab,
              seenKeysByTab = seenKeysByTab,
            )

          DeckTabCell(
            label = label,
            isSelected = isSelected,
            isAlerting = isAlerting,
            isExcisionAccent = tab == OrganDeckTab.EXCISION,
            tabTestTag = "deck_tab_${tab.name.lowercase()}",
            alertPipTestTag = "deck_tab_alert_${tab.name.lowercase()}",
            onClick = {
              val unlocked = TabAlertEvaluator.arc1UnlockKeys(state, tab)
              seenKeysByTab = seenKeysByTab + (tab to (seenKeysByTab[tab].orEmpty() + unlocked))
              onSelectTab(tab)
            },
            modifier = Modifier.weight(1f),
          )
        }
      }

      HorizontalDivider(color = atmosphere.hairline, thickness = 1.dp)

      Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        when (activeTab) {
          OrganDeckTab.APPARATUS ->
            OrganismsList(
              state = state,
              onBuyCreature = onBuyCreature,
              onBuyMaxCreature = onBuyMaxCreature,
              onPerformGraft = onPerformGraft,
              onPerformChoirLungGraft = onPerformChoirLungGraft,
            )
          OrganDeckTab.JARS_AND_GRAFT ->
            JarsList(
              state = state,
              onBottleJar = onBottleJar,
            )
          OrganDeckTab.SCAR_TREE ->
            ScarTreePanel(
              state = state,
              onUnlockNode = onUnlockScarNode,
              onBuyRepeatableGraft = onBuyRepeatableScarGraft,
            )
          OrganDeckTab.CHAMBER_RITES ->
            ChamberRitesPanel(
              state = state,
              onToggleDrainSeal = onToggleDrainSeal,
              onDredgeSump = onDredgeSump,
              onSetEchoDiversion = onSetEchoDiversion,
              onActivateValveClamp = onActivateValveClamp,
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
  onBuyMaxCreature: (CreatureType) -> Unit,
  onPerformGraft: () -> Unit,
  onPerformChoirLungGraft: () -> Unit,
) {
  val numberFormat = remember { NumberFormat.getIntegerInstance() }
  val colorScheme = MaterialTheme.colorScheme
  val atmosphere = BleakTheme.atmosphere

  val visibleCreatures = CreatureType.entries.filter { state.isCreatureUnlocked(it) }
  visibleCreatures.forEach { type ->
    OrganismSpecimenCard(
      type = type,
      state = state,
      onBuy = { onBuyCreature(type) },
      onBuyMax = { onBuyMaxCreature(type) },
    )
  }

  val hasTendril = state.isGraftUnlocked
  val hasLung = state.highestExcisionMilestone >= 4
  if (hasTendril || hasLung) {
    HorizontalDivider(color = atmosphere.hairline, thickness = 1.dp)
  }

  if (hasTendril) {
    val graftCost = state.graftStillnessCost
    val onCooldown = state.isGraftOnCooldown
    val actionLabel =
      if (onCooldown) {
        stringResource(R.string.graft_cooldown_format, ceil(state.graftCooldownRemainingSeconds).toInt())
      } else {
        stringResource(R.string.graft_action_cost, numberFormat.format(graftCost))
      }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      DeckActionRow(
        title =
          "${stringResource(R.string.graft_title)} ${
            stringResource(R.string.owned_count_format, state.graftedTendrils)
          }",
        subtitle = stringResource(R.string.graft_desc),
        actionLabel = actionLabel,
        enabled = state.canAffordGraft,
        accentAction = true,
        testTag = "perform_graft_button",
        leadingArtwork = {
          GraftArtworkEmblem(
            isLung = false,
            ready = state.canAffordGraft,
            onCooldown = onCooldown,
          )
        },
        onClick = onPerformGraft,
      )
      if (onCooldown) {
        val ratio =
          (state.graftCooldownRemainingSeconds / state.graftCooldownDurationSec)
            .toFloat()
            .coerceIn(0f, 1f)
        LinearProgressIndicator(
          progress = { 1f - ratio },
          modifier = Modifier.fillMaxWidth().height(3.dp).testTag("graft_cooldown_bar"),
          color = colorScheme.tertiary,
          trackColor = atmosphere.hairline,
          drawStopIndicator = {},
        )
      }
    }
  }

  if (hasLung) {
    val onLungCooldown = state.isChoirLungOnCooldown
    val lungActionLabel =
      if (onLungCooldown) {
        stringResource(
          R.string.graft_cooldown_format,
          ceil(state.choirLungCooldownRemainingSeconds).toInt(),
        )
      } else {
        stringResource(
          R.string.choir_lung_action_cost,
          numberFormat.format(state.choirLungStillnessCost),
        )
      }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      DeckActionRow(
        title =
          "${stringResource(R.string.choir_lung_title)} ${
            stringResource(R.string.owned_count_format, state.choirLungs)
          }",
        subtitle = stringResource(R.string.choir_lung_desc),
        actionLabel = lungActionLabel,
        enabled = state.canAffordChoirLung,
        accentAction = true,
        testTag = "perform_choir_lung_button",
        leadingArtwork = {
          GraftArtworkEmblem(
            isLung = true,
            ready = state.canAffordChoirLung,
            onCooldown = onLungCooldown,
          )
        },
        onClick = onPerformChoirLungGraft,
      )
      if (onLungCooldown) {
        val ratio =
          (state.choirLungCooldownRemainingSeconds / state.choirLungCooldownDurationSec)
            .toFloat()
            .coerceIn(0f, 1f)
        LinearProgressIndicator(
          progress = { 1f - ratio },
          modifier = Modifier.fillMaxWidth().height(3.dp).testTag("choir_lung_cooldown_bar"),
          color = colorScheme.tertiary,
          trackColor = atmosphere.hairline,
          drawStopIndicator = {},
        )
      }
    }
  }
}

@Composable
private fun JarsList(
  state: OrganGameState,
  onBottleJar: (SpecimenJar) -> Unit,
) {
  val numberFormat = remember { NumberFormat.getIntegerInstance() }
  val visibleJars = SpecimenJar.entries.filter { state.isJarUnlocked(it) }

  visibleJars.forEach { jar ->
    val owned = jar in state.ownedJars
    val cost = state.effectiveJarCost(jar)
    val canAfford = !owned && state.stillness >= cost

    val title =
      when (jar) {
        SpecimenJar.YESTERDAYS_DUST -> stringResource(R.string.jar_dust_name)
        SpecimenJar.UNSPENT_BREATH -> stringResource(R.string.jar_breath_name)
        SpecimenJar.ALMOST_A_NAME -> stringResource(R.string.jar_name_title)
        SpecimenJar.SALT_FROM_THE_GROUT -> stringResource(R.string.jar_grout_name)
        SpecimenJar.A_BORROWED_PULSE -> stringResource(R.string.jar_pulse_name)
        SpecimenJar.THE_UNCOUNTED_HOUR -> stringResource(R.string.jar_hour_name)
      }
    val effect =
      when (jar) {
        SpecimenJar.YESTERDAYS_DUST -> stringResource(R.string.jar_dust_effect)
        SpecimenJar.UNSPENT_BREATH -> stringResource(R.string.jar_breath_effect)
        SpecimenJar.ALMOST_A_NAME -> stringResource(R.string.jar_name_effect)
        SpecimenJar.SALT_FROM_THE_GROUT -> stringResource(R.string.jar_grout_effect)
        SpecimenJar.A_BORROWED_PULSE -> stringResource(R.string.jar_pulse_effect)
        SpecimenJar.THE_UNCOUNTED_HOUR -> stringResource(R.string.jar_hour_effect)
      }

    val actionLabel =
      if (owned) {
        stringResource(R.string.jar_sealed_badge)
      } else {
        "${stringResource(R.string.jar_bottle_action)} · ${
          stringResource(R.string.cost_stillness_format, numberFormat.format(cost.toLong()))
        }"
      }

    DeckActionRow(
      title = title,
      subtitle = effect,
      actionLabel = actionLabel,
      enabled = canAfford,
      accentAction = owned,
      testTag = "jar_${jar.id}",
      leadingArtwork = {
        JarArtworkEmblem(jar = jar, owned = owned, affordable = canAfford)
      },
      onClick = { onBottleJar(jar) },
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
  val atmosphere = BleakTheme.atmosphere
  val numberFormat = remember { NumberFormat.getIntegerInstance() }
  var armed by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier.fillMaxWidth().testTag("excision_panel"),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    val activeExcisionHeading =
      if (state.canClearNextMilestone && state.nextExcisionMilestone != null) {
        state.nextExcisionMilestone!!.code
      } else {
        stringResource(R.string.excision_title)
      }
    Text(
      text = activeExcisionHeading,
      style = MaterialTheme.typography.titleMedium,
      color = colorScheme.tertiary,
    )
    Text(
      text = stringResource(R.string.excision_summary),
      style = MaterialTheme.typography.bodyMedium,
      color = colorScheme.onSurfaceVariant,
    )

    // Relocated cycle / last severance / permanent scar resonance ledger (keeps top header uncluttered)
    if (state.cycle > 0 || state.scarTissue > 0L || state.highestExcisionMilestone > 0) {
      val lastExcisionName = ExcisionMilestone.byNumber(state.highestExcisionMilestone)?.code
      val cycleSummary =
        if (lastExcisionName != null) {
          stringResource(
            R.string.excision_cycle_summary,
            state.cycle,
            lastExcisionName,
            numberFormat.format(state.scarTissue),
          )
        } else {
          stringResource(
            R.string.excision_cycle_summary_unmarked,
            state.cycle,
            numberFormat.format(state.scarTissue),
          )
        }
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, atmosphere.hairline),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          Text(
            text = cycleSummary,
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.primary,
            modifier = Modifier.testTag("cycle_status"),
          )
          Text(
            text =
              stringResource(
                R.string.scar_passive_readout,
                formatMultiplier(state.scarMultiplier),
                formatMultiplier(state.scarStillnessMultiplier),
              ),
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("scar_passive_status"),
          )
        }
      }
    }

    // Next Scar Tissue visual progress indicator
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text =
          stringResource(
            R.string.excision_next_scar_format,
            numberFormat.format(state.cycleLifetimeIchorDisplay),
            numberFormat.format(state.nextBaseScarMassThreshold),
          ),
        style = MaterialTheme.typography.labelSmall,
        color = colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("next_scar_status"),
      )
      LinearProgressIndicator(
        progress = { state.nextScarProgress.coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(3.dp).testTag("next_scar_progress_bar"),
        color = colorScheme.primary,
        trackColor = atmosphere.hairline,
        drawStopIndicator = {},
      )
    }

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
      if (state.hasExcessMilestoneMass) {
        Text(
          text = stringResource(R.string.excision_seam_hold_note),
          style = MaterialTheme.typography.labelSmall,
          color = colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag("excision_seam_hold_note"),
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
      val armLabel =
        when {
          !state.canExcise -> stringResource(R.string.excision_action_arm)
          state.canClearNextMilestone && state.nextExcisionMilestone != null ->
            stringResource(
              R.string.excision_action_arm_milestone,
              state.nextExcisionMilestone!!.code,
              numberFormat.format(state.pendingScarTissue),
            )
          else ->
            stringResource(
              R.string.excision_action_arm_farm,
              numberFormat.format(state.pendingScarTissue),
            )
        }
      Surface(
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("excision_arm_button")
            .clickable(enabled = state.canExcise, role = Role.Button) { armed = true },
        shape = MaterialTheme.shapes.extraSmall,
        color =
          if (state.canExcise) colorScheme.tertiaryContainer
          else colorScheme.surfaceContainerLowest,
        border =
          BorderStroke(
            1.dp,
            if (state.canExcise) colorScheme.tertiary else colorScheme.outlineVariant,
          ),
      ) {
        Text(
          text = armLabel,
          style = MaterialTheme.typography.labelLarge,
          color =
            if (state.canExcise) colorScheme.onTertiaryContainer
            else colorScheme.onSurfaceVariant,
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

    if (state.canBreachToArc2) {
      HorizontalDivider(color = atmosphere.hairline)
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
  leadingArtwork: (@Composable () -> Unit)? = null,
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
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (leadingArtwork != null) {
        leadingArtwork()
        Spacer(modifier = Modifier.width(10.dp))
      }

      Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
        Text(
          text = title,
          style =
            MaterialTheme.typography.titleMedium.copy(
              fontSize = 12.5.sp,
              lineHeight = 17.sp,
              letterSpacing = 0.7.sp,
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
          style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.5.sp, letterSpacing = 0.7.sp),
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
