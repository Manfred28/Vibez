package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.Arc3DeckTab
import com.example.game.Arc3Stratum
import com.example.game.Arc3UnmakeAct
import com.example.game.Arc3UnmakeCategory
import com.example.game.CreatureType
import com.example.game.OrganGameState
import com.example.ui.theme.ArterialCrimson
import com.example.ui.theme.ArterialShadow
import com.example.ui.theme.BloodSoakedPaper
import com.example.ui.theme.BrightMeniscus
import com.example.ui.theme.DeepClotTide
import com.example.ui.theme.FreshSeepage
import java.text.NumberFormat
import kotlin.math.ceil

/**
 * Two-tab Arc 3 ("The Unmaking") action deck:
 * - UNMAKE: 18 chamber-dismantling acts (Shattered Jars, Reversed Apparatus, Unstitched Seams)
 *   reflecting real Arc 1 unlocks with canonical fallbacks.
 * - WEDGES: 5 irreversible Memory Wedge checkpoints (Arc 2's 5 Keystones) with active Clotting
 *   tension bar and pulsing crimson Drive Wedge CTA.
 */
@Composable
internal fun Arc3BreachDeck(
  state: OrganGameState,
  selectedTab: Arc3DeckTab,
  onSelectTab: (Arc3DeckTab) -> Unit,
  onPerformUnmake: (Arc3UnmakeAct) -> Unit,
  onDriveWedge: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  var seenKeysByTab by remember {
    mutableStateOf(
      Arc3DeckTab.entries.associateWith { tab -> TabAlertEvaluator.arc3UnlockKeys(state, tab) }
    )
  }

  LaunchedEffect(selectedTab, state.arc3.currentStratum, state.arc3.completedUnmakes.size, state.arc3.canDriveWedge) {
    val activeKeys = TabAlertEvaluator.arc3UnlockKeys(state, selectedTab)
    val prev = seenKeysByTab[selectedTab].orEmpty()
    if (!activeKeys.all { it in prev }) {
      seenKeysByTab = seenKeysByTab + (selectedTab to (prev + activeKeys))
    }
  }

  val alertUnmake =
    TabAlertEvaluator.shouldAlertArc3Tab(state, Arc3DeckTab.UNMAKE, selectedTab, seenKeysByTab)
  val alertWedges =
    TabAlertEvaluator.shouldAlertArc3Tab(state, Arc3DeckTab.WEDGES, selectedTab, seenKeysByTab)

  Column(
    modifier = modifier.fillMaxWidth().testTag("arc3_breach_deck"),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // 2-Tab Selector Row
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.small,
      color = colorScheme.surface,
      border = BorderStroke(1.dp, colorScheme.outlineVariant),
    ) {
      Row(modifier = Modifier.fillMaxWidth()) {
        DeckTabCell(
          label = stringResource(R.string.arc3_tab_unmake),
          isSelected = selectedTab == Arc3DeckTab.UNMAKE,
          isAlerting = alertUnmake,
          isExcisionAccent = false,
          tabTestTag = "arc3_tab_unmake",
          alertPipTestTag = "arc3_tab_alert_unmake",
          onClick = { onSelectTab(Arc3DeckTab.UNMAKE) },
          modifier = Modifier.weight(1f),
        )
        DeckTabCell(
          label = stringResource(R.string.arc3_tab_wedges),
          isSelected = selectedTab == Arc3DeckTab.WEDGES,
          isAlerting = alertWedges,
          isExcisionAccent = true,
          tabTestTag = "arc3_tab_wedges",
          alertPipTestTag = "arc3_tab_alert_wedges",
          onClick = { onSelectTab(Arc3DeckTab.WEDGES) },
          modifier = Modifier.weight(1f),
        )
      }
    }

    when (selectedTab) {
      Arc3DeckTab.UNMAKE -> Arc3UnmakePanel(state = state, onPerformUnmake = onPerformUnmake)
      Arc3DeckTab.WEDGES -> Arc3WedgesPanel(state = state, onDriveWedge = onDriveWedge)
    }
  }
}

