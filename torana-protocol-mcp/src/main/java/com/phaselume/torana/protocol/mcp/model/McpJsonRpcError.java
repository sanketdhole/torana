package com.phaselume.torana.protocol.mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Standard JSON-RPC 2.0 error object with MCP specific error codes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpJsonRpcError {

    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
    public static final int TOOL_NOT_FOUND = -32001;
    public static final int AUTHORIZATION_DENIED = -32002;
    public static final int RATE_LIMIT_EXCEEDED = -32003;
    public static final int BACKEND_UNAVAILABLE = -32004;

    private int code;
    private String message;
    private Map<String, Object> data;

    public static McpJsonRpcError parseError(String message) {
        return new McpJsonRpcError(PARSE_ERROR, message != null ? message : "Parse error", null);
    }

    public static McpJsonRpcError methodNotFound(String method) {
        return new McpJsonRpcError(METHOD_NOT_FOUND, "Method not found: " + method, null);
    }

    public static McpJsonRpcError invalidParams(String message) {
        return new McpJsonRpcError(INVALID_PARAMS, message != null ? message : "Invalid params", null);
    }

    public static McpJsonRpcError internalError(String message) {
        return new McpJsonRpcError(INTERNAL_ERROR, message != null ? message : "Internal error", null);
    }

    public static McpJsonRpcError toolNotFound(String toolName) {
        return new McpJsonRpcError(TOOL_NOT_FOUND, "Tool not found: " + toolName, null);
    }

    public static McpJsonRpcError invalidRequest(String string) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'invalidRequest'");
    }
}
