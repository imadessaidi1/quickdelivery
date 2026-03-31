#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="${PROJECT_ROOT:-/opt/quickdelivery}"
RELEASE_ROLE="${RELEASE_ROLE:-}"
RELEASE_MODE="${RELEASE_MODE:-full}"
RELEASE_MODULES="${RELEASE_MODULES:-}"
FRONT_TMP_DIR="${FRONT_TMP_DIR:-/tmp/quickdelivery-front-dist}"
FRONT_PUBLIC_DIR="${FRONT_PUBLIC_DIR:-/var/www/quickdelivery-front}"
CERTBOT_WEBROOT="${CERTBOT_WEBROOT:-/var/www/certbot}"
NGINX_TEMPLATE="${NGINX_TEMPLATE:-/tmp/quickdelivery.5vm.conf}"
NGINX_CONF="${NGINX_CONF:-/etc/nginx/sites-available/quickdelivery.conf}"
APP_DOMAIN="${APP_DOMAIN:-app.quickdelivery.fr}"
API_DOMAIN="${API_DOMAIN:-api.quickdelivery.fr}"
AUTH_DOMAIN="${AUTH_DOMAIN:-auth.quickdelivery.fr}"
CERTBOT_EMAIL="${CERTBOT_EMAIL:-}"
SWAP_FILE="${SWAP_FILE:-/swapfile}"
SWAP_SIZE="${SWAP_SIZE:-2G}"
COMPOSE_BIN=""

if [[ -z "$RELEASE_ROLE" ]]; then
  echo "RELEASE_ROLE is required." >&2
  exit 1
fi

ENV_FILE="${ENV_FILE:-$PROJECT_ROOT/deploy/docker/5vm/.env.$RELEASE_ROLE}"

case "$RELEASE_ROLE" in
  vm1-gateway)
    COMPOSE_FILE="$PROJECT_ROOT/deploy/docker/5vm/docker-compose.vm1-gateway.yml"
    ;;
  vm2-platform)
    COMPOSE_FILE="$PROJECT_ROOT/deploy/docker/5vm/docker-compose.vm2-platform.yml"
    ;;
  vm3-app)
    COMPOSE_FILE="$PROJECT_ROOT/deploy/docker/5vm/docker-compose.vm3-app.yml"
    ;;
  vm4-app)
    COMPOSE_FILE="$PROJECT_ROOT/deploy/docker/5vm/docker-compose.vm4-app.yml"
    ;;
  vm5-mysql)
    COMPOSE_FILE="$PROJECT_ROOT/deploy/docker/5vm/docker-compose.vm5-mysql.yml"
    ;;
  *)
    echo "Unsupported RELEASE_ROLE: $RELEASE_ROLE" >&2
    exit 1
    ;;
esac

require_file() {
  local path="$1"
  if [[ ! -e "$path" ]]; then
    echo "Required file not found: $path" >&2
    exit 1
  fi
}

