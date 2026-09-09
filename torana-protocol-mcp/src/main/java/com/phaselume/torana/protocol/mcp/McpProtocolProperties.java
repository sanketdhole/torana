package com.phaselume.torana.protocol.mcp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for the MCP protocol adapter.
 */
@Data
@ConfigurationProperties(prefix = "torana.protocols.mcp")
public class McpProtocolProperties {

    /**
     * Whether the MCP protocol adapter is enabled.
     */
    private boolean enabled = true;

    /**
     * Endpoint path for MCP JSON-RPC over HTTP/SSE.
     */
    private String path = "/mcp/v1";

    /**
     * Heartbeat interval for Server-Sent Events (SSE).
     */
    private Duration sseHeartbeatInterval = Duration.ofSeconds(30);

    /**
     * Maximum request body size (in bytes). Default 10MB.
     */
    private long maxRequestSize = 10 * 1024 * 1024;

    /**
     * Session time-to-live.
     */
    private Duration sessionTtl = Duration.ofSeconds(300);

    /**
     * Gateway server version string.
     */
    private String serverVersion = "0.1.0-SNAPSHOT";
}
