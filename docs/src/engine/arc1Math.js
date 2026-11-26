/**
 * Pure Arc 1 Chamber derived selectors, multipliers, cost curves, and Excision Scar harvest math.
 */

import {
  CREATURE_LIST,
  CREATURE_TYPES,
  CULT_DOSSIER_LINES,
  DREDGED_RELICS,
  EXCISION_MILESTONES,
  ORGAN_BALANCE,
  SPECIMEN_JARS,
  creatureById,
  dossierLineById,
  jarById,
  repeatableGraftById,
  scarNodeById,
} from './constants.js?v=3.12';

export function hasNode(state, nodeId) {
  return Array.isArray(state.unlockedScarNodes) && state.unlockedScarNodes.includes(nodeId);
}

export function canUnlockNode(state, nodeId) {
  const node = typeof nodeId === 'string' ? scarNodeById(nodeId) : nodeId;
  if (!node || hasNode(state, node.id)) return false;
  const prereqOk = node.prerequisite == null || hasNode(state, node.prerequisite);
  return prereqOk && state.availableScarTissue >= node.cost;
}

export function repeatableRank(state, graftOrId) {
  const id = typeof graftOrId === 'string' ? graftOrId : graftOrId?.id;
  return state.repeatableScarRanks?.[id] ?? 0;
}

export function repeatableCost(state, graftOrId) {
  const graft = typeof graftOrId === 'string' ? repeatableGraftById(graftOrId) : graftOrId;
  if (!graft) return 0;
  return Math.max(1, Math.floor(graft.baseCost * Math.pow(graft.costGrowth, repeatableRank(state, graft.id))));
}

export function isRepeatableGraftsUnlocked(state) {
  return hasNode(state, 'first_incision');
}

export function canBuyRepeatableGraft(state, graftOrId) {
  const graft = typeof graftOrId === 'string' ? repeatableGraftById(graftOrId) : graftOrId;
  if (!graft || !isRepeatableGraftsUnlocked(state)) return false;
  return state.availableScarTissue >= repeatableCost(state, graft);
}

export function creatureCount(state, typeId) {
  return state.creatures?.[typeId] ?? 0;
}

export function isCreatureUnlocked(state, typeId) {
  const spec = typeof typeId === 'string' ? creatureById(typeId) : typeId;
  if (!spec) return false;
  if (state.highestExcisionMilestone < spec.requiredMilestone) return false;
  return (
    state.cycle > 0 ||
    cycleLifetimeIchorDisplay(state) >= spec.unlockIchorThreshold ||
    creatureCount(state, spec.id) > 0
  );
}

function effectiveCreatureGrowth(state, spec) {
  return hasNode(state, 'colony_inertia')
    ? Math.max(1.14, spec.costGrowth - 0.04)
    : spec.costGrowth;
}

export function creatureCost(state, typeId) {
  const spec = typeof typeId === 'string' ? creatureById(typeId) : typeId;
  if (!spec) return 0;
  const owned = creatureCount(state, spec.id);
  return Math.floor(spec.baseCost * Math.pow(effectiveCreatureGrowth(state, spec), owned));
}

export function isColonyProliferationUnlocked(state) {
  return state.highestExcisionMilestone >= 3;
}

export function maxAffordableCreatureQuote(state, typeId) {
  const spec = typeof typeId === 'string' ? creatureById(typeId) : typeId;
  if (!spec || !isCreatureUnlocked(state, spec)) {
    return { count: 0, totalCost: 0 };
  }
  let remaining = state.ichor;
  let owned = creatureCount(state, spec.id);
  const growth = effectiveCreatureGrowth(state, spec);
  let bought = 0;
  let totalSpent = 0;
  while (bought < 500) {
    const nextCost = Math.max(1, Math.floor(spec.baseCost * Math.pow(growth, owned)));
    if (remaining < nextCost) break;
    remaining -= nextCost;
    totalSpent += nextCost;
    owned += 1;
    bought += 1;
  }
  return { count: bought, totalCost: totalSpent };
}

export function stillnessIdleDelaySec(state) {
  if (hasNode(state, 'catatonic_tide')) return ORGAN_BALANCE.CATATONIC_TIDE_DELAY_SEC;
  if (hasNode(state, 'shorter_silence')) return ORGAN_BALANCE.SHORTER_SILENCE_DELAY_SEC;
  return ORGAN_BALANCE.STILLNESS_IDLE_DELAY_SEC;
}

