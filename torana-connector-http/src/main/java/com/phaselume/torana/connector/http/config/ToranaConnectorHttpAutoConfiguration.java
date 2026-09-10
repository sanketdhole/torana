package com.phaselume.torana.connector.http.config;

import com.phaselume.torana.connector.http.HttpBackendConnector;
import com.phaselume.torana.connector.http.auth.HttpConnectorAuthInjector;
import com.phaselume.torana.connector.http.proxy.HttpConnectorWebClientFactory;
import com.phaselume.torana.connector.http.proxy.RequestForwardingStrategy;
import com.phaselume.torana.connector.http.proxy.ResponseMappingStrategy;
import com.phaselume.torana.core.spi.BackendConnector;
import com.phaselume.torana.core.spi.ResilienceDecorator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configuration for the generic HTTP microservice connector.
 */
@AutoConfiguration
public class ToranaConnectorHttpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public HttpConnectorAuthInjector httpConnectorAuthInjector() {
        return new HttpConnectorAuthInjector();
    }

    @Bean
    @ConditionalOnMissingBean
    public RequestForwardingStrategy requestForwardingStrategy() {
        return new RequestForwardingStrategy();
    }

    @Bean
    @ConditionalOnMissingBean
    public ResponseMappingStrategy responseMappingStrategy() {
        return new ResponseMappingStrategy();
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpConnectorWebClientFactory httpConnectorWebClientFactory(ObjectProvider<WebClient.Builder> webClientBuilderProvider) {
        return new HttpConnectorWebClientFactory(webClientBuilderProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(name = "httpBackendConnector")
    public BackendConnector httpBackendConnector(HttpConnectorWebClientFactory clientFactory,
                                                 RequestForwardingStrategy forwardingStrategy,
                                                 ResponseMappingStrategy mappingStrategy,
                                                 HttpConnectorAuthInjector authInjector,
                                                 ObjectProvider<ResilienceDecorator> resilienceDecoratorProvider) {
        return new HttpBackendConnector(
                clientFactory,
                forwardingStrategy,
                mappingStrategy,
                authInjector,
                resilienceDecoratorProvider.getIfAvailable()
        );
    }
}
