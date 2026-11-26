/**
 * Flicker-free DOM renderer facade for the GitHub Pages multi-arc web version.
 * Composes modular Arc 1, Arc 2, shared chrome, tab alert, and dripping margin sub-modules
 * under `./ui/` while preserving the exact public export surface.
 */

import { GAME_ARCS } from './engine.js?v=3.14';
import {
  REPEAT_EVENT_DRIP_CHANCE,
  STORY_DRIP_QUEUE_GAP_MS,
  applyDripMotionProfile,
  computeDripMotionProfile,
  createDrippingMarginController,
  createStoryDripQueue,
  shouldTriggerEventDrip,
} from './ui/drippingMargin.js?v=3.14';
import {
  arc1UnlockKeys,
  arc2UnlockKeys,
  isArc1TabReady,
  isArc2TabReady,
  shouldAlertArc1Tab,
  shouldAlertArc2Tab,
} from './ui/tabAlerts.js?v=3.14';
import {
  DEEP_SHELF_JARS,
  FOUNDATIONAL_SHELF_JARS,
  createArc1ChamberUi,
  partitionChamberShelfJars,
} from './ui/arc1ChamberUi.js?v=3.14';
import { createArc2InvestigatorUi } from './ui/arc2InvestigatorUi.js?v=3.14';
import { createSharedChromeUi } from './ui/sharedChromeUi.js?v=3.14';

export {
  DEEP_SHELF_JARS,
  FOUNDATIONAL_SHELF_JARS,
  REPEAT_EVENT_DRIP_CHANCE,
  STORY_DRIP_QUEUE_GAP_MS,
  applyDripMotionProfile,
  arc1UnlockKeys,
  arc2UnlockKeys,
  computeDripMotionProfile,
  createStoryDripQueue,
  isArc1TabReady,
  isArc2TabReady,
  partitionChamberShelfJars,
  shouldAlertArc1Tab,
  shouldAlertArc2Tab,
  shouldTriggerEventDrip,
};

