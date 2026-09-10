package com.phaselume.torana.security.vault.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Maps a backend service reference to a Vault path and secret format.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaultCredentialMapping {

    /**
     * Backend identifier referenced in routes (e.g. "postgres-prod", "s3-docs", "hr-api").
     */
    private String backend;

    /**
     * Relative path in Vault (e.g. "database/creds/torana-readonly", "aws/creds/torana-s3-role", "secret/data/torana/hr-api-token").
     */
    private String vaultPath;

    /**
     * Credential type: "database", "aws-sts", "pki-cert", "kv", "bearer".
     */
    @Builder.Default
    private String credentialType = "kv";

    /**
     * For KV secrets: field key containing the token/password (default "token" or "password").
     */
    private String kvField;

    /**
     * For database/custom secrets: username field key (default "username").
     */
    @Builder.Default
    private String usernameField = "username";

    /**
     * For database/custom secrets: password field key (default "password").
     */
    @Builder.Default
    private String passwordField = "password";

    /**
     * For PKI certificates: common name to request.
     */
    private String commonName;
}
