package com.phaselume.torana.core.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Short-lived backend credentials brokered from Vault, AWS STS, or static secret vaults.
 */
@Value
@Builder(toBuilder = true)
public class BackendCredentials {

    String type; // "BEARER", "BASIC", "AWS_STS", "DATABASE_DYNAMIC", "API_KEY"
    String token;
    String username;
    String password;
    String accessKeyId;
    String secretAccessKey;
    String sessionToken;
    Instant expiresAt;

    @Builder.Default
    Map<String, String> additionalHeaders = Collections.emptyMap();

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public static BackendCredentials bearer(String token, Instant expiresAt) {
        return BackendCredentials.builder()
                .type("BEARER")
                .token(token)
                .expiresAt(expiresAt)
                .build();
    }

    public static BackendCredentials basic(String username, String password, Instant expiresAt) {
        return BackendCredentials.builder()
                .type("BASIC")
                .username(username)
                .password(password)
                .expiresAt(expiresAt)
                .build();
    }

    public static BackendCredentials awsSts(String accessKeyId, String secretAccessKey, String sessionToken, Instant expiresAt) {
        return BackendCredentials.builder()
                .type("AWS_STS")
                .accessKeyId(accessKeyId)
                .secretAccessKey(secretAccessKey)
                .sessionToken(sessionToken)
                .expiresAt(expiresAt)
                .build();
    }
}
