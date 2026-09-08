# torana-core — Kernel: Domain Model & SPI Interfaces

## Responsibility

`torana-core` is the **foundation of the entire framework**. It contains:

1. **Domain model** — protocol-agnostic request/response/context value objects
2. **SPI (Service Provider Interface) contracts** — every extension point in the framework is defined here as a Java interface
3. **Configuration schema** — `ToranaProperties` binds the entire YAML configuration tree

**Critical constraint**: This module has **zero** Spring Boot Starter dependencies and **zero** implementations. It only depends on `spring-boot-autoconfigure` (for `@ConfigurationProperties`) and `reactor-core`. Every other Torana module depends on this one. This ensures the SPI contracts stay stable and light.

## Key Types

### Domain Model (`model/`)

| Class | Description |
|-------|-------------|
| `AgentRequest` | Normalized, protocol-agnostic inbound request. Carries: method, path, headers, body as `DataBuffer`, query params, protocol type, raw exchange reference. |
| `AgentResponse` | Normalized outbound response wrapping a `Flux<Chunk>` for streaming or a single `Mono<byte[]>` for buffered responses. |
| `AgentResponse.Chunk` | A single streaming unit: delta content, finish reason, token count (for LLM), metadata. |
| `AgentContext` | **Immutable** propagation object that flows through the entire reactive chain. Carries: `AgentRequest`, `ToranaAuthentication`, matched `RouteDefinition`, `AgentContext.Obligations` (from OPA), trace ID, tenant ID, span. Supports `withXxx()` copy-on-modify methods. |
| `ToranaAuthentication` | Authenticated principal: name, scopes, claims map, auth method, tenant ID, raw token (opaque). |
| `AuthzDecision` | OPA authorization result: `allow` boolean, `obligations` list, `denyReason` optional string. |
| `RateLimitDecision` | Rate limit check result: `allowed`, `remaining`, `retryAfterSeconds`, key used. |
| `AuditEvent` | Structured audit record: principal, route ID, action, outcome, latency millis, payload hash (SHA-256 of body), timestamp, tenant ID, trace ID. Never contains raw payloads. |
| `BackendCredentials` | Short-lived credentials fetched from Vault: type (Bearer/Basic/AWS STS), value, expiry instant. |
| `ConnectorConfig` | YAML-bound connector configuration snapshot passed to `BackendConnector.execute()`. |
| `ResilienceProfile` | Snapshot of a named resilience profile (CB thresholds, retry config, timeout). |
| `RateLimitPolicy` | Snapshot of a named rate limit policy (rpm, burst, key strategy, on-exceed action). |

### SPI Interfaces (`spi/`)

| Interface | Contract | Used By |
|-----------|----------|---------|
| `ProtocolAdapter` | `String protocol()` — identifies protocol name.<br>`RouterFunction<ServerResponse> routerFunction()` — registers HTTP routes.<br>`Mono<AgentRequest> decode(ServerWebExchange)` — parse wire message to domain model.<br>`Mono<Void> encode(AgentContext, Flux<Chunk>, ServerWebExchange)` — write response to wire. | `torana-protocol-*` modules |
| `AuthenticationProvider` | `String type()` — matches YAML `type:` key.<br>`Mono<ToranaAuthentication> authenticate(ServerWebExchange)` — extract + validate credentials. Empty `Mono` means "not applicable for this request". | `torana-security-authn` |
| `AuthorizationEngine` | `Mono<AuthzDecision> authorize(AgentContext)` — evaluate policy for a fully-authenticated context. | `torana-security-authz-opa` |
| `CredentialBroker` | `Mono<BackendCredentials> broker(AgentContext, String backendRef)` — fetch short-lived credentials for a named backend. | `torana-security-vault` |
| `BackendConnector` | `String type()` — matches YAML `type:` key.<br>`boolean supports(ConnectorConfig)` — can this connector handle the given config?<br>`Flux<AgentResponse.Chunk> execute(AgentContext, ConnectorConfig)` — call backend, return streaming response. | `torana-connector-*` modules |
| `PipelineStep` | `String type()` — matches YAML step `type:` key.<br>`Mono<AgentContext> execute(AgentContext)` — transform context or produce side effects. | `torana-pipeline` |
| `RateLimiter` | `Mono<RateLimitDecision> check(AgentContext, RateLimitPolicy)` — evaluate rate limit for this request. | `torana-ratelimit-redis` |
| `AuditSink` | `String type()` — matches YAML sink `type:` key.<br>`Mono<Void> publish(AuditEvent)` — emit audit event to the sink. | `torana-observability` |
| `ResilienceDecorator` | `<T> Flux<T> decorate(String connectorId, Supplier<Flux<T>> call)` — wrap a connector call with resilience patterns. | `torana-resilience` |

### Configuration Schema (`config/`)

