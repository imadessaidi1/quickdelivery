#!/usr/bin/env bash
set -euo pipefail

APP_DOMAIN="${APP_DOMAIN:-app.quickdelivery.fr}"
API_DOMAIN="${API_DOMAIN:-api.quickdelivery.fr}"
AUTH_DOMAIN="${AUTH_DOMAIN:-auth.quickdelivery.fr}"
RELEASE_ROLE="${RELEASE_ROLE:-}"
CONFIG_USERNAME="${CONFIG_USERNAME:-configuser}"
CONFIG_SERVER_PASSWORD="${CONFIG_SERVER_PASSWORD:-${CONFIG_PASSWORD:-}}"

if [[ -z "$RELEASE_ROLE" ]]; then
  echo "RELEASE_ROLE is required." >&2
  exit 1
fi

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

wait_for_mysql() {
  local label="$1"
  local max_attempts="${2:-24}"
  local delay_seconds="${3:-5}"
  local attempt

  for attempt in $(seq 1 "$max_attempts"); do
    if docker exec quickdelivery-mysql sh -c 'mysqladmin ping -h 127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" --silent' >/dev/null 2>&1; then
      echo "$label: OK"
      return 0
    fi
    sleep "$delay_seconds"
  done

  echo "$label: FAILED" >&2
  return 1
}

echo "== Docker containers =="
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
echo

case "$RELEASE_ROLE" in
  vm1-gateway)
    wait_for_http "api-gateway local" "http://127.0.0.1:8443/actuator/health" "-fsS" 24 5
    wait_for_http "front vhost" "https://${APP_DOMAIN}/" "-k -fsSI --resolve ${APP_DOMAIN}:443:127.0.0.1" 24 5
    wait_for_http "api vhost" "https://${API_DOMAIN}/actuator/health" "-k -fsSI --resolve ${API_DOMAIN}:443:127.0.0.1" 24 5
    wait_for_http "auth vhost via vm1" "https://${AUTH_DOMAIN}/auth" "-k -fsSI --resolve ${AUTH_DOMAIN}:443:127.0.0.1" 24 5
    ;;
  vm2-platform)
    if [[ -n "$CONFIG_SERVER_PASSWORD" ]]; then
      wait_for_http "config-server" "http://127.0.0.1:8889/actuator/health" "-fsS -u ${CONFIG_USERNAME}:${CONFIG_SERVER_PASSWORD}" 24 5
    else
      echo "config-server auth check skipped (CONFIG_SERVER_PASSWORD not set)"
    fi
    wait_for_http "discovery-server" "http://127.0.0.1:8080/actuator/health" "-fsS" 24 5
    wait_for_http "oauth local" "http://127.0.0.1:18443/auth" "-fsSI" 24 5
    ;;
  vm3-app|vm4-app)
    wait_for_http "users-service" "http://127.0.0.1:8081/actuator/health" "-fsS" 24 5
    wait_for_http "packages-service" "http://127.0.0.1:8082/actuator/health" "-fsS" 24 5
    ;;
  vm5-mysql)
    wait_for_mysql "mysql" 36 5
    ;;
  *)
    echo "Unsupported RELEASE_ROLE: $RELEASE_ROLE" >&2
    exit 1
    ;;
esac

echo
echo "5vm release verification completed for $RELEASE_ROLE."
