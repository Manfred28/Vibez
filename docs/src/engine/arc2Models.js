/**
 * Arc 2 Investigator Desk models, 5-Case Slow Boil progression, 10 Exhibits, 10 Curated
 * Corroborations, Lamp On/Off dual-lens economy, Acoustic Reel Scrubbing, and 4 Case IV Audit
 * Redactions (1:1 mathematical & narrative parity with Android Arc2InvestigatorModels.kt).
 */

export const CASE_ANGLES = Object.freeze([
  Object.freeze({
    number: 1,
    id: 'unclaimed_arrears',
    title: 'CASE I · UNCLAIMED ARREARS',
    subtitle: 'Unsigned plea and twelve unpaid municipal water notices',
    leadsCost: 10000,
    silenceCost: 90,
    requiredCorroborations: 1,
    requiredReelScrubs: 0,
    requiredRedactions: 0,
    keystoneReward: 'tax_plat_1998',
  }),
  Object.freeze({
    number: 2,
    id: 'the_second_chair',
    title: 'CASE II · THE SECOND CHAIR',
    subtitle: 'Blank brick on the 1998 plat and damp scuffs across the floorboards',
    leadsCost: 115000,
    silenceCost: 360,
    requiredCorroborations: 2,
    requiredReelScrubs: 0,
    requiredRedactions: 0,
    keystoneReward: 'second_chair_polaroid',
  }),
  Object.freeze({
    number: 3,
    id: 'static_on_reel_4',
    title: 'CASE III · STATIC ON REEL 4',
    subtitle: 'Forty minutes of basement room tone hiding a slow metallic thump',
    leadsCost: 1100000,
    silenceCost: 1500,
    requiredCorroborations: 3,
    requiredReelScrubs: 1,
    requiredRedactions: 0,
    keystoneReward: 'reel_four_waveform',
  }),
  Object.freeze({
    number: 4,
    id: 'sub_surface_survey',
    title: 'CASE IV · SUB-SURFACE SURVEY',
    subtitle: 'Looping drainage blueprints and four blacked-out audit clauses',
    leadsCost: 10500000,
    silenceCost: 4800,
    requiredCorroborations: 4,
    requiredReelScrubs: 1,
    requiredRedactions: 2,
    keystoneReward: 'drainage_loop_blueprint',
  }),
  Object.freeze({
    number: 5,
    id: 'internal_audit_file_zero',
    title: 'CASE V · INTERNAL AUDIT',
    subtitle: 'Uncatalogued folder sweating beneath the desk lamp',
    leadsCost: 85000000,
    silenceCost: 11500,
    requiredCorroborations: 5,
    requiredReelScrubs: 1,
    requiredRedactions: 4,
    keystoneReward: null,
  }),
]);

export const INVESTIGATOR_CONTACTS = Object.freeze([
  Object.freeze({
    id: 'night_archivist',
    code: 'DESK-01',
    name: 'NIGHT ARCHIVIST',
    desc: 'Cross-indexes missing municipal meter cards and county arrears ledgers.',
    baseCost: 20.0,
    costGrowth: 1.17,
    baseLeadsPerSec: 0.6,
    unlockCaseAngle: 1,
  }),
  Object.freeze({
    id: 'bribed_orderly',
    code: 'DESK-02',
    name: 'MUNICIPAL COURIER',
    desc: 'Leaves carbon receipts and late-shift mailroom slips beneath your office door.',
    baseCost: 140.0,
    costGrowth: 1.19,
    baseLeadsPerSec: 3.2,
    unlockCaseAngle: 1,
  }),
  Object.freeze({
    id: 'microfilm_scanner',
    code: 'DESK-03',
    name: 'MICROFILM READER',
    desc: 'Hums through county parcel rolls comparing old annex plats to blank brick walls.',
    baseCost: 950.0,
    costGrowth: 1.21,
    baseLeadsPerSec: 16.0,
    unlockCaseAngle: 2,
  }),
  Object.freeze({
    id: 'audio_transcriber',
    code: 'DESK-04',
    name: 'REEL TRANSCRIBER',
    desc: 'Logs forty-minute room-tone cassettes and marks every off-beat metallic knock.',
    baseCost: 6500.0,
    costGrowth: 1.23,
    baseLeadsPerSec: 78.0,
    unlockCaseAngle: 3,
  }),
]);

