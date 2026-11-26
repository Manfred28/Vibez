/**
 * Arc 2 Investigator Desk DOM renderer (bespoke photographic edition):
 * Diegetic Case Angle header, Brass Lamp On/Off pull-chain, circular feathered macro Desk Anomalies,
 * File #00 reveal, and 4-tab deck: CONTACTS (with Bulk Warrant), BOARD (interactive photographic
 * Red-String Corkboard with Polaroids & brass pushpins), DOSSIER (Silence Exhibits with macro thumbs,
 * Acoustic Reel Scrubbing, 4 Redactions), and REVISION (Dead-End Pivot).
 */

import {
  AUDIT_REDACTIONS,
  CASE_EXHIBITS,
  DESK_ANOMALY_TYPES,
  INVESTIGATOR_CONTACTS,
  arc2ActiveCorroborations,
  arc2CanDissolveRedaction,
  arc2CanPivotNextCase,
  arc2CanTriggerReelScrub,
  arc2CanUnlockExhibit,
  arc2CaseProgressFraction,
  arc2ContactCost,
  arc2ContactCount,
  arc2CurrentCase,
  arc2EffectiveContactRate,
  arc2InspectYield,
  arc2IsBulkWarrantUnlocked,
  arc2IsContactUnlocked,
  arc2IsRedactionSolventUnlocked,
  arc2IsReelScrubUnlocked,
  arc2IsReelScrubbing,
  arc2IsRevisionTabVisible,
  arc2IsSilenceActive,
  arc2LeadsPerSecond,
  arc2MaxAffordableContacts,
  arc2MaxStringSpools,
  arc2SilencePerSecond,
  arc2SpeculativeThreadCount,
  exhibitById,
  formatCompactDecimal,
  formatInteger,
  matchCorroborationPair,
} from '../engine.js?v=3.14';
import { deskAnomalyAssetUrl, exhibitAssetUrl } from '../version.js?v=3.14';
import { arc2UnlockKeys, markTabSeen, shouldAlertArc2Tab } from './tabAlerts.js?v=3.14';

