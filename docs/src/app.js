/**
 * Bootstrap & lifecycle controller for the GitHub Pages playable multi-arc web version of
 * "The Tending of the Unnamed Organ".
 *
 * Loads progress-bound fragments strictly from `./assets/dialogues.json`, persists state
 * automatically in `localStorage`, wires the Clinical Dev Console, and drives the 100ms engine
 * tick + 60fps Canvas loop across Arc 1, Arc 2, and Arc 3.
 */

import {
  CREATURE_TYPES,
  GAME_ARCS,
  ORGAN_BALANCE,
  WALL_ANOMALY_TYPES,
  activateValveClamp,
  anomalyRewardMultiplier,
  arc2InspectYield,
  bottleJar,
  buyCreature,
  connectNextChapter,
  creatureCount,
  deserializeState,
  devAssignArc1Resources,
  devAssignArc2Resources,
  devResetAll,
  devSetActiveArc,
  devSetAllScarNodes,
  devSetHighestMilestone,
  devSpawnDeskAnomaly,
  devSpawnWallAnomaly,
  dossierIchorMultiplier,
  dredgeSump,
  enterArc3Convergence,
  formatCompactDecimal,
  initialState,
  inspectCaseFile,
  interactAnomaly,
  interactDeskAnomaly,
  manualTapYield,
  offerToThroat,
  performBreachToArc2,
  performChoirLungGraft,
  performExcision,
  performGraft,
  pinDeduction,
  pressValve,
  recruitContact,
  relicIchorMultiplier,
  relicStillnessMultiplier,
  scarMultiplier,
  scarStillnessMultiplier,
  serializeState,
  setEchoDiversion,
  tick,
  toggleDrainSeal,
  unlockScarNode,
} from './engine.js?v=2.2';
import {
  drawChamberBackdrop,
  drawInvestigatorDeskBackdrop,
  drawLeechClerks,
  drawValveAndHands,
} from './chamberCanvas.js?v=2.2';
import { createUiController } from './ui.js?v=2.2';

const SAVE_STORAGE_KEY_V2 = 'unnamed_organ_save_v2';
const SAVE_STORAGE_KEY_V1 = 'unnamed_organ_save_v1';
const PULSE_DURATIONS_MS = [5200, 4400, 3600, 3000, 2400];

function readSavedRaw() {
  try {
    return (
      window.localStorage.getItem(SAVE_STORAGE_KEY_V2) ??
      window.localStorage.getItem(SAVE_STORAGE_KEY_V1)
    );
  } catch {
    return null;
  }
}

function writeSavedRaw(raw) {
  try {
    window.localStorage.setItem(SAVE_STORAGE_KEY_V2, raw);
  } catch {
    // Private browsing or blocked storage — continue safely in memory.
  }
}

function clearSavedRaw() {
  try {
    window.localStorage.removeItem(SAVE_STORAGE_KEY_V2);
    window.localStorage.removeItem(SAVE_STORAGE_KEY_V1);
  } catch {
    // Ignore storage errors.
  }
}

async function loadDialogueCatalog() {
  try {
    const response = await fetch('./assets/dialogues.json?v=2.2', { cache: 'no-cache' });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    return await response.json();
  } catch (err) {
    console.warn('Could not load ./assets/dialogues.json:', err);
    return { schemaVersion: 3, phases: [], entries: [] };
  }
}

