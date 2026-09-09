package com.phaselume.torana.pipeline.executor;

import com.phaselume.torana.core.config.PipelineDefinition;
import com.phaselume.torana.core.exception.PipelineException;
import com.phaselume.torana.pipeline.registry.PipelineRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FallbackPipelineResolverTest {

    private PipelineRegistry pipelineRegistry;
    private FallbackPipelineResolver resolver;

    @BeforeEach
    void setUp() {
        pipelineRegistry = new PipelineRegistry();
        resolver = new FallbackPipelineResolver(pipelineRegistry);
    }

    @Test
    void testResolveValidFallback() {
        PipelineDefinition fallback = PipelineDefinition.builder().name("fallback-pipeline").build();
        PipelineDefinition primary = PipelineDefinition.builder().name("primary").onError("fallback-pipeline").build();

        pipelineRegistry.register(fallback);
        pipelineRegistry.register(primary);

        Optional<PipelineDefinition> resolved = resolver.resolveFallback(primary, Set.of(), 0);
        assertTrue(resolved.isPresent());
        assertEquals("fallback-pipeline", resolved.get().getName());
    }

    @Test
    void testResolveNoFallbackConfiguredReturnsEmpty() {
        PipelineDefinition primary = PipelineDefinition.builder().name("primary").build();
        Optional<PipelineDefinition> resolved = resolver.resolveFallback(primary, Set.of(), 0);
        assertTrue(resolved.isEmpty());
    }

    @Test
    void testResolveMissingFallbackThrowsPipelineException() {
        PipelineDefinition primary = PipelineDefinition.builder().name("primary").onError("missing").build();
        assertThrows(PipelineException.class, () -> resolver.resolveFallback(primary, Set.of(), 0));
    }

    @Test
    void testCircularFallbackThrowsPipelineException() {
        PipelineDefinition primary = PipelineDefinition.builder().name("primary").onError("fallback").build();
        PipelineDefinition fallback = PipelineDefinition.builder().name("fallback").onError("primary").build();

        pipelineRegistry.register(primary);
        pipelineRegistry.register(fallback);

        assertThrows(PipelineException.class, () -> resolver.resolveFallback(primary, Set.of("fallback"), 1));
    }

    @Test
    void testMaxDepthExceededThrowsPipelineException() {
        PipelineDefinition primary = PipelineDefinition.builder().name("primary").onError("fallback").build();
        PipelineDefinition fallback = PipelineDefinition.builder().name("fallback").build();
        pipelineRegistry.register(fallback);

        assertThrows(PipelineException.class, () -> resolver.resolveFallback(primary, Set.of(), 5));
    }
}
