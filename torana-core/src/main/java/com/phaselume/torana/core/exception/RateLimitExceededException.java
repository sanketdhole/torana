package com.phaselume.torana.core.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Thrown when rate limit quotas are exhausted.
 */
public class RateLimitExceededException extends ToranaException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super("RATE_LIMIT_EXCEEDED", message, HttpStatus.TOO_MANY_REQUESTS, null, Map.of("retryAfterSeconds", retryAfterSeconds));
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
