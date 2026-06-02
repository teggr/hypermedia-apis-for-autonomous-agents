# HATEOAS for Agent Systems

**Source:** https://chatgpt.com/share/6a1ef696-1604-832c-8786-5f8795f95acd

> This conversation explored the potential benefits of hypermedia-driven APIs (HATEOAS) for autonomous AI agents, compared to the more typical MCP (Model Context Protocol) or skill/tool-based approaches.

## Summary of Key Points

### The Problem with Static API Contracts for Agents

Current agent frameworks (LangChain, Spring AI, etc.) typically expose APIs to agents as a static set of tools or skills — the agent is pre-programmed with knowledge of available operations (URLs, parameters, schemas). This creates tight coupling:

- Agents need out-of-band documentation (OpenAPI specs, MCP tool definitions) to know what they can do
- When the API evolves, agent integrations must be updated
- The agent cannot discover new capabilities at runtime
- Agents often receive full capability lists regardless of current state/context

### What Hypermedia Offers

HATEOAS (Hypertext As The Engine Of Application State) is a REST constraint where the server drives application state by returning links and actions alongside data. Applied to agent APIs:

- **Dynamic capability discovery**: The server tells the agent what it can do *from the current state*, not what it can do in general
- **State-aware affordances**: Only valid transitions/actions are included in responses, reducing hallucination risk
- **Self-describing interfaces**: Agents can navigate an API by following links without pre-loaded tool schemas
- **Reduced brittleness**: Agents are less sensitive to API changes because they follow links rather than hardcoded paths

### Potential Benefits for Autonomous Agents

1. **Reduced context pollution**: Instead of loading every possible tool, agents receive only the links/actions valid in the current context
2. **Server-side guard-rails**: The server controls what the agent can attempt next, preventing invalid state transitions
3. **Evolvability**: APIs can add, change, or deprecate operations without breaking agents that navigate by following links
4. **Composability**: Agents can navigate multi-step workflows by following hypermedia trails, similar to a human browsing a UI

### Challenges and Open Questions

- Do LLM-based agents actually benefit from hypermedia? Can they interpret `_links` / HAL / Siren structures?
- How does performance compare — does the extra round-trip discovery overhead matter?
- What is the right hypermedia format for agents (HAL, Siren, JSON-LD, custom)?
- How do hypermedia APIs compare to MCP for structured tool invocation?

### Proposed Investigation

Build reference services using **Spring Boot + Spring HATEOAS** and compare:

1. A traditional REST API with an OpenAPI/MCP tool definition
2. A hypermedia-driven API where the agent discovers capabilities at runtime

Evaluate both approaches using **Spring AI** agents against defined test plans covering correctness, efficiency, adaptability, and robustness.
