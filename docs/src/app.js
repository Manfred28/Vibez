/**
 * Bootstrap & lifecycle controller for the GitHub Pages playable multi-arc web version of
 * "The Tending of the Unnamed Organ".
 *
 * Loads progress-bound fragments strictly from `./assets/dialogues.json`, persists state
 * automatically in `localStorage`, wires the Clinical Dev Console, and drives the 100ms engine
 * tick + 60fps Canvas loop across Arc 1, Arc 2, and Arc 3.
 */

import {
  BREACH_RUPTURE_TYPES,
  CREATURE_TYPES,
  DESK_ANOMALY_TYPES,
  GAME_ARCS,
  WALL_ANOMALY_TYPES,
  activateValveClamp,
  arc2InspectYield,
  arc3ActiveStratum,
  arc3FloodLevel,
  arc3WrenchFractureYield,
  bottleJar,
  buyCreature,
  buyMaxCreature,
  buyRepeatableScarGraft,
  creatureCount,
  deserializeState,
  devAssignArc1Resources,
  devAssignArc2Resources,
  devAssignArc3Resources,
  devJumpArc3Finale,
  devResetAll,
  devResetArc2,
  devResetArc3,
  devSetActiveArc,
  devSetAllScarNodes,
  devSetArc2AllExhibits,
  devSetArc3Stratum,
  devSetHighestMilestone,
  devSpawnBreachRupture,
  devSpawnDeskAnomaly,
  devSpawnWallAnomaly,
  devToggleArc2Lamp,
  dissolveRedaction,
  dredgeSump,
  driveMemoryWedge,
  enterArc3Convergence,
  formatCompactDecimal,
  formatValveYield,
  initialState,
  inspectCaseFile,
  interactAnomaly,
  interactDeskAnomaly,
  manualTapYield,
  offerToThroat,
  performArc3FinalPull,
  performArc3Unmake,
  performBreachToArc2,
  performChoirLungGraft,
  performExcision,
  performGraft,
  pivotNextCase,
  pressValve,
  recruitContact,
  recruitMaxContact,
  serializeState,
  setEchoDiversion,
  tearBreachRupture,
  tick,
  toggleDrainSeal,
  toggleExhibitPair,
  toggleLamp,
  triggerReelScrub,
  unlockExhibit,
  unlockScarNode,
  wrenchArc3Breach,
} from './engine.js?v=3.16';
import {
  drawArc3BreachBackdrop,
  drawChamberBackdrop,
  drawCrimsonFloodCanvas,
  drawForegroundLampsAndSacrifice,
  drawInvestigatorDeskBackdrop,
  drawLeechClerks,
  drawValveAndHands,
} from './chamberCanvas.js?v=3.16';
import { createUiController } from './ui.js?v=3.16';
import {
  SOUND_ACTION_HINTS,
  SOUND_CUES,
  computeAmbientProfile,
  createWebAudioEngine,
  resolveTransitionCues,
} from './audio.js?v=3.16';
import {
  ANOMALY_ASSET_URLS,
  ARC2_STAMP_ASSET_URL,
  ARC3_BREACH_ASSET_URLS,
  ARC3_CONVERGENCE_ASSET_URL,
  CONTACT_ASSET_URLS,
  DESK_ANOMALY_ASSET_URLS,
  DESK_STAGE_ASSET_URLS,
  DIALOGUES_ASSET_URL,
  EXHIBIT_ASSET_URLS,
  GRAFT_ASSET_URLS,
  JAR_ASSET_URLS,
  RELIC_ASSET_URLS,
  SPECIMEN_ASSET_URLS,
  WALL_ASSET_URLS,
} from './version.js?v=3.16';

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
    const response = await fetch(DIALOGUES_ASSET_URL, { cache: 'no-cache' });
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
    phase1: preloadWallImage(WALL_ASSET_URLS.phase1),
    phase2: preloadWallImage(WALL_ASSET_URLS.phase2),
    phase3: preloadWallImage(WALL_ASSET_URLS.phase3),
    phase4: preloadWallImage(WALL_ASSET_URLS.phase4),
    excisionLate: preloadWallImage(WALL_ASSET_URLS.excisionLate),
  };
  // Preload photographic wall anomaly macro specimens so eruptions appear immediately on spawn
  const anomalyImages = {
    pale_tooth: preloadWallImage(ANOMALY_ASSET_URLS.pale_tooth),
    glass_eye: preloadWallImage(ANOMALY_ASSET_URLS.glass_eye),
    swollen_nodule: preloadWallImage(ANOMALY_ASSET_URLS.swollen_nodule),
    chitin_rib: preloadWallImage(ANOMALY_ASSET_URLS.chitin_rib),
  };
  void anomalyImages;
  // Preload all 6 wet-lab macro specimen jar photos for chamber wall shelves & JARS tab portraits
  const jarImages = {
    yesterdays_dust: preloadWallImage(JAR_ASSET_URLS.yesterdays_dust),
    unspent_breath: preloadWallImage(JAR_ASSET_URLS.unspent_breath),
    almost_a_name: preloadWallImage(JAR_ASSET_URLS.almost_a_name),
    salt_from_the_grout: preloadWallImage(JAR_ASSET_URLS.salt_from_the_grout),
    a_borrowed_pulse: preloadWallImage(JAR_ASSET_URLS.a_borrowed_pulse),
    the_uncounted_hour: preloadWallImage(JAR_ASSET_URLS.the_uncounted_hour),
  };
  void jarImages;
  // Preload 5 progressive Arc 2 nocturnal desk stage backdrops + 4 circular desk anomalies + 10 exhibits
  const deskImages = {
    case1: preloadWallImage(DESK_STAGE_ASSET_URLS.case1),
    case2: preloadWallImage(DESK_STAGE_ASSET_URLS.case2),
    case3: preloadWallImage(DESK_STAGE_ASSET_URLS.case3),
    case4: preloadWallImage(DESK_STAGE_ASSET_URLS.case4),
    case5: preloadWallImage(DESK_STAGE_ASSET_URLS.case5),
  };
  const deskAnomalyImages = Object.values(DESK_ANOMALY_ASSET_URLS).map(preloadWallImage);
  const exhibitImages = Object.values(EXHIBIT_ASSET_URLS).map(preloadWallImage);
  const extraPhotographicAssets = [
    ...Object.values(SPECIMEN_ASSET_URLS),
    ...Object.values(GRAFT_ASSET_URLS),
    ...Object.values(RELIC_ASSET_URLS),
    ...Object.values(CONTACT_ASSET_URLS),
    ARC2_STAMP_ASSET_URL,
    ARC3_CONVERGENCE_ASSET_URL,
    ...Object.values(ARC3_BREACH_ASSET_URLS),
  ].map(preloadWallImage);
  const arc3Images = {
    wallEarly: preloadWallImage(ARC3_BREACH_ASSET_URLS.wallEarly),
    wallMid: preloadWallImage(ARC3_BREACH_ASSET_URLS.wallMid),
    wallDeep: preloadWallImage(ARC3_BREACH_ASSET_URLS.wallDeep),
    crimsonFlood: preloadWallImage(ARC3_BREACH_ASSET_URLS.crimsonFlood),
  };
  void deskAnomalyImages;
  void exhibitImages;
  void extraPhotographicAssets;

  const backdropCanvas = document.getElementById('chamber-backdrop-canvas');
  const lampsForegroundCanvas = document.getElementById('lamps-foreground-canvas');
  const valveHandsCanvas = document.getElementById('valve-hands-canvas');
  const leechCanvas = document.getElementById('leech-overlay-canvas');
  const deskCanvas = document.getElementById('investigator-desk-canvas');
  const arc3BreachCanvas = document.getElementById('arc3-breach-canvas');
  const arc3FloodCanvas = document.getElementById('arc3-flood-canvas');
  const arc3SubmergedCanvas = document.getElementById('arc3-submerged-canvas');
  const valveWheelRotator = document.getElementById('valve-wheel-rotator');
  const arc3ValveRotator = document.getElementById('arc3-valve-rotator');

  const valveBtn = document.getElementById('valve-button');
  const lampToggleBtn = document.getElementById('lamp-toggle-btn');
  const inspectCaseBtn = document.getElementById('inspect-case-btn');
  const enterArc3Btn = document.getElementById('enter-arc3-btn');
  const arc3WrenchBtn = document.getElementById('arc3-wrench-btn');
  const arc3DriveBannerBtn = document.getElementById('arc3-drive-banner-btn');
  const arc3FinalPullBtn = document.getElementById('arc3-final-pull-btn');
  const arc3SubmergedArchiveBtn = document.getElementById('arc3-submerged-archive-btn');
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
  let arc3ValveAngleDeg = 0;
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
    onInspectRelic() {
      audio.unlock();
      audio.playCues([SOUND_CUES.RELIC_INSPECT], state.phase);
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
    onRecruitMaxContact(contactId) {
      applyState(recruitMaxContact(state, contactId, catalog), {
        action: SOUND_ACTION_HINTS.ARC2_CONTACT,
      });
    },
    onUnlockExhibit(exhibitId) {
      applyState(unlockExhibit(state, exhibitId, catalog), {
        action: SOUND_ACTION_HINTS.ARC2_DEDUCTION,
      });
    },
    onToggleExhibitPair(aId, bId) {
      applyState(toggleExhibitPair(state, aId, bId, catalog), {
        action: SOUND_ACTION_HINTS.ARC2_STRING_WIRE,
      });
    },
    onTriggerReelScrub() {
      applyState(triggerReelScrub(state, catalog), {
        action: SOUND_ACTION_HINTS.ARC2_REEL_SCRUB,
      });
    },
    onDissolveRedaction(redactionId) {
      applyState(dissolveRedaction(state, redactionId, catalog), {
        action: SOUND_ACTION_HINTS.ARC2_REDACTION_DISSOLVE,
      });
    },
    onInteractDeskAnomaly(anomalyId) {
      const anomaly = state.arc2.activeDeskAnomalies.find((a) => a.id === anomalyId);
      const meta = anomaly
        ? Object.values(DESK_ANOMALY_TYPES).find((t) => t.id === anomaly.type)
        : null;
      applyState(interactDeskAnomaly(state, anomalyId, catalog), {
        action: SOUND_ACTION_HINTS.ARC2_DESK_ANOMALY,
      });
      ui.triggerEventDrip(
        meta?.dripLine ?? 'Another damp slip pinned to the margin.',
        'CASE DESK',
        `desk_${anomaly?.type ?? 'anomaly'}`,
      );
    },
    onPivotNextCase() {
      applyState(pivotNextCase(state, catalog), { action: SOUND_ACTION_HINTS.ARC2_CHAPTER });
    },
    onPerformArc3Unmake(actId) {
      audio.unlock();
      applyState(performArc3Unmake(state, actId, catalog), {
        action: SOUND_ACTION_HINTS.ARC3_UNMAKE,
      });
    },
    onTearBreachRupture(ruptureId) {
      audio.unlock();
      const target = (state.arc3?.activeRuptures ?? []).find((r) => r.id === ruptureId);
      const meta = target
        ? Object.values(BREACH_RUPTURE_TYPES).find((t) => t.id === target.type)
        : null;
      applyState(tearBreachRupture(state, ruptureId, catalog), {
        action: SOUND_ACTION_HINTS.ARC3_RUPTURE,
      });
      if (meta?.dripLine) {
        ui.triggerEventDrip(meta.dripLine, 'BREACH', `rupture_${meta.id}`);
      }
    },
    onDriveMemoryWedge() {
      audio.unlock();
      applyState(driveMemoryWedge(state, catalog), {
        action: SOUND_ACTION_HINTS.ARC3_WEDGE,
      });
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

  function preventSpaceButtonActivation(event) {
    if (event.code === 'Space' || event.key === ' ') {
      event.preventDefault();
    }
  }

  valveBtn.addEventListener('click', triggerValvePress);
  valveBtn.addEventListener('keydown', preventSpaceButtonActivation);
  if (lampToggleBtn) {
    lampToggleBtn.addEventListener('click', () => {
      audio.unlock();
      applyState(toggleLamp(state, catalog), { action: SOUND_ACTION_HINTS.ARC2_LAMP_TOGGLE });
    });
  }
  inspectCaseBtn.addEventListener('click', triggerCaseInspect);
  inspectCaseBtn.addEventListener('keydown', preventSpaceButtonActivation);
  function triggerArc3Wrench() {
    if (state.activeArc !== GAME_ARCS.ARC_3_CONVERGENCE) return;
    audio.unlock();
    arc3ValveAngleDeg = (arc3ValveAngleDeg - 28) % 360000;
    const yieldLabel = formatCompactDecimal(arc3WrenchFractureYield(state));
    applyState(wrenchArc3Breach(state, catalog), {
      persist: false,
      action: SOUND_ACTION_HINTS.ARC3_WRENCH,
    });
    ui.spawnArc3Residue(yieldLabel);
  }

  enterArc3Btn.addEventListener('click', () =>
    applyState(enterArc3Convergence(state, catalog), { action: SOUND_ACTION_HINTS.ARC3_ENTER }),
  );
  if (arc3WrenchBtn) {
    arc3WrenchBtn.addEventListener('click', triggerArc3Wrench);
    arc3WrenchBtn.addEventListener('keydown', preventSpaceButtonActivation);
  }
  if (arc3DriveBannerBtn) {
    arc3DriveBannerBtn.addEventListener('click', () => {
      audio.unlock();
      applyState(driveMemoryWedge(state, catalog), { action: SOUND_ACTION_HINTS.ARC3_WEDGE });
    });
  }
  if (arc3FinalPullBtn) {
    arc3FinalPullBtn.addEventListener('click', () => {
      audio.unlock();
      applyState(performArc3FinalPull(state, catalog), {
        action: SOUND_ACTION_HINTS.ARC3_FINAL_PULL,
      });
    });
  }
  if (arc3SubmergedArchiveBtn) {
    arc3SubmergedArchiveBtn.addEventListener('click', () => ui.openArchive(state));
  }
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
  for (const btn of devModal.querySelectorAll('[data-dev-case]')) {
    btn.addEventListener('click', () => {
      applyState(
        devAssignArc2Resources(state, { caseAngle: Number(btn.dataset.devCase) }, catalog),
      );
    });
  }
  const devFileZeroBtn = document.getElementById('dev-arc2-f0-btn');
  if (devFileZeroBtn) {
    devFileZeroBtn.addEventListener('click', () => {
      applyState(devAssignArc2Resources(state, { completedChapters: 5 }, catalog));
    });
  }
  const devLampBtn = document.getElementById('dev-toggle-lamp-btn');
  if (devLampBtn) {
    devLampBtn.addEventListener('click', () => {
      applyState(devToggleArc2Lamp(state, catalog));
    });
  }
  const devAllExBtn = document.getElementById('dev-all-exhibits-btn');
  if (devAllExBtn) {
    devAllExBtn.addEventListener('click', () => {
      applyState(devSetArc2AllExhibits(state, true, catalog));
    });
  }
  const devResetArc2Btn = document.getElementById('dev-reset-arc2-btn');
  if (devResetArc2Btn) {
    devResetArc2Btn.addEventListener('click', () => {
      applyState(devResetArc2(state, catalog));
    });
  }
  const devApplyArc3Btn = document.getElementById('dev-apply-arc3-btn');
  if (devApplyArc3Btn) {
    devApplyArc3Btn.addEventListener('click', () => {
      const hemRaw = document.getElementById('dev-hemorrhage-input')?.value ?? '';
      const fracRaw = document.getElementById('dev-fracture-input')?.value ?? '';
      applyState(
        devAssignArc3Resources(
          state,
          {
            hemorrhage: hemRaw !== '' ? Number(hemRaw) : null,
            fracture: fracRaw !== '' ? Number(fracRaw) : null,
          },
          catalog,
        ),
      );
    });
  }
  for (const btn of devModal.querySelectorAll('[data-dev-stratum]')) {
    btn.addEventListener('click', () => {
      applyState(devSetArc3Stratum(state, Number(btn.dataset.devStratum), catalog));
    });
  }
  const devArc3FillBtn = document.getElementById('dev-arc3-fill-fracture-btn');
  if (devArc3FillBtn) {
    devArc3FillBtn.addEventListener('click', () => {
      const goal = arc3ActiveStratum(state.arc3).fractureGoal;
      applyState(devAssignArc3Resources(state, { fracture: goal }, catalog));
    });
  }
  const devArc3FinaleBtn = document.getElementById('dev-arc3-finale-btn');
  if (devArc3FinaleBtn) {
    devArc3FinaleBtn.addEventListener('click', () => {
      applyState(devJumpArc3Finale(state, catalog));
    });
  }
  const devSpawnRuptureBtn = document.getElementById('dev-spawn-rupture-btn');
  if (devSpawnRuptureBtn) {
    devSpawnRuptureBtn.addEventListener('click', () => {
      applyState(devSpawnBreachRupture(state));
    });
  }
  const devResetArc3Btn = document.getElementById('dev-reset-arc3-btn');
  if (devResetArc3Btn) {
    devResetArc3Btn.addEventListener('click', () => {
      applyState(devResetArc3(state, catalog));
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

  // Prevent accidental double-tap / gesture zoom during rapid tapping on iOS Safari
  document.addEventListener(
    'dblclick',
    (event) => {
      event.preventDefault();
    },
    { passive: false },
  );
  document.addEventListener(
    'gesturestart',
    (event) => {
      event.preventDefault();
    },
    { passive: false },
  );
  document.addEventListener(
    'gesturechange',
    (event) => {
      event.preventDefault();
    },
    { passive: false },
  );

  // Keyboard support (Escape closes modals; Space is intentionally not bound to clicking)
  window.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      ui.closeArchive();
      resetModal.hidden = true;
      devModal.hidden = true;
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
        drawInvestigatorDeskBackdrop(deskCanvas, deskImages, state, breath, dropPhase);
        ui.renderAnimatedDeskGlyphs(breath);
      } else if (state.activeArc === GAME_ARCS.ARC_3_CONVERGENCE) {
        const wavePhase = ((now - breathOrigin) % 4400) / 4400;
        const crimsonPulse = computeArterialPulse(now);
        const flood = arc3FloodLevel(state.arc3);
        if (arc3ValveRotator) {
          arc3ValveRotator.style.transform = `rotate(${(arc3ValveAngleDeg % 360).toFixed(2)}deg)`;
        }
        if (state.arc3?.submersionComplete) {
          drawCrimsonFloodCanvas(arc3SubmergedCanvas, 1.0, wavePhase, crimsonPulse, arc3Images.crimsonFlood);
        } else {
          drawArc3BreachBackdrop(arc3BreachCanvas, arc3Images, state, breath, wavePhase);
          drawCrimsonFloodCanvas(arc3FloodCanvas, flood, wavePhase, crimsonPulse, arc3Images.crimsonFlood);
        }
      }
    }

    window.requestAnimationFrame(renderCanvasFrame);
  }
  window.requestAnimationFrame(renderCanvasFrame);
}

main();
