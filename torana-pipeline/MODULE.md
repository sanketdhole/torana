# torana-pipeline — Reactive Pipeline Executor

## Responsibility

Implements the **reactive step-chain pipeline** that processes every matched request from authentication through to backend invocation. The pipeline is the "brain" of Torana — it defines what happens between receiving a request and calling a backend.

Pipelines are defined **entirely in YAML** as an ordered list of step types. Each step is a `PipelineStep` SPI implementation discovered by Spring at startup. Steps are composed as a `Mono<AgentContext>` flatMap chain — fully non-blocking, backpressure-aware, and cancellable.

## Design Principles

- **Steps are pure functions**: `AgentContext in → AgentContext out`. No hidden state.
- **Composable**: any combination of steps is valid.
- **Fault-tolerant**: `on-error` at the pipeline level routes to a named fallback pipeline.
- **Short-circuit**: a step can signal "done" by setting a `response` on `AgentContext` — remaining steps are skipped.
- **Fan-out**: future support for parallel step execution using `Mono.zip`.

## Key Classes

### `model/`

| Class | Description |
|-------|-------------|
| `PipelineDefinition` | YAML-bound pipeline: name, `timeout`, `on-error` (fallback pipeline name), ordered `List<StepDefinition>`. |
| `StepDefinition` | A single step: `type` (matches `PipelineStep.type()`), `Map<String, Object> params` (step-specific config). |
| `PipelineResult` | Final result of pipeline execution: `AgentContext` with response set, or a `Throwable` if all fallbacks failed. |

### `executor/`

| Class | Description |
|-------|-------------|
| `PipelineExecutor` | Main orchestrator. Given a `PipelineDefinition` and an initial `AgentContext`, chains all `PipelineStep.execute()` calls as `Mono.flatMap` operations. Applies pipeline-level timeout. On error, looks up the fallback pipeline and re-executes. Returns `Flux<AgentResponse.Chunk>`. |
| `FallbackPipelineResolver` | Resolves the `on-error` fallback pipeline name to a `PipelineDefinition`. Detects and breaks circular fallback chains. |
| `PipelineTimeoutOperator` | Applies `Mono.timeout(Duration)` to the entire pipeline chain. On timeout: publishes `PipelineException` with `TIMEOUT` cause. |

### `registry/`

| Class | Description |
|-------|-------------|
| `PipelineStepRegistry` | Map of `type → PipelineStep` bean. Built at startup from all `PipelineStep` beans in the application context. Provides `PipelineStep resolve(String type)` — throws `ConfigurationException` for unknown types. |
| `PipelineRegistry` | Map of `name → PipelineDefinition`. Built from `ToranaProperties.pipelines`. `@RefreshScope`-compatible for hot reload. |

### Built-in Pipeline Steps (`step/`)

#### `transform/`

| Class | Step Type | Description |
|-------|-----------|-------------|
| `RequestTransformStep` | `request-transform` | Transforms `AgentRequest` body using SpEL expression or JSONata. Replaces body in `AgentContext`. |
| `ResponseTransformStep` | `response-transform` | Transforms the response chunks: normalize to OpenAI format, add fields, remove fields. |
| `HeaderEnrichStep` | `header-enrich` | Injects headers into the outgoing backend request: static values, context values (`${principal.name}`), or Vault secrets. |

#### `llm/`

| Class | Step Type | Description |
|-------|-----------|-------------|
| `LlmCallStep` | `llm-call` | Invokes an LLM via the configured `BackendConnector` (typically `torana-connector-litellm`). Sets `stream: true` to return `Flux<AgentResponse.Chunk>`. Applies model override if specified. |

#### `rag/`

| Class | Step Type | Description |
|-------|-----------|-------------|
| `RagRetrievalStep` | `rag-retrieval` | Performs vector similarity search against a configured retriever backend (REST or gRPC vector DB). Appends retrieved document chunks to the request context as `system` or `context` messages. |
| `RagContextBuilder` | (internal) | Formats retrieved chunks into an LLM-readable context block (Markdown or XML tags). |

#### `tool/`

| Class | Step Type | Description |
|-------|-----------|-------------|
| `ToolCallStep` | `tool-call` | Dispatches an MCP tool call to a registered tool backend. Injects the tool result into `AgentContext` for subsequent steps. Handles tool-not-found, tool errors. |
| `ToolResultInjector` | (internal) | Appends tool result as a `tool` role message in the conversation. |

#### `agent/`

