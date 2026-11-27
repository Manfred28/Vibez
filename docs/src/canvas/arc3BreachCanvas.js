/**
 * Arc 3 ("The Unmaking") HTML5 Canvas renderers:
 * - `drawArc3BreachBackdrop`: progressive fractured walls, bleeding scar gashes, clotting halo,
 *   and Strata IV-V arterial tunnel vignette.
 * - `drawCrimsonFloodCanvas`: animated volumetric Abstract Crimson Flood rising across the
 *   chamber and swallowing the full viewport during the 5-pull dissolution finale.
 */

import { drawCoverImage, syncCanvasSize } from './canvasShared.js?v=3.15';
import {
  arc3FloodLevel,
  arc3IsActivelyClotting,
  arc3IsInFinale,
  arc3StratumProgress,
  arc3UnstitchedScarCount,
} from '../engine/arc3Models.js?v=3.15';

const SCAR_SLASHES = [
  [0.14, 0.2, 0.34, 0.42],
  [0.86, 0.18, 0.65, 0.4],
  [0.18, 0.56, 0.42, 0.72],
  [0.82, 0.54, 0.59, 0.74],
  [0.3, 0.14, 0.48, 0.34],
  [0.7, 0.16, 0.52, 0.36],
];

export function drawArc3BreachBackdrop(canvas, breachImages, state, breath = 0, wavePhase = 0) {
  if (!canvas) return;
  const { ctx, width, height } = syncCanvasSize(canvas);
  ctx.clearRect(0, 0, width, height);

  const breach = state?.arc3 ?? {};
  const stratum = breach.currentStratum ?? 1;
  const progress = arc3StratumProgress(breach);
  const flood = arc3FloodLevel(breach);
  const clotting = arc3IsActivelyClotting(breach);
  const inFinale = arc3IsInFinale(breach);

  // Base arterial dark void
  ctx.fillStyle = '#0D0607';
  ctx.fillRect(0, 0, width, height);

  // Progressive wall cross-fade: wallEarly -> wallDeep -> crimsonFlood
  const earlyAlpha = stratum <= 2 ? 0.92 : stratum === 3 ? Math.max(0.15, 0.75 - progress * 0.55) : 0.08;
  const deepAlpha =
    stratum <= 1
      ? 0.0
      : stratum === 2
        ? progress * 0.45
        : stratum === 3
          ? 0.45 + progress * 0.45
          : 0.92;
  const floodPhotoAlpha = inFinale
    ? 0.55 + Math.min(0.45, (breach.finalPullsCompleted ?? 0) * 0.09)
    : stratum >= 4
      ? Math.max(0, (flood - 0.5) * 1.1)
      : 0.0;

  if (breachImages?.wallEarly && earlyAlpha > 0.01) {
    drawCoverImage(ctx, breachImages.wallEarly, width, height, earlyAlpha);
  }
  if (breachImages?.wallDeep && deepAlpha > 0.01) {
    drawCoverImage(ctx, breachImages.wallDeep, width, height, deepAlpha);
  }
  if (breachImages?.crimsonFlood && floodPhotoAlpha > 0.01) {
    drawCoverImage(ctx, breachImages.crimsonFlood, width, height, floodPhotoAlpha);
  }

  // Bleeding diagonal scar slashes corresponding to unstitched scars
  const scars = Math.min(SCAR_SLASHES.length, arc3UnstitchedScarCount(breach));
  if (scars > 0) {
    ctx.save();
    for (let i = 0; i < scars; i += 1) {
      const [x1, y1, x2, y2] = SCAR_SLASHES[i];
      const ax = x1 * width;
      const ay = y1 * height;
      const bx = x2 * width;
      const by = y2 * height;

      // Outer crimson seepage glow
      ctx.strokeStyle = `rgba(116, 18, 26, ${0.38 + breath * 0.18})`;
      ctx.lineWidth = 7;
      ctx.lineCap = 'round';
      ctx.beginPath();
      ctx.moveTo(ax, ay);
      ctx.lineTo(bx, by);
      ctx.stroke();

      // Raw core tear
      ctx.strokeStyle = `rgba(182, 42, 52, ${0.72 + breath * 0.16})`;
      ctx.lineWidth = 2.2;
      ctx.beginPath();
      ctx.moveTo(ax, ay);
      ctx.lineTo(bx, by);
      ctx.stroke();

      // Slow downward seep trail
      const seepLen = 14 + Math.sin(wavePhase * Math.PI * 2 + i * 1.4) * 5;
      ctx.strokeStyle = 'rgba(130, 20, 30, 0.45)';
      ctx.lineWidth = 1.6;
      ctx.beginPath();
      ctx.moveTo((ax + bx) * 0.5, (ay + by) * 0.5);
      ctx.lineTo((ax + bx) * 0.5, (ay + by) * 0.5 + seepLen);
      ctx.stroke();
    }
    ctx.restore();
  }

  // Active clotting inward strain ring when player pauses
  if (clotting) {
    const grad = ctx.createRadialGradient(
      width * 0.5,
      height * 0.5,
      Math.min(width, height) * 0.22,
      width * 0.5,
      height * 0.5,
      Math.max(width, height) * 0.72,
    );
    grad.addColorStop(0, 'rgba(0, 0, 0, 0)');
    grad.addColorStop(0.68, `rgba(74, 16, 22, ${0.22 + breath * 0.16})`);
    grad.addColorStop(1, `rgba(148, 28, 36, ${0.42 + breath * 0.2})`);
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, width, height);
  }

  // Atmospheric vignette intensifying in Strata IV-V
  const vignetteStrength = 0.52 + flood * 0.34;
  const vignette = ctx.createRadialGradient(
    width * 0.5,
    height * 0.5,
    Math.min(width, height) * 0.18,
    width * 0.5,
    height * 0.5,
    Math.max(width, height) * 0.78,
  );
  vignette.addColorStop(0, 'rgba(12, 4, 5, 0.08)');
  vignette.addColorStop(0.65, `rgba(18, 4, 6, ${(vignetteStrength * 0.55).toFixed(3)})`);
  vignette.addColorStop(1, `rgba(8, 2, 3, ${Math.min(0.92, vignetteStrength).toFixed(3)})`);
  ctx.fillStyle = vignette;
  ctx.fillRect(0, 0, width, height);
}

