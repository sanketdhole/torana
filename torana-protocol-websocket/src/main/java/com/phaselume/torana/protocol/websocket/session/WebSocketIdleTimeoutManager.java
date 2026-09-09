package com.phaselume.torana.protocol.websocket.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.socket.CloseStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Periodically or reactively cleans up idle WebSocket sessions.
 */
@Slf4j
public class WebSocketIdleTimeoutManager {

    private final WebSocketSessionRegistry registry;
    private final Duration idleTimeout;

    public WebSocketIdleTimeoutManager(WebSocketSessionRegistry registry, Duration idleTimeout) {
        this.registry = registry;
        this.idleTimeout = idleTimeout != null ? idleTimeout : Duration.ofSeconds(300);
    }

    /**
     * Inspects active sessions and closes any that have been inactive longer than idleTimeout.
     * Returns the list of expired session IDs.
     */
    public List<String> cleanIdleSessions() {
        List<String> expiredSessions = new ArrayList<>();
        Instant now = Instant.now();

        for (WebSocketSessionState session : registry.getAllSessions()) {
            Duration inactiveDuration = Duration.between(session.getLastActivityAt(), now);
            if (inactiveDuration.compareTo(idleTimeout) > 0) {
                log.info("Closing idle WebSocket session: {} (inactive for {}s, timeout is {}s)",
                        session.getSessionId(), inactiveDuration.toSeconds(), idleTimeout.toSeconds());
                expiredSessions.add(session.getSessionId());

                if (session.getWebSocketSession() != null && session.getWebSocketSession().isOpen()) {
                    session.getWebSocketSession().close(CloseStatus.GOING_AWAY)
                            .subscribe(null, err -> log.debug("Error closing idle session {}: {}", session.getSessionId(), err.getMessage()));
                }
                registry.unregister(session.getSessionId());
            }
        }

        return expiredSessions;
    }

    public Duration getIdleTimeout() {
        return idleTimeout;
    }
}
