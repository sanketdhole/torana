package com.phaselume.torana.protocol.grpc.codec;

import com.google.protobuf.ByteString;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.protocol.grpc.proto.AgentCallRequest;
import com.phaselume.torana.protocol.grpc.proto.AgentCallResponse;
import com.phaselume.torana.protocol.grpc.proto.AgentChunk;
import io.grpc.Metadata;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrpcMapperTest {

    @Test
    void testMapRequest() {
        GrpcToAgentRequestMapper mapper = new GrpcToAgentRequestMapper();

        AgentCallRequest request = AgentCallRequest.newBuilder()
                .setRouteId("weather-tool")
                .setTraceId("trace-12345")
                .setTenantId("tenant-acme")
                .putHeaders("X-Custom", "custom-value")
                .setBody(ByteString.copyFromUtf8("{\"city\":\"San Francisco\"}"))
                .build();

        AgentRequest agentRequest = mapper.map(request, new Metadata());

        assertNotNull(agentRequest);
        assertEquals("trace-12345", agentRequest.getId());
        assertEquals("grpc", agentRequest.getProtocol());
        assertEquals("/grpc/weather-tool", agentRequest.getPath());
        assertEquals("custom-value", agentRequest.getHeaders().getFirst("X-Custom"));
        assertEquals("tenant-acme", agentRequest.getAttribute("torana.tenant.id"));
        assertEquals("weather-tool", agentRequest.getAttribute("torana.route.id"));
        assertEquals("{\"city\":\"San Francisco\"}", new String(agentRequest.getCachedBody(), StandardCharsets.UTF_8));
    }

    @Test
    void testMapUnaryResponse() {
        AgentResponseToGrpcMapper mapper = new AgentResponseToGrpcMapper();

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Server", "torana");

        AgentResponse agentResponse = AgentResponse.builder()
                .status(HttpStatus.OK)
                .headers(headers)
                .bufferedBody(reactor.core.publisher.Mono.just("response-body".getBytes(StandardCharsets.UTF_8)))
                .build();

        StepVerifier.create(mapper.mapUnary(agentResponse))
                .assertNext(grpcResponse -> {
                    assertEquals(200, grpcResponse.getStatusCode());
                    assertEquals("torana", grpcResponse.getHeadersMap().get("X-Server"));
                    assertEquals("response-body", grpcResponse.getBody().toStringUtf8());
                })
                .verifyComplete();
    }

    @Test
    void testMapStreamingResponse() {
        AgentResponseToGrpcMapper mapper = new AgentResponseToGrpcMapper();

        Flux<AgentResponse.Chunk> chunks = Flux.just(
                AgentResponse.Chunk.builder().index(1).textDelta("Thinking...").build(),
                AgentResponse.Chunk.builder().index(2).textDelta("Done").last(true).finishReason("stop").tokenCount(42).build()
        );

        StepVerifier.create(mapper.mapStream(chunks))
                .assertNext(chunk1 -> {
                    assertEquals("Thinking...", chunk1.getDelta());
                    assertFalse(chunk1.getIsFinal());
                })
                .assertNext(chunk2 -> {
                    assertEquals("Done", chunk2.getDelta());
                    assertTrue(chunk2.getIsFinal());
                    assertEquals("stop", chunk2.getFinishReason());
                    assertEquals(42, chunk2.getTokenCount());
                })
                .verifyComplete();
    }

    @Test
    void testMetadataExtractor() {
        GrpcMetadataExtractor extractor = new GrpcMetadataExtractor();

        Metadata metadata = new Metadata();
        metadata.put(GrpcMetadataExtractor.AUTHORIZATION_KEY, "Bearer jwt-token-123");
        metadata.put(GrpcMetadataExtractor.API_KEY, "api-key-xyz");
        metadata.put(GrpcMetadataExtractor.TENANT_ID_KEY, "tenant-test");
        metadata.put(GrpcMetadataExtractor.TRACEPARENT_KEY, "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");

        HttpHeaders headers = extractor.extractHeaders(metadata);
        assertEquals("Bearer jwt-token-123", headers.getFirst("authorization"));
        assertEquals("api-key-xyz", headers.getFirst("x-api-key"));

        Map<String, Object> attributes = extractor.extractAttributes(metadata);
        assertEquals("tenant-test", attributes.get("torana.tenant.id"));
        assertEquals("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01", attributes.get("torana.trace.parent"));
    }
}
