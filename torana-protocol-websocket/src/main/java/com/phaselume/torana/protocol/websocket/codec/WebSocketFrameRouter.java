package com.phaselume.torana.protocol.websocket.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.protocol.mcp.codec.McpToAgentRequestMapper;
import com.phaselume.torana.protocol.mcp.handler.McpRequestDispatcher;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcError;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcRequest;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcResponse;
import com.phaselume.torana.protocol.websocket.session.WebSocketSessionState;

import java.util.Map;

/**
 * Routes inbound MCP frames to local control handlers, subscription managers, or the execution pipeline.
 */
public class WebSocketFrameRouter {

    private final McpRequestDispatcher dispatcher;
    private final McpToAgentRequestMapper requestMapper;

    public WebSocketFrameRouter(McpRequestDispatcher dispatcher) {
        this(dispatcher, new McpToAgentRequestMapper(null));
    }

    public WebSocketFrameRouter(McpRequestDispatcher dispatcher, McpToAgentRequestMapper requestMapper) {
        this.dispatcher = dispatcher;
        this.requestMapper = requestMapper != null ? requestMapper : new McpToAgentRequestMapper(null);
    }

    /**
     * Attempts to resolve the request locally. Returns an McpJsonRpcResponse if resolved, or null if execution pipeline is required.
     */
    public McpJsonRpcResponse routeControl(McpJsonRpcRequest request, WebSocketSessionState sessionState) {
        if (request == null || request.getMethod() == null) {
            return McpJsonRpcResponse.error(
                    request != null ? request.getId() : null,
                    McpJsonRpcError.invalidRequest("Method is missing")
            );
        }

        String method = request.getMethod();

        // Handle WebSocket-specific resource subscriptions
        if ("resources/subscribe".equals(method)) {
            JsonNode params = request.getParams();
            if (params != null && params.has("uri")) {
                String uri = params.get("uri").asText();
                if (sessionState != null) {
                    sessionState.addSubscription(uri);
                }
                return McpJsonRpcResponse.success(request.getId(), Map.of());
            } else {
                return McpJsonRpcResponse.error(request.getId(), McpJsonRpcError.invalidParams("Missing 'uri' in params"));
            }
        }

        if ("resources/unsubscribe".equals(method)) {
            JsonNode params = request.getParams();
            if (params != null && params.has("uri")) {
                String uri = params.get("uri").asText();
                if (sessionState != null) {
                    sessionState.removeSubscription(uri);
                }
                return McpJsonRpcResponse.success(request.getId(), Map.of());
            } else {
                return McpJsonRpcResponse.error(request.getId(), McpJsonRpcError.invalidParams("Missing 'uri' in params"));
            }
        }

        // Delegate to MCP request dispatcher for standard control methods (init, ping, list tools/resources/prompts)
        String sessionId = sessionState != null ? sessionState.getSessionId() : null;
        return dispatcher.dispatchControl(request, sessionId);
    }

    /**
     * Validates execution requests before delegating to the backend pipeline.
     */
    public McpJsonRpcError validateExecution(McpJsonRpcRequest request) {
        return dispatcher.validateExecutionRequest(request);
    }

    /**
     * Maps an MCP execution request to an AgentRequest.
     */
    public AgentRequest mapToAgentRequest(McpJsonRpcRequest request, WebSocketSessionState sessionState) {
        AgentRequest agentReq = requestMapper.map(request, null, null);
        if (sessionState != null && sessionState.getTenantId() != null) {
            agentReq = agentReq.toBuilder()
                    .attributes(Map.of("torana.tenant.id", sessionState.getTenantId()))
                    .build();
        }
        return agentReq;
    }

    public McpRequestDispatcher getDispatcher() {
        return dispatcher;
    }
}
