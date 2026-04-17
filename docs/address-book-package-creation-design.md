# Evolution creation colis: carnet d'adresses client

## Objectif

Dans le flux de creation de colis connecte, un client doit pouvoir reutiliser des destinataires deja connus depuis un carnet d'adresses personnel.

Sur l'etape `Adresse de livraison`:

- le client cherche un destinataire par nom, prenom ou adresse email;
- si un destinataire existe, le formulaire de livraison est auto-rempli;
- si aucun destinataire n'est selectionne, une case `Ajouter a mon carnet d'adresses` permet d'enregistrer les informations saisies;
- le bouton `Suivant` de l'etape livraison valide l'adresse puis cree le destinataire dans le carnet si la case est cochee.

Le flux invite/anonyme reste inchange et ne voit pas le carnet d'adresses.

## Etat actuel

Frontend:

- `quickdelivery-googlemaps-front/src/pages/CreatePackagePage.vue` pilote le wizard.
- L'etape 2 rend `PackageAddress` avec `addressType="ARRIVAL"`.
- `validateAndStoreAddress(..., 'ARRIVAL', true)` valide l'adresse et avance a l'etape colis.
- `PackageAddress.vue` edite directement `store.state.package_.addresses[1]`.

Backend:

- `AddressDTO` contient deja les champs utiles: `firstName`, `lastName`, `email`, `phone`, `line1`, `line2`, `town`, `zipCode`, `country`, `floor`, `hasElevator`, `latitude`, `longitude`, `addressAuto`.
- L'entite `Address` est deja liee soit a un colis (`package_id`), soit a un utilisateur via `residents_user_id`.
- `residents_user_id` est aujourd'hui utilise pour l'adresse personnelle du compte. Le reutiliser tel quel pour un carnet de destinataires melangerait deux concepts metier.

## Decision de conception

Creer un modele dedie `CustomerAddressBookEntry`.

Raison:

- le carnet contient des destinataires, pas l'adresse personnelle du client;
- les adresses de colis doivent rester des snapshots independants, car une modification ulterieure du carnet ne doit pas modifier les anciens colis;
- les contraintes d'un carnet sont differentes: recherche, unicite par client, date de derniere utilisation, archivage eventuel.

## Modele de donnees

Nouvelle entite dans `quickdelivery-abstarct-dao`:

```java
CustomerAddressBookEntry
```

Champs:

- `id`, `version`
- `owner: User` avec colonne `owner_user_id`, obligatoire
- `firstName`, `lastName`, `email`, `phone`
- `line1`, `line2`, `town`, `zipCode`, `country`
- `floor`, `hasElevator`
- `latitude`, `longitude`
- `addressAuto`
- `createdAt`, `updatedAt`, `lastUsedAt`
- `active`

Index conseilles:

- `idx_address_book_owner_updated_at(owner_user_id, updated_at)`
- `idx_address_book_owner_email(owner_user_id, email)`
- `idx_address_book_owner_name(owner_user_id, last_name, first_name)`

Unicite souple:

- ne pas bloquer par contrainte SQL sur email seule, car un meme email peut avoir plusieurs lieux de livraison;
- cote service, detecter un doublon par `owner + normalizedEmail + normalizedAddressSignature`;
- si doublon detecte lors de l'ajout depuis le wizard, mettre a jour l'entree existante et `lastUsedAt` au lieu de creer une ligne.

## DTOs

Ajouter dans `quickdelivery-abstarct-dao/src/main/java/com/quickdelivery/abstarct/dto`:

```java
AddressBookEntryDTO
```

Champs proches de `AddressDTO`, sans `dateTime` ni `type`, plus:

- `id`
- `ownerUserId`
- `displayLabel`
- `lastUsedAt`
- `active`

Ajouter eventuellement:

```java
AddressBookSearchResultDTO
```

Si on veut optimiser la liste de suggestions. Sinon `AddressBookEntryDTO` suffit.

## API backend

Service conseille: `quickdelivery-users`, car le carnet appartient au client, pas au colis.

Controller:

```http
GET /users/v1/address-book?ownerUserId={id}&q={query}&limit=10
POST /users/v1/address-book?ownerUserId={id}
PUT /users/v1/address-book/{entryId}?ownerUserId={id}
DELETE /users/v1/address-book/{entryId}?ownerUserId={id}
POST /users/v1/address-book/{entryId}/touch?ownerUserId={id}
```

Pour le besoin wizard, seuls `GET` et `POST` sont obligatoires au premier lot.

### Recherche

`GET /address-book`:

- accessible uniquement a l'utilisateur connecte proprietaire du carnet;
- recherche case-insensitive sur:
  - `firstName`
  - `lastName`
  - `email`
  - `line1`
  - `town`
  - `zipCode`
  - `addressAuto`
- `q` declenche a partir de 2 caracteres;
- retour limite a 10 resultats, tries par `lastUsedAt desc`, puis `updatedAt desc`.

Exemple de reponse:

```json
[
  {
    "id": 42,
    "ownerUserId": 102,
    "firstName": "Nora",
    "lastName": "Martin",
    "email": "nora@example.com",
    "phone": "+33600000000",
    "addressAuto": "10 Rue de Rivoli, 75001 Paris, France",
    "line1": "10 Rue de Rivoli",
    "zipCode": "75001",
    "town": "Paris",
    "country": "France",
    "floor": 2,
    "hasElevator": true,
    "latitude": 48.8566,
    "longitude": 2.3522
  }
]
```

### Creation depuis le wizard

`POST /address-book?ownerUserId={id}`:

- valide les champs obligatoires: prenom, nom, email, telephone, adresse;
- normalise email et signature adresse;
- geocode si les coordonnees sont absentes;
- cree ou met a jour l'entree existante;
- met `lastUsedAt=now`;
- retourne l'entree sauvegardee.

Payload:

```json
{
  "firstName": "Nora",
  "lastName": "Martin",
  "email": "nora@example.com",
  "phone": "+33600000000",
  "addressAuto": "10 Rue de Rivoli, 75001 Paris, France",
  "line1": "10 Rue de Rivoli",
  "zipCode": "75001",
  "town": "Paris",
  "country": "France",
  "floor": 2,
  "hasElevator": true,
  "latitude": 48.8566,
  "longitude": 2.3522
}
```

## Integration frontend

### Composant

Extraire la recherche carnet dans un composant dedie:

```text
src/components/AddressBookRecipientSearch.vue
```

Responsabilites:

- affiche un champ `Rechercher dans mon carnet d'adresses`;
- debounce de 250 a 300 ms;
- appelle `GET userRootURL + address-book`;
- affiche les suggestions avec nom, email et adresse compacte;
- emet `recipient-selected` avec l'entree choisie;
- gere etat vide: `Aucun destinataire trouve`.

