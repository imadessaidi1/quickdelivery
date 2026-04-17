# Guide de Lancement Local - QuickDelivery

Ce guide explique comment lancer tous les modules QuickDelivery sur votre machine Windows locale sans impacter la production.

## Prérequis
1. **MySQL** : Installé et tournant sur `localhost:3306`.
2. **Base de données** : Créez une base nommée `QuickDeliveryDB`.
3. **Utilisateur MySQL** :
   ```sql
   CREATE USER 'QuickDelivery'@'localhost' IDENTIFIED BY 'QuickDelivery123@';
   GRANT ALL PRIVILEGES ON QuickDeliveryDB.* TO 'QuickDelivery'@'localhost';
   FLUSH PRIVILEGES;
   ```
4. **Dossier de Documents** : Créez le dossier `C:\Users\imess\Documents\QuickDelivery`.

## Ordre de Lancement des Modules
Pour chaque module, utilisez le profil `local`.

### 1. Registre (Eureka)
**Module** : `quickdelivery-registy-server`
**Commande** : `mvn spring-boot:run -Dspring-boot.run.profiles=local`

### 2. Services Backend (Peuvent être lancés en parallèle après Eureka)
Lancez chaque module avec le profil `local` :
- `quickdelivery-users` (Port 8081)
- `quickdelivery-packages` (Port 8082)
- `quickdelivery-shipment`
- `quickdelivery-batches`
- `oauth-authorization-server` (Port 18084)

**Commande type** : `mvn spring-boot:run -Dspring-boot.run.profiles=local`

### 3. API Gateway
**Module** : `quickdelivery-api-gateway`
**Commande** : `mvn spring-boot:run -Dspring-boot.run.profiles=local`

## Points Clés de la Configuration Locale
- **Config Server** : Désactivé en local (`spring.cloud.config.enabled=false`) pour éviter les conflits avec le serveur AWS.
- **Stockage** : Les documents sont stockés dans `C:\Users\imess\Documents\QuickDelivery`.
- **Base de données** : Connexion à `localhost:3306`.
- **Discovery** : Utilise le Eureka local sur `localhost:8080`.

> [!IMPORTANT]
> Ne modifiez pas les fichiers `application.properties` de base, car ils sont utilisés pour le déploiement AWS. Toutes vos modifications locales doivent rester dans les fichiers `application-local.properties`.
