# Could Hypermedia APIs Be Better for AI Agents Than MCP Tools?

## Initial Question

Currently agents typically use a skill with a CLI or MCP to talk to third-party systems. Most of those are a facade over a company's API.

Would using a HATEOAS API with sufficient links and navigation and a generic client—or even plain curl—be a better approach for agents?

It feels like an agent could navigate and browse the API much like a website, but using the API instead.

---

## Why Hypermedia Might Be Better Suited to Agents

Today's architecture usually looks like:

```text
Agent
  ↓
Tool / MCP Server
  ↓
Company API
  ↓
Backend
```

The MCP server essentially acts as a navigation guide:

```text
search_customer()
create_invoice()
send_invoice()
```

The agent relies on the tool definitions and descriptions to know what actions are available.

With HATEOAS, the API itself becomes discoverable.

Example:

```json
{
  "customer": {
    "id": 123,
    "name": "Acme"
  },
  "_links": {
    "invoices": "/customers/123/invoices",
    "orders": "/customers/123/orders",
    "contacts": "/customers/123/contacts"
  }
}
```

An agent can explore:

```text
GET /
→ discover customers

GET /customers/123
→ discover invoices

GET /customers/123/invoices
→ discover payment actions
```

This resembles how humans browse websites.

---

## Advantages for Agents

### 1. Runtime Discovery

Traditional tools require prior knowledge:

```text
create_invoice
search_customer
send_invoice
```

Hypermedia APIs can advertise capabilities dynamically:

```json
{
  "_links": {
    "create-invoice": {},
    "analytics": {},
    "bulk-import": {}
  }
}
```

The agent discovers capabilities rather than memorizing them.

---

### 2. More Resilient to Change

Tool wrappers often encode implementation details:

```python
def create_invoice(...):
    pass
```

When APIs change, wrappers must be updated.

With hypermedia:

* Links change
* Workflows change
* Actions change

The client simply follows the current graph.

This was the original promise of HATEOAS.

---

### 3. Better Alignment with LLM Capabilities

LLMs are naturally good at:

* Reading structured documents
* Following links
* Building mental models
* Planning navigation

A hypermedia API presents exactly those primitives.

Reasoning becomes:

```text
I need invoices.

I see an invoices link.

Follow it.
```

instead of:

```text
Call tool create_invoice with parameters X and Y.
```

---

### 4. State-Aware Workflows

Hypermedia can expose actions that are valid for the current resource state.

```json
{
  "_actions": {
    "approve": {
      "method": "POST",
      "href": "/orders/123/approve"
    },
    "cancel": {
      "method": "POST",
      "href": "/orders/123/cancel"
    }
  }
}
```

The API tells the agent what is possible right now.

---

## Why Hypermedia Didn't Win Historically

### Human Developers Preferred SDKs

Most developers prefer:

```python
client.create_invoice(...)
```

over:

```python
follow_link("create-invoice")
```

HATEOAS was largely rejected because developers preferred explicit APIs.

---

### Agents Need Semantics

A link:

```json
{
  "rel": "approve"
}
```

is not enough.

Agents benefit from richer metadata:

```json
{
  "title": "Approve Purchase Order",
  "description": "Final approval. Irreversible."
}
```

---

### More Round Trips

A tool call:

```text
create_invoice(...)
```

might be one request.

Hypermedia navigation could require:

```text
GET /
GET customer
GET invoices
POST invoice
```

which increases latency.

---

## Enter HAL-FORMS

At this point the discussion shifted toward HAL-FORMS.

### Why HAL Alone Isn't Enough

HAL might provide:

```json
{
  "_links": {
    "approve": {
      "href": "/orders/123/approve"
    }
  }
}
```

The agent knows the action exists but not much else.

---

### HAL-FORMS Adds Action Templates

```json
{
  "_templates": {
    "approve": {
      "method": "POST",
      "target": "/orders/123/approve",
      "properties": [
        {
          "name": "comment",
          "required": false
        }
      ]
    }
  }
}
```

Now the agent can discover:

* The action
* The HTTP method
* The endpoint
* Required fields
* Optional fields

without external documentation.

---

## HAL-FORMS Already Contains Useful Agent Semantics

A key observation:

HAL-FORMS includes titles and prompts.

Example:

```json
{
  "_templates": {
    "approve": {
      "title": "Approve Purchase Order",
      "method": "POST",
      "target": "/orders/123/approve",
      "properties": [
        {
          "name": "comment",
          "prompt": "Reason for approval"
        }
      ]
    }
  }
}
```

For an LLM:

* `title` explains intent
* `prompt` explains expected input
* `method` explains execution
* `target` explains location

This looks remarkably similar to a tool definition:

```json
{
  "name": "approve_purchase_order",
  "description": "Approve Purchase Order",
  "parameters": {
    "comment": {
      "description": "Reason for approval"
    }
  }
}
```

The difference is that the definition is embedded within the API state itself.

---

## Dynamic Workflows

Imagine a purchase request moving through states.

```text
DRAFT
↓
SUBMITTED
↓
APPROVED
```

In DRAFT:

```text
submit
edit
delete
```

In SUBMITTED:

```text
approve
reject
```

In APPROVED:

```text
archive
```

The agent only sees actions that are valid at the current state.

This significantly reduces planning errors.

---

## Remaining Gap

HAL-FORMS already provides:

* title
* prompt
* required
* method
* target

The main missing piece is machine-readable consequences.

For example:

```json
{
  "effects": [
    "status=approved"
  ],
  "reversible": false
}
```

This would help agents reason about outcomes.

---

# How Could This Be Tested?

The most interesting test is not performance.

It's:

> Can the agent successfully complete tasks when it has never seen the API before?