export const CASE_EXHIBITS = Object.freeze([
  Object.freeze({
    id: 'water_stained_petition',
    shortLabel: 'EX-01',
    title: 'Water-Stained Petition',
    caption: 'Unsigned plea for salvation left beneath your blotter in rain-warped ink.',
    isKeystone: true,
    unlockCaseAngle: 1,
    silenceCost: 0.0,
    boardX: 0.16,
    boardY: 0.22,
  }),
  Object.freeze({
    id: 'unlisted_pay_stub',
    shortLabel: 'EX-02',
    title: 'Unlisted Timecard',
    caption: 'Municipal meter clerk timecard stamped on the fourteenth with no surname.',
    isKeystone: false,
    unlockCaseAngle: 1,
    silenceCost: 45.0,
    boardX: 0.42,
    boardY: 0.18,
  }),
  Object.freeze({
    id: 'tax_plat_1998',
    shortLabel: 'EX-03',
    title: '1998 Tax Plat',
    caption: 'Parcel map marking an annex door where only blank windowless brick stands.',
    isKeystone: true,
    unlockCaseAngle: 2,
    silenceCost: 0.0,
    boardX: 0.68,
    boardY: 0.22,
  }),
  Object.freeze({
    id: 'inside_condensation_swab',
    shortLabel: 'EX-04',
    title: 'Inside Glass Smear',
    caption: 'Cold moisture lifted from the inside of a fourth-floor window painted shut.',
    isKeystone: false,
    unlockCaseAngle: 2,
    silenceCost: 120.0,
    boardX: 0.86,
    boardY: 0.36,
  }),
  Object.freeze({
    id: 'tuesday_statement_copy',
    shortLabel: 'EX-05',
    title: 'Tuesday Carbon Copy',
    caption: 'Witness sheet insisting you already took and signed this statement Tuesday.',
    isKeystone: false,
    unlockCaseAngle: 2,
    silenceCost: 260.0,
    boardX: 0.28,
    boardY: 0.48,
  }),
  Object.freeze({
    id: 'second_chair_polaroid',
    shortLabel: 'EX-06',
    title: 'Second Chair Scuffs',
    caption: 'Damp floorboard marks where a second wooden chair faces your blotter.',
    isKeystone: true,
    unlockCaseAngle: 3,
    silenceCost: 0.0,
    boardX: 0.56,
    boardY: 0.48,
  }),
  Object.freeze({
    id: 'coat_claim_ticket',
    shortLabel: 'EX-07',
    title: 'Coat Claim Ticket',
    caption: 'Dry-cleaning stub matching the damp grey wool coat behind your door.',
    isKeystone: false,
    unlockCaseAngle: 3,
    silenceCost: 750.0,
    boardX: 0.82,
    boardY: 0.68,
  }),
  Object.freeze({
    id: 'reel_four_waveform',
    shortLabel: 'EX-08',
    title: 'Reel 4 Trace',
    caption: 'Paper oscillograph showing an unhurried rhythmic thump beneath forty minutes of static.',
    isKeystone: true,
    unlockCaseAngle: 4,
    silenceCost: 0.0,
    boardX: 0.18,
    boardY: 0.78,
  }),
  Object.freeze({
    id: 'intake_thumb_cast',
    shortLabel: 'EX-09',
    title: 'Grey Wax Cast',
    caption: 'Cold wax impression bearing a faint transverse ridge across the left thumb.',
    isKeystone: false,
    unlockCaseAngle: 4,
    silenceCost: 2200.0,
    boardX: 0.46,
    boardY: 0.8,
  }),
  Object.freeze({
    id: 'drainage_loop_blueprint',
    shortLabel: 'EX-10',
    title: 'Closed Drainage Plat',
    caption: 'Sub-surface storm schematic folding inward into a single windowless square.',
    isKeystone: true,
    unlockCaseAngle: 5,
    silenceCost: 0.0,
    boardX: 0.72,
    boardY: 0.84,
  }),
]);

