package com.phaselume.torana.security.authn.apikey;

import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthnProperties.ApiKeyConfig;
import com.phaselume.torana.core.model.ToranaAuthentication;
import com.phaselume.torana.core.spi.AuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Authentication provider for API keys stored in Redis or in-memory repository.
 */
public class ApiKeyAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationProvider.class);

    private final ApiKeyValidator validator;
    private final ApiKeyConfig config;

    public ApiKeyAuthenticationProvider(ApiKeyValidator validator, ApiKeyConfig config) {
        this.validator = validator;
        this.config = config != null ? config : new ApiKeyConfig();
    }

    @Override
    public String type() {
        return "apikey";
    }

    @Override
    public Mono<ToranaAuthentication> authenticate(ServerWebExchange exchange) {
        if (exchange == null || exchange.getRequest() == null) {
            return Mono.empty();
        }

        String rawKey = exchange.getRequest().getHeaders().getFirst(config.getHeader());

        if ((rawKey == null || rawKey.isBlank()) && config.isAllowQueryParam()) {
            rawKey = exchange.getRequest().getQueryParams().getFirst(config.getQueryParamName());
        }

        if (rawKey == null || rawKey.isBlank()) {
            return Mono.empty();
        }

        final String extractedKey = rawKey.trim();

        return validator.validate(extractedKey)
                .map(metadata -> {
                    String principalId = metadata.getPrincipalId() != null ? metadata.getPrincipalId() : metadata.getName();
                    return ToranaAuthentication.builder()
                            .principalId(principalId)
                            .name(metadata.getName() != null ? metadata.getName() : principalId)
                            .tenantId(metadata.getTenantId() != null ? metadata.getTenantId() : "default")
                            .authMethod("apikey")
                            .roles(metadata.getRoles())
                            .scopes(metadata.getScopes())
                            .claims(metadata.getAttributes() != null ? metadata.getAttributes() : Map.of())
                            .rawToken(extractedKey)
                            .build();
                })
                .doOnError(err -> log.debug("API key authentication failed: {}", err.getMessage()));
    }
}
