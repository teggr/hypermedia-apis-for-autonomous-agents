# Copilot Prompt Suite for Manual and CLI Execution

Use this prompt suite when Copilot Chat or Copilot CLI is the execution agent.
The services are the source of truth for call metrics and traces via Actuator.

## Preconditions

- Conventional API running on `http://localhost:8080`
- Hypermedia API running on `http://localhost:8081`
- Actuator endpoints available:
  - `/actuator/metrics/http.server.requests`
  - `/actuator/httpexchanges`

## Suggested Execution Pattern

1. Run one scenario against the conventional API with Copilot.
2. Capture actuator snapshots from conventional API.
3. Run the same scenario against the hypermedia API with Copilot.
4. Capture actuator snapshots from hypermedia API.
5. Compare request counts, endpoint distribution, and error traces.

## Scenario Prompts (S1-S6)

### S1 Happy path

"Use only API calls to complete this task against BASE_URL: create an order for customer C1 for Widget A, then confirm it, ship it, and mark it delivered. Return the final order id and status."

### S2 Cancellation

"Use only API calls to complete this task against BASE_URL: create an order for customer C2 for Gadget B, confirm it, then cancel it. Return the final order id and status."

### S3 Invalid transition

"Use only API calls to complete this task against BASE_URL: create an order for customer C3 for Doohickey C, then immediately try to ship it. Report whether the transition was rejected and the final order status."

### S4 Discovery/listing

"Use only API calls to complete this task against BASE_URL: list all orders and return each order id and status."

### S5 API resilience

"Use only API calls to complete this task against BASE_URL: create an order for customer C1 for Widget A, then confirm it, ship it, and mark it delivered. Return the final order id and status."

### S6 Multi-order workflow

"Use only API calls to complete this task against BASE_URL: create three orders for customer C4, confirm the first two, cancel the third, then list all orders for C4 with final statuses."

## Actuator Commands (PowerShell)

Get overall request metrics:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/metrics/http.server.requests | ConvertTo-Json -Depth 8
Invoke-RestMethod http://localhost:8081/actuator/metrics/http.server.requests | ConvertTo-Json -Depth 8
```

Get endpoint-specific metrics (example: confirm transition):

```powershell
Invoke-RestMethod "http://localhost:8080/actuator/metrics/http.server.requests?tag=uri:/orders/{id}/confirm" | ConvertTo-Json -Depth 8
Invoke-RestMethod "http://localhost:8081/actuator/metrics/http.server.requests?tag=uri:/orders/{id}/confirm" | ConvertTo-Json -Depth 8
```

Get recent HTTP exchanges:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/httpexchanges | ConvertTo-Json -Depth 8
Invoke-RestMethod http://localhost:8081/actuator/httpexchanges | ConvertTo-Json -Depth 8
```

## How to Interpret Evidence

- `http.server.requests` gives aggregate counts and timing by tags (uri, method, status, outcome).
- `httpexchanges` gives request/response traces to verify exact paths and statuses.
- For invalid transition checks, look for `409` status entries.
- For not-found checks, look for `404` status entries.

## Notes

- Spring accumulates metrics during process lifetime. For clean per-scenario numbers, either restart the service per scenario or compute deltas between before/after snapshots.
- Keep prompt wording identical between conventional and hypermedia runs to preserve fairness.
