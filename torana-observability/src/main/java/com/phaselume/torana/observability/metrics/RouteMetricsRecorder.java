package com.phaselume.torana.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Records per-route request counts, active in-flight requests, and latency timers.
 */
public class RouteMetricsRecorder {

    private final MeterRegistry registry;
    private final ConcurrentHashMap<String, AtomicInteger> activeRequests = new ConcurrentHashMap<>();

    public RouteMetricsRecorder(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordRequest(String routeId, String method, int statusCode, Duration duration) {
        if (registry == null) return;

        String route = (routeId != null && !routeId.isBlank()) ? routeId : "global";
        String meth = (method != null && !method.isBlank()) ? method : "UNKNOWN";
        String status = String.valueOf(statusCode);
        String outcome = statusCode < 400 ? "SUCCESS" : (statusCode < 500 ? "CLIENT_ERROR" : "SERVER_ERROR");

        Counter.builder("torana.requests.total")
                .tag("route", route)
                .tag("method", meth)
                .tag("status", status)
                .register(registry)
                .increment();

        Timer.builder("torana.request.duration")
                .tag("route", route)
                .tag("outcome", outcome)
                .register(registry)
                .record(duration);
    }

    public void incrementActive(String routeId) {
        if (registry == null) return;
        String route = (routeId != null && !routeId.isBlank()) ? routeId : "global";
        activeRequests.computeIfAbsent(route, r -> {
            AtomicInteger gaugeVal = new AtomicInteger(0);
            registry.gauge("torana.requests.active", io.micrometer.core.instrument.Tags.of("route", r), gaugeVal);
            return gaugeVal;
        }).incrementAndGet();
    }

    public void decrementActive(String routeId) {
        if (registry == null) return;
        String route = (routeId != null && !routeId.isBlank()) ? routeId : "global";
        AtomicInteger count = activeRequests.get(route);
        if (count != null) {
            count.decrementAndGet();
        }
    }
}
