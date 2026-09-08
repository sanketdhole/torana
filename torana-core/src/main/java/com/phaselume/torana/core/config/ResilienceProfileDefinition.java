package com.phaselume.torana.core.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

/**
 * Definition of a named resilience profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResilienceProfileDefinition {

    private String name;

    @Builder.Default
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();

    @Builder.Default
    private BulkheadConfig bulkhead = new BulkheadConfig();

    @Builder.Default
    private RetryConfig retry = new RetryConfig();

    @Builder.Default
    private TimeLimiterConfig timeLimiter = new TimeLimiterConfig();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CircuitBreakerConfig {
        @Builder.Default
        private boolean enabled = true;
        @Builder.Default
        private int failureRateThreshold = 50;
        @Builder.Default
        private int slowCallRateThreshold = 100;
        @Builder.Default
        private Duration slowCallDurationThreshold = Duration.ofSeconds(5);
        @Builder.Default
        private int minimumNumberOfCalls = 10;
        @Builder.Default
        private int slidingWindowSize = 100;
        @Builder.Default
        private Duration waitDurationInOpenState = Duration.ofSeconds(10);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkheadConfig {
        @Builder.Default
        private boolean enabled = true;
        @Builder.Default
        private int maxConcurrentCalls = 25;
        @Builder.Default
        private Duration maxWaitDuration = Duration.ofMillis(500);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetryConfig {
        @Builder.Default
        private boolean enabled = true;
        @Builder.Default
        private int maxAttempts = 3;
        @Builder.Default
        private Duration waitDuration = Duration.ofMillis(500);
        @Builder.Default
        private double backoffMultiplier = 1.5;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeLimiterConfig {
        @Builder.Default
        private boolean enabled = true;
        @Builder.Default
        private Duration timeoutDuration = Duration.ofSeconds(30);
    }
}
