package com.phaselume.torana.resilience.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration model bound to YAML properties under torana.resilience.profiles.{name}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResilienceProfileDefinition {

    @Builder.Default
    private CircuitBreakerProperties circuitBreaker = new CircuitBreakerProperties();

    @Builder.Default
    private BulkheadProperties bulkhead = new BulkheadProperties();

    @Builder.Default
    private RetryProperties retry = new RetryProperties();

    @Builder.Default
    private TimeLimiterProperties timeLimiter = new TimeLimiterProperties();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CircuitBreakerProperties {
        @Builder.Default
        private boolean enabled = true;
        @Builder.Default
        private String slidingWindowType = "COUNT_BASED"; // COUNT_BASED or TIME_BASED
        @Builder.Default
        private int slidingWindowSize = 10;
        @Builder.Default
        private int failureRateThreshold = 50;
        @Builder.Default
        private Duration slowCallDurationThreshold = Duration.ofSeconds(2);
        @Builder.Default
        private int slowCallRateThreshold = 80;
        @Builder.Default
        private int minimumNumberOfCalls = 5;
        @Builder.Default
        private Duration waitDurationInOpenState = Duration.ofSeconds(30);
        @Builder.Default
        private int permittedNumberOfCallsInHalfOpenState = 3;
        @Builder.Default
        private boolean automaticTransitionFromOpenToHalfOpenEnabled = true;
        @Builder.Default
        private List<String> recordExceptions = new ArrayList<>();
        @Builder.Default
        private List<String> ignoreExceptions = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkheadProperties {
        @Builder.Default
        private boolean enabled = true;
        @Builder.Default
        private int maxConcurrentCalls = 25;
        @Builder.Default
        private Duration maxWaitDuration = Duration.ofMillis(100);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetryProperties {
        @Builder.Default
        private boolean enabled = true;
        @Builder.Default
        private int maxAttempts = 3;
        @Builder.Default
        private Duration waitDuration = Duration.ofMillis(500);
        @Builder.Default
        private double exponentialBackoffMultiplier = 2.0;
        @Builder.Default
        private List<String> retryOnExceptions = new ArrayList<>();
        @Builder.Default
        private List<String> ignoreExceptions = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeLimiterProperties {
        @Builder.Default
        private boolean enabled = true;
        @Builder.Default
        private Duration timeoutDuration = Duration.ofSeconds(10);
        @Builder.Default
        private boolean cancelRunningFuture = true;
    }
}
