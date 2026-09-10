package com.phaselume.torana.autoconfigure.routing;

import com.phaselume.torana.routing.registry.RouteRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Torana RouteRegistry.
 */
@AutoConfiguration
@ConditionalOnClass(RouteRegistry.class)
public class RoutingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RouteRegistry routeRegistry() {
        return new RouteRegistry();
    }
}
