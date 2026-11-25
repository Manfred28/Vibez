/**
 * Dual-lane dripping margin controller, FIFO Story drip queue, randomized 3D parallax
 * motion profiles, and first-sighting / 30% repeat event drip gating.
 */

function randomBetween(min, max, rng) {
  const raw = typeof rng === 'function' ? Number(rng()) : Math.random();
  const unit = Number.isFinite(raw) ? Math.min(1, Math.max(0, raw)) : 0.5;
  return min + (max - min) * unit;
}

/**
 * Computes randomized, non-uniform seepage timing and subtle multi-layer parallax parameters
 * for a `.drip-card`. Story drips descend slowly (~15.0–17.0s) for unhurried reading; chamber
 * event drips remain brisk (~4.45–5.55s) on a deeper Z-plane and are hidden while story drips run.
 */
export function computeDripMotionProfile(kind = 'story', rng = Math.random) {
  const isEvent = kind === 'event';
  const delayMs = Math.round(
    isEvent ? randomBetween(90, 540, rng) : randomBetween(40, 280, rng),
  );
  const durationSec = Number(
    (isEvent ? randomBetween(4.45, 5.55, rng) : randomBetween(15.0, 17.0, rng)).toFixed(2),
  );
  const driftX = Number(
    (isEvent ? randomBetween(-7.5, 7.5, rng) : randomBetween(-5.5, 5.5, rng)).toFixed(2),
  );
  const driftMidX = Number(
    (driftX * randomBetween(-0.55, 0.75, rng) + randomBetween(-2.2, 2.2, rng)).toFixed(2),
  );
  const parallaxZ = Number(
    (isEvent ? randomBetween(-28, -4, rng) : randomBetween(-12, 14, rng)).toFixed(2),
  );
  const scale = Number(
    (isEvent ? randomBetween(0.955, 0.995, rng) : randomBetween(0.985, 1.02, rng)).toFixed(3),
  );
  const tiltDeg = Number(
    (isEvent ? randomBetween(-0.7, 0.7, rng) : randomBetween(-0.45, 0.45, rng)).toFixed(2),
  );
  const ribbonLagPx = Number(
    (isEvent ? randomBetween(12, 24, rng) : randomBetween(10, 20, rng)).toFixed(1),
  );
  const ribbonDelayMs = Math.round(
    delayMs + (isEvent ? randomBetween(50, 240, rng) : randomBetween(40, 180, rng)),
  );
  const beadDelayMs = Math.round(
    delayMs + (isEvent ? randomBetween(110, 420, rng) : randomBetween(90, 320, rng)),
  );
  const beadDurationSec = Number(
    (isEvent ? randomBetween(1.45, 2.35, rng) : randomBetween(2.4, 3.4, rng)).toFixed(2),
  );
  const bubbleParallaxX = Number(
    (
      -driftX * randomBetween(0.28, 0.58, rng) +
      randomBetween(isEvent ? -1.4 : -1.0, isEvent ? 1.4 : 1.0, rng)
    ).toFixed(2),
  );
  const bubbleParallaxY = Number(
    (isEvent ? randomBetween(-10, 8, rng) : randomBetween(-8, 6, rng)).toFixed(2),
  );
  const bubbleDelayMs = Math.round(
    delayMs + (isEvent ? randomBetween(30, 160, rng) : randomBetween(20, 120, rng)),
  );

  return {
    kind: isEvent ? 'event' : 'story',
    delayMs,
    durationSec,
    driftX,
    driftMidX,
    parallaxZ,
    scale,
    tiltDeg,
    ribbonLagPx,
    ribbonDelayMs,
    beadDelayMs,
    beadDurationSec,
    bubbleParallaxX,
    bubbleParallaxY,
    bubbleDelayMs,
  };
}

export function applyDripMotionProfile(card, profile) {
  if (!card?.style || !profile) return;
  card.style.setProperty('--drip-delay', `${profile.delayMs}ms`);
  card.style.setProperty('--drip-duration', `${profile.durationSec}s`);
  card.style.setProperty('--drip-drift-x', `${profile.driftX}px`);
  card.style.setProperty('--drip-drift-mid-x', `${profile.driftMidX}px`);
  card.style.setProperty('--drip-parallax-z', `${profile.parallaxZ}px`);
  card.style.setProperty('--drip-scale', String(profile.scale));
  card.style.setProperty('--drip-tilt', `${profile.tiltDeg}deg`);
  card.style.setProperty('--drip-ribbon-lag', `${profile.ribbonLagPx}px`);
  card.style.setProperty('--drip-ribbon-delay', `${profile.ribbonDelayMs}ms`);
  card.style.setProperty('--drip-bead-delay', `${profile.beadDelayMs}ms`);
  card.style.setProperty('--drip-bead-duration', `${profile.beadDurationSec}s`);
  card.style.setProperty('--drip-bubble-parallax-x', `${profile.bubbleParallaxX}px`);
  card.style.setProperty('--drip-bubble-parallax-y', `${profile.bubbleParallaxY}px`);
  card.style.setProperty('--drip-bubble-delay', `${profile.bubbleDelayMs}ms`);
}

