package com.phaselume.torana.security.authn.outbound;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthnProperties.OutboundAuthnConfig.JwtSignerConfig;
import com.phaselume.torana.core.model.ToranaAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Mints short-lived, signed internal JWTs (Service Token Service) to authenticate Torana
 * and propagate caller identities to internal cloud microservices.
 */
public class InternalJwtMinter {

    private static final Logger log = LoggerFactory.getLogger(InternalJwtMinter.class);

    private final JwtSignerConfig config;
    private final JWSSigner signer;
    private final JWSAlgorithm algorithm;

    public InternalJwtMinter(JwtSignerConfig config) {
        this.config = config != null ? config : new JwtSignerConfig();
        try {
            if ("RS256".equalsIgnoreCase(this.config.getAlgorithm()) && this.config.getPrivateKeyPem() != null) {
                RSAPrivateKey rsaKey = parsePrivateKey(this.config.getPrivateKeyPem());
                this.signer = new RSASSASigner(rsaKey);
                this.algorithm = JWSAlgorithm.RS256;
            } else {
                byte[] secretBytes = this.config.getSecret().getBytes(StandardCharsets.UTF_8);
                // HMAC key must be at least 256 bits (32 bytes)
                if (secretBytes.length < 32) {
                    byte[] padded = new byte[32];
                    System.arraycopy(secretBytes, 0, padded, 0, secretBytes.length);
                    secretBytes = padded;
                }
                this.signer = new MACSigner(secretBytes);
                this.algorithm = JWSAlgorithm.HS256;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize InternalJwtMinter signer: " + e.getMessage(), e);
        }
    }

    public InternalJwtMinter() {
        this(new JwtSignerConfig());
    }

    /**
     * Mints a signed internal JWT for the given caller authentication and target service audience.
     */
    public String mintToken(ToranaAuthentication auth, String targetAudience) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date exp = new Date(nowMillis + (config.getTokenTtlSeconds() * 1000));

        String principalId = auth != null && auth.getPrincipalId() != null ? auth.getPrincipalId() : "torana-gateway";
        String tenantId = auth != null && auth.getTenantId() != null ? auth.getTenantId() : "default";

        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .issuer(config.getIssuer())
                .subject(principalId)
                .audience(targetAudience != null && !targetAudience.isBlank() ? targetAudience : "internal-service")
                .issueTime(now)
                .notBeforeTime(now)
                .expirationTime(exp)
                .jwtID(UUID.randomUUID().toString())
                .claim("tenant_id", tenantId);

        if (auth != null) {
            if (auth.getName() != null) {
                claimsBuilder.claim("name", auth.getName());
            }
            if (auth.getRoles() != null && !auth.getRoles().isEmpty()) {
                claimsBuilder.claim("roles", new ArrayList<>(auth.getRoles()));
            }
            if (auth.getScopes() != null && !auth.getScopes().isEmpty()) {
                claimsBuilder.claim("scp", new ArrayList<>(auth.getScopes()));
            }
            if (auth.getAuthMethod() != null) {
                claimsBuilder.claim("auth_method", auth.getAuthMethod());
            }
        }

        JWSHeader header = new JWSHeader.Builder(algorithm)
                .keyID(config.getKeyId())
                .build();

        SignedJWT signedJwt = new SignedJWT(header, claimsBuilder.build());

        try {
            signedJwt.sign(signer);
            return signedJwt.serialize();
        } catch (Exception e) {
            log.error("Failed to sign internal JWT: {}", e.getMessage(), e);
            throw new IllegalStateException("Internal JWT signing error", e);
        }
    }

    private static RSAPrivateKey parsePrivateKey(String pem) throws Exception {
        String cleanPem = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(cleanPem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(spec);
    }
}
