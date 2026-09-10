package com.phaselume.torana.ratelimit.redis;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.RateLimitDecision;
import com.phaselume.torana.core.model.RateLimitPolicy;
import com.phaselume.torana.core.spi.RateLimiter;
import com.phaselume.torana.ratelimit.model.RateLimitKey;
import com.phaselume.torana.ratelimit.model.RateLimitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Distributed rate limiter implementation using Redis sliding-window sorted sets.
 */
public class RedisRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private final SlidingWindowLuaScript luaScript;
    private final RateLimitKeyResolver keyResolver;
    private final RateLimitPolicyRegistry policyRegistry;

    public RedisRateLimiter(SlidingWindowLuaScript luaScript,
                            RateLimitKeyResolver keyResolver,
                            RateLimitPolicyRegistry policyRegistry) {
        this.luaScript = luaScript;
        this.keyResolver = keyResolver != null ? keyResolver : new RateLimitKeyResolver();
        this.policyRegistry = policyRegistry != null ? policyRegistry : new RateLimitPolicyRegistry();
    }

    @Override
    public Mono<RateLimitDecision> check(AgentContext context, RateLimitPolicy policy) {
        RateLimitPolicy effectivePolicy = policy != null ? policy : policyRegistry.get("default");

        List<RateLimitKey> keys = keyResolver.resolveKeys(context, effectivePolicy.getKeyStrategy());
        long windowSizeMs = 60_000L;
        long limit = effectivePolicy.getRequestsPerMinute() > 0 ? effectivePolicy.getRequestsPerMinute() : 60L;

        return Flux.fromIterable(keys)
                .flatMap(key -> luaScript.execute(key, windowSizeMs, limit))
                .collectList()
                .map(results -> aggregateDecisions(results, limit, keys));
    }

    private RateLimitDecision aggregateDecisions(List<RateLimitResult> results, long totalCapacity, List<RateLimitKey> keys) {
        if (results == null || results.isEmpty()) {
            return RateLimitDecision.allow(totalCapacity, totalCapacity, "none");
        }

        boolean allAllowed = true;
        long minRemaining = totalCapacity;
        long maxRetryAfter = 0;
        String deniedKey = "";
        String primaryKey = keys.get(0).getRedisKey();

        for (RateLimitResult res : results) {
            if (!res.isAllowed()) {
                allAllowed = false;
                if (res.getRetryAfterSeconds() > maxRetryAfter) {
                    maxRetryAfter = res.getRetryAfterSeconds();
                }
                deniedKey = res.getKeyUsed();
            } else {
                if (res.getRemainingTokens() < minRemaining) {
                    minRemaining = res.getRemainingTokens();
                }
            }
        }

        if (allAllowed) {
            return RateLimitDecision.allow(minRemaining, totalCapacity, primaryKey);
        } else {
            return RateLimitDecision.deny(maxRetryAfter, deniedKey);
        }
    }
}
