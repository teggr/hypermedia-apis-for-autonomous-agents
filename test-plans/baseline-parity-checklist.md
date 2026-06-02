# Baseline Parity Checklist

This checklist is used before any hypothesis evaluation run to ensure both API styles are compared fairly.

## Service and Domain Parity

- [ ] Both services build successfully from the same commit.
- [ ] Both services expose the same order fields in payloads.
- [ ] Both services implement the same status graph:
  - [ ] `PENDING -> CONFIRMED`
  - [ ] `PENDING -> CANCELLED`
  - [ ] `CONFIRMED -> SHIPPED`
  - [ ] `CONFIRMED -> CANCELLED`
  - [ ] `SHIPPED -> DELIVERED`
- [ ] Invalid transitions are rejected consistently.

## Endpoint and Capability Parity

- [ ] Conventional API supports create/get/list/confirm/ship/deliver/cancel operations.
- [ ] Hypermedia API supports equivalent business transitions via affordances.
- [ ] Hypermedia responses include state-appropriate links for next valid actions.
- [ ] Hypermedia responses omit invalid transition links from current state.

## Test and Environment Parity

- [ ] Conventional integration tests pass.
- [ ] Hypermedia integration tests pass.
- [ ] Services run on expected ports (`8080` conventional, `8081` hypermedia).
- [ ] Agent run mode is explicitly set per experiment (`conventional` or `hypermedia`).
- [ ] OpenAI model configuration is identical for both modes.

## Contract Visibility

- [ ] Conventional API OpenAPI endpoint is reachable and captured as artifact.
- [ ] Hypermedia representation samples are captured for each order state.
- [ ] Conventional API actuator metrics endpoint is reachable (`/actuator/metrics/http.server.requests`).
- [ ] Hypermedia API actuator metrics endpoint is reachable (`/actuator/metrics/http.server.requests`).
- [ ] Conventional API HTTP exchange tracing endpoint is reachable (`/actuator/httpexchanges`).
- [ ] Hypermedia API HTTP exchange tracing endpoint is reachable (`/actuator/httpexchanges`).

## Fixture and Prompt Parity

- [ ] Starting data is reset between runs.
- [ ] Scenario prompts are identical between modes.
- [ ] Timeout and retry behavior are consistent between modes.

## Signoff

- [ ] Parity gate passed for current run date.
- [ ] Reviewer initials and date recorded in run notes.
