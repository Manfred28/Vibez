/**
 * Pure deterministic multi-arc game engine for "The Tending of the Unnamed Organ" (Web / GitHub Pages).
 * Maintains 1:1 mechanical, mathematical, and dialogue-trigger parity with the Android Kotlin engine
 * (`ScarSkillTree.kt`, `OrganGameModels.kt`, `OrganGameEngine.kt`, `Arc2InvestigatorModels.kt`,
 * `Arc2InvestigatorEngine.kt`, `OrganDevEngine.kt`).
 *
 * Narrative fragments are never hardcoded here — they are loaded at runtime from `./assets/dialogues.json`.
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
    firstClearScarBonus: 0,
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

export const INVESTIGATOR_CONTACTS = Object.freeze([
  Object.freeze({
    id: 'night_archivist',
    code: 'DESK-01',
    name: 'NIGHT ARCHIVIST',
    desc: 'Cross-indexes missing caretaker punch cards from basement water logs.',
    baseCost: 20.0,
    costGrowth: 1.22,
    baseLeadsPerSec: 1.2,
    unlockLeadsThreshold: 15,
  }),
  Object.freeze({
    id: 'bribed_orderly',
    code: 'DESK-02',
    name: 'BRIBED ORDERLY',
    desc: 'Slides damp Hydro-Ward intake receipts under your office door at 2 AM.',
    baseCost: 150.0,
    costGrowth: 1.25,
    baseLeadsPerSec: 6.5,
    unlockLeadsThreshold: 90,
  }),
  Object.freeze({
    id: 'microfilm_scanner',
    code: 'DESK-03',
    name: 'MICROFILM READER',
    desc: 'Hums through bricked municipal blueprints looking for sealed sub-basements.',
    baseCost: 950.0,
    costGrowth: 1.28,
    baseLeadsPerSec: 28.0,
    unlockLeadsThreshold: 700,
  }),
  Object.freeze({
    id: 'audio_transcriber',
    code: 'DESK-04',
    name: 'REEL TRANSCRIBER',
    desc: 'Logs rhythmic valve clicks off water-warped magnetic tape.',
    baseCost: 5500.0,
    costGrowth: 1.3,
    baseLeadsPerSec: 120.0,
    unlockLeadsThreshold: 4000,
  }),
]);

export const CASE_DEDUCTIONS = Object.freeze([
  Object.freeze({
    id: 'hydro_ward_invoices',
    name: 'HYDRO-WARD INVOICES',
    effect: 'Each stamped page turns up sharper ink.',
    silenceCost: 18.0,
  }),
  Object.freeze({
    id: 'missing_caretaker_roster',
    name: 'CARETAKER ROSTER',
    effect: 'The contacts find names twice as fast.',
    silenceCost: 50.0,
  }),
  Object.freeze({
    id: 'mortar_sample_match',
    name: 'MORTAR SAMPLE MATCH',
    effect: 'Late-night quiet deepens; oddities surface clearer.',
    silenceCost: 115.0,
  }),
]);

export const DESK_ANOMALY_TYPES = Object.freeze({
  EVIDENCE_ENVELOPE: Object.freeze({
    id: 'evidence_envelope',
    minChapter: 0,
    label: 'SLIT',
    ariaLabel: 'Slit open damp evidence envelope on desk',
  }),
  FOGGED_SPECTACLES: Object.freeze({
    id: 'fogged_spectacles',
    minChapter: 1,
    label: 'WIPE',
    ariaLabel: 'Wipe condensation from fogged spectacles',
  }),
  RED_STRING: Object.freeze({
    id: 'red_string',
    minChapter: 2,
    label: 'TRACE',
    ariaLabel: 'Trace stiff red pinboard thread across map',
  }),
  REEL_TAPE: Object.freeze({
    id: 'reel_tape',
    minChapter: 3,
    label: 'PLAY',
    ariaLabel: 'Play warped magnetic reel tape on desk',
  }),
});

export const DESK_ANOMALY_LIST = Object.freeze([
  DESK_ANOMALY_TYPES.EVIDENCE_ENVELOPE,
  DESK_ANOMALY_TYPES.FOGGED_SPECTACLES,
  DESK_ANOMALY_TYPES.RED_STRING,
  DESK_ANOMALY_TYPES.REEL_TAPE,
]);

export const INVESTIGATION_CHAPTERS = Object.freeze([
  Object.freeze({
    number: 1,
    title: 'THE MISSING CARETAKERS',
    summary: 'Three Hydro-Ward attendants vanished on Day 14—leaving bled-ink signatures identical to yours.',
    leadsCost: 400,
    silenceCost: 0,
  }),
  Object.freeze({
    number: 2,
    title: 'THE HYDRO-WARD CULT',
    summary: 'Private invoices for wax jars, iron valves, and quiet attendants referred to only as Vessels.',
    leadsCost: 4500,
    silenceCost: 20,
  }),
  Object.freeze({
    number: 3,
    title: 'THE BRICK-WORK BLUEPRINT',
    summary: 'The basement survey shows no exterior door at all—only brick laid three winters ago.',
    leadsCost: 35000,
    silenceCost: 65,
  }),
  Object.freeze({
    number: 4,
    title: 'FILE #00: SUBJECT INTAKE',
    summary: 'Distinguishing mark: pale transverse scar across left thumb. Status: Still seated at valve.',
    leadsCost: 180000,
    silenceCost: 140,
  }),
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

export const ARC2_BALANCE = Object.freeze({
  SILENCE_IDLE_DELAY_SEC: 2.5,
  BASE_DESK_SILENCE_PER_SEC: 0.65,
  CONTACT_SILENCE_PER_UNIT: 0.25,
  FOCUS_DURATION_SEC: 14.0,
  FOCUS_MULTIPLIER: 1.8,
  INVOICES_TAP_BONUS: 4.0,
  ROSTER_CONTACT_MULTIPLIER: 2.0,
  MORTAR_SILENCE_AND_ANOMALY_MULTIPLIER: 2.2,
  FIRST_DESK_ANOMALY_DELAY_SEC: 10.0,
  DESK_ANOMALY_INTERVAL_SEC: 20.0,
  MAX_DESK_ANOMALIES: 3,
  ENVELOPE_BASE_LEADS: 40.0,
  RED_STRING_BASE_LEADS: 320.0,
  RED_STRING_BASE_SILENCE: 9.0,
  REEL_TAPE_BASE_LEADS: 2400.0,
  REEL_TAPE_BASE_SILENCE: 20.0,
});

const ANOMALY_ANCHORS = Object.freeze([
  [0.14, 0.26],
  [0.86, 0.28],
  [0.14, 0.74],
  [0.86, 0.72],
  [0.2, 0.5],
  [0.8, 0.52],
]);

const DESK_ANCHORS = Object.freeze([
  [0.16, 0.26],
  [0.84, 0.28],
  [0.17, 0.74],
  [0.83, 0.72],
  [0.24, 0.5],
  [0.77, 0.52],
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

export function initialArc2State() {
  return {
    leads: 0.0,
    lifetimeLeads: 0.0,
    silence: 0.0,
    lifetimeSilence: 0.0,
    complicity: 0.05,
    contacts: {},
    ownedDeductions: [],
    completedChapters: 0,
    activeDeskAnomalies: [],
    deskAnomalyInteractions: {},
    secondsSinceLastAction: 0.0,
    isSilenceActive: false,
    focusRemainingSeconds: 0.0,
    secondsUntilNextAnomaly: ARC2_BALANCE.FIRST_DESK_ANOMALY_DELAY_SEC,
    nextAnomalyId: 1,
    fileZeroRevealed: false,
  };
}

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

export function contactById(id) {
  return INVESTIGATOR_CONTACTS.find((c) => c.id === id) ?? null;
}

export function deductionById(id) {
  return CASE_DEDUCTIONS.find((d) => d.id === id) ?? null;
}

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
  return currentBaseScarHarvest(state);
}

/* ==================== ARC 2 DERIVED HELPERS ==================== */

