/**
 * Shared high-DPI HTML5 Canvas palettes, sizing cache, and geometry helpers.
 */

export const PALETTE = Object.freeze({
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

export const DESK_PALETTE = Object.freeze({
  slateVoid: '#0B0F13',
  deskDark: '#12181E',
  deskSurface: '#1B222A',
  rainSlate: '#2C3742',
  tungstenAmber: '#B89E68',
  paperBone: '#D2CFC5',
  caseRed: '#8F4646',
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

export function rgba(hex, alpha) {
  let rgb = HEX_RGB_CACHE.get(hex);
  if (rgb === undefined) {
    const n = parseInt(hex.slice(1), 16);
    rgb = `${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}`;
    HEX_RGB_CACHE.set(hex, rgb);
  }
  return `rgba(${rgb}, ${alpha})`;
}

export function syncCanvasSize(canvas) {
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

export function drawCoverImage(ctx, img, width, height, alpha, scale = 1) {
  if (!img || !img.complete || img.naturalWidth <= 0 || alpha <= 0) return;
  ctx.save();
  ctx.globalAlpha = alpha;
  const imgRatio = img.naturalWidth / img.naturalHeight;
  const boxRatio = width / height;
  let drawW = width;
  let drawH = height;
  if (imgRatio > boxRatio) {
    drawW = height * imgRatio;
  } else {
    drawH = width / imgRatio;
  }
  drawW *= scale;
  drawH *= scale;
  const dx = (width - drawW) / 2;
  const dy = (height - drawH) / 2;
  ctx.drawImage(img, dx, dy, drawW, drawH);
  ctx.restore();
}

export function drawRoundedRectPath(ctx, x, y, w, h, r) {
  const rad = Math.min(r, w * 0.5, h * 0.5);
  ctx.beginPath();
  ctx.moveTo(x + rad, y);
  ctx.lineTo(x + w - rad, y);
  ctx.quadraticCurveTo(x + w, y, x + w, y + rad);
  ctx.lineTo(x + w, y + h - rad);
  ctx.quadraticCurveTo(x + w, y + h, x + w - rad, y + h);
  ctx.lineTo(x + rad, y + h);
  ctx.quadraticCurveTo(x, y + h, x, y + h - rad);
  ctx.lineTo(x, y + rad);
  ctx.quadraticCurveTo(x, y, x + rad, y);
  ctx.closePath();
}
