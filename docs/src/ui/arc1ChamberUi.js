/**
 * Arc 1 Chamber DOM renderer: telemetry header, HUD counters, twin specimen jar shelves,
 * feathered photographic wall anomalies, and the 5-tab apparatus deck (Organisms, Jars,
 * Surgical Scar Tree, Chamber Rites, Excision).
 */

import {
  CREATURE_LIST,
  CREATURE_TYPES,
  CULT_DOSSIER_LINES,
  DEFAULT_PHASE_NAMES,
  DREDGED_RELICS,
  EXCISION_MILESTONES,
  REPEATABLE_SCAR_GRAFTS,
  SCAR_NODES,
  SPECIMEN_JAR_LIST,
  WALL_ANOMALY_TYPES,
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
  formatValveYield,
  graftCooldownDurationSec,
  graftStillnessCost,
  hasExcessMilestoneMass,
  hasNode,
  ichorDisplay,
  ichorPerSecond,
  isApparatusUnlocked,
  isChamberRitesUnlocked,
  isChoirLungOnCooldown,
  isColonyProliferationUnlocked,
  isCreatureUnlocked,
  isGraftOnCooldown,
  isGraftUnlocked,
  isJarUnlocked,
  isJarsAndGraftUnlocked,
  isRepeatableGraftsUnlocked,
  isScarTreeUnlocked,
  isValveClamped,
  manualTapYield,
  maxAffordableCreatureQuote,
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
  totalCreatureIchorPerSec,
  valveClampDurationSec,
} from '../engine.js?v=3.15';
import { drawActionArtwork } from '../chamberCanvas.js?v=3.15';
import {
  anomalyAssetUrl,
  graftAssetUrl,
  jarAssetUrl,
  relicAssetUrl,
  specimenAssetUrl,
} from '../version.js?v=3.15';
import { arc1UnlockKeys, markTabSeen, shouldAlertArc1Tab } from './tabAlerts.js?v=3.15';

export const FOUNDATIONAL_SHELF_JARS = Object.freeze([
  'yesterdays_dust',
  'unspent_breath',
  'almost_a_name',
]);

export const DEEP_SHELF_JARS = Object.freeze([
  'salt_from_the_grout',
  'a_borrowed_pulse',
  'the_uncounted_hour',
]);

/**
 * Splits owned Specimen Jars into the left foundational wall shelf and right deep wall shelf
 * in canonical order. Mirrors Android `partitionChamberShelfJars` 1:1.
 */
export function partitionChamberShelfJars(ownedJars) {
  const owned = new Set(Array.isArray(ownedJars) ? ownedJars : []);
  return {
    left: FOUNDATIONAL_SHELF_JARS.filter((id) => owned.has(id)),
    right: DEEP_SHELF_JARS.filter((id) => owned.has(id)),
  };
}

export function lastClearedExcisionName(state) {
  if (state.highestExcisionMilestone <= 0) return null;
  return EXCISION_MILESTONES.find((m) => m.number === state.highestExcisionMilestone)?.code ?? null;
}

