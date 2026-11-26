/**
 * Pure tab unlock-key and readiness alert evaluators for Arc 1 and Arc 2 decks.
 */

import {
  AUDIT_REDACTIONS,
  CASE_EXHIBITS,
  CREATURE_LIST,
  CULT_DOSSIER_LINES,
  INVESTIGATOR_CONTACTS,
  REPEATABLE_SCAR_GRAFTS,
  SCAR_NODES,
  SPECIMEN_JAR_LIST,
  arc2CanDissolveRedaction,
  arc2CanPivotNextCase,
  arc2CanTriggerReelScrub,
  arc2CanUnlockExhibit,
  arc2IsBulkWarrantUnlocked,
  arc2IsContactUnlocked,
  arc2IsRedactionSolventUnlocked,
  arc2IsReelScrubUnlocked,
  arc2MaxStringSpools,
  canAffordChoirLung,
  canAffordGraft,
  canBreachToArc2,
  canBuyRepeatableGraft,
  canClearNextMilestone,
  canDredgeSump,
  canExcise,
  canOfferToThroat,
  canUnlockNode,
  effectiveJarCost,
  isColonyProliferationUnlocked,
  isCreatureUnlocked,
  isGraftUnlocked,
  isJarUnlocked,
  isRepeatableGraftsUnlocked,
  isScarTreeUnlocked,
  nextExcisionMilestone,
} from '../engine.js?v=3.13';

export function markTabSeen(seenMap, tab, keys) {
  let bucket = seenMap.get(tab);
  if (!bucket) {
    bucket = new Set();
    seenMap.set(tab, bucket);
  }
  for (const k of keys) bucket.add(k);
}

export function arc1UnlockKeys(state, tab) {
  const keys = new Set();
  if (tab === 'apparatus') {
    for (const c of CREATURE_LIST) {
      if (isCreatureUnlocked(state, c)) keys.add(`creature_${c.id}`);
    }
    if (isGraftUnlocked(state)) keys.add('graft_tendril');
    if (isColonyProliferationUnlocked(state)) keys.add('colony_proliferation');
    if (state.highestExcisionMilestone >= 4) keys.add('graft_choir_lung');
  } else if (tab === 'jars_and_graft') {
    for (const jar of SPECIMEN_JAR_LIST) {
      if (isJarUnlocked(state, jar)) keys.add(`jar_${jar.id}`);
    }
  } else if (tab === 'scar_tree') {
    if (isScarTreeUnlocked(state)) keys.add('scar_tree');
    if (isRepeatableGraftsUnlocked(state)) keys.add('scar_repeatables');
    const hasAffordableScar =
      isScarTreeUnlocked(state) &&
      (SCAR_NODES.some((n) => canUnlockNode(state, n.id)) ||
        REPEATABLE_SCAR_GRAFTS.some((g) => canBuyRepeatableGraft(state, g)));
    if (hasAffordableScar) keys.add(`scar_budget_${state.scarTissue}`);
  } else if (tab === 'chamber_rites') {
    if (state.highestExcisionMilestone >= 2) keys.add('rite_m2_drain');
    if (state.highestExcisionMilestone >= 3) keys.add('rite_m3_echo');
    if (state.highestExcisionMilestone >= 4) keys.add('rite_m4_clamp');
    if (state.highestExcisionMilestone >= 5) keys.add('rite_m5_throat');
  } else if (tab === 'excision') {
    if (
      canExcise(state) ||
      canBreachToArc2(state) ||
      state.cycle > 0 ||
      state.highestExcisionMilestone > 0
    ) {
      keys.add('excision_tab');
    }
    if (canExcise(state)) keys.add(`excision_ready_c${state.cycle}`);
    if (canClearNextMilestone(state)) {
      const next = nextExcisionMilestone(state);
      if (next) keys.add(`milestone_${next.number}_ready_c${state.cycle}`);
    }
    if (canBreachToArc2(state)) keys.add('breach_arc2');
  }
  return keys;
}

