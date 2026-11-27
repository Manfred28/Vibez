/**
 * Pure audio cue resolver, ambient profile mapper, and mute preference storage
 * for "The Tending of the Unnamed Organ" (1:1 parity with Android SoundCueResolver).
 */

import { CREATURE_TYPES, GAME_ARCS, WALL_ANOMALY_TYPES } from '../engine/constants.js?v=3.15';
import {
  DESK_ANOMALY_TYPES,
  INVESTIGATOR_CONTACTS,
  arc2HauntDepth,
  arc2IsReelScrubbing,
  arc2IsSilenceActive,
  matchCorroborationPair,
} from '../engine/arc2Models.js?v=3.15';
import {
  arc3FloodLevel,
  arc3IsActivelyClotting,
  arc3ShatteredJarCount,
  arc3TotalRuptureInteractions,
} from '../engine/arc3Models.js?v=3.15';

export const AUDIO_MUTE_STORAGE_KEY = 'unnamed_organ_audio_muted_v1';

export const SOUND_CUES = Object.freeze({
  VALVE_PRESS: 'valve_press',
  CREATURE_BUY_SUTURE_HAND: 'creature_buy_suture_hand',
  CREATURE_BUY_LEECH_CLERK: 'creature_buy_leech_clerk',
  CREATURE_BUY_WEEPING_LAMP: 'creature_buy_weeping_lamp',
  CREATURE_BUY_SUMP_SIPHON: 'creature_buy_sump_siphon',
  ANOMALY_TOOTH: 'anomaly_tooth',
  ANOMALY_EYE: 'anomaly_eye',
  ANOMALY_NODULE: 'anomaly_nodule',
  ANOMALY_RIB: 'anomaly_rib',
  JAR_BOTTLE: 'jar_bottle',
  GRAFT_PERFORM: 'graft_perform',
  GRAFT_CHOIR_LUNG: 'graft_choir_lung',
  RELIC_INSPECT: 'relic_inspect',
  SCAR_UNLOCK: 'scar_unlock',
  DRAIN_TOGGLE: 'drain_toggle',
  SUMP_DREDGE: 'sump_dredge',
  ECHO_PULSE: 'echo_pulse',
  CLAMP_ENGAGE: 'clamp_engage',
  MARROW_BLOOM: 'marrow_bloom',
  THROAT_OFFER: 'throat_offer',
  EXCISION_CUT: 'excision_cut',
  PLEAD_SALVATION: 'plead_salvation',
  STORY_DRIP: 'story_drip',
  PHASE_SHIFT: 'phase_shift',
  ARC2_STAMP: 'arc2_stamp',
  ARC2_LAMP_ON: 'arc2_lamp_on',
  ARC2_LAMP_OFF: 'arc2_lamp_off',
  ARC2_CONTACT_ARCHIVIST: 'arc2_contact_archivist',
  ARC2_CONTACT_ORDERLY: 'arc2_contact_orderly',
  ARC2_CONTACT_SCANNER: 'arc2_contact_scanner',
  ARC2_CONTACT_TRANSCRIBER: 'arc2_contact_transcriber',
  ARC2_DEDUCTION: 'arc2_deduction',
  ARC2_CORROBORATION_LOCK: 'arc2_corroboration_lock',
  ARC2_STRING_WIRE: 'arc2_string_wire',
  ARC2_STRING_UNWIRE: 'arc2_string_unwire',
  ARC2_REEL_SCRUB: 'arc2_reel_scrub',
  ARC2_REDACTION_DISSOLVE: 'arc2_redaction_dissolve',
  ARC2_ANOMALY_ENVELOPE: 'arc2_anomaly_envelope',
  ARC2_ANOMALY_SPECTACLES: 'arc2_anomaly_spectacles',
  ARC2_ANOMALY_REEL_TAPE: 'arc2_anomaly_reel_tape',
  ARC2_ANOMALY_ENAMEL: 'arc2_anomaly_enamel',
  ARC2_CHAPTER: 'arc2_chapter',
  ARC2_FILE_ZERO: 'arc2_file_zero',
  ARC3_ENTER: 'arc3_enter',
  ARC3_WRENCH: 'arc3_wrench',
  ARC3_JAR_SHATTER: 'arc3_jar_shatter',
  ARC3_UNMAKE_TEAR: 'arc3_unmake_tear',
  ARC3_RUPTURE_POP: 'arc3_rupture_pop',
  ARC3_WEDGE_DRIVE: 'arc3_wedge_drive',
  ARC3_CLOT_WARNING: 'arc3_clot_warning',
  ARC3_FINAL_PULL: 'arc3_final_pull',
  ARC3_SUBMERSION_END: 'arc3_submersion_end',
});