@Composable
private fun Arc3UnmakePanel(
  state: OrganGameState,
  onPerformUnmake: (Arc3UnmakeAct) -> Unit,
) {
  val breach = state.arc3
  val colorScheme = MaterialTheme.colorScheme
  val numberFormat = remember { NumberFormat.getIntegerInstance() }
  val legacyJarTag = stringResource(R.string.arc3_legacy_jar_tag)
  val legacyScarTag = stringResource(R.string.arc3_legacy_scar_tag)

  val completedActs =
    Arc3UnmakeAct.entries.filter { it in breach.completedUnmakes }

  Column(
    modifier = Modifier.fillMaxWidth().testTag("arc3_unmake_panel"),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Arc3UnmakeCategory.entries.forEach { category ->
      val activeActs =
        Arc3UnmakeAct.entries.filter {
          it.category == category &&
            breach.isUnmakeVisible(it) &&
            it !in breach.completedUnmakes
        }
      if (activeActs.isNotEmpty()) {
        Text(
          text = category.label,
          style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
          color = BrightMeniscus.copy(alpha = 0.85f),
          modifier = Modifier.padding(top = 2.dp),
        )
        activeActs.forEach { act ->
          Arc3UnmakeCard(
            state = state,
            act = act,
            completed = false,
            canPerform = breach.canPerformUnmake(act),
            legacyJarTag = legacyJarTag,
            legacyScarTag = legacyScarTag,
            numberFormat = numberFormat,
            onPerformUnmake = onPerformUnmake,
          )
        }
      }
    }

    if (completedActs.isNotEmpty()) {
      HorizontalDivider(color = colorScheme.outlineVariant)
      Text(
        text = stringResource(R.string.arc3_unmake_completed_section),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
        color = colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        modifier = Modifier.padding(top = 2.dp),
      )
      completedActs.forEach { act ->
        Arc3UnmakeCard(
          state = state,
          act = act,
          completed = true,
          canPerform = false,
          legacyJarTag = legacyJarTag,
          legacyScarTag = legacyScarTag,
          numberFormat = numberFormat,
          onPerformUnmake = onPerformUnmake,
        )
      }
    }
  }
}

