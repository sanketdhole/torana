package com.phaselume.torana.connector.nfs.handler;

import com.phaselume.torana.core.exception.ConnectorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NfsOperationGuardTest {

    private NfsOperationGuard guard;

    @BeforeEach
    void setUp() {
        guard = new NfsOperationGuard();
    }

    @Test
    void testAllowedOperationAndExtensionPasses() {
        Path path = Paths.get("/mnt/docs/whitepaper.pdf");
        assertDoesNotThrow(() ->
                guard.validate("READ_FILE", path, List.of("READ_FILE"), List.of(".pdf", ".txt"))
        );
    }

    @Test
    void testDisallowedOperationThrowsException() {
        Path path = Paths.get("/mnt/docs/file.txt");
        assertThrows(ConnectorException.class, () ->
                guard.validate("DELETE_FILE", path, List.of("READ_FILE"), List.of(".txt"))
        );
    }

    @Test
    void testDisallowedExtensionThrowsException() {
        Path path = Paths.get("/mnt/docs/script.sh");
        assertThrows(ConnectorException.class, () ->
                guard.validate("READ_FILE", path, List.of("READ_FILE"), List.of(".pdf", ".txt"))
        );
    }
}
