package com.example.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.CaseDeduction
import com.example.game.CreatureType
import com.example.game.CultDossierLine
import com.example.game.InvestigatorDeckTab
import com.example.game.OrganDeckTab
import com.example.game.OrganGameState
import com.example.game.RepeatableScarGraft
import com.example.game.ScarNodeId
import com.example.game.SpecimenJar

/**
 * Pure deterministic evaluator for unseen unlocks and off-tab action readiness across Arc 1 and Arc 2.
 * Ensures newly unlocked tabs/items and actionable rites pulse deep opaque clot-red until inspected.
 */
object TabAlertEvaluator {
  fun arc1UnlockKeys(state: OrganGameState, tab: OrganDeckTab): Set<String> =
    when (tab) {
      OrganDeckTab.APPARATUS ->
        buildSet {
          CreatureType.entries
            .filter { state.isCreatureUnlocked(it) }
            .forEach { add("creature_${it.id}") }
          if (state.isGraftUnlocked) add("graft_tendril")
          if (state.isColonyProliferationUnlocked) add("colony_proliferation")
          if (state.highestExcisionMilestone >= 4) add("graft_choir_lung")
        }
      OrganDeckTab.JARS_AND_GRAFT ->
        SpecimenJar.entries
          .filter { state.isJarUnlocked(it) }
          .map { "jar_${it.id}" }
          .toSet()
      OrganDeckTab.SCAR_TREE ->
        buildSet {
          if (state.isScarTreeUnlocked) add("scar_tree")
          if (state.isRepeatableGraftsUnlocked) add("scar_repeatables")
          val hasAffordableScar =
            state.isScarTreeUnlocked &&
              (ScarNodeId.entries.any { state.canUnlockNode(it) } ||
                RepeatableScarGraft.entries.any { state.canBuyRepeatableGraft(it) })
          if (hasAffordableScar) add("scar_budget_${state.scarTissue}")
        }
      OrganDeckTab.CHAMBER_RITES ->
        buildSet {
          if (state.highestExcisionMilestone >= 2) add("rite_m2_drain")
          if (state.highestExcisionMilestone >= 3) add("rite_m3_echo")
          if (state.highestExcisionMilestone >= 4) add("rite_m4_clamp")
          if (state.highestExcisionMilestone >= 5) add("rite_m5_throat")
        }
      OrganDeckTab.EXCISION ->
        buildSet {
          if (state.canExcise || state.canBreachToArc2 || state.cycle > 0 || state.highestExcisionMilestone > 0) {
            add("excision_tab")
          }
          if (state.canExcise) add("excision_ready_c${state.cycle}")
          if (state.canClearNextMilestone) {
            state.nextExcisionMilestone?.let { add("milestone_${it.number}_ready_c${state.cycle}") }
          }
          if (state.canBreachToArc2) add("breach_arc2")
        }
    }

  fun isArc1TabReady(state: OrganGameState, tab: OrganDeckTab): Boolean =
    when (tab) {
      OrganDeckTab.APPARATUS -> state.canAffordGraft || state.canAffordChoirLung
      OrganDeckTab.JARS_AND_GRAFT ->
        SpecimenJar.entries.any { jar ->
          state.isJarUnlocked(jar) &&
            jar !in state.ownedJars &&
            state.stillness >= state.effectiveJarCost(jar)
        }
      OrganDeckTab.SCAR_TREE -> false
      OrganDeckTab.CHAMBER_RITES ->
        state.canDredgeSump ||
          CultDossierLine.entries.any { state.canOfferToThroat(it) }
      OrganDeckTab.EXCISION -> false
    }

  fun shouldAlertArc1Tab(
    state: OrganGameState,
    tab: OrganDeckTab,
    activeTab: OrganDeckTab,
    seenKeysByTab: Map<OrganDeckTab, Set<String>>,
  ): Boolean {
    if (tab == activeTab) return false
    val currentKeys = arc1UnlockKeys(state, tab)
    val seen = seenKeysByTab[tab].orEmpty()
    val hasUnseen = currentKeys.any { it !in seen }
    return hasUnseen || isArc1TabReady(state, tab)
  }

