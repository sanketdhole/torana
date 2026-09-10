package com.phaselume.torana.security.vault.auth;

import com.phaselume.torana.security.vault.config.VaultProperties;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultAuthenticatorTest {

    @Test
    void testTokenVaultAuthenticator() {
        TokenVaultAuthenticator authenticator = new TokenVaultAuthenticator();

        VaultProperties props = new VaultProperties();
        props.setAuthMethod("token");
        props.setToken("s.my-static-token");

        assertTrue(authenticator.supports(props));

        StepVerifier.create(authenticator.authenticate(null, props))
                .assertNext(token -> assertEquals("s.my-static-token", token))
                .verifyComplete();
    }

    @Test
    void testVaultAuthenticatorChain() {
        TokenVaultAuthenticator tokenAuth = new TokenVaultAuthenticator();
        VaultAuthenticatorChain chain = new VaultAuthenticatorChain(List.of(tokenAuth));

        VaultProperties props = new VaultProperties();
        props.setAuthMethod("token");
        props.setToken("s.chain-token");

        StepVerifier.create(chain.authenticate(null, props))
                .assertNext(token -> assertEquals("s.chain-token", token))
                .verifyComplete();

        VaultProperties unsupported = new VaultProperties();
        unsupported.setAuthMethod("unsupported");

        StepVerifier.create(chain.authenticate(null, unsupported))
                .expectError(IllegalStateException.class)
                .verify();
    }
}
