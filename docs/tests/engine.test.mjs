import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import {
  ARC2_BALANCE,
  CASE_DEDUCTIONS,
  CREATURE_TYPES,
  CULT_DOSSIER_LINES,
  DESK_ANOMALY_TYPES,
  DREDGED_RELICS,
  EXCISION_MILESTONES,
  GAME_ARCS,
  INVESTIGATOR_CONTACTS,
  ORGAN_BALANCE,
  REPEATABLE_SCAR_GRAFTS,
  SCAR_NODES,
  SPECIMEN_JARS,
  WALL_ANOMALY_TYPES,
  activateValveClamp,
  arc2CanConnectNextChapter,
  bottleJar,
  buyCreature,
  buyMaxCreature,
  buyRepeatableScarGraft,
  canAffordChoirLung,
  canAffordGraft,
  canBreachToArc2,
  canBuyRepeatableGraft,
  canClearNextMilestone,
  canDredgeSump,
  canExcise,
  canOfferToThroat,
  canUnlockNode,
  catatonicTideMultiplier,
  connectNextChapter,
  creatureCost,
  creatureCount,
  currentBaseScarHarvest,
  cycleLifetimeIchorDisplay,
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
  echoPulseIntervalSec,
  effectiveCreatureIchorPerSec,
  effectiveJarCost,
  effectiveLampStillnessPerSec,
  enterArc3Convergence,
  formatValveYield,
  gazeMultiplier,
  graftCooldownDurationSec,
  graftStillnessCost,
  hasExcessMilestoneMass,
  ichorDisplay,
  ichorPerSecond,
  initialState,
  inspectCaseFile,
  interactAnomaly,
  interactDeskAnomaly,
  isApparatusUnlocked,
  isColonyProliferationUnlocked,
  isJarUnlocked,
  isJarsAndGraftUnlocked,
  isRepeatableGraftsUnlocked,
  jarEffectBoost,
  manualTapYield,
  marrowBloomDurationSec,
  massRequiredForScarCount,
  maxAffordableCreatureQuote,
  nextBaseScarMassThreshold,
  nextScarProgress,
  offerToThroat,
  pendingScarTissue,
  performBreachToArc2,
  performChoirLungGraft,
  performExcision,
  performGraft,
  phaseForCount,
  pinDeduction,
  pressValve,
  progressWithinPhase,
  rawIchorPerSecond,
  recruitContact,
  relicAmplifier,
  repeatableCost,
  repeatableRank,
  scarHarvestMultiplier,
  scarMultiplier,
  scarStillnessMultiplier,
  sedimentCostMultiplier,
  serializeState,
  setEchoDiversion,
  stillnessCostMultiplier,
  stillnessDisplay,
  stillnessIdleDelaySec,
  stillnessPerSecond,
  tick,
  toggleDrainSeal,
  totalCreatureIchorPerSec,
  unlockScarNode,
  valveClampDurationSec,
} from '../src/engine.js';
import {
  REPEAT_EVENT_DRIP_CHANCE,
  applyDripMotionProfile,
  arc1UnlockKeys,
  arc2UnlockKeys,
  computeDripMotionProfile,
  isArc1TabReady,
  isArc2TabReady,
  shouldAlertArc1Tab,
  shouldAlertArc2Tab,
  shouldTriggerEventDrip,
} from '../src/ui.js';
import {
  AUDIO_MUTE_STORAGE_KEY,
  IRON_MODE_RATIOS,
  SOUND_ACTION_HINTS,
  SOUND_CUES,
  computeAmbientProfile,
  createWebAudioEngine,
  readMutedPreference,
  resolveTransitionCues,
  writeMutedPreference,
} from '../src/audio.js';
import { computeWallLayerAlphas, getWeepingLampLayout } from '../src/chamberCanvas.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, '..', '..');
const docsDir = path.resolve(__dirname, '..');
const webDialoguesPath = path.join(docsDir, 'assets', 'dialogues.json');
const androidDialoguesPath = path.join(repoRoot, 'app', 'src', 'main', 'assets', 'dialogues.json');

const realCatalog = JSON.parse(fs.readFileSync(webDialoguesPath, 'utf8'));

test('GitHub Pages static bundle integrity and Android dialogues.json parity', () => {
  const requiredFiles = [
    '.nojekyll',
    'index.html',
    'styles.css',
    'src/engine.js',
    'src/chamberCanvas.js',
    'src/ui.js',
    'src/audio.js',
    'src/app.js',
    'assets/dialogues.json',
    'assets/img_wall_phase1.jpg',
    'assets/img_wall_fleshy.jpg',
    'assets/img_wall_phase3.jpg',
    'assets/img_wall_phase4.jpg',
    'assets/img_wall_excision_late.jpg',
    'assets/img_anomaly_pale_tooth.jpg',
    'assets/img_anomaly_glass_eye.jpg',
    'assets/img_anomaly_swollen_nodule.jpg',
    'assets/img_anomaly_chitin_rib.jpg',
    'assets/img_recessed_valve.jpg',
    'assets/img_suture_hand.jpg',
    'assets/img_leech_clerk.jpg',
    'assets/icon.png',
    'assets/jetbrains_mono.ttf',
  ];

  for (const rel of requiredFiles) {
    const abs = path.join(docsDir, rel);
    assert.ok(fs.existsSync(abs), `Missing required GitHub Pages file: docs/${rel}`);
  }

  // Verify all 5 progressive chamber wall JPEGs + 4 photographic wall anomaly JPEGs match Android byte-for-byte
  for (const imgFile of [
    'img_wall_phase1.jpg',
    'img_wall_fleshy.jpg',
    'img_wall_phase3.jpg',
    'img_wall_phase4.jpg',
    'img_wall_excision_late.jpg',
    'img_anomaly_pale_tooth.jpg',
    'img_anomaly_glass_eye.jpg',
    'img_anomaly_swollen_nodule.jpg',
    'img_anomaly_chitin_rib.jpg',
  ]) {
    const webBuf = fs.readFileSync(path.join(docsDir, 'assets', imgFile));
    const androidBuf = fs.readFileSync(
      path.join(repoRoot, 'app', 'src', 'main', 'res', 'drawable', imgFile),
    );
    assert.ok(webBuf.length > 5000, `${imgFile} must be a real photographic asset`);
    assert.ok(webBuf.equals(androidBuf), `docs/assets/${imgFile} must match Android res/drawable/${imgFile}`);
  }

  // Verify exact dialogue parity with canonical Android dialogues.json
  const webRaw = fs.readFileSync(webDialoguesPath, 'utf8').trim();
  const androidRaw = fs.readFileSync(androidDialoguesPath, 'utf8').trim();
  assert.equal(webRaw, androidRaw, 'docs/assets/dialogues.json must match app/src/main/assets/dialogues.json');
  assert.ok(realCatalog.schemaVersion >= 3);
  assert.equal(realCatalog.phases.length, 7);
  assert.ok(realCatalog.entries.length >= 45);

  // Verify root index.html redirects to ./docs/ when GitHub Pages serves from /(root)
  const rootIndexPath = path.join(repoRoot, 'index.html');
  assert.ok(fs.existsSync(rootIndexPath), 'Missing root index.html redirect for default GitHub Pages root');
  const rootHtml = fs.readFileSync(rootIndexPath, 'utf8');
  assert.ok(rootHtml.includes('./docs/'), 'Root index.html must redirect to ./docs/');

  // Verify index.html uses relative ./ paths so GitHub Pages subpaths work
  const html = fs.readFileSync(path.join(docsDir, 'index.html'), 'utf8');
  assert.ok(html.includes('href="./styles.css?v=3.8"'));
  assert.ok(html.includes('src="./src/app.js?v=3.8"'));
  assert.ok(html.includes('src="./assets/img_recessed_valve.jpg"'));
  assert.ok(html.includes('THE COUNTING ROOM'));
  assert.ok(html.includes('id="phase-progress-fill"'), 'Diegetic progress bar must be restored in header');
  assert.ok(html.includes('NEXT: UNCANNY DRIFT · 0 / 75'), 'Header must display diegetic next stage progress');
  assert.ok(!html.includes('PHASE 00'), 'User-facing HTML must not show the word PHASE');
  assert.ok(!html.includes('id="phase-badge"'), 'User-facing HTML must not show a P0..P4 phase badge');
  assert.ok(!html.includes('id="arc2-chapter-badge"'), 'User-facing HTML must not show a CH 0/4 badge');
  assert.ok(!html.includes('ARC 02 //'), 'User-facing HTML must not show ARC 02 prefix');
  assert.ok(!html.includes('ARC 03 //'), 'User-facing HTML must not show ARC 03 prefix');
  assert.ok(!html.includes('NEXT FRACTURE'), 'Upcoming unlock thresholds must not use clinical labels');
  assert.ok(!html.includes('NEXT CHAPTER'), 'Upcoming Inquiry chapters must be hidden');
  assert.ok(html.includes('id="audio-mute-btn"'), 'Web UI must include compact audio mute toggle');
  assert.ok(html.includes('id="open-dev-btn"'), 'Web UI must include Clinical Dev Console trigger');
  assert.ok(html.includes('id="arc2-shell"'), 'Web UI must include Arc 2 Investigator Desk shell');
  assert.ok(!html.includes('href="/styles.css"'), 'Avoid root-absolute asset paths that break GitHub Pages subpaths');
});

test('initial state starts bare in Phase 0 and valve press triggers first fragment', () => {
  const start = initialState(realCatalog);
  assert.equal(start.activeArc, GAME_ARCS.ARC_1_CHAMBER);
  assert.equal(ichorDisplay(start), 0);
  assert.equal(start.phase, 0);
  assert.equal(start.phaseName, 'The Counting Room');
  assert.equal(start.nextPhaseThreshold, 75);
  assert.equal(isApparatusUnlocked(start), false);
  assert.equal(isJarsAndGraftUnlocked(start), false);
  assert.equal(canExcise(start), false);
  assert.equal(start.activeDialogue, null);

  const afterTap = pressValve({ ...start, secondsSinceLastTap: 5, isStillnessActive: true }, realCatalog);
  assert.equal(ichorDisplay(afterTap), 1);
  assert.equal(afterTap.isStillnessActive, false);
  assert.equal(afterTap.activeDialogue?.id, 'dlg_p0_first_press');
  assert.equal(afterTap.discoveredDialogues.length, 1);
});

test('organisms purchase, scaling cost, Weeping Lamp pricing, and Stillness 3s idle behavior', () => {
  let state = { ...initialState(realCatalog), ichor: 50, cycleLifetimeIchor: 50 };
  assert.equal(creatureCost(state, CREATURE_TYPES.WEEPING_LAMP.id), 1190);
  assert.equal(CREATURE_TYPES.WEEPING_LAMP.unlockIchorThreshold, 1000);

  state = buyCreature(state, CREATURE_TYPES.SUTURE_HAND.id, realCatalog);
  assert.equal(creatureCount(state, CREATURE_TYPES.SUTURE_HAND.id), 1);
  assert.equal(state.ichor, 25);
  assert.equal(state.activeDialogue?.id, 'dlg_p0_suture_hand');
  assert.ok(creatureCost(state, CREATURE_TYPES.SUTURE_HAND.id) > 25);

  // Test Weeping Lamps 3s Stillness trigger & 2x Ichor surge
  let lampState = {
    ...initialState(realCatalog),
    ichor: 1600,
    cycleLifetimeIchor: 1600,
    creatures: { [CREATURE_TYPES.WEEPING_LAMP.id]: 2 },
  };
  lampState = tick(lampState, 2.0, realCatalog);
  assert.equal(lampState.isStillnessActive, false);
  assert.equal(lampState.stillness, 0);
  assert.ok(Math.abs(lampState.ichor - 1624) < 0.001);

  lampState = tick(lampState, 15.0, realCatalog);
  assert.equal(lampState.isStillnessActive, true);
  assert.ok(Math.abs(lampState.stillness - 13.5) < 0.001);
  assert.ok(Math.abs(lampState.ichor - 1984) < 0.001);
  assert.equal(lampState.activeDialogue?.id, 'dlg_p2_stillness');

  const tapped = pressValve(lampState, realCatalog);
  assert.equal(tapped.isStillnessActive, false);
});

