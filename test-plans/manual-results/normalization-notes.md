# Normalization Notes: Conventional API-Docs Skew

This note addresses potential skew when the client interacts with non-domain endpoints (for example API docs, swagger assets, actuator endpoints, and wildcard routes).

## Method

- Source: per-scenario `delta-*.json` operation distributions.
- Domain scope for normalization: URIs starting with `/orders`.
- Non-domain scope: all other URIs (`/actuator*`, `/swagger*`, `/**`, `UNKNOWN`, etc).
- `orders_4xx_approx` is estimated from scenario-level 4xx ratio:

`orders_4xx_approx = orders_calls * (invalid_4xx / metric_delta)`

This is an approximation because fallback metric-tag mode does not provide per-operation status counts.

## Latest Runs: Domain vs Non-Domain Calls

| Scenario | Mode | Metric Delta | Invalid 4xx | Orders Calls | Non-Orders Calls | Orders 4xx Approx |
|---|---|---:|---:|---:|---:|---:|
| S1 | conventional | 26.00 | 0.00 | 0.00 | 26.00 | 0.00 |
| S1 | hypermedia | 12.00 | 0.00 | 0.00 | 12.00 | 0.00 |
| S2 | conventional | 143.00 | 115.00 | 97.00 | 46.00 | 78.01 |
| S2 | hypermedia | 13.00 | 5.00 | 5.00 | 8.00 | 1.92 |
| S3 | conventional | 50.00 | 33.00 | 22.00 | 28.00 | 14.52 |
| S3 | hypermedia | 14.00 | 5.00 | 6.00 | 8.00 | 2.14 |
| S4 | conventional | 14.00 | 8.00 | 4.00 | 10.00 | 2.29 |
| S4 | hypermedia | 14.00 | 7.00 | 4.00 | 10.00 | 2.00 |
| S5 | conventional | 34.00 | 16.00 | 11.00 | 23.00 | 5.18 |
| S5 | hypermedia | 18.00 | 7.00 | 9.00 | 9.00 | 3.50 |
| S6 | conventional | 36.00 | 10.00 | 15.00 | 21.00 | 4.17 |
| S6 | hypermedia | 32.00 | 8.00 | 21.00 | 11.00 | 5.25 |

## Interpretation

- The concern is valid: non-domain traffic is present and can skew direct call/error totals.
- Conventional mode appears more affected in S2 and S3 by large non-domain traffic.
- Even after scope filtering, hypermedia still tends to show lower order-domain activity for S2/S3 and mixed results elsewhere.

## Recommended Use

- Keep `metrics-deltas.csv` as immutable raw evidence.
- Use this normalization note alongside `metrics-deltas-adjusted.csv` for fairer comparison discussion.
- For stronger evidence, rerun key scenarios with stricter prompt guardrails (domain endpoints only) and deterministic reset.
