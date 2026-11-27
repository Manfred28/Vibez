/**
 * Arc 3 ("The Unmaking") models, balance constants, and pure derived selectors
 * (1:1 mathematical parity with Android Arc3BreachModels.kt).
 */

export const ARC3_STRATA = Object.freeze([
  {
    number: 1,
    id: 'broken_grout',
    title: 'BROKEN GROUT',
    blurTitle: 'BROKEN GROUT',
    fractureGoal: 120.0,
    baseClotPerSec: 3.5,
    baseFloodLevel: 0.08,
    wedgeId: 'tax_plat_1998',
    wedgeTitle: '1998 TAX PLAT',
    wedgeCaption: 'Folded parcel map jammed dry into the cracked tile seam above the valve.',
  },
  {
    number: 2,
    id: 'unstitched_seam',
    title: 'UNSTITCHED SEAM',
    blurTitle: 'UNSTITCHED SEAM',
    fractureGoal: 750.0,
    baseClotPerSec: 16.0,
    baseFloodLevel: 0.24,
    wedgeId: 'second_chair_polaroid',
    wedgeTitle: 'SECOND CHAIR POLAROID',
    wedgeCaption: 'Forced deep into the torn scar ridge before warm plaster knits back shut.',
  },
  {
    number: 3,
    id: 'splintered_ribs',
    title: 'SPLINTERED RIBS',
    blurTitle: 'SPLINTERED RIBS',
    fractureGoal: 3800.0,
    baseClotPerSec: 68.0,
    baseFloodLevel: 0.44,
    wedgeId: 'reel_four_waveform',
    wedgeTitle: 'REEL 4 WAVEFORM',
    wedgeCaption: 'Slack magnetic ribbon packed tight between cracked calcified ribs.',
  },
  {
    number: 4,
    id: 'shared_pulse',
    title: 'SHARED PULSE',
    blurTitle: 'YOUR PULSE',
    fractureGoal: 18000.0,
    baseClotPerSec: 260.0,
    baseFloodLevel: 0.68,
    wedgeId: 'drainage_loop_blueprint',
    wedgeTitle: 'CLOSED DRAINAGE PLAT',
    wedgeCaption: 'Soaked storm-main schematic braced behind the sheared flange bolts.',
  },
  {
    number: 5,
    id: 'the_open_seam',
    title: 'THE OPEN SEAM',
    blurTitle: 'OPEN VESSEL',
    fractureGoal: 75000.0,
    baseClotPerSec: 820.0,
    baseFloodLevel: 0.86,
    wedgeId: 'file_zero_intake',
    wedgeTitle: 'FILE #00 · SUBJECT INTAKE',
    wedgeCaption: 'Your own badge sheet driven straight into the weeping doorway seam.',
  },
]);

export const ARC3_UNMAKE_CATEGORIES = Object.freeze([
  { id: 'shatter_jar', label: 'SHATTERED JARS' },
  { id: 'reverse_apparatus', label: 'REVERSED APPARATUS' },
  { id: 'unstitch_scar', label: 'UNSTITCHED SEAMS' },
]);

