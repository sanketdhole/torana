package com.phaselume.torana.ratelimit.redis;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.RateLimitDecision;
import com.phaselume.torana.core.model.RateLimitPolicy;
import com.phaselume.torana.core.model.ToranaAuthentication;
import com.phaselume.torana.ratelimit.model.RateLimitKey;
import com.phaselume.torana.ratelimit.model.RateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class RedisRateLimiterTest {

    private SlidingWindowLuaScript luaScript;
    private RateLimitKeyResolver keyResolver;
    private RateLimitPolicyRegistry policyRegistry;
    private RedisRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        luaScript = Mockito.mock(SlidingWindowLuaScript.class);
        keyResolver = new RateLimitKeyResolver();
        policyRegistry = new RateLimitPolicyRegistry();
        rateLimiter = new RedisRateLimiter(luaScript, keyResolver, policyRegistry);
    }

    @Test
    void testAllowSingleStrategy() {
        when(luaScript.execute(any(RateLimitKey.class), anyLong(), anyLong()))
                .thenReturn(Mono.just(RateLimitResult.allowed(45, "torana:rl:by-user:route1:alice")));

        ToranaAuthentication auth = ToranaAuthentication.builder()
                .principalId("alice")
                .build();

        AgentContext context = AgentContext.builder()
                .authentication(auth)
                .attributes(Map.of("routeId", "route1"))
                .build();

        RateLimitPolicy policy = RateLimitPolicy.builder()
                .name("test-policy")
                .requestsPerMinute(60)
                .keyStrategy("by-user")
                .build();

        Mono<RateLimitDecision> decisionMono = rateLimiter.check(context, policy);

        StepVerifier.create(decisionMono)
                .assertNext(decision -> {
                    assertTrue(decision.isAllowed());
                    assertEquals(45, decision.getRemainingTokens());
                    assertEquals(60, decision.getTotalCapacity());
                    assertEquals(0, decision.getRetryAfterSeconds());
                })
                .verifyComplete();
    }

    @Test
    void testDenySingleStrategy() {
        when(luaScript.execute(any(RateLimitKey.class), anyLong(), anyLong()))
                .thenReturn(Mono.just(RateLimitResult.denied(12, "torana:rl:by-user:route1:bob")));

        ToranaAuthentication auth = ToranaAuthentication.builder()
                .principalId("bob")
                .build();

        AgentContext context = AgentContext.builder()
                .authentication(auth)
                .attributes(Map.of("routeId", "route1"))
                .build();

        RateLimitPolicy policy = RateLimitPolicy.builder()
                .name("test-policy")
                .requestsPerMinute(10)
                .keyStrategy("by-user")
                .build();

        Mono<RateLimitDecision> decisionMono = rateLimiter.check(context, policy);

        StepVerifier.create(decisionMono)
                .assertNext(decision -> {
                    assertFalse(decision.isAllowed());
                    assertEquals(0, decision.getRemainingTokens());
                    assertEquals(12, decision.getRetryAfterSeconds());
                    assertEquals("torana:rl:by-user:route1:bob", decision.getLimitKey());
                })
                .verifyComplete();
    }

    @Test
    void testMultiStrategyAggregationDeniesIfOneFails() {
        when(luaScript.execute(argThat(k -> k != null && "by-user".equalsIgnoreCase(k.getStrategy())), anyLong(), anyLong()))
                .thenReturn(Mono.just(RateLimitResult.allowed(15, "key-user")));
        when(luaScript.execute(argThat(k -> k != null && "by-tenant".equalsIgnoreCase(k.getStrategy())), anyLong(), anyLong()))
                .thenReturn(Mono.just(RateLimitResult.denied(25, "key-tenant")));

        ToranaAuthentication auth = ToranaAuthentication.builder()
                .principalId("charlie")
                .tenantId("tenant-99")
                .build();

        AgentContext context = AgentContext.builder()
                .authentication(auth)
                .attributes(Map.of("routeId", "route2"))
                .build();

        RateLimitPolicy policy = RateLimitPolicy.builder()
                .name("multi-policy")
                .requestsPerMinute(100)
                .keyStrategy("by-user, by-tenant")
                .build();

        Mono<RateLimitDecision> decisionMono = rateLimiter.check(context, policy);

        StepVerifier.create(decisionMono)
                .assertNext(decision -> {
                    assertFalse(decision.isAllowed());
                    assertEquals(25, decision.getRetryAfterSeconds());
                    assertEquals("key-tenant", decision.getLimitKey());
                })
                .verifyComplete();
    }
}
