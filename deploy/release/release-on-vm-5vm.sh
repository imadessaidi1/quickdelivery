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
NGINX_WS_UPSTREAMS_CONF="${NGINX_WS_UPSTREAMS_CONF:-/etc/nginx/quickdelivery/quickdelivery-ws-upstreams.inc}"
APP_DOMAIN="${APP_DOMAIN:-app.quickdelivery.fr}"
API_DOMAIN="${API_DOMAIN:-api.quickdelivery.fr}"
AUTH_DOMAIN="${AUTH_DOMAIN:-auth.quickdelivery.fr}"
CERTBOT_EMAIL="${CERTBOT_EMAIL:-}"
SWAP_FILE="${SWAP_FILE:-/swapfile}"
SWAP_SIZE="${SWAP_SIZE:-2G}"
VM1_LIMIT_CONN_PER_IP="${VM1_LIMIT_CONN_PER_IP:-40}"
VM1_WS_LIMIT_CONN_PER_IP="${VM1_WS_LIMIT_CONN_PER_IP:-6}"
VM1_API_PUBLIC_RATE="${VM1_API_PUBLIC_RATE:-20r/s}"
VM1_API_PUBLIC_BURST="${VM1_API_PUBLIC_BURST:-40}"
VM1_AUTH_PUBLIC_RATE="${VM1_AUTH_PUBLIC_RATE:-10r/s}"
VM1_AUTH_PUBLIC_BURST="${VM1_AUTH_PUBLIC_BURST:-20}"
VM1_TRACKING_PUBLIC_RATE="${VM1_TRACKING_PUBLIC_RATE:-15r/s}"
VM1_TRACKING_PUBLIC_BURST="${VM1_TRACKING_PUBLIC_BURST:-30}"
VM1_STATIC_CACHE_EXPIRES="${VM1_STATIC_CACHE_EXPIRES:-7d}"
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
    ensure_apt_package python3
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

configure_vm1_limits() {
  install -d -m 0755 /etc/systemd/system/nginx.service.d

  cat > /etc/systemd/system/nginx.service.d/override.conf <<'EOF'
[Service]
LimitNOFILE=65535
EOF

  cat > /etc/security/limits.d/99-quickdelivery.conf <<'EOF'
* soft nofile 65535
* hard nofile 65535
root soft nofile 65535
root hard nofile 65535
www-data soft nofile 65535
www-data hard nofile 65535
EOF

  cat > /etc/sysctl.d/99-quickdelivery-vm1.conf <<'EOF'
net.core.somaxconn = 4096
net.core.netdev_max_backlog = 16384
net.ipv4.tcp_max_syn_backlog = 4096
net.ipv4.ip_local_port_range = 10240 65535
net.ipv4.tcp_fin_timeout = 15
net.ipv4.tcp_tw_reuse = 1
net.netfilter.nf_conntrack_max = 1048576
EOF

  sysctl --system >/dev/null
  systemctl daemon-reload
}

configure_vm1_nginx_base() {
  python3 - <<'PY'
from pathlib import Path

path = Path("/etc/nginx/nginx.conf")
text = path.read_text()

if "worker_rlimit_nofile 65535;" not in text:
    text = text.replace("worker_processes auto;\n", "worker_processes auto;\nworker_rlimit_nofile 65535;\n", 1)

text = text.replace("worker_connections 768;", "worker_connections 4096;")
text = text.replace("\t# multi_accept on;", "\tmulti_accept on;")
text = text.replace("ssl_protocols TLSv1 TLSv1.1 TLSv1.2 TLSv1.3;", "ssl_protocols TLSv1.2 TLSv1.3;")

path.write_text(text)
PY

  cat > /etc/nginx/conf.d/quickdelivery-log-format.conf <<'EOF'
log_format quickdelivery_upstream
  '$remote_addr - $remote_user [$time_local] '
  '"$request" $status $body_bytes_sent '
  '"$http_referer" "$http_user_agent" '
  'rt=$request_time urt=$upstream_response_time '
  'uaddr=$upstream_addr ustatus=$upstream_status';
EOF

  cat > /etc/nginx/conf.d/quickdelivery-http-tuning.conf <<'EOF'
keepalive_timeout 65;
keepalive_requests 10000;
reset_timedout_connection on;
client_body_timeout 15s;
client_header_timeout 15s;
send_timeout 30s;
proxy_socket_keepalive on;
EOF

  cat > /etc/nginx/conf.d/quickdelivery-protection.conf <<EOF
limit_conn_zone \$binary_remote_addr zone=conn_per_ip:10m;
limit_conn_zone \$binary_remote_addr zone=ws_conn_per_ip:10m;
limit_req_zone \$binary_remote_addr zone=api_public_ip:10m rate=$VM1_API_PUBLIC_RATE;
limit_req_zone \$binary_remote_addr zone=auth_public_ip:10m rate=$VM1_AUTH_PUBLIC_RATE;
limit_req_zone \$binary_remote_addr zone=tracking_public_ip:10m rate=$VM1_TRACKING_PUBLIC_RATE;
EOF
}

