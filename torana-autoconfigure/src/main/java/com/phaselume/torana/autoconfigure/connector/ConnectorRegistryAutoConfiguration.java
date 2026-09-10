package com.phaselume.torana.autoconfigure.connector;

import com.phaselume.torana.core.spi.BackendConnector;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Auto-configuration collecting all BackendConnector beans into a lookup map.
 */
@AutoConfiguration
public class ConnectorRegistryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "connectorRegistryMap")
    public Map<String, BackendConnector> connectorRegistryMap(List<BackendConnector> connectors) {
        Map<String, BackendConnector> registry = new ConcurrentHashMap<>();
        if (connectors != null) {
            connectors.forEach(c -> registry.put(c.type(), c));
        }
        return registry;
    }
}
