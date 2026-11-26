package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.Arc2DeskState
import com.example.game.AuditRedaction
import com.example.game.CaseExhibit
import com.example.game.InvestigatorContact
import com.example.ui.InvestigatorActions
import com.example.ui.InvestigatorActionRow
import com.example.ui.theme.BleakTheme
import java.text.NumberFormat
import kotlin.math.ceil

@Composable
internal fun InvestigatorContactsPanel(
  desk: Arc2DeskState,
  nf: NumberFormat,
  actions: InvestigatorActions,
) {
  val colorScheme = MaterialTheme.colorScheme
  val unlockedContacts = InvestigatorContact.entries.filter { desk.isContactUnlocked(it) }
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    unlockedContacts.forEach { contact ->
      val count = desk.contactCount(contact)
      val cost = desk.contactCost(contact)
      val canAfford = desk.leads >= cost.toDouble()
      val title =
        when (contact) {
          InvestigatorContact.NIGHT_ARCHIVIST -> stringResource(R.string.arc2_contact_archivist)
          InvestigatorContact.BRIBED_ORDERLY -> stringResource(R.string.arc2_contact_orderly)
          InvestigatorContact.MICROFILM_SCANNER -> stringResource(R.string.arc2_contact_scanner)
          InvestigatorContact.AUDIO_TRANSCRIBER -> stringResource(R.string.arc2_contact_transcriber)
        }
      val desc =
        when (contact) {
          InvestigatorContact.NIGHT_ARCHIVIST -> stringResource(R.string.arc2_contact_archivist_desc)
          InvestigatorContact.BRIBED_ORDERLY -> stringResource(R.string.arc2_contact_orderly_desc)
          InvestigatorContact.MICROFILM_SCANNER -> stringResource(R.string.arc2_contact_scanner_desc)
          InvestigatorContact.AUDIO_TRANSCRIBER -> stringResource(R.string.arc2_contact_transcriber_desc)
        }
      val each = formatCompactDecimal(desk.effectiveContactLeadsPerSec(contact))
      InvestigatorActionRow(
        title = "$title [$count]",
        subtitle = "$desc · +$each/s",
        actionLabel = "${nf.format(cost)} LEADS",
        enabled = canAfford,
        accent = false,
        testTag = "recruit_${contact.id}",
        onClick = { actions.onRecruitContact(contact) },
      )
      if (desk.isBulkWarrantUnlocked) {
        val (bulkCount, bulkCost) = desk.maxAffordableContacts(contact)
        if (bulkCount >= 2) {
          Surface(
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("warrant_max_${contact.id}")
                .clickable(role = Role.Button) { actions.onRecruitMaxContact(contact) },
            shape = MaterialTheme.shapes.extraSmall,
            color = colorScheme.primaryContainer.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, colorScheme.primary),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = stringResource(R.string.arc2_warrant_action, bulkCount),
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.primary,
              )
              Text(
                text = "${nf.format(bulkCost)} LEADS",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onBackground,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun InvestigatorDossierPanel(
  desk: Arc2DeskState,
  nf: NumberFormat,
  actions: InvestigatorActions,
) {
  val colorScheme = MaterialTheme.colorScheme
  val atmosphere = BleakTheme.atmosphere
  val dossierExhibits =
    CaseExhibit.entries.filter { !it.isKeystone && desk.caseAngle >= it.unlockCaseAngle }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = "SILENCE EXHIBITS (PERMANENT)",
      style = MaterialTheme.typography.labelSmall,
      color = colorScheme.onSurfaceVariant,
    )
    dossierExhibits.forEach { exhibit ->
      val pinned = exhibit in desk.pinnedExhibits
      val canUnlock = desk.canUnlockExhibit(exhibit)
      InvestigatorActionRow(
        title = "${exhibit.shortLabel} · ${exhibit.title.uppercase()}",
        subtitle = exhibit.caption,
        actionLabel =
          if (pinned) stringResource(R.string.arc2_exhibit_pinned)
          else stringResource(R.string.arc2_exhibit_unlock, nf.format(exhibit.silenceCost.toLong())),
        enabled = canUnlock,
        accent = pinned,
        testTag = "unlock_exhibit_${exhibit.id}",
        onClick = { actions.onUnlockExhibit(exhibit) },
      )
    }

    if (desk.isReelScrubUnlocked) {
      HorizontalDivider(color = atmosphere.hairline)
      Text(
        text = stringResource(R.string.arc2_reel_scrub_title),
        style = MaterialTheme.typography.labelSmall,
        color = colorScheme.primary,
      )
      val scrubLabel =
        when {
          desk.isReelScrubbing ->
            stringResource(R.string.arc2_reel_scrub_active, ceil(desk.reelScrubRemainingSeconds).toInt())
          desk.reelScrubCooldownSeconds > 0.0 ->
            stringResource(R.string.arc2_reel_scrub_cooldown, ceil(desk.reelScrubCooldownSeconds).toInt())
          desk.lampOn -> stringResource(R.string.arc2_reel_scrub_need_dark)
          else -> stringResource(R.string.arc2_reel_scrub_action)
        }
      InvestigatorActionRow(
        title = "REEL 4 MAGNETIC HEAD (SCRUBS: ${desk.reelScrubCount})",
        subtitle = stringResource(R.string.arc2_reel_scrub_desc),
        actionLabel = scrubLabel,
        enabled = desk.canTriggerReelScrub,
        accent = desk.isReelScrubbing,
        testTag = "reel_scrub_button",
        onClick = actions.onTriggerReelScrub,
      )
    }

    if (desk.isRedactionSolventUnlocked) {
      HorizontalDivider(color = atmosphere.hairline)
      Text(
        text = "${stringResource(R.string.arc2_redaction_title)} (${desk.dissolvedRedactions.size}/4)",
        style = MaterialTheme.typography.labelSmall,
        color = colorScheme.tertiary,
      )
      Text(
        text = stringResource(R.string.arc2_redaction_desc),
        style = MaterialTheme.typography.bodySmall,
        color = colorScheme.onSurfaceVariant,
      )
      AuditRedaction.entries.forEach { clause ->
        val dissolved = clause in desk.dissolvedRedactions
        val canDissolve = desk.canDissolveRedaction(clause)
        InvestigatorActionRow(
          title = clause.label,
          subtitle = if (dissolved) "${clause.revealedText} (${clause.effectSummary})" else "[BLACKED OUT — ${clause.effectSummary}]",
          actionLabel =
            when {
              dissolved -> stringResource(R.string.arc2_redaction_clear)
              desk.lampOn -> stringResource(R.string.arc2_reel_scrub_need_dark)
              else -> stringResource(R.string.arc2_redaction_dissolve, nf.format(clause.silenceCost.toLong()))
            },
          enabled = canDissolve,
          accent = dissolved,
          testTag = "redaction_${clause.id}",
          onClick = { actions.onDissolveRedaction(clause) },
        )
      }
    }
  }
}

@Composable
internal fun InvestigatorRevisionPanel(
  desk: Arc2DeskState,
  nf: NumberFormat,
  actions: InvestigatorActions,
) {
  val colorScheme = MaterialTheme.colorScheme
  val current = desk.currentCase

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    if (desk.fileZeroRevealed) {
      Surface(
        modifier = Modifier.fillMaxWidth().testTag("revision_file_zero_panel"),
        shape = MaterialTheme.shapes.extraSmall,
        color = colorScheme.tertiaryContainer.copy(alpha = 0.78f),
        border = BorderStroke(1.2.dp, colorScheme.tertiary),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = stringResource(R.string.arc2_file_zero_banner),
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.onTertiaryContainer,
          )
          Surface(
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("revision_enter_arc3_button")
                .clickable(role = Role.Button, onClick = actions.onEnterArc3),
            shape = MaterialTheme.shapes.extraSmall,
            color = colorScheme.errorContainer,
            border = BorderStroke(1.dp, colorScheme.error),
          ) {
            Text(
              text = stringResource(R.string.arc2_enter_arc3),
              style = MaterialTheme.typography.labelLarge,
              color = colorScheme.onErrorContainer,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            )
          }
        }
      }
      return
    }

    Text(
      text = stringResource(R.string.arc2_pivot_title),
      style = MaterialTheme.typography.labelLarge,
      color = colorScheme.primary,
    )
    Text(
      text = stringResource(R.string.arc2_pivot_desc),
      style = MaterialTheme.typography.bodySmall,
      color = colorScheme.onSurfaceVariant,
    )

    // Explicit Dead-End Requirements Checklist
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      ChecklistLine(
        label = "LEADS",
        current = nf.format(desk.leadsDisplay),
        target = nf.format(current.leadsCost),
        met = desk.leads >= current.leadsCost.toDouble(),
      )
      ChecklistLine(
        label = "SILENCE",
        current = nf.format(desk.silenceDisplay),
        target = nf.format(current.silenceCost),
        met = desk.silence >= current.silenceCost.toDouble(),
      )
      ChecklistLine(
        label = "CORROBORATIONS DISCOVERED",
        current = desk.discoveredCorroborations.size.toString(),
        target = current.requiredCorroborations.toString(),
        met = desk.discoveredCorroborations.size >= current.requiredCorroborations,
      )
      if (current.requiredReelScrubs > 0) {
        ChecklistLine(
          label = "ACOUSTIC REEL SCRUBS",
          current = desk.reelScrubCount.toString(),
          target = current.requiredReelScrubs.toString(),
          met = desk.reelScrubCount >= current.requiredReelScrubs,
        )
      }
      if (current.requiredRedactions > 0) {
        ChecklistLine(
          label = "AUDIT CLAUSES DISSOLVED",
          current = desk.dissolvedRedactions.size.toString(),
          target = current.requiredRedactions.toString(),
          met = desk.dissolvedRedactions.size >= current.requiredRedactions,
        )
      }
    }

    val rewardText =
      current.keystoneReward?.let { "REWARD: PIN ${it.title.uppercase()} +1 STRING SPOOL" }
        ?: "REWARD: UNSEAL FILE #00 (SUBJECT INTAKE)"
    Text(
      text = rewardText,
      style = MaterialTheme.typography.labelSmall,
      color = colorScheme.tertiary,
    )

    val canPivot = desk.canPivotNextCase
    Surface(
      modifier =
        Modifier.fillMaxWidth()
          .minimumInteractiveComponentSize()
          .testTag("pivot_case_button")
          .clickable(enabled = canPivot, role = Role.Button, onClick = actions.onPivotNextCase),
      shape = MaterialTheme.shapes.extraSmall,
      color = if (canPivot) colorScheme.tertiaryContainer else colorScheme.surfaceContainerLowest,
      border = BorderStroke(1.2.dp, if (canPivot) colorScheme.tertiary else colorScheme.outlineVariant),
    ) {
      Text(
        text =
          if (current.number >= 5) stringResource(R.string.arc2_file_zero_action)
          else stringResource(R.string.arc2_pivot_action),
        style = MaterialTheme.typography.labelLarge,
        color = if (canPivot) colorScheme.onTertiaryContainer else colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
      )
    }
  }
}

@Composable
private fun ChecklistLine(label: String, current: String, target: String, met: Boolean) {
  val colorScheme = MaterialTheme.colorScheme
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = "${if (met) "[✓]" else "[ ]"} $label",
      style = MaterialTheme.typography.labelSmall,
      color = if (met) colorScheme.primary else colorScheme.onSurfaceVariant,
    )
    Text(
      text = "$current / $target",
      style = MaterialTheme.typography.labelSmall,
      color = if (met) colorScheme.primary else colorScheme.onSurfaceVariant,
    )
  }
}