export const REPEAT_EVENT_DRIP_CHANCE = 0.30;
export const STORY_DRIP_QUEUE_GAP_MS = 300;

/**
 * Pure FIFO queue for right-margin story drips.
 * Mirrors Android `StoryDripQueue` 1:1: incoming story fragments never interrupt or
 * dismiss an ongoing drip, and duplicate dialogue IDs already active or pending are ignored.
 */
export function createStoryDripQueue() {
  let active = null;
  let pending = [];

  return {
    enqueue(entry) {
      if (!entry || !entry.id) return { started: null, queued: false };
      if (active?.id === entry.id || pending.some((item) => item.id === entry.id)) {
        return { started: null, queued: false };
      }
      if (!active) {
        active = entry;
        return { started: entry, queued: false };
      }
      pending = [...pending, entry];
      return { started: null, queued: true };
    },
    completeActive() {
      if (pending.length === 0) {
        active = null;
        return null;
      }
      const [next, ...rest] = pending;
      active = next;
      pending = rest;
      return next;
    },
    clear() {
      active = null;
      pending = [];
    },
    get active() {
      return active;
    },
    get pending() {
      return [...pending];
    },
    get isBusy() {
      return active != null || pending.length > 0;
    },
  };
}

export function shouldTriggerEventDrip(eventKey, seenKeys, rng = Math.random) {
  if (!eventKey) return false;
  if (!seenKeys || !seenKeys.has(eventKey)) return true;
  const raw = typeof rng === 'function' ? Number(rng()) : Math.random();
  return Number.isFinite(raw) && raw < REPEAT_EVENT_DRIP_CHANCE;
}

