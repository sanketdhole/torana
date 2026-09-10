package com.phaselume.torana.streaming.sse;

import com.phaselume.torana.core.model.AgentResponse;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * Interleaves heartbeat ping comments into an SSE chunk stream to prevent idle proxy timeouts.
 */
public class SseHeartbeatEmitter {

    private final Duration interval;

    public SseHeartbeatEmitter() {
        this(Duration.ofSeconds(15));
    }

    public SseHeartbeatEmitter(Duration interval) {
        this.interval = (interval != null) ? interval : Duration.ofSeconds(15);
    }

    public Flux<String> applyHeartbeats(Flux<AgentResponse.Chunk> chunkStream, SseEventFormatter formatter) {
        SseEventFormatter fmt = (formatter != null) ? formatter : new SseEventFormatter();

        Flux<String> dataStream = chunkStream.map(fmt::format);
        Flux<String> heartbeatStream = Flux.interval(interval, interval)
                .map(tick -> fmt.formatPing());

        return Flux.merge(dataStream, heartbeatStream);
    }
}
