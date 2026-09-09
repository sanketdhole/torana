package com.phaselume.torana.protocol.mcp.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcError;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcRequest;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcResponse;
import com.phaselume.torana.protocol.mcp.model.McpToolDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles MCP tools/list and tools/call operations.
 */
public class McpToolsHandler {

    private final Map<String, McpToolDefinition> registeredTools = new ConcurrentHashMap<>();

    public void registerTool(McpToolDefinition tool) {
        if (tool != null && tool.getName() != null) {
            registeredTools.put(tool.getName(), tool);
        }
    }

    public void unregisterTool(String toolName) {
        if (toolName != null) {
            registeredTools.remove(toolName);
        }
    }

    /**
     * Handles tools/list returning all registered tools.
     */
    public McpJsonRpcResponse handleToolsList(McpJsonRpcRequest request) {
        List<McpToolDefinition> tools = new ArrayList<>(registeredTools.values());
        return McpJsonRpcResponse.success(request.getId(), Map.of("tools", tools));
    }

    /**
     * Validates a tools/call request.
     */
    public McpJsonRpcError validateToolCall(McpJsonRpcRequest request) {
        JsonNode params = request.getParams();
        if (params == null || !params.has("name")) {
            return McpJsonRpcError.invalidParams("Missing required 'name' parameter for tools/call");
        }

        String toolName = params.get("name").asText();
        if (!registeredTools.isEmpty() && !registeredTools.containsKey(toolName)) {
            return McpJsonRpcError.toolNotFound(toolName);
        }

        return null;
    }

    public Map<String, McpToolDefinition> getRegisteredTools() {
        return registeredTools;
    }
}
