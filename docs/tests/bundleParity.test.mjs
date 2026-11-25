import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

import {
  ANOMALY_ASSET_URLS,
  ASSET_VERSION,
  DIALOGUES_ASSET_URL,
  JAR_ASSET_URLS,
  WALL_ASSET_URLS,
  anomalyAssetUrl,
  jarAssetUrl,
  versionedAssetUrl,
} from '../src/version.js';
import {
  androidDialoguesPath,
  docsDir,
  listJsFiles,
  readDocsFile,
  realCatalog,
  repoRoot,
  webDialoguesPath,
} from './testHelpers.mjs';

test('GitHub Pages static bundle integrity and Android dialogues.json parity', () => {
  const requiredFiles = [
    '.nojekyll',
    'index.html',
    'styles.css',
    'src/version.js',
    'src/engine.js',
    'src/engine/constants.js',
    'src/engine/arc2Models.js',
    'src/engine/arc1Math.js',
    'src/engine/progression.js',
    'src/engine/arc1Engine.js',
    'src/engine/arc2Engine.js',
    'src/engine/devEngine.js',
    'src/engine/serialization.js',
    'src/chamberCanvas.js',
    'src/canvas/canvasShared.js',
    'src/canvas/arc1ChamberCanvas.js',
    'src/canvas/arc1PortraitsCanvas.js',
    'src/canvas/arc2DeskCanvas.js',
    'src/audio.js',
    'src/audio/cueResolver.js',
    'src/audio/dspPrimitives.js',
    'src/audio/arc1CueBank.js',
    'src/audio/arc2CueBank.js',
    'src/ui.js',
    'src/ui/drippingMargin.js',
    'src/ui/tabAlerts.js',
    'src/ui/arc1ChamberUi.js',
    'src/ui/arc2InvestigatorUi.js',
    'src/ui/sharedChromeUi.js',
    'src/app.js',
    'assets/dialogues.json',
    'assets/img_wall_phase1.jpg',
    'assets/img_wall_fleshy.jpg',
    'assets/img_wall_phase3.jpg',
    'assets/img_wall_phase4.jpg',
    'assets/img_wall_excision_late.jpg',
    'assets/img_anomaly_pale_tooth.jpg',
    'assets/img_anomaly_glass_eye.jpg',
    'assets/img_anomaly_swollen_nodule.jpg',
    'assets/img_anomaly_chitin_rib.jpg',
    'assets/img_jar_yesterdays_dust.jpg',
    'assets/img_jar_unspent_breath.jpg',
    'assets/img_jar_almost_a_name.jpg',
    'assets/img_jar_salt_from_the_grout.jpg',
    'assets/img_jar_a_borrowed_pulse.jpg',
    'assets/img_jar_the_uncounted_hour.jpg',
    'assets/img_recessed_valve.jpg',
    'assets/img_suture_hand.jpg',
    'assets/img_leech_clerk.jpg',
    'assets/icon.png',
    'assets/jetbrains_mono.ttf',
  ];

  for (const rel of requiredFiles) {
    const abs = path.join(docsDir, rel);
    assert.ok(fs.existsSync(abs), `Missing required GitHub Pages file: docs/${rel}`);
  }

  // Verify all 5 wall JPEGs + 4 wall anomaly JPEGs + 6 specimen jar JPEGs match Android byte-for-byte
  for (const imgFile of [
    'img_wall_phase1.jpg',
    'img_wall_fleshy.jpg',
    'img_wall_phase3.jpg',
    'img_wall_phase4.jpg',
    'img_wall_excision_late.jpg',
    'img_anomaly_pale_tooth.jpg',
    'img_anomaly_glass_eye.jpg',
    'img_anomaly_swollen_nodule.jpg',
    'img_anomaly_chitin_rib.jpg',
    'img_jar_yesterdays_dust.jpg',
    'img_jar_unspent_breath.jpg',
    'img_jar_almost_a_name.jpg',
    'img_jar_salt_from_the_grout.jpg',
    'img_jar_a_borrowed_pulse.jpg',
    'img_jar_the_uncounted_hour.jpg',
  ]) {
    const webBuf = fs.readFileSync(path.join(docsDir, 'assets', imgFile));
    const androidBuf = fs.readFileSync(
      path.join(repoRoot, 'app', 'src', 'main', 'res', 'drawable', imgFile),
    );
    assert.ok(webBuf.length > 5000, `${imgFile} must be a real photographic asset`);
    assert.ok(webBuf.equals(androidBuf), `docs/assets/${imgFile} must match Android res/drawable/${imgFile}`);
  }

  // Verify exact dialogue parity with canonical Android dialogues.json
  const webRaw = fs.readFileSync(webDialoguesPath, 'utf8').trim();
  const androidRaw = fs.readFileSync(androidDialoguesPath, 'utf8').trim();
  assert.equal(webRaw, androidRaw, 'docs/assets/dialogues.json must match app/src/main/assets/dialogues.json');
  assert.ok(realCatalog.schemaVersion >= 3);
  assert.equal(realCatalog.phases.length, 7);
  assert.ok(realCatalog.entries.length >= 45);

  // Verify root index.html redirects to ./docs/ when GitHub Pages serves from /(root)
  const rootIndexPath = path.join(repoRoot, 'index.html');
  assert.ok(fs.existsSync(rootIndexPath), 'Missing root index.html redirect for default GitHub Pages root');
  const rootHtml = fs.readFileSync(rootIndexPath, 'utf8');
  assert.ok(rootHtml.includes('./docs/'), 'Root index.html must redirect to ./docs/');

  // Verify index.html uses relative ./ paths so GitHub Pages subpaths work
  const html = readDocsFile('index.html');
  assert.ok(html.includes(`href="./styles.css?v=${ASSET_VERSION}"`));
  assert.ok(html.includes(`src="./src/app.js?v=${ASSET_VERSION}"`));
  assert.ok(html.includes(`content="${ASSET_VERSION}"`), 'index.html app-build meta must match ASSET_VERSION');
  assert.ok(html.includes('id="chamber-jar-shelves"'));
  assert.ok(html.includes('src="./assets/img_recessed_valve.jpg"'));
  assert.ok(html.includes('THE COUNTING ROOM'));
  assert.ok(html.includes('id="phase-progress-fill"'), 'Diegetic progress bar must be restored in header');
  assert.ok(html.includes('NEXT: UNCANNY DRIFT · 0 / 75'), 'Header must display diegetic next stage progress');
  assert.ok(!html.includes('PHASE 00'), 'User-facing HTML must not show the word PHASE');
  assert.ok(!html.includes('id="phase-badge"'), 'User-facing HTML must not show a P0..P4 phase badge');
  assert.ok(!html.includes('id="arc2-chapter-badge"'), 'User-facing HTML must not show a CH 0/4 badge');
  assert.ok(!html.includes('ARC 02 //'), 'User-facing HTML must not show ARC 02 prefix');
  assert.ok(!html.includes('ARC 03 //'), 'User-facing HTML must not show ARC 03 prefix');
  assert.ok(!html.includes('NEXT FRACTURE'), 'Upcoming unlock thresholds must not use clinical labels');
  assert.ok(!html.includes('NEXT CHAPTER'), 'Upcoming Inquiry chapters must be hidden');
  assert.ok(html.includes('id="audio-mute-btn"'), 'Web UI must include compact audio mute toggle');
  assert.ok(html.includes('id="open-dev-btn"'), 'Web UI must include Clinical Dev Console trigger');
  assert.ok(html.includes('id="arc2-shell"'), 'Web UI must include Arc 2 Investigator Desk shell');
  assert.ok(!html.includes('href="/styles.css"'), 'Avoid root-absolute asset paths that break GitHub Pages subpaths');
});

