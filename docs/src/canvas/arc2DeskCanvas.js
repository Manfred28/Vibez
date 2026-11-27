/**
 * Arc 2 Investigator Desk photographic Canvas renderer & Slow-Boil cross-fade helper.
 * Mirrors Android `InvestigatorArtwork.kt` + `InvestigatorDeskCanvas.kt` 1:1:
 * - Cases I–III: strictly Slow-Boil quarantined (zero Case IV/V bleed or grout).
 * - Case IV Lamp Off: faint blotter grout hint + subtle Case V shadow bleed at high haunt.
 * - Case V+ / File #00: persistent hospital tile grout, weeping grey oil drop, corner scratches,
 *   and peeled blotter corner.
 */

import {
  DESK_PALETTE,
  PALETTE,
  drawCoverImage,
  rgba,
  syncCanvasSize,
} from './canvasShared.js?v=3.16';
import { arc2HauntDepth } from '../engine/arc2Models.js?v=3.16';

export function computeDeskLayerAlphas(
  caseAngle,
  hauntDepth = 0,
  lampOn = true,
  fileZeroRevealed = false,
) {
  const clampedCase = Math.max(1, Math.min(5, Math.floor(caseAngle || 1)));
  const haunt = Math.max(0, Math.min(1, Number(hauntDepth || 0)));
  if (fileZeroRevealed) {
    return { case1: 0, case2: 0, case3: 0, case4: 0.14, case5: 0.94 };
  }
  switch (clampedCase) {
    case 1:
      return { case1: 0.9, case2: 0, case3: 0, case4: 0, case5: 0 };
    case 2:
      return { case1: 0.22, case2: 0.88, case3: 0, case4: 0, case5: 0 };
    case 3:
      return { case1: 0, case2: 0.2, case3: 0.88, case4: 0, case5: 0 };
    case 4:
      return {
        case1: 0,
        case2: 0,
        case3: 0.18,
        case4: lampOn ? 0.78 : 0.92,
        case5: !lampOn && haunt >= 0.72 ? 0.24 : 0,
      };
    default:
      return {
        case1: 0,
        case2: 0,
        case3: 0,
        case4: 0.26,
        case5: lampOn ? 0.82 : 0.92,
      };
  }
}

