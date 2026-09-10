package com.phaselume.torana.resilience.decorator;

import com.phaselume.torana.core.model.AgentResponse;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Creates structured fallback chunks and responses when resilience policies are tripped.
 */
public class FallbackResponseFactory {

    public boolean isResilienceFailure(Throwable throwable) {
        return throwable instanceof CallNotPermittedException
                || throwable instanceof BulkheadFullException
                || throwable instanceof TimeoutException;
    }

    public AgentResponse.Chunk createFallbackChunk(Throwable throwable, String profileName) {
        HttpStatus status;
        String errorCode;
        String message;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("resilienceProfile", profileName != null ? profileName : "default");

        if (throwable instanceof CallNotPermittedException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            errorCode = "CIRCUIT_BREAKER_OPEN";
            message = "Service unavailable: Circuit breaker is OPEN for profile " + profileName;
            metadata.put("X-Torana-CB-State", "OPEN");
        } else if (throwable instanceof BulkheadFullException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            errorCode = "BULKHEAD_FULL";
            message = "Service unavailable: Concurrency bulkhead limit exceeded for profile " + profileName;
            metadata.put("X-Torana-Bulkhead-State", "FULL");
        } else if (throwable instanceof TimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            errorCode = "GATEWAY_TIMEOUT";
            message = "Gateway timeout: Request exceeded maximum duration for profile " + profileName;
            metadata.put("X-Torana-Timeout", "EXCEEDED");
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorCode = "INTERNAL_RESILIENCE_ERROR";
            message = throwable != null ? throwable.getMessage() : "Unknown resilience error";
        }

        metadata.put("status", status.value());
        metadata.put("error", errorCode);

        String jsonPayload = String.format(
                "{\"error\":\"%s\",\"message\":\"%s\",\"status\":%d}",
                errorCode, message, status.value()
        );

        return AgentResponse.Chunk.builder()
                .textDelta(message)
                .data(jsonPayload.getBytes(StandardCharsets.UTF_8))
                .last(true)
                .finishReason("error")
                .metadata(metadata)
                .build();
    }
}
