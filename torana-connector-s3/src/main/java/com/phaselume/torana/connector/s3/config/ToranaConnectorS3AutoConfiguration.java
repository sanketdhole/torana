package com.phaselume.torana.connector.s3.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.connector.s3.S3BackendConnector;
import com.phaselume.torana.connector.s3.client.S3AsyncClientFactory;
import com.phaselume.torana.connector.s3.client.S3OperationGuard;
import com.phaselume.torana.core.spi.BackendConnector;
import com.phaselume.torana.core.spi.ResilienceDecorator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for S3 Backend Connector module.
 */
@AutoConfiguration
public class ToranaConnectorS3AutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public S3AsyncClientFactory s3AsyncClientFactory() {
        return new S3AsyncClientFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public S3OperationGuard s3OperationGuard() {
        return new S3OperationGuard();
    }

    @Bean
    @ConditionalOnMissingBean(name = "s3BackendConnector")
    public BackendConnector s3BackendConnector(S3AsyncClientFactory clientFactory,
                                               S3OperationGuard operationGuard,
                                               ObjectProvider<ResilienceDecorator> resilienceDecoratorProvider,
                                               ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new S3BackendConnector(
                clientFactory,
                operationGuard,
                resilienceDecoratorProvider.getIfAvailable(),
                objectMapperProvider.getIfAvailable(ObjectMapper::new)
        );
    }
}