export const ARC3_UNMAKE_ACTS = Object.freeze([
  // 6 Specimen Jar Shatters
  {
    id: 'shatter_yesterdays_dust',
    category: 'shatter_jar',
    minStratum: 1,
    title: "SHATTER: YESTERDAY'S DUST",
    description: 'Break the first wax seal on the baseboard; spilled brine weakens clotting.',
    hemorrhageCost: 18.0,
    wrenchMultiplier: 1.25,
    ruptureMultiplier: 1.0,
    antiClotFraction: 0.06,
    bleedPressurePerSec: 0.0,
    instantFractureSurge: 24.0,
    linkedJar: 'yesterdays_dust',
  },
  {
    id: 'shatter_unspent_breath',
    category: 'shatter_jar',
    minStratum: 1,
    title: 'SHATTER: UNSPENT BREATH',
    description: 'Crush the fogged jar against the grout; cold pressure jolts the valve loose.',
    hemorrhageCost: 52.0,
    wrenchMultiplier: 1.25,
    ruptureMultiplier: 1.0,
    antiClotFraction: 0.06,
    bleedPressurePerSec: 0.0,
    instantFractureSurge: 40.0,
    linkedJar: 'unspent_breath',
  },
  {
    id: 'shatter_almost_a_name',
    category: 'shatter_jar',
    minStratum: 1,
    title: 'SHATTER: ALMOST A NAME',
    description: 'Smash the unlabeled glass; warm mortar flinches away from the shards.',
    hemorrhageCost: 115.0,
    wrenchMultiplier: 1.3,
    ruptureMultiplier: 1.0,
    antiClotFraction: 0.06,
    bleedPressurePerSec: 0.0,
    instantFractureSurge: 65.0,
    linkedJar: 'almost_a_name',
  },
  {
    id: 'shatter_salt_grout',
    category: 'shatter_jar',
    minStratum: 2,
    title: 'SHATTER: SALT FROM THE GROUT',
    description: 'Salt-burn the weeping seams so clots tear open cleaner and slower.',
    hemorrhageCost: 420.0,
    wrenchMultiplier: 1.32,
    ruptureMultiplier: 1.0,
    antiClotFraction: 0.06,
    bleedPressurePerSec: 0.0,
    instantFractureSurge: 190.0,
    linkedJar: 'salt_from_the_grout',
  },
  {
    id: 'shatter_borrowed_pulse',
    category: 'shatter_jar',
    minStratum: 2,
    title: 'SHATTER: A BORROWED PULSE',
    description: 'Drop the pulsing vessel onto tile; its double-beat floods your wrenching.',
    hemorrhageCost: 1150.0,
    wrenchMultiplier: 1.35,
    ruptureMultiplier: 1.0,
    antiClotFraction: 0.06,
    bleedPressurePerSec: 0.0,
    instantFractureSurge: 310.0,
    linkedJar: 'a_borrowed_pulse',
  },
  {
    id: 'shatter_uncounted_hour',
    category: 'shatter_jar',
    minStratum: 3,
    title: 'SHATTER: THE UNCOUNTED HOUR',
    description: 'Break the tallest seal on the shelf; stillness drains from the chamber forever.',
    hemorrhageCost: 4200.0,
    wrenchMultiplier: 1.4,
    ruptureMultiplier: 1.0,
    antiClotFraction: 0.06,
    bleedPressurePerSec: 0.0,
    instantFractureSurge: 1100.0,
    linkedJar: 'the_uncounted_hour',
  },
  // 6 Reversed Organism Apparatuses
  {
    id: 'reverse_suture_hands',
    category: 'reverse_apparatus',
    minStratum: 1,
    title: 'REVERSE SUTURE-HANDS',
    description: 'Force the stitched fingers to claw outward at the mortar instead of turning clockwise.',
    hemorrhageCost: 35.0,
    wrenchMultiplier: 1.45,
    ruptureMultiplier: 1.0,
    antiClotFraction: 0.0,
    bleedPressurePerSec: 1.4,
    instantFractureSurge: 0.0,
    linkedCreature: 'suture_hand',
  },
  {
    id: 'reverse_leech_clerks',
    category: 'reverse_apparatus',
    minStratum: 2,
    title: 'SPLIT LEECH-CLERK GLASS',
    description: 'Crack the tally bezel; blind mouths chew straight through the wet plaster.',
    hemorrhageCost: 260.0,
    wrenchMultiplier: 1.5,
    ruptureMultiplier: 1.0,
    antiClotFraction: 0.0,
    bleedPressurePerSec: 6.5,
    instantFractureSurge: 0.0,
    linkedCreature: 'leech_clerk',
  },
  {
    id: 'burst_weeping_lamps',
    category: 'reverse_apparatus',
    minStratum: 2,
    title: 'BURST WEEPING LAMPS',
    description: 'Overload the ceiling glass until dark oil and sparks rain over the seams.',
    hemorrhageCost: 780.0,
    wrenchMultiplier: 1.55,
    ruptureMultiplier: 1.0,
    antiClotFraction: 0.0,
    bleedPressurePerSec: 10.0,
    instantFractureSurge: 0.0,
    linkedCreature: 'weeping_lamp',
  },
  {
    id: 'backflood_sump_siphons',
    category: 'reverse_apparatus',
    minStratum: 3,
    title: 'BACK-FLOOD SUMP-SIPHONS',
    description: 'Invert the leather bladders to pump crimson runoff backward beneath the tiles.',
    hemorrhageCost: 2400.0,
    wrenchMultiplier: 1.65,
    ruptureMultiplier: 1.0,
    antiClotFraction: 0.0,
    bleedPressurePerSec: 28.0,
    instantFractureSurge: 0.0,
    linkedCreature: 'sump_siphon',
  },
  {
    id: 'rip_choir_tendrils',
    category: 'reverse_apparatus',
    minStratum: 3,
    title: 'TEAR CHOIR-TENDRILS',
    description: 'Unbraid the hooked sinews from the valve rim and drag them against the ribs.',
    hemorrhageCost: 6800.0,
    wrenchMultiplier: 1.7,
    ruptureMultiplier: 1.0,
    antiClotFraction: 0.0,
    bleedPressurePerSec: 42.0,
    instantFractureSurge: 0.0,
  },
  {
    id: 'rupture_choir_lungs',
    category: 'reverse_apparatus',
    minStratum: 4,
    title: 'OVER-INFLATE CHOIR-LUNGS',
    description: 'Force every pleural graft past bursting until the calcified arches crack outward.',
    hemorrhageCost: 21000.0,
    wrenchMultiplier: 1.85,
    ruptureMultiplier: 1.0,
    antiClotFraction: 0.0,
    bleedPressurePerSec: 120.0,
    instantFractureSurge: 0.0,
  },
  // 6 Unstitched Scar Seams & Broken Rites
  {
    id: 'unstitch_first_incision',
    category: 'unstitch_scar',
    minStratum: 1,
    title: 'UNSTITCH: FIRST INCISION',
    description: 'Rip open the root suture across the chamber wall; refuse to let it close again.',
    hemorrhageCost: 85.0,
    wrenchMultiplier: 1.6,
    ruptureMultiplier: 1.35,
    antiClotFraction: 0.0,
    bleedPressurePerSec: 0.0,
    instantFractureSurge: 0.0,
    linkedScarNode: 'first_incision',
  },
  {
    id: 'unstitch_suture_line',
    category: 'unstitch_scar',
    minStratum: 2,
    title: 'UNSTITCH: SUTURE BRANCH',
    description: 'Pull the bone-thread loose from Calloused Palm to Perpetual Seam.',
    hemorrhageCost: 1500.0,
    wrenchMultiplier: 1.75,
    ruptureMultiplier: 1.4,
    antiClotFraction: 0.0,
    bleedPressurePerSec: 0.0,
    instantFractureSurge: 0.0,
    linkedScarNode: 'perpetual_seam',
  },
  {
    id: 'unstitch_stillness_line',
    category: 'unstitch_scar',
    minStratum: 3,
    title: 'UNSTITCH: STILLNESS BRANCH',
    description: 'Tear apart the catatonic tide sutures so the room can no longer lull you quiet.',
    hemorrhageCost: 9200.0,
    wrenchMultiplier: 1.8,
    ruptureMultiplier: 1.45,
    antiClotFraction: 0.0,
    bleedPressurePerSec: 0.0,
    instantFractureSurge: 0.0,
    linkedScarNode: 'catatonic_tide',
  },
  {
    id: 'unstitch_marrow_line',
    category: 'unstitch_scar',
    minStratum: 4,
    title: 'UNSTITCH: MARROW BRANCH',
    description: 'Split the resonant bone ridges wide; your own thumb aches along the fault line.',
    hemorrhageCost: 38000.0,
    wrenchMultiplier: 1.95,
    ruptureMultiplier: 1.55,
    antiClotFraction: 0.0,
    bleedPressurePerSec: 0.0,
    instantFractureSurge: 0.0,
    linkedScarNode: 'bone_resonance',
  },
  {
    id: 'break_floor_grate_throat',
    category: 'unstitch_scar',
    minStratum: 4,
    title: 'SPLIT GRATE & THROAT',
    description: 'Pry the cast-iron floor grate out of the sump and tear through the ossified throat.',
    hemorrhageCost: 72000.0,
    wrenchMultiplier: 2.05,
    ruptureMultiplier: 1.0,
    antiClotFraction: 0.0,
    bleedPressurePerSec: 150.0,
    instantFractureSurge: 0.0,
  },
  {
    id: 'split_bricked_jamb',
    category: 'unstitch_scar',
    minStratum: 5,
    title: 'TEAR THE BRICKED FRAME',
    description: 'Wrench the ancient mortar loose around the sealed doorway silhouette.',
    hemorrhageCost: 165000.0,
    wrenchMultiplier: 2.4,
    ruptureMultiplier: 1.7,
    antiClotFraction: 0.0,
    bleedPressurePerSec: 320.0,
    instantFractureSurge: 0.0,
  },
]);

