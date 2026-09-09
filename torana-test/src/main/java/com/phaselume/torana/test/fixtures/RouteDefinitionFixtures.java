package com.phaselume.torana.test.fixtures;

import com.phaselume.torana.core.config.RouteDefinition;
import org.springframework.http.HttpMethod;

import java.time.Duration;
import java.util.Set;

/**
 * Pre-configured RouteDefinition fixtures for testing.
 */
public final class RouteDefinitionFixtures {

    private RouteDefinitionFixtures() {}

    public static RouteDefinition mcpRoute() {
        return RouteDefinition.builder()
                .id("mcp-default-route")
                .path("/mcp/v1")
                .protocols(Set.of("mcp"))
                .methods(Set.of(HttpMethod.POST))
                .pipelineRef("mcp-pipeline")
                .timeout(Duration.ofSeconds(30))
                .build();
    }

    public static RouteDefinition llmProxyRoute() {
        return RouteDefinition.builder()
                .id("llm-proxy-route")
                .path("/v1/chat/completions")
                .protocols(Set.of("http", "websocket"))
                .methods(Set.of(HttpMethod.POST))
                .pipelineRef("llm-chat-pipeline")
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    public static RouteDefinition restRoute(String path) {
        return RouteDefinition.builder()
                .id("rest-route-" + path.replace("/", "-"))
                .path(path)
                .protocols(Set.of("http"))
                .methods(Set.of(HttpMethod.GET, HttpMethod.POST))
                .timeout(Duration.ofSeconds(10))
                .build();
    }

    public static RouteDefinition securedRoute(String path, String authRef) {
        return RouteDefinition.builder()
                .id("secured-route-" + path.replace("/", "-"))
                .path(path)
                .authRef(authRef)
                .protocols(Set.of("http", "mcp"))
                .methods(Set.of(HttpMethod.POST))
                .timeout(Duration.ofSeconds(15))
                .build();
    }
}
