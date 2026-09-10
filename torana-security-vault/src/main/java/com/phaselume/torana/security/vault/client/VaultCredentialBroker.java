package com.phaselume.torana.security.vault.client;

import com.phaselume.torana.core.model.AgentContext;
import com.phaselume.torana.core.model.BackendCredentials;
import com.phaselume.torana.core.spi.CredentialBroker;
import com.phaselume.torana.security.vault.config.VaultProperties;
import com.phaselume.torana.security.vault.model.VaultCredentialMapping;
import com.phaselume.torana.security.vault.model.VaultSecretResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vault-backed dynamic CredentialBroker implementation.
 */
public class VaultCredentialBroker implements CredentialBroker {

    private static final Logger log = LoggerFactory.getLogger(VaultCredentialBroker.class);

    private final VaultReactiveClient vaultClient;
    private final Map<String, VaultCredentialMapping> mappings = new ConcurrentHashMap<>();

    public VaultCredentialBroker(VaultReactiveClient vaultClient, VaultProperties properties) {
        this.vaultClient = vaultClient;
        if (properties != null && properties.getMappings() != null) {
            for (VaultCredentialMapping mapping : properties.getMappings()) {
                if (mapping.getBackend() != null) {
                    mappings.put(mapping.getBackend(), mapping);
                }
            }
        }
    }

    public void addMapping(VaultCredentialMapping mapping) {
        if (mapping != null && mapping.getBackend() != null) {
            mappings.put(mapping.getBackend(), mapping);
        }
    }

    @Override
    public Mono<BackendCredentials> broker(AgentContext context, String backendRef) {
        if (backendRef == null || !mappings.containsKey(backendRef)) {
            log.debug("No Vault mapping found for backend: {}", backendRef);
            return Mono.empty();
        }

        VaultCredentialMapping mapping = mappings.get(backendRef);
        return vaultClient.readSecret(mapping.getVaultPath())
                .flatMap(response -> mapResponseToCredentials(response, mapping));
    }

    @SuppressWarnings("unchecked")
    private Mono<BackendCredentials> mapResponseToCredentials(VaultSecretResponse response, VaultCredentialMapping mapping) {
        if (response == null || response.getData() == null) {
            return Mono.empty();
        }

        Map<String, Object> data = response.getData();
        // Handle KV v2 nested data wrapper if present
        if (data.containsKey("data") && data.get("data") instanceof Map) {
            data = (Map<String, Object>) data.get("data");
        }

        Instant expiresAt = null;
        if (response.getLeaseDuration() > 0) {
            expiresAt = Instant.now().plusSeconds(response.getLeaseDuration());
        }

        String type = mapping.getCredentialType() != null ? mapping.getCredentialType().toLowerCase() : "kv";

        switch (type) {
            case "database":
                String userField = mapping.getUsernameField() != null ? mapping.getUsernameField() : "username";
                String passField = mapping.getPasswordField() != null ? mapping.getPasswordField() : "password";
                String username = data.get(userField) != null ? data.get(userField).toString() : null;
                String password = data.get(passField) != null ? data.get(passField).toString() : null;
                return Mono.just(BackendCredentials.basic(username, password, expiresAt));

            case "aws-sts":
            case "aws":
                String accessKey = data.get("access_key") != null ? data.get("access_key").toString() : null;
                String secretKey = data.get("secret_key") != null ? data.get("secret_key").toString() : null;
                String securityToken = data.get("security_token") != null ? data.get("security_token").toString() : null;
                return Mono.just(BackendCredentials.awsSts(accessKey, secretKey, securityToken, expiresAt));

            case "bearer":
            case "kv":
            default:
                String kvField = mapping.getKvField();
                String token = null;
                if (kvField != null && data.containsKey(kvField)) {
                    token = data.get(kvField).toString();
                } else if (data.containsKey("token")) {
                    token = data.get("token").toString();
                } else if (data.containsKey("password")) {
                    token = data.get("password").toString();
                } else if (data.containsKey("value")) {
                    token = data.get("value").toString();
                } else if (!data.isEmpty()) {
                    token = data.values().iterator().next().toString();
                }
                return Mono.just(BackendCredentials.bearer(token, expiresAt));
        }
    }
}
