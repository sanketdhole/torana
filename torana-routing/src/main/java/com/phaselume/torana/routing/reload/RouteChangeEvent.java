package com.phaselume.torana.routing.reload;

import com.phaselume.torana.core.config.RouteDefinition;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Application event published when routes are hot-reloaded.
 */
public class RouteChangeEvent extends ApplicationEvent {

    private final List<RouteDefinition> previousRoutes;
    private final List<RouteDefinition> currentRoutes;
    private final Instant timestamp;

    public RouteChangeEvent(Object source, List<RouteDefinition> previousRoutes, List<RouteDefinition> currentRoutes) {
        super(source);
        this.previousRoutes = previousRoutes != null ? previousRoutes : Collections.emptyList();
        this.currentRoutes = currentRoutes != null ? currentRoutes : Collections.emptyList();
        this.timestamp = Instant.now();
    }

    public List<RouteDefinition> getPreviousRoutes() {
        return previousRoutes;
    }

    public List<RouteDefinition> getCurrentRoutes() {
        return currentRoutes;
    }

    public Instant getEventTimestamp() {
        return timestamp;
    }
}
