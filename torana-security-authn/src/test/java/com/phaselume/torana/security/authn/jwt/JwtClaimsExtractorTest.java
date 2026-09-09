package com.phaselume.torana.security.authn.jwt;

import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthnProperties.JwtConfig;
import com.phaselume.torana.core.model.ToranaAuthentication;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtClaimsExtractorTest {

    @Test
    void testExtractStandardClaims() {
        JwtConfig config = JwtConfig.builder()
                .principalClaim("sub")
                .tenantClaim("tenant_id")
                .scopeClaim("scp")
                .rolesClaim("roles")
                .build();

        JwtClaimsExtractor extractor = new JwtClaimsExtractor(config);

        Map<String, Object> claims = Map.of(
                "sub", "user-123",
                "name", "Alice Engineer",
                "tenant_id", "tenant-alpha",
                "scp", List.of("read:tools", "write:tools"),
                "roles", List.of("admin", "developer")
        );

        ToranaAuthentication auth = extractor.extract(claims, "raw-token-xyz", null);

        assertNotNull(auth);
        assertEquals("user-123", auth.getPrincipalId());
        assertEquals("Alice Engineer", auth.getName());
        assertEquals("tenant-alpha", auth.getTenantId());
        assertEquals("jwt", auth.getAuthMethod());
        assertEquals(Set.of("read:tools", "write:tools"), auth.getScopes());
        assertEquals(Set.of("admin", "developer"), auth.getRoles());
        assertEquals("raw-token-xyz", auth.getRawToken());
        assertTrue(auth.hasRole("admin"));
        assertTrue(auth.hasScope("read:tools"));
    }

    @Test
    void testExtractKeycloakClaims() {
        JwtConfig config = JwtConfig.builder().build();
        JwtClaimsExtractor extractor = new JwtClaimsExtractor(config);

        Map<String, Object> claims = Map.of(
                "sub", "kc-user-456",
                "scope", "openid profile email",
                "realm_access", Map.of("roles", List.of("agent-operator", "auditor"))
        );

        ToranaAuthentication auth = extractor.extract(claims, "kc-token", null);

        assertNotNull(auth);
        assertEquals("kc-user-456", auth.getPrincipalId());
        assertTrue(auth.getScopes().contains("profile"));
        assertTrue(auth.getRoles().contains("agent-operator"));
    }
}
