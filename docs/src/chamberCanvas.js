/**
 * High-DPI HTML5 Canvas renderers mirroring the Android Jetpack Compose Canvas artwork:
 * - Phase 0..4 mutating chamber wall backdrop (tile warping, corner seepage, olive streaks,
 *   bruised tissue pockets, Phase 4 chitinous rib arches, stitched Scar Tissue seams, fissures)
 * - Suspended organic pear/bell Weeping Lamps with Stillness glow & falling condensation tears
 * - Shaded anatomical Suture-Hands & intertwined Grafted Choir-Tendrils gripping the valve collar
 * - 7-segment undulating Leech-Clerks clinging to the recessed counter glass edges
 * - Weeping Lamp specimen portrait & anatomical Wall Anomaly glyphs
 */

import { CREATURE_TYPES, creatureCount } from './engine.js?v=2.2';

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

const HEX_RGB_CACHE = new Map();
const CANVAS_SIZE_CACHE = new WeakMap();
const CANVAS_RESIZE_OBSERVER =
  typeof ResizeObserver === 'function'
    ? new ResizeObserver((entries) => {
        for (const entry of entries) {
          const canvas = entry.target;
          const rect = entry.contentRect;
          if (rect.width > 0 && rect.height > 0) {
            CANVAS_SIZE_CACHE.set(canvas, {
              width: Math.max(1, Math.round(rect.width)),
              height: Math.max(1, Math.round(rect.height)),
            });
          } else {
            CANVAS_SIZE_CACHE.delete(canvas);
          }
        }
      })
    : null;

function rgba(hex, alpha) {
  let rgb = HEX_RGB_CACHE.get(hex);
  if (rgb === undefined) {
    const n = parseInt(hex.slice(1), 16);
    rgb = `${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}`;
    HEX_RGB_CACHE.set(hex, rgb);
  }
  return `rgba(${rgb}, ${alpha})`;
}

function syncCanvasSize(canvas) {
  const dpr = window.devicePixelRatio || 1;
  let cached = CANVAS_SIZE_CACHE.get(canvas);
  if (!cached) {
    const rect = canvas.getBoundingClientRect();
    const measuredW = Math.round(rect.width);
    const measuredH = Math.round(rect.height);
    if (measuredW > 0 && measuredH > 0) {
      cached = { width: measuredW, height: measuredH };
      CANVAS_SIZE_CACHE.set(canvas, cached);
      CANVAS_RESIZE_OBSERVER?.observe(canvas);
    } else {
      cached = { width: Math.max(1, measuredW), height: Math.max(1, measuredH) };
    }
  }
  const w = cached.width;
  const h = cached.height;
  const targetW = Math.round(w * dpr);
  const targetH = Math.round(h * dpr);
  if (canvas.width !== targetW || canvas.height !== targetH) {
    canvas.width = targetW;
    canvas.height = targetH;
  }
  const ctx = canvas.getContext('2d');
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  return { ctx, width: w, height: h };
}

export function drawChamberBackdrop(canvas, wallImage, state, breath, dropPhase = breath) {
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
    drawWeepingLampsCeiling(ctx, width, lampCount, state.isStillnessActive, breath, dropPhase);
  }

  // 11. Excision II-V Chamber Mutations
  drawExcisionChamberMutations(ctx, width, height, state, breath);
}

