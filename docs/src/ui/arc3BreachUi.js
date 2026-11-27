/**
 * Arc 3 ("The Unmaking") DOM controller for the GitHub Pages web version
 * (1:1 feature & narrative parity with Android Arc3ConvergenceScreen + Arc3BreachCanvas + Arc3BreachDeck).
 */

import {
  ARC3_BALANCE,
  ARC3_STRATA,
  ARC3_UNMAKE_ACTS,
  ARC3_UNMAKE_CATEGORIES,
  BREACH_RUPTURE_TYPES,
  SPECIMEN_JAR_LIST,
  arc3ActiveClotPerSec,
  arc3ActiveStratum,
  arc3CanDriveWedge,
  arc3CanPerformFinalPull,
  arc3CanPerformUnmake,
  arc3FloodLevel,
  arc3IsActivelyClotting,
  arc3IsClotFrozen,
  arc3IsInFinale,
  arc3IsJarShattered,
  arc3IsUnmakeVisible,
  arc3IsValveSheared,
  arc3LegacyFractureMultiplier,
  arc3LegacyHemorrhageMultiplier,
  arc3StratumProgress,
  arc3TelemetryBlurFactor,
  arc3WrenchFractureYield,
  formatCompactDecimal,
  formatInteger,
} from '../engine.js?v=3.15';
import { breachRuptureAssetUrl, jarAssetUrl } from '../version.js?v=3.15';
import {
  arc3UnlockKeys,
  markTabSeen,
  shouldAlertArc3Tab,
} from './tabAlerts.js?v=3.15';

const ARC3_TABS = [
  { id: 'unmake', label: 'UNMAKE' },
  { id: 'wedges', label: 'WEDGES' },
];

const FINAL_PULL_LABELS = [
  'WRENCH THE JAMB FREE',
  'SPLIT THE SEALED GROUT',
  'TEAR THE DOORWAY WIDE',
  'PULL THROUGH THE FLOOD',
  'LET THE SEAM GIVE WAY',
];

function formatBleedValue(value) {
  return value >= 1000 ? formatCompactDecimal(value) : formatInteger(value);
}

