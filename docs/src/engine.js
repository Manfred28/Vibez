/**
 * Pure multi-arc simulation facade for the GitHub Pages web version of
 * "The Tending of the Unnamed Organ" (1:1 mathematical & narrative parity with Android).
 *
 * Re-exports all modular engine sub-modules under `./engine/` and dispatches the unified
 * `tick(state, deltaSeconds, catalog)` across Arc 1, Arc 2, and Arc 3.
 */

import { GAME_ARCS } from './engine/constants.js?v=3.12';
import { tickArc1 } from './engine/arc1Engine.js?v=3.12';
import { tickArc2 } from './engine/arc2Engine.js?v=3.12';

export * from './engine/constants.js?v=3.12';
export * from './engine/arc2Models.js?v=3.12';
export * from './engine/arc1Math.js?v=3.12';
export * from './engine/progression.js?v=3.12';
export * from './engine/arc1Engine.js?v=3.12';
export * from './engine/arc2Engine.js?v=3.12';
export * from './engine/devEngine.js?v=3.12';
export * from './engine/serialization.js?v=3.12';

export function tick(state, deltaSeconds, catalog) {
  if (deltaSeconds <= 0.0) return state;
  if (state.activeArc === GAME_ARCS.ARC_2_INVESTIGATION) {
    return tickArc2(state, deltaSeconds, catalog);
  }
  if (state.activeArc === GAME_ARCS.ARC_3_CONVERGENCE) return state;
  return tickArc1(state, deltaSeconds, catalog);
}
