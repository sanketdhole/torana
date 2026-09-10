package com.phaselume.torana.connector.http.proxy;

import com.phaselume.torana.core.model.AgentResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Maps upstream HTTP response data buffers into normalized {@link AgentResponse.Chunk} stream.
 */
public class ResponseMappingStrategy {

    /**
     * Converts a {@link ClientResponse} into a reactive Flux of response chunks.
     */
    public Flux<AgentResponse.Chunk> mapResponse(ClientResponse clientResponse) {
        if (clientResponse == null) {
            return Flux.empty();
        }

        AtomicInteger indexCounter = new AtomicInteger(0);

        return clientResponse.bodyToFlux(DataBuffer.class)
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    return AgentResponse.Chunk.builder()
                            .index(indexCounter.getAndIncrement())
                            .data(bytes)
                            .last(false)
                            .build();
                })
                .concatWith(Flux.defer(() -> Flux.just(
                        AgentResponse.Chunk.builder()
                                .index(indexCounter.get())
                                .data(new byte[0])
                                .last(true)
                                .finishReason("stop")
                                .build()
                )));
    }
}
