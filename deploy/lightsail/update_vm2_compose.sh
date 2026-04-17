#!/bin/bash
# Script de mise à jour du mapping environment sur VM2 via réseau privé
# Ce script doit être exécuté sur VM3

KEY_PATH="/tmp/lightsail_internal.pem"
VM2_IP="172.26.4.12"
COMPOSE_FILE="/opt/quickdelivery/deploy/docker/5vm/docker-compose.vm2-platform.yml"

echo "Connexion à la VM2 ($VM2_IP) pour modification du Docker Compose..."

ssh -i "$KEY_PATH" -o StrictHostKeyChecking=no ubuntu@$VM2_IP << 'REMOTE_EOF'
    # Vérifier si le mapping existe déjà pour éviter les doublons
    if ! grep -q "KEYCLOAK_ADMIN_PASSWORD" /opt/quickdelivery/deploy/docker/5vm/docker-compose.vm2-platform.yml; then
        echo "Injection du mapping des variables KEYCLOAK_ADMIN..."
        sudo sed -i '/KEYCLOAK_SERVER_ADMIN_USER_PASSWORD: ${KEYCLOAK_SERVER_ADMIN_USER_PASSWORD}/a \      KEYCLOAK_ADMIN_USERNAME: ${KEYCLOAK_ADMIN_USERNAME}\n      KEYCLOAK_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD}' /opt/quickdelivery/deploy/docker/5vm/docker-compose.vm2-platform.yml
    else
        echo "Le mapping KEYCLOAK_ADMIN existe déjà."
    fi

    echo "Redéploiement forcé du serveur d'authentification..."
    cd /opt/quickdelivery/deploy/docker/5vm/
    sudo docker compose -f docker-compose.vm2-platform.yml up -d --force-recreate oauth-authorization-server
REMOTE_EOF

echo "Mise à jour et redéploiement terminés sur VM2."
