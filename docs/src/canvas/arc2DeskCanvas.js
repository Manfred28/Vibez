/**
 * Arc 2 Investigator Desk backdrop & desk anomaly glyph Canvas renderers.
 * Enforces the 4-Act Slow Boil visual quarantine:
 * - Cases I–III: cold municipal noir & eerie haunting (Lamp On amber vs Lamp Off moonlight,
 *   second-chair silhouette in Cases II–IV Lamp Off) with ZERO grotesque hospital-room bleed.
 * - Case IV Lamp Off: faint blotter grout hint.
 * - Case V+ / File #00: persistent hospital tile grout, grey oil weep, corner scratches, and blotter curl.
 */

import { DESK_PALETTE, PALETTE, rgba, syncCanvasSize } from './canvasShared.js?v=3.13';
import { arc2HauntDepth } from '../engine/arc2Models.js?v=3.13';

export function drawInvestigatorDeskBackdrop(canvas, state, breath, dropPhase = breath) {
  const { ctx, width, height } = syncCanvasSize(canvas);
  ctx.clearRect(0, 0, width, height);

  const desk = state.arc2 ?? {};
  const lampOn = desk.lampOn !== false;
  const caseAngle = desk.caseAngle ?? 1;
  const haunt = arc2HauntDepth(desk);
  const fileZero = Boolean(desk.fileZeroRevealed);

  // 1. Office desk slate backdrop (warmer shadows with Lamp On, chilled moonlight with Lamp Off)
  const bg = ctx.createLinearGradient(0, 0, 0, height);
  bg.addColorStop(0, lampOn ? DESK_PALETTE.slateVoid : '#07090d');
  bg.addColorStop(0.45, lampOn ? DESK_PALETTE.deskDark : '#0b0f17');
  bg.addColorStop(1, lampOn ? DESK_PALETTE.slateVoid : '#06080c');
  ctx.fillStyle = bg;
  ctx.fillRect(0, 0, width, height);

  // 2. Overhead desk lamp glow (Lamp On) OR cold window moonlight wash (Lamp Off)
  const lampX = width * 0.5;
  const lampY = 18;
  if (lampOn) {
    const cone = ctx.createRadialGradient(lampX, lampY + 26, 4, lampX, lampY + 26, width * 0.64);
    cone.addColorStop(0, rgba(DESK_PALETTE.tungstenAmber, 0.25));
    cone.addColorStop(0.55, rgba(DESK_PALETTE.tungstenAmber, 0.08));
    cone.addColorStop(1, 'transparent');
    ctx.fillStyle = cone;
    ctx.fillRect(0, 0, width, height);
  } else {
    const moon = ctx.createRadialGradient(
      width * 0.16,
      height * 0.14,
      6,
      width * 0.16,
      height * 0.14,
      width * 0.72,
    );
    moon.addColorStop(0, rgba('#7c91ad', 0.16 + breath * 0.04));
    moon.addColorStop(0.55, rgba('#52647d', 0.06));
    moon.addColorStop(1, 'transparent');
    ctx.fillStyle = moon;
    ctx.fillRect(0, 0, width, height);
  }

  // 3. Rain streaks on dark precinct window
  ctx.strokeStyle = rgba(DESK_PALETTE.paperBone, lampOn ? 0.1 : 0.18);
  ctx.lineWidth = 1;
  for (let i = 0; i < 14; i += 1) {
    const rx = width * ((i * 0.077 + 0.03) % 1.0);
    const offset = ((dropPhase + i * 0.19) % 1.0) * height;
    ctx.beginPath();
    ctx.moveTo(rx, offset - 18);
    ctx.lineTo(rx - 4, offset + 12);
    ctx.stroke();
  }

  // 4. Case II–IV Lamp Off eerie haunting: faint silhouette of the second chair facing the desk
  if (!lampOn && caseAngle >= 2 && caseAngle <= 4) {
    ctx.fillStyle = rgba('#04060a', 0.36 + breath * 0.08);
    ctx.fillRect(width * 0.08, height * 0.3, width * 0.14, height * 0.42);
    ctx.strokeStyle = rgba('#7c91ad', 0.14);
    ctx.lineWidth = 1;
    ctx.strokeRect(width * 0.08, height * 0.3, width * 0.14, height * 0.42);
  }

  // 5. Slow-Boil-quarantined Room Bleed:
  //    ZERO grout in Cases I–III. Faint hint in Case IV Lamp Off. Persistent tile & oil weep in Case V+.
  const showGrout = fileZero || caseAngle >= 5 || (caseAngle === 4 && !lampOn);
  if (showGrout) {
    const groutAlpha = fileZero
      ? 0.32
      : caseAngle >= 5
        ? 0.22 + (haunt - 0.85) * 0.5
        : 0.11 + breath * 0.03;
    const tileStep = 36;
    ctx.strokeStyle = rgba(PALETTE.driedClot, Math.max(0.05, Math.min(0.36, groutAlpha)));
    ctx.lineWidth = 1;
    for (let x = tileStep; x < width; x += tileStep) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, height);
      ctx.stroke();
    }
    for (let y = tileStep; y < height; y += tileStep) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(width, y);
      ctx.stroke();
    }
  }

  // 6. Leather desk blotter (curls back on File #00 reveal)
  const pad = 16;
  const blotterTop = height * 0.36;
  const blotterH = height * 0.56;
  ctx.fillStyle = rgba(DESK_PALETTE.deskSurface, lampOn ? 0.78 : 0.64);
  ctx.strokeStyle = rgba(DESK_PALETTE.rainSlate, 0.9);
  ctx.lineWidth = 1;
  ctx.fillRect(pad, blotterTop, width - pad * 2, blotterH);
  ctx.strokeRect(pad, blotterTop, width - pad * 2, blotterH);

  if (fileZero) {
    // Peeled lower-right blotter corner revealing damp grey tile underneath
    ctx.fillStyle = rgba(PALETTE.driedClot, 0.28);
    ctx.beginPath();
    ctx.moveTo(width - pad, blotterTop + blotterH);
    ctx.lineTo(width - pad - 44, blotterTop + blotterH);
    ctx.lineTo(width - pad, blotterTop + blotterH - 44);
    ctx.closePath();
    ctx.fill();
  }

  // 7. Suspended brass lamp hood + pull-chain + after-hours condensation bead
  ctx.fillStyle = DESK_PALETTE.rainSlate;
  ctx.beginPath();
  ctx.moveTo(lampX - 24, lampY + 6);
  ctx.lineTo(lampX + 24, lampY + 6);
  ctx.lineTo(lampX + 12, lampY - 8);
  ctx.lineTo(lampX - 12, lampY - 8);
  ctx.closePath();
  ctx.fill();

  if (!lampOn) {
    const fall = Math.pow(dropPhase % 1.0, 2);
    const dy = lampY + 8 + fall * 38;
    ctx.fillStyle = rgba(PALETTE.paleLichen, Math.max(0.14, 0.86 - fall * 0.72));
    ctx.beginPath();
    ctx.arc(lampX, dy, 2.3, 0, Math.PI * 2);
    ctx.fill();
  }

  // 8. Case V+ ONLY: grey oil weep & subtle corner scratch echoes
  if (caseAngle >= 5 || fileZero) {
    const marks = Math.min(state.scarTissue > 0 ? state.scarTissue : 3, 5);
    ctx.strokeStyle = rgba(DESK_PALETTE.paperBone, 0.22);
    ctx.lineWidth = 1.2;
    for (let i = 0; i < marks; i += 1) {
      const y = 26 + i * 11 + breath * 1.5;
      ctx.beginPath();
      ctx.moveTo(width - 42, y);
      ctx.lineTo(width - 14, y + 4);
      ctx.stroke();
    }
  }

  // 9. Faint red-string corner threads scaling with wired corkboard pairs
  const wiredCount = desk.wiredPairs?.length ?? 0;
  if (wiredCount > 0) {
    ctx.strokeStyle = rgba(DESK_PALETTE.caseRed, 0.45);
    ctx.lineWidth = 1.2;
    const pins = [
      [width * 0.14, height * 0.2],
      [width * 0.84, height * 0.24],
      [width * 0.2, height * 0.84],
      [width * 0.82, height * 0.8],
      [width * 0.5, height * 0.14],
    ];
    for (let i = 0; i < Math.min(wiredCount, pins.length - 1); i += 1) {
      ctx.beginPath();
      ctx.moveTo(pins[i][0], pins[i][1]);
      ctx.lineTo(pins[i + 1][0], pins[i + 1][1]);
      ctx.stroke();
    }
  }
}

