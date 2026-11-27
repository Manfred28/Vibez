/**
 * Arc 1 Chamber high-DPI Canvas renderers:
 * - 5-stage progressive photographic biomechanical wall backdrop + Excision II-V mutations
 * - Flanking suspended Weeping Lamps with Stillness halos & falling condensation droplets
 * - Graft sacrifice animation overlay
 * - Bolted cast-iron valve flange, Choir-Lungs, Grafted Choir-Tendrils, and Suture-Hands
 * - 7-segment patrolling Leech-Clerks around the recessed counter bezel
 */

import { CREATURE_TYPES } from '../engine/constants.js?v=3.15';
import { creatureCount } from '../engine/arc1Math.js?v=3.15';
import {
  PALETTE,
  drawCoverImage,
  drawRoundedRectPath,
  rgba,
  syncCanvasSize,
} from './canvasShared.js?v=3.15';

export function computeWallLayerAlphas(phase, highestExcisionMilestone, cycle = 0) {
  const clampedPhase = Math.max(0, Math.min(4, Math.floor(phase || 0)));
  const phase1Tile =
    clampedPhase === 0
      ? 0.34
      : clampedPhase === 1
        ? 0.76
        : clampedPhase === 2
          ? 0.34
          : clampedPhase === 3
            ? 0.14
            : 0;
  const phase2Fleshy = clampedPhase === 2 ? 0.8 : clampedPhase === 3 ? 0.28 : 0;
  const phase3Muscle = clampedPhase === 3 ? 0.86 : clampedPhase >= 4 ? 0.26 : 0;
  const phase4Hollow = clampedPhase >= 4 ? 0.9 : 0;

  const ms = Math.max(0, Math.floor(highestExcisionMilestone || 0));
  const baseExcision =
    ms >= 6 ? 0.78 : ms === 5 ? 0.62 : ms === 4 ? 0.44 : ms === 3 ? 0.26 : ms === 2 && cycle >= 2 ? 0.12 : 0;
  const phaseScale = clampedPhase === 0 ? 0.42 : clampedPhase === 1 ? 0.58 : clampedPhase === 2 ? 0.78 : 1.0;
  const lateExcision = Math.max(0, Math.min(0.82, baseExcision * phaseScale));

  return { phase1Tile, phase2Fleshy, phase3Muscle, phase4Hollow, lateExcision };
}

