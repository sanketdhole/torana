package com.phaselume.torana.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;

/**
 * Records backend connector metrics.
 */
public class ConnectorMetricsRecorder {

    private final MeterRegistry registry;

    public ConnectorMetricsRecorder(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordCall(String connectorId, String outcome, Duration duration) {
        if (registry == null) return;

        String connector = (connectorId != null && !connectorId.isBlank()) ? connectorId : "unknown";
        String out = (outcome != null && !outcome.isBlank()) ? outcome : "SUCCESS";

        Counter.builder("torana.connector.calls")
                .tag("connector", connector)
                .tag("outcome", out)
                .register(registry)
                .increment();

        if (duration != null) {
            Timer.builder("torana.connector.duration")
                    .tag("connector", connector)
                    .register(registry)
                    .record(duration);
        }
    }
}
