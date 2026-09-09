package com.phaselume.torana.protocol.mcp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Deserialized MCP JSON-RPC 2.0 Request or Notification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpJsonRpcRequest {

    @Builder.Default
    private String jsonrpc = "2.0";

    private Object id; // String or Integer or null for notifications
    private String method;
    private JsonNode params;

    public boolean isNotification() {
        return id == null;
    }
}
