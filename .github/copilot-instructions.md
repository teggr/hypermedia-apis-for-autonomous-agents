# Copilot Instructions — Hypermedia APIs for Autonomous Agents

## Purpose

This repository is an experimental investigation into whether **hypermedia-driven APIs** (HATEOAS / REST Level 3) provide meaningful benefits for autonomous AI agents over the more conventional **MCP (Model Context Protocol) or static skill/tool-based** approaches.

The investigation is grounded in a conversation summarised in [`conversations/hateoas-for-agent-systems.md`](../conversations/hateoas-for-agent-systems.md).

## First Principles

### 1. Agents Should Discover, Not Be Told

Prefer designs where the server communicates what an agent *can do next* based on current state, rather than requiring the agent to have pre-loaded knowledge of the entire API surface. Embed affordances (links, actions) directly in responses.

### 2. State Drives Capability

API responses should include only the transitions that are valid from the current resource state. An agent that follows these links cannot trigger invalid state transitions — the server is the source of truth for what is permissible, not the agent's tool registry.

### 3. Loose Coupling Over Tight Integration

Avoid designs where agents depend on hardcoded URLs, fixed parameter schemas, or static OpenAPI/MCP definitions that break when the API evolves. Prefer link relations and semantic types that remain stable as implementations change.

### 4. Compare Fairly

When building reference services, implement the *same domain* (e.g. an order management workflow) in both a conventional REST + MCP style and a hypermedia style, so comparisons are meaningful and apples-to-apples.

### 5. Measure What Matters for Agents

Evaluation should cover:
- **Correctness**: Does the agent complete the task successfully?
- **Efficiency**: How many API calls / tokens does each approach require?
- **Adaptability**: How well does the agent cope with API changes?
- **Robustness**: Does the agent attempt invalid operations or hallucinate endpoints?
- **Developer experience**: How much wiring is needed to integrate a new agent?

## Technology Stack

All reference services and tests use:

- **Spring Boot** — service foundation
- **Spring HATEOAS** — hypermedia response building (HAL / HAL-FORMS)
- **Spring AI** — AI agent orchestration and tool/function calling

When writing service code, follow idiomatic Spring Boot conventions: `@RestController`, `@Service`, `@Repository`, constructor injection, and standard Spring application packaging.

When using Spring HATEOAS:
- Model resources as `RepresentationModel` subclasses or use `EntityModel` / `CollectionModel` wrappers
- Build links using `WebMvcLinkBuilder.linkTo` / `methodOn`
- Use HAL as the default media type; consider HAL-FORMS for action affordances

When using Spring AI:
- Use the `ChatClient` / `AgentExecutor` abstractions
- Register tools via `@Tool` or the `FunctionCallback` API
- Write agents that operate against both the hypermedia and the conventional API variants to enable comparison

## Repository Structure (target)

```
conversations/          # Source conversations that informed the investigation
reference-services/
  conventional-api/     # Spring Boot REST API exposed via OpenAPI + MCP tools
  hypermedia-api/       # Spring Boot REST API using Spring HATEOAS (HAL/HAL-FORMS)
agents/
  spring-ai-agent/      # Spring AI agent that can target either service variant
test-plans/             # Scenario-based test plans and evaluation criteria
docs/                   # Findings, design decisions, evaluation results
```

## What To Build Next

1. **Reference domain**: choose a simple but realistic workflow domain (e.g. order management, content publishing) with 3–5 state transitions
2. **Conventional API service**: expose the domain as a plain REST API with an OpenAPI spec and MCP tool definitions
3. **Hypermedia API service**: expose the same domain using Spring HATEOAS with HAL-FORMS affordances
4. **Spring AI agent**: implement an agent that can be pointed at either service and tasked with completing workflows
5. **Test plan**: define scenarios, metrics, and pass/fail criteria for comparing the two approaches

## Code Style

- Java 21+ with records and sealed types where appropriate
- Tests use JUnit 5 and Spring Boot Test; use `@SpringBootTest` for integration tests
- Follow standard Maven project layout unless there is a compelling reason not to
- Keep each reference service as a self-contained Maven module
