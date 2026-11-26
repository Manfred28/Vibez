/**
 * Shared multi-arc identifiers, Arc 1 Chamber catalog definitions, balance constants,
 * lookup helpers, and display formatters (1:1 parity with Android Kotlin models).
 */

export const PHASE_THRESHOLDS = Object.freeze([0, 75, 1600, 16000, 85000]);

export const GAME_ARCS = Object.freeze({
  ARC_1_CHAMBER: 'arc_1',
  ARC_2_INVESTIGATION: 'arc_2',
  ARC_3_CONVERGENCE: 'arc_3',
});

export const EXCISION_MILESTONES = Object.freeze([
  Object.freeze({
    number: 1,
    massThreshold: 210000,
    firstClearScarBonus: 1,
    code: 'FIRST SEVERANCE',
    unlockSummary: 'Unlocks the 5-tier Scar Tissue Surgical Skill Tree & Layered Grafts.',
  }),
  Object.freeze({
    number: 2,
    massThreshold: 1800000,
    firstClearScarBonus: 0,
    code: 'THE SUNKEN DRAIN',
    unlockSummary: 'Unlocks Floor Drain Grate Sealing, Relic Dredging & Sump-Siphon.',
  }),
  Object.freeze({
    number: 3,
    massThreshold: 24000000,
    firstClearScarBonus: 0,
    code: 'THE UNBRICKED SEAM',
    unlockSummary:
      'Unlocks Neighbor Aperture Echo Routing (0% / 25% / 50%), Echo Bursts & Colony Proliferation.',
  }),
  Object.freeze({
    number: 4,
    massThreshold: 320000000,
    firstClearScarBonus: 0,
    code: 'THE BREATH-HOLD',
    unlockSummary: 'Unlocks Valve Clamp (Marrow Bloom) & Choir-Lung Grafting.',
  }),
  Object.freeze({
    number: 5,
    massThreshold: 4500000000,
    firstClearScarBonus: 0,
    code: 'THE MEMBRANE AUDIT',
    unlockSummary: 'Unlocks The Ossified Throat & the 4-line Redacted Cult Dossier.',
  }),
  Object.freeze({
    number: 6,
    massThreshold: 160000000000,
    firstClearScarBonus: 0,
    code: 'PLEAD FOR SALVATION',
    unlockSummary: 'Opens the last wet seam — send a desperate plea for salvation upward into The Inquiry.',
  }),
]);