export function canonicalPairKey(aId, bId) {
  if (!aId || !bId || aId === bId) return null;
  return aId <= bId ? `${aId}|${bId}` : `${bId}|${aId}`;
}

export const CORROBORATIONS = Object.freeze([
  Object.freeze({
    id: 'smudged_ledger_date',
    title: 'Smudged Ledger Date',
    effectSummary: '+65% Stamp Leads',
    exhibitA: 'water_stained_petition',
    exhibitB: 'unlisted_pay_stub',
    pairKey: canonicalPairKey('water_stained_petition', 'unlisted_pay_stub'),
  }),
  Object.freeze({
    id: 'unlisted_annex',
    title: 'Unlisted Annex',
    effectSummary: '+65% Contact Leads/s',
    exhibitA: 'tax_plat_1998',
    exhibitB: 'second_chair_polaroid',
    pairKey: canonicalPairKey('tax_plat_1998', 'second_chair_polaroid'),
  }),
  Object.freeze({
    id: 'warm_room_tone',
    title: 'Warm Room Tone',
    effectSummary: '+70% Lamp-Off Silence/s',
    exhibitA: 'inside_condensation_swab',
    exhibitB: 'tuesday_statement_copy',
    pairKey: canonicalPairKey('inside_condensation_swab', 'tuesday_statement_copy'),
  }),
  Object.freeze({
    id: 'phantom_roster',
    title: 'Phantom Roster',
    effectSummary: '+55% Contact Leads/s & +35% Stamp',
    exhibitA: 'unlisted_pay_stub',
    exhibitB: 'tuesday_statement_copy',
    pairKey: canonicalPairKey('unlisted_pay_stub', 'tuesday_statement_copy'),
  }),
  Object.freeze({
    id: 'sub_surface_cadence',
    title: 'Sub-Surface Cadence',
    effectSummary: '+75% Desk Oddity Yield & +40% Silence',
    exhibitA: 'inside_condensation_swab',
    exhibitB: 'reel_four_waveform',
    pairKey: canonicalPairKey('inside_condensation_swab', 'reel_four_waveform'),
  }),
  Object.freeze({
    id: 'witness_in_two_places',
    title: 'Witness In Two Places',
    effectSummary: 'Stronger Reel Scrubbing & +45% Silence',
    exhibitA: 'coat_claim_ticket',
    exhibitB: 'reel_four_waveform',
    pairKey: canonicalPairKey('coat_claim_ticket', 'reel_four_waveform'),
  }),
  Object.freeze({
    id: 'borrowed_coat',
    title: 'Borrowed Coat',
    effectSummary: 'Darkness dims Leads less & +50% Contacts',
    exhibitA: 'coat_claim_ticket',
    exhibitB: 'second_chair_polaroid',
    pairKey: canonicalPairKey('coat_claim_ticket', 'second_chair_polaroid'),
  }),
  Object.freeze({
    id: 'closed_municipal_loop',
    title: 'Closed Municipal Loop',
    effectSummary: '×2.1 All Leads & Stamp Yield',
    exhibitA: 'tax_plat_1998',
    exhibitB: 'drainage_loop_blueprint',
    pairKey: canonicalPairKey('tax_plat_1998', 'drainage_loop_blueprint'),
  }),
  Object.freeze({
    id: 'hydro_ward_sub_basement',
    title: 'Sub-Basement Seam',
    effectSummary: '×2.2 Silence/s & +60% Oddity Yield',
    exhibitA: 'drainage_loop_blueprint',
    exhibitB: 'intake_thumb_cast',
    pairKey: canonicalPairKey('drainage_loop_blueprint', 'intake_thumb_cast'),
  }),
  Object.freeze({
    id: 'matching_hand',
    title: 'Matching Hand',
    effectSummary: '×2.4 Stamp & ×1.8 All Leads/s',
    exhibitA: 'water_stained_petition',
    exhibitB: 'intake_thumb_cast',
    pairKey: canonicalPairKey('water_stained_petition', 'intake_thumb_cast'),
  }),
]);

