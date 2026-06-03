# Hypermedia APIs for Autonomous Agents

Autonomous agents are getting better at calling tools, but most integrations still assume a static contract model: give the model a fixed list of operations, hope it chooses well, and update the integration every time APIs evolve.

That includes many MCP and CLI-wrapped skill patterns. They improve invocation ergonomics, and they can carry more state if the request/response loop is designed to do so, but they still drift when contracts go stale and they still expose broad action menus unless enough state context is carried with each step.

This project asks a narrower and practical question:

Can we get better agent behavior by moving from static API contracts to state-aware hypermedia affordances?

## Why This Matters

In many real workflows, the hardest part is not issuing HTTP requests. It is deciding what the valid next action is at each step.

Static contract integrations often force the agent to reason over a global operation set. Hypermedia aims to reduce that burden by returning context-valid transitions directly in each response.

In practice, this is the key distinction: static wrappers (including MCP tools and CLI skills) describe what can be called in general, while HAL-FORMS-style affordances describe what should be called now, for this state. By carrying state in the response, we also reduce the amount of static context we need to load upfront, because we do not need every tool or skill definition in memory before deciding the next action. That state should include both the state of the data and the state of the authorized user, because fixed contracts do not always encode the runtime permissions and context that determine whether an action is actually valid. When we call the API directly, we cut out the middleman and reduce synchronization issues between an external wrapper and the live system state.

For long-lived agent integrations, that shift has architectural implications:

- less brittle coupling to endpoint shapes
- fewer invalid transition attempts
- lower ambiguity in multi-step stateful flows

## Hypothesis

The working hypothesis for this experiment is:

1. Raw API usage without discoverable contracts produces the highest ambiguity and guesswork.
2. OpenAPI-discoverable conventional APIs improve speed and accuracy when docs are found and used.
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

## Caveats You Should Take Seriously

These findings are intentionally caveated.

Known limits in current evidence:

- some runs were interruption-prone and required time-based adjustment
- conventional runs can be skewed by non-domain traffic (docs, swagger, actuator)
- token usage was projected directionally, not directly instrumented per run
- several hypotheses still require targeted experiments (for example H2/H4/H7/H8)

Key supporting artifacts:

- `test-plans/manual-results/summary.md`
- `test-plans/manual-results/metrics-deltas-adjusted.csv`
- `test-plans/manual-results/normalization-notes.md`
- `test-plans/manual-results/verdict-draft.md`

## Conclusion

Given the current caveated evidence, the practical recommendation is:

1. Use static contracts where workflows are simple, stable, and OpenAPI/MCP discovery is reliably available.
2. Prefer hypermedia for stateful, multi-step agent workflows where adaptability and transition safety matter.

Put differently: MCP and CLI-wrapped skills are helpful, and they can be improved by carrying state through their requests and responses, but without that they inherit the same core limitations as other static contracts. Direct API traversal removes an extra synchronization layer, so the agent stays closer to the source of truth and avoids loading unnecessary upfront tool context while also keeping user authorization and data state in sync.

This is directional guidance, not final-proof. It is strong enough for current architecture decisions, while still leaving clear follow-up work for tighter validation.

## Where This Goes Next

The next high-value improvements are:

1. deterministic reset/seed between runs
2. scripted Copilot CLI execution for cleaner repeatability
3. targeted follow-up experiments for unresolved hypotheses

Progress tracking remains in:

- `ROADMAP.md`

## Canonical Sources

If summary docs diverge, treat the full conversation as canonical:

- `conversations/chat-gpt-conversation.md`
- `conversations/hateoas-for-agent-systems.md`
