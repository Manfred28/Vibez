import test from 'node:test';
import assert from 'node:assert/strict';

import {
  CREATURE_TYPES,
  GAME_ARCS,
  ORGAN_BALANCE,
  buyCreature,
  devAssignArc1Resources,
  devAssignArc2Resources,
  devSetActiveArc,
  dissolveRedaction,
  driveMemoryWedge,
  enterArc3Convergence,
  initialState,
  inspectCaseFile,
  interactDeskAnomaly,
  performArc3FinalPull,
  performArc3Unmake,
  performBreachToArc2,
  performChoirLungGraft,
  performExcision,
  pivotNextCase,
  pressValve,
  recruitContact,
  tearBreachRupture,
  tick,
  toggleExhibitPair,
  toggleLamp,
  triggerReelScrub,
  unlockExhibit,
  wrenchArc3Breach,
} from '../src/engine.js';
import {
  DEEP_SHELF_JARS,
  FOUNDATIONAL_SHELF_JARS,
  REPEAT_EVENT_DRIP_CHANCE,
  STORY_DRIP_QUEUE_GAP_MS,
  applyDripMotionProfile,
  arc1UnlockKeys,
  arc2UnlockKeys,
  arc3UnlockKeys,
  computeDripMotionProfile,
  createStoryDripQueue,
  isArc1TabReady,
  isArc2TabReady,
  isArc3TabReady,
  partitionChamberShelfJars,
  shouldAlertArc1Tab,
  shouldAlertArc2Tab,
  shouldAlertArc3Tab,
  shouldTriggerEventDrip,
} from '../src/ui.js';
import {
  AUDIO_MUTE_STORAGE_KEY,
  IRON_MODE_RATIOS,
  SOUND_ACTION_HINTS,
  SOUND_CUES,
  computeAmbientProfile,
  createWebAudioEngine,
  readMutedPreference,
  resolveTransitionCues,
  writeMutedPreference,
} from '../src/audio.js';
import {
  computeDeskLayerAlphas,
  computeWallLayerAlphas,
  getWeepingLampLayout,
} from '../src/chamberCanvas.js';
import {
  ASSET_VERSION,
  ANOMALY_ASSET_URLS,
  DESK_ANOMALY_ASSET_URLS,
  DESK_STAGE_ASSET_URLS,
  EXHIBIT_ASSET_URLS,
  JAR_ASSET_URLS,
  WALL_ASSET_URLS,
} from '../src/version.js';
import { readDocsFile, realCatalog } from './testHelpers.mjs';

test('.drip-card multi-layer parallax CSS, randomized delay intervals, and safe animationend handling', () => {
  // Deterministic low vs high RNG seeds confirm non-uniform randomized delays & parallax bounds
  const storyLow = computeDripMotionProfile('story', () => 0);
  const storyHigh = computeDripMotionProfile('story', () => 1);
  assert.equal(storyLow.kind, 'story');
  assert.equal(storyLow.delayMs, 40);
  assert.equal(storyHigh.delayMs, 280);
  assert.ok(storyHigh.delayMs > storyLow.delayMs);
  assert.ok(storyLow.durationSec >= 15.0 && storyHigh.durationSec <= 17.0);
  assert.ok(storyLow.parallaxZ < storyHigh.parallaxZ);
  assert.ok(storyLow.ribbonDelayMs > storyLow.delayMs);
  assert.ok(storyLow.beadDelayMs > storyLow.delayMs);

  const eventLow = computeDripMotionProfile('event', () => 0);
  const eventHigh = computeDripMotionProfile('event', () => 1);
  assert.equal(eventLow.kind, 'event');
  assert.equal(eventLow.delayMs, 90);
  assert.equal(eventHigh.delayMs, 540);
  assert.ok(storyLow.durationSec > eventHigh.durationSec * 2, 'Story drip must be explicitly slower than chamber event drip');
  assert.ok(eventHigh.delayMs > storyHigh.delayMs, 'Event lane should have wider delay jitter than priority Story lane');
  assert.ok(eventHigh.parallaxZ < 0, 'Event drips stay recessed on a deeper Z-plane behind Story drips');

  // Out-of-range / NaN RNG values clamp safely without producing NaN CSS variables
  const clamped = computeDripMotionProfile('story', () => Number.NaN);
  assert.ok(Number.isFinite(clamped.delayMs) && Number.isFinite(clamped.parallaxZ));

  const applied = new Map();
  const mockCard = {
    style: {
      setProperty(name, value) {
        applied.set(name, value);
      },
    },
  };
  applyDripMotionProfile(mockCard, storyHigh);
  for (const cssVar of [
    '--drip-delay',
    '--drip-duration',
    '--drip-drift-x',
    '--drip-drift-mid-x',
    '--drip-parallax-z',
    '--drip-scale',
    '--drip-tilt',
    '--drip-ribbon-lag',
    '--drip-ribbon-delay',
    '--drip-bead-delay',
    '--drip-bead-duration',
    '--drip-bubble-parallax-x',
    '--drip-bubble-parallax-y',
    '--drip-bubble-delay',
  ]) {
    assert.ok(applied.has(cssVar), `Missing CSS custom property ${cssVar}`);
    assert.ok(!String(applied.get(cssVar)).includes('NaN'), `NaN detected in ${cssVar}`);
  }

  // Verify docs/styles.css defines 3D parallax container, both fill-mode, viscous keyframes, and reduced motion
  const css = readDocsFile('styles.css');
  assert.ok(css.includes('perspective: 760px;'));
  assert.ok(css.includes('transform-style: preserve-3d;'));
  assert.ok(css.includes('var(--drip-delay, 0ms) both;'));
  assert.ok(css.includes('@keyframes drip-ribbon-seep'));
  assert.ok(css.includes('@keyframes drip-bead-swell'));
  assert.ok(css.includes('@keyframes drip-bubble-parallax'));
  assert.ok(css.includes('@keyframes tab-clot-pulse'));
  assert.ok(css.includes('.deck-tab--alert:not(.is-active)'));
  assert.ok(css.includes('.drip-lane--event.is-suppressed'));
  assert.ok(css.includes('@media (prefers-reduced-motion: reduce)'));

  // Verify drippingMargin.js ignores bubbled child animationend events & suppresses chamber drips during active story drips
  const dripSrc = readDocsFile('src/ui/drippingMargin.js');
  assert.ok(dripSrc.includes('if (event.target !== card) return;'));
  assert.ok(dripSrc.includes('storyDripLayer.childElementCount > 0'));
  assert.ok(dripSrc.includes("eventDripLayer.classList.add('is-suppressed')"));
});

