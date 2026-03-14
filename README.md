# QuickDelivery - Guide De Demarrage

## 1. Prerequis

- Java 21
- Maven 3.9+
- Node.js 18+ et npm
- MySQL accessible en local (base `QuickDeliveryDB`)
- MySQL accessible en local (base `KeycloakDB`)
- certificat TLS de dev present dans `certs/quickdelivery-dev.p12`

## 2. Demarrage Rapide (Terminal)

Depuis la racine du projet:

```powershell
cd C:\Users\imess\Documents\WorkSpace\Projects\DEV_WorkSpace\quickdelivery-parent
mvn clean install -DskipTests
```

Lancer ensuite chaque service dans un terminal separe, dans cet ordre:

1. Config Server (`8889`)
2. Discovery Server / Eureka (`8080`)
3. OAuth Authorization Server (`18443`)
4. Users Service (`8081`)
5. Packages Service (`8082`)
6. API Gateway (`8443`)

```powershell
cd quickdemivery-config-server
mvn spring-boot:run
```

```powershell
cd quickdelivery-registy-server
mvn spring-boot:run
```

```powershell
cd oauth-authorization-server
mvn spring-boot:run
```

```powershell
cd quickdelivery-users
mvn spring-boot:run
```

```powershell
cd quickdelivery-packages
mvn spring-boot:run
```

```powershell
cd quickdelivery-api-gateway
mvn spring-boot:run
```

Puis lancer le front:

```powershell
cd quickdelivery-googlemaps-front
npm install
npm run serve
```

## 3. URLs Utiles

- Front (Vue): `https://localhost:8084`
- Config Server base: `http://localhost:8889`
- Config Server actuator health: `http://localhost:8889/actuator/health`
- Registry/Eureka dashboard: `http://localhost:8080`
- Registry/Eureka actuator health: `http://localhost:8080/actuator/health`
- API Gateway base: `https://localhost:8443`
- API Gateway actuator health: `https://localhost:8443/actuator/health`
- OAuth base: `https://localhost:18443/auth`
- OAuth realm base: `https://localhost:18443/auth/realms/quickdelivery`
- OAuth OpenID configuration: `https://localhost:18443/auth/realms/quickdelivery/.well-known/openid-configuration`
- OAuth token endpoint: `https://localhost:18443/auth/realms/quickdelivery/protocol/openid-connect/token`
- OAuth certs (JWKS): `https://localhost:18443/auth/realms/quickdelivery/protocol/openid-connect/certs`
- OAuth logout endpoint: `https://localhost:18443/auth/realms/quickdelivery/protocol/openid-connect/logout`
- Users via gateway: `https://localhost:8443/users/v1/...`
- Packages via gateway: `https://localhost:8443/packages/v1/...`
- Packages websocket via gateway: `wss://localhost:8443/ws`
- Users direct (bloque): `http://localhost:8081/...`
- Packages direct (bloque): `http://localhost:8082/...`

Les appels front doivent passer par la gateway. Les appels directs vers les services backend (`8081`, `8082`) sont rejetes par filtre de securite.

## 4. Reseau Dev, LAN Et Production

### 4.1 Fonctionnement en developpement

- le front tourne sur `https://<host-dev>:8084`
- la gateway est exposee sur `https://<host-dev>:8443`
- OAuth/Keycloak est expose sur `https://<host-dev>:18443/auth`
- le WebSocket front passe par la gateway sur `wss://<host-dev>:8443/ws`

Le front derive automatiquement ses URLs backend a partir du hostname du navigateur si aucune variable d'environnement n'est fournie:

- gateway: `https://<host>:8443`
- auth: `https://<host>:18443/auth`
- websocket: `wss://<host>:8443/ws`

La gateway et OAuth detectent aussi automatiquement les hosts locaux du poste de developpement pour accepter:

- les origins CORS du front
- les redirect URIs Keycloak
- les web origins Keycloak
- les issuers JWT LAN du poste

Resultat:

- si ton PC change d'adresse IP sur le meme reseau, le mode dev continue de fonctionner apres redemarrage des services
- un autre developpeur sur son propre reseau local n'a pas besoin de recoder une IP dans le projet

### 4.2 Variables d'environnement Front

Le front supporte trois surcharges explicites:

- `VUE_APP_GATEWAY_BASE_URL`
- `VUE_APP_AUTH_BASE_URL`
- `VUE_APP_WS_BASE_URL`

Modeles fournis:

