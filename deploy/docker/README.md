# QuickDelivery Docker MVP

Cette stack Docker couvre le backend MVP pour une VM `Amazon Lightsail`.

Le front n'est pas inclus ici, car l'option AWS retenue pour le MVP est:

- front Vue sur `Nginx` hote Lightsail
- backend complet sur une seule VM

## 1. Fichiers

- `docker-compose.lightsail.yml`
- `docker-compose.production.yml`
- `Dockerfile.java-service`
- `Dockerfile.runtime-jar`
- `.env.example`
- `.env.production.template`
- `.env.production.sample`
- `prepare-artifacts.ps1`
- `mysql/init/01-init-databases.sh`

## 2. Preparation

Depuis `deploy/docker`:

```powershell
Copy-Item .env.example .env
```

Pour une vraie configuration de prod, partir plutot de:

- [deploy/docker/.env.production.template](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/docker/.env.production.template)
- [deploy/docker/.env.production.sample](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/docker/.env.production.sample)

Puis adapter au minimum:

- `APP_DOMAIN`
- `API_DOMAIN`
- `AUTH_DOMAIN`
- `MYSQL_ROOT_PASSWORD`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `GOOGLE_MAPS_KEY`
- `MAPQUEST_KEY`

## 3. Demarrage

Depuis la racine du projet:

```powershell
docker compose --env-file deploy/docker/.env -f deploy/docker/docker-compose.lightsail.yml build
docker compose --env-file deploy/docker/.env -f deploy/docker/docker-compose.lightsail.yml up -d
```

Mode production reel valide sur Lightsail, avec `Nginx` sur l'hote Ubuntu et publication locale seulement de `config-server`, `gateway` et `auth`:

```powershell
powershell -ExecutionPolicy Bypass -File deploy/docker/prepare-artifacts.ps1
docker compose --env-file deploy/docker/.env.production -f deploy/docker/docker-compose.production.yml build
docker compose --env-file deploy/docker/.env.production -f deploy/docker/docker-compose.production.yml up -d
```

Important:

- `oauth-authorization-server`, `Users_Service`, `Package_Service`, `DISCOVERY-SERVER` et `APIGatewayApplication` doivent avoir `SPRING_APPLICATION_NAME` force dans `docker-compose.production.yml`
- sans cela, les services peuvent ne charger que `application/default` depuis le Config Server au lieu de leur fichier nomme
- symptome typique: placeholder introuvable au demarrage, par exemple `quickdelivery.gateway.internal-token`

## 4. Services exposes

- config server local hote: `http://127.0.0.1:8889`
- discovery server: interne Docker uniquement
- oauth server local hote: `http://127.0.0.1:18443`
- api gateway local hote: `http://127.0.0.1:8443`

## 5. Important

Cette stack prepare uniquement les conteneurs backend.

L'exposition publique se fait ensuite via `Nginx` installe sur l'hote Ubuntu:

- `api.quickdelivery.fr` -> `http://127.0.0.1:8443`
- `auth.quickdelivery.fr` -> `http://127.0.0.1:18443`
- `app.quickdelivery.fr` -> build front statique servi par `Nginx`

Cette variante est faite pour un deploiement a partir des `jar` presents dans `artifacts/`.

## 6. Strategie Artefacts Pour La VM

Pour un premier deploiement AWS, la strategie recommandee est:

1. builder les `jar` localement
2. remplir `artifacts/`
3. copier sur la VM seulement:
   - `artifacts/`
   - `config/`
   - `deploy/`
4. lancer `docker-compose.production.yml` sur la VM

Preparation locale:

```powershell
powershell -ExecutionPolicy Bypass -File deploy/docker/prepare-artifacts.ps1
```

Les `jar` attendus sont documentes dans:

- [artifacts/README.md](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/artifacts/README.md)

## 7. Sequence Reelle Validee Sur Lightsail

1. generer les `jar` localement
2. copier sur la VM:
   - `artifacts/`
   - `config/`
   - `deploy/`
3. ajouter du swap sur la VM si elle n'en a pas
4. remplir `deploy/docker/.env.production`
5. lancer:

```bash
docker compose --env-file deploy/docker/.env.production -f deploy/docker/docker-compose.production.yml up -d
```

6. verifier localement:

```bash
curl -u configuser:'<mot_de_passe>' http://127.0.0.1:8889/actuator/health
curl -I http://127.0.0.1:8443/actuator/health
curl -I http://127.0.0.1:18443/auth
```

7. publier ensuite avec `Nginx` hote
