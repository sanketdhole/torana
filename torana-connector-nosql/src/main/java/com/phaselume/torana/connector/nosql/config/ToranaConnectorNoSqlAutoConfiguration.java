package com.phaselume.torana.connector.nosql.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.connector.nosql.mongodb.MongoBackendConnector;
import com.phaselume.torana.connector.nosql.mongodb.MongoOperationGuard;
import com.phaselume.torana.connector.nosql.mongodb.MongoQueryBuilder;
import com.phaselume.torana.connector.nosql.mongodb.MongoResultSerializer;
import com.phaselume.torana.connector.nosql.mongodb.ReactiveMongoClientFactory;
import com.phaselume.torana.core.spi.BackendConnector;
import com.phaselume.torana.core.spi.ResilienceDecorator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for NoSQL (MongoDB) Backend Connector module.
 */
@AutoConfiguration
public class ToranaConnectorNoSqlAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ReactiveMongoClientFactory reactiveMongoClientFactory() {
        return new ReactiveMongoClientFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public MongoQueryBuilder mongoQueryBuilder(ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new MongoQueryBuilder(objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean
    public MongoOperationGuard mongoOperationGuard() {
        return new MongoOperationGuard();
    }

    @Bean
    @ConditionalOnMissingBean
    public MongoResultSerializer mongoResultSerializer(ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new MongoResultSerializer(objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean(name = "mongoBackendConnector")
    public BackendConnector mongoBackendConnector(ReactiveMongoClientFactory clientFactory,
                                                  MongoQueryBuilder queryBuilder,
                                                  MongoOperationGuard operationGuard,
                                                  MongoResultSerializer resultSerializer,
                                                  ObjectProvider<ResilienceDecorator> resilienceDecoratorProvider) {
        return new MongoBackendConnector(
                clientFactory,
                queryBuilder,
                operationGuard,
                resultSerializer,
                resilienceDecoratorProvider.getIfAvailable()
        );
    }
}