- [`.env.development.local.example`](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-googlemaps-front/.env.development.local.example)
- [`.env.production.example`](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-googlemaps-front/.env.production.example)

Usage:

1. copier `.env.development.local.example` vers `.env.development.local` si tu veux figer des URLs de dev
2. copier `.env.production.example` vers `.env.production` pour un build de prod

Si tu ne crées pas ces fichiers, le mode auto base sur le hostname reste actif.

### 4.3 Variables d'environnement Backend

Les services exposes utilisent maintenant ces surcharges:

- `QUICKDELIVERY_FRONTEND_BASE_URLS`
- `QUICKDELIVERY_AUTH_ISSUER_URIS`

Elles sont lues via le Config Server dans:

- [APIGatewayApplication.yml](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/config/APIGatewayApplication.yml)
- [oauth-authorization-server.yml](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/config/oauth-authorization-server.yml)

Comportement:

- si elles sont vides: auto-decouverte des hosts locaux pour le dev
- si elles sont renseignees: utilisation stricte des URLs fournies, utile pour la recette et la prod

Les services Java qui appellent des surfaces HTTPS locales utilisent aussi un truststore JVM local de dev:

- gateway
- users
- packages

Objectif:

- faire confiance au certificat HTTPS local QuickDelivery
- conserver en meme temps les certificats publics du JDK
- permettre aux services d'appeler a la fois:
  - OAuth local sur `https://localhost:18443`
  - la gateway locale en HTTPS
  - des APIs HTTPS externes comme Google Maps

Variables supportees:

- gateway:
  - `API_GATEWAY_SSL_TRUST_STORE`
  - `API_GATEWAY_SSL_TRUST_STORE_PASSWORD`
  - `API_GATEWAY_SSL_TRUST_STORE_TYPE`
- users:
  - `USERS_SSL_TRUST_STORE`
  - `USERS_SSL_TRUST_STORE_PASSWORD`
  - `USERS_SSL_TRUST_STORE_TYPE`
- packages:
  - `PACKAGES_SSL_TRUST_STORE`
  - `PACKAGES_SSL_TRUST_STORE_PASSWORD`
  - `PACKAGES_SSL_TRUST_STORE_TYPE`

Valeurs de dev par defaut:

- fichier: `certs/quickdelivery-dev.p12`
- type: `PKCS12`
- mot de passe: `QuickDelivery123@`

Comportement:

- le service charge le `cacerts` standard du JDK
- le certificat de dev QuickDelivery est ajoute a ce truststore standard
- un truststore fusionne temporaire est ensuite expose au process Java
- cela permet de faire confiance a la fois:
  - au HTTPS local QuickDelivery
  - aux services HTTPS externes comme Google Maps

Mise en place minimale:

1. verifier que le fichier `certs/quickdelivery-dev.p12` existe dans le depot
2. ne rien configurer si tu veux utiliser les valeurs par defaut
3. sinon definir les variables du service concerne avant son demarrage
4. redemarrer completement le service

Exemples PowerShell:

Gateway:

```powershell
$env:API_GATEWAY_SSL_TRUST_STORE="C:\Users\imess\Documents\WorkSpace\Projects\DEV_WorkSpace\quickdelivery-parent\certs\quickdelivery-dev.p12"
$env:API_GATEWAY_SSL_TRUST_STORE_PASSWORD="QuickDelivery123@"
$env:API_GATEWAY_SSL_TRUST_STORE_TYPE="PKCS12"
cd quickdelivery-api-gateway
mvn spring-boot:run
```

Users:

```powershell
$env:USERS_SSL_TRUST_STORE="C:\Users\imess\Documents\WorkSpace\Projects\DEV_WorkSpace\quickdelivery-parent\certs\quickdelivery-dev.p12"
$env:USERS_SSL_TRUST_STORE_PASSWORD="QuickDelivery123@"
$env:USERS_SSL_TRUST_STORE_TYPE="PKCS12"
cd quickdelivery-users
mvn spring-boot:run
```

Packages:

```powershell
$env:PACKAGES_SSL_TRUST_STORE="C:\Users\imess\Documents\WorkSpace\Projects\DEV_WorkSpace\quickdelivery-parent\certs\quickdelivery-dev.p12"
$env:PACKAGES_SSL_TRUST_STORE_PASSWORD="QuickDelivery123@"
$env:PACKAGES_SSL_TRUST_STORE_TYPE="PKCS12"
cd quickdelivery-packages
mvn spring-boot:run
```

