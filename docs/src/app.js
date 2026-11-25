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
  WALL_ANOMALY_TYPES,
  activateValveClamp,
  arc2InspectYield,
  bottleJar,
  buyCreature,
  buyMaxCreature,
  buyRepeatableScarGraft,
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
  dredgeSump,
  enterArc3Convergence,
  formatCompactDecimal,
  formatValveYield,
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
  serializeState,
  setEchoDiversion,
  tick,
  toggleDrainSeal,
  unlockScarNode,
} from './engine.js?v=3.11';
import {
  drawChamberBackdrop,
  drawForegroundLampsAndSacrifice,
  drawInvestigatorDeskBackdrop,
  drawLeechClerks,
  drawValveAndHands,
} from './chamberCanvas.js?v=3.11';
import { createUiController } from './ui.js?v=3.11';
import {
  SOUND_ACTION_HINTS,
  computeAmbientProfile,
  createWebAudioEngine,
  resolveTransitionCues,
} from './audio.js?v=3.11';

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
    const response = await fetch('./assets/dialogues.json?v=3.11', { cache: 'no-cache' });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    return await response.json();
  } catch (err) {
    console.warn('Could not load ./assets/dialogues.json:', err);
    return { schemaVersion: 3, phases: [], entries: [] };
  }
}

function preloadWallImage(src) {
  const img = new Image();
  img.src = src;
  return img;
}

