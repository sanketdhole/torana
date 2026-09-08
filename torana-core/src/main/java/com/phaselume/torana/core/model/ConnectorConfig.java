package com.phaselume.torana.core.model;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

/**
 * Snapshot of runtime configuration passed to a BackendConnector.
 */
@Value
@Builder(toBuilder = true)
public class ConnectorConfig {

    String id;
    String type; // "litellm", "http", "jdbc", "nosql", "s3", "nfs"
    String endpoint;
    String resilienceProfileRef;
    
    @Builder.Default
    Duration timeout = Duration.ofSeconds(30);

    @Builder.Default
    Map<String, Object> properties = Collections.emptyMap();

    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, T defaultValue) {
        if (properties == null || !properties.containsKey(key)) {
            return defaultValue;
        }
        return (T) properties.get(key);
    }
}
