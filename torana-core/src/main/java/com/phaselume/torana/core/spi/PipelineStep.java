package com.phaselume.torana.core.spi;

import com.phaselume.torana.core.model.AgentContext;
import reactor.core.publisher.Mono;

/**
 * SPI for individual pipeline execution steps (e.g. PromptAugment, RagRetrieval, ToolCall, LlmCall, Transform).
 */
public interface PipelineStep {

    /**
     * Unique step type identifier matching YAML step definitions (e.g. "rag-retrieval", "llm-call", "transform").
     */
    String type();

    /**
     * Execute this step asynchronously, transforming the context or performing side-effects.
     */
    Mono<AgentContext> execute(AgentContext context);
}