export const BREACH_RUPTURE_TYPES = Object.freeze({
  CLOTTING_SEAM: {
    id: 'clotting_seam',
    minStratum: 1,
    actionLabel: 'RIP',
    fractureWrenchMult: 4.2,
    hemorrhageWrenchMult: 3.0,
    clotFreezeSec: 3.5,
    ribForceSec: 0.0,
    frenzySec: 0.0,
    dripLine: 'Wet mortar peels back; the clotting hesitates.',
  },
  SPLINTERED_RIB: {
    id: 'splintered_rib',
    minStratum: 2,
    actionLabel: 'SNAP',
    fractureWrenchMult: 3.2,
    hemorrhageWrenchMult: 6.5,
    clotFreezeSec: 0.0,
    ribForceSec: 4.5,
    frenzySec: 0.0,
    dripLine: 'Calcified bone snaps dry; your knuckles carry its weight.',
  },
  TORN_SUTURE: {
    id: 'torn_suture',
    minStratum: 3,
    actionLabel: 'TEAR',
    fractureWrenchMult: 4.8,
    hemorrhageWrenchMult: 4.5,
    clotFreezeSec: 6.0,
    ribForceSec: 0.0,
    frenzySec: 6.0,
    dripLine: 'Old thread rips loose in a warm surge down both forearms.',
  },
  ARTERIAL_WELL: {
    id: 'arterial_well',
    minStratum: 4,
    actionLabel: 'BREACH',
    fractureWrenchMult: 7.5,
    hemorrhageWrenchMult: 8.0,
    clotFreezeSec: 4.5,
    ribForceSec: 0.0,
    frenzySec: 0.0,
    dripLine: 'Dark red wells straight up out of the grout, warm as skin.',
  },
});

