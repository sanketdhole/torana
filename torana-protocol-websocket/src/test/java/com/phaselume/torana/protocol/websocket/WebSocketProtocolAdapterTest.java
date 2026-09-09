package com.phaselume.torana.protocol.websocket;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.protocol.websocket.handler.McpWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class WebSocketProtocolAdapterTest {

    private WebSocketProtocolAdapter adapter;

    @BeforeEach
    void setUp() {
        WebSocketProtocolProperties properties = new WebSocketProtocolProperties();
        McpWebSocketHandler handler = mock(McpWebSocketHandler.class);
        adapter = new WebSocketProtocolAdapter(properties, handler);
    }

    @Test
    void testProtocolName() {
        assertEquals("websocket", adapter.protocol());
    }

    @Test
    void testRouterFunction() {
        assertNotNull(adapter.routerFunction());
    }

    @Test
    void testHandlerMapping() {
        assertNotNull(adapter.getHandlerMapping());
    }

    @Test
    void testDecode() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/ws/mcp")
                .header("Upgrade", "websocket")
                .header("Connection", "Upgrade")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(adapter.decode(exchange))
                .assertNext(agentRequest -> {
                    assertEquals("websocket", agentRequest.getProtocol());
                    assertEquals("/ws/mcp", agentRequest.getPath());
                })
                .verifyComplete();
    }

    @Test
    void testEncode() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/ws/mcp").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AgentContext context = AgentContext.builder().id("ctx-1").build();
        Flux<AgentResponse.Chunk> chunks = Flux.just(
                AgentResponse.Chunk.text("delta 1"),
                AgentResponse.Chunk.last("stop")
        );

        StepVerifier.create(adapter.encode(context, chunks, exchange))
                .verifyComplete();
    }
}
