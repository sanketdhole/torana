package com.phaselume.torana.streaming.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.streaming.backpressure.BackpressureOperator;
import com.phaselume.torana.streaming.backpressure.SlowConsumerDetector;
import com.phaselume.torana.streaming.ndjson.NdjsonResponseWriter;
import com.phaselume.torana.streaming.sse.SseEventFormatter;
import com.phaselume.torana.streaming.sse.SseHeartbeatEmitter;
import com.phaselume.torana.streaming.sse.SseResponseWriter;
import com.phaselume.torana.streaming.websocket.WebSocketChunkWriter;
import com.phaselume.torana.streaming.websocket.WebSocketErrorHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot AutoConfiguration for Torana Streaming subsystem.
 */
@AutoConfiguration
@EnableConfigurationProperties(ToranaStreamingProperties.class)
@ConditionalOnProperty(prefix = "torana.streaming", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ToranaStreamingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SseEventFormatter sseEventFormatter(ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new SseEventFormatter(objectMapperProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public SseHeartbeatEmitter sseHeartbeatEmitter(ToranaStreamingProperties properties) {
        return new SseHeartbeatEmitter(properties.getSse().getHeartbeatInterval());
    }

    @Bean
    @ConditionalOnMissingBean
    public SseResponseWriter sseResponseWriter(SseEventFormatter formatter, SseHeartbeatEmitter heartbeatEmitter) {
        return new SseResponseWriter(formatter, heartbeatEmitter);
    }

    @Bean
    @ConditionalOnMissingBean
    public NdjsonResponseWriter ndjsonResponseWriter(ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new NdjsonResponseWriter(objectMapperProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public WebSocketChunkWriter webSocketChunkWriter(ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new WebSocketChunkWriter(objectMapperProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public WebSocketErrorHandler webSocketErrorHandler(ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new WebSocketErrorHandler(objectMapperProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public BackpressureOperator backpressureOperator(ToranaStreamingProperties properties) {
        return new BackpressureOperator(properties.getBackpressure());
    }

    @Bean
    @ConditionalOnMissingBean
    public SlowConsumerDetector slowConsumerDetector(ToranaStreamingProperties properties) {
        return new SlowConsumerDetector(properties.getBackpressure().getSlowConsumerTimeout());
    }
}
