/**
 * Clinical Dev Console state helpers (direct jumps, resource grants, anomaly triggers, and wipe).
 */

import { GAME_ARCS, SCAR_NODES } from './constants.js?v=3.15';
import { CASE_EXHIBITS, initialArc2State } from './arc2Models.js?v=3.15';
import { ARC3_STRATA, initialArc3State } from './arc3Models.js?v=3.15';
import { finalizeProgression, initialState } from './progression.js?v=3.15';
import { spawnImmediateWallAnomaly } from './arc1Engine.js?v=3.15';
import { spawnImmediateDeskAnomaly } from './arc2Engine.js?v=3.15';
import { spawnImmediateBreachRupture } from './arc3Engine.js?v=3.15';

export function devSetActiveArc(state, arcId, catalog) {
  const adjusted = { ...state, activeArc: arcId };
  if (arcId === GAME_ARCS.ARC_2_INVESTIGATION) {
    adjusted.highestExcisionMilestone = Math.max(state.highestExcisionMilestone, 6);
  } else if (arcId === GAME_ARCS.ARC_3_CONVERGENCE) {
    adjusted.highestExcisionMilestone = Math.max(state.highestExcisionMilestone, 6);
    adjusted.arc2 = {
      ...state.arc2,
      caseAngle: 5,
      completedCasePivots: 5,
      fileZeroRevealed: true,
    };
  }
  return finalizeProgression(adjusted, catalog);
}

export function devSetHighestMilestone(state, milestone, catalog) {
  const clamped = Math.max(0, Math.min(6, Math.floor(milestone)));
  const adjusted = {
    ...state,
    highestExcisionMilestone: clamped,
    cycle: clamped > 0 ? Math.max(state.cycle, clamped) : state.cycle,
  };
  return finalizeProgression(adjusted, catalog);
}

export function devAssignArc1Resources(state, { ichor, stillness, availableScar }, catalog) {
  const nextIchor = ichor != null ? Math.max(0, Number(ichor) || 0) : state.ichor;
  const nextStillness = stillness != null ? Math.max(0, Number(stillness) || 0) : state.stillness;
  const nextAvailScar =
    availableScar != null
      ? Math.max(0, Math.floor(Number(availableScar) || 0))
      : state.availableScarTissue;
  const adjusted = {
    ...state,
    ichor: nextIchor,
    cycleLifetimeIchor: Math.max(state.cycleLifetimeIchor, nextIchor),
    stillness: nextStillness,
    cycleLifetimeStillness: Math.max(state.cycleLifetimeStillness, nextStillness),
    availableScarTissue: nextAvailScar,
    scarTissue: Math.max(state.scarTissue, nextAvailScar),
  };
  return finalizeProgression(adjusted, catalog);
}

export function devSetAllScarNodes(state, unlockAll, catalog) {
  const adjusted = {
    ...state,
    unlockedScarNodes: unlockAll ? SCAR_NODES.map((n) => n.id) : [],
    repeatableScarRanks: unlockAll ? { ...(state.repeatableScarRanks ?? {}) } : {},
    highestExcisionMilestone: unlockAll
      ? Math.max(state.highestExcisionMilestone, 1)
      : state.highestExcisionMilestone,
  };
  return finalizeProgression(adjusted, catalog);
}

export function devAssignArc2Resources(
  state,
  { leads, silence, caseAngle, completedChapters },
  catalog,
) {
  const desk = state.arc2;
  const nextLeads = leads != null ? Math.max(0, Number(leads) || 0) : desk.leads;
  const nextSilence = silence != null ? Math.max(0, Number(silence) || 0) : desk.silence;
  const resolvedAngle =
    caseAngle != null
      ? Math.max(1, Math.min(5, Math.floor(Number(caseAngle) || 1)))
      : completedChapters != null
        ? Math.max(1, Math.min(5, Math.floor(Number(completedChapters) || 0) + 1))
        : desk.caseAngle;
  const resolvedPivots =
    completedChapters != null
      ? Math.max(0, Math.min(5, Math.floor(Number(completedChapters) || 0)))
      : caseAngle != null
        ? Math.max(0, resolvedAngle - 1)
        : desk.completedCasePivots;
  const nextPinned = new Set(desk.pinnedExhibits ?? ['water_stained_petition']);
  for (const ex of CASE_EXHIBITS) {
    if (ex.isKeystone && ex.unlockCaseAngle <= resolvedAngle) {
      nextPinned.add(ex.id);
    }
  }
  const nextDesk = {
    ...desk,
    leads: nextLeads,
    lifetimeLeads: Math.max(desk.lifetimeLeads, nextLeads),
    silence: nextSilence,
    lifetimeSilence: Math.max(desk.lifetimeSilence, nextSilence),
    caseAngle: resolvedAngle,
    completedCasePivots: resolvedPivots,
    pinnedExhibits: Array.from(nextPinned),
    fileZeroRevealed: resolvedPivots >= 5 || desk.fileZeroRevealed,
  };
  return finalizeProgression({ ...state, arc2: nextDesk }, catalog);
}