export function drawChamberBackdrop(
  canvas,
  wallImagesOrPhase1,
  fleshyWallImageLegacy,
  state,
  breath,
  dropPhase = breath,
  wallCreep = 0,
  arterialPulse = 0,
  echoRipple = 0,
) {
  const { ctx, width, height } = syncCanvasSize(canvas);
  ctx.clearRect(0, 0, width, height);

  const wallImages =
    wallImagesOrPhase1 && typeof wallImagesOrPhase1 === 'object' && 'phase1' in wallImagesOrPhase1
      ? wallImagesOrPhase1
      : {
          phase1: wallImagesOrPhase1,
          phase2: fleshyWallImageLegacy,
          phase3: fleshyWallImageLegacy,
          phase4: fleshyWallImageLegacy,
          excisionLate: null,
        };

  const phase = state.phase;
  const valveCenter = { x: width * 0.5, y: height * 0.65 };
  const layers = computeWallLayerAlphas(phase, state.highestExcisionMilestone, state.cycle);

  // 1. Five-stage progressive photographic biomechanical wall textures
  if (layers.phase1Tile > 0) {
    drawCoverImage(ctx, wallImages.phase1, width, height, layers.phase1Tile, 1);
  }
  if (layers.phase2Fleshy > 0) {
    drawCoverImage(ctx, wallImages.phase2, width, height, layers.phase2Fleshy, 1);
  }
  if (layers.phase3Muscle > 0) {
    drawCoverImage(ctx, wallImages.phase3, width, height, layers.phase3Muscle, 1.015 + breath * 0.018);
  }
  if (layers.phase4Hollow > 0) {
    drawCoverImage(
      ctx,
      wallImages.phase4,
      width,
      height,
      layers.phase4Hollow,
      1.02 + breath * 0.018 + arterialPulse * 0.016,
    );
  }
  if (layers.lateExcision > 0) {
    drawCoverImage(
      ctx,
      wallImages.excisionLate,
      width,
      height,
      layers.lateExcision,
      1.01 + breath * 0.014,
    );
  }

  // 2. Atmospheric vertical vignette & central readability recess
  const midTints = [
    rgba(PALETTE.voidAbyss, 0.42),
    rgba(PALETTE.voidAbyss, 0.28),
    rgba(PALETTE.bileShadow, 0.18),
    rgba(PALETTE.clotShadow, 0.2),
    rgba(PALETTE.clotShadow, 0.24 + arterialPulse * 0.08),
  ];
  const veil = ctx.createLinearGradient(0, 0, 0, height);
  veil.addColorStop(0, rgba(PALETTE.voidAbyss, 0.72));
  veil.addColorStop(0.5, midTints[Math.min(phase, 4)]);
  veil.addColorStop(1, rgba(PALETTE.voidAbyss, 0.66));
  ctx.fillStyle = veil;
  ctx.fillRect(0, 0, width, height);

  // Subtle shadowed readability recess behind counter HUD
  const hudShadow = ctx.createRadialGradient(
    width * 0.5,
    height * 0.28,
    0,
    width * 0.5,
    height * 0.28,
    Math.min(width, height) * 0.52,
  );
  hudShadow.addColorStop(0, rgba(PALETTE.voidAbyss, 0.44));
  hudShadow.addColorStop(1, 'transparent');
  ctx.fillStyle = hudShadow;
  ctx.fillRect(0, 0, width, height);

  // 3. Phase 0 only: sterile overhead fluorescent falloff & whisper-thin slate architectural grout
  if (phase === 0) {
    const glow = ctx.createRadialGradient(width * 0.5, 0, 0, width * 0.5, 0, width * 0.68);
    glow.addColorStop(0, rgba(PALETTE.bleachedBone, 0.07));
    glow.addColorStop(1, 'transparent');
    ctx.fillStyle = glow;
    ctx.fillRect(0, 0, width, height);

    ctx.strokeStyle = rgba(PALETTE.hairlineAsh, 0.12);
    ctx.lineWidth = 1;
    const step = 42;
    for (let x = step; x < width; x += step) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, height);
      ctx.stroke();
    }
    for (let y = step; y < height; y += step) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(width, y);
      ctx.stroke();
    }
  }

  // 4. Phase 1+: Deep damp corner moisture seepage stains
  if (phase >= 1) {
    const r = Math.min(width, height) * (0.28 + phase * 0.045);
    const stain = phase >= 3 ? rgba(PALETTE.clotShadow, 0.36) : rgba(PALETTE.voidAbyss, 0.44);
    for (const [sx, sy] of [
      [0, height],
      [width, height * 0.88],
    ]) {
      const g = ctx.createRadialGradient(sx, sy, 0, sx, sy, r);
      g.addColorStop(0, stain);
      g.addColorStop(1, 'transparent');
      ctx.fillStyle = g;
      ctx.beginPath();
      ctx.arc(sx, sy, r, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  // 5. Phase 2+: Fine damp condensation glints sliding quietly down wet masonry
  if (phase >= 2) {
    const streakAlpha = phase >= 4 ? 0.1 : 0.15;
    ctx.lineCap = 'round';
    for (let s = 1; s <= 4; s += 1) {
      const sx = width * (0.12 + s * 0.19);
      const topY = height * (0.1 + (s % 2) * 0.16);
      const travel = phase >= 3 ? ((wallCreep + s * 0.27) % 1) * height * 0.24 : breath * height * 0.05;
      const endY = Math.min(height * 0.88, topY + height * 0.14 + travel);
      const grad = ctx.createLinearGradient(sx, topY, sx, endY);
      grad.addColorStop(0, 'transparent');
      grad.addColorStop(0.5, rgba(PALETTE.paleLichen, streakAlpha));
      grad.addColorStop(1, rgba(PALETTE.paleLichen, streakAlpha * 1.4));
      ctx.strokeStyle = grad;
      ctx.lineWidth = 1.0;
      ctx.beginPath();
      ctx.moveTo(sx, topY);
      ctx.lineTo(sx, endY);
      ctx.stroke();
      ctx.fillStyle = rgba(PALETTE.paleLichen, streakAlpha * 1.6);
      ctx.beginPath();
      ctx.arc(sx, endY, 1.4, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  const creepAngle = wallCreep * Math.PI * 2;

  // 6. Phase 3+: Soft volumetric peristaltic subsurface swells & wet pore glints along outer flanks
  if (phase >= 3) {
    for (let p = 0; p < 4; p += 1) {
      const phaseShift = (wallCreep + p * 0.25) % 1;
      const swell = Math.sin(phaseShift * Math.PI);
      const px = p % 2 === 0 ? width * (0.1 + swell * 0.04) : width * (0.9 - swell * 0.04);
      const py = height * (0.22 + phaseShift * 0.56);
      const haloR = 16 + swell * 12 + arterialPulse * 6;
      const swellGrad = ctx.createRadialGradient(px, py, 0, px, py, haloR);
      swellGrad.addColorStop(0, rgba(PALETTE.driedClot, (0.14 + arterialPulse * 0.1) * swell));
      swellGrad.addColorStop(0.55, rgba(PALETTE.voidAbyss, 0.22 * swell));
      swellGrad.addColorStop(1, 'transparent');
      ctx.fillStyle = swellGrad;
      ctx.beginPath();
      ctx.arc(px, py, haloR, 0, Math.PI * 2);
      ctx.fill();

      const beadOff = Math.sin(creepAngle + p * 1.3) * 1.5;
      ctx.fillStyle = rgba(PALETTE.bleachedBone, 0.16 * swell);
      ctx.beginPath();
      ctx.arc(px + beadOff, py - 1.5, 1.4, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  // 7. Phase 4: Soft subsurface cardiac arterial illumination (no harsh wireframe circle)
  if (phase >= 4 && arterialPulse > 0.03) {
    const waveR = Math.min(width, height) * (0.26 + (1 - arterialPulse) * 0.44);
    const pulseGrad = ctx.createRadialGradient(
      valveCenter.x,
      valveCenter.y,
      0,
      valveCenter.x,
      valveCenter.y,
      waveR,
    );
    pulseGrad.addColorStop(0, rgba(PALETTE.driedClot, arterialPulse * 0.24));
    pulseGrad.addColorStop(0.55, rgba(PALETTE.clotShadow, arterialPulse * 0.14));
    pulseGrad.addColorStop(1, 'transparent');
    ctx.fillStyle = pulseGrad;
    ctx.fillRect(0, 0, width, height);
  }

  // 8. Volumetric muscular arterial root-cords anchoring the recessed valve into the living wall (Phases 2..4)
  if (phase >= 2) {
    const cordCount = Math.min(10, 4 + phase + Math.min(state.graftedTendrils, 3));
    const rootStartR = 60;
    const maxReach = Math.min(width, height) * (0.25 + (phase - 1) * 0.065 + arterialPulse * 0.025);
    const bodyAlpha = Math.min(0.86, 0.48 + (phase - 2) * 0.16 + arterialPulse * 0.14);
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';

    for (let i = 0; i < cordCount; i += 1) {
      const baseAng = i * ((2 * Math.PI) / cordCount) + 0.35;
      const sway = Math.sin(creepAngle + i * 1.37) * 0.055 + (breath - 0.5) * 0.04;
      const ang = baseAng + sway;
      const dx = Math.cos(ang);
      const dy = Math.sin(ang);
      const px = -dy;
      const py = dx;
      const bendDir = i % 2 === 0 ? 1 : -1;
      const cordReach = maxReach * (0.78 + (i % 3) * 0.14);

      const sx = valveCenter.x + dx * rootStartR;
      const sy = valveCenter.y + dy * rootStartR;
      const c1x = valveCenter.x + dx * (rootStartR + cordReach * 0.38) + px * (11 * bendDir);
      const c1y = valveCenter.y + dy * (rootStartR + cordReach * 0.38) + py * (11 * bendDir);
      const c2x = valveCenter.x + dx * (rootStartR + cordReach * 0.72) - px * (7 * bendDir);
      const c2y = valveCenter.y + dy * (rootStartR + cordReach * 0.72) - py * (7 * bendDir);
      const ex = valveCenter.x + dx * (rootStartR + cordReach);
      const ey = valveCenter.y + dy * (rootStartR + cordReach);

      const thickness = 2.8 + (phase - 2) * 1.1 + arterialPulse * 0.9;

      // Recessed cavity shadow
      const shGrad = ctx.createLinearGradient(sx, sy, ex, ey);
      shGrad.addColorStop(0, rgba(PALETTE.voidAbyss, 0.85));
      shGrad.addColorStop(0.55, rgba(PALETTE.voidAbyss, 0.52));
      shGrad.addColorStop(1, 'transparent');
      ctx.beginPath();
      ctx.moveTo(sx, sy);
      ctx.bezierCurveTo(c1x, c1y, c2x, c2y, ex, ey);
      ctx.strokeStyle = shGrad;
      ctx.lineWidth = thickness * 2.2;
      ctx.stroke();

      // Bruised muscular oxblood body
      const bodyGrad = ctx.createLinearGradient(sx, sy, ex, ey);
      bodyGrad.addColorStop(0, rgba(PALETTE.clotShadow, bodyAlpha));
      bodyGrad.addColorStop(0.5, rgba(PALETTE.driedClot, bodyAlpha * 0.78));
      bodyGrad.addColorStop(1, 'transparent');
      ctx.strokeStyle = bodyGrad;
      ctx.lineWidth = thickness;
      ctx.stroke();

      // Glossy wet specular ridge
      const hiGrad = ctx.createLinearGradient(sx, sy, ex, ey);
      hiGrad.addColorStop(0, rgba(PALETTE.bleachedBone, 0.16 + arterialPulse * 0.1));
      hiGrad.addColorStop(0.45, rgba(PALETTE.driedClot, 0.18));
      hiGrad.addColorStop(1, 'transparent');
      ctx.strokeStyle = hiGrad;
      ctx.lineWidth = Math.max(1, thickness * 0.28);
      ctx.stroke();
    }
  }

  // 9. Permanent Scar Tissue: Recessed organic keloid suture clefts tucked along chamber flanks (Cycles 1+)
  if (state.scarTissue > 0) {
    const cleftCount = Math.min(state.scarTissue, 4);
    const flankAnchors = [
      [0.02, 0.24, 1],
      [0.98, 0.3, -1],
      [0.02, 0.74, 1],
      [0.98, 0.7, -1],
    ];
    ctx.lineCap = 'round';
    for (let s = 0; s < cleftCount; s += 1) {
      const [fx, fy, inward] = flankAnchors[s];
      const ax = width * fx;
      const ay = height * fy;
      const len = width * 0.17;
      const endX = ax + inward * len;
      const endY = ay + height * (s % 2 === 0 ? 0.065 : -0.055);
      const midX = (ax + endX) * 0.5 + inward * 6;
      const midY = (ay + endY) * 0.5 - 8;

      const troughGrad = ctx.createLinearGradient(ax, ay, endX, endY);
      troughGrad.addColorStop(0, rgba(PALETTE.voidAbyss, 0.85));
      troughGrad.addColorStop(1, 'transparent');
      ctx.beginPath();
      ctx.moveTo(ax, ay);
      ctx.quadraticCurveTo(midX, midY, endX, endY);
      ctx.strokeStyle = troughGrad;
      ctx.lineWidth = 7;
      ctx.stroke();

      const seamGrad = ctx.createLinearGradient(ax, ay, endX, endY);
      seamGrad.addColorStop(0, rgba(PALETTE.driedClot, 0.62));
      seamGrad.addColorStop(1, 'transparent');
      ctx.strokeStyle = seamGrad;
      ctx.lineWidth = 2.0;
      ctx.stroke();

      for (let st = 1; st <= 4; st += 1) {
        const t = st / 5.2;
        const omt = 1 - t;
        const sx = omt * omt * ax + 2 * omt * t * midX + t * t * endX;
        const sy = omt * omt * ay + 2 * omt * t * midY + t * t * endY;
        const stapleAlpha = (1 - t * 0.75) * 0.58;
        ctx.strokeStyle = rgba(PALETTE.voidAbyss, stapleAlpha);
        ctx.lineWidth = 2.2;
        ctx.beginPath();
        ctx.moveTo(sx - 2, sy - 4.5);
        ctx.lineTo(sx + 2, sy + 4.5);
        ctx.stroke();

        ctx.strokeStyle = rgba(PALETTE.bleachedBone, stapleAlpha * 0.85);
        ctx.lineWidth = 1.0;
        ctx.beginPath();
        ctx.moveTo(sx - 1.8, sy - 4);
        ctx.lineTo(sx + 1.8, sy + 4);
        ctx.stroke();
      }
    }
  }

  // 10. Excision II-V Chamber Mutations & Flanking Weeping Lamps (behind foreground HUD so income stays unobstructed)
  drawExcisionChamberMutations(ctx, width, height, state, breath, wallCreep, echoRipple);

  const lampCount = creatureCount(state, CREATURE_TYPES.WEEPING_LAMP.id);
  if (lampCount > 0) {
    drawWeepingLampsCeiling(ctx, width, lampCount, state.isStillnessActive, breath, dropPhase);
  }
}

function drawExcisionChamberMutations(ctx, width, height, state, breath, wallCreep = 0, echoRipple = 0) {
  // Excision II+: Heavy recessed cast-iron & mortared floor sump basin along bottom threshold
  if (state.highestExcisionMilestone >= 2) {
    const pressureFrac = Math.max(0, Math.min(1, (state.drainPressure || 0) / 100));
    const basinW = Math.min(230, width * 0.5);
    const basinH = 22;
    const basinLeft = (width - basinW) * 0.5;
    const basinTop = height - basinH - 4;

    // Dark wet runoff pool seeping outward across the chamber floor
    const poolR = basinW * (0.64 + pressureFrac * 0.22);
    const poolGrad = ctx.createRadialGradient(width * 0.5, height, 0, width * 0.5, height, poolR);
    poolGrad.addColorStop(
      0,
      state.drainSealed
        ? rgba(PALETTE.driedClot, 0.28 + pressureFrac * 0.34)
        : rgba(PALETTE.voidAbyss, 0.86),
    );
    poolGrad.addColorStop(0.55, rgba(PALETTE.voidAbyss, 0.62));
    poolGrad.addColorStop(1, 'transparent');
    ctx.fillStyle = poolGrad;
    ctx.beginPath();
    ctx.arc(width * 0.5, height, poolR, 0, Math.PI * 2);
    ctx.fill();

    // Mortared stone outer bezel frame with recessed shadow lip
    const pad = 4;
    drawRoundedRectPath(ctx, basinLeft - pad, basinTop - pad, basinW + pad * 2, basinH + pad * 2, 7);
    ctx.fillStyle = rgba(PALETTE.voidAbyss, 0.94);
    ctx.fill();
    ctx.strokeStyle = rgba(PALETTE.fractureOutline, 0.52);
    ctx.lineWidth = 1.2;
    ctx.stroke();

    // Deep abyssal sump pit interior
    drawRoundedRectPath(ctx, basinLeft, basinTop, basinW, basinH, 5);
    const pitGrad = ctx.createLinearGradient(0, basinTop, 0, basinTop + basinH);
    pitGrad.addColorStop(0, PALETTE.voidAbyss);
    pitGrad.addColorStop(
      1,
      state.drainSealed
        ? rgba(PALETTE.driedClot, 0.22 + pressureFrac * 0.42)
        : rgba(PALETTE.voidAbyss, 0.96),
    );
    ctx.fillStyle = pitGrad;
    ctx.fill();

    // Animated viscous sump bubbles / hydraulic meniscus surging underneath the iron grate
    const bubblePhase = wallCreep * Math.PI * 2;
    for (let b = 0; b < 5; b += 1) {
      const bx = basinLeft + basinW * (0.14 + b * 0.18);
      const pulse = (Math.sin(bubblePhase + b * 1.45) + 1) * 0.5;
      const by = basinTop + basinH * (0.68 - pulse * 0.28 * (0.4 + pressureFrac));
      const br = 1.6 + pulse * (1.8 + pressureFrac * 2.2);
      ctx.fillStyle = state.drainSealed
        ? rgba(PALETTE.driedClot, 0.4 + pressureFrac * 0.45)
        : rgba(PALETTE.paleLichen, 0.16 + pulse * 0.16);
      ctx.beginPath();
      ctx.arc(bx, by, br, 0, Math.PI * 2);
      ctx.fill();

      ctx.fillStyle = rgba(PALETTE.bleachedBone, 0.18 * pulse);
      ctx.beginPath();
      ctx.arc(bx - br * 0.25, by - br * 0.25, br * 0.35, 0, Math.PI * 2);
      ctx.fill();
    }

    // Heavy forged cast-iron bars with metallic bevel highlight & center cross-beam
    const barCount = 7;
    ctx.lineCap = 'butt';
    for (let i = 1; i <= barCount; i += 1) {
      const bx = basinLeft + basinW * (i / (barCount + 1));
      ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.95);
      ctx.lineWidth = 4.2;
      ctx.beginPath();
      ctx.moveTo(bx, basinTop + 1);
      ctx.lineTo(bx, basinTop + basinH - 1);
      ctx.stroke();

      ctx.strokeStyle = rgba(PALETTE.fractureOutline, state.drainSealed ? 0.82 : 0.62);
      ctx.lineWidth = 2.0;
      ctx.beginPath();
      ctx.moveTo(bx - 0.6, basinTop + 1);
      ctx.lineTo(bx - 0.6, basinTop + basinH - 1);
      ctx.stroke();

      ctx.strokeStyle = rgba(PALETTE.bleachedBone, 0.18);
      ctx.lineWidth = 0.7;
      ctx.beginPath();
      ctx.moveTo(bx - 1.2, basinTop + 2);
      ctx.lineTo(bx - 1.2, basinTop + basinH - 2);
      ctx.stroke();
    }

    const midBarY = basinTop + basinH * 0.5;
    ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.92);
    ctx.lineWidth = 3.2;
    ctx.beginPath();
    ctx.moveTo(basinLeft + 2, midBarY);
    ctx.lineTo(basinLeft + basinW - 2, midBarY);
    ctx.stroke();
    ctx.strokeStyle = rgba(PALETTE.fractureOutline, 0.65);
    ctx.lineWidth = 1.1;
    ctx.beginPath();
    ctx.moveTo(basinLeft + 2, midBarY - 0.5);
    ctx.lineTo(basinLeft + basinW - 2, midBarY - 0.5);
    ctx.stroke();

    // Outer cast-iron flange rim + 4 forged hex rivets
    drawRoundedRectPath(ctx, basinLeft, basinTop, basinW, basinH, 5);
    ctx.strokeStyle = state.drainSealed
      ? rgba(PALETTE.driedClot, 0.72 + pressureFrac * 0.24)
      : rgba(PALETTE.fractureOutline, 0.78);
    ctx.lineWidth = 1.8;
    ctx.stroke();

    const rivets = [
      [basinLeft + 6, basinTop + 5],
      [basinLeft + basinW - 6, basinTop + 5],
      [basinLeft + 6, basinTop + basinH - 5],
      [basinLeft + basinW - 6, basinTop + basinH - 5],
    ];
    for (const [rx, ry] of rivets) {
      ctx.fillStyle = PALETTE.voidAbyss;
      ctx.beginPath();
      ctx.arc(rx, ry, 2.4, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = rgba(PALETTE.fractureOutline, 0.85);
      ctx.beginPath();
      ctx.arc(rx, ry, 1.5, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = rgba(PALETTE.bleachedBone, 0.28);
      ctx.beginPath();
      ctx.arc(rx - 0.4, ry - 0.4, 0.6, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  // Excision III+: Chiseled jagged Neighbor Aperture masonry breach in upper-right wall + Echo ripples
  if (state.highestExcisionMilestone >= 3) {
    const sx = width * 0.86;
    const sy = height * 0.22;
    const activeEcho = state.echoDiversionPercent > 0;

    const breachGlow = ctx.createRadialGradient(sx, sy, 0, sx, sy, 24);
    breachGlow.addColorStop(
      0,
      activeEcho ? rgba(PALETTE.paleLichen, 0.24 + breath * 0.16) : rgba(PALETTE.voidAbyss, 0.78),
    );
    breachGlow.addColorStop(1, 'transparent');
    ctx.fillStyle = breachGlow;
    ctx.beginPath();
    ctx.arc(sx, sy, 24, 0, Math.PI * 2);
    ctx.fill();

    ctx.beginPath();
    ctx.moveTo(sx - 5, sy - 20);
    ctx.lineTo(sx + 4, sy - 16);
    ctx.lineTo(sx + 6.5, sy - 4);
    ctx.lineTo(sx + 3.5, sy + 10);
    ctx.lineTo(sx + 5, sy + 20);
    ctx.lineTo(sx - 4, sy + 17);
    ctx.lineTo(sx - 6, sy + 3);
    ctx.lineTo(sx - 3, sy - 9);
    ctx.closePath();
    ctx.fillStyle = rgba(PALETTE.voidAbyss, 0.96);
    ctx.fill();
    ctx.strokeStyle = activeEcho
      ? rgba(PALETTE.paleLichen, 0.48 + breath * 0.28)
      : rgba(PALETTE.fractureOutline, 0.65);
    ctx.lineWidth = 1.4;
    ctx.stroke();

    const slitCore = ctx.createRadialGradient(sx, sy, 0, sx, sy, 6);
    slitCore.addColorStop(
      0,
      activeEcho ? rgba(PALETTE.paleLichen, 0.72 + breath * 0.22) : rgba(PALETTE.bleachedBone, 0.16),
    );
    slitCore.addColorStop(1, 'transparent');
    ctx.fillStyle = slitCore;
    ctx.beginPath();
    ctx.arc(sx, sy, 6, 0, Math.PI * 2);
    ctx.fill();

    if (echoRipple > 0 && echoRipple < 1) {
      const fade = 1 - echoRipple;
      for (let ring = 0; ring < 2; ring += 1) {
        const ringT = Math.max(0, echoRipple - ring * 0.16);
        if (ringT <= 0) continue;
        const r = 10 + ringT * (ring === 0 ? width * 0.32 : width * 0.2);
        ctx.strokeStyle = rgba(PALETTE.paleLichen, fade * (ring === 0 ? 0.62 : 0.36));
        ctx.lineWidth = ring === 0 ? 1.6 : 1.0;
        ctx.beginPath();
        ctx.arc(sx, sy, r, 0, Math.PI * 2);
        ctx.stroke();
      }
    }
  }

  // Excision IV+: Clamped Valve / Marrow Bloom crimson arterial tension pulse
  if (state.clampRemainingSeconds > 0 || state.marrowBloomRemainingSeconds > 0) {
    const pulseAlpha = state.marrowBloomRemainingSeconds > 0 ? 0.2 : 0.24;
    const grad = ctx.createRadialGradient(
      width * 0.5,
      height * 0.62,
      width * 0.12,
      width * 0.5,
      height * 0.62,
      Math.max(width, height) * 0.65,
    );
    grad.addColorStop(0, 'transparent');
    grad.addColorStop(1, rgba(PALETTE.driedClot, pulseAlpha + breath * 0.1));
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, width, height);
  }

  // Excision V+: Organic stretched calcified cartilage & translucent pale sinew webs in upper corners
  if (state.highestExcisionMilestone >= 5) {
    ctx.lineCap = 'round';
    for (const side of [-1, 1]) {
      const cornerX = side < 0 ? 0 : width;
      const spanX = width * 0.24 * -side;
      const spanY = height * 0.2;
      const stretch = breath * 4;

      ctx.beginPath();
      ctx.moveTo(cornerX, 0);
      ctx.lineTo(cornerX + spanX, 0);
      ctx.bezierCurveTo(
        cornerX + spanX * 0.55,
        spanY * 0.35 + stretch,
        cornerX + spanX * 0.28,
        spanY * 0.68 + stretch,
        cornerX,
        spanY,
      );
      ctx.closePath();
      const webGrad = ctx.createRadialGradient(cornerX, 0, 0, cornerX, 0, spanY * 1.15);
      webGrad.addColorStop(0, rgba(PALETTE.bleachedBone, 0.14 + breath * 0.04));
      webGrad.addColorStop(0.55, rgba(PALETTE.driedClot, 0.16));
      webGrad.addColorStop(1, 'transparent');
      ctx.fillStyle = webGrad;
      ctx.fill();

      for (let strand = 0; strand <= 2; strand += 1) {
        const sx = cornerX + spanX * (0.38 + strand * 0.26);
        const ey = spanY * (0.42 + strand * 0.25);
        const ctrlX = cornerX + spanX * (0.18 + strand * 0.1);
        const ctrlY = ey * 0.52 + stretch;
        ctx.beginPath();
        ctx.moveTo(sx, 0);
        ctx.quadraticCurveTo(ctrlX, ctrlY, cornerX, ey);
        ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.72);
        ctx.lineWidth = 3.4 - strand * 0.6;
        ctx.stroke();
        ctx.strokeStyle =
          strand === 1
            ? rgba(PALETTE.bleachedBone, 0.32 + breath * 0.08)
            : rgba(PALETTE.driedClot, 0.38 + breath * 0.08);
        ctx.lineWidth = 1.6 - strand * 0.25;
        ctx.stroke();
      }
    }
  }
}

export function drawForegroundLampsAndSacrifice(
  canvas,
  _state,
  _breath,
  _dropPhase,
  sacrificeProgress = 0,
  sacrificeKind = 'tendril',
) {
  const { ctx, width, height } = syncCanvasSize(canvas);
  ctx.clearRect(0, 0, width, height);

  if (sacrificeProgress > 0 && sacrificeProgress < 1) {
    drawGraftSacrificeOverlay(ctx, width, height, sacrificeProgress, sacrificeKind);
  }
}

function drawGraftSacrificeOverlay(ctx, width, height, progress, kind) {
  const cx = width * 0.5;
  const cy = height * 0.64;
  const fade = Math.max(0, 1 - Math.pow(progress, 1.5));
  const collapse = Math.pow(progress, 0.75);

  // Dark clot flash ring contracting toward valve
  const ringR = Math.max(8, (1 - collapse) * 92 + 16);
  ctx.strokeStyle = rgba(PALETTE.driedClot, fade * 0.68);
  ctx.lineWidth = 2.4 * fade;
  ctx.beginPath();
  ctx.arc(cx, cy, ringR, 0, Math.PI * 2);
  ctx.stroke();

  if (kind === 'choir_lung') {
    // Two shattering ceiling lamp vessels + severed tendril pulled inward
    for (const side of [-1, 1]) {
      const startX = cx + side * 86;
      const startY = height * 0.24;
      const px = startX + (cx - startX) * collapse;
      const py = startY + (cy - startY) * collapse;
      const jitter = Math.sin(progress * 28 + side) * 4 * (1 - progress);
      ctx.save();
      ctx.translate(px + jitter, py);
      ctx.rotate(side * progress * 1.4);
      // Cracked glass shards
      ctx.fillStyle = rgba(PALETTE.paleLichen, fade * 0.75);
      ctx.strokeStyle = rgba(PALETTE.bleachedBone, fade * 0.85);
      ctx.lineWidth = 1.1;
      for (let sh = 0; sh < 4; sh += 1) {
        const a = sh * 1.55 + progress * 2.5;
        const d = progress * 18;
        ctx.beginPath();
        ctx.moveTo(Math.cos(a) * d, Math.sin(a) * d);
        ctx.lineTo(Math.cos(a) * d + 5, Math.sin(a) * d + 2);
        ctx.lineTo(Math.cos(a) * d + 1, Math.sin(a) * d + 7);
        ctx.closePath();
        ctx.fill();
        ctx.stroke();
      }
      ctx.restore();
    }
  } else {
    // Two severed Suture-Hands & two curling Leech-Clerks dissolving into the valve
    const actors = [
      { sx: cx - 96, sy: cy - 26, type: 'hand', side: -1 },
      { sx: cx + 96, sy: cy - 22, type: 'hand', side: 1 },
      { sx: cx - 72, sy: height * 0.28, type: 'leech', side: -1 },
      { sx: cx + 72, sy: height * 0.28, type: 'leech', side: 1 },
    ];
    for (let i = 0; i < actors.length; i += 1) {
      const a = actors[i];
      const px = a.sx + (cx - a.sx) * collapse;
      const py = a.sy + (cy - a.sy) * collapse;
      const spasm = Math.sin(progress * 34 + i * 1.7) * 5.5 * (1 - progress);
      ctx.save();
      ctx.translate(px + spasm, py);
      ctx.rotate(a.side * progress * 2.2);
      if (a.type === 'hand') {
        // Curling pale finger silhouettes + snapping suture red wisps
        ctx.strokeStyle = rgba(PALETTE.bleachedBone, fade * 0.9);
        ctx.lineWidth = 2.0;
        for (let f = -1; f <= 1; f += 1) {
          ctx.beginPath();
          ctx.moveTo(0, 0);
          ctx.quadraticCurveTo(f * 6, -8 + progress * 5, f * 9 * (1 - progress * 0.4), -14);
          ctx.stroke();
        }
        ctx.strokeStyle = rgba(PALETTE.driedClot, fade * 0.9);
        ctx.lineWidth = 1.4;
        ctx.beginPath();
        ctx.moveTo(-6, 3);
        ctx.lineTo(6, -3);
        ctx.stroke();
      } else {
        // Curled olive leech body dissolving into dark segments
        for (let seg = 0; seg < 5; seg += 1) {
          const ang = seg * 0.65 + progress * 3.0;
          ctx.fillStyle =
            seg % 2 === 0
              ? rgba(PALETTE.bileOlive, fade * 0.92)
              : rgba(PALETTE.clotShadow, fade * 0.9);
          ctx.beginPath();
          ctx.arc(Math.cos(ang) * (6 - seg * 0.8), Math.sin(ang) * (5 - seg * 0.6), 3.2 - seg * 0.35, 0, Math.PI * 2);
          ctx.fill();
        }
      }
      ctx.restore();
    }
  }

  // Dark clot spatter droplets dispersing outward
  for (let d = 0; d < 10; d += 1) {
    const ang = d * ((Math.PI * 2) / 10) + 0.3;
    const dist = 14 + progress * (44 + (d % 3) * 14);
    ctx.fillStyle = rgba(PALETTE.driedClot, fade * 0.72);
    ctx.beginPath();
    ctx.arc(cx + Math.cos(ang) * dist, cy + Math.sin(ang) * dist, 2.2 * fade + 0.6, 0, Math.PI * 2);
    ctx.fill();
  }
}

/**
 * Staggered ceiling anchor slots for 1..5 visible Weeping Lamps.
 * Every slot stays strictly on the left (`<= 0.24`) or right (`>= 0.76`) flanks so no lamp,
 * Stillness halo, or condensation droplet ever sits over the center income readout or digits.
 */
export function getWeepingLampLayout(lampCount) {
  const visible = Math.min(Math.max(0, Math.floor(lampCount || 0)), 5);
  if (visible <= 0) return [];
  const layouts = {
    1: [{ x: 0.14, cord: 26 }],
    2: [
      { x: 0.13, cord: 26 },
      { x: 0.87, cord: 30 },
    ],
    3: [
      { x: 0.1, cord: 30 },
      { x: 0.21, cord: 16 },
      { x: 0.88, cord: 28 },
    ],
    4: [
      { x: 0.09, cord: 32 },
      { x: 0.21, cord: 16 },
      { x: 0.79, cord: 18 },
      { x: 0.91, cord: 32 },
    ],
    5: [
      { x: 0.08, cord: 24 },
      { x: 0.2, cord: 14 },
      { x: 0.14, cord: 62 },
      { x: 0.8, cord: 16 },
      { x: 0.92, cord: 34 },
    ],
  };
  return layouts[visible] ?? layouts[5];
}

function drawWeepingLampsCeiling(ctx, width, lampCount, isStillnessActive, breath, dropPhase) {
  const slots = getWeepingLampLayout(lampCount);

  for (let idx = 0; idx < slots.length; idx += 1) {
    const i = idx + 1;
    const anchorX = width * slots[idx].x;
    const sway = Math.sin(breath * 2 * Math.PI + i * 0.95) * 4.5;
    const cordLen = slots[idx].cord;
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

export function drawValveAndHands(canvas, state, breath, handCyclePhase = breath) {
  const { ctx, width, height } = syncCanvasSize(canvas);
  ctx.clearRect(0, 0, width, height);

  const cx = width / 2;
  const cy = height / 2;
  const minDim = Math.min(width, height);
  const distortion = Math.min(1, state.phase * 0.25);

  // 1. Recessed cast-iron mounting flange, 8 perimeter bolts, and bruised tissue collar (no CAD dashed circle)
  const socketRadius = minDim * 0.365;
  const flangeRadius = minDim * 0.415;
  const bruiseR = flangeRadius + 8 + distortion * 12 + breath * 4;
  const bruiseGrad = ctx.createRadialGradient(cx, cy, socketRadius * 0.6, cx, cy, bruiseR);
  bruiseGrad.addColorStop(0, rgba(PALETTE.voidAbyss, 0.9));
  bruiseGrad.addColorStop(0.58, rgba(PALETTE.clotShadow, 0.22 + distortion * 0.34));
  bruiseGrad.addColorStop(1, 'transparent');
  ctx.fillStyle = bruiseGrad;
  ctx.beginPath();
  ctx.arc(cx, cy, bruiseR, 0, Math.PI * 2);
  ctx.fill();

  ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.82);
  ctx.lineWidth = 7;
  ctx.beginPath();
  ctx.arc(cx, cy, flangeRadius, 0, Math.PI * 2);
  ctx.stroke();

  ctx.strokeStyle = rgba(PALETTE.hairlineAsh, 0.58);
  ctx.lineWidth = 1.2;
  ctx.beginPath();
  ctx.arc(cx, cy, flangeRadius + 2.5, 0, Math.PI * 2);
  ctx.stroke();

  ctx.strokeStyle = rgba(PALETTE.bleachedBone, 0.14);
  ctx.lineWidth = 1.0;
  ctx.beginPath();
  ctx.arc(cx, cy, flangeRadius - 2.5, 0, Math.PI * 2);
  ctx.stroke();

  for (let b = 0; b < 8; b += 1) {
    const ang = b * (Math.PI / 4) + Math.PI / 8;
    const bx = cx + Math.cos(ang) * flangeRadius;
    const by = cy + Math.sin(ang) * flangeRadius;
    ctx.fillStyle = rgba(PALETTE.voidAbyss, 0.94);
    ctx.beginPath();
    ctx.arc(bx, by, 3.2, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = rgba(PALETTE.fractureOutline, 0.72);
    ctx.beginPath();
    ctx.arc(bx, by, 2.0, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = rgba(PALETTE.bleachedBone, 0.22);
    ctx.beginPath();
    ctx.arc(bx - 0.6, by - 0.6, 0.8, 0, Math.PI * 2);
    ctx.fill();
  }

  ctx.strokeStyle = rgba(PALETTE.driedClot, 0.24 + distortion * 0.36 + breath * 0.08);
  ctx.lineWidth = 2.2 + distortion * 2.8 + breath * 1.2;
  ctx.beginPath();
  ctx.arc(cx, cy, socketRadius, 0, Math.PI * 2);
  ctx.stroke();

  const sutureCount = creatureCount(state, CREATURE_TYPES.SUTURE_HAND.id);
  const graftCount = state.graftedTendrils || 0;
  const choirLungCount = state.choirLungs || 0;
  if (sutureCount <= 0 && graftCount <= 0 && choirLungCount <= 0) return;

  const valveRimRadius = 64;
  const armStartRadius = minDim * 0.495;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';

  // 2. Grafted Choir-Lungs (M4+): visibly breathing translucent ribbed pleural sacs on outer diagonals
  const visibleLungs = Math.min(choirLungCount, 4);
  const lungAngles = [-Math.PI * 0.78, -Math.PI * 0.22, Math.PI * 0.78, Math.PI * 0.22];
  for (let l = 0; l < visibleLungs; l += 1) {
    const ang = lungAngles[l];
    const dx = Math.cos(ang);
    const dy = Math.sin(ang);
    const px = -dy;
    const py = dx;
    const inhaleWave = (Math.sin(breath * 2 * Math.PI + l * 1.15) + 1) * 0.5;
    const inflate = 1 + inhaleWave * (state.isStillnessActive ? 0.22 : 0.12);
    const sacR = valveRimRadius + 30;
    const sacX = cx + dx * sacR;
    const sacY = cy + dy * sacR;
    const rootX = cx + dx * (armStartRadius + 4);
    const rootY = cy + dy * (armStartRadius + 4);
    const linkX = cx + dx * (valveRimRadius - 2);
    const linkY = cy + dy * (valveRimRadius - 2);

    ctx.beginPath();
    ctx.moveTo(rootX, rootY);
    ctx.lineTo(linkX, linkY);
    ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.9);
    ctx.lineWidth = 5.2;
    ctx.stroke();
    ctx.strokeStyle = rgba(PALETTE.clotShadow, 0.88);
    ctx.lineWidth = 2.8;
    ctx.stroke();
    ctx.strokeStyle = rgba(PALETTE.driedClot, 0.52);
    ctx.lineWidth = 1.1;
    ctx.stroke();

    if (state.isStillnessActive) {
      const glowR = 20 * inflate;
      const gGrad = ctx.createRadialGradient(sacX, sacY, 0, sacX, sacY, glowR);
      gGrad.addColorStop(0, rgba(PALETTE.paleLichen, 0.24 + inhaleWave * 0.14));
      gGrad.addColorStop(1, 'transparent');
      ctx.fillStyle = gGrad;
      ctx.beginPath();
      ctx.arc(sacX, sacY, glowR, 0, Math.PI * 2);
      ctx.fill();
    }

    const lenHalf = 11.5 * inflate;
    const widHalf = 8.2 * inflate;
    ctx.beginPath();
    ctx.moveTo(sacX - dx * lenHalf, sacY - dy * lenHalf);
    ctx.bezierCurveTo(
      sacX - dx * (lenHalf * 0.4) + px * widHalf,
      sacY - dy * (lenHalf * 0.4) + py * widHalf,
      sacX + dx * (lenHalf * 0.55) + px * (widHalf * 1.15),
      sacY + dy * (lenHalf * 0.55) + py * (widHalf * 1.15),
      sacX + dx * lenHalf,
      sacY + dy * lenHalf,
    );
    ctx.bezierCurveTo(
      sacX + dx * (lenHalf * 0.55) - px * (widHalf * 1.15),
      sacY + dy * (lenHalf * 0.55) - py * (widHalf * 1.15),
      sacX - dx * (lenHalf * 0.4) - px * widHalf,
      sacY - dy * (lenHalf * 0.4) - py * widHalf,
      sacX - dx * lenHalf,
      sacY - dy * lenHalf,
    );
    ctx.closePath();

    ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.94);
    ctx.lineWidth = 3.6;
    ctx.stroke();

    const sacGrad = ctx.createRadialGradient(
      sacX - px * 2,
      sacY - py * 2,
      1,
      sacX,
      sacY,
      lenHalf * 1.2,
    );
    sacGrad.addColorStop(
      0,
      state.isStillnessActive ? rgba(PALETTE.paleLichen, 0.58) : rgba(PALETTE.bleachedBone, 0.42),
    );
    sacGrad.addColorStop(0.58, rgba(PALETTE.clotShadow, 0.86));
    sacGrad.addColorStop(1, rgba(PALETTE.voidAbyss, 0.92));
    ctx.fillStyle = sacGrad;
    ctx.fill();

    for (let rib = -1; rib <= 1; rib += 1) {
      const along = rib * (lenHalf * 0.44);
      const bandW = widHalf * (rib === 0 ? 0.92 : 0.7);
      const p1x = sacX + dx * along - px * bandW;
      const p1y = sacY + dy * along - py * bandW;
      const p2x = sacX + dx * along + px * bandW;
      const p2y = sacY + dy * along + py * bandW;
      ctx.beginPath();
      ctx.moveTo(p1x, p1y);
      ctx.lineTo(p2x, p2y);
      ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.72);
      ctx.lineWidth = 2.0;
      ctx.stroke();
      ctx.strokeStyle = rgba(PALETTE.bleachedBone, 0.52 + inhaleWave * 0.2);
      ctx.lineWidth = 0.95;
      ctx.stroke();
    }
  }

  // 3. Sculpted organic Grafted Choir-Tendrils: segmented bruised muscular stalks + helical bone-sinew + bony clasps
  const visibleGrafts = Math.min(graftCount, 4);
  for (let g = 0; g < visibleGrafts; g += 1) {
    const writhe = Math.sin(breath * 2 * Math.PI + g * 1.55);
    const angle = g * (Math.PI / 2) + Math.PI / 4 + writhe * 0.075;
    const dx = Math.cos(angle);
    const dy = Math.sin(angle);
    const px = -dy;
    const py = dx;
    const startX = cx + dx * armStartRadius;
    const startY = cy + dy * armStartRadius;

    // Raw bruised socket cavity at wall anchor
    const rootGrad = ctx.createRadialGradient(startX, startY, 0, startX, startY, 12);
    rootGrad.addColorStop(0, rgba(PALETTE.voidAbyss, 0.96));
    rootGrad.addColorStop(0.55, rgba(PALETTE.clotShadow, 0.74));
    rootGrad.addColorStop(1, 'transparent');
    ctx.fillStyle = rootGrad;
    ctx.beginPath();
    ctx.arc(startX, startY, 12, 0, Math.PI * 2);
    ctx.fill();

    const segments = 9;
    const spinePts = [];
    const helixPts = [];
    let prevX = startX;
    let prevY = startY;
    for (let s = 0; s <= segments; s += 1) {
      const t = s / segments;
      const r = armStartRadius + (valveRimRadius - 4 - armStartRadius) * t;
      const wave = Math.sin(t * Math.PI * 1.45 + breath * 2 * Math.PI + g * 1.2) * (1 - t * 0.45) * 6.5;
      const ptX = cx + dx * r + px * wave;
      const ptY = cy + dy * r + py * wave;
      const helixOff = Math.sin(t * Math.PI * 3.0 + g * 1.4) * (4.4 - t * 1.6);
      spinePts.push([ptX, ptY]);
      helixPts.push([ptX + px * helixOff, ptY + py * helixOff]);

      if (s > 0) {
        const segRadius = 5.4 - t * 2.1 + (s % 2 === 0 ? 0.7 : 0);
        const mx = (prevX + ptX) * 0.5;
        const my = (prevY + ptY) * 0.5;
        ctx.fillStyle = rgba(PALETTE.voidAbyss, 0.92);
        ctx.beginPath();
        ctx.arc(mx, my, segRadius + 1.4, 0, Math.PI * 2);
        ctx.fill();

        const segGrad = ctx.createRadialGradient(
          mx - px * 1.2,
          my - py * 1.2,
          0.5,
          mx,
          my,
          segRadius * 1.2,
        );
        segGrad.addColorStop(
          0,
          s % 2 === 0 ? rgba(PALETTE.driedClot, 0.9) : rgba(PALETTE.bileOlive, 0.76),
        );
        segGrad.addColorStop(0.55, rgba(PALETTE.clotShadow, 0.94));
        segGrad.addColorStop(1, rgba(PALETTE.voidAbyss, 0.96));
        ctx.fillStyle = segGrad;
        ctx.beginPath();
        ctx.arc(mx, my, segRadius, 0, Math.PI * 2);
        ctx.fill();
      }
      prevX = ptX;
      prevY = ptY;
    }

    // Glossy dorsal vascular ridge
    ctx.beginPath();
    spinePts.forEach(([sx, sy], idx) => (idx === 0 ? ctx.moveTo(sx, sy) : ctx.lineTo(sx, sy)));
    ctx.strokeStyle = rgba(PALETTE.driedClot, 0.52);
    ctx.lineWidth = 2.0;
    ctx.stroke();

    // Braided pale bone-sinew helix wrapping the stalk
    ctx.beginPath();
    helixPts.forEach(([hx, hy], idx) => (idx === 0 ? ctx.moveTo(hx, hy) : ctx.lineTo(hx, hy)));
    ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.85);
    ctx.lineWidth = 2.6;
    ctx.stroke();
    ctx.strokeStyle = rgba(PALETTE.bleachedBone, 0.68);
    ctx.lineWidth = 1.15;
    ctx.stroke();

    // Hooked calcified clasp gripping the iron valve rim
    for (const clawSide of [-1, 1]) {
      const jawX = prevX - dx * 4.5 + px * (4.8 * clawSide);
      const jawY = prevY - dy * 4.5 + py * (4.8 * clawSide);
      ctx.beginPath();
      ctx.moveTo(prevX, prevY);
      ctx.lineTo(jawX, jawY);
      ctx.strokeStyle = rgba(PALETTE.voidAbyss, 0.96);
      ctx.lineWidth = 3.2;
      ctx.stroke();
      ctx.strokeStyle = rgba(PALETTE.bleachedBone, 0.88);
      ctx.lineWidth = 1.5;
      ctx.stroke();
    }
    ctx.fillStyle = rgba(PALETTE.voidAbyss, 0.96);
    ctx.beginPath();
    ctx.arc(prevX, prevY, 3.8, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = rgba(PALETTE.driedClot, 0.88);
    ctx.beginPath();
    ctx.arc(prevX, prevY, 2.2, 0, Math.PI * 2);
    ctx.fill();
  }

  // Grotesque, emaciated 5-fingered stitched Suture-Hands clawing & turning the valve rim clockwise
  const visibleHands = Math.min(sutureCount, 8);
  for (let i = 0; i < visibleHands; i += 1) {
    const handAnglePhase = handCyclePhase * 2 * Math.PI + i * 1.15;
    // Slight clockwise rim tug synced with pressWave
    const pressWave =
      (Math.sin(handAnglePhase) * 0.78 + Math.sin(handAnglePhase * 3) * 0.22 + 1) * 0.5;
    const cwTugAngle = (pressWave - 0.5) * 0.085;
    const baseAngle = i * ((2 * Math.PI) / visibleHands) - Math.PI / 2 + Math.PI / 8 + cwTugAngle;
    const handedness = i % 2 === 0 ? 1 : -1;
    const dx = Math.cos(baseAngle);
    const dy = Math.sin(baseAngle);
    const px = -dy * handedness;
    const py = dx * handedness;

    const pressInset = pressWave * 7.2;

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

function sampleBezelPerimeter(t, left, top, right, bottom) {
  const w = Math.max(8, right - left);
  const h = Math.max(8, bottom - top);
  const perim = 2 * (w + h);
  let dist = (((t % 1) + 1) % 1) * perim;
  if (dist < w) {
    return { x: left + dist, y: top, tx: 1, ty: 0, nx: 0, ny: -1 };
  }
  dist -= w;
  if (dist < h) {
    return { x: right, y: top + dist, tx: 0, ty: 1, nx: 1, ny: 0 };
  }
  dist -= h;
  if (dist < w) {
    return { x: right - dist, y: bottom, tx: -1, ty: 0, nx: 0, ny: 1 };
  }
  dist -= w;
  return { x: left, y: bottom - dist, tx: 0, ty: -1, nx: -1, ny: 0 };
}

export function drawLeechClerks(canvas, leechCount, breath, patrolPhase = breath) {
  const { ctx, width, height } = syncCanvasSize(canvas);
  ctx.clearRect(0, 0, width, height);
  if (leechCount <= 0) return;

  // Track hugs the counter glass bezel with generous outer margin so tails never clip
  const left = 28;
  const right = Math.max(left + 12, width - 28);
  const top = 8;
  const bottom = Math.max(top + 12, height - 8);
  const perim = 2 * (right - left + (bottom - top));
  const segStepT = 3.2 / Math.max(40, perim);

  const visible = Math.min(leechCount, 6);
  for (let i = 0; i < visible; i += 1) {
    const direction = i % 2 === 0 ? 1 : -1;
    const speed = 0.72 + (i % 3) * 0.22;
    const headT = (i / visible + direction * patrolPhase * speed) % 1;
    const wavePhase = patrolPhase * Math.PI * 10 + i * 1.45;

    for (let seg = 6; seg >= 0; seg -= 1) {
      const segT = headT - direction * seg * segStepT;
      const pt = sampleBezelPerimeter(segT, left, top, right, bottom);
      const undulate = Math.sin(wavePhase - seg * 0.68) * (1.4 + seg * 0.22);
      const sx = pt.x + pt.nx * undulate;
      const sy = pt.y + pt.ny * undulate;
      const r =
        seg === 0 ? 3.8 : seg <= 3 ? 4.2 - seg * 0.24 : Math.max(1.5, 3.3 - (seg - 3) * 0.55);

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

    const head = sampleBezelPerimeter(headT, left, top, right, bottom);
    const mouthPulse = 1 + 0.14 * Math.sin(wavePhase * 1.4);
    const mx = head.x + head.tx * direction * 2.2;
    const my = head.y + head.ty * direction * 2.2;
    ctx.fillStyle = rgba(PALETTE.driedClot, 0.68);
    ctx.beginPath();
    ctx.arc(mx, my, 4.4 * mouthPulse, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = rgba(PALETTE.voidAbyss, 0.92);
    ctx.beginPath();
    ctx.arc(mx, my, 1.9 * mouthPulse, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = rgba(PALETTE.paleLichen, 0.55);
    ctx.lineWidth = 0.9;
    ctx.beginPath();
    ctx.arc(mx, my, 4.4 * mouthPulse, 0, Math.PI * 2);
    ctx.stroke();
  }
}
