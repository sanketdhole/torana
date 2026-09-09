package com.phaselume.torana.routing.matcher;

import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance path pattern matcher wrapping Spring's PathPatternParser.
 * Supports exact paths, Ant-style wildcards (/api/v1/**), and URI path variables (/chat/{id}).
 */
public class PathPatternMatcher {

    private static final PathPatternParser PARSER = new PathPatternParser();
    private final Map<String, PathPattern> compiledPatterns = new ConcurrentHashMap<>();

    /**
     * Checks if the given request path matches the route path pattern.
     */
    public boolean matches(String pattern, String requestPath) {
        if (pattern == null || requestPath == null) {
            return false;
        }
        PathPattern compiled = getCompiledPattern(pattern);
        PathContainer container = PathContainer.parsePath(requestPath);
        return compiled.matches(container);
    }

    /**
     * Extracts URI path variables if the pattern matches the path.
     */
    public Map<String, String> extractPathVariables(String pattern, String requestPath) {
        if (pattern == null || requestPath == null) {
            return Collections.emptyMap();
        }
        PathPattern compiled = getCompiledPattern(pattern);
        PathContainer container = PathContainer.parsePath(requestPath);
        PathPattern.PathMatchInfo matchInfo = compiled.matchAndExtract(container);
        return matchInfo != null ? matchInfo.getUriVariables() : Collections.emptyMap();
    }

    public PathPattern getCompiledPattern(String pattern) {
        return compiledPatterns.computeIfAbsent(pattern, PARSER::parse);
    }
}
