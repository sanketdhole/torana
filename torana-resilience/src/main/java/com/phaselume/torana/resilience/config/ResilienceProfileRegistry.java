package com.phaselume.torana.resilience.config;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry storing named resilience profile definitions.
 */
public class ResilienceProfileRegistry {

    private final Map<String, ResilienceProfileDefinition> profiles = new ConcurrentHashMap<>();

    public ResilienceProfileRegistry() {
        // Register default fallback profile
        profiles.put("default", new ResilienceProfileDefinition());
    }

    public ResilienceProfileRegistry(Map<String, ResilienceProfileDefinition> initialProfiles) {
        if (initialProfiles != null) {
            this.profiles.putAll(initialProfiles);
        }
        this.profiles.putIfAbsent("default", new ResilienceProfileDefinition());
    }

    public void register(String name, ResilienceProfileDefinition definition) {
        if (name != null && definition != null) {
            profiles.put(name, definition);
        }
    }

    public ResilienceProfileDefinition get(String name) {
        if (name == null) {
            return profiles.get("default");
        }
        return profiles.getOrDefault(name, profiles.get("default"));
    }

    public boolean contains(String name) {
        return name != null && profiles.containsKey(name);
    }

    public Map<String, ResilienceProfileDefinition> getAll() {
        return Collections.unmodifiableMap(profiles);
    }
}
