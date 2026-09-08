# torana-protocol-websocket — WebSocket MCP Protocol Adapter

## Responsibility

Implements the **MCP protocol over WebSocket** transport. Unlike the HTTP/SSE adapter which uses a new HTTP connection per request, WebSocket provides a **persistent, bidirectional channel** — ideal for interactive AI agents that need low-latency, high-frequency tool calls.

This adapter shares the same MCP JSON-RPC 2.0 message model as `torana-protocol-mcp` but uses WebSocket frames for transport. Both binary and text frame modes are supported.

## Key Differences from HTTP/SSE Adapter

| Concern | HTTP/SSE (`torana-protocol-mcp`) | WebSocket (this module) |
|---------|----------------------------------|--------------------------|
| Connection model | Request-response + SSE stream | Persistent bidirectional |
| Session lifecycle | `X-Session-Id` header per request | One session per WS connection |
| Streaming | SSE `data:` events | Binary/text WS frames |
| Multiplexing | Multiple HTTP connections | Multiple requests on one WS connection via `id` field |
| Backpressure | HTTP/2 flow control | WS flow control (frame buffering) |

## Key Classes

### `handler/`

| Class | Description |
|-------|-------------|
| `McpWebSocketHandler` | Implements Spring `WebSocketHandler`. Entry point for all WS connections. Performs handshake validation, creates a per-connection `McpWebSocketSession`, and runs the message processing loop. |
| `McpWebSocketSessionHandler` | Per-connection handler. Maintains a `Flux<WebSocketMessage>` → `Mono<AgentContext>` reactive pipeline. Handles multiplexed JSON-RPC requests (by `id`) on a single connection. |
| `WebSocketHandlerRegistry` | Registers `McpWebSocketHandler` at the configured path via Spring's `WebSocketHandlerMapping`. |

### `codec/`

| Class | Description |
|-------|-------------|
| `WebSocketMcpCodec` | Encodes `McpJsonRpcResponse` → `WebSocketMessage` (text or binary). Decodes `WebSocketMessage` → `McpJsonRpcRequest`. Reuses `McpMessageCodec` from `torana-protocol-mcp`. |
| `WebSocketFrameRouter` | Routes decoded `McpJsonRpcRequest` objects to the correct MCP handler (reuses `McpRequestDispatcher`). |

### `session/`

| Class | Description |
|-------|-------------|
| `WebSocketSessionState` | Per-connection mutable state: session ID, client info, authenticated principal, active subscriptions. Stored locally (no Redis needed — WS is stateful per node). |
| `WebSocketSessionRegistry` | In-memory `ConcurrentHashMap` of active `WebSocketSessionState` per connection ID. Used for server-initiated push (e.g., resource change notifications). |
| `WebSocketIdleTimeoutManager` | Closes idle sessions after configurable timeout. Uses `Flux.timeout` on the inbound message stream. |

## Protocol Flow

```
Client → WS Upgrade (HTTP 101) → McpWebSocketHandler
  ├── Handshake validation (auth header / token query param)
  ├── Create WebSocketSessionState
  └── Start inbound Flux<WebSocketMessage>
        │
        ├── Decode → McpJsonRpcRequest
        ├── Route  → McpRequestDispatcher (same as HTTP adapter)
        ├── Execute → pipeline → BackendConnector
        └── Encode → WebSocketMessage → send to client
```

## Authentication

WebSocket connections authenticate at **handshake time**:
1. `Authorization: Bearer <token>` HTTP header in the upgrade request, or
2. `?token=<jwt>` query parameter (less secure; for browser clients)

The authenticated `ToranaAuthentication` is attached to `WebSocketSessionState` and reused for all subsequent requests on that connection (no re-auth per message).

## YAML Configuration

```yaml
torana:
  protocols:
    websocket:
      enabled: true
      path: /ws/mcp
      max-frame-size: 65536        # bytes
      idle-timeout: 300s
      max-sessions-per-user: 5
      text-mode: true              # true=text frames, false=binary frames
```

## Development Phases

### Phase 2A — Core WebSocket MCP
- [ ] Implement `McpWebSocketHandler` and `WebSocketHandlerRegistry`
- [ ] Implement `WebSocketMcpCodec` (text frame mode)
- [ ] Implement `WebSocketSessionState` + `WebSocketSessionRegistry`
- [ ] Implement `WebSocketFrameRouter` reusing `McpRequestDispatcher`
- [ ] Integration test: WS connect → initialize → tools/call → receive result frames

### Phase 2B — Full Feature
- [ ] Implement `WebSocketIdleTimeoutManager`
- [ ] Add binary frame mode support
- [ ] Add server-push for `resources/subscribe` via `WebSocketSessionRegistry`
- [ ] Add per-user session limit enforcement

### Phase 4 — Multi-tenant
- [ ] Namespace session registry by tenant ID
- [ ] Per-tenant idle timeout config

## Package Layout

```
com.phaselume.torana.protocol.websocket
├── WebSocketProtocolAdapter.java
├── WebSocketProtocolProperties.java
├── handler/
│   ├── McpWebSocketHandler.java
│   ├── McpWebSocketSessionHandler.java
│   └── WebSocketHandlerRegistry.java
├── codec/
│   ├── WebSocketMcpCodec.java
│   └── WebSocketFrameRouter.java
└── session/
    ├── WebSocketSessionState.java
    ├── WebSocketSessionRegistry.java
    └── WebSocketIdleTimeoutManager.java
```
