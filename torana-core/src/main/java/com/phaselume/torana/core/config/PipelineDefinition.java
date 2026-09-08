package com.phaselume.torana.core.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Definition of a named execution pipeline composed of sequential Steps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineDefinition {

    /**
     * Unique pipeline name (e.g. "rag-pipeline", "llm-proxy-pipeline").
     */
    private String name;

    /**
     * Pipeline execution timeout.
     */
    @Builder.Default
    private Duration timeout = Duration.ofSeconds(60);

    /**
     * Fallback pipeline name to execute on error.
     */
    private String onError;

    /**
     * Ordered list of step definitions.
     */
    @Builder.Default
    private List<StepDefinition> steps = new ArrayList<>();
}
