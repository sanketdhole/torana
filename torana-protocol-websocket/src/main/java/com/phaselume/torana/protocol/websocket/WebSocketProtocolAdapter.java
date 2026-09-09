package com.phaselume.torana.protocol.websocket;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.spi.ProtocolAdapter;
import com.phaselume.torana.protocol.websocket.handler.McpWebSocketHandler;
import com.phaselume.torana.protocol.websocket.handler.WebSocketHandlerRegistry;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * ProtocolAdapter implementation for WebSocket MCP bidirectional transport.
 */
public class WebSocketProtocolAdapter implements ProtocolAdapter {

    public static final String PROTOCOL_NAME = "websocket";

    private final WebSocketProtocolProperties properties;
    private final McpWebSocketHandler webSocketHandler;
    private final WebSocketHandlerRegistry handlerRegistry;

    public WebSocketProtocolAdapter(
            WebSocketProtocolProperties properties,
            McpWebSocketHandler webSocketHandler) {
        this.properties = properties != null ? properties : new WebSocketProtocolProperties();
        this.webSocketHandler = webSocketHandler;
        this.handlerRegistry = new WebSocketHandlerRegistry(this.properties, this.webSocketHandler);
    }

    @Override
    public String protocol() {
        return PROTOCOL_NAME;
    }

    @Override
    public RouterFunction<ServerResponse> routerFunction() {
        String wsPath = properties.getPath() != null ? properties.getPath() : "/ws/mcp";
        return RouterFunctions.route(
                RequestPredicates.GET(wsPath + "/info"),
                request -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue("{\"protocol\":\"websocket\",\"endpoint\":\"" + wsPath + "\"}")
        );
    }

    @Override
    public Mono<AgentRequest> decode(ServerWebExchange exchange) {
        if (exchange == null) {
            return Mono.error(new IllegalArgumentException("ServerWebExchange cannot be null"));
        }

        return DataBufferUtils.join(exchange.getRequest().getBody())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .defaultIfEmpty(new byte[0])
                .map(bodyBytes -> AgentRequest.builder()
                        .id(exchange.getRequest().getId())
                        .protocol(PROTOCOL_NAME)
                        .method(exchange.getRequest().getMethod())
                        .path(exchange.getRequest().getPath().value())
                        .headers(exchange.getRequest().getHeaders())
                        .cachedBody(bodyBytes)
                        .rawExchange(exchange)
                        .build());
    }

    @Override
    public Mono<Void> encode(AgentContext context, Flux<AgentResponse.Chunk> responseStream, ServerWebExchange exchange) {
        if (exchange == null) {
            return Mono.error(new IllegalArgumentException("ServerWebExchange cannot be null"));
        }

        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Flux<DataBuffer> bufferFlux = responseStream.map(chunk -> {
            String delta = chunk.getTextDelta() != null ? chunk.getTextDelta() : "";
            return exchange.getResponse().bufferFactory().wrap(delta.getBytes(StandardCharsets.UTF_8));
        });

        return exchange.getResponse().writeWith(bufferFlux);
    }

    public HandlerMapping getHandlerMapping() {
        return handlerRegistry.createHandlerMapping();
    }

    public WebSocketProtocolProperties getProperties() {
        return properties;
    }

    public McpWebSocketHandler getWebSocketHandler() {
        return webSocketHandler;
    }

    public WebSocketHandlerRegistry getHandlerRegistry() {
        return handlerRegistry;
    }
}
