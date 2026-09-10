package com.phaselume.torana.security.vault.auth;

import com.phaselume.torana.security.vault.config.VaultProperties;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Authenticator using a static Vault token (for local dev and testing).
 */
public class TokenVaultAuthenticator implements VaultAuthenticator {

    @Override
    public boolean supports(VaultProperties properties) {
        return properties != null && "token".equalsIgnoreCase(properties.getAuthMethod()) && properties.getToken() != null && !properties.getToken().isBlank();
    }

    @Override
    public Mono<String> authenticate(WebClient webClient, VaultProperties properties) {
        if (properties == null || properties.getToken() == null || properties.getToken().isBlank()) {
            return Mono.error(new IllegalStateException("Vault static token is not configured"));
        }
        return Mono.just(properties.getToken());
    }
}
