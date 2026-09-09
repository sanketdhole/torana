package com.phaselume.torana.test.support;

import com.phaselume.torana.core.model.AgentResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fluent assertion utilities for reactive pipelines and streams.
 */
public final class ReactiveTestSupport {

    private ReactiveTestSupport() {}

    /**
     * Verifies that a stream produces exactly expectedCount chunks and completes.
     */
    public static void expectChunks(Flux<AgentResponse.Chunk> stream, int expectedCount) {
        StepVerifier.create(stream)
                .expectNextCount(expectedCount)
                .verifyComplete();
    }

    /**
     * Verifies that a stream produces expected chunks with specific text deltas.
     */
    public static void expectTextDeltas(Flux<AgentResponse.Chunk> stream, String... expectedDeltas) {
        StepVerifier.Step<AgentResponse.Chunk> verifier = StepVerifier.create(stream);
        for (String delta : expectedDeltas) {
            verifier = verifier.expectNextMatches(chunk -> delta.equals(chunk.getTextDelta()));
        }
        verifier.verifyComplete();
    }

    /**
     * Verifies that a publisher errors with the specified exception class.
     */
    public static <T> void expectError(Flux<T> stream, Class<? extends Throwable> expectedErrorClass) {
        StepVerifier.create(stream)
                .expectError(expectedErrorClass)
                .verify();
    }

    /**
     * Verifies that a Mono errors with the specified exception class.
     */
    public static <T> void expectError(Mono<T> mono, Class<? extends Throwable> expectedErrorClass) {
        StepVerifier.create(mono)
                .expectError(expectedErrorClass)
                .verify();
    }

    /**
     * Collects all items from a flux and passes the list to a consumer assertion.
     */
    public static <T> void collectAndAssert(Flux<T> flux, Consumer<List<T>> assertion) {
        List<T> collected = flux.collectList().block(Duration.ofSeconds(10));
        assertion.accept(collected);
    }
}
