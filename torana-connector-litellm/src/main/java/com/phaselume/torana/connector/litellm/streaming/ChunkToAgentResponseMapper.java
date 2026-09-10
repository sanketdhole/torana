package com.phaselume.torana.connector.litellm.streaming;

import com.phaselume.torana.connector.litellm.model.ChatCompletionChunk;
import com.phaselume.torana.core.model.AgentResponse;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Maps incoming {@link ChatCompletionChunk} elements to normalized {@link AgentResponse.Chunk} elements.
 */
public class ChunkToAgentResponseMapper {

    /**
     * Converts a stream of {@link ChatCompletionChunk}s into an {@link AgentResponse.Chunk} stream.
     */
    public Flux<AgentResponse.Chunk> map(Flux<ChatCompletionChunk> chunkFlux) {
        if (chunkFlux == null) {
            return Flux.empty();
        }

        AtomicInteger indexCounter = new AtomicInteger(0);

        return chunkFlux.map(chunk -> {
            String textDelta = "";
            String finishReason = null;

            if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                ChatCompletionChunk.Choice choice = chunk.getChoices().get(0);
                if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
                    textDelta = choice.getDelta().getContent();
                }
                finishReason = choice.getFinishReason();
            }

            Integer tokenCount = null;
            if (chunk.getUsage() != null) {
                tokenCount = chunk.getUsage().getCompletionTokens();
            }

            boolean isLast = finishReason != null && !finishReason.isBlank();

            Map<String, Object> metadata = new HashMap<>();
            if (chunk.getModel() != null) {
                metadata.put("model", chunk.getModel());
            }
            if (chunk.getId() != null) {
                metadata.put("completion_id", chunk.getId());
            }

            return AgentResponse.Chunk.builder()
                    .index(indexCounter.getAndIncrement())
                    .data(textDelta.getBytes(StandardCharsets.UTF_8))
                    .textDelta(textDelta)
                    .finishReason(finishReason)
                    .tokenCount(tokenCount)
                    .last(isLast)
                    .metadata(Collections.unmodifiableMap(metadata))
                    .build();
        });
    }
}