export const BREACH_RUPTURE_LIST = Object.freeze(Object.values(BREACH_RUPTURE_TYPES));

export const ARC3_BALANCE = Object.freeze({
  BASE_WRENCH_FRACTURE: 4.5,
  BASE_WRENCH_HEMORRHAGE: 5.5,
  MOMENTUM_GAIN_PER_TAP: 0.12,
  MOMENTUM_MAX_BONUS: 0.8,
  MOMENTUM_DECAY_DELAY_SEC: 0.85,
  MOMENTUM_DECAY_PER_SEC: 0.36,
  CLOT_GRACE_SEC: 1.4,
  CLOT_RAMP_SEC: 1.5,
  MAX_ANTI_CLOT_FRACTION: 0.45,
  RIB_FORCE_MULTIPLIER: 1.5,
  FRENZY_MULTIPLIER: 2.0,
  FIRST_RUPTURE_DELAY_SEC: 5.0,
  RUPTURE_SPAWN_INTERVAL_SEC: 9.0,
  RUPTURE_LIFETIME_SEC: 16.0,
  MAX_ACTIVE_RUPTURES: 3,
  FINAL_PULLS_TOTAL: 5,
  FINAL_PULL_COOLDOWN_SEC: 1.8,
});

export function initialArc3State() {
  return {
    currentStratum: 1,
    lockedWedges: [],
    fracture: 0.0,
    hemorrhage: 0.0,
    lifetimeHemorrhage: 0.0,
    wrenchCount: 0,
    momentumCharge: 0.0,
    secondsSinceLastAction: 0.0,
    clotFreezeRemainingSeconds: 0.0,
    ribForceRemainingSeconds: 0.0,
    frenzyRemainingSeconds: 0.0,
    completedUnmakes: [],
    activeRuptures: [],
    ruptureInteractions: {},
    secondsUntilNextRupture: ARC3_BALANCE.FIRST_RUPTURE_DELAY_SEC,
    nextRuptureId: 1,
    finalPullsCompleted: 0,
    finalPullCooldownSeconds: 0.0,
    submersionComplete: false,
  };
}

export function arc3StratumByNumber(num) {
  return ARC3_STRATA.find((s) => s.number === num) ?? ARC3_STRATA[ARC3_STRATA.length - 1];
}

export function arc3UnmakeById(id) {
  return ARC3_UNMAKE_ACTS.find((a) => a.id === id) ?? null;
}

export function arc3RuptureTypeById(id) {
  return BREACH_RUPTURE_LIST.find((r) => r.id === id) ?? null;
}

export function arc3ActiveStratum(breach) {
  return arc3StratumByNumber(breach?.currentStratum ?? 1);
}

export function arc3IsInFinale(breach) {
  return (breach?.lockedWedges?.length ?? 0) >= ARC3_STRATA.length;
}

export function arc3IsValveSheared(breach) {
  return (breach?.currentStratum ?? 1) >= 4 || arc3IsInFinale(breach);
}

