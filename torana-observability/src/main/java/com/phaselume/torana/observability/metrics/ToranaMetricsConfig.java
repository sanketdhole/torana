package com.phaselume.torana.observability.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metric recorders configuration registering Micrometer meter wrappers.
 */
@Configuration
public class ToranaMetricsConfig {

    @Bean
    @ConditionalOnMissingBean
    public RouteMetricsRecorder routeMetricsRecorder(MeterRegistry meterRegistry) {
        return new RouteMetricsRecorder(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConnectorMetricsRecorder connectorMetricsRecorder(MeterRegistry meterRegistry) {
        return new ConnectorMetricsRecorder(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public StreamingMetricsRecorder streamingMetricsRecorder(MeterRegistry meterRegistry) {
        return new StreamingMetricsRecorder(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthMetricsRecorder authMetricsRecorder(MeterRegistry meterRegistry) {
        return new AuthMetricsRecorder(meterRegistry);
    }
}
