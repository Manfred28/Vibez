import test from 'node:test';
import assert from 'node:assert/strict';

import {
  CREATURE_TYPES,
  EXCISION_MILESTONES,
  GAME_ARCS,
  ORGAN_BALANCE,
  REPEATABLE_SCAR_GRAFTS,
  SCAR_NODES,
  SPECIMEN_JARS,
  WALL_ANOMALY_TYPES,
  activateValveClamp,
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
  creatureCost,
  creatureCount,
  currentBaseScarHarvest,
  cycleLifetimeIchorDisplay,
  deserializeState,
  devAssignArc1Resources,
  devSetAllScarNodes,
  devSetHighestMilestone,
  dredgeSump,
  echoPulseIntervalSec,
  effectiveCreatureIchorPerSec,
  effectiveJarCost,
  effectiveLampStillnessPerSec,
  formatValveYield,
  gazeMultiplier,
  graftCooldownDurationSec,
  graftStillnessCost,
  hasExcessMilestoneMass,
  ichorDisplay,
  ichorPerSecond,
  initialState,
  interactAnomaly,
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
  pressValve,
  progressWithinPhase,
  rawIchorPerSecond,
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
import { arc1UnlockKeys } from '../src/ui.js';
import { realCatalog } from './testHelpers.mjs';

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

  // Excision I -> Cycle 1 grants 3 Scars (2 curve + 1 First Severance bonus)
  const excisionReady = {
    ...midP4,
    ichor: 215000,
    cycleLifetimeIchor: 215000,
    phase: 4,
  };
  assert.equal(canExcise(excisionReady), true);
  assert.equal(pendingScarTissue(excisionReady), 3);

  const cycle1 = performExcision(excisionReady, realCatalog);
  assert.equal(cycle1.cycle, 1);
  assert.equal(cycle1.highestExcisionMilestone, 1);
  assert.equal(cycle1.scarTissue, 3);
  assert.equal(cycle1.availableScarTissue, 3);
  // Subsequent early harvest at 215k after M1 is cleared yields 2 Scars (no repeat bonus)
  assert.equal(
    pendingScarTissue({ ...cycle1, ichor: 215000, cycleLifetimeIchor: 215000, phase: 4 }),
    2,
  );
  const expectedScarMult = 1.0 + 1 * ORGAN_BALANCE.MILESTONE_ICHOR_RESONANCE + Math.sqrt(3) * ORGAN_BALANCE.SCAR_RESONANCE_SQRT_FACTOR;
  const expectedStillMult = 1.0 + 1 * ORGAN_BALANCE.MILESTONE_STILLNESS_RESONANCE + Math.sqrt(3) * ORGAN_BALANCE.SCAR_STILLNESS_SQRT_FACTOR;
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
  assert.equal(pendingScarTissue({ ...s0, cycleLifetimeIchor: 210000 }), 3);
  assert.equal(pendingScarTissue({ ...s0, highestExcisionMilestone: 1, cycleLifetimeIchor: 210000 }), 2);
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

test('Anti-skip sequential cap, First Severance +1 Scar bonus (M2..M6 zero), M5 Arc 2 gate, 16-node Scar Tree, and 3 Repeatable Grafts match Android 1:1', () => {
  assert.equal(SCAR_NODES.length, 16);
  assert.equal(REPEATABLE_SCAR_GRAFTS.length, 3);
  assert.equal(EXCISION_MILESTONES[0].firstClearScarBonus, 1);
  assert.ok(EXCISION_MILESTONES.slice(1).every((m) => m.firstClearScarBonus === 0));

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
  assert.equal(afterM0.scarTissue, currentBaseScarHarvest(massiveM0) + 1);
  assert.equal(afterM0.scarTissue, pendingScarTissue(massiveM0));

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
