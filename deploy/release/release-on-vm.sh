#!/usr/bin/env bash
set -euo pipefail

APP_DOMAIN="${APP_DOMAIN:-app.quickdelivery.fr}"
API_DOMAIN="${API_DOMAIN:-api.quickdelivery.fr}"
AUTH_DOMAIN="${AUTH_DOMAIN:-auth.quickdelivery.fr}"
PROJECT_ROOT="${PROJECT_ROOT:-/opt/quickdelivery}"
ENV_FILE="${ENV_FILE:-$PROJECT_ROOT/deploy/docker/.env.production}"
COMPOSE_FILE="${COMPOSE_FILE:-$PROJECT_ROOT/deploy/docker/docker-compose.production.yml}"
NGINX_TEMPLATE="${NGINX_TEMPLATE:-/tmp/quickdelivery.conf}"
NGINX_CONF="${NGINX_CONF:-/etc/nginx/sites-available/quickdelivery.conf}"
FRONT_TMP_DIR="${FRONT_TMP_DIR:-/tmp/quickdelivery-front-dist}"
FRONT_PUBLIC_DIR="${FRONT_PUBLIC_DIR:-/var/www/quickdelivery-front}"
CERTBOT_WEBROOT="${CERTBOT_WEBROOT:-/var/www/certbot}"
SWAP_FILE="${SWAP_FILE:-/swapfile}"
SWAP_SIZE="${SWAP_SIZE:-2G}"
RELEASE_MODE="${RELEASE_MODE:-full}"
RELEASE_MODULES="${RELEASE_MODULES:-}"

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

wait_for_container_log() {
  local container_name="$1"
  local pattern="$2"
  local max_attempts="${3:-36}"
  local delay_seconds="${4:-5}"
  local attempt

  for attempt in $(seq 1 "$max_attempts"); do
    if docker logs "$container_name" 2>&1 | grep -q "$pattern"; then
      echo "$container_name is ready."
      return 0
    fi
    sleep "$delay_seconds"
  done

  echo "$container_name did not become ready (pattern: $pattern)" >&2
  return 1
}

require_file() {
  local path="$1"
  if [[ ! -e "$path" ]]; then
    echo "Required file not found: $path" >&2
    exit 1
  fi
}

ensure_swap() {
  if swapon --show | grep -q .; then
    echo "Swap already enabled."
    return
  fi

  echo "No swap detected. Creating $SWAP_SIZE swap file..."
  fallocate -l "$SWAP_SIZE" "$SWAP_FILE"
  chmod 600 "$SWAP_FILE"
  mkswap "$SWAP_FILE"
  swapon "$SWAP_FILE"
  if ! grep -q "^$SWAP_FILE " /etc/fstab; then
    echo "$SWAP_FILE none swap sw 0 0" >> /etc/fstab
  fi
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
  if [[ ! -f "/etc/letsencrypt/live/$API_DOMAIN/fullchain.pem" ]]; then
    certbot --nginx -d "$API_DOMAIN" -d "$AUTH_DOMAIN"
  else
    echo "API/Auth certificate already present."
  fi

  if [[ ! -f "/etc/letsencrypt/live/$APP_DOMAIN/fullchain.pem" ]]; then
    certbot certonly --webroot -w "$CERTBOT_WEBROOT" -d "$APP_DOMAIN"
  else
    echo "App certificate already present."
  fi
}

install_final_nginx() {
  require_file "$NGINX_TEMPLATE"
  cp "$NGINX_TEMPLATE" "$NGINX_CONF"
  sed -i \
    -e "s/app.quickdelivery.tld/$APP_DOMAIN/g" \
    -e "s/api.quickdelivery.tld/$API_DOMAIN/g" \
    -e "s/auth.quickdelivery.tld/$AUTH_DOMAIN/g" \
    "$NGINX_CONF"

  ln -sf "$NGINX_CONF" /etc/nginx/sites-enabled/quickdelivery.conf
  rm -f /etc/nginx/sites-enabled/default
  nginx -t
  systemctl reload nginx
}