export function arc3StratumProgress(breach) {
  if (!breach) return 0;
  if (arc3IsInFinale(breach)) {
    return Math.max(0, Math.min(1, (breach.finalPullsCompleted ?? 0) / ARC3_BALANCE.FINAL_PULLS_TOTAL));
  }
  const goal = arc3ActiveStratum(breach).fractureGoal;
  return Math.max(0, Math.min(1, (breach.fracture ?? 0) / Math.max(1, goal)));
}

export function arc3CanDriveWedge(breach) {
  if (!breach || arc3IsInFinale(breach)) return false;
  const active = arc3ActiveStratum(breach);
  return !(breach.lockedWedges ?? []).includes(active.id) && (breach.fracture ?? 0) >= active.fractureGoal;
}

export function arc3CanPerformFinalPull(breach) {
  if (!breach) return false;
  return (
    arc3IsInFinale(breach) &&
    !breach.submersionComplete &&
    (breach.finalPullsCompleted ?? 0) < ARC3_BALANCE.FINAL_PULLS_TOTAL &&
    (breach.finalPullCooldownSeconds ?? 0) <= 0.0
  );
}

export function arc3ShatteredJarCount(breach) {
  const done = breach?.completedUnmakes ?? [];
  return ARC3_UNMAKE_ACTS.filter((a) => a.category === 'shatter_jar' && done.includes(a.id)).length;
}

export function arc3UnstitchedScarCount(breach) {
  const done = breach?.completedUnmakes ?? [];
  return ARC3_UNMAKE_ACTS.filter((a) => a.category === 'unstitch_scar' && done.includes(a.id)).length;
}

export function arc3IsJarShattered(breach, jarId) {
  const done = breach?.completedUnmakes ?? [];
  return ARC3_UNMAKE_ACTS.some((a) => a.linkedJar === jarId && done.includes(a.id));
}

export function arc3TotalRuptureInteractions(breach) {
  const map = breach?.ruptureInteractions ?? {};
  return Object.values(map).reduce((sum, v) => sum + (Number(v) || 0), 0);
}

export function arc3AntiClotReduction(breach) {
  const done = breach?.completedUnmakes ?? [];
  let sum = 0;
  for (const act of ARC3_UNMAKE_ACTS) {
    if (done.includes(act.id)) sum += act.antiClotFraction;
  }
  return Math.max(0, Math.min(ARC3_BALANCE.MAX_ANTI_CLOT_FRACTION, sum));
}

export function arc3UnmakeWrenchMultiplier(breach) {
  const done = breach?.completedUnmakes ?? [];
  let mult = 1.0;
  for (const act of ARC3_UNMAKE_ACTS) {
    if (done.includes(act.id)) mult *= act.wrenchMultiplier;
  }
  return mult;
}

export function arc3UnmakeRuptureMultiplier(breach) {
  const done = breach?.completedUnmakes ?? [];
  let mult = 1.0;
  for (const act of ARC3_UNMAKE_ACTS) {
    if (done.includes(act.id)) mult *= act.ruptureMultiplier;
  }
  return mult;
}

export function arc3BleedPressurePerSec(breach) {
  const done = breach?.completedUnmakes ?? [];
  let sum = 0;
  for (const act of ARC3_UNMAKE_ACTS) {
    if (done.includes(act.id)) sum += act.bleedPressurePerSec;
  }
  return sum;
}

export function arc3IsClotFrozen(breach) {
  return (breach?.clotFreezeRemainingSeconds ?? 0) > 0 || (breach?.frenzyRemainingSeconds ?? 0) > 0;
}

export function arc3ActiveClotPerSec(breach) {
  if (!breach || arc3IsInFinale(breach) || breach.submersionComplete || arc3IsClotFrozen(breach)) {
    return 0.0;
  }
  if ((breach.fracture ?? 0) <= 0.0) return 0.0;
  const idle = breach.secondsSinceLastAction ?? 0;
  if (idle < ARC3_BALANCE.CLOT_GRACE_SEC) return 0.0;
  const ramp = Math.max(0, Math.min(1, (idle - ARC3_BALANCE.CLOT_GRACE_SEC) / ARC3_BALANCE.CLOT_RAMP_SEC));
  const reducedBase = arc3ActiveStratum(breach).baseClotPerSec * (1.0 - arc3AntiClotReduction(breach));
  const netClot = Math.max(reducedBase * 0.25, reducedBase - arc3BleedPressurePerSec(breach) * 0.45);
  return netClot * (0.35 + 0.65 * ramp);
}