test('per-organism effective yields match aggregate ichorPerSecond and stillnessPerSecond across all multipliers', () => {
  const state = {
    ...initialState(realCatalog),
    creatures: {
      [CREATURE_TYPES.SUTURE_HAND.id]: 3,
      [CREATURE_TYPES.LEECH_CLERK.id]: 2,
      [CREATURE_TYPES.WEEPING_LAMP.id]: 2,
    },
    ownedJars: [SPECIMEN_JARS.UNSPENT_BREATH.id, SPECIMEN_JARS.ALMOST_A_NAME.id],
    isStillnessActive: true,
    gazeRemainingSeconds: 10.0,
    scarTissue: 2,
    highestExcisionMilestone: 0,
  };

  // Root-softcapped resonance: 1.0 + 0 * 0.20 + sqrt(2) * 0.25
  const expectedScarIchor = 1.0 + Math.sqrt(2) * ORGAN_BALANCE.SCAR_RESONANCE_SQRT_FACTOR;
  const summedIchor =
    totalCreatureIchorPerSec(state, CREATURE_TYPES.SUTURE_HAND.id) +
    totalCreatureIchorPerSec(state, CREATURE_TYPES.LEECH_CLERK.id) +
    totalCreatureIchorPerSec(state, CREATURE_TYPES.WEEPING_LAMP.id);

  assert.ok(Math.abs(ichorPerSecond(state) - summedIchor) < 0.0001);
  assert.ok(Math.abs(stillnessPerSecond(state) - 2 * effectiveLampStillnessPerSec(state)) < 0.0001);
  assert.ok(
    Math.abs(
      effectiveCreatureIchorPerSec(state, CREATURE_TYPES.SUTURE_HAND.id) -
        0.6 * ORGAN_BALANCE.UNSPENT_BREATH_MULTIPLIER * expectedScarIchor * ORGAN_BALANCE.GAZE_PASSIVE_MULTIPLIER,
    ) < 0.0001,
  );
});

test('wall anomalies, specimen jars, grafting, Excision I, and Cycle 1+ prestige beats match Android engine', () => {
  // Phase 1 spawns Pale Tooth
  let p1 = { ...initialState(realCatalog), ichor: 80, cycleLifetimeIchor: 80, phase: 1 };
  p1 = tick(p1, ORGAN_BALANCE.FIRST_ANOMALY_DELAY_SEC + 0.5, realCatalog);
  assert.equal(p1.activeAnomalies.length, 1);
  assert.equal(p1.activeAnomalies[0].type, WALL_ANOMALY_TYPES.PALE_TOOTH.id);

  const pulled = interactAnomaly(p1, p1.activeAnomalies[0].id, realCatalog);
  assert.equal(pulled.activeAnomalies.length, 0);
  assert.equal(manualTapYield(pulled), 1.5);
  assert.equal(pulled.activeDialogue?.id, 'dlg_p1_first_tooth');

  // Glass Eye grants 1.6x Gaze
  const eyeState = {
    ...initialState(realCatalog),
    phase: 2,
    activeAnomalies: [{ id: 50, type: WALL_ANOMALY_TYPES.GLASS_EYE.id, xFraction: 0.2, yFraction: 0.2 }],
  };
  const wiped = interactAnomaly(eyeState, 50, realCatalog);
  assert.equal(gazeMultiplier(wiped), ORGAN_BALANCE.GAZE_PASSIVE_MULTIPLIER);

  // Jars + Grafting in Phase 3 (with Stillness cost & cooldown)
  let p3 = {
    ...initialState(realCatalog),
    cycleLifetimeIchor: 17000,
    ichor: 17000,
    stillness: 70,
    phase: 3,
    creatures: {
      [CREATURE_TYPES.SUTURE_HAND.id]: 2,
      [CREATURE_TYPES.LEECH_CLERK.id]: 2,
    },
  };
  p3 = bottleJar(p3, SPECIMEN_JARS.YESTERDAYS_DUST.id, realCatalog);
  assert.ok(p3.ownedJars.includes(SPECIMEN_JARS.YESTERDAYS_DUST.id));
  assert.equal(p3.stillness, 46);
  assert.equal(graftStillnessCost(p3), 35);
  assert.equal(canAffordGraft(p3), true);
  p3 = performGraft(p3, realCatalog);
  assert.equal(p3.graftedTendrils, 1);
  assert.equal(p3.stillness, 11);
  assert.equal(p3.graftCooldownRemainingSeconds, ORGAN_BALANCE.GRAFT_COOLDOWN_SEC);
  assert.equal(p3.lastSacrificePulse, 1);
  assert.equal(p3.lastSacrificeKind, 'tendril');
  assert.equal(canAffordGraft(p3), false);
  assert.equal(p3.activeDialogue?.id, 'dlg_p3_first_graft');

  // Phase 4 Split Chitin Rib grants Ichor + Stillness + 6s Eye Gaze + dlg_p4_chitin_rib
  const ribState = {
    ...p3,
    ichor: 90000,
    cycleLifetimeIchor: 90000,
    stillness: 10,
    phase: 4,
    isStillnessActive: true,
    activeAnomalies: [{ id: 88, type: WALL_ANOMALY_TYPES.CHITIN_RIB.id, xFraction: 0.8, yFraction: 0.3 }],
  };
  const pried = interactAnomaly(ribState, 88, realCatalog);
  assert.equal(pried.ichor, 90000 + ORGAN_BALANCE.RIB_BASE_ICHOR_REWARD);
  assert.equal(pried.stillness, 10 + ORGAN_BALANCE.RIB_BASE_STILLNESS_REWARD);
  assert.equal(pried.gazeRemainingSeconds, ORGAN_BALANCE.RIB_GAZE_DURATION_SEC);
  assert.equal(pried.isStillnessActive, true);
  assert.equal(pried.activeDialogue?.id, 'dlg_p4_chitin_rib');

  // Mid-Phase 4 counter threshold triggers dlg_p4_mid_hollow
  const midP4 = pressValve({ ...pried, ichor: 144999, cycleLifetimeIchor: 144999 }, realCatalog);
  assert.equal(midP4.activeDialogue?.id, 'dlg_p4_mid_hollow');

  // Excision I -> Cycle 1 grants 2 Scars along smooth power curve (zero milestone bonus)
  const excisionReady = {
    ...midP4,
    ichor: 215000,
    cycleLifetimeIchor: 215000,
    phase: 4,
  };
  assert.equal(canExcise(excisionReady), true);
  assert.equal(pendingScarTissue(excisionReady), 2);

  const cycle1 = performExcision(excisionReady, realCatalog);
  assert.equal(cycle1.cycle, 1);
  assert.equal(cycle1.highestExcisionMilestone, 1);
  assert.equal(cycle1.scarTissue, 2);
  assert.equal(cycle1.availableScarTissue, 2);
  const expectedScarMult = 1.0 + 1 * ORGAN_BALANCE.MILESTONE_ICHOR_RESONANCE + Math.sqrt(2) * ORGAN_BALANCE.SCAR_RESONANCE_SQRT_FACTOR;
  const expectedStillMult = 1.0 + 1 * ORGAN_BALANCE.MILESTONE_STILLNESS_RESONANCE + Math.sqrt(2) * ORGAN_BALANCE.SCAR_STILLNESS_SQRT_FACTOR;
  assert.ok(Math.abs(scarMultiplier(cycle1) - expectedScarMult) < 0.0001);
  assert.ok(Math.abs(scarStillnessMultiplier(cycle1) - expectedStillMult) < 0.0001);
  assert.equal(cycle1.activeDialogue?.id, 'dlg_cycle_1_excision');

  const cycle1P1 = pressValve({ ...cycle1, ichor: 74, cycleLifetimeIchor: 74 }, realCatalog);
  assert.equal(cycle1P1.activeDialogue?.id, 'dlg_cycle_1_scar_handwriting');

  const cycle1P2 = pressValve({ ...cycle1P1, ichor: 1599, cycleLifetimeIchor: 1599 }, realCatalog);
  assert.equal(cycle1P2.activeDialogue?.id, 'dlg_cycle_1_returned_letter');
});

test('Scar Tissue Surgical Skill Tree enforces prerequisites, deducts spendable Scar, and persists upgrades across resets', () => {
  let state = {
    ...initialState(realCatalog),
    highestExcisionMilestone: 1,
    scarTissue: 12,
    availableScarTissue: 12,
  };

  // Cannot unlock Tier 1 before root first_incision
  assert.equal(canUnlockNode(state, 'calloused_palm'), false);
  assert.equal(canUnlockNode(state, 'first_incision'), true);

  state = unlockScarNode(state, 'first_incision', realCatalog);
  assert.equal(state.availableScarTissue, 11);
  assert.equal(state.activeDialogue?.id, 'dlg_scar_first_incision');

  assert.equal(SCAR_NODES.find((n) => n.id === 'shorter_silence')?.cost, 1);
  assert.equal(SCAR_NODES.find((n) => n.id === 'wax_sealed_memory')?.cost, 2);
  assert.equal(SCAR_NODES.find((n) => n.id === 'deep_weeping')?.cost, 3);
  assert.equal(SCAR_NODES.find((n) => n.id === 'hollow_lung_choir')?.cost, 9);
  assert.equal(SCAR_NODES.find((n) => n.id === 'catatonic_tide')?.cost, 36);
  assert.equal(SCAR_NODES.find((n) => n.id === 'colony_inertia')?.cost, 8);
  assert.equal(SCAR_NODES.find((n) => n.id === 'membrane_recall')?.cost, 12);
  assert.equal(SCAR_NODES.find((n) => n.id === 'perpetual_seam')?.cost, 36);
  assert.equal(SCAR_NODES.find((n) => n.id === 'auto_tendril_suture')?.cost, 5);
  assert.equal(SCAR_NODES.find((n) => n.id === 'echo_ossuary')?.cost, 9);
  assert.equal(SCAR_NODES.find((n) => n.id === 'bloom_incision')?.cost, 16);
  assert.equal(SCAR_NODES.find((n) => n.id === 'bone_resonance')?.cost, 40);

  state = unlockScarNode(state, 'calloused_palm', realCatalog);
  const beforeShorter = scarStillnessMultiplier(state);
  state = unlockScarNode(state, 'shorter_silence', realCatalog);
  assert.ok(Math.abs(scarStillnessMultiplier(state) - beforeShorter * 1.5) < 0.0001);
  state = unlockScarNode(state, 'wax_sealed_memory', realCatalog);
  // Spent: 1 (first_incision) + 2 (calloused_palm) + 1 (shorter_silence) + 2 (wax_sealed_memory) = 6 -> 6 left
  assert.equal(state.availableScarTissue, 6);

  // Reworked Wax-Sealed Memory: clears jars on Excision, discounts Stillness costs by 30%, halves graft cooldown
  state = {
    ...state,
    ichor: 220000,
    cycleLifetimeIchor: 220000,
    phase: 4,
    ownedJars: [SPECIMEN_JARS.YESTERDAYS_DUST.id],
  };
  const reset = performExcision(state, realCatalog);
  assert.equal(creatureCount(reset, CREATURE_TYPES.SUTURE_HAND.id), 3);
  assert.equal(creatureCount(reset, CREATURE_TYPES.LEECH_CLERK.id), 1);
  assert.equal(reset.ownedJars.length, 0);
  assert.ok(Math.abs(stillnessCostMultiplier(reset) - 0.70) < 0.0001);
  assert.ok(Math.abs(jarEffectBoost(reset) - 1.35) < 0.0001);
  assert.equal(effectiveJarCost(reset, SPECIMEN_JARS.YESTERDAYS_DUST.id), 16);
  assert.equal(graftStillnessCost(reset), 24);
  assert.equal(graftCooldownDurationSec(reset), 3.0);

  // Shorter Silence activates Stillness after 1.5s instead of 3.0s (requires >= 1 Weeping Lamp)
  const withLamp = {
    ...reset,
    creatures: { ...reset.creatures, [CREATURE_TYPES.WEEPING_LAMP.id]: 1 },
  };
  const idleShort = tick(withLamp, 1.6, realCatalog);
  assert.equal(idleShort.isStillnessActive, true);
});

