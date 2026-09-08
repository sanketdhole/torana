# Torana — Embeddable Enterprise Gateway (EEG) Blueprint

**Torana** (तोरण — _Sanskrit for "gateway arch"_) is a stateless, reactive, enterprise-grade AI agent gateway built on **Spring Boot 4 + WebFlux**. It unifies MCP, gRPC, WebSocket, and HTTP/REST protocol surfaces across enterprise cloud infrastructure with zero-trust security, declarative configuration, and out-of-the-box resilience.

---

## 1. Design Principles

| Principle | Manifestation |
|-----------|---------------|
| **Stateless** | All runtime state in Redis / Vault / Config Server; every JVM instance is identical |
| **Declarative-first** | Entire behavior driven by YAML/JSON; no Java code changes for new routes, policies, backends |
| **Modular** | Each concern is a Gradle sub-module / Spring Boot Auto-configuration; pull only what you need |
| **Reactive** | Project Reactor throughout; no blocking threads; backpressure-aware streaming |
| **Zero-trust** | Authenticate + authorize every hop; credential lifecycle managed by Vault |
| **Open-extension** | Every internal subsystem exposes an SPI; custom implementations auto-discovered |

---

## 2. High-Level Module Diagram

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        TORANA GATEWAY (JVM process)                          │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │                    INBOUND PROTOCOL LAYER                              │  │
│  │  ┌──────────┐  ┌───────────┐  ┌──────────┐  ┌─────────────────────┐  │  │
│  │  │  MCP/SSE │  │ WebSocket │  │  gRPC    │  │   HTTP/REST (proxy) │  │  │
│  │  └────┬─────┘  └─────┬─────┘  └─────┬────┘  └──────────┬──────────┘  │  │
│  └───────┼──────────────┼──────────────┼───────────────────┼─────────────┘  │
│          └──────────────┴──────────────┴───────────────────┘                │
│                                    │                                         │
│                         ┌──────────▼──────────┐                             │
│                         │   AgentRequest       │  (normalized internal model) │
│                         └──────────┬──────────┘                             │
│                                    │                                         │
│  ┌─────────────────────────────────▼────────────────────────────────────┐   │
│  │                      SECURITY FILTER CHAIN                            │   │
│  │  [1] CredentialExtractor → [2] AuthN (JWT/mTLS/APIKey/SAML)          │   │
│  │  [3] CredentialBroker (Vault) → [4] OPA AuthZ → [5] RateLimit        │   │
│  └─────────────────────────────────┬────────────────────────────────────┘   │
│                                    │                                         │
│  ┌─────────────────────────────────▼────────────────────────────────────┐   │
│  │                     ROUTING ENGINE (YAML-driven)                      │   │
│  │            RouteRegistry → RouteMatcher → AgentContext                │   │
│  └─────────────────────────────────┬────────────────────────────────────┘   │
│                                    │                                         │
│  ┌─────────────────────────────────▼────────────────────────────────────┐   │
│  │                     PIPELINE EXECUTOR                                 │   │
│  │   Step chain: Transform → RAG → ToolCall → LLM → Transform           │   │
│  │   Each step: Function<AgentContext, Mono<AgentContext>>               │   │
│  └─────────────────────────────────┬────────────────────────────────────┘   │
│                                    │                                         │
│  ┌─────────────────────────────────▼────────────────────────────────────┐   │
│  │                   RESILIENCE LAYER (per-connector)                    │   │
│  │         CircuitBreaker → Bulkhead → Retry → Timeout → Fallback        │   │
│  └─────────────────────────────────┬────────────────────────────────────┘   │
│                                    │                                         │
│  ┌─────────────────────────────────▼────────────────────────────────────┐   │
│  │                      BACKEND CONNECTOR LAYER                          │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────┐  │   │
│  │  │ LiteLLM  │ │Microsvcs │ │ SQL/NoSQL│ │  AWS S3  │ │  NFS/Block│  │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └───────────┘  │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                         │
│  ┌─────────────────────────────────▼────────────────────────────────────┐   │
│  │                    OUTBOUND STREAMING LAYER                           │   │
│  │          SSE  /  WebSocket frames  /  gRPC streams  /  JSON          │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │  CROSS-CUTTING: Metrics (Micrometer) │ Tracing (OTEL) │ Audit (Kafka) │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────┘

