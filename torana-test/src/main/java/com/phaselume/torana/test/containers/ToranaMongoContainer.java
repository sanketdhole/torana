package com.phaselume.torana.test.containers;

import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Pre-configured MongoDB Testcontainer for NoSQL connector testing.
 */
public class ToranaMongoContainer {

    private static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse("mongo:7.0");
    private final MongoDBContainer container;

    public ToranaMongoContainer() {
        this.container = new MongoDBContainer(DEFAULT_IMAGE);
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

    public String getConnectionUrl() {
        return container.getConnectionString();
    }

    public MongoDBContainer getContainer() {
        return container;
    }
}
