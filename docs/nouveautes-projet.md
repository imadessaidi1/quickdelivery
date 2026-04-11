# Nouveautes Projet QuickDelivery

Ce document centralise les evolutions fonctionnelles et techniques majeures ajoutees recemment au projet.

## 1. Homepage et recherche logistique

La homepage livreur/admin a ete restructuree autour de 3 usages metier:

- `Autour de moi`
- `Direct`
- `Tournee`

### Autour de moi

Le mode `Autour de moi` continue d'afficher les colis disponibles proches de la position du livreur.

### Direct

Le mode `Direct` n'utilise plus un rayon choisi manuellement dans l'UI.

Le comportement attendu est maintenant:

- rayon de recherche derive automatiquement du `deliveryMode`
- collecte du colis proche de la position du livreur
- livraison du colis proche de l'adresse saisie
- limitation du nombre de colis a la capacite du vehicule
- proposition d'une tournee de collecte puis d'une tournee de livraison

### Tournee

Le mode `Tournee` construit un trajet personnel:

- position actuelle du livreur
- destination saisie

Puis il applique:

- recherche des colis sur corridor
- contraintes metier de compatibilite colis/vehicule
- reservation groupee
- affichage de la tournee ordonnee sur la carte

## 2. Moteur de planification de tournee

Le moteur de tournee a evolue en plusieurs etapes.

### V1 UI

Premiere logique heuristique cote front:

- `Tournee`: progression monotone le long du trajet
- `Direct`: nearest-neighbor sur les collectes puis les livraisons

### V2

La logique a ensuite ete sortie du composant UI dans un module dedie:

- [routePlanning.js](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-googlemaps-front/src/services/routePlanning.js)

Ajouts:

- metriques de qualite de tournee
- distance totale
- duree estimee
- detour
- gain affiche
- gain par km
- gain par heure

### V2.3 backend source of truth

Le backend calcule maintenant le plan de route autoritaire via:

- `POST /packages/v1/plan-route`

Le front ne garde plus que:

- l'affichage
- un fallback local si l'API de planning echoue

## 3. Reservation groupee planifiee

La reservation batch a ete renforcee.

Le flux n'envoie plus seulement une liste brute d'IDs.

Il passe maintenant par:

- `PUT /packages/v1/reserve-batch-planned`

Le backend:

- revalide le plan de tournee
- reserve les colis retenus
- renvoie la tournee reservee officielle

Resultat:

- l'ordre affiche et l'ordre reserve sont coherents
- la reservation groupee repose sur un plan backend verifie

## 4. Tournee persistante comme entite metier

Une vraie mission de tournee a ete introduite dans le modele metier.

Nouvelles entites:

- [DeliveryRoute.java](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-abstarct-dao/src/main/java/com/quickdelivery/abstarct/entities/DeliveryRoute.java)
- [DeliveryRouteStop.java](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-abstarct-dao/src/main/java/com/quickdelivery/abstarct/entities/DeliveryRouteStop.java)

Nouveaux statuts:

- `PLANNED`
- `ACTIVE`
- `COMPLETED`
- `CANCELLED`

La tournee persistante contient:

- le livreur
- le point de depart
- le point d'arrivee
- la distance totale
- la duree estimee
- le montant total affiche
- le poids total
- le volume total
- la liste ordonnee des stops

## 5. Blocage des reservations pendant une tournee

Quand un livreur a une tournee:

- `PLANNED`
- ou `ACTIVE`

il ne peut plus reserver de nouveaux colis.

Ce verrou est applique:

- sur reservation simple
- sur reservation groupee
- sur reservation groupee planifiee

Le front reflète ce verrou par:

- des boutons de reservation desactives
- un bandeau de contexte de tournee active

## 6. Navigation Google Maps sur tournee

Apres reservation groupee:

- l'application ouvre Google Maps avec la tournee
- les etapes sont segmentees si le nombre de waypoints depasse la limite raisonnable

Le backend genere:

- `googleMapsNavigationUrl`
- `googleMapsNavigationUrls`

Le front peut:

- ouvrir le premier segment directement
- reprendre le segment courant depuis l'ecran `Ma tournee`

## 7. Ecran Ma tournee

Une page dediee a la mission active a ete ajoutee:

- [ActiveRoutePage.vue](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-googlemaps-front/src/pages/ActiveRoutePage.vue)

Elle propose:

- resume de mission
- statut de tournee
- nombre de colis
- distance totale
- duree estimee
- montant total affiche
- progression des stops
- liste ordonnee collecte/livraison
- reprise segment par segment dans Google Maps

Elle est accessible via:

- `/myRoute`
- le menu principal
- le bandeau de tournee active

## 8. Contraintes capacitaires sur le moteur logistique

Le moteur de recherche et de planning ne se limite plus au nombre de colis.

Il integre maintenant:

- capacite max de colis par type de vehicule
- compatibilite de taille colis/vehicule
- poids total maximum
- volume total maximum

