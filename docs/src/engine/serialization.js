/**
 * Save-file serialization & versioned migration (v1 -> v2 -> v3 compatible, with dedicated Arc 2
 * Slow Boil codec seam preserving Arc 1 state untouched).
 */

import { GAME_ARCS, ORGAN_BALANCE, REPEATABLE_SCAR_GRAFTS } from './constants.js?v=3.15';
import {
  ARC2_BALANCE,
  AUDIT_REDACTIONS,
  CASE_EXHIBITS,
  CORROBORATIONS,
  DESK_ANOMALY_TYPES,
  canonicalPairKey,
  initialArc2State,
} from './arc2Models.js?v=3.15';
import {
  ARC3_BALANCE,
  ARC3_STRATA,
  ARC3_UNMAKE_ACTS,
  BREACH_RUPTURE_LIST,
  initialArc3State,
} from './arc3Models.js?v=3.15';
import { finalizeProgression, initialState } from './progression.js?v=3.15';

const VALID_EXHIBIT_IDS = new Set(CASE_EXHIBITS.map((e) => e.id));
const VALID_CORR_IDS = new Set(CORROBORATIONS.map((c) => c.id));
const VALID_REDACTION_IDS = new Set(AUDIT_REDACTIONS.map((r) => r.id));
const VALID_ANOMALY_IDS = new Set(Object.values(DESK_ANOMALY_TYPES).map((t) => t.id));
const VALID_ARC3_STRATUM_IDS = new Set(ARC3_STRATA.map((s) => s.id));
const VALID_ARC3_UNMAKE_IDS = new Set(ARC3_UNMAKE_ACTS.map((a) => a.id));
const VALID_ARC3_RUPTURE_IDS = new Set(BREACH_RUPTURE_LIST.map((r) => r.id));

function normalizeAnomalyTypeId(rawId) {
  if (rawId === 'red_string') return DESK_ANOMALY_TYPES.ENAMEL_EXHIBIT.id;
  return VALID_ANOMALY_IDS.has(rawId) ? rawId : null;
}

export function encodeArc2State(arc2) {
  return arc2;
}

