package com.phaselume.torana.pipeline.executor;

import com.phaselume.torana.core.config.PipelineDefinition;
import com.phaselume.torana.core.config.StepDefinition;
import com.phaselume.torana.core.exception.PipelineException;
import com.phaselume.torana.core.exception.PipelineException.ErrorCode;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.spi.PipelineStep;
import com.phaselume.torana.pipeline.model.PipelineConstants;
import com.phaselume.torana.pipeline.model.PipelineResult;
import com.phaselume.torana.pipeline.registry.PipelineRegistry;
import com.phaselume.torana.pipeline.registry.PipelineStepRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Main reactive orchestrator for pipeline execution.
 * Executes steps sequentially, applies timeouts, handles short-circuiting, and
 * triggers fallbacks.
 */
@Component
public class PipelineExecutor {

    private static final Logger log = LoggerFactory.getLogger(PipelineExecutor.class);

    private final PipelineRegistry pipelineRegistry;
    private final PipelineStepRegistry stepRegistry;
    private final FallbackPipelineResolver fallbackResolver;

    public PipelineExecutor(
            PipelineRegistry pipelineRegistry,
            PipelineStepRegistry stepRegistry,
            FallbackPipelineResolver fallbackResolver) {
        this.pipelineRegistry = pipelineRegistry;
        this.stepRegistry = stepRegistry;
        this.fallbackResolver = fallbackResolver;
    }

    /**
     * Executes a pipeline by name.
     */
    public Mono<PipelineResult> execute(String pipelineName, AgentContext context) {
        if (pipelineName == null || pipelineName.isBlank()) {
            return Mono.error(new PipelineException("Pipeline name cannot be null or empty", ErrorCode.NOT_FOUND));
        }

        Optional<PipelineDefinition> pipelineOpt = pipelineRegistry.getPipeline(pipelineName);
        if (pipelineOpt.isEmpty()) {
            return Mono.error(new PipelineException(
                    "Pipeline not found: " + pipelineName,
                    ErrorCode.NOT_FOUND));
        }

        return execute(pipelineOpt.get(), context);
    }

    /**
     * Executes a given PipelineDefinition.
     */
    public Mono<PipelineResult> execute(PipelineDefinition pipeline, AgentContext initialContext) {
        if (pipeline == null) {
            return Mono.error(new PipelineException("PipelineDefinition cannot be null", ErrorCode.NOT_FOUND));
        }
        if (initialContext == null) {
            return Mono.error(new IllegalArgumentException("AgentContext cannot be null"));
        }

        Instant startTime = Instant.now();
        String pipelineName = pipeline.getName() != null ? pipeline.getName() : "anonymous-pipeline";

        return executeChain(pipeline, initialContext, new HashSet<>(), 0)
                .map(finalContext -> {
                    Duration duration = Duration.between(startTime, Instant.now());
                    AgentResponse response = finalContext.getAttribute(PipelineConstants.ATTR_RESPONSE);
                    if (response == null) {
                        response = AgentResponse.builder().build();
                    }
                    return PipelineResult.success(pipelineName, finalContext, response, duration);
                })
                .onErrorResume(error -> {
                    Duration duration = Duration.between(startTime, Instant.now());
                    log.error("Pipeline '{}' failed after {} ms: {}", pipelineName, duration.toMillis(),
                            error.getMessage());
                    return Mono.just(PipelineResult.failure(pipelineName, initialContext, error, duration));
                });
    }

    /**
     * Executes a pipeline step chain with recursion and fallback tracking.
     */
    public Mono<AgentContext> executeChain(
            PipelineDefinition pipeline,
            AgentContext context,
            Set<String> visitedPipelines,
            int depth) {

        String pipelineName = pipeline.getName() != null ? pipeline.getName() : "anonymous-pipeline";
        List<StepDefinition> steps = pipeline.getSteps();

        Mono<AgentContext> chain = Mono.just(context);

        if (steps != null && !steps.isEmpty()) {
            for (int i = 0; i < steps.size(); i++) {
                final int stepIndex = i;
                final StepDefinition stepDef = steps.get(i);

                chain = chain.flatMap(currentCtx -> {
                    // Check if short-circuited by an earlier step
                    Boolean shortCircuit = currentCtx.getAttribute(PipelineConstants.ATTR_SHORT_CIRCUIT);
                    if (Boolean.TRUE.equals(shortCircuit)) {
                        log.debug("Pipeline '{}' short-circuited; skipping step {} ('{}')",
                                pipelineName, stepIndex, stepDef.getType());
                        return Mono.just(currentCtx);
                    }

                    PipelineStep stepBean = stepRegistry.getRequiredStep(stepDef.getType());

                    AgentContext enrichedCtx = currentCtx
                            .withAttribute(PipelineConstants.ATTR_CURRENT_STEP, stepDef)
                            .withAttribute(PipelineConstants.ATTR_STEP_INDEX, stepIndex);

                    log.debug("Executing step {} ('{}') in pipeline '{}'", stepIndex, stepDef.getType(), pipelineName);
                    return stepBean.execute(enrichedCtx);
                });
            }
        }

        // Apply timeout to this pipeline execution
        Mono<AgentContext> timedChain = PipelineTimeoutOperator.applyTimeout(chain, pipeline.getTimeout(),
                pipelineName);

        // Fallback handling on error
        return timedChain.onErrorResume(error -> {
            log.warn("Error in pipeline '{}': {}. Attempting fallback...", pipelineName, error.getMessage());

            Optional<PipelineDefinition> fallbackOpt;
            try {
                fallbackOpt = fallbackResolver.resolveFallback(pipeline, visitedPipelines, depth);
            } catch (Exception resolveEx) {
                return Mono.error(resolveEx);
            }

            if (fallbackOpt.isPresent()) {
                PipelineDefinition fallbackPipeline = fallbackOpt.get();
                Set<String> nextVisited = new HashSet<>(visitedPipelines);
                nextVisited.add(pipelineName);

                log.info("Executing fallback pipeline '{}' for failed pipeline '{}'",
                        fallbackPipeline.getName(), pipelineName);

                AgentContext fallbackContext = context
                        .withAttribute(PipelineConstants.ATTR_FALLBACK_DEPTH, depth + 1)
                        .withAttribute("torana.pipeline.fallback_from", pipelineName)
                        .withAttribute("torana.pipeline.original_error", error.getMessage());

                return executeChain(fallbackPipeline, fallbackContext, nextVisited, depth + 1);
            }

            return Mono.error(error);
        });
    }

    public PipelineRegistry getPipelineRegistry() {
        return pipelineRegistry;
    }

    public PipelineStepRegistry getStepRegistry() {
        return stepRegistry;
    }

    public FallbackPipelineResolver getFallbackResolver() {
        return fallbackResolver;
    }
}
