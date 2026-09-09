package com.phaselume.torana.routing.matcher;

import com.phaselume.torana.core.config.RouteDefinition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Evaluates inbound requests against an ordered list of RouteDefinitions using priority matching rules:
 * 1. Exact path matches
 * 2. Longest prefix / parameterized patterns (longest pattern length first)
 * 3. Catch-all wildcards (/**)
 * 4. Explicit order value and declaration index
 */
public class RouteMatcher {

    private final CompositePredicateMatcher compositeMatcher;

    public RouteMatcher() {
        this(new CompositePredicateMatcher());
    }

    public RouteMatcher(CompositePredicateMatcher compositeMatcher) {
        this.compositeMatcher = compositeMatcher;
    }

    /**
     * Finds the first matching route from the provided route list.
     */
    public Optional<RouteDefinition> match(List<RouteDefinition> routes, String path, HttpMethod method, HttpHeaders headers, String protocol) {
        if (routes == null || routes.isEmpty() || path == null) {
            return Optional.empty();
        }

        List<RouteDefinition> sorted = sortRoutesByPriority(routes);
        for (RouteDefinition route : sorted) {
            if (compositeMatcher.matches(route, path, method, headers, protocol)) {
                return Optional.of(route);
            }
        }

        return Optional.empty();
    }

    /**
     * Sorts routes by priority.
     */
    public List<RouteDefinition> sortRoutesByPriority(List<RouteDefinition> routes) {
        List<RouteDefinition> sortedList = new ArrayList<>(routes);
        sortedList.sort(Comparator
                .<RouteDefinition>comparingInt(this::calculatePatternWeight)
                .thenComparingInt(RouteDefinition::getOrder));
        return sortedList;
    }

    /**
     * Lower score = higher priority.
     */
    private int calculatePatternWeight(RouteDefinition route) {
        String path = route.getPath();
        if (path == null) {
            return 10000;
        }

        if (path.equals("/**") || path.equals("/*")) {
            return 9000; // Catch-all lowest priority
        }

        boolean isExact = !path.contains("*") && !path.contains("{");
        if (isExact) {
            // Exact paths: highest priority, tie-break by longer length
            return 1000 - Math.min(path.length(), 500);
        }

        // Prefix and pattern paths: prioritized by length of prefix before wildcard
        int wildcardIndex = path.indexOf('*');
        int varIndex = path.indexOf('{');
        int firstSpecial = Math.min(
                wildcardIndex != -1 ? wildcardIndex : Integer.MAX_VALUE,
                varIndex != -1 ? varIndex : Integer.MAX_VALUE
        );

        int prefixLength = (firstSpecial != Integer.MAX_VALUE) ? firstSpecial : path.length();
        return 5000 - Math.min(prefixLength * 10 + path.length(), 3000);
    }

    public CompositePredicateMatcher getCompositeMatcher() {
        return compositeMatcher;
    }
}
