package com.phaselume.torana.protocol.grpc.server;

import com.google.protobuf.ByteString;
import com.phaselume.torana.core.model.AgentRequest;
import com.phaselume.torana.core.model.AgentResponse;
import com.phaselume.torana.protocol.grpc.proto.AgentCallRequest;
import com.phaselume.torana.protocol.grpc.proto.AgentCallResponse;
import com.phaselume.torana.protocol.grpc.proto.AgentChunk;
import com.phaselume.torana.protocol.grpc.proto.AgentGatewayGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentGatewayGrpcServiceTest {

    private Server server;
    private ManagedChannel channel;
    private AgentGatewayGrpc.AgentGatewayBlockingStub blockingStub;
    private AgentGatewayGrpc.AgentGatewayStub asyncStub;

    @BeforeEach
    void setUp() throws IOException {
        String serverName = InProcessServerBuilder.generateName();

        AgentGatewayGrpcService service = new AgentGatewayGrpcService(request -> {
            if ("/grpc/stream-route".equals(request.getPath())) {
                Flux<AgentResponse.Chunk> chunks = Flux.just(
                        AgentResponse.Chunk.text("Chunk 1"),
                        AgentResponse.Chunk.text("Chunk 2"),
                        AgentResponse.Chunk.last("stop")
                );
                return Mono.just(AgentResponse.streaming(chunks));
            }

            return Mono.just(AgentResponse.builder()
                    .status(HttpStatus.OK)
                    .bufferedBody(Mono.just("gRPC Unary Success".getBytes(StandardCharsets.UTF_8)))
                    .build());
        });

        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(service)
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build();

        blockingStub = AgentGatewayGrpc.newBlockingStub(channel);
        asyncStub = AgentGatewayGrpc.newStub(channel);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (channel != null) {
            channel.shutdown().awaitTermination(2, TimeUnit.SECONDS);
        }
        if (server != null) {
            server.shutdown().awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void testUnaryCall() {
        AgentCallRequest request = AgentCallRequest.newBuilder()
                .setRouteId("unary-route")
                .setBody(ByteString.copyFromUtf8("Hello Torana"))
                .build();

        AgentCallResponse response = blockingStub.call(request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertEquals("gRPC Unary Success", response.getBody().toStringUtf8());
    }

    @Test
    void testServerStreamingCall() {
        AgentCallRequest request = AgentCallRequest.newBuilder()
                .setRouteId("stream-route")
                .build();

        var iterator = blockingStub.stream(request);
        List<AgentChunk> received = new ArrayList<>();
        while (iterator.hasNext()) {
            received.add(iterator.next());
        }

        assertEquals(3, received.size());
        assertEquals("Chunk 1", received.get(0).getDelta());
        assertEquals("Chunk 2", received.get(1).getDelta());
        assertTrue(received.get(2).getIsFinal());
        assertEquals("stop", received.get(2).getFinishReason());
    }

    @Test
    void testBidirectionalExchange() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<AgentChunk> responses = new ArrayList<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        StreamObserver<AgentCallRequest> requestObserver = asyncStub.exchange(new StreamObserver<>() {
            @Override
            public void onNext(AgentChunk value) {
                responses.add(value);
            }

            @Override
            public void onError(Throwable t) {
                errorRef.set(t);
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        });

        requestObserver.onNext(AgentCallRequest.newBuilder()
                .setRouteId("stream-route")
                .build());
        requestObserver.onCompleted();

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(3, responses.size());
        assertEquals("Chunk 1", responses.get(0).getDelta());
    }
}
