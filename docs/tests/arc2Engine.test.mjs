import test from 'node:test';
import assert from 'node:assert/strict';

import {
  ARC2_BALANCE,
  AUDIT_REDACTIONS,
  CASE_ANGLES,
  CASE_EXHIBITS,
  CORROBORATIONS,
  CULT_DOSSIER_LINES,
  DESK_ANOMALY_TYPES,
  DREDGED_RELICS,
  EXCISION_MILESTONES,
  GAME_ARCS,
  INVESTIGATOR_CONTACTS,
  SCAR_NODES,
  arc2CanDissolveRedaction,
  arc2CanPivotNextCase,
  arc2CanTriggerReelScrub,
  arc2CanUnlockExhibit,
  arc2ContactCost,
  arc2HauntDepth,
  arc2InspectYield,
  arc2IsBulkWarrantUnlocked,
  arc2IsContactUnlocked,
  arc2IsRedactionSolventUnlocked,
  arc2IsReelScrubUnlocked,
  arc2LeadsPerSecond,
  arc2MaxStringSpools,
  arc2SilencePerSecond,
  canonicalPairKey,
  deserializeState,
  devAssignArc2Resources,
  devResetAll,
  devResetArc2,
  devSetActiveArc,
  devSetAllScarNodes,
  devSetArc2AllExhibits,
  devSpawnDeskAnomaly,
  devSpawnWallAnomaly,
  devToggleArc2Lamp,
  dissolveRedaction,
  enterArc3Convergence,
  initialState,
  inspectCaseFile,
  interactDeskAnomaly,
  pivotNextCase,
  recruitContact,
  recruitMaxContact,
  serializeState,
  tick,
  toggleExhibitPair,
  toggleLamp,
  triggerReelScrub,
  unlockExhibit,
} from '../src/engine.js';
import { realCatalog } from './testHelpers.mjs';