test('Web procedural audio cue resolver, ambient profile escalation, and independent mute persistence match Android 1:1', () => {
  assert.equal(Object.keys(SOUND_CUES).length, 52);

  const start = initialState(realCatalog);

  // Failed unaffordable buy stays silent
  const failedBuy = buyCreature(start, CREATURE_TYPES.SUTURE_HAND.id, realCatalog);
  assert.deepEqual(
    resolveTransitionCues(start, failedBuy, SOUND_ACTION_HINTS.CREATURE_BUY),
    [],
  );

  // All 4 purchasable organisms resolve to distinct signature cues
  const richBase = {
    ...start,
    ichor: 50000,
    cycleLifetimeIchor: 50000,
    phase: 3,
    highestExcisionMilestone: 2,
    triggeredDialogueIds: [
      'dlg_p0_suture_hand',
      'dlg_p1_leech_clerk',
      'dlg_p2_weeping_lamp',
      'dlg_m2_sump_siphon',
    ],
  };
  for (const [creatureId, expectedCue] of [
    [CREATURE_TYPES.SUTURE_HAND.id, SOUND_CUES.CREATURE_BUY_SUTURE_HAND],
    [CREATURE_TYPES.LEECH_CLERK.id, SOUND_CUES.CREATURE_BUY_LEECH_CLERK],
    [CREATURE_TYPES.WEEPING_LAMP.id, SOUND_CUES.CREATURE_BUY_WEEPING_LAMP],
    [CREATURE_TYPES.SUMP_SIPHON.id, SOUND_CUES.CREATURE_BUY_SUMP_SIPHON],
  ]) {
    const bought = buyCreature(richBase, creatureId, realCatalog);
    assert.ok(
      resolveTransitionCues(richBase, bought, SOUND_ACTION_HINTS.CREATURE_BUY).includes(
        expectedCue,
      ),
      `Expected ${creatureId} to emit ${expectedCue}`,
    );
  }

  // Choir-Lung graft resolves to dedicated GRAFT_CHOIR_LUNG cue (distinct from Choir-Tendril)
  const lungBase = {
    ...richBase,
    highestExcisionMilestone: 4,
    stillness: 300,
    graftedTendrils: 1,
    creatures: {
      [CREATURE_TYPES.SUTURE_HAND.id]: 2,
      [CREATURE_TYPES.LEECH_CLERK.id]: 2,
      [CREATURE_TYPES.WEEPING_LAMP.id]: 2,
    },
  };
  const lungGrafted = performChoirLungGraft(lungBase, realCatalog);
  assert.ok(
    resolveTransitionCues(lungBase, lungGrafted, SOUND_ACTION_HINTS.GRAFT_PERFORM).includes(
      SOUND_CUES.GRAFT_CHOIR_LUNG,
    ),
    'Choir-Lung graft must emit dedicated GRAFT_CHOIR_LUNG cue',
  );
  assert.equal(SOUND_CUES.RELIC_INSPECT, 'relic_inspect');

  // First valve press emits VALVE_PRESS + STORY_DRIP
  const afterTap = pressValve(start, realCatalog);
  assert.deepEqual(
    resolveTransitionCues(start, afterTap, SOUND_ACTION_HINTS.VALVE_PRESS),
    [SOUND_CUES.VALVE_PRESS, SOUND_CUES.STORY_DRIP],
  );

  // Dev console mutations emit zero cues; switching tabs has no sound cue
  const devState = devAssignArc1Resources(start, { ichor: 500000 }, realCatalog);
  assert.deepEqual(resolveTransitionCues(start, devState, SOUND_ACTION_HINTS.DEV), []);
  assert.equal('TAB_SWITCH' in SOUND_CUES, false);

  // Passive tick emits ECHO_PULSE, MARROW_BLOOM, and PHASE_SHIFT
  const tickBase = {
    ...start,
    phase: 1,
    ichor: 1599,
    cycleLifetimeIchor: 1599,
    highestExcisionMilestone: 4,
    creatures: { [CREATURE_TYPES.SUTURE_HAND.id]: 4 },
    echoDiversionPercent: 50,
    echoChargeSeconds: ORGAN_BALANCE.ECHO_PULSE_INTERVAL_SEC - 0.1,
    clampRemainingSeconds: 0.1,
  };
  const afterTick = tick(tickBase, 0.5, realCatalog);
  const tickCues = resolveTransitionCues(tickBase, afterTick, SOUND_ACTION_HINTS.TICK);
  assert.ok(tickCues.includes(SOUND_CUES.ECHO_PULSE));
  assert.ok(tickCues.includes(SOUND_CUES.MARROW_BLOOM));
  assert.ok(tickCues.includes(SOUND_CUES.PHASE_SHIFT));

  // Excision VI Plead for Salvation -> PLEAD_SALVATION, Arc 2 Stamp -> ARC2_STAMP, Ch4 -> ARC2_FILE_ZERO, Arc 3 -> ARC3_ENTER
  const readyPlea = {
    ...start,
    highestExcisionMilestone: 5,
    ichor: 160000000000,
    cycleLifetimeIchor: 160000000000,
    phase: 4,
  };
  const arc2 = performBreachToArc2(readyPlea, realCatalog);
  assert.deepEqual(
    resolveTransitionCues(readyPlea, arc2, SOUND_ACTION_HINTS.BREACH_TO_ARC2),
    [SOUND_CUES.PLEAD_SALVATION],
  );

  const stamped = inspectCaseFile(arc2, realCatalog);
  assert.ok(
    resolveTransitionCues(arc2, stamped, SOUND_ACTION_HINTS.ARC2_STAMP).includes(
      SOUND_CUES.ARC2_STAMP,
    ),
  );

  // Distinct Arc 2 Lamp OFF vs Lamp ON brass chain cues
  const lampOff = toggleLamp(arc2, realCatalog);
  assert.ok(
    resolveTransitionCues(arc2, lampOff, SOUND_ACTION_HINTS.ARC2_LAMP_TOGGLE).includes(
      SOUND_CUES.ARC2_LAMP_OFF,
    ),
  );
  const lampBackOn = toggleLamp(lampOff, realCatalog);
  assert.ok(
    resolveTransitionCues(lampOff, lampBackOn, SOUND_ACTION_HINTS.ARC2_LAMP_TOGGLE).includes(
      SOUND_CUES.ARC2_LAMP_ON,
    ),
  );

  // All 4 Arc 2 contacts map to distinct signature foley cues
  const richArc2 = devAssignArc2Resources(
    lampOff,
    { caseAngle: 4, completedChapters: 3, leads: 5_000_000, silence: 10_000 },
    realCatalog,
  );
  for (const [contactId, expectedCue] of [
    ['night_archivist', SOUND_CUES.ARC2_CONTACT_ARCHIVIST],
    ['bribed_orderly', SOUND_CUES.ARC2_CONTACT_ORDERLY],
    ['microfilm_scanner', SOUND_CUES.ARC2_CONTACT_SCANNER],
    ['audio_transcriber', SOUND_CUES.ARC2_CONTACT_TRANSCRIBER],
  ]) {
    const hired = recruitContact(richArc2, contactId, realCatalog);
    assert.ok(
      resolveTransitionCues(richArc2, hired, SOUND_ACTION_HINTS.ARC2_CONTACT).includes(expectedCue),
      `Expected contact ${contactId} to emit ${expectedCue}`,
    );
  }

  // Corkboard Stringing: curated pair -> ARC2_CORROBORATION_LOCK; unwiring -> ARC2_STRING_UNWIRE; non-curated -> ARC2_STRING_WIRE
  const readyBoard = devAssignArc2Resources(lampOff, { caseAngle: 4, silence: 1000 }, realCatalog);
  const pinnedEx = unlockExhibit(
    unlockExhibit(readyBoard, 'unlisted_pay_stub', realCatalog),
    'tax_plat_1998',
    realCatalog,
  );
  const corroborated = toggleExhibitPair(
    pinnedEx,
    'water_stained_petition',
    'unlisted_pay_stub',
    realCatalog,
  );
  assert.ok(
    resolveTransitionCues(pinnedEx, corroborated, SOUND_ACTION_HINTS.ARC2_STRING_WIRE).includes(
      SOUND_CUES.ARC2_CORROBORATION_LOCK,
    ),
  );
  const unwired = toggleExhibitPair(
    corroborated,
    'water_stained_petition',
    'unlisted_pay_stub',
    realCatalog,
  );
  assert.ok(
    resolveTransitionCues(corroborated, unwired, SOUND_ACTION_HINTS.ARC2_STRING_WIRE).includes(
      SOUND_CUES.ARC2_STRING_UNWIRE,
    ),
  );
  const speculativeWire = toggleExhibitPair(
    unwired,
    'water_stained_petition',
    'tax_plat_1998',
    realCatalog,
  );
  assert.ok(
    resolveTransitionCues(unwired, speculativeWire, SOUND_ACTION_HINTS.ARC2_STRING_WIRE).includes(
      SOUND_CUES.ARC2_STRING_WIRE,
    ),
  );

  // All 4 Arc 2 Desk Anomalies resolve to distinct tactile cues
  for (const [anomalyType, expectedCue] of [
    ['evidence_envelope', SOUND_CUES.ARC2_ANOMALY_ENVELOPE],
    ['fogged_spectacles', SOUND_CUES.ARC2_ANOMALY_SPECTACLES],
    ['reel_tape', SOUND_CUES.ARC2_ANOMALY_REEL_TAPE],
    ['enamel_exhibit', SOUND_CUES.ARC2_ANOMALY_ENAMEL],
  ]) {
    const withDeskAnomaly = {
      ...readyBoard,
      arc2: {
        ...readyBoard.arc2,
        activeDeskAnomalies: [{ id: 88, type: anomalyType, x: 0.5, y: 0.5, remainingSec: 12 }],
      },
    };
    const collected = interactDeskAnomaly(withDeskAnomaly, 88, realCatalog);
    assert.ok(
      resolveTransitionCues(withDeskAnomaly, collected, SOUND_ACTION_HINTS.ARC2_DESK_ANOMALY).includes(
        expectedCue,
      ),
      `Expected desk anomaly ${anomalyType} to emit ${expectedCue}`,
    );
  }

  const scrubbed = triggerReelScrub(corroborated, realCatalog);
  assert.ok(
    resolveTransitionCues(corroborated, scrubbed, SOUND_ACTION_HINTS.ARC2_REEL_SCRUB).includes(
      SOUND_CUES.ARC2_REEL_SCRUB,
    ),
  );

  const dissolved = dissolveRedaction(scrubbed, 'clause_seepage', realCatalog);
  assert.ok(
    resolveTransitionCues(scrubbed, dissolved, SOUND_ACTION_HINTS.ARC2_REDACTION_DISSOLVE).includes(
      SOUND_CUES.ARC2_REDACTION_DISSOLVE,
    ),
  );

  const c5Ready = {
    ...devAssignArc2Resources(arc2, { caseAngle: 5, completedChapters: 4, leads: 90000000, silence: 15000 }, realCatalog),
  };
  c5Ready.arc2 = {
    ...c5Ready.arc2,
    discoveredCorroborations: [
      'smudged_ledger_date',
      'unlisted_annex',
      'warm_room_tone',
      'phantom_roster',
      'sub_surface_cadence',
    ],
    reelScrubCount: 1,
    dissolvedRedactions: ['clause_seepage', 'clause_shift', 'clause_quiet', 'clause_intake'],
  };
  const fileZeroState = pivotNextCase(c5Ready, realCatalog);
  assert.deepEqual(
    resolveTransitionCues(c5Ready, fileZeroState, SOUND_ACTION_HINTS.ARC2_CHAPTER),
    [SOUND_CUES.ARC2_FILE_ZERO],
  );

  const arc3 = enterArc3Convergence(fileZeroState, realCatalog);
  assert.ok(
    resolveTransitionCues(fileZeroState, arc3, SOUND_ACTION_HINTS.ARC3_ENTER).includes(SOUND_CUES.ARC3_ENTER),
  );

  // Ambient profile escalation across Phases 0..4, Stillness, Clamp, Arc 2 Slow-Boil Quarantine, and Arc 3
  const p0 = computeAmbientProfile(start);
  const p4 = computeAmbientProfile({ ...start, phase: 4 });
  const still = computeAmbientProfile({ ...start, phase: 2, isStillnessActive: true });
  const active = computeAmbientProfile({ ...start, phase: 2, isStillnessActive: false });
  const clamped = computeAmbientProfile({ ...start, phase: 4, clampRemainingSeconds: 5 });
  const arc2EarlyProfile = computeAmbientProfile(lampOff);
  const arc2LateProfile = computeAmbientProfile({
    ...fileZeroState,
    arc2: { ...fileZeroState.arc2, lampOn: false },
  });
  const arc3Profile = computeAmbientProfile(arc3);

  assert.ok(p4.humGain > p0.humGain);
  assert.ok(p4.fleshDepth > p0.fleshDepth);
  assert.equal(p0.cardiacPulse, 0);
  assert.ok(p4.cardiacPulse > 0.5);
  assert.ok(still.dripRate > active.dripRate);
  assert.ok(still.humGain < active.humGain);
  assert.ok(clamped.clampTension > 0.8);
  // Slow-Boil Quarantine: zero fleshDepth/cardiacPulse in Case I
  assert.equal(arc2EarlyProfile.fleshDepth, 0);
  assert.equal(arc2EarlyProfile.cardiacPulse, 0);
  assert.ok(arc2EarlyProfile.rainMix > 0.7);
  // Late Case V / File #00 restores Room bleed; Arc 3 drops rain and surges crimson arterial resonance
  assert.ok(arc2LateProfile.fleshDepth > 0.4 && arc2LateProfile.cardiacPulse > 0.4);
  assert.equal(arc3Profile.phase, 7);
  assert.equal(arc3Profile.rainMix, 0);
  assert.ok(arc3Profile.fleshDepth >= 0.45 && arc3Profile.cardiacPulse >= 0.25);

  // Arc 3 breach cues resolve cleanly across wrench, jar shatter, unmake tear, rupture, wedge, clotting, and finale
  const arc3Wrenched = wrenchArc3Breach(arc3, realCatalog);
  assert.ok(
    resolveTransitionCues(arc3, arc3Wrenched, SOUND_ACTION_HINTS.ARC3_WRENCH).includes(SOUND_CUES.ARC3_WRENCH),
  );
  const arc3Rich = { ...arc3, arc3: { ...arc3.arc3, hemorrhage: 100 } };
  const arc3Jar = performArc3Unmake(arc3Rich, 'shatter_yesterdays_dust', realCatalog);
  assert.ok(
    resolveTransitionCues(arc3Rich, arc3Jar, SOUND_ACTION_HINTS.ARC3_UNMAKE).includes(SOUND_CUES.ARC3_JAR_SHATTER),
  );
  const arc3Rev = performArc3Unmake(arc3Rich, 'reverse_suture_hands', realCatalog);
  assert.ok(
    resolveTransitionCues(arc3Rich, arc3Rev, SOUND_ACTION_HINTS.ARC3_UNMAKE).includes(SOUND_CUES.ARC3_UNMAKE_TEAR),
  );
  const arc3WithRup = {
    ...arc3,
    arc3: {
      ...arc3.arc3,
      activeRuptures: [{ id: 1, type: 'clotting_seam', xFraction: 0.2, yFraction: 0.3, remainingSeconds: 10 }],
    },
  };
  const arc3Popped = tearBreachRupture(arc3WithRup, 1, realCatalog);
  assert.ok(
    resolveTransitionCues(arc3WithRup, arc3Popped, SOUND_ACTION_HINTS.ARC3_RUPTURE).includes(
      SOUND_CUES.ARC3_RUPTURE_POP,
    ),
  );
  const arc3WedgeReady = { ...arc3, arc3: { ...arc3.arc3, fracture: 120 } };
  const arc3Wedged = driveMemoryWedge(arc3WedgeReady, realCatalog);
  assert.ok(
    resolveTransitionCues(arc3WedgeReady, arc3Wedged, SOUND_ACTION_HINTS.ARC3_WEDGE).includes(
      SOUND_CUES.ARC3_WEDGE_DRIVE,
    ),
  );
  const arc3Finale = {
    ...arc3,
    arc3: {
      ...arc3.arc3,
      lockedWedges: ['broken_grout', 'unstitched_seam', 'splintered_ribs', 'shared_pulse', 'the_open_seam'],
    },
  };
  const arc3Pull1 = performArc3FinalPull(arc3Finale, realCatalog);
  assert.ok(
    resolveTransitionCues(arc3Finale, arc3Pull1, SOUND_ACTION_HINTS.ARC3_FINAL_PULL).includes(
      SOUND_CUES.ARC3_FINAL_PULL,
    ),
  );
  const arc3PreEnd = { ...arc3Finale, arc3: { ...arc3Finale.arc3, finalPullsCompleted: 4 } };
  const arc3Ended = performArc3FinalPull(arc3PreEnd, realCatalog);
  assert.ok(
    resolveTransitionCues(arc3PreEnd, arc3Ended, SOUND_ACTION_HINTS.ARC3_FINAL_PULL).includes(
      SOUND_CUES.ARC3_SUBMERSION_END,
    ),
  );

  // Mute preference defaults to unmuted (false), persists independently of game save, and works headlessly
  const storeMap = new Map();
  const mockStorage = {
    getItem: (k) => (storeMap.has(k) ? storeMap.get(k) : null),
    setItem: (k, v) => storeMap.set(k, String(v)),
  };
  assert.equal(readMutedPreference(mockStorage), false);
  writeMutedPreference(true, mockStorage);
  assert.equal(storeMap.get(AUDIO_MUTE_STORAGE_KEY), 'true');
  assert.equal(readMutedPreference(mockStorage), true);

  const webEngine = createWebAudioEngine({ storage: mockStorage });
  assert.equal(webEngine.isMuted(), true);
  assert.equal(webEngine.toggleMuted(), false);
  assert.equal(storeMap.get(AUDIO_MUTE_STORAGE_KEY), 'false');
  webEngine.playCues([SOUND_CUES.VALVE_PRESS], 0);
  webEngine.updateAmbience(p4);
  webEngine.setForegroundActive(false);
  webEngine.dispose();
});

