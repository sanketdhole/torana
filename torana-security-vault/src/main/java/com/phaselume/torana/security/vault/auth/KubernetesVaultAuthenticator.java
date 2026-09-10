package com.phaselume.torana.security.vault.auth;

import com.phaselume.torana.security.vault.config.VaultProperties;
import com.phaselume.torana.security.vault.model.VaultSecretResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Authenticates to Vault using Kubernetes service account JWT.
 */
public class KubernetesVaultAuthenticator implements VaultAuthenticator {

    private static final Logger log = LoggerFactory.getLogger(KubernetesVaultAuthenticator.class);

    @Override
    public boolean supports(VaultProperties properties) {
        return properties != null && "kubernetes".equalsIgnoreCase(properties.getAuthMethod());
    }

    @Override
    public Mono<String> authenticate(WebClient webClient, VaultProperties properties) {
        return Mono.fromCallable(() -> {
            String jwtPath = properties.getKubernetes().getServiceAccountTokenPath();
            return Files.readString(Path.of(jwtPath)).trim();
        }).flatMap(jwt -> {
            String authPath = properties.getKubernetes().getAuthPath();
            String role = properties.getRole();

            Map<String, Object> body = Map.of(
                    "role", role != null ? role : "default",
                    "jwt", jwt
            );

            return webClient.post()
                    .uri("/v1/auth/" + authPath + "/login")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(VaultSecretResponse.class)
                    .flatMap(resp -> {
                        if (resp.getAuth() != null && resp.getAuth().getClientToken() != null) {
                            return Mono.just(resp.getAuth().getClientToken());
                        }
                        return Mono.error(new IllegalStateException("Vault Kubernetes auth did not return a client token"));
                    });
        }).doOnError(e -> log.error("Failed to authenticate to Vault via Kubernetes: {}", e.getMessage()));
    }
}