export const SCAR_NODES = Object.freeze([
  Object.freeze({
    id: 'first_incision',
    branch: 'root',
    tier: 0,
    cost: 1,
    prerequisite: null,
    name: 'FIRST INCISION',
    desc: 'The first mark remembers how to bleed; three paths open.',
  }),
  // SUTURE BRANCH (T1..T5)
  Object.freeze({
    id: 'calloused_palm',
    branch: 'suture',
    tier: 1,
    cost: 2,
    prerequisite: 'first_incision',
    name: 'CALLOUSED PALM',
    desc: 'Old hands wait at the valve before you even wake.',
  }),
  Object.freeze({
    id: 'twin_stitch',
    branch: 'suture',
    tier: 2,
    cost: 5,
    prerequisite: 'calloused_palm',
    name: 'TWIN STITCH',
    desc: 'Every turn of iron wakes a second tremor in the colony.',
  }),
  Object.freeze({
    id: 'colony_inertia',
    branch: 'suture',
    tier: 3,
    cost: 8,
    prerequisite: 'twin_stitch',
    name: 'COLONY INERTIA',
    desc: 'The flesh multiplies with less persuasion.',
  }),
  Object.freeze({
    id: 'membrane_recall',
    branch: 'suture',
    tier: 4,
    cost: 12,
    prerequisite: 'colony_inertia',
    name: 'MEMBRANE RECALL',
    desc: 'Lamps, sump-bladders, and a braided tendril already hang where you left them, pulling twice as hard.',
  }),
  Object.freeze({
    id: 'perpetual_seam',
    branch: 'suture',
    tier: 5,
    cost: 36,
    prerequisite: 'membrane_recall',
    name: 'PERPETUAL SEAM',
    desc: 'The valve turns on its own without breaking the quiet; every stitch and lung answers harder.',
  }),
  // STILLNESS BRANCH (T1..T5)
  Object.freeze({
    id: 'shorter_silence',
    branch: 'stillness',
    tier: 1,
    cost: 1,
    prerequisite: 'first_incision',
    name: 'SHORTER SILENCE',
    desc: 'The room falls quiet almost as soon as you let go, and the quiet gathers thicker.',
  }),
  Object.freeze({
    id: 'wax_sealed_memory',
    branch: 'stillness',
    tier: 2,
    cost: 2,
    prerequisite: 'shorter_silence',
    name: 'WAX-SEALED MEMORY',
    desc: "Wax softens the quiet's price; fresh grafts knit twice as fast.",
  }),
  Object.freeze({
    id: 'deep_weeping',
    branch: 'stillness',
    tier: 3,
    cost: 3,
    prerequisite: 'wax_sealed_memory',
    name: 'DEEP WEEPING',
    desc: 'Even while you press the iron, the lamps keep weeping.',
  }),
  Object.freeze({
    id: 'hollow_lung_choir',
    branch: 'stillness',
    tier: 4,
    cost: 9,
    prerequisite: 'deep_weeping',
    name: 'HOLLOW LUNG CHOIR',
    desc: 'Hauled relics never sink back beneath the grate; in quiet, lamps and lungs draw twice as deep.',
  }),
  Object.freeze({
    id: 'catatonic_tide',
    branch: 'stillness',
    tier: 5,
    cost: 36,
    prerequisite: 'hollow_lung_choir',
    name: 'CATATONIC TIDE',
    desc: 'Quiet settles in a heartbeat, swells like a deep tide over every vein, and keeps the first three jars sealed.',
  }),
  // MARROW BRANCH (T1..T5)
  Object.freeze({
    id: 'softened_mortar',
    branch: 'marrow',
    tier: 1,
    cost: 2,
    prerequisite: 'first_incision',
    name: 'SOFTENED MORTAR',
    desc: 'The plaster gives way easily to what pushes through.',
  }),
  Object.freeze({
    id: 'auto_tendril_suture',
    branch: 'marrow',
    tier: 2,
    cost: 5,
    prerequisite: 'softened_mortar',
    name: 'AUTO-TENDRIL SUTURE',
    desc: 'Spare hands and leeches braid themselves without your consent.',
  }),
  Object.freeze({
    id: 'echo_ossuary',
    branch: 'marrow',
    tier: 3,
    cost: 9,
    prerequisite: 'auto_tendril_suture',
    name: 'ECHO OSSUARY',
    desc: 'The sump swells faster; the neighbor answers sooner and louder.',
  }),
  Object.freeze({
    id: 'bloom_incision',
    branch: 'marrow',
    tier: 4,
    cost: 16,
    prerequisite: 'echo_ossuary',
    name: 'BLOOM INCISION',
    desc: 'The clamped wall splits sooner and bleeds wider through every fissure.',
  }),
  Object.freeze({
    id: 'bone_resonance',
    branch: 'marrow',
    tier: 5,
    cost: 40,
    prerequisite: 'bloom_incision',
    name: 'BONE RESONANCE',
    desc: 'The masonry hums in tune with every unsealed line and drowned relic.',
  }),
]);

export const REPEATABLE_SCAR_GRAFTS = Object.freeze([
  Object.freeze({
    id: 'keloid_thickening',
    baseCost: 1,
    costGrowth: 1.45,
    name: 'KELOID THICKENING',
    desc: 'Layered scar ridges thicken every flow in the room.',
  }),
  Object.freeze({
    id: 'sediment_well',
    baseCost: 1,
    costGrowth: 1.45,
    name: 'SEDIMENT WELL',
    desc: 'Quiet gathers thicker and asks less wax to bottle or bind.',
  }),
  Object.freeze({
    id: 'scar_harvest_hook',
    baseCost: 1,
    costGrowth: 1.55,
    name: 'SCAR-HARVEST HOOK',
    desc: 'Curved steel brings up more scar tissue on every excision.',
  }),
]);

