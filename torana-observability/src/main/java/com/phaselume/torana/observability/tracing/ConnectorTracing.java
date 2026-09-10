package com.phaselume.torana.observability.tracing;

import com.phaselume.torana.core.model.AgentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Tracing wrapper for BackendConnector execution.
 */
public class ConnectorTracing {

    private static final Logger log = LoggerFactory.getLogger(ConnectorTracing.class);

    public static Mono<AgentResponse> traceConnector(String connectorId, Mono<AgentResponse> responseMono) {
        if (responseMono == null) return Mono.empty();

        long start = System.currentTimeMillis();
        return responseMono
                .doOnSuccess(resp -> {
                    long duration = System.currentTimeMillis() - start;
                    log.debug("Connector [{}] executed in {}ms with status {}", connectorId, duration, resp != null ? resp.getStatus() : "null");
                })
                .doOnError(err -> {
                    long duration = System.currentTimeMillis() - start;
                    log.warn("Connector [{}] failed after {}ms: {}", connectorId, duration, err.getMessage());
                });
    }
}
