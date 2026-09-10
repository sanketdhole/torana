package com.phaselume.torana.resilience.config;

import com.phaselume.torana.core.model.ResilienceProfile;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Resilience4jConfigFactoryTest {

    private Resilience4jConfigFactory factory;

    @BeforeEach
    void setUp() {
        factory = new Resilience4jConfigFactory();
    }

    @Test
    void testCreateCircuitBreakerConfigFromDefinition() {
        ResilienceProfileDefinition.CircuitBreakerProperties props = ResilienceProfileDefinition.CircuitBreakerProperties.builder()
                .failureRateThreshold(40)
                .slidingWindowSize(20)
                .minimumNumberOfCalls(8)
                .slidingWindowType("TIME_BASED")
                .waitDurationInOpenState(Duration.ofSeconds(15))
                .recordExceptions(List.of("java.lang.RuntimeException"))
                .build();

        CircuitBreakerConfig config = factory.createCircuitBreakerConfig(props);
        assertNotNull(config);
        assertEquals(40.0f, config.getFailureRateThreshold());
        assertEquals(20, config.getSlidingWindowSize());
        assertEquals(8, config.getMinimumNumberOfCalls());
        assertEquals(CircuitBreakerConfig.SlidingWindowType.TIME_BASED, config.getSlidingWindowType());
        assertEquals(15000L, config.getWaitIntervalFunctionInOpenState().apply(1));
    }

    @Test
    void testCreateCircuitBreakerConfigFromProfile() {
        ResilienceProfile profile = ResilienceProfile.builder()
                .failureRateThreshold(35)
                .slidingWindowSize(15)
                .minimumNumberOfCalls(5)
                .waitDurationInOpenState(Duration.ofSeconds(20))
                .build();

        CircuitBreakerConfig config = factory.createCircuitBreakerConfig(profile);
        assertNotNull(config);
        assertEquals(35.0f, config.getFailureRateThreshold());
        assertEquals(15, config.getSlidingWindowSize());
    }

    @Test
    void testCreateBulkheadConfig() {
        ResilienceProfileDefinition.BulkheadProperties props = ResilienceProfileDefinition.BulkheadProperties.builder()
                .maxConcurrentCalls(50)
                .maxWaitDuration(Duration.ofMillis(200))
                .build();

        BulkheadConfig config = factory.createBulkheadConfig(props);
        assertNotNull(config);
        assertEquals(50, config.getMaxConcurrentCalls());
        assertEquals(Duration.ofMillis(200), config.getMaxWaitDuration());
    }

    @Test
    void testCreateRetryConfig() {
        ResilienceProfileDefinition.RetryProperties props = ResilienceProfileDefinition.RetryProperties.builder()
                .maxAttempts(4)
                .waitDuration(Duration.ofMillis(300))
                .exponentialBackoffMultiplier(2.0)
                .build();

        RetryConfig config = factory.createRetryConfig(props);
        assertNotNull(config);
        assertEquals(4, config.getMaxAttempts());
    }

    @Test
    void testCreateTimeLimiterConfig() {
        ResilienceProfileDefinition.TimeLimiterProperties props = ResilienceProfileDefinition.TimeLimiterProperties.builder()
                .timeoutDuration(Duration.ofSeconds(8))
                .cancelRunningFuture(true)
                .build();

        TimeLimiterConfig config = factory.createTimeLimiterConfig(props);
        assertNotNull(config);
        assertEquals(Duration.ofSeconds(8), config.getTimeoutDuration());
        assertTrue(config.shouldCancelRunningFuture());
    }
}
