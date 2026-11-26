/**
 * Single-source asset & cache-bust version registry for the GitHub Pages web build.
 * All dynamic asset fetches and image preloads resolve through this module so version bumps
 * stay synchronized across HTML, JS modules, and static artwork.
 */

export const ASSET_VERSION = '3.14';

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

export const DESK_STAGE_ASSET_URLS = Object.freeze({
  case1: versionedAssetUrl('./assets/img_desk_case1.jpg'),
  case2: versionedAssetUrl('./assets/img_desk_case2.jpg'),
  case3: versionedAssetUrl('./assets/img_desk_case3.jpg'),
  case4: versionedAssetUrl('./assets/img_desk_case4.jpg'),
  case5: versionedAssetUrl('./assets/img_desk_case5.jpg'),
});

export const DESK_ANOMALY_ASSET_URLS = Object.freeze({
  evidence_envelope: versionedAssetUrl('./assets/img_desk_anomaly_envelope.jpg'),
  fogged_spectacles: versionedAssetUrl('./assets/img_desk_anomaly_spectacles.jpg'),
  reel_tape: versionedAssetUrl('./assets/img_desk_anomaly_reel_tape.jpg'),
  enamel_exhibit: versionedAssetUrl('./assets/img_desk_anomaly_enamel.jpg'),
});

export const EXHIBIT_ASSET_URLS = Object.freeze({
  water_stained_petition: versionedAssetUrl('./assets/img_exhibit_water_stained_petition.jpg'),
  unlisted_pay_stub: versionedAssetUrl('./assets/img_exhibit_unlisted_pay_stub.jpg'),
  tax_plat_1998: versionedAssetUrl('./assets/img_exhibit_tax_plat_1998.jpg'),
  inside_condensation_swab: versionedAssetUrl('./assets/img_exhibit_inside_condensation_swab.jpg'),
  tuesday_statement_copy: versionedAssetUrl('./assets/img_exhibit_tuesday_statement_copy.jpg'),
  second_chair_polaroid: versionedAssetUrl('./assets/img_exhibit_second_chair_polaroid.jpg'),
  coat_claim_ticket: versionedAssetUrl('./assets/img_exhibit_coat_claim_ticket.jpg'),
  reel_four_waveform: versionedAssetUrl('./assets/img_exhibit_reel_four_waveform.jpg'),
  intake_thumb_cast: versionedAssetUrl('./assets/img_exhibit_intake_thumb_cast.jpg'),
  drainage_loop_blueprint: versionedAssetUrl('./assets/img_exhibit_drainage_loop_blueprint.jpg'),
});

export function anomalyAssetUrl(type) {
  return ANOMALY_ASSET_URLS[type] ?? versionedAssetUrl(`./assets/img_anomaly_${type}.jpg`);
}

export function jarAssetUrl(jarId) {
  return JAR_ASSET_URLS[jarId] ?? versionedAssetUrl(`./assets/img_jar_${jarId}.jpg`);
}

export function deskAnomalyAssetUrl(type) {
  return DESK_ANOMALY_ASSET_URLS[type] ?? versionedAssetUrl(`./assets/img_desk_anomaly_${type}.jpg`);
}

export function exhibitAssetUrl(exhibitId) {
  return EXHIBIT_ASSET_URLS[exhibitId] ?? versionedAssetUrl(`./assets/img_exhibit_${exhibitId}.jpg`);
}
