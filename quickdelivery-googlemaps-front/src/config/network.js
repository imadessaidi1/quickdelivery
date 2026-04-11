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

export function isMobileBrowser() {
  if (typeof window === 'undefined') {
    return false;
  }

  const coarsePointer = window.matchMedia?.('(pointer: coarse)')?.matches;
  const narrowViewport = window.innerWidth <= 900;
  const userAgent = (window.navigator?.userAgent || '').toLowerCase();
  const mobileUserAgent = /android|iphone|ipad|ipod|mobile/.test(userAgent);
  return Boolean(mobileUserAgent || (coarsePointer && narrowViewport));
}

export function shouldUseMobileSafeDocumentRendering() {
  return isMobileCapacitorRuntime() || isMobileBrowser();
}

export function shouldUseCapacitorSafeDocumentRendering() {
  return isMobileCapacitorRuntime();
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
  // Mobile builds must rely on explicit env URLs or existing overrides.
  // Never prompt end users for a backend IP in production.
  return;
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
