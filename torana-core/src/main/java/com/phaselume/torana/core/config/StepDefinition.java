package com.phaselume.torana.core.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Definition of a single step within a pipeline.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepDefinition {

    /**
     * Step type matching a PipelineStep SPI implementation (e.g. "rag-retrieval", "llm-call", "transform").
     */
    private String type;

    /**
     * Arbitrary parameters passed to the step.
     */
    @Builder.Default
    private Map<String, Object> params = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T getParam(String key, T defaultValue) {
        if (params == null || !params.containsKey(key)) {
            return defaultValue;
        }
        return (T) params.get(key);
    }
}
