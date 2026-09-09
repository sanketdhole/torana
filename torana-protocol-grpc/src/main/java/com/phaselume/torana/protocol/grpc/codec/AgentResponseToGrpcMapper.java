package com.phaselume.torana.protocol.grpc.codec;

import com.google.protobuf.ByteString;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.protocol.grpc.proto.AgentCallResponse;
import com.phaselume.torana.protocol.grpc.proto.AgentChunk;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps Torana AgentResponse domain objects and streaming Chunks to gRPC protobuf messages.
 */
public class AgentResponseToGrpcMapper {

    /**
     * Maps an AgentResponse to an AgentCallResponse protobuf message.
     */
    public Mono<AgentCallResponse> mapUnary(AgentResponse response) {
        if (response == null) {
            return Mono.just(AgentCallResponse.newBuilder()
                    .setStatusCode(200)
                    .build());
        }

        int statusCode = response.getStatus() != null ? response.getStatus().value() : 200;
        Map<String, String> headerMap = new HashMap<>();
        if (response.getHeaders() != null) {
            response.getHeaders().forEach((k, v) -> {
                if (v != null && !v.isEmpty()) {
                    headerMap.put(k, String.join(",", v));
                }
            });
        }

        if (response.getBufferedBody() != null) {
            return response.getBufferedBody().map(bytes -> AgentCallResponse.newBuilder()
                    .setStatusCode(statusCode)
                    .putAllHeaders(headerMap)
                    .setBody(ByteString.copyFrom(bytes))
                    .build()
            ).defaultIfEmpty(AgentCallResponse.newBuilder()
                    .setStatusCode(statusCode)
                    .putAllHeaders(headerMap)
                    .build()
            );
        }

        return Mono.just(AgentCallResponse.newBuilder()
                .setStatusCode(statusCode)
                .putAllHeaders(headerMap)
                .build());
    }

    /**
     * Maps a stream of AgentResponse.Chunk to a stream of gRPC AgentChunk protobuf messages.
     */
    public Flux<AgentChunk> mapStream(Flux<AgentResponse.Chunk> chunkFlux) {
        if (chunkFlux == null) {
            return Flux.empty();
        }

        return chunkFlux.map(this::mapChunk);
    }

    /**
     * Maps a single AgentResponse.Chunk to an AgentChunk protobuf message.
     */
    public AgentChunk mapChunk(AgentResponse.Chunk chunk) {
        AgentChunk.Builder builder = AgentChunk.newBuilder();

        if (chunk.getTextDelta() != null) {
            builder.setDelta(chunk.getTextDelta());
        } else if (chunk.getData() != null) {
            builder.setDelta(new String(chunk.getData()));
        }

        builder.setIsFinal(chunk.isLast());

        if (chunk.getFinishReason() != null) {
            builder.setFinishReason(chunk.getFinishReason());
        }

        if (chunk.getTokenCount() != null) {
            builder.setTokenCount(chunk.getTokenCount());
        }

        if (chunk.getMetadata() != null && !chunk.getMetadata().isEmpty()) {
            Map<String, String> metadataMap = new HashMap<>();
            chunk.getMetadata().forEach((k, v) -> {
                if (v != null) {
                    metadataMap.put(k, String.valueOf(v));
                }
            });
            builder.putAllMetadata(metadataMap);
        }

        return builder.build();
    }
}