export const AUDIT_REDACTIONS = Object.freeze([
  Object.freeze({
    id: 'clause_seepage',
    label: 'CLAUSE I · SUB-SURFACE',
    revealedText:
      'Municipal maintenance ceases at the fourth floor; below grade, the tenant maintains the masonry.',
    silenceCost: 750.0,
    effectSummary: '+40% Leads/s · +25% Silence/s',
  }),
  Object.freeze({
    id: 'clause_shift',
    label: 'CLAUSE II · UNRELIEVED WATCH',
    revealedText:
      'Relief clerk reassigned indefinitely; attendance continues until the count stabilizes.',
    silenceCost: 1700.0,
    effectSummary: '+50% Stamp Leads · +35% Silence/s',
  }),
  Object.freeze({
    id: 'clause_quiet',
    label: 'CLAUSE III · QUIET COLLECTOR',
    revealedText:
      'Where speech disturbs the wall, keep quiet until the glass collects what is owed.',
    silenceCost: 3400.0,
    effectSummary: '+60% Leads/s · +45% Silence · 15% Lamp-On trickle',
  }),
  Object.freeze({
    id: 'clause_intake',
    label: 'CLAUSE IV · SUBJECT INTAKE',
    revealedText:
      "File #00 remains open under the investigator's own signature; no transfer out was ever countersigned.",
    silenceCost: 6200.0,
    effectSummary: '+80% Leads/s · +60% Silence · Unseals Audit',
  }),
]);

export const DESK_ANOMALY_TYPES = Object.freeze({
  EVIDENCE_ENVELOPE: Object.freeze({
    id: 'evidence_envelope',
    unlockCaseAngle: 1,
    label: 'SLIT',
    ariaLabel: 'Slit open damp evidence envelope on desk',
    dripLine: 'Another damp municipal notice pinned to the margin.',
  }),
  FOGGED_SPECTACLES: Object.freeze({
    id: 'fogged_spectacles',
    unlockCaseAngle: 2,
    label: 'WIPE',
    ariaLabel: 'Wipe condensation from fogged spectacles',
    dripLine: 'Your spectacles fog over from the inside; clarity sharpens.',
  }),
  REEL_TAPE: Object.freeze({
    id: 'reel_tape',
    unlockCaseAngle: 3,
    label: 'PLAY',
    ariaLabel: 'Play warped magnetic reel tape on desk',
    dripLine: 'The warped tape catches a slow metallic knock beneath the static.',
  }),
  ENAMEL_EXHIBIT: Object.freeze({
    id: 'enamel_exhibit',
    unlockCaseAngle: 4,
    label: 'BAG',
    ariaLabel: 'Bag cold enamel exhibit surfaced on desk',
    dripLine: 'A cold smooth fragment sits in your evidence tray, dry and uncatalogued.',
  }),
});

export const DESK_ANOMALY_LIST = Object.freeze([
  DESK_ANOMALY_TYPES.EVIDENCE_ENVELOPE,
  DESK_ANOMALY_TYPES.FOGGED_SPECTACLES,
  DESK_ANOMALY_TYPES.REEL_TAPE,
  DESK_ANOMALY_TYPES.ENAMEL_EXHIBIT,
]);

