package com.phaselume.torana.streaming.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Handles errors on WebSocket streams by transmitting structured error frames and closing the session.
 */
public class WebSocketErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketErrorHandler.class);

    private final ObjectMapper objectMapper;

    public WebSocketErrorHandler() {
        this(new ObjectMapper());
    }

    public WebSocketErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
    }

    public Mono<Void> handleError(WebSocketSession session, Throwable error) {
        if (session == null) return Mono.empty();

        String msg = (error != null) ? error.getMessage() : "Unknown gateway stream error";
        log.warn("Handling WebSocket streaming error for session {}: {}", session.getId(), msg);

        try {
            Map<String, Object> errFrame = Map.of(
                    "error", true,
                    "message", msg
            );
            String json = objectMapper.writeValueAsString(errFrame);
            return session.send(Mono.just(session.textMessage(json)))
                    .then(session.close(CloseStatus.SERVER_ERROR));
        } catch (Exception e) {
            return session.close(CloseStatus.SERVER_ERROR);
        }
    }
}
