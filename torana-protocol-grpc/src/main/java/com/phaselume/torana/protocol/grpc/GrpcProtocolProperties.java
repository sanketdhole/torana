package com.phaselume.torana.protocol.grpc;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for the gRPC protocol adapter.
 */
@Data
@ConfigurationProperties(prefix = "torana.protocols.grpc")
public class GrpcProtocolProperties {

    /**
     * Whether the gRPC protocol adapter is enabled.
     */
    private boolean enabled = true;

    /**
     * Port on which the embedded gRPC server listens.
     */
    private int port = 9090;

    /**
     * Whether gRPC Server Reflection is enabled (for grpcurl/grpc_cli discovery).
     */
    private boolean reflectionEnabled = true;

    /**
     * Maximum inbound message size in bytes (default: 10MB).
     */
    private int maxInboundMessageSize = 10 * 1024 * 1024;

    /**
     * Keep-alive time duration for idle HTTP/2 connections.
     */
    private Duration keepAliveTime = Duration.ofSeconds(60);

    /**
     * Keep-alive timeout duration.
     */
    private Duration keepAliveTimeout = Duration.ofSeconds(20);

    /**
     * TLS configuration for gRPC server.
     */
    private TlsConfig tls = new TlsConfig();

    @Data
    public static class TlsConfig {
        private boolean enabled = false;
        private String certChain;
        private String privateKey;
    }
}
