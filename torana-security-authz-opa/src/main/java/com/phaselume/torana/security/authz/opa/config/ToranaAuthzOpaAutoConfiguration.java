package com.phaselume.torana.security.authz.opa.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.core.config.ToranaProperties;
import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthzProperties;
import com.phaselume.torana.core.spi.AuthorizationEngine;
import com.phaselume.torana.security.authz.opa.cache.CacheInvalidator;
import com.phaselume.torana.security.authz.opa.cache.OpaDecisionCache;
import com.phaselume.torana.security.authz.opa.client.OpaClient;
import com.phaselume.torana.security.authz.opa.client.OpaHealthIndicator;
import com.phaselume.torana.security.authz.opa.client.OpaInputBuilder;
import com.phaselume.torana.security.authz.opa.engine.OpaAuthorizationEngine;
import com.phaselume.torana.security.authz.opa.filter.AccessControlWebFilter;
import com.phaselume.torana.security.authz.opa.filter.ObligationProcessor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Spring Boot Auto-configuration for Open Policy Agent (OPA) Authorization Engine.
 */
@AutoConfiguration
@EnableConfigurationProperties(ToranaProperties.class)
@ConditionalOnProperty(prefix = "torana.security.authz", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ToranaAuthzOpaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OpaInputBuilder opaInputBuilder() {
        return new OpaInputBuilder();
    }

    @Bean
    @ConditionalOnMissingBean
    public ObligationProcessor obligationProcessor() {
        return new ObligationProcessor();
    }

    @Bean
    @ConditionalOnMissingBean
    public OpaClient opaClient(ObjectProvider<WebClient.Builder> webClientBuilderProvider,
                               ObjectProvider<ObjectMapper> objectMapperProvider,
                               ToranaProperties properties) {
        return new OpaClient(
                webClientBuilderProvider.getIfAvailable(),
                properties.getSecurity().getAuthz(),
                objectMapperProvider.getIfAvailable()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public OpaHealthIndicator opaHealthIndicator(ObjectProvider<WebClient.Builder> webClientBuilderProvider,
                                                 ToranaProperties properties) {
        return new OpaHealthIndicator(
                webClientBuilderProvider.getIfAvailable(),
                properties.getSecurity().getAuthz()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public OpaDecisionCache opaDecisionCache(ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider,
                                             ObjectProvider<ObjectMapper> objectMapperProvider,
                                             ToranaProperties properties) {
        return new OpaDecisionCache(
                redisTemplateProvider.getIfAvailable(),
                properties.getSecurity().getAuthz().getCache(),
                objectMapperProvider.getIfAvailable()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheInvalidator cacheInvalidator(OpaDecisionCache decisionCache,
                                             ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider,
                                             ToranaProperties properties) {
        return new CacheInvalidator(
                decisionCache,
                redisTemplateProvider.getIfAvailable(),
                properties.getSecurity().getAuthz().getCache()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationEngine authorizationEngine(OpaClient opaClient,
                                                   OpaInputBuilder inputBuilder,
                                                   OpaDecisionCache decisionCache,
                                                   ObligationProcessor obligationProcessor,
                                                   ToranaProperties properties) {
        return new OpaAuthorizationEngine(
                opaClient,
                inputBuilder,
                decisionCache,
                obligationProcessor,
                properties.getSecurity().getAuthz()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessControlWebFilter accessControlWebFilter(AuthorizationEngine authorizationEngine) {
        return new AccessControlWebFilter(authorizationEngine);
    }
}
