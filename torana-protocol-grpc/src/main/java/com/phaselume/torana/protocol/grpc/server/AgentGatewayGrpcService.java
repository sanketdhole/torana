package com.phaselume.torana.protocol.grpc.server;

import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.protocol.grpc.codec.AgentResponseToGrpcMapper;
import com.phaselume.torana.protocol.grpc.codec.GrpcToAgentRequestMapper;
import com.phaselume.torana.protocol.grpc.proto.AgentCallRequest;
import com.phaselume.torana.protocol.grpc.proto.AgentCallResponse;
import com.phaselume.torana.protocol.grpc.proto.AgentChunk;
import com.phaselume.torana.protocol.grpc.proto.AgentGatewayGrpc;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * gRPC service implementing the generated AgentGatewayGrpc.AgentGatewayImplBase.
 * Dispatches unary, server-streaming, and bidirectional-streaming calls to Torana's reactive pipeline.
 */
public class AgentGatewayGrpcService extends AgentGatewayGrpc.AgentGatewayImplBase {

    private final GrpcToAgentRequestMapper requestMapper;
    private final AgentResponseToGrpcMapper responseMapper;
    private final Function<AgentRequest, Mono<AgentResponse>> requestDispatcher;

    public AgentGatewayGrpcService(
            Function<AgentRequest, Mono<AgentResponse>> requestDispatcher) {
        this(new GrpcToAgentRequestMapper(), new AgentResponseToGrpcMapper(), requestDispatcher);
    }

    public AgentGatewayGrpcService(
            GrpcToAgentRequestMapper requestMapper,
            AgentResponseToGrpcMapper responseMapper,
            Function<AgentRequest, Mono<AgentResponse>> requestDispatcher) {
        this.requestMapper = requestMapper != null ? requestMapper : new GrpcToAgentRequestMapper();
        this.responseMapper = responseMapper != null ? responseMapper : new AgentResponseToGrpcMapper();
        this.requestDispatcher = requestDispatcher != null ? requestDispatcher : req -> Mono.just(AgentResponse.buffered(new byte[0]));
    }

    @Override
    public void call(AgentCallRequest request, StreamObserver<AgentCallResponse> responseObserver) {
        Metadata metadata = GrpcInterceptorChain.METADATA_CONTEXT_KEY.get();
        AgentRequest agentRequest = requestMapper.map(request, metadata);

        requestDispatcher.apply(agentRequest)
                .flatMap(responseMapper::mapUnary)
                .subscribe(
                        responseObserver::onNext,
                        error -> responseObserver.onError(Status.INTERNAL
                                .withDescription("Error processing gRPC request: " + error.getMessage())
                                .withCause(error)
                                .asRuntimeException()),
                        responseObserver::onCompleted
                );
    }

    @Override
    public void stream(AgentCallRequest request, StreamObserver<AgentChunk> responseObserver) {
        Metadata metadata = GrpcInterceptorChain.METADATA_CONTEXT_KEY.get();
        AgentRequest agentRequest = requestMapper.map(request, metadata);

        requestDispatcher.apply(agentRequest)
                .flatMapMany(response -> {
                    if (response.getStream() != null) {
                        return responseMapper.mapStream(response.getStream());
                    } else if (response.getBufferedBody() != null) {
                        return response.getBufferedBody().map(bytes ->
                                AgentChunk.newBuilder()
                                        .setDelta(new String(bytes))
                                        .setIsFinal(true)
                                        .setFinishReason("stop")
                                        .build()
                        );
                    } else {
                        return Mono.just(AgentChunk.newBuilder().setIsFinal(true).build());
                    }
                })
                .subscribe(
                        responseObserver::onNext,
                        error -> responseObserver.onError(Status.INTERNAL
                                .withDescription("Error streaming gRPC response: " + error.getMessage())
                                .withCause(error)
                                .asRuntimeException()),
                        responseObserver::onCompleted
                );
    }

    @Override
    public StreamObserver<AgentCallRequest> exchange(StreamObserver<AgentChunk> responseObserver) {
        return new StreamObserver<AgentCallRequest>() {
            @Override
            public void onNext(AgentCallRequest request) {
                Metadata metadata = GrpcInterceptorChain.METADATA_CONTEXT_KEY.get();
                AgentRequest agentRequest = requestMapper.map(request, metadata);

                requestDispatcher.apply(agentRequest)
                        .flatMapMany(response -> {
                            if (response.getStream() != null) {
                                return responseMapper.mapStream(response.getStream());
                            } else if (response.getBufferedBody() != null) {
                                return response.getBufferedBody().map(bytes ->
                                        AgentChunk.newBuilder()
                                                .setDelta(new String(bytes))
                                                .setIsFinal(true)
                                                .setFinishReason("stop")
                                                .build()
                                );
                            } else {
                                return Mono.just(AgentChunk.newBuilder().setIsFinal(true).build());
                            }
                        })
                        .subscribe(
                                responseObserver::onNext,
                                error -> responseObserver.onError(Status.INTERNAL
                                        .withDescription("Error in gRPC exchange: " + error.getMessage())
                                        .withCause(error)
                                        .asRuntimeException())
                        );
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}