render_ws_upstreams_conf() {
  local raw_backends="${PACKAGES_WS_BACKENDS:-}"
  local fallback_hostport
  local backend
  local has_backend=0

  mkdir -p "$(dirname "$NGINX_WS_UPSTREAMS_CONF")"
  rm -f /etc/nginx/conf.d/quickdelivery-ws-upstreams.conf
  : > "$NGINX_WS_UPSTREAMS_CONF"

  if [[ -n "$raw_backends" ]]; then
    IFS=',' read -r -a backend_list <<< "$raw_backends"
    for backend in "${backend_list[@]}"; do
      backend="$(printf '%s' "$backend" | xargs)"
      [[ -z "$backend" ]] && continue
      echo "server $backend max_fails=3 fail_timeout=10s;" >> "$NGINX_WS_UPSTREAMS_CONF"
      has_backend=1
    done
  fi

  if [[ "$has_backend" -eq 0 ]]; then
    fallback_hostport="$(url_to_hostport "${PACKAGES_WS_PINNED_URL:-ws://10.0.3.10:8082}")"
    echo "server $fallback_hostport max_fails=3 fail_timeout=10s;" >> "$NGINX_WS_UPSTREAMS_CONF"
  fi
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

  auth_backend_hostport="$(url_to_hostport "${KEYCLOAK_INTERNAL_URL:-http://10.0.2.10:18443/auth}")"
  gateway_internal_token_escaped="$(printf '%s' "${GATEWAY_INTERNAL_TOKEN:-}" | sed 's/[\\/&]/\\&/g')"

  configure_vm1_limits
  configure_vm1_nginx_base
  render_ws_upstreams_conf

  cp "$NGINX_TEMPLATE" "$NGINX_CONF"
  sed -i \
    -e "s/app.quickdelivery.tld/$APP_DOMAIN/g" \
    -e "s/api.quickdelivery.tld/$API_DOMAIN/g" \
    -e "s/auth.quickdelivery.tld/$AUTH_DOMAIN/g" \
    -e "s#10.0.2.10:18443#$auth_backend_hostport#g" \
    -e "s/static_cache_expires_placeholder/$VM1_STATIC_CACHE_EXPIRES/g" \
    -e "s/ws_limit_conn_per_ip_placeholder/$VM1_WS_LIMIT_CONN_PER_IP/g" \
    -e "s/limit_conn_per_ip_placeholder/$VM1_LIMIT_CONN_PER_IP/g" \
    -e "s/api_burst_placeholder/$VM1_API_PUBLIC_BURST/g" \
    -e "s/auth_burst_placeholder/$VM1_AUTH_PUBLIC_BURST/g" \
    -e "s/tracking_burst_placeholder/$VM1_TRACKING_PUBLIC_BURST/g" \
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
  if [[ -d "$PROJECT_ROOT/deploy/docker/5vm/certs" ]]; then
    chmod -R a+rX "$PROJECT_ROOT/deploy/docker/5vm/certs"
    find "$PROJECT_ROOT/deploy/docker/5vm/certs" -type f -exec chmod 644 {} \;
    find "$PROJECT_ROOT/deploy/docker/5vm/certs" -type d -exec chmod 755 {} \;
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