export const DREDGED_RELICS = Object.freeze([
  Object.freeze({
    id: 'patient_wristband',
    name: 'WRISTBAND',
    hint: 'Sodden tag #14 — the valve pulls heavier from beneath the mortar.',
    ichorMultiplier: 1.28,
    stillnessMultiplier: 1.12,
  }),
  Object.freeze({
    id: 'brass_ward_key',
    name: 'BRASS KEY',
    hint: 'Bent ward key — the room settles into quiet deeper than before.',
    ichorMultiplier: 1.35,
    stillnessMultiplier: 1.18,
  }),
  Object.freeze({
    id: 'bleeding_logbook',
    name: 'LOGBOOK',
    hint: 'Bled-ink ledger — every tending thing works a fraction harder.',
    ichorMultiplier: 1.45,
    stillnessMultiplier: 1.25,
  }),
]);

export const CULT_DOSSIER_LINES = Object.freeze([
  Object.freeze({
    id: 'dossier_line_1',
    lineNumber: 1,
    title: 'CLAUSE I // HYDRO-WARD TRANSFER',
    text: '"Hydro-Ward Sub-Basement IV was never decommissioned; municipal records transferred to private custody."',
    siphonCost: 2,
    tendrilCost: 0,
    requiresJar: false,
    ichorMultiplier: 1.35,
  }),
  Object.freeze({
    id: 'dossier_line_2',
    lineNumber: 2,
    title: 'CLAUSE II // MEMORY IN BRINE',
    text: '"Bottled stillness slows seepage beneath the mains. Attendants must not retain outside schedules."',
    siphonCost: 3,
    tendrilCost: 1,
    requiresJar: true,
    ichorMultiplier: 1.4,
  }),
  Object.freeze({
    id: 'dossier_line_3',
    lineNumber: 3,
    title: 'CLAUSE III // DAY 14 SEALING',
    text: '"Brick the corridor threshold on Day 14. Leave one recessed valve, one grate, one ledger."',
    siphonCost: 4,
    tendrilCost: 2,
    requiresJar: true,
    ichorMultiplier: 1.45,
  }),
  Object.freeze({
    id: 'dossier_line_4',
    lineNumber: 4,
    title: 'CLAUSE IV // QUIET TENANCY',
    text: '"An attendant who ceases to distinguish threshold from wall no longer requires egress. Leave one wet seam for petitions and rain."',
    siphonCost: 5,
    tendrilCost: 3,
    requiresJar: true,
    ichorMultiplier: 1.55,
  }),
]);

