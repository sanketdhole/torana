package com.phaselume.torana.ratelimit.config;

import com.phaselume.torana.core.spi.RateLimiter;
import com.phaselume.torana.ratelimit.filter.RateLimitResponseWriter;
import com.phaselume.torana.ratelimit.filter.RateLimitWebFilter;
import com.phaselume.torana.ratelimit.redis.RateLimitKeyResolver;
import com.phaselume.torana.ratelimit.redis.RateLimitPolicyRegistry;
import com.phaselume.torana.ratelimit.redis.RedisRateLimiter;
import com.phaselume.torana.ratelimit.redis.SlidingWindowLuaScript;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

/**
 * Auto-configuration for Torana Redis Rate Limiting module.
 */
@AutoConfiguration
public class ToranaRateLimitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RateLimitPolicyRegistry rateLimitPolicyRegistry() {
        return new RateLimitPolicyRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitKeyResolver rateLimitKeyResolver() {
        return new RateLimitKeyResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public SlidingWindowLuaScript slidingWindowLuaScript(ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider) {
        return new SlidingWindowLuaScript(redisTemplateProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(RateLimiter.class)
    public RateLimiter redisRateLimiter(SlidingWindowLuaScript luaScript,
                                        RateLimitKeyResolver keyResolver,
                                        RateLimitPolicyRegistry policyRegistry) {
        return new RedisRateLimiter(luaScript, keyResolver, policyRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitResponseWriter rateLimitResponseWriter() {
        return new RateLimitResponseWriter();
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitWebFilter rateLimitWebFilter(RateLimiter rateLimiter,
                                                 RateLimitPolicyRegistry policyRegistry,
                                                 RateLimitResponseWriter responseWriter) {
        return new RateLimitWebFilter(rateLimiter, policyRegistry, responseWriter);
    }
}