export function decodeArc2State(rawArc2, schemaVersion = 3) {
  const src = rawArc2 && typeof rawArc2 === 'object' ? rawArc2 : {};
  const isV3 = schemaVersion >= 3 || src.caseAngle != null || Array.isArray(src.pinnedExhibits);

  let caseAngle;
  let completedCasePivots;
  let pinnedExhibits;
  let fileZeroRevealed = Boolean(src.fileZeroRevealed);

  if (isV3) {
    completedCasePivots = Math.max(0, Math.min(5, Math.floor(Number(src.completedCasePivots) || 0)));
    caseAngle = Math.max(1, Math.min(5, Math.floor(Number(src.caseAngle) || 1)));
    const rawPinned = Array.isArray(src.pinnedExhibits) ? src.pinnedExhibits : [];
    const set = new Set(['water_stained_petition']);
    for (const id of rawPinned) {
      if (VALID_EXHIBIT_IDS.has(id)) set.add(id);
    }
    pinnedExhibits = Array.from(set);
  } else {
    // v2 -> v3 migration preserving investigator progress cleanly
    const legacyChapters = Math.max(0, Math.min(4, Math.floor(Number(src.completedChapters) || 0)));
    completedCasePivots = legacyChapters >= 4 ? 5 : legacyChapters;
    caseAngle = Math.min(5, legacyChapters + 1);
    fileZeroRevealed = fileZeroRevealed || legacyChapters >= 4;

    const set = new Set(['water_stained_petition']);
    if (legacyChapters >= 1) set.add('tax_plat_1998');
    if (legacyChapters >= 2) set.add('second_chair_polaroid');
    if (legacyChapters >= 3) set.add('reel_four_waveform');
    if (legacyChapters >= 4) set.add('drainage_loop_blueprint');

    const legacyDeds = Array.isArray(src.ownedDeductions) ? src.ownedDeductions : [];
    if (legacyDeds.includes('hydro_ward_invoices')) set.add('unlisted_pay_stub');
    if (legacyDeds.includes('missing_caretaker_roster')) set.add('inside_condensation_swab');
    if (legacyDeds.includes('mortar_sample_match')) set.add('tuesday_statement_copy');
    pinnedExhibits = Array.from(set);
  }

  const wiredPairs = [];
  if (Array.isArray(src.wiredPairs)) {
    const seen = new Set();
    for (const rawKey of src.wiredPairs) {
      if (typeof rawKey !== 'string') continue;
      const parts = rawKey.split('|');
      if (parts.length !== 2) continue;
      if (!VALID_EXHIBIT_IDS.has(parts[0]) || !VALID_EXHIBIT_IDS.has(parts[1])) continue;
      const key = canonicalPairKey(parts[0], parts[1]);
      if (key && !seen.has(key)) {
        seen.add(key);
        wiredPairs.push(key);
      }
    }
  }

  const discoveredCorroborations = Array.isArray(src.discoveredCorroborations)
    ? Array.from(new Set(src.discoveredCorroborations.filter((id) => VALID_CORR_IDS.has(id))))
    : [];

  const dissolvedRedactions = Array.isArray(src.dissolvedRedactions)
    ? Array.from(new Set(src.dissolvedRedactions.filter((id) => VALID_REDACTION_IDS.has(id))))
    : [];

  const activeDeskAnomalies = Array.isArray(src.activeDeskAnomalies)
    ? src.activeDeskAnomalies
        .map((a) => {
          const type = normalizeAnomalyTypeId(a?.type);
          if (!type) return null;
          return {
            id: Math.max(1, Math.floor(Number(a.id) || 1)),
            type,
            xFraction: Math.max(0.04, Math.min(0.95, Number(a.xFraction) || 0.16)),
            yFraction: Math.max(0.15, Math.min(0.88, Number(a.yFraction) || 0.32)),
            remainingSeconds: Math.max(
              1.0,
              Number(a.remainingSeconds) || ARC2_BALANCE.DESK_ANOMALY_LIFETIME_SEC,
            ),
          };
        })
        .filter(Boolean)
    : [];

  const deskAnomalyInteractions = {};
  if (src.deskAnomalyInteractions && typeof src.deskAnomalyInteractions === 'object') {
    for (const [rawKey, rawVal] of Object.entries(src.deskAnomalyInteractions)) {
      const normalized = normalizeAnomalyTypeId(rawKey);
      const count = Math.max(0, Math.floor(Number(rawVal) || 0));
      if (normalized && count > 0) {
        deskAnomalyInteractions[normalized] = (deskAnomalyInteractions[normalized] ?? 0) + count;
      }
    }
  }

  return {
    ...initialArc2State(),
    leads: Math.max(0, Number(src.leads) || 0),
    lifetimeLeads: Math.max(0, Number(src.lifetimeLeads) || 0),
    silence: Math.max(0, Number(src.silence) || 0),
    lifetimeSilence: Math.max(0, Number(src.lifetimeSilence) || 0),
    caseAngle,
    completedCasePivots,
    lampOn: src.lampOn != null ? Boolean(src.lampOn) : true,
    lampToggleCount: Math.max(0, Math.floor(Number(src.lampToggleCount) || 0)),
    lampOffCount: Math.max(0, Math.floor(Number(src.lampOffCount) || 0)),
    totalLampOffSeconds: Math.max(0, Number(src.totalLampOffSeconds) || 0),
    contacts: src.contacts && typeof src.contacts === 'object' ? { ...src.contacts } : {},
    pinnedExhibits,
    wiredPairs,
    discoveredCorroborations,
    reelScrubRemainingSeconds: Math.max(0, Number(src.reelScrubRemainingSeconds) || 0),
    reelScrubCooldownSeconds: Math.max(0, Number(src.reelScrubCooldownSeconds) || 0),
    reelScrubCount: Math.max(0, Math.floor(Number(src.reelScrubCount) || 0)),
    dissolvedRedactions,
    activeDeskAnomalies,
    deskAnomalyInteractions,
    focusRemainingSeconds: Math.max(0, Number(src.focusRemainingSeconds) || 0),
    secondsUntilNextAnomaly: Math.max(
      0,
      Number(src.secondsUntilNextAnomaly) || ARC2_BALANCE.FIRST_DESK_ANOMALY_DELAY_SEC,
    ),
    nextAnomalyId: Math.max(1, Math.floor(Number(src.nextAnomalyId) || 1)),
    fileZeroRevealed,
  };
}

