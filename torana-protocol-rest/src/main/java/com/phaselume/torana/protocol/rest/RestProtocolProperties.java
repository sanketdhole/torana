package com.phaselume.torana.protocol.rest;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration properties for the HTTP/REST reverse proxy protocol adapter.
 */
@Data
@ConfigurationProperties(prefix = "torana.protocols.rest")
public class RestProtocolProperties {

    /**
     * Whether the REST protocol adapter is enabled.
     */
    private boolean enabled = true;

    /**
     * Base path pattern for the REST adapter (e.g. "/api/v1/**" or "/rest/**").
     */
    private String path = "/api/v1/**";

    /**
     * Whether to automatically strip the base path prefix before forwarding to backend.
     */
    private boolean stripPathPrefix = false;

    /**
     * Default request timeout.
     */
    private Duration timeout = Duration.ofSeconds(30);

    /**
     * Maximum request body size allowed (in bytes). Default 10MB.
     */
    private long maxRequestBodySize = 10 * 1024 * 1024;

    /**
     * Path rewrite rules applied to incoming requests.
     */
    private List<PathRewriteConfig> pathRewrites = new ArrayList<>();

    /**
     * Header forwarding policies.
     */
    private HeaderPolicyConfig headers = new HeaderPolicyConfig();

    @Data
    public static class PathRewriteConfig {
        private String from;
        private String to;
        private boolean regex = false;
    }

    @Data
    public static class HeaderPolicyConfig {
        private Set<String> allowList = new HashSet<>();
        private Set<String> blockList = new HashSet<>();
        private boolean forwardTraceHeaders = true;
    }
}