  fun arc2UnlockKeys(state: OrganGameState, tab: InvestigatorDeckTab): Set<String> {
    val desk = state.arc2
    return when (tab) {
      InvestigatorDeckTab.CONTACTS -> setOf("arc2_contacts")
      InvestigatorDeckTab.DEDUCTIONS -> setOf("arc2_deductions")
      InvestigatorDeckTab.EVIDENCE_BOARD ->
        buildSet {
          desk.nextChapter?.let { add("arc2_ch_${it.number}") }
          if (desk.fileZeroRevealed) add("arc2_file_zero")
        }
    }
  }

  fun isArc2TabReady(state: OrganGameState, tab: InvestigatorDeckTab): Boolean {
    val desk = state.arc2
    return when (tab) {
      InvestigatorDeckTab.CONTACTS -> false
      InvestigatorDeckTab.DEDUCTIONS ->
        CaseDeduction.entries.any { ded ->
          ded !in desk.ownedDeductions && desk.silence >= ded.silenceCost
        }
      InvestigatorDeckTab.EVIDENCE_BOARD -> desk.canConnectNextChapter
    }
  }

  fun shouldAlertArc2Tab(
    state: OrganGameState,
    tab: InvestigatorDeckTab,
    activeTab: InvestigatorDeckTab,
    seenKeysByTab: Map<InvestigatorDeckTab, Set<String>>,
  ): Boolean {
    if (tab == activeTab) return false
    val currentKeys = arc2UnlockKeys(state, tab)
    val seen = seenKeysByTab[tab].orEmpty()
    val hasUnseen = currentKeys.any { it !in seen }
    return hasUnseen || isArc2TabReady(state, tab)
  }
}

private val ClotPulseBgLow = Color(0xFF341315)
private val ClotPulseBgHigh = Color(0xFF611E22)
private val ClotPulseBorderLow = Color(0xFF6E2C2F)
private val ClotPulseBorderHigh = Color(0xFFB2494C)
private val ClotPulseText = Color(0xFFE8DCD8)
private val ClotPulsePip = Color(0xFFD05C60)

/**
 * Shared deck tab cell supporting a deep dried-clot red opaque pulse for unseen unlocks or ready actions.
 */
@Composable
internal fun DeckTabCell(
  label: String,
  isSelected: Boolean,
  isAlerting: Boolean,
  isExcisionAccent: Boolean,
  tabTestTag: String,
  alertPipTestTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme

  val backgroundColor: Color
  val borderColor: Color
  val textColor: Color
  val pipAlpha: Float

  if (isAlerting && !isSelected) {
    val pulse = rememberInfiniteTransition(label = "clot_tab_pulse_$tabTestTag")
    val animBg by
      pulse.animateColor(
        initialValue = ClotPulseBgLow,
        targetValue = ClotPulseBgHigh,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
          ),
        label = "clot_bg",
      )
    val animBorder by
      pulse.animateColor(
        initialValue = ClotPulseBorderLow,
        targetValue = ClotPulseBorderHigh,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
          ),
        label = "clot_border",
      )
    val animPip by
      pulse.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
          ),
        label = "clot_pip",
      )
    backgroundColor = animBg
    borderColor = animBorder
    textColor = ClotPulseText
    pipAlpha = animPip
  } else {
    backgroundColor =
      when {
        isSelected && isExcisionAccent -> colorScheme.tertiaryContainer
        isSelected -> colorScheme.surfaceVariant
        else -> colorScheme.surface
      }
    borderColor = Color.Transparent
    textColor =
      when {
        isSelected && isExcisionAccent -> colorScheme.tertiary
        isSelected -> colorScheme.onBackground
        else -> colorScheme.onSurfaceVariant
      }
    pipAlpha = 0f
  }

  val alertStateDesc = stringResource(R.string.tab_alert_state_desc)
  Surface(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .testTag(tabTestTag)
        .semantics {
          if (isAlerting && !isSelected) {
            stateDescription = alertStateDesc
          }
        }
        .then(
          if (isAlerting && !isSelected) Modifier.border(1.dp, borderColor) else Modifier
        )
        .clickable(role = Role.Tab, onClick = onClick),
    color = backgroundColor,
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      if (isAlerting && !isSelected) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(2.dp)
              .align(Alignment.TopCenter)
              .background(borderColor)
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (isAlerting && !isSelected) {
          Box(
            modifier =
              Modifier.size(6.dp)
                .alpha(pipAlpha)
                .clip(CircleShape)
                .background(ClotPulsePip)
                .testTag(alertPipTestTag)
          )
          Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, letterSpacing = 0.5.sp),
          color = textColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}
