package com.phaselume.torana.autoconfigure.resilience;

import com.phaselume.torana.resilience.config.ResilienceProfileRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Torana Resilience subsystem.
 */
@AutoConfiguration
@ConditionalOnClass(ResilienceProfileRegistry.class)
public class ResilienceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ResilienceProfileRegistry resilienceProfileRegistry() {
        return new ResilienceProfileRegistry();
    }
}
