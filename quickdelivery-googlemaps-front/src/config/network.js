const MOBILE_BACKEND_HOST_OVERRIDE_KEY = 'qd_backend_host';
const EXPLICIT_GATEWAY_BASE_URL = (process.env.VUE_APP_GATEWAY_BASE_URL || '').trim();
const EXPLICIT_AUTH_BASE_URL = (process.env.VUE_APP_AUTH_BASE_URL || '').trim();
const EXPLICIT_WS_BASE_URL = (process.env.VUE_APP_WS_BASE_URL || '').trim();

function normalizeHost(rawHost) {
  const trimmed = typeof rawHost === 'string' ? rawHost.trim() : '';
  if (!trimmed) {
    return '';
  }

  let normalized = trimmed.replace(/^https?:\/\//i, '');
  normalized = normalized.replace(/\/.*$/, '');
  normalized = normalized.replace(/:\d+$/, '');
  return normalized.trim();
}

function getCapacitorPlatform() {
  try {
    return window.Capacitor?.getPlatform?.() || null;
  } catch (_) {
    return null;
  }
}

export function isMobileCapacitorRuntime() {
  const platform = getCapacitorPlatform();
  return platform === 'android' || platform === 'ios';
}

function getStoredHostOverride() {
  try {
    const stored = localStorage.getItem(MOBILE_BACKEND_HOST_OVERRIDE_KEY) || '';
    const normalized = normalizeHost(stored);
    if (stored !== normalized) {
      if (normalized) {
        localStorage.setItem(MOBILE_BACKEND_HOST_OVERRIDE_KEY, normalized);
      } else {
        localStorage.removeItem(MOBILE_BACKEND_HOST_OVERRIDE_KEY);
      }
    }
    return normalized;
  } catch (_) {
    return '';
  }
}

export function setBackendHostOverride(host) {
  const normalizedHost = normalizeHost(host);
  if (!normalizedHost) {
    localStorage.removeItem(MOBILE_BACKEND_HOST_OVERRIDE_KEY);
    return;
  }
  localStorage.setItem(MOBILE_BACKEND_HOST_OVERRIDE_KEY, normalizedHost);
}

export function getBackendHostOverrideKey() {
  return MOBILE_BACKEND_HOST_OVERRIDE_KEY;
}

export function ensureMobileBackendHostConfigured() {
  if (!isMobileCapacitorRuntime()) {
    return;
  }

  const existingHost = getStoredHostOverride();
  if (existingHost) {
    return;
  }

  const suggestedHost = getCapacitorPlatform() === 'android' ? '10.0.2.2' : '';
  const enteredHost = window.prompt(
    "Saisis l'IP de la machine qui execute le backend (ex: 192.168.1.25). Sur emulateur Android, tu peux aussi utiliser 10.0.2.2.",
    suggestedHost
  );

  if (typeof enteredHost === 'string' && enteredHost.trim()) {
    setBackendHostOverride(enteredHost);
  } else if (suggestedHost) {
    setBackendHostOverride(suggestedHost);
  }
}

export function resolveBackendHost() {
  const explicitHost = getStoredHostOverride();
  if (explicitHost) {
    return explicitHost;
  }

  const platform = getCapacitorPlatform();
  if (platform === 'android') {
    return '10.0.2.2';
  }

  const currentHost = window.location.hostname;
  if (currentHost && currentHost !== 'localhost') {
    return currentHost;
  }

  return 'localhost';
}

export function getGatewayBaseUrl() {
  if (EXPLICIT_GATEWAY_BASE_URL) {
    return EXPLICIT_GATEWAY_BASE_URL;
  }
  return `https://${resolveBackendHost()}:8443`;
}

export function getAuthBaseUrl() {
  if (EXPLICIT_AUTH_BASE_URL) {
    return EXPLICIT_AUTH_BASE_URL;
  }
  return `https://${resolveBackendHost()}:18443/auth`;
}

export function getWsBaseUrl() {
  if (EXPLICIT_WS_BASE_URL) {
    return EXPLICIT_WS_BASE_URL;
  }
  return `wss://${resolveBackendHost()}:8443/ws`;
}
