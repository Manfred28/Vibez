/**
 * Pure deterministic game engine for "The Tending of the Unnamed Organ" (Web / GitHub Pages).
 * Maintains 1:1 mechanical, mathematical, and dialogue-trigger parity with the Android Kotlin engine
 * (`OrganGameModels.kt`, `OrganGameEngine.kt`, `DialogueModels.kt`).
 *
 * Narrative fragments are never hardcoded here — they are loaded at runtime from `./assets/dialogues.json`.
 */

export const PHASE_THRESHOLDS = Object.freeze([0, 75, 1600, 16000, 85000]);

export const CREATURE_TYPES = Object.freeze({
  SUTURE_HAND: Object.freeze({
    id: 'suture_hand',
    code: 'SPEC-01',
    name: 'SUTURE-HAND',
    desc: 'Stitched appendage gripping and pressing the wall valve.',
    baseCost: 25.0,
    costGrowth: 1.24,
    baseIchorPerSec: 0.6,
    unlockIchorThreshold: 25,
    portrait: './assets/img_suture_hand.jpg',
    portraitAlt: 'Stitched pale anatomical Suture-Hand specimen',
  }),
  LEECH_CLERK: Object.freeze({
    id: 'leech_clerk',
    code: 'SPEC-02',
    name: 'LEECH-CLERK',
    desc: 'Blind annelid clinging to the counter glass, chewing digits.',
    baseCost: 180.0,
    costGrowth: 1.26,
    baseIchorPerSec: 2.4,
    unlockIchorThreshold: 75,
    portrait: './assets/img_leech_clerk.jpg',
    portraitAlt: 'Blind segmented Leech-Clerk specimen',
  }),
  WEEPING_LAMP: Object.freeze({
    id: 'weeping_lamp',
    code: 'SPEC-03',
    name: 'WEEPING LAMP',
    desc: 'Ceiling vessel that drips Stillness & 2× Ichor after 3s idle.',
    baseCost: 1190.0,
    costGrowth: 1.32,
    baseIchorPerSec: 6.0,
    unlockIchorThreshold: 1000,
    portrait: null,
    portraitAlt: 'Suspended glass Weeping Lamp specimen',
  }),
});

export const CREATURE_LIST = Object.freeze([
  CREATURE_TYPES.SUTURE_HAND,
  CREATURE_TYPES.LEECH_CLERK,
  CREATURE_TYPES.WEEPING_LAMP,
]);

export const SPECIMEN_JARS = Object.freeze({
  YESTERDAYS_DUST: Object.freeze({
    id: 'yesterdays_dust',
    name: "YESTERDAY'S DUST",
    effect: 'Valve extraction +3 base Ichor',
    stillnessCost: 24.0,
  }),
  UNSPENT_BREATH: Object.freeze({
    id: 'unspent_breath',
    name: 'UNSPENT BREATH',
    effect: 'Suture-Hands & Leech-Clerks ×1.75',
    stillnessCost: 65.0,
  }),
  ALMOST_A_NAME: Object.freeze({
    id: 'almost_a_name',
    name: 'A NAME YOU ALMOST REMEMBER',
    effect: 'Weeping Lamps & wall anomalies ×1.8',
    stillnessCost: 140.0,
  }),
});

export const SPECIMEN_JAR_LIST = Object.freeze([
  SPECIMEN_JARS.YESTERDAYS_DUST,
  SPECIMEN_JARS.UNSPENT_BREATH,
  SPECIMEN_JARS.ALMOST_A_NAME,
]);

export const WALL_ANOMALY_TYPES = Object.freeze({
  PALE_TOOTH: Object.freeze({
    id: 'pale_tooth',
    minPhase: 1,
    label: 'PULL',
    ariaLabel: 'Pull pale tooth protruding from plaster crack',
  }),
  GLASS_EYE: Object.freeze({
    id: 'glass_eye',
    minPhase: 2,
    label: 'WIPE',
    ariaLabel: 'Wipe condensation from blinking glass eye',
  }),
  SWOLLEN_NODULE: Object.freeze({
    id: 'swollen_nodule',
    minPhase: 3,
    label: 'PROD',
    ariaLabel: 'Prod swollen nodule pulsing along fissure',
  }),
});