export function drawCrimsonFloodCanvas(canvas, floodLevel, wavePhase = 0, pulse = 0, crimsonImg = null) {
  if (!canvas) return;
  const { ctx, width, height } = syncCanvasSize(canvas);
  ctx.clearRect(0, 0, width, height);

  const clamped = Math.max(0, Math.min(1, Number(floodLevel) || 0));
  if (clamped <= 0.005) return;

  const surfaceY = height * (1 - clamped);
  const amp = clamped >= 0.99 ? 0 : Math.max(2.5, (1 - clamped) * 11 + 3);
  const tau = Math.PI * 2;

  ctx.save();
  // Build undulating liquid mask
  ctx.beginPath();
  if (clamped >= 0.995) {
    ctx.rect(0, 0, width, height);
  } else {
    ctx.moveTo(0, height);
    const steps = 36;
    for (let i = 0; i <= steps; i += 1) {
      const frac = i / steps;
      const x = frac * width;
      const y =
        surfaceY +
        Math.sin(frac * tau * 1.4 + wavePhase * tau) * amp +
        Math.cos(frac * tau * 2.6 - wavePhase * tau * 1.35) * (amp * 0.42);
      ctx.lineTo(x, y);
    }
    ctx.lineTo(width, height);
    ctx.closePath();
  }
  ctx.clip();

  // Deep venous crimson gradient body
  const bodyGrad = ctx.createLinearGradient(0, Math.max(0, surfaceY - 12), 0, height);
  bodyGrad.addColorStop(0, `rgba(148, 22, 32, ${0.78 + pulse * 0.08})`);
  bodyGrad.addColorStop(0.28, 'rgba(98, 12, 20, 0.92)');
  bodyGrad.addColorStop(0.7, 'rgba(54, 6, 11, 0.97)');
  bodyGrad.addColorStop(1, 'rgba(26, 3, 6, 0.99)');
  ctx.fillStyle = bodyGrad;
  ctx.fillRect(0, 0, width, height);

  // Subtle abstract crimson oil photographic veil blended into deep flood
  if (crimsonImg && clamped > 0.25) {
    const imgAlpha = Math.min(0.46, (clamped - 0.25) * 0.58 + pulse * 0.06);
    drawCoverImage(ctx, crimsonImg, width, height, imgAlpha);
  }

  // Slow internal current ribbons
  ctx.strokeStyle = `rgba(196, 48, 58, ${0.14 + pulse * 0.1})`;
  ctx.lineWidth = 2.4;
  for (let r = 0; r < 3; r += 1) {
    const bandY = surfaceY + (height - surfaceY) * (0.24 + r * 0.26);
    ctx.beginPath();
    for (let x = 0; x <= width; x += 24) {
      const t = x / Math.max(1, width);
      const dy = Math.sin(t * tau * (1.2 + r * 0.4) + wavePhase * tau + r * 2.1) * 7;
      if (x === 0) ctx.moveTo(x, bandY + dy);
      else ctx.lineTo(x, bandY + dy);
    }
    ctx.stroke();
  }
  ctx.restore();

  // Meniscus highlight along the rising liquid edge
  if (clamped < 0.995) {
    ctx.save();
    ctx.strokeStyle = `rgba(232, 92, 98, ${0.52 + pulse * 0.22})`;
    ctx.lineWidth = 1.8;
    ctx.beginPath();
    const steps = 36;
    for (let i = 0; i <= steps; i += 1) {
      const frac = i / steps;
      const x = frac * width;
      const y =
        surfaceY +
        Math.sin(frac * tau * 1.4 + wavePhase * tau) * amp +
        Math.cos(frac * tau * 2.6 - wavePhase * tau * 1.35) * (amp * 0.42);
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    ctx.stroke();
    ctx.restore();
  }
}
