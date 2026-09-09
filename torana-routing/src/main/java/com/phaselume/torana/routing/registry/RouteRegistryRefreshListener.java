package com.phaselume.torana.routing.registry;

import com.phaselume.torana.core.config.RouteDefinition;
import com.phaselume.torana.core.config.ToranaProperties;
import com.phaselume.torana.routing.reload.RouteChangeEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

import java.util.List;

/**
 * Listens for Spring Cloud refresh events (e.g. from Config Server webhook or /actuator/refresh)
 * and atomically reloads routes into RouteRegistry.
 */
public class RouteRegistryRefreshListener {

    private final ToranaProperties properties;
    private final RouteRegistry routeRegistry;
    private final ApplicationEventPublisher eventPublisher;

    public RouteRegistryRefreshListener(ToranaProperties properties,
                                        RouteRegistry routeRegistry,
                                        ApplicationEventPublisher eventPublisher) {
        this.properties = properties;
        this.routeRegistry = routeRegistry;
        this.eventPublisher = eventPublisher;
    }

    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onRefresh(RefreshScopeRefreshedEvent event) {
        if (properties == null || properties.getRouting() == null) {
            return;
        }

        List<RouteDefinition> oldRoutes = routeRegistry.getRoutes();
        List<RouteDefinition> newRoutes = properties.getRouting().getRoutes();

        routeRegistry.setRoutes(newRoutes);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new RouteChangeEvent(this, oldRoutes, newRoutes));
        }
    }
}