export const CREATURE_TYPES = Object.freeze({
  SUTURE_HAND: Object.freeze({
    id: 'suture_hand',
    code: 'SPEC-01',
    name: 'SUTURE-HAND',
    desc: 'Stitched fingers that will not stop turning the iron.',
    baseCost: 25.0,
    costGrowth: 1.24,
    baseIchorPerSec: 0.6,
    unlockIchorThreshold: 25,
    requiredMilestone: 0,
    portrait: './assets/img_suture_hand.jpg',
    portraitAlt: 'Stitched pale anatomical Suture-Hand specimen',
  }),
  LEECH_CLERK: Object.freeze({
    id: 'leech_clerk',
    code: 'SPEC-02',
    name: 'LEECH-CLERK',
    desc: 'Blind wet mouth grazing across the counter glass.',
    baseCost: 180.0,
    costGrowth: 1.26,
    baseIchorPerSec: 2.4,
    unlockIchorThreshold: 75,
    requiredMilestone: 0,
    portrait: './assets/img_leech_clerk.jpg',
    portraitAlt: 'Blind segmented Leech-Clerk specimen',
  }),
  WEEPING_LAMP: Object.freeze({
    id: 'weeping_lamp',
    code: 'SPEC-03',
    name: 'WEEPING LAMP',
    desc: 'Only weeps when your hands leave the room alone.',
    baseCost: 1190.0,
    costGrowth: 1.32,
    baseIchorPerSec: 6.0,
    unlockIchorThreshold: 1000,
    requiredMilestone: 0,
    portrait: null,
    portraitAlt: 'Suspended glass Weeping Lamp specimen',
  }),
  SUMP_SIPHON: Object.freeze({
    id: 'sump_siphon',
    code: 'SPEC-04',
    name: 'SUMP-SIPHON',
    desc: 'Ribbed leather bladder pulling what settled beneath the tiles.',
    baseCost: 28000.0,
    costGrowth: 1.28,
    baseIchorPerSec: 42.0,
    unlockIchorThreshold: 15000,
    requiredMilestone: 2,
    portrait: null,
    portraitAlt: 'Ribbed Sump-Siphon specimen tethered to the floor grate',
  }),
});

export const CREATURE_LIST = Object.freeze([
  CREATURE_TYPES.SUTURE_HAND,
  CREATURE_TYPES.LEECH_CLERK,
  CREATURE_TYPES.WEEPING_LAMP,
  CREATURE_TYPES.SUMP_SIPHON,
]);

export const SPECIMEN_JARS = Object.freeze({
  YESTERDAYS_DUST: Object.freeze({
    id: 'yesterdays_dust',
    name: "YESTERDAY'S DUST",
    effect: 'The iron valve turns heavier, and yields thicker.',
    stillnessCost: 24.0,
    unlockPhase: 0,
    requiresCycleOrMilestone: false,
  }),
  UNSPENT_BREATH: Object.freeze({
    id: 'unspent_breath',
    name: 'UNSPENT BREATH',
    effect: 'Hands and leeches draw from what you held back.',
    stillnessCost: 65.0,
    unlockPhase: 0,
    requiresCycleOrMilestone: false,
  }),
  ALMOST_A_NAME: Object.freeze({
    id: 'almost_a_name',
    name: 'A NAME YOU ALMOST REMEMBER',
    effect: 'Lamps weep warmer; the cracks part wider.',
    stillnessCost: 140.0,
    unlockPhase: 0,
    requiresCycleOrMilestone: false,
  }),
  SALT_FROM_THE_GROUT: Object.freeze({
    id: 'salt_from_the_grout',
    name: 'SALT FROM THE GROUT',
    effect: 'Every tending creature drinks brine from the seams.',
    stillnessCost: 320.0,
    unlockPhase: 3,
    requiresCycleOrMilestone: false,
  }),
  A_BORROWED_PULSE: Object.freeze({
    id: 'a_borrowed_pulse',
    name: 'A BORROWED PULSE',
    effect: "The room's quiet beats twice beneath your palm.",
    stillnessCost: 750.0,
    unlockPhase: 4,
    requiresCycleOrMilestone: false,
  }),
  THE_UNCOUNTED_HOUR: Object.freeze({
    id: 'the_uncounted_hour',
    name: 'THE UNCOUNTED HOUR',
    effect: 'Even the wall forgets how long you have been here.',
    stillnessCost: 1800.0,
    unlockPhase: 0,
    requiresCycleOrMilestone: true,
  }),
});

export const SPECIMEN_JAR_LIST = Object.freeze([
  SPECIMEN_JARS.YESTERDAYS_DUST,
  SPECIMEN_JARS.UNSPENT_BREATH,
  SPECIMEN_JARS.ALMOST_A_NAME,
  SPECIMEN_JARS.SALT_FROM_THE_GROUT,
  SPECIMEN_JARS.A_BORROWED_PULSE,
  SPECIMEN_JARS.THE_UNCOUNTED_HOUR,
]);