test('Excisions II-VI unlock Sump-Siphon/Drain/Relics, Echo Routing, Valve Clamp/Bloom/Choir-Lung, Ossified Throat/Dossier, and Breach to Arc 2', () => {
  // M2: Drain Seal + Relic Dredging + Sump-Siphon
  let s = devSetHighestMilestone(initialState(realCatalog), 2, realCatalog);
  s = devAssignArc1Resources(s, { ichor: 500000, stillness: 200 }, realCatalog);
  s = buyCreature(s, CREATURE_TYPES.SUMP_SIPHON.id, realCatalog);
  assert.equal(creatureCount(s, CREATURE_TYPES.SUMP_SIPHON.id), 1);

  s = toggleDrainSeal(s, realCatalog);
  assert.equal(s.drainSealed, true);
  s = tick(s, 20.0, realCatalog);
  assert.equal(canDredgeSump(s), true);
  s = dredgeSump(s, realCatalog);
  assert.ok(s.dredgedRelics.includes('patient_wristband'));
  assert.equal(s.drainPressure, 0);

  // M3: Echo Routing triggers 12s burst
  s = devSetHighestMilestone(s, 3, realCatalog);
  s = setEchoDiversion(s, 50, realCatalog);
  assert.equal(s.echoDiversionPercent, 50);
  const ichorBeforeEcho = s.ichor;
  s = tick(s, 13.0, realCatalog);
  assert.ok(s.echoPulsesTriggered >= 1);
  assert.ok(s.ichor > ichorBeforeEcho);

  // M4: Valve Clamp 20s -> Marrow Bloom + Choir-Lung Grafting
  s = devSetHighestMilestone(s, 4, realCatalog);
  s = activateValveClamp(s, realCatalog);
  assert.ok(s.clampRemainingSeconds > 0);
  // Valve taps are blocked while clamped
  const ichorBefore = s.ichor;
  const blockedTap = pressValve(s, realCatalog);
  assert.equal(blockedTap.ichor, ichorBefore);
  s = tick(s, 21.0, realCatalog);
  assert.equal(s.marrowBloomCount, 1);
  assert.ok(s.marrowBloomRemainingSeconds > 0);
  assert.ok(s.activeAnomalies.length >= 2);

  s = {
    ...s,
    stillness: 250,
    creatures: { ...s.creatures, [CREATURE_TYPES.WEEPING_LAMP.id]: 3 },
    graftedTendrils: 2,
  };
  assert.equal(canAffordChoirLung(s), true);
  s = performChoirLungGraft(s, realCatalog);
  assert.equal(s.choirLungs, 1);
  assert.equal(creatureCount(s, CREATURE_TYPES.WEEPING_LAMP.id), 1);
  assert.equal(s.graftedTendrils, 1);
  assert.equal(s.stillness, 70);
  assert.equal(s.choirLungCooldownRemainingSeconds, ORGAN_BALANCE.CHOIR_LUNG_COOLDOWN_SEC);
  assert.equal(s.lastSacrificeKind, 'choir_lung');

  // M5: Ossified Throat & Redacted Cult Dossier
  s = devSetHighestMilestone(s, 5, realCatalog);
  s = {
    ...s,
    creatures: { ...s.creatures, [CREATURE_TYPES.SUMP_SIPHON.id]: 3 },
  };
  assert.equal(canOfferToThroat(s, 'dossier_line_1'), true);
  s = offerToThroat(s, 'dossier_line_1', realCatalog);
  assert.ok(s.unredactedDossierLines.includes('dossier_line_1'));

  // M6: Breach Ritual transitions cleanly to Arc 2
  s = devSetHighestMilestone(s, 6, realCatalog);
  assert.equal(canBreachToArc2(s), true);
  const arc2State = performBreachToArc2(s, realCatalog);
  assert.equal(arc2State.activeArc, GAME_ARCS.ARC_2_INVESTIGATION);
  assert.equal(arc2State.activeDialogue?.id, 'dlg_arc2_entry');
});

test('Arc 2 Investigator Desk, Contacts, Deductions, Desk Anomalies, File #00 Twist, and Arc 3 Convergence work end-to-end', () => {
  let state = devSetActiveArc(initialState(realCatalog), GAME_ARCS.ARC_2_INVESTIGATION, realCatalog);
  assert.equal(state.activeArc, GAME_ARCS.ARC_2_INVESTIGATION);

  state = inspectCaseFile(state, realCatalog);
  assert.ok(state.arc2.leads >= 1);

  // Silence activates after 2.5s idle and climbs Complicity
  state = tick(state, 3.0, realCatalog);
  assert.equal(state.arc2.isSilenceActive, true);

  // Assign resources via Dev helper, recruit Contact, pin Deduction, interact with Desk Anomaly
  state = devAssignArc2Resources(state, { leads: 250000, silence: 500 }, realCatalog);
  state = recruitContact(state, 'night_archivist', realCatalog);
  assert.equal(state.arc2.contacts.night_archivist, 1);

  state = pinDeduction(state, 'hydro_ward_invoices', realCatalog);
  assert.ok(state.arc2.ownedDeductions.includes('hydro_ward_invoices'));

  state = devSpawnDeskAnomaly(state);
  assert.equal(state.arc2.activeDeskAnomalies.length, 1);
  state = interactDeskAnomaly(state, state.arc2.activeDeskAnomalies[0].id, realCatalog);
  assert.equal(state.arc2.activeDeskAnomalies.length, 0);

  // Connect all 4 chapters to trigger File #00 twist
  for (let ch = 1; ch <= 4; ch += 1) {
    assert.equal(arc2CanConnectNextChapter(state.arc2), true);
    state = connectNextChapter(state, realCatalog);
    assert.equal(state.arc2.completedChapters, ch);
  }
  assert.equal(state.arc2.fileZeroRevealed, true);
  assert.equal(state.arc2.complicity, 1.0);
  assert.equal(state.activeDialogue?.id, 'dlg_arc2_ch4_file_zero');

  // Cross into Arc 3 open convergence threshold
  const arc3 = enterArc3Convergence(state, realCatalog);
  assert.equal(arc3.activeArc, GAME_ARCS.ARC_3_CONVERGENCE);
  assert.equal(arc3.activeDialogue?.id, 'dlg_arc3_convergence');

  // Dev utilities: unlock all scars, spawn wall anomaly, reset all
  let devState = devSetAllScarNodes(devSetActiveArc(arc3, GAME_ARCS.ARC_1_CHAMBER, realCatalog), true, realCatalog);
  assert.equal(devState.unlockedScarNodes.length, SCAR_NODES.length);
  devState = devSpawnWallAnomaly(devState);
  assert.ok(devState.activeAnomalies.length >= 1);
  const wiped = devResetAll(realCatalog);
  assert.equal(wiped.activeArc, GAME_ARCS.ARC_1_CHAMBER);
  assert.equal(wiped.highestExcisionMilestone, 0);
  assert.equal(EXCISION_MILESTONES.length, 6);
  assert.equal(DREDGED_RELICS.length, 3);
  assert.equal(CULT_DOSSIER_LINES.length, 4);
  assert.equal(INVESTIGATOR_CONTACTS.length, 4);
  assert.equal(CASE_DEDUCTIONS.length, 3);
  assert.equal(Object.keys(DESK_ANOMALY_TYPES).length, 4);
  assert.equal(ARC2_BALANCE.SILENCE_IDLE_DELAY_SEC, 2.5);
});

test('Cycle 0 moderate-activity pacing reaches First Excision in 12..18 minutes (exact Android parity)', () => {
  let state = initialState(realCatalog);
  let elapsedSec = 0;

  while (!canExcise(state) && elapsedSec < 1800) {
    const hasLamps = creatureCount(state, CREATURE_TYPES.WEEPING_LAMP.id) > 0;
    const inStillnessWindow = hasLamps && elapsedSec % 45 < 25;

    if (!inStillnessWindow) {
      const taps = !hasLamps ? (elapsedSec % 5 === 0 ? 2 : 1) : elapsedSec % 2 === 0 ? 1 : 0;
      for (let t = 0; t < taps; t += 1) {
        state = pressValve(state, realCatalog);
      }
      if (state.activeAnomalies.length > 0) {
        state = interactAnomaly(state, state.activeAnomalies[0].id, realCatalog);
      }
    }

    for (const jar of Object.values(SPECIMEN_JARS)) {
      if (!state.ownedJars.includes(jar.id) && state.stillness >= jar.stillnessCost) {
        state = bottleJar(state, jar.id, realCatalog);
      }
    }

    if (canAffordGraft(state)) {
      state = performGraft(state, realCatalog);
    }

    const lampCost = creatureCost(state, CREATURE_TYPES.WEEPING_LAMP.id);
    const leechCost = creatureCost(state, CREATURE_TYPES.LEECH_CLERK.id);
    const sutureCost = creatureCost(state, CREATURE_TYPES.SUTURE_HAND.id);
    const lampUnlocked =
      cycleLifetimeIchorDisplay(state) >= CREATURE_TYPES.WEEPING_LAMP.unlockIchorThreshold;
    const leechUnlocked =
      cycleLifetimeIchorDisplay(state) >= CREATURE_TYPES.LEECH_CLERK.unlockIchorThreshold;

    if (lampUnlocked && state.ichor >= lampCost) {
      state = buyCreature(state, CREATURE_TYPES.WEEPING_LAMP.id, realCatalog);
    } else if (leechUnlocked && state.ichor >= leechCost && (!lampUnlocked || leechCost < lampCost * 0.55)) {
      state = buyCreature(state, CREATURE_TYPES.LEECH_CLERK.id, realCatalog);
    } else if (
      isApparatusUnlocked(state) &&
      state.ichor >= sutureCost &&
      (!leechUnlocked || sutureCost < leechCost * 0.45)
    ) {
      state = buyCreature(state, CREATURE_TYPES.SUTURE_HAND.id, realCatalog);
    }

    state = tick(state, 1.0, realCatalog);
    elapsedSec += 1;
  }

  assert.equal(canExcise(state), true, 'Expected Cycle 0 to reach Excision within 30 min');
  assert.ok(
    elapsedSec >= 720 && elapsedSec <= 1080,
    `Expected 12..18 min (720..1080s), actual was ${elapsedSec}s (${(elapsedSec / 60).toFixed(2)} min)`,
  );
});

