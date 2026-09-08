package com.phaselume.torana.core.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpMethod;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Definition of a routing rule mapping an incoming request to a pipeline or backend connector.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteDefinition {

    /**
     * Unique route identifier.
     */
    private String id;

    /**
     * Order of evaluation (lower runs first).
     */
    @Builder.Default
    private int order = 0;

    /**
     * Protocols enabled for this route (e.g. ["mcp", "http", "websocket", "grpc"]).
     */
    @Builder.Default
    private Set<String> protocols = new HashSet<>();

    /**
     * URL path pattern (e.g. "/mcp/v1", "/v1/chat/completions", "/api/data/**").
     */
    private String path;

    /**
     * Allowed HTTP methods. Empty means all.
     */
    @Builder.Default
    private Set<HttpMethod> methods = new HashSet<>();

    /**
     * Header match predicates.
     */
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();

    /**
     * Authentication profile reference or type required for this route.
     */
    private String authRef;

    /**
     * Rate limit policy reference for this route.
     */
    private String rateLimitRef;

    /**
     * Named pipeline to execute for this route.
     */
    private String pipelineRef;

    /**
     * Direct backend connector reference (if not using a multi-step pipeline).
     */
    private String backendRef;

    /**
     * Execution timeout.
     */
    @Builder.Default
    private Duration timeout = Duration.ofSeconds(30);

    /**
     * Additional arbitrary route metadata.
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
