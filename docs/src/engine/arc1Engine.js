/**
 * Arc 1 Chamber state transitions, anomaly spawning, Chamber Rites, Scar Tree, and Excision actions.
 */

import {
  ANOMALY_ANCHORS,
  CREATURE_TYPES,
  GAME_ARCS,
  ORGAN_BALANCE,
  SPECIMEN_JARS,
  WALL_ANOMALY_LIST,
  WALL_ANOMALY_TYPES,
  creatureById,
  dossierLineById,
  jarById,
  repeatableGraftById,
  scarNodeById,
} from './constants.js?v=3.14';
import {
  anomalyRewardMultiplier,
  canAffordChoirLung,
  canAffordGraft,
  canBreachToArc2,
  canBuyRepeatableGraft,
  canDredgeSump,
  canExcise,
  canOfferToThroat,
  canUnlockNode,
  choirLungCooldownDurationSec,
  choirLungStillnessCost,
  creatureCost,
  creatureCount,
  cycleLifetimeIchorDisplay,
  dossierIchorMultiplier,
  echoPulseIntervalSec,
  effectiveJarCost,
  graftCooldownDurationSec,
  graftStillnessCost,
  hasNode,
  ichorPerSecond,
  isColonyProliferationUnlocked,
  isCreatureUnlocked,
  isGraftUnlocked,
  isJarUnlocked,
  isValveClamped,
  manualTapYield,
  marrowBloomDurationSec,
  maxAffordableCreatureQuote,
  nextDredgeableRelic,
  nextExcisionMilestone,
  pendingScarTissue,
  rawIchorPerSecond,
  relicIchorMultiplier,
  relicStillnessMultiplier,
  repeatableCost,
  repeatableRank,
  scarMultiplier,
  scarStillnessMultiplier,
  stillnessIdleDelaySec,
  stillnessPerSecond,
  valveClampDurationSec,
} from './arc1Math.js?v=3.14';
import { finalizeProgression, initialState } from './progression.js?v=3.14';

export function spawnImmediateWallAnomaly(state, allowOverflow = false) {
  const maxAnomalies = allowOverflow
    ? 5
    : hasNode(state, 'softened_mortar')
      ? 4
      : ORGAN_BALANCE.MAX_ACTIVE_ANOMALIES;
  if (state.activeAnomalies.length >= maxAnomalies) return state;

  const effectivePhase = Math.max(1, state.phase);
  const eligibleTypes = WALL_ANOMALY_LIST.filter((t) => t.minPhase <= effectivePhase);
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

  const spawnInterval = hasNode(state, 'softened_mortar')
    ? ORGAN_BALANCE.ANOMALY_SPAWN_INTERVAL_SEC / ORGAN_BALANCE.SOFTENED_MORTAR_SPEED_MULT
    : ORGAN_BALANCE.ANOMALY_SPAWN_INTERVAL_SEC;

  return {
    ...state,
    activeAnomalies: [
      ...state.activeAnomalies,
      { id, type: chosenType.id, xFraction, yFraction },
    ],
    nextAnomalyId: id + 1,
    secondsUntilNextAnomaly: spawnInterval,
  };
}

function advanceAnomalySpawner(state, deltaSeconds) {
  const maxAnomalies = hasNode(state, 'softened_mortar')
    ? 4
    : ORGAN_BALANCE.MAX_ACTIVE_ANOMALIES;
  if (state.phase < 1 || state.activeAnomalies.length >= maxAnomalies) {
    return state;
  }
  const remaining = state.secondsUntilNextAnomaly - deltaSeconds;
  if (remaining > 0.0) {
    return { ...state, secondsUntilNextAnomaly: remaining };
  }
  return spawnImmediateWallAnomaly(state, false);
}

function triggerMarrowBloom(state) {
  let bloomed = {
    ...state,
    marrowBloomRemainingSeconds: marrowBloomDurationSec(state),
    marrowBloomCount: state.marrowBloomCount + 1,
  };
  for (let i = 0; i < 4; i += 1) {
    bloomed = spawnImmediateWallAnomaly(bloomed, true);
  }
  return bloomed;
}

