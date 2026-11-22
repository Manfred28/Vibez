/**
 * High-DPI HTML5 Canvas renderers mirroring the Android Jetpack Compose Canvas artwork:
 * - Phase 0..4 mutating chamber wall backdrop (tile warping, corner seepage, olive streaks,
 *   bruised tissue pockets, Phase 4 chitinous rib arches, stitched Scar Tissue seams, fissures)
 * - Suspended organic pear/bell Weeping Lamps with Stillness glow & falling condensation tears
 * - Shaded anatomical Suture-Hands & intertwined Grafted Choir-Tendrils gripping the valve collar
 * - 7-segment undulating Leech-Clerks clinging to the recessed counter glass edges
 * - Weeping Lamp specimen portrait & anatomical Wall Anomaly glyphs
 */

import { CREATURE_TYPES, creatureCount } from './engine.js';

const PALETTE = Object.freeze({
  voidAbyss: '#0A0B0B',
  sunkenSlate: '#111313',
  chitinSurface: '#1F2321',
  hairlineAsh: '#2C312F',
  fractureOutline: '#3E4441',
  bleachedBone: '#C8CBC3',
  weatheredAsh: '#838881',
  paleLichen: '#8D988A',
  bileOlive: '#6B705C',
  bileShadow: '#21241E',
  driedClot: '#7B4A4A',
  clotShadow: '#28191A',
});

function rgba(hex, alpha) {
  const n = parseInt(hex.slice(1), 16);
  const r = (n >> 16) & 255;
  const g = (n >> 8) & 255;
  const b = n & 255;
  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
}

function syncCanvasSize(canvas) {
  const dpr = window.devicePixelRatio || 1;
  const rect = canvas.getBoundingClientRect();
  const w = Math.max(1, Math.round(rect.width));
  const h = Math.max(1, Math.round(rect.height));
  if (canvas.width !== Math.round(w * dpr) || canvas.height !== Math.round(h * dpr)) {
    canvas.width = Math.round(w * dpr);
    canvas.height = Math.round(h * dpr);
  }
  const ctx = canvas.getContext('2d');
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  return { ctx, width: w, height: h };
}

