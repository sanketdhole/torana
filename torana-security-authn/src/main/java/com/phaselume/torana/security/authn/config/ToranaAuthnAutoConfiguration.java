package com.phaselume.torana.security.authn.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaselume.torana.core.config.ToranaProperties;
import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthnProperties;
import com.phaselume.torana.core.spi.AuthenticationProvider;
import com.phaselume.torana.security.authn.apikey.ApiKeyAuthenticationProvider;
import com.phaselume.torana.security.authn.apikey.ApiKeyRotationSupport;
import com.phaselume.torana.security.authn.apikey.ApiKeyValidator;
import com.phaselume.torana.security.authn.chain.AuthenticationProviderChain;
import com.phaselume.torana.security.authn.chain.AuthenticationProviderChain.Mode;
import com.phaselume.torana.security.authn.filter.ToranaAuthenticationWebFilter;
import com.phaselume.torana.security.authn.jwt.JwksCache;
import com.phaselume.torana.security.authn.jwt.JwtClaimsExtractor;
import com.phaselume.torana.security.authn.jwt.JwtOidcAuthenticationProvider;
import com.phaselume.torana.security.authn.jwt.MultiIssuerJwtDecoder;
import com.phaselume.torana.security.authn.mtls.CertificateDnExtractor;
import com.phaselume.torana.security.authn.mtls.ClientCertificateValidator;
import com.phaselume.torana.security.authn.mtls.CrlChecker;
import com.phaselume.torana.security.authn.mtls.MtlsAuthenticationProvider;
import com.phaselume.torana.security.authn.outbound.InternalJwtMinter;
import com.phaselume.torana.security.authn.outbound.OutboundAuthExchangeFilter;
import com.phaselume.torana.security.authn.outbound.OutboundAuthPolicyEngine;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto-configuration for Torana Security Authentication (JWT/OIDC, API Key, mTLS, and Outbound identity).
 */
@AutoConfiguration
@EnableConfigurationProperties(ToranaProperties.class)
@ConditionalOnProperty(prefix = "torana.security.authn", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ToranaAuthnAutoConfiguration {

    // ─── JWT / OIDC Infrastructure ─────────────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean
    public JwksCache jwksCache(ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider,
                               ToranaProperties properties) {
        ReactiveStringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        return new JwksCache(redis, "torana:jwks:");
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtClaimsExtractor jwtClaimsExtractor(ToranaProperties properties) {
        return new JwtClaimsExtractor(properties.getSecurity().getAuthn().getJwt());
    }

    @Bean
    @ConditionalOnMissingBean
    public MultiIssuerJwtDecoder multiIssuerJwtDecoder(ToranaProperties properties,
                                                       JwksCache jwksCache,
                                                       ObjectProvider<WebClient.Builder> webClientBuilderProvider) {
        return new MultiIssuerJwtDecoder(
                properties.getSecurity().getAuthn().getJwt(),
                jwksCache,
                webClientBuilderProvider.getIfAvailable()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtOidcAuthenticationProvider jwtOidcAuthenticationProvider(MultiIssuerJwtDecoder jwtDecoder,
                                                                       JwtClaimsExtractor claimsExtractor) {
        return new JwtOidcAuthenticationProvider(jwtDecoder, claimsExtractor);
    }

    // ─── API Key Infrastructure ────────────────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean
    public ApiKeyValidator apiKeyValidator(ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider,
                                           ObjectProvider<ObjectMapper> objectMapperProvider,
                                           ToranaProperties properties) {
        return new ApiKeyValidator(
                redisTemplateProvider.getIfAvailable(),
                properties.getSecurity().getAuthn().getApiKey().getRedisPrefix(),
                objectMapperProvider.getIfAvailable()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiKeyRotationSupport apiKeyRotationSupport(ApiKeyValidator validator,
                                                       ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider,
                                                       ToranaProperties properties) {
        return new ApiKeyRotationSupport(
                validator,
                redisTemplateProvider.getIfAvailable(),
                properties.getSecurity().getAuthn().getApiKey().getRedisPrefix()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiKeyAuthenticationProvider apiKeyAuthenticationProvider(ApiKeyValidator validator,
                                                                     ToranaProperties properties) {
        return new ApiKeyAuthenticationProvider(validator, properties.getSecurity().getAuthn().getApiKey());
    }

    // ─── mTLS Infrastructure ───────────────────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean
    public CrlChecker crlChecker(ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider) {
        return new CrlChecker(redisTemplateProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public CertificateDnExtractor certificateDnExtractor() {
        return new CertificateDnExtractor();
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientCertificateValidator clientCertificateValidator(CrlChecker crlChecker,
                                                                 ToranaProperties properties) {
        AuthnProperties.MtlsConfig mtls = properties.getSecurity().getAuthn().getMtls();
        return new ClientCertificateValidator(crlChecker, mtls.isCrlCheckEnabled(), mtls.getCrlCacheTtlSeconds());
    }

    @Bean
    @ConditionalOnMissingBean
    public MtlsAuthenticationProvider mtlsAuthenticationProvider(ClientCertificateValidator validator,
                                                                 CertificateDnExtractor dnExtractor,
                                                                 ToranaProperties properties) {
        return new MtlsAuthenticationProvider(validator, dnExtractor, properties.getSecurity().getAuthn().getMtls());
    }

    // ─── Outbound Identity & JWT Minter ────────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean
    public InternalJwtMinter internalJwtMinter(ToranaProperties properties) {
        return new InternalJwtMinter(properties.getSecurity().getAuthn().getOutbound().getJwtSigner());
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboundAuthPolicyEngine outboundAuthPolicyEngine(ToranaProperties properties,
                                                             InternalJwtMinter jwtMinter) {
        return new OutboundAuthPolicyEngine(properties.getSecurity().getAuthn().getOutbound(), jwtMinter);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboundAuthExchangeFilter outboundAuthExchangeFilter(OutboundAuthPolicyEngine policyEngine) {
        return new OutboundAuthExchangeFilter(policyEngine);
    }

    // ─── Provider Chain & WebFilter ────────────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationProviderChain authenticationProviderChain(
            List<AuthenticationProvider> providers,
            ToranaProperties properties) {
        AuthnProperties authn = properties.getSecurity().getAuthn();
        Mode mode = Mode.fromString(authn.getChainMode());
        return new AuthenticationProviderChain(providers, mode, authn.isAllowAnonymous());
    }

    @Bean
    @ConditionalOnMissingBean
    public ToranaAuthenticationWebFilter toranaAuthenticationWebFilter(AuthenticationProviderChain providerChain) {
        return new ToranaAuthenticationWebFilter(providerChain);
    }
}
