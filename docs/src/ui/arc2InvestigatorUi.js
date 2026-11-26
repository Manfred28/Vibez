/**
 * Arc 2 Investigator Desk DOM renderer: telemetry header, desk HUD, desk anomalies,
 * File #00 reveal, and Contacts / Deductions / Evidence Board deck.
 */

import {
  CASE_DEDUCTIONS,
  DESK_ANOMALY_TYPES,
  INVESTIGATION_CHAPTERS,
  INVESTIGATOR_CONTACTS,
  arc2CanConnectNextChapter,
  arc2ContactCost,
  arc2ContactCount,
  arc2EffectiveContactRate,
  arc2InspectYield,
  arc2LeadsPerSecond,
  arc2NextChapter,
  formatCompactDecimal,
  formatInteger,
} from '../engine.js?v=3.12';
import { drawDeskAnomalyGlyph } from '../chamberCanvas.js?v=3.12';
import { arc2UnlockKeys, markTabSeen, shouldAlertArc2Tab } from './tabAlerts.js?v=3.12';

export function createArc2InvestigatorUi(els, handlers) {
  let selectedArc2Tab = 'contacts';
  let lastArc2DeckSignature = '';
  const seenArc2Keys = new Map();
  const arc2TabButtons = new Map();
  const deskAnomalyCanvasMap = new Map();
  let arc2DeckRefs = null;

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

  function updateArc2TabAlerts(state) {
    for (const [tabId, btn] of arc2TabButtons.entries()) {
      const alerting = shouldAlertArc2Tab(state, tabId, selectedArc2Tab, seenArc2Keys);
      btn.classList.toggle('deck-tab--alert', alerting);
      btn.dataset.tabAlert = alerting ? 'true' : 'false';
    }
  }

  function renderArc2Deck(state) {
    const desk = state.arc2;
    markTabSeen(seenArc2Keys, selectedArc2Tab, arc2UnlockKeys(state, selectedArc2Tab));
    const sig = `${selectedArc2Tab}|ch${desk.completedChapters}|d${desk.ownedDeductions.length}`;
    if (sig !== lastArc2DeckSignature) {
      lastArc2DeckSignature = sig;
      const tabs = [
        { id: 'contacts', label: 'CONTACTS' },
        { id: 'deductions', label: 'DEDUCTIONS' },
        { id: 'evidence_board', label: 'EVIDENCE BOARD' },
      ];
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
        const visibleChapters = INVESTIGATION_CHAPTERS.filter(
          (ch) => ch.number <= desk.completedChapters + 1,
        );
        for (const ch of visibleChapters) {
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

    updateArc2TabAlerts(state);

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
        statusEl.textContent = done ? 'CONNECTED' : 'ACTIVE LEAD';
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

  return {
    render(state) {
      const desk = state.arc2;
      const compPct = Math.round(desk.complicity * 100);
      els.arc2Header.textContent = desk.fileZeroRevealed
        ? 'FILE #00 — SUBJECT INTAKE'
        : 'THE INQUIRY · CASE #14-B';
      els.arc2ComplicityLabel.textContent = `COMPLICITY: ${compPct}% · SCAR ECHOES: ${formatInteger(state.scarTissue)}`;
      els.arc2ComplicityFill.style.width = `${compPct}%`;

      const nextCh = arc2NextChapter(desk);
      const chapterReady = arc2CanConnectNextChapter(desk) || desk.fileZeroRevealed;
      els.arc2ChapterStatus.hidden = !chapterReady;
      els.arc2ChapterStatus.classList.toggle('threshold-status--ready', chapterReady);
      if (desk.fileZeroRevealed) {
        els.arc2ChapterStatus.textContent = 'FILE #00 UNSEALED // SEAM OPEN';
      } else if (nextCh && arc2CanConnectNextChapter(desk)) {
        els.arc2ChapterStatus.textContent = `LEAD READY: ${nextCh.title}`;
      }

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
    },
    renderAnimatedDeskGlyphs(breath) {
      for (const { canvas, type } of deskAnomalyCanvasMap.values()) {
        drawDeskAnomalyGlyph(canvas, type, breath);
      }
    },
    resetViewState() {
      selectedArc2Tab = 'contacts';
      lastArc2DeckSignature = '';
      seenArc2Keys.clear();
      arc2DeckRefs = null;
    },
  };
}
