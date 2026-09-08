# torana-routing — Declarative Route Registry & Matching Engine

## Responsibility

Implements the **YAML-driven route registry** and **request matching engine**. Every inbound request goes through this module to determine:
1. Which route (if any) it matches
2. Which pipeline to execute
3. Which backend to call
4. What auth requirements and rate-limit policy apply

Routes are defined entirely in YAML and **hot-reloadable** at runtime via Spring Cloud Config + `@RefreshScope` — no restart required to add, modify, or disable routes.

## Key Classes

### `model/`

| Class | Description |
|-------|-------------|
| `RouteDefinition` | YAML-bound POJO for a single route: id, `MatchPredicate`, protocol list, auth config, rate-limit-ref, pipeline-ref, backend-ref, timeout, metadata. Bound from `torana.routes[]`. |
| `MatchPredicate` | Route matching criteria: `path` (Ant pattern), `methods` (HTTP verbs), `headers` (required header key-value pairs), `queryParams` (required query params). |
| `RouteAuthConfig` | Per-route auth config: `required` boolean, `scopes` list, `allowAnonymous` boolean. |

### `registry/`

| Class | Description |
|-------|-------------|
| `RouteRegistry` | `@RefreshScope` bean. Holds the current route list in a `CopyOnWriteArrayList<RouteDefinition>`. On `@RefreshScope` refresh, atomically replaces the list. Exposes `Mono<RouteDefinition> match(ServerWebExchange)`. |
| `RouteRegistryRefreshListener` | Listens for Spring Cloud `RefreshEvent`. Triggers `RouteRegistry` reload by calling `ToranaProperties` to re-read updated route list from config server. |
| `RouteRegistrar` | At startup, loads all routes from `ToranaProperties.routes` into `RouteRegistry`. Validates that all `pipeline-ref` and `backend-ref` values exist in their respective registries. |

### `matcher/`

| Class | Description |
|-------|-------------|
| `RouteMatcher` | Core matching logic. Given a `ServerWebExchange`, iterates routes in priority order. Priority: exact path > longest prefix > wildcard `**`. Returns the first matching `RouteDefinition`. |
| `PathPatternMatcher` | Wraps Spring `PathPatternParser` for Ant-style path matching (`/api/v1/**`, `/chat/{sessionId}`). |
| `HeaderPredicateMatcher` | Checks required headers: exact match, regex match, presence-only. |
| `MethodPredicateMatcher` | Checks HTTP method: exact list or `*` wildcard. |
| `CompositePredicateMatcher` | Combines path + method + header + query param matchers with AND logic. |

### `filter/`

| Class | Description |
|-------|-------------|
| `RoutingWebFilter` | Spring `WebFilter`. Runs after auth filters. Calls `RouteMatcher.match()`. If matched: populates `AgentContext` with `RouteDefinition`, stores in `ServerWebExchange.attributes`. If not matched: returns 404 with structured error body. |
| `RouteContextPopulator` | Copies route-specific configuration into `AgentContext`: resolved pipeline, resolved backend, auth requirements, timeout. |

### `reload/`

| Class | Description |
|-------|-------------|
| `HotReloadConfig` | `@Configuration` class enabling `@RefreshScope` support. Imports Spring Cloud Context. |
| `RouteChangeEvent` | Application event published when routes are reloaded. Carries old route list and new route list. |
| `RouteChangeAuditLogger` | Listens for `RouteChangeEvent` and emits an audit log entry: who triggered the reload (config server push), what changed. |

## Route Matching Priority Algorithm

```
1. Exact match:     path = "/api/v1/chat"    → highest priority
2. Prefix match:    path = "/api/v1/**"      → ordered by prefix length (longest first)
3. Wildcard match:  path = "/**"             → lowest priority / catch-all
Ties broken by declaration order in YAML.
```

## YAML Configuration

```yaml
torana:
  routes:
    - id: exact-health
      match:
        path: /health
        methods: [GET]
      auth:
        required: false
        allow-anonymous: true
      backend-ref: health-stub    # static response connector

    - id: agent-chat-route
      match:
        path: /api/v1/chat/**
        methods: [POST]
        headers:
          X-Agent-Type: "openai-compat"
      protocols: [mcp, rest]
      auth:
        required: true
        scopes: [chat:write]
        allow-anonymous: false
      rate-limit-ref: llm-tier
      pipeline-ref: rag-chat-pipeline
      timeout: 60s
      metadata:
        owner: platform-team
        sla: tier-1

    - id: docs-passthrough
      match:
        path: /api/v1/docs/**
        methods: [GET, HEAD]
      auth:
        required: true
        scopes: [docs:read]
      rate-limit-ref: standard
      backend-ref: s3-docs       # direct passthrough, no pipeline
      timeout: 10s
```

## Development Phases

### Phase 1A — Core Routing (implement early)
- [ ] Implement `RouteDefinition`, `MatchPredicate`, `RouteAuthConfig` POJOs
- [ ] Implement `PathPatternMatcher`, `MethodPredicateMatcher`, `CompositePredicateMatcher`
- [ ] Implement `RouteMatcher` with priority ordering
- [ ] Implement `RouteRegistry` (non-refreshable first)
- [ ] Implement `RouteRegistrar` with startup validation
- [ ] Implement `RoutingWebFilter` and `RouteContextPopulator`
- [ ] Unit test: 20+ route permutations, priority ordering, 404 on no match

### Phase 2A — Hot Reload
- [ ] Add `@RefreshScope` to `RouteRegistry`
- [ ] Implement `RouteRegistryRefreshListener`
- [ ] Implement `RouteChangeEvent` + `RouteChangeAuditLogger`
- [ ] Integration test: push new route to Config Server → gateway serves it within 5s without restart

### Phase 4 — Multi-tenant
- [ ] Add tenant namespace to route matching: routes can be scoped to a tenant ID
- [ ] Route priority becomes: `(tenantId + exact path)` > `(tenantId + prefix)` > `(global exact)` > `(global prefix)`

## Package Layout

```
com.phaselume.torana.routing
├── model/
│   ├── RouteDefinition.java
│   ├── MatchPredicate.java
│   └── RouteAuthConfig.java
├── registry/
│   ├── RouteRegistry.java
│   ├── RouteRegistrar.java
│   └── RouteRegistryRefreshListener.java
├── matcher/
│   ├── RouteMatcher.java
│   ├── PathPatternMatcher.java
│   ├── HeaderPredicateMatcher.java
│   ├── MethodPredicateMatcher.java
│   └── CompositePredicateMatcher.java
├── filter/
│   ├── RoutingWebFilter.java
│   └── RouteContextPopulator.java
└── reload/
    ├── HotReloadConfig.java
    ├── RouteChangeEvent.java
    └── RouteChangeAuditLogger.java
```