export function createUiController(handlers) {
  const els = {
    body: document.body,
    landingTitle: document.getElementById('landing-title'),
    arc1Shell: document.getElementById('arc1-shell'),
    arc2Shell: document.getElementById('arc2-shell'),
    arc3Shell: document.getElementById('arc3-shell'),

    // Arc 1 (Diegetic Top Telemetry)
    phaseHeader: document.getElementById('phase-header'),
    phaseProgressFill: document.getElementById('phase-progress-fill'),
    thresholdStatus: document.getElementById('threshold-status'),

    ichorRate: document.getElementById('ichor-rate'),
    counterGlassPlate: document.getElementById('counter-glass-plate'),
    counterValue: document.getElementById('counter-value'),
    stillnessText: document.getElementById('stillness-text'),
    stillnessActiveBadge: document.getElementById('stillness-active-badge'),
    gazeActiveBadge: document.getElementById('gaze-active-badge'),
    clampActiveBadge: document.getElementById('clamp-active-badge'),
    bloomActiveBadge: document.getElementById('bloom-active-badge'),
    valveButton: document.getElementById('valve-button'),
    valveYieldText: document.getElementById('valve-yield-text'),
    valveSubLabel: document.getElementById('valve-sub-label'),
    residueLayer: document.getElementById('residue-layer'),
    anomalyLayer: document.getElementById('anomaly-layer'),
    chamberJarShelves: document.getElementById('chamber-jar-shelves'),

    deckPanel: document.getElementById('apparatus-deck'),
    deckTabs: document.getElementById('deck-tabs'),
    deckContent: document.getElementById('deck-content'),

    // Arc 2
    arc2Header: document.getElementById('arc2-header'),
    arc2CaseSubtitle: document.getElementById('arc2-case-subtitle'),
    arc2CaseProgressFill: document.getElementById('arc2-case-progress-fill'),
    arc2ChapterStatus: document.getElementById('arc2-chapter-status'),
    arc2LeadsRate: document.getElementById('arc2-leads-rate'),
    arc2LeadsValue: document.getElementById('arc2-leads-value'),
    arc2SilenceText: document.getElementById('arc2-silence-text'),
    arc2SilenceBadge: document.getElementById('arc2-silence-badge'),
    arc2ScrubBadge: document.getElementById('arc2-scrub-badge'),
    arc2FocusBadge: document.getElementById('arc2-focus-badge'),
    lampToggleBtn: document.getElementById('lamp-toggle-btn'),
    inspectYieldText: document.getElementById('inspect-yield-text'),
    arc2ResidueLayer: document.getElementById('arc2-residue-layer'),
    deskAnomalyLayer: document.getElementById('desk-anomaly-layer'),
    fileZeroReveal: document.getElementById('file-zero-reveal'),
    arc2DeckTabs: document.getElementById('arc2-deck-tabs'),
    arc2DeckContent: document.getElementById('arc2-deck-content'),

    // Arc 3
    arc3Stats: document.getElementById('arc3-stats'),

    // Shared Ledger, Archive & Dual-Lane Dripping Margin Overlays
    ledgerPanel: document.getElementById('ledger-panel'),
    ledgerSpeaker: document.getElementById('ledger-speaker'),
    ledgerDivider: document.getElementById('ledger-divider'),
    ledgerBody: document.getElementById('ledger-body'),
    openArchiveBtn: document.getElementById('open-archive-btn'),
    archiveModal: document.getElementById('archive-modal'),
    archiveList: document.getElementById('archive-list'),
    storyDripLayer: document.getElementById('story-drip-layer'),
    eventDripLayer: document.getElementById('event-drip-layer'),
  };

  const drippingMargin = createDrippingMarginController({
    storyDripLayer: els.storyDripLayer,
    eventDripLayer: els.eventDripLayer,
  });
  const arc1Ui = createArc1ChamberUi(els, handlers);
  const arc2Ui = createArc2InvestigatorUi(els, handlers);
  const sharedChrome = createSharedChromeUi(els, drippingMargin);

  return {
    render(state) {
      els.body.dataset.arc = state.activeArc;
      const isArc1 = state.activeArc === GAME_ARCS.ARC_1_CHAMBER;
      const isArc2 = state.activeArc === GAME_ARCS.ARC_2_INVESTIGATION;
      const isArc3 = state.activeArc === GAME_ARCS.ARC_3_CONVERGENCE;

      els.arc1Shell.hidden = !isArc1;
      els.arc2Shell.hidden = !isArc2;
      els.arc3Shell.hidden = !isArc3;
      els.landingTitle.textContent = isArc1
        ? 'THE COUNTING ROOM // WEB TERMINAL'
        : isArc2
          ? 'THE INQUIRY // CASE DESK TERMINAL'
          : 'CONVERGENCE // FILE #00 SEAM';

      if (isArc1) {
        arc1Ui.render(state);
      } else if (isArc2) {
        arc2Ui.render(state);
      } else {
        sharedChrome.renderArc3(state);
      }
      sharedChrome.renderLedger(state);
    },
    renderAnimatedGlyphs() {
      // Wall anomalies now use feathered photographic macro specimens driven by CSS breathing keyframes.
    },
    renderAnimatedDeskGlyphs(breath) {
      arc2Ui.renderAnimatedDeskGlyphs(breath);
    },
    spawnResidue(label, isRaw = false) {
      sharedChrome.spawnResidueIn(els.residueLayer, label, isRaw);
    },
    spawnArc2Residue(label, isRaw = false) {
      sharedChrome.spawnResidueIn(els.arc2ResidueLayer, label, isRaw);
    },
    triggerEventDrip(text, speaker = 'CHAMBER', eventKey = null) {
      return drippingMargin.triggerEventDrip(text, speaker, eventKey);
    },
    openArchive(state) {
      sharedChrome.openArchive(state);
    },
    closeArchive() {
      sharedChrome.closeArchive();
    },
    resetViewState() {
      arc1Ui.resetViewState();
      arc2Ui.resetViewState();
      sharedChrome.resetViewState();
      drippingMargin.reset();
    },
  };
}
