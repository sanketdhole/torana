package com.phaselume.torana.streaming.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.core.model.AgentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Encodes AgentResponse chunks as WebSocket frames sent over a WebSocketSession.
 */
public class WebSocketChunkWriter {

    private static final Logger log = LoggerFactory.getLogger(WebSocketChunkWriter.class);

    private final ObjectMapper objectMapper;

    public WebSocketChunkWriter() {
        this(new ObjectMapper());
    }

    public WebSocketChunkWriter(ObjectMapper objectMapper) {
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
    }

    public Mono<Void> write(WebSocketSession session, Flux<AgentResponse.Chunk> chunks) {
        if (session == null || chunks == null) {
            return Mono.empty();
        }

        Flux<WebSocketMessage> messageFlux = chunks.map(chunk -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("index", chunk.getIndex());
                payload.put("last", chunk.isLast());
                if (chunk.getTextDelta() != null) {
                    payload.put("delta", chunk.getTextDelta());
                }
                if (chunk.getFinishReason() != null) {
                    payload.put("finish_reason", chunk.getFinishReason());
                }
                String json = objectMapper.writeValueAsString(payload);
                return session.textMessage(json);
            } catch (Exception e) {
                log.error("Failed to serialize WebSocket frame", e);
                return session.textMessage("{\"error\":\"Serialization error\"}");
            }
        });

        return session.send(messageFlux);
    }
}
