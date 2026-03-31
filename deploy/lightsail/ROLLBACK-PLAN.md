# QuickDelivery Rollback Plan

Le rollback doit permettre de revenir a l'infra actuelle sans improvisation.

## 1. Conditions De Declenchement

Rollback immediat si l'un des points suivants se produit apres bascule:

- taux de `5xx` durablement eleve
- login impossible
- paiement guest casse
- tracking casse pour les livraisons en cours
- erreurs DB ou saturation critique
- incident `keycloak`, `gateway` ou `mysql` non resolu en moins de `15 min`

## 2. Ce Qui Ne Doit Pas Etre Detruit Avant Stabilisation

- ancienne VM prod
- ancienne base
- ancienne configuration `nginx`
- anciens certificats
- dump final de la base avant bascule

## 3. Rollback DNS

1. remettre `app`, `api`, `auth` vers l'ancienne IP publique
2. attendre la propagation grace au TTL bas
3. verifier:
   - front
   - auth
   - API
   - paiement
   - tracking

## 4. Rollback DB

La base etant le point le plus sensible, il faut minimiser la periode d'ecriture concurrente.

Strategie recommandee:

1. basculer avec gel court des ecritures
2. si rollback necessaire, reouvrir l'ancienne prod sur sa base d'origine
3. si des ecritures ont eu lieu sur la nouvelle prod apres la bascule, les traiter comme un incident de reconciliation, pas comme un rollback silencieux

## 5. Rollback Applicatif

### Si `VM1` est en cause

- repointer DNS
- couper `VM1` nouvelle infra du trafic

### Si `VM2` est en cause

- repointer DNS
- arreter la nouvelle pile auth/config/discovery

### Si `VM3` ou `VM4` est en cause

- si `VM3` seulement: le tracking live MVP est impacte, rollback si correction immediate impossible
- si `VM4` seulement: garder la nouvelle infra ouverte seulement si la charge est acceptable sur `VM3`

### Si `VM5` est en cause

- rollback complet recommande

## 6. Verification Apres Rollback

- login admin
- creation package guest
- paiement guest
- tracking
- dashboards admin
- absence d'erreur `5xx` anormale

## 7. Duree De Conservation De L'Ancienne Infra

Ne rien decomissionner avant:

- `7 jours` de stabilite minimum
- au moins `1` campagne perf `smoke`
- au moins `1` campagne perf `nominal`
- validation metier du tracking, du paiement et du back-office
