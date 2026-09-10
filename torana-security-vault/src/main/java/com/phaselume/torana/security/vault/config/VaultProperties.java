package com.phaselume.torana.security.vault.config;

import com.phaselume.torana.security.vault.model.VaultCredentialMapping;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for HashiCorp Vault credential broker.
 */
@Data
@ConfigurationProperties(prefix = "torana.security.credential-broker.vault")
public class VaultProperties {

    private boolean enabled = true;
    private String uri = "http://127.0.0.1:8200";
    private String authMethod = "token"; // "token", "kubernetes", "approle"
    private String token;
    private String role;
    private Duration tokenRenewThreshold = Duration.ofSeconds(30);

    private KubernetesAuthProperties kubernetes = new KubernetesAuthProperties();
    private AppRoleAuthProperties approle = new AppRoleAuthProperties();
    private List<VaultCredentialMapping> mappings = new ArrayList<>();

    @Data
    public static class KubernetesAuthProperties {
        private String serviceAccountTokenPath = "/var/run/secrets/kubernetes.io/serviceaccount/token";
        private String kubernetesHost = "https://kubernetes.default.svc";
        private String authPath = "kubernetes";
    }

    @Data
    public static class AppRoleAuthProperties {
        private String roleId;
        private String secretId;
        private String authPath = "approle";
    }
}