test('Web tab alert evaluator, Grafting-in-Organisms alignment, and unseen/readiness clearing match Android 1:1', () => {
  const base = initialState(realCatalog);

  // Phase 2 unlocks JARS; unseen jars trigger alert while on apparatus tab
  const p2 = { ...base, ichor: 1700, cycleLifetimeIchor: 1700, phase: 2, stillness: 0 };
  const p2JarKeys = arc1UnlockKeys(p2, 'jars_and_graft');
  assert.ok(p2JarKeys.has('jar_yesterdays_dust'));
  assert.equal(p2JarKeys.has('jar_salt_from_the_grout'), false);
  assert.equal(shouldAlertArc1Tab(p2, 'jars_and_graft', 'apparatus', new Map()), true);

  // Opening JARS clears unseen alert when no jar is affordable
  const seen = new Map([['jars_and_graft', new Set(p2JarKeys)]]);
  assert.equal(shouldAlertArc1Tab(p2, 'jars_and_graft', 'apparatus', seen), false);

  // Becoming able to afford a jar pulses JARS while inactive, never while active
  const p2Afford = { ...p2, stillness: 30 };
  assert.equal(isArc1TabReady(p2Afford, 'jars_and_graft'), true);
  assert.equal(shouldAlertArc1Tab(p2Afford, 'jars_and_graft', 'apparatus', seen), true);
  assert.equal(shouldAlertArc1Tab(p2Afford, 'jars_and_graft', 'jars_and_graft', new Map()), false);

  // Grafting (Choir-Tendril & Choir-Lung) lives inside apparatus (ORGANISMS)
  const m4Ready = {
    ...base,
    phase: 3,
    cycle: 1,
    highestExcisionMilestone: 4,
    stillness: 250,
    graftedTendrils: 1,
    creatures: {
      [CREATURE_TYPES.SUTURE_HAND.id]: 2,
      [CREATURE_TYPES.LEECH_CLERK.id]: 2,
      [CREATURE_TYPES.WEEPING_LAMP.id]: 2,
    },
  };
  const appKeys = arc1UnlockKeys(m4Ready, 'apparatus');
  assert.ok(appKeys.has('graft_tendril'));
  assert.ok(appKeys.has('graft_choir_lung'));
  assert.equal(isArc1TabReady(m4Ready, 'apparatus'), true);
  assert.equal(shouldAlertArc1Tab(m4Ready, 'apparatus', 'chamber_rites', new Map([['apparatus', new Set(appKeys)]])), true);

  // Cycle 0 EXCISION stays hidden until ready, pulses once, and stops pulsing after first click
  assert.equal(arc1UnlockKeys(p2, 'excision').size, 0);
  const exciseReady = { ...base, ichor: 220000, cycleLifetimeIchor: 220000, phase: 4 };
  assert.equal(isArc1TabReady(exciseReady, 'excision'), false);
  assert.equal(shouldAlertArc1Tab(exciseReady, 'excision', 'apparatus', new Map()), true);
  const seenExcision = new Map([['excision', new Set(arc1UnlockKeys(exciseReady, 'excision'))]]);
  assert.equal(shouldAlertArc1Tab(exciseReady, 'excision', 'apparatus', seenExcision), false);

  // SCARS pulses with fresh Scar budget, stops after first click even with unspent Scars, and re-pulses after earning new Scars
  const withScars = {
    ...base,
    cycle: 1,
    highestExcisionMilestone: 1,
    scarTissue: 4,
    availableScarTissue: 4,
  };
  assert.equal(isArc1TabReady(withScars, 'scar_tree'), false);
  assert.equal(shouldAlertArc1Tab(withScars, 'scar_tree', 'apparatus', new Map()), true);
  const seenScars = new Map([['scar_tree', new Set(arc1UnlockKeys(withScars, 'scar_tree'))]]);
  assert.equal(shouldAlertArc1Tab(withScars, 'scar_tree', 'apparatus', seenScars), false);
  const earnedMoreScars = { ...withScars, scarTissue: 9, availableScarTissue: 9 };
  assert.equal(shouldAlertArc1Tab(earnedMoreScars, 'scar_tree', 'apparatus', seenScars), true);

  // Crossing M2 (1.8M) re-pulses EXCISION after prior M1/cycle-1 sighting
  const cycle1Ready = { ...withScars, ichor: 300000, cycleLifetimeIchor: 300000, phase: 4 };
  const seenCycle1Excision = new Map([['excision', new Set(arc1UnlockKeys(cycle1Ready, 'excision'))]]);
  assert.equal(shouldAlertArc1Tab(cycle1Ready, 'excision', 'apparatus', seenCycle1Excision), false);
  const crossedM2 = { ...cycle1Ready, ichor: 1800000, cycleLifetimeIchor: 1800000 };
  assert.equal(shouldAlertArc1Tab(crossedM2, 'excision', 'apparatus', seenCycle1Excision), true);

  // Arc 2 4-tab alerts: dossier readiness, board readiness, and revision first-click silencing
  const arc2Ready = {
    ...devSetActiveArc(base, GAME_ARCS.ARC_2_INVESTIGATION, realCatalog),
  };
  arc2Ready.arc2 = {
    ...arc2Ready.arc2,
    leads: 12000,
    lifetimeLeads: 12000,
    silence: 100,
    discoveredCorroborations: ['smudged_ledger_date'],
  };
  assert.ok(arc2UnlockKeys(arc2Ready, 'revision').has('arc2_pivot_ready_1'));
  assert.equal(isArc2TabReady(arc2Ready, 'dossier'), true);
  assert.equal(isArc2TabReady(arc2Ready, 'revision'), false);
  assert.equal(
    shouldAlertArc2Tab(arc2Ready, 'dossier', 'contacts', new Map([['dossier', new Set(['arc2_dossier_ex_unlisted_pay_stub'])]])),
    true,
  );
  // Revision pulses when pivot becomes ready, then silences immediately once seen
  assert.equal(shouldAlertArc2Tab(arc2Ready, 'revision', 'contacts', new Map()), true);
  const seenRev = new Map([['revision', new Set(arc2UnlockKeys(arc2Ready, 'revision'))]]);
  assert.equal(shouldAlertArc2Tab(arc2Ready, 'revision', 'contacts', seenRev), false);

  // Arc 3 2-tab alerts: UNMAKE readiness when affordable & WEDGES pulse when fracture reaches goal
  const arc3State = devSetActiveArc(base, GAME_ARCS.ARC_3_CONVERGENCE, realCatalog);
  assert.equal(isArc3TabReady(arc3State, 'unmake'), false);
  assert.equal(isArc3TabReady(arc3State, 'wedges'), false);
  const arc3ReadyBoth = {
    ...arc3State,
    arc3: { ...arc3State.arc3, hemorrhage: 50, fracture: 120 },
  };
  assert.equal(isArc3TabReady(arc3ReadyBoth, 'unmake'), true);
  assert.equal(isArc3TabReady(arc3ReadyBoth, 'wedges'), true);
  assert.ok(arc3UnlockKeys(arc3ReadyBoth, 'wedges').has('arc3_wedge_ready_1'));
  assert.equal(shouldAlertArc3Tab(arc3ReadyBoth, 'wedges', 'unmake', new Map()), true);
  assert.equal(shouldAlertArc3Tab(arc3ReadyBoth, 'wedges', 'wedges', new Map()), false);
});

