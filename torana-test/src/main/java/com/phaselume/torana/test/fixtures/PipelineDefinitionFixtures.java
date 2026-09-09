package com.phaselume.torana.test.fixtures;

import com.phaselume.torana.core.config.PipelineDefinition;
import com.phaselume.torana.core.config.StepDefinition;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Pre-configured PipelineDefinition test instances.
 */
public final class PipelineDefinitionFixtures {

    private PipelineDefinitionFixtures() {}

    public static PipelineDefinition ragPipeline() {
        return PipelineDefinition.builder()
                .name("rag-pipeline")
                .timeout(Duration.ofSeconds(45))
                .steps(List.of(
                        StepDefinition.builder().type("rag-retrieval").params(Map.of("topK", 5)).build(),
                        StepDefinition.builder().type("prompt-augment").params(Map.of("template", "rag-v1")).build(),
                        StepDefinition.builder().type("llm-call").params(Map.of("model", "gpt-4o")).build()
                ))
                .build();
    }

    public static PipelineDefinition llmProxyPipeline() {
        return PipelineDefinition.builder()
                .name("llm-proxy-pipeline")
                .timeout(Duration.ofSeconds(30))
                .steps(List.of(
                        StepDefinition.builder().type("llm-call").params(Map.of("model", "gpt-4o")).build()
                ))
                .build();
    }

    public static PipelineDefinition twoStepPipeline(String stepType1, String stepType2) {
        return PipelineDefinition.builder()
                .name("test-pipeline-" + stepType1 + "-" + stepType2)
                .timeout(Duration.ofSeconds(10))
                .steps(List.of(
                        StepDefinition.builder().type(stepType1).build(),
                        StepDefinition.builder().type(stepType2).build()
                ))
                .build();
    }
}
