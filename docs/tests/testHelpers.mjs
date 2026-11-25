import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export const docsDir = path.resolve(__dirname, '..');
export const repoRoot = path.resolve(docsDir, '..');
export const webDialoguesPath = path.join(docsDir, 'assets', 'dialogues.json');
export const androidDialoguesPath = path.join(repoRoot, 'app', 'src', 'main', 'assets', 'dialogues.json');

export const realCatalog = JSON.parse(fs.readFileSync(webDialoguesPath, 'utf8'));

export function readDocsFile(relPath) {
  return fs.readFileSync(path.join(docsDir, relPath), 'utf8');
}

export function listJsFiles(dirAbs, baseAbs = dirAbs) {
  const results = [];
  for (const entry of fs.readdirSync(dirAbs, { withFileTypes: true })) {
    const full = path.join(dirAbs, entry.name);
    if (entry.isDirectory()) {
      results.push(...listJsFiles(full, baseAbs));
    } else if (entry.isFile() && entry.name.endsWith('.js')) {
      results.push(path.relative(baseAbs, full).split(path.sep).join('/'));
    }
  }
  return results.sort();
}
