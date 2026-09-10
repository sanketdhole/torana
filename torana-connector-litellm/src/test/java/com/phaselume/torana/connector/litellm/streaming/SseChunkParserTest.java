package com.phaselume.torana.connector.litellm.streaming;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SseChunkParserTest {

    private final SseChunkParser parser = new SseChunkParser();
    private final DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();

    @Test
    void testParseSseStreamWithDoneSentinel() {
        String ssePayload = """
                data: {"id":"chatcmpl-1","choices":[{"delta":{"content":"Hello"}}]}
                
                data: {"id":"chatcmpl-1","choices":[{"delta":{"content":" world"}}]}
                
                data: {"id":"chatcmpl-1","choices":[{"finish_reason":"stop"}]}
                
                data: [DONE]
                """;

        DataBuffer buffer = bufferFactory.wrap(ssePayload.getBytes(StandardCharsets.UTF_8));
        Flux<DataBuffer> dataBufferFlux = Flux.just(buffer);

        StepVerifier.create(parser.parse(dataBufferFlux))
                .assertNext(chunk -> {
                    assertNotNull(chunk);
                    assertEquals("chatcmpl-1", chunk.getId());
                    assertEquals("Hello", chunk.getChoices().get(0).getDelta().getContent());
                })
                .assertNext(chunk -> {
                    assertEquals(" world", chunk.getChoices().get(0).getDelta().getContent());
                })
                .assertNext(chunk -> {
                    assertEquals("stop", chunk.getChoices().get(0).getFinishReason());
                })
                .verifyComplete();
    }
}
