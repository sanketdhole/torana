package com.phaselume.torana.protocol.websocket.session;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory thread-safe registry of active WebSocket sessions.
 */
@Slf4j
public class WebSocketSessionRegistry {

    private final Map<String, WebSocketSessionState> sessions = new ConcurrentHashMap<>();
    private final int maxSessionsPerUser;

    public WebSocketSessionRegistry() {
        this(5);
    }

    public WebSocketSessionRegistry(int maxSessionsPerUser) {
        this.maxSessionsPerUser = maxSessionsPerUser;
    }

    /**
     * Registers a new session. Throws IllegalStateException if per-user session limit is exceeded.
     */
    public void register(WebSocketSessionState state) {
        if (state == null || state.getSessionId() == null) {
            return;
        }

        String userId = state.getUserId();
        if (userId != null && !userId.isBlank() && !"anonymous".equalsIgnoreCase(userId)) {
            long userSessionCount = countSessionsByUser(userId);
            if (userSessionCount >= maxSessionsPerUser) {
                throw new IllegalStateException("Max concurrent sessions (" + maxSessionsPerUser + ") exceeded for user: " + userId);
            }
        }

        sessions.put(state.getSessionId(), state);
        log.debug("Registered WebSocket session: {}, total active: {}", state.getSessionId(), sessions.size());
    }

    /**
     * Unregisters and removes a session by ID.
     */
    public WebSocketSessionState unregister(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        WebSocketSessionState removed = sessions.remove(sessionId);
        if (removed != null) {
            log.debug("Unregistered WebSocket session: {}, remaining active: {}", sessionId, sessions.size());
        }
        return removed;
    }

    /**
     * Retrieves session state by session ID.
     */
    public WebSocketSessionState getSession(String sessionId) {
        return sessionId != null ? sessions.get(sessionId) : null;
    }

    /**
     * Returns all active session states.
     */
    public Collection<WebSocketSessionState> getAllSessions() {
        return Collections.unmodifiableCollection(sessions.values());
    }

    /**
     * Returns all sessions belonging to a specific tenant.
     */
    public List<WebSocketSessionState> getSessionsByTenant(String tenantId) {
        if (tenantId == null) {
            return Collections.emptyList();
        }
        return sessions.values().stream()
                .filter(s -> tenantId.equals(s.getTenantId()))
                .collect(Collectors.toList());
    }

    /**
     * Returns all sessions subscribed to a specific resource URI.
     */
    public List<WebSocketSessionState> getSubscribers(String resourceUri) {
        if (resourceUri == null) {
            return Collections.emptyList();
        }
        return sessions.values().stream()
                .filter(s -> s.hasSubscription(resourceUri))
                .collect(Collectors.toList());
    }

    public long countSessionsByUser(String userId) {
        if (userId == null) {
            return 0;
        }
        return sessions.values().stream()
                .filter(s -> userId.equals(s.getUserId()))
                .count();
    }

    public int size() {
        return sessions.size();
    }

    public void clear() {
        sessions.clear();
    }
}