export function drawChamberBackdrop(canvas, wallImage, state, breath) {
  const { ctx, width, height } = syncCanvasSize(canvas);
  ctx.clearRect(0, 0, width, height);

  const phase = state.phase;
  const distortion = Math.min(1, phase * 0.25);
  const valveCenter = { x: width * 0.5, y: height * 0.65 };

  // 1. Damp cracked wall photo with phase-appropriate opacity & tint
  if (wallImage && wallImage.complete && wallImage.naturalWidth > 0) {
    const alphas = [0.26, 0.66, 0.72, 0.78, 0.84];
    ctx.save();
    ctx.globalAlpha = alphas[Math.min(phase, 4)];
    const imgRatio = wallImage.naturalWidth / wallImage.naturalHeight;
    const boxRatio = width / height;
    let drawW = width;
    let drawH = height;
    let dx = 0;
    let dy = 0;
    if (imgRatio > boxRatio) {
      drawW = height * imgRatio;
      dx = (width - drawW) / 2;
    } else {
      drawH = width / imgRatio;
      dy = (height - drawH) / 2;
    }
    ctx.drawImage(wallImage, dx, dy, drawW, drawH);
    ctx.restore();
  }

  // 2. Phase atmospheric vertical gradient veil
  const midTints = [
    rgba(PALETTE.voidAbyss, 0.44),
    rgba(PALETTE.voidAbyss, 0.30),
    rgba(PALETTE.bileShadow, 0.38),
    rgba(PALETTE.clotShadow, 0.44),
    rgba(PALETTE.clotShadow, 0.54),
  ];
  const veil = ctx.createLinearGradient(0, 0, 0, height);
  veil.addColorStop(0, rgba(PALETTE.voidAbyss, 0.78));
  veil.addColorStop(0.5, midTints[Math.min(phase, 4)]);
  veil.addColorStop(1, rgba(PALETTE.voidAbyss, 0.74));
  ctx.fillStyle = veil;
  ctx.fillRect(0, 0, width, height);

  // Phase 0 sterile fluorescent top wash
  if (phase === 0) {
    const glow = ctx.createRadialGradient(width * 0.5, 0, 0, width * 0.5, 0, width * 0.65);
    glow.addColorStop(0, rgba(PALETTE.bleachedBone, 0.08));
    glow.addColorStop(1, 'transparent');
    ctx.fillStyle = glow;
    ctx.fillRect(0, 0, width, height);
  }

  // 3. Tile grout grid (warps inward around valve in Phases 3-4)
  const tileStep = 38;
  ctx.strokeStyle =
    phase >= 2
      ? rgba(PALETTE.driedClot, 0.14 + phase * 0.04)
      : rgba(PALETTE.hairlineAsh, 0.28);
  ctx.lineWidth = phase >= 3 ? 1.15 : 1;
  const warpStrength = phase >= 4 ? 18 + breath * 5 : phase === 3 ? 10 + breath * 3 : 0;

  for (let x = tileStep; x < width; x += tileStep) {
    ctx.beginPath();
    ctx.moveTo(x, 0);
    if (warpStrength > 0) {
      const pull = ((valveCenter.x - x) / width) * warpStrength * 2.2;
      ctx.quadraticCurveTo(x + pull, valveCenter.y, x, height);
    } else {
      ctx.lineTo(x, height);
    }
    ctx.stroke();
  }
  for (let y = tileStep; y < height; y += tileStep) {
    ctx.beginPath();
    ctx.moveTo(0, y);
    if (warpStrength > 0) {
      const pull = ((valveCenter.y - y) / height) * warpStrength * 1.8;
      ctx.quadraticCurveTo(valveCenter.x, y + pull, width, y);
    } else {
      ctx.lineTo(width, y);
    }
    ctx.stroke();
  }

  // 4. Phase 1+ damp corner seepage stains
  if (phase >= 1) {
    const r = Math.min(width, height) * (0.28 + phase * 0.05);
    const stain = phase >= 3 ? rgba(PALETTE.clotShadow, 0.44) : rgba(PALETTE.voidAbyss, 0.48);
    for (const [sx, sy] of [[0, height], [width, height * 0.85]]) {
      const g = ctx.createRadialGradient(sx, sy, 0, sx, sy, r);
      g.addColorStop(0, stain);
      g.addColorStop(1, 'transparent');
      ctx.fillStyle = g;
      ctx.beginPath();
      ctx.arc(sx, sy, r, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  // 5. Phase 2+ vertical condensation streaks
  if (phase >= 2) {
    ctx.lineCap = 'round';
    for (let s = 1; s <= 5; s += 1) {
      const sx = width * (s / 6);
      const topY = height * (0.12 + (s % 3) * 0.14);
      const len = height * (0.24 + breath * 0.08);
      ctx.strokeStyle = rgba(PALETTE.paleLichen, 0.16);
      ctx.lineWidth = 1.2;
      ctx.beginPath();
      ctx.moveTo(sx, topY);
      ctx.lineTo(sx, topY + len);
      ctx.stroke();
      ctx.fillStyle = rgba(PALETTE.paleLichen, 0.24);
      ctx.beginPath();
      ctx.arc(sx, topY + len, 1.8, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  // 6. Phase 3+ bruised tissue pockets along wall margins
  if (phase >= 3) {
    const pocket = 46;
    ctx.fillStyle = rgba(PALETTE.driedClot, 0.26 + breath * 0.08);
    ctx.strokeStyle = rgba(PALETTE.fractureOutline, 0.65);
    ctx.lineWidth = 1.2;
    ctx.beginPath();
    ctx.moveTo(0, height * 0.36);
    ctx.quadraticCurveTo(pocket * 0.85, height * 0.44, 0, height * 0.56);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(width, height * 0.42);
    ctx.quadraticCurveTo(width - pocket * 0.85, height * 0.50, width, height * 0.64);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();
  }

  // 7. Phase 4 encroaching chitinous rib arches
  if (phase >= 4) {
    ctx.lineCap = 'round';
    for (let r = 0; r <= 3; r += 1) {
      const baseY = height * (0.22 + r * 0.20);
      const reach = width * (0.20 + (r === 1 || r === 2 ? 0.07 : 0) + breath * 0.02);
      for (const side of [-1, 1]) {
        const edgeX = side === -1 ? 0 : width;
        const ctrlX = side === -1 ? reach : width - reach;
        ctx.beginPath();
        ctx.moveTo(edgeX, baseY - 12);
        ctx.quadraticCurveTo(ctrlX, baseY, edgeX, baseY + 12);
        ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.78);
        ctx.lineWidth = 6;
        ctx.stroke();
        ctx.strokeStyle = rgba(PALETTE.weatheredAsh, 0.34 + breath * 0.10);
        ctx.lineWidth = 2.6;
        ctx.stroke();
      }
    }
  }

  // 8. Permanent Scar Tissue stitched seams across Cycles 1+
  if (state.scarTissue > 0) {
    const seams = Math.min(state.scarTissue, 4);
    ctx.strokeStyle = rgba(PALETTE.bleachedBone, 0.36);
    for (let s = 0; s < seams; s += 1) {
      const startY = height * (0.14 + s * 0.22);
      const endY = startY + height * 0.16;
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.moveTo(0, startY);
      ctx.lineTo(width, endY);
      ctx.stroke();
      ctx.lineWidth = 1.1;
      for (let st = 1; st < 8; st += 1) {
        const t = st / 8;
        const sx = width * t;
        const sy = startY + (endY - startY) * t;
        ctx.beginPath();
        ctx.moveTo(sx - 3, sy - 5);
        ctx.lineTo(sx + 3, sy + 5);
        ctx.stroke();
      }
    }
  }

  // 9. Radiating cracks & arterial veins from the recessed valve
  if (phase >= 1) {
    const count = phase * 2 + 2 + Math.min(state.graftedTendrils, 4);
    ctx.strokeStyle =
      phase >= 2
        ? rgba(PALETTE.driedClot, 0.24 + distortion * 0.30)
        : rgba(PALETTE.fractureOutline, 0.56);
    ctx.lineWidth = 1.3 + phase * 0.45;
    ctx.lineCap = 'round';
    for (let i = 0; i < count; i += 1) {
      const angle = i * ((2 * Math.PI) / count) + (breath - 0.5) * 0.08;
      const reach = Math.min(width, height) * (0.36 + phase * 0.11 + breath * 0.03);
      const midX = valveCenter.x + Math.cos(angle + 0.14) * reach * 0.52;
      const midY = valveCenter.y + Math.sin(angle - 0.12) * reach * 0.52;
      const endX = valveCenter.x + Math.cos(angle) * reach;
      const endY = valveCenter.y + Math.sin(angle) * reach;
      ctx.beginPath();
      ctx.moveTo(valveCenter.x, valveCenter.y);
      ctx.quadraticCurveTo(midX, midY, endX, endY);
      ctx.stroke();
    }
  }

  // 10. Suspended Weeping Lamps hanging from the ceiling
  const lampCount = creatureCount(state, CREATURE_TYPES.WEEPING_LAMP.id);
  if (lampCount > 0) {
    drawWeepingLampsCeiling(ctx, width, lampCount, state.isStillnessActive, breath);
  }
}

function drawWeepingLampsCeiling(ctx, width, lampCount, isStillnessActive, breath) {
  const visible = Math.min(lampCount, 5);
  const spacing = width / (visible + 1);

  for (let i = 1; i <= visible; i += 1) {
    const anchorX = spacing * i;
    const sway = Math.sin(breath * 2 * Math.PI + i * 0.95) * 6.5;
    const cordLen = 24 + (i % 2) * 8;
    const sx = anchorX + sway;
    const sy = cordLen;

    // Iron hook + swaying braided cord
    ctx.fillStyle = PALETTE.fractureOutline;
    ctx.beginPath();
    ctx.arc(anchorX, 2, 2.2, 0, Math.PI * 2);
    ctx.fill();

    ctx.beginPath();
    ctx.moveTo(anchorX, 2);
    ctx.quadraticCurveTo(anchorX + sway * 0.35, cordLen * 0.55, sx, sy);
    ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.85);
    ctx.lineWidth = 2.4;
    ctx.stroke();
    ctx.strokeStyle = rgba(PALETTE.fractureOutline, 0.9);
    ctx.lineWidth = 1.2;
    ctx.stroke();

    // Sickly lichen Stillness glow halo
    if (isStillnessActive) {
      const haloR = 26 + breath * 5;
      const halo = ctx.createRadialGradient(sx, sy + 12, 0, sx, sy + 12, haloR);
      halo.addColorStop(0, rgba(PALETTE.paleLichen, 0.32));
      halo.addColorStop(1, 'transparent');
      ctx.fillStyle = halo;
      ctx.beginPath();
      ctx.arc(sx, sy + 12, haloR, 0, Math.PI * 2);
      ctx.fill();
    }

    // Tarnished collar
    ctx.fillStyle = PALETTE.chitinSurface;
    ctx.strokeStyle = PALETTE.fractureOutline;
    ctx.lineWidth = 0.9;
    ctx.fillRect(sx - 4, sy - 1.5, 8, 4.5);
    ctx.strokeRect(sx - 4, sy - 1.5, 8, 4.5);

    // Organic bell glass vessel
    const vTop = sy + 3;
    const vBot = vTop + 19;
    ctx.beginPath();
    ctx.moveTo(sx - 3.4, vTop);
    ctx.bezierCurveTo(sx - 9, vTop + 6, sx - 9.5, vBot - 2, sx, vBot);
    ctx.bezierCurveTo(sx + 9.5, vBot - 2, sx + 9, vTop + 6, sx + 3.4, vTop);
    ctx.closePath();

    const bulbFill = ctx.createRadialGradient(sx - 1.5, vTop + 9, 1, sx - 1.5, vTop + 9, 12);
    bulbFill.addColorStop(
      0,
      isStillnessActive ? rgba(PALETTE.paleLichen, 0.78) : rgba(PALETTE.bileOlive, 0.42),
    );
    bulbFill.addColorStop(1, rgba(PALETTE.chitinSurface, 0.92));
    ctx.fillStyle = bulbFill;
    ctx.fill();
    ctx.strokeStyle = isStillnessActive
      ? rgba(PALETTE.paleLichen, 0.88)
      : rgba(PALETTE.fractureOutline, 0.8);
    ctx.lineWidth = 1;
    ctx.stroke();

    // Internal filament
    ctx.strokeStyle = isStillnessActive
      ? rgba(PALETTE.bleachedBone, 0.75)
      : rgba(PALETTE.fractureOutline, 0.6);
    ctx.beginPath();
    ctx.moveTo(sx, vTop + 1.5);
    ctx.lineTo(sx, vTop + 11);
    ctx.stroke();

    // Falling condensation droplet during Stillness
    if (isStillnessActive) {
      const progress = (breath + i * 0.27) % 1;
      const dropY = vBot + 3 + progress * 26;
      const alpha = Math.max(0.18, Math.min(0.92, 1 - progress * 0.72));
      ctx.fillStyle = rgba(PALETTE.paleLichen, alpha);
      ctx.beginPath();
      ctx.moveTo(sx, dropY - 4);
      ctx.quadraticCurveTo(sx - 2.6, dropY + 1, sx, dropY + 2.5);
      ctx.quadraticCurveTo(sx + 2.6, dropY + 1, sx, dropY - 4);
      ctx.closePath();
      ctx.fill();
    }
  }
}

export function drawValveAndHands(canvas, state, breath) {
  const { ctx, width, height } = syncCanvasSize(canvas);
  ctx.clearRect(0, 0, width, height);

  const cx = width / 2;
  const cy = height / 2;
  const minDim = Math.min(width, height);
  const distortion = Math.min(1, state.phase * 0.25);

  // Dashed outer calibration ring
  ctx.save();
  ctx.strokeStyle = rgba(PALETTE.hairlineAsh, 0.85);
  ctx.lineWidth = 1.1;
  ctx.setLineDash([9, 10]);
  ctx.beginPath();
  ctx.arc(cx, cy, minDim * (0.43 + breath * 0.015), 0, Math.PI * 2);
  ctx.stroke();
  ctx.restore();

  // Inner organic drifting vein halo
  const drift = (2 + 6 * distortion) * (breath - 0.5);
  ctx.strokeStyle = rgba(PALETTE.driedClot, 0.20 + distortion * 0.34);
  ctx.lineWidth = 1.3;
  ctx.beginPath();
  ctx.arc(cx + drift, cy - drift * 0.7, minDim * 0.35, 0, Math.PI * 2);
  ctx.stroke();

  const sutureCount = creatureCount(state, CREATURE_TYPES.SUTURE_HAND.id);
  const graftCount = state.graftedTendrils;
  if (sutureCount <= 0 && graftCount <= 0) return;

  const valveRimRadius = 64;
  const armStartRadius = minDim * 0.495;

  // Braided Grafted Choir-Tendrils
  const visibleGrafts = Math.min(graftCount, 4);
  ctx.lineCap = 'round';
  for (let g = 0; g < visibleGrafts; g += 1) {
    const angle = g * (Math.PI / 2) + Math.PI / 4 + (breath - 0.5) * 0.1;
    const dx = Math.cos(angle);
    const dy = Math.sin(angle);
    const px = -dy;
    const py = dx;
    const startX = cx + dx * armStartRadius;
    const startY = cy + dy * armStartRadius;
    const midR = (armStartRadius + valveRimRadius) * 0.52;
    const endX = cx + dx * (valveRimRadius - 4);
    const endY = cy + dy * (valveRimRadius - 4);

    ctx.beginPath();
    ctx.moveTo(startX, startY);
    ctx.quadraticCurveTo(cx + dx * midR + px * 8.5, cy + dy * midR + py * 8.5, endX, endY);
    ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.88);
    ctx.lineWidth = 8;
    ctx.stroke();
    ctx.strokeStyle = rgba(PALETTE.driedClot, 0.92);
    ctx.lineWidth = 4.2;
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(startX - px * 2, startY - py * 2);
    ctx.quadraticCurveTo(cx + dx * midR - px * 6.5, cy + dy * midR - py * 6.5, endX, endY);
    ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.8);
    ctx.lineWidth = 5.5;
    ctx.stroke();
    ctx.strokeStyle = rgba(PALETTE.weatheredAsh, 0.72);
    ctx.lineWidth = 2.2;
    ctx.stroke();

    ctx.fillStyle = PALETTE.voidAbyss;
    ctx.beginPath();
    ctx.arc(endX, endY, 4.6, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = rgba(PALETTE.bleachedBone, 0.84);
    ctx.beginPath();
    ctx.arc(endX, endY, 3.1, 0, Math.PI * 2);
    ctx.fill();
  }

  // Shaded pale stitched Suture-Hands gripping the valve lip
  const visibleHands = Math.min(sutureCount, 8);
  for (let i = 0; i < visibleHands; i += 1) {
    const baseAngle = i * ((2 * Math.PI) / visibleHands) - Math.PI / 2 + Math.PI / 8;
    const dx = Math.cos(baseAngle);
    const dy = Math.sin(baseAngle);
    const px = -dy;
    const py = dx;

    const pressWave = (Math.sin(breath * 2 * Math.PI + i * 1.05) + 1) * 0.5;
    const pressInset = pressWave * 7;

    const wristX = cx + dx * armStartRadius;
    const wristY = cy + dy * armStartRadius;
    const palmR = valveRimRadius + 13.5 - pressInset * 0.65;
    const palmX = cx + dx * palmR;
    const palmY = cy + dy * palmR;
    const tipR = valveRimRadius - 5.5 - pressInset;

    const wristHalfW = 4.2;
    const palmHalfW = 7.4;

    ctx.beginPath();
    ctx.moveTo(wristX - px * wristHalfW, wristY - py * wristHalfW);
    ctx.quadraticCurveTo(
      (wristX + palmX) * 0.5 - px * wristHalfW * 0.85,
      (wristY + palmY) * 0.5 - py * wristHalfW * 0.85,
      palmX - px * palmHalfW,
      palmY - py * palmHalfW,
    );
    ctx.quadraticCurveTo(palmX - dx * 4, palmY - dy * 4, palmX + px * palmHalfW, palmY + py * palmHalfW);
    ctx.quadraticCurveTo(
      (wristX + palmX) * 0.5 + px * wristHalfW * 0.85,
      (wristY + palmY) * 0.5 + py * wristHalfW * 0.85,
      wristX + px * wristHalfW,
      wristY + py * wristHalfW,
    );
    ctx.closePath();

    ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.92);
    ctx.lineWidth = 3.6;
    ctx.lineJoin = 'round';
    ctx.stroke();

    const grad = ctx.createLinearGradient(wristX, wristY, palmX, palmY);
    grad.addColorStop(0, rgba(PALETTE.weatheredAsh, 0.82));
    grad.addColorStop(1, rgba(PALETTE.bleachedBone, 0.95));
    ctx.fillStyle = grad;
    ctx.fill();

    // Surgical cross-sutures + tiny dried-clot punctures
    for (let s = 1; s <= 3; s += 1) {
      const t = s * 0.24;
      const sx = wristX + (palmX - wristX) * t;
      const sy = wristY + (palmY - wristY) * t;
      const halfLen = 3.4 + s * 0.5;
      const p1x = sx - px * halfLen;
      const p1y = sy - py * halfLen;
      const p2x = sx + px * halfLen;
      const p2y = sy + py * halfLen;
      ctx.beginPath();
      ctx.moveTo(p1x, p1y);
      ctx.lineTo(p2x, p2y);
      ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.9);
      ctx.lineWidth = 2;
      ctx.stroke();
      ctx.strokeStyle = PALETTE.driedClot;
      ctx.lineWidth = 1.1;
      ctx.stroke();
      ctx.fillStyle = rgba(PALETTE.driedClot, 0.9);
      ctx.beginPath();
      ctx.arc(p1x, p1y, 1.1, 0, Math.PI * 2);
      ctx.arc(p2x, p2y, 1.1, 0, Math.PI * 2);
      ctx.fill();
    }

    // Three articulated pale fingers + knuckle beads + nails
    const spreads = [-6.2, 0, 6.2];
    const lengths = [0.9, 1.07, 0.88];
    for (let f = 0; f < 3; f += 1) {
      const sp = spreads[f];
      const baseX = palmX + px * (sp * 0.68);
      const baseY = palmY + py * (sp * 0.68);
      const kX = palmX - dx * 5.6 + px * (sp * 0.84);
      const kY = palmY - dy * 5.6 + py * (sp * 0.84);
      const tR = tipR - (f === 1 ? 2.6 : 0) + (1 - lengths[f]) * 7;
      const tX = cx + dx * tR + px * (sp * 0.52);
      const tY = cy + dy * tR + py * (sp * 0.52);

      ctx.beginPath();
      ctx.moveTo(baseX, baseY);
      ctx.lineTo(kX, kY);
      ctx.lineTo(tX, tY);
      ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.92);
      ctx.lineWidth = 4.5;
      ctx.stroke();
      ctx.strokeStyle = rgba(PALETTE.bleachedBone, 0.95);
      ctx.lineWidth = 2.5;
      ctx.stroke();

      ctx.fillStyle = rgba(PALETTE.weatheredAsh, 0.82);
      ctx.beginPath();
      ctx.arc(kX, kY, 1.5, 0, Math.PI * 2);
      ctx.fill();

      ctx.fillStyle = rgba(PALETTE.bleachedBone, 0.96);
      ctx.beginPath();
      ctx.arc(tX, tY, 1.25, 0, Math.PI * 2);
      ctx.fill();

      if (f === 1 && pressWave > 0.72) {
        const pulseAlpha = ((pressWave - 0.72) / 0.28) * 0.58;
        ctx.strokeStyle = rgba(PALETTE.driedClot, pulseAlpha);
        ctx.lineWidth = 1.2;
        ctx.beginPath();
        ctx.arc(tX, tY, 3.5 + pressWave * 5, 0, Math.PI * 2);
        ctx.stroke();
      }
    }
  }
}

