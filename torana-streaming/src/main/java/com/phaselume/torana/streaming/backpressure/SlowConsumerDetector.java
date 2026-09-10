package com.phaselume.torana.streaming.backpressure;

import com.phaselume.torana.core.model.AgentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * Monitors chunk delivery rate to detect slow consumers exceeding a timeout threshold.
 */
public class SlowConsumerDetector {

    private static final Logger log = LoggerFactory.getLogger(SlowConsumerDetector.class);

    private final Duration timeout;

    public SlowConsumerDetector() {
        this(Duration.ofSeconds(60));
    }

    public SlowConsumerDetector(Duration timeout) {
        this.timeout = (timeout != null) ? timeout : Duration.ofSeconds(60);
    }

    public Flux<AgentResponse.Chunk> monitor(Flux<AgentResponse.Chunk> source, String streamId) {
        if (source == null) return Flux.empty();

        return source.timeout(timeout)
                .doOnError(e -> log.warn("Slow consumer detected or timeout exceeded for stream [{}]: {}", streamId, e.getMessage()));
    }
}
