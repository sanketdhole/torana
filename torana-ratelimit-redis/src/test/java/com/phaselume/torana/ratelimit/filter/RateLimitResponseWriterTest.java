package com.phaselume.torana.ratelimit.filter;

import com.phaselume.torana.core.model.RateLimitDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitResponseWriterTest {

    private RateLimitResponseWriter responseWriter;

    @BeforeEach
    void setUp() {
        responseWriter = new RateLimitResponseWriter();
    }

    @Test
    void testWriteRateLimitExceeded() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/chat").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        RateLimitDecision decision = RateLimitDecision.builder()
                .allowed(false)
                .retryAfterSeconds(30)
                .totalCapacity(100)
                .remainingTokens(0)
                .limitKey("torana:rl:by-user:chat:user1")
                .build();

        StepVerifier.create(responseWriter.writeRateLimitExceeded(exchange, decision))
                .verifyComplete();

        MockServerHttpResponse response = exchange.getResponse();
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
        assertEquals("30", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertEquals("100", response.getHeaders().getFirst("X-RateLimit-Limit"));
        assertEquals("0", response.getHeaders().getFirst("X-RateLimit-Remaining"));

        String body = response.getBodyAsString().block();
        assertNotNull(body);
        assertTrue(body.contains("rate_limit_exceeded"));
        assertTrue(body.contains("\"retryAfter\":30"));
    }

    @Test
    void testInjectRateLimitHeaders() {
        MockServerHttpResponse response = new MockServerHttpResponse();
        RateLimitDecision decision = RateLimitDecision.allow(50, 100, "key1");

        responseWriter.injectRateLimitHeaders(response, decision);

        assertEquals("100", response.getHeaders().getFirst("X-RateLimit-Limit"));
        assertEquals("50", response.getHeaders().getFirst("X-RateLimit-Remaining"));
    }
}