export function pressValve(state, catalog) {
  if (state.activeArc !== GAME_ARCS.ARC_1_CHAMBER || isValveClamped(state)) return state;
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

export function tickArc1(state, deltaSeconds, catalog) {
  const nextIdleSec = state.secondsSinceLastTap + deltaSeconds;
  const hasLamp = creatureCount(state, CREATURE_TYPES.WEEPING_LAMP.id) > 0;
  const stillnessNowActive = hasLamp && nextIdleSec >= stillnessIdleDelaySec(state);

  const preProduction =
    stillnessNowActive === state.isStillnessActive
      ? state
      : { ...state, isStillnessActive: stillnessNowActive };
  const gainedIchor = ichorPerSecond(preProduction) * deltaSeconds;
  const gainedStillness = stillnessPerSecond(preProduction) * deltaSeconds;
  const nextGaze = Math.max(0.0, state.gazeRemainingSeconds - deltaSeconds);
  const nextBloom = Math.max(0.0, state.marrowBloomRemainingSeconds - deltaSeconds);
  const nextGraftCooldown = Math.max(
    0.0,
    (state.graftCooldownRemainingSeconds ?? 0.0) - deltaSeconds,
  );
  const nextLungCooldown = Math.max(
    0.0,
    (state.choirLungCooldownRemainingSeconds ?? 0.0) - deltaSeconds,
  );

  // Suture Tier 5: Perpetual Seam auto-ratchets valve turns without breaking Stillness
  const autoValveActive = hasNode(state, 'perpetual_seam') && !isValveClamped(state);
  const autoValveCharge = autoValveActive
    ? (state.autoValveChargeSeconds ?? 0.0) + deltaSeconds
    : (state.autoValveChargeSeconds ?? 0.0);
  const autoValveTurns = autoValveActive
    ? Math.floor(autoValveCharge / ORGAN_BALANCE.PERPETUAL_SEAM_INTERVAL_SEC)
    : 0;
  const nextAutoValveCharge = autoValveActive
    ? autoValveCharge - autoValveTurns * ORGAN_BALANCE.PERPETUAL_SEAM_INTERVAL_SEC
    : autoValveCharge;
  const autoValveIchor = autoValveTurns > 0 ? autoValveTurns * manualTapYield(preProduction) : 0.0;

  const ossuarySpeed = hasNode(state, 'echo_ossuary') ? 1.45 : 1.0;
  const nextDrainPressure =
    state.highestExcisionMilestone >= 2 && state.drainSealed
      ? Math.min(
          100.0,
          state.drainPressure +
            (4.0 + creatureCount(state, CREATURE_TYPES.SUMP_SIPHON.id) * 1.5) *
              ossuarySpeed *
              deltaSeconds,
        )
      : Math.max(0.0, state.drainPressure - 2.0 * deltaSeconds);

  const echoInterval = echoPulseIntervalSec(state);
  const echoActive = state.highestExcisionMilestone >= 3 && state.echoDiversionPercent > 0;
  const chargedEcho = echoActive ? state.echoChargeSeconds + deltaSeconds : state.echoChargeSeconds;
  const echoPulseReady = echoActive && chargedEcho >= echoInterval;

  const wasClamped = state.clampRemainingSeconds > 0.0;
  const nextClamp = wasClamped ? Math.max(0.0, state.clampRemainingSeconds - deltaSeconds) : 0.0;

  const totalCycleIchorGain = gainedIchor + autoValveIchor;
  let nextState = {
    ...preProduction,
    ichor: state.ichor + totalCycleIchorGain,
    cycleLifetimeIchor: state.cycleLifetimeIchor + totalCycleIchorGain,
    stillness: state.stillness + gainedStillness,
    cycleLifetimeStillness: state.cycleLifetimeStillness + gainedStillness,
    secondsSinceLastTap: nextIdleSec,
    gazeRemainingSeconds: nextGaze,
    marrowBloomRemainingSeconds: nextBloom,
    graftCooldownRemainingSeconds: nextGraftCooldown,
    choirLungCooldownRemainingSeconds: nextLungCooldown,
    autoValveChargeSeconds: nextAutoValveCharge,
    drainPressure: nextDrainPressure,
    echoChargeSeconds: echoPulseReady ? state.echoChargeSeconds : chargedEcho,
    clampRemainingSeconds: nextClamp,
  };

  // Excision III: Neighbor Aperture Echo bursts
  if (echoPulseReady) {
    const baseEchoMultiplier =
      nextState.echoDiversionPercent === 25
        ? 10.5
        : nextState.echoDiversionPercent === 50
          ? 18.0
          : 0.0;
    const echoYieldBoost =
      (hasNode(nextState, 'echo_ossuary') ? 1.35 : 1.0) *
      (hasNode(nextState, 'bone_resonance') ? 1.4 : 1.0);
    const echoBurst = Math.max(
      250.0,
      rawIchorPerSecond(nextState) * baseEchoMultiplier * echoYieldBoost,
    );
    nextState = {
      ...nextState,
      ichor: nextState.ichor + echoBurst,
      cycleLifetimeIchor: nextState.cycleLifetimeIchor + echoBurst,
      echoChargeSeconds: chargedEcho - echoInterval,
      echoPulsesTriggered: nextState.echoPulsesTriggered + 1,
    };
  }

  // Excision IV: Valve Clamp countdown -> Marrow Bloom
  if (wasClamped && nextClamp === 0.0) {
    nextState = triggerMarrowBloom(nextState);
  }

  // Surgical Tree (Marrow Tier 2): Auto Tendril Suture
  if (
    hasNode(nextState, 'auto_tendril_suture') &&
    isGraftUnlocked(nextState) &&
    creatureCount(nextState, CREATURE_TYPES.SUTURE_HAND.id) >= 4 &&
    creatureCount(nextState, CREATURE_TYPES.LEECH_CLERK.id) >= 4
  ) {
    nextState = performGraft(nextState, catalog);
  }

  nextState = finalizeProgression(nextState, catalog);
  nextState = advanceAnomalySpawner(nextState, deltaSeconds);
  return nextState;
}

export function buyCreature(state, typeId, catalog) {
  const spec = typeof typeId === 'string' ? creatureById(typeId) : typeId;
  if (!spec || !isCreatureUnlocked(state, spec)) return state;
  const cost = creatureCost(state, spec.id);
  if (state.ichor < cost) return state;

  const updatedCreatures = {
    ...state.creatures,
    [spec.id]: creatureCount(state, spec.id) + 1,
  };
  return finalizeProgression({ ...state, ichor: state.ichor - cost, creatures: updatedCreatures }, catalog);
}

export function buyMaxCreature(state, typeId, catalog) {
  const spec = typeof typeId === 'string' ? creatureById(typeId) : typeId;
  if (!spec || !isColonyProliferationUnlocked(state) || !isCreatureUnlocked(state, spec)) {
    return state;
  }
  const quote = maxAffordableCreatureQuote(state, spec);
  if (quote.count <= 0) return state;

  const updatedCreatures = {
    ...state.creatures,
    [spec.id]: creatureCount(state, spec.id) + quote.count,
  };
  return finalizeProgression(
    {
      ...state,
      ichor: Math.max(0.0, state.ichor - quote.totalCost),
      creatures: updatedCreatures,
    },
    catalog,
  );
}

export function bottleJar(state, jarId, catalog) {
  const jar = typeof jarId === 'string' ? jarById(jarId) : jarId;
  if (!jar || !isJarUnlocked(state, jar) || state.ownedJars.includes(jar.id)) {
    return state;
  }
  const cost = effectiveJarCost(state, jar);
  if (state.stillness < cost) {
    return state;
  }
  return finalizeProgression(
    {
      ...state,
      stillness: state.stillness - cost,
      ownedJars: [...state.ownedJars, jar.id],
    },
    catalog,
  );
}

export function performGraft(state, catalog) {
  if (!canAffordGraft(state)) return state;
  const stillnessCost = graftStillnessCost(state);
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
      stillness: Math.max(0.0, state.stillness - stillnessCost),
      creatures: updatedCreatures,
      graftedTendrils: state.graftedTendrils + 1,
      graftCooldownRemainingSeconds: graftCooldownDurationSec(state),
      lastSacrificePulse: (state.lastSacrificePulse ?? 0) + 1,
      lastSacrificeKind: 'tendril',
    },
    catalog,
  );
}