export const WALL_ANOMALY_LIST = Object.freeze([
  WALL_ANOMALY_TYPES.PALE_TOOTH,
  WALL_ANOMALY_TYPES.GLASS_EYE,
  WALL_ANOMALY_TYPES.SWOLLEN_NODULE,
]);

export const ORGAN_BALANCE = Object.freeze({
  STILLNESS_IDLE_DELAY_SEC: 3.0,
  LAMP_STILLNESS_PER_SEC: 0.45,
  GAZE_DURATION_SEC: 12.0,
  GAZE_PASSIVE_MULTIPLIER: 1.6,
  YESTERDAYS_DUST_TAP_BONUS: 3.0,
  UNSPENT_BREATH_MULTIPLIER: 1.75,
  ALMOST_A_NAME_MULTIPLIER: 1.8,
  GRAFT_SUTURE_COST: 2,
  GRAFT_LEECH_COST: 2,
  GRAFT_ICHOR_PER_SEC: 18.0,
  GRAFT_STILLNESS_PER_SEC: 0.7,
  EXCISION_ICHOR_THRESHOLD: 210000,
  SCAR_TISSUE_BONUS_PER_UNIT: 0.75,
  SCAR_STILLNESS_BONUS_PER_UNIT: 0.25,
  FIRST_ANOMALY_DELAY_SEC: 14.0,
  ANOMALY_SPAWN_INTERVAL_SEC: 26.0,
  MAX_ACTIVE_ANOMALIES: 3,
  TOOTH_BASE_ICHOR_REWARD: 25.0,
  NODULE_BASE_ICHOR_REWARD: 220.0,
  NODULE_BASE_STILLNESS_REWARD: 6.0,
});

const ANOMALY_ANCHORS = Object.freeze([
  [0.14, 0.26],
  [0.86, 0.28],
  [0.14, 0.74],
  [0.86, 0.72],
  [0.20, 0.50],
  [0.80, 0.52],
]);

export const DEFAULT_PHASE_NAMES = Object.freeze([
  'THE COUNTING ROOM',
  'UNCANNY DRIFT',
  'APPARATUS & FLESH',
  'COLLAPSE OF MEANING',
  'THE ENDLESS HOLLOW',
]);

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
  return found?.name ?? DEFAULT_PHASE_NAMES[Math.max(0, Math.min(phaseId, 4))];
}

export function creatureById(id) {
  return CREATURE_LIST.find((c) => c.id === id) ?? null;
}

export function jarById(id) {
  return SPECIMEN_JAR_LIST.find((j) => j.id === id) ?? null;
}

export function creatureCount(state, typeId) {
  return state.creatures?.[typeId] ?? 0;
}

export function creatureCost(state, typeId) {
  const spec = typeof typeId === 'string' ? creatureById(typeId) : typeId;
  if (!spec) return 0;
  const owned = creatureCount(state, spec.id);
  return Math.floor(spec.baseCost * Math.pow(spec.costGrowth, owned));
}

export function scarMultiplier(state) {
  return 1.0 + state.scarTissue * ORGAN_BALANCE.SCAR_TISSUE_BONUS_PER_UNIT;
}

export function scarStillnessMultiplier(state) {
  return 1.0 + state.scarTissue * ORGAN_BALANCE.SCAR_STILLNESS_BONUS_PER_UNIT;
}

export function gazeMultiplier(state) {
  return state.gazeRemainingSeconds > 0 ? ORGAN_BALANCE.GAZE_PASSIVE_MULTIPLIER : 1.0;
}

export function manualTapYield(state) {
  const dustBonus = state.ownedJars.includes(SPECIMEN_JARS.YESTERDAYS_DUST.id)
    ? ORGAN_BALANCE.YESTERDAYS_DUST_TAP_BONUS
    : 0.0;
  const toothBonus = (state.anomalyInteractions?.pale_tooth ?? 0) * 0.5;
  return (1.0 + dustBonus + toothBonus) * scarMultiplier(state);
}