test('Centralized asset versioning and browser-safe static ESM invariants hold across docs/src', () => {
  assert.equal(DIALOGUES_ASSET_URL, `./assets/dialogues.json?v=${ASSET_VERSION}`);
  assert.equal(versionedAssetUrl('./assets/test.jpg'), `./assets/test.jpg?v=${ASSET_VERSION}`);
  assert.equal(anomalyAssetUrl('pale_tooth'), `./assets/img_anomaly_pale_tooth.jpg?v=${ASSET_VERSION}`);
  assert.equal(jarAssetUrl('yesterdays_dust'), `./assets/img_jar_yesterdays_dust.jpg?v=${ASSET_VERSION}`);
  assert.equal(Object.keys(WALL_ASSET_URLS).length, 5);
  assert.equal(Object.keys(ANOMALY_ASSET_URLS).length, 4);
  assert.equal(Object.keys(JAR_ASSET_URLS).length, 6);

  const srcDir = path.join(docsDir, 'src');
  const jsFiles = listJsFiles(srcDir);
  assert.ok(jsFiles.length >= 25, 'Expected modular docs/src files');

  const importPattern = /(?:import|export)\s+[^'"]*?\s+from\s+['"]([^'"]+)['"]/g;
  const hardcodedAssetVersionPattern = /\.\/assets\/[^'"`\s]+\?v=/;
  for (const rel of jsFiles) {
    const source = fs.readFileSync(path.join(srcDir, rel), 'utf8');
    // No Node-only APIs in browser modules
    assert.ok(!source.includes('node:'), `Browser module docs/src/${rel} must not import node:* modules`);
    // No hardcoded ./assets/...?v= outside version.js
    if (rel !== 'version.js') {
      assert.ok(
        !hardcodedAssetVersionPattern.test(source),
        `docs/src/${rel} must use docs/src/version.js helpers instead of hardcoded ./assets/...?v= URLs`,
      );
    }
    // Every relative ESM import/re-export must use explicit .js?v=<ASSET_VERSION>
    for (const match of source.matchAll(importPattern)) {
      const spec = match[1];
      assert.ok(
        spec.startsWith('./') || spec.startsWith('../'),
        `docs/src/${rel} has bare import specifier "${spec}"`,
      );
      assert.ok(
        spec.endsWith(`.js?v=${ASSET_VERSION}`),
        `docs/src/${rel} import "${spec}" must end with .js?v=${ASSET_VERSION}`,
      );
    }
  }

  // Verify app.js imports and preloads all centralized asset registries
  const appSrc = readDocsFile('src/app.js');
  for (const token of [
    'DIALOGUES_ASSET_URL',
    'WALL_ASSET_URLS',
    'ANOMALY_ASSET_URLS',
    'JAR_ASSET_URLS',
  ]) {
    assert.ok(appSrc.includes(token), `docs/src/app.js must use ${token} from version.js`);
  }

  // Scan ALL UI modules for forbidden clinical meta-labels
  const uiFiles = ['src/ui.js', ...listJsFiles(path.join(srcDir, 'ui')).map((f) => `src/ui/${f}`)];
  for (const rel of uiFiles) {
    const uiSrc = readDocsFile(rel);
    for (const forbidden of [
      'PHASE 0',
      'NEXT MILESTONE',
      'MILESTONE M',
      'EXCISION MILESTONES',
      'EXCISION VI //',
      'FLOOR DRAIN GRATE // M2',
      'NEIGHBOR APERTURE // M3',
      'VALVE CLAMP // M4',
      'THE OSSIFIED THROAT // M5',
      'ARC 02 //',
      'ARC 1 SCAR ECHOES',
      '[RANK ',
    ]) {
      assert.ok(!uiSrc.includes(forbidden), `docs/${rel} must not render clinical meta-label: ${forbidden}`);
    }
  }
});