export const WALL_ANOMALY_TYPES = Object.freeze({
  PALE_TOOTH: Object.freeze({
    id: 'pale_tooth',
    minPhase: 1,
    label: 'PULL',
    ariaLabel: 'Pull pale tooth protruding from plaster crack',
    dripLine: 'Enamel gives way; the iron valve drinks deeper.',
  }),
  GLASS_EYE: Object.freeze({
    id: 'glass_eye',
    minPhase: 2,
    label: 'WIPE',
    ariaLabel: 'Wipe condensation from blinking glass eye',
    dripLine: 'The glass blinks clean; the colony quickens under its gaze.',
  }),
  SWOLLEN_NODULE: Object.freeze({
    id: 'swollen_nodule',
    minPhase: 3,
    label: 'PROD',
    ariaLabel: 'Prod swollen nodule pulsing along fissure',
    dripLine: 'The blister splits warm; ichor and quiet seep down the grout.',
  }),
  CHITIN_RIB: Object.freeze({
    id: 'chitin_rib',
    minPhase: 4,
    label: 'PRY',
    ariaLabel: 'Pry cracked calcified rib parting the chamber wall',
    dripLine: 'The pale rib snaps free; everything in the wall surges.',
  }),
});

export const WALL_ANOMALY_LIST = Object.freeze([
  WALL_ANOMALY_TYPES.PALE_TOOTH,
  WALL_ANOMALY_TYPES.GLASS_EYE,
  WALL_ANOMALY_TYPES.SWOLLEN_NODULE,
  WALL_ANOMALY_TYPES.CHITIN_RIB,
]);

export const ORGAN_BALANCE = Object.freeze({
  STILLNESS_IDLE_DELAY_SEC: 3.0,
  SHORTER_SILENCE_DELAY_SEC: 1.5,
  CATATONIC_TIDE_DELAY_SEC: 0.8,
  LAMP_STILLNESS_PER_SEC: 0.45,
  GAZE_DURATION_SEC: 12.0,
  GAZE_PASSIVE_MULTIPLIER: 1.6,
  YESTERDAYS_DUST_TAP_BONUS: 3.0,
  UNSPENT_BREATH_MULTIPLIER: 1.75,
  ALMOST_A_NAME_MULTIPLIER: 1.8,
  SALT_GROUT_BONUS: 0.45,
  BORROWED_PULSE_BONUS: 0.55,
  UNCOUNTED_HOUR_BONUS: 0.75,
  WAX_STILLNESS_COST_MULTIPLIER: 0.7,
  WAX_GRAFT_COOLDOWN_MULTIPLIER: 0.5,
  GRAFT_SUTURE_COST: 2,
  GRAFT_LEECH_COST: 2,
  GRAFT_BASE_STILLNESS_COST: 35.0,
  GRAFT_STILLNESS_GROWTH: 1.55,
  GRAFT_COOLDOWN_SEC: 6.0,
  GRAFT_ICHOR_PER_SEC: 18.0,
  GRAFT_STILLNESS_PER_SEC: 0.7,
  CHOIR_LUNG_LAMP_COST: 2,
  CHOIR_LUNG_TENDRIL_COST: 1,
  CHOIR_LUNG_BASE_STILLNESS_COST: 180.0,
  CHOIR_LUNG_STILLNESS_GROWTH: 1.65,
  CHOIR_LUNG_COOLDOWN_SEC: 10.0,
  CHOIR_LUNG_ICHOR_PER_SEC: 160.0,
  CHOIR_LUNG_STILLNESS_PER_SEC: 3.5,
  EXCISION_ICHOR_THRESHOLD: 210000,
  SCAR_CURVE_BASE_SCALE: 2.0,
  SCAR_CURVE_EXPONENT: 0.3,
  MILESTONE_ICHOR_RESONANCE: 0.2,
  MILESTONE_STILLNESS_RESONANCE: 0.15,
  SCAR_RESONANCE_SQRT_FACTOR: 0.25,
  SCAR_STILLNESS_SQRT_FACTOR: 0.18,
  KELOID_ICHOR_PER_RANK: 0.18,
  SEDIMENT_STILLNESS_PER_RANK: 0.16,
  SEDIMENT_WELL_COST_FACTOR: 0.96,
  SEDIMENT_WELL_COST_FLOOR: 0.68,
  SCAR_HARVEST_HOOK_PER_RANK: 0.12,
  PERPETUAL_SEAM_INTERVAL_SEC: 0.75,
  FIRST_ANOMALY_DELAY_SEC: 14.0,
  ANOMALY_SPAWN_INTERVAL_SEC: 26.0,
  SOFTENED_MORTAR_SPEED_MULT: 1.6,
  MAX_ACTIVE_ANOMALIES: 3,
  TOOTH_BASE_ICHOR_REWARD: 25.0,
  NODULE_BASE_ICHOR_REWARD: 220.0,
  NODULE_BASE_STILLNESS_REWARD: 6.0,
  RIB_BASE_ICHOR_REWARD: 1400.0,
  RIB_BASE_STILLNESS_REWARD: 14.0,
  RIB_GAZE_DURATION_SEC: 6.0,
  ECHO_PULSE_INTERVAL_SEC: 12.0,
  ECHO_OSSUARY_INTERVAL_SEC: 9.0,
  VALVE_CLAMP_DURATION_SEC: 20.0,
  BLOOM_INCISION_CLAMP_DURATION_SEC: 15.0,
  MARROW_BLOOM_DURATION_SEC: 25.0,
  BLOOM_INCISION_DURATION_SEC: 32.0,
  MARROW_BLOOM_MULTIPLIER: 2.2,
  BLOOM_INCISION_MULTIPLIER: 3.1,
});