export function effectiveCreatureIchorPerSec(state, typeId) {
  const spec = typeof typeId === 'string' ? creatureById(typeId) : typeId;
  if (!spec) return 0.0;

  let specimenMultiplier = 1.0;
  if (spec.id === CREATURE_TYPES.SUTURE_HAND.id || spec.id === CREATURE_TYPES.LEECH_CLERK.id) {
    if (state.ownedJars.includes(SPECIMEN_JARS.UNSPENT_BREATH.id)) {
      specimenMultiplier = ORGAN_BALANCE.UNSPENT_BREATH_MULTIPLIER;
    }
  } else if (spec.id === CREATURE_TYPES.WEEPING_LAMP.id) {
    const jarMult = state.ownedJars.includes(SPECIMEN_JARS.ALMOST_A_NAME.id)
      ? ORGAN_BALANCE.ALMOST_A_NAME_MULTIPLIER
      : 1.0;
    specimenMultiplier = jarMult * (state.isStillnessActive ? 2.0 : 1.0);
  }
  return spec.baseIchorPerSec * specimenMultiplier * scarMultiplier(state) * gazeMultiplier(state);
}

export function totalCreatureIchorPerSec(state, typeId) {
  const id = typeof typeId === 'string' ? typeId : typeId.id;
  return creatureCount(state, id) * effectiveCreatureIchorPerSec(state, id);
}

export function effectiveLampStillnessPerSec(state) {
  const jarMult = state.ownedJars.includes(SPECIMEN_JARS.ALMOST_A_NAME.id)
    ? ORGAN_BALANCE.ALMOST_A_NAME_MULTIPLIER
    : 1.0;
  return (
    ORGAN_BALANCE.LAMP_STILLNESS_PER_SEC *
    jarMult *
    scarStillnessMultiplier(state) *
    gazeMultiplier(state)
  );
}

export function ichorPerSecond(state) {
  const creaturesRate =
    totalCreatureIchorPerSec(state, CREATURE_TYPES.SUTURE_HAND.id) +
    totalCreatureIchorPerSec(state, CREATURE_TYPES.LEECH_CLERK.id) +
    totalCreatureIchorPerSec(state, CREATURE_TYPES.WEEPING_LAMP.id);
  const graftRate =
    state.graftedTendrils *
    ORGAN_BALANCE.GRAFT_ICHOR_PER_SEC *
    scarMultiplier(state) *
    gazeMultiplier(state);
  return creaturesRate + graftRate;
}

export function stillnessPerSecond(state) {
  const lampStillness = state.isStillnessActive
    ? creatureCount(state, CREATURE_TYPES.WEEPING_LAMP.id) * effectiveLampStillnessPerSec(state)
    : 0.0;
  const graftStillness =
    state.graftedTendrils *
    ORGAN_BALANCE.GRAFT_STILLNESS_PER_SEC *
    scarStillnessMultiplier(state) *
    gazeMultiplier(state);
  return lampStillness + graftStillness;
}

export function ichorDisplay(state) {
  return Math.floor(state.ichor);
}

export function cycleLifetimeIchorDisplay(state) {
  return Math.floor(state.cycleLifetimeIchor);
}

export function stillnessDisplay(state) {
  return Math.floor(state.stillness);
}

export function isApparatusUnlocked(state) {
  return (
    state.cycle > 0 ||
    cycleLifetimeIchorDisplay(state) >= CREATURE_TYPES.SUTURE_HAND.unlockIchorThreshold ||
    Object.values(state.creatures).some((count) => count > 0)
  );
}

export function isJarsAndGraftUnlocked(state) {
  return (
    state.cycle > 0 ||
    state.phase >= 2 ||
    stillnessDisplay(state) > 0 ||
    state.ownedJars.length > 0 ||
    state.graftedTendrils > 0
  );
}

export function isGraftUnlocked(state) {
  return state.cycle > 0 || state.phase >= 3 || state.graftedTendrils > 0;
}

export function canAffordGraft(state) {
  return (
    isGraftUnlocked(state) &&
    creatureCount(state, CREATURE_TYPES.SUTURE_HAND.id) >= ORGAN_BALANCE.GRAFT_SUTURE_COST &&
    creatureCount(state, CREATURE_TYPES.LEECH_CLERK.id) >= ORGAN_BALANCE.GRAFT_LEECH_COST
  );
}

export function canExcise(state) {
  return cycleLifetimeIchorDisplay(state) >= ORGAN_BALANCE.EXCISION_ICHOR_THRESHOLD;
}

export function pendingScarTissue(state) {
  if (!canExcise(state)) return 0;
  return Math.max(
    1,
    Math.floor(Math.sqrt(state.cycleLifetimeIchor / ORGAN_BALANCE.EXCISION_ICHOR_THRESHOLD)),
  );
}

