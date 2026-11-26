package com.example.ui

import androidx.compose.runtime.Immutable
import com.example.game.CaseDeduction
import com.example.game.InvestigatorContact

/**
 * Grouped immutable action contract for Arc 2 (The Inquiry).
 * Keeps screen signatures compact and avoids parameter sprawl as upcoming Arc 2 Corkboard,
 * Desk Lamp, and Forensic interactions are added.
 */
@Immutable
data class InvestigatorActions(
  val onInspectCaseFile: () -> Unit = {},
  val onRecruitContact: (InvestigatorContact) -> Unit = {},
  val onPinDeduction: (CaseDeduction) -> Unit = {},
  val onInteractDeskAnomaly: (Long) -> Unit = {},
  val onConnectNextChapter: () -> Unit = {},
  val onEnterArc3: () -> Unit = {},
)
