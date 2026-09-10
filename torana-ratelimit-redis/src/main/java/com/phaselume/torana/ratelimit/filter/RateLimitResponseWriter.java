package com.phaselume.torana.ratelimit.filter;

import com.phaselume.torana.core.model.RateLimitDecision;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Formats and writes standard RFC 7807 / JSON 429 responses when rate limits are exceeded.
 */
public class RateLimitResponseWriter {

    public Mono<Void> writeRateLimitExceeded(ServerWebExchange exchange, RateLimitDecision decision) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        long retryAfter = decision != null && decision.getRetryAfterSeconds() > 0 ? decision.getRetryAfterSeconds() : 1;
        long totalCapacity = decision != null ? decision.getTotalCapacity() : 0;

        response.getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
        response.getHeaders().set("X-RateLimit-Limit", String.valueOf(totalCapacity));
        response.getHeaders().set("X-RateLimit-Remaining", "0");

        String jsonPayload = String.format(
                "{\"error\":\"rate_limit_exceeded\",\"message\":\"Rate limit exceeded. Please retry after %d seconds.\",\"retryAfter\":%d,\"limit\":%d,\"remaining\":0,\"status\":429}",
                retryAfter, retryAfter, totalCapacity
        );

        byte[] bytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }

    public void injectRateLimitHeaders(ServerHttpResponse response, RateLimitDecision decision) {
        if (response == null || decision == null) return;
        response.getHeaders().set("X-RateLimit-Limit", String.valueOf(decision.getTotalCapacity()));
        response.getHeaders().set("X-RateLimit-Remaining", String.valueOf(decision.getRemainingTokens()));
    }
}