export function createArc3BreachUi(els, handlers) {
  let activeTab = 'unmake';
  let lastDeckSignature = '';
  let lastShelfSignature = '';
  let lastRuptureSignature = '';
  const seenKeysByTab = new Map();

  function clearDeckCache() {
    lastDeckSignature = '';
  }

  function renderJarShelves(state) {
    if (!els.arc3JarShelves) return;
    const breach = state.arc3;
    const sig = SPECIMEN_JAR_LIST.map((j) => {
      const owned = (state.ownedJars ?? []).includes(j.id);
      const shattered = arc3IsJarShattered(breach, j.id);
      return `${j.id}:${owned ? 1 : 0}:${shattered ? 1 : 0}`;
    }).join('|');
    if (sig === lastShelfSignature) return;
    lastShelfSignature = sig;

    els.arc3JarShelves.innerHTML = '';
    for (const jar of SPECIMEN_JAR_LIST) {
      const shattered = arc3IsJarShattered(breach, jar.id);
      const slot = document.createElement('div');
      slot.className = `arc3-jar-slot${shattered ? ' is-shattered' : ''}`;
      if (shattered) {
        slot.innerHTML = '<span class="arc3-jar-shard">✕</span>';
        slot.title = `${jar.title} (Shattered)`;
      } else {
        const img = document.createElement('img');
        img.className = 'arc3-jar-img';
        img.src = jarAssetUrl(jar.id);
        img.alt = '';
        img.draggable = false;
        slot.appendChild(img);
        slot.title = jar.title;
      }
      els.arc3JarShelves.appendChild(slot);
    }
  }

  function renderRuptures(state) {
    if (!els.arc3RuptureLayer) return;
    const breach = state.arc3;
    const list = breach?.activeRuptures ?? [];
    const sig = list.map((r) => `${r.id}:${r.type}:${r.xFraction.toFixed(2)}:${r.yFraction.toFixed(2)}`).join('|');
    if (sig === lastRuptureSignature) return;
    lastRuptureSignature = sig;

    els.arc3RuptureLayer.innerHTML = '';
    for (const rupture of list) {
      const meta = Object.values(BREACH_RUPTURE_TYPES).find((t) => t.id === rupture.type);
      if (!meta) continue;
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = `anomaly-node anomaly-node--wall arc3-rupture-node arc3-rupture--${rupture.type}`;
      btn.style.left = `${(rupture.xFraction * 100).toFixed(1)}%`;
      btn.style.top = `${(rupture.yFraction * 100).toFixed(1)}%`;
      btn.setAttribute('aria-label', `${meta.actionLabel} breach rupture`);

      const photo = document.createElement('img');
      photo.className = 'anomaly-photo';
      photo.src = breachRuptureAssetUrl(rupture.type);
      photo.alt = '';
      photo.draggable = false;

      const label = document.createElement('span');
      label.className = 'anomaly-label arc3-rupture-label';
      label.textContent = meta.actionLabel;

      btn.append(photo, label);
      btn.addEventListener('click', () => handlers.onTearBreachRupture?.(rupture.id));
      els.arc3RuptureLayer.appendChild(btn);
    }
  }

  function renderTabs(state) {
    if (!els.arc3DeckTabs) return;
    markTabSeen(seenKeysByTab, activeTab, arc3UnlockKeys(state, activeTab));

    els.arc3DeckTabs.innerHTML = '';
    for (const tab of ARC3_TABS) {
      const btn = document.createElement('button');
      btn.type = 'button';
      const isActive = tab.id === activeTab;
      const alerted = shouldAlertArc3Tab(state, tab.id, activeTab, seenKeysByTab);
      btn.className = `deck-tab${isActive ? ' is-active' : ''}${alerted ? ' deck-tab--alert' : ''}`;
      btn.setAttribute('role', 'tab');
      btn.setAttribute('aria-selected', isActive ? 'true' : 'false');
      btn.textContent = tab.label;
      if (alerted) {
        const pip = document.createElement('span');
        pip.className = 'deck-tab-pip';
        pip.setAttribute('aria-hidden', 'true');
        btn.appendChild(pip);
      }
      btn.addEventListener('click', () => {
        activeTab = tab.id;
        markTabSeen(seenKeysByTab, tab.id, arc3UnlockKeys(state, tab.id));
        clearDeckCache();
        handlers.onRequestRender?.();
      });
      els.arc3DeckTabs.appendChild(btn);
    }
  }

  function legacyTagForAct(state, act) {
    if (act.linkedJar) {
      return (state.ownedJars ?? []).includes(act.linkedJar)
        ? 'BOTTLED IN CHAMBER'
        : 'CHAMBER FALLBACK';
    }
    if (act.linkedScarNode) {
      return (state.unlockedScarNodes ?? []).includes(act.linkedScarNode)
        ? 'STITCHED IN CHAMBER'
        : 'SCAR FALLBACK';
    }
    if (act.linkedCreature) {
      const owned = state.creatures?.[act.linkedCreature] ?? 0;
      return owned > 0 ? `${owned} TENDED` : 'CHAMBER APPARATUS';
    }
    return null;
  }

  function renderUnmakeTab(state) {
    const breach = state.arc3;
    const container = document.createElement('div');
    container.className = 'arc3-deck-list';

    // Legacy resonance strip showing non-destructive reflection of Arc 1 & Arc 2 progress
    const hemBonusPct = Math.round((arc3LegacyHemorrhageMultiplier(state) - 1) * 100);
    const fracBonusPct = Math.round((arc3LegacyFractureMultiplier(state) - 1) * 100);
    const legacyStrip = document.createElement('div');
    legacyStrip.className = 'arc3-legacy-strip';
    legacyStrip.textContent = `CHAMBER MEMORY +${hemBonusPct}% HEMORRHAGE · CASE MEMORY +${fracBonusPct}% FRACTURE`;
    container.appendChild(legacyStrip);

    for (const cat of ARC3_UNMAKE_CATEGORIES) {
      const visibleActs = ARC3_UNMAKE_ACTS.filter(
        (a) => a.category === cat.id && arc3IsUnmakeVisible(breach, a),
      );
      if (visibleActs.length === 0) continue;

      const header = document.createElement('div');
      header.className = 'arc3-category-label';
      header.textContent = cat.label;
      container.appendChild(header);

      for (const act of visibleActs) {
        const done = (breach.completedUnmakes ?? []).includes(act.id);
        const affordable = arc3CanPerformUnmake(breach, act);
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = `deck-row arc3-unmake-row${done ? ' is-done' : ''}${affordable ? ' deck-row--accent' : ''}`;
        btn.disabled = done || !affordable;

        const body = document.createElement('div');
        body.className = 'deck-row-main';

        const titleRow = document.createElement('div');
        titleRow.className = 'arc3-unmake-title-row';
        const title = document.createElement('span');
        title.className = 'deck-row-title';
        title.textContent = act.title;
        titleRow.appendChild(title);

        const tag = legacyTagForAct(state, act);
        if (tag) {
          const chip = document.createElement('span');
          chip.className = 'arc3-legacy-tag';
          chip.textContent = tag;
          titleRow.appendChild(chip);
        }

        const desc = document.createElement('div');
        desc.className = 'deck-row-desc';
        desc.textContent = act.description;

        const effectParts = [`WRENCH ×${act.wrenchMultiplier.toFixed(2)}`];
        if (act.antiClotFraction > 0) {
          effectParts.push(`CLOT -${Math.round(act.antiClotFraction * 100)}%`);
        }
        if (act.bleedPressurePerSec > 0) {
          effectParts.push(`BLEED +${formatCompactDecimal(act.bleedPressurePerSec)}/s`);
        }
        if (act.instantFractureSurge > 0) {
          effectParts.push(`SURGE +${formatBleedValue(act.instantFractureSurge)}`);
        }
        if (act.ruptureMultiplier > 1.0) {
          effectParts.push(`RUPTURE ×${act.ruptureMultiplier.toFixed(2)}`);
        }
        const meta = document.createElement('div');
        meta.className = 'arc3-unmake-meta';
        meta.textContent = effectParts.join(' · ');

        body.append(titleRow, desc, meta);

        const pill = document.createElement('span');
        pill.className = 'deck-row-pill';
        pill.textContent = done ? 'UNMADE' : `${formatBleedValue(act.hemorrhageCost)} HEM`;

        btn.append(body, pill);
        if (!done) {
          btn.addEventListener('click', () => handlers.onPerformArc3Unmake?.(act.id));
        }
        container.appendChild(btn);
      }
    }

    els.arc3DeckContent.innerHTML = '';
    els.arc3DeckContent.appendChild(container);
  }

  function renderWedgesTab(state) {
    const breach = state.arc3;
    const active = arc3ActiveStratum(breach);
    const canDrive = arc3CanDriveWedge(breach);

    const container = document.createElement('div');
    container.className = 'arc3-deck-list';

    const note = document.createElement('div');
    note.className = 'arc3-legacy-strip';
    note.textContent = 'DRIVEN WEDGES LOCK PERMANENTLY — CLOTTING CAN NEVER SEAL PAST THEM';
    container.appendChild(note);

    for (const stratum of ARC3_STRATA) {
      const isLocked = (breach.lockedWedges ?? []).includes(stratum.id);
      const isCurrent = !arc3IsInFinale(breach) && active.id === stratum.id;
      const isReady = isCurrent && canDrive;

      const card = document.createElement('div');
      card.className = `arc3-wedge-card${isLocked ? ' is-locked' : ''}${isReady ? ' is-ready' : ''}`;

      const top = document.createElement('div');
      top.className = 'arc3-wedge-head';
      const name = document.createElement('span');
      name.className = 'deck-row-title';
      name.textContent = `${stratum.title} · ${stratum.wedgeTitle}`;
      const status = document.createElement('span');
      status.className = 'arc3-wedge-status';
      status.textContent = isLocked
        ? 'LOCKED'
        : isReady
          ? 'READY'
          : isCurrent
            ? `${formatBleedValue(breach.fracture)} / ${formatBleedValue(stratum.fractureGoal)}`
            : `GOAL ${formatBleedValue(stratum.fractureGoal)}`;
      top.append(name, status);

      const caption = document.createElement('p');
      caption.className = 'deck-row-desc';
      caption.textContent = stratum.wedgeCaption;

      card.append(top, caption);

      if (isReady) {
        const driveBtn = document.createElement('button');
        driveBtn.type = 'button';
        driveBtn.className = 'excision-btn excision-btn--confirm arc3-drive-btn';
        driveBtn.textContent = `DRIVE WEDGE: ${stratum.wedgeTitle}`;
        driveBtn.addEventListener('click', () => handlers.onDriveMemoryWedge?.());
        card.appendChild(driveBtn);
      }

      container.appendChild(card);
    }

    els.arc3DeckContent.innerHTML = '';
    els.arc3DeckContent.appendChild(container);
  }

  function renderDeck(state) {
    if (!els.arc3DeckPanel || !els.arc3DeckContent) return;
    const breach = state.arc3;
    const inFinale = arc3IsInFinale(breach);
    els.arc3DeckPanel.hidden = inFinale || breach.submersionComplete;
    if (inFinale || breach.submersionComplete) return;

    renderTabs(state);

    const hemBucket = Math.floor(breach.hemorrhage ?? 0);
    const fracReady = arc3CanDriveWedge(breach) ? 1 : 0;
    const fracBucket = activeTab === 'wedges' ? Math.floor(breach.fracture ?? 0) : 0;
    const sig = [
      activeTab,
      breach.currentStratum,
      (breach.completedUnmakes ?? []).length,
      (breach.lockedWedges ?? []).length,
      fracReady,
      fracBucket,
      hemBucket >= 1000 ? Math.floor(hemBucket / 25) : hemBucket,
    ].join('|');

    if (sig === lastDeckSignature) return;
    lastDeckSignature = sig;

    if (activeTab === 'unmake') {
      renderUnmakeTab(state);
    } else {
      renderWedgesTab(state);
    }
  }

  function render(state) {
    const breach = state.arc3;
    if (!breach) return;

    const stratum = arc3ActiveStratum(breach);
    const inFinale = arc3IsInFinale(breach);
    const submerged = Boolean(breach.submersionComplete);
    const flood = arc3FloodLevel(breach);
    const blur = arc3TelemetryBlurFactor(breach);
    const clotting = arc3IsActivelyClotting(breach);
    const clotPerSec = arc3ActiveClotPerSec(breach);
    const sheared = arc3IsValveSheared(breach);

    // Body crimson breach attributes for CSS theming & dissolution
    els.body.dataset.arc3Stratum = String(breach.currentStratum ?? 1);
    els.body.dataset.arc3Finale = inFinale ? 'true' : 'false';
    els.body.dataset.arc3Submerged = submerged ? 'true' : 'false';
    els.body.style.setProperty('--arc3-flood', flood.toFixed(3));

    // Toggle main breach stage vs submerged aftermath
    if (els.arc3ActiveStage) els.arc3ActiveStage.hidden = submerged;
    if (els.arc3SubmergedOverlay) els.arc3SubmergedOverlay.hidden = !submerged;

    // Dissolution opacity on chrome during the 5-pull finale
    const pulls = breach.finalPullsCompleted ?? 0;
    const chromeOpacity = submerged
      ? 0
      : inFinale
        ? Math.max(0.06, 1 - pulls * 0.22)
        : 1;
    if (els.arc3TelemetryHeader) {
      els.arc3TelemetryHeader.style.opacity = chromeOpacity.toFixed(2);
    }

    // Header telemetry with Strata IV-V identity blur (room vs character ambiguity)
    const showBlurredTitle = blur >= 0.45 && Math.floor(Date.now() / 2200) % 2 === 1;
    if (els.arc3HeaderTitle) {
      els.arc3HeaderTitle.textContent = submerged
        ? 'SUBMERGED'
        : inFinale
          ? 'THE OPEN SEAM'
          : showBlurredTitle
            ? stratum.blurTitle
            : stratum.title;
    }
    if (els.arc3StratumProgressFill) {
      els.arc3StratumProgressFill.style.width = `${(arc3StratumProgress(breach) * 100).toFixed(1)}%`;
    }
    if (els.arc3WedgePips) {
      const locked = breach.lockedWedges ?? [];
      els.arc3WedgePips.innerHTML = ARC3_STRATA.map(
        (s) =>
          `<span class="arc3-wedge-pip${locked.includes(s.id) ? ' is-locked' : ''}" title="${s.wedgeTitle}"></span>`,
      ).join('');
    }
    if (els.arc3StatusLine) {
      if (inFinale) {
        els.arc3StatusLine.textContent = `CLOTTING BROKEN · FINAL PULLS ${pulls} / ${ARC3_BALANCE.FINAL_PULLS_TOTAL}`;
      } else {
        const fracLabel = blur >= 0.55 ? 'YOUR PULSE' : 'FRACTURE';
        els.arc3StatusLine.textContent = `${fracLabel}: ${formatBleedValue(breach.fracture)} / ${formatBleedValue(stratum.fractureGoal)} · FLOOD ${Math.round(flood * 100)}%`;
      }
    }

    // Counter plate (telemetry blur HEMORRHAGE <-> BLOOD LOSS at high Stratum)
    if (els.arc3HemorrhageLabel) {
      els.arc3HemorrhageLabel.textContent = blur >= 0.5 ? 'BLOOD LOSS' : 'HEMORRHAGE';
    }
    if (els.arc3HemorrhageValue) {
      els.arc3HemorrhageValue.textContent = formatBleedValue(breach.hemorrhage);
    }
    if (els.arc3FloodReadout) {
      els.arc3FloodReadout.textContent = `FLOOD ${Math.round(flood * 100)}%`;
    }

    // Live tactical badges
    if (els.arc3ClotBadge) {
      els.arc3ClotBadge.hidden = !clotting || inFinale;
      if (clotting) {
        els.arc3ClotBadge.textContent = `CLOTTING -${formatCompactDecimal(clotPerSec)}/s`;
      }
    }
    if (els.arc3FreezeBadge) {
      const frozen = arc3IsClotFrozen(breach) && !inFinale;
      els.arc3FreezeBadge.hidden = !frozen;
      if (frozen) {
        const secs = Math.max(breach.clotFreezeRemainingSeconds ?? 0, breach.frenzyRemainingSeconds ?? 0);
        els.arc3FreezeBadge.textContent = `CLOT FROZEN ${secs.toFixed(1)}s`;
      }
    }
    if (els.arc3MomentumBadge) {
      const pct = Math.round((breach.momentumCharge ?? 0) * ARC3_BALANCE.MOMENTUM_MAX_BONUS * 100);
      els.arc3MomentumBadge.hidden = pct <= 2 || inFinale;
      els.arc3MomentumBadge.textContent = `MOMENTUM +${pct}%`;
    }
    if (els.arc3SurgeBadge) {
      const hasRib = (breach.ribForceRemainingSeconds ?? 0) > 0;
      const hasFrenzy = (breach.frenzyRemainingSeconds ?? 0) > 0;
      els.arc3SurgeBadge.hidden = (!hasRib && !hasFrenzy) || inFinale;
      if (hasFrenzy) {
        els.arc3SurgeBadge.textContent = `FRENZY ×2 · ${breach.frenzyRemainingSeconds.toFixed(1)}s`;
      } else if (hasRib) {
        els.arc3SurgeBadge.textContent = `RIB FORCE ×1.5 · ${breach.ribForceRemainingSeconds.toFixed(1)}s`;
      }
    }

    // Centerpiece wrench aperture vs Finale 5-pull button
    if (els.arc3WrenchArena) els.arc3WrenchArena.hidden = inFinale || submerged;
    if (els.arc3FinalePanel) els.arc3FinalePanel.hidden = !inFinale || submerged;

    if (!inFinale && !submerged) {
      if (els.arc3WrenchBtn) {
        els.arc3WrenchBtn.classList.toggle('arc3-wrench-btn--sheared', sheared);
      }
      if (els.arc3WrenchYield) {
        els.arc3WrenchYield.textContent = `+${formatCompactDecimal(arc3WrenchFractureYield(state))}`;
      }
      if (els.arc3WrenchSub) {
        els.arc3WrenchSub.textContent = sheared ? 'TEAR SEAM' : 'WRENCH CCW';
      }
      if (els.arc3DriveBanner) {
        const ready = arc3CanDriveWedge(breach);
        els.arc3DriveBanner.hidden = !ready;
        if (ready && els.arc3DriveBannerBtn) {
          els.arc3DriveBannerBtn.textContent = `DRIVE WEDGE: ${stratum.wedgeTitle}`;
        }
      }
    } else if (inFinale && !submerged) {
      const canPull = arc3CanPerformFinalPull(breach);
      const cd = breach.finalPullCooldownSeconds ?? 0;
      const cdPct = Math.max(0, Math.min(1, 1 - cd / ARC3_BALANCE.FINAL_PULL_COOLDOWN_SEC));
      if (els.arc3FinalPullBtn) {
        els.arc3FinalPullBtn.disabled = !canPull;
        const prompt = FINAL_PULL_LABELS[Math.min(pulls, FINAL_PULL_LABELS.length - 1)];
        els.arc3FinalPullBtn.textContent = canPull
          ? `${prompt} (${pulls + 1} / ${ARC3_BALANCE.FINAL_PULLS_TOTAL})`
          : `BREATHING IN THE RED... (${cd.toFixed(1)}s)`;
      }
      if (els.arc3FinalPullCooldownFill) {
        els.arc3FinalPullCooldownFill.style.width = `${(cdPct * 100).toFixed(1)}%`;
      }
      if (els.arc3FinalPullCount) {
        els.arc3FinalPullCount.textContent = `PULL ${pulls} OF ${ARC3_BALANCE.FINAL_PULLS_TOTAL}`;
      }
    }

    if (submerged && els.arc3SubmergedCount) {
      els.arc3SubmergedCount.textContent = `NOTES RECOVERED: ${state.discoveredDialogues.length}`;
    }

    renderJarShelves(state);
    renderRuptures(state);
    renderDeck(state);
  }

  return {
    render,
    resetViewState() {
      activeTab = 'unmake';
      lastDeckSignature = '';
      lastShelfSignature = '';
      lastRuptureSignature = '';
      seenKeysByTab.clear();
    },
  };
}
