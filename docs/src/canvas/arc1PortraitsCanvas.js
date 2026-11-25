/**
 * Arc 1 specimen portraits, wall anomaly fallback glyphs, and deck action artwork medallions.
 */

import { PALETTE, rgba, syncCanvasSize } from './canvasShared.js?v=3.11';

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

export function drawActionArtwork(canvas, kind, id, active = false) {
  const { ctx, width, height } = syncCanvasSize(canvas);
  ctx.clearRect(0, 0, width, height);
  const cx = width * 0.5;
  const cy = height * 0.5;
  const s = Math.min(width, height) / 40;

  ctx.save();
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';

  if (kind === 'jar') {
    const accent = active ? PALETTE.paleLichen : PALETTE.weatheredAsh;
    const fluid = active ? rgba(PALETTE.paleLichen, 0.24) : rgba(PALETTE.bileShadow, 0.58);
    // Wax cork stopper
    ctx.fillStyle = active ? PALETTE.driedClot : PALETTE.fractureOutline;
    ctx.fillRect(cx - 6 * s, cy - 15 * s, 12 * s, 4.5 * s);
    // Jar shoulders & body
    ctx.beginPath();
    ctx.moveTo(cx - 7 * s, cy - 10.5 * s);
    ctx.lineTo(cx - 11 * s, cy - 5 * s);
    ctx.lineTo(cx - 11 * s, cy + 12 * s);
    ctx.quadraticCurveTo(cx - 11 * s, cy + 15 * s, cx - 8 * s, cy + 15 * s);
    ctx.lineTo(cx + 8 * s, cy + 15 * s);
    ctx.quadraticCurveTo(cx + 11 * s, cy + 15 * s, cx + 11 * s, cy + 12 * s);
    ctx.lineTo(cx + 11 * s, cy - 5 * s);
    ctx.lineTo(cx + 7 * s, cy - 10.5 * s);
    ctx.closePath();
    ctx.fillStyle = fluid;
    ctx.fill();
    ctx.strokeStyle = accent;
    ctx.lineWidth = 1.25 * s;
    ctx.stroke();

    // Unique specimen motif inside jar
    if (id === 'yesterdays_dust') {
      ctx.fillStyle = rgba(PALETTE.bleachedBone, 0.58);
      for (const [dx, dy, r] of [
        [-4, 10, 1.5],
        [0, 11, 2.0],
        [4, 9.5, 1.4],
        [-2, 5, 1.0],
        [3, 4, 1.1],
      ]) {
        ctx.beginPath();
        ctx.arc(cx + dx * s, cy + dy * s, r * s, 0, Math.PI * 2);
        ctx.fill();
      }
    } else if (id === 'unspent_breath') {
      ctx.strokeStyle = rgba(PALETTE.paleLichen, 0.85);
      ctx.lineWidth = 1.1 * s;
      ctx.beginPath();
      ctx.arc(cx - 1 * s, cy + 2 * s, 5.5 * s, 0, Math.PI * 2);
      ctx.stroke();
      ctx.beginPath();
      ctx.arc(cx + 4 * s, cy - 3 * s, 2.2 * s, 0, Math.PI * 2);
      ctx.stroke();
    } else if (id === 'almost_a_name') {
      ctx.strokeStyle = rgba(PALETTE.bleachedBone, 0.75);
      ctx.lineWidth = 1.1 * s;
      ctx.strokeRect(cx - 6 * s, cy - 1 * s, 12 * s, 9 * s);
      ctx.beginPath();
      ctx.moveTo(cx - 4 * s, cy + 2.5 * s);
      ctx.lineTo(cx + 4 * s, cy + 2.5 * s);
      ctx.moveTo(cx - 4 * s, cy + 5.5 * s);
      ctx.lineTo(cx + 1 * s, cy + 5.5 * s);
      ctx.stroke();
    } else if (id === 'salt_from_the_grout') {
      ctx.fillStyle = rgba(PALETTE.bleachedBone, 0.72);
      for (const [dx, dy] of [
        [-4, 8],
        [0, 5],
        [4, 9],
      ]) {
        ctx.beginPath();
        ctx.moveTo(cx + dx * s, cy + (dy - 4) * s);
        ctx.lineTo(cx + (dx + 2.8) * s, cy + dy * s);
        ctx.lineTo(cx + dx * s, cy + (dy + 3.5) * s);
        ctx.lineTo(cx + (dx - 2.8) * s, cy + dy * s);
        ctx.closePath();
        ctx.fill();
      }
    } else if (id === 'a_borrowed_pulse') {
      ctx.strokeStyle = PALETTE.driedClot;
      ctx.lineWidth = 1.5 * s;
      ctx.beginPath();
      ctx.moveTo(cx - 8 * s, cy + 4 * s);
      ctx.lineTo(cx - 4 * s, cy + 4 * s);
      ctx.lineTo(cx - 2 * s, cy - 3 * s);
      ctx.lineTo(cx + 1 * s, cy + 8 * s);
      ctx.lineTo(cx + 3.5 * s, cy + 1 * s);
      ctx.lineTo(cx + 8 * s, cy + 4 * s);
      ctx.stroke();
    } else {
      // the_uncounted_hour: handless tilted clock face
      ctx.strokeStyle = rgba(PALETTE.bleachedBone, 0.85);
      ctx.lineWidth = 1.2 * s;
      ctx.beginPath();
      ctx.arc(cx, cy + 3 * s, 6.5 * s, 0, Math.PI * 2);
      ctx.stroke();
      ctx.fillStyle = PALETTE.driedClot;
      ctx.beginPath();
      ctx.arc(cx, cy + 3 * s, 1.6 * s, 0, Math.PI * 2);
      ctx.fill();
    }
  } else if (kind === 'graft') {
    if (id === 'choir_lung') {
      ctx.strokeStyle = PALETTE.paleLichen;
      ctx.lineWidth = 1.6 * s;
      ctx.beginPath();
      ctx.moveTo(cx, cy - 14 * s);
      ctx.lineTo(cx, cy - 4 * s);
      ctx.moveTo(cx, cy - 5 * s);
      ctx.lineTo(cx - 6 * s, cy + 1 * s);
      ctx.moveTo(cx, cy - 5 * s);
      ctx.lineTo(cx + 6 * s, cy + 1 * s);
      ctx.stroke();
      for (const side of [-1, 1]) {
        ctx.fillStyle = rgba(PALETTE.driedClot, active ? 0.55 : 0.34);
        ctx.strokeStyle = active ? PALETTE.driedClot : PALETTE.fractureOutline;
        ctx.lineWidth = 1.3 * s;
        ctx.beginPath();
        ctx.ellipse(cx + side * 7.5 * s, cy + 5.5 * s, 5.5 * s, 8.5 * s, side * -0.18, 0, Math.PI * 2);
        ctx.fill();
        ctx.stroke();
      }
    } else {
      // Braided tendril knot
      ctx.strokeStyle = PALETTE.driedClot;
      ctx.lineWidth = 2.6 * s;
      ctx.beginPath();
      ctx.moveTo(cx - 12 * s, cy + 12 * s);
      ctx.bezierCurveTo(cx - 4 * s, cy - 10 * s, cx + 6 * s, cy + 10 * s, cx + 12 * s, cy - 12 * s);
      ctx.stroke();
      ctx.strokeStyle = PALETTE.bleachedBone;
      ctx.lineWidth = 1.5 * s;
      ctx.beginPath();
      ctx.moveTo(cx - 12 * s, cy - 10 * s);
      ctx.bezierCurveTo(cx - 3 * s, cy + 10 * s, cx + 5 * s, cy - 10 * s, cx + 12 * s, cy + 11 * s);
      ctx.stroke();
      ctx.fillStyle = PALETTE.paleLichen;
      ctx.beginPath();
      ctx.arc(cx, cy, 3 * s, 0, Math.PI * 2);
      ctx.fill();
    }
  } else if (kind === 'rite') {
    if (id === 'drain') {
      ctx.fillStyle = rgba(PALETTE.voidAbyss, 0.9);
      ctx.strokeStyle = active ? PALETTE.driedClot : PALETTE.fractureOutline;
      ctx.lineWidth = 1.3 * s;
      ctx.fillRect(cx - 13 * s, cy - 8 * s, 26 * s, 16 * s);
      ctx.strokeRect(cx - 13 * s, cy - 8 * s, 26 * s, 16 * s);
      for (let i = -2; i <= 2; i += 1) {
        ctx.beginPath();
        ctx.moveTo(cx + i * 4.5 * s, cy - 8 * s);
        ctx.lineTo(cx + i * 4.5 * s, cy + 8 * s);
        ctx.stroke();
      }
    } else if (id === 'echo') {
      ctx.fillStyle = PALETTE.voidAbyss;
      ctx.strokeStyle = active ? PALETTE.paleLichen : PALETTE.fractureOutline;
      ctx.lineWidth = 1.3 * s;
      ctx.fillRect(cx - 3.5 * s, cy - 13 * s, 7 * s, 26 * s);
      ctx.strokeRect(cx - 3.5 * s, cy - 13 * s, 7 * s, 26 * s);
      ctx.strokeStyle = rgba(PALETTE.paleLichen, active ? 0.85 : 0.42);
      for (const r of [8, 13]) {
        ctx.beginPath();
        ctx.arc(cx, cy, r * s, -0.7, 0.7);
        ctx.stroke();
        ctx.beginPath();
        ctx.arc(cx, cy, r * s, Math.PI - 0.7, Math.PI + 0.7);
        ctx.stroke();
      }
    } else if (id === 'clamp') {
      ctx.strokeStyle = active ? PALETTE.driedClot : PALETTE.bleachedBone;
      ctx.lineWidth = 2.4 * s;
      ctx.beginPath();
      ctx.arc(cx, cy, 10 * s, 0.4, Math.PI * 2 - 0.4);
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(cx - 13 * s, cy);
      ctx.lineTo(cx + 13 * s, cy);
      ctx.stroke();
      ctx.fillStyle = PALETTE.driedClot;
      ctx.beginPath();
      ctx.arc(cx, cy, 3 * s, 0, Math.PI * 2);
      ctx.fill();
    } else {
      // throat
      ctx.fillStyle = rgba(PALETTE.clotShadow, 0.88);
      ctx.strokeStyle = PALETTE.driedClot;
      ctx.lineWidth = 1.4 * s;
      ctx.beginPath();
      ctx.ellipse(cx, cy, 11 * s, 14 * s, 0, 0, Math.PI * 2);
      ctx.fill();
      ctx.stroke();
      ctx.strokeStyle = PALETTE.bleachedBone;
      ctx.lineWidth = 1.2 * s;
      ctx.beginPath();
      ctx.moveTo(cx, cy - 10 * s);
      ctx.quadraticCurveTo(cx - 4 * s, cy, cx, cy + 10 * s);
      ctx.moveTo(cx, cy - 10 * s);
      ctx.quadraticCurveTo(cx + 4 * s, cy, cx, cy + 10 * s);
      ctx.stroke();
    }
  } else if (kind === 'relic') {
    const stroke = active ? PALETTE.paleLichen : PALETTE.weatheredAsh;
    ctx.strokeStyle = stroke;
    ctx.fillStyle = active ? rgba(PALETTE.paleLichen, 0.24) : rgba(PALETTE.voidAbyss, 0.5);
    ctx.lineWidth = 1.5 * s;
    if (id === 'patient_wristband') {
      ctx.beginPath();
      ctx.ellipse(cx, cy, 12 * s, 7 * s, -0.3, 0, Math.PI * 2);
      ctx.fill();
      ctx.stroke();
      ctx.fillStyle = active ? PALETTE.driedClot : PALETTE.fractureOutline;
      ctx.fillRect(cx - 4 * s, cy - 2 * s, 8 * s, 4 * s);
    } else if (id === 'brass_ward_key') {
      ctx.beginPath();
      ctx.arc(cx - 6 * s, cy, 5.5 * s, 0, Math.PI * 2);
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(cx - 0.5 * s, cy);
      ctx.lineTo(cx + 12 * s, cy);
      ctx.moveTo(cx + 7 * s, cy);
      ctx.lineTo(cx + 7 * s, cy + 5 * s);
      ctx.moveTo(cx + 11 * s, cy);
      ctx.lineTo(cx + 11 * s, cy + 4 * s);
      ctx.stroke();
    } else {
      ctx.fillRect(cx - 9 * s, cy - 11 * s, 18 * s, 22 * s);
      ctx.strokeRect(cx - 9 * s, cy - 11 * s, 18 * s, 22 * s);
      ctx.beginPath();
      ctx.moveTo(cx - 5 * s, cy - 11 * s);
      ctx.lineTo(cx - 5 * s, cy + 11 * s);
      ctx.stroke();
      ctx.fillStyle = active ? PALETTE.driedClot : PALETTE.fractureOutline;
      ctx.beginPath();
      ctx.arc(cx + 3 * s, cy + 4 * s, 3 * s, 0, Math.PI * 2);
      ctx.fill();
    }
  }
  ctx.restore();
}
