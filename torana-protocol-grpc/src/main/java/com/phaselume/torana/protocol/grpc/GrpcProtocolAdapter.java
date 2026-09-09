package com.phaselume.torana.protocol.grpc;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.core.spi.ProtocolAdapter;
import com.phaselume.torana.protocol.grpc.codec.AgentResponseToGrpcMapper;
import com.phaselume.torana.protocol.grpc.codec.GrpcMetadataExtractor;
import com.phaselume.torana.protocol.grpc.codec.GrpcToAgentRequestMapper;
import com.phaselume.torana.protocol.grpc.server.AgentGatewayGrpcService;
import com.phaselume.torana.protocol.grpc.server.GrpcInterceptorChain;
import com.phaselume.torana.protocol.grpc.server.ToranaGrpcServer;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

/**
 * Inbound protocol adapter for gRPC (HTTP/2 binary protobuf transport).
 */
public class GrpcProtocolAdapter implements ProtocolAdapter {

    public static final String PROTOCOL_NAME = "grpc";

    private final GrpcProtocolProperties properties;
    private final GrpcToAgentRequestMapper requestMapper;
    private final AgentResponseToGrpcMapper responseMapper;
    private final GrpcMetadataExtractor metadataExtractor;
    private final ToranaGrpcServer grpcServer;

    public GrpcProtocolAdapter(
            GrpcProtocolProperties properties,
            AgentGatewayGrpcService grpcService) {
        this(properties, new GrpcToAgentRequestMapper(), new AgentResponseToGrpcMapper(),
                new GrpcMetadataExtractor(), new ToranaGrpcServer(properties, grpcService, new GrpcInterceptorChain()));
    }

    public GrpcProtocolAdapter(
            GrpcProtocolProperties properties,
            GrpcToAgentRequestMapper requestMapper,
            AgentResponseToGrpcMapper responseMapper,
            GrpcMetadataExtractor metadataExtractor,
            ToranaGrpcServer grpcServer) {
        this.properties = properties != null ? properties : new GrpcProtocolProperties();
        this.requestMapper = requestMapper != null ? requestMapper : new GrpcToAgentRequestMapper();
        this.responseMapper = responseMapper != null ? responseMapper : new AgentResponseToGrpcMapper();
        this.metadataExtractor = metadataExtractor != null ? metadataExtractor : new GrpcMetadataExtractor();
        this.grpcServer = grpcServer;
    }

    @Override
    public String protocol() {
        return PROTOCOL_NAME;
    }

    @Override
    public RouterFunction<ServerResponse> routerFunction() {
        return RouterFunctions.route(
                RequestPredicates.POST("/grpc/**"),
                request -> ServerResponse.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).bodyValue("Torana gRPC Endpoint")
        );
    }

    @Override
    public Mono<AgentRequest> decode(ServerWebExchange exchange) {
        if (exchange == null) {
            return Mono.error(new IllegalArgumentException("ServerWebExchange cannot be null"));
        }

        return DataBufferUtils.join(exchange.getRequest().getBody())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .defaultIfEmpty(new byte[0])
                .map(bodyBytes -> AgentRequest.builder()
                        .id(exchange.getRequest().getId())
                        .protocol(PROTOCOL_NAME)
                        .method(exchange.getRequest().getMethod())
                        .path(exchange.getRequest().getPath().value())
                        .headers(exchange.getRequest().getHeaders())
                        .cachedBody(bodyBytes)
                        .rawExchange(exchange)
                        .build());
    }

    @Override
    public Mono<Void> encode(AgentContext context, Flux<AgentResponse.Chunk> responseStream, ServerWebExchange exchange) {
        if (exchange == null) {
            return Mono.error(new IllegalArgumentException("ServerWebExchange cannot be null"));
        }

        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_OCTET_STREAM);
        Flux<DataBuffer> bufferFlux = responseMapper.mapStream(responseStream).map(chunk ->
                exchange.getResponse().bufferFactory().wrap(chunk.toByteArray())
        );
        return exchange.getResponse().writeWith(bufferFlux);
    }

    public void startServer() throws IOException {
        if (grpcServer != null) {
            grpcServer.start();
        }
    }

    public void stopServer() {
        if (grpcServer != null) {
            grpcServer.stop();
        }
    }

    public ToranaGrpcServer getGrpcServer() {
        return grpcServer;
    }

    public GrpcProtocolProperties getProperties() {
        return properties;
    }

    public GrpcToAgentRequestMapper getRequestMapper() {
        return requestMapper;
    }

    public AgentResponseToGrpcMapper getResponseMapper() {
        return responseMapper;
    }
}
