package com.phaselume.torana.security.authn.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthnProperties.JwtConfig;
import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthnProperties.JwtIssuerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MultiIssuerJwtDecoderTest {

    private RSAKey rsaJWK;
    private JwksCache jwksCache;
    private MultiIssuerJwtDecoder decoder;
    private final String issuerUri = "https://idp.torana.internal";
    private final String audience = "torana-gateway";

    @BeforeEach
    void setUp() throws Exception {
        rsaJWK = new RSAKeyGenerator(2048)
                .keyID("rsa-test-key-1")
                .generate();

        jwksCache = new JwksCache();
        JWKSet jwkSet = new JWKSet(rsaJWK.toPublicJWK());
        jwksCache.put(issuerUri + "/protocol/openid-connect/certs", jwkSet, 3600).block();

        JwtIssuerConfig issuerConfig = JwtIssuerConfig.builder()
                .uri(issuerUri)
                .audience(audience)
                .build();

        JwtConfig jwtConfig = JwtConfig.builder()
                .defaultAudience(audience)
                .issuers(List.of(issuerConfig))
                .build();

        decoder = new MultiIssuerJwtDecoder(jwtConfig, jwksCache);
    }

    private String createToken(String issuer, String aud, Date exp, Date nbf) throws Exception {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("test-subject")
                .issuer(issuer)
                .audience(aud)
                .expirationTime(exp)
                .notBeforeTime(nbf)
                .issueTime(new Date())
                .claim("tenant_id", "tenant-test")
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID()).build(),
                claimsSet
        );
        signedJWT.sign(new RSASSASigner(rsaJWK));
        return signedJWT.serialize();
    }

    @Test
    void testValidJwtDecodesSuccessfully() throws Exception {
        Date exp = new Date(System.currentTimeMillis() + 60_000);
        Date nbf = new Date(System.currentTimeMillis() - 60_000);
        String token = createToken(issuerUri, audience, exp, nbf);

        StepVerifier.create(decoder.decode(token))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals("test-subject", result.getClaims().get("sub"));
                    assertEquals("tenant-test", result.getClaims().get("tenant_id"));
                })
                .verifyComplete();
    }

    @Test
    void testExpiredJwtFails() throws Exception {
        Date exp = new Date(System.currentTimeMillis() - 10_000);
        String token = createToken(issuerUri, audience, exp, null);

        StepVerifier.create(decoder.decode(token))
                .expectErrorMatches(err -> err instanceof IllegalArgumentException && err.getMessage().contains("expired"))
                .verify();
    }

    @Test
    void testAudienceMismatchFails() throws Exception {
        Date exp = new Date(System.currentTimeMillis() + 60_000);
        String token = createToken(issuerUri, "wrong-audience", exp, null);

        StepVerifier.create(decoder.decode(token))
                .expectErrorMatches(err -> err instanceof IllegalArgumentException && err.getMessage().contains("audience mismatch"))
                .verify();
    }

    @Test
    void testUnknownIssuerFails() throws Exception {
        Date exp = new Date(System.currentTimeMillis() + 60_000);
        String token = createToken("https://untrusted-issuer.com", audience, exp, null);

        StepVerifier.create(decoder.decode(token))
                .expectErrorMatches(err -> err instanceof IllegalArgumentException && err.getMessage().contains("Unrecognized JWT issuer"))
                .verify();
    }
}
