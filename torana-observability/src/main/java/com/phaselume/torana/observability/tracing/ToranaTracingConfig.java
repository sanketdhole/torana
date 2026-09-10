package com.phaselume.torana.observability.tracing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tracing configuration registering distributed tracing filters and components.
 */
@Configuration
public class ToranaTracingConfig {

    @Bean
    @ConditionalOnMissingBean
    public RequestTracingFilter requestTracingFilter() {
        return new RequestTracingFilter();
    }
}
