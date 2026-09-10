package com.phaselume.torana.resilience.decorator;

import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.model.ResilienceProfile;
import com.phaselume.torana.core.spi.ResilienceDecorator;
import com.phaselume.torana.resilience.registry.ResilienceInstanceFactory;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

/**
 * Implementation of ResilienceDecorator SPI wrapping Flux streams with Resilience4j patterns.
 */
public class Resilience4jResilienceDecorator implements ResilienceDecorator {

    private static final Logger log = LoggerFactory.getLogger(Resilience4jResilienceDecorator.class);

    private final ResilienceInstanceFactory instanceFactory;
    private final ResilienceEventListener eventListener;
    private final FallbackResponseFactory fallbackResponseFactory;

    public Resilience4jResilienceDecorator(ResilienceInstanceFactory instanceFactory,
                                           ResilienceEventListener eventListener,
                                           FallbackResponseFactory fallbackResponseFactory) {
        this.instanceFactory = instanceFactory;
        this.eventListener = eventListener != null ? eventListener : new ResilienceEventListener(null, null);
        this.fallbackResponseFactory = fallbackResponseFactory != null ? fallbackResponseFactory : new FallbackResponseFactory();
    }

    @Override
    public <T> Flux<T> decorate(String profileName, ResilienceProfile profile, Supplier<Flux<T>> publisherSupplier) {
        if (publisherSupplier == null) {
            return Flux.empty();
        }

        String name = (profileName != null && !profileName.isBlank()) ? profileName : "default";

        CircuitBreaker cb = instanceFactory.getCircuitBreaker(name, profile);
        Bulkhead bh = instanceFactory.getBulkhead(name, profile);
        Retry retry = instanceFactory.getRetry(name, profile);
        TimeLimiter tl = instanceFactory.getTimeLimiter(name, profile);

        eventListener.attachListeners(cb);
        eventListener.attachListeners(bh);
        eventListener.attachListeners(retry);
        eventListener.attachListeners(tl);

        Flux<T> source = Flux.defer(publisherSupplier);

        // Fixed composition order:
        // Source -> Retry -> Bulkhead -> CircuitBreaker -> TimeLimiter
        Flux<T> decorated = source
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(BulkheadOperator.of(bh))
                .transformDeferred(CircuitBreakerOperator.of(cb))
                .transformDeferred(TimeLimiterOperator.of(tl));

        return decorated.onErrorResume(ex -> {
            log.debug("Resilience decorator caught error for profile '{}': {}", name, ex.toString());
            if (fallbackResponseFactory.isResilienceFailure(ex)) {
                AgentResponse.Chunk chunk = fallbackResponseFactory.createFallbackChunk(ex, name);
                try {
                    @SuppressWarnings("unchecked")
                    T typedChunk = (T) chunk;
                    return Flux.just(typedChunk);
                } catch (Exception e) {
                    return Flux.error(ex);
                }
            }
            return Flux.error(ex);
        });
    }
}
