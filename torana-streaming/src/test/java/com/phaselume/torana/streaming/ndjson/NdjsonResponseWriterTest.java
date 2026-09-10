package com.phaselume.torana.streaming.ndjson;

import com.phaselume.torana.core.model.AgentResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NdjsonResponseWriterTest {

    @Test
    void testFormatNdjson() {
        NdjsonResponseWriter writer = new NdjsonResponseWriter();
        AgentResponse.Chunk chunk = AgentResponse.Chunk.builder()
                .index(5)
                .textDelta("stream token")
                .last(false)
                .build();

        String line = writer.format(chunk);
        assertTrue(line.endsWith("\n"));
        assertTrue(line.contains("stream token"));
        assertTrue(line.contains("\"index\":5"));
    }
}
