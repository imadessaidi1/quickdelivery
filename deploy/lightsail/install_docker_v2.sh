#!/bin/bash
# Script pour installer Docker Compose V2 sur Ubuntu
set -e

echo "Mise à jour des dépôts..."
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg

echo "Configuration du trousseau de clés Docker..."
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg --yes
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo "Ajout du dépôt Docker..."
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

echo "Installation du plugin Docker Compose V2..."
sudo apt-get update
sudo apt-get install -y docker-compose-plugin

echo "Vérification de l'installation..."
docker compose version
