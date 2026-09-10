package com.phaselume.torana.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Records streaming throughput, chunks count, and active stream gauges.
 */
public class StreamingMetricsRecorder {

    private final MeterRegistry registry;
    private final ConcurrentHashMap<String, AtomicInteger> activeStreams = new ConcurrentHashMap<>();

    public StreamingMetricsRecorder(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordChunk(String routeId) {
        if (registry == null) return;
        String route = (routeId != null && !routeId.isBlank()) ? routeId : "global";
        Counter.builder("torana.streaming.chunks")
                .tag("route", route)
                .register(registry)
                .increment();
    }

    public void incrementStream(String protocol) {
        if (registry == null) return;
        String proto = (protocol != null && !protocol.isBlank()) ? protocol : "http-sse";
        activeStreams.computeIfAbsent(proto, p -> {
            AtomicInteger val = new AtomicInteger(0);
            registry.gauge("torana.streaming.active", Tags.of("protocol", p), val);
            return val;
        }).incrementAndGet();
    }

    public void decrementStream(String protocol) {
        if (registry == null) return;
        String proto = (protocol != null && !protocol.isBlank()) ? protocol : "http-sse";
        AtomicInteger count = activeStreams.get(proto);
        if (count != null) {
            count.decrementAndGet();
        }
    }
}
