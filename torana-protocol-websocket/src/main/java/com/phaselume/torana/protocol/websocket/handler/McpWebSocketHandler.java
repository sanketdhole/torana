package com.phaselume.torana.protocol.websocket.handler;

import com.phaselume.torana.core.model.ToranaAuthentication;
import com.phaselume.torana.protocol.websocket.session.WebSocketSessionRegistry;
import com.phaselume.torana.protocol.websocket.session.WebSocketSessionState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Spring WebFlux WebSocketHandler that establishes sessions, validates handshake authentication,
 * and passes the session stream to McpWebSocketSessionHandler.
 */
@Slf4j
public class McpWebSocketHandler implements WebSocketHandler {

    private final WebSocketSessionRegistry sessionRegistry;
    private final McpWebSocketSessionHandler sessionHandler;

    public McpWebSocketHandler(
            WebSocketSessionRegistry sessionRegistry,
            McpWebSocketSessionHandler sessionHandler) {
        this.sessionRegistry = sessionRegistry != null ? sessionRegistry : new WebSocketSessionRegistry();
        this.sessionHandler = sessionHandler;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        HttpHeaders headers = session.getHandshakeInfo().getHeaders();
        String sessionId = headers.getFirst("X-Session-Id");
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "ws-" + UUID.randomUUID();
        }

        String tenantId = headers.getFirst("X-Tenant-Id");
        if (tenantId == null) {
            tenantId = "default";
        }

        String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
        ToranaAuthentication auth = extractAuth(authHeader);

        WebSocketSessionState sessionState = WebSocketSessionState.builder()
                .sessionId(sessionId)
                .webSocketSession(session)
                .tenantId(tenantId)
                .userId(auth.getPrincipalId())
                .authentication(auth)
                .build();

        try {
            sessionRegistry.register(sessionState);
        } catch (IllegalStateException e) {
            log.warn("Rejected WebSocket connection: {}", e.getMessage());
            return session.close();
        }

        final String finalSessionId = sessionId;

        return sessionHandler.handle(session, sessionState)
                .doFinally(signalType -> {
                    log.debug("WebSocket connection terminated with signal: {} for session: {}", signalType, finalSessionId);
                    sessionRegistry.unregister(finalSessionId);
                });
    }

    private ToranaAuthentication extractAuth(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return ToranaAuthentication.builder()
                    .principalId("jwt-user")
                    .authMethod("jwt")
                    .rawToken(token)
                    .build();
        }
        return ToranaAuthentication.anonymous();
    }

    public WebSocketSessionRegistry getSessionRegistry() {
        return sessionRegistry;
    }

    public McpWebSocketSessionHandler getSessionHandler() {
        return sessionHandler;
    }
}
