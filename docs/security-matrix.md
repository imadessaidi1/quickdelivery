# QuickDelivery - Security Matrix

## Roles

- `ROLE_CLIENT`
- `ROLE_CLIENT_PRO`
- `ROLE_LIVREUR`
- `ROLE_ADMIN`

## Front Navigation Matrix

| UI Area | Client | Livreur | Admin |
|---|---|---|---|
| Menu `Home` (`/`) | No | Yes | Yes |
| Menu `Mes packages` (`/myPackages`) | Yes | Yes | Yes |
| Menu `Créer package` (`/createPackage`) | Yes | No | Yes |
| Compte `Mon compte` (`/userAccount`) | Yes | Yes | Yes |
| Compte `Se déconnecter` | Yes | Yes | Yes |
| Compte `Créer compte` (`/userSignInPage`) | No | No | Yes |
| Compte `Tracking` (`/packageTracking`) | No | No | Yes |
| Compte `Validation comptes` (`/usersAccountValidation`) | No | No | Yes |

## Front Route Guards (implemented)

| Route | Allowed Roles |
|---|---|
| `/` | `ROLE_LIVREUR`, `ROLE_ADMIN` |
| `/createPackage` | `ROLE_CLIENT`, `ROLE_CLIENT_PRO`, `ROLE_ADMIN` |
| `/myPackages` | `ROLE_CLIENT`, `ROLE_CLIENT_PRO`, `ROLE_LIVREUR`, `ROLE_ADMIN` |
| `/paymentPage` | `ROLE_CLIENT`, `ROLE_CLIENT_PRO`, `ROLE_ADMIN` |
| `/userSignInPage` | `ROLE_ADMIN` |
| `/packageTracking` | `ROLE_ADMIN` |
| `/usersAccountValidation` | `ROLE_ADMIN` |
| `/userAccount` | `ROLE_CLIENT`, `ROLE_CLIENT_PRO`, `ROLE_LIVREUR`, `ROLE_ADMIN` |
| `/package` | `ROLE_CLIENT`, `ROLE_CLIENT_PRO`, `ROLE_LIVREUR`, `ROLE_ADMIN` |

Behavior:
- If token is missing/expired: redirect to Keycloak login.
- If role is insufficient: redirect to role landing page.

## Gateway API Authorization Matrix

Source: `quickdelivery-api-gateway/src/main/java/com/quickdelivery/ResourceServerSecurityConfig.java`

### Public

- `POST /users/v1/create`
- `GET /users/v1/validateEmail**`
- `GET /packages/v1/getPackage**`
- `GET /packages/v1/packages-around-address**`
- `GET /actuator/health`
- `GET /actuator/info`

### Client / Client Pro / Admin

- `POST /packages/v1/create`
- `PUT /packages/v1/update-packages-status`
- `GET /packages/v1/getPackagesByDeliveryPerson**`

### Client Pro / Admin

- `POST /packages/v1/bulk-create`

### Livreur / Admin

- `PUT /packages/v1/reserve**`
- `PUT /packages/v1/reserve-batch**`
- `PUT /packages/v1/pickup**`
- `PUT /packages/v1/deliver**`
- `GET /packages/v1/checkOTPForPickup**`
- `GET /packages/v1/checkOTPForDelivery**`
- `GET /packages/v1/packages-around**`
- `GET /packages/v1/packages-around-me**`
- `GET /packages/v1/packages-on-my-road**`

### Admin

- `GET/PUT /users/v1/usersForValidation`
- `PUT /users/v1/validateUser`

### Authenticated fallback

- Any remaining `/users/v1/**`, `/packages/v1/**`, `/ws/**` requires valid JWT.

## UI Action Matrix (buttons)

| Action | Client | Livreur | Admin |
|---|---|---|---|
| Reserve package | No | Yes | Yes |
| Reserve selected + on my road | No | Yes | Yes |
| Pickup package | No | Yes | Yes |
| Deliver package | No | Yes | Yes |
| Create package wizard | Yes | No | Yes |
| Validate user account | No | No | Yes |
| Update own profile | Yes | Yes | Yes |

## Notes

- Admin self-registration is disabled by navigation and access policy.
- `bulk-create` remains restricted to `ROLE_CLIENT_PRO` and `ROLE_ADMIN`.
- Direct backend access is blocked by service-level gateway filters; calls must pass through API Gateway.
- On `On my road`, the map highlights only packages that are geometrically on the delivery corridor of the selected package.
