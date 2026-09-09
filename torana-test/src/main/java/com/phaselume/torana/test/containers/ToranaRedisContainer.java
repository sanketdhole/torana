package com.phaselume.torana.test.containers;

import com.redis.testcontainers.RedisContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Pre-configured Redis Testcontainer for distributed rate limiting tests.
 */
public class ToranaRedisContainer {

    private static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse("redis:7.2-alpine");
    private final RedisContainer container;

    public ToranaRedisContainer() {
        this.container = new RedisContainer(DEFAULT_IMAGE);
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

    public String getRedisUrl() {
        return container.getRedisURI();
    }

    public String getHost() {
        return container.getHost();
    }

    public int getPort() {
        return container.getFirstMappedPort();
    }

    public RedisContainer getContainer() {
        return container;
    }
}