export const ARC2_BALANCE = Object.freeze({
  LAMP_OFF_BASE_LEADS_FACTOR: 0.25,
  BORROWED_COAT_LAMP_OFF_LEADS_FACTOR: 0.3875,
  BASE_LAMP_OFF_SILENCE_PER_SEC: 0.45,
  SILENCE_PER_CONTACT_PER_SEC: 0.035,
  SPECULATIVE_THREAD_BONUS: 0.15,
  FOCUS_DURATION_SEC: 18.0,
  FOCUS_MULTIPLIER: 1.8,
  REEL_SCRUB_DURATION_SEC: 24.0,
  REEL_SCRUB_COOLDOWN_SEC: 45.0,
  REEL_SCRUB_SILENCE_MULT: 2.6,
  REEL_SCRUB_WITNESS_SILENCE_MULT: 3.4,
  FIRST_DESK_ANOMALY_DELAY_SEC: 16.0,
  LAMP_ON_ANOMALY_INTERVAL_SEC: 32.0,
  LAMP_OFF_ANOMALY_INTERVAL_SEC: 20.0,
  DESK_ANOMALY_LIFETIME_SEC: 14.0,
  MAX_ACTIVE_DESK_ANOMALIES: 2,
});

export function initialArc2State() {
  return {
    leads: 0.0,
    lifetimeLeads: 0.0,
    silence: 0.0,
    lifetimeSilence: 0.0,
    caseAngle: 1,
    completedCasePivots: 0,
    lampOn: true,
    lampToggleCount: 0,
    lampOffCount: 0,
    totalLampOffSeconds: 0.0,
    contacts: {},
    pinnedExhibits: ['water_stained_petition'],
    wiredPairs: [],
    discoveredCorroborations: [],
    reelScrubRemainingSeconds: 0.0,
    reelScrubCooldownSeconds: 0.0,
    reelScrubCount: 0,
    dissolvedRedactions: [],
    activeDeskAnomalies: [],
    deskAnomalyInteractions: {},
    focusRemainingSeconds: 0.0,
    secondsUntilNextAnomaly: ARC2_BALANCE.FIRST_DESK_ANOMALY_DELAY_SEC,
    nextAnomalyId: 1,
    fileZeroRevealed: false,
  };
}

export function caseAngleByNumber(num) {
  const clamped = Math.max(1, Math.min(5, Math.floor(Number(num) || 1)));
  return CASE_ANGLES.find((c) => c.number === clamped) ?? CASE_ANGLES[0];
}

export function contactById(id) {
  return INVESTIGATOR_CONTACTS.find((c) => c.id === id) ?? null;
}

export function exhibitById(id) {
  return CASE_EXHIBITS.find((e) => e.id === id) ?? null;
}

export function corroborationById(id) {
  return CORROBORATIONS.find((c) => c.id === id) ?? null;
}

export function matchCorroborationPair(pairKey) {
  return CORROBORATIONS.find((c) => c.pairKey === pairKey) ?? null;
}

export function redactionById(id) {
  return AUDIT_REDACTIONS.find((r) => r.id === id) ?? null;
}

export function arc2CurrentCase(desk) {
  return caseAngleByNumber(desk.caseAngle);
}

export function arc2IsSilenceActive(desk) {
  return !desk.lampOn;
}

export function arc2IsReelScrubbing(desk) {
  return (desk.reelScrubRemainingSeconds ?? 0) > 0.0;
}

export function arc2IsBulkWarrantUnlocked(desk) {
  return (desk.caseAngle ?? 1) >= 3 || (desk.completedCasePivots ?? 0) >= 2;
}

export function arc2IsReelScrubUnlocked(desk) {
  return (desk.caseAngle ?? 1) >= 3 || (desk.completedCasePivots ?? 0) >= 2;
}

export function arc2IsRedactionSolventUnlocked(desk) {
  return (desk.caseAngle ?? 1) >= 4 || (desk.completedCasePivots ?? 0) >= 3;
}

