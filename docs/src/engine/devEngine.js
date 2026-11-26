/**
 * Clinical Dev Console state helpers (direct jumps, resource grants, anomaly triggers, and wipe).
 */

import { GAME_ARCS, SCAR_NODES } from './constants.js?v=3.12';
import { finalizeProgression, initialState } from './progression.js?v=3.12';
import { spawnImmediateWallAnomaly } from './arc1Engine.js?v=3.12';
import { spawnImmediateDeskAnomaly } from './arc2Engine.js?v=3.12';

export function devSetActiveArc(state, arcId, catalog) {
  const adjusted = { ...state, activeArc: arcId };
  if (arcId === GAME_ARCS.ARC_2_INVESTIGATION) {
    adjusted.highestExcisionMilestone = Math.max(state.highestExcisionMilestone, 6);
  } else if (arcId === GAME_ARCS.ARC_3_CONVERGENCE) {
    adjusted.highestExcisionMilestone = Math.max(state.highestExcisionMilestone, 6);
    adjusted.arc2 = {
      ...state.arc2,
      completedChapters: 4,
      fileZeroRevealed: true,
      complicity: 1.0,
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
    availableScar != null ? Math.max(0, Math.floor(Number(availableScar) || 0)) : state.availableScarTissue;
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
  { leads, silence, complicity, completedChapters },
  catalog,
) {
  const desk = state.arc2;
  const nextLeads = leads != null ? Math.max(0, Number(leads) || 0) : desk.leads;
  const nextSilence = silence != null ? Math.max(0, Number(silence) || 0) : desk.silence;
  const nextChapters =
    completedChapters != null
      ? Math.max(0, Math.min(4, Math.floor(Number(completedChapters) || 0)))
      : desk.completedChapters;
  const nextComplicity =
    complicity != null ? Math.max(0.05, Math.min(1.0, Number(complicity) || 0.05)) : desk.complicity;
  const nextDesk = {
    ...desk,
    leads: nextLeads,
    lifetimeLeads: Math.max(desk.lifetimeLeads, nextLeads),
    silence: nextSilence,
    lifetimeSilence: Math.max(desk.lifetimeSilence, nextSilence),
    complicity: nextComplicity,
    completedChapters: nextChapters,
    fileZeroRevealed: nextChapters >= 4 || desk.fileZeroRevealed,
  };
  return finalizeProgression({ ...state, arc2: nextDesk }, catalog);
}

export function devSpawnWallAnomaly(state) {
  return spawnImmediateWallAnomaly(state, true);
}

export function devSpawnDeskAnomaly(state) {
  return { ...state, arc2: spawnImmediateDeskAnomaly(state.arc2) };
}

export function devResetAll(catalog) {
  return initialState(catalog);
}