function drawExcisionChamberMutations(ctx, width, height, state, breath) {
  // Excision II: Floor Drain Grate & pooled sump runoff at bottom center
  if (state.highestExcisionMilestone >= 2) {
    const grateW = width * 0.42;
    const grateH = 14;
    const left = (width - grateW) * 0.5;
    const top = height - grateH - 6;

    const poolAlpha = 0.25 + (state.drainPressure / 100) * 0.45;
    const poolGrad = ctx.createRadialGradient(
      width * 0.5,
      height,
      0,
      width * 0.5,
      height,
      grateW * 0.72,
    );
    poolGrad.addColorStop(
      0,
      state.drainSealed ? rgba(PALETTE.driedClot, poolAlpha) : rgba(PALETTE.voidAbyss, poolAlpha),
    );
    poolGrad.addColorStop(1, 'transparent');
    ctx.fillStyle = poolGrad;
    ctx.fillRect(left - grateW * 0.3, top - 28, grateW * 1.6, 56);

    ctx.fillStyle = rgba(PALETTE.voidAbyss, 0.92);
    ctx.fillRect(left, top, grateW, grateH);
    ctx.strokeStyle = state.drainSealed
      ? rgba(PALETTE.driedClot, 0.9)
      : rgba(PALETTE.fractureOutline, 0.85);
    ctx.lineWidth = 1.2;
    ctx.strokeRect(left, top, grateW, grateH);

    for (let i = 1; i <= 7; i += 1) {
      const bx = left + grateW * (i / 8);
      ctx.beginPath();
      ctx.moveTo(bx, top + 1);
      ctx.lineTo(bx, top + grateH - 1);
      ctx.strokeStyle = rgba(PALETTE.hairlineAsh, 0.9);
      ctx.lineWidth = 1.5;
      ctx.stroke();
    }
  }

  // Excision III: Unbricked Neighbor Aperture slit in upper-right masonry
  if (state.highestExcisionMilestone >= 3) {
    const sx = width * 0.85;
    const sy = height * 0.16;
    const activeEcho = state.echoDiversionPercent > 0;
    ctx.fillStyle = rgba(PALETTE.voidAbyss, 0.96);
    ctx.fillRect(sx - 5, sy - 16, 10, 32);
    ctx.strokeStyle = activeEcho
      ? rgba(PALETTE.paleLichen, 0.6 + breath * 0.3)
      : rgba(PALETTE.fractureOutline, 0.8);
    ctx.lineWidth = 1.2;
    ctx.strokeRect(sx - 5, sy - 16, 10, 32);

    ctx.fillStyle = activeEcho
      ? rgba(PALETTE.paleLichen, 0.48 + breath * 0.3)
      : rgba(PALETTE.weatheredAsh, 0.28);
    ctx.beginPath();
    ctx.arc(sx, sy, 2.6, 0, Math.PI * 2);
    ctx.fill();
  }

  // Excision IV: Clamped Valve / Marrow Bloom crimson pressure pulse
  if (state.clampRemainingSeconds > 0 || state.marrowBloomRemainingSeconds > 0) {
    const pulseAlpha = state.marrowBloomRemainingSeconds > 0 ? 0.24 : 0.18;
    const grad = ctx.createRadialGradient(
      width * 0.5,
      height * 0.65,
      width * 0.12,
      width * 0.5,
      height * 0.65,
      Math.max(width, height) * 0.62,
    );
    grad.addColorStop(0, 'transparent');
    grad.addColorStop(1, rgba(PALETTE.driedClot, pulseAlpha + breath * 0.08));
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, width, height);
  }

  // Excision V: Pale creeping Ossified Throat membrane along top corners
  if (state.highestExcisionMilestone >= 5) {
    ctx.fillStyle = rgba(PALETTE.bleachedBone, 0.09 + breath * 0.04);
    ctx.strokeStyle = rgba(PALETTE.driedClot, 0.45);
    ctx.lineWidth = 1.2;

    ctx.beginPath();
    ctx.moveTo(0, 0);
    ctx.lineTo(width * 0.24, 0);
    ctx.quadraticCurveTo(width * 0.11, height * 0.12, 0, height * 0.19);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(width, 0);
    ctx.lineTo(width * 0.76, 0);
    ctx.quadraticCurveTo(width * 0.89, height * 0.12, width, height * 0.19);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();
  }
}