async function main() {
  const catalog = await loadDialogueCatalog();
  let state = deserializeState(readSavedRaw(), catalog) ?? initialState(catalog);

  const wallImages = {
    phase1: preloadWallImage('./assets/img_wall_phase1.jpg?v=3.11'),
    phase2: preloadWallImage('./assets/img_wall_fleshy.jpg?v=3.11'),
    phase3: preloadWallImage('./assets/img_wall_phase3.jpg?v=3.11'),
    phase4: preloadWallImage('./assets/img_wall_phase4.jpg?v=3.11'),
    excisionLate: preloadWallImage('./assets/img_wall_excision_late.jpg?v=3.11'),
  };
  // Preload photographic wall anomaly macro specimens so eruptions appear immediately on spawn
  const anomalyImages = {
    pale_tooth: preloadWallImage('./assets/img_anomaly_pale_tooth.jpg?v=3.11'),
    glass_eye: preloadWallImage('./assets/img_anomaly_glass_eye.jpg?v=3.11'),
    swollen_nodule: preloadWallImage('./assets/img_anomaly_swollen_nodule.jpg?v=3.11'),
    chitin_rib: preloadWallImage('./assets/img_anomaly_chitin_rib.jpg?v=3.11'),
  };
  void anomalyImages;
  // Preload all 6 wet-lab macro specimen jar photos for chamber wall shelves & JARS tab portraits
  const jarImages = {
    yesterdays_dust: preloadWallImage('./assets/img_jar_yesterdays_dust.jpg?v=3.11'),
    unspent_breath: preloadWallImage('./assets/img_jar_unspent_breath.jpg?v=3.11'),
    almost_a_name: preloadWallImage('./assets/img_jar_almost_a_name.jpg?v=3.11'),
    salt_from_the_grout: preloadWallImage('./assets/img_jar_salt_from_the_grout.jpg?v=3.11'),
    a_borrowed_pulse: preloadWallImage('./assets/img_jar_a_borrowed_pulse.jpg?v=3.11'),
    the_uncounted_hour: preloadWallImage('./assets/img_jar_the_uncounted_hour.jpg?v=3.11'),
  };
  void jarImages;

  const backdropCanvas = document.getElementById('chamber-backdrop-canvas');
  const lampsForegroundCanvas = document.getElementById('lamps-foreground-canvas');
  const valveHandsCanvas = document.getElementById('valve-hands-canvas');
  const leechCanvas = document.getElementById('leech-overlay-canvas');
  const deskCanvas = document.getElementById('investigator-desk-canvas');
  const valveWheelRotator = document.getElementById('valve-wheel-rotator');

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

  const audioMuteBtn = document.getElementById('audio-mute-btn');
  const audioMuteLabel = document.getElementById('audio-mute-label');

  const wipeSaveBtn = document.getElementById('wipe-save-btn');
  const resetModal = document.getElementById('reset-modal');
  const cancelResetBtn = document.getElementById('cancel-reset-btn');
  const confirmResetBtn = document.getElementById('confirm-reset-btn');

  const audio = createWebAudioEngine();
  audio.updateAmbience(computeAmbientProfile(state));

  function syncAudioToggleUi() {
    if (!audioMuteBtn) return;
    const isMutedNow = audio.isMuted();
    audioMuteBtn.classList.toggle('is-muted', isMutedNow);
    audioMuteBtn.setAttribute('aria-pressed', isMutedNow ? 'true' : 'false');
    audioMuteBtn.setAttribute(
      'aria-label',
      isMutedNow ? 'Unmute sound effects and room ambience' : 'Mute sound effects and room ambience',
    );
    if (audioMuteLabel) {
      audioMuteLabel.textContent = isMutedNow ? 'MUTE' : 'SFX';
    }
  }
  syncAudioToggleUi();

  const unlockAudioOnce = () => {
    audio.unlock();
  };
  window.addEventListener('pointerdown', unlockAudioOnce, { passive: true });
  window.addEventListener('keydown', unlockAudioOnce, { passive: true });

  if (audioMuteBtn) {
    audioMuteBtn.addEventListener('click', () => {
      audio.toggleMuted();
      syncAudioToggleUi();
    });
  }

  let autosaveAccumulator = 0;
  let hasUnsavedChanges = false;
  let valveAngleDeg = 0;
  let handCyclePhase = 0;
  let leechPatrolPhase = 0;
  let lastEchoPulses = state.echoPulsesTriggered;
  let echoRippleStartMs = -1;
  let lastSacrificePulse = state.lastSacrificePulse ?? 0;
  let sacrificeStartMs = -1;
  let sacrificeKind = 'tendril';

  function observeStateEvents(next) {
    if (next.echoPulsesTriggered > lastEchoPulses) {
      echoRippleStartMs = performance.now();
      ui.triggerEventDrip('Something behind the mortar answers in kind.', 'APERTURE', 'echo_answer');
    }
    lastEchoPulses = next.echoPulsesTriggered;

    const pulse = next.lastSacrificePulse ?? 0;
    if (pulse > lastSacrificePulse) {
      sacrificeStartMs = performance.now();
      sacrificeKind = next.lastSacrificeKind || 'tendril';
      const msg =
        sacrificeKind === 'choir_lung'
          ? 'Two lamps and a tendril collapse inward into wet breath.'
          : 'Two hands and two leeches slacken, braided into one hum.';
      ui.triggerEventDrip(msg, 'GRAFT', `graft_${sacrificeKind}`);
    }
    lastSacrificePulse = pulse;
  }

  function persistNow() {
    writeSavedRaw(serializeState(state));
    hasUnsavedChanges = false;
  }

  function flushIfDirty() {
    if (hasUnsavedChanges) {
      persistNow();
    }
  }

  function dispatchAudioTransition(prev, next, action) {
    const cues = resolveTransitionCues(prev, next, action);
    if (cues.length > 0) {
      audio.playCues(cues, next.phase);
    }
    audio.updateAmbience(computeAmbientProfile(next));
  }

  function applyState(next, { persist = true, action = SOUND_ACTION_HINTS.DEV } = {}) {
    const prev = state;
    state = next;
    if (next !== prev) {
      dispatchAudioTransition(prev, next, action);
    }
    if (persist) {
      persistNow();
    } else {
      hasUnsavedChanges = true;
    }
    ui.render(state);
    observeStateEvents(next);
  }

  const ui = createUiController({
    onBuyCreature(typeId) {
      applyState(buyCreature(state, typeId, catalog), { action: SOUND_ACTION_HINTS.CREATURE_BUY });
    },
    onBuyMaxCreature(typeId) {
      applyState(buyMaxCreature(state, typeId, catalog), {
        action: SOUND_ACTION_HINTS.CREATURE_BUY,
      });
    },
    onBottleJar(jarId) {
      applyState(bottleJar(state, jarId, catalog), { action: SOUND_ACTION_HINTS.JAR_BOTTLE });
    },
    onPerformGraft() {
      applyState(performGraft(state, catalog), { action: SOUND_ACTION_HINTS.GRAFT_PERFORM });
    },
    onPerformChoirLung() {
      applyState(performChoirLungGraft(state, catalog), {
        action: SOUND_ACTION_HINTS.GRAFT_PERFORM,
      });
    },
    onUnlockScarNode(nodeId) {
      applyState(unlockScarNode(state, nodeId, catalog), {
        action: SOUND_ACTION_HINTS.SCAR_UNLOCK,
      });
    },
    onBuyRepeatableScarGraft(graftId) {
      applyState(buyRepeatableScarGraft(state, graftId, catalog), {
        action: SOUND_ACTION_HINTS.SCAR_UNLOCK,
      });
    },
    onToggleDrainSeal() {
      applyState(toggleDrainSeal(state, catalog), { action: SOUND_ACTION_HINTS.DRAIN_TOGGLE });
    },
    onDredgeSump() {
      applyState(dredgeSump(state, catalog), { action: SOUND_ACTION_HINTS.SUMP_DREDGE });
      ui.triggerEventDrip(
        'Something wet and cold comes up from beneath the grate.',
        'SUMP',
        'sump_dredge',
      );
    },
    onSetEchoDiversion(pct) {
      applyState(setEchoDiversion(state, pct, catalog), {
        action: SOUND_ACTION_HINTS.ECHO_DIVERSION,
      });
    },
    onActivateValveClamp() {
      applyState(activateValveClamp(state, catalog), { action: SOUND_ACTION_HINTS.CLAMP_ENGAGE });
      ui.triggerEventDrip(
        'The iron bolts shut; pressure thickens behind the tiles.',
        'CLAMP',
        'valve_clamp',
      );
    },
    onOfferToThroat(lineId) {
      applyState(offerToThroat(state, lineId, catalog), {
        action: SOUND_ACTION_HINTS.THROAT_OFFER,
      });
    },
    onInteractAnomaly(anomalyId) {
      const anomaly = state.activeAnomalies.find((a) => a.id === anomalyId);
      const meta = anomaly
        ? Object.values(WALL_ANOMALY_TYPES).find((t) => t.id === anomaly.type)
        : null;
      applyState(interactAnomaly(state, anomalyId, catalog), {
        action: SOUND_ACTION_HINTS.ANOMALY_INTERACT,
      });
      if (anomaly && meta?.dripLine) {
        ui.triggerEventDrip(meta.dripLine, 'CHAMBER', `anomaly_${anomaly.type}`);
      }
    },
    onPerformExcision() {
      applyState(performExcision(state, catalog), { action: SOUND_ACTION_HINTS.EXCISION });
      window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    onPerformBreachToArc2() {
      applyState(performBreachToArc2(state, catalog), {
        action: SOUND_ACTION_HINTS.BREACH_TO_ARC2,
      });
      window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    onRecruitContact(contactId) {
      applyState(recruitContact(state, contactId, catalog), {
        action: SOUND_ACTION_HINTS.ARC2_CONTACT,
      });
    },
    onPinDeduction(deductionId) {
      applyState(pinDeduction(state, deductionId, catalog), {
        action: SOUND_ACTION_HINTS.ARC2_DEDUCTION,
      });
    },
    onInteractDeskAnomaly(anomalyId) {
      const anomaly = state.arc2.activeDeskAnomalies.find((a) => a.id === anomalyId);
      applyState(interactDeskAnomaly(state, anomalyId, catalog), {
        action: SOUND_ACTION_HINTS.ARC2_DESK_ANOMALY,
      });
      ui.triggerEventDrip(
        'Another damp slip pinned to the margin.',
        'CASE DESK',
        `desk_${anomaly?.type ?? 'anomaly'}`,
      );
    },
    onConnectNextChapter() {
      applyState(connectNextChapter(state, catalog), { action: SOUND_ACTION_HINTS.ARC2_CHAPTER });
    },
    onRequestRender() {
      ui.render(state);
    },
  });

  function triggerValvePress() {
    if (state.activeArc !== GAME_ARCS.ARC_1_CHAMBER) return;
    if (state.clampRemainingSeconds > 0) return;
    audio.unlock();
    valveAngleDeg = (valveAngleDeg + 24) % 360000;
    const yieldLabel = formatValveYield(manualTapYield(state));
    applyState(pressValve(state, catalog), {
      persist: false,
      action: SOUND_ACTION_HINTS.VALVE_PRESS,
    });
    ui.spawnResidue(yieldLabel);
  }

  function triggerCaseInspect() {
    if (state.activeArc !== GAME_ARCS.ARC_2_INVESTIGATION) return;
    audio.unlock();
    const yieldLabel = formatCompactDecimal(arc2InspectYield(state.arc2));
    applyState(inspectCaseFile(state, catalog), {
      persist: false,
      action: SOUND_ACTION_HINTS.ARC2_STAMP,
    });
    ui.spawnArc2Residue(yieldLabel);
  }

  valveBtn.addEventListener('click', triggerValvePress);
  inspectCaseBtn.addEventListener('click', triggerCaseInspect);
  enterArc3Btn.addEventListener('click', () =>
    applyState(enterArc3Convergence(state, catalog), { action: SOUND_ACTION_HINTS.ARC3_ENTER }),
  );
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
    audio.updateAmbience(computeAmbientProfile(state));
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

    const prev = state;
    const next = tick(state, dt, catalog);
    const changed = next !== state;
    if (changed) {
      state = next;
      dispatchAudioTransition(prev, next, SOUND_ACTION_HINTS.TICK);
      hasUnsavedChanges = true;
    }
    ui.render(state);
    if (changed) {
      observeStateEvents(next);
    }

    autosaveAccumulator += dt;
    if (autosaveAccumulator >= 2.0) {
      autosaveAccumulator = 0;
      flushIfDirty();
    }
  }, 100);

  window.addEventListener('beforeunload', persistNow);
  window.addEventListener('pagehide', persistNow);
  document.addEventListener('visibilitychange', () => {
    audio.setForegroundActive(!document.hidden);
    if (document.hidden) flushIfDirty();
  });

  let breathOrigin = performance.now();
  let lastFrameMs = breathOrigin;
  function computeArterialPulse(nowMs) {
    // Cardiac systolic/diastolic double-beat wave every 1450ms in Phase 4
    const phase = ((nowMs - breathOrigin) % 1450) / 1450;
    const beat1 = Math.exp(-Math.pow((phase - 0.14) / 0.055, 2));
    const beat2 = 0.62 * Math.exp(-Math.pow((phase - 0.30) / 0.065, 2));
    return Math.min(1, beat1 + beat2);
  }

  function renderCanvasFrame(now) {
    const dtSec = Math.min(0.1, Math.max(0, (now - lastFrameMs) / 1000));
    lastFrameMs = now;

    if (!document.hidden) {
      const duration = PULSE_DURATIONS_MS[Math.min(state.phase, 4)];
      const elapsed = (now - breathOrigin) % (duration * 2);
      const breath = elapsed <= duration ? elapsed / duration : 2 - elapsed / duration;
      const dropPhase = ((now - breathOrigin) % 2200) / 2200;
      const wallCreep = ((now - breathOrigin) % 7200) / 7200;
      const arterialPulse = state.phase >= 4 ? computeArterialPulse(now) : 0;

      if (state.activeArc === GAME_ARCS.ARC_1_CHAMBER) {
        const sutureCount = creatureCount(state, CREATURE_TYPES.SUTURE_HAND.id);
        const leechCount = creatureCount(state, CREATURE_TYPES.LEECH_CLERK.id);
        if (sutureCount > 0 && state.clampRemainingSeconds <= 0) {
          const degPerSec = Math.min(110, 12 + 26 * Math.log(1 + sutureCount));
          valveAngleDeg = (valveAngleDeg + degPerSec * dtSec) % 360000;
          const handCyclesPerSec = Math.min(2.6, 0.45 + 0.55 * Math.log(1 + sutureCount));
          handCyclePhase = (handCyclePhase + handCyclesPerSec * dtSec) % 1000;
        }
        if (valveWheelRotator) {
          valveWheelRotator.style.transform = `rotate(${(valveAngleDeg % 360).toFixed(2)}deg)`;
        }
        if (leechCount > 0) {
          leechPatrolPhase = (leechPatrolPhase + 0.095 * dtSec) % 1000;
        }

        const echoRipple =
          echoRippleStartMs > 0 ? Math.min(1, (now - echoRippleStartMs) / 1100) : 0;
        if (echoRipple >= 1) echoRippleStartMs = -1;

        const sacrificeProgress =
          sacrificeStartMs > 0 ? Math.min(1, (now - sacrificeStartMs) / 1400) : 0;
        if (sacrificeProgress >= 1) sacrificeStartMs = -1;

        drawChamberBackdrop(
          backdropCanvas,
          wallImages,
          null,
          state,
          breath,
          dropPhase,
          wallCreep,
          arterialPulse,
          echoRipple,
        );
        drawForegroundLampsAndSacrifice(
          lampsForegroundCanvas,
          state,
          breath,
          dropPhase,
          sacrificeProgress,
          sacrificeKind,
        );
        drawValveAndHands(valveHandsCanvas, state, breath, handCyclePhase);
        drawLeechClerks(leechCanvas, leechCount, breath, leechPatrolPhase);
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