export function encodeArc3State(arc3) {
  return arc3 ?? initialArc3State();
}

export function decodeArc3State(rawArc3) {
  if (!rawArc3 || typeof rawArc3 !== 'object') return initialArc3State();
  const src = rawArc3;
  const lockedWedges = Array.isArray(src.lockedWedges)
    ? Array.from(new Set(src.lockedWedges.filter((id) => VALID_ARC3_STRATUM_IDS.has(id))))
    : [];
  const completedUnmakes = Array.isArray(src.completedUnmakes)
    ? Array.from(new Set(src.completedUnmakes.filter((id) => VALID_ARC3_UNMAKE_IDS.has(id))))
    : [];
  const activeRuptures = Array.isArray(src.activeRuptures)
    ? src.activeRuptures
        .map((r) => {
          if (!r || !VALID_ARC3_RUPTURE_IDS.has(r.type)) return null;
          return {
            id: Math.max(1, Math.floor(Number(r.id) || 1)),
            type: r.type,
            xFraction: Math.max(0.08, Math.min(0.92, Number(r.xFraction) || 0.2)),
            yFraction: Math.max(0.14, Math.min(0.75, Number(r.yFraction) || 0.3)),
            remainingSeconds: Math.max(
              0.5,
              Number(r.remainingSeconds) || ARC3_BALANCE.RUPTURE_LIFETIME_SEC,
            ),
          };
        })
        .filter(Boolean)
    : [];
  const ruptureInteractions = {};
  if (src.ruptureInteractions && typeof src.ruptureInteractions === 'object') {
    for (const [key, val] of Object.entries(src.ruptureInteractions)) {
      const count = Math.max(0, Math.floor(Number(val) || 0));
      if (VALID_ARC3_RUPTURE_IDS.has(key) && count > 0) {
        ruptureInteractions[key] = count;
      }
    }
  }
  const finalPullsCompleted = Math.max(
    0,
    Math.min(ARC3_BALANCE.FINAL_PULLS_TOTAL, Math.floor(Number(src.finalPullsCompleted) || 0)),
  );
  return {
    ...initialArc3State(),
    currentStratum: Math.max(1, Math.min(5, Math.floor(Number(src.currentStratum) || 1))),
    lockedWedges,
    fracture: Math.max(0, Number(src.fracture) || 0),
    hemorrhage: Math.max(0, Number(src.hemorrhage) || 0),
    lifetimeHemorrhage: Math.max(0, Number(src.lifetimeHemorrhage) || 0),
    wrenchCount: Math.max(0, Math.floor(Number(src.wrenchCount) || 0)),
    momentumCharge: Math.max(0, Math.min(1, Number(src.momentumCharge) || 0)),
    secondsSinceLastAction: Math.max(0, Number(src.secondsSinceLastAction) || 0),
    clotFreezeRemainingSeconds: Math.max(0, Number(src.clotFreezeRemainingSeconds) || 0),
    ribForceRemainingSeconds: Math.max(0, Number(src.ribForceRemainingSeconds) || 0),
    frenzyRemainingSeconds: Math.max(0, Number(src.frenzyRemainingSeconds) || 0),
    completedUnmakes,
    activeRuptures,
    ruptureInteractions,
    secondsUntilNextRupture: Math.max(
      0,
      Number(src.secondsUntilNextRupture) || ARC3_BALANCE.FIRST_RUPTURE_DELAY_SEC,
    ),
    nextRuptureId: Math.max(1, Math.floor(Number(src.nextRuptureId) || 1)),
    finalPullsCompleted,
    finalPullCooldownSeconds: Math.max(0, Number(src.finalPullCooldownSeconds) || 0),
    submersionComplete:
      Boolean(src.submersionComplete) || finalPullsCompleted >= ARC3_BALANCE.FINAL_PULLS_TOTAL,
  };
}

