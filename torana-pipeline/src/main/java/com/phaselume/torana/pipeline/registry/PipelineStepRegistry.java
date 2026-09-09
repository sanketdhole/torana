package com.phaselume.torana.pipeline.registry;

import com.phaselume.torana.core.exception.ConfigurationException;
import com.phaselume.torana.core.spi.PipelineStep;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry indexing all available PipelineStep SPI implementations by their unique step type.
 */
@Component
public class PipelineStepRegistry {

    private final Map<String, PipelineStep> stepMap = new ConcurrentHashMap<>();

    public PipelineStepRegistry() {
    }

    public PipelineStepRegistry(List<PipelineStep> steps) {
        if (steps != null) {
            for (PipelineStep step : steps) {
                register(step);
            }
        }
    }

    public void register(PipelineStep step) {
        if (step != null && step.type() != null) {
            stepMap.put(step.type(), step);
        }
    }

    public Optional<PipelineStep> getStep(String type) {
        if (type == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(stepMap.get(type));
    }

    public PipelineStep getRequiredStep(String type) {
        return getStep(type)
                .orElseThrow(() -> new ConfigurationException("No PipelineStep registered for step type: " + type));
    }

    public boolean hasStep(String type) {
        return type != null && stepMap.containsKey(type);
    }

    public Map<String, PipelineStep> getAllSteps() {
        return Collections.unmodifiableMap(stepMap);
    }
}