export function arc2ContactCount(desk, contactId) {
  return desk.contacts?.[contactId] ?? 0;
}

export function arc2ContactCost(desk, contactOrId) {
  const spec = typeof contactOrId === 'string' ? contactById(contactOrId) : contactOrId;
  if (!spec) return 0;
  return Math.floor(spec.baseCost * Math.pow(spec.costGrowth, arc2ContactCount(desk, spec.id)));
}

export function arc2FocusMultiplier(desk) {
  return desk.focusRemainingSeconds > 0 ? ARC2_BALANCE.FOCUS_MULTIPLIER : 1.0;
}

export function arc2ChapterMultiplier(desk) {
  return 1.0 + desk.completedChapters * 0.65;
}

export function arc2InspectYield(desk) {
  const invoiceBonus = desk.ownedDeductions.includes('hydro_ward_invoices')
    ? ARC2_BALANCE.INVOICES_TAP_BONUS
    : 0.0;
  const envelopeBonus = (desk.deskAnomalyInteractions?.evidence_envelope ?? 0) * 1.0;
  return (1.0 + invoiceBonus + envelopeBonus) * arc2ChapterMultiplier(desk);
}

export function arc2EffectiveContactRate(desk, contactOrId) {
  const spec = typeof contactOrId === 'string' ? contactById(contactOrId) : contactOrId;
  if (!spec) return 0.0;
  const rosterMult = desk.ownedDeductions.includes('missing_caretaker_roster')
    ? ARC2_BALANCE.ROSTER_CONTACT_MULTIPLIER
    : 1.0;
  return spec.baseLeadsPerSec * rosterMult * arc2ChapterMultiplier(desk) * arc2FocusMultiplier(desk);
}

