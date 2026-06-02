# Roadmap: Hypothesis Validation Checklist

This roadmap defines all tasks required to answer the hypotheses from the conversation artifacts with measurable evidence.

## Scope

- In scope: work needed to determine whether hypermedia-driven APIs provide practical agent benefits versus static-contract integrations.
- Out of scope: production hardening, load testing, security hardening, and UI/dashboard polish beyond minimal reporting.

## Canonical Sources

- `conversations/chat-gpt-conversation.md` (canonical intent and hypotheses)
- `conversations/hateoas-for-agent-systems.md` (distilled summary, must remain aligned)
- `test-plans/order-management.md` (S1-S6 baseline scenarios and pass criteria)

## Hypothesis Matrix

- [ ] H1 Runtime discovery: Agent can discover capabilities from responses at runtime without pre-loaded endpoint-specific knowledge.
  - Evidence required: Zero-documentation scenario outcomes + logs proving link/template traversal.
- [ ] H2 API evolution resilience: Hypermedia agent adapts to endpoint/workflow changes without code/tool updates better than conventional.
  - Evidence required: Controlled breaking-change experiment (S5-like) with before/after results.
- [ ] H3 State-aware safety: Hypermedia affordances reduce invalid operations versus conventional tool invocation.
  - Evidence required: Invalid-attempt counts and state-transition audit per scenario.
- [ ] H4 Context pollution reduction: Hypermedia exposes fewer irrelevant operations in each step than static tool lists.
  - Evidence required: Per-step operation-set size comparison (offered operations per decision point).
- [ ] H5 Efficiency tradeoff: Hypermedia may have discovery overhead but should be acceptable relative to robustness/adaptability gains.
  - Evidence required: API call and token distributions across repeated runs.
- [ ] H6 Generic client sufficiency: Domain-agnostic client logic can execute workflows without domain-specific wrappers.
  - Evidence required: Same traversal client succeeds across at least two capability sets/domains.
- [ ] H7 Zero-documentation execution: Agent can complete realistic multi-step tasks with only base URL, auth, and goal.
  - Evidence required: Success in explicit no-endpoint-knowledge scenarios.
- [ ] H8 Unknown capability discovery: Agent can discover and use newly introduced capability without integration changes.
  - Evidence required: New action added server-side and successfully used by unchanged hypermedia client path.

## Phase 1: Baseline Parity and Experiment Readiness

- [ ] Verify domain parity across both services (entity fields, status model, transition rules).
- [ ] Verify lifecycle parity for create/read/list/confirm/ship/deliver/cancel behavior.
- [ ] Confirm integration tests pass in both services before comparative experiments.
- [ ] Freeze and document baseline configuration:
  - [x] Ports and base URLs
  - [ ] Profiles and environment variables
  - [x] Startup sequence for both services and Copilot execution flow
  - [x] Repeatable run commands
- [ ] Confirm conventional baseline contract availability:
  - [ ] Expose/verify OpenAPI output
  - [ ] Capture versioned spec artifact for experiment traceability

## Phase 2: Copilot Execution Harness

- [ ] Define scriptable Copilot execution workflow for both modes:
  - [ ] `conventional`
  - [ ] `hypermedia`
- [ ] Add deterministic service reset/seed steps for repeatable scenario runs.
- [ ] Implement scenario execution for existing baseline set:
  - [ ] S1 Happy path
  - [ ] S2 Cancellation
  - [ ] S3 Invalid transition attempt
  - [ ] S4 Discovery without prior knowledge
  - [ ] S5 API change resilience
  - [ ] S6 Multi-order workflow
- [ ] Persist run artifacts in machine-readable and human-readable forms:
  - [x] JSON or CSV raw run metrics
  - [ ] Markdown summary table
- [ ] Add pass/fail assertions tied to scenario criteria from `test-plans/order-management.md`.

## Phase 3: Metrics and Instrumentation

- [ ] Capture correctness signals per run (final states and task completion).
- [ ] Capture API call count per scenario and per mode.
- [ ] Capture invalid-attempt count (4xx transition/operation failures).
- [ ] Capture token usage per scenario and per mode.
- [ ] Capture API interaction footprint:
  - [ ] Conventional endpoint-call distribution
  - [ ] Hypermedia affordance-following distribution
- [ ] Capture integration effort signal (code footprint and touchpoints).
- [ ] Capture operation-set size per decision step to assess context pollution.

## Phase 4: Hypothesis-Specific Experiments

### H1 + H7: Runtime Discovery and Zero-Documentation

- [ ] Add explicit zero-documentation prompts (base URL + auth + goal only).
- [ ] Run zero-documentation tasks for both modes where feasible.
- [ ] Verify hypermedia runs demonstrate navigation from affordances, not hardcoded paths.