Quand utiliser ces surcharges:

- utile si le certificat n'est pas a l'emplacement par defaut
- utile si un autre dev utilise un autre keystore local
- inutile si tu gardes le certificat versionne dans `certs/`

### 4.3.1 Configuration SMTP des emails applicatifs

Les emails envoyes par `users` et `packages` utilisent la configuration servie par le Config Server.

Proprietes actuellement definies:

- `quickdelivery.mail.host=smtp.gmail.com`
- `quickdelivery.mail.port=587`
- `quickdelivery.mail.username=quickdelivery529@gmail.com`
- `quickdelivery.mail.password=Quickdelivery123@`
- `quickdelivery.mail.smtp.auth=true`
- `quickdelivery.mail.smtp.starttls.enable=true`
- `quickdelivery.mail.debug=false`

Fichiers concernes:

- [Users_Service.properties](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/config/Users_Service.properties)
- [Package_Service.properties](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/config/Package_Service.properties)

Lecture effective au runtime:

- `quickdelivery-users` propage ces proprietes Spring en `System.setProperty(...)` au demarrage dans [UsersMain.java](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-users/src/main/java/com/quickdelivery/UsersMain.java)
- `quickdelivery-packages` fait la meme chose dans [PackageMain.java](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-packages/src/main/java/com/quickdelivery/PackageMain.java)
- le helper d'envoi lit ensuite ces proprietes dans [MailHelper.java](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-abstarct-dao/src/main/java/com/quickdelivery/abstarct/helpers/MailHelper.java)

Logs attendus au demarrage:

- `Users mail config: host=smtp.gmail.com, port=587, username=quickdelivery529@gmail.com, password=****`
- `Packages mail config: host=smtp.gmail.com, port=587, username=quickdelivery529@gmail.com, password=****`

Important:

- si Gmail refuse encore l'authentification, le mot de passe doit etre un mot de passe d'application valide, pas le mot de passe principal du compte
- les emails `users` et `packages` sont maintenant non bloquants: un echec SMTP ne doit plus casser un parcours metier comme creation de compte, reservation ou livraison

### 4.4 Exemple Production

Exemple de valeurs stables en production:

- `QUICKDELIVERY_FRONTEND_BASE_URLS=https://app.quickdelivery.com`
- `QUICKDELIVERY_AUTH_ISSUER_URIS=https://auth.quickdelivery.com/auth/realms/quickdelivery`
- `VUE_APP_GATEWAY_BASE_URL=https://api.quickdelivery.com`
- `VUE_APP_AUTH_BASE_URL=https://auth.quickdelivery.com/auth`
- `VUE_APP_WS_BASE_URL=wss://api.quickdelivery.com/ws`

## 5. Authentification Front + JWT

- Le front utilise OAuth2/OIDC (Keycloak realm `quickdelivery`) avec Authorization Code + PKCE.
- Si le token est absent/invalide, le front redirige automatiquement vers la page de login Keycloak.
- Le menu compte contient maintenant un item `Deconnexion` qui ferme la session locale et redirige vers le logout Keycloak.

Comptes de test:

- `client.test` / `Quickdelivery123@`
- `livreur.test` / `Quickdelivery123@`
- `admin.test` / `Quickdelivery123@`

Comptes techniques:

- Keycloak master admin: `bael-admin` / `pass`
- Config Server basic auth: `configuser` / `configpass`

### 5.1 Recreer Un Admin Keycloak

Si le compte admin applicatif du realm `quickdelivery` a ete supprime, tu peux le recreer depuis le master admin Keycloak.

Compte admin applicatif attendu:

- username: `admin.test`
- email: `admin.test@quickdelivery.local`
- mot de passe: `Quickdelivery123@`
- role realm: `ROLE_ADMIN`

Prerequis:

1. demarrer `oauth-authorization-server`
2. verifier que Keycloak repond sur `https://localhost:18443/auth`

Script PowerShell de recreation/mise a jour:

