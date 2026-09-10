package com.phaselume.torana.resilience.registry;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;

/**
 * Node-local in-memory registry holding the core Resilience4j registries.
 */
public class ConnectorResilienceRegistry {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final RetryRegistry retryRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;

    public ConnectorResilienceRegistry() {
        this.circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        this.bulkheadRegistry = BulkheadRegistry.ofDefaults();
        this.retryRegistry = RetryRegistry.ofDefaults();
        this.timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
    }

    public ConnectorResilienceRegistry(CircuitBreakerRegistry circuitBreakerRegistry,
                                       BulkheadRegistry bulkheadRegistry,
                                       RetryRegistry retryRegistry,
                                       TimeLimiterRegistry timeLimiterRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry != null ? circuitBreakerRegistry : CircuitBreakerRegistry.ofDefaults();
        this.bulkheadRegistry = bulkheadRegistry != null ? bulkheadRegistry : BulkheadRegistry.ofDefaults();
        this.retryRegistry = retryRegistry != null ? retryRegistry : RetryRegistry.ofDefaults();
        this.timeLimiterRegistry = timeLimiterRegistry != null ? timeLimiterRegistry : TimeLimiterRegistry.ofDefaults();
    }

    public CircuitBreakerRegistry getCircuitBreakerRegistry() {
        return circuitBreakerRegistry;
    }

    public BulkheadRegistry getBulkheadRegistry() {
        return bulkheadRegistry;
    }

    public RetryRegistry getRetryRegistry() {
        return retryRegistry;
    }

    public TimeLimiterRegistry getTimeLimiterRegistry() {
        return timeLimiterRegistry;
    }
}
