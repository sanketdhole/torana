package com.phaselume.torana.security.authn.mtls;

import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthnProperties.MtlsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MtlsAuthenticationProviderTest {

    private MtlsAuthenticationProvider provider;
    private CertificateDnExtractor dnExtractor;
    private ClientCertificateValidator validator;
    private MtlsConfig config;

    @BeforeEach
    void setUp() {
        dnExtractor = new CertificateDnExtractor();
        validator = new ClientCertificateValidator();
        config = MtlsConfig.builder()
                .principalField("CN")
                .clientCertHeader("X-Forwarded-Client-Cert")
                .build();
        provider = new MtlsAuthenticationProvider(validator, dnExtractor, config);
    }

    @Test
    void testMtlsAuthenticationWithSslInfo() throws Exception {
        X509Certificate cert = mock(X509Certificate.class);
        when(cert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=agent-worker-01, OU=Engineering, O=Torana Corp, C=US"));
        when(cert.getIssuerX500Principal()).thenReturn(new X500Principal("CN=Torana Internal CA"));
        when(cert.getSerialNumber()).thenReturn(BigInteger.valueOf(123456));

        SslInfo sslInfo = mock(SslInfo.class);
        when(sslInfo.getPeerCertificates()).thenReturn(new X509Certificate[]{cert});

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/secure")
                .sslInfo(sslInfo)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(provider.authenticate(exchange))
                .assertNext(auth -> {
                    assertNotNull(auth);
                    assertEquals("agent-worker-01", auth.getPrincipalId());
                    assertEquals("mtls", auth.getAuthMethod());
                    assertTrue(auth.getClaims().get("subject_dn").toString().contains("CN=agent-worker-01"));
                })
                .verifyComplete();
    }

    @Test
    void testMtlsWithoutCertReturnsEmpty() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/secure").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(provider.authenticate(exchange))
                .verifyComplete();
    }
}