export function performChoirLungGraft(state, catalog) {
  if (!canAffordChoirLung(state)) return state;
  const stillnessCost = choirLungStillnessCost(state);
  const updatedCreatures = {
    ...state.creatures,
    [CREATURE_TYPES.WEEPING_LAMP.id]:
      creatureCount(state, CREATURE_TYPES.WEEPING_LAMP.id) - ORGAN_BALANCE.CHOIR_LUNG_LAMP_COST,
  };
  return finalizeProgression(
    {
      ...state,
      stillness: Math.max(0.0, state.stillness - stillnessCost),
      creatures: updatedCreatures,
      graftedTendrils: state.graftedTendrils - ORGAN_BALANCE.CHOIR_LUNG_TENDRIL_COST,
      choirLungs: state.choirLungs + 1,
      choirLungCooldownRemainingSeconds: choirLungCooldownDurationSec(state),
      lastSacrificePulse: (state.lastSacrificePulse ?? 0) + 1,
      lastSacrificeKind: 'choir_lung',
    },
    catalog,
  );
}

export function unlockScarNode(state, nodeId, catalog) {
  const node = typeof nodeId === 'string' ? scarNodeById(nodeId) : nodeId;
  if (!node || !canUnlockNode(state, node)) return state;
  return finalizeProgression(
    {
      ...state,
      availableScarTissue: state.availableScarTissue - node.cost,
      unlockedScarNodes: [...state.unlockedScarNodes, node.id],
    },
    catalog,
  );
}

