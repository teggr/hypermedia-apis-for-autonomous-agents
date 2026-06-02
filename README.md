# Hypermedia APIs for Autonomous Agents

This repository investigates whether hypermedia-driven APIs (HATEOAS / REST Level 3), especially HAL-FORMS affordances, provide practical advantages for autonomous AI agents compared to conventional OpenAPI + MCP/skill-based integrations.

## Source Conversations

The project direction is grounded in two conversation artifacts:

- [conversations/chat-gpt-conversation.md](conversations/chat-gpt-conversation.md): full narrative discussion and prototype ideas
- [conversations/hateoas-for-agent-systems.md](conversations/hateoas-for-agent-systems.md): distilled summary of hypotheses, benefits, challenges, and proposed experiments

If there is a mismatch between documents, treat the full conversation as the canonical source and update the summary to match it.

## Core Hypothesis

Agents may behave more like web browsers than SDK consumers:

- Discover capabilities at runtime from server-provided links and action templates
- Follow state-dependent affordances instead of invoking a static global tool list
- Adapt better to endpoint/workflow evolution because navigation is link-driven

## What This Repo Compares

1. A conventional API exposed through static contracts (OpenAPI + MCP/tool wrappers)
2. A hypermedia API that returns dynamic, state-aware affordances (HAL/HAL-FORMS)

Both are exercised by Copilot Chat or Copilot CLI prompts to evaluate:

- correctness
- efficiency (calls/tokens)
- robustness (invalid/hallucinated operations)
- adaptability to API evolution
- developer integration effort

## Project Layout

- `reference-services/conventional-api`: baseline service using conventional REST patterns
- `reference-services/hypermedia-api`: service using Spring HATEOAS affordances
- `test-plans`: scenario plans and evaluation criteria
- `conversations`: source reasoning and distilled findings

## Experiment Runbook

Use this runbook to execute comparable baseline experiments before implementing the full
automated evaluation harness.

### 1) Build all modules

```powershell
mvn clean verify
```

### 2) Start both reference services

Open two terminals from the repository root:

```powershell
mvn -pl reference-services/conventional-api spring-boot:run
```

```powershell
mvn -pl reference-services/hypermedia-api spring-boot:run
```

Expected base URLs:

- Conventional API: `http://localhost:8080`
- Hypermedia API: `http://localhost:8081`

### 3) Use Copilot Chat or Copilot CLI as the executing agent

This repository now treats Copilot as the primary execution agent. Use the same
scenario prompts against both services.

- Prompt suite: `test-plans/copilot-prompt-suite.md`
- Scenario definitions: `test-plans/order-management.md`

### 4) Capture service-side evidence from Actuator

Collect evidence from each service after every scenario run:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/metrics/http.server.requests
Invoke-RestMethod http://localhost:8081/actuator/metrics/http.server.requests
Invoke-RestMethod http://localhost:8080/actuator/httpexchanges
Invoke-RestMethod http://localhost:8081/actuator/httpexchanges
```

Endpoint-level drill-down examples:

```powershell
Invoke-RestMethod "http://localhost:8080/actuator/metrics/http.server.requests?tag=uri:/orders/{id}/confirm"
Invoke-RestMethod "http://localhost:8081/actuator/metrics/http.server.requests?tag=uri:/orders/{id}/confirm"
```

### 5) Store experiment artifacts

For each run, store:

- Prompt text
- Copilot response summary
- Relevant API response snippets
- Scenario result and final order status
- Actuator metric snapshots and HTTP exchange traces

Recommended location for manual notes and snapshots:

- `test-plans/manual-results/`

### 6) Fairness checklist before comparing outcomes

- Same prompt text for both modes
- Same starting dataset/fixture state
- Same Copilot execution mode (Chat window or CLI) and prompt format
- Same retry policy and timeout assumptions

This runbook is the primary execution workflow for the checklist in `ROADMAP.md`.
