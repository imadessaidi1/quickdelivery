import { getAuthBaseUrl } from './network';
import { App as CapacitorApp } from '@capacitor/app';
import { cancelPendingRequests } from './requestControl';

const REALM = 'quickdelivery';
const CLIENT_ID = 'quickdelivery-front';
const MOBILE_REDIRECT_URI = 'quickdelivery://auth/callback';

const TOKEN_STORAGE_KEY = 'qd_access_token';
const REFRESH_TOKEN_STORAGE_KEY = 'qd_refresh_token';
const OIDC_STATE_KEY = 'qd_oidc_state';
const OIDC_VERIFIER_KEY = 'qd_oidc_verifier';
const OIDC_REDIRECT_KEY = 'qd_oidc_redirect';

const ALLOWED_REDIRECTS = new Set([
  '/createPackage',
  '/app',
  '/usersAccountValidation',
  '/dashboard/admin',
  '/dashboard/metrics',
  '/dashboard/courier',
  '/dashboard/client'
]);

function safeSessionStorage() {
  try {
    return window.sessionStorage;
  } catch (_) {
    return null;
  }
}

function safeLocalStorage() {
  try {
    return window.localStorage;
  } catch (_) {
    return null;
  }
}

function storageGet(key) {
  const session = safeSessionStorage();
  const local = safeLocalStorage();

  const sessionValue = session?.getItem(key) || '';
  if (sessionValue) {
    return sessionValue;
  }

  const legacyValue = local?.getItem(key) || '';
  if (legacyValue && session) {
    session.setItem(key, legacyValue);
  }
  if (legacyValue && local) {
    local.removeItem(key);
  }
  return legacyValue;
}

function storageSet(key, value) {
  const session = safeSessionStorage();
  const local = safeLocalStorage();
  if (session) {
    session.setItem(key, value);
  }
  if (local) {
    local.removeItem(key);
  }
}

function storageRemove(key) {
  const session = safeSessionStorage();
  const local = safeLocalStorage();
  session?.removeItem(key);
  local?.removeItem(key);
}

