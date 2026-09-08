# torana-connector-litellm — LiteLLM / OpenAI-Compatible LLM Connector

## Responsibility

Implements a `BackendConnector` for **LLM invocation** via the **LiteLLM proxy**, which exposes a unified OpenAI-compatible API supporting 100+ LLM providers (OpenAI, Azure OpenAI, Anthropic, Google Gemini, AWS Bedrock, Ollama, vLLM, and more).

This connector is the **primary LLM backend** in Torana. It handles:
- Non-streaming (single response) and streaming (`Flux<Chunk>`) invocation
- SSE chunk parsing from OpenAI-format `data: {...}` events
- Model routing, cost tagging, and fallback (delegated to LiteLLM's own router)
- Token usage reporting back to the rate limit system

## Key Classes

### `client/`

| Class | Description |
|-------|-------------|
| `LiteLLMWebClient` | Configured `WebClient` instance for the LiteLLM base URL. Handles connection pooling, timeout, TLS. One instance per named connector in YAML. |
| `LiteLLMConnector` | Implements `BackendConnector` SPI. `type() = "litellm"`. Builds OpenAI Chat Completions request from `AgentContext`. Calls LiteLLM. Returns `Flux<AgentResponse.Chunk>`. |
| `LiteLLMRequestBuilder` | Converts `AgentContext` → `ChatCompletionRequest` (OpenAI format). Applies model override, temperature, max tokens from step params. Supports `stream: true`. |
| `LiteLLMHealthIndicator` | Pings LiteLLM `/health` endpoint. Registered as Spring Actuator indicator. |

### `model/`

| Class | Description |
|-------|-------------|
| `ChatCompletionRequest` | OpenAI Chat Completions request body: `model`, `messages[]`, `stream`, `temperature`, `max_tokens`, `tools[]`, `tool_choice`. |
| `ChatMessage` | A single message: `role` (system/user/assistant/tool), `content`, `name`, `tool_calls[]`, `tool_call_id`. |
| `ChatCompletionResponse` | Non-streaming response: `id`, `choices[].message`, `usage`. |
| `ChatCompletionChunk` | SSE streaming chunk: `id`, `choices[].delta`, `choices[].finish_reason`, `usage` (on last chunk). |
| `ToolCall` | OpenAI-format tool call: `id`, `type`, `function.name`, `function.arguments`. |
| `LiteLLMConnectorConfig` | YAML-bound connector config: `base-url`, `api-key`, `default-model`, `timeout`, `resilience-profile`, cost tag metadata. |

### `streaming/`

| Class | Description |
|-------|-------------|
| `SseChunkParser` | Parses the `Flux<DataBuffer>` SSE response from LiteLLM. Emits one `ChatCompletionChunk` per `data: {...}` line. Filters out `data: [DONE]` sentinel. Handles partial chunks across buffer boundaries. |
| `ChunkToAgentResponseMapper` | Converts `ChatCompletionChunk` → `AgentResponse.Chunk`. Maps `delta.content` → `delta`, `finish_reason`, `usage.completion_tokens` → `tokenCount`. |
| `TokenUsageReporter` | After the stream completes (on `finish_reason = stop`), publishes total token usage to the `RateLimiter` for token-level accounting (Phase 4). |

## LiteLLM Request/Response Flow

```
AgentContext
  → LiteLLMRequestBuilder → ChatCompletionRequest (JSON)
    → LiteLLMWebClient POST /v1/chat/completions
      ← Flux<DataBuffer> (SSE)
        → SseChunkParser → Flux<ChatCompletionChunk>
          → ChunkToAgentResponseMapper → Flux<AgentResponse.Chunk>
```

## YAML Configuration

```yaml
torana:
  connectors:
    litellm-prod:
      type: litellm
      base-url: http://litellm:4000
      api-key: ${LITELLM_API_KEY}
      default-model: gpt-4o
      timeout: 120s
      resilience-profile: llm-profile
      metadata:
        cost-center: ai-platform
        environment: production

    litellm-dev:
      type: litellm
      base-url: http://localhost:4000
      api-key: sk-dev-key
      default-model: ollama/llama3.2
      timeout: 60s
      resilience-profile: default
```

## Supported LiteLLM-Routed Providers (via model string)

| Provider | Model string example |
|----------|---------------------|
| OpenAI | `gpt-4o`, `gpt-4o-mini` |
| Azure OpenAI | `azure/my-deployment-name` |
| Anthropic | `claude-3-5-sonnet-20241022` |
| Google Gemini | `gemini/gemini-1.5-pro` |
| AWS Bedrock | `bedrock/anthropic.claude-3-sonnet` |
| Ollama (local) | `ollama/llama3.2` |
| vLLM | `hosted_vllm/Meta-Llama-3.1-8B` |

## Development Phases

### Phase 1C — Core LLM Connector
- [ ] Implement `ChatCompletionRequest` + `ChatMessage` + `ChatCompletionChunk` models
- [ ] Implement `LiteLLMRequestBuilder`
- [ ] Implement `SseChunkParser` (streaming SSE parsing)
- [ ] Implement `ChunkToAgentResponseMapper`
- [ ] Implement `LiteLLMConnector` (streaming + non-streaming)
- [ ] Implement `LiteLLMWebClient` with connection pool config
- [ ] Integration test (WireMock LiteLLM mock): streaming chat → parsed chunks → AgentResponse

### Phase 2A — Tool Calls
- [ ] Add `ToolCall` model support (OpenAI function calling)
- [ ] Pass tool definitions from `ToolCallStep` in `ChatCompletionRequest.tools[]`
- [ ] Handle `finish_reason = tool_calls` → dispatch `ToolCallStep`

### Phase 3 — Health + Cost Tracking
- [ ] Implement `LiteLLMHealthIndicator`
- [ ] Implement `TokenUsageReporter`
- [ ] Add cost metadata tagging (LiteLLM response headers)

## Package Layout

```
com.phaselume.torana.connector.litellm
├── LiteLLMConnector.java
├── LiteLLMConnectorConfig.java
├── client/
│   ├── LiteLLMWebClient.java
│   ├── LiteLLMRequestBuilder.java
│   └── LiteLLMHealthIndicator.java
├── model/
│   ├── ChatCompletionRequest.java
│   ├── ChatMessage.java
│   ├── ChatCompletionResponse.java
│   ├── ChatCompletionChunk.java
│   └── ToolCall.java
└── streaming/
    ├── SseChunkParser.java
    ├── ChunkToAgentResponseMapper.java
    └── TokenUsageReporter.java
```
