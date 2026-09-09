package com.phaselume.torana.pipeline.registry;

import com.phaselume.torana.core.config.ConnectorDefinition;
import com.phaselume.torana.core.config.ToranaProperties;
import com.phaselume.torana.core.exception.ConnectorException;
import com.phaselume.torana.core.model.ConnectorConfig;
import com.phaselume.torana.core.spi.BackendConnector;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry indexing BackendConnector SPI implementations and configured connector instances.
 */
@Component
public class BackendConnectorRegistry {

    private final Map<String, BackendConnector> connectorBeansByType = new ConcurrentHashMap<>();
    private final Map<String, ConnectorDefinition> connectorDefinitionsById = new ConcurrentHashMap<>();

    public BackendConnectorRegistry() {
    }

    public BackendConnectorRegistry(List<BackendConnector> connectors, ToranaProperties properties) {
        if (connectors != null) {
            for (BackendConnector connector : connectors) {
                registerConnector(connector);
            }
        }
        if (properties != null && properties.getConnectors() != null && properties.getConnectors().getBackends() != null) {
            setDefinitions(properties.getConnectors().getBackends());
        }
    }

    public void registerConnector(BackendConnector connector) {
        if (connector != null && connector.type() != null) {
            connectorBeansByType.put(connector.type(), connector);
        }
    }

    public void registerDefinition(ConnectorDefinition definition) {
        if (definition != null && definition.getId() != null) {
            connectorDefinitionsById.put(definition.getId(), definition);
        }
    }

    public void setDefinitions(Collection<ConnectorDefinition> definitions) {
        connectorDefinitionsById.clear();
        if (definitions != null) {
            for (ConnectorDefinition def : definitions) {
                registerDefinition(def);
            }
        }
    }

    public Optional<BackendConnector> getConnectorByType(String type) {
        if (type == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(connectorBeansByType.get(type));
    }

    public Optional<ConnectorDefinition> getDefinition(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(connectorDefinitionsById.get(id));
    }

    /**
     * Resolves the connector and builds runtime ConnectorConfig for a backend reference id.
     */
    public ResolvedConnector resolve(String backendRef) {
        if (backendRef == null) {
            throw new ConnectorException("Backend reference cannot be null");
        }

        ConnectorDefinition def = connectorDefinitionsById.get(backendRef);
        String type = def != null ? def.getType() : backendRef;

        BackendConnector connector = connectorBeansByType.get(type);
        if (connector == null) {
            throw new ConnectorException("No BackendConnector found for type/ref: " + backendRef);
        }

        ConnectorConfig config = def != null
                ? ConnectorConfig.builder()
                .id(def.getId())
                .type(def.getType())
                .endpoint(def.getEndpoint())
                .resilienceProfileRef(def.getResilienceProfileRef())
                .timeout(def.getTimeout())
                .properties(def.getProperties())
                .build()
                : ConnectorConfig.builder()
                .id(backendRef)
                .type(type)
                .build();

        return new ResolvedConnector(connector, config);
    }

    public Map<String, BackendConnector> getAllConnectors() {
        return Collections.unmodifiableMap(connectorBeansByType);
    }

    public Map<String, ConnectorDefinition> getAllDefinitions() {
        return Collections.unmodifiableMap(connectorDefinitionsById);
    }

    public record ResolvedConnector(BackendConnector connector, ConnectorConfig config) {
    }
}