export const SOUND_ACTION_HINTS = Object.freeze({
  TICK: 'tick',
  VALVE_PRESS: 'valve_press',
  CREATURE_BUY: 'creature_buy',
  JAR_BOTTLE: 'jar_bottle',
  GRAFT_PERFORM: 'graft_perform',
  ANOMALY_INTERACT: 'anomaly_interact',
  SCAR_UNLOCK: 'scar_unlock',
  DRAIN_TOGGLE: 'drain_toggle',
  SUMP_DREDGE: 'sump_dredge',
  ECHO_DIVERSION: 'echo_diversion',
  CLAMP_ENGAGE: 'clamp_engage',
  THROAT_OFFER: 'throat_offer',
  EXCISION: 'excision',
  BREACH_TO_ARC2: 'breach_to_arc2',
  ARC2_STAMP: 'arc2_stamp',
  ARC2_LAMP_TOGGLE: 'arc2_lamp_toggle',
  ARC2_CONTACT: 'arc2_contact',
  ARC2_DEDUCTION: 'arc2_deduction',
  ARC2_STRING_WIRE: 'arc2_string_wire',
  ARC2_REEL_SCRUB: 'arc2_reel_scrub',
  ARC2_REDACTION_DISSOLVE: 'arc2_redaction_dissolve',
  ARC2_DESK_ANOMALY: 'arc2_desk_anomaly',
  ARC2_CHAPTER: 'arc2_chapter',
  ARC3_ENTER: 'arc3_enter',
  ARC3_WRENCH: 'arc3_wrench',
  ARC3_UNMAKE: 'arc3_unmake',
  ARC3_RUPTURE: 'arc3_rupture',
  ARC3_WEDGE: 'arc3_wedge',
  ARC3_FINAL_PULL: 'arc3_final_pull',
  DEV: 'dev',
});

export const CUE_MIN_INTERVAL_MS = Object.freeze({
  [SOUND_CUES.VALVE_PRESS]: 62,
  [SOUND_CUES.CREATURE_BUY_SUTURE_HAND]: 85,
  [SOUND_CUES.CREATURE_BUY_LEECH_CLERK]: 95,
  [SOUND_CUES.CREATURE_BUY_WEEPING_LAMP]: 100,
  [SOUND_CUES.CREATURE_BUY_SUMP_SIPHON]: 110,
  [SOUND_CUES.ANOMALY_TOOTH]: 95,
  [SOUND_CUES.ANOMALY_EYE]: 115,
  [SOUND_CUES.ANOMALY_NODULE]: 115,
  [SOUND_CUES.ANOMALY_RIB]: 130,
  [SOUND_CUES.JAR_BOTTLE]: 125,
  [SOUND_CUES.GRAFT_PERFORM]: 300,
  [SOUND_CUES.GRAFT_CHOIR_LUNG]: 300,
  [SOUND_CUES.RELIC_INSPECT]: 95,
  [SOUND_CUES.SCAR_UNLOCK]: 115,
  [SOUND_CUES.DRAIN_TOGGLE]: 110,
  [SOUND_CUES.SUMP_DREDGE]: 190,
  [SOUND_CUES.ECHO_PULSE]: 260,
  [SOUND_CUES.CLAMP_ENGAGE]: 170,
  [SOUND_CUES.MARROW_BLOOM]: 320,
  [SOUND_CUES.THROAT_OFFER]: 220,
  [SOUND_CUES.EXCISION_CUT]: 420,
  [SOUND_CUES.PLEAD_SALVATION]: 450,
  [SOUND_CUES.STORY_DRIP]: 380,
  [SOUND_CUES.PHASE_SHIFT]: 450,
  [SOUND_CUES.ARC2_STAMP]: 62,
  [SOUND_CUES.ARC2_LAMP_ON]: 95,
  [SOUND_CUES.ARC2_LAMP_OFF]: 95,
  [SOUND_CUES.ARC2_CONTACT_ARCHIVIST]: 80,
  [SOUND_CUES.ARC2_CONTACT_ORDERLY]: 80,
  [SOUND_CUES.ARC2_CONTACT_SCANNER]: 80,
  [SOUND_CUES.ARC2_CONTACT_TRANSCRIBER]: 80,
  [SOUND_CUES.ARC2_DEDUCTION]: 120,
  [SOUND_CUES.ARC2_CORROBORATION_LOCK]: 110,
  [SOUND_CUES.ARC2_STRING_WIRE]: 110,
  [SOUND_CUES.ARC2_STRING_UNWIRE]: 110,
  [SOUND_CUES.ARC2_REEL_SCRUB]: 240,
  [SOUND_CUES.ARC2_REDACTION_DISSOLVE]: 220,
  [SOUND_CUES.ARC2_ANOMALY_ENVELOPE]: 110,
  [SOUND_CUES.ARC2_ANOMALY_SPECTACLES]: 110,
  [SOUND_CUES.ARC2_ANOMALY_REEL_TAPE]: 110,
  [SOUND_CUES.ARC2_ANOMALY_ENAMEL]: 110,
  [SOUND_CUES.ARC2_CHAPTER]: 240,
  [SOUND_CUES.ARC2_FILE_ZERO]: 450,
  [SOUND_CUES.ARC3_ENTER]: 450,
  [SOUND_CUES.ARC3_WRENCH]: 58,
  [SOUND_CUES.ARC3_JAR_SHATTER]: 140,
  [SOUND_CUES.ARC3_UNMAKE_TEAR]: 160,
  [SOUND_CUES.ARC3_RUPTURE_POP]: 120,
  [SOUND_CUES.ARC3_WEDGE_DRIVE]: 320,
  [SOUND_CUES.ARC3_CLOT_WARNING]: 2400,
  [SOUND_CUES.ARC3_FINAL_PULL]: 480,
  [SOUND_CUES.ARC3_SUBMERSION_END]: 600,
});

