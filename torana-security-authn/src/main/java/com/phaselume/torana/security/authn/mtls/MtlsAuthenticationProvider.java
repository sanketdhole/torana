package com.phaselume.torana.security.authn.mtls;

import com.phaselume.torana.core.config.ToranaProperties.SecurityProperties.AuthnProperties.MtlsConfig;
import com.phaselume.torana.core.model.ToranaAuthentication;
import com.phaselume.torana.core.spi.AuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Map;

/**
 * Authentication provider for Mutual TLS (mTLS) client certificate verification.
 */
public class MtlsAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(MtlsAuthenticationProvider.class);

    private final ClientCertificateValidator validator;
    private final CertificateDnExtractor dnExtractor;
    private final MtlsConfig config;

    public MtlsAuthenticationProvider(ClientCertificateValidator validator,
                                      CertificateDnExtractor dnExtractor,
                                      MtlsConfig config) {
        this.validator = validator != null ? validator : new ClientCertificateValidator();
        this.dnExtractor = dnExtractor != null ? dnExtractor : new CertificateDnExtractor();
        this.config = config != null ? config : new MtlsConfig();
    }

    @Override
    public String type() {
        return "mtls";
    }

    @Override
    public Mono<ToranaAuthentication> authenticate(ServerWebExchange exchange) {
        if (exchange == null || exchange.getRequest() == null) {
            return Mono.empty();
        }

        X509Certificate cert = extractCertificate(exchange);
        if (cert == null) {
            return Mono.empty();
        }

        return validator.validate(cert)
                .then(Mono.fromCallable(() -> {
                    String principalId = dnExtractor.extractPrincipal(cert, config.getPrincipalField());
                    String dn = cert.getSubjectX500Principal().getName();

                    return ToranaAuthentication.builder()
                            .principalId(principalId != null ? principalId : "unknown-client-cert")
                            .name(principalId)
                            .tenantId("default")
                            .authMethod("mtls")
                            .roles(Collections.emptySet())
                            .scopes(Collections.emptySet())
                            .claims(Map.of(
                                    "subject_dn", dn,
                                    "serial_number", cert.getSerialNumber().toString(16),
                                    "issuer_dn", cert.getIssuerX500Principal().getName()
                            ))
                            .build();
                }))
                .doOnError(err -> log.debug("mTLS authentication failed: {}", err.getMessage()));
    }

    private X509Certificate extractCertificate(ServerWebExchange exchange) {
        // Check SSL info from reactive exchange
        SslInfo sslInfo = exchange.getRequest().getSslInfo();
        if (sslInfo != null) {
            X509Certificate[] certs = sslInfo.getPeerCertificates();
            if (certs != null && certs.length > 0 && certs[0] != null) {
                return certs[0];
            }
        }

        // Check proxy forwarding header if configured
        String headerName = config.getClientCertHeader();
        if (headerName != null && !headerName.isBlank()) {
            String certHeader = exchange.getRequest().getHeaders().getFirst(headerName);
            if (certHeader != null && !certHeader.isBlank()) {
                return parseCertHeader(certHeader);
            }
        }

        return null;
    }

    private X509Certificate parseCertHeader(String header) {
        try {
            String pem = header;
            if (header.contains("%")) {
                pem = URLDecoder.decode(header, StandardCharsets.UTF_8);
            }
            // If raw base64 or PEM
            if (!pem.contains("-----BEGIN CERTIFICATE-----")) {
                pem = "-----BEGIN CERTIFICATE-----\n" + pem + "\n-----END CERTIFICATE-----";
            }
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.debug("Failed to parse client certificate from header: {}", e.getMessage());
            return null;
        }
    }
}