test('Web wall/chamber event drip gate guarantees first sighting (100%) and applies 30% chance on repeats', () => {
  assert.equal(REPEAT_EVENT_DRIP_CHANCE, 0.3);

  const seen = new Set();
  // Empty key never triggers
  assert.equal(shouldTriggerEventDrip('', seen, () => 0.0), false);
  assert.equal(shouldTriggerEventDrip(null, seen, () => 0.0), false);

  // First sighting of a wall event always triggers even when rng returns 0.99
  assert.equal(shouldTriggerEventDrip('anomaly_pale_tooth', seen, () => 0.99), true);
  seen.add('anomaly_pale_tooth');

  // Repeat sightings use strict < 0.30 probability
  assert.equal(shouldTriggerEventDrip('anomaly_pale_tooth', seen, () => 0.0), true);
  assert.equal(shouldTriggerEventDrip('anomaly_pale_tooth', seen, () => 0.299), true);
  assert.equal(shouldTriggerEventDrip('anomaly_pale_tooth', seen, () => 0.3), false);
  assert.equal(shouldTriggerEventDrip('anomaly_pale_tooth', seen, () => 0.99), false);
  assert.equal(shouldTriggerEventDrip('anomaly_pale_tooth', seen, () => Number.NaN), false);

  // A distinct unseen wall event still triggers 100% regardless of rng
  assert.equal(shouldTriggerEventDrip('anomaly_glass_eye', seen, () => 0.99), true);

  // Verify drippingMargin.js checks story suppression BEFORE repeat gating & seen marking, and clears on reset
  const dripSrc = readDocsFile('src/ui/drippingMargin.js');
  const suppressIdx = dripSrc.indexOf('storyDripLayer.childElementCount > 0');
  const gateIdx = dripSrc.indexOf('shouldTriggerEventDrip(resolvedKey, seenEventDripKeys');
  const markIdx = dripSrc.indexOf('seenEventDripKeys.add(resolvedKey)');
  assert.ok(suppressIdx > 0 && gateIdx > suppressIdx && markIdx > gateIdx);
  assert.ok(dripSrc.includes('seenEventDripKeys.clear();'));

  // Verify app.js passes explicit event keys and renders story drips before observing chamber events
  const appSrc = readDocsFile('src/app.js');
  for (const keyToken of [
    "'echo_answer'",
    '`graft_${sacrificeKind}`',
    "'sump_dredge'",
    "'valve_clamp'",
    '`anomaly_${anomaly.type}`',
    "`desk_${anomaly?.type ?? 'anomaly'}`",
  ]) {
    assert.ok(appSrc.includes(keyToken), `app.js must pass explicit event key ${keyToken}`);
  }
  const anomalyApplyIdx = appSrc.indexOf('applyState(interactAnomaly(state, anomalyId, catalog)');
  const anomalyDripIdx = appSrc.indexOf('`anomaly_${anomaly.type}`');
  assert.ok(anomalyApplyIdx > 0 && anomalyDripIdx > anomalyApplyIdx, 'Anomaly drip must run after applyState so story drips suppress it cleanly');
});

