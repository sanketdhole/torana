package com.phaselume.torana.security.vault.client;

import com.phaselume.torana.security.vault.auth.VaultAuthenticatorChain;
import com.phaselume.torana.security.vault.config.VaultProperties;
import com.phaselume.torana.security.vault.model.VaultSecretResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Reactive HTTP client for interacting with HashiCorp Vault.
 */
public class VaultReactiveClient {

    private static final Logger log = LoggerFactory.getLogger(VaultReactiveClient.class);

    private final WebClient webClient;
    private final VaultProperties properties;
    private final VaultAuthenticatorChain authenticatorChain;
    private final AtomicReference<String> tokenHolder = new AtomicReference<>();

    public VaultReactiveClient(WebClient.Builder webClientBuilder,
                               VaultProperties properties,
                               VaultAuthenticatorChain authenticatorChain) {
        this.properties = properties != null ? properties : new VaultProperties();
        this.webClient = webClientBuilder.baseUrl(this.properties.getUri()).build();
        this.authenticatorChain = authenticatorChain;
    }

    public Mono<String> getOrRenewToken() {
        String existing = tokenHolder.get();
        if (existing != null && !existing.isBlank()) {
            return Mono.just(existing);
        }

        if (authenticatorChain == null) {
            return Mono.error(new IllegalStateException("No VaultAuthenticatorChain configured"));
        }

        return authenticatorChain.authenticate(webClient, properties)
                .doOnNext(tokenHolder::set);
    }

    public Mono<VaultSecretResponse> readSecret(String vaultPath) {
        String path = vaultPath.startsWith("/") ? vaultPath : "/" + vaultPath;
        if (!path.startsWith("/v1/")) {
            path = "/v1/" + (path.startsWith("/") ? path.substring(1) : path);
        }

        final String requestPath = path;
        return getOrRenewToken()
                .flatMap(token -> webClient.get()
                        .uri(requestPath)
                        .header("X-Vault-Token", token)
                        .retrieve()
                        .bodyToMono(VaultSecretResponse.class)
                )
                .doOnError(e -> log.error("Failed to read secret from Vault at {}: {}", requestPath, e.getMessage()));
    }

    public void setToken(String token) {
        this.tokenHolder.set(token);
    }

    public String getCurrentToken() {
        return tokenHolder.get();
    }
}