External: Redis  │  Vault  │  OPA  │  LiteLLM  │  Config Server  │  Kafka
```

---

## 3. Gradle Multi-Module Layout

```
torana/                                  ← root project
├── torana-bom/                          ← Bill of Materials (version catalog)
├── torana-core/                         ← SPI interfaces, domain model, config schema
├── torana-autoconfigure/                ← Spring Boot Auto-configuration glue
│
├── torana-protocol-mcp/                 ← MCP JSON-RPC 2.0 over HTTP/SSE
├── torana-protocol-websocket/           ← WebSocket protocol adapter
├── torana-protocol-grpc/                ← gRPC protocol adapter (Phase 2)
├── torana-protocol-rest/                ← Plain HTTP/REST reverse proxy adapter
│
├── torana-security-authn/               ← Authentication: JWT, mTLS, API Key, SAML
├── torana-security-authz-opa/           ← OPA-backed authorization engine
├── torana-security-vault/               ← HashiCorp Vault credential broker
│
├── torana-routing/                      ← YAML route registry + matching engine
├── torana-pipeline/                     ← Reactive pipeline executor + built-in steps
│
├── torana-resilience/                   ← Resilience4j wrappers (CB, bulkhead, retry)
├── torana-ratelimit-redis/              ← Distributed sliding-window rate limiter
│
├── torana-connector-litellm/            ← LiteLLM / OpenAI-compat LLM connector
├── torana-connector-http/               ← Generic HTTP microservice connector
├── torana-connector-jdbc/               ← SQL connector (R2DBC reactive)
├── torana-connector-nosql/              ← MongoDB / Cassandra reactive connector
├── torana-connector-s3/                 ← AWS S3 connector (S3AsyncClient)
├── torana-connector-nfs/                ← NFS/block storage connector
│
├── torana-streaming/                    ← Unified SSE / WS / ndjson streaming layer
├── torana-observability/                ← Micrometer + OTEL + audit log publisher
│
├── torana-starter/                      ← Convenience Spring Boot starter (pulls all above)
├── torana-test/                         ← Test utilities, Testcontainers fixtures
│
├── torana-deployment/
│   ├── docker-compose/                  ← Full stack compose file
│   └── helm/                            ← Helm chart (Phase 3)
│
└── docs/                                ← ADRs, config schema reference, API docs
```

> **Dependency Rule**: `torana-core` has zero runtime dependencies beyond Spring `core` and Reactor. Every other module depends on `torana-core`. The `torana-autoconfigure` module depends on everything but is `optional` / `compileOnly` at runtime so consumers pull only what they need.

---

## 4. Module-by-Module Responsibility

### 4.1 `torana-core`

The kernel. Contains only interfaces and value objects — no implementations.

**Key types:**

| Type | Role |
|------|------|
| `AgentRequest` | Normalized, protocol-agnostic inbound request |
| `AgentResponse` | Normalized outbound response (supports streaming via `Flux<Chunk>`) |
| `AgentContext` | Immutable propagation context: identity, claims, route, trace ID, tenant ID, obligations |
| `ProtocolAdapter` SPI | Translates wire-protocol messages ↔ `AgentRequest`/`AgentResponse` |
| `AuthenticationProvider` SPI | Extracts + validates credentials → `ToranaAuthentication` |
| `AuthorizationEngine` SPI | `Mono<AuthzDecision> authorize(AgentContext)` |
| `CredentialBroker` SPI | `Mono<Credentials> broker(AgentContext, BackendRef)` |
| `BackendConnector` SPI | `Flux<Chunk> execute(AgentContext, ConnectorConfig)` |
| `PipelineStep` SPI | `Mono<AgentContext> execute(AgentContext)` |
| `RateLimiter` SPI | `Mono<RateLimitDecision> check(AgentContext)` |
| `AuditSink` SPI | `Mono<Void> publish(AuditEvent)` |
| `ToranaProperties` | `@ConfigurationProperties(prefix="torana")` root — entire YAML schema bound here |

**SPI discovery**: Spring `@ConditionalOnMissingBean` + `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

---

### 4.2 `torana-autoconfigure`

Wires every module together using Spring Boot Auto-configuration. Provides:
- `ToranaAutoConfiguration` — root configuration class
- `@ConditionalOnToranaProtocol(value="mcp")` custom condition
- `ToranaFilterChainAutoConfiguration` — builds `SecurityWebFilterChain` from YAML provider list
- `ToranaRouterFunctionAutoConfiguration` — registers protocol-specific `RouterFunction` beans
- Default bean definitions for every SPI (fail-fast if no implementation found)

---

### 4.3 Protocol Adapters

Each adapter is a `ProtocolAdapter` implementation that:
1. Accepts raw wire-protocol messages
2. Translates to `AgentRequest`
3. Returns the pipeline result as wire-protocol responses (streaming-capable)

| Module | Protocol | Transport | Content Type |
|--------|----------|-----------|--------------|
| `torana-protocol-mcp` | MCP JSON-RPC 2.0 | HTTP POST | `text/event-stream` (SSE) |
| `torana-protocol-websocket` | MCP JSON-RPC 2.0 | WebSocket | Binary / Text frames |
| `torana-protocol-grpc` | gRPC | HTTP/2 | `application/grpc` |
| `torana-protocol-rest` | REST | HTTP | `application/json`, streaming `ndjson` |

**MCP adapter** handles the full MCP lifecycle:
- `initialize` / `initialized` handshake
- `tools/list`, `tools/call`
- `resources/read`, `resources/list`
- `prompts/get`, `prompts/list`
- Streaming `$/progress` notifications over SSE

**Declarative activation:**
```yaml
torana:
  protocols:
    mcp:
      enabled: true
      path: /mcp/v1
      sse-heartbeat-interval: 30s
    websocket:
      enabled: true
      path: /ws/mcp
      max-frame-size: 65536
    grpc:
      enabled: false       # pull torana-protocol-grpc dep to enable
    rest:
      enabled: true
      path: /api
```

---

### 4.4 Security Layer

#### Authentication (`torana-security-authn`)

A chain of `AuthenticationProvider` implementations evaluated in declared order. First provider to return a non-empty result wins (or all are tried in `any-of` mode).