load_env_file() {
  require_file "$ENV_FILE"
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ -z "$line" ]] && continue
    [[ "$line" =~ ^[[:space:]]*# ]] && continue
    export "$line"
  done < "$ENV_FILE"
}

ensure_swap() {
  if swapon --show | grep -q .; then
    return
  fi

  fallocate -l "$SWAP_SIZE" "$SWAP_FILE"
  chmod 600 "$SWAP_FILE"
  mkswap "$SWAP_FILE"
  swapon "$SWAP_FILE"
  if ! grep -q "^$SWAP_FILE " /etc/fstab; then
    echo "$SWAP_FILE none swap sw 0 0" >> /etc/fstab
  fi
}

ensure_apt_package() {
  local package_name="$1"
  if ! dpkg -s "$package_name" >/dev/null 2>&1; then
    apt-get install -y "$package_name"
  fi
}

install_role_prerequisites() {
  apt-get update

  if ! command -v docker >/dev/null 2>&1; then
    ensure_apt_package docker.io
  fi

  if ! docker compose version >/dev/null 2>&1; then
    if apt-cache show docker-compose-plugin >/dev/null 2>&1; then
      ensure_apt_package docker-compose-plugin
    fi
    if ! docker compose version >/dev/null 2>&1; then
      ensure_apt_package docker-compose
    fi
  fi

  systemctl enable --now docker

  if [[ "$RELEASE_ROLE" == "vm1-gateway" ]]; then
    ensure_apt_package nginx
    ensure_apt_package certbot
    ensure_apt_package python3-certbot-nginx
    systemctl enable --now nginx
  fi
}

resolve_compose_bin() {
  if docker compose version >/dev/null 2>&1; then
    COMPOSE_BIN="docker compose"
    return
  fi

  if command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_BIN="docker-compose"
    return
  fi

  echo "Neither 'docker compose' nor 'docker-compose' is available." >&2
  exit 1
}

wait_for_url() {
  local label="$1"
  local url="$2"
  local max_attempts="${3:-24}"
  local delay_seconds="${4:-5}"
  local attempt

  for attempt in $(seq 1 "$max_attempts"); do
    if curl -fsS "$url" >/dev/null; then
      echo "$label is ready."
      return 0
    fi
    sleep "$delay_seconds"
  done

  echo "$label did not become ready: $url" >&2
  return 1
}

url_to_hostport() {
  local raw="$1"
  raw="${raw#http://}"
  raw="${raw#https://}"
  raw="${raw#ws://}"
  raw="${raw#wss://}"
  raw="${raw%%/*}"
  printf '%s' "$raw"
}

install_front() {
  require_file "$FRONT_TMP_DIR/index.html"
  mkdir -p "$FRONT_PUBLIC_DIR"
  rm -rf "$FRONT_PUBLIC_DIR"/*
  cp -R "$FRONT_TMP_DIR"/. "$FRONT_PUBLIC_DIR"/
  chown -R www-data:www-data "$FRONT_PUBLIC_DIR"
}

bootstrap_nginx() {
  mkdir -p "$CERTBOT_WEBROOT"
  cat > "$NGINX_CONF" <<EOF
server {
    listen 80;
    listen [::]:80;
    server_name $APP_DOMAIN $API_DOMAIN $AUTH_DOMAIN;

    location /.well-known/acme-challenge/ {
        root $CERTBOT_WEBROOT;
    }

    location / {
        return 200 "quickdelivery nginx bootstrap ok\n";
        add_header Content-Type text/plain;
    }
}
EOF

  ln -sf "$NGINX_CONF" /etc/nginx/sites-enabled/quickdelivery.conf
  rm -f /etc/nginx/sites-enabled/default
  nginx -t
  systemctl restart nginx
}

ensure_certificates() {
  if pgrep -f "/usr/bin/certbot" >/dev/null 2>&1; then
    pkill -f "/usr/bin/certbot" || true
    sleep 2
  fi

  local certbot_args=(
    --non-interactive
    --agree-tos
    --keep-until-expiring
  )

  if [[ -n "$CERTBOT_EMAIL" ]]; then
    certbot_args+=(--email "$CERTBOT_EMAIL")
  else
    certbot_args+=(--register-unsafely-without-email)
  fi

  if [[ ! -f "/etc/letsencrypt/live/$API_DOMAIN/fullchain.pem" ]]; then
    certbot --nginx "${certbot_args[@]}" -d "$API_DOMAIN" -d "$AUTH_DOMAIN"
  fi

  if [[ ! -f "/etc/letsencrypt/live/$APP_DOMAIN/fullchain.pem" ]]; then
    certbot --nginx "${certbot_args[@]}" -d "$APP_DOMAIN"
  fi
}

install_final_nginx() {
  require_file "$NGINX_TEMPLATE"
  local auth_backend_hostport
  local gateway_internal_token_escaped
  local packages_ws_backend_hostport

  auth_backend_hostport="$(url_to_hostport "${KEYCLOAK_INTERNAL_URL:-http://10.0.2.10:18443/auth}")"
  packages_ws_backend_hostport="$(url_to_hostport "${PACKAGES_WS_PINNED_URL:-ws://10.0.3.10:8082}")"
  gateway_internal_token_escaped="$(printf '%s' "${GATEWAY_INTERNAL_TOKEN:-}" | sed 's/[\\/&]/\\&/g')"

  cp "$NGINX_TEMPLATE" "$NGINX_CONF"
  sed -i \
    -e "s/app.quickdelivery.tld/$APP_DOMAIN/g" \
    -e "s/api.quickdelivery.tld/$API_DOMAIN/g" \
    -e "s/auth.quickdelivery.tld/$AUTH_DOMAIN/g" \
    -e "s#10.0.2.10:18443#$auth_backend_hostport#g" \
    -e "s#10.0.3.10:8082#$packages_ws_backend_hostport#g" \
    -e "s/gateway_internal_token_placeholder/$gateway_internal_token_escaped/g" \
    "$NGINX_CONF"

  ln -sf "$NGINX_CONF" /etc/nginx/sites-enabled/quickdelivery.conf
  rm -f /etc/nginx/sites-enabled/default
  nginx -t
  systemctl reload nginx
}

resolve_role_services() {
  case "$RELEASE_ROLE" in
    vm1-gateway)
      echo "api-gateway"
      ;;
    vm2-platform)
      echo "config-server"
      echo "discovery-server"
      echo "oauth-authorization-server"
      ;;
    vm3-app|vm4-app)
      echo "users-service"
      echo "packages-service"
      ;;
    vm5-mysql)
      echo "mysql"
      ;;
  esac
}

resolve_module_services() {
  local item
  IFS=',' read -r -a requested <<< "$RELEASE_MODULES"

  for item in "${requested[@]}"; do
    case "$item" in
      config-server|quickdelivery-config-server.jar)
        echo "config-server"
        ;;
      discovery-server|quickdelivery-registry-server.jar)
        echo "discovery-server"
        ;;
      oauth|oauth-authorization-server.jar)
        echo "oauth-authorization-server"
        ;;
      api-gateway|quickdelivery-api-gateway.jar)
        echo "api-gateway"
        ;;
      users|quickdelivery-users.jar)
        echo "users-service"
        ;;
      packages|quickdelivery-packages.jar)
        echo "packages-service"
        ;;
      mysql)
        echo "mysql"
        ;;
      "")
        ;;
      *)
        echo "Unsupported module in RELEASE_MODULES: $item" >&2
        exit 1
        ;;
    esac
  done | awk 'NF && !seen[$0]++'
}

compose_up_services() {
  local services=("$@")
  require_file "$ENV_FILE"
  require_file "$COMPOSE_FILE"
  if [[ -d "$PROJECT_ROOT/deploy/docker/mysql" ]]; then
    chmod -R a+rX "$PROJECT_ROOT/deploy/docker/mysql"
    find "$PROJECT_ROOT/deploy/docker/mysql" -type f -exec chmod 644 {} \;
    find "$PROJECT_ROOT/deploy/docker/mysql" -type d -exec chmod 755 {} \;
  fi
  if [[ -d "$PROJECT_ROOT/config" ]]; then
    chmod -R a+rX "$PROJECT_ROOT/config"
    find "$PROJECT_ROOT/config" -type f -exec chmod 644 {} \;
    find "$PROJECT_ROOT/config" -type d -exec chmod 755 {} \;
  fi
  cd "$PROJECT_ROOT"
  if [[ "$COMPOSE_BIN" == "docker compose" ]]; then
    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --build "${services[@]}"
  else
    docker-compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" rm -fsv "${services[@]}" >/dev/null 2>&1 || true
    docker-compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --build "${services[@]}"
  fi
}

run_backend_release() {
  mapfile -t services < <(resolve_role_services)
  compose_up_services "${services[@]}"
}

run_module_release() {
  if [[ -z "$RELEASE_MODULES" ]]; then
    echo "RELEASE_MODULES is required when RELEASE_MODE=module" >&2
    exit 1
  fi
  mapfile -t services < <(resolve_module_services)
  if [[ "${#services[@]}" -eq 0 ]]; then
    echo "No services resolved from RELEASE_MODULES=$RELEASE_MODULES" >&2
    exit 1
  fi
  compose_up_services "${services[@]}"
}

main() {
  if [[ "${EUID}" -ne 0 ]]; then
    echo "Run as root: sudo RELEASE_ROLE=$RELEASE_ROLE bash $0" >&2
    exit 1
  fi

  ensure_swap
  install_role_prerequisites
  resolve_compose_bin
  load_env_file

  case "$RELEASE_MODE" in
    full)
      if [[ "$RELEASE_ROLE" == "vm1-gateway" ]]; then
        run_backend_release
        install_front
        bootstrap_nginx
        ensure_certificates
        install_final_nginx
      else
        run_backend_release
      fi
      ;;
    backend)
      run_backend_release
      ;;
    module)
      run_module_release
      ;;
    frontend)
      if [[ "$RELEASE_ROLE" != "vm1-gateway" ]]; then
        echo "frontend mode is supported only for vm1-gateway" >&2
        exit 1
      fi
      install_front
      if [[ ! -f "/etc/letsencrypt/live/$APP_DOMAIN/fullchain.pem" ]]; then
        bootstrap_nginx
        ensure_certificates
      fi
      install_final_nginx
      ;;
    *)
      echo "Unsupported RELEASE_MODE: $RELEASE_MODE" >&2
      exit 1
      ;;
  esac

  bash "$PROJECT_ROOT/deploy/release/verify-release-5vm.sh"
}

main "$@"