export function jarEffectBoost(state) {
  return (
    1.0 +
    (hasNode(state, 'wax_sealed_memory') ? 0.35 : 0.0) +
    (hasNode(state, 'catatonic_tide') ? 0.65 : 0.0)
  );
}

export function sedimentCostMultiplier(state) {
  return Math.max(
    ORGAN_BALANCE.SEDIMENT_WELL_COST_FLOOR,
    Math.pow(ORGAN_BALANCE.SEDIMENT_WELL_COST_FACTOR, repeatableRank(state, 'sediment_well')),
  );
}

export function stillnessCostMultiplier(state) {
  const waxMult = hasNode(state, 'wax_sealed_memory')
    ? ORGAN_BALANCE.WAX_STILLNESS_COST_MULTIPLIER
    : 1.0;
  return waxMult * sedimentCostMultiplier(state);
}

export function graftCooldownDurationSec(state) {
  return (
    ORGAN_BALANCE.GRAFT_COOLDOWN_SEC *
    (hasNode(state, 'wax_sealed_memory') ? ORGAN_BALANCE.WAX_GRAFT_COOLDOWN_MULTIPLIER : 1.0)
  );
}

export function choirLungCooldownDurationSec(state) {
  return (
    ORGAN_BALANCE.CHOIR_LUNG_COOLDOWN_SEC *
    (hasNode(state, 'wax_sealed_memory') ? ORGAN_BALANCE.WAX_GRAFT_COOLDOWN_MULTIPLIER : 1.0)
  );
}

export function echoPulseIntervalSec(state) {
  return hasNode(state, 'echo_ossuary')
    ? ORGAN_BALANCE.ECHO_OSSUARY_INTERVAL_SEC
    : ORGAN_BALANCE.ECHO_PULSE_INTERVAL_SEC;
}

export function valveClampDurationSec(state) {
  return hasNode(state, 'bloom_incision')
    ? ORGAN_BALANCE.BLOOM_INCISION_CLAMP_DURATION_SEC
    : ORGAN_BALANCE.VALVE_CLAMP_DURATION_SEC;
}

export function marrowBloomDurationSec(state) {
  return hasNode(state, 'bloom_incision')
    ? ORGAN_BALANCE.BLOOM_INCISION_DURATION_SEC
    : ORGAN_BALANCE.MARROW_BLOOM_DURATION_SEC;
}

export function isJarUnlocked(state, jarOrId) {
  const jar = typeof jarOrId === 'string' ? jarById(jarOrId) : jarOrId;
  if (!jar) return false;
  if (state.ownedJars.includes(jar.id)) return true;
  if (jar.requiresCycleOrMilestone) {
    return state.cycle > 0 || state.highestExcisionMilestone >= 1 || state.scarTissue > 0;
  }
  return state.cycle > 0 || state.phase >= (jar.unlockPhase ?? 0);
}

export function effectiveJarCost(state, jarOrId) {
  const jar = typeof jarOrId === 'string' ? jarById(jarOrId) : jarOrId;
  if (!jar) return 0;
  return Math.max(1.0, Math.floor(jar.stillnessCost * stillnessCostMultiplier(state)));
}

export function graftStillnessCost(state) {
  return Math.max(
    1,
    Math.floor(
      ORGAN_BALANCE.GRAFT_BASE_STILLNESS_COST *
        Math.pow(ORGAN_BALANCE.GRAFT_STILLNESS_GROWTH, state.graftedTendrils) *
        stillnessCostMultiplier(state),
    ),
  );
}

export function choirLungStillnessCost(state) {
  return Math.max(
    1,
    Math.floor(
      ORGAN_BALANCE.CHOIR_LUNG_BASE_STILLNESS_COST *
        Math.pow(ORGAN_BALANCE.CHOIR_LUNG_STILLNESS_GROWTH, state.choirLungs) *
        stillnessCostMultiplier(state),
    ),
  );
}

export function groutSaltOrganismMultiplier(state) {
  return state.ownedJars.includes(SPECIMEN_JARS.SALT_FROM_THE_GROUT.id)
    ? 1.0 + ORGAN_BALANCE.SALT_GROUT_BONUS * jarEffectBoost(state)
    : 1.0;
}