test('Web Weeping Lamps hang strictly on left/right flanks behind foreground HUD (no middle lamp over income)', () => {
  assert.deepEqual(getWeepingLampLayout(0), []);
  for (let count = 1; count <= 6; count += 1) {
    const slots = getWeepingLampLayout(count);
    assert.equal(slots.length, Math.min(count, 5));
    for (const slot of slots) {
      assert.ok(
        slot.x <= 0.24 || slot.x >= 0.76,
        `Lamp at x=${slot.x} for count=${count} must stay clear of center income band (0.25..0.75)`,
      );
    }
  }

  // Verify drawChamberBackdrop renders lamps behind foreground HUD while drawForegroundLampsAndSacrifice only draws sacrifice FX
  const canvasSrc = readDocsFile('src/canvas/arc1ChamberCanvas.js');
  const backdropFnStart = canvasSrc.indexOf('export function drawChamberBackdrop(');
  const foregroundFnStart = canvasSrc.indexOf('export function drawForegroundLampsAndSacrifice(');
  const lampsCallIdx = canvasSrc.indexOf('drawWeepingLampsCeiling(ctx, width, lampCount');
  assert.ok(
    backdropFnStart >= 0 && foregroundFnStart > backdropFnStart && lampsCallIdx > backdropFnStart && lampsCallIdx < foregroundFnStart,
    'Weeping lamps must draw inside drawChamberBackdrop behind foreground income text',
  );
});