export function arc2MaxStringSpools(desk) {
  return Math.max(1, Math.min(5, 1 + (desk.completedCasePivots ?? 0)));
}

export function arc2ActiveCorroborations(desk) {
  const active = new Set();
  for (const key of desk.wiredPairs ?? []) {
    const matched = matchCorroborationPair(key);
    if (matched) active.add(matched.id);
  }
  return active;
}

export function arc2SpeculativeThreadCount(desk) {
  let count = 0;
  for (const key of desk.wiredPairs ?? []) {
    if (!matchCorroborationPair(key)) count += 1;
  }
  return count;
}

export function arc2TotalContactsOwned(desk) {
  return Object.values(desk.contacts ?? {}).reduce((a, b) => a + (Number(b) || 0), 0);
}

export function arc2HauntDepth(desk) {
  if (desk.fileZeroRevealed) return 1.0;
  const angle = Math.max(1, Math.min(5, desk.caseAngle ?? 1));
  const bases = [0.04, 0.22, 0.42, 0.62, 0.86];
  const caps = [0.18, 0.38, 0.55, 0.82, 0.98];
  const base = bases[angle - 1];
  const cap = caps[angle - 1];
  const c = caseAngleByNumber(angle);
  const leadRatio = Math.max(0, Math.min(1, (desk.leads ?? 0) / Math.max(1, c.leadsCost)));
  const lampBonus = Math.max(0, Math.min(1, (desk.totalLampOffSeconds ?? 0) / 360.0)) * 0.05;
  const redactionBonus = angle >= 4 ? (desk.dissolvedRedactions?.length ?? 0) * 0.03 : 0.0;
  const span = Math.max(0, cap - base);
  return Math.max(base, Math.min(cap, base + leadRatio * span * 0.75 + lampBonus + redactionBonus));
}

export function arc2CaseProgressFraction(desk) {
  if (desk.fileZeroRevealed) return 1.0;
  const c = arc2CurrentCase(desk);
  const leadsPart = Math.max(0, Math.min(1, (desk.leads ?? 0) / Math.max(1, c.leadsCost)));
  const silPart = Math.max(0, Math.min(1, (desk.silence ?? 0) / Math.max(1, c.silenceCost)));
  return Math.max(0, Math.min(1, leadsPart * 0.65 + silPart * 0.35));
}

export function arc2IsRevisionTabVisible(desk) {
  const c = arc2CurrentCase(desk);
  return (
    (desk.completedCasePivots ?? 0) > 0 ||
    Boolean(desk.fileZeroRevealed) ||
    (desk.leads ?? 0) >= c.leadsCost * 0.35 ||
    (desk.discoveredCorroborations?.length ?? 0) > 0
  );
}

export function arc2ContactCount(desk, contactId) {
  return desk.contacts?.[contactId] ?? 0;
}

export function arc2IsContactUnlocked(desk, contactOrId) {
  const spec = typeof contactOrId === 'string' ? contactById(contactOrId) : contactOrId;
  if (!spec) return false;
  return (desk.caseAngle ?? 1) >= spec.unlockCaseAngle || arc2ContactCount(desk, spec.id) > 0;
}

export function arc2ContactCost(desk, contactOrId) {
  const spec = typeof contactOrId === 'string' ? contactById(contactOrId) : contactOrId;
  if (!spec) return 0;
  return Math.ceil(spec.baseCost * Math.pow(spec.costGrowth, arc2ContactCount(desk, spec.id)));
}

export function arc2MaxAffordableContacts(desk, contactOrId) {
  const spec = typeof contactOrId === 'string' ? contactById(contactOrId) : contactOrId;
  if (!spec || !arc2IsContactUnlocked(desk, spec)) return { count: 0, totalCost: 0 };
  let count = 0;
  let totalCost = 0;
  let owned = arc2ContactCount(desk, spec.id);
  while (count < 250) {
    const nextCost = Math.ceil(spec.baseCost * Math.pow(spec.costGrowth, owned));
    if (desk.leads < totalCost + nextCost) break;
    totalCost += nextCost;
    owned += 1;
    count += 1;
  }
  return { count, totalCost };
}

