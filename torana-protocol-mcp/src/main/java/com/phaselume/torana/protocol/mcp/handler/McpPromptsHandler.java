package com.phaselume.torana.protocol.mcp.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcError;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcRequest;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcResponse;
import com.phaselume.torana.protocol.mcp.model.McpPromptDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles MCP prompts/list and prompts/get.
 */
public class McpPromptsHandler {

    private final Map<String, McpPromptDefinition> registeredPrompts = new ConcurrentHashMap<>();

    public void registerPrompt(McpPromptDefinition prompt) {
        if (prompt != null && prompt.getName() != null) {
            registeredPrompts.put(prompt.getName(), prompt);
        }
    }

    public void unregisterPrompt(String name) {
        if (name != null) {
            registeredPrompts.remove(name);
        }
    }

    /**
     * Handles prompts/list returning all registered prompt templates.
     */
    public McpJsonRpcResponse handlePromptsList(McpJsonRpcRequest request) {
        List<McpPromptDefinition> prompts = new ArrayList<>(registeredPrompts.values());
        return McpJsonRpcResponse.success(request.getId(), Map.of("prompts", prompts));
    }

    /**
     * Validates a prompts/get request.
     */
    public McpJsonRpcError validatePromptGet(McpJsonRpcRequest request) {
        JsonNode params = request.getParams();
        if (params == null || !params.has("name")) {
            return McpJsonRpcError.invalidParams("Missing required 'name' parameter for prompts/get");
        }
        return null;
    }

    public Map<String, McpPromptDefinition> getRegisteredPrompts() {
        return registeredPrompts;
    }
}
