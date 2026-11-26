/**
 * Arc 2 Investigator Desk pure simulation engine (1:1 parity with Android Arc2InvestigatorEngine.kt).
 * Implements Lamp On/Off dual-lens investigation, Red-String Corkboard wiring, 5 escalating Case
 * Angles with soft-reset pivots, Bulk Warrants, Acoustic Reel Scrubbing, 4 Case IV Redactions,
 * and strictly Slow-Boil-gated Desk Anomalies.
 */

import { GAME_ARCS } from './constants.js?v=3.13';
import {
  ARC2_BALANCE,
  DESK_ANOMALY_TYPES,
  arc2AnomalyPotencyMultiplier,
  arc2CanDissolveRedaction,
  arc2CanPivotNextCase,
  arc2CanTriggerReelScrub,
  arc2CanUnlockExhibit,
  arc2ContactCost,
  arc2ContactCount,
  arc2CurrentCase,
  arc2InspectYield,
  arc2IsBulkWarrantUnlocked,
  arc2IsContactUnlocked,
  arc2IsReelScrubbing,
  arc2LeadsPerSecond,
  arc2MaxAffordableContacts,
  arc2MaxStringSpools,
  arc2SilencePerSecond,
  canonicalPairKey,
  contactById,
  exhibitById,
  matchCorroborationPair,
  redactionById,
} from './arc2Models.js?v=3.13';
import { finalizeProgression } from './progression.js?v=3.13';

function pickDeskAnomaly(desk, id, rng = Math.random) {
  const pool = [];
  const pushTimes = (typeId, count) => {
    for (let i = 0; i < count; i += 1) pool.push(typeId);
  };
  const angle = desk.caseAngle ?? 1;
  pushTimes(DESK_ANOMALY_TYPES.EVIDENCE_ENVELOPE.id, desk.lampOn ? 4 : 1);
  if (angle >= DESK_ANOMALY_TYPES.FOGGED_SPECTACLES.unlockCaseAngle) {
    pushTimes(DESK_ANOMALY_TYPES.FOGGED_SPECTACLES.id, desk.lampOn ? 1 : 3);
  }
  if (angle >= DESK_ANOMALY_TYPES.REEL_TAPE.unlockCaseAngle) {
    pushTimes(DESK_ANOMALY_TYPES.REEL_TAPE.id, desk.lampOn ? 1 : 3);
  }
  if (angle >= DESK_ANOMALY_TYPES.ENAMEL_EXHIBIT.unlockCaseAngle) {
    pushTimes(DESK_ANOMALY_TYPES.ENAMEL_EXHIBIT.id, angle >= 5 ? 3 : 2);
  }
  const type = pool[Math.floor(rng() * pool.length) % pool.length];
  const onLeft = rng() < 0.5;
  const xFraction = onLeft ? 0.05 + rng() * 0.17 : 0.74 + rng() * 0.17;
  const yFraction = 0.24 + rng() * 0.56;
  return {
    id,
    type,
    xFraction,
    yFraction,
    remainingSeconds: ARC2_BALANCE.DESK_ANOMALY_LIFETIME_SEC,
  };
}

export function spawnImmediateDeskAnomaly(desk, rng = Math.random) {
  const anomaly = pickDeskAnomaly(desk, desk.nextAnomalyId, rng);
  const nextList = [...(desk.activeDeskAnomalies ?? []), anomaly].slice(
    -ARC2_BALANCE.MAX_ACTIVE_DESK_ANOMALIES,
  );
  return {
    ...desk,
    activeDeskAnomalies: nextList,
    nextAnomalyId: desk.nextAnomalyId + 1,
    secondsUntilNextAnomaly: desk.lampOn
      ? ARC2_BALANCE.LAMP_ON_ANOMALY_INTERVAL_SEC
      : ARC2_BALANCE.LAMP_OFF_ANOMALY_INTERVAL_SEC,
  };
}

export function toggleLamp(state, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_2_INVESTIGATION) return state;
  const desk = state.arc2;
  const nextLampOn = !desk.lampOn;
  const updatedDesk = {
    ...desk,
    lampOn: nextLampOn,
    lampToggleCount: (desk.lampToggleCount ?? 0) + 1,
    lampOffCount: !nextLampOn ? (desk.lampOffCount ?? 0) + 1 : desk.lampOffCount ?? 0,
    reelScrubRemainingSeconds: nextLampOn ? 0.0 : desk.reelScrubRemainingSeconds ?? 0.0,
  };
  return finalizeProgression({ ...state, arc2: updatedDesk }, catalog);
}