---

## Test 1: Zero Documentation

Give the agent:

* Base URL
* Authentication
* Goal

Example:

```text
Create a customer called Acme Ltd.
Create a £500 invoice.
Email it to the customer.
```

The agent discovers everything through HAL-FORMS.

Compare against:

* MCP tools
* OpenAPI ingestion
* Custom SDKs

Metrics:

* Success rate
* API calls
* Token usage
* Errors

---

## Test 2: API Evolution

Version 1:

```http
POST /customers/{id}/invoice
```

Version 2:

```http
POST /billing/invoices
```

Only update the links/templates.

A hypermedia client should continue working.

---

## Test 3: Unknown Features

Add a capability the agent has never encountered.

```json
{
  "_templates": {
    "issue-credit-note": {
      "title": "Issue Credit Note"
    }
  }
}
```

Ask:

```text
Reverse invoice INV-123
```

Can the agent discover the feature?

---

## Test 4: Workflow Navigation

Example workflow:

```text
Draft
↓
Submitted
↓
Manager Approval
↓
Finance Approval
↓
Completed
```

Ask:

```text
Get this request approved
```

The agent must:

1. Inspect state
2. Discover actions
3. Execute actions
4. React to failures

This is where hypermedia should shine.

---

## Test 5: Generic Client

The strongest experiment:

Build a client that only understands:

```text
GET
POST
PUT
DELETE
HAL-FORMS
```

No business-specific code.

If an agent can operate the system through this generic interface, the API is genuinely self-describing.

---

# Spring Boot + Spring HATEOAS Prototype

A practical implementation could be built using Spring Boot and Spring HATEOAS.

Rather than CRUD, use a workflow-based domain.

---

## Purchase Approval Example

Model:

```text
Purchase Request

id
amount
description
status
```

Statuses:

```text
DRAFT
SUBMITTED
APPROVED
REJECTED
```

---

## Draft Representation

```json
{
  "id": 123,
  "status": "DRAFT",

  "_links": {
    "self": {
      "href": "/requests/123"
    }
  },

  "_templates": {
    "submit": {
      "title": "Submit Purchase Request",
      "method": "POST",
      "target": "/requests/123/submit"
    },

    "edit": {
      "title": "Edit Purchase Request",
      "method": "PUT",
      "target": "/requests/123",
      "properties": [
        {
          "name": "amount",
          "prompt": "Requested amount"
        },
        {
          "name": "description",
          "prompt": "Business justification"
        }
      ]
    }
  }
}
```

---

## Submitted Representation

```json
{
  "id": 123,
  "status": "SUBMITTED",

  "_templates": {
    "approve": {
      "title": "Approve Purchase Request",
      "method": "POST",
      "target": "/requests/123/approve"
    },

    "reject": {
      "title": "Reject Purchase Request",
      "method": "POST",
      "target": "/requests/123/reject",
      "properties": [
        {
          "name": "reason",
          "prompt": "Reason for rejection"
        }
      ]
    }
  }
}
```

The workflow becomes discoverable.

---

## Generic Hypermedia Client

The client should understand only:

```java
class HypermediaAgentClient {

    Resource get(String uri);

    List<Action> availableActions(Resource resource);

    Resource invoke(
        Action action,
        Map<String,Object> values
    );
}
```

No business-specific logic.

---

## Build Two Systems

### System 1

Purchase Requests

```text
submit
approve
reject
```

### System 2

Support Tickets

```text
open
assign
close
escalate
```

Expose both via HAL-FORMS.

If the same agent client can operate both systems without new tool definitions, you've demonstrated discoverability.

---

## Demonstrate API Evolution

Version 1:

```http
POST /requests/{id}/approve
```

Version 2:

```http
POST /workflow/tasks/{id}/approve
```

Only update links/templates.

The agent continues to work unchanged.

This is the classic HATEOAS promise applied to AI agents.

---

## Spring HATEOAS Features to Use

Useful Spring HATEOAS features include:

* `RepresentationModel`
* `EntityModel`
* `CollectionModel`
* `Affordances`
* HAL-FORMS support

Example:

```java
linkTo(methodOn(RequestController.class)
    .approve(id))
    .withRel("approve")
    .andAffordance(
        afford(methodOn(RequestController.class)
            .approve(id))
    );
```

Affordances are especially interesting because they already model discoverable actions.

---

## Metrics to Compare

| Metric                    | Purpose                   |
| ------------------------- | ------------------------- |
| Task Success              | Can the goal be achieved? |
| HTTP Requests             | Navigation overhead       |
| Token Usage               | Agent cost                |
| Hardcoded Knowledge       | Discoverability           |
| Success After API Changes | Resilience                |

Example outcome:

```text
OpenAPI Agent

Before change: 95%
After change: 40%

HAL-FORMS Agent

Before change: 90%
After change: 88%
```

Even if hypermedia is slightly less efficient, resilience to change may be the property that makes it valuable for long-lived agent integrations.

---

# Conclusion

The core hypothesis is:

> HATEOAS may have been ahead of its time.

Human developers generally preferred SDKs, generated clients, and explicit APIs.

Agents are different.

They are closer to web browsers than software engineers:

* They discover
* They navigate
* They reason from context
* They adapt to change

HAL-FORMS is particularly interesting because it combines:

* Hypermedia navigation
* Self-describing actions
* Field prompts
* Human-readable titles
* State-dependent workflows

In many ways it starts to look like a dynamically generated tool catalog embedded directly into the API.

A Spring Boot + Spring HATEOAS prototype with a generic agent client would be a practical way to evaluate whether hypermedia APIs can reduce the need for bespoke MCP servers and tool wrappers while improving resilience and discoverability for AI agents.