function sumMapValues(mapObj) {
  if (!mapObj || typeof mapObj !== 'object') return 0;
  let total = 0;
  for (const val of Object.values(mapObj)) {
    total += Number(val) || 0;
  }
  return total;
}

function setsDiffer(a, b) {
  const sa = new Set(a || []);
  const sb = new Set(b || []);
  if (sa.size !== sb.size) return true;
  for (const item of sa) {
    if (!sb.has(item)) return true;
  }
  return false;
}

function clamp01(v) {
  return Math.max(0, Math.min(1, v));
}

export function readMutedPreference(storage) {
  const store = storage ?? (typeof window !== 'undefined' ? window.localStorage : null);
  if (!store) return false;
  try {
    return store.getItem(AUDIO_MUTE_STORAGE_KEY) === 'true';
  } catch {
    return false;
  }
}

export function writeMutedPreference(muted, storage) {
  const store = storage ?? (typeof window !== 'undefined' ? window.localStorage : null);
  if (!store) return;
  try {
    store.setItem(AUDIO_MUTE_STORAGE_KEY, muted ? 'true' : 'false');
  } catch {
    // Ignore private browsing storage errors.
  }
}

/**
 * Pure transition-diff cue resolver matching Android `SoundCueResolver.resolveTransitionCues`.
 */
