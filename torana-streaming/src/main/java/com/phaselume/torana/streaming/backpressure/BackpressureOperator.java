package com.phaselume.torana.streaming.backpressure;

import com.phaselume.torana.core.model.AgentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;

/**
 * Applies configured backpressure operators to an AgentResponse.Chunk Flux.
 */
public class BackpressureOperator {

    private static final Logger log = LoggerFactory.getLogger(BackpressureOperator.class);

    private final BackpressureConfig config;

    public BackpressureOperator() {
        this(new BackpressureConfig());
    }

    public BackpressureOperator(BackpressureConfig config) {
        this.config = (config != null) ? config : new BackpressureConfig();
    }

    public Flux<AgentResponse.Chunk> apply(Flux<AgentResponse.Chunk> source) {
        if (source == null) return Flux.empty();

        BackpressureConfig.Strategy strategy = config.getStrategy() != null ? config.getStrategy() : BackpressureConfig.Strategy.BUFFER;
        int bufferSize = config.getBufferSize() > 0 ? config.getBufferSize() : 256;

        switch (strategy) {
            case DROP:
                return source.onBackpressureDrop(dropped -> log.warn("Dropped streaming chunk index {} due to backpressure", dropped.getIndex()));
            case LATEST:
                return source.onBackpressureLatest();
            case ERROR:
                return source.onBackpressureBuffer(bufferSize, BufferOverflowStrategy.ERROR);
            case BUFFER:
            default:
                return source.onBackpressureBuffer(bufferSize, BufferOverflowStrategy.DROP_OLDEST);
        }
    }
}
