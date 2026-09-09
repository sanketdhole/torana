package com.phaselume.torana.protocol.websocket.codec;

import com.phaselume.torana.protocol.mcp.model.McpJsonRpcRequest;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.adapter.NettyWebSocketSessionSupport;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSocketMcpCodecTest {

    @Test
    void testDecodeTextFrame() {
        WebSocketMcpCodec codec = new WebSocketMcpCodec(true);
        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();

        String json = "{\"jsonrpc\":\"2.0\",\"id\":\"req-1\",\"method\":\"tools/list\",\"params\":{}}";
        WebSocketMessage message = new WebSocketMessage(WebSocketMessage.Type.TEXT, factory.wrap(json.getBytes(StandardCharsets.UTF_8)));

        McpJsonRpcRequest request = codec.decode(message);

        assertNotNull(request);
        assertEquals("2.0", request.getJsonrpc());
        assertEquals("req-1", request.getId());
        assertEquals("tools/list", request.getMethod());
    }

    @Test
    void testDecodeBinaryFrame() {
        WebSocketMcpCodec codec = new WebSocketMcpCodec(false);
        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();

        String json = "{\"jsonrpc\":\"2.0\",\"id\":100,\"method\":\"ping\"}";
        WebSocketMessage message = new WebSocketMessage(WebSocketMessage.Type.BINARY, factory.wrap(json.getBytes(StandardCharsets.UTF_8)));

        McpJsonRpcRequest request = codec.decode(message);

        assertNotNull(request);
        assertEquals(100, request.getId());
        assertEquals("ping", request.getMethod());
    }

    @Test
    void testEncodeText() {
        WebSocketMcpCodec codec = new WebSocketMcpCodec(true);
        McpJsonRpcResponse response = McpJsonRpcResponse.success("req-1", Map.of("result", "ok"));

        String json = codec.getMcpCodec().encodeToString(response);
        assertTrue(json.contains("\"result\":\"ok\""));
        assertTrue(json.contains("\"jsonrpc\":\"2.0\""));
    }
}
