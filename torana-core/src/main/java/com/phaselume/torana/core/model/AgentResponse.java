package com.phaselume.torana.core.model;

import lombok.Builder;
import lombok.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Normalized, outbound response from Torana.
 * Wraps either a reactive streaming Flux of Chunks or a single buffered Mono response.
 */
@Value
@Builder(toBuilder = true)
public class AgentResponse {

    @Builder.Default
    HttpStatusCode status = HttpStatus.OK;

    @Builder.Default
    HttpHeaders headers = new HttpHeaders();

    Flux<Chunk> stream;

    Mono<byte[]> bufferedBody;

    @Builder.Default
    Map<String, Object> metadata = Collections.emptyMap();

    /**
     * Represents a single streaming chunk (SSE event, MCP stream delta, or LLM token).
     */
    @Value
    @Builder(toBuilder = true)
    public static class Chunk {
        int index;
        byte[] data;
        String textDelta;
        boolean last;
        String finishReason;
        Integer tokenCount;
        
        @Builder.Default
        Instant timestamp = Instant.now();
        
        @Builder.Default
        Map<String, Object> metadata = Collections.emptyMap();

        public static Chunk text(String delta) {
            return Chunk.builder()
                    .textDelta(delta)
                    .data(delta != null ? delta.getBytes() : new byte[0])
                    .last(false)
                    .build();
        }

        public static Chunk last(String finishReason) {
            return Chunk.builder()
                    .last(true)
                    .finishReason(finishReason)
                    .build();
        }
    }

    public static AgentResponse streaming(Flux<Chunk> stream) {
        return AgentResponse.builder()
                .status(HttpStatus.OK)
                .stream(stream)
                .build();
    }

    public static AgentResponse buffered(byte[] body) {
        return AgentResponse.builder()
                .status(HttpStatus.OK)
                .bufferedBody(Mono.just(body))
                .build();
    }

    public static AgentResponse buffered(HttpStatusCode status, byte[] body) {
        return AgentResponse.builder()
                .status(status)
                .bufferedBody(Mono.just(body))
                .build();
    }
}
