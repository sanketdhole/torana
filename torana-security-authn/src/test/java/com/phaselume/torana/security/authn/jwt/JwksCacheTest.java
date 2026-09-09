package com.phaselume.torana.security.authn.jwt;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwksCacheTest {

    @Test
    void testInMemoryJwksCachePutGetInvalidate() throws Exception {
        JwksCache cache = new JwksCache();

        RSAKey rsaKey = new RSAKeyGenerator(2048)
                .keyID("key-1")
                .generate();
        JWKSet jwkSet = new JWKSet(rsaKey.toPublicJWK());

        String issuerUri = "https://auth.company.com";

        // Should be empty initially
        StepVerifier.create(cache.get(issuerUri))
                .verifyComplete();

        // Put in cache
        StepVerifier.create(cache.put(issuerUri, jwkSet, 300))
                .verifyComplete();

        // Get from cache
        StepVerifier.create(cache.get(issuerUri))
                .assertNext(foundSet -> {
                    assertNotNull(foundSet);
                    assertEquals(1, foundSet.getKeys().size());
                    assertEquals("key-1", foundSet.getKeys().get(0).getKeyID());
                })
                .verifyComplete();

        // Invalidate
        StepVerifier.create(cache.invalidate(issuerUri))
                .verifyComplete();

        // Should be empty again
        StepVerifier.create(cache.get(issuerUri))
                .verifyComplete();
    }
}
