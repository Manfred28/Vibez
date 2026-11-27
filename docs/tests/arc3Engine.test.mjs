import test from 'node:test';
import assert from 'node:assert/strict';

import {
  ARC3_BALANCE,
  ARC3_STRATA,
  ARC3_UNMAKE_ACTS,
  GAME_ARCS,
  ORGAN_BALANCE,
  arc3ActiveClotPerSec,
  arc3CanDriveWedge,
  arc3CanPerformFinalPull,
  arc3FloodLevel,
  arc3IsActivelyClotting,
  arc3IsClotFrozen,
  arc3IsInFinale,
  arc3IsJarShattered,
  arc3IsValveSheared,
  arc3LegacyFractureMultiplier,
  arc3LegacyHemorrhageMultiplier,
  arc3ShatteredJarCount,
  arc3TelemetryBlurFactor,
  arc3TotalRuptureInteractions,
  arc3UnstitchedScarCount,
  arc3WrenchFractureYield,
  arc3WrenchHemorrhageYield,
  deserializeState,
  devAssignArc3Resources,
  devJumpArc3Finale,
  devResetArc3,
  devSetActiveArc,
  devSetArc3Stratum,
  devSpawnBreachRupture,
  driveMemoryWedge,
  initialState,
  performArc3FinalPull,
  performArc3Unmake,
  performExcision,
  serializeState,
  tearBreachRupture,
  tick,
  wrenchArc3Breach,
} from '../src/engine.js';
import { realCatalog } from './testHelpers.mjs';

function freshArc3() {
  return devSetActiveArc(initialState(realCatalog), GAME_ARCS.ARC_3_CONVERGENCE, realCatalog);
}

test('Arc 3 Breach wrenching builds momentum, caps fracture at stratum goal, and triggers Phase 7 dialogues', () => {
  let state = freshArc3();
  assert.equal(state.arc3.fracture, 0);
  assert.equal(state.arc3.momentumCharge, 0);

  state = wrenchArc3Breach(state, realCatalog);
  assert.equal(state.arc3.wrenchCount, 1);
  assert.equal(state.arc3.fracture, ARC3_BALANCE.BASE_WRENCH_FRACTURE);
  assert.equal(state.arc3.hemorrhage, ARC3_BALANCE.BASE_WRENCH_HEMORRHAGE);
  assert.equal(state.arc3.momentumCharge, ARC3_BALANCE.MOMENTUM_GAIN_PER_TAP);

  const yieldAfterOne = arc3WrenchFractureYield(state);
  for (let i = 0; i < 12; i += 1) {
    state = wrenchArc3Breach(state, realCatalog);
  }
  assert.ok(state.triggeredDialogueIds.includes('dlg_arc3_first_wrench'));
  assert.equal(state.arc3.momentumCharge, 1.0);
  assert.ok(arc3WrenchFractureYield(state) > yieldAfterOne);

  for (let i = 0; i < 30; i += 1) {
    state = wrenchArc3Breach(state, realCatalog);
  }
  assert.equal(state.arc3.fracture, ARC3_STRATA[0].fractureGoal);
  assert.equal(arc3CanDriveWedge(state.arc3), true);
});

test('Arc 3 forgiving clotting respects 1.4s grace window, ramps smoothly, floors at 0, and preserves wedges/hemorrhage', () => {
  let state = freshArc3();
  state = {
    ...state,
    arc3: {
      ...state.arc3,
      currentStratum: 2,
      lockedWedges: ['broken_grout'],
      fracture: 80.0,
      hemorrhage: 250.0,
      lifetimeHemorrhage: 250.0,
      momentumCharge: 0.8,
    },
  };

  // Within grace window: zero clotting decay
  state = tick(state, 1.0, realCatalog);
  assert.equal(state.arc3.fracture, 80.0);
  assert.equal(arc3IsActivelyClotting(state.arc3), false);

  // Past grace window: clotting engages and pulls fracture down while preserving wedges & hemorrhage
  state = tick(state, 1.0, realCatalog);
  assert.equal(arc3IsActivelyClotting(state.arc3), true);
  assert.ok(arc3ActiveClotPerSec(state.arc3) > 0);
  assert.ok(state.arc3.fracture < 80.0);
  assert.equal(state.arc3.hemorrhage, 250.0);
  assert.deepEqual(state.arc3.lockedWedges, ['broken_grout']);

  // Long idle floors at 0 without losing locked wedges or hemorrhage
  state = tick(state, 120.0, realCatalog);
  assert.equal(state.arc3.fracture, 0.0);
  assert.equal(state.arc3.momentumCharge, 0.0);
  assert.equal(state.arc3.hemorrhage, 250.0);
  assert.deepEqual(state.arc3.lockedWedges, ['broken_grout']);
});

