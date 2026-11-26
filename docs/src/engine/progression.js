/**
 * Shared progression helpers, initial state factory, and progress-bound dialogue trigger evaluator.
 */

import {
  DEFAULT_PHASE_NAMES,
  EXCISION_MILESTONES,
  GAME_ARCS,
  ORGAN_BALANCE,
  PHASE_THRESHOLDS,
} from './constants.js?v=3.14';
import { initialArc2State } from './arc2Models.js?v=3.14';
import {
  creatureCount,
  cycleLifetimeIchorDisplay,
  hasNode,
  nextExcisionMilestone,
} from './arc1Math.js?v=3.14';

export function phaseForCount(count) {
  const safe = Math.max(0, Math.floor(count));
  let resolved = 0;
  for (let i = 0; i < PHASE_THRESHOLDS.length; i += 1) {
    if (safe >= PHASE_THRESHOLDS[i]) resolved = i;
  }
  return resolved;
}

export function nextThresholdForPhase(phase) {
  const clamped = Math.max(0, Math.min(phase, PHASE_THRESHOLDS.length - 1));
  return clamped + 1 < PHASE_THRESHOLDS.length ? PHASE_THRESHOLDS[clamped + 1] : null;
}

export function progressWithinPhase(count, finalGoalThreshold = ORGAN_BALANCE.EXCISION_ICHOR_THRESHOLD) {
  const safe = Math.max(0, Math.floor(count));
  const phase = phaseForCount(safe);
  const nextThreshold = nextThresholdForPhase(phase) ?? finalGoalThreshold;
  if (nextThreshold == null) return 1;
  const startThreshold = PHASE_THRESHOLDS[phase];
  const span = Math.max(1, nextThreshold - startThreshold);
  return Math.max(0, Math.min(1, (safe - startThreshold) / span));
}

export function phaseNameFromCatalog(catalog, phaseId) {
  const found = catalog?.phases?.find((p) => p.id === phaseId);
  return found?.name ?? DEFAULT_PHASE_NAMES[Math.max(0, Math.min(phaseId, DEFAULT_PHASE_NAMES.length - 1))];
}

export function initialState(catalog) {
  return {
    activeArc: GAME_ARCS.ARC_1_CHAMBER,
    ichor: 0.0,
    cycleLifetimeIchor: 0.0,
    stillness: 0.0,
    cycleLifetimeStillness: 0.0,
    scarTissue: 0,
    availableScarTissue: 0,
    highestExcisionMilestone: 0,
    unlockedScarNodes: [],
    repeatableScarRanks: {},
    cycle: 0,
    phase: 0,
    phaseName: phaseNameFromCatalog(catalog, 0),
    nextPhaseThreshold: nextThresholdForPhase(0),
    phaseProgress: 0,
    creatures: {},
    graftedTendrils: 0,
    choirLungs: 0,
    graftCooldownRemainingSeconds: 0.0,
    choirLungCooldownRemainingSeconds: 0.0,
    autoValveChargeSeconds: 0.0,
    lastSacrificePulse: 0,
    lastSacrificeKind: null,
    ownedJars: [],
    activeAnomalies: [],
    anomalyInteractions: {},
    secondsSinceLastTap: 0.0,
    isStillnessActive: false,
    gazeRemainingSeconds: 0.0,
    secondsUntilNextAnomaly: ORGAN_BALANCE.FIRST_ANOMALY_DELAY_SEC,
    nextAnomalyId: 1,
    drainSealed: false,
    drainPressure: 0.0,
    dredgedRelics: [],
    lifetimeRelicsDredged: 0,
    echoDiversionPercent: 0,
    echoChargeSeconds: 0.0,
    echoPulsesTriggered: 0,
    clampRemainingSeconds: 0.0,
    marrowBloomRemainingSeconds: 0.0,
    marrowBloomCount: 0,
    unredactedDossierLines: [],
    arc2: initialArc2State(),
    activeDialogue: null,
    discoveredDialogues: [],
    triggeredDialogueIds: [],
  };
}

