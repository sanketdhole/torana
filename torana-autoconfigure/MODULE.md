# torana-autoconfigure — Spring Boot Auto-configuration

## Responsibility

`torana-autoconfigure` is the **wiring layer** of the framework. It acts as the bridge between all independent Torana modules and the Spring Boot application context. It provides:

1. **Auto-configuration classes** — discovered via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` and activated based on classpath presence and YAML flags.
2. **Custom `@Conditional` annotations** — `@ConditionalOnToranaProtocol`, `@ConditionalOnToranaAuthProvider`, etc., that read from `ToranaProperties` to decide whether to activate a bean.
3. **`SecurityWebFilterChain` assembly** — builds the ordered filter chain from the declared auth provider list without any Java code change.
4. **`RouterFunction` registration** — registers all active protocol adapter route functions.
5. **Fail-fast validation** — at context startup, verifies that all `*-ref` values in routes/pipelines resolve to known beans.

This module depends on every other Torana module, but **all as `compileOnly`** — meaning they are optional at runtime. A module's beans are only activated when its JAR is present on the classpath.

## Key Classes

### Root Auto-configuration

| Class | Role |
|-------|------|
| `ToranaAutoConfiguration` | Root `@AutoConfiguration` class. Imports all sub-configurations in order. |
| `ToranaPropertiesAutoConfiguration` | Registers `ToranaProperties` as a validated `@ConfigurationProperties` bean. |
| `ToranaStartupValidator` | `ApplicationRunner` that validates all `*-ref` cross-references at startup. Fails fast with a clear `ConfigurationException` if misconfigured. |

### Protocol Auto-configuration

| Class | Role |
|-------|------|
| `ProtocolAdapterRegistryAutoConfiguration` | Collects all `ProtocolAdapter` beans and registers their `RouterFunction`s into the reactive web framework. |
| `McpProtocolAutoConfiguration` | `@ConditionalOnClass(McpProtocolAdapter.class)` + `@ConditionalOnProperty("torana.protocols.mcp.enabled")`. Creates `McpProtocolAdapter` bean. |
| `WebSocketProtocolAutoConfiguration` | Same pattern for WebSocket adapter. Registers `WebSocketHandlerMapping`. |
| `GrpcProtocolAutoConfiguration` | Same pattern. Starts embedded gRPC server on configured port. |
| `RestProtocolAutoConfiguration` | Same pattern for REST reverse proxy adapter. |

### Security Auto-configuration

| Class | Role |
|-------|------|
| `ToranaSecurityAutoConfiguration` | Assembles `SecurityWebFilterChain`. Iterates `torana.security.authn.providers[]` list, resolves each declared `type` to an `AuthenticationProvider` bean, and composes them in order. |
| `AuthenticationProviderRegistryAutoConfiguration` | Collects all `AuthenticationProvider` beans and builds the chain (first-match or all-required based on `mode`). |
| `OpaAuthorizationAutoConfiguration` | `@ConditionalOnClass(OpaAuthorizationEngine.class)` + `@ConditionalOnProperty("torana.security.authz.engine", havingValue="opa")`. Creates `OpaClient`, `OpaAuthorizationEngine`, and `AccessControlWebFilter` beans. |
| `VaultCredentialBrokerAutoConfiguration` | `@ConditionalOnClass(VaultCredentialBroker.class)`. Creates Vault client + `VaultCredentialBroker` bean. |

### Routing & Pipeline Auto-configuration

| Class | Role |
|-------|------|
| `RoutingAutoConfiguration` | Creates `RouteRegistry` from `ToranaProperties.routes`. Registers `RoutingWebFilter` in the filter chain. |
| `PipelineAutoConfiguration` | Collects all `PipelineStep` beans (by `type()`), creates `PipelineStepRegistry`, creates `PipelineExecutor`. |

### Resilience & Rate Limit

| Class | Role |
|-------|------|
| `ResilienceAutoConfiguration` | `@ConditionalOnClass(Resilience4jResilienceDecorator.class)`. Registers resilience profiles from YAML. Creates `ConnectorResilienceRegistry`. |
| `RateLimitAutoConfiguration` | `@ConditionalOnClass(RedisRateLimiter.class)`. Creates `RateLimiterRegistry` and `RateLimitWebFilter`. |

### Connector Auto-configuration

| Class | Role |
|-------|------|
| `ConnectorRegistryAutoConfiguration` | Collects all `BackendConnector` beans by `type()`. Creates `ConnectorRegistry`. |
| `LiteLLMConnectorAutoConfiguration` | Conditional on class presence. Creates `LiteLLMConnector` with `WebClient`. |
| `HttpConnectorAutoConfiguration` | Conditional. Creates `HttpBackendConnector`. |
| `JdbcConnectorAutoConfiguration` | Conditional on `R2dbcAutoConfiguration`. Creates `R2dbcBackendConnector`. |
| `NoSqlConnectorAutoConfiguration` | Conditional. Creates MongoDB or Cassandra connector. |
| `S3ConnectorAutoConfiguration` | Conditional. Creates `S3AsyncClient` + `S3BackendConnector`. |
| `NfsConnectorAutoConfiguration` | Conditional. Creates `NfsBackendConnector` with virtual thread executor. |

### Observability Auto-configuration

| Class | Role |
|-------|------|
| `ObservabilityAutoConfiguration` | Registers Micrometer meters, OTEL tracer, health indicators, and `AuditSink` (default: `LogAuditSink`). |

## Custom `@Conditional` Annotations

```
@ConditionalOnToranaProtocol(value = "mcp")     — checks torana.protocols.mcp.enabled=true
@ConditionalOnToranaAuthProvider(type = "jwt-oidc") — checks provider list contains this type
@ConditionalOnToranaAuthzEngine(value = "opa")  — checks torana.security.authz.engine=opa
@ConditionalOnToranaConnector(type = "litellm") — checks a connector of this type is declared
```

## Filter Chain Order

The `SecurityWebFilterChain` is assembled with this fixed order:

```
1.  CorsFilter                    (if CORS configured)
2.  RequestTracingFilter          (inject trace/span IDs)
3.  RateLimitWebFilter            (shed load early — before auth)
4.  AuthenticationWebFilter       (run auth provider chain)
5.  AccessControlWebFilter        (OPA evaluation)
6.  RoutingWebFilter              (match route, populate AgentContext)
7.  AuditFilter                   (record start of request)
```

## `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
com.phaselume.torana.autoconfigure.ToranaPropertiesAutoConfiguration
com.phaselume.torana.autoconfigure.ToranaAutoConfiguration
```

## Development Phases

### Phase 0 — Skeleton
- [ ] Create `ToranaPropertiesAutoConfiguration`
- [ ] Create `ToranaStartupValidator` (no-op initially, just logs)
- [ ] Write `AutoConfiguration.imports` file
- [ ] Verify `./gradlew bootRun` starts without errors

### Phase 1 — Protocol + Auth wiring
- [ ] Implement `McpProtocolAutoConfiguration` + `RestProtocolAutoConfiguration`
- [ ] Implement `ToranaSecurityAutoConfiguration` with JWT provider chain
- [ ] Implement `OpaAuthorizationAutoConfiguration`
- [ ] Implement `RoutingAutoConfiguration`
- [ ] Implement `PipelineAutoConfiguration`

### Phase 2 — Connectors + Resilience
- [ ] Implement all connector auto-configurations
- [ ] Implement `ResilienceAutoConfiguration`
- [ ] Implement `RateLimitAutoConfiguration`
- [ ] Implement `ToranaStartupValidator` with full cross-reference validation

### Phase 3 — WebSocket + gRPC + Vault
- [ ] Implement `WebSocketProtocolAutoConfiguration`
- [ ] Implement `GrpcProtocolAutoConfiguration`
- [ ] Implement `VaultCredentialBrokerAutoConfiguration`

## Package Layout

```
com.phaselume.torana.autoconfigure
├── ToranaAutoConfiguration.java
├── ToranaPropertiesAutoConfiguration.java
├── ToranaStartupValidator.java
├── condition/
│   ├── ConditionalOnToranaProtocol.java
│   ├── OnToranaProtocolCondition.java
│   ├── ConditionalOnToranaAuthProvider.java
│   └── OnToranaAuthProviderCondition.java
├── protocol/
│   ├── McpProtocolAutoConfiguration.java
│   ├── WebSocketProtocolAutoConfiguration.java
│   ├── GrpcProtocolAutoConfiguration.java
│   └── RestProtocolAutoConfiguration.java
├── security/
│   ├── ToranaSecurityAutoConfiguration.java
│   ├── AuthenticationProviderRegistryAutoConfiguration.java
│   ├── OpaAuthorizationAutoConfiguration.java
│   └── VaultCredentialBrokerAutoConfiguration.java
├── routing/
│   └── RoutingAutoConfiguration.java
├── pipeline/
│   └── PipelineAutoConfiguration.java
├── resilience/
│   └── ResilienceAutoConfiguration.java
├── ratelimit/
│   └── RateLimitAutoConfiguration.java
├── connector/
│   ├── ConnectorRegistryAutoConfiguration.java
│   ├── LiteLLMConnectorAutoConfiguration.java
│   ├── HttpConnectorAutoConfiguration.java
│   ├── JdbcConnectorAutoConfiguration.java
│   ├── NoSqlConnectorAutoConfiguration.java
│   ├── S3ConnectorAutoConfiguration.java
│   └── NfsConnectorAutoConfiguration.java
└── observability/
    └── ObservabilityAutoConfiguration.java
```
