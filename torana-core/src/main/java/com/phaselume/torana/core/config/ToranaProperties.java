package com.phaselume.torana.core.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Root configuration properties for the Torana Embeddable Enterprise Gateway.
 * Binds YAML configuration under prefix 'torana'.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "torana")
public class ToranaProperties {

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private ProtocolsProperties protocols = new ProtocolsProperties();

    @Builder.Default
    private SecurityProperties security = new SecurityProperties();

    @Builder.Default
    private RoutingProperties routing = new RoutingProperties();

    @Builder.Default
    private PipelineProperties pipeline = new PipelineProperties();

    @Builder.Default
    private ResilienceProperties resilience = new ResilienceProperties();

    @Builder.Default
    private RateLimitProperties ratelimit = new RateLimitProperties();

    @Builder.Default
    private ConnectorProperties connectors = new ConnectorProperties();

    @Builder.Default
    private ObservabilityProperties observability = new ObservabilityProperties();

    @Builder.Default
    private MultiTenancyProperties multiTenancy = new MultiTenancyProperties();

    // ─── Sub-Properties ────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProtocolsProperties {
        @Builder.Default
        private ProtocolConfig mcp = new ProtocolConfig(true, "/mcp/v1");
        @Builder.Default
        private ProtocolConfig rest = new ProtocolConfig(true, "/v1");
        @Builder.Default
        private ProtocolConfig websocket = new ProtocolConfig(true, "/ws");
        @Builder.Default
        private ProtocolConfig grpc = new ProtocolConfig(false, null);

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ProtocolConfig {
            private boolean enabled;
            private String path;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityProperties {
        @Builder.Default
        private boolean enabled = true;
        @Builder.Default
        private AuthnProperties authn = new AuthnProperties();
        @Builder.Default
        private AuthzProperties authz = new AuthzProperties();
        @Builder.Default
        private VaultProperties vault = new VaultProperties();

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AuthnProperties {
            @Builder.Default
            private boolean enabled = true;
            @Builder.Default
            private String defaultProvider = "jwt";
            @Builder.Default
            private Map<String, Object> providers = new HashMap<>();
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AuthzProperties {
            @Builder.Default
            private boolean enabled = true;
            @Builder.Default
            private String opaUrl = "http://localhost:8181/v1/data/torana/authz";
            @Builder.Default
            private String policyPackage = "torana.authz";
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class VaultProperties {
            @Builder.Default
            private boolean enabled = false;
            private String address;
            private String token;
            @Builder.Default
            private String pathPrefix = "secret/data/torana";
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoutingProperties {
        @Builder.Default
        private List<RouteDefinition> routes = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PipelineProperties {
        @Builder.Default
        private List<PipelineDefinition> pipelines = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResilienceProperties {
        @Builder.Default
        private List<ResilienceProfileDefinition> profiles = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateLimitProperties {
        @Builder.Default
        private boolean enabled = true;
        @Builder.Default
        private String redisUrl = "redis://localhost:6379";
        @Builder.Default
        private List<RateLimitPolicyDefinition> policies = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectorProperties {
        @Builder.Default
        private List<ConnectorDefinition> backends = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ObservabilityProperties {
        @Builder.Default
        private boolean enabled = true;
        @Builder.Default
        private TracingProperties tracing = new TracingProperties();
        @Builder.Default
        private AuditProperties audit = new AuditProperties();

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class TracingProperties {
            @Builder.Default
            private boolean enabled = true;
            private String otlpEndpoint;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AuditProperties {
            @Builder.Default
            private boolean enabled = true;
            @Builder.Default
            private String sink = "log"; // "log", "kafka", "opensearch"
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MultiTenancyProperties {
        @Builder.Default
        private boolean enabled = false;
        @Builder.Default
        private String defaultTenant = "default";
        @Builder.Default
        private String resolutionStrategy = "HEADER_THEN_JWT_THEN_SUBDOMAIN";
        @Builder.Default
        private String headerName = "X-Torana-Tenant";
    }
}
