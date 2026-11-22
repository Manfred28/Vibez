/**
 * Flicker-free DOM renderer for the GitHub Pages web version.
 * Keeps interactive controls mounted across 100ms ticks (updating text, disabled state, classes,
 * and affordability bars in place) so pointer clicks and keyboard focus are never dropped.
 */

import {
  CREATURE_LIST,
  CREATURE_TYPES,
  ORGAN_BALANCE,
  SPECIMEN_JAR_LIST,
  WALL_ANOMALY_TYPES,
  canAffordGraft,
  canExcise,
  creatureCost,
  creatureCount,
  cycleLifetimeIchorDisplay,
  effectiveCreatureIchorPerSec,
  effectiveLampStillnessPerSec,
  formatCompactDecimal,
  formatInteger,
  formatMultiplier,
  ichorDisplay,
  ichorPerSecond,
  isApparatusUnlocked,
  isGraftUnlocked,
  isJarsAndGraftUnlocked,
  manualTapYield,
  pendingScarTissue,
  scarMultiplier,
  scarStillnessMultiplier,
  stillnessDisplay,
  totalCreatureIchorPerSec,
} from './engine.js';
import { drawAnomalyGlyph, drawWeepingLampPortrait } from './chamberCanvas.js';

export function createUiController(handlers) {
  const els = {
    body: document.body,
    phaseHeader: document.getElementById('phase-header'),
    phaseBadge: document.getElementById('phase-badge'),
    cycleStatus: document.getElementById('cycle-status'),
    scarPassiveStatus: document.getElementById('scar-passive-status'),
    phaseProgressFill: document.getElementById('phase-progress-fill'),
    thresholdStatus: document.getElementById('threshold-status'),

    ichorRate: document.getElementById('ichor-rate'),
    counterGlassPlate: document.getElementById('counter-glass-plate'),
    counterValue: document.getElementById('counter-value'),
    stillnessText: document.getElementById('stillness-text'),
    stillnessActiveBadge: document.getElementById('stillness-active-badge'),
    gazeActiveBadge: document.getElementById('gaze-active-badge'),
    valveYieldText: document.getElementById('valve-yield-text'),
    residueLayer: document.getElementById('residue-layer'),
    anomalyLayer: document.getElementById('anomaly-layer'),

    deckPanel: document.getElementById('apparatus-deck'),
    deckTabs: document.getElementById('deck-tabs'),
    prestigeBanner: document.getElementById('prestige-banner'),
    deckContent: document.getElementById('deck-content'),

    ledgerSpeaker: document.getElementById('ledger-speaker'),
    ledgerDivider: document.getElementById('ledger-divider'),
    ledgerBody: document.getElementById('ledger-body'),
    openArchiveBtn: document.getElementById('open-archive-btn'),
    archiveModal: document.getElementById('archive-modal'),
    archiveList: document.getElementById('archive-list'),
  };

  let selectedTab = 'apparatus';
  let excisionArmed = false;
  let lastDeckSignature = '';
  let lastCounterDisplay = -1;
  let lastDialogueId = null;
  let residueSeq = 0;

  const anomalyCanvasMap = new Map();
  let lampPortraitCanvas = null;
  let lastLampWeeping = null;

  function resolveAvailableTabs(state) {
    const tabs = ['apparatus'];
    if (isJarsAndGraftUnlocked(state)) tabs.push('jars_and_graft');
    if (canExcise(state)) tabs.push('excision');
    return tabs;
  }

  function spawnResidue(yieldLabel) {
    const node = document.createElement('span');
    node.className = 'residue-float';
    const drift = ((residueSeq++ % 5) - 2) * 14;
    node.style.setProperty('--drift-x', `${drift}px`);
    node.textContent = `+${yieldLabel}`;
    els.residueLayer.appendChild(node);
    node.addEventListener('animationend', () => node.remove(), { once: true });
    if (els.residueLayer.childElementCount > 14) {
      els.residueLayer.firstElementChild?.remove();
    }
  }

  function renderTelemetry(state) {
    els.body.dataset.phase = String(state.phase);
    const phaseTitle = (state.phaseName || '').toUpperCase();
    els.phaseHeader.textContent = `PHASE ${String(state.phase).padStart(2, '0')} // ${phaseTitle}`;
    els.phaseBadge.textContent = `P${state.phase}`;

    const showCycle = state.cycle > 0 || state.scarTissue > 0;
    els.cycleStatus.hidden = !showCycle;
    if (showCycle) {
      els.cycleStatus.textContent = `CYCLE ${String(state.cycle).padStart(2, '0')} // SCAR TISSUE: ${formatInteger(state.scarTissue)}`;
    }

    els.scarPassiveStatus.hidden = state.scarTissue <= 0;
    if (state.scarTissue > 0) {
      els.scarPassiveStatus.textContent = `SCAR PASSIVE: ×${formatMultiplier(scarMultiplier(state))} ICHOR · ×${formatMultiplier(scarStillnessMultiplier(state))} STILLNESS`;
    }

    const pct = Math.max(0, Math.min(100, state.phaseProgress * 100));
    els.phaseProgressFill.style.width = `${pct.toFixed(2)}%`;
    els.phaseProgressFill.classList.toggle('progress-fill--deep', state.phase >= 2);

    const ready = canExcise(state);
    els.thresholdStatus.classList.toggle('threshold-status--ready', ready);
    if (ready) {
      els.thresholdStatus.textContent = 'OVERGROWTH CRITICAL // EXCISION READY';
    } else if (state.nextPhaseThreshold != null) {
      els.thresholdStatus.textContent = `NEXT FRACTURE: ${formatInteger(cycleLifetimeIchorDisplay(state))} / ${formatInteger(state.nextPhaseThreshold)}`;
    } else if (state.phase >= 4) {
      els.thresholdStatus.textContent = `EXCISION MASS: ${formatInteger(cycleLifetimeIchorDisplay(state))} / ${formatInteger(ORGAN_BALANCE.EXCISION_ICHOR_THRESHOLD)}`;
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
      els.gazeActiveBadge.textContent = `EYE FOCUSED ×1.6 (${Math.ceil(state.gazeRemainingSeconds)}s)`;
    }

    els.valveYieldText.textContent = `+${formatCompactDecimal(manualTapYield(state))}`;
  }

  function syncAnomalies(state) {
    const activeIds = new Set(state.activeAnomalies.map((a) => String(a.id)));

    // Remove interacted anomalies
    for (const child of Array.from(els.anomalyLayer.children)) {
      const id = child.dataset.anomalyId;
      if (!activeIds.has(id)) {
        anomalyCanvasMap.delete(id);
        child.remove();
      }
    }

    // Mount newly spawned anomalies
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
    lastLampWeeping = null;

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
        lampPortraitCanvas = canvas;
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

      if (spec.id === CREATURE_TYPES.WEEPING_LAMP.id) {
        const stillEl = document.createElement('span');
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
    }
  }

  function updateOrganismCardsInPlace(state) {
    for (const card of els.deckContent.querySelectorAll('.organism-card')) {
      const id = card.dataset.creatureId;
      const spec = CREATURE_LIST.find((c) => c.id === id);
      if (!spec) continue;

      const count = creatureCount(state, id);
      const cost = creatureCost(state, id);
      const canAfford = state.ichor >= cost;
      const progress = cost <= 0 ? 1 : Math.max(0, Math.min(1, state.ichor / cost));
      const isWeeping = id === CREATURE_TYPES.WEEPING_LAMP.id && count > 0 && state.isStillnessActive;
      const isWorking = count > 0 && (id !== CREATURE_TYPES.WEEPING_LAMP.id || isWeeping);

      card.classList.toggle('organism-card--weeping', isWeeping);
      card.classList.toggle('organism-card--affordable', canAfford && !isWeeping);
      card.classList.toggle('organism-card--dormant', !canAfford && count === 0);

      card.querySelector('[data-role="name"]').textContent = `${spec.name} [${count}]`;

      const statusEl = card.querySelector('[data-role="status"]');
      statusEl.textContent =
        count <= 0
          ? 'DORMANT'
          : id === CREATURE_TYPES.SUTURE_HAND.id
            ? 'PRESSING VALVE'
            : id === CREATURE_TYPES.LEECH_CLERK.id
              ? 'GRAZING GLASS'
              : state.isStillnessActive
                ? 'WEEPING ×2'
                : 'IDLE 3s TO WEEP';
      statusEl.className = 'organism-status-badge';
      if (isWeeping) statusEl.classList.add('organism-status-badge--weeping');
      else if (isWorking) statusEl.classList.add('organism-status-badge--active');

      const viewport = card.querySelector('[data-role="viewport"]');
      viewport.classList.toggle('specimen-viewport--dim', !canAfford && count === 0);
      viewport.classList.toggle('specimen-viewport--weeping', isWeeping);

      const eachRate = effectiveCreatureIchorPerSec(state, id);
      const colonyRate = totalCreatureIchorPerSec(state, id);
      const yieldEl = card.querySelector('[data-role="yield"]');
      yieldEl.textContent =
        count > 0
          ? `EACH: +${formatCompactDecimal(eachRate)}/s · COLONY: +${formatCompactDecimal(colonyRate)}/s`
          : `EACH: +${formatCompactDecimal(eachRate)}/s`;
      yieldEl.classList.toggle('organism-yield--idle', count === 0);

      const stillEl = card.querySelector('[data-role="stillness"]');
      if (stillEl) {
        const totalStill = effectiveLampStillnessPerSec(state) * Math.max(1, count);
        stillEl.textContent = `IDLE DRIP: +${formatCompactDecimal(totalStill)} STILLNESS/s`;
        stillEl.classList.toggle('organism-stillness--active', isWeeping);
      }

      const buyBtn = card.querySelector('[data-role="buy"]');
      buyBtn.disabled = !canAfford;
      card.querySelector('[data-role="cost"]').textContent = `${formatInteger(cost)} ICHOR`;

      const affordFill = card.querySelector('[data-role="afford"]');
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
    }
  }

  function buildJarsAndGraftPanel(state) {
    els.deckContent.innerHTML = '';
    for (const jar of SPECIMEN_JAR_LIST) {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'deck-row';
      btn.dataset.jarId = jar.id;
      btn.innerHTML = `
        <span class="deck-row-text">
          <span class="deck-row-title">${jar.name}</span>
          <span class="deck-row-sub">${jar.effect}</span>
        </span>
        <span class="deck-row-pill" data-role="pill"></span>
      `;
      btn.addEventListener('click', () => handlers.onBottleJar(jar.id));
      els.deckContent.appendChild(btn);
    }

    const divider = document.createElement('div');
    divider.className = 'deck-divider';
    els.deckContent.appendChild(divider);

    if (isGraftUnlocked(state)) {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'deck-row deck-row--accent';
      btn.dataset.role = 'graft-btn';
      btn.innerHTML = `
        <span class="deck-row-text">
          <span class="deck-row-title" data-role="graft-title"></span>
          <span class="deck-row-sub">Fuse 2 Suture-Hands + 2 Leech-Clerks (+18/s Ichor, +0.7/s Stillness)</span>
        </span>
        <span class="deck-row-pill">GRAFT</span>
      `;
      btn.addEventListener('click', () => handlers.onPerformGraft());
      els.deckContent.appendChild(btn);
    } else {
      const hint = document.createElement('div');
      hint.className = 'locked-hint';
      hint.textContent = 'UNLOCKS AT PHASE 03';
      els.deckContent.appendChild(hint);
    }
  }

  function updateJarsAndGraftInPlace(state) {
    for (const btn of els.deckContent.querySelectorAll('[data-jar-id]')) {
      const jar = SPECIMEN_JAR_LIST.find((j) => j.id === btn.dataset.jarId);
      if (!jar) continue;
      const owned = state.ownedJars.includes(jar.id);
      const canAfford = !owned && state.stillness >= jar.stillnessCost;
      btn.disabled = !canAfford;
      btn.classList.toggle('deck-row--accent', owned);
      btn.querySelector('[data-role="pill"]').textContent = owned
        ? 'SEALED'
        : `BOTTLE · ${formatInteger(jar.stillnessCost)} STILLNESS`;
    }

    const graftBtn = els.deckContent.querySelector('[data-role="graft-btn"]');
    if (graftBtn) {
      graftBtn.disabled = !canAffordGraft(state);
      graftBtn.querySelector('[data-role="graft-title"]').textContent =
        `GRAFTED CHOIR-TENDRIL [${state.graftedTendrils}]`;
    }
  }

  function buildExcisionPanel(state) {
    els.deckContent.innerHTML = '';
    const panel = document.createElement('div');
    panel.className = 'excision-panel';
    panel.innerHTML = `
      <h3 class="excision-title">EXCISION // MOLTING</h3>
      <p class="excision-summary">Sever the overgrown organ back to bare tile. Retains permanent Scar Tissue (+75% Ichor, +25% Stillness per unit) and reveals deeper Cycle 1+ fragments.</p>
      <div class="excision-reward" data-role="excision-reward"></div>
    `;

    if (!excisionArmed) {
      const armBtn = document.createElement('button');
      armBtn.type = 'button';
      armBtn.className = 'excision-btn excision-btn--arm';
      armBtn.textContent = 'EXCISE & MOLT';
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
        selectedTab = 'apparatus';
        lastDeckSignature = '';
        handlers.onPerformExcision();
      });

      row.append(cancelBtn, confirmBtn);
      panel.appendChild(row);
    }

    els.deckContent.appendChild(panel);
    updateExcisionInPlace(state);
  }

  function updateExcisionInPlace(state) {
    const reward = els.deckContent.querySelector('[data-role="excision-reward"]');
    if (reward) {
      reward.textContent = `YIELD: +${formatInteger(pendingScarTissue(state))} SCAR TISSUE`;
    }
  }

  function renderDeck(state) {
    const unlocked = isApparatusUnlocked(state);
    els.deckPanel.hidden = !unlocked;
    if (!unlocked) return;

    const availableTabs = resolveAvailableTabs(state);
    const activeTab = availableTabs.includes(selectedTab) ? selectedTab : 'apparatus';
    selectedTab = activeTab;

    els.prestigeBanner.hidden = state.scarTissue <= 0;
    if (state.scarTissue > 0) {
      els.prestigeBanner.textContent = `SCAR PASSIVE: ×${formatMultiplier(scarMultiplier(state))} ICHOR · ×${formatMultiplier(scarStillnessMultiplier(state))} STILLNESS`;
    }

    const visibleCreatures = CREATURE_LIST.filter(
      (c) =>
        state.cycle > 0 ||
        cycleLifetimeIchorDisplay(state) >= c.unlockIchorThreshold ||
        creatureCount(state, c.id) > 0,
    );

    const signature = [
      availableTabs.join(','),
      activeTab,
      visibleCreatures.map((c) => c.id).join(','),
      isGraftUnlocked(state) ? 'g1' : 'g0',
      excisionArmed ? 'arm1' : 'arm0',
    ].join('|');

    if (signature !== lastDeckSignature) {
      lastDeckSignature = signature;
      renderDeckTabs(availableTabs, activeTab);
      if (activeTab === 'apparatus') {
        buildOrganismCards(visibleCreatures);
      } else if (activeTab === 'jars_and_graft') {
        buildJarsAndGraftPanel(state);
      } else {
        buildExcisionPanel(state);
      }
    }

    if (activeTab === 'apparatus') {
      updateOrganismCardsInPlace(state);
    } else if (activeTab === 'jars_and_graft') {
      updateJarsAndGraftInPlace(state);
    } else {
      updateExcisionInPlace(state);
    }
  }

  function renderLedger(state) {
    const count = state.discoveredDialogues.length;
    els.openArchiveBtn.textContent = `ARCHIVE [${count}]`;
    els.openArchiveBtn.classList.toggle('archive-pill--has-notes', count > 0);

    const entry = state.activeDialogue;
    const nextId = entry?.id ?? null;
    if (nextId === lastDialogueId && count > 0) return;
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
      renderTelemetry(state);
      renderChamberHud(state);
      syncAnomalies(state);
      renderDeck(state);
      renderLedger(state);
    },
    renderAnimatedGlyphs(breath) {
      for (const { canvas, type } of anomalyCanvasMap.values()) {
        drawAnomalyGlyph(canvas, type, breath);
      }
    },
    spawnResidue,
    openArchive(state) {
      renderArchive(state);
      els.archiveModal.hidden = false;
    },
    closeArchive() {
      els.archiveModal.hidden = true;
    },
    resetViewState() {
      selectedTab = 'apparatus';
      excisionArmed = false;
      lastDeckSignature = '';
      lastDialogueId = null;
      lastCounterDisplay = -1;
    },
  };
}