export function buyRepeatableScarGraft(state, graftId, catalog) {
  const graft = typeof graftId === 'string' ? repeatableGraftById(graftId) : graftId;
  if (!graft || !canBuyRepeatableGraft(state, graft)) return state;
  const cost = repeatableCost(state, graft);
  return finalizeProgression(
    {
      ...state,
      availableScarTissue: state.availableScarTissue - cost,
      repeatableScarRanks: {
        ...(state.repeatableScarRanks ?? {}),
        [graft.id]: repeatableRank(state, graft.id) + 1,
      },
    },
    catalog,
  );
}

export function toggleDrainSeal(state, catalog) {
  if (state.highestExcisionMilestone < 2) return state;
  return finalizeProgression({ ...state, drainSealed: !state.drainSealed }, catalog);
}

export function dredgeSump(state, catalog) {
  const nextRelic = nextDredgeableRelic(state);
  if (!nextRelic || !canDredgeSump(state)) return state;
  const burstIchor = Math.max(25000.0, rawIchorPerSecond(state) * 15.0);
  return finalizeProgression(
    {
      ...state,
      ichor: state.ichor + burstIchor,
      cycleLifetimeIchor: state.cycleLifetimeIchor + burstIchor,
      drainPressure: 0.0,
      drainSealed: false,
      dredgedRelics: [...state.dredgedRelics, nextRelic.id],
      lifetimeRelicsDredged: state.lifetimeRelicsDredged + 1,
    },
    catalog,
  );
}

export function setEchoDiversion(state, percent, catalog) {
  if (state.highestExcisionMilestone < 3) return state;
  const clamped = percent === 25 || percent === 50 ? percent : 0;
  return finalizeProgression({ ...state, echoDiversionPercent: clamped }, catalog);
}

export function activateValveClamp(state, catalog) {
  if (state.highestExcisionMilestone < 4 || isValveClamped(state)) return state;
  return finalizeProgression(
    { ...state, clampRemainingSeconds: valveClampDurationSec(state) },
    catalog,
  );
}

export function offerToThroat(state, lineId, catalog) {
  const line = typeof lineId === 'string' ? dossierLineById(lineId) : lineId;
  if (!line || !canOfferToThroat(state, line)) return state;
  const updatedCreatures = {
    ...state.creatures,
    [CREATURE_TYPES.SUMP_SIPHON.id]:
      creatureCount(state, CREATURE_TYPES.SUMP_SIPHON.id) - line.siphonCost,
  };
  const nextJars =
    line.requiresJar && !hasNode(state, 'wax_sealed_memory')
      ? state.ownedJars.slice(1)
      : state.ownedJars;
  return finalizeProgression(
    {
      ...state,
      creatures: updatedCreatures,
      graftedTendrils: state.graftedTendrils - line.tendrilCost,
      ownedJars: nextJars,
      unredactedDossierLines: [...state.unredactedDossierLines, line.id],
    },
    catalog,
  );
}

