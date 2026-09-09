package com.phaselume.torana.protocol.grpc.server;

import com.phaselume.torana.protocol.grpc.GrpcProtocolProperties;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Embedded gRPC server lifecycle manager.
 */
@Slf4j
public class ToranaGrpcServer {

    private final GrpcProtocolProperties properties;
    private final AgentGatewayGrpcService grpcService;
    private final GrpcInterceptorChain interceptorChain;
    private Server server;
    private boolean running = false;

    public ToranaGrpcServer(
            GrpcProtocolProperties properties,
            AgentGatewayGrpcService grpcService,
            GrpcInterceptorChain interceptorChain) {
        this.properties = properties != null ? properties : new GrpcProtocolProperties();
        this.grpcService = grpcService;
        this.interceptorChain = interceptorChain != null ? interceptorChain : new GrpcInterceptorChain();
    }

    public synchronized void start() throws IOException {
        if (running) {
            return;
        }

        NettyServerBuilder serverBuilder = NettyServerBuilder.forPort(properties.getPort())
                .addService(grpcService)
                .intercept(interceptorChain)
                .maxInboundMessageSize(properties.getMaxInboundMessageSize())
                .keepAliveTime(properties.getKeepAliveTime().toSeconds(), TimeUnit.SECONDS)
                .keepAliveTimeout(properties.getKeepAliveTimeout().toSeconds(), TimeUnit.SECONDS);

        // TLS configuration
        if (properties.getTls() != null && properties.getTls().isEnabled()) {
            String certChainPath = properties.getTls().getCertChain();
            String privateKeyPath = properties.getTls().getPrivateKey();
            if (certChainPath != null && privateKeyPath != null) {
                serverBuilder.useTransportSecurity(new File(certChainPath), new File(privateKeyPath));
            }
        }

        // Proto reflection
        if (properties.isReflectionEnabled()) {
            try {
                serverBuilder.addService(ProtoReflectionService.newInstance());
            } catch (Throwable t) {
                log.debug("Proto reflection service not loaded: {}", t.getMessage());
            }
        }

        this.server = serverBuilder.build().start();
        this.running = true;
        log.info("Torana gRPC server started on port {}", server.getPort());
    }

    public synchronized void stop() {
        if (!running || server == null) {
            return;
        }
        log.info("Stopping Torana gRPC server...");
        server.shutdown();
        try {
            if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                server.shutdownNow();
            }
        } catch (InterruptedException e) {
            server.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            this.running = false;
        }
    }

    public boolean isRunning() {
        return running && server != null && !server.isShutdown();
    }

    public int getPort() {
        return server != null ? server.getPort() : properties.getPort();
    }

    public Server getServer() {
        return server;
    }
}