test('save serialization v2 round-trips multi-arc state, migrates v1 saves safely, and rejects corrupt JSON', () => {
  let state = initialState(realCatalog);
  for (let i = 0; i < 80; i += 1) state = pressValve(state, realCatalog);
  state = buyCreature(state, CREATURE_TYPES.SUTURE_HAND.id, realCatalog);
  state = devSetHighestMilestone(state, 2, realCatalog);
  state = devAssignArc1Resources(state, { availableScar: 5 }, realCatalog);
  state = unlockScarNode(state, 'first_incision', realCatalog);
  state = tick(state, 5.0, realCatalog);

  const serialized = serializeState(state);
  const restored = deserializeState(serialized, realCatalog);

  assert.ok(restored !== null);
  assert.equal(ichorDisplay(restored), ichorDisplay(state));
  assert.equal(stillnessDisplay(restored), stillnessDisplay(state));
  assert.equal(creatureCount(restored, CREATURE_TYPES.SUTURE_HAND.id), 1);
  assert.equal(restored.highestExcisionMilestone, 2);
  assert.ok(restored.unlockedScarNodes.includes('first_incision'));
  assert.equal(restored.phase, state.phase);
  assert.equal(phaseForCount(cycleLifetimeIchorDisplay(restored)), restored.phase);
  assert.equal(progressWithinPhase(147500), 0.5);
  assert.equal(restored.discoveredDialogues.length, state.discoveredDialogues.length);
  assert.equal(restored.activeDialogue?.id, state.activeDialogue?.id);

  // Verify legacy v1 save JSON migrates without crashing
  const legacyV1 = JSON.stringify({
    ichor: 1200,
    cycleLifetimeIchor: 1200,
    stillness: 15,
    scarTissue: 2,
    cycle: 1,
    creatures: { suture_hand: 3 },
    triggeredDialogueIds: ['dlg_p0_first_press'],
  });
  const migrated = deserializeState(legacyV1, realCatalog);
  assert.ok(migrated !== null);
  assert.equal(migrated.activeArc, GAME_ARCS.ARC_1_CHAMBER);
  assert.equal(migrated.highestExcisionMilestone, 1);
  assert.equal(migrated.scarTissue, 2);
  assert.equal(migrated.availableScarTissue, 2);

  assert.equal(deserializeState('{invalid json', realCatalog), null);
  assert.equal(deserializeState(null, realCatalog), null);
});

test('6-tier Specimen Jars, Next Scar quadratic progress, and cooldown serialization match Android 1:1', () => {
  assert.equal(Object.values(SPECIMEN_JARS).length, 6);

  // Phase 2 locks Salt (P3), Borrowed Pulse (P4), and Uncounted Hour (post-Severance)
  const p2 = { ...initialState(realCatalog), phase: 2, stillness: 5000 };
  assert.equal(isJarUnlocked(p2, SPECIMEN_JARS.YESTERDAYS_DUST.id), true);
  assert.equal(isJarUnlocked(p2, SPECIMEN_JARS.SALT_FROM_THE_GROUT.id), false);
  assert.equal(isJarUnlocked(p2, SPECIMEN_JARS.A_BORROWED_PULSE.id), false);
  assert.equal(isJarUnlocked(p2, SPECIMEN_JARS.THE_UNCOUNTED_HOUR.id), false);

  // Phase 3 Salt from the Grout boosts organisms by +45%
  let p3 = {
    ...p2,
    phase: 3,
    cycleLifetimeIchor: 17000,
    creatures: { [CREATURE_TYPES.SUTURE_HAND.id]: 2 },
  };
  const preSaltRate = ichorPerSecond(p3);
  p3 = bottleJar(p3, SPECIMEN_JARS.SALT_FROM_THE_GROUT.id, realCatalog);
  assert.ok(Math.abs(ichorPerSecond(p3) - preSaltRate * (1 + ORGAN_BALANCE.SALT_GROUT_BONUS)) < 0.001);

  // Phase 4 A Borrowed Pulse boosts graft/lung Ichor & all Stillness/s by +55%
  let p4 = { ...p3, phase: 4, cycleLifetimeIchor: 90000, graftedTendrils: 1 };
  const prePulseStill = stillnessPerSecond(p4);
  p4 = bottleJar(p4, SPECIMEN_JARS.A_BORROWED_PULSE.id, realCatalog);
  assert.ok(Math.abs(stillnessPerSecond(p4) - prePulseStill * (1 + ORGAN_BALANCE.BORROWED_PULSE_BONUS)) < 0.001);

  // Cycle 1+ unlocks The Uncounted Hour (+75% global Ichor & all Stillness/s)
  let c1 = { ...p4, cycle: 1, highestExcisionMilestone: 1 };
  const preHourIchor = ichorPerSecond(c1);
  const preHourStill = stillnessPerSecond(c1);
  c1 = bottleJar(c1, SPECIMEN_JARS.THE_UNCOUNTED_HOUR.id, realCatalog);
  assert.ok(Math.abs(ichorPerSecond(c1) - preHourIchor * (1 + ORGAN_BALANCE.UNCOUNTED_HOUR_BONUS)) < 0.001);
  assert.ok(Math.abs(stillnessPerSecond(c1) - preHourStill * (1 + ORGAN_BALANCE.UNCOUNTED_HOUR_BONUS)) < 0.001);

  // Next Scar smooth power-curve threshold & exact inverse
  const s0 = initialState(realCatalog);
  assert.equal(currentBaseScarHarvest(s0), 0);
  assert.equal(nextBaseScarMassThreshold(s0), 210000);
  assert.ok(Math.abs(nextScarProgress({ ...s0, cycleLifetimeIchor: 105000 }) - 0.5) < 0.001);
  assert.equal(currentBaseScarHarvest({ ...s0, cycleLifetimeIchor: 210000 }), 2);
  assert.equal(currentBaseScarHarvest({ ...s0, cycleLifetimeIchor: 1800000 }), 3);
  assert.equal(currentBaseScarHarvest({ ...s0, cycleLifetimeIchor: 16000000 }), 7);
  assert.equal(currentBaseScarHarvest({ ...s0, cycleLifetimeIchor: 140000000 }), 14);
  assert.equal(currentBaseScarHarvest({ ...s0, cycleLifetimeIchor: 1250000000 }), 27);
  assert.equal(currentBaseScarHarvest({ ...s0, cycleLifetimeIchor: 12000000000 }), 53);

  for (let target = 3; target <= 40; target += 1) {
    const req = massRequiredForScarCount(s0, target);
    assert.ok(currentBaseScarHarvest({ ...s0, cycleLifetimeIchor: req }) >= target);
    assert.ok(currentBaseScarHarvest({ ...s0, cycleLifetimeIchor: req - 1 }) < target);
  }

  // Next Scar threshold never exceeds active Excision milestone and awards pulled scar early at M2..M6
  const activeMilestoneExpectations = [
    [1, 1800000, 4],
    [2, 24000000, 9],
    [3, 320000000, 19],
    [4, 4500000000, 40],
    [5, 160000000000, 117],
  ];
  for (const [cleared, milestoneMass, expectedScars] of activeMilestoneExpectations) {
    const justBefore = {
      ...s0,
      highestExcisionMilestone: cleared,
      cycleLifetimeIchor: milestoneMass - 1,
    };
    assert.equal(currentBaseScarHarvest(justBefore), expectedScars - 1);
    assert.equal(nextBaseScarMassThreshold(justBefore), milestoneMass);
    assert.ok(nextScarProgress(justBefore) > 0.99);

    const exactMilestone = {
      ...s0,
      highestExcisionMilestone: cleared,
      cycleLifetimeIchor: milestoneMass,
    };
    assert.equal(currentBaseScarHarvest(exactMilestone), expectedScars);
    assert.equal(pendingScarTissue(exactMilestone), expectedScars);
    assert.ok(Math.abs(nextScarProgress(exactMilestone)) < 0.001);
    assert.ok(nextBaseScarMassThreshold(exactMilestone) > milestoneMass);
  }

  // M2 progression: 1.2M clamps to 1.8M, 1.8M awards 4 Scars on Excision, 2.12M does not double-grant
  const m2Base = { ...s0, highestExcisionMilestone: 1 };
  assert.equal(nextBaseScarMassThreshold({ ...m2Base, cycleLifetimeIchor: 1200000 }), 1800000);
  assert.equal(currentBaseScarHarvest({ ...m2Base, cycleLifetimeIchor: 2120000 }), 4);
  const fifthScarReq = massRequiredForScarCount(m2Base, 5);
  assert.equal(nextBaseScarMassThreshold({ ...m2Base, cycleLifetimeIchor: 2120000 }), fifthScarReq);
  assert.equal(currentBaseScarHarvest({ ...m2Base, cycleLifetimeIchor: fifthScarReq }), 5);

  const excisedAtM2 = performExcision(
    { ...m2Base, ichor: 1800000, cycleLifetimeIchor: 1800000, phase: 4 },
    realCatalog,
  );
  assert.equal(excisedAtM2.highestExcisionMilestone, 2);
  assert.equal(excisedAtM2.scarTissue, 4);

  const hookedM2 = {
    ...m2Base,
    unlockedScarNodes: ['first_incision'],
    repeatableScarRanks: { scar_harvest_hook: 1 },
  };
  for (const candidateState of [m2Base, hookedM2]) {
    for (let target = 3; target <= 30; target += 1) {
      const req = massRequiredForScarCount(candidateState, target);
      assert.ok(currentBaseScarHarvest({ ...candidateState, cycleLifetimeIchor: req }) >= target);
      assert.ok(currentBaseScarHarvest({ ...candidateState, cycleLifetimeIchor: req - 1 }) < target);
    }
  }

  // Cooldown, late-tier jar, and repeatableScarRanks serialization round-trip
  const withCooldown = {
    ...c1,
    graftCooldownRemainingSeconds: 4.5,
    choirLungCooldownRemainingSeconds: 8.25,
    repeatableScarRanks: { keloid_thickening: 2, scar_harvest_hook: 1 },
  };
  const restored = deserializeState(serializeState(withCooldown), realCatalog);
  assert.equal(restored.graftCooldownRemainingSeconds, 4.5);
  assert.equal(restored.choirLungCooldownRemainingSeconds, 8.25);
  assert.ok(restored.ownedJars.includes(SPECIMEN_JARS.THE_UNCOUNTED_HOUR.id));
  assert.equal(repeatableRank(restored, 'keloid_thickening'), 2);
  assert.equal(repeatableRank(restored, 'scar_harvest_hook'), 1);
});

