package com.phaselume.torana.test.fixtures;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenFactoryTest {

    @Test
    void testValidTokenGenerationAndVerification() throws Exception {
        JwtTokenFactory factory = new JwtTokenFactory();

        String token = factory.createValidUserToken("alice", "acme-corp");
        assertNotNull(token);

        SignedJWT parsed = SignedJWT.parse(token);
        JWSVerifier verifier = new RSASSAVerifier(factory.getPublicKey());

        assertTrue(parsed.verify(verifier));
        assertEquals("alice", parsed.getJWTClaimsSet().getSubject());
        assertEquals("acme-corp", parsed.getJWTClaimsSet().getStringClaim("tenant_id"));
        assertEquals(List.of("user"), parsed.getJWTClaimsSet().getStringListClaim("roles"));
        assertTrue(parsed.getJWTClaimsSet().getExpirationTime().after(new Date()));
    }

    @Test
    void testExpiredTokenGeneration() throws Exception {
        JwtTokenFactory factory = new JwtTokenFactory();

        String token = factory.createExpiredToken("bob", "acme-corp");
        SignedJWT parsed = SignedJWT.parse(token);

        assertTrue(parsed.getJWTClaimsSet().getExpirationTime().before(new Date()));
    }

    @Test
    void testJwksJsonExport() {
        JwtTokenFactory factory = new JwtTokenFactory();
        String jwks = factory.getJwksJson();

        assertNotNull(jwks);
        assertTrue(jwks.contains("keys"));
        assertTrue(jwks.contains("RSA"));
    }
}