export function borrowedPulseMultiplier(state) {
  return state.ownedJars.includes(SPECIMEN_JARS.A_BORROWED_PULSE.id)
    ? 1.0 + ORGAN_BALANCE.BORROWED_PULSE_BONUS * jarEffectBoost(state)
    : 1.0;
}

export function uncountedHourMultiplier(state) {
  return state.ownedJars.includes(SPECIMEN_JARS.THE_UNCOUNTED_HOUR.id)
    ? 1.0 + ORGAN_BALANCE.UNCOUNTED_HOUR_BONUS * jarEffectBoost(state)
    : 1.0;
}

export function keloidIchorMultiplier(state) {
  return 1.0 + repeatableRank(state, 'keloid_thickening') * ORGAN_BALANCE.KELOID_ICHOR_PER_RANK;
}

export function sedimentStillnessMultiplier(state) {
  return 1.0 + repeatableRank(state, 'sediment_well') * ORGAN_BALANCE.SEDIMENT_STILLNESS_PER_RANK;
}

export function scarHarvestMultiplier(state) {
  return 1.0 + repeatableRank(state, 'scar_harvest_hook') * ORGAN_BALANCE.SCAR_HARVEST_HOOK_PER_RANK;
}

export function catatonicTideMultiplier(state) {
  if (!hasNode(state, 'catatonic_tide')) return 1.0;
  return state.isStillnessActive ? 3.2 : 1.75;
}

export function scarMultiplier(state) {
  const baseResonance =
    1.0 +
    state.highestExcisionMilestone * ORGAN_BALANCE.MILESTONE_ICHOR_RESONANCE +
    Math.sqrt(state.scarTissue) * ORGAN_BALANCE.SCAR_RESONANCE_SQRT_FACTOR;
  const rootMult = hasNode(state, 'first_incision') ? 1.45 : 1.0;
  const boneMult = hasNode(state, 'bone_resonance') ? 2.1 : 1.0;
  return baseResonance * rootMult * boneMult * keloidIchorMultiplier(state);
}

export function scarStillnessMultiplier(state) {
  const baseResonance =
    1.0 +
    state.highestExcisionMilestone * ORGAN_BALANCE.MILESTONE_STILLNESS_RESONANCE +
    Math.sqrt(state.scarTissue) * ORGAN_BALANCE.SCAR_STILLNESS_SQRT_FACTOR;
  const rootMult = hasNode(state, 'first_incision') ? 1.2 : 1.0;
  const silenceMult = hasNode(state, 'shorter_silence') ? 1.5 : 1.0;
  return baseResonance * rootMult * silenceMult * sedimentStillnessMultiplier(state);
}

export function relicAmplifier(state) {
  return (hasNode(state, 'echo_ossuary') ? 1.35 : 1.0) * (hasNode(state, 'bone_resonance') ? 1.4 : 1.0);
}

export function relicIchorMultiplier(state) {
  let mult = 1.0;
  const amp = relicAmplifier(state);
  for (const id of state.dredgedRelics ?? []) {
    const relic = DREDGED_RELICS.find((r) => r.id === id);
    if (relic) {
      mult *= 1.0 + (relic.ichorMultiplier - 1.0) * amp;
    }
  }
  return mult;
}

export function relicStillnessMultiplier(state) {
  let mult = 1.0;
  for (const id of state.dredgedRelics ?? []) {
    const relic = DREDGED_RELICS.find((r) => r.id === id);
    if (relic) mult *= relic.stillnessMultiplier;
  }
  return mult;
}

export function dossierIchorMultiplier(state) {
  let mult = 1.0;
  const perLineBoost = hasNode(state, 'bone_resonance') ? 1.18 : 1.0;
  for (const id of state.unredactedDossierLines ?? []) {
    const line = dossierLineById(id);
    if (line) mult *= line.ichorMultiplier * perLineBoost;
  }
  return mult;
}

export function gazeMultiplier(state) {
  return state.gazeRemainingSeconds > 0 ? ORGAN_BALANCE.GAZE_PASSIVE_MULTIPLIER : 1.0;
}

