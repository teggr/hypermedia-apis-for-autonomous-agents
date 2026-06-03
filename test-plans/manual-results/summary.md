# Scenario Metrics Summary

This summary is generated from `metrics-deltas.csv` and companion per-scenario delta JSON files.

## Outlier Review (Duration)

Elapsed minutes were computed from the before/after snapshot filenames in each scenario folder.

Rule used for interruption outliers:
- Flag as outlier when elapsed minutes > 2 x mode median elapsed minutes.
- Mode median elapsed minutes:
  - conventional: 5.69
  - hypermedia: 1.55

Flagged runs:
- S2 conventional (20.25m)
- S6 conventional (13.38m)
- S6 hypermedia (4.20m)

Adjusted values are kept in `metrics-deltas-adjusted.csv` and are scaled by:

adjusted_value = raw_value x (mode_median_minutes / elapsed_minutes)

Raw evidence is unchanged in `metrics-deltas.csv`.

## Raw Results (From metrics-deltas.csv)

| Scenario | Mode | Metric Delta | Invalid 4xx | Status 2xx | Status 4xx |
|---|---|---:|---:|---:|---:|
| S1 | conventional | 26.00 | 0 | 0 | 0 |
| S1 | hypermedia | 12.00 | 0 | 0 | 0 |
| S2 | conventional | 143.00 | 115 | 31 | 115 |
| S2 | hypermedia | 13.00 | 5 | 10 | 5 |
| S3 | conventional | 50.00 | 33 | 18 | 33 |
| S3 | hypermedia | 14.00 | 5 | 11 | 5 |
| S4 | conventional | 14.00 | 8 | 9 | 8 |
| S4 | hypermedia | 14.00 | 7 | 10 | 7 |
| S5 | conventional | 34.00 | 16 | 20 | 16 |
| S5 | hypermedia | 18.00 | 7 | 13 | 7 |
| S6 | conventional | 36.00 | 10 | 27 | 10 |
| S6 | hypermedia | 32.00 | 8 | 26 | 8 |

## Adjusted Representative Results (Time-Outlier Corrected)

| Scenario | Mode | Elapsed Min | Outlier | Adjusted Metric Delta | Adjusted Invalid 4xx |
|---|---|---:|---|---:|---:|
| S1 | conventional | 7.95 | no | 26.00 | 0.00 |
| S1 | hypermedia | 1.23 | no | 12.00 | 0.00 |
| S2 | conventional | 20.25 | yes | 40.16 | 32.00 |
| S2 | hypermedia | 1.62 | no | 13.00 | 5.00 |
| S3 | conventional | 3.43 | no | 50.00 | 33.00 |
| S3 | hypermedia | 1.63 | no | 14.00 | 5.00 |
| S4 | conventional | 1.42 | no | 14.00 | 8.00 |
| S4 | hypermedia | 0.98 | no | 14.00 | 7.00 |
| S5 | conventional | 1.95 | no | 34.00 | 16.00 |
| S5 | hypermedia | 1.47 | no | 18.00 | 7.00 |
| S6 | conventional | 13.38 | yes | 15.31 | 4.25 |
| S6 | hypermedia | 4.20 | yes | 11.81 | 2.95 |

## Notes

- This summary provides a comparable baseline while preserving raw artifacts.
- If you prefer stricter correction, re-run only flagged scenarios (S2 conventional, S6 conventional, S6 hypermedia).
