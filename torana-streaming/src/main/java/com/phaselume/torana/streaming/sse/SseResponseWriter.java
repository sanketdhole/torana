package com.phaselume.torana.streaming.sse;

import com.phaselume.torana.core.model.AgentResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Encodes streaming chunks into Server-Sent Events written to the reactive ServerHttpResponse.
 */
public class SseResponseWriter {

    private final SseEventFormatter formatter;
    private final SseHeartbeatEmitter heartbeatEmitter;

    public SseResponseWriter() {
        this(new SseEventFormatter(), new SseHeartbeatEmitter());
    }

    public SseResponseWriter(SseEventFormatter formatter, SseHeartbeatEmitter heartbeatEmitter) {
        this.formatter = (formatter != null) ? formatter : new SseEventFormatter();
        this.heartbeatEmitter = (heartbeatEmitter != null) ? heartbeatEmitter : new SseHeartbeatEmitter();
    }

    public Mono<Void> write(ServerHttpResponse response, Flux<AgentResponse.Chunk> chunks, boolean enableHeartbeats) {
        if (response == null || chunks == null) {
            return Mono.empty();
        }

        HttpHeaders headers = response.getHeaders();
        headers.setContentType(MediaType.TEXT_EVENT_STREAM);
        headers.set(HttpHeaders.CACHE_CONTROL, "no-cache");
        headers.set(HttpHeaders.CONNECTION, "keep-alive");

        DataBufferFactory bufferFactory = response.bufferFactory();

        Flux<String> sseLines = enableHeartbeats
                ? heartbeatEmitter.applyHeartbeats(chunks, formatter)
                : chunks.map(formatter::format);

        Flux<DataBuffer> dataBuffers = sseLines
                .map(str -> bufferFactory.wrap(str.getBytes(StandardCharsets.UTF_8)));

        return response.writeWith(dataBuffers);
    }
}