restart_backend() {
  require_file "$ENV_FILE"
  require_file "$COMPOSE_FILE"

  chmod -R a+rX "$PROJECT_ROOT/deploy/docker/mysql"
  find "$PROJECT_ROOT/deploy/docker/mysql" -type f -exec chmod 644 {} \;
  find "$PROJECT_ROOT/deploy/docker/mysql" -type d -exec chmod 755 {} \;

  cd "$PROJECT_ROOT"
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --build
}

resolve_backend_services() {
  local modules_csv="$1"
  local services=()
  local item
  IFS=',' read -r -a requested <<< "$modules_csv"

  for item in "${requested[@]}"; do
    case "$item" in
      config-server|quickdelivery-config-server.jar)
        services+=("config-server")
        ;;
      discovery-server|quickdelivery-registry-server.jar)
        services+=("discovery-server")
        ;;
      oauth|oauth-authorization-server.jar)
        services+=("oauth-authorization-server")
        ;;
      api-gateway|quickdelivery-api-gateway.jar)
        services+=("api-gateway")
        ;;
      users|quickdelivery-users.jar)
        services+=("users-service")
        ;;
      packages|quickdelivery-packages.jar)
        services+=("packages-service")
        ;;
      "")
        ;;
      *)
        echo "Unsupported module in RELEASE_MODULES: $item" >&2
        exit 1
        ;;
    esac
  done

  printf '%s\n' "${services[@]}" | awk 'NF && !seen[$0]++'
}

restart_backend_modules() {
  require_file "$ENV_FILE"
  require_file "$COMPOSE_FILE"

  if [[ -z "$RELEASE_MODULES" ]]; then
    echo "RELEASE_MODULES is required when RELEASE_MODE=module" >&2
    exit 1
  fi

  chmod -R a+rX "$PROJECT_ROOT/deploy/docker/mysql"
  find "$PROJECT_ROOT/deploy/docker/mysql" -type f -exec chmod 644 {} \;
  find "$PROJECT_ROOT/deploy/docker/mysql" -type d -exec chmod 755 {} \;

  mapfile -t services < <(resolve_backend_services "$RELEASE_MODULES")
  if [[ "${#services[@]}" -eq 0 ]]; then
    echo "No backend services resolved from RELEASE_MODULES=$RELEASE_MODULES" >&2
    exit 1
  fi

  cd "$PROJECT_ROOT"
  local infra_services=()
  local app_services=()
  local service

  for service in "${services[@]}"; do
    case "$service" in
      config-server|discovery-server)
        infra_services+=("$service")
        ;;
      *)
        app_services+=("$service")
        ;;
    esac
  done

  if [[ " ${infra_services[*]} " == *" config-server "* ]]; then
    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --build config-server
  fi
  wait_for_url "config-server" "http://127.0.0.1:8889/actuator/health"

  if [[ " ${infra_services[*]} " == *" discovery-server "* ]]; then
    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --build discovery-server
  fi
  if [[ " ${infra_services[*]} " == *" discovery-server "* || " ${app_services[*]} " == *" api-gateway "* || " ${app_services[*]} " == *" users-service "* || " ${app_services[*]} " == *" packages-service "* ]]; then
    wait_for_container_log "quickdelivery-discovery-server" "Started ServiceRegistrationAndDiscoveryServer" 36 5
  fi

  if [[ "${#app_services[@]}" -gt 0 ]]; then
    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --build "${app_services[@]}"
  fi
}

main() {
  if [[ "${EUID}" -ne 0 ]]; then
    echo "Run as root: sudo bash $0" >&2
    exit 1
  fi

  require_file "$PROJECT_ROOT/deploy/docker/docker-compose.production.yml"
  ensure_swap

  case "$RELEASE_MODE" in
    full)
      install_front
      restart_backend
      bootstrap_nginx
      ensure_certificates
      install_final_nginx
      ;;
    backend)
      restart_backend
      ;;
    module)
      restart_backend_modules
      ;;
    frontend)
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

  bash "$PROJECT_ROOT/deploy/release/verify-release.sh"
}

main "$@"
