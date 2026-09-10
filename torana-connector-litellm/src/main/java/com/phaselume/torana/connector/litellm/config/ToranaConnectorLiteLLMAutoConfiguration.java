package com.phaselume.torana.connector.litellm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.connector.litellm.LiteLLMConnector;
import com.phaselume.torana.connector.litellm.client.LiteLLMHealthIndicator;
import com.phaselume.torana.connector.litellm.client.LiteLLMRequestBuilder;
import com.phaselume.torana.connector.litellm.client.LiteLLMWebClient;
import com.phaselume.torana.connector.litellm.streaming.ChunkToAgentResponseMapper;
import com.phaselume.torana.connector.litellm.streaming.SseChunkParser;
import com.phaselume.torana.core.spi.BackendConnector;
import com.phaselume.torana.core.spi.ResilienceDecorator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configuration for LiteLLM / OpenAI-compatible model connector.
 */
@AutoConfiguration
public class ToranaConnectorLiteLLMAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SseChunkParser sseChunkParser(ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new SseChunkParser(objectMapperProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public ChunkToAgentResponseMapper chunkToAgentResponseMapper() {
        return new ChunkToAgentResponseMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public LiteLLMRequestBuilder liteLLMRequestBuilder(ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new LiteLLMRequestBuilder(objectMapperProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public LiteLLMWebClient liteLLMWebClient(ObjectProvider<WebClient.Builder> webClientBuilderProvider) {
        return new LiteLLMWebClient(webClientBuilderProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public LiteLLMHealthIndicator liteLLMHealthIndicator(ObjectProvider<WebClient.Builder> webClientBuilderProvider) {
        return new LiteLLMHealthIndicator(webClientBuilderProvider.getIfAvailable(), "http://localhost:4000");
    }

    @Bean
    @ConditionalOnMissingBean(name = "litellmBackendConnector")
    public BackendConnector litellmBackendConnector(LiteLLMWebClient clientFactory,
                                                    LiteLLMRequestBuilder requestBuilder,
                                                    SseChunkParser sseParser,
                                                    ChunkToAgentResponseMapper responseMapper,
                                                    ObjectProvider<ResilienceDecorator> resilienceDecoratorProvider) {
        return new LiteLLMConnector(
                clientFactory,
                requestBuilder,
                sseParser,
                responseMapper,
                resilienceDecoratorProvider.getIfAvailable()
        );
    }
}