export function drawLeechClerks(canvas, leechCount, breath) {
  const { ctx, width, height } = syncCanvasSize(canvas);
  ctx.clearRect(0, 0, width, height);
  if (leechCount <= 0) return;

  const visible = Math.min(leechCount, 6);
  for (let i = 0; i < visible; i += 1) {
    const onLeft = i % 2 === 0;
    const row = Math.floor(i / 2);
    const phaseAngle = breath * 2 * Math.PI + i * 1.15;
    const crawl = Math.sin(phaseAngle);
    const headX = onLeft ? 15 + crawl * 2.2 : width - 15 - crawl * 2.2;
    const baseY = height * (0.24 + row * 0.27);

    for (let seg = 6; seg >= 0; seg -= 1) {
      const dir = onLeft ? -1 : 1;
      const segWave = Math.sin(phaseAngle - seg * 0.62) * (1.8 + seg * 0.25);
      const sx = headX + dir * seg * 3.1;
      const sy = baseY + segWave;
      const r = seg === 0 ? 4.0 : seg <= 3 ? 4.4 - seg * 0.22 : Math.max(1.8, 3.6 - (seg - 3) * 0.55);

      ctx.fillStyle = rgba(PALETTE.voidAbyss, 0.88);
      ctx.beginPath();
      ctx.arc(sx, sy + 0.8, r + 1.2, 0, Math.PI * 2);
      ctx.fill();

      ctx.fillStyle = seg % 2 === 0 ? rgba(PALETTE.bileOlive, 0.94) : rgba(PALETTE.bileShadow, 0.96);
      ctx.beginPath();
      ctx.arc(sx, sy, r, 0, Math.PI * 2);
      ctx.fill();

      ctx.fillStyle = rgba(PALETTE.paleLichen, 0.32);
      ctx.beginPath();
      ctx.arc(sx - r * 0.22, sy - r * 0.32, r * 0.34, 0, Math.PI * 2);
      ctx.fill();
    }

    const mouthPulse = 1 + 0.14 * Math.sin(phaseAngle * 2);
    const mx = headX + (onLeft ? 2.2 : -2.2);
    ctx.fillStyle = rgba(PALETTE.driedClot, 0.65);
    ctx.beginPath();
    ctx.arc(mx, baseY, 4.6 * mouthPulse, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = rgba(PALETTE.voidAbyss, 0.92);
    ctx.beginPath();
    ctx.arc(mx, baseY, 2.0 * mouthPulse, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = rgba(PALETTE.paleLichen, 0.55);
    ctx.lineWidth = 0.9;
    ctx.beginPath();
    ctx.arc(mx, baseY, 4.6 * mouthPulse, 0, Math.PI * 2);
    ctx.stroke();
  }
}

export function drawWeepingLampPortrait(canvas, isWeeping) {
  const { ctx, width, height } = syncCanvasSize(canvas);
  ctx.clearRect(0, 0, width, height);

  const cx = width * 0.5;
  const socketY = height * 0.24;
  const botY = height * 0.73;

  const halo = ctx.createRadialGradient(cx, height * 0.5, 0, cx, height * 0.5, width * 0.48);
  halo.addColorStop(0, rgba(PALETTE.paleLichen, isWeeping ? 0.40 : 0.18));
  halo.addColorStop(1, 'transparent');
  ctx.fillStyle = halo;
  ctx.fillRect(0, 0, width, height);

  ctx.strokeStyle = PALETTE.fractureOutline;
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.moveTo(cx, 0);
  ctx.lineTo(cx, socketY);
  ctx.stroke();

  ctx.fillStyle = PALETTE.chitinSurface;
  ctx.strokeStyle = PALETTE.fractureOutline;
  ctx.lineWidth = 1;
  ctx.fillRect(cx - 5.5, socketY - 3, 11, 6.5);
  ctx.strokeRect(cx - 5.5, socketY - 3, 11, 6.5);

  ctx.beginPath();
  ctx.moveTo(cx - 4.5, socketY + 3);
  ctx.bezierCurveTo(cx - 12, height * 0.42, cx - 14, height * 0.66, cx, botY);
  ctx.bezierCurveTo(cx + 14, height * 0.66, cx + 12, height * 0.42, cx + 4.5, socketY + 3);
  ctx.closePath();

  const fill = ctx.createRadialGradient(cx - 2, height * 0.48, 1, cx - 2, height * 0.48, 18);
  fill.addColorStop(0, isWeeping ? rgba(PALETTE.paleLichen, 0.72) : rgba(PALETTE.bileOlive, 0.62));
  fill.addColorStop(0.75, rgba(PALETTE.chitinSurface, 0.92));
  fill.addColorStop(1, PALETTE.voidAbyss);
  ctx.fillStyle = fill;
  ctx.fill();
  ctx.strokeStyle = isWeeping ? rgba(PALETTE.paleLichen, 0.92) : rgba(PALETTE.fractureOutline, 0.9);
  ctx.lineWidth = 1.1;
  ctx.stroke();

  ctx.strokeStyle = isWeeping ? rgba(PALETTE.bleachedBone, 0.78) : rgba(PALETTE.paleLichen, 0.62);
  ctx.beginPath();
  ctx.moveTo(cx, socketY + 4);
  ctx.lineTo(cx, height * 0.56);
  ctx.stroke();

  ctx.strokeStyle = rgba(PALETTE.paleLichen, 0.55);
  ctx.beginPath();
  ctx.moveTo(cx - 7, height * 0.58);
  ctx.lineTo(cx + 7, height * 0.58);
  ctx.stroke();

  const dropTop = botY + 2;
  const dropBot = height * 0.9;
  ctx.fillStyle = isWeeping ? PALETTE.paleLichen : rgba(PALETTE.bileOlive, 0.85);
  ctx.beginPath();
  ctx.moveTo(cx, dropTop);
  ctx.quadraticCurveTo(cx - 3.6, dropBot, cx, dropBot + 1.5);
  ctx.quadraticCurveTo(cx + 3.6, dropBot, cx, dropTop);
  ctx.closePath();
  ctx.fill();
}

export function drawAnomalyGlyph(canvas, type, breath) {
  const { ctx, width, height } = syncCanvasSize(canvas);
  ctx.clearRect(0, 0, width, height);
  const cx = width / 2;
  const cy = height / 2;

  if (type === 'pale_tooth') {
    const lift = breath * 2;
    ctx.fillStyle = rgba(PALETTE.voidAbyss, 0.92);
    ctx.beginPath();
    ctx.ellipse(cx, height * 0.70, 9, 3.5, 0, 0, Math.PI * 2);
    ctx.fill();

    ctx.strokeStyle = PALETTE.fractureOutline;
    ctx.lineWidth = 1.3;
    ctx.beginPath();
    ctx.moveTo(2, height * 0.72);
    ctx.lineTo(cx - 6, height * 0.68);
    ctx.lineTo(cx, height * 0.79);
    ctx.lineTo(cx + 6, height * 0.66);
    ctx.lineTo(width - 2, height * 0.70);
    ctx.stroke();

    const topY = cy - 9 - lift;
    const midY = cy + 1 - lift;
    const rootY = cy + 7.5 - lift * 0.4;
    ctx.beginPath();
    ctx.moveTo(cx - 6.5, topY + 2.5);
    ctx.quadraticCurveTo(cx - 4.5, topY - 1.5, cx - 1.2, topY + 1.4);
    ctx.quadraticCurveTo(cx, topY + 2.4, cx + 1.2, topY + 1.4);
    ctx.quadraticCurveTo(cx + 4.5, topY - 1.5, cx + 6.5, topY + 2.5);
    ctx.quadraticCurveTo(cx + 7, midY, cx + 4.2, rootY);
    ctx.lineTo(cx + 1.6, midY + 2);
    ctx.lineTo(cx, midY + 1);
    ctx.lineTo(cx - 1.6, midY + 2);
    ctx.lineTo(cx - 4.2, rootY);
    ctx.quadraticCurveTo(cx - 7, midY, cx - 6.5, topY + 2.5);
    ctx.closePath();

    const g = ctx.createLinearGradient(0, topY, 0, rootY);
    g.addColorStop(0, PALETTE.bleachedBone);
    g.addColorStop(0.6, PALETTE.weatheredAsh);
    g.addColorStop(1, PALETTE.chitinSurface);
    ctx.fillStyle = g;
    ctx.fill();
    ctx.strokeStyle = rgba(PALETTE.fractureOutline, 0.85);
    ctx.lineWidth = 1;
    ctx.stroke();
  } else if (type === 'glass_eye') {
    const eyeH = 12 * (0.45 + 0.55 * (1 - breath * 0.4));
    ctx.fillStyle = '#232924';
    ctx.strokeStyle = PALETTE.paleLichen;
    ctx.lineWidth = 1.2;
    ctx.beginPath();
    ctx.ellipse(cx, cy, 12, eyeH / 2, 0, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();

    ctx.fillStyle = PALETTE.bleachedBone;
    ctx.beginPath();
    ctx.arc(cx + (breath - 0.5) * 3, cy, 3.2, 0, Math.PI * 2);
    ctx.fill();
  } else if (type === 'swollen_nodule') {
    const r = 8.5 + breath * 2.5;
    ctx.fillStyle = PALETTE.clotShadow;
    ctx.strokeStyle = PALETTE.driedClot;
    ctx.lineWidth = 1.4;
    ctx.beginPath();
    ctx.arc(cx, cy, r, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();
    ctx.fillStyle = rgba(PALETTE.driedClot, 0.55);
    ctx.beginPath();
    ctx.arc(cx - 2, cy - 1.5, r * 0.42, 0, Math.PI * 2);
    ctx.fill();
  }
}
