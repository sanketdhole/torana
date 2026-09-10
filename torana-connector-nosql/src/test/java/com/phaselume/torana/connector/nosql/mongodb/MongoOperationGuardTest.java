package com.phaselume.torana.connector.nosql.mongodb;

import com.phaselume.torana.core.exception.ConnectorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MongoOperationGuardTest {

    private MongoOperationGuard guard;

    @BeforeEach
    void setUp() {
        guard = new MongoOperationGuard();
    }

    @Test
    void testAllowedOperationPasses() {
        assertDoesNotThrow(() ->
                guard.validate("find", "users", List.of("find", "count"), List.of("users", "orders"))
        );
    }

    @Test
    void testDisallowedOperationThrows() {
        assertThrows(ConnectorException.class, () ->
                guard.validate("drop", "users", List.of("find", "count"), List.of("users"))
        );
    }

    @Test
    void testDisallowedCollectionThrows() {
        assertThrows(ConnectorException.class, () ->
                guard.validate("find", "secrets", List.of("find"), List.of("users"))
        );
    }
}
