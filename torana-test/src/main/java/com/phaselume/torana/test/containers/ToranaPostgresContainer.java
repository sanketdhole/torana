package com.phaselume.torana.test.containers;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Pre-configured PostgreSQL Testcontainer for JDBC/R2DBC connector testing.
 */
public class ToranaPostgresContainer {

    private static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse("postgres:16-alpine");
    private final PostgreSQLContainer<?> container;

    public ToranaPostgresContainer() {
        this.container = new PostgreSQLContainer<>(DEFAULT_IMAGE)
                .withDatabaseName("torana_test_db")
                .withUsername("torana_user")
                .withPassword("torana_pass");
    }

    public void start() {
        if (!container.isRunning()) {
            container.start();
        }
    }

    public void stop() {
        if (container.isRunning()) {
            container.stop();
        }
    }

    public String getJdbcUrl() {
        return container.getJdbcUrl();
    }

    public String getR2dbcUrl() {
        return String.format("r2dbc:postgresql://%s:%d/%s",
                container.getHost(),
                container.getFirstMappedPort(),
                container.getDatabaseName());
    }

    public String getUsername() {
        return container.getUsername();
    }

    public String getPassword() {
        return container.getPassword();
    }

    public PostgreSQLContainer<?> getContainer() {
        return container;
    }
}
