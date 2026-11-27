/**
 * Pure deterministic Arc 3 ("The Unmaking") simulation engine for Web
 * (1:1 parity with Android Arc3BreachEngine.kt).
 */

import { GAME_ARCS } from './constants.js?v=3.16';
import {
  ARC3_BALANCE,
  ARC3_STRATA,
  BREACH_RUPTURE_LIST,
  arc3ActiveClotPerSec,
  arc3ActiveStratum,
  arc3BleedPressurePerSec,
  arc3CanDriveWedge,
  arc3CanPerformFinalPull,
  arc3CanPerformUnmake,
  arc3FloodLevel,
  arc3IsInFinale,
  arc3RuptureTypeById,
  arc3UnmakeById,
  arc3UnmakeRuptureMultiplier,
  arc3WrenchFractureYield,
  arc3WrenchHemorrhageYield,
  initialArc3State,
} from './arc3Models.js?v=3.16';
import { finalizeProgression } from './progression.js?v=3.16';

const RUPTURE_ANCHORS = Object.freeze([
  [0.16, 0.22],
  [0.84, 0.24],
  [0.2, 0.46],
  [0.81, 0.44],
  [0.17, 0.64],
  [0.83, 0.62],
]);

export function wrenchArc3Breach(state, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_3_CONVERGENCE) return state;
  const breach = state.arc3 ?? initialArc3State();
  if (arc3IsInFinale(breach) || breach.submersionComplete) return state;

  const gainedFracture = arc3WrenchFractureYield(state);
  const gainedHemorrhage = arc3WrenchHemorrhageYield(state);
  const nextMomentum = Math.min(1.0, (breach.momentumCharge ?? 0) + ARC3_BALANCE.MOMENTUM_GAIN_PER_TAP);
  const cappedFracture = Math.min(
    arc3ActiveStratum(breach).fractureGoal,
    (breach.fracture ?? 0) + gainedFracture,
  );

  const updated = {
    ...breach,
    fracture: cappedFracture,
    hemorrhage: (breach.hemorrhage ?? 0) + gainedHemorrhage,
    lifetimeHemorrhage: (breach.lifetimeHemorrhage ?? 0) + gainedHemorrhage,
    wrenchCount: (breach.wrenchCount ?? 0) + 1,
    momentumCharge: nextMomentum,
    secondsSinceLastAction: 0.0,
  };
  return finalizeProgression({ ...state, arc3: updated }, catalog);
}

export function performArc3Unmake(state, actId, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_3_CONVERGENCE) return state;
  const breach = state.arc3 ?? initialArc3State();
  const act = typeof actId === 'string' ? arc3UnmakeById(actId) : actId;
  if (!act || !arc3CanPerformUnmake(breach, act)) return state;

  const nextHemorrhage = Math.max(0.0, (breach.hemorrhage ?? 0) - act.hemorrhageCost);
  const nextFracture = Math.min(
    arc3ActiveStratum(breach).fractureGoal,
    (breach.fracture ?? 0) + act.instantFractureSurge,
  );
  const jarFreeze =
    act.category === 'shatter_jar'
      ? Math.max(breach.clotFreezeRemainingSeconds ?? 0, 3.0)
      : breach.clotFreezeRemainingSeconds ?? 0;

  const updated = {
    ...breach,
    hemorrhage: nextHemorrhage,
    fracture: nextFracture,
    completedUnmakes: [...(breach.completedUnmakes ?? []), act.id],
    clotFreezeRemainingSeconds: jarFreeze,
    secondsSinceLastAction: 0.0,
  };
  return finalizeProgression({ ...state, arc3: updated }, catalog);
}

