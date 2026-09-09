package com.phaselume.torana.test.containers;

import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Pre-configured Apache Kafka (KRaft mode) Testcontainer for observability audit sink tests.
 */
public class ToranaKafkaContainer {

    private static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse("apache/kafka:3.7.0");
    private final KafkaContainer container;

    public ToranaKafkaContainer() {
        this.container = new KafkaContainer(DEFAULT_IMAGE);
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

    public String getBootstrapServers() {
        return container.getBootstrapServers();
    }

    public KafkaContainer getContainer() {
        return container;
    }
}
