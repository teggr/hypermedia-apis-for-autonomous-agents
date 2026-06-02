# Test Plan: Order Management — Conventional vs Hypermedia API

## Overview

This test plan defines the scenarios, metrics, and pass/fail criteria for evaluating
the conventional REST API approach against the hypermedia (HATEOAS) approach when used
by an autonomous AI agent.

Both services implement the same **order management domain**:

```
PENDING ──► CONFIRMED ──► SHIPPED ──► DELIVERED
   │              │
   └──────────────┴──► CANCELLED
```

---

## Evaluation Dimensions

| Dimension | What we measure |
|---|---|
| **Correctness** | Did the agent complete the task with the right final state? |
| **Efficiency** | Number of API calls and LLM tokens consumed per scenario |
| **Invalid attempts** | Number of tool calls that resulted in a 4xx error |
| **Adaptability** | Agent behaviour after an API breaking change is introduced |
| **Tool footprint** | Number of tool definitions registered with the agent |
| **Developer effort** | Lines of tool/integration code written per API style |

---

## Scenarios

### S1 — Happy path: full lifecycle

**Task prompt:** `"Create an order for customer C1 for 'Widget A', then confirm it, ship it, and mark it delivered."`

| Check | Pass criteria |
|---|---|
| Final order status | `DELIVERED` |
| API calls | ≤ 5 (create + confirm + ship + deliver + optional GET) |
| Invalid attempts | 0 |

---

### S2 — Cancellation

**Task prompt:** `"Create an order for customer C2 for 'Gadget B', confirm it, then cancel it."`

| Check | Pass criteria |
|---|---|
| Final order status | `CANCELLED` |
| API calls | ≤ 4 |
| Invalid attempts | 0 |

---

### S3 — Invalid transition attempt

**Task prompt:** `"Create an order for customer C3 for 'Doohickey C' and immediately try to ship it."`

| Check | Pass criteria |
|---|---|
| Agent response | Agent should report the transition failed or not allowed |
| Final order status | `PENDING` (unchanged) |
| Notes | Conventional: agent may attempt `/ship` and receive 409; Hypermedia: ship link should not be present in response so agent should not attempt it |

---

### S4 — Discovery without prior knowledge

**Task prompt:** `"List all orders and report the status of each one."`

| Check | Pass criteria |
|---|---|
| Response | Correct list of statuses for all existing orders |
| Invalid attempts | 0 |
| Notes | Hypermedia agent should follow the `orders` collection link from any prior response |

---

### S5 — API change resilience *(adaptability test)*

Introduce a breaking change to the conventional API: rename `/orders/{id}/confirm` to `/orders/{id}/approve`.
Leave the hypermedia API unchanged (the assembler emits the link under the same `confirm` rel, pointing to the new URL internally).

**Task prompt:** Same as S1.

| Check | Pass criteria |
|---|---|
| Conventional | Agent fails or requires tool definition update |
| Hypermedia | Agent succeeds without any tool changes |

---

### S6 — Multi-order workflow

**Task prompt:** `"Create three orders for customer C4. Confirm the first two and cancel the third. Then list all orders for C4 and report their statuses."`

| Check | Pass criteria |
|---|---|
| Order 1 status | `CONFIRMED` |
| Order 2 status | `CONFIRMED` |
| Order 3 status | `CANCELLED` |
| Invalid attempts | 0 |

---

## Running the Scenarios

### Prerequisites

1. Start the target service:
   - Conventional API: `cd reference-services/conventional-api && mvn spring-boot:run`
   - Hypermedia API: `cd reference-services/hypermedia-api && mvn spring-boot:run`
2. Execute prompts using Copilot Chat or Copilot CLI.
3. Use `test-plans/copilot-prompt-suite.md` for prompt templates and actuator evidence capture.

### Automated evaluation

> **TODO**: Add a scriptable Copilot-run workflow (CLI prompt runner + actuator snapshot export)
> to execute each scenario and persist comparable evidence automatically.

---

## Recording Results

Fill in this table after each evaluation run:

| Scenario | Approach | Correct? | API calls | Invalid attempts | Tokens |
|---|---|---|---|---|---|
| S1 | Conventional | | | | |
| S1 | Hypermedia | | | | |
| S2 | Conventional | | | | |
| S2 | Hypermedia | | | | |
| S3 | Conventional | | | | |
| S3 | Hypermedia | | | | |
| S4 | Conventional | | | | |
| S4 | Hypermedia | | | | |
| S5 | Conventional | | | | |
| S5 | Hypermedia | | | | |
| S6 | Conventional | | | | |
| S6 | Hypermedia | | | | |
