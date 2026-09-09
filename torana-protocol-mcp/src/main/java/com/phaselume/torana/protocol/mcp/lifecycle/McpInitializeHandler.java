package com.phaselume.torana.protocol.mcp.lifecycle;

import com.phaselume.torana.protocol.mcp.codec.McpMessageCodec;
import com.phaselume.torana.protocol.mcp.model.McpClientInfo;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcRequest;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcResponse;
import com.phaselume.torana.protocol.mcp.model.McpServerInfo;

import java.util.Map;
import java.util.UUID;

/**
 * Handles MCP initialize handshake requests and negotiates server capabilities.
 */
public class McpInitializeHandler {

    private final McpSessionManager sessionManager;
    private final McpMessageCodec codec;
    private final String serverVersion;

    public McpInitializeHandler(McpSessionManager sessionManager, McpMessageCodec codec, String serverVersion) {
        this.sessionManager = sessionManager != null ? sessionManager : new McpSessionManager();
        this.codec = codec != null ? codec : new McpMessageCodec();
        this.serverVersion = serverVersion != null ? serverVersion : "0.1.0-SNAPSHOT";
    }

    /**
     * Processes initialize request and registers a session.
     */
    public McpJsonRpcResponse handleInitialize(McpJsonRpcRequest request, String existingSessionId) {
        McpClientInfo clientInfo = null;
        if (request.getParams() != null) {
            clientInfo = codec.convertParams(request.getParams(), McpClientInfo.class);
        }

        String sessionId = (existingSessionId != null && !existingSessionId.isBlank())
                ? existingSessionId
                : UUID.randomUUID().toString();

        sessionManager.createSession(sessionId, clientInfo);

        McpServerInfo serverInfo = McpServerInfo.builder()
                .protocolVersion("2024-11-05")
                .serverInfo(McpServerInfo.ServerImplementation.builder()
                        .name("torana-gateway")
                        .version(serverVersion)
                        .build())
                .capabilities(McpServerInfo.ServerCapabilities.builder()
                        .tools(Map.of("listChanged", false))
                        .resources(Map.of("subscribe", true, "listChanged", false))
                        .prompts(Map.of("listChanged", false))
                        .logging(Map.of())
                        .build())
                .build();

        return McpJsonRpcResponse.success(request.getId(), serverInfo);
    }

    public McpSessionManager getSessionManager() {
        return sessionManager;
    }
}
