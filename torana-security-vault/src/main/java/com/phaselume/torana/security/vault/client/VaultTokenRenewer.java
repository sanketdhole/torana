package com.phaselume.torana.security.vault.client;

import com.phaselume.torana.security.vault.config.VaultProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Periodically renews the gateway's own client token in Vault.
 */
public class VaultTokenRenewer {

    private static final Logger log = LoggerFactory.getLogger(VaultTokenRenewer.class);

    private final VaultReactiveClient vaultClient;
    private final WebClient webClient;
    private final Duration renewInterval;
    private Disposable subscription;

    public VaultTokenRenewer(VaultReactiveClient vaultClient, VaultProperties properties) {
        this.vaultClient = vaultClient;
        this.webClient = WebClient.builder().baseUrl(properties != null ? properties.getUri() : "http://127.0.0.1:8200").build();
        this.renewInterval = (properties != null && properties.getTokenRenewThreshold() != null)
                ? properties.getTokenRenewThreshold()
                : Duration.ofSeconds(30);
    }

    public void start() {
        if (subscription != null && !subscription.isDisposed()) {
            return;
        }

        subscription = Flux.interval(renewInterval)
                .flatMap(tick -> renewSelf().onErrorResume(e -> {
                    log.warn("Vault token renewal attempt failed: {}", e.getMessage());
                    return Mono.empty();
                }))
                .subscribe();
    }

    public void stop() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }

    public Mono<Void> renewSelf() {
        String token = vaultClient.getCurrentToken();
        if (token == null || token.isBlank()) {
            return Mono.empty();
        }

        return webClient.post()
                .uri("/v1/auth/token/renew-self")
                .header("X-Vault-Token", token)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.debug("Vault token renewed successfully"))
                .then();
    }
}