export function isTriggerSatisfied(state, entry) {
  const trigger = entry.trigger;
  if (!trigger) return false;
  switch (trigger.type) {
    case 'counter_reached':
      return cycleLifetimeIchorDisplay(state) >= trigger.threshold;
    case 'cycle_counter_reached': {
      const parsed = Number.parseInt(trigger.target, 10);
      const requiredCycle = Number.isNaN(parsed) ? 1 : parsed;
      return state.cycle >= requiredCycle && cycleLifetimeIchorDisplay(state) >= trigger.threshold;
    }
    case 'creature_owned':
      if (trigger.target === 'grafted_tendril') return state.graftedTendrils >= trigger.threshold;
      if (trigger.target === 'choir_lung') return state.choirLungs >= trigger.threshold;
      return creatureCount(state, trigger.target) >= trigger.threshold;
    case 'anomaly_interacted':
      return (state.anomalyInteractions?.[trigger.target] ?? 0) >= trigger.threshold;
    case 'stillness_reached':
      return state.cycleLifetimeStillness >= trigger.threshold;
    case 'jar_bottled':
      return state.ownedJars.length >= trigger.threshold;
    case 'excision_completed':
      return state.cycle >= trigger.threshold;
    case 'excision_milestone_reached':
      return state.highestExcisionMilestone >= trigger.threshold;
    case 'scar_node_unlocked':
      return state.unlockedScarNodes.length >= trigger.threshold;
    case 'scar_node_specific':
      return Boolean(trigger.target) && hasNode(state, trigger.target);
    case 'repeatable_graft_total': {
      const totalRanks = Object.values(state.repeatableScarRanks ?? {}).reduce(
        (sum, rank) => sum + (Number(rank) || 0),
        0,
      );
      return totalRanks >= trigger.threshold;
    }
    case 'relic_dredged':
      return state.lifetimeRelicsDredged >= trigger.threshold;
    case 'echo_routed':
      return state.echoPulsesTriggered >= trigger.threshold;
    case 'marrow_bloom':
      return state.marrowBloomCount >= trigger.threshold;
    case 'dossier_line_unredacted':
      return state.unredactedDossierLines.length >= trigger.threshold;
    case 'arc_entered':
      return state.activeArc === trigger.target;
    case 'case_chapter_completed':
      return (state.arc2.completedCasePivots ?? 0) >= trigger.threshold;
    case 'case_angle_reached':
      return (
        state.activeArc === GAME_ARCS.ARC_2_INVESTIGATION &&
        (state.arc2.caseAngle ?? 1) >= trigger.threshold
      );
    case 'corroboration_discovered':
      return (
        Boolean(trigger.target) &&
        (state.arc2.discoveredCorroborations ?? []).includes(trigger.target)
      );
    case 'lamp_toggled':
      if (trigger.target === 'off') {
        return (state.arc2.lampOffCount ?? 0) >= trigger.threshold;
      }
      return (state.arc2.lampToggleCount ?? 0) >= trigger.threshold;
    case 'arc2_silence_reached':
      return (state.arc2.lifetimeSilence ?? 0) >= trigger.threshold;
    case 'reel_scrubbed':
      return (state.arc2.reelScrubCount ?? 0) >= trigger.threshold;
    case 'redaction_dissolved':
      return (state.arc2.dissolvedRedactions?.length ?? 0) >= trigger.threshold;
    case 'desk_anomaly_interacted': {
      const normalizedTarget = trigger.target === 'red_string' ? 'enamel_exhibit' : trigger.target;
      return (state.arc2.deskAnomalyInteractions?.[normalizedTarget] ?? 0) >= trigger.threshold;
    }
    default:
      return false;
  }
}

export function finalizeProgression(state, catalog) {
  const lifetimeDisplay = cycleLifetimeIchorDisplay(state);
  const nextPhase = phaseForCount(lifetimeDisplay);
  const nextPhaseName = phaseNameFromCatalog(catalog, nextPhase);
  const nextThreshold = nextThresholdForPhase(nextPhase);
  const goalThreshold =
    nextExcisionMilestone(state)?.massThreshold ?? EXCISION_MILESTONES[5].massThreshold;
  const nextProgress = progressWithinPhase(lifetimeDisplay, goalThreshold);

  const base =
    state.phase === nextPhase &&
    state.phaseName === nextPhaseName &&
    state.nextPhaseThreshold === nextThreshold &&
    state.phaseProgress === nextProgress
      ? state
      : {
          ...state,
          phase: nextPhase,
          phaseName: nextPhaseName,
          nextPhaseThreshold: nextThreshold,
          phaseProgress: nextProgress,
        };

  const entries = catalog?.entries;
  if (!entries || entries.length === 0) return base;

  const triggeredIds = base.triggeredDialogueIds;
  let newlyTriggered = null;
  for (let i = 0; i < entries.length; i += 1) {
    const entry = entries[i];
    if (entry.once && triggeredIds.includes(entry.id)) continue;
    if (isTriggerSatisfied(base, entry)) {
      if (newlyTriggered === null) newlyTriggered = [];
      newlyTriggered.push(entry);
    }
  }

  if (newlyTriggered === null) return base;

  const latest = newlyTriggered[newlyTriggered.length - 1];
  return {
    ...base,
    activeDialogue: latest,
    discoveredDialogues: [...base.discoveredDialogues, ...newlyTriggered],
    triggeredDialogueIds: [...triggeredIds, ...newlyTriggered.map((e) => e.id)],
  };
}