test('Anti-skip sequential cap, zero milestone bonus Scars, M5 Arc 2 gate, 16-node Scar Tree, and 3 Repeatable Grafts match Android 1:1', () => {
  assert.equal(SCAR_NODES.length, 16);
  assert.equal(REPEATABLE_SCAR_GRAFTS.length, 3);
  assert.ok(EXCISION_MILESTONES.every((m) => m.firstClearScarBonus === 0));

  // Hard anti-skip cap: 999B Ichor at M0 advances strictly to M1 and blocks Arc 2 Breach
  const massiveM0 = {
    ...initialState(realCatalog),
    ichor: 999000000000,
    cycleLifetimeIchor: 999000000000,
    phase: 4,
  };
  assert.equal(canClearNextMilestone(massiveM0), true);
  assert.equal(hasExcessMilestoneMass(massiveM0), true);
  assert.equal(canBreachToArc2(massiveM0), false);
  const afterM0 = performExcision(massiveM0, realCatalog);
  assert.equal(afterM0.highestExcisionMilestone, 1);

  // M4 with 200B still cannot Breach Arc 2 and advances strictly to M5
  const massiveM4 = {
    ...afterM0,
    highestExcisionMilestone: 4,
    ichor: 200000000000,
    cycleLifetimeIchor: 200000000000,
    phase: 4,
  };
  assert.equal(canBreachToArc2(massiveM4), false);
  const afterM4 = performExcision(massiveM4, realCatalog);
  assert.equal(afterM4.highestExcisionMilestone, 5);

  // M5 with 36B is below the 160B threshold; 160B opens Breach to Arc 2 and grants 0 bonus Scars
  const shortM5 = { ...afterM4, ichor: 36000000000, cycleLifetimeIchor: 36000000000, phase: 4 };
  assert.equal(canBreachToArc2(shortM5), false);

  const readyM5 = { ...afterM4, ichor: 160000000000, cycleLifetimeIchor: 160000000000, phase: 4 };
  assert.equal(canBreachToArc2(readyM5), true);
  const scarBeforeBreach = readyM5.scarTissue;
  const breached = performBreachToArc2(readyM5, realCatalog);
  assert.equal(breached.activeArc, GAME_ARCS.ARC_2_INVESTIGATION);
  assert.equal(breached.highestExcisionMilestone, 6);
  assert.equal(breached.scarTissue, scarBeforeBreach);

  // Tier 4-5 nodes: MEMBRANE_RECALL, PERPETUAL_SEAM, HOLLOW_LUNG_CHOIR (relic retention), CATATONIC_TIDE (foundational jar retention), ECHO_OSSUARY, BLOOM_INCISION, BONE_RESONANCE
  const recallBase = {
    ...initialState(realCatalog),
    highestExcisionMilestone: 2,
    cycle: 2,
    ichor: 300000,
    cycleLifetimeIchor: 300000,
    phase: 4,
    creatures: { [CREATURE_TYPES.SUMP_SIPHON.id]: 1 },
    graftedTendrils: 1,
    unlockedScarNodes: ['membrane_recall'],
  };
  const recallPlain = { ...recallBase, unlockedScarNodes: [] };
  assert.ok(Math.abs(rawIchorPerSecond(recallBase) - rawIchorPerSecond(recallPlain) * 2.2) < 0.01);
  const resetRecall = performExcision(recallBase, realCatalog);
  assert.equal(creatureCount(resetRecall, CREATURE_TYPES.WEEPING_LAMP.id), 2);
  assert.equal(creatureCount(resetRecall, CREATURE_TYPES.SUMP_SIPHON.id), 2);
  assert.equal(resetRecall.graftedTendrils, 1);

  const perpetualPlain = {
    ...initialState(realCatalog),
    creatures: { [CREATURE_TYPES.WEEPING_LAMP.id]: 1 },
    graftedTendrils: 1,
    choirLungs: 1,
    secondsSinceLastTap: 5.0,
    isStillnessActive: true,
  };
  const perpetual = { ...perpetualPlain, unlockedScarNodes: ['perpetual_seam'] };
  assert.equal(ORGAN_BALANCE.PERPETUAL_SEAM_INTERVAL_SEC, 0.75);
  assert.ok(Math.abs(rawIchorPerSecond(perpetual) - rawIchorPerSecond(perpetualPlain) * 2.5) < 0.01);
  const afterPerp = tick(perpetual, ORGAN_BALANCE.PERPETUAL_SEAM_INTERVAL_SEC + 0.05, realCatalog);
  assert.equal(afterPerp.isStillnessActive, true);
  assert.ok(afterPerp.ichor >= manualTapYield(perpetual));

  // Hollow Lung Choir retains all dredgedRelics across Excisions
  const hollowWithRelics = {
    ...initialState(realCatalog),
    highestExcisionMilestone: 2,
    ichor: 300000,
    cycleLifetimeIchor: 300000,
    phase: 4,
    dredgedRelics: ['patient_wristband', 'brass_ward_key'],
    unlockedScarNodes: ['hollow_lung_choir'],
  };
  const resetHollow = performExcision(hollowWithRelics, realCatalog);
  assert.deepEqual(resetHollow.dredgedRelics, ['patient_wristband', 'brass_ward_key']);

  // Catatonic Tide: 0.8s onset, 3.20x idle / 1.75x active floor, +0.65 Jar boost, retains foundational 3 Jars only
  const tideInactive = {
    ...initialState(realCatalog),
    highestExcisionMilestone: 2,
    ichor: 300000,
    cycleLifetimeIchor: 300000,
    phase: 4,
    unlockedScarNodes: ['catatonic_tide'],
    creatures: { [CREATURE_TYPES.SUTURE_HAND.id]: 2 },
    ownedJars: [
      SPECIMEN_JARS.YESTERDAYS_DUST.id,
      SPECIMEN_JARS.UNSPENT_BREATH.id,
      SPECIMEN_JARS.ALMOST_A_NAME.id,
      SPECIMEN_JARS.SALT_FROM_THE_GROUT.id,
      SPECIMEN_JARS.THE_UNCOUNTED_HOUR.id,
    ],
    isStillnessActive: false,
  };
  assert.equal(stillnessIdleDelaySec(tideInactive), 0.8);
  assert.equal(catatonicTideMultiplier(tideInactive), 1.75);
  assert.equal(catatonicTideMultiplier({ ...tideInactive, isStillnessActive: true }), 3.2);
  assert.equal(jarEffectBoost(tideInactive), 1.65);
  const resetTide = performExcision(tideInactive, realCatalog);
  assert.deepEqual(resetTide.ownedJars, [
    SPECIMEN_JARS.YESTERDAYS_DUST.id,
    SPECIMEN_JARS.UNSPENT_BREATH.id,
    SPECIMEN_JARS.ALMOST_A_NAME.id,
  ]);

  const ossuary = { ...initialState(realCatalog), highestExcisionMilestone: 3, unlockedScarNodes: ['echo_ossuary'] };
  assert.equal(echoPulseIntervalSec(ossuary), 9.0);
  assert.equal(relicAmplifier(ossuary), 1.35);
  assert.ok(Math.abs(relicAmplifier({ ...ossuary, unlockedScarNodes: ['echo_ossuary', 'bone_resonance'] }) - 1.35 * 1.4) < 0.0001);

  const bloomNode = { ...initialState(realCatalog), highestExcisionMilestone: 4, unlockedScarNodes: ['bloom_incision'] };
  assert.equal(valveClampDurationSec(bloomNode), 15.0);
  assert.equal(marrowBloomDurationSec(bloomNode), 32.0);
  assert.equal(ORGAN_BALANCE.BLOOM_INCISION_MULTIPLIER, 3.1);

  // Repeatable Surgical Grafts gated behind FIRST_INCISION, persist across Excisions, cleared by dev reset
  let rep = { ...initialState(realCatalog), highestExcisionMilestone: 1, scarTissue: 20, availableScarTissue: 20 };
  assert.equal(isRepeatableGraftsUnlocked(rep), false);
  assert.equal(canBuyRepeatableGraft(rep, 'keloid_thickening'), false);

  rep = unlockScarNode(rep, 'first_incision', realCatalog);
  assert.equal(isRepeatableGraftsUnlocked(rep), true);
  assert.equal(repeatableCost(rep, 'keloid_thickening'), 1);
  assert.equal(repeatableCost(rep, 'sediment_well'), 1);
  assert.equal(repeatableCost(rep, 'scar_harvest_hook'), 1);
  assert.deepEqual(
    [0, 1, 2, 3, 4, 5].map((rank) =>
      repeatableCost({ ...rep, repeatableScarRanks: { keloid_thickening: rank } }, 'keloid_thickening'),
    ),
    [1, 1, 2, 3, 4, 6],
  );
  assert.deepEqual(
    [0, 1, 2, 3, 4, 5].map((rank) =>
      repeatableCost({ ...rep, repeatableScarRanks: { scar_harvest_hook: rank } }, 'scar_harvest_hook'),
    ),
    [1, 1, 2, 3, 5, 8],
  );
  rep = buyRepeatableScarGraft(rep, 'keloid_thickening', realCatalog);
  assert.equal(repeatableRank(rep, 'keloid_thickening'), 1);
  assert.equal(repeatableCost(rep, 'keloid_thickening'), 1);

  rep = buyRepeatableScarGraft(rep, 'sediment_well', realCatalog);
  assert.equal(repeatableRank(rep, 'sediment_well'), 1);
  assert.ok(Math.abs(sedimentCostMultiplier(rep) - 0.96) < 0.0001);
  assert.ok(Math.abs(sedimentCostMultiplier({ ...rep, repeatableScarRanks: { sediment_well: 25 } }) - 0.68) < 0.0001);

  rep = buyRepeatableScarGraft(rep, 'scar_harvest_hook', realCatalog);
  assert.ok(Math.abs(scarHarvestMultiplier(rep) - 1.12) < 0.0001);

  const repAfterExcise = performExcision({ ...rep, ichor: 250000, cycleLifetimeIchor: 250000, phase: 4 }, realCatalog);
  assert.equal(repeatableRank(repAfterExcise, 'keloid_thickening'), 1);
  assert.equal(repeatableRank(repAfterExcise, 'scar_harvest_hook'), 1);

  const repCleared = devSetAllScarNodes(repAfterExcise, false, realCatalog);
  assert.equal(Object.keys(repCleared.repeatableScarRanks).length, 0);
});

test('Arc 1 escape-to-plea narrative threads, scar_node_specific, repeatable_graft_total, and Plead for Salvation match Android 1:1', () => {
  assert.ok(realCatalog.entries.length >= 70);
  assert.equal(EXCISION_MILESTONES[0].code, 'FIRST SEVERANCE');
  assert.equal(EXCISION_MILESTONES[5].code, 'PLEAD FOR SALVATION');
  assert.ok(EXCISION_MILESTONES.every((m) => !m.code.includes('EXCISION')));

  // Specific Tier 5 Scar branch capstone trigger (prior M2-M4 and T1/T4 entries already seen in play)
  let s = {
    ...initialState(realCatalog),
    highestExcisionMilestone: 4,
    scarTissue: 140,
    availableScarTissue: 140,
    unlockedScarNodes: [
      'first_incision',
      'calloused_palm',
      'twin_stitch',
      'colony_inertia',
      'membrane_recall',
    ],
    triggeredDialogueIds: [
      'dlg_excision_m2_drain',
      'dlg_excision_m3_seam',
      'dlg_excision_m4_clamp',
      'dlg_scar_tree_first',
      'dlg_scar_branch_suture_t4',
    ],
  };
  s = unlockScarNode(s, 'perpetual_seam', realCatalog);
  assert.equal(s.activeDialogue?.id, 'dlg_scar_branch_suture_t5');

  // Repeatable Surgical Graft narrative thresholds (1 and 4 total ranks)
  s = buyRepeatableScarGraft(s, 'keloid_thickening', realCatalog);
  assert.equal(s.activeDialogue?.id, 'dlg_repeatable_graft_first');
  s = buyRepeatableScarGraft(s, 'sediment_well', realCatalog);
  s = buyRepeatableScarGraft(s, 'scar_harvest_hook', realCatalog);
  assert.ok(!s.triggeredDialogueIds.includes('dlg_repeatable_graft_deep'));
  s = buyRepeatableScarGraft(s, 'keloid_thickening', realCatalog);
  assert.equal(s.activeDialogue?.id, 'dlg_repeatable_graft_deep');

  // Multi-step Dredged Relics (2nd Brass Key -> 3rd Bleeding Logbook)
  s = {
    ...s,
    drainPressure: 100,
    dredgedRelics: ['patient_wristband'],
    lifetimeRelicsDredged: 1,
  };
  s = dredgeSump(s, realCatalog);
  assert.equal(s.activeDialogue?.id, 'dlg_second_relic_dredged');
  s = { ...s, drainPressure: 100 };
  s = dredgeSump(s, realCatalog);
  assert.equal(s.activeDialogue?.id, 'dlg_third_relic_dredged');

  // Multi-step Neighbor Aperture Echoes (4th SOS -> 12th thumb cadence -> 25th sealed twin)
  s = { ...s, echoDiversionPercent: 50, echoPulsesTriggered: 3, echoChargeSeconds: 11.5 };
  s = tick(s, 1.0, realCatalog);
  assert.equal(s.activeDialogue?.id, 'dlg_echo_sos_tapped');
  s = { ...s, echoPulsesTriggered: 11, echoChargeSeconds: 11.5 };
  s = tick(s, 1.0, realCatalog);
  assert.equal(s.activeDialogue?.id, 'dlg_echo_mirror_thumb');
  s = { ...s, echoPulsesTriggered: 24, echoChargeSeconds: 11.5 };
  s = tick(s, 1.0, realCatalog);
  assert.equal(s.activeDialogue?.id, 'dlg_echo_sealed_twin');

  // Multi-step Marrow Blooms (3rd cold city rain draft -> 6th healed wall)
  s = { ...s, clampRemainingSeconds: 0.5, marrowBloomCount: 2 };
  s = tick(s, 1.0, realCatalog);
  assert.equal(s.activeDialogue?.id, 'dlg_third_marrow_bloom');
  s = { ...s, clampRemainingSeconds: 0.5, marrowBloomCount: 5 };
  s = tick(s, 1.0, realCatalog);
  assert.equal(s.activeDialogue?.id, 'dlg_sixth_marrow_bloom');

  // Excision VI Plead for Salvation -> Arc 2 Case Folder #14-B unsigned petition entry
  const readyForPlea = {
    ...s,
    highestExcisionMilestone: 5,
    ichor: 160000000000,
    cycleLifetimeIchor: 160000000000,
    phase: 4,
  };
  const arc2 = performBreachToArc2(readyForPlea, realCatalog);
  assert.equal(arc2.activeArc, GAME_ARCS.ARC_2_INVESTIGATION);
  assert.ok(arc2.triggeredDialogueIds.includes('dlg_excision_m6_breach'));
  assert.equal(arc2.activeDialogue?.id, 'dlg_arc2_entry');
  assert.ok(arc2.activeDialogue.lines.join(' ').toLowerCase().includes('plea for salvation'));
  const climaxEntry = realCatalog.entries.find((e) => e.id === 'dlg_excision_m6_breach');
  const climaxText = climaxEntry.lines.join(' ').toLowerCase();
  assert.ok(climaxText.includes('pulled out') && climaxText.includes('stay'));
  const allText = realCatalog.entries.map((e) => e.lines.join(' ')).join('\n').toLowerCase();
  for (const cliche of ['crowbar', 'rescue code', 'blow the chamber open', 'night shift ran late']) {
    assert.ok(!allText.includes(cliche), `Unexpected escape-room cliché: ${cliche}`);
  }
});

