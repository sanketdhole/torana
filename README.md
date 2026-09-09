# Torana — Embeddable Enterprise Gateway (EEG)

> **तोरण** *(Sanskrit: "gateway arch")* — A stateless, reactive, enterprise-grade AI agent gateway.

[![Java 17](https://img.shields.io/badge/Java-17-blue)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4.1.1-green)](https://spring.io/projects/spring-boot)
[![License: PolyForm Noncommercial](https://img.shields.io/badge/License-PolyForm%20Noncommercial-orange.svg)](LICENSE)

---

## What Is Torana?

Torana is an **embeddable, framework-grade gateway** for enterprise AI agent infrastructure. It provides a unified, declarative surface for:

- **MCP (Model Context Protocol)** — the emerging standard for AI agent tool/resource access
- **gRPC + WebSocket** — high-performance and persistent agent-to-agent (A2A) communication
- **HTTP/REST** — universal compatibility for existing enterprise APIs
- **RAG Pipelines** — retrieval-augmented generation over internal data (SQL, NoSQL, S3, NFS)
- **Zero-trust Security** — JWT/mTLS/API Key auth, OPA fine-grained authorization, Vault credential brokering
- **Enterprise Resilience** — circuit breakers, bulkheads, retries, distributed rate limiting

**All configuration is declarative YAML. No code changes required for new routes, policies, or backends.**

---

## Quick Start (2 Files)

**`application.yaml`**
```yaml
torana:
  protocols:
    mcp:
      enabled: true
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
  rate-limits:
    default:
      requests-per-minute: 60
      key-by: by-user
  routes:
    - id: llm-chat
      match:
        path: /chat/**
      auth:
        required: true
        scopes: [chat:write]
      rate-limit-ref: default
      pipeline-ref: simple-llm
  pipelines:
    simple-llm:
      steps:
        - type: llm-call
          backend-ref: my-llm
          stream: true
  connectors:
    my-llm:
      type: litellm
      base-url: http://litellm:4000
      api-key: ${LITELLM_API_KEY}
      default-model: gpt-4o
```

**`opa-policy.rego`**
```rego
package torana.v1
default allow = false
allow if { "chat:write" in input.principal.scopes }
```

Start: `docker compose up` → Gateway is live at `http://localhost:8080`.

---

## Architecture

```
┌──────────────────────────────────────────────────────┐
│                  TORANA GATEWAY                      │
│                                                      │
│  [Protocol Layer]  MCP/SSE · WebSocket · gRPC · REST │
│         │                                            │
│  [Security Chain] RateLimit→AuthN→AuthZ(OPA)→Route   │
│         │                                            │
│  [Pipeline]  Transform→RAG→LLM→ToolCall→Audit        │
│         │                                            │
│  [Resilience]  CircuitBreaker→Bulkhead→Retry         │
│         │                                            │
│  [Connectors]  LiteLLM · HTTP · SQL · NoSQL · S3 · NFS│
│         │                                            │
│  [Streaming]   SSE · WebSocket · ndjson · gRPC       │
└──────────────────────────────────────────────────────┘

External: Redis · Vault · OPA · LiteLLM · Config Server
```

---

## Module Structure

This is a **Gradle multi-module project**. Each module is independently publishable and has its own `MODULE.md` with full implementation details.

### Module Dependency Graph

```
                    ┌─────────────┐
                    │ torana-core │  ← SPI interfaces + domain model
                    └──────┬──────┘      (no implementations)
                           │ (all modules depend on core)
          ┌────────────────┼────────────────┐
          │                │                │
   ┌──────▼──────┐  ┌──────▼──────┐  ┌─────▼────────┐
   │  Protocols  │  │  Security   │  │  Data Layer  │
   └─────────────┘  └─────────────┘  └──────────────┘
          │                │                │
          └────────────────▼────────────────┘
                    ┌──────────────┐
                    │torana-auto   │  ← wires everything together
                    │  configure   │
                    └──────┬───────┘
                           │
                    ┌──────▼──────┐
                    │torana-starter│ ← single dep for adopters
                    └─────────────┘
```

### Complete Module List

| Module | Layer | Phase | `MODULE.md` |
|--------|-------|-------|-------------|
| [`torana-bom`](torana-bom/MODULE.md) | Platform | 0 | Manages all dependency versions |
| [`torana-core`](torana-core/MODULE.md) | Kernel | 0 | SPI interfaces, domain model, config schema |
| [`torana-autoconfigure`](torana-autoconfigure/MODULE.md) | Wiring | 0 | Spring Boot auto-configuration |
| [`torana-protocol-mcp`](torana-protocol-mcp/MODULE.md) | Protocol | 1 | MCP JSON-RPC 2.0 over HTTP/SSE |
| [`torana-protocol-rest`](torana-protocol-rest/MODULE.md) | Protocol | 1 | HTTP/REST reverse proxy |
| [`torana-protocol-websocket`](torana-protocol-websocket/MODULE.md) | Protocol | 2 | WebSocket MCP adapter |
| [`torana-protocol-grpc`](torana-protocol-grpc/MODULE.md) | Protocol | 2 | gRPC adapter + .proto schema |
| [`torana-security-authn`](torana-security-authn/MODULE.md) | Security | 1 | JWT/OIDC, API Key, mTLS, SAML |
| [`torana-security-authz-opa`](torana-security-authz-opa/MODULE.md) | Security | 1 | OPA authorization engine |
| [`torana-security-vault`](torana-security-vault/MODULE.md) | Security | 3 | HashiCorp Vault credential broker |
| [`torana-routing`](torana-routing/MODULE.md) | Core | 1 | Declarative route registry + hot-reload |
| [`torana-pipeline`](torana-pipeline/MODULE.md) | Core | 1 | Reactive pipeline executor + steps |
| [`torana-resilience`](torana-resilience/MODULE.md) | Resilience | 2 | Resilience4j (CB, bulkhead, retry) |
| [`torana-ratelimit-redis`](torana-ratelimit-redis/MODULE.md) | Resilience | 1 | Distributed sliding-window rate limiter |
| [`torana-streaming`](torana-streaming/MODULE.md) | Streaming | 1 | SSE, WebSocket, ndjson, gRPC writers |
| [`torana-connector-litellm`](torana-connector-litellm/MODULE.md) | Connector | 1 | LiteLLM / OpenAI-compatible LLM |
| [`torana-connector-http`](torana-connector-http/MODULE.md) | Connector | 2 | Generic HTTP microservice proxy |
| [`torana-connector-jdbc`](torana-connector-jdbc/MODULE.md) | Connector | 2 | SQL via R2DBC (reactive) |
| [`torana-connector-nosql`](torana-connector-nosql/MODULE.md) | Connector | 2 | MongoDB + Cassandra |
| [`torana-connector-s3`](torana-connector-s3/MODULE.md) | Connector | 2 | AWS S3 (non-blocking) |
| [`torana-connector-nfs`](torana-connector-nfs/MODULE.md) | Connector | 2 | NFS/block storage (virtual threads) |
| [`torana-observability`](torana-observability/MODULE.md) | Cross-cutting | 1 | Metrics, tracing, audit |
| [`torana-starter`](torana-starter/MODULE.md) | Packaging | 1 | Convenience Spring Boot starter |
| [`torana-test`](torana-test/MODULE.md) | Testing | 0 | Testcontainers fixtures, mock builders |

---

## Phased Development Roadmap

Development is organized in **6 phases** ordered by importance for enterprise adoption. Each phase produces a working, demonstrable artifact.

### Phase 0 — Foundation (Week 1–2)
**Priority: CRITICAL — nothing else builds without this**

Build the skeleton that every other module depends on.

| Module | Task |
|--------|------|
| `torana-bom` | Initial version catalog |
| `torana-core` | All SPI interfaces, domain model (AgentRequest, AgentResponse, AgentContext), ToranaProperties, all exceptions |
| `torana-test` | Core fixtures, mock SPI implementations, JwtTokenFactory |
| `torana-autoconfigure` | ToranaPropertiesAutoConfiguration, AutoConfiguration.imports file |
| Root | `settings.gradle`, `build.gradle`, `gradle/libs.versions.toml` |

**✅ Deliverable**: `./gradlew build` succeeds across all modules. `./gradlew bootRun` starts the application (no endpoints yet).

---

### Phase 1 — Core Gateway (Week 3–6)
**Priority: HIGH — minimum viable product for MCP + JWT + OPA**

| Module | Task |
|--------|------|
| `torana-security-authn` | JWT/OIDC provider (single issuer), API Key provider |
| `torana-security-authz-opa` | OPA client, AccessControlWebFilter, starter Rego policy |
| `torana-routing` | RouteRegistry, RouteMatcher, RoutingWebFilter |
| `torana-pipeline` | PipelineExecutor, LlmCallStep, RequestTransformStep, AuditStep |
| `torana-ratelimit-redis` | RedisRateLimiter (sliding window Lua), RateLimitWebFilter |
| `torana-connector-litellm` | LiteLLMConnector, SseChunkParser, streaming |
| `torana-protocol-mcp` | McpProtocolAdapter, tools/list, tools/call, SSE streaming |
| `torana-protocol-rest` | RestProtocolAdapter, basic reverse proxy |
| `torana-streaming` | SseResponseWriter, SseHeartbeatEmitter, BackpressureOperator |
| `torana-observability` | Core metrics, LogAuditSink, RedisHealthIndicator, OpaHealthIndicator |
| `torana-autoconfigure` | All Phase 1 auto-configurations |
| `torana-starter` | Phase 1 module dependencies |

**✅ Deliverable**: MCP client connects, authenticates with JWT, OPA policy evaluated, rate limited, LLM call proxied via LiteLLM, response streamed as SSE. Full Docker Compose stack works.

---

### Phase 2 — Enterprise Protocols + Connectors + Resilience (Week 7–10)
**Priority: HIGH — enterprise completeness**

| Module | Task |
|--------|------|
| `torana-protocol-websocket` | WebSocket MCP adapter, session management |
| `torana-protocol-grpc` | gRPC server, AgentGateway proto, unary + streaming |
| `torana-resilience` | Resilience4j circuit breaker, bulkhead, retry, time-limiter |
| `torana-pipeline` | RagRetrievalStep, ToolCallStep, CacheStep, HeaderEnrichStep |
| `torana-connector-http` | Generic HTTP reverse proxy, auth injection |
| `torana-connector-jdbc` | R2DBC SQL connector, SqlOperationGuard |
| `torana-connector-nosql` | MongoDB connector |
| `torana-connector-s3` | S3 connector (static credentials first) |
| `torana-connector-nfs` | NFS/block connector with virtual threads |
| `torana-security-authn` | Multi-issuer JWT, mTLS provider |
| `torana-routing` | Hot-reload with Spring Cloud Config |
| `torana-observability` | OTEL tracing, RedisStreamAuditSink, ConnectorMetrics |

**✅ Deliverable**: Full protocol matrix works. All connectors tested with Testcontainers. Resilience4j CB opens/closes correctly. Hot-reload of routes without restart.

---

### Phase 3 — Zero-Trust + Vault (Week 11–13)
**Priority: HIGH for production hardening**

| Module | Task |
|--------|------|
| `torana-security-vault` | Vault client, Kubernetes auth, database + AWS STS + PKI credential types |
| `torana-connector-jdbc` | Vault-managed DB credentials |
| `torana-connector-s3` | Vault AWS STS credentials |
| `torana-connector-http` | Vault Bearer token injection |
| `torana-observability` | KafkaAuditSink, VaultHealthIndicator, CircuitBreakerHealthIndicator |

**✅ Deliverable**: Zero static credentials. All backend calls use Vault-issued short-lived secrets. Vault sidecar in Docker Compose.

---

### Phase 4 — Multi-tenancy + A2A + Tool Registry (Week 14–17)
**Priority: MEDIUM — enterprise scale features**

| Module | Task |
|--------|------|
| All | Multi-tenant namespacing (route, rate-limit, audit, OPA policy) |
| `torana-pipeline` | AgentCallStep (A2A routing), semantic CacheStep |
| `torana-protocol-grpc` | Bidi streaming Exchange RPC |
| `torana-observability` | Per-tenant metrics + audit |

**✅ Deliverable**: Multi-tenant integration test: two tenants with isolated policies. A2A: agent calls another agent route.

---

### Phase 5 — SAML + Kubernetes + CLI (Week 18–22)
**Priority: MEDIUM — production packaging**

| Module | Task |
|--------|------|
| `torana-security-authn` | SAML2 provider |
| `torana-deployment/helm` | Helm chart, values.yaml, ConfigMap for OPA policies |
| Root | `torana-ctl` CLI tool |
| `torana-starter` | Maven Central publication |

**✅ Deliverable**: `helm install torana` works. SAML2 federation with ADFS. CLI for route management.

---

## Technology Stack

| Layer | Technology | Why |
|-------|-----------|-----|
| **Runtime** | Spring Boot 4.1 + WebFlux | Non-blocking I/O, Reactor, enterprise maturity |
| **Reactive** | Project Reactor | Composable async chains, backpressure |
| **Security** | Spring Security 6, Nimbus JOSE | WebFlux-native filter chain, JWT validation |
| **Policy** | Open Policy Agent (OPA) | Decoupled Rego policies, hot-reload, auditable |
| **Secrets** | HashiCorp Vault | Dynamic credentials, PKI, AWS STS |
| **Resilience** | Resilience4j | Reactor-native CB/bulkhead/retry |
| **State** | Redis (Reactive/Lettuce) | Rate limits, JWKS cache, API keys, audit stream |
| **LLM** | LiteLLM proxy | 100+ provider support, single API |
| **SQL** | R2DBC | Reactive relational DB, no thread blocking |
| **NoSQL** | Spring Data Reactive MongoDB | Document store, vector search |
| **Cloud** | AWS SDK v2 (async) | Non-blocking S3 via Netty |
| **gRPC** | gRPC-Java + Protobuf | High-perf A2A, native streaming |
| **Observability** | Micrometer + OpenTelemetry | Vendor-neutral metrics + distributed tracing |
| **Testing** | JUnit 5 + Testcontainers | Real infra integration tests |

---

## Repository Structure

```
torana/
├── README.md                         ← this file
├── IMPLEMENTATION_PLAN.md            ← full implementation blueprint
├── build.gradle                      ← root convention plugin
├── settings.gradle                   ← all 24 sub-modules registered
├── gradle/
│   └── libs.versions.toml            ← central version catalog
│
├── torana-bom/                       ← Bill of Materials
├── torana-core/                      ← SPI + domain model (implement first)
├── torana-autoconfigure/             ← Spring Boot wiring layer
│
├── torana-protocol-mcp/              ← MCP JSON-RPC 2.0 over SSE
├── torana-protocol-websocket/        ← WebSocket MCP adapter
├── torana-protocol-grpc/             ← gRPC adapter
├── torana-protocol-rest/             ← HTTP/REST reverse proxy
│
├── torana-security-authn/            ← JWT, API Key, mTLS, SAML
├── torana-security-authz-opa/        ← OPA authorization
├── torana-security-vault/            ← Vault credential broker
│
├── torana-routing/                   ← Declarative route registry
├── torana-pipeline/                  ← Reactive pipeline executor
│
├── torana-resilience/                ← Circuit breaker, bulkhead, retry
├── torana-ratelimit-redis/           ← Distributed rate limiting
│
├── torana-connector-litellm/         ← LLM via LiteLLM
├── torana-connector-http/            ← Generic HTTP connector
├── torana-connector-jdbc/            ← SQL via R2DBC
├── torana-connector-nosql/           ← MongoDB + Cassandra
├── torana-connector-s3/              ← AWS S3
├── torana-connector-nfs/             ← NFS/block storage
│
├── torana-streaming/                 ← SSE, WS, ndjson, gRPC writers
├── torana-observability/             ← Metrics, tracing, audit
│
├── torana-starter/                   ← Convenience all-in-one starter
├── torana-test/                      ← Test utilities + Testcontainers
│
├── torana-deployment/
│   ├── docker-compose/               ← Full local stack
│   └── helm/torana/                  ← Kubernetes Helm chart
│
└── docs/
    └── adr/                          ← Architecture Decision Records
```

---

## Development Workflow

```bash
# Build all modules
./gradlew build

# Run unit tests across all modules
./gradlew test

# Run integration tests (starts Testcontainers)
./gradlew integrationTest

# Start the gateway locally (uses docker-compose for Redis + OPA)
docker compose -f torana-deployment/docker-compose/docker-compose.yml up -d redis opa
./gradlew :torana-starter:bootRun

# Check gateway health
curl http://localhost:8080/actuator/health
```

---

## Extension Points

| Extension | Interface | How to Register |
|-----------|-----------|----------------|
| Custom auth provider | `AuthenticationProvider` | `@Component` + add type to YAML |
| Custom backend | `BackendConnector` | `@Component` + add type to YAML |
| Custom pipeline step | `PipelineStep` | `@Component` + use type in pipeline YAML |
| Custom audit sink | `AuditSink` | `@Component` + add type to YAML |
| Custom rate limiter | `RateLimiter` | `@Component` (replaces default) |
| Custom OPA policy | `.rego` file | Load into OPA bundle — no Java needed |

---

## License

Torana is licensed under the **PolyForm Noncommercial License 1.0.0** — see [LICENSE](LICENSE).

- **Non-Commercial Use**: Free to use, modify, and distribute for non-commercial, personal, academic, and evaluation purposes.
- **Commercial Use**: Commercial usage (with or without modifications) requires a commercial enterprise license. Contact `licensing@phaselume.com` or visit [phaselume.com](https://phaselume.com).
