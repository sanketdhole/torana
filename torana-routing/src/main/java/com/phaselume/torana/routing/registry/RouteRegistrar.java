package com.phaselume.torana.routing.registry;

import com.phaselume.torana.core.config.RouteDefinition;
import com.phaselume.torana.core.config.ToranaProperties;
import com.phaselume.torana.core.exception.ConfigurationException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.List;

/**
 * Initializes and validates routes from ToranaProperties into RouteRegistry upon application startup.
 */
public class RouteRegistrar {

    private final ToranaProperties properties;
    private final RouteRegistry routeRegistry;

    public RouteRegistrar(ToranaProperties properties, RouteRegistry routeRegistry) {
        this.properties = properties;
        this.routeRegistry = routeRegistry;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        loadRoutes();
    }

    public synchronized void loadRoutes() {
        if (properties == null || properties.getRouting() == null) {
            return;
        }

        List<RouteDefinition> configuredRoutes = properties.getRouting().getRoutes();
        if (configuredRoutes != null) {
            validateRoutes(configuredRoutes);
            routeRegistry.setRoutes(configuredRoutes);
        }
    }

    private void validateRoutes(List<RouteDefinition> routes) {
        for (RouteDefinition route : routes) {
            if (route.getId() == null || route.getId().isBlank()) {
                throw new ConfigurationException("Route definition must have a non-empty 'id'");
            }
            if (route.getPath() == null || route.getPath().isBlank()) {
                throw new ConfigurationException(String.format("Route '%s' must have a non-empty 'path'", route.getId()));
            }
        }
    }
}
