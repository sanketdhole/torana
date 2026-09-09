package com.phaselume.torana.protocol.mcp.handler;

import com.phaselume.torana.protocol.mcp.lifecycle.McpInitializeHandler;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcError;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcRequest;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcResponse;

import java.util.Map;

/**
 * Central dispatcher for MCP JSON-RPC 2.0 requests.
 * Handles protocol-level control messages and validates execution calls.
 */
public class McpRequestDispatcher {

    private final McpInitializeHandler initializeHandler;
    private final McpToolsHandler toolsHandler;
    private final McpResourcesHandler resourcesHandler;
    private final McpPromptsHandler promptsHandler;

    public McpRequestDispatcher(
            McpInitializeHandler initializeHandler,
            McpToolsHandler toolsHandler,
            McpResourcesHandler resourcesHandler,
            McpPromptsHandler promptsHandler) {
        this.initializeHandler = initializeHandler;
        this.toolsHandler = toolsHandler != null ? toolsHandler : new McpToolsHandler();
        this.resourcesHandler = resourcesHandler != null ? resourcesHandler : new McpResourcesHandler();
        this.promptsHandler = promptsHandler != null ? promptsHandler : new McpPromptsHandler();
    }

    /**
     * Checks if this request is a local protocol control request (e.g. initialize, tools/list) that can be
     * resolved immediately without pipeline dispatch.
     *
     * @return An McpJsonRpcResponse if resolved directly, or null if it must proceed to pipeline execution.
     */
    public McpJsonRpcResponse dispatchControl(McpJsonRpcRequest request, String sessionId) {
        if (request == null || request.getMethod() == null) {
            return McpJsonRpcResponse.error(
                    request != null ? request.getId() : null,
                    McpJsonRpcError.invalidRequest("Method is missing")
            );
        }

        String method = request.getMethod();

        return switch (method) {
            case "initialize" -> initializeHandler.handleInitialize(request, sessionId);
            case "initialized", "notifications/initialized" -> McpJsonRpcResponse.success(request.getId(), Map.of());
            case "ping" -> McpJsonRpcResponse.success(request.getId(), Map.of());
            case "tools/list" -> toolsHandler.handleToolsList(request);
            case "resources/list" -> resourcesHandler.handleResourcesList(request);
            case "prompts/list" -> promptsHandler.handlePromptsList(request);
            default -> null; // Needs pipeline execution
        };
    }

    /**
     * Validates execution requests before sending to pipeline.
     */
    public McpJsonRpcError validateExecutionRequest(McpJsonRpcRequest request) {
        if (request == null || request.getMethod() == null) {
            return McpJsonRpcError.invalidRequest("Method is missing");
        }

        String method = request.getMethod();
        return switch (method) {
            case "tools/call" -> toolsHandler.validateToolCall(request);
            case "resources/read" -> resourcesHandler.validateResourceRead(request);
            case "prompts/get" -> promptsHandler.validatePromptGet(request);
            default -> McpJsonRpcError.methodNotFound(method);
        };
    }

    public McpToolsHandler getToolsHandler() {
        return toolsHandler;
    }

    public McpResourcesHandler getResourcesHandler() {
        return resourcesHandler;
    }

    public McpPromptsHandler getPromptsHandler() {
        return promptsHandler;
    }

    public McpInitializeHandler getInitializeHandler() {
        return initializeHandler;
    }
}