export function arc2LeadsPerSecond(desk) {
  let sum = 0.0;
  for (const c of INVESTIGATOR_CONTACTS) {
    sum += arc2ContactCount(desk, c.id) * arc2EffectiveContactRate(desk, c.id);
  }
  return sum;
}

export function arc2SilencePerSecond(desk) {
  if (!desk.isSilenceActive) return 0.0;
  const totalContacts = Object.values(desk.contacts ?? {}).reduce((a, b) => a + b, 0);
  const mortarMult = desk.ownedDeductions.includes('mortar_sample_match')
    ? ARC2_BALANCE.MORTAR_SILENCE_AND_ANOMALY_MULTIPLIER
    : 1.0;
  const base = ARC2_BALANCE.BASE_DESK_SILENCE_PER_SEC + totalContacts * ARC2_BALANCE.CONTACT_SILENCE_PER_UNIT;
  return base * mortarMult * arc2FocusMultiplier(desk);
}

export function arc2NextChapter(desk) {
  return INVESTIGATION_CHAPTERS.find((ch) => ch.number === desk.completedChapters + 1) ?? null;
}

export function arc2CanConnectNextChapter(desk) {
  const next = arc2NextChapter(desk);
  if (!next) return false;
  return desk.leads >= next.leadsCost && desk.silence >= next.silenceCost;
}

/* ==================== INITIAL STATE & PROGRESSION ==================== */

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
      return state.arc2.completedChapters >= trigger.threshold;
    case 'desk_anomaly_interacted':
      return (state.arc2.deskAnomalyInteractions?.[trigger.target] ?? 0) >= trigger.threshold;
    default:
      return false;
  }
}

