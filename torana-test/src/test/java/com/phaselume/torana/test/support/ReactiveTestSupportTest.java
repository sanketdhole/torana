package com.phaselume.torana.test.support;

import com.phaselume.torana.core.exception.ToranaException;
import com.phaselume.torana.core.model.AgentResponse;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReactiveTestSupportTest {

    @Test
    void testExpectChunksAndTextDeltas() {
        Flux<AgentResponse.Chunk> stream = Flux.just(
                AgentResponse.Chunk.text("Hello"),
                AgentResponse.Chunk.text(" World")
        );

        ReactiveTestSupport.expectChunks(stream, 2);

        Flux<AgentResponse.Chunk> stream2 = Flux.just(
                AgentResponse.Chunk.text("Hello"),
                AgentResponse.Chunk.text(" World")
        );
        ReactiveTestSupport.expectTextDeltas(stream2, "Hello", " World");
    }

    @Test
    void testExpectError() {
        Flux<String> errorFlux = Flux.error(new ToranaException("TEST_ERR", "Test message"));
        ReactiveTestSupport.expectError(errorFlux, ToranaException.class);

        Mono<String> errorMono = Mono.error(new IllegalArgumentException("Bad arg"));
        ReactiveTestSupport.expectError(errorMono, IllegalArgumentException.class);
    }

    @Test
    void testCollectAndAssert() {
        Flux<String> flux = Flux.just("A", "B", "C");
        ReactiveTestSupport.collectAndAssert(flux, list -> {
            assertEquals(3, list.size());
            assertEquals("A", list.get(0));
            assertEquals("C", list.get(2));
        });
    }
}