async function main() {
  const catalog = await loadDialogueCatalog();
  let state = deserializeState(readSavedRaw(), catalog) ?? initialState(catalog);

  const wallImage = new Image();
  wallImage.src = './assets/img_wall_phase1.jpg';

  const backdropCanvas = document.getElementById('chamber-backdrop-canvas');
  const valveHandsCanvas = document.getElementById('valve-hands-canvas');
  const leechCanvas = document.getElementById('leech-overlay-canvas');
  const deskCanvas = document.getElementById('investigator-desk-canvas');

  const valveBtn = document.getElementById('valve-button');
  const inspectCaseBtn = document.getElementById('inspect-case-btn');
  const enterArc3Btn = document.getElementById('enter-arc3-btn');
  const arc3BackDeskBtn = document.getElementById('arc3-back-desk-btn');
  const arc3BackChamberBtn = document.getElementById('arc3-back-chamber-btn');

  const openArchiveBtn = document.getElementById('open-archive-btn');
  const closeArchiveBtn = document.getElementById('close-archive-btn');
  const archiveModal = document.getElementById('archive-modal');

  const openDevBtn = document.getElementById('open-dev-btn');
  const closeDevBtn = document.getElementById('close-dev-btn');
  const devModal = document.getElementById('dev-modal');

  const wipeSaveBtn = document.getElementById('wipe-save-btn');
  const resetModal = document.getElementById('reset-modal');
  const cancelResetBtn = document.getElementById('cancel-reset-btn');
  const confirmResetBtn = document.getElementById('confirm-reset-btn');

  let autosaveAccumulator = 0;
  let hasUnsavedChanges = false;

  function persistNow() {
    writeSavedRaw(serializeState(state));
    hasUnsavedChanges = false;
  }

  function flushIfDirty() {
    if (hasUnsavedChanges) {
      persistNow();
    }
  }

  function applyState(next, { persist = true } = {}) {
    state = next;
    if (persist) {
      persistNow();
    } else {
      hasUnsavedChanges = true;
    }
    ui.render(state);
  }

  const ui = createUiController({
    onBuyCreature(typeId) {
      applyState(buyCreature(state, typeId, catalog));
    },
    onBottleJar(jarId) {
      applyState(bottleJar(state, jarId, catalog));
    },
    onPerformGraft() {
      applyState(performGraft(state, catalog));
    },
    onPerformChoirLung() {
      applyState(performChoirLungGraft(state, catalog));
    },
    onUnlockScarNode(nodeId) {
      applyState(unlockScarNode(state, nodeId, catalog));
    },
    onToggleDrainSeal() {
      applyState(toggleDrainSeal(state, catalog));
    },
    onDredgeSump() {
      applyState(dredgeSump(state, catalog));
      ui.spawnResidue('RELIC DREDGED', true);
    },
    onSetEchoDiversion(pct) {
      applyState(setEchoDiversion(state, pct, catalog));
    },
    onActivateValveClamp() {
      applyState(activateValveClamp(state, catalog));
      ui.spawnResidue('VALVE CLAMPED (20s)', true);
    },
    onOfferToThroat(lineId) {
      applyState(offerToThroat(state, lineId, catalog));
    },
    onInteractAnomaly(anomalyId) {
      const anomaly = state.activeAnomalies.find((a) => a.id === anomalyId);
      if (anomaly) {
        const rewardBoost = anomalyRewardMultiplier(state);
        const ichorMult =
          scarMultiplier(state) * relicIchorMultiplier(state) * dossierIchorMultiplier(state) * rewardBoost;
        const stillMult = scarStillnessMultiplier(state) * relicStillnessMultiplier(state) * rewardBoost;
        if (anomaly.type === WALL_ANOMALY_TYPES.PALE_TOOTH.id) {
          ui.spawnResidue(`${formatCompactDecimal(ORGAN_BALANCE.TOOTH_BASE_ICHOR_REWARD * ichorMult)} ICHOR · +0.5 TAP`);
        } else if (anomaly.type === WALL_ANOMALY_TYPES.GLASS_EYE.id) {
          ui.spawnResidue('PASSIVE ×1.6 (12s)', true);
        } else if (anomaly.type === WALL_ANOMALY_TYPES.SWOLLEN_NODULE.id) {
          const ichor = formatCompactDecimal(ORGAN_BALANCE.NODULE_BASE_ICHOR_REWARD * ichorMult);
          const still = formatCompactDecimal(ORGAN_BALANCE.NODULE_BASE_STILLNESS_REWARD * stillMult);
          ui.spawnResidue(`${ichor} ICHOR · +${still} STILL`);
        } else if (anomaly.type === WALL_ANOMALY_TYPES.CHITIN_RIB.id) {
          const ichor = formatCompactDecimal(ORGAN_BALANCE.RIB_BASE_ICHOR_REWARD * ichorMult);
          const still = formatCompactDecimal(ORGAN_BALANCE.RIB_BASE_STILLNESS_REWARD * stillMult);
          ui.spawnResidue(`${ichor} ICHOR · +${still} STILL · ×1.6`);
        }
      }
      applyState(interactAnomaly(state, anomalyId, catalog));
    },
    onPerformExcision() {
      applyState(performExcision(state, catalog));
      window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    onPerformBreachToArc2() {
      applyState(performBreachToArc2(state, catalog));
      window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    onRecruitContact(contactId) {
      applyState(recruitContact(state, contactId, catalog));
    },
    onPinDeduction(deductionId) {
      applyState(pinDeduction(state, deductionId, catalog));
    },
    onInteractDeskAnomaly(anomalyId) {
      applyState(interactDeskAnomaly(state, anomalyId, catalog));
      ui.spawnArc2Residue('CLUE LOGGED', true);
    },
    onConnectNextChapter() {
      applyState(connectNextChapter(state, catalog));
    },
    onRequestRender() {
      ui.render(state);
    },
  });

  function triggerValvePress() {
    if (state.activeArc !== GAME_ARCS.ARC_1_CHAMBER) return;
    if (state.clampRemainingSeconds > 0) return;
    const yieldLabel = formatCompactDecimal(manualTapYield(state));
    applyState(pressValve(state, catalog), { persist: false });
    ui.spawnResidue(yieldLabel);
  }

  function triggerCaseInspect() {
    if (state.activeArc !== GAME_ARCS.ARC_2_INVESTIGATION) return;
    const yieldLabel = formatCompactDecimal(arc2InspectYield(state.arc2));
    applyState(inspectCaseFile(state, catalog), { persist: false });
    ui.spawnArc2Residue(yieldLabel);
  }

  valveBtn.addEventListener('click', triggerValvePress);
  inspectCaseBtn.addEventListener('click', triggerCaseInspect);
  enterArc3Btn.addEventListener('click', () => applyState(enterArc3Convergence(state, catalog)));
  arc3BackDeskBtn.addEventListener('click', () =>
    applyState(devSetActiveArc(state, GAME_ARCS.ARC_2_INVESTIGATION, catalog)),
  );
  arc3BackChamberBtn.addEventListener('click', () =>
    applyState(devSetActiveArc(state, GAME_ARCS.ARC_1_CHAMBER, catalog)),
  );

  // Archive Modal
  openArchiveBtn.addEventListener('click', () => ui.openArchive(state));
  closeArchiveBtn.addEventListener('click', () => ui.closeArchive());
  archiveModal.addEventListener('click', (event) => {
    if (event.target === archiveModal) ui.closeArchive();
  });

  // Clinical Dev Console Modal
  openDevBtn.addEventListener('click', () => {
    devModal.hidden = false;
  });
  closeDevBtn.addEventListener('click', () => {
    devModal.hidden = true;
  });
  devModal.addEventListener('click', (event) => {
    if (event.target === devModal) devModal.hidden = true;
  });

  for (const btn of devModal.querySelectorAll('[data-dev-arc]')) {
    btn.addEventListener('click', () => {
      applyState(devSetActiveArc(state, btn.dataset.devArc, catalog));
    });
  }
  for (const btn of devModal.querySelectorAll('[data-dev-m]')) {
    btn.addEventListener('click', () => {
      applyState(devSetHighestMilestone(state, Number(btn.dataset.devM), catalog));
    });
  }
  document.getElementById('dev-unlock-scars-btn').addEventListener('click', () => {
    applyState(devSetAllScarNodes(state, true, catalog));
  });
  document.getElementById('dev-clear-scars-btn').addEventListener('click', () => {
    applyState(devSetAllScarNodes(state, false, catalog));
  });
  document.getElementById('dev-apply-arc1-btn').addEventListener('click', () => {
    const ichorRaw = document.getElementById('dev-ichor-input').value;
    const stillRaw = document.getElementById('dev-stillness-input').value;
    const scarRaw = document.getElementById('dev-scar-input').value;
    applyState(
      devAssignArc1Resources(
        state,
        {
          ichor: ichorRaw !== '' ? Number(ichorRaw) : null,
          stillness: stillRaw !== '' ? Number(stillRaw) : null,
          availableScar: scarRaw !== '' ? Number(scarRaw) : null,
        },
        catalog,
      ),
    );
  });
  for (const btn of devModal.querySelectorAll('[data-dev-ichor]')) {
    btn.addEventListener('click', () => {
      applyState(devAssignArc1Resources(state, { ichor: Number(btn.dataset.devIchor) }, catalog));
    });
  }
  document.getElementById('dev-preset-stillness-btn').addEventListener('click', () => {
    applyState(devAssignArc1Resources(state, { stillness: state.stillness + 300 }, catalog));
  });
  document.getElementById('dev-preset-scar-btn').addEventListener('click', () => {
    applyState(
      devAssignArc1Resources(state, { availableScar: state.availableScarTissue + 40 }, catalog),
    );
  });
  document.getElementById('dev-apply-arc2-btn').addEventListener('click', () => {
    const leadsRaw = document.getElementById('dev-leads-input').value;
    const silenceRaw = document.getElementById('dev-silence-input').value;
    applyState(
      devAssignArc2Resources(
        state,
        {
          leads: leadsRaw !== '' ? Number(leadsRaw) : null,
          silence: silenceRaw !== '' ? Number(silenceRaw) : null,
        },
        catalog,
      ),
    );
  });
  for (const btn of devModal.querySelectorAll('[data-dev-leads]')) {
    btn.addEventListener('click', () => {
      applyState(devAssignArc2Resources(state, { leads: Number(btn.dataset.devLeads) }, catalog));
    });
  }
  for (const btn of devModal.querySelectorAll('[data-dev-ch]')) {
    btn.addEventListener('click', () => {
      applyState(
        devAssignArc2Resources(state, { completedChapters: Number(btn.dataset.devCh) }, catalog),
      );
    });
  }
  document.getElementById('dev-spawn-wall-btn').addEventListener('click', () => {
    applyState(devSpawnWallAnomaly(state));
  });
  document.getElementById('dev-spawn-desk-btn').addEventListener('click', () => {
    applyState(devSpawnDeskAnomaly(state));
  });
  document.getElementById('dev-skip-60s-btn').addEventListener('click', () => {
    let next = state;
    for (let i = 0; i < 60; i += 1) {
      next = tick(next, 1.0, catalog);
    }
    applyState(next);
  });
  document.getElementById('dev-wipe-btn').addEventListener('click', () => {
    clearSavedRaw();
    ui.resetViewState();
    applyState(devResetAll(catalog));
    devModal.hidden = true;
  });

  // Reset Modal
  wipeSaveBtn.addEventListener('click', () => {
    resetModal.hidden = false;
  });
  cancelResetBtn.addEventListener('click', () => {
    resetModal.hidden = true;
  });
  resetModal.addEventListener('click', (event) => {
    if (event.target === resetModal) resetModal.hidden = true;
  });
  confirmResetBtn.addEventListener('click', () => {
    clearSavedRaw();
    state = initialState(catalog);
    ui.resetViewState();
    ui.render(state);
    resetModal.hidden = true;
  });

  // Keyboard support
  window.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      ui.closeArchive();
      resetModal.hidden = true;
      devModal.hidden = true;
      return;
    }
    if (event.code === 'Space' && archiveModal.hidden && resetModal.hidden && devModal.hidden) {
      const tag = document.activeElement?.tagName;
      if (tag !== 'BUTTON' && tag !== 'INPUT' && tag !== 'TEXTAREA') {
        event.preventDefault();
        if (state.activeArc === GAME_ARCS.ARC_1_CHAMBER) {
          valveBtn.classList.add('is-pressed');
          triggerValvePress();
          window.setTimeout(() => valveBtn.classList.remove('is-pressed'), 95);
        } else if (state.activeArc === GAME_ARCS.ARC_2_INVESTIGATION) {
          inspectCaseBtn.classList.add('is-pressed');
          triggerCaseInspect();
          window.setTimeout(() => inspectCaseBtn.classList.remove('is-pressed'), 95);
        }
      }
    }
  });

  ui.render(state);

  let lastTickTime = performance.now();
  window.setInterval(() => {
    const now = performance.now();
    const dt = Math.min(5.0, Math.max(0, (now - lastTickTime) / 1000));
    lastTickTime = now;
    if (dt <= 0) return;

    const next = tick(state, dt, catalog);
    if (next !== state) {
      state = next;
      hasUnsavedChanges = true;
    }
    ui.render(state);

    autosaveAccumulator += dt;
    if (autosaveAccumulator >= 2.0) {
      autosaveAccumulator = 0;
      flushIfDirty();
    }
  }, 100);

  window.addEventListener('beforeunload', persistNow);
  window.addEventListener('pagehide', persistNow);
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) flushIfDirty();
  });

  let breathOrigin = performance.now();
  function renderCanvasFrame(now) {
    if (!document.hidden) {
      const duration = PULSE_DURATIONS_MS[Math.min(state.phase, 4)];
      const elapsed = (now - breathOrigin) % (duration * 2);
      const breath = elapsed <= duration ? elapsed / duration : 2 - elapsed / duration;
      const dropPhase = ((now - breathOrigin) % 2200) / 2200;

      if (state.activeArc === GAME_ARCS.ARC_1_CHAMBER) {
        drawChamberBackdrop(backdropCanvas, wallImage, state, breath, dropPhase);
        drawValveAndHands(valveHandsCanvas, state, breath);
        drawLeechClerks(leechCanvas, creatureCount(state, CREATURE_TYPES.LEECH_CLERK.id), breath);
        ui.renderAnimatedGlyphs(breath);
      } else if (state.activeArc === GAME_ARCS.ARC_2_INVESTIGATION) {
        drawInvestigatorDeskBackdrop(deskCanvas, state, breath, dropPhase);
        ui.renderAnimatedDeskGlyphs(breath);
      }
    }

    window.requestAnimationFrame(renderCanvasFrame);
  }
  window.requestAnimationFrame(renderCanvasFrame);
}

main();
