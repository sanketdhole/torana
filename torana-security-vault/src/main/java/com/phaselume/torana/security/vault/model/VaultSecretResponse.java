package com.phaselume.torana.security.vault.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

/**
 * Deserialized response model from HashiCorp Vault HTTP API.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VaultSecretResponse {

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("lease_id")
    private String leaseId;

    @JsonProperty("renewable")
    private boolean renewable;

    @JsonProperty("lease_duration")
    private long leaseDuration;

    @JsonProperty("data")
    private Map<String, Object> data = Collections.emptyMap();

    @JsonProperty("auth")
    private VaultAuth auth;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VaultAuth {
        @JsonProperty("client_token")
        private String clientToken;

        @JsonProperty("accessor")
        private String accessor;

        @JsonProperty("lease_duration")
        private long leaseDuration;

        @JsonProperty("renewable")
        private boolean renewable;
    }
}
