# QuickDelivery Front AWS

Ce dossier couvre le deploiement du front `quickdelivery-googlemaps-front` sur:

- `Amazon S3`
- `Amazon CloudFront`

## 1. Fichiers

- `.env.production.template`
- `.env.production.sample`
- `deploy-s3.sh`

## 2. Variables Front De Production

Copier:

```bash
cp deploy/frontend-aws/.env.production.template quickdelivery-googlemaps-front/.env.production
```

Ou partir d'un exemple deja rempli de facon coherente:

```bash
cp deploy/frontend-aws/.env.production.sample quickdelivery-googlemaps-front/.env.production
```

Puis adapter si besoin:

- `VUE_APP_GATEWAY_BASE_URL=https://api.quickdelivery.tld`
- `VUE_APP_AUTH_BASE_URL=https://auth.quickdelivery.tld/auth`
- `VUE_APP_WS_BASE_URL=wss://api.quickdelivery.tld/ws`

## 3. Ressources AWS A Creer

### 3.1 Bucket S3

Creer un bucket prive pour le front.

Exemple:

- `quickdelivery-front-prod`

Usage:

- stocker le contenu de `dist/`

### 3.2 Distribution CloudFront

Creer une distribution `CloudFront` avec:

- origine: bucket `S3`
- viewer protocol policy: `Redirect HTTP to HTTPS`
- default root object: `index.html`

Pour une SPA Vue, configurer aussi une gestion des erreurs:

- `403` -> `/index.html` avec code de reponse `200`
- `404` -> `/index.html` avec code de reponse `200`

Sinon le refresh navigateur sur une route type `/dashboard/client` cassera.

### 3.3 Certificat ACM

Pour `CloudFront`, le certificat public `ACM` doit etre emis dans:

- `us-east-1`

Nom conseille:

- `app.quickdelivery.tld`

### 3.4 DNS Route 53

Creer un enregistrement:

- `app.quickdelivery.tld`

vers la distribution `CloudFront`.

## 4. Politique D'Acces S3

Le bucket ne doit pas etre public si possible.

Approche recommandee:

- origine `CloudFront`
- acces restreint au bucket depuis la distribution

Si tu veux aller au plus vite pour un MVP, tu peux temporairement utiliser une politique publique de lecture, mais ce n'est pas l'option recommandee.

## 5. Deploiement Du Front

Prerequis:

- `aws cli` configure
- bucket `S3` cree
- distribution `CloudFront` creee
- `quickdelivery-googlemaps-front/.env.production` renseigne

Commande:

```bash
bash deploy/frontend-aws/deploy-s3.sh <s3-bucket-name> <cloudfront-distribution-id>
```

Le script:

1. build le front
2. synchronise `dist/` vers `S3`
3. demande une invalidation `CloudFront`

## 6. Checklist Finale Front

- `https://app.quickdelivery.tld` charge la landing
- le refresh d'une route SPA fonctionne
- le front appelle `https://api.quickdelivery.tld`
- le login part vers `https://auth.quickdelivery.tld/auth`
- les websockets passent par `wss://api.quickdelivery.tld/ws`

## 7. Couplage Avec Le Backend

Le front de prod doit etre coherent avec:

- [deploy/docker/.env.production.template](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/docker/.env.production.template)
- [deploy/nginx/README.md](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/nginx/README.md)
- [deploy/lightsail/README.md](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/README.md)

Les trois domaines doivent rester alignes:

- `app.quickdelivery.tld`
- `api.quickdelivery.tld`
- `auth.quickdelivery.tld`
