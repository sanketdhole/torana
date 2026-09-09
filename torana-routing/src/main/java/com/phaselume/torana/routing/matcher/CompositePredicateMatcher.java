package com.phaselume.torana.routing.matcher;

import com.phaselume.torana.core.config.RouteDefinition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * Evaluates composite matching criteria (path, method, headers, protocol) for a RouteDefinition.
 */
public class CompositePredicateMatcher {

    private final PathPatternMatcher pathMatcher;
    private final MethodPredicateMatcher methodMatcher;
    private final HeaderPredicateMatcher headerMatcher;

    public CompositePredicateMatcher() {
        this.pathMatcher = new PathPatternMatcher();
        this.methodMatcher = new MethodPredicateMatcher();
        this.headerMatcher = new HeaderPredicateMatcher();
    }

    public CompositePredicateMatcher(PathPatternMatcher pathMatcher,
                                     MethodPredicateMatcher methodMatcher,
                                     HeaderPredicateMatcher headerMatcher) {
        this.pathMatcher = pathMatcher;
        this.methodMatcher = methodMatcher;
        this.headerMatcher = headerMatcher;
    }

    /**
     * Checks if the request matches the given route definition.
     */
    public boolean matches(RouteDefinition route, String path, HttpMethod method, HttpHeaders headers, String protocol) {
        if (route == null) {
            return false;
        }

        // Protocol matching (if route restricts protocols)
        if (protocol != null && route.getProtocols() != null && !route.getProtocols().isEmpty()) {
            if (!route.getProtocols().contains(protocol.toLowerCase())) {
                return false;
            }
        }

        // Path matching
        if (route.getPath() != null && !pathMatcher.matches(route.getPath(), path)) {
            return false;
        }

        // Method matching
        if (!methodMatcher.matches(route.getMethods(), method)) {
            return false;
        }

        // Header matching
        return headerMatcher.matches(route.getHeaders(), headers);
    }

    public PathPatternMatcher getPathMatcher() {
        return pathMatcher;
    }
}
