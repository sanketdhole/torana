package com.phaselume.torana.test.fixtures;

import com.phaselume.torana.core.model.ToranaAuthentication;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Pre-configured ToranaAuthentication instances for testing.
 */
public final class ToranaAuthenticationFixtures {

    private ToranaAuthenticationFixtures() {}

    public static ToranaAuthentication aliceUser() {
        return ToranaAuthentication.builder()
                .principalId("usr_alice")
                .name("Alice Smith")
                .tenantId("acme-corp")
                .authMethod("jwt")
                .roles(Set.of("user", "developer"))
                .scopes(Set.of("read", "execute"))
                .claims(Map.of("email", "alice@acme.com", "dept", "engineering"))
                .build();
    }

    public static ToranaAuthentication bobAdmin() {
        return ToranaAuthentication.builder()
                .principalId("usr_bob")
                .name("Bob Admin")
                .tenantId("acme-corp")
                .authMethod("jwt")
                .roles(Set.of("admin", "user"))
                .scopes(Set.of("read", "write", "admin", "execute"))
                .claims(Map.of("email", "bob@acme.com", "dept", "security"))
                .build();
    }

    public static ToranaAuthentication tenantUser(String username, String tenantId, Set<String> roles) {
        return ToranaAuthentication.builder()
                .principalId("usr_" + username)
                .name(username)
                .tenantId(tenantId)
                .authMethod("jwt")
                .roles(roles != null ? roles : Collections.emptySet())
                .scopes(Set.of("read", "execute"))
                .build();
    }

    public static ToranaAuthentication anonymous() {
        return ToranaAuthentication.anonymous();
    }
}
