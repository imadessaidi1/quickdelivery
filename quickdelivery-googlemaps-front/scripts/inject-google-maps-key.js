const fs = require('fs');
const path = require('path');

function readEnvValue(key) {
  const envFile = path.resolve(__dirname, '../.env.production');
  if (!fs.existsSync(envFile)) {
    return '';
  }

  const lines = fs.readFileSync(envFile, 'utf8').split(/\r?\n/);
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) {
      continue;
    }
    const separatorIndex = trimmed.indexOf('=');
    if (separatorIndex === -1) {
      continue;
    }
    const currentKey = trimmed.slice(0, separatorIndex).trim();
    if (currentKey !== key) {
      continue;
    }
    return trimmed.slice(separatorIndex + 1).trim();
  }

  return '';
}

const googleMapsKey = (process.env.VUE_APP_GOOGLE_MAPS_KEY || readEnvValue('VUE_APP_GOOGLE_MAPS_KEY') || '').trim();
const placeholder = '<%= VUE_APP_GOOGLE_MAPS_KEY %>';
const targetFiles = [
  path.resolve(__dirname, '../dist/google-maps.html'),
  path.resolve(__dirname, '../dist/google-maps-package-tracking.html'),
];

if (!googleMapsKey) {
  console.error('Missing VUE_APP_GOOGLE_MAPS_KEY for front build.');
  process.exit(1);
}

for (const targetFile of targetFiles) {
  if (!fs.existsSync(targetFile)) {
    console.error(`Missing build artifact: ${targetFile}`);
    process.exit(1);
  }

  const content = fs.readFileSync(targetFile, 'utf8');
  const updatedContent = content.split(placeholder).join(googleMapsKey);

  if (content === updatedContent) {
    if (content.includes(`key=${googleMapsKey}`)) {
      continue;
    }
    console.error(`Google Maps placeholder not found in ${path.basename(targetFile)}`);
    process.exit(1);
  }

  fs.writeFileSync(targetFile, updatedContent, 'utf8');
}

console.log('Injected Google Maps key into static map artifacts.');