| Provider | Config key | Notes |
|----------|------------|-------|
| `JwtOidcAuthenticationProvider` | `jwt-oidc` | Multi-issuer; JWKS cached in Redis |
| `ApiKeyAuthenticationProvider` | `api-key` | Key metadata in Redis hash |
| `MtlsAuthenticationProvider` | `mtls` | Client cert DN → principal |
| `SamlAuthenticationProvider` | `saml2` | Phase 3; Spring Security SAML2 |

```yaml
torana:
  security:
    authn:
      mode: first-match        # or: all-required (for step-up auth)
      providers:
        - type: jwt-oidc
          issuers:
            - uri: https://keycloak.corp.com/realms/internal
              audience: torana-gateway
            - uri: https://login.microsoftonline.com/{tenant}/v2.0
              audience: api://torana
        - type: api-key
          header: X-Api-Key
          redis-prefix: torana:apikey:
        - type: mtls
          trust-store: classpath:enterprise-ca.p12
          trust-store-password: ${MTLS_TRUSTSTORE_PASSWORD}
```

#### Authorization (`torana-security-authz-opa`)

OPA is called as a reactive HTTP sidecar. The full `AgentContext` (identity, route, claims, resource, action) is posted to OPA's data API. The response can include:
- `allow: true/false`
- `obligations[]` — data-masking rules, audit tags, downstream header injections
- `deny_reason` — surfaced to the caller in a normalized error

```yaml
torana:
  security:
    authz:
      engine: opa
      opa:
        base-url: http://opa-sidecar:8181
        policy-path: torana/v1/allow
        timeout: 200ms
        cache:
          enabled: true
          ttl: 5s
          redis-prefix: torana:authz:cache:
```

Bundled starter Rego (`opa-base-policy.rego`):
```rego
package torana.v1

import future.keywords.in

default allow = false

allow if {
  valid_scope
  not route_denied
  not tenant_mismatch
}

valid_scope if {
  required := data.routes[input.route_id].required_scopes
  some scope in required
  scope in input.principal.scopes
}

route_denied if {
  input.route_id in data.deny_list
}

tenant_mismatch if {
  input.tenant_id != input.principal.tenant_id
}
```

#### Credential Broker (`torana-security-vault`)

Fetches short-lived backend credentials from Vault at request time. Never stores credentials in memory beyond the request scope.

```yaml
torana:
  security:
    credential-broker:
      engine: vault
      vault:
        uri: https://vault.corp.com
        auth-method: kubernetes   # or: approle, token
        role: torana-gateway
      mappings:
        - backend: postgres-prod
          vault-path: database/creds/torana-readonly
        - backend: s3-docs
          vault-path: aws/creds/torana-s3-role
```

---

### 4.5 Routing Engine (`torana-routing`)

A hot-reloadable, predicate-based route matcher. Routes are loaded from YAML (or Config Server) and stored in a `ConcurrentHashMap`-backed `RouteRegistry`. On Spring Cloud `RefreshEvent`, routes are atomically swapped.

**Route definition schema:**
```yaml
torana:
  routes:
    - id: agent-chat-route
      match:
        path: /api/v1/chat/**
        methods: [POST]
        headers:
          X-Agent-Type: "openai-compat"
      protocols: [mcp, rest]
      auth:
        required: true
        scopes: [torana:chat:write]
        allow-anonymous: false
      rate-limit-ref: standard-tier
      pipeline-ref: rag-chat-pipeline
      backend-ref: litellm-prod
      timeout: 30s
      metadata:
        description: "Main agent chat endpoint"
        owner: platform-team
```

**Route matching priority**: Exact path > prefix > wildcard; ties broken by declaration order.

---

### 4.6 Pipeline Executor (`torana-pipeline`)

A reactive step-chain executor. Each step implements `PipelineStep` SPI. Steps are composed as `Mono<AgentContext>` flatMap chains. Steps can short-circuit (return early), branch (conditional steps), or fan-out.

**Built-in step types:**

| Step type | YAML key | Description |
|-----------|----------|-------------|
| `RequestTransformStep` | `request-transform` | JSONata / SpEL expression transforms |
| `HeaderEnrichStep` | `header-enrich` | Inject headers from context / Vault secrets |
| `RagRetrievalStep` | `rag-retrieval` | Vector search → context augmentation |
| `LlmCallStep` | `llm-call` | LLM invocation via LiteLLM (streaming) |
| `ToolCallStep` | `tool-call` | MCP tool dispatch + result injection |
| `AgentCallStep` | `agent-call` | A2A — call another Torana route |
| `ResponseTransformStep` | `response-transform` | Normalize / redact response |
| `CacheStep` | `cache` | Semantic / exact-match response cache (Redis) |
| `AuditStep` | `audit` | Emit structured audit event |

**Pipeline YAML:**
```yaml
torana:
  pipelines:
    rag-chat-pipeline:
      timeout: 25s
      on-error: fallback-pipeline      # named fallback
      steps:
        - type: request-transform
          expression: "{ 'messages': input.messages, 'system': systemPrompt }"
        - type: rag-retrieval
          retriever-ref: internal-vector-db
          top-k: 5
          score-threshold: 0.75
        - type: llm-call
          backend-ref: litellm-prod
          stream: true
          model-override: gpt-4o
        - type: response-transform
          template: openai-compat
        - type: audit
          include-payload-hash: true
          redact-fields: [messages[].content]
```

---

