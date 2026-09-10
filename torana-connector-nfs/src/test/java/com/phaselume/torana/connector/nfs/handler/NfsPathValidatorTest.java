package com.phaselume.torana.connector.nfs.handler;

import com.phaselume.torana.core.exception.ConnectorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class NfsPathValidatorTest {

    private NfsPathValidator validator;

    @BeforeEach
    void setUp() {
        validator = new NfsPathValidator();
    }

    @Test
    void testValidPathResolvesCorrectly(@TempDir Path tempDir) {
        Path resolved = validator.validateAndResolve(tempDir.toString(), "docs/report.txt");
        assertNotNull(resolved);
        assertTrue(resolved.startsWith(tempDir));
    }

    @Test
    void testPathTraversalThrowsSecurityException(@TempDir Path tempDir) {
        assertThrows(ConnectorException.class, () ->
                validator.validateAndResolve(tempDir.toString(), "../../../etc/passwd")
        );
    }
}
