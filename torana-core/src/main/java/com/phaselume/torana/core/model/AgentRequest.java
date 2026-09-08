package com.phaselume.torana.core.model;

import lombok.Builder;
import lombok.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Normalized, protocol-agnostic inbound agent request.
 * Represents wire-level incoming calls across MCP, REST, WebSocket, or gRPC.
 */
@Value
@Builder(toBuilder = true)
public class AgentRequest {

    String id;
    String protocol; // "mcp", "http", "websocket", "grpc"
    HttpMethod method;
    String path;
    HttpHeaders headers;
    MultiValueMap<String, String> queryParams;
    Flux<DataBuffer> body;
    byte[] cachedBody;
    
    @Builder.Default
    Map<String, Object> attributes = Collections.emptyMap();
    
    @Builder.Default
    Instant timestamp = Instant.now();

    ServerWebExchange rawExchange;

    /**
     * Retrieve an attribute cast to the desired type.
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return attributes != null ? (T) attributes.get(key) : null;
    }
}
