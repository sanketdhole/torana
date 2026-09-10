package com.phaselume.torana.observability.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for Torana Observability subsystem.
 */
@Data
@ConfigurationProperties(prefix = "torana.observability")
public class ToranaObservabilityProperties {

    private MetricsProperties metrics = new MetricsProperties();
    private TracingProperties tracing = new TracingProperties();
    private AuditProperties audit = new AuditProperties();
    private HealthProperties health = new HealthProperties();

    @Data
    public static class MetricsProperties {
        private boolean enabled = true;
        private boolean includeRouteTags = true;
        private boolean includeConnectorTags = true;
    }

    @Data
    public static class TracingProperties {
        private boolean enabled = true;
        private double sampleRate = 1.0;
        private String exporter = "otlp";
        private String otlpEndpoint = "http://localhost:4317";
        private String propagation = "W3C";
    }

    @Data
    public static class AuditProperties {
        private boolean enabled = true;
        private String redisStreamKey = "torana:audit:stream";
        private List<String> redactFields = new ArrayList<>();
    }

    @Data
    public static class HealthProperties {
        private String opaUrl = "http://localhost:8181/health";
        private String litellmUrl = "http://localhost:4000/health";
    }
}
