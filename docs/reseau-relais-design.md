# QuickDelivery - Evolution Reseau Relais

## Objectif

Mettre en place un reseau de points relais QuickDelivery pour gerer les cas ou le destinataire est absent lors du passage du livreur.

Le relais devient un acteur metier complet:

- il recoit des colis deposes par les livreurs;
- il garde les colis pendant une duree limitee;
- il remet les colis aux destinataires contre validation;
- il est remunere pour les depots, retraits et eventuellement la garde prolongee;
- il est suivi dans le centre financier et dans l'administration.

## Probleme A Resoudre

Quand le destinataire est absent, le livreur ne peut pas finaliser la livraison avec l'OTP destinataire. Le colis est deja sous responsabilite du livreur et ne doit pas repasser a `NEW`.

Le workflow cible est:

```text
Destinataire absent
-> QuickDelivery propose des points relais autorises
-> Le livreur choisit un relais eligible
-> Le livreur depose le colis au relais
-> Le relais confirme la reception
-> Le destinataire recoit l'adresse, les horaires et le code de retrait
-> Le destinataire retire le colis au relais
-> Le relais confirme le retrait
-> Le colis est cloture
```

## Nouveaux Acteurs

### Compte Relais

Ajouter un nouveau type de compte:

```text
RELAY_POINT
```

Option plus complete:

```text
RELAY_MANAGER
RELAY_STAFF
```

`RELAY_MANAGER` gere l'etablissement, les informations commerciales, les horaires et les paiements.

`RELAY_STAFF` peut scanner, accepter, remettre et signaler des incidents.

### Point Relais

Un compte relais doit etre lie a une fiche etablissement:

```text
RelayPoint
id
ownerUserId
name
siret
addressLine1
addressLine2
zipCode
town
country
latitude
longitude
phone
email
openingHours
maxStorageCapacity
currentStoredPackages
maxPackageWeightKg
maxPackageVolumeCm3
active
validationStatus
suspensionReason
createdAt
updatedAt
```

Statuts de validation:

```text
PENDING_REVIEW
APPROVED
REJECTED
SUSPENDED
CLOSED
```

## Onboarding Relais

Le relais doit passer par un onboarding dedie:

1. creation du compte relais;
2. saisie de la fiche etablissement;
3. upload des documents;
4. validation admin;
5. activation du point relais;
6. configuration des horaires et capacites.

Documents utiles:

```text
SIRET / KBIS
piece identite du gerant
RIB / IBAN
justificatif adresse etablissement
contrat partenaire signe
```

L'application ne doit jamais proposer un relais non valide par l'admin.

## Statuts Colis

Statuts deja utiles ou a ajouter:

```text
RELAY_DROPOFF_REQUIRED
RELAY_SELECTED
DROPPED_AT_RELAY
READY_FOR_RECIPIENT_PICKUP
PICKED_UP_BY_RECIPIENT
RELAY_PICKUP_EXPIRED
RETURN_TO_SENDER_REQUIRED
RETURNED_TO_SENDER
```

Sens metier:

- `RELAY_DROPOFF_REQUIRED`: le destinataire est absent, un depot relais est requis.
- `RELAY_SELECTED`: un relais a ete choisi et valide par QuickDelivery.
- `DROPPED_AT_RELAY`: le livreur a depose le colis, le relais l'a accepte.
- `READY_FOR_RECIPIENT_PICKUP`: le destinataire peut retirer le colis.
- `PICKED_UP_BY_RECIPIENT`: le destinataire a retire le colis.
- `RELAY_PICKUP_EXPIRED`: la duree de garde est depassee.
- `RETURN_TO_SENDER_REQUIRED`: le colis doit repartir vers l'expediteur.
- `RETURNED_TO_SENDER`: retour finalise.

## Workflow Destinataire Absent

### 1. Signalement Par Le Livreur

Le livreur clique sur:

```text
Destinataire absent
```

Le backend valide:

- colis en `PICKEDUP` ou `INDELIVERY`;
- reservation active pour ce livreur;
- presence GPS du livreur au point de livraison;
- stop de livraison encore actif;
- absence non deja declaree.

Le colis passe a:

```text
RELAY_DROPOFF_REQUIRED
```

### 2. Recherche De Relais Eligibles

Le backend cherche les relais autour de l'adresse de livraison ou autour de la position courante du livreur.

Criteres d'eligibilite:

- relais `APPROVED`;
- relais actif;
- relais ouvert ou ouvrant bientot;
- capacite disponible;
- compatible poids/volume du colis;
- distance acceptable;
- pas suspendu;
- pas en fermeture exceptionnelle.

