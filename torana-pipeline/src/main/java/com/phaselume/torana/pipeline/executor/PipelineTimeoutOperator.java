package com.phaselume.torana.pipeline.executor;

import com.phaselume.torana.core.exception.PipelineException;
import com.phaselume.torana.core.exception.PipelineException.ErrorCode;
import com.phaselume.torana.core.model.AgentContext;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Applies non-blocking timeout operators to pipeline and step executions.
 */
public final class PipelineTimeoutOperator {

    private PipelineTimeoutOperator() {
    }

    /**
     * Applies a timeout to a reactive pipeline execution.
     */
    public static Mono<AgentContext> applyTimeout(Mono<AgentContext> source, Duration timeout, String pipelineName) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return source;
        }

        return source.timeout(timeout, Mono.defer(() ->
                Mono.error(new PipelineException(
                        String.format("Pipeline '%s' execution timed out after %s", pipelineName, timeout),
                        ErrorCode.TIMEOUT
                ))
        ));
    }

    /**
     * Applies a timeout to a single step execution.
     */
    public static Mono<AgentContext> applyStepTimeout(Mono<AgentContext> source, Duration timeout, String stepType) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return source;
        }

        return source.timeout(timeout, Mono.defer(() ->
                Mono.error(new PipelineException(
                        String.format("Step '%s' execution timed out after %s", stepType, timeout),
                        ErrorCode.TIMEOUT
                ))
        ));
    }
}
