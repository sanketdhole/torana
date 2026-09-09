package com.phaselume.torana.routing.matcher;

import com.phaselume.torana.core.config.RouteDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RouteMatcherTest {

    private RouteMatcher routeMatcher;

    @BeforeEach
    void setUp() {
        routeMatcher = new RouteMatcher();
    }

    @Test
    void testExactPathMatch() {
        RouteDefinition route = RouteDefinition.builder()
                .id("exact-health")
                .path("/health")
                .methods(Set.of(HttpMethod.GET))
                .build();

        Optional<RouteDefinition> match = routeMatcher.match(
                List.of(route),
                "/health",
                HttpMethod.GET,
                new HttpHeaders(),
                "http"
        );

        assertTrue(match.isPresent());
        assertEquals("exact-health", match.get().getId());
    }

    @Test
    void testMethodMismatchFails() {
        RouteDefinition route = RouteDefinition.builder()
                .id("post-only")
                .path("/api/data")
                .methods(Set.of(HttpMethod.POST))
                .build();

        Optional<RouteDefinition> match = routeMatcher.match(
                List.of(route),
                "/api/data",
                HttpMethod.GET,
                new HttpHeaders(),
                "http"
        );

        assertFalse(match.isPresent());
    }

    @Test
    void testHeaderPredicateMatching() {
        RouteDefinition route = RouteDefinition.builder()
                .id("openai-route")
                .path("/v1/chat/completions")
                .headers(Map.of("X-Agent-Type", "openai"))
                .build();

        HttpHeaders matchingHeaders = new HttpHeaders();
        matchingHeaders.set("X-Agent-Type", "openai");

        Optional<RouteDefinition> match = routeMatcher.match(
                List.of(route),
                "/v1/chat/completions",
                HttpMethod.POST,
                matchingHeaders,
                "http"
        );
        assertTrue(match.isPresent());

        HttpHeaders nonMatchingHeaders = new HttpHeaders();
        nonMatchingHeaders.set("X-Agent-Type", "claude");

        Optional<RouteDefinition> mismatch = routeMatcher.match(
                List.of(route),
                "/v1/chat/completions",
                HttpMethod.POST,
                nonMatchingHeaders,
                "http"
        );
        assertFalse(mismatch.isPresent());
    }

    @Test
    void testPriorityExactOverPrefixOverWildcard() {
        RouteDefinition wildcard = RouteDefinition.builder()
                .id("wildcard-catchall")
                .path("/**")
                .build();

        RouteDefinition prefix = RouteDefinition.builder()
                .id("api-prefix")
                .path("/api/v1/**")
                .build();

        RouteDefinition exact = RouteDefinition.builder()
                .id("chat-exact")
                .path("/api/v1/chat")
                .build();

        // Pass in reverse order to test sorting
        List<RouteDefinition> routes = List.of(wildcard, prefix, exact);

        // 1. Should match exact
        Optional<RouteDefinition> matchExact = routeMatcher.match(
                routes,
                "/api/v1/chat",
                HttpMethod.POST,
                new HttpHeaders(),
                "http"
        );
        assertTrue(matchExact.isPresent());
        assertEquals("chat-exact", matchExact.get().getId());

        // 2. Should match prefix
        Optional<RouteDefinition> matchPrefix = routeMatcher.match(
                routes,
                "/api/v1/other",
                HttpMethod.POST,
                new HttpHeaders(),
                "http"
        );
        assertTrue(matchPrefix.isPresent());
        assertEquals("api-prefix", matchPrefix.get().getId());

        // 3. Should match catchall
        Optional<RouteDefinition> matchWildcard = routeMatcher.match(
                routes,
                "/unknown/resource",
                HttpMethod.POST,
                new HttpHeaders(),
                "http"
        );
        assertTrue(matchWildcard.isPresent());
        assertEquals("wildcard-catchall", matchWildcard.get().getId());
    }

    @Test
    void testPathVariablesExtraction() {
        PathPatternMatcher matcher = new PathPatternMatcher();
        Map<String, String> vars = matcher.extractPathVariables("/users/{userId}/docs/{docId}", "/users/alice/docs/doc123");

        assertEquals("alice", vars.get("userId"));
        assertEquals("doc123", vars.get("docId"));
    }
}