export function initialState(catalog) {
  return {
    ichor: 0.0,
    cycleLifetimeIchor: 0.0,
    stillness: 0.0,
    cycleLifetimeStillness: 0.0,
    scarTissue: 0,
    cycle: 0,
    phase: 0,
    phaseName: phaseNameFromCatalog(catalog, 0),
    nextPhaseThreshold: nextThresholdForPhase(0),
    phaseProgress: 0,
    creatures: {},
    graftedTendrils: 0,
    ownedJars: [],
    activeAnomalies: [],
    anomalyInteractions: {},
    secondsSinceLastTap: 0.0,
    isStillnessActive: false,
    gazeRemainingSeconds: 0.0,
    secondsUntilNextAnomaly: ORGAN_BALANCE.FIRST_ANOMALY_DELAY_SEC,
    nextAnomalyId: 1,
    activeDialogue: null,
    discoveredDialogues: [],
    triggeredDialogueIds: [],
  };
}

function isTriggerSatisfied(state, entry) {
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
      if (trigger.target === 'grafted_tendril') {
        return state.graftedTendrils >= trigger.threshold;
      }
      return creatureCount(state, trigger.target) >= trigger.threshold;
    case 'anomaly_interacted':
      return (state.anomalyInteractions?.[trigger.target] ?? 0) >= trigger.threshold;
    case 'stillness_reached':
      return state.cycleLifetimeStillness >= trigger.threshold;
    case 'jar_bottled':
      return state.ownedJars.length >= trigger.threshold;
    case 'excision_completed':
      return state.cycle >= trigger.threshold;
    default:
      return false;
  }
}

function finalizeProgression(state, catalog) {
  const lifetimeDisplay = cycleLifetimeIchorDisplay(state);
  const nextPhase = phaseForCount(lifetimeDisplay);
  const base = {
    ...state,
    phase: nextPhase,
    phaseName: phaseNameFromCatalog(catalog, nextPhase),
    nextPhaseThreshold: nextThresholdForPhase(nextPhase),
    phaseProgress: progressWithinPhase(lifetimeDisplay, ORGAN_BALANCE.EXCISION_ICHOR_THRESHOLD),
  };

  const entries = catalog?.entries ?? [];
  const triggeredSet = new Set(base.triggeredDialogueIds);
  const newlyTriggered = entries.filter(
    (entry) => (!entry.once || !triggeredSet.has(entry.id)) && isTriggerSatisfied(base, entry),
  );

  if (newlyTriggered.length === 0) return base;

  const latest = newlyTriggered[newlyTriggered.length - 1];
  return {
    ...base,
    activeDialogue: latest,
    discoveredDialogues: [...base.discoveredDialogues, ...newlyTriggered],
    triggeredDialogueIds: [...base.triggeredDialogueIds, ...newlyTriggered.map((e) => e.id)],
  };
}

function advanceAnomalySpawner(state, deltaSeconds) {
  if (state.phase < 1 || state.activeAnomalies.length >= ORGAN_BALANCE.MAX_ACTIVE_ANOMALIES) {
    return state;
  }

  const remaining = state.secondsUntilNextAnomaly - deltaSeconds;
  if (remaining > 0.0) {
    return { ...state, secondsUntilNextAnomaly: remaining };
  }

  const eligibleTypes = WALL_ANOMALY_LIST.filter((t) => t.minPhase <= state.phase);
  if (eligibleTypes.length === 0) return state;

  const activeTypes = new Set(state.activeAnomalies.map((a) => a.type));
  const preferredPool = eligibleTypes.filter((t) => !activeTypes.has(t.id));
  const pool = preferredPool.length > 0 ? preferredPool : eligibleTypes;
  const id = state.nextAnomalyId;
  const chosenType = pool[(id - 1) % pool.length];

  const usedKeys = new Set(state.activeAnomalies.map((a) => `${a.xFraction},${a.yFraction}`));
  const freeAnchors = ANOMALY_ANCHORS.filter(([x, y]) => !usedKeys.has(`${x},${y}`));
  const anchors = freeAnchors.length > 0 ? freeAnchors : ANOMALY_ANCHORS;
  const [xFraction, yFraction] = anchors[(id - 1) % anchors.length];

  return {
    ...state,
    activeAnomalies: [
      ...state.activeAnomalies,
      { id, type: chosenType.id, xFraction, yFraction },
    ],
    nextAnomalyId: id + 1,
    secondsUntilNextAnomaly: ORGAN_BALANCE.ANOMALY_SPAWN_INTERVAL_SEC,
  };
}