export function tearBreachRupture(state, ruptureId, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_3_CONVERGENCE) return state;
  const breach = state.arc3 ?? initialArc3State();
  if (arc3IsInFinale(breach) || breach.submersionComplete) return state;

  const target = (breach.activeRuptures ?? []).find((r) => r.id === ruptureId);
  if (!target) return state;
  const typeMeta = arc3RuptureTypeById(target.type);
  if (!typeMeta) return state;

  const remaining = (breach.activeRuptures ?? []).filter((r) => r.id !== ruptureId);
  const ruptureMult = arc3UnmakeRuptureMultiplier(breach);
  const fracGain = arc3WrenchFractureYield(state) * ruptureMult * typeMeta.fractureWrenchMult;
  const hemGain = arc3WrenchHemorrhageYield(state) * ruptureMult * typeMeta.hemorrhageWrenchMult;

  const nextCounts = { ...(breach.ruptureInteractions ?? {}) };
  nextCounts[typeMeta.id] = (nextCounts[typeMeta.id] ?? 0) + 1;

  const updated = {
    ...breach,
    fracture: Math.min(arc3ActiveStratum(breach).fractureGoal, (breach.fracture ?? 0) + fracGain),
    hemorrhage: (breach.hemorrhage ?? 0) + hemGain,
    lifetimeHemorrhage: (breach.lifetimeHemorrhage ?? 0) + hemGain,
    activeRuptures: remaining,
    ruptureInteractions: nextCounts,
    clotFreezeRemainingSeconds: Math.max(
      breach.clotFreezeRemainingSeconds ?? 0,
      typeMeta.clotFreezeSec,
    ),
    ribForceRemainingSeconds: Math.max(breach.ribForceRemainingSeconds ?? 0, typeMeta.ribForceSec),
    frenzyRemainingSeconds: Math.max(breach.frenzyRemainingSeconds ?? 0, typeMeta.frenzySec),
    secondsSinceLastAction: 0.0,
  };
  return finalizeProgression({ ...state, arc3: updated }, catalog);
}

export function driveMemoryWedge(state, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_3_CONVERGENCE) return state;
  const breach = state.arc3 ?? initialArc3State();
  if (!arc3CanDriveWedge(breach)) return state;

  const locked = arc3ActiveStratum(breach);
  const nextLocked = Array.from(new Set([...(breach.lockedWedges ?? []), locked.id]));
  const enteringFinale = nextLocked.length >= ARC3_STRATA.length;
  const nextStratum = enteringFinale ? 5 : locked.number + 1;

  const updated = {
    ...breach,
    currentStratum: nextStratum,
    lockedWedges: nextLocked,
    fracture: 0.0,
    secondsSinceLastAction: 0.0,
    clotFreezeRemainingSeconds: enteringFinale ? 0.0 : 4.0,
    activeRuptures: enteringFinale ? [] : breach.activeRuptures ?? [],
    finalPullCooldownSeconds: enteringFinale ? 0.0 : breach.finalPullCooldownSeconds ?? 0.0,
  };
  return finalizeProgression({ ...state, arc3: updated }, catalog);
}

export function performArc3FinalPull(state, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_3_CONVERGENCE) return state;
  const breach = state.arc3 ?? initialArc3State();
  if (!arc3CanPerformFinalPull(breach)) return state;

  const nextPulls = (breach.finalPullsCompleted ?? 0) + 1;
  const finished = nextPulls >= ARC3_BALANCE.FINAL_PULLS_TOTAL;

  const updated = {
    ...breach,
    finalPullsCompleted: nextPulls,
    finalPullCooldownSeconds: finished ? 0.0 : ARC3_BALANCE.FINAL_PULL_COOLDOWN_SEC,
    submersionComplete: finished,
  };
  return finalizeProgression({ ...state, arc3: updated }, catalog);
}

