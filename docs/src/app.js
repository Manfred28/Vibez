/**
 * Bootstrap & lifecycle controller for the GitHub Pages playable web version of
 * "The Tending of the Unnamed Organ".
 *
 * Loads progress-bound fragments strictly from `./assets/dialogues.json`, persists state
 * automatically in `localStorage`, and drives the 100ms engine tick + 60fps Canvas loop.
 */

import {
  CREATURE_TYPES,
  bottleJar,
  buyCreature,
  creatureCount,
  deserializeState,
  formatCompactDecimal,
  initialState,
  interactAnomaly,
  manualTapYield,
  performExcision,
  performGraft,
  pressValve,
  serializeState,
  tick,
} from './engine.js';
import {
  drawChamberBackdrop,
  drawLeechClerks,
  drawValveAndHands,
} from './chamberCanvas.js';
import { createUiController } from './ui.js';

const SAVE_STORAGE_KEY = 'unnamed_organ_save_v1';
const PULSE_DURATIONS_MS = [5200, 4400, 3600, 3000, 2400];

function readSavedRaw() {
  try {
    return window.localStorage.getItem(SAVE_STORAGE_KEY);
  } catch {
    return null;
  }
}

function writeSavedRaw(raw) {
  try {
    window.localStorage.setItem(SAVE_STORAGE_KEY, raw);
  } catch {
    // Private browsing or blocked storage — continue safely in memory.
  }
}

function clearSavedRaw() {
  try {
    window.localStorage.removeItem(SAVE_STORAGE_KEY);
  } catch {
    // Ignore storage errors.
  }
}

async function loadDialogueCatalog() {
  try {
    const response = await fetch('./assets/dialogues.json', { cache: 'no-cache' });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    return await response.json();
  } catch (err) {
    console.warn('Could not load ./assets/dialogues.json:', err);
    return { schemaVersion: 2, phases: [], entries: [] };
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
  const valveBtn = document.getElementById('valve-button');
  const openArchiveBtn = document.getElementById('open-archive-btn');
  const closeArchiveBtn = document.getElementById('close-archive-btn');
  const archiveModal = document.getElementById('archive-modal');
  const wipeSaveBtn = document.getElementById('wipe-save-btn');
  const resetModal = document.getElementById('reset-modal');
  const cancelResetBtn = document.getElementById('cancel-reset-btn');
  const confirmResetBtn = document.getElementById('confirm-reset-btn');

  let autosaveAccumulator = 0;

  function persistNow() {
    writeSavedRaw(serializeState(state));
  }

  const ui = createUiController({
    onBuyCreature(typeId) {
      state = buyCreature(state, typeId, catalog);
      persistNow();
      ui.render(state);
    },
    onBottleJar(jarId) {
      state = bottleJar(state, jarId, catalog);
      persistNow();
      ui.render(state);
    },
    onPerformGraft() {
      state = performGraft(state, catalog);
      persistNow();
      ui.render(state);
    },
    onInteractAnomaly(anomalyId) {
      state = interactAnomaly(state, anomalyId, catalog);
      persistNow();
      ui.render(state);
    },
    onPerformExcision() {
      state = performExcision(state, catalog);
      persistNow();
      ui.render(state);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    onRequestRender() {
      ui.render(state);
    },
  });

  function triggerValvePress() {
    const yieldLabel = formatCompactDecimal(manualTapYield(state));
    state = pressValve(state, catalog);
    ui.spawnResidue(yieldLabel);
    ui.render(state);
    persistNow();
  }

  valveBtn.addEventListener('click', triggerValvePress);

  openArchiveBtn.addEventListener('click', () => ui.openArchive(state));
  closeArchiveBtn.addEventListener('click', () => ui.closeArchive());
  archiveModal.addEventListener('click', (event) => {
    if (event.target === archiveModal) ui.closeArchive();
  });

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

  // Keyboard support: Space presses the valve when modals are closed; Escape closes modals
  window.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      ui.closeArchive();
      resetModal.hidden = true;
      return;
    }
    if (event.code === 'Space' && archiveModal.hidden && resetModal.hidden) {
      const tag = document.activeElement?.tagName;
      if (tag !== 'BUTTON' && tag !== 'INPUT' && tag !== 'TEXTAREA') {
        event.preventDefault();
        valveBtn.classList.add('is-pressed');
        triggerValvePress();
        window.setTimeout(() => valveBtn.classList.remove('is-pressed'), 95);
      }
    }
  });

  // Initial render
  ui.render(state);

  // 100ms deterministic tick loop (clamped so background-tab resumes stay smooth)
  let lastTickTime = performance.now();
  window.setInterval(() => {
    const now = performance.now();
    const dt = Math.min(5.0, Math.max(0, (now - lastTickTime) / 1000));
    lastTickTime = now;
    if (dt <= 0) return;

    state = tick(state, dt, catalog);
    ui.render(state);

    autosaveAccumulator += dt;
    if (autosaveAccumulator >= 2.0) {
      autosaveAccumulator = 0;
      persistNow();
    }
  }, 100);

  window.addEventListener('beforeunload', persistNow);

  // 60fps breathing & organic Canvas loop
  let breathOrigin = performance.now();
  function renderCanvasFrame(now) {
    const duration = PULSE_DURATIONS_MS[Math.min(state.phase, 4)];
    const elapsed = (now - breathOrigin) % (duration * 2);
    const breath = elapsed <= duration ? elapsed / duration : 2 - elapsed / duration;

    drawChamberBackdrop(backdropCanvas, wallImage, state, breath);
    drawValveAndHands(valveHandsCanvas, state, breath);
    drawLeechClerks(leechCanvas, creatureCount(state, CREATURE_TYPES.LEECH_CLERK.id), breath);
    ui.renderAnimatedGlyphs(breath);

    window.requestAnimationFrame(renderCanvasFrame);
  }
  window.requestAnimationFrame(renderCanvasFrame);
}

main();