export function createArc1ChamberUi(els, handlers) {
  let selectedTab = 'apparatus';
  let selectedScarNodeId = 'first_incision';
  let selectedRelicId = null;
  let excisionArmed = false;
  let lastDeckSignature = '';
  let lastJarShelfSignature = '';
  let lastCounterDisplay = -1;

  const seenArc1Keys = new Map();
  const deckTabButtons = new Map();
  const mountedWallAnomalyIds = new Set();

  // Cached element references for active Arc 1 deck panels (avoids 10Hz DOM queries)
  let organismCardRefs = [];
  let organismGraftRefs = { graftRef: null, lungRef: null };
  let jarsPanelRefs = null;
  let scarPanelRefs = null;
  let ritesPanelRefs = null;
  let excisionRefs = null;

  function resolveAvailableTabs(state) {
    const tabs = ['apparatus'];
    if (isJarsAndGraftUnlocked(state)) tabs.push('jars_and_graft');
    if (isScarTreeUnlocked(state)) tabs.push('scar_tree');
    if (isChamberRitesUnlocked(state)) tabs.push('chamber_rites');
    if (
      canExcise(state) ||
      canBreachToArc2(state) ||
      state.cycle > 0 ||
      state.highestExcisionMilestone > 0
    ) {
      tabs.push('excision');
    }
    return tabs;
  }

  function renderTelemetry(state) {
    els.body.dataset.phase = String(state.phase);
    const phaseTitle = (state.phaseName || '').toUpperCase();
    els.phaseHeader.textContent = phaseTitle;

    const nextThreshold = state.nextPhaseThreshold;
    const nextMilestone = nextExcisionMilestone(state);
    const readyAccent =
      canBreachToArc2(state) || canClearNextMilestone(state) || canExcise(state);
    const pct = Math.max(0, Math.min(1, state.phaseProgress ?? 0)) * 100;
    if (els.phaseProgressFill) {
      els.phaseProgressFill.style.width = `${pct.toFixed(1)}%`;
      els.phaseProgressFill.classList.toggle('progress-fill--deep', readyAccent);
    }

    els.thresholdStatus.hidden = false;
    els.thresholdStatus.classList.toggle(
      'threshold-status--ready',
      readyAccent && nextThreshold == null,
    );

    const currentFormatted = formatInteger(cycleLifetimeIchorDisplay(state));
    if (nextThreshold != null) {
      const nextStageName =
        DEFAULT_PHASE_NAMES[Math.min(state.phase + 1, 4)] ?? DEFAULT_PHASE_NAMES[4];
      els.thresholdStatus.textContent = `NEXT: ${nextStageName} · ${currentFormatted} / ${formatInteger(nextThreshold)}`;
    } else if (canBreachToArc2(state)) {
      els.thresholdStatus.textContent = 'SEAM UNSEALED // PLEAD FOR SALVATION';
    } else if (nextMilestone && canClearNextMilestone(state)) {
      els.thresholdStatus.textContent = `SEAM READY // ${nextMilestone.code}`;
    } else if (nextMilestone && canExcise(state)) {
      els.thresholdStatus.textContent = `${nextMilestone.code} · ${currentFormatted} / ${formatInteger(nextMilestone.massThreshold)} · EXCISION READY`;
    } else if (nextMilestone) {
      els.thresholdStatus.textContent = `NEXT: ${nextMilestone.code} · ${currentFormatted} / ${formatInteger(nextMilestone.massThreshold)}`;
    } else {
      els.thresholdStatus.textContent = 'SEAM UNSEALED // PLEAD FOR SALVATION';
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
    const valveLabel = clamped ? 'CLAMP' : `+${formatValveYield(manualTapYield(state))}`;
    els.valveYieldText.textContent = valveLabel;
    els.valveYieldText.classList.toggle('valve-yield-text--compact', valveLabel.length >= 6);
    els.valveSubLabel.textContent = clamped ? `${Math.ceil(state.clampRemainingSeconds)}s HOLD` : 'VALVE';
  }

  function syncAnomalies(state) {
    const activeIds = new Set(state.activeAnomalies.map((a) => String(a.id)));
    for (const child of Array.from(els.anomalyLayer.children)) {
      const id = child.dataset.anomalyId;
      if (!activeIds.has(id)) {
        mountedWallAnomalyIds.delete(id);
        child.remove();
      }
    }

    for (const anomaly of state.activeAnomalies) {
      const key = String(anomaly.id);
      if (mountedWallAnomalyIds.has(key)) continue;
      const meta = Object.values(WALL_ANOMALY_TYPES).find((t) => t.id === anomaly.type);
      if (!meta) continue;

      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = `anomaly-node anomaly-node--wall anomaly-node--${anomaly.type}`;
      btn.dataset.anomalyId = key;
      btn.style.left = `${(anomaly.xFraction * 100).toFixed(1)}%`;
      btn.style.top = `${(anomaly.yFraction * 100).toFixed(1)}%`;
      const phaseOffsetSec = ((Math.abs(Number(anomaly.id) || 0) * 1.37) % 4.6).toFixed(2);
      btn.style.setProperty('--breath-delay', `-${phaseOffsetSec}s`);
      btn.setAttribute('aria-label', meta.ariaLabel);

      const photo = document.createElement('img');
      photo.className = 'anomaly-photo';
      photo.src = anomalyAssetUrl(anomaly.type);
      photo.alt = '';
      photo.draggable = false;
      photo.setAttribute('aria-hidden', 'true');

      const label = document.createElement('span');
      label.className = 'anomaly-label';
      label.textContent = meta.label;

      btn.append(photo, label);
      btn.addEventListener('click', () => handlers.onInteractAnomaly(anomaly.id));
      els.anomalyLayer.appendChild(btn);
      mountedWallAnomalyIds.add(key);
    }
  }

  function renderChamberJarShelves(state) {
    const container = els.chamberJarShelves;
    if (!container) return;
    const { left, right } = partitionChamberShelfJars(state.ownedJars);
    const stillnessFlag = state.isStillnessActive ? '1' : '0';
    const sig = `${left.join(',')}|${right.join(',')}|${stillnessFlag}`;
    if (sig === lastJarShelfSignature) return;
    lastJarShelfSignature = sig;

    container.classList.toggle('chamber-jar-shelves--stillness', Boolean(state.isStillnessActive));
    container.innerHTML = '';
    if (left.length === 0 && right.length === 0) return;

    const buildShelf = (jarIds, side) => {
      if (jarIds.length === 0) return;
      const shelf = document.createElement('div');
      shelf.className = `chamber-jar-shelf chamber-jar-shelf--${side}`;
      shelf.dataset.shelfSide = side;

      const row = document.createElement('div');
      row.className = 'chamber-jar-row';

      for (const jarId of jarIds) {
        const specimen = document.createElement('span');
        const extraClass =
          jarId === 'the_uncounted_hour'
            ? ' chamber-jar-specimen--tall'
            : jarId === 'a_borrowed_pulse'
              ? ' chamber-jar-specimen--pulse'
              : '';
        specimen.className = `chamber-jar-specimen${extraClass}`;
        specimen.dataset.jarId = jarId;

        const img = document.createElement('img');
        img.className = 'chamber-jar-img';
        img.src = jarAssetUrl(jarId);
        img.alt = '';
        img.draggable = false;
        img.setAttribute('aria-hidden', 'true');

        specimen.appendChild(img);
        row.appendChild(specimen);
      }

      const ledge = document.createElement('div');
      ledge.className = 'chamber-jar-ledge';
      shelf.append(row, ledge);
      container.appendChild(shelf);
    };

    buildShelf(left, 'left');
    buildShelf(right, 'right');
  }

  function renderDeckTabs(state, availableTabs, activeTab) {
    const tabLabels = {
      apparatus: 'ORGANISMS',
      jars_and_graft: 'JARS',
      scar_tree: 'SCARS',
      chamber_rites: 'RITES',
      excision: 'EXCISION',
    };
    els.deckTabs.innerHTML = '';
    deckTabButtons.clear();
    for (const tab of availableTabs) {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.role = 'tab';
      btn.className = 'deck-tab';
      btn.dataset.tabId = tab;
      if (tab === 'excision') btn.classList.add('deck-tab--excision');
      if (tab === activeTab) btn.classList.add('is-active');
      btn.setAttribute('aria-selected', String(tab === activeTab));
      const pip = document.createElement('span');
      pip.className = 'deck-tab-pip';
      pip.setAttribute('aria-hidden', 'true');
      const labelSpan = document.createElement('span');
      labelSpan.textContent = tabLabels[tab];
      btn.append(pip, labelSpan);
      btn.addEventListener('click', () => {
        selectedTab = tab;
        markTabSeen(seenArc1Keys, tab, arc1UnlockKeys(state, tab));
        if (tab !== 'excision') excisionArmed = false;
        lastDeckSignature = '';
        handlers.onRequestRender();
      });
      els.deckTabs.appendChild(btn);
      deckTabButtons.set(tab, btn);
    }
    updateDeckTabAlerts(state, availableTabs, activeTab);
  }

  function updateDeckTabAlerts(state, availableTabs, activeTab) {
    for (const tab of availableTabs) {
      const btn = deckTabButtons.get(tab);
      if (!btn) continue;
      const alerting = shouldAlertArc1Tab(state, tab, activeTab, seenArc1Keys);
      btn.classList.toggle('deck-tab--alert', alerting);
      btn.dataset.tabAlert = alerting ? 'true' : 'false';
    }
  }

  function buildOrganismCards(state, visibleCreatures) {
    els.deckContent.innerHTML = '';
    organismCardRefs = [];
    organismGraftRefs = { graftRef: null, lungRef: null };

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
      const img = document.createElement('img');
      img.src = specimenAssetUrl(spec.id);
      img.alt = spec.portraitAlt;
      img.draggable = false;
      viewport.appendChild(img);

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

      let proliferateBtn = null;
      let proliferateLabelEl = null;
      let proliferateCostEl = null;
      if (isColonyProliferationUnlocked(state)) {
        proliferateBtn = document.createElement('button');
        proliferateBtn.type = 'button';
        proliferateBtn.className = 'proliferate-btn';
        proliferateBtn.dataset.role = 'proliferate';
        proliferateBtn.dataset.creatureId = spec.id;
        proliferateLabelEl = document.createElement('span');
        proliferateLabelEl.className = 'proliferate-label';
        proliferateLabelEl.textContent = 'PROLIFERATE';
        proliferateCostEl = document.createElement('span');
        proliferateCostEl.className = 'proliferate-cost';
        proliferateBtn.append(proliferateLabelEl, proliferateCostEl);
        proliferateBtn.addEventListener('click', () => handlers.onBuyMaxCreature?.(spec.id));
      }

      const affordBar = document.createElement('div');
      affordBar.className = 'afford-bar';
      const affordFill = document.createElement('div');
      affordFill.className = 'afford-bar-fill';
      affordFill.dataset.role = 'afford';
      affordBar.appendChild(affordFill);

      if (proliferateBtn) {
        card.append(header, body, proliferateBtn, affordBar);
      } else {
        card.append(header, body, affordBar);
      }
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
        proliferateBtn,
        proliferateLabelEl,
        proliferateCostEl,
        affordFill,
      });
    }
    // Append Grafting (Choir-Tendril & Choir-Lung) right inside ORGANISMS once unlocked
    const hasTendril = isGraftUnlocked(state);
    const hasLung = state.highestExcisionMilestone >= 4;
    if (hasTendril || hasLung) {
      const divider = document.createElement('div');
      divider.className = 'deck-divider';
      els.deckContent.appendChild(divider);
    }

    let graftRef = null;
    if (hasTendril) {
      const graftBtn = document.createElement('button');
      graftBtn.type = 'button';
      graftBtn.className = 'deck-row deck-row--accent';
      graftBtn.dataset.role = 'graft-btn';
      graftBtn.innerHTML = `
        <span class="graft-photo-frame" data-role="graft-frame" aria-hidden="true">
          <img
            class="graft-photo-emblem"
            src="${graftAssetUrl('tendril')}"
            alt=""
            draggable="false"
            aria-hidden="true"
          />
        </span>
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
      graftRef = {
        btn: graftBtn,
        frameEl: graftBtn.querySelector('[data-role="graft-frame"]'),
        titleEl: graftBtn.querySelector('[data-role="graft-title"]'),
        pillEl: graftBtn.querySelector('[data-role="graft-pill"]'),
        cdTrack: graftBtn.querySelector('[data-role="graft-cd-track"]'),
        cdFill: graftBtn.querySelector('[data-role="graft-cd-fill"]'),
      };
    }

    let lungRef = null;
    if (hasLung) {
      const lungBtn = document.createElement('button');
      lungBtn.type = 'button';
      lungBtn.className = 'deck-row deck-row--accent';
      lungBtn.dataset.role = 'choir-lung-btn';
      lungBtn.innerHTML = `
        <span class="graft-photo-frame" data-role="lung-frame" aria-hidden="true">
          <img
            class="graft-photo-emblem"
            src="${graftAssetUrl('choir_lung')}"
            alt=""
            draggable="false"
            aria-hidden="true"
          />
        </span>
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
      lungRef = {
        btn: lungBtn,
        frameEl: lungBtn.querySelector('[data-role="lung-frame"]'),
        titleEl: lungBtn.querySelector('[data-role="choir-lung-title"]'),
        pillEl: lungBtn.querySelector('[data-role="lung-pill"]'),
        cdTrack: lungBtn.querySelector('[data-role="lung-cd-track"]'),
        cdFill: lungBtn.querySelector('[data-role="lung-cd-fill"]'),
      };
    }

    organismGraftRefs = { graftRef, lungRef };
  }

  function updateOrganismCardsInPlace(state) {
    for (const ref of organismCardRefs) {
      const {
        spec,
        card,
        nameEl,
        statusEl,
        viewport,
        yieldEl,
        stillEl,
        buyBtn,
        costEl,
        proliferateBtn,
        proliferateLabelEl,
        proliferateCostEl,
        affordFill,
      } = ref;
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

      if (proliferateBtn && proliferateLabelEl && proliferateCostEl) {
        const bulk = maxAffordableCreatureQuote(state, id);
        const canProliferate = bulk.count > 0;
        proliferateBtn.disabled = !canProliferate;
        proliferateLabelEl.textContent = canProliferate ? `PROLIFERATE ×${bulk.count}` : 'PROLIFERATE';
        proliferateCostEl.textContent = canProliferate
          ? `${formatInteger(bulk.totalCost)} ICHOR`
          : 'INSUFFICIENT ICHOR';
      }

      affordFill.style.width = `${(progress * 100).toFixed(1)}%`;
      affordFill.classList.toggle('afford-bar-fill--ready', canAfford);
    }

    if (organismGraftRefs.graftRef) {
      const { btn, frameEl, titleEl, pillEl, cdTrack, cdFill } = organismGraftRefs.graftRef;
      const onCd = isGraftOnCooldown(state);
      const ready = canAffordGraft(state);
      btn.disabled = !ready;
      frameEl?.classList.toggle('graft-photo-frame--ready', ready);
      frameEl?.classList.toggle('graft-photo-frame--cooldown', onCd);
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

    if (organismGraftRefs.lungRef) {
      const { btn, frameEl, titleEl, pillEl, cdTrack, cdFill } = organismGraftRefs.lungRef;
      const onCd = isChoirLungOnCooldown(state);
      const ready = canAffordChoirLung(state);
      btn.disabled = !ready;
      frameEl?.classList.toggle('graft-photo-frame--ready', ready);
      frameEl?.classList.toggle('graft-photo-frame--cooldown', onCd);
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

  function buildJarsAndGraftPanel(state) {
    els.deckContent.innerHTML = '';
    const jarRows = [];
    const visibleJars = SPECIMEN_JAR_LIST.filter((jar) => isJarUnlocked(state, jar));
    for (const jar of visibleJars) {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'deck-row';
      btn.dataset.jarId = jar.id;
      btn.innerHTML = `
        <span class="jar-photo-frame" data-role="jar-frame" aria-hidden="true">
          <img
            class="jar-photo-emblem"
            src="${jarAssetUrl(jar.id)}"
            alt=""
            draggable="false"
            aria-hidden="true"
          />
        </span>
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
        frameEl: btn.querySelector('[data-role="jar-frame"]'),
        pillEl: btn.querySelector('[data-role="pill"]'),
      });
    }

    jarsPanelRefs = { jarRows };
  }

  function updateJarsAndGraftInPlace(state) {
    if (!jarsPanelRefs) return;
    for (const row of jarsPanelRefs.jarRows) {
      const { jar, btn, frameEl, pillEl } = row;
      const owned = state.ownedJars.includes(jar.id);
      const cost = effectiveJarCost(state, jar);
      const canAfford = !owned && state.stillness >= cost;
      btn.disabled = !canAfford;
      btn.classList.toggle('deck-row--accent', owned);
      frameEl.classList.toggle('jar-photo-frame--sealed', owned);
      frameEl.classList.toggle('jar-photo-frame--affordable', canAfford);
      pillEl.textContent = owned ? 'SEALED' : `BOTTLE · ${formatInteger(cost)} STILL`;
    }
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
      <button type="button" class="excision-btn excision-btn--arm" data-role="scar-suture-btn">SUTURE</button>
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
    repeatTitle.textContent = 'LAYERED SURGICAL GRAFTS';
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
    repeatSection.append(repeatTitle, repeatList);
    els.deckContent.appendChild(repeatSection);

    scarPanelRefs = {
      summary,
      nodeRefs,
      nameEl: detail.querySelector('[data-role="scar-detail-name"]'),
      costEl: detail.querySelector('[data-role="scar-detail-cost"]'),
      descEl: detail.querySelector('[data-role="scar-detail-desc"]'),
      actionBtn,
      repeatSection,
      repeatableRefs,
    };

    updateScarTreeInPlace(state);
  }

  function updateScarTreeInPlace(state) {
    if (!scarPanelRefs) return;
    const totalIchorMult =
      scarMultiplier(state) * relicIchorMultiplier(state) * dossierIchorMultiplier(state);
    const lastExcision = lastClearedExcisionName(state);
    const cyclePrefix = lastExcision
      ? `CYCLE ${String(state.cycle).padStart(2, '0')} · ${lastExcision}`
      : `CYCLE ${String(state.cycle).padStart(2, '0')}`;
    scarPanelRefs.summary.textContent = `${cyclePrefix} · AVAIL SCAR: ${formatInteger(state.availableScarTissue)} (${formatInteger(state.scarTissue)} TOTAL) · ×${formatMultiplier(totalIchorMult)} ICHOR · ×${formatMultiplier(scarStillnessMultiplier(state))} STILL`;

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
    scarPanelRefs.repeatSection.hidden = !repeatablesOpen;
    if (repeatablesOpen) {
      for (const { graft, btn, titleEl, pillEl } of scarPanelRefs.repeatableRefs) {
        const rank = repeatableRank(state, graft);
        const cost = repeatableCost(state, graft);
        const affordable = canBuyRepeatableGraft(state, graft);
        btn.disabled = !affordable;
        btn.classList.toggle('deck-row--accent', rank > 0);
        titleEl.textContent = `${graft.name} [${rank}]`;
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
            <span class="rite-title">FLOOR DRAIN GRATE</span>
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
          <span class="relic-photo-frame" data-role="relic-frame" aria-hidden="true">
            <img
              class="relic-photo-emblem"
              src="${relicAssetUrl(relic.id)}"
              alt=""
              draggable="false"
              aria-hidden="true"
            />
          </span>
          <span>${relic.name}</span>
        `;
        chip.addEventListener('click', () => {
          const opening = selectedRelicId !== relic.id;
          if (opening) handlers.onInspectRelic?.();
          selectedRelicId = opening ? relic.id : null;
          handlers.onRequestRender();
        });
        chipRow.appendChild(chip);
        relicChips.push({
          relic,
          chip,
          frameEl: chip.querySelector('[data-role="relic-frame"]'),
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
            <span class="rite-title">NEIGHBOR APERTURE</span>
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
            <span class="rite-title">VALVE CLAMP</span>
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
            <span class="rite-title">THE OSSIFIED THROAT</span>
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
        entry.frameEl?.classList.toggle('is-dredged', dredged);
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
        <div class="file-zero-kicker">PLEAD FOR SALVATION</div>
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
        ? 'SEAM READY — CUTTING OPENS THIS SEAM'
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
    const lastExcision = lastClearedExcisionName(state);
    cycleLine.textContent = lastExcision
      ? `CYCLE ${String(state.cycle).padStart(2, '0')} · ${lastExcision} · TOTAL SCAR ${formatInteger(state.scarTissue)}`
      : `CYCLE ${String(state.cycle).padStart(2, '0')} · TOTAL SCAR ${formatInteger(state.scarTissue)}`;
    resonanceLine.textContent = `RESONANCE: ×${formatMultiplier(totalIchorMult)} ICHOR · ×${formatMultiplier(scarStillnessMultiplier(state))} STILLNESS`;
    milestoneLine.hidden = !nextM;
    if (nextM) {
      milestoneLine.textContent = `${nextM.code} · ${formatInteger(cycleLifetimeIchorDisplay(state))} / ${formatInteger(nextM.massThreshold)}`;
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
          ? `SEVER: ${nextM.code} (+${formatInteger(pending)} SCAR)`
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
    markTabSeen(seenArc1Keys, activeTab, arc1UnlockKeys(state, activeTab));

    const visibleCreatures = CREATURE_LIST.filter((c) => isCreatureUnlocked(state, c));
    const visibleJars = SPECIMEN_JAR_LIST.filter((j) => isJarUnlocked(state, j));

    const signature = [
      availableTabs.join(','),
      activeTab,
      visibleCreatures.map((c) => c.id).join(','),
      visibleJars.map((j) => j.id).join(','),
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
      renderDeckTabs(state, availableTabs, activeTab);
      if (activeTab === 'apparatus') {
        buildOrganismCards(state, visibleCreatures);
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

    updateDeckTabAlerts(state, availableTabs, activeTab);

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

  return {
    render(state) {
      renderTelemetry(state);
      renderChamberHud(state);
      renderChamberJarShelves(state);
      syncAnomalies(state);
      renderDeck(state);
    },
    resetViewState() {
      selectedTab = 'apparatus';
      selectedScarNodeId = 'first_incision';
      selectedRelicId = null;
      excisionArmed = false;
      lastDeckSignature = '';
      lastJarShelfSignature = '';
      lastCounterDisplay = -1;
      seenArc1Keys.clear();
      organismCardRefs = [];
      organismGraftRefs = { graftRef: null, lungRef: null };
      jarsPanelRefs = null;
      scarPanelRefs = null;
      ritesPanelRefs = null;
      excisionRefs = null;
    },
  };
}
