package com.phaselume.torana.pipeline.registry;

import com.phaselume.torana.core.config.PipelineDefinition;
import com.phaselume.torana.core.config.StepDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PipelineRegistryTest {

    private PipelineRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new PipelineRegistry();
    }

    @Test
    void testRegisterAndRetrievePipeline() {
        PipelineDefinition pipeline = PipelineDefinition.builder()
                .name("chat-pipeline")
                .timeout(Duration.ofSeconds(45))
                .steps(List.of(
                        StepDefinition.builder().type("request-transform").build(),
                        StepDefinition.builder().type("llm-call").build()
                ))
                .build();

        registry.register(pipeline);

        assertTrue(registry.hasPipeline("chat-pipeline"));
        Optional<PipelineDefinition> retrieved = registry.getPipeline("chat-pipeline");
        assertTrue(retrieved.isPresent());
        assertEquals("chat-pipeline", retrieved.get().getName());
        assertEquals(Duration.ofSeconds(45), retrieved.get().getTimeout());
        assertEquals(2, retrieved.get().getSteps().size());
    }

    @Test
    void testSetPipelinesAtomicReplacement() {
        PipelineDefinition p1 = PipelineDefinition.builder().name("p1").build();
        PipelineDefinition p2 = PipelineDefinition.builder().name("p2").build();

        registry.setPipelines(List.of(p1, p2));
        assertEquals(2, registry.size());
        assertTrue(registry.hasPipeline("p1"));
        assertTrue(registry.hasPipeline("p2"));

        // Replace
        PipelineDefinition p3 = PipelineDefinition.builder().name("p3").build();
        registry.setPipelines(List.of(p3));
        assertEquals(1, registry.size());
        assertFalse(registry.hasPipeline("p1"));
        assertTrue(registry.hasPipeline("p3"));
    }

    @Test
    void testGetNonExistentPipelineReturnsEmpty() {
        assertFalse(registry.hasPipeline("non-existent"));
        assertTrue(registry.getPipeline("non-existent").isEmpty());
        assertTrue(registry.getPipeline(null).isEmpty());
    }
}