### 4.7 Resilience Layer (`torana-resilience`)

Wraps every `BackendConnector.execute()` call with Resilience4j operators applied in order: **Circuit Breaker → Bulkhead → Retry → Time Limiter**.

All configuration is per-connector and declared in YAML:

```yaml
torana:
  resilience:
    profiles:
      default:
        circuit-breaker:
          sliding-window-size: 10
          failure-rate-threshold: 50
          wait-duration-in-open-state: 30s
          permitted-calls-in-half-open-state: 3
        bulkhead:
          max-concurrent-calls: 25
          max-wait-duration: 100ms
        retry:
          max-attempts: 3
          wait-duration: 500ms
          exponential-backoff-multiplier: 2
          retry-on-exceptions: [java.net.ConnectException, java.net.SocketTimeoutException]
        time-limiter:
          timeout-duration: 10s
      llm-profile:
        circuit-breaker:
          failure-rate-threshold: 30
          wait-duration-in-open-state: 60s
        time-limiter:
          timeout-duration: 120s    # LLMs can be slow
        retry:
          max-attempts: 2

  connectors:
    litellm-prod:
      resilience-profile: llm-profile
    postgres-prod:
      resilience-profile: default
```

Resilience state (CB state machine) is **not** stored externally — it is node-local and reconstructed on restart. This is intentional and correct for stateless gateways.

---

### 4.8 Rate Limiting (`torana-ratelimit-redis`)

Distributed sliding-window counter implemented via Lua scripts on Redis. Evaluated before OPA to shed load early.

**Key strategies:**

| Strategy | Redis key pattern |
|----------|-------------------|
| `by-user` | `torana:rl:{route_id}:{user_id}` |
| `by-api-key` | `torana:rl:{route_id}:{api_key_hash}` |
| `by-tenant` | `torana:rl:{route_id}:{tenant_id}` |
| `by-ip` | `torana:rl:{route_id}:{client_ip}` |
| `by-route` (global) | `torana:rl:{route_id}:global` |

```yaml
torana:
  rate-limits:
    standard-tier:
      requests-per-minute: 120
      burst: 20
      key-by: by-user
      on-exceed: reject          # or: queue, shed-oldest
      headers:
        expose: true             # inject X-RateLimit-* headers
    llm-tier:
      requests-per-minute: 10
      tokens-per-minute: 100000  # token-level limiting (Phase 2)
      key-by: by-tenant
      on-exceed: reject
```

---

### 4.9 Backend Connectors

Each connector implements `BackendConnector` SPI and is independently deployable.

#### `torana-connector-litellm`
LiteLLM proxy as a universal LLM adapter. Handles streaming via `Flux<String>` over SSE chunks parsed from `data:` lines.

#### `torana-connector-http`
Generic reactive HTTP reverse proxy using `WebClient`. Supports header forwarding, body transforms, streaming passthrough.

```yaml
torana:
  connectors:
    internal-hr-api:
      type: http
      base-url: https://hr-api.internal.corp.com
      auth:
        type: vault-injected       # Vault credential broker fetches Bearer token
        vault-ref: database/creds/hr-api
      tls:
        enabled: true
        client-cert: vault:secret/torana/mtls-cert
      timeout: 5s
      resilience-profile: default
```

#### `torana-connector-jdbc`
R2DBC reactive SQL. Maps agent tool calls to parameterized SQL statements. Results serialized as JSON.

```yaml
torana:
  connectors:
    postgres-prod:
      type: r2dbc
      url: r2dbc:postgresql://db.internal.corp.com:5432/enterprisedb
      username: ${DB_USER}
      password: ${DB_PASSWORD}         # or vault-ref
      pool:
        max-size: 20
        acquire-timeout: 2s
      allowed-operations: [SELECT]     # whitelist for zero-trust
```

#### `torana-connector-s3`
AWS S3 via non-blocking `S3AsyncClient`. Supports presigned URL generation, streaming object read/write, multipart.

```yaml
torana:
  connectors:
    s3-docs:
      type: s3
      region: us-east-1
      bucket: enterprise-ai-docs
      auth:
        type: vault-aws-sts          # Vault AWS secrets engine → STS AssumeRole
        vault-ref: aws/creds/torana-s3-role
      allowed-operations: [GET_OBJECT, LIST_OBJECTS]
```

#### `torana-connector-nfs`
NFS/CIFS/block storage access via reactive file channel wrappers (Loom virtual threads for blocking I/O). Returns objects as `Flux<DataBuffer>`.

---

### 4.10 Streaming Infrastructure (`torana-streaming`)

Unified outbound streaming layer. Translates `Flux<AgentResponse.Chunk>` to the correct wire format based on the originating protocol adapter:

| Source Protocol | Wire Format | Content-Type |
|-----------------|-------------|--------------|
| MCP (SSE) | `data: {...}\n\n` | `text/event-stream` |
| WebSocket | Binary/Text WS frames | — |
| gRPC | Streaming RPC | `application/grpc` |
| REST | newline-delimited JSON | `application/x-ndjson` |

Backpressure is honored end-to-end. The streaming layer applies configurable buffer strategies (drop, error, latest) when downstream is slow.

---

### 4.11 Observability (`torana-observability`)

