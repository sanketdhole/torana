package com.phaselume.torana.autoconfigure.security;

import com.phaselume.torana.core.spi.AuthenticationProvider;
import com.phaselume.torana.security.authn.chain.AuthenticationProviderChain;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Auto-configuration for Torana Security and Authentication providers.
 */
@AutoConfiguration
@ConditionalOnClass(AuthenticationProviderChain.class)
public class ToranaSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationProviderChain authenticationProviderChain(List<AuthenticationProvider> providers) {
        return new AuthenticationProviderChain(providers);
    }
}
