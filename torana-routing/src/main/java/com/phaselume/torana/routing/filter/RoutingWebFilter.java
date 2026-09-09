package com.phaselume.torana.routing.filter;

import com.phaselume.torana.core.exception.RouteNotFoundException;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.routing.registry.RouteRegistry;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Spring WebFilter that evaluates incoming requests against the RouteRegistry,
 * attaches matched RouteDefinition to AgentContext, and returns structured 404 for unmatched routes.
 */
public class RoutingWebFilter implements WebFilter, Ordered {

    public static final String CONTEXT_ATTRIBUTE = "torana.context";
    public static final String MATCHED_ROUTE_ATTRIBUTE = "torana.matchedRoute";

    private final RouteRegistry routeRegistry;
    private final RouteContextPopulator contextPopulator;
    private int order = 0; // Configurable filter order

    public RoutingWebFilter(RouteRegistry routeRegistry) {
        this(routeRegistry, new RouteContextPopulator());
    }

    public RoutingWebFilter(RouteRegistry routeRegistry, RouteContextPopulator contextPopulator) {
        this.routeRegistry = routeRegistry;
        this.contextPopulator = contextPopulator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();

        // Bypass Spring Boot actuator / health endpoints
        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        return routeRegistry.match(exchange)
                .switchIfEmpty(Mono.defer(() -> handleRouteNotFound(exchange, path)))
                .flatMap(route -> {
                    AgentContext currentContext = exchange.getAttribute(CONTEXT_ATTRIBUTE);
                    if (currentContext == null) {
                        AgentRequest agentRequest = AgentRequest.builder()
                                .id(exchange.getRequest().getId())
                                .method(exchange.getRequest().getMethod())
                                .path(path)
                                .headers(exchange.getRequest().getHeaders())
                                .queryParams(exchange.getRequest().getQueryParams())
                                .rawExchange(exchange)
                                .build();

                        currentContext = AgentContext.builder()
                                .id(exchange.getRequest().getId())
                                .request(agentRequest)
                                .build();
                    }
                    currentContext = contextPopulator.populate(currentContext, route);

                    exchange.getAttributes().put(MATCHED_ROUTE_ATTRIBUTE, route);
                    exchange.getAttributes().put(CONTEXT_ATTRIBUTE, currentContext);
                    return chain.filter(exchange);
                });
    }

    private <T> Mono<T> handleRouteNotFound(ServerWebExchange exchange, String path) {
        String protocol = exchange.getAttributeOrDefault("torana.protocol", "http");
        RouteNotFoundException ex = new RouteNotFoundException(path, protocol);

        exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String errorJson = String.format("""
                {"error":"ROUTE_NOT_FOUND","message":"%s","status":404}
                """, ex.getMessage().replace("\"", "\\\""));

        byte[] bytes = errorJson.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)))
                .then(Mono.empty());
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