test('Web wall anomaly nodes use subtle CSS keyframe breathing/pulsing with desynchronized delays and reduced-motion support', () => {
  const css = readDocsFile('styles.css');
  const arc1UiSrc = readDocsFile('src/ui/arc1ChamberUi.js');
  const arc2UiSrc = readDocsFile('src/ui/arc2InvestigatorUi.js');

  // Verify both organic breathing and arterial pulse keyframes exist with translate(-50%, -50%) centering preserved
  assert.ok(css.includes('@keyframes wall-anomaly-breathe'));
  assert.ok(css.includes('@keyframes wall-anomaly-pulse'));
  assert.ok(css.includes('.anomaly-layer .anomaly-node,\n.anomaly-node--wall'));
  assert.ok(css.includes('animation: wall-anomaly-breathe var(--breath-duration, 4.6s)'));

  // Verify all 4 wall anomaly types have dedicated breathing tokens (and swollen_nodule uses pulse)
  for (const selector of [
    '.anomaly-node--pale_tooth',
    '.anomaly-node--glass_eye',
    '.anomaly-node--swollen_nodule',
    '.anomaly-node--chitin_rib',
  ]) {
    assert.ok(css.includes(selector), `styles.css must define ${selector}`);
  }
  const noduleIdx = css.indexOf('.anomaly-node--swollen_nodule');
  const ribIdx = css.indexOf('.anomaly-node--chitin_rib');
  assert.ok(
    noduleIdx > 0 && ribIdx > noduleIdx && css.slice(noduleIdx, ribIdx).includes('animation-name: wall-anomaly-pulse;'),
    'Swollen nodule must use the arterial wall-anomaly-pulse keyframe',
  );

  // Verify hover override and prefers-reduced-motion disable animation cleanly
  assert.ok(css.includes('.anomaly-layer .anomaly-node:hover'));
  const reducedMotionBlock = css.slice(css.indexOf('@media (prefers-reduced-motion: reduce)'));
  assert.ok(
    reducedMotionBlock.includes('.anomaly-layer .anomaly-node') &&
      reducedMotionBlock.includes('animation: none;'),
    'Reduced-motion media query must disable wall anomaly breathing animation',
  );

  // Verify arc1ChamberUi.js tags wall anomalies with anomaly-node--wall and negative --breath-delay (without tagging desk anomalies)
  assert.ok(arc1UiSrc.includes('anomaly-node anomaly-node--wall anomaly-node--${anomaly.type}'));
  assert.ok(arc1UiSrc.includes("btn.style.setProperty('--breath-delay'"));
  assert.ok(
    !arc2UiSrc.includes('anomaly-node--wall'),
    'Arc 2 desk anomalies must stay separate from Arc 1 wall anomaly breathing class',
  );
});

test('Web 5-stage progressive wall texture blending, heavy sump basin, sculpted Choir-Tendrils, and breathing Choir-Lungs match Android 1:1', () => {
  // Phase 0..4 Cycle 0 progression
  const p0 = computeWallLayerAlphas(0, 0, 0);
  assert.equal(p0.phase1Tile, 0.34);
  assert.equal(p0.phase2Fleshy, 0);
  assert.equal(p0.phase3Muscle, 0);
  assert.equal(p0.phase4Hollow, 0);
  assert.equal(p0.lateExcision, 0);

  const p1 = computeWallLayerAlphas(1, 0, 0);
  assert.equal(p1.phase1Tile, 0.76);
  assert.equal(p1.phase2Fleshy, 0);

  const p2 = computeWallLayerAlphas(2, 0, 0);
  assert.equal(p2.phase1Tile, 0.34);
  assert.equal(p2.phase2Fleshy, 0.8);
  assert.equal(p2.phase3Muscle, 0);

  const p3 = computeWallLayerAlphas(3, 0, 0);
  assert.equal(p3.phase3Muscle, 0.86);
  assert.equal(p3.phase4Hollow, 0);

  const p4 = computeWallLayerAlphas(4, 0, 0);
  assert.equal(p4.phase1Tile, 0);
  assert.equal(p4.phase3Muscle, 0.26);
  assert.equal(p4.phase4Hollow, 0.9);
  assert.equal(p4.lateExcision, 0);

  // Late-Excision surgical wall scar layer escalates across M3..M6 and scales cleanly by Phase
  const m3p0 = computeWallLayerAlphas(0, 3, 3);
  const m3p4 = computeWallLayerAlphas(4, 3, 3);
  const m5p4 = computeWallLayerAlphas(4, 5, 5);
  const m6p4 = computeWallLayerAlphas(4, 6, 6);
  assert.ok(m3p0.lateExcision > 0 && m3p0.lateExcision < m3p4.lateExcision);
  assert.ok(m3p4.lateExcision < m5p4.lateExcision && m5p4.lateExcision < m6p4.lateExcision);
  assert.ok(m6p4.lateExcision <= 0.82);

  // Verify arc1ChamberCanvas.js eliminated dashed CAD rings & flat side ovals, and includes new visual elements
  const canvasSrc = readDocsFile('src/canvas/arc1ChamberCanvas.js');
  assert.ok(!canvasSrc.includes('setLineDash([7, 7])'), 'Dashed CAD ring around valve must be replaced by bolted flange');
  assert.ok(canvasSrc.includes('wallImages.phase3') && canvasSrc.includes('wallImages.phase4') && canvasSrc.includes('wallImages.excisionLate'));
  assert.ok(canvasSrc.includes('visibleLungs'), 'Grafted Choir-Lungs must render visually around valve collar');
  assert.ok(canvasSrc.includes('const rivets ='), 'Floor sump basin must include riveted cast-iron architecture');
  assert.ok(canvasSrc.includes('helixPts') && canvasSrc.includes('segRadius'), 'Grafted Choir-Tendrils must render sculpted segmented stalks with helical sinew');

  // Verify centralized version registry & app.js preload all 5 wall textures + 4 wall anomalies + 6 specimen jars
  const allVersionedAssets = [
    ...Object.values(WALL_ASSET_URLS),
    ...Object.values(ANOMALY_ASSET_URLS),
    ...Object.values(JAR_ASSET_URLS),
  ];
  for (const expected of [
    `./assets/img_wall_phase1.jpg?v=${ASSET_VERSION}`,
    `./assets/img_wall_fleshy.jpg?v=${ASSET_VERSION}`,
    `./assets/img_wall_phase3.jpg?v=${ASSET_VERSION}`,
    `./assets/img_wall_phase4.jpg?v=${ASSET_VERSION}`,
    `./assets/img_wall_excision_late.jpg?v=${ASSET_VERSION}`,
    `./assets/img_anomaly_pale_tooth.jpg?v=${ASSET_VERSION}`,
    `./assets/img_anomaly_glass_eye.jpg?v=${ASSET_VERSION}`,
    `./assets/img_anomaly_swollen_nodule.jpg?v=${ASSET_VERSION}`,
    `./assets/img_anomaly_chitin_rib.jpg?v=${ASSET_VERSION}`,
    `./assets/img_jar_yesterdays_dust.jpg?v=${ASSET_VERSION}`,
    `./assets/img_jar_unspent_breath.jpg?v=${ASSET_VERSION}`,
    `./assets/img_jar_almost_a_name.jpg?v=${ASSET_VERSION}`,
    `./assets/img_jar_salt_from_the_grout.jpg?v=${ASSET_VERSION}`,
    `./assets/img_jar_a_borrowed_pulse.jpg?v=${ASSET_VERSION}`,
    `./assets/img_jar_the_uncounted_hour.jpg?v=${ASSET_VERSION}`,
  ]) {
    assert.ok(allVersionedAssets.includes(expected), `version.js must expose ${expected}`);
  }
  const appSrc = readDocsFile('src/app.js');
  assert.ok(appSrc.includes('WALL_ASSET_URLS.phase1') && appSrc.includes('WALL_ASSET_URLS.excisionLate'));
  assert.ok(appSrc.includes('ANOMALY_ASSET_URLS.pale_tooth') && appSrc.includes('ANOMALY_ASSET_URLS.chitin_rib'));
  assert.ok(appSrc.includes('JAR_ASSET_URLS.yesterdays_dust') && appSrc.includes('JAR_ASSET_URLS.the_uncounted_hour'));
});

