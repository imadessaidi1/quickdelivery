# Performance Load Plan

## Objectives

The campaign validates four critical areas independently and under mixed load:

- customer guest checkout
- courier operations
- live tracking
- admin dashboards

## Main KPIs

- HTTP latency: `p50`, `p95`, `p99`
- HTTP error rate
- check success rate
- websocket propagation delay
- throughput by scenario
- gateway and service degradation under mixed load

## Validation thresholds

### Guest checkout

- estimate price: `p95 < 800 ms`
- create package: `p95 < 2500 ms`
- confirm guest payment: `p95 < 1500 ms`
- consult guest package: `p95 < 800 ms`
- error rate: `< 1%`

### Admin dashboards

- metrics dashboard: `p95 < 2000 ms`
- finance dashboard: `p95 < 2000 ms`
- error rate: `< 1%`

### Tracking

- tracking position HTTP update: `p95 < 1200 ms`
- websocket propagation: `p95 < 2000 ms`
- missed updates: `< 2%`

### Stress acceptance

- total HTTP error rate under stress: `< 5%`
- no container restart loop
- no sustained websocket collapse

## Execution sequence

### 1. Smoke

Purpose:

- validate scripts
- validate tokens
- validate connectivity

Load:

- guest checkout: 1 VU, 2 iterations
- admin dashboards: 1 VU, 2 iterations
- courier lifecycle: 1 VU, 1 iteration if seeded values exist
- tracking: 1 VU, 1 iteration if tracking values exist

### 2. Nominal

Purpose:

- validate platform under realistic load

Load:

- guest checkout: ramp to 10 VUs, sustain 10 minutes
- admin dashboards: 2 VUs, 10 minutes
- courier lifecycle: 2 VUs, 5 minutes
- tracking live: 5 VUs, 10 minutes

### 3. Stress

Purpose:

- identify first bottleneck and point of degradation

Load:

- guest checkout: ramp 10 -> 40 VUs
- tracking live: ramp 5 -> 20 VUs
- admin dashboards: fixed 4 VUs
- courier lifecycle: fixed 4 VUs

Duration:

- 20 minutes total

### 4. Endurance

Purpose:

- identify leaks and long-run degradation

Suggested extension:

- reuse nominal profile for 60 to 120 minutes

## Operational checklist

Before each run:

1. confirm all containers are healthy
2. confirm `config`, `discovery`, `gateway`, `packages`, `users` are up
3. confirm seeded courier and tracking data exists
4. clear old dashboards or note baseline CPU and RAM
5. capture `docker stats` during the run

After each run:

1. export `k6` summary
2. inspect application logs for `500`, `timeout`, `UnknownHostException`, `LazyInitializationException`
3. compare `p95` and error rate with the thresholds above
4. record which service degraded first