```powershell
$token = (
  curl.exe -k -s -X POST "https://localhost:18443/auth/realms/master/protocol/openid-connect/token" `
    -H "Content-Type: application/x-www-form-urlencoded" `
    --data "grant_type=password&client_id=admin-cli&username=bael-admin&password=pass" |
  ConvertFrom-Json
).access_token

$userLookup = curl.exe -k -s `
  -H "Authorization: Bearer $token" `
  "https://localhost:18443/auth/admin/realms/quickdelivery/users?username=admin.test" |
  ConvertFrom-Json

$role = curl.exe -k -s `
  -H "Authorization: Bearer $token" `
  "https://localhost:18443/auth/admin/realms/quickdelivery/roles/ROLE_ADMIN" |
  ConvertFrom-Json

$userPayload = '{"username":"admin.test","enabled":true,"emailVerified":true,"firstName":"Admin","lastName":"Test","email":"admin.test@quickdelivery.local"}'
$pwdPayload = '{"type":"password","value":"Quickdelivery123@","temporary":false}'
$rolePayload = "[{`"id`":`"$($role.id)`",`"name`":`"ROLE_ADMIN`"}]"

$userFile = Join-Path $env:TEMP "quickdelivery-admin-user.json"
$pwdFile = Join-Path $env:TEMP "quickdelivery-admin-password.json"
$roleFile = Join-Path $env:TEMP "quickdelivery-admin-role.json"

Set-Content -Path $userFile -Value $userPayload -Encoding ascii
Set-Content -Path $pwdFile -Value $pwdPayload -Encoding ascii
Set-Content -Path $roleFile -Value $rolePayload -Encoding ascii