test('Web photographic feathered wall anomalies and cast-iron / tendon-tearing / wet-squelch audio architecture match Android 1:1', () => {
  const arc1UiSrc = readDocsFile('src/ui/arc1ChamberUi.js');
  const css = readDocsFile('styles.css');
  const audioFacadeSrc = readDocsFile('src/audio.js');
  const dspSrc = readDocsFile('src/audio/dspPrimitives.js');
  const arc1AudioSrc = readDocsFile('src/audio/arc1CueBank.js');

  // Photographic circular feathered wall anomalies replace flat icon canvases on Arc 1 wall
  assert.ok(arc1UiSrc.includes("photo.className = 'anomaly-photo'"));
  assert.ok(arc1UiSrc.includes('anomalyAssetUrl(anomaly.type)'));
  assert.ok(css.includes('.anomaly-photo'));
  assert.ok(css.includes('mask-image: radial-gradient('));
  assert.ok(css.includes('border: none;') && css.includes('border-radius: 50%;'));

  // Cast-iron inharmonic modal ratios + organic fibrous tendon-tearing + wet mucosa squelch + slapback room
  assert.deepEqual([...IRON_MODE_RATIOS], [1.0, 1.53, 2.24, 2.89, 3.71, 4.68]);
  for (const token of [
    'createCrackleNoiseBuffer',
    'triggerIronModalRing',
    'triggerTendonTear',
    'triggerWetSquelch',
    'triggerBubbleCluster',
    'clangScale = 1.0',
  ]) {
    assert.ok(dspSrc.includes(token), `docs/src/audio/dspPrimitives.js must implement ${token}`);
  }
  for (const token of ['slapDelay1', 'slapDelay2']) {
    assert.ok(audioFacadeSrc.includes(token), `docs/src/audio.js must implement ${token}`);
  }
  for (const token of [
    '108 * darkPitch, 36 * darkPitch',
    '152 * darkPitch, 62 * darkPitch',
    '154 * darkPitch',
  ]) {
    assert.ok(arc1AudioSrc.includes(token), `docs/src/audio/arc1CueBank.js must implement ${token}`);
  }
});

test('Web twin chamber specimen jar shelves and macro JARS tab portraits match Android 1:1', () => {
  assert.deepEqual([...FOUNDATIONAL_SHELF_JARS], ['yesterdays_dust', 'unspent_breath', 'almost_a_name']);
  assert.deepEqual([...DEEP_SHELF_JARS], ['salt_from_the_grout', 'a_borrowed_pulse', 'the_uncounted_hour']);

  const empty = partitionChamberShelfJars([]);
  assert.deepEqual(empty, { left: [], right: [] });

  const mixed = partitionChamberShelfJars([
    'the_uncounted_hour',
    'almost_a_name',
    'yesterdays_dust',
    'a_borrowed_pulse',
  ]);
  assert.deepEqual(mixed, {
    left: ['yesterdays_dust', 'almost_a_name'],
    right: ['a_borrowed_pulse', 'the_uncounted_hour'],
  });

  // After Excision with CATATONIC_TIDE, left foundational jars persist while right deep shelf clears
  const preExcision = {
    ...initialState(realCatalog),
    cycle: 2,
    highestExcisionMilestone: 2,
    ichor: 300_000,
    cycleLifetimeIchor: 300_000,
    unlockedScarNodes: ['first_incision', 'catatonic_tide'],
    ownedJars: ['yesterdays_dust', 'unspent_breath', 'a_borrowed_pulse', 'the_uncounted_hour'],
  };
  const postExcision = performExcision(preExcision, realCatalog);
  assert.deepEqual(partitionChamberShelfJars(postExcision.ownedJars), {
    left: ['yesterdays_dust', 'unspent_breath'],
    right: [],
  });

  const html = readDocsFile('index.html');
  const css = readDocsFile('styles.css');
  const arc1UiSrc = readDocsFile('src/ui/arc1ChamberUi.js');

  assert.ok(html.includes('id="chamber-jar-shelves"'));
  assert.ok(css.includes('.chamber-jar-shelves') && css.includes('pointer-events: none;'));
  assert.ok(css.includes('.chamber-jar-ledge') && css.includes('.jar-photo-emblem'));
  assert.ok(css.includes('@keyframes jar-borrowed-pulse'));
  assert.ok(arc1UiSrc.includes('jarAssetUrl(jarId)'));
  assert.ok(arc1UiSrc.includes('jarAssetUrl(jar.id)'));
  assert.ok(arc1UiSrc.includes('renderChamberJarShelves(state)'));
});

test('Web story drip FIFO queue never dismisses ongoing drips and matches Android 1:1', () => {
  assert.equal(STORY_DRIP_QUEUE_GAP_MS, 300);
  const queue = createStoryDripQueue();
  const [a, b, c] = realCatalog.entries;

  assert.equal(queue.isBusy, false);
  assert.equal(queue.active, null);

  // First story starts immediately
  const r1 = queue.enqueue(a);
  assert.equal(r1.started?.id, a.id);
  assert.equal(r1.queued, false);
  assert.equal(queue.isBusy, true);

  // Subsequent stories queue behind the active drip without replacing it, deduplicating active & pending IDs
  const r2 = queue.enqueue(b);
  assert.equal(r2.started, null);
  assert.equal(r2.queued, true);
  assert.deepEqual(queue.enqueue(a), { started: null, queued: false });
  assert.deepEqual(queue.enqueue(b), { started: null, queued: false });
  const r3 = queue.enqueue(c);
  assert.equal(r3.started, null);
  assert.equal(r3.queued, true);
  assert.equal(queue.active?.id, a.id);
  assert.deepEqual(
    queue.pending.map((x) => x.id),
    [b.id, c.id],
  );

  // Completing active promotes strictly FIFO
  assert.equal(queue.completeActive()?.id, b.id);
  assert.equal(queue.active?.id, b.id);
  assert.equal(queue.completeActive()?.id, c.id);
  assert.equal(queue.active?.id, c.id);
  assert.equal(queue.completeActive(), null);
  assert.equal(queue.isBusy, false);

  // Clear wipes both active and pending
  queue.enqueue(a);
  queue.enqueue(b);
  queue.clear();
  assert.equal(queue.isBusy, false);
  assert.equal(queue.active, null);
  assert.deepEqual(queue.pending, []);

  // Verify drippingMargin.js never clears storyDripLayer inside triggerStoryDrip/mountStoryDripCard and suppresses events while busy
  const dripSrc = readDocsFile('src/ui/drippingMargin.js');
  const mountFnIdx = dripSrc.indexOf('function mountStoryDripCard');
  const triggerEventIdx = dripSrc.indexOf('function triggerEventDrip');
  const storyDripBlock = dripSrc.slice(mountFnIdx, triggerEventIdx);
  assert.ok(
    !storyDripBlock.includes("storyDripLayer.innerHTML = ''"),
    'Incoming story drips must never wipe/dismiss an ongoing story card',
  );
  assert.ok(storyDripBlock.includes('storyDripQueue.enqueue(entry)'));
  assert.ok(storyDripBlock.includes('STORY_DRIP_QUEUE_GAP_MS'));
  assert.ok(dripSrc.includes('if (isStoryDripBusy()) return false;'));
  assert.ok(dripSrc.includes('storyDripQueue.clear();'));
});