export function spawnImmediateBreachRupture(breach) {
  if (!breach || arc3IsInFinale(breach) || breach.submersionComplete) return breach;
  const existing = breach.activeRuptures ?? [];
  if (existing.length >= ARC3_BALANCE.MAX_ACTIVE_RUPTURES + 1) return breach;

  const eligible = BREACH_RUPTURE_LIST.filter((t) => t.minStratum <= (breach.currentStratum ?? 1));
  if (eligible.length === 0) return breach;

  const activeTypes = new Set(existing.map((r) => r.type));
  const nonDup = eligible.filter((t) => !activeTypes.has(t.id));
  const pool = nonDup.length > 0 ? nonDup : eligible;
  const id = breach.nextRuptureId ?? 1;
  const chosen = pool[(id - 1) % pool.length];

  const maxY = Math.max(0.26, Math.min(0.66, 0.82 - arc3FloodLevel(breach) * 0.45));
  const usedKeys = new Set(existing.map((r) => `${r.xFraction.toFixed(2)}:${r.yFraction.toFixed(2)}`));
  const free = RUPTURE_ANCHORS.filter(([x, y]) => !usedKeys.has(`${x.toFixed(2)}:${y.toFixed(2)}`));
  const anchorPool = free.length > 0 ? free : RUPTURE_ANCHORS;
  const [rawX, rawY] = anchorPool[(id - 1) % anchorPool.length];
  const clampedY = Math.min(rawY, maxY);

  return {
    ...breach,
    activeRuptures: [
      ...existing,
      {
        id,
        type: chosen.id,
        xFraction: rawX,
        yFraction: clampedY,
        remainingSeconds: ARC3_BALANCE.RUPTURE_LIFETIME_SEC,
      },
    ],
    nextRuptureId: id + 1,
    secondsUntilNextRupture: ARC3_BALANCE.RUPTURE_SPAWN_INTERVAL_SEC,
  };
}

export function tickArc3(state, deltaSeconds, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_3_CONVERGENCE || deltaSeconds <= 0.0) return state;
  const breach = state.arc3 ?? initialArc3State();
  if (breach.submersionComplete) return state;

  if (arc3IsInFinale(breach)) {
    if ((breach.finalPullCooldownSeconds ?? 0) <= 0.0) return state;
    const nextCd = Math.max(0.0, breach.finalPullCooldownSeconds - deltaSeconds);
    return { ...state, arc3: { ...breach, finalPullCooldownSeconds: nextCd } };
  }

  const nextIdle = (breach.secondsSinceLastAction ?? 0) + deltaSeconds;
  const nextClotFreeze = Math.max(0.0, (breach.clotFreezeRemainingSeconds ?? 0) - deltaSeconds);
  const nextRibForce = Math.max(0.0, (breach.ribForceRemainingSeconds ?? 0) - deltaSeconds);
  const nextFrenzy = Math.max(0.0, (breach.frenzyRemainingSeconds ?? 0) - deltaSeconds);
  const nextMomentum =
    nextIdle >= ARC3_BALANCE.MOMENTUM_DECAY_DELAY_SEC
      ? Math.max(0.0, (breach.momentumCharge ?? 0) - ARC3_BALANCE.MOMENTUM_DECAY_PER_SEC * deltaSeconds)
      : breach.momentumCharge ?? 0;

  const preClot = {
    ...breach,
    secondsSinceLastAction: nextIdle,
    clotFreezeRemainingSeconds: nextClotFreeze,
    ribForceRemainingSeconds: nextRibForce,
    frenzyRemainingSeconds: nextFrenzy,
    momentumCharge: nextMomentum,
  };

  const clotPull = arc3ActiveClotPerSec(preClot) * deltaSeconds;
  const nextFracture = Math.max(0.0, (breach.fracture ?? 0) - clotPull);
  const bleedHemorrhage = arc3BleedPressurePerSec(breach) * 0.2 * deltaSeconds;

  const survivingRuptures = (breach.activeRuptures ?? [])
    .map((r) => ({ ...r, remainingSeconds: r.remainingSeconds - deltaSeconds }))
    .filter((r) => r.remainingSeconds > 0.0);

  let nextBreach = {
    ...preClot,
    fracture: nextFracture,
    hemorrhage: (breach.hemorrhage ?? 0) + bleedHemorrhage,
    lifetimeHemorrhage: (breach.lifetimeHemorrhage ?? 0) + bleedHemorrhage,
    activeRuptures: survivingRuptures,
  };

  if (nextBreach.activeRuptures.length < ARC3_BALANCE.MAX_ACTIVE_RUPTURES) {
    const remSpawn = (nextBreach.secondsUntilNextRupture ?? ARC3_BALANCE.RUPTURE_SPAWN_INTERVAL_SEC) - deltaSeconds;
    nextBreach =
      remSpawn <= 0.0
        ? spawnImmediateBreachRupture(nextBreach)
        : { ...nextBreach, secondsUntilNextRupture: remSpawn };
  }

  return finalizeProgression({ ...state, arc3: nextBreach }, catalog);
}
