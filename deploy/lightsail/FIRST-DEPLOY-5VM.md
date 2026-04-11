# QuickDelivery First Deploy 5 VM

Ce guide donne l'ordre exact de premier deploiement pour la nouvelle architecture.

## Inventaire Des VMs

- `qd-vm1-gateway`
  - IP publique: `15.224.36.99`
  - IP privee: `172.26.1.114`
- `qd-vm2-platform`
  - IP publique: `35.181.210.112`
  - IP privee: `172.26.4.12`
- `qd-vm3-app`
  - IP publique: `15.224.39.34`
  - IP privee: `172.26.1.120`
- `qd-vm4-app`
  - IP publique: `13.36.9.2`
  - IP privee: `172.26.12.25`
- `qd-vm5-mysql`
  - IP publique: `15.188.208.12`
  - IP privee: `172.26.10.139`

## 1. Remplir Les Fichiers `.env`

Completer localement:

- `deploy/lightsail/vm5-mysql.env`
- `deploy/lightsail/vm2-platform.env`
- `deploy/lightsail/vm3-app.env`
- `deploy/lightsail/vm4-app.env`
- `deploy/lightsail/vm1-gateway.env`

Ces fichiers sont ignores par Git.

## 2. Ordre De Deploiement

1. `VM5 mysql`
2. `VM2 platform`
3. `VM3 app`
4. `VM4 app`
5. `VM1 gateway`

## 3. Commandes

### VM5

```powershell
powershell -ExecutionPolicy Bypass -File deploy/release/update-deploy-5vm.ps1 -Role vm5-mysql -ServerIp <VM5_PUBLIC_IP> -Mode backend
```

### VM2

```powershell
powershell -ExecutionPolicy Bypass -File deploy/release/update-deploy-5vm.ps1 -Role vm2-platform -ServerIp <VM2_PUBLIC_IP> -Mode backend
```

### VM3

```powershell
powershell -ExecutionPolicy Bypass -File deploy/release/update-deploy-5vm.ps1 -Role vm3-app -ServerIp <VM3_PUBLIC_IP> -Mode backend
```

### VM4

```powershell
powershell -ExecutionPolicy Bypass -File deploy/release/update-deploy-5vm.ps1 -Role vm4-app -ServerIp <VM4_PUBLIC_IP> -Mode backend
```

### VM1

```powershell
powershell -ExecutionPolicy Bypass -File deploy/release/update-deploy-5vm.ps1 -Role vm1-gateway -ServerIp <VM1_PUBLIC_IP> -Mode full
```

## 4. Verification Apres Chaque Role

### VM5

- container `mysql` `UP`
- connexion MySQL possible

### VM2

- `config-server` `UP`
- `discovery-server` `UP`
- `oauth` `UP`

### VM3 / VM4

- `users-service` `UP`
- `packages-service` `UP`
- visible dans Eureka

### VM1

- `api-gateway` `UP`
- front servi par `nginx`
- `api` accessible
- `auth` accessible via reverse proxy

## 5. Recette Privee Minimum Avant DNS

- login admin
- login livreur
- create package
- guest payment
- tracking live
- dashboard finance

## 6. Puis

Une fois cette sequence passee:

1. executer `perf smoke`
2. executer `perf nominal`
3. preparer la bascule DNS
