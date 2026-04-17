# QuickDelivery Feature Execution Tree

Ce document aligne les features de l'application avec les routes front, les endpoints backend et les suites de test existantes. Il sert de reference pour verifier qu'une evolution produit reste couverte par au moins une suite rapide (`smoke` ou `regression`) et, si necessaire, par une campagne de charge.

## Lecture

- `Route front`: entree utilisateur dans `quickdelivery-googlemaps-front/src/routers/index.js`.
- `Backend`: endpoints principaux appeles par le parcours.
- `Tests rapides`: suites a lancer apres chaque deploy ou changement fonctionnel.
- `Tests charge`: suites a lancer avant release majeure ou optimisation infra.
- `Gap`: couverture a renforcer par test unitaire, scenario k6 ou test UI.

## Arbre D'Execution

### 1. Acces Public Et Authentification

- Landing publique
  - Route front: `/`
  - Backend: assets statiques, vhost `app.quickdelivery.fr`
  - Tests rapides: `security-pentest` pour headers, `vm1-frontdoor` pour entree publique
  - Gap: pas de test UI automatise sur contenu landing

- Login OIDC
  - Route front: `/login`
  - Backend: Keycloak `/auth/realms/quickdelivery/protocol/openid-connect/*`
  - Tests rapides: `security-pentest` pour CORS/auth, `smoke` et `regression` via refresh token dans `perf/run.ps1`
  - Tests charge: `nominal`, `stress`
  - Gap: pas de test navigateur du callback OAuth

- Conditions d'utilisation
  - Route front: `/terms-of-use`
  - Backend: assets statiques
  - Tests rapides: non couvert
  - Gap: test UI statique a ajouter

### 2. Creation Et Gestion De Compte

- Inscription client
  - Routes front: `/register`, `/userSignInPage`
  - Backend: `POST /users/v1/create-account`, `GET /users/v1/public-registration-status`, `GET /users/v1/validateEmail`
  - Tests rapides: `smoke`, `regression`, scenario `customer-onboarding`
  - Tests charge: `nominal`, `stress`, `db-pressure`, `public-realistic`
  - Gap: validation captcha front non testee en navigateur

- Inscription livreur / onboarding
  - Routes front: `/register`, `/userSignInPage`
  - Backend: `POST /users/v1/create-account`, `POST /users/v1/save-onboarding-draft`, `POST /users/v1/complete-onboarding`, `GET /users/v1/onboarding-status`
  - Tests rapides: `smoke`, `regression`, scenario `courier-onboarding`
  - Tests charge: `nominal`, `stress`
  - Gap: OCR/Textract et upload documents couverts en k6 mais pas en unitaire service

- Validation des comptes
  - Routes front: `/usersAccountValidation`, `/userValidationDetails`, `/document`
  - Backend: `GET /users/v1/usersForValidation`, `PUT /users/v1/validateUser`, `GET /users/v1/document-content`
  - Tests rapides: `smoke`, `regression`, scenario `courier-onboarding`
  - Tests charge: `nominal`, `stress`
  - Gap: pagination/recherche admin a renforcer

- Mon compte / mise a jour profil
  - Route front: `/userAccount`
  - Backend: `GET /users/v1/user{id}`, `POST /users/v1/update`, `POST /users/v1/public-update`, `GET /users/v1/public-update-profile`
  - Tests rapides: partiel via onboarding
  - Gap: scenario update compte dedie a ajouter

### 3. Creation Et Paiement Colis

- Creation colis authentifiee
  - Route front: `/createPackage`
  - Backend: `POST /packages/v1/estimate-price`, `POST /packages/v1/create`
  - Tests rapides: `smoke`, `regression`, scenario `e2e-delivery`
  - Tests charge: `nominal`, `stress`, `packages-write-heavy`
  - Tests unitaires: `PackageDeliveryPriceCalculatorTest`, `PackagesServiceValidationTest`
  - Gap: upload document colis non teste en unitaire stockage

- Creation colis anonyme / paiement invite
  - Routes front: `/createPackage`, `/paymentPage`
  - Backend: `POST /packages/v1/estimate-price`, `POST /packages/v1/create`, `PUT /packages/v1/confirm-guest-payment`, `GET /packages/v1/getGuestPackage`
  - Tests rapides: `smoke`, `regression`, scenario `guest-checkout`
  - Tests charge: `public-realistic`, `packages-write-heavy`, `vm1-frontdoor`, `db-pressure`
  - Gap: parcours UI paiement non teste en navigateur

- Detail colis
  - Routes front: `/package`, `/document`
  - Backend: `GET /packages/v1/getPackage`, `GET /packages/v1/getGuestPackage`, `GET /packages/v1/document-content`
  - Tests rapides: `guest-checkout`, `e2e-delivery`
  - Gap: affichage conditionnel des adresses par statut a couvrir en test UI ou composant

### 4. Recherche, Reservation Et Tournee Livreur

- Recherche colis autour de moi
  - Route front: `/app`
  - Backend: `GET /packages/v1/packages-around-me`, `GET /packages/v1/packages-around-me-by-destination`, `GET /packages/v1/map/packages-in-bounds`, `GET /packages/v1/packages-on-my-road`
  - Tests rapides: scenario `seed-visible-packages` pour donnees de recette
  - Tests charge: `vm1-frontdoor`, `public-realistic`
  - Gap: filtres rayon/destination et rendu carte non testes automatiquement