if ($userLookup.Count -eq 0) {
  curl.exe -k -X POST "https://localhost:18443/auth/admin/realms/quickdelivery/users" `
    -H "Authorization: Bearer $token" `
    -H "Content-Type: application/json" `
    --data-binary "@$userFile"

  $userLookup = curl.exe -k -s `
    -H "Authorization: Bearer $token" `
    "https://localhost:18443/auth/admin/realms/quickdelivery/users?username=admin.test" |
    ConvertFrom-Json
}

$userId = $userLookup[0].id

curl.exe -k -X PUT "https://localhost:18443/auth/admin/realms/quickdelivery/users/$userId" `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  --data-binary "@$userFile"

curl.exe -k -X PUT "https://localhost:18443/auth/admin/realms/quickdelivery/users/$userId/reset-password" `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  --data-binary "@$pwdFile"

curl.exe -k -X POST "https://localhost:18443/auth/admin/realms/quickdelivery/users/$userId/role-mappings/realm" `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  --data-binary "@$roleFile"
```

Verification:

1. se connecter au front avec `admin.test` / `Quickdelivery123@`
2. verifier l'acces a `usersAccountValidation`

Postman (client OAuth de test):

- `client_id`: `quickdelivery-postman`
- token URL: `https://localhost:18443/auth/realms/quickdelivery/protocol/openid-connect/token`
- grant type: `password`
- username/password: utiliser un des comptes de test ci-dessus

### 5.1 Collection Postman du projet

La collection versionnee est:

- [quickdelivery-api-gateway.postman_collection.json](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-api-gateway.postman_collection.json)

Elle est alignee sur le setup HTTPS/TLS actuel:

- `gateway_url = https://localhost:8443`
- `iam_token_url = https://localhost:18443/auth/realms/quickdelivery/protocol/openid-connect/token`

Important:

- si Postman appelle encore `8087` ou `18084`, ce n'est pas la collection du depot mais une variable d'environnement Postman active qui ecrase les variables de collection
- verifier dans Postman la valeur resolue de `{{gateway_url}}` et `{{iam_token_url}}`
- verifier aussi l'environnement actif avant execution

### 5.2 Configuration Postman recommandee

Pour utiliser Postman avec le setup local actuel:

1. importer la collection du projet
2. verifier que les variables resolues sont bien:
   - `https://localhost:8443`
   - `https://localhost:18443/...`
3. dans `Settings > General`, mettre temporairement `SSL certificate verification = OFF` pour le dev local autosigne
4. ne pas utiliser `http://` sur les ports `8443` et `18443`

Erreurs typiques:

- `ECONNREFUSED 127.0.0.1:18084`
  - la requete vise encore l'ancien port OAuth
- `This combination of host and port requires TLS`
  - la requete part en `http://` vers un port HTTPS
- `ECONNREFUSED 192.168.x.x:8087`
  - une variable Postman active pointe encore vers l'ancienne gateway HTTP

## 6. Consulter La Configuration Dans Le Config Server

- Auth basic par defaut:
- username: `configuser`
- password: `configpass`

Exemples d'URLs:

- `http://localhost:8889/APIGatewayApplication/default`
- `http://localhost:8889/Users_Service/default`
- `http://localhost:8889/Package_Service/default`
- `http://localhost:8889/DISCOVERY-SERVER/default`
- `http://localhost:8889/oauth-authorization-server/default`

Exemple PowerShell:

```powershell
curl -u configuser:configpass http://localhost:8889/APIGatewayApplication/default
```
## 7. Actions A Faire Dans IntelliJ

### 6.1 Ouvrir le projet

1. `File` -> `Open`
2. Selectionner le dossier `quickdelivery-parent`
3. Laisser IntelliJ importer le projet Maven

### 6.2 Configurer le SDK

1. `File` -> `Project Structure` -> `Project`
2. Choisir `Project SDK: 21`
3. `Project language level`: `SDK default (21)`

### 6.3 Recharger Maven

1. Ouvrir l'onglet `Maven` (a droite)
2. Cliquer `Reload All Maven Projects`
3. Verifier que le build passe:
   - `Lifecycle` -> `clean`
   - `Lifecycle` -> `install` (avec `-DskipTests` si besoin)

### 6.4 Creer les Run Configurations backend

Creer 6 configurations de type **Spring Boot**:

1. `quickdemivery-config-server`
   - Main class: `com.quickdelivery.ConfigServerLauncher`
   - Working directory: `.../quickdemivery-config-server`

2. `quickdelivery-registy-server`
   - Main class: `com.quickdelivery.ServiceRegistrationAndDiscoveryServer`
   - Working directory: `.../quickdelivery-registy-server`

3. `oauth-authorization-server`
   - Main class: `com.baeldung.auth.AuthorizationServerApp`
   - Working directory: `.../oauth-authorization-server`

4. `quickdelivery-users`
   - Main class: `com.quickdelivery.UsersMain`
   - Working directory: `.../quickdelivery-users`

5. `quickdelivery-packages`
   - Main class: `com.quickdelivery.PackageMain`
   - Working directory: `.../quickdelivery-packages`

6. `quickdelivery-api-gateway`
   - Main class: `com.quickdelivery.APIGatewayApplication`
   - Working directory: `.../quickdelivery-api-gateway`

Lancer dans cet ordre:

1. `quickdemivery-config-server`
2. `quickdelivery-registy-server`
3. `oauth-authorization-server`
4. `quickdelivery-users`
5. `quickdelivery-packages`
6. `quickdelivery-api-gateway`

### 6.5 Lancer le front dans IntelliJ

Option simple: Terminal integre IntelliJ

```powershell
cd quickdelivery-googlemaps-front
npm install
npm run serve
```

Option alternative: Run configuration `npm`

1. `Run` -> `Edit Configurations...`
2. `+` -> `npm`
3. `package.json`: `quickdelivery-googlemaps-front/package.json`
4. `Command`: `run`
5. `Scripts`: `serve`

## 8. Certificats TLS En Developpement

Les surfaces exposees au navigateur/mobile sont en HTTPS:

- front: `8084`
- gateway: `8443`
- oauth: `18443`

Le certificat de dev par defaut est:

- [quickdelivery-dev.p12](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/certs/quickdelivery-dev.p12)

Points importants:

- le navigateur du poste de dev doit faire confiance au certificat si besoin
- un telephone ou un autre PC du meme reseau doit aussi faire confiance au certificat pour eviter les erreurs TLS
- la gateway ajoute automatiquement le certificat dev QuickDelivery au truststore JVM standard
- `quickdelivery-users` ajoute automatiquement le certificat dev QuickDelivery au truststore JVM standard
- `quickdelivery-packages` ajoute automatiquement le certificat dev QuickDelivery au truststore JVM standard

Au demarrage, les logs attendus sont:

- `Gateway truststore: path=..., type=PKCS12, mode=merged-default`
- `Users truststore: path=..., type=PKCS12, mode=merged-default`
- `Packages truststore: path=..., type=PKCS12, mode=merged-default`

## 9. Arreter Tous Les Services

- Dans IntelliJ: bouton `Stop` sur chaque configuration en cours.
- En terminal: `Ctrl + C` dans chaque terminal.

## 10. Application Mobile

L'application mobile repose sur le front Vue [quickdelivery-googlemaps-front](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-googlemaps-front), embarque via Capacitor dans :
- Android : [android](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-googlemaps-front/android)
- iOS : [ios](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-googlemaps-front/ios)

Identite Capacitor :
- `appId`: `com.quickdelivery.app`
- `appName`: `QuickDelivery`

### 8.1 Prerequis Mobile

- Node.js + npm
- Android Studio pour Android
- SDK Android installe
- macOS + Xcode + CocoaPods pour iOS

### 8.2 Scripts Mobile

Depuis [quickdelivery-googlemaps-front](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-googlemaps-front) :

```powershell
npm run cap:sync
```

```powershell
npm run cap:sync:all
```

```powershell
npm run cap:sync:android
```

```powershell
npm run cap:sync:ios
```

```powershell
npm run cap:open:android
```

```powershell
npm run cap:open:ios
```

Notes :
- `cap:sync:*` relance automatiquement `npm run build`
- sur Windows, `cap:open:ios` affiche un message explicite car Xcode n'est pas disponible
- sur Mac, tu peux aussi utiliser `npm run cap:open:ios:force`

### 8.3 Executer L'App Mobile Android

1. Demarrer tout le backend comme pour le web.
2. Demarrer le front une premiere fois si tu veux verifier le build web :

```powershell
cd quickdelivery-googlemaps-front
npm install
npm run build
```

3. Synchroniser Android :

```powershell
npm run cap:sync:android
```

4. Ouvrir le projet natif :

```powershell
npm run cap:open:android
```

5. Depuis Android Studio :
- attendre la synchro Gradle
- choisir un emulateur ou un device physique
- lancer l'application

### 8.4 Tester L'App Mobile Android

Verifier au minimum :
- ouverture de l'application
- acces a la landing publique
- navigation vers connexion / creation de compte
- creation de colis
- affichage carte / geolocalisation
- consultation des colis et suivi

Prevoir un device/emulateur ayant acces aux services locaux :
- emulateur Android :
  - si l'app appelle `localhost` depuis le mobile, il faudra souvent remplacer par `10.0.2.2` pour atteindre la machine hote
- device physique :
  - utiliser l'IP reseau locale de ta machine au lieu de `localhost`

Si tes URLs front/back sont encore en `localhost`, le web desktop marchera mais pas forcement l'app mobile hors navigateur. C'est le point principal a verifier avant campagne de tests mobile.

### 8.5 Executer L'App Mobile iOS

1. Depuis Windows, tu peux seulement generer/synchroniser le projet :

```powershell
cd quickdelivery-googlemaps-front
npm run cap:sync:ios
```

2. Sur un Mac :
- recuperer le projet
- installer CocoaPods si besoin
- lancer :

```bash
cd quickdelivery-googlemaps-front
npm install
npm run cap:sync:ios
npm run cap:open:ios
```

3. Dans Xcode :
- choisir un simulateur ou un iPhone physique
- verifier la signature
- lancer l'application

### 8.6 Tester L'App Mobile iOS

Verifier les memes parcours que sur Android :
- landing publique
- authentification
- creation de compte
- creation de colis
- paiement de test
- suivi
- geolocalisation

Attention aux memes contraintes reseau :
- `localhost` depuis le simulateur/device iOS ne cible pas automatiquement les services de ta machine de dev
- utiliser l'IP reseau de la machine ou une configuration adaptee

## 11. Troubleshooting

- Si un service n'arrive pas a charger sa config:
  - Verifier que `quickdemivery-config-server` tourne sur `8889`.
- Si l'auth OAuth ne demarre pas:
  - Verifier qu'aucun process n'utilise `18443`.
- Si `users`/`packages` ne demarrent pas apres changement Maven:
  - Relancer `mvn clean install -DskipTests` a la racine.
- Si la gateway ne demarre pas:
  - Verifier que `config-server`, `registry`, `oauth`, `users`, `packages` sont deja lances.
- Si le front ne repond pas:
  - Verifier que `npm run serve` expose bien `https://localhost:8084`.
- Si l'auth marche en local mais pas depuis un autre device du LAN:
  - verifier que le certificat TLS est accepte sur ce device
  - verifier que le device appelle bien l'URL du poste de dev, pas `localhost`
- Si le front charge mais que les appels API sont bloques:
  - verifier que `quickdelivery-api-gateway` a bien ete redemarre apres changement de reseau/config
- Si les tokens sont rejetes apres changement d'host/IP:
  - redemarrer `oauth-authorization-server` et `quickdelivery-api-gateway`
- Si l'app mobile ne joint pas le backend:
  - verifier les URLs `localhost`
  - sur emulateur Android, tester avec `10.0.2.2`
  - sur device physique, utiliser l'IP locale de la machine de developpement
