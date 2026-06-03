# Experiment Runbook

This runbook contains operational steps for reproducing scenario runs and collecting evidence.

## 1) Build all modules

```powershell
mvn clean verify
```

## 2) Start both reference services

From the repository root, use two terminals:

```powershell
mvn -pl reference-services/conventional-api spring-boot:run
```

```powershell
mvn -pl reference-services/hypermedia-api spring-boot:run
```

Expected base URLs:

- Conventional API: `http://localhost:8080`
- Hypermedia API: `http://localhost:8081`

## 3) Execute scenario prompts with Copilot

Use the same prompt wording for both modes:

- Prompt suite: `test-plans/copilot-prompt-suite.md`
- Scenario criteria: `test-plans/order-management.md`

## 4) Capture before/after evidence with JBang

Conventional example:

```powershell
jbang scripts/actuator-delta.java start --scenario S1 --mode conventional --base-url http://localhost:8080
# Run S1 prompt in Copilot
jbang scripts/actuator-delta.java finish --scenario S1 --mode conventional --base-url http://localhost:8080
```

Hypermedia example:

```powershell
jbang scripts/actuator-delta.java start --scenario S1 --mode hypermedia --base-url http://localhost:8081
# Run S1 prompt in Copilot
jbang scripts/actuator-delta.java finish --scenario S1 --mode hypermedia --base-url http://localhost:8081
```

Optional (if Actuator uses a different base path):

```powershell
jbang scripts/actuator-delta.java start --scenario S1 --mode conventional --base-url http://localhost:8080 --actuator-base-path /actuator
jbang scripts/actuator-delta.java finish --scenario S1 --mode conventional --base-url http://localhost:8080 --actuator-base-path /actuator
```

## 5) Artifacts produced

The script captures:

- `/actuator/metrics/http.server.requests`
- `/actuator/httpexchanges`
- Delta summary including request-count delta, 4xx count, status buckets, and operation distribution

Primary output location:

- `test-plans/manual-results/`

Key output files:

- `metrics-deltas.csv` (raw aggregate)
- `metrics-deltas-adjusted.csv` (duration-adjusted representative values)
- `summary.md` (markdown summary table)
- `normalization-notes.md` (domain-vs-non-domain caveat)
- `verdict-draft.md` (caveated interpretation and recommendation)

## 6) Fairness checklist

- Same prompt text for both modes
- Same starting dataset/fixture state
- Same Copilot execution mode (Chat or CLI)
- Same retry policy and timeout assumptions

## 7) Known caveat

Conventional runs can be skewed by non-domain traffic (OpenAPI/Swagger/Actuator discovery). Use:

- `test-plans/manual-results/normalization-notes.md`

for scoped interpretation.
