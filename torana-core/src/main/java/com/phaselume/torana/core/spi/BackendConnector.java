package com.phaselume.torana.core.spi;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.model.ConnectorConfig;
import reactor.core.publisher.Flux;

/**
 * SPI for backend enterprise connectors (e.g. LiteLLM, HTTP, PostgreSQL/R2DBC, MongoDB, S3, NFS).
 */
public interface BackendConnector {

    /**
     * Unique connector type matching YAML definitions (e.g. "litellm", "http", "jdbc", "nosql", "s3", "nfs").
     */
    String type();

    /**
     * Check if this connector implementation supports the provided configuration.
     */
    boolean supports(ConnectorConfig config);

    /**
     * Execute the backend call reactively and return a streaming Flux of response chunks.
     */
    Flux<AgentResponse.Chunk> execute(AgentContext context, ConnectorConfig config);
}
