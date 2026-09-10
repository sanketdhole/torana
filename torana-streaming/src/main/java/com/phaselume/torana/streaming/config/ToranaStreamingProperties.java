package com.phaselume.torana.streaming.config;

import com.phaselume.torana.streaming.backpressure.BackpressureConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for Torana streaming layer.
 */
@Data
@ConfigurationProperties(prefix = "torana.streaming")
public class ToranaStreamingProperties {

    private boolean enabled = true;
    private SseProperties sse = new SseProperties();
    private BackpressureConfig backpressure = new BackpressureConfig();
    private boolean ndjsonEnabled = true;

    @Data
    public static class SseProperties {
        private boolean heartbeatEnabled = true;
        private Duration heartbeatInterval = Duration.ofSeconds(15);
    }
}
