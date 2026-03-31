# Tracking V2 Design

## Goal

Make live tracking scalable and predictable by centering the write path on `packageReference`, not only on `deliveryPersonId`.

## Why

The current tracking update endpoint updates all packages in delivery for one courier:

- `POST /packages/v1/tracking/position?deliveryPersonId=...`

This is simple, but it creates broad fanout under load when a courier is associated with multiple active packages.

The WebSocket subscription model is already package-centric:

- `TRACK_PACKAGE_SUBSCRIBE`
- `packageReference`

The write path should match the read path.

## Target API

### POST `/packages/v1/tracking/position`

Existing endpoint kept for compatibility.

Behavior:

- still accepts `deliveryPersonId`
- resolves active package references
- delegates internally to package-centric tracking updates

### POST `/packages/v1/tracking/package-position`

New endpoint.

Params:

- `packageReference`

Body:

- `latitude`
- `longitude`

Behavior:

- validates package exists and is in tracking-eligible state
- updates only that package last position
- stores position in Redis cache
- publishes only that package update

### GET `/packages/v1/tracking/active-package?deliveryPersonId=...`

Returns the most relevant active package for the courier:

- `packageReference`
- `status`

Used by the mobile/web courier app to send targeted updates when one active delivery is in focus.

## Runtime Model

- Redis stores `last-position:{packageReference}`
- WebSocket subscriptions remain keyed by `packageReference`
- HTTP tracking writes publish only the updated package reference

## Compatibility Strategy

Phase 1:

- keep `/ws`
- keep `/packages/v1/tracking/position`
- add `/packages/v1/tracking/package-position`
- front prefers package-position when it knows the active package

Phase 2:

- move tracking into a dedicated deployable if needed

## Performance Impact Expected

- lower fanout per write
- lower contention under concurrent e2e traffic
- lower p95 on `tracking_propagation_ms`

## Validation Plan

1. targeted `tracking-live`
2. `regression`
3. `nominal`
4. `stress`

