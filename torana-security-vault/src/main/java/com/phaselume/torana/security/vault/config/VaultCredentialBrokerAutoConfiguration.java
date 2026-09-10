package com.phaselume.torana.security.vault.config;

import com.phaselume.torana.core.spi.CredentialBroker;
import com.phaselume.torana.security.vault.auth.AppRoleVaultAuthenticator;
import com.phaselume.torana.security.vault.auth.KubernetesVaultAuthenticator;
import com.phaselume.torana.security.vault.auth.TokenVaultAuthenticator;
import com.phaselume.torana.security.vault.auth.VaultAuthenticator;
import com.phaselume.torana.security.vault.auth.VaultAuthenticatorChain;
import com.phaselume.torana.security.vault.client.VaultCredentialBroker;
import com.phaselume.torana.security.vault.client.VaultCredentialCache;
import com.phaselume.torana.security.vault.client.VaultReactiveClient;
import com.phaselume.torana.security.vault.client.VaultTokenRenewer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Auto-configuration for HashiCorp Vault credential broker.
 */
@AutoConfiguration
@EnableConfigurationProperties(VaultProperties.class)
@ConditionalOnProperty(prefix = "torana.security.credential-broker.vault", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VaultCredentialBrokerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TokenVaultAuthenticator tokenVaultAuthenticator() {
        return new TokenVaultAuthenticator();
    }

    @Bean
    @ConditionalOnMissingBean
    public KubernetesVaultAuthenticator kubernetesVaultAuthenticator() {
        return new KubernetesVaultAuthenticator();
    }

    @Bean
    @ConditionalOnMissingBean
    public AppRoleVaultAuthenticator appRoleVaultAuthenticator() {
        return new AppRoleVaultAuthenticator();
    }

    @Bean
    @ConditionalOnMissingBean
    public VaultAuthenticatorChain vaultAuthenticatorChain(List<VaultAuthenticator> authenticators) {
        return new VaultAuthenticatorChain(authenticators);
    }

    @Bean
    @ConditionalOnMissingBean
    public VaultReactiveClient vaultReactiveClient(VaultProperties properties, VaultAuthenticatorChain chain) {
        return new VaultReactiveClient(WebClient.builder(), properties, chain);
    }

    @Bean
    @ConditionalOnMissingBean(CredentialBroker.class)
    public VaultCredentialBroker vaultCredentialBroker(VaultReactiveClient vaultClient, VaultProperties properties) {
        return new VaultCredentialBroker(vaultClient, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public VaultCredentialCache vaultCredentialCache() {
        return new VaultCredentialCache();
    }

    @Bean
    @ConditionalOnMissingBean
    public VaultTokenRenewer vaultTokenRenewer(VaultReactiveClient vaultClient, VaultProperties properties) {
        return new VaultTokenRenewer(vaultClient, properties);
    }
}
