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
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtOidcAuthenticationProviderTest {

    private RSAKey rsaJWK;
    private JwtOidcAuthenticationProvider provider;
    private final String issuerUri = "https://auth.example.com";
    private final String audience = "torana-gateway";

    @BeforeEach
    void setUp() throws Exception {
        rsaJWK = new RSAKeyGenerator(2048).keyID("k1").generate();
        JwksCache jwksCache = new JwksCache();
        jwksCache.put(issuerUri + "/protocol/openid-connect/certs", new JWKSet(rsaJWK.toPublicJWK()), 3600).block();

        JwtIssuerConfig issuerConfig = JwtIssuerConfig.builder()
                .uri(issuerUri)
                .audience(audience)
                .build();

        JwtConfig jwtConfig = JwtConfig.builder()
                .defaultAudience(audience)
                .issuers(List.of(issuerConfig))
                .build();

        MultiIssuerJwtDecoder decoder = new MultiIssuerJwtDecoder(jwtConfig, jwksCache);
        JwtClaimsExtractor extractor = new JwtClaimsExtractor(jwtConfig);
        provider = new JwtOidcAuthenticationProvider(decoder, extractor);
    }

    private String createValidToken() throws Exception {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("agent-alice")
                .issuer(issuerUri)
                .audience(audience)
                .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                .claim("tenant_id", "tenant-1")
                .claim("scp", List.of("mcp:tools:call"))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("k1").build(),
                claimsSet
        );
        signedJWT.sign(new RSASSASigner(rsaJWK));
        return signedJWT.serialize();
    }

    @Test
    void testAuthenticateWithValidBearerHeader() throws Exception {
        String token = createValidToken();
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/tools")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(provider.authenticate(exchange))
                .assertNext(auth -> {
                    assertNotNull(auth);
                    assertEquals("agent-alice", auth.getPrincipalId());
                    assertEquals("tenant-1", auth.getTenantId());
                    assertEquals("jwt", auth.getAuthMethod());
                    assertEquals(token, auth.getRawToken());
                    assertEquals(true, auth.hasScope("mcp:tools:call"));
                })
                .verifyComplete();
    }

    @Test
    void testAuthenticateWithoutHeaderReturnsEmpty() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/tools").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(provider.authenticate(exchange))
                .verifyComplete();
    }
}
