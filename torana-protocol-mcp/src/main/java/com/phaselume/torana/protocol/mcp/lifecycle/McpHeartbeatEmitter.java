package com.phaselume.torana.protocol.mcp.lifecycle;

import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * Emits periodic heartbeat ping events over SSE streams to prevent connection timeouts.
 */
public class McpHeartbeatEmitter {

    private final Duration interval;

    public McpHeartbeatEmitter() {
        this(Duration.ofSeconds(30));
    }

    public McpHeartbeatEmitter(Duration interval) {
        this.interval = (interval != null && !interval.isZero() && !interval.isNegative())
                ? interval
                : Duration.ofSeconds(30);
    }

    /**
     * Returns a Flux that emits SSE ping events on the configured interval.
     */
    public Flux<String> createHeartbeatStream() {
        return Flux.interval(interval)
                .map(tick -> "event: ping\ndata: {}\n\n");
    }

    public Duration getInterval() {
        return interval;
    }
}