test('Arc 3 all 18 Unmaking acts scale wrench force, freeze clotting on jar shatters, and trigger canon dialogues', () => {
  assert.equal(ARC3_UNMAKE_ACTS.length, 18);
  let state = freshArc3();
  state = {
    ...state,
    arc3: {
      ...state.arc3,
      currentStratum: 5,
      lockedWedges: ['broken_grout', 'unstitched_seam', 'splintered_ribs', 'shared_pulse'],
      hemorrhage: 5_000_000,
      lifetimeHemorrhage: 5_000_000,
    },
  };
  const initialYield = arc3WrenchFractureYield(state);

  state = performArc3Unmake(state, 'shatter_yesterdays_dust', realCatalog);
  assert.equal(arc3IsJarShattered(state.arc3, 'yesterdays_dust'), true);
  assert.equal(arc3IsClotFrozen(state.arc3), true);
  assert.ok(state.arc3.clotFreezeRemainingSeconds >= 3.0);
  assert.ok(state.triggeredDialogueIds.includes('dlg_arc3_first_jar_shatter'));

  // Duplicate unmake is a safe no-op
  const dup = performArc3Unmake(state, 'shatter_yesterdays_dust', realCatalog);
  assert.deepEqual(dup, state);

  for (const act of ARC3_UNMAKE_ACTS.slice(1)) {
    state = performArc3Unmake(state, act.id, realCatalog);
  }
  assert.equal(state.arc3.completedUnmakes.length, 18);
  assert.equal(arc3ShatteredJarCount(state.arc3), 6);
  assert.equal(arc3UnstitchedScarCount(state.arc3), 6);
  assert.ok(arc3WrenchFractureYield(state) > initialYield * 100);
  assert.ok(state.triggeredDialogueIds.includes('dlg_arc3_all_jars_shattered'));
  assert.ok(state.triggeredDialogueIds.includes('dlg_arc3_scar_unstitched'));
  assert.ok(state.triggeredDialogueIds.includes('dlg_arc3_first_unmake'));
});

test('Arc 3 breach ruptures spawn, grant distinct buffs, and trigger Phase 7 rupture fragments', () => {
  let state = devSetArc3Stratum(freshArc3(), 4, realCatalog);
  state = tick(state, ARC3_BALANCE.FIRST_RUPTURE_DELAY_SEC + 0.1, realCatalog);
  assert.equal(state.arc3.activeRuptures.length, 1);

  state = {
    ...state,
    arc3: {
      ...state.arc3,
      activeRuptures: [
        { id: 101, type: 'clotting_seam', xFraction: 0.2, yFraction: 0.25, remainingSeconds: 12 },
        { id: 102, type: 'splintered_rib', xFraction: 0.8, yFraction: 0.25, remainingSeconds: 12 },
        { id: 103, type: 'torn_suture', xFraction: 0.2, yFraction: 0.5, remainingSeconds: 12 },
        { id: 104, type: 'arterial_well', xFraction: 0.8, yFraction: 0.5, remainingSeconds: 12 },
      ],
    },
  };

  state = tearBreachRupture(state, 101, realCatalog);
  assert.ok(state.arc3.clotFreezeRemainingSeconds >= 3.5);
  state = tearBreachRupture(state, 102, realCatalog);
  assert.ok(state.arc3.ribForceRemainingSeconds >= 4.5);
  state = tearBreachRupture(state, 103, realCatalog);
  assert.ok(state.arc3.frenzyRemainingSeconds >= 6.0);
  state = tearBreachRupture(state, 104, realCatalog);
  assert.equal(arc3TotalRuptureInteractions(state.arc3), 4);
  assert.ok(state.triggeredDialogueIds.includes('dlg_arc3_first_rupture'));
});

