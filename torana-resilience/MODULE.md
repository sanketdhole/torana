# torana-resilience — Resilience4j Decorator Layer

## Responsibility

Wraps every `BackendConnector.execute()` call with **Resilience4j** fault-tolerance patterns applied in a fixed order:

```
BackendConnector.execute()
  └── TimeLimiter      (overall call timeout)
        └── CircuitBreaker (fail-fast when backend is down)
              └── Bulkhead   (concurrency cap per connector)
                    └── Retry  (retry on transient failures)
```

This module is **cross-cutting** — it decorates connectors transparently. Neither the pipeline nor the connectors know about resilience patterns. The decorator is applied by `ConnectorRegistry` when it proxies `BackendConnector.execute()`.

All resilience state (circuit breaker state machine, bulkhead semaphore, retry count) is **node-local** — this is intentional for stateless gateway nodes. Each instance reconstructs its own resilience state on startup.

## Key Classes

### `config/`

| Class | Description |
|-------|-------------|
| `ResilienceProfileDefinition` | YAML-bound profile: CB config, bulkhead config, retry config, time-limiter config. Bound from `torana.resilience.profiles.{name}`. |
| `ResilienceProfileRegistry` | Map of `profileName → ResilienceProfileDefinition`. Built at startup from `ToranaProperties.resilience.profiles`. |
| `Resilience4jConfigFactory` | Converts `ResilienceProfileDefinition` → Resilience4j `CircuitBreakerConfig`, `BulkheadConfig`, `RetryConfig`, `TimeLimiterConfig`. |

### `registry/`

| Class | Description |
|-------|-------------|
| `ConnectorResilienceRegistry` | Holds one `CircuitBreakerRegistry`, `BulkheadRegistry`, `RetryRegistry`, `TimeLimiterRegistry` per connector (by connector ID). Created lazily on first use. |
| `ResilienceInstanceFactory` | Given a connector ID and profile name, creates or retrieves resilience instances from their registries. |

### `decorator/`

| Class | Description |
|-------|-------------|
| `Resilience4jResilienceDecorator` | Implements `ResilienceDecorator` SPI. Composes Reactor operators in order: `TimeLimiter.executeFlux` → `CircuitBreaker.decorateFlux` → `Bulkhead.decorateFlux` → `Retry.executeFlux`. Returns wrapped `Flux<T>`. |
| `ResilienceEventListener` | Subscribes to Resilience4j event streams (CB state changes, retry attempts, bulkhead rejections) and emits Micrometer metrics + log entries. |
| `FallbackResponseFactory` | Creates structured fallback responses when the circuit is open or bulkhead is full: HTTP 503 with `X-Torana-CB-State: OPEN` header. |

## Circuit Breaker States

```
CLOSED (normal) → [failure rate > threshold] → OPEN (fail-fast)
OPEN → [wait duration] → HALF-OPEN (probe) → [probe success] → CLOSED
                                             → [probe failure] → OPEN
```

When OPEN: `BackendConnector.execute()` immediately throws `CallNotPermittedException` → mapped to 503 by `FallbackResponseFactory`.

## YAML Configuration

```yaml
torana:
  resilience:
    profiles:
      default:
        circuit-breaker:
          sliding-window-type: COUNT_BASED    # or: TIME_BASED
          sliding-window-size: 10
          failure-rate-threshold: 50           # % failures to open CB
          slow-call-duration-threshold: 2s     # calls slower than this = "slow"
          slow-call-rate-threshold: 80         # % slow calls to open CB
          wait-duration-in-open-state: 30s
          permitted-calls-in-half-open-state: 3
          record-exceptions:
            - java.net.ConnectException
            - java.net.SocketTimeoutException
            - com.phaselume.torana.core.exception.ConnectorException
        bulkhead:
          max-concurrent-calls: 25
          max-wait-duration: 100ms
        retry:
          max-attempts: 3
          wait-duration: 500ms
          exponential-backoff-multiplier: 2.0
          retry-on-exceptions:
            - java.net.ConnectException
          ignore-exceptions:
            - com.phaselume.torana.core.exception.AuthorizationException
        time-limiter:
          timeout-duration: 10s
          cancel-running-future: true

      llm-profile:
        circuit-breaker:
          failure-rate-threshold: 30
          wait-duration-in-open-state: 60s
          sliding-window-size: 5
        bulkhead:
          max-concurrent-calls: 10
          max-wait-duration: 0ms             # reject immediately if LLM pool full
        retry:
          max-attempts: 2
          wait-duration: 2s
        time-limiter:
          timeout-duration: 120s             # LLMs can take long

  connectors:
    litellm-prod:
      resilience-profile: llm-profile
    postgres-prod:
      resilience-profile: default
```

## Metrics Emitted (via `ResilienceEventListener`)

| Metric | Tags | Description |
|--------|------|-------------|
| `torana.cb.state` | `connector`, `state` | Current circuit breaker state (CLOSED=0, OPEN=1, HALF_OPEN=2) |
| `torana.cb.calls` | `connector`, `outcome` | Count of calls (success, error, slow, not_permitted) |
| `torana.retry.attempts` | `connector` | Count of retry attempts |
| `torana.bulkhead.rejected` | `connector` | Count of bulkhead rejections |
| `torana.connector.duration` | `connector`, `outcome` | Histogram of backend call durations |

## Development Phases

### Phase 2A — Core Resilience
- [ ] Implement `ResilienceProfileDefinition` + `ResilienceProfileRegistry`
- [ ] Implement `Resilience4jConfigFactory`
- [ ] Implement `ConnectorResilienceRegistry`
- [ ] Implement `Resilience4jResilienceDecorator`
- [ ] Implement `FallbackResponseFactory`
- [ ] Test: CB opens after threshold, returns 503 with header; closes after wait duration + probe

### Phase 2B — Observability
- [ ] Implement `ResilienceEventListener` with Micrometer meters
- [ ] Expose CB state in `/actuator/health` → `torana.connectors.{name}.cb`

### Phase 3 — Advanced
- [ ] Add time-based sliding window support
- [ ] Add slow-call detection thresholds
- [ ] Bulkhead rejection → queue with configurable drain timeout

## Package Layout

```
com.phaselume.torana.resilience
├── config/
│   ├── ResilienceProfileDefinition.java
│   ├── ResilienceProfileRegistry.java
│   └── Resilience4jConfigFactory.java
├── registry/
│   ├── ConnectorResilienceRegistry.java
│   └── ResilienceInstanceFactory.java
└── decorator/
    ├── Resilience4jResilienceDecorator.java
    ├── ResilienceEventListener.java
    └── FallbackResponseFactory.java
```
