package com.phaselume.torana.connector.s3.client;

import com.phaselume.torana.connector.s3.model.S3ConnectorConfig;
import com.phaselume.torana.connector.s3.model.S3ObjectReference;
import com.phaselume.torana.core.exception.ConnectorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class S3OperationGuardTest {

    private S3OperationGuard guard;

    @BeforeEach
    void setUp() {
        guard = new S3OperationGuard();
    }

    @Test
    void testAllowedOperationPasses() {
        S3ConnectorConfig config = S3ConnectorConfig.builder()
                .allowedOperations(List.of("GET_OBJECT", "LIST_OBJECTS"))
                .pathPrefix("docs/")
                .build();

        S3ObjectReference ref = S3ObjectReference.builder()
                .operation("GET_OBJECT")
                .key("docs/architecture.pdf")
                .build();

        assertDoesNotThrow(() -> guard.validate(config, ref));
    }

    @Test
    void testDisallowedOperationThrowsSecurityException() {
        S3ConnectorConfig config = S3ConnectorConfig.builder()
                .allowedOperations(List.of("GET_OBJECT"))
                .build();

        S3ObjectReference ref = S3ObjectReference.builder()
                .operation("DELETE_OBJECT")
                .key("docs/report.pdf")
                .build();

        assertThrows(ConnectorException.class, () -> guard.validate(config, ref));
    }

    @Test
    void testPrefixViolationThrowsSecurityException() {
        S3ConnectorConfig config = S3ConnectorConfig.builder()
                .allowedOperations(List.of("GET_OBJECT"))
                .pathPrefix("public/")
                .build();

        S3ObjectReference ref = S3ObjectReference.builder()
                .operation("GET_OBJECT")
                .key("private/secret.key")
                .build();

        assertThrows(ConnectorException.class, () -> guard.validate(config, ref));
    }
}
