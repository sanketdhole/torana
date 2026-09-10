package com.phaselume.torana.ratelimit.redis;

import com.phaselume.torana.ratelimit.model.RateLimitKey;
import com.phaselume.torana.ratelimit.model.RateLimitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Encapsulates the execution of the sliding window Redis Lua script.
 */
public class SlidingWindowLuaScript {

    private static final Logger log = LoggerFactory.getLogger(SlidingWindowLuaScript.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisScript<List> script;

    public SlidingWindowLuaScript(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.script = RedisScript.of(new ClassPathResource("scripts/sliding_window_rate_limit.lua"), List.class);
    }

    public SlidingWindowLuaScript(ReactiveStringRedisTemplate redisTemplate, RedisScript<List> script) {
        this.redisTemplate = redisTemplate;
        this.script = script;
    }

    public Mono<RateLimitResult> execute(RateLimitKey key, long windowSizeMs, long maxRequests) {
        if (redisTemplate == null) {
            log.warn("RedisTemplate is not configured, defaulting to allow");
            return Mono.just(RateLimitResult.allowed(maxRequests, key.getRedisKey()));
        }

        List<String> keys = List.of(key.getRedisKey());
        long now = System.currentTimeMillis();
        String eventId = UUID.randomUUID().toString();

        List<String> args = List.of(
                String.valueOf(windowSizeMs),
                String.valueOf(maxRequests),
                String.valueOf(now),
                eventId
        );

        return redisTemplate.execute(script, keys, args)
                .next()
                .map(rawList -> {
                    if (rawList != null && rawList.size() >= 3) {
                        long allowedVal = toLong(rawList.get(0));
                        long remaining = toLong(rawList.get(1));
                        long retryAfter = toLong(rawList.get(2));

                        if (allowedVal == 1L) {
                            return RateLimitResult.allowed(remaining, key.getRedisKey());
                        } else {
                            return RateLimitResult.denied(retryAfter, key.getRedisKey());
                        }
                    }
                    return RateLimitResult.allowed(maxRequests, key.getRedisKey());
                })
                .onErrorResume(ex -> {
                    log.error("Failed to execute rate limit Lua script for key {}: {}", key.getRedisKey(), ex.getMessage());
                    // Fail-open by default to prevent Redis hiccups from taking down all traffic
                    return Mono.just(RateLimitResult.allowed(maxRequests, key.getRedisKey()));
                });
    }

    private long toLong(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        if (obj != null) {
            try {
                return Long.parseLong(obj.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0L;
    }
}