export function drawInvestigatorDeskBackdrop(
  canvas,
  deskImagesOrState,
  stateOrBreath,
  breathOrDrop,
  dropPhaseArg,
) {
  // Support both `(canvas, deskImages, state, breath, dropPhase)` and legacy `(canvas, state, breath, dropPhase)`
  const hasDeskImages =
    deskImagesOrState &&
    typeof deskImagesOrState === 'object' &&
    ('case1' in deskImagesOrState || 'case5' in deskImagesOrState);
  const deskImages = hasDeskImages ? deskImagesOrState : null;
  const state = hasDeskImages ? stateOrBreath : deskImagesOrState;
  const breath = hasDeskImages ? (breathOrDrop ?? 0) : (stateOrBreath ?? 0);
  const dropPhase = hasDeskImages ? (dropPhaseArg ?? breath) : (breathOrDrop ?? breath);

  const { ctx, width, height } = syncCanvasSize(canvas);
  ctx.clearRect(0, 0, width, height);

  const desk = state?.arc2 ?? {};
  const lampOn = desk.lampOn !== false;
  const caseAngle = desk.caseAngle ?? 1;
  const haunt = arc2HauntDepth(desk);
  const fileZero = Boolean(desk.fileZeroRevealed);

  // 1. Base nocturnal office slate underneath photography
  ctx.fillStyle = lampOn ? DESK_PALETTE.slateVoid : '#06080c';
  ctx.fillRect(0, 0, width, height);

  // 2. Five-stage progressive photographic desk backdrop cross-fade (Slow-Boil quarantined)
  const layers = computeDeskLayerAlphas(caseAngle, haunt, lampOn, fileZero);
  if (deskImages) {
    if (layers.case1 > 0) {
      drawCoverImage(ctx, deskImages.case1, width, height, layers.case1, 1);
    }
    if (layers.case2 > 0) {
      drawCoverImage(ctx, deskImages.case2, width, height, layers.case2, 1);
    }
    if (layers.case3 > 0) {
      drawCoverImage(ctx, deskImages.case3, width, height, layers.case3, 1);
    }
    if (layers.case4 > 0) {
      drawCoverImage(ctx, deskImages.case4, width, height, layers.case4, 1);
    }
    if (layers.case5 > 0) {
      drawCoverImage(ctx, deskImages.case5, width, height, layers.case5, 1.012 + breath * 0.014);
    }
  }

  // 3. Atmospheric Lamp On / Lamp Off vignette & central readability recess
  const veil = ctx.createLinearGradient(0, 0, 0, height);
  veil.addColorStop(0, rgba('#06090d', lampOn ? 0.56 : 0.72));
  veil.addColorStop(0.5, rgba('#070a0e', lampOn ? 0.24 : 0.44));
  veil.addColorStop(1, rgba('#06080b', lampOn ? 0.58 : 0.72));
  ctx.fillStyle = veil;
  ctx.fillRect(0, 0, width, height);

  const hudShadow = ctx.createRadialGradient(
    width * 0.5,
    height * 0.28,
    0,
    width * 0.5,
    height * 0.28,
    Math.min(width, height) * 0.54,
  );
  hudShadow.addColorStop(0, rgba('#06090d', 0.52));
  hudShadow.addColorStop(1, 'transparent');
  ctx.fillStyle = hudShadow;
  ctx.fillRect(0, 0, width, height);

  // 4. Overhead tungsten lamp cone (Lamp On) OR cold moonlit slate wash (Lamp Off)
  const lampSway = (breath - 0.5) * 8;
  const lampX = width * 0.5 + lampSway;
  if (lampOn) {
    const cone = ctx.createRadialGradient(lampX, height * 0.42, 4, lampX, height * 0.42, width * 0.62);
    cone.addColorStop(0, rgba(DESK_PALETTE.tungstenAmber, 0.24 + breath * 0.04));
    cone.addColorStop(0.55, rgba(DESK_PALETTE.tungstenAmber, 0.06));
    cone.addColorStop(1, 'transparent');
    ctx.fillStyle = cone;
    ctx.fillRect(0, 0, width, height);
  } else {
    const moon = ctx.createRadialGradient(
      width * 0.16,
      height * 0.18,
      6,
      width * 0.16,
      height * 0.18,
      width * 0.76,
    );
    moon.addColorStop(0, rgba('#7c91ad', 0.16 + breath * 0.04));
    moon.addColorStop(0.55, rgba('#465a74', 0.07));
    moon.addColorStop(1, 'transparent');
    ctx.fillStyle = moon;
    ctx.fillRect(0, 0, width, height);
  }

  // 5. Cold rain streaks across dark precinct glass
  ctx.strokeStyle = rgba(DESK_PALETTE.paperBone, lampOn ? 0.11 : 0.21);
  ctx.lineWidth = 1;
  ctx.lineCap = 'round';
  for (let i = 0; i < 12; i += 1) {
    const rx = width * ((i * 0.082 + 0.04) % 1.0);
    const offset = ((dropPhase + i * 0.19) % 1.0) * (height * 0.52);
    ctx.beginPath();
    ctx.moveTo(rx, offset);
    ctx.lineTo(rx - 2.5, offset + 15);
    ctx.stroke();
  }

  // 6. Slow-Boil-quarantined Room Bleed hairline grout (ZERO in Cases I–III)
  const showGrout = fileZero || caseAngle >= 5 || (caseAngle === 4 && !lampOn);
  if (showGrout) {
    const bleedStrength = Math.max(0.15, Math.min(1, (haunt - 0.55) / 0.45));
    const groutAlpha = Math.max(0.05, Math.min(0.26, bleedStrength * (lampOn ? 0.14 : 0.24)));
    const tileStep = 38;
    ctx.strokeStyle = rgba(PALETTE.driedClot, groutAlpha);
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

  // 7. Case V+ ONLY: slow weeping grey oil bead + corner reflection scratches
  if (caseAngle >= 5 || fileZero) {
    const dropY = 16 + (dropPhase % 1.0) * (height * 0.46);
    const dropAlpha = (1 - (dropPhase % 1.0)) * 0.72;
    ctx.fillStyle = rgba(PALETTE.paleLichen, dropAlpha);
    ctx.beginPath();
    ctx.arc(lampX, dropY, 2.4, 0, Math.PI * 2);
    ctx.fill();

    ctx.strokeStyle = rgba(PALETTE.driedClot, 0.26 + breath * 0.14);
    ctx.lineWidth = 1.2;
    for (let s = 0; s < 4; s += 1) {
      const sx = width * 0.8 + s * 6;
      ctx.beginPath();
      ctx.moveTo(sx, 14);
      ctx.lineTo(sx - 5, 40 + (s % 2) * 8);
      ctx.stroke();
    }
  }

  // 8. File #00 blotter peel-back accent corner
  if (fileZero) {
    ctx.fillStyle = rgba(PALETTE.clotShadow, 0.64);
    ctx.strokeStyle = PALETTE.driedClot;
    ctx.lineWidth = 1.3;
    ctx.beginPath();
    ctx.moveTo(0, height);
    ctx.lineTo(width * 0.22, height);
    ctx.lineTo(0, height * 0.74);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();
  }
}

// Legacy stub preserved for backward compatibility with canvas exports
export function drawDeskAnomalyGlyph(canvas, type, breath) {
  if (!canvas) return;
  const { ctx, width, height } = syncCanvasSize(canvas);
  ctx.clearRect(0, 0, width, height);
  const cx = width * 0.5;
  const cy = height * 0.5;
  ctx.fillStyle = rgba(DESK_PALETTE.tungstenAmber, 0.2 + (breath || 0) * 0.1);
  ctx.beginPath();
  ctx.arc(cx, cy, Math.min(width, height) * 0.3, 0, Math.PI * 2);
  ctx.fill();
}
