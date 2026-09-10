package com.phaselume.torana.connector.jdbc.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.connector.jdbc.R2dbcBackendConnector;
import com.phaselume.torana.connector.jdbc.query.ResultSetSerializer;
import com.phaselume.torana.connector.jdbc.query.SqlOperationGuard;
import com.phaselume.torana.connector.jdbc.query.SqlQueryExtractor;
import com.phaselume.torana.connector.jdbc.r2dbc.R2dbcConnectionFactory;
import com.phaselume.torana.connector.jdbc.security.SchemaAllowList;
import com.phaselume.torana.core.spi.BackendConnector;
import com.phaselume.torana.core.spi.ResilienceDecorator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for R2DBC SQL Connector module.
 */
@AutoConfiguration
public class ToranaConnectorJdbcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public R2dbcConnectionFactory r2dbcConnectionFactory() {
        return new R2dbcConnectionFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public SqlQueryExtractor sqlQueryExtractor(ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new SqlQueryExtractor(objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean
    public SqlOperationGuard sqlOperationGuard() {
        return new SqlOperationGuard();
    }

    @Bean
    @ConditionalOnMissingBean
    public SchemaAllowList schemaAllowList() {
        return new SchemaAllowList();
    }

    @Bean
    @ConditionalOnMissingBean
    public ResultSetSerializer resultSetSerializer(ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new ResultSetSerializer(objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean(name = "r2dbcBackendConnector")
    public BackendConnector r2dbcBackendConnector(R2dbcConnectionFactory connectionFactory,
                                                  SqlQueryExtractor queryExtractor,
                                                  SqlOperationGuard operationGuard,
                                                  SchemaAllowList schemaAllowList,
                                                  ResultSetSerializer resultSerializer,
                                                  ObjectProvider<ResilienceDecorator> resilienceDecoratorProvider) {
        return new R2dbcBackendConnector(
                connectionFactory,
                queryExtractor,
                operationGuard,
                schemaAllowList,
                resultSerializer,
                resilienceDecoratorProvider.getIfAvailable()
        );
    }
}
