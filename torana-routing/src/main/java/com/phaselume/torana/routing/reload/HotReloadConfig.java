package com.phaselume.torana.routing.reload;

import com.phaselume.torana.core.config.ToranaProperties;
import com.phaselume.torana.routing.filter.RouteContextPopulator;
import com.phaselume.torana.routing.filter.RoutingWebFilter;
import com.phaselume.torana.routing.matcher.RouteMatcher;
import com.phaselume.torana.routing.registry.RouteRegistrar;
import com.phaselume.torana.routing.registry.RouteRegistry;
import com.phaselume.torana.routing.registry.RouteRegistryRefreshListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration providing routing engine beans and hot-reload support.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "torana.routing", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HotReloadConfig {

    @Bean
    @ConditionalOnMissingBean
    public RouteMatcher routeMatcher() {
        return new RouteMatcher();
    }

    @Bean
    @ConditionalOnMissingBean
    public RouteRegistry routeRegistry(RouteMatcher routeMatcher) {
        return new RouteRegistry(routeMatcher);
    }

    @Bean
    @ConditionalOnMissingBean
    public RouteRegistrar routeRegistrar(ToranaProperties properties, RouteRegistry routeRegistry) {
        return new RouteRegistrar(properties, routeRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public RouteRegistryRefreshListener routeRegistryRefreshListener(ToranaProperties properties,
                                                                     RouteRegistry routeRegistry,
                                                                     ApplicationEventPublisher eventPublisher) {
        return new RouteRegistryRefreshListener(properties, routeRegistry, eventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public RouteContextPopulator routeContextPopulator() {
        return new RouteContextPopulator();
    }

    @Bean
    @ConditionalOnMissingBean
    public RoutingWebFilter routingWebFilter(RouteRegistry routeRegistry, RouteContextPopulator routeContextPopulator) {
        return new RoutingWebFilter(routeRegistry, routeContextPopulator);
    }

    @Bean
    @ConditionalOnMissingBean
    public RouteChangeAuditLogger routeChangeAuditLogger() {
        return new RouteChangeAuditLogger();
    }
}
