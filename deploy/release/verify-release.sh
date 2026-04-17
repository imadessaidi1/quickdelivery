#!/usr/bin/env bash
set -euo pipefail

APP_DOMAIN="${APP_DOMAIN:-app.quickdelivery.fr}"
API_DOMAIN="${API_DOMAIN:-api.quickdelivery.fr}"
AUTH_DOMAIN="${AUTH_DOMAIN:-auth.quickdelivery.fr}"
CONFIG_USERNAME="${CONFIG_USERNAME:-configuser}"
CONFIG_SERVER_PASSWORD="${CONFIG_SERVER_PASSWORD:-${CONFIG_PASSWORD:-}}"

wait_for_http() {
  local label="$1"
  local url="$2"
  local extra_args="${3:-}"
  local max_attempts="${4:-12}"
  local delay_seconds="${5:-5}"
  local attempt

  for attempt in $(seq 1 "$max_attempts"); do
    if [[ -n "$extra_args" ]]; then
      if eval "curl $extra_args '$url' >/dev/null 2>&1"; then
        echo "$label: OK"
        return 0
      fi
    else
      if curl -fsS "$url" >/dev/null 2>&1; then
        echo "$label: OK"
        return 0
      fi
    fi
    sleep "$delay_seconds"
  done

  echo "$label: FAILED" >&2
  return 1
}

echo "== Docker containers =="
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'

echo
echo "== Local checks =="
if [[ -n "$CONFIG_SERVER_PASSWORD" ]]; then
  wait_for_http "config-server" "http://127.0.0.1:8889/actuator/health" "-fsS -u ${CONFIG_USERNAME}:${CONFIG_SERVER_PASSWORD}" 24 5
else
  echo "config-server: skipped (CONFIG_SERVER_PASSWORD not set)"
fi

wait_for_http "api-gateway local" "http://127.0.0.1:8443/actuator/health" "-fsS" 36 5

wait_for_http "oauth local" "http://127.0.0.1:18443/auth" "-fsSI" 24 5

echo
echo "== Public vhost checks =="
wait_for_http "front vhost" "https://${APP_DOMAIN}/" "-k -fsSI --resolve ${APP_DOMAIN}:443:127.0.0.1" 24 5

if curl -k -sSI --resolve "${APP_DOMAIN}:443:127.0.0.1" "https://${APP_DOMAIN}/js/asset-that-should-not-exist.js" | grep -q "404"; then
  echo "front missing asset handling: OK"
else
  echo "front missing asset handling: FAILED" >&2
  exit 1
fi

wait_for_http "api vhost" "https://${API_DOMAIN}/actuator/health" "-k -fsSI --resolve ${API_DOMAIN}:443:127.0.0.1" 24 5

wait_for_http "auth vhost" "https://${AUTH_DOMAIN}/auth" "-k -fsSI --resolve ${AUTH_DOMAIN}:443:127.0.0.1" 24 5

echo
echo "Release verification completed."
