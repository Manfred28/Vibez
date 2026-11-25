/**
 * Arc 2 Investigator Desk engine actions, desk anomaly spawning, and Arc 3 Convergence transition.
 */

import { GAME_ARCS } from './constants.js?v=3.11';
import {
  ARC2_BALANCE,
  DESK_ANCHORS,
  DESK_ANOMALY_LIST,
  DESK_ANOMALY_TYPES,
  arc2CanConnectNextChapter,
  arc2ChapterMultiplier,
  arc2ContactCost,
  arc2ContactCount,
  arc2InspectYield,
  arc2LeadsPerSecond,
  arc2NextChapter,
  arc2SilencePerSecond,
  contactById,
  deductionById,
} from './arc2Models.js?v=3.11';
import { finalizeProgression } from './progression.js?v=3.11';

export function spawnImmediateDeskAnomaly(desk) {
  if (desk.activeDeskAnomalies.length >= ARC2_BALANCE.MAX_DESK_ANOMALIES) return desk;
  const eligible = DESK_ANOMALY_LIST.filter((t) => t.minChapter <= desk.completedChapters);
  const activeTypes = new Set(desk.activeDeskAnomalies.map((a) => a.type));
  const preferred = eligible.filter((t) => !activeTypes.has(t.id));
  const pool = preferred.length > 0 ? preferred : eligible;
  const id = desk.nextAnomalyId;
  const chosen = pool[(id - 1) % pool.length];
  const used = new Set(desk.activeDeskAnomalies.map((a) => `${a.xFraction},${a.yFraction}`));
  const free = DESK_ANCHORS.filter(([x, y]) => !used.has(`${x},${y}`));
  const anchors = free.length > 0 ? free : DESK_ANCHORS;
  const [xFraction, yFraction] = anchors[(id - 1) % anchors.length];

  return {
    ...desk,
    activeDeskAnomalies: [
      ...desk.activeDeskAnomalies,
      { id, type: chosen.id, xFraction, yFraction },
    ],
    nextAnomalyId: id + 1,
    secondsUntilNextAnomaly: ARC2_BALANCE.DESK_ANOMALY_INTERVAL_SEC,
  };
}

function advanceDeskAnomalySpawner(desk, deltaSeconds) {
  if (desk.activeDeskAnomalies.length >= ARC2_BALANCE.MAX_DESK_ANOMALIES) return desk;
  const remaining = desk.secondsUntilNextAnomaly - deltaSeconds;
  if (remaining > 0.0) return { ...desk, secondsUntilNextAnomaly: remaining };
  return spawnImmediateDeskAnomaly(desk);
}

export function tickArc2(state, deltaSeconds, catalog) {
  const desk = state.arc2;
  const nextIdle = desk.secondsSinceLastAction + deltaSeconds;
  const silenceNow = nextIdle >= ARC2_BALANCE.SILENCE_IDLE_DELAY_SEC;

  const activeDesk = { ...desk, isSilenceActive: silenceNow };
  const gainedLeads = arc2LeadsPerSecond(activeDesk) * deltaSeconds;
  const gainedSilence = arc2SilencePerSecond(activeDesk) * deltaSeconds;
  const nextFocus = Math.max(0.0, desk.focusRemainingSeconds - deltaSeconds);

  const chapterFloor = Math.min(0.92, desk.completedChapters * 0.22);
  const silenceCreep = silenceNow ? deltaSeconds * 0.004 : 0.0;
  const nextComplicity = Math.max(0.05, Math.min(1.0, Math.max(desk.complicity + silenceCreep, chapterFloor)));

  let updatedDesk = {
    ...activeDesk,
    leads: desk.leads + gainedLeads,
    lifetimeLeads: desk.lifetimeLeads + gainedLeads,
    silence: desk.silence + gainedSilence,
    lifetimeSilence: desk.lifetimeSilence + gainedSilence,
    complicity: nextComplicity,
    secondsSinceLastAction: nextIdle,
    focusRemainingSeconds: nextFocus,
  };
  updatedDesk = advanceDeskAnomalySpawner(updatedDesk, deltaSeconds);
  return finalizeProgression({ ...state, arc2: updatedDesk }, catalog);
}

export function inspectCaseFile(state, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_2_INVESTIGATION) return state;
  const desk = state.arc2;
  const gained = arc2InspectYield(desk);
  const nextDesk = {
    ...desk,
    leads: desk.leads + gained,
    lifetimeLeads: desk.lifetimeLeads + gained,
    secondsSinceLastAction: 0.0,
    isSilenceActive: false,
  };
  return finalizeProgression({ ...state, arc2: nextDesk }, catalog);
}

export function recruitContact(state, contactId, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_2_INVESTIGATION) return state;
  const spec = typeof contactId === 'string' ? contactById(contactId) : contactId;
  if (!spec) return state;
  const desk = state.arc2;
  const cost = arc2ContactCost(desk, spec);
  if (desk.leads < cost) return state;

  const nextDesk = {
    ...desk,
    leads: desk.leads - cost,
    contacts: {
      ...desk.contacts,
      [spec.id]: arc2ContactCount(desk, spec.id) + 1,
    },
  };
  return finalizeProgression({ ...state, arc2: nextDesk }, catalog);
}

