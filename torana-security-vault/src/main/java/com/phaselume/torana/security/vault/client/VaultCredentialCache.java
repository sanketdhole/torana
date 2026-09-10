package com.phaselume.torana.security.vault.client;

import com.phaselume.torana.core.model.BackendCredentials;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory short-lived credential cache for dynamic credentials within a request/tenant scope.
 */
public class VaultCredentialCache {

    private final ConcurrentHashMap<String, BackendCredentials> cache = new ConcurrentHashMap<>();

    public Mono<BackendCredentials> getOrCompute(String key, Mono<BackendCredentials> loader) {
        BackendCredentials cached = cache.get(key);
        if (cached != null && !cached.isExpired()) {
            return Mono.just(cached);
        }

        return loader.doOnNext(creds -> {
            if (creds != null) {
                cache.put(key, creds);
            }
        });
    }

    public void invalidate(String key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
    }
}
