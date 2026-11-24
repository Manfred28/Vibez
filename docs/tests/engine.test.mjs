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
  isJarUnlocked,
  isJarsAndGraftUnlocked,
  isRepeatableGraftsUnlocked,
  jarEffectBoost,
  manualTapYield,
  marrowBloomDurationSec,
  massRequiredForScarCount,
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
  stillnessPerSecond,
  tick,
  toggleDrainSeal,
  totalCreatureIchorPerSec,
  unlockScarNode,
  valveClampDurationSec,
} from '../src/engine.js';
import { applyDripMotionProfile, computeDripMotionProfile } from '../src/ui.js';
import {
  AUDIO_MUTE_STORAGE_KEY,
  SOUND_ACTION_HINTS,
  SOUND_CUES,
  computeAmbientProfile,
  createWebAudioEngine,
  readMutedPreference,
  resolveTransitionCues,
  writeMutedPreference,
} from '../src/audio.js';

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
  assert.ok(html.includes('href="./styles.css?v=2.9"'));
  assert.ok(html.includes('src="./src/app.js?v=2.9"'));
  assert.ok(html.includes('src="./assets/img_recessed_valve.jpg"'));
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

  state = unlockScarNode(state, 'calloused_palm', realCatalog);
  state = unlockScarNode(state, 'shorter_silence', realCatalog);
  state = unlockScarNode(state, 'wax_sealed_memory', realCatalog);
  assert.equal(state.availableScarTissue, 2);

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

  // M4 with 50B still cannot Breach Arc 2 and advances strictly to M5
  const massiveM4 = {
    ...afterM0,
    highestExcisionMilestone: 4,
    ichor: 50000000000,
    cycleLifetimeIchor: 50000000000,
    phase: 4,
  };
  assert.equal(canBreachToArc2(massiveM4), false);
  const afterM4 = performExcision(massiveM4, realCatalog);
  assert.equal(afterM4.highestExcisionMilestone, 5);

  // M5 with 12B opens Breach to Arc 2 and grants 0 bonus Scars
  const readyM5 = { ...afterM4, ichor: 12000000000, cycleLifetimeIchor: 12000000000, phase: 4 };
  assert.equal(canBreachToArc2(readyM5), true);
  const scarBeforeBreach = readyM5.scarTissue;
  const breached = performBreachToArc2(readyM5, realCatalog);
  assert.equal(breached.activeArc, GAME_ARCS.ARC_2_INVESTIGATION);
  assert.equal(breached.highestExcisionMilestone, 6);
  assert.equal(breached.scarTissue, scarBeforeBreach);

  // New Tier 4-5 nodes: MEMBRANE_RECALL, PERPETUAL_SEAM, CATATONIC_TIDE, ECHO_OSSUARY, BLOOM_INCISION
  const recallBase = {
    ...initialState(realCatalog),
    highestExcisionMilestone: 2,
    cycle: 2,
    ichor: 300000,
    cycleLifetimeIchor: 300000,
    phase: 4,
    unlockedScarNodes: ['membrane_recall'],
  };
  const resetRecall = performExcision(recallBase, realCatalog);
  assert.equal(creatureCount(resetRecall, CREATURE_TYPES.WEEPING_LAMP.id), 1);
  assert.equal(creatureCount(resetRecall, CREATURE_TYPES.SUMP_SIPHON.id), 1);

  const perpetual = {
    ...initialState(realCatalog),
    unlockedScarNodes: ['perpetual_seam'],
    creatures: { [CREATURE_TYPES.WEEPING_LAMP.id]: 1 },
    secondsSinceLastTap: 5.0,
    isStillnessActive: true,
  };
  const afterPerp = tick(perpetual, ORGAN_BALANCE.PERPETUAL_SEAM_INTERVAL_SEC + 0.1, realCatalog);
  assert.equal(afterPerp.isStillnessActive, true);
  assert.ok(afterPerp.ichor >= manualTapYield(perpetual));

  const ossuary = { ...initialState(realCatalog), highestExcisionMilestone: 3, unlockedScarNodes: ['echo_ossuary'] };
  assert.equal(echoPulseIntervalSec(ossuary), 9.0);
  assert.equal(relicAmplifier(ossuary), 1.45);

  const bloomNode = { ...initialState(realCatalog), highestExcisionMilestone: 4, unlockedScarNodes: ['bloom_incision'] };
  assert.equal(valveClampDurationSec(bloomNode), 15.0);
  assert.equal(marrowBloomDurationSec(bloomNode), 32.0);

  // Repeatable Surgical Grafts gated behind FIRST_INCISION, persist across Excisions, cleared by dev reset
  let rep = { ...initialState(realCatalog), highestExcisionMilestone: 1, scarTissue: 20, availableScarTissue: 20 };
  assert.equal(isRepeatableGraftsUnlocked(rep), false);
  assert.equal(canBuyRepeatableGraft(rep, 'keloid_thickening'), false);

  rep = unlockScarNode(rep, 'first_incision', realCatalog);
  assert.equal(isRepeatableGraftsUnlocked(rep), true);
  assert.equal(repeatableCost(rep, 'keloid_thickening'), 3);
  rep = buyRepeatableScarGraft(rep, 'keloid_thickening', realCatalog);
  assert.equal(repeatableRank(rep, 'keloid_thickening'), 1);
  assert.equal(repeatableCost(rep, 'keloid_thickening'), 5);

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
  assert.equal(EXCISION_MILESTONES[5].code, 'EXCISION VI // PLEAD FOR SALVATION');

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
    ichor: 12000000000,
    cycleLifetimeIchor: 12000000000,
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
  assert.ok(storyLow.durationSec >= 6.45 && storyHigh.durationSec <= 7.35);
  assert.ok(storyLow.parallaxZ < storyHigh.parallaxZ);
  assert.ok(storyLow.ribbonDelayMs > storyLow.delayMs);
  assert.ok(storyLow.beadDelayMs > storyLow.delayMs);

  const eventLow = computeDripMotionProfile('event', () => 0);
  const eventHigh = computeDripMotionProfile('event', () => 1);
  assert.equal(eventLow.kind, 'event');
  assert.equal(eventLow.delayMs, 90);
  assert.equal(eventHigh.delayMs, 540);
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
  assert.ok(css.includes('@media (prefers-reduced-motion: reduce)'));

  // Verify ui.js ignores bubbled child animationend events so ribbon/bubble animations never kill .drip-card early
  const uiSrc = fs.readFileSync(path.join(docsDir, 'src', 'ui.js'), 'utf8');
  assert.ok(uiSrc.includes('if (event.target !== card) return;'));
});

test('Web procedural audio cue resolver, ambient profile escalation, and independent mute persistence match Android 1:1', () => {
  assert.equal(Object.keys(SOUND_CUES).length, 26);

  const start = initialState(realCatalog);

  // Failed unaffordable buy stays silent
  const failedBuy = buyCreature(start, CREATURE_TYPES.SUTURE_HAND.id, realCatalog);
  assert.deepEqual(
    resolveTransitionCues(start, failedBuy, SOUND_ACTION_HINTS.CREATURE_BUY),
    [],
  );

  // First valve press emits VALVE_PRESS + STORY_DRIP
  const afterTap = pressValve(start, realCatalog);
  assert.deepEqual(
    resolveTransitionCues(start, afterTap, SOUND_ACTION_HINTS.VALVE_PRESS),
    [SOUND_CUES.VALVE_PRESS, SOUND_CUES.STORY_DRIP],
  );

  // Dev console mutations emit zero cues
  const devState = devAssignArc1Resources(start, { ichor: 500000 }, realCatalog);
  assert.deepEqual(resolveTransitionCues(start, devState, SOUND_ACTION_HINTS.DEV), []);

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
    ichor: 12000000000,
    cycleLifetimeIchor: 12000000000,
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