test('.drip-card multi-layer parallax CSS, randomized delay intervals, and safe animationend handling', () => {
  // Deterministic low vs high RNG seeds confirm non-uniform randomized delays & parallax bounds
  const storyLow = computeDripMotionProfile('story', () => 0);
  const storyHigh = computeDripMotionProfile('story', () => 1);
  assert.equal(storyLow.kind, 'story');
  assert.equal(storyLow.delayMs, 40);
  assert.equal(storyHigh.delayMs, 280);
  assert.ok(storyHigh.delayMs > storyLow.delayMs);
  assert.ok(storyLow.durationSec >= 15.0 && storyHigh.durationSec <= 17.0);
  assert.ok(storyLow.parallaxZ < storyHigh.parallaxZ);
  assert.ok(storyLow.ribbonDelayMs > storyLow.delayMs);
  assert.ok(storyLow.beadDelayMs > storyLow.delayMs);

  const eventLow = computeDripMotionProfile('event', () => 0);
  const eventHigh = computeDripMotionProfile('event', () => 1);
  assert.equal(eventLow.kind, 'event');
  assert.equal(eventLow.delayMs, 90);
  assert.equal(eventHigh.delayMs, 540);
  assert.ok(storyLow.durationSec > eventHigh.durationSec * 2, 'Story drip must be explicitly slower than chamber event drip');
  assert.ok(eventHigh.delayMs > storyHigh.delayMs, 'Event lane should have wider delay jitter than priority Story lane');
  assert.ok(eventHigh.parallaxZ < 0, 'Event drips stay recessed on a deeper Z-plane behind Story drips');

  // Out-of-range / NaN RNG values clamp safely without producing NaN CSS variables
  const clamped = computeDripMotionProfile('story', () => Number.NaN);
  assert.ok(Number.isFinite(clamped.delayMs) && Number.isFinite(clamped.parallaxZ));

  const applied = new Map();
  const mockCard = {
    style: {
      setProperty(name, value) {
        applied.set(name, value);
      },
    },
  };
  applyDripMotionProfile(mockCard, storyHigh);
  for (const cssVar of [
    '--drip-delay',
    '--drip-duration',
    '--drip-drift-x',
    '--drip-drift-mid-x',
    '--drip-parallax-z',
    '--drip-scale',
    '--drip-tilt',
    '--drip-ribbon-lag',
    '--drip-ribbon-delay',
    '--drip-bead-delay',
    '--drip-bead-duration',
    '--drip-bubble-parallax-x',
    '--drip-bubble-parallax-y',
    '--drip-bubble-delay',
  ]) {
    assert.ok(applied.has(cssVar), `Missing CSS custom property ${cssVar}`);
    assert.ok(!String(applied.get(cssVar)).includes('NaN'), `NaN detected in ${cssVar}`);
  }

  // Verify docs/styles.css defines 3D parallax container, both fill-mode, viscous keyframes, and reduced motion
  const css = fs.readFileSync(path.join(docsDir, 'styles.css'), 'utf8');
  assert.ok(css.includes('perspective: 760px;'));
  assert.ok(css.includes('transform-style: preserve-3d;'));
  assert.ok(css.includes('var(--drip-delay, 0ms) both;'));
  assert.ok(css.includes('@keyframes drip-ribbon-seep'));
  assert.ok(css.includes('@keyframes drip-bead-swell'));
  assert.ok(css.includes('@keyframes drip-bubble-parallax'));
  assert.ok(css.includes('@keyframes tab-clot-pulse'));
  assert.ok(css.includes('.deck-tab--alert:not(.is-active)'));
  assert.ok(css.includes('.drip-lane--event.is-suppressed'));
  assert.ok(css.includes('@media (prefers-reduced-motion: reduce)'));

  // Verify ui.js ignores bubbled child animationend events, suppresses chamber drips during active story drips, and omits clinical meta-labels
  const uiSrc = fs.readFileSync(path.join(docsDir, 'src', 'ui.js'), 'utf8');
  assert.ok(uiSrc.includes('if (event.target !== card) return;'));
  assert.ok(uiSrc.includes('storyDripLayer.childElementCount > 0'));
  assert.ok(uiSrc.includes("eventDripLayer.classList.add('is-suppressed')"));
  for (const forbidden of [
    'PHASE 0',
    'NEXT MILESTONE',
    'MILESTONE M',
    'EXCISION MILESTONES',
    'EXCISION VI //',
    'FLOOR DRAIN GRATE // M2',
    'NEIGHBOR APERTURE // M3',
    'VALVE CLAMP // M4',
    'THE OSSIFIED THROAT // M5',
    'ARC 02 //',
    'ARC 1 SCAR ECHOES',
    '[RANK ',
  ]) {
    assert.ok(!uiSrc.includes(forbidden), `ui.js must not render clinical meta-label: ${forbidden}`);
  }
});

test('Web procedural audio cue resolver, ambient profile escalation, and independent mute persistence match Android 1:1', () => {
  assert.equal(Object.keys(SOUND_CUES).length, 29);

  const start = initialState(realCatalog);

  // Failed unaffordable buy stays silent
  const failedBuy = buyCreature(start, CREATURE_TYPES.SUTURE_HAND.id, realCatalog);
  assert.deepEqual(
    resolveTransitionCues(start, failedBuy, SOUND_ACTION_HINTS.CREATURE_BUY),
    [],
  );

  // All 4 purchasable organisms resolve to distinct signature cues
  const richBase = {
    ...start,
    ichor: 50000,
    cycleLifetimeIchor: 50000,
    phase: 3,
    highestExcisionMilestone: 2,
    triggeredDialogueIds: [
      'dlg_p0_suture_hand',
      'dlg_p1_leech_clerk',
      'dlg_p2_weeping_lamp',
      'dlg_m2_sump_siphon',
    ],
  };
  for (const [creatureId, expectedCue] of [
    [CREATURE_TYPES.SUTURE_HAND.id, SOUND_CUES.CREATURE_BUY_SUTURE_HAND],
    [CREATURE_TYPES.LEECH_CLERK.id, SOUND_CUES.CREATURE_BUY_LEECH_CLERK],
    [CREATURE_TYPES.WEEPING_LAMP.id, SOUND_CUES.CREATURE_BUY_WEEPING_LAMP],
    [CREATURE_TYPES.SUMP_SIPHON.id, SOUND_CUES.CREATURE_BUY_SUMP_SIPHON],
  ]) {
    const bought = buyCreature(richBase, creatureId, realCatalog);
    assert.ok(
      resolveTransitionCues(richBase, bought, SOUND_ACTION_HINTS.CREATURE_BUY).includes(
        expectedCue,
      ),
      `Expected ${creatureId} to emit ${expectedCue}`,
    );
  }

  // First valve press emits VALVE_PRESS + STORY_DRIP
  const afterTap = pressValve(start, realCatalog);
  assert.deepEqual(
    resolveTransitionCues(start, afterTap, SOUND_ACTION_HINTS.VALVE_PRESS),
    [SOUND_CUES.VALVE_PRESS, SOUND_CUES.STORY_DRIP],
  );

  // Dev console mutations emit zero cues; switching tabs has no sound cue
  const devState = devAssignArc1Resources(start, { ichor: 500000 }, realCatalog);
  assert.deepEqual(resolveTransitionCues(start, devState, SOUND_ACTION_HINTS.DEV), []);
  assert.equal('TAB_SWITCH' in SOUND_CUES, false);

  // Passive tick emits ECHO_PULSE, MARROW_BLOOM, and PHASE_SHIFT
  const tickBase = {
    ...start,
    phase: 1,
    ichor: 1599,
    cycleLifetimeIchor: 1599,
    highestExcisionMilestone: 4,
    creatures: { [CREATURE_TYPES.SUTURE_HAND.id]: 4 },
    echoDiversionPercent: 50,
    echoChargeSeconds: ORGAN_BALANCE.ECHO_PULSE_INTERVAL_SEC - 0.1,
    clampRemainingSeconds: 0.1,
  };
  const afterTick = tick(tickBase, 0.5, realCatalog);
  const tickCues = resolveTransitionCues(tickBase, afterTick, SOUND_ACTION_HINTS.TICK);
  assert.ok(tickCues.includes(SOUND_CUES.ECHO_PULSE));
  assert.ok(tickCues.includes(SOUND_CUES.MARROW_BLOOM));
  assert.ok(tickCues.includes(SOUND_CUES.PHASE_SHIFT));

  // Excision VI Plead for Salvation -> PLEAD_SALVATION, Arc 2 Stamp -> ARC2_STAMP, Ch4 -> ARC2_FILE_ZERO, Arc 3 -> ARC3_ENTER
  const readyPlea = {
    ...start,
    highestExcisionMilestone: 5,
    ichor: 160000000000,
    cycleLifetimeIchor: 160000000000,
    phase: 4,
  };
  const arc2 = performBreachToArc2(readyPlea, realCatalog);
  assert.deepEqual(
    resolveTransitionCues(readyPlea, arc2, SOUND_ACTION_HINTS.BREACH_TO_ARC2),
    [SOUND_CUES.PLEAD_SALVATION],
  );

  const stamped = inspectCaseFile(arc2, realCatalog);
  assert.ok(
    resolveTransitionCues(arc2, stamped, SOUND_ACTION_HINTS.ARC2_STAMP).includes(
      SOUND_CUES.ARC2_STAMP,
    ),
  );

  const ch3 = {
    ...arc2,
    arc2: { ...arc2.arc2, leads: 250000, lifetimeLeads: 250000, silence: 250, completedChapters: 3 },
  };
  const ch4 = connectNextChapter(ch3, realCatalog);
  assert.deepEqual(
    resolveTransitionCues(ch3, ch4, SOUND_ACTION_HINTS.ARC2_CHAPTER),
    [SOUND_CUES.ARC2_FILE_ZERO],
  );

  const arc3 = enterArc3Convergence(ch4, realCatalog);
  assert.ok(
    resolveTransitionCues(ch4, arc3, SOUND_ACTION_HINTS.ARC3_ENTER).includes(SOUND_CUES.ARC3_ENTER),
  );

  // Ambient profile escalation across Phases 0..4, Stillness, Clamp, Arc 2, and Arc 3
  const p0 = computeAmbientProfile(start);
  const p4 = computeAmbientProfile({ ...start, phase: 4 });
  const still = computeAmbientProfile({ ...start, phase: 2, isStillnessActive: true });
  const active = computeAmbientProfile({ ...start, phase: 2, isStillnessActive: false });
  const clamped = computeAmbientProfile({ ...start, phase: 4, clampRemainingSeconds: 5 });
  const arc2Profile = computeAmbientProfile({
    ...arc2,
    arc2: { ...arc2.arc2, complicity: 0.75, isSilenceActive: true },
  });
  const arc3Profile = computeAmbientProfile(arc3);

  assert.ok(p4.humGain > p0.humGain);
  assert.ok(p4.fleshDepth > p0.fleshDepth);
  assert.equal(p0.cardiacPulse, 0);
  assert.ok(p4.cardiacPulse > 0.5);
  assert.ok(still.dripRate > active.dripRate);
  assert.ok(still.humGain < active.humGain);
  assert.ok(clamped.clampTension > 0.8);
  assert.ok(arc2Profile.rainMix > 0.7 && arc2Profile.complicityBleed >= 0.75);
  assert.ok(arc3Profile.rainMix > 0.3 && arc3Profile.fleshDepth > 0.2);

  // Mute preference defaults to unmuted (false), persists independently of game save, and works headlessly
  const storeMap = new Map();
  const mockStorage = {
    getItem: (k) => (storeMap.has(k) ? storeMap.get(k) : null),
    setItem: (k, v) => storeMap.set(k, String(v)),
  };
  assert.equal(readMutedPreference(mockStorage), false);
  writeMutedPreference(true, mockStorage);
  assert.equal(storeMap.get(AUDIO_MUTE_STORAGE_KEY), 'true');
  assert.equal(readMutedPreference(mockStorage), true);

  const webEngine = createWebAudioEngine({ storage: mockStorage });
  assert.equal(webEngine.isMuted(), true);
  assert.equal(webEngine.toggleMuted(), false);
  assert.equal(storeMap.get(AUDIO_MUTE_STORAGE_KEY), 'false');
  webEngine.playCues([SOUND_CUES.VALVE_PRESS], 0);
  webEngine.updateAmbience(p4);
  webEngine.setForegroundActive(false);
  webEngine.dispose();
});