function base64UrlEncode(bytes) {
  return btoa(String.fromCharCode(...bytes))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

function randomString(length = 64) {
  const charset = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~';
  const values = new Uint8Array(length);
  window.crypto.getRandomValues(values);
  return Array.from(values).map((v) => charset[v % charset.length]).join('');
}

async function sha256(plainText) {
  const encoder = new TextEncoder();
  const data = encoder.encode(plainText);
  const digest = await window.crypto.subtle.digest('SHA-256', data);
  return new Uint8Array(digest);
}

function defaultRedirectPath() {
  const currentPath = window.location.pathname || '/';
  if (ALLOWED_REDIRECTS.has(currentPath)) {
    return currentPath;
  }
  return '/';
}

function isMobileRuntime() {
  const platform = window.Capacitor?.getPlatform?.();
  return platform === 'android' || platform === 'ios';
}

function getRedirectUri() {
  if (isMobileRuntime()) {
    return MOBILE_REDIRECT_URI;
  }
  const redirectPath = defaultRedirectPath();
  return `${window.location.origin}${redirectPath}`;
}

async function buildAuthorizeUrl(options = {}) {
  const codeVerifier = randomString(96);
  const codeChallengeMethod = 'S256';
  const codeChallenge = base64UrlEncode(await sha256(codeVerifier));
  const state = randomString(32);
  const redirectPath = defaultRedirectPath();
  const redirectUri = getRedirectUri();

  storageSet(OIDC_STATE_KEY, state);
  storageSet(OIDC_VERIFIER_KEY, codeVerifier);
  storageSet(OIDC_REDIRECT_KEY, redirectPath);

  const authorizeUrl = new URL(`${getAuthBaseUrl()}/realms/${REALM}/protocol/openid-connect/auth`);
  authorizeUrl.searchParams.set('client_id', CLIENT_ID);
  authorizeUrl.searchParams.set('response_type', 'code');
  authorizeUrl.searchParams.set('scope', 'openid profile email');
  authorizeUrl.searchParams.set('redirect_uri', redirectUri);
  authorizeUrl.searchParams.set('state', state);
  authorizeUrl.searchParams.set('code_challenge', codeChallenge);
  authorizeUrl.searchParams.set('code_challenge_method', codeChallengeMethod);
  if (options.loginHint) {
    authorizeUrl.searchParams.set('login_hint', options.loginHint);
  }
  return authorizeUrl.toString();
}

function parseJwt(token) {
  try {
    const payload = token.split('.')[1];
    let base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padding = base64.length % 4;
    if (padding) {
      base64 += '='.repeat(4 - padding);
    }
    return JSON.parse(atob(base64));
  } catch (_) {
    return null;
  }
}

export function getCurrentUserIdentity() {
  const token = getAccessToken();
  if (!token) {
    return null;
  }

  const payload = parseJwt(token);
  if (!payload) {
    return null;
  }

  return {
    sub: payload.sub || '',
    email: payload.email || '',
    username: payload.preferred_username || '',
    givenName: payload.given_name || '',
    familyName: payload.family_name || '',
    fullName: payload.name || '',
  };
}

function getRealmRoles(token) {
  const payload = parseJwt(token);
  const roleSet = new Set();

  const realmRoles = payload?.realm_access?.roles;
  if (Array.isArray(realmRoles)) {
    realmRoles.forEach((role) => roleSet.add(role));
  }

  const resourceAccess = payload?.resource_access;
  if (resourceAccess && typeof resourceAccess === 'object') {
    Object.values(resourceAccess).forEach((clientObj) => {
      const clientRoles = clientObj?.roles;
      if (Array.isArray(clientRoles)) {
        clientRoles.forEach((role) => roleSet.add(role));
      }
    });
  }

  return Array.from(roleSet);
}

export function getCurrentUserRoles() {
  const token = getAccessToken();
  if (!token) {
    return [];
  }
  return getRealmRoles(token);
}

function getLandingPathByRoles(roles) {
  if (roles.includes('ROLE_ADMIN')) {
    return '/dashboard/admin';
  }
  if (roles.includes('ROLE_LIVREUR')) {
    return '/dashboard/courier';
  }
  if (roles.includes('ROLE_CLIENT') || roles.includes('ROLE_CLIENT_PRO')) {
    return '/dashboard/client';
  }
  return '/dashboard/client';
}

export function resolveLandingPathForRoles(roles) {
  return getLandingPathByRoles(Array.isArray(roles) ? roles : []);
}

export function resolveLandingPathForCurrentUser() {
  return getLandingPathByRoles(getCurrentUserRoles());
}

export function userHasAnyRole(expectedRoles) {
  if (!Array.isArray(expectedRoles) || expectedRoles.length === 0) {
    return true;
  }
  const currentRoles = getCurrentUserRoles();
  return expectedRoles.some((role) => currentRoles.includes(role));
}

export function getAccessToken() {
  return storageGet(TOKEN_STORAGE_KEY);
}

function clearAuthStorage() {
  storageRemove(TOKEN_STORAGE_KEY);
  storageRemove(REFRESH_TOKEN_STORAGE_KEY);
  storageRemove(OIDC_STATE_KEY);
  storageRemove(OIDC_VERIFIER_KEY);
  storageRemove(OIDC_REDIRECT_KEY);
}

export function hasValidAccessToken() {
  const token = getAccessToken();
  if (!token) {
    return false;
  }
  const payload = parseJwt(token);
  if (!payload || !payload.exp) {
    return false;
  }
  const now = Math.floor(Date.now() / 1000);
  return payload.exp > now + 10;
}

export async function redirectToLogin(options = {}) {
  const authUrl = await buildAuthorizeUrl(options);
  window.location.assign(authUrl);
}

export function logout() {
  cancelPendingRequests('Logout in progress');
  clearAuthStorage();
  const postLogoutRedirectUri = isMobileRuntime() ? MOBILE_REDIRECT_URI : `${window.location.origin}/`;
  const logoutUrl = new URL(`${getAuthBaseUrl()}/realms/${REALM}/protocol/openid-connect/logout`);
  logoutUrl.searchParams.set('client_id', CLIENT_ID);
  logoutUrl.searchParams.set('post_logout_redirect_uri', postLogoutRedirectUri);
  window.location.assign(logoutUrl.toString());
}

async function processAuthCallback(sourceUrl) {
  const url = new URL(sourceUrl);
  const code = url.searchParams.get('code');
  const state = url.searchParams.get('state');
  if (!code) {
    return { handled: false, redirected: false };
  }

  const expectedState = storageGet(OIDC_STATE_KEY);
  const codeVerifier = storageGet(OIDC_VERIFIER_KEY);
  const redirectUri = getRedirectUri();

  if (!expectedState || expectedState !== state || !codeVerifier) {
    if (!isMobileRuntime()) {
      url.searchParams.delete('code');
      url.searchParams.delete('state');
      url.searchParams.delete('session_state');
      url.searchParams.delete('iss');
      window.history.replaceState({}, document.title, `${url.pathname}${url.search}${url.hash}`);
    }
    await redirectToLogin();
    return { handled: true, redirected: true };
  }

  const tokenUrl = `${getAuthBaseUrl()}/realms/${REALM}/protocol/openid-connect/token`;
  const form = new URLSearchParams();
  form.set('grant_type', 'authorization_code');
  form.set('client_id', CLIENT_ID);
  form.set('code', code);
  form.set('redirect_uri', redirectUri);
  form.set('code_verifier', codeVerifier);

  const response = await fetch(tokenUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: form.toString()
  });

  if (!response.ok) {
    throw new Error(`Token exchange failed with status ${response.status}`);
  }

  const json = await response.json();
  const accessToken = json.access_token || '';
  storageSet(TOKEN_STORAGE_KEY, accessToken);
  storageRemove(REFRESH_TOKEN_STORAGE_KEY);

  storageRemove(OIDC_STATE_KEY);
  storageRemove(OIDC_VERIFIER_KEY);
  storageRemove(OIDC_REDIRECT_KEY);

  if (!isMobileRuntime()) {
    url.searchParams.delete('code');
    url.searchParams.delete('state');
    url.searchParams.delete('session_state');
    url.searchParams.delete('iss');
  }

  const roles = getRealmRoles(accessToken);
  const landingPath = getLandingPathByRoles(roles);
  const finalPath = ALLOWED_REDIRECTS.has(landingPath) ? landingPath : '/';
  if (isMobileRuntime()) {
    window.history.replaceState({}, document.title, finalPath);
    window.dispatchEvent(new PopStateEvent('popstate'));
    return { handled: true, redirected: false };
  } else {
    const destinationUrl = `${window.location.origin}${finalPath}`;
    if (window.location.pathname !== finalPath) {
      window.location.replace(destinationUrl);
      return { handled: true, redirected: true };
    } else {
      window.history.replaceState({}, document.title, finalPath);
      return { handled: true, redirected: false };
    }
  }
}

export async function handleAuthCallback(sourceUrl = window.location.href) {
  return processAuthCallback(sourceUrl);
}

export async function initializeMobileAuthCallbackListener() {
  if (!isMobileRuntime()) {
    return;
  }

  const launchUrl = await CapacitorApp.getLaunchUrl();
  if (launchUrl?.url && launchUrl.url.startsWith(MOBILE_REDIRECT_URI)) {
    await processAuthCallback(launchUrl.url);
  }

  await CapacitorApp.addListener('appUrlOpen', async ({ url }) => {
    if (!url || !url.startsWith(MOBILE_REDIRECT_URI)) {
      return;
    }
    try {
      await processAuthCallback(url);
    } catch (error) {
      console.error('Mobile OIDC callback processing failed:', error);
    }
  });
}
