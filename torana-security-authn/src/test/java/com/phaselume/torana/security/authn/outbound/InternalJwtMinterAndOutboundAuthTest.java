package com.phaselume.torana.security.authn.outbound;

import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthnProperties.OutboundAuthnConfig;
import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthnProperties.OutboundAuthnConfig.JwtSignerConfig;
import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthnProperties.OutboundAuthnConfig.ServiceOutboundConfig;
import com.phaselume.torana.core.model.ToranaAuthentication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InternalJwtMinterAndOutboundAuthTest {

    private final String secret = "super-secret-cluster-signing-key-minimum-32-bytes-long!";
    private InternalJwtMinter minter;
    private OutboundAuthPolicyEngine policyEngine;
    private OutboundAuthnConfig outboundConfig;

    @BeforeEach
    void setUp() {
        JwtSignerConfig signerConfig = JwtSignerConfig.builder()
                .issuer("torana-gateway")
                .secret(secret)
                .tokenTtlSeconds(300)
                .keyId("gw-k1")
                .build();

        minter = new InternalJwtMinter(signerConfig);

        outboundConfig = OutboundAuthnConfig.builder()
                .enabled(true)
                .defaultPolicy("MINT_INTERNAL_JWT")
                .jwtSigner(signerConfig)
                .services(Map.of(
                        "llm-service", ServiceOutboundConfig.builder()
                                .policy("INJECT_API_KEY")
                                .apiKeyHeader("X-LLM-Key")
                                .apiKeyValue("secret-llm-token-123")
                                .build(),
                        "legacy-service", ServiceOutboundConfig.builder()
                                .policy("FORWARD_CALLER_TOKEN")
                                .build(),
                        "mesh-service", ServiceOutboundConfig.builder()
                                .policy("PROPAGATE_IDENTITY_HEADERS")
                                .customHeaders(Map.of("X-Mesh-Tag", "torana-v1"))
                                .build()
                ))
                .build();

        policyEngine = new OutboundAuthPolicyEngine(outboundConfig, minter);
    }

    @Test
    void testMintInternalJwt() throws Exception {
        ToranaAuthentication auth = ToranaAuthentication.builder()
                .principalId("agent-007")
                .name("James Bond")
                .tenantId("tenant-mi6")
                .roles(Set.of("special-agent"))
                .scopes(Set.of("license:execute"))
                .build();

        String jwt = minter.mintToken(auth, "internal-vault-service");
        assertNotNull(jwt);

        SignedJWT parsed = SignedJWT.parse(jwt);
        assertTrue(parsed.verify(new MACVerifier(secret.getBytes(StandardCharsets.UTF_8))));

        assertEquals("torana-gateway", parsed.getJWTClaimsSet().getIssuer());
        assertEquals("agent-007", parsed.getJWTClaimsSet().getSubject());
        assertTrue(parsed.getJWTClaimsSet().getAudience().contains("internal-vault-service"));
        assertEquals("tenant-mi6", parsed.getJWTClaimsSet().getStringClaim("tenant_id"));
    }

    @Test
    void testOutboundPolicyMintInternalJwtByDefault() {
        ToranaAuthentication auth = ToranaAuthentication.builder()
                .principalId("user-alpha")
                .tenantId("tenant-core")
                .build();

        HttpHeaders headers = new HttpHeaders();
        policyEngine.applyOutboundAuth(headers, "unknown-internal-service", auth);

        assertNotNull(headers.getFirst(HttpHeaders.AUTHORIZATION));
        assertTrue(headers.getFirst(HttpHeaders.AUTHORIZATION).startsWith("Bearer ey"));
    }

    @Test
    void testOutboundPolicyInjectApiKey() {
        ToranaAuthentication auth = ToranaAuthentication.builder()
                .principalId("user-alpha")
                .build();

        HttpHeaders headers = new HttpHeaders();
        policyEngine.applyOutboundAuth(headers, "llm-service", auth);

        assertEquals("secret-llm-token-123", headers.getFirst("X-LLM-Key"));
    }

    @Test
    void testOutboundPolicyForwardCallerToken() {
        ToranaAuthentication auth = ToranaAuthentication.builder()
                .principalId("user-alpha")
                .rawToken("inbound-caller-bearer-token-999")
                .build();

        HttpHeaders headers = new HttpHeaders();
        policyEngine.applyOutboundAuth(headers, "legacy-service", auth);

        assertEquals("Bearer inbound-caller-bearer-token-999", headers.getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void testOutboundPolicyPropagateIdentityHeaders() {
        ToranaAuthentication auth = ToranaAuthentication.builder()
                .principalId("user-mesh-1")
                .tenantId("tenant-mesh")
                .roles(Set.of("admin"))
                .build();

        HttpHeaders headers = new HttpHeaders();
        policyEngine.applyOutboundAuth(headers, "mesh-service", auth);

        assertEquals("user-mesh-1", headers.getFirst("X-Torana-User"));
        assertEquals("tenant-mesh", headers.getFirst("X-Torana-Tenant"));
        assertEquals("admin", headers.getFirst("X-Torana-Roles"));
        assertEquals("torana-v1", headers.getFirst("X-Mesh-Tag"));
    }
}
