package com.phaselume.torana.protocol.mcp;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.spi.ProtocolAdapter;
import com.phaselume.torana.protocol.mcp.codec.AgentResponseToMcpMapper;
import com.phaselume.torana.protocol.mcp.codec.McpMessageCodec;
import com.phaselume.torana.protocol.mcp.codec.McpToAgentRequestMapper;
import com.phaselume.torana.protocol.mcp.handler.McpRequestDispatcher;
import com.phaselume.torana.protocol.mcp.handler.McpResourcesHandler;
import com.phaselume.torana.protocol.mcp.handler.McpToolsHandler;
import com.phaselume.torana.protocol.mcp.handler.McpPromptsHandler;
import com.phaselume.torana.protocol.mcp.lifecycle.McpHeartbeatEmitter;
import com.phaselume.torana.protocol.mcp.lifecycle.McpInitializeHandler;
import com.phaselume.torana.protocol.mcp.lifecycle.McpSessionManager;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcError;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcRequest;
import com.phaselume.torana.protocol.mcp.model.McpJsonRpcResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * ProtocolAdapter SPI implementation for Model Context Protocol (MCP) over HTTP and SSE.
 */
@Component
public class McpProtocolAdapter implements ProtocolAdapter {

    public static final String PROTOCOL_NAME = "mcp";
    public static final String HEADER_SESSION_ID = "X-Session-Id";

    private final McpProtocolProperties properties;
    private final McpMessageCodec codec;
    private final McpToAgentRequestMapper requestMapper;
    private final AgentResponseToMcpMapper responseMapper;
    private final McpRequestDispatcher dispatcher;
    private final McpHeartbeatEmitter heartbeatEmitter;
    private final McpSessionManager sessionManager;

    public McpProtocolAdapter(McpProtocolProperties properties) {
        this.properties = properties != null ? properties : new McpProtocolProperties();
        this.codec = new McpMessageCodec();
        this.requestMapper = new McpToAgentRequestMapper(this.codec);
        this.responseMapper = new AgentResponseToMcpMapper(this.codec);
        this.sessionManager = new McpSessionManager(this.properties.getSessionTtl());
        this.heartbeatEmitter = new McpHeartbeatEmitter(this.properties.getSseHeartbeatInterval());

        McpInitializeHandler initializeHandler = new McpInitializeHandler(
                this.sessionManager,
                this.codec,
                this.properties.getServerVersion()
        );
        this.dispatcher = new McpRequestDispatcher(
                initializeHandler,
                new McpToolsHandler(),
                new McpResourcesHandler(),
                new McpPromptsHandler()
        );
    }

    public McpProtocolAdapter(
            McpProtocolProperties properties,
            McpMessageCodec codec,
            McpRequestDispatcher dispatcher,
            McpSessionManager sessionManager,
            McpHeartbeatEmitter heartbeatEmitter) {
        this.properties = properties != null ? properties : new McpProtocolProperties();
        this.codec = codec != null ? codec : new McpMessageCodec();
        this.requestMapper = new McpToAgentRequestMapper(this.codec);
        this.responseMapper = new AgentResponseToMcpMapper(this.codec);
        this.dispatcher = dispatcher;
        this.sessionManager = sessionManager;
        this.heartbeatEmitter = heartbeatEmitter != null ? heartbeatEmitter : new McpHeartbeatEmitter();
    }

    @Override
    public String protocol() {
        return PROTOCOL_NAME;
    }

    @Override
    public RouterFunction<ServerResponse> routerFunction() {
        String basePath = properties.getPath() != null ? properties.getPath() : "/mcp/v1";
        return RouterFunctions.route(
                RequestPredicates.POST(basePath).or(RequestPredicates.POST(basePath + "/**")),
                request -> ServerResponse.ok().bodyValue("Torana MCP Endpoint")
        ).andRoute(
                RequestPredicates.GET(basePath + "/sse"),
                request -> ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(heartbeatEmitter.createHeartbeatStream(), String.class)
        );
    }

    @Override
    public Mono<AgentRequest> decode(ServerWebExchange exchange) {
        if (exchange == null) {
            return Mono.error(new IllegalArgumentException("ServerWebExchange cannot be null"));
        }

        String sessionId = exchange.getRequest().getHeaders().getFirst(HEADER_SESSION_ID);

        return DataBufferUtils.join(exchange.getRequest().getBody())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .flatMap(bodyBytes -> {
                    McpJsonRpcRequest rpcRequest;
                    try {
                        rpcRequest = codec.decodeRequest(bodyBytes);
                    } catch (Exception e) {
                        return Mono.error(new IllegalArgumentException("Invalid JSON-RPC 2.0 payload: " + e.getMessage(), e));
                    }

                    // Check for direct control requests (initialize, tools/list, etc.)
                    McpJsonRpcResponse directResponse = dispatcher.dispatchControl(rpcRequest, sessionId);
                    if (directResponse != null) {
                        // Store direct response in exchange attributes for fast-path return
                        exchange.getAttributes().put("torana.mcp.direct_response", directResponse);
                    } else {
                        // Validate execution request
                        McpJsonRpcError validationError = dispatcher.validateExecutionRequest(rpcRequest);
                        if (validationError != null) {
                            exchange.getAttributes().put("torana.mcp.direct_response", McpJsonRpcResponse.error(rpcRequest.getId(), validationError));
                        }
                    }

                    AgentRequest agentRequest = requestMapper.map(rpcRequest, exchange, bodyBytes);
                    return Mono.just(agentRequest);
                });
    }

    @Override
    public Mono<Void> encode(AgentContext context, Flux<AgentResponse.Chunk> responseStream, ServerWebExchange exchange) {
        if (exchange == null) {
            return Mono.error(new IllegalArgumentException("ServerWebExchange cannot be null"));
        }

        ServerHttpResponse response = exchange.getResponse();

        // Check if a direct response was stored in exchange attributes
        McpJsonRpcResponse directResponse = exchange.getAttribute("torana.mcp.direct_response");
        if (directResponse != null) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            byte[] jsonBytes = codec.encode(directResponse);
            DataBuffer buffer = response.bufferFactory().wrap(jsonBytes);
            return response.writeWith(Mono.just(buffer));
        }

        Object rpcId = context != null ? context.getAttribute("mcp.id") : null;

        // If streaming SSE
        response.getHeaders().setContentType(MediaType.TEXT_EVENT_STREAM);
        Flux<String> sseFlux = responseMapper.mapStreamToSse(responseStream, rpcId);

        Flux<DataBuffer> bufferFlux = sseFlux.map(sseText ->
                response.bufferFactory().wrap(sseText.getBytes(StandardCharsets.UTF_8))
        );

        return response.writeWith(bufferFlux);
    }

    public McpRequestDispatcher getDispatcher() {
        return dispatcher;
    }

    public McpSessionManager getSessionManager() {
        return sessionManager;
    }

    public McpMessageCodec getCodec() {
        return codec;
    }

    public McpProtocolProperties getProperties() {
        return properties;
    }
}
