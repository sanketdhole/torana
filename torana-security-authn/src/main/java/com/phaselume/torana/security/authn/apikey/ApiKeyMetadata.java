package com.phaselume.torana.security.authn.apikey;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Metadata associated with a hashed API key.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiKeyMetadata {

    private String name;
    private String principalId;
    @Builder.Default
    private String tenantId = "default";
    @Builder.Default
    private boolean active = true;
    private Long expiresAtMillis;
    @Builder.Default
    private Set<String> roles = Collections.emptySet();
    @Builder.Default
    private Set<String> scopes = Collections.emptySet();
    @Builder.Default
    private Map<String, Object> attributes = Collections.emptyMap();

    public boolean isExpired() {
        return expiresAtMillis != null && System.currentTimeMillis() > expiresAtMillis;
    }
}
