package com.phaselume.torana.security.vault.auth;

import com.phaselume.torana.security.vault.config.VaultProperties;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Evaluates authenticators in chain order until an authenticator succeeds.
 */
public class VaultAuthenticatorChain {

    private final List<VaultAuthenticator> authenticators;

    public VaultAuthenticatorChain(List<VaultAuthenticator> authenticators) {
        this.authenticators = (authenticators != null) ? authenticators : List.of();
    }

    public Mono<String> authenticate(WebClient webClient, VaultProperties properties) {
        return Flux.fromIterable(authenticators)
                .filter(auth -> auth.supports(properties))
                .concatMap(auth -> auth.authenticate(webClient, properties)
                        .onErrorResume(e -> Mono.empty()))
                .next()
                .switchIfEmpty(Mono.error(new IllegalStateException("No supported Vault authenticator succeeded for auth-method: " + (properties != null ? properties.getAuthMethod() : "null"))));
    }
}