| Class | Step Type | Description |
|-------|-----------|-------------|
| `AgentCallStep` | `agent-call` | **Agent-to-Agent (A2A)**. Calls another Torana route (by route ID) as if it were a backend. Passes the current `AgentContext` tenant/auth through. Enables multi-agent orchestration. |

#### `cache/`

| Class | Step Type | Description |
|-------|-----------|-------------|
| `CacheStep` | `cache` | Checks Redis for a cached response. On hit: short-circuits pipeline and returns cached `Flux<AgentResponse.Chunk>`. On miss: continues pipeline, caches the response after it completes. Supports exact-match and semantic similarity cache (Phase 4). |
| `CacheKeyBuilder` | (internal) | Builds cache key from: route ID, request body hash, model name, top-k. |

#### `audit/`

| Class | Step Type | Description |
|-------|-----------|-------------|
| `AuditStep` | `audit` | Emits an `AuditEvent` via the configured `AuditSink`. Captures: principal, route, action, request hash, start/end time, status. Payload is never stored raw — only SHA-256 hash. |

## Step Configuration Examples

```yaml
torana:
  pipelines:
    rag-chat-pipeline:
      timeout: 55s
      on-error: simple-error-pipeline
      steps:
        - type: request-transform
          expression: "{ 'messages': input.messages, 'system': 'You are a helpful assistant.' }"

        - type: header-enrich
          headers:
            X-Tenant-Id: "${context.tenantId}"
            X-Trace-Id: "${context.traceId}"

        - type: cache
          ttl: 60s
          key-include: [route-id, body-hash, model]

        - type: rag-retrieval
          retriever-ref: vector-db-connector
          top-k: 5
          score-threshold: 0.75
          context-format: markdown

        - type: llm-call
          backend-ref: litellm-prod
          stream: true
          model-override: gpt-4o
          temperature: 0.7

        - type: response-transform
          template: openai-compat

        - type: audit
          include-payload-hash: true
          redact-fields: [messages[*].content]

    simple-error-pipeline:
      steps:
        - type: response-transform
          static-response:
            status: 503
            body: '{"error": "Service temporarily unavailable"}'
```

## Development Phases

### Phase 1C — Core Pipeline
- [ ] Implement `PipelineStepRegistry` + `PipelineRegistry`
- [ ] Implement `PipelineExecutor` with sequential flatMap chain
- [ ] Implement `PipelineTimeoutOperator`
- [ ] Implement `FallbackPipelineResolver`
- [ ] Implement `LlmCallStep` (calls `BackendConnector`)
- [ ] Implement `RequestTransformStep` (SpEL only, JSONata Phase 2)
- [ ] Implement `ResponseTransformStep` (OpenAI format normalization)
- [ ] Implement `AuditStep`
- [ ] Test: pipeline executes steps in order, timeout fires, fallback activates

### Phase 2A — RAG + Tool + Cache
- [ ] Implement `RagRetrievalStep` + `RagContextBuilder`
- [ ] Implement `ToolCallStep` + `ToolResultInjector`
- [ ] Implement `CacheStep` (exact-match, Redis-backed)
- [ ] Implement `HeaderEnrichStep`

### Phase 4 — A2A + Advanced Cache
- [ ] Implement `AgentCallStep`
- [ ] Add semantic similarity cache (Redis Vector / pgvector)
- [ ] Add parallel step execution (fan-out via `Mono.zip`)
- [ ] Add conditional step execution (YAML `when:` expression)
- [ ] Add JSONata support to `RequestTransformStep`

## Package Layout

```
com.phaselume.torana.pipeline
├── model/
│   ├── PipelineDefinition.java
│   ├── StepDefinition.java
│   └── PipelineResult.java
├── executor/
│   ├── PipelineExecutor.java
│   ├── FallbackPipelineResolver.java
│   └── PipelineTimeoutOperator.java
├── registry/
│   ├── PipelineStepRegistry.java
│   └── PipelineRegistry.java
└── step/
    ├── transform/
    │   ├── RequestTransformStep.java
    │   ├── ResponseTransformStep.java
    │   └── HeaderEnrichStep.java
    ├── llm/
    │   └── LlmCallStep.java
    ├── rag/
    │   ├── RagRetrievalStep.java
    │   └── RagContextBuilder.java
    ├── tool/
    │   ├── ToolCallStep.java
    │   └── ToolResultInjector.java
    ├── agent/
    │   └── AgentCallStep.java
    ├── cache/
    │   ├── CacheStep.java
    │   └── CacheKeyBuilder.java
    └── audit/
        └── AuditStep.java
```
