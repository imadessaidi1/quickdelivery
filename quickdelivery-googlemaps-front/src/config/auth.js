const AUTH_BASE_URL = 'http://localhost:18084/auth';
const REALM = 'quickdelivery';
const CLIENT_ID = 'quickdelivery-front';

const TOKEN_STORAGE_KEY = 'qd_access_token';
const REFRESH_TOKEN_STORAGE_KEY = 'qd_refresh_token';
const OIDC_STATE_KEY = 'qd_oidc_state';
const OIDC_VERIFIER_KEY = 'qd_oidc_verifier';
const OIDC_REDIRECT_KEY = 'qd_oidc_redirect';

const ALLOWED_REDIRECTS = new Set([
  '/createPackage',
  '/app',
  '/usersAccountValidation'
]);

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

async function buildAuthorizeUrl(options = {}) {
  const codeVerifier = randomString(96);
  let codeChallengeMethod = 'S256';
  let codeChallenge = '';
  if (window.crypto && window.crypto.subtle) {
    codeChallenge = base64UrlEncode(await sha256(codeVerifier));
  } else {
    codeChallengeMethod = 'plain';
    codeChallenge = codeVerifier;
  }
  const state = randomString(32);
  const redirectPath = defaultRedirectPath();
  const redirectUri = `${window.location.origin}${redirectPath}`;

  localStorage.setItem(OIDC_STATE_KEY, state);
  localStorage.setItem(OIDC_VERIFIER_KEY, codeVerifier);
  localStorage.setItem(OIDC_REDIRECT_KEY, redirectPath);

  const authorizeUrl = new URL(`${AUTH_BASE_URL}/realms/${REALM}/protocol/openid-connect/auth`);
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
    return '/usersAccountValidation';
  }
  if (roles.includes('ROLE_LIVREUR')) {
    return '/app';
  }
  if (roles.includes('ROLE_CLIENT') || roles.includes('ROLE_CLIENT_PRO')) {
    return '/createPackage';
  }
  return '/app';
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
  return localStorage.getItem(TOKEN_STORAGE_KEY);
}

function clearAuthStorage() {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
  localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
  localStorage.removeItem(OIDC_STATE_KEY);
  localStorage.removeItem(OIDC_VERIFIER_KEY);
  localStorage.removeItem(OIDC_REDIRECT_KEY);
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
  clearAuthStorage();
  const postLogoutRedirectUri = `${window.location.origin}/`;
  const logoutUrl = new URL(`${AUTH_BASE_URL}/realms/${REALM}/protocol/openid-connect/logout`);
  logoutUrl.searchParams.set('client_id', CLIENT_ID);
  logoutUrl.searchParams.set('post_logout_redirect_uri', postLogoutRedirectUri);
  window.location.assign(logoutUrl.toString());
}

export async function handleAuthCallback() {
  const url = new URL(window.location.href);
  const code = url.searchParams.get('code');
  const state = url.searchParams.get('state');
  if (!code) {
    return;
  }

  const expectedState = localStorage.getItem(OIDC_STATE_KEY);
  const codeVerifier = localStorage.getItem(OIDC_VERIFIER_KEY);
  const redirectPath = localStorage.getItem(OIDC_REDIRECT_KEY) || defaultRedirectPath();
  const redirectUri = `${window.location.origin}${redirectPath}`;

  if (!expectedState || expectedState !== state || !codeVerifier) {
    // State can be missing after manual refresh/storage clear.
    // Clean callback params and restart a fresh auth flow.
    url.searchParams.delete('code');
    url.searchParams.delete('state');
    url.searchParams.delete('session_state');
    url.searchParams.delete('iss');
    window.history.replaceState({}, document.title, `${url.pathname}${url.search}${url.hash}`);
    await redirectToLogin();
    return;
  }

  const tokenUrl = `${AUTH_BASE_URL}/realms/${REALM}/protocol/openid-connect/token`;
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
  localStorage.setItem(TOKEN_STORAGE_KEY, accessToken);
  localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, json.refresh_token || '');

  localStorage.removeItem(OIDC_STATE_KEY);
  localStorage.removeItem(OIDC_VERIFIER_KEY);
  localStorage.removeItem(OIDC_REDIRECT_KEY);

  url.searchParams.delete('code');
  url.searchParams.delete('state');
  url.searchParams.delete('session_state');
  url.searchParams.delete('iss');

  const roles = getRealmRoles(accessToken);
  const landingPath = getLandingPathByRoles(roles);
  const finalPath = ALLOWED_REDIRECTS.has(landingPath) ? landingPath : '/';
  const destinationUrl = `${window.location.origin}${finalPath}`;
  if (window.location.pathname !== finalPath) {
    window.location.replace(destinationUrl);
  } else {
    window.history.replaceState({}, document.title, finalPath);
  }
}
