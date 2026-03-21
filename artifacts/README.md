# QuickDelivery Runtime Artifacts

Ce dossier contient les `jar` de runtime utilises par la stack Docker orientee deploiement.

Fichiers attendus:

- `quickdelivery-config-server.jar`
- `quickdelivery-registry-server.jar`
- `oauth-authorization-server.jar`
- `quickdelivery-api-gateway.jar`
- `quickdelivery-users.jar`
- `quickdelivery-packages.jar`

Generation locale:

```powershell
powershell -ExecutionPolicy Bypass -File deploy/docker/prepare-artifacts.ps1
```

Ensuite, pour un deploiement VM sans code source, copier au minimum:

- `artifacts/`
- `config/`
- `deploy/`

vers la machine cible.

