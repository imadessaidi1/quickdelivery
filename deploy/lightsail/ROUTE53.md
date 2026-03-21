# QuickDelivery Route 53 MVP

Cette note couvre uniquement les enregistrements DNS minimaux pour l'option MVP:

- front sur `S3 + CloudFront`
- backend sur `Lightsail`

## 1. Sous-domaines cibles

- `app.quickdelivery.tld`
- `api.quickdelivery.tld`
- `auth.quickdelivery.tld`

## 2. Cibles AWS

### Front

Le front pointe vers la distribution `CloudFront`.

Enregistrement conseille:

- type `A`
- alias `Yes`
- target: distribution `CloudFront`

### API

L'API pointe vers l'IP publique statique de la VM `Lightsail`.

Enregistrement conseille:

- type `A`
- alias `No`
- value: IP publique de la VM

### Auth

L'auth pointe aussi vers l'IP publique statique de la VM `Lightsail`.

Enregistrement conseille:

- type `A`
- alias `No`
- value: IP publique de la VM

## 3. Zone minimale

Exemple:

- `app` -> `CloudFront`
- `api` -> `x.x.x.x`
- `auth` -> `x.x.x.x`

## 4. Prerequis

- reserver une IP statique `Lightsail`
- l'attacher a la VM avant d'exposer `api` et `auth`

Sans IP statique:

- un redemarrage/recreation de VM peut casser les DNS

## 5. Verification

Verifier apres propagation:

```bash
nslookup app.quickdelivery.tld
nslookup api.quickdelivery.tld
nslookup auth.quickdelivery.tld
```

Puis:

```bash
curl -I https://app.quickdelivery.tld
curl -I https://api.quickdelivery.tld/actuator/health
curl -I https://auth.quickdelivery.tld/auth
```
