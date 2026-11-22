import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import {
  CREATURE_TYPES,
  ORGAN_BALANCE,
  SPECIMEN_JARS,
  WALL_ANOMALY_TYPES,
  bottleJar,
  buyCreature,
  canAffordGraft,
  canExcise,
  creatureCost,
  creatureCount,
  cycleLifetimeIchorDisplay,
  deserializeState,
  effectiveCreatureIchorPerSec,
  effectiveLampStillnessPerSec,
  gazeMultiplier,
  ichorDisplay,
  ichorPerSecond,
  initialState,
  interactAnomaly,
  isApparatusUnlocked,
  isJarsAndGraftUnlocked,
  manualTapYield,
  pendingScarTissue,
  performExcision,
  performGraft,
  phaseForCount,
  pressValve,
  progressWithinPhase,
  scarMultiplier,
  scarStillnessMultiplier,
  serializeState,
  stillnessDisplay,
  stillnessPerSecond,
  tick,
  totalCreatureIchorPerSec,
} from '../src/engine.js';

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
    'src/app.js',
    'assets/dialogues.json',
    'assets/img_wall_phase1.jpg',
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

  // Verify exact dialogue parity with canonical Android dialogues.json
  const webRaw = fs.readFileSync(webDialoguesPath, 'utf8').trim();
  const androidRaw = fs.readFileSync(androidDialoguesPath, 'utf8').trim();
  assert.equal(webRaw, androidRaw, 'docs/assets/dialogues.json must match app/src/main/assets/dialogues.json');

  // Verify root index.html redirects to ./docs/ when GitHub Pages serves from /(root)
  const rootIndexPath = path.join(repoRoot, 'index.html');
  assert.ok(fs.existsSync(rootIndexPath), 'Missing root index.html redirect for default GitHub Pages root');
  const rootHtml = fs.readFileSync(rootIndexPath, 'utf8');
  assert.ok(rootHtml.includes('./docs/'), 'Root index.html must redirect to ./docs/');

  // Verify index.html uses relative ./ paths so GitHub Pages subpaths work
  const html = fs.readFileSync(path.join(docsDir, 'index.html'), 'utf8');
  assert.ok(html.includes('href="./styles.css"'));
  assert.ok(html.includes('src="./src/app.js"'));
  assert.ok(html.includes('src="./assets/img_recessed_valve.jpg"'));
  assert.ok(!html.includes('href="/styles.css"'), 'Avoid root-absolute asset paths that break GitHub Pages subpaths');
});

test('initial state starts bare in Phase 0 and valve press triggers first fragment', () => {
  const start = initialState(realCatalog);
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
  };

  const summedIchor =
    totalCreatureIchorPerSec(state, CREATURE_TYPES.SUTURE_HAND.id) +
    totalCreatureIchorPerSec(state, CREATURE_TYPES.LEECH_CLERK.id) +
    totalCreatureIchorPerSec(state, CREATURE_TYPES.WEEPING_LAMP.id);

  assert.ok(Math.abs(ichorPerSecond(state) - summedIchor) < 0.0001);
  assert.ok(Math.abs(stillnessPerSecond(state) - 2 * effectiveLampStillnessPerSec(state)) < 0.0001);
  assert.ok(
    Math.abs(
      effectiveCreatureIchorPerSec(state, CREATURE_TYPES.SUTURE_HAND.id) -
        0.6 * ORGAN_BALANCE.UNSPENT_BREATH_MULTIPLIER * 2.5 * ORGAN_BALANCE.GAZE_PASSIVE_MULTIPLIER,
    ) < 0.0001,
  );
});

test('wall anomalies, specimen jars, grafting, excision, and Cycle 1+ prestige beats match Android engine', () => {
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

  // Jars + Grafting in Phase 3
  let p3 = {
    ...initialState(realCatalog),
    cycleLifetimeIchor: 17000,
    ichor: 17000,
    stillness: 30,
    phase: 3,
    creatures: {
      [CREATURE_TYPES.SUTURE_HAND.id]: 2,
      [CREATURE_TYPES.LEECH_CLERK.id]: 2,
    },
  };
  p3 = bottleJar(p3, SPECIMEN_JARS.YESTERDAYS_DUST.id, realCatalog);
  assert.ok(p3.ownedJars.includes(SPECIMEN_JARS.YESTERDAYS_DUST.id));
  assert.equal(canAffordGraft(p3), true);
  p3 = performGraft(p3, realCatalog);
  assert.equal(p3.graftedTendrils, 1);
  assert.equal(p3.activeDialogue?.id, 'dlg_p3_first_graft');

  // Excision -> Cycle 1 dual passive Scar multipliers + Cycle 1+ narrative beats
  const excisionReady = {
    ...p3,
    ichor: 215000,
    cycleLifetimeIchor: 215000,
    phase: 4,
  };
  assert.equal(canExcise(excisionReady), true);
  assert.equal(pendingScarTissue(excisionReady), 1);

  const cycle1 = performExcision(excisionReady, realCatalog);
  assert.equal(cycle1.cycle, 1);
  assert.equal(cycle1.scarTissue, 1);
  assert.equal(scarMultiplier(cycle1), 1.75);
  assert.equal(scarStillnessMultiplier(cycle1), 1.25);
  assert.equal(cycle1.activeDialogue?.id, 'dlg_cycle_1_excision');

  const cycle1P1 = pressValve({ ...cycle1, ichor: 74, cycleLifetimeIchor: 74 }, realCatalog);
  assert.equal(cycle1P1.activeDialogue?.id, 'dlg_cycle_1_scar_handwriting');

  const cycle1P2 = pressValve({ ...cycle1P1, ichor: 1599, cycleLifetimeIchor: 1599 }, realCatalog);
  assert.equal(cycle1P2.activeDialogue?.id, 'dlg_cycle_1_returned_letter');
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

test('save serialization round-trips state cleanly and rejects corrupt JSON safely', () => {
  let state = initialState(realCatalog);
  for (let i = 0; i < 80; i += 1) state = pressValve(state, realCatalog);
  state = buyCreature(state, CREATURE_TYPES.SUTURE_HAND.id, realCatalog);
  state = tick(state, 5.0, realCatalog);

  const serialized = serializeState(state);
  const restored = deserializeState(serialized, realCatalog);

  assert.ok(restored !== null);
  assert.equal(ichorDisplay(restored), ichorDisplay(state));
  assert.equal(stillnessDisplay(restored), stillnessDisplay(state));
  assert.equal(creatureCount(restored, CREATURE_TYPES.SUTURE_HAND.id), 1);
  assert.equal(restored.phase, state.phase);
  assert.equal(phaseForCount(cycleLifetimeIchorDisplay(restored)), restored.phase);
  assert.equal(progressWithinPhase(147500), 0.5);
  assert.equal(restored.discoveredDialogues.length, state.discoveredDialogues.length);
  assert.equal(restored.activeDialogue?.id, state.activeDialogue?.id);

  assert.equal(deserializeState('{invalid json', realCatalog), null);
  assert.equal(deserializeState(null, realCatalog), null);
});
