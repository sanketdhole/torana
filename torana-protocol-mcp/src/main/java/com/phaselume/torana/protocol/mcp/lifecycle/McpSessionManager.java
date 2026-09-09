package com.phaselume.torana.protocol.mcp.lifecycle;

import com.phaselume.torana.protocol.mcp.model.McpClientInfo;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages MCP session state and client capability handshakes.
 */
public class McpSessionManager {

    private final Map<String, SessionEntry> sessions = new ConcurrentHashMap<>();
    private final Duration sessionTtl;

    public McpSessionManager() {
        this(Duration.ofMinutes(30));
    }

    public McpSessionManager(Duration sessionTtl) {
        this.sessionTtl = sessionTtl != null ? sessionTtl : Duration.ofMinutes(30);
    }

    public void createSession(String sessionId, McpClientInfo clientInfo) {
        if (sessionId != null) {
            sessions.put(sessionId, new SessionEntry(clientInfo, Instant.now()));
        }
    }

    public Optional<McpClientInfo> getSession(String sessionId) {
        if (sessionId == null) {
            return Optional.empty();
        }
        SessionEntry entry = sessions.get(sessionId);
        if (entry == null) {
            return Optional.empty();
        }
        if (Duration.between(entry.lastAccessed(), Instant.now()).compareTo(sessionTtl) > 0) {
            sessions.remove(sessionId);
            return Optional.empty();
        }
        entry.touch();
        return Optional.ofNullable(entry.clientInfo());
    }

    public void removeSession(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }

    private static class SessionEntry {
        private final McpClientInfo clientInfo;
        private volatile Instant lastAccessed;

        public SessionEntry(McpClientInfo clientInfo, Instant lastAccessed) {
            this.clientInfo = clientInfo;
            this.lastAccessed = lastAccessed;
        }

        public McpClientInfo clientInfo() {
            return clientInfo;
        }

        public Instant lastAccessed() {
            return lastAccessed;
        }

        public void touch() {
            this.lastAccessed = Instant.now();
        }
    }
}
