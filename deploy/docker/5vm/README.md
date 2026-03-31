# QuickDelivery Docker 5 VM

Ce dossier contient la base de deploiement Docker pour la cible Lightsail 5 VM.

## 1. Fichiers

- `docker-compose.vm1-gateway.yml`
- `docker-compose.vm2-platform.yml`
- `docker-compose.vm3-app.yml`
- `docker-compose.vm4-app.yml`
- `docker-compose.vm5-mysql.yml`

## 2. Principe

Chaque VM porte son propre `docker compose`.

- `VM1`: `api-gateway`
- `VM2`: `keycloak + config-server + discovery-server + redis`
- `VM3`: `users-service + packages-service`
- `VM4`: `users-service + packages-service`
- `VM5`: `mysql`

`nginx` reste installe sur l'hote Ubuntu de `VM1`, pas en conteneur.

## 3. Variables D'Environnement

Utiliser les fichiers exemples du dossier Lightsail:

- [vm1-gateway.env.example](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/vm1-gateway.env.example)
- [vm2-platform.env.example](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/vm2-platform.env.example)
- [vm3-app.env.example](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/vm3-app.env.example)
- [vm4-app.env.example](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/vm4-app.env.example)
- [vm5-mysql.env.example](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/vm5-mysql.env.example)

## 4. Equilibrage Cible

La gateway route desormais:

- `users` via `lb://users-service`
- `packages` via `lb://package-service`

Donc l'equilibrage HTTP metier passe par Eureka entre `VM3` et `VM4`.

Le tracking WebSocket reste epingle sur `VM3` pour le point d'entree `/ws`, mais les updates de position peuvent maintenant etre repropages entre `VM3` et `VM4` via Redis quand `QUICKDELIVERY_TRACKING_REDIS_ENABLED=true`.

Les services `users` et `packages` exposent aussi des caches TTL courts pour:

- dashboards admin
- validation de comptes
- lecture des packages par statut
- cartes packages autour de moi

Ces compose constituent le socle de migration a valider en recette privee avant bascule publique.
