package com.phaselume.torana.security.vault.auth;

import com.phaselume.torana.security.vault.config.VaultProperties;
import com.phaselume.torana.security.vault.model.VaultSecretResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Authenticates to Vault using AppRole role_id and secret_id.
 */
public class AppRoleVaultAuthenticator implements VaultAuthenticator {

    private static final Logger log = LoggerFactory.getLogger(AppRoleVaultAuthenticator.class);

    @Override
    public boolean supports(VaultProperties properties) {
        return properties != null && "approle".equalsIgnoreCase(properties.getAuthMethod());
    }

    @Override
    public Mono<String> authenticate(WebClient webClient, VaultProperties properties) {
        String authPath = properties.getApprole().getAuthPath();
        String roleId = properties.getApprole().getRoleId();
        String secretId = properties.getApprole().getSecretId();

        if (roleId == null || roleId.isBlank()) {
            return Mono.error(new IllegalStateException("AppRole role_id is required for Vault AppRole authentication"));
        }

        Map<String, String> body = new HashMap<>();
        body.put("role_id", roleId);
        if (secretId != null && !secretId.isBlank()) {
            body.put("secret_id", secretId);
        }

        return webClient.post()
                .uri("/v1/auth/" + authPath + "/login")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(VaultSecretResponse.class)
                .flatMap(resp -> {
                    if (resp.getAuth() != null && resp.getAuth().getClientToken() != null) {
                        return Mono.just(resp.getAuth().getClientToken());
                    }
                    return Mono.error(new IllegalStateException("Vault AppRole auth did not return a client token"));
                })
                .doOnError(e -> log.error("Failed to authenticate to Vault via AppRole: {}", e.getMessage()));
    }
}
