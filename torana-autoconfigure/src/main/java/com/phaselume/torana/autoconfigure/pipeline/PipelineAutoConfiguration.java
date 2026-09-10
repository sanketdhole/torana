package com.phaselume.torana.autoconfigure.pipeline;

import com.phaselume.torana.core.spi.PipelineStep;
import com.phaselume.torana.pipeline.executor.FallbackPipelineResolver;
import com.phaselume.torana.pipeline.executor.PipelineExecutor;
import com.phaselume.torana.pipeline.registry.PipelineRegistry;
import com.phaselume.torana.pipeline.registry.PipelineStepRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Auto-configuration for PipelineStepRegistry, PipelineRegistry, FallbackPipelineResolver, and PipelineExecutor.
 */
@AutoConfiguration
@ConditionalOnClass(PipelineExecutor.class)
public class PipelineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PipelineStepRegistry pipelineStepRegistry(List<PipelineStep> steps) {
        return new PipelineStepRegistry(steps);
    }

    @Bean
    @ConditionalOnMissingBean
    public PipelineRegistry pipelineRegistry() {
        return new PipelineRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public FallbackPipelineResolver fallbackPipelineResolver(PipelineRegistry pipelineRegistry) {
        return new FallbackPipelineResolver(pipelineRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public PipelineExecutor pipelineExecutor(PipelineRegistry pipelineRegistry,
                                             PipelineStepRegistry stepRegistry,
                                             FallbackPipelineResolver fallbackResolver) {
        return new PipelineExecutor(pipelineRegistry, stepRegistry, fallbackResolver);
    }
}
