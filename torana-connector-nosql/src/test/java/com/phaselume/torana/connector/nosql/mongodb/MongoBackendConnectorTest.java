package com.phaselume.torana.connector.nosql.mongodb;

import com.phaselume.torana.core.exception.ConnectorException;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.ConnectorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MongoBackendConnectorTest {

    private ReactiveMongoClientFactory clientFactory;
    private MongoBackendConnector connector;

    @BeforeEach
    void setUp() {
        clientFactory = Mockito.mock(ReactiveMongoClientFactory.class);
        connector = new MongoBackendConnector(
                clientFactory,
                new MongoQueryBuilder(),
                new MongoOperationGuard(),
                new MongoResultSerializer(),
                null
        );
    }

    @Test
    void testSupports() {
        assertEquals("mongodb", connector.type());
        assertTrue(connector.supports(ConnectorConfig.builder().type("mongodb").build()));
        assertTrue(connector.supports(ConnectorConfig.builder().type("mongo").build()));
        assertTrue(connector.supports(ConnectorConfig.builder().type("nosql").build()));
        assertFalse(connector.supports(ConnectorConfig.builder().type("http").build()));
    }

    @Test
    void testDisallowedOperationFailsEarly() {
        AgentContext context = AgentContext.builder()
                .attributes(Map.of("operation", "drop", "collection", "users"))
                .build();

        ConnectorConfig config = ConnectorConfig.builder()
                .id("mongo-1")
                .type("mongodb")
                .properties(Map.of("allowedOperations", List.of("find")))
                .build();

        assertThrows(ConnectorException.class, () -> connector.execute(context, config));
    }
}
