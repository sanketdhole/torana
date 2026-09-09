package com.phaselume.torana.protocol.websocket.handler;

import com.phaselume.torana.protocol.websocket.WebSocketProtocolProperties;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates Spring WebFlux HandlerMapping and WebSocketHandlerAdapter for WebSocket endpoints.
 */
public class WebSocketHandlerRegistry {

    private final WebSocketProtocolProperties properties;
    private final McpWebSocketHandler webSocketHandler;

    public WebSocketHandlerRegistry(
            WebSocketProtocolProperties properties,
            McpWebSocketHandler webSocketHandler) {
        this.properties = properties != null ? properties : new WebSocketProtocolProperties();
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * Creates a SimpleUrlHandlerMapping binding the configured path to the McpWebSocketHandler.
     */
    public HandlerMapping createHandlerMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();
        String path = properties.getPath() != null ? properties.getPath() : "/ws/mcp";
        map.put(path, webSocketHandler);

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(-1); // High priority before standard HTTP routers
        return mapping;
    }

    /**
     * Creates a WebSocketHandlerAdapter required by Spring WebFlux.
     */
    public WebSocketHandlerAdapter createHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

    public WebSocketProtocolProperties getProperties() {
        return properties;
    }

    public McpWebSocketHandler getWebSocketHandler() {
        return webSocketHandler;
    }
}
