package com.phaselume.torana.streaming.backpressure;

import com.phaselume.torana.core.model.AgentResponse;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class BackpressureOperatorTest {

    @Test
    void testBufferStrategy() {
        BackpressureConfig config = BackpressureConfig.builder()
                .strategy(BackpressureConfig.Strategy.BUFFER)
                .bufferSize(100)
                .build();
        BackpressureOperator operator = new BackpressureOperator(config);

        Flux<AgentResponse.Chunk> source = Flux.range(1, 10)
                .map(i -> AgentResponse.Chunk.builder().index(i).textDelta("chunk-" + i).build());

        StepVerifier.create(operator.apply(source))
                .expectNextCount(10)
                .verifyComplete();
    }
}
