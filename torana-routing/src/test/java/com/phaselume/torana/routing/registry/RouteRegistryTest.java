package com.phaselume.torana.routing.registry;

import com.phaselume.torana.core.config.RouteDefinition;
import com.phaselume.torana.core.config.ToranaProperties;
import com.phaselume.torana.routing.reload.RouteChangeEvent;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RouteRegistryTest {

    @Test
    void testRegistryMatchingAndAtomicUpdate() {
        RouteRegistry registry = new RouteRegistry();
        RouteDefinition route1 = RouteDefinition.builder().id("r1").path("/api/v1/test").build();
        registry.setRoutes(List.of(route1));

        assertEquals(1, registry.size());

        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/test").build());
        StepVerifier.create(registry.match(exchange))
                .expectNextMatches(r -> "r1".equals(r.getId()))
                .verifyComplete();

        // Update routes
        RouteDefinition route2 = RouteDefinition.builder().id("r2").path("/api/v2/new").build();
        registry.setRoutes(List.of(route2));

        assertEquals(1, registry.size());

        MockServerWebExchange exchange2 = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v2/new").build());
        StepVerifier.create(registry.match(exchange2))
                .expectNextMatches(r -> "r2".equals(r.getId()))
                .verifyComplete();
    }

    @Test
    void testRefreshListenerTriggersAtomicReloadAndEvent() {
        RouteRegistry registry = new RouteRegistry();
        RouteDefinition initialRoute = RouteDefinition.builder().id("init").path("/initial").build();
        registry.setRoutes(List.of(initialRoute));

        ToranaProperties props = new ToranaProperties();
        RouteDefinition updatedRoute = RouteDefinition.builder().id("updated").path("/updated").build();
        props.getRouting().setRoutes(List.of(updatedRoute));

        AtomicReference<RouteChangeEvent> publishedEvent = new AtomicReference<>();
        ApplicationEventPublisher publisher = event -> {
            if (event instanceof RouteChangeEvent rce) {
                publishedEvent.set(rce);
            }
        };

        RouteRegistryRefreshListener listener = new RouteRegistryRefreshListener(props, registry, publisher);
        listener.onRefresh(new RefreshScopeRefreshedEvent());

        assertEquals(1, registry.size());
        assertEquals("updated", registry.getRoutes().get(0).getId());
        assertNotNull(publishedEvent.get());
        assertEquals(1, publishedEvent.get().getPreviousRoutes().size());
        assertEquals(1, publishedEvent.get().getCurrentRoutes().size());
    }
}
