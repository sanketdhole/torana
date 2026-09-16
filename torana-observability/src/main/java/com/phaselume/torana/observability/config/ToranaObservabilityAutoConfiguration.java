package com.phaselume.torana.observability.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.core.spi.AuditSink;
import com.phaselume.torana.observability.audit.sink.AuditSinkRegistry;
import com.phaselume.torana.observability.audit.sink.CompositeAuditSink;
import com.phaselume.torana.observability.audit.sink.LogAuditSink;
import com.phaselume.torana.observability.audit.sink.RedisStreamAuditSink;
import com.phaselume.torana.observability.health.CircuitBreakerHealthIndicator;
import com.phaselume.torana.observability.health.LiteLLMHealthIndicator;
import com.phaselume.torana.observability.health.OpaHealthIndicator;
import com.phaselume.torana.observability.health.RedisHealthIndicator;
import com.phaselume.torana.observability.metrics.ToranaMetricsConfig;
import com.phaselume.torana.observability.tracing.ToranaTracingConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Spring Boot Auto-Configuration for Torana Observability.
 */
@AutoConfiguration
@EnableConfigurationProperties(ToranaObservabilityProperties.class)
@Import({ToranaMetricsConfig.class, ToranaTracingConfig.class})
public class ToranaObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LogAuditSink logAuditSink(ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new LogAuditSink(objectMapperProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnBean(ReactiveStringRedisTemplate.class)
    @ConditionalOnMissingBean
    public RedisStreamAuditSink redisStreamAuditSink(ReactiveStringRedisTemplate redisTemplate,
                                                     ToranaObservabilityProperties properties,
                                                     ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new RedisStreamAuditSink(redisTemplate, properties.getAudit().getRedisStreamKey(), objectMapperProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditSinkRegistry auditSinkRegistry(LogAuditSink logAuditSink,
                                               ObjectProvider<RedisStreamAuditSink> redisStreamAuditSinkProvider) {
        AuditSinkRegistry registry = new AuditSinkRegistry();
        if (logAuditSink != null) {
            registry.register(logAuditSink);
        }
        RedisStreamAuditSink redisSink = redisStreamAuditSinkProvider != null ? redisStreamAuditSinkProvider.getIfAvailable() : null;
        if (redisSink != null) {
            registry.register(redisSink);
        }
        return registry;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "primaryAuditSink")
    public CompositeAuditSink primaryAuditSink(AuditSinkRegistry registry) {
        return registry.toComposite();
    }

    @Bean
    @ConditionalOnBean(ReactiveStringRedisTemplate.class)
    @ConditionalOnMissingBean
    public RedisHealthIndicator redisHealthIndicator(ReactiveStringRedisTemplate redisTemplate) {
        return new RedisHealthIndicator(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public OpaHealthIndicator opaHealthIndicator(ToranaObservabilityProperties properties,
                                                 ObjectProvider<WebClient> webClientProvider) {
        return new OpaHealthIndicator(webClientProvider.getIfAvailable(), properties.getHealth().getOpaUrl());
    }

    @Bean
    @ConditionalOnMissingBean
    public LiteLLMHealthIndicator liteLLMHealthIndicator(ToranaObservabilityProperties properties,
                                                         ObjectProvider<WebClient> webClientProvider) {
        return new LiteLLMHealthIndicator(webClientProvider.getIfAvailable(), properties.getHealth().getLitellmUrl());
    }

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerHealthIndicator circuitBreakerHealthIndicator() {
        return new CircuitBreakerHealthIndicator();
    }
}
