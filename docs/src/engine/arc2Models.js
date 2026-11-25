/**
 * Arc 2 Investigator Desk models, balance constants, initial state factory, and pure derived math.
 * Isolated from Arc 1 Chamber mechanics so Arc 2 can evolve cleanly without risking Arc 1 regressions.
 */

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

export const DESK_ANCHORS = Object.freeze([
  [0.16, 0.26],
  [0.84, 0.28],
  [0.17, 0.74],
  [0.83, 0.72],
  [0.24, 0.5],
  [0.77, 0.52],
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

export function contactById(id) {
  return INVESTIGATOR_CONTACTS.find((c) => c.id === id) ?? null;
}

export function deductionById(id) {
  return CASE_DEDUCTIONS.find((d) => d.id === id) ?? null;
}

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
