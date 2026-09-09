package com.phaselume.torana.test.containers;

import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Pre-configured LocalStack Testcontainer for AWS S3 and STS connector testing.
 */
public class ToranaLocalStackContainer {

    private static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse("localstack/localstack:3.0");
    private final LocalStackContainer container;

    public ToranaLocalStackContainer() {
        this.container = new LocalStackContainer(DEFAULT_IMAGE)
                .withServices(LocalStackContainer.Service.S3, LocalStackContainer.Service.STS);
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

    public String getEndpointOverride() {
        return container.getEndpointOverride(LocalStackContainer.Service.S3).toString();
    }

    public String getAccessKey() {
        return container.getAccessKey();
    }

    public String getSecretKey() {
        return container.getSecretKey();
    }

    public String getRegion() {
        return container.getRegion();
    }

    public LocalStackContainer getContainer() {
        return container;
    }
}
