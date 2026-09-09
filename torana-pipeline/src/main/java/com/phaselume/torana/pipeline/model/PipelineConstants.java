package com.phaselume.torana.pipeline.model;

/**
 * Standard AgentContext attribute keys and constants for the pipeline execution engine.
 */
public final class PipelineConstants {

    private PipelineConstants() {
    }

    /**
     * Context attribute holding the resolved or synthesized AgentResponse.
     */
    public static final String ATTR_RESPONSE = "torana.pipeline.response";

    /**
     * Context attribute holding the streaming response Flux of chunks.
     */
    public static final String ATTR_RESPONSE_STREAM = "torana.pipeline.response.stream";

    /**
     * Context attribute holding the current step definition.
     */
    public static final String ATTR_CURRENT_STEP = "torana.pipeline.current_step";

    /**
     * Context attribute holding the current step index (0-based integer).
     */
    public static final String ATTR_STEP_INDEX = "torana.pipeline.step_index";

    /**
     * Context attribute tracking recursion depth during fallback execution.
     */
    public static final String ATTR_FALLBACK_DEPTH = "torana.pipeline.fallback_depth";

    /**
     * Context attribute indicating that the pipeline should short-circuit and skip downstream steps.
     */
    public static final String ATTR_SHORT_CIRCUIT = "torana.pipeline.short_circuit";

    /**
     * Maximum allowed fallback chain depth before aborting to prevent infinite loops.
     */
    public static final int MAX_FALLBACK_DEPTH = 5;
}