| Concern | Technology | Details |
|---------|-----------|---------|
| **Metrics** | Micrometer + Prometheus | Per-route: req/sec, latency p50/p99, error rate; per-connector: CB state, retry count; streaming: tokens/sec |
| **Tracing** | OpenTelemetry Java agent + OTLP | Trace spans across protocol → pipeline → connector; propagate W3C `traceparent` |
| **Audit** | Structured JSON → pluggable `AuditSink` | Principal, route, action, outcome, latency, payload hash (never raw payloads) |
| **Health** | Spring Boot Actuator | `/actuator/health` with CB state, Redis conn, Vault conn, OPA conn indicators |

**Audit sinks (pluggable via SPI):**
- `LogAuditSink` — default; writes to structured logger
- `RedisStreamAuditSink` — pushes to Redis Stream (fast, at-most-once)
- `KafkaAuditSink` — Kafka topic (at-least-once, Phase 2)
- `S3AuditSink` — batched Parquet to S3 (Phase 3)

---

## 5. Technology Choices & Rationale

| Technology | Module | Why |
|------------|--------|-----|
| **Spring Boot 4 + WebFlux** | Core | Non-blocking I/O, first-class Reactor, mature enterprise ecosystem |
| **Project Reactor** | All | Composable, backpressure-aware async chains; `Flux`/`Mono` fit gateway fan-out/streaming perfectly |
| **Spring Security 6** | `torana-security-authn` | WebFlux-native reactive security filter chain, OAuth2 Resource Server built-in |
| **Spring Cloud Config** | All | Centralized, hot-reloadable config; `@RefreshScope` for route/policy updates |
| **Resilience4j** | `torana-resilience` | Reactor-native operators; no thread blocking; fine-grained per-instance config |
| **Redis (Reactive)** | Rate limit, JWKS cache, API key store, audit stream | Distributed shared state; Lettuce async driver; Lua scripts for atomicity |
| **HashiCorp Vault** | `torana-security-vault` | Dynamic secrets, PKI, AWS STS; short-lived credentials; audit trail |
| **OPA (sidecar)** | `torana-security-authz-opa` | Decoupled policy evaluation; Rego is expressive for RBAC + ABAC; hot-reload via bundle API |
| **LiteLLM** | `torana-connector-litellm` | 100+ provider compatibility via single OpenAI-compat API; handles cost routing, fallbacks |
| **R2DBC** | `torana-connector-jdbc` | Reactive SQL; works with Reactor naturally; no thread-blocking JDBC |
| **AWS SDK v2 (async)** | `torana-connector-s3` | Non-blocking S3 via Netty async HTTP client |
| **gRPC + protobuf** | `torana-protocol-grpc` | Standard for high-performance agent-to-agent; native streaming |
| **Micrometer + OTEL** | `torana-observability` | Vendor-neutral metrics + traces; integrates with Prometheus, Grafana, Jaeger, Datadog |
| **Testcontainers** | `torana-test` | Redis, OPA, Kafka, Vault, WireMock — real infra in integration tests |
| **Lombok** | All | Reduce boilerplate on DTOs and value objects |

---

## 6. Declarative Configuration Schema — Minimal "Getting Started" Example

An enterprise adopter only needs this to get a fully operational gateway:

### `application.yaml` (minimal)
```yaml
spring:
  application:
    name: my-enterprise-gateway

torana:
  # 1. Which protocols to expose
  protocols:
    mcp:
      enabled: true
    rest:
      enabled: true

  # 2. How to authenticate callers
  security:
    authn:
      providers:
        - type: jwt-oidc
          issuers:
            - uri: https://your-idp.example.com
              audience: my-gateway
    authz:
      engine: opa
      opa:
        base-url: http://opa:8181

  # 3. Rate limits
  rate-limits:
    default:
      requests-per-minute: 60
      key-by: by-user

  # 4. Routes
  routes:
    - id: llm-chat
      match:
        path: /chat/**
      auth:
        required: true
        scopes: [chat:write]
      rate-limit-ref: default
      pipeline-ref: simple-llm-pipeline

  # 5. Pipeline
  pipelines:
    simple-llm-pipeline:
      steps:
        - type: llm-call
          backend-ref: my-llm
          stream: true

  # 6. Backend
  connectors:
    my-llm:
      type: litellm
      base-url: http://litellm:4000
      api-key: ${LITELLM_API_KEY}
      default-model: gpt-4o
```

### `opa-policy.rego` (minimal)
```rego
package torana.v1
default allow = false
allow if { "chat:write" in input.principal.scopes }
```

That's it. Two files. The gateway handles everything else.

---

## 7. Full Reference Configuration Schema

