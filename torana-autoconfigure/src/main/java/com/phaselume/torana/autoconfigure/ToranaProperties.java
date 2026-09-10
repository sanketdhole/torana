package com.phaselume.torana.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Root configuration properties for the entire Torana Enterprise Gateway.
 */
@Data
@ConfigurationProperties(prefix = "torana")
public class ToranaProperties {

    private ProtocolsProperties protocols = new ProtocolsProperties();
    private SecurityProperties security = new SecurityProperties();
    private ObservabilityProperties observability = new ObservabilityProperties();
    private StreamingProperties streaming = new StreamingProperties();
    private Map<String, RouteDefinition> routes = new HashMap<>();

    @Data
    public static class ProtocolsProperties {
        private McpProperties mcp = new McpProperties();
        private WebSocketProperties websocket = new WebSocketProperties();
        private GrpcProperties grpc = new GrpcProperties();
        private RestProperties rest = new RestProperties();
    }

    @Data
    public static class McpProperties {
        private boolean enabled = true;
        private String path = "/mcp";
    }

    @Data
    public static class WebSocketProperties {
        private boolean enabled = true;
        private String path = "/ws/v1";
    }

    @Data
    public static class GrpcProperties {
        private boolean enabled = false;
        private int port = 9090;
    }

    @Data
    public static class RestProperties {
        private boolean enabled = true;
    }

    @Data
    public static class SecurityProperties {
        private AuthnProperties authn = new AuthnProperties();
        private AuthzProperties authz = new AuthzProperties();
    }

    @Data
    public static class AuthnProperties {
        private boolean enabled = true;
        private String mode = "FIRST_MATCH"; // FIRST_MATCH, ALL_REQUIRED
        private List<Map<String, Object>> providers = new ArrayList<>();
    }

    @Data
    public static class AuthzProperties {
        private String engine = "opa";
        private boolean enabled = true;
    }

    @Data
    public static class ObservabilityProperties {
        private boolean enabled = true;
    }

    @Data
    public static class StreamingProperties {
        private boolean enabled = true;
    }

    @Data
    public static class RouteDefinition {
        private String id;
        private String path;
        private List<String> methods = new ArrayList<>();
        private String connectorRef;
        private String pipelineRef;
        private Map<String, Object> metadata = new HashMap<>();
    }
}