export function bloomMultiplier(state) {
  if (state.marrowBloomRemainingSeconds > 0) {
    return hasNode(state, 'bloom_incision')
      ? ORGAN_BALANCE.BLOOM_INCISION_MULTIPLIER
      : ORGAN_BALANCE.MARROW_BLOOM_MULTIPLIER;
  }
  return 1.0;
}

export function anomalyRewardMultiplier(state) {
  const jarBoost = state.ownedJars.includes(SPECIMEN_JARS.ALMOST_A_NAME.id)
    ? ORGAN_BALANCE.ALMOST_A_NAME_MULTIPLIER * jarEffectBoost(state)
    : 1.0;
  const mortarBoost = hasNode(state, 'softened_mortar') ? 1.8 : 1.0;
  const baseEchoBonus =
    state.echoDiversionPercent === 25 ? 0.35 : state.echoDiversionPercent === 50 ? 0.7 : 0.0;
  const echoBoost = 1.0 + baseEchoBonus * (hasNode(state, 'bone_resonance') ? 1.4 : 1.0);
  const bloomAnomalyBoost =
    state.marrowBloomRemainingSeconds > 0 && hasNode(state, 'bloom_incision') ? 1.5 : 1.0;
  return jarBoost * mortarBoost * echoBoost * bloomAnomalyBoost;
}

export function globalIchorMultiplier(state) {
  return (
    scarMultiplier(state) *
    relicIchorMultiplier(state) *
    dossierIchorMultiplier(state) *
    gazeMultiplier(state) *
    bloomMultiplier(state) *
    uncountedHourMultiplier(state) *
    catatonicTideMultiplier(state)
  );
}

export function effectiveCreatureIchorPerSec(state, typeId) {
  const spec = typeof typeId === 'string' ? creatureById(typeId) : typeId;
  if (!spec) return 0.0;
  const inertiaMult = hasNode(state, 'colony_inertia') ? 1.9 : 1.0;
  const seamMult = hasNode(state, 'perpetual_seam') ? 2.5 : 1.0;
  const jarPotency = jarEffectBoost(state);

  let specimenMultiplier = 1.0;
  if (spec.id === CREATURE_TYPES.SUTURE_HAND.id || spec.id === CREATURE_TYPES.LEECH_CLERK.id) {
    const breath = state.ownedJars.includes(SPECIMEN_JARS.UNSPENT_BREATH.id)
      ? ORGAN_BALANCE.UNSPENT_BREATH_MULTIPLIER * jarPotency
      : 1.0;
    const twin = hasNode(state, 'twin_stitch') ? 1.6 : 1.0;
    specimenMultiplier = breath * twin;
  } else if (spec.id === CREATURE_TYPES.WEEPING_LAMP.id) {
    const nameJar = state.ownedJars.includes(SPECIMEN_JARS.ALMOST_A_NAME.id)
      ? ORGAN_BALANCE.ALMOST_A_NAME_MULTIPLIER * jarPotency
      : 1.0;
    const weepingSurge = state.isStillnessActive
      ? hasNode(state, 'hollow_lung_choir')
        ? 2.6
        : 2.0
      : hasNode(state, 'deep_weeping')
        ? 1.25
        : 1.0;
    const deepMult = hasNode(state, 'deep_weeping') ? 2.0 : 1.0;
    specimenMultiplier = nameJar * weepingSurge * deepMult;
  } else if (spec.id === CREATURE_TYPES.SUMP_SIPHON.id) {
    const autoMult = hasNode(state, 'auto_tendril_suture') ? 1.65 : 1.0;
    const recallMult = hasNode(state, 'membrane_recall') ? 2.2 : 1.0;
    const sealPenalty = state.drainSealed ? 0.85 : 1.0;
    specimenMultiplier = autoMult * recallMult * sealPenalty;
  }

  return (
    spec.baseIchorPerSec *
    specimenMultiplier *
    inertiaMult *
    seamMult *
    groutSaltOrganismMultiplier(state) *
    globalIchorMultiplier(state)
  );
}

export function totalCreatureIchorPerSec(state, typeId) {
  const id = typeof typeId === 'string' ? typeId : typeId.id;
  return creatureCount(state, id) * effectiveCreatureIchorPerSec(state, id);
}