test('Web tab alert evaluator, Grafting-in-Organisms alignment, and unseen/readiness clearing match Android 1:1', () => {
  const base = initialState(realCatalog);

  // Phase 2 unlocks JARS; unseen jars trigger alert while on apparatus tab
  const p2 = { ...base, ichor: 1700, cycleLifetimeIchor: 1700, phase: 2, stillness: 0 };
  const p2JarKeys = arc1UnlockKeys(p2, 'jars_and_graft');
  assert.ok(p2JarKeys.has('jar_yesterdays_dust'));
  assert.equal(p2JarKeys.has('jar_salt_from_the_grout'), false);
  assert.equal(shouldAlertArc1Tab(p2, 'jars_and_graft', 'apparatus', new Map()), true);

  // Opening JARS clears unseen alert when no jar is affordable
  const seen = new Map([['jars_and_graft', new Set(p2JarKeys)]]);
  assert.equal(shouldAlertArc1Tab(p2, 'jars_and_graft', 'apparatus', seen), false);

  // Becoming able to afford a jar pulses JARS while inactive, never while active
  const p2Afford = { ...p2, stillness: 30 };
  assert.equal(isArc1TabReady(p2Afford, 'jars_and_graft'), true);
  assert.equal(shouldAlertArc1Tab(p2Afford, 'jars_and_graft', 'apparatus', seen), true);
  assert.equal(shouldAlertArc1Tab(p2Afford, 'jars_and_graft', 'jars_and_graft', new Map()), false);

  // Grafting (Choir-Tendril & Choir-Lung) lives inside apparatus (ORGANISMS)
  const m4Ready = {
    ...base,
    phase: 3,
    cycle: 1,
    highestExcisionMilestone: 4,
    stillness: 250,
    graftedTendrils: 1,
    creatures: {
      [CREATURE_TYPES.SUTURE_HAND.id]: 2,
      [CREATURE_TYPES.LEECH_CLERK.id]: 2,
      [CREATURE_TYPES.WEEPING_LAMP.id]: 2,
    },
  };
  const appKeys = arc1UnlockKeys(m4Ready, 'apparatus');
  assert.ok(appKeys.has('graft_tendril'));
  assert.ok(appKeys.has('graft_choir_lung'));
  assert.equal(isArc1TabReady(m4Ready, 'apparatus'), true);
  assert.equal(shouldAlertArc1Tab(m4Ready, 'apparatus', 'chamber_rites', new Map([['apparatus', new Set(appKeys)]])), true);

  // Cycle 0 EXCISION stays hidden until ready, pulses once, and stops pulsing after first click
  assert.equal(arc1UnlockKeys(p2, 'excision').size, 0);
  const exciseReady = { ...base, ichor: 220000, cycleLifetimeIchor: 220000, phase: 4 };
  assert.equal(isArc1TabReady(exciseReady, 'excision'), false);
  assert.equal(shouldAlertArc1Tab(exciseReady, 'excision', 'apparatus', new Map()), true);
  const seenExcision = new Map([['excision', new Set(arc1UnlockKeys(exciseReady, 'excision'))]]);
  assert.equal(shouldAlertArc1Tab(exciseReady, 'excision', 'apparatus', seenExcision), false);

  // SCARS pulses with fresh Scar budget, stops after first click even with unspent Scars, and re-pulses after earning new Scars
  const withScars = {
    ...base,
    cycle: 1,
    highestExcisionMilestone: 1,
    scarTissue: 4,
    availableScarTissue: 4,
  };
  assert.equal(isArc1TabReady(withScars, 'scar_tree'), false);
  assert.equal(shouldAlertArc1Tab(withScars, 'scar_tree', 'apparatus', new Map()), true);
  const seenScars = new Map([['scar_tree', new Set(arc1UnlockKeys(withScars, 'scar_tree'))]]);
  assert.equal(shouldAlertArc1Tab(withScars, 'scar_tree', 'apparatus', seenScars), false);
  const earnedMoreScars = { ...withScars, scarTissue: 9, availableScarTissue: 9 };
  assert.equal(shouldAlertArc1Tab(earnedMoreScars, 'scar_tree', 'apparatus', seenScars), true);

  // Crossing M2 (1.8M) re-pulses EXCISION after prior M1/cycle-1 sighting
  const cycle1Ready = { ...withScars, ichor: 300000, cycleLifetimeIchor: 300000, phase: 4 };
  const seenCycle1Excision = new Map([['excision', new Set(arc1UnlockKeys(cycle1Ready, 'excision'))]]);
  assert.equal(shouldAlertArc1Tab(cycle1Ready, 'excision', 'apparatus', seenCycle1Excision), false);
  const crossedM2 = { ...cycle1Ready, ichor: 1800000, cycleLifetimeIchor: 1800000 };
  assert.equal(shouldAlertArc1Tab(crossedM2, 'excision', 'apparatus', seenCycle1Excision), true);

  // Arc 2 unseen chapter & readiness checks
  const arc2Ready = {
    ...devSetActiveArc(base, GAME_ARCS.ARC_2_INVESTIGATION, realCatalog),
  };
  arc2Ready.arc2 = { ...arc2Ready.arc2, leads: 1000, lifetimeLeads: 1000, silence: 50 };
  assert.ok(arc2UnlockKeys(arc2Ready, 'evidence_board').has('arc2_ch_1'));
  assert.equal(isArc2TabReady(arc2Ready, 'deductions'), true);
  assert.equal(
    shouldAlertArc2Tab(arc2Ready, 'deductions', 'contacts', new Map([['deductions', new Set(['arc2_deductions'])]])),
    true,
  );
  assert.equal(shouldAlertArc2Tab(arc2Ready, 'deductions', 'deductions', new Map()), false);
});

test('Web wall/chamber event drip gate guarantees first sighting (100%) and applies 30% chance on repeats', () => {
  assert.equal(REPEAT_EVENT_DRIP_CHANCE, 0.3);

  const seen = new Set();
  // Empty key never triggers
  assert.equal(shouldTriggerEventDrip('', seen, () => 0.0), false);
  assert.equal(shouldTriggerEventDrip(null, seen, () => 0.0), false);

  // First sighting of a wall event always triggers even when rng returns 0.99
  assert.equal(shouldTriggerEventDrip('anomaly_pale_tooth', seen, () => 0.99), true);
  seen.add('anomaly_pale_tooth');

  // Repeat sightings use strict < 0.30 probability
  assert.equal(shouldTriggerEventDrip('anomaly_pale_tooth', seen, () => 0.0), true);
  assert.equal(shouldTriggerEventDrip('anomaly_pale_tooth', seen, () => 0.299), true);
  assert.equal(shouldTriggerEventDrip('anomaly_pale_tooth', seen, () => 0.3), false);
  assert.equal(shouldTriggerEventDrip('anomaly_pale_tooth', seen, () => 0.99), false);
  assert.equal(shouldTriggerEventDrip('anomaly_pale_tooth', seen, () => Number.NaN), false);

  // A distinct unseen wall event still triggers 100% regardless of rng
  assert.equal(shouldTriggerEventDrip('anomaly_glass_eye', seen, () => 0.99), true);

  // Verify Web ui.js checks story suppression BEFORE repeat gating & seen marking, and clears on reset
  const uiSrc = fs.readFileSync(path.join(docsDir, 'src', 'ui.js'), 'utf8');
  const suppressIdx = uiSrc.indexOf('storyDripLayer.childElementCount > 0');
  const gateIdx = uiSrc.indexOf('shouldTriggerEventDrip(resolvedKey, seenEventDripKeys');
  const markIdx = uiSrc.indexOf('seenEventDripKeys.add(resolvedKey)');
  assert.ok(suppressIdx > 0 && gateIdx > suppressIdx && markIdx > gateIdx);
  assert.ok(uiSrc.includes('seenEventDripKeys.clear();'));

  // Verify app.js passes explicit event keys and renders story drips before observing chamber events
  const appSrc = fs.readFileSync(path.join(docsDir, 'src', 'app.js'), 'utf8');
  for (const keyToken of [
    "'echo_answer'",
    '`graft_${sacrificeKind}`',
    "'sump_dredge'",
    "'valve_clamp'",
    '`anomaly_${anomaly.type}`',
    "`desk_${anomaly?.type ?? 'anomaly'}`",
  ]) {
    assert.ok(appSrc.includes(keyToken), `app.js must pass explicit event key ${keyToken}`);
  }
  const anomalyApplyIdx = appSrc.indexOf('applyState(interactAnomaly(state, anomalyId, catalog)');
  const anomalyDripIdx = appSrc.indexOf('`anomaly_${anomaly.type}`');
  assert.ok(anomalyApplyIdx > 0 && anomalyDripIdx > anomalyApplyIdx, 'Anomaly drip must run after applyState so story drips suppress it cleanly');
});

test('Web Weeping Lamps hang strictly on left/right flanks behind foreground HUD (no middle lamp over income)', () => {
  assert.deepEqual(getWeepingLampLayout(0), []);
  for (let count = 1; count <= 6; count += 1) {
    const slots = getWeepingLampLayout(count);
    assert.equal(slots.length, Math.min(count, 5));
    for (const slot of slots) {
      assert.ok(
        slot.x <= 0.24 || slot.x >= 0.76,
        `Lamp at x=${slot.x} for count=${count} must stay clear of center income band (0.25..0.75)`,
      );
    }
  }

  // Verify drawChamberBackdrop renders lamps behind foreground HUD while drawForegroundLampsAndSacrifice only draws sacrifice FX
  const canvasSrc = fs.readFileSync(path.join(docsDir, 'src', 'chamberCanvas.js'), 'utf8');
  const backdropFnStart = canvasSrc.indexOf('export function drawChamberBackdrop(');
  const foregroundFnStart = canvasSrc.indexOf('export function drawForegroundLampsAndSacrifice(');
  const lampsCallIdx = canvasSrc.indexOf('drawWeepingLampsCeiling(ctx, width, lampCount');
  assert.ok(
    backdropFnStart >= 0 && foregroundFnStart > backdropFnStart && lampsCallIdx > backdropFnStart && lampsCallIdx < foregroundFnStart,
    'Weeping lamps must draw inside drawChamberBackdrop behind foreground income text',
  );
});