function drawWeepingLampsCeiling(ctx, width, lampCount, isStillnessActive, breath, dropPhase) {
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

    // Strictly downward-falling condensation droplet during Stillness
    if (isStillnessActive) {
      const progress = (dropPhase + i * 0.27) % 1;
      const fallNorm = Math.max(0, Math.min(1, (progress - 0.16) / 0.84));
      const gravityCurve = fallNorm * fallNorm;
      const dropY = vBot + 2 + gravityCurve * 30;
      const swellScale = progress < 0.16 ? 0.55 + (progress / 0.16) * 0.45 : 1;
      const stretchY = 1 + fallNorm * 0.35;
      const alpha =
        progress < 0.16
          ? 0.55 + (progress / 0.16) * 0.35
          : Math.max(0.1, Math.min(0.9, 1 - fallNorm * 0.85));

      const halfW = 2.4 * swellScale;
      const topOff = 3.8 * swellScale * stretchY;
      const botOff = 2.4 * swellScale;
      ctx.fillStyle = rgba(PALETTE.paleLichen, alpha);
      ctx.beginPath();
      ctx.moveTo(sx, dropY - topOff);
      ctx.quadraticCurveTo(sx - halfW, dropY + botOff * 0.45, sx, dropY + botOff);
      ctx.quadraticCurveTo(sx + halfW, dropY + botOff * 0.45, sx, dropY - topOff);
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

  // Grotesque, emaciated 5-fingered stitched Suture-Hands clawing and drumming the valve rim
  const visibleHands = Math.min(sutureCount, 8);
  for (let i = 0; i < visibleHands; i += 1) {
    const baseAngle = i * ((2 * Math.PI) / visibleHands) - Math.PI / 2 + Math.PI / 8;
    const handedness = i % 2 === 0 ? 1 : -1;
    const dx = Math.cos(baseAngle);
    const dy = Math.sin(baseAngle);
    const px = -dy * handedness;
    const py = dx * handedness;

    const pressWave =
      (Math.sin(breath * 2 * Math.PI + i * 1.15) * 0.78 +
        Math.sin(breath * 6 * Math.PI + i * 2.3) * 0.22 +
        1) *
      0.5;
    const pressInset = pressWave * 6.8;

    const wristR = armStartRadius;
    const seamR = armStartRadius - 7.5;
    const palmR = valveRimRadius + 15.5 - pressInset * 0.55;
    const tipBaseR = valveRimRadius - 7.5 - pressInset;

    const wristX = cx + dx * wristR;
    const wristY = cy + dy * wristR;
    const seamX = cx + dx * seamR;
    const seamY = cy + dy * seamR;
    const palmX = cx + dx * palmR;
    const palmY = cy + dy * palmR;

    // 1. Raw torn-masonry bruise cavity at wrist root
    const woundGrad = ctx.createRadialGradient(wristX, wristY, 0, wristX, wristY, 12);
    woundGrad.addColorStop(0, rgba(PALETTE.voidAbyss, 0.96));
    woundGrad.addColorStop(0.55, rgba(PALETTE.clotShadow, 0.75));
    woundGrad.addColorStop(1, 'transparent');
    ctx.fillStyle = woundGrad;
    ctx.beginPath();
    ctx.arc(wristX, wristY, 12, 0, Math.PI * 2);
    ctx.fill();

    // 2. Gaunt asymmetric wrist-to-metacarpal palm silhouette
    const wristHalfW = 4.6;
    const pinchHalfW = 3.4;
    const palmLeftW = 8.4;
    const palmRightW = 7.2;
    const midR = (wristR + palmR) * 0.52;
    const midX = cx + dx * midR;
    const midY = cy + dy * midR;

    ctx.beginPath();
    ctx.moveTo(wristX - px * wristHalfW, wristY - py * wristHalfW);
    ctx.bezierCurveTo(
      midX - px * pinchHalfW,
      midY - py * pinchHalfW,
      palmX + dx * 5 - px * palmLeftW * 0.95,
      palmY + dy * 5 - py * palmLeftW * 0.95,
      palmX - px * palmLeftW,
      palmY - py * palmLeftW,
    );
    ctx.quadraticCurveTo(
      palmX - dx * 4.2 - px * 2,
      palmY - dy * 4.2 - py * 2,
      palmX - dx * 2.6 + px * 2.5,
      palmY - dy * 2.6 + py * 2.5,
    );
    ctx.quadraticCurveTo(
      palmX - dx * 1.2 + px * palmRightW * 0.82,
      palmY - dy * 1.2 + py * palmRightW * 0.82,
      palmX + px * palmRightW,
      palmY + py * palmRightW,
    );
    ctx.bezierCurveTo(
      palmX + dx * 5 + px * palmRightW * 0.88,
      palmY + dy * 5 + py * palmRightW * 0.88,
      midX + px * pinchHalfW * 1.15,
      midY + py * pinchHalfW * 1.15,
      wristX + px * wristHalfW,
      wristY + py * wristHalfW,
    );
    ctx.closePath();

    ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.96);
    ctx.lineWidth = 3.6;
    ctx.lineJoin = 'round';
    ctx.stroke();

    const grad = ctx.createLinearGradient(wristX, wristY, palmX - dx * 3, palmY - dy * 3);
    grad.addColorStop(0, rgba(PALETTE.clotShadow, 0.96));
    grad.addColorStop(0.32, rgba(PALETTE.bileOlive, 0.78));
    grad.addColorStop(0.66, rgba(PALETTE.weatheredAsh, 0.88));
    grad.addColorStop(1, rgba(PALETTE.bleachedBone, 0.94));
    ctx.fillStyle = grad;
    ctx.fill();

    // Sunken hollow shadow between dorsal tendons
    const hollowX = palmX + dx * 4.5;
    const hollowY = palmY + dy * 4.5;
    const hollowGrad = ctx.createRadialGradient(hollowX, hollowY, 0, hollowX, hollowY, 5.5);
    hollowGrad.addColorStop(0, rgba(PALETTE.voidAbyss, 0.44));
    hollowGrad.addColorStop(1, 'transparent');
    ctx.fillStyle = hollowGrad;
    ctx.beginPath();
    ctx.arc(hollowX, hollowY, 5.5, 0, Math.PI * 2);
    ctx.fill();

    // Exposed pale dorsal metacarpal tendons
    for (const off of [-5.2, -1.8, 1.8, 5.0]) {
      const tStartX = seamX + px * (off * 0.42);
      const tStartY = seamY + py * (off * 0.42);
      const tEndX = palmX - dx * 1.5 + px * off;
      const tEndY = palmY - dy * 1.5 + py * off;
      ctx.beginPath();
      ctx.moveTo(tStartX, tStartY);
      ctx.lineTo(tEndX, tEndY);
      ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.55);
      ctx.lineWidth = 1.9;
      ctx.stroke();
      ctx.strokeStyle = rgba(PALETTE.bleachedBone, 0.64);
      ctx.lineWidth = 0.85;
      ctx.stroke();
    }

    // 3. Raw jagged wrist incision + crooked black-clot X-sutures & loose thread end
    const seamLX = seamX - px * 5.2;
    const seamLY = seamY - py * 5.2;
    const seamMX = seamX + dx * 1.4 - px * 0.5;
    const seamMY = seamY + dy * 1.4 - py * 0.5;
    const seamRX = seamX + px * 5.2;
    const seamRY = seamY + py * 5.2;
    ctx.beginPath();
    ctx.moveTo(seamLX, seamLY);
    ctx.lineTo(seamMX, seamMY);
    ctx.lineTo(seamRX, seamRY);
    ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.96);
    ctx.lineWidth = 2.4;
    ctx.stroke();
    ctx.strokeStyle = rgba(PALETTE.driedClot, 0.94);
    ctx.lineWidth = 1.3;
    ctx.stroke();

    const stitchSlots = [-3.4, 0, 3.3];
    for (let s = 0; s < stitchSlots.length; s += 1) {
      const scX = seamX + px * stitchSlots[s];
      const scY = seamY + py * stitchSlots[s];
      const dA = 2.6 + (s % 2) * 0.6;
      const dX = 1.55;
      const a1x = scX + dx * dA - px * dX;
      const a1y = scY + dy * dA - py * dX;
      const a2x = scX - dx * dA + px * dX;
      const a2y = scY - dy * dA + py * dX;
      const b1x = scX + dx * dA + px * dX;
      const b1y = scY + dy * dA + py * dX;
      const b2x = scX - dx * dA - px * dX;
      const b2y = scY - dy * dA - py * dX;

      ctx.beginPath();
      ctx.moveTo(a1x, a1y);
      ctx.lineTo(a2x, a2y);
      ctx.moveTo(b1x, b1y);
      ctx.lineTo(b2x, b2y);
      ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.95);
      ctx.lineWidth = 1.7;
      ctx.stroke();
      ctx.strokeStyle = rgba(PALETTE.driedClot, 0.96);
      ctx.lineWidth = 0.95;
      ctx.stroke();

      ctx.fillStyle = rgba(PALETTE.driedClot, 0.92);
      ctx.beginPath();
      ctx.arc(a1x, a1y, 1.15, 0, Math.PI * 2);
      ctx.arc(a2x, a2y, 1.15, 0, Math.PI * 2);
      ctx.fill();
    }

    // Stray dangling black suture thread
    ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.9);
    ctx.lineWidth = 1.1;
    ctx.beginPath();
    ctx.moveTo(seamRX, seamRY);
    ctx.lineTo(
      seamRX + px * 3.8 + dx * (2.2 + pressWave * 1.4),
      seamRY + py * 3.8 + dy * (2.2 + pressWave * 1.4),
    );
    ctx.stroke();

    // 4. Opposable crooked thumb with pale transverse scar (echoes dlg_p0_suture_hand)
    const thumbTwitch = Math.sin(breath * 2 * Math.PI + i * 1.7 + 0.5) * 1.6;
    const tbX = palmX + dx * 5.2 - px * palmLeftW * 0.78;
    const tbY = palmY + dy * 5.2 - py * palmLeftW * 0.78;
    const tjX = palmX + dx * 0.6 - px * (palmLeftW + 4.4);
    const tjY = palmY + dy * 0.6 - py * (palmLeftW + 4.4);
    const ttX = palmX - dx * (5.6 + thumbTwitch) - px * (palmLeftW + 1.2);
    const ttY = palmY - dy * (5.6 + thumbTwitch) - py * (palmLeftW + 1.2);
    ctx.beginPath();
    ctx.moveTo(tbX, tbY);
    ctx.lineTo(tjX, tjY);
    ctx.lineTo(ttX, ttY);
    ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.95);
    ctx.lineWidth = 4.4;
    ctx.stroke();
    ctx.strokeStyle = rgba(PALETTE.bleachedBone, 0.92);
    ctx.lineWidth = 2.4;
    ctx.stroke();

    ctx.fillStyle = rgba(PALETTE.weatheredAsh, 0.88);
    ctx.beginPath();
    ctx.arc(tjX, tjY, 1.65, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = rgba(PALETTE.bleachedBone, 0.92);
    ctx.lineWidth = 0.95;
    ctx.beginPath();
    ctx.moveTo(tjX - dx * 1.3 - px * 1.1, tjY - dy * 1.3 - py * 1.1);
    ctx.lineTo(tjX + dx * 1.3 + px * 1.1, tjY + dy * 1.3 + py * 1.1);
    ctx.stroke();

    ctx.fillStyle = rgba(PALETTE.voidAbyss, 0.96);
    ctx.beginPath();
    ctx.arc(ttX, ttY, 1.55, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = rgba(PALETTE.driedClot, 0.85);
    ctx.beginPath();
    ctx.arc(ttX, ttY, 0.95, 0, Math.PI * 2);
    ctx.fill();

    // 5. Four gaunt crooked multi-jointed fingers with asynchronous twitching claws
    const spreads = [-6.2, -2.0, 2.3, 6.4];
    const lenBonuses = [1.5, 4.8, 2.4, -2.6];
    const crooks = [-1.6, 0.9, -1.2, 2.3];
    const widths = [2.15, 2.3, 2.1, 1.75];

    for (let f = 0; f < spreads.length; f += 1) {
      const sp = spreads[f];
      const cr = crooks[f];
      const fPhase = breath * 2 * Math.PI + i * 1.37 + f * 1.18;
      const fCurl = (Math.sin(fPhase) + 1) * 0.5;
      const fJitter = Math.sin(fPhase * 2 + i) * 0.9;

      const mcpInset = f === 1 || f === 2 ? 2.2 : 0.8;
      const mcpX = palmX - dx * mcpInset + px * (sp * 0.84);
      const mcpY = palmY - dy * mcpInset + py * (sp * 0.84);
      const pipX =
        mcpX - dx * (6.8 + lenBonuses[f] * 0.35) + px * (sp * 0.24 + cr + fJitter * 0.4);
      const pipY =
        mcpY - dy * (6.8 + lenBonuses[f] * 0.35) + py * (sp * 0.24 + cr + fJitter * 0.4);
      const dipX = pipX - dx * (5.6 + lenBonuses[f] * 0.35) + px * (-cr * 0.65 + fJitter);
      const dipY = pipY - dy * (5.6 + lenBonuses[f] * 0.35) + py * (-cr * 0.65 + fJitter);
      const tipR = tipBaseR - lenBonuses[f] - fCurl * 3.8;
      const tipX = cx + dx * tipR + px * (sp * 0.62 + cr * 0.35);
      const tipY = cy + dy * tipR + py * (sp * 0.62 + cr * 0.35);

      const w = widths[f];
      ctx.beginPath();
      ctx.moveTo(mcpX, mcpY);
      ctx.lineTo(pipX, pipY);
      ctx.lineTo(dipX, dipY);
      ctx.lineTo(tipX, tipY);
      ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.96);
      ctx.lineWidth = w + 2.1;
      ctx.stroke();

      const fGrad = ctx.createLinearGradient(mcpX, mcpY, tipX, tipY);
      fGrad.addColorStop(0, rgba(PALETTE.weatheredAsh, 0.9));
      fGrad.addColorStop(1, rgba(PALETTE.bleachedBone, 0.96));
      ctx.strokeStyle = fGrad;
      ctx.lineWidth = w;
      ctx.stroke();

      // Swollen bony knuckles
      ctx.fillStyle = rgba(PALETTE.weatheredAsh, 0.88);
      ctx.beginPath();
      ctx.arc(mcpX, mcpY, w * 0.72, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = rgba(PALETTE.bleachedBone, 0.92);
      ctx.beginPath();
      ctx.arc(pipX, pipY, w * 0.68, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = rgba(PALETTE.voidAbyss, 0.42);
      ctx.beginPath();
      ctx.arc(pipX, pipY, w * 0.32, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = rgba(PALETTE.weatheredAsh, 0.86);
      ctx.beginPath();
      ctx.arc(dipX, dipY, w * 0.56, 0, Math.PI * 2);
      ctx.fill();

      // Dark-stained hooked nail claw
      const nailX = tipX - dx * 2.1;
      const nailY = tipY - dy * 2.1;
      ctx.beginPath();
      ctx.moveTo(tipX, tipY);
      ctx.lineTo(nailX, nailY);
      ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.98);
      ctx.lineWidth = 2.0;
      ctx.stroke();
      ctx.strokeStyle = rgba(PALETTE.driedClot, 0.88);
      ctx.lineWidth = 1.0;
      ctx.stroke();

      if (fCurl > 0.76) {
        const smearAlpha = ((fCurl - 0.76) / 0.24) * 0.56;
        ctx.strokeStyle = rgba(PALETTE.driedClot, smearAlpha);
        ctx.lineWidth = 1.1;
        ctx.beginPath();
        ctx.arc(nailX, nailY, 2.8 + fCurl * 3.8, 0, Math.PI * 2);
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
    // Anchored at the inner glass plate bezel (32px outer clearance keeps tails 100% unclipped)
    const headX = onLeft ? 34 + crawl * 2.0 : width - 34 - crawl * 2.0;
    const baseY = height * (0.25 + row * 0.25);

    for (let seg = 6; seg >= 0; seg -= 1) {
      const dir = onLeft ? -1 : 1;
      const segWave = Math.sin(phaseAngle - seg * 0.62) * (1.7 + seg * 0.24);
      const sx = headX + dir * seg * 3.0;
      const sy = baseY + segWave;
      const r =
        seg === 0 ? 3.9 : seg <= 3 ? 4.3 - seg * 0.24 : Math.max(1.5, 3.4 - (seg - 3) * 0.58);

      ctx.fillStyle = rgba(PALETTE.voidAbyss, 0.88);
      ctx.beginPath();
      ctx.arc(sx, sy + 0.8, r + 1.1, 0, Math.PI * 2);
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
  } else if (type === 'chitin_rib') {
    const spread = 1.0 + breath * 1.6;
    ctx.fillStyle = rgba(PALETTE.clotShadow, 0.96);
    ctx.beginPath();
    ctx.ellipse(cx, cy, 6.5, 11, 0, 0, Math.PI * 2);
    ctx.fill();

    ctx.strokeStyle = rgba(PALETTE.driedClot, 0.72 + breath * 0.22);
    ctx.lineWidth = 2.2 + breath * 0.9;
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(cx, cy - 8.5);
    ctx.lineTo(cx, cy + 8.5);
    ctx.stroke();

    for (const s of [-1, 1]) {
      ctx.beginPath();
      ctx.moveTo(cx + s * 11.5, cy - 8.5);
      ctx.bezierCurveTo(
        cx + s * (3.0 + spread),
        cy - 5,
        cx + s * (2.2 + spread),
        cy + 5,
        cx + s * 10.5,
        cy + 9,
      );
      ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.94);
      ctx.lineWidth = 5.4;
      ctx.stroke();
      ctx.strokeStyle = rgba(PALETTE.bleachedBone, 0.92);
      ctx.lineWidth = 3.0;
      ctx.stroke();

      ctx.strokeStyle = rgba(PALETTE.driedClot, 0.85);
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(cx + s * (4.8 + spread * 0.6), cy - 1);
      ctx.lineTo(cx + s * (7.2 + spread * 0.6), cy + 1.4);
      ctx.stroke();
    }
  }
}

export function drawSumpSiphonPortrait(canvas, active) {
  const { ctx, width, height } = syncCanvasSize(canvas);
  ctx.clearRect(0, 0, width, height);
  const cx = width * 0.5;

  // Floor grate bars
  ctx.fillStyle = PALETTE.voidAbyss;
  ctx.fillRect(width * 0.14, height * 0.76, width * 0.72, height * 0.16);
  ctx.strokeStyle = PALETTE.fractureOutline;
  ctx.lineWidth = 1.4;
  for (let i = 0; i <= 4; i += 1) {
    const x = width * (0.2 + i * 0.15);
    ctx.beginPath();
    ctx.moveTo(x, height * 0.76);
    ctx.lineTo(x, height * 0.92);
    ctx.stroke();
  }

  // Ribbed intake hose
  ctx.beginPath();
  ctx.moveTo(cx - 5, height * 0.76);
  ctx.bezierCurveTo(cx - 12, height * 0.52, cx - 10, height * 0.24, cx, height * 0.16);
  ctx.bezierCurveTo(cx + 10, height * 0.24, cx + 12, height * 0.52, cx + 5, height * 0.76);
  ctx.closePath();

  const grad = ctx.createLinearGradient(0, height * 0.16, 0, height * 0.76);
  grad.addColorStop(0, active ? rgba(PALETTE.driedClot, 0.84) : rgba(PALETTE.bileOlive, 0.62));
  grad.addColorStop(0.55, rgba(PALETTE.chitinSurface, 0.95));
  grad.addColorStop(1, PALETTE.voidAbyss);
  ctx.fillStyle = grad;
  ctx.fill();
  ctx.strokeStyle = active ? rgba(PALETTE.paleLichen, 0.78) : rgba(PALETTE.fractureOutline, 0.85);
  ctx.lineWidth = 1.2;
  ctx.stroke();

  // Horizontal bellows ribs
  ctx.strokeStyle = rgba(PALETTE.weatheredAsh, 0.62);
  ctx.lineWidth = 1.1;
  for (let r = 0; r < 4; r += 1) {
    const ry = height * (0.32 + r * 0.11);
    ctx.beginPath();
    ctx.moveTo(cx - 8, ry);
    ctx.lineTo(cx + 8, ry);
    ctx.stroke();
  }

  // Dark gulping intake orifice
  ctx.fillStyle = PALETTE.voidAbyss;
  ctx.beginPath();
  ctx.arc(cx, height * 0.25, 4.2, 0, Math.PI * 2);
  ctx.fill();
  ctx.strokeStyle = rgba(PALETTE.bleachedBone, 0.65);
  ctx.lineWidth = 1;
  ctx.stroke();
}

const DESK_PALETTE = Object.freeze({
  slateVoid: '#0B0F13',
  deskDark: '#12181E',
  deskSurface: '#1B222A',
  rainSlate: '#2C3742',
  tungstenAmber: '#B89E68',
  paperBone: '#D2CFC5',
  caseRed: '#8F4646',
});

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
    const fall = Math.pow((dropPhase % 1.0), 2);
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

