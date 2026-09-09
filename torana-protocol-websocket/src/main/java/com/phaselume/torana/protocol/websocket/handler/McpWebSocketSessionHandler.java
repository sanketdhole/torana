package com.phaselume.torana.protocol.websocket.handler;

import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.protocol.mcp.codec.AgentResponseToMcpMapper;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcError;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcRequest;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcResponse;
import com.phaselume.torana.protocol.mcp.model.McpProgressNotification;
import com.phaselume.torana.protocol.websocket.codec.WebSocketFrameRouter;
import com.phaselume.torana.protocol.websocket.codec.WebSocketMcpCodec;
import com.phaselume.torana.protocol.websocket.session.WebSocketSessionState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;

/**
 * Handles message processing and multiplexed bidirectional streaming on a single WebSocket connection.
 */
@Slf4j
public class McpWebSocketSessionHandler {

    private final WebSocketMcpCodec codec;
    private final WebSocketFrameRouter router;
    private final AgentResponseToMcpMapper responseMapper;
    private final Function<AgentRequest, Mono<AgentResponse>> requestDispatcher;

    public McpWebSocketSessionHandler(
            WebSocketMcpCodec codec,
            WebSocketFrameRouter router,
            Function<AgentRequest, Mono<AgentResponse>> requestDispatcher) {
        this.codec = codec != null ? codec : new WebSocketMcpCodec();
        this.router = router;
        this.responseMapper = new AgentResponseToMcpMapper(this.codec.getMcpCodec());
        this.requestDispatcher = requestDispatcher != null ? requestDispatcher : req -> Mono.just(AgentResponse.buffered(new byte[0]));
    }

    /**
     * Connects inbound messages to outbound response messages for the given session.
     */
    public Mono<Void> handle(WebSocketSession session, WebSocketSessionState sessionState) {
        Flux<WebSocketMessage> output = session.receive()
                .filter(msg -> msg.getType() == WebSocketMessage.Type.TEXT || msg.getType() == WebSocketMessage.Type.BINARY)
                .flatMap(msg -> {
                    sessionState.recordActivity();
                    return processMessage(session, sessionState, msg);
                });

        return session.send(output);
    }

    /**
     * Processes a single inbound WebSocketMessage and produces a Flux of outbound response messages.
     */
    public Flux<WebSocketMessage> processMessage(WebSocketSession session, WebSocketSessionState sessionState, WebSocketMessage message) {
        McpJsonRpcRequest rpcRequest;
        try {
            rpcRequest = codec.decode(message);
        } catch (Exception e) {
            McpJsonRpcResponse parseErr = McpJsonRpcResponse.error(null, McpJsonRpcError.parseError("Invalid JSON-RPC: " + e.getMessage()));
            return Flux.just(codec.encode(session, parseErr));
        }

        // 1. Check local control methods (init, tools/list, resources/subscribe, etc.)
        McpJsonRpcResponse directResponse = router.routeControl(rpcRequest, sessionState);
        if (directResponse != null) {
            return Flux.just(codec.encode(session, directResponse));
        }

        // 2. Validate execution requests
        McpJsonRpcError validationError = router.validateExecution(rpcRequest);
        if (validationError != null) {
            McpJsonRpcResponse errorResponse = McpJsonRpcResponse.error(rpcRequest.getId(), validationError);
            return Flux.just(codec.encode(session, errorResponse));
        }

        // 3. Map to AgentRequest and dispatch to execution pipeline
        AgentRequest agentRequest = router.mapToAgentRequest(rpcRequest, sessionState);
        Object rpcId = rpcRequest.getId();

        return requestDispatcher.apply(agentRequest)
                .flatMapMany(agentResponse -> {
                    if (agentResponse.getStream() != null) {
                        return agentResponse.getStream().map(chunk -> {
                            if (chunk.isLast()) {
                                McpJsonRpcResponse finalResponse = McpJsonRpcResponse.success(rpcId, Map.of(
                                        "content", new Object[]{
                                                Map.of("type", "text", "text", chunk.getTextDelta() != null ? chunk.getTextDelta() : "")
                                        },
                                        "finishReason", chunk.getFinishReason() != null ? chunk.getFinishReason() : "stop"
                                ));
                                return codec.encode(session, finalResponse);
                            } else {
                                McpProgressNotification notification = McpProgressNotification.builder()
                                        .progressToken(rpcId)
                                        .progress(chunk.getIndex())
                                        .message(chunk.getTextDelta())
                                        .build();
                                return codec.encode(session, notification);
                            }
                        });
                    } else {
                        return responseMapper.mapBuffered(agentResponse, rpcId)
                                .map(mcpResp -> codec.encode(session, mcpResp))
                                .flux();
                    }
                })
                .onErrorResume(error -> {
                    log.error("Error executing MCP tool call for request {}: {}", rpcId, error.getMessage(), error);
                    McpJsonRpcResponse errResponse = McpJsonRpcResponse.error(rpcId, McpJsonRpcError.internalError("Pipeline error: " + error.getMessage()));
                    return Flux.just(codec.encode(session, errResponse));
                });
    }

    public WebSocketMcpCodec getCodec() {
        return codec;
    }

    public WebSocketFrameRouter getRouter() {
        return router;
    }
}
