/**
 * Single-source asset & cache-bust version registry for the GitHub Pages web build.
 * All dynamic asset fetches and image preloads resolve through this module so version bumps
 * stay synchronized across HTML, JS modules, and static artwork.
 */

export const ASSET_VERSION = '3.15';

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

export const SPECIMEN_ASSET_URLS = Object.freeze({
  suture_hand: versionedAssetUrl('./assets/img_suture_hand.jpg'),
  leech_clerk: versionedAssetUrl('./assets/img_leech_clerk.jpg'),
  weeping_lamp: versionedAssetUrl('./assets/img_weeping_lamp.jpg'),
  sump_siphon: versionedAssetUrl('./assets/img_sump_siphon.jpg'),
});

export const GRAFT_ASSET_URLS = Object.freeze({
  tendril: versionedAssetUrl('./assets/img_graft_tendril.jpg'),
  choir_lung: versionedAssetUrl('./assets/img_graft_choir_lung.jpg'),
});

export const RELIC_ASSET_URLS = Object.freeze({
  patient_wristband: versionedAssetUrl('./assets/img_relic_patient_wristband.jpg'),
  brass_ward_key: versionedAssetUrl('./assets/img_relic_brass_ward_key.jpg'),
  bleeding_logbook: versionedAssetUrl('./assets/img_relic_caretaker_logbook.jpg'),
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

export const CONTACT_ASSET_URLS = Object.freeze({
  night_archivist: versionedAssetUrl('./assets/img_contact_night_archivist.jpg'),
  bribed_orderly: versionedAssetUrl('./assets/img_contact_bribed_orderly.jpg'),
  microfilm_scanner: versionedAssetUrl('./assets/img_contact_microfilm_scanner.jpg'),
  audio_transcriber: versionedAssetUrl('./assets/img_contact_audio_transcriber.jpg'),
});

export const ARC2_STAMP_ASSET_URL = versionedAssetUrl('./assets/img_arc2_case_stamp.jpg');
export const ARC3_CONVERGENCE_ASSET_URL = versionedAssetUrl('./assets/img_arc3_convergence.jpg');

export const ARC3_BREACH_ASSET_URLS = Object.freeze({
  wallEarly: versionedAssetUrl('./assets/img_wall_arc3_breach_early.jpg'),
  wallDeep: versionedAssetUrl('./assets/img_wall_arc3_breach_deep.jpg'),
  crimsonFlood: versionedAssetUrl('./assets/img_arc3_crimson_flood.jpg'),
  ruptureClottingSeam: versionedAssetUrl('./assets/img_rupture_clotting_seam.jpg'),
  ruptureArterialWell: versionedAssetUrl('./assets/img_rupture_arterial_well.jpg'),
});

export function anomalyAssetUrl(type) {
  return ANOMALY_ASSET_URLS[type] ?? versionedAssetUrl(`./assets/img_anomaly_${type}.jpg`);
}

export function jarAssetUrl(jarId) {
  return JAR_ASSET_URLS[jarId] ?? versionedAssetUrl(`./assets/img_jar_${jarId}.jpg`);
}

export function specimenAssetUrl(creatureId) {
  return SPECIMEN_ASSET_URLS[creatureId] ?? versionedAssetUrl(`./assets/img_${creatureId}.jpg`);
}

export function graftAssetUrl(graftId) {
  return GRAFT_ASSET_URLS[graftId] ?? versionedAssetUrl(`./assets/img_graft_${graftId}.jpg`);
}

export function relicAssetUrl(relicId) {
  return RELIC_ASSET_URLS[relicId] ?? versionedAssetUrl(`./assets/img_relic_${relicId}.jpg`);
}

export function deskAnomalyAssetUrl(type) {
  return DESK_ANOMALY_ASSET_URLS[type] ?? versionedAssetUrl(`./assets/img_desk_anomaly_${type}.jpg`);
}

export function exhibitAssetUrl(exhibitId) {
  return EXHIBIT_ASSET_URLS[exhibitId] ?? versionedAssetUrl(`./assets/img_exhibit_${exhibitId}.jpg`);
}

export function contactAssetUrl(contactId) {
  return CONTACT_ASSET_URLS[contactId] ?? versionedAssetUrl(`./assets/img_contact_${contactId}.jpg`);
}

export function breachRuptureAssetUrl(ruptureType) {
  if (ruptureType === 'clotting_seam' || ruptureType === 'torn_suture') {
    return ARC3_BREACH_ASSET_URLS.ruptureClottingSeam;
  }
  if (ruptureType === 'splintered_rib') {
    return ANOMALY_ASSET_URLS.chitin_rib;
  }
  return ARC3_BREACH_ASSET_URLS.ruptureArterialWell;
}