export function pressValve(state, catalog) {
  const gained = manualTapYield(state);
  const updated = {
    ...state,
    ichor: state.ichor + gained,
    cycleLifetimeIchor: state.cycleLifetimeIchor + gained,
    secondsSinceLastTap: 0.0,
    isStillnessActive: false,
  };
  return finalizeProgression(updated, catalog);
}

export function tick(state, deltaSeconds, catalog) {
  if (deltaSeconds <= 0.0) return state;

  const nextIdleSec = state.secondsSinceLastTap + deltaSeconds;
  const hasLamp = creatureCount(state, CREATURE_TYPES.WEEPING_LAMP.id) > 0;
  const stillnessNowActive = hasLamp && nextIdleSec >= ORGAN_BALANCE.STILLNESS_IDLE_DELAY_SEC;

  const preProduction = { ...state, isStillnessActive: stillnessNowActive };
  const gainedIchor = ichorPerSecond(preProduction) * deltaSeconds;
  const gainedStillness = stillnessPerSecond(preProduction) * deltaSeconds;
  const nextGaze = Math.max(0.0, state.gazeRemainingSeconds - deltaSeconds);

  let nextState = {
    ...preProduction,
    ichor: state.ichor + gainedIchor,
    cycleLifetimeIchor: state.cycleLifetimeIchor + gainedIchor,
    stillness: state.stillness + gainedStillness,
    cycleLifetimeStillness: state.cycleLifetimeStillness + gainedStillness,
    secondsSinceLastTap: nextIdleSec,
    gazeRemainingSeconds: nextGaze,
  };

  nextState = finalizeProgression(nextState, catalog);
  nextState = advanceAnomalySpawner(nextState, deltaSeconds);
  return nextState;
}

export function buyCreature(state, typeId, catalog) {
  const spec = typeof typeId === 'string' ? creatureById(typeId) : typeId;
  if (!spec) return state;
  const cost = creatureCost(state, spec.id);
  if (state.ichor < cost) return state;

  const updatedCreatures = {
    ...state.creatures,
    [spec.id]: creatureCount(state, spec.id) + 1,
  };
  return finalizeProgression({ ...state, ichor: state.ichor - cost, creatures: updatedCreatures }, catalog);
}

export function bottleJar(state, jarId, catalog) {
  const jar = typeof jarId === 'string' ? jarById(jarId) : jarId;
  if (!jar || state.ownedJars.includes(jar.id) || state.stillness < jar.stillnessCost) {
    return state;
  }
  return finalizeProgression(
    {
      ...state,
      stillness: state.stillness - jar.stillnessCost,
      ownedJars: [...state.ownedJars, jar.id],
    },
    catalog,
  );
}

export function performGraft(state, catalog) {
  if (!canAffordGraft(state)) return state;

  const updatedCreatures = {
    ...state.creatures,
    [CREATURE_TYPES.SUTURE_HAND.id]:
      creatureCount(state, CREATURE_TYPES.SUTURE_HAND.id) - ORGAN_BALANCE.GRAFT_SUTURE_COST,
    [CREATURE_TYPES.LEECH_CLERK.id]:
      creatureCount(state, CREATURE_TYPES.LEECH_CLERK.id) - ORGAN_BALANCE.GRAFT_LEECH_COST,
  };
  return finalizeProgression(
    {
      ...state,
      creatures: updatedCreatures,
      graftedTendrils: state.graftedTendrils + 1,
    },
    catalog,
  );
}

