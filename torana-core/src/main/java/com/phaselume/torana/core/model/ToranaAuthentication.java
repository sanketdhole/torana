package com.phaselume.torana.core.model;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Represents the authenticated principal identity and security context in Torana.
 */
@Value
@Builder(toBuilder = true)
public class ToranaAuthentication {

    String principalId;
    String name;
    String tenantId;
    String authMethod; // "jwt", "apikey", "mtls", "saml", "anonymous"
    
    @Builder.Default
    Set<String> roles = Collections.emptySet();
    
    @Builder.Default
    Set<String> scopes = Collections.emptySet();
    
    @Builder.Default
    Map<String, Object> claims = Collections.emptyMap();
    
    String rawToken; // Kept opaque, never logged
    
    @Builder.Default
    Map<String, Object> attributes = Collections.emptyMap();

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasScope(String scope) {
        return scopes != null && scopes.contains(scope);
    }

    @SuppressWarnings("unchecked")
    public <T> T getClaim(String claimName) {
        return claims != null ? (T) claims.get(claimName) : null;
    }

    public static ToranaAuthentication anonymous() {
        return ToranaAuthentication.builder()
                .principalId("anonymous")
                .name("Anonymous User")
                .tenantId("default")
                .authMethod("anonymous")
                .roles(Collections.emptySet())
                .scopes(Collections.emptySet())
                .claims(Collections.emptyMap())
                .build();
    }
}
