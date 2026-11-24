/**
 * Flicker-free DOM renderer for the GitHub Pages multi-arc web version.
 * Supports Arc 1 (Chamber + 6 Excisions + Surgical Scar Tree + Chamber Rites),
 * Arc 2 (Haunted Investigator Desk + Red-String Chapters + File #00),
 * Arc 3 (Open Convergence Threshold), and the shared Grout/Case Ledger.
 */

import {
  CASE_DEDUCTIONS,
  CREATURE_LIST,
  CREATURE_TYPES,
  CULT_DOSSIER_LINES,
  DESK_ANOMALY_TYPES,
  DREDGED_RELICS,
  EXCISION_MILESTONES,
  GAME_ARCS,
  INVESTIGATION_CHAPTERS,
  INVESTIGATOR_CONTACTS,
  ORGAN_BALANCE,
  REPEATABLE_SCAR_GRAFTS,
  SCAR_NODES,
  SPECIMEN_JAR_LIST,
  WALL_ANOMALY_TYPES,
  arc2CanConnectNextChapter,
  arc2ContactCost,
  arc2ContactCount,
  arc2EffectiveContactRate,
  arc2InspectYield,
  arc2LeadsPerSecond,
  arc2NextChapter,
  bloomMultiplier,
  canAffordChoirLung,
  canAffordGraft,
  canBreachToArc2,
  canBuyRepeatableGraft,
  canClearNextMilestone,
  canDredgeSump,
  canExcise,
  canOfferToThroat,
  canUnlockNode,
  choirLungCooldownDurationSec,
  choirLungStillnessCost,
  creatureCost,
  creatureCount,
  cycleLifetimeIchorDisplay,
  dossierIchorMultiplier,
  effectiveCreatureIchorPerSec,
  effectiveJarCost,
  effectiveLampStillnessPerSec,
  formatCompactDecimal,
  formatInteger,
  formatMultiplier,
  graftCooldownDurationSec,
  graftStillnessCost,
  hasExcessMilestoneMass,
  hasNode,
  ichorDisplay,
  ichorPerSecond,
  isApparatusUnlocked,
  isChamberRitesUnlocked,
  isChoirLungOnCooldown,
  isCreatureUnlocked,
  isGraftOnCooldown,
  isGraftUnlocked,
  isJarUnlocked,
  isJarsAndGraftUnlocked,
  isRepeatableGraftsUnlocked,
  isScarTreeUnlocked,
  isValveClamped,
  manualTapYield,
  nextBaseScarMassThreshold,
  nextDredgeableRelic,
  nextExcisionMilestone,
  nextScarProgress,
  pendingScarTissue,
  relicIchorMultiplier,
  repeatableCost,
  repeatableRank,
  scarMultiplier,
  scarNodeById,
  scarStillnessMultiplier,
  stillnessDisplay,
  stillnessIdleDelaySec,
  totalCreatureIchorPerSec,
  valveClampDurationSec,
} from './engine.js?v=2.9';
import {
  drawActionArtwork,
  drawAnomalyGlyph,
  drawDeskAnomalyGlyph,
  drawSumpSiphonPortrait,
  drawWeepingLampPortrait,
} from './chamberCanvas.js?v=2.9';

function randomBetween(min, max, rng) {
  const raw = typeof rng === 'function' ? Number(rng()) : Math.random();
  const unit = Number.isFinite(raw) ? Math.min(1, Math.max(0, raw)) : 0.5;
  return min + (max - min) * unit;
}

/**
 * Computes randomized, non-uniform seepage timing and subtle multi-layer parallax parameters
 * for a `.drip-card`. Story drips remain prompt and slightly foregrounded; event drips sit
 * on a slightly deeper parallax plane with wider hesitation jitter.
 */
export function computeDripMotionProfile(kind = 'story', rng = Math.random) {
  const isEvent = kind === 'event';
  const delayMs = Math.round(
    isEvent ? randomBetween(90, 540, rng) : randomBetween(40, 280, rng),
  );
  const durationSec = Number(
    (isEvent ? randomBetween(4.45, 5.55, rng) : randomBetween(6.45, 7.35, rng)).toFixed(2),
  );
  const driftX = Number(
    (isEvent ? randomBetween(-7.5, 7.5, rng) : randomBetween(-5.5, 5.5, rng)).toFixed(2),
  );
  const driftMidX = Number(
    (driftX * randomBetween(-0.55, 0.75, rng) + randomBetween(-2.2, 2.2, rng)).toFixed(2),
  );
  const parallaxZ = Number(
    (isEvent ? randomBetween(-28, -4, rng) : randomBetween(-12, 14, rng)).toFixed(2),
  );
  const scale = Number(
    (isEvent ? randomBetween(0.955, 0.995, rng) : randomBetween(0.985, 1.02, rng)).toFixed(3),
  );
  const tiltDeg = Number(
    (isEvent ? randomBetween(-0.7, 0.7, rng) : randomBetween(-0.45, 0.45, rng)).toFixed(2),
  );
  const ribbonLagPx = Number(
    (isEvent ? randomBetween(12, 24, rng) : randomBetween(10, 20, rng)).toFixed(1),
  );
  const ribbonDelayMs = Math.round(
    delayMs + (isEvent ? randomBetween(50, 240, rng) : randomBetween(40, 180, rng)),
  );
  const beadDelayMs = Math.round(
    delayMs + (isEvent ? randomBetween(110, 420, rng) : randomBetween(90, 320, rng)),
  );
  const beadDurationSec = Number(
    (isEvent ? randomBetween(1.45, 2.35, rng) : randomBetween(1.75, 2.65, rng)).toFixed(2),
  );
  const bubbleParallaxX = Number(
    (
      -driftX * randomBetween(0.28, 0.58, rng) +
      randomBetween(isEvent ? -1.4 : -1.0, isEvent ? 1.4 : 1.0, rng)
    ).toFixed(2),
  );
  const bubbleParallaxY = Number(
    (isEvent ? randomBetween(-10, 8, rng) : randomBetween(-8, 6, rng)).toFixed(2),
  );
  const bubbleDelayMs = Math.round(
    delayMs + (isEvent ? randomBetween(30, 160, rng) : randomBetween(20, 120, rng)),
  );

  return {
    kind: isEvent ? 'event' : 'story',
    delayMs,
    durationSec,
    driftX,
    driftMidX,
    parallaxZ,
    scale,
    tiltDeg,
    ribbonLagPx,
    ribbonDelayMs,
    beadDelayMs,
    beadDurationSec,
    bubbleParallaxX,
    bubbleParallaxY,
    bubbleDelayMs,
  };
}

export function applyDripMotionProfile(card, profile) {
  if (!card?.style || !profile) return;
  card.style.setProperty('--drip-delay', `${profile.delayMs}ms`);
  card.style.setProperty('--drip-duration', `${profile.durationSec}s`);
  card.style.setProperty('--drip-drift-x', `${profile.driftX}px`);
  card.style.setProperty('--drip-drift-mid-x', `${profile.driftMidX}px`);
  card.style.setProperty('--drip-parallax-z', `${profile.parallaxZ}px`);
  card.style.setProperty('--drip-scale', String(profile.scale));
  card.style.setProperty('--drip-tilt', `${profile.tiltDeg}deg`);
  card.style.setProperty('--drip-ribbon-lag', `${profile.ribbonLagPx}px`);
  card.style.setProperty('--drip-ribbon-delay', `${profile.ribbonDelayMs}ms`);
  card.style.setProperty('--drip-bead-delay', `${profile.beadDelayMs}ms`);
  card.style.setProperty('--drip-bead-duration', `${profile.beadDurationSec}s`);
  card.style.setProperty('--drip-bubble-parallax-x', `${profile.bubbleParallaxX}px`);
  card.style.setProperty('--drip-bubble-parallax-y', `${profile.bubbleParallaxY}px`);
  card.style.setProperty('--drip-bubble-delay', `${profile.bubbleDelayMs}ms`);
}

