package com.phaselume.torana.security.authn.apikey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Facilitates zero-downtime API key rotation and key invalidation.
 */
public class ApiKeyRotationSupport {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyRotationSupport.class);

    private final ApiKeyValidator validator;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final String redisPrefix;

    public ApiKeyRotationSupport(ApiKeyValidator validator, ReactiveStringRedisTemplate redisTemplate, String redisPrefix) {
        this.validator = validator;
        this.redisTemplate = redisTemplate;
        this.redisPrefix = redisPrefix != null ? redisPrefix : "torana:apikey:";
    }

    /**
     * Rotates an existing key to a new key. The old key remains valid until the grace period expires.
     */
    public Mono<Void> rotateKey(String oldRawKey, String newRawKey, ApiKeyMetadata newMetadata, Duration gracePeriod) {
        if (newRawKey == null || newMetadata == null) {
            return Mono.error(new IllegalArgumentException("New API key and metadata must not be null"));
        }

        // Register new key immediately
        validator.registerKey(newRawKey, newMetadata);

        if (oldRawKey != null && gracePeriod != null && redisTemplate != null) {
            String oldHash = ApiKeyValidator.hashKey(oldRawKey);
            // Set expiration on old key in Redis
            return redisTemplate.expire(redisPrefix + oldHash, gracePeriod)
                    .doOnSuccess(ok -> log.info("Scheduled expiration for old API key {} in {}", oldHash, gracePeriod))
                    .then();
        }

        return Mono.empty();
    }

    /**
     * Immediately revokes an API key.
     */
    public Mono<Void> invalidateKey(String rawKey) {
        if (rawKey == null) {
            return Mono.empty();
        }
        String keyHash = ApiKeyValidator.hashKey(rawKey);
        if (redisTemplate != null) {
            return redisTemplate.delete(redisPrefix + keyHash).then();
        }
        return Mono.empty();
    }
}
