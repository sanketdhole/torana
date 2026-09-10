package com.phaselume.torana.connector.jdbc;

import com.phaselume.torana.connector.jdbc.query.ResultSetSerializer;
import com.phaselume.torana.connector.jdbc.query.SqlOperationGuard;
import com.phaselume.torana.connector.jdbc.query.SqlQueryExtractor;
import com.phaselume.torana.connector.jdbc.r2dbc.R2dbcConnectionFactory;
import com.phaselume.torana.connector.jdbc.security.SchemaAllowList;
import com.phaselume.torana.core.exception.ConnectorException;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.ConnectorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class R2dbcBackendConnectorTest {

    private R2dbcConnectionFactory connectionFactory;
    private R2dbcBackendConnector connector;

    @BeforeEach
    void setUp() {
        connectionFactory = Mockito.mock(R2dbcConnectionFactory.class);
        connector = new R2dbcBackendConnector(
                connectionFactory,
                new SqlQueryExtractor(),
                new SqlOperationGuard(),
                new SchemaAllowList(),
                new ResultSetSerializer(),
                null
        );
    }

    @Test
    void testSupports() {
        assertEquals("r2dbc", connector.type());
        assertTrue(connector.supports(ConnectorConfig.builder().type("r2dbc").build()));
        assertTrue(connector.supports(ConnectorConfig.builder().type("jdbc").build()));
        assertTrue(connector.supports(ConnectorConfig.builder().type("sql").build()));
        assertFalse(connector.supports(ConnectorConfig.builder().type("http").build()));
    }

    @Test
    void testDisallowedOperationFailsEarly() {
        AgentContext context = AgentContext.builder()
                .attributes(Map.of("sql", "DELETE FROM accounts WHERE id = 1"))
                .build();

        ConnectorConfig config = ConnectorConfig.builder()
                .id("db-1")
                .type("r2dbc")
                .properties(Map.of("allowedOperations", java.util.List.of("SELECT")))
                .build();

        assertThrows(ConnectorException.class, () -> connector.execute(context, config));
    }
}
