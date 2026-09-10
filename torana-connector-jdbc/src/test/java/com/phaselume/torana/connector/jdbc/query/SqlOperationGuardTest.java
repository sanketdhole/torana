package com.phaselume.torana.connector.jdbc.query;

import com.phaselume.torana.core.exception.ConnectorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlOperationGuardTest {

    private SqlOperationGuard guard;

    @BeforeEach
    void setUp() {
        guard = new SqlOperationGuard();
    }

    @Test
    void testValidSelectPasses() {
        assertDoesNotThrow(() -> guard.validate("SELECT id, name FROM users WHERE tenant_id = 't1'", List.of("SELECT")));
    }

    @Test
    void testDisallowedInsertThrowsException() {
        assertThrows(ConnectorException.class, () ->
                guard.validate("INSERT INTO audit_log (event) VALUES ('test')", List.of("SELECT"))
        );
    }

    @Test
    void testDestructiveKeywordThrowsException() {
        assertThrows(ConnectorException.class, () ->
                guard.validate("DROP TABLE users", List.of("SELECT", "DROP"))
        );
    }
}