test('Arc 2 Slow-Boil Lamp On/Off, Corkboard Wiring, Bulk Warrants, Reel Scrub, 4 Redactions, Soft-Reset Pivots, and File #00 work end-to-end', () => {
  let state = devSetActiveArc(initialState(realCatalog), GAME_ARCS.ARC_2_INVESTIGATION, realCatalog);
  assert.equal(state.activeArc, GAME_ARCS.ARC_2_INVESTIGATION);
  assert.equal(state.arc2.caseAngle, 1);
  assert.equal(state.arc2.lampOn, true);
  assert.equal(arc2SilencePerSecond(state.arc2), 0);
  assert.equal(state.activeDialogue?.id, 'dlg_arc2_entry');

  // Lamp On gathers full Leads; Lamp Off dims Leads to 0.25x and activates After-Hours Silence
  const lampOnTap = arc2InspectYield(state.arc2);
  state = inspectCaseFile(state, realCatalog);
  assert.equal(state.arc2.leads, lampOnTap);

  state = toggleLamp(state, realCatalog);
  assert.equal(state.arc2.lampOn, false);
  assert.equal(state.arc2.lampOffCount, 1);
  assert.ok(arc2SilencePerSecond(state.arc2) > 0);
  assert.ok(arc2InspectYield(state.arc2) < lampOnTap);

  state = tick(state, 120.0, realCatalog);
  assert.ok(state.arc2.silence >= 45);

  // Unlock Case I Silence Exhibit & wire first curated Corroboration (Smudged Ledger Date)
  assert.equal(arc2CanUnlockExhibit(state.arc2, 'unlisted_pay_stub'), true);
  state = unlockExhibit(state, 'unlisted_pay_stub', realCatalog);
  assert.ok(state.arc2.pinnedExhibits.includes('unlisted_pay_stub'));

  state = toggleExhibitPair(state, 'water_stained_petition', 'unlisted_pay_stub', realCatalog);
  assert.equal(state.arc2.wiredPairs.length, 1);
  assert.ok(state.arc2.discoveredCorroborations.includes('smudged_ledger_date'));
  assert.ok(state.triggeredDialogueIds.includes('dlg_arc2_corr_smudged_date'));

  // Spool cap is 1 in Case I — cannot wire a second pair until pivoting
  state = devSetArc2AllExhibits(state, true, realCatalog);
  const blockedWire = toggleExhibitPair(state, 'tax_plat_1998', 'second_chair_polaroid', realCatalog);
  assert.equal(blockedWire.arc2.wiredPairs.length, 1);

  // Unwire and wire a non-curated Speculative Thread (+15%)
  state = toggleExhibitPair(state, 'water_stained_petition', 'unlisted_pay_stub', realCatalog);
  assert.equal(state.arc2.wiredPairs.length, 0);
  const baseLeadYield = arc2InspectYield(state.arc2);
  state = toggleExhibitPair(state, 'water_stained_petition', 'tax_plat_1998', realCatalog);
  assert.equal(state.arc2.wiredPairs.length, 1);
  assert.ok(arc2InspectYield(state.arc2) > baseLeadYield * 1.14);

  // Soft-reset Case I Pivot wipes Leads & Contacts but preserves Exhibits, Wired Pairs, & Corroborations
  state = devAssignArc2Resources(state, { leads: 12000, silence: 120 }, realCatalog);
  state = recruitContact(state, 'night_archivist', realCatalog);
  assert.equal(state.arc2.contacts.night_archivist, 1);
  assert.equal(arc2CanPivotNextCase(state.arc2), true);
  state = pivotNextCase(state, realCatalog);
  assert.equal(state.arc2.caseAngle, 2);
  assert.equal(state.arc2.completedCasePivots, 1);
  assert.equal(state.arc2.leads, 0);
  assert.equal(Object.keys(state.arc2.contacts).length, 0);
  assert.equal(arc2MaxStringSpools(state.arc2), 2);
  assert.ok(state.arc2.pinnedExhibits.includes('tax_plat_1998'));
  assert.ok(state.arc2.discoveredCorroborations.includes('smudged_ledger_date'));

  // Case III unlocks Bulk Warrants and Acoustic Reel Scrubbing (Lamp Off only)
  state = devAssignArc2Resources(state, { caseAngle: 3, leads: 50000, silence: 500 }, realCatalog);
  assert.equal(arc2IsBulkWarrantUnlocked(state.arc2), true);
  assert.equal(arc2IsReelScrubUnlocked(state.arc2), true);
  state = recruitMaxContact(state, 'night_archivist', realCatalog);
  assert.ok((state.arc2.contacts.night_archivist ?? 0) > 5);

  if (state.arc2.lampOn) state = toggleLamp(state, realCatalog);
  const preScrubSilenceRate = arc2SilencePerSecond(state.arc2);
  assert.equal(arc2CanTriggerReelScrub(state.arc2), true);
  state = triggerReelScrub(state, realCatalog);
  assert.equal(state.arc2.reelScrubCount, 1);
  assert.ok(arc2SilencePerSecond(state.arc2) > preScrubSilenceRate * 2.5);

  // Case IV unlocks 4 Audit Redactions (Lamp Off only); Clause III enables 15% Lamp-On Silence trickle
  state = devAssignArc2Resources(state, { caseAngle: 4, leads: 15000000, silence: 20000 }, realCatalog);
  assert.equal(arc2IsRedactionSolventUnlocked(state.arc2), true);
  for (const r of AUDIT_REDACTIONS) {
    assert.equal(arc2CanDissolveRedaction(state.arc2, r), true);
    state = dissolveRedaction(state, r.id, realCatalog);
  }
  assert.equal(state.arc2.dissolvedRedactions.length, 4);

  // With Clause III dissolved, turning Lamp On still yields 15% Silence trickle
  state = toggleLamp(state, realCatalog);
  assert.equal(state.arc2.lampOn, true);
  assert.ok(arc2SilencePerSecond(state.arc2) > 0);

  // Desk anomaly spawning & interaction
  state = devSpawnDeskAnomaly(state);
  assert.ok(state.arc2.activeDeskAnomalies.length >= 1);
  state = interactDeskAnomaly(state, state.arc2.activeDeskAnomalies[0].id, realCatalog);

  // Serialization v3 round-trip preserves all Arc 2 fields cleanly
  const raw = serializeState(state);
  assert.equal(JSON.parse(raw).version, 3);
  const restored = deserializeState(raw, realCatalog);
  assert.ok(restored !== null);
  assert.equal(restored.arc2.caseAngle, 4);
  assert.equal(restored.arc2.dissolvedRedactions.length, 4);
  assert.equal(restored.arc2.reelScrubCount, 1);

  // Complete Case V -> unseals File #00 -> enter Arc 3
  state = devAssignArc2Resources(
    state,
    { caseAngle: 5, completedChapters: 4, leads: 90000000, silence: 15000 },
    realCatalog,
  );
  state = {
    ...state,
    arc2: {
      ...state.arc2,
      discoveredCorroborations: CORROBORATIONS.slice(0, 5).map((c) => c.id),
    },
  };
  assert.equal(arc2CanPivotNextCase(state.arc2), true);
  state = pivotNextCase(state, realCatalog);
  assert.equal(state.arc2.fileZeroRevealed, true);
  assert.equal(state.arc2.completedCasePivots, 5);
  assert.equal(state.activeDialogue?.id, 'dlg_arc2_ch4_file_zero');

  const arc3 = enterArc3Convergence(state, realCatalog);
  assert.equal(arc3.activeArc, GAME_ARCS.ARC_3_CONVERGENCE);
  assert.equal(arc3.activeDialogue?.id, 'dlg_arc3_convergence');

  // Dev helpers sanity
  let devState = devToggleArc2Lamp(devResetArc2(devSetActiveArc(arc3, GAME_ARCS.ARC_2_INVESTIGATION, realCatalog), realCatalog), realCatalog);
  assert.equal(devState.arc2.lampOn, false);
  devState = devSetAllScarNodes(devSetActiveArc(devState, GAME_ARCS.ARC_1_CHAMBER, realCatalog), true, realCatalog);
  assert.equal(devState.unlockedScarNodes.length, SCAR_NODES.length);
  devState = devSpawnWallAnomaly(devState);
  assert.ok(devState.activeAnomalies.length >= 1);
  const wiped = devResetAll(realCatalog);
  assert.equal(wiped.activeArc, GAME_ARCS.ARC_1_CHAMBER);

  // Structural invariants
  assert.equal(EXCISION_MILESTONES.length, 6);
  assert.equal(DREDGED_RELICS.length, 3);
  assert.equal(CULT_DOSSIER_LINES.length, 4);
  assert.equal(CASE_ANGLES.length, 5);
  assert.equal(INVESTIGATOR_CONTACTS.length, 4);
  assert.equal(CASE_EXHIBITS.length, 10);
  assert.equal(CORROBORATIONS.length, 10);
  assert.equal(AUDIT_REDACTIONS.length, 4);
  assert.equal(Object.keys(DESK_ANOMALY_TYPES).length, 4);
  assert.equal(ARC2_BALANCE.LAMP_OFF_BASE_LEADS_FACTOR, 0.25);
});