### PackageAddress.vue

Ajouter des props:

```js
enableAddressBook: Boolean
showAddToAddressBook: Boolean
```

Ajouter des emits:

```js
recipient-selected
update:addToAddressBook
```

Sur `addressType === 'ARRIVAL'` et utilisateur connecte:

- afficher `AddressBookRecipientSearch` au-dessus des champs destinataire;
- quand une entree est selectionnee:
  - copier les champs dans `address`;
  - appeler `syncAddressAutocomplete()`;
  - masquer ou decocher `Ajouter a mon carnet d'adresses`, car l'entree existe deja;
- si le client modifie ensuite email/adresse de facon significative, remettre l'etat en mode saisie manuelle et reautoriser la case.

Case a cocher:

- visible seulement en mode connecte, et seulement sur l'etape livraison;
- visible quand aucune entree existante n'est selectionnee;
- libelle: `Ajouter ce destinataire a mon carnet d'adresses`.

### CreatePackagePage.vue

Etat a ajouter:

```js
selectedAddressBookEntryId: null,
addArrivalRecipientToAddressBook: false,
```

Dans l'etape 2:

```vue
<PackageAddress
  ref="arrivalAddress"
  addressType="ARRIVAL"
  :enable-address-book="isAuthenticated"
  :show-add-to-address-book="isAuthenticated && !selectedAddressBookEntryId"
  v-model:addToAddressBook="addArrivalRecipientToAddressBook"
  @recipient-selected="handleAddressBookRecipientSelected"
/>
```

Modification de `validateAndStoreAddress`:

