package com.phaselume.torana.protocol.mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP JSON-RPC 2.0 Response object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpJsonRpcResponse {

    @Builder.Default
    private String jsonrpc = "2.0";

    private Object id;
    private Object result;
    private McpJsonRpcError error;

    public static McpJsonRpcResponse success(Object id, Object result) {
        return McpJsonRpcResponse.builder()
                .id(id)
                .result(result)
                .build();
    }

    public static McpJsonRpcResponse error(Object id, McpJsonRpcError error) {
        return McpJsonRpcResponse.builder()
                .id(id)
                .error(error)
                .build();
    }
}
