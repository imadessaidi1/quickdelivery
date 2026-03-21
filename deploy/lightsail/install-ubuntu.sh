#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run this script as root: sudo bash deploy/lightsail/install-ubuntu.sh"
  exit 1
fi

export DEBIAN_FRONTEND=noninteractive

apt-get update
apt-get install -y \
  ca-certificates \
  curl \
  gnupg \
  lsb-release \
  unzip \
  git \
  nginx \
  certbot \
  python3-certbot-nginx

install -m 0755 -d /etc/apt/keyrings
if [[ ! -f /etc/apt/keyrings/docker.asc ]]; then
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
fi

ARCH="$(dpkg --print-architecture)"
CODENAME="$(
  . /etc/os-release
  echo "${VERSION_CODENAME}"
)"

cat >/etc/apt/sources.list.d/docker.list <<EOF
deb [arch=${ARCH} signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu ${CODENAME} stable
EOF

apt-get update
apt-get install -y \
  docker-ce \
  docker-ce-cli \
  containerd.io \
  docker-buildx-plugin \
  docker-compose-plugin

systemctl enable docker
systemctl start docker
systemctl enable nginx
systemctl start nginx

if ! id -u ubuntu >/dev/null 2>&1; then
  echo "User ubuntu not found. Skipping docker group assignment."
else
  usermod -aG docker ubuntu || true
fi

mkdir -p /opt/quickdelivery
mkdir -p /var/www/certbot

cat <<'EOF'
Base packages installed:
- Docker Engine
- Docker Compose plugin
- Nginx
- Certbot

Next steps:
1. copy the repository to /opt/quickdelivery
2. copy deploy/docker/.env.example to deploy/docker/.env and fill it
3. run:
   docker compose --env-file deploy/docker/.env -f deploy/docker/docker-compose.lightsail.yml build
   docker compose --env-file deploy/docker/.env -f deploy/docker/docker-compose.lightsail.yml up -d
4. install the Nginx config from deploy/nginx
5. issue Let's Encrypt certificates
EOF
