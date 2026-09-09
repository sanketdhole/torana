package com.phaselume.torana.protocol.websocket;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for the WebSocket MCP protocol adapter.
 */
@Data
@ConfigurationProperties(prefix = "torana.protocols.websocket")
public class WebSocketProtocolProperties {

    /**
     * Whether the WebSocket MCP protocol adapter is enabled.
     */
    private boolean enabled = true;

    /**
     * Endpoint path for WebSocket MCP connections (default: /ws/mcp).
     */
    private String path = "/ws/mcp";

    /**
     * Maximum frame size in bytes (default: 64KB).
     */
    private int maxFrameSize = 65536;

    /**
     * Idle connection timeout duration before auto-closing.
     */
    private Duration idleTimeout = Duration.ofSeconds(300);

    /**
     * Maximum concurrent WebSocket sessions per authenticated user.
     */
    private int maxSessionsPerUser = 5;

    /**
     * Frame format mode: true for text frames (JSON string), false for binary frames.
     */
    private boolean textMode = true;
}
