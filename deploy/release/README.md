# QuickDelivery Release Automation

Ce dossier contient l'automatisation minimale pour redeployer front et back sur la VM Lightsail deja preparee.

## Fichiers

- `package-and-upload.ps1`
- `package-and-upload-5vm.ps1`
- `update-deploy.ps1`
- `update-deploy-5vm.ps1`
- `release-on-vm.sh`
- `release-on-vm-5vm.sh`
- `verify-release.sh`
- `verify-release-5vm.sh`

## Flux retenu

1. depuis le PC:
   - generer les `jar`
   - builder le front
   - uploader `artifacts/`, `config/`, `deploy/` et `dist/`
2. sur la VM:
   - verifier/creer le swap
   - redemarrer le backend Docker
   - installer le front statique
   - appliquer le bootstrap `Nginx`
   - emettre les certificats si absents
   - appliquer la conf finale `Nginx`
   - verifier `front`, `api`, `auth`

## Usage

### 1. Depuis le PC

```powershell
powershell -ExecutionPolicy Bypass -File deploy/release/package-and-upload.ps1
```

Options utiles:

```powershell
powershell -ExecutionPolicy Bypass -File deploy/release/package-and-upload.ps1 -ServerIp 15.224.36.99 -SshKeyPath C:\Users\imess\.ssh\lightsail.pem
```

Si `deploy/docker/.env.production` existe localement, il est aussi copie sur la VM.

### 2. Sur la VM

```bash
sudo bash /opt/quickdelivery/deploy/release/release-on-vm.sh
```

### 3. Mise A Jour Automatisee

Redeploiement complet:

```powershell
powershell -ExecutionPolicy Bypass -File deploy/release/update-deploy.ps1 -Mode full
```

Mise a jour backend seulement, apres evolution des `jar`/artefacts:

```powershell
powershell -ExecutionPolicy Bypass -File deploy/release/update-deploy.ps1 -Mode backend
```

Mise a jour front seulement:

```powershell
powershell -ExecutionPolicy Bypass -File deploy/release/update-deploy.ps1 -Mode frontend
```

Mise a jour par module backend:

```powershell
powershell -ExecutionPolicy Bypass -File deploy/release/update-deploy.ps1 -Mode module -Modules users
```

```powershell
powershell -ExecutionPolicy Bypass -File deploy/release/update-deploy.ps1 -Mode module -Modules packages
```

```powershell
powershell -ExecutionPolicy Bypass -File deploy/release/update-deploy.ps1 -Mode module -Modules users,api-gateway
```

### 4. Verification seule

```bash
sudo CONFIG_PASSWORD='<mot_de_passe_config>' bash /opt/quickdelivery/deploy/release/verify-release.sh
```

## Preconditions

- la VM Lightsail existe deja
- `Docker`, `Compose`, `Nginx` et `Certbot` sont installes
- les DNS `app`, `api`, `auth` pointent deja vers la VM
- `deploy/docker/.env.production` est present sur la VM ou localement avant upload

## Limites

- ces scripts n'initialisent pas Lightsail ni Route 53
- ils supposent l'architecture validee:
  - backend en Docker
  - `Nginx` sur l'hote Ubuntu
  - front statique dans `/var/www/quickdelivery-front`

## Modes De Release

- `full`: front + back + `Nginx` + certificats si absents
- `backend`: mise a jour des artefacts backend et redemarrage Docker seulement
- `module`: mise a jour d'un ou plusieurs modules backend seulement
- `frontend`: mise a jour du build front et rechargement `Nginx` seulement

## Modules Backend Supportes

Valeurs supportees pour `-Modules`:

- `config-server`
- `discovery-server`
- `oauth`
- `api-gateway`
- `users`
- `packages`

Tu peux aussi utiliser le nom du `jar` correspondant:

- `quickdelivery-config-server.jar`
- `quickdelivery-registry-server.jar`
- `oauth-authorization-server.jar`
- `quickdelivery-api-gateway.jar`
- `quickdelivery-users.jar`
- `quickdelivery-packages.jar`

## Variante 5 VM

Pour l'architecture:

- `VM1`: `front + nginx + gateway`
- `VM2`: `keycloak + config-server + discovery-server`
- `VM3`: `users + packages`
- `VM4`: `users + packages`
- `VM5`: `mysql`

utiliser les scripts `5vm` dedies.

Exemples:

```powershell
powershell -ExecutionPolicy Bypass -File deploy/release/update-deploy-5vm.ps1 -Role vm5-mysql -ServerIp 10.0.5.10 -Mode backend
```

```powershell
powershell -ExecutionPolicy Bypass -File deploy/release/update-deploy-5vm.ps1 -Role vm2-platform -ServerIp 10.0.2.10 -Mode backend
```

```powershell
powershell -ExecutionPolicy Bypass -File deploy/release/update-deploy-5vm.ps1 -Role vm3-app -ServerIp 10.0.3.10 -Mode backend
```

```powershell
powershell -ExecutionPolicy Bypass -File deploy/release/update-deploy-5vm.ps1 -Role vm1-gateway -ServerIp 10.0.1.10 -Mode full
```

Chaque role lit par defaut son fichier:

- `deploy/docker/5vm/.env.<role>` sur la VM

et le met a jour depuis:

- `deploy/lightsail/<role>.env` localement si ce fichier existe.

## Durcissement VM1

Le role `vm1-gateway` supporte aussi des variables de durcissement `Nginx`:

- `VM1_LIMIT_CONN_PER_IP`
- `VM1_WS_LIMIT_CONN_PER_IP`
- `VM1_API_PUBLIC_RATE`
- `VM1_API_PUBLIC_BURST`
- `VM1_AUTH_PUBLIC_RATE`
- `VM1_AUTH_PUBLIC_BURST`
- `VM1_TRACKING_PUBLIC_RATE`
- `VM1_TRACKING_PUBLIC_BURST`
- `VM1_STATIC_CACHE_EXPIRES`

Ces variables se definissent dans:

- `deploy/lightsail/vm1-gateway.env`

Une checklist infra AWS/Lightsail associee est disponible dans:

- [docs/aws-5vm-hardening.md](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/docs/aws-5vm-hardening.md)