test('Schema v2 -> v3 save migration preserves Arc 1 untouched and maps legacy Arc 2 progress safely', () => {
  const legacyV2 = JSON.stringify({
    version: 2,
    activeArc: 'arc_2',
    ichor: 123456,
    cycleLifetimeIchor: 250000,
    stillness: 99,
    cycleLifetimeStillness: 150,
    scarTissue: 12,
    availableScarTissue: 5,
    highestExcisionMilestone: 6,
    unlockedScarNodes: ['first_incision'],
    cycle: 6,
    arc2: {
      leads: 8800,
      lifetimeLeads: 25000,
      silence: 75,
      lifetimeSilence: 140,
      complicity: 0.45,
      contacts: { night_archivist: 4 },
      ownedDeductions: ['hydro_ward_invoices', 'missing_caretaker_roster'],
      completedChapters: 2,
      activeDeskAnomalies: [{ id: 7, type: 'red_string', xFraction: 0.2, yFraction: 0.4 }],
      deskAnomalyInteractions: { red_string: 3 },
      fileZeroRevealed: false,
    },
  });

  const migrated = deserializeState(legacyV2, realCatalog);
  assert.ok(migrated !== null);
  assert.equal(migrated.ichor, 123456);
  assert.equal(migrated.scarTissue, 12);
  assert.equal(migrated.highestExcisionMilestone, 6);
  assert.equal(migrated.arc2.caseAngle, 3);
  assert.equal(migrated.arc2.completedCasePivots, 2);
  assert.equal(migrated.arc2.lampOn, true);
  assert.ok(migrated.arc2.pinnedExhibits.includes('tax_plat_1998'));
  assert.ok(migrated.arc2.pinnedExhibits.includes('second_chair_polaroid'));
  assert.ok(migrated.arc2.pinnedExhibits.includes('unlisted_pay_stub'));
  assert.ok(migrated.arc2.pinnedExhibits.includes('inside_condensation_swab'));
  assert.equal(migrated.arc2.activeDeskAnomalies[0].type, 'enamel_exhibit');
  assert.equal(migrated.arc2.deskAnomalyInteractions.enamel_exhibit, 3);
});

