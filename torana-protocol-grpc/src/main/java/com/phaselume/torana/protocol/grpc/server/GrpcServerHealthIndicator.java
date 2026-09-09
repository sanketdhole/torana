package com.phaselume.torana.protocol.grpc.server;

import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for the embedded gRPC server.
 */
public class GrpcServerHealthIndicator {

    private final ToranaGrpcServer grpcServer;

    public GrpcServerHealthIndicator(ToranaGrpcServer grpcServer) {
        this.grpcServer = grpcServer;
    }

    public boolean isUp() {
        return grpcServer != null && grpcServer.isRunning();
    }

    public Map<String, Object> getDetails() {
        Map<String, Object> details = new HashMap<>();
        if (grpcServer != null) {
            details.put("status", grpcServer.isRunning() ? "UP" : "DOWN");
            details.put("port", grpcServer.getPort());
        } else {
            details.put("status", "DOWN");
            details.put("error", "gRPC server instance is null");
        }
        return details;
    }
}