test('Web Arc 2 5-stage photographic desk Slow-Boil quarantine, feathered desk anomalies, and Exhibit Polaroids match Android 1:1', () => {
  // 1. Slow-Boil Quarantine: Cases I–III strictly keep case4 === 0 && case5 === 0 (Lamp On & Lamp Off)
  for (const caseAngle of [1, 2, 3]) {
    for (const lampOn of [true, false]) {
      const layers = computeDeskLayerAlphas(caseAngle, 0.95, lampOn, false);
      assert.equal(layers.case4, 0, `Case ${caseAngle} (lampOn=${lampOn}) must quarantine Case IV`);
      assert.equal(layers.case5, 0, `Case ${caseAngle} (lampOn=${lampOn}) must quarantine Case V`);
    }
  }
  assert.equal(computeDeskLayerAlphas(1, 0, true, false).case1, 0.9);
  assert.equal(computeDeskLayerAlphas(2, 0.3, true, false).case2, 0.88);
  assert.equal(computeDeskLayerAlphas(3, 0.5, false, false).case3, 0.88);

  // Case IV Lamp On keeps case5 === 0; Lamp Off at high haunt subtly surfaces case5
  const c4On = computeDeskLayerAlphas(4, 0.9, true, false);
  assert.equal(c4On.case4, 0.78);
  assert.equal(c4On.case5, 0);
  const c4OffHigh = computeDeskLayerAlphas(4, 0.8, false, false);
  assert.equal(c4OffHigh.case4, 0.92);
  assert.equal(c4OffHigh.case5, 0.24);

  // Case V and File #00 escalate Case V backdrop while clearing early stages
  const c5On = computeDeskLayerAlphas(5, 0.9, true, false);
  assert.equal(c5On.case1, 0);
  assert.equal(c5On.case5, 0.82);
  const fileZero = computeDeskLayerAlphas(5, 1.0, false, true);
  assert.equal(fileZero.case1, 0);
  assert.equal(fileZero.case4, 0.14);
  assert.equal(fileZero.case5, 0.94);

  // 2. Centralized Arc 2 photographic asset URLs
  assert.equal(Object.keys(DESK_STAGE_ASSET_URLS).length, 5);
  assert.equal(Object.keys(DESK_ANOMALY_ASSET_URLS).length, 4);
  assert.equal(Object.keys(EXHIBIT_ASSET_URLS).length, 10);
  for (const url of [
    ...Object.values(DESK_STAGE_ASSET_URLS),
    ...Object.values(DESK_ANOMALY_ASSET_URLS),
    ...Object.values(EXHIBIT_ASSET_URLS),
  ]) {
    assert.ok(url.endsWith(`.jpg?v=${ASSET_VERSION}`), `Arc 2 asset URL ${url} must use ASSET_VERSION`);
  }

  // 3. CSS & UI parity for circular feathered Desk Anomalies, Corkboard Polaroids, and reduced motion
  const css = readDocsFile('styles.css');
  const arc2UiSrc = readDocsFile('src/ui/arc2InvestigatorUi.js');
  const deskCanvasSrc = readDocsFile('src/canvas/arc2DeskCanvas.js');

  assert.ok(css.includes('@keyframes desk-anomaly-breathe'));
  assert.ok(css.includes('@keyframes desk-anomaly-pulse'));
  assert.ok(css.includes('.anomaly-node--desk'));
  assert.ok(css.includes('.exhibit-polaroid-frame'));
  assert.ok(css.includes('.exhibit-pin-head'));
  assert.ok(css.includes('.exhibit-dossier-frame'));

  const reducedMotionBlock = css.slice(css.indexOf('@media (prefers-reduced-motion: reduce)'));
  assert.ok(
    reducedMotionBlock.includes('.anomaly-node--desk') &&
      reducedMotionBlock.includes('animation: none;'),
    'Reduced-motion media query must disable Arc 2 desk anomaly animation',
  );

  assert.ok(arc2UiSrc.includes('anomaly-node anomaly-node--desk anomaly-node--${anomaly.type}'));
  assert.ok(arc2UiSrc.includes("img.className = 'anomaly-photo'"));
  assert.ok(arc2UiSrc.includes('deskAnomalyAssetUrl(anomaly.type)'));
  assert.ok(arc2UiSrc.includes('exhibitAssetUrl(ex.id)'));
  assert.ok(arc2UiSrc.includes('exhibit-polaroid-frame'));
  assert.ok(arc2UiSrc.includes('exhibit-dossier-frame'));
  assert.ok(arc2UiSrc.includes('contactAssetUrl(c.id)'));
  assert.ok(arc2UiSrc.includes('contact-portrait-frame'));
  assert.ok(deskCanvasSrc.includes('computeDeskLayerAlphas(') && deskCanvasSrc.includes('drawCoverImage('));

  // 4. Photographic wiring for remaining specimens, grafts, relics, Arc 2 stamp, and Arc 3 convergence backdrop
  const arc1UiSrc = readDocsFile('src/ui/arc1ChamberUi.js');
  const appSrc = readDocsFile('src/app.js');
  assert.ok(arc1UiSrc.includes('specimenAssetUrl('));
  assert.ok(arc1UiSrc.includes('graftAssetUrl('));
  assert.ok(arc1UiSrc.includes('relicAssetUrl('));
  assert.ok(arc1UiSrc.includes('onInspectRelic'));
  assert.ok(appSrc.includes('SOUND_CUES.RELIC_INSPECT'));
  for (const selector of [
    '.graft-photo-frame',
    '.relic-photo-frame',
    '.contact-portrait-frame',
    '.desk-stamp-photo',
    '.arc3-backdrop-photo',
  ]) {
    assert.ok(css.includes(selector), `styles.css must style ${selector}`);
  }
});

test('Arc 2 File #00 seam banner strictly honors [hidden] CSS and Arc 3 exposes canvas wall actions & active-first sorting', () => {
  const css = readDocsFile('styles.css');
  const html = readDocsFile('index.html');
  const arc3UiSrc = readDocsFile('src/ui/arc3BreachUi.js');
  const appSrc = readDocsFile('src/app.js');

  // 1. Arc 2 red seam banner must never override [hidden] with display: flex
  assert.ok(css.includes('[hidden] {\n  display: none !important;\n}'));
  assert.ok(css.includes('.file-zero-card[hidden]'));
  assert.ok(html.includes('id="file-zero-reveal" class="file-zero-card" hidden'));

  // 2. Arc 3 wall-style canvas actions (interactive shelf jars & on-canvas Drive Wedge)
  assert.ok(html.includes('id="arc3-canvas-drive-wedge"'));
  assert.ok(appSrc.includes("document.getElementById('arc3-canvas-drive-wedge')"));
  assert.ok(css.includes('.arc3-canvas-wedge-btn'));
  assert.ok(css.includes('.arc3-jar-slot.is-actionable'));
  assert.ok(css.includes('.status-tag--surge'));
  assert.ok(arc3UiSrc.includes('arc3ShatterActForJar('));
  assert.ok(arc3UiSrc.includes('arc3IsClotBursting('));
  assert.ok(arc3UiSrc.includes('ALREADY UNMADE'));

  // 3. Arc 3 submerged aftermath ends in crimson finality (no return buttons to Chamber or Desk)
  assert.ok(html.includes('id="arc3-submerged-archive-btn"'));
  assert.ok(!html.includes('arc3-back-chamber-btn'));
  assert.ok(!html.includes('arc3-back-desk-btn'));
  assert.ok(!appSrc.includes('arc3BackChamberBtn'));
  assert.ok(!appSrc.includes('arc3BackDeskBtn'));
});