function caseMasteryLeadsMult(desk) {
  return 1.0 + (desk.completedCasePivots ?? 0) * 0.45;
}

function caseMasterySilenceMult(desk) {
  return 1.0 + (desk.completedCasePivots ?? 0) * 0.35;
}

function speculativeMultiplier(desk) {
  return 1.0 + arc2SpeculativeThreadCount(desk) * ARC2_BALANCE.SPECULATIVE_THREAD_BONUS;
}

export function arc2FocusMultiplier(desk) {
  return (desk.focusRemainingSeconds ?? 0) > 0 ? ARC2_BALANCE.FOCUS_MULTIPLIER : 1.0;
}

export function arc2LampLeadsMultiplier(desk) {
  if (desk.lampOn) return 1.0;
  const active = arc2ActiveCorroborations(desk);
  return active.has('borrowed_coat')
    ? ARC2_BALANCE.BORROWED_COAT_LAMP_OFF_LEADS_FACTOR
    : ARC2_BALANCE.LAMP_OFF_BASE_LEADS_FACTOR;
}

export function arc2AnomalyPotencyMultiplier(desk) {
  const active = arc2ActiveCorroborations(desk);
  let mult = 1.0 + (desk.completedCasePivots ?? 0) * 0.25;
  if (active.has('sub_surface_cadence')) mult *= 1.75;
  if (active.has('hydro_ward_sub_basement')) mult *= 1.6;
  return mult;
}

export function arc2InspectYield(desk) {
  const active = arc2ActiveCorroborations(desk);
  const redactions = desk.dissolvedRedactions ?? [];
  let mult = caseMasteryLeadsMult(desk) * speculativeMultiplier(desk) * arc2FocusMultiplier(desk);
  if (active.has('smudged_ledger_date')) mult *= 1.65;
  if (active.has('phantom_roster')) mult *= 1.35;
  if (active.has('closed_municipal_loop')) mult *= 2.1;
  if (active.has('matching_hand')) mult *= 2.4;
  if (redactions.includes('clause_shift')) mult *= 1.5;
  const raw = (1.0 + arc2TotalContactsOwned(desk) * 0.12) * mult * arc2LampLeadsMultiplier(desk);
  return Math.max(0.25, raw);
}

export function arc2EffectiveContactRate(desk, contactOrId) {
  const spec = typeof contactOrId === 'string' ? contactById(contactOrId) : contactOrId;
  if (!spec) return 0.0;
  const active = arc2ActiveCorroborations(desk);
  const redactions = desk.dissolvedRedactions ?? [];
  let mult = caseMasteryLeadsMult(desk) * speculativeMultiplier(desk) * arc2FocusMultiplier(desk);
  if (active.has('unlisted_annex')) mult *= 1.65;
  if (active.has('phantom_roster')) mult *= 1.55;
  if (active.has('borrowed_coat')) mult *= 1.5;
  if (active.has('closed_municipal_loop')) mult *= 2.1;
  if (active.has('matching_hand')) mult *= 1.8;
  if (redactions.includes('clause_seepage')) mult *= 1.4;
  if (redactions.includes('clause_quiet')) mult *= 1.6;
  if (redactions.includes('clause_intake')) mult *= 1.8;
  return spec.baseLeadsPerSec * mult * arc2LampLeadsMultiplier(desk);
}

export function arc2LeadsPerSecond(desk) {
  let sum = 0.0;
  for (const c of INVESTIGATOR_CONTACTS) {
    sum += arc2ContactCount(desk, c.id) * arc2EffectiveContactRate(desk, c.id);
  }
  return sum;
}

