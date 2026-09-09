package com.phaselume.torana.protocol.mcp.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcResponse;
import com.phaselume.torana.protocol.mcp.model.McpProgressNotification;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Maps outbound Torana AgentResponse and chunk streams to MCP JSON-RPC 2.0 responses.
 */
public class AgentResponseToMcpMapper {

    private final McpMessageCodec codec;

    public AgentResponseToMcpMapper(McpMessageCodec codec) {
        this.codec = codec != null ? codec : new McpMessageCodec();
    }

    /**
     * Maps a buffered AgentResponse to an McpJsonRpcResponse.
     */
    public Mono<McpJsonRpcResponse> mapBuffered(AgentResponse response, Object requestId) {
        if (response == null) {
            return Mono.just(McpJsonRpcResponse.success(requestId, Map.of()));
        }

        if (response.getBufferedBody() != null) {
            return response.getBufferedBody().map(bytes -> {
                if (bytes == null || bytes.length == 0) {
                    return McpJsonRpcResponse.success(requestId, Map.of());
                }
                try {
                    JsonNode jsonNode = codec.getObjectMapper().readTree(bytes);
                    return McpJsonRpcResponse.success(requestId, jsonNode);
                } catch (Exception e) {
                    // If not JSON, return as text content object
                    return McpJsonRpcResponse.success(requestId, Map.of(
                            "content", new Object[]{
                                    Map.of("type", "text", "text", new String(bytes, StandardCharsets.UTF_8))
                            }
                    ));
                }
            });
        }

        return Mono.just(McpJsonRpcResponse.success(requestId, Map.of()));
    }

    /**
     * Maps a chunk stream from AgentResponse to formatted SSE event strings.
     */
    public Flux<String> mapStreamToSse(Flux<AgentResponse.Chunk> chunkStream, Object requestId) {
        if (chunkStream == null) {
            return Flux.empty();
        }

        return chunkStream.map(chunk -> {
            if (chunk.isLast()) {
                // Final result event
                McpJsonRpcResponse finalResponse = McpJsonRpcResponse.success(requestId, Map.of(
                        "content", new Object[]{
                                Map.of("type", "text", "text", chunk.getTextDelta() != null ? chunk.getTextDelta() : "")
                        },
                        "finishReason", chunk.getFinishReason() != null ? chunk.getFinishReason() : "stop"
                ));
                return "event: message\ndata: " + codec.encodeToString(finalResponse) + "\n\n";
            } else {
                // Progress notification
                McpProgressNotification notification = McpProgressNotification.builder()
                        .progressToken(requestId)
                        .progress(chunk.getIndex())
                        .message(chunk.getTextDelta())
                        .build();
                return "event: progress\ndata: " + codec.encodeToString(notification) + "\n\n";
            }
        });
    }
}