export function createArc2InvestigatorUi(els, handlers) {
  let selectedArc2Tab = 'contacts';
  let selectedExhibitId = null;
  let lastArc2DeckSignature = '';
  const seenArc2Keys = new Map();
  const arc2TabButtons = new Map();
  const deskAnomalyNodeMap = new Map();
  let arc2DeckRefs = null;

  function syncDeskAnomalies(state) {
    const list = state.arc2.activeDeskAnomalies ?? [];
    const activeIds = new Set(list.map((a) => String(a.id)));
    for (const child of Array.from(els.deskAnomalyLayer.children)) {
      const id = child.dataset.anomalyId;
      if (!activeIds.has(id)) {
        deskAnomalyNodeMap.delete(id);
        child.remove();
      }
    }

    for (const anomaly of list) {
      const key = String(anomaly.id);
      if (deskAnomalyNodeMap.has(key)) continue;
      const meta = Object.values(DESK_ANOMALY_TYPES).find((t) => t.id === anomaly.type);
      if (!meta) continue;

      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = `anomaly-node anomaly-node--desk anomaly-node--${anomaly.type}`;
      btn.dataset.anomalyId = key;
      btn.dataset.anomalyType = anomaly.type;
      const clampedX = Math.max(12, Math.min(88, anomaly.xFraction * 100));
      const clampedY = Math.max(18, Math.min(84, anomaly.yFraction * 100));
      btn.style.left = `${clampedX.toFixed(1)}%`;
      btn.style.top = `${clampedY.toFixed(1)}%`;
      const phaseOffsetSec = -((Math.abs(Number(anomaly.id) || 1) * 0.93) % 4.6);
      btn.style.setProperty('--breath-delay', `${phaseOffsetSec.toFixed(2)}s`);
      btn.setAttribute('aria-label', meta.ariaLabel);

      const img = document.createElement('img');
      img.className = 'anomaly-photo';
      img.src = deskAnomalyAssetUrl(anomaly.type);
      img.alt = '';
      img.draggable = false;
      img.setAttribute('aria-hidden', 'true');

      const label = document.createElement('span');
      label.className = 'anomaly-label';
      label.textContent = meta.label;

      btn.append(img, label);
      btn.addEventListener('click', () => handlers.onInteractDeskAnomaly(anomaly.id));
      els.deskAnomalyLayer.appendChild(btn);
      deskAnomalyNodeMap.set(key, { btn, type: anomaly.type });
    }
  }

  function updateArc2TabAlerts(state) {
    for (const [tabId, btn] of arc2TabButtons.entries()) {
      const alerting = shouldAlertArc2Tab(state, tabId, selectedArc2Tab, seenArc2Keys);
      btn.classList.toggle('deck-tab--alert', alerting);
      btn.dataset.tabAlert = alerting ? 'true' : 'false';
    }
  }

  function drawCorkboardStrings(stageEl, canvasEl, wiredPairs) {
    if (!stageEl || !canvasEl) return;
    const rect = stageEl.getBoundingClientRect();
    const width = Math.max(260, Math.round(rect.width || 320));
    const height = Math.max(160, Math.round(rect.height || 180));
    if (canvasEl.width !== width || canvasEl.height !== height) {
      canvasEl.width = width;
      canvasEl.height = height;
    }
    const ctx = canvasEl.getContext('2d');
    if (!ctx) return;
    ctx.clearRect(0, 0, width, height);

    const cards = new Map();
    for (const card of stageEl.querySelectorAll('[data-exhibit-id]')) {
      const cr = card.getBoundingClientRect();
      cards.set(card.dataset.exhibitId, {
        x: cr.left - rect.left + cr.width * 0.5,
        y: cr.top - rect.top + 11,
      });
    }

    ctx.lineCap = 'round';
    for (const pairKey of wiredPairs) {
      const [aId, bId] = pairKey.split('|');
      const a = cards.get(aId);
      const b = cards.get(bId);
      if (!a || !b) continue;
      const isCurated = Boolean(matchCorroborationPair(pairKey));
      const midX = (a.x + b.x) * 0.5;
      const midY = Math.max(a.y, b.y) + 16;

      // Soft shadow beneath taut cotton thread
      ctx.save();
      ctx.strokeStyle = 'rgba(0, 0, 0, 0.52)';
      ctx.lineWidth = isCurated ? 3.6 : 2.4;
      ctx.beginPath();
      ctx.moveTo(a.x, a.y + 1.5);
      ctx.quadraticCurveTo(midX, midY + 1.5, b.x, b.y + 1.5);
      ctx.stroke();

      ctx.strokeStyle = isCurated ? '#a82d30' : 'rgba(180, 165, 160, 0.55)';
      ctx.lineWidth = isCurated ? 2.1 : 1.4;
      if (!isCurated) ctx.setLineDash([6, 5]);
      ctx.beginPath();
      ctx.moveTo(a.x, a.y);
      ctx.quadraticCurveTo(midX, midY, b.x, b.y);
      ctx.stroke();
      ctx.restore();
    }
  }

  function buildBoardDom(desk) {
    const pinnedSet = new Set(desk.pinnedExhibits ?? []);
    const wiredPairs = desk.wiredPairs ?? [];
    const activeCorr = arc2ActiveCorroborations(desk);

    const wrap = document.createElement('div');
    wrap.className = 'corkboard-wrap';

    const header = document.createElement('div');
    header.className = 'corkboard-meta';
    header.textContent = `SPOOLS: ${wiredPairs.length}/${arc2MaxStringSpools(desk)} · CORROBORATIONS: ${activeCorr.size} ACTIVE (${desk.discoveredCorroborations?.length ?? 0}/10 LOGGED)`;
    wrap.appendChild(header);

    const hint = document.createElement('div');
    hint.className = 'corkboard-hint';
    hint.textContent = selectedExhibitId
      ? `SELECTED: ${exhibitById(selectedExhibitId)?.shortLabel ?? selectedExhibitId} — TAP ANOTHER EXHIBIT TO WIRE OR UNWIRE`
      : 'TAP TWO PINNED EXHIBITS TO STRING A CONNECTION. NON-MATCHED PAIRS GIVE +15% SPECULATIVE THREAD.';
    wrap.appendChild(hint);

    const stage = document.createElement('div');
    stage.className = 'corkboard-stage';
    const canvas = document.createElement('canvas');
    canvas.className = 'corkboard-strings-canvas';
    canvas.setAttribute('aria-hidden', 'true');
    const grid = document.createElement('div');
    grid.className = 'corkboard-grid';

    const pinnedList = CASE_EXHIBITS.filter((e) => pinnedSet.has(e.id));
    for (const ex of pinnedList) {
      const card = document.createElement('button');
      card.type = 'button';
      card.className = 'exhibit-card';
      card.dataset.exhibitId = ex.id;
      if (selectedExhibitId === ex.id) card.classList.add('is-selected');
      const wireCount = wiredPairs.filter((p) => p.split('|').includes(ex.id)).length;
      if (wireCount > 0) card.classList.add('is-wired');

      const pinHead = document.createElement('span');
      pinHead.className = 'exhibit-pin-head';
      pinHead.setAttribute('aria-hidden', 'true');

      const frame = document.createElement('span');
      frame.className = 'exhibit-polaroid-frame';
      const img = document.createElement('img');
      img.className = 'exhibit-polaroid-img';
      img.src = exhibitAssetUrl(ex.id);
      img.alt = ex.title;
      img.loading = 'lazy';
      img.draggable = false;
      frame.appendChild(img);

      const metaRow = document.createElement('span');
      metaRow.className = 'exhibit-meta-row';
      const code = document.createElement('span');
      code.className = 'exhibit-code';
      code.textContent = ex.shortLabel;
      metaRow.appendChild(code);
      if (wireCount > 0) {
        const wireBadge = document.createElement('span');
        wireBadge.className = 'exhibit-wire-badge';
        wireBadge.textContent = `×${wireCount}`;
        metaRow.appendChild(wireBadge);
      }

      const title = document.createElement('span');
      title.className = 'exhibit-title';
      title.textContent = ex.title;

      card.append(pinHead, frame, metaRow, title);
      card.addEventListener('click', () => {
        if (!selectedExhibitId) {
          selectedExhibitId = ex.id;
          lastArc2DeckSignature = '';
          handlers.onRequestRender();
        } else if (selectedExhibitId === ex.id) {
          selectedExhibitId = null;
          lastArc2DeckSignature = '';
          handlers.onRequestRender();
        } else {
          const first = selectedExhibitId;
          selectedExhibitId = null;
          lastArc2DeckSignature = '';
          handlers.onToggleExhibitPair(first, ex.id);
        }
      });
      grid.appendChild(card);
    }

    stage.append(canvas, grid);
    wrap.appendChild(stage);

    if (wiredPairs.length > 0) {
      const list = document.createElement('div');
      list.className = 'corkboard-pair-list';
      for (const pairKey of wiredPairs) {
        const [aId, bId] = pairKey.split('|');
        const exA = exhibitById(aId);
        const exB = exhibitById(bId);
        const matched = matchCorroborationPair(pairKey);
        const row = document.createElement('button');
        row.type = 'button';
        row.className = 'deck-row';
        row.dataset.unwirePair = pairKey;
        row.innerHTML = `
          <span class="deck-row-text">
            <span class="deck-row-title">${matched ? matched.title.toUpperCase() : 'SPECULATIVE THREAD (+15%)'}</span>
            <span class="deck-row-sub">${exA?.shortLabel} + ${exB?.shortLabel} · ${matched ? matched.effectSummary : '+15% Leads & Silence'}</span>
          </span>
          <span class="deck-row-pill">UNWIRE</span>
        `;
        row.addEventListener('click', () => handlers.onToggleExhibitPair(aId, bId));
        list.appendChild(row);
      }
      wrap.appendChild(list);
    }

    const specCount = arc2SpeculativeThreadCount(desk);
    if (specCount > 0) {
      const specNote = document.createElement('div');
      specNote.className = 'locked-hint';
      specNote.textContent = `SPECULATIVE THREADS ACTIVE: ${specCount} (+${specCount * 15}% ALL RATES)`;
      wrap.appendChild(specNote);
    }

    els.arc2DeckContent.appendChild(wrap);
    window.requestAnimationFrame(() => drawCorkboardStrings(stage, canvas, wiredPairs));
    return { tab: 'board', stage, canvas };
  }

  function renderArc2Deck(state) {
    const desk = state.arc2;
    const revVisible = arc2IsRevisionTabVisible(desk);
    if (!revVisible && selectedArc2Tab === 'revision') {
      selectedArc2Tab = 'contacts';
    }
    markTabSeen(seenArc2Keys, selectedArc2Tab, arc2UnlockKeys(state, selectedArc2Tab));

    const sig = [
      selectedArc2Tab,
      `c${desk.caseAngle}`,
      `p${desk.completedCasePivots}`,
      `ex${(desk.pinnedExhibits ?? []).join(',')}`,
      `wp${(desk.wiredPairs ?? []).join(',')}`,
      `red${(desk.dissolvedRedactions ?? []).join(',')}`,
      `sel${selectedExhibitId ?? ''}`,
      `rev${revVisible ? 1 : 0}`,
      `f0${desk.fileZeroRevealed ? 1 : 0}`,
    ].join('|');

    if (sig !== lastArc2DeckSignature) {
      lastArc2DeckSignature = sig;
      const tabs = [
        { id: 'contacts', label: 'CONTACTS' },
        { id: 'board', label: 'BOARD' },
        { id: 'dossier', label: 'DOSSIER' },
      ];
      if (revVisible) tabs.push({ id: 'revision', label: 'REVISION' });

      els.arc2DeckTabs.innerHTML = '';
      arc2TabButtons.clear();
      for (const t of tabs) {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.role = 'tab';
        btn.className = 'deck-tab';
        btn.dataset.tabId = t.id;
        if (t.id === selectedArc2Tab) btn.classList.add('is-active');
        btn.setAttribute('aria-selected', String(t.id === selectedArc2Tab));
        const pip = document.createElement('span');
        pip.className = 'deck-tab-pip';
        pip.setAttribute('aria-hidden', 'true');
        const labelSpan = document.createElement('span');
        labelSpan.textContent = t.label;
        btn.append(pip, labelSpan);
        btn.addEventListener('click', () => {
          selectedArc2Tab = t.id;
          selectedExhibitId = null;
          markTabSeen(seenArc2Keys, t.id, arc2UnlockKeys(state, t.id));
          lastArc2DeckSignature = '';
          handlers.onRequestRender();
        });
        els.arc2DeckTabs.appendChild(btn);
        arc2TabButtons.set(t.id, btn);
      }

      els.arc2DeckContent.innerHTML = '';

      if (selectedArc2Tab === 'contacts') {
        const rows = [];
        for (const c of INVESTIGATOR_CONTACTS) {
          if (!arc2IsContactUnlocked(desk, c)) continue;
          const wrap = document.createElement('div');
          wrap.className = 'contact-card';
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
          wrap.appendChild(btn);

          let warrantBtn = null;
          if (arc2IsBulkWarrantUnlocked(desk)) {
            warrantBtn = document.createElement('button');
            warrantBtn.type = 'button';
            warrantBtn.className = 'proliferate-btn';
            warrantBtn.dataset.warrantContactId = c.id;
            warrantBtn.innerHTML = `
              <span class="proliferate-label" data-role="warrant-label">WARRANT ×0</span>
              <span class="proliferate-cost" data-role="warrant-cost"></span>
            `;
            warrantBtn.addEventListener('click', () => handlers.onRecruitMaxContact(c.id));
            wrap.appendChild(warrantBtn);
          }

          els.arc2DeckContent.appendChild(wrap);
          rows.push({
            spec: c,
            btn,
            warrantBtn,
            titleEl: btn.querySelector('[data-role="title"]'),
            rateEl: btn.querySelector('[data-role="rate"]'),
            costEl: btn.querySelector('[data-role="cost"]'),
            warrantLabelEl: warrantBtn?.querySelector('[data-role="warrant-label"]'),
            warrantCostEl: warrantBtn?.querySelector('[data-role="warrant-cost"]'),
          });
        }
        arc2DeckRefs = { tab: 'contacts', rows };
      } else if (selectedArc2Tab === 'board') {
        arc2DeckRefs = buildBoardDom(desk);
      } else if (selectedArc2Tab === 'dossier') {
        const exhibitRows = [];
        for (const ex of CASE_EXHIBITS) {
          if (ex.isKeystone || (desk.caseAngle ?? 1) < ex.unlockCaseAngle) continue;
          const btn = document.createElement('button');
          btn.type = 'button';
          btn.className = 'deck-row';
          btn.dataset.exhibitUnlockId = ex.id;

          const thumbFrame = document.createElement('span');
          thumbFrame.className = 'exhibit-dossier-frame';
          const thumbImg = document.createElement('img');
          thumbImg.className = 'exhibit-dossier-img';
          thumbImg.src = exhibitAssetUrl(ex.id);
          thumbImg.alt = ex.title;
          thumbImg.loading = 'lazy';
          thumbImg.draggable = false;
          thumbFrame.appendChild(thumbImg);

          const textWrap = document.createElement('span');
          textWrap.className = 'deck-row-text';
          textWrap.innerHTML = `
            <span class="deck-row-title">${ex.shortLabel} · ${ex.title.toUpperCase()}</span>
            <span class="deck-row-sub">${ex.caption}</span>
          `;

          const pill = document.createElement('span');
          pill.className = 'deck-row-pill';
          pill.dataset.role = 'pill';

          btn.append(thumbFrame, textWrap, pill);
          btn.addEventListener('click', () => handlers.onUnlockExhibit(ex.id));
          els.arc2DeckContent.appendChild(btn);
          exhibitRows.push({ ex, btn, thumbFrame, pillEl: pill });
        }

        let scrubBtn = null;
        let scrubSub = null;
        let scrubPill = null;
        if (arc2IsReelScrubUnlocked(desk)) {
          const div = document.createElement('div');
          div.className = 'deck-divider';
          els.arc2DeckContent.appendChild(div);
          scrubBtn = document.createElement('button');
          scrubBtn.type = 'button';
          scrubBtn.className = 'deck-row';
          scrubBtn.dataset.role = 'reel-scrub-btn';
          scrubBtn.innerHTML = `
            <span class="deck-row-text">
              <span class="deck-row-title">ACOUSTIC REEL SCRUBBING</span>
              <span class="deck-row-sub" data-role="scrub-sub">Switch the Lamp Off to scrub Reel 4 for heightened Silence.</span>
            </span>
            <span class="deck-row-pill" data-role="scrub-pill">SCRUB</span>
          `;
          scrubBtn.addEventListener('click', () => handlers.onTriggerReelScrub());
          els.arc2DeckContent.appendChild(scrubBtn);
          scrubSub = scrubBtn.querySelector('[data-role="scrub-sub"]');
          scrubPill = scrubBtn.querySelector('[data-role="scrub-pill"]');
        }

        const redactionRows = [];
        if (arc2IsRedactionSolventUnlocked(desk)) {
          const div = document.createElement('div');
          div.className = 'deck-divider';
          els.arc2DeckContent.appendChild(div);
          for (const r of AUDIT_REDACTIONS) {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'deck-row';
            btn.dataset.redactionId = r.id;
            btn.innerHTML = `
              <span class="deck-row-text">
                <span class="deck-row-title">${r.label}</span>
                <span class="deck-row-sub" data-role="red-text"></span>
                <span class="organism-yield">${r.effectSummary}</span>
              </span>
              <span class="deck-row-pill" data-role="red-pill"></span>
            `;
            btn.addEventListener('click', () => handlers.onDissolveRedaction(r.id));
            els.arc2DeckContent.appendChild(btn);
            redactionRows.push({
              r,
              btn,
              textEl: btn.querySelector('[data-role="red-text"]'),
              pillEl: btn.querySelector('[data-role="red-pill"]'),
            });
          }
        }
        arc2DeckRefs = { tab: 'dossier', exhibitRows, scrubBtn, scrubSub, scrubPill, redactionRows };
      } else {
        const panel = document.createElement('div');
        panel.className = 'excision-panel';
        panel.innerHTML = `
          <h3 class="excision-title" data-role="rev-title"></h3>
          <p class="excision-summary" data-role="rev-sub"></p>
          <div class="checklist-box" data-role="rev-checklist"></div>
          <button type="button" class="excision-btn excision-btn--confirm" data-role="rev-pivot-btn"></button>
        `;
        const pivotBtn = panel.querySelector('[data-role="rev-pivot-btn"]');
        pivotBtn.addEventListener('click', () => handlers.onPivotNextCase());
        els.arc2DeckContent.appendChild(panel);
        arc2DeckRefs = {
          tab: 'revision',
          titleEl: panel.querySelector('[data-role="rev-title"]'),
          subEl: panel.querySelector('[data-role="rev-sub"]'),
          checklistEl: panel.querySelector('[data-role="rev-checklist"]'),
          pivotBtn,
        };
      }
    }

    updateArc2TabAlerts(state);
    if (!arc2DeckRefs) return;

    if (arc2DeckRefs.tab === 'contacts') {
      for (const row of arc2DeckRefs.rows) {
        const count = arc2ContactCount(desk, row.spec.id);
        const cost = arc2ContactCost(desk, row.spec);
        row.btn.disabled = desk.leads < cost;
        row.titleEl.textContent = `${row.spec.name} [${count}]`;
        row.rateEl.textContent = `EACH: +${formatCompactDecimal(arc2EffectiveContactRate(desk, row.spec))}/s`;
        row.costEl.textContent = `${formatInteger(cost)} LEADS`;
        if (row.warrantBtn) {
          const { count: bulkCount, totalCost } = arc2MaxAffordableContacts(desk, row.spec);
          row.warrantBtn.disabled = bulkCount <= 0;
          row.warrantLabelEl.textContent = `WARRANT ×${bulkCount}`;
          row.warrantCostEl.textContent =
            bulkCount > 0 ? `${formatInteger(totalCost)} LEADS` : 'INSUFFICIENT LEADS';
        }
      }
    } else if (arc2DeckRefs.tab === 'dossier') {
      const pinned = new Set(desk.pinnedExhibits ?? []);
      for (const { ex, btn, thumbFrame, pillEl } of arc2DeckRefs.exhibitRows) {
        const owned = pinned.has(ex.id);
        const canUnlock = arc2CanUnlockExhibit(desk, ex);
        btn.disabled = owned || !canUnlock;
        btn.classList.toggle('deck-row--accent', owned);
        thumbFrame.classList.toggle('is-pinned', owned);
        thumbFrame.classList.toggle('is-affordable', !owned && canUnlock);
        pillEl.textContent = owned ? 'PINNED' : `PIN · ${formatInteger(ex.silenceCost)} SILENCE`;
      }
      if (arc2DeckRefs.scrubBtn) {
        const active = arc2IsReelScrubbing(desk);
        const cd = desk.reelScrubCooldownSeconds ?? 0;
        arc2DeckRefs.scrubBtn.disabled = !arc2CanTriggerReelScrub(desk);
        arc2DeckRefs.scrubBtn.classList.toggle('deck-row--accent', active);
        if (active) {
          arc2DeckRefs.scrubSub.textContent = `Scrubbing Reel 4 in After-Hours Silence (${Math.ceil(desk.reelScrubRemainingSeconds)}s left).`;
          arc2DeckRefs.scrubPill.textContent = `${Math.ceil(desk.reelScrubRemainingSeconds)}s`;
        } else if (cd > 0) {
          arc2DeckRefs.scrubSub.textContent = `Tape capstan cooling down (${Math.ceil(cd)}s).`;
          arc2DeckRefs.scrubPill.textContent = `WAIT ${Math.ceil(cd)}s`;
        } else if (desk.lampOn) {
          arc2DeckRefs.scrubSub.textContent = 'Pull the desk lamp off first to listen to Reel 4.';
          arc2DeckRefs.scrubPill.textContent = 'LAMP OFF REQ';
        } else {
          arc2DeckRefs.scrubSub.textContent = '24s burst of heightened After-Hours Silence & haunted spawns.';
          arc2DeckRefs.scrubPill.textContent = 'SCRUB REEL';
        }
      }
      const dissolved = new Set(desk.dissolvedRedactions ?? []);
      for (const { r, btn, textEl, pillEl } of arc2DeckRefs.redactionRows) {
        const done = dissolved.has(r.id);
        btn.disabled = done || !arc2CanDissolveRedaction(desk, r);
        btn.classList.toggle('deck-row--accent', done);
        textEl.textContent = done
          ? r.revealedText
          : desk.lampOn
            ? '[BLACKED OUT — TURN LAMP OFF TO APPLY SOLVENT]'
            : '[BLACKED OUT — READY FOR SILENCE SOLVENT]';
        pillEl.textContent = done ? 'DISSOLVED' : `${formatInteger(r.silenceCost)} SILENCE`;
      }
    } else if (arc2DeckRefs.tab === 'revision') {
      const c = arc2CurrentCase(desk);
      arc2DeckRefs.titleEl.textContent = desk.fileZeroRevealed
        ? 'FILE #00 — UNSEALED'
        : c.title;
      arc2DeckRefs.subEl.textContent = desk.fileZeroRevealed
        ? 'The blotter has curled back from wet tile. Step past the seam above.'
        : 'Filing a Dead End clears Leads and Contacts, pins the next Keystone Exhibit, and adds +1 Red String Spool.';

      const checks = [
        {
          ok: desk.leads >= c.leadsCost,
          text: `LEADS: ${formatInteger(desk.leads)} / ${formatInteger(c.leadsCost)}`,
        },
        {
          ok: desk.silence >= c.silenceCost,
          text: `SILENCE: ${formatInteger(desk.silence)} / ${formatInteger(c.silenceCost)}`,
        },
        {
          ok: (desk.discoveredCorroborations?.length ?? 0) >= c.requiredCorroborations,
          text: `CORROBORATIONS LOGGED: ${desk.discoveredCorroborations?.length ?? 0} / ${c.requiredCorroborations}`,
        },
      ];
      if (c.requiredReelScrubs > 0) {
        checks.push({
          ok: (desk.reelScrubCount ?? 0) >= c.requiredReelScrubs,
          text: `REEL 4 SCRUBBED: ${desk.reelScrubCount ?? 0} / ${c.requiredReelScrubs}`,
        });
      }
      if (c.requiredRedactions > 0) {
        checks.push({
          ok: (desk.dissolvedRedactions?.length ?? 0) >= c.requiredRedactions,
          text: `REDACTIONS DISSOLVED: ${desk.dissolvedRedactions?.length ?? 0} / ${c.requiredRedactions}`,
        });
      }
      arc2DeckRefs.checklistEl.innerHTML = checks
        .map(
          (item) =>
            `<div class="checklist-row ${item.ok ? 'is-ok' : ''}">${item.ok ? '[OK]' : '[  ]'} ${item.text}</div>`,
        )
        .join('');

      arc2DeckRefs.pivotBtn.hidden = Boolean(desk.fileZeroRevealed);
      arc2DeckRefs.pivotBtn.disabled = !arc2CanPivotNextCase(desk);
      arc2DeckRefs.pivotBtn.textContent =
        c.number === 5 ? 'UNSEAL FILE #00' : 'FILE DEAD END & REOPEN CASE';
    }
  }

  return {
    render(state) {
      const desk = state.arc2;
      const curCase = arc2CurrentCase(desk);
      const pct = Math.round(arc2CaseProgressFraction(desk) * 100);

      els.arc2Header.textContent = desk.fileZeroRevealed
        ? 'FILE #00 — SUBJECT INTAKE'
        : curCase.title;
      if (els.arc2CaseSubtitle) {
        els.arc2CaseSubtitle.textContent = desk.fileZeroRevealed
          ? 'Uncatalogued folder open beneath the desk lamp'
          : curCase.subtitle;
      }
      if (els.arc2CaseProgressFill) {
        els.arc2CaseProgressFill.style.width = `${pct}%`;
      }

      const pivotReady = arc2CanPivotNextCase(desk) || desk.fileZeroRevealed;
      els.arc2ChapterStatus.hidden = !pivotReady;
      els.arc2ChapterStatus.classList.toggle('threshold-status--ready', pivotReady);
      if (desk.fileZeroRevealed) {
        els.arc2ChapterStatus.textContent = 'FILE #00 UNSEALED // SEAM OPEN';
      } else if (arc2CanPivotNextCase(desk)) {
        els.arc2ChapterStatus.textContent =
          curCase.number === 5
            ? 'AUDIT COMPLETE · OPEN REVISION TO UNSEAL FILE #00'
            : 'DEAD END REACHED · OPEN REVISION TO REOPEN CASE';
      }

      els.arc2LeadsValue.textContent = formatInteger(desk.leads);
      const rate = arc2LeadsPerSecond(desk);
      els.arc2LeadsRate.hidden = rate <= 0;
      if (rate > 0) els.arc2LeadsRate.textContent = `${formatCompactDecimal(rate)} /s`;

      const silRate = arc2SilencePerSecond(desk);
      const silent = arc2IsSilenceActive(desk);
      els.arc2SilenceText.textContent =
        silRate > 0
          ? `SILENCE: ${formatInteger(desk.silence)} (+${formatCompactDecimal(silRate)}/s)`
          : `SILENCE: ${formatInteger(desk.silence)}`;
      els.arc2SilenceText.classList.toggle('status-tag--stillness-active', silent);
      els.arc2SilenceBadge.hidden = !silent;
      if (els.arc2ScrubBadge) {
        const scrubbing = arc2IsReelScrubbing(desk);
        els.arc2ScrubBadge.hidden = !scrubbing;
        if (scrubbing) {
          els.arc2ScrubBadge.textContent = `REEL SCRUB (${Math.ceil(desk.reelScrubRemainingSeconds)}s)`;
        }
      }
      els.arc2FocusBadge.hidden = (desk.focusRemainingSeconds ?? 0) <= 0;
      if ((desk.focusRemainingSeconds ?? 0) > 0) {
        els.arc2FocusBadge.textContent = `FOCUS ×1.8 (${Math.ceil(desk.focusRemainingSeconds)}s)`;
      }

      if (els.lampToggleBtn) {
        els.lampToggleBtn.classList.toggle('lamp-toggle-btn--off', !desk.lampOn);
        els.lampToggleBtn.setAttribute('aria-pressed', desk.lampOn ? 'true' : 'false');
        els.lampToggleBtn.textContent = desk.lampOn
          ? 'LAMP: ON (DAYLIGHT LEADS)'
          : 'LAMP: OFF (AFTER-HOURS SILENCE)';
      }

      els.inspectYieldText.textContent = `+${formatCompactDecimal(arc2InspectYield(desk))}`;
      els.fileZeroReveal.hidden = !desk.fileZeroRevealed;

      syncDeskAnomalies(state);
      renderArc2Deck(state);
    },
    renderAnimatedDeskGlyphs() {
      // Desk anomalies now use GPU-accelerated circular feathered macro photos + CSS breathing keyframes
    },
    resetViewState() {
      selectedArc2Tab = 'contacts';
      selectedExhibitId = null;
      lastArc2DeckSignature = '';
      seenArc2Keys.clear();
      arc2DeckRefs = null;
    },
  };
}
