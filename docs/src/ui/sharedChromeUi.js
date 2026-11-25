/**
 * Shared multi-arc UI chrome: Arc 3 Convergence summary, Grout/Case Ledger,
 * Recovered Notes Archive modal, and floating tap residue pills.
 */

import { formatInteger } from '../engine.js?v=3.11';
import { lastClearedExcisionName } from './arc1ChamberUi.js?v=3.11';

export function createSharedChromeUi(els, drippingMargin) {
  let lastDialogueId = null;
  let hasHydratedDialogue = false;
  let residueSeq = 0;
  let ledgerPulseTimer = null;

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

  function renderArc3(state) {
    const lastExcision = lastClearedExcisionName(state);
    const parts = [`SCAR TISSUE: ${formatInteger(state.scarTissue)}`];
    if (lastExcision) parts.push(lastExcision);
    parts.push(`DOSSIER: ${state.unredactedDossierLines.length}/4`);
    parts.push(`RECOVERED NOTES: ${state.discoveredDialogues.length}`);
    els.arc3Stats.textContent = parts.join(' · ');
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
        drippingMargin.triggerStoryDrip(entry);
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
    renderArc3,
    renderLedger,
    openArchive(state) {
      renderArchive(state);
      els.archiveModal.hidden = false;
    },
    closeArchive() {
      els.archiveModal.hidden = true;
    },
    spawnResidueIn,
    resetViewState() {
      lastDialogueId = null;
      hasHydratedDialogue = false;
    },
  };
}
