package com.phaselume.torana.autoconfigure;

import com.phaselume.torana.autoconfigure.connector.ConnectorRegistryAutoConfiguration;
import com.phaselume.torana.autoconfigure.pipeline.PipelineAutoConfiguration;
import com.phaselume.torana.autoconfigure.ratelimit.RateLimitAutoConfiguration;
import com.phaselume.torana.autoconfigure.resilience.ResilienceAutoConfiguration;
import com.phaselume.torana.autoconfigure.routing.RoutingAutoConfiguration;
import com.phaselume.torana.autoconfigure.security.ToranaSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Root Spring Boot Auto-Configuration orchestrating all Torana Gateway modules.
 */
@AutoConfiguration
@EnableConfigurationProperties(ToranaProperties.class)
@Import({
        ToranaPropertiesAutoConfiguration.class,
        RoutingAutoConfiguration.class,
        PipelineAutoConfiguration.class,
        ToranaSecurityAutoConfiguration.class,
        ResilienceAutoConfiguration.class,
        RateLimitAutoConfiguration.class,
        ConnectorRegistryAutoConfiguration.class
})
public class ToranaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ToranaStartupValidator toranaStartupValidator(ToranaProperties properties) {
        return new ToranaStartupValidator(properties);
    }
}
