package com.phaselume.torana.protocol.websocket.codec;

import com.phaselume.torana.protocol.mcp.codec.McpMessageCodec;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcRequest;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.nio.charset.StandardCharsets;

/**
 * Codec for translating between Spring WebSocketMessage and MCP JSON-RPC 2.0 objects.
 */
public class WebSocketMcpCodec {

    private final McpMessageCodec mcpCodec;
    private final boolean textMode;

    public WebSocketMcpCodec() {
        this(new McpMessageCodec(), true);
    }

    public WebSocketMcpCodec(boolean textMode) {
        this(new McpMessageCodec(), textMode);
    }

    public WebSocketMcpCodec(McpMessageCodec mcpCodec, boolean textMode) {
        this.mcpCodec = mcpCodec != null ? mcpCodec : new McpMessageCodec();
        this.textMode = textMode;
    }

    /**
     * Decodes an inbound WebSocketMessage into an McpJsonRpcRequest.
     */
    public McpJsonRpcRequest decode(WebSocketMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("WebSocketMessage cannot be null");
        }

        if (message.getType() == WebSocketMessage.Type.TEXT) {
            String payload = message.getPayloadAsText();
            return mcpCodec.decodeRequest(payload);
        } else if (message.getType() == WebSocketMessage.Type.BINARY) {
            DataBuffer buffer = message.getPayload();
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            return mcpCodec.decodeRequest(bytes);
        } else {
            throw new IllegalArgumentException("Unsupported WebSocket message type: " + message.getType());
        }
    }

    /**
     * Encodes an outbound response object (e.g. McpJsonRpcResponse) into a WebSocketMessage.
     */
    public WebSocketMessage encode(WebSocketSession session, Object response) {
        if (session == null || response == null) {
            throw new IllegalArgumentException("Session and response cannot be null");
        }

        if (textMode) {
            String json = mcpCodec.encodeToString(response);
            return session.textMessage(json);
        } else {
            byte[] bytes = mcpCodec.encode(response);
            return session.binaryMessage(factory -> factory.wrap(bytes));
        }
    }

    public McpMessageCodec getMcpCodec() {
        return mcpCodec;
    }

    public boolean isTextMode() {
        return textMode;
    }
}
