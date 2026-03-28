import { check } from 'k6';
import exec from 'k6/execution';

export function uniqueSuffix() {
  return `${exec.scenario.name}-${exec.vu.idInTest}-${exec.scenario.iterationInTest}-${Date.now()}`;
}

export function futureIso(minutesAhead) {
  return new Date(Date.now() + minutesAhead * 60 * 1000).toISOString();
}

export function asJson(response) {
  try {
    return response.json();
  } catch (_) {
    return null;
  }
}

export function ensureResponse(response, validation, tags) {
  return check(response, validation, tags);
}

export function hasEnv(...values) {
  return values.every((value) => value !== undefined && value !== null && `${value}`.trim() !== '');
}

export function sleepRange(minSeconds, maxSeconds) {
  const seconds = minSeconds + (Math.random() * (maxSeconds - minSeconds));
  return seconds;
}