| Class | Description |
|-------|-------------|
| `ToranaProperties` | Root `@ConfigurationProperties(prefix = "torana")`. All YAML keys map here. Sub-properties: `ProtocolsProperties`, `SecurityProperties`, `RoutingProperties`, `PipelineProperties`, `ResilienceProperties`, `RateLimitProperties`, `ConnectorProperties`, `ObservabilityProperties`. |
| `RouteDefinition` | A single YAML route entry: id, match predicate (path, methods, headers), protocol list, auth config, rate-limit-ref, pipeline-ref, backend-ref, timeout, metadata map. |
| `PipelineDefinition` | A named pipeline: timeout, on-error (fallback pipeline name), ordered list of `StepDefinition`. |
| `StepDefinition` | A pipeline step: `type` (matches `PipelineStep.type()`), and an arbitrary `Map<String, Object> params` passed to the step. |
| `ConnectorDefinition` | A named connector: `type`, all connector-specific properties, `resilience-profile` reference. |
| `ResilienceProfileDefinition` | Named resilience profile with CB, bulkhead, retry, time-limiter sub-configs. |
| `RateLimitPolicyDefinition` | Named rate limit policy: rpm, burst, key-by strategy, on-exceed action. |

### Exceptions (`exception/`)

| Exception | When Thrown |
|-----------|-------------|
| `ToranaException` | Base runtime exception for all Torana errors |
| `AuthenticationException` | No valid credentials found / token invalid |
| `AuthorizationException` | OPA denied; includes `denyReason` |
| `RateLimitExceededException` | Rate limit hit; includes `retryAfterSeconds` |
| `RouteNotFoundException` | No route matched the incoming request |
| `PipelineException` | Step execution failure; wraps original cause |
| `ConnectorException` | Backend call failed; wraps original cause |
| `ConfigurationException` | Invalid YAML configuration at startup |

## Development Phases

### Phase 0 — Foundation (implement first)
- [ ] Create all domain model records/classes with full Javadoc
- [ ] Define all SPI interfaces with full Javadoc and method-level contracts
- [ ] Implement `ToranaProperties` with `@Validated` bean validation on required fields
- [ ] Implement `RouteDefinition`, `PipelineDefinition`, `StepDefinition`, `ConnectorDefinition`
- [ ] Implement all custom exceptions with structured fields
- [ ] Add `@ConfigurationPropertiesBinding` converters for `Duration`, `DataSize` YAML values
- [ ] Write unit tests for all YAML binding (verify `@ConfigurationProperties` round-trips)

### Phase 1 — Enrichment
- [ ] Add `AgentContext.withObligation()` / `withClaim()` / `withTenantId()` fluent builders
- [ ] Add `AgentContext.tracing()` accessors for OpenTelemetry span propagation
- [ ] Add multi-tenancy fields to `ToranaAuthentication`

### Phase 2 — Stability
- [ ] Add JSON Schema export for `ToranaProperties` (enables IDE auto-complete for adopters)
- [ ] Add SPI validation utility: at startup, verify all YAML `*-ref` values resolve to known beans

## Package Layout

```
com.phaselume.torana.core
├── model/
│   ├── AgentRequest.java
│   ├── AgentResponse.java           (+ nested Chunk record)
│   ├── AgentContext.java            (+ nested Obligations record)
│   ├── ToranaAuthentication.java
│   ├── AuthzDecision.java
│   ├── RateLimitDecision.java
│   ├── AuditEvent.java
│   ├── BackendCredentials.java
│   ├── ConnectorConfig.java
│   ├── ResilienceProfile.java
│   └── RateLimitPolicy.java
├── spi/
│   ├── ProtocolAdapter.java
│   ├── AuthenticationProvider.java
│   ├── AuthorizationEngine.java
│   ├── CredentialBroker.java
│   ├── BackendConnector.java
│   ├── PipelineStep.java
│   ├── RateLimiter.java
│   ├── AuditSink.java
│   └── ResilienceDecorator.java
├── config/
│   ├── ToranaProperties.java
│   ├── RouteDefinition.java
│   ├── PipelineDefinition.java
│   ├── StepDefinition.java
│   ├── ConnectorDefinition.java
│   ├── ResilienceProfileDefinition.java
│   └── RateLimitPolicyDefinition.java
└── exception/
    ├── ToranaException.java
    ├── AuthenticationException.java
    ├── AuthorizationException.java
    ├── RateLimitExceededException.java
    ├── RouteNotFoundException.java
    ├── PipelineException.java
    ├── ConnectorException.java
    └── ConfigurationException.java
```

## Extension Guide

Any enterprise module that wants to add a custom SPI implementation:

1. Implement the relevant interface from `com.phaselume.torana.core.spi`
2. Annotate with `@Component` (Spring discovers it automatically)
3. Optionally annotate with `@ConditionalOnProperty` to activate via YAML

No changes to `torana-core` are ever needed to add new providers, connectors, or steps.
