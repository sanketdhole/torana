package com.phaselume.torana.ratelimit.filter;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.RateLimitDecision;
import com.phaselume.torana.core.model.RateLimitPolicy;
import com.phaselume.torana.core.spi.RateLimiter;
import com.phaselume.torana.ratelimit.redis.RateLimitPolicyRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class RateLimitWebFilterTest {

    private RateLimiter rateLimiter;
    private RateLimitPolicyRegistry policyRegistry;
    private RateLimitResponseWriter responseWriter;
    private RateLimitWebFilter webFilter;

    @BeforeEach
    void setUp() {
        rateLimiter = Mockito.mock(RateLimiter.class);
        policyRegistry = new RateLimitPolicyRegistry();
        responseWriter = new RateLimitResponseWriter();
        webFilter = new RateLimitWebFilter(rateLimiter, policyRegistry, responseWriter);
    }

    @Test
    void testFilterAllowsAndContinuesChain() {
        when(rateLimiter.check(any(AgentContext.class), any(RateLimitPolicy.class)))
                .thenReturn(Mono.just(RateLimitDecision.allow(95, 100, "key1")));

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/tools").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        WebFilterChain chain = ex -> {
            chainInvoked.set(true);
            return Mono.empty();
        };

        StepVerifier.create(webFilter.filter(exchange, chain))
                .verifyComplete();

        assertTrue(chainInvoked.get());
        assertEquals("100", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"));
        assertEquals("95", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining"));
    }

    @Test
    void testFilterDeniesAndShortCircuitsWith429() {
        when(rateLimiter.check(any(AgentContext.class), any(RateLimitPolicy.class)))
                .thenReturn(Mono.just(RateLimitDecision.deny(15, "key-denied")));

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/tools").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        WebFilterChain chain = ex -> {
            chainInvoked.set(true);
            return Mono.empty();
        };

        StepVerifier.create(webFilter.filter(exchange, chain))
                .verifyComplete();

        assertFalse(chainInvoked.get(), "Downstream chain should not be invoked when rate limit is exceeded");
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
        assertEquals("15", exchange.getResponse().getHeaders().getFirst("Retry-After"));
    }
}
