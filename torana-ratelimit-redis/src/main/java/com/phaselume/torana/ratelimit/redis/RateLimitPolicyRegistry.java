package com.phaselume.torana.ratelimit.redis;

import com.phaselume.torana.core.model.RateLimitPolicy;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry holding named rate limit policies.
 */
public class RateLimitPolicyRegistry {

    private final Map<String, RateLimitPolicy> policies = new ConcurrentHashMap<>();

    public RateLimitPolicyRegistry() {
        policies.put("default", RateLimitPolicy.defaultPolicy());
    }

    public RateLimitPolicyRegistry(Map<String, RateLimitPolicy> initialPolicies) {
        if (initialPolicies != null) {
            policies.putAll(initialPolicies);
        }
        policies.putIfAbsent("default", RateLimitPolicy.defaultPolicy());
    }

    public void register(String name, RateLimitPolicy policy) {
        if (name != null && policy != null) {
            policies.put(name, policy);
        }
    }

    public RateLimitPolicy get(String name) {
        if (name == null) {
            return policies.get("default");
        }
        return policies.getOrDefault(name, policies.get("default"));
    }

    public boolean contains(String name) {
        return name != null && policies.containsKey(name);
    }

    public Map<String, RateLimitPolicy> getAll() {
        return Collections.unmodifiableMap(policies);
    }
}