export function devSetArc2AllExhibits(state, unlockAll, catalog) {
  const desk = state.arc2;
  const nextDesk = {
    ...desk,
    pinnedExhibits: unlockAll ? CASE_EXHIBITS.map((e) => e.id) : ['water_stained_petition'],
    wiredPairs: unlockAll ? desk.wiredPairs : [],
  };
  return finalizeProgression({ ...state, arc2: nextDesk }, catalog);
}

export function devToggleArc2Lamp(state, catalog) {
  const desk = state.arc2;
  const nextLampOn = !desk.lampOn;
  const nextDesk = {
    ...desk,
    lampOn: nextLampOn,
    lampToggleCount: (desk.lampToggleCount ?? 0) + 1,
    lampOffCount: !nextLampOn ? (desk.lampOffCount ?? 0) + 1 : desk.lampOffCount ?? 0,
  };
  return finalizeProgression({ ...state, arc2: nextDesk }, catalog);
}

export function devResetArc2(state, catalog) {
  return finalizeProgression({ ...state, arc2: initialArc2State() }, catalog);
}

export function devSpawnWallAnomaly(state) {
  return spawnImmediateWallAnomaly(state, true);
}

export function devSpawnDeskAnomaly(state) {
  return { ...state, arc2: spawnImmediateDeskAnomaly(state.arc2) };
}

export function devSetArc3Stratum(state, stratum, catalog) {
  const clamped = Math.max(1, Math.min(5, Math.floor(Number(stratum) || 1)));
  const locked = ARC3_STRATA.filter((s) => s.number < clamped).map((s) => s.id);
  const nextBreach = {
    ...(state.arc3 ?? initialArc3State()),
    currentStratum: clamped,
    lockedWedges: locked,
    fracture: 0.0,
    finalPullsCompleted: 0,
    finalPullCooldownSeconds: 0.0,
    submersionComplete: false,
    secondsSinceLastAction: 0.0,
  };
  return finalizeProgression({ ...state, arc3: nextBreach }, catalog);
}

export function devAssignArc3Resources(state, { hemorrhage, fracture }, catalog) {
  const breach = state.arc3 ?? initialArc3State();
  const nextHem = hemorrhage != null ? Math.max(0, Number(hemorrhage) || 0) : breach.hemorrhage;
  const activeGoal = (ARC3_STRATA.find((s) => s.number === breach.currentStratum) ?? ARC3_STRATA[4]).fractureGoal;
  const nextFrac =
    fracture != null ? Math.max(0, Math.min(activeGoal, Number(fracture) || 0)) : breach.fracture;
  const nextBreach = {
    ...breach,
    hemorrhage: nextHem,
    lifetimeHemorrhage: Math.max(breach.lifetimeHemorrhage ?? 0, nextHem),
    fracture: nextFrac,
    secondsSinceLastAction: 0.0,
  };
  return finalizeProgression({ ...state, arc3: nextBreach }, catalog);
}

export function devJumpArc3Finale(state, catalog) {
  const nextBreach = {
    ...(state.arc3 ?? initialArc3State()),
    currentStratum: 5,
    lockedWedges: ARC3_STRATA.map((s) => s.id),
    fracture: 0.0,
    activeRuptures: [],
    finalPullsCompleted: 0,
    finalPullCooldownSeconds: 0.0,
    submersionComplete: false,
    secondsSinceLastAction: 0.0,
  };
  return finalizeProgression(
    { ...state, activeArc: GAME_ARCS.ARC_3_CONVERGENCE, arc3: nextBreach },
    catalog,
  );
}

export function devSpawnBreachRupture(state) {
  return { ...state, arc3: spawnImmediateBreachRupture(state.arc3 ?? initialArc3State()) };
}

export function devResetArc3(state, catalog) {
  return finalizeProgression({ ...state, arc3: initialArc3State() }, catalog);
}

export function devResetAll(catalog) {
  return initialState(catalog);
}
