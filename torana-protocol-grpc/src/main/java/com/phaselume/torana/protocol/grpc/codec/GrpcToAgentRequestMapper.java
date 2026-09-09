package com.phaselume.torana.protocol.grpc.codec;

import com.google.protobuf.ByteString;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.protocol.grpc.proto.AgentCallRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Maps incoming gRPC AgentCallRequest protobuf messages into Torana AgentRequest domain objects.
 */
public class GrpcToAgentRequestMapper {

    public static final String PROTOCOL_NAME = "grpc";

    /**
     * Maps an AgentCallRequest to an AgentRequest.
     */
    public AgentRequest map(AgentCallRequest request, io.grpc.Metadata grpcMetadata) {
        String requestId = request.getTraceId().isEmpty() ? UUID.randomUUID().toString() : request.getTraceId();
        String path = request.getRouteId().isEmpty() ? "/grpc/default" : "/grpc/" + request.getRouteId();

        HttpHeaders headers = new HttpHeaders();
        if (request.getHeadersCount() > 0) {
            request.getHeadersMap().forEach(headers::add);
        }

        Map<String, Object> attributes = new HashMap<>();
        if (!request.getTenantId().isEmpty()) {
            attributes.put("torana.tenant.id", request.getTenantId());
        }
        if (!request.getRouteId().isEmpty()) {
            attributes.put("torana.route.id", request.getRouteId());
        }

        byte[] bodyBytes = request.getBody().toByteArray();

        return AgentRequest.builder()
                .id(requestId)
                .protocol(PROTOCOL_NAME)
                .method(HttpMethod.POST)
                .path(path)
                .headers(headers)
                .cachedBody(bodyBytes)
                .attributes(attributes)
                .timestamp(Instant.now())
                .build();
    }
}