export function arc3IsActivelyClotting(breach) {
  return arc3ActiveClotPerSec(breach) > 0.05 && (breach?.fracture ?? 0) > 0.0;
}

export function arc3MomentumMultiplier(breach) {
  const charge = Math.max(0, Math.min(1, breach?.momentumCharge ?? 0));
  return 1.0 + charge * ARC3_BALANCE.MOMENTUM_MAX_BONUS;
}

export function arc3TemporaryForceMultiplier(breach) {
  return (
    ((breach?.ribForceRemainingSeconds ?? 0) > 0 ? ARC3_BALANCE.RIB_FORCE_MULTIPLIER : 1.0) *
    ((breach?.frenzyRemainingSeconds ?? 0) > 0 ? ARC3_BALANCE.FRENZY_MULTIPLIER : 1.0)
  );
}

export function arc3FloodLevel(breach) {
  if (!breach) return 0.02;
  if (breach.submersionComplete || (breach.finalPullsCompleted ?? 0) >= ARC3_BALANCE.FINAL_PULLS_TOTAL) {
    return 1.0;
  }
  if (arc3IsInFinale(breach)) {
    const table = [0.86, 0.9, 0.94, 0.97, 0.99, 1.0];
    return table[Math.min(5, Math.max(0, breach.finalPullsCompleted ?? 0))];
  }
  const lockedCount = breach.lockedWedges?.length ?? 0;
  const prevFloor = lockedCount === 0 ? 0.02 : arc3StratumByNumber(lockedCount).baseFloodLevel;
  const targetFloor = arc3ActiveStratum(breach).baseFloodLevel;
  const unmakeLift = ((breach.completedUnmakes?.length ?? 0) / 18.0) * 0.04;
  return Math.min(0.86, prevFloor + (targetFloor - prevFloor) * arc3StratumProgress(breach) * 0.72 + unmakeLift);
}

export function arc3TelemetryBlurFactor(breach) {
  if (!breach) return 0.0;
  if (breach.submersionComplete) return 1.0;
  if (arc3IsInFinale(breach)) {
    return Math.min(1, 0.85 + (breach.finalPullsCompleted ?? 0) * 0.03);
  }
  const s = breach.currentStratum ?? 1;
  const p = arc3StratumProgress(breach);
  if (s >= 5) return Math.min(1, 0.62 + p * 0.23);
  if (s === 4) return Math.min(1, 0.25 + p * 0.37);
  return 0.0;
}

export function arc3IsUnmakeVisible(breach, act) {
  const done = breach?.completedUnmakes ?? [];
  return done.includes(act.id) || act.minStratum <= (breach?.currentStratum ?? 1);
}

export function arc3CanPerformUnmake(breach, act) {
  if (!breach || !act || arc3IsInFinale(breach)) return false;
  const done = breach.completedUnmakes ?? [];
  return (
    !done.includes(act.id) &&
    act.minStratum <= (breach.currentStratum ?? 1) &&
    (breach.hemorrhage ?? 0) >= act.hemorrhageCost
  );
}

export function arc3LegacyHemorrhageMultiplier(state) {
  const jars = state?.ownedJars?.length ?? 0;
  const scars = state?.unlockedScarNodes?.length ?? 0;
  return 1.0 + (jars / 6.0) * 0.15 + (scars / 16.0) * 0.15;
}

export function arc3LegacyFractureMultiplier(state) {
  const corrs = state?.arc2?.discoveredCorroborations?.length ?? 0;
  return 1.0 + corrs * 0.03;
}

export function arc3WrenchFractureYield(state) {
  const breach = state?.arc3 ?? initialArc3State();
  return (
    ARC3_BALANCE.BASE_WRENCH_FRACTURE *
    arc3UnmakeWrenchMultiplier(breach) *
    arc3MomentumMultiplier(breach) *
    arc3TemporaryForceMultiplier(breach) *
    arc3LegacyFractureMultiplier(state)
  );
}

export function arc3WrenchHemorrhageYield(state) {
  const breach = state?.arc3 ?? initialArc3State();
  return (
    ARC3_BALANCE.BASE_WRENCH_HEMORRHAGE *
    arc3UnmakeWrenchMultiplier(breach) *
    (1.0 + (breach.momentumCharge ?? 0) * 0.4) *
    arc3TemporaryForceMultiplier(breach) *
    arc3LegacyHemorrhageMultiplier(state)
  );
}
