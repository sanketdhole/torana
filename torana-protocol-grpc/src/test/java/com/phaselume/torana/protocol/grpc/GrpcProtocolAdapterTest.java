package com.phaselume.torana.protocol.grpc;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.protocol.grpc.server.AgentGatewayGrpcService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GrpcProtocolAdapterTest {

    private GrpcProtocolAdapter adapter;

    @BeforeEach
    void setUp() {
        GrpcProtocolProperties properties = new GrpcProtocolProperties();
        AgentGatewayGrpcService grpcService = new AgentGatewayGrpcService(req -> Mono.just(AgentResponse.buffered(new byte[0])));
        adapter = new GrpcProtocolAdapter(properties, grpcService);
    }

    @Test
    void testProtocolName() {
        assertEquals("grpc", adapter.protocol());
    }

    @Test
    void testRouterFunction() {
        assertNotNull(adapter.routerFunction());
    }

    @Test
    void testDecode() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/grpc/test-service")
                .header("Content-Type", "application/grpc")
                .body("grpc-payload");

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(adapter.decode(exchange))
                .assertNext(agentRequest -> {
                    assertEquals("grpc", agentRequest.getProtocol());
                    assertEquals("/grpc/test-service", agentRequest.getPath());
                    assertEquals("application/grpc", agentRequest.getHeaders().getFirst("Content-Type"));
                })
                .verifyComplete();
    }

    @Test
    void testEncode() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/grpc/test-service").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AgentContext context = AgentContext.builder().id("ctx-1").build();
        Flux<AgentResponse.Chunk> chunks = Flux.just(
                AgentResponse.Chunk.text("chunk-data"),
                AgentResponse.Chunk.last("stop")
        );

        StepVerifier.create(adapter.encode(context, chunks, exchange))
                .verifyComplete();
    }
}
