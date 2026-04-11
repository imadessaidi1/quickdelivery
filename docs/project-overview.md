# QuickDelivery Project Overview

## Functional scope

Detailed recent feature documentation:

- `docs/nouveautes-projet.md`

QuickDelivery is a delivery platform composed of:

- a Vue 3 frontend with Google Maps integration and Capacitor mobile wrappers
- a Spring Cloud microservice backend
- an embedded OAuth/Keycloak authorization server
- deployment automation for Docker on AWS Lightsail
- performance test suites based on `k6`

## Solution architecture

## Main runtime components

- `quickdemivery-config-server`: Spring Cloud Config Server
- `quickdelivery-registy-server`: Eureka discovery server
- `oauth-authorization-server`: OAuth2/OIDC auth server exposed under `/auth`
- `quickdelivery-api-gateway`: Spring Cloud Gateway, JWT resource server, load balancer
- `quickdelivery-users`: user/account domain service
- `quickdelivery-packages`: package/delivery domain service, websocket tracking
- `quickdelivery-abstarct-dao`: shared DTO/entity/repository library
- `quickdelivery-googlemaps-front`: Vue 3 web frontend + Capacitor wrappers
- `quickdelivery-batches`: placeholder module, currently almost empty

## Repository shape

The Maven parent currently builds these modules:

- `quickdemivery-config-server`
- `quickdelivery-registy-server`
- `oauth-authorization-server`
- `quickdelivery-api-gateway`
- `quickdelivery-users`
- `quickdelivery-packages`
- `quickdelivery-abstarct-dao`
- `quickdelivery-batches`

Important mismatch:

- `quickdelivery-shipment/` exists in the repository but is not declared in the parent `pom.xml`, so it is not part of the aggregated build.

## Runtime flow

Standard request flow:

1. frontend calls the public API through the gateway only
2. gateway validates JWT and routes to backend services through Eureka
3. services read central configuration from Config Server
4. `users` and `packages` use MySQL
5. auth is handled by the OAuth/Keycloak server
6. realtime tracking uses websocket traffic through `/ws`

## Infra architecture

## Local development

Documented local startup order:

1. config server on `8889`
2. registry server on `8080`
3. auth server on `18443`
4. users service on `8081`
5. packages service on `8082`
6. API gateway on `8443`
7. frontend on `8084`

Local HTTPS/TLS development relies on:

- `certs/quickdelivery-dev.p12`
- service-specific truststore environment variables

The frontend can auto-derive backend/auth/websocket URLs from the browser hostname when env overrides are absent.

## Production path documented in the repo

Two infra shapes are documented:

- current/recent MVP: backend Dockerized on a Lightsail VM, frontend exposed by host Nginx
- target/recommended MVP: 5-VM Lightsail topology

### Current validated AWS/Lightsail direction

- host Nginx exposes `app`, `api`, `auth`
- backend services run in Docker
- artifacts are prepared locally and copied to the VM
- only `artifacts/`, `config/` and `deploy/` are required on the target VM for backend runtime

### Target 5-VM topology

- `VM1`: front + nginx + api-gateway
- `VM2`: keycloak + config-server + discovery-server + redis
- `VM3`: users-service + packages-service
- `VM4`: users-service + packages-service
- `VM5`: mysql
- `S3`: durable object/document storage

Operational rules already documented:

- public TLS terminates on `VM1`
- business HTTP load balancing is handled by gateway + Eureka, not by Nginx
- websocket tracking stays pinned to `VM3` for the MVP
- MySQL must remain private

## Deployment scripts

## Artifact preparation

- `deploy/docker/prepare-artifacts.ps1`

Purpose:

- builds selected Maven modules
- copies runtime jars into `artifacts/`
- supports full build or module-scoped packaging

## Docker deployment assets

- `deploy/docker/docker-compose.production.yml`
- `deploy/docker/docker-compose.lightsail.yml`
- `deploy/docker/5vm/docker-compose.vm*.yml`

Purpose:

- localize the runtime topology for 1-VM and 5-VM targets
- mount `config/`
- run runtime jars from `artifacts/`

## Release automation

Desktop-side scripts:

- `deploy/release/package-and-upload.ps1`
- `deploy/release/package-and-upload-5vm.ps1`
- `deploy/release/update-deploy.ps1`
- `deploy/release/update-deploy-5vm.ps1`

VM-side scripts:

- `deploy/release/release-on-vm.sh`
- `deploy/release/release-on-vm-5vm.sh`
- `deploy/release/verify-release.sh`
- `deploy/release/verify-release-5vm.sh`

Key release modes:

- `full`
- `backend`
- `frontend`
- `module`

The Linux release script handles:

- swap creation if missing
- backend restart with Docker Compose
- optional module-only restarts
- frontend installation under `/var/www/quickdelivery-front`
- bootstrap Nginx config
- certificate issuance with Certbot
- final Nginx config activation

## Testing strategy

## Java tests

Current repository state:

- almost no backend tests outside `quickdelivery-packages`
- detected tests:
  - `PackageDeliveryPriceCalculatorTest`
  - `PackagesServiceValidationTest`

Observed status on `mvn -pl quickdelivery-packages test`:

- the test suite currently fails
- failures indicate contract drift with `quickdelivery-abstarct-dao`
- examples:
  - missing `PackageDTO.setDeliverySpeed(String)`
  - missing class `com.quickdelivery.abstarct.parameters.COURIER_PAYOUT_STATUS`

This means the current test baseline is broken and should not be treated as trustworthy CI protection.

## Performance tests

The `perf/` folder contains a much stronger operational test kit than the unit/integration layer:

- smoke
- regression
- nominal
- stress
- crash
- frontdoor isolation
- packages write-heavy isolation
- DB pressure isolation
- targeted scenarios for onboarding, delivery, tracking and seeding data

Execution entrypoint:

- `perf/run.ps1`

Notable behavior:

- loads env from `.env.local`, `.env`, then `.env.example`
- can refresh tokens automatically through `perf/init-env.ps1`
- uses `k6`

## Security and access control

Security documentation already exists in:

- `docs/security-matrix.md`

Documented controls:

- role-based frontend route guards
- gateway-level authorization matrix
- direct backend access blocked in favor of gateway-only access

## Project risks and gaps to keep in mind

- `quickdelivery-shipment` is present but outside the parent build
- test coverage is very limited
- existing tests are currently broken by shared-model drift
- secrets appear documented in README examples and should be treated carefully before any wider sharing
- naming inconsistencies exist in module names, for example `quickdemivery` and `registy`

## Skills useful for future work

Already available in this Codex session:

- `frontend-skill`
- `playwright`
- `security-best-practices`
- `security-threat-model`

Additionally installed during this review:

- `doc`

Practical usage:

- use `frontend-skill` for major UI work on `quickdelivery-googlemaps-front`
- use `playwright` for browser flows, screenshots and auth/UI debugging
- use `security-best-practices` or `security-threat-model` only for explicit AppSec work
- use `doc` when updating or restructuring README/runbook content