export function interactAnomaly(state, anomalyId, catalog) {
  const anomaly = state.activeAnomalies.find((a) => a.id === anomalyId);
  if (!anomaly) return state;

  const remaining = state.activeAnomalies.filter((a) => a.id !== anomalyId);
  const jarBoost = state.ownedJars.includes(SPECIMEN_JARS.ALMOST_A_NAME.id)
    ? ORGAN_BALANCE.ALMOST_A_NAME_MULTIPLIER
    : 1.0;

  const updatedCounts = {
    ...state.anomalyInteractions,
    [anomaly.type]: (state.anomalyInteractions?.[anomaly.type] ?? 0) + 1,
  };

  let rewarded = state;
  if (anomaly.type === WALL_ANOMALY_TYPES.PALE_TOOTH.id) {
    const bonus = ORGAN_BALANCE.TOOTH_BASE_ICHOR_REWARD * scarMultiplier(state) * jarBoost;
    rewarded = {
      ...state,
      ichor: state.ichor + bonus,
      cycleLifetimeIchor: state.cycleLifetimeIchor + bonus,
      activeAnomalies: remaining,
      anomalyInteractions: updatedCounts,
    };
  } else if (anomaly.type === WALL_ANOMALY_TYPES.GLASS_EYE.id) {
    rewarded = {
      ...state,
      gazeRemainingSeconds: ORGAN_BALANCE.GAZE_DURATION_SEC,
      activeAnomalies: remaining,
      anomalyInteractions: updatedCounts,
    };
  } else if (anomaly.type === WALL_ANOMALY_TYPES.SWOLLEN_NODULE.id) {
    const ichorBonus = ORGAN_BALANCE.NODULE_BASE_ICHOR_REWARD * scarMultiplier(state) * jarBoost;
    const stillnessBonus =
      ORGAN_BALANCE.NODULE_BASE_STILLNESS_REWARD * scarStillnessMultiplier(state) * jarBoost;
    rewarded = {
      ...state,
      ichor: state.ichor + ichorBonus,
      cycleLifetimeIchor: state.cycleLifetimeIchor + ichorBonus,
      stillness: state.stillness + stillnessBonus,
      cycleLifetimeStillness: state.cycleLifetimeStillness + stillnessBonus,
      activeAnomalies: remaining,
      anomalyInteractions: updatedCounts,
    };
  }

  return finalizeProgression(rewarded, catalog);
}

export function performExcision(state, catalog) {
  if (!canExcise(state)) return state;
  const gainedScars = pendingScarTissue(state);
  const reset = {
    ...initialState(catalog),
    scarTissue: state.scarTissue + gainedScars,
    cycle: state.cycle + 1,
    nextAnomalyId: state.nextAnomalyId,
    activeDialogue: state.activeDialogue,
    discoveredDialogues: state.discoveredDialogues,
    triggeredDialogueIds: state.triggeredDialogueIds,
  };
  return finalizeProgression(reset, catalog);
}

export function serializeState(state) {
  return JSON.stringify({
    version: 1,
    ichor: state.ichor,
    cycleLifetimeIchor: state.cycleLifetimeIchor,
    stillness: state.stillness,
    cycleLifetimeStillness: state.cycleLifetimeStillness,
    scarTissue: state.scarTissue,
    cycle: state.cycle,
    creatures: state.creatures,
    graftedTendrils: state.graftedTendrils,
    ownedJars: state.ownedJars,
    activeAnomalies: state.activeAnomalies,
    anomalyInteractions: state.anomalyInteractions,
    secondsSinceLastTap: state.secondsSinceLastTap,
    isStillnessActive: state.isStillnessActive,
    gazeRemainingSeconds: state.gazeRemainingSeconds,
    secondsUntilNextAnomaly: state.secondsUntilNextAnomaly,
    nextAnomalyId: state.nextAnomalyId,
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

    const restored = {
      ...initialState(catalog),
      ichor: Math.max(0, Number(data.ichor) || 0),
      cycleLifetimeIchor: Math.max(0, Number(data.cycleLifetimeIchor) || 0),
      stillness: Math.max(0, Number(data.stillness) || 0),
      cycleLifetimeStillness: Math.max(0, Number(data.cycleLifetimeStillness) || 0),
      scarTissue: Math.max(0, Math.floor(Number(data.scarTissue) || 0)),
      cycle: Math.max(0, Math.floor(Number(data.cycle) || 0)),
      creatures: data.creatures && typeof data.creatures === 'object' ? { ...data.creatures } : {},
      graftedTendrils: Math.max(0, Math.floor(Number(data.graftedTendrils) || 0)),
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

export function formatInteger(value) {
  return Math.floor(value).toLocaleString('en-US');
}

export function formatCompactDecimal(value) {
  const rounded = Math.round(value * 10) / 10;
  if (rounded === Math.trunc(rounded)) {
    return Math.trunc(rounded).toLocaleString('en-US');
  }
  return rounded.toLocaleString('en-US', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
}

export function formatMultiplier(value) {
  const rounded = Math.round(value * 100) / 100;
  return rounded.toLocaleString('en-US', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
    useGrouping: false,
  });
}
