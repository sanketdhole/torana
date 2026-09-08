package com.phaselume.torana.core.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Definition of a backend connector instance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectorDefinition {

    /**
     * Unique connector identifier (e.g. "litellm-prod", "enterprise-postgres-jdbc").
     */
    private String id;

    /**
     * Connector type matching a BackendConnector SPI (e.g. "litellm", "http", "jdbc", "nosql", "s3", "nfs").
     */
    private String type;

    /**
     * Backend endpoint or connection URI.
     */
    private String endpoint;

    /**
     * Resilience profile reference.
     */
    private String resilienceProfileRef;

    /**
     * Connector timeout.
     */
    @Builder.Default
    private Duration timeout = Duration.ofSeconds(30);

    /**
     * Connector-specific properties.
     */
    @Builder.Default
    private Map<String, Object> properties = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, T defaultValue) {
        if (properties == null || !properties.containsKey(key)) {
            return defaultValue;
        }
        return (T) properties.get(key);
    }
}
