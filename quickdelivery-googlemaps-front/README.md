# quickdelivery-googlemaps-front

Front web Vue 3 de QuickDelivery.

## Installation
```bash
npm install
```

## Scripts principaux

### Développement
```bash
npm run serve
```

### Build
```bash
npm run build
```

### Lint
```bash
npm run lint
```

### Capacitor
```bash
npm run cap:sync
npm run cap:sync:all
npm run cap:sync:android
npm run cap:sync:ios
npm run cap:open:android
npm run cap:open:ios
```

La documentation d'execution et de test mobile a ete deplacee dans le [README racine](/C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/README.md).

## Référence Vue CLI
Voir [Configuration Reference](https://cli.vuejs.org/config/).
# Environment variables

Copy one of these templates to a real local env file depending on the target environment:

- `.env.development.local.example` -> `.env.development.local`
- `.env.production.example` -> `.env.production`

Variables supported by the front:

- `VUE_APP_GATEWAY_BASE_URL`
- `VUE_APP_AUTH_BASE_URL`
- `VUE_APP_WS_BASE_URL`

If these variables are not set, the app falls back to the current browser hostname with the default dev ports:

- gateway: `https://<host>:8443`
- auth: `https://<host>:18443/auth`
- websocket: `wss://<host>:8443/ws`
