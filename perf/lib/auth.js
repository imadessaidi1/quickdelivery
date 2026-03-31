import http from 'k6/http';
import { authHeaders, config } from './config.js';
import { asJson, hasEnv } from './utils.js';

const TOKEN_REFRESH_SAFETY_MS = 60 * 1000;

const adminTokenCache = {
  token: config.adminBearerToken || '',
  expiresAt: 0,
};

const courierTokenCache = {
  token: config.courierBearerToken || config.trackingBearerToken || '',
  expiresAt: 0,
};

function requestAccessToken(username, password, fallbackToken) {
  if (!hasEnv(username, password)) {
    return fallbackToken || '';
  }

  const response = http.post(
    `${config.authBaseUrl}/realms/${encodeURIComponent(config.oauthRealm)}/protocol/openid-connect/token`,
    {
      grant_type: 'password',
      client_id: config.oauthClientId,
      client_secret: config.oauthClientSecret || undefined,
      username,
      password,
      scope: 'email profile',
    },
    {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      tags: { flow: 'auth-refresh' },
      responseCallback: http.expectedStatuses(200),
    },
  );

  if (response.status !== 200) {
    console.warn(`Unable to refresh token for ${username}: HTTP ${response.status}`);
    return fallbackToken || '';
  }

  const body = asJson(response);
  if (!body?.access_token) {
    console.warn(`Unable to refresh token for ${username}: missing access_token`);
    return fallbackToken || '';
  }

  const expiresInSeconds = Number(body.expires_in || 900);
  return {
    token: body.access_token,
    expiresAt: Date.now() + Math.max(30, expiresInSeconds) * 1000,
  };
}

function resolveCachedToken(cache, username, password) {
  if (cache.token && cache.expiresAt > Date.now() + TOKEN_REFRESH_SAFETY_MS) {
    return cache.token;
  }

  if (!hasEnv(username, password)) {
    return cache.token || '';
  }

  const refreshed = requestAccessToken(username, password, cache.token);
  if (typeof refreshed === 'string') {
    cache.token = refreshed;
    if (!cache.expiresAt) {
      cache.expiresAt = Date.now() + (14 * 60 * 1000);
    }
    return cache.token;
  }

  cache.token = refreshed.token;
  cache.expiresAt = refreshed.expiresAt;
  return cache.token;
}

export function hasAdminAuth() {
  return hasEnv(config.adminBearerToken) || hasEnv(config.adminUsername, config.adminPassword);
}

export function hasCourierAuth() {
  return hasEnv(config.courierBearerToken) || hasEnv(config.courierUsername, config.courierPassword);
}

export function hasTrackingAuth() {
  return hasEnv(config.trackingBearerToken) || hasEnv(config.courierUsername, config.courierPassword);
}

export function resolveAdminToken(explicitToken = '') {
  if (explicitToken) {
    return explicitToken;
  }
  return resolveCachedToken(adminTokenCache, config.adminUsername, config.adminPassword);
}

export function resolveCourierToken(explicitToken = '') {
  if (explicitToken) {
    return explicitToken;
  }
  return resolveCachedToken(courierTokenCache, config.courierUsername, config.courierPassword);
}

export function resolveTrackingToken(explicitToken = '') {
  if (explicitToken) {
    return explicitToken;
  }
  if (hasEnv(config.courierUsername, config.courierPassword)) {
    return resolveCourierToken('');
  }
  if (config.trackingBearerToken) {
    return config.trackingBearerToken;
  }
  return resolveCourierToken('');
}

export function adminHeaders(explicitToken = '') {
  return authHeaders(resolveAdminToken(explicitToken));
}

export function courierHeaders(explicitToken = '') {
  return authHeaders(resolveCourierToken(explicitToken));
}

export function trackingHeaders(explicitToken = '') {
  return authHeaders(resolveTrackingToken(explicitToken));
}
