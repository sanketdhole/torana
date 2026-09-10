package com.phaselume.torana.autoconfigure.ratelimit;

import com.phaselume.torana.ratelimit.redis.RateLimitKeyResolver;
import com.phaselume.torana.ratelimit.redis.RateLimitPolicyRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Torana Rate Limiting subsystem.
 */
@AutoConfiguration
@ConditionalOnClass(RateLimitPolicyRegistry.class)
public class RateLimitAutoConfiguration {

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
}