@Composable
private fun Arc3UnmakeCard(
  state: OrganGameState,
  act: Arc3UnmakeAct,
  completed: Boolean,
  canPerform: Boolean,
  legacyJarTag: String,
  legacyScarTag: String,
  numberFormat: NumberFormat,
  onPerformUnmake: (Arc3UnmakeAct) -> Unit,
) {
  val colorScheme = MaterialTheme.colorScheme
  val legacyTag =
    when {
      act.linkedJar != null && act.linkedJar in state.ownedJars -> legacyJarTag
      act.linkedScarNode != null && act.linkedScarNode in state.unlockedScarNodes -> legacyScarTag
      else -> null
    }

  Surface(
    modifier = Modifier.fillMaxWidth().testTag("arc3_unmake_card_${act.id}"),
    shape = MaterialTheme.shapes.small,
    color =
      when {
        completed -> DeepClotTide.copy(alpha = 0.55f)
        canPerform -> colorScheme.surfaceVariant
        else -> colorScheme.surface
      },
    border =
      BorderStroke(
        width = 1.dp,
        color =
          when {
            completed -> ArterialCrimson.copy(alpha = 0.55f)
            canPerform -> BrightMeniscus
            else -> colorScheme.outlineVariant
          },
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Arc3ThumbnailFrame(
        drawableRes = arc3UnmakeDrawableRes(act, completed),
        accent = completed || canPerform,
        dimmed = completed,
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = act.title,
            style = MaterialTheme.typography.labelMedium,
            color = if (completed) BloodSoakedPaper.copy(alpha = 0.72f) else BloodSoakedPaper,
          )
          if (legacyTag != null) {
            Text(
              text = legacyTag,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
              color = BrightMeniscus,
              modifier =
                Modifier.background(FreshSeepage, MaterialTheme.shapes.extraSmall)
                  .padding(horizontal = 4.dp, vertical = 1.dp),
            )
          }
        }
        Text(
          text = act.description,
          style = MaterialTheme.typography.bodySmall,
          color = colorScheme.onSurfaceVariant,
        )
      }

      if (completed) {
        Text(
          text = stringResource(R.string.arc3_unmake_done_badge),
          style = MaterialTheme.typography.labelSmall,
          color = ArterialCrimson,
          modifier = Modifier.padding(horizontal = 6.dp),
        )
      } else {
        val actionLabel =
          stringResource(
            if (act.category == Arc3UnmakeCategory.SHATTER_JAR) R.string.arc3_shatter_action
            else R.string.arc3_unmake_action,
            numberFormat.format(ceil(act.hemorrhageCost).toLong()),
          )
        Surface(
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .testTag("arc3_unmake_${act.id}")
              .clickable(enabled = canPerform, role = Role.Button) {
                onPerformUnmake(act)
              },
          shape = MaterialTheme.shapes.extraSmall,
          color = if (canPerform) ArterialCrimson else ArterialShadow,
          border =
            BorderStroke(
              width = 1.dp,
              color = if (canPerform) BrightMeniscus else colorScheme.outlineVariant,
            ),
        ) {
          Text(
            text = actionLabel,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
            color = if (canPerform) BloodSoakedPaper else colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun Arc3WedgesPanel(
  state: OrganGameState,
  onDriveWedge: () -> Unit,
) {
  val breach = state.arc3
  val colorScheme = MaterialTheme.colorScheme
  val numberFormat = remember { NumberFormat.getIntegerInstance() }
  val activeStratum = breach.activeStratum

  Column(
    modifier = Modifier.fillMaxWidth().testTag("arc3_wedges_panel"),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // Active Stratum Fracture & Drive Wedge Card
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.small,
      color = if (breach.canDriveWedge) FreshSeepage else colorScheme.surfaceVariant,
      border =
        BorderStroke(
          width = 1.dp,
          color = if (breach.canDriveWedge) BrightMeniscus else colorScheme.outline,
        ),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = activeStratum.title,
            style = MaterialTheme.typography.titleMedium,
            color = BloodSoakedPaper,
          )
          Text(
            text = "${numberFormat.format(breach.fractureDisplay)} / ${numberFormat.format(activeStratum.fractureGoal.toLong())}",
            style = MaterialTheme.typography.labelMedium,
            color = BrightMeniscus,
          )
        }
        LinearProgressIndicator(
          progress = { breach.stratumProgress.coerceIn(0f, 1f) },
          modifier = Modifier.fillMaxWidth().height(5.dp).testTag("arc3_wedge_progress_bar"),
          color = BrightMeniscus,
          trackColor = ArterialShadow,
        )
        Text(
          text = activeStratum.wedgeCaption,
          style = MaterialTheme.typography.bodySmall,
          color = colorScheme.onSurfaceVariant,
        )

        Surface(
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("arc3_drive_wedge_button")
              .clickable(enabled = breach.canDriveWedge, role = Role.Button, onClick = onDriveWedge),
          shape = MaterialTheme.shapes.extraSmall,
          color = if (breach.canDriveWedge) ArterialCrimson else ArterialShadow,
          border =
            BorderStroke(
              width = 1.dp,
              color = if (breach.canDriveWedge) BrightMeniscus else colorScheme.outlineVariant,
            ),
        ) {
          Text(
            text = stringResource(R.string.arc3_wedge_drive_action, activeStratum.wedgeTitle),
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
            color = if (breach.canDriveWedge) BloodSoakedPaper else colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
          )
        }
      }
    }

    HorizontalDivider(color = colorScheme.outlineVariant)

    // 5 Permanent Memory Wedge Strips (Arc 2's 5 Keystones)
    Arc3Stratum.entries.forEach { stratum ->
      val locked = stratum in breach.lockedWedges
      val isCurrent = !breach.isInFinale && stratum == activeStratum
      Surface(
        modifier = Modifier.fillMaxWidth().testTag("arc3_wedge_card_${stratum.number}"),
        shape = MaterialTheme.shapes.small,
        color =
          when {
            locked -> DeepClotTide.copy(alpha = 0.85f)
            isCurrent -> colorScheme.surfaceVariant
            else -> colorScheme.surface.copy(alpha = 0.72f)
          },
        border =
          BorderStroke(
            width = 1.dp,
            color =
              when {
                locked -> BrightMeniscus.copy(alpha = 0.75f)
                isCurrent -> ArterialCrimson
                else -> colorScheme.outlineVariant.copy(alpha = 0.5f)
              },
          ),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Arc3ThumbnailFrame(
            drawableRes = arc3WedgeDrawableRes(stratum),
            accent = locked || isCurrent,
            dimmed = !locked && !isCurrent,
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = stratum.wedgeTitle,
              style = MaterialTheme.typography.labelMedium,
              color = if (locked || isCurrent) BloodSoakedPaper else colorScheme.onSurfaceVariant,
            )
            Text(
              text = stratum.title,
              style = MaterialTheme.typography.labelSmall,
              color = if (locked) BrightMeniscus.copy(alpha = 0.85f) else colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            )
          }
          Spacer(modifier = Modifier.height(1.dp))
          Text(
            text =
              stringResource(
                if (locked) R.string.arc3_wedge_locked_badge else R.string.arc3_wedge_awaiting_badge
              ),
            style = MaterialTheme.typography.labelSmall,
            color = if (locked) BrightMeniscus else colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
          )
        }
      }
    }
  }
}