Important:

- la prise en compte poids/volume est actuellement conservative au niveau selection globale du lot
- ce n'est pas encore un solveur complet de charge dynamique stop par stop

## 9. Categorisation des colis persistante

La taille du colis choisie dans le formulaire est maintenant persistante de bout en bout.

La categorie du formulaire:

- `SMALL`
- `MEDIUM`
- `LARGE`
- `EXTRA_LARGE`

est:

- envoyee par le front
- stockee en base
- renvoyee par les DTO
- reutilisee dans les filtres vehicule

## 10. Moteur de prix hybride

Le moteur de pricing a ete profondement revu.

Fichier principal:

- [PackageDeliveryPriceCalculator.java](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-packages/src/main/java/com/quickdelivery/helpers/PackageDeliveryPriceCalculator.java)

### Regime court et moyen trajet

Pour `distance <= 100 km`:

- calcul par tranches
- frais de service a `6 %`
- prix minimums par palier pour proteger la rentabilite

### Regime longue distance V4

Pour `distance > 100 km`:

- structure ultra-degressive
- mode `standard` plus agressif commercialement
- assurance et service fee gardes
- arrondi au `0.10 EUR` superieur

### Sortie du moteur

Le breakdown retourne maintenant:

- total client
- base logistique
- assurance
- frais de service
- commission plateforme
- reversement livreur
- version de pricing

## 11. Affichage des montants selon le role

L'UI n'affiche plus le meme prix pour tout le monde.

Logique actuelle:

- client: `customerTotalPrice`
- livreur: `courierPayoutAmount`

Cette regle est appliquee sur:

- carousel homepage
- detail colis
- page `Mes colis`
- resume de tournee

Le total affiche en `Direct` et `Tournee` est aligne sur cette meme regle.

## 12. Simplification du parcours de modification de compte

Le parcours de modification de compte a ete simplifie.

Le front n'affiche plus que:

- etape 1
- etape 2

Les champs modifiables sont limites a:

- telephone
- email
- adresse postale

Tous les autres champs sont grises.

Le backend met a jour uniquement:

- email
- telephone
- adresse personnelle

sans repasser par l'onboarding complet.

## 13. Homepage et carte Google Maps

Des corrections importantes ont ete faites sur la carte:

- suppression des doubles loaders
- persistance du premier trajet affiche
- affichage de la tournee globale plutot qu'un simple trajet mono-colis
- suppression des doublons de marqueurs standards en mode tournee
- affichage numerote collecte/livraison
- liaisons colorees entre collecte et livraison

## 14. Notifications et mobile push

Le projet a ajoute une base de notifications plus complete:

- enregistrement des devices mobiles
- preference de langue de notifications
- push natif si disponible
- fallback no-op si Firebase est absent

Documentation associee:

- [mobile-push-setup.md](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/docs/mobile-push-setup.md)

## 15. Scripts de test et jeu de donnees

Le projet dispose maintenant d'un script de reset et reseed cible homepage et pricing:

- [reset-and-seed-homepage-data.ps1](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/perf/reset-and-seed-homepage-data.ps1)

Ce script:

- supprime les colis de test avec adresses et reservations
- recree un dataset propre
- couvre les cas de calcul de prix
- couvre les modes de recherche homepage

Il produit aussi un rapport JSON avec:

- valeur declaree assurance
- prix calcules
- breakdown de pricing

## 16. Deploiement et verification

Les modules modifies ont ete rebuild et redeployes avec les scripts `5vm`.

Les modules les plus souvent redeployes sur ces evolutions sont:

- `packages`
- `frontend`
- ponctuellement `users`

Le chemin de reference pour ces deploiements reste:

- [FIRST-DEPLOY-5VM.md](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/FIRST-DEPLOY-5VM.md)

## 17. Limites actuelles a connaitre

- le planner reste heuristique, pas un solveur VRP complet
- la capacite poids/volume est integree de maniere conservative
- les tests backend restent encore modestes hors `quickdelivery-packages`
- les builds front gardent des warnings de taille d'assets

## 18. Fichiers pivots a connaitre

Backend:

- [PackagesService.java](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-packages/src/main/java/com/quickdelivery/services/implementations/PackagesService.java)
- [PackageController.java](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-packages/src/main/java/com/quickdelivery/controllers/PackageController.java)
- [PackageDeliveryPriceCalculator.java](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-packages/src/main/java/com/quickdelivery/helpers/PackageDeliveryPriceCalculator.java)

Frontend:

- [HomePage.vue](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-googlemaps-front/src/pages/HomePage.vue)
- [PackagesCarousel.vue](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-googlemaps-front/src/components/PackagesCarousel.vue)
- [google-maps.html](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-googlemaps-front/public/google-maps.html)
- [routePlanning.js](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-googlemaps-front/src/services/routePlanning.js)
- [ActiveRoutePage.vue](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-googlemaps-front/src/pages/ActiveRoutePage.vue)
