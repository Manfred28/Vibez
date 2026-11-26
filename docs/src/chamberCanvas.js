/**
 * High-DPI HTML5 Canvas renderer facade mirroring the Android Jetpack Compose Canvas artwork.
 * Re-exports modular Arc 1 chamber, portrait/medallion, and Arc 2 desk renderers under `./canvas/`.
 */

export {
  computeWallLayerAlphas,
  drawChamberBackdrop,
  drawForegroundLampsAndSacrifice,
  drawLeechClerks,
  drawValveAndHands,
  getWeepingLampLayout,
} from './canvas/arc1ChamberCanvas.js?v=3.14';
export {
  drawActionArtwork,
  drawAnomalyGlyph,
  drawSumpSiphonPortrait,
  drawWeepingLampPortrait,
} from './canvas/arc1PortraitsCanvas.js?v=3.14';
export {
  computeDeskLayerAlphas,
  drawDeskAnomalyGlyph,
  drawInvestigatorDeskBackdrop,
} from './canvas/arc2DeskCanvas.js?v=3.14';
