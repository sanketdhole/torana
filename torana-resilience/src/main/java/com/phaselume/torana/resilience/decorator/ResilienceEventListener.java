package com.phaselume.torana.resilience.decorator;

import com.phaselume.torana.resilience.registry.ConnectorResilienceRegistry;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.micrometer.tagged.TaggedBulkheadMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedTimeLimiterMetrics;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens to Resilience4j events and registers Micrometer metrics if a registry is present.
 */
public class ResilienceEventListener {

    private static final Logger log = LoggerFactory.getLogger(ResilienceEventListener.class);

    public ResilienceEventListener(ConnectorResilienceRegistry registry, MeterRegistry meterRegistry) {
        if (registry != null && meterRegistry != null) {
            try {
                TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry.getCircuitBreakerRegistry()).bindTo(meterRegistry);
                TaggedBulkheadMetrics.ofBulkheadRegistry(registry.getBulkheadRegistry()).bindTo(meterRegistry);
                TaggedRetryMetrics.ofRetryRegistry(registry.getRetryRegistry()).bindTo(meterRegistry);
                TaggedTimeLimiterMetrics.ofTimeLimiterRegistry(registry.getTimeLimiterRegistry()).bindTo(meterRegistry);
                log.info("Successfully bound Resilience4j metrics to Micrometer MeterRegistry");
            } catch (Exception e) {
                log.warn("Failed to bind Resilience4j metrics to MeterRegistry: {}", e.getMessage());
            }
        }
    }

    public void attachListeners(CircuitBreaker cb) {
        if (cb == null) return;
        cb.getEventPublisher()
                .onStateTransition(event -> log.warn("CircuitBreaker '{}' state transition: {}", cb.getName(), event.getStateTransition()))
                .onCallNotPermitted(event -> log.debug("CircuitBreaker '{}' call rejected (not permitted)", cb.getName()))
                .onError(event -> log.debug("CircuitBreaker '{}' recorded error: {}", cb.getName(), event.getThrowable().getMessage()));
    }

    public void attachListeners(Bulkhead bulkhead) {
        if (bulkhead == null) return;
        bulkhead.getEventPublisher()
                .onCallRejected(event -> log.warn("Bulkhead '{}' call rejected: concurrency limit reached", bulkhead.getName()));
    }

    public void attachListeners(Retry retry) {
        if (retry == null) return;
        retry.getEventPublisher()
                .onRetry(event -> log.info("Retry '{}' attempt #{} after failure: {}", retry.getName(), event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()));
    }

    public void attachListeners(TimeLimiter timeLimiter) {
        if (timeLimiter == null) return;
        timeLimiter.getEventPublisher()
                .onTimeout(event -> log.warn("TimeLimiter '{}' call timed out", timeLimiter.getName()));
    }
}
