package com.phaselume.torana.core.spi;

import com.phaselume.torana.core.model.AuditEvent;
import reactor.core.publisher.Mono;

/**
 * SPI for exporting audit events (e.g. Logging, Kafka, OpenSearch, SIEM).
 */
public interface AuditSink {

    /**
     * Unique sink type matching YAML definitions (e.g. "log", "kafka", "opensearch").
     */
    String type();

    /**
     * Publish the structured audit event asynchronously.
     */
    Mono<Void> publish(AuditEvent event);
}
