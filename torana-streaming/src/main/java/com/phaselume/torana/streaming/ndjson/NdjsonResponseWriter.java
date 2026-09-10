package com.phaselume.torana.streaming.ndjson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.core.model.AgentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Encodes streaming chunks into Newline-Delimited JSON (application/x-ndjson).
 */
public class NdjsonResponseWriter {

    private static final Logger log = LoggerFactory.getLogger(NdjsonResponseWriter.class);
    public static final MediaType NDJSON_MEDIA_TYPE = MediaType.parseMediaType("application/x-ndjson");

    private final ObjectMapper objectMapper;

    public NdjsonResponseWriter() {
        this(new ObjectMapper());
    }

    public NdjsonResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
    }

    public String format(AgentResponse.Chunk chunk) {
        if (chunk == null) return "\n";
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("index", chunk.getIndex());
            map.put("last", chunk.isLast());
            if (chunk.getTextDelta() != null) {
                map.put("delta", chunk.getTextDelta());
            }
            if (chunk.getFinishReason() != null) {
                map.put("finish_reason", chunk.getFinishReason());
            }
            if (chunk.getTokenCount() != null) {
                map.put("token_count", chunk.getTokenCount());
            }
            return objectMapper.writeValueAsString(map) + "\n";
        } catch (Exception e) {
            log.error("Failed to serialize NDJSON chunk", e);
            return "{\"error\":\"Serialization failure\"}\n";
        }
    }

    public Mono<Void> write(ServerHttpResponse response, Flux<AgentResponse.Chunk> chunks) {
        if (response == null || chunks == null) {
            return Mono.empty();
        }

        HttpHeaders headers = response.getHeaders();
        headers.setContentType(NDJSON_MEDIA_TYPE);
        headers.set(HttpHeaders.CACHE_CONTROL, "no-cache");

        DataBufferFactory bufferFactory = response.bufferFactory();

        Flux<DataBuffer> dataBuffers = chunks
                .map(this::format)
                .map(line -> bufferFactory.wrap(line.getBytes(StandardCharsets.UTF_8)));

        return response.writeWith(dataBuffers);
    }
}
