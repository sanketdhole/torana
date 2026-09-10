package com.phaselume.torana.streaming.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.core.model.AgentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Formats internal AgentResponse.Chunk objects into W3C Server-Sent Event (SSE) wire frames.
 */
public class SseEventFormatter {

    private static final Logger log = LoggerFactory.getLogger(SseEventFormatter.class);
    public static final String DONE_SENTINEL = "data: [DONE]\n\n";

    private final ObjectMapper objectMapper;

    public SseEventFormatter() {
        this.objectMapper = new ObjectMapper();
    }

    public SseEventFormatter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public String format(AgentResponse.Chunk chunk) {
        if (chunk == null) {
            return "";
        }

        if (chunk.isLast()) {
            StringBuilder sb = new StringBuilder();
            if (chunk.getFinishReason() != null) {
                try {
                    Map<String, Object> finishPayload = Map.of(
                            "finish_reason", chunk.getFinishReason(),
                            "last", true
                    );
                    sb.append("data: ").append(objectMapper.writeValueAsString(finishPayload)).append("\n\n");
                } catch (Exception e) {
                    log.error("Failed to serialize finish payload", e);
                }
            }
            sb.append(DONE_SENTINEL);
            return sb.toString();
        }

        StringBuilder sb = new StringBuilder();

        // Optional Event / ID from metadata
        if (chunk.getMetadata() != null) {
            Object id = chunk.getMetadata().get("id");
            if (id != null) {
                sb.append("id: ").append(id).append("\n");
            }
            Object event = chunk.getMetadata().get("event");
            if (event != null) {
                sb.append("event: ").append(event).append("\n");
            }
        }

        // Payload data formatting
        if (chunk.getTextDelta() != null) {
            try {
                Map<String, Object> deltaMap = new HashMap<>();
                deltaMap.put("delta", chunk.getTextDelta());
                deltaMap.put("index", chunk.getIndex());
                sb.append("data: ").append(objectMapper.writeValueAsString(deltaMap)).append("\n\n");
            } catch (Exception e) {
                sb.append("data: ").append(chunk.getTextDelta()).append("\n\n");
            }
        } else if (chunk.getData() != null && chunk.getData().length > 0) {
            String text = new String(chunk.getData(), StandardCharsets.UTF_8);
            sb.append("data: ").append(text).append("\n\n");
        } else {
            sb.append("data: {}\n\n");
        }

        return sb.toString();
    }

    public String formatPing() {
        return ": ping\n\n";
    }

    public String formatError(String errorMessage) {
        try {
            Map<String, Object> errorMap = Map.of(
                    "error", true,
                    "message", errorMessage != null ? errorMessage : "Internal Gateway Streaming Error"
            );
            return "event: error\ndata: " + objectMapper.writeValueAsString(errorMap) + "\n\n";
        } catch (Exception e) {
            return "event: error\ndata: {\"error\":\"" + errorMessage + "\"}\n\n";
        }
    }
}
