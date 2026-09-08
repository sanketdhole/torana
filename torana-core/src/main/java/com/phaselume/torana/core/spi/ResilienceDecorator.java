package com.phaselume.torana.core.spi;

import com.phaselume.torana.core.model.ResilienceProfile;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

/**
 * SPI for wrapping reactive publisher calls with resilience patterns (Circuit Breaker, Bulkhead, Retry, TimeLimiter).
 */
public interface ResilienceDecorator {

    /**
     * Decorates the supplier publisher with the named resilience profile.
     */
    <T> Flux<T> decorate(String profileName, ResilienceProfile profile, Supplier<Flux<T>> publisherSupplier);
}
