# QuickDelivery Migration Runbook

Ce runbook couvre la migration maitrisee depuis l'architecture actuelle vers l'architecture Lightsail 5 VM retenue.

Architecture cible:

- `VM1`: `front + nginx + api-gateway`
- `VM2`: `keycloak + config-server + discovery-server`
- `VM3`: `users-service + packages-service`
- `VM4`: `users-service + packages-service`
- `VM5`: `mysql`

## 1. Regles De Migration

- ne jamais couper la prod actuelle avant validation complete de la nouvelle pile
- preparer la nouvelle infra en parallele
- ne basculer le trafic public qu'apres recette privee complete
- garder un rollback DNS et base de donnees simple
- reduire le TTL DNS avant la bascule

## 2. Decoupage Des Phases

### Phase A

Preparation:

1. reduire le TTL DNS a `60s`
2. inventorier tous les secrets
3. preparer les 5 VMs et leurs IP privees
4. preparer `S3`
5. preparer le dump initial de base

### Phase B

Infrastructure privee:

1. deployer `VM5 mysql`
2. deployer `VM2 keycloak + config + discovery`
3. deployer `VM3 users + packages`
4. deployer `VM4 users + packages`
5. deployer `VM1 gateway + front + nginx`

### Phase C

Validation privee:

1. tests healthchecks
2. tests login
3. tests create package
4. tests payment guest
5. tests tracking
6. tests dashboards
7. tests `smoke`
8. tests `nominal`

### Phase D

Bascule:

1. gel court des ecritures
2. sauvegarde finale de la prod actuelle
3. delta final DB
4. verification finale de la nouvelle infra
5. bascule DNS vers `VM1`
6. surveillance renforcee

## 3. Checklist J-7

- [ ] TTL DNS abaisse
- [ ] taille des VMs validee
- [ ] IP privees reservees
- [ ] acces SSH testes
- [ ] certificats/TLS prepares
- [ ] bucket `S3` cree
- [ ] secrets centralises
- [ ] dump base initial disponible

## 4. Checklist J-3

- [ ] `VM5` installee
- [ ] MySQL operationnel
- [ ] restauration du dump effectuee
- [ ] tests de connexion DB depuis `VM2`, `VM3`, `VM4`
- [ ] firewall prive DB applique

## 5. Checklist J-2

- [ ] `VM2` deployee
- [ ] `config-server` `UP`
- [ ] `discovery-server` `UP`
- [ ] `keycloak` `UP`
- [ ] realm importe
- [ ] clients et redirect URIs verifies
- [ ] login admin et livreur verifies

## 6. Checklist J-1

- [ ] `VM3` deployee
- [ ] `VM4` deployee
- [ ] `users-service` et `packages-service` visibles dans Eureka
- [ ] `VM1` deployee
- [ ] `nginx` en place
- [ ] domaine de recette prive teste
- [ ] documents `S3` testes
- [ ] campagne `smoke` passee

## 7. Checklist H-2

- [ ] pas de changement applicatif en cours
- [ ] backup complet de la prod actuelle
- [ ] logs et monitoring ouverts
- [ ] DNS prets
- [ ] rollback DNS documente
- [ ] checklist de verification distribuee

## 8. Checklist H-0

- [ ] activer le gel d'ecritures si necessaire
- [ ] prendre le dump final
- [ ] injecter le delta DB sur `VM5`
- [ ] verifier healthchecks des 5 VMs
- [ ] verifier login admin
- [ ] verifier create package
- [ ] verifier guest payment
- [ ] verifier tracking live
- [ ] basculer `app`, `api`, `auth` vers `VM1`
- [ ] surveiller pendant `30 min` sans redeployer

## 9. Verification Post-Bascule

- [ ] `https://app...` repond
- [ ] `https://api.../actuator/health` repond
- [ ] `https://auth.../auth` repond
- [ ] email validation fonctionne
- [ ] tracking websocket fonctionne
- [ ] creation guest fonctionne
- [ ] paiement guest fonctionne
- [ ] dashboard finance fonctionne
- [ ] aucune erreur `5xx` anormale

## 10. Point De Vigilance Important

`api-gateway` route maintenant `users` et `packages` via `lb://users-service` et `lb://package-service`.

Cela signifie:

1. `discovery-server` doit toujours voir `VM3` et `VM4` comme `UP`
2. les deux services doivent bien s'enregistrer sous `users-service` et `package-service`
3. le WebSocket tracking reste volontairement epingle sur `VM3` pour le MVP

## 11. Fichiers Associes

- [MVP-5VM-ARCHITECTURE.md](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/MVP-5VM-ARCHITECTURE.md)
- [ROLLBACK-PLAN.md](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/ROLLBACK-PLAN.md)
- [deploy/docker/5vm/README.md](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/docker/5vm/README.md)