export function serializeState(state) {
  return JSON.stringify({
    version: 4,
    activeArc: state.activeArc,
    ichor: state.ichor,
    cycleLifetimeIchor: state.cycleLifetimeIchor,
    stillness: state.stillness,
    cycleLifetimeStillness: state.cycleLifetimeStillness,
    scarTissue: state.scarTissue,
    availableScarTissue: state.availableScarTissue,
    highestExcisionMilestone: state.highestExcisionMilestone,
    unlockedScarNodes: state.unlockedScarNodes,
    repeatableScarRanks: state.repeatableScarRanks ?? {},
    cycle: state.cycle,
    creatures: state.creatures,
    graftedTendrils: state.graftedTendrils,
    choirLungs: state.choirLungs,
    graftCooldownRemainingSeconds: state.graftCooldownRemainingSeconds ?? 0.0,
    choirLungCooldownRemainingSeconds: state.choirLungCooldownRemainingSeconds ?? 0.0,
    ownedJars: state.ownedJars,
    activeAnomalies: state.activeAnomalies,
    anomalyInteractions: state.anomalyInteractions,
    secondsSinceLastTap: state.secondsSinceLastTap,
    isStillnessActive: state.isStillnessActive,
    gazeRemainingSeconds: state.gazeRemainingSeconds,
    secondsUntilNextAnomaly: state.secondsUntilNextAnomaly,
    nextAnomalyId: state.nextAnomalyId,
    drainSealed: state.drainSealed,
    drainPressure: state.drainPressure,
    dredgedRelics: state.dredgedRelics,
    lifetimeRelicsDredged: state.lifetimeRelicsDredged,
    echoDiversionPercent: state.echoDiversionPercent,
    echoChargeSeconds: state.echoChargeSeconds,
    echoPulsesTriggered: state.echoPulsesTriggered,
    clampRemainingSeconds: state.clampRemainingSeconds,
    marrowBloomRemainingSeconds: state.marrowBloomRemainingSeconds,
    marrowBloomCount: state.marrowBloomCount,
    unredactedDossierLines: state.unredactedDossierLines,
    arc2: encodeArc2State(state.arc2),
    arc3: encodeArc3State(state.arc3),
    triggeredDialogueIds: state.triggeredDialogueIds,
    activeDialogueId: state.activeDialogue?.id ?? null,
    discoveredDialogueIds: state.discoveredDialogues.map((d) => d.id),
  });
}

