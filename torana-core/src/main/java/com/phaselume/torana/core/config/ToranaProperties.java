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
            private String chainMode = "first-match"; // first-match, all-required, any-of
            @Builder.Default
            private boolean allowAnonymous = false;
            @Builder.Default
            private JwtConfig jwt = new JwtConfig();
            @Builder.Default
            private ApiKeyConfig apiKey = new ApiKeyConfig();
            @Builder.Default
            private MtlsConfig mtls = new MtlsConfig();
            @Builder.Default
            private OutboundAuthnConfig outbound = new OutboundAuthnConfig();
            @Builder.Default
            private Map<String, Object> providers = new HashMap<>();

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class JwtConfig {
                @Builder.Default
                private boolean enabled = true;
                @Builder.Default
                private List<JwtIssuerConfig> issuers = new ArrayList<>();
                @Builder.Default
                private String defaultAudience = "torana-gateway";
                @Builder.Default
                private long jwksCacheTtlSeconds = 300;
                @Builder.Default
                private String scopeClaim = "scp";
                @Builder.Default
                private String rolesClaim = "roles";
                @Builder.Default
                private String tenantClaim = "tenant_id";
                @Builder.Default
                private String principalClaim = "sub";
            }

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class JwtIssuerConfig {
                private String uri;
                private String jwksUri;
                private String audience;
                private String scopeClaim;
                private String rolesClaim;
                private String tenantClaim;
                private String principalClaim;
                @Builder.Default
                private long jwksCacheTtlSeconds = 300;
            }

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class ApiKeyConfig {
                @Builder.Default
                private boolean enabled = true;
                @Builder.Default
                private String header = "X-Api-Key";
                @Builder.Default
                private boolean allowQueryParam = false;
                @Builder.Default
                private String queryParamName = "api_key";
                @Builder.Default
                private String redisPrefix = "torana:apikey:";
                @Builder.Default
                private long rotationGracePeriodSeconds = 86400; // 24 hours
            }

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class MtlsConfig {
                @Builder.Default
                private boolean enabled = false;
                private String trustStore;
                private String trustStorePassword;
                @Builder.Default
                private String principalField = "CN"; // CN, OU, SAN, EMAIL
                @Builder.Default
                private String clientCertHeader = "X-Forwarded-Client-Cert";
                @Builder.Default
                private boolean crlCheckEnabled = false;
                @Builder.Default
                private long crlCacheTtlSeconds = 3600;
            }

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class OutboundAuthnConfig {
                @Builder.Default
                private boolean enabled = true;
                @Builder.Default
                private String defaultPolicy = "MINT_INTERNAL_JWT"; // MINT_INTERNAL_JWT, FORWARD_CALLER_TOKEN, INJECT_API_KEY, PROPAGATE_IDENTITY_HEADERS, NONE
                @Builder.Default
                private JwtSignerConfig jwtSigner = new JwtSignerConfig();
                @Builder.Default
                private Map<String, ServiceOutboundConfig> services = new HashMap<>();

                @Data
                @Builder
                @NoArgsConstructor
                @AllArgsConstructor
                public static class JwtSignerConfig {
                    @Builder.Default
                    private String issuer = "torana-gateway";
                    @Builder.Default
                    private String secret = "torana-internal-cluster-secret-key-change-me-in-production-32bytes";
                    @Builder.Default
                    private long tokenTtlSeconds = 300; // 5 mins
                    @Builder.Default
                    private String algorithm = "HS256"; // HS256, RS256
                    private String privateKeyPem;
                    @Builder.Default
                    private String keyId = "torana-gw-1";
                }

                @Data
                @Builder
                @NoArgsConstructor
                @AllArgsConstructor
                public static class ServiceOutboundConfig {
                    private String policy; // override defaultPolicy for this service
                    private String audience;
                    private String apiKeyHeader;
                    private String apiKeyValue;
                    private String bearerToken;
                    @Builder.Default
                    private Map<String, String> customHeaders = new HashMap<>();
                }
            }
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