- garder les validations existantes;
- apres `ensureAddressCoordinates(address)`;
- si `type === 'ARRIVAL' && isAuthenticated && addArrivalRecipientToAddressBook && !selectedAddressBookEntryId`:
  - appeler `POST userRootURL + address-book?ownerUserId=connectedUser.id`;
  - attendre la reponse avant d'avancer;
  - si succes, memoriser `selectedAddressBookEntryId`;
  - si erreur, rester sur l'etape et afficher un message coherent;
- ensuite seulement `updatePackageArrivalAddress` et `currentStep += 1`.

Important: la creation dans le carnet ne doit jamais remplacer la creation du colis. Elle est une action annexe de l'etape livraison.

## Flux utilisateur cible

1. Client connecte arrive a l'etape livraison.
2. Il saisit `no`, `nora`, ou `nora@example.com` dans la recherche carnet.
3. Si un resultat existe:
   - il clique dessus;
   - le formulaire livraison est auto-rempli;
   - `Suivant` valide l'adresse et passe a l'etape colis.
4. Si aucun resultat ne correspond:
   - il remplit les champs manuellement;
   - il coche `Ajouter ce destinataire a mon carnet d'adresses`;
   - `Suivant` valide l'adresse, cree ou met a jour l'entree carnet, puis passe a l'etape colis.
5. Invite/anonyme:
   - pas de recherche carnet;
   - pas de case d'ajout.

## Regles metier

- Le carnet est strictement personnel au client connecte.
- Un utilisateur ne peut pas lire ou modifier le carnet d'un autre utilisateur.
- Les adresses colis restent des copies; elles ne gardent pas de relation obligatoire vers le carnet.
- Une entree carnet selectionnee peut etre modifiee dans le formulaire pour le colis courant sans modifier automatiquement le carnet.
- La creation carnet au clic `Suivant` doit etre idempotente: si le meme destinataire/adresse existe, mettre a jour plutot que dupliquer.
- Le champ email reste obligatoire pour la recherche fiable et la detection de doublon.
- Les coordonnees `latitude/longitude` doivent etre conservees si disponibles; sinon le backend peut geocoder.

## Securite et validation

- Ne pas faire confiance a `ownerUserId` seul: verifier qu'il correspond au user authentifie ou au claim utilisateur deja utilise par la gateway.
- Limiter `limit` a 20 maximum cote backend.
- Ne jamais retourner le carnet complet sans filtre au-dela d'une limite raisonnable.
- Sanitizer/normaliser email, telephone et champs texte.
- Journaliser les creations/mises a jour carnet sans exposer les donnees sensibles completes.

## Decoupage d'implementation

Lot 1 backend:

- ajouter `CustomerAddressBookEntry`;
- ajouter repository;
- ajouter DTO;
- ajouter service `AddressBookService`;
- ajouter endpoints `GET` et `POST`;
- ajouter tests service pour recherche, creation, doublon, ownership.

Lot 2 frontend:

- ajouter les cles i18n FR/EN;
- ajouter `AddressBookRecipientSearch.vue`;
- enrichir `PackageAddress.vue`;
- enrichir `CreatePackagePage.vue` avec creation au clic `Suivant`;
- tester lint/build.

Lot 3 UX/admin optionnel:

- page `Mon carnet d'adresses` dans `Mon compte`;
- edition/suppression d'un destinataire;
- bouton `Enregistrer les modifications dans mon carnet` quand un destinataire selectionne est modifie.

## Points d'attention

- Le front actuel stocke le package dans Vuex pendant le wizard. Le choix carnet doit rester dans l'etat de page, pas dans `package_.addresses`, sauf les champs adresse eux-memes.
- Ne pas reutiliser `User.personalAddress` comme carnet: ce champ sert au profil et a la pre-remplissage de l'adresse de depart.
- La creation carnet doit se faire avant `currentStep += 1`, sinon l'utilisateur ne voit pas l'erreur d'enregistrement.
- Si l'appel carnet echoue mais l'adresse colis est valide, il faut choisir une strategie produit:
  - recommande: bloquer uniquement si la case est cochee, avec message `Impossible d'ajouter ce destinataire au carnet d'adresses. Decochez l'option ou reessayez.`
  - alternative: avancer quand meme et afficher un warning non bloquant.

