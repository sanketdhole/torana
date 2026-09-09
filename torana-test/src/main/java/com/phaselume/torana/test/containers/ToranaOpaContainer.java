package com.phaselume.torana.test.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Pre-configured Open Policy Agent (OPA) Testcontainer for authorization tests.
 */
public class ToranaOpaContainer {

    private static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse("openpolicyagent/opa:latest-static");
    private final GenericContainer<?> container;

    public ToranaOpaContainer() {
        this.container = new GenericContainer<>(DEFAULT_IMAGE)
                .withExposedPorts(8181)
                .withCommand("run", "--server", "--log-level", "debug")
                .waitingFor(Wait.forHttp("/v1/data").forStatusCode(200));
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

    public String getOpaUrl() {
        return String.format("http://%s:%d/v1/data/torana/authz", container.getHost(), container.getFirstMappedPort());
    }

    public String getBaseUrl() {
        return String.format("http://%s:%d", container.getHost(), container.getFirstMappedPort());
    }

    public GenericContainer<?> getContainer() {
        return container;
    }
}
