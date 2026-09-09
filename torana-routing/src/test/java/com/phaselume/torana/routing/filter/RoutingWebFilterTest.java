package com.phaselume.torana.routing.filter;

import com.phaselume.torana.core.config.RouteDefinition;
import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.routing.registry.RouteRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoutingWebFilterTest {

    @Test
    void testFilterPopulatesContextOnMatch() {
        RouteRegistry registry = new RouteRegistry();
        RouteDefinition route = RouteDefinition.builder()
                .id("mcp-route")
                .path("/mcp/v1")
                .pipelineRef("mcp-pipeline")
                .timeout(Duration.ofSeconds(45))
                .build();
        registry.setRoutes(List.of(route));

        RoutingWebFilter filter = new RoutingWebFilter(registry);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/mcp/v1").build());

        WebFilterChain chain = ex -> {
            AgentContext ctx = ex.getAttribute(RoutingWebFilter.CONTEXT_ATTRIBUTE);
            assertNotNull(ctx);
            assertEquals("mcp-route", ctx.getMatchedRoute().getId());
            assertEquals("mcp-pipeline", ctx.getAttribute("torana.pipeline.ref"));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    void testFilterReturns404WhenNoRouteMatches() {
        RouteRegistry registry = new RouteRegistry();
        registry.setRoutes(List.of(RouteDefinition.builder().id("r1").path("/api/known").build()));

        RoutingWebFilter filter = new RoutingWebFilter(registry);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/unknown").build());

        WebFilterChain chain = ex -> Mono.error(new AssertionError("Chain should not be called on 404"));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
    }

    @Test
    void testActuatorEndpointsBypassed() {
        RouteRegistry registry = new RouteRegistry();
        RoutingWebFilter filter = new RoutingWebFilter(registry);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/health").build());

        WebFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertNull(exchange.getResponse().getStatusCode()); // Not modified to 404
    }
}
