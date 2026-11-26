/**
 * Arc 2 Investigator Desk backdrop & desk anomaly glyph Canvas renderers.
 */

import { DESK_PALETTE, PALETTE, rgba, syncCanvasSize } from './canvasShared.js?v=3.12';

export function drawInvestigatorDeskBackdrop(canvas, state, breath, dropPhase = breath) {
  const { ctx, width, height } = syncCanvasSize(canvas);
  ctx.clearRect(0, 0, width, height);

  const desk = state.arc2;
  const complicity = Math.max(0.05, Math.min(1.0, desk?.complicity ?? 0.05));
  const isSilence = Boolean(desk?.isSilenceActive);

  // 1. Rain-chilled slate office desk surface
  const bg = ctx.createLinearGradient(0, 0, 0, height);
  bg.addColorStop(0, DESK_PALETTE.slateVoid);
  bg.addColorStop(0.42, DESK_PALETTE.deskDark);
  bg.addColorStop(1, DESK_PALETTE.slateVoid);
  ctx.fillStyle = bg;
  ctx.fillRect(0, 0, width, height);

  // 2. Overhead tungsten desk lamp glow cone
  const lampX = width * 0.5;
  const lampY = 18;
  const cone = ctx.createRadialGradient(lampX, lampY + 26, 4, lampX, lampY + 26, width * 0.62);
  cone.addColorStop(0, rgba(DESK_PALETTE.tungstenAmber, isSilence ? 0.28 : 0.16));
  cone.addColorStop(0.55, rgba(DESK_PALETTE.tungstenAmber, 0.05));
  cone.addColorStop(1, 'transparent');
  ctx.fillStyle = cone;
  ctx.fillRect(0, 0, width, height);

  // 3. Rain streaks on dark precinct glass behind desk
  ctx.strokeStyle = rgba(DESK_PALETTE.paperBone, 0.12);
  ctx.lineWidth = 1;
  for (let i = 0; i < 14; i += 1) {
    const rx = width * ((i * 0.077 + 0.03) % 1.0);
    const offset = ((dropPhase + i * 0.19) % 1.0) * height;
    ctx.beginPath();
    ctx.moveTo(rx, offset - 18);
    ctx.lineTo(rx - 4, offset + 12);
    ctx.stroke();
  }

  // 4. Arc 1 Haunting Bleed: wet hospital tile grout emerging beneath the desk blotter
  if (complicity > 0.1) {
    const tileStep = 36;
    ctx.strokeStyle = rgba(PALETTE.driedClot, complicity * 0.34);
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

  // 5. Leather desk blotter (curls back slightly on File #00 reveal)
  const pad = 16;
  ctx.fillStyle = rgba(DESK_PALETTE.deskSurface, 0.72 - complicity * 0.2);
  ctx.strokeStyle = rgba(DESK_PALETTE.rainSlate, 0.9);
  ctx.lineWidth = 1;
  ctx.fillRect(pad, height * 0.36, width - pad * 2, height * 0.56);
  ctx.strokeRect(pad, height * 0.36, width - pad * 2, height * 0.56);

  // 6. Suspended tungsten lamp hood + downward condensation drip during Late-Night Silence
  ctx.fillStyle = DESK_PALETTE.rainSlate;
  ctx.beginPath();
  ctx.moveTo(lampX - 24, lampY + 6);
  ctx.lineTo(lampX + 24, lampY + 6);
  ctx.lineTo(lampX + 12, lampY - 8);
  ctx.lineTo(lampX - 12, lampY - 8);
  ctx.closePath();
  ctx.fill();

  if (isSilence) {
    const fall = Math.pow(dropPhase % 1.0, 2);
    const dy = lampY + 8 + fall * 38;
    ctx.fillStyle = rgba(PALETTE.paleLichen, Math.max(0.12, 0.86 - fall * 0.72));
    ctx.beginPath();
    ctx.arc(lampX, dy, 2.3, 0, Math.PI * 2);
    ctx.fill();
  }

  // 7. Haunted corner reflection showing Arc 1 Scar Tissue stitch echoes
  const scarMarks = Math.min(state.scarTissue > 0 ? state.scarTissue : 3, 5);
  ctx.strokeStyle = rgba(DESK_PALETTE.paperBone, 0.14 + complicity * 0.28);
  ctx.lineWidth = 1.2;
  for (let i = 0; i < scarMarks; i += 1) {
    const y = 26 + i * 11 + breath * 1.5;
    ctx.beginPath();
    ctx.moveTo(width - 42, y);
    ctx.lineTo(width - 14, y + 4);
    ctx.stroke();
  }

  // 8. Red-string diagonal case pins when chapters progress
  const chapters = desk?.completedChapters ?? 0;
  if (chapters > 0) {
    ctx.strokeStyle = rgba(DESK_PALETTE.caseRed, 0.55);
    ctx.lineWidth = 1.2;
    const pins = [
      [width * 0.14, height * 0.2],
      [width * 0.84, height * 0.24],
      [width * 0.2, height * 0.84],
      [width * 0.82, height * 0.8],
    ];
    for (let i = 0; i < Math.min(chapters, pins.length - 1); i += 1) {
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
  } else if (type === 'red_string') {
    ctx.strokeStyle = DESK_PALETTE.caseRed;
    ctx.lineWidth = 1.8;
    ctx.beginPath();
    ctx.moveTo(cx - 10, cy + 7);
    ctx.quadraticCurveTo(cx - 2, cy - 10 - breath * 2, cx + 10, cy - 4);
    ctx.stroke();
    ctx.fillStyle = DESK_PALETTE.paperBone;
    ctx.beginPath();
    ctx.arc(cx - 10, cy + 7, 2.2, 0, Math.PI * 2);
    ctx.arc(cx + 10, cy - 4, 2.2, 0, Math.PI * 2);
    ctx.fill();
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
  }
}
