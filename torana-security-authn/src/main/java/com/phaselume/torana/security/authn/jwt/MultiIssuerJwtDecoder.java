package com.phaselume.torana.security.authn.jwt;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthnProperties.JwtConfig;
import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthnProperties.JwtIssuerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates and decodes JWTs across multiple configured identity providers and issuers.
 */
public class MultiIssuerJwtDecoder {

    private static final Logger log = LoggerFactory.getLogger(MultiIssuerJwtDecoder.class);

    private final JwtConfig jwtConfig;
    private final JwksCache jwksCache;
    private final WebClient webClient;
    private final Map<String, JwtIssuerConfig> issuersByUri = new ConcurrentHashMap<>();

    public MultiIssuerJwtDecoder(JwtConfig jwtConfig, JwksCache jwksCache, WebClient.Builder webClientBuilder) {
        this.jwtConfig = jwtConfig != null ? jwtConfig : new JwtConfig();
        this.jwksCache = jwksCache != null ? jwksCache : new JwksCache();
        this.webClient = webClientBuilder != null ? webClientBuilder.build() : WebClient.builder().build();

        if (this.jwtConfig.getIssuers() != null) {
            for (JwtIssuerConfig issuer : this.jwtConfig.getIssuers()) {
                if (issuer.getUri() != null) {
                    issuersByUri.put(issuer.getUri().replaceAll("/+$", ""), issuer);
                }
            }
        }
    }

    public MultiIssuerJwtDecoder(JwtConfig jwtConfig, JwksCache jwksCache) {
        this(jwtConfig, jwksCache, null);
    }

    /**
     * Parses, verifies, and extracts claims from a raw JWT token.
     */
    public Mono<DecodedJwtResult> decode(String token) {
        if (token == null || token.isBlank()) {
            return Mono.error(new IllegalArgumentException("JWT token is empty"));
        }

        SignedJWT signedJwt;
        try {
            signedJwt = SignedJWT.parse(token);
        } catch (ParseException e) {
            return Mono.error(new IllegalArgumentException("Invalid JWT format: " + e.getMessage()));
        }

        JWTClaimsSet claimsSet;
        try {
            claimsSet = signedJwt.getJWTClaimsSet();
        } catch (ParseException e) {
            return Mono.error(new IllegalArgumentException("Invalid JWT claims: " + e.getMessage()));
        }

        String issuerUri = claimsSet.getIssuer();
        if (issuerUri == null && !issuersByUri.isEmpty()) {
            return Mono.error(new IllegalArgumentException("JWT missing issuer claim"));
        }

        String normalizedIssuer = issuerUri != null ? issuerUri.replaceAll("/+$", "") : "";
        JwtIssuerConfig issuerConfig = issuersByUri.get(normalizedIssuer);

        // If no issuer list is configured but a default JWKS is available, create synthetic config
        if (issuerConfig == null && issuersByUri.isEmpty()) {
            issuerConfig = JwtIssuerConfig.builder()
                    .uri(issuerUri)
                    .audience(jwtConfig.getDefaultAudience())
                    .jwksCacheTtlSeconds(jwtConfig.getJwksCacheTtlSeconds())
                    .build();
        } else if (issuerConfig == null) {
            return Mono.error(new IllegalArgumentException("Unrecognized JWT issuer: " + issuerUri));
        }

        final JwtIssuerConfig resolvedConfig = issuerConfig;

        // Check expiration and not-before
        Date now = new Date();
        if (claimsSet.getExpirationTime() != null && claimsSet.getExpirationTime().before(now)) {
            return Mono.error(new IllegalArgumentException("JWT is expired"));
        }
        if (claimsSet.getNotBeforeTime() != null && claimsSet.getNotBeforeTime().after(now)) {
            return Mono.error(new IllegalArgumentException("JWT not yet valid"));
        }

        // Check audience
        String expectedAudience = resolvedConfig.getAudience() != null ? resolvedConfig.getAudience() : jwtConfig.getDefaultAudience();
        if (expectedAudience != null && !expectedAudience.isBlank()) {
            List<String> audList = claimsSet.getAudience();
            if (audList == null || audList.stream().noneMatch(expectedAudience::equals)) {
                return Mono.error(new IllegalArgumentException("JWT audience mismatch. Expected: " + expectedAudience));
            }
        }

        // Verify signature
        return verifySignature(signedJwt, resolvedConfig)
                .thenReturn(new DecodedJwtResult(claimsSet.getClaims(), resolvedConfig));
    }

