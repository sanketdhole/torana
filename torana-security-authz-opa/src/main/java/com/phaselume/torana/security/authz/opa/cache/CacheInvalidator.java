package com.phaselume.torana.security.authz.opa.cache;

import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthzProperties.OpaCacheProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

/**
 * Provides administrative cache invalidation for OPA policy decisions.
 */
public class CacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidator.class);

    private final OpaDecisionCache decisionCache;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final OpaCacheProperties cacheProperties;

    public CacheInvalidator(OpaDecisionCache decisionCache,
                            ReactiveStringRedisTemplate redisTemplate,
                            OpaCacheProperties cacheProperties) {
        this.decisionCache = decisionCache;
        this.redisTemplate = redisTemplate;
        this.cacheProperties = cacheProperties != null ? cacheProperties : new OpaCacheProperties();
    }

    /**
     * Flushes all cached authorization decisions.
     */
    public Mono<Void> invalidateAll() {
        if (decisionCache != null) {
            decisionCache.clearLocal();
        }

        if (redisTemplate != null) {
            String pattern = cacheProperties.getRedisPrefix() + "*";
            return redisTemplate.keys(pattern)
                    .flatMap(redisTemplate::delete)
                    .then()
                    .doOnSuccess(v -> log.info("Invalidated all OPA cached decisions"));
        }

        return Mono.empty();
    }
}