export function deserializeState(rawJson, catalog) {
  try {
    if (!rawJson) return null;
    const data = JSON.parse(rawJson);
    if (!data || typeof data !== 'object' || typeof data.ichor !== 'number') return null;

    const schemaVersion = Math.max(1, Math.floor(Number(data.version) || 1));
    const entryMap = new Map((catalog?.entries ?? []).map((e) => [e.id, e]));
    const discovered = (Array.isArray(data.discoveredDialogueIds) ? data.discoveredDialogueIds : [])
      .map((id) => entryMap.get(id))
      .filter(Boolean);
    const active = data.activeDialogueId ? entryMap.get(data.activeDialogueId) ?? null : null;
    const scarLifetime = Math.max(0, Math.floor(Number(data.scarTissue) || 0));
    const cycleVal = Math.max(0, Math.floor(Number(data.cycle) || 0));
    const highestM =
      data.highestExcisionMilestone != null
        ? Math.max(0, Math.min(6, Math.floor(Number(data.highestExcisionMilestone) || 0)))
        : cycleVal > 0
          ? 1
          : 0;
    const availScar =
      data.availableScarTissue != null
        ? Math.max(0, Math.floor(Number(data.availableScarTissue) || 0))
        : scarLifetime;

    const arc2 = decodeArc2State(data.arc2, schemaVersion);
    const arc3 = decodeArc3State(data.arc3);

    const validArc = Object.values(GAME_ARCS).includes(data.activeArc)
      ? data.activeArc
      : GAME_ARCS.ARC_1_CHAMBER;

    const repeatableRanks = {};
    if (data.repeatableScarRanks && typeof data.repeatableScarRanks === 'object') {
      for (const graft of REPEATABLE_SCAR_GRAFTS) {
        const rank = Math.floor(Number(data.repeatableScarRanks[graft.id]) || 0);
        if (rank > 0) repeatableRanks[graft.id] = rank;
      }
    }

    const restored = {
      ...initialState(catalog),
      activeArc: validArc,
      ichor: Math.max(0, Number(data.ichor) || 0),
      cycleLifetimeIchor: Math.max(0, Number(data.cycleLifetimeIchor) || 0),
      stillness: Math.max(0, Number(data.stillness) || 0),
      cycleLifetimeStillness: Math.max(0, Number(data.cycleLifetimeStillness) || 0),
      scarTissue: scarLifetime,
      availableScarTissue: availScar,
      highestExcisionMilestone: highestM,
      unlockedScarNodes: Array.isArray(data.unlockedScarNodes) ? [...data.unlockedScarNodes] : [],
      repeatableScarRanks: repeatableRanks,
      cycle: cycleVal,
      creatures: data.creatures && typeof data.creatures === 'object' ? { ...data.creatures } : {},
      graftedTendrils: Math.max(0, Math.floor(Number(data.graftedTendrils) || 0)),
      choirLungs: Math.max(0, Math.floor(Number(data.choirLungs) || 0)),
      graftCooldownRemainingSeconds: Math.max(0, Number(data.graftCooldownRemainingSeconds) || 0),
      choirLungCooldownRemainingSeconds: Math.max(
        0,
        Number(data.choirLungCooldownRemainingSeconds) || 0,
      ),
      ownedJars: Array.isArray(data.ownedJars) ? [...data.ownedJars] : [],
      activeAnomalies: Array.isArray(data.activeAnomalies) ? [...data.activeAnomalies] : [],
      anomalyInteractions:
        data.anomalyInteractions && typeof data.anomalyInteractions === 'object'
          ? { ...data.anomalyInteractions }
          : {},
      secondsSinceLastTap: Math.max(0, Number(data.secondsSinceLastTap) || 0),
      isStillnessActive: Boolean(data.isStillnessActive),
      gazeRemainingSeconds: Math.max(0, Number(data.gazeRemainingSeconds) || 0),
      secondsUntilNextAnomaly: Math.max(
        0,
        Number(data.secondsUntilNextAnomaly) || ORGAN_BALANCE.FIRST_ANOMALY_DELAY_SEC,
      ),
      nextAnomalyId: Math.max(1, Math.floor(Number(data.nextAnomalyId) || 1)),
      drainSealed: Boolean(data.drainSealed),
      drainPressure: Math.max(0, Math.min(100, Number(data.drainPressure) || 0)),
      dredgedRelics: Array.isArray(data.dredgedRelics) ? [...data.dredgedRelics] : [],
      lifetimeRelicsDredged: Math.max(0, Math.floor(Number(data.lifetimeRelicsDredged) || 0)),
      echoDiversionPercent: [0, 25, 50].includes(data.echoDiversionPercent)
        ? data.echoDiversionPercent
        : 0,
      echoChargeSeconds: Math.max(0, Number(data.echoChargeSeconds) || 0),
      echoPulsesTriggered: Math.max(0, Math.floor(Number(data.echoPulsesTriggered) || 0)),
      clampRemainingSeconds: Math.max(0, Number(data.clampRemainingSeconds) || 0),
      marrowBloomRemainingSeconds: Math.max(0, Number(data.marrowBloomRemainingSeconds) || 0),
      marrowBloomCount: Math.max(0, Math.floor(Number(data.marrowBloomCount) || 0)),
      unredactedDossierLines: Array.isArray(data.unredactedDossierLines)
        ? [...data.unredactedDossierLines]
        : [],
      arc2,
      arc3,
      triggeredDialogueIds: Array.isArray(data.triggeredDialogueIds)
        ? [...data.triggeredDialogueIds]
        : [],
      discoveredDialogues: discovered,
      activeDialogue: active ?? (discovered.length > 0 ? discovered[discovered.length - 1] : null),
    };
    return finalizeProgression(restored, catalog);
  } catch {
    return null;
  }
}
