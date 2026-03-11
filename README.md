# QuickDelivery - Guide De Demarrage

## 1. Prerequis

- Java 21
- Maven 3.9+
- Node.js 18+ et npm
- MySQL accessible en local (base `QuickDeliveryDB`)

## 2. Demarrage Rapide (Terminal)

Depuis la racine du projet:

```powershell
cd C:\Users\imess\Documents\WorkSpace\Projects\DEV_WorkSpace\quickdelivery-parent
mvn clean install -DskipTests
```

Lancer ensuite chaque service dans un terminal separe, dans cet ordre:

1. Config Server (`8889`)
2. Discovery Server / Eureka (`8080`)
3. OAuth Authorization Server (`18084`)
4. Users Service (`8081`)
5. Packages Service (`8082`)
6. API Gateway (`8087`)

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

- Front (Vue): `http://localhost:8084` (ou le port affiche par `npm run serve`)
- Config Server base: `http://localhost:8889`
- Config Server actuator health: `http://localhost:8889/actuator/health`
- Registry/Eureka dashboard: `http://localhost:8080`
- Registry/Eureka actuator health: `http://localhost:8080/actuator/health`
- API Gateway base: `http://localhost:8087`
- API Gateway actuator health: `http://localhost:8087/actuator/health`
- OAuth base: `http://localhost:18084/auth`
- OAuth realm base: `http://localhost:18084/auth/realms/quickdelivery`
- OAuth OpenID configuration: `http://localhost:18084/auth/realms/quickdelivery/.well-known/openid-configuration`
- OAuth token endpoint: `http://localhost:18084/auth/realms/quickdelivery/protocol/openid-connect/token`
- OAuth certs (JWKS): `http://localhost:18084/auth/realms/quickdelivery/protocol/openid-connect/certs`
- OAuth logout endpoint: `http://localhost:18084/auth/realms/quickdelivery/protocol/openid-connect/logout`
- Users via gateway: `http://localhost:8087/users/v1/...`
- Packages via gateway: `http://localhost:8087/packages/v1/...`
- Packages websocket via gateway: `ws://localhost:8087/ws`
- Users direct (bloque): `http://localhost:8081/...`
- Packages direct (bloque): `http://localhost:8082/...`

Les appels front doivent passer par la gateway. Les appels directs vers les services backend (`8081`, `8082`) sont rejetes par filtre de securite.

## 4. Authentification Front + JWT

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

Postman (client OAuth de test):

- `client_id`: `quickdelivery-postman`
- token URL: `http://localhost:18084/auth/realms/quickdelivery/protocol/openid-connect/token`
- grant type: `password`
- username/password: utiliser un des comptes de test ci-dessus

## 5. Consulter La Configuration Dans Le Config Server

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
## 6. Actions A Faire Dans IntelliJ

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

## 7. Arreter Tous Les Services

- Dans IntelliJ: bouton `Stop` sur chaque configuration en cours.
- En terminal: `Ctrl + C` dans chaque terminal.

## 8. Application Mobile

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

## 9. Troubleshooting

- Si un service n'arrive pas a charger sa config:
  - Verifier que `quickdemivery-config-server` tourne sur `8889`.
- Si l'auth OAuth ne demarre pas:
  - Verifier qu'aucun process n'utilise `18084`.
- Si `users`/`packages` ne demarrent pas apres changement Maven:
  - Relancer `mvn clean install -DskipTests` a la racine.
- Si la gateway ne demarre pas:
  - Verifier que `config-server`, `registry`, `oauth`, `users`, `packages` sont deja lances.
- Si le front ne repond pas:
  - Regarder l'URL exacte affichee dans la console (`App running at`).
- Si l'app mobile ne joint pas le backend:
  - verifier les URLs `localhost`
  - sur emulateur Android, tester avec `10.0.2.2`
  - sur device physique, utiliser l'IP locale de la machine de developpement