export function interactAnomaly(state, anomalyId, catalog) {
  const anomaly = state.activeAnomalies.find((a) => a.id === anomalyId);
  if (!anomaly) return state;

  const remaining = state.activeAnomalies.filter((a) => a.id !== anomalyId);
  const rewardBoost = anomalyRewardMultiplier(state);
  const ichorBaseMult =
    scarMultiplier(state) * relicIchorMultiplier(state) * dossierIchorMultiplier(state) * rewardBoost;
  const stillBaseMult =
    scarStillnessMultiplier(state) * relicStillnessMultiplier(state) * rewardBoost;

  const updatedCounts = {
    ...state.anomalyInteractions,
    [anomaly.type]: (state.anomalyInteractions?.[anomaly.type] ?? 0) + 1,
  };

  let rewarded = state;
  if (anomaly.type === WALL_ANOMALY_TYPES.PALE_TOOTH.id) {
    const bonus = ORGAN_BALANCE.TOOTH_BASE_ICHOR_REWARD * ichorBaseMult;
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
    const ichorBonus = ORGAN_BALANCE.NODULE_BASE_ICHOR_REWARD * ichorBaseMult;
    const stillnessBonus = ORGAN_BALANCE.NODULE_BASE_STILLNESS_REWARD * stillBaseMult;
    rewarded = {
      ...state,
      ichor: state.ichor + ichorBonus,
      cycleLifetimeIchor: state.cycleLifetimeIchor + ichorBonus,
      stillness: state.stillness + stillnessBonus,
      cycleLifetimeStillness: state.cycleLifetimeStillness + stillnessBonus,
      activeAnomalies: remaining,
      anomalyInteractions: updatedCounts,
    };
  } else if (anomaly.type === WALL_ANOMALY_TYPES.CHITIN_RIB.id) {
    const ichorBonus = ORGAN_BALANCE.RIB_BASE_ICHOR_REWARD * ichorBaseMult;
    const stillnessBonus = ORGAN_BALANCE.RIB_BASE_STILLNESS_REWARD * stillBaseMult;
    rewarded = {
      ...state,
      ichor: state.ichor + ichorBonus,
      cycleLifetimeIchor: state.cycleLifetimeIchor + ichorBonus,
      stillness: state.stillness + stillnessBonus,
      cycleLifetimeStillness: state.cycleLifetimeStillness + stillnessBonus,
      gazeRemainingSeconds: Math.max(
        state.gazeRemainingSeconds,
        ORGAN_BALANCE.RIB_GAZE_DURATION_SEC,
      ),
      activeAnomalies: remaining,
      anomalyInteractions: updatedCounts,
    };
  }

  return finalizeProgression(rewarded, catalog);
}

export function performExcision(state, catalog) {
  if (!canExcise(state)) return state;
  const gainedScars = pendingScarTissue(state);
  // Strict anti-skip sequential lock: advance by at most +1 Excision Milestone per Excision
  const nextMilestone = nextExcisionMilestone(state);
  const newHighest =
    nextMilestone && cycleLifetimeIchorDisplay(state) >= nextMilestone.massThreshold
      ? nextMilestone.number
      : state.highestExcisionMilestone;

  const startingCreatures = {};
  if (hasNode(state, 'calloused_palm')) {
    startingCreatures.suture_hand = 3;
    startingCreatures.leech_clerk = 1;
  }
  if (hasNode(state, 'membrane_recall')) {
    startingCreatures.weeping_lamp = 2;
    if (newHighest >= 2) {
      startingCreatures.sump_siphon = 2;
    }
  }
  const startingTendrils = hasNode(state, 'membrane_recall') ? 1 : 0;
  const retainedRelics = hasNode(state, 'hollow_lung_choir') ? [...(state.dredgedRelics ?? [])] : [];
  const foundationalJars = new Set([
    SPECIMEN_JARS.YESTERDAYS_DUST.id,
    SPECIMEN_JARS.UNSPENT_BREATH.id,
    SPECIMEN_JARS.ALMOST_A_NAME.id,
  ]);
  const retainedJars = hasNode(state, 'catatonic_tide')
    ? (state.ownedJars ?? []).filter((id) => foundationalJars.has(id))
    : [];

  const reset = {
    ...initialState(catalog),
    activeArc: GAME_ARCS.ARC_1_CHAMBER,
    scarTissue: state.scarTissue + gainedScars,
    availableScarTissue: state.availableScarTissue + gainedScars,
    highestExcisionMilestone: newHighest,
    unlockedScarNodes: [...state.unlockedScarNodes],
    repeatableScarRanks: { ...(state.repeatableScarRanks ?? {}) },
    cycle: state.cycle + 1,
    creatures: startingCreatures,
    graftedTendrils: startingTendrils,
    ownedJars: retainedJars,
    dredgedRelics: retainedRelics,
    autoValveChargeSeconds: 0.0,
    nextAnomalyId: state.nextAnomalyId,
    lifetimeRelicsDredged: state.lifetimeRelicsDredged,
    echoPulsesTriggered: state.echoPulsesTriggered,
    marrowBloomCount: state.marrowBloomCount,
    unredactedDossierLines: [...state.unredactedDossierLines],
    arc2: state.arc2,
    activeDialogue: state.activeDialogue,
    discoveredDialogues: state.discoveredDialogues,
    triggeredDialogueIds: state.triggeredDialogueIds,
  };
  return finalizeProgression(reset, catalog);
}

export function performBreachToArc2(state, catalog) {
  if (!canBreachToArc2(state)) return state;
  const breached = {
    ...state,
    activeArc: GAME_ARCS.ARC_2_INVESTIGATION,
    highestExcisionMilestone: Math.max(state.highestExcisionMilestone, 6),
  };
  return finalizeProgression(breached, catalog);
}
