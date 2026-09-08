# torana-protocol-mcp — MCP JSON-RPC 2.0 over HTTP/SSE

## Responsibility

Implements the **Model Context Protocol (MCP)** specification over HTTP + Server-Sent Events (SSE). This module is the primary protocol surface for AI agent communication.

MCP is a JSON-RPC 2.0 protocol that defines a structured interface for AI models to discover and invoke tools, read resources, and get prompt templates. This adapter handles the **full MCP session lifecycle** and translates every MCP message into Torana's internal `AgentRequest` model.

## MCP Specification Coverage

### Session Lifecycle
- `initialize` request → respond with `ServerInfo` + `capabilities`
- `initialized` notification → mark session active
- Session ID managed via `X-Session-Id` header (stateless: session state in Redis)

### Tool Operations
- `tools/list` → return paginated list of registered tools
- `tools/call` → invoke tool, stream `$/progress` notifications, return final result

### Resource Operations
- `resources/list` → list available resources (paginated)
- `resources/read` → read resource content (text or binary)
- `resources/subscribe` → subscribe to resource change notifications (SSE)
- `resources/unsubscribe` → unsubscribe

### Prompt Operations
- `prompts/list` → list available prompt templates
- `prompts/get` → get a rendered prompt with argument substitution

### Streaming
- All long-running operations use SSE: `data: {...}\n\n` format
- Heartbeat `data: {"type":"ping"}\n\n` every configurable interval (default 30s)
- Error events: `event: error\ndata: {...}\n\n`

## Key Classes

### `model/`

| Class | Description |
|-------|-------------|
| `McpJsonRpcRequest` | Deserialized MCP JSON-RPC 2.0 request: `jsonrpc`, `id`, `method`, `params` |
| `McpJsonRpcResponse` | JSON-RPC 2.0 response: `id`, `result`, `error` |
| `McpJsonRpcError` | Error object: `code`, `message`, `data` |
| `McpToolDefinition` | Tool descriptor: `name`, `description`, `inputSchema` (JSON Schema) |
| `McpResourceDefinition` | Resource descriptor: `uri`, `name`, `mimeType`, `description` |
| `McpPromptDefinition` | Prompt template: `name`, `description`, `arguments[]` |
| `McpServerInfo` | Server capabilities response for `initialize` |
| `McpClientInfo` | Client capabilities from `initialize` request |
| `McpProgressNotification` | SSE progress event during long-running tool call |

### `codec/`

| Class | Description |
|-------|-------------|
| `McpMessageCodec` | Encodes/decodes MCP JSON-RPC messages using Jackson. Handles `params` polymorphism by method name. |
| `McpToAgentRequestMapper` | Maps `McpJsonRpcRequest` → `AgentRequest`. Extracts tool name, arguments, resource URI into a normalized form. |
| `AgentResponseToMcpMapper` | Maps `Flux<AgentResponse.Chunk>` → `Flux<ServerSentEvent<McpJsonRpcResponse>>`. Builds progress SSE events + final result event. |

### `handler/`

| Class | Description |
|-------|-------------|
| `McpRequestDispatcher` | Routes MCP method names (`tools/call`, `resources/read`, etc.) to the correct handler method. Acts as the internal router for the MCP surface. |
| `McpToolsHandler` | Handles `tools/list` (reads from Tool Registry) and `tools/call` (dispatches to pipeline). |
| `McpResourcesHandler` | Handles `resources/list`, `resources/read`, `resources/subscribe`. |
| `McpPromptsHandler` | Handles `prompts/list`, `prompts/get`. |

### `lifecycle/`

| Class | Description |
|-------|-------------|
| `McpSessionManager` | Manages stateless session metadata. Stores `ClientInfo` + session state in Redis with TTL. |
| `McpInitializeHandler` | Handles `initialize` request: validates client capabilities, stores session, returns `ServerInfo`. |
| `McpHeartbeatEmitter` | Emits SSE heartbeat pings on a fixed schedule to keep connections alive. |

### Root

