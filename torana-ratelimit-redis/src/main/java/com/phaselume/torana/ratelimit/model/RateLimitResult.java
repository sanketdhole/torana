package com.phaselume.torana.ratelimit.model;

import lombok.Builder;
import lombok.Value;

/**
 * Result returned from a single Redis rate limit evaluation.
 */
@Value
@Builder
public class RateLimitResult {

    boolean allowed;
    long remainingTokens;
    long retryAfterSeconds;
    String keyUsed;

    public static RateLimitResult allowed(long remainingTokens, String keyUsed) {
        return RateLimitResult.builder()
                .allowed(true)
                .remainingTokens(remainingTokens)
                .retryAfterSeconds(0)
                .keyUsed(keyUsed)
                .build();
    }

    public static RateLimitResult denied(long retryAfterSeconds, String keyUsed) {
        return RateLimitResult.builder()
                .allowed(false)
                .remainingTokens(0)
                .retryAfterSeconds(retryAfterSeconds)
                .keyUsed(keyUsed)
                .build();
    }
}
