package com.phaselume.torana.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MetricsRecorderTest {

    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    void testRouteMetricsRecorder() {
        RouteMetricsRecorder recorder = new RouteMetricsRecorder(meterRegistry);

        recorder.incrementActive("route-1");
        Gauge activeGauge = meterRegistry.find("torana.requests.active").tag("route", "route-1").gauge();
        assertNotNull(activeGauge);
        assertEquals(1.0, activeGauge.value());

        recorder.recordRequest("route-1", "GET", 200, Duration.ofMillis(50));
        Counter counter = meterRegistry.find("torana.requests.total").tag("route", "route-1").tag("status", "200").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());

        Timer timer = meterRegistry.find("torana.request.duration").tag("route", "route-1").tag("outcome", "SUCCESS").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());

        recorder.decrementActive("route-1");
        assertEquals(0.0, activeGauge.value());
    }

    @Test
    void testConnectorMetricsRecorder() {
        ConnectorMetricsRecorder recorder = new ConnectorMetricsRecorder(meterRegistry);

        recorder.recordCall("http-backend", "SUCCESS", Duration.ofMillis(30));

        Counter counter = meterRegistry.find("torana.connector.calls").tag("connector", "http-backend").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());

        Timer timer = meterRegistry.find("torana.connector.duration").tag("connector", "http-backend").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void testStreamingMetricsRecorder() {
        StreamingMetricsRecorder recorder = new StreamingMetricsRecorder(meterRegistry);

        recorder.incrementStream("mcp");
        Gauge activeGauge = meterRegistry.find("torana.streaming.active").tag("protocol", "mcp").gauge();
        assertNotNull(activeGauge);
        assertEquals(1.0, activeGauge.value());

        recorder.recordChunk("sse-route");
        Counter chunkCounter = meterRegistry.find("torana.streaming.chunks").tag("route", "sse-route").counter();
        assertNotNull(chunkCounter);
        assertEquals(1.0, chunkCounter.count());

        recorder.decrementStream("mcp");
        assertEquals(0.0, activeGauge.value());
    }

    @Test
    void testAuthMetricsRecorder() {
        AuthMetricsRecorder recorder = new AuthMetricsRecorder(meterRegistry);

        recorder.recordAuthAttempt("jwt-provider", true);
        Counter counter = meterRegistry.find("torana.auth.attempts").tag("provider", "jwt-provider").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());

        recorder.recordAuthzLatency("opa", Duration.ofMillis(15));
        Timer opaTimer = meterRegistry.find("torana.authz.latency").tag("engine", "opa").timer();
        assertNotNull(opaTimer);
        assertEquals(1, opaTimer.count());

        recorder.recordRateLimitRejected("route-api", "sliding_window");
        Counter rlCounter = meterRegistry.find("torana.ratelimit.rejected").tag("route", "route-api").counter();
        assertNotNull(rlCounter);
        assertEquals(1.0, rlCounter.count());
    }
}
