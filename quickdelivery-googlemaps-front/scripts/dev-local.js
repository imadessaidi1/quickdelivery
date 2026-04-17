const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');

// ── Helpers ────────────────────────────────────────────────────────────────

function readEnvFile(filePath) {
  if (!fs.existsSync(filePath)) return {};
  const result = {};
  for (const line of fs.readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const sep = trimmed.indexOf('=');
    if (sep === -1) continue;
    result[trimmed.slice(0, sep).trim()] = trimmed.slice(sep + 1).trim();
  }
  return result;
}

// ── Load key (.env.local takes priority over .env.development.local) ───────

const root = path.resolve(__dirname, '..');
const envLocal = readEnvFile(path.join(root, '.env.local'));
const envDevLocal = readEnvFile(path.join(root, '.env.development.local'));

const googleMapsKey = (
  process.env.VUE_APP_GOOGLE_MAPS_KEY ||
  envLocal.VUE_APP_GOOGLE_MAPS_KEY ||
  envDevLocal.VUE_APP_GOOGLE_MAPS_KEY ||
  ''
).trim();

if (!googleMapsKey) {
  console.error('[dev-local] Missing VUE_APP_GOOGLE_MAPS_KEY in .env.local');
  process.exit(1);
}

// ── Patch public/ HTML files ───────────────────────────────────────────────

const PLACEHOLDER = '<%= VUE_APP_GOOGLE_MAPS_KEY %>';
const targetFiles = [
  path.join(root, 'public', 'google-maps.html'),
  path.join(root, 'public', 'google-maps-package-tracking.html'),
];

const originals = {};

for (const file of targetFiles) {
  if (!fs.existsSync(file)) {
    console.warn(`[dev-local] File not found, skipping: ${path.basename(file)}`);
    continue;
  }
  const content = fs.readFileSync(file, 'utf8');
  if (!content.includes(PLACEHOLDER)) {
    console.log(`[dev-local] Placeholder already absent in ${path.basename(file)}, skipping.`);
    continue;
  }
  originals[file] = content;
  fs.writeFileSync(file, content.split(PLACEHOLDER).join(googleMapsKey), 'utf8');
  console.log(`[dev-local] Injected Google Maps key → ${path.basename(file)}`);
}

// ── Restore originals on exit ──────────────────────────────────────────────

function restore() {
  for (const [file, content] of Object.entries(originals)) {
    fs.writeFileSync(file, content, 'utf8');
    console.log(`\n[dev-local] Restored ${path.basename(file)}`);
  }
}

process.on('exit', restore);
process.on('SIGINT', () => process.exit(0));
process.on('SIGTERM', () => process.exit(0));

// ── Launch vue-cli-service serve ──────────────────────────────────────────

console.log('[dev-local] Starting vue-cli-service serve...\n');

const child = spawn(
  'npx',
  ['vue-cli-service', 'serve'],
  { cwd: root, stdio: 'inherit', shell: true, env: { ...process.env, ...envLocal } }
);

child.on('close', (code) => process.exit(code ?? 0));