```yaml
torana:

  # ─── PROTOCOLS ────────────────────────────────────────────────────────────
  protocols:
    mcp:
      enabled: true
      path: /mcp/v1
      sse-heartbeat-interval: 30s
      max-request-size: 10MB
    websocket:
      enabled: false
      path: /ws/mcp
      max-frame-size: 65536
      idle-timeout: 300s
    grpc:
      enabled: false
      port: 9090
      reflection-enabled: false
    rest:
      enabled: true
      path: /api/v1
      strip-path-prefix: true

  # ─── SECURITY ─────────────────────────────────────────────────────────────
  security:
    authn:
      mode: first-match             # first-match | all-required
      providers:
        - type: jwt-oidc
          issuers:
            - uri: https://idp1.example.com
              audience: torana
              jwks-cache-ttl: 300s
        - type: api-key
          header: X-Api-Key
          redis-prefix: torana:apikey:
        - type: mtls
          trust-store: classpath:ca.p12
          trust-store-password: ${MTLS_PW}

    authz:
      engine: opa
      opa:
        base-url: http://opa:8181
        policy-path: torana/v1/allow
        timeout: 200ms
        cache:
          enabled: true
          ttl: 5s

    credential-broker:
      engine: vault
      vault:
        uri: https://vault.example.com
        auth-method: kubernetes
        role: torana-gw
      mappings:
        - backend: postgres-prod
          vault-path: database/creds/torana-readonly

  # ─── RATE LIMITS ──────────────────────────────────────────────────────────
  rate-limits:
    standard:
      requests-per-minute: 120
      burst: 20
      key-by: by-user
      on-exceed: reject
    llm-tier:
      requests-per-minute: 10
      key-by: by-tenant
      on-exceed: reject

  # ─── ROUTES ───────────────────────────────────────────────────────────────
  routes:
    - id: chat-route
      match:
        path: /api/v1/chat/**
        methods: [POST]
      auth:
        required: true
        scopes: [chat:write]
      rate-limit-ref: llm-tier
      pipeline-ref: rag-chat
      timeout: 60s

    - id: docs-route
      match:
        path: /api/v1/docs/**
        methods: [GET]
      auth:
        required: true
        scopes: [docs:read]
      rate-limit-ref: standard
      backend-ref: s3-docs       # direct pass-through (no pipeline)

  # ─── PIPELINES ────────────────────────────────────────────────────────────
  pipelines:
    rag-chat:
      timeout: 55s
      on-error: fallback-response
      steps:
        - type: request-transform
          expression: "{ messages: input.messages }"
        - type: rag-retrieval
          retriever-ref: vector-db
          top-k: 5
        - type: llm-call
          backend-ref: litellm-prod
          stream: true
        - type: audit
          include-payload-hash: true

  # ─── RESILIENCE ───────────────────────────────────────────────────────────
  resilience:
    profiles:
      default:
        circuit-breaker:
          sliding-window-size: 10
          failure-rate-threshold: 50
          wait-duration-in-open-state: 30s
        retry:
          max-attempts: 3
          wait-duration: 500ms
        time-limiter:
          timeout-duration: 10s
      llm:
        circuit-breaker:
          failure-rate-threshold: 30
          wait-duration-in-open-state: 60s
        retry:
          max-attempts: 2
        time-limiter:
          timeout-duration: 120s

  # ─── CONNECTORS ───────────────────────────────────────────────────────────
  connectors:
    litellm-prod:
      type: litellm
      base-url: http://litellm:4000
      api-key: ${LITELLM_API_KEY}
      default-model: gpt-4o
      resilience-profile: llm

    postgres-prod:
      type: r2dbc
      url: r2dbc:postgresql://db:5432/prod
      credentials-from-vault: true
      vault-ref: database/creds/torana-readonly
      allowed-operations: [SELECT]
      resilience-profile: default

    s3-docs:
      type: s3
      region: us-east-1
      bucket: corp-docs
      credentials-from-vault: true
      vault-ref: aws/creds/s3-role
      allowed-operations: [GET_OBJECT, LIST_OBJECTS]

  # ─── OBSERVABILITY ────────────────────────────────────────────────────────
  observability:
    metrics:
      enabled: true
      include-route-tags: true
    tracing:
      enabled: true
      sample-rate: 1.0
      exporter: otlp
      otlp-endpoint: http://otel-collector:4317
    audit:
      enabled: true
      sink: log            # log | redis-stream | kafka
      kafka:
        topic: torana-audit
        bootstrap-servers: kafka:9092
```

---

## 8. Extension Points (Custom Backends / Policies)

### Custom Backend Connector

```java
@Component
@ConditionalOnProperty("torana.connectors.*.type", havingValue = "my-sap-erp")
public class SapErpConnector implements BackendConnector {

    @Override
    public String type() { return "my-sap-erp"; }

    @Override
    public Flux<AgentResponse.Chunk> execute(AgentContext ctx, ConnectorConfig config) {
        // your reactive implementation
    }
}
```

Register via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

### Custom Pipeline Step

```java
@Component
public class ComplianceCheckStep implements PipelineStep {

    @Override
    public String type() { return "compliance-check"; }

    @Override
    public Mono<AgentContext> execute(AgentContext ctx) {
        return complianceService.check(ctx.request())
            .map(result -> ctx.withObligation("compliance-result", result));
    }
}
```

Then use in YAML:
```yaml
pipelines:
  my-pipeline:
    steps:
      - type: compliance-check
      - type: llm-call
        backend-ref: llm
```

### Custom Authentication Provider

```java
@Component
public class SmartCardAuthProvider implements AuthenticationProvider {
    @Override
    public String type() { return "smart-card"; }

    @Override
    public Mono<ToranaAuthentication> authenticate(ServerWebExchange exchange) {
        // extract + validate smart card credential
    }
}
```

---

## 9. Key Interface Definitions