### H2: API Evolution Resilience

- [ ] Introduce controlled breaking change in conventional API pathing (confirm action rename/move).
- [ ] Keep hypermedia relation semantics stable while target URL changes.
- [ ] Re-run equivalent scenarios without changing hypermedia prompt workflow.
- [ ] Record comparative adaptation outcomes.

### H3: State-Aware Safety

- [ ] Validate state-conditioned links/templates only expose valid transitions.
- [ ] Assert invalid transition attempts are absent or lower in hypermedia runs.

### H4: Context Pollution

- [ ] Instrument operation menu breadth seen by model per step.
- [ ] Compare static tool-list breadth vs state-scoped affordance breadth.

### H5: Efficiency vs Robustness

- [ ] Run repeated trials (minimum 3 per scenario/mode).
- [ ] Report central tendency and spread for calls/tokens.
- [ ] Evaluate whether any efficiency penalty is offset by adaptability/robustness gains.

### H6: Generic Client Sufficiency

- [ ] Add second capability set/domain (or equivalent extension) traversable by same generic client behavior.
- [ ] Execute tasks in both domains without domain-specific wrappers.

### H8: Unknown Capability Discovery

- [ ] Add a new server-side action not present during initial integration.
- [ ] Verify hypermedia client discovers and uses it without code/tool updates.

## Phase 5: Analysis, Decisions, and Documentation

- [ ] Build final results table by scenario and mode:
  - [ ] Correctness
  - [ ] API calls
  - [ ] Invalid attempts
  - [ ] Tokens
  - [ ] Adaptation outcome
- [ ] Build final verdict table by hypothesis (H1-H8):
  - [ ] Supported
  - [ ] Partially supported
  - [ ] Not supported
  - [ ] Inconclusive
- [ ] Perform root-cause analysis for unmet hypotheses:
  - [ ] API design limitation
  - [ ] Agent prompting/tooling limitation
  - [ ] Model behavior limitation
  - [ ] Instrumentation gap
- [ ] Update summary docs with findings and keep canonical alignment:
  - [ ] Update `conversations/hateoas-for-agent-systems.md`
  - [ ] Ensure consistency with `conversations/chat-gpt-conversation.md`
- [ ] Publish final recommendation for this domain: when hypermedia is preferable, and where static contracts remain sufficient.

## Completion Gates

- [ ] Every hypothesis H1-H8 has at least:
  - [ ] One implemented scenario test
  - [ ] One measured metric set
  - [ ] One linked evidence artifact
- [ ] All S1-S6 scenarios executed in both modes under equivalent fixture conditions.
- [ ] Reproducibility validated with repeated full-run execution.
- [ ] Report and docs are internally consistent and traceable to evidence.

## Suggested Execution Order

- [ ] Week 1: Phase 1 + Phase 2 scaffolding
- [ ] Week 2: Phase 3 instrumentation + Phase 4 H1/H2/H3 experiments
- [ ] Week 3: Phase 4 H4/H5/H8 + optional H6
- [ ] Week 4: Phase 5 analysis, write-up, and final decision

## Notes

- H6 may be treated as stretch scope if schedule is constrained.
- HAL-FORMS-specific expansion should be triggered if H1/H7/H8 are inconclusive with plain HAL links alone.

## Implementation Progress (Current)

- [x] Added experiment runbook to `README.md` with reproducible Maven and runtime commands.
- [x] Added baseline parity gate checklist at `test-plans/baseline-parity-checklist.md`.
- [x] Added Copilot prompt suite at `test-plans/copilot-prompt-suite.md` for S1-S6 execution.
- [x] Pivoted measurement strategy to service-side Actuator endpoints so Copilot Chat/CLI can act as the execution agent.
- [x] Enabled `http.server.requests` metrics and `httpexchanges` traces in both reference services.
- [x] Removed legacy `agents/` module and cleaned root Maven reactor.
- [x] Updated core docs (`README.md`, `test-plans/order-management.md`, `.github/copilot-instructions.md`) to Copilot-driven execution.
- [x] Added `scripts/actuator-delta.java` (JBang) to capture before/after actuator snapshots and write per-scenario JSON/CSV deltas to `test-plans/manual-results/`.
- [ ] Next: add deterministic data reset/seed operations between scenarios.
- [ ] Next: add scriptable Copilot CLI scenario execution and evidence export for S1 and S2.

## Session Status Snapshot

- [x] Repository simplified to two reference services only.
- [x] Actuator metrics/tracing enabled for both services.
- [x] Manual Copilot prompt workflow documented.
- [x] Scripted evidence capture automation implemented.
- [ ] Scenario runs recorded with before/after metric deltas.
