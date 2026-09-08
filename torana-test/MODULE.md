# torana-test — Shared Test Utilities & Testcontainers Fixtures

## Responsibility

Provides **reusable test infrastructure** for all other Torana modules and for enterprise adopters who want to write integration tests against their Torana deployment. Avoids duplicating Testcontainers setup, mock builders, and test data factories across every module.

## Key Classes

### `containers/`

| Class | Description |
|-------|-------------|
| `ToranaRedisContainer` | Configured `RedisContainer` with `testcontainers-redis`. Exposes reactive `ReactiveRedisTemplate` for test use. |
| `ToranaOpaContainer` | OPA container (`openpolicyagent/opa:latest-static`). Auto-loads `opa-base-policy.rego`. Exposes OPA HTTP URL. |
| `ToranaVaultContainer` | HashiCorp Vault container. Pre-configured with KV v2, database secrets engine stub, and AppRole auth for test. |
| `ToranaKafkaContainer` | Kafka (KRaft mode) container. Provides topic auto-creation helpers. |
| `ToranaWireMockServer` | Pre-configured WireMock server for mocking LiteLLM, HTTP connectors, and upstream APIs. Provides SSE response stubs. |
| `ToranaPostgresContainer` | PostgreSQL container with R2DBC URL helper. |
| `ToranaMongoContainer` | MongoDB container. |
| `ToranaLocalStackContainer` | LocalStack S3 container. |

### `fixtures/`

| Class | Description |
|-------|-------------|
| `AgentRequestFixtures` | Factory methods for common `AgentRequest` test instances: `chatRequest()`, `toolCallRequest()`, `restGetRequest()`. |
| `AgentContextFixtures` | Factory methods for `AgentContext` with common auth scenarios: `authenticatedAsUser()`, `authenticatedAsTenant()`, `anonymous()`. |
| `ToranaAuthenticationFixtures` | Factory for `ToranaAuthentication`: `withScopes(String...)`, `withClaims(Map)`, `withTenant(String)`. |
| `RouteDefinitionFixtures` | Sample `RouteDefinition` instances for unit tests. |
| `PipelineDefinitionFixtures` | Sample `PipelineDefinition` instances. |
| `JwtTokenFactory` | Generates signed JWTs for tests using an embedded JWKS. Allows creating: valid JWT, expired JWT, wrong-issuer JWT, missing-scope JWT. |
| `LiteLLMSseStubFactory` | Generates WireMock SSE stubs that simulate streaming LLM responses with configurable chunk count and delay. |

### `support/`

| Class | Description |
|-------|-------------|
| `ReactiveTestSupport` | Utility methods for testing reactive pipelines: `expectChunks(Flux, int)`, `expectError(Flux, Class)`, `collectAndAssert(Flux, Consumer)`. |
| `ToranaIntegrationTestBase` | Base class for integration tests: starts all required containers, provides pre-wired beans (`RouteRegistry`, `PipelineExecutor`, etc.), handles container lifecycle. |
| `MockAuthenticationProvider` | Test `AuthenticationProvider` that returns a fixed `ToranaAuthentication`. Use to bypass real auth in unit tests. |
| `MockBackendConnector` | Test `BackendConnector` that returns a configurable `Flux<AgentResponse.Chunk>`. |
| `MockPipelineStep` | Test `PipelineStep` that records calls and optionally modifies context. |

## Development Phases

### Phase 0 — Core Test Utilities (implement alongside torana-core)
- [ ] Implement `AgentRequestFixtures`, `AgentContextFixtures`, `ToranaAuthenticationFixtures`
- [ ] Implement `MockAuthenticationProvider`, `MockBackendConnector`, `MockPipelineStep`
- [ ] Implement `ReactiveTestSupport`
- [ ] Implement `JwtTokenFactory`

### Phase 1 — Containers
- [ ] Implement `ToranaRedisContainer`
- [ ] Implement `ToranaOpaContainer`
- [ ] Implement `ToranaWireMockServer` with SSE stubs
- [ ] Implement `ToranaIntegrationTestBase`
- [ ] Implement `LiteLLMSseStubFactory`

### Phase 2 — Additional Containers
- [ ] Implement `ToranaVaultContainer`
- [ ] Implement `ToranaKafkaContainer`
- [ ] Implement `ToranaPostgresContainer`
- [ ] Implement `ToranaMongoContainer`
- [ ] Implement `ToranaLocalStackContainer`

## Package Layout

```
com.phaselume.torana.test
├── containers/
│   ├── ToranaRedisContainer.java
│   ├── ToranaOpaContainer.java
│   ├── ToranaVaultContainer.java
│   ├── ToranaKafkaContainer.java
│   ├── ToranaWireMockServer.java
│   ├── ToranaPostgresContainer.java
│   ├── ToranaMongoContainer.java
│   └── ToranaLocalStackContainer.java
├── fixtures/
│   ├── AgentRequestFixtures.java
│   ├── AgentContextFixtures.java
│   ├── ToranaAuthenticationFixtures.java
│   ├── RouteDefinitionFixtures.java
│   ├── PipelineDefinitionFixtures.java
│   ├── JwtTokenFactory.java
│   └── LiteLLMSseStubFactory.java
└── support/
    ├── ReactiveTestSupport.java
    ├── ToranaIntegrationTestBase.java
    ├── MockAuthenticationProvider.java
    ├── MockBackendConnector.java
    └── MockPipelineStep.java
```