export function resolveTransitionCues(prev, next, action = SOUND_ACTION_HINTS.TICK) {
  if (!prev || !next || prev === next || action === SOUND_ACTION_HINTS.DEV) {
    return [];
  }
  const cues = [];

  switch (action) {
    case SOUND_ACTION_HINTS.VALVE_PRESS:
      if (next.cycleLifetimeIchor > prev.cycleLifetimeIchor) {
        cues.push(SOUND_CUES.VALVE_PRESS);
      }
      break;
    case SOUND_ACTION_HINTS.CREATURE_BUY: {
      const bought = Object.values(CREATURE_TYPES).find(
        (t) => (next.creatures?.[t.id] ?? 0) > (prev.creatures?.[t.id] ?? 0),
      );
      if (bought?.id === CREATURE_TYPES.SUTURE_HAND.id) {
        cues.push(SOUND_CUES.CREATURE_BUY_SUTURE_HAND);
      } else if (bought?.id === CREATURE_TYPES.LEECH_CLERK.id) {
        cues.push(SOUND_CUES.CREATURE_BUY_LEECH_CLERK);
      } else if (bought?.id === CREATURE_TYPES.WEEPING_LAMP.id) {
        cues.push(SOUND_CUES.CREATURE_BUY_WEEPING_LAMP);
      } else if (bought?.id === CREATURE_TYPES.SUMP_SIPHON.id) {
        cues.push(SOUND_CUES.CREATURE_BUY_SUMP_SIPHON);
      }
      break;
    }
    case SOUND_ACTION_HINTS.JAR_BOTTLE:
      if ((next.ownedJars?.length ?? 0) > (prev.ownedJars?.length ?? 0)) {
        cues.push(SOUND_CUES.JAR_BOTTLE);
      }
      break;
    case SOUND_ACTION_HINTS.GRAFT_PERFORM:
      if ((next.choirLungs ?? 0) > (prev.choirLungs ?? 0)) {
        cues.push(SOUND_CUES.GRAFT_CHOIR_LUNG);
      } else if ((next.graftedTendrils ?? 0) > (prev.graftedTendrils ?? 0)) {
        cues.push(SOUND_CUES.GRAFT_PERFORM);
      }
      break;
    case SOUND_ACTION_HINTS.ANOMALY_INTERACT: {
      const types = Object.values(WALL_ANOMALY_TYPES);
      const interacted = types.find(
        (t) => (next.anomalyInteractions?.[t.id] ?? 0) > (prev.anomalyInteractions?.[t.id] ?? 0),
      );
      if (interacted?.id === WALL_ANOMALY_TYPES.PALE_TOOTH.id) {
        cues.push(SOUND_CUES.ANOMALY_TOOTH);
      } else if (interacted?.id === WALL_ANOMALY_TYPES.GLASS_EYE.id) {
        cues.push(SOUND_CUES.ANOMALY_EYE);
      } else if (interacted?.id === WALL_ANOMALY_TYPES.SWOLLEN_NODULE.id) {
        cues.push(SOUND_CUES.ANOMALY_NODULE);
      } else if (interacted?.id === WALL_ANOMALY_TYPES.CHITIN_RIB.id) {
        cues.push(SOUND_CUES.ANOMALY_RIB);
      }
      break;
    }
    case SOUND_ACTION_HINTS.SCAR_UNLOCK:
      if (
        (next.unlockedScarNodes?.length ?? 0) > (prev.unlockedScarNodes?.length ?? 0) ||
        sumMapValues(next.repeatableScarRanks) > sumMapValues(prev.repeatableScarRanks)
      ) {
        cues.push(SOUND_CUES.SCAR_UNLOCK);
      }
      break;
    case SOUND_ACTION_HINTS.DRAIN_TOGGLE:
      if (next.drainSealed !== prev.drainSealed) {
        cues.push(SOUND_CUES.DRAIN_TOGGLE);
      }
      break;
    case SOUND_ACTION_HINTS.SUMP_DREDGE:
      if ((next.lifetimeRelicsDredged ?? 0) > (prev.lifetimeRelicsDredged ?? 0)) {
        cues.push(SOUND_CUES.SUMP_DREDGE);
      }
      break;
    case SOUND_ACTION_HINTS.ECHO_DIVERSION:
      if (next.echoDiversionPercent !== prev.echoDiversionPercent) {
        cues.push(SOUND_CUES.DRAIN_TOGGLE);
      }
      break;
    case SOUND_ACTION_HINTS.CLAMP_ENGAGE:
      if ((next.clampRemainingSeconds ?? 0) > (prev.clampRemainingSeconds ?? 0)) {
        cues.push(SOUND_CUES.CLAMP_ENGAGE);
      }
      break;
    case SOUND_ACTION_HINTS.THROAT_OFFER:
      if ((next.unredactedDossierLines?.length ?? 0) > (prev.unredactedDossierLines?.length ?? 0)) {
        cues.push(SOUND_CUES.THROAT_OFFER);
      }
      break;
    case SOUND_ACTION_HINTS.EXCISION:
      if ((next.cycle ?? 0) > (prev.cycle ?? 0)) {
        cues.push(SOUND_CUES.EXCISION_CUT);
      }
      break;
    case SOUND_ACTION_HINTS.BREACH_TO_ARC2:
      if (
        next.activeArc === GAME_ARCS.ARC_2_INVESTIGATION &&
        prev.activeArc !== GAME_ARCS.ARC_2_INVESTIGATION
      ) {
        cues.push(SOUND_CUES.PLEAD_SALVATION);
      }
      break;
    case SOUND_ACTION_HINTS.ARC2_STAMP:
      if ((next.arc2?.lifetimeLeads ?? 0) > (prev.arc2?.lifetimeLeads ?? 0)) {
        cues.push(SOUND_CUES.ARC2_STAMP);
      }
      break;
    case SOUND_ACTION_HINTS.ARC2_LAMP_TOGGLE:
      if (next.arc2?.lampOn !== prev.arc2?.lampOn) {
        cues.push(next.arc2?.lampOn ? SOUND_CUES.ARC2_LAMP_ON : SOUND_CUES.ARC2_LAMP_OFF);
      }
      break;
    case SOUND_ACTION_HINTS.ARC2_CONTACT: {
      const boughtContact = INVESTIGATOR_CONTACTS.find(
        (c) => (next.arc2?.contacts?.[c.id] ?? 0) > (prev.arc2?.contacts?.[c.id] ?? 0),
      );
      if (boughtContact?.id === 'night_archivist') {
        cues.push(SOUND_CUES.ARC2_CONTACT_ARCHIVIST);
      } else if (boughtContact?.id === 'bribed_orderly') {
        cues.push(SOUND_CUES.ARC2_CONTACT_ORDERLY);
      } else if (boughtContact?.id === 'microfilm_scanner') {
        cues.push(SOUND_CUES.ARC2_CONTACT_SCANNER);
      } else if (boughtContact?.id === 'audio_transcriber') {
        cues.push(SOUND_CUES.ARC2_CONTACT_TRANSCRIBER);
      }
      break;
    }
    case SOUND_ACTION_HINTS.ARC2_DEDUCTION:
      if ((next.arc2?.pinnedExhibits?.length ?? 0) > (prev.arc2?.pinnedExhibits?.length ?? 0)) {
        cues.push(SOUND_CUES.ARC2_DEDUCTION);
      }
      break;
    case SOUND_ACTION_HINTS.ARC2_STRING_WIRE: {
      const prevPairs = prev.arc2?.wiredPairs ?? [];
      const nextPairs = next.arc2?.wiredPairs ?? [];
      if (nextPairs.length < prevPairs.length) {
        cues.push(SOUND_CUES.ARC2_STRING_UNWIRE);
      } else if (setsDiffer(prevPairs, nextPairs)) {
        const prevSet = new Set(prevPairs);
        const addedPairs = nextPairs.filter((p) => !prevSet.has(p));
        const lockedCurated = addedPairs.some((p) => Boolean(matchCorroborationPair(p)));
        cues.push(lockedCurated ? SOUND_CUES.ARC2_CORROBORATION_LOCK : SOUND_CUES.ARC2_STRING_WIRE);
      }
      break;
    }
    case SOUND_ACTION_HINTS.ARC2_REEL_SCRUB:
      if ((next.arc2?.reelScrubCount ?? 0) > (prev.arc2?.reelScrubCount ?? 0)) {
        cues.push(SOUND_CUES.ARC2_REEL_SCRUB);
      }
      break;
    case SOUND_ACTION_HINTS.ARC2_REDACTION_DISSOLVE:
      if (
        (next.arc2?.dissolvedRedactions?.length ?? 0) >
        (prev.arc2?.dissolvedRedactions?.length ?? 0)
      ) {
        cues.push(SOUND_CUES.ARC2_REDACTION_DISSOLVE);
      }
      break;
    case SOUND_ACTION_HINTS.ARC2_DESK_ANOMALY: {
      const interactedDesk = Object.values(DESK_ANOMALY_TYPES).find(
        (t) =>
          (next.arc2?.deskAnomalyInteractions?.[t.id] ?? 0) >
          (prev.arc2?.deskAnomalyInteractions?.[t.id] ?? 0),
      );
      if (interactedDesk?.id === DESK_ANOMALY_TYPES.EVIDENCE_ENVELOPE.id) {
        cues.push(SOUND_CUES.ARC2_ANOMALY_ENVELOPE);
      } else if (interactedDesk?.id === DESK_ANOMALY_TYPES.FOGGED_SPECTACLES.id) {
        cues.push(SOUND_CUES.ARC2_ANOMALY_SPECTACLES);
      } else if (interactedDesk?.id === DESK_ANOMALY_TYPES.REEL_TAPE.id) {
        cues.push(SOUND_CUES.ARC2_ANOMALY_REEL_TAPE);
      } else if (interactedDesk?.id === DESK_ANOMALY_TYPES.ENAMEL_EXHIBIT.id) {
        cues.push(SOUND_CUES.ARC2_ANOMALY_ENAMEL);
      }
      break;
    }
    case SOUND_ACTION_HINTS.ARC2_CHAPTER:
      if (next.arc2?.fileZeroRevealed && !prev.arc2?.fileZeroRevealed) {
        cues.push(SOUND_CUES.ARC2_FILE_ZERO);
      } else if ((next.arc2?.completedCasePivots ?? 0) > (prev.arc2?.completedCasePivots ?? 0)) {
        cues.push(SOUND_CUES.ARC2_CHAPTER);
      }
      break;
    case SOUND_ACTION_HINTS.ARC3_ENTER:
      if (
        next.activeArc === GAME_ARCS.ARC_3_CONVERGENCE &&
        prev.activeArc !== GAME_ARCS.ARC_3_CONVERGENCE
      ) {
        cues.push(SOUND_CUES.ARC3_ENTER);
      }
      break;
    case SOUND_ACTION_HINTS.ARC3_WRENCH:
      if ((next.arc3?.wrenchCount ?? 0) > (prev.arc3?.wrenchCount ?? 0)) {
        cues.push(SOUND_CUES.ARC3_WRENCH);
      }
      break;
    case SOUND_ACTION_HINTS.ARC3_UNMAKE:
      if (arc3ShatteredJarCount(next.arc3) > arc3ShatteredJarCount(prev.arc3)) {
        cues.push(SOUND_CUES.ARC3_JAR_SHATTER);
      } else if (
        (next.arc3?.completedUnmakes?.length ?? 0) > (prev.arc3?.completedUnmakes?.length ?? 0)
      ) {
        cues.push(SOUND_CUES.ARC3_UNMAKE_TEAR);
      }
      break;
    case SOUND_ACTION_HINTS.ARC3_RUPTURE:
      if (arc3TotalRuptureInteractions(next.arc3) > arc3TotalRuptureInteractions(prev.arc3)) {
        cues.push(SOUND_CUES.ARC3_RUPTURE_POP);
      }
      break;
    case SOUND_ACTION_HINTS.ARC3_WEDGE:
      if ((next.arc3?.lockedWedges?.length ?? 0) > (prev.arc3?.lockedWedges?.length ?? 0)) {
        cues.push(SOUND_CUES.ARC3_WEDGE_DRIVE);
      }
      break;
    case SOUND_ACTION_HINTS.ARC3_FINAL_PULL:
      if (next.arc3?.submersionComplete && !prev.arc3?.submersionComplete) {
        cues.push(SOUND_CUES.ARC3_SUBMERSION_END);
      } else if (
        (next.arc3?.finalPullsCompleted ?? 0) > (prev.arc3?.finalPullsCompleted ?? 0)
      ) {
        cues.push(SOUND_CUES.ARC3_FINAL_PULL);
      }
      break;
    default:
      break;
  }

  // Passive / emergent chamber events
  if ((next.echoPulsesTriggered ?? 0) > (prev.echoPulsesTriggered ?? 0)) {
    cues.push(SOUND_CUES.ECHO_PULSE);
  }
  if ((next.marrowBloomCount ?? 0) > (prev.marrowBloomCount ?? 0)) {
    cues.push(SOUND_CUES.MARROW_BLOOM);
  }
  if (
    action === SOUND_ACTION_HINTS.TICK &&
    (next.graftedTendrils ?? 0) > (prev.graftedTendrils ?? 0)
  ) {
    cues.push(SOUND_CUES.GRAFT_PERFORM);
  }
  if (
    next.activeArc === GAME_ARCS.ARC_1_CHAMBER &&
    prev.activeArc === GAME_ARCS.ARC_1_CHAMBER &&
    next.phase > prev.phase &&
    !cues.includes(SOUND_CUES.EXCISION_CUT)
  ) {
    cues.push(SOUND_CUES.PHASE_SHIFT);
  }

  if (
    action === SOUND_ACTION_HINTS.TICK &&
    next.activeArc === GAME_ARCS.ARC_3_CONVERGENCE &&
    arc3IsActivelyClotting(next.arc3) &&
    !arc3IsActivelyClotting(prev.arc3)
  ) {
    cues.push(SOUND_CUES.ARC3_CLOT_WARNING);
  }

  // Quiet story condensation cue when a brand-new fragment unlocks
  const newStoryUnlocked =
    next.activeDialogue != null &&
    next.activeDialogue.id !== prev.activeDialogue?.id &&
    (next.triggeredDialogueIds?.length ?? 0) > (prev.triggeredDialogueIds?.length ?? 0);
  if (
    newStoryUnlocked &&
    !cues.includes(SOUND_CUES.EXCISION_CUT) &&
    !cues.includes(SOUND_CUES.PLEAD_SALVATION) &&
    !cues.includes(SOUND_CUES.ARC2_FILE_ZERO) &&
    !cues.includes(SOUND_CUES.ARC3_SUBMERSION_END)
  ) {
    cues.push(SOUND_CUES.STORY_DRIP);
  }

  return cues;
}

