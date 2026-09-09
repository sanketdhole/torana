package com.phaselume.torana.protocol.mcp.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcError;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcRequest;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcResponse;
import com.phaselume.torana.protocol.mcp.model.McpResourceDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles MCP resources/list, resources/read, and resources/subscribe.
 */
public class McpResourcesHandler {

    private final Map<String, McpResourceDefinition> registeredResources = new ConcurrentHashMap<>();

    public void registerResource(McpResourceDefinition resource) {
        if (resource != null && resource.getUri() != null) {
            registeredResources.put(resource.getUri(), resource);
        }
    }

    public void unregisterResource(String uri) {
        if (uri != null) {
            registeredResources.remove(uri);
        }
    }

    /**
     * Handles resources/list returning all registered resources.
     */
    public McpJsonRpcResponse handleResourcesList(McpJsonRpcRequest request) {
        List<McpResourceDefinition> resources = new ArrayList<>(registeredResources.values());
        return McpJsonRpcResponse.success(request.getId(), Map.of("resources", resources));
    }

    /**
     * Validates a resources/read request.
     */
    public McpJsonRpcError validateResourceRead(McpJsonRpcRequest request) {
        JsonNode params = request.getParams();
        if (params == null || !params.has("uri")) {
            return McpJsonRpcError.invalidParams("Missing required 'uri' parameter for resources/read");
        }
        return null;
    }

    public Map<String, McpResourceDefinition> getRegisteredResources() {
        return registeredResources;
    }
}
