package com.phaselume.torana.protocol.mcp.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcError;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcRequest;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcResponse;
import com.phaselume.torana.protocol.mcp.model.McpToolDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class McpToolsHandlerTest {

    private McpToolsHandler toolsHandler;

    @BeforeEach
    void setUp() {
        toolsHandler = new McpToolsHandler();
        toolsHandler.registerTool(McpToolDefinition.builder()
                .name("db_query")
                .description("Run SQL query")
                .inputSchema(Map.of("type", "object"))
                .build());
    }

    @Test
    void testToolsList() {
        McpJsonRpcRequest request = McpJsonRpcRequest.builder()
                .id(10)
                .method("tools/list")
                .build();

        McpJsonRpcResponse response = toolsHandler.handleToolsList(request);

        assertNotNull(response);
        assertEquals(10, response.getId());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        @SuppressWarnings("unchecked")
        List<McpToolDefinition> tools = (List<McpToolDefinition>) result.get("tools");
        assertEquals(1, tools.size());
        assertEquals("db_query", tools.get(0).getName());
    }

    @Test
    void testValidateToolCallSuccess() throws Exception {
        McpJsonRpcRequest request = McpJsonRpcRequest.builder()
                .id(11)
                .method("tools/call")
                .params(new ObjectMapper().readTree("{\"name\": \"db_query\", \"arguments\": {\"sql\": \"SELECT 1\"}}"))
                .build();

        McpJsonRpcError error = toolsHandler.validateToolCall(request);
        assertNull(error);
    }

    @Test
    void testValidateToolCallNotFound() throws Exception {
        McpJsonRpcRequest request = McpJsonRpcRequest.builder()
                .id(12)
                .method("tools/call")
                .params(new ObjectMapper().readTree("{\"name\": \"non_existent_tool\"}"))
                .build();

        McpJsonRpcError error = toolsHandler.validateToolCall(request);
        assertNotNull(error);
        assertEquals(McpJsonRpcError.TOOL_NOT_FOUND, error.getCode());
    }
}
