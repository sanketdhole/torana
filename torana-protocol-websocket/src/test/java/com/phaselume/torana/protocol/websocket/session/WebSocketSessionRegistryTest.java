package com.phaselume.torana.protocol.websocket.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSocketSessionRegistryTest {

    private WebSocketSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new WebSocketSessionRegistry(2);
    }

    @Test
    void testRegisterAndUnregister() {
        WebSocketSessionState session = WebSocketSessionState.builder()
                .sessionId("sess-1")
                .userId("alice")
                .tenantId("tenant-1")
                .build();

        registry.register(session);
        assertEquals(1, registry.size());
        assertNotNull(registry.getSession("sess-1"));

        WebSocketSessionState removed = registry.unregister("sess-1");
        assertNotNull(removed);
        assertEquals(0, registry.size());
        assertNull(registry.getSession("sess-1"));
    }

    @Test
    void testMaxSessionsPerUserEnforced() {
        WebSocketSessionState s1 = WebSocketSessionState.builder().sessionId("s1").userId("bob").build();
        WebSocketSessionState s2 = WebSocketSessionState.builder().sessionId("s2").userId("bob").build();
        WebSocketSessionState s3 = WebSocketSessionState.builder().sessionId("s3").userId("bob").build();

        registry.register(s1);
        registry.register(s2);

        assertThrows(IllegalStateException.class, () -> registry.register(s3));
    }

    @Test
    void testTenantAndResourceFiltering() {
        WebSocketSessionState s1 = WebSocketSessionState.builder()
                .sessionId("s1")
                .tenantId("tenant-a")
                .build();
        s1.addSubscription("resource://weather/sf");

        WebSocketSessionState s2 = WebSocketSessionState.builder()
                .sessionId("s2")
                .tenantId("tenant-b")
                .build();
        s2.addSubscription("resource://weather/ny");

        registry.register(s1);
        registry.register(s2);

        List<WebSocketSessionState> tenantASessions = registry.getSessionsByTenant("tenant-a");
        assertEquals(1, tenantASessions.size());
        assertEquals("s1", tenantASessions.get(0).getSessionId());

        List<WebSocketSessionState> sfSubscribers = registry.getSubscribers("resource://weather/sf");
        assertEquals(1, sfSubscribers.size());
        assertEquals("s1", sfSubscribers.get(0).getSessionId());
    }

    @Test
    void testIdleTimeoutCleanup() {
        WebSocketIdleTimeoutManager timeoutManager = new WebSocketIdleTimeoutManager(registry, Duration.ofMillis(10));

        WebSocketSessionState s1 = WebSocketSessionState.builder()
                .sessionId("idle-sess")
                .build();
        registry.register(s1);

        try {
            Thread.sleep(30);
        } catch (InterruptedException ignored) {}

        List<String> expired = timeoutManager.cleanIdleSessions();
        assertTrue(expired.contains("idle-sess"));
        assertEquals(0, registry.size());
    }
}
