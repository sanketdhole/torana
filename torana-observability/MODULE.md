# torana-observability — Metrics, Tracing & Audit

## Responsibility

Provides the **cross-cutting observability infrastructure** for the entire gateway:

1. **Metrics** — Micrometer meters (counters, timers, gauges) for every gateway subsystem, exported to Prometheus
2. **Distributed Tracing** — OpenTelemetry spans propagated through the entire reactive chain (protocol → pipeline → connector), exported via OTLP
3. **Structured Audit Logging** — Compliance-grade audit events published to pluggable sinks (log file, Redis Stream, Kafka, S3)
4. **Health Indicators** — Spring Actuator health checks for all external dependencies (Redis, OPA, Vault, LiteLLM)

## Key Classes

### `metrics/`

| Class | Description |
|-------|-------------|
| `ToranaMetricsConfig` | Registers all Micrometer meters. Uses `MeterRegistry` to define counters, timers, and gauges for every subsystem. |
| `RouteMetricsRecorder` | Records per-route metrics in `RoutingWebFilter`: request count, active requests gauge, response time histogram (p50, p99), error count by status code. |
| `ConnectorMetricsRecorder` | Records per-connector metrics: call count, latency, error rate. Reads circuit breaker state from Resilience4j. |
| `StreamingMetricsRecorder` | Records streaming metrics: active SSE streams gauge, chunks-per-second counter, stream duration. |
| `AuthMetricsRecorder` | Records auth metrics: auth success/failure per provider, OPA latency, rate limit rejection rate. |

**Micrometer Meters Published:**

| Metric | Type | Tags |
|--------|------|------|
| `torana.requests.total` | Counter | `route`, `method`, `status` |
| `torana.requests.active` | Gauge | `route` |
| `torana.request.duration` | Timer | `route`, `outcome` |
| `torana.auth.attempts` | Counter | `provider`, `outcome` |
| `torana.authz.latency` | Timer | `engine` |
| `torana.ratelimit.rejected` | Counter | `route`, `policy` |
| `torana.connector.calls` | Counter | `connector`, `outcome` |
| `torana.connector.duration` | Timer | `connector` |
| `torana.cb.state` | Gauge | `connector` |
| `torana.streaming.active` | Gauge | `protocol` |
| `torana.streaming.chunks` | Counter | `route` |
| `torana.pipeline.duration` | Timer | `pipeline` |

### `tracing/`

| Class | Description |
|-------|-------------|
| `ToranaTracingConfig` | Configures OpenTelemetry `Tracer` with OTLP exporter. Integrates with Micrometer Tracing bridge. |
| `RequestTracingFilter` | Spring `WebFilter` at the very start of the chain. Creates a root span for each inbound request. Extracts W3C `traceparent` / `tracestate` from inbound headers (for distributed tracing continuity). Injects span into Reactor Context for downstream propagation. |
| `PipelineTracing` | Wraps each pipeline step execution in a child span named `torana.pipeline.step.{type}`. |
| `ConnectorTracing` | Wraps each `BackendConnector.execute()` call in a child span named `torana.connector.{connectorId}`. Adds `http.url`, `http.method`, `db.system` span attributes. |
| `TraceContextPropagator` | Injects `traceparent` header into all outgoing upstream HTTP calls (propagates trace context to backend services). |

### `audit/`

#### `model/`

| Class | Description |
|-------|-------------|
| `AuditEvent` | Audit record (from `torana-core`). Enriched here with `gatewayVersion`, `instanceId`, `datacenter`. |
| `AuditEventBuilder` | Fluent builder for `AuditEvent`. Accumulates fields across the request lifecycle (start, auth, route match, pipeline, response). Assembled in `AuditStep`. |

#### `sink/`

| Class | Description |
|-------|-------------|
| `LogAuditSink` | Default sink. Writes structured JSON audit events to SLF4J logger `torana.audit` at `INFO` level. |
| `RedisStreamAuditSink` | Publishes `AuditEvent` JSON to a Redis Stream (`XADD torana:audit:stream * ...`). At-most-once delivery. |
| `KafkaAuditSink` | Publishes `AuditEvent` to a Kafka topic using `reactor-kafka`. At-least-once delivery with configurable acks. Schema: JSON or Avro (configurable). |
| `S3AuditSink` | Batches audit events (configurable size/time window), serializes to JSON Lines, uploads to S3 as `audit/{date}/{hour}/{uuid}.jsonl`. Phase 3. |
| `CompositeAuditSink` | Fan-out sink: publishes to multiple sinks simultaneously. |
| `AuditSinkRegistry` | Discovers all `AuditSink` beans, builds `CompositeAuditSink`. |

