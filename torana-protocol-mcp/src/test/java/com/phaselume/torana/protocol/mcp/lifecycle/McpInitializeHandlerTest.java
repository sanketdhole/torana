package com.phaselume.torana.protocol.mcp.lifecycle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.protocol.mcp.codec.McpMessageCodec;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcRequest;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcResponse;
import com.phaselume.torana.protocol.mcp.model.McpServerInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpInitializeHandlerTest {

    private McpInitializeHandler handler;
    private McpSessionManager sessionManager;
    private McpMessageCodec codec;

    @BeforeEach
    void setUp() {
        codec = new McpMessageCodec();
        sessionManager = new McpSessionManager();
        handler = new McpInitializeHandler(sessionManager, codec, "1.0.0");
    }

    @Test
    void testInitializeHandshake() throws Exception {
        String clientJson = """
                {
                    "protocolVersion": "2024-11-05",
                    "clientInfo": {
                        "name": "claude-agent",
                        "version": "1.0.0"
                    },
                    "capabilities": {}
                }
                """;

        McpJsonRpcRequest request = McpJsonRpcRequest.builder()
                .id("init-1")
                .method("initialize")
                .params(new ObjectMapper().readTree(clientJson))
                .build();

        McpJsonRpcResponse response = handler.handleInitialize(request, "sess-100");

        assertNotNull(response);
        assertEquals("init-1", response.getId());
        assertTrue(response.getResult() instanceof McpServerInfo);

        McpServerInfo serverInfo = (McpServerInfo) response.getResult();
        assertEquals("torana-gateway", serverInfo.getServerInfo().getName());
        assertEquals("1.0.0", serverInfo.getServerInfo().getVersion());
        assertEquals(1, sessionManager.getActiveSessionCount());
    }
}
