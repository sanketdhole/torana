package com.phaselume.torana.resilience.config;

import com.phaselume.torana.core.spi.ResilienceDecorator;
import com.phaselume.torana.resilience.decorator.FallbackResponseFactory;
import com.phaselume.torana.resilience.decorator.Resilience4jResilienceDecorator;
import com.phaselume.torana.resilience.decorator.ResilienceEventListener;
import com.phaselume.torana.resilience.registry.ConnectorResilienceRegistry;
import com.phaselume.torana.resilience.registry.ResilienceInstanceFactory;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

/**
 * Auto-configuration for Torana Resilience4j module.
 */
@AutoConfiguration
public class ToranaResilienceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ResilienceProfileRegistry resilienceProfileRegistry() {
        return new ResilienceProfileRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public Resilience4jConfigFactory resilience4jConfigFactory() {
        return new Resilience4jConfigFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConnectorResilienceRegistry connectorResilienceRegistry() {
        return new ConnectorResilienceRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public ResilienceInstanceFactory resilienceInstanceFactory(ConnectorResilienceRegistry registry,
                                                               ResilienceProfileRegistry profileRegistry,
                                                               Resilience4jConfigFactory configFactory) {
        return new ResilienceInstanceFactory(registry, profileRegistry, configFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public FallbackResponseFactory fallbackResponseFactory() {
        return new FallbackResponseFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public ResilienceEventListener resilienceEventListener(ConnectorResilienceRegistry registry,
                                                           ObjectProvider<MeterRegistry> meterRegistryProvider) {
        return new ResilienceEventListener(registry, meterRegistryProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(ResilienceDecorator.class)
    public ResilienceDecorator resilienceDecorator(ResilienceInstanceFactory instanceFactory,
                                                   ResilienceEventListener eventListener,
                                                   FallbackResponseFactory fallbackResponseFactory) {
        return new Resilience4jResilienceDecorator(instanceFactory, eventListener, fallbackResponseFactory);
    }
}
