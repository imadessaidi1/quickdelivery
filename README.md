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

```powershell
cd quickdelivery-registy-server
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

## 3. URLs De Verification

- Eureka: `http://localhost:8080`
- Gateway health: `http://localhost:8083/actuator/health`
- Front: URL affichee dans la console Vue (`App running at`, souvent `http://localhost:8084` si d'autres ports sont pris)

Les appels front passent par la gateway:

- `http://localhost:8083/users/v1/...`
- `http://localhost:8083/packages/v1/...`

## 4. Actions A Faire Dans IntelliJ

### 4.1 Ouvrir le projet

1. `File` -> `Open`
2. Selectionner le dossier `quickdelivery-parent`
3. Laisser IntelliJ importer le projet Maven

### 4.2 Configurer le SDK

1. `File` -> `Project Structure` -> `Project`
2. Choisir `Project SDK: 21`
3. `Project language level`: `SDK default (21)`

### 4.3 Recharger Maven

1. Ouvrir l'onglet `Maven` (a droite)
2. Cliquer `Reload All Maven Projects`
3. Verifier que le build passe:
   - `Lifecycle` -> `clean`
   - `Lifecycle` -> `install` (avec `-DskipTests` si besoin)

### 4.4 Creer les Run Configurations backend

Creer 4 configurations de type **Spring Boot**:

1. `quickdelivery-registy-server`
   - Main class: `com.quickdelivery.ServiceRegistrationAndDiscoveryServer`
   - Working directory: `.../quickdelivery-registy-server`

2. `quickdelivery-users`
   - Main class: `com.quickdelivery.UsersMain`
   - Working directory: `.../quickdelivery-users`

3. `quickdelivery-packages`
   - Main class: `com.quickdelivery.PackageMain`
   - Working directory: `.../quickdelivery-packages`

4. `quickdelivery-api-gateway`
   - Main class: `com.quickdelivery.APIGatewayApplication`
   - Working directory: `.../quickdelivery-api-gateway`

Lancer dans cet ordre:

1. `quickdelivery-registy-server`
2. `quickdelivery-users`
3. `quickdelivery-packages`
4. `quickdelivery-api-gateway`

### 4.5 Lancer le front dans IntelliJ

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

## 5. Arreter Tous Les Services

- Dans IntelliJ: bouton `Stop` sur chaque configuration en cours.
- En terminal: `Ctrl + C` dans chaque terminal.

## 6. Troubleshooting

- Si `users`/`packages` ne demarrent pas apres changement Maven:
  - Relancer `mvn clean install -DskipTests` a la racine.
- Si la gateway ne demarre pas:
  - Verifier que `registry` est deja lance sur `8080`.
- Si le front ne repond pas:
  - Regarder l'URL exacte affichee dans la console (`App running at`).