```java
// SPI — every module implements one or more of these

public interface ProtocolAdapter {
    String protocol();                // "mcp", "websocket", "grpc", "rest"
    RouterFunction<ServerResponse> routerFunction();
    Mono<AgentRequest> decode(ServerWebExchange exchange);
    Mono<Void> encode(AgentContext ctx, Flux<AgentResponse.Chunk> chunks, ServerWebExchange exchange);
}

public interface AuthenticationProvider {
    String type();                    // "jwt-oidc", "api-key", "mtls"
    Mono<ToranaAuthentication> authenticate(ServerWebExchange exchange);
}

public interface AuthorizationEngine {
    Mono<AuthzDecision> authorize(AgentContext ctx);
    // AuthzDecision: allow, obligations[], deny_reason
}

public interface CredentialBroker {
    Mono<BackendCredentials> broker(AgentContext ctx, String backendRef);
}

public interface BackendConnector {
    String type();                    // "litellm", "http", "r2dbc", "s3", "nfs"
    boolean supports(ConnectorConfig config);
    Flux<AgentResponse.Chunk> execute(AgentContext ctx, ConnectorConfig config);
}

public interface PipelineStep {
    String type();                    // matches YAML step type key
    Mono<AgentContext> execute(AgentContext ctx);
}

public interface RateLimiter {
    Mono<RateLimitDecision> check(AgentContext ctx, RateLimitPolicy policy);
}

public interface AuditSink {
    String type();
    Mono<Void> publish(AuditEvent event);
}
```

---

## 10. Project Milestones — Phased Roadmap

### Phase 0 — Foundations (Week 1–2)
**Goal**: Build skeleton that compiles, starts, and has the SPI wired.

- [ ] Convert `build.gradle` to multi-module Gradle (create `settings.gradle` with all subprojects)
- [ ] Create `torana-bom` with version catalog (`libs.versions.toml`)
- [ ] Implement `AgentRequest`, `AgentResponse`, `AgentContext` value objects
- [ ] Define all SPI interfaces in `torana-core`
- [ ] Implement `ToranaProperties` full YAML binding with validation (`@Validated`)
- [ ] Create `torana-autoconfigure` skeleton with fail-fast missing-bean detection
- [ ] Add `torana-test` with Testcontainers fixtures (Redis, OPA)
- [ ] Basic health endpoint (`/actuator/health`) returns 200

**Deliverable**: `./gradlew bootRun` starts; `GET /actuator/health` → `{"status":"UP"}`

---

### Phase 1 — Core Security + HTTP/SSE MCP (Week 3–5)
**Goal**: MCP over HTTP/SSE works end-to-end with JWT auth and OPA policy.

- [ ] `torana-protocol-mcp`: HTTP/SSE adapter, MCP JSON-RPC 2.0 decode/encode
- [ ] `torana-security-authn`: JWT/OIDC provider (multi-issuer, JWKS Redis cache)
- [ ] `torana-security-authn`: API key provider (Redis-backed)
- [ ] `torana-security-authz-opa`: Reactive OPA client + bundled starter Rego
- [ ] `torana-routing`: `RouteRegistry`, `RouteMatcher`, `RoutingWebFilter`
- [ ] `torana-pipeline`: `PipelineExecutor` + `LlmCallStep`
- [ ] `torana-connector-litellm`: Streaming LiteLLM connector
- [ ] `torana-ratelimit-redis`: Sliding window + `X-RateLimit-*` headers
- [ ] `torana-streaming`: SSE writer with heartbeats

**Deliverable**: `curl -N http://localhost:8080/mcp/v1` streams an LLM response; 401 without JWT; 403 when OPA denies; 429 when rate limited.

---

### Phase 2 — WebSocket + Resilience + Connectors (Week 6–8)
**Goal**: WebSocket MCP works; all resilience patterns active; SQL + S3 connectors.

- [ ] `torana-protocol-websocket`: WebSocket adapter with MCP framing
- [ ] `torana-protocol-rest`: HTTP reverse proxy adapter
- [ ] `torana-resilience`: Circuit breaker, bulkhead, retry, time-limiter wrappers
- [ ] `torana-connector-jdbc`: R2DBC SQL connector (SELECT whitelist)
- [ ] `torana-connector-s3`: S3AsyncClient connector (GET/LIST whitelist)
- [ ] `torana-pipeline`: `RagRetrievalStep`, `RequestTransformStep`, `ResponseTransformStep`, `ToolCallStep`
- [ ] Hot-reload: `@RefreshScope` on `RouteRegistry`; Spring Cloud Config integration
- [ ] `torana-observability`: Micrometer meters + OTEL spans

**Deliverable**: Full `docker compose up` with Torana + Redis + OPA + LiteLLM + WireMock backends runs all integration tests green.

---

### Phase 3 — Zero-Trust + Vault + mTLS (Week 9–11)
**Goal**: Production-grade security; no static credentials anywhere.

- [ ] `torana-security-vault`: HashiCorp Vault credential broker (Kubernetes auth)
- [ ] `torana-security-authn`: mTLS provider (Spring WebFlux SSL + client cert DN extraction)
- [ ] Dynamic secret injection into connector configs at request-time
- [ ] Vault PKI for gateway's own TLS certificate rotation
- [ ] OPA bundle API integration for policy hot-reload
- [ ] Audit log: `RedisStreamAuditSink` + `KafkaAuditSink`
- [ ] `torana-connector-nfs`: NFS/block storage with virtual-thread I/O
- [ ] `torana-connector-http`: Generic HTTP connector with Vault-injected bearer token