/**
 * Pure ambient soundscape parameter mapper matching Android `SoundCueResolver.computeAmbientProfile`.
 * Strictly enforces Slow-Boil quarantine: zero fleshDepth/cardiacPulse while hauntDepth < 0.58.
 */
export function computeAmbientProfile(state) {
  if (!state || state.activeArc === GAME_ARCS.ARC_1_CHAMBER) {
    const p = Math.max(0, Math.min(4, state?.phase ?? 0));
    const clamped = (state?.clampRemainingSeconds ?? 0) > 0;
    const still = Boolean(state?.isStillnessActive);
    const humTable = [0.24, 0.28, 0.34, 0.4, 0.46];
    const dripTable = [0.08, 0.26, 0.42, 0.5, 0.56];
    const fleshTable = [0.0, 0.12, 0.36, 0.62, 0.82];
    const hum = humTable[p] * (still ? 0.72 : 1.0);
    const drips = dripTable[p] + (still ? 0.28 : 0.0);
    const flesh = fleshTable[p];
    const cardiac = p >= 4 ? 0.72 : p === 3 ? 0.18 : 0.0;
    return {
      activeArc: GAME_ARCS.ARC_1_CHAMBER,
      phase: p,
      humGain: clamp01(hum),
      dripRate: clamp01(drips),
      fleshDepth: clamp01(flesh),
      cardiacPulse: cardiac,
      stillnessFocus: still ? 1.0 : 0.0,
      clampTension: clamped ? 0.9 : 0.0,
      rainMix: 0.0,
      complicityBleed: 0.0,
    };
  }
  if (state.activeArc === GAME_ARCS.ARC_2_INVESTIGATION) {
    const desk = state.arc2 ?? {};
    const haunt = clamp01(arc2HauntDepth(desk));
    const silent = arc2IsSilenceActive(desk);
    const scrubbing = arc2IsReelScrubbing(desk);
    const fileZero = Boolean(desk.fileZeroRevealed);
    const roomBleed = haunt < 0.58 ? 0.0 : clamp01((haunt - 0.58) / 0.42);
    return {
      activeArc: GAME_ARCS.ARC_2_INVESTIGATION,
      phase: fileZero ? 6 : 5,
      humGain: scrubbing ? 0.34 : silent ? 0.18 : 0.26,
      dripRate: clamp01(0.08 + haunt * 0.28 + (silent ? 0.22 : 0.0)),
      fleshDepth: clamp01(roomBleed * 0.52),
      cardiacPulse: fileZero ? 0.45 : roomBleed * 0.24,
      stillnessFocus: silent ? 0.88 : 0.0,
      clampTension: scrubbing ? 0.45 : 0.0,
      rainMix: silent ? 0.88 : 0.72,
      complicityBleed: haunt,
    };
  }
  const breach = state.arc3 ?? {};
  const flood = clamp01(arc3FloodLevel(breach));
  const clotting = arc3IsActivelyClotting(breach);
  const submerged = Boolean(breach.submersionComplete);
  return {
    activeArc: GAME_ARCS.ARC_3_CONVERGENCE,
    phase: 7,
    humGain: submerged ? 0.14 : clamp01(0.32 + flood * 0.24),
    dripRate: submerged ? 0.12 : clamp01(0.38 + flood * 0.42),
    fleshDepth: clamp01(0.45 + flood * 0.55),
    cardiacPulse: submerged ? 0.48 : clamp01(0.28 + flood * 0.68),
    stillnessFocus: submerged ? 0.92 : 0.0,
    clampTension: clotting ? 0.62 : 0.0,
    rainMix: 0.0,
    complicityBleed: clamp01(0.55 + flood * 0.45),
  };
}
