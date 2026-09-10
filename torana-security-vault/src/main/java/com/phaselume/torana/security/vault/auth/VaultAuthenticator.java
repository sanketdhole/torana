package com.phaselume.torana.security.vault.auth;

import com.phaselume.torana.security.vault.config.VaultProperties;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Strategy interface for authenticating to HashiCorp Vault.
 */
public interface VaultAuthenticator {

    /**
     * Determines if this authenticator supports the given configuration.
     */
    boolean supports(VaultProperties properties);

    /**
     * Authenticates against Vault and returns a client token Mono.
     */
    Mono<String> authenticate(WebClient webClient, VaultProperties properties);
}
