/**
 * Save-file serialization & versioned migration (v1 -> v2 compatible, with dedicated Arc 2 codec seam).
 */

import { GAME_ARCS, ORGAN_BALANCE, REPEATABLE_SCAR_GRAFTS } from './constants.js?v=3.11';
import { ARC2_BALANCE, initialArc2State } from './arc2Models.js?v=3.11';
import { finalizeProgression, initialState } from './progression.js?v=3.11';

export function encodeArc2State(arc2) {
  return arc2;
}

export function decodeArc2State(rawArc2) {
  const src = rawArc2 && typeof rawArc2 === 'object' ? rawArc2 : {};
  return {
    ...initialArc2State(),
    leads: Math.max(0, Number(src.leads) || 0),
    lifetimeLeads: Math.max(0, Number(src.lifetimeLeads) || 0),
    silence: Math.max(0, Number(src.silence) || 0),
    lifetimeSilence: Math.max(0, Number(src.lifetimeSilence) || 0),
    complicity: Math.max(0.05, Math.min(1.0, Number(src.complicity) || 0.05)),
    contacts: src.contacts && typeof src.contacts === 'object' ? { ...src.contacts } : {},
    ownedDeductions: Array.isArray(src.ownedDeductions) ? [...src.ownedDeductions] : [],
    completedChapters: Math.max(0, Math.min(4, Math.floor(Number(src.completedChapters) || 0))),
    activeDeskAnomalies: Array.isArray(src.activeDeskAnomalies) ? [...src.activeDeskAnomalies] : [],
    deskAnomalyInteractions:
      src.deskAnomalyInteractions && typeof src.deskAnomalyInteractions === 'object'
        ? { ...src.deskAnomalyInteractions }
        : {},
    secondsSinceLastAction: Math.max(0, Number(src.secondsSinceLastAction) || 0),
    isSilenceActive: Boolean(src.isSilenceActive),
    focusRemainingSeconds: Math.max(0, Number(src.focusRemainingSeconds) || 0),
    secondsUntilNextAnomaly: Math.max(
      0,
      Number(src.secondsUntilNextAnomaly) || ARC2_BALANCE.FIRST_DESK_ANOMALY_DELAY_SEC,
    ),
    nextAnomalyId: Math.max(1, Math.floor(Number(src.nextAnomalyId) || 1)),
    fileZeroRevealed: Boolean(src.fileZeroRevealed),
  };
}

export function serializeState(state) {
  return JSON.stringify({
    version: 2,
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

    const arc2 = decodeArc2State(data.arc2);

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
