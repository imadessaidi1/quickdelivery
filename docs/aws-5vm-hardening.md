# AWS 5 VM Hardening Plan

## Ce Que Le Depot Gere

Les scripts et la configuration du depot gerent maintenant les points suivants sur `VM1`:

- limitations de debit `nginx` par IP sur `auth`, API publique et tracking
- limitation du nombre de connexions simultanees par IP
- cache statique plus agressif pour le front
- timeouts HTTP plus stricts
- upstream WebSocket actif/actif sur `VM3` et `VM4`
- logs `nginx` enrichis avec latence et upstream

Fichiers principaux:

- [deploy/nginx/quickdelivery.5vm.conf.template](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/nginx/quickdelivery.5vm.conf.template)
- [deploy/release/release-on-vm-5vm.sh](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/release/release-on-vm-5vm.sh)
- [deploy/lightsail/vm1-gateway.env.example](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/vm1-gateway.env.example)

## Actions Infra A Faire Cote AWS

### 1. Exposition Publique

Garder publics uniquement:

- `VM1`: `80`, `443`

Fermer au public:

- `VM2`: `8080`, `8889`, `18443`, `6379`
- `VM3`: `8081`, `8082`
- `VM4`: `8081`, `8082`
- `VM5`: `3306`

SSH:

- autoriser `22` uniquement depuis les IP d'administration
- retirer `0.0.0.0/0` sur SSH si present

### 2. Flux Prives A Autoriser

- `VM1 -> VM2`: auth, config, discovery
- `VM1 -> VM3` et `VM1 -> VM4`: `8081`, `8082`
- `VM3 -> VM5` et `VM4 -> VM5`: `3306`
- `VM3 -> VM2` et `VM4 -> VM2`: config, discovery, auth, redis si necessaire

### 3. DNS

Les noms publics doivent pointer vers `VM1` seulement:

- `app.quickdelivery.fr`
- `api.quickdelivery.fr`
- `auth.quickdelivery.fr`

Ne jamais exposer directement `VM3`, `VM4` ou `VM5` dans le DNS public.

### 4. Sauvegardes

Mettre en place:

- snapshot regulier de `VM5`
- dump MySQL quotidien
- conservation hors VM si possible
- test de restauration au moins une fois

### 5. Acces Admin

- desactiver les mots de passe SSH si possible
- garder uniquement les cles SSH
- restreindre les comptes sudo
- journaliser les acces admin

## Variables VM1 A Ajuster Si Necessaire

Fichier:

- [deploy/lightsail/vm1-gateway.env](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/vm1-gateway.env)

Variables utiles:

- `VM1_LIMIT_CONN_PER_IP`
- `VM1_WS_LIMIT_CONN_PER_IP`
- `VM1_API_PUBLIC_RATE`
- `VM1_API_PUBLIC_BURST`
- `VM1_AUTH_PUBLIC_RATE`
- `VM1_AUTH_PUBLIC_BURST`
- `VM1_TRACKING_PUBLIC_RATE`
- `VM1_TRACKING_PUBLIC_BURST`
- `VM1_STATIC_CACHE_EXPIRES`

## Ordre De Mise En Production

1. appliquer les regles firewall AWS/Lightsail
2. verifier la connectivite privee entre les 5 VM
3. redeployer `vm1-gateway`
4. verifier `front`, `api`, `auth`, `/ws`
5. relancer les smoke tests
6. relancer les tests de charge realistes
