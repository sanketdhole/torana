package com.phaselume.torana.core.spi;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.AgentResponse;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * SPI for inbound protocol adapters (e.g. MCP, REST, WebSocket, gRPC).
 */
public interface ProtocolAdapter {

    /**
     * Unique protocol identifier matching YAML configuration (e.g. "mcp", "http", "websocket", "grpc").
     */
    String protocol();

    /**
     * Reactive router function registering routes handled by this protocol adapter.
     */
    RouterFunction<ServerResponse> routerFunction();

    /**
     * Decode the wire protocol exchange into a normalized domain AgentRequest.
     */
    Mono<AgentRequest> decode(ServerWebExchange exchange);

    /**
     * Encode and write the normalized response stream back to the wire client.
     */
    Mono<Void> encode(AgentContext context, Flux<AgentResponse.Chunk> responseStream, ServerWebExchange exchange);
}
