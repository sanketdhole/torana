# torana-protocol-rest — HTTP/REST Reverse Proxy Adapter

## Responsibility

Implements a **declarative HTTP/REST reverse proxy** as a Torana protocol adapter. This adapter allows enterprises to route plain HTTP API calls (from external clients, internal services, or AI agents using REST) through the Torana security + pipeline + resilience stack without requiring MCP or gRPC clients.

This is the **most broadly compatible** protocol adapter — any HTTP client can use it. It is also the basis for the **RAG pipeline's retrieval HTTP calls** to backend REST APIs.

## Key Capabilities

- Transparent reverse proxy: forwards request to backend, streams response back
- Path rewriting (strip prefix, add prefix, regex replace)
- Header forwarding with allow-list / block-list
- Request/response body transformation (optional — via pipeline steps)
- Streaming passthrough: `application/x-ndjson` and `text/event-stream` pass-through
- Timeout enforcement at the adapter level (independent of backend timeout)

## Key Classes

### `proxy/`

| Class | Description |
|-------|-------------|
| `RestProxyHandler` | Core reverse proxy. Receives `AgentRequest`, builds an outgoing `WebClient` request to the backend, returns `Flux<DataBuffer>` response. |
| `PathRewriteRule` | Encodes a path rewrite: strip-prefix, add-prefix, or regex substitution. Applied before the upstream call. |
| `HeaderForwardingPolicy` | Allow-list / block-list of headers to forward upstream and return downstream. Prevents header leakage. |
| `RestProtocolAdapter` | Implements `ProtocolAdapter`. Registers `RouterFunction` for all paths under `torana.protocols.rest.path`. Decodes HTTP request → `AgentRequest`, encodes response. |

### `filter/`

| Class | Description |
|-------|-------------|
| `ContentTypeStreamingFilter` | Detects `Content-Type: text/event-stream` or `application/x-ndjson` on the upstream response and switches to `Flux<DataBuffer>` streaming passthrough mode. |
| `ResponseNormalizationFilter` | Normalizes response: adds `X-Torana-Route-Id`, `X-Torana-Trace-Id` headers to every downstream response. |

## Path Rewrite Examples

```yaml
torana:
  protocols:
    rest:
      enabled: true
      path: /api/v1
      strip-path-prefix: true      # strips /api/v1 before forwarding
      path-rewrites:
        - from: /api/v1/hr/**
          to: /internal/hr/**
        - from: /api/v1/docs/**
          to: /documents/**
```

## Routes for REST Adapter

```yaml
torana:
  routes:
    - id: hr-passthrough
      match:
        path: /api/v1/hr/**
        methods: [GET, POST, PUT, DELETE]
      protocols: [rest]
      auth:
        required: true
        scopes: [hr:read]
      backend-ref: hr-api     # direct passthrough, no pipeline
      timeout: 10s
```

## Development Phases

### Phase 1C — Core REST Proxy
- [ ] Implement `RestProtocolAdapter` with WebClient-based forwarding
- [ ] Implement `PathRewriteRule` (strip-prefix and add-prefix)
- [ ] Implement `HeaderForwardingPolicy`
- [ ] Implement `ResponseNormalizationFilter`
- [ ] Integration test: REST client → Torana → WireMock backend → response

### Phase 2B — Advanced Features
- [ ] Implement `ContentTypeStreamingFilter` (SSE + ndjson passthrough)
- [ ] Add regex path rewrite support
- [ ] Add request body size limiting
- [ ] Add `X-Forwarded-For` / `X-Real-IP` header injection

### Phase 3 — Polish
- [ ] Per-route header allow-list / block-list configuration
- [ ] Request/response body logging (with redaction) for debug mode

## Package Layout

```
com.phaselume.torana.protocol.rest
├── RestProtocolAdapter.java
├── RestProtocolProperties.java
├── proxy/
│   ├── RestProxyHandler.java
│   ├── PathRewriteRule.java
│   └── HeaderForwardingPolicy.java
└── filter/
    ├── ContentTypeStreamingFilter.java
    └── ResponseNormalizationFilter.java
```
