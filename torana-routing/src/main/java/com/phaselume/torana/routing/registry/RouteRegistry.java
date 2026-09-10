package com.phaselume.torana.routing.registry;

import com.phaselume.torana.core.config.RouteDefinition;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.routing.matcher.RouteMatcher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe registry holding active RouteDefinitions with support for atomic hot-reload.
 */
public class RouteRegistry {

    private final List<RouteDefinition> routes = new CopyOnWriteArrayList<>();
    private final RouteMatcher routeMatcher;

    public RouteRegistry() {
        this(new RouteMatcher());
    }

    public RouteRegistry(RouteMatcher routeMatcher) {
        this.routeMatcher = routeMatcher;
    }

    /**
     * Atomically replaces the current routes with a new pre-sorted list.
     */
    public void setRoutes(List<RouteDefinition> newRoutes) {
        this.routes.clear();
        if (newRoutes != null && !newRoutes.isEmpty()) {
            List<RouteDefinition> sorted = routeMatcher.sortRoutesByPriority(newRoutes);
            this.routes.addAll(sorted);
        }
    }

    public void addRoute(RouteDefinition route) {
        if (route != null) {
            java.util.List<RouteDefinition> updated = new java.util.ArrayList<>(this.routes);
            updated.add(route);
            java.util.List<RouteDefinition> sorted = routeMatcher.sortRoutesByPriority(updated);
            this.routes.clear();
            this.routes.addAll(sorted);
        }
    }

    public List<RouteDefinition> getRoutes() {
        return Collections.unmodifiableList(routes);
    }

    public int size() {
        return routes.size();
    }

    /**
     * Matches an incoming ServerWebExchange against registered routes reactively.
     */
    public Mono<RouteDefinition> match(ServerWebExchange exchange) {
        if (exchange == null) {
            return Mono.empty();
        }

        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        HttpMethod method = exchange.getRequest().getMethod();
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String protocol = exchange.getAttributeOrDefault("torana.protocol", "http");

        return match(path, method, headers, protocol);
    }

    /**
     * Matches an AgentRequest against registered routes.
     */
    public Mono<RouteDefinition> match(AgentRequest request) {
        if (request == null) {
            return Mono.empty();
        }
        return match(request.getPath(), request.getMethod(), request.getHeaders(), request.getProtocol());
    }

    public Mono<RouteDefinition> match(String path, HttpMethod method, HttpHeaders headers, String protocol) {
        Optional<RouteDefinition> matched = routeMatcher.match(routes, path, method, headers, protocol);
        return Mono.justOrEmpty(matched);
    }

    public RouteMatcher getRouteMatcher() {
        return routeMatcher;
    }
}