- Planification / reservation tournee
  - Routes front: `/app`, `/myRoute`
  - Backend: `POST /packages/v1/plan-route`, `PUT /packages/v1/reserve-batch`, `PUT /packages/v1/reserve-batch-planned`, `GET /packages/v1/active-route`
  - Tests rapides: partiel via `e2e-delivery`; `courier-lifecycle` si `ENABLE_COURIER_LIFECYCLE_SMOKE=true`
  - Gap: non-demarrage/annulation de tournee personnalisee a couvrir par tests service et scenario k6 dedie

- Reservation individuelle
  - Route front: `/package`
  - Backend: `PUT /packages/v1/reserve`, `PUT /packages/v1/cancel-reservation`, `GET /packages/v1/reservation-availability`
  - Tests rapides: `courier-lifecycle` si donnees preparees
  - Gap: penalites reservation individuelle non demarree a couvrir en unitaire service

- Pickup / livraison
  - Routes front: `/myPackages`, `/package`, `/myRoute`
  - Backend: `PUT /packages/v1/pickup`, `PUT /packages/v1/deliver`, `GET /packages/v1/checkOTPForPickup`, `GET /packages/v1/checkOTPForDelivery`, `GET /packages/v1/delivery-context`
  - Tests rapides: `smoke`, `regression`, scenario `e2e-delivery`
  - Tests charge: `nominal`, `stress`, `packages-write-heavy`
  - Gap: tests unitaires OTP/validation rayon a completer

### 5. Suivi Live Et Notifications

- Tracking colis
  - Routes front: `/packageTracking`, `/packageTrackingSummary`
  - Backend: `POST /packages/v1/tracking/position`, `POST /packages/v1/tracking/package-position`, `GET /packages/v1/tracking/active-package`, WebSocket `/ws`
  - Tests rapides: `smoke`, `regression`, scenario `tracking-live`
  - Tests charge: `crash`, scenario `tracking-live`
  - Gap: test UI carte tracking non couvert

- Notifications
  - Route front: `/notifications`
  - Backend: `GET /packages/v1/notifications`, `POST /packages/v1/notifications/mark-read`, `POST /packages/v1/notifications/mark-all-read`, `POST /packages/v1/devices/register`, `DELETE /packages/v1/devices/unregister`, `POST /packages/v1/notifications/preferences`
  - Tests rapides: `smoke`, `regression` via probe admin notifications sans 5xx
  - Gap: scenario utilisateur complet lecture/marquage a ajouter

### 6. Back Office Et Observabilite

- Dashboards role admin/client/livreur
  - Routes front: `/dashboard/admin`, `/dashboard/client`, `/dashboard/courier`
  - Backend: `GET /packages/v1/admin/dashboard-summary`, `GET /users/v1/admin/user-overview`
  - Tests rapides: `smoke`, `regression`, scenario `admin-dashboards`
  - Tests charge: `nominal`, `stress`, `db-pressure`, `public-realistic`
  - Gap: dashboards client/livreur non separes dans k6

- Centre metriques
  - Route front: `/dashboard/metrics`
  - Backend: `GET /gateway/v1/admin/metrics`, `GET /packages/v1/admin/metrics`, `GET /users/v1/admin/metrics`, `GET /packages/v1/admin/tracking-metrics`, breakdown/log insights
  - Tests rapides: `smoke`, `regression`, `security-pentest`
  - Tests charge: `nominal`, `stress`
  - Gap: assertions detaillees par widget a ajouter

- Centre financier
  - Route front: `/dashboard/finance`
  - Backend: `GET /packages/v1/admin/financial-dashboard`
  - Tests rapides: `smoke`, `regression`
  - Tests charge: `nominal`, `stress`, `db-pressure`
  - Gap: assertions metier montants/periodes a ajouter

- Penalites livreur
  - Route front: `/dashboard/courier-penalties`
  - Backend: `GET /packages/v1/admin/courier-penalties`, `POST /packages/v1/admin/courier-penalties/{penaltyId}/lift`, `POST /packages/v1/admin/courier-penalties/impose`
  - Tests rapides: lecture couverte par `admin-dashboards`
  - Gap: levage/imposition manuelle a couvrir avec scenario dedie non destructif ou donnees de test

## Alignement Des Suites

| Suite | Role dans l'arbre | Branches couvertes |
| --- | --- | --- |
| `smoke` | Verification rapide post-deploy | guest checkout, onboarding client/livreur, e2e delivery, admin centers, tracking live, courier lifecycle optionnel |
| `regression` | Non-regression courte | meme couverture que `smoke` avec 1 iteration par branche |
| `nominal` | Charge quotidienne | onboarding client/livreur, delivery, admin, courier lifecycle optionnel |
| `stress` | Degradation sous charge | onboarding, delivery, admin |
| `public-realistic` | Mix public | guest checkout, onboarding client, delivery, admin |
| `packages-write-heavy` | Hot path packages | guest create/pay, package lifecycle |
| `db-pressure` | Pression DB | guest checkout, onboarding client, dashboards admin |
| `vm1-frontdoor` | Nginx/gateway/front door | guest checkout et admin background |
| `tracking-live` | Tracking cible | WebSocket + HTTP position |
| `security-pentest` | Acces et hardening | headers, CORS, anonymous/invalid token, payloads hostiles |

## Gaps Prioritaires

1. Tests UI navigateur pour callback OAuth, affichage mobile et rendu carte/slider.
2. Tests service pour penalites de tournee: non-demarrage, annulation apres demarrage, reservation individuelle non demarree.
3. Scenario notifications complet: lister, marquer une notification, tout marquer lu, preferences/device.
4. Scenario update compte utilisateur: update profil, documents, vehicule.
5. Assertions metier detaillees sur finance, dashboard summary et penalties.
