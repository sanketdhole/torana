package com.phaselume.torana.core.model;

import lombok.Builder;
import lombok.Value;

/**
 * Result of a distributed rate-limit check (e.g. from Redis token bucket).
 */
@Value
@Builder(toBuilder = true)
public class RateLimitDecision {

    boolean allowed;
    long remainingTokens;
    long totalCapacity;
    long retryAfterSeconds;
    String limitKey;

    public static RateLimitDecision allow(long remainingTokens, long totalCapacity, String limitKey) {
        return RateLimitDecision.builder()
                .allowed(true)
                .remainingTokens(remainingTokens)
                .totalCapacity(totalCapacity)
                .retryAfterSeconds(0)
                .limitKey(limitKey)
                .build();
    }

    public static RateLimitDecision deny(long retryAfterSeconds, String limitKey) {
        return RateLimitDecision.builder()
                .allowed(false)
                .remainingTokens(0)
                .retryAfterSeconds(retryAfterSeconds)
                .limitKey(limitKey)
                .build();
    }
}
