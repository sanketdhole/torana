# torana-protocol-grpc — gRPC Protocol Adapter

## Responsibility

Implements the Torana gateway surface over **gRPC (HTTP/2)**. This adapter is intended for **high-performance, high-throughput agent-to-agent (A2A) communication** where low latency and binary efficiency matter more than human readability.

gRPC provides:
- Native bidirectional streaming (client, server, and bidi streaming RPCs)
- Protobuf binary serialization (smaller payloads than JSON)
- HTTP/2 multiplexing (no head-of-line blocking)
- Built-in deadline propagation

This module compiles `.proto` schema files into Java stubs and registers them with an embedded gRPC server alongside the main Spring WebFlux HTTP server.

## Protobuf Schema (`src/main/proto/torana/`)

### `agent.proto`
```proto
syntax = "proto3";
package torana.v1;

service AgentGateway {
  // Unary: single request → single response
  rpc Call(AgentCallRequest) returns (AgentCallResponse);

  // Server streaming: single request → stream of chunks
  rpc Stream(AgentCallRequest) returns (stream AgentChunk);

  // Bidi streaming: stream of requests → stream of responses (A2A use case)
  rpc Exchange(stream AgentCallRequest) returns (stream AgentChunk);
}

message AgentCallRequest {
  string route_id = 1;
  map<string, string> headers = 2;
  bytes body = 3;
  string trace_id = 4;
  string tenant_id = 5;
}

message AgentCallResponse {
  int32 status_code = 1;
  map<string, string> headers = 2;
  bytes body = 3;
}

message AgentChunk {
  string delta = 1;
  bool is_final = 2;
  string finish_reason = 3;
  int32 token_count = 4;
  map<string, string> metadata = 5;
}
```

## Key Classes

### `server/`

| Class | Description |
|-------|-------------|
| `ToranaGrpcServer` | Embeds a Netty-based gRPC server on a separate port (default 9090). Registers `AgentGatewayGrpcService`. Lifecycle tied to Spring context. |
| `AgentGatewayGrpcService` | Implements the generated `AgentGatewayGrpc.AgentGatewayImplBase`. Translates gRPC calls to `AgentRequest`, invokes the pipeline, streams back `AgentChunk` responses. |
| `GrpcServerHealthIndicator` | Spring Actuator `HealthIndicator` for the embedded gRPC server. |
| `GrpcInterceptorChain` | Registers gRPC server interceptors for: auth token extraction, trace context propagation, deadline enforcement. |

### `codec/`

| Class | Description |
|-------|-------------|
| `GrpcToAgentRequestMapper` | Maps `AgentCallRequest` protobuf → `AgentRequest` domain object. Extracts headers, body, trace ID. |
| `AgentResponseToGrpcMapper` | Maps `Flux<AgentResponse.Chunk>` → `Flux<AgentChunk>` protobuf. |
| `GrpcMetadataExtractor` | Extracts gRPC metadata (JWT from `Authorization` key, trace context from `traceparent`) for the auth filter. |

## Authentication in gRPC

gRPC auth uses **gRPC Metadata** (equivalent to HTTP headers):
- `authorization: Bearer <jwt>` metadata key
- `x-api-key: <key>` metadata key
- mTLS: client certificate in TLS layer (verified at `ToranaGrpcServer` SSL config)

The `GrpcInterceptorChain` extracts credentials and calls the same `AuthenticationProvider` chain as HTTP adapters.

## YAML Configuration

```yaml
torana:
  protocols:
    grpc:
      enabled: true
      port: 9090
      reflection-enabled: true    # enables grpc_cli / grpcurl discovery
      max-inbound-message-size: 10MB
      keep-alive-time: 60s
      keep-alive-timeout: 20s
      tls:
        enabled: true
        cert-chain: classpath:server.crt
        private-key: ${GRPC_PRIVATE_KEY}
```

## Development Phases

### Phase 2A — Core gRPC
- [ ] Write `agent.proto` schema
- [ ] Configure protobuf Gradle plugin for stub generation
- [ ] Implement `ToranaGrpcServer` with Netty embedded server
- [ ] Implement `AgentGatewayGrpcService` (unary + server streaming)
- [ ] Implement `GrpcToAgentRequestMapper` + `AgentResponseToGrpcMapper`
- [ ] Implement `GrpcMetadataExtractor` for JWT auth
- [ ] Integration test: `grpcurl` → Torana → LiteLLM → streaming response

### Phase 2B — Full gRPC
- [ ] Implement bidi streaming `Exchange` RPC for A2A
- [ ] Add TLS/mTLS support
- [ ] Add `GrpcServerHealthIndicator`
- [ ] Add server reflection (`ProtoReflectionService`)
- [ ] gRPC deadline propagation → pipeline timeout

### Phase 4 — Multi-tenant
- [ ] Tenant ID from gRPC metadata → `AgentContext`

## Package Layout

```
com.phaselume.torana.protocol.grpc
├── GrpcProtocolAdapter.java
├── GrpcProtocolProperties.java
├── server/
│   ├── ToranaGrpcServer.java
│   ├── AgentGatewayGrpcService.java
│   ├── GrpcServerHealthIndicator.java
│   └── GrpcInterceptorChain.java
└── codec/
    ├── GrpcToAgentRequestMapper.java
    ├── AgentResponseToGrpcMapper.java
    └── GrpcMetadataExtractor.java
```
