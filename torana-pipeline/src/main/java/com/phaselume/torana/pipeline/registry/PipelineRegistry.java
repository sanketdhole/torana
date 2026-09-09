package com.phaselume.torana.pipeline.registry;

import com.phaselume.torana.core.config.PipelineDefinition;
import com.phaselume.torana.core.config.ToranaProperties;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory registry holding active PipelineDefinitions.
 * Supports runtime atomic replacement for zero-downtime hot-reload.
 */
@Component
public class PipelineRegistry {

    private final Map<String, PipelineDefinition> pipelineMap = new ConcurrentHashMap<>();

    public PipelineRegistry() {
    }

    public PipelineRegistry(ToranaProperties properties) {
        if (properties != null && properties.getPipeline() != null && properties.getPipeline().getPipelines() != null) {
            setPipelines(properties.getPipeline().getPipelines());
        }
    }

    public void register(PipelineDefinition definition) {
        if (definition != null && definition.getName() != null) {
            pipelineMap.put(definition.getName(), definition);
        }
    }

    public Optional<PipelineDefinition> getPipeline(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(pipelineMap.get(name));
    }

    public void setPipelines(Collection<PipelineDefinition> definitions) {
        pipelineMap.clear();
        if (definitions != null) {
            for (PipelineDefinition def : definitions) {
                register(def);
            }
        }
    }

    public void setPipelines(Map<String, PipelineDefinition> definitions) {
        pipelineMap.clear();
        if (definitions != null) {
            pipelineMap.putAll(definitions);
        }
    }

    public boolean hasPipeline(String name) {
        return name != null && pipelineMap.containsKey(name);
    }

    public Map<String, PipelineDefinition> getAllPipelines() {
        return Collections.unmodifiableMap(pipelineMap);
    }

    public int size() {
        return pipelineMap.size();
    }
}