function finalizeProgression(state, catalog) {
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

/* ==================== ARC 1 ENGINE ACTIONS ==================== */

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

export function tick(state, deltaSeconds, catalog) {
  if (deltaSeconds <= 0.0) return state;
  if (state.activeArc === GAME_ARCS.ARC_2_INVESTIGATION) {
    return tickArc2(state, deltaSeconds, catalog);
  }
  if (state.activeArc === GAME_ARCS.ARC_3_CONVERGENCE) return state;

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

/* ==================== ARC 2 INVESTIGATOR ENGINE ==================== */

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

function tickArc2(state, deltaSeconds, catalog) {
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

/* ==================== CLINICAL DEV CONSOLE HELPERS ==================== */

export function devSetActiveArc(state, arcId, catalog) {
  let adjusted = { ...state, activeArc: arcId };
  if (arcId === GAME_ARCS.ARC_2_INVESTIGATION) {
    adjusted.highestExcisionMilestone = Math.max(state.highestExcisionMilestone, 6);
  } else if (arcId === GAME_ARCS.ARC_3_CONVERGENCE) {
    adjusted.highestExcisionMilestone = Math.max(state.highestExcisionMilestone, 6);
    adjusted.arc2 = {
      ...state.arc2,
      completedChapters: 4,
      fileZeroRevealed: true,
      complicity: 1.0,
    };
  }
  return finalizeProgression(adjusted, catalog);
}

export function devSetHighestMilestone(state, milestone, catalog) {
  const clamped = Math.max(0, Math.min(6, Math.floor(milestone)));
  const adjusted = {
    ...state,
    highestExcisionMilestone: clamped,
    cycle: clamped > 0 ? Math.max(state.cycle, clamped) : state.cycle,
  };
  return finalizeProgression(adjusted, catalog);
}

export function devAssignArc1Resources(state, { ichor, stillness, availableScar }, catalog) {
  const nextIchor = ichor != null ? Math.max(0, Number(ichor) || 0) : state.ichor;
  const nextStillness = stillness != null ? Math.max(0, Number(stillness) || 0) : state.stillness;
  const nextAvailScar =
    availableScar != null ? Math.max(0, Math.floor(Number(availableScar) || 0)) : state.availableScarTissue;
  const adjusted = {
    ...state,
    ichor: nextIchor,
    cycleLifetimeIchor: Math.max(state.cycleLifetimeIchor, nextIchor),
    stillness: nextStillness,
    cycleLifetimeStillness: Math.max(state.cycleLifetimeStillness, nextStillness),
    availableScarTissue: nextAvailScar,
    scarTissue: Math.max(state.scarTissue, nextAvailScar),
  };
  return finalizeProgression(adjusted, catalog);
}

export function devSetAllScarNodes(state, unlockAll, catalog) {
  const adjusted = {
    ...state,
    unlockedScarNodes: unlockAll ? SCAR_NODES.map((n) => n.id) : [],
    repeatableScarRanks: unlockAll ? { ...(state.repeatableScarRanks ?? {}) } : {},
    highestExcisionMilestone: unlockAll
      ? Math.max(state.highestExcisionMilestone, 1)
      : state.highestExcisionMilestone,
  };
  return finalizeProgression(adjusted, catalog);
}

export function devAssignArc2Resources(
  state,
  { leads, silence, complicity, completedChapters },
  catalog,
) {
  const desk = state.arc2;
  const nextLeads = leads != null ? Math.max(0, Number(leads) || 0) : desk.leads;
  const nextSilence = silence != null ? Math.max(0, Number(silence) || 0) : desk.silence;
  const nextChapters =
    completedChapters != null
      ? Math.max(0, Math.min(4, Math.floor(Number(completedChapters) || 0)))
      : desk.completedChapters;
  const nextComplicity =
    complicity != null ? Math.max(0.05, Math.min(1.0, Number(complicity) || 0.05)) : desk.complicity;
  const nextDesk = {
    ...desk,
    leads: nextLeads,
    lifetimeLeads: Math.max(desk.lifetimeLeads, nextLeads),
    silence: nextSilence,
    lifetimeSilence: Math.max(desk.lifetimeSilence, nextSilence),
    complicity: nextComplicity,
    completedChapters: nextChapters,
    fileZeroRevealed: nextChapters >= 4 || desk.fileZeroRevealed,
  };
  return finalizeProgression({ ...state, arc2: nextDesk }, catalog);
}

export function devSpawnWallAnomaly(state) {
  return spawnImmediateWallAnomaly(state, true);
}

export function devSpawnDeskAnomaly(state) {
  return { ...state, arc2: spawnImmediateDeskAnomaly(state.arc2) };
}

export function devResetAll(catalog) {
  return initialState(catalog);
}

/* ==================== SERIALIZATION (V1 -> V2 COMPATIBLE) ==================== */

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
    arc2: state.arc2,
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

    const rawArc2 = data.arc2 && typeof data.arc2 === 'object' ? data.arc2 : {};
    const arc2 = {
      ...initialArc2State(),
      leads: Math.max(0, Number(rawArc2.leads) || 0),
      lifetimeLeads: Math.max(0, Number(rawArc2.lifetimeLeads) || 0),
      silence: Math.max(0, Number(rawArc2.silence) || 0),
      lifetimeSilence: Math.max(0, Number(rawArc2.lifetimeSilence) || 0),
      complicity: Math.max(0.05, Math.min(1.0, Number(rawArc2.complicity) || 0.05)),
      contacts: rawArc2.contacts && typeof rawArc2.contacts === 'object' ? { ...rawArc2.contacts } : {},
      ownedDeductions: Array.isArray(rawArc2.ownedDeductions) ? [...rawArc2.ownedDeductions] : [],
      completedChapters: Math.max(0, Math.min(4, Math.floor(Number(rawArc2.completedChapters) || 0))),
      activeDeskAnomalies: Array.isArray(rawArc2.activeDeskAnomalies)
        ? [...rawArc2.activeDeskAnomalies]
        : [],
      deskAnomalyInteractions:
        rawArc2.deskAnomalyInteractions && typeof rawArc2.deskAnomalyInteractions === 'object'
          ? { ...rawArc2.deskAnomalyInteractions }
          : {},
      secondsSinceLastAction: Math.max(0, Number(rawArc2.secondsSinceLastAction) || 0),
      isSilenceActive: Boolean(rawArc2.isSilenceActive),
      focusRemainingSeconds: Math.max(0, Number(rawArc2.focusRemainingSeconds) || 0),
      secondsUntilNextAnomaly: Math.max(
        0,
        Number(rawArc2.secondsUntilNextAnomaly) || ARC2_BALANCE.FIRST_DESK_ANOMALY_DELAY_SEC,
      ),
      nextAnomalyId: Math.max(1, Math.floor(Number(rawArc2.nextAnomalyId) || 1)),
      fileZeroRevealed: Boolean(rawArc2.fileZeroRevealed),
    };

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