test('Arc 3 sequential 5 Memory Wedges and 5 interactive Final Pulls complete into Abstract Crimson Flood', () => {
  let state = freshArc3();

  for (const stratum of ARC3_STRATA) {
    assert.equal(state.arc3.currentStratum, stratum.number);
    state = { ...state, arc3: { ...state.arc3, fracture: stratum.fractureGoal } };
    assert.equal(arc3CanDriveWedge(state.arc3), true);
    state = driveMemoryWedge(state, realCatalog);
    assert.ok(state.arc3.lockedWedges.includes(stratum.id));
  }

  assert.equal(arc3IsInFinale(state.arc3), true);
  assert.equal(state.arc3.submersionComplete, false);
  assert.equal(arc3FloodLevel(state.arc3), 0.86);
  assert.equal(arc3CanPerformFinalPull(state.arc3), true);
  assert.ok(state.triggeredDialogueIds.includes('dlg_arc3_wedge_5_file_zero'));

  for (let pull = 1; pull <= ARC3_BALANCE.FINAL_PULLS_TOTAL; pull += 1) {
    assert.equal(arc3CanPerformFinalPull(state.arc3), true);
    state = performArc3FinalPull(state, realCatalog);
    assert.equal(state.arc3.finalPullsCompleted, pull);
    if (pull < ARC3_BALANCE.FINAL_PULLS_TOTAL) {
      assert.equal(arc3CanPerformFinalPull(state.arc3), false);
      const blocked = performArc3FinalPull(state, realCatalog);
      assert.equal(blocked.arc3.finalPullsCompleted, pull);
      state = tick(state, ARC3_BALANCE.FINAL_PULL_COOLDOWN_SEC + 0.05, realCatalog);
      assert.equal(arc3CanPerformFinalPull(state.arc3), true);
    }
  }

  assert.equal(state.arc3.submersionComplete, true);
  assert.equal(arc3FloodLevel(state.arc3), 1.0);
  assert.equal(arc3TelemetryBlurFactor(state.arc3), 1.0);
  assert.ok(state.triggeredDialogueIds.includes('dlg_arc3_submersion_end'));
});

test('Arc 3 legacy reflection, schema v4 persistence, v3->v4 migration, Arc 1 Excision isolation, and Dev Console controls', () => {
  const devEmpty = freshArc3();
  assert.equal(arc3LegacyHemorrhageMultiplier(devEmpty), 1.0);
  assert.equal(arc3LegacyFractureMultiplier(devEmpty), 1.0);

  const fullLegacy = {
    ...devEmpty,
    ownedJars: [
      'yesterdays_dust',
      'unspent_breath',
      'almost_a_name',
      'salt_from_the_grout',
      'a_borrowed_pulse',
      'the_uncounted_hour',
    ],
    unlockedScarNodes: Array.from({ length: 16 }, (_, i) => `n_${i}`),
    arc2: {
      ...devEmpty.arc2,
      discoveredCorroborations: Array.from({ length: 10 }, (_, i) => `c_${i}`),
    },
  };
  assert.ok(Math.abs(arc3LegacyHemorrhageMultiplier(fullLegacy) - 1.3) < 0.0001);
  assert.ok(Math.abs(arc3LegacyFractureMultiplier(fullLegacy) - 1.3) < 0.0001);
  assert.ok(arc3WrenchHemorrhageYield(fullLegacy) > arc3WrenchHemorrhageYield(devEmpty));

  // Dev console mutations
  let s = devSetArc3Stratum(devEmpty, 4, realCatalog);
  assert.equal(s.arc3.currentStratum, 4);
  assert.equal(arc3IsValveSheared(s.arc3), true);
  s = devAssignArc3Resources(s, { hemorrhage: 50_000, fracture: 18_000 }, realCatalog);
  assert.equal(s.arc3.hemorrhage, 50_000);
  assert.equal(arc3CanDriveWedge(s.arc3), true);
  s = devSpawnBreachRupture(s);
  assert.equal(s.arc3.activeRuptures.length, 1);

  // Schema v4 serialization round-trip + v3->v4 migration
  const rawV4 = serializeState(s);
  const parsedV4 = JSON.parse(rawV4);
  assert.equal(parsedV4.version, 4);
  const restored = deserializeState(rawV4, realCatalog);
  assert.ok(restored);
  assert.equal(restored.arc3.currentStratum, 4);
  assert.equal(restored.arc3.hemorrhage, 50_000);

  delete parsedV4.arc3;
  parsedV4.version = 3;
  const migratedV3 = deserializeState(JSON.stringify(parsedV4), realCatalog);
  assert.ok(migratedV3);
  assert.equal(migratedV3.arc3.currentStratum, 1);
  assert.deepEqual(migratedV3.arc3.lockedWedges, []);

  // Arc 1 Excision preserves Arc 3 progress
  const readyExcision = {
    ...s,
    activeArc: GAME_ARCS.ARC_1_CHAMBER,
    ichor: ORGAN_BALANCE.EXCISION_ICHOR_THRESHOLD + 1000,
    cycleLifetimeIchor: ORGAN_BALANCE.EXCISION_ICHOR_THRESHOLD + 1000,
    phase: 4,
  };
  const postExcision = performExcision(readyExcision, realCatalog);
  assert.equal(postExcision.arc3.currentStratum, 4);
  assert.equal(postExcision.arc3.hemorrhage, 50_000);

  // Finale jump & reset
  const fin = devJumpArc3Finale(s, realCatalog);
  assert.equal(arc3IsInFinale(fin.arc3), true);
  const reset = devResetArc3(fin, realCatalog);
  assert.equal(reset.arc3.currentStratum, 1);
  assert.equal(reset.arc3.lockedWedges.length, 0);
});