test('Web wall anomaly nodes use subtle CSS keyframe breathing/pulsing with desynchronized delays and reduced-motion support', () => {
  const css = fs.readFileSync(path.join(docsDir, 'styles.css'), 'utf8');
  const uiSrc = fs.readFileSync(path.join(docsDir, 'src', 'ui.js'), 'utf8');

  // Verify both organic breathing and arterial pulse keyframes exist with translate(-50%, -50%) centering preserved
  assert.ok(css.includes('@keyframes wall-anomaly-breathe'));
  assert.ok(css.includes('@keyframes wall-anomaly-pulse'));
  assert.ok(css.includes('.anomaly-layer .anomaly-node,\n.anomaly-node--wall'));
  assert.ok(css.includes('animation: wall-anomaly-breathe var(--breath-duration, 4.6s)'));

  // Verify all 4 wall anomaly types have dedicated breathing tokens (and swollen_nodule uses pulse)
  for (const selector of [
    '.anomaly-node--pale_tooth',
    '.anomaly-node--glass_eye',
    '.anomaly-node--swollen_nodule',
    '.anomaly-node--chitin_rib',
  ]) {
    assert.ok(css.includes(selector), `styles.css must define ${selector}`);
  }
  const noduleIdx = css.indexOf('.anomaly-node--swollen_nodule');
  const ribIdx = css.indexOf('.anomaly-node--chitin_rib');
  assert.ok(
    noduleIdx > 0 && ribIdx > noduleIdx && css.slice(noduleIdx, ribIdx).includes('animation-name: wall-anomaly-pulse;'),
    'Swollen nodule must use the arterial wall-anomaly-pulse keyframe',
  );

  // Verify hover override and prefers-reduced-motion disable animation cleanly
  assert.ok(css.includes('.anomaly-layer .anomaly-node:hover'));
  const reducedMotionBlock = css.slice(css.indexOf('@media (prefers-reduced-motion: reduce)'));
  assert.ok(
    reducedMotionBlock.includes('.anomaly-layer .anomaly-node') &&
      reducedMotionBlock.includes('animation: none;'),
    'Reduced-motion media query must disable wall anomaly breathing animation',
  );

  // Verify ui.js tags wall anomalies with anomaly-node--wall and negative --breath-delay (without tagging desk anomalies)
  assert.ok(uiSrc.includes('anomaly-node anomaly-node--wall anomaly-node--${anomaly.type}'));
  assert.ok(uiSrc.includes("btn.style.setProperty('--breath-delay'"));
  const deskSyncStart = uiSrc.indexOf('function syncDeskAnomalies(');
  const deskSyncSlice = uiSrc.slice(deskSyncStart, deskSyncStart + 1200);
  assert.ok(
    !deskSyncSlice.includes('anomaly-node--wall'),
    'Arc 2 desk anomalies must stay separate from Arc 1 wall anomaly breathing class',
  );
});

test('Web Colony Proliferation (M3 bulk-buy), Echo burst rebalancing, and compact valve yield match Android 1:1', () => {
  // Proliferate locked at M2
  const m2 = {
    ...initialState(realCatalog),
    highestExcisionMilestone: 2,
    cycle: 2,
    ichor: 50000,
    cycleLifetimeIchor: 50000,
  };
  assert.equal(isColonyProliferationUnlocked(m2), false);
  assert.equal(buyMaxCreature(m2, CREATURE_TYPES.SUTURE_HAND.id, realCatalog), m2);
  assert.equal(arc1UnlockKeys(m2, 'apparatus').has('colony_proliferation'), false);

  // Unlocked at M3: exact parity with sequential buyCreature across all 4 organisms
  const m3 = {
    ...m2,
    highestExcisionMilestone: 3,
    ichor: 250000,
    cycleLifetimeIchor: 250000,
    unlockedScarNodes: ['colony_inertia'],
    creatures: { [CREATURE_TYPES.SUTURE_HAND.id]: 3, [CREATURE_TYPES.LEECH_CLERK.id]: 1 },
  };
  assert.equal(isColonyProliferationUnlocked(m3), true);
  assert.equal(arc1UnlockKeys(m3, 'apparatus').has('colony_proliferation'), true);

  for (const spec of Object.values(CREATURE_TYPES)) {
    const quote = maxAffordableCreatureQuote(m3, spec.id);
    assert.ok(quote.count > 0);
    let seq = m3;
    let seqCount = 0;
    while (seq.ichor >= creatureCost(seq, spec.id)) {
      seq = buyCreature(seq, spec.id, realCatalog);
      seqCount += 1;
    }
    const bulk = buyMaxCreature(m3, spec.id, realCatalog);
    assert.equal(quote.count, seqCount);
    assert.equal(creatureCount(bulk, spec.id), creatureCount(seq, spec.id));
    assert.ok(Math.abs(bulk.ichor - seq.ichor) < 0.01);
    assert.ok(bulk.ichor < creatureCost(bulk, spec.id));
  }

  // Rebalanced Echo burst at 50% diversion = 18x raw/sec
  const echo50 = {
    ...initialState(realCatalog),
    highestExcisionMilestone: 3,
    echoDiversionPercent: 50,
    echoChargeSeconds: 11.9,
    creatures: { [CREATURE_TYPES.SUMP_SIPHON.id]: 4 },
  };
  const afterEcho = tick(echo50, 0.2, realCatalog);
  assert.equal(afterEcho.echoPulsesTriggered, 1);
  assert.ok(
    Math.abs(afterEcho.ichor - (rawIchorPerSecond(echo50) * 18.0 + ichorPerSecond(echo50) * 0.2)) < 0.5,
  );

  // Compact valve yield formatting prevents late-game circle overflow
  assert.equal(formatValveYield(1), '1');
  assert.equal(formatValveYield(9840), '9,840');
  assert.equal(formatValveYield(12500), '12.5K');
  assert.equal(formatValveYield(142300), '142K');
  assert.equal(formatValveYield(2923756), '2.92M');
  assert.equal(formatValveYield(36000000000), '36B');
});

test('Web 5-stage progressive wall texture blending, heavy sump basin, sculpted Choir-Tendrils, and breathing Choir-Lungs match Android 1:1', () => {
  // Phase 0..4 Cycle 0 progression
  const p0 = computeWallLayerAlphas(0, 0, 0);
  assert.equal(p0.phase1Tile, 0.34);
  assert.equal(p0.phase2Fleshy, 0);
  assert.equal(p0.phase3Muscle, 0);
  assert.equal(p0.phase4Hollow, 0);
  assert.equal(p0.lateExcision, 0);

  const p1 = computeWallLayerAlphas(1, 0, 0);
  assert.equal(p1.phase1Tile, 0.76);
  assert.equal(p1.phase2Fleshy, 0);

  const p2 = computeWallLayerAlphas(2, 0, 0);
  assert.equal(p2.phase1Tile, 0.34);
  assert.equal(p2.phase2Fleshy, 0.8);
  assert.equal(p2.phase3Muscle, 0);

  const p3 = computeWallLayerAlphas(3, 0, 0);
  assert.equal(p3.phase3Muscle, 0.86);
  assert.equal(p3.phase4Hollow, 0);

  const p4 = computeWallLayerAlphas(4, 0, 0);
  assert.equal(p4.phase1Tile, 0);
  assert.equal(p4.phase3Muscle, 0.26);
  assert.equal(p4.phase4Hollow, 0.9);
  assert.equal(p4.lateExcision, 0);

  // Late-Excision surgical wall scar layer escalates across M3..M6 and scales cleanly by Phase
  const m3p0 = computeWallLayerAlphas(0, 3, 3);
  const m3p4 = computeWallLayerAlphas(4, 3, 3);
  const m5p4 = computeWallLayerAlphas(4, 5, 5);
  const m6p4 = computeWallLayerAlphas(4, 6, 6);
  assert.ok(m3p0.lateExcision > 0 && m3p0.lateExcision < m3p4.lateExcision);
  assert.ok(m3p4.lateExcision < m5p4.lateExcision && m5p4.lateExcision < m6p4.lateExcision);
  assert.ok(m6p4.lateExcision <= 0.82);

  // Verify chamberCanvas.js eliminated dashed CAD rings & flat side ovals, and includes new visual elements
  const canvasSrc = fs.readFileSync(path.join(docsDir, 'src', 'chamberCanvas.js'), 'utf8');
  assert.ok(!canvasSrc.includes('setLineDash([7, 7])'), 'Dashed CAD ring around valve must be replaced by bolted flange');
  assert.ok(canvasSrc.includes('wallImages.phase3') && canvasSrc.includes('wallImages.phase4') && canvasSrc.includes('wallImages.excisionLate'));
  assert.ok(canvasSrc.includes('visibleLungs'), 'Grafted Choir-Lungs must render visually around valve collar');
  assert.ok(canvasSrc.includes('const rivets ='), 'Floor sump basin must include riveted cast-iron architecture');
  assert.ok(canvasSrc.includes('helixPts') && canvasSrc.includes('segRadius'), 'Grafted Choir-Tendrils must render sculpted segmented stalks with helical sinew');

  // Verify app.js preloads all 5 wall textures + 4 photographic wall anomalies with v=3.8 cache bust
  const appSrc = fs.readFileSync(path.join(docsDir, 'src', 'app.js'), 'utf8');
  for (const wallAsset of [
    'img_wall_phase1.jpg?v=3.8',
    'img_wall_fleshy.jpg?v=3.8',
    'img_wall_phase3.jpg?v=3.8',
    'img_wall_phase4.jpg?v=3.8',
    'img_wall_excision_late.jpg?v=3.8',
    'img_anomaly_pale_tooth.jpg?v=3.8',
    'img_anomaly_glass_eye.jpg?v=3.8',
    'img_anomaly_swollen_nodule.jpg?v=3.8',
    'img_anomaly_chitin_rib.jpg?v=3.8',
  ]) {
    assert.ok(appSrc.includes(wallAsset), `app.js must preload ${wallAsset}`);
  }
});

test('Web photographic feathered wall anomalies and cast-iron / tendon-tearing / wet-squelch audio architecture match Android 1:1', () => {
  const uiSrc = fs.readFileSync(path.join(docsDir, 'src', 'ui.js'), 'utf8');
  const css = fs.readFileSync(path.join(docsDir, 'styles.css'), 'utf8');
  const audioSrc = fs.readFileSync(path.join(docsDir, 'src', 'audio.js'), 'utf8');

  // Photographic circular feathered wall anomalies replace flat icon canvases on Arc 1 wall
  assert.ok(uiSrc.includes("photo.className = 'anomaly-photo'"));
  assert.ok(uiSrc.includes('./assets/img_anomaly_${anomaly.type}.jpg?v=3.8'));
  assert.ok(css.includes('.anomaly-photo'));
  assert.ok(css.includes('mask-image: radial-gradient('));
  assert.ok(css.includes('border: none;') && css.includes('border-radius: 50%;'));

  // Cast-iron inharmonic modal ratios + organic fibrous tendon-tearing + wet mucosa squelch + slapback room
  assert.deepEqual([...IRON_MODE_RATIOS], [1.0, 1.53, 2.24, 2.89, 3.71, 4.68]);
  for (const token of [
    'createCrackleNoiseBuffer',
    'triggerIronModalRing',
    'triggerTendonTear',
    'triggerWetSquelch',
    'triggerBubbleCluster',
    'slapDelay1',
    'slapDelay2',
  ]) {
    assert.ok(audioSrc.includes(token), `docs/src/audio.js must implement ${token}`);
  }
});




