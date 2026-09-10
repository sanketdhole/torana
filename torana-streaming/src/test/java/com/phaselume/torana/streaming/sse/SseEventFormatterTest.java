package com.phaselume.torana.streaming.sse;

import com.phaselume.torana.core.model.AgentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SseEventFormatterTest {

    private SseEventFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new SseEventFormatter();
    }

    @Test
    void testFormatTextDeltaChunk() {
        AgentResponse.Chunk chunk = AgentResponse.Chunk.builder()
                .index(0)
                .textDelta("Hello world")
                .build();

        String formatted = formatter.format(chunk);
        assertTrue(formatted.startsWith("data: "));
        assertTrue(formatted.contains("Hello world"));
        assertTrue(formatted.endsWith("\n\n"));
    }

    @Test
    void testFormatLastChunkContainsDoneSentinel() {
        AgentResponse.Chunk lastChunk = AgentResponse.Chunk.builder()
                .index(1)
                .last(true)
                .finishReason("stop")
                .build();

        String formatted = formatter.format(lastChunk);
        assertTrue(formatted.contains("stop"));
        assertTrue(formatted.endsWith(SseEventFormatter.DONE_SENTINEL));
    }

    @Test
    void testFormatMetadataIdAndEvent() {
        AgentResponse.Chunk chunk = AgentResponse.Chunk.builder()
                .index(2)
                .textDelta("custom event")
                .metadata(Map.of("id", "msg-123", "event", "delta"))
                .build();

        String formatted = formatter.format(chunk);
        assertTrue(formatted.contains("id: msg-123\n"));
        assertTrue(formatted.contains("event: delta\n"));
        assertTrue(formatted.contains("custom event"));
    }

    @Test
    void testFormatPing() {
        String ping = formatter.formatPing();
        assertEquals(": ping\n\n", ping);
    }
}
