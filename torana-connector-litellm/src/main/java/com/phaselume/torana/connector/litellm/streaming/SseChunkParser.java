package com.phaselume.torana.connector.litellm.streaming;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.connector.litellm.model.ChatCompletionChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;

/**
 * Parses reactive {@link DataBuffer} streams containing SSE frames into {@link ChatCompletionChunk} objects.
 */
public class SseChunkParser {

    private static final Logger log = LoggerFactory.getLogger(SseChunkParser.class);
    private static final String DATA_PREFIX = "data:";
    private static final String DONE_SENTINEL = "[DONE]";

    private final ObjectMapper objectMapper;

    public SseChunkParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public SseChunkParser() {
        this(new ObjectMapper());
    }

    /**
     * Transforms incoming SSE data buffers into a Flux of typed chat completion chunks.
     */
    public Flux<ChatCompletionChunk> parse(Flux<DataBuffer> dataBufferFlux) {
        if (dataBufferFlux == null) {
            return Flux.empty();
        }

        return dataBufferFlux
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return new String(bytes, StandardCharsets.UTF_8);
                })
                .flatMap(text -> Flux.fromArray(text.split("\n")))
                .map(String::trim)
                .filter(line -> line.startsWith(DATA_PREFIX))
                .map(line -> line.substring(DATA_PREFIX.length()).trim())
                .filter(data -> !data.isEmpty() && !DONE_SENTINEL.equals(data))
                .flatMap(json -> {
                    try {
                        ChatCompletionChunk chunk = objectMapper.readValue(json, ChatCompletionChunk.class);
                        return Flux.just(chunk);
                    } catch (Exception e) {
                        log.debug("Failed to parse SSE JSON chunk: '{}' error: {}", json, e.getMessage());
                        return Flux.empty();
                    }
                });
    }
}
