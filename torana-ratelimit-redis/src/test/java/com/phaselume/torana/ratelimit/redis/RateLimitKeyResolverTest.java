package com.phaselume.torana.ratelimit.redis;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.ToranaAuthentication;
import com.phaselume.torana.ratelimit.model.RateLimitKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitKeyResolverTest {

    private RateLimitKeyResolver keyResolver;

    @BeforeEach
    void setUp() {
        keyResolver = new RateLimitKeyResolver();
    }

    @Test
    void testResolveUserKey() {
        ToranaAuthentication auth = ToranaAuthentication.builder()
                .principalId("alice")
                .tenantId("tenant-1")
                .build();

        AgentContext context = AgentContext.builder()
                .authentication(auth)
                .attributes(Map.of("routeId", "chat-route"))
                .build();

        List<RateLimitKey> keys = keyResolver.resolveKeys(context, "by-user");
        assertEquals(1, keys.size());
        assertEquals("by-user", keys.get(0).getStrategy());
        assertEquals("chat-route", keys.get(0).getRouteId());
        assertEquals("alice", keys.get(0).getDiscriminator());
        assertEquals("torana:rl:by-user:chat-route:alice", keys.get(0).getRedisKey());
    }

    @Test
    void testResolveTenantKey() {
        ToranaAuthentication auth = ToranaAuthentication.builder()
                .principalId("bob")
                .tenantId("org-42")
                .build();

        AgentContext context = AgentContext.builder()
                .authentication(auth)
                .attributes(Map.of("routeId", "completion-route"))
                .build();

        List<RateLimitKey> keys = keyResolver.resolveKeys(context, "by-tenant");
        assertEquals(1, keys.size());
        assertEquals("org-42", keys.get(0).getDiscriminator());
        assertEquals("torana:rl:by-tenant:completion-route:org-42", keys.get(0).getRedisKey());
    }

    @Test
    void testResolveIpKey() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-For", "203.0.113.195, 10.0.0.1");

        AgentRequest req = AgentRequest.builder()
                .headers(headers)
                .build();

        AgentContext context = AgentContext.builder()
                .request(req)
                .attributes(Map.of("routeId", "public-api"))
                .build();

        List<RateLimitKey> keys = keyResolver.resolveKeys(context, "by-ip");
        assertEquals(1, keys.size());
        assertEquals("203.0.113.195", keys.get(0).getDiscriminator());
        assertEquals("torana:rl:by-ip:public-api:203.0.113.195", keys.get(0).getRedisKey());
    }

    @Test
    void testResolveApiKey() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", "sk_live_12345");

        AgentRequest req = AgentRequest.builder()
                .headers(headers)
                .build();

        AgentContext context = AgentContext.builder()
                .request(req)
                .attributes(Map.of("routeId", "mcp-route"))
                .build();

        List<RateLimitKey> keys = keyResolver.resolveKeys(context, "by-api-key");
        assertEquals(1, keys.size());
        assertEquals("sk_live_12345", keys.get(0).getDiscriminator());
    }

    @Test
    void testResolveMultiStrategyStacking() {
        ToranaAuthentication auth = ToranaAuthentication.builder()
                .principalId("charlie")
                .tenantId("tenant-x")
                .build();

        AgentContext context = AgentContext.builder()
                .authentication(auth)
                .attributes(Map.of("routeId", "secure-route"))
                .build();

        List<RateLimitKey> keys = keyResolver.resolveKeys(context, "by-user, by-tenant, by-route");
        assertEquals(3, keys.size());
        assertEquals("charlie", keys.get(0).getDiscriminator());
        assertEquals("tenant-x", keys.get(1).getDiscriminator());
        assertEquals("secure-route", keys.get(2).getDiscriminator());
    }
}
