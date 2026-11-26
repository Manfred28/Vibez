/**
 * Single-source asset & cache-bust version registry for the GitHub Pages web build.
 * All dynamic asset fetches and image preloads resolve through this module so version bumps
 * stay synchronized across HTML, JS modules, and static artwork.
 */

export const ASSET_VERSION = '3.12';

export function versionedAssetUrl(relativePath) {
  const separator = relativePath.includes('?') ? '&' : '?';
  return `${relativePath}${separator}v=${ASSET_VERSION}`;
}

export const DIALOGUES_ASSET_URL = versionedAssetUrl('./assets/dialogues.json');

export const WALL_ASSET_URLS = Object.freeze({
  phase1: versionedAssetUrl('./assets/img_wall_phase1.jpg'),
  phase2: versionedAssetUrl('./assets/img_wall_fleshy.jpg'),
  phase3: versionedAssetUrl('./assets/img_wall_phase3.jpg'),
  phase4: versionedAssetUrl('./assets/img_wall_phase4.jpg'),
  excisionLate: versionedAssetUrl('./assets/img_wall_excision_late.jpg'),
});

export const ANOMALY_ASSET_URLS = Object.freeze({
  pale_tooth: versionedAssetUrl('./assets/img_anomaly_pale_tooth.jpg'),
  glass_eye: versionedAssetUrl('./assets/img_anomaly_glass_eye.jpg'),
  swollen_nodule: versionedAssetUrl('./assets/img_anomaly_swollen_nodule.jpg'),
  chitin_rib: versionedAssetUrl('./assets/img_anomaly_chitin_rib.jpg'),
});

export const JAR_ASSET_URLS = Object.freeze({
  yesterdays_dust: versionedAssetUrl('./assets/img_jar_yesterdays_dust.jpg'),
  unspent_breath: versionedAssetUrl('./assets/img_jar_unspent_breath.jpg'),
  almost_a_name: versionedAssetUrl('./assets/img_jar_almost_a_name.jpg'),
  salt_from_the_grout: versionedAssetUrl('./assets/img_jar_salt_from_the_grout.jpg'),
  a_borrowed_pulse: versionedAssetUrl('./assets/img_jar_a_borrowed_pulse.jpg'),
  the_uncounted_hour: versionedAssetUrl('./assets/img_jar_the_uncounted_hour.jpg'),
});

export function anomalyAssetUrl(type) {
  return ANOMALY_ASSET_URLS[type] ?? versionedAssetUrl(`./assets/img_anomaly_${type}.jpg`);
}

export function jarAssetUrl(jarId) {
  return JAR_ASSET_URLS[jarId] ?? versionedAssetUrl(`./assets/img_jar_${jarId}.jpg`);
}