    private Mono<Void> verifySignature(SignedJWT signedJwt, JwtIssuerConfig issuerConfig) {
        JWSHeader header = signedJwt.getHeader();
        String jwksUri = resolveJwksUri(issuerConfig);

        if (jwksUri == null || jwksUri.isBlank()) {
            return Mono.error(new IllegalStateException("No JWKS URI resolved for issuer: " + issuerConfig.getUri()));
        }

        return getJwks(jwksUri, issuerConfig.getJwksCacheTtlSeconds())
                .flatMap(jwkSet -> {
                    try {
                        JWKMatcher matcher = new JWKMatcher.Builder()
                                .keyID(header.getKeyID())
                                .keyType(header.getAlgorithm().getName().startsWith("RS") ? com.nimbusds.jose.jwk.KeyType.RSA :
                                        header.getAlgorithm().getName().startsWith("ES") ? com.nimbusds.jose.jwk.KeyType.EC : null)
                                .build();
                        List<JWK> matches = new JWKSelector(matcher).select(jwkSet);
                        if (matches.isEmpty() && header.getKeyID() == null && !jwkSet.getKeys().isEmpty()) {
                            matches = jwkSet.getKeys();
                        }

                        if (matches.isEmpty()) {
                            return Mono.error(new IllegalArgumentException("No matching key found in JWKS for kid: " + header.getKeyID()));
                        }

                        JWK jwk = matches.get(0);
                        JWSVerifier verifier = createVerifier(jwk, header);

                        if (!signedJwt.verify(verifier)) {
                            return Mono.error(new IllegalArgumentException("JWT signature validation failed"));
                        }

                        return Mono.empty();
                    } catch (Exception e) {
                        return Mono.error(new IllegalArgumentException("JWT signature check error: " + e.getMessage(), e));
                    }
                });
    }

    private Mono<JWKSet> getJwks(String jwksUri, long ttlSeconds) {
        return jwksCache.get(jwksUri)
                .switchIfEmpty(
                        webClient.get()
                                .uri(jwksUri)
                                .retrieve()
                                .bodyToMono(String.class)
                                .flatMap(body -> {
                                    try {
                                        JWKSet parsed = JWKSet.parse(body);
                                        return jwksCache.put(jwksUri, parsed, ttlSeconds)
                                                .thenReturn(parsed);
                                    } catch (ParseException e) {
                                        return Mono.error(new IllegalArgumentException("Failed to parse remote JWKS: " + e.getMessage()));
                                    }
                                })
                );
    }

    private JWSVerifier createVerifier(JWK jwk, JWSHeader header) throws Exception {
        if (jwk instanceof RSAKey rsaKey) {
            return new RSASSAVerifier(rsaKey.toRSAPublicKey());
        } else if (jwk instanceof ECKey ecKey) {
            return new ECDSAVerifier(ecKey.toECPublicKey());
        }
        throw new IllegalArgumentException("Unsupported JWK key type: " + jwk.getKeyType());
    }

    private String resolveJwksUri(JwtIssuerConfig issuerConfig) {
        if (issuerConfig.getJwksUri() != null && !issuerConfig.getJwksUri().isBlank()) {
            return issuerConfig.getJwksUri();
        }
        if (issuerConfig.getUri() != null) {
            String base = issuerConfig.getUri().replaceAll("/+$", "");
            return base + "/protocol/openid-connect/certs"; // Standard OIDC / Keycloak
        }
        return null;
    }

    public static class DecodedJwtResult {
        private final Map<String, Object> claims;
        private final JwtIssuerConfig issuerConfig;

        public DecodedJwtResult(Map<String, Object> claims, JwtIssuerConfig issuerConfig) {
            this.claims = claims;
            this.issuerConfig = issuerConfig;
        }

        public Map<String, Object> getClaims() {
            return claims;
        }

        public JwtIssuerConfig getIssuerConfig() {
            return issuerConfig;
        }
    }
}
