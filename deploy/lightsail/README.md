# QuickDelivery Lightsail Runbook

Ce dossier regroupe la mise en service de la VM `Amazon Lightsail` pour le MVP.

Il complete:

- [deploy/docker/README.md](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/docker/README.md)
- [deploy/nginx/README.md](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/nginx/README.md)

## 1. Fichiers

- `install-ubuntu.sh`
- `ROUTE53.md`
- `MVP-5VM-ARCHITECTURE.md`
- `vm1-gateway.env.example`
- `vm2-platform.env.example`
- `vm3-app.env.example`
- `vm4-app.env.example`
- `vm5-mysql.env.example`

## 1.1 Variante 5 VM Recommandee

Pour l'architecture MVP retenue:

- `VM1`: `front + nginx + gateway`
- `VM2`: `keycloak + config-server + discovery-server`
- `VM3`: `users-service + packages-service`
- `VM4`: `users-service + packages-service`
- `VM5`: `mysql`

utiliser en priorite:

- [MVP-5VM-ARCHITECTURE.md](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/MVP-5VM-ARCHITECTURE.md)

Le present README reste utile comme runbook general Lightsail, mais le mode `tout sur une seule VM` n'est plus la cible recommandee pour la prod MVP.

## 2. Sequence Recommandee

1. creer une VM `Lightsail Ubuntu`
2. reserver et attacher une IP statique
3. ouvrir les ports `22`, `80`, `443`
4. copier le depot sur la VM dans `/opt/quickdelivery`
5. lancer:

```bash
sudo bash /opt/quickdelivery/deploy/lightsail/install-ubuntu.sh
```

6. ajouter du swap si la VM a 2 GB RAM ou moins:

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h
```

7. preparer la stack Docker:

```bash
cd /opt/quickdelivery
cp deploy/docker/.env.production.sample deploy/docker/.env.production
nano deploy/docker/.env.production
docker compose --env-file deploy/docker/.env.production -f deploy/docker/docker-compose.production.yml build
docker compose --env-file deploy/docker/.env.production -f deploy/docker/docker-compose.production.yml up -d
```

8. verifier les services localement:

```bash
docker ps
curl -u configuser:'<mot_de_passe>' http://127.0.0.1:8889/actuator/health
curl -I http://127.0.0.1:8443/actuator/health
curl -I http://127.0.0.1:18443/auth
```

9. configurer le DNS avec:

- [ROUTE53.md](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/ROUTE53.md)

10. copier le build front dans `/var/www/quickdelivery-front`
11. installer la config `Nginx` HTTP de bootstrap
12. emettre le certificat `api/auth`
13. emettre le certificat `app`
14. installer la config `Nginx` finale
15. tester publiquement:
   - `https://app.quickdelivery.tld`
   - `https://api.quickdelivery.tld/actuator/health`
   - `https://auth.quickdelivery.tld/auth`

## 3. Variables Publiques A Fixer

Dans `deploy/docker/.env.production`:

- `APP_DOMAIN`
- `API_DOMAIN`
- `AUTH_DOMAIN`

Et dans la config applicative, les URLs derivees doivent correspondre a ces domaines.

## 4. Commandes TLS Validees

Une sequence validee est:

```bash
sudo certbot --nginx -d api.quickdelivery.tld -d auth.quickdelivery.tld
sudo certbot certonly --webroot -w /var/www/certbot -d app.quickdelivery.tld
sudo nginx -t
sudo systemctl reload nginx
```

La conf finale `Nginx` ne doit etre activee qu'apres emission de ces certificats.

## 5. Checklist Finale Backend
- `https://api.quickdelivery.tld/actuator/health` repond
- Keycloak repond sur `https://auth.quickdelivery.tld/auth`
- les emails utilisent les domaines publics
- les QR codes utilisent les domaines publics
- aucun lien ne contient `localhost`
- aucun lien ne contient une IP LAN

## 6. Checklist Finale Front
- `https://app.quickdelivery.tld` repond
- les assets JS/CSS se chargent sans erreur `404`
- la SPA recharge correctement sur une route interne
- le front appelle `https://api.quickdelivery.tld`
- le front utilise `https://auth.quickdelivery.tld/auth`

## 7. Copie Minimale Vers La VM

Pour cette strategie orientee `jar`, il n'est pas necessaire de copier tout le depot sur la VM.

Copier au minimum:

- `artifacts/`
- `config/`
- `deploy/`

Les `jar` doivent d'abord etre prepares localement avec:

```powershell
powershell -ExecutionPolicy Bypass -File deploy/docker/prepare-artifacts.ps1
```

Pour le front, copier aussi le build de:

- `quickdelivery-googlemaps-front/dist/`

## 8. Point Important Valide En Production

La variante retenue n'utilise pas `Nginx` en conteneur.

Le deploiement backend valide est:

- services metier en Docker
- `Nginx` installe sur l'hote Ubuntu
- ports locaux exposes seulement sur l'hote:
  - `127.0.0.1:8889`
  - `127.0.0.1:8443`
  - `127.0.0.1:18443`

## 9. Automatisation Du Redeploiement

Une automatisation minimale du redeploiement front + back est disponible dans:

- [deploy/release/README.md](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/release/README.md)
- [deploy/release/package-and-upload.ps1](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/release/package-and-upload.ps1)
- [deploy/release/release-on-vm.sh](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/release/release-on-vm.sh)
- [deploy/release/verify-release.sh](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/release/verify-release.sh)
