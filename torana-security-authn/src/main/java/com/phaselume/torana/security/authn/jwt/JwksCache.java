package com.phaselume.torana.security.authn.jwt;

import com.nimbusds.jose.jwk.JWKSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches JWKS key sets in memory with optional Redis backing to prevent redundant JWKS fetches.
 */
public class JwksCache {

    private static final Logger log = LoggerFactory.getLogger(JwksCache.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final Map<String, CachedJWKSet> localCache = new ConcurrentHashMap<>();
    private final String redisKeyPrefix;

    public JwksCache(ReactiveStringRedisTemplate redisTemplate, String redisKeyPrefix) {
        this.redisTemplate = redisTemplate;
        this.redisKeyPrefix = redisKeyPrefix != null ? redisKeyPrefix : "torana:jwks:";
    }

    public JwksCache() {
        this(null, "torana:jwks:");
    }

    /**
     * Retrieves cached JWKSet or returns empty Mono.
     */
    public Mono<JWKSet> get(String issuerUri) {
        if (issuerUri == null) {
            return Mono.empty();
        }

        // Check local in-memory cache first
        CachedJWKSet local = localCache.get(issuerUri);
        if (local != null && !local.isExpired()) {
            return Mono.just(local.jwkSet);
        }

        // If Redis is available, check Redis
        if (redisTemplate != null) {
            return redisTemplate.opsForValue().get(redisKeyPrefix + issuerUri)
                    .flatMap(json -> {
                        try {
                            JWKSet jwkSet = JWKSet.parse(json);
                            localCache.put(issuerUri, new CachedJWKSet(jwkSet, System.currentTimeMillis() + 60_000));
                            return Mono.just(jwkSet);
                        } catch (ParseException e) {
                            log.warn("Failed to parse JWKS from Redis for issuer {}: {}", issuerUri, e.getMessage());
                            return Mono.empty();
                        }
                    });
        }

        return Mono.empty();
    }

    /**
     * Stores JWKSet in cache with specified TTL in seconds.
     */
    public Mono<Void> put(String issuerUri, JWKSet jwkSet, long ttlSeconds) {
        if (issuerUri == null || jwkSet == null) {
            return Mono.empty();
        }

        long ttlMillis = Math.max(ttlSeconds, 10) * 1000;
        localCache.put(issuerUri, new CachedJWKSet(jwkSet, System.currentTimeMillis() + ttlMillis));

        if (redisTemplate != null) {
            String json = jwkSet.toString(true);
            return redisTemplate.opsForValue()
                    .set(redisKeyPrefix + issuerUri, json, Duration.ofSeconds(ttlSeconds))
                    .then();
        }

        return Mono.empty();
    }

    /**
     * Invalidates cache entry for issuer (e.g. on key rotation).
     */
    public Mono<Void> invalidate(String issuerUri) {
        if (issuerUri == null) {
            return Mono.empty();
        }
        localCache.remove(issuerUri);
        if (redisTemplate != null) {
            return redisTemplate.delete(redisKeyPrefix + issuerUri).then();
        }
        return Mono.empty();
    }

    private static class CachedJWKSet {
        final JWKSet jwkSet;
        final long expiresAtMillis;

        CachedJWKSet(JWKSet jwkSet, long expiresAtMillis) {
            this.jwkSet = jwkSet;
            this.expiresAtMillis = expiresAtMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMillis;
        }
    }
}
