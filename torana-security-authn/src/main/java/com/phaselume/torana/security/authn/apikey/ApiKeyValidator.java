package com.phaselume.torana.security.authn.apikey;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates API keys by calculating SHA-256 hash and looking up permissions in Redis / in-memory store.
 */
public class ApiKeyValidator {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyValidator.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final String redisPrefix;
    private final ObjectMapper objectMapper;
    private final Map<String, ApiKeyMetadata> inMemoryStore = new ConcurrentHashMap<>();

    public ApiKeyValidator(ReactiveStringRedisTemplate redisTemplate, String redisPrefix, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.redisPrefix = redisPrefix != null ? redisPrefix : "torana:apikey:";
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public ApiKeyValidator() {
        this(null, "torana:apikey:", new ObjectMapper());
    }

    /**
     * Stores or registers an API key metadata entry directly (for testing or bootstrap).
     */
    public void registerKey(String rawKey, ApiKeyMetadata metadata) {
        if (rawKey != null && metadata != null) {
            String hash = hashKey(rawKey);
            inMemoryStore.put(hash, metadata);
            if (redisTemplate != null) {
                try {
                    String json = objectMapper.writeValueAsString(metadata);
                    redisTemplate.opsForValue().set(redisPrefix + hash, json).subscribe();
                } catch (Exception e) {
                    log.error("Failed to serialize API key metadata for Redis: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Validates a raw API key and returns its metadata if valid, active, and not expired.
     */
    public Mono<ApiKeyMetadata> validate(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return Mono.empty();
        }

        String keyHash = hashKey(rawKey);

        // Check in-memory store first
        ApiKeyMetadata local = inMemoryStore.get(keyHash);
        if (local != null) {
            if (!local.isActive() || local.isExpired()) {
                return Mono.error(new IllegalArgumentException("API key is inactive or expired"));
            }
            return Mono.just(local);
        }

        // Check Redis if available
        if (redisTemplate != null) {
            return redisTemplate.opsForValue().get(redisPrefix + keyHash)
                    .flatMap(json -> {
                        try {
                            ApiKeyMetadata meta = objectMapper.readValue(json, ApiKeyMetadata.class);
                            if (!meta.isActive() || meta.isExpired()) {
                                return Mono.error(new IllegalArgumentException("API key is inactive or expired"));
                            }
                            // Cache in local store
                            inMemoryStore.put(keyHash, meta);
                            return Mono.just(meta);
                        } catch (Exception e) {
                            log.error("Failed to parse API key metadata from Redis: {}", e.getMessage());
                            return Mono.empty();
                        }
                    });
        }

        return Mono.empty();
    }

    /**
     * Hashes an API key with SHA-256 for secure storage and comparison.
     */
    public static String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
