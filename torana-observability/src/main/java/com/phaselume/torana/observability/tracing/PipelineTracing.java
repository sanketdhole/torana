package com.phaselume.torana.observability.tracing;

import com.phaselume.torana.core.model.AgentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Tracing wrapper for pipeline step execution in Torana gateway.
 */
public class PipelineTracing {

    private static final Logger log = LoggerFactory.getLogger(PipelineTracing.class);

    public static Mono<AgentContext> traceStep(String stepType, Mono<AgentContext> stepMono) {
        if (stepMono == null) return Mono.empty();

        long start = System.currentTimeMillis();
        return stepMono
                .doOnSuccess(ctx -> {
                    long duration = System.currentTimeMillis() - start;
                    log.debug("Pipeline step [{}] completed in {}ms", stepType, duration);
                })
                .doOnError(err -> {
                    long duration = System.currentTimeMillis() - start;
                    log.warn("Pipeline step [{}] failed after {}ms: {}", stepType, duration, err.getMessage());
                });
    }
}
