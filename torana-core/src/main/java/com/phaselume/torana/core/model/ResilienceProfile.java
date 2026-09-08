package com.phaselume.torana.core.model;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;

/**
 * Immutable snapshot of a named resilience profile configuration.
 */
@Value
@Builder(toBuilder = true)
public class ResilienceProfile {

    String name;

    // Circuit Breaker settings
    @Builder.Default
    boolean circuitBreakerEnabled = true;
    @Builder.Default
    int failureRateThreshold = 50;
    @Builder.Default
    int slowCallRateThreshold = 100;
    @Builder.Default
    Duration slowCallDurationThreshold = Duration.ofSeconds(5);
    @Builder.Default
    int minimumNumberOfCalls = 10;
    @Builder.Default
    int slidingWindowSize = 100;
    @Builder.Default
    Duration waitDurationInOpenState = Duration.ofSeconds(10);

    // Bulkhead settings
    @Builder.Default
    boolean bulkheadEnabled = true;
    @Builder.Default
    int maxConcurrentCalls = 25;
    @Builder.Default
    Duration maxWaitDuration = Duration.ofMillis(500);

    // Retry settings
    @Builder.Default
    boolean retryEnabled = true;
    @Builder.Default
    int maxAttempts = 3;
    @Builder.Default
    Duration retryWaitDuration = Duration.ofMillis(500);
    @Builder.Default
    double retryBackoffMultiplier = 1.5;

    // TimeLimiter settings
    @Builder.Default
    boolean timeLimiterEnabled = true;
    @Builder.Default
    Duration timeoutDuration = Duration.ofSeconds(30);

    public static ResilienceProfile defaultProfile() {
        return ResilienceProfile.builder().name("default").build();
    }
}