export function drawDeskAnomalyGlyph(canvas, type, breath) {
  const { ctx, width, height } = syncCanvasSize(canvas);
  ctx.clearRect(0, 0, width, height);
  const cx = width * 0.5;
  const cy = height * 0.5;

  if (type === 'evidence_envelope') {
    ctx.fillStyle = rgba(DESK_PALETTE.tungstenAmber, 0.24);
    ctx.strokeStyle = DESK_PALETTE.tungstenAmber;
    ctx.lineWidth = 1.3;
    ctx.fillRect(cx - 11, cy - 8, 22, 16);
    ctx.strokeRect(cx - 11, cy - 8, 22, 16);
    ctx.beginPath();
    ctx.moveTo(cx - 11, cy - 8);
    ctx.lineTo(cx, cy + 1);
    ctx.lineTo(cx + 11, cy - 8);
    ctx.stroke();
  } else if (type === 'fogged_spectacles') {
    ctx.strokeStyle = DESK_PALETTE.paperBone;
    ctx.fillStyle = rgba(PALETTE.paleLichen, 0.22 + breath * 0.16);
    ctx.lineWidth = 1.4;
    ctx.beginPath();
    ctx.arc(cx - 6, cy, 5.5, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();
    ctx.beginPath();
    ctx.arc(cx + 6, cy, 5.5, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(cx - 0.5, cy);
    ctx.lineTo(cx + 0.5, cy);
    ctx.stroke();
  } else if (type === 'reel_tape') {
    ctx.strokeStyle = DESK_PALETTE.paperBone;
    ctx.lineWidth = 1.3;
    ctx.beginPath();
    ctx.arc(cx - 6, cy, 5.5, 0, Math.PI * 2);
    ctx.stroke();
    ctx.beginPath();
    ctx.arc(cx + 6, cy, 5.5, 0, Math.PI * 2);
    ctx.stroke();
    ctx.strokeStyle = DESK_PALETTE.caseRed;
    ctx.beginPath();
    ctx.moveTo(cx - 6, cy + 5.5);
    ctx.lineTo(cx + 6, cy + 5.5);
    ctx.stroke();
  } else {
    // enamel_exhibit (Case IV+ molar/enamel evidence pouch)
    ctx.fillStyle = rgba(DESK_PALETTE.paperBone, 0.2 + breath * 0.12);
    ctx.strokeStyle = DESK_PALETTE.caseRed;
    ctx.lineWidth = 1.4;
    ctx.beginPath();
    ctx.roundRect(cx - 8, cy - 9, 16, 18, 4);
    ctx.fill();
    ctx.stroke();
    ctx.fillStyle = DESK_PALETTE.paperBone;
    ctx.beginPath();
    ctx.ellipse(cx, cy, 4.2, 5.5, 0.2, 0, Math.PI * 2);
    ctx.fill();
  }
}
