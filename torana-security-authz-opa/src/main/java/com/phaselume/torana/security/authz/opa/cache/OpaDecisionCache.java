package com.phaselume.torana.security.authz.opa.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthzProperties.OpaCacheProperties;
import com.phaselume.torana.security.authz.opa.model.OpaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches OPA authorization decisions to eliminate redundant network hops for high-frequency identical requests.
 */
public class OpaDecisionCache {

    private static final Logger log = LoggerFactory.getLogger(OpaDecisionCache.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final OpaCacheProperties cacheProperties;
    private final ObjectMapper objectMapper;
    private final Map<String, CachedDecision> localCache = new ConcurrentHashMap<>();

    public OpaDecisionCache(ReactiveStringRedisTemplate redisTemplate,
                            OpaCacheProperties cacheProperties,
                            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.cacheProperties = cacheProperties != null ? cacheProperties : new OpaCacheProperties();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public OpaDecisionCache(OpaCacheProperties cacheProperties) {
        this(null, cacheProperties, new ObjectMapper());
    }

    public OpaDecisionCache() {
        this(null, new OpaCacheProperties(), new ObjectMapper());
    }

    /**
     * Looks up a cached OPA response or returns empty Mono.
     */
    public Mono<OpaResponse> get(String policyPath, Map<String, Object> input) {
        if (!cacheProperties.isEnabled()) {
            return Mono.empty();
        }

        String cacheKey = computeCacheKey(policyPath, input);

        // Check local cache
        CachedDecision local = localCache.get(cacheKey);
        if (local != null && !local.isExpired()) {
            return Mono.just(local.response);
        }

        // Check Redis if available
        if (redisTemplate != null) {
            return redisTemplate.opsForValue().get(cacheProperties.getRedisPrefix() + cacheKey)
                    .flatMap(json -> {
                        try {
                            OpaResponse resp = objectMapper.readValue(json, OpaResponse.class);
                            localCache.put(cacheKey, new CachedDecision(resp, System.currentTimeMillis() + 5000));
                            return Mono.just(resp);
                        } catch (Exception e) {
                            log.warn("Failed to parse cached OPA decision from Redis: {}", e.getMessage());
                            return Mono.empty();
                        }
                    });
        }

        return Mono.empty();
    }

    /**
     * Stores an OPA decision in cache.
     */
    public Mono<Void> put(String policyPath, Map<String, Object> input, OpaResponse response) {
        if (!cacheProperties.isEnabled() || response == null) {
            return Mono.empty();
        }

        String cacheKey = computeCacheKey(policyPath, input);
        long ttlMillis = Math.max(cacheProperties.getTtlSeconds(), 1) * 1000;
        localCache.put(cacheKey, new CachedDecision(response, System.currentTimeMillis() + ttlMillis));

        if (redisTemplate != null) {
            try {
                String json = objectMapper.writeValueAsString(response);
                return redisTemplate.opsForValue()
                        .set(cacheProperties.getRedisPrefix() + cacheKey, json, Duration.ofSeconds(cacheProperties.getTtlSeconds()))
                        .then();
            } catch (Exception e) {
                log.error("Failed to serialize OPA decision for Redis cache: {}", e.getMessage());
            }
        }

        return Mono.empty();
    }

    /**
     * Clears all local cache entries.
     */
    public void clearLocal() {
        localCache.clear();
    }

    public String computeCacheKey(String policyPath, Map<String, Object> input) {
        try {
            String json = objectMapper.writeValueAsString(input);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((policyPath + ":" + json).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return String.valueOf((policyPath + ":" + input).hashCode());
        }
    }

    private static class CachedDecision {
        final OpaResponse response;
        final long expiresAtMillis;

        CachedDecision(OpaResponse response, long expiresAtMillis) {
            this.response = response;
            this.expiresAtMillis = expiresAtMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMillis;
        }
    }
}