test('Arc 2 Slow-Boil Quarantine keeps grotesque Room spoilers strictly out of Cases I–III', () => {
  const bannedEarlyTerms = [
    'hydro-ward',
    'molar',
    'grout',
    'valve',
    'ichor',
    'suture',
    'caretaker',
    'tile',
    'vessel',
    'scar',
  ];
  const earlyIds = new Set([
    'dlg_arc2_entry',
    'dlg_arc2_lamp_first_off',
    'dlg_arc2_silence_first',
    'dlg_arc2_envelope',
    'dlg_arc2_corr_smudged_date',
    'dlg_arc2_case1_dead_end',
    'dlg_arc2_case2_entry',
    'dlg_arc2_spectacles',
    'dlg_arc2_corr_unlisted_annex',
    'dlg_arc2_corr_warm_room_tone',
    'dlg_arc2_corr_phantom_roster',
    'dlg_arc2_case2_dead_end',
    'dlg_arc2_case3_entry',
    'dlg_arc2_reel_tape',
    'dlg_arc2_reel_scrub_first',
    'dlg_arc2_reel_scrub_deep',
    'dlg_arc2_corr_cadence',
    'dlg_arc2_corr_witness_two_places',
    'dlg_arc2_corr_borrowed_coat',
    'dlg_arc2_case3_dead_end',
  ]);

  const entryMap = new Map(realCatalog.entries.map((e) => [e.id, e]));
  for (const id of earlyIds) {
    const entry = entryMap.get(id);
    assert.ok(entry, `Expected early Arc 2 entry ${id}`);
    const body = `${entry.speaker} ${entry.lines.join(' ')}`.toLowerCase();
    for (const banned of bannedEarlyTerms) {
      assert.equal(
        body.includes(banned),
        false,
        `Early Arc 2 entry ${id} leaked quarantined Room term "${banned}"`,
      );
    }
  }

  // Enamel exhibit anomaly must NEVER spawn in Cases I–III
  for (const angle of [1, 2, 3]) {
    let state = devAssignArc2Resources(
      devSetActiveArc(initialState(realCatalog), GAME_ARCS.ARC_2_INVESTIGATION, realCatalog),
      { caseAngle: angle },
      realCatalog,
    );
    assert.ok(arc2HauntDepth(state.arc2) <= 0.56, `Case ${angle} hauntDepth must stay quarantined`);
    for (let i = 0; i < 40; i += 1) {
      state = { ...state, arc2: { ...state.arc2, activeDeskAnomalies: [] } };
      state = devSpawnDeskAnomaly(state);
      assert.notEqual(
        state.arc2.activeDeskAnomalies[0].type,
        DESK_ANOMALY_TYPES.ENAMEL_EXHIBIT.id,
        `enamel_exhibit must not spawn in Case ${angle}`,
      );
    }
  }
});