export function inspectCaseFile(state, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_2_INVESTIGATION) return state;
  const desk = state.arc2;
  const gained = arc2InspectYield(desk);
  const updatedDesk = {
    ...desk,
    leads: desk.leads + gained,
    lifetimeLeads: desk.lifetimeLeads + gained,
  };
  return finalizeProgression({ ...state, arc2: updatedDesk }, catalog);
}

export function recruitContact(state, contactId, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_2_INVESTIGATION) return state;
  const spec = typeof contactId === 'string' ? contactById(contactId) : contactId;
  if (!spec) return state;
  const desk = state.arc2;
  if (!arc2IsContactUnlocked(desk, spec)) return state;
  const cost = arc2ContactCost(desk, spec);
  if (desk.leads < cost) return state;

  const updatedDesk = {
    ...desk,
    leads: Math.max(0.0, desk.leads - cost),
    contacts: {
      ...desk.contacts,
      [spec.id]: arc2ContactCount(desk, spec.id) + 1,
    },
  };
  return finalizeProgression({ ...state, arc2: updatedDesk }, catalog);
}

export function recruitMaxContact(state, contactId, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_2_INVESTIGATION) return state;
  const spec = typeof contactId === 'string' ? contactById(contactId) : contactId;
  if (!spec) return state;
  const desk = state.arc2;
  if (!arc2IsBulkWarrantUnlocked(desk) || !arc2IsContactUnlocked(desk, spec)) return state;
  const { count, totalCost } = arc2MaxAffordableContacts(desk, spec);
  if (count <= 0) return state;

  const updatedDesk = {
    ...desk,
    leads: Math.max(0.0, desk.leads - totalCost),
    contacts: {
      ...desk.contacts,
      [spec.id]: arc2ContactCount(desk, spec.id) + count,
    },
  };
  return finalizeProgression({ ...state, arc2: updatedDesk }, catalog);
}

export function unlockExhibit(state, exhibitId, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_2_INVESTIGATION) return state;
  const ex = typeof exhibitId === 'string' ? exhibitById(exhibitId) : exhibitId;
  if (!ex) return state;
  const desk = state.arc2;
  if (!arc2CanUnlockExhibit(desk, ex)) return state;
  const updatedDesk = {
    ...desk,
    silence: Math.max(0.0, desk.silence - ex.silenceCost),
    pinnedExhibits: [...(desk.pinnedExhibits ?? []), ex.id],
  };
  return finalizeProgression({ ...state, arc2: updatedDesk }, catalog);
}

export function toggleExhibitPair(state, aId, bId, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_2_INVESTIGATION) return state;
  const pairKey = canonicalPairKey(aId, bId);
  if (!pairKey) return state;
  const desk = state.arc2;
  const pinned = desk.pinnedExhibits ?? [];
  if (!pinned.includes(aId) || !pinned.includes(bId)) return state;

  const currentPairs = desk.wiredPairs ?? [];
  let updatedDesk;
  if (currentPairs.includes(pairKey)) {
    updatedDesk = {
      ...desk,
      wiredPairs: currentPairs.filter((k) => k !== pairKey),
    };
  } else {
    if (currentPairs.length >= arc2MaxStringSpools(desk)) return state;
    const matched = matchCorroborationPair(pairKey);
    const prevDiscovered = desk.discoveredCorroborations ?? [];
    const nextDiscovered =
      matched && !prevDiscovered.includes(matched.id)
        ? [...prevDiscovered, matched.id]
        : prevDiscovered;
    updatedDesk = {
      ...desk,
      wiredPairs: [...currentPairs, pairKey],
      discoveredCorroborations: nextDiscovered,
    };
  }
  return finalizeProgression({ ...state, arc2: updatedDesk }, catalog);
}

export function triggerReelScrub(state, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_2_INVESTIGATION) return state;
  const desk = state.arc2;
  if (!arc2CanTriggerReelScrub(desk)) return state;
  const updatedDesk = {
    ...desk,
    reelScrubRemainingSeconds: ARC2_BALANCE.REEL_SCRUB_DURATION_SEC,
    reelScrubCooldownSeconds: 0.0,
    reelScrubCount: (desk.reelScrubCount ?? 0) + 1,
  };
  return finalizeProgression({ ...state, arc2: updatedDesk }, catalog);
}

