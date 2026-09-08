# torana-security-authz-opa — OPA Authorization Engine

## Responsibility

Implements **fine-grained authorization (AuthZ)** using **Open Policy Agent (OPA)** as an external policy decision point. This module's job is to decide, for every authenticated request, whether the caller is **permitted to perform the requested action** on the matched route.

OPA enables enterprise-grade policy scenarios:
- **RBAC**: Role-based access control via JWT scopes
- **ABAC**: Attribute-based control (tenant, department, data classification)
- **Route-level deny lists**: Instantly disable specific routes without redeployment
- **Obligations**: Return data-masking rules, downstream header injections, audit tags alongside allow/deny

OPA is called as a **reactive HTTP sidecar** — decoupled from the Java process, hot-reloadable policies, and auditable.

## Key Classes

### `client/`

| Class | Description |
|-------|-------------|
| `OpaClient` | Reactive `WebClient` wrapper around OPA's Data API. Posts `POST /v1/data/{policy_path}` with the full `AgentContext` serialized as the OPA `input` document. Returns `Mono<OpaResponse>`. Respects configurable `timeout` and circuit breaker (via `torana-resilience`). |
| `OpaInputBuilder` | Serializes `AgentContext` into the OPA input JSON document. Includes: principal (name, scopes, claims, tenant, auth method), route (id, path, required scopes), request (method, path, headers subset, body hash), environment (timestamp, client IP). |
| `OpaHealthIndicator` | Spring Actuator `HealthIndicator`. Pings `GET /health` on OPA sidecar. |

### `model/`

| Class | Description |
|-------|-------------|
| `OpaRequest` | Jackson-serializable OPA evaluation request: `{ "input": { ... } }` |
| `OpaResponse` | Jackson-deserialized OPA response: `{ "result": { "allow": true, "obligations": [...], "deny_reason": "..." } }` |
| `OpaObligation` | A single obligation returned by OPA: `type` (mask-field, inject-header, add-audit-tag) and `params` map. |

### `cache/`

| Class | Description |
|-------|-------------|
| `OpaDecisionCache` | Redis-backed cache for OPA decisions. Cache key: SHA-256 of (policy path + serialized input). TTL: `torana.security.authz.opa.cache.ttl` (default 5s). Cache is used only when `cache.enabled: true`. |
| `CacheInvalidator` | Provides `Mono<Void> invalidate(String routeId)` to flush decisions for a specific route (e.g., after policy change). Called by the admin API. |

### `filter/`

| Class | Description |
|-------|-------------|
| `AccessControlWebFilter` | Spring `WebFilter`. Runs after `AuthenticationWebFilter`. Invokes `OpaDecisionCache` (if enabled) or `OpaClient` directly. If `allow = false`, returns `403 Forbidden` with `deny_reason`. If `allow = true`, attaches `obligations` to `AgentContext` and passes to the next filter. |
| `ObligationProcessor` | Processes `OpaObligation` list from the decision: applies field masking rules to the response, injects required headers into the downstream request, adds audit tags to the `AuditEvent`. |

### Bundled Rego Policy (`src/main/resources/rego/`)

`torana-base-policy.rego` — shipped as a classpath resource, enterprise loads it into OPA as the base bundle and extends with their own `.rego` files:

```rego
package torana.v1

import future.keywords.in
import future.keywords.if

# ── Default deny ────────────────────────────────────────────────────
default allow := false

# ── Main allow rule ─────────────────────────────────────────────────
allow if {
    not route_in_deny_list
    not tenant_mismatch
    has_required_scope
}

# ── Scope check ─────────────────────────────────────────────────────
has_required_scope if {
    required := data.routes[input.route.id].required_scopes
    some scope in required
    scope in input.principal.scopes
}

# ── Route deny list ─────────────────────────────────────────────────
route_in_deny_list if {
    input.route.id in data.deny_list
}

# ── Tenant isolation ────────────────────────────────────────────────
tenant_mismatch if {
    input.route.tenant_id != ""
    input.route.tenant_id != input.principal.tenant_id
}

# ── Obligations ─────────────────────────────────────────────────────
obligations := obs if {
    allow
    obs := [
        ob | data.masking_rules[input.route.id][ob]
    ]
} else := []

deny_reason := msg if {
    not allow
    route_in_deny_list
    msg := "Route is disabled"
} else := "Insufficient permissions"
```

## OPA Input Document Structure

```json
{
  "input": {
    "principal": {
      "name": "user@corp.com",
      "scopes": ["chat:write", "docs:read"],
      "claims": { "department": "engineering" },
      "tenant_id": "acme-corp",
      "auth_method": "jwt-oidc"
    },
    "route": {
      "id": "agent-chat-route",
      "path": "/api/v1/chat",
      "required_scopes": ["chat:write"],
      "tenant_id": ""
    },
    "request": {
      "method": "POST",
      "path": "/api/v1/chat/completions",
      "client_ip": "10.0.1.5",
      "body_hash": "sha256:abc123...",
      "headers": { "X-Custom": "value" }
    },
    "environment": {
      "timestamp": "2026-09-08T09:00:00Z"
    }
  }
}
```

## Development Phases

### Phase 1D — Core OPA Integration
- [ ] Implement `OpaClient` with reactive `WebClient`
- [ ] Implement `OpaInputBuilder` (principal + route + request)
- [ ] Implement `OpaRequest` / `OpaResponse` / `OpaObligation` model
- [ ] Implement `AccessControlWebFilter`
- [ ] Ship `torana-base-policy.rego` as classpath resource
- [ ] Integration test (Testcontainers OPA): allow → 200; deny → 403
- [ ] Implement `OpaHealthIndicator`

### Phase 2A — Caching + Obligations
- [ ] Implement `OpaDecisionCache` (Redis)
- [ ] Implement `CacheInvalidator`
- [ ] Implement `ObligationProcessor` (header injection + audit tags)
- [ ] Test: cache hit rate, TTL expiry, invalidation

### Phase 3 — Advanced Rego
- [ ] Ship multi-tenant Rego policy extensions
- [ ] OPA Bundle API polling (for hot-reload without OPA restart)
- [ ] Add field-masking obligation type to `ObligationProcessor`

## Package Layout

```
com.phaselume.torana.security.authz.opa
├── client/
│   ├── OpaClient.java
│   ├── OpaInputBuilder.java
│   └── OpaHealthIndicator.java
├── model/
│   ├── OpaRequest.java
│   ├── OpaResponse.java
│   └── OpaObligation.java
├── cache/
│   ├── OpaDecisionCache.java
│   └── CacheInvalidator.java
└── filter/
    ├── AccessControlWebFilter.java
    └── ObligationProcessor.java
```