test('Arc 2 5-Case Pacing Simulation completes cleanly within the target 50–75 minute active-play window', () => {
  // Deterministic LCG RNG so simulation is 100% reproducible
  let seed = 1337;
  const rng = () => {
    seed = (seed * 1664525 + 1013904223) % 4294967296;
    return seed / 4294967296;
  };

  let state = devSetActiveArc(initialState(realCatalog), GAME_ARCS.ARC_2_INVESTIGATION, realCatalog);
  const optimalPairsByPriority = [
    ['tax_plat_1998', 'drainage_loop_blueprint'],
    ['water_stained_petition', 'intake_thumb_cast'],
    ['drainage_loop_blueprint', 'intake_thumb_cast'],
    ['coat_claim_ticket', 'second_chair_polaroid'],
    ['tax_plat_1998', 'second_chair_polaroid'],
    ['unlisted_pay_stub', 'tuesday_statement_copy'],
    ['inside_condensation_swab', 'tuesday_statement_copy'],
    ['coat_claim_ticket', 'reel_four_waveform'],
    ['inside_condensation_swab', 'reel_four_waveform'],
    ['water_stained_petition', 'unlisted_pay_stub'],
  ];

  let elapsedSeconds = 0;
  const maxSeconds = 90 * 60;

  while (!state.arc2.fileZeroRevealed && elapsedSeconds < maxSeconds) {
    const desk = state.arc2;
    const curCase = CASE_ANGLES[desk.caseAngle - 1];

    // 1. Unlock any affordable Silence Exhibits
    for (const ex of CASE_EXHIBITS) {
      if (arc2CanUnlockExhibit(state.arc2, ex)) {
        state = unlockExhibit(state, ex.id, realCatalog);
      }
    }

    // 2. Wire highest-value available curated corroborations up to spool cap
    const pinnedSet = new Set(state.arc2.pinnedExhibits);
    const desiredKeys = [];
    for (const [a, b] of optimalPairsByPriority) {
      if (pinnedSet.has(a) && pinnedSet.has(b)) {
        desiredKeys.push(canonicalPairKey(a, b));
        if (desiredKeys.length >= arc2MaxStringSpools(state.arc2)) break;
      }
    }
    // Unwire pairs not in desiredKeys
    for (const existing of [...state.arc2.wiredPairs]) {
      if (!desiredKeys.includes(existing)) {
        const [a, b] = existing.split('|');
        state = toggleExhibitPair(state, a, b, realCatalog);
      }
    }
    for (const key of desiredKeys) {
      if (!state.arc2.wiredPairs.includes(key) && state.arc2.wiredPairs.length < arc2MaxStringSpools(state.arc2)) {
        const [a, b] = key.split('|');
        state = toggleExhibitPair(state, a, b, realCatalog);
      }
    }

    // 3. Determine whether we currently need Silence (for Exhibits, Redactions, Reel Scrub, or Case Silence gate)
    const nextUnpinnedEx = CASE_EXHIBITS.find(
      (e) => !e.isKeystone && e.unlockCaseAngle <= state.arc2.caseAngle && !state.arc2.pinnedExhibits.includes(e.id),
    );
    const nextRedaction =
      state.arc2.caseAngle >= 4
        ? AUDIT_REDACTIONS.find((r) => !state.arc2.dissolvedRedactions.includes(r.id))
        : null;
    const needSilence =
      Boolean(nextUnpinnedEx) ||
      (Boolean(nextRedaction) &&
        (state.arc2.dissolvedRedactions.length < curCase.requiredRedactions || state.arc2.caseAngle >= 4)) ||
      (curCase.requiredReelScrubs > state.arc2.reelScrubCount) ||
      state.arc2.silence < curCase.silenceCost;

    // Build an initial small contact base before switching Lamp Off so Silence scales nicely
    const totalContacts = Object.values(state.arc2.contacts).reduce((a, b) => a + b, 0);
    const wantLampOn = !needSilence || totalContacts < 6;
    if (state.arc2.lampOn !== wantLampOn) {
      state = toggleLamp(state, realCatalog);
    }

    // 4. Trigger Reel Scrub & dissolve Redactions when Lamp Off
    if (!state.arc2.lampOn && arc2CanTriggerReelScrub(state.arc2)) {
      state = triggerReelScrub(state, realCatalog);
    }
    if (!state.arc2.lampOn) {
      for (const r of AUDIT_REDACTIONS) {
        if (arc2CanDissolveRedaction(state.arc2, r)) {
          state = dissolveRedaction(state, r.id, realCatalog);
        }
      }
    }

    // 5. Interact with active Desk Anomalies
    for (const a of [...state.arc2.activeDeskAnomalies]) {
      state = interactDeskAnomaly(state, a.id, realCatalog);
    }

    // 6. Recruit best ROI contact when affordable (stop buying contacts once close to Case Leads gate)
    if (state.arc2.leads < curCase.leadsCost) {
      let bestContact = null;
      let bestPayback = Infinity;
      for (const c of INVESTIGATOR_CONTACTS) {
        if (!arc2IsContactUnlocked(state.arc2, c)) continue;
        const cost = arc2ContactCost(state.arc2, c);
        if (state.arc2.leads < cost) continue;
        const remainingNeeded = curCase.leadsCost - state.arc2.leads;
        if (cost > remainingNeeded * 0.45 && arc2LeadsPerSecond(state.arc2) > 0) continue;
        const payback = cost / c.baseLeadsPerSec;
        if (payback < bestPayback) {
          bestPayback = payback;
          bestContact = c;
        }
      }
      if (bestContact) {
        state = recruitContact(state, bestContact.id, realCatalog);
      }
    }

    // 7. Moderate human tapping (~1.5 stamps/sec when Lamp On, 0.3/sec when Lamp Off)
    const tapsThisSec = state.arc2.lampOn ? (elapsedSeconds % 2 === 0 ? 2 : 1) : elapsedSeconds % 4 === 0 ? 1 : 0;
    for (let t = 0; t < tapsThisSec; t += 1) {
      state = inspectCaseFile(state, realCatalog);
    }

    // 8. Pivot Case Angle as soon as ready
    if (arc2CanPivotNextCase(state.arc2)) {
      state = pivotNextCase(state, realCatalog);
    }

    state = tick(state, 1.0, realCatalog, rng);
    elapsedSeconds += 1;
  }

  const elapsedMinutes = elapsedSeconds / 60;
  assert.equal(
    state.arc2.fileZeroRevealed,
    true,
    `Expected File #00 to be unsealed within 90m; stuck at Case ${state.arc2.caseAngle}, leads=${Math.round(state.arc2.leads)}, silence=${Math.round(state.arc2.silence)}`,
  );
  assert.ok(
    elapsedMinutes >= 45 && elapsedMinutes <= 80,
    `Expected 5-Case Arc 2 pacing in ~50–75m band (tolerance 45–80m), actual: ${elapsedMinutes.toFixed(1)} min`,
  );
});
