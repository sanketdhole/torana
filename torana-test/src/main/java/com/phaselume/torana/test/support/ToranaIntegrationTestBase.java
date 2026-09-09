package com.phaselume.torana.test.support;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

/**
 * Base class for Torana integration test suites.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ToranaIntegrationTestBase {

    @BeforeAll
    public void setupTestContext() {
        // Subclasses can start containers or initialize mock servers here
    }

    @AfterAll
    public void tearDownTestContext() {
        // Subclasses can stop containers here
    }
}
