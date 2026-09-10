package com.phaselume.torana.streaming.grpc;

import com.phaselume.torana.core.model.AgentResponse;
import io.grpc.stub.StreamObserver;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.function.Function;

/**
 * Bridges a reactive Flux of AgentResponse.Chunk to a gRPC StreamObserver.
 */
public class GrpcStreamWriter {

    public static <T> Disposable write(Flux<AgentResponse.Chunk> stream,
                                        StreamObserver<T> responseObserver,
                                        Function<AgentResponse.Chunk, T> mapper) {
        if (stream == null || responseObserver == null || mapper == null) {
            return () -> {};
        }

        return stream.subscribe(
                chunk -> responseObserver.onNext(mapper.apply(chunk)),
                error -> responseObserver.onError(error),
                () -> responseObserver.onCompleted()
        );
    }
}