Tri recommande:

```text
score =
distance adresse livraison
+ distance position livreur
+ disponibilite horaire
+ capacite restante
+ fiabilite historique
+ cout relais
```

Le frontend affiche au livreur 3 a 5 relais maximum.

### 3. Selection Du Relais

Le livreur choisit un relais dans la liste.

Le backend revalide:

- le relais existe;
- il est toujours actif;
- il est compatible;
- il est toujours dans le rayon autorise;
- il a encore de la capacite.

Le colis passe a:

```text
RELAY_SELECTED
```

Donnees a stocker:

```text
selectedRelayPointId
relaySelectedAt
relaySelectionSource
relayExpectedDropoffDeadline
```

### 4. Depot Au Relais

Le livreur arrive au relais et scanne l'etiquette colis.

Le relais scanne aussi l'etiquette ou valide depuis son dashboard.

Conditions de validation:

- colis attendu dans ce relais;
- colis dans le bon statut;
- livreur associe a la reservation;
- presence GPS du livreur au relais;
- relais connecte avec un compte autorise;
- photo preuve optionnelle;
- signature ou confirmation relais.

Le colis passe a:

```text
DROPPED_AT_RELAY
READY_FOR_RECIPIENT_PICKUP
```

Le livreur n'est plus responsable du colis apres acceptation relais.

### 5. Retrait Par Le Destinataire

Le destinataire recoit:

```text
adresse relais
horaires
date limite de retrait
code de retrait
QR code de retrait
```

Le relais remet le colis uniquement si:

- le code de retrait est valide;
- le colis est bien dans son stock;
- le retrait n'est pas expire;
- l'identite est controlee si la politique l'impose.

Le colis passe a:

```text
PICKED_UP_BY_RECIPIENT
DELIVERED
```

## Actions Relais

Le dashboard relais doit permettre:

```text
Scanner une etiquette
Confirmer depot livreur
Refuser depot
Voir colis attendus
Voir colis en stock
Confirmer retrait destinataire
Signaler incident
Voir revenus
Voir paiements
Modifier horaires
Declarer fermeture exceptionnelle
```

Actions sur colis:

```text
ACCEPT_RELAY_DROPOFF
REJECT_RELAY_DROPOFF
CONFIRM_RECIPIENT_PICKUP
REPORT_DAMAGED_PACKAGE
REPORT_MISSING_PACKAGE
REPORT_INVALID_PICKUP_CODE
REQUEST_SUPPORT
```

## Dashboard Relais

Sections recommandees:

```text
Aujourd'hui
Colis attendus
Colis en stock
Retraits a effectuer
Colis expires
Incidents
Capacite restante
Revenus du mois
Paiements
Parametres du relais
```

Indicateurs:

```text
nombre de colis en stock
nombre de depots du jour
nombre de retraits du jour
revenu estime
revenu confirme
incidents ouverts
taux de retrait
duree moyenne de garde
```

## Centre Financier

Le relais doit etre integre au centre financier.

Nouvelles notions:

```text
RelayCommission
RelayPayout
RelayInvoice
RelayPenalty
```

Modele simple de remuneration:

```text
commission depot accepte: 0.50 EUR
commission retrait confirme: 0.50 EUR
frais garde prolongee: 0.20 EUR / jour apres 3 jours
```

Ces montants doivent etre configurables.

Exemple de configuration:

```properties
quickdelivery.relay.commission.dropoff-accepted=0.50
quickdelivery.relay.commission.recipient-pickup=0.50
quickdelivery.relay.storage.free-days=3
quickdelivery.relay.storage.daily-fee=0.20
quickdelivery.relay.currency=EUR
```

Etats payout:

```text
PENDING
READY
PAID
FAILED
CANCELLED
```

Le centre financier admin doit afficher:

- revenus livreurs;
- penalites livreurs;
- penalites utilisateurs;
- commissions relais;
- payouts relais;
- incidents financiers.

## Frais Client

En cas d'absence destinataire, QuickDelivery peut facturer des frais:

```text
frais absence destinataire
frais depot relais
frais garde prolongee
frais retour expediteur
```

Regle recommandee:

```text
Absence destinataire -> frais depot relais factures selon CGV.
Absence expediteur -> penalite expediteur.
Non retrait apres delai -> frais garde ou retour expediteur.
```

## Securite

Controles obligatoires:

- authentification du compte relais;
- roles `RELAY_MANAGER` / `RELAY_STAFF`;
- scan etiquette obligatoire;
- code retrait obligatoire;
- audit log sur chaque action;
- preuve photo configurable;
- validation GPS pour depot relais;
- impossibilite pour le livreur de saisir une adresse libre;
- impossibilite pour un relais non valide de recevoir un colis;
- limitation des actions au stock du relais connecte.