export function isArc1TabReady(state, tab) {
  if (tab === 'apparatus') {
    return canAffordGraft(state) || canAffordChoirLung(state);
  }
  if (tab === 'jars_and_graft') {
    return SPECIMEN_JAR_LIST.some(
      (jar) =>
        isJarUnlocked(state, jar) &&
        !state.ownedJars.includes(jar.id) &&
        state.stillness >= effectiveJarCost(state, jar),
    );
  }
  if (tab === 'scar_tree') {
    return false;
  }
  if (tab === 'chamber_rites') {
    return (
      canDredgeSump(state) ||
      CULT_DOSSIER_LINES.some((line) => canOfferToThroat(state, line.id))
    );
  }
  if (tab === 'excision') {
    return false;
  }
  return false;
}

export function shouldAlertArc1Tab(state, tab, activeTab, seenKeysByTab) {
  if (tab === activeTab) return false;
  const currentKeys = arc1UnlockKeys(state, tab);
  const seen = seenKeysByTab?.get(tab);
  for (const key of currentKeys) {
    if (!seen || !seen.has(key)) return true;
  }
  return isArc1TabReady(state, tab);
}

export function arc2UnlockKeys(state, tab) {
  const desk = state.arc2;
  const keys = new Set();
  if (tab === 'contacts') {
    for (const c of INVESTIGATOR_CONTACTS) {
      if (arc2IsContactUnlocked(desk, c)) keys.add(`arc2_contact_${c.id}`);
    }
    if (arc2IsBulkWarrantUnlocked(desk)) keys.add('arc2_warrant');
  } else if (tab === 'board') {
    for (const exId of desk.pinnedExhibits ?? []) {
      keys.add(`arc2_exhibit_${exId}`);
    }
    keys.add(`arc2_spools_${arc2MaxStringSpools(desk)}`);
  } else if (tab === 'dossier') {
    for (const ex of CASE_EXHIBITS) {
      if (!ex.isKeystone && (desk.caseAngle ?? 1) >= ex.unlockCaseAngle) {
        keys.add(`arc2_dossier_ex_${ex.id}`);
      }
    }
    if (arc2IsReelScrubUnlocked(desk)) keys.add('arc2_reel_scrub');
    if (arc2IsRedactionSolventUnlocked(desk)) keys.add('arc2_redactions');
  } else if (tab === 'revision') {
    keys.add(`arc2_case_${desk.caseAngle ?? 1}`);
    if (arc2CanPivotNextCase(desk)) keys.add(`arc2_pivot_ready_${desk.caseAngle ?? 1}`);
    if (desk.fileZeroRevealed) keys.add('arc2_file_zero');
  }
  return keys;
}

export function isArc2TabReady(state, tab) {
  const desk = state.arc2;
  if (tab === 'contacts') return false;
  if (tab === 'board') {
    return (
      (desk.pinnedExhibits?.length ?? 0) >= 2 &&
      (desk.wiredPairs?.length ?? 0) < arc2MaxStringSpools(desk)
    );
  }
  if (tab === 'dossier') {
    return (
      CASE_EXHIBITS.some((ex) => arc2CanUnlockExhibit(desk, ex)) ||
      arc2CanTriggerReelScrub(desk) ||
      AUDIT_REDACTIONS.some((r) => arc2CanDissolveRedaction(desk, r))
    );
  }
  // Revision relies strictly on unlock keys so the first tap silences its alert pulse cleanly
  if (tab === 'revision') return false;
  return false;
}

export function shouldAlertArc2Tab(state, tab, activeTab, seenKeysByTab) {
  if (tab === activeTab) return false;
  const currentKeys = arc2UnlockKeys(state, tab);
  const seen = seenKeysByTab?.get(tab);
  for (const key of currentKeys) {
    if (!seen || !seen.has(key)) return true;
  }
  return isArc2TabReady(state, tab);
}