### `health/`

| Class | Description |
|-------|-------------|
| `RedisHealthIndicator` | Spring Actuator indicator: pings Redis with `PING` command. |
| `OpaHealthIndicator` | Pings OPA `/health` endpoint. |
| `VaultHealthIndicator` | Calls Vault `GET /v1/sys/health`. |
| `LiteLLMHealthIndicator` | Pings LiteLLM `/health`. |
| `CircuitBreakerHealthIndicator` | Reports OPEN circuit breakers. `status=DEGRADED` when any CB is OPEN. |

## YAML Configuration

```yaml
torana:
  observability:
    metrics:
      enabled: true
      include-route-tags: true
      include-connector-tags: true
      prometheus:
        enabled: true

    tracing:
      enabled: true
      sample-rate: 1.0            # 1.0 = 100% in dev; use 0.1 in prod
      exporter: otlp
      otlp-endpoint: http://otel-collector:4317
      propagation: W3C            # W3C | B3 | B3_MULTI

    audit:
      enabled: true
      sinks:
        - type: log
        - type: redis-stream
          redis-stream-key: torana:audit:stream
          max-stream-length: 100000
        # - type: kafka
        #   topic: torana-audit
        #   bootstrap-servers: kafka:9092
      redact-fields:
        - messages[*].content
        - authorization

management:
  endpoints:
    web:
      exposure:
        include: health, metrics, info, prometheus
  endpoint:
    health:
      show-details: when-authorized
```

## Development Phases

### Phase 1E — Metrics + Health
- [ ] Implement `ToranaMetricsConfig` with core meters (request count, latency, errors)
- [ ] Implement `RouteMetricsRecorder`
- [ ] Implement `AuthMetricsRecorder`
- [ ] Implement `RedisHealthIndicator`, `OpaHealthIndicator`
- [ ] Expose Prometheus metrics at `/actuator/prometheus`

### Phase 2A — Tracing
- [ ] Implement `ToranaTracingConfig` (OTEL + Micrometer bridge)
- [ ] Implement `RequestTracingFilter` (root span, W3C propagation)
- [ ] Implement `PipelineTracing` (child span per step)
- [ ] Implement `ConnectorTracing` + `TraceContextPropagator`

### Phase 2B — Audit Sinks
- [ ] Implement `LogAuditSink`
- [ ] Implement `RedisStreamAuditSink`
- [ ] Implement `AuditSinkRegistry` + `CompositeAuditSink`
- [ ] Implement `ConnectorMetricsRecorder`, `StreamingMetricsRecorder`

### Phase 3 — Kafka + Advanced
- [ ] Implement `KafkaAuditSink`
- [ ] Implement `LiteLLMHealthIndicator`, `VaultHealthIndicator`, `CircuitBreakerHealthIndicator`
- [ ] Implement `S3AuditSink` (Phase 3)

## Package Layout

```
com.phaselume.torana.observability
├── metrics/
│   ├── ToranaMetricsConfig.java
│   ├── RouteMetricsRecorder.java
│   ├── ConnectorMetricsRecorder.java
│   ├── StreamingMetricsRecorder.java
│   └── AuthMetricsRecorder.java
├── tracing/
│   ├── ToranaTracingConfig.java
│   ├── RequestTracingFilter.java
│   ├── PipelineTracing.java
│   ├── ConnectorTracing.java
│   └── TraceContextPropagator.java
├── audit/
│   ├── model/
│   │   └── AuditEventBuilder.java
│   └── sink/
│       ├── LogAuditSink.java
│       ├── RedisStreamAuditSink.java
│       ├── KafkaAuditSink.java
│       ├── S3AuditSink.java
│       ├── CompositeAuditSink.java
│       └── AuditSinkRegistry.java
└── health/
    ├── RedisHealthIndicator.java
    ├── OpaHealthIndicator.java
    ├── VaultHealthIndicator.java
    ├── LiteLLMHealthIndicator.java
    └── CircuitBreakerHealthIndicator.java
```