export function createUiController(handlers) {
  const els = {
    body: document.body,
    landingTitle: document.getElementById('landing-title'),
    arc1Shell: document.getElementById('arc1-shell'),
    arc2Shell: document.getElementById('arc2-shell'),
    arc3Shell: document.getElementById('arc3-shell'),

    // Arc 1 (Decluttered Top Telemetry)
    phaseHeader: document.getElementById('phase-header'),
    phaseBadge: document.getElementById('phase-badge'),
    phaseProgressFill: document.getElementById('phase-progress-fill'),
    thresholdStatus: document.getElementById('threshold-status'),

    ichorRate: document.getElementById('ichor-rate'),
    counterGlassPlate: document.getElementById('counter-glass-plate'),
    counterValue: document.getElementById('counter-value'),
    stillnessText: document.getElementById('stillness-text'),
    stillnessActiveBadge: document.getElementById('stillness-active-badge'),
    gazeActiveBadge: document.getElementById('gaze-active-badge'),
    clampActiveBadge: document.getElementById('clamp-active-badge'),
    bloomActiveBadge: document.getElementById('bloom-active-badge'),
    valveButton: document.getElementById('valve-button'),
    valveYieldText: document.getElementById('valve-yield-text'),
    valveSubLabel: document.getElementById('valve-sub-label'),
    residueLayer: document.getElementById('residue-layer'),
    anomalyLayer: document.getElementById('anomaly-layer'),

    deckPanel: document.getElementById('apparatus-deck'),
    deckTabs: document.getElementById('deck-tabs'),
    deckContent: document.getElementById('deck-content'),

    // Arc 2
    arc2Header: document.getElementById('arc2-header'),
    arc2ChapterBadge: document.getElementById('arc2-chapter-badge'),
    arc2ComplicityLabel: document.getElementById('arc2-complicity-label'),
    arc2ComplicityFill: document.getElementById('arc2-complicity-fill'),
    arc2ChapterStatus: document.getElementById('arc2-chapter-status'),
    arc2LeadsRate: document.getElementById('arc2-leads-rate'),
    arc2LeadsValue: document.getElementById('arc2-leads-value'),
    arc2SilenceText: document.getElementById('arc2-silence-text'),
    arc2SilenceBadge: document.getElementById('arc2-silence-badge'),
    arc2FocusBadge: document.getElementById('arc2-focus-badge'),
    inspectYieldText: document.getElementById('inspect-yield-text'),
    arc2ResidueLayer: document.getElementById('arc2-residue-layer'),
    deskAnomalyLayer: document.getElementById('desk-anomaly-layer'),
    fileZeroReveal: document.getElementById('file-zero-reveal'),
    arc2DeckTabs: document.getElementById('arc2-deck-tabs'),
    arc2DeckContent: document.getElementById('arc2-deck-content'),

    // Arc 3
    arc3Stats: document.getElementById('arc3-stats'),

    // Shared Ledger, Archive & Dual-Lane Dripping Margin Overlays
    ledgerPanel: document.getElementById('ledger-panel'),
    ledgerSpeaker: document.getElementById('ledger-speaker'),
    ledgerDivider: document.getElementById('ledger-divider'),
    ledgerBody: document.getElementById('ledger-body'),
    openArchiveBtn: document.getElementById('open-archive-btn'),
    archiveModal: document.getElementById('archive-modal'),
    archiveList: document.getElementById('archive-list'),
    storyDripLayer: document.getElementById('story-drip-layer'),
    eventDripLayer: document.getElementById('event-drip-layer'),
  };

  let selectedTab = 'apparatus';
  let selectedArc2Tab = 'contacts';
  let selectedScarNodeId = 'first_incision';
  let selectedRelicId = null;
  let excisionArmed = false;
  let lastDeckSignature = '';
  let lastArc2DeckSignature = '';
  let lastCounterDisplay = -1;
  let lastDialogueId = null;
  let hasHydratedDialogue = false;
  let residueSeq = 0;
  let ledgerPulseTimer = null;

  const anomalyCanvasMap = new Map();
  const deskAnomalyCanvasMap = new Map();
  let lampPortraitCanvas = null;
  let siphonPortraitCanvas = null;
  let lastLampWeeping = null;
  let lastSiphonActive = null;

  // Cached element references for active Arc 1 & Arc 2 deck panels (avoids 10Hz DOM queries)
  let organismCardRefs = [];
  let jarsPanelRefs = null;
  let scarPanelRefs = null;
  let ritesPanelRefs = null;
  let excisionRefs = null;
  let arc2DeckRefs = null;

  function resolveAvailableTabs(state) {
    const tabs = ['apparatus'];
    if (isJarsAndGraftUnlocked(state)) tabs.push('jars_and_graft');
    if (isScarTreeUnlocked(state)) tabs.push('scar_tree');
    if (isChamberRitesUnlocked(state)) tabs.push('chamber_rites');
    if (canExcise(state) || canBreachToArc2(state) || state.cycle > 0 || state.phase >= 4) {
      tabs.push('excision');
    }
    return tabs;
  }

  function spawnResidueIn(container, yieldLabel, isRawLabel = false) {
    if (!container) return;
    const node = document.createElement('span');
    node.className = 'residue-float';
    const drift = ((residueSeq++ % 5) - 2) * 14;
    node.style.setProperty('--drift-x', `${drift}px`);
    node.textContent = isRawLabel ? yieldLabel : `+${yieldLabel}`;
    container.appendChild(node);
    node.addEventListener('animationend', () => node.remove(), { once: true });
    if (container.childElementCount > 14) {
      container.firstElementChild?.remove();
    }
  }

  // Subtle passive scroll & pointer parallax separating Story (near plane) from Event (deeper plane)
  let parallaxRaf = 0;
  let parallaxPointerX = 0;
  let parallaxPointerY = 0;
  function scheduleLaneParallax() {
    if (parallaxRaf) return;
    parallaxRaf = window.requestAnimationFrame(() => {
      parallaxRaf = 0;
      if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) return;
      const scrollRatio = Math.max(-1, Math.min(1, (window.scrollY || 0) / 360 - 0.35));
      const storyX = parallaxPointerX * 5.0;
      const storyY = parallaxPointerY * 4.0 - scrollRatio * 9.0;
      const eventX = parallaxPointerX * 2.6;
      const eventY = parallaxPointerY * 2.2 - scrollRatio * 5.0;
      els.storyDripLayer?.style.setProperty('--lane-parallax-x', `${storyX.toFixed(2)}px`);
      els.storyDripLayer?.style.setProperty('--lane-parallax-y', `${storyY.toFixed(2)}px`);
      els.eventDripLayer?.style.setProperty('--lane-parallax-x', `${eventX.toFixed(2)}px`);
      els.eventDripLayer?.style.setProperty('--lane-parallax-y', `${eventY.toFixed(2)}px`);
    });
  }
  if (typeof window !== 'undefined' && (els.storyDripLayer || els.eventDripLayer)) {
    window.addEventListener('scroll', scheduleLaneParallax, { passive: true });
    window.addEventListener(
      'pointermove',
      (event) => {
        const vw = Math.max(1, window.innerWidth || 1);
        const vh = Math.max(1, window.innerHeight || 1);
        parallaxPointerX = (event.clientX / vw - 0.5) * 2;
        parallaxPointerY = (event.clientY / vh - 0.5) * 2;
        scheduleLaneParallax();
      },
      { passive: true },
    );
  }

  function triggerStoryDrip(entry) {
    if (!els.storyDripLayer || !entry) return;
    // Story lane is strictly dedicated to newly unlocked DialogueEntry fragments
    els.storyDripLayer.innerHTML = '';
    const card = document.createElement('div');
    card.className = 'drip-card drip-card--story';
    applyDripMotionProfile(card, computeDripMotionProfile('story'));
    const ribbon = document.createElement('div');
    ribbon.className = 'drip-ribbon';
    const bubble = document.createElement('div');
    bubble.className = 'drip-bubble';
    const speaker = document.createElement('span');
    speaker.className = 'drip-speaker';
    speaker.textContent = `// ${(entry.speaker || 'CHAMBER').toUpperCase()}`;
    const body = document.createElement('p');
    body.className = 'drip-text';
    body.textContent = (entry.lines || []).join('\n');
    bubble.append(speaker, body);
    card.append(ribbon, bubble);
    els.storyDripLayer.appendChild(card);

    els.eventDripLayer?.classList.add('is-dimmed');
    const handleAnimationEnd = (event) => {
      // Ignore bubbled child animationend events from .drip-ribbon / .drip-bubble
      if (event.target !== card) return;
      card.removeEventListener('animationend', handleAnimationEnd);
      card.remove();
      if (els.storyDripLayer.childElementCount === 0) {
        els.eventDripLayer?.classList.remove('is-dimmed');
      }
    };
    card.addEventListener('animationend', handleAnimationEnd);
  }

  function triggerEventDrip(text, speakerLabel = 'CHAMBER') {
    if (!els.eventDripLayer || !text) return;
    // Subordinate left lane — never touches or interferes with the primary Story lane
    els.eventDripLayer.innerHTML = '';
    const card = document.createElement('div');
    card.className = 'drip-card drip-card--event';
    applyDripMotionProfile(card, computeDripMotionProfile('event'));
    const ribbon = document.createElement('div');
    ribbon.className = 'drip-ribbon';
    const bubble = document.createElement('div');
    bubble.className = 'drip-bubble';
    const speaker = document.createElement('span');
    speaker.className = 'drip-speaker';
    speaker.textContent = `// ${speakerLabel.toUpperCase()}`;
    const body = document.createElement('p');
    body.className = 'drip-text';
    body.textContent = text;
    bubble.append(speaker, body);
    card.append(ribbon, bubble);
    els.eventDripLayer.appendChild(card);
    const handleAnimationEnd = (event) => {
      if (event.target !== card) return;
      card.removeEventListener('animationend', handleAnimationEnd);
      card.remove();
    };
    card.addEventListener('animationend', handleAnimationEnd);
  }

  /* ==================== ARC 1 RENDERING ==================== */

  function renderTelemetry(state) {
    els.body.dataset.phase = String(state.phase);
    const phaseTitle = (state.phaseName || '').toUpperCase();
    els.phaseHeader.textContent = `PHASE ${String(state.phase).padStart(2, '0')} // ${phaseTitle}`;
    els.phaseBadge.textContent = `P${state.phase}`;

    const pct = Math.max(0, Math.min(100, state.phaseProgress * 100));
    els.phaseProgressFill.style.width = `${pct.toFixed(2)}%`;
    els.phaseProgressFill.classList.toggle('progress-fill--deep', state.phase >= 2);

    const nextM = nextExcisionMilestone(state);
    const ready = canClearNextMilestone(state) || canBreachToArc2(state) || canExcise(state);
    els.thresholdStatus.classList.toggle('threshold-status--ready', ready);
    if (canBreachToArc2(state)) {
      els.thresholdStatus.textContent = 'SEAM UNSEALED // PLEAD FOR SALVATION';
    } else if (canClearNextMilestone(state) && nextM) {
      els.thresholdStatus.textContent = `${nextM.code} READY`;
    } else if (state.nextPhaseThreshold != null) {
      els.thresholdStatus.textContent = `NEXT FRACTURE: ${formatInteger(cycleLifetimeIchorDisplay(state))} / ${formatInteger(state.nextPhaseThreshold)}`;
    } else if (nextM) {
      els.thresholdStatus.textContent = `NEXT MILESTONE (${nextM.code}): ${formatInteger(cycleLifetimeIchorDisplay(state))} / ${formatInteger(nextM.massThreshold)}`;
    } else {
      els.thresholdStatus.textContent = 'NO SUMMIT REMAINS';
    }
  }

  function renderChamberHud(state) {
    const countVal = ichorDisplay(state);
    const formatted = formatInteger(countVal);
    els.counterValue.textContent = formatted;
    els.counterValue.setAttribute('aria-label', `Current ichor count: ${formatted}`);
    els.counterValue.classList.toggle('counter-value--medium', formatted.length > 6 && formatted.length <= 9);
    els.counterValue.classList.toggle('counter-value--compact', formatted.length > 9);

    if (countVal !== lastCounterDisplay && countVal > 0) {
      els.counterValue.classList.add('counter-value--recoil');
      window.setTimeout(() => els.counterValue.classList.remove('counter-value--recoil'), 95);
    }
    lastCounterDisplay = countVal;

    const rate = ichorPerSecond(state);
    els.ichorRate.hidden = rate <= 0;
    if (rate > 0) {
      els.ichorRate.textContent = `${formatCompactDecimal(rate)} /s`;
    }

    const leechCount = creatureCount(state, CREATURE_TYPES.LEECH_CLERK.id);
    els.counterGlassPlate.classList.toggle('counter-glass-plate--grazed', leechCount > 0);

    const showStillness = isJarsAndGraftUnlocked(state) || stillnessDisplay(state) > 0;
    els.stillnessText.hidden = !showStillness;
    if (showStillness) {
      els.stillnessText.textContent = `STILLNESS: ${formatInteger(stillnessDisplay(state))}`;
      els.stillnessText.classList.toggle('status-tag--stillness-active', state.isStillnessActive);
    }

    els.stillnessActiveBadge.hidden = !state.isStillnessActive;
    els.gazeActiveBadge.hidden = state.gazeRemainingSeconds <= 0;
    if (state.gazeRemainingSeconds > 0) {
      els.gazeActiveBadge.textContent = `EYE GAZE (${Math.ceil(state.gazeRemainingSeconds)}s)`;
    }

    const clamped = isValveClamped(state);
    els.clampActiveBadge.hidden = !clamped;
    if (clamped) {
      els.clampActiveBadge.textContent = `VALVE CLAMPED (${Math.ceil(state.clampRemainingSeconds)}s)`;
    }

    els.bloomActiveBadge.hidden = state.marrowBloomRemainingSeconds <= 0;
    if (state.marrowBloomRemainingSeconds > 0) {
      els.bloomActiveBadge.textContent = `MARROW BLOOM (${Math.ceil(state.marrowBloomRemainingSeconds)}s)`;
    }

    els.valveButton.disabled = clamped;
    els.valveButton.classList.toggle('valve-button--clamped', clamped);
    els.valveYieldText.textContent = clamped ? 'CLAMP' : `+${formatCompactDecimal(manualTapYield(state))}`;
    els.valveSubLabel.textContent = clamped ? `${Math.ceil(state.clampRemainingSeconds)}s HOLD` : 'VALVE';
  }

  function syncAnomalies(state) {
    const activeIds = new Set(state.activeAnomalies.map((a) => String(a.id)));
    for (const child of Array.from(els.anomalyLayer.children)) {
      const id = child.dataset.anomalyId;
      if (!activeIds.has(id)) {
        anomalyCanvasMap.delete(id);
        child.remove();
      }
    }

    for (const anomaly of state.activeAnomalies) {
      const key = String(anomaly.id);
      if (anomalyCanvasMap.has(key)) continue;
      const meta = Object.values(WALL_ANOMALY_TYPES).find((t) => t.id === anomaly.type);
      if (!meta) continue;

      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = `anomaly-node anomaly-node--${anomaly.type}`;
      btn.dataset.anomalyId = key;
      btn.style.left = `${(anomaly.xFraction * 100).toFixed(1)}%`;
      btn.style.top = `${(anomaly.yFraction * 100).toFixed(1)}%`;
      btn.setAttribute('aria-label', meta.ariaLabel);

      const canvas = document.createElement('canvas');
      canvas.className = 'anomaly-glyph';
      canvas.width = 60;
      canvas.height = 60;

      const label = document.createElement('span');
      label.className = 'anomaly-label';
      label.textContent = meta.label;

      btn.append(canvas, label);
      btn.addEventListener('click', () => handlers.onInteractAnomaly(anomaly.id));
      els.anomalyLayer.appendChild(btn);
      anomalyCanvasMap.set(key, { canvas, type: anomaly.type });
    }
  }

  function renderDeckTabs(availableTabs, activeTab) {
    const tabLabels = {
      apparatus: 'ORGANISMS',
      jars_and_graft: 'JARS & GRAFT',
      scar_tree: 'SCARS',
      chamber_rites: 'RITES',
      excision: 'EXCISION',
    };
    els.deckTabs.innerHTML = '';
    for (const tab of availableTabs) {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.role = 'tab';
      btn.className = 'deck-tab';
      if (tab === 'excision') btn.classList.add('deck-tab--excision');
      if (tab === activeTab) btn.classList.add('is-active');
      btn.setAttribute('aria-selected', String(tab === activeTab));
      btn.textContent = tabLabels[tab];
      btn.addEventListener('click', () => {
        selectedTab = tab;
        if (tab !== 'excision') excisionArmed = false;
        lastDeckSignature = '';
        handlers.onRequestRender();
      });
      els.deckTabs.appendChild(btn);
    }
  }

  function buildOrganismCards(visibleCreatures) {
    els.deckContent.innerHTML = '';
    lampPortraitCanvas = null;
    siphonPortraitCanvas = null;
    lastLampWeeping = null;
    lastSiphonActive = null;
    organismCardRefs = [];

    for (const spec of visibleCreatures) {
      const card = document.createElement('article');
      card.className = 'organism-card';
      card.dataset.creatureId = spec.id;

      const header = document.createElement('div');
      header.className = 'organism-header';
      const titleGroup = document.createElement('div');
      titleGroup.className = 'organism-title-group';
      const code = document.createElement('span');
      code.className = 'organism-code';
      code.textContent = spec.code;
      const name = document.createElement('span');
      name.className = 'organism-name';
      name.dataset.role = 'name';
      titleGroup.append(code, name);

      const status = document.createElement('span');
      status.className = 'organism-status-badge';
      status.dataset.role = 'status';
      header.append(titleGroup, status);

      const body = document.createElement('div');
      body.className = 'organism-body';

      const viewport = document.createElement('div');
      viewport.className = 'specimen-viewport';
      viewport.dataset.role = 'viewport';
      if (spec.portrait) {
        const img = document.createElement('img');
        img.src = spec.portrait;
        img.alt = spec.portraitAlt;
        viewport.appendChild(img);
      } else {
        const canvas = document.createElement('canvas');
        canvas.setAttribute('role', 'img');
        canvas.setAttribute('aria-label', spec.portraitAlt);
        viewport.appendChild(canvas);
        if (spec.id === CREATURE_TYPES.WEEPING_LAMP.id) lampPortraitCanvas = canvas;
        if (spec.id === CREATURE_TYPES.SUMP_SIPHON.id) siphonPortraitCanvas = canvas;
      }

      const info = document.createElement('div');
      info.className = 'organism-info';
      const desc = document.createElement('p');
      desc.className = 'organism-desc';
      desc.textContent = spec.desc;
      const yieldEl = document.createElement('span');
      yieldEl.className = 'organism-yield';
      yieldEl.dataset.role = 'yield';
      info.append(desc, yieldEl);

      let stillEl = null;
      if (spec.id === CREATURE_TYPES.WEEPING_LAMP.id) {
        stillEl = document.createElement('span');
        stillEl.className = 'organism-stillness';
        stillEl.dataset.role = 'stillness';
        info.appendChild(stillEl);
      }

      const buyBtn = document.createElement('button');
      buyBtn.type = 'button';
      buyBtn.className = 'cultivate-btn';
      buyBtn.dataset.role = 'buy';
      const kicker = document.createElement('span');
      kicker.className = 'cultivate-kicker';
      kicker.textContent = 'CULTIVATE';
      const costEl = document.createElement('span');
      costEl.className = 'cultivate-cost';
      costEl.dataset.role = 'cost';
      buyBtn.append(kicker, costEl);
      buyBtn.addEventListener('click', () => handlers.onBuyCreature(spec.id));

      body.append(viewport, info, buyBtn);

      const affordBar = document.createElement('div');
      affordBar.className = 'afford-bar';
      const affordFill = document.createElement('div');
      affordFill.className = 'afford-bar-fill';
      affordFill.dataset.role = 'afford';
      affordBar.appendChild(affordFill);

      card.append(header, body, affordBar);
      els.deckContent.appendChild(card);

      organismCardRefs.push({
        spec,
        card,
        nameEl: name,
        statusEl: status,
        viewport,
        yieldEl,
        stillEl,
        buyBtn,
        costEl,
        affordFill,
      });
    }
  }

  function updateOrganismCardsInPlace(state) {
    for (const ref of organismCardRefs) {
      const { spec, card, nameEl, statusEl, viewport, yieldEl, stillEl, buyBtn, costEl, affordFill } = ref;
      const id = spec.id;

      const count = creatureCount(state, id);
      const cost = creatureCost(state, id);
      const canAfford = state.ichor >= cost;
      const progress = cost <= 0 ? 1 : Math.max(0, Math.min(1, state.ichor / cost));
      const isWeeping = id === CREATURE_TYPES.WEEPING_LAMP.id && count > 0 && state.isStillnessActive;
      const isWorking = count > 0 && (id !== CREATURE_TYPES.WEEPING_LAMP.id || isWeeping);

      card.classList.toggle('organism-card--weeping', isWeeping);
      card.classList.toggle('organism-card--affordable', canAfford && !isWeeping);
      card.classList.toggle('organism-card--dormant', !canAfford && count === 0);

      nameEl.textContent = `${spec.name} [${count}]`;

      statusEl.textContent =
        count <= 0
          ? 'DORMANT'
          : id === CREATURE_TYPES.SUTURE_HAND.id
            ? 'TURNING IRON'
            : id === CREATURE_TYPES.LEECH_CLERK.id
              ? 'GRAZING GLASS'
              : id === CREATURE_TYPES.SUMP_SIPHON.id
                ? state.drainSealed
                  ? 'PRESSURIZING'
                  : 'DREDGING SUMP'
                : state.isStillnessActive
                  ? 'WEEPING'
                  : 'AWAITING QUIET';
      statusEl.className = 'organism-status-badge';
      if (isWeeping) statusEl.classList.add('organism-status-badge--weeping');
      else if (isWorking) statusEl.classList.add('organism-status-badge--active');

      viewport.classList.toggle('specimen-viewport--dim', !canAfford && count === 0);
      viewport.classList.toggle('specimen-viewport--weeping', isWeeping);

      const eachRate = effectiveCreatureIchorPerSec(state, id);
      const colonyRate = totalCreatureIchorPerSec(state, id);
      yieldEl.textContent =
        count > 0
          ? `EACH: +${formatCompactDecimal(eachRate)}/s · COLONY: +${formatCompactDecimal(colonyRate)}/s`
          : `EACH: +${formatCompactDecimal(eachRate)}/s`;
      yieldEl.classList.toggle('organism-yield--idle', count === 0);

      if (stillEl) {
        const totalStill = effectiveLampStillnessPerSec(state) * Math.max(1, count);
        stillEl.textContent = `IDLE DRIP: +${formatCompactDecimal(totalStill)} STILLNESS/s`;
        stillEl.classList.toggle('organism-stillness--active', isWeeping);
      }

      buyBtn.disabled = !canAfford;
      costEl.textContent = `${formatInteger(cost)} ICHOR`;

      affordFill.style.width = `${(progress * 100).toFixed(1)}%`;
      affordFill.classList.toggle('afford-bar-fill--ready', canAfford);

      if (
        id === CREATURE_TYPES.WEEPING_LAMP.id &&
        lampPortraitCanvas &&
        (lastLampWeeping !== isWeeping || lampPortraitCanvas.width <= 1)
      ) {
        lastLampWeeping = isWeeping;
        drawWeepingLampPortrait(lampPortraitCanvas, isWeeping);
      }
      if (
        id === CREATURE_TYPES.SUMP_SIPHON.id &&
        siphonPortraitCanvas &&
        (lastSiphonActive !== (count > 0) || siphonPortraitCanvas.width <= 1)
      ) {
        lastSiphonActive = count > 0;
        drawSumpSiphonPortrait(siphonPortraitCanvas, count > 0);
      }
    }
  }

  function buildJarsAndGraftPanel(state) {
    els.deckContent.innerHTML = '';
    const jarRows = [];
    for (const jar of SPECIMEN_JAR_LIST) {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'deck-row';
      btn.dataset.jarId = jar.id;
      btn.innerHTML = `
        <canvas class="action-emblem" width="38" height="38" aria-hidden="true"></canvas>
        <span class="deck-row-text">
          <span class="deck-row-title">${jar.name}</span>
          <span class="deck-row-sub">${jar.effect}</span>
        </span>
        <span class="deck-row-pill" data-role="pill"></span>
      `;
      btn.addEventListener('click', () => handlers.onBottleJar(jar.id));
      els.deckContent.appendChild(btn);
      jarRows.push({
        jar,
        btn,
        emblemCanvas: btn.querySelector('canvas'),
        pillEl: btn.querySelector('[data-role="pill"]'),
        lastDrawnActive: null,
      });
    }

    const divider = document.createElement('div');
    divider.className = 'deck-divider';
    els.deckContent.appendChild(divider);

    let graftRef = null;
    if (isGraftUnlocked(state)) {
      const graftBtn = document.createElement('button');
      graftBtn.type = 'button';
      graftBtn.className = 'deck-row deck-row--accent';
      graftBtn.dataset.role = 'graft-btn';
      graftBtn.innerHTML = `
        <canvas class="action-emblem action-emblem--accent" width="38" height="38" aria-hidden="true"></canvas>
        <span class="deck-row-text">
          <span class="deck-row-title" data-role="graft-title"></span>
          <span class="deck-row-sub">Bind 2 Hands &amp; 2 Leeches into one braided hum.</span>
          <span class="cooldown-track" data-role="graft-cd-track" hidden>
            <span class="cooldown-fill" data-role="graft-cd-fill"></span>
          </span>
        </span>
        <span class="deck-row-pill" data-role="graft-pill">GRAFT</span>
      `;
      graftBtn.addEventListener('click', () => handlers.onPerformGraft());
      els.deckContent.appendChild(graftBtn);
      const emblemCanvas = graftBtn.querySelector('canvas');
      drawActionArtwork(emblemCanvas, 'graft', 'tendril', true);
      graftRef = {
        btn: graftBtn,
        titleEl: graftBtn.querySelector('[data-role="graft-title"]'),
        pillEl: graftBtn.querySelector('[data-role="graft-pill"]'),
        cdTrack: graftBtn.querySelector('[data-role="graft-cd-track"]'),
        cdFill: graftBtn.querySelector('[data-role="graft-cd-fill"]'),
      };
    } else {
      const hint = document.createElement('div');
      hint.className = 'locked-hint';
      hint.textContent = 'GRAFTING UNLOCKS AT PHASE 03';
      els.deckContent.appendChild(hint);
    }

    let lungRef = null;
    if (state.highestExcisionMilestone >= 4) {
      const lungBtn = document.createElement('button');
      lungBtn.type = 'button';
      lungBtn.className = 'deck-row deck-row--accent';
      lungBtn.dataset.role = 'choir-lung-btn';
      lungBtn.innerHTML = `
        <canvas class="action-emblem action-emblem--accent" width="38" height="38" aria-hidden="true"></canvas>
        <span class="deck-row-text">
          <span class="deck-row-title" data-role="choir-lung-title"></span>
          <span class="deck-row-sub">Bind 2 Lamps &amp; 1 Tendril; it breathes deepest in quiet.</span>
          <span class="cooldown-track" data-role="lung-cd-track" hidden>
            <span class="cooldown-fill" data-role="lung-cd-fill"></span>
          </span>
        </span>
        <span class="deck-row-pill" data-role="lung-pill">GRAFT</span>
      `;
      lungBtn.addEventListener('click', () => handlers.onPerformChoirLung());
      els.deckContent.appendChild(lungBtn);
      const emblemCanvas = lungBtn.querySelector('canvas');
      drawActionArtwork(emblemCanvas, 'graft', 'choir_lung', true);
      lungRef = {
        btn: lungBtn,
        titleEl: lungBtn.querySelector('[data-role="choir-lung-title"]'),
        pillEl: lungBtn.querySelector('[data-role="lung-pill"]'),
        cdTrack: lungBtn.querySelector('[data-role="lung-cd-track"]'),
        cdFill: lungBtn.querySelector('[data-role="lung-cd-fill"]'),
      };
    }

    jarsPanelRefs = { jarRows, graftRef, lungRef };
  }

  function updateJarsAndGraftInPlace(state) {
    if (!jarsPanelRefs) return;
    for (const row of jarsPanelRefs.jarRows) {
      const { jar, btn, emblemCanvas, pillEl } = row;
      const owned = state.ownedJars.includes(jar.id);
      const unlocked = isJarUnlocked(state, jar);
      const cost = effectiveJarCost(state, jar);
      const canAfford = unlocked && !owned && state.stillness >= cost;
      btn.disabled = !canAfford;
      btn.classList.toggle('deck-row--accent', owned);
      emblemCanvas.classList.toggle('action-emblem--active', owned);
      if (row.lastDrawnActive !== owned || emblemCanvas.width <= 1) {
        row.lastDrawnActive = owned;
        drawActionArtwork(emblemCanvas, 'jar', jar.id, owned);
      }
      if (owned) {
        pillEl.textContent = 'SEALED';
      } else if (!unlocked) {
        pillEl.textContent = jar.requiresCycleOrMilestone
          ? 'AFTER SEVERANCE'
          : `PHASE ${String(jar.unlockPhase).padStart(2, '0')}`;
      } else {
        pillEl.textContent = `BOTTLE · ${formatInteger(cost)} STILL`;
      }
    }

    if (jarsPanelRefs.graftRef) {
      const { btn, titleEl, pillEl, cdTrack, cdFill } = jarsPanelRefs.graftRef;
      const onCd = isGraftOnCooldown(state);
      btn.disabled = !canAffordGraft(state);
      titleEl.textContent = `GRAFTED CHOIR-TENDRIL [${state.graftedTendrils}]`;
      pillEl.textContent = onCd
        ? `SETTLING (${Math.ceil(state.graftCooldownRemainingSeconds)}s)`
        : `GRAFT · ${formatInteger(graftStillnessCost(state))} STILL`;
      cdTrack.hidden = !onCd;
      if (onCd) {
        const dur = Math.max(0.1, graftCooldownDurationSec(state));
        const pct = Math.max(0, Math.min(100, (state.graftCooldownRemainingSeconds / dur) * 100));
        cdFill.style.width = `${pct.toFixed(1)}%`;
      }
    }

    if (jarsPanelRefs.lungRef) {
      const { btn, titleEl, pillEl, cdTrack, cdFill } = jarsPanelRefs.lungRef;
      const onCd = isChoirLungOnCooldown(state);
      btn.disabled = !canAffordChoirLung(state);
      titleEl.textContent = `GRAFTED CHOIR-LUNG [${state.choirLungs}]`;
      pillEl.textContent = onCd
        ? `SETTLING (${Math.ceil(state.choirLungCooldownRemainingSeconds)}s)`
        : `GRAFT · ${formatInteger(choirLungStillnessCost(state))} STILL`;
      cdTrack.hidden = !onCd;
      if (onCd) {
        const dur = Math.max(0.1, choirLungCooldownDurationSec(state));
        const pct = Math.max(
          0,
          Math.min(100, (state.choirLungCooldownRemainingSeconds / dur) * 100),
        );
        cdFill.style.width = `${pct.toFixed(1)}%`;
      }
    }
  }

  function buildScarTreePanel(state) {
    els.deckContent.innerHTML = '';

    const summary = document.createElement('div');
    summary.className = 'scar-summary-bar';
    summary.dataset.role = 'scar-summary';
    els.deckContent.appendChild(summary);

    const grid = document.createElement('div');
    grid.className = 'scar-tree-grid';
    const nodeRefs = [];

    const rootRow = document.createElement('div');
    rootRow.className = 'scar-root-row';
    const rootNode = scarNodeById('first_incision');
    const rootRef = createScarNodeButton(rootNode, true);
    rootRow.appendChild(rootRef.btn);
    nodeRefs.push(rootRef);
    grid.appendChild(rootRow);

    const branchesRow = document.createElement('div');
    branchesRow.className = 'scar-branches-row';
    const branches = [
      { key: 'suture', label: 'SUTURE' },
      { key: 'stillness', label: 'STILLNESS' },
      { key: 'marrow', label: 'MARROW' },
    ];
    for (const branch of branches) {
      const col = document.createElement('div');
      col.className = 'scar-branch-col';
      const title = document.createElement('div');
      title.className = 'scar-branch-title';
      title.textContent = branch.label;
      col.appendChild(title);
      for (const node of SCAR_NODES.filter((n) => n.branch === branch.key)) {
        const ref = createScarNodeButton(node, false);
        col.appendChild(ref.btn);
        nodeRefs.push(ref);
      }
      branchesRow.appendChild(col);
    }
    grid.appendChild(branchesRow);
    els.deckContent.appendChild(grid);

    const detail = document.createElement('div');
    detail.className = 'scar-detail-card';
    detail.innerHTML = `
      <div class="rite-head">
        <span class="rite-title" data-role="scar-detail-name"></span>
        <span class="rite-meta" data-role="scar-detail-cost"></span>
      </div>
      <p class="rite-desc" data-role="scar-detail-desc"></p>
      <button type="button" class="excision-btn excision-btn--arm" data-role="scar-suture-btn">SUTURE NODE</button>
    `;
    const actionBtn = detail.querySelector('[data-role="scar-suture-btn"]');
    actionBtn.addEventListener('click', () => {
      handlers.onUnlockScarNode(selectedScarNodeId);
    });
    els.deckContent.appendChild(detail);

    const repeatSection = document.createElement('div');
    repeatSection.className = 'scar-repeatables-section';
    repeatSection.dataset.role = 'scar-repeatables';
    const repeatTitle = document.createElement('div');
    repeatTitle.className = 'scar-repeatables-title';
    repeatTitle.textContent = 'REPEATABLE SURGICAL GRAFTS';
    const repeatLocked = document.createElement('div');
    repeatLocked.className = 'locked-hint';
    repeatLocked.textContent = 'SUTURE FIRST INCISION TO OPEN REPEATABLE GRAFTS';
    const repeatList = document.createElement('div');
    repeatList.className = 'scar-repeatables-list';

    const repeatableRefs = [];
    for (const graft of REPEATABLE_SCAR_GRAFTS) {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'deck-row scar-repeatable-row';
      btn.dataset.repeatableGraftId = graft.id;
      btn.innerHTML = `
        <span class="deck-row-text">
          <span class="deck-row-title" data-role="rep-title"></span>
          <span class="deck-row-sub">${graft.desc}</span>
        </span>
        <span class="deck-row-pill" data-role="rep-pill"></span>
      `;
      btn.addEventListener('click', () => handlers.onBuyRepeatableScarGraft(graft.id));
      repeatList.appendChild(btn);
      repeatableRefs.push({
        graft,
        btn,
        titleEl: btn.querySelector('[data-role="rep-title"]'),
        pillEl: btn.querySelector('[data-role="rep-pill"]'),
      });
    }
    repeatSection.append(repeatTitle, repeatLocked, repeatList);
    els.deckContent.appendChild(repeatSection);

    scarPanelRefs = {
      summary,
      nodeRefs,
      nameEl: detail.querySelector('[data-role="scar-detail-name"]'),
      costEl: detail.querySelector('[data-role="scar-detail-cost"]'),
      descEl: detail.querySelector('[data-role="scar-detail-desc"]'),
      actionBtn,
      repeatLocked,
      repeatList,
      repeatableRefs,
    };

    updateScarTreeInPlace(state);
  }

  function createScarNodeButton(node, isRoot) {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = isRoot ? 'scar-node-btn scar-node-btn--root' : 'scar-node-btn';
    btn.dataset.scarNodeId = node.id;
    btn.innerHTML = `
      <span>${node.name}</span>
      <span class="scar-node-cost" data-role="node-cost">${node.cost} SCAR</span>
    `;
    btn.addEventListener('click', () => {
      selectedScarNodeId = node.id;
      handlers.onRequestRender();
    });
    return { node, btn, costEl: btn.querySelector('[data-role="node-cost"]') };
  }

  function updateScarTreeInPlace(state) {
    if (!scarPanelRefs) return;
    const totalIchorMult =
      scarMultiplier(state) * relicIchorMultiplier(state) * dossierIchorMultiplier(state);
    scarPanelRefs.summary.textContent = `CYCLE ${String(state.cycle).padStart(2, '0')} · M${state.highestExcisionMilestone}/6 · AVAIL SCAR: ${formatInteger(state.availableScarTissue)} (${formatInteger(state.scarTissue)} TOTAL) · ×${formatMultiplier(totalIchorMult)} ICHOR · ×${formatMultiplier(scarStillnessMultiplier(state))} STILL`;

    for (const { node, btn, costEl } of scarPanelRefs.nodeRefs) {
      const id = node.id;
      const unlocked = hasNode(state, id);
      const affordable = canUnlockNode(state, id);
      btn.classList.toggle('scar-node-btn--unlocked', unlocked);
      btn.classList.toggle('scar-node-btn--affordable', !unlocked && affordable);
      btn.classList.toggle('scar-node-btn--selected', id === selectedScarNodeId);
      costEl.textContent = unlocked ? 'SUTURED' : `${node.cost} SCAR`;
    }

    const active = scarNodeById(selectedScarNodeId) ?? SCAR_NODES[0];
    const unlocked = hasNode(state, active.id);
    const canBuy = canUnlockNode(state, active.id);
    scarPanelRefs.nameEl.textContent = active.name;
    scarPanelRefs.costEl.textContent = unlocked ? 'SUTURED' : `COST: ${active.cost} SCAR`;
    scarPanelRefs.descEl.textContent = active.desc;
    scarPanelRefs.actionBtn.disabled = !canBuy;
    scarPanelRefs.actionBtn.textContent = unlocked
      ? 'ALREADY SUTURED'
      : active.prerequisite && !hasNode(state, active.prerequisite)
        ? `REQUIRES: ${scarNodeById(active.prerequisite)?.name ?? ''}`
        : `SUTURE (${active.cost} SCAR)`;

    const repeatablesOpen = isRepeatableGraftsUnlocked(state);
    scarPanelRefs.repeatLocked.hidden = repeatablesOpen;
    scarPanelRefs.repeatList.hidden = !repeatablesOpen;
    if (repeatablesOpen) {
      for (const { graft, btn, titleEl, pillEl } of scarPanelRefs.repeatableRefs) {
        const rank = repeatableRank(state, graft);
        const cost = repeatableCost(state, graft);
        const affordable = canBuyRepeatableGraft(state, graft);
        btn.disabled = !affordable;
        btn.classList.toggle('deck-row--accent', rank > 0);
        titleEl.textContent = `${graft.name} [RANK ${rank}]`;
        pillEl.textContent = `GRAFT · ${formatInteger(cost)} SCAR`;
      }
    }
  }

  function buildChamberRitesPanel(state) {
    els.deckContent.innerHTML = '';
    const refs = {
      drain: null,
      echo: null,
      clamp: null,
      dossier: null,
    };

    // Excision II: Floor Drain Grate + Inspectable Graphic Relics
    if (state.highestExcisionMilestone >= 2) {
      const card = document.createElement('div');
      card.className = 'rite-card';
      card.innerHTML = `
        <div class="rite-head-with-art">
          <canvas class="action-emblem" width="38" height="38" data-role="drain-art" aria-hidden="true"></canvas>
          <div class="rite-head-text">
            <span class="rite-title">FLOOR DRAIN GRATE // M2</span>
            <span class="rite-meta" data-role="drain-meta"></span>
          </div>
        </div>
        <p class="rite-desc">Stop the grate to build weight beneath the tiles, then haul up what sank.</p>
        <div class="relic-chip-row" data-role="relic-chips"></div>
        <div class="relic-inspect-box" data-role="relic-inspect" hidden></div>
        <div class="rite-btn-row">
          <button type="button" class="rite-btn" data-role="drain-toggle-btn"></button>
          <button type="button" class="rite-btn" data-role="drain-dredge-btn"></button>
        </div>
      `;
      const drainArt = card.querySelector('[data-role="drain-art"]');
      drawActionArtwork(drainArt, 'rite', 'drain', state.drainSealed);

      const chipRow = card.querySelector('[data-role="relic-chips"]');
      const relicChips = [];
      for (const relic of DREDGED_RELICS) {
        const chip = document.createElement('button');
        chip.type = 'button';
        chip.className = 'relic-chip';
        chip.dataset.relicId = relic.id;
        chip.innerHTML = `
          <canvas class="relic-chip-canvas" width="20" height="20" aria-hidden="true"></canvas>
          <span>${relic.name}</span>
        `;
        chip.addEventListener('click', () => {
          selectedRelicId = selectedRelicId === relic.id ? null : relic.id;
          handlers.onRequestRender();
        });
        chipRow.appendChild(chip);
        relicChips.push({
          relic,
          chip,
          canvas: chip.querySelector('canvas'),
          lastDredged: null,
        });
      }

      const toggleBtn = card.querySelector('[data-role="drain-toggle-btn"]');
      const dredgeBtn = card.querySelector('[data-role="drain-dredge-btn"]');
      toggleBtn.addEventListener('click', () => handlers.onToggleDrainSeal());
      dredgeBtn.addEventListener('click', () => handlers.onDredgeSump());
      els.deckContent.appendChild(card);
      refs.drain = {
        metaEl: card.querySelector('[data-role="drain-meta"]'),
        artCanvas: drainArt,
        lastSealed: state.drainSealed,
        relicChips,
        inspectBox: card.querySelector('[data-role="relic-inspect"]'),
        toggleBtn,
        dredgeBtn,
      };
    }

    // Excision III: Neighbor Aperture Echo Routing
    if (state.highestExcisionMilestone >= 3) {
      const card = document.createElement('div');
      card.className = 'rite-card';
      card.innerHTML = `
        <div class="rite-head-with-art">
          <canvas class="action-emblem" width="38" height="38" data-role="echo-art" aria-hidden="true"></canvas>
          <div class="rite-head-text">
            <span class="rite-title">NEIGHBOR APERTURE // M3</span>
            <span class="rite-meta" data-role="echo-meta"></span>
          </div>
        </div>
        <p class="rite-desc">Let part of the flow slip through the wall slit; listen for what answers.</p>
        <div class="rite-btn-row">
          <button type="button" class="rite-btn" data-echo-pct="0">0% SEALED</button>
          <button type="button" class="rite-btn" data-echo-pct="25">25% ECHO</button>
          <button type="button" class="rite-btn" data-echo-pct="50">50% ECHO</button>
        </div>
      `;
      const echoArt = card.querySelector('[data-role="echo-art"]');
      drawActionArtwork(echoArt, 'rite', 'echo', state.echoDiversionPercent > 0);
      const buttons = [];
      for (const btn of card.querySelectorAll('[data-echo-pct]')) {
        const pct = Number(btn.dataset.echoPct);
        btn.addEventListener('click', () => handlers.onSetEchoDiversion(pct));
        buttons.push({ pct, btn });
      }
      els.deckContent.appendChild(card);
      refs.echo = {
        metaEl: card.querySelector('[data-role="echo-meta"]'),
        artCanvas: echoArt,
        lastActive: state.echoDiversionPercent > 0,
        buttons,
      };
    }

    // Excision IV: Valve Clamp & Marrow Bloom
    if (state.highestExcisionMilestone >= 4) {
      const card = document.createElement('div');
      card.className = 'rite-card';
      card.innerHTML = `
        <div class="rite-head-with-art">
          <canvas class="action-emblem" width="38" height="38" data-role="clamp-art" aria-hidden="true"></canvas>
          <div class="rite-head-text">
            <span class="rite-title">VALVE CLAMP // M4</span>
            <span class="rite-meta" data-role="clamp-meta"></span>
          </div>
        </div>
        <p class="rite-desc">Bolt the valve shut until the walls swell and split open.</p>
        <button type="button" class="rite-btn" data-role="clamp-btn"></button>
      `;
      const clampArt = card.querySelector('[data-role="clamp-art"]');
      drawActionArtwork(clampArt, 'rite', 'clamp', isValveClamped(state));
      const clampBtn = card.querySelector('[data-role="clamp-btn"]');
      clampBtn.addEventListener('click', () => handlers.onActivateValveClamp());
      els.deckContent.appendChild(card);
      refs.clamp = {
        metaEl: card.querySelector('[data-role="clamp-meta"]'),
        artCanvas: clampArt,
        lastClamped: isValveClamped(state),
        clampBtn,
      };
    }

    // Excision V: Ossified Throat & Redacted Cult Dossier
    if (state.highestExcisionMilestone >= 5) {
      const card = document.createElement('div');
      card.className = 'rite-card';
      card.innerHTML = `
        <div class="rite-head-with-art">
          <canvas class="action-emblem action-emblem--accent" width="38" height="38" data-role="throat-art" aria-hidden="true"></canvas>
          <div class="rite-head-text">
            <span class="rite-title">THE OSSIFIED THROAT // M5</span>
            <span class="rite-meta" data-role="dossier-meta"></span>
          </div>
        </div>
        <p class="rite-desc">Feed the throat in the mortar; it coughs up inked lines that were blacked out.</p>
      `;
      drawActionArtwork(card.querySelector('[data-role="throat-art"]'), 'rite', 'throat', true);
      const rows = [];
      for (const line of CULT_DOSSIER_LINES) {
        const row = document.createElement('div');
        row.className = 'dossier-line';
        row.dataset.dossierId = line.id;
        row.innerHTML = `
          <div class="rite-head">
            <span class="rite-title">${line.title}</span>
            <span class="rite-meta" data-role="clause-status">SEALED</span>
          </div>
          <p class="dossier-line-quote" data-role="quote"></p>
          <button type="button" class="rite-btn" data-role="offer-btn"></button>
        `;
        const quoteEl = row.querySelector('[data-role="quote"]');
        const offerBtn = row.querySelector('[data-role="offer-btn"]');
        const statusEl = row.querySelector('[data-role="clause-status"]');
        offerBtn.addEventListener('click', () => handlers.onOfferToThroat(line.id));
        card.appendChild(row);
        rows.push({ line, row, quoteEl, offerBtn, statusEl });
      }
      els.deckContent.appendChild(card);
      refs.dossier = { metaEl: card.querySelector('[data-role="dossier-meta"]'), rows };
    }

    ritesPanelRefs = refs;
    updateChamberRitesInPlace(state);
  }

  function updateChamberRitesInPlace(state) {
    if (!ritesPanelRefs) return;

    if (ritesPanelRefs.drain) {
      const { metaEl, artCanvas, relicChips, inspectBox, toggleBtn, dredgeBtn } =
        ritesPanelRefs.drain;
      if (ritesPanelRefs.drain.lastSealed !== state.drainSealed) {
        ritesPanelRefs.drain.lastSealed = state.drainSealed;
        drawActionArtwork(artCanvas, 'rite', 'drain', state.drainSealed);
      }
      metaEl.textContent = `SUMP PRESSURE: ${Math.floor(state.drainPressure)}% · RELICS: ${state.dredgedRelics.length}/${DREDGED_RELICS.length}`;
      toggleBtn.textContent = state.drainSealed ? 'VENT GRATE' : 'SEAL GRATE';
      toggleBtn.classList.toggle('rite-btn--active', state.drainSealed);

      for (const entry of relicChips) {
        const dredged = state.dredgedRelics.includes(entry.relic.id);
        entry.chip.classList.toggle('relic-chip--dredged', dredged);
        entry.chip.classList.toggle('relic-chip--selected', selectedRelicId === entry.relic.id);
        if (entry.lastDredged !== dredged || entry.canvas.width <= 1) {
          entry.lastDredged = dredged;
          drawActionArtwork(entry.canvas, 'relic', entry.relic.id, dredged);
        }
      }

      const inspected = DREDGED_RELICS.find((r) => r.id === selectedRelicId);
      if (inspected) {
        const dredged = state.dredgedRelics.includes(inspected.id);
        inspectBox.hidden = false;
        inspectBox.textContent = dredged
          ? `${inspected.name}: ${inspected.hint}`
          : `${inspected.name}: Still sunk beneath the grate. Seal the floor until it rises.`;
      } else {
        inspectBox.hidden = true;
      }

      const nextRelic = nextDredgeableRelic(state);
      dredgeBtn.disabled = !canDredgeSump(state);
      dredgeBtn.textContent = nextRelic
        ? canDredgeSump(state)
          ? `DREDGE: ${nextRelic.name}`
          : `DREDGE AT 100% (${Math.floor(state.drainPressure)}%)`
        : 'ALL RELICS HAULED';
    }

    if (ritesPanelRefs.echo) {
      const { metaEl, artCanvas, buttons } = ritesPanelRefs.echo;
      const active = state.echoDiversionPercent > 0;
      if (ritesPanelRefs.echo.lastActive !== active) {
        ritesPanelRefs.echo.lastActive = active;
        drawActionArtwork(artCanvas, 'rite', 'echo', active);
      }
      metaEl.textContent = active
        ? `${state.echoDiversionPercent}% ROUTED · ANSWERS HEARD: ${state.echoPulsesTriggered}`
        : `SLIT SEALED · ANSWERS: ${state.echoPulsesTriggered}`;
      for (const { pct, btn } of buttons) {
        btn.classList.toggle('rite-btn--active', pct === state.echoDiversionPercent);
      }
    }

    if (ritesPanelRefs.clamp) {
      const { metaEl, artCanvas, clampBtn } = ritesPanelRefs.clamp;
      const clamped = isValveClamped(state);
      if (ritesPanelRefs.clamp.lastClamped !== clamped) {
        ritesPanelRefs.clamp.lastClamped = clamped;
        drawActionArtwork(artCanvas, 'rite', 'clamp', clamped);
      }
      metaEl.textContent = clamped
        ? `SWELLING (${Math.ceil(state.clampRemainingSeconds)}s)`
        : state.marrowBloomRemainingSeconds > 0
          ? `MARROW BLOOM (${Math.ceil(state.marrowBloomRemainingSeconds)}s)`
          : `BLOOMS: ${state.marrowBloomCount}`;
      clampBtn.disabled = clamped;
      clampBtn.textContent = clamped
        ? `SWELLING (${Math.ceil(state.clampRemainingSeconds)}s)`
        : `CLAMP VALVE (${Math.round(valveClampDurationSec(state))}s)`;
    }

    if (ritesPanelRefs.dossier) {
      const { metaEl, rows } = ritesPanelRefs.dossier;
      metaEl.textContent = `${state.unredactedDossierLines.length}/${CULT_DOSSIER_LINES.length} UNSEALED`;
      for (const { line, row, quoteEl, offerBtn, statusEl } of rows) {
        const unredacted = state.unredactedDossierLines.includes(line.id);
        row.classList.toggle('dossier-line--unredacted', unredacted);
        statusEl.textContent = unredacted ? 'UNSEALED' : 'REDACTED';
        quoteEl.textContent = unredacted
          ? line.text
          : '[██████████████████████████████████████]';
        offerBtn.hidden = unredacted;
        if (!unredacted) {
          offerBtn.disabled = !canOfferToThroat(state, line);
          const costParts = [`${line.siphonCost} SIPHONS`];
          if (line.tendrilCost > 0) costParts.push(`${line.tendrilCost} TENDRILS`);
          if (line.requiresJar) costParts.push('1 JAR');
          offerBtn.textContent = `FEED THROAT: ${costParts.join(' + ')}`;
        }
      }
    }
  }

  function buildExcisionPanel(state) {
    els.deckContent.innerHTML = '';
    const panel = document.createElement('div');
    panel.className = 'excision-panel';
    const nextM = nextExcisionMilestone(state);
    const breachReady = canBreachToArc2(state);

    // Relocated Cycle / Milestone / Scar Resonance telemetry
    const telemetryBox = document.createElement('div');
    telemetryBox.className = 'excision-telemetry-box';
    const cycleLine = document.createElement('div');
    cycleLine.className = 'cycle-status';
    const resonanceLine = document.createElement('div');
    resonanceLine.className = 'scar-passive-status';
    telemetryBox.append(cycleLine, resonanceLine);
    panel.appendChild(telemetryBox);

    if (breachReady) {
      const breachCard = document.createElement('div');
      breachCard.className = 'file-zero-card';
      breachCard.innerHTML = `
        <div class="file-zero-kicker">EXCISION VI // PLEAD FOR SALVATION</div>
        <p class="file-zero-body">Lay your palm against the last wet seam. Ask the rain outside for salvation—whether that means being found, or finally forgetting there was ever a door.</p>
        <button type="button" class="excision-btn excision-btn--confirm" data-role="breach-btn">PLEAD FOR SALVATION</button>
      `;
      breachCard.querySelector('[data-role="breach-btn"]').addEventListener('click', () => {
        handlers.onPerformBreachToArc2();
      });
      panel.appendChild(breachCard);
    }

    const title = document.createElement('h3');
    title.className = 'excision-title';
    title.textContent =
      canClearNextMilestone(state) && nextM ? nextM.code : 'EXCISION // MOLTING';
    const summary = document.createElement('p');
    summary.className = 'excision-summary';
    summary.textContent =
      canClearNextMilestone(state) && nextM
        ? 'SEAM READY — SEVERING UNLOCKS THIS MILESTONE'
        : 'Cut away the overgrown mass to carry its scars forward.';

    const milestoneLine = document.createElement('div');
    milestoneLine.className = 'excision-milestone-line';
    milestoneLine.hidden = !nextM;

    const seamNote = document.createElement('div');
    seamNote.className = 'excision-seam-note';
    seamNote.textContent =
      'ONLY ONE SEAM GIVES WAY PER EXCISION — SURPLUS MASS STILL HARVESTS AS SCARS';
    seamNote.hidden = !hasExcessMilestoneMass(state);

    const reward = document.createElement('div');
    reward.className = 'excision-reward';
    reward.dataset.role = 'excision-reward';

    // Next Scar Tissue visual progress indicator
    const nextScarBlock = document.createElement('div');
    nextScarBlock.className = 'next-scar-block';
    const nextScarLabel = document.createElement('div');
    nextScarLabel.className = 'next-scar-label';
    const nextScarTrack = document.createElement('div');
    nextScarTrack.className = 'progress-track';
    const nextScarFill = document.createElement('div');
    nextScarFill.className = 'progress-fill progress-fill--deep';
    nextScarTrack.appendChild(nextScarFill);
    nextScarBlock.append(nextScarLabel, nextScarTrack);

    panel.append(title, summary, milestoneLine, seamNote, reward, nextScarBlock);

    let armBtn = null;
    if (canExcise(state)) {
      if (!excisionArmed) {
        armBtn = document.createElement('button');
        armBtn.type = 'button';
        armBtn.className = 'excision-btn excision-btn--arm';
        armBtn.addEventListener('click', () => {
          excisionArmed = true;
          lastDeckSignature = '';
          handlers.onRequestRender();
        });
        panel.appendChild(armBtn);
      } else {
        const row = document.createElement('div');
        row.className = 'excision-btn-row';
        const cancelBtn = document.createElement('button');
        cancelBtn.type = 'button';
        cancelBtn.className = 'excision-btn excision-btn--cancel';
        cancelBtn.textContent = 'HOLD';
        cancelBtn.addEventListener('click', () => {
          excisionArmed = false;
          lastDeckSignature = '';
          handlers.onRequestRender();
        });
        const confirmBtn = document.createElement('button');
        confirmBtn.type = 'button';
        confirmBtn.className = 'excision-btn excision-btn--confirm';
        confirmBtn.textContent = 'CONFIRM SEVERANCE';
        confirmBtn.addEventListener('click', () => {
          excisionArmed = false;
          selectedTab = 'scar_tree';
          lastDeckSignature = '';
          handlers.onPerformExcision();
        });
        row.append(cancelBtn, confirmBtn);
        panel.appendChild(row);
      }
    }

    els.deckContent.appendChild(panel);
    excisionRefs = {
      cycleLine,
      resonanceLine,
      milestoneLine,
      seamNote,
      reward,
      nextScarLabel,
      nextScarFill,
      armBtn,
    };
    updateExcisionInPlace(state);
  }

  function updateExcisionInPlace(state) {
    if (!excisionRefs) return;
    const {
      cycleLine,
      resonanceLine,
      milestoneLine,
      seamNote,
      reward,
      nextScarLabel,
      nextScarFill,
      armBtn,
    } = excisionRefs;
    const totalIchorMult =
      scarMultiplier(state) * relicIchorMultiplier(state) * dossierIchorMultiplier(state);
    const nextM = nextExcisionMilestone(state);
    const pending = pendingScarTissue(state);
    cycleLine.textContent = `CYCLE ${String(state.cycle).padStart(2, '0')} · MILESTONE M${state.highestExcisionMilestone}/6 · TOTAL SCAR ${formatInteger(state.scarTissue)}`;
    resonanceLine.textContent = `RESONANCE: ×${formatMultiplier(totalIchorMult)} ICHOR · ×${formatMultiplier(scarStillnessMultiplier(state))} STILLNESS`;
    milestoneLine.hidden = !nextM;
    if (nextM) {
      milestoneLine.textContent = `NEXT MILESTONE: ${nextM.code} (${formatInteger(cycleLifetimeIchorDisplay(state))} / ${formatInteger(nextM.massThreshold)})`;
    }
    seamNote.hidden = !hasExcessMilestoneMass(state);
    reward.textContent = canExcise(state)
      ? `HARVEST YIELD: +${formatInteger(pending)} SCAR TISSUE`
      : 'REACH 210,000 CYCLE ICHOR TO HARVEST';
    nextScarLabel.textContent = `NEXT SCAR TISSUE: ${formatInteger(cycleLifetimeIchorDisplay(state))} / ${formatInteger(nextBaseScarMassThreshold(state))}`;
    nextScarFill.style.width = `${(nextScarProgress(state) * 100).toFixed(1)}%`;
    if (armBtn) {
      armBtn.textContent =
        canClearNextMilestone(state) && nextM
          ? `SEVER EXCISION ${nextM.number} (+${formatInteger(pending)} SCAR)`
          : `HARVEST SCARS EARLY (+${formatInteger(pending)})`;
    }
  }

  function renderDeck(state) {
    const unlocked = isApparatusUnlocked(state) || isScarTreeUnlocked(state);
    els.deckPanel.hidden = !unlocked;
    if (!unlocked) return;

    const availableTabs = resolveAvailableTabs(state);
    const activeTab = availableTabs.includes(selectedTab) ? selectedTab : 'apparatus';
    selectedTab = activeTab;

    const visibleCreatures = CREATURE_LIST.filter((c) => isCreatureUnlocked(state, c));

    const signature = [
      availableTabs.join(','),
      activeTab,
      visibleCreatures.map((c) => c.id).join(','),
      `m${state.highestExcisionMilestone}`,
      isGraftUnlocked(state) ? 'g1' : 'g0',
      canExcise(state) ? 'ex1' : 'ex0',
      canClearNextMilestone(state) ? 'cm1' : 'cm0',
      hasExcessMilestoneMass(state) ? 'ov1' : 'ov0',
      canBreachToArc2(state) ? 'br1' : 'br0',
      excisionArmed ? 'arm1' : 'arm0',
    ].join('|');

    if (signature !== lastDeckSignature) {
      lastDeckSignature = signature;
      renderDeckTabs(availableTabs, activeTab);
      if (activeTab === 'apparatus') {
        buildOrganismCards(visibleCreatures);
      } else if (activeTab === 'jars_and_graft') {
        buildJarsAndGraftPanel(state);
      } else if (activeTab === 'scar_tree') {
        buildScarTreePanel(state);
      } else if (activeTab === 'chamber_rites') {
        buildChamberRitesPanel(state);
      } else {
        buildExcisionPanel(state);
      }
    }

    if (activeTab === 'apparatus') {
      updateOrganismCardsInPlace(state);
    } else if (activeTab === 'jars_and_graft') {
      updateJarsAndGraftInPlace(state);
    } else if (activeTab === 'scar_tree') {
      updateScarTreeInPlace(state);
    } else if (activeTab === 'chamber_rites') {
      updateChamberRitesInPlace(state);
    } else {
      updateExcisionInPlace(state);
    }
  }

  /* ==================== ARC 2 RENDERING ==================== */

  function renderArc2(state) {
    const desk = state.arc2;
    const compPct = Math.round(desk.complicity * 100);
    els.arc2Header.textContent = desk.fileZeroRevealed
      ? 'ARC 02 // FILE #00 — SUBJECT INTAKE'
      : 'ARC 02 // THE INQUIRY · CASE #14-B';
    els.arc2ChapterBadge.textContent = `CH ${desk.completedChapters}/4`;
    els.arc2ComplicityLabel.textContent = `COMPLICITY: ${compPct}% · ARC 1 SCAR ECHOES: ${formatInteger(state.scarTissue)}`;
    els.arc2ComplicityFill.style.width = `${compPct}%`;

    const nextCh = arc2NextChapter(desk);
    els.arc2ChapterStatus.textContent = nextCh
      ? `NEXT CHAPTER: ${nextCh.title} (${formatInteger(nextCh.leadsCost)} LEADS${nextCh.silenceCost > 0 ? ` · ${formatInteger(nextCh.silenceCost)} SILENCE` : ''})`
      : 'FILE #00 UNSEALED // SEAM OPEN';
    els.arc2ChapterStatus.classList.toggle('threshold-status--ready', arc2CanConnectNextChapter(desk) || desk.fileZeroRevealed);

    const formattedLeads = formatInteger(desk.leads);
    els.arc2LeadsValue.textContent = formattedLeads;
    const rate = arc2LeadsPerSecond(desk);
    els.arc2LeadsRate.hidden = rate <= 0;
    if (rate > 0) els.arc2LeadsRate.textContent = `${formatCompactDecimal(rate)} /s`;

    els.arc2SilenceText.textContent = `SILENCE: ${formatInteger(desk.silence)}`;
    els.arc2SilenceText.classList.toggle('status-tag--stillness-active', desk.isSilenceActive);
    els.arc2SilenceBadge.hidden = !desk.isSilenceActive;
    els.arc2FocusBadge.hidden = desk.focusRemainingSeconds <= 0;
    if (desk.focusRemainingSeconds > 0) {
      els.arc2FocusBadge.textContent = `FOCUS ×1.8 (${Math.ceil(desk.focusRemainingSeconds)}s)`;
    }
    els.inspectYieldText.textContent = `+${formatCompactDecimal(arc2InspectYield(desk))}`;

    els.fileZeroReveal.hidden = !desk.fileZeroRevealed;

    syncDeskAnomalies(state);
    renderArc2Deck(state);
  }

  function syncDeskAnomalies(state) {
    const list = state.arc2.activeDeskAnomalies ?? [];
    const activeIds = new Set(list.map((a) => String(a.id)));
    for (const child of Array.from(els.deskAnomalyLayer.children)) {
      const id = child.dataset.anomalyId;
      if (!activeIds.has(id)) {
        deskAnomalyCanvasMap.delete(id);
        child.remove();
      }
    }

    for (const anomaly of list) {
      const key = String(anomaly.id);
      if (deskAnomalyCanvasMap.has(key)) continue;
      const meta = Object.values(DESK_ANOMALY_TYPES).find((t) => t.id === anomaly.type);
      if (!meta) continue;

      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'anomaly-node';
      btn.dataset.anomalyId = key;
      btn.style.left = `${(anomaly.xFraction * 100).toFixed(1)}%`;
      btn.style.top = `${(anomaly.yFraction * 100).toFixed(1)}%`;
      btn.setAttribute('aria-label', meta.ariaLabel);

      const canvas = document.createElement('canvas');
      canvas.className = 'anomaly-glyph';
      canvas.width = 60;
      canvas.height = 60;

      const label = document.createElement('span');
      label.className = 'anomaly-label';
      label.textContent = meta.label;

      btn.append(canvas, label);
      btn.addEventListener('click', () => handlers.onInteractDeskAnomaly(anomaly.id));
      els.deskAnomalyLayer.appendChild(btn);
      deskAnomalyCanvasMap.set(key, { canvas, type: anomaly.type });
    }
  }

  function renderArc2Deck(state) {
    const desk = state.arc2;
    const sig = `${selectedArc2Tab}|ch${desk.completedChapters}|d${desk.ownedDeductions.length}`;
    if (sig !== lastArc2DeckSignature) {
      lastArc2DeckSignature = sig;
      const tabs = [
        { id: 'contacts', label: 'CONTACTS' },
        { id: 'deductions', label: 'DEDUCTIONS' },
        { id: 'evidence_board', label: 'EVIDENCE BOARD' },
      ];
      els.arc2DeckTabs.innerHTML = '';
      for (const t of tabs) {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.role = 'tab';
        btn.className = 'deck-tab';
        if (t.id === selectedArc2Tab) btn.classList.add('is-active');
        btn.textContent = t.label;
        btn.addEventListener('click', () => {
          selectedArc2Tab = t.id;
          lastArc2DeckSignature = '';
          handlers.onRequestRender();
        });
        els.arc2DeckTabs.appendChild(btn);
      }

      els.arc2DeckContent.innerHTML = '';
      if (selectedArc2Tab === 'contacts') {
        const rows = [];
        for (const c of INVESTIGATOR_CONTACTS) {
          const btn = document.createElement('button');
          btn.type = 'button';
          btn.className = 'deck-row';
          btn.dataset.contactId = c.id;
          btn.innerHTML = `
            <span class="deck-row-text">
              <span class="deck-row-title" data-role="title"></span>
              <span class="deck-row-sub">${c.desc}</span>
              <span class="organism-yield" data-role="rate"></span>
            </span>
            <span class="deck-row-pill" data-role="cost"></span>
          `;
          btn.addEventListener('click', () => handlers.onRecruitContact(c.id));
          els.arc2DeckContent.appendChild(btn);
          rows.push({
            spec: c,
            btn,
            titleEl: btn.querySelector('[data-role="title"]'),
            rateEl: btn.querySelector('[data-role="rate"]'),
            costEl: btn.querySelector('[data-role="cost"]'),
          });
        }
        arc2DeckRefs = { tab: 'contacts', rows };
      } else if (selectedArc2Tab === 'deductions') {
        const rows = [];
        for (const d of CASE_DEDUCTIONS) {
          const btn = document.createElement('button');
          btn.type = 'button';
          btn.className = 'deck-row';
          btn.dataset.deductionId = d.id;
          btn.innerHTML = `
            <span class="deck-row-text">
              <span class="deck-row-title">${d.name}</span>
              <span class="deck-row-sub">${d.effect}</span>
            </span>
            <span class="deck-row-pill" data-role="pill"></span>
          `;
          btn.addEventListener('click', () => handlers.onPinDeduction(d.id));
          els.arc2DeckContent.appendChild(btn);
          rows.push({ spec: d, btn, pillEl: btn.querySelector('[data-role="pill"]') });
        }
        arc2DeckRefs = { tab: 'deductions', rows };
      } else {
        const rows = [];
        for (const ch of INVESTIGATION_CHAPTERS) {
          const row = document.createElement('div');
          row.className = 'dossier-line';
          row.dataset.chapterNum = String(ch.number);
          row.innerHTML = `
            <div class="rite-head">
              <span class="rite-title">${ch.title}</span>
              <span class="rite-meta" data-role="ch-status"></span>
            </div>
            <p class="dossier-line-quote" data-role="ch-summary"></p>
            <button type="button" class="rite-btn" data-role="ch-connect-btn"></button>
          `;
          const connectBtn = row.querySelector('[data-role="ch-connect-btn"]');
          connectBtn.addEventListener('click', () => {
            handlers.onConnectNextChapter();
          });
          els.arc2DeckContent.appendChild(row);
          rows.push({
            ch,
            row,
            statusEl: row.querySelector('[data-role="ch-status"]'),
            summaryEl: row.querySelector('[data-role="ch-summary"]'),
            connectBtn,
          });
        }
        arc2DeckRefs = { tab: 'evidence_board', rows };
      }
    }

    if (!arc2DeckRefs) return;

    if (arc2DeckRefs.tab === 'contacts') {
      for (const { spec, btn, titleEl, rateEl, costEl } of arc2DeckRefs.rows) {
        const count = arc2ContactCount(desk, spec.id);
        const cost = arc2ContactCost(desk, spec);
        btn.disabled = desk.leads < cost;
        titleEl.textContent = `${spec.name} [${count}]`;
        rateEl.textContent = `EACH: +${formatCompactDecimal(arc2EffectiveContactRate(desk, spec))}/s`;
        costEl.textContent = `${formatInteger(cost)} LEADS`;
      }
    } else if (arc2DeckRefs.tab === 'deductions') {
      for (const { spec, btn, pillEl } of arc2DeckRefs.rows) {
        const owned = desk.ownedDeductions.includes(spec.id);
        btn.disabled = owned || desk.silence < spec.silenceCost;
        btn.classList.toggle('deck-row--accent', owned);
        pillEl.textContent = owned ? 'PINNED' : `PIN · ${formatInteger(spec.silenceCost)} SILENCE`;
      }
    } else {
      const nextCh = arc2NextChapter(desk);
      for (const { ch, row, statusEl, summaryEl, connectBtn } of arc2DeckRefs.rows) {
        const done = desk.completedChapters >= ch.number;
        const isNext = nextCh?.number === ch.number;
        row.classList.toggle('dossier-line--unredacted', done);
        statusEl.textContent = done ? 'CONNECTED' : isNext ? 'ACTIVE LEAD' : 'LOCKED';
        summaryEl.textContent = done ? ch.summary : '[PIN EVIDENCE TO RECONSTRUCT TIMELINE]';
        connectBtn.hidden = !isNext;
        if (isNext) {
          connectBtn.disabled = !arc2CanConnectNextChapter(desk);
          const costStr =
            ch.silenceCost > 0
              ? `${formatInteger(ch.leadsCost)} LEADS + ${formatInteger(ch.silenceCost)} SILENCE`
              : `${formatInteger(ch.leadsCost)} LEADS`;
          connectBtn.textContent = `CONNECT RED STRING (${costStr})`;
        }
      }
    }
  }

  /* ==================== ARC 3 & SHARED LEDGER ==================== */

  function renderArc3(state) {
    els.arc3Stats.textContent = `EXCISION MILESTONES: ${state.highestExcisionMilestone}/6 · LIFETIME SCAR TISSUE: ${formatInteger(state.scarTissue)} · CASE CHAPTERS: ${state.arc2.completedChapters}/4 · RECOVERED NOTES: ${state.discoveredDialogues.length}`;
  }

  function renderLedger(state) {
    const count = state.discoveredDialogues.length;
    els.openArchiveBtn.textContent = `ARCHIVE [${count}]`;
    els.openArchiveBtn.classList.toggle('archive-pill--has-notes', count > 0);

    const entry = state.activeDialogue;
    const nextId = entry?.id ?? null;
    if (nextId === lastDialogueId && count > 0) return;

    const isFreshArrival = hasHydratedDialogue && nextId != null && nextId !== lastDialogueId;
    hasHydratedDialogue = true;
    lastDialogueId = nextId;

    if (!entry) {
      els.ledgerSpeaker.textContent = '// GROUT LEDGER: SILENT';
      els.ledgerSpeaker.classList.remove('ledger-speaker--active');
      els.ledgerDivider.hidden = true;
      els.ledgerBody.textContent = 'AWAITING INPUT_';
      els.ledgerBody.classList.add('ledger-body--idle');
    } else {
      els.ledgerSpeaker.textContent = `// ${entry.speaker.toUpperCase()}`;
      els.ledgerSpeaker.classList.add('ledger-speaker--active');
      els.ledgerDivider.hidden = false;
      els.ledgerBody.textContent = entry.lines.join('\n');
      els.ledgerBody.classList.remove('ledger-body--idle');
      if (isFreshArrival) {
        triggerStoryDrip(entry);
        if (els.ledgerPanel) {
          els.ledgerPanel.classList.add('ledger-panel--arrived');
          window.clearTimeout(ledgerPulseTimer);
          ledgerPulseTimer = window.setTimeout(() => {
            els.ledgerPanel?.classList.remove('ledger-panel--arrived');
          }, 1600);
        }
      }
    }
  }

  function renderArchive(state) {
    els.archiveList.innerHTML = '';
    if (state.discoveredDialogues.length === 0) {
      const empty = document.createElement('p');
      empty.className = 'ledger-body ledger-body--idle';
      empty.textContent = 'NO FRAGMENTS RECOVERED YET';
      els.archiveList.appendChild(empty);
      return;
    }

    for (const entry of [...state.discoveredDialogues].reverse()) {
      const item = document.createElement('article');
      item.className = 'archive-entry';
      const head = document.createElement('div');
      head.className = 'archive-entry-head';
      const speaker = document.createElement('span');
      speaker.className = 'archive-entry-speaker';
      speaker.textContent = `// ${entry.speaker.toUpperCase()}`;
      const mood = document.createElement('span');
      mood.className = 'archive-entry-mood';
      mood.textContent = entry.mood.toUpperCase();
      head.append(speaker, mood);

      const lines = document.createElement('p');
      lines.className = 'archive-entry-lines';
      lines.textContent = entry.lines.join('\n');

      item.append(head, lines);
      els.archiveList.appendChild(item);
    }
  }

  return {
    render(state) {
      els.body.dataset.arc = state.activeArc;
      const isArc1 = state.activeArc === GAME_ARCS.ARC_1_CHAMBER;
      const isArc2 = state.activeArc === GAME_ARCS.ARC_2_INVESTIGATION;
      const isArc3 = state.activeArc === GAME_ARCS.ARC_3_CONVERGENCE;

      els.arc1Shell.hidden = !isArc1;
      els.arc2Shell.hidden = !isArc2;
      els.arc3Shell.hidden = !isArc3;
      els.landingTitle.textContent = isArc1
        ? 'THE COUNTING ROOM // WEB TERMINAL'
        : isArc2
          ? 'THE INQUIRY // CASE DESK TERMINAL'
          : 'CONVERGENCE // FILE #00 SEAM';

      if (isArc1) {
        renderTelemetry(state);
        renderChamberHud(state);
        syncAnomalies(state);
        renderDeck(state);
      } else if (isArc2) {
        renderArc2(state);
      } else {
        renderArc3(state);
      }
      renderLedger(state);
    },
    renderAnimatedGlyphs(breath) {
      for (const { canvas, type } of anomalyCanvasMap.values()) {
        drawAnomalyGlyph(canvas, type, breath);
      }
    },
    renderAnimatedDeskGlyphs(breath) {
      for (const { canvas, type } of deskAnomalyCanvasMap.values()) {
        drawDeskAnomalyGlyph(canvas, type, breath);
      }
    },
    spawnResidue(label, isRaw = false) {
      spawnResidueIn(els.residueLayer, label, isRaw);
    },
    spawnArc2Residue(label, isRaw = false) {
      spawnResidueIn(els.arc2ResidueLayer, label, isRaw);
    },
    triggerEventDrip(text, speaker = 'CHAMBER') {
      triggerEventDrip(text, speaker);
    },
    openArchive(state) {
      renderArchive(state);
      els.archiveModal.hidden = false;
    },
    closeArchive() {
      els.archiveModal.hidden = true;
    },
    resetViewState() {
      selectedTab = 'apparatus';
      selectedArc2Tab = 'contacts';
      selectedScarNodeId = 'first_incision';
      selectedRelicId = null;
      excisionArmed = false;
      lastDeckSignature = '';
      lastArc2DeckSignature = '';
      lastDialogueId = null;
      hasHydratedDialogue = false;
      lastCounterDisplay = -1;
      organismCardRefs = [];
      jarsPanelRefs = null;
      scarPanelRefs = null;
      ritesPanelRefs = null;
      excisionRefs = null;
      arc2DeckRefs = null;
      if (els.storyDripLayer) els.storyDripLayer.innerHTML = '';
      if (els.eventDripLayer) {
        els.eventDripLayer.innerHTML = '';
        els.eventDripLayer.classList.remove('is-dimmed');
      }
    },
  };
}
