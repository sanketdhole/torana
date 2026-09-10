package com.phaselume.torana.resilience.registry;

import com.phaselume.torana.core.model.ResilienceProfile;
import com.phaselume.torana.resilience.config.Resilience4jConfigFactory;
import com.phaselume.torana.resilience.config.ResilienceProfileDefinition;
import com.phaselume.torana.resilience.config.ResilienceProfileRegistry;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

/**
 * Creates and retrieves Resilience4j instances from their respective registries.
 */
public class ResilienceInstanceFactory {

    private final ConnectorResilienceRegistry registry;
    private final ResilienceProfileRegistry profileRegistry;
    private final Resilience4jConfigFactory configFactory;

    public ResilienceInstanceFactory(ConnectorResilienceRegistry registry,
                                     ResilienceProfileRegistry profileRegistry,
                                     Resilience4jConfigFactory configFactory) {
        this.registry = registry != null ? registry : new ConnectorResilienceRegistry();
        this.profileRegistry = profileRegistry != null ? profileRegistry : new ResilienceProfileRegistry();
        this.configFactory = configFactory != null ? configFactory : new Resilience4jConfigFactory();
    }

    public CircuitBreaker getCircuitBreaker(String name, ResilienceProfile profile) {
        if (profile != null) {
            CircuitBreakerConfig config = configFactory.createCircuitBreakerConfig(profile);
            return registry.getCircuitBreakerRegistry().circuitBreaker(name, config);
        }
        ResilienceProfileDefinition def = profileRegistry.get(name);
        CircuitBreakerConfig config = configFactory.createCircuitBreakerConfig(def.getCircuitBreaker());
        return registry.getCircuitBreakerRegistry().circuitBreaker(name, config);
    }

    public Bulkhead getBulkhead(String name, ResilienceProfile profile) {
        if (profile != null) {
            BulkheadConfig config = configFactory.createBulkheadConfig(profile);
            return registry.getBulkheadRegistry().bulkhead(name, config);
        }
        ResilienceProfileDefinition def = profileRegistry.get(name);
        BulkheadConfig config = configFactory.createBulkheadConfig(def.getBulkhead());
        return registry.getBulkheadRegistry().bulkhead(name, config);
    }

    public Retry getRetry(String name, ResilienceProfile profile) {
        if (profile != null) {
            RetryConfig config = configFactory.createRetryConfig(profile);
            return registry.getRetryRegistry().retry(name, config);
        }
        ResilienceProfileDefinition def = profileRegistry.get(name);
        RetryConfig config = configFactory.createRetryConfig(def.getRetry());
        return registry.getRetryRegistry().retry(name, config);
    }

    public TimeLimiter getTimeLimiter(String name, ResilienceProfile profile) {
        if (profile != null) {
            TimeLimiterConfig config = configFactory.createTimeLimiterConfig(profile);
            return registry.getTimeLimiterRegistry().timeLimiter(name, config);
        }
        ResilienceProfileDefinition def = profileRegistry.get(name);
        TimeLimiterConfig config = configFactory.createTimeLimiterConfig(def.getTimeLimiter());
        return registry.getTimeLimiterRegistry().timeLimiter(name, config);
    }
}
