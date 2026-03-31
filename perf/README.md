# QuickDelivery Performance Test Kit

This folder contains a ready-to-run `k6` performance test kit for QuickDelivery.

## Contents

- `suites/`
  - `smoke.js`: fast end-to-end validation of account onboarding, delivery and admin flows
  - `nominal.js`: baseline load close to expected daily usage with end-to-end account and package flows
  - `stress.js`: push the platform with end-to-end scenarios until latency and errors degrade
  - `regression.js`: short post-deploy integrity and non-regression suite
- `scenarios/`
  - `guest-checkout.js`: estimate, create, confirm guest payment, consult package
  - `customer-onboarding.js`: create a customer account then verify it from admin
  - `courier-onboarding.js`: create a courier with vehicle/documents then validate it from admin
  - `e2e-delivery.js`: estimate, create, pay, reserve, pickup, track and deliver a package
  - `admin-dashboards.js`: admin metrics and finance dashboards
  - `courier-lifecycle.js`: reserve, pickup, deliver on prepared data
  - `tracking-live.js`: live tracking with websocket subscription and HTTP position updates
  - `seed-visible-packages.js`: create paid `NEW` packages around Limeil-Brevannes for map inspection
- `lib/`
  - shared config, thresholds, payload builders, helpers
- `.env.example`
  - environment variables to copy before running
- `load-plan.md`
  - load plan, SLOs and execution order

## Prerequisites

Install `k6`:

```bash
k6 version
```

If it is not installed, see https://k6.io/docs/get-started/installation/

## Required environment variables

Copy `perf/.env.example` and adapt it to your target environment.

Minimum for guest smoke:

```bash
BASE_URL=https://api.quickdelivery.fr
k6 run perf/suites/smoke.js
```

Admin dashboard tests require:

```bash
ADMIN_BEARER_TOKEN=...
```

Courier lifecycle tests require prepared delivery data:

```bash
COURIER_BEARER_TOKEN=...
COURIER_ID=123
COURIER_PACKAGE_ID=456
COURIER_PICKUP_OTP=...
COURIER_DELIVERY_OTP=...
```

Tracking tests require:

```bash
TRACKING_BEARER_TOKEN=...
TRACKING_DELIVERY_PERSON_ID=123
TRACKING_PACKAGE_REFERENCE=PACKFR...
```

## Recommended execution order

1. Smoke

```bash
k6 run perf/suites/smoke.js
```

2. Nominal

```bash
k6 run perf/suites/nominal.js
```

3. Stress

```bash
k6 run perf/suites/stress.js
```

4. Focused tracking campaign

```bash
k6 run perf/scenarios/tracking-live.js
```

5. Seed visible `NEW` packages around `3 rue Pasteur, 94450 Limeil-Brevannes`

```bash
k6 run perf/scenarios/seed-visible-packages.js
```

## PowerShell runner

On Windows, you can launch the suites with the helper script:

```powershell
powershell -ExecutionPolicy Bypass -File perf/run.ps1 -Suite smoke
powershell -ExecutionPolicy Bypass -File perf/run.ps1 -Suite regression
powershell -ExecutionPolicy Bypass -File perf/run.ps1 -Suite nominal
powershell -ExecutionPolicy Bypass -File perf/run.ps1 -Suite stress
powershell -ExecutionPolicy Bypass -File perf/run.ps1 -Suite e2e-delivery
powershell -ExecutionPolicy Bypass -File perf/run.ps1 -Suite seed-visible-packages
```

The runner automatically loads the first file it finds in this order:

1. `perf/.env.local`
2. `perf/.env`
3. `perf/.env.example`

If `ADMIN_USERNAME` / `ADMIN_PASSWORD` and or `COURIER_USERNAME` / `COURIER_PASSWORD` are present in the env file, `run.ps1` refreshes the bearer tokens through `init-env.ps1` before launching `k6`. This is important for `nominal` and `stress`, because long campaigns can outlive the previous access tokens.

You can also force a specific env file and pass extra `k6` arguments:

```powershell
powershell -ExecutionPolicy Bypass -File perf/run.ps1 -Suite tracking-live -EnvFile perf/.env.local -ExtraArgs "--vus=2","--iterations=2"
```

## PowerShell environment bootstrap

You can auto-fill the main variables with:

```powershell
powershell -ExecutionPolicy Bypass -File perf/init-env.ps1
```

You can also force the courier email if the courier token payload does not expose it:

```powershell
powershell -ExecutionPolicy Bypass -File perf/init-env.ps1 -CourierEmail livreur.test@quickdelivery.local
```

The bootstrap script:

- reads or creates `perf/.env.local`
- can request tokens directly from Keycloak with username and password
- uses `quickdelivery-postman` by default because it supports direct access grants
- asks for access tokens only if automatic login is not available
- resolves the courier id from the courier token email
- selects one `NEW` package for the courier lifecycle scenario
- selects one `PICKEDUP` package for the tracking scenario when available
- writes the resolved values back to `perf/.env.local`

If no `NEW` package or no `PICKEDUP` package exists, the related variables stay empty and the corresponding scenario will be skipped.

## Notes

- `guest-checkout.js` is self-contained and creates its own guest package.
- `courier-onboarding.js` uploads a full courier file with vehicle documents, then validates it as admin.
- `e2e-delivery.js` creates a package near `3 Rue Pasteur, 94450 Limeil-Brevannes` and drives it to `DELIVERED`.
- `seed-visible-packages.js` leaves paid `NEW` packages around Limeil-Brevannes so they can be seen on the “packages autour de moi” map in 10, 20 and 30 km radius filters.
- `tracking-live.js` retries once before failing the websocket update assertion to reduce false negatives.
- Thresholds are versioned in `perf/lib/thresholds.js`.
- `cleanup-test-data.ps1` is `dry-run` by default and only executes with `-Execute -ConfirmationPhrase DELETE_QUICKDELIVERY_PERF_DATA`.