export function pinDeduction(state, deductionId, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_2_INVESTIGATION) return state;
  const spec = typeof deductionId === 'string' ? deductionById(deductionId) : deductionId;
  if (!spec) return state;
  const desk = state.arc2;
  if (desk.ownedDeductions.includes(spec.id) || desk.silence < spec.silenceCost) return state;

  const nextDesk = {
    ...desk,
    silence: desk.silence - spec.silenceCost,
    ownedDeductions: [...desk.ownedDeductions, spec.id],
    complicity: Math.min(1.0, desk.complicity + 0.08),
  };
  return finalizeProgression({ ...state, arc2: nextDesk }, catalog);
}

export function interactDeskAnomaly(state, anomalyId, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_2_INVESTIGATION) return state;
  const desk = state.arc2;
  const anomaly = desk.activeDeskAnomalies.find((a) => a.id === anomalyId);
  if (!anomaly) return state;
  const remaining = desk.activeDeskAnomalies.filter((a) => a.id !== anomalyId);
  const mortarBoost = desk.ownedDeductions.includes('mortar_sample_match')
    ? ARC2_BALANCE.MORTAR_SILENCE_AND_ANOMALY_MULTIPLIER
    : 1.0;
  const chMult = arc2ChapterMultiplier(desk);
  const updatedCounts = {
    ...desk.deskAnomalyInteractions,
    [anomaly.type]: (desk.deskAnomalyInteractions?.[anomaly.type] ?? 0) + 1,
  };

  let nextDesk = desk;
  if (anomaly.type === DESK_ANOMALY_TYPES.EVIDENCE_ENVELOPE.id) {
    const bonus = ARC2_BALANCE.ENVELOPE_BASE_LEADS * chMult * mortarBoost;
    nextDesk = {
      ...desk,
      leads: desk.leads + bonus,
      lifetimeLeads: desk.lifetimeLeads + bonus,
      complicity: Math.min(1.0, desk.complicity + 0.06),
      activeDeskAnomalies: remaining,
      deskAnomalyInteractions: updatedCounts,
    };
  } else if (anomaly.type === DESK_ANOMALY_TYPES.FOGGED_SPECTACLES.id) {
    nextDesk = {
      ...desk,
      focusRemainingSeconds: ARC2_BALANCE.FOCUS_DURATION_SEC,
      complicity: Math.min(1.0, desk.complicity + 0.05),
      activeDeskAnomalies: remaining,
      deskAnomalyInteractions: updatedCounts,
    };
  } else if (anomaly.type === DESK_ANOMALY_TYPES.RED_STRING.id) {
    const leadBonus = ARC2_BALANCE.RED_STRING_BASE_LEADS * chMult * mortarBoost;
    const silenceBonus = ARC2_BALANCE.RED_STRING_BASE_SILENCE * mortarBoost;
    nextDesk = {
      ...desk,
      leads: desk.leads + leadBonus,
      lifetimeLeads: desk.lifetimeLeads + leadBonus,
      silence: desk.silence + silenceBonus,
      lifetimeSilence: desk.lifetimeSilence + silenceBonus,
      complicity: Math.min(1.0, desk.complicity + 0.08),
      activeDeskAnomalies: remaining,
      deskAnomalyInteractions: updatedCounts,
    };
  } else if (anomaly.type === DESK_ANOMALY_TYPES.REEL_TAPE.id) {
    const leadBonus = ARC2_BALANCE.REEL_TAPE_BASE_LEADS * chMult * mortarBoost;
    const silenceBonus = ARC2_BALANCE.REEL_TAPE_BASE_SILENCE * mortarBoost;
    nextDesk = {
      ...desk,
      leads: desk.leads + leadBonus,
      lifetimeLeads: desk.lifetimeLeads + leadBonus,
      silence: desk.silence + silenceBonus,
      lifetimeSilence: desk.lifetimeSilence + silenceBonus,
      focusRemainingSeconds: Math.max(desk.focusRemainingSeconds, 8.0),
      complicity: Math.min(1.0, desk.complicity + 0.1),
      activeDeskAnomalies: remaining,
      deskAnomalyInteractions: updatedCounts,
    };
  }

  return finalizeProgression({ ...state, arc2: nextDesk }, catalog);
}

export function connectNextChapter(state, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_2_INVESTIGATION) return state;
  const desk = state.arc2;
  const chapter = arc2NextChapter(desk);
  if (!chapter || !arc2CanConnectNextChapter(desk)) return state;
  const isFileZero = chapter.number === 4;
  const nextDesk = {
    ...desk,
    leads: desk.leads - chapter.leadsCost,
    silence: desk.silence - chapter.silenceCost,
    completedChapters: chapter.number,
    complicity: isFileZero ? 1.0 : Math.min(0.95, desk.complicity + 0.22),
    fileZeroRevealed: desk.fileZeroRevealed || isFileZero,
  };
  return finalizeProgression({ ...state, arc2: nextDesk }, catalog);
}

export function enterArc3Convergence(state, catalog) {
  if (!state.arc2.fileZeroRevealed) return state;
  return finalizeProgression({ ...state, activeArc: GAME_ARCS.ARC_3_CONVERGENCE }, catalog);
}
