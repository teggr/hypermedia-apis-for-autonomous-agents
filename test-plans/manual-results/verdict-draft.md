# Verdict Draft from Captured Runs

This draft uses:
- Raw scenario deltas: `test-plans/manual-results/metrics-deltas.csv`
- Duration-adjusted representative values: `test-plans/manual-results/metrics-deltas-adjusted.csv`

## Decision Mode

Project decision for current phase:
- Proceed with caveated interpretation using existing evidence artifacts.
- Do not run immediate cleanup reruns for interrupted/skewed scenarios in this pass.

Practical implications:
- Treat H3/H5 findings as directional (not definitive).
- Keep H1/H2/H4/H6/H7/H8 as inconclusive until targeted experiments are executed.
- Preserve current dataset as baseline for next iteration.

## Scenario Results Table (Available Metrics)

| Scenario | Mode | API Calls (Raw Delta) | Invalid Attempts 4xx (Raw) | API Calls (Adjusted) | Invalid Attempts 4xx (Adjusted) |
|---|---|---:|---:|---:|---:|
| S1 | conventional | 26.00 | 0.00 | 26.00 | 0.00 |
| S1 | hypermedia | 12.00 | 0.00 | 12.00 | 0.00 |
| S2 | conventional | 143.00 | 115.00 | 40.16 | 32.00 |
| S2 | hypermedia | 13.00 | 5.00 | 13.00 | 5.00 |
| S3 | conventional | 50.00 | 33.00 | 50.00 | 33.00 |
| S3 | hypermedia | 14.00 | 5.00 | 14.00 | 5.00 |
| S4 | conventional | 14.00 | 8.00 | 14.00 | 8.00 |
| S4 | hypermedia | 14.00 | 7.00 | 14.00 | 7.00 |
| S5 | conventional | 34.00 | 16.00 | 34.00 | 16.00 |
| S5 | hypermedia | 18.00 | 7.00 | 18.00 | 7.00 |
| S6 | conventional | 36.00 | 10.00 | 15.31 | 4.25 |
| S6 | hypermedia | 32.00 | 8.00 | 11.81 | 2.95 |

## Preliminary Hypothesis Status (Evidence from Current Dataset)

| Hypothesis | Preliminary Status | Notes |
|---|---|---|
| H1 Runtime discovery | Inconclusive | Requires explicit zero-documentation runs and affordance-traversal proof logs. |
| H2 API evolution resilience | Inconclusive | S5 breaking-change control needs explicit confirmation/evidence capture. |
| H3 State-aware safety | Partially supported | Hypermedia shows fewer 4xx invalid attempts than conventional in S2/S3/S5/S6, but still non-zero. |
| H4 Context pollution reduction | Inconclusive | No per-step operation-set breadth metric captured yet. |
| H5 Efficiency tradeoff | Partially supported | Hypermedia generally lower API call deltas in S1/S2/S3/S5; mixed in S4 and raw S6; adjusted S6 favors hypermedia. |
| H6 Generic client sufficiency | Inconclusive | Second domain/capability set not executed yet. |
| H7 Zero-documentation execution | Inconclusive | Explicit no-endpoint-knowledge prompt set not run yet. |
| H8 Unknown capability discovery | Inconclusive | New server-side capability experiment not run yet. |

## Remaining Gaps Before Final Verdict

- Correctness per scenario and final order states are not yet normalized into a machine-readable results file.
- Token usage is not captured in current artifacts.
- H2 needs controlled endpoint/workflow break with unchanged hypermedia relation semantics and re-run evidence.
- H1/H7 need explicit zero-documentation prompt executions.
- H4 needs per-step operation-set size instrumentation.

## Evidence Caveat: Conventional API-Docs Skew

Conventional totals can be inflated when the client explores non-domain endpoints (for example OpenAPI/Swagger/Actuator paths).

See normalization analysis:
- `test-plans/manual-results/normalization-notes.md`

Impact on interpretation:
- Use raw totals for traceability, but prefer domain-scoped interpretation (`/orders*`) when comparing behavioral efficiency and invalid attempts.
- Treat H3/H5 as preliminary until a domain-restricted rerun (or tighter instrumentation) is executed.

## Projected Token Usage Direction (No Hard Figures)

Token-use expectation based on observed behavior patterns:
- Raw API without discoverable contract: highest token pressure, because the agent must infer capabilities and recover from more trial-and-error turns.
- Conventional API with discoverable OpenAPI: lower token pressure than raw API, because schema and operation hints reduce guessing when docs are found.
- Hypermedia with state-aware affordances: typically the lowest token pressure for multi-step workflows, because each response narrows valid next actions and reduces exploratory prompting.

Important caveat:
- If conventional runs include API-doc discovery detours (or repeated non-domain calls), token use can spike and mask underlying workflow efficiency.
- If hypermedia relations are sparse or poorly named, token savings can shrink.

## Final Recommendation (Caveated, Current Evidence)

Current caveated ordering:
- Raw API (no discoverable contract): highest ambiguity, most guessing.
- Conventional API with OpenAPI discovery: better speed and accuracy when docs are found.
- Hypermedia API with state-aware affordances: further speed and accuracy gains in stateful workflows.

Prefer hypermedia when:
- Workflows are multi-step/stateful.
- Invalid transitions and adaptability to change are important.
- You want low-ambiguity next-step guidance from the server.

Static contracts are sufficient when:
- Workflows are simple and stable.
- OpenAPI discovery is reliable in your execution environment.
- Tooling compatibility matters more than runtime affordance navigation.

Confidence level:
- Directional for architecture decisions now.
- Not final-proof until targeted follow-up closes H2/H4/H7/H8 and adds token/correctness measurements.
