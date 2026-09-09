package com.phaselume.torana.security.authn.jwt;

import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthnProperties.JwtConfig;
import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthnProperties.JwtIssuerConfig;
import com.phaselume.torana.core.model.ToranaAuthentication;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Extracts normalized identities, roles, scopes, and tenant information from JWT claims.
 */
public class JwtClaimsExtractor {

    private final JwtConfig defaultJwtConfig;

    public JwtClaimsExtractor(JwtConfig defaultJwtConfig) {
        this.defaultJwtConfig = defaultJwtConfig != null ? defaultJwtConfig : new JwtConfig();
    }

    /**
     * Maps raw JWT claims and raw token into a normalized {@link ToranaAuthentication}.
     */
    public ToranaAuthentication extract(Map<String, Object> claims, String rawToken, JwtIssuerConfig issuerConfig) {
        if (claims == null || claims.isEmpty()) {
            return null;
        }

        String principalClaim = resolve(issuerConfig != null ? issuerConfig.getPrincipalClaim() : null, defaultJwtConfig.getPrincipalClaim(), "sub");
        String tenantClaim = resolve(issuerConfig != null ? issuerConfig.getTenantClaim() : null, defaultJwtConfig.getTenantClaim(), "tenant_id");
        String scopeClaim = resolve(issuerConfig != null ? issuerConfig.getScopeClaim() : null, defaultJwtConfig.getScopeClaim(), "scp");
        String rolesClaim = resolve(issuerConfig != null ? issuerConfig.getRolesClaim() : null, defaultJwtConfig.getRolesClaim(), "roles");

        String principalId = getStringValue(claims, principalClaim, "unknown-principal");
        String name = getStringValue(claims, "name", principalId);
        String tenantId = getStringValue(claims, tenantClaim, "default");

        Set<String> scopes = extractSet(claims, scopeClaim);
        if (scopes.isEmpty() && !"scope".equals(scopeClaim)) {
            scopes = extractSet(claims, "scope");
        }

        Set<String> roles = extractSet(claims, rolesClaim);
        if (roles.isEmpty() && !"realm_access".equals(rolesClaim)) {
            // Check Keycloak format realm_access.roles
            Object realmAccess = claims.get("realm_access");
            if (realmAccess instanceof Map<?, ?> map) {
                Object r = map.get("roles");
                if (r instanceof Collection<?> col) {
                    roles = new HashSet<>();
                    for (Object item : col) {
                        if (item != null) roles.add(item.toString());
                    }
                }
            }
        }

        return ToranaAuthentication.builder()
                .principalId(principalId)
                .name(name)
                .tenantId(tenantId)
                .authMethod("jwt")
                .roles(Collections.unmodifiableSet(roles))
                .scopes(Collections.unmodifiableSet(scopes))
                .claims(Collections.unmodifiableMap(claims))
                .rawToken(rawToken)
                .build();
    }

    private static String resolve(String primary, String secondary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (secondary != null && !secondary.isBlank()) {
            return secondary;
        }
        return fallback;
    }

    private static String getStringValue(Map<String, Object> claims, String key, String defaultValue) {
        Object val = claims.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    private static Set<String> extractSet(Map<String, Object> claims, String key) {
        Object val = claims.get(key);
        if (val == null) {
            return Collections.emptySet();
        }
        Set<String> set = new HashSet<>();
        if (val instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item != null) {
                    set.add(item.toString());
                }
            }
        } else if (val instanceof String str) {
            String[] tokens = str.split("[\\s,]+");
            for (String t : tokens) {
                if (!t.isBlank()) {
                    set.add(t.trim());
                }
            }
        }
        return set;
    }
}
