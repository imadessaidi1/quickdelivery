import { getAuthBaseUrl } from './network';
import { App as CapacitorApp } from '@capacitor/app';
import { CapacitorHttp } from '@capacitor/core';
import { cancelPendingRequests } from './requestControl';

const REALM = 'quickdelivery';
const CLIENT_ID = 'quickdelivery-front';
const MOBILE_REDIRECT_URI = 'quickdelivery://auth/callback';

const TOKEN_STORAGE_KEY = 'qd_access_token';
const REFRESH_TOKEN_STORAGE_KEY = 'qd_refresh_token';
const OIDC_STATE_KEY = 'qd_oidc_state';
const OIDC_VERIFIER_KEY = 'qd_oidc_verifier';
const OIDC_REDIRECT_KEY = 'qd_oidc_redirect';
const LOGIN_ATTEMPT_COUNT_KEY = 'qd_login_attempt_count';
const POST_AUTH_ROUTE_KEY = 'qd_post_auth_route';
const LAST_AUTH_SUCCESS_AT_KEY = 'qd_last_auth_success_at';
const MOBILE_LOGOUT_MARKER = 'logout';

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
  if (local && isMobileRuntime()) {
    local.setItem(key, value);
  } else if (local) {
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

async function exchangeAuthorizationCode(tokenUrl, formBody) {
  if (isMobileRuntime()) {
    const response = await CapacitorHttp.post({
      url: tokenUrl,
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      data: formBody.toString(),
    });
    if (response.status < 200 || response.status >= 300) {
      throw new Error(`Token exchange failed with status ${response.status}`);
    }
    return response.data || {};
  }

  const response = await fetch(tokenUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: formBody.toString()
  });

  if (!response.ok) {
    throw new Error(`Token exchange failed with status ${response.status}`);
  }

  return response.json();
}

async function performMobileKeycloakLogout(refreshToken) {
  if (!refreshToken) {
    return;
  }

  const logoutUrl = `${getAuthBaseUrl()}/realms/${REALM}/protocol/openid-connect/logout`;
  const form = new URLSearchParams();
  form.set('client_id', CLIENT_ID);
  form.set('refresh_token', refreshToken);

  await CapacitorHttp.post({
    url: logoutUrl,
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    data: form.toString(),
  });
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
  storageRemove(POST_AUTH_ROUTE_KEY);
  storageRemove(LAST_AUTH_SUCCESS_AT_KEY);
}

export function getLoginAttemptCount() {
  const rawValue = storageGet(LOGIN_ATTEMPT_COUNT_KEY);
  const count = Number(rawValue);
  return Number.isFinite(count) && count > 0 ? count : 0;
}

export function registerLoginAttempt() {
  const nextCount = getLoginAttemptCount() + 1;
  storageSet(LOGIN_ATTEMPT_COUNT_KEY, `${nextCount}`);
  return nextCount;
}

export function resetLoginAttemptCounter() {
  storageRemove(LOGIN_ATTEMPT_COUNT_KEY);
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

export function consumePendingPostAuthRoute() {
  const pendingRoute = storageGet(POST_AUTH_ROUTE_KEY);
  storageRemove(POST_AUTH_ROUTE_KEY);
  return pendingRoute || '';
}

export function markAuthSuccess() {
  storageSet(LAST_AUTH_SUCCESS_AT_KEY, `${Date.now()}`);
}

export function wasRecentAuthSuccess(windowMs = 8000) {
  const rawValue = storageGet(LAST_AUTH_SUCCESS_AT_KEY);
  const timestamp = Number(rawValue);
  if (!Number.isFinite(timestamp) || timestamp <= 0) {
    return false;
  }
  return Date.now() - timestamp <= windowMs;
}

export async function redirectToLogin(options = {}) {
  const authUrl = await buildAuthorizeUrl(options);
  window.location.assign(authUrl);
}

export function logout() {
  const refreshToken = storageGet(REFRESH_TOKEN_STORAGE_KEY);
  cancelPendingRequests('Logout in progress');

  if (isMobileRuntime()) {
    performMobileKeycloakLogout(refreshToken)
      .catch((error) => {
        console.error('Mobile Keycloak logout failed:', error);
      })
      .finally(() => {
        clearAuthStorage();
        resetLoginAttemptCounter();
        window.location.replace(`${window.location.origin}/`);
      });
    return;
  }

  clearAuthStorage();
  resetLoginAttemptCounter();

  const postLogoutRedirectUri = isMobileRuntime()
    ? `${MOBILE_REDIRECT_URI}?${MOBILE_LOGOUT_MARKER}=1`
    : `${window.location.origin}/`;
  const logoutUrl = new URL(`${getAuthBaseUrl()}/realms/${REALM}/protocol/openid-connect/logout`);
  logoutUrl.searchParams.set('client_id', CLIENT_ID);
  logoutUrl.searchParams.set('post_logout_redirect_uri', postLogoutRedirectUri);
  window.location.assign(logoutUrl.toString());
}

function processMobileLogoutCallback(sourceUrl) {
  if (!isMobileRuntime()) {
    return false;
  }

  const url = new URL(sourceUrl);
  if (url.searchParams.get(MOBILE_LOGOUT_MARKER) !== '1') {
    return false;
  }

  clearAuthStorage();
  resetLoginAttemptCounter();
  storageSet(POST_AUTH_ROUTE_KEY, '/');
  window.history.replaceState({}, document.title, '/');
  window.dispatchEvent(new CustomEvent('qd-auth-route-resolved', { detail: { path: '/' } }));
  window.dispatchEvent(new PopStateEvent('popstate'));
  return true;
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

  const json = await exchangeAuthorizationCode(tokenUrl, form);
  const accessToken = json.access_token || '';
  const refreshToken = json.refresh_token || '';
  storageSet(TOKEN_STORAGE_KEY, accessToken);
  if (refreshToken) {
    storageSet(REFRESH_TOKEN_STORAGE_KEY, refreshToken);
  } else {
    storageRemove(REFRESH_TOKEN_STORAGE_KEY);
  }
  resetLoginAttemptCounter();
  markAuthSuccess();

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
    storageSet(POST_AUTH_ROUTE_KEY, finalPath);
    window.history.replaceState({}, document.title, finalPath);
    window.dispatchEvent(new CustomEvent('qd-auth-route-resolved', { detail: { path: finalPath } }));
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
  if (processMobileLogoutCallback(sourceUrl)) {
    return { handled: true, redirected: false };
  }
  return processAuthCallback(sourceUrl);
}

export async function initializeMobileAuthCallbackListener() {
  if (!isMobileRuntime()) {
    return;
  }

  const launchUrl = await CapacitorApp.getLaunchUrl();
  if (launchUrl?.url && launchUrl.url.startsWith(MOBILE_REDIRECT_URI)) {
    await processAuthCallback(launchUrl.url);
    processMobileLogoutCallback(launchUrl.url);
  }

  await CapacitorApp.addListener('appUrlOpen', async ({ url }) => {
    if (!url || !url.startsWith(MOBILE_REDIRECT_URI)) {
      return;
    }
    try {
      if (processMobileLogoutCallback(url)) {
        return;
      }
      await processAuthCallback(url);
    } catch (error) {
      console.error('Mobile OIDC callback processing failed:', error);
    }
  });
}
