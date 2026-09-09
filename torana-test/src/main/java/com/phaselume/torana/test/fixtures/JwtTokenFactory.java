package com.phaselume.torana.test.fixtures;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Generates cryptographically signed RSA JWT tokens for test cases without needing an external IdP.
 * Also exposes an in-memory JWKS JSON payload for mock OIDC discovery endpoints.
 */
public class JwtTokenFactory {

    private final RSAKey rsaJwk;
    private final String keyId;
    private final String defaultIssuer;

    public JwtTokenFactory() {
        this("https://auth.torana.test", "test-key-1");
    }

    public JwtTokenFactory(String defaultIssuer, String keyId) {
        this.defaultIssuer = defaultIssuer;
        this.keyId = keyId;
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair keyPair = gen.generateKeyPair();

            this.rsaJwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(keyId)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JwtTokenFactory RSA keys", e);
        }
    }

    /**
     * Create a valid signed JWT for a given subject, tenant, and roles.
     */
    public String createToken(String subject, String tenantId, List<String> roles, Map<String, Object> customClaims, Duration ttl) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issuer(defaultIssuer)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plus(ttl)))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("tenant_id", tenantId)
                    .claim("roles", roles);

            if (customClaims != null) {
                customClaims.forEach(builder::claim);
            }

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyId).build(),
                    builder.build()
            );

            signedJWT.sign(new RSASSASigner(rsaJwk.toRSAPrivateKey()));
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create signed JWT", e);
        }
    }

    public String createValidUserToken(String username, String tenantId) {
        return createToken(username, tenantId, List.of("user"), Collections.emptyMap(), Duration.ofHours(1));
    }

    public String createValidAdminToken(String username, String tenantId) {
        return createToken(username, tenantId, List.of("admin", "user"), Collections.emptyMap(), Duration.ofHours(1));
    }

    public String createExpiredToken(String username, String tenantId) {
        return createToken(username, tenantId, List.of("user"), Collections.emptyMap(), Duration.ofHours(-1));
    }

    /**
     * Exposes public JWKS in JSON format to mock `/.well-known/jwks.json`.
     */
    public String getJwksJson() {
        return new JWKSet(rsaJwk.toPublicJWK()).toString();
    }

    public RSAPublicKey getPublicKey() {
        try {
            return rsaJwk.toRSAPublicKey();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getDefaultIssuer() {
        return defaultIssuer;
    }
}
