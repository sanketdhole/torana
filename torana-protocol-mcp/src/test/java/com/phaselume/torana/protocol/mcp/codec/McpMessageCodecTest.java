package com.phaselume.torana.protocol.mcp.codec;

import com.phaselume.torana.protocol.mcp.model.McpJsonRpcRequest;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcResponse;
import com.phaselume.torana.protocol.mcp.model.McpToolDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpMessageCodecTest {

    private McpMessageCodec codec;

    @BeforeEach
    void setUp() {
        codec = new McpMessageCodec();
    }

    @Test
    void testDecodeJsonRpcRequest() {
        String json = """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "method": "tools/call",
                    "params": {
                        "name": "calculate_tax",
                        "arguments": {
                            "amount": 100
                        }
                    }
                }
                """;

        McpJsonRpcRequest request = codec.decodeRequest(json);

        assertNotNull(request);
        assertEquals("2.0", request.getJsonrpc());
        assertEquals(1, request.getId());
        assertEquals("tools/call", request.getMethod());
        assertNotNull(request.getParams());
        assertEquals("calculate_tax", request.getParams().get("name").asText());
        assertFalse(request.isNotification());
    }

    @Test
    void testEncodeJsonRpcResponse() {
        McpToolDefinition tool = McpToolDefinition.builder()
                .name("fetch_weather")
                .description("Fetches weather forecast")
                .inputSchema(Map.of("type", "object"))
                .build();

        McpJsonRpcResponse response = McpJsonRpcResponse.success("req-42", Map.of("tools", new Object[]{tool}));
        String json = codec.encodeToString(response);

        assertTrue(json.contains("\"jsonrpc\":\"2.0\""));
        assertTrue(json.contains("\"id\":\"req-42\""));
        assertTrue(json.contains("\"fetch_weather\""));
    }
}