export function createDrippingMarginController({ storyDripLayer, eventDripLayer }) {
  let storyDripGapTimer = null;
  const storyDripQueue = createStoryDripQueue();
  const seenEventDripKeys = new Set();

  // Subtle passive scroll & pointer parallax separating Story (near plane) from Event (deeper plane)
  let parallaxRaf = 0;
  let parallaxPointerX = 0;
  let parallaxPointerY = 0;
  function scheduleLaneParallax() {
    if (parallaxRaf) return;
    parallaxRaf = window.requestAnimationFrame(() => {
      parallaxRaf = 0;
      if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) return;
      const scrollRatio = Math.max(-1, Math.min(1, (window.scrollY || 0) / 360 - 0.35));
      const storyX = parallaxPointerX * 5.0;
      const storyY = parallaxPointerY * 4.0 - scrollRatio * 9.0;
      const eventX = parallaxPointerX * 2.6;
      const eventY = parallaxPointerY * 2.2 - scrollRatio * 5.0;
      storyDripLayer?.style.setProperty('--lane-parallax-x', `${storyX.toFixed(2)}px`);
      storyDripLayer?.style.setProperty('--lane-parallax-y', `${storyY.toFixed(2)}px`);
      eventDripLayer?.style.setProperty('--lane-parallax-x', `${eventX.toFixed(2)}px`);
      eventDripLayer?.style.setProperty('--lane-parallax-y', `${eventY.toFixed(2)}px`);
    });
  }
  if (typeof window !== 'undefined' && (storyDripLayer || eventDripLayer)) {
    window.addEventListener('scroll', scheduleLaneParallax, { passive: true });
    window.addEventListener(
      'pointermove',
      (event) => {
        const vw = Math.max(1, window.innerWidth || 1);
        const vh = Math.max(1, window.innerHeight || 1);
        parallaxPointerX = (event.clientX / vw - 0.5) * 2;
        parallaxPointerY = (event.clientY / vh - 0.5) * 2;
        scheduleLaneParallax();
      },
      { passive: true },
    );
  }

  function isStoryDripBusy() {
    return (
      storyDripQueue.isBusy ||
      storyDripGapTimer != null ||
      Boolean(storyDripLayer && storyDripLayer.childElementCount > 0)
    );
  }

  function mountStoryDripCard(entry) {
    if (!storyDripLayer || !entry) return;
    // Story lane takes exclusive priority — clear and suppress chamber event drips while active
    if (eventDripLayer) {
      eventDripLayer.innerHTML = '';
      eventDripLayer.classList.add('is-suppressed');
    }
    const card = document.createElement('div');
    card.className = 'drip-card drip-card--story';
    card.dataset.dialogueId = entry.id;
    applyDripMotionProfile(card, computeDripMotionProfile('story'));
    const ribbon = document.createElement('div');
    ribbon.className = 'drip-ribbon';
    const bubble = document.createElement('div');
    bubble.className = 'drip-bubble';
    const speaker = document.createElement('span');
    speaker.className = 'drip-speaker';
    speaker.textContent = `// ${(entry.speaker || 'CHAMBER').toUpperCase()}`;
    const body = document.createElement('p');
    body.className = 'drip-text';
    body.textContent = (entry.lines || []).join('\n');
    bubble.append(speaker, body);
    card.append(ribbon, bubble);
    storyDripLayer.appendChild(card);

    const handleAnimationEnd = (event) => {
      // Ignore bubbled child animationend events from .drip-ribbon / .drip-bubble
      if (event.target !== card) return;
      card.removeEventListener('animationend', handleAnimationEnd);
      card.remove();
      if (storyDripQueue.pending.length > 0) {
        window.clearTimeout(storyDripGapTimer);
        storyDripGapTimer = window.setTimeout(() => {
          storyDripGapTimer = null;
          const next = storyDripQueue.completeActive();
          if (next) {
            mountStoryDripCard(next);
          } else if (!isStoryDripBusy()) {
            eventDripLayer?.classList.remove('is-suppressed');
          }
        }, STORY_DRIP_QUEUE_GAP_MS);
      } else {
        storyDripQueue.completeActive();
        if (!isStoryDripBusy()) {
          eventDripLayer?.classList.remove('is-suppressed');
        }
      }
    };
    card.addEventListener('animationend', handleAnimationEnd);
  }

  function triggerStoryDrip(entry) {
    if (!storyDripLayer || !entry) return;
    // FIFO queue: never dismiss or overwrite an ongoing story drip
    const { started } = storyDripQueue.enqueue(entry);
    if (started) {
      mountStoryDripCard(started);
    }
  }

  function triggerEventDrip(text, speakerLabel = 'CHAMBER', eventKey = null, rng = Math.random) {
    if (!eventDripLayer || !text) return false;
    // Suppress chamber event drips whenever a story fragment is actively dripping or queued
    if (isStoryDripBusy()) return false;
    const resolvedKey = eventKey || `${speakerLabel}:${text}`;
    if (!shouldTriggerEventDrip(resolvedKey, seenEventDripKeys, rng)) return false;
    seenEventDripKeys.add(resolvedKey);
    eventDripLayer.innerHTML = '';
    const card = document.createElement('div');
    card.className = 'drip-card drip-card--event';
    applyDripMotionProfile(card, computeDripMotionProfile('event'));
    const ribbon = document.createElement('div');
    ribbon.className = 'drip-ribbon';
    const bubble = document.createElement('div');
    bubble.className = 'drip-bubble';
    const speaker = document.createElement('span');
    speaker.className = 'drip-speaker';
    speaker.textContent = `// ${speakerLabel.toUpperCase()}`;
    const body = document.createElement('p');
    body.className = 'drip-text';
    body.textContent = text;
    bubble.append(speaker, body);
    card.append(ribbon, bubble);
    eventDripLayer.appendChild(card);
    const handleAnimationEnd = (event) => {
      if (event.target !== card) return;
      card.removeEventListener('animationend', handleAnimationEnd);
      card.remove();
    };
    card.addEventListener('animationend', handleAnimationEnd);
    return true;
  }

  function reset() {
    seenEventDripKeys.clear();
    window.clearTimeout(storyDripGapTimer);
    storyDripGapTimer = null;
    storyDripQueue.clear();
    if (storyDripLayer) storyDripLayer.innerHTML = '';
    if (eventDripLayer) {
      eventDripLayer.innerHTML = '';
      eventDripLayer.classList.remove('is-suppressed');
    }
  }

  return {
    isStoryDripBusy,
    triggerStoryDrip,
    triggerEventDrip,
    reset,
  };
}