export function dissolveRedaction(state, redactionId, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_2_INVESTIGATION) return state;
  const r = typeof redactionId === 'string' ? redactionById(redactionId) : redactionId;
  if (!r) return state;
  const desk = state.arc2;
  if (!arc2CanDissolveRedaction(desk, r)) return state;
  const updatedDesk = {
    ...desk,
    silence: Math.max(0.0, desk.silence - r.silenceCost),
    dissolvedRedactions: [...(desk.dissolvedRedactions ?? []), r.id],
  };
  return finalizeProgression({ ...state, arc2: updatedDesk }, catalog);
}

export function interactDeskAnomaly(state, anomalyId, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_2_INVESTIGATION) return state;
  const desk = state.arc2;
  const anomaly = (desk.activeDeskAnomalies ?? []).find((a) => a.id === anomalyId);
  if (!anomaly) return state;
  const remaining = desk.activeDeskAnomalies.filter((a) => a.id !== anomalyId);
  const updatedCounts = {
    ...desk.deskAnomalyInteractions,
    [anomaly.type]: (desk.deskAnomalyInteractions?.[anomaly.type] ?? 0) + 1,
  };

  const potency = arc2AnomalyPotencyMultiplier(desk);
  const leadsAnchor = Math.max(
    12.0 * (1 + (desk.completedCasePivots ?? 0)),
    arc2LeadsPerSecond(desk) * 14.0 + arc2InspectYield(desk) * 10.0,
  );
  const silenceAnchor = Math.max(
    5.0 * (1 + (desk.completedCasePivots ?? 0)),
    arc2SilencePerSecond(desk) * 9.0 + 4.0,
  );

  let leadsGain = 0.0;
  let silenceGain = 0.0;
  let nextFocus = desk.focusRemainingSeconds ?? 0.0;

  if (anomaly.type === DESK_ANOMALY_TYPES.EVIDENCE_ENVELOPE.id) {
    leadsGain = leadsAnchor * 1.15 * potency;
  } else if (anomaly.type === DESK_ANOMALY_TYPES.FOGGED_SPECTACLES.id) {
    nextFocus = Math.max(nextFocus, ARC2_BALANCE.FOCUS_DURATION_SEC);
    silenceGain = silenceAnchor * 0.65 * potency;
  } else if (anomaly.type === DESK_ANOMALY_TYPES.REEL_TAPE.id) {
    const scrubAmp = arc2IsReelScrubbing(desk) ? 1.5 : 1.0;
    leadsGain = leadsAnchor * 0.85 * potency * scrubAmp;
    silenceGain = silenceAnchor * 1.35 * potency * scrubAmp;
  } else if (anomaly.type === DESK_ANOMALY_TYPES.ENAMEL_EXHIBIT.id) {
    leadsGain = leadsAnchor * 1.85 * potency;
    silenceGain = silenceAnchor * 1.65 * potency;
  }

  const updatedDesk = {
    ...desk,
    leads: desk.leads + leadsGain,
    lifetimeLeads: desk.lifetimeLeads + leadsGain,
    silence: desk.silence + silenceGain,
    lifetimeSilence: desk.lifetimeSilence + silenceGain,
    activeDeskAnomalies: remaining,
    deskAnomalyInteractions: updatedCounts,
    focusRemainingSeconds: nextFocus,
  };
  return finalizeProgression({ ...state, arc2: updatedDesk }, catalog);
}

export function pivotNextCase(state, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_2_INVESTIGATION) return state;
  const desk = state.arc2;
  if (!arc2CanPivotNextCase(desk)) return state;

  const current = arc2CurrentCase(desk);
  const nextPivots = Math.min(5, (desk.completedCasePivots ?? 0) + 1);
  const isFileZero = nextPivots >= 5;
  const nextAngle = isFileZero ? 5 : Math.min(5, current.number + 1);
  const pinned = desk.pinnedExhibits ?? [];
  const nextPinned =
    current.keystoneReward && !pinned.includes(current.keystoneReward)
      ? [...pinned, current.keystoneReward]
      : pinned;

  const updatedDesk = isFileZero
    ? {
        ...desk,
        leads: Math.max(0.0, desk.leads - current.leadsCost),
        silence: Math.max(0.0, desk.silence - current.silenceCost),
        completedCasePivots: 5,
        caseAngle: 5,
        fileZeroRevealed: true,
      }
    : {
        ...desk,
        leads: 0.0,
        silence: Math.max(0.0, desk.silence - current.silenceCost),
        contacts: {},
        caseAngle: nextAngle,
        completedCasePivots: nextPivots,
        pinnedExhibits: nextPinned,
        activeDeskAnomalies: [],
        focusRemainingSeconds: 0.0,
        reelScrubRemainingSeconds: 0.0,
        secondsUntilNextAnomaly: ARC2_BALANCE.FIRST_DESK_ANOMALY_DELAY_SEC,
      };
  return finalizeProgression({ ...state, arc2: updatedDesk }, catalog);
}

