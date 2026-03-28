# QuickDelivery Lightsail MVP 5 VM Architecture

Cette cible couvre l'architecture MVP retenue pour un premier lancement public avec separation du front, de l'auth, des services metier et de MySQL.

## 1. Topologie

- `VM1`: `front + nginx + api-gateway`
- `VM2`: `keycloak + config-server + discovery-server`
- `VM3`: `users-service + packages-service`
- `VM4`: `users-service + packages-service`
- `VM5`: `mysql`
- `S3`: stockage des documents et assets metier persistants

## 2. Sizing Recommande

- `VM1`: `2 vCPU / 4 GB RAM`
- `VM2`: `2 vCPU / 4 GB RAM`
- `VM3`: `2 vCPU / 4 GB RAM` minimum, `8 GB` prefere
- `VM4`: `2 vCPU / 4 GB RAM` minimum, `8 GB` prefere
- `VM5`: `2 vCPU / 4 GB RAM` minimum, `4 vCPU / 8 GB RAM` prefere

## 3. Roles Par VM

### VM1

- sert le front statique via `nginx`
- termine TLS pour `app`, `api`, `auth`
- reverse proxy public
- balance le trafic HTTP vers `VM3` et `VM4`
- route le WebSocket tracking vers `VM3` uniquement
- heberge `api-gateway`

### VM2

- heberge `keycloak`
- heberge `config-server`
- heberge `discovery-server`
- ne doit etre joignable publiquement que sur `auth`

### VM3

- heberge `users-service`
- heberge `packages-service`
- sert de cible unique pour le tracking WebSocket

### VM4

- heberge `users-service`
- heberge `packages-service`
- absorbe la charge HTTP metier en parallele de `VM3`

### VM5

- heberge `mysql`
- n'est accessible qu'en reseau prive depuis `VM2`, `VM3`, `VM4`

## 4. Flux Reseau

### Flux public

- `https://app.quickdelivery.tld` -> `VM1 nginx` -> front statique
- `https://api.quickdelivery.tld` -> `VM1 nginx` -> `api-gateway` local
- `https://auth.quickdelivery.tld` -> `VM1 nginx` -> `VM2 keycloak`
- `wss://api.quickdelivery.tld/ws` -> `VM1 nginx` -> `VM3 packages-service`

### Flux prives inter-VM

- `VM1 api-gateway` -> `VM2 config-server`
- `VM1 api-gateway` -> `VM2 discovery-server`
- `VM1 api-gateway` -> `VM2 keycloak`
- `VM1 api-gateway` -> `VM3 users-service`
- `VM1 api-gateway` -> `VM4 users-service`
- `VM1 api-gateway` -> `VM3 packages-service`
- `VM1 api-gateway` -> `VM4 packages-service`
- `VM2 keycloak` -> `VM5 mysql`
- `VM3 users-service` -> `VM5 mysql`
- `VM3 packages-service` -> `VM5 mysql`
- `VM4 users-service` -> `VM5 mysql`
- `VM4 packages-service` -> `VM5 mysql`
- `VM3 users-service` -> `VM2 config-server + discovery-server`
- `VM3 packages-service` -> `VM2 config-server + discovery-server`
- `VM4 users-service` -> `VM2 config-server + discovery-server`
- `VM4 packages-service` -> `VM2 config-server + discovery-server`

## 5. DNS Recommande

- `app.quickdelivery.tld` -> IP publique `VM1`
- `api.quickdelivery.tld` -> IP publique `VM1`
- `auth.quickdelivery.tld` -> IP publique `VM1`

Les services `config`, `discovery`, `users`, `packages`, `mysql` doivent utiliser des IP privees ou DNS internes Lightsail.

## 6. Repartition De Charge

### HTTP classique

- `nginx` publie l'API publique vers `api-gateway` sur `VM1`
- `api-gateway` consomme `discovery-server` sur `VM2`
- `api-gateway` route ensuite `users-service` et `packages-service` vers `VM3` et `VM4`
- l'equilibrage HTTP metier est donc pilote par la gateway et `discovery-server`, pas par `nginx`

### Tracking temps reel

- le WebSocket est route vers `VM3` uniquement
- les requetes HTTP de tracking peuvent rester balancees sur `VM3` et `VM4`
- si un scale horizontal du tracking devient necessaire, il faudra ajouter un mecanisme partage de messages, par exemple `Redis pub/sub`

## 7. Impact Applicatif

- toutes les URLs internes doivent etre externalisees
- les documents ne doivent plus etre conserves sur disque local comme source de verite
- `S3` doit devenir la cible des documents metier
- le front ne doit parler qu'aux domaines publics
- le tracking doit utiliser `wss://api.quickdelivery.tld/ws`

## 8. Ports Recommandes

### VM1

- `22`
- `80`
- `443`
- ports applicatifs internes exposes seulement en `127.0.0.1`

### VM2

- `22`
- `8080` prive pour `discovery-server`
- `8889` prive pour `config-server`
- port `keycloak` prive derriere `VM1 nginx`

### VM3 / VM4

- `22`
- `8081` prive pour `users-service`
- `8082` prive pour `packages-service`

### VM5

- `22`
- `3306` prive uniquement

## 9. Variables D'Environnement Par VM

Les exemples prets a adapter sont fournis dans ce dossier:

- [vm1-gateway.env.example](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/vm1-gateway.env.example)
- [vm2-platform.env.example](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/vm2-platform.env.example)
- [vm3-app.env.example](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/vm3-app.env.example)
- [vm4-app.env.example](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/vm4-app.env.example)
- [vm5-mysql.env.example](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/vm5-mysql.env.example)

## 10. Configuration Nginx

Le template cible pour `VM1` est:

- [quickdelivery.5vm.conf.template](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/nginx/quickdelivery.5vm.conf.template)

Ce template:

- sert le front depuis `VM1`
- reverse proxy `api-gateway`
- route `auth` vers `VM2`
- epingle `/ws` sur `VM3`

## 11. Ordre De Deploiement

1. preparer `VM5 mysql`
2. preparer `VM2 keycloak + config-server + discovery-server`
3. preparer `VM3 users + packages`
4. preparer `VM4 users + packages`
5. preparer `VM1 front + nginx + gateway`
6. verifier `config-server`, `discovery-server`, `mysql`
7. verifier `keycloak`
8. verifier `users-service` et `packages-service` sur `VM3` et `VM4`
9. verifier `gateway`
10. verifier le tracking WebSocket sur `VM3`

## 12. Checklist Avant Go Live

- aucun service ne pointe sur `localhost` hors boucle locale de sa propre VM
- `VM5 mysql` n'est pas expose publiquement
- les certificats TLS sont poses sur `VM1`
- `keycloak` utilise ses domaines publics corrects
- les uploads documents sont externalises vers `S3`
- les healthchecks des 5 VMs sont testes
- les campagnes `smoke` et `nominal` sont rejouees apres migration
