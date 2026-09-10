package com.phaselume.torana.resilience.config;

import com.phaselume.torana.core.model.ResilienceProfile;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory creating Resilience4j configuration instances from Torana profiles.
 */
public class Resilience4jConfigFactory {

    private static final Logger log = LoggerFactory.getLogger(Resilience4jConfigFactory.class);

    @SuppressWarnings("unchecked")
    public CircuitBreakerConfig createCircuitBreakerConfig(ResilienceProfileDefinition.CircuitBreakerProperties props) {
        if (props == null) {
            props = new ResilienceProfileDefinition.CircuitBreakerProperties();
        }

        CircuitBreakerConfig.Builder builder = CircuitBreakerConfig.custom()
                .failureRateThreshold(props.getFailureRateThreshold())
                .slowCallRateThreshold(props.getSlowCallRateThreshold())
                .slowCallDurationThreshold(props.getSlowCallDurationThreshold() != null ? props.getSlowCallDurationThreshold() : Duration.ofSeconds(2))
                .minimumNumberOfCalls(props.getMinimumNumberOfCalls())
                .slidingWindowSize(props.getSlidingWindowSize())
                .waitDurationInOpenState(props.getWaitDurationInOpenState() != null ? props.getWaitDurationInOpenState() : Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(props.getPermittedNumberOfCallsInHalfOpenState())
                .automaticTransitionFromOpenToHalfOpenEnabled(props.isAutomaticTransitionFromOpenToHalfOpenEnabled());

        if ("TIME_BASED".equalsIgnoreCase(props.getSlidingWindowType())) {
            builder.slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED);
        } else {
            builder.slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED);
        }

        List<Class<? extends Throwable>> recordClasses = resolveExceptionClasses(props.getRecordExceptions());
        if (!recordClasses.isEmpty()) {
            builder.recordExceptions(recordClasses.toArray(new Class[0]));
        }

        List<Class<? extends Throwable>> ignoreClasses = resolveExceptionClasses(props.getIgnoreExceptions());
        if (!ignoreClasses.isEmpty()) {
            builder.ignoreExceptions(ignoreClasses.toArray(new Class[0]));
        }

        return builder.build();
    }

    public CircuitBreakerConfig createCircuitBreakerConfig(ResilienceProfile profile) {
        if (profile == null) {
            return CircuitBreakerConfig.ofDefaults();
        }

        return CircuitBreakerConfig.custom()
                .failureRateThreshold(profile.getFailureRateThreshold())
                .slowCallRateThreshold(profile.getSlowCallRateThreshold())
                .slowCallDurationThreshold(profile.getSlowCallDurationThreshold() != null ? profile.getSlowCallDurationThreshold() : Duration.ofSeconds(5))
                .minimumNumberOfCalls(profile.getMinimumNumberOfCalls())
                .slidingWindowSize(profile.getSlidingWindowSize())
                .waitDurationInOpenState(profile.getWaitDurationInOpenState() != null ? profile.getWaitDurationInOpenState() : Duration.ofSeconds(10))
                .build();
    }

    public BulkheadConfig createBulkheadConfig(ResilienceProfileDefinition.BulkheadProperties props) {
        if (props == null) {
            props = new ResilienceProfileDefinition.BulkheadProperties();
        }

        return BulkheadConfig.custom()
                .maxConcurrentCalls(props.getMaxConcurrentCalls())
                .maxWaitDuration(props.getMaxWaitDuration() != null ? props.getMaxWaitDuration() : Duration.ofMillis(100))
                .build();
    }

    public BulkheadConfig createBulkheadConfig(ResilienceProfile profile) {
        if (profile == null) {
            return BulkheadConfig.ofDefaults();
        }

        return BulkheadConfig.custom()
                .maxConcurrentCalls(profile.getMaxConcurrentCalls())
                .maxWaitDuration(profile.getMaxWaitDuration() != null ? profile.getMaxWaitDuration() : Duration.ofMillis(500))
                .build();
    }

    @SuppressWarnings("unchecked")
    public RetryConfig createRetryConfig(ResilienceProfileDefinition.RetryProperties props) {
        if (props == null) {
            props = new ResilienceProfileDefinition.RetryProperties();
        }

        Duration waitDuration = props.getWaitDuration() != null ? props.getWaitDuration() : Duration.ofMillis(500);
        double multiplier = props.getExponentialBackoffMultiplier() > 1.0 ? props.getExponentialBackoffMultiplier() : 1.0;

        RetryConfig.Builder<Object> builder = RetryConfig.custom()
                .maxAttempts(props.getMaxAttempts());

        if (multiplier > 1.0) {
            builder.intervalFunction(IntervalFunction.ofExponentialBackoff(waitDuration, multiplier));
        } else {
            builder.waitDuration(waitDuration);
        }

        List<Class<? extends Throwable>> retryClasses = resolveExceptionClasses(props.getRetryOnExceptions());
        if (!retryClasses.isEmpty()) {
            builder.retryExceptions(retryClasses.toArray(new Class[0]));
        }

        List<Class<? extends Throwable>> ignoreClasses = resolveExceptionClasses(props.getIgnoreExceptions());
        if (!ignoreClasses.isEmpty()) {
            builder.ignoreExceptions(ignoreClasses.toArray(new Class[0]));
        }

        return builder.build();
    }

    public RetryConfig createRetryConfig(ResilienceProfile profile) {
        if (profile == null) {
            return RetryConfig.ofDefaults();
        }

        Duration waitDuration = profile.getRetryWaitDuration() != null ? profile.getRetryWaitDuration() : Duration.ofMillis(500);
        double multiplier = profile.getRetryBackoffMultiplier() > 1.0 ? profile.getRetryBackoffMultiplier() : 1.0;

        RetryConfig.Builder<Object> builder = RetryConfig.custom()
                .maxAttempts(profile.getMaxAttempts());

        if (multiplier > 1.0) {
            builder.intervalFunction(IntervalFunction.ofExponentialBackoff(waitDuration, multiplier));
        } else {
            builder.waitDuration(waitDuration);
        }

        return builder.build();
    }

    public TimeLimiterConfig createTimeLimiterConfig(ResilienceProfileDefinition.TimeLimiterProperties props) {
        if (props == null) {
            props = new ResilienceProfileDefinition.TimeLimiterProperties();
        }

        return TimeLimiterConfig.custom()
                .timeoutDuration(props.getTimeoutDuration() != null ? props.getTimeoutDuration() : Duration.ofSeconds(10))
                .cancelRunningFuture(props.isCancelRunningFuture())
                .build();
    }

    public TimeLimiterConfig createTimeLimiterConfig(ResilienceProfile profile) {
        if (profile == null) {
            return TimeLimiterConfig.ofDefaults();
        }

        return TimeLimiterConfig.custom()
                .timeoutDuration(profile.getTimeoutDuration() != null ? profile.getTimeoutDuration() : Duration.ofSeconds(30))
                .cancelRunningFuture(true)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<Class<? extends Throwable>> resolveExceptionClasses(List<String> classNames) {
        List<Class<? extends Throwable>> classes = new ArrayList<>();
        if (classNames == null) return classes;

        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className.trim());
                if (Throwable.class.isAssignableFrom(clazz)) {
                    classes.add((Class<? extends Throwable>) clazz);
                } else {
                    log.warn("Class {} does not extend Throwable, skipping", className);
                }
            } catch (ClassNotFoundException e) {
                log.warn("Could not find exception class for resilience config: {}", className);
            }
        }
        return classes;
    }
}