export const connectNextChapter = pivotNextCase;

export function enterArc3Convergence(state, catalog) {
  if (!state.arc2.fileZeroRevealed) return state;
  return finalizeProgression({ ...state, activeArc: GAME_ARCS.ARC_3_CONVERGENCE }, catalog);
}

export function tickArc2(state, deltaSeconds, catalog, rng = Math.random) {
  if (deltaSeconds <= 0.0 || state.activeArc !== GAME_ARCS.ARC_2_INVESTIGATION) return state;
  const desk = state.arc2;
  const nextFocus = Math.max(0.0, (desk.focusRemainingSeconds ?? 0.0) - deltaSeconds);

  let nextScrubRem = desk.reelScrubRemainingSeconds ?? 0.0;
  let nextScrubCd = desk.reelScrubCooldownSeconds ?? 0.0;
  if (nextScrubRem > 0.0) {
    if (desk.lampOn) {
      nextScrubRem = 0.0;
      nextScrubCd = ARC2_BALANCE.REEL_SCRUB_COOLDOWN_SEC;
    } else {
      const after = nextScrubRem - deltaSeconds;
      if (after <= 0.0) {
        nextScrubRem = 0.0;
        nextScrubCd = ARC2_BALANCE.REEL_SCRUB_COOLDOWN_SEC;
      } else {
        nextScrubRem = after;
      }
    }
  } else if (nextScrubCd > 0.0) {
    nextScrubCd = Math.max(0.0, nextScrubCd - deltaSeconds);
  }

  const gainedLeads = arc2LeadsPerSecond(desk) * deltaSeconds;
  const gainedSilence = arc2SilencePerSecond(desk) * deltaSeconds;
  const nextLampOffSec = !desk.lampOn
    ? (desk.totalLampOffSeconds ?? 0.0) + deltaSeconds
    : desk.totalLampOffSeconds ?? 0.0;

  const survived = (desk.activeDeskAnomalies ?? [])
    .map((a) => ({ ...a, remainingSeconds: (a.remainingSeconds ?? 14.0) - deltaSeconds }))
    .filter((a) => a.remainingSeconds > 0.0);

  const spawnRateFactor = arc2IsReelScrubbing(desk) && !desk.lampOn ? 1.65 : 1.0;
  let timer = (desk.secondsUntilNextAnomaly ?? ARC2_BALANCE.FIRST_DESK_ANOMALY_DELAY_SEC) - deltaSeconds * spawnRateFactor;
  let nextId = desk.nextAnomalyId ?? 1;
  const finalAnomalies = [...survived];
  if (timer <= 0.0 && finalAnomalies.length < ARC2_BALANCE.MAX_ACTIVE_DESK_ANOMALIES) {
    finalAnomalies.push(pickDeskAnomaly(desk, nextId, rng));
    nextId += 1;
    timer = desk.lampOn
      ? ARC2_BALANCE.LAMP_ON_ANOMALY_INTERVAL_SEC
      : ARC2_BALANCE.LAMP_OFF_ANOMALY_INTERVAL_SEC;
  }

  const updatedDesk = {
    ...desk,
    leads: desk.leads + gainedLeads,
    lifetimeLeads: desk.lifetimeLeads + gainedLeads,
    silence: desk.silence + gainedSilence,
    lifetimeSilence: desk.lifetimeSilence + gainedSilence,
    totalLampOffSeconds: nextLampOffSec,
    focusRemainingSeconds: nextFocus,
    reelScrubRemainingSeconds: nextScrubRem,
    reelScrubCooldownSeconds: nextScrubCd,
    activeDeskAnomalies: finalAnomalies,
    secondsUntilNextAnomaly: Math.max(0.0, timer),
    nextAnomalyId: nextId,
  };
  return finalizeProgression({ ...state, arc2: updatedDesk }, catalog);
}
