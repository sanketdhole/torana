package com.phaselume.torana.pipeline.executor;

import com.phaselume.torana.core.config.PipelineDefinition;
import com.phaselume.torana.core.exception.PipelineException;
import com.phaselume.torana.core.exception.PipelineException.ErrorCode;
import com.phaselume.torana.pipeline.model.PipelineConstants;
import com.phaselume.torana.pipeline.registry.PipelineRegistry;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves on-error fallback pipelines with circular reference detection and recursion depth guarding.
 */
@Component
public class FallbackPipelineResolver {

    private final PipelineRegistry pipelineRegistry;

    public FallbackPipelineResolver(PipelineRegistry pipelineRegistry) {
        this.pipelineRegistry = pipelineRegistry;
    }

    /**
     * Resolves the fallback pipeline for a failed pipeline, tracking visited pipelines.
     */
    public Optional<PipelineDefinition> resolveFallback(
            PipelineDefinition failedPipeline,
            Set<String> visitedPipelines,
            int currentDepth) {

        if (failedPipeline == null || failedPipeline.getOnError() == null || failedPipeline.getOnError().isBlank()) {
            return Optional.empty();
        }

        String fallbackName = failedPipeline.getOnError();

        if (currentDepth >= PipelineConstants.MAX_FALLBACK_DEPTH) {
            throw new PipelineException(
                    String.format("Exceeded maximum fallback pipeline recursion depth of %d at '%s'",
                            PipelineConstants.MAX_FALLBACK_DEPTH, fallbackName),
                    ErrorCode.EXECUTION_FAILED
            );
        }

        Set<String> visited = visitedPipelines != null ? new HashSet<>(visitedPipelines) : new HashSet<>();
        if (failedPipeline.getName() != null) {
            visited.add(failedPipeline.getName());
        }

        if (visited.contains(fallbackName)) {
            throw new PipelineException(
                    String.format("Circular fallback pipeline detected: '%s' is already in visited chain %s",
                            fallbackName, visited),
                    ErrorCode.EXECUTION_FAILED
            );
        }

        Optional<PipelineDefinition> fallbackPipeline = pipelineRegistry.getPipeline(fallbackName);
        if (fallbackPipeline.isEmpty()) {
            throw new PipelineException(
                    String.format("Configured fallback pipeline '%s' for pipeline '%s' was not found in registry",
                            fallbackName, failedPipeline.getName()),
                    ErrorCode.NOT_FOUND
            );
        }

        return fallbackPipeline;
    }
}