export function arc2SilencePerSecond(desk) {
  const redactions = desk.dissolvedRedactions ?? [];
  const lampOnTrickle = desk.lampOn ? (redactions.includes('clause_quiet') ? 0.15 : 0.0) : 1.0;
  if (lampOnTrickle <= 0.0) return 0.0;

  const active = arc2ActiveCorroborations(desk);
  let mult = caseMasterySilenceMult(desk) * speculativeMultiplier(desk) * arc2FocusMultiplier(desk);
  if (active.has('warm_room_tone')) mult *= 1.7;
  if (active.has('sub_surface_cadence')) mult *= 1.4;
  if (active.has('witness_in_two_places')) mult *= 1.45;
  if (active.has('hydro_ward_sub_basement')) mult *= 2.2;
  if (redactions.includes('clause_seepage')) mult *= 1.25;
  if (redactions.includes('clause_shift')) mult *= 1.35;
  if (redactions.includes('clause_quiet')) mult *= 1.45;
  if (redactions.includes('clause_intake')) mult *= 1.6;
  if (arc2IsReelScrubbing(desk)) {
    mult *= active.has('witness_in_two_places')
      ? ARC2_BALANCE.REEL_SCRUB_WITNESS_SILENCE_MULT
      : ARC2_BALANCE.REEL_SCRUB_SILENCE_MULT;
  }
  const base =
    ARC2_BALANCE.BASE_LAMP_OFF_SILENCE_PER_SEC +
    arc2TotalContactsOwned(desk) * ARC2_BALANCE.SILENCE_PER_CONTACT_PER_SEC;
  return base * mult * lampOnTrickle;
}

export function arc2IsExhibitAvailableToUnlock(desk, exhibitOrId) {
  const ex = typeof exhibitOrId === 'string' ? exhibitById(exhibitOrId) : exhibitOrId;
  if (!ex) return false;
  return (
    !ex.isKeystone &&
    !(desk.pinnedExhibits ?? []).includes(ex.id) &&
    (desk.caseAngle ?? 1) >= ex.unlockCaseAngle
  );
}

export function arc2CanUnlockExhibit(desk, exhibitOrId) {
  const ex = typeof exhibitOrId === 'string' ? exhibitById(exhibitOrId) : exhibitOrId;
  if (!ex) return false;
  return arc2IsExhibitAvailableToUnlock(desk, ex) && (desk.silence ?? 0) >= ex.silenceCost;
}

export function arc2CanTriggerReelScrub(desk) {
  return (
    arc2IsReelScrubUnlocked(desk) &&
    !desk.lampOn &&
    (desk.reelScrubRemainingSeconds ?? 0) <= 0.0 &&
    (desk.reelScrubCooldownSeconds ?? 0) <= 0.0
  );
}

export function arc2CanDissolveRedaction(desk, redactionOrId) {
  const r = typeof redactionOrId === 'string' ? redactionById(redactionOrId) : redactionOrId;
  if (!r) return false;
  return (
    arc2IsRedactionSolventUnlocked(desk) &&
    !desk.lampOn &&
    !(desk.dissolvedRedactions ?? []).includes(r.id) &&
    (desk.silence ?? 0) >= r.silenceCost
  );
}

export function arc2CanPivotNextCase(desk) {
  if (desk.fileZeroRevealed) return false;
  const c = arc2CurrentCase(desk);
  return (
    (desk.leads ?? 0) >= c.leadsCost &&
    (desk.silence ?? 0) >= c.silenceCost &&
    (desk.discoveredCorroborations?.length ?? 0) >= c.requiredCorroborations &&
    (desk.reelScrubCount ?? 0) >= c.requiredReelScrubs &&
    (desk.dissolvedRedactions?.length ?? 0) >= c.requiredRedactions
  );
}

// Backward-compatible aliases for legacy callers
export const arc2CanConnectNextChapter = arc2CanPivotNextCase;
export const arc2NextChapter = arc2CurrentCase;
