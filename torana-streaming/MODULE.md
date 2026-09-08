# torana-streaming — Unified Outbound Streaming Layer

## Responsibility

Provides a **protocol-agnostic streaming abstraction** that translates `Flux<AgentResponse.Chunk>` (the internal streaming model) into the correct wire format for each outbound protocol. This is the last layer before bytes hit the network.

Without this module, each protocol adapter would need its own streaming logic. Instead, they all hand off `Flux<AgentResponse.Chunk>` to this layer, which handles the encoding, framing, backpressure, heartbeats, and error propagation.

## Wire Format Per Protocol

| Protocol | Format | Content-Type | Termination |
|----------|--------|--------------|-------------|
| MCP/HTTP | `data: {"delta":"..."}\n\n` | `text/event-stream` | `data: [DONE]\n\n` |
| WebSocket | Text/Binary WS frames | — | Close frame |
| gRPC | Streaming `AgentChunk` protobuf | `application/grpc` | `onCompleted()` |
| REST/ndjson | `{"delta":"..."}\n` | `application/x-ndjson` | Connection close |

## Key Classes

### `sse/`

| Class | Description |
|-------|-------------|
| `SseResponseWriter` | Writes `Flux<AgentResponse.Chunk>` as an HTTP `text/event-stream` response. Each chunk becomes `data: {json}\n\n`. Completion sends `data: [DONE]\n\n`. Errors send `event: error\ndata: {json}\n\n`. |
| `SseHeartbeatEmitter` | Merges a `Flux.interval`-based heartbeat (`data: {"type":"ping"}\n\n`) with the content stream to keep long-lived SSE connections alive through proxies. |
| `SseEventFormatter` | Formats `AgentResponse.Chunk` → SSE event string. Handles: `data`, `id`, `event`, `retry` SSE fields. |

### `websocket/`

| Class | Description |
|-------|-------------|
| `WebSocketChunkWriter` | Writes `Flux<AgentResponse.Chunk>` as WebSocket messages via `WebSocketSession.send()`. Encodes chunks as text frames (JSON) or binary frames (protobuf). |
| `WebSocketErrorHandler` | On pipeline error, sends a structured error frame and closes the WebSocket session with appropriate close code. |

### `ndjson/`

| Class | Description |
|-------|-------------|
| `NdjsonResponseWriter` | Writes `Flux<AgentResponse.Chunk>` as `application/x-ndjson`. Each chunk is a JSON object followed by `\n`. Used for REST clients that support streaming JSON. |

### `grpc/`

| Class | Description |
|-------|-------------|
| `GrpcStreamWriter` | Writes `Flux<AgentResponse.Chunk>` to a gRPC `StreamObserver<AgentChunk>`. Maps Reactor `onNext` → `onNext`, `onComplete` → `onCompleted`, `onError` → `onError`. |

### `backpressure/`

| Class | Description |
|-------|-------------|
| `BackpressureConfig` | YAML-bound config: `buffer-strategy` (BUFFER, DROP, LATEST, ERROR), `buffer-size`, `overflow-strategy`. |
| `BackpressureOperator` | Applies `Flux.onBackpressureBuffer()` / `onBackpressureDrop()` / `onBackpressureLatest()` based on config. Applied between the pipeline output and the protocol writer. |
| `SlowConsumerDetector` | Monitors downstream demand. If no demand signal received within `slow-consumer-timeout`, logs a warning and optionally closes the connection. |

## YAML Configuration

```yaml
torana:
  streaming:
    sse:
      heartbeat-interval: 30s
      heartbeat-enabled: true
    backpressure:
      strategy: BUFFER         # BUFFER | DROP | LATEST | ERROR
      buffer-size: 256
      slow-consumer-timeout: 60s
    ndjson:
      enabled: true
```

## Development Phases

### Phase 1C — SSE Streaming (implement with MCP adapter)
- [ ] Implement `SseEventFormatter`
- [ ] Implement `SseResponseWriter`
- [ ] Implement `SseHeartbeatEmitter`
- [ ] Implement `BackpressureOperator` (BUFFER mode only)
- [ ] Test: stream 100 chunks → all arrive with correct SSE format; heartbeats emitted; DONE sentinel sent

### Phase 2A — WebSocket + ndjson
- [ ] Implement `WebSocketChunkWriter`
- [ ] Implement `WebSocketErrorHandler`
- [ ] Implement `NdjsonResponseWriter`
- [ ] Test: WS client receives all chunks; error frame on pipeline failure

### Phase 2B — gRPC + Backpressure
- [ ] Implement `GrpcStreamWriter`
- [ ] Implement full `BackpressureConfig` (all strategies)
- [ ] Implement `SlowConsumerDetector`

## Package Layout

```
com.phaselume.torana.streaming
├── sse/
│   ├── SseResponseWriter.java
│   ├── SseHeartbeatEmitter.java
│   └── SseEventFormatter.java
├── websocket/
│   ├── WebSocketChunkWriter.java
│   └── WebSocketErrorHandler.java
├── ndjson/
│   └── NdjsonResponseWriter.java
├── grpc/
│   └── GrpcStreamWriter.java
└── backpressure/
    ├── BackpressureConfig.java
    ├── BackpressureOperator.java
    └── SlowConsumerDetector.java
```