export function effectiveLampStillnessPerSec(state) {
  const jarMult = state.ownedJars.includes(SPECIMEN_JARS.ALMOST_A_NAME.id)
    ? ORGAN_BALANCE.ALMOST_A_NAME_MULTIPLIER * jarEffectBoost(state)
    : 1.0;
  return (
    ORGAN_BALANCE.LAMP_STILLNESS_PER_SEC *
    jarMult *
    scarStillnessMultiplier(state) *
    relicStillnessMultiplier(state) *
    gazeMultiplier(state)
  );
}

export function rawIchorPerSecond(state) {
  let creaturesRate = 0.0;
  for (const spec of CREATURE_LIST) {
    creaturesRate += totalCreatureIchorPerSec(state, spec.id);
  }
  const seamColonyMult = hasNode(state, 'perpetual_seam') ? 2.5 : 1.0;
  const graftNodeMult =
    (hasNode(state, 'auto_tendril_suture') ? 1.65 : 1.0) *
    (hasNode(state, 'membrane_recall') ? 2.2 : 1.0);
  const pulseMult = borrowedPulseMultiplier(state);
  const graftRate =
    state.graftedTendrils *
    ORGAN_BALANCE.GRAFT_ICHOR_PER_SEC *
    graftNodeMult *
    seamColonyMult *
    pulseMult *
    globalIchorMultiplier(state);
  const choirNodeMult = hasNode(state, 'hollow_lung_choir') ? 2.1 : 1.0;
  const lungStillnessBoost = state.isStillnessActive
    ? hasNode(state, 'hollow_lung_choir')
      ? 2.0
      : 1.5
    : 1.0;
  const lungRate =
    state.choirLungs *
    ORGAN_BALANCE.CHOIR_LUNG_ICHOR_PER_SEC *
    choirNodeMult *
    seamColonyMult *
    lungStillnessBoost *
    pulseMult *
    globalIchorMultiplier(state);
  return creaturesRate + graftRate + lungRate;
}

export function ichorPerSecond(state) {
  const echoFactor = 1.0 - (state.echoDiversionPercent ?? 0) / 100.0;
  const drainFactor = state.drainSealed ? 0.85 : 1.0;
  return rawIchorPerSecond(state) * echoFactor * drainFactor;
}

export function manualTapYield(state) {
  const dustBonus = state.ownedJars.includes(SPECIMEN_JARS.YESTERDAYS_DUST.id)
    ? ORGAN_BALANCE.YESTERDAYS_DUST_TAP_BONUS * jarEffectBoost(state)
    : 0.0;
  const toothBonus = (state.anomalyInteractions?.pale_tooth ?? 0) * 0.5;
  const palmMult = hasNode(state, 'calloused_palm') ? 1.8 : 1.0;
  const seamMult = hasNode(state, 'perpetual_seam') ? 2.5 : 1.0;
  const echoBurst = hasNode(state, 'twin_stitch') ? ichorPerSecond(state) * 1.5 : 0.0;
  return (
    (1.0 + dustBonus + toothBonus) *
      scarMultiplier(state) *
      relicIchorMultiplier(state) *
      dossierIchorMultiplier(state) *
      palmMult *
      seamMult +
    echoBurst
  );
}

