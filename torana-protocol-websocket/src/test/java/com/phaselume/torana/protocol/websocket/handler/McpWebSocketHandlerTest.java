package com.phaselume.torana.protocol.websocket.handler;

import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.protocol.mcp.codec.McpMessageCodec;
import com.phaselume.torana.protocol.mcp.handler.McpPromptsHandler;
import com.phaselume.torana.protocol.mcp.handler.McpRequestDispatcher;
import com.phaselume.torana.protocol.mcp.handler.McpResourcesHandler;
import com.phaselume.torana.protocol.mcp.handler.McpToolsHandler;
import com.phaselume.torana.protocol.mcp.lifecycle.McpInitializeHandler;
import com.phaselume.torana.protocol.mcp.lifecycle.McpSessionManager;
import com.phaselume.torana.protocol.websocket.codec.WebSocketFrameRouter;
import com.phaselume.torana.protocol.websocket.codec.WebSocketMcpCodec;
import com.phaselume.torana.protocol.websocket.session.WebSocketSessionRegistry;
import com.phaselume.torana.protocol.websocket.session.WebSocketSessionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpWebSocketHandlerTest {

    private WebSocketMcpCodec codec;
    private WebSocketFrameRouter router;
    private McpWebSocketSessionHandler sessionHandler;
    private WebSocketSession mockSession;
    private WebSocketSessionState sessionState;

    @BeforeEach
    void setUp() {
        codec = new WebSocketMcpCodec(true);
        McpSessionManager sessionManager = new McpSessionManager();
        McpMessageCodec mcpCodec = new McpMessageCodec();
        McpInitializeHandler initHandler = new McpInitializeHandler(sessionManager, mcpCodec, "0.1.0");
        McpToolsHandler toolsHandler = new McpToolsHandler();
        McpResourcesHandler resourcesHandler = new McpResourcesHandler();
        McpPromptsHandler promptsHandler = new McpPromptsHandler();

        McpRequestDispatcher dispatcher = new McpRequestDispatcher(initHandler, toolsHandler, resourcesHandler, promptsHandler);
        router = new WebSocketFrameRouter(dispatcher);

        sessionHandler = new McpWebSocketSessionHandler(codec, router, req -> Mono.just(AgentResponse.buffered("{\"status\":\"success\"}".getBytes(StandardCharsets.UTF_8))));

        mockSession = mock(WebSocketSession.class);
        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
        when(mockSession.bufferFactory()).thenReturn(factory);
        when(mockSession.textMessage(org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation -> {
            String txt = invocation.getArgument(0);
            return new WebSocketMessage(WebSocketMessage.Type.TEXT, factory.wrap(txt.getBytes(StandardCharsets.UTF_8)));
        });

        sessionState = WebSocketSessionState.builder()
                .sessionId("ws-test-1")
                .userId("test-user")
                .build();
    }

    @Test
    void testProcessPingMessage() {
        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
        String json = "{\"jsonrpc\":\"2.0\",\"id\":\"ping-1\",\"method\":\"ping\"}";
        WebSocketMessage message = new WebSocketMessage(WebSocketMessage.Type.TEXT, factory.wrap(json.getBytes(StandardCharsets.UTF_8)));

        Flux<WebSocketMessage> responseFlux = sessionHandler.processMessage(mockSession, sessionState, message);

        StepVerifier.create(responseFlux)
                .assertNext(respMsg -> {
                    String payload = respMsg.getPayloadAsText();
                    assertTrue(payload.contains("\"ping-1\""));
                    assertTrue(payload.contains("\"jsonrpc\":\"2.0\""));
                })
                .verifyComplete();
    }

    @Test
    void testProcessSubscribeMessage() {
        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
        String json = "{\"jsonrpc\":\"2.0\",\"id\":\"sub-1\",\"method\":\"resources/subscribe\",\"params\":{\"uri\":\"resource://db/users\"}}";
        WebSocketMessage message = new WebSocketMessage(WebSocketMessage.Type.TEXT, factory.wrap(json.getBytes(StandardCharsets.UTF_8)));

        Flux<WebSocketMessage> responseFlux = sessionHandler.processMessage(mockSession, sessionState, message);

        StepVerifier.create(responseFlux)
                .assertNext(respMsg -> {
                    String payload = respMsg.getPayloadAsText();
                    assertTrue(payload.contains("\"sub-1\""));
                })
                .verifyComplete();

        assertTrue(sessionState.hasSubscription("resource://db/users"));
    }

    @Test
    void testInvalidJsonError() {
        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
        String json = "not valid json";
        WebSocketMessage message = new WebSocketMessage(WebSocketMessage.Type.TEXT, factory.wrap(json.getBytes(StandardCharsets.UTF_8)));

        Flux<WebSocketMessage> responseFlux = sessionHandler.processMessage(mockSession, sessionState, message);

        StepVerifier.create(responseFlux)
                .assertNext(respMsg -> {
                    String payload = respMsg.getPayloadAsText();
                    assertTrue(payload.contains("-32700")); // Parse error code
                })
                .verifyComplete();
    }
}
