package com.phaselume.torana.protocol.rest;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.AgentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestProtocolAdapterTest {

    private RestProtocolAdapter adapter;

    @BeforeEach
    void setUp() {
        RestProtocolProperties properties = new RestProtocolProperties();
        properties.setPath("/api/v1/**");
        properties.setStripPathPrefix(true);

        RestProtocolProperties.PathRewriteConfig rewrite = new RestProtocolProperties.PathRewriteConfig();
        rewrite.setFrom("/api/v1/legacy/**");
        rewrite.setTo("/v2/**");
        properties.setPathRewrites(List.of(rewrite));

        adapter = new RestProtocolAdapter(properties);
    }

    @Test
    void testProtocolName() {
        assertEquals("rest", adapter.protocol());
    }

    @Test
    void testDecodePostRequestWithBody() {
        String bodyJson = "{\"prompt\": \"hello world\"}";
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/chat")
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("X-Request-ID", "req-test-123")
                .queryParam("stream", "true")
                .body(bodyJson);

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(adapter.decode(exchange))
                .assertNext(agentRequest -> {
                    assertEquals("req-test-123", agentRequest.getId());
                    assertEquals("rest", agentRequest.getProtocol());
                    assertEquals(HttpMethod.POST, agentRequest.getMethod());
                    assertEquals("/chat", agentRequest.getPath()); // stripped /api/v1
                    assertEquals("true", agentRequest.getQueryParams().getFirst("stream"));
                    assertNotNull(agentRequest.getCachedBody());
                    assertEquals(bodyJson, new String(agentRequest.getCachedBody(), StandardCharsets.UTF_8));
                })
                .verifyComplete();
    }

    @Test
    void testEncodeStreamingResponse() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/status").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AgentContext context = AgentContext.builder()
                .request(AgentRequest.builder().id("req-test-123").build())
                .traceId("trace-xyz-789")
                .build()
                .withAttribute("torana.route.id", "route-status");

        Flux<AgentResponse.Chunk> chunks = Flux.just(
                AgentResponse.Chunk.text("Hello "),
                AgentResponse.Chunk.text("World!"),
                AgentResponse.Chunk.last("stop"));

        StepVerifier.create(adapter.encode(context, chunks, exchange))
                .verifyComplete();

        assertEquals("req-test-123", exchange.getResponse().getHeaders().getFirst("X-Torana-Request-Id"));
        assertEquals("trace-xyz-789", exchange.getResponse().getHeaders().getFirst("X-Torana-Trace-Id"));
        assertEquals("route-status", exchange.getResponse().getHeaders().getFirst("X-Torana-Route-Id"));
        assertTrue(exchange.getResponse().getHeaders().containsHeader("X-Torana-Latency-Ms"));
    }
}
