package com.phaselume.torana.protocol.mcp;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpProtocolAdapterTest {

    private McpProtocolAdapter adapter;

    @BeforeEach
    void setUp() {
        McpProtocolProperties properties = new McpProtocolProperties();
        properties.setPath("/mcp/v1");
        adapter = new McpProtocolAdapter(properties);
    }

    @Test
    void testProtocolName() {
        assertEquals("mcp", adapter.protocol());
    }

    @Test
    void testDecodeControlRequestDirectlyHandled() {
        String initJson = """
                {
                    "jsonrpc": "2.0",
                    "id": "init-123",
                    "method": "initialize",
                    "params": {
                        "protocolVersion": "2024-11-05",
                        "clientInfo": {"name": "test-client", "version": "1.0.0"}
                    }
                }
                """;

        MockServerHttpRequest request = MockServerHttpRequest.post("/mcp/v1")
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(initJson);

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(adapter.decode(exchange))
                .assertNext(agentRequest -> {
                    assertEquals("init-123", agentRequest.getId());
                    assertEquals("mcp", agentRequest.getProtocol());
                    assertEquals("/mcp/initialize", agentRequest.getPath());

                    // Verify direct response was attached to exchange
                    McpJsonRpcResponse directResp = exchange.getAttribute("torana.mcp.direct_response");
                    assertNotNull(directResp);
                    assertEquals("init-123", directResp.getId());
                })
                .verifyComplete();
    }

    @Test
    void testEncodeStreamingSseResponse() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/mcp/v1").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AgentContext context = AgentContext.builder()
                .id("rpc-999")
                .build()
                .withAttribute("mcp.id", "rpc-999");

        Flux<AgentResponse.Chunk> chunks = Flux.just(
                AgentResponse.Chunk.builder().index(1).textDelta("Thinking...").build(),
                AgentResponse.Chunk.builder().index(2).textDelta("Done").last(true).finishReason("stop").build());

        StepVerifier.create(adapter.encode(context, chunks, exchange))
                .verifyComplete();

        assertEquals(MediaType.TEXT_EVENT_STREAM, exchange.getResponse().getHeaders().getContentType());
    }
}