**Deliverable**: Penetration test checklist passes; Vault credential TTL < 60s per request; mTLS works with client cert.

---

### Phase 4 — Multi-Tenancy + gRPC + A2A (Week 12–15)
**Goal**: Multi-tenant isolation; gRPC protocol; agent-to-agent calls.

- [ ] Multi-tenant route namespace: tenant ID extracted from JWT claim / path prefix
- [ ] Per-tenant OPA policy bundles
- [ ] Per-tenant Redis key namespacing for rate limits + cache
- [ ] `torana-protocol-grpc`: gRPC server adapter (protobuf schema for `AgentRequest`)
- [ ] `torana-pipeline`: `AgentCallStep` for A2A (call another Torana route reactively)
- [ ] Tool Registry: MCP tool self-registration API + YAML catalog + Redis-backed discovery
- [ ] Semantic response cache in `CacheStep` (embedding similarity lookup in Redis Vector)
- [ ] Admin API: `GET /admin/routes`, `POST /admin/routes/{id}/disable`

**Deliverable**: Multi-tenant integration test: two tenants, each with isolated policies and rate limits, cannot access each other's resources.

---

### Phase 5 — Kubernetes + SAML + CLI (Week 16–20)
**Goal**: Production-grade deployment packaging; federation; operator tooling.

- [ ] Helm chart (`helm/torana/`): ConfigMap for YAML config, Secret for API keys, RBAC for Vault
- [ ] Kubernetes Readiness/Liveness probes tuned with CB state awareness
- [ ] Horizontal Pod Autoscaler config (Prometheus adapter for custom metrics)
- [ ] `torana-security-authn`: SAML2 provider (Spring Security SAML2 extension)
- [ ] `torana-starter`: single convenience dependency for quick-start adopters
- [ ] `torana-ctl` CLI: `torana-ctl routes list`, `torana-ctl policy push`, `torana-ctl replay audit`
- [ ] Published to Maven Central under `com.phaselume.torana`
- [ ] Full documentation site (Docusaurus or MkDocs)

**Deliverable**: A new enterprise can `helm install torana` and be operational in under 30 minutes with their own `values.yaml`.

---

## 11. Verification Plan

### Automated Test Strategy

| Level | Framework | What's tested |
|-------|-----------|---------------|
| Unit | JUnit 5 + Mockito | Every SPI implementation in isolation |
| Integration | Testcontainers | Redis, OPA, Vault, WireMock, Kafka — real infra |
| Contract | Spring Cloud Contract | Route + pipeline YAML schema validation |
| Performance | Gatling | 1000 req/s per route; p99 < 50ms (non-LLM) |
| Security | OWASP ZAP | OWASP Top 10 scan on gateway endpoints |

```bash
./gradlew test                    # unit tests (all modules)
./gradlew integrationTest         # Testcontainers integration tests
./gradlew performanceTest         # Gatling load tests
```

### Manual Smoke Test (after `docker compose up`)
```bash
# 1. Health
curl http://localhost:8080/actuator/health

# 2. JWT auth working
TOKEN=$(curl -s -X POST https://idp.example.com/token ...)
curl -N -H "Authorization: Bearer $TOKEN" http://localhost:8080/mcp/v1/tools/list

# 3. MCP streaming chat
curl -N -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"chat","arguments":{"messages":[{"role":"user","content":"Hello!"}]}}}' \
     http://localhost:8080/mcp/v1

# 4. Rate limit (run 200 times quickly → expect 429)
for i in {1..200}; do curl -o /dev/null -s -w "%{http_code}\n" -H "Authorization: Bearer $TOKEN" http://localhost:8080/mcp/v1/tools/list; done | sort | uniq -c

# 5. OPA deny (use a token without required scope)
LIMITED_TOKEN=$(curl -s -X POST ... scope=readonly)
curl -H "Authorization: Bearer $LIMITED_TOKEN" http://localhost:8080/api/v1/chat
# expect 403

# 6. Circuit breaker (stop LiteLLM, send requests → expect CB open after threshold)
docker compose stop litellm
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/chat
# expect 503 with X-Torana-CB-State: OPEN
```

---

## 12. Open Questions

> [!IMPORTANT]
> **Multi-tenancy**: Should tenant ID come from (a) JWT claim, (b) path prefix like `/t/{tenantId}/api/...`, or (c) subdomain? This determines how `AgentContext` extracts and isolates tenant scope. Recommend JWT claim as primary, path prefix as secondary.

> [!IMPORTANT]
> **Tool Registry**: Should MCP tool registration be (a) static YAML catalog only, (b) dynamic self-registration via `POST /admin/tools/register` (tools announce themselves), or (c) Consul/Kubernetes service-discovery-driven? Recommend option (c) with YAML fallback.

> [!IMPORTANT]
> **Module packaging**: Confirm preference between (a) single fat-jar with feature flags or (b) separate Gradle sub-modules each published to Maven Central independently. The plan currently uses (b) which is more flexible but more complex to publish. Option (a) is faster to ship v1.

> [!NOTE]
> **NoSQL Connector**: MongoDB vs. Cassandra vs. both? R2DBC covers PostgreSQL/MySQL. For MongoDB, `spring-data-mongodb-reactive` is the natural choice. For Cassandra, `spring-data-cassandra-reactive`. Phase 2 can start with MongoDB only if needed.