@Composable
private fun Arc3ThumbnailFrame(
  drawableRes: Int,
  accent: Boolean,
  dimmed: Boolean,
) {
  val frameShape = RoundedCornerShape(3.dp)
  Box(
    modifier =
      Modifier.size(44.dp)
        .clip(frameShape)
        .background(ArterialShadow)
        .border(
          width = 1.dp,
          color = if (accent) ArterialCrimson.copy(alpha = 0.85f) else MaterialTheme.colorScheme.outlineVariant,
          shape = frameShape,
        )
  ) {
    Image(
      painter = painterResource(id = drawableRes),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize().alpha(if (dimmed) 0.56f else 0.92f),
    )
  }
}

internal fun arc3UnmakeDrawableRes(act: Arc3UnmakeAct, completed: Boolean): Int {
  if (act.category == Arc3UnmakeCategory.SHATTER_JAR) {
    if (completed) return R.drawable.img_arc3_shattered_jar
    val jar = act.linkedJar
    if (jar != null) return specimenJarDrawableRes(jar)
    return R.drawable.img_arc3_shattered_jar
  }
  return when (act) {
    Arc3UnmakeAct.REVERSE_SUTURE_HANDS -> R.drawable.img_suture_hand
    Arc3UnmakeAct.REVERSE_LEECH_CLERKS -> R.drawable.img_leech_clerk
    Arc3UnmakeAct.BURST_WEEPING_LAMPS -> R.drawable.img_weeping_lamp
    Arc3UnmakeAct.BACKFLOOD_SUMP_SIPHONS -> R.drawable.img_sump_siphon
    Arc3UnmakeAct.RIP_CHOIR_TENDRILS -> R.drawable.img_graft_tendril
    Arc3UnmakeAct.RUPTURE_CHOIR_LUNGS -> R.drawable.img_graft_choir_lung
    Arc3UnmakeAct.UNSTITCH_FIRST_INCISION,
    Arc3UnmakeAct.UNSTITCH_SUTURE_LINE,
    Arc3UnmakeAct.UNSTITCH_STILLNESS_LINE -> R.drawable.img_rupture_torn_suture
    Arc3UnmakeAct.UNSTITCH_MARROW_LINE -> R.drawable.img_rupture_splintered_rib
    Arc3UnmakeAct.BREAK_FLOOR_GRATE_THROAT -> R.drawable.img_rupture_arterial_well
    Arc3UnmakeAct.SPLIT_BRICKED_JAMB -> R.drawable.img_arc3_open_seam
    else -> {
      when (act.linkedCreature) {
        CreatureType.SUTURE_HAND -> R.drawable.img_suture_hand
        CreatureType.LEECH_CLERK -> R.drawable.img_leech_clerk
        CreatureType.WEEPING_LAMP -> R.drawable.img_weeping_lamp
        CreatureType.SUMP_SIPHON -> R.drawable.img_sump_siphon
        null -> R.drawable.img_rupture_torn_suture
      }
    }
  }
}

internal fun arc3WedgeDrawableRes(stratum: Arc3Stratum): Int =
  when (stratum) {
    Arc3Stratum.BROKEN_GROUT -> R.drawable.img_exhibit_tax_plat_1998
    Arc3Stratum.UNSTITCHED_SEAM -> R.drawable.img_exhibit_second_chair_polaroid
    Arc3Stratum.SPLINTERED_RIBS -> R.drawable.img_exhibit_reel_four_waveform
    Arc3Stratum.SHARED_PULSE -> R.drawable.img_exhibit_drainage_loop_blueprint
    Arc3Stratum.THE_OPEN_SEAM -> R.drawable.img_arc3_wedge_file_zero
  }
