package com.phaselume.torana.pipeline.registry;

import com.phaselume.torana.core.exception.ConfigurationException;
import com.phaselume.torana.core.spi.PipelineStep;
import com.phaselume.torana.test.support.MockPipelineStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PipelineStepRegistryTest {

    private PipelineStepRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new PipelineStepRegistry();
    }

    @Test
    void testRegisterAndRetrieveStep() {
        MockPipelineStep mockStep = new MockPipelineStep("custom-step");
        registry.register(mockStep);

        assertTrue(registry.hasStep("custom-step"));
        assertTrue(registry.getStep("custom-step").isPresent());
        assertEquals("custom-step", registry.getRequiredStep("custom-step").type());
    }

    @Test
    void testGetRequiredStepThrowsWhenNotFound() {
        assertThrows(ConfigurationException.class, () -> registry.getRequiredStep("unknown-step"));
    }

    @Test
    void testConstructorWithStepList() {
        PipelineStep s1 = new MockPipelineStep("s1");
        PipelineStep s2 = new MockPipelineStep("s2");

        PipelineStepRegistry reg = new PipelineStepRegistry(List.of(s1, s2));
        assertEquals(2, reg.getAllSteps().size());
        assertTrue(reg.hasStep("s1"));
        assertTrue(reg.hasStep("s2"));
    }
}
