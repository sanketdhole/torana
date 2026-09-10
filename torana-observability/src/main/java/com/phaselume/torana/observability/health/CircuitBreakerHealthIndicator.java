package com.phaselume.torana.observability.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Health indicator monitoring circuit breaker states across gateway connectors.
 */
public class CircuitBreakerHealthIndicator implements HealthIndicator {

    private final Map<String, String> circuitBreakerStates = new ConcurrentHashMap<>();

    public void updateState(String connectorId, String state) {
        if (connectorId != null && state != null) {
            circuitBreakerStates.put(connectorId, state.toUpperCase());
        }
    }

    @Override
    public Health health() {
        if (circuitBreakerStates.isEmpty()) {
            return Health.up().withDetail("circuitBreakers", "All nominal / None registered").build();
        }

        boolean hasOpen = circuitBreakerStates.values().stream().anyMatch(s -> "OPEN".equalsIgnoreCase(s) || "FORCED_OPEN".equalsIgnoreCase(s));
        Health.Builder builder = hasOpen ? Health.status("DEGRADED") : Health.up();

        builder.withDetail("circuitBreakers", circuitBreakerStates);
        return builder.build();
    }
}
