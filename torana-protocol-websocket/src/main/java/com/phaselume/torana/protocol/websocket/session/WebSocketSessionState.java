package com.phaselume.torana.protocol.websocket.session;

import com.phaselume.torana.core.model.ToranaAuthentication;
import com.phaselume.torana.protocol.mcp.model.McpClientInfo;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mutable state held per active WebSocket connection.
 */
@Data
@Builder
public class WebSocketSessionState {

    private final String sessionId;
    private final WebSocketSession webSocketSession;
    private McpClientInfo clientInfo;
    private ToranaAuthentication authentication;
    private String tenantId;
    private String userId;

    @Builder.Default
    private Instant connectedAt = Instant.now();

    @Builder.Default
    private volatile Instant lastActivityAt = Instant.now();

    @Builder.Default
    private Set<String> resourceSubscriptions = ConcurrentHashMap.newKeySet();

    @Builder.Default
    private Map<String, Object> attributes = new ConcurrentHashMap<>();

    private volatile boolean initialized;

    public void recordActivity() {
        this.lastActivityAt = Instant.now();
    }

    public void addSubscription(String uri) {
        if (uri != null) {
            resourceSubscriptions.add(uri);
        }
    }

    public void removeSubscription(String uri) {
        if (uri != null) {
            resourceSubscriptions.remove(uri);
        }
    }

    public boolean hasSubscription(String uri) {
        return uri != null && resourceSubscriptions.contains(uri);
    }
}
