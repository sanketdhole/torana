package com.phaselume.torana.pipeline.registry;

import com.phaselume.torana.core.config.ConnectorDefinition;
import com.phaselume.torana.core.exception.ConnectorException;
import com.phaselume.torana.test.support.MockBackendConnector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class BackendConnectorRegistryTest {

    private BackendConnectorRegistry registry;
    private MockBackendConnector mockConnector;

    @BeforeEach
    void setUp() {
        registry = new BackendConnectorRegistry();
        mockConnector = new MockBackendConnector("litellm");
        registry.registerConnector(mockConnector);
    }

    @Test
    void testResolveDirectConnector() {
        BackendConnectorRegistry.ResolvedConnector resolved = registry.resolve("litellm");
        assertNotNull(resolved);
        assertEquals("litellm", resolved.connector().type());
        assertEquals("litellm", resolved.config().getType());
    }

    @Test
    void testResolveConnectorDefinition() {
        ConnectorDefinition def = ConnectorDefinition.builder()
                .id("litellm-prod")
                .type("litellm")
                .endpoint("http://litellm:4000")
                .timeout(Duration.ofSeconds(45))
                .build();
        registry.registerDefinition(def);

        BackendConnectorRegistry.ResolvedConnector resolved = registry.resolve("litellm-prod");
        assertNotNull(resolved);
        assertEquals("litellm", resolved.connector().type());
        assertEquals("litellm-prod", resolved.config().getId());
        assertEquals("http://litellm:4000", resolved.config().getEndpoint());
        assertEquals(Duration.ofSeconds(45), resolved.config().getTimeout());
    }

    @Test
    void testResolveUnknownThrowsConnectorException() {
        assertThrows(ConnectorException.class, () -> registry.resolve("non-existent"));
        assertThrows(ConnectorException.class, () -> registry.resolve(null));
    }
}