export const ANOMALY_ANCHORS = Object.freeze([
  [0.14, 0.26],
  [0.86, 0.28],
  [0.14, 0.74],
  [0.86, 0.72],
  [0.2, 0.5],
  [0.8, 0.52],
]);

export const DEFAULT_PHASE_NAMES = Object.freeze([
  'THE COUNTING ROOM',
  'UNCANNY DRIFT',
  'APPARATUS & FLESH',
  'COLLAPSE OF MEANING',
  'THE ENDLESS HOLLOW',
  'THE INQUIRY',
  'FILE #00',
]);

export function creatureById(id) {
  return CREATURE_LIST.find((c) => c.id === id) ?? null;
}

export function jarById(id) {
  return SPECIMEN_JAR_LIST.find((j) => j.id === id) ?? null;
}

export function scarNodeById(id) {
  return SCAR_NODES.find((n) => n.id === id) ?? null;
}

export function repeatableGraftById(id) {
  return REPEATABLE_SCAR_GRAFTS.find((g) => g.id === id) ?? null;
}

export function dossierLineById(id) {
  return CULT_DOSSIER_LINES.find((l) => l.id === id) ?? null;
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

export function formatValveYield(value) {
  if (value < 10000) return formatCompactDecimal(value);
  let scaled = value;
  let suffix = '';
  if (value < 1000000) {
    scaled = value / 1000;
    suffix = 'K';
  } else if (value < 1000000000) {
    scaled = value / 1000000;
    suffix = 'M';
  } else if (value < 1000000000000) {
    scaled = value / 1000000000;
    suffix = 'B';
  } else {
    scaled = value / 1000000000000;
    suffix = 'T';
  }
  const maxDecimals = scaled < 10 ? 2 : scaled < 100 ? 1 : 0;
  const formatted = scaled.toLocaleString('en-US', {
    minimumFractionDigits: 0,
    maximumFractionDigits: maxDecimals,
    useGrouping: false,
  });
  return `${formatted}${suffix}`;
}

export function formatMultiplier(value) {
  const rounded = Math.round(value * 100) / 100;
  return rounded.toLocaleString('en-US', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
    useGrouping: false,
  });
}