Evenements a historiser:

```text
RECIPIENT_ABSENT_REPORTED
RELAY_OPTIONS_GENERATED
RELAY_SELECTED
RELAY_DROPOFF_ACCEPTED
RELAY_DROPOFF_REJECTED
RECIPIENT_PICKUP_CONFIRMED
RELAY_INCIDENT_REPORTED
RELAY_PICKUP_EXPIRED
RETURN_TO_SENDER_TRIGGERED
```

## Notifications

Destinataire:

- colis depose en relais;
- adresse et horaires;
- code ou QR de retrait;
- rappel avant expiration;
- expiration et retour.

Expediteur:

- destinataire absent;
- depot relais en cours;
- depot relais confirme;
- retrait effectue;
- retour si non retrait.

Livreur:

- relais selectionne;
- trajet vers relais;
- depot confirme;
- depot refuse.

Relais:

- colis attendu;
- colis depose;
- retrait effectue;
- incident;
- payout disponible.

Admin:

- nouveau relais a valider;
- incident relais critique;
- colis expire;
- litige.

## API A Prevoir

Backend packages:

```text
GET  /packages/v1/relay-points/search?packageID=&deliveryPersonID=&latitude=&longitude=
PUT  /packages/v1/relay-points/select
PUT  /packages/v1/relay-dropoff/confirm
PUT  /packages/v1/relay-pickup/confirm
POST /packages/v1/relay-incidents
```

Backend users:

```text
POST /users/v1/relay/register
PUT  /users/v1/relay/profile
GET  /users/v1/relay/profile
POST /users/v1/admin/relay/{relayId}/approve
POST /users/v1/admin/relay/{relayId}/suspend
```

Admin:

```text
GET /packages/v1/admin/relay-points
GET /packages/v1/admin/relay-stock
GET /packages/v1/admin/relay-incidents
GET /packages/v1/admin/relay-finance
```

## Entites A Ajouter

Minimum:

```text
RelayPoint
RelayPointOpeningHours
RelayPackageAssignment
RelayPackageEvent
RelayCommission
RelayPayout
RelayIncident
```

Optionnel mais utile:

```text
RelayStaffMembership
RelayCapacitySnapshot
RelayClosurePeriod
RelayContract
RelayPickupCode
```

## Impact Frontend

Pages a ajouter:

```text
/relay/onboarding
/relay/dashboard
/relay/scan
/relay/stock
/relay/finance
/relay/incidents
/admin/relay-points
/admin/relay-finance
```

Composants:

```text
RelayPointSelector
RelayScanPanel
RelayPackageStockList
RelayFinanceSummary
RelayIncidentForm
```

## Impact Mobile

Le scan peut etre fait avec:

- camera mobile via Capacitor;
- saisie manuelle tracking number en fallback;
- QR code deja present sur les etiquettes colis.

Le fallback manuel est obligatoire si la camera echoue.

## MVP Recommande

Phase 1:

```text
RELAY_POINT comme nouveau type de compte
RelayPoint avec validation admin
Recherche relais autour de l'adresse livraison
Selection relais par livreur
Depot relais avec scan + confirmation relais
Code retrait destinataire
Retrait destinataire avec code
Commission simple depot + retrait
Dashboard relais minimal
```

Phase 2:

```text
staff multi-utilisateurs
horaires avances
fermetures exceptionnelles
incidents complets
payout relais
facturation frais absence
retour expediteur automatique
preuves photo/signature
```

Phase 3:

```text
optimisation selection relais
score fiabilite relais
limites capacite dynamiques
integration comptable
contrats numeriques
statistiques admin avancees
```

## Questions A Trancher

- Combien de jours un colis peut rester en relais?
- Qui paie les frais relais en cas d'absence destinataire?
- Quel montant de commission relais?
- Le relais peut-il refuser un colis apres selection?
- Quelles tailles/poids maximum sont acceptes?
- Le retrait necessite-t-il une piece d'identite?
- Que faire si le destinataire ne retire pas le colis?
- Qui arbitre un litige relais/livreur/destinataire?
- Quelle preuve est obligatoire au depot?
- Quelle preuve est obligatoire au retrait?

## Regle Produit Centrale

Le relais ne doit jamais etre une adresse libre saisie par le livreur.

QuickDelivery doit proposer et valider uniquement des relais internes:

```text
relais approuve
ouvert
compatible
capacite disponible
autorise par l'admin
trace dans la finance
```

