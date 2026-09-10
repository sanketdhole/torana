package com.phaselume.torana.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;

/**
 * Records authentication, authorization, and rate-limiting metrics.
 */
public class AuthMetricsRecorder {

    private final MeterRegistry registry;

    public AuthMetricsRecorder(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordAuthAttempt(String provider, boolean success) {
        if (registry == null) return;
        String prov = (provider != null && !provider.isBlank()) ? provider : "unknown";
        String outcome = success ? "SUCCESS" : "FAILURE";

        Counter.builder("torana.auth.attempts")
                .tag("provider", prov)
                .tag("outcome", outcome)
                .register(registry)
                .increment();
    }

    public void recordAuthzLatency(String engine, Duration duration) {
        if (registry == null || duration == null) return;
        String eng = (engine != null && !engine.isBlank()) ? engine : "opa";

        Timer.builder("torana.authz.latency")
                .tag("engine", eng)
                .register(registry)
                .record(duration);
    }

    public void recordRateLimitRejected(String routeId, String policy) {
        if (registry == null) return;
        String route = (routeId != null && !routeId.isBlank()) ? routeId : "global";
        String pol = (policy != null && !policy.isBlank()) ? policy : "default";

        Counter.builder("torana.ratelimit.rejected")
                .tag("route", route)
                .tag("policy", pol)
                .register(registry)
                .increment();
    }
}