export function stillnessPerSecond(state) {
  const lampCount = creatureCount(state, CREATURE_TYPES.WEEPING_LAMP.id);
  const lampFraction = state.isStillnessActive
    ? 1.0
    : hasNode(state, 'deep_weeping') && lampCount > 0
      ? 0.5
      : 0.0;
  const lampStillness = lampCount * effectiveLampStillnessPerSec(state) * lampFraction;
  const graftStillness =
    state.graftedTendrils *
    ORGAN_BALANCE.GRAFT_STILLNESS_PER_SEC *
    scarStillnessMultiplier(state) *
    relicStillnessMultiplier(state) *
    gazeMultiplier(state);
  const choirNodeMult = hasNode(state, 'hollow_lung_choir') ? 2.1 : 1.0;
  const lungStillnessBoost = state.isStillnessActive
    ? hasNode(state, 'hollow_lung_choir')
      ? 2.0
      : 1.5
    : 1.0;
  const lungStillness =
    state.choirLungs *
    ORGAN_BALANCE.CHOIR_LUNG_STILLNESS_PER_SEC *
    choirNodeMult *
    scarStillnessMultiplier(state) *
    relicStillnessMultiplier(state) *
    gazeMultiplier(state) *
    lungStillnessBoost;
  return (
    (lampStillness + graftStillness + lungStillness) *
    borrowedPulseMultiplier(state) *
    uncountedHourMultiplier(state) *
    catatonicTideMultiplier(state)
  );
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

export function isScarTreeUnlocked(state) {
  return state.highestExcisionMilestone >= 1 || state.cycle > 0 || state.scarTissue > 0;
}

export function isChamberRitesUnlocked(state) {
  return state.highestExcisionMilestone >= 2;
}

export function isValveClamped(state) {
  return state.clampRemainingSeconds > 0;
}

export function isGraftOnCooldown(state) {
  return (state.graftCooldownRemainingSeconds ?? 0) > 0;
}

export function isChoirLungOnCooldown(state) {
  return (state.choirLungCooldownRemainingSeconds ?? 0) > 0;
}

export function canAffordGraft(state) {
  return (
    isGraftUnlocked(state) &&
    !isGraftOnCooldown(state) &&
    creatureCount(state, CREATURE_TYPES.SUTURE_HAND.id) >= ORGAN_BALANCE.GRAFT_SUTURE_COST &&
    creatureCount(state, CREATURE_TYPES.LEECH_CLERK.id) >= ORGAN_BALANCE.GRAFT_LEECH_COST &&
    state.stillness >= graftStillnessCost(state)
  );
}

export function canAffordChoirLung(state) {
  return (
    state.highestExcisionMilestone >= 4 &&
    !isChoirLungOnCooldown(state) &&
    creatureCount(state, CREATURE_TYPES.WEEPING_LAMP.id) >= ORGAN_BALANCE.CHOIR_LUNG_LAMP_COST &&
    state.graftedTendrils >= ORGAN_BALANCE.CHOIR_LUNG_TENDRIL_COST &&
    state.stillness >= choirLungStillnessCost(state)
  );
}

export function nextDredgeableRelic(state) {
  return DREDGED_RELICS.find((r) => !(state.dredgedRelics ?? []).includes(r.id)) ?? null;
}

export function canDredgeSump(state) {
  return (
    state.highestExcisionMilestone >= 2 &&
    state.drainPressure >= 100.0 &&
    nextDredgeableRelic(state) != null
  );
}

export function nextRedactedLine(state) {
  return (
    CULT_DOSSIER_LINES.find((l) => !(state.unredactedDossierLines ?? []).includes(l.id)) ?? null
  );
}

export function canOfferToThroat(state, lineOrId) {
  const line = typeof lineOrId === 'string' ? dossierLineById(lineOrId) : lineOrId;
  if (!line || state.highestExcisionMilestone < 5) return false;
  if ((state.unredactedDossierLines ?? []).includes(line.id)) return false;
  const enoughSiphons = creatureCount(state, CREATURE_TYPES.SUMP_SIPHON.id) >= line.siphonCost;
  const enoughTendrils = state.graftedTendrils >= line.tendrilCost;
  const enoughJars = !line.requiresJar || state.ownedJars.length > 0;
  return enoughSiphons && enoughTendrils && enoughJars;
}

export function nextExcisionMilestone(state) {
  return EXCISION_MILESTONES.find((m) => m.number === state.highestExcisionMilestone + 1) ?? null;
}

export function canExcise(state) {
  return cycleLifetimeIchorDisplay(state) >= ORGAN_BALANCE.EXCISION_ICHOR_THRESHOLD;
}

export function canClearNextMilestone(state) {
  const next = nextExcisionMilestone(state);
  if (!next) return false;
  return cycleLifetimeIchorDisplay(state) >= next.massThreshold;
}

export function hasExcessMilestoneMass(state) {
  if (!canClearNextMilestone(state)) return false;
  const beyond = EXCISION_MILESTONES.find((m) => m.number === state.highestExcisionMilestone + 2);
  if (!beyond) return false;
  return cycleLifetimeIchorDisplay(state) >= beyond.massThreshold;
}

export function canBreachToArc2(state) {
  return (
    state.highestExcisionMilestone >= 6 ||
    (state.highestExcisionMilestone >= 5 &&
      cycleLifetimeIchorDisplay(state) >= EXCISION_MILESTONES[5].massThreshold)
  );
}

function minExcisionScarHarvest(state) {
  return Math.max(2, Math.floor(ORGAN_BALANCE.SCAR_CURVE_BASE_SCALE * scarHarvestMultiplier(state)));
}

function rawScarHarvestForMass(state, mass) {
  if (mass < ORGAN_BALANCE.EXCISION_ICHOR_THRESHOLD) return 0;
  const ratio = mass / ORGAN_BALANCE.EXCISION_ICHOR_THRESHOLD;
  const raw =
    ORGAN_BALANCE.SCAR_CURVE_BASE_SCALE *
    Math.pow(ratio, ORGAN_BALANCE.SCAR_CURVE_EXPONENT) *
    scarHarvestMultiplier(state);
  return Math.max(2, Math.floor(raw));
}

function milestonePulledScarCount(state) {
  const milestoneMass = nextExcisionMilestone(state)?.massThreshold ?? null;
  if (milestoneMass == null || milestoneMass <= ORGAN_BALANCE.EXCISION_ICHOR_THRESHOLD) {
    return null;
  }
  const atMilestone = rawScarHarvestForMass(state, milestoneMass);
  const justBefore = rawScarHarvestForMass(state, milestoneMass - 1);
  return atMilestone === justBefore && atMilestone > 0 ? atMilestone + 1 : null;
}

function scarHarvestForMass(state, mass) {
  const raw = rawScarHarvestForMass(state, mass);
  if (raw === 0) return 0;
  const pulled = milestonePulledScarCount(state);
  const milestoneMass = nextExcisionMilestone(state)?.massThreshold ?? null;
  if (pulled != null && milestoneMass != null && mass >= milestoneMass && raw === pulled - 1) {
    return pulled;
  }
  return raw;
}

export function massRequiredForScarCount(state, targetScars) {
  if (targetScars <= 0) return 0;
  if (targetScars <= minExcisionScarHarvest(state)) return ORGAN_BALANCE.EXCISION_ICHOR_THRESHOLD;
  const pulled = milestonePulledScarCount(state);
  const milestoneMass = nextExcisionMilestone(state)?.massThreshold ?? null;
  if (pulled != null && milestoneMass != null && targetScars === pulled) {
    return milestoneMass;
  }
  const denom = ORGAN_BALANCE.SCAR_CURVE_BASE_SCALE * scarHarvestMultiplier(state);
  const ratio = Math.pow(targetScars / denom, 1.0 / ORGAN_BALANCE.SCAR_CURVE_EXPONENT);
  let candidate = Math.max(
    ORGAN_BALANCE.EXCISION_ICHOR_THRESHOLD,
    Math.ceil(ORGAN_BALANCE.EXCISION_ICHOR_THRESHOLD * ratio),
  );
  while (rawScarHarvestForMass(state, candidate) < targetScars) {
    candidate += 1;
  }
  return candidate;
}

export function currentBaseScarHarvest(state) {
  if (!canExcise(state)) return 0;
  return scarHarvestForMass(state, state.cycleLifetimeIchor);
}

export function nextBaseScarMassThreshold(state) {
  if (!canExcise(state)) return ORGAN_BALANCE.EXCISION_ICHOR_THRESHOLD;
  return massRequiredForScarCount(state, currentBaseScarHarvest(state) + 1);
}

export function nextScarProgress(state) {
  if (!canExcise(state)) {
    return Math.max(
      0,
      Math.min(1, state.cycleLifetimeIchor / ORGAN_BALANCE.EXCISION_ICHOR_THRESHOLD),
    );
  }
  const current = currentBaseScarHarvest(state);
  const prevMass =
    current <= minExcisionScarHarvest(state)
      ? ORGAN_BALANCE.EXCISION_ICHOR_THRESHOLD
      : massRequiredForScarCount(state, current);
  const nextMass = nextBaseScarMassThreshold(state);
  const span = Math.max(1, nextMass - prevMass);
  return Math.max(0, Math.min(1, (state.cycleLifetimeIchor - prevMass) / span));
}

export function pendingScarTissue(state) {
  if (!canExcise(state)) return 0;
  const milestoneBonus = canClearNextMilestone(state)
    ? (nextExcisionMilestone(state)?.firstClearScarBonus ?? 0)
    : 0;
  return currentBaseScarHarvest(state) + milestoneBonus;
}
