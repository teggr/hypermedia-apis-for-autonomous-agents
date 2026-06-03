# Hypermedia APIs for Autonomous Agents

Autonomous agents are getting better at calling tools, but most integrations still assume a static contract model: publish a fixed operation menu, hope the model chooses correctly, and keep middleware in sync every time APIs evolve.

That includes many MCP and CLI-wrapped skill patterns. They can improve ergonomics, but they also create an extra synchronization surface: wrappers, tool schemas, and skill definitions must be maintained alongside the live API. When they drift, agents either guess or fail.

This project asks a narrower and practical question:

Can we get better agent behavior by reducing dependency on published middleware and letting agents navigate the API directly, from static contracts to state-aware hypermedia affordances?

## Why This Matters

In many real workflows, the hardest part is not issuing HTTP requests. It is deciding what the valid next action is at each step.

Static contract integrations often force the agent to reason over a global operation set. That increases ambiguity, especially in multi-step workflows where the valid next action depends on runtime state.

In practice, the distinction is:

- raw API usage without discoverable contract leads to the most guessing
- OpenAPI reduces guessing by making operations discoverable
- hypermedia affordances (HAL/HAL-FORMS) reduce ambiguity further by returning what is valid now for this specific state

Static wrappers (including MCP tools and CLI skills) describe what can be called in general. HAL-FORMS-style affordances describe what should be called next. By carrying state in the response, we reduce upfront tool context, reduce invalid transitions, and reduce the need to continuously publish and maintain middleware catalogs for every workflow nuance.

For long-lived agent integrations, that shift has architectural implications:

- less brittle coupling to endpoint shapes
- fewer invalid transition attempts
- lower ambiguity in multi-step stateful flows

## Hypothesis

The working hypothesis for this experiment is:

1. Raw API usage without discoverable contracts produces the highest ambiguity and guesswork.
2. OpenAPI-discoverable conventional APIs reduce ambiguity and improve speed/accuracy, but still leave workflow and state nuances to client-side reasoning.
3. Hypermedia APIs improve speed and accuracy further for stateful workflows because valid next actions are carried with state.

This hypothesis was derived from the source conversation and tested directionally in this repository.

## What We Built

We implemented the same order-management domain in two styles:

1. Conventional REST service with static contract orientation.
2. Hypermedia service with Spring HATEOAS and HAL-FORMS affordances.

Both were exercised through Copilot-driven scenario prompts across S1-S6.

Project structure:

- `reference-services/conventional-api`
- `reference-services/hypermedia-api`
- `test-plans`
- `conversations`

## How We Measured It

We captured service-side evidence through Actuator snapshots before and after each scenario run, then produced per-scenario deltas.

Measured dimensions in current artifacts include:

- API call deltas
- invalid attempt indicators (4xx)
- status distribution and operation distribution

Operational steps and commands were moved to:

- `test-plans/experiment-runbook.md`

## Findings (Current Dataset)

From S1-S6 directional evidence:

- Hypermedia usually showed lower API-call and 4xx pressure in the more stateful scenarios.
- Conventional results were more sensitive to discovery behavior, especially where non-domain traffic appeared.
- The relative pattern supports the architectural intuition that state-aware affordances reduce next-step ambiguity.

Key supporting artifacts:

- `test-plans/manual-results/summary.md`
- `test-plans/manual-results/metrics-deltas-adjusted.csv`
- `test-plans/manual-results/normalization-notes.md`
- `test-plans/manual-results/verdict-draft.md`

## Conclusion

Given the current caveated evidence, the practical recommendation is:

1. Use static contracts where workflows are simple, stable, and OpenAPI discovery is reliably available.
2. Prefer hypermedia for stateful, multi-step agent workflows where adaptability, transition safety, and operational efficiency matter.

Put differently: middleware wrappers can still be useful, but they should be optional accelerators, not the primary source of truth for workflow decisions. OpenAPI removes much of the guessing, but it does not fully encode stateful transition nuance at runtime. Hypermedia closes that gap by publishing context-valid actions directly from the server, so the agent can talk straight to the API, act faster with less ambiguity, and depend less on constantly updated middleware definitions.

This is directional guidance, not final-proof. It is strong enough for current architecture decisions, while still leaving clear follow-up work for tighter validation.

