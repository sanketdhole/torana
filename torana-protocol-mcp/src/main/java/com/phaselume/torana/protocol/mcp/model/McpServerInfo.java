package com.phaselume.torana.protocol.mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Server information and capabilities returned during MCP initialize handshake.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpServerInfo {

    private String protocolVersion;
    private ServerImplementation serverInfo;
    private ServerCapabilities capabilities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ServerImplementation {
        private String name;
        private String version;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ServerCapabilities {
        private Map<String, Object> tools;
        private Map<String, Object> resources;
        private Map<String, Object> prompts;
        private Map<String, Object> logging;
    }
}
