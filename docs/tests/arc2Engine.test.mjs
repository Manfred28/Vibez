import test from 'node:test';
import assert from 'node:assert/strict';

import {
  ARC2_BALANCE,
  CASE_DEDUCTIONS,
  CULT_DOSSIER_LINES,
  DESK_ANOMALY_TYPES,
  DREDGED_RELICS,
  EXCISION_MILESTONES,
  GAME_ARCS,
  INVESTIGATOR_CONTACTS,
  SCAR_NODES,
  arc2CanConnectNextChapter,
  connectNextChapter,
  deserializeState,
  devAssignArc2Resources,
  devResetAll,
  devSetActiveArc,
  devSetAllScarNodes,
  devSpawnDeskAnomaly,
  devSpawnWallAnomaly,
  enterArc3Convergence,
  initialState,
  inspectCaseFile,
  interactDeskAnomaly,
  pinDeduction,
  recruitContact,
  serializeState,
  tick,
} from '../src/engine.js';
import { realCatalog } from './testHelpers.mjs';

test('Arc 2 Investigator Desk, Contacts, Deductions, Desk Anomalies, File #00 Twist, and Arc 3 Convergence work end-to-end', () => {
  let state = devSetActiveArc(initialState(realCatalog), GAME_ARCS.ARC_2_INVESTIGATION, realCatalog);
  assert.equal(state.activeArc, GAME_ARCS.ARC_2_INVESTIGATION);

  state = inspectCaseFile(state, realCatalog);
  assert.ok(state.arc2.leads >= 1);

  // Silence activates after 2.5s idle and climbs Complicity
  state = tick(state, 3.0, realCatalog);
  assert.equal(state.arc2.isSilenceActive, true);

  // Assign resources via Dev helper, recruit Contact, pin Deduction, interact with Desk Anomaly
  state = devAssignArc2Resources(state, { leads: 250000, silence: 500 }, realCatalog);
  state = recruitContact(state, 'night_archivist', realCatalog);
  assert.equal(state.arc2.contacts.night_archivist, 1);

  state = pinDeduction(state, 'hydro_ward_invoices', realCatalog);
  assert.ok(state.arc2.ownedDeductions.includes('hydro_ward_invoices'));

  state = devSpawnDeskAnomaly(state);
  assert.equal(state.arc2.activeDeskAnomalies.length, 1);
  state = interactDeskAnomaly(state, state.arc2.activeDeskAnomalies[0].id, realCatalog);
  assert.equal(state.arc2.activeDeskAnomalies.length, 0);

  // Serialization preserves Arc 2 progress cleanly
  const restoredMidArc2 = deserializeState(serializeState(state), realCatalog);
  assert.ok(restoredMidArc2 !== null);
  assert.equal(restoredMidArc2.activeArc, GAME_ARCS.ARC_2_INVESTIGATION);
  assert.equal(restoredMidArc2.arc2.contacts.night_archivist, 1);
  assert.ok(restoredMidArc2.arc2.ownedDeductions.includes('hydro_ward_invoices'));

  // Connect all 4 chapters to trigger File #00 twist
  for (let ch = 1; ch <= 4; ch += 1) {
    assert.equal(arc2CanConnectNextChapter(state.arc2), true);
    state = connectNextChapter(state, realCatalog);
    assert.equal(state.arc2.completedChapters, ch);
  }
  assert.equal(state.arc2.fileZeroRevealed, true);
  assert.equal(state.arc2.complicity, 1.0);
  assert.equal(state.activeDialogue?.id, 'dlg_arc2_ch4_file_zero');

  // Cross into Arc 3 open convergence threshold
  const arc3 = enterArc3Convergence(state, realCatalog);
  assert.equal(arc3.activeArc, GAME_ARCS.ARC_3_CONVERGENCE);
  assert.equal(arc3.activeDialogue?.id, 'dlg_arc3_convergence');

  // Dev utilities: unlock all scars, spawn wall anomaly, reset all
  let devState = devSetAllScarNodes(devSetActiveArc(arc3, GAME_ARCS.ARC_1_CHAMBER, realCatalog), true, realCatalog);
  assert.equal(devState.unlockedScarNodes.length, SCAR_NODES.length);
  devState = devSpawnWallAnomaly(devState);
  assert.ok(devState.activeAnomalies.length >= 1);
  const wiped = devResetAll(realCatalog);
  assert.equal(wiped.activeArc, GAME_ARCS.ARC_1_CHAMBER);
  assert.equal(wiped.highestExcisionMilestone, 0);
  assert.equal(EXCISION_MILESTONES.length, 6);
  assert.equal(DREDGED_RELICS.length, 3);
  assert.equal(CULT_DOSSIER_LINES.length, 4);
  assert.equal(INVESTIGATOR_CONTACTS.length, 4);
  assert.equal(CASE_DEDUCTIONS.length, 3);
  assert.equal(Object.keys(DESK_ANOMALY_TYPES).length, 4);
  assert.equal(ARC2_BALANCE.SILENCE_IDLE_DELAY_SEC, 2.5);
});