| Class | Description |
|-------|-------------|
| `McpProtocolAdapter` | Implements `ProtocolAdapter`. Provides `RouterFunction` for `POST /mcp/v1`. Orchestrates `McpMessageCodec` → `McpRequestDispatcher` → SSE response stream. |
| `McpProtocolProperties` | Sub-properties: `path`, `sse-heartbeat-interval`, `max-request-size`, `session-ttl`. |

## HTTP Endpoints Registered

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `{path}/` | Main MCP endpoint; accepts JSON-RPC, responds with SSE |
| `GET` | `{path}/sse` | SSE subscription endpoint (for clients that open persistent SSE connection first) |
| `DELETE` | `{path}/session/{id}` | Explicitly close an MCP session |

## Error Codes (JSON-RPC)

| Code | Meaning |
|------|---------|
| `-32700` | Parse error |
| `-32600` | Invalid request |
| `-32601` | Method not found |
| `-32602` | Invalid params |
| `-32603` | Internal error |
| `-32001` | Tool not found |
| `-32002` | Authorization denied |
| `-32003` | Rate limit exceeded |
| `-32004` | Backend unavailable (circuit open) |

## YAML Configuration

```yaml
torana:
  protocols:
    mcp:
      enabled: true
      path: /mcp/v1
      sse-heartbeat-interval: 30s
      max-request-size: 10MB
      session-ttl: 300s
      capabilities:
        tools: true
        resources: true
        prompts: true
        streaming: true
```

## Development Phases

### Phase 1A — Core MCP over HTTP/SSE (implement first)
- [ ] Implement `McpJsonRpcRequest/Response/Error` model with Jackson annotations
- [ ] Implement `McpMessageCodec` for all method types
- [ ] Implement `McpToAgentRequestMapper` for `tools/call`
- [ ] Implement `AgentResponseToMcpMapper` for streaming SSE
- [ ] Implement `McpProtocolAdapter` with `POST /mcp/v1` route
- [ ] Implement `McpInitializeHandler` (session handshake)
- [ ] Implement `McpToolsHandler` for `tools/list` and `tools/call`
- [ ] Implement `McpHeartbeatEmitter`
- [ ] Integration test: MCP client connects → `initialize` → `tools/call` → SSE stream

### Phase 1B — Full Method Coverage
- [ ] Implement `McpResourcesHandler` (list, read)
- [ ] Implement `McpPromptsHandler` (list, get)
- [ ] Implement `McpSessionManager` with Redis-backed session state
- [ ] Add `resources/subscribe` SSE subscription

### Phase 2 — Multi-tenant & Progress Notifications
- [ ] Add `$/progress` notification streaming during long tool calls
- [ ] Add tenant namespace to session key: `torana:mcp:session:{tenantId}:{sessionId}`
- [ ] Add `resources/unsubscribe`

## Package Layout

```
com.phaselume.torana.protocol.mcp
├── McpProtocolAdapter.java
├── McpProtocolProperties.java
├── model/
│   ├── McpJsonRpcRequest.java
│   ├── McpJsonRpcResponse.java
│   ├── McpJsonRpcError.java
│   ├── McpToolDefinition.java
│   ├── McpResourceDefinition.java
│   ├── McpPromptDefinition.java
│   ├── McpServerInfo.java
│   ├── McpClientInfo.java
│   └── McpProgressNotification.java
├── codec/
│   ├── McpMessageCodec.java
│   ├── McpToAgentRequestMapper.java
│   └── AgentResponseToMcpMapper.java
├── handler/
│   ├── McpRequestDispatcher.java
│   ├── McpToolsHandler.java
│   ├── McpResourcesHandler.java
│   └── McpPromptsHandler.java
└── lifecycle/
    ├── McpSessionManager.java
    ├── McpInitializeHandler.java
    └── McpHeartbeatEmitter.java
```

## Dependencies

- `torana-core` (SPI + AgentRequest model)
- `torana-streaming` (SSE emitter)
- `spring-boot-starter-webflux`
- Redis (via `torana-ratelimit-redis` for session state — optional dep, falls back to in-memory)
