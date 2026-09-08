package com.phaselume.torana.core.spi;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.RateLimitDecision;
import com.phaselume.torana.core.model.RateLimitPolicy;
import reactor.core.publisher.Mono;

/**
 * SPI for distributed rate limiting checks.
 */
public interface RateLimiter {

    /**
     * Evaluate rate limiting status for this context against the requested policy.
     */
    Mono<RateLimitDecision> check(AgentContext context, RateLimitPolicy policy);
}
