package com.example.ui

import androidx.compose.runtime.Immutable
import com.example.game.AuditRedaction
import com.example.game.CaseExhibit
import com.example.game.InvestigatorContact

/**
 * Grouped immutable action contract for Arc 2 (The Inquiry).
 * Encapsulates Lamp On/Off dual-lens, Contacts + Bulk Warrants, Red-String Corkboard wiring,
 * Silence Exhibits, Acoustic Reel Scrubbing, Case IV Redactions, and Case Angle soft-reset pivots.
 */
@Immutable
data class InvestigatorActions(
  val onToggleLamp: () -> Unit = {},
  val onInspectCaseFile: () -> Unit = {},
  val onRecruitContact: (InvestigatorContact) -> Unit = {},
  val onRecruitMaxContact: (InvestigatorContact) -> Unit = {},
  val onUnlockExhibit: (CaseExhibit) -> Unit = {},
  val onToggleExhibitPair: (CaseExhibit, CaseExhibit) -> Unit = { _, _ -> },
  val onTriggerReelScrub: () -> Unit = {},
  val onDissolveRedaction: (AuditRedaction) -> Unit = {},
  val onInteractDeskAnomaly: (Long) -> Unit = {},
  val onPivotNextCase: () -> Unit = {},
  val onEnterArc3: () -> Unit = {},
)
