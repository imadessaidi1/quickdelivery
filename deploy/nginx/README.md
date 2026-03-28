# QuickDelivery Nginx Lightsail

Ce dossier contient la base `Nginx` pour exposer publiquement QuickDelivery sur une VM `Amazon Lightsail`.

Le role de `Nginx` ici est de publier:

- `app.quickdelivery.tld`
- `api.quickdelivery.tld`
- `auth.quickdelivery.tld`

avec:

- front statique servi depuis le disque de la VM
- reverse proxy vers les conteneurs Docker locaux pour `api` et `auth`

## 1. Cible

Flux public:

- `https://app.quickdelivery.tld` -> `Nginx` -> fichiers statiques front
- `https://api.quickdelivery.tld` -> `Nginx` -> `quickdelivery-api-gateway`
- `https://auth.quickdelivery.tld` -> `Nginx` -> `oauth-authorization-server`

Flux interne sur la VM:

- `127.0.0.1:8443` -> `quickdelivery-api-gateway`
- `127.0.0.1:18443` -> `oauth-authorization-server`

## 1.1 Variante 5 VM

Pour l'architecture MVP retenue sur 5 VMs, utiliser le template:

- [quickdelivery.5vm.conf.template](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/nginx/quickdelivery.5vm.conf.template)

Cette variante:

- sert le front sur `VM1`
- route `auth` vers `VM2`
- laisse la gateway de `VM1` router le HTTP metier vers `VM3` et `VM4` via `discovery-server`
- epingle `/ws` vers `VM3` pour le tracking temps reel

## 2. Fichiers fournis

- `quickdelivery.conf.template`
- `quickdelivery.docker.conf.template`

Le fichier `quickdelivery.conf.template` est un point de depart, mais la procedure reelle validee sur Lightsail est:

1. bootstrap HTTP simple sur `80`
2. emission des certificats `api/auth`
3. emission du certificat `app`
4. mise en place de la conf finale TLS/reverse proxy

La conf finale doit servir:

- `app.quickdelivery.tld` depuis un dossier statique
- `api.quickdelivery.tld` vers `http://127.0.0.1:8443`
- `auth.quickdelivery.tld` vers `http://127.0.0.1:18443`
- `WebSocket` sur `/ws/`
- le certificat `api.quickdelivery.tld` peut aussi couvrir `auth.quickdelivery.tld` si l'emission a ete faite avec les 2 domaines

## 3. Installation Sur Lightsail

### 3.1 Installer Nginx et Certbot

Ubuntu:

```bash
sudo apt update
sudo apt install -y nginx certbot python3-certbot-nginx
```

### 3.2 Bootstrap HTTP Pour Let's Encrypt

Creer d'abord une conf minimale HTTP sans SSL:

```bash
/etc/nginx/sites-available/quickdelivery.conf
```

Contenu de bootstrap valide:

```bash
server {
    listen 80;
    listen [::]:80;
    server_name app.quickdelivery.tld api.quickdelivery.tld auth.quickdelivery.tld;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 200 "quickdelivery nginx bootstrap ok\n";
        add_header Content-Type text/plain;
    }
}
```

Puis:

```bash
sudo mkdir -p /var/www/certbot
sudo ln -sf /etc/nginx/sites-available/quickdelivery.conf /etc/nginx/sites-enabled/quickdelivery.conf
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl restart nginx
```

## 4. Strategie TLS Let's Encrypt

### 4.1 Preconditions

Les DNS doivent deja pointer vers la VM Lightsail:

- `app.quickdelivery.tld`
- `api.quickdelivery.tld`
- `auth.quickdelivery.tld`

Et les ports Lightsail doivent etre ouverts:

- `80`
- `443`

### 4.2 Emission des certificats

Sequence validee:

```bash
sudo certbot --nginx -d api.quickdelivery.tld -d auth.quickdelivery.tld
sudo certbot certonly --webroot -w /var/www/certbot -d app.quickdelivery.tld
```

Verifier ensuite:

```bash
sudo certbot certificates
```

### 4.3 Configuration Finale TLS Et Reverse Proxy

Une fois les certificats emis, mettre une conf finale du type:

```bash
map $http_upgrade $connection_upgrade {
    default upgrade;
    '' close;
}

server {
    listen 80;
    listen [::]:80;
    server_name api.quickdelivery.tld auth.quickdelivery.tld app.quickdelivery.tld;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name app.quickdelivery.tld;

    ssl_certificate /etc/letsencrypt/live/app.quickdelivery.tld/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/app.quickdelivery.tld/privkey.pem;

    root /var/www/quickdelivery-front;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name api.quickdelivery.tld;

    ssl_certificate /etc/letsencrypt/live/api.quickdelivery.tld/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.quickdelivery.tld/privkey.pem;

    location /auth/ {
        proxy_pass http://127.0.0.1:18443/auth/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }

    location / {
        proxy_pass http://127.0.0.1:8443;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }

    location /ws {
        proxy_pass http://127.0.0.1:8443;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto https;
        proxy_read_timeout 86400s;
        proxy_send_timeout 86400s;
    }
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name auth.quickdelivery.tld;

    ssl_certificate /etc/letsencrypt/live/api.quickdelivery.tld/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.quickdelivery.tld/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:18443;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}
```

Puis:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

### 4.4 Verification

Verifier:

```bash
sudo systemctl status nginx
sudo certbot certificates
curl -k -I --resolve app.quickdelivery.tld:443:127.0.0.1 https://app.quickdelivery.tld/
curl -k -I --resolve api.quickdelivery.tld:443:127.0.0.1 https://api.quickdelivery.tld/actuator/health
curl -k -I --resolve auth.quickdelivery.tld:443:127.0.0.1 https://auth.quickdelivery.tld/auth
```

## 5. Ordre De Mise En Place Recommande

1. creer la VM `Lightsail`
2. ouvrir les ports:
   - `22`
   - `80`
   - `443`
3. installer `Docker`, `Docker Compose`, `Nginx`, `Certbot`
4. preparer les `jar` localement et copier:
   - `artifacts/`
   - `config/`
   - `deploy/`
5. ajouter du swap sur la VM si besoin
6. deployer les conteneurs backend avec:
   - [deploy/docker/docker-compose.production.yml](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/docker/docker-compose.production.yml)
7. verifier que les services locaux repondent:
   - `http://127.0.0.1:8889/actuator/health`
   - `http://127.0.0.1:8443/actuator/health`
   - `http://127.0.0.1:18443/auth`
8. pointer les DNS publics vers la VM
9. copier le build front dans `/var/www/quickdelivery-front`
10. installer la conf HTTP de bootstrap
11. emettre le certificat `api/auth`
12. emettre le certificat `app`
13. installer la conf finale TLS/reverse proxy + front statique
14. mettre a jour la configuration publique du backend et du front
15. tester:
   - front
   - API
   - auth
   - websocket
   - login
   - callback OAuth
   - validation email

## 6. Points D'Attention

- le front doit appeler `https://api.quickdelivery.tld`
- Keycloak doit exposer ses redirect URIs sur `https://app.quickdelivery.tld`
- les emails et QR codes doivent utiliser les domaines publics, jamais `localhost`
- le WebSocket transitant via `/ws/` doit rester route vers la gateway
- ne pas installer directement la conf finale si `app.quickdelivery.tld` n'a pas encore son certificat